/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Adapter for creating event listeners from method references or lambdas.
 *
 * <p>This class provides a way to create event listeners without implementing
 * the {@link EventListener} interface. It wraps a handler function and optional
 * filter into a listener that can be registered with the event bus.
 *
 * <h2>Usage with Lambda</h2>
 * <pre>{@code
 * // Simple lambda handler
 * ListenerAdapter<PlayerJoinEvent> adapter = ListenerAdapter.of(
 *     PlayerJoinEvent.class,
 *     event -> event.getPlayer().sendMessage(Component.text("Welcome!"))
 * );
 * eventBus.register(plugin, adapter);
 *
 * // With filter
 * ListenerAdapter<PlayerJoinEvent> vipAdapter = ListenerAdapter.of(
 *     PlayerJoinEvent.class,
 *     event -> event.getPlayer().hasPermission("vip"),
 *     event -> event.getPlayer().sendMessage(Component.text("VIP Welcome!"))
 * );
 *
 * // With priority
 * ListenerAdapter<BlockBreakEvent> protectionAdapter = ListenerAdapter.builder(BlockBreakEvent.class)
 *     .priority(EventPriority.HIGH)
 *     .filter(event -> isProtected(event.getBlock()))
 *     .handler(event -> event.setCancelled(true))
 *     .build();
 * }</pre>
 *
 * <h2>Usage with Method Reference</h2>
 * <pre>{@code
 * // From instance method
 * ListenerAdapter<PlayerJoinEvent> adapter = ListenerAdapter.fromMethod(
 *     PlayerJoinEvent.class,
 *     myService::handlePlayerJoin
 * );
 *
 * // From static method
 * ListenerAdapter<PlayerJoinEvent> staticAdapter = ListenerAdapter.fromMethod(
 *     PlayerJoinEvent.class,
 *     MyHandler::onPlayerJoin
 * );
 * }</pre>
 *
 * <h2>Chaining</h2>
 * <pre>{@code
 * ListenerAdapter<PlayerMoveEvent> chainedAdapter = ListenerAdapter.of(PlayerMoveEvent.class)
 *     .filter(e -> !e.getPlayer().isFlying())
 *     .andThen(e -> checkBorder(e))
 *     .andThen(e -> updatePosition(e))
 *     .build();
 * }</pre>
 *
 * @param <T> the event type this adapter handles
 * @since 1.0.0
 * @author Supatuck
 * @see EventListener
 * @see EventBus
 */
public class ListenerAdapter<T extends UnifiedEvent> implements EventListener {

    private final Class<T> eventType;
    private final Consumer<T> handler;
    private final Predicate<T> filter;
    private final EventPriority priority;
    private final boolean ignoreCancelled;
    private final boolean async;
    private final String description;

    /**
     * Constructs a new listener adapter.
     *
     * @param eventType       the event type to handle
     * @param handler         the handler function
     * @param filter          optional filter predicate
     * @param priority        the handler priority
     * @param ignoreCancelled whether to skip cancelled events
     * @param async           whether to run asynchronously
     * @param description     optional description
     */
    private ListenerAdapter(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @Nullable Predicate<T> filter,
            @NotNull EventPriority priority,
            boolean ignoreCancelled,
            boolean async,
            @Nullable String description
    ) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.handler = Objects.requireNonNull(handler, "handler cannot be null");
        this.filter = filter;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
        this.async = async;
        this.description = description;
    }

    /**
     * Creates a simple listener adapter with a handler.
     *
     * @param eventType the event type to handle
     * @param handler   the handler function
     * @param <T>       the event type
     * @return a new listener adapter
     * @since 1.0.0
     */
    @NotNull
    public static <T extends UnifiedEvent> ListenerAdapter<T> of(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    ) {
        return new ListenerAdapter<>(
                eventType, handler, null, EventPriority.NORMAL, false, false, null
        );
    }

    /**
     * Creates a listener adapter with a filter and handler.
     *
     * @param eventType the event type to handle
     * @param filter    the filter predicate
     * @param handler   the handler function
     * @param <T>       the event type
     * @return a new listener adapter
     * @since 1.0.0
     */
    @NotNull
    public static <T extends UnifiedEvent> ListenerAdapter<T> of(
            @NotNull Class<T> eventType,
            @NotNull Predicate<T> filter,
            @NotNull Consumer<T> handler
    ) {
        return new ListenerAdapter<>(
                eventType, handler, filter, EventPriority.NORMAL, false, false, null
        );
    }

    /**
     * Creates a builder for a listener adapter.
     *
     * @param eventType the event type to handle
     * @param <T>       the event type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <T extends UnifiedEvent> Builder<T> builder(@NotNull Class<T> eventType) {
        return new Builder<>(eventType);
    }

    /**
     * Creates a listener adapter from a method reference.
     *
     * @param eventType the event type to handle
     * @param handler   the method reference handler
     * @param <T>       the event type
     * @return a new listener adapter
     * @since 1.0.0
     */
    @NotNull
    public static <T extends UnifiedEvent> ListenerAdapter<T> fromMethod(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    ) {
        return of(eventType, handler);
    }

    /**
     * Handles the event by invoking the wrapped handler.
     *
     * <p>This method is called by the event system and should not be
     * called directly.
     *
     * @param event the event to handle
     * @since 1.0.0
     */
    @EventHandler
    public void handleEvent(UnifiedEvent event) {
        if (!eventType.isInstance(event)) {
            return;
        }
        T typedEvent = eventType.cast(event);
        if (filter != null && !filter.test(typedEvent)) {
            return;
        }
        handler.accept(typedEvent);
    }

    /**
     * Returns the event type this adapter handles.
     *
     * @return the event type
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getEventType() {
        return eventType;
    }

    /**
     * Returns the handler priority.
     *
     * @return the priority
     * @since 1.0.0
     */
    @NotNull
    public EventPriority getPriority() {
        return priority;
    }

    /**
     * Returns whether cancelled events are ignored.
     *
     * @return true if cancelled events are skipped
     * @since 1.0.0
     */
    public boolean isIgnoreCancelled() {
        return ignoreCancelled;
    }

    /**
     * Returns whether this adapter runs asynchronously.
     *
     * @return true if async
     * @since 1.0.0
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Returns the description of this adapter.
     *
     * @return the description, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Returns the handler method for registration.
     *
     * @return the handler method
     * @since 1.0.0
     */
    @NotNull
    public Method getHandlerMethod() {
        try {
            return getClass().getMethod("handleEvent", UnifiedEvent.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("handleEvent method not found", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ListenerAdapter[");
        sb.append("event=").append(eventType.getSimpleName());
        sb.append(", priority=").append(priority);
        if (ignoreCancelled) {
            sb.append(", ignoreCancelled");
        }
        if (async) {
            sb.append(", async");
        }
        if (description != null) {
            sb.append(", description=\"").append(description).append("\"");
        }
        return sb.append("]").toString();
    }

    /**
     * Builder for creating listener adapters with custom options.
     *
     * @param <T> the event type
     * @since 1.0.0
     */
    public static class Builder<T extends UnifiedEvent> {

        private final Class<T> eventType;
        private Consumer<T> handler;
        private Predicate<T> filter;
        private EventPriority priority = EventPriority.NORMAL;
        private boolean ignoreCancelled = false;
        private boolean async = false;
        private String description;

        private Builder(@NotNull Class<T> eventType) {
            this.eventType = eventType;
        }

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
         * Sets the filter predicate.
         *
         * @param filter the filter
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> filter(@NotNull Predicate<T> filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Adds an additional filter (AND with existing).
         *
         * @param additionalFilter the additional filter
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> andFilter(@NotNull Predicate<T> additionalFilter) {
            if (this.filter == null) {
                this.filter = additionalFilter;
            } else {
                this.filter = this.filter.and(additionalFilter);
            }
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
         * Marks this handler as async.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> async() {
            this.async = true;
            return this;
        }

        /**
         * Sets the async flag.
         *
         * @param async true for async execution
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> description(@NotNull String description) {
            this.description = description;
            return this;
        }

        /**
         * Chains another handler after the main handler.
         *
         * @param next the next handler to chain
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> andThen(@NotNull Consumer<T> next) {
            if (this.handler == null) {
                this.handler = next;
            } else {
                this.handler = this.handler.andThen(next);
            }
            return this;
        }

        /**
         * Builds the listener adapter.
         *
         * @return the listener adapter
         * @throws IllegalStateException if no handler is set
         * @since 1.0.0
         */
        @NotNull
        public ListenerAdapter<T> build() {
            if (handler == null) {
                throw new IllegalStateException("Handler must be set");
            }
            return new ListenerAdapter<>(
                    eventType, handler, filter, priority, ignoreCancelled, async, description
            );
        }
    }
}
