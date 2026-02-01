/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.scheduler;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.scheduler.TaskChain;
import sh.pcx.unified.scheduler.TaskHandle;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Mock implementation of TaskChain for sequential task execution in tests.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class MockTaskChain implements TaskChain {

    private final MockScheduler scheduler;
    private final List<ChainedTask> tasks = new ArrayList<>();
    private Consumer<Throwable> errorHandler;
    private Runnable completionCallback;

    /**
     * Creates a new mock task chain.
     *
     * @param scheduler the mock scheduler
     */
    MockTaskChain(@NotNull MockScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
    }

    @Override
    @NotNull
    public TaskChain sync(@NotNull Runnable runnable) {
        tasks.add(new ChainedTask(runnable, false, 0));
        return this;
    }

    @Override
    @NotNull
    public <T> TaskChainWithData<T> sync(@NotNull Supplier<T> supplier) {
        return new MockTaskChainWithData<>(this, supplier, false);
    }

    @Override
    @NotNull
    public <T> TaskChain syncConsume(@NotNull Consumer<T> consumer) {
        // This is a no-data version, just run it
        tasks.add(new ChainedTask(() -> consumer.accept(null), false, 0));
        return this;
    }

    @Override
    @NotNull
    public TaskChain async(@NotNull Runnable runnable) {
        tasks.add(new ChainedTask(runnable, true, 0));
        return this;
    }

    @Override
    @NotNull
    public <T> TaskChainWithData<T> async(@NotNull Supplier<T> supplier) {
        return new MockTaskChainWithData<>(this, supplier, true);
    }

    @Override
    @NotNull
    public <T> TaskChain asyncConsume(@NotNull Consumer<T> consumer) {
        tasks.add(new ChainedTask(() -> consumer.accept(null), true, 0));
        return this;
    }

    @Override
    @NotNull
    public TaskChain atEntity(@NotNull Object entity, @NotNull Runnable runnable) {
        tasks.add(new ChainedTask(runnable, false, 0));
        return this;
    }

    @Override
    @NotNull
    public TaskChain atPlayer(@NotNull UnifiedPlayer player, @NotNull Runnable runnable) {
        tasks.add(new ChainedTask(runnable, false, 0));
        return this;
    }

    @Override
    @NotNull
    public <T> TaskChainWithData<T> atPlayer(@NotNull UnifiedPlayer player, @NotNull Supplier<T> supplier) {
        return new MockTaskChainWithData<>(this, supplier, false);
    }

    @Override
    @NotNull
    public TaskChain atLocation(@NotNull UnifiedLocation location, @NotNull Runnable runnable) {
        tasks.add(new ChainedTask(runnable, false, 0));
        return this;
    }

    @Override
    @NotNull
    public <T> TaskChainWithData<T> atLocation(@NotNull UnifiedLocation location, @NotNull Supplier<T> supplier) {
        return new MockTaskChainWithData<>(this, supplier, false);
    }

    @Override
    @NotNull
    public TaskChain delay(long ticks) {
        tasks.add(new ChainedTask(() -> {}, false, ticks));
        return this;
    }

    @Override
    @NotNull
    public TaskChain delay(long duration, @NotNull TimeUnit unit) {
        return delay(unit.toMillis(duration) / 50);
    }

    @Override
    @NotNull
    public TaskChain delay(@NotNull Duration duration) {
        return delay(duration.toMillis() / 50);
    }

    @Override
    @NotNull
    public TaskChain onError(@NotNull Consumer<Throwable> errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler);
        return this;
    }

    @Override
    @NotNull
    public TaskChain onComplete(@NotNull Runnable callback) {
        this.completionCallback = callback;
        return this;
    }

    @Override
    @NotNull
    public TaskHandle execute() {
        CompletableFuture<Void> future = executeInternal();
        return new ChainTaskHandle(future);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> executeAsync() {
        return executeInternal();
    }

    void addTask(ChainedTask task) {
        tasks.add(task);
    }

    private CompletableFuture<Void> executeInternal() {
        if (tasks.isEmpty()) {
            if (completionCallback != null) {
                completionCallback.run();
            }
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (ChainedTask task : tasks) {
            future = future.thenCompose(v -> {
                CompletableFuture<Void> taskFuture = new CompletableFuture<>();

                Runnable wrappedTask = () -> {
                    try {
                        task.runnable.run();
                        taskFuture.complete(null);
                    } catch (Throwable t) {
                        if (errorHandler != null) {
                            errorHandler.accept(t);
                        }
                        taskFuture.completeExceptionally(t);
                    }
                };

                if (task.delay > 0) {
                    if (task.async) {
                        scheduler.runTaskLaterAsync(wrappedTask, task.delay);
                    } else {
                        scheduler.runTaskLater(wrappedTask, task.delay);
                    }
                } else {
                    if (task.async) {
                        scheduler.runTaskAsync(wrappedTask);
                    } else {
                        scheduler.runTask(wrappedTask);
                    }
                }

                return taskFuture;
            });
        }

        if (completionCallback != null) {
            future = future.thenRun(completionCallback);
        }

        return future;
    }

    /**
     * Represents a single task in the chain.
     */
    record ChainedTask(Runnable runnable, boolean async, long delay) {}

    /**
     * Mock implementation of TaskChainWithData.
     */
    private static final class MockTaskChainWithData<T> implements TaskChainWithData<T> {
        private final MockTaskChain chain;
        private final Supplier<T> supplier;
        private final boolean async;
        private T currentValue;
        private Consumer<Throwable> errorHandler;
        private Consumer<T> completionCallback;

        MockTaskChainWithData(MockTaskChain chain, Supplier<T> supplier, boolean async) {
            this.chain = chain;
            this.supplier = supplier;
            this.async = async;
        }

        @Override
        @NotNull
        public <R> TaskChainWithData<R> sync(@NotNull java.util.function.Function<T, R> function) {
            chain.addTask(new ChainedTask(() -> currentValue = supplier.get(), async, 0));
            return new MockTaskChainWithData<>(chain, () -> function.apply(currentValue), false);
        }

        @Override
        @NotNull
        public TaskChain syncConsume(@NotNull Consumer<T> consumer) {
            chain.addTask(new ChainedTask(() -> consumer.accept(supplier.get()), false, 0));
            return chain;
        }

        @Override
        @NotNull
        public <R> TaskChainWithData<R> async(@NotNull java.util.function.Function<T, R> function) {
            chain.addTask(new ChainedTask(() -> currentValue = supplier.get(), async, 0));
            return new MockTaskChainWithData<>(chain, () -> function.apply(currentValue), true);
        }

        @Override
        @NotNull
        public TaskChain asyncConsume(@NotNull Consumer<T> consumer) {
            chain.addTask(new ChainedTask(() -> consumer.accept(supplier.get()), true, 0));
            return chain;
        }

        @Override
        @NotNull
        public <R> TaskChainWithData<R> atPlayer(@NotNull UnifiedPlayer player, @NotNull java.util.function.Function<T, R> function) {
            chain.addTask(new ChainedTask(() -> currentValue = supplier.get(), false, 0));
            return new MockTaskChainWithData<>(chain, () -> function.apply(currentValue), false);
        }

        @Override
        @NotNull
        public TaskChain atPlayerConsume(@NotNull UnifiedPlayer player, @NotNull Consumer<T> consumer) {
            chain.addTask(new ChainedTask(() -> consumer.accept(supplier.get()), false, 0));
            return chain;
        }

        @Override
        @NotNull
        public <R> TaskChainWithData<R> atLocation(@NotNull UnifiedLocation location, @NotNull java.util.function.Function<T, R> function) {
            chain.addTask(new ChainedTask(() -> currentValue = supplier.get(), false, 0));
            return new MockTaskChainWithData<>(chain, () -> function.apply(currentValue), false);
        }

        @Override
        @NotNull
        public TaskChain atLocationConsume(@NotNull UnifiedLocation location, @NotNull Consumer<T> consumer) {
            chain.addTask(new ChainedTask(() -> consumer.accept(supplier.get()), false, 0));
            return chain;
        }

        @Override
        @NotNull
        public TaskChainWithData<T> delay(long ticks) {
            chain.addTask(new ChainedTask(() -> {}, false, ticks));
            return this;
        }

        @Override
        @NotNull
        public TaskChainWithData<T> delay(long duration, @NotNull TimeUnit unit) {
            return delay(unit.toMillis(duration) / 50);
        }

        @Override
        @NotNull
        public TaskChainWithData<T> delay(@NotNull Duration duration) {
            return delay(duration.toMillis() / 50);
        }

        @Override
        @NotNull
        public TaskChainWithData<T> onError(@NotNull Consumer<Throwable> errorHandler) {
            this.errorHandler = errorHandler;
            chain.onError(errorHandler);
            return this;
        }

        @Override
        @NotNull
        public TaskChainWithData<T> onComplete(@NotNull Consumer<T> callback) {
            this.completionCallback = callback;
            return this;
        }

        @Override
        @NotNull
        public TaskHandle execute() {
            chain.addTask(new ChainedTask(() -> {
                T value = supplier.get();
                if (completionCallback != null) {
                    completionCallback.accept(value);
                }
            }, async, 0));
            return chain.execute();
        }

        @Override
        @NotNull
        public CompletableFuture<T> executeAsync() {
            CompletableFuture<T> result = new CompletableFuture<>();
            chain.addTask(new ChainedTask(() -> {
                T value = supplier.get();
                if (completionCallback != null) {
                    completionCallback.accept(value);
                }
                result.complete(value);
            }, async, 0));
            chain.executeAsync();
            return result;
        }
    }

    /**
     * TaskHandle wrapper for chain execution.
     */
    private static final class ChainTaskHandle implements TaskHandle {
        private final CompletableFuture<Void> future;
        private final java.util.UUID id = java.util.UUID.randomUUID();
        private final java.time.Instant createdAt = java.time.Instant.now();
        private volatile boolean cancelled = false;

        ChainTaskHandle(CompletableFuture<Void> future) {
            this.future = future;
        }

        @Override
        @NotNull
        public java.util.UUID getTaskId() {
            return id;
        }

        @Override
        @NotNull
        public sh.pcx.unified.scheduler.Task getTask() {
            throw new UnsupportedOperationException("Chain does not have a single task");
        }

        @Override
        public boolean cancel() {
            cancelled = true;
            return future.cancel(false);
        }

        @Override
        public boolean isActive() {
            return !future.isDone() && !cancelled;
        }

        @Override
        public boolean isCancelled() {
            return cancelled || future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        @NotNull
        public sh.pcx.unified.scheduler.Task.TaskState getState() {
            if (cancelled) return sh.pcx.unified.scheduler.Task.TaskState.CANCELLED;
            if (future.isDone()) return sh.pcx.unified.scheduler.Task.TaskState.COMPLETED;
            return sh.pcx.unified.scheduler.Task.TaskState.RUNNING;
        }

        @Override
        @NotNull
        public sh.pcx.unified.scheduler.Task.TaskType getType() {
            return sh.pcx.unified.scheduler.Task.TaskType.SYNC;
        }

        @Override
        public long getExecutionCount() {
            return future.isDone() ? 1 : 0;
        }

        @Override
        @NotNull
        public java.time.Instant getScheduledAt() {
            return createdAt;
        }

        @Override
        @NotNull
        public java.util.Optional<java.time.Instant> getLastExecutedAt() {
            return future.isDone() ? java.util.Optional.of(java.time.Instant.now()) : java.util.Optional.empty();
        }

        @Override
        @NotNull
        public java.util.Optional<java.time.Instant> getNextExecutionAt() {
            return java.util.Optional.empty();
        }

        @Override
        @NotNull
        public java.time.Duration getTotalExecutionTime() {
            return java.time.Duration.ZERO;
        }

        @Override
        @NotNull
        public java.time.Duration getAverageExecutionTime() {
            return java.time.Duration.ZERO;
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
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }

        @Override
        public boolean await(@NotNull java.time.Duration timeout) throws InterruptedException {
            try {
                future.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                return true;
            } catch (java.util.concurrent.TimeoutException e) {
                return false;
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }
}
