/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.channels;

import sh.pcx.unified.messaging.MessagingService.TransportType;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Channel adapter for BungeeCord plugin messaging.
 *
 * <p>This adapter uses BungeeCord's plugin messaging API to communicate
 * between servers connected to a BungeeCord proxy.
 *
 * <h2>BungeeCord Channel Protocol</h2>
 * <p>Messages are sent over the "BungeeCord" channel using the standard
 * BungeeCord plugin message format:
 * <pre>
 * Forward / ForwardToPlayer:
 *   [SubChannel: UTF]
 *   [Target Server / Player: UTF]
 *   [Channel Name: UTF]
 *   [Message Length: Short]
 *   [Message Data: Bytes]
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create the adapter
 * BungeeCordChannelAdapter adapter = new BungeeCordChannelAdapter("survival-1", plugin);
 *
 * // Connect
 * adapter.connect().join();
 *
 * // Subscribe to messages
 * adapter.subscribe("mynetwork:events", message -> {
 *     // Handle incoming message
 * });
 *
 * // Send a message
 * adapter.broadcast("mynetwork:events", messageData);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class BungeeCordChannelAdapter extends AbstractChannelAdapter {

    /**
     * The BungeeCord plugin channel name.
     */
    public static final String BUNGEE_CHANNEL = "BungeeCord";

    /**
     * Prefix for custom channels.
     */
    private static final String CHANNEL_PREFIX = "unified:";

    private final Object plugin;
    private final PluginMessageSender sender;
    private final Map<String, CompletableFuture<?>> pendingRequests;
    private Consumer<byte[]> incomingMessageHandler;

    /**
     * Creates a new BungeeCord channel adapter.
     *
     * @param serverId the current server ID
     * @param plugin   the plugin instance
     * @param sender   the plugin message sender
     */
    public BungeeCordChannelAdapter(
            @NotNull String serverId,
            @NotNull Object plugin,
            @NotNull PluginMessageSender sender
    ) {
        super(serverId);
        this.plugin = plugin;
        this.sender = sender;
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    @Override
    @NotNull
    public TransportType transportType() {
        return TransportType.BUNGEECORD;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Register the BungeeCord channel
                sender.registerOutgoing(BUNGEE_CHANNEL);
                sender.registerIncoming(BUNGEE_CHANNEL, this::handleIncoming);
                connected.set(true);
                logger.info("Connected to BungeeCord messaging");
            } catch (Exception e) {
                logger.error("Failed to connect to BungeeCord messaging", e);
                throw new RuntimeException("Failed to connect", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            connected.set(false);
            pendingRequests.values().forEach(f -> f.cancel(true));
            pendingRequests.clear();
            logger.info("Disconnected from BungeeCord messaging");
        }, executor);
    }

    @Override
    @NotNull
    protected CompletableFuture<Void> doRegisterChannel(@NotNull String channelName) {
        // BungeeCord uses a single channel with sub-channels
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @NotNull
    protected CompletableFuture<Void> doUnregisterChannel(@NotNull String channelName) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> broadcast(@NotNull String channelName, byte @NotNull [] data) {
        ensureConnected();
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] packet = buildForwardPacket("ALL", channelName, data);
                sender.send(BUNGEE_CHANNEL, packet);
            } catch (IOException e) {
                throw new RuntimeException("Failed to broadcast message", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendTo(
            @NotNull String channelName,
            @NotNull String targetServer,
            byte @NotNull [] data
    ) {
        ensureConnected();
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] packet = buildForwardPacket(targetServer, channelName, data);
                sender.send(BUNGEE_CHANNEL, packet);
            } catch (IOException e) {
                throw new RuntimeException("Failed to send message", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendToPlayer(
            @NotNull String channelName,
            @NotNull UUID playerId,
            byte @NotNull [] data
    ) {
        ensureConnected();
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] packet = buildForwardToPlayerPacket(playerId, channelName, data);
                sender.send(BUNGEE_CHANNEL, packet);
            } catch (IOException e) {
                throw new RuntimeException("Failed to send message to player", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<String>> serverIds() {
        ensureConnected();
        CompletableFuture<Collection<String>> future = new CompletableFuture<>();
        String requestId = "servers:" + UUID.randomUUID();
        pendingRequests.put(requestId, future);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("GetServers");
            sender.send(BUNGEE_CHANNEL, baos.toByteArray());
        } catch (IOException e) {
            future.completeExceptionally(e);
            pendingRequests.remove(requestId);
        }

        return future.orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    @NotNull
    public CompletableFuture<String> findPlayerServer(@NotNull UUID playerId) {
        ensureConnected();
        CompletableFuture<String> future = new CompletableFuture<>();
        String requestId = "playerserver:" + playerId;
        pendingRequests.put(requestId, future);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("ServerIP");
            out.writeUTF(playerId.toString()); // Note: BungeeCord uses player names typically
            sender.send(BUNGEE_CHANNEL, baos.toByteArray());
        } catch (IOException e) {
            future.completeExceptionally(e);
            pendingRequests.remove(requestId);
        }

        return future.orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> transferPlayer(@NotNull UUID playerId, @NotNull String targetServer) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                out.writeUTF("ConnectOther");
                out.writeUTF(playerId.toString());
                out.writeUTF(targetServer);
                sender.send(BUNGEE_CHANNEL, baos.toByteArray());
                return true;
            } catch (IOException e) {
                logger.error("Failed to transfer player {} to {}", playerId, targetServer, e);
                return false;
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> kickPlayer(@NotNull UUID playerId, String reason) {
        ensureConnected();
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                out.writeUTF("KickPlayer");
                out.writeUTF(playerId.toString());
                out.writeUTF(reason != null ? reason : "Kicked from network");
                sender.send(BUNGEE_CHANNEL, baos.toByteArray());
            } catch (IOException e) {
                logger.error("Failed to kick player {}", playerId, e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> playerCount(@NotNull String serverId) {
        ensureConnected();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String requestId = "playercount:" + serverId;
        pendingRequests.put(requestId, future);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("PlayerCount");
            out.writeUTF(serverId);
            sender.send(BUNGEE_CHANNEL, baos.toByteArray());
        } catch (IOException e) {
            future.completeExceptionally(e);
            pendingRequests.remove(requestId);
        }

        return future.orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> networkPlayerCount() {
        return playerCount("ALL");
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<UUID>> playersOn(@NotNull String serverId) {
        ensureConnected();
        CompletableFuture<Collection<UUID>> future = new CompletableFuture<>();
        String requestId = "playerlist:" + serverId;
        pendingRequests.put(requestId, future);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("PlayerList");
            out.writeUTF(serverId);
            sender.send(BUNGEE_CHANNEL, baos.toByteArray());
        } catch (IOException e) {
            future.completeExceptionally(e);
            pendingRequests.remove(requestId);
        }

        return future.orTimeout(5, TimeUnit.SECONDS);
    }

    /**
     * Handles incoming BungeeCord messages.
     *
     * @param data the message data
     */
    private void handleIncoming(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            String subChannel = in.readUTF();

            switch (subChannel) {
                case "GetServers" -> handleGetServersResponse(in);
                case "PlayerCount" -> handlePlayerCountResponse(in);
                case "PlayerList" -> handlePlayerListResponse(in);
                case "Forward" -> handleForwardMessage(in);
                default -> {
                    // Check for custom channel
                    if (subChannel.startsWith(CHANNEL_PREFIX)) {
                        handleCustomMessage(subChannel, in);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error handling incoming BungeeCord message", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleGetServersResponse(DataInputStream in) throws IOException {
        String servers = in.readUTF();
        List<String> serverList = List.of(servers.split(", "));

        pendingRequests.entrySet().stream()
                .filter(e -> e.getKey().startsWith("servers:"))
                .findFirst()
                .ifPresent(e -> {
                    ((CompletableFuture<Collection<String>>) e.getValue()).complete(serverList);
                    pendingRequests.remove(e.getKey());
                });
    }

    @SuppressWarnings("unchecked")
    private void handlePlayerCountResponse(DataInputStream in) throws IOException {
        String server = in.readUTF();
        int count = in.readInt();

        String requestId = "playercount:" + server;
        CompletableFuture<Integer> future = (CompletableFuture<Integer>) pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(count);
        }
    }

    @SuppressWarnings("unchecked")
    private void handlePlayerListResponse(DataInputStream in) throws IOException {
        String server = in.readUTF();
        String playerNames = in.readUTF();

        List<UUID> players = new ArrayList<>();
        if (!playerNames.isEmpty()) {
            // Note: BungeeCord returns names, we'd need to resolve to UUIDs
            // For now, this is a placeholder
            for (String name : playerNames.split(", ")) {
                // In a real implementation, resolve name to UUID
                players.add(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
            }
        }

        String requestId = "playerlist:" + server;
        CompletableFuture<Collection<UUID>> future =
                (CompletableFuture<Collection<UUID>>) pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(players);
        }
    }

    private void handleForwardMessage(DataInputStream in) throws IOException {
        String channel = in.readUTF();
        short length = in.readShort();
        byte[] msgData = new byte[length];
        in.readFully(msgData);

        // Extract source server from message data or use default
        String sourceServer = extractSourceServer(msgData);

        ReceivedMessage message = new SimpleReceivedMessage(channel, sourceServer, msgData);
        dispatchMessage(message);
    }

    private void handleCustomMessage(String subChannel, DataInputStream in) throws IOException {
        short length = in.readShort();
        byte[] msgData = new byte[length];
        in.readFully(msgData);

        String sourceServer = extractSourceServer(msgData);
        ReceivedMessage message = new SimpleReceivedMessage(subChannel, sourceServer, msgData);
        dispatchMessage(message);
    }

    private String extractSourceServer(byte[] data) {
        // Messages should have source server encoded in first bytes
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            return in.readUTF();
        } catch (IOException e) {
            return "unknown";
        }
    }

    private byte[] buildForwardPacket(String target, String channel, byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF("Forward");
        out.writeUTF(target);
        out.writeUTF(channel);
        out.writeShort(data.length);
        out.write(data);

        return baos.toByteArray();
    }

    private byte[] buildForwardToPlayerPacket(UUID playerId, String channel, byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF("ForwardToPlayer");
        out.writeUTF(playerId.toString());
        out.writeUTF(channel);
        out.writeShort(data.length);
        out.write(data);

        return baos.toByteArray();
    }

    /**
     * Interface for sending plugin messages.
     *
     * <p>This abstracts the platform-specific message sending.
     *
     * @since 1.0.0
     */
    public interface PluginMessageSender {

        /**
         * Registers an outgoing channel.
         *
         * @param channel the channel name
         */
        void registerOutgoing(@NotNull String channel);

        /**
         * Registers an incoming channel with a handler.
         *
         * @param channel the channel name
         * @param handler the message handler
         */
        void registerIncoming(@NotNull String channel, @NotNull Consumer<byte[]> handler);

        /**
         * Sends a message on a channel.
         *
         * @param channel the channel name
         * @param data    the message data
         */
        void send(@NotNull String channel, byte @NotNull [] data);
    }
}
