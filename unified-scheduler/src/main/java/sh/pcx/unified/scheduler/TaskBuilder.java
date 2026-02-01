/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.scheduler.execution.ExecutionContext;
import sh.pcx.unified.scheduler.util.Ticks;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Fluent builder for creating and scheduling tasks.
 *
 * <p>TaskBuilder provides a flexible, type-safe way to configure tasks
 * with various execution modes, timing, and callbacks. All builder methods
 * return the builder instance for method chaining.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple async delayed task
 * scheduler.builder()
 *     .async()
 *     .delay(5, TimeUnit.SECONDS)
 *     .execute(() -> saveData())
 *     .build();
 *
 * // Repeating sync task with max executions
 * scheduler.builder()
 *     .sync()
 *     .delay(1, TimeUnit.SECONDS)
 *     .repeat(30, TimeUnit.SECONDS)
 *     .maxExecutions(10)
 *     .name("scoreboard-updater")
 *     .execute(() -> updateScoreboard())
 *     .build();
 *
 * // Entity-bound task with retired callback
 * scheduler.builder()
 *     .atEntity(player)
 *     .delay(20) // ticks
 *     .onRetired(() -> log.warn("Player left before task ran"))
 *     .execute(() -> player.sendMessage("Welcome!"))
 *     .build();
 *
 * // Location-bound task
 * scheduler.builder()
 *     .atLocation(spawnLocation)
 *     .repeat(100) // ticks
 *     .execute(() -> spawnParticles())
 *     .build();
 *
 * // Task with context
 * scheduler.builder()
 *     .async()
 *     .repeat(1, TimeUnit.MINUTES)
 *     .executeWithContext(ctx -> {
 *         log.info("Execution #" + ctx.getExecutionCount());
 *         if (ctx.getExecutionCount() >= 60) {
 *             ctx.cancel();
 *         }
 *     })
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>TaskBuilder instances are not thread-safe. Each builder should only
 * be used from a single thread. The resulting tasks and handles are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchedulerService#builder()
 * @see Task
 * @see TaskHandle
 */
public interface TaskBuilder {

    // ==================== Execution Mode ====================

    /**
     * Configures the task to run synchronously on the main thread.
     *
     * <p>On Folia, this uses the global region scheduler.
     * For entity/location-specific operations, use {@link #atEntity} or {@link #atLocation}.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder sync();

    /**
     * Configures the task to run asynchronously on the thread pool.
     *
     * <p>Async tasks should not access Minecraft world state directly.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder async();

    /**
     * Configures the task to run on the global region scheduler.
     *
     * <p>On non-Folia servers, this behaves the same as {@link #sync()}.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder global();

    /**
     * Configures the task to run on the thread owning the specified entity.
     *
     * <p>On Folia, this ensures thread-safe access to the entity.
     * On Paper/Spigot, this runs on the main thread.
     *
     * @param entity the entity to bind the task to
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder atEntity(@NotNull Object entity);

    /**
     * Configures the task to run on the thread owning the specified player.
     *
     * <p>This is a convenience method that extracts the platform entity
     * from the UnifiedPlayer.
     *
     * @param player the player to bind the task to
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder atPlayer(@NotNull UnifiedPlayer player);

    /**
     * Configures the task to run on the thread owning the specified location.
     *
     * <p>On Folia, this ensures thread-safe access to blocks at the location.
     * On Paper/Spigot, this runs on the main thread.
     *
     * @param location the location to bind the task to
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder atLocation(@NotNull UnifiedLocation location);

    // ==================== Timing ====================

    /**
     * Sets the initial delay before the first execution in ticks.
     *
     * @param ticks the delay in server ticks (20 ticks = 1 second)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder delay(long ticks);

    /**
     * Sets the initial delay before the first execution.
     *
     * @param duration the delay duration
     * @param unit     the time unit
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder delay(long duration, @NotNull TimeUnit unit);

    /**
     * Sets the initial delay before the first execution.
     *
     * @param duration the delay duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder delay(@NotNull Duration duration);

    /**
     * Sets the period between repeated executions in ticks.
     *
     * <p>Calling this method makes the task repeat until cancelled.
     *
     * @param ticks the period in server ticks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder repeat(long ticks);

    /**
     * Sets the period between repeated executions.
     *
     * @param duration the period duration
     * @param unit     the time unit
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder repeat(long duration, @NotNull TimeUnit unit);

    /**
     * Sets the period between repeated executions.
     *
     * @param duration the period duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder repeat(@NotNull Duration duration);

    /**
     * Sets the maximum number of times this task will execute.
     *
     * <p>After reaching the maximum, the task is automatically cancelled.
     * Only applicable to repeating tasks.
     *
     * @param count the maximum number of executions
     * @return this builder
     * @throws IllegalArgumentException if count is less than 1
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder maxExecutions(long count);

    // ==================== Naming and Metadata ====================

    /**
     * Sets a name for this task (useful for debugging).
     *
     * @param name the task name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder name(@NotNull String name);

    // ==================== Callbacks ====================

    /**
     * Sets the callback to run if an entity-bound task's entity is retired.
     *
     * <p>On Folia, entities can be "retired" (removed) before a scheduled
     * task runs. This callback allows handling that case gracefully.
     *
     * @param callback the callback to run when the entity is retired
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder onRetired(@NotNull Runnable callback);

    /**
     * Sets an exception handler for task execution errors.
     *
     * <p>If not set, exceptions are logged but don't cancel the task.
     *
     * @param handler the exception handler
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder onException(@NotNull Consumer<Throwable> handler);

    /**
     * Sets a callback to run when the task completes (non-repeating) or is cancelled.
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TaskBuilder onComplete(@NotNull Runnable callback);

    // ==================== Execution ====================

    /**
     * Sets the task's runnable and schedules it for execution.
     *
     * <p>This is a terminal operation that schedules the task and returns
     * a handle for cancellation and status checking.
     *
     * @param runnable the task to execute
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle execute(@NotNull Runnable runnable);

    /**
     * Sets the task's context-aware consumer and schedules it for execution.
     *
     * <p>The consumer receives an {@link ExecutionContext} with metadata
     * about the current execution, including execution count and cancellation.
     *
     * @param consumer the context-aware task consumer
     * @return a handle to the scheduled task
     * @since 1.0.0
     */
    @NotNull
    TaskHandle executeWithContext(@NotNull Consumer<ExecutionContext> consumer);

    /**
     * Builds the task without scheduling it.
     *
     * <p>The returned Task object can be inspected or scheduled manually.
     * Most users should use {@link #execute} instead.
     *
     * @param runnable the task to execute
     * @return the built task (not yet scheduled)
     * @since 1.0.0
     */
    @NotNull
    Task buildTask(@NotNull Runnable runnable);

    /**
     * Builds a context-aware task without scheduling it.
     *
     * @param consumer the context-aware task consumer
     * @return the built task (not yet scheduled)
     * @since 1.0.0
     */
    @NotNull
    Task buildTaskWithContext(@NotNull Consumer<ExecutionContext> consumer);
}
