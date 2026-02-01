/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an asynchronously computed placeholder value.
 *
 * <p>AsyncPlaceholder wraps placeholder computations that may be slow (database queries,
 * API calls, etc.) and provides mechanisms for handling the async lifecycle including
 * timeouts, fallbacks, and caching of completed results.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create an async placeholder
 * AsyncPlaceholder<String> balance = AsyncPlaceholder.compute(
 *     () -> database.getBalance(playerId),
 *     "Loading..."
 * );
 *
 * // With timeout and fallback
 * AsyncPlaceholder<String> stats = AsyncPlaceholder.builder(() -> fetchStats())
 *     .fallback("N/A")
 *     .timeout(5, TimeUnit.SECONDS)
 *     .cacheResult(CacheTTL.MINUTES_1)
 *     .build();
 *
 * // Usage in placeholder handler
 * @Placeholder(value = "balance", async = true)
 * public CompletableFuture<String> getBalance(UnifiedPlayer player) {
 *     return AsyncPlaceholder.compute(
 *         () -> economy.getBalance(player),
 *         "..."
 *     ).getFuture();
 * }
 *
 * // Wait with fallback
 * String value = async.getOrFallback();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>AsyncPlaceholder is thread-safe and the underlying computation runs on a separate thread.
 *
 * @param <T> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see Placeholder#async()
 * @see PlaceholderService
 */
public final class AsyncPlaceholder<T> {

    private final CompletableFuture<T> future;
    private final T fallback;
    private final long timeoutMs;
    private volatile T cachedResult;
    private volatile boolean completed;

    private AsyncPlaceholder(CompletableFuture<T> future, T fallback, long timeoutMs) {
        this.future = future;
        this.fallback = fallback;
        this.timeoutMs = timeoutMs;
        this.completed = false;

        // Set up completion handler
        future.whenComplete((result, error) -> {
            if (error == null && result != null) {
                this.cachedResult = result;
            }
            this.completed = true;
        });
    }

    /**
     * Creates an async placeholder that computes a value.
     *
     * @param supplier the value supplier
     * @param fallback the fallback value while computing
     * @param <T>      the value type
     * @return a new async placeholder
     */
    @NotNull
    public static <T> AsyncPlaceholder<T> compute(@NotNull Supplier<T> supplier, @Nullable T fallback) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return new AsyncPlaceholder<>(
            CompletableFuture.supplyAsync(supplier),
            fallback,
            5000 // Default 5 second timeout
        );
    }

    /**
     * Creates an async placeholder that computes a value with a custom executor.
     *
     * @param supplier the value supplier
     * @param executor the executor to run on
     * @param fallback the fallback value while computing
     * @param <T>      the value type
     * @return a new async placeholder
     */
    @NotNull
    public static <T> AsyncPlaceholder<T> compute(@NotNull Supplier<T> supplier,
                                                   @NotNull Executor executor,
                                                   @Nullable T fallback) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        return new AsyncPlaceholder<>(
            CompletableFuture.supplyAsync(supplier, executor),
            fallback,
            5000
        );
    }

    /**
     * Creates an async placeholder from an existing future.
     *
     * @param future   the future
     * @param fallback the fallback value
     * @param <T>      the value type
     * @return a new async placeholder
     */
    @NotNull
    public static <T> AsyncPlaceholder<T> fromFuture(@NotNull CompletableFuture<T> future, @Nullable T fallback) {
        Objects.requireNonNull(future, "future cannot be null");
        return new AsyncPlaceholder<>(future, fallback, 5000);
    }

    /**
     * Creates a completed async placeholder with an immediate value.
     *
     * @param value the value
     * @param <T>   the value type
     * @return a completed async placeholder
     */
    @NotNull
    public static <T> AsyncPlaceholder<T> completed(@NotNull T value) {
        Objects.requireNonNull(value, "value cannot be null");
        AsyncPlaceholder<T> placeholder = new AsyncPlaceholder<>(
            CompletableFuture.completedFuture(value),
            value,
            0
        );
        placeholder.cachedResult = value;
        placeholder.completed = true;
        return placeholder;
    }

    /**
     * Creates a new builder for configuring an async placeholder.
     *
     * @param supplier the value supplier
     * @param <T>      the value type
     * @return a new builder
     */
    @NotNull
    public static <T> Builder<T> builder(@NotNull Supplier<T> supplier) {
        return new Builder<>(supplier);
    }

    /**
     * Returns the underlying CompletableFuture.
     *
     * @return the future
     */
    @NotNull
    public CompletableFuture<T> getFuture() {
        return future;
    }

    /**
     * Checks if the computation has completed.
     *
     * @return {@code true} if completed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Checks if the computation completed successfully.
     *
     * @return {@code true} if completed without error
     */
    public boolean isSuccess() {
        return completed && !future.isCompletedExceptionally();
    }

    /**
     * Checks if the computation failed.
     *
     * @return {@code true} if completed with an error
     */
    public boolean isFailed() {
        return future.isCompletedExceptionally();
    }

    /**
     * Returns the result if available, otherwise the fallback.
     *
     * @return the result or fallback
     */
    @Nullable
    public T getOrFallback() {
        if (cachedResult != null) {
            return cachedResult;
        }
        if (completed && !future.isCompletedExceptionally()) {
            return future.getNow(fallback);
        }
        return fallback;
    }

    /**
     * Returns the result, waiting up to the timeout.
     *
     * @return the result or fallback on timeout
     */
    @Nullable
    public T getWithTimeout() {
        if (cachedResult != null) {
            return cachedResult;
        }
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Blocks until the result is available.
     *
     * @return the result
     * @throws RuntimeException if computation fails
     */
    @NotNull
    public T get() {
        try {
            T result = future.get();
            return result != null ? result : fallback;
        } catch (Exception e) {
            if (fallback != null) {
                return fallback;
            }
            throw new RuntimeException("Async placeholder computation failed", e);
        }
    }

    /**
     * Maps the result using the given function.
     *
     * @param mapper the mapping function
     * @param <R>    the new value type
     * @return a new async placeholder with the mapped value
     */
    @NotNull
    public <R> AsyncPlaceholder<R> map(@NotNull Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return new AsyncPlaceholder<>(
            future.thenApply(mapper),
            fallback != null ? mapper.apply(fallback) : null,
            timeoutMs
        );
    }

    /**
     * Flat-maps the result using the given function.
     *
     * @param mapper the mapping function
     * @param <R>    the new value type
     * @return a new async placeholder
     */
    @NotNull
    public <R> AsyncPlaceholder<R> flatMap(@NotNull Function<T, AsyncPlaceholder<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        CompletableFuture<R> newFuture = future.thenCompose(value -> mapper.apply(value).getFuture());
        return new AsyncPlaceholder<>(newFuture, null, timeoutMs);
    }

    /**
     * Registers a callback for when the computation completes.
     *
     * @param callback the callback
     * @return this async placeholder
     */
    @NotNull
    public AsyncPlaceholder<T> onComplete(@NotNull java.util.function.BiConsumer<T, Throwable> callback) {
        future.whenComplete(callback);
        return this;
    }

    /**
     * Registers a callback for successful completion.
     *
     * @param callback the callback
     * @return this async placeholder
     */
    @NotNull
    public AsyncPlaceholder<T> onSuccess(@NotNull java.util.function.Consumer<T> callback) {
        future.thenAccept(callback);
        return this;
    }

    /**
     * Registers a callback for failed completion.
     *
     * @param callback the callback
     * @return this async placeholder
     */
    @NotNull
    public AsyncPlaceholder<T> onError(@NotNull java.util.function.Consumer<Throwable> callback) {
        future.exceptionally(ex -> {
            callback.accept(ex);
            return fallback;
        });
        return this;
    }

    /**
     * Builder for creating {@link AsyncPlaceholder} instances.
     *
     * @param <T> the value type
     * @since 1.0.0
     */
    public static final class Builder<T> {

        private final Supplier<T> supplier;
        private T fallback;
        private long timeoutMs = 5000;
        private Executor executor;
        private CacheTTL cacheTTL;

        private Builder(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * Sets the fallback value.
         *
         * @param fallback the fallback
         * @return this builder
         */
        @NotNull
        public Builder<T> fallback(@Nullable T fallback) {
            this.fallback = fallback;
            return this;
        }

        /**
         * Sets the timeout.
         *
         * @param timeout the timeout value
         * @param unit    the time unit
         * @return this builder
         */
        @NotNull
        public Builder<T> timeout(long timeout, @NotNull TimeUnit unit) {
            this.timeoutMs = unit.toMillis(timeout);
            return this;
        }

        /**
         * Sets the executor for async computation.
         *
         * @param executor the executor
         * @return this builder
         */
        @NotNull
        public Builder<T> executor(@NotNull Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Enables caching of the result.
         *
         * @param ttl the cache TTL
         * @return this builder
         */
        @NotNull
        public Builder<T> cacheResult(@NotNull CacheTTL ttl) {
            this.cacheTTL = ttl;
            return this;
        }

        /**
         * Builds the async placeholder.
         *
         * @return a new AsyncPlaceholder
         */
        @NotNull
        public AsyncPlaceholder<T> build() {
            CompletableFuture<T> future = executor != null
                ? CompletableFuture.supplyAsync(supplier, executor)
                : CompletableFuture.supplyAsync(supplier);

            return new AsyncPlaceholder<>(future, fallback, timeoutMs);
        }
    }
}
