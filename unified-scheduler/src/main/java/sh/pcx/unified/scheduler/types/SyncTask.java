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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A synchronous task that executes on the main server thread.
 *
 * <p>Sync tasks are used for operations that must interact with Minecraft
 * world state, entities, or other non-thread-safe APIs. On Folia, sync tasks
 * run on the global region scheduler; for entity/location-specific operations,
 * use {@link sh.pcx.unified.scheduler.folia.EntityTask} or
 * {@link sh.pcx.unified.scheduler.folia.LocationTask} instead.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * SyncTask task = new SyncTask(
 *     () -> player.sendMessage("Hello!"),
 *     0,  // no delay
 *     0   // not repeating
 * );
 *
 * // With delay
 * SyncTask delayed = new SyncTask(
 *     () -> world.setBlockData(loc, data),
 *     20,  // 1 second delay
 *     0
 * );
 *
 * // Repeating
 * SyncTask repeating = new SyncTask(
 *     () -> updateScoreboard(),
 *     0,
 *     20  // every second
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The task itself executes on a single thread,
 * but the state can be queried from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AsyncTask
 * @see Task
 */
public class SyncTask implements Task {

    private final UUID id;
    private final String name;
    private final Runnable runnable;
    private final Consumer<ExecutionContext> contextConsumer;
    private final long delayTicks;
    private final long periodTicks;
    private final Long maxExecutions;
    private final Runnable retiredCallback;
    private final Consumer<Throwable> exceptionHandler;
    private final Runnable completionCallback;
    private final Instant createdAt;

    private final AtomicReference<TaskState> state;
    private final AtomicLong executionCount;
    private final AtomicReference<Instant> lastExecutedAt;
    private final AtomicReference<Instant> nextExecutionAt;
    private final AtomicReference<Duration> totalExecutionTime;
    private final AtomicReference<Throwable> lastException;

    /**
     * Creates a new synchronous task with a runnable.
     *
     * @param runnable    the task to execute
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period between executions (0 for one-time)
     */
    public SyncTask(@NotNull Runnable runnable, long delayTicks, long periodTicks) {
        this(null, runnable, null, delayTicks, periodTicks, null, null, null, null);
    }

    /**
     * Creates a new synchronous task with full configuration.
     *
     * @param name               optional task name
     * @param runnable           the task to execute (or null if using contextConsumer)
     * @param contextConsumer    context-aware consumer (or null if using runnable)
     * @param delayTicks         initial delay in ticks
     * @param periodTicks        period between executions (0 for one-time)
     * @param maxExecutions      maximum number of executions (null for unlimited)
     * @param retiredCallback    callback if entity retired (Folia)
     * @param exceptionHandler   exception handler
     * @param completionCallback completion callback
     */
    public SyncTask(
            @Nullable String name,
            @Nullable Runnable runnable,
            @Nullable Consumer<ExecutionContext> contextConsumer,
            long delayTicks,
            long periodTicks,
            @Nullable Long maxExecutions,
            @Nullable Runnable retiredCallback,
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
        this.periodTicks = Math.max(0, periodTicks);
        this.maxExecutions = maxExecutions;
        this.retiredCallback = retiredCallback;
        this.exceptionHandler = exceptionHandler;
        this.completionCallback = completionCallback;
        this.createdAt = Instant.now();

        this.state = new AtomicReference<>(TaskState.PENDING);
        this.executionCount = new AtomicLong(0);
        this.lastExecutedAt = new AtomicReference<>(null);
        this.nextExecutionAt = new AtomicReference<>(null);
        this.totalExecutionTime = new AtomicReference<>(Duration.ZERO);
        this.lastException = new AtomicReference<>(null);
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
        return TaskType.SYNC;
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
        return periodTicks;
    }

    @Override
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    @NotNull
    public Optional<Instant> getLastExecutedAt() {
        return Optional.ofNullable(lastExecutedAt.get());
    }

    /**
     * Records that the task has executed.
     *
     * @param executedAt the execution timestamp
     * @param duration   the execution duration
     */
    public void recordExecution(@NotNull Instant executedAt, @NotNull Duration duration) {
        this.lastExecutedAt.set(executedAt);
        this.executionCount.incrementAndGet();
        this.totalExecutionTime.updateAndGet(current -> current.plus(duration));
    }

    @Override
    @NotNull
    public Optional<Instant> getNextExecutionAt() {
        return Optional.ofNullable(nextExecutionAt.get());
    }

    /**
     * Sets the next scheduled execution time.
     *
     * @param nextExecution the next execution time
     */
    public void setNextExecutionAt(@Nullable Instant nextExecution) {
        this.nextExecutionAt.set(nextExecution);
    }

    @Override
    public long getExecutionCount() {
        return executionCount.get();
    }

    @Override
    @NotNull
    public Optional<Long> getMaxExecutions() {
        return Optional.ofNullable(maxExecutions);
    }

    /**
     * Checks if the task has reached its maximum execution count.
     *
     * @return true if max executions reached
     */
    public boolean hasReachedMaxExecutions() {
        if (maxExecutions == null) {
            return false;
        }
        return executionCount.get() >= maxExecutions;
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
        return Optional.ofNullable(retiredCallback);
    }

    @Override
    @NotNull
    public Duration getTotalExecutionTime() {
        return totalExecutionTime.get();
    }

    @Override
    @NotNull
    public Duration getAverageExecutionTime() {
        long count = executionCount.get();
        if (count == 0) {
            return Duration.ZERO;
        }
        return totalExecutionTime.get().dividedBy(count);
    }

    @Override
    @NotNull
    public Optional<Throwable> getLastException() {
        return Optional.ofNullable(lastException.get());
    }

    /**
     * Records an exception that occurred during execution.
     *
     * @param exception the exception
     */
    public void recordException(@NotNull Throwable exception) {
        this.lastException.set(exception);
    }

    @Override
    public String toString() {
        return "SyncTask{" +
                "id=" + id +
                ", name=" + name +
                ", state=" + state.get() +
                ", delay=" + delayTicks +
                ", period=" + periodTicks +
                ", executions=" + executionCount.get() +
                '}';
    }
}
