/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.core;

import sh.pcx.unified.network.messaging.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a messaging channel for cross-server communication.
 *
 * <p>A channel is a named conduit for messages of a specific type. Messages
 * sent on a channel are only received by listeners registered to that channel.
 * This allows plugins to organize their messaging by topic or purpose.
 *
 * <h2>Channel Naming</h2>
 * <p>Channel names should follow the format: {@code namespace:name}
 * <ul>
 *   <li>{@code myplugin:player_updates} - Player-related updates</li>
 *   <li>{@code myplugin:server_sync} - Server synchronization</li>
 *   <li>{@code unified:system} - Built-in system channel</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get or create a channel
 * MessageChannel<PlayerUpdateMessage> channel = messaging.registerChannel(
 *     "myplugin:players",
 *     PlayerUpdateMessage.class
 * );
 *
 * // Send a message
 * channel.send(new PlayerUpdateMessage(player.getUniqueId(), "connected"));
 *
 * // Send to specific server
 * channel.sendTo("lobby-1", new PlayerUpdateMessage(player.getUniqueId(), "transfer"));
 *
 * // Broadcast to all servers
 * channel.broadcast(new PlayerUpdateMessage(player.getUniqueId(), "level_up"));
 *
 * // Register a listener
 * channel.addListener(message -> {
 *     getLogger().info("Player " + message.getPlayerId() + ": " + message.getAction());
 * });
 *
 * // Filter messages
 * channel.filter(msg -> msg.getAction().equals("connected"))
 *        .addListener(msg -> handlePlayerConnect(msg));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Channels are thread-safe. Messages can be sent from any thread.
 * Listeners are invoked on a configurable executor.
 *
 * @param <T> the type of messages this channel handles
 * @since 1.0.0
 * @author Supatuck
 * @see MessagingService
 * @see ChannelRegistry
 */
public interface MessageChannel<T extends Message> {

    /**
     * Returns the unique name of this channel.
     *
     * @return the channel name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the message class handled by this channel.
     *
     * @return the message class
     * @since 1.0.0
     */
    @NotNull
    Class<T> getMessageClass();

    /**
     * Checks if this channel is open and accepting messages.
     *
     * @return true if the channel is open
     * @since 1.0.0
     */
    boolean isOpen();

    // =========================================================================
    // Sending Messages
    // =========================================================================

    /**
     * Sends a message on this channel.
     *
     * <p>The message is sent as a broadcast to all servers unless
     * it has a specific target server set.
     *
     * @param message the message to send
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> send(@NotNull T message);

    /**
     * Sends a message to a specific server.
     *
     * @param targetServer the target server ID
     * @param message      the message to send
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendTo(@NotNull String targetServer, @NotNull T message);

    /**
     * Broadcasts a message to all servers.
     *
     * @param message the message to broadcast
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> broadcast(@NotNull T message);

    /**
     * Sends a message to the server a player is on.
     *
     * @param playerId the player's UUID
     * @param message  the message to send
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToPlayer(@NotNull UUID playerId, @NotNull T message);

    /**
     * Sends a message to multiple servers.
     *
     * @param servers the target server IDs
     * @param message the message to send
     * @return a future that completes when all messages are sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToMany(@NotNull Collection<String> servers, @NotNull T message);

    /**
     * Sends a message to all servers except the specified ones.
     *
     * @param excludedServers servers to exclude
     * @param message         the message to send
     * @return a future that completes when all messages are sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendExcluding(
            @NotNull Collection<String> excludedServers,
            @NotNull T message
    );

    // =========================================================================
    // Listeners
    // =========================================================================

    /**
     * Adds a listener for messages on this channel.
     *
     * @param listener the listener function
     * @return this channel for chaining
     * @since 1.0.0
     */
    @NotNull
    MessageChannel<T> addListener(@NotNull Consumer<T> listener);

    /**
     * Adds a listener that only receives messages matching a predicate.
     *
     * @param filter   the filter predicate
     * @param listener the listener function
     * @return this channel for chaining
     * @since 1.0.0
     */
    @NotNull
    MessageChannel<T> addListener(@NotNull Predicate<T> filter, @NotNull Consumer<T> listener);

    /**
     * Removes a listener from this channel.
     *
     * @param listener the listener to remove
     * @return true if the listener was removed
     * @since 1.0.0
     */
    boolean removeListener(@NotNull Consumer<T> listener);

    /**
     * Removes all listeners from this channel.
     *
     * @since 1.0.0
     */
    void clearListeners();

    /**
     * Returns the number of registered listeners.
     *
     * @return the listener count
     * @since 1.0.0
     */
    int getListenerCount();

    // =========================================================================
    // Filtering
    // =========================================================================

    /**
     * Creates a filtered view of this channel.
     *
     * <p>The returned channel only receives messages that match the predicate.
     * This is useful for creating specialized handlers.
     *
     * @param filter the filter predicate
     * @return a filtered channel view
     * @since 1.0.0
     */
    @NotNull
    MessageChannel<T> filter(@NotNull Predicate<T> filter);

    /**
     * Creates a filtered view that only receives messages from a specific server.
     *
     * @param sourceServer the source server to filter by
     * @return a filtered channel view
     * @since 1.0.0
     */
    @NotNull
    default MessageChannel<T> fromServer(@NotNull String sourceServer) {
        return filter(msg -> msg.getSourceServer().equals(sourceServer));
    }

    /**
     * Creates a filtered view that only receives broadcast messages.
     *
     * @return a filtered channel view
     * @since 1.0.0
     */
    @NotNull
    default MessageChannel<T> broadcastsOnly() {
        return filter(Message::isBroadcast);
    }

    /**
     * Creates a filtered view that only receives targeted messages.
     *
     * @return a filtered channel view
     * @since 1.0.0
     */
    @NotNull
    default MessageChannel<T> targetedOnly() {
        return filter(msg -> !msg.isBroadcast());
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Closes this channel.
     *
     * <p>After closing, no more messages can be sent or received on this channel.
     * All listeners are removed.
     *
     * @since 1.0.0
     */
    void close();

    /**
     * Returns statistics about this channel.
     *
     * @return the channel statistics
     * @since 1.0.0
     */
    @NotNull
    ChannelStats getStats();

    /**
     * Statistics for a message channel.
     *
     * @since 1.0.0
     */
    interface ChannelStats {
        /**
         * Returns the total number of messages sent.
         *
         * @return messages sent count
         */
        long getMessagesSent();

        /**
         * Returns the total number of messages received.
         *
         * @return messages received count
         */
        long getMessagesReceived();

        /**
         * Returns the number of bytes sent.
         *
         * @return bytes sent
         */
        long getBytesSent();

        /**
         * Returns the number of bytes received.
         *
         * @return bytes received
         */
        long getBytesReceived();

        /**
         * Returns the average message size in bytes.
         *
         * @return average message size
         */
        double getAverageMessageSize();

        /**
         * Returns the number of errors encountered.
         *
         * @return error count
         */
        long getErrors();

        /**
         * Returns the last error message, if any.
         *
         * @return the last error, or null if no errors
         */
        String getLastError();

        /**
         * Resets all statistics to zero.
         */
        void reset();
    }
}
