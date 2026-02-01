/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.types;

import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.execution.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A one-time task that executes after a specified delay.
 *
 * <p>DelayedTask is a specialized task type for operations that need
 * to run once after a certain amount of time has passed. Unlike
 * {@link RepeatingTask}, it completes after a single execution.
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Countdown timers that trigger an action</li>
 *   <li>Delayed welcome messages after player join</li>
 *   <li>Cleanup operations after a grace period</li>
 *   <li>Debouncing rapid events</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Send welcome message 3 seconds after join
 * DelayedTask welcome = new DelayedTask(
 *     () -> player.sendMessage("Welcome!"),
 *     60,  // 3 seconds
 *     false // sync
 * );
 *
 * // Async database cleanup after 5 minutes
 * DelayedTask cleanup = new DelayedTask(
 *     () -> database.cleanupExpiredSessions(),
 *     20 * 60 * 5,  // 5 minutes
 *     true // async
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. State can be queried from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RepeatingTask
 * @see SyncTask
 * @see AsyncTask
 */
public class DelayedTask implements Task {

    private final UUID id;
    private final String name;
    private final Runnable runnable;
    private final Consumer<ExecutionContext> contextConsumer;
    private final long delayTicks;
    private final boolean async;
    private final Consumer<Throwable> exceptionHandler;
    private final Runnable completionCallback;
    private final Instant createdAt;

    private final AtomicReference<TaskState> state;
    private final AtomicReference<Instant> executedAt;
    private final AtomicReference<Instant> scheduledFor;
    private final AtomicReference<Duration> executionDuration;
    private final AtomicReference<Throwable> exception;

    /**
     * Creates a new delayed task.
     *
     * @param runnable   the task to execute
     * @param delayTicks the delay before execution in ticks
     * @param async      whether to run asynchronously
     */
    public DelayedTask(@NotNull Runnable runnable, long delayTicks, boolean async) {
        this(null, runnable, null, delayTicks, async, null, null);
    }

    /**
     * Creates a new delayed task with full configuration.
     *
     * @param name               optional task name
     * @param runnable           the task to execute (or null if using contextConsumer)
     * @param contextConsumer    context-aware consumer (or null if using runnable)
     * @param delayTicks         the delay before execution in ticks
     * @param async              whether to run asynchronously
     * @param exceptionHandler   exception handler
     * @param completionCallback completion callback
     */
    public DelayedTask(
            @Nullable String name,
            @Nullable Runnable runnable,
            @Nullable Consumer<ExecutionContext> contextConsumer,
            long delayTicks,
            boolean async,
            @Nullable Consumer<Throwable> exceptionHandler,
            @Nullable Runnable completionCallback
    ) {
        if (runnable == null && contextConsumer == null) {
            throw new IllegalArgumentException("Either runnable or contextConsumer must be provided");
        }

        this.id = UUID.randomUUID();
        this.name = name;
        this.runnable = runnable;
        this.contextConsumer = contextConsumer;
        this.delayTicks = Math.max(0, delayTicks);
        this.async = async;
        this.exceptionHandler = exceptionHandler;
        this.completionCallback = completionCallback;
        this.createdAt = Instant.now();

        this.state = new AtomicReference<>(TaskState.PENDING);
        this.executedAt = new AtomicReference<>(null);
        this.scheduledFor = new AtomicReference<>(null);
        this.executionDuration = new AtomicReference<>(Duration.ZERO);
        this.exception = new AtomicReference<>(null);
    }

    @Override
    @NotNull
    public UUID getId() {
        return id;
    }

    @Override
    @NotNull
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    @NotNull
    public TaskType getType() {
        return async ? TaskType.ASYNC : TaskType.SYNC;
    }

    /**
     * Returns whether this task runs asynchronously.
     *
     * @return true if async
     */
    public boolean isAsync() {
        return async;
    }

    @Override
    @NotNull
    public TaskState getState() {
        return state.get();
    }

    /**
     * Updates the task state.
     *
     * @param newState the new state
     */
    public void setState(@NotNull TaskState newState) {
        this.state.set(newState);
    }

    /**
     * Atomically updates the state if it matches the expected value.
     *
     * @param expected the expected current state
     * @param newState the new state
     * @return true if the update was successful
     */
    public boolean compareAndSetState(@NotNull TaskState expected, @NotNull TaskState newState) {
        return this.state.compareAndSet(expected, newState);
    }

    @Override
    @NotNull
    public Runnable getRunnable() {
        return runnable != null ? runnable : () -> {};
    }

    @Override
    @NotNull
    public Optional<Consumer<ExecutionContext>> getContextConsumer() {
        return Optional.ofNullable(contextConsumer);
    }

    /**
     * Returns the exception handler if set.
     *
     * @return an Optional containing the exception handler
     */
    @NotNull
    public Optional<Consumer<Throwable>> getExceptionHandler() {
        return Optional.ofNullable(exceptionHandler);
    }

    /**
     * Returns the completion callback if set.
     *
     * @return an Optional containing the completion callback
     */
    @NotNull
    public Optional<Runnable> getCompletionCallback() {
        return Optional.ofNullable(completionCallback);
    }

    @Override
    public long getDelayTicks() {
        return delayTicks;
    }

    @Override
    public long getPeriodTicks() {
        return 0; // Never repeats
    }

    @Override
    public boolean isRepeating() {
        return false;
    }

    @Override
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    @NotNull
    public Optional<Instant> getLastExecutedAt() {
        return Optional.ofNullable(executedAt.get());
    }

    /**
     * Records that the task has executed.
     *
     * @param timestamp the execution timestamp
     * @param duration  the execution duration
     */
    public void recordExecution(@NotNull Instant timestamp, @NotNull Duration duration) {
        this.executedAt.set(timestamp);
        this.executionDuration.set(duration);
    }

    @Override
    @NotNull
    public Optional<Instant> getNextExecutionAt() {
        if (state.get() == TaskState.COMPLETED || state.get() == TaskState.CANCELLED) {
            return Optional.empty();
        }
        return Optional.ofNullable(scheduledFor.get());
    }

    /**
     * Sets the scheduled execution time.
     *
     * @param scheduledTime the scheduled time
     */
    public void setScheduledFor(@Nullable Instant scheduledTime) {
        this.scheduledFor.set(scheduledTime);
    }

    @Override
    public long getExecutionCount() {
        return executedAt.get() != null ? 1 : 0;
    }

    @Override
    @NotNull
    public Optional<Long> getMaxExecutions() {
        return Optional.of(1L);
    }

    @Override
    @NotNull
    public Optional<Object> getBoundEntity() {
        return Optional.empty();
    }

    @Override
    @NotNull
    public Optional<Object> getBoundLocation() {
        return Optional.empty();
    }

    @Override
    @NotNull
    public Optional<Runnable> getRetiredCallback() {
        return Optional.empty();
    }

    @Override
    @NotNull
    public Duration getTotalExecutionTime() {
        return executionDuration.get();
    }

    @Override
    @NotNull
    public Duration getAverageExecutionTime() {
        return executionDuration.get();
    }

    @Override
    @NotNull
    public Optional<Throwable> getLastException() {
        return Optional.ofNullable(exception.get());
    }

    /**
     * Records an exception that occurred during execution.
     *
     * @param ex the exception
     */
    public void recordException(@NotNull Throwable ex) {
        this.exception.set(ex);
    }

    /**
     * Returns the time remaining until execution.
     *
     * @return the remaining duration, or empty if already executed
     */
    @NotNull
    public Optional<Duration> getTimeRemaining() {
        Instant scheduled = scheduledFor.get();
        if (scheduled == null || state.get() == TaskState.COMPLETED || state.get() == TaskState.CANCELLED) {
            return Optional.empty();
        }
        Duration remaining = Duration.between(Instant.now(), scheduled);
        return Optional.of(remaining.isNegative() ? Duration.ZERO : remaining);
    }

    @Override
    public String toString() {
        return "DelayedTask{" +
                "id=" + id +
                ", name=" + name +
                ", state=" + state.get() +
                ", delay=" + delayTicks +
                ", async=" + async +
                ", executed=" + (executedAt.get() != null) +
                '}';
    }
}
