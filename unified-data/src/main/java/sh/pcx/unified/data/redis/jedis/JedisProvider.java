/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.jedis;

import sh.pcx.unified.data.redis.RedisConfig;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Objects;

/**
 * Provider for Jedis client instances.
 *
 * <p>This class manages the creation and lifecycle of Jedis connections,
 * integrating with the {@link JedisPoolManager} for connection pooling.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a provider
 * JedisProvider provider = new JedisProvider(config);
 *
 * // Get a connection from the pool
 * try (Jedis jedis = provider.getResource()) {
 *     jedis.set("key", "value");
 *     String value = jedis.get("key");
 * }
 *
 * // Shutdown when done
 * provider.close();
 * }</pre>
 *
 * <h2>Connection Management</h2>
 * <p>Connections obtained via {@link #getResource()} should always be closed
 * after use. Using try-with-resources ensures proper cleanup:
 * <pre>{@code
 * try (Jedis jedis = provider.getResource()) {
 *     // Use jedis
 * } // Automatically returned to pool
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see JedisPoolManager
 * @see JedisRedisService
 */
public final class JedisProvider implements AutoCloseable {

    private final RedisConfig config;
    private final JedisPoolManager poolManager;

    /**
     * Creates a new Jedis provider with the given configuration.
     *
     * @param config the Redis configuration
     * @throws NullPointerException if config is null
     * @since 1.0.0
     */
    public JedisProvider(@NotNull RedisConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.poolManager = new JedisPoolManager(config);
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
     * Gets the underlying Jedis pool.
     *
     * @return the JedisPool
     * @since 1.0.0
     */
    @NotNull
    public JedisPool getPool() {
        return poolManager.getPool();
    }

    /**
     * Gets a Jedis connection from the pool.
     *
     * <p>The returned connection should be closed after use to return it
     * to the pool. Use try-with-resources for automatic cleanup.
     *
     * @return a Jedis connection
     * @throws redis.clients.jedis.exceptions.JedisException if connection fails
     * @since 1.0.0
     */
    @NotNull
    public Jedis getResource() {
        return poolManager.getPool().getResource();
    }

    /**
     * Gets the pool manager.
     *
     * @return the pool manager
     * @since 1.0.0
     */
    @NotNull
    public JedisPoolManager getPoolManager() {
        return poolManager;
    }

    /**
     * Checks if the connection is healthy.
     *
     * @return true if a connection can be established and responds to ping
     * @since 1.0.0
     */
    public boolean isHealthy() {
        try (Jedis jedis = getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the number of active connections.
     *
     * @return the number of active (borrowed) connections
     * @since 1.0.0
     */
    public int getActiveConnections() {
        return poolManager.getPool().getNumActive();
    }

    /**
     * Gets the number of idle connections.
     *
     * @return the number of idle connections in the pool
     * @since 1.0.0
     */
    public int getIdleConnections() {
        return poolManager.getPool().getNumIdle();
    }

    /**
     * Closes this provider and releases all connections.
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        poolManager.close();
    }

    /**
     * Checks if this provider is closed.
     *
     * @return true if closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        return poolManager.isClosed();
    }

    @Override
    public String toString() {
        return "JedisProvider[host=" + config.host() + ":" + config.port() +
               ", active=" + getActiveConnections() +
               ", idle=" + getIdleConnections() + "]";
    }
}
