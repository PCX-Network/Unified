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
 * Channel adapter for Velocity proxy messaging.
 *
 * <p>This adapter uses Velocity's plugin messaging API for cross-server
 * communication. It supports both the BungeeCord-compatible channel and
 * Velocity's native modern forwarding.
 *
 * <h2>Velocity Channel Protocol</h2>
 * <p>Velocity supports both legacy BungeeCord channels and modern channels.
 * This adapter uses modern Velocity channels when available:
 * <pre>
 * Channel: velocity:main (or bungeecord:main for compatibility)
 *
 * Message Format:
 *   [Message ID: VarInt]
 *   [Data: varies by message type]
 * </pre>
 *
 * <h2>Key Differences from BungeeCord</h2>
 * <ul>
 *   <li>Uses modern forwarding for better performance</li>
 *   <li>Supports larger message payloads</li>
 *   <li>Native UUID support (no name resolution needed)</li>
 *   <li>Better connection state handling</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class VelocityChannelAdapter extends AbstractChannelAdapter {

    /**
     * Velocity's modern plugin messaging channel.
     */
    public static final String VELOCITY_CHANNEL = "velocity:main";

    /**
     * BungeeCord compatibility channel on Velocity.
     */
    public static final String BUNGEECORD_CHANNEL = "bungeecord:main";

    private final Object proxyServer;
    private final PluginMessageBroker broker;
    private final Map<String, CompletableFuture<?>> pendingRequests;
    private final boolean useModernChannel;

    /**
     * Creates a new Velocity channel adapter.
     *
     * @param serverId the current server ID
     * @param proxyServer the Velocity proxy server instance
     * @param broker the plugin message broker
     * @param useModernChannel whether to use Velocity's modern channel
     */
    public VelocityChannelAdapter(
            @NotNull String serverId,
            @NotNull Object proxyServer,
            @NotNull PluginMessageBroker broker,
            boolean useModernChannel
    ) {
        super(serverId);
        this.proxyServer = proxyServer;
        this.broker = broker;
        this.useModernChannel = useModernChannel;
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    /**
     * Creates an adapter using modern Velocity channels.
     *
     * @param serverId the server ID
     * @param proxyServer the proxy server
     * @param broker the message broker
     */
    public VelocityChannelAdapter(
            @NotNull String serverId,
            @NotNull Object proxyServer,
            @NotNull PluginMessageBroker broker
    ) {
        this(serverId, proxyServer, broker, true);
    }

    @Override
    @NotNull
    public TransportType transportType() {
        return TransportType.VELOCITY;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                String channel = useModernChannel ? VELOCITY_CHANNEL : BUNGEECORD_CHANNEL;
                broker.registerChannel(channel, this::handleIncoming);
                connected.set(true);
                logger.info("Connected to Velocity messaging using channel: {}", channel);
            } catch (Exception e) {
                logger.error("Failed to connect to Velocity messaging", e);
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
            logger.info("Disconnected from Velocity messaging");
        }, executor);
    }

    @Override
    @NotNull
    protected CompletableFuture<Void> doRegisterChannel(@NotNull String channelName) {
        return CompletableFuture.runAsync(() -> {
            broker.registerChannel(channelName, data -> handleCustomChannel(channelName, data));
        }, executor);
    }

    @Override
    @NotNull
    protected CompletableFuture<Void> doUnregisterChannel(@NotNull String channelName) {
        return CompletableFuture.runAsync(() -> {
            broker.unregisterChannel(channelName);
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> broadcast(@NotNull String channelName, byte @NotNull [] data) {
        ensureConnected();
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] packet = buildForwardPacket("ALL", channelName, data);
                broker.sendToAll(getMainChannel(), packet);
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
                broker.sendToServer(targetServer, getMainChannel(), packet);
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
                broker.sendToPlayer(playerId, getMainChannel(), packet);
            } catch (IOException e) {
                throw new RuntimeException("Failed to send message to player", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<String>> serverIds() {
        ensureConnected();
        // Velocity provides server list directly through the proxy API
        return CompletableFuture.supplyAsync(() -> broker.getAllServers(), executor);
    }

    @Override
    @NotNull
    public CompletableFuture<String> findPlayerServer(@NotNull UUID playerId) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            String server = broker.getPlayerServer(playerId);
            if (server == null) {
                throw new RuntimeException("Player not found: " + playerId);
            }
            return server;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> transferPlayer(@NotNull UUID playerId, @NotNull String targetServer) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return broker.connectPlayer(playerId, targetServer);
            } catch (Exception e) {
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
            broker.kickPlayer(playerId, reason != null ? reason : "Kicked from network");
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> playerCount(@NotNull String serverId) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> broker.getPlayerCount(serverId), executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> networkPlayerCount() {
        ensureConnected();
        return CompletableFuture.supplyAsync(broker::getTotalPlayerCount, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<UUID>> playersOn(@NotNull String serverId) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> broker.getPlayersOn(serverId), executor);
    }

    private String getMainChannel() {
        return useModernChannel ? VELOCITY_CHANNEL : BUNGEECORD_CHANNEL;
    }

    private void handleIncoming(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            int messageId = readVarInt(in);

            // Handle different Velocity message types
            switch (messageId) {
                case 0 -> handleForwardMessage(in);
                case 1 -> handleServerListResponse(in);
                case 2 -> handlePlayerCountResponse(in);
                default -> logger.debug("Unknown Velocity message ID: {}", messageId);
            }
        } catch (IOException e) {
            logger.error("Error handling incoming Velocity message", e);
        }
    }

    private void handleCustomChannel(String channel, byte[] data) {
        String sourceServer = extractSourceServer(data);
        ReceivedMessage message = new SimpleReceivedMessage(channel, sourceServer, data);
        dispatchMessage(message);
    }

    private void handleForwardMessage(DataInputStream in) throws IOException {
        String channel = in.readUTF();
        String sourceServer = in.readUTF();
        int length = in.readInt();
        byte[] msgData = new byte[length];
        in.readFully(msgData);

        ReceivedMessage message = new SimpleReceivedMessage(channel, sourceServer, msgData);
        dispatchMessage(message);
    }

    @SuppressWarnings("unchecked")
    private void handleServerListResponse(DataInputStream in) throws IOException {
        int count = readVarInt(in);
        List<String> servers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            servers.add(in.readUTF());
        }

        pendingRequests.entrySet().stream()
                .filter(e -> e.getKey().startsWith("servers:"))
                .findFirst()
                .ifPresent(e -> {
                    ((CompletableFuture<Collection<String>>) e.getValue()).complete(servers);
                    pendingRequests.remove(e.getKey());
                });
    }

    @SuppressWarnings("unchecked")
    private void handlePlayerCountResponse(DataInputStream in) throws IOException {
        String server = in.readUTF();
        int count = readVarInt(in);

        String requestId = "playercount:" + server;
        CompletableFuture<Integer> future = (CompletableFuture<Integer>) pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(count);
        }
    }

    private String extractSourceServer(byte[] data) {
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

        writeVarInt(out, 0); // Forward message ID
        out.writeUTF(target);
        out.writeUTF(channel);
        out.writeUTF(serverId); // Source server
        out.writeInt(data.length);
        out.write(data);

        return baos.toByteArray();
    }

    private byte[] buildForwardToPlayerPacket(UUID playerId, String channel, byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        writeVarInt(out, 3); // ForwardToPlayer message ID
        out.writeLong(playerId.getMostSignificantBits());
        out.writeLong(playerId.getLeastSignificantBits());
        out.writeUTF(channel);
        out.writeUTF(serverId);
        out.writeInt(data.length);
        out.write(data);

        return baos.toByteArray();
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;

            if ((currentByte & 0x80) == 0) break;

            position += 7;
            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.writeByte(value);
                return;
            }
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    /**
     * Interface for Velocity plugin message operations.
     *
     * <p>This abstracts Velocity-specific API calls for testability.
     *
     * @since 1.0.0
     */
    public interface PluginMessageBroker {

        /**
         * Registers a channel for receiving messages.
         *
         * @param channel the channel identifier
         * @param handler the message handler
         */
        void registerChannel(@NotNull String channel, @NotNull Consumer<byte[]> handler);

        /**
         * Unregisters a channel.
         *
         * @param channel the channel identifier
         */
        void unregisterChannel(@NotNull String channel);

        /**
         * Sends a message to all servers.
         *
         * @param channel the channel
         * @param data the message data
         */
        void sendToAll(@NotNull String channel, byte @NotNull [] data);

        /**
         * Sends a message to a specific server.
         *
         * @param server the server name
         * @param channel the channel
         * @param data the message data
         */
        void sendToServer(@NotNull String server, @NotNull String channel, byte @NotNull [] data);

        /**
         * Sends a message to a player's current server.
         *
         * @param playerId the player UUID
         * @param channel the channel
         * @param data the message data
         */
        void sendToPlayer(@NotNull UUID playerId, @NotNull String channel, byte @NotNull [] data);

        /**
         * Gets all registered server names.
         *
         * @return server names
         */
        @NotNull
        Collection<String> getAllServers();

        /**
         * Gets the server a player is on.
         *
         * @param playerId the player UUID
         * @return server name, or null if not found
         */
        String getPlayerServer(@NotNull UUID playerId);

        /**
         * Connects a player to a server.
         *
         * @param playerId the player UUID
         * @param server the target server
         * @return true if connection initiated
         */
        boolean connectPlayer(@NotNull UUID playerId, @NotNull String server);

        /**
         * Kicks a player from the network.
         *
         * @param playerId the player UUID
         * @param reason the kick reason
         */
        void kickPlayer(@NotNull UUID playerId, @NotNull String reason);

        /**
         * Gets the player count for a server.
         *
         * @param server the server name
         * @return player count
         */
        int getPlayerCount(@NotNull String server);

        /**
         * Gets the total player count.
         *
         * @return total players
         */
        int getTotalPlayerCount();

        /**
         * Gets players on a server.
         *
         * @param server the server name
         * @return player UUIDs
         */
        @NotNull
        Collection<UUID> getPlayersOn(@NotNull String server);
    }
}
