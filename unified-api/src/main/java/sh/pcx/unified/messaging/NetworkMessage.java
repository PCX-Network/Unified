/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.messaging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for network messages that are transmitted between servers.
 *
 * <p>This interface defines the common metadata that all network messages
 * share. Messages implementing this interface can be sent, received, and
 * tracked across the network.
 *
 * <h2>Message Lifecycle</h2>
 * <ol>
 *   <li>Message is created (by user or as a response)</li>
 *   <li>Message metadata is populated by the messaging service</li>
 *   <li>Message is serialized and transmitted</li>
 *   <li>Message is deserialized on receiving server(s)</li>
 *   <li>Handlers are invoked with the received message</li>
 * </ol>
 *
 * <h2>Implementation</h2>
 * <p>Most messages should use the {@link Message} annotation on records
 * or classes. The messaging service automatically handles the metadata.
 *
 * <pre>{@code
 * @Message(channel = "myplugin:events")
 * public record PlayerJoinedMessage(UUID playerId, String server) {}
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Message
 * @see MessagingService
 */
public interface NetworkMessage {

    /**
     * Returns the unique identifier for this message instance.
     *
     * <p>Each message has a unique ID used for:
     * <ul>
     *   <li>Request/response correlation</li>
     *   <li>Message deduplication</li>
     *   <li>Tracing and debugging</li>
     * </ul>
     *
     * @return the unique message ID
     * @since 1.0.0
     */
    @NotNull
    UUID messageId();

    /**
     * Returns the channel this message was sent on.
     *
     * @return the channel name
     * @since 1.0.0
     */
    @NotNull
    String channel();

    /**
     * Returns the type identifier for this message.
     *
     * <p>This is typically derived from the class name or
     * the {@link Message#value()} annotation attribute.
     *
     * @return the message type
     * @since 1.0.0
     */
    @NotNull
    String type();

    /**
     * Returns the timestamp when this message was created.
     *
     * @return the creation timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant timestamp();

    /**
     * Returns the ID of the server that sent this message.
     *
     * @return the source server ID
     * @since 1.0.0
     */
    @NotNull
    String sourceServer();

    /**
     * Returns the target server ID, if this is a targeted message.
     *
     * <p>Returns empty for broadcast messages.
     *
     * @return the target server, or empty for broadcasts
     * @since 1.0.0
     */
    @NotNull
    Optional<String> targetServer();

    /**
     * Returns the correlation ID for request/response matching.
     *
     * <p>When this message is a response, this contains the ID
     * of the original request message.
     *
     * @return the correlation ID, or empty if not a response
     * @since 1.0.0
     */
    @NotNull
    Optional<UUID> correlationId();

    /**
     * Returns the time-to-live in milliseconds.
     *
     * <p>A value of 0 or negative means no expiration.
     *
     * @return TTL in milliseconds
     * @since 1.0.0
     */
    long ttl();

    /**
     * Checks if this message has expired.
     *
     * @return true if expired
     * @since 1.0.0
     */
    default boolean isExpired() {
        if (ttl() <= 0) {
            return false;
        }
        return Instant.now().isAfter(timestamp().plusMillis(ttl()));
    }

    /**
     * Checks if this is a broadcast message.
     *
     * @return true if broadcast
     * @since 1.0.0
     */
    default boolean isBroadcast() {
        return targetServer().isEmpty();
    }

    /**
     * Checks if this is a response to another message.
     *
     * @return true if this is a response
     * @since 1.0.0
     */
    default boolean isResponse() {
        return correlationId().isPresent();
    }

    /**
     * Returns custom metadata attached to this message.
     *
     * @param key the metadata key
     * @param <T> the expected type
     * @return the value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    <T> T metadata(@NotNull String key);
}
