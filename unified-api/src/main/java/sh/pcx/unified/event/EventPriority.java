/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

/**
 * Enumeration of event handler priorities.
 *
 * <p>Priority determines the order in which event handlers are called. Handlers
 * with lower priority values are called first, allowing higher priority handlers
 * to override or respond to changes made by lower priority handlers.
 *
 * <h2>Execution Order</h2>
 * <p>Handlers are executed in the following order:
 * <ol>
 *   <li>{@link #LOWEST} - First to run, sets initial values</li>
 *   <li>{@link #LOW} - Runs early, may modify event</li>
 *   <li>{@link #NORMAL} - Default priority, standard handling</li>
 *   <li>{@link #HIGH} - Runs late, may override earlier changes</li>
 *   <li>{@link #HIGHEST} - Last to modify, final overrides</li>
 *   <li>{@link #MONITOR} - Read-only observation, should not modify event</li>
 * </ol>
 *
 * <h2>Usage Guidelines</h2>
 * <ul>
 *   <li><b>LOWEST/LOW</b>: Use for plugins that set default values or perform
 *       initial processing that other plugins may want to override</li>
 *   <li><b>NORMAL</b>: Use for most event handling. This is the default
 *       priority if none is specified</li>
 *   <li><b>HIGH/HIGHEST</b>: Use for plugins that need to have the final
 *       say on event modifications, such as protection plugins</li>
 *   <li><b>MONITOR</b>: Use for logging, analytics, or other read-only
 *       operations. Never modify the event at this priority</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Default handler
 * @EventHandler
 * public void onJoin(PlayerJoinEvent event) {
 *     // Runs at NORMAL priority
 * }
 *
 * // High priority handler for protection override
 * @EventHandler(priority = EventPriority.HIGH)
 * public void onBlockBreakProtection(BlockBreakEvent event) {
 *     if (isProtected(event.getBlock())) {
 *         event.setCancelled(true);
 *     }
 * }
 *
 * // Monitor for logging (never modify!)
 * @EventHandler(priority = EventPriority.MONITOR)
 * public void onBlockBreakLog(BlockBreakEvent event) {
 *     log(event.getPlayer() + " broke " + event.getBlock()
 *         + " cancelled=" + event.isCancelled());
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventHandler
 */
public enum EventPriority {

    /**
     * Executed first; sets initial values that other handlers may change.
     *
     * <p>Use this priority for plugins that need to set default values
     * or perform initial processing before other plugins handle the event.
     *
     * @since 1.0.0
     */
    LOWEST(0),

    /**
     * Executed early; may modify event after LOWEST handlers.
     *
     * <p>Use this priority for early event handling that may be
     * overridden by later handlers.
     *
     * @since 1.0.0
     */
    LOW(1),

    /**
     * Default priority for standard event handling.
     *
     * <p>Most event handlers should use this priority. It runs after
     * low-priority handlers and before high-priority handlers.
     *
     * @since 1.0.0
     */
    NORMAL(2),

    /**
     * Executed late; may override earlier changes.
     *
     * <p>Use this priority for handlers that need to have the final
     * say on most event modifications, but may still be overridden.
     *
     * @since 1.0.0
     */
    HIGH(3),

    /**
     * Executed last before monitoring; final opportunity to modify.
     *
     * <p>Use this priority for protection plugins or handlers that
     * must have the absolute final word on event modifications.
     *
     * @since 1.0.0
     */
    HIGHEST(4),

    /**
     * Executed last; for observation only, should not modify the event.
     *
     * <p>Use this priority for logging, analytics, or debugging purposes.
     * Handlers at this priority should NEVER modify the event state,
     * as this can cause unexpected behavior and conflicts with other plugins.
     *
     * @since 1.0.0
     */
    MONITOR(5);

    private final int slot;

    /**
     * Constructs an event priority with the specified execution slot.
     *
     * @param slot the execution order slot (lower runs first)
     */
    EventPriority(int slot) {
        this.slot = slot;
    }

    /**
     * Returns the execution slot for this priority.
     *
     * <p>Lower values indicate earlier execution. This value is used
     * internally by the event system to order handlers.
     *
     * @return the execution slot
     * @since 1.0.0
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Returns whether this priority is the default (NORMAL).
     *
     * @return true if this is the default priority
     * @since 1.0.0
     */
    public boolean isDefault() {
        return this == NORMAL;
    }

    /**
     * Returns whether this priority is for monitoring only.
     *
     * <p>Handlers at monitor priority should not modify event state.
     *
     * @return true if this is the monitor priority
     * @since 1.0.0
     */
    public boolean isMonitor() {
        return this == MONITOR;
    }

    /**
     * Returns whether handlers at this priority run before the specified priority.
     *
     * @param other the other priority to compare
     * @return true if this priority runs before the other
     * @since 1.0.0
     */
    public boolean runsBefore(EventPriority other) {
        return this.slot < other.slot;
    }

    /**
     * Returns whether handlers at this priority run after the specified priority.
     *
     * @param other the other priority to compare
     * @return true if this priority runs after the other
     * @since 1.0.0
     */
    public boolean runsAfter(EventPriority other) {
        return this.slot > other.slot;
    }
}
