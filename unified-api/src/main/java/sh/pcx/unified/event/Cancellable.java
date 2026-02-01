/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

/**
 * Interface for events that can be cancelled.
 *
 * <p>When an event is cancelled, it indicates that the action associated with
 * the event should not proceed. However, event handlers will still be called
 * (unless they explicitly check for cancellation) to allow monitoring and
 * potential uncancellation.
 *
 * <h2>Usage</h2>
 * <p>Implement this interface in your event class to make it cancellable:
 *
 * <pre>{@code
 * public class PlayerDamageEvent extends UnifiedEvent implements Cancellable {
 *
 *     private boolean cancelled;
 *     private final UnifiedPlayer player;
 *     private double damage;
 *
 *     public PlayerDamageEvent(UnifiedPlayer player, double damage) {
 *         this.player = player;
 *         this.damage = damage;
 *     }
 *
 *     @Override
 *     public boolean isCancelled() {
 *         return cancelled;
 *     }
 *
 *     @Override
 *     public void setCancelled(boolean cancelled) {
 *         this.cancelled = cancelled;
 *     }
 *
 *     // Event-specific getters/setters...
 * }
 * }</pre>
 *
 * <h2>Handler Behavior</h2>
 * <p>Event handlers should check if the event is cancelled before performing
 * their actions, unless they need to run regardless of cancellation state:
 *
 * <pre>{@code
 * @EventHandler
 * public void onPlayerDamage(PlayerDamageEvent event) {
 *     if (event.isCancelled()) {
 *         return; // Skip if already cancelled
 *     }
 *     // Perform damage handling...
 * }
 *
 * @EventHandler(priority = EventPriority.MONITOR)
 * public void onPlayerDamageMonitor(PlayerDamageEvent event) {
 *     // Monitor handlers typically run regardless of cancellation
 *     // to log or track event outcomes
 *     logDamage(event, event.isCancelled());
 * }
 * }</pre>
 *
 * <h2>Uncancellation</h2>
 * <p>Events can be uncancelled by calling {@code setCancelled(false)}. This
 * allows higher-priority handlers to override lower-priority cancellations:
 *
 * <pre>{@code
 * @EventHandler(priority = EventPriority.HIGH)
 * public void onPlayerDamageAdminOverride(PlayerDamageEvent event) {
 *     if (event.getPlayer().hasPermission("admin.bypass")) {
 *         event.setCancelled(false); // Force event to proceed
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedEvent
 * @see EventPriority
 */
public interface Cancellable {

    /**
     * Returns whether this event has been cancelled.
     *
     * <p>A cancelled event indicates that the associated action should
     * not proceed. The exact meaning of "cancelled" depends on the
     * specific event type.
     *
     * @return true if this event is cancelled
     * @since 1.0.0
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of this event.
     *
     * <p>Setting this to {@code true} will cancel the event, preventing
     * the associated action from occurring (if the event source respects
     * the cancellation). Setting it to {@code false} will uncancel
     * a previously cancelled event.
     *
     * @param cancelled true to cancel the event, false to uncancel
     * @since 1.0.0
     */
    void setCancelled(boolean cancelled);

    /**
     * Cancels this event.
     *
     * <p>This is a convenience method equivalent to {@code setCancelled(true)}.
     *
     * @since 1.0.0
     */
    default void cancel() {
        setCancelled(true);
    }

    /**
     * Uncancels this event.
     *
     * <p>This is a convenience method equivalent to {@code setCancelled(false)}.
     *
     * @since 1.0.0
     */
    default void uncancel() {
        setCancelled(false);
    }
}
