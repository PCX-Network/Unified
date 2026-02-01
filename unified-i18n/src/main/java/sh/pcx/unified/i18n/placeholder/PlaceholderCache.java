/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Cache for placeholder resolution results.
 *
 * <p>The placeholder cache stores computed placeholder values to avoid redundant
 * calculations, especially for expensive operations like database queries or
 * API calls. Each cached entry has a configurable TTL after which it expires.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a cache with default settings
 * PlaceholderCache cache = PlaceholderCache.create();
 *
 * // Create a cache with custom settings
 * PlaceholderCache customCache = PlaceholderCache.builder()
 *     .defaultTTL(CacheTTL.SECONDS_30)
 *     .maxSize(10000)
 *     .cleanupInterval(CacheTTL.MINUTES_1)
 *     .build();
 *
 * // Cache a value
 * cache.put("server_tps", "20.0", CacheTTL.SECONDS_5);
 *
 * // Get cached value
 * Optional<String> tps = cache.get("server_tps");
 *
 * // Get or compute
 * String name = cache.getOrCompute("server_name", CacheTTL.HOURS_1, () -> server.getName());
 *
 * // Player-specific cache
 * cache.put(playerId, "player_health", "20.0", CacheTTL.SECONDS_1);
 * Optional<String> health = cache.get(playerId, "player_health");
 *
 * // Invalidation
 * cache.invalidate("server_tps");
 * cache.invalidatePlayer(playerId);
 * cache.invalidateAll();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>PlaceholderCache is fully thread-safe and can be accessed from multiple threads.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CacheTTL
 * @see PlaceholderService
 */
public final class PlaceholderCache {

    private static final String GLOBAL_KEY_PREFIX = "__global__:";

    private final Map<String, CacheEntry> cache;
    private final CacheTTL defaultTTL;
    private final int maxSize;
    private final ScheduledExecutorService cleanupExecutor;
    private final AtomicLong hits;
    private final AtomicLong misses;

    private PlaceholderCache(Builder builder) {
        this.cache = new ConcurrentHashMap<>();
        this.defaultTTL = builder.defaultTTL;
        this.maxSize = builder.maxSize;
        this.hits = new AtomicLong(0);
        this.misses = new AtomicLong(0);

        // Start cleanup task
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlaceholderCache-Cleanup");
            t.setDaemon(true);
            return t;
        });

        long cleanupMs = builder.cleanupInterval.toMillis();
        if (cleanupMs > 0) {
            cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                cleanupMs,
                cleanupMs,
                TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Creates a cache with default settings.
     *
     * @return a new cache instance
     */
    @NotNull
    public static PlaceholderCache create() {
        return builder().build();
    }

    /**
     * Creates a new builder for configuring the cache.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieves a global cached value.
     *
     * @param key the cache key
     * @return an Optional containing the cached value if present and not expired
     */
    @NotNull
    public Optional<String> get(@NotNull String key) {
        return get(GLOBAL_KEY_PREFIX + key);
    }

    /**
     * Retrieves a player-specific cached value.
     *
     * @param playerId the player's UUID
     * @param key      the cache key
     * @return an Optional containing the cached value if present and not expired
     */
    @NotNull
    public Optional<String> get(@NotNull UUID playerId, @NotNull String key) {
        return getInternal(playerId.toString() + ":" + key);
    }

    /**
     * Retrieves a relational cached value.
     *
     * @param viewerId the viewer's UUID
     * @param targetId the target's UUID
     * @param key      the cache key
     * @return an Optional containing the cached value if present and not expired
     */
    @NotNull
    public Optional<String> get(@NotNull UUID viewerId, @NotNull UUID targetId, @NotNull String key) {
        return getInternal(viewerId.toString() + ":" + targetId.toString() + ":" + key);
    }

    @NotNull
    private Optional<String> getInternal(@NotNull String fullKey) {
        CacheEntry entry = cache.get(fullKey);
        if (entry == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        if (entry.isExpired()) {
            cache.remove(fullKey);
            misses.incrementAndGet();
            return Optional.empty();
        }
        hits.incrementAndGet();
        return Optional.of(entry.value);
    }

    /**
     * Stores a global value in the cache.
     *
     * @param key   the cache key
     * @param value the value to cache
     * @param ttl   the time-to-live
     */
    public void put(@NotNull String key, @NotNull String value, @NotNull CacheTTL ttl) {
        putInternal(GLOBAL_KEY_PREFIX + key, value, ttl);
    }

    /**
     * Stores a global value with default TTL.
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    public void put(@NotNull String key, @NotNull String value) {
        put(key, value, defaultTTL);
    }

    /**
     * Stores a player-specific value in the cache.
     *
     * @param playerId the player's UUID
     * @param key      the cache key
     * @param value    the value to cache
     * @param ttl      the time-to-live
     */
    public void put(@NotNull UUID playerId, @NotNull String key, @NotNull String value, @NotNull CacheTTL ttl) {
        putInternal(playerId.toString() + ":" + key, value, ttl);
    }

    /**
     * Stores a player-specific value with default TTL.
     *
     * @param playerId the player's UUID
     * @param key      the cache key
     * @param value    the value to cache
     */
    public void put(@NotNull UUID playerId, @NotNull String key, @NotNull String value) {
        put(playerId, key, value, defaultTTL);
    }

    /**
     * Stores a relational value in the cache.
     *
     * @param viewerId the viewer's UUID
     * @param targetId the target's UUID
     * @param key      the cache key
     * @param value    the value to cache
     * @param ttl      the time-to-live
     */
    public void put(@NotNull UUID viewerId, @NotNull UUID targetId, @NotNull String key,
                    @NotNull String value, @NotNull CacheTTL ttl) {
        putInternal(viewerId.toString() + ":" + targetId.toString() + ":" + key, value, ttl);
    }

    private void putInternal(@NotNull String fullKey, @NotNull String value, @NotNull CacheTTL ttl) {
        if (ttl.isNone()) {
            return; // Don't cache if TTL is zero
        }

        // Evict if over size limit
        if (cache.size() >= maxSize) {
            evictOldest();
        }

        cache.put(fullKey, new CacheEntry(value, ttl));
    }

    /**
     * Gets a cached value or computes it if not present.
     *
     * @param key      the cache key
     * @param ttl      the TTL for new entries
     * @param supplier the value supplier
     * @return the cached or computed value
     */
    @NotNull
    public String getOrCompute(@NotNull String key, @NotNull CacheTTL ttl, @NotNull Supplier<String> supplier) {
        return get(key).orElseGet(() -> {
            String value = supplier.get();
            if (value != null) {
                put(key, value, ttl);
            }
            return value;
        });
    }

    /**
     * Gets a player-specific cached value or computes it.
     *
     * @param playerId the player's UUID
     * @param key      the cache key
     * @param ttl      the TTL for new entries
     * @param supplier the value supplier
     * @return the cached or computed value
     */
    @NotNull
    public String getOrCompute(@NotNull UUID playerId, @NotNull String key, @NotNull CacheTTL ttl,
                               @NotNull Supplier<String> supplier) {
        return get(playerId, key).orElseGet(() -> {
            String value = supplier.get();
            if (value != null) {
                put(playerId, key, value, ttl);
            }
            return value;
        });
    }

    /**
     * Invalidates a global cache entry.
     *
     * @param key the cache key
     */
    public void invalidate(@NotNull String key) {
        cache.remove(GLOBAL_KEY_PREFIX + key);
    }

    /**
     * Invalidates a player-specific cache entry.
     *
     * @param playerId the player's UUID
     * @param key      the cache key
     */
    public void invalidate(@NotNull UUID playerId, @NotNull String key) {
        cache.remove(playerId.toString() + ":" + key);
    }

    /**
     * Invalidates all cache entries for a player.
     *
     * @param playerId the player's UUID
     */
    public void invalidatePlayer(@NotNull UUID playerId) {
        String prefix = playerId.toString() + ":";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Invalidates all cache entries matching a pattern.
     *
     * @param pattern the pattern to match (supports * wildcard at end)
     */
    public void invalidatePattern(@NotNull String pattern) {
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            cache.keySet().removeIf(k -> k.contains(":" + prefix) || k.endsWith(":" + prefix.substring(0, prefix.length() > 0 ? prefix.length() - 1 : 0)));
        } else {
            cache.keySet().removeIf(k -> k.endsWith(":" + pattern));
        }
    }

    /**
     * Clears all cached entries.
     */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * Returns the current cache size.
     *
     * @return the number of cached entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Returns the cache hit count.
     *
     * @return the number of cache hits
     */
    public long getHits() {
        return hits.get();
    }

    /**
     * Returns the cache miss count.
     *
     * @return the number of cache misses
     */
    public long getMisses() {
        return misses.get();
    }

    /**
     * Returns the cache hit rate.
     *
     * @return the hit rate (0.0 to 1.0)
     */
    public double getHitRate() {
        long total = hits.get() + misses.get();
        return total > 0 ? (double) hits.get() / total : 0.0;
    }

    /**
     * Resets cache statistics.
     */
    public void resetStats() {
        hits.set(0);
        misses.set(0);
    }

    /**
     * Performs cache cleanup, removing expired entries.
     */
    public void cleanup() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /**
     * Evicts the oldest entries when cache is full.
     */
    private void evictOldest() {
        // Simple eviction: remove 10% of entries
        int toRemove = Math.max(1, maxSize / 10);
        cache.entrySet().stream()
            .sorted((a, b) -> Long.compare(a.getValue().timestamp, b.getValue().timestamp))
            .limit(toRemove)
            .map(Map.Entry::getKey)
            .forEach(cache::remove);
    }

    /**
     * Shuts down the cache cleanup executor.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Internal cache entry holding value and expiration data.
     */
    private static final class CacheEntry {
        final String value;
        final long timestamp;
        final CacheTTL ttl;

        CacheEntry(String value, CacheTTL ttl) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.ttl = ttl;
        }

        boolean isExpired() {
            return ttl.isExpired(timestamp);
        }
    }

    /**
     * Builder for creating {@link PlaceholderCache} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private CacheTTL defaultTTL = CacheTTL.SECONDS_30;
        private int maxSize = 10000;
        private CacheTTL cleanupInterval = CacheTTL.MINUTES_1;

        private Builder() {}

        /**
         * Sets the default TTL for cache entries.
         *
         * @param ttl the default TTL
         * @return this builder
         */
        @NotNull
        public Builder defaultTTL(@NotNull CacheTTL ttl) {
            this.defaultTTL = Objects.requireNonNull(ttl, "ttl cannot be null");
            return this;
        }

        /**
         * Sets the maximum cache size.
         *
         * @param size the maximum number of entries
         * @return this builder
         */
        @NotNull
        public Builder maxSize(int size) {
            this.maxSize = Math.max(100, size);
            return this;
        }

        /**
         * Sets the cleanup interval for expired entries.
         *
         * @param interval the cleanup interval
         * @return this builder
         */
        @NotNull
        public Builder cleanupInterval(@NotNull CacheTTL interval) {
            this.cleanupInterval = Objects.requireNonNull(interval, "interval cannot be null");
            return this;
        }

        /**
         * Builds the cache instance.
         *
         * @return a new PlaceholderCache
         */
        @NotNull
        public PlaceholderCache build() {
            return new PlaceholderCache(this);
        }
    }
}
