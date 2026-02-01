/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler;

import sh.pcx.unified.scheduler.execution.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a scheduled task with metadata and execution state.
 *
 * <p>A Task encapsulates all information about a scheduled operation,
 * including its runnable, timing configuration, and current state.
 * Tasks are immutable once created; use {@link TaskBuilder} to create
 * new tasks with different configurations.
 *
 * <h2>Task Types</h2>
 * <ul>
 *   <li>{@link TaskType#SYNC} - Runs on the main/region thread</li>
 *   <li>{@link TaskType#ASYNC} - Runs on the async thread pool</li>
 *   <li>{@link TaskType#ENTITY} - Runs on an entity's owning thread (Folia)</li>
 *   <li>{@link TaskType#LOCATION} - Runs on a location's owning thread (Folia)</li>
 *   <li>{@link TaskType#GLOBAL} - Runs on the global region thread (Folia)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a task via the builder
 * Task task = scheduler.builder()
 *     .async()
 *     .delay(20L)
 *     .repeat(100L)
 *     .name("data-saver")
 *     .execute(() -> saveData())
 *     .buildTask();
 *
 * // Check task state
 * if (task.isRunning()) {
 *     System.out.println("Task " + task.getName() + " is running");
 * }
 *
 * // Get execution count
 * System.out.println("Executed " + task.getExecutionCount() + " times");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TaskBuilder
 * @see TaskHandle
 * @see SchedulerService
 */
public interface Task {

    /**
     * Returns the unique identifier for this task.
     *
     * @return the task's unique ID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns the optional name of this task.
     *
     * <p>Task names are useful for debugging and logging purposes.
     * If no name was provided during creation, this returns empty.
     *
     * @return an Optional containing the task name
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getName();

    /**
     * Returns the type of this task.
     *
     * @return the task type
     * @since 1.0.0
     */
    @NotNull
    TaskType getType();

    /**
     * Returns the current state of this task.
     *
     * @return the task state
     * @since 1.0.0
     */
    @NotNull
    TaskState getState();

    /**
     * Returns the runnable associated with this task.
     *
     * @return the task's runnable
     * @since 1.0.0
     */
    @NotNull
    Runnable getRunnable();

    /**
     * Returns the context-aware consumer if one was provided.
     *
     * @return an Optional containing the context consumer
     * @since 1.0.0
     */
    @NotNull
    Optional<Consumer<ExecutionContext>> getContextConsumer();

    /**
     * Returns the initial delay before first execution in ticks.
     *
     * @return the delay in ticks, or 0 for immediate execution
     * @since 1.0.0
     */
    long getDelayTicks();

    /**
     * Returns the period between executions in ticks.
     *
     * @return the period in ticks, or 0 for non-repeating tasks
     * @since 1.0.0
     */
    long getPeriodTicks();

    /**
     * Returns whether this task repeats.
     *
     * @return true if the task repeats
     * @since 1.0.0
     */
    default boolean isRepeating() {
        return getPeriodTicks() > 0;
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
     * Returns the time this task was last executed.
     *
     * @return an Optional containing the last execution time
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getLastExecutedAt();

    /**
     * Returns the scheduled time for the next execution.
     *
     * @return an Optional containing the next execution time
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getNextExecutionAt();

    /**
     * Returns the number of times this task has been executed.
     *
     * @return the execution count
     * @since 1.0.0
     */
    long getExecutionCount();

    /**
     * Returns the maximum number of executions before auto-cancellation.
     *
     * @return an Optional containing the max executions, empty for unlimited
     * @since 1.0.0
     */
    @NotNull
    Optional<Long> getMaxExecutions();

    /**
     * Returns the entity this task is bound to, if any.
     *
     * @return an Optional containing the bound entity
     * @since 1.0.0
     */
    @NotNull
    Optional<Object> getBoundEntity();

    /**
     * Returns the location this task is bound to, if any.
     *
     * @return an Optional containing the bound location
     * @since 1.0.0
     */
    @NotNull
    Optional<Object> getBoundLocation();

    /**
     * Returns the callback to run if an entity-bound task's entity is retired.
     *
     * <p>This is only applicable for entity-bound tasks on Folia.
     * The callback runs when the entity is removed before the task executes.
     *
     * @return an Optional containing the retired callback
     * @since 1.0.0
     */
    @NotNull
    Optional<Runnable> getRetiredCallback();

    /**
     * Returns whether this task is currently scheduled and waiting to run.
     *
     * @return true if the task is scheduled
     * @since 1.0.0
     */
    default boolean isScheduled() {
        return getState() == TaskState.SCHEDULED;
    }

    /**
     * Returns whether this task is currently executing.
     *
     * @return true if the task is running
     * @since 1.0.0
     */
    default boolean isRunning() {
        return getState() == TaskState.RUNNING;
    }

    /**
     * Returns whether this task has been cancelled.
     *
     * @return true if the task is cancelled
     * @since 1.0.0
     */
    default boolean isCancelled() {
        return getState() == TaskState.CANCELLED;
    }

    /**
     * Returns whether this task has completed execution.
     *
     * @return true if the task is completed
     * @since 1.0.0
     */
    default boolean isCompleted() {
        return getState() == TaskState.COMPLETED;
    }

    /**
     * Returns the total time spent executing this task.
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
     * Returns the exception thrown during the last execution, if any.
     *
     * @return an Optional containing the last exception
     * @since 1.0.0
     */
    @NotNull
    Optional<Throwable> getLastException();

    /**
     * Enumeration of task types based on execution context.
     *
     * @since 1.0.0
     */
    enum TaskType {
        /**
         * Synchronous task running on the main thread.
         */
        SYNC,

        /**
         * Asynchronous task running on the thread pool.
         */
        ASYNC,

        /**
         * Task bound to an entity's region thread (Folia).
         */
        ENTITY,

        /**
         * Task bound to a location's region thread (Folia).
         */
        LOCATION,

        /**
         * Task running on the global region thread (Folia).
         */
        GLOBAL
    }

    /**
     * Enumeration of task states.
     *
     * @since 1.0.0
     */
    enum TaskState {
        /**
         * Task has been created but not yet scheduled.
         */
        PENDING,

        /**
         * Task is scheduled and waiting to run.
         */
        SCHEDULED,

        /**
         * Task is currently executing.
         */
        RUNNING,

        /**
         * Task has been cancelled.
         */
        CANCELLED,

        /**
         * Task has completed (non-repeating tasks only).
         */
        COMPLETED,

        /**
         * Task failed with an unhandled exception.
         */
        FAILED,

        /**
         * Entity-bound task's entity was retired before execution (Folia).
         */
        RETIRED
    }
}
