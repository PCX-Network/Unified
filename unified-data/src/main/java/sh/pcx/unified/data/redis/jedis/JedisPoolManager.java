/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.jedis;

import sh.pcx.unified.data.redis.RedisConfig;
import sh.pcx.unified.data.redis.RedisService.PoolStats;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager for Jedis connection pool.
 *
 * <p>This class handles the creation and configuration of a {@link JedisPool},
 * applying settings from {@link RedisConfig} and tracking pool statistics.
 *
 * <h2>Pool Configuration</h2>
 * <p>The pool is configured based on the {@link RedisConfig.PoolConfig} settings:
 * <ul>
 *   <li><strong>maxTotal</strong> - Maximum total connections</li>
 *   <li><strong>maxIdle</strong> - Maximum idle connections</li>
 *   <li><strong>minIdle</strong> - Minimum idle connections</li>
 *   <li><strong>maxWait</strong> - Maximum wait time for a connection</li>
 *   <li><strong>testOnBorrow</strong> - Validate connections when borrowed</li>
 *   <li><strong>testWhileIdle</strong> - Validate idle connections</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create pool manager
 * JedisPoolManager poolManager = new JedisPoolManager(config);
 *
 * // Get a connection
 * try (Jedis jedis = poolManager.getPool().getResource()) {
 *     jedis.set("key", "value");
 * }
 *
 * // Check statistics
 * PoolStats stats = poolManager.getStats();
 * System.out.println("Active: " + stats.active());
 * System.out.println("Idle: " + stats.idle());
 *
 * // Shutdown
 * poolManager.close();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see JedisPool
 * @see JedisProvider
 */
public final class JedisPoolManager implements AutoCloseable {

    private final RedisConfig config;
    private final JedisPool pool;
    private volatile boolean closed = false;

    // Statistics counters
    private final AtomicLong borrowedCount = new AtomicLong();
    private final AtomicLong returnedCount = new AtomicLong();
    private final AtomicLong createdCount = new AtomicLong();
    private final AtomicLong destroyedCount = new AtomicLong();

    /**
     * Creates a new pool manager with the given configuration.
     *
     * @param config the Redis configuration
     * @throws NullPointerException if config is null
     * @since 1.0.0
     */
    public JedisPoolManager(@NotNull RedisConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.pool = createPool(config);
    }

    /**
     * Creates a JedisPool from the configuration.
     *
     * @param config the Redis configuration
     * @return the configured JedisPool
     */
    private JedisPool createPool(RedisConfig config) {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();

        // Apply pool settings
        RedisConfig.PoolConfig pc = config.pool();
        poolConfig.setMaxTotal(pc.maxTotal());
        poolConfig.setMaxIdle(pc.maxIdle());
        poolConfig.setMinIdle(pc.minIdle());
        poolConfig.setMaxWait(pc.maxWait());
        poolConfig.setTestOnBorrow(pc.testOnBorrow());
        poolConfig.setTestOnReturn(pc.testOnReturn());
        poolConfig.setTestWhileIdle(pc.testWhileIdle());
        poolConfig.setTimeBetweenEvictionRuns(pc.timeBetweenEvictionRuns());

        // Block when exhausted
        poolConfig.setBlockWhenExhausted(true);

        // Enable JMX for monitoring (optional)
        poolConfig.setJmxEnabled(true);
        poolConfig.setJmxNamePrefix("jedis-pool");

        // Create the pool
        return new JedisPool(
                poolConfig,
                config.host(),
                config.port(),
                (int) config.timeout().toMillis(),
                config.password(),
                config.database(),
                config.clientName(),
                config.ssl()
        );
    }

    /**
     * Gets the underlying JedisPool.
     *
     * @return the JedisPool
     * @throws IllegalStateException if the pool is closed
     * @since 1.0.0
     */
    @NotNull
    public JedisPool getPool() {
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }
        return pool;
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
     * Gets pool statistics.
     *
     * @return the current pool statistics
     * @since 1.0.0
     */
    @NotNull
    public PoolStats getStats() {
        return new PoolStats(
                pool.getNumActive(),
                pool.getNumIdle(),
                pool.getNumActive() + pool.getNumIdle(),
                borrowedCount.get(),
                returnedCount.get(),
                createdCount.get(),
                destroyedCount.get()
        );
    }

    /**
     * Increments the borrowed counter.
     *
     * <p>Called internally when a connection is borrowed.
     *
     * @since 1.0.0
     */
    public void recordBorrowed() {
        borrowedCount.incrementAndGet();
    }

    /**
     * Increments the returned counter.
     *
     * <p>Called internally when a connection is returned.
     *
     * @since 1.0.0
     */
    public void recordReturned() {
        returnedCount.incrementAndGet();
    }

    /**
     * Increments the created counter.
     *
     * <p>Called internally when a connection is created.
     *
     * @since 1.0.0
     */
    public void recordCreated() {
        createdCount.incrementAndGet();
    }

    /**
     * Increments the destroyed counter.
     *
     * <p>Called internally when a connection is destroyed.
     *
     * @since 1.0.0
     */
    public void recordDestroyed() {
        destroyedCount.incrementAndGet();
    }

    /**
     * Gets the number of active connections.
     *
     * @return the number of borrowed connections
     * @since 1.0.0
     */
    public int getActiveCount() {
        return pool.getNumActive();
    }

    /**
     * Gets the number of idle connections.
     *
     * @return the number of idle connections
     * @since 1.0.0
     */
    public int getIdleCount() {
        return pool.getNumIdle();
    }

    /**
     * Gets the maximum pool size.
     *
     * @return the maximum total connections
     * @since 1.0.0
     */
    public int getMaxTotal() {
        return config.pool().maxTotal();
    }

    /**
     * Checks if the pool is closed.
     *
     * @return true if closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes the pool and releases all resources.
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            pool.close();
        }
    }

    /**
     * Clears idle connections from the pool.
     *
     * <p>This can be useful to force reconnection after configuration changes.
     *
     * @since 1.0.0
     */
    public void clearIdleConnections() {
        pool.clear();
    }

    @Override
    public String toString() {
        return "JedisPoolManager[" +
               "host=" + config.host() + ":" + config.port() +
               ", active=" + getActiveCount() +
               ", idle=" + getIdleCount() +
               ", maxTotal=" + getMaxTotal() +
               ", closed=" + closed +
               "]";
    }
}
