/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.messaging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining typed cross-server messages.
 *
 * <p>This annotation is used to define message types that can be sent across
 * servers in a Minecraft network. It supports both records and classes for
 * maximum flexibility.
 *
 * <h2>Example with Records (Recommended)</h2>
 * <pre>{@code
 * @Message(channel = "mynetwork:games")
 * public record GameStartMessage(
 *     String arenaId,
 *     List<UUID> players,
 *     Instant startTime
 * ) {}
 *
 * // Send the message
 * messaging.send(new GameStartMessage("arena1", players, Instant.now()));
 *
 * // Subscribe to messages
 * messaging.subscribe(GameStartMessage.class, message -> {
 *     log.info("Game starting in " + message.arenaId());
 * });
 * }</pre>
 *
 * <h2>Request/Response Pattern</h2>
 * <pre>{@code
 * @Message(channel = "mynetwork:data", responseType = PlayerDataResponse.class)
 * public record PlayerDataRequest(UUID playerId) {}
 *
 * @Message(channel = "mynetwork:data")
 * public record PlayerDataResponse(UUID playerId, String name, int level) {}
 *
 * // Send request and await response
 * messaging.request(new PlayerDataRequest(uuid))
 *     .thenAccept(response -> {
 *         log.info("Player level: " + response.level());
 *     });
 * }</pre>
 *
 * <h2>Channel Naming Conventions</h2>
 * <p>Channel names should follow the format: {@code namespace:name}
 * <ul>
 *   <li>{@code myplugin:player_sync} - Player synchronization</li>
 *   <li>{@code myplugin:economy} - Economy transactions</li>
 *   <li>{@code network:broadcast} - Network-wide announcements</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessagingService
 * @see NetworkMessage
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Message {

    /**
     * The channel this message type belongs to.
     *
     * <p>Messages are routed based on their channel. Only subscribers
     * to the same channel will receive the message.
     *
     * <p>Format: {@code namespace:name} (e.g., "myplugin:events")
     *
     * @return the channel name
     * @since 1.0.0
     */
    String channel();

    /**
     * Optional identifier for the message type within the channel.
     *
     * <p>If not specified, the class simple name is used. This is useful
     * when you want a shorter or different identifier in the wire format.
     *
     * @return the message type identifier
     * @since 1.0.0
     */
    String value() default "";

    /**
     * The version of this message format.
     *
     * <p>Use this for backwards compatibility when message formats change.
     * The serializer can use this to handle different versions appropriately.
     *
     * @return the message format version
     * @since 1.0.0
     */
    int version() default 1;

    /**
     * The expected response type for request/response patterns.
     *
     * <p>When set, this message is considered a request that expects
     * a response of the specified type.
     *
     * @return the response message class, or Void.class if no response expected
     * @since 1.0.0
     */
    Class<?> responseType() default Void.class;

    /**
     * Default time-to-live for messages of this type in milliseconds.
     *
     * <p>Messages older than this TTL will be discarded by receivers.
     * A value of 0 means no expiration.
     *
     * @return the default TTL in milliseconds
     * @since 1.0.0
     */
    long ttl() default 0;

    /**
     * Whether this message should be persisted for delivery
     * to servers that connect later.
     *
     * <p>Persistent messages are stored and delivered when a new server
     * connects. This is useful for configuration sync or important
     * announcements.
     *
     * @return true if the message should be persistent
     * @since 1.0.0
     */
    boolean persistent() default false;

    /**
     * Priority level for message delivery.
     *
     * <p>Higher priority messages are processed before lower priority ones.
     *
     * @return the priority level (0-10, default 5)
     * @since 1.0.0
     */
    int priority() default 5;
}
