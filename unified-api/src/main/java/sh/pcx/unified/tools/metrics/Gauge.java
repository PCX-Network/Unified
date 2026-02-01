/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * A metric that represents a single numerical value that can go up and down.
 *
 * <p>Gauges are used for values that can increase or decrease, such as
 * current temperature, memory usage, or number of active connections.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Static gauge
 * Gauge temperature = metrics.gauge("room_temperature", "Current room temperature");
 * temperature.set(72.5);
 * temperature.increment();
 * temperature.decrement(2.0);
 *
 * // Dynamic gauge with supplier
 * metrics.gauge("players_online", "Current online players",
 *     () -> Bukkit.getOnlinePlayers().size());
 *
 * // With labels
 * Gauge queueSize = metrics.gauge("queue_size", "Queue size by priority")
 *     .labels("priority");
 * queueSize.labels("high").set(10);
 * queueSize.labels("low").set(100);
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
public interface Gauge extends Metric {

    /**
     * Sets the gauge to the specified value.
     *
     * @param value the new value
     * @since 1.0.0
     */
    void set(double value);

    /**
     * Sets the gauge to the specified value.
     *
     * @param value the new value
     * @since 1.0.0
     */
    default void set(long value) {
        set((double) value);
    }

    /**
     * Returns the current value of the gauge.
     *
     * @return the current value
     * @since 1.0.0
     */
    double get();

    /**
     * Increments the gauge by one.
     *
     * @since 1.0.0
     */
    default void increment() {
        increment(1.0);
    }

    /**
     * Increments the gauge by the specified amount.
     *
     * @param amount the amount to increment
     * @since 1.0.0
     */
    void increment(double amount);

    /**
     * Decrements the gauge by one.
     *
     * @since 1.0.0
     */
    default void decrement() {
        decrement(1.0);
    }

    /**
     * Decrements the gauge by the specified amount.
     *
     * @param amount the amount to decrement
     * @since 1.0.0
     */
    void decrement(double amount);

    /**
     * Creates a labeled child gauge.
     *
     * @param labelValues the label values
     * @return a child gauge with the specified labels
     * @throws IllegalArgumentException if the number of values doesn't match
     * @since 1.0.0
     */
    @NotNull
    Gauge labels(@NotNull String... labelValues);

    /**
     * Resets the gauge to zero.
     *
     * @since 1.0.0
     */
    void reset();

    /**
     * A labeled gauge that is a child of a parent gauge.
     *
     * @since 1.0.0
     */
    interface Labeled extends Gauge {

        /**
         * Returns the label values for this gauge.
         *
         * @return the label values
         * @since 1.0.0
         */
        @NotNull
        String[] labelValues();
    }

    /**
     * A gauge backed by a supplier function.
     *
     * <p>The supplier is called each time the gauge value is requested.
     * This is useful for gauges that reflect a value from another source.
     *
     * @since 1.0.0
     */
    interface Supplier extends Gauge {

        /**
         * {@inheritDoc}
         *
         * <p>For supplier-based gauges, this method has no effect.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        default void set(double value) {
            throw new UnsupportedOperationException("Cannot set value on a supplier-based gauge");
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        default void increment(double amount) {
            throw new UnsupportedOperationException("Cannot increment a supplier-based gauge");
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        default void decrement(double amount) {
            throw new UnsupportedOperationException("Cannot decrement a supplier-based gauge");
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        default void reset() {
            throw new UnsupportedOperationException("Cannot reset a supplier-based gauge");
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        @NotNull
        default Gauge labels(@NotNull String... labelValues) {
            throw new UnsupportedOperationException("Supplier gauges do not support labels");
        }
    }

}
