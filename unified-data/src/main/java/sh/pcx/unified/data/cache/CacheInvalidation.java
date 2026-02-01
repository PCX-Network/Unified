/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Handles cross-server cache invalidation via pub/sub messaging.
 *
 * <p>CacheInvalidation provides a mechanism for distributed caches to
 * coordinate invalidation across multiple server instances. When a cache
 * entry is invalidated on one server, a message is published to notify
 * other servers to invalidate their local copies.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create an invalidation handler
 * CacheInvalidation invalidation = CacheInvalidation.builder()
 *     .cacheName("player-data")
 *     .publisher(message -> redis.publish("cache:invalidation", message))
 *     .build();
 *
 * // Invalidate a key (broadcasts to all servers)
 * invalidation.invalidate(playerUuid);
 *
 * // Invalidate multiple keys
 * invalidation.invalidateAll(uuidSet);
 *
 * // Subscribe to invalidation messages
 * invalidation.subscribe(message -> {
 *     localCache.invalidate(message.key());
 * });
 * }</pre>
 *
 * <h2>Message Format</h2>
 * <p>Invalidation messages contain:
 * <ul>
 *   <li>Source server identifier</li>
 *   <li>Cache name</li>
 *   <li>Invalidation type (single, multiple, or all)</li>
 *   <li>Affected keys</li>
 *   <li>Timestamp</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DistributedCache
 */
public final class CacheInvalidation {

    private final String cacheName;
    private final String serverId;
    private final MessagePublisher publisher;
    private volatile Consumer<InvalidationMessage> subscriber;

    /**
     * Creates a new cache invalidation handler.
     *
     * @param cacheName the cache name
     * @param serverId  the local server identifier
     * @param publisher the message publisher
     */
    private CacheInvalidation(
            @NotNull String cacheName,
            @NotNull String serverId,
            @NotNull MessagePublisher publisher) {
        this.cacheName = Objects.requireNonNull(cacheName, "cacheName cannot be null");
        this.serverId = Objects.requireNonNull(serverId, "serverId cannot be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher cannot be null");
    }

    /**
     * Returns the cache name.
     *
     * @return the cache name
     * @since 1.0.0
     */
    @NotNull
    public String cacheName() {
        return cacheName;
    }

    /**
     * Returns the local server identifier.
     *
     * @return the server ID
     * @since 1.0.0
     */
    @NotNull
    public String serverId() {
        return serverId;
    }

    /**
     * Invalidates a single key across all servers.
     *
     * @param <K> the key type
     * @param key the key to invalidate
     * @since 1.0.0
     */
    public <K> void invalidate(@NotNull K key) {
        InvalidationMessage message = new InvalidationMessage(
                serverId,
                cacheName,
                InvalidationType.SINGLE,
                Set.of(key.toString()),
                Instant.now()
        );
        publisher.publish(message);
    }

    /**
     * Invalidates multiple keys across all servers.
     *
     * @param <K>  the key type
     * @param keys the keys to invalidate
     * @since 1.0.0
     */
    public <K> void invalidateAll(@NotNull Collection<? extends K> keys) {
        if (keys.isEmpty()) {
            return;
        }

        Set<String> keyStrings = new java.util.HashSet<>();
        for (K key : keys) {
            keyStrings.add(key.toString());
        }

        InvalidationMessage message = new InvalidationMessage(
                serverId,
                cacheName,
                InvalidationType.MULTIPLE,
                keyStrings,
                Instant.now()
        );
        publisher.publish(message);
    }

    /**
     * Invalidates all entries in the cache across all servers.
     *
     * @since 1.0.0
     */
    public void invalidateAll() {
        InvalidationMessage message = new InvalidationMessage(
                serverId,
                cacheName,
                InvalidationType.ALL,
                Set.of(),
                Instant.now()
        );
        publisher.publish(message);
    }

    /**
     * Subscribes to invalidation messages from other servers.
     *
     * @param handler the message handler
     * @since 1.0.0
     */
    public void subscribe(@NotNull Consumer<InvalidationMessage> handler) {
        this.subscriber = handler;
    }

    /**
     * Handles an incoming invalidation message.
     *
     * <p>Messages from the local server are ignored to prevent loops.
     *
     * @param message the invalidation message
     * @since 1.0.0
     */
    public void handleMessage(@NotNull InvalidationMessage message) {
        // Ignore messages from self
        if (serverId.equals(message.sourceServer())) {
            return;
        }

        // Ignore messages for other caches
        if (!cacheName.equals(message.cacheName())) {
            return;
        }

        Consumer<InvalidationMessage> handler = subscriber;
        if (handler != null) {
            handler.accept(message);
        }
    }

    /**
     * Creates a new builder for CacheInvalidation.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Type of invalidation operation.
     *
     * @since 1.0.0
     */
    public enum InvalidationType {
        /** Single key invalidation. */
        SINGLE,
        /** Multiple keys invalidation. */
        MULTIPLE,
        /** All entries invalidation. */
        ALL
    }

    /**
     * Message representing a cache invalidation event.
     *
     * @param sourceServer the server that originated the invalidation
     * @param cacheName    the name of the cache
     * @param type         the type of invalidation
     * @param keys         the affected keys (empty for ALL type)
     * @param timestamp    when the invalidation occurred
     * @since 1.0.0
     */
    public record InvalidationMessage(
            @NotNull String sourceServer,
            @NotNull String cacheName,
            @NotNull InvalidationType type,
            @NotNull Set<String> keys,
            @NotNull Instant timestamp
    ) implements Serializable {

        /**
         * Checks if this is a single-key invalidation.
         *
         * @return true if single key
         */
        public boolean isSingle() {
            return type == InvalidationType.SINGLE;
        }

        /**
         * Checks if this is a multi-key invalidation.
         *
         * @return true if multiple keys
         */
        public boolean isMultiple() {
            return type == InvalidationType.MULTIPLE;
        }

        /**
         * Checks if this invalidates all entries.
         *
         * @return true if all entries
         */
        public boolean isAll() {
            return type == InvalidationType.ALL;
        }

        /**
         * Returns the first (or only) key for single invalidations.
         *
         * @return the key, or null if ALL type
         */
        @Nullable
        public String key() {
            return keys.isEmpty() ? null : keys.iterator().next();
        }
    }

    /**
     * Interface for publishing invalidation messages.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface MessagePublisher {

        /**
         * Publishes an invalidation message to all subscribers.
         *
         * @param message the message to publish
         */
        void publish(@NotNull InvalidationMessage message);
    }

    /**
     * Interface for subscribing to invalidation messages.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface MessageSubscriber {

        /**
         * Called when an invalidation message is received.
         *
         * @param message the received message
         */
        void onMessage(@NotNull InvalidationMessage message);
    }

    /**
     * Builder for CacheInvalidation.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private String cacheName;
        private String serverId;
        private MessagePublisher publisher;

        private Builder() {}

        /**
         * Sets the cache name.
         *
         * @param cacheName the cache name
         * @return this builder
         */
        @NotNull
        public Builder cacheName(@NotNull String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        /**
         * Sets the local server identifier.
         *
         * <p>Defaults to a random UUID if not set.
         *
         * @param serverId the server ID
         * @return this builder
         */
        @NotNull
        public Builder serverId(@NotNull String serverId) {
            this.serverId = serverId;
            return this;
        }

        /**
         * Sets the message publisher.
         *
         * @param publisher the publisher
         * @return this builder
         */
        @NotNull
        public Builder publisher(@NotNull MessagePublisher publisher) {
            this.publisher = publisher;
            return this;
        }

        /**
         * Builds the CacheInvalidation instance.
         *
         * @return a new CacheInvalidation
         * @throws IllegalStateException if required fields are not set
         */
        @NotNull
        public CacheInvalidation build() {
            if (cacheName == null) {
                throw new IllegalStateException("cacheName is required");
            }
            if (publisher == null) {
                throw new IllegalStateException("publisher is required");
            }
            if (serverId == null) {
                serverId = UUID.randomUUID().toString();
            }
            return new CacheInvalidation(cacheName, serverId, publisher);
        }
    }

    @Override
    public String toString() {
        return "CacheInvalidation{cacheName='" + cacheName + "', serverId='" + serverId + "'}";
    }
}
