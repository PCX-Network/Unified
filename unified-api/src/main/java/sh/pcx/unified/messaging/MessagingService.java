/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.messaging;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for cross-server messaging in a Minecraft network.
 *
 * <p>The MessagingService provides a unified API for sending messages between
 * servers. It abstracts the underlying transport (BungeeCord, Velocity, or Redis)
 * and provides high-level patterns for common messaging use cases.
 *
 * <h2>Supported Transports</h2>
 * <ul>
 *   <li><strong>BungeeCord</strong> - Plugin messaging via BungeeCord proxy</li>
 *   <li><strong>Velocity</strong> - Plugin messaging via Velocity proxy</li>
 *   <li><strong>Redis</strong> - Pub/sub messaging for custom networks</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Define message type
 * @Message(channel = "mynetwork:games")
 * public record GameStartMessage(
 *     String arenaId,
 *     List<UUID> players,
 *     Instant startTime
 * ) {}
 *
 * // Get the messaging service
 * MessagingService messaging = services.get(MessagingService.class).orElseThrow();
 *
 * // Send a message
 * messaging.send(new GameStartMessage("arena1", players, Instant.now()));
 *
 * // Receive messages
 * messaging.subscribe(GameStartMessage.class, message -> {
 *     log.info("Game starting in " + message.arenaId());
 * });
 *
 * // Request/Response
 * messaging.request(new PlayerDataRequest(uuid))
 *     .thenAccept(response -> {
 *         PlayerData data = response.getData();
 *     });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This service is thread-safe. All operations can be called from any thread.
 * Message handlers are invoked on a configurable executor.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Message
 * @see MessageChannel
 * @see ServerInfo
 */
public interface MessagingService extends Service {

    /**
     * Returns the current server's ID.
     *
     * @return the server ID
     * @since 1.0.0
     */
    @NotNull
    String serverId();

    /**
     * Returns information about the current server.
     *
     * @return server information
     * @since 1.0.0
     */
    @NotNull
    ServerInfo serverInfo();

    /**
     * Returns information about all known servers.
     *
     * @return collection of server information
     * @since 1.0.0
     */
    @NotNull
    Collection<ServerInfo> servers();

    /**
     * Returns information about a specific server.
     *
     * @param serverId the server ID
     * @return server info, or empty if unknown
     * @since 1.0.0
     */
    @NotNull
    Optional<ServerInfo> server(@NotNull String serverId);

    // =========================================================================
    // Channels
    // =========================================================================

    /**
     * Gets or creates a channel for a message type.
     *
     * <p>The channel name is derived from the {@link Message} annotation.
     *
     * @param <T>          the message type
     * @param messageClass the message class
     * @return the channel
     * @throws IllegalArgumentException if class lacks @Message annotation
     * @since 1.0.0
     */
    @NotNull
    <T> MessageChannel<T> channel(@NotNull Class<T> messageClass);

    /**
     * Gets or creates a channel by name.
     *
     * @param <T>          the message type
     * @param channelName  the channel name
     * @param messageClass the message class
     * @return the channel
     * @since 1.0.0
     */
    @NotNull
    <T> MessageChannel<T> channel(@NotNull String channelName, @NotNull Class<T> messageClass);

    /**
     * Closes a channel.
     *
     * @param channelName the channel name
     * @return true if the channel was closed
     * @since 1.0.0
     */
    boolean closeChannel(@NotNull String channelName);

    /**
     * Returns all registered channel names.
     *
     * @return channel names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> channelNames();

    // =========================================================================
    // Sending
    // =========================================================================

    /**
     * Sends a message using the channel from its {@link Message} annotation.
     *
     * @param message the message to send
     * @return future completing when sent
     * @throws IllegalArgumentException if message class lacks @Message annotation
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> send(@NotNull Object message);

    /**
     * Sends a message to a specific server.
     *
     * @param targetServer the target server ID
     * @param message      the message
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendTo(@NotNull String targetServer, @NotNull Object message);

    /**
     * Broadcasts a message to all servers.
     *
     * @param message the message
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> broadcast(@NotNull Object message);

    /**
     * Sends a message to the server where a player is located.
     *
     * @param playerId the player's UUID
     * @param message  the message
     * @return future completing when sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToPlayer(@NotNull UUID playerId, @NotNull Object message);

    /**
     * Sends a message to multiple servers.
     *
     * @param servers the target server IDs
     * @param message the message
     * @return future completing when all sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToMany(@NotNull Collection<String> servers, @NotNull Object message);

    // =========================================================================
    // Request/Response
    // =========================================================================

    /**
     * Sends a request and awaits a response.
     *
     * <p>The response type is determined from the {@link Message#responseType()}.
     *
     * @param <R>     the response type
     * @param request the request message
     * @return future with the response
     * @since 1.0.0
     */
    @NotNull
    <R> CompletableFuture<R> request(@NotNull Object request);

    /**
     * Sends a request to a specific server and awaits a response.
     *
     * @param <R>          the response type
     * @param targetServer the target server
     * @param request      the request message
     * @return future with the response
     * @since 1.0.0
     */
    @NotNull
    <R> CompletableFuture<R> request(@NotNull String targetServer, @NotNull Object request);

    /**
     * Sends a request with a custom timeout.
     *
     * @param <R>          the response type
     * @param targetServer the target server
     * @param request      the request message
     * @param timeout      the timeout duration
     * @return future with the response
     * @since 1.0.0
     */
    @NotNull
    <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull Object request,
            @NotNull Duration timeout
    );

    /**
     * Sends a request with explicit response type.
     *
     * @param <R>           the response type
     * @param targetServer  the target server
     * @param request       the request message
     * @param responseClass the expected response class
     * @param timeout       the timeout duration
     * @return future with the response
     * @since 1.0.0
     */
    @NotNull
    <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull Object request,
            @NotNull Class<R> responseClass,
            @NotNull Duration timeout
    );

    // =========================================================================
    // Subscribing
    // =========================================================================

    /**
     * Subscribes to messages of a specific type.
     *
     * @param <T>          the message type
     * @param messageClass the message class
     * @param handler      the message handler
     * @return subscription that can be cancelled
     * @since 1.0.0
     */
    @NotNull
    <T> MessageChannel.Subscription subscribe(
            @NotNull Class<T> messageClass,
            @NotNull Consumer<T> handler
    );

    /**
     * Registers a request handler for responding to requests.
     *
     * @param <T>          the request type
     * @param <R>          the response type
     * @param requestClass the request class
     * @param handler      the handler that returns a response
     * @return subscription that can be cancelled
     * @since 1.0.0
     */
    @NotNull
    <T, R> MessageChannel.Subscription handleRequests(
            @NotNull Class<T> requestClass,
            @NotNull RequestHandler<T, R> handler
    );

    // =========================================================================
    // Player Transfer
    // =========================================================================

    /**
     * Returns the player transfer utilities.
     *
     * @return the transfer service
     * @since 1.0.0
     */
    @NotNull
    PlayerTransfer transfers();

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Returns the default timeout for requests.
     *
     * @return the default timeout
     * @since 1.0.0
     */
    @NotNull
    Duration defaultTimeout();

    /**
     * Sets the default timeout for requests.
     *
     * @param timeout the timeout duration
     * @since 1.0.0
     */
    void setDefaultTimeout(@NotNull Duration timeout);

    /**
     * Returns the transport type in use.
     *
     * @return the transport type
     * @since 1.0.0
     */
    @NotNull
    TransportType transportType();

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Connects the messaging service.
     *
     * <p>Called automatically during plugin enable.
     *
     * @return future completing when connected
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> connect();

    /**
     * Disconnects the messaging service.
     *
     * <p>Called automatically during plugin disable.
     *
     * @return future completing when disconnected
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> disconnect();

    /**
     * Checks if the service is connected.
     *
     * @return true if connected
     * @since 1.0.0
     */
    boolean isConnected();

    /**
     * Handler for processing requests and returning responses.
     *
     * @param <T> request type
     * @param <R> response type
     * @since 1.0.0
     */
    @FunctionalInterface
    interface RequestHandler<T, R> {

        /**
         * Handles a request and returns a response.
         *
         * @param request the request message
         * @return the response, or a future containing the response
         * @since 1.0.0
         */
        @NotNull
        Object handle(@NotNull T request);
    }

    /**
     * Transport types for cross-server messaging.
     *
     * @since 1.0.0
     */
    enum TransportType {
        /**
         * BungeeCord plugin messaging.
         */
        BUNGEECORD,

        /**
         * Velocity plugin messaging.
         */
        VELOCITY,

        /**
         * Redis pub/sub messaging.
         */
        REDIS,

        /**
         * RabbitMQ messaging.
         */
        RABBITMQ,

        /**
         * Custom/unknown transport.
         */
        CUSTOM
    }
}
