/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Metrics system for monitoring and analytics.
 *
 * <p>This package provides a comprehensive metrics system with support for:
 * <ul>
 *   <li>{@link sh.pcx.unified.tools.metrics.Counter} - Monotonically increasing counters</li>
 *   <li>{@link sh.pcx.unified.tools.metrics.Gauge} - Values that can go up and down</li>
 *   <li>{@link sh.pcx.unified.tools.metrics.Timer} - Duration measurements</li>
 *   <li>{@link sh.pcx.unified.tools.metrics.Histogram} - Statistical distributions</li>
 * </ul>
 *
 * <p>The metrics system integrates with:
 * <ul>
 *   <li>Prometheus via {@link sh.pcx.unified.tools.metrics.PrometheusExporter}</li>
 *   <li>bStats via {@link sh.pcx.unified.tools.metrics.BStatsIntegration}</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private MetricsService metrics;
 *
 * // Counter
 * Counter requests = metrics.counter("api_requests_total", "Total requests");
 * requests.increment();
 *
 * // Gauge
 * metrics.gauge("players_online", () -> server.getOnlinePlayers().size());
 *
 * // Timer
 * Timer timer = metrics.timer("query_duration");
 * try (Timer.Context ctx = timer.time()) {
 *     database.query(...);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.tools.metrics.MetricsService
 */
package sh.pcx.unified.tools.metrics;
