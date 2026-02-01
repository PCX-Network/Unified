/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.execution;

import sh.pcx.unified.scheduler.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A priority queue for pending tasks ordered by scheduled execution time.
 *
 * <p>TaskQueue manages the ordering and retrieval of scheduled tasks based
 * on when they should next execute. Tasks are ordered by their next execution
 * time, with the earliest first.
 *
 * <h2>Queue Operations</h2>
 * <ul>
 *   <li>{@link #offer}: Add a task to the queue</li>
 *   <li>{@link #poll}: Remove and return the next ready task</li>
 *   <li>{@link #peek}: View the next task without removing</li>
 *   <li>{@link #remove}: Remove a specific task</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as tasks may be added from any
 * thread while the scheduler thread polls for ready tasks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * TaskQueue queue = new PriorityTaskQueue();
 *
 * // Add tasks
 * queue.offer(task1, Instant.now().plusMillis(1000));
 * queue.offer(task2, Instant.now().plusMillis(500));
 *
 * // Poll returns task2 first (earlier execution time)
 * Task next = queue.poll(Instant.now().plusMillis(600));
 *
 * // Check next ready task
 * Optional<Instant> nextTime = queue.getNextExecutionTime();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TaskExecutor
 */
public interface TaskQueue {

    /**
     * Adds a task to the queue with the specified execution time.
     *
     * @param task        the task to add
     * @param executeAt   when the task should execute
     * @return true if the task was added successfully
     * @since 1.0.0
     */
    boolean offer(@NotNull Task task, @NotNull Instant executeAt);

    /**
     * Retrieves and removes the next task if its execution time has passed.
     *
     * @param currentTime the current time to check against
     * @return the next ready task, or null if no tasks are ready
     * @since 1.0.0
     */
    @Nullable
    Task poll(@NotNull Instant currentTime);

    /**
     * Retrieves the next task if its execution time has passed without removing it.
     *
     * @param currentTime the current time to check against
     * @return the next ready task, or null if no tasks are ready
     * @since 1.0.0
     */
    @Nullable
    Task peek(@NotNull Instant currentTime);

    /**
     * Retrieves the next task regardless of execution time without removing it.
     *
     * @return an Optional containing the next task
     * @since 1.0.0
     */
    @NotNull
    Optional<Task> peekNext();

    /**
     * Removes a specific task from the queue.
     *
     * @param taskId the ID of the task to remove
     * @return true if the task was found and removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UUID taskId);

    /**
     * Removes a specific task from the queue.
     *
     * @param task the task to remove
     * @return true if the task was found and removed
     * @since 1.0.0
     */
    default boolean remove(@NotNull Task task) {
        return remove(task.getId());
    }

    /**
     * Removes all tasks matching the given predicate.
     *
     * @param filter the predicate to match tasks
     * @return the number of tasks removed
     * @since 1.0.0
     */
    int removeIf(@NotNull Predicate<Task> filter);

    /**
     * Checks if the queue contains a task with the given ID.
     *
     * @param taskId the task ID to check
     * @return true if the task is in the queue
     * @since 1.0.0
     */
    boolean contains(@NotNull UUID taskId);

    /**
     * Returns the task with the given ID if present.
     *
     * @param taskId the task ID to find
     * @return an Optional containing the task
     * @since 1.0.0
     */
    @NotNull
    Optional<Task> get(@NotNull UUID taskId);

    /**
     * Returns all tasks in the queue.
     *
     * @return a collection of all queued tasks
     * @since 1.0.0
     */
    @NotNull
    Collection<Task> getAll();

    /**
     * Returns all tasks matching the given predicate.
     *
     * @param filter the predicate to match tasks
     * @return a collection of matching tasks
     * @since 1.0.0
     */
    @NotNull
    Collection<Task> getAll(@NotNull Predicate<Task> filter);

    /**
     * Returns the scheduled execution time of the next task.
     *
     * @return an Optional containing the next execution time
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getNextExecutionTime();

    /**
     * Returns the number of tasks in the queue.
     *
     * @return the queue size
     * @since 1.0.0
     */
    int size();

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue has no tasks
     * @since 1.0.0
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Removes all tasks from the queue.
     *
     * @since 1.0.0
     */
    void clear();

    /**
     * Updates the scheduled execution time for a task.
     *
     * @param taskId      the task ID
     * @param newExecuteAt the new execution time
     * @return true if the task was found and updated
     * @since 1.0.0
     */
    boolean reschedule(@NotNull UUID taskId, @NotNull Instant newExecuteAt);

    /**
     * Returns the number of tasks ready to execute.
     *
     * @param currentTime the current time to check against
     * @return the count of ready tasks
     * @since 1.0.0
     */
    int countReady(@NotNull Instant currentTime);

    /**
     * Drains all ready tasks from the queue.
     *
     * @param currentTime the current time to check against
     * @return a collection of ready tasks (removed from queue)
     * @since 1.0.0
     */
    @NotNull
    Collection<Task> drainReady(@NotNull Instant currentTime);

    /**
     * Drains up to the specified number of ready tasks.
     *
     * @param currentTime the current time to check against
     * @param maxTasks    the maximum number of tasks to drain
     * @return a collection of ready tasks (removed from queue)
     * @since 1.0.0
     */
    @NotNull
    Collection<Task> drainReady(@NotNull Instant currentTime, int maxTasks);
}
