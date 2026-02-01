/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.folia;

import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.execution.ExecutionContext;
import sh.pcx.unified.world.UnifiedLocation;
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
 * A task that executes on the thread owning a specific location in Folia.
 *
 * <p>In Folia's multi-threaded architecture, each chunk region is owned by
 * a specific thread. LocationTask ensures that the task runs on the correct
 * thread for the specified location, making it safe to modify blocks and
 * interact with entities at that location.
 *
 * <h2>Location Binding</h2>
 * <p>The task is bound to the chunk region containing the specified location.
 * This means:
 * <ul>
 *   <li>Block modifications at the location are thread-safe</li>
 *   <li>Entity queries at the location return consistent results</li>
 *   <li>Particle spawning at the location works correctly</li>
 * </ul>
 *
 * <h2>Fallback Behavior</h2>
 * <p>On non-Folia servers, location tasks run on the main thread with
 * the same safety guarantees as regular sync tasks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Safe block modification
 * LocationTask blockTask = new LocationTask(
 *     location,
 *     () -> {
 *         world.setBlockData(location, blockData);
 *     },
 *     0,
 *     0
 * );
 *
 * // Repeating particle effect at location
 * LocationTask particleTask = new LocationTask(
 *     spawnLocation,
 *     () -> {
 *         world.spawnParticle(Particle.FLAME, location, 10);
 *     },
 *     0,
 *     10  // every 10 ticks
 * );
 *
 * // Delayed explosion
 * LocationTask explosion = new LocationTask(
 *     bombLocation,
 *     () -> {
 *         world.createExplosion(location, 4.0f);
 *     },
 *     100,  // 5 second fuse
 *     0
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RegionTask
 * @see EntityTask
 */
public class LocationTask implements Task {

    private final UUID id;
    private final String name;
    private final UnifiedLocation location;
    private final Runnable runnable;
    private final Consumer<ExecutionContext> contextConsumer;
    private final long delayTicks;
    private final long periodTicks;
    private final Long maxExecutions;
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
     * Creates a new location task.
     *
     * @param location    the location to bind the task to
     * @param runnable    the task to execute
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period between executions (0 for one-time)
     */
    public LocationTask(
            @NotNull UnifiedLocation location,
            @NotNull Runnable runnable,
            long delayTicks,
            long periodTicks
    ) {
        this(null, location, runnable, null, delayTicks, periodTicks, null, null, null);
    }

    /**
     * Creates a new location task with full configuration.
     *
     * @param name               optional task name
     * @param location           the location to bind the task to
     * @param runnable           the task to execute
     * @param contextConsumer    context-aware consumer
     * @param delayTicks         initial delay in ticks
     * @param periodTicks        period between executions
     * @param maxExecutions      maximum executions
     * @param exceptionHandler   exception handler
     * @param completionCallback completion callback
     */
    public LocationTask(
            @Nullable String name,
            @NotNull UnifiedLocation location,
            @Nullable Runnable runnable,
            @Nullable Consumer<ExecutionContext> contextConsumer,
            long delayTicks,
            long periodTicks,
            @Nullable Long maxExecutions,
            @Nullable Consumer<Throwable> exceptionHandler,
            @Nullable Runnable completionCallback
    ) {
        if (runnable == null && contextConsumer == null) {
            throw new IllegalArgumentException("Either runnable or contextConsumer must be provided");
        }

        this.id = UUID.randomUUID();
        this.name = name;
        this.location = location;
        this.runnable = runnable;
        this.contextConsumer = contextConsumer;
        this.delayTicks = Math.max(0, delayTicks);
        this.periodTicks = Math.max(0, periodTicks);
        this.maxExecutions = maxExecutions;
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

    /**
     * Returns the location this task is bound to.
     *
     * @return the unified location
     */
    @NotNull
    public UnifiedLocation getLocation() {
        return location;
    }

    /**
     * Returns the chunk X coordinate for this location.
     *
     * @return the chunk X
     */
    public int getChunkX() {
        return location.getChunkX();
    }

    /**
     * Returns the chunk Z coordinate for this location.
     *
     * @return the chunk Z
     */
    public int getChunkZ() {
        return location.getChunkZ();
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
        return TaskType.LOCATION;
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
        return Optional.of(location);
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
     * @param exception the exception
     */
    public void recordException(@NotNull Throwable exception) {
        this.lastException.set(exception);
    }

    @Override
    public String toString() {
        return "LocationTask{" +
                "id=" + id +
                ", name=" + name +
                ", location=" + location +
                ", chunk=[" + getChunkX() + ", " + getChunkZ() + "]" +
                ", state=" + state.get() +
                ", delay=" + delayTicks +
                ", period=" + periodTicks +
                ", executions=" + executionCount.get() +
                '}';
    }
}
