/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.scheduler.execution.ExecutionContext;
import sh.pcx.unified.service.Service;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Platform-aware task scheduling service with Folia region support.
 *
 * <p>This service provides a unified API for scheduling tasks across different
 * Minecraft server platforms (Paper, Spigot, Folia, Sponge). It automatically
 * detects the platform and uses the appropriate scheduler implementation.
 *
 * <h2>Platform Behavior</h2>
 * <ul>
 *   <li><b>Paper/Spigot:</b> Uses the standard Bukkit scheduler with main thread execution</li>
 *   <li><b>Folia:</b> Uses region-aware scheduling for thread-safe entity and location operations</li>
 *   <li><b>Sponge:</b> Uses the Sponge scheduler API</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple sync task
 * scheduler.runTask(() -> {
 *     player.sendMessage("Hello!");
 * });
 *
 * // Async task with delay
 * scheduler.runTaskLaterAsync(() -> {
 *     saveData();
 * }, 20L); // 1 second delay
 *
 * // Repeating task
 * TaskHandle handle = scheduler.runTaskTimer(() -> {
 *     updateScoreboard();
 * }, 0L, 20L);
 *
 * // Cancel when done
 * handle.cancel();
 *
 * // Entity-bound task (Folia-safe)
 * scheduler.runAtEntity(player, () -> {
 *     player.setHealth(20.0);
 * });
 *
 * // Location-bound task (Folia-safe)
 * scheduler.runAtLocation(location, () -> {
 *     world.setBlockData(location, blockData);
 * });
 *
 * // Use the fluent builder
 * scheduler.builder()
 *     .async()
 *     .delay(5, TimeUnit.SECONDS)
 *     .repeat(1, TimeUnit.MINUTES)
 *     .execute(() -> cleanupCache())
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe. However, the tasks themselves
 * execute on specific threads depending on the task type:
 * <ul>
 *   <li>Sync tasks: Execute on the main server thread (or region thread on Folia)</li>
 *   <li>Async tasks: Execute on a background thread pool</li>
 *   <li>Entity tasks: Execute on the entity's region thread (Folia) or main thread</li>
 *   <li>Location tasks: Execute on the location's region thread (Folia) or main thread</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Task
 * @see TaskBuilder
 * @see TaskHandle
 */
public interface SchedulerService extends Service {

    // ==================== Immediate Execution ====================

    /**
     * Runs a task synchronously on the main thread.
     *
     * <p>On Folia, this runs on the global region scheduler.
     * For entity or location-specific operations on Folia, use
     * {@link #runAtEntity} or {@link #runAtLocation} instead.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTask(@NotNull Runnable task);

    /**
     * Runs a task asynchronously on the async thread pool.
     *
     * <p>Async tasks should never access Minecraft world state directly.
     * Use async tasks for database operations, file I/O, HTTP requests, etc.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskAsync(@NotNull Runnable task);

    // ==================== Delayed Execution ====================

    /**
     * Runs a task synchronously after a delay.
     *
     * @param task       the task to run
     * @param delayTicks the delay in server ticks (20 ticks = 1 second)
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskLater(@NotNull Runnable task, long delayTicks);

    /**
     * Runs a task asynchronously after a delay.
     *
     * @param task       the task to run
     * @param delayTicks the delay in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskLaterAsync(@NotNull Runnable task, long delayTicks);

    // ==================== Repeating Execution ====================

    /**
     * Runs a task synchronously on a repeating schedule.
     *
     * @param task        the task to run
     * @param delayTicks  the initial delay in server ticks
     * @param periodTicks the period between executions in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskTimer(@NotNull Runnable task, long delayTicks, long periodTicks);

    /**
     * Runs a task asynchronously on a repeating schedule.
     *
     * @param task        the task to run
     * @param delayTicks  the initial delay in server ticks
     * @param periodTicks the period between executions in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskTimerAsync(@NotNull Runnable task, long delayTicks, long periodTicks);

    // ==================== Entity-Bound Tasks (Folia Support) ====================

    /**
     * Runs a task on the thread that owns the specified entity.
     *
     * <p>On Folia, this ensures the task runs on the correct region thread
     * for the entity. On Paper/Spigot, this runs on the main thread.
     *
     * <p>If the entity is removed before the task can run, the retired
     * callback will be executed instead (if provided via the builder).
     *
     * @param entity the entity to run the task for
     * @param task   the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtEntity(@NotNull Object entity, @NotNull Runnable task);

    /**
     * Runs a task on the thread that owns the specified entity after a delay.
     *
     * @param entity     the entity to run the task for
     * @param task       the task to run
     * @param delayTicks the delay in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtEntityLater(@NotNull Object entity, @NotNull Runnable task, long delayTicks);

    /**
     * Runs a task on the thread that owns the specified player.
     *
     * <p>This is a convenience method that extracts the underlying platform
     * player from the UnifiedPlayer and delegates to {@link #runAtEntity}.
     *
     * @param player the player to run the task for
     * @param task   the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtPlayer(@NotNull UnifiedPlayer player, @NotNull Runnable task);

    /**
     * Runs a task on the thread that owns the specified player after a delay.
     *
     * @param player     the player to run the task for
     * @param task       the task to run
     * @param delayTicks the delay in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtPlayerLater(@NotNull UnifiedPlayer player, @NotNull Runnable task, long delayTicks);

    // ==================== Location-Bound Tasks (Folia Support) ====================

    /**
     * Runs a task on the thread that owns the specified location.
     *
     * <p>On Folia, this ensures the task runs on the correct region thread
     * for the location. On Paper/Spigot, this runs on the main thread.
     *
     * @param location the location to run the task at
     * @param task     the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtLocation(@NotNull UnifiedLocation location, @NotNull Runnable task);

    /**
     * Runs a task on the thread that owns the specified location after a delay.
     *
     * @param location   the location to run the task at
     * @param task       the task to run
     * @param delayTicks the delay in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runAtLocationLater(@NotNull UnifiedLocation location, @NotNull Runnable task, long delayTicks);

    // ==================== Global Region Tasks (Folia Support) ====================

    /**
     * Runs a task on the global region thread.
     *
     * <p>On Folia, this runs on the global region scheduler which is suitable
     * for operations that don't access world state. On Paper/Spigot, this
     * runs on the main thread.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runOnGlobal(@NotNull Runnable task);

    /**
     * Runs a task on the global region thread after a delay.
     *
     * @param task       the task to run
     * @param delayTicks the delay in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runOnGlobalLater(@NotNull Runnable task, long delayTicks);

    /**
     * Runs a task on the global region thread on a repeating schedule.
     *
     * @param task        the task to run
     * @param delayTicks  the initial delay in server ticks
     * @param periodTicks the period between executions in server ticks
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runOnGlobalTimer(@NotNull Runnable task, long delayTicks, long periodTicks);

    // ==================== CompletableFuture Support ====================

    /**
     * Executes a supplier synchronously and returns the result as a CompletableFuture.
     *
     * @param <T>      the result type
     * @param supplier the supplier to execute
     * @return a future that completes with the supplier's result
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<T> supplySync(@NotNull java.util.function.Supplier<T> supplier);

    /**
     * Executes a supplier asynchronously and returns the result as a CompletableFuture.
     *
     * @param <T>      the result type
     * @param supplier the supplier to execute
     * @return a future that completes with the supplier's result
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<T> supplyAsync(@NotNull java.util.function.Supplier<T> supplier);

    /**
     * Executes a runnable synchronously and returns a CompletableFuture that completes when done.
     *
     * @param runnable the runnable to execute
     * @return a future that completes when the runnable finishes
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> runSync(@NotNull Runnable runnable);

    /**
     * Executes a runnable asynchronously and returns a CompletableFuture that completes when done.
     *
     * @param runnable the runnable to execute
     * @return a future that completes when the runnable finishes
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> runAsync(@NotNull Runnable runnable);

    // ==================== Context-Aware Execution ====================

    /**
     * Runs a task with an execution context that provides task metadata.
     *
     * @param task the task consumer that receives the execution context
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle runTaskWithContext(@NotNull Consumer<ExecutionContext> task);

    // ==================== Builder ====================

    /**
     * Creates a new task builder for fluent task configuration.
     *
     * <p>Example usage:
     * <pre>{@code
     * scheduler.builder()
     *     .async()
     *     .delay(1, TimeUnit.SECONDS)
     *     .repeat(5, TimeUnit.SECONDS)
     *     .execute(() -> saveData())
     *     .build();
     * }</pre>
     *
     * @return a new task builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder builder();

    /**
     * Creates a new task chain for sequential task execution.
     *
     * <p>Task chains allow you to execute multiple tasks in sequence,
     * switching between sync and async execution as needed:
     * <pre>{@code
     * scheduler.chain()
     *     .async(() -> loadDataFromDatabase())
     *     .sync(() -> updatePlayerInventory())
     *     .async(() -> saveToCache())
     *     .execute();
     * }</pre>
     *
     * @return a new task chain
     * @since 1.0.0
     */
    @NotNull
    TaskChain chain();

    // ==================== Utility Methods ====================

    /**
     * Cancels all tasks scheduled by this scheduler.
     *
     * <p>This should be called during plugin shutdown to clean up
     * any pending tasks.
     *
     * @since 1.0.0
     */
    void cancelAllTasks();

    /**
     * Returns whether the current thread is the main server thread.
     *
     * <p>On Folia, this returns true if the current thread is any
     * region thread. Use {@link #isGlobalThread()} to check specifically
     * for the global region thread.
     *
     * @return true if on the main/region thread
     * @since 1.0.0
     */
    boolean isMainThread();

    /**
     * Returns whether the current thread is the global region thread.
     *
     * <p>On non-Folia servers, this returns the same as {@link #isMainThread()}.
     *
     * @return true if on the global region thread
     * @since 1.0.0
     */
    boolean isGlobalThread();

    /**
     * Returns whether Folia's region-based threading is active.
     *
     * @return true if running on Folia
     * @since 1.0.0
     */
    boolean isFolia();

    /**
     * Returns the number of currently scheduled tasks.
     *
     * @return the number of pending tasks
     * @since 1.0.0
     */
    int getPendingTaskCount();
}
