/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Immutable record containing cache statistics.
 *
 * <p>CacheStats provides a snapshot of various cache metrics including
 * hit/miss counts, load statistics, and eviction information. Statistics
 * are only recorded when enabled via {@link CacheConfig#recordStats()}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CacheStats stats = cache.stats();
 *
 * // Hit/miss metrics
 * long hits = stats.hitCount();
 * long misses = stats.missCount();
 * double hitRate = stats.hitRate();
 *
 * // Load metrics
 * long loadSuccesses = stats.loadSuccessCount();
 * long loadFailures = stats.loadFailureCount();
 * Duration avgLoadTime = stats.averageLoadPenalty();
 *
 * // Eviction metrics
 * long evictions = stats.evictionCount();
 * long evictionWeight = stats.evictionWeight();
 *
 * // Size metrics
 * long size = stats.size();
 * long weightedSize = stats.weightedSize();
 *
 * // Log formatted stats
 * log.info("Cache hit rate: " + String.format("%.2f%%", stats.hitRate() * 100));
 * }</pre>
 *
 * @param hitCount          the number of cache hits
 * @param missCount         the number of cache misses
 * @param loadSuccessCount  the number of successful loads
 * @param loadFailureCount  the number of failed loads
 * @param totalLoadTime     the total time spent loading values (nanoseconds)
 * @param evictionCount     the number of entries evicted
 * @param evictionWeight    the total weight of entries evicted
 * @param size              the current number of entries
 * @param weightedSize      the current weighted size
 * @since 1.0.0
 * @author Supatuck
 * @see CacheMetrics
 */
public record CacheStats(
        long hitCount,
        long missCount,
        long loadSuccessCount,
        long loadFailureCount,
        long totalLoadTime,
        long evictionCount,
        long evictionWeight,
        long size,
        long weightedSize
) {

    /**
     * An empty stats instance with all zero values.
     */
    public static final CacheStats EMPTY = new CacheStats(0, 0, 0, 0, 0, 0, 0, 0, 0);

    /**
     * Validates the statistics values.
     */
    public CacheStats {
        if (hitCount < 0) {
            throw new IllegalArgumentException("hitCount must be non-negative");
        }
        if (missCount < 0) {
            throw new IllegalArgumentException("missCount must be non-negative");
        }
        if (loadSuccessCount < 0) {
            throw new IllegalArgumentException("loadSuccessCount must be non-negative");
        }
        if (loadFailureCount < 0) {
            throw new IllegalArgumentException("loadFailureCount must be non-negative");
        }
        if (totalLoadTime < 0) {
            throw new IllegalArgumentException("totalLoadTime must be non-negative");
        }
        if (evictionCount < 0) {
            throw new IllegalArgumentException("evictionCount must be non-negative");
        }
        if (evictionWeight < 0) {
            throw new IllegalArgumentException("evictionWeight must be non-negative");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (weightedSize < 0) {
            throw new IllegalArgumentException("weightedSize must be non-negative");
        }
    }

    /**
     * Returns the total number of cache requests (hits + misses).
     *
     * @return the total request count
     * @since 1.0.0
     */
    public long requestCount() {
        return hitCount + missCount;
    }

    /**
     * Returns the ratio of cache hits to total requests.
     *
     * <p>Returns 1.0 if there have been no requests.
     *
     * @return the hit rate as a value between 0.0 and 1.0
     * @since 1.0.0
     */
    public double hitRate() {
        long total = requestCount();
        return total == 0 ? 1.0 : (double) hitCount / total;
    }

    /**
     * Returns the ratio of cache misses to total requests.
     *
     * <p>Returns 0.0 if there have been no requests.
     *
     * @return the miss rate as a value between 0.0 and 1.0
     * @since 1.0.0
     */
    public double missRate() {
        long total = requestCount();
        return total == 0 ? 0.0 : (double) missCount / total;
    }

    /**
     * Returns the total number of load operations (successful + failed).
     *
     * @return the total load count
     * @since 1.0.0
     */
    public long loadCount() {
        return loadSuccessCount + loadFailureCount;
    }

    /**
     * Returns the ratio of failed loads to total loads.
     *
     * <p>Returns 0.0 if there have been no loads.
     *
     * @return the load failure rate as a value between 0.0 and 1.0
     * @since 1.0.0
     */
    public double loadFailureRate() {
        long total = loadCount();
        return total == 0 ? 0.0 : (double) loadFailureCount / total;
    }

    /**
     * Returns the average time spent loading a value.
     *
     * <p>Returns zero if there have been no loads.
     *
     * @return the average load time as a Duration
     * @since 1.0.0
     */
    @NotNull
    public Duration averageLoadPenalty() {
        long total = loadCount();
        if (total == 0) {
            return Duration.ZERO;
        }
        return Duration.ofNanos(totalLoadTime / total);
    }

    /**
     * Returns the average load time in nanoseconds.
     *
     * @return the average load penalty in nanoseconds
     * @since 1.0.0
     */
    public double averageLoadPenaltyNanos() {
        long total = loadCount();
        return total == 0 ? 0.0 : (double) totalLoadTime / total;
    }

    /**
     * Combines this stats with another stats instance.
     *
     * <p>Useful for aggregating stats from multiple caches.
     *
     * @param other the other stats to combine with
     * @return a new CacheStats with combined values
     * @since 1.0.0
     */
    @NotNull
    public CacheStats plus(@NotNull CacheStats other) {
        return new CacheStats(
                this.hitCount + other.hitCount,
                this.missCount + other.missCount,
                this.loadSuccessCount + other.loadSuccessCount,
                this.loadFailureCount + other.loadFailureCount,
                this.totalLoadTime + other.totalLoadTime,
                this.evictionCount + other.evictionCount,
                this.evictionWeight + other.evictionWeight,
                this.size + other.size,
                this.weightedSize + other.weightedSize
        );
    }

    /**
     * Returns the difference between this stats and another.
     *
     * <p>Useful for calculating delta between snapshots.
     *
     * @param other the baseline stats to subtract
     * @return a new CacheStats with the difference
     * @since 1.0.0
     */
    @NotNull
    public CacheStats minus(@NotNull CacheStats other) {
        return new CacheStats(
                Math.max(0, this.hitCount - other.hitCount),
                Math.max(0, this.missCount - other.missCount),
                Math.max(0, this.loadSuccessCount - other.loadSuccessCount),
                Math.max(0, this.loadFailureCount - other.loadFailureCount),
                Math.max(0, this.totalLoadTime - other.totalLoadTime),
                Math.max(0, this.evictionCount - other.evictionCount),
                Math.max(0, this.evictionWeight - other.evictionWeight),
                this.size,  // Size is current, not cumulative
                this.weightedSize
        );
    }

    /**
     * Creates a stats instance from Caffeine's stats object.
     *
     * @param caffeineStats the Caffeine stats object
     * @param size          the current cache size
     * @param weightedSize  the current weighted size
     * @return a new CacheStats instance
     * @since 1.0.0
     */
    @NotNull
    public static CacheStats fromCaffeine(
            @NotNull com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats,
            long size,
            long weightedSize) {
        return new CacheStats(
                caffeineStats.hitCount(),
                caffeineStats.missCount(),
                caffeineStats.loadSuccessCount(),
                caffeineStats.loadFailureCount(),
                caffeineStats.totalLoadTime(),
                caffeineStats.evictionCount(),
                caffeineStats.evictionWeight(),
                size,
                weightedSize
        );
    }

    /**
     * Creates a builder for CacheStats.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CacheStats.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private long hitCount = 0;
        private long missCount = 0;
        private long loadSuccessCount = 0;
        private long loadFailureCount = 0;
        private long totalLoadTime = 0;
        private long evictionCount = 0;
        private long evictionWeight = 0;
        private long size = 0;
        private long weightedSize = 0;

        private Builder() {}

        public Builder hitCount(long hitCount) {
            this.hitCount = hitCount;
            return this;
        }

        public Builder missCount(long missCount) {
            this.missCount = missCount;
            return this;
        }

        public Builder loadSuccessCount(long loadSuccessCount) {
            this.loadSuccessCount = loadSuccessCount;
            return this;
        }

        public Builder loadFailureCount(long loadFailureCount) {
            this.loadFailureCount = loadFailureCount;
            return this;
        }

        public Builder totalLoadTime(long totalLoadTime) {
            this.totalLoadTime = totalLoadTime;
            return this;
        }

        public Builder evictionCount(long evictionCount) {
            this.evictionCount = evictionCount;
            return this;
        }

        public Builder evictionWeight(long evictionWeight) {
            this.evictionWeight = evictionWeight;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder weightedSize(long weightedSize) {
            this.weightedSize = weightedSize;
            return this;
        }

        @NotNull
        public CacheStats build() {
            return new CacheStats(
                    hitCount,
                    missCount,
                    loadSuccessCount,
                    loadFailureCount,
                    totalLoadTime,
                    evictionCount,
                    evictionWeight,
                    size,
                    weightedSize
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
                "CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, loads=%d, evictions=%d, size=%d}",
                hitCount, missCount, hitRate() * 100, loadCount(), evictionCount, size
        );
    }
}
