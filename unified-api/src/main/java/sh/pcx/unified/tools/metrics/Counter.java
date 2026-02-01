/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import org.jetbrains.annotations.NotNull;

/**
 * A monotonically increasing counter metric.
 *
 * <p>Counters are used to track values that only increase, such as
 * request counts, errors, or bytes transferred. They are useful for
 * calculating rates of events over time.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Counter requests = metrics.counter("api_requests_total", "Total API requests");
 * requests.increment();
 * requests.increment(5);
 *
 * // With labels
 * Counter requestsByMethod = metrics.counter("requests_by_method", "Requests by HTTP method")
 *     .labels("method");
 * requestsByMethod.labels("GET").increment();
 * requestsByMethod.labels("POST").increment(10);
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
public interface Counter extends Metric {

    /**
     * Increments the counter by one.
     *
     * @since 1.0.0
     */
    void increment();

    /**
     * Increments the counter by the specified amount.
     *
     * @param amount the amount to increment (must be non-negative)
     * @throws IllegalArgumentException if amount is negative
     * @since 1.0.0
     */
    void increment(long amount);

    /**
     * Returns the current value of the counter.
     *
     * @return the current count
     * @since 1.0.0
     */
    long get();

    /**
     * Resets the counter to zero.
     *
     * <p><strong>Warning:</strong> Resetting counters can cause discontinuities
     * in rate calculations. Use with caution.
     *
     * @since 1.0.0
     */
    void reset();

    /**
     * Creates a labeled child counter.
     *
     * <p>Labels allow you to partition counter values by different dimensions.
     *
     * @param labelValues the label values in the order they were defined
     * @return a child counter with the specified labels
     * @throws IllegalArgumentException if the number of values doesn't match the defined labels
     * @since 1.0.0
     */
    @NotNull
    Counter labels(@NotNull String... labelValues);

    /**
     * A labeled counter that is a child of a parent counter.
     *
     * @since 1.0.0
     */
    interface Labeled extends Counter {

        /**
         * Returns the label values for this counter.
         *
         * @return the label values
         * @since 1.0.0
         */
        @NotNull
        String[] labelValues();
    }
}
