/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interface for Redis Pub/Sub messaging operations.
 *
 * <p>Redis Pub/Sub provides a messaging system where publishers send messages
 * to channels without knowing which subscribers will receive them. Subscribers
 * express interest in channels and receive messages published to those channels.
 *
 * <h2>Channel Subscriptions</h2>
 * <pre>{@code
 * RedisPubSub pubsub = redis.pubSub();
 *
 * // Subscribe to a specific channel
 * Subscription sub = pubsub.subscribe("chat:general", message -> {
 *     System.out.println("Received: " + message);
 * });
 *
 * // Later, unsubscribe
 * sub.unsubscribe();
 *
 * // Or use try-with-resources
 * try (var subscription = pubsub.subscribe("events", this::handleEvent)) {
 *     // Subscription is active here
 * } // Automatically unsubscribed
 * }</pre>
 *
 * <h2>Pattern Subscriptions</h2>
 * <pre>{@code
 * // Subscribe to all chat channels
 * pubsub.psubscribe("chat:*", (channel, message) -> {
 *     String room = channel.substring("chat:".length());
 *     System.out.println("[" + room + "] " + message);
 * });
 *
 * // Subscribe to multiple patterns
 * pubsub.psubscribe("events:*", (channel, message) -> handleEvent(channel, message));
 * pubsub.psubscribe("alerts:*", (channel, message) -> handleAlert(channel, message));
 * }</pre>
 *
 * <h2>Publishing Messages</h2>
 * <pre>{@code
 * // Publish to a channel
 * long receivers = pubsub.publish("chat:general", "Hello, world!");
 * System.out.println("Message sent to " + receivers + " subscribers");
 *
 * // Async publishing
 * pubsub.publishAsync("events:login", playerName)
 *     .thenAccept(count -> log.debug("Login event sent to " + count + " subscribers"));
 * }</pre>
 *
 * <h2>Cross-Server Communication</h2>
 * <pre>{@code
 * // Server synchronization via Redis
 * pubsub.subscribe("server:sync", message -> {
 *     SyncMessage sync = gson.fromJson(message, SyncMessage.class);
 *     handleServerSync(sync);
 * });
 *
 * // Broadcast to all servers
 * pubsub.publish("server:sync", gson.toJson(new SyncMessage(...)));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe. Message handlers are
 * invoked from Redis connection threads and should avoid blocking operations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisService#pubSub()
 * @see PubSubListener
 */
public interface RedisPubSub {

    // ========== Publishing ==========

    /**
     * Publishes a message to a channel.
     *
     * @param channel the channel name
     * @param message the message to publish
     * @return the number of subscribers that received the message
     * @since 1.0.0
     */
    long publish(@NotNull String channel, @NotNull String message);

    /**
     * Publishes a message to a channel asynchronously.
     *
     * @param channel the channel name
     * @param message the message to publish
     * @return a future that completes with the subscriber count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message);

    /**
     * Publishes a message to multiple channels.
     *
     * @param message  the message to publish
     * @param channels the channel names
     * @return the total number of subscribers that received the message
     * @since 1.0.0
     */
    long publishToMany(@NotNull String message, @NotNull String... channels);

    /**
     * Publishes to multiple channels asynchronously.
     *
     * @param message  the message to publish
     * @param channels the channel names
     * @return a future that completes with the total subscriber count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> publishToManyAsync(@NotNull String message, @NotNull String... channels);

    // ========== Subscribing ==========

    /**
     * Subscribes to a channel.
     *
     * @param channel  the channel name
     * @param listener the message listener
     * @return a subscription handle for managing the subscription
     * @since 1.0.0
     */
    @NotNull
    Subscription subscribe(@NotNull String channel, @NotNull Consumer<String> listener);

    /**
     * Subscribes to multiple channels with the same listener.
     *
     * @param listener the message listener
     * @param channels the channel names
     * @return a subscription handle for all channels
     * @since 1.0.0
     */
    @NotNull
    Subscription subscribe(@NotNull Consumer<String> listener, @NotNull String... channels);

    /**
     * Subscribes to channels matching a pattern.
     *
     * <p>Pattern syntax:
     * <ul>
     *   <li>{@code *} - matches any sequence of characters</li>
     *   <li>{@code ?} - matches any single character</li>
     *   <li>{@code [abc]} - matches any character in the set</li>
     * </ul>
     *
     * @param pattern  the channel pattern (e.g., "chat:*")
     * @param listener the message listener
     * @return a subscription handle
     * @since 1.0.0
     */
    @NotNull
    Subscription psubscribe(@NotNull String pattern, @NotNull PubSubListener listener);

    /**
     * Subscribes to multiple patterns with the same listener.
     *
     * @param listener the message listener
     * @param patterns the channel patterns
     * @return a subscription handle for all patterns
     * @since 1.0.0
     */
    @NotNull
    Subscription psubscribe(@NotNull PubSubListener listener, @NotNull String... patterns);

    // ========== Subscription Management ==========

    /**
     * Gets the set of currently subscribed channels.
     *
     * @return the subscribed channel names
     * @since 1.0.0
     */
    @NotNull
    Set<String> getSubscribedChannels();

    /**
     * Gets the set of currently subscribed patterns.
     *
     * @return the subscribed patterns
     * @since 1.0.0
     */
    @NotNull
    Set<String> getSubscribedPatterns();

    /**
     * Gets the total number of active subscriptions.
     *
     * @return the subscription count
     * @since 1.0.0
     */
    int getSubscriptionCount();

    /**
     * Checks if there are any active subscriptions.
     *
     * @return true if any subscriptions are active
     * @since 1.0.0
     */
    boolean hasSubscriptions();

    /**
     * Unsubscribes from all channels and patterns.
     *
     * @since 1.0.0
     */
    void unsubscribeAll();

    // ========== Channel Information ==========

    /**
     * Gets the number of subscribers to a channel.
     *
     * <p><strong>Note:</strong> This only counts subscribers on the same
     * Redis server, not across a cluster.
     *
     * @param channel the channel name
     * @return the number of subscribers
     * @since 1.0.0
     */
    long getSubscriberCount(@NotNull String channel);

    /**
     * Gets subscriber counts for multiple channels.
     *
     * @param channels the channel names
     * @return a future that completes with channel-to-count mapping
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<java.util.Map<String, Long>> getSubscriberCountsAsync(@NotNull String... channels);

    /**
     * Lists channels with active subscribers matching a pattern.
     *
     * @param pattern the channel pattern (null or "*" for all)
     * @return the active channel names
     * @since 1.0.0
     */
    @NotNull
    Set<String> listActiveChannels(@NotNull String pattern);

    // ========== Typed Messages ==========

    /**
     * Publishes a typed message using serialization.
     *
     * @param <T>        the message type
     * @param channel    the channel name
     * @param message    the message object
     * @param serializer the serializer
     * @return the number of subscribers that received the message
     * @since 1.0.0
     */
    <T> long publish(@NotNull String channel, @NotNull T message, @NotNull RedisSerializer<T> serializer);

    /**
     * Subscribes to typed messages using deserialization.
     *
     * @param <T>        the message type
     * @param channel    the channel name
     * @param type       the message class
     * @param serializer the serializer
     * @param listener   the typed message listener
     * @return a subscription handle
     * @since 1.0.0
     */
    @NotNull
    <T> Subscription subscribe(
            @NotNull String channel,
            @NotNull Class<T> type,
            @NotNull RedisSerializer<T> serializer,
            @NotNull Consumer<T> listener
    );

    /**
     * Subscription handle for managing channel subscriptions.
     *
     * @since 1.0.0
     */
    interface Subscription extends AutoCloseable {

        /**
         * Gets the channels this subscription is for.
         *
         * @return the channel names or patterns
         */
        @NotNull
        Set<String> getChannels();

        /**
         * Checks if this is a pattern subscription.
         *
         * @return true if this is a pattern subscription
         */
        boolean isPatternSubscription();

        /**
         * Checks if this subscription is currently active.
         *
         * @return true if active
         */
        boolean isActive();

        /**
         * Unsubscribes from the channel(s).
         */
        void unsubscribe();

        /**
         * Closes this subscription (same as unsubscribe).
         */
        @Override
        default void close() {
            unsubscribe();
        }

        /**
         * Adds a callback to be called when the subscription ends.
         *
         * @param callback the callback to invoke
         * @return this subscription for chaining
         */
        @NotNull
        Subscription onUnsubscribe(@NotNull Runnable callback);

        /**
         * Gets the number of messages received on this subscription.
         *
         * @return the message count
         */
        long getMessageCount();
    }

    /**
     * Builder for complex subscription configurations.
     *
     * @since 1.0.0
     */
    interface SubscriptionBuilder {

        /**
         * Adds channels to subscribe to.
         *
         * @param channels the channel names
         * @return this builder
         */
        @NotNull
        SubscriptionBuilder channels(@NotNull String... channels);

        /**
         * Adds patterns to subscribe to.
         *
         * @param patterns the channel patterns
         * @return this builder
         */
        @NotNull
        SubscriptionBuilder patterns(@NotNull String... patterns);

        /**
         * Sets the message handler for direct channel subscriptions.
         *
         * @param handler the message handler
         * @return this builder
         */
        @NotNull
        SubscriptionBuilder onMessage(@NotNull Consumer<String> handler);

        /**
         * Sets the message handler for pattern subscriptions.
         *
         * @param handler the pattern message handler
         * @return this builder
         */
        @NotNull
        SubscriptionBuilder onPatternMessage(@NotNull PubSubListener handler);

        /**
         * Sets a callback for when the subscription is established.
         *
         * @param callback the callback
         * @return this builder
         */
        @NotNull
        SubscriptionBuilder onSubscribed(@NotNull Consumer<String> callback);

        /**
         * Sets a callback for when the subscription ends.
         *
         * @param callback the callback
         * @return this builder
         */
        @NotNull
        SubscriptionBuilder onUnsubscribed(@NotNull Consumer<String> callback);

        /**
         * Enables exception handling for message processing.
         *
         * @param handler the exception handler
         * @return this builder
         */
        @NotNull
        SubscriptionBuilder onError(@NotNull java.util.function.BiConsumer<String, Throwable> handler);

        /**
         * Builds and starts the subscription.
         *
         * @return the subscription handle
         */
        @NotNull
        Subscription subscribe();
    }

    /**
     * Creates a subscription builder for complex subscriptions.
     *
     * @return a new subscription builder
     * @since 1.0.0
     */
    @NotNull
    SubscriptionBuilder builder();
}
