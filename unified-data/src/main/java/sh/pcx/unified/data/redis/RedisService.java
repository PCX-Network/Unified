/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Main interface for Redis operations in the UnifiedPlugin API.
 *
 * <p>This service provides a unified API for Redis operations, supporting both
 * Jedis and Lettuce client implementations. It offers synchronous, asynchronous,
 * and reactive operations with built-in connection pooling.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Dual client support (Jedis/Lettuce)</li>
 *   <li>Connection pooling with configurable limits</li>
 *   <li>Key namespacing to prevent collisions</li>
 *   <li>Pub/Sub messaging</li>
 *   <li>Lua script execution</li>
 *   <li>TTL management utilities</li>
 *   <li>Both sync and async operations</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the Redis service
 * RedisService redis = api.getService(RedisService.class);
 *
 * // Simple string operations
 * redis.set("player:uuid:name", "Steve");
 * Optional<String> name = redis.get("player:uuid:name");
 *
 * // With expiration
 * redis.setex("session:token", sessionData, Duration.ofHours(1));
 *
 * // Async operations
 * redis.getAsync("key")
 *     .thenAccept(value -> System.out.println("Value: " + value));
 *
 * // Using connections directly
 * try (RedisConnection conn = redis.getConnection()) {
 *     conn.multi()
 *         .set("key1", "value1")
 *         .set("key2", "value2")
 *         .incr("counter")
 *         .exec();
 * }
 *
 * // Pub/Sub
 * redis.subscribe("chat", message -> {
 *     System.out.println("Received: " + message);
 * });
 * redis.publish("chat", "Hello, world!");
 *
 * // With key namespace
 * KeyNamespace ns = redis.namespace("myplugin");
 * ns.set("data", "value");  // Actually stores "myplugin:data"
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisConfig
 * @see RedisConnection
 * @see KeyNamespace
 */
public interface RedisService extends Service {

    // ========== Connection Management ==========

    /**
     * Gets the current Redis configuration.
     *
     * @return the Redis configuration
     * @since 1.0.0
     */
    @NotNull
    RedisConfig getConfig();

    /**
     * Gets a connection from the pool.
     *
     * <p>The returned connection should be closed after use to return it
     * to the pool. Use try-with-resources for automatic cleanup.
     *
     * @return a Redis connection
     * @throws RedisConnection.RedisException if no connection is available
     * @since 1.0.0
     */
    @NotNull
    RedisConnection getConnection();

    /**
     * Executes an operation with a connection from the pool.
     *
     * <p>The connection is automatically returned to the pool after the
     * operation completes.
     *
     * @param <T>      the return type
     * @param function the function to execute
     * @return the result of the function
     * @throws RedisConnection.RedisException if the operation fails
     * @since 1.0.0
     */
    <T> T withConnection(@NotNull Function<RedisConnection, T> function);

    /**
     * Executes an operation with a connection asynchronously.
     *
     * @param <T>      the return type
     * @param function the function to execute
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<T> withConnectionAsync(@NotNull Function<RedisConnection, T> function);

    /**
     * Checks if the Redis connection is healthy.
     *
     * @return true if the connection is healthy
     * @since 1.0.0
     */
    boolean isHealthy();

    /**
     * Checks Redis health asynchronously.
     *
     * @return a future that completes with the health status
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> isHealthyAsync();

    /**
     * Gets pool statistics.
     *
     * @return the pool statistics
     * @since 1.0.0
     */
    @NotNull
    PoolStats getPoolStats();

    // ========== Key Namespacing ==========

    /**
     * Creates a key namespace for prefixing keys.
     *
     * <p>Key namespacing helps prevent key collisions between different
     * plugins or components sharing the same Redis instance.
     *
     * @param prefix the namespace prefix
     * @return a KeyNamespace instance
     * @since 1.0.0
     */
    @NotNull
    KeyNamespace namespace(@NotNull String prefix);

    /**
     * Creates a nested key namespace.
     *
     * @param prefixes the namespace prefixes (joined with ":")
     * @return a KeyNamespace instance
     * @since 1.0.0
     */
    @NotNull
    KeyNamespace namespace(@NotNull String... prefixes);

    // ========== String Operations ==========

    /**
     * Gets the value of a key.
     *
     * @param key the key
     * @return an Optional containing the value
     * @since 1.0.0
     */
    @NotNull
    Optional<String> get(@NotNull String key);

    /**
     * Gets the value of a key asynchronously.
     *
     * @param key the key
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> getAsync(@NotNull String key);

    /**
     * Sets the value of a key.
     *
     * @param key   the key
     * @param value the value
     * @since 1.0.0
     */
    void set(@NotNull String key, @NotNull String value);

    /**
     * Sets the value of a key asynchronously.
     *
     * @param key   the key
     * @param value the value
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setAsync(@NotNull String key, @NotNull String value);

    /**
     * Sets a key with an expiration time.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the time-to-live
     * @since 1.0.0
     */
    void setex(@NotNull String key, @NotNull String value, @NotNull Duration ttl);

    /**
     * Sets a key with expiration asynchronously.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the time-to-live
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setexAsync(@NotNull String key, @NotNull String value, @NotNull Duration ttl);

    /**
     * Sets a key only if it does not exist.
     *
     * @param key   the key
     * @param value the value
     * @return true if the key was set
     * @since 1.0.0
     */
    boolean setnx(@NotNull String key, @NotNull String value);

    /**
     * Sets a key only if it does not exist, asynchronously.
     *
     * @param key   the key
     * @param value the value
     * @return a future with true if set
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> setnxAsync(@NotNull String key, @NotNull String value);

    /**
     * Gets multiple values.
     *
     * @param keys the keys
     * @return list of values (null for missing keys)
     * @since 1.0.0
     */
    @NotNull
    List<@Nullable String> mget(@NotNull String... keys);

    /**
     * Gets multiple values asynchronously.
     *
     * @param keys the keys
     * @return a future with the values
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<@Nullable String>> mgetAsync(@NotNull String... keys);

    /**
     * Sets multiple key-value pairs.
     *
     * @param keyValues the key-value pairs
     * @since 1.0.0
     */
    void mset(@NotNull Map<String, String> keyValues);

    /**
     * Sets multiple key-value pairs asynchronously.
     *
     * @param keyValues the key-value pairs
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> msetAsync(@NotNull Map<String, String> keyValues);

    // ========== Key Operations ==========

    /**
     * Deletes keys.
     *
     * @param keys the keys to delete
     * @return the number deleted
     * @since 1.0.0
     */
    long del(@NotNull String... keys);

    /**
     * Deletes keys asynchronously.
     *
     * @param keys the keys to delete
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> delAsync(@NotNull String... keys);

    /**
     * Checks if a key exists.
     *
     * @param key the key
     * @return true if exists
     * @since 1.0.0
     */
    boolean exists(@NotNull String key);

    /**
     * Checks if a key exists asynchronously.
     *
     * @param key the key
     * @return a future with true if exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> existsAsync(@NotNull String key);

    /**
     * Sets a key's TTL.
     *
     * @param key the key
     * @param ttl the TTL
     * @return true if the timeout was set
     * @since 1.0.0
     */
    boolean expire(@NotNull String key, @NotNull Duration ttl);

    /**
     * Sets a key's TTL asynchronously.
     *
     * @param key the key
     * @param ttl the TTL
     * @return a future with true if set
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> expireAsync(@NotNull String key, @NotNull Duration ttl);

    /**
     * Gets a key's TTL.
     *
     * @param key the key
     * @return the TTL, empty if no TTL or key doesn't exist
     * @since 1.0.0
     */
    @NotNull
    Optional<Duration> ttl(@NotNull String key);

    /**
     * Gets a key's TTL asynchronously.
     *
     * @param key the key
     * @return a future with the TTL
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Duration>> ttlAsync(@NotNull String key);

    // ========== Numeric Operations ==========

    /**
     * Increments a key.
     *
     * @param key the key
     * @return the new value
     * @since 1.0.0
     */
    long incr(@NotNull String key);

    /**
     * Increments a key asynchronously.
     *
     * @param key the key
     * @return a future with the new value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> incrAsync(@NotNull String key);

    /**
     * Increments a key by a specific amount.
     *
     * @param key       the key
     * @param increment the increment
     * @return the new value
     * @since 1.0.0
     */
    long incrBy(@NotNull String key, long increment);

    /**
     * Increments a key by an amount asynchronously.
     *
     * @param key       the key
     * @param increment the increment
     * @return a future with the new value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> incrByAsync(@NotNull String key, long increment);

    /**
     * Decrements a key.
     *
     * @param key the key
     * @return the new value
     * @since 1.0.0
     */
    long decr(@NotNull String key);

    /**
     * Decrements a key asynchronously.
     *
     * @param key the key
     * @return a future with the new value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> decrAsync(@NotNull String key);

    // ========== Hash Operations ==========

    /**
     * Gets a hash field value.
     *
     * @param key   the hash key
     * @param field the field
     * @return the value
     * @since 1.0.0
     */
    @NotNull
    Optional<String> hget(@NotNull String key, @NotNull String field);

    /**
     * Gets a hash field asynchronously.
     *
     * @param key   the hash key
     * @param field the field
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> hgetAsync(@NotNull String key, @NotNull String field);

    /**
     * Sets a hash field.
     *
     * @param key   the hash key
     * @param field the field
     * @param value the value
     * @return true if created, false if updated
     * @since 1.0.0
     */
    boolean hset(@NotNull String key, @NotNull String field, @NotNull String value);

    /**
     * Sets a hash field asynchronously.
     *
     * @param key   the hash key
     * @param field the field
     * @param value the value
     * @return a future with true if created
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> hsetAsync(@NotNull String key, @NotNull String field, @NotNull String value);

    /**
     * Gets all fields and values from a hash.
     *
     * @param key the hash key
     * @return the field-value map
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> hgetAll(@NotNull String key);

    /**
     * Gets all hash fields asynchronously.
     *
     * @param key the hash key
     * @return a future with the map
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Map<String, String>> hgetAllAsync(@NotNull String key);

    /**
     * Sets multiple hash fields.
     *
     * @param key         the hash key
     * @param fieldValues the field-value pairs
     * @since 1.0.0
     */
    void hmset(@NotNull String key, @NotNull Map<String, String> fieldValues);

    /**
     * Sets multiple hash fields asynchronously.
     *
     * @param key         the hash key
     * @param fieldValues the field-value pairs
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> hmsetAsync(@NotNull String key, @NotNull Map<String, String> fieldValues);

    /**
     * Deletes hash fields.
     *
     * @param key    the hash key
     * @param fields the fields to delete
     * @return the number deleted
     * @since 1.0.0
     */
    long hdel(@NotNull String key, @NotNull String... fields);

    /**
     * Deletes hash fields asynchronously.
     *
     * @param key    the hash key
     * @param fields the fields
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> hdelAsync(@NotNull String key, @NotNull String... fields);

    // ========== List Operations ==========

    /**
     * Pushes to the left of a list.
     *
     * @param key    the list key
     * @param values the values
     * @return the new length
     * @since 1.0.0
     */
    long lpush(@NotNull String key, @NotNull String... values);

    /**
     * Pushes to list left asynchronously.
     *
     * @param key    the list key
     * @param values the values
     * @return a future with the length
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> lpushAsync(@NotNull String key, @NotNull String... values);

    /**
     * Pushes to the right of a list.
     *
     * @param key    the list key
     * @param values the values
     * @return the new length
     * @since 1.0.0
     */
    long rpush(@NotNull String key, @NotNull String... values);

    /**
     * Pushes to list right asynchronously.
     *
     * @param key    the list key
     * @param values the values
     * @return a future with the length
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> rpushAsync(@NotNull String key, @NotNull String... values);

    /**
     * Pops from the left of a list.
     *
     * @param key the list key
     * @return the popped value
     * @since 1.0.0
     */
    @NotNull
    Optional<String> lpop(@NotNull String key);

    /**
     * Pops from list left asynchronously.
     *
     * @param key the list key
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> lpopAsync(@NotNull String key);

    /**
     * Pops from the right of a list.
     *
     * @param key the list key
     * @return the popped value
     * @since 1.0.0
     */
    @NotNull
    Optional<String> rpop(@NotNull String key);

    /**
     * Pops from list right asynchronously.
     *
     * @param key the list key
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> rpopAsync(@NotNull String key);

    /**
     * Gets a range from a list.
     *
     * @param key   the list key
     * @param start the start index
     * @param stop  the stop index
     * @return the elements
     * @since 1.0.0
     */
    @NotNull
    List<String> lrange(@NotNull String key, long start, long stop);

    /**
     * Gets a list range asynchronously.
     *
     * @param key   the list key
     * @param start the start index
     * @param stop  the stop index
     * @return a future with the elements
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<String>> lrangeAsync(@NotNull String key, long start, long stop);

    // ========== Set Operations ==========

    /**
     * Adds members to a set.
     *
     * @param key     the set key
     * @param members the members
     * @return the number added
     * @since 1.0.0
     */
    long sadd(@NotNull String key, @NotNull String... members);

    /**
     * Adds to a set asynchronously.
     *
     * @param key     the set key
     * @param members the members
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> saddAsync(@NotNull String key, @NotNull String... members);

    /**
     * Gets all set members.
     *
     * @param key the set key
     * @return the members
     * @since 1.0.0
     */
    @NotNull
    Set<String> smembers(@NotNull String key);

    /**
     * Gets set members asynchronously.
     *
     * @param key the set key
     * @return a future with the members
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Set<String>> smembersAsync(@NotNull String key);

    /**
     * Checks set membership.
     *
     * @param key    the set key
     * @param member the member
     * @return true if member exists
     * @since 1.0.0
     */
    boolean sismember(@NotNull String key, @NotNull String member);

    /**
     * Checks set membership asynchronously.
     *
     * @param key    the set key
     * @param member the member
     * @return a future with true if member
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> sismemberAsync(@NotNull String key, @NotNull String member);

    /**
     * Removes set members.
     *
     * @param key     the set key
     * @param members the members
     * @return the number removed
     * @since 1.0.0
     */
    long srem(@NotNull String key, @NotNull String... members);

    /**
     * Removes set members asynchronously.
     *
     * @param key     the set key
     * @param members the members
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> sremAsync(@NotNull String key, @NotNull String... members);

    // ========== Pub/Sub ==========

    /**
     * Publishes a message to a channel.
     *
     * @param channel the channel
     * @param message the message
     * @return the number of subscribers that received the message
     * @since 1.0.0
     */
    long publish(@NotNull String channel, @NotNull String message);

    /**
     * Publishes a message asynchronously.
     *
     * @param channel the channel
     * @param message the message
     * @return a future with the subscriber count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message);

    /**
     * Subscribes to a channel.
     *
     * @param channel  the channel
     * @param listener the message listener
     * @return a subscription that can be cancelled
     * @since 1.0.0
     */
    @NotNull
    PubSubSubscription subscribe(@NotNull String channel, @NotNull Consumer<String> listener);

    /**
     * Subscribes to channels matching a pattern.
     *
     * @param pattern  the pattern (e.g., "chat:*")
     * @param listener the message listener (receives channel and message)
     * @return a subscription that can be cancelled
     * @since 1.0.0
     */
    @NotNull
    PubSubSubscription psubscribe(@NotNull String pattern, @NotNull PubSubListener listener);

    /**
     * Gets the pub/sub interface for advanced operations.
     *
     * @return the pub/sub interface
     * @since 1.0.0
     */
    @NotNull
    RedisPubSub pubSub();

    // ========== Lua Scripts ==========

    /**
     * Executes a Lua script.
     *
     * @param script the script
     * @param keys   the keys
     * @param args   the arguments
     * @return the result
     * @since 1.0.0
     */
    @Nullable
    Object eval(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Executes a Lua script asynchronously.
     *
     * @param script the script
     * @param keys   the keys
     * @param args   the arguments
     * @return a future with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Object> evalAsync(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Loads a Lua script.
     *
     * @param script the script
     * @return the Lua script wrapper
     * @since 1.0.0
     */
    @NotNull
    RedisLuaScript loadScript(@NotNull String script);

    // ========== Serialization ==========

    /**
     * Gets a typed value using serialization.
     *
     * @param <T>        the value type
     * @param key        the key
     * @param type       the value class
     * @param serializer the serializer
     * @return the value
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type, @NotNull RedisSerializer<T> serializer);

    /**
     * Sets a typed value using serialization.
     *
     * @param <T>        the value type
     * @param key        the key
     * @param value      the value
     * @param serializer the serializer
     * @since 1.0.0
     */
    <T> void set(@NotNull String key, @NotNull T value, @NotNull RedisSerializer<T> serializer);

    /**
     * Sets a typed value with TTL using serialization.
     *
     * @param <T>        the value type
     * @param key        the key
     * @param value      the value
     * @param ttl        the TTL
     * @param serializer the serializer
     * @since 1.0.0
     */
    <T> void setex(@NotNull String key, @NotNull T value, @NotNull Duration ttl, @NotNull RedisSerializer<T> serializer);

    // ========== Shutdown ==========

    /**
     * Shuts down the Redis service and closes all connections.
     *
     * @since 1.0.0
     */
    void shutdown();

    /**
     * Pool statistics.
     *
     * @param active    active connections
     * @param idle      idle connections
     * @param total     total connections created
     * @param borrowed  total times borrowed
     * @param returned  total times returned
     * @param created   total connections created
     * @param destroyed total connections destroyed
     * @since 1.0.0
     */
    record PoolStats(
            int active,
            int idle,
            int total,
            long borrowed,
            long returned,
            long created,
            long destroyed
    ) {}

    /**
     * Pub/Sub subscription handle.
     *
     * @since 1.0.0
     */
    interface PubSubSubscription extends AutoCloseable {

        /**
         * Checks if the subscription is active.
         *
         * @return true if active
         */
        boolean isActive();

        /**
         * Gets the channel or pattern this subscription is for.
         *
         * @return the channel or pattern
         */
        @NotNull
        String getChannel();

        /**
         * Unsubscribes from the channel.
         */
        void unsubscribe();

        @Override
        default void close() {
            unsubscribe();
        }
    }
}
