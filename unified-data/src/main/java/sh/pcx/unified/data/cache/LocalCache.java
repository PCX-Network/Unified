/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * High-performance local cache implementation backed by Caffeine.
 *
 * <p>LocalCache provides a thread-safe, in-memory cache with support for:
 * <ul>
 *   <li>Size-based eviction (LRU/LFU hybrid)</li>
 *   <li>Time-based expiration (after access or write)</li>
 *   <li>Weak and soft references for memory-sensitive caching</li>
 *   <li>Statistics recording for monitoring</li>
 *   <li>Removal listeners for cleanup actions</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a basic cache
 * LocalCache<UUID, PlayerData> cache = LocalCache.<UUID, PlayerData>builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .expireAfterWrite(Duration.ofMinutes(30))
 *     .build();
 *
 * // Basic operations
 * cache.put(uuid, playerData);
 * Optional<PlayerData> data = cache.get(uuid);
 * cache.invalidate(uuid);
 * cache.invalidateAll();
 *
 * // Compute if absent
 * PlayerData data = cache.computeIfAbsent(uuid, id -> loadFromDatabase(id));
 *
 * // Get statistics
 * CacheStats stats = cache.stats();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see CacheService
 * @see LoadingCache
 */
public class LocalCache<K, V> {

    protected final Cache<K, V> cache;
    protected final CacheConfig config;
    protected final ConflictResolver<V> conflictResolver;
    protected volatile RemovalListener<K, V> removalListener;

    /**
     * Creates a new local cache with the given configuration.
     *
     * @param config the cache configuration
     */
    public LocalCache(@NotNull CacheConfig config) {
        this(config, null, null);
    }

    /**
     * Creates a new local cache with configuration and optional handlers.
     *
     * @param config           the cache configuration
     * @param conflictResolver optional conflict resolver
     * @param removalListener  optional removal listener
     */
    public LocalCache(
            @NotNull CacheConfig config,
            @Nullable ConflictResolver<V> conflictResolver,
            @Nullable RemovalListener<K, V> removalListener) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.conflictResolver = conflictResolver;
        this.removalListener = removalListener;
        this.cache = buildCache(config, removalListener);
    }

    /**
     * Builds the underlying Caffeine cache from configuration.
     */
    private Cache<K, V> buildCache(CacheConfig config, @Nullable RemovalListener<K, V> listener) {
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

        if (config.weakValues()) {
            builder.weakValues();
        }

        if (config.softValues()) {
            builder.softValues();
        }

        if (config.recordStats()) {
            builder.recordStats();
        }

        if (listener != null) {
            builder.removalListener(listener);
        }

        @SuppressWarnings("unchecked")
        Cache<K, V> result = (Cache<K, V>) builder.build();
        return result;
    }

    /**
     * Returns the name of this cache.
     *
     * @return the cache name
     * @since 1.0.0
     */
    @NotNull
    public String name() {
        return config.name();
    }

    /**
     * Returns the configuration of this cache.
     *
     * @return the cache configuration
     * @since 1.0.0
     */
    @NotNull
    public CacheConfig config() {
        return config;
    }

    /**
     * Retrieves a value from the cache.
     *
     * @param key the key to look up
     * @return an Optional containing the value if present
     * @since 1.0.0
     */
    @NotNull
    public Optional<V> get(@NotNull K key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    /**
     * Retrieves a value, or null if not present.
     *
     * @param key the key to look up
     * @return the value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    public V getIfPresent(@NotNull K key) {
        return cache.getIfPresent(key);
    }

    /**
     * Retrieves multiple values from the cache.
     *
     * @param keys the keys to look up
     * @return a map of present key-value pairs
     * @since 1.0.0
     */
    @NotNull
    public Map<K, V> getAll(@NotNull Iterable<? extends K> keys) {
        return cache.getAllPresent(keys);
    }

    /**
     * Stores a value in the cache.
     *
     * <p>If a conflict resolver is configured, it will be used to resolve
     * conflicts with existing values.
     *
     * @param key   the key
     * @param value the value to store
     * @since 1.0.0
     */
    public void put(@NotNull K key, @NotNull V value) {
        if (conflictResolver != null) {
            cache.asMap().compute(key, (k, existing) ->
                    conflictResolver.resolve(existing, value));
        } else {
            cache.put(key, value);
        }
    }

    /**
     * Stores multiple values in the cache.
     *
     * @param entries the entries to store
     * @since 1.0.0
     */
    public void putAll(@NotNull Map<? extends K, ? extends V> entries) {
        if (conflictResolver != null) {
            entries.forEach(this::put);
        } else {
            cache.putAll(entries);
        }
    }

    /**
     * Stores a value only if not already present.
     *
     * @param key   the key
     * @param value the value to store
     * @return the existing value if present, otherwise the new value
     * @since 1.0.0
     */
    @NotNull
    public V putIfAbsent(@NotNull K key, @NotNull V value) {
        V existing = cache.asMap().putIfAbsent(key, value);
        return existing != null ? existing : value;
    }

    /**
     * Computes a value if not present.
     *
     * @param key             the key
     * @param mappingFunction function to compute the value
     * @return the current or computed value
     * @since 1.0.0
     */
    @NotNull
    public V computeIfAbsent(@NotNull K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        return cache.get(key, mappingFunction);
    }

    /**
     * Computes a new value for an existing key.
     *
     * @param key               the key
     * @param remappingFunction function to compute the new value
     * @return the computed value, or null if removed
     * @since 1.0.0
     */
    @Nullable
    public V computeIfPresent(
            @NotNull K key,
            @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return cache.asMap().computeIfPresent(key, remappingFunction);
    }

    /**
     * Computes a value, handling both present and absent cases.
     *
     * @param key               the key
     * @param remappingFunction function to compute the value
     * @return the computed value, or null if removed
     * @since 1.0.0
     */
    @Nullable
    public V compute(
            @NotNull K key,
            @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return cache.asMap().compute(key, remappingFunction);
    }

    /**
     * Invalidates (removes) a single key from the cache.
     *
     * @param key the key to invalidate
     * @since 1.0.0
     */
    public void invalidate(@NotNull K key) {
        cache.invalidate(key);
    }

    /**
     * Invalidates multiple keys from the cache.
     *
     * @param keys the keys to invalidate
     * @since 1.0.0
     */
    public void invalidateAll(@NotNull Iterable<? extends K> keys) {
        cache.invalidateAll(keys);
    }

    /**
     * Invalidates all entries in the cache.
     *
     * @since 1.0.0
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Returns the approximate number of entries in the cache.
     *
     * @return the entry count
     * @since 1.0.0
     */
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * Checks if the cache contains a key.
     *
     * @param key the key to check
     * @return true if the key is present
     * @since 1.0.0
     */
    public boolean containsKey(@NotNull K key) {
        return cache.getIfPresent(key) != null;
    }

    /**
     * Returns all keys currently in the cache.
     *
     * @return a set of all keys
     * @since 1.0.0
     */
    @NotNull
    public Set<K> keys() {
        return cache.asMap().keySet();
    }

    /**
     * Returns all values currently in the cache.
     *
     * @return a collection of all values
     * @since 1.0.0
     */
    @NotNull
    public Collection<V> values() {
        return cache.asMap().values();
    }

    /**
     * Returns a view of this cache as a ConcurrentMap.
     *
     * <p>Changes to the map are reflected in the cache and vice versa.
     *
     * @return a ConcurrentMap view
     * @since 1.0.0
     */
    @NotNull
    public ConcurrentMap<K, V> asMap() {
        return cache.asMap();
    }

    /**
     * Returns cache statistics.
     *
     * <p>Statistics are only recorded when enabled via
     * {@link CacheConfig#recordStats()}.
     *
     * @return the cache statistics
     * @since 1.0.0
     */
    @NotNull
    public CacheStats stats() {
        if (!config.recordStats()) {
            return CacheStats.builder()
                    .size(cache.estimatedSize())
                    .build();
        }

        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();
        return CacheStats.fromCaffeine(caffeineStats, cache.estimatedSize(), 0);
    }

    /**
     * Performs any pending maintenance operations.
     *
     * <p>This triggers eviction of expired entries and runs cleanup tasks.
     * Normally, maintenance is performed automatically during cache operations.
     *
     * @since 1.0.0
     */
    public void cleanUp() {
        cache.cleanUp();
    }

    /**
     * Sets the removal listener for this cache.
     *
     * <p>Note: The listener is only effective if set before entries are added.
     * For best results, set the listener during cache construction.
     *
     * @param listener the removal listener
     * @since 1.0.0
     */
    public void setRemovalListener(@Nullable RemovalListener<K, V> listener) {
        this.removalListener = listener;
    }

    /**
     * Creates a new builder for LocalCache.
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
     * Builder for LocalCache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @since 1.0.0
     */
    public static final class Builder<K, V> {

        private final CacheConfig.Builder configBuilder = CacheConfig.builder();
        private ConflictResolver<V> conflictResolver;
        private RemovalListener<K, V> removalListener;

        private Builder() {}

        /**
         * Sets the cache name.
         *
         * @param name the cache name
         * @return this builder
         */
        @NotNull
        public Builder<K, V> name(@NotNull String name) {
            configBuilder.name(name);
            return this;
        }

        /**
         * Sets the maximum number of entries.
         *
         * @param maximumSize the maximum size
         * @return this builder
         */
        @NotNull
        public Builder<K, V> maximumSize(long maximumSize) {
            configBuilder.maximumSize(maximumSize);
            return this;
        }

        /**
         * Sets the maximum total weight of entries.
         *
         * @param maximumWeight the maximum weight
         * @return this builder
         */
        @NotNull
        public Builder<K, V> maximumWeight(long maximumWeight) {
            configBuilder.maximumWeight(maximumWeight);
            return this;
        }

        /**
         * Sets the time after last access before expiration.
         *
         * @param duration the expiration duration
         * @return this builder
         */
        @NotNull
        public Builder<K, V> expireAfterAccess(@NotNull java.time.Duration duration) {
            configBuilder.expireAfterAccess(duration);
            return this;
        }

        /**
         * Sets the time after write before expiration.
         *
         * @param duration the expiration duration
         * @return this builder
         */
        @NotNull
        public Builder<K, V> expireAfterWrite(@NotNull java.time.Duration duration) {
            configBuilder.expireAfterWrite(duration);
            return this;
        }

        /**
         * Enables weak references for keys.
         *
         * @param weakKeys true to use weak keys
         * @return this builder
         */
        @NotNull
        public Builder<K, V> weakKeys(boolean weakKeys) {
            configBuilder.weakKeys(weakKeys);
            return this;
        }

        /**
         * Enables weak references for values.
         *
         * @param weakValues true to use weak values
         * @return this builder
         */
        @NotNull
        public Builder<K, V> weakValues(boolean weakValues) {
            configBuilder.weakValues(weakValues);
            return this;
        }

        /**
         * Enables soft references for values.
         *
         * @param softValues true to use soft values
         * @return this builder
         */
        @NotNull
        public Builder<K, V> softValues(boolean softValues) {
            configBuilder.softValues(softValues);
            return this;
        }

        /**
         * Enables statistics recording.
         *
         * @param recordStats true to record stats
         * @return this builder
         */
        @NotNull
        public Builder<K, V> recordStats(boolean recordStats) {
            configBuilder.recordStats(recordStats);
            return this;
        }

        /**
         * Sets the conflict resolver for handling concurrent updates.
         *
         * @param resolver the conflict resolver
         * @return this builder
         */
        @NotNull
        public Builder<K, V> conflictResolver(@Nullable ConflictResolver<V> resolver) {
            this.conflictResolver = resolver;
            return this;
        }

        /**
         * Sets the removal listener for handling evictions.
         *
         * @param listener the removal listener
         * @return this builder
         */
        @NotNull
        public Builder<K, V> removalListener(@Nullable RemovalListener<K, V> listener) {
            this.removalListener = listener;
            return this;
        }

        /**
         * Builds the LocalCache instance.
         *
         * @return a new LocalCache
         */
        @NotNull
        public LocalCache<K, V> build() {
            return new LocalCache<>(configBuilder.build(), conflictResolver, removalListener);
        }
    }

    @Override
    public String toString() {
        return "LocalCache{name='" + config.name() + "', size=" + size() + "}";
    }
}
