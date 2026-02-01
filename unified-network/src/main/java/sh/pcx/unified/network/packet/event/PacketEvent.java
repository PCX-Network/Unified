/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet.event;

import sh.pcx.unified.network.packet.Packet;
import sh.pcx.unified.network.packet.PacketType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when a packet is sent or received.
 *
 * <p>This event allows listeners to inspect, modify, or cancel packets
 * as they flow through the network pipeline.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * packets.listen(PacketType.PLAY_IN_POSITION)
 *     .handler(event -> {
 *         // Get the player
 *         Player player = (Player) event.getPlayer();
 *
 *         // Read packet data
 *         Packet packet = event.getPacket();
 *         double x = packet.getDoubles().read(0);
 *         double y = packet.getDoubles().read(1);
 *         double z = packet.getDoubles().read(2);
 *
 *         // Modify packet
 *         packet.getDoubles().write(1, Math.max(y, 0));
 *
 *         // Or cancel the packet
 *         if (isInvalidMovement(x, y, z)) {
 *             event.setCancelled(true);
 *         }
 *     })
 *     .register();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Packet events are typically fired on the Netty IO thread. Modifications
 * to game state should be scheduled on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Packet
 * @see PacketType
 */
public sealed interface PacketEvent permits PacketEvent.Inbound, PacketEvent.Outbound {

    /**
     * Returns the player associated with this packet.
     *
     * <p>For Bukkit servers, this returns the Bukkit Player object.
     *
     * @param <T> the player type
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    <T> T getPlayer();

    /**
     * Returns the player's UUID.
     *
     * @return the player UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getPlayerId();

    /**
     * Returns the packet being processed.
     *
     * @return the packet
     * @since 1.0.0
     */
    @NotNull
    Packet getPacket();

    /**
     * Returns the type of the packet.
     *
     * @return the packet type
     * @since 1.0.0
     */
    @NotNull
    PacketType getPacketType();

    /**
     * Checks if this event has been cancelled.
     *
     * @return true if cancelled
     * @since 1.0.0
     */
    boolean isCancelled();

    /**
     * Sets whether this event is cancelled.
     *
     * <p>Cancelled packets are not processed further.
     *
     * @param cancelled true to cancel
     * @since 1.0.0
     */
    void setCancelled(boolean cancelled);

    /**
     * Checks if this is an async packet event.
     *
     * <p>Async events are processed off the main thread.
     *
     * @return true if async
     * @since 1.0.0
     */
    boolean isAsync();

    /**
     * Returns the direction of this packet.
     *
     * @return the packet direction
     * @since 1.0.0
     */
    @NotNull
    PacketType.PacketDirection getDirection();

    /**
     * Replaces the packet with a new one.
     *
     * @param packet the new packet
     * @since 1.0.0
     */
    void setPacket(@NotNull Packet packet);

    /**
     * Returns the original unmodified packet.
     *
     * @return the original packet
     * @since 1.0.0
     */
    @NotNull
    Packet getOriginalPacket();

    /**
     * Checks if the packet has been modified.
     *
     * @return true if modified
     * @since 1.0.0
     */
    boolean isModified();

    /**
     * Returns any exception that occurred during packet handling.
     *
     * @return the exception, or null if none
     * @since 1.0.0
     */
    @Nullable
    Throwable getException();

    /**
     * Sets an exception that occurred during handling.
     *
     * @param exception the exception
     * @since 1.0.0
     */
    void setException(@NotNull Throwable exception);

    /**
     * Event for incoming packets (client to server).
     *
     * @since 1.0.0
     */
    non-sealed interface Inbound extends PacketEvent {

        /**
         * {@inheritDoc}
         *
         * @return always {@link PacketType.PacketDirection#INBOUND}
         */
        @Override
        @NotNull
        default PacketType.PacketDirection getDirection() {
            return PacketType.PacketDirection.INBOUND;
        }

        /**
         * Returns the raw bytes of the packet if available.
         *
         * @return the raw bytes, or null if not available
         * @since 1.0.0
         */
        @Nullable
        byte[] getRawBytes();
    }

    /**
     * Event for outgoing packets (server to client).
     *
     * @since 1.0.0
     */
    non-sealed interface Outbound extends PacketEvent {

        /**
         * {@inheritDoc}
         *
         * @return always {@link PacketType.PacketDirection#OUTBOUND}
         */
        @Override
        @NotNull
        default PacketType.PacketDirection getDirection() {
            return PacketType.PacketDirection.OUTBOUND;
        }

        /**
         * Schedules the packet to be sent later instead of immediately.
         *
         * @param delayTicks the delay in ticks
         * @since 1.0.0
         */
        void delay(long delayTicks);

        /**
         * Returns the scheduled delay in ticks.
         *
         * @return the delay, or 0 if not delayed
         * @since 1.0.0
         */
        long getDelay();

        /**
         * Checks if this packet should skip other listeners.
         *
         * @return true if skipping listeners
         * @since 1.0.0
         */
        boolean isSkippingListeners();

        /**
         * Sets whether to skip remaining listeners.
         *
         * @param skip true to skip
         * @since 1.0.0
         */
        void setSkipListeners(boolean skip);
    }
}
