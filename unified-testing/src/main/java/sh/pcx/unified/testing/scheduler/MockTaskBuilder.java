/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.scheduler;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.TaskBuilder;
import sh.pcx.unified.scheduler.TaskHandle;
import sh.pcx.unified.scheduler.execution.ExecutionContext;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Mock implementation of TaskBuilder for the testing framework.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class MockTaskBuilder implements TaskBuilder {

    private final MockScheduler scheduler;
    private boolean async = false;
    private long delayTicks = 0;
    private long periodTicks = 0;
    private long maxExecutions = 0;
    private Runnable task;
    private Consumer<ExecutionContext> contextConsumer;
    private String name;
    private Consumer<Throwable> errorHandler;
    private Runnable retiredCallback;
    private Runnable completionCallback;

    /**
     * Creates a new mock task builder.
     *
     * @param scheduler the mock scheduler
     */
    MockTaskBuilder(@NotNull MockScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
    }

    @Override
    @NotNull
    public TaskBuilder sync() {
        this.async = false;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder async() {
        this.async = true;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder global() {
        this.async = false;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder atEntity(@NotNull Object entity) {
        // In mock, entity-bound tasks are just sync tasks
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder atPlayer(@NotNull UnifiedPlayer player) {
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder atLocation(@NotNull UnifiedLocation location) {
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder delay(long ticks) {
        this.delayTicks = ticks;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder delay(long duration, @NotNull TimeUnit unit) {
        // Convert to ticks (20 ticks = 1 second)
        this.delayTicks = unit.toMillis(duration) / 50;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder delay(@NotNull Duration duration) {
        this.delayTicks = duration.toMillis() / 50;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder repeat(long ticks) {
        this.periodTicks = ticks;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder repeat(long duration, @NotNull TimeUnit unit) {
        this.periodTicks = unit.toMillis(duration) / 50;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder repeat(@NotNull Duration duration) {
        this.periodTicks = duration.toMillis() / 50;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder maxExecutions(long count) {
        if (count < 1) {
            throw new IllegalArgumentException("maxExecutions must be at least 1");
        }
        this.maxExecutions = count;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder name(@NotNull String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder onRetired(@NotNull Runnable callback) {
        this.retiredCallback = callback;
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder onException(@NotNull Consumer<Throwable> handler) {
        this.errorHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public TaskBuilder onComplete(@NotNull Runnable callback) {
        this.completionCallback = callback;
        return this;
    }

    @Override
    @NotNull
    public TaskHandle execute(@NotNull Runnable runnable) {
        this.task = Objects.requireNonNull(runnable);
        return buildAndSchedule();
    }

    @Override
    @NotNull
    public TaskHandle executeWithContext(@NotNull Consumer<ExecutionContext> consumer) {
        this.contextConsumer = Objects.requireNonNull(consumer);
        this.task = () -> {}; // Placeholder
        return buildAndSchedule();
    }

    @Override
    @NotNull
    public Task buildTask(@NotNull Runnable runnable) {
        this.task = Objects.requireNonNull(runnable);
        return createTask();
    }

    @Override
    @NotNull
    public Task buildTaskWithContext(@NotNull Consumer<ExecutionContext> consumer) {
        this.contextConsumer = Objects.requireNonNull(consumer);
        this.task = () -> {};
        return createTask();
    }

    private TaskHandle buildAndSchedule() {
        Runnable wrappedTask = task;
        if (errorHandler != null) {
            final Runnable originalTask = task;
            wrappedTask = () -> {
                try {
                    originalTask.run();
                } catch (Throwable t) {
                    errorHandler.accept(t);
                }
            };
        }

        if (async) {
            if (periodTicks > 0) {
                return scheduler.runTaskTimerAsync(wrappedTask, delayTicks, periodTicks);
            } else if (delayTicks > 0) {
                return scheduler.runTaskLaterAsync(wrappedTask, delayTicks);
            } else {
                return scheduler.runTaskAsync(wrappedTask);
            }
        } else {
            if (periodTicks > 0) {
                return scheduler.runTaskTimer(wrappedTask, delayTicks, periodTicks);
            } else if (delayTicks > 0) {
                return scheduler.runTaskLater(wrappedTask, delayTicks);
            } else {
                return scheduler.runTask(wrappedTask);
            }
        }
    }

    private Task createTask() {
        // Return a simple mock task implementation
        return new MockTask(task, async, delayTicks, periodTicks, name);
    }

    /**
     * Simple mock Task implementation.
     */
    private static class MockTask implements Task {
        private final Runnable runnable;
        private final boolean async;
        private final long delayTicks;
        private final long periodTicks;
        private final String name;
        private final java.util.UUID id = java.util.UUID.randomUUID();
        private final java.time.Instant createdAt = java.time.Instant.now();

        MockTask(Runnable runnable, boolean async, long delayTicks, long periodTicks, String name) {
            this.runnable = runnable;
            this.async = async;
            this.delayTicks = delayTicks;
            this.periodTicks = periodTicks;
            this.name = name;
        }

        @Override public @NotNull java.util.UUID getId() { return id; }
        @Override public @NotNull java.util.Optional<String> getName() { return java.util.Optional.ofNullable(name); }
        @Override public @NotNull TaskType getType() { return async ? TaskType.ASYNC : TaskType.SYNC; }
        @Override public @NotNull TaskState getState() { return TaskState.PENDING; }
        @Override public @NotNull Runnable getRunnable() { return runnable; }
        @Override public @NotNull java.util.Optional<Consumer<ExecutionContext>> getContextConsumer() { return java.util.Optional.empty(); }
        @Override public long getDelayTicks() { return delayTicks; }
        @Override public long getPeriodTicks() { return periodTicks; }
        @Override public @NotNull java.time.Instant getCreatedAt() { return createdAt; }
        @Override public @NotNull java.util.Optional<java.time.Instant> getLastExecutedAt() { return java.util.Optional.empty(); }
        @Override public @NotNull java.util.Optional<java.time.Instant> getNextExecutionAt() { return java.util.Optional.empty(); }
        @Override public long getExecutionCount() { return 0; }
        @Override public @NotNull java.util.Optional<Long> getMaxExecutions() { return java.util.Optional.empty(); }
        @Override public @NotNull java.util.Optional<Object> getBoundEntity() { return java.util.Optional.empty(); }
        @Override public @NotNull java.util.Optional<Object> getBoundLocation() { return java.util.Optional.empty(); }
        @Override public @NotNull java.util.Optional<Runnable> getRetiredCallback() { return java.util.Optional.empty(); }
        @Override public @NotNull Duration getTotalExecutionTime() { return Duration.ZERO; }
        @Override public @NotNull Duration getAverageExecutionTime() { return Duration.ZERO; }
        @Override public @NotNull java.util.Optional<Throwable> getLastException() { return java.util.Optional.empty(); }
    }
}
