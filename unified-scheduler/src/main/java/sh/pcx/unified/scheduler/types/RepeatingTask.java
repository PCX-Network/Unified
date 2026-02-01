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
 * A task that executes repeatedly on a fixed interval.
 *
 * <p>RepeatingTask is designed for operations that need to run periodically,
 * such as:
 * <ul>
 *   <li>Scoreboard updates</li>
 *   <li>Auto-save operations</li>
 *   <li>Particle effects</li>
 *   <li>Mob AI ticks</li>
 *   <li>Economy interest calculations</li>
 * </ul>
 *
 * <h2>Execution Timing</h2>
 * <p>The period is measured from the start of each execution. If an execution
 * takes longer than the period, the next execution starts immediately after.
 * Consider using async tasks or increasing the period if this occurs frequently.
 *
 * <h2>Max Executions</h2>
 * <p>You can optionally limit the number of executions. The task automatically
 * cancels after reaching the limit.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Update scoreboard every second
 * RepeatingTask scoreboard = new RepeatingTask(
 *     () -> updateScoreboard(),
 *     0,   // no initial delay
 *     20,  // every second
 *     false // sync
 * );
 *
 * // Auto-save every 5 minutes, async
 * RepeatingTask autoSave = new RepeatingTask(
 *     () -> saveAllPlayers(),
 *     20 * 60,     // 1 minute initial delay
 *     20 * 60 * 5, // every 5 minutes
 *     true // async
 * );
 *
 * // Limited countdown
 * RepeatingTask countdown = new RepeatingTask(
 *     "countdown",
 *     () -> announceCountdown(),
 *     null,
 *     0,
 *     20,
 *     10L, // run 10 times
 *     false,
 *     null,
 *     () -> startGame()
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. State can be queried from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DelayedTask
 * @see SyncTask
 * @see AsyncTask
 */
public class RepeatingTask implements Task {

    private final UUID id;
    private final String name;
    private final Runnable runnable;
    private final Consumer<ExecutionContext> contextConsumer;
    private final long delayTicks;
    private final long periodTicks;
    private final Long maxExecutions;
    private final boolean async;
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
     * Creates a new repeating task.
     *
     * @param runnable    the task to execute
     * @param delayTicks  initial delay before first execution
     * @param periodTicks period between executions
     * @param async       whether to run asynchronously
     */
    public RepeatingTask(@NotNull Runnable runnable, long delayTicks, long periodTicks, boolean async) {
        this(null, runnable, null, delayTicks, periodTicks, null, async, null, null);
    }

    /**
     * Creates a new repeating task with full configuration.
     *
     * @param name               optional task name
     * @param runnable           the task to execute (or null if using contextConsumer)
     * @param contextConsumer    context-aware consumer (or null if using runnable)
     * @param delayTicks         initial delay before first execution
     * @param periodTicks        period between executions
     * @param maxExecutions      maximum executions before auto-cancel (null for unlimited)
     * @param async              whether to run asynchronously
     * @param exceptionHandler   exception handler
     * @param completionCallback completion callback
     */
    public RepeatingTask(
            @Nullable String name,
            @Nullable Runnable runnable,
            @Nullable Consumer<ExecutionContext> contextConsumer,
            long delayTicks,
            long periodTicks,
            @Nullable Long maxExecutions,
            boolean async,
            @Nullable Consumer<Throwable> exceptionHandler,
            @Nullable Runnable completionCallback
    ) {
        if (runnable == null && contextConsumer == null) {
            throw new IllegalArgumentException("Either runnable or contextConsumer must be provided");
        }
        if (periodTicks <= 0) {
            throw new IllegalArgumentException("Period must be positive for repeating tasks");
        }

        this.id = UUID.randomUUID();
        this.name = name;
        this.runnable = runnable;
        this.contextConsumer = contextConsumer;
        this.delayTicks = Math.max(0, delayTicks);
        this.periodTicks = periodTicks;
        this.maxExecutions = maxExecutions;
        this.async = async;
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
        return periodTicks;
    }

    @Override
    public boolean isRepeating() {
        return true;
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
     * @param timestamp the execution timestamp
     * @param duration  the execution duration
     */
    public void recordExecution(@NotNull Instant timestamp, @NotNull Duration duration) {
        this.lastExecutedAt.set(timestamp);
        this.executionCount.incrementAndGet();
        this.totalExecutionTime.updateAndGet(current -> current.plus(duration));
    }

    @Override
    @NotNull
    public Optional<Instant> getNextExecutionAt() {
        TaskState currentState = state.get();
        if (currentState == TaskState.COMPLETED || currentState == TaskState.CANCELLED) {
            return Optional.empty();
        }
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

    /**
     * Returns the remaining executions before auto-cancel.
     *
     * @return the remaining count, or empty if unlimited
     */
    @NotNull
    public Optional<Long> getRemainingExecutions() {
        if (maxExecutions == null) {
            return Optional.empty();
        }
        long remaining = maxExecutions - executionCount.get();
        return Optional.of(Math.max(0, remaining));
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
     * @param ex the exception
     */
    public void recordException(@NotNull Throwable ex) {
        this.lastException.set(ex);
    }

    /**
     * Returns the time remaining until the next execution.
     *
     * @return the remaining duration, or empty if not scheduled
     */
    @NotNull
    public Optional<Duration> getTimeUntilNextExecution() {
        Instant next = nextExecutionAt.get();
        if (next == null) {
            return Optional.empty();
        }
        Duration remaining = Duration.between(Instant.now(), next);
        return Optional.of(remaining.isNegative() ? Duration.ZERO : remaining);
    }

    /**
     * Returns the total run time since the task was created.
     *
     * @return the duration since creation
     */
    @NotNull
    public Duration getUptime() {
        return Duration.between(createdAt, Instant.now());
    }

    @Override
    public String toString() {
        return "RepeatingTask{" +
                "id=" + id +
                ", name=" + name +
                ", state=" + state.get() +
                ", delay=" + delayTicks +
                ", period=" + periodTicks +
                ", async=" + async +
                ", executions=" + executionCount.get() +
                (maxExecutions != null ? ", maxExecutions=" + maxExecutions : "") +
                '}';
    }
}
