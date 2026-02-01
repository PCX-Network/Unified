/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * Interface for collecting and exposing cache metrics.
 *
 * <p>CacheMetrics provides integration with monitoring systems (e.g., Prometheus,
 * Micrometer) by exposing cache statistics as gauges and counters.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a metrics implementation
 * CacheMetrics metrics = new PrometheusCacheMetrics();
 *
 * // Register cache metrics
 * LocalCache<UUID, PlayerData> cache = caches.<UUID, PlayerData>builder()
 *     .name("player-data")
 *     .recordStats(true)
 *     .build();
 *
 * metrics.registerCache("player_cache", cache);
 *
 * // Or manually register individual metrics
 * metrics.gauge("cache.hit_rate", cache.stats()::hitRate);
 * metrics.gauge("cache.size", cache::size);
 * metrics.counter("cache.requests", () -> cache.stats().requestCount());
 * }</pre>
 *
 * <h2>Standard Metrics</h2>
 * <p>The following metrics are typically exposed:
 * <ul>
 *   <li>cache.hits - Number of cache hits</li>
 *   <li>cache.misses - Number of cache misses</li>
 *   <li>cache.hit_rate - Ratio of hits to total requests</li>
 *   <li>cache.size - Current number of entries</li>
 *   <li>cache.evictions - Number of evicted entries</li>
 *   <li>cache.load_time - Total time spent loading values</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CacheStats
 * @see CacheStatsCollector
 */
public interface CacheMetrics {

    /**
     * Registers a gauge metric with a double value supplier.
     *
     * <p>A gauge represents a value that can go up or down, such as
     * cache hit rate or current size.
     *
     * @param name     the metric name
     * @param supplier the value supplier
     * @since 1.0.0
     */
    void gauge(@NotNull String name, @NotNull DoubleSupplier supplier);

    /**
     * Registers a gauge metric with tags.
     *
     * @param name     the metric name
     * @param tags     the metric tags (key-value pairs)
     * @param supplier the value supplier
     * @since 1.0.0
     */
    void gauge(@NotNull String name, @NotNull Tags tags, @NotNull DoubleSupplier supplier);

    /**
     * Registers a counter metric with a long value supplier.
     *
     * <p>A counter represents a value that only increases, such as
     * total hits or misses.
     *
     * @param name     the metric name
     * @param supplier the value supplier
     * @since 1.0.0
     */
    void counter(@NotNull String name, @NotNull LongSupplier supplier);

    /**
     * Registers a counter metric with tags.
     *
     * @param name     the metric name
     * @param tags     the metric tags (key-value pairs)
     * @param supplier the value supplier
     * @since 1.0.0
     */
    void counter(@NotNull String name, @NotNull Tags tags, @NotNull LongSupplier supplier);

    /**
     * Registers all standard metrics for a cache.
     *
     * <p>This registers the following metrics:
     * <ul>
     *   <li>{name}.hits</li>
     *   <li>{name}.misses</li>
     *   <li>{name}.hit_rate</li>
     *   <li>{name}.size</li>
     *   <li>{name}.evictions</li>
     *   <li>{name}.load_success</li>
     *   <li>{name}.load_failure</li>
     *   <li>{name}.load_time_nanos</li>
     * </ul>
     *
     * @param <K>   the key type
     * @param <V>   the value type
     * @param name  the metric prefix
     * @param cache the cache to monitor
     * @since 1.0.0
     */
    default <K, V> void registerCache(@NotNull String name, @NotNull LocalCache<K, V> cache) {
        Tags cacheTags = Tags.of("cache", cache.name());

        counter(name + ".hits", cacheTags, () -> cache.stats().hitCount());
        counter(name + ".misses", cacheTags, () -> cache.stats().missCount());
        gauge(name + ".hit_rate", cacheTags, () -> cache.stats().hitRate());
        gauge(name + ".size", cacheTags, () -> cache.size());
        counter(name + ".evictions", cacheTags, () -> cache.stats().evictionCount());
        counter(name + ".load_success", cacheTags, () -> cache.stats().loadSuccessCount());
        counter(name + ".load_failure", cacheTags, () -> cache.stats().loadFailureCount());
        counter(name + ".load_time_nanos", cacheTags, () -> cache.stats().totalLoadTime());
    }

    /**
     * Registers all standard metrics for a distributed cache.
     *
     * @param <K>   the key type
     * @param <V>   the value type
     * @param name  the metric prefix
     * @param cache the cache to monitor
     * @since 1.0.0
     */
    default <K, V> void registerDistributedCache(
            @NotNull String name,
            @NotNull DistributedCache<K, V> cache) {
        Tags cacheTags = Tags.of("cache", cache.name(), "type", "distributed");

        cache.localCache().ifPresent(local -> {
            Tags localTags = cacheTags.and("tier", "local");
            counter(name + ".local.hits", localTags, () -> local.stats().hitCount());
            counter(name + ".local.misses", localTags, () -> local.stats().missCount());
            gauge(name + ".local.hit_rate", localTags, () -> local.stats().hitRate());
            gauge(name + ".local.size", localTags, () -> local.size());
        });

        gauge(name + ".strategy", cacheTags, () -> cache.syncStrategy().ordinal());
    }

    /**
     * Removes all metrics for a cache.
     *
     * @param name the metric prefix used during registration
     * @since 1.0.0
     */
    void unregister(@NotNull String name);

    /**
     * Represents a collection of metric tags.
     *
     * @since 1.0.0
     */
    interface Tags {

        /**
         * Returns an empty tags instance.
         *
         * @return empty tags
         */
        @NotNull
        static Tags empty() {
            return new TagsImpl(java.util.Map.of());
        }

        /**
         * Creates tags from key-value pairs.
         *
         * @param keysAndValues alternating keys and values
         * @return the tags
         */
        @NotNull
        static Tags of(@NotNull String... keysAndValues) {
            if (keysAndValues.length % 2 != 0) {
                throw new IllegalArgumentException("Must provide key-value pairs");
            }
            java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
            for (int i = 0; i < keysAndValues.length; i += 2) {
                map.put(keysAndValues[i], keysAndValues[i + 1]);
            }
            return new TagsImpl(map);
        }

        /**
         * Returns the tags as a map.
         *
         * @return the tag map
         */
        @NotNull
        java.util.Map<String, String> asMap();

        /**
         * Creates new tags by adding to this tags.
         *
         * @param keysAndValues additional key-value pairs
         * @return new tags with additions
         */
        @NotNull
        default Tags and(@NotNull String... keysAndValues) {
            if (keysAndValues.length % 2 != 0) {
                throw new IllegalArgumentException("Must provide key-value pairs");
            }
            java.util.Map<String, String> map = new java.util.LinkedHashMap<>(asMap());
            for (int i = 0; i < keysAndValues.length; i += 2) {
                map.put(keysAndValues[i], keysAndValues[i + 1]);
            }
            return new TagsImpl(map);
        }
    }

    /**
     * Simple Tags implementation.
     */
    record TagsImpl(@NotNull java.util.Map<String, String> asMap) implements Tags {}

    /**
     * A no-op metrics implementation for when monitoring is disabled.
     *
     * @since 1.0.0
     */
    CacheMetrics NOOP = new CacheMetrics() {
        @Override
        public void gauge(@NotNull String name, @NotNull DoubleSupplier supplier) {}

        @Override
        public void gauge(@NotNull String name, @NotNull Tags tags, @NotNull DoubleSupplier supplier) {}

        @Override
        public void counter(@NotNull String name, @NotNull LongSupplier supplier) {}

        @Override
        public void counter(@NotNull String name, @NotNull Tags tags, @NotNull LongSupplier supplier) {}

        @Override
        public void unregister(@NotNull String name) {}
    };

    /**
     * Creates a simple metrics implementation that logs to a consumer.
     *
     * @param logger the log consumer
     * @return a logging metrics implementation
     * @since 1.0.0
     */
    @NotNull
    static CacheMetrics logging(@NotNull java.util.function.Consumer<String> logger) {
        return new CacheMetrics() {
            private final java.util.Map<String, Object> registered = new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            public void gauge(@NotNull String name, @NotNull DoubleSupplier supplier) {
                registered.put(name, supplier);
                logger.accept("Registered gauge: " + name);
            }

            @Override
            public void gauge(@NotNull String name, @NotNull Tags tags, @NotNull DoubleSupplier supplier) {
                String fullName = name + tags.asMap();
                registered.put(fullName, supplier);
                logger.accept("Registered gauge: " + fullName);
            }

            @Override
            public void counter(@NotNull String name, @NotNull LongSupplier supplier) {
                registered.put(name, supplier);
                logger.accept("Registered counter: " + name);
            }

            @Override
            public void counter(@NotNull String name, @NotNull Tags tags, @NotNull LongSupplier supplier) {
                String fullName = name + tags.asMap();
                registered.put(fullName, supplier);
                logger.accept("Registered counter: " + fullName);
            }

            @Override
            public void unregister(@NotNull String name) {
                registered.entrySet().removeIf(e -> e.getKey().startsWith(name));
                logger.accept("Unregistered metrics: " + name);
            }
        };
    }
}
