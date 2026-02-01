/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.async;

import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.event.UnifiedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A future representing the result of an async event dispatch.
 *
 * <p>EventFuture provides a rich API for handling async event results, including:
 * <ul>
 *   <li>Chaining transformations and handlers</li>
 *   <li>Handling success, failure, and cancellation</li>
 *   <li>Timeout support</li>
 *   <li>Conditional execution based on event state</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * EventFuture<MyEvent> future = eventBus.fireAsync(new MyEvent());
 *
 * // Handle result
 * future.onSuccess(event -> {
 *     processResult(event);
 * }).onError(error -> {
 *     logger.error("Event failed", error);
 * }).onCancelled(() -> {
 *     logger.info("Event was cancelled");
 * });
 * }</pre>
 *
 * <h2>Chaining</h2>
 * <pre>{@code
 * eventBus.fireAsync(new LoadDataEvent(playerId))
 *     .thenApply(event -> event.getData())
 *     .thenAccept(data -> {
 *         useData(data);
 *     });
 * }</pre>
 *
 * <h2>Combining Futures</h2>
 * <pre>{@code
 * EventFuture<EventA> futureA = eventBus.fireAsync(new EventA());
 * EventFuture<EventB> futureB = eventBus.fireAsync(new EventB());
 *
 * EventFuture.allOf(futureA, futureB).thenRun(() -> {
 *     // Both events completed
 * });
 * }</pre>
 *
 * <h2>Timeout</h2>
 * <pre>{@code
 * future.withTimeout(5, TimeUnit.SECONDS)
 *     .onSuccess(event -> {
 *         // Completed in time
 *     })
 *     .onError(error -> {
 *         if (error instanceof TimeoutException) {
 *             // Timed out
 *         }
 *     });
 * }</pre>
 *
 * @param <T> the event type
 * @since 1.0.0
 * @author Supatuck
 * @see AsyncEvent
 * @see AsyncEventHandler
 */
public class EventFuture<T extends UnifiedEvent> implements CompletionStage<T> {

    private final CompletableFuture<T> delegate;
    private final T event;

    /**
     * Constructs a new event future.
     *
     * @param event    the event
     * @param delegate the backing future
     */
    private EventFuture(@NotNull T event, @NotNull CompletableFuture<T> delegate) {
        this.event = Objects.requireNonNull(event, "event cannot be null");
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    /**
     * Creates an event future from an event.
     *
     * <p>The returned future is already completed with the event.
     *
     * @param event the event
     * @param <T>   the event type
     * @return a completed event future
     * @since 1.0.0
     */
    @NotNull
    public static <T extends UnifiedEvent> EventFuture<T> of(@NotNull T event) {
        return new EventFuture<>(event, CompletableFuture.completedFuture(event));
    }

    /**
     * Creates an event future from an event and a backing future.
     *
     * @param event    the event
     * @param delegate the backing future
     * @param <T>      the event type
     * @return an event future
     * @since 1.0.0
     */
    @NotNull
    public static <T extends UnifiedEvent> EventFuture<T> of(
            @NotNull T event,
            @NotNull CompletableFuture<T> delegate
    ) {
        return new EventFuture<>(event, delegate);
    }

    /**
     * Creates a failed event future.
     *
     * @param event the event
     * @param error the error
     * @param <T>   the event type
     * @return a failed event future
     * @since 1.0.0
     */
    @NotNull
    public static <T extends UnifiedEvent> EventFuture<T> failed(
            @NotNull T event,
            @NotNull Throwable error
    ) {
        return new EventFuture<>(event, CompletableFuture.failedFuture(error));
    }

    /**
     * Creates a future that completes when all provided futures complete.
     *
     * @param futures the futures to wait for
     * @return a future that completes when all are done
     * @since 1.0.0
     */
    @NotNull
    public static CompletableFuture<Void> allOf(@NotNull EventFuture<?>... futures) {
        CompletableFuture<?>[] delegates = new CompletableFuture[futures.length];
        for (int i = 0; i < futures.length; i++) {
            delegates[i] = futures[i].delegate;
        }
        return CompletableFuture.allOf(delegates);
    }

    /**
     * Creates a future that completes when any of the provided futures complete.
     *
     * @param futures the futures to wait for
     * @return a future that completes when any is done
     * @since 1.0.0
     */
    @NotNull
    public static CompletableFuture<Object> anyOf(@NotNull EventFuture<?>... futures) {
        CompletableFuture<?>[] delegates = new CompletableFuture[futures.length];
        for (int i = 0; i < futures.length; i++) {
            delegates[i] = futures[i].delegate;
        }
        return CompletableFuture.anyOf(delegates);
    }

    /**
     * Returns the event associated with this future.
     *
     * @return the event
     * @since 1.0.0
     */
    @NotNull
    public T getEvent() {
        return event;
    }

    /**
     * Returns the backing CompletableFuture.
     *
     * @return the delegate future
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<T> toCompletableFuture() {
        return delegate;
    }

    /**
     * Returns whether the future is completed.
     *
     * @return true if completed
     * @since 1.0.0
     */
    public boolean isDone() {
        return delegate.isDone();
    }

    /**
     * Returns whether the future completed exceptionally.
     *
     * @return true if completed with an error
     * @since 1.0.0
     */
    public boolean isCompletedExceptionally() {
        return delegate.isCompletedExceptionally();
    }

    /**
     * Returns whether the future was cancelled.
     *
     * @return true if cancelled
     * @since 1.0.0
     */
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    /**
     * Returns whether the event was cancelled (if cancellable).
     *
     * @return true if the event is cancellable and was cancelled
     * @since 1.0.0
     */
    public boolean isEventCancelled() {
        return event instanceof Cancellable && ((Cancellable) event).isCancelled();
    }

    /**
     * Blocks and waits for the result.
     *
     * @return the event
     * @throws InterruptedException if interrupted
     * @throws ExecutionException   if the future completed exceptionally
     * @since 1.0.0
     */
    @NotNull
    public T get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    /**
     * Blocks and waits for the result with a timeout.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit
     * @return the event
     * @throws InterruptedException if interrupted
     * @throws ExecutionException   if the future completed exceptionally
     * @throws TimeoutException     if the timeout expired
     * @since 1.0.0
     */
    @NotNull
    public T get(long timeout, @NotNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }

    /**
     * Returns the result immediately or null if not complete.
     *
     * @return the event or null
     * @since 1.0.0
     */
    @Nullable
    public T getNow() {
        return delegate.getNow(null);
    }

    /**
     * Joins and returns the result, throwing unchecked exceptions.
     *
     * @return the event
     * @throws CancellationException if cancelled
     * @throws CompletionException   if completed exceptionally
     * @since 1.0.0
     */
    @NotNull
    public T join() {
        return delegate.join();
    }

    /**
     * Adds a handler that runs on successful completion.
     *
     * @param handler the success handler
     * @return this future for chaining
     * @since 1.0.0
     */
    @NotNull
    public EventFuture<T> onSuccess(@NotNull Consumer<T> handler) {
        delegate.thenAccept(handler);
        return this;
    }

    /**
     * Adds a handler that runs on exceptional completion.
     *
     * @param handler the error handler
     * @return this future for chaining
     * @since 1.0.0
     */
    @NotNull
    public EventFuture<T> onError(@NotNull Consumer<Throwable> handler) {
        delegate.exceptionally(error -> {
            handler.accept(error);
            return null;
        });
        return this;
    }

    /**
     * Adds a handler that runs if the event was cancelled.
     *
     * @param handler the cancellation handler
     * @return this future for chaining
     * @since 1.0.0
     */
    @NotNull
    public EventFuture<T> onCancelled(@NotNull Runnable handler) {
        delegate.thenAccept(e -> {
            if (e instanceof Cancellable && ((Cancellable) e).isCancelled()) {
                handler.run();
            }
        });
        return this;
    }

    /**
     * Adds a handler that runs only if the event was not cancelled.
     *
     * @param handler the handler
     * @return this future for chaining
     * @since 1.0.0
     */
    @NotNull
    public EventFuture<T> ifNotCancelled(@NotNull Consumer<T> handler) {
        delegate.thenAccept(e -> {
            if (!(e instanceof Cancellable) || !((Cancellable) e).isCancelled()) {
                handler.accept(e);
            }
        });
        return this;
    }

    /**
     * Adds a handler that runs only if a condition is met.
     *
     * @param condition the condition to check
     * @param handler   the handler
     * @return this future for chaining
     * @since 1.0.0
     */
    @NotNull
    public EventFuture<T> when(@NotNull Predicate<T> condition, @NotNull Consumer<T> handler) {
        delegate.thenAccept(e -> {
            if (condition.test(e)) {
                handler.accept(e);
            }
        });
        return this;
    }

    /**
     * Returns a future that times out if not completed in time.
     *
     * @param timeout the timeout value
     * @param unit    the time unit
     * @return a new future with timeout
     * @since 1.0.0
     */
    @NotNull
    public EventFuture<T> withTimeout(long timeout, @NotNull TimeUnit unit) {
        return new EventFuture<>(event, delegate.orTimeout(timeout, unit));
    }

    // CompletionStage implementation

    @Override
    @NotNull
    public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
        return delegate.thenApply(fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return delegate.thenApplyAsync(fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return delegate.thenApplyAsync(fn, executor);
    }

    @Override
    @NotNull
    public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
        return delegate.thenAccept(action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
        return delegate.thenAcceptAsync(action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return delegate.thenAcceptAsync(action, executor);
    }

    @Override
    @NotNull
    public CompletionStage<Void> thenRun(Runnable action) {
        return delegate.thenRun(action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> thenRunAsync(Runnable action) {
        return delegate.thenRunAsync(action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        return delegate.thenRunAsync(action, executor);
    }

    @Override
    @NotNull
    public <U, V> CompletionStage<V> thenCombine(
            CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return delegate.thenCombine(other, fn);
    }

    @Override
    @NotNull
    public <U, V> CompletionStage<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return delegate.thenCombineAsync(other, fn);
    }

    @Override
    @NotNull
    public <U, V> CompletionStage<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn,
            Executor executor
    ) {
        return delegate.thenCombineAsync(other, fn, executor);
    }

    @Override
    @NotNull
    public <U> CompletionStage<Void> thenAcceptBoth(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action
    ) {
        return delegate.thenAcceptBoth(other, action);
    }

    @Override
    @NotNull
    public <U> CompletionStage<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action
    ) {
        return delegate.thenAcceptBothAsync(other, action);
    }

    @Override
    @NotNull
    public <U> CompletionStage<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action,
            Executor executor
    ) {
        return delegate.thenAcceptBothAsync(other, action, executor);
    }

    @Override
    @NotNull
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return delegate.runAfterBoth(other, action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return delegate.runAfterBothAsync(other, action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> runAfterBothAsync(
            CompletionStage<?> other,
            Runnable action,
            Executor executor
    ) {
        return delegate.runAfterBothAsync(other, action, executor);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> applyToEither(
            CompletionStage<? extends T> other,
            Function<? super T, U> fn
    ) {
        return delegate.applyToEither(other, fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> applyToEitherAsync(
            CompletionStage<? extends T> other,
            Function<? super T, U> fn
    ) {
        return delegate.applyToEitherAsync(other, fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> applyToEitherAsync(
            CompletionStage<? extends T> other,
            Function<? super T, U> fn,
            Executor executor
    ) {
        return delegate.applyToEitherAsync(other, fn, executor);
    }

    @Override
    @NotNull
    public CompletionStage<Void> acceptEither(
            CompletionStage<? extends T> other,
            Consumer<? super T> action
    ) {
        return delegate.acceptEither(other, action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> acceptEitherAsync(
            CompletionStage<? extends T> other,
            Consumer<? super T> action
    ) {
        return delegate.acceptEitherAsync(other, action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> acceptEitherAsync(
            CompletionStage<? extends T> other,
            Consumer<? super T> action,
            Executor executor
    ) {
        return delegate.acceptEitherAsync(other, action, executor);
    }

    @Override
    @NotNull
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return delegate.runAfterEither(other, action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return delegate.runAfterEitherAsync(other, action);
    }

    @Override
    @NotNull
    public CompletionStage<Void> runAfterEitherAsync(
            CompletionStage<?> other,
            Runnable action,
            Executor executor
    ) {
        return delegate.runAfterEitherAsync(other, action, executor);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return delegate.thenCompose(fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return delegate.thenComposeAsync(fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor
    ) {
        return delegate.thenComposeAsync(fn, executor);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return delegate.handle(fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return delegate.handleAsync(fn);
    }

    @Override
    @NotNull
    public <U> CompletionStage<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn,
            Executor executor
    ) {
        return delegate.handleAsync(fn, executor);
    }

    @Override
    @NotNull
    public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return delegate.whenComplete(action);
    }

    @Override
    @NotNull
    public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return delegate.whenCompleteAsync(action);
    }

    @Override
    @NotNull
    public CompletionStage<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action,
            Executor executor
    ) {
        return delegate.whenCompleteAsync(action, executor);
    }

    @Override
    @NotNull
    public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return delegate.exceptionally(fn);
    }

    @Override
    public String toString() {
        return "EventFuture[event=" + event.getEventName()
                + ", done=" + isDone()
                + ", cancelled=" + isCancelled()
                + ", failed=" + isCompletedExceptionally()
                + "]";
    }
}
