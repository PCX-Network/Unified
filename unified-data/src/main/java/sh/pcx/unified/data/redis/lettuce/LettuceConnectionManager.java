/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import sh.pcx.unified.data.redis.RedisConfig;
import sh.pcx.unified.data.redis.RedisService.PoolStats;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager for Lettuce Redis connections.
 *
 * <p>Unlike Jedis, Lettuce connections are thread-safe and can be shared
 * across multiple threads. This manager maintains a single connection
 * (or pool of connections for high-throughput scenarios) and handles
 * lifecycle management.
 *
 * <h2>Connection Model</h2>
 * <p>Lettuce uses a different connection model than Jedis:
 * <ul>
 *   <li>Connections are multiplexed across threads</li>
 *   <li>A single connection can handle multiple concurrent operations</li>
 *   <li>Automatic reconnection on connection loss</li>
 *   <li>Pipelining is implicit in async operations</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create connection manager
 * LettuceConnectionManager manager = new LettuceConnectionManager(client, config);
 *
 * // Get the shared connection
 * StatefulRedisConnection<String, String> connection = manager.getConnection();
 *
 * // Use sync commands
 * connection.sync().set("key", "value");
 *
 * // Use async commands
 * connection.async().get("key")
 *     .thenAccept(value -> System.out.println(value));
 *
 * // Get pub/sub connection
 * StatefulRedisPubSubConnection<String, String> pubsub = manager.getPubSubConnection();
 *
 * // Shutdown
 * manager.close();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LettuceProvider
 */
public final class LettuceConnectionManager implements AutoCloseable {

    private final RedisClient redisClient;
    private final RedisConfig config;

    private volatile StatefulRedisConnection<String, String> connection;
    private volatile StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private volatile boolean closed = false;

    // Statistics
    private final AtomicLong commandCount = new AtomicLong();
    private final AtomicLong errorCount = new AtomicLong();

    /**
     * Creates a new connection manager.
     *
     * @param redisClient the Lettuce Redis client
     * @param config      the Redis configuration
     * @since 1.0.0
     */
    public LettuceConnectionManager(@NotNull RedisClient redisClient, @NotNull RedisConfig config) {
        this.redisClient = Objects.requireNonNull(redisClient, "redisClient cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    /**
     * Gets the shared Redis connection.
     *
     * <p>The connection is lazily created on first access. The same connection
     * is returned for all subsequent calls.
     *
     * @return the stateful Redis connection
     * @throws IllegalStateException if the manager is closed
     * @since 1.0.0
     */
    @NotNull
    public StatefulRedisConnection<String, String> getConnection() {
        if (closed) {
            throw new IllegalStateException("Connection manager is closed");
        }

        StatefulRedisConnection<String, String> conn = connection;
        if (conn == null || !conn.isOpen()) {
            synchronized (this) {
                conn = connection;
                if (conn == null || !conn.isOpen()) {
                    conn = redisClient.connect();
                    connection = conn;
                }
            }
        }
        return conn;
    }

    /**
     * Gets a pub/sub connection.
     *
     * <p>The pub/sub connection is separate from the main connection because
     * pub/sub commands put the connection in a special mode.
     *
     * @return the pub/sub connection
     * @throws IllegalStateException if the manager is closed
     * @since 1.0.0
     */
    @NotNull
    public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        if (closed) {
            throw new IllegalStateException("Connection manager is closed");
        }

        StatefulRedisPubSubConnection<String, String> conn = pubSubConnection;
        if (conn == null || !conn.isOpen()) {
            synchronized (this) {
                conn = pubSubConnection;
                if (conn == null || !conn.isOpen()) {
                    conn = redisClient.connectPubSub();
                    pubSubConnection = conn;
                }
            }
        }
        return conn;
    }

    /**
     * Gets connection statistics.
     *
     * <p>Note: Lettuce doesn't use traditional connection pooling, so some
     * stats may not be applicable.
     *
     * @return the pool statistics
     * @since 1.0.0
     */
    @NotNull
    public PoolStats getStats() {
        int active = (connection != null && connection.isOpen()) ? 1 : 0;
        int pubsubActive = (pubSubConnection != null && pubSubConnection.isOpen()) ? 1 : 0;

        return new PoolStats(
                active + pubsubActive,  // active
                0,                       // idle (not applicable)
                active + pubsubActive,  // total
                commandCount.get(),     // borrowed (used as command count)
                commandCount.get(),     // returned (same as commands)
                active + pubsubActive,  // created
                0                        // destroyed
        );
    }

    /**
     * Records a command execution.
     *
     * @since 1.0.0
     */
    public void recordCommand() {
        commandCount.incrementAndGet();
    }

    /**
     * Records an error.
     *
     * @since 1.0.0
     */
    public void recordError() {
        errorCount.incrementAndGet();
    }

    /**
     * Gets the total command count.
     *
     * @return the number of commands executed
     * @since 1.0.0
     */
    public long getCommandCount() {
        return commandCount.get();
    }

    /**
     * Gets the error count.
     *
     * @return the number of errors
     * @since 1.0.0
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /**
     * Checks if the main connection is open.
     *
     * @return true if connected
     * @since 1.0.0
     */
    public boolean isConnected() {
        StatefulRedisConnection<String, String> conn = connection;
        return conn != null && conn.isOpen();
    }

    /**
     * Checks if this manager is closed.
     *
     * @return true if closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Resets the connections.
     *
     * <p>This closes existing connections, which will be recreated on next access.
     *
     * @since 1.0.0
     */
    public void reset() {
        synchronized (this) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
                connection = null;
            }
            if (pubSubConnection != null) {
                try {
                    pubSubConnection.close();
                } catch (Exception ignored) {
                }
                pubSubConnection = null;
            }
        }
    }

    /**
     * Closes all connections and marks the manager as closed.
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            synchronized (this) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception ignored) {
                    }
                }
                if (pubSubConnection != null) {
                    try {
                        pubSubConnection.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "LettuceConnectionManager[" +
               "connected=" + isConnected() +
               ", commands=" + getCommandCount() +
               ", errors=" + getErrorCount() +
               ", closed=" + closed +
               "]";
    }
}
