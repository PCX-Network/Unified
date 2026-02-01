/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet.listener;

/**
 * Priority levels for packet listeners.
 *
 * <p>Listeners are executed in priority order from LOWEST to MONITOR.
 * Higher priority listeners see the effects of lower priority listeners.
 *
 * <h2>Priority Order</h2>
 * <ol>
 *   <li>LOWEST - First to receive, modifications affect all later listeners</li>
 *   <li>LOW - Early processing</li>
 *   <li>NORMAL - Default priority for most listeners</li>
 *   <li>HIGH - Late processing, sees most modifications</li>
 *   <li>HIGHEST - Very late processing</li>
 *   <li>MONITOR - Read-only observation, should not modify packets</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public enum ListenerPriority {

    /**
     * Lowest priority - first to receive the packet.
     *
     * <p>Use this for filtering or early cancellation.
     */
    LOWEST(0),

    /**
     * Low priority - early in the chain.
     */
    LOW(1),

    /**
     * Normal priority - default for most listeners.
     */
    NORMAL(2),

    /**
     * High priority - late in the chain.
     */
    HIGH(3),

    /**
     * Highest priority - last before monitor.
     */
    HIGHEST(4),

    /**
     * Monitor priority - for observation only.
     *
     * <p>Monitor listeners should not modify or cancel packets.
     * They see the final state after all other processing.
     */
    MONITOR(5);

    private final int slot;

    ListenerPriority(int slot) {
        this.slot = slot;
    }

    /**
     * Returns the numeric priority slot.
     *
     * <p>Lower values are processed first.
     *
     * @return the priority slot
     * @since 1.0.0
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Checks if this priority is before another.
     *
     * @param other the other priority
     * @return true if this priority is processed first
     * @since 1.0.0
     */
    public boolean isBefore(ListenerPriority other) {
        return this.slot < other.slot;
    }

    /**
     * Checks if this priority is after another.
     *
     * @param other the other priority
     * @return true if this priority is processed later
     * @since 1.0.0
     */
    public boolean isAfter(ListenerPriority other) {
        return this.slot > other.slot;
    }

    /**
     * Checks if this is the monitor priority.
     *
     * @return true if monitor priority
     * @since 1.0.0
     */
    public boolean isMonitor() {
        return this == MONITOR;
    }
}
