/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.execution;

import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides context and control for task execution.
 *
 * <p>ExecutionContext is passed to tasks that use the context-aware
 * execution pattern. It provides access to execution metadata and
 * allows the task to control its own lifecycle.
 *
 * <h2>Available Information</h2>
 * <ul>
 *   <li>Current execution count</li>
 *   <li>Time since task creation</li>
 *   <li>Time since last execution</li>
 *   <li>Task metadata (name, ID, type)</li>
 *   <li>Remaining executions for limited tasks</li>
 * </ul>
 *
 * <h2>Control Operations</h2>
 * <ul>
 *   <li>Cancel the task</li>
 *   <li>Skip the next execution</li>
 *   <li>Store data between executions</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * scheduler.builder()
 *     .async()
 *     .repeat(1, TimeUnit.SECONDS)
 *     .executeWithContext(ctx -> {
 *         log.info("Execution #{}", ctx.getExecutionCount());
 *
 *         if (ctx.getExecutionCount() >= 10) {
 *             log.info("Reached 10 executions, cancelling");
 *             ctx.cancel();
 *             return;
 *         }
 *
 *         if (someCondition) {
 *             log.info("Skipping this execution");
 *             ctx.skipNext();
 *         }
 *
 *         // Access stored data
 *         int counter = ctx.getData("counter", Integer.class).orElse(0);
 *         ctx.setData("counter", counter + 1);
 *     })
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.scheduler.TaskBuilder#executeWithContext
 */
public interface ExecutionContext {

    /**
     * Returns the unique identifier of the task.
     *
     * @return the task ID
     * @since 1.0.0
     */
    @NotNull
    UUID getTaskId();

    /**
     * Returns the task name if one was provided.
     *
     * @return an Optional containing the task name
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getTaskName();

    /**
     * Returns the type of the task.
     *
     * @return the task type
     * @since 1.0.0
     */
    @NotNull
    Task.TaskType getTaskType();

    /**
     * Returns the current execution count (1-indexed).
     *
     * <p>This is the number of times the task has been executed,
     * including the current execution.
     *
     * @return the execution count
     * @since 1.0.0
     */
    long getExecutionCount();

    /**
     * Returns whether this is the first execution.
     *
     * @return true if this is the first execution
     * @since 1.0.0
     */
    default boolean isFirstExecution() {
        return getExecutionCount() == 1;
    }

    /**
     * Returns the maximum number of executions, if limited.
     *
     * @return an Optional containing the max executions
     * @since 1.0.0
     */
    @NotNull
    Optional<Long> getMaxExecutions();

    /**
     * Returns the remaining number of executions.
     *
     * @return an Optional containing remaining executions, empty if unlimited
     * @since 1.0.0
     */
    @NotNull
    default Optional<Long> getRemainingExecutions() {
        return getMaxExecutions().map(max -> Math.max(0, max - getExecutionCount()));
    }

    /**
     * Returns whether this is the last execution for limited tasks.
     *
     * @return true if this is the final execution
     * @since 1.0.0
     */
    default boolean isLastExecution() {
        return getRemainingExecutions().map(r -> r == 0).orElse(false);
    }

    /**
     * Returns the time this task was created.
     *
     * @return the creation timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getCreatedAt();

    /**
     * Returns the duration since the task was created.
     *
     * @return the uptime duration
     * @since 1.0.0
     */
    @NotNull
    default Duration getUptime() {
        return Duration.between(getCreatedAt(), Instant.now());
    }

    /**
     * Returns the time of the previous execution.
     *
     * @return an Optional containing the previous execution time
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getLastExecutedAt();

    /**
     * Returns the duration since the last execution.
     *
     * @return an Optional containing the duration, empty for first execution
     * @since 1.0.0
     */
    @NotNull
    default Optional<Duration> getTimeSinceLastExecution() {
        return getLastExecutedAt().map(last -> Duration.between(last, Instant.now()));
    }

    /**
     * Returns the current execution start time.
     *
     * @return the current execution start timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getCurrentExecutionStart();

    /**
     * Returns the delay in ticks before the first execution.
     *
     * @return the delay in ticks
     * @since 1.0.0
     */
    long getDelayTicks();

    /**
     * Returns the period between executions in ticks.
     *
     * @return the period in ticks, 0 for non-repeating tasks
     * @since 1.0.0
     */
    long getPeriodTicks();

    /**
     * Returns whether the task is repeating.
     *
     * @return true if the task repeats
     * @since 1.0.0
     */
    default boolean isRepeating() {
        return getPeriodTicks() > 0;
    }

    /**
     * Returns the total execution time across all previous runs.
     *
     * @return the total execution duration
     * @since 1.0.0
     */
    @NotNull
    Duration getTotalExecutionTime();

    /**
     * Returns the average execution time per run.
     *
     * @return the average execution duration
     * @since 1.0.0
     */
    @NotNull
    Duration getAverageExecutionTime();

    /**
     * Returns the handle for this task.
     *
     * @return the task handle
     * @since 1.0.0
     */
    @NotNull
    TaskHandle getHandle();

    // ==================== Control Methods ====================

    /**
     * Cancels the task after this execution completes.
     *
     * <p>The current execution will complete normally, but no further
     * executions will occur.
     *
     * @since 1.0.0
     */
    void cancel();

    /**
     * Checks if the task has been marked for cancellation.
     *
     * @return true if cancel() has been called
     * @since 1.0.0
     */
    boolean isCancelled();

    /**
     * Skips the next scheduled execution.
     *
     * <p>Only applicable to repeating tasks. The execution after the
     * skipped one will run normally.
     *
     * @since 1.0.0
     */
    void skipNext();

    /**
     * Checks if the next execution will be skipped.
     *
     * @return true if skipNext() has been called
     * @since 1.0.0
     */
    boolean willSkipNext();

    // ==================== Data Storage ====================

    /**
     * Stores data that persists between executions.
     *
     * @param key   the data key
     * @param value the value to store
     * @since 1.0.0
     */
    void setData(@NotNull String key, @Nullable Object value);

    /**
     * Retrieves stored data.
     *
     * @param <T>  the expected type
     * @param key  the data key
     * @param type the expected class type
     * @return an Optional containing the stored value
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getData(@NotNull String key, @NotNull Class<T> type);

    /**
     * Retrieves stored data with a default value.
     *
     * @param <T>          the expected type
     * @param key          the data key
     * @param type         the expected class type
     * @param defaultValue the default value if not found
     * @return the stored value or default
     * @since 1.0.0
     */
    @NotNull
    default <T> T getData(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        return getData(key, type).orElse(defaultValue);
    }

    /**
     * Checks if data exists for the given key.
     *
     * @param key the data key
     * @return true if data exists
     * @since 1.0.0
     */
    boolean hasData(@NotNull String key);

    /**
     * Removes stored data.
     *
     * @param key the data key
     * @since 1.0.0
     */
    void removeData(@NotNull String key);

    /**
     * Clears all stored data.
     *
     * @since 1.0.0
     */
    void clearData();
}
