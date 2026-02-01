/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject.lifecycle;

import com.google.inject.Injector;
import com.google.inject.spi.InjectionListener;
import sh.pcx.unified.inject.PostConstruct;
import sh.pcx.unified.inject.PreDestroy;
import sh.pcx.unified.inject.OnReload;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guice {@link InjectionListener} that processes lifecycle annotations after injection.
 *
 * <p>This listener is automatically registered by {@link sh.pcx.unified.inject.UnifiedModule}
 * to process {@link PostConstruct} annotations on newly injected objects. It invokes
 * annotated methods immediately after all dependencies have been injected.</p>
 *
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Guice creates an object and injects all dependencies</li>
 *   <li>LifecycleListener is notified via {@link #afterInjection(Object)}</li>
 *   <li>Listener scans for {@code @PostConstruct} methods</li>
 *   <li>Annotated methods are invoked in priority order</li>
 * </ol>
 *
 * <h2>Registration</h2>
 * <p>The listener is registered automatically when using {@code UnifiedModule}:</p>
 *
 * <pre>{@code
 * public class UnifiedModule extends AbstractModule {
 *     @Override
 *     protected void configure() {
 *         // Register lifecycle listener for all types
 *         bindListener(Matchers.any(), new LifecycleTypeListener());
 *     }
 * }
 * }</pre>
 *
 * <h2>Manual Registration</h2>
 * <p>For custom modules, register the listener explicitly:</p>
 *
 * <pre>{@code
 * public class MyModule extends AbstractModule {
 *     @Override
 *     protected void configure() {
 *         // Register for specific types
 *         bindListener(Matchers.subclassesOf(MyService.class), encounter -> {
 *             encounter.register(new LifecycleListener<>());
 *         });
 *
 *         // Or for all types
 *         bindListener(Matchers.any(), encounter -> {
 *             encounter.register(new LifecycleListener<>());
 *         });
 *     }
 * }
 * }</pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class DatabasePool {
 *     @Inject
 *     private DatabaseConfig config;
 *
 *     private HikariDataSource dataSource;
 *
 *     // This method is called automatically after injection
 *     @PostConstruct
 *     public void initialize() {
 *         HikariConfig hikariConfig = new HikariConfig();
 *         hikariConfig.setJdbcUrl(config.getJdbcUrl());
 *         hikariConfig.setUsername(config.getUsername());
 *         hikariConfig.setPassword(config.getPassword());
 *         this.dataSource = new HikariDataSource(hikariConfig);
 *     }
 *
 *     @PreDestroy
 *     public void shutdown() {
 *         if (dataSource != null) {
 *             dataSource.close();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <p>If a {@code @PostConstruct} method throws an exception, it is wrapped in a
 * {@link RuntimeException} and propagated. This causes the injection to fail,
 * which is usually the desired behavior for initialization failures.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Multiple threads can safely create and inject
 * objects concurrently.</p>
 *
 * @param <T> the type of object being listened to
 * @author Supatuck
 * @since 1.0.0
 * @see PostConstruct
 * @see PreDestroy
 * @see OnReload
 * @see LifecycleProcessor
 */
public class LifecycleListener<T> implements InjectionListener<T> {

    private static final Logger LOGGER = Logger.getLogger(LifecycleListener.class.getName());

    /**
     * Cached reference to the LifecycleProcessor.
     */
    private volatile LifecycleProcessor processor;

    /**
     * Creates a new LifecycleListener.
     */
    public LifecycleListener() {
    }

    /**
     * Called by Guice after an object has been fully injected.
     *
     * <p>This method scans the injected object for {@link PostConstruct} annotated
     * methods and invokes them in priority order.</p>
     *
     * @param injectee the object that was just injected
     */
    @Override
    public void afterInjection(T injectee) {
        if (injectee == null) {
            return;
        }

        // Check if the class has any lifecycle annotations to avoid unnecessary processing
        if (!hasLifecycleAnnotations(injectee.getClass())) {
            return;
        }

        LOGGER.fine("Processing lifecycle annotations for: " + injectee.getClass().getName());

        try {
            // Try to get LifecycleProcessor from injector
            if (processor == null) {
                processor = getOrCreateProcessor();
            }

            // Invoke PostConstruct methods
            processor.invokePostConstruct(injectee);

        } catch (RuntimeException e) {
            // Re-throw RuntimeExceptions (including wrapped PostConstruct failures)
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process lifecycle for: " + injectee.getClass().getName(), e);
            throw new RuntimeException("Lifecycle processing failed for " + injectee.getClass().getName(), e);
        }
    }

    /**
     * Checks if a class has any lifecycle annotations.
     *
     * <p>This is a quick check to avoid reflection overhead for classes
     * that don't use lifecycle annotations.</p>
     *
     * @param clazz the class to check
     * @return {@code true} if the class has lifecycle annotations
     */
    private boolean hasLifecycleAnnotations(Class<?> clazz) {
        // Walk the class hierarchy
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class) ||
                    method.isAnnotationPresent(PreDestroy.class) ||
                    method.isAnnotationPresent(OnReload.class)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    /**
     * Gets or creates a LifecycleProcessor.
     *
     * <p>This method creates a standalone processor if one isn't available
     * from the injector. This can happen in some edge cases during early
     * injection phases.</p>
     *
     * @return the lifecycle processor
     */
    private LifecycleProcessor getOrCreateProcessor() {
        // Create a new processor if not available
        // In a real implementation, this would be retrieved from the Injector
        return new LifecycleProcessor();
    }

    /**
     * Sets the LifecycleProcessor to use for callback invocation.
     *
     * <p>This is typically called by the Guice module during setup to ensure
     * all listeners share the same processor instance.</p>
     *
     * @param processor the processor to use
     */
    public void setProcessor(LifecycleProcessor processor) {
        this.processor = processor;
    }

    /**
     * Creates a LifecycleListener that uses the specified processor.
     *
     * @param processor the lifecycle processor to use
     * @param <T> the type parameter
     * @return a new listener instance
     */
    public static <T> LifecycleListener<T> withProcessor(LifecycleProcessor processor) {
        LifecycleListener<T> listener = new LifecycleListener<>();
        listener.setProcessor(processor);
        return listener;
    }
}
