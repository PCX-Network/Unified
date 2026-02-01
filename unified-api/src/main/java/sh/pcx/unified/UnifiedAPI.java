/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.service.Service;
import sh.pcx.unified.service.ServiceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Static entry point for the UnifiedPlugin API.
 *
 * <p>This class provides convenient static access to core API components
 * including the server instance, player lookups, and service registry.
 * It acts as the primary gateway for plugins to interact with the
 * unified abstraction layer.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the API instance
 * UnifiedAPI api = UnifiedAPI.getInstance();
 *
 * // Get the server
 * UnifiedServer server = UnifiedAPI.getServer();
 *
 * // Get a player by UUID
 * Optional<UnifiedPlayer> player = UnifiedAPI.getPlayer(uuid);
 * player.ifPresent(p -> p.sendMessage(Component.text("Hello!")));
 *
 * // Get a player by name
 * Optional<UnifiedPlayer> player = UnifiedAPI.getPlayer("Steve");
 *
 * // Access services
 * Optional<MyService> service = api.getService(MyService.class);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The API instance is thread-safe and can be accessed from any thread.
 * However, certain operations returned by the API may have their own
 * threading requirements (e.g., player operations on the main thread).
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class UnifiedAPI {

    private static volatile UnifiedAPI instance;
    private static final Object LOCK = new Object();

    private final UnifiedServer server;
    private final ServiceRegistry serviceRegistry;

    /**
     * Private constructor to enforce singleton pattern.
     *
     * @param server          the unified server instance
     * @param serviceRegistry the service registry
     */
    private UnifiedAPI(@NotNull UnifiedServer server, @NotNull ServiceRegistry serviceRegistry) {
        this.server = server;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Returns the singleton instance of the UnifiedAPI.
     *
     * @return the API instance
     * @throws IllegalStateException if the API has not been initialized
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedAPI getInstance() {
        UnifiedAPI api = instance;
        if (api == null) {
            throw new IllegalStateException(
                    "UnifiedAPI has not been initialized. " +
                    "Ensure UnifiedPluginAPI is loaded as a dependency."
            );
        }
        return api;
    }

    /**
     * Checks if the API has been initialized.
     *
     * @return true if the API is ready to use
     * @since 1.0.0
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Returns the unified server instance.
     *
     * <p>This is a convenience method equivalent to {@code getInstance().server()}.
     *
     * @return the server instance
     * @throws IllegalStateException if the API has not been initialized
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedServer getServer() {
        return getInstance().server();
    }

    /**
     * Returns a player by their unique ID.
     *
     * <p>This is a convenience method equivalent to
     * {@code getServer().getPlayer(uuid)}.
     *
     * @param uuid the player's UUID
     * @return an Optional containing the player if online, or empty
     * @throws IllegalStateException if the API has not been initialized
     * @since 1.0.0
     */
    @NotNull
    public static Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid) {
        return getServer().getPlayer(uuid);
    }

    /**
     * Returns a player by their name.
     *
     * <p>This is a convenience method equivalent to
     * {@code getServer().getPlayer(name)}.
     *
     * <p><strong>Note:</strong> Player names are not unique and may change.
     * Prefer using {@link #getPlayer(UUID)} when possible.
     *
     * @param name the player's name (case-insensitive)
     * @return an Optional containing the player if online, or empty
     * @throws IllegalStateException if the API has not been initialized
     * @since 1.0.0
     */
    @NotNull
    public static Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        return getServer().getPlayer(name);
    }

    /**
     * Returns the server instance.
     *
     * @return the unified server instance
     * @since 1.0.0
     */
    @NotNull
    public UnifiedServer server() {
        return server;
    }

    /**
     * Returns the service registry.
     *
     * @return the service registry
     * @since 1.0.0
     */
    @NotNull
    public ServiceRegistry services() {
        return serviceRegistry;
    }

    /**
     * Retrieves a registered service by its type.
     *
     * @param <T>         the service type
     * @param serviceType the class of the service to retrieve
     * @return an Optional containing the service if registered, or empty
     * @since 1.0.0
     */
    @NotNull
    public <T extends Service> Optional<T> getService(@NotNull Class<T> serviceType) {
        return serviceRegistry.get(serviceType);
    }

    /**
     * Retrieves a registered service by its type, throwing if not found.
     *
     * @param <T>         the service type
     * @param serviceType the class of the service to retrieve
     * @return the service instance
     * @throws IllegalStateException if the service is not registered
     * @since 1.0.0
     */
    @NotNull
    public <T extends Service> T getServiceOrThrow(@NotNull Class<T> serviceType) {
        return getService(serviceType).orElseThrow(() ->
                new IllegalStateException("Service not registered: " + serviceType.getName())
        );
    }

    /**
     * Checks if a service is registered.
     *
     * @param <T>         the service type
     * @param serviceType the class of the service to check
     * @return true if the service is registered
     * @since 1.0.0
     */
    public <T extends Service> boolean hasService(@NotNull Class<T> serviceType) {
        return serviceRegistry.isRegistered(serviceType);
    }

    /**
     * Initializes the UnifiedAPI singleton.
     *
     * <p>This method is called by the platform implementation during startup
     * and should not be called by plugins directly.
     *
     * @param server          the unified server instance
     * @param serviceRegistry the service registry
     * @throws IllegalStateException if the API has already been initialized
     * @since 1.0.0
     */
    public static void initialize(@NotNull UnifiedServer server, @NotNull ServiceRegistry serviceRegistry) {
        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("UnifiedAPI has already been initialized");
            }
            instance = new UnifiedAPI(server, serviceRegistry);
        }
    }

    /**
     * Shuts down the UnifiedAPI singleton.
     *
     * <p>This method is called by the platform implementation during shutdown
     * and should not be called by plugins directly.
     *
     * @since 1.0.0
     */
    public static void shutdown() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
