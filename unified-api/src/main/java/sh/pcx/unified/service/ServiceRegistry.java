/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Registry for managing service registration and lookup.
 *
 * <p>The ServiceRegistry provides a centralized location for registering and
 * retrieving services. Services are identified by their interface type and
 * can be registered with different priorities.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the service registry
 * ServiceRegistry services = UnifiedAPI.getInstance().services();
 *
 * // Register a service
 * services.register(MyService.class, new MyServiceImpl());
 *
 * // Register with a specific provider
 * services.register(MyService.class, myPlugin, new MyServiceImpl(), ServicePriority.NORMAL);
 *
 * // Retrieve a service
 * Optional<MyService> service = services.get(MyService.class);
 *
 * // Check if service exists
 * if (services.isRegistered(MyService.class)) {
 *     // Service is available
 * }
 *
 * // Lazy registration
 * services.registerLazy(ExpensiveService.class, () -> new ExpensiveServiceImpl());
 *
 * // Unregister a service
 * services.unregister(MyService.class, myPlugin);
 * }</pre>
 *
 * <h2>Priority System</h2>
 * <p>When multiple implementations of a service are registered, the one with
 * the highest priority is returned. This allows plugins to provide fallback
 * implementations that can be overridden.
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Service
 * @see ServiceProvider
 */
public interface ServiceRegistry {

    /**
     * Registers a service implementation.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param provider    the plugin providing this service
     * @param service     the service implementation
     * @param priority    the service priority
     * @throws IllegalArgumentException if serviceType is null or not an interface
     * @since 1.0.0
     */
    <T extends Service> void register(
            @NotNull Class<T> serviceType,
            @NotNull Object provider,
            @NotNull T service,
            @NotNull ServicePriority priority
    );

    /**
     * Registers a service with normal priority.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param provider    the plugin providing this service
     * @param service     the service implementation
     * @since 1.0.0
     */
    default <T extends Service> void register(
            @NotNull Class<T> serviceType,
            @NotNull Object provider,
            @NotNull T service
    ) {
        register(serviceType, provider, service, ServicePriority.NORMAL);
    }

    /**
     * Registers a service with the registry itself as the provider.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param service     the service implementation
     * @since 1.0.0
     */
    <T extends Service> void register(@NotNull Class<T> serviceType, @NotNull T service);

    /**
     * Registers a lazily-initialized service.
     *
     * <p>The supplier is only called when the service is first requested.
     * This is useful for expensive services that may not always be needed.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param provider    the plugin providing this service
     * @param supplier    the supplier that creates the service
     * @param priority    the service priority
     * @since 1.0.0
     */
    <T extends Service> void registerLazy(
            @NotNull Class<T> serviceType,
            @NotNull Object provider,
            @NotNull Supplier<T> supplier,
            @NotNull ServicePriority priority
    );

    /**
     * Registers a lazily-initialized service with normal priority.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param supplier    the supplier that creates the service
     * @since 1.0.0
     */
    default <T extends Service> void registerLazy(
            @NotNull Class<T> serviceType,
            @NotNull Supplier<T> supplier
    ) {
        registerLazy(serviceType, this, supplier, ServicePriority.NORMAL);
    }

    /**
     * Unregisters a service.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param provider    the plugin that registered the service
     * @return true if the service was unregistered
     * @since 1.0.0
     */
    <T extends Service> boolean unregister(@NotNull Class<T> serviceType, @NotNull Object provider);

    /**
     * Unregisters all services registered by a provider.
     *
     * @param provider the plugin to unregister services for
     * @return the number of services unregistered
     * @since 1.0.0
     */
    int unregisterAll(@NotNull Object provider);

    /**
     * Retrieves a service by its type.
     *
     * <p>If multiple implementations are registered, returns the one with
     * the highest priority.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return an Optional containing the service if registered
     * @since 1.0.0
     */
    @NotNull
    <T extends Service> Optional<T> get(@NotNull Class<T> serviceType);

    /**
     * Retrieves a service or throws if not registered.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return the service instance
     * @throws IllegalStateException if the service is not registered
     * @since 1.0.0
     */
    @NotNull
    default <T extends Service> T getOrThrow(@NotNull Class<T> serviceType) {
        return get(serviceType).orElseThrow(() ->
                new IllegalStateException("Service not registered: " + serviceType.getName())
        );
    }

    /**
     * Retrieves a service or returns a default value.
     *
     * @param <T>          the service type
     * @param serviceType  the service interface class
     * @param defaultValue the default value if not registered
     * @return the service or default value
     * @since 1.0.0
     */
    @NotNull
    default <T extends Service> T getOrDefault(@NotNull Class<T> serviceType, @NotNull T defaultValue) {
        return get(serviceType).orElse(defaultValue);
    }

    /**
     * Retrieves all registered implementations of a service.
     *
     * <p>Returns implementations in priority order (highest first).
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return a collection of all registered implementations
     * @since 1.0.0
     */
    @NotNull
    <T extends Service> Collection<T> getAll(@NotNull Class<T> serviceType);

    /**
     * Checks if a service is registered.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return true if at least one implementation is registered
     * @since 1.0.0
     */
    <T extends Service> boolean isRegistered(@NotNull Class<T> serviceType);

    /**
     * Returns the provider that registered a service.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return an Optional containing the provider if the service is registered
     * @since 1.0.0
     */
    @NotNull
    <T extends Service> Optional<Object> getProvider(@NotNull Class<T> serviceType);

    /**
     * Returns all registered service types.
     *
     * @return a collection of all registered service interface classes
     * @since 1.0.0
     */
    @NotNull
    Collection<Class<? extends Service>> getRegisteredServices();

    /**
     * Returns the number of registered services.
     *
     * @return the count of registered service types
     * @since 1.0.0
     */
    int size();

    /**
     * Checks if the registry is empty.
     *
     * @return true if no services are registered
     * @since 1.0.0
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Service priority levels for registration.
     *
     * @since 1.0.0
     */
    enum ServicePriority {
        /**
         * Lowest priority - fallback implementation.
         */
        LOWEST(0),

        /**
         * Low priority.
         */
        LOW(25),

        /**
         * Normal/default priority.
         */
        NORMAL(50),

        /**
         * High priority.
         */
        HIGH(75),

        /**
         * Highest priority - should override all others.
         */
        HIGHEST(100);

        private final int value;

        ServicePriority(int value) {
            this.value = value;
        }

        /**
         * Returns the numeric priority value.
         *
         * @return the priority value (0-100)
         * @since 1.0.0
         */
        public int getValue() {
            return value;
        }
    }
}
