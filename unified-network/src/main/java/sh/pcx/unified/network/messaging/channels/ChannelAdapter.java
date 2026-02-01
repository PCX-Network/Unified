/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.channels;

import sh.pcx.unified.messaging.MessagingService.TransportType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Adapter interface for different messaging transports.
 *
 * <p>This interface abstracts the underlying messaging mechanism (BungeeCord,
 * Velocity, Redis, etc.) and provides a unified API for sending and receiving
 * messages.
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link BungeeCordChannelAdapter} - BungeeCord plugin messaging</li>
 *   <li>{@link VelocityChannelAdapter} - Velocity plugin messaging</li>
 *   <li>{@link RedisChannelAdapter} - Redis pub/sub</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface ChannelAdapter extends AutoCloseable {

    /**
     * Returns the transport type for this adapter.
     *
     * @return the transport type
     * @since 1.0.0
     */
    @NotNull
    TransportType transportType();

    /**
     * Connects the adapter to the messaging backend.
     *
     * @return future completing when connected
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> connect();

    /**
     * Disconnects from the messaging backend.
     *
     * @return future completing when disconnected
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> disconnect();

    /**
     * Checks if the adapter is connected.
     *
     * @return true if connected
     * @since 1.0.0
     */
    boolean isConnected();

    /**
     * Registers a channel for messaging.
     *
     * @param channelName the channel name
     * @return future completing when registered
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> registerChannel(@NotNull String channelName);

    /**
     * Unregisters a channel.
     *
     * @param channelName the channel name
     * @return future completing when unregistered
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> unregisterChannel(@NotNull String channelName);

    /**
     * Sends a message to all servers.
     *
     * @param channelName the channel name
     * @param data        the serialized message data
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> broadcast(@NotNull String channelName, byte @NotNull [] data);

    /**
     * Sends a message to a specific server.
     *
     * @param channelName the channel name
     * @param targetServer the target server ID
     * @param data        the serialized message data
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendTo(
            @NotNull String channelName,
            @NotNull String targetServer,
            byte @NotNull [] data
    );

    /**
     * Sends a message to the server where a player is located.
     *
     * @param channelName the channel name
     * @param playerId    the player's UUID
     * @param data        the serialized message data
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToPlayer(
            @NotNull String channelName,
            @NotNull UUID playerId,
            byte @NotNull [] data
    );

    /**
     * Subscribes to messages on a channel.
     *
     * @param channelName the channel name
     * @param handler     handler for received messages
     * @since 1.0.0
     */
    void subscribe(@NotNull String channelName, @NotNull Consumer<ReceivedMessage> handler);

    /**
     * Unsubscribes from a channel.
     *
     * @param channelName the channel name
     * @since 1.0.0
     */
    void unsubscribe(@NotNull String channelName);

    /**
     * Returns the current server ID.
     *
     * @return the server ID
     * @since 1.0.0
     */
    @NotNull
    String serverId();

    /**
     * Returns all known server IDs.
     *
     * @return collection of server IDs
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<String>> serverIds();

    /**
     * Finds which server a player is on.
     *
     * @param playerId the player's UUID
     * @return the server ID, or null if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<String> findPlayerServer(@NotNull UUID playerId);

    /**
     * Transfers a player to another server.
     *
     * @param playerId     the player's UUID
     * @param targetServer the target server
     * @return future completing when transfer is initiated
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> transferPlayer(@NotNull UUID playerId, @NotNull String targetServer);

    /**
     * Kicks a player from the network.
     *
     * @param playerId the player's UUID
     * @param reason   the kick reason (may be null)
     * @return future completing when kicked
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> kickPlayer(@NotNull UUID playerId, String reason);

    /**
     * Gets the player count for a server.
     *
     * @param serverId the server ID
     * @return the player count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> playerCount(@NotNull String serverId);

    /**
     * Gets the total player count across the network.
     *
     * @return the total player count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> networkPlayerCount();

    /**
     * Gets the player UUIDs on a server.
     *
     * @param serverId the server ID
     * @return the player UUIDs
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<UUID>> playersOn(@NotNull String serverId);

    @Override
    default void close() {
        disconnect().join();
    }

    /**
     * Represents a message received from the network.
     *
     * @since 1.0.0
     */
    interface ReceivedMessage {

        /**
         * The channel the message was received on.
         *
         * @return channel name
         * @since 1.0.0
         */
        @NotNull
        String channel();

        /**
         * The source server that sent the message.
         *
         * @return source server ID
         * @since 1.0.0
         */
        @NotNull
        String sourceServer();

        /**
         * The raw message data.
         *
         * @return the data bytes
         * @since 1.0.0
         */
        byte @NotNull [] data();
    }
}
