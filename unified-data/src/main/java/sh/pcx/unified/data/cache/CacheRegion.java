/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A named cache region that groups related caches with shared defaults.
 *
 * <p>CacheRegion provides a way to organize caches by concern, apply shared
 * configuration defaults, and perform bulk operations on related caches.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a region with defaults
 * CacheRegion playerRegion = caches.region("players")
 *     .defaultTtl(Duration.ofMinutes(30))
 *     .defaultMaxSize(1000)
 *     .build();
 *
 * // Create caches within the region
 * LocalCache<UUID, PlayerData> dataCache = playerRegion.cache("data");
 * LocalCache<UUID, PlayerSettings> settingsCache = playerRegion.cache("settings");
 *
 * // Invalidate entire region
 * playerRegion.invalidateAll();
 *
 * // Get region stats
 * CacheStats regionStats = playerRegion.stats();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>CacheRegion is thread-safe for all operations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CacheService
 */
public final class CacheRegion {

    private final String name;
    private final CacheService cacheService;
    private final Duration defaultTtl;
    private final long defaultMaxSize;
    private final boolean recordStats;
    private final Map<String, LocalCache<?, ?>> caches;

    /**
     * Creates a new cache region.
     *
     * @param name           the region name
     * @param cacheService   the parent cache service
     * @param defaultTtl     the default TTL for caches in this region
     * @param defaultMaxSize the default max size for caches in this region
     * @param recordStats    whether to record stats by default
     */
    private CacheRegion(
            @NotNull String name,
            @NotNull CacheService cacheService,
            @Nullable Duration defaultTtl,
            long defaultMaxSize,
            boolean recordStats) {
        this.name = name;
        this.cacheService = cacheService;
        this.defaultTtl = defaultTtl;
        this.defaultMaxSize = defaultMaxSize;
        this.recordStats = recordStats;
        this.caches = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new builder for CacheRegion.
     *
     * @param name         the region name
     * @param cacheService the parent cache service
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull String name, @NotNull CacheService cacheService) {
        return new Builder(name, cacheService);
    }

    /**
     * Returns the name of this region.
     *
     * @return the region name
     * @since 1.0.0
     */
    @NotNull
    public String name() {
        return name;
    }

    /**
     * Returns the default TTL for caches in this region.
     *
     * @return the default TTL, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public Duration defaultTtl() {
        return defaultTtl;
    }

    /**
     * Returns the default maximum size for caches in this region.
     *
     * @return the default max size
     * @since 1.0.0
     */
    public long defaultMaxSize() {
        return defaultMaxSize;
    }

    /**
     * Gets or creates a cache within this region.
     *
     * <p>If a cache with the given name already exists, it is returned.
     * Otherwise, a new cache is created with the region's default settings.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param cacheName the cache name within this region
     * @return the cache instance
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <K, V> LocalCache<K, V> cache(@NotNull String cacheName) {
        return (LocalCache<K, V>) caches.computeIfAbsent(cacheName, name -> {
            CacheConfig config = CacheConfig.builder()
                    .name(this.name + ":" + name)
                    .maximumSize(defaultMaxSize)
                    .expireAfterWrite(defaultTtl)
                    .recordStats(recordStats)
                    .build();
            return new LocalCache<>(config);
        });
    }

    /**
     * Gets or creates a cache with custom configuration.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param cacheName the cache name within this region
     * @param config    the cache configuration
     * @return the cache instance
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <K, V> LocalCache<K, V> cache(@NotNull String cacheName, @NotNull CacheConfig config) {
        return (LocalCache<K, V>) caches.computeIfAbsent(cacheName, name ->
            new LocalCache<>(config.toBuilder()
                    .name(this.name + ":" + config.name())
                    .build())
        );
    }

    /**
     * Gets an existing cache if it exists.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param cacheName the cache name
     * @return an Optional containing the cache if it exists
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <K, V> Optional<LocalCache<K, V>> getCache(@NotNull String cacheName) {
        return Optional.ofNullable((LocalCache<K, V>) caches.get(cacheName));
    }

    /**
     * Checks if a cache with the given name exists in this region.
     *
     * @param cacheName the cache name
     * @return true if the cache exists
     * @since 1.0.0
     */
    public boolean hasCache(@NotNull String cacheName) {
        return caches.containsKey(cacheName);
    }

    /**
     * Returns all cache names in this region.
     *
     * @return a collection of cache names
     * @since 1.0.0
     */
    @NotNull
    public Collection<String> cacheNames() {
        return caches.keySet();
    }

    /**
     * Returns all caches in this region.
     *
     * @return a collection of caches
     * @since 1.0.0
     */
    @NotNull
    public Collection<LocalCache<?, ?>> allCaches() {
        return caches.values();
    }

    /**
     * Invalidates all entries in all caches in this region.
     *
     * @since 1.0.0
     */
    public void invalidateAll() {
        for (LocalCache<?, ?> cache : caches.values()) {
            cache.invalidateAll();
        }
    }

    /**
     * Returns aggregated statistics for all caches in this region.
     *
     * @return the combined cache statistics
     * @since 1.0.0
     */
    @NotNull
    public CacheStats stats() {
        CacheStats combined = CacheStats.EMPTY;
        for (LocalCache<?, ?> cache : caches.values()) {
            combined = combined.plus(cache.stats());
        }
        return combined;
    }

    /**
     * Returns the number of caches in this region.
     *
     * @return the cache count
     * @since 1.0.0
     */
    public int size() {
        return caches.size();
    }

    /**
     * Returns the total number of entries across all caches in this region.
     *
     * @return the total entry count
     * @since 1.0.0
     */
    public long totalEntries() {
        return caches.values().stream()
                .mapToLong(LocalCache::size)
                .sum();
    }

    /**
     * Removes a cache from this region.
     *
     * @param cacheName the cache name to remove
     * @return true if the cache was removed
     * @since 1.0.0
     */
    public boolean removeCache(@NotNull String cacheName) {
        LocalCache<?, ?> removed = caches.remove(cacheName);
        if (removed != null) {
            removed.invalidateAll();
            return true;
        }
        return false;
    }

    /**
     * Clears all caches and removes them from this region.
     *
     * @since 1.0.0
     */
    public void clear() {
        for (LocalCache<?, ?> cache : caches.values()) {
            cache.invalidateAll();
        }
        caches.clear();
    }

    @Override
    public String toString() {
        return "CacheRegion{name='" + name + "', caches=" + caches.size() +
               ", entries=" + totalEntries() + "}";
    }

    /**
     * Builder for CacheRegion.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final String name;
        private final CacheService cacheService;
        private Duration defaultTtl;
        private long defaultMaxSize = 1000;
        private boolean recordStats = true;

        private Builder(@NotNull String name, @NotNull CacheService cacheService) {
            this.name = name;
            this.cacheService = cacheService;
        }

        /**
         * Sets the default TTL for caches in this region.
         *
         * @param ttl the default TTL
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder defaultTtl(@Nullable Duration ttl) {
            this.defaultTtl = ttl;
            return this;
        }

        /**
         * Sets the default maximum size for caches in this region.
         *
         * @param maxSize the default max size
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder defaultMaxSize(long maxSize) {
            this.defaultMaxSize = maxSize;
            return this;
        }

        /**
         * Sets whether to record stats by default.
         *
         * @param recordStats true to record stats
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        /**
         * Builds the CacheRegion.
         *
         * @return a new CacheRegion instance
         * @since 1.0.0
         */
        @NotNull
        public CacheRegion build() {
            return new CacheRegion(name, cacheService, defaultTtl, defaultMaxSize, recordStats);
        }
    }
}
