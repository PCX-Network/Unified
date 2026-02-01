/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.health;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the health status of a module with associated metrics.
 *
 * <p>This record encapsulates the health state of a module including:
 * <ul>
 *   <li>The health level (HEALTHY, WARNING, DEGRADED, UNHEALTHY)</li>
 *   <li>A human-readable message describing the status</li>
 *   <li>A map of metrics for monitoring and debugging</li>
 *   <li>The timestamp when the status was recorded</li>
 * </ul>
 *
 * <h2>Health Levels</h2>
 * <ul>
 *   <li><b>HEALTHY</b>: Module is operating normally</li>
 *   <li><b>WARNING</b>: Module is operational but approaching limits</li>
 *   <li><b>DEGRADED</b>: Module has reduced functionality or performance</li>
 *   <li><b>UNHEALTHY</b>: Module is not functioning correctly</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Creating Status</h3>
 * <pre>{@code
 * // Simple healthy status
 * HealthStatus status = HealthStatus.healthy();
 *
 * // Healthy with message
 * HealthStatus status = HealthStatus.healthy("All systems operational");
 *
 * // Healthy with metrics
 * HealthStatus status = HealthStatus.healthy("Operating normally", Map.of(
 *     "activeConnections", 15,
 *     "cacheHitRate", 0.95,
 *     "avgResponseTime", 45
 * ));
 *
 * // Unhealthy status
 * HealthStatus status = HealthStatus.unhealthy("Database connection lost");
 * }</pre>
 *
 * <h3>Checking Status</h3>
 * <pre>{@code
 * HealthStatus status = module.checkHealth();
 *
 * if (status.isHealthy()) {
 *     // Normal operation
 * } else if (status.getLevel() == HealthLevel.DEGRADED) {
 *     logger.warn("Module degraded: " + status.getMessage());
 * } else if (status.isUnhealthy()) {
 *     logger.error("Module unhealthy: " + status.getMessage());
 *     // Take corrective action
 * }
 *
 * // Access metrics
 * int activeConns = (int) status.getMetric("activeConnections", 0);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see HealthCheck
 * @see HealthLevel
 */
public record HealthStatus(
        @NotNull HealthLevel level,
        @NotNull String message,
        @NotNull Map<String, Object> metrics,
        @NotNull Instant timestamp
) {

    /**
     * Constructs a health status with validation.
     *
     * @param level     the health level
     * @param message   the status message
     * @param metrics   the associated metrics
     * @param timestamp the time this status was recorded
     */
    public HealthStatus {
        Objects.requireNonNull(level, "Health level cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
        metrics = metrics != null ? Collections.unmodifiableMap(new HashMap<>(metrics)) : Collections.emptyMap();
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    /**
     * Creates a healthy status with no message or metrics.
     *
     * @return a healthy status
     */
    @NotNull
    public static HealthStatus healthy() {
        return healthy("OK");
    }

    /**
     * Creates a healthy status with a message.
     *
     * @param message the status message
     * @return a healthy status
     */
    @NotNull
    public static HealthStatus healthy(@NotNull String message) {
        return healthy(message, null);
    }

    /**
     * Creates a healthy status with a message and metrics.
     *
     * @param message the status message
     * @param metrics the associated metrics
     * @return a healthy status
     */
    @NotNull
    public static HealthStatus healthy(@NotNull String message, @Nullable Map<String, Object> metrics) {
        return new HealthStatus(HealthLevel.HEALTHY, message, metrics, Instant.now());
    }

    /**
     * Creates a warning status with a message.
     *
     * @param message the warning message
     * @return a warning status
     */
    @NotNull
    public static HealthStatus warning(@NotNull String message) {
        return warning(message, null);
    }

    /**
     * Creates a warning status with a message and metrics.
     *
     * @param message the warning message
     * @param metrics the associated metrics
     * @return a warning status
     */
    @NotNull
    public static HealthStatus warning(@NotNull String message, @Nullable Map<String, Object> metrics) {
        return new HealthStatus(HealthLevel.WARNING, message, metrics, Instant.now());
    }

    /**
     * Creates a degraded status with a message.
     *
     * @param message the degraded message
     * @return a degraded status
     */
    @NotNull
    public static HealthStatus degraded(@NotNull String message) {
        return degraded(message, null);
    }

    /**
     * Creates a degraded status with a message and metrics.
     *
     * @param message the degraded message
     * @param metrics the associated metrics
     * @return a degraded status
     */
    @NotNull
    public static HealthStatus degraded(@NotNull String message, @Nullable Map<String, Object> metrics) {
        return new HealthStatus(HealthLevel.DEGRADED, message, metrics, Instant.now());
    }

    /**
     * Creates an unhealthy status with a message.
     *
     * @param message the unhealthy message
     * @return an unhealthy status
     */
    @NotNull
    public static HealthStatus unhealthy(@NotNull String message) {
        return unhealthy(message, null);
    }

    /**
     * Creates an unhealthy status with a message and metrics.
     *
     * @param message the unhealthy message
     * @param metrics the associated metrics
     * @return an unhealthy status
     */
    @NotNull
    public static HealthStatus unhealthy(@NotNull String message, @Nullable Map<String, Object> metrics) {
        return new HealthStatus(HealthLevel.UNHEALTHY, message, metrics, Instant.now());
    }

    /**
     * Returns whether this status indicates healthy operation.
     *
     * @return {@code true} if the level is HEALTHY
     */
    public boolean isHealthy() {
        return level == HealthLevel.HEALTHY;
    }

    /**
     * Returns whether this status indicates unhealthy operation.
     *
     * @return {@code true} if the level is UNHEALTHY
     */
    public boolean isUnhealthy() {
        return level == HealthLevel.UNHEALTHY;
    }

    /**
     * Returns whether this status indicates any issues (not HEALTHY).
     *
     * @return {@code true} if the level is WARNING, DEGRADED, or UNHEALTHY
     */
    public boolean hasIssues() {
        return level != HealthLevel.HEALTHY;
    }

    /**
     * Returns a metric value, or a default if not present.
     *
     * @param key          the metric key
     * @param defaultValue the default value if not found
     * @param <T>          the expected type of the metric
     * @return the metric value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetric(@NotNull String key, T defaultValue) {
        Object value = metrics.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a metric value as a double, or a default if not present.
     *
     * @param key          the metric key
     * @param defaultValue the default value if not found
     * @return the metric value as a double
     */
    public double getDoubleMetric(@NotNull String key, double defaultValue) {
        Object value = metrics.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    /**
     * Returns a metric value as a long, or a default if not present.
     *
     * @param key          the metric key
     * @param defaultValue the default value if not found
     * @return the metric value as a long
     */
    public long getLongMetric(@NotNull String key, long defaultValue) {
        Object value = metrics.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return defaultValue;
    }

    /**
     * Returns a new status with an additional metric.
     *
     * @param key   the metric key
     * @param value the metric value
     * @return a new status with the added metric
     */
    @NotNull
    public HealthStatus withMetric(@NotNull String key, @NotNull Object value) {
        Map<String, Object> newMetrics = new HashMap<>(metrics);
        newMetrics.put(key, value);
        return new HealthStatus(level, message, newMetrics, timestamp);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (metrics=%d)", level, message, metrics.size());
    }
}
