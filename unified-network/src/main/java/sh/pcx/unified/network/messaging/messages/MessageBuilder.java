/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Fluent builder for creating {@link Message} instances.
 *
 * <p>This builder provides a convenient way to construct messages with
 * all necessary metadata and payload data.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple broadcast message
 * Message broadcast = MessageBuilder.create("server_announcement")
 *     .data("message", "Server restarting in 5 minutes!")
 *     .data("priority", "high")
 *     .build();
 *
 * // Targeted message to specific server
 * Message targeted = MessageBuilder.create("player_transfer")
 *     .target("lobby-1")
 *     .data("playerId", player.getUniqueId())
 *     .data("reason", "hub command")
 *     .ttl(Duration.ofSeconds(30))
 *     .build();
 *
 * // Request expecting a response
 * Message request = MessageBuilder.create("player_count_request")
 *     .target("hub")
 *     .expectResponse()
 *     .ttl(Duration.ofSeconds(10))
 *     .build();
 *
 * // Response to a request
 * Message response = originalMessage.reply("player_count_response")
 *     .data("count", 42)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Builders are not thread-safe. Each thread should use its own builder instance.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Message
 */
public final class MessageBuilder {

    private final String type;
    private UUID messageId;
    private String sourceServer;
    private String targetServer;
    private UUID correlationId;
    private long ttl;
    private boolean expectsResponse;
    private final Map<String, Object> data;
    private final Map<String, Object> metadata;

    /**
     * Creates a new message builder for the specified type.
     *
     * @param type the message type identifier
     */
    private MessageBuilder(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.messageId = UUID.randomUUID();
        this.data = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Creates a new message builder for the specified type.
     *
     * @param type the message type identifier
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static MessageBuilder create(@NotNull String type) {
        return new MessageBuilder(type);
    }

    /**
     * Creates a new message builder for a message class.
     *
     * <p>The type is extracted from the {@link MessageType} annotation on the class.
     *
     * @param messageClass the message class
     * @return a new builder instance
     * @throws IllegalArgumentException if the class has no MessageType annotation
     * @since 1.0.0
     */
    @NotNull
    public static MessageBuilder create(@NotNull Class<? extends Message> messageClass) {
        MessageType annotation = messageClass.getAnnotation(MessageType.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Message class must have @MessageType annotation: " + messageClass.getName());
        }
        return new MessageBuilder(annotation.value());
    }

    /**
     * Sets a custom message ID.
     *
     * <p>By default, a random UUID is generated. Only use this if you need
     * to specify a particular ID.
     *
     * @param id the message ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder messageId(@NotNull UUID id) {
        this.messageId = Objects.requireNonNull(id, "id cannot be null");
        return this;
    }

    /**
     * Sets the source server ID.
     *
     * <p>This is typically set automatically by the messaging service based
     * on the current server. Only use this for testing or special cases.
     *
     * @param server the source server ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder source(@NotNull String server) {
        this.sourceServer = Objects.requireNonNull(server, "server cannot be null");
        return this;
    }

    /**
     * Sets the target server ID for a targeted message.
     *
     * <p>If not set, the message will be a broadcast to all servers.
     *
     * @param server the target server ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder target(@NotNull String server) {
        this.targetServer = Objects.requireNonNull(server, "server cannot be null");
        return this;
    }

    /**
     * Sets this as a broadcast message (clears target server).
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder broadcast() {
        this.targetServer = null;
        return this;
    }

    /**
     * Sets the correlation ID for request/response matching.
     *
     * <p>This is typically set automatically when replying to a message.
     *
     * @param id the correlation ID (usually the original message's ID)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder correlationId(@NotNull UUID id) {
        this.correlationId = Objects.requireNonNull(id, "id cannot be null");
        return this;
    }

    /**
     * Sets the time-to-live for the message.
     *
     * <p>Messages older than the TTL will be discarded by receivers.
     *
     * @param ttl the time-to-live in milliseconds
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder ttl(long ttl) {
        if (ttl < 0) {
            throw new IllegalArgumentException("TTL cannot be negative");
        }
        this.ttl = ttl;
        return this;
    }

    /**
     * Sets the time-to-live for the message.
     *
     * @param duration the time-to-live duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder ttl(@NotNull Duration duration) {
        return ttl(duration.toMillis());
    }

    /**
     * Marks this message as expecting a response.
     *
     * <p>This is informational and helps with debugging and monitoring.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder expectResponse() {
        this.expectsResponse = true;
        return this;
    }

    /**
     * Adds data to the message payload.
     *
     * @param key   the data key
     * @param value the data value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder data(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value != null) {
            this.data.put(key, value);
        } else {
            this.data.remove(key);
        }
        return this;
    }

    /**
     * Adds multiple data entries to the message payload.
     *
     * @param data the data map
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder data(@NotNull Map<String, Object> data) {
        this.data.putAll(data);
        return this;
    }

    /**
     * Adds metadata to the message.
     *
     * <p>Metadata is separate from the payload and used for routing,
     * tracing, and debugging purposes.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MessageBuilder metadata(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value != null) {
            this.metadata.put(key, value);
        } else {
            this.metadata.remove(key);
        }
        return this;
    }

    /**
     * Builds the message.
     *
     * @return the constructed message
     * @since 1.0.0
     */
    @NotNull
    public Message build() {
        String source = sourceServer != null ? sourceServer : "unknown";
        return new BuiltMessage(
                messageId, type, Instant.now(), source, targetServer,
                correlationId, ttl, expectsResponse, data, metadata
        );
    }

    /**
     * Implementation of Message created by the builder.
     */
    private static final class BuiltMessage implements Message {
        private final UUID messageId;
        private final String type;
        private final Instant timestamp;
        private final String sourceServer;
        private final String targetServer;
        private final UUID correlationId;
        private final long ttl;
        private final boolean expectsResponse;
        private final Map<String, Object> data;
        private final Map<String, Object> metadata;

        BuiltMessage(UUID messageId, String type, Instant timestamp, String sourceServer,
                     @Nullable String targetServer, @Nullable UUID correlationId, long ttl,
                     boolean expectsResponse, Map<String, Object> data, Map<String, Object> metadata) {
            this.messageId = messageId;
            this.type = type;
            this.timestamp = timestamp;
            this.sourceServer = sourceServer;
            this.targetServer = targetServer;
            this.correlationId = correlationId;
            this.ttl = ttl;
            this.expectsResponse = expectsResponse;
            this.data = Map.copyOf(data);
            this.metadata = Map.copyOf(metadata);
        }

        @Override
        @NotNull
        public UUID getMessageId() {
            return messageId;
        }

        @Override
        @NotNull
        public String getType() {
            return type;
        }

        @Override
        @NotNull
        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        @NotNull
        public String getSourceServer() {
            return sourceServer;
        }

        @Override
        @NotNull
        public Optional<String> getTargetServer() {
            return Optional.ofNullable(targetServer);
        }

        @Override
        @NotNull
        public Optional<UUID> getCorrelationId() {
            return Optional.ofNullable(correlationId);
        }

        @Override
        public long getTimeToLive() {
            return ttl;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMetadata(@NotNull String key) {
            return (T) metadata.get(key);
        }

        /**
         * Returns a data value from the payload.
         *
         * @param key the data key
         * @param <T> the expected type
         * @return the value, or null if not present
         */
        @SuppressWarnings("unchecked")
        public <T> T getData(@NotNull String key) {
            return (T) data.get(key);
        }

        /**
         * Returns the entire data map.
         *
         * @return the data map
         */
        @NotNull
        public Map<String, Object> getData() {
            return data;
        }

        /**
         * Returns whether this message expects a response.
         *
         * @return true if a response is expected
         */
        public boolean expectsResponse() {
            return expectsResponse;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "id=" + messageId +
                    ", type='" + type + '\'' +
                    ", source='" + sourceServer + '\'' +
                    ", target=" + (targetServer != null ? "'" + targetServer + "'" : "broadcast") +
                    ", data=" + data +
                    '}';
        }
    }
}
