/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for Redis Lua script execution.
 *
 * <p>Lua scripts in Redis provide atomic operations and can reduce network
 * round-trips by executing multiple commands in a single call. Scripts are
 * cached on the Redis server using their SHA1 hash.
 *
 * <h2>Script Execution</h2>
 * <pre>{@code
 * // Load a script
 * RedisLuaScript script = redis.loadScript("""
 *     local current = redis.call('GET', KEYS[1])
 *     if current == false then
 *         current = 0
 *     end
 *     local new = tonumber(current) + tonumber(ARGV[1])
 *     redis.call('SET', KEYS[1], new)
 *     return new
 *     """);
 *
 * // Execute the script
 * Long result = script.execute(Long.class, List.of("counter"), List.of("5"));
 * System.out.println("New value: " + result);
 *
 * // Async execution
 * script.executeAsync(List.of("counter"), List.of("10"))
 *     .thenAccept(value -> System.out.println("Incremented to: " + value));
 * }</pre>
 *
 * <h2>Common Patterns</h2>
 * <pre>{@code
 * // Atomic compare-and-set
 * RedisLuaScript cas = redis.loadScript("""
 *     if redis.call('GET', KEYS[1]) == ARGV[1] then
 *         return redis.call('SET', KEYS[1], ARGV[2])
 *     else
 *         return nil
 *     end
 *     """);
 * String result = cas.execute(String.class, List.of("key"), List.of("oldValue", "newValue"));
 *
 * // Distributed lock with expiration
 * RedisLuaScript lock = redis.loadScript("""
 *     if redis.call('SETNX', KEYS[1], ARGV[1]) == 1 then
 *         redis.call('PEXPIRE', KEYS[1], ARGV[2])
 *         return 1
 *     end
 *     return 0
 *     """);
 * boolean acquired = lock.execute(Long.class, List.of("lock:resource"), List.of(lockId, "30000")) == 1;
 *
 * // Rate limiting
 * RedisLuaScript rateLimit = redis.loadScript("""
 *     local current = redis.call('INCR', KEYS[1])
 *     if current == 1 then
 *         redis.call('EXPIRE', KEYS[1], ARGV[1])
 *     end
 *     return current
 *     """);
 * long count = rateLimit.execute(Long.class, List.of("ratelimit:" + userId), List.of("60"));
 * boolean allowed = count <= maxRequestsPerMinute;
 * }</pre>
 *
 * <h2>Script Caching</h2>
 * <p>Scripts are automatically cached on the Redis server. The SHA1 hash is
 * used for subsequent executions, reducing bandwidth. If the script is evicted
 * from the cache, it is automatically reloaded.
 *
 * <h2>Built-in Scripts</h2>
 * <p>Use the static factory methods for common operations:
 * <ul>
 *   <li>{@link #compareAndSet()} - Atomic CAS operation</li>
 *   <li>{@link #incrementWithLimit()} - Bounded increment</li>
 *   <li>{@link #acquireLock()} - Distributed lock acquisition</li>
 *   <li>{@link #releaseLock()} - Distributed lock release</li>
 *   <li>{@link #rateLimitSlidingWindow()} - Sliding window rate limit</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisService#loadScript(String)
 */
public interface RedisLuaScript {

    /**
     * Gets the Lua script source code.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    String getScript();

    /**
     * Gets the SHA1 hash of the script.
     *
     * <p>This hash is used for EVALSHA commands to avoid sending
     * the entire script on each execution.
     *
     * @return the SHA1 hash
     * @since 1.0.0
     */
    @NotNull
    String getSha1();

    /**
     * Checks if the script is loaded on the Redis server.
     *
     * @return true if the script is cached on the server
     * @since 1.0.0
     */
    boolean isLoaded();

    /**
     * Checks if the script is loaded asynchronously.
     *
     * @return a future that completes with true if loaded
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> isLoadedAsync();

    /**
     * Ensures the script is loaded on the Redis server.
     *
     * <p>If the script is not cached, it will be loaded.
     *
     * @since 1.0.0
     */
    void ensureLoaded();

    /**
     * Ensures the script is loaded asynchronously.
     *
     * @return a future that completes when loaded
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> ensureLoadedAsync();

    // ========== Execution Methods ==========

    /**
     * Executes the script with the given keys and arguments.
     *
     * @param keys the keys to pass (KEYS in Lua)
     * @param args the arguments to pass (ARGV in Lua)
     * @return the script result
     * @since 1.0.0
     */
    @Nullable
    Object execute(@NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Executes the script asynchronously.
     *
     * @param keys the keys to pass
     * @param args the arguments to pass
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Object> executeAsync(@NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Executes the script with typed result.
     *
     * @param <T>        the result type
     * @param resultType the expected result class
     * @param keys       the keys to pass
     * @param args       the arguments to pass
     * @return the script result cast to the expected type
     * @since 1.0.0
     */
    @Nullable
    <T> T execute(@NotNull Class<T> resultType, @NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Executes the script with typed result asynchronously.
     *
     * @param <T>        the result type
     * @param resultType the expected result class
     * @param keys       the keys to pass
     * @param args       the arguments to pass
     * @return a future with the typed result
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<T> executeAsync(@NotNull Class<T> resultType, @NotNull List<String> keys, @NotNull List<String> args);

    /**
     * Executes the script with no keys.
     *
     * @param args the arguments to pass
     * @return the script result
     * @since 1.0.0
     */
    @Nullable
    default Object execute(@NotNull List<String> args) {
        return execute(Collections.emptyList(), args);
    }

    /**
     * Executes the script with no keys asynchronously.
     *
     * @param args the arguments to pass
     * @return a future with the result
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Object> executeAsync(@NotNull List<String> args) {
        return executeAsync(Collections.emptyList(), args);
    }

    /**
     * Executes the script with a single key and arguments.
     *
     * @param key  the key
     * @param args the arguments
     * @return the script result
     * @since 1.0.0
     */
    @Nullable
    default Object execute(@NotNull String key, @NotNull String... args) {
        return execute(List.of(key), Arrays.asList(args));
    }

    /**
     * Executes the script with a single key asynchronously.
     *
     * @param key  the key
     * @param args the arguments
     * @return a future with the result
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Object> executeAsync(@NotNull String key, @NotNull String... args) {
        return executeAsync(List.of(key), Arrays.asList(args));
    }

    // ========== Built-in Scripts ==========

    /**
     * Creates a compare-and-set script.
     *
     * <p>Usage: {@code script.execute(List.of(key), List.of(expected, newValue))}
     *
     * <p>Returns "OK" if the swap succeeded, null otherwise.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String compareAndSet() {
        return """
                local current = redis.call('GET', KEYS[1])
                if current == ARGV[1] then
                    return redis.call('SET', KEYS[1], ARGV[2])
                else
                    return nil
                end
                """;
    }

    /**
     * Creates an increment-with-limit script.
     *
     * <p>Usage: {@code script.execute(List.of(key), List.of(increment, maxValue))}
     *
     * <p>Returns the new value, or -1 if limit would be exceeded.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String incrementWithLimit() {
        return """
                local current = tonumber(redis.call('GET', KEYS[1]) or '0')
                local increment = tonumber(ARGV[1])
                local limit = tonumber(ARGV[2])
                local new = current + increment
                if new > limit then
                    return -1
                end
                redis.call('SET', KEYS[1], new)
                return new
                """;
    }

    /**
     * Creates a distributed lock acquisition script.
     *
     * <p>Usage: {@code script.execute(List.of(lockKey), List.of(lockValue, ttlMillis))}
     *
     * <p>Returns 1 if lock acquired, 0 otherwise.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String acquireLock() {
        return """
                if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
                    return 1
                else
                    return 0
                end
                """;
    }

    /**
     * Creates a distributed lock release script.
     *
     * <p>Usage: {@code script.execute(List.of(lockKey), List.of(lockValue))}
     *
     * <p>Returns 1 if lock released, 0 if not owned.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String releaseLock() {
        return """
                if redis.call('GET', KEYS[1]) == ARGV[1] then
                    return redis.call('DEL', KEYS[1])
                else
                    return 0
                end
                """;
    }

    /**
     * Creates a sliding window rate limiter script.
     *
     * <p>Usage: {@code script.execute(List.of(key), List.of(limit, windowSizeSeconds))}
     *
     * <p>Returns 1 if allowed, 0 if rate limited.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String rateLimitSlidingWindow() {
        return """
                local key = KEYS[1]
                local limit = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local now = redis.call('TIME')
                local now_ms = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
                local window_start = now_ms - (window * 1000)

                redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
                local count = redis.call('ZCARD', key)

                if count < limit then
                    redis.call('ZADD', key, now_ms, now_ms .. '-' .. math.random())
                    redis.call('PEXPIRE', key, window * 1000)
                    return 1
                else
                    return 0
                end
                """;
    }

    /**
     * Creates a fixed window rate limiter script.
     *
     * <p>Usage: {@code script.execute(List.of(key), List.of(limit, windowSeconds))}
     *
     * <p>Returns remaining requests, or -1 if rate limited.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String rateLimitFixedWindow() {
        return """
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[2])
                end
                local limit = tonumber(ARGV[1])
                if current > limit then
                    return -1
                end
                return limit - current
                """;
    }

    /**
     * Creates a deduplication script that sets a value only if not recently set.
     *
     * <p>Usage: {@code script.execute(List.of(key), List.of(value, ttlSeconds))}
     *
     * <p>Returns 1 if set (new), 0 if duplicate.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String deduplicate() {
        return """
                if redis.call('EXISTS', KEYS[1]) == 1 then
                    return 0
                end
                redis.call('SETEX', KEYS[1], ARGV[2], ARGV[1])
                return 1
                """;
    }

    /**
     * Creates a get-and-delete script for atomic pop.
     *
     * <p>Usage: {@code script.execute(List.of(key), List.of())}
     *
     * <p>Returns the value and deletes the key atomically.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String getAndDelete() {
        return """
                local value = redis.call('GET', KEYS[1])
                if value then
                    redis.call('DEL', KEYS[1])
                end
                return value
                """;
    }

    /**
     * Creates a script to atomically move a value between keys.
     *
     * <p>Usage: {@code script.execute(List.of(sourceKey, destKey), List.of())}
     *
     * <p>Returns the moved value, or nil if source doesn't exist.
     *
     * @return the script source
     * @since 1.0.0
     */
    @NotNull
    static String atomicMove() {
        return """
                local value = redis.call('GET', KEYS[1])
                if value then
                    redis.call('SET', KEYS[2], value)
                    redis.call('DEL', KEYS[1])
                end
                return value
                """;
    }

    /**
     * Abstract base implementation for RedisLuaScript.
     *
     * @since 1.0.0
     */
    abstract class AbstractLuaScript implements RedisLuaScript {

        protected final String script;
        protected final String sha1;

        /**
         * Creates a new script wrapper.
         *
         * @param script the Lua script source
         * @param sha1   the SHA1 hash
         */
        protected AbstractLuaScript(@NotNull String script, @NotNull String sha1) {
            this.script = Objects.requireNonNull(script, "script cannot be null");
            this.sha1 = Objects.requireNonNull(sha1, "sha1 cannot be null");
        }

        @Override
        @NotNull
        public String getScript() {
            return script;
        }

        @Override
        @NotNull
        public String getSha1() {
            return sha1;
        }

        @SuppressWarnings("unchecked")
        @Override
        @Nullable
        public <T> T execute(@NotNull Class<T> resultType, @NotNull List<String> keys, @NotNull List<String> args) {
            Object result = execute(keys, args);
            if (result == null) {
                return null;
            }
            return (T) result;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NotNull
        public <T> CompletableFuture<T> executeAsync(@NotNull Class<T> resultType, @NotNull List<String> keys, @NotNull List<String> args) {
            return executeAsync(keys, args).thenApply(result -> (T) result);
        }

        @Override
        public String toString() {
            return "RedisLuaScript[sha1=" + sha1.substring(0, 8) + "...]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AbstractLuaScript that = (AbstractLuaScript) o;
            return Objects.equals(sha1, that.sha1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sha1);
        }
    }
}
