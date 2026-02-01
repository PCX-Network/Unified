/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import sh.pcx.unified.tools.metrics.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link MetricsService}.
 *
 * @since 1.0.0
 */
public final class DefaultMetricsService implements MetricsService {

    private final Map<String, Metric> metrics = new ConcurrentHashMap<>();
    private final DefaultPrometheusExporter prometheusExporter;
    private final DefaultBStatsIntegration bstatsIntegration;

    /**
     * Creates a new metrics service.
     */
    public DefaultMetricsService() {
        this.prometheusExporter = new DefaultPrometheusExporter(this);
        this.bstatsIntegration = new DefaultBStatsIntegration();
    }

    @Override
    public @NotNull Counter counter(@NotNull String name, @Nullable String description) {
        return (Counter) metrics.computeIfAbsent(name, n ->
                new SimpleCounter(n, description)
        );
    }

    @Override
    public @NotNull Counter counter(@NotNull String name, @Nullable String description, @NotNull String... labelNames) {
        return (Counter) metrics.computeIfAbsent(name, n ->
                new SimpleCounter(n, description, labelNames)
        );
    }

    @Override
    public @NotNull Gauge gauge(@NotNull String name, @Nullable String description) {
        return (Gauge) metrics.computeIfAbsent(name, n ->
                new SimpleGauge(n, description)
        );
    }

    @Override
    public @NotNull Gauge gauge(@NotNull String name, @Nullable String description, @NotNull DoubleSupplier supplier) {
        return (Gauge) metrics.computeIfAbsent(name, n ->
                new SimpleGauge.SupplierGauge(n, description, supplier)
        );
    }

    @Override
    public @NotNull Gauge gauge(@NotNull String name, @Nullable String description, @NotNull String... labelNames) {
        return (Gauge) metrics.computeIfAbsent(name, n ->
                new SimpleGauge(n, description, labelNames)
        );
    }

    @Override
    public @NotNull Timer timer(@NotNull String name, @Nullable String description) {
        return (Timer) metrics.computeIfAbsent(name, n ->
                new SimpleTimer(n, description)
        );
    }

    @Override
    public @NotNull Timer timer(@NotNull String name, @Nullable String description, @NotNull String... labelNames) {
        return (Timer) metrics.computeIfAbsent(name, n ->
                new SimpleTimer(n, description, labelNames)
        );
    }

    @Override
    public Histogram.@NotNull Builder histogram(@NotNull String name, @Nullable String description) {
        return new SimpleHistogram.HistogramBuilder(name, description) {
            @Override
            public @NotNull Histogram build() {
                Histogram histogram = super.build();
                metrics.put(name, histogram);
                return histogram;
            }
        };
    }

    @Override
    public @NotNull Optional<Metric> get(@NotNull String name) {
        return Optional.ofNullable(metrics.get(name));
    }

    @Override
    public boolean remove(@NotNull String name) {
        return metrics.remove(name) != null;
    }

    @Override
    public @NotNull Collection<Metric> all() {
        return List.copyOf(metrics.values());
    }

    @Override
    public @NotNull Collection<Metric> byType(@NotNull Metric.Type type) {
        return metrics.values().stream()
                .filter(m -> m.type() == type)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull PrometheusExporter prometheus() {
        return prometheusExporter;
    }

    @Override
    public @NotNull BStatsIntegration bstats() {
        return bstatsIntegration;
    }

    @Override
    public void clear() {
        metrics.clear();
    }

    @Override
    public String getServiceName() {
        return "MetricsService";
    }
}
