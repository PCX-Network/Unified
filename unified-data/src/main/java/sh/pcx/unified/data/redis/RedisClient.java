/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

/**
 * Enumeration of supported Redis client implementations.
 *
 * <p>The UnifiedPlugin API supports two Redis client libraries:
 * <ul>
 *   <li><strong>JEDIS</strong> - Synchronous, simple, and widely used</li>
 *   <li><strong>LETTUCE</strong> - Asynchronous, reactive, and feature-rich</li>
 * </ul>
 *
 * <h2>Choosing a Client</h2>
 * <table>
 *   <tr><th>Feature</th><th>Jedis</th><th>Lettuce</th></tr>
 *   <tr><td>Sync Operations</td><td>Native</td><td>Supported</td></tr>
 *   <tr><td>Async Operations</td><td>Manual</td><td>Native</td></tr>
 *   <tr><td>Reactive Streams</td><td>No</td><td>Yes</td></tr>
 *   <tr><td>Thread Safety</td><td>Per-connection</td><td>Thread-safe</td></tr>
 *   <tr><td>Connection Pooling</td><td>Built-in</td><td>Built-in</td></tr>
 *   <tr><td>Cluster Support</td><td>Yes</td><td>Yes</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Configure Redis with Jedis
 * RedisConfig config = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .client(RedisClient.JEDIS)
 *     .build();
 *
 * // Configure Redis with Lettuce for async operations
 * RedisConfig asyncConfig = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .client(RedisClient.LETTUCE)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisConfig
 * @see RedisService
 */
public enum RedisClient {

    /**
     * Jedis client implementation.
     *
     * <p>Jedis is a synchronous Redis client that provides a simple, familiar API.
     * It uses connection pooling through JedisPool for thread-safe operations.
     *
     * <p>Best suited for:
     * <ul>
     *   <li>Simple synchronous operations</li>
     *   <li>Projects already using Jedis</li>
     *   <li>Quick prototyping</li>
     * </ul>
     *
     * @see <a href="https://github.com/redis/jedis">Jedis GitHub</a>
     */
    JEDIS("redis.clients.jedis.JedisPool", "7.2.0"),

    /**
     * Lettuce client implementation.
     *
     * <p>Lettuce is an advanced, thread-safe Redis client with native support
     * for asynchronous and reactive operations. Connections are multiplexed
     * across multiple threads.
     *
     * <p>Best suited for:
     * <ul>
     *   <li>High-throughput applications</li>
     *   <li>Asynchronous/reactive programming</li>
     *   <li>Complex Redis operations</li>
     * </ul>
     *
     * @see <a href="https://github.com/lettuce-io/lettuce-core">Lettuce GitHub</a>
     */
    LETTUCE("io.lettuce.core.RedisClient", "7.2.0");

    private final String mainClass;
    private final String recommendedVersion;

    /**
     * Creates a new Redis client enum value.
     *
     * @param mainClass          the fully qualified main class name
     * @param recommendedVersion the recommended library version
     */
    RedisClient(String mainClass, String recommendedVersion) {
        this.mainClass = mainClass;
        this.recommendedVersion = recommendedVersion;
    }

    /**
     * Returns the fully qualified class name of the client's main class.
     *
     * <p>This can be used to check if the library is available on the classpath.
     *
     * @return the main class name
     * @since 1.0.0
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Returns the recommended version of this client library.
     *
     * @return the recommended version string
     * @since 1.0.0
     */
    public String getRecommendedVersion() {
        return recommendedVersion;
    }

    /**
     * Checks if this client library is available on the classpath.
     *
     * @return true if the client library classes are available
     * @since 1.0.0
     */
    public boolean isAvailable() {
        try {
            Class.forName(mainClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Detects and returns an available Redis client.
     *
     * <p>Attempts to find an available client in the following order:
     * <ol>
     *   <li>LETTUCE (preferred for async support)</li>
     *   <li>JEDIS (fallback)</li>
     * </ol>
     *
     * @return the first available Redis client
     * @throws IllegalStateException if no Redis client is available
     * @since 1.0.0
     */
    public static RedisClient detectAvailable() {
        if (LETTUCE.isAvailable()) {
            return LETTUCE;
        }
        if (JEDIS.isAvailable()) {
            return JEDIS;
        }
        throw new IllegalStateException(
                "No Redis client library found. Please add Jedis or Lettuce to your dependencies."
        );
    }
}
