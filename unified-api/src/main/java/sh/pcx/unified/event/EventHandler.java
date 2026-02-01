/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as an event handler.
 *
 * <p>Methods annotated with {@code @EventHandler} will be automatically registered
 * as event listeners when the containing class is registered with the {@link EventBus}.
 * The method must have exactly one parameter, which must be a subclass of {@link UnifiedEvent}.
 *
 * <h2>Method Requirements</h2>
 * <ul>
 *   <li>Method must be public</li>
 *   <li>Method must have exactly one parameter</li>
 *   <li>Parameter must be a subclass of {@link UnifiedEvent}</li>
 *   <li>Method may have any return type (return value is ignored)</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * public class MyListener implements EventListener {
 *
 *     @EventHandler
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         // Handle player join at NORMAL priority
 *         event.getPlayer().sendMessage(Component.text("Welcome!"));
 *     }
 *
 *     @EventHandler(priority = EventPriority.HIGH)
 *     public void onBlockBreak(BlockBreakEvent event) {
 *         // Handle block break at HIGH priority
 *         if (isProtected(event.getBlock())) {
 *             event.setCancelled(true);
 *         }
 *     }
 *
 *     @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
 *     public void logBlockBreak(BlockBreakEvent event) {
 *         // Only log successful block breaks (not cancelled)
 *         logger.info(event.getPlayer().getName() + " broke " + event.getBlock());
 *     }
 * }
 * }</pre>
 *
 * <h2>Priority</h2>
 * <p>The {@link #priority()} attribute determines when this handler runs relative
 * to other handlers for the same event. See {@link EventPriority} for details.
 *
 * <h2>Ignore Cancelled</h2>
 * <p>When {@link #ignoreCancelled()} is true, this handler will not be called
 * if a previous handler has cancelled the event. This is useful for handlers
 * that only want to process successful events.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventPriority
 * @see EventListener
 * @see EventBus
 * @see Filter
 * @see AsyncHandler
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * The priority of this event handler.
     *
     * <p>Handlers with lower priority values are executed first.
     * The default priority is {@link EventPriority#NORMAL}.
     *
     * @return the handler priority
     * @since 1.0.0
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Whether to skip this handler if the event is cancelled.
     *
     * <p>When set to {@code true}, this handler will not be called if the
     * event has been cancelled by a previous handler. This is useful for
     * handlers that should only process successful (non-cancelled) events.
     *
     * <p>When set to {@code false} (the default), this handler will be
     * called regardless of the event's cancellation state. This allows
     * handlers to monitor or uncancel events.
     *
     * <p>Note: This setting has no effect on non-cancellable events.
     *
     * @return true to skip cancelled events
     * @since 1.0.0
     */
    boolean ignoreCancelled() default false;

    /**
     * Whether this handler should receive events from all sources.
     *
     * <p>When set to {@code true}, this handler will receive events from
     * all plugins and the platform itself. When set to {@code false}
     * (the default), the handler only receives events relevant to its
     * registering plugin.
     *
     * <p>This is primarily used for monitoring and analytics purposes.
     *
     * @return true to receive all events
     * @since 1.0.0
     */
    boolean receiveAll() default false;
}
