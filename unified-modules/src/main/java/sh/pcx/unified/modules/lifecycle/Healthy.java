/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.lifecycle;

import sh.pcx.unified.modules.health.HealthContext;

/**
 * Interface for modules that respond to server health/TPS changes.
 *
 * <p>Modules implementing this interface are notified when server TPS
 * drops below or recovers above configured thresholds. This allows
 * modules to reduce their load during performance issues and resume
 * normal operation when the server recovers.
 *
 * <h2>Health Monitoring Flow</h2>
 * <pre>
 *         TPS Normal (20.0)
 *              │
 *              ▼
 *     ┌────────────────────┐
 *     │  Module Operating  │◄──────────────┐
 *     │     Normally       │               │
 *     └─────────┬──────────┘               │
 *               │                          │
 *     TPS drops below threshold            │
 *     (e.g., < 18.0)                       │
 *               │                          │
 *               ▼                          │
 *     ┌────────────────────┐               │
 *     │  ifUnhealthy()     │               │
 *     │  called            │               │
 *     └─────────┬──────────┘               │
 *               │                          │
 *               ▼                          │ TPS recovers above
 *     ┌────────────────────┐               │ recovery threshold
 *     │  Low Performance   │               │ (e.g., > 19.5)
 *     │     Mode           │───────────────┘
 *     └────────────────────┘               │
 *               │                          │
 *               ▼                          │
 *     ┌────────────────────┐               │
 *     │  ifBackToHealth()  │               │
 *     │  called            │───────────────┘
 *     └────────────────────┘
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * public class BattlePassModule implements Initializable, Healthy {
 *
 *     private PassProgressManager progressManager;
 *     private boolean lowPerformanceMode = false;
 *
 *     @Override
 *     public void ifUnhealthy(HealthContext context) {
 *         lowPerformanceMode = true;
 *         progressManager.enableLowPerformanceMode();
 *
 *         // Reduce particle effects, async saves, etc.
 *         progressManager.setParticleMultiplier(0.1);
 *         progressManager.setSaveInterval(300); // Less frequent saves
 *
 *         context.getLogger().warn(
 *             "BattlePass entering low-performance mode (TPS: %.1f)",
 *             context.getTps()
 *         );
 *     }
 *
 *     @Override
 *     public void ifBackToHealth(HealthContext context) {
 *         lowPerformanceMode = false;
 *         progressManager.disableLowPerformanceMode();
 *
 *         // Restore normal operation
 *         progressManager.setParticleMultiplier(1.0);
 *         progressManager.setSaveInterval(60);
 *
 *         context.getLogger().info("BattlePass resuming normal operation");
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>Health thresholds are configured when building the ModuleManager:
 *
 * <pre>{@code
 * ModuleManager modules = ModuleManager.builder(this)
 *     .enableHealthMonitoring(true)
 *     .healthThreshold(18.0)      // TPS below this = unhealthy
 *     .recoveryThreshold(19.5)    // TPS above this = healthy again
 *     .checkInterval(Duration.ofSeconds(5))
 *     .build();
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Reduce CPU-intensive operations (particles, calculations)</li>
 *   <li>Increase intervals between scheduled tasks</li>
 *   <li>Batch or defer non-critical operations</li>
 *   <li>Skip cosmetic features (particles, sounds)</li>
 *   <li>Don't disable critical functionality (data saving)</li>
 *   <li>Log state changes for debugging</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see HealthContext
 * @see sh.pcx.unified.modules.health.TPSTracker
 */
public interface Healthy {

    /**
     * Called when server TPS drops below the configured health threshold.
     *
     * <p>This indicates the server is under load and modules should
     * reduce their resource usage to help TPS recover.
     *
     * <p>Common actions:
     * <ul>
     *   <li>Reduce particle effects and visual flair</li>
     *   <li>Increase task intervals</li>
     *   <li>Defer non-critical operations</li>
     *   <li>Skip expensive calculations</li>
     *   <li>Batch operations together</li>
     * </ul>
     *
     * @param context the health context with current TPS and utilities
     */
    void ifUnhealthy(HealthContext context);

    /**
     * Called when server TPS recovers above the configured recovery threshold.
     *
     * <p>This indicates the server has recovered and modules can resume
     * normal operation.
     *
     * <p>Common actions:
     * <ul>
     *   <li>Restore normal particle effects</li>
     *   <li>Reset task intervals to normal</li>
     *   <li>Resume deferred operations</li>
     *   <li>Clear any operation queues</li>
     * </ul>
     *
     * @param context the health context with current TPS and utilities
     */
    void ifBackToHealth(HealthContext context);
}
