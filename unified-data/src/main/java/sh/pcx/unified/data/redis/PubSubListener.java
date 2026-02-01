/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;

/**
 * Listener interface for Redis Pub/Sub messages.
 *
 * <p>This interface is used for pattern-based subscriptions where the
 * message handler needs to know both the channel and the message.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Subscribe to a pattern
 * PubSubListener listener = (channel, message) -> {
 *     System.out.println("Received on " + channel + ": " + message);
 * };
 * redis.psubscribe("chat:*", listener);
 *
 * // Using method reference
 * redis.psubscribe("events:*", this::handleEvent);
 *
 * // Using lambda with pattern matching
 * redis.psubscribe("notifications:*", (channel, message) -> {
 *     String type = channel.substring("notifications:".length());
 *     switch (type) {
 *         case "alert" -> showAlert(message);
 *         case "info" -> showInfo(message);
 *         default -> log.debug("Unknown notification type: " + type);
 *     }
 * });
 * }</pre>
 *
 * <h2>Pattern Subscriptions</h2>
 * <p>Pattern subscriptions use glob-style patterns:
 * <ul>
 *   <li>{@code *} - matches any sequence of characters</li>
 *   <li>{@code ?} - matches any single character</li>
 *   <li>{@code [abc]} - matches any character in the set</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Message handlers are called from Redis connection threads. Implementations
 * should be thread-safe and avoid blocking operations. For complex processing,
 * consider offloading work to an async executor.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisPubSub
 * @see RedisService#psubscribe(String, PubSubListener)
 */
@FunctionalInterface
public interface PubSubListener {

    /**
     * Called when a message is received on a matching channel.
     *
     * <p>This method is invoked from the Redis connection thread.
     * Implementations should be fast and non-blocking. For long-running
     * operations, consider using an async executor.
     *
     * @param channel the channel the message was published to
     * @param message the message content
     * @since 1.0.0
     */
    void onMessage(@NotNull String channel, @NotNull String message);

    /**
     * Creates a listener that ignores the channel and only processes the message.
     *
     * <p>Useful when subscribing to a single channel pattern where the
     * channel name isn't needed.
     *
     * @param messageHandler the message handler
     * @return a PubSubListener that delegates to the message handler
     * @since 1.0.0
     */
    static PubSubListener messageOnly(@NotNull java.util.function.Consumer<String> messageHandler) {
        return (channel, message) -> messageHandler.accept(message);
    }

    /**
     * Creates a listener that extracts a suffix from the channel.
     *
     * <p>Useful for pattern subscriptions where the channel suffix
     * contains meaningful information.
     *
     * <pre>{@code
     * // For pattern "user:*:events", extract the user ID
     * PubSubListener listener = PubSubListener.withSuffix(
     *     "user:",
     *     (suffix, message) -> {
     *         String userId = suffix.substring(0, suffix.indexOf(":events"));
     *         handleUserEvent(userId, message);
     *     }
     * );
     * }</pre>
     *
     * @param prefix  the prefix to strip from the channel
     * @param handler the handler that receives the suffix and message
     * @return a PubSubListener that extracts and passes the suffix
     * @since 1.0.0
     */
    static PubSubListener withSuffix(@NotNull String prefix, @NotNull SuffixHandler handler) {
        return (channel, message) -> {
            String suffix = channel.startsWith(prefix)
                    ? channel.substring(prefix.length())
                    : channel;
            handler.onMessage(suffix, message);
        };
    }

    /**
     * Creates a listener that logs messages and delegates to another listener.
     *
     * @param delegate the delegate listener
     * @param logger   the logging function (receives formatted log message)
     * @return a logging listener
     * @since 1.0.0
     */
    static PubSubListener logging(
            @NotNull PubSubListener delegate,
            @NotNull java.util.function.Consumer<String> logger
    ) {
        return (channel, message) -> {
            logger.accept("PubSub [" + channel + "]: " + message);
            delegate.onMessage(channel, message);
        };
    }

    /**
     * Creates a listener that filters messages based on channel.
     *
     * @param delegate  the delegate listener
     * @param predicate the channel filter
     * @return a filtering listener
     * @since 1.0.0
     */
    static PubSubListener filter(
            @NotNull PubSubListener delegate,
            @NotNull java.util.function.Predicate<String> predicate
    ) {
        return (channel, message) -> {
            if (predicate.test(channel)) {
                delegate.onMessage(channel, message);
            }
        };
    }

    /**
     * Creates a listener that wraps exceptions and continues processing.
     *
     * @param delegate     the delegate listener
     * @param errorHandler the error handler
     * @return an error-handling listener
     * @since 1.0.0
     */
    static PubSubListener catching(
            @NotNull PubSubListener delegate,
            @NotNull java.util.function.BiConsumer<Exception, String> errorHandler
    ) {
        return (channel, message) -> {
            try {
                delegate.onMessage(channel, message);
            } catch (Exception e) {
                errorHandler.accept(e, "Error handling message on channel: " + channel);
            }
        };
    }

    /**
     * Handler interface for suffix-based message processing.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface SuffixHandler {
        /**
         * Handles a message with the extracted suffix.
         *
         * @param suffix  the channel suffix (after prefix is stripped)
         * @param message the message content
         */
        void onMessage(@NotNull String suffix, @NotNull String message);
    }
}
