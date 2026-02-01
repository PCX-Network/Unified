/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

/**
 * Main service interface for cache management and creation.
 *
 * <p>CacheService provides a unified API for creating and managing various
 * types of caches including local caches, loading caches, write-behind caches,
 * and distributed caches.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private CacheService caches;
 *
 * // Create a simple local cache
 * LocalCache<UUID, PlayerData> playerCache = caches.<UUID, PlayerData>builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .expireAfterAccess(Duration.ofMinutes(30))
 *     .expireAfterWrite(Duration.ofHours(1))
 *     .recordStats(true)
 *     .build();
 *
 * // Create a loading cache
 * LoadingCache<UUID, PlayerData> loadingCache = caches.<UUID, PlayerData>loadingBuilder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .loader(uuid -> database.loadPlayerData(uuid))
 *     .refreshAfterWrite(Duration.ofMinutes(5))
 *     .build();
 *
 * // Create a write-behind cache
 * WriteBehindCache<UUID, PlayerData> writeCache = caches.<UUID, PlayerData>writeBehindBuilder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .writer(entries -> database.batchSave(entries))
 *     .writeDelay(Duration.ofSeconds(5))
 *     .batchSize(100)
 *     .build();
 *
 * // Create a distributed cache
 * DistributedCache<UUID, PlayerData> distCache = caches.<UUID, PlayerData>distributedBuilder()
 *     .name("player-data")
 *     .redisOperations(redisOps)
 *     .localCache(true)
 *     .ttl(Duration.ofHours(1))
 *     .build();
 *
 * // Use cache regions
 * CacheRegion playerRegion = caches.region("players")
 *     .defaultTtl(Duration.ofMinutes(15))
 *     .defaultMaxSize(500)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LocalCache
 * @see LoadingCache
 * @see WriteBehindCache
 * @see DistributedCache
 * @see CacheRegion
 */
public interface CacheService {

    /**
     * Creates a new builder for a local cache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new LocalCache builder
     * @since 1.0.0
     */
    @NotNull
    <K, V> LocalCache.Builder<K, V> builder();

    /**
     * Creates a new builder for a loading cache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new LoadingCache builder
     * @since 1.0.0
     */
    @NotNull
    <K, V> LoadingCache.Builder<K, V> loadingBuilder();

    /**
     * Creates a new builder for a write-behind cache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new WriteBehindCache builder
     * @since 1.0.0
     */
    @NotNull
    <K, V> WriteBehindCache.Builder<K, V> writeBehindBuilder();

    /**
     * Creates a new builder for an async cache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new AsyncCache builder
     * @since 1.0.0
     */
    @NotNull
    <K, V> AsyncCache.Builder<K, V> asyncBuilder();

    /**
     * Creates a new builder for a distributed cache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new DistributedCache builder
     * @since 1.0.0
     */
    @NotNull
    <K, V> DistributedCache.Builder<K, V> distributedBuilder();

    /**
     * Creates a new builder for a cache region.
     *
     * @param name the region name
     * @return a new CacheRegion builder
     * @since 1.0.0
     */
    @NotNull
    CacheRegion.Builder region(@NotNull String name);

    /**
     * Creates a cache with default settings.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param name the cache name
     * @return a new cache with defaults
     * @since 1.0.0
     */
    @NotNull
    default <K, V> LocalCache<K, V> create(@NotNull String name) {
        return this.<K, V>builder()
                .name(name)
                .maximumSize(CacheConfig.DEFAULT_MAXIMUM_SIZE)
                .expireAfterWrite(CacheConfig.DEFAULT_EXPIRE_AFTER_WRITE)
                .recordStats(true)
                .build();
    }

    /**
     * Creates a cache with the specified configuration.
     *
     * @param <K>    the key type
     * @param <V>    the value type
     * @param config the cache configuration
     * @return a new cache
     * @since 1.0.0
     */
    @NotNull
    <K, V> LocalCache<K, V> create(@NotNull CacheConfig config);

    /**
     * Gets a previously registered cache by name.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param name the cache name
     * @return an Optional containing the cache if found
     * @since 1.0.0
     */
    @NotNull
    <K, V> Optional<LocalCache<K, V>> getCache(@NotNull String name);

    /**
     * Gets a previously registered region by name.
     *
     * @param name the region name
     * @return an Optional containing the region if found
     * @since 1.0.0
     */
    @NotNull
    Optional<CacheRegion> getRegion(@NotNull String name);

    /**
     * Returns all registered cache names.
     *
     * @return a collection of cache names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getCacheNames();

    /**
     * Returns all registered region names.
     *
     * @return a collection of region names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getRegionNames();

    /**
     * Returns the statistics collector for this service.
     *
     * @return the stats collector
     * @since 1.0.0
     */
    @NotNull
    CacheStatsCollector statsCollector();

    /**
     * Returns the metrics interface for this service.
     *
     * @return the metrics interface
     * @since 1.0.0
     */
    @NotNull
    CacheMetrics metrics();

    /**
     * Invalidates all caches managed by this service.
     *
     * @since 1.0.0
     */
    void invalidateAll();

    /**
     * Returns aggregated statistics across all caches.
     *
     * @return the aggregate cache statistics
     * @since 1.0.0
     */
    @NotNull
    CacheStats aggregateStats();

    /**
     * Shuts down all caches and releases resources.
     *
     * <p>This will flush any write-behind caches before shutting down.
     *
     * @since 1.0.0
     */
    void shutdown();

    /**
     * Creates a default implementation of CacheService.
     *
     * @return a new CacheService instance
     * @since 1.0.0
     */
    @NotNull
    static CacheService create() {
        return new DefaultCacheService();
    }

    /**
     * Creates a CacheService with custom metrics.
     *
     * @param metrics the metrics implementation
     * @return a new CacheService instance
     * @since 1.0.0
     */
    @NotNull
    static CacheService create(@NotNull CacheMetrics metrics) {
        return new DefaultCacheService(metrics);
    }
}

/**
 * Default implementation of CacheService.
 */
class DefaultCacheService implements CacheService {

    private final java.util.Map<String, LocalCache<?, ?>> caches;
    private final java.util.Map<String, CacheRegion> regions;
    private final CacheStatsCollector statsCollector;
    private final CacheMetrics metrics;

    DefaultCacheService() {
        this(CacheMetrics.NOOP);
    }

    DefaultCacheService(@NotNull CacheMetrics metrics) {
        this.caches = new java.util.concurrent.ConcurrentHashMap<>();
        this.regions = new java.util.concurrent.ConcurrentHashMap<>();
        this.metrics = metrics;
        this.statsCollector = CacheStatsCollector.builder()
                .metrics(metrics)
                .reportInterval(Duration.ofMinutes(5))
                .build();
    }

    @Override
    @NotNull
    public <K, V> LocalCache.Builder<K, V> builder() {
        return LocalCache.builder();
    }

    @Override
    @NotNull
    public <K, V> LoadingCache.Builder<K, V> loadingBuilder() {
        return LoadingCache.loadingBuilder();
    }

    @Override
    @NotNull
    public <K, V> WriteBehindCache.Builder<K, V> writeBehindBuilder() {
        return WriteBehindCache.writeBehindBuilder();
    }

    @Override
    @NotNull
    public <K, V> AsyncCache.Builder<K, V> asyncBuilder() {
        return AsyncCache.builder();
    }

    @Override
    @NotNull
    public <K, V> DistributedCache.Builder<K, V> distributedBuilder() {
        return DistributedCache.builder();
    }

    @Override
    @NotNull
    public CacheRegion.Builder region(@NotNull String name) {
        return CacheRegion.builder(name, this);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <K, V> LocalCache<K, V> create(@NotNull CacheConfig config) {
        LocalCache<K, V> cache = new LocalCache<>(config);
        caches.put(config.name(), cache);
        statsCollector.register(config.name(), cache);
        return cache;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <K, V> Optional<LocalCache<K, V>> getCache(@NotNull String name) {
        return Optional.ofNullable((LocalCache<K, V>) caches.get(name));
    }

    @Override
    @NotNull
    public Optional<CacheRegion> getRegion(@NotNull String name) {
        return Optional.ofNullable(regions.get(name));
    }

    @Override
    @NotNull
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    @NotNull
    public Collection<String> getRegionNames() {
        return regions.keySet();
    }

    @Override
    @NotNull
    public CacheStatsCollector statsCollector() {
        return statsCollector;
    }

    @Override
    @NotNull
    public CacheMetrics metrics() {
        return metrics;
    }

    @Override
    public void invalidateAll() {
        for (LocalCache<?, ?> cache : caches.values()) {
            cache.invalidateAll();
        }
        for (CacheRegion region : regions.values()) {
            region.invalidateAll();
        }
    }

    @Override
    @NotNull
    public CacheStats aggregateStats() {
        return statsCollector.aggregateStats();
    }

    @Override
    public void shutdown() {
        statsCollector.shutdown();

        for (LocalCache<?, ?> cache : caches.values()) {
            if (cache instanceof WriteBehindCache<?, ?> wbc) {
                wbc.shutdown();
            }
        }

        caches.clear();
        regions.clear();
    }
}
