/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import sh.pcx.unified.data.redis.RedisConfig;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

/**
 * Provider for Lettuce Redis client instances.
 *
 * <p>This class manages the creation and lifecycle of Lettuce connections,
 * providing both synchronous and asynchronous command access.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Native async support with CompletableFuture</li>
 *   <li>Thread-safe connection sharing</li>
 *   <li>Reactive streams support</li>
 *   <li>Automatic reconnection</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a provider
 * LettuceProvider provider = new LettuceProvider(config);
 *
 * // Synchronous operations
 * RedisCommands<String, String> sync = provider.sync();
 * sync.set("key", "value");
 * String value = sync.get("key");
 *
 * // Asynchronous operations
 * RedisAsyncCommands<String, String> async = provider.async();
 * async.set("key", "value")
 *     .thenCompose(v -> async.get("key"))
 *     .thenAccept(v -> System.out.println("Value: " + v));
 *
 * // Shutdown when done
 * provider.close();
 * }</pre>
 *
 * <h2>Connection Management</h2>
 * <p>Unlike Jedis, Lettuce connections are thread-safe and can be shared
 * across multiple threads. A single connection is typically sufficient
 * for most applications.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LettuceConnectionManager
 * @see LettuceRedisService
 */
public final class LettuceProvider implements AutoCloseable {

    private final RedisConfig config;
    private final ClientResources clientResources;
    private final RedisClient redisClient;
    private final LettuceConnectionManager connectionManager;

    /**
     * Creates a new Lettuce provider with the given configuration.
     *
     * @param config the Redis configuration
     * @throws NullPointerException if config is null
     * @since 1.0.0
     */
    public LettuceProvider(@NotNull RedisConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");

        // Create client resources (thread pools, event loops)
        this.clientResources = DefaultClientResources.builder()
                .ioThreadPoolSize(config.pool().maxTotal())
                .computationThreadPoolSize(config.pool().maxTotal())
                .build();

        // Build Redis URI
        RedisURI redisURI = buildRedisURI(config);

        // Create the client
        this.redisClient = RedisClient.create(clientResources, redisURI);

        // Note: timeout is already configured via RedisURI.withTimeout()

        // Create connection manager
        this.connectionManager = new LettuceConnectionManager(redisClient, config);
    }

    /**
     * Builds a RedisURI from the configuration.
     *
     * @param config the Redis configuration
     * @return the RedisURI
     */
    private RedisURI buildRedisURI(RedisConfig config) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(config.host())
                .withPort(config.port())
                .withDatabase(config.database())
                .withTimeout(config.timeout())
                .withSsl(config.ssl());

        if (config.password() != null && !config.password().isEmpty()) {
            builder.withPassword(config.password().toCharArray());
        }

        if (config.clientName() != null && !config.clientName().isEmpty()) {
            builder.withClientName(config.clientName());
        }

        return builder.build();
    }

    /**
     * Gets the Redis configuration.
     *
     * @return the configuration
     * @since 1.0.0
     */
    @NotNull
    public RedisConfig getConfig() {
        return config;
    }

    /**
     * Gets the underlying Lettuce client.
     *
     * @return the RedisClient
     * @since 1.0.0
     */
    @NotNull
    public RedisClient getClient() {
        return redisClient;
    }

    /**
     * Gets the connection manager.
     *
     * @return the connection manager
     * @since 1.0.0
     */
    @NotNull
    public LettuceConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Gets a stateful Redis connection.
     *
     * <p>This connection is shared and thread-safe. Do not close it directly;
     * use {@link #close()} to shut down the provider.
     *
     * @return the connection
     * @since 1.0.0
     */
    @NotNull
    public StatefulRedisConnection<String, String> getConnection() {
        return connectionManager.getConnection();
    }

    /**
     * Gets a pub/sub connection.
     *
     * @return the pub/sub connection
     * @since 1.0.0
     */
    @NotNull
    public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        return connectionManager.getPubSubConnection();
    }

    /**
     * Gets synchronous Redis commands.
     *
     * <p>The returned object is thread-safe.
     *
     * @return the synchronous commands
     * @since 1.0.0
     */
    @NotNull
    public RedisCommands<String, String> sync() {
        return getConnection().sync();
    }

    /**
     * Gets asynchronous Redis commands.
     *
     * <p>The returned object is thread-safe.
     *
     * @return the asynchronous commands
     * @since 1.0.0
     */
    @NotNull
    public RedisAsyncCommands<String, String> async() {
        return getConnection().async();
    }

    /**
     * Checks if the connection is healthy.
     *
     * @return true if a ping succeeds
     * @since 1.0.0
     */
    public boolean isHealthy() {
        try {
            return "PONG".equals(sync().ping());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if this provider is closed.
     *
     * @return true if closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        return connectionManager.isClosed();
    }

    /**
     * Closes this provider and releases all resources.
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        connectionManager.close();
        redisClient.shutdown();
        clientResources.shutdown();
    }

    @Override
    public String toString() {
        return "LettuceProvider[host=" + config.host() + ":" + config.port() +
               ", healthy=" + isHealthy() + "]";
    }
}
