/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.messaging;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a typed messaging channel for cross-server communication.
 *
 * <p>A channel is a named conduit for messages of a specific type. Messages
 * sent on a channel are only received by listeners registered to that channel.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a channel for a message type
 * MessageChannel<PlayerUpdateMessage> channel = messaging.channel(PlayerUpdateMessage.class);
 *
 * // Send a message
 * channel.send(new PlayerUpdateMessage(playerId, "connected"));
 *
 * // Send to specific server
 * channel.sendTo("lobby-1", new PlayerUpdateMessage(playerId, "transfer"));
 *
 * // Broadcast to all servers
 * channel.broadcast(new PlayerUpdateMessage(playerId, "level_up"));
 *
 * // Subscribe to messages
 * channel.subscribe(message -> {
 *     log.info("Player update: " + message);
 * });
 *
 * // Subscribe with filter
 * channel.filter(msg -> "connected".equals(msg.action()))
 *        .subscribe(msg -> handleConnect(msg));
 * }</pre>
 *
 * @param <T> the message type
 * @since 1.0.0
 * @author Supatuck
 * @see MessagingService
 * @see Message
 */
public interface MessageChannel<T> {

    /**
     * Returns the channel name.
     *
     * @return the channel name (e.g., "myplugin:events")
     * @since 1.0.0
     */
    @NotNull
    String name();

    /**
     * Returns the message class for this channel.
     *
     * @return the message class
     * @since 1.0.0
     */
    @NotNull
    Class<T> messageType();

    /**
     * Checks if the channel is open and accepting messages.
     *
     * @return true if open
     * @since 1.0.0
     */
    boolean isOpen();

    // =========================================================================
    // Sending
    // =========================================================================

    /**
     * Sends a message on this channel.
     *
     * <p>The message is sent as a broadcast unless a target is specified.
     *
     * @param message the message to send
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> send(@NotNull T message);

    /**
     * Sends a message to a specific server.
     *
     * @param server  the target server ID
     * @param message the message
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendTo(@NotNull String server, @NotNull T message);

    /**
     * Broadcasts a message to all servers.
     *
     * @param message the message
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> broadcast(@NotNull T message);

    /**
     * Sends a message to the server a player is on.
     *
     * @param playerId the player's UUID
     * @param message  the message
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToPlayer(@NotNull UUID playerId, @NotNull T message);

    /**
     * Sends a message to multiple servers.
     *
     * @param servers the target server IDs
     * @param message the message
     * @return future completing when all sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToMany(@NotNull Collection<String> servers, @NotNull T message);

    /**
     * Sends a message to all servers except specified ones.
     *
     * @param excludedServers servers to exclude
     * @param message         the message
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendExcluding(@NotNull Collection<String> excludedServers, @NotNull T message);

    // =========================================================================
    // Request/Response
    // =========================================================================

    /**
     * Sends a request and waits for a response.
     *
     * @param <R>           response type
     * @param targetServer  the server to send to
     * @param request       the request message
     * @param responseType  expected response class
     * @return future with the response
     * @since 1.0.0
     */
    @NotNull
    <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull T request,
            @NotNull Class<R> responseType
    );

    /**
     * Sends a request with a custom timeout.
     *
     * @param <R>           response type
     * @param targetServer  the server to send to
     * @param request       the request message
     * @param responseType  expected response class
     * @param timeout       the timeout duration
     * @return future with the response
     * @since 1.0.0
     */
    @NotNull
    <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull T request,
            @NotNull Class<R> responseType,
            @NotNull Duration timeout
    );

    // =========================================================================
    // Subscribing
    // =========================================================================

    /**
     * Subscribes to messages on this channel.
     *
     * @param handler the message handler
     * @return subscription that can be cancelled
     * @since 1.0.0
     */
    @NotNull
    Subscription subscribe(@NotNull Consumer<T> handler);

    /**
     * Subscribes to messages matching a predicate.
     *
     * @param filter  the filter predicate
     * @param handler the message handler
     * @return subscription that can be cancelled
     * @since 1.0.0
     */
    @NotNull
    Subscription subscribe(@NotNull Predicate<T> filter, @NotNull Consumer<T> handler);

    /**
     * Subscribes to messages from a specific server.
     *
     * @param sourceServer the source server ID
     * @param handler      the message handler
     * @return subscription that can be cancelled
     * @since 1.0.0
     */
    @NotNull
    default Subscription subscribeFrom(@NotNull String sourceServer, @NotNull Consumer<T> handler) {
        return subscribe(msg -> {
            if (msg instanceof NetworkMessage nm && sourceServer.equals(nm.sourceServer())) {
                handler.accept(msg);
            } else {
                handler.accept(msg); // Non-NetworkMessage, accept all
            }
        });
    }

    // =========================================================================
    // Filtering
    // =========================================================================

    /**
     * Creates a filtered view of this channel.
     *
     * @param filter the filter predicate
     * @return filtered channel view
     * @since 1.0.0
     */
    @NotNull
    MessageChannel<T> filter(@NotNull Predicate<T> filter);

    /**
     * Creates a filtered view for messages from a specific server.
     *
     * @param sourceServer the source server
     * @return filtered channel view
     * @since 1.0.0
     */
    @NotNull
    MessageChannel<T> fromServer(@NotNull String sourceServer);

    /**
     * Creates a filtered view for broadcast messages only.
     *
     * @return filtered channel view
     * @since 1.0.0
     */
    @NotNull
    MessageChannel<T> broadcastsOnly();

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Closes this channel.
     *
     * <p>After closing, no more messages can be sent or received.
     *
     * @since 1.0.0
     */
    void close();

    /**
     * Returns statistics for this channel.
     *
     * @return the channel statistics
     * @since 1.0.0
     */
    @NotNull
    ChannelStats stats();

    /**
     * Represents a message subscription that can be cancelled.
     *
     * @since 1.0.0
     */
    interface Subscription extends AutoCloseable {

        /**
         * Checks if this subscription is active.
         *
         * @return true if active
         * @since 1.0.0
         */
        boolean isActive();

        /**
         * Cancels this subscription.
         *
         * @since 1.0.0
         */
        void cancel();

        @Override
        default void close() {
            cancel();
        }
    }

    /**
     * Statistics for a message channel.
     *
     * @since 1.0.0
     */
    interface ChannelStats {

        /**
         * Total messages sent on this channel.
         *
         * @return sent count
         * @since 1.0.0
         */
        long messagesSent();

        /**
         * Total messages received on this channel.
         *
         * @return received count
         * @since 1.0.0
         */
        long messagesReceived();

        /**
         * Total bytes sent.
         *
         * @return bytes sent
         * @since 1.0.0
         */
        long bytesSent();

        /**
         * Total bytes received.
         *
         * @return bytes received
         * @since 1.0.0
         */
        long bytesReceived();

        /**
         * Number of active subscriptions.
         *
         * @return subscription count
         * @since 1.0.0
         */
        int subscriptionCount();

        /**
         * Number of errors encountered.
         *
         * @return error count
         * @since 1.0.0
         */
        long errorCount();

        /**
         * Resets all statistics.
         *
         * @since 1.0.0
         */
        void reset();
    }
}
