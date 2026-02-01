/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Collects and reports cache statistics from multiple caches.
 *
 * <p>CacheStatsCollector provides centralized statistics collection with
 * support for periodic reporting, snapshot history, and metric aggregation.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a stats collector
 * CacheStatsCollector collector = CacheStatsCollector.builder()
 *     .reportInterval(Duration.ofMinutes(5))
 *     .historySize(12)  // Keep 12 snapshots (1 hour of 5-min intervals)
 *     .reporter(stats -> {
 *         log.info("Cache stats: " + stats);
 *     })
 *     .build();
 *
 * // Register caches
 * collector.register("players", playerCache);
 * collector.register("items", itemCache);
 *
 * // Get current stats
 * CacheStats playerStats = collector.getStats("players");
 *
 * // Get aggregated stats
 * CacheStats allStats = collector.aggregateStats();
 *
 * // Get snapshot history
 * List<StatsSnapshot> history = collector.getHistory("players");
 *
 * // Shutdown
 * collector.shutdown();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CacheStats
 * @see CacheMetrics
 */
public final class CacheStatsCollector implements AutoCloseable {

    private final Map<String, LocalCache<?, ?>> caches;
    private final Map<String, java.util.Deque<StatsSnapshot>> history;
    private final Duration reportInterval;
    private final int historySize;
    private final Consumer<Report> reporter;
    private final ScheduledExecutorService scheduler;
    private final CacheMetrics metrics;

    private ScheduledFuture<?> reportTask;
    private volatile boolean running;

    /**
     * Creates a new stats collector.
     */
    private CacheStatsCollector(
            @NotNull Duration reportInterval,
            int historySize,
            @Nullable Consumer<Report> reporter,
            @Nullable CacheMetrics metrics) {
        this.caches = new ConcurrentHashMap<>();
        this.history = new ConcurrentHashMap<>();
        this.reportInterval = Objects.requireNonNull(reportInterval, "reportInterval cannot be null");
        this.historySize = historySize > 0 ? historySize : 10;
        this.reporter = reporter;
        this.metrics = metrics != null ? metrics : CacheMetrics.NOOP;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-stats-collector");
            t.setDaemon(true);
            return t;
        });
        this.running = false;
    }

    /**
     * Registers a cache for statistics collection.
     *
     * @param <K>   the key type
     * @param <V>   the value type
     * @param name  the cache name for reporting
     * @param cache the cache to monitor
     * @since 1.0.0
     */
    public <K, V> void register(@NotNull String name, @NotNull LocalCache<K, V> cache) {
        caches.put(name, cache);
        history.put(name, new java.util.concurrent.LinkedBlockingDeque<>(historySize));
        metrics.registerCache(name, cache);
    }

    /**
     * Unregisters a cache from statistics collection.
     *
     * @param name the cache name
     * @return true if the cache was unregistered
     * @since 1.0.0
     */
    public boolean unregister(@NotNull String name) {
        LocalCache<?, ?> removed = caches.remove(name);
        if (removed != null) {
            history.remove(name);
            metrics.unregister(name);
            return true;
        }
        return false;
    }

    /**
     * Returns all registered cache names.
     *
     * @return the cache names
     * @since 1.0.0
     */
    @NotNull
    public Collection<String> registeredCaches() {
        return caches.keySet();
    }

    /**
     * Returns the current statistics for a cache.
     *
     * @param name the cache name
     * @return the current stats, or null if not registered
     * @since 1.0.0
     */
    @Nullable
    public CacheStats getStats(@NotNull String name) {
        LocalCache<?, ?> cache = caches.get(name);
        return cache != null ? cache.stats() : null;
    }

    /**
     * Returns the current statistics for all registered caches.
     *
     * @return a map of cache name to stats
     * @since 1.0.0
     */
    @NotNull
    public Map<String, CacheStats> getAllStats() {
        Map<String, CacheStats> result = new java.util.HashMap<>();
        for (Map.Entry<String, LocalCache<?, ?>> entry : caches.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stats());
        }
        return result;
    }

    /**
     * Returns aggregated statistics across all registered caches.
     *
     * @return the aggregated stats
     * @since 1.0.0
     */
    @NotNull
    public CacheStats aggregateStats() {
        CacheStats aggregate = CacheStats.EMPTY;
        for (LocalCache<?, ?> cache : caches.values()) {
            aggregate = aggregate.plus(cache.stats());
        }
        return aggregate;
    }

    /**
     * Returns the snapshot history for a cache.
     *
     * @param name the cache name
     * @return the snapshot history, or empty list if not registered
     * @since 1.0.0
     */
    @NotNull
    public java.util.List<StatsSnapshot> getHistory(@NotNull String name) {
        java.util.Deque<StatsSnapshot> deque = history.get(name);
        if (deque == null) {
            return java.util.List.of();
        }
        return new java.util.ArrayList<>(deque);
    }

    /**
     * Takes a snapshot of current statistics.
     *
     * <p>This is called automatically at the report interval, but can
     * also be called manually to take immediate snapshots.
     *
     * @since 1.0.0
     */
    public void takeSnapshot() {
        Instant now = Instant.now();
        for (Map.Entry<String, LocalCache<?, ?>> entry : caches.entrySet()) {
            String name = entry.getKey();
            CacheStats stats = entry.getValue().stats();

            java.util.Deque<StatsSnapshot> deque = history.get(name);
            if (deque != null) {
                StatsSnapshot snapshot = new StatsSnapshot(name, stats, now);

                // Remove oldest if at capacity
                while (deque.size() >= historySize) {
                    deque.pollFirst();
                }
                deque.offerLast(snapshot);
            }
        }
    }

    /**
     * Starts periodic statistics collection and reporting.
     *
     * @since 1.0.0
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        reportTask = scheduler.scheduleAtFixedRate(
                this::reportStats,
                reportInterval.toMillis(),
                reportInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops periodic statistics collection.
     *
     * @since 1.0.0
     */
    public void stop() {
        running = false;
        if (reportTask != null) {
            reportTask.cancel(false);
            reportTask = null;
        }
    }

    /**
     * Reports current statistics.
     */
    private void reportStats() {
        takeSnapshot();

        if (reporter != null) {
            Map<String, CacheStats> stats = getAllStats();
            CacheStats aggregate = aggregateStats();
            Report report = new Report(stats, aggregate, Instant.now());
            reporter.accept(report);
        }
    }

    /**
     * Checks if the collector is running.
     *
     * @return true if running
     * @since 1.0.0
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Shuts down the stats collector.
     *
     * @since 1.0.0
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Creates a new builder for CacheStatsCollector.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A snapshot of cache statistics at a point in time.
     *
     * @param cacheName the cache name
     * @param stats     the statistics
     * @param timestamp when the snapshot was taken
     * @since 1.0.0
     */
    public record StatsSnapshot(
            @NotNull String cacheName,
            @NotNull CacheStats stats,
            @NotNull Instant timestamp
    ) {

        /**
         * Calculates the delta from a previous snapshot.
         *
         * @param previous the previous snapshot
         * @return the difference in stats
         */
        @NotNull
        public CacheStats delta(@NotNull StatsSnapshot previous) {
            return stats.minus(previous.stats);
        }

        /**
         * Returns the age of this snapshot.
         *
         * @return the duration since this snapshot was taken
         */
        @NotNull
        public Duration age() {
            return Duration.between(timestamp, Instant.now());
        }
    }

    /**
     * A statistics report for all monitored caches.
     *
     * @param byCache   statistics by cache name
     * @param aggregate aggregated statistics
     * @param timestamp when the report was generated
     * @since 1.0.0
     */
    public record Report(
            @NotNull Map<String, CacheStats> byCache,
            @NotNull CacheStats aggregate,
            @NotNull Instant timestamp
    ) {

        /**
         * Returns statistics for a specific cache.
         *
         * @param name the cache name
         * @return the stats, or null if not found
         */
        @Nullable
        public CacheStats forCache(@NotNull String name) {
            return byCache.get(name);
        }

        /**
         * Returns the total number of caches in this report.
         *
         * @return the cache count
         */
        public int cacheCount() {
            return byCache.size();
        }

        /**
         * Returns the overall hit rate across all caches.
         *
         * @return the aggregate hit rate
         */
        public double overallHitRate() {
            return aggregate.hitRate();
        }

        /**
         * Returns the total number of entries across all caches.
         *
         * @return the total size
         */
        public long totalSize() {
            return aggregate.size();
        }

        @Override
        public String toString() {
            return String.format(
                    "CacheReport{caches=%d, totalSize=%d, hitRate=%.2f%%, time=%s}",
                    cacheCount(), totalSize(), overallHitRate() * 100, timestamp
            );
        }
    }

    /**
     * Builder for CacheStatsCollector.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private Duration reportInterval = Duration.ofMinutes(5);
        private int historySize = 12;
        private Consumer<Report> reporter;
        private CacheMetrics metrics;

        private Builder() {}

        /**
         * Sets the interval between automatic reports.
         *
         * @param interval the report interval
         * @return this builder
         */
        @NotNull
        public Builder reportInterval(@NotNull Duration interval) {
            this.reportInterval = interval;
            return this;
        }

        /**
         * Sets the number of snapshots to keep in history.
         *
         * @param size the history size
         * @return this builder
         */
        @NotNull
        public Builder historySize(int size) {
            this.historySize = size;
            return this;
        }

        /**
         * Sets the report consumer.
         *
         * @param reporter the report handler
         * @return this builder
         */
        @NotNull
        public Builder reporter(@Nullable Consumer<Report> reporter) {
            this.reporter = reporter;
            return this;
        }

        /**
         * Sets the metrics registry.
         *
         * @param metrics the metrics registry
         * @return this builder
         */
        @NotNull
        public Builder metrics(@Nullable CacheMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * Builds the CacheStatsCollector instance.
         *
         * @return a new CacheStatsCollector
         */
        @NotNull
        public CacheStatsCollector build() {
            return new CacheStatsCollector(reportInterval, historySize, reporter, metrics);
        }
    }

    @Override
    public String toString() {
        return "CacheStatsCollector{caches=" + caches.size() +
               ", running=" + running +
               ", interval=" + reportInterval + "}";
    }
}
