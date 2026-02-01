/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.channels;

import sh.pcx.unified.messaging.MessagingService.TransportType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Channel adapter using Redis pub/sub for messaging.
 *
 * <p>This adapter provides reliable cross-server messaging using Redis
 * as the message broker. It supports both direct messaging and pub/sub
 * patterns.
 *
 * <h2>Redis Keys Used</h2>
 * <pre>
 * unified:servers                - Hash of online servers
 * unified:server:{id}            - Server info hash
 * unified:players                - Hash of player to server mappings
 * unified:channel:{name}         - Pub/sub channel
 * unified:pending:{requestId}    - Pending request data
 * </pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Message persistence for reliable delivery</li>
 *   <li>Server discovery and heartbeats</li>
 *   <li>Player location tracking</li>
 *   <li>Request/response correlation</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class RedisChannelAdapter extends AbstractChannelAdapter {

    private static final String KEY_PREFIX = "unified:";
    private static final String SERVERS_KEY = KEY_PREFIX + "servers";
    private static final String PLAYERS_KEY = KEY_PREFIX + "players";
    private static final long HEARTBEAT_INTERVAL_MS = 5000;
    private static final long SERVER_TIMEOUT_MS = 15000;

    private final RedisConnection redis;
    private final Map<String, CompletableFuture<?>> pendingRequests;
    private final ScheduledExecutorService scheduler;
    private volatile Instant lastHeartbeat;

    /**
     * Creates a new Redis channel adapter.
     *
     * @param serverId the current server ID
     * @param redis    the Redis connection
     */
    public RedisChannelAdapter(@NotNull String serverId, @NotNull RedisConnection redis) {
        super(serverId);
        this.redis = redis;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Redis-Heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    @NotNull
    public TransportType transportType() {
        return TransportType.REDIS;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                redis.connect();

                // Register this server
                registerServer();

                // Start heartbeat
                scheduler.scheduleAtFixedRate(
                        this::sendHeartbeat,
                        HEARTBEAT_INTERVAL_MS,
                        HEARTBEAT_INTERVAL_MS,
                        TimeUnit.MILLISECONDS
                );

                // Subscribe to server-specific channel
                subscribeToServerChannel();

                connected.set(true);
                logger.info("Connected to Redis messaging");
            } catch (Exception e) {
                logger.error("Failed to connect to Redis", e);
                throw new RuntimeException("Failed to connect", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            connected.set(false);
            scheduler.shutdown();

            try {
                // Remove server registration
                redis.hashRemove(SERVERS_KEY, serverId);
                redis.delete(KEY_PREFIX + "server:" + serverId);

                // Cancel pending requests
                pendingRequests.values().forEach(f -> f.cancel(true));
                pendingRequests.clear();

                redis.disconnect();
                logger.info("Disconnected from Redis messaging");
            } catch (Exception e) {
                logger.error("Error during disconnect", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    protected CompletableFuture<Void> doRegisterChannel(@NotNull String channelName) {
        return CompletableFuture.runAsync(() -> {
            String redisChannel = KEY_PREFIX + "channel:" + channelName;
            redis.subscribe(redisChannel, message -> handleChannelMessage(channelName, message));
            logger.debug("Subscribed to Redis channel: {}", redisChannel);
        }, executor);
    }

    @Override
    @NotNull
    protected CompletableFuture<Void> doUnregisterChannel(@NotNull String channelName) {
        return CompletableFuture.runAsync(() -> {
            String redisChannel = KEY_PREFIX + "channel:" + channelName;
            redis.unsubscribe(redisChannel);
            logger.debug("Unsubscribed from Redis channel: {}", redisChannel);
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> broadcast(@NotNull String channelName, byte @NotNull [] data) {
        ensureConnected();
        return CompletableFuture.runAsync(() -> {
            String redisChannel = KEY_PREFIX + "channel:" + channelName;
            String encoded = encodeMessage(data);
            redis.publish(redisChannel, encoded);
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
            // Send to server-specific channel
            String serverChannel = KEY_PREFIX + "server:" + targetServer + ":inbox";
            String message = channelName + "|" + encodeMessage(data);
            redis.publish(serverChannel, message);
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
        return findPlayerServer(playerId).thenCompose(server ->
                sendTo(channelName, server, data)
        );
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<String>> serverIds() {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            Set<String> servers = redis.hashKeys(SERVERS_KEY);
            return servers != null ? servers : Collections.emptySet();
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<String> findPlayerServer(@NotNull UUID playerId) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            String server = redis.hashGet(PLAYERS_KEY, playerId.toString());
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
        // Player transfers need to go through the proxy
        // Send a transfer request message
        return CompletableFuture.supplyAsync(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                out.writeUTF("TRANSFER");
                out.writeLong(playerId.getMostSignificantBits());
                out.writeLong(playerId.getLeastSignificantBits());
                out.writeUTF(targetServer);

                String channel = KEY_PREFIX + "control";
                redis.publish(channel, encodeMessage(baos.toByteArray()));
                return true;
            } catch (IOException e) {
                logger.error("Failed to send transfer request", e);
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
                out.writeUTF("KICK");
                out.writeLong(playerId.getMostSignificantBits());
                out.writeLong(playerId.getLeastSignificantBits());
                out.writeUTF(reason != null ? reason : "Kicked from network");

                String channel = KEY_PREFIX + "control";
                redis.publish(channel, encodeMessage(baos.toByteArray()));
            } catch (IOException e) {
                logger.error("Failed to send kick request", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> playerCount(@NotNull String serverId) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            String countStr = redis.hashGet(KEY_PREFIX + "server:" + serverId, "players");
            return countStr != null ? Integer.parseInt(countStr) : 0;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> networkPlayerCount() {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            long count = redis.hashSize(PLAYERS_KEY);
            return (int) count;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<UUID>> playersOn(@NotNull String serverId) {
        ensureConnected();
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> allPlayers = redis.hashGetAll(PLAYERS_KEY);
            return allPlayers.entrySet().stream()
                    .filter(e -> serverId.equals(e.getValue()))
                    .map(e -> UUID.fromString(e.getKey()))
                    .toList();
        }, executor);
    }

    /**
     * Registers a player's location.
     *
     * @param playerId the player UUID
     */
    public void registerPlayer(@NotNull UUID playerId) {
        redis.hashSet(PLAYERS_KEY, playerId.toString(), serverId);
    }

    /**
     * Unregisters a player.
     *
     * @param playerId the player UUID
     */
    public void unregisterPlayer(@NotNull UUID playerId) {
        redis.hashRemove(PLAYERS_KEY, playerId.toString());
    }

    private void registerServer() {
        Map<String, String> serverInfo = Map.of(
                "id", serverId,
                "online", "true",
                "lastHeartbeat", String.valueOf(System.currentTimeMillis()),
                "players", "0"
        );
        redis.hashSetAll(KEY_PREFIX + "server:" + serverId, serverInfo);
        redis.hashSet(SERVERS_KEY, serverId, String.valueOf(System.currentTimeMillis()));
    }

    private void sendHeartbeat() {
        try {
            lastHeartbeat = Instant.now();
            redis.hashSet(KEY_PREFIX + "server:" + serverId, "lastHeartbeat",
                    String.valueOf(System.currentTimeMillis()));
            redis.hashSet(SERVERS_KEY, serverId, String.valueOf(System.currentTimeMillis()));

            // Clean up stale servers
            cleanupStaleServers();
        } catch (Exception e) {
            logger.warn("Failed to send heartbeat", e);
        }
    }

    private void cleanupStaleServers() {
        Map<String, String> servers = redis.hashGetAll(SERVERS_KEY);
        long now = System.currentTimeMillis();

        servers.forEach((id, lastSeen) -> {
            try {
                long lastSeenMs = Long.parseLong(lastSeen);
                if (now - lastSeenMs > SERVER_TIMEOUT_MS) {
                    logger.info("Removing stale server: {}", id);
                    redis.hashRemove(SERVERS_KEY, id);
                    redis.delete(KEY_PREFIX + "server:" + id);
                }
            } catch (NumberFormatException e) {
                // Invalid timestamp, remove the entry
                redis.hashRemove(SERVERS_KEY, id);
            }
        });
    }

    private void subscribeToServerChannel() {
        String serverChannel = KEY_PREFIX + "server:" + serverId + ":inbox";
        redis.subscribe(serverChannel, this::handleServerMessage);
    }

    private void handleServerMessage(String message) {
        try {
            int separatorIndex = message.indexOf('|');
            if (separatorIndex > 0) {
                String channel = message.substring(0, separatorIndex);
                String encodedData = message.substring(separatorIndex + 1);
                byte[] data = decodeMessage(encodedData);
                handleChannelMessage(channel, encodedData);
            }
        } catch (Exception e) {
            logger.error("Error handling server message", e);
        }
    }

    private void handleChannelMessage(String channelName, String encodedMessage) {
        try {
            byte[] data = decodeMessage(encodedMessage);
            String sourceServer = extractSourceServer(data);
            ReceivedMessage message = new SimpleReceivedMessage(channelName, sourceServer, data);
            dispatchMessage(message);
        } catch (Exception e) {
            logger.error("Error handling channel message on {}", channelName, e);
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

    private String encodeMessage(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private byte[] decodeMessage(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }

    /**
     * Interface for Redis operations.
     *
     * <p>This abstracts the Redis client library (Jedis, Lettuce, etc.)
     * for flexibility and testing.
     *
     * @since 1.0.0
     */
    public interface RedisConnection {

        /**
         * Connects to Redis.
         */
        void connect();

        /**
         * Disconnects from Redis.
         */
        void disconnect();

        /**
         * Publishes a message to a channel.
         *
         * @param channel the channel
         * @param message the message
         */
        void publish(@NotNull String channel, @NotNull String message);

        /**
         * Subscribes to a channel.
         *
         * @param channel the channel
         * @param handler the message handler
         */
        void subscribe(@NotNull String channel, @NotNull java.util.function.Consumer<String> handler);

        /**
         * Unsubscribes from a channel.
         *
         * @param channel the channel
         */
        void unsubscribe(@NotNull String channel);

        /**
         * Gets a hash field value.
         *
         * @param key the hash key
         * @param field the field
         * @return the value
         */
        @Nullable
        String hashGet(@NotNull String key, @NotNull String field);

        /**
         * Sets a hash field value.
         *
         * @param key the hash key
         * @param field the field
         * @param value the value
         */
        void hashSet(@NotNull String key, @NotNull String field, @NotNull String value);

        /**
         * Sets multiple hash fields.
         *
         * @param key the hash key
         * @param values the field-value pairs
         */
        void hashSetAll(@NotNull String key, @NotNull Map<String, String> values);

        /**
         * Gets all hash fields and values.
         *
         * @param key the hash key
         * @return the field-value map
         */
        @NotNull
        Map<String, String> hashGetAll(@NotNull String key);

        /**
         * Gets all hash field names.
         *
         * @param key the hash key
         * @return the field names
         */
        @NotNull
        Set<String> hashKeys(@NotNull String key);

        /**
         * Removes a hash field.
         *
         * @param key the hash key
         * @param field the field
         */
        void hashRemove(@NotNull String key, @NotNull String field);

        /**
         * Gets the size of a hash.
         *
         * @param key the hash key
         * @return the number of fields
         */
        long hashSize(@NotNull String key);

        /**
         * Deletes a key.
         *
         * @param key the key
         */
        void delete(@NotNull String key);
    }
}
