/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.scheduler.execution.ExecutionContext;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fluent API for chaining multiple tasks together with different execution contexts.
 *
 * <p>TaskChain allows you to compose complex workflows that involve switching
 * between sync and async execution, processing data sequentially, and handling
 * errors gracefully.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Load data async, process sync, save async
 * scheduler.chain()
 *     .async(() -> database.loadPlayerData(uuid))  // Returns PlayerData
 *     .sync(data -> {
 *         player.setData(data);
 *         return player.getInventory();
 *     })
 *     .async(inventory -> {
 *         cache.store(uuid, inventory);
 *         return null;
 *     })
 *     .onError(error -> log.error("Failed to load player", error))
 *     .execute();
 *
 * // Simple sync-async-sync chain
 * scheduler.chain()
 *     .sync(() -> prepareData())
 *     .delay(1, TimeUnit.SECONDS)
 *     .async(() -> sendToServer())
 *     .sync(() -> updateUI())
 *     .execute();
 *
 * // With entity binding for Folia
 * scheduler.chain()
 *     .async(() -> database.loadItems())
 *     .atPlayer(player, items -> {
 *         player.giveItems(items);
 *         return items.size();
 *     })
 *     .sync(count -> {
 *         broadcast("Player received " + count + " items");
 *     })
 *     .execute();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>TaskChain is not thread-safe during construction. Each chain should
 * be built and executed from a single thread. The execution itself handles
 * thread transitions safely.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchedulerService#chain()
 */
public interface TaskChain {

    // ==================== Sync Execution ====================

    /**
     * Adds a synchronous task to the chain.
     *
     * @param runnable the task to run
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain sync(@NotNull Runnable runnable);

    /**
     * Adds a synchronous supplier task to the chain.
     *
     * <p>The result is passed to the next step in the chain.
     *
     * @param <T>      the result type
     * @param supplier the task that produces a value
     * @return a typed chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    <T> TaskChainWithData<T> sync(@NotNull Supplier<T> supplier);

    /**
     * Adds a synchronous consumer task that receives the previous result.
     *
     * @param <T>      the input type
     * @param consumer the task that consumes a value
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    <T> TaskChain syncConsume(@NotNull Consumer<T> consumer);

    // ==================== Async Execution ====================

    /**
     * Adds an asynchronous task to the chain.
     *
     * @param runnable the task to run
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain async(@NotNull Runnable runnable);

    /**
     * Adds an asynchronous supplier task to the chain.
     *
     * @param <T>      the result type
     * @param supplier the task that produces a value
     * @return a typed chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    <T> TaskChainWithData<T> async(@NotNull Supplier<T> supplier);

    /**
     * Adds an asynchronous consumer task that receives the previous result.
     *
     * @param <T>      the input type
     * @param consumer the task that consumes a value
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    <T> TaskChain asyncConsume(@NotNull Consumer<T> consumer);

    // ==================== Entity/Location Binding ====================

    /**
     * Adds a task that runs on the entity's owning thread.
     *
     * @param entity   the entity to bind to
     * @param runnable the task to run
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain atEntity(@NotNull Object entity, @NotNull Runnable runnable);

    /**
     * Adds a task that runs on the player's owning thread.
     *
     * @param player   the player to bind to
     * @param runnable the task to run
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain atPlayer(@NotNull UnifiedPlayer player, @NotNull Runnable runnable);

    /**
     * Adds a supplier task that runs on the player's owning thread.
     *
     * @param <T>      the result type
     * @param player   the player to bind to
     * @param supplier the task that produces a value
     * @return a typed chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    <T> TaskChainWithData<T> atPlayer(@NotNull UnifiedPlayer player, @NotNull Supplier<T> supplier);

    /**
     * Adds a task that runs on the location's owning thread.
     *
     * @param location the location to bind to
     * @param runnable the task to run
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain atLocation(@NotNull UnifiedLocation location, @NotNull Runnable runnable);

    /**
     * Adds a supplier task that runs on the location's owning thread.
     *
     * @param <T>      the result type
     * @param location the location to bind to
     * @param supplier the task that produces a value
     * @return a typed chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    <T> TaskChainWithData<T> atLocation(@NotNull UnifiedLocation location, @NotNull Supplier<T> supplier);

    // ==================== Timing ====================

    /**
     * Adds a delay before the next task in the chain.
     *
     * @param ticks the delay in server ticks
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain delay(long ticks);

    /**
     * Adds a delay before the next task in the chain.
     *
     * @param duration the delay duration
     * @param unit     the time unit
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain delay(long duration, @NotNull TimeUnit unit);

    /**
     * Adds a delay before the next task in the chain.
     *
     * @param duration the delay duration
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain delay(@NotNull Duration duration);

    // ==================== Error Handling ====================

    /**
     * Sets the error handler for this chain.
     *
     * <p>If any task in the chain throws an exception, execution stops
     * and the error handler is called. Without an error handler, exceptions
     * are logged and the chain aborts silently.
     *
     * @param errorHandler the exception handler
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain onError(@NotNull Consumer<Throwable> errorHandler);

    /**
     * Sets the completion callback for this chain.
     *
     * <p>Called after all tasks complete successfully. Not called if
     * the chain is aborted due to an error.
     *
     * @param callback the completion callback
     * @return this chain for further configuration
     * @since 1.0.0
     */
    @NotNull
    TaskChain onComplete(@NotNull Runnable callback);

    // ==================== Execution ====================

    /**
     * Executes the task chain.
     *
     * @return a handle that can be used to cancel the chain
     * @since 1.0.0
     */
    @NotNull
    TaskHandle execute();

    /**
     * Executes the task chain and returns a future.
     *
     * @return a future that completes when the chain finishes
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> executeAsync();

    // ==================== Typed Chain with Data ====================

    /**
     * A task chain that carries data from one step to the next.
     *
     * @param <T> the data type
     * @since 1.0.0
     */
    interface TaskChainWithData<T> {

        /**
         * Adds a synchronous task that transforms the data.
         *
         * @param <R>      the result type
         * @param function the transformation function
         * @return a typed chain with the new data type
         * @since 1.0.0
         */
        @NotNull
        <R> TaskChainWithData<R> sync(@NotNull Function<T, R> function);

        /**
         * Adds a synchronous task that consumes the data.
         *
         * @param consumer the consumer
         * @return the untyped chain for further configuration
         * @since 1.0.0
         */
        @NotNull
        TaskChain syncConsume(@NotNull Consumer<T> consumer);

        /**
         * Adds an asynchronous task that transforms the data.
         *
         * @param <R>      the result type
         * @param function the transformation function
         * @return a typed chain with the new data type
         * @since 1.0.0
         */
        @NotNull
        <R> TaskChainWithData<R> async(@NotNull Function<T, R> function);

        /**
         * Adds an asynchronous task that consumes the data.
         *
         * @param consumer the consumer
         * @return the untyped chain for further configuration
         * @since 1.0.0
         */
        @NotNull
        TaskChain asyncConsume(@NotNull Consumer<T> consumer);

        /**
         * Adds a task on the player's thread that transforms the data.
         *
         * @param <R>      the result type
         * @param player   the player to bind to
         * @param function the transformation function
         * @return a typed chain with the new data type
         * @since 1.0.0
         */
        @NotNull
        <R> TaskChainWithData<R> atPlayer(@NotNull UnifiedPlayer player, @NotNull Function<T, R> function);

        /**
         * Adds a task on the player's thread that consumes the data.
         *
         * @param player   the player to bind to
         * @param consumer the consumer
         * @return the untyped chain for further configuration
         * @since 1.0.0
         */
        @NotNull
        TaskChain atPlayerConsume(@NotNull UnifiedPlayer player, @NotNull Consumer<T> consumer);

        /**
         * Adds a task on the location's thread that transforms the data.
         *
         * @param <R>      the result type
         * @param location the location to bind to
         * @param function the transformation function
         * @return a typed chain with the new data type
         * @since 1.0.0
         */
        @NotNull
        <R> TaskChainWithData<R> atLocation(@NotNull UnifiedLocation location, @NotNull Function<T, R> function);

        /**
         * Adds a task on the location's thread that consumes the data.
         *
         * @param location the location to bind to
         * @param consumer the consumer
         * @return the untyped chain for further configuration
         * @since 1.0.0
         */
        @NotNull
        TaskChain atLocationConsume(@NotNull UnifiedLocation location, @NotNull Consumer<T> consumer);

        /**
         * Adds a delay before the next task.
         *
         * @param ticks the delay in ticks
         * @return this chain
         * @since 1.0.0
         */
        @NotNull
        TaskChainWithData<T> delay(long ticks);

        /**
         * Adds a delay before the next task.
         *
         * @param duration the delay duration
         * @param unit     the time unit
         * @return this chain
         * @since 1.0.0
         */
        @NotNull
        TaskChainWithData<T> delay(long duration, @NotNull TimeUnit unit);

        /**
         * Adds a delay before the next task.
         *
         * @param duration the delay duration
         * @return this chain
         * @since 1.0.0
         */
        @NotNull
        TaskChainWithData<T> delay(@NotNull Duration duration);

        /**
         * Sets the error handler for this chain.
         *
         * @param errorHandler the exception handler
         * @return this chain
         * @since 1.0.0
         */
        @NotNull
        TaskChainWithData<T> onError(@NotNull Consumer<Throwable> errorHandler);

        /**
         * Sets the completion callback for this chain.
         *
         * @param callback the completion callback receiving the final value
         * @return this chain
         * @since 1.0.0
         */
        @NotNull
        TaskChainWithData<T> onComplete(@NotNull Consumer<T> callback);

        /**
         * Executes the task chain.
         *
         * @return a handle that can be used to cancel the chain
         * @since 1.0.0
         */
        @NotNull
        TaskHandle execute();

        /**
         * Executes the task chain and returns a future with the result.
         *
         * @return a future that completes with the final value
         * @since 1.0.0
         */
        @NotNull
        CompletableFuture<T> executeAsync();
    }
}
