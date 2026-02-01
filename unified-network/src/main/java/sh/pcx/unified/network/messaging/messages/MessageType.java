/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.messages;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define the type identifier for a message class.
 *
 * <p>This annotation is used by the messaging system to automatically
 * register and route messages based on their type. The value should
 * be unique within your plugin's namespace.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @MessageType("player_join")
 * public class PlayerJoinMessage extends AbstractMessage {
 *     private final UUID playerId;
 *     private final String playerName;
 *     private final String server;
 *
 *     public PlayerJoinMessage(UUID playerId, String playerName, String server) {
 *         this.playerId = playerId;
 *         this.playerName = playerName;
 *         this.server = server;
 *     }
 *
 *     // ... getters
 * }
 *
 * // Register handler for this message type
 * messaging.registerHandler(PlayerJoinMessage.class, message -> {
 *     getLogger().info(message.getPlayerName() + " joined " + message.getServer());
 * });
 * }</pre>
 *
 * <h2>Naming Conventions</h2>
 * <p>Message type identifiers should follow these conventions:
 * <ul>
 *   <li>Use lowercase with underscores: {@code player_update}</li>
 *   <li>Prefix with plugin name for uniqueness: {@code myplugin:player_update}</li>
 *   <li>Keep names short but descriptive</li>
 *   <li>Use verb_noun format: {@code sync_data}, {@code request_info}</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Message
 * @see MessageSerializer
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MessageType {

    /**
     * The unique type identifier for this message class.
     *
     * <p>This value is used to:
     * <ul>
     *   <li>Serialize/deserialize messages correctly</li>
     *   <li>Route messages to the appropriate handler</li>
     *   <li>Identify message types in logs and debugging</li>
     * </ul>
     *
     * @return the message type identifier
     * @since 1.0.0
     */
    String value();

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
     * Whether this message type requires a response.
     *
     * <p>If true, senders should expect a response and the receiver
     * is expected to reply. This is used for request/response patterns.
     *
     * @return true if a response is expected
     * @since 1.0.0
     */
    boolean expectsResponse() default false;

    /**
     * The expected response message type.
     *
     * <p>Only relevant if {@link #expectsResponse()} is true.
     * Specifies the type of message expected as a response.
     *
     * @return the response message type, or empty if not applicable
     * @since 1.0.0
     */
    String responseType() default "";

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
}
