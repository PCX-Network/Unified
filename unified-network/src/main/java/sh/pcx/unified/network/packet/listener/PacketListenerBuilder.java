/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet.listener;

import sh.pcx.unified.network.packet.PacketType;
import sh.pcx.unified.network.packet.event.PacketEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Builder for creating and registering packet listeners.
 *
 * <p>This builder provides a fluent API for configuring packet listeners
 * with various options like priority, filtering, and async handling.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Basic listener
 * packets.listen(PacketType.PLAY_IN_POSITION)
 *     .handler(event -> {
 *         // Handle position packet
 *     })
 *     .register();
 *
 * // Full configuration
 * packets.listen(PacketType.PLAY_IN_CHAT, PacketType.PLAY_IN_COMMANDS)
 *     .handler(event -> {
 *         // Handle chat/command packets
 *     })
 *     .priority(ListenerPriority.HIGH)
 *     .filter(event -> event.getPlayer() != null)
 *     .async(true)
 *     .description("Chat filter")
 *     .register();
 *
 * // Player-specific listener
 * packets.listen(PacketType.PLAY_OUT_ENTITY_METADATA)
 *     .handler(event -> {
 *         // Only for specific player
 *     })
 *     .forPlayer(player.getUniqueId())
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PacketListener
 * @see ListenerPriority
 */
public interface PacketListenerBuilder {

    /**
     * Sets the handler function for this listener.
     *
     * @param handler the packet event handler
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder handler(@NotNull Consumer<PacketEvent> handler);

    /**
     * Sets a handler that only processes inbound packets.
     *
     * @param handler the inbound packet handler
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder onReceive(@NotNull Consumer<PacketEvent.Inbound> handler);

    /**
     * Sets a handler that only processes outbound packets.
     *
     * @param handler the outbound packet handler
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder onSend(@NotNull Consumer<PacketEvent.Outbound> handler);

    /**
     * Sets the priority of this listener.
     *
     * @param priority the listener priority
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder priority(@NotNull ListenerPriority priority);

    /**
     * Adds a filter predicate for this listener.
     *
     * <p>The handler is only called if the filter returns true.
     *
     * @param filter the filter predicate
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder filter(@NotNull Predicate<PacketEvent> filter);

    /**
     * Restricts this listener to a specific player.
     *
     * @param playerId the player's UUID
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder forPlayer(@NotNull UUID playerId);

    /**
     * Restricts this listener to multiple specific players.
     *
     * @param playerIds the player UUIDs
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder forPlayers(@NotNull UUID... playerIds);

    /**
     * Sets whether this listener handles packets asynchronously.
     *
     * <p>Async listeners are processed on a worker thread instead of
     * the network thread. This can improve performance for slow handlers.
     *
     * @param async true for async processing
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder async(boolean async);

    /**
     * Enables async processing for this listener.
     *
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    default PacketListenerBuilder async() {
        return async(true);
    }

    /**
     * Sets whether to receive cancelled packets.
     *
     * @param ignoreCancelled true to skip cancelled packets
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder ignoreCancelled(boolean ignoreCancelled);

    /**
     * Sets a description for this listener.
     *
     * <p>Descriptions are useful for debugging and monitoring.
     *
     * @param description the listener description
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder description(@NotNull String description);

    /**
     * Adds additional packet types to listen for.
     *
     * @param types the packet types
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder alsoListen(@NotNull PacketType... types);

    /**
     * Sets whether to include the original packet in events.
     *
     * <p>When enabled, {@link PacketEvent#getOriginalPacket()} returns
     * a copy of the packet before any modifications.
     *
     * @param include true to include original packet
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder includeOriginal(boolean include);

    /**
     * Sets the timeout for async handlers.
     *
     * @param timeoutMs the timeout in milliseconds
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder timeout(long timeoutMs);

    /**
     * Sets an error handler for exceptions in the listener.
     *
     * @param errorHandler the error handler
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    PacketListenerBuilder onError(@NotNull Consumer<Throwable> errorHandler);

    /**
     * Registers the listener and starts receiving packets.
     *
     * @return the registered listener
     * @since 1.0.0
     */
    @NotNull
    PacketListener register();

    /**
     * Builds the listener without registering it.
     *
     * <p>Use this when you want to register the listener later.
     *
     * @return the unregistered listener
     * @since 1.0.0
     */
    @NotNull
    PacketListener build();
}
