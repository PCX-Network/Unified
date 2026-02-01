/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.scheduler;

import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Mock implementation of TaskHandle for testing scheduled tasks.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockScheduler
 */
public final class MockTaskHandle implements TaskHandle {

    private final MockScheduler.ScheduledMockTask scheduledTask;
    private final MockScheduler scheduler;
    private final MockTask task;
    private final CompletableFuture<Void> future;

    /**
     * Creates a new mock task handle.
     *
     * @param scheduledTask the underlying scheduled task
     * @param scheduler     the scheduler that created this task
     */
    MockTaskHandle(
        @NotNull MockScheduler.ScheduledMockTask scheduledTask,
        @NotNull MockScheduler scheduler
    ) {
        this.scheduledTask = Objects.requireNonNull(scheduledTask, "scheduledTask cannot be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
        this.task = new MockTask(scheduledTask);
        this.future = new CompletableFuture<>();
    }

    @Override
    @NotNull
    public UUID getTaskId() {
        return scheduledTask.getId();
    }

    @Override
    @NotNull
    public Task getTask() {
        return task;
    }

    @Override
    public boolean cancel() {
        if (!scheduledTask.isCancelled()) {
            scheduler.cancelTask(scheduledTask);
            future.complete(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isActive() {
        return !scheduledTask.isCancelled() && !isDone();
    }

    @Override
    public boolean isCancelled() {
        return scheduledTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        if (scheduledTask.isCancelled()) {
            return true;
        }
        if (!scheduledTask.isRepeating()) {
            return scheduledTask.getExecutionCount() > 0;
        }
        return false;
    }

    @Override
    @NotNull
    public Task.TaskState getState() {
        if (scheduledTask.isCancelled()) {
            return Task.TaskState.CANCELLED;
        }
        if (isDone()) {
            return Task.TaskState.COMPLETED;
        }
        if (scheduledTask.getExecutionCount() > 0) {
            return Task.TaskState.RUNNING;
        }
        return Task.TaskState.PENDING;
    }

    @Override
    @NotNull
    public Task.TaskType getType() {
        return scheduledTask.isAsync() ? Task.TaskType.ASYNC : Task.TaskType.SYNC;
    }

    @Override
    public long getExecutionCount() {
        return scheduledTask.getExecutionCount();
    }

    @Override
    @NotNull
    public Instant getScheduledAt() {
        return scheduledTask.getCreatedAt();
    }

    @Override
    @NotNull
    public Optional<Instant> getLastExecutedAt() {
        return scheduledTask.getLastExecutedAt();
    }

    @Override
    @NotNull
    public Optional<Instant> getNextExecutionAt() {
        if (isDone()) {
            return Optional.empty();
        }
        // Approximate based on ticks
        return Optional.of(Instant.now().plusMillis(50)); // Assume 50ms per tick
    }

    @Override
    @NotNull
    public Duration getTotalExecutionTime() {
        // Simplified - would track actual execution time in full implementation
        return Duration.ofMillis(scheduledTask.getExecutionCount());
    }

    @Override
    @NotNull
    public Duration getAverageExecutionTime() {
        long count = scheduledTask.getExecutionCount();
        if (count == 0) {
            return Duration.ZERO;
        }
        return getTotalExecutionTime().dividedBy(count);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> toFuture() {
        return future;
    }

    @Override
    public void await() throws InterruptedException {
        try {
            future.get();
        } catch (Exception e) {
            if (e instanceof InterruptedException ie) {
                throw ie;
            }
        }
    }

    @Override
    public boolean await(@NotNull Duration timeout) throws InterruptedException {
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            if (e instanceof InterruptedException ie) {
                throw ie;
            }
            return false;
        }
    }

    /**
     * Mock implementation of Task.
     */
    private static class MockTask implements Task {
        private final MockScheduler.ScheduledMockTask scheduledTask;

        MockTask(MockScheduler.ScheduledMockTask scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        @NotNull
        public UUID getId() {
            return scheduledTask.getId();
        }

        @Override
        @NotNull
        public Optional<String> getName() {
            return Optional.empty();
        }

        @Override
        @NotNull
        public TaskType getType() {
            return scheduledTask.isAsync() ? TaskType.ASYNC : TaskType.SYNC;
        }

        @Override
        @NotNull
        public TaskState getState() {
            if (scheduledTask.isCancelled()) {
                return TaskState.CANCELLED;
            }
            if (scheduledTask.getExecutionCount() > 0 && !scheduledTask.isRepeating()) {
                return TaskState.COMPLETED;
            }
            return TaskState.PENDING;
        }

        @Override
        @NotNull
        public Runnable getRunnable() {
            return scheduledTask.getRunnable();
        }

        @Override
        @NotNull
        public Optional<java.util.function.Consumer<sh.pcx.unified.scheduler.execution.ExecutionContext>> getContextConsumer() {
            return Optional.empty();
        }

        @Override
        public long getDelayTicks() {
            return scheduledTask.getScheduledTick();
        }

        @Override
        public long getPeriodTicks() {
            return scheduledTask.getPeriodTicks();
        }

        @Override
        @NotNull
        public Instant getCreatedAt() {
            return scheduledTask.getCreatedAt();
        }

        @Override
        @NotNull
        public Optional<Instant> getLastExecutedAt() {
            return scheduledTask.getLastExecutedAt();
        }

        @Override
        @NotNull
        public Optional<Instant> getNextExecutionAt() {
            return Optional.empty();
        }

        @Override
        public long getExecutionCount() {
            return scheduledTask.getExecutionCount();
        }

        @Override
        @NotNull
        public Optional<Long> getMaxExecutions() {
            return Optional.empty();
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
            return Duration.ofMillis(scheduledTask.getExecutionCount());
        }

        @Override
        @NotNull
        public Duration getAverageExecutionTime() {
            long count = scheduledTask.getExecutionCount();
            return count > 0 ? Duration.ofMillis(1) : Duration.ZERO;
        }

        @Override
        @NotNull
        public Optional<Throwable> getLastException() {
            return scheduledTask.getLastError();
        }
    }
}
