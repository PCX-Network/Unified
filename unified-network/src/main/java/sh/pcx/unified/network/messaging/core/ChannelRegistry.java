/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.core;

import sh.pcx.unified.network.messaging.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Registry for managing message channels.
 *
 * <p>The ChannelRegistry maintains a collection of all registered message channels
 * and provides methods for creating, retrieving, and managing channels. It ensures
 * channel names are unique and handles channel lifecycle.
 *
 * <h2>Channel Naming</h2>
 * <p>Channel names must be unique and should follow the format {@code namespace:name}.
 * The namespace prevents collisions between plugins.
 *
 * <h2>Built-in Channels</h2>
 * <p>The following channels are registered automatically:
 * <ul>
 *   <li>{@code unified:system} - System messages (heartbeats, discovery)</li>
 *   <li>{@code unified:player} - Player-related events</li>
 *   <li>{@code unified:server} - Server status updates</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ChannelRegistry registry = messaging.getChannelRegistry();
 *
 * // Register a new channel
 * MessageChannel<MyMessage> channel = registry.register(
 *     "myplugin:events",
 *     MyMessage.class
 * );
 *
 * // Get an existing channel
 * registry.get("myplugin:events", MyMessage.class)
 *     .ifPresent(ch -> ch.send(new MyMessage("Hello")));
 *
 * // Find channels by namespace
 * Collection<MessageChannel<?>> myChannels = registry.findByNamespace("myplugin");
 *
 * // Unregister when done
 * registry.unregister("myplugin:events");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This registry is thread-safe. All operations can be called from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessageChannel
 * @see MessagingService
 */
public interface ChannelRegistry {

    /**
     * The system channel name for internal messaging.
     */
    String SYSTEM_CHANNEL = "unified:system";

    /**
     * The player channel name for player-related events.
     */
    String PLAYER_CHANNEL = "unified:player";

    /**
     * The server channel name for server status updates.
     */
    String SERVER_CHANNEL = "unified:server";

    // =========================================================================
    // Registration
    // =========================================================================

    /**
     * Registers a new message channel.
     *
     * @param <T>          the message type
     * @param name         the unique channel name
     * @param messageClass the message class for this channel
     * @return the registered channel
     * @throws IllegalArgumentException if the name is already registered
     * @since 1.0.0
     */
    @NotNull
    <T extends Message> MessageChannel<T> register(
            @NotNull String name,
            @NotNull Class<T> messageClass
    );

    /**
     * Registers a new message channel with custom options.
     *
     * @param <T>          the message type
     * @param name         the unique channel name
     * @param messageClass the message class for this channel
     * @param options      the channel options
     * @return the registered channel
     * @throws IllegalArgumentException if the name is already registered
     * @since 1.0.0
     */
    @NotNull
    <T extends Message> MessageChannel<T> register(
            @NotNull String name,
            @NotNull Class<T> messageClass,
            @NotNull ChannelOptions options
    );

    /**
     * Gets or creates a channel.
     *
     * <p>If the channel already exists, it is returned. Otherwise, a new
     * channel is registered with the given message class.
     *
     * @param <T>          the message type
     * @param name         the channel name
     * @param messageClass the message class
     * @return the channel
     * @since 1.0.0
     */
    @NotNull
    <T extends Message> MessageChannel<T> getOrCreate(
            @NotNull String name,
            @NotNull Class<T> messageClass
    );

    /**
     * Unregisters a channel.
     *
     * <p>The channel is closed and all its listeners are removed.
     *
     * @param name the channel name
     * @return true if the channel was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull String name);

    /**
     * Unregisters all channels with a specific namespace.
     *
     * @param namespace the namespace prefix (e.g., "myplugin")
     * @return the number of channels unregistered
     * @since 1.0.0
     */
    int unregisterByNamespace(@NotNull String namespace);

    // =========================================================================
    // Retrieval
    // =========================================================================

    /**
     * Gets a channel by name.
     *
     * @param name the channel name
     * @return the channel, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<MessageChannel<?>> get(@NotNull String name);

    /**
     * Gets a channel by name with type checking.
     *
     * @param <T>          the message type
     * @param name         the channel name
     * @param messageClass the expected message class
     * @return the channel, or empty if not found or type mismatch
     * @since 1.0.0
     */
    @NotNull
    <T extends Message> Optional<MessageChannel<T>> get(
            @NotNull String name,
            @NotNull Class<T> messageClass
    );

    /**
     * Gets a channel or throws an exception.
     *
     * @param name the channel name
     * @return the channel
     * @throws IllegalArgumentException if the channel is not found
     * @since 1.0.0
     */
    @NotNull
    default MessageChannel<?> getOrThrow(@NotNull String name) {
        return get(name).orElseThrow(() ->
                new IllegalArgumentException("Channel not found: " + name));
    }

    /**
     * Gets a channel with type checking or throws an exception.
     *
     * @param <T>          the message type
     * @param name         the channel name
     * @param messageClass the expected message class
     * @return the channel
     * @throws IllegalArgumentException if the channel is not found or type mismatch
     * @since 1.0.0
     */
    @NotNull
    default <T extends Message> MessageChannel<T> getOrThrow(
            @NotNull String name,
            @NotNull Class<T> messageClass
    ) {
        return get(name, messageClass).orElseThrow(() ->
                new IllegalArgumentException("Channel not found or type mismatch: " + name));
    }

    /**
     * Checks if a channel is registered.
     *
     * @param name the channel name
     * @return true if the channel exists
     * @since 1.0.0
     */
    boolean contains(@NotNull String name);

    // =========================================================================
    // Querying
    // =========================================================================

    /**
     * Returns all registered channels.
     *
     * @return all channels
     * @since 1.0.0
     */
    @NotNull
    Collection<MessageChannel<?>> getAll();

    /**
     * Returns all channel names.
     *
     * @return all channel names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getNames();

    /**
     * Finds channels by namespace prefix.
     *
     * @param namespace the namespace prefix (e.g., "myplugin")
     * @return matching channels
     * @since 1.0.0
     */
    @NotNull
    Collection<MessageChannel<?>> findByNamespace(@NotNull String namespace);

    /**
     * Finds channels matching a predicate.
     *
     * @param predicate the filter predicate
     * @return matching channels
     * @since 1.0.0
     */
    @NotNull
    Collection<MessageChannel<?>> findAll(@NotNull Predicate<MessageChannel<?>> predicate);

    /**
     * Returns the number of registered channels.
     *
     * @return the channel count
     * @since 1.0.0
     */
    int size();

    /**
     * Checks if the registry is empty.
     *
     * @return true if no channels are registered
     * @since 1.0.0
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Closes all channels and clears the registry.
     *
     * <p>This is called during shutdown.
     *
     * @since 1.0.0
     */
    void closeAll();

    /**
     * Options for configuring a channel.
     *
     * @since 1.0.0
     */
    interface ChannelOptions {

        /**
         * Creates a new options builder.
         *
         * @return a new builder
         */
        @NotNull
        static Builder builder() {
            return new Builder();
        }

        /**
         * Returns the default channel options.
         *
         * @return default options
         */
        @NotNull
        static ChannelOptions defaults() {
            return builder().build();
        }

        /**
         * Whether to buffer messages when disconnected.
         *
         * @return true if buffering is enabled
         */
        boolean isBuffered();

        /**
         * Maximum buffer size when buffering is enabled.
         *
         * @return the buffer size
         */
        int getBufferSize();

        /**
         * Whether to compress messages.
         *
         * @return true if compression is enabled
         */
        boolean isCompressed();

        /**
         * Whether to enable message deduplication.
         *
         * @return true if deduplication is enabled
         */
        boolean isDeduplicationEnabled();

        /**
         * Time in milliseconds to keep message IDs for deduplication.
         *
         * @return the deduplication window in milliseconds
         */
        long getDeduplicationWindow();

        /**
         * Builder for channel options.
         */
        class Builder {
            private boolean buffered = false;
            private int bufferSize = 100;
            private boolean compressed = false;
            private boolean deduplication = false;
            private long deduplicationWindow = 60000;

            /**
             * Enables message buffering when disconnected.
             *
             * @param enabled true to enable buffering
             * @return this builder
             */
            @NotNull
            public Builder buffered(boolean enabled) {
                this.buffered = enabled;
                return this;
            }

            /**
             * Sets the buffer size.
             *
             * @param size the buffer size
             * @return this builder
             */
            @NotNull
            public Builder bufferSize(int size) {
                this.bufferSize = size;
                return this;
            }

            /**
             * Enables message compression.
             *
             * @param enabled true to enable compression
             * @return this builder
             */
            @NotNull
            public Builder compressed(boolean enabled) {
                this.compressed = enabled;
                return this;
            }

            /**
             * Enables message deduplication.
             *
             * @param enabled true to enable deduplication
             * @return this builder
             */
            @NotNull
            public Builder deduplication(boolean enabled) {
                this.deduplication = enabled;
                return this;
            }

            /**
             * Sets the deduplication window.
             *
             * @param milliseconds the window in milliseconds
             * @return this builder
             */
            @NotNull
            public Builder deduplicationWindow(long milliseconds) {
                this.deduplicationWindow = milliseconds;
                return this;
            }

            /**
             * Builds the options.
             *
             * @return the options
             */
            @NotNull
            public ChannelOptions build() {
                return new ChannelOptions() {
                    @Override
                    public boolean isBuffered() {
                        return buffered;
                    }

                    @Override
                    public int getBufferSize() {
                        return bufferSize;
                    }

                    @Override
                    public boolean isCompressed() {
                        return compressed;
                    }

                    @Override
                    public boolean isDeduplicationEnabled() {
                        return deduplication;
                    }

                    @Override
                    public long getDeduplicationWindow() {
                        return deduplicationWindow;
                    }
                };
            }
        }
    }
}
