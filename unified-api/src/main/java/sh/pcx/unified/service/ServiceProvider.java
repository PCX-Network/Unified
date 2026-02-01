/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.service;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * Service Provider Interface (SPI) for registering services with the UnifiedPlugin API.
 *
 * <p>ServiceProvider implementations are discovered via Java's {@link ServiceLoader}
 * mechanism and are responsible for providing service implementations to the
 * {@link ServiceRegistry}.
 *
 * <h2>Implementation Guidelines</h2>
 * <ol>
 *   <li>Create a class implementing this interface</li>
 *   <li>Register it in {@code META-INF/services/sh.pcx.unified.service.ServiceProvider}</li>
 *   <li>Implement the required methods to provide your services</li>
 * </ol>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class MyPluginServiceProvider implements ServiceProvider {
 *
 *     @Override
 *     public String getName() {
 *         return "MyPlugin Services";
 *     }
 *
 *     @Override
 *     public int getPriority() {
 *         return 50; // Normal priority
 *     }
 *
 *     @Override
 *     public Collection<ServiceDefinition<?>> getServices() {
 *         return List.of(
 *             ServiceDefinition.of(EconomyService.class, new VaultEconomyService()),
 *             ServiceDefinition.of(ChatService.class, new MyChatService())
 *         );
 *     }
 *
 *     @Override
 *     public void onRegister(ServiceRegistry registry) {
 *         // Additional setup after services are registered
 *     }
 *
 *     @Override
 *     public void onUnregister(ServiceRegistry registry) {
 *         // Cleanup when services are being removed
 *     }
 * }
 * }</pre>
 *
 * <h2>Discovery</h2>
 * <p>Providers are automatically discovered and loaded when the UnifiedPlugin API
 * initializes. The loading order is determined by {@link #getPriority()}.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Service
 * @see ServiceRegistry
 */
public interface ServiceProvider {

    /**
     * Loads all ServiceProvider implementations using ServiceLoader.
     *
     * @return an iterable of all discovered providers
     * @since 1.0.0
     */
    @NotNull
    static Iterable<ServiceProvider> loadAll() {
        return ServiceLoader.load(ServiceProvider.class);
    }

    /**
     * Returns the name of this service provider.
     *
     * <p>This is used for logging and debugging purposes.
     *
     * @return the provider name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the priority of this service provider.
     *
     * <p>Providers with higher priority are loaded first and their services
     * take precedence. Default priority is 50.
     *
     * @return the provider priority (0-100)
     * @since 1.0.0
     */
    default int getPriority() {
        return 50;
    }

    /**
     * Checks if this provider should be loaded.
     *
     * <p>Override this method to conditionally enable the provider based on
     * runtime conditions (e.g., checking if a dependency is available).
     *
     * @return true if the provider should be loaded
     * @since 1.0.0
     */
    default boolean shouldLoad() {
        return true;
    }

    /**
     * Returns the services this provider offers.
     *
     * @return a collection of service definitions
     * @since 1.0.0
     */
    @NotNull
    Collection<ServiceDefinition<?>> getServices();

    /**
     * Called after all services from this provider have been registered.
     *
     * <p>Use this for any post-registration setup.
     *
     * @param registry the service registry
     * @since 1.0.0
     */
    default void onRegister(@NotNull ServiceRegistry registry) {
        // Default implementation does nothing
    }

    /**
     * Called before services from this provider are unregistered.
     *
     * <p>Use this for cleanup before services are removed.
     *
     * @param registry the service registry
     * @since 1.0.0
     */
    default void onUnregister(@NotNull ServiceRegistry registry) {
        // Default implementation does nothing
    }

    /**
     * Represents a service definition with its type, implementation, and priority.
     *
     * @param <T> the service type
     * @since 1.0.0
     */
    record ServiceDefinition<T extends Service>(
            @NotNull Class<T> serviceType,
            @NotNull T implementation,
            @NotNull ServiceRegistry.ServicePriority priority
    ) {

        /**
         * Creates a service definition with normal priority.
         *
         * @param <T>            the service type
         * @param serviceType    the service interface class
         * @param implementation the service implementation
         * @return a new service definition
         * @since 1.0.0
         */
        @NotNull
        public static <T extends Service> ServiceDefinition<T> of(
                @NotNull Class<T> serviceType,
                @NotNull T implementation
        ) {
            return new ServiceDefinition<>(serviceType, implementation, ServiceRegistry.ServicePriority.NORMAL);
        }

        /**
         * Creates a service definition with specified priority.
         *
         * @param <T>            the service type
         * @param serviceType    the service interface class
         * @param implementation the service implementation
         * @param priority       the service priority
         * @return a new service definition
         * @since 1.0.0
         */
        @NotNull
        public static <T extends Service> ServiceDefinition<T> of(
                @NotNull Class<T> serviceType,
                @NotNull T implementation,
                @NotNull ServiceRegistry.ServicePriority priority
        ) {
            return new ServiceDefinition<>(serviceType, implementation, priority);
        }
    }
}
