/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging;

import sh.pcx.unified.messaging.Message;
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
 * Wraps a user-defined message with network metadata.
 *
 * <p>This class takes a message annotated with {@link Message} and adds
 * the necessary network metadata for transmission. The original payload
 * is preserved and can be retrieved.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Message(channel = "myplugin:events")
 * public record PlayerJoinedMessage(UUID playerId, String server) {}
 *
 * // Wrap for sending
 * var wrapped = MessageWrapper.wrap(
 *     new PlayerJoinedMessage(playerId, "lobby-1"),
 *     "server-1"
 * );
 *
 * // Send over the wire...
 *
 * // On receiving end
 * PlayerJoinedMessage original = wrapped.payload();
 * }</pre>
 *
 * @param <T> the payload type
 * @since 1.0.0
 * @author Supatuck
 */
public final class MessageWrapper<T> implements NetworkMessage {

    private final UUID messageId;
    private final String channel;
    private final String type;
    private final Instant timestamp;
    private final String sourceServer;
    private final String targetServer;
    private final UUID correlationId;
    private final long ttl;
    private final T payload;
    private final Class<T> payloadClass;
    private final Map<String, Object> metadata;

    private MessageWrapper(
            @NotNull UUID messageId,
            @NotNull String channel,
            @NotNull String type,
            @NotNull Instant timestamp,
            @NotNull String sourceServer,
            @Nullable String targetServer,
            @Nullable UUID correlationId,
            long ttl,
            @NotNull T payload,
            @NotNull Class<T> payloadClass,
            @NotNull Map<String, Object> metadata
    ) {
        this.messageId = Objects.requireNonNull(messageId);
        this.channel = Objects.requireNonNull(channel);
        this.type = Objects.requireNonNull(type);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.sourceServer = Objects.requireNonNull(sourceServer);
        this.targetServer = targetServer;
        this.correlationId = correlationId;
        this.ttl = ttl;
        this.payload = Objects.requireNonNull(payload);
        this.payloadClass = Objects.requireNonNull(payloadClass);
        this.metadata = new ConcurrentHashMap<>(metadata);
    }

    /**
     * Wraps a message with network metadata.
     *
     * @param <T>          the payload type
     * @param payload      the message payload
     * @param sourceServer the source server ID
     * @return the wrapped message
     * @throws IllegalArgumentException if payload lacks @Message annotation
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> MessageWrapper<T> wrap(@NotNull T payload, @NotNull String sourceServer) {
        Objects.requireNonNull(payload);
        Objects.requireNonNull(sourceServer);

        Class<T> payloadClass = (Class<T>) payload.getClass();
        Message annotation = payloadClass.getAnnotation(Message.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Message class must have @Message annotation: " + payloadClass.getName()
            );
        }

        String channel = annotation.channel();
        String type = annotation.value().isEmpty() ? payloadClass.getSimpleName() : annotation.value();
        long ttl = annotation.ttl();

        return new MessageWrapper<>(
                UUID.randomUUID(),
                channel,
                type,
                Instant.now(),
                sourceServer,
                null,
                null,
                ttl,
                payload,
                payloadClass,
                Map.of()
        );
    }

    /**
     * Wraps a message targeted to a specific server.
     *
     * @param <T>          the payload type
     * @param payload      the message payload
     * @param sourceServer the source server ID
     * @param targetServer the target server ID
     * @return the wrapped message
     * @since 1.0.0
     */
    @NotNull
    public static <T> MessageWrapper<T> wrap(
            @NotNull T payload,
            @NotNull String sourceServer,
            @NotNull String targetServer
    ) {
        MessageWrapper<T> wrapper = wrap(payload, sourceServer);
        return wrapper.withTarget(targetServer);
    }

    /**
     * Creates a copy with a target server set.
     *
     * @param targetServer the target server
     * @return new wrapper with target
     * @since 1.0.0
     */
    @NotNull
    public MessageWrapper<T> withTarget(@NotNull String targetServer) {
        return new MessageWrapper<>(
                messageId, channel, type, timestamp, sourceServer,
                targetServer, correlationId, ttl, payload, payloadClass, metadata
        );
    }

    /**
     * Creates a copy with a correlation ID for responses.
     *
     * @param correlationId the correlation ID
     * @return new wrapper with correlation ID
     * @since 1.0.0
     */
    @NotNull
    public MessageWrapper<T> withCorrelation(@NotNull UUID correlationId) {
        return new MessageWrapper<>(
                messageId, channel, type, timestamp, sourceServer,
                targetServer, correlationId, ttl, payload, payloadClass, metadata
        );
    }

    /**
     * Creates a copy with custom TTL.
     *
     * @param ttl the TTL in milliseconds
     * @return new wrapper with TTL
     * @since 1.0.0
     */
    @NotNull
    public MessageWrapper<T> withTtl(long ttl) {
        return new MessageWrapper<>(
                messageId, channel, type, timestamp, sourceServer,
                targetServer, correlationId, ttl, payload, payloadClass, metadata
        );
    }

    /**
     * Creates a copy with additional metadata.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return new wrapper with metadata
     * @since 1.0.0
     */
    @NotNull
    public MessageWrapper<T> withMetadata(@NotNull String key, @NotNull Object value) {
        Map<String, Object> newMetadata = new ConcurrentHashMap<>(metadata);
        newMetadata.put(key, value);
        return new MessageWrapper<>(
                messageId, channel, type, timestamp, sourceServer,
                targetServer, correlationId, ttl, payload, payloadClass, newMetadata
        );
    }

    /**
     * Returns the original payload message.
     *
     * @return the payload
     * @since 1.0.0
     */
    @NotNull
    public T payload() {
        return payload;
    }

    /**
     * Returns the payload class.
     *
     * @return the payload class
     * @since 1.0.0
     */
    @NotNull
    public Class<T> payloadClass() {
        return payloadClass;
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
    public <M> M metadata(@NotNull String key) {
        return (M) metadata.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageWrapper<?> that)) return false;
        return messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return messageId.hashCode();
    }

    @Override
    public String toString() {
        return "MessageWrapper{" +
                "messageId=" + messageId +
                ", channel='" + channel + '\'' +
                ", type='" + type + '\'' +
                ", source='" + sourceServer + '\'' +
                ", target=" + (targetServer != null ? "'" + targetServer + "'" : "broadcast") +
                ", payload=" + payload +
                '}';
    }
}
