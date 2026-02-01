/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import com.sun.net.httpserver.HttpServer;
import sh.pcx.unified.tools.metrics.Metric;
import sh.pcx.unified.tools.metrics.PrometheusExporter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Default implementation of {@link PrometheusExporter}.
 *
 * @since 1.0.0
 */
public final class DefaultPrometheusExporter implements PrometheusExporter {

    private final DefaultMetricsService metricsService;
    private final Map<String, String> defaultLabels = new ConcurrentHashMap<>();
    private String prefix = "";
    private volatile HttpServer server;
    private volatile int serverPort = -1;

    /**
     * Creates a new Prometheus exporter.
     *
     * @param metricsService the metrics service
     */
    public DefaultPrometheusExporter(DefaultMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public @NotNull String export() {
        StringBuilder sb = new StringBuilder();
        for (Metric metric : metricsService.all()) {
            sb.append(export(metric));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public void writeTo(@NotNull Writer writer) throws IOException {
        writer.write(export());
    }

    @Override
    public @NotNull String export(@NotNull Metric metric) {
        StringBuilder sb = new StringBuilder();
        String metricName = prefix + sanitizeName(metric.name());

        // Write HELP comment
        if (metric.description() != null) {
            sb.append("# HELP ").append(metricName).append(" ").append(metric.description()).append("\n");
        }

        // Write TYPE comment
        sb.append("# TYPE ").append(metricName).append(" ").append(getPrometheusType(metric.type())).append("\n");

        // Write metric value(s)
        switch (metric) {
            case SimpleCounter counter -> exportCounter(sb, metricName, counter);
            case SimpleGauge gauge -> exportGauge(sb, metricName, gauge);
            case SimpleGauge.SupplierGauge gauge -> exportSupplierGauge(sb, metricName, gauge);
            case SimpleTimer timer -> exportTimer(sb, metricName, timer);
            case SimpleHistogram histogram -> exportHistogram(sb, metricName, histogram);
            default -> sb.append(metricName).append(" ").append("0\n");
        }

        return sb.toString();
    }

    private void exportCounter(StringBuilder sb, String name, SimpleCounter counter) {
        if (!counter.hasLabels()) {
            sb.append(name).append(formatLabels(null)).append(" ").append(counter.get()).append("\n");
        } else {
            // Export labeled children
            for (var entry : counter.children().entrySet()) {
                String labels = formatLabels(counter.labelNames(), entry.getKey().values());
                sb.append(name).append(labels).append(" ").append(entry.getValue().get()).append("\n");
            }
        }
    }

    private void exportGauge(StringBuilder sb, String name, SimpleGauge gauge) {
        if (!gauge.hasLabels()) {
            sb.append(name).append(formatLabels(null)).append(" ").append(gauge.get()).append("\n");
        } else {
            for (var entry : gauge.children().entrySet()) {
                String labels = formatLabels(gauge.labelNames(), entry.getKey().values());
                sb.append(name).append(labels).append(" ").append(entry.getValue().get()).append("\n");
            }
        }
    }

    private void exportSupplierGauge(StringBuilder sb, String name, SimpleGauge.SupplierGauge gauge) {
        sb.append(name).append(formatLabels(null)).append(" ").append(gauge.get()).append("\n");
    }

    private void exportTimer(StringBuilder sb, String name, SimpleTimer timer) {
        // Export as summary-like metrics
        sb.append(name).append("_count").append(formatLabels(null)).append(" ").append(timer.count()).append("\n");
        sb.append(name).append("_sum").append(formatLabels(null)).append(" ")
                .append(timer.totalTime().toNanos() / 1_000_000_000.0).append("\n");
    }

    private void exportHistogram(StringBuilder sb, String name, SimpleHistogram histogram) {
        double[] boundaries = histogram.bucketBoundaries();
        long[] counts = histogram.bucketCounts();
        long cumulative = 0;

        for (int i = 0; i < boundaries.length; i++) {
            cumulative += counts[i];
            sb.append(name).append("_bucket{le=\"").append(boundaries[i]).append("\"} ")
                    .append(cumulative).append("\n");
        }
        cumulative += counts[counts.length - 1];
        sb.append(name).append("_bucket{le=\"+Inf\"} ").append(cumulative).append("\n");

        sb.append(name).append("_sum").append(formatLabels(null)).append(" ").append(histogram.sum()).append("\n");
        sb.append(name).append("_count").append(formatLabels(null)).append(" ").append(histogram.count()).append("\n");
    }

    private String formatLabels(String[] names, String... values) {
        if ((names == null || names.length == 0) && defaultLabels.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        // Add default labels
        for (var entry : defaultLabels.entrySet()) {
            if (!first) sb.append(",");
            sb.append(entry.getKey()).append("=\"").append(escapeLabel(entry.getValue())).append("\"");
            first = false;
        }

        // Add metric-specific labels
        if (names != null && values != null) {
            for (int i = 0; i < names.length && i < values.length; i++) {
                if (!first) sb.append(",");
                sb.append(names[i]).append("=\"").append(escapeLabel(values[i])).append("\"");
                first = false;
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_:]", "_");
    }

    private String escapeLabel(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String getPrometheusType(Metric.Type type) {
        return switch (type) {
            case COUNTER -> "counter";
            case GAUGE -> "gauge";
            case TIMER -> "summary";
            case HISTOGRAM -> "histogram";
            case SUMMARY -> "summary";
        };
    }

    @Override
    public @NotNull CompletableFuture<Void> startServer(int port, @NotNull String path) {
        return CompletableFuture.runAsync(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext(path, exchange -> {
                    String response = export();
                    byte[] bytes = response.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
                server.setExecutor(Executors.newSingleThreadExecutor());
                server.start();
                serverPort = port;
            } catch (IOException e) {
                throw new RuntimeException("Failed to start Prometheus HTTP server", e);
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> stopServer() {
        return CompletableFuture.runAsync(() -> {
            if (server != null) {
                server.stop(1);
                server = null;
                serverPort = -1;
            }
        });
    }

    @Override
    public boolean isServerRunning() {
        return server != null;
    }

    @Override
    public int serverPort() {
        return serverPort;
    }

    @Override
    public void setPrefix(@NotNull String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void addDefaultLabel(@NotNull String name, @NotNull String value) {
        defaultLabels.put(name, value);
    }
}
