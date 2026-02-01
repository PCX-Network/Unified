/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.scheduler;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.scheduler.SchedulerService;
import sh.pcx.unified.scheduler.Task;
import sh.pcx.unified.scheduler.TaskBuilder;
import sh.pcx.unified.scheduler.TaskChain;
import sh.pcx.unified.scheduler.TaskHandle;
import sh.pcx.unified.scheduler.execution.ExecutionContext;
import sh.pcx.unified.testing.server.MockServer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Mock implementation of the scheduler service for testing.
 *
 * <p>MockScheduler provides complete control over task execution timing,
 * allowing tests to advance the server tick and verify scheduled task behavior
 * without waiting for real time to pass.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Tick-based task execution</li>
 *   <li>Manual tick advancement</li>
 *   <li>Async task handling</li>
 *   <li>Task cancellation</li>
 *   <li>Task inspection</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockServer server = MockServer.start();
 * MockScheduler scheduler = server.getScheduler();
 *
 * // Schedule a delayed task
 * AtomicBoolean executed = new AtomicBoolean(false);
 * scheduler.runTaskLater(() -> executed.set(true), 20);
 *
 * // Task hasn't run yet
 * assertThat(executed.get()).isFalse();
 *
 * // Advance time
 * server.advanceTicks(20);
 *
 * // Now it has run
 * assertThat(executed.get()).isTrue();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockServer
 * @see MockTaskHandle
 */
public final class MockScheduler implements SchedulerService {

    private final MockServer server;
    private final PriorityQueue<ScheduledMockTask> scheduledTasks;
    private final List<ScheduledMockTask> repeatingTasks;
    private final ExecutorService asyncExecutor;
    private final List<CompletableFuture<?>> pendingAsyncTasks;
    private final AtomicLong taskIdCounter = new AtomicLong(0);

    /**
     * Creates a new mock scheduler.
     *
     * @param server the mock server
     */
    public MockScheduler(@NotNull MockServer server) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.scheduledTasks = new PriorityQueue<>(Comparator.comparingLong(t -> t.scheduledTick));
        this.repeatingTasks = new CopyOnWriteArrayList<>();
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "MockAsync-" + taskIdCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        this.pendingAsyncTasks = new CopyOnWriteArrayList<>();
    }

    /**
     * Processes all tasks scheduled for the current tick.
     *
     * <p>This method is called by the server during tick advancement.
     */
    public void tick() {
        long currentTick = server.getCurrentTick();

        // Process all tasks scheduled for this tick or earlier
        while (!scheduledTasks.isEmpty() && scheduledTasks.peek().scheduledTick <= currentTick) {
            ScheduledMockTask task = scheduledTasks.poll();
            if (task != null && !task.isCancelled()) {
                executeTask(task);
            }
        }

        // Process repeating tasks
        for (ScheduledMockTask task : repeatingTasks) {
            if (!task.isCancelled() && task.nextExecutionTick <= currentTick) {
                executeTask(task);
                task.nextExecutionTick = currentTick + task.periodTicks;
            }
        }
    }

    private void executeTask(ScheduledMockTask task) {
        try {
            task.runnable.run();
            task.executionCount++;
            task.lastExecutedAt = Instant.now();
        } catch (Exception e) {
            task.lastError = e;
            // Log but don't fail - matches real scheduler behavior
            System.err.println("Error executing scheduled task: " + e.getMessage());
        }
    }

    /**
     * Resets the scheduler, cancelling all tasks.
     */
    public void reset() {
        scheduledTasks.clear();
        repeatingTasks.clear();
        pendingAsyncTasks.clear();
    }

    /**
     * Waits for all pending async tasks to complete.
     */
    public void waitForAsyncTasks() {
        waitForAsyncTasks(Duration.ofSeconds(10));
    }

    /**
     * Waits for all pending async tasks to complete with a timeout.
     *
     * @param timeout the maximum time to wait
     */
    public void waitForAsyncTasks(@NotNull Duration timeout) {
        try {
            CompletableFuture.allOf(
                pendingAsyncTasks.toArray(new CompletableFuture<?>[0])
            ).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout waiting for async tasks", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for async tasks", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error in async task", e.getCause());
        }
    }

    /**
     * Returns the number of pending sync tasks.
     *
     * @return the count of pending sync tasks
     */
    public int getPendingSyncTaskCount() {
        return scheduledTasks.size() + repeatingTasks.size();
    }

    /**
     * Returns all scheduled tasks for inspection.
     *
     * @return unmodifiable list of scheduled tasks
     */
    @NotNull
    public List<ScheduledMockTask> getScheduledTasks() {
        List<ScheduledMockTask> all = new ArrayList<>(scheduledTasks);
        all.addAll(repeatingTasks);
        return Collections.unmodifiableList(all);
    }

    // ==================== SchedulerService Implementation ====================

    @Override
    @NotNull
    public TaskHandle runTask(@NotNull Runnable task) {
        return scheduleTask(task, 0, 0, false, false);
    }

    @Override
    @NotNull
    public TaskHandle runTaskAsync(@NotNull Runnable task) {
        return scheduleTask(task, 0, 0, true, false);
    }

    @Override
    @NotNull
    public TaskHandle runTaskLater(@NotNull Runnable task, long delayTicks) {
        return scheduleTask(task, delayTicks, 0, false, false);
    }

    @Override
    @NotNull
    public TaskHandle runTaskLaterAsync(@NotNull Runnable task, long delayTicks) {
        return scheduleTask(task, delayTicks, 0, true, false);
    }

    @Override
    @NotNull
    public TaskHandle runTaskTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduleTask(task, delayTicks, periodTicks, false, true);
    }

    @Override
    @NotNull
    public TaskHandle runTaskTimerAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduleTask(task, delayTicks, periodTicks, true, true);
    }

    @Override
    @NotNull
    public TaskHandle runAtEntity(@NotNull Object entity, @NotNull Runnable task) {
        // In mock, entity-bound tasks run immediately on sync thread
        return runTask(task);
    }

    @Override
    @NotNull
    public TaskHandle runAtEntityLater(@NotNull Object entity, @NotNull Runnable task, long delayTicks) {
        return runTaskLater(task, delayTicks);
    }

    @Override
    @NotNull
    public TaskHandle runAtPlayer(@NotNull UnifiedPlayer player, @NotNull Runnable task) {
        return runTask(task);
    }

    @Override
    @NotNull
    public TaskHandle runAtPlayerLater(@NotNull UnifiedPlayer player, @NotNull Runnable task, long delayTicks) {
        return runTaskLater(task, delayTicks);
    }

    @Override
    @NotNull
    public TaskHandle runAtLocation(@NotNull UnifiedLocation location, @NotNull Runnable task) {
        return runTask(task);
    }

    @Override
    @NotNull
    public TaskHandle runAtLocationLater(@NotNull UnifiedLocation location, @NotNull Runnable task, long delayTicks) {
        return runTaskLater(task, delayTicks);
    }

    @Override
    @NotNull
    public TaskHandle runOnGlobal(@NotNull Runnable task) {
        return runTask(task);
    }

    @Override
    @NotNull
    public TaskHandle runOnGlobalLater(@NotNull Runnable task, long delayTicks) {
        return runTaskLater(task, delayTicks);
    }

    @Override
    @NotNull
    public TaskHandle runOnGlobalTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return runTaskTimer(task, delayTicks, periodTicks);
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> supplySync(@NotNull Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runTask(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> runSync(@NotNull Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runTask(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> runAsync(@NotNull Runnable runnable) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(runnable, asyncExecutor);
        pendingAsyncTasks.add(future);
        future.whenComplete((v, e) -> pendingAsyncTasks.remove(future));
        return future;
    }

    @Override
    @NotNull
    public TaskHandle runTaskWithContext(@NotNull Consumer<ExecutionContext> task) {
        return runTask(() -> {
            ExecutionContext context = new MockExecutionContext();
            task.accept(context);
        });
    }

    @Override
    @NotNull
    public TaskBuilder builder() {
        return new MockTaskBuilder(this);
    }

    @Override
    @NotNull
    public TaskChain chain() {
        return new MockTaskChain(this);
    }

    @Override
    public void cancelAllTasks() {
        scheduledTasks.forEach(ScheduledMockTask::cancel);
        scheduledTasks.clear();
        repeatingTasks.forEach(ScheduledMockTask::cancel);
        repeatingTasks.clear();
    }

    @Override
    public boolean isMainThread() {
        return server.isPrimaryThread();
    }

    @Override
    public boolean isGlobalThread() {
        return isMainThread();
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    @Override
    public int getPendingTaskCount() {
        return scheduledTasks.size() + repeatingTasks.size() + pendingAsyncTasks.size();
    }

    // ==================== Internal Methods ====================

    private TaskHandle scheduleTask(
        Runnable runnable,
        long delayTicks,
        long periodTicks,
        boolean async,
        boolean repeating
    ) {
        Objects.requireNonNull(runnable, "runnable cannot be null");

        ScheduledMockTask task = new ScheduledMockTask(
            UUID.randomUUID(),
            runnable,
            server.getCurrentTick() + delayTicks,
            periodTicks,
            async,
            repeating
        );

        if (async && delayTicks == 0 && !repeating) {
            // Execute async task immediately
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    runnable.run();
                    task.executionCount++;
                    task.lastExecutedAt = Instant.now();
                } catch (Exception e) {
                    task.lastError = e;
                }
            }, asyncExecutor);
            pendingAsyncTasks.add(future);
            future.whenComplete((v, e) -> pendingAsyncTasks.remove(future));
        } else if (repeating) {
            repeatingTasks.add(task);
        } else {
            scheduledTasks.add(task);
        }

        return new MockTaskHandle(task, this);
    }

    void cancelTask(ScheduledMockTask task) {
        task.cancel();
        scheduledTasks.remove(task);
        repeatingTasks.remove(task);
    }

    /**
     * Represents a scheduled task in the mock scheduler.
     */
    public static final class ScheduledMockTask {
        private final UUID id;
        private final Runnable runnable;
        private final long scheduledTick;
        private final long periodTicks;
        private final boolean async;
        private final boolean repeating;
        private final Instant createdAt;

        private long nextExecutionTick;
        private long executionCount = 0;
        private Instant lastExecutedAt;
        private Throwable lastError;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        ScheduledMockTask(
            UUID id,
            Runnable runnable,
            long scheduledTick,
            long periodTicks,
            boolean async,
            boolean repeating
        ) {
            this.id = id;
            this.runnable = runnable;
            this.scheduledTick = scheduledTick;
            this.nextExecutionTick = scheduledTick;
            this.periodTicks = periodTicks;
            this.async = async;
            this.repeating = repeating;
            this.createdAt = Instant.now();
        }

        public UUID getId() {
            return id;
        }

        public Runnable getRunnable() {
            return runnable;
        }

        public long getScheduledTick() {
            return scheduledTick;
        }

        public long getPeriodTicks() {
            return periodTicks;
        }

        public boolean isAsync() {
            return async;
        }

        public boolean isRepeating() {
            return repeating;
        }

        public long getExecutionCount() {
            return executionCount;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public Optional<Instant> getLastExecutedAt() {
            return Optional.ofNullable(lastExecutedAt);
        }

        public Optional<Throwable> getLastError() {
            return Optional.ofNullable(lastError);
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        void cancel() {
            cancelled.set(true);
        }
    }

    /**
     * Mock implementation of ExecutionContext.
     */
    private static class MockExecutionContext implements ExecutionContext {
        private final java.util.UUID taskId = java.util.UUID.randomUUID();
        private final Instant createdAt = Instant.now();
        private final Instant currentExecutionStart = Instant.now();
        private final java.util.Map<String, Object> data = new java.util.concurrent.ConcurrentHashMap<>();
        private volatile boolean cancelled = false;
        private volatile boolean skipNext = false;

        @Override
        public @NotNull java.util.UUID getTaskId() { return taskId; }

        @Override
        public @NotNull java.util.Optional<String> getTaskName() { return java.util.Optional.empty(); }

        @Override
        public @NotNull sh.pcx.unified.scheduler.Task.TaskType getTaskType() {
            return sh.pcx.unified.scheduler.Task.TaskType.SYNC;
        }

        @Override
        public long getExecutionCount() { return 1; }

        @Override
        public @NotNull java.util.Optional<Long> getMaxExecutions() { return java.util.Optional.empty(); }

        @Override
        public @NotNull Instant getCreatedAt() { return createdAt; }

        @Override
        public @NotNull java.util.Optional<Instant> getLastExecutedAt() { return java.util.Optional.empty(); }

        @Override
        public @NotNull Instant getCurrentExecutionStart() { return currentExecutionStart; }

        @Override
        public long getDelayTicks() { return 0; }

        @Override
        public long getPeriodTicks() { return 0; }

        @Override
        public @NotNull Duration getTotalExecutionTime() { return Duration.ZERO; }

        @Override
        public @NotNull Duration getAverageExecutionTime() { return Duration.ZERO; }

        @Override
        public @NotNull TaskHandle getHandle() {
            throw new UnsupportedOperationException("Handle not available in mock context");
        }

        @Override
        public void cancel() { cancelled = true; }

        @Override
        public boolean isCancelled() { return cancelled; }

        @Override
        public void skipNext() { skipNext = true; }

        @Override
        public boolean willSkipNext() { return skipNext; }

        @Override
        public void setData(@NotNull String key, @org.jetbrains.annotations.Nullable Object value) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull <T> java.util.Optional<T> getData(@NotNull String key, @NotNull Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isInstance(value)) {
                return java.util.Optional.of((T) value);
            }
            return java.util.Optional.empty();
        }

        @Override
        public boolean hasData(@NotNull String key) { return data.containsKey(key); }

        @Override
        public void removeData(@NotNull String key) { data.remove(key); }

        @Override
        public void clearData() { data.clear(); }
    }
}
