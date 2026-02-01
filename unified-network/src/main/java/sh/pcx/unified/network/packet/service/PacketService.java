/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet.service;

import sh.pcx.unified.network.packet.Packet;
import sh.pcx.unified.network.packet.PacketType;
import sh.pcx.unified.network.packet.event.PacketEvent;
import sh.pcx.unified.network.packet.listener.PacketListener;
import sh.pcx.unified.network.packet.listener.PacketListenerBuilder;
import sh.pcx.unified.network.packet.listener.ListenerPriority;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Service for low-level packet manipulation with ProtocolLib integration.
 *
 * <p>The PacketService provides a unified API for intercepting, modifying, and
 * sending Minecraft protocol packets. It automatically uses ProtocolLib when
 * available and falls back to native NMS handling otherwise.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private PacketService packets;
 *
 * // Listen to incoming packets
 * packets.listen(PacketType.PLAY_IN_POSITION)
 *     .handler(event -> {
 *         Player player = event.getPlayer();
 *         Packet packet = event.getPacket();
 *         double x = packet.getDoubles().read(0);
 *         // Validate movement...
 *         if (isInvalidMovement(player, x)) {
 *             event.setCancelled(true);
 *         }
 *     })
 *     .priority(ListenerPriority.NORMAL)
 *     .register();
 *
 * // Send packets
 * Packet spawn = packets.createPacket(PacketType.PLAY_OUT_SPAWN_ENTITY);
 * spawn.getIntegers().write(0, entityId);
 * packets.send(player, spawn);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This service is thread-safe. Packet listeners are invoked on the network
 * thread unless configured otherwise.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Packet
 * @see PacketType
 * @see PacketListener
 */
public interface PacketService extends Service {

    // =========================================================================
    // ProtocolLib Integration
    // =========================================================================

    /**
     * Checks if ProtocolLib is available on this server.
     *
     * @return true if ProtocolLib is available
     * @since 1.0.0
     */
    boolean isProtocolLibAvailable();

    /**
     * Returns the ProtocolLib ProtocolManager if available.
     *
     * <p>This allows access to native ProtocolLib features when needed.
     *
     * @param <T> the ProtocolManager type
     * @return the ProtocolManager, or empty if ProtocolLib is not available
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getProtocolManager();

    /**
     * Returns the current packet handling mode.
     *
     * @return the packet handling mode
     * @since 1.0.0
     */
    @NotNull
    PacketMode getMode();

    // =========================================================================
    // Packet Creation
    // =========================================================================

    /**
     * Creates a new packet of the specified type.
     *
     * @param type the packet type
     * @return a new packet instance
     * @since 1.0.0
     */
    @NotNull
    Packet createPacket(@NotNull PacketType type);

    /**
     * Creates a packet from a raw NMS packet object.
     *
     * @param nmsPacket the NMS packet object
     * @return a wrapped packet
     * @since 1.0.0
     */
    @NotNull
    Packet wrapPacket(@NotNull Object nmsPacket);

    /**
     * Creates a clone of an existing packet.
     *
     * @param packet the packet to clone
     * @return a deep copy of the packet
     * @since 1.0.0
     */
    @NotNull
    Packet clonePacket(@NotNull Packet packet);

    // =========================================================================
    // Packet Sending
    // =========================================================================

    /**
     * Sends a packet to a player.
     *
     * @param player the player to send to (Bukkit Player or UUID)
     * @param packet the packet to send
     * @since 1.0.0
     */
    void send(@NotNull Object player, @NotNull Packet packet);

    /**
     * Sends a packet to a player by UUID.
     *
     * @param playerId the player's UUID
     * @param packet   the packet to send
     * @since 1.0.0
     */
    void send(@NotNull UUID playerId, @NotNull Packet packet);

    /**
     * Sends a packet to multiple players.
     *
     * @param players the players to send to
     * @param packet  the packet to send
     * @since 1.0.0
     */
    void broadcast(@NotNull Collection<?> players, @NotNull Packet packet);

    /**
     * Sends a packet to all online players.
     *
     * @param packet the packet to send
     * @since 1.0.0
     */
    void broadcastAll(@NotNull Packet packet);

    /**
     * Sends a packet to players within range of a location.
     *
     * @param location the center location (Bukkit Location)
     * @param range    the range in blocks
     * @param packet   the packet to send
     * @since 1.0.0
     */
    void broadcastNearby(@NotNull Object location, double range, @NotNull Packet packet);

    /**
     * Sends a packet to a player after a delay.
     *
     * @param player     the player to send to
     * @param packet     the packet to send
     * @param delayTicks the delay in ticks
     * @since 1.0.0
     */
    void sendLater(@NotNull Object player, @NotNull Packet packet, long delayTicks);

    /**
     * Sends a packet to a player asynchronously.
     *
     * @param player the player to send to
     * @param packet the packet to send
     * @return a future that completes when the packet is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendAsync(@NotNull Object player, @NotNull Packet packet);

    /**
     * Receives a packet as if it came from the player.
     *
     * <p>This injects a packet into the incoming packet pipeline.
     *
     * @param player the player the packet appears to come from
     * @param packet the packet to inject
     * @since 1.0.0
     */
    void receivePacket(@NotNull Object player, @NotNull Packet packet);

    // =========================================================================
    // Packet Listening
    // =========================================================================

    /**
     * Creates a builder for registering a packet listener.
     *
     * @param type the packet type to listen for
     * @return a listener builder
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder listen(@NotNull PacketType type);

    /**
     * Creates a builder for listening to multiple packet types.
     *
     * @param types the packet types to listen for
     * @return a listener builder
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder listen(@NotNull PacketType... types);

    /**
     * Registers a simple packet listener.
     *
     * @param type    the packet type
     * @param handler the handler function
     * @return the registered listener
     * @since 1.0.0
     */
    @NotNull
    PacketListener addListener(@NotNull PacketType type, @NotNull Consumer<PacketEvent> handler);

    /**
     * Registers a typed listener for receiving packets.
     *
     * <p>The handler receives the player and a typed packet wrapper.
     *
     * @param <T>         the typed packet class
     * @param packetClass the typed packet class
     * @param handler     the handler function
     * @return the registered listener
     * @since 1.0.0
     */
    @NotNull
    <T> PacketListener onReceive(@NotNull Class<T> packetClass, @NotNull BiConsumer<Object, T> handler);

    /**
     * Registers a typed listener for sending packets.
     *
     * <p>The handler can modify or cancel the packet by returning null.
     *
     * @param <T>         the typed packet class
     * @param packetClass the typed packet class
     * @param handler     the handler function (return null to cancel)
     * @return the registered listener
     * @since 1.0.0
     */
    @NotNull
    <T> PacketListener onSend(@NotNull Class<T> packetClass, @NotNull BiFunction<Object, T, @Nullable T> handler);

    /**
     * Unregisters a packet listener.
     *
     * @param listener the listener to unregister
     * @since 1.0.0
     */
    void removeListener(@NotNull PacketListener listener);

    /**
     * Unregisters all listeners for a specific packet type.
     *
     * @param type the packet type
     * @since 1.0.0
     */
    void removeListeners(@NotNull PacketType type);

    /**
     * Unregisters all packet listeners.
     *
     * @since 1.0.0
     */
    void removeAllListeners();

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Gets the entity ID from a packet if present.
     *
     * @param packet the packet
     * @return the entity ID, or empty if not present
     * @since 1.0.0
     */
    @NotNull
    Optional<Integer> getEntityId(@NotNull Packet packet);

    /**
     * Gets the packet type from an NMS packet class.
     *
     * @param nmsPacket the NMS packet object
     * @return the packet type
     * @since 1.0.0
     */
    @NotNull
    PacketType getPacketType(@NotNull Object nmsPacket);

    /**
     * Checks if a packet type is supported on the current server version.
     *
     * @param type the packet type
     * @return true if supported
     * @since 1.0.0
     */
    boolean isSupported(@NotNull PacketType type);

    /**
     * Packet handling mode enumeration.
     *
     * @since 1.0.0
     */
    enum PacketMode {
        /** Using ProtocolLib for packet handling. */
        PROTOCOL_LIB,
        /** Using native NMS for packet handling. */
        NATIVE_NMS,
        /** Using reflection-based NMS handling. */
        REFLECTION,
        /** Packet handling is disabled. */
        DISABLED
    }
}
