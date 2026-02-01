/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages MongoDB client lifecycle and provides connection management.
 *
 * <p>This class handles the creation, caching, and cleanup of MongoDB clients.
 * It supports multiple named clients for connecting to different MongoDB
 * clusters or with different configurations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create manager with default client
 * MongoConfig config = new MongoConfig("mongodb://localhost:27017", "myDatabase");
 * MongoClientManager manager = new MongoClientManager(config);
 *
 * // Get the default client
 * MongoClient client = manager.getClient();
 *
 * // Register additional clients
 * MongoConfig analyticsConfig = new MongoConfig("mongodb://analytics:27017", "analytics");
 * manager.registerClient("analytics", analyticsConfig);
 *
 * // Get a named client
 * MongoClient analyticsClient = manager.getClient("analytics").orElseThrow();
 *
 * // Close all clients when done
 * manager.close();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All client operations are synchronized
 * using concurrent data structures.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoClientProvider
 * @see MongoConfig
 */
public class MongoClientManager implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientManager.class);

    /**
     * The default client name.
     */
    public static final String DEFAULT_CLIENT = "default";

    private final Map<String, ManagedClient> clients = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final CodecRegistry codecRegistry;

    /**
     * Creates a new client manager without a default client.
     *
     * @since 1.0.0
     */
    public MongoClientManager() {
        this((CodecRegistry) null);
    }

    /**
     * Creates a new client manager with a custom codec registry.
     *
     * @param codecRegistry the codec registry to use for all clients (null for default)
     * @since 1.0.0
     */
    public MongoClientManager(@Nullable CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    /**
     * Creates a new client manager with a default client.
     *
     * @param defaultConfig the default client configuration
     * @since 1.0.0
     */
    public MongoClientManager(@NotNull MongoConfig defaultConfig) {
        this(defaultConfig, null);
    }

    /**
     * Creates a new client manager with a default client and custom codec registry.
     *
     * @param defaultConfig the default client configuration
     * @param codecRegistry the codec registry to use for all clients (null for default)
     * @since 1.0.0
     */
    public MongoClientManager(@NotNull MongoConfig defaultConfig, @Nullable CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
        registerClient(DEFAULT_CLIENT, defaultConfig);
    }

    /**
     * Registers a new MongoDB client.
     *
     * @param name   the client name
     * @param config the client configuration
     * @return this manager for chaining
     * @throws IllegalStateException if the manager is closed
     * @throws IllegalArgumentException if a client with the name already exists
     * @since 1.0.0
     */
    @NotNull
    public MongoClientManager registerClient(@NotNull String name, @NotNull MongoConfig config) {
        ensureNotClosed();
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");

        if (clients.containsKey(name)) {
            throw new IllegalArgumentException("Client with name '" + name + "' already exists");
        }

        MongoClient client = codecRegistry != null
                ? MongoClientProvider.createClient(config, codecRegistry)
                : MongoClientProvider.createClient(config);

        clients.put(name, new ManagedClient(client, config));
        LOGGER.info("Registered MongoDB client '{}' for database '{}'", name, config.database());

        return this;
    }

    /**
     * Registers a new MongoDB client with POJO support.
     *
     * @param name   the client name
     * @param config the client configuration
     * @return this manager for chaining
     * @throws IllegalStateException if the manager is closed
     * @throws IllegalArgumentException if a client with the name already exists
     * @since 1.0.0
     */
    @NotNull
    public MongoClientManager registerClientWithPojoSupport(
            @NotNull String name,
            @NotNull MongoConfig config
    ) {
        ensureNotClosed();
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");

        if (clients.containsKey(name)) {
            throw new IllegalArgumentException("Client with name '" + name + "' already exists");
        }

        MongoClient client = MongoClientProvider.createClientWithPojoSupport(config, codecRegistry);
        clients.put(name, new ManagedClient(client, config));
        LOGGER.info("Registered MongoDB client '{}' with POJO support for database '{}'",
                name, config.database());

        return this;
    }

    /**
     * Registers a pre-existing MongoDB client.
     *
     * <p>Use this method when you need full control over client creation.
     * The client will be closed when the manager is closed.
     *
     * @param name   the client name
     * @param client the MongoDB client
     * @param config the client configuration (for reference)
     * @return this manager for chaining
     * @throws IllegalStateException if the manager is closed
     * @throws IllegalArgumentException if a client with the name already exists
     * @since 1.0.0
     */
    @NotNull
    public MongoClientManager registerClient(
            @NotNull String name,
            @NotNull MongoClient client,
            @NotNull MongoConfig config
    ) {
        ensureNotClosed();
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(client, "Client cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");

        if (clients.containsKey(name)) {
            throw new IllegalArgumentException("Client with name '" + name + "' already exists");
        }

        clients.put(name, new ManagedClient(client, config));
        LOGGER.info("Registered external MongoDB client '{}' for database '{}'",
                name, config.database());

        return this;
    }

    /**
     * Returns the default MongoDB client.
     *
     * @return the default client
     * @throws IllegalStateException if no default client is registered or manager is closed
     * @since 1.0.0
     */
    @NotNull
    public MongoClient getClient() {
        return getClient(DEFAULT_CLIENT)
                .orElseThrow(() -> new IllegalStateException("No default client registered"));
    }

    /**
     * Returns a named MongoDB client.
     *
     * @param name the client name
     * @return an Optional containing the client if found
     * @throws IllegalStateException if the manager is closed
     * @since 1.0.0
     */
    @NotNull
    public Optional<MongoClient> getClient(@NotNull String name) {
        ensureNotClosed();
        Objects.requireNonNull(name, "Name cannot be null");
        ManagedClient managed = clients.get(name);
        return managed != null ? Optional.of(managed.client()) : Optional.empty();
    }

    /**
     * Returns the configuration for a named client.
     *
     * @param name the client name
     * @return an Optional containing the config if found
     * @since 1.0.0
     */
    @NotNull
    public Optional<MongoConfig> getConfig(@NotNull String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        ManagedClient managed = clients.get(name);
        return managed != null ? Optional.of(managed.config()) : Optional.empty();
    }

    /**
     * Returns the default client configuration.
     *
     * @return an Optional containing the default config if registered
     * @since 1.0.0
     */
    @NotNull
    public Optional<MongoConfig> getDefaultConfig() {
        return getConfig(DEFAULT_CLIENT);
    }

    /**
     * Checks if a client with the given name is registered.
     *
     * @param name the client name
     * @return true if a client with the name exists
     * @since 1.0.0
     */
    public boolean hasClient(@NotNull String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        return clients.containsKey(name);
    }

    /**
     * Checks if a default client is registered.
     *
     * @return true if a default client exists
     * @since 1.0.0
     */
    public boolean hasDefaultClient() {
        return hasClient(DEFAULT_CLIENT);
    }

    /**
     * Unregisters and closes a named client.
     *
     * @param name the client name
     * @return true if a client was removed
     * @since 1.0.0
     */
    public boolean unregisterClient(@NotNull String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        ManagedClient managed = clients.remove(name);
        if (managed != null) {
            try {
                managed.client().close();
                LOGGER.info("Unregistered and closed MongoDB client '{}'", name);
            } catch (Exception e) {
                LOGGER.error("Error closing MongoDB client '{}'", name, e);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the number of registered clients.
     *
     * @return the client count
     * @since 1.0.0
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Checks if the manager is closed.
     *
     * @return true if closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes all managed MongoDB clients.
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOGGER.info("Closing {} MongoDB client(s)", clients.size());

            for (Map.Entry<String, ManagedClient> entry : clients.entrySet()) {
                try {
                    entry.getValue().client().close();
                    LOGGER.debug("Closed MongoDB client '{}'", entry.getKey());
                } catch (Exception e) {
                    LOGGER.error("Error closing MongoDB client '{}'", entry.getKey(), e);
                }
            }

            clients.clear();
            LOGGER.info("All MongoDB clients closed");
        }
    }

    private void ensureNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("MongoClientManager is closed");
        }
    }

    /**
     * Internal record for managed client with its configuration.
     */
    private record ManagedClient(MongoClient client, MongoConfig config) {}
}
