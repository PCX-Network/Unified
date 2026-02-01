/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.execution;

import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Executes scheduled tasks with proper error handling and lifecycle management.
 *
 * <p>TaskExecutor is responsible for:
 * <ul>
 *   <li>Running tasks on the appropriate thread/executor</li>
 *   <li>Managing task lifecycle transitions</li>
 *   <li>Handling exceptions during execution</li>
 *   <li>Recording execution metrics</li>
 *   <li>Invoking callbacks on completion</li>
 * </ul>
 *
 * <h2>Execution Flow</h2>
 * <ol>
 *   <li>Pre-execution: Validate task state, prepare context</li>
 *   <li>Execution: Run the task's runnable or context consumer</li>
 *   <li>Post-execution: Update metrics, schedule next run, invoke callbacks</li>
 *   <li>Error handling: Invoke exception handlers or log errors</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>TaskExecutor implementations must be thread-safe, as they may be
 * invoked from multiple threads simultaneously.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TaskQueue
 * @see ExecutionContext
 */
public interface TaskExecutor {

    /**
     * Executes a task immediately.
     *
     * <p>The task runs on the appropriate thread based on its type:
     * <ul>
     *   <li>SYNC: Main thread or region thread (Folia)</li>
     *   <li>ASYNC: Background thread pool</li>
     *   <li>ENTITY: Entity's owning thread (Folia) or main thread</li>
     *   <li>LOCATION: Location's owning thread (Folia) or main thread</li>
     *   <li>GLOBAL: Global region thread (Folia) or main thread</li>
     * </ul>
     *
     * @param task the task to execute
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> execute(@NotNull Task task);

    /**
     * Executes a task and returns a handle for monitoring.
     *
     * @param task the task to execute
     * @return the task handle
     * @since 1.0.0
     */
    @NotNull
    TaskHandle executeWithHandle(@NotNull Task task);

    /**
     * Schedules a task for execution after the specified delay.
     *
     * @param task       the task to schedule
     * @param delayTicks the delay in server ticks
     * @return the task handle
     * @since 1.0.0
     */
    @NotNull
    TaskHandle schedule(@NotNull Task task, long delayTicks);

    /**
     * Schedules a task for repeated execution.
     *
     * @param task        the task to schedule
     * @param delayTicks  the initial delay in ticks
     * @param periodTicks the period between executions
     * @return the task handle
     * @since 1.0.0
     */
    @NotNull
    TaskHandle scheduleRepeating(@NotNull Task task, long delayTicks, long periodTicks);

    /**
     * Cancels a scheduled task.
     *
     * @param handle the task handle
     * @return true if the task was cancelled
     * @since 1.0.0
     */
    boolean cancel(@NotNull TaskHandle handle);

    /**
     * Cancels all tasks.
     *
     * @since 1.0.0
     */
    void cancelAll();

    /**
     * Returns the number of currently scheduled tasks.
     *
     * @return the pending task count
     * @since 1.0.0
     */
    int getPendingTaskCount();

    /**
     * Returns the number of currently executing tasks.
     *
     * @return the active task count
     * @since 1.0.0
     */
    int getActiveTaskCount();

    /**
     * Returns the total number of tasks executed.
     *
     * @return the total execution count
     * @since 1.0.0
     */
    long getTotalExecutionCount();

    /**
     * Returns the average task execution time.
     *
     * @return the average execution duration
     * @since 1.0.0
     */
    @NotNull
    Duration getAverageExecutionTime();

    /**
     * Sets the global exception handler for unhandled task exceptions.
     *
     * <p>This handler is called when a task throws an exception and the
     * task doesn't have its own exception handler, or when the task's
     * exception handler also throws an exception.
     *
     * @param handler the exception handler
     * @since 1.0.0
     */
    void setGlobalExceptionHandler(@NotNull Consumer<TaskExecutionException> handler);

    /**
     * Shuts down the executor gracefully.
     *
     * <p>Waits for currently executing tasks to complete, then cancels
     * all pending tasks.
     *
     * @param timeout the maximum time to wait for running tasks
     * @return true if shutdown completed within the timeout
     * @since 1.0.0
     */
    boolean shutdown(@NotNull Duration timeout);

    /**
     * Forces immediate shutdown without waiting for tasks.
     *
     * @since 1.0.0
     */
    void shutdownNow();

    /**
     * Checks if the executor has been shut down.
     *
     * @return true if shutdown has been initiated
     * @since 1.0.0
     */
    boolean isShutdown();

    /**
     * Exception wrapper for task execution failures.
     *
     * @since 1.0.0
     */
    class TaskExecutionException extends RuntimeException {

        private final Task task;

        /**
         * Creates a new task execution exception.
         *
         * @param task  the task that failed
         * @param cause the underlying exception
         */
        public TaskExecutionException(@NotNull Task task, @NotNull Throwable cause) {
            super("Task execution failed: " + task.getName().orElse(task.getId().toString()), cause);
            this.task = task;
        }

        /**
         * Returns the task that failed.
         *
         * @return the failed task
         */
        @NotNull
        public Task getTask() {
            return task;
        }
    }
}
