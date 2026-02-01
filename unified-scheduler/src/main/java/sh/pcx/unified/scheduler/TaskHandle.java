/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handle for managing a scheduled task's lifecycle.
 *
 * <p>TaskHandle provides methods to cancel, query status, and wait for
 * task completion. It is returned by all scheduling methods in
 * {@link SchedulerService} and {@link TaskBuilder}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Schedule a repeating task
 * TaskHandle handle = scheduler.runTaskTimer(() -> {
 *     updateScoreboard();
 * }, 0L, 20L);
 *
 * // Check if still running
 * if (handle.isActive()) {
 *     System.out.println("Task is running");
 * }
 *
 * // Cancel when player leaves
 * playerQuitEvent.listen(event -> {
 *     handle.cancel();
 * });
 *
 * // Wait for a one-time task to complete
 * TaskHandle oneTime = scheduler.runTaskLater(() -> {
 *     processData();
 * }, 100L);
 *
 * oneTime.toFuture().thenRun(() -> {
 *     System.out.println("Task completed!");
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in TaskHandle are thread-safe and can be called
 * from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchedulerService
 * @see Task
 */
public interface TaskHandle {

    /**
     * Returns the unique identifier for the associated task.
     *
     * @return the task's unique ID
     * @since 1.0.0
     */
    @NotNull
    UUID getTaskId();

    /**
     * Returns the underlying Task object.
     *
     * @return the task
     * @since 1.0.0
     */
    @NotNull
    Task getTask();

    /**
     * Cancels the task.
     *
     * <p>If the task is currently executing, it will complete its current
     * execution but won't run again. If the task hasn't started yet, it
     * will be removed from the schedule.
     *
     * <p>Calling cancel on an already cancelled or completed task has no effect.
     *
     * @return true if the task was cancelled, false if already cancelled/completed
     * @since 1.0.0
     */
    boolean cancel();

    /**
     * Returns whether the task is currently active (scheduled or running).
     *
     * @return true if the task is active
     * @since 1.0.0
     */
    boolean isActive();

    /**
     * Returns whether the task has been cancelled.
     *
     * @return true if the task is cancelled
     * @since 1.0.0
     */
    boolean isCancelled();

    /**
     * Returns whether the task has completed.
     *
     * <p>For repeating tasks, this is only true if cancelled or max executions reached.
     * For one-time tasks, this is true after execution completes.
     *
     * @return true if the task is done
     * @since 1.0.0
     */
    boolean isDone();

    /**
     * Returns the current state of the task.
     *
     * @return the task state
     * @since 1.0.0
     */
    @NotNull
    Task.TaskState getState();

    /**
     * Returns the type of the task.
     *
     * @return the task type
     * @since 1.0.0
     */
    @NotNull
    Task.TaskType getType();

    /**
     * Returns the number of times this task has executed.
     *
     * @return the execution count
     * @since 1.0.0
     */
    long getExecutionCount();

    /**
     * Returns the time when this task was scheduled.
     *
     * @return the scheduling timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getScheduledAt();

    /**
     * Returns the time when this task last executed.
     *
     * @return an Optional containing the last execution time
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getLastExecutedAt();

    /**
     * Returns the next scheduled execution time.
     *
     * @return an Optional containing the next execution time
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getNextExecutionAt();

    /**
     * Returns the time remaining until the next execution.
     *
     * @return an Optional containing the remaining duration
     * @since 1.0.0
     */
    @NotNull
    default Optional<Duration> getTimeUntilNextExecution() {
        return getNextExecutionAt().map(next -> {
            Duration remaining = Duration.between(Instant.now(), next);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        });
    }

    /**
     * Returns the total execution time across all runs.
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
     * Converts this handle to a CompletableFuture.
     *
     * <p>The future completes when:
     * <ul>
     *   <li>One-time tasks: After execution completes</li>
     *   <li>Repeating tasks: After cancellation or max executions reached</li>
     * </ul>
     *
     * <p>The future completes exceptionally if the task throws an unhandled exception.
     *
     * @return a future that completes when the task is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> toFuture();

    /**
     * Waits for the task to complete, blocking the current thread.
     *
     * <p>For repeating tasks, this blocks until cancellation.
     * Use with caution on the main thread.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     * @since 1.0.0
     */
    void await() throws InterruptedException;

    /**
     * Waits for the task to complete with a timeout.
     *
     * @param timeout the maximum time to wait
     * @return true if the task completed, false if timeout elapsed
     * @throws InterruptedException if the thread is interrupted while waiting
     * @since 1.0.0
     */
    boolean await(@NotNull Duration timeout) throws InterruptedException;

    /**
     * Returns the optional name of this task.
     *
     * @return an Optional containing the task name
     * @since 1.0.0
     */
    @NotNull
    default Optional<String> getName() {
        return getTask().getName();
    }

    /**
     * Returns whether this is a repeating task.
     *
     * @return true if the task repeats
     * @since 1.0.0
     */
    default boolean isRepeating() {
        return getTask().isRepeating();
    }
}
