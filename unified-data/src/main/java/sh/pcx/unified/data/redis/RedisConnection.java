/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper interface for Redis connections.
 *
 * <p>This interface provides a unified API for interacting with Redis,
 * abstracting away the differences between Jedis and Lettuce clients.
 * It supports both synchronous and asynchronous operations.
 *
 * <h2>Synchronous Operations</h2>
 * <pre>{@code
 * try (RedisConnection conn = redisService.getConnection()) {
 *     conn.set("key", "value");
 *     String value = conn.get("key").orElse("default");
 *     conn.expire("key", Duration.ofMinutes(30));
 * }
 * }</pre>
 *
 * <h2>Asynchronous Operations</h2>
 * <pre>{@code
 * RedisConnection conn = redisService.getConnection();
 * conn.setAsync("key", "value")
 *     .thenCompose(v -> conn.getAsync("key"))
 *     .thenAccept(value -> System.out.println("Value: " + value));
 * }</pre>
 *
 * <h2>Pipelining</h2>
 * <pre>{@code
 * try (RedisConnection conn = redisService.getConnection()) {
 *     conn.pipeline()
 *         .set("key1", "value1")
 *         .set("key2", "value2")
 *         .incr("counter")
 *         .sync();
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisService
 * @see RedisConfig
 */
public interface RedisConnection extends AutoCloseable {

    // ========== Connection Management ==========

    /**
     * Checks if this connection is currently valid and connected.
     *
     * @return true if the connection is active
     * @since 1.0.0
     */
    boolean isConnected();

    /**
     * Pings the Redis server to test connectivity.
     *
     * @return "PONG" if successful
     * @throws RedisException if the ping fails
     * @since 1.0.0
     */
    @NotNull
    String ping();

    /**
     * Pings the Redis server asynchronously.
     *
     * @return a future that completes with "PONG"
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<String> pingAsync();

    /**
     * Closes this connection and returns it to the pool.
     *
     * @since 1.0.0
     */
    @Override
    void close();

    // ========== String Operations ==========

    /**
     * Gets the value of a key.
     *
     * @param key the key
     * @return an Optional containing the value if the key exists
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Optional<String> get(@NotNull String key);

    /**
     * Gets the value of a key asynchronously.
     *
     * @param key the key
     * @return a future that completes with an Optional containing the value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> getAsync(@NotNull String key);

    /**
     * Sets the value of a key.
     *
     * @param key   the key
     * @param value the value
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    void set(@NotNull String key, @NotNull String value);

    /**
     * Sets the value of a key asynchronously.
     *
     * @param key   the key
     * @param value the value
     * @return a future that completes when the operation is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setAsync(@NotNull String key, @NotNull String value);

    /**
     * Sets the value of a key with an expiration time.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the time-to-live duration
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    void setex(@NotNull String key, @NotNull String value, @NotNull Duration ttl);

    /**
     * Sets the value of a key with expiration asynchronously.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the time-to-live duration
     * @return a future that completes when the operation is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setexAsync(@NotNull String key, @NotNull String value, @NotNull Duration ttl);

    /**
     * Sets a key only if it does not exist.
     *
     * @param key   the key
     * @param value the value
     * @return true if the key was set (did not exist)
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    boolean setnx(@NotNull String key, @NotNull String value);

    /**
     * Sets a key only if it does not exist, asynchronously.
     *
     * @param key   the key
     * @param value the value
     * @return a future that completes with true if the key was set
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> setnxAsync(@NotNull String key, @NotNull String value);

    /**
     * Gets multiple values in a single operation.
     *
     * @param keys the keys to get
     * @return a list of values (null for non-existent keys)
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    List<@Nullable String> mget(@NotNull String... keys);

    /**
     * Gets multiple values asynchronously.
     *
     * @param keys the keys to get
     * @return a future that completes with the values
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<@Nullable String>> mgetAsync(@NotNull String... keys);

    /**
     * Sets multiple key-value pairs in a single operation.
     *
     * @param keyValues the key-value pairs
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    void mset(@NotNull Map<String, String> keyValues);

    /**
     * Sets multiple key-value pairs asynchronously.
     *
     * @param keyValues the key-value pairs
     * @return a future that completes when the operation is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> msetAsync(@NotNull Map<String, String> keyValues);

    // ========== Numeric Operations ==========

    /**
     * Increments the integer value of a key by one.
     *
     * @param key the key
     * @return the new value after incrementing
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long incr(@NotNull String key);

    /**
     * Increments a key asynchronously.
     *
     * @param key the key
     * @return a future that completes with the new value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> incrAsync(@NotNull String key);

    /**
     * Increments the integer value of a key by a specific amount.
     *
     * @param key       the key
     * @param increment the amount to increment by
     * @return the new value after incrementing
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long incrBy(@NotNull String key, long increment);

    /**
     * Increments by a specific amount asynchronously.
     *
     * @param key       the key
     * @param increment the amount to increment by
     * @return a future that completes with the new value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> incrByAsync(@NotNull String key, long increment);

    /**
     * Decrements the integer value of a key by one.
     *
     * @param key the key
     * @return the new value after decrementing
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long decr(@NotNull String key);

    /**
     * Decrements a key asynchronously.
     *
     * @param key the key
     * @return a future that completes with the new value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> decrAsync(@NotNull String key);

    // ========== Key Operations ==========

    /**
     * Deletes one or more keys.
     *
     * @param keys the keys to delete
     * @return the number of keys deleted
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long del(@NotNull String... keys);

    /**
     * Deletes keys asynchronously.
     *
     * @param keys the keys to delete
     * @return a future that completes with the number deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> delAsync(@NotNull String... keys);

    /**
     * Checks if a key exists.
     *
     * @param key the key
     * @return true if the key exists
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    boolean exists(@NotNull String key);

    /**
     * Checks if a key exists asynchronously.
     *
     * @param key the key
     * @return a future that completes with true if exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> existsAsync(@NotNull String key);

    /**
     * Sets a timeout on a key.
     *
     * @param key the key
     * @param ttl the time-to-live duration
     * @return true if the timeout was set
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    boolean expire(@NotNull String key, @NotNull Duration ttl);

    /**
     * Sets a timeout on a key asynchronously.
     *
     * @param key the key
     * @param ttl the time-to-live duration
     * @return a future that completes with true if timeout was set
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> expireAsync(@NotNull String key, @NotNull Duration ttl);

    /**
     * Gets the time-to-live of a key.
     *
     * @param key the key
     * @return the TTL duration, or empty if key doesn't exist or has no TTL
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Optional<Duration> ttl(@NotNull String key);

    /**
     * Gets the TTL of a key asynchronously.
     *
     * @param key the key
     * @return a future that completes with the TTL
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Duration>> ttlAsync(@NotNull String key);

    /**
     * Finds all keys matching a pattern.
     *
     * <p><strong>Warning:</strong> This command should not be used in production
     * environments with large key spaces as it may block Redis.
     *
     * @param pattern the pattern to match (e.g., "user:*")
     * @return the matching keys
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Set<String> keys(@NotNull String pattern);

    /**
     * Finds keys matching a pattern asynchronously.
     *
     * @param pattern the pattern to match
     * @return a future that completes with the matching keys
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Set<String>> keysAsync(@NotNull String pattern);

    // ========== Hash Operations ==========

    /**
     * Gets the value of a hash field.
     *
     * @param key   the hash key
     * @param field the field name
     * @return an Optional containing the field value
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Optional<String> hget(@NotNull String key, @NotNull String field);

    /**
     * Gets a hash field asynchronously.
     *
     * @param key   the hash key
     * @param field the field name
     * @return a future that completes with the field value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> hgetAsync(@NotNull String key, @NotNull String field);

    /**
     * Sets the value of a hash field.
     *
     * @param key   the hash key
     * @param field the field name
     * @param value the field value
     * @return true if the field was created, false if updated
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    boolean hset(@NotNull String key, @NotNull String field, @NotNull String value);

    /**
     * Sets a hash field asynchronously.
     *
     * @param key   the hash key
     * @param field the field name
     * @param value the field value
     * @return a future that completes with true if created
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> hsetAsync(@NotNull String key, @NotNull String field, @NotNull String value);

    /**
     * Gets all fields and values in a hash.
     *
     * @param key the hash key
     * @return a map of field names to values
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> hgetAll(@NotNull String key);

    /**
     * Gets all hash fields and values asynchronously.
     *
     * @param key the hash key
     * @return a future that completes with the field map
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Map<String, String>> hgetAllAsync(@NotNull String key);

    /**
     * Sets multiple hash fields in a single operation.
     *
     * @param key         the hash key
     * @param fieldValues the field-value pairs
     * @throws RedisException if the operation fails
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
     * Deletes one or more hash fields.
     *
     * @param key    the hash key
     * @param fields the fields to delete
     * @return the number of fields deleted
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long hdel(@NotNull String key, @NotNull String... fields);

    /**
     * Deletes hash fields asynchronously.
     *
     * @param key    the hash key
     * @param fields the fields to delete
     * @return a future that completes with the number deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> hdelAsync(@NotNull String key, @NotNull String... fields);

    /**
     * Checks if a hash field exists.
     *
     * @param key   the hash key
     * @param field the field name
     * @return true if the field exists
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    boolean hexists(@NotNull String key, @NotNull String field);

    /**
     * Checks if a hash field exists asynchronously.
     *
     * @param key   the hash key
     * @param field the field name
     * @return a future that completes with true if exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> hexistsAsync(@NotNull String key, @NotNull String field);

    // ========== List Operations ==========

    /**
     * Pushes values to the left (head) of a list.
     *
     * @param key    the list key
     * @param values the values to push
     * @return the length of the list after the push
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long lpush(@NotNull String key, @NotNull String... values);

    /**
     * Pushes values to the left of a list asynchronously.
     *
     * @param key    the list key
     * @param values the values to push
     * @return a future that completes with the new length
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> lpushAsync(@NotNull String key, @NotNull String... values);

    /**
     * Pushes values to the right (tail) of a list.
     *
     * @param key    the list key
     * @param values the values to push
     * @return the length of the list after the push
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long rpush(@NotNull String key, @NotNull String... values);

    /**
     * Pushes values to the right of a list asynchronously.
     *
     * @param key    the list key
     * @param values the values to push
     * @return a future that completes with the new length
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> rpushAsync(@NotNull String key, @NotNull String... values);

    /**
     * Pops a value from the left of a list.
     *
     * @param key the list key
     * @return an Optional containing the popped value
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Optional<String> lpop(@NotNull String key);

    /**
     * Pops from the left of a list asynchronously.
     *
     * @param key the list key
     * @return a future that completes with the popped value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> lpopAsync(@NotNull String key);

    /**
     * Pops a value from the right of a list.
     *
     * @param key the list key
     * @return an Optional containing the popped value
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Optional<String> rpop(@NotNull String key);

    /**
     * Pops from the right of a list asynchronously.
     *
     * @param key the list key
     * @return a future that completes with the popped value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> rpopAsync(@NotNull String key);

    /**
     * Gets a range of elements from a list.
     *
     * @param key   the list key
     * @param start the start index (0-based, inclusive)
     * @param stop  the stop index (inclusive, -1 for end)
     * @return the list of elements
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    List<String> lrange(@NotNull String key, long start, long stop);

    /**
     * Gets a range of list elements asynchronously.
     *
     * @param key   the list key
     * @param start the start index
     * @param stop  the stop index
     * @return a future that completes with the elements
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<String>> lrangeAsync(@NotNull String key, long start, long stop);

    /**
     * Gets the length of a list.
     *
     * @param key the list key
     * @return the length of the list
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long llen(@NotNull String key);

    /**
     * Gets the length of a list asynchronously.
     *
     * @param key the list key
     * @return a future that completes with the length
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> llenAsync(@NotNull String key);

    // ========== Set Operations ==========

    /**
     * Adds members to a set.
     *
     * @param key     the set key
     * @param members the members to add
     * @return the number of members added (not including duplicates)
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long sadd(@NotNull String key, @NotNull String... members);

    /**
     * Adds members to a set asynchronously.
     *
     * @param key     the set key
     * @param members the members to add
     * @return a future that completes with the number added
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> saddAsync(@NotNull String key, @NotNull String... members);

    /**
     * Gets all members of a set.
     *
     * @param key the set key
     * @return the set members
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Set<String> smembers(@NotNull String key);

    /**
     * Gets all set members asynchronously.
     *
     * @param key the set key
     * @return a future that completes with the members
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Set<String>> smembersAsync(@NotNull String key);

    /**
     * Checks if a value is a member of a set.
     *
     * @param key    the set key
     * @param member the member to check
     * @return true if the member exists in the set
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    boolean sismember(@NotNull String key, @NotNull String member);

    /**
     * Checks set membership asynchronously.
     *
     * @param key    the set key
     * @param member the member to check
     * @return a future that completes with true if member exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> sismemberAsync(@NotNull String key, @NotNull String member);

    /**
     * Removes members from a set.
     *
     * @param key     the set key
     * @param members the members to remove
     * @return the number of members removed
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long srem(@NotNull String key, @NotNull String... members);

    /**
     * Removes set members asynchronously.
     *
     * @param key     the set key
     * @param members the members to remove
     * @return a future that completes with the number removed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> sremAsync(@NotNull String key, @NotNull String... members);

    // ========== Sorted Set Operations ==========

    /**
     * Adds members with scores to a sorted set.
     *
     * @param key          the sorted set key
     * @param scoreMembers map of members to their scores
     * @return the number of members added (not updated)
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long zadd(@NotNull String key, @NotNull Map<String, Double> scoreMembers);

    /**
     * Adds members with scores asynchronously.
     *
     * @param key          the sorted set key
     * @param scoreMembers map of members to their scores
     * @return a future that completes with the number added
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> zaddAsync(@NotNull String key, @NotNull Map<String, Double> scoreMembers);

    /**
     * Gets the score of a sorted set member.
     *
     * @param key    the sorted set key
     * @param member the member
     * @return an Optional containing the score
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    Optional<Double> zscore(@NotNull String key, @NotNull String member);

    /**
     * Gets the score of a member asynchronously.
     *
     * @param key    the sorted set key
     * @param member the member
     * @return a future that completes with the score
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Double>> zscoreAsync(@NotNull String key, @NotNull String member);

    /**
     * Gets a range of members from a sorted set by index.
     *
     * @param key   the sorted set key
     * @param start the start index
     * @param stop  the stop index (-1 for end)
     * @return the members in ascending order
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    List<String> zrange(@NotNull String key, long start, long stop);

    /**
     * Gets a range of sorted set members asynchronously.
     *
     * @param key   the sorted set key
     * @param start the start index
     * @param stop  the stop index
     * @return a future that completes with the members
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<String>> zrangeAsync(@NotNull String key, long start, long stop);

    // ========== Pub/Sub Operations ==========

    /**
     * Publishes a message to a channel.
     *
     * @param channel the channel name
     * @param message the message to publish
     * @return the number of clients that received the message
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    long publish(@NotNull String channel, @NotNull String message);

    /**
     * Publishes a message asynchronously.
     *
     * @param channel the channel name
     * @param message the message to publish
     * @return a future that completes with the number of receivers
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message);

    // ========== Script Operations ==========

    /**
     * Evaluates a Lua script.
     *
     * @param script the Lua script
     * @param keys   the keys to pass to the script
     * @param args   the arguments to pass to the script
     * @return the script result
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @Nullable
    Object eval(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Evaluates a Lua script asynchronously.
     *
     * @param script the Lua script
     * @param keys   the keys to pass to the script
     * @param args   the arguments to pass to the script
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Object> evalAsync(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Evaluates a cached Lua script by its SHA1 hash.
     *
     * @param sha1 the script's SHA1 hash
     * @param keys the keys to pass to the script
     * @param args the arguments to pass to the script
     * @return the script result
     * @throws RedisException if the operation fails or script not cached
     * @since 1.0.0
     */
    @Nullable
    Object evalsha(@NotNull String sha1, @NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Loads a script into the Redis script cache.
     *
     * @param script the Lua script
     * @return the SHA1 hash of the script
     * @throws RedisException if the operation fails
     * @since 1.0.0
     */
    @NotNull
    String scriptLoad(@NotNull String script);

    // ========== Transaction Support ==========

    /**
     * Begins a Redis transaction (MULTI).
     *
     * @return a transaction object for queueing commands
     * @since 1.0.0
     */
    @NotNull
    RedisTransaction multi();

    /**
     * Redis transaction interface for atomic operations.
     *
     * @since 1.0.0
     */
    interface RedisTransaction {

        /**
         * Queues a SET command.
         *
         * @param key   the key
         * @param value the value
         * @return this transaction
         */
        @NotNull
        RedisTransaction set(@NotNull String key, @NotNull String value);

        /**
         * Queues a GET command.
         *
         * @param key the key
         * @return this transaction
         */
        @NotNull
        RedisTransaction get(@NotNull String key);

        /**
         * Queues a DEL command.
         *
         * @param keys the keys to delete
         * @return this transaction
         */
        @NotNull
        RedisTransaction del(@NotNull String... keys);

        /**
         * Queues an INCR command.
         *
         * @param key the key
         * @return this transaction
         */
        @NotNull
        RedisTransaction incr(@NotNull String key);

        /**
         * Queues an EXPIRE command.
         *
         * @param key the key
         * @param ttl the TTL
         * @return this transaction
         */
        @NotNull
        RedisTransaction expire(@NotNull String key, @NotNull Duration ttl);

        /**
         * Executes the transaction.
         *
         * @return the list of results from each command
         * @throws RedisException if the transaction fails
         */
        @NotNull
        List<Object> exec();

        /**
         * Discards the transaction.
         */
        void discard();
    }

    /**
     * Exception thrown for Redis operation failures.
     *
     * @since 1.0.0
     */
    class RedisException extends RuntimeException {

        /**
         * Creates a new Redis exception.
         *
         * @param message the error message
         */
        public RedisException(String message) {
            super(message);
        }

        /**
         * Creates a new Redis exception with a cause.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public RedisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
