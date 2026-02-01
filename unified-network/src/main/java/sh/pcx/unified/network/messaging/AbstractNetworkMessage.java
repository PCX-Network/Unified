/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging;

import sh.pcx.unified.messaging.NetworkMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of {@link NetworkMessage}.
 *
 * <p>This class provides the common metadata handling for all network messages.
 * Subclasses only need to implement their specific payload data.
 *
 * <h2>Usage</h2>
 * <p>Most users should use the {@link sh.pcx.unified.messaging.Message}
 * annotation on records or classes instead of extending this class directly.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractNetworkMessage implements NetworkMessage {

    private final UUID messageId;
    private final String channel;
    private final String type;
    private final Instant timestamp;
    private final String sourceServer;
    private final String targetServer;
    private final UUID correlationId;
    private final long ttl;
    private final Map<String, Object> metadata;

    /**
     * Creates a new network message with all metadata.
     *
     * @param messageId     unique message ID
     * @param channel       the channel name
     * @param type          the message type
     * @param timestamp     creation timestamp
     * @param sourceServer  source server ID
     * @param targetServer  target server ID (null for broadcast)
     * @param correlationId correlation ID for responses (null if not a response)
     * @param ttl           time-to-live in milliseconds
     */
    protected AbstractNetworkMessage(
            @NotNull UUID messageId,
            @NotNull String channel,
            @NotNull String type,
            @NotNull Instant timestamp,
            @NotNull String sourceServer,
            @Nullable String targetServer,
            @Nullable UUID correlationId,
            long ttl
    ) {
        this.messageId = Objects.requireNonNull(messageId, "messageId");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.type = Objects.requireNonNull(type, "type");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.sourceServer = Objects.requireNonNull(sourceServer, "sourceServer");
        this.targetServer = targetServer;
        this.correlationId = correlationId;
        this.ttl = ttl;
        this.metadata = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new message with minimal metadata.
     *
     * <p>Other metadata is set to defaults (new UUID, current time, etc.).
     *
     * @param channel      the channel name
     * @param type         the message type
     * @param sourceServer source server ID
     */
    protected AbstractNetworkMessage(
            @NotNull String channel,
            @NotNull String type,
            @NotNull String sourceServer
    ) {
        this(
                UUID.randomUUID(),
                channel,
                type,
                Instant.now(),
                sourceServer,
                null,
                null,
                0
        );
    }

    @Override
    @NotNull
    public UUID messageId() {
        return messageId;
    }

    @Override
    @NotNull
    public String channel() {
        return channel;
    }

    @Override
    @NotNull
    public String type() {
        return type;
    }

    @Override
    @NotNull
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    @NotNull
    public String sourceServer() {
        return sourceServer;
    }

    @Override
    @NotNull
    public Optional<String> targetServer() {
        return Optional.ofNullable(targetServer);
    }

    @Override
    @NotNull
    public Optional<UUID> correlationId() {
        return Optional.ofNullable(correlationId);
    }

    @Override
    public long ttl() {
        return ttl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T metadata(@NotNull String key) {
        return (T) metadata.get(key);
    }

    /**
     * Sets a metadata value.
     *
     * @param key   the key
     * @param value the value
     * @since 1.0.0
     */
    public void setMetadata(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
    }

    /**
     * Returns the full metadata map.
     *
     * @return the metadata map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> allMetadata() {
        return Map.copyOf(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractNetworkMessage that)) return false;
        return messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return messageId.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "messageId=" + messageId +
                ", channel='" + channel + '\'' +
                ", type='" + type + '\'' +
                ", source='" + sourceServer + '\'' +
                ", target=" + (targetServer != null ? "'" + targetServer + "'" : "broadcast") +
                '}';
    }
}
