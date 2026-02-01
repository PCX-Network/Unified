/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * High-performance caching layer with Caffeine integration and distributed caching support.
 *
 * <p>This package provides a comprehensive caching solution for Minecraft plugins, including:
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.cache.CacheService} - Main service interface for cache management</li>
 *   <li>{@link sh.pcx.unified.data.cache.CacheConfig} - Immutable cache configuration</li>
 *   <li>{@link sh.pcx.unified.data.cache.CacheRegion} - Named cache region for organizing related caches</li>
 * </ul>
 *
 * <h2>Cache Implementations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.cache.LocalCache} - High-performance Caffeine-based local cache</li>
 *   <li>{@link sh.pcx.unified.data.cache.LoadingCache} - Cache with automatic loading on miss</li>
 *   <li>{@link sh.pcx.unified.data.cache.WriteBehindCache} - Asynchronous batch-write cache</li>
 *   <li>{@link sh.pcx.unified.data.cache.AsyncCache} - Fully asynchronous cache operations</li>
 *   <li>{@link sh.pcx.unified.data.cache.DistributedCache} - Redis-backed distributed cache</li>
 * </ul>
 *
 * <h2>Statistics and Monitoring</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.cache.CacheStats} - Cache statistics record</li>
 *   <li>{@link sh.pcx.unified.data.cache.CacheMetrics} - Metrics collection interface</li>
 *   <li>{@link sh.pcx.unified.data.cache.CacheStatsCollector} - Centralized stats collection</li>
 * </ul>
 *
 * <h2>Conflict Resolution</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.cache.ConflictResolver} - Interface for resolving conflicts</li>
 *   <li>{@link sh.pcx.unified.data.cache.LastWriteWins} - LWW resolution strategy</li>
 *   <li>{@link sh.pcx.unified.data.cache.MergeStrategy} - Custom merge strategies</li>
 * </ul>
 *
 * <h2>Distributed Caching</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.cache.CacheInvalidation} - Cross-server invalidation via pub/sub</li>
 *   <li>{@link sh.pcx.unified.data.cache.CacheSyncStrategy} - Write-through, write-behind, refresh-ahead</li>
 * </ul>
 *
 * <h2>Utilities</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.cache.CacheKey} - Type-safe cache key wrapper</li>
 *   <li>{@link sh.pcx.unified.data.cache.CacheLoader} - Functional interface for loading values</li>
 *   <li>{@link sh.pcx.unified.data.cache.CacheWriter} - Functional interface for writing values</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Inject the cache service
 * @Inject
 * private CacheService caches;
 *
 * // Create a basic cache
 * LocalCache<UUID, PlayerData> playerCache = caches.<UUID, PlayerData>builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .expireAfterWrite(Duration.ofMinutes(30))
 *     .recordStats(true)
 *     .build();
 *
 * // Use the cache
 * playerCache.put(uuid, data);
 * Optional<PlayerData> cached = playerCache.get(uuid);
 *
 * // View statistics
 * CacheStats stats = playerCache.stats();
 * log.info("Hit rate: " + String.format("%.2f%%", stats.hitRate() * 100));
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Caffeine Integration</b> - High-performance local caching with W-TinyLFU eviction</li>
 *   <li><b>TTL Support</b> - Time-based expiration after access or write</li>
 *   <li><b>Size Limits</b> - Maximum entry count with automatic eviction</li>
 *   <li><b>Weak/Soft References</b> - Memory-sensitive caching</li>
 *   <li><b>Statistics</b> - Hit/miss rates, load times, eviction counts</li>
 *   <li><b>Write-Behind</b> - Asynchronous batch writes for throughput</li>
 *   <li><b>Redis Integration</b> - Distributed caching with pub/sub invalidation</li>
 *   <li><b>Conflict Resolution</b> - Strategies for handling concurrent modifications</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.cache;
