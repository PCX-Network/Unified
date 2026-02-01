/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.folia;

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
 * A task that executes on a specific region's thread in Folia.
 *
 * <p>Folia divides the world into regions, each with its own tick thread.
 * RegionTask is the base class for tasks that need to execute within
 * a specific region's context, ensuring thread-safe access to that region's
 * entities and blocks.
 *
 * <h2>Folia Regions</h2>
 * <p>In Folia, the world is partitioned into regions based on chunk groups.
 * Each region ticks independently on its own thread. Operations that modify
 * entities or blocks must run on the correct region thread.
 *
 * <h2>Fallback Behavior</h2>
 * <p>On non-Folia servers (Paper, Spigot), region tasks fall back to
 * running on the main server thread, maintaining compatibility.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a region task bound to a specific world region
 * RegionTask task = new RegionTask(
 *     world,
 *     chunkX,
 *     chunkZ,
 *     () -> {
 *         // Safe to modify blocks/entities in this region
 *         world.setBlockData(loc, blockData);
 *     },
 *     0,
 *     0
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EntityTask
 * @see LocationTask
 * @see GlobalTask
 */
public class RegionTask implements Task {

    private final UUID id;
    private final String name;
    private final Object world;
    private final int chunkX;
    private final int chunkZ;
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
     * Creates a new region task.
     *
     * @param world       the world containing the region
     * @param chunkX      the chunk X coordinate of the region
     * @param chunkZ      the chunk Z coordinate of the region
     * @param runnable    the task to execute
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period between executions (0 for one-time)
     */
    public RegionTask(
            @NotNull Object world,
            int chunkX,
            int chunkZ,
            @NotNull Runnable runnable,
            long delayTicks,
            long periodTicks
    ) {
        this(null, world, chunkX, chunkZ, runnable, null, delayTicks, periodTicks, null, null, null);
    }

    /**
     * Creates a new region task with full configuration.
     *
     * @param name               optional task name
     * @param world              the world containing the region
     * @param chunkX             the chunk X coordinate
     * @param chunkZ             the chunk Z coordinate
     * @param runnable           the task to execute
     * @param contextConsumer    context-aware consumer
     * @param delayTicks         initial delay in ticks
     * @param periodTicks        period between executions
     * @param maxExecutions      maximum executions
     * @param exceptionHandler   exception handler
     * @param completionCallback completion callback
     */
    public RegionTask(
            @Nullable String name,
            @NotNull Object world,
            int chunkX,
            int chunkZ,
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
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
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
     * Returns the world this task is bound to.
     *
     * @return the world object
     */
    @NotNull
    public Object getWorld() {
        return world;
    }

    /**
     * Returns the chunk X coordinate.
     *
     * @return the chunk X
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * Returns the chunk Z coordinate.
     *
     * @return the chunk Z
     */
    public int getChunkZ() {
        return chunkZ;
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
        return TaskType.LOCATION; // Region tasks are location-based
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
        return Optional.empty(); // Region is identified by world + chunk coords
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
        return "RegionTask{" +
                "id=" + id +
                ", name=" + name +
                ", chunk=[" + chunkX + ", " + chunkZ + "]" +
                ", state=" + state.get() +
                ", delay=" + delayTicks +
                ", period=" + periodTicks +
                ", executions=" + executionCount.get() +
                '}';
    }
}
