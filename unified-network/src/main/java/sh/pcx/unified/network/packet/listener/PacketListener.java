/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet.listener;

import sh.pcx.unified.network.packet.PacketType;
import sh.pcx.unified.network.packet.event.PacketEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a registered packet listener.
 *
 * <p>Packet listeners intercept packets as they flow through the network
 * pipeline, allowing inspection, modification, or cancellation.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Register a listener
 * PacketListener listener = packets.listen(PacketType.PLAY_IN_CHAT)
 *     .handler(event -> {
 *         String message = event.getPacket().getStrings().read(0);
 *         getLogger().info("Chat: " + message);
 *     })
 *     .register();
 *
 * // Later, unregister
 * listener.unregister();
 *
 * // Check listener state
 * if (listener.isActive()) {
 *     // Still listening
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PacketListenerBuilder
 * @see ListenerPriority
 */
public interface PacketListener {

    /**
     * Returns the unique ID of this listener.
     *
     * @return the listener ID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns the packet types this listener handles.
     *
     * @return the set of packet types
     * @since 1.0.0
     */
    @NotNull
    Set<PacketType> getPacketTypes();

    /**
     * Returns the priority of this listener.
     *
     * @return the listener priority
     * @since 1.0.0
     */
    @NotNull
    ListenerPriority getPriority();

    /**
     * Checks if this listener is currently active.
     *
     * @return true if active
     * @since 1.0.0
     */
    boolean isActive();

    /**
     * Checks if this listener handles async packets.
     *
     * @return true if async
     * @since 1.0.0
     */
    boolean isAsync();

    /**
     * Called when a packet matching this listener's types is intercepted.
     *
     * @param event the packet event
     * @since 1.0.0
     */
    void onPacket(@NotNull PacketEvent event);

    /**
     * Unregisters this listener.
     *
     * <p>After unregistering, this listener will no longer receive packets.
     *
     * @since 1.0.0
     */
    void unregister();

    /**
     * Temporarily disables this listener.
     *
     * <p>A disabled listener remains registered but does not receive packets.
     *
     * @since 1.0.0
     */
    void disable();

    /**
     * Re-enables a disabled listener.
     *
     * @since 1.0.0
     */
    void enable();

    /**
     * Checks if this listener is enabled.
     *
     * @return true if enabled
     * @since 1.0.0
     */
    boolean isEnabled();

    /**
     * Returns the number of packets this listener has processed.
     *
     * @return the packet count
     * @since 1.0.0
     */
    long getPacketCount();

    /**
     * Returns the number of packets this listener has cancelled.
     *
     * @return the cancelled packet count
     * @since 1.0.0
     */
    long getCancelledCount();

    /**
     * Resets the packet statistics.
     *
     * @since 1.0.0
     */
    void resetStats();

    /**
     * Returns the owning plugin name.
     *
     * @return the plugin name
     * @since 1.0.0
     */
    @NotNull
    String getOwnerPlugin();

    /**
     * Returns a description of this listener.
     *
     * @return the description, or empty string if none
     * @since 1.0.0
     */
    @NotNull
    String getDescription();
}
