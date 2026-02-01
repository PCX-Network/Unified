/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A cache that provides asynchronous operations for all cache methods.
 *
 * <p>AsyncCache wraps the underlying cache operations in CompletableFutures,
 * allowing non-blocking cache access. This is useful for I/O-bound operations
 * or when integrating with reactive/async frameworks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create an async cache
 * AsyncCache<UUID, PlayerData> cache = AsyncCache.<UUID, PlayerData>builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .expireAfterWrite(Duration.ofMinutes(30))
 *     .build();
 *
 * // Async operations
 * CompletableFuture<Optional<PlayerData>> future = cache.get(uuid);
 *
 * // Chain operations
 * cache.get(uuid)
 *     .thenApply(opt -> opt.orElseGet(() -> loadFromDatabase(uuid)))
 *     .thenAccept(data -> cache.put(uuid, data));
 *
 * // Compute if absent
 * CompletableFuture<PlayerData> data = cache.computeIfAbsent(uuid,
 *     id -> CompletableFuture.supplyAsync(() -> loadFromDatabase(id)));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe and non-blocking.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see LocalCache
 * @see LoadingCache
 */
public class AsyncCache<K, V> {

    private final com.github.benmanes.caffeine.cache.AsyncCache<K, V> cache;
    private final CacheConfig config;
    private final Executor executor;

    /**
     * Creates a new async cache.
     *
     * @param config   the cache configuration
     * @param executor the executor for async operations
     */
    private AsyncCache(@NotNull CacheConfig config, @Nullable Executor executor) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.executor = executor != null ? executor : ForkJoinPool.commonPool();
        this.cache = buildCache(config, this.executor);
    }

    private com.github.benmanes.caffeine.cache.AsyncCache<K, V> buildCache(
            CacheConfig config, Executor executor) {

        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (config.maximumSize() > 0) {
            builder.maximumSize(config.maximumSize());
        }

        if (config.maximumWeight() > 0) {
            builder.maximumWeight(config.maximumWeight());
        }

        if (config.expireAfterAccess() != null) {
            builder.expireAfterAccess(config.expireAfterAccess());
        }

        if (config.expireAfterWrite() != null) {
            builder.expireAfterWrite(config.expireAfterWrite());
        }

        if (config.weakKeys()) {
            builder.weakKeys();
        }

        if (config.recordStats()) {
            builder.recordStats();
        }

        builder.executor(executor);

        @SuppressWarnings("unchecked")
        com.github.benmanes.caffeine.cache.AsyncCache<K, V> result =
                (com.github.benmanes.caffeine.cache.AsyncCache<K, V>) builder.buildAsync();
        return result;
    }

    /**
     * Returns the cache name.
     *
     * @return the name
     * @since 1.0.0
     */
    @NotNull
    public String name() {
        return config.name();
    }

    /**
     * Returns the cache configuration.
     *
     * @return the configuration
     * @since 1.0.0
     */
    @NotNull
    public CacheConfig config() {
        return config;
    }

    /**
     * Returns the executor used for async operations.
     *
     * @return the executor
     * @since 1.0.0
     */
    @NotNull
    public Executor executor() {
        return executor;
    }

    /**
     * Asynchronously retrieves a value from the cache.
     *
     * @param key the key to look up
     * @return a future containing an Optional with the value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<V>> get(@NotNull K key) {
        CompletableFuture<V> future = cache.getIfPresent(key);
        if (future == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return future.thenApply(Optional::ofNullable);
    }

    /**
     * Asynchronously retrieves a value, returning null if not present.
     *
     * @param key the key to look up
     * @return a future with the value or null
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<V> getIfPresent(@NotNull K key) {
        CompletableFuture<V> future = cache.getIfPresent(key);
        return future != null ? future : CompletableFuture.completedFuture(null);
    }

    /**
     * Asynchronously retrieves multiple values.
     *
     * @param keys the keys to look up
     * @return a future with a map of present key-value pairs
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Map<K, V>> getAll(@NotNull Set<? extends K> keys) {
        return CompletableFuture.supplyAsync(() -> {
            return cache.synchronous().getAllPresent(keys);
        }, executor);
    }

    /**
     * Asynchronously stores a value in the cache.
     *
     * @param key   the key
     * @param value the value to store
     * @return a future that completes when the value is stored
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> put(@NotNull K key, @NotNull V value) {
        return CompletableFuture.runAsync(() -> {
            cache.put(key, CompletableFuture.completedFuture(value));
        }, executor);
    }

    /**
     * Asynchronously stores a value as a future.
     *
     * <p>The value will be stored when the future completes.
     *
     * @param key         the key
     * @param valueFuture a future that will produce the value
     * @return a future that completes when the value is stored
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> put(@NotNull K key, @NotNull CompletableFuture<V> valueFuture) {
        cache.put(key, valueFuture);
        return valueFuture.thenApply(v -> null);
    }

    /**
     * Asynchronously stores multiple values.
     *
     * @param entries the entries to store
     * @return a future that completes when all values are stored
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> putAll(@NotNull Map<? extends K, ? extends V> entries) {
        return CompletableFuture.runAsync(() -> {
            entries.forEach((k, v) -> cache.put(k, CompletableFuture.completedFuture(v)));
        }, executor);
    }

    /**
     * Asynchronously computes a value if not present.
     *
     * @param key             the key
     * @param mappingFunction function to compute the value
     * @return a future with the current or computed value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<V> computeIfAbsent(
            @NotNull K key,
            @NotNull Function<? super K, ? extends V> mappingFunction) {
        return cache.get(key, (k, exec) -> CompletableFuture.supplyAsync(() -> mappingFunction.apply(k), exec));
    }

    /**
     * Asynchronously computes a value if not present using an async function.
     *
     * @param key             the key
     * @param mappingFunction async function to compute the value
     * @return a future with the current or computed value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<V> computeIfAbsentAsync(
            @NotNull K key,
            @NotNull Function<? super K, ? extends CompletableFuture<V>> mappingFunction) {
        return cache.get(key, (k, exec) -> mappingFunction.apply(k));
    }

    /**
     * Asynchronously computes a new value for an existing key.
     *
     * @param key               the key
     * @param remappingFunction function to compute the new value
     * @return a future with the computed value, or empty if removed
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<V>> computeIfPresent(
            @NotNull K key,
            @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return CompletableFuture.supplyAsync(() -> {
            V result = cache.synchronous().asMap().computeIfPresent(key, remappingFunction);
            return Optional.ofNullable(result);
        }, executor);
    }

    /**
     * Asynchronously invalidates a key.
     *
     * @param key the key to invalidate
     * @return a future that completes when invalidated
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> invalidate(@NotNull K key) {
        return CompletableFuture.runAsync(() -> {
            cache.synchronous().invalidate(key);
        }, executor);
    }

    /**
     * Asynchronously invalidates multiple keys.
     *
     * @param keys the keys to invalidate
     * @return a future that completes when all are invalidated
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> invalidateAll(@NotNull Iterable<? extends K> keys) {
        return CompletableFuture.runAsync(() -> {
            cache.synchronous().invalidateAll(keys);
        }, executor);
    }

    /**
     * Asynchronously invalidates all entries.
     *
     * @return a future that completes when all are invalidated
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> invalidateAll() {
        return CompletableFuture.runAsync(() -> {
            cache.synchronous().invalidateAll();
        }, executor);
    }

    /**
     * Returns the approximate number of entries.
     *
     * @return the entry count
     * @since 1.0.0
     */
    public long size() {
        return cache.synchronous().estimatedSize();
    }

    /**
     * Returns the synchronous view from the underlying Caffeine cache.
     *
     * @return the synchronous cache
     * @since 1.0.0
     */
    @NotNull
    public com.github.benmanes.caffeine.cache.Cache<K, V> synchronous() {
        return cache.synchronous();
    }

    /**
     * Returns a view of this cache as a ConcurrentMap of futures.
     *
     * @return a ConcurrentMap view
     * @since 1.0.0
     */
    @NotNull
    public ConcurrentMap<K, CompletableFuture<V>> asMap() {
        return cache.asMap();
    }

    /**
     * Returns cache statistics.
     *
     * @return the cache statistics
     * @since 1.0.0
     */
    @NotNull
    public CacheStats stats() {
        if (!config.recordStats()) {
            return CacheStats.builder()
                    .size(cache.synchronous().estimatedSize())
                    .build();
        }

        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats =
                cache.synchronous().stats();
        return CacheStats.fromCaffeine(caffeineStats, cache.synchronous().estimatedSize(), 0);
    }

    /**
     * Performs any pending maintenance operations.
     *
     * @since 1.0.0
     */
    public void cleanUp() {
        cache.synchronous().cleanUp();
    }

    /**
     * Creates a new builder for AsyncCache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Builder for AsyncCache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @since 1.0.0
     */
    public static final class Builder<K, V> {

        private final CacheConfig.Builder configBuilder = CacheConfig.builder();
        private Executor executor;

        private Builder() {}

        /**
         * Sets the cache name.
         */
        @NotNull
        public Builder<K, V> name(@NotNull String name) {
            configBuilder.name(name);
            return this;
        }

        /**
         * Sets the maximum number of entries.
         */
        @NotNull
        public Builder<K, V> maximumSize(long maximumSize) {
            configBuilder.maximumSize(maximumSize);
            return this;
        }

        /**
         * Sets the time after last access before expiration.
         */
        @NotNull
        public Builder<K, V> expireAfterAccess(@NotNull Duration duration) {
            configBuilder.expireAfterAccess(duration);
            return this;
        }

        /**
         * Sets the time after write before expiration.
         */
        @NotNull
        public Builder<K, V> expireAfterWrite(@NotNull Duration duration) {
            configBuilder.expireAfterWrite(duration);
            return this;
        }

        /**
         * Enables weak references for keys.
         */
        @NotNull
        public Builder<K, V> weakKeys(boolean weakKeys) {
            configBuilder.weakKeys(weakKeys);
            return this;
        }

        /**
         * Enables statistics recording.
         */
        @NotNull
        public Builder<K, V> recordStats(boolean recordStats) {
            configBuilder.recordStats(recordStats);
            return this;
        }

        /**
         * Sets the executor for async operations.
         */
        @NotNull
        public Builder<K, V> executor(@Nullable Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Builds the AsyncCache instance.
         *
         * @return a new AsyncCache
         */
        @NotNull
        public AsyncCache<K, V> build() {
            return new AsyncCache<>(configBuilder.build(), executor);
        }
    }

    @Override
    public String toString() {
        return "AsyncCache{name='" + config.name() + "', size=" + size() + "}";
    }
}
