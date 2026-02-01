/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Service for creating and managing metrics.
 *
 * <p>The MetricsService provides a unified interface for creating and
 * registering metrics that can be exported to various monitoring systems
 * including Prometheus and bStats.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private MetricsService metrics;
 *
 * // Counter
 * Counter requests = metrics.counter("api_requests_total", "Total API requests");
 * requests.increment();
 * requests.increment(5);
 *
 * // Gauge
 * metrics.gauge("players_online", () -> Bukkit.getOnlinePlayers().size());
 * metrics.gauge("cache_size", cache::size);
 *
 * // Timer
 * Timer timer = metrics.timer("database_query_duration", "Database query time");
 * try (Timer.Context ctx = timer.time()) {
 *     database.query(...);
 * }
 *
 * // Histogram
 * Histogram sizes = metrics.histogram("response_size_bytes", "Response sizes")
 *     .buckets(100, 500, 1000, 5000)
 *     .build();
 *
 * // Custom metrics
 * metrics.register("custom_metric", () -> calculateCustomValue());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Counter
 * @see Gauge
 * @see Timer
 * @see Histogram
 */
public interface MetricsService extends Service {

    /**
     * Creates or retrieves a counter with the given name.
     *
     * @param name        the metric name
     * @param description the metric description
     * @return the counter
     * @since 1.0.0
     */
    @NotNull
    Counter counter(@NotNull String name, @Nullable String description);

    /**
     * Creates or retrieves a counter with the given name.
     *
     * @param name the metric name
     * @return the counter
     * @since 1.0.0
     */
    @NotNull
    default Counter counter(@NotNull String name) {
        return counter(name, null);
    }

    /**
     * Creates or retrieves a counter with labels.
     *
     * @param name        the metric name
     * @param description the metric description
     * @param labelNames  the label names
     * @return the counter
     * @since 1.0.0
     */
    @NotNull
    Counter counter(@NotNull String name, @Nullable String description, @NotNull String... labelNames);

    /**
     * Creates or retrieves a gauge with the given name.
     *
     * @param name        the metric name
     * @param description the metric description
     * @return the gauge
     * @since 1.0.0
     */
    @NotNull
    Gauge gauge(@NotNull String name, @Nullable String description);

    /**
     * Creates or retrieves a gauge with the given name.
     *
     * @param name the metric name
     * @return the gauge
     * @since 1.0.0
     */
    @NotNull
    default Gauge gauge(@NotNull String name) {
        return gauge(name, (String) null);
    }

    /**
     * Creates or retrieves a gauge backed by a double supplier.
     *
     * @param name        the metric name
     * @param description the metric description
     * @param supplier    the value supplier
     * @return the gauge
     * @since 1.0.0
     */
    @NotNull
    Gauge gauge(@NotNull String name, @Nullable String description, @NotNull DoubleSupplier supplier);

    /**
     * Creates or retrieves a gauge backed by a long supplier.
     *
     * @param name     the metric name
     * @param supplier the value supplier
     * @return the gauge
     * @since 1.0.0
     */
    @NotNull
    default Gauge gauge(@NotNull String name, @NotNull LongSupplier supplier) {
        return gauge(name, null, (DoubleSupplier) supplier::getAsLong);
    }

    /**
     * Creates or retrieves a gauge backed by a number supplier.
     *
     * @param name     the metric name
     * @param supplier the value supplier
     * @return the gauge
     * @since 1.0.0
     */
    @NotNull
    default Gauge gauge(@NotNull String name, @NotNull Supplier<? extends Number> supplier) {
        return gauge(name, null, () -> supplier.get().doubleValue());
    }

    /**
     * Creates or retrieves a gauge with labels.
     *
     * @param name        the metric name
     * @param description the metric description
     * @param labelNames  the label names
     * @return the gauge
     * @since 1.0.0
     */
    @NotNull
    Gauge gauge(@NotNull String name, @Nullable String description, @NotNull String... labelNames);

    /**
     * Creates or retrieves a timer with the given name.
     *
     * @param name        the metric name
     * @param description the metric description
     * @return the timer
     * @since 1.0.0
     */
    @NotNull
    Timer timer(@NotNull String name, @Nullable String description);

    /**
     * Creates or retrieves a timer with the given name.
     *
     * @param name the metric name
     * @return the timer
     * @since 1.0.0
     */
    @NotNull
    default Timer timer(@NotNull String name) {
        return timer(name, null);
    }

    /**
     * Creates or retrieves a timer with labels.
     *
     * @param name        the metric name
     * @param description the metric description
     * @param labelNames  the label names
     * @return the timer
     * @since 1.0.0
     */
    @NotNull
    Timer timer(@NotNull String name, @Nullable String description, @NotNull String... labelNames);

    /**
     * Creates a histogram builder.
     *
     * @param name        the metric name
     * @param description the metric description
     * @return the histogram builder
     * @since 1.0.0
     */
    @NotNull
    Histogram.Builder histogram(@NotNull String name, @Nullable String description);

    /**
     * Creates a histogram builder.
     *
     * @param name the metric name
     * @return the histogram builder
     * @since 1.0.0
     */
    @NotNull
    default Histogram.Builder histogram(@NotNull String name) {
        return histogram(name, null);
    }

    /**
     * Registers a custom metric backed by a supplier.
     *
     * @param name     the metric name
     * @param supplier the value supplier
     * @return the gauge
     * @since 1.0.0
     */
    @NotNull
    default Gauge register(@NotNull String name, @NotNull DoubleSupplier supplier) {
        return gauge(name, null, supplier);
    }

    /**
     * Retrieves a metric by name.
     *
     * @param name the metric name
     * @return an Optional containing the metric if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Metric> get(@NotNull String name);

    /**
     * Removes a metric by name.
     *
     * @param name the metric name
     * @return true if the metric was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull String name);

    /**
     * Returns all registered metrics.
     *
     * @return collection of all metrics
     * @since 1.0.0
     */
    @NotNull
    Collection<Metric> all();

    /**
     * Returns all metrics of a specific type.
     *
     * @param type the metric type
     * @return collection of metrics of the given type
     * @since 1.0.0
     */
    @NotNull
    Collection<Metric> byType(@NotNull Metric.Type type);

    /**
     * Returns the Prometheus exporter for these metrics.
     *
     * @return the Prometheus exporter
     * @since 1.0.0
     */
    @NotNull
    PrometheusExporter prometheus();

    /**
     * Returns the bStats integration for these metrics.
     *
     * @return the bStats integration
     * @since 1.0.0
     */
    @NotNull
    BStatsIntegration bstats();

    /**
     * Clears all registered metrics.
     *
     * @since 1.0.0
     */
    void clear();
}
