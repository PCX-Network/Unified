/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import sh.pcx.unified.UnifiedPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Main event bus interface for registering listeners and firing events.
 *
 * <p>The event bus is the central hub for all event operations in the unified
 * API. It provides methods to:
 * <ul>
 *   <li>Register and unregister event listeners</li>
 *   <li>Fire events synchronously or asynchronously</li>
 *   <li>Subscribe to specific event types programmatically</li>
 *   <li>Query registered handlers</li>
 * </ul>
 *
 * <h2>Obtaining the Event Bus</h2>
 * <pre>{@code
 * EventBus eventBus = UnifiedAPI.getEventBus();
 * }</pre>
 *
 * <h2>Registering Listeners</h2>
 * <pre>{@code
 * // Register a listener class
 * eventBus.register(plugin, new MyListener());
 *
 * // Register with automatic scanning
 * eventBus.registerAll(plugin, listenerPackage);
 * }</pre>
 *
 * <h2>Firing Events</h2>
 * <pre>{@code
 * // Fire synchronously
 * MyEvent event = new MyEvent();
 * eventBus.fire(event);
 * if (!event.isCancelled()) {
 *     // Handle result
 * }
 *
 * // Fire asynchronously
 * eventBus.fireAsync(event).thenAccept(e -> {
 *     // Handle result after all handlers complete
 * });
 * }</pre>
 *
 * <h2>Programmatic Subscriptions</h2>
 * <pre>{@code
 * // Lambda-based subscription
 * Subscription sub = eventBus.subscribe(PlayerJoinEvent.class, event -> {
 *     event.getPlayer().sendMessage(Component.text("Welcome!"));
 * });
 *
 * // With options
 * eventBus.subscribe(PlayerJoinEvent.class)
 *     .priority(EventPriority.HIGH)
 *     .filter(e -> e.getPlayer().hasPermission("vip"))
 *     .handler(event -> {
 *         // VIP handling
 *     })
 *     .register(plugin);
 *
 * // Unsubscribe later
 * sub.unsubscribe();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventListener
 * @see EventHandler
 * @see EventRegistry
 * @see EventDispatcher
 */
public interface EventBus {

    /**
     * Registers an event listener with the event bus.
     *
     * <p>All methods in the listener class annotated with {@link EventHandler}
     * or {@link AsyncHandler} will be registered as event handlers.
     *
     * @param plugin   the plugin registering the listener
     * @param listener the listener to register
     * @throws IllegalArgumentException if the listener has invalid handler methods
     * @since 1.0.0
     */
    void register(@NotNull UnifiedPlugin plugin, @NotNull EventListener listener);

    /**
     * Registers multiple event listeners with the event bus.
     *
     * @param plugin    the plugin registering the listeners
     * @param listeners the listeners to register
     * @throws IllegalArgumentException if any listener has invalid handler methods
     * @since 1.0.0
     */
    void register(@NotNull UnifiedPlugin plugin, @NotNull EventListener... listeners);

    /**
     * Unregisters an event listener from the event bus.
     *
     * <p>After this call, the listener will no longer receive any events.
     *
     * @param listener the listener to unregister
     * @since 1.0.0
     */
    void unregister(@NotNull EventListener listener);

    /**
     * Unregisters all listeners registered by a specific plugin.
     *
     * <p>This is typically called when a plugin is disabled to clean up
     * all its event registrations.
     *
     * @param plugin the plugin whose listeners should be unregistered
     * @since 1.0.0
     */
    void unregisterAll(@NotNull UnifiedPlugin plugin);

    /**
     * Unregisters all listeners for a specific event type.
     *
     * @param eventType the event type to unregister listeners for
     * @param <T>       the event type
     * @since 1.0.0
     */
    <T extends UnifiedEvent> void unregisterAll(@NotNull Class<T> eventType);

    /**
     * Fires an event synchronously.
     *
     * <p>This method blocks until all handlers have been called. Handlers
     * are executed in priority order on the current thread.
     *
     * @param event the event to fire
     * @param <T>   the event type
     * @return the event after all handlers have processed it
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> T fire(@NotNull T event);

    /**
     * Fires an event asynchronously.
     *
     * <p>This method returns immediately and processes handlers on an
     * async thread pool. The returned future completes when all handlers
     * have finished processing.
     *
     * @param event the event to fire
     * @param <T>   the event type
     * @return a future that completes with the event when processing is done
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> CompletableFuture<T> fireAsync(@NotNull T event);

    /**
     * Fires an event and returns whether it was cancelled.
     *
     * <p>This is a convenience method for cancellable events that fires
     * the event and returns the cancellation status.
     *
     * @param event the event to fire
     * @param <T>   the event type (must be Cancellable)
     * @return true if the event was cancelled
     * @since 1.0.0
     */
    <T extends UnifiedEvent & Cancellable> boolean fireAndCheckCancelled(@NotNull T event);

    /**
     * Creates a subscription builder for programmatic event subscriptions.
     *
     * <p>This allows registering event handlers without creating a listener class:
     *
     * <pre>{@code
     * eventBus.subscribe(PlayerJoinEvent.class)
     *     .priority(EventPriority.HIGH)
     *     .handler(event -> {
     *         // Handle event
     *     })
     *     .register(plugin);
     * }</pre>
     *
     * @param eventType the type of event to subscribe to
     * @param <T>       the event type
     * @return a subscription builder
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> SubscriptionBuilder<T> subscribe(@NotNull Class<T> eventType);

    /**
     * Creates a simple subscription for an event type with a handler.
     *
     * <p>This is a convenience method for simple subscriptions:
     *
     * <pre>{@code
     * Subscription sub = eventBus.subscribe(PlayerJoinEvent.class, event -> {
     *     event.getPlayer().sendMessage(Component.text("Welcome!"));
     * });
     * }</pre>
     *
     * @param eventType the type of event to subscribe to
     * @param handler   the handler to call when the event fires
     * @param plugin    the plugin owning this subscription
     * @param <T>       the event type
     * @return a subscription that can be used to unsubscribe
     * @since 1.0.0
     */
    @NotNull
    <T extends UnifiedEvent> Subscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull UnifiedPlugin plugin
    );

    /**
     * Checks if there are any handlers registered for an event type.
     *
     * @param eventType the event type to check
     * @return true if there are handlers for this event type
     * @since 1.0.0
     */
    boolean hasHandlers(@NotNull Class<? extends UnifiedEvent> eventType);

    /**
     * Returns the number of handlers registered for an event type.
     *
     * @param eventType the event type to count handlers for
     * @return the number of registered handlers
     * @since 1.0.0
     */
    int getHandlerCount(@NotNull Class<? extends UnifiedEvent> eventType);

    /**
     * Returns the event registry for direct handler management.
     *
     * @return the event registry
     * @since 1.0.0
     */
    @NotNull
    EventRegistry getRegistry();

    /**
     * Returns the event dispatcher for direct event dispatch.
     *
     * @return the event dispatcher
     * @since 1.0.0
     */
    @NotNull
    EventDispatcher getDispatcher();

    /**
     * Builder for programmatic event subscriptions.
     *
     * @param <T> the event type
     * @since 1.0.0
     */
    interface SubscriptionBuilder<T extends UnifiedEvent> {

        /**
         * Sets the priority for this subscription.
         *
         * @param priority the handler priority
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        SubscriptionBuilder<T> priority(@NotNull EventPriority priority);

        /**
         * Sets whether to ignore cancelled events.
         *
         * @param ignore true to skip cancelled events
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        SubscriptionBuilder<T> ignoreCancelled(boolean ignore);

        /**
         * Adds a filter predicate.
         *
         * @param filter the filter predicate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        SubscriptionBuilder<T> filter(@NotNull Predicate<T> filter);

        /**
         * Sets the handler for this subscription.
         *
         * @param handler the event handler
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        SubscriptionBuilder<T> handler(@NotNull Consumer<T> handler);

        /**
         * Marks this subscription as async.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        SubscriptionBuilder<T> async();

        /**
         * Sets the executor for async handling.
         *
         * @param executor the executor name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        SubscriptionBuilder<T> executor(@NotNull String executor);

        /**
         * Registers this subscription with the event bus.
         *
         * @param plugin the plugin owning this subscription
         * @return the subscription for later unsubscription
         * @since 1.0.0
         */
        @NotNull
        Subscription register(@NotNull UnifiedPlugin plugin);
    }

    /**
     * Represents an active event subscription.
     *
     * @since 1.0.0
     */
    interface Subscription {

        /**
         * Returns whether this subscription is currently active.
         *
         * @return true if the subscription is active
         * @since 1.0.0
         */
        boolean isActive();

        /**
         * Unsubscribes from the event.
         *
         * <p>After calling this method, the handler will no longer receive events.
         *
         * @since 1.0.0
         */
        void unsubscribe();

        /**
         * Returns the event type this subscription handles.
         *
         * @return the event type class
         * @since 1.0.0
         */
        @NotNull
        Class<? extends UnifiedEvent> getEventType();

        /**
         * Returns the priority of this subscription.
         *
         * @return the handler priority
         * @since 1.0.0
         */
        @NotNull
        EventPriority getPriority();

        /**
         * Returns the plugin that owns this subscription.
         *
         * @return the owning plugin
         * @since 1.0.0
         */
        @NotNull
        UnifiedPlugin getPlugin();
    }
}
