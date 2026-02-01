/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CompletableFuture;

/**
 * Exports metrics in Prometheus text format.
 *
 * <p>The PrometheusExporter provides functionality to export all registered
 * metrics in the Prometheus exposition format, which can be scraped by
 * Prometheus servers.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PrometheusExporter exporter = metrics.prometheus();
 *
 * // Get all metrics as string
 * String output = exporter.export();
 *
 * // Write to a writer
 * exporter.writeTo(responseWriter);
 *
 * // Start HTTP server
 * exporter.startServer(9090, "/metrics");
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * metrics:
 *   prometheus:
 *     enabled: true
 *     port: 9090
 *     path: /metrics
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MetricsService
 */
public interface PrometheusExporter {

    /**
     * Exports all metrics in Prometheus text format.
     *
     * @return the formatted metrics
     * @since 1.0.0
     */
    @NotNull
    String export();

    /**
     * Writes all metrics to the given writer in Prometheus text format.
     *
     * @param writer the writer to write to
     * @throws IOException if an I/O error occurs
     * @since 1.0.0
     */
    void writeTo(@NotNull Writer writer) throws IOException;

    /**
     * Exports metrics for a specific metric.
     *
     * @param metric the metric to export
     * @return the formatted metric
     * @since 1.0.0
     */
    @NotNull
    String export(@NotNull Metric metric);

    /**
     * Starts an HTTP server for Prometheus scraping.
     *
     * @param port the port to listen on
     * @param path the path to expose metrics on
     * @return a future that completes when the server is started
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> startServer(int port, @NotNull String path);

    /**
     * Starts an HTTP server with default configuration (port 9090, path /metrics).
     *
     * @return a future that completes when the server is started
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> startServer() {
        return startServer(9090, "/metrics");
    }

    /**
     * Stops the HTTP server if running.
     *
     * @return a future that completes when the server is stopped
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> stopServer();

    /**
     * Checks if the HTTP server is running.
     *
     * @return true if the server is running
     * @since 1.0.0
     */
    boolean isServerRunning();

    /**
     * Returns the port the server is running on.
     *
     * @return the port, or -1 if not running
     * @since 1.0.0
     */
    int serverPort();

    /**
     * Sets a prefix to add to all metric names.
     *
     * @param prefix the prefix (e.g., "minecraft_")
     * @since 1.0.0
     */
    void setPrefix(@NotNull String prefix);

    /**
     * Adds a default label to all exported metrics.
     *
     * @param name  the label name
     * @param value the label value
     * @since 1.0.0
     */
    void addDefaultLabel(@NotNull String name, @NotNull String value);
}
