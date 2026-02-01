/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import org.jetbrains.annotations.NotNull;

/**
 * A metric that samples observations and provides statistical distribution.
 *
 * <p>Histograms are used to track the distribution of values, providing
 * bucketed counts and percentile calculations. They are useful for
 * measuring things like request sizes, response times, or any value
 * where understanding the distribution is important.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create with default buckets
 * Histogram responseSize = metrics.histogram("response_size_bytes", "Response size in bytes");
 * responseSize.observe(1024);
 * responseSize.observe(2048);
 *
 * // Create with custom buckets
 * Histogram latency = metrics.histogram("request_latency_ms", "Request latency")
 *     .buckets(10, 25, 50, 100, 250, 500, 1000);
 *
 * // Access statistics
 * long count = latency.count();
 * double sum = latency.sum();
 * double mean = latency.mean();
 * double p99 = latency.percentile(0.99);
 *
 * // Bucket information
 * long[] bucketCounts = latency.bucketCounts();
 * double[] bucketBoundaries = latency.bucketBoundaries();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations must be thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Metric
 * @see MetricsService
 */
public interface Histogram extends Metric {

    /**
     * Default histogram buckets suitable for latency measurements (in milliseconds).
     */
    double[] DEFAULT_BUCKETS = {5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000};

    /**
     * Records an observation.
     *
     * @param value the value to observe
     * @since 1.0.0
     */
    void observe(double value);

    /**
     * Records an observation.
     *
     * @param value the value to observe
     * @since 1.0.0
     */
    default void observe(long value) {
        observe((double) value);
    }

    /**
     * Returns the number of observations.
     *
     * @return the count
     * @since 1.0.0
     */
    long count();

    /**
     * Returns the sum of all observed values.
     *
     * @return the sum
     * @since 1.0.0
     */
    double sum();

    /**
     * Returns the mean (average) of all observed values.
     *
     * @return the mean, or 0 if no observations
     * @since 1.0.0
     */
    default double mean() {
        long count = count();
        return count == 0 ? 0 : sum() / count;
    }

    /**
     * Returns the minimum observed value.
     *
     * @return the minimum, or 0 if no observations
     * @since 1.0.0
     */
    double min();

    /**
     * Returns the maximum observed value.
     *
     * @return the maximum, or 0 if no observations
     * @since 1.0.0
     */
    double max();

    /**
     * Returns an estimated percentile value.
     *
     * @param percentile the percentile (0.0 to 1.0, e.g., 0.99 for 99th percentile)
     * @return the estimated value at the given percentile
     * @throws IllegalArgumentException if percentile is not in [0, 1]
     * @since 1.0.0
     */
    double percentile(double percentile);

    /**
     * Returns the count of observations in each bucket.
     *
     * <p>The returned array has length {@code bucketBoundaries().length + 1},
     * where the last element is the count of values greater than the highest boundary.
     *
     * @return the bucket counts
     * @since 1.0.0
     */
    @NotNull
    long[] bucketCounts();

    /**
     * Returns the upper boundaries of each bucket.
     *
     * @return the bucket boundaries
     * @since 1.0.0
     */
    @NotNull
    double[] bucketBoundaries();

    /**
     * Creates a labeled child histogram.
     *
     * @param labelValues the label values
     * @return a child histogram with the specified labels
     * @throws IllegalArgumentException if the number of values doesn't match
     * @since 1.0.0
     */
    @NotNull
    Histogram labels(@NotNull String... labelValues);

    /**
     * Resets all histogram statistics.
     *
     * @since 1.0.0
     */
    void reset();

    /**
     * Builder for creating histograms with custom configuration.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets custom bucket boundaries.
         *
         * @param boundaries the upper bounds of each bucket
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder buckets(double... boundaries);

        /**
         * Sets linear bucket boundaries.
         *
         * @param start the starting boundary
         * @param width the width of each bucket
         * @param count the number of buckets
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder linearBuckets(double start, double width, int count);

        /**
         * Sets exponential bucket boundaries.
         *
         * @param start  the starting boundary
         * @param factor the factor between buckets
         * @param count  the number of buckets
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder exponentialBuckets(double start, double factor, int count);

        /**
         * Defines labels for the histogram.
         *
         * @param labelNames the label names
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder labelNames(@NotNull String... labelNames);

        /**
         * Builds the histogram.
         *
         * @return the configured histogram
         * @since 1.0.0
         */
        @NotNull
        Histogram build();
    }
}
