/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet;

/**
 * Priority levels for packet handlers in the registry.
 *
 * <p>Handlers are executed in priority order from LOWEST to MONITOR.
 * Higher priority handlers see the effects of lower priority handlers.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PacketRegistry
 */
public enum PacketPriority {

    /**
     * Lowest priority - first to receive the packet.
     *
     * <p>Use this for filtering or early cancellation.
     */
    LOWEST(false),

    /**
     * Low priority - early in the chain.
     */
    LOW(false),

    /**
     * Normal priority - default for most handlers.
     */
    NORMAL(false),

    /**
     * High priority - late in the chain.
     */
    HIGH(false),

    /**
     * Highest priority - last before monitor.
     */
    HIGHEST(false),

    /**
     * Monitor priority - for observation only.
     *
     * <p>Monitor handlers should not modify or cancel packets.
     * They see the final state after all other processing.
     * By default, monitor priority ignores cancelled events.
     */
    MONITOR(true);

    private final boolean ignoreCancelled;

    PacketPriority(boolean ignoreCancelled) {
        this.ignoreCancelled = ignoreCancelled;
    }

    /**
     * Checks if this priority should ignore cancelled events.
     *
     * @return true if cancelled events should be skipped
     * @since 1.0.0
     */
    public boolean isIgnoreCancelled() {
        return ignoreCancelled;
    }
}
