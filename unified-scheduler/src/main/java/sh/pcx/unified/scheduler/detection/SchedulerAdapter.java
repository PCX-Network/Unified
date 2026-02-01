/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.detection;

import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.TaskHandle;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Adapts platform-specific scheduler APIs to a unified interface.
 *
 * <p>SchedulerAdapter is the bridge between the unified scheduler API and
 * the underlying platform scheduler. Implementations exist for each
 * supported platform (Bukkit, Paper, Folia, Sponge).
 *
 * <h2>Platform Implementations</h2>
 * <ul>
 *   <li><b>BukkitSchedulerAdapter:</b> Uses BukkitScheduler for Spigot/Bukkit</li>
 *   <li><b>PaperSchedulerAdapter:</b> Uses Paper's async scheduler enhancements</li>
 *   <li><b>FoliaSchedulerAdapter:</b> Uses Folia's region-aware schedulers</li>
 *   <li><b>SpongeSchedulerAdapter:</b> Uses Sponge's Task API</li>
 * </ul>
 *
 * <h2>Adapter Selection</h2>
 * <p>The appropriate adapter is selected automatically based on
 * {@link SchedulerDetector#detect()}.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class BukkitSchedulerAdapter implements SchedulerAdapter {
 *
 *     private final Plugin plugin;
 *     private final BukkitScheduler scheduler;
 *
 *     public BukkitSchedulerAdapter(Plugin plugin) {
 *         this.plugin = plugin;
 *         this.scheduler = Bukkit.getScheduler();
 *     }
 *
 *     @Override
 *     public TaskHandle runTask(Task task) {
 *         BukkitTask bukkitTask = scheduler.runTask(plugin, task.getRunnable());
 *         return new BukkitTaskHandle(task, bukkitTask);
 *     }
 *
 *     // ... other methods
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchedulerDetector
 */
public interface SchedulerAdapter {

    // ==================== Immediate Execution ====================

    /**
     * Runs a task synchronously on the main thread.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTask(@NotNull Task task);

    /**
     * Runs a task asynchronously on the thread pool.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskAsync(@NotNull Task task);

    // ==================== Delayed Execution ====================

    /**
     * Runs a task synchronously after a delay.
     *
     * @param task       the task to run
     * @param delayTicks the delay in ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskLater(@NotNull Task task, long delayTicks);

    /**
     * Runs a task asynchronously after a delay.
     *
     * @param task       the task to run
     * @param delayTicks the delay in ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskLaterAsync(@NotNull Task task, long delayTicks);

    // ==================== Repeating Execution ====================

    /**
     * Runs a task synchronously on a repeating schedule.
     *
     * @param task        the task to run
     * @param delayTicks  the initial delay in ticks
     * @param periodTicks the period between executions
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskTimer(@NotNull Task task, long delayTicks, long periodTicks);

    /**
     * Runs a task asynchronously on a repeating schedule.
     *
     * @param task        the task to run
     * @param delayTicks  the initial delay in ticks
     * @param periodTicks the period between executions
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskTimerAsync(@NotNull Task task, long delayTicks, long periodTicks);

    // ==================== Entity-Bound Tasks ====================

    /**
     * Runs a task on the thread owning the specified entity.
     *
     * <p>On non-Folia platforms, this delegates to {@link #runTask}.
     *
     * @param entity          the entity to bind to
     * @param task            the task to run
     * @param retiredCallback callback if entity is retired (Folia only)
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtEntity(@NotNull Object entity, @NotNull Task task, @Nullable Runnable retiredCallback);

    /**
     * Runs a task on the thread owning the specified entity after a delay.
     *
     * @param entity          the entity to bind to
     * @param task            the task to run
     * @param delayTicks      the delay in ticks
     * @param retiredCallback callback if entity is retired
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtEntityLater(
            @NotNull Object entity,
            @NotNull Task task,
            long delayTicks,
            @Nullable Runnable retiredCallback
    );

    // ==================== Location-Bound Tasks ====================

    /**
     * Runs a task on the thread owning the specified location.
     *
     * <p>On non-Folia platforms, this delegates to {@link #runTask}.
     *
     * @param location the location to bind to
     * @param task     the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtLocation(@NotNull UnifiedLocation location, @NotNull Task task);

    /**
     * Runs a task on the thread owning the specified location after a delay.
     *
     * @param location   the location to bind to
     * @param task       the task to run
     * @param delayTicks the delay in ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtLocationLater(@NotNull UnifiedLocation location, @NotNull Task task, long delayTicks);

    // ==================== Global Region Tasks ====================

    /**
     * Runs a task on the global region thread.
     *
     * <p>On non-Folia platforms, this delegates to {@link #runTask}.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runOnGlobal(@NotNull Task task);

    /**
     * Runs a task on the global region thread after a delay.
     *
     * @param task       the task to run
     * @param delayTicks the delay in ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runOnGlobalLater(@NotNull Task task, long delayTicks);

    /**
     * Runs a task on the global region thread on a repeating schedule.
     *
     * @param task        the task to run
     * @param delayTicks  the initial delay in ticks
     * @param periodTicks the period between executions
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runOnGlobalTimer(@NotNull Task task, long delayTicks, long periodTicks);

    // ==================== Cancellation ====================

    /**
     * Cancels a scheduled task.
     *
     * @param handle the task handle
     * @return true if the task was cancelled
     * @since 1.0.0
     */
    boolean cancel(@NotNull TaskHandle handle);

    /**
     * Cancels all tasks scheduled by this adapter.
     *
     * @since 1.0.0
     */
    void cancelAll();

    // ==================== Status ====================

    /**
     * Returns the number of pending tasks.
     *
     * @return the pending task count
     * @since 1.0.0
     */
    int getPendingTaskCount();

    /**
     * Checks if the current thread is the main server thread.
     *
     * @return true if on the main thread
     * @since 1.0.0
     */
    boolean isMainThread();

    /**
     * Checks if the current thread is the global region thread.
     *
     * <p>On non-Folia platforms, this is the same as {@link #isMainThread()}.
     *
     * @return true if on the global region thread
     * @since 1.0.0
     */
    boolean isGlobalThread();

    /**
     * Checks if this adapter supports Folia's region-aware scheduling.
     *
     * @return true if Folia scheduling is available
     * @since 1.0.0
     */
    boolean isFoliaSupported();

    /**
     * Returns the scheduler type this adapter implements.
     *
     * @return the scheduler type
     * @since 1.0.0
     */
    @NotNull
    SchedulerDetector.SchedulerType getSchedulerType();

    // ==================== Shutdown ====================

    /**
     * Shuts down the adapter and releases resources.
     *
     * <p>Cancels all pending tasks and stops accepting new ones.
     *
     * @since 1.0.0
     */
    void shutdown();

    /**
     * Checks if the adapter has been shut down.
     *
     * @return true if shutdown has been called
     * @since 1.0.0
     */
    boolean isShutdown();
}
