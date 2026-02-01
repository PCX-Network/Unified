/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Interface representing a player's current session data.
 *
 * <p>A session begins when a player joins the server and ends when they disconnect.
 * Session data is transient and is cleared when the session ends. Use this interface
 * to store temporary per-player data that should not persist across logins.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PlayerSession session = player.getSession();
 *
 * // Store session data
 * session.set("last_command_time", Instant.now());
 * session.set("combat_tagged", true);
 * session.set("kills_this_session", 5);
 *
 * // Retrieve session data
 * Optional<Instant> lastCommand = session.get("last_command_time", Instant.class);
 * boolean inCombat = session.getOrDefault("combat_tagged", false);
 * int kills = session.getOrDefault("kills_this_session", 0);
 *
 * // Check session info
 * Duration playTime = session.getDuration();
 * Instant joinTime = session.getJoinTime();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All session operations are thread-safe. However, if you need to perform
 * compound operations (read-modify-write), use the {@link #compute} method
 * to ensure atomicity.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlayer#getSession()
 */
public interface PlayerSession {

    /**
     * Returns the unique session ID.
     *
     * <p>Each session has a unique identifier that can be used for
     * logging, tracking, or correlating events within a single session.
     *
     * @return the unique session identifier
     * @since 1.0.0
     */
    @NotNull
    String getSessionId();

    /**
     * Returns the UUID of the player this session belongs to.
     *
     * @return the player's UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getPlayerId();

    /**
     * Returns the time when this session started.
     *
     * @return the instant when the player joined
     * @since 1.0.0
     */
    @NotNull
    Instant getJoinTime();

    /**
     * Returns how long the session has been active.
     *
     * @return the duration since the session started
     * @since 1.0.0
     */
    @NotNull
    Duration getDuration();

    /**
     * Stores a value in the session.
     *
     * @param key   the key to store the value under
     * @param value the value to store, or null to remove the key
     * @param <T>   the type of the value
     * @since 1.0.0
     */
    <T> void set(@NotNull String key, @Nullable T value);

    /**
     * Retrieves a value from the session.
     *
     * @param key  the key to retrieve
     * @param type the expected type of the value
     * @param <T>  the type of the value
     * @return an Optional containing the value if present and of correct type
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type);

    /**
     * Retrieves a value from the session, returning a default if not present.
     *
     * @param key          the key to retrieve
     * @param defaultValue the default value to return if key is not present
     * @param <T>          the type of the value
     * @return the stored value or the default value
     * @since 1.0.0
     */
    @NotNull
    <T> T getOrDefault(@NotNull String key, @NotNull T defaultValue);

    /**
     * Checks if a key exists in the session.
     *
     * @param key the key to check
     * @return true if the key exists
     * @since 1.0.0
     */
    boolean has(@NotNull String key);

    /**
     * Removes a value from the session.
     *
     * @param key the key to remove
     * @return true if the key was present and removed
     * @since 1.0.0
     */
    boolean remove(@NotNull String key);

    /**
     * Clears all session data.
     *
     * <p>This removes all stored values but does not end the session.
     *
     * @since 1.0.0
     */
    void clear();

    /**
     * Returns all keys currently stored in the session.
     *
     * @return an unmodifiable set of all keys
     * @since 1.0.0
     */
    @NotNull
    Set<String> getKeys();

    /**
     * Returns the number of entries in the session.
     *
     * @return the number of stored key-value pairs
     * @since 1.0.0
     */
    int size();

    /**
     * Checks if the session is empty.
     *
     * @return true if no data is stored
     * @since 1.0.0
     */
    boolean isEmpty();

    /**
     * Atomically computes a new value for the given key.
     *
     * <p>This method is useful for atomic read-modify-write operations.
     *
     * @param key      the key to compute
     * @param type     the type of the value
     * @param function the function to compute the new value
     * @param <T>      the type of the value
     * @return the new computed value
     * @since 1.0.0
     */
    @NotNull
    <T> T compute(@NotNull String key, @NotNull Class<T> type,
                  @NotNull java.util.function.Function<Optional<T>, T> function);

    /**
     * Atomically sets a value if the key is not already present.
     *
     * @param key   the key to set
     * @param value the value to set
     * @param <T>   the type of the value
     * @return true if the value was set, false if key already existed
     * @since 1.0.0
     */
    <T> boolean setIfAbsent(@NotNull String key, @NotNull T value);

    /**
     * Increments an integer value atomically.
     *
     * <p>If the key does not exist, it is initialized to 0 before incrementing.
     *
     * @param key   the key to increment
     * @param delta the amount to add (can be negative)
     * @return the new value after incrementing
     * @since 1.0.0
     */
    int increment(@NotNull String key, int delta);

    /**
     * Increments an integer value by 1 atomically.
     *
     * @param key the key to increment
     * @return the new value after incrementing
     * @since 1.0.0
     */
    default int increment(@NotNull String key) {
        return increment(key, 1);
    }

    /**
     * Decrements an integer value by 1 atomically.
     *
     * @param key the key to decrement
     * @return the new value after decrementing
     * @since 1.0.0
     */
    default int decrement(@NotNull String key) {
        return increment(key, -1);
    }

    /**
     * Returns an immutable snapshot of all session data.
     *
     * <p>The returned map is a copy and will not reflect future changes.
     *
     * @return an immutable map of all session data
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> toMap();

    /**
     * Returns the IP address associated with this session.
     *
     * @return the player's IP address
     * @since 1.0.0
     */
    @NotNull
    String getIpAddress();

    /**
     * Returns the server name this session is connected to.
     *
     * <p>For single-server setups, this returns the server name.
     * For network setups, this returns the backend server name.
     *
     * @return the server name
     * @since 1.0.0
     */
    @NotNull
    String getServerName();

    /**
     * Checks if this session is still valid (player is still online).
     *
     * @return true if the session is still active
     * @since 1.0.0
     */
    boolean isValid();
}
