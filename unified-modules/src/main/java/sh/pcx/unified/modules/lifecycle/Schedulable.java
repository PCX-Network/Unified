/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.lifecycle;

import java.util.List;

/**
 * Interface for modules that register scheduled/repeating tasks.
 *
 * <p>Modules implementing this interface can declare their scheduled tasks
 * in a structured way. Tasks are automatically registered when the module
 * is enabled and cancelled when the module is disabled.
 *
 * <h2>Task Lifecycle</h2>
 * <ul>
 *   <li>Tasks are registered after {@link Initializable#init} completes</li>
 *   <li>Tasks are automatically cancelled when the module is disabled</li>
 *   <li>Tasks are re-registered when the module is re-enabled</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "AutoSave")
 * public class AutoSaveModule implements Initializable, Schedulable {
 *
 *     private DataManager dataManager;
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         this.dataManager = new DataManager();
 *     }
 *
 *     @Override
 *     public List<ScheduledTask> getTasks() {
 *         return List.of(
 *             // Save all data every 5 minutes
 *             ScheduledTask.builder("auto-save")
 *                 .async(true)
 *                 .period(Duration.ofMinutes(5))
 *                 .action(() -> dataManager.saveAll())
 *                 .build(),
 *
 *             // Cleanup expired entries every hour
 *             ScheduledTask.builder("cleanup")
 *                 .async(true)
 *                 .delay(Duration.ofMinutes(10))
 *                 .period(Duration.ofHours(1))
 *                 .action(() -> dataManager.cleanupExpired())
 *                 .build(),
 *
 *             // Update scoreboard every second (sync)
 *             ScheduledTask.builder("scoreboard-update")
 *                 .period(Duration.ofSeconds(1))
 *                 .action(this::updateScoreboards)
 *                 .build()
 *         );
 *     }
 *
 *     private void updateScoreboards() {
 *         // Update player scoreboards
 *     }
 * }
 * }</pre>
 *
 * <h2>Health-Aware Tasks</h2>
 * <p>Tasks can be configured to respect server health:
 *
 * <pre>{@code
 * @Override
 * public List<ScheduledTask> getTasks() {
 *     return List.of(
 *         ScheduledTask.builder("particles")
 *             .period(Duration.ofMillis(50))
 *             .action(this::spawnParticles)
 *             .pauseWhenUnhealthy(true)  // Skip during low TPS
 *             .build()
 *     );
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ScheduledTask
 * @see Initializable
 */
public interface Schedulable {

    /**
     * Returns the list of scheduled tasks for this module.
     *
     * <p>These tasks are automatically:
     * <ul>
     *   <li>Registered when the module is enabled</li>
     *   <li>Cancelled when the module is disabled</li>
     *   <li>Re-registered on module reload (if tasks changed)</li>
     * </ul>
     *
     * @return a list of scheduled tasks to register
     */
    List<ScheduledTask> getTasks();
}
