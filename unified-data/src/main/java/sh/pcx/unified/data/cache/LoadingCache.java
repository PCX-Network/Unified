/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * A cache that automatically loads values on cache miss.
 *
 * <p>LoadingCache extends LocalCache with automatic value loading. When a
 * requested key is not present, the configured loader is invoked to fetch
 * the value. This provides a read-through caching pattern.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a loading cache
 * LoadingCache<UUID, PlayerData> cache = LoadingCache.<UUID, PlayerData>builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .expireAfterWrite(Duration.ofMinutes(30))
 *     .loader(uuid -> database.loadPlayerData(uuid))
 *     .refreshAfterWrite(Duration.ofMinutes(5))
 *     .build();
 *
 * // Get will auto-load if missing
 * PlayerData data = cache.get(uuid);  // Never returns null for valid keys
 *
 * // Async loading
 * CompletableFuture<PlayerData> future = cache.getAsync(uuid);
 *
 * // Bulk loading
 * Map<UUID, PlayerData> data = cache.getAll(uuidSet);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe. The loader may be invoked from
 * multiple threads concurrently for different keys, but only one
 * load is performed per key at a time.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see CacheLoader
 * @see LocalCache
 */
public class LoadingCache<K, V> extends LocalCache<K, V> {

    private final com.github.benmanes.caffeine.cache.LoadingCache<K, V> loadingCache;
    private final CacheLoader<K, V> loader;
    private final Executor executor;

    /**
     * Creates a new loading cache with the given configuration and loader.
     *
     * @param config           the cache configuration
     * @param loader           the cache loader
     * @param executor         the executor for async operations
     * @param conflictResolver optional conflict resolver
     * @param removalListener  optional removal listener
     */
    @SuppressWarnings("unchecked")
    private LoadingCache(
            @NotNull CacheConfig config,
            @NotNull CacheLoader<K, V> loader,
            @Nullable Executor executor,
            @Nullable ConflictResolver<V> conflictResolver,
            @Nullable RemovalListener<K, V> removalListener) {
        super(config, conflictResolver, removalListener);
        this.loader = Objects.requireNonNull(loader, "loader cannot be null");
        this.executor = executor != null ? executor : ForkJoinPool.commonPool();
        this.loadingCache = buildLoadingCache(config, loader, this.executor, removalListener);
    }

    private com.github.benmanes.caffeine.cache.LoadingCache<K, V> buildLoadingCache(
            CacheConfig config,
            CacheLoader<K, V> loader,
            Executor executor,
            @Nullable RemovalListener<K, V> listener) {

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

        if (config.refreshAfterWrite() != null) {
            builder.refreshAfterWrite(config.refreshAfterWrite());
        }

        if (config.weakKeys()) {
            builder.weakKeys();
        }

        if (config.weakValues()) {
            builder.weakValues();
        }

        if (config.softValues()) {
            builder.softValues();
        }

        if (config.recordStats()) {
            builder.recordStats();
        }

        builder.executor(executor);

        if (listener != null) {
            builder.removalListener(listener);
        }

        @SuppressWarnings("unchecked")
        com.github.benmanes.caffeine.cache.LoadingCache<K, V> result =
                (com.github.benmanes.caffeine.cache.LoadingCache<K, V>) builder.build(
                        (Object key) -> {
                            try {
                                return loader.load((K) key);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to load value for key: " + key, e);
                            }
                        }
                );
        return result;
    }

    /**
     * Returns the cache loader.
     *
     * @return the loader
     * @since 1.0.0
     */
    @NotNull
    public CacheLoader<K, V> loader() {
        return loader;
    }

    /**
     * Gets a value from the cache, loading it if necessary.
     *
     * <p>If the key is not present, the loader is invoked synchronously
     * to fetch the value. The loaded value is cached before being returned.
     *
     * @param key the key to look up
     * @return the cached or loaded value
     * @throws RuntimeException if the loader throws an exception
     * @since 1.0.0
     */
    @NotNull
    public V getOrLoad(@NotNull K key) {
        V value = loadingCache.get(key);
        if (value == null) {
            throw new NullPointerException("Loader returned null for key: " + key);
        }
        return value;
    }

    /**
     * Gets a value from the cache, loading it asynchronously if necessary.
     *
     * @param key the key to look up
     * @return a future that completes with the value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<V> getOrLoadAsync(@NotNull K key) {
        return CompletableFuture.supplyAsync(() -> getOrLoad(key), executor);
    }

    /**
     * Gets multiple values from the cache, loading missing ones.
     *
     * <p>Keys that are present in the cache are returned immediately.
     * Missing keys are loaded using the loader's bulk load method if
     * available, otherwise loaded individually.
     *
     * @param keys the keys to look up
     * @return a map of key-value pairs
     * @since 1.0.0
     */
    @NotNull
    public Map<K, V> getAll(@NotNull Set<? extends K> keys) {
        return loadingCache.getAll(keys);
    }

    /**
     * Gets multiple values asynchronously.
     *
     * @param keys the keys to look up
     * @return a future that completes with the key-value map
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Map<K, V>> getAllAsync(@NotNull Set<? extends K> keys) {
        return CompletableFuture.supplyAsync(() -> getAll(keys), executor);
    }

    /**
     * Refreshes a value in the background.
     *
     * <p>The current value (if any) remains accessible while the refresh
     * is in progress. Once the new value is loaded, it replaces the old one.
     *
     * @param key the key to refresh
     * @since 1.0.0
     */
    public void refresh(@NotNull K key) {
        loadingCache.refresh(key);
    }

    /**
     * Refreshes multiple values in the background.
     *
     * @param keys the keys to refresh
     * @since 1.0.0
     */
    public void refreshAll(@NotNull Iterable<? extends K> keys) {
        for (K key : keys) {
            refresh(key);
        }
    }

    /**
     * Returns a future that completes when the value is loaded or refreshed.
     *
     * @param key the key to refresh
     * @return a future that completes with the refreshed value
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<V> refreshAsync(@NotNull K key) {
        return CompletableFuture.supplyAsync(() -> {
            refresh(key);
            return getOrLoad(key);
        }, executor);
    }

    @Override
    @NotNull
    public CacheStats stats() {
        if (!config.recordStats()) {
            return CacheStats.builder()
                    .size(loadingCache.estimatedSize())
                    .build();
        }

        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = loadingCache.stats();
        return CacheStats.fromCaffeine(caffeineStats, loadingCache.estimatedSize(), 0);
    }

    @Override
    public long size() {
        return loadingCache.estimatedSize();
    }

    @Override
    public void invalidate(@NotNull K key) {
        loadingCache.invalidate(key);
    }

    @Override
    public void invalidateAll(@NotNull Iterable<? extends K> keys) {
        loadingCache.invalidateAll(keys);
    }

    @Override
    public void invalidateAll() {
        loadingCache.invalidateAll();
    }

    @Override
    public void cleanUp() {
        loadingCache.cleanUp();
    }

    /**
     * Creates a new builder for LoadingCache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <K, V> Builder<K, V> loadingBuilder() {
        return new Builder<>();
    }

    /**
     * Builder for LoadingCache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @since 1.0.0
     */
    public static final class Builder<K, V> {

        private final CacheConfig.Builder configBuilder = CacheConfig.builder();
        private CacheLoader<K, V> loader;
        private Executor executor;
        private ConflictResolver<V> conflictResolver;
        private RemovalListener<K, V> removalListener;

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
         * Sets the time after write before background refresh.
         */
        @NotNull
        public Builder<K, V> refreshAfterWrite(@NotNull Duration duration) {
            configBuilder.refreshAfterWrite(duration);
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
         * Enables weak references for values.
         */
        @NotNull
        public Builder<K, V> weakValues(boolean weakValues) {
            configBuilder.weakValues(weakValues);
            return this;
        }

        /**
         * Enables soft references for values.
         */
        @NotNull
        public Builder<K, V> softValues(boolean softValues) {
            configBuilder.softValues(softValues);
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
         * Sets the cache loader.
         */
        @NotNull
        public Builder<K, V> loader(@NotNull CacheLoader<K, V> loader) {
            this.loader = loader;
            return this;
        }

        /**
         * Sets the cache loader using a simple function.
         */
        @NotNull
        public Builder<K, V> loader(@NotNull java.util.function.Function<K, V> loader) {
            this.loader = CacheLoader.from(loader);
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
         * Sets the conflict resolver.
         */
        @NotNull
        public Builder<K, V> conflictResolver(@Nullable ConflictResolver<V> resolver) {
            this.conflictResolver = resolver;
            return this;
        }

        /**
         * Sets the removal listener.
         */
        @NotNull
        public Builder<K, V> removalListener(@Nullable RemovalListener<K, V> listener) {
            this.removalListener = listener;
            return this;
        }

        /**
         * Builds the LoadingCache instance.
         *
         * @return a new LoadingCache
         * @throws IllegalStateException if loader is not set
         */
        @NotNull
        public LoadingCache<K, V> build() {
            if (loader == null) {
                throw new IllegalStateException("loader is required");
            }
            return new LoadingCache<>(configBuilder.build(), loader, executor, conflictResolver, removalListener);
        }
    }

    @Override
    public String toString() {
        return "LoadingCache{name='" + config.name() + "', size=" + size() + "}";
    }
}
