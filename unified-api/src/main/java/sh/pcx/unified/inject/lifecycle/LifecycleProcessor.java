/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject.lifecycle;

import sh.pcx.unified.inject.OnReload;
import sh.pcx.unified.inject.PostConstruct;
import sh.pcx.unified.inject.PreDestroy;

import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes lifecycle annotations on injected objects.
 *
 * <p>LifecycleProcessor is responsible for discovering and invoking methods
 * annotated with {@link PostConstruct}, {@link PreDestroy}, and {@link OnReload}.
 * It caches reflection data for performance and handles error cases gracefully.</p>
 *
 * <h2>Annotation Processing</h2>
 * <ul>
 *   <li>{@link PostConstruct} - Invoked immediately after injection completes</li>
 *   <li>{@link PreDestroy} - Invoked before object destruction or scope exit</li>
 *   <li>{@link OnReload} - Invoked when plugin configuration is reloaded</li>
 * </ul>
 *
 * <h2>Method Discovery</h2>
 * <p>The processor discovers annotated methods by scanning the class hierarchy,
 * including superclasses. Methods are cached by class to avoid repeated reflection.</p>
 *
 * <h2>Execution Order</h2>
 * <ul>
 *   <li>{@code @PostConstruct}: Superclass first, then subclass (by priority)</li>
 *   <li>{@code @PreDestroy}: Subclass first, then superclass (by priority)</li>
 *   <li>{@code @OnReload}: By priority (lower values first)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class MyService {
 *     @Inject private DatabaseService database;
 *
 *     @PostConstruct
 *     public void initialize() {
 *         // Called after injection - database is available
 *         database.connect();
 *     }
 *
 *     @PreDestroy
 *     public void cleanup() {
 *         // Called before destruction
 *         database.disconnect();
 *     }
 *
 *     @OnReload
 *     public void onConfigReload() {
 *         // Called when config reloads
 *         reloadSettings();
 *     }
 * }
 * }</pre>
 *
 * <h2>Manual Invocation</h2>
 * <pre>{@code
 * @Inject
 * private LifecycleProcessor lifecycle;
 *
 * // Invoke PostConstruct on an object
 * lifecycle.invokePostConstruct(myObject);
 *
 * // Invoke PreDestroy on an object
 * lifecycle.invokePreDestroy(myObject);
 *
 * // Trigger reload on all registered objects
 * lifecycle.triggerReload();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PostConstruct
 * @see PreDestroy
 * @see OnReload
 * @see LifecycleListener
 */
@Singleton
public class LifecycleProcessor {

    private static final Logger LOGGER = Logger.getLogger(LifecycleProcessor.class.getName());

    /**
     * Cache of PostConstruct methods by class.
     */
    private final Map<Class<?>, List<Method>> postConstructCache = new ConcurrentHashMap<>();

    /**
     * Cache of PreDestroy methods by class.
     */
    private final Map<Class<?>, List<Method>> preDestroyCache = new ConcurrentHashMap<>();

    /**
     * Cache of OnReload methods by class.
     */
    private final Map<Class<?>, List<Method>> onReloadCache = new ConcurrentHashMap<>();

    /**
     * Set of objects registered for reload callbacks.
     */
    private final Set<Object> reloadTargets = ConcurrentHashMap.newKeySet();

    /**
     * Executor for async reload methods.
     */
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "UnifiedPlugin-Lifecycle-Async");
        t.setDaemon(true);
        return t;
    });

    /**
     * Creates a new LifecycleProcessor.
     */
    public LifecycleProcessor() {
    }

    // ========== PostConstruct ==========

    /**
     * Invokes all {@link PostConstruct} methods on the given object.
     *
     * <p>Methods are invoked in order: superclass first, then subclass,
     * sorted by priority within each class.</p>
     *
     * @param instance the object to process
     * @throws RuntimeException if a PostConstruct method throws an exception
     */
    public void invokePostConstruct(Object instance) {
        if (instance == null) {
            return;
        }

        List<Method> methods = getPostConstructMethods(instance.getClass());
        for (Method method : methods) {
            try {
                LOGGER.fine("Invoking @PostConstruct: " + method.getDeclaringClass().getSimpleName() +
                    "." + method.getName());
                method.setAccessible(true);
                method.invoke(instance);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error invoking @PostConstruct on " +
                    instance.getClass().getName() + "." + method.getName(), e);
                throw new RuntimeException("PostConstruct failed for " + method, e.getCause());
            }
        }

        // Register for reload callbacks if has @OnReload methods
        if (!getOnReloadMethods(instance.getClass()).isEmpty()) {
            reloadTargets.add(instance);
        }
    }

    /**
     * Gets cached PostConstruct methods for a class.
     */
    private List<Method> getPostConstructMethods(Class<?> clazz) {
        return postConstructCache.computeIfAbsent(clazz, this::discoverPostConstructMethods);
    }

    /**
     * Discovers PostConstruct methods in the class hierarchy.
     */
    private List<Method> discoverPostConstructMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        collectAnnotatedMethods(clazz, PostConstruct.class, methods);

        // Sort by priority (lower first), then by declaration order
        methods.sort(Comparator.comparingInt(m ->
            m.getAnnotation(PostConstruct.class).priority()
        ));

        return methods;
    }

    // ========== PreDestroy ==========

    /**
     * Invokes all {@link PreDestroy} methods on the given object.
     *
     * <p>Methods are invoked in reverse order: subclass first, then superclass,
     * sorted by priority within each class. Exceptions are logged but do not
     * prevent other methods from being called.</p>
     *
     * @param instance the object to process
     */
    public void invokePreDestroy(Object instance) {
        if (instance == null) {
            return;
        }

        // Unregister from reload callbacks
        reloadTargets.remove(instance);

        List<Method> methods = getPreDestroyMethods(instance.getClass());
        for (Method method : methods) {
            try {
                PreDestroy annotation = method.getAnnotation(PreDestroy.class);
                long timeout = annotation.timeout();

                LOGGER.fine("Invoking @PreDestroy: " + method.getDeclaringClass().getSimpleName() +
                    "." + method.getName());
                method.setAccessible(true);

                if (timeout > 0) {
                    // Invoke with timeout
                    invokeWithTimeout(instance, method, timeout);
                } else {
                    // Invoke directly
                    method.invoke(instance);
                }
            } catch (TimeoutException e) {
                LOGGER.warning("@PreDestroy timed out: " + method);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error invoking @PreDestroy on " +
                    instance.getClass().getName() + "." + method.getName(), e);
                // Continue with other methods
            }
        }
    }

    /**
     * Invokes a method with a timeout.
     */
    private void invokeWithTimeout(Object instance, Method method, long timeoutMs)
            throws Exception {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, asyncExecutor);

        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * Gets cached PreDestroy methods for a class.
     */
    private List<Method> getPreDestroyMethods(Class<?> clazz) {
        return preDestroyCache.computeIfAbsent(clazz, this::discoverPreDestroyMethods);
    }

    /**
     * Discovers PreDestroy methods in the class hierarchy.
     */
    private List<Method> discoverPreDestroyMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        collectAnnotatedMethods(clazz, PreDestroy.class, methods);

        // Sort by priority (higher first for cleanup - reverse order)
        methods.sort(Comparator.comparingInt((Method m) ->
            m.getAnnotation(PreDestroy.class).priority()
        ).reversed());

        return methods;
    }

    // ========== OnReload ==========

    /**
     * Invokes all {@link OnReload} methods on the given object.
     *
     * @param instance the object to process
     */
    public void invokeOnReload(Object instance) {
        if (instance == null) {
            return;
        }

        List<Method> methods = getOnReloadMethods(instance.getClass());
        for (Method method : methods) {
            try {
                OnReload annotation = method.getAnnotation(OnReload.class);

                LOGGER.fine("Invoking @OnReload: " + method.getDeclaringClass().getSimpleName() +
                    "." + method.getName());
                method.setAccessible(true);

                if (annotation.async()) {
                    // Invoke asynchronously
                    asyncExecutor.submit(() -> {
                        try {
                            method.invoke(instance);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error invoking async @OnReload on " +
                                instance.getClass().getName() + "." + method.getName(), e);
                        }
                    });
                } else {
                    // Invoke synchronously
                    method.invoke(instance);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error invoking @OnReload on " +
                    instance.getClass().getName() + "." + method.getName(), e);
            }
        }
    }

    /**
     * Triggers reload on all registered objects.
     *
     * <p>All objects that have {@code @OnReload} methods and were processed
     * by this processor will have their reload methods invoked.</p>
     */
    public void triggerReload() {
        LOGGER.info("Triggering reload for " + reloadTargets.size() + " registered objects");

        // Create sorted list by priority
        List<Object> sortedTargets = new ArrayList<>(reloadTargets);

        for (Object target : sortedTargets) {
            try {
                invokeOnReload(target);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during reload for " + target.getClass().getName(), e);
            }
        }

        LOGGER.info("Reload complete");
    }

    /**
     * Triggers reload only for objects with methods watching specific config keys.
     *
     * @param changedKeys the configuration keys that changed
     */
    public void triggerReload(Set<String> changedKeys) {
        if (changedKeys == null || changedKeys.isEmpty()) {
            triggerReload();
            return;
        }

        for (Object target : reloadTargets) {
            List<Method> methods = getOnReloadMethods(target.getClass());
            for (Method method : methods) {
                OnReload annotation = method.getAnnotation(OnReload.class);
                String[] watchKeys = annotation.watchKeys();

                // If no watch keys specified, or if any watched key changed
                if (watchKeys.length == 0 || Arrays.stream(watchKeys).anyMatch(changedKeys::contains)) {
                    try {
                        method.setAccessible(true);
                        if (annotation.async()) {
                            asyncExecutor.submit(() -> {
                                try {
                                    method.invoke(target);
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Error invoking async @OnReload", e);
                                }
                            });
                        } else {
                            method.invoke(target);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error invoking @OnReload", e);
                    }
                }
            }
        }
    }

    /**
     * Gets cached OnReload methods for a class.
     */
    private List<Method> getOnReloadMethods(Class<?> clazz) {
        return onReloadCache.computeIfAbsent(clazz, this::discoverOnReloadMethods);
    }

    /**
     * Discovers OnReload methods in the class hierarchy.
     */
    private List<Method> discoverOnReloadMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        collectAnnotatedMethods(clazz, OnReload.class, methods);

        // Sort by priority (lower first)
        methods.sort(Comparator.comparingInt(m ->
            m.getAnnotation(OnReload.class).priority()
        ));

        return methods;
    }

    // ========== Helper Methods ==========

    /**
     * Collects methods with a specific annotation from the class hierarchy.
     */
    private void collectAnnotatedMethods(Class<?> clazz,
                                          Class<? extends java.lang.annotation.Annotation> annotation,
                                          List<Method> result) {
        if (clazz == null || clazz == Object.class) {
            return;
        }

        // Process superclass first (for PostConstruct order)
        collectAnnotatedMethods(clazz.getSuperclass(), annotation, result);

        // Process declared methods
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                result.add(method);
            }
        }
    }

    /**
     * Registers an object for reload callbacks.
     *
     * <p>Objects are automatically registered when processed via
     * {@link #invokePostConstruct(Object)} if they have @OnReload methods.</p>
     *
     * @param instance the object to register
     */
    public void registerForReload(Object instance) {
        if (instance != null && !getOnReloadMethods(instance.getClass()).isEmpty()) {
            reloadTargets.add(instance);
        }
    }

    /**
     * Unregisters an object from reload callbacks.
     *
     * @param instance the object to unregister
     */
    public void unregisterFromReload(Object instance) {
        reloadTargets.remove(instance);
    }

    /**
     * Gets the count of objects registered for reload callbacks.
     *
     * @return the number of registered reload targets
     */
    public int getReloadTargetCount() {
        return reloadTargets.size();
    }

    /**
     * Shuts down the lifecycle processor.
     *
     * <p>This terminates the async executor and clears all caches.</p>
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        reloadTargets.clear();
        postConstructCache.clear();
        preDestroyCache.clear();
        onReloadCache.clear();

        LOGGER.info("LifecycleProcessor shutdown complete");
    }
}
