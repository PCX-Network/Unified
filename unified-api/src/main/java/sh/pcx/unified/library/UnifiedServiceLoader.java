/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Custom service loader utility for the UnifiedPlugin API framework.
 *
 * <p>UnifiedServiceLoader extends Java's standard ServiceLoader with additional
 * features like priority-based ordering, caching, and enhanced error handling.
 * It is designed to work seamlessly with the library classloading system.
 *
 * <h2>Key Differences from java.util.ServiceLoader</h2>
 * <ul>
 *   <li>Priority-based ordering (highest priority first)</li>
 *   <li>Thread-safe caching of discovered services</li>
 *   <li>Enhanced error messages for debugging</li>
 *   <li>Support for programmatic registration</li>
 *   <li>Integration with library classloaders</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Load the highest priority service
 * Optional<MyService> service = UnifiedServiceLoader.load(MyService.class);
 *
 * // Load all services
 * List<MyService> services = UnifiedServiceLoader.loadAll(MyService.class);
 *
 * // Load with custom classloader
 * List<MyService> customServices = UnifiedServiceLoader.loadAll(
 *     MyService.class,
 *     customClassLoader
 * );
 *
 * // Iterate over services
 * for (MyService svc : UnifiedServiceLoader.iterate(MyService.class)) {
 *     svc.doSomething();
 * }
 *
 * // Get service definitions for inspection
 * List<ServiceDefinition> defs = UnifiedServiceLoader.discover(MyService.class);
 * for (ServiceDefinition def : defs) {
 *     System.out.println(def.implementationClass() + " priority=" + def.priority());
 * }
 *
 * // Clear cache to force rediscovery
 * UnifiedServiceLoader.clearCache();
 * }</pre>
 *
 * <h2>Service Priority</h2>
 * <p>Service priority can be specified using a special comment format in
 * the META-INF/services file:
 * <pre>
 * # priority=1000
 * com.example.HighPriorityImpl
 * # priority=100
 * com.example.DefaultImpl
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Service discovery results are cached and
 * shared across threads. Use {@link #clearCache()} if you need to force
 * rediscovery after adding new service providers.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ServiceDiscovery
 * @see ServiceDefinition
 */
public final class UnifiedServiceLoader {

    /**
     * Prefix for META-INF/services files.
     */
    private static final String SERVICES_PREFIX = "META-INF/services/";

    /**
     * Cache of discovered service definitions.
     */
    private static final Map<CacheKey, List<ServiceDefinition>> definitionCache =
            new ConcurrentHashMap<>();

    /**
     * Cache of instantiated services.
     */
    private static final Map<CacheKey, List<?>> instanceCache = new ConcurrentHashMap<>();

    /**
     * Programmatically registered services.
     */
    private static final Map<Class<?>, List<ServiceDefinition>> registeredServices =
            new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private UnifiedServiceLoader() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Loads the highest priority implementation of a service.
     *
     * @param serviceType The service interface or abstract class
     * @param <T>         The service type
     * @return An Optional containing the service, or empty if none found
     * @throws NullPointerException if serviceType is null
     */
    public static <T> Optional<T> load(Class<T> serviceType) {
        return load(serviceType, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads the highest priority implementation using a specific classloader.
     *
     * @param serviceType The service interface or abstract class
     * @param classLoader The classloader to use
     * @param <T>         The service type
     * @return An Optional containing the service, or empty if none found
     * @throws NullPointerException if serviceType or classLoader is null
     */
    public static <T> Optional<T> load(Class<T> serviceType, ClassLoader classLoader) {
        List<T> services = loadAll(serviceType, classLoader);
        return services.isEmpty() ? Optional.empty() : Optional.of(services.get(0));
    }

    /**
     * Loads all implementations of a service, sorted by priority.
     *
     * @param serviceType The service interface or abstract class
     * @param <T>         The service type
     * @return A list of service instances (highest priority first)
     * @throws NullPointerException if serviceType is null
     */
    public static <T> List<T> loadAll(Class<T> serviceType) {
        return loadAll(serviceType, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads all implementations using a specific classloader.
     *
     * @param serviceType The service interface or abstract class
     * @param classLoader The classloader to use
     * @param <T>         The service type
     * @return A list of service instances (highest priority first)
     * @throws NullPointerException if serviceType or classLoader is null
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> loadAll(Class<T> serviceType, ClassLoader classLoader) {
        Objects.requireNonNull(serviceType, "Service type cannot be null");
        Objects.requireNonNull(classLoader, "ClassLoader cannot be null");

        CacheKey key = new CacheKey(serviceType, classLoader);

        // Check instance cache
        List<?> cached = instanceCache.get(key);
        if (cached != null) {
            return (List<T>) cached;
        }

        // Load and cache
        List<ServiceDefinition> definitions = discover(serviceType, classLoader);
        List<T> instances = instantiateAll(definitions, classLoader);
        instanceCache.put(key, instances);

        return instances;
    }

    /**
     * Discovers service definitions without instantiating.
     *
     * @param serviceType The service interface or abstract class
     * @param <T>         The service type
     * @return A list of definitions (highest priority first)
     * @throws NullPointerException if serviceType is null
     */
    public static <T> List<ServiceDefinition> discover(Class<T> serviceType) {
        return discover(serviceType, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Discovers service definitions using a specific classloader.
     *
     * @param serviceType The service interface or abstract class
     * @param classLoader The classloader to use
     * @param <T>         The service type
     * @return A list of definitions (highest priority first)
     * @throws NullPointerException if serviceType or classLoader is null
     */
    public static <T> List<ServiceDefinition> discover(Class<T> serviceType, ClassLoader classLoader) {
        Objects.requireNonNull(serviceType, "Service type cannot be null");
        Objects.requireNonNull(classLoader, "ClassLoader cannot be null");

        CacheKey key = new CacheKey(serviceType, classLoader);

        return definitionCache.computeIfAbsent(key, k -> {
            List<ServiceDefinition> definitions = new ArrayList<>();

            // Add programmatically registered services
            List<ServiceDefinition> registered = registeredServices.get(serviceType);
            if (registered != null) {
                definitions.addAll(registered);
            }

            // Discover from META-INF/services
            definitions.addAll(discoverFromSPI(serviceType, classLoader));

            // Sort by priority (highest first)
            definitions.sort(Comparator.comparingInt(ServiceDefinition::priority).reversed());

            return Collections.unmodifiableList(definitions);
        });
    }

    /**
     * Discovers services from SPI configuration files.
     */
    private static <T> List<ServiceDefinition> discoverFromSPI(
            Class<T> serviceType,
            ClassLoader classLoader
    ) {
        List<ServiceDefinition> definitions = new ArrayList<>();
        String resourceName = SERVICES_PREFIX + serviceType.getName();

        try {
            Enumeration<URL> resources = classLoader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                definitions.addAll(parseProviderFile(serviceType, url));
            }
        } catch (IOException e) {
            // Log warning but continue
            System.err.println("Warning: Error reading service providers for "
                    + serviceType.getName() + ": " + e.getMessage());
        }

        return definitions;
    }

    /**
     * Parses a provider configuration file.
     */
    private static <T> List<ServiceDefinition> parseProviderFile(Class<T> serviceType, URL url) {
        List<ServiceDefinition> definitions = new ArrayList<>();
        int currentPriority = ServiceDefinition.DEFAULT_PRIORITY;
        Map<String, String> currentProperties = new LinkedHashMap<>();

        try (InputStream is = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Parse special comments
                if (line.startsWith("#")) {
                    String comment = line.substring(1).trim();
                    if (comment.startsWith("priority=")) {
                        try {
                            currentPriority = Integer.parseInt(comment.substring(9).trim());
                        } catch (NumberFormatException e) {
                            // Ignore invalid priority
                        }
                    } else if (comment.contains("=")) {
                        int eq = comment.indexOf('=');
                        String key = comment.substring(0, eq).trim();
                        String value = comment.substring(eq + 1).trim();
                        currentProperties.put(key, value);
                    }
                    continue;
                }

                // Remove inline comments
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex).trim();
                }

                if (!line.isEmpty()) {
                    definitions.add(new ServiceDefinition(
                            serviceType,
                            line,
                            currentPriority,
                            new LinkedHashMap<>(currentProperties)
                    ));
                    // Reset properties but keep priority for next entry
                    currentProperties.clear();
                }
            }
        } catch (IOException e) {
            throw new ServiceConfigurationError(
                    "Error reading provider file: " + url, e);
        }

        return definitions;
    }

    /**
     * Instantiates all service implementations.
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> instantiateAll(
            List<ServiceDefinition> definitions,
            ClassLoader classLoader
    ) {
        List<T> instances = new ArrayList<>();

        for (ServiceDefinition def : definitions) {
            if (def.isDisabled()) {
                continue;
            }

            try {
                Class<?> implClass = Class.forName(def.implementationClass(), true, classLoader);
                Constructor<?> constructor = implClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                T instance = (T) constructor.newInstance();
                instances.add(instance);
            } catch (ClassNotFoundException e) {
                System.err.println("Warning: Service implementation not found: "
                        + def.implementationClass());
            } catch (Exception e) {
                System.err.println("Warning: Failed to instantiate service "
                        + def.implementationClass() + ": " + e.getMessage());
            }
        }

        return instances;
    }

    /**
     * Returns an iterator over service implementations.
     *
     * @param serviceType The service interface or abstract class
     * @param <T>         The service type
     * @return An iterator over service instances
     * @throws NullPointerException if serviceType is null
     */
    public static <T> Iterable<T> iterate(Class<T> serviceType) {
        return () -> loadAll(serviceType).iterator();
    }

    /**
     * Returns an iterator using a specific classloader.
     *
     * @param serviceType The service interface or abstract class
     * @param classLoader The classloader to use
     * @param <T>         The service type
     * @return An iterator over service instances
     * @throws NullPointerException if serviceType or classLoader is null
     */
    public static <T> Iterable<T> iterate(Class<T> serviceType, ClassLoader classLoader) {
        return () -> loadAll(serviceType, classLoader).iterator();
    }

    /**
     * Loads services matching a filter.
     *
     * @param serviceType The service interface or abstract class
     * @param filter      A predicate to filter definitions
     * @param <T>         The service type
     * @return A list of matching service instances
     * @throws NullPointerException if serviceType or filter is null
     */
    public static <T> List<T> loadFiltered(Class<T> serviceType, Predicate<ServiceDefinition> filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");

        List<ServiceDefinition> filtered = new ArrayList<>();
        for (ServiceDefinition def : discover(serviceType)) {
            if (filter.test(def)) {
                filtered.add(def);
            }
        }

        return instantiateAll(filtered, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Registers a service implementation programmatically.
     *
     * @param definition The service definition to register
     * @throws NullPointerException if definition is null
     */
    public static void register(ServiceDefinition definition) {
        Objects.requireNonNull(definition, "Definition cannot be null");

        registeredServices.computeIfAbsent(definition.serviceType(), k -> new ArrayList<>())
                .add(definition);

        // Clear relevant caches
        clearCacheForType(definition.serviceType());
    }

    /**
     * Registers a service instance with the given priority.
     *
     * @param serviceType The service interface
     * @param instance    The service instance
     * @param priority    The priority value
     * @param <T>         The service type
     * @throws NullPointerException if serviceType or instance is null
     */
    public static <T> void registerInstance(Class<T> serviceType, T instance, int priority) {
        Objects.requireNonNull(serviceType, "Service type cannot be null");
        Objects.requireNonNull(instance, "Instance cannot be null");

        ServiceDefinition definition = new ServiceDefinition(
                serviceType,
                instance.getClass().getName(),
                priority,
                Map.of()
        );

        register(definition);
    }

    /**
     * Unregisters a service implementation.
     *
     * @param serviceType         The service interface
     * @param implementationClass The implementation class name
     * @return true if the service was unregistered
     */
    public static boolean unregister(Class<?> serviceType, String implementationClass) {
        List<ServiceDefinition> definitions = registeredServices.get(serviceType);
        if (definitions == null) {
            return false;
        }

        boolean removed = definitions.removeIf(
                def -> def.implementationClass().equals(implementationClass));

        if (removed) {
            clearCacheForType(serviceType);
        }

        return removed;
    }

    /**
     * Checks if a service type has any implementations.
     *
     * @param serviceType The service interface or abstract class
     * @return true if at least one implementation is available
     * @throws NullPointerException if serviceType is null
     */
    public static boolean hasService(Class<?> serviceType) {
        return !discover(serviceType).isEmpty();
    }

    /**
     * Gets the count of available implementations.
     *
     * @param serviceType The service interface or abstract class
     * @return The count of implementations
     * @throws NullPointerException if serviceType is null
     */
    public static int getServiceCount(Class<?> serviceType) {
        return discover(serviceType).size();
    }

    /**
     * Clears all cached service discoveries and instances.
     */
    public static void clearCache() {
        definitionCache.clear();
        instanceCache.clear();
    }

    /**
     * Clears cache entries for a specific service type.
     */
    private static void clearCacheForType(Class<?> serviceType) {
        definitionCache.entrySet().removeIf(e -> e.getKey().serviceType == serviceType);
        instanceCache.entrySet().removeIf(e -> e.getKey().serviceType == serviceType);
    }

    /**
     * Cache key combining service type and classloader.
     */
    private record CacheKey(Class<?> serviceType, ClassLoader classLoader) {
    }
}
