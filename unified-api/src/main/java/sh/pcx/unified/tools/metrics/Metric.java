/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Base interface for all metric types.
 *
 * <p>Metrics are named measurements that can be collected and exported
 * to monitoring systems like Prometheus or bStats. Each metric has a
 * unique name, optional description, and optional labels.
 *
 * <h2>Naming Conventions</h2>
 * <ul>
 *   <li>Use snake_case for metric names</li>
 *   <li>Include a unit suffix when applicable (e.g., {@code _bytes}, {@code _seconds})</li>
 *   <li>Use descriptive names that indicate what the metric measures</li>
 *   <li>Prefix with your plugin name to avoid collisions</li>
 * </ul>
 *
 * <h2>Labels</h2>
 * <p>Labels allow you to partition metric data by different dimensions.
 * However, be careful with high-cardinality labels (labels with many
 * possible values) as they can significantly increase memory usage.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Counter
 * @see Gauge
 * @see Timer
 * @see Histogram
 */
public interface Metric {

    /**
     * Returns the name of this metric.
     *
     * @return the metric name
     * @since 1.0.0
     */
    @NotNull
    String name();

    /**
     * Returns the description of this metric.
     *
     * @return the description, or null if not set
     * @since 1.0.0
     */
    @Nullable
    String description();

    /**
     * Returns the metric type.
     *
     * @return the type
     * @since 1.0.0
     */
    @NotNull
    Type type();

    /**
     * Returns the label names for this metric.
     *
     * @return an array of label names, empty if no labels
     * @since 1.0.0
     */
    @NotNull
    String[] labelNames();

    /**
     * Checks if this metric has labels.
     *
     * @return true if this metric has labels
     * @since 1.0.0
     */
    default boolean hasLabels() {
        return labelNames().length > 0;
    }

    /**
     * Returns metadata associated with this metric.
     *
     * @return the metadata map
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> metadata();

    /**
     * Enumeration of metric types.
     *
     * @since 1.0.0
     */
    enum Type {
        /**
         * A counter that only increases.
         */
        COUNTER,

        /**
         * A gauge that can increase or decrease.
         */
        GAUGE,

        /**
         * A timer for measuring durations.
         */
        TIMER,

        /**
         * A histogram for statistical distribution.
         */
        HISTOGRAM,

        /**
         * A summary for percentile calculations.
         */
        SUMMARY
    }
}
