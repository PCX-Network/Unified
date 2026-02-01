/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static service location fallback for accessing services outside of DI context.
 *
 * <p>ServiceLocator provides a way to access services when dependency injection
 * is not available or practical. While DI is preferred, there are legitimate
 * use cases for service location:</p>
 *
 * <ul>
 *   <li>Legacy code integration that cannot be easily refactored</li>
 *   <li>Static utility methods that need service access</li>
 *   <li>Framework callbacks where injection is not supported</li>
 *   <li>Cross-plugin service discovery</li>
 * </ul>
 *
 * <h2>Important Note</h2>
 * <p><b>Prefer dependency injection over service location whenever possible.</b>
 * Service location makes code harder to test and hides dependencies. Use this
 * class only when DI is not feasible.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Get a service
 * Optional<DatabaseService> db = ServiceLocator.get(DatabaseService.class);
 *
 * // Get with default
 * EconomyService economy = ServiceLocator.getOrDefault(
 *     EconomyService.class,
 *     new NoOpEconomyService()
 * );
 *
 * // Get required service (throws if not found)
 * PlayerManager players = ServiceLocator.require(PlayerManager.class);
 * }</pre>
 *
 * <h2>Named Services</h2>
 * <pre>{@code
 * // Get named service
 * Optional<Cache> redisCache = ServiceLocator.get(Cache.class, "redis");
 * Optional<Cache> memoryCache = ServiceLocator.get(Cache.class, "memory");
 * }</pre>
 *
 * <h2>Plugin-Specific Services</h2>
 * <pre>{@code
 * // Get service from specific plugin
 * Optional<CustomService> service = ServiceLocator.getFromPlugin(
 *     "MyPlugin",
 *     CustomService.class
 * );
 * }</pre>
 *
 * <h2>Registration</h2>
 * <pre>{@code
 * // Manual service registration (prefer using Guice modules instead)
 * ServiceLocator.register(MyService.class, myServiceInstance);
 *
 * // Register with name
 * ServiceLocator.register(Cache.class, "redis", redisCacheInstance);
 *
 * // Register supplier for lazy instantiation
 * ServiceLocator.registerSupplier(ExpensiveService.class, ExpensiveService::new);
 *
 * // Configure from injector
 * ServiceLocator.configureFromInjector(injector);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe. All operations can be safely called from
 * any thread concurrently.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see InjectorHolder
 * @see InjectorFactory
 */
public final class ServiceLocator {

    private static final Logger LOGGER = Logger.getLogger(ServiceLocator.class.getName());

    /**
     * Direct service registry for manually registered services.
     */
    private static final Map<Key<?>, Object> SERVICES = new ConcurrentHashMap<>();

    /**
     * Supplier registry for lazy service instantiation.
     */
    private static final Map<Key<?>, Supplier<?>> SUPPLIERS = new ConcurrentHashMap<>();

    /**
     * Reference to the primary injector for lookups.
     */
    private static volatile Injector primaryInjector;

    /**
     * Default plugin name for injector lookups.
     */
    private static volatile String defaultPlugin;

    private ServiceLocator() {
        // Utility class
    }

    // ========== Configuration ==========

    /**
     * Sets the primary injector used for service lookups.
     *
     * @param injector the primary injector
     */
    public static void setPrimaryInjector(Injector injector) {
        primaryInjector = injector;
        LOGGER.fine("Primary injector configured");
    }

    /**
     * Sets the default plugin name for injector lookups.
     *
     * @param pluginName the default plugin name
     */
    public static void setDefaultPlugin(String pluginName) {
        defaultPlugin = pluginName;
        LOGGER.fine("Default plugin set to: " + pluginName);
    }

    /**
     * Configures the service locator from an injector.
     *
     * <p>This sets the injector as primary and makes all its bindings
     * available through the service locator.</p>
     *
     * @param injector the injector to configure from
     */
    public static void configureFromInjector(Injector injector) {
        setPrimaryInjector(injector);
    }

    /**
     * Configures the service locator for a specific plugin.
     *
     * @param pluginName the plugin name
     * @param injector the plugin's injector
     */
    public static void configureForPlugin(String pluginName, Injector injector) {
        InjectorHolder.register(pluginName, injector);
        if (defaultPlugin == null) {
            setDefaultPlugin(pluginName);
        }
        if (primaryInjector == null) {
            setPrimaryInjector(injector);
        }
    }

    // ========== Service Lookup ==========

    /**
     * Gets a service by type.
     *
     * <p>Lookup order:</p>
     * <ol>
     *   <li>Direct service registry</li>
     *   <li>Supplier registry (instantiating if needed)</li>
     *   <li>Primary injector</li>
     *   <li>Default plugin's injector</li>
     * </ol>
     *
     * @param type the service type
     * @param <T> the service type
     * @return an Optional containing the service, or empty if not found
     */
    public static <T> Optional<T> get(Class<T> type) {
        return get(Key.get(type));
    }

    /**
     * Gets a named service by type and name.
     *
     * @param type the service type
     * @param name the service name
     * @param <T> the service type
     * @return an Optional containing the service, or empty if not found
     */
    public static <T> Optional<T> get(Class<T> type, String name) {
        return get(Key.get(type, Names.named(name)));
    }

    /**
     * Gets a service by Guice key.
     *
     * @param key the Guice binding key
     * @param <T> the service type
     * @return an Optional containing the service, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(Key<T> key) {
        // Check direct registry
        Object service = SERVICES.get(key);
        if (service != null) {
            return Optional.of((T) service);
        }

        // Check supplier registry
        Supplier<?> supplier = SUPPLIERS.get(key);
        if (supplier != null) {
            T instance = (T) supplier.get();
            // Cache the instance
            SERVICES.put(key, instance);
            return Optional.of(instance);
        }

        // Try primary injector
        if (primaryInjector != null) {
            try {
                return Optional.of(primaryInjector.getInstance(key));
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Service not found in primary injector: " + key, e);
            }
        }

        // Try default plugin's injector
        if (defaultPlugin != null) {
            Optional<T> fromPlugin = InjectorHolder.get(defaultPlugin)
                .map(injector -> {
                    try {
                        return injector.getInstance(key);
                    } catch (Exception e) {
                        return null;
                    }
                });
            if (fromPlugin.isPresent()) {
                return fromPlugin;
            }
        }

        return Optional.empty();
    }

    /**
     * Gets a service, throwing if not found.
     *
     * @param type the service type
     * @param <T> the service type
     * @return the service
     * @throws IllegalStateException if the service is not found
     */
    public static <T> T require(Class<T> type) {
        return get(type).orElseThrow(() ->
            new IllegalStateException("Required service not found: " + type.getName())
        );
    }

    /**
     * Gets a named service, throwing if not found.
     *
     * @param type the service type
     * @param name the service name
     * @param <T> the service type
     * @return the service
     * @throws IllegalStateException if the service is not found
     */
    public static <T> T require(Class<T> type, String name) {
        return get(type, name).orElseThrow(() ->
            new IllegalStateException("Required service not found: " + type.getName() + " named '" + name + "'")
        );
    }

    /**
     * Gets a service with a default fallback.
     *
     * @param type the service type
     * @param defaultValue the default value if not found
     * @param <T> the service type
     * @return the service or the default value
     */
    public static <T> T getOrDefault(Class<T> type, T defaultValue) {
        return get(type).orElse(defaultValue);
    }

    /**
     * Gets a service with a default supplier.
     *
     * @param type the service type
     * @param defaultSupplier supplier for the default value
     * @param <T> the service type
     * @return the service or the supplied default value
     */
    public static <T> T getOrElse(Class<T> type, Supplier<T> defaultSupplier) {
        return get(type).orElseGet(defaultSupplier);
    }

    /**
     * Gets a service from a specific plugin's injector.
     *
     * @param pluginName the plugin name
     * @param type the service type
     * @param <T> the service type
     * @return an Optional containing the service, or empty if not found
     */
    public static <T> Optional<T> getFromPlugin(String pluginName, Class<T> type) {
        return InjectorHolder.getInstance(pluginName, type);
    }

    /**
     * Checks if a service is available.
     *
     * @param type the service type
     * @return {@code true} if the service can be resolved
     */
    public static boolean isAvailable(Class<?> type) {
        return get(type).isPresent();
    }

    // ========== Service Registration ==========

    /**
     * Registers a service instance directly.
     *
     * @param type the service type
     * @param instance the service instance
     * @param <T> the service type
     */
    public static <T> void register(Class<T> type, T instance) {
        register(Key.get(type), instance);
    }

    /**
     * Registers a named service instance.
     *
     * @param type the service type
     * @param name the service name
     * @param instance the service instance
     * @param <T> the service type
     */
    public static <T> void register(Class<T> type, String name, T instance) {
        register(Key.get(type, Names.named(name)), instance);
    }

    /**
     * Registers a service instance with a Guice key.
     *
     * @param key the Guice key
     * @param instance the service instance
     * @param <T> the service type
     */
    public static <T> void register(Key<T> key, T instance) {
        SERVICES.put(key, instance);
        LOGGER.fine("Registered service: " + key);
    }

    /**
     * Registers a service supplier for lazy instantiation.
     *
     * <p>The supplier is called once on first access, and the result is cached.</p>
     *
     * @param type the service type
     * @param supplier the supplier for creating the service
     * @param <T> the service type
     */
    public static <T> void registerSupplier(Class<T> type, Supplier<T> supplier) {
        SUPPLIERS.put(Key.get(type), supplier);
        LOGGER.fine("Registered service supplier: " + type.getName());
    }

    /**
     * Unregisters a service.
     *
     * @param type the service type
     */
    public static void unregister(Class<?> type) {
        unregister(Key.get(type));
    }

    /**
     * Unregisters a named service.
     *
     * @param type the service type
     * @param name the service name
     */
    public static void unregister(Class<?> type, String name) {
        unregister(Key.get(type, Names.named(name)));
    }

    /**
     * Unregisters a service by key.
     *
     * @param key the Guice key
     */
    public static void unregister(Key<?> key) {
        SERVICES.remove(key);
        SUPPLIERS.remove(key);
        LOGGER.fine("Unregistered service: " + key);
    }

    // ========== Cleanup ==========

    /**
     * Clears all registered services and configuration.
     *
     * <p>This is primarily intended for testing or server shutdown.</p>
     */
    public static void clear() {
        SERVICES.clear();
        SUPPLIERS.clear();
        primaryInjector = null;
        defaultPlugin = null;
        LOGGER.info("ServiceLocator cleared");
    }

    /**
     * Gets the count of directly registered services.
     *
     * @return the number of registered services
     */
    public static int getRegisteredCount() {
        return SERVICES.size() + SUPPLIERS.size();
    }
}
