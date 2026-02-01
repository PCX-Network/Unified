/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.core;

import sh.pcx.unified.network.messaging.channels.ChannelAdapter;
import sh.pcx.unified.network.messaging.messages.Message;
import sh.pcx.unified.network.messaging.messages.MessageSerializer;
import sh.pcx.unified.network.messaging.patterns.Broadcast;
import sh.pcx.unified.network.messaging.patterns.PlayerMessage;
import sh.pcx.unified.network.messaging.patterns.RequestResponse;
import sh.pcx.unified.network.messaging.patterns.TargetedMessage;
import sh.pcx.unified.network.messaging.util.ServerInfo;
import sh.pcx.unified.network.messaging.util.ServerList;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Main service interface for cross-server messaging.
 *
 * <p>The MessagingService provides a unified API for sending messages between
 * servers in a Minecraft network. It abstracts the underlying transport mechanism
 * (BungeeCord, Velocity, or Redis) and provides high-level patterns for common
 * messaging use cases.
 *
 * <h2>Supported Transports</h2>
 * <ul>
 *   <li><strong>BungeeCord</strong> - Plugin messaging via BungeeCord proxy</li>
 *   <li><strong>Velocity</strong> - Plugin messaging via Velocity proxy</li>
 *   <li><strong>Redis</strong> - Pub/sub messaging for custom networks</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the messaging service
 * MessagingService messaging = services.get(MessagingService.class).orElseThrow();
 *
 * // Register a channel for your plugin
 * MessageChannel<MyMessage> channel = messaging.registerChannel(
 *     "myplugin:updates",
 *     MyMessage.class
 * );
 *
 * // Send a broadcast message
 * messaging.broadcast(new ServerAnnouncement("Hello, network!"));
 *
 * // Send to a specific server
 * messaging.sendTo("lobby-1", new PlayerTransfer(player.getUniqueId()));
 *
 * // Request/response pattern
 * CompletableFuture<PlayerCountResponse> response = messaging.request(
 *     "hub",
 *     new PlayerCountRequest(),
 *     PlayerCountResponse.class,
 *     Duration.ofSeconds(5)
 * );
 *
 * response.thenAccept(count ->
 *     getLogger().info("Hub has " + count.getCount() + " players")
 * );
 *
 * // Register a message handler
 * messaging.registerHandler(PlayerUpdateMessage.class, message -> {
 *     getLogger().info("Player " + message.getPlayerId() + " changed servers");
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This service is thread-safe. All operations can be called from any thread.
 * Message handlers are invoked on a configurable executor.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessageChannel
 * @see ChannelRegistry
 * @see Message
 */
public interface MessagingService extends Service {

    /**
     * Returns the ID of the current server.
     *
     * <p>This is used as the source server for outgoing messages.
     *
     * @return the current server ID
     * @since 1.0.0
     */
    @NotNull
    String getServerId();

    /**
     * Returns information about the current server.
     *
     * @return the current server info
     * @since 1.0.0
     */
    @NotNull
    ServerInfo getServerInfo();

    /**
     * Returns the channel registry for managing channels.
     *
     * @return the channel registry
     * @since 1.0.0
     */
    @NotNull
    ChannelRegistry getChannelRegistry();

    /**
     * Returns the message serializer used by this service.
     *
     * @return the message serializer
     * @since 1.0.0
     */
    @NotNull
    MessageSerializer getSerializer();

    /**
     * Returns the server list service for discovering servers.
     *
     * @return the server list
     * @since 1.0.0
     */
    @NotNull
    ServerList getServerList();

    /**
     * Returns the current channel adapter in use.
     *
     * @return the channel adapter
     * @since 1.0.0
     */
    @NotNull
    ChannelAdapter getAdapter();

    // =========================================================================
    // Channel Registration
    // =========================================================================

    /**
     * Registers a new messaging channel.
     *
     * @param <T>          the message type for this channel
     * @param channelName  the unique channel name (e.g., "myplugin:updates")
     * @param messageClass the message class
     * @return the registered channel
     * @throws IllegalArgumentException if channel name is already registered
     * @since 1.0.0
     */
    @NotNull
    <T extends Message> MessageChannel<T> registerChannel(
            @NotNull String channelName,
            @NotNull Class<T> messageClass
    );

    /**
     * Gets an existing channel by name.
     *
     * @param <T>         the message type
     * @param channelName the channel name
     * @return the channel, or empty if not registered
     * @since 1.0.0
     */
    @NotNull
    <T extends Message> Optional<MessageChannel<T>> getChannel(@NotNull String channelName);

    /**
     * Unregisters a channel.
     *
     * @param channelName the channel name
     * @return true if the channel was unregistered
     * @since 1.0.0
     */
    boolean unregisterChannel(@NotNull String channelName);

    // =========================================================================
    // Message Sending
    // =========================================================================

    /**
     * Sends a message to a specific server.
     *
     * @param targetServer the target server ID
     * @param message      the message to send
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendTo(@NotNull String targetServer, @NotNull Message message);

    /**
     * Broadcasts a message to all servers.
     *
     * @param message the message to broadcast
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> broadcast(@NotNull Message message);

    /**
     * Sends a message to the server a player is on.
     *
     * @param playerId the player's UUID
     * @param message  the message to send
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendToPlayer(@NotNull UUID playerId, @NotNull Message message);

    /**
     * Creates a targeted message helper for a specific server.
     *
     * @param targetServer the target server ID
     * @return a targeted message helper
     * @since 1.0.0
     */
    @NotNull
    TargetedMessage targetServer(@NotNull String targetServer);

    /**
     * Creates a broadcast helper for sending to all servers.
     *
     * @return a broadcast helper
     * @since 1.0.0
     */
    @NotNull
    Broadcast broadcastAll();

    /**
     * Creates a player message helper for following a player.
     *
     * @param playerId the player's UUID
     * @return a player message helper
     * @since 1.0.0
     */
    @NotNull
    PlayerMessage followPlayer(@NotNull UUID playerId);

    // =========================================================================
    // Request/Response
    // =========================================================================

    /**
     * Sends a request and waits for a response.
     *
     * @param <R>           the response type
     * @param targetServer  the target server ID
     * @param request       the request message
     * @param responseClass the expected response class
     * @return a future containing the response
     * @since 1.0.0
     */
    @NotNull
    <R extends Message> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull Message request,
            @NotNull Class<R> responseClass
    );

    /**
     * Sends a request with a custom timeout.
     *
     * @param <R>           the response type
     * @param targetServer  the target server ID
     * @param request       the request message
     * @param responseClass the expected response class
     * @param timeout       the timeout duration
     * @return a future containing the response
     * @since 1.0.0
     */
    @NotNull
    <R extends Message> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull Message request,
            @NotNull Class<R> responseClass,
            @NotNull Duration timeout
    );

    /**
     * Creates a request/response helper for complex request patterns.
     *
     * @param <R>           the response type
     * @param responseClass the expected response class
     * @return a request/response helper
     * @since 1.0.0
     */
    @NotNull
    <R extends Message> RequestResponse<R> prepareRequest(@NotNull Class<R> responseClass);

    // =========================================================================
    // Message Handlers
    // =========================================================================

    /**
     * Registers a handler for a message type.
     *
     * @param <T>          the message type
     * @param messageClass the message class to handle
     * @param handler      the handler function
     * @since 1.0.0
     */
    <T extends Message> void registerHandler(
            @NotNull Class<T> messageClass,
            @NotNull Consumer<T> handler
    );

    /**
     * Registers a handler for a message type with a specific channel.
     *
     * @param <T>          the message type
     * @param channelName  the channel to listen on
     * @param messageClass the message class to handle
     * @param handler      the handler function
     * @since 1.0.0
     */
    <T extends Message> void registerHandler(
            @NotNull String channelName,
            @NotNull Class<T> messageClass,
            @NotNull Consumer<T> handler
    );

    /**
     * Unregisters a handler for a message type.
     *
     * @param <T>          the message type
     * @param messageClass the message class
     * @return true if a handler was unregistered
     * @since 1.0.0
     */
    <T extends Message> boolean unregisterHandler(@NotNull Class<T> messageClass);

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Connects the messaging service.
     *
     * <p>This is called automatically during plugin enable.
     *
     * @return a future that completes when connected
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> connect();

    /**
     * Disconnects the messaging service.
     *
     * <p>This is called automatically during plugin disable.
     *
     * @return a future that completes when disconnected
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> disconnect();

    /**
     * Checks if the messaging service is connected.
     *
     * @return true if connected
     * @since 1.0.0
     */
    boolean isConnected();

    /**
     * Returns the default timeout for requests.
     *
     * @return the default timeout duration
     * @since 1.0.0
     */
    @NotNull
    Duration getDefaultTimeout();

    /**
     * Sets the default timeout for requests.
     *
     * @param timeout the timeout duration
     * @since 1.0.0
     */
    void setDefaultTimeout(@NotNull Duration timeout);
}
