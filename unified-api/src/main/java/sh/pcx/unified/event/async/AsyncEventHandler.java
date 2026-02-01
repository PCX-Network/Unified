/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.async;

import sh.pcx.unified.event.EventPriority;
import sh.pcx.unified.event.UnifiedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handler for processing events asynchronously.
 *
 * <p>This interface defines how async event handlers are executed. It provides
 * control over the executor, timeout, error handling, and completion callbacks.
 *
 * <h2>Default Implementation</h2>
 * <p>The default implementation uses a cached thread pool executor and handles
 * errors by logging them. Custom implementations can override this behavior.
 *
 * <h2>Creating an Async Handler</h2>
 * <pre>{@code
 * // Simple async handler
 * AsyncEventHandler<MyEvent> handler = AsyncEventHandler.of(event -> {
 *     // Async processing
 *     processAsync(event);
 * });
 *
 * // With error handling
 * AsyncEventHandler<MyEvent> handler = AsyncEventHandler.builder(MyEvent.class)
 *     .handler(event -> {
 *         processAsync(event);
 *     })
 *     .onError((event, error) -> {
 *         logger.error("Failed to process event", error);
 *     })
 *     .timeout(5, TimeUnit.SECONDS)
 *     .build();
 * }</pre>
 *
 * <h2>Using with EventBus</h2>
 * <pre>{@code
 * // Register async handler
 * eventBus.subscribe(MyEvent.class)
 *     .async()
 *     .handler(event -> processAsync(event))
 *     .register(plugin);
 * }</pre>
 *
 * @param <T> the event type
 * @since 1.0.0
 * @author Supatuck
 * @see AsyncEvent
 * @see sh.pcx.unified.event.AsyncHandler
 */
public interface AsyncEventHandler<T extends UnifiedEvent> {

    /**
     * Handles the event asynchronously.
     *
     * <p>This method is called on an async thread. Implementations should
     * perform the async processing and may call completion methods on
     * {@link AsyncEvent} subclasses.
     *
     * @param event the event to handle
     * @return a future that completes when handling is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> handle(@NotNull T event);

    /**
     * Returns the priority of this handler.
     *
     * @return the handler priority
     * @since 1.0.0
     */
    @NotNull
    default EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * Returns the executor used for this handler.
     *
     * @return the executor, or null for the default async executor
     * @since 1.0.0
     */
    @Nullable
    default Executor getExecutor() {
        return null;
    }

    /**
     * Returns the timeout for this handler in milliseconds.
     *
     * @return the timeout in milliseconds, or 0 for no timeout
     * @since 1.0.0
     */
    default long getTimeoutMillis() {
        return 0;
    }

    /**
     * Returns whether to ignore cancelled events.
     *
     * @return true to skip cancelled events
     * @since 1.0.0
     */
    default boolean ignoreCancelled() {
        return false;
    }

    /**
     * Creates a simple async handler from a consumer.
     *
     * @param handler the handler function
     * @param <T>     the event type
     * @return an async event handler
     * @since 1.0.0
     */
    @NotNull
    static <T extends UnifiedEvent> AsyncEventHandler<T> of(@NotNull Consumer<T> handler) {
        return event -> CompletableFuture.runAsync(() -> handler.accept(event));
    }

    /**
     * Creates an async handler with a custom executor.
     *
     * @param handler  the handler function
     * @param executor the executor to use
     * @param <T>      the event type
     * @return an async event handler
     * @since 1.0.0
     */
    @NotNull
    static <T extends UnifiedEvent> AsyncEventHandler<T> of(
            @NotNull Consumer<T> handler,
            @NotNull Executor executor
    ) {
        return new AsyncEventHandler<>() {
            @Override
            @NotNull
            public CompletableFuture<Void> handle(@NotNull T event) {
                return CompletableFuture.runAsync(() -> handler.accept(event), executor);
            }

            @Override
            @Nullable
            public Executor getExecutor() {
                return executor;
            }
        };
    }

    /**
     * Creates a builder for an async event handler.
     *
     * @param eventType the event type
     * @param <T>       the event type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static <T extends UnifiedEvent> Builder<T> builder(@NotNull Class<T> eventType) {
        return new Builder<>();
    }

    /**
     * Builder for async event handlers.
     *
     * @param <T> the event type
     * @since 1.0.0
     */
    class Builder<T extends UnifiedEvent> {

        private Consumer<T> handler;
        private BiConsumer<T, Throwable> errorHandler;
        private Executor executor;
        private EventPriority priority = EventPriority.NORMAL;
        private long timeoutMillis = 0;
        private boolean ignoreCancelled = false;

        private Builder() {}

        /**
         * Sets the handler function.
         *
         * @param handler the handler
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> handler(@NotNull Consumer<T> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Sets the error handler.
         *
         * @param errorHandler the error handler
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> onError(@NotNull BiConsumer<T, Throwable> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Sets the executor.
         *
         * @param executor the executor
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> executor(@NotNull Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Sets the handler priority.
         *
         * @param priority the priority
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> priority(@NotNull EventPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the timeout.
         *
         * @param timeout the timeout value
         * @param unit    the time unit
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> timeout(long timeout, @NotNull TimeUnit unit) {
            this.timeoutMillis = unit.toMillis(timeout);
            return this;
        }

        /**
         * Sets whether to ignore cancelled events.
         *
         * @param ignoreCancelled true to skip cancelled events
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> ignoreCancelled(boolean ignoreCancelled) {
            this.ignoreCancelled = ignoreCancelled;
            return this;
        }

        /**
         * Builds the async event handler.
         *
         * @return the async event handler
         * @throws IllegalStateException if no handler is set
         * @since 1.0.0
         */
        @NotNull
        public AsyncEventHandler<T> build() {
            if (handler == null) {
                throw new IllegalStateException("Handler must be set");
            }

            Consumer<T> finalHandler = handler;
            BiConsumer<T, Throwable> finalErrorHandler = errorHandler;
            Executor finalExecutor = executor;
            EventPriority finalPriority = priority;
            long finalTimeout = timeoutMillis;
            boolean finalIgnoreCancelled = ignoreCancelled;

            return new AsyncEventHandler<>() {
                @Override
                @NotNull
                public CompletableFuture<Void> handle(@NotNull T event) {
                    Executor exec = finalExecutor != null
                            ? finalExecutor
                            : Runnable::run; // Will be replaced with actual default

                    CompletableFuture<Void> future = CompletableFuture.runAsync(
                            () -> finalHandler.accept(event),
                            exec
                    );

                    if (finalErrorHandler != null) {
                        future = future.exceptionally(error -> {
                            finalErrorHandler.accept(event, error);
                            return null;
                        });
                    }

                    return future;
                }

                @Override
                @NotNull
                public EventPriority getPriority() {
                    return finalPriority;
                }

                @Override
                @Nullable
                public Executor getExecutor() {
                    return finalExecutor;
                }

                @Override
                public long getTimeoutMillis() {
                    return finalTimeout;
                }

                @Override
                public boolean ignoreCancelled() {
                    return finalIgnoreCancelled;
                }
            };
        }
    }
}
