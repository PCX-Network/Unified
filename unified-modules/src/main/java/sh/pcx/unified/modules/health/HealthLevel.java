/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.health;

/**
 * Represents the severity level of a health status.
 *
 * <p>Health levels are ordered from best to worst:
 * HEALTHY > WARNING > DEGRADED > UNHEALTHY
 *
 * <h2>Level Descriptions</h2>
 * <ul>
 *   <li><b>HEALTHY</b>: Everything is operating normally</li>
 *   <li><b>WARNING</b>: Operational but approaching limits or potential issues</li>
 *   <li><b>DEGRADED</b>: Reduced functionality or performance</li>
 *   <li><b>UNHEALTHY</b>: Critical issues preventing normal operation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * HealthLevel level = status.level();
 *
 * switch (level) {
 *     case HEALTHY -> logger.info("System healthy");
 *     case WARNING -> logger.warn("System warning: " + status.message());
 *     case DEGRADED -> logger.warn("System degraded: " + status.message());
 *     case UNHEALTHY -> logger.error("System unhealthy: " + status.message());
 * }
 *
 * // Compare levels
 * if (level.isWorseThan(HealthLevel.WARNING)) {
 *     // Take action for DEGRADED or UNHEALTHY
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see HealthStatus
 */
public enum HealthLevel {

    /**
     * Everything is operating normally.
     *
     * <p>This level indicates:
     * <ul>
     *   <li>All systems functional</li>
     *   <li>Performance within expected ranges</li>
     *   <li>No issues detected</li>
     * </ul>
     */
    HEALTHY(0, "green"),

    /**
     * Operational but approaching limits or potential issues detected.
     *
     * <p>This level indicates:
     * <ul>
     *   <li>Resource usage approaching thresholds</li>
     *   <li>Minor issues that may escalate</li>
     *   <li>Potential problems detected but not affecting operation</li>
     * </ul>
     */
    WARNING(1, "yellow"),

    /**
     * Reduced functionality or performance.
     *
     * <p>This level indicates:
     * <ul>
     *   <li>Some features unavailable</li>
     *   <li>Performance significantly reduced</li>
     *   <li>Fallback mechanisms in use</li>
     *   <li>Partial outage</li>
     * </ul>
     */
    DEGRADED(2, "orange"),

    /**
     * Critical issues preventing normal operation.
     *
     * <p>This level indicates:
     * <ul>
     *   <li>Critical functionality unavailable</li>
     *   <li>Complete failure of component</li>
     *   <li>Immediate attention required</li>
     *   <li>Full outage</li>
     * </ul>
     */
    UNHEALTHY(3, "red");

    private final int severity;
    private final String color;

    /**
     * Constructs a health level.
     *
     * @param severity the numeric severity (higher = worse)
     * @param color    the display color for this level
     */
    HealthLevel(int severity, String color) {
        this.severity = severity;
        this.color = color;
    }

    /**
     * Returns the numeric severity of this level.
     *
     * <p>Higher values indicate worse health.
     *
     * @return the severity value
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * Returns the display color for this level.
     *
     * @return the color name (e.g., "green", "red")
     */
    public String getColor() {
        return color;
    }

    /**
     * Returns whether this level is worse than another.
     *
     * @param other the level to compare with
     * @return {@code true} if this level is worse
     */
    public boolean isWorseThan(HealthLevel other) {
        return this.severity > other.severity;
    }

    /**
     * Returns whether this level is better than another.
     *
     * @param other the level to compare with
     * @return {@code true} if this level is better
     */
    public boolean isBetterThan(HealthLevel other) {
        return this.severity < other.severity;
    }

    /**
     * Returns whether this level indicates healthy operation.
     *
     * @return {@code true} if this is HEALTHY
     */
    public boolean isHealthy() {
        return this == HEALTHY;
    }

    /**
     * Returns whether this level indicates critical issues.
     *
     * @return {@code true} if this is UNHEALTHY
     */
    public boolean isCritical() {
        return this == UNHEALTHY;
    }

    /**
     * Returns the worst (most severe) of two levels.
     *
     * @param a the first level
     * @param b the second level
     * @return the level with higher severity
     */
    public static HealthLevel worst(HealthLevel a, HealthLevel b) {
        return a.severity > b.severity ? a : b;
    }

    /**
     * Returns the best (least severe) of two levels.
     *
     * @param a the first level
     * @param b the second level
     * @return the level with lower severity
     */
    public static HealthLevel best(HealthLevel a, HealthLevel b) {
        return a.severity < b.severity ? a : b;
    }
}
