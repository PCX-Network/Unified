/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Functional interface for asynchronously loading items for paginated GUIs.
 *
 * <p>AsyncItemLoader enables loading data from slow sources (databases, APIs,
 * file systems) without blocking the main server thread. Items are loaded
 * in the background and the GUI is updated when loading completes.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple async loader from database
 * AsyncItemLoader<PlayerData> dbLoader = () ->
 *     CompletableFuture.supplyAsync(() -> {
 *         return database.getAllPlayerData();
 *     });
 *
 * // Loader with custom executor
 * Executor ioExecutor = Executors.newFixedThreadPool(4);
 * AsyncItemLoader<ShopItem> shopLoader = AsyncItemLoader.withExecutor(
 *     () -> shopDatabase.getItems(),
 *     ioExecutor
 * );
 *
 * // Loader with progress tracking
 * AsyncItemLoader<Quest> questLoader = AsyncItemLoader.withProgress(
 *     () -> {
 *         List<Quest> quests = new ArrayList<>();
 *         for (String questId : questIds) {
 *             quests.add(loadQuest(questId));
 *             progress.increment();
 *         }
 *         return quests;
 *     },
 *     progressConsumer
 * );
 *
 * // Use with paginated GUI
 * paginatedGui.setAsyncLoader(dbLoader);
 * paginatedGui.loadAsync().thenRun(() -> {
 *     player.sendMessage("Items loaded!");
 * });
 *
 * // Cached loader that only loads once
 * AsyncItemLoader<Item> cachedLoader = AsyncItemLoader.cached(expensiveLoader);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe. The returned CompletableFuture
 * should complete with the loaded items on any thread - the GUI framework
 * will handle scheduling the update on the appropriate thread.
 *
 * @param <T> the type of items to load
 * @since 1.0.0
 * @author Supatuck
 * @see PaginatedGUI
 * @see LoadingIndicator
 */
@FunctionalInterface
public interface AsyncItemLoader<T> {

    /**
     * Loads items asynchronously.
     *
     * <p>This method should return immediately with a CompletableFuture
     * that will be completed when the items are loaded. The actual loading
     * should happen on a background thread.
     *
     * <p>If loading fails, the returned future should complete exceptionally
     * with an appropriate exception.
     *
     * @return a CompletableFuture that completes with the loaded items
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<T>> load();

    /**
     * Creates an AsyncItemLoader from a synchronous supplier.
     *
     * <p>The supplier will be executed asynchronously using the common
     * ForkJoinPool.
     *
     * @param <T>      the item type
     * @param supplier the synchronous supplier function
     * @return an AsyncItemLoader that runs the supplier asynchronously
     * @since 1.0.0
     */
    @NotNull
    static <T> AsyncItemLoader<T> fromSupplier(@NotNull Supplier<Collection<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return () -> CompletableFuture.supplyAsync(supplier);
    }

    /**
     * Creates an AsyncItemLoader that runs on a specific executor.
     *
     * @param <T>      the item type
     * @param supplier the synchronous supplier function
     * @param executor the executor to use for loading
     * @return an AsyncItemLoader that runs on the specified executor
     * @since 1.0.0
     */
    @NotNull
    static <T> AsyncItemLoader<T> withExecutor(@NotNull Supplier<Collection<T>> supplier,
                                                @NotNull Executor executor) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        return () -> CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * Creates an AsyncItemLoader that caches the result after first load.
     *
     * <p>Subsequent calls to {@link #load()} will return the cached result
     * immediately without invoking the underlying loader.
     *
     * @param <T>    the item type
     * @param loader the underlying loader
     * @return a caching AsyncItemLoader
     * @since 1.0.0
     */
    @NotNull
    static <T> AsyncItemLoader<T> cached(@NotNull AsyncItemLoader<T> loader) {
        Objects.requireNonNull(loader, "loader cannot be null");
        return new CachedAsyncItemLoader<>(loader);
    }

    /**
     * Creates an AsyncItemLoader that retries on failure.
     *
     * @param <T>        the item type
     * @param loader     the underlying loader
     * @param maxRetries the maximum number of retry attempts
     * @param delayMs    the delay between retries in milliseconds
     * @return a retrying AsyncItemLoader
     * @since 1.0.0
     */
    @NotNull
    static <T> AsyncItemLoader<T> withRetry(@NotNull AsyncItemLoader<T> loader,
                                             int maxRetries,
                                             long delayMs) {
        Objects.requireNonNull(loader, "loader cannot be null");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative");
        }
        return new RetryingAsyncItemLoader<>(loader, maxRetries, delayMs);
    }

    /**
     * Creates an AsyncItemLoader with a timeout.
     *
     * @param <T>       the item type
     * @param loader    the underlying loader
     * @param timeoutMs the timeout in milliseconds
     * @return a timeout-aware AsyncItemLoader
     * @since 1.0.0
     */
    @NotNull
    static <T> AsyncItemLoader<T> withTimeout(@NotNull AsyncItemLoader<T> loader,
                                               long timeoutMs) {
        Objects.requireNonNull(loader, "loader cannot be null");
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be positive");
        }
        return () -> loader.load().orTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Combines this loader with a transformation applied to the results.
     *
     * @param <U>         the transformed item type
     * @param transformer the transformation function
     * @return a transformed AsyncItemLoader
     * @since 1.0.0
     */
    @NotNull
    default <U> AsyncItemLoader<U> map(
            @NotNull java.util.function.Function<Collection<T>, Collection<U>> transformer) {
        Objects.requireNonNull(transformer, "transformer cannot be null");
        return () -> load().thenApply(transformer);
    }

    /**
     * Combines this loader with a filter applied to the results.
     *
     * @param filter the filter predicate
     * @return a filtered AsyncItemLoader
     * @since 1.0.0
     */
    @NotNull
    default AsyncItemLoader<T> filter(@NotNull java.util.function.Predicate<T> filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        return () -> load().thenApply(items ->
                items.stream()
                        .filter(filter)
                        .collect(java.util.stream.Collectors.toList())
        );
    }

    /**
     * Chains this loader with another loader, combining their results.
     *
     * @param other the other loader
     * @return a combined AsyncItemLoader
     * @since 1.0.0
     */
    @NotNull
    default AsyncItemLoader<T> andThen(@NotNull AsyncItemLoader<T> other) {
        Objects.requireNonNull(other, "other cannot be null");
        return () -> load().thenCombine(other.load(), (items1, items2) -> {
            java.util.List<T> combined = new java.util.ArrayList<>(items1);
            combined.addAll(items2);
            return combined;
        });
    }

    /**
     * Adds a callback that is invoked when loading starts.
     *
     * @param callback the callback to invoke
     * @return an AsyncItemLoader that invokes the callback
     * @since 1.0.0
     */
    @NotNull
    default AsyncItemLoader<T> onStart(@NotNull Runnable callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        return () -> {
            callback.run();
            return load();
        };
    }

    /**
     * Adds a callback that is invoked when loading completes.
     *
     * @param callback the callback to invoke with the loaded items
     * @return an AsyncItemLoader that invokes the callback
     * @since 1.0.0
     */
    @NotNull
    default AsyncItemLoader<T> onComplete(
            @NotNull java.util.function.Consumer<Collection<T>> callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        return () -> load().whenComplete((items, error) -> {
            if (items != null) {
                callback.accept(items);
            }
        });
    }

    /**
     * Adds an error handler that is invoked when loading fails.
     *
     * @param handler the error handler
     * @return an AsyncItemLoader that handles errors
     * @since 1.0.0
     */
    @NotNull
    default AsyncItemLoader<T> onError(
            @NotNull java.util.function.Consumer<Throwable> handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        return () -> load().whenComplete((items, error) -> {
            if (error != null) {
                handler.accept(error);
            }
        });
    }
}

/**
 * Internal implementation of cached async loader.
 */
class CachedAsyncItemLoader<T> implements AsyncItemLoader<T> {

    private final AsyncItemLoader<T> delegate;
    private volatile CompletableFuture<Collection<T>> cachedFuture;
    private volatile Collection<T> cachedResult;

    CachedAsyncItemLoader(AsyncItemLoader<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<T>> load() {
        if (cachedResult != null) {
            return CompletableFuture.completedFuture(cachedResult);
        }

        synchronized (this) {
            if (cachedFuture != null) {
                return cachedFuture;
            }

            cachedFuture = delegate.load().thenApply(items -> {
                cachedResult = items;
                return items;
            });

            return cachedFuture;
        }
    }

    /**
     * Clears the cache, allowing a fresh load on next invocation.
     */
    public void invalidate() {
        synchronized (this) {
            cachedFuture = null;
            cachedResult = null;
        }
    }
}

/**
 * Internal implementation of retrying async loader.
 */
class RetryingAsyncItemLoader<T> implements AsyncItemLoader<T> {

    private final AsyncItemLoader<T> delegate;
    private final int maxRetries;
    private final long delayMs;

    RetryingAsyncItemLoader(AsyncItemLoader<T> delegate, int maxRetries, long delayMs) {
        this.delegate = delegate;
        this.maxRetries = maxRetries;
        this.delayMs = delayMs;
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<T>> load() {
        return loadWithRetry(0);
    }

    private CompletableFuture<Collection<T>> loadWithRetry(int attempt) {
        return delegate.load().exceptionallyCompose(error -> {
            if (attempt >= maxRetries) {
                return CompletableFuture.failedFuture(error);
            }

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }).thenCompose(ignored -> loadWithRetry(attempt + 1));
        });
    }
}
