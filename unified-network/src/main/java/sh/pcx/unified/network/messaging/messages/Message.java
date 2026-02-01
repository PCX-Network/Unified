/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Base interface for all cross-server messages.
 *
 * <p>Messages are the fundamental unit of communication in the messaging system.
 * Each message has metadata including a unique ID, timestamp, source server,
 * and optional target server.
 *
 * <h2>Creating Custom Messages</h2>
 * <pre>{@code
 * // Define a message type using the annotation
 * @MessageType("player_update")
 * public record PlayerUpdateMessage(
 *     UUID playerId,
 *     String newServer
 * ) implements Message {
 *
 *     @Override
 *     public String getType() {
 *         return "player_update";
 *     }
 * }
 *
 * // Using with MessageBuilder
 * Message message = MessageBuilder.create("player_update")
 *     .data("playerId", player.getUniqueId())
 *     .data("newServer", "lobby-1")
 *     .build();
 * }</pre>
 *
 * <h2>Message Lifecycle</h2>
 * <ol>
 *   <li>Message is created with {@link MessageBuilder} or directly</li>
 *   <li>Message is serialized via {@link MessageSerializer}</li>
 *   <li>Message is transmitted over the channel</li>
 *   <li>Message is deserialized on the receiving server</li>
 *   <li>Message handlers are invoked</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessageType
 * @see MessageBuilder
 * @see MessageSerializer
 */
public interface Message {

    /**
     * Returns the unique identifier for this message.
     *
     * <p>The message ID is used for:
     * <ul>
     *   <li>Correlating request/response pairs</li>
     *   <li>Deduplication of messages</li>
     *   <li>Logging and debugging</li>
     * </ul>
     *
     * @return the unique message ID
     * @since 1.0.0
     */
    @NotNull
    UUID getMessageId();

    /**
     * Returns the type identifier for this message.
     *
     * <p>The type is used to determine which handler should process
     * the message on the receiving end. It should be unique within
     * a channel and match the value in {@link MessageType} annotation.
     *
     * @return the message type identifier
     * @since 1.0.0
     */
    @NotNull
    String getType();

    /**
     * Returns the timestamp when this message was created.
     *
     * @return the creation timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getTimestamp();

    /**
     * Returns the ID of the server that originated this message.
     *
     * @return the source server ID
     * @since 1.0.0
     */
    @NotNull
    String getSourceServer();

    /**
     * Returns the ID of the target server, if this is a targeted message.
     *
     * <p>For broadcast messages, this returns an empty Optional.
     *
     * @return the target server ID, or empty for broadcasts
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getTargetServer();

    /**
     * Returns the correlation ID for request/response matching.
     *
     * <p>When a message is a response to a request, this contains
     * the message ID of the original request.
     *
     * @return the correlation ID, or empty if not a response
     * @since 1.0.0
     */
    @NotNull
    Optional<UUID> getCorrelationId();

    /**
     * Checks if this message has expired based on its TTL.
     *
     * <p>Expired messages should be discarded by receivers.
     *
     * @return true if the message has expired
     * @since 1.0.0
     */
    default boolean isExpired() {
        long ttl = getTimeToLive();
        if (ttl <= 0) {
            return false; // No expiration
        }
        return Instant.now().isAfter(getTimestamp().plusMillis(ttl));
    }

    /**
     * Returns the time-to-live for this message in milliseconds.
     *
     * <p>A value of 0 or negative means the message does not expire.
     *
     * @return the TTL in milliseconds
     * @since 1.0.0
     */
    default long getTimeToLive() {
        return 0; // No expiration by default
    }

    /**
     * Checks if this message is a broadcast to all servers.
     *
     * @return true if this is a broadcast message
     * @since 1.0.0
     */
    default boolean isBroadcast() {
        return getTargetServer().isEmpty();
    }

    /**
     * Checks if this message is a response to another message.
     *
     * @return true if this is a response message
     * @since 1.0.0
     */
    default boolean isResponse() {
        return getCorrelationId().isPresent();
    }

    /**
     * Returns additional metadata attached to this message.
     *
     * @param key the metadata key
     * @param <T> the expected value type
     * @return the metadata value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    <T> T getMetadata(@NotNull String key);

    /**
     * Creates a reply message to this message.
     *
     * <p>The reply will have:
     * <ul>
     *   <li>Correlation ID set to this message's ID</li>
     *   <li>Target server set to this message's source server</li>
     * </ul>
     *
     * @param type the reply message type
     * @return a builder for the reply message
     * @since 1.0.0
     */
    @NotNull
    default MessageBuilder reply(@NotNull String type) {
        return MessageBuilder.create(type)
                .correlationId(getMessageId())
                .target(getSourceServer());
    }
}
