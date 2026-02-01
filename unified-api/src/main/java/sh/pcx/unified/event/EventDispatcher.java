/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * Dispatches events to registered handlers.
 *
 * <p>The event dispatcher is responsible for the actual invocation of event
 * handlers. It handles priority ordering, cancellation checks, filter evaluation,
 * async execution, and error handling.
 *
 * <h2>Dispatch Process</h2>
 * <ol>
 *   <li>Retrieve handlers from the registry in priority order</li>
 *   <li>For each handler:
 *     <ol>
 *       <li>Check if event is cancelled (if ignoreCancelled is true)</li>
 *       <li>Evaluate filter conditions</li>
 *       <li>Execute handler (sync or async based on configuration)</li>
 *       <li>Handle any exceptions</li>
 *     </ol>
 *   </li>
 *   <li>Return the processed event</li>
 * </ol>
 *
 * <h2>Error Handling</h2>
 * <p>By default, exceptions from handlers are logged but do not prevent
 * subsequent handlers from executing. This can be customized through the
 * exception handler.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * EventDispatcher dispatcher = eventBus.getDispatcher();
 *
 * // Dispatch with custom exception handling
 * dispatcher.dispatch(event, (handler, exception) -> {
 *     logger.error("Handler failed: " + handler.getMethod().getName(), exception);
 * });
 *
 * // Check dispatch statistics
 * DispatchStats stats = dispatcher.getStats();
 * logger.info("Total dispatches: " + stats.getTotalDispatches());
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventBus
 * @see EventRegistry
 */
public interface EventDispatcher {

    /**
     * Dispatches an event to all registered handlers.
     *
     * <p>Handlers are called in priority order. Sync handlers block until
     * complete, while async handlers may complete after this method returns.
     *
     * @param event the event to dispatch
     * @param <T>   the event type
     * @return the event after all sync handlers have processed it
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> T dispatch(@NotNull T event);

    /**
     * Dispatches an event with a custom exception handler.
     *
     * @param event            the event to dispatch
     * @param exceptionHandler handler for exceptions from event handlers
     * @param <T>              the event type
     * @return the event after all sync handlers have processed it
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> T dispatch(
            @NotNull T event,
            @Nullable BiConsumer<EventRegistry.RegisteredHandler, Throwable> exceptionHandler
    );

    /**
     * Dispatches an event asynchronously.
     *
     * <p>The event is dispatched on an async thread. The returned future
     * completes when all handlers (including async ones) have finished.
     *
     * @param event the event to dispatch
     * @param <T>   the event type
     * @return a future that completes with the processed event
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> CompletableFuture<T> dispatchAsync(@NotNull T event);

    /**
     * Dispatches an event asynchronously with a custom executor.
     *
     * @param event    the event to dispatch
     * @param executor the executor to use for dispatch
     * @param <T>      the event type
     * @return a future that completes with the processed event
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> CompletableFuture<T> dispatchAsync(
            @NotNull T event,
            @NotNull Executor executor
    );

    /**
     * Dispatches an event and waits for all handlers including async ones.
     *
     * <p>Unlike {@link #dispatch(UnifiedEvent)}, this method blocks until
     * all handlers have completed, including async handlers.
     *
     * @param event the event to dispatch
     * @param <T>   the event type
     * @return the event after all handlers have completed
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> T dispatchAndAwait(@NotNull T event);

    /**
     * Sets the default exception handler for this dispatcher.
     *
     * <p>This handler is called when an event handler throws an exception
     * and no custom exception handler is provided.
     *
     * @param handler the exception handler
     * @since 1.0.0
     */
    void setDefaultExceptionHandler(
            @NotNull BiConsumer<EventRegistry.RegisteredHandler, Throwable> handler
    );

    /**
     * Returns the default async executor.
     *
     * @return the async executor
     * @since 1.0.0
     */
    @NotNull
    Executor getAsyncExecutor();

    /**
     * Sets the default async executor.
     *
     * @param executor the executor for async handlers
     * @since 1.0.0
     */
    void setAsyncExecutor(@NotNull Executor executor);

    /**
     * Returns a named executor for async handlers.
     *
     * @param name the executor name
     * @return the executor, or null if not found
     * @since 1.0.0
     */
    @Nullable
    Executor getExecutor(@NotNull String name);

    /**
     * Registers a named executor for async handlers.
     *
     * @param name     the executor name
     * @param executor the executor
     * @since 1.0.0
     */
    void registerExecutor(@NotNull String name, @NotNull Executor executor);

    /**
     * Returns dispatch statistics.
     *
     * @return the dispatch statistics
     * @since 1.0.0
     */
    @NotNull
    DispatchStats getStats();

    /**
     * Resets dispatch statistics.
     *
     * @since 1.0.0
     */
    void resetStats();

    /**
     * Enables or disables timing collection.
     *
     * <p>When enabled, the dispatcher collects detailed timing information
     * for each handler invocation. This has a small performance overhead.
     *
     * @param enabled true to enable timing
     * @since 1.0.0
     */
    void setTimingEnabled(boolean enabled);

    /**
     * Returns whether timing collection is enabled.
     *
     * @return true if timing is enabled
     * @since 1.0.0
     */
    boolean isTimingEnabled();

    /**
     * Dispatch statistics.
     *
     * @since 1.0.0
     */
    interface DispatchStats {

        /**
         * Returns the total number of events dispatched.
         *
         * @return total dispatch count
         * @since 1.0.0
         */
        long getTotalDispatches();

        /**
         * Returns the total number of handler invocations.
         *
         * @return total handler invocation count
         * @since 1.0.0
         */
        long getTotalHandlerInvocations();

        /**
         * Returns the number of handler exceptions.
         *
         * @return exception count
         * @since 1.0.0
         */
        long getExceptionCount();

        /**
         * Returns the number of cancelled events.
         *
         * @return cancelled event count
         * @since 1.0.0
         */
        long getCancelledCount();

        /**
         * Returns the number of filtered (skipped) handler calls.
         *
         * @return filtered handler count
         * @since 1.0.0
         */
        long getFilteredCount();

        /**
         * Returns the average dispatch time in nanoseconds.
         *
         * @return average dispatch time
         * @since 1.0.0
         */
        double getAverageDispatchTimeNanos();

        /**
         * Returns the maximum dispatch time in nanoseconds.
         *
         * @return max dispatch time
         * @since 1.0.0
         */
        long getMaxDispatchTimeNanos();

        /**
         * Returns the average handler execution time in nanoseconds.
         *
         * @return average handler time
         * @since 1.0.0
         */
        double getAverageHandlerTimeNanos();

        /**
         * Returns the maximum handler execution time in nanoseconds.
         *
         * @return max handler time
         * @since 1.0.0
         */
        long getMaxHandlerTimeNanos();

        /**
         * Returns the number of async dispatches.
         *
         * @return async dispatch count
         * @since 1.0.0
         */
        long getAsyncDispatchCount();

        /**
         * Returns timing data for a specific event type.
         *
         * @param eventType the event type
         * @return timing data, or null if not collected
         * @since 1.0.0
         */
        @Nullable
        EventTimingData getTimingData(@NotNull Class<? extends UnifiedEvent> eventType);
    }

    /**
     * Timing data for a specific event type.
     *
     * @since 1.0.0
     */
    interface EventTimingData {

        /**
         * Returns the event type.
         *
         * @return the event class
         * @since 1.0.0
         */
        @NotNull
        Class<? extends UnifiedEvent> getEventType();

        /**
         * Returns the number of times this event was dispatched.
         *
         * @return dispatch count
         * @since 1.0.0
         */
        long getDispatchCount();

        /**
         * Returns the total time spent dispatching this event type.
         *
         * @return total time in nanoseconds
         * @since 1.0.0
         */
        long getTotalTimeNanos();

        /**
         * Returns the average time per dispatch.
         *
         * @return average time in nanoseconds
         * @since 1.0.0
         */
        double getAverageTimeNanos();

        /**
         * Returns the minimum dispatch time.
         *
         * @return min time in nanoseconds
         * @since 1.0.0
         */
        long getMinTimeNanos();

        /**
         * Returns the maximum dispatch time.
         *
         * @return max time in nanoseconds
         * @since 1.0.0
         */
        long getMaxTimeNanos();
    }
}
