/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.health;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Context provided to modules during health state transitions.
 *
 * <p>This context is passed to {@link sh.pcx.unified.modules.lifecycle.Healthy}
 * interface methods when the server TPS transitions above or below configured thresholds.
 * It provides information about the current server state and utilities for modules
 * to adapt their behavior accordingly.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Override
 * public void ifUnhealthy(HealthContext context) {
 *     context.getLogger().warn(
 *         "Entering low-performance mode (TPS: %.1f, threshold: %.1f)",
 *         context.getTps(),
 *         context.getThreshold()
 *     );
 *
 *     // Adapt behavior based on how severe the TPS drop is
 *     double tpsSeverity = context.getTpsSeverity();
 *     if (tpsSeverity > 0.5) {
 *         // Severe drop - aggressive reduction
 *         particleMultiplier = 0.1;
 *         saveInterval = 300;
 *     } else {
 *         // Moderate drop - gentle reduction
 *         particleMultiplier = 0.5;
 *         saveInterval = 120;
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.modules.lifecycle.Healthy
 * @see TPSTracker
 */
public final class HealthContext {

    private final double tps;
    private final double threshold;
    private final double recoveryThreshold;
    private final Logger logger;
    private final TPSTracker tpsTracker;

    /**
     * Constructs a health context with the specified values.
     *
     * @param tps               the current server TPS
     * @param threshold         the health threshold (TPS below = unhealthy)
     * @param recoveryThreshold the recovery threshold (TPS above = healthy)
     * @param logger            the logger for the module
     * @param tpsTracker        the TPS tracker for historical data
     */
    public HealthContext(
            double tps,
            double threshold,
            double recoveryThreshold,
            @NotNull Logger logger,
            @NotNull TPSTracker tpsTracker
    ) {
        this.tps = tps;
        this.threshold = threshold;
        this.recoveryThreshold = recoveryThreshold;
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.tpsTracker = Objects.requireNonNull(tpsTracker, "TPS tracker cannot be null");
    }

    /**
     * Returns the current server TPS (ticks per second).
     *
     * <p>Normal Minecraft servers run at 20 TPS. Lower values indicate
     * the server is struggling to keep up.
     *
     * @return the current TPS (0.0 to 20.0)
     */
    public double getTps() {
        return tps;
    }

    /**
     * Returns the configured health threshold.
     *
     * <p>When TPS drops below this value, modules implementing
     * {@link sh.pcx.unified.modules.lifecycle.Healthy} receive
     * the {@code ifUnhealthy} callback.
     *
     * @return the health threshold TPS
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Returns the configured recovery threshold.
     *
     * <p>When TPS rises above this value, modules implementing
     * {@link sh.pcx.unified.modules.lifecycle.Healthy} receive
     * the {@code ifBackToHealth} callback.
     *
     * @return the recovery threshold TPS
     */
    public double getRecoveryThreshold() {
        return recoveryThreshold;
    }

    /**
     * Returns the logger for this module.
     *
     * <p>Use this to log status messages during health transitions.
     *
     * @return the module logger
     */
    @NotNull
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the TPS tracker for historical data.
     *
     * @return the TPS tracker
     */
    @NotNull
    public TPSTracker getTpsTracker() {
        return tpsTracker;
    }

    /**
     * Returns the severity of the TPS drop as a value between 0.0 and 1.0.
     *
     * <p>This measures how far below the threshold the current TPS is:
     * <ul>
     *   <li>0.0 = TPS is at or above threshold</li>
     *   <li>0.5 = TPS is halfway between threshold and 0</li>
     *   <li>1.0 = TPS is at or near 0</li>
     * </ul>
     *
     * <p>Use this to scale your performance reductions proportionally.
     *
     * @return the TPS drop severity (0.0 to 1.0)
     */
    public double getTpsSeverity() {
        if (tps >= threshold) {
            return 0.0;
        }
        return Math.min(1.0, (threshold - tps) / threshold);
    }

    /**
     * Returns the percentage of normal TPS the server is running at.
     *
     * <p>Based on the ideal 20 TPS:
     * <ul>
     *   <li>100% = TPS at 20 (perfect)</li>
     *   <li>90% = TPS at 18</li>
     *   <li>50% = TPS at 10</li>
     * </ul>
     *
     * @return the TPS percentage (0 to 100)
     */
    public double getTpsPercentage() {
        return (tps / 20.0) * 100.0;
    }

    /**
     * Returns whether the current TPS is critically low.
     *
     * <p>Critical is defined as TPS below 10 (50% of normal).
     *
     * @return {@code true} if TPS is critically low
     */
    public boolean isCritical() {
        return tps < 10.0;
    }

    /**
     * Returns the average TPS over the last minute.
     *
     * @return the 1-minute average TPS
     */
    public double getAverageTps() {
        return tpsTracker.getAverageTps();
    }

    /**
     * Returns the minimum TPS recorded in the last minute.
     *
     * @return the minimum TPS
     */
    public double getMinTps() {
        return tpsTracker.getMinTps();
    }

    @Override
    public String toString() {
        return String.format(
                "HealthContext{tps=%.1f, threshold=%.1f, recovery=%.1f, severity=%.2f}",
                tps, threshold, recoveryThreshold, getTpsSeverity()
        );
    }
}
