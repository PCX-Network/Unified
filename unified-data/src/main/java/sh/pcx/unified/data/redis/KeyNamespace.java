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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Utility for key prefixing and namespacing in Redis.
 *
 * <p>Key namespacing prevents key collisions when multiple plugins or components
 * share the same Redis instance. All keys are automatically prefixed with the
 * namespace prefix.
 *
 * <h2>Key Format</h2>
 * <p>Keys are formatted as: {@code namespace:key} or for nested namespaces:
 * {@code parent:child:key}
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a namespace for your plugin
 * KeyNamespace ns = redis.namespace("myplugin");
 *
 * // All operations are automatically prefixed
 * ns.set("player:uuid", "data");      // Actually stores "myplugin:player:uuid"
 * ns.get("player:uuid");              // Actually gets "myplugin:player:uuid"
 *
 * // Nested namespaces
 * KeyNamespace playerNs = ns.child("player");
 * playerNs.set("uuid", "data");       // Stores "myplugin:player:uuid"
 *
 * // Raw key access
 * String rawKey = ns.key("player:uuid");  // Returns "myplugin:player:uuid"
 *
 * // Get keys without namespace prefix
 * Set<String> keys = ns.keys("*");    // Returns keys with prefix stripped
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Use lowercase, descriptive prefixes</li>
 *   <li>Use colons as separators within keys</li>
 *   <li>Keep namespace prefixes short but unique</li>
 *   <li>Consider using plugin name as the root namespace</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisService#namespace(String)
 */
public final class KeyNamespace {

    /**
     * The separator used between namespace parts and keys.
     */
    public static final String SEPARATOR = ":";

    private final RedisService redisService;
    private final String prefix;

    /**
     * Creates a new key namespace.
     *
     * @param redisService the Redis service
     * @param prefix       the namespace prefix
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if prefix is blank
     */
    public KeyNamespace(@NotNull RedisService redisService, @NotNull String prefix) {
        Objects.requireNonNull(redisService, "redisService cannot be null");
        Objects.requireNonNull(prefix, "prefix cannot be null");

        if (prefix.isBlank()) {
            throw new IllegalArgumentException("prefix cannot be blank");
        }

        this.redisService = redisService;
        this.prefix = normalizePrefix(prefix);
    }

    /**
     * Creates a new key namespace with multiple parts.
     *
     * @param redisService the Redis service
     * @param prefixes     the namespace prefix parts
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if no prefixes provided
     */
    public KeyNamespace(@NotNull RedisService redisService, @NotNull String... prefixes) {
        Objects.requireNonNull(redisService, "redisService cannot be null");
        Objects.requireNonNull(prefixes, "prefixes cannot be null");

        if (prefixes.length == 0) {
            throw new IllegalArgumentException("at least one prefix is required");
        }

        this.redisService = redisService;
        this.prefix = normalizePrefix(String.join(SEPARATOR, prefixes));
    }

    private static String normalizePrefix(String prefix) {
        // Remove trailing separator
        String normalized = prefix.strip();
        while (normalized.endsWith(SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Gets the namespace prefix.
     *
     * @return the prefix (without trailing separator)
     * @since 1.0.0
     */
    @NotNull
    public String getPrefix() {
        return prefix;
    }

    /**
     * Creates a child namespace.
     *
     * <p>The child namespace has this namespace's prefix followed by the child prefix.
     *
     * @param childPrefix the child prefix
     * @return a new child namespace
     * @since 1.0.0
     */
    @NotNull
    public KeyNamespace child(@NotNull String childPrefix) {
        Objects.requireNonNull(childPrefix, "childPrefix cannot be null");
        return new KeyNamespace(redisService, prefix + SEPARATOR + childPrefix);
    }

    /**
     * Converts a key to its namespaced form.
     *
     * @param key the key (without namespace)
     * @return the full key with namespace prefix
     * @since 1.0.0
     */
    @NotNull
    public String key(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return prefix + SEPARATOR + key;
    }

    /**
     * Converts multiple keys to their namespaced form.
     *
     * @param keys the keys (without namespace)
     * @return the full keys with namespace prefix
     * @since 1.0.0
     */
    @NotNull
    public String[] keys(@NotNull String... keys) {
        String[] result = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            result[i] = key(keys[i]);
        }
        return result;
    }

    /**
     * Strips the namespace prefix from a key.
     *
     * @param namespacedKey the key with namespace prefix
     * @return the key without prefix, or the original if it doesn't have the prefix
     * @since 1.0.0
     */
    @NotNull
    public String stripPrefix(@NotNull String namespacedKey) {
        String prefixWithSep = prefix + SEPARATOR;
        if (namespacedKey.startsWith(prefixWithSep)) {
            return namespacedKey.substring(prefixWithSep.length());
        }
        return namespacedKey;
    }

    /**
     * Checks if a key belongs to this namespace.
     *
     * @param namespacedKey the key to check
     * @return true if the key has this namespace's prefix
     * @since 1.0.0
     */
    public boolean belongsToNamespace(@NotNull String namespacedKey) {
        return namespacedKey.startsWith(prefix + SEPARATOR);
    }

    // ========== String Operations ==========

    /**
     * Gets the value of a key in this namespace.
     *
     * @param key the key (without namespace)
     * @return the value
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> get(@NotNull String key) {
        return redisService.get(key(key));
    }

    /**
     * Gets the value asynchronously.
     *
     * @param key the key (without namespace)
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<String>> getAsync(@NotNull String key) {
        return redisService.getAsync(key(key));
    }

    /**
     * Sets a key-value pair in this namespace.
     *
     * @param key   the key (without namespace)
     * @param value the value
     * @since 1.0.0
     */
    public void set(@NotNull String key, @NotNull String value) {
        redisService.set(key(key), value);
    }

    /**
     * Sets a key-value pair asynchronously.
     *
     * @param key   the key (without namespace)
     * @param value the value
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> setAsync(@NotNull String key, @NotNull String value) {
        return redisService.setAsync(key(key), value);
    }

    /**
     * Sets a key with expiration.
     *
     * @param key   the key (without namespace)
     * @param value the value
     * @param ttl   the TTL
     * @since 1.0.0
     */
    public void setex(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        redisService.setex(key(key), value, ttl);
    }

    /**
     * Sets a key with expiration asynchronously.
     *
     * @param key   the key (without namespace)
     * @param value the value
     * @param ttl   the TTL
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> setexAsync(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        return redisService.setexAsync(key(key), value, ttl);
    }

    /**
     * Sets a key only if it doesn't exist.
     *
     * @param key   the key (without namespace)
     * @param value the value
     * @return true if set
     * @since 1.0.0
     */
    public boolean setnx(@NotNull String key, @NotNull String value) {
        return redisService.setnx(key(key), value);
    }

    /**
     * Sets a key only if it doesn't exist, asynchronously.
     *
     * @param key   the key (without namespace)
     * @param value the value
     * @return a future with true if set
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> setnxAsync(@NotNull String key, @NotNull String value) {
        return redisService.setnxAsync(key(key), value);
    }

    /**
     * Gets multiple values.
     *
     * @param keys the keys (without namespace)
     * @return the values
     * @since 1.0.0
     */
    @NotNull
    public List<@Nullable String> mget(@NotNull String... keys) {
        return redisService.mget(keys(keys));
    }

    /**
     * Gets multiple values asynchronously.
     *
     * @param keys the keys (without namespace)
     * @return a future with the values
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<@Nullable String>> mgetAsync(@NotNull String... keys) {
        return redisService.mgetAsync(keys(keys));
    }

    /**
     * Sets multiple key-value pairs.
     *
     * @param keyValues the key-value pairs (keys without namespace)
     * @since 1.0.0
     */
    public void mset(@NotNull Map<String, String> keyValues) {
        Map<String, String> namespacedMap = keyValues.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> key(e.getKey()),
                        Map.Entry::getValue
                ));
        redisService.mset(namespacedMap);
    }

    /**
     * Sets multiple key-value pairs asynchronously.
     *
     * @param keyValues the key-value pairs (keys without namespace)
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> msetAsync(@NotNull Map<String, String> keyValues) {
        Map<String, String> namespacedMap = keyValues.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> key(e.getKey()),
                        Map.Entry::getValue
                ));
        return redisService.msetAsync(namespacedMap);
    }

    // ========== Key Operations ==========

    /**
     * Deletes keys in this namespace.
     *
     * @param keys the keys (without namespace)
     * @return the number deleted
     * @since 1.0.0
     */
    public long del(@NotNull String... keys) {
        return redisService.del(keys(keys));
    }

    /**
     * Deletes keys asynchronously.
     *
     * @param keys the keys (without namespace)
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> delAsync(@NotNull String... keys) {
        return redisService.delAsync(keys(keys));
    }

    /**
     * Checks if a key exists in this namespace.
     *
     * @param key the key (without namespace)
     * @return true if exists
     * @since 1.0.0
     */
    public boolean exists(@NotNull String key) {
        return redisService.exists(key(key));
    }

    /**
     * Checks if a key exists asynchronously.
     *
     * @param key the key (without namespace)
     * @return a future with true if exists
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> existsAsync(@NotNull String key) {
        return redisService.existsAsync(key(key));
    }

    /**
     * Sets a key's TTL.
     *
     * @param key the key (without namespace)
     * @param ttl the TTL
     * @return true if the timeout was set
     * @since 1.0.0
     */
    public boolean expire(@NotNull String key, @NotNull Duration ttl) {
        return redisService.expire(key(key), ttl);
    }

    /**
     * Sets a key's TTL asynchronously.
     *
     * @param key the key (without namespace)
     * @param ttl the TTL
     * @return a future with true if set
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> expireAsync(@NotNull String key, @NotNull Duration ttl) {
        return redisService.expireAsync(key(key), ttl);
    }

    /**
     * Gets a key's TTL.
     *
     * @param key the key (without namespace)
     * @return the TTL
     * @since 1.0.0
     */
    @NotNull
    public Optional<Duration> ttl(@NotNull String key) {
        return redisService.ttl(key(key));
    }

    /**
     * Gets a key's TTL asynchronously.
     *
     * @param key the key (without namespace)
     * @return a future with the TTL
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<Duration>> ttlAsync(@NotNull String key) {
        return redisService.ttlAsync(key(key));
    }

    // ========== Numeric Operations ==========

    /**
     * Increments a key.
     *
     * @param key the key (without namespace)
     * @return the new value
     * @since 1.0.0
     */
    public long incr(@NotNull String key) {
        return redisService.incr(key(key));
    }

    /**
     * Increments a key asynchronously.
     *
     * @param key the key (without namespace)
     * @return a future with the new value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> incrAsync(@NotNull String key) {
        return redisService.incrAsync(key(key));
    }

    /**
     * Increments a key by an amount.
     *
     * @param key       the key (without namespace)
     * @param increment the increment
     * @return the new value
     * @since 1.0.0
     */
    public long incrBy(@NotNull String key, long increment) {
        return redisService.incrBy(key(key), increment);
    }

    /**
     * Increments a key by an amount asynchronously.
     *
     * @param key       the key (without namespace)
     * @param increment the increment
     * @return a future with the new value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> incrByAsync(@NotNull String key, long increment) {
        return redisService.incrByAsync(key(key), increment);
    }

    /**
     * Decrements a key.
     *
     * @param key the key (without namespace)
     * @return the new value
     * @since 1.0.0
     */
    public long decr(@NotNull String key) {
        return redisService.decr(key(key));
    }

    /**
     * Decrements a key asynchronously.
     *
     * @param key the key (without namespace)
     * @return a future with the new value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> decrAsync(@NotNull String key) {
        return redisService.decrAsync(key(key));
    }

    // ========== Hash Operations ==========

    /**
     * Gets a hash field.
     *
     * @param key   the hash key (without namespace)
     * @param field the field
     * @return the value
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> hget(@NotNull String key, @NotNull String field) {
        return redisService.hget(key(key), field);
    }

    /**
     * Gets a hash field asynchronously.
     *
     * @param key   the hash key (without namespace)
     * @param field the field
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<String>> hgetAsync(@NotNull String key, @NotNull String field) {
        return redisService.hgetAsync(key(key), field);
    }

    /**
     * Sets a hash field.
     *
     * @param key   the hash key (without namespace)
     * @param field the field
     * @param value the value
     * @return true if created
     * @since 1.0.0
     */
    public boolean hset(@NotNull String key, @NotNull String field, @NotNull String value) {
        return redisService.hset(key(key), field, value);
    }

    /**
     * Sets a hash field asynchronously.
     *
     * @param key   the hash key (without namespace)
     * @param field the field
     * @param value the value
     * @return a future with true if created
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> hsetAsync(@NotNull String key, @NotNull String field, @NotNull String value) {
        return redisService.hsetAsync(key(key), field, value);
    }

    /**
     * Gets all hash fields.
     *
     * @param key the hash key (without namespace)
     * @return the field-value map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, String> hgetAll(@NotNull String key) {
        return redisService.hgetAll(key(key));
    }

    /**
     * Gets all hash fields asynchronously.
     *
     * @param key the hash key (without namespace)
     * @return a future with the map
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Map<String, String>> hgetAllAsync(@NotNull String key) {
        return redisService.hgetAllAsync(key(key));
    }

    /**
     * Sets multiple hash fields.
     *
     * @param key         the hash key (without namespace)
     * @param fieldValues the field-value pairs
     * @since 1.0.0
     */
    public void hmset(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        redisService.hmset(key(key), fieldValues);
    }

    /**
     * Sets multiple hash fields asynchronously.
     *
     * @param key         the hash key (without namespace)
     * @param fieldValues the field-value pairs
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> hmsetAsync(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        return redisService.hmsetAsync(key(key), fieldValues);
    }

    /**
     * Deletes hash fields.
     *
     * @param key    the hash key (without namespace)
     * @param fields the fields
     * @return the number deleted
     * @since 1.0.0
     */
    public long hdel(@NotNull String key, @NotNull String... fields) {
        return redisService.hdel(key(key), fields);
    }

    /**
     * Deletes hash fields asynchronously.
     *
     * @param key    the hash key (without namespace)
     * @param fields the fields
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> hdelAsync(@NotNull String key, @NotNull String... fields) {
        return redisService.hdelAsync(key(key), fields);
    }

    // ========== List Operations ==========

    /**
     * Pushes to the left of a list.
     *
     * @param key    the list key (without namespace)
     * @param values the values
     * @return the new length
     * @since 1.0.0
     */
    public long lpush(@NotNull String key, @NotNull String... values) {
        return redisService.lpush(key(key), values);
    }

    /**
     * Pushes to the left asynchronously.
     *
     * @param key    the list key (without namespace)
     * @param values the values
     * @return a future with the length
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> lpushAsync(@NotNull String key, @NotNull String... values) {
        return redisService.lpushAsync(key(key), values);
    }

    /**
     * Pushes to the right of a list.
     *
     * @param key    the list key (without namespace)
     * @param values the values
     * @return the new length
     * @since 1.0.0
     */
    public long rpush(@NotNull String key, @NotNull String... values) {
        return redisService.rpush(key(key), values);
    }

    /**
     * Pushes to the right asynchronously.
     *
     * @param key    the list key (without namespace)
     * @param values the values
     * @return a future with the length
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> rpushAsync(@NotNull String key, @NotNull String... values) {
        return redisService.rpushAsync(key(key), values);
    }

    /**
     * Pops from the left.
     *
     * @param key the list key (without namespace)
     * @return the popped value
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> lpop(@NotNull String key) {
        return redisService.lpop(key(key));
    }

    /**
     * Pops from the left asynchronously.
     *
     * @param key the list key (without namespace)
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<String>> lpopAsync(@NotNull String key) {
        return redisService.lpopAsync(key(key));
    }

    /**
     * Pops from the right.
     *
     * @param key the list key (without namespace)
     * @return the popped value
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> rpop(@NotNull String key) {
        return redisService.rpop(key(key));
    }

    /**
     * Pops from the right asynchronously.
     *
     * @param key the list key (without namespace)
     * @return a future with the value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<String>> rpopAsync(@NotNull String key) {
        return redisService.rpopAsync(key(key));
    }

    /**
     * Gets a range from a list.
     *
     * @param key   the list key (without namespace)
     * @param start the start index
     * @param stop  the stop index
     * @return the elements
     * @since 1.0.0
     */
    @NotNull
    public List<String> lrange(@NotNull String key, long start, long stop) {
        return redisService.lrange(key(key), start, stop);
    }

    /**
     * Gets a list range asynchronously.
     *
     * @param key   the list key (without namespace)
     * @param start the start index
     * @param stop  the stop index
     * @return a future with the elements
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<String>> lrangeAsync(@NotNull String key, long start, long stop) {
        return redisService.lrangeAsync(key(key), start, stop);
    }

    // ========== Set Operations ==========

    /**
     * Adds members to a set.
     *
     * @param key     the set key (without namespace)
     * @param members the members
     * @return the number added
     * @since 1.0.0
     */
    public long sadd(@NotNull String key, @NotNull String... members) {
        return redisService.sadd(key(key), members);
    }

    /**
     * Adds to a set asynchronously.
     *
     * @param key     the set key (without namespace)
     * @param members the members
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> saddAsync(@NotNull String key, @NotNull String... members) {
        return redisService.saddAsync(key(key), members);
    }

    /**
     * Gets all set members.
     *
     * @param key the set key (without namespace)
     * @return the members
     * @since 1.0.0
     */
    @NotNull
    public Set<String> smembers(@NotNull String key) {
        return redisService.smembers(key(key));
    }

    /**
     * Gets set members asynchronously.
     *
     * @param key the set key (without namespace)
     * @return a future with the members
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Set<String>> smembersAsync(@NotNull String key) {
        return redisService.smembersAsync(key(key));
    }

    /**
     * Checks set membership.
     *
     * @param key    the set key (without namespace)
     * @param member the member
     * @return true if member
     * @since 1.0.0
     */
    public boolean sismember(@NotNull String key, @NotNull String member) {
        return redisService.sismember(key(key), member);
    }

    /**
     * Checks set membership asynchronously.
     *
     * @param key    the set key (without namespace)
     * @param member the member
     * @return a future with true if member
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> sismemberAsync(@NotNull String key, @NotNull String member) {
        return redisService.sismemberAsync(key(key), member);
    }

    /**
     * Removes set members.
     *
     * @param key     the set key (without namespace)
     * @param members the members
     * @return the number removed
     * @since 1.0.0
     */
    public long srem(@NotNull String key, @NotNull String... members) {
        return redisService.srem(key(key), members);
    }

    /**
     * Removes set members asynchronously.
     *
     * @param key     the set key (without namespace)
     * @param members the members
     * @return a future with the count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> sremAsync(@NotNull String key, @NotNull String... members) {
        return redisService.sremAsync(key(key), members);
    }

    // ========== Typed Operations ==========

    /**
     * Gets a typed value.
     *
     * @param <T>        the value type
     * @param key        the key (without namespace)
     * @param type       the value class
     * @param serializer the serializer
     * @return the value
     * @since 1.0.0
     */
    @NotNull
    public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type, @NotNull RedisSerializer<T> serializer) {
        return redisService.get(key(key), type, serializer);
    }

    /**
     * Sets a typed value.
     *
     * @param <T>        the value type
     * @param key        the key (without namespace)
     * @param value      the value
     * @param serializer the serializer
     * @since 1.0.0
     */
    public <T> void set(@NotNull String key, @NotNull T value, @NotNull RedisSerializer<T> serializer) {
        redisService.set(key(key), value, serializer);
    }

    /**
     * Sets a typed value with TTL.
     *
     * @param <T>        the value type
     * @param key        the key (without namespace)
     * @param value      the value
     * @param ttl        the TTL
     * @param serializer the serializer
     * @since 1.0.0
     */
    public <T> void setex(@NotNull String key, @NotNull T value, @NotNull Duration ttl, @NotNull RedisSerializer<T> serializer) {
        redisService.setex(key(key), value, ttl, serializer);
    }

    @Override
    public String toString() {
        return "KeyNamespace[prefix=" + prefix + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyNamespace that = (KeyNamespace) o;
        return Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }
}
