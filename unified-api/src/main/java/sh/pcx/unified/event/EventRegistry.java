/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import sh.pcx.unified.UnifiedPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Registry for managing event listener registrations.
 *
 * <p>The event registry maintains the mapping between event types and their
 * handlers. It handles the low-level registration, unregistration, and lookup
 * of event handlers.
 *
 * <h2>Handler Registration</h2>
 * <p>When a listener is registered, the registry scans for annotated methods
 * and creates handler entries for each valid event handler method.
 *
 * <h2>Handler Lookup</h2>
 * <p>The registry provides efficient lookup of handlers for specific event
 * types, taking into account inheritance so that handlers for parent event
 * types also receive child events.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * EventRegistry registry = eventBus.getRegistry();
 *
 * // Get all handlers for an event type
 * List<RegisteredHandler> handlers = registry.getHandlers(PlayerJoinEvent.class);
 *
 * // Check if a listener is registered
 * boolean isRegistered = registry.isRegistered(myListener);
 *
 * // Get all registered event types
 * Set<Class<? extends UnifiedEvent>> types = registry.getRegisteredEventTypes();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventBus
 * @see RegisteredHandler
 */
public interface EventRegistry {

    /**
     * Registers a listener and all its handler methods.
     *
     * @param plugin   the plugin registering the listener
     * @param listener the listener to register
     * @throws IllegalArgumentException if the listener has invalid handler methods
     * @since 1.0.0
     */
    void registerListener(@NotNull UnifiedPlugin plugin, @NotNull EventListener listener);

    /**
     * Registers a single handler method.
     *
     * @param plugin    the plugin registering the handler
     * @param listener  the listener instance
     * @param method    the handler method
     * @param eventType the event type this method handles
     * @param priority  the handler priority
     * @param options   additional handler options
     * @return the registered handler
     * @since 1.0.0
     */
    @NotNull
    RegisteredHandler registerHandler(
            @NotNull UnifiedPlugin plugin,
            @NotNull EventListener listener,
            @NotNull Method method,
            @NotNull Class<? extends UnifiedEvent> eventType,
            @NotNull EventPriority priority,
            @NotNull HandlerOptions options
    );

    /**
     * Unregisters a listener and all its handlers.
     *
     * @param listener the listener to unregister
     * @since 1.0.0
     */
    void unregisterListener(@NotNull EventListener listener);

    /**
     * Unregisters a specific handler.
     *
     * @param handler the handler to unregister
     * @since 1.0.0
     */
    void unregisterHandler(@NotNull RegisteredHandler handler);

    /**
     * Unregisters all handlers for a plugin.
     *
     * @param plugin the plugin whose handlers to unregister
     * @since 1.0.0
     */
    void unregisterAll(@NotNull UnifiedPlugin plugin);

    /**
     * Unregisters all handlers for an event type.
     *
     * @param eventType the event type to unregister handlers for
     * @since 1.0.0
     */
    void unregisterAll(@NotNull Class<? extends UnifiedEvent> eventType);

    /**
     * Clears all registered handlers.
     *
     * @since 1.0.0
     */
    void clear();

    /**
     * Returns all handlers registered for an event type.
     *
     * <p>Handlers are returned in priority order (LOWEST first, MONITOR last).
     * This includes handlers for parent event types.
     *
     * @param eventType the event type
     * @return list of handlers in execution order
     * @since 1.0.0
     */
    @NotNull
    List<RegisteredHandler> getHandlers(@NotNull Class<? extends UnifiedEvent> eventType);

    /**
     * Returns handlers registered for an event type by a specific plugin.
     *
     * @param eventType the event type
     * @param plugin    the plugin to filter by
     * @return list of handlers from the plugin
     * @since 1.0.0
     */
    @NotNull
    List<RegisteredHandler> getHandlers(
            @NotNull Class<? extends UnifiedEvent> eventType,
            @NotNull UnifiedPlugin plugin
    );

    /**
     * Returns all handlers registered by a plugin.
     *
     * @param plugin the plugin
     * @return list of all handlers registered by the plugin
     * @since 1.0.0
     */
    @NotNull
    List<RegisteredHandler> getHandlers(@NotNull UnifiedPlugin plugin);

    /**
     * Checks if a listener is currently registered.
     *
     * @param listener the listener to check
     * @return true if the listener is registered
     * @since 1.0.0
     */
    boolean isRegistered(@NotNull EventListener listener);

    /**
     * Checks if there are any handlers for an event type.
     *
     * @param eventType the event type
     * @return true if there are handlers
     * @since 1.0.0
     */
    boolean hasHandlers(@NotNull Class<? extends UnifiedEvent> eventType);

    /**
     * Returns the count of handlers for an event type.
     *
     * @param eventType the event type
     * @return the number of handlers
     * @since 1.0.0
     */
    int getHandlerCount(@NotNull Class<? extends UnifiedEvent> eventType);

    /**
     * Returns all registered event types.
     *
     * @return set of event types with registered handlers
     * @since 1.0.0
     */
    @NotNull
    Set<Class<? extends UnifiedEvent>> getRegisteredEventTypes();

    /**
     * Returns all registered listeners.
     *
     * @return set of registered listeners
     * @since 1.0.0
     */
    @NotNull
    Set<EventListener> getRegisteredListeners();

    /**
     * Returns all registered listeners for a plugin.
     *
     * @param plugin the plugin
     * @return set of listeners registered by the plugin
     * @since 1.0.0
     */
    @NotNull
    Set<EventListener> getRegisteredListeners(@NotNull UnifiedPlugin plugin);

    /**
     * Bakes the handler list for an event type.
     *
     * <p>This creates an optimized, cached list of handlers for efficient
     * dispatching. Called automatically when handlers change, but can be
     * called manually to pre-warm the cache.
     *
     * @param eventType the event type to bake
     * @since 1.0.0
     */
    void bakeHandlers(@NotNull Class<? extends UnifiedEvent> eventType);

    /**
     * Invalidates the cached handler list for an event type.
     *
     * <p>Forces re-computation of the handler list on next dispatch.
     *
     * @param eventType the event type to invalidate, or null for all types
     * @since 1.0.0
     */
    void invalidateCache(@Nullable Class<? extends UnifiedEvent> eventType);

    /**
     * Represents a registered event handler.
     *
     * @since 1.0.0
     */
    interface RegisteredHandler {

        /**
         * Returns the plugin that registered this handler.
         *
         * @return the owning plugin
         * @since 1.0.0
         */
        @NotNull
        UnifiedPlugin getPlugin();

        /**
         * Returns the listener instance.
         *
         * @return the listener, or null if garbage collected (weak listener)
         * @since 1.0.0
         */
        @Nullable
        EventListener getListener();

        /**
         * Returns the handler method.
         *
         * @return the method
         * @since 1.0.0
         */
        @NotNull
        Method getMethod();

        /**
         * Returns the event type this handler processes.
         *
         * @return the event type
         * @since 1.0.0
         */
        @NotNull
        Class<? extends UnifiedEvent> getEventType();

        /**
         * Returns the handler priority.
         *
         * @return the priority
         * @since 1.0.0
         */
        @NotNull
        EventPriority getPriority();

        /**
         * Returns the handler options.
         *
         * @return the options
         * @since 1.0.0
         */
        @NotNull
        HandlerOptions getOptions();

        /**
         * Invokes this handler with the given event.
         *
         * @param event the event to handle
         * @throws Exception if the handler throws an exception
         * @since 1.0.0
         */
        void invoke(@NotNull UnifiedEvent event) throws Exception;

        /**
         * Returns whether this handler is still valid.
         *
         * <p>A handler becomes invalid if its listener has been garbage
         * collected (for weak listeners) or explicitly unregistered.
         *
         * @return true if the handler is valid
         * @since 1.0.0
         */
        boolean isValid();

        /**
         * Returns whether this is an async handler.
         *
         * @return true if async
         * @since 1.0.0
         */
        boolean isAsync();
    }

    /**
     * Options for handler registration.
     *
     * @since 1.0.0
     */
    interface HandlerOptions {

        /**
         * Returns whether to ignore cancelled events.
         *
         * @return true to skip cancelled events
         * @since 1.0.0
         */
        boolean ignoreCancelled();

        /**
         * Returns whether this is an async handler.
         *
         * @return true if async
         * @since 1.0.0
         */
        boolean isAsync();

        /**
         * Returns the executor name for async handlers.
         *
         * @return the executor name, or empty for default
         * @since 1.0.0
         */
        @NotNull
        String getExecutor();

        /**
         * Returns the timeout for async handlers.
         *
         * @return timeout in milliseconds, or 0 for no timeout
         * @since 1.0.0
         */
        long getTimeout();

        /**
         * Returns whether to await async completion.
         *
         * @return true to await completion
         * @since 1.0.0
         */
        boolean awaitCompletion();

        /**
         * Returns the filters for this handler.
         *
         * @return array of filters
         * @since 1.0.0
         */
        @NotNull
        Filter[] getFilters();

        /**
         * Creates a default options instance.
         *
         * @return default options
         * @since 1.0.0
         */
        static HandlerOptions defaults() {
            return new HandlerOptions() {
                @Override
                public boolean ignoreCancelled() {
                    return false;
                }

                @Override
                public boolean isAsync() {
                    return false;
                }

                @Override
                @NotNull
                public String getExecutor() {
                    return "";
                }

                @Override
                public long getTimeout() {
                    return 0;
                }

                @Override
                public boolean awaitCompletion() {
                    return false;
                }

                @Override
                @NotNull
                public Filter[] getFilters() {
                    return new Filter[0];
                }
            };
        }
    }
}
