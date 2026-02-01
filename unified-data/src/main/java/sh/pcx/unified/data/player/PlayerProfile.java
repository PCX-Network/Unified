/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Complete player profile containing all persistent and transient data.
 *
 * <p>PlayerProfile is the central interface for accessing and modifying player data.
 * It provides type-safe access to data through {@link DataKey}s, handles persistence
 * automatically, and supports cross-server synchronization on networks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private PlayerDataService playerData;
 *
 * public void onPlayerJoin(Player player) {
 *     PlayerProfile profile = playerData.getProfile(player);
 *
 *     // Read data
 *     int kills = profile.getData(MyPluginData.KILLS);
 *     double balance = profile.getData(MyPluginData.BALANCE);
 *     List<String> unlocks = profile.getData(MyPluginData.UNLOCKS);
 *
 *     // Write data
 *     profile.setData(MyPluginData.KILLS, kills + 1);
 *     profile.setData(MyPluginData.LAST_LOGIN, Instant.now());
 *
 *     // Check for data presence
 *     if (profile.hasData(MyPluginData.HOME_LOCATION)) {
 *         Location home = profile.getData(MyPluginData.HOME_LOCATION);
 *         // ...
 *     }
 *
 *     // Profile metadata
 *     Instant firstJoin = profile.getFirstJoin();
 *     Duration playTime = profile.getTotalPlayTime();
 *     boolean online = profile.isOnline();
 * }
 * }</pre>
 *
 * <h2>Data Persistence</h2>
 * <p>Changes to persistent data keys are automatically tracked and saved:
 * <ul>
 *   <li>Automatic saving on configurable intervals (e.g., every 5 minutes)</li>
 *   <li>Automatic saving when player disconnects</li>
 *   <li>Manual saving via {@link #save()}</li>
 *   <li>Dirty tracking to only save changed data</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Read operations can be called from any thread.
 * Write operations are atomic. For compound operations (read-modify-write),
 * use the {@link #compute} method to ensure atomicity.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerDataService
 * @see DataKey
 * @see PlayerProfileBuilder
 */
public interface PlayerProfile {

    // ==================== Identity ====================

    /**
     * Returns the player's unique identifier.
     *
     * @return the player's UUID, never null
     * @since 1.0.0
     */
    @NotNull
    UUID getUniqueId();

    /**
     * Returns the player's last known username.
     *
     * <p>This is updated each time the player joins.
     *
     * @return the player's username, never null
     * @since 1.0.0
     */
    @NotNull
    String getLastKnownName();

    /**
     * Returns the player's name history.
     *
     * <p>Contains all known usernames the player has used, with the most recent first.
     *
     * @return an unmodifiable list of previous usernames
     * @since 1.0.0
     */
    @NotNull
    List<String> getNameHistory();

    // ==================== Timestamps ====================

    /**
     * Returns when the player first joined the server.
     *
     * @return the first join timestamp, never null
     * @since 1.0.0
     */
    @NotNull
    Instant getFirstJoin();

    /**
     * Returns when the player last joined the server.
     *
     * <p>For online players, this is when their current session started.
     * For offline players, this is when they last connected.
     *
     * @return the last join timestamp, never null
     * @since 1.0.0
     */
    @NotNull
    Instant getLastJoin();

    /**
     * Returns when the player was last seen online.
     *
     * <p>For online players, this returns the current time.
     * For offline players, this returns when they last disconnected.
     *
     * @return the last seen timestamp, never null
     * @since 1.0.0
     */
    @NotNull
    Instant getLastSeen();

    /**
     * Returns the player's total play time across all sessions.
     *
     * @return the total play time, never null
     * @since 1.0.0
     */
    @NotNull
    Duration getTotalPlayTime();

    /**
     * Returns the player's play time in their current session.
     *
     * <p>Returns {@link Duration#ZERO} if the player is offline.
     *
     * @return the current session play time, never null
     * @since 1.0.0
     */
    @NotNull
    Duration getSessionPlayTime();

    // ==================== Session ====================

    /**
     * Returns the current session ID.
     *
     * <p>Each login creates a new unique session ID that can be used for
     * logging and tracking. Returns null if the player is offline.
     *
     * @return the session ID, or null if offline
     * @since 1.0.0
     */
    @Nullable
    String getSessionId();

    /**
     * Returns whether the player is currently online.
     *
     * @return true if the player is online on any server
     * @since 1.0.0
     */
    boolean isOnline();

    /**
     * Returns the name of the server the player is currently on.
     *
     * <p>For single-server setups, returns the server name.
     * For networks, returns the backend server name (e.g., "lobby", "survival").
     * Returns empty if the player is offline.
     *
     * @return the current server name, or empty if offline
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getCurrentServer();

    // ==================== Data Access ====================

    /**
     * Retrieves data for the specified key.
     *
     * <p>If no value is set, returns the key's default value (which may be null).
     *
     * @param key the data key
     * @param <T> the type of the value
     * @return the stored value, or the default value
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    @Nullable
    <T> T getData(@NotNull DataKey<T> key);

    /**
     * Retrieves data for the specified key with a custom default.
     *
     * <p>If no value is set and the key has no default, returns the provided default.
     *
     * @param key          the data key
     * @param defaultValue the default value to return if no value is set
     * @param <T>          the type of the value
     * @return the stored value, or the default value
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    @NotNull
    <T> T getData(@NotNull DataKey<T> key, @NotNull T defaultValue);

    /**
     * Stores data for the specified key.
     *
     * <p>For persistent keys, the value will be saved to the database according
     * to the configured save strategy. For keys with {@link SyncStrategy#EAGER},
     * the change is immediately published to other servers.
     *
     * @param key   the data key
     * @param value the value to store, or null to remove
     * @param <T>   the type of the value
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    <T> void setData(@NotNull DataKey<T> key, @Nullable T value);

    /**
     * Removes data for the specified key.
     *
     * <p>After removal, {@link #getData(DataKey)} will return the key's default value.
     *
     * @param key the data key
     * @param <T> the type of the value
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    <T> void removeData(@NotNull DataKey<T> key);

    /**
     * Checks if data is set for the specified key.
     *
     * <p>Returns true if a value has been explicitly set, even if that value is null.
     *
     * @param key the data key
     * @return true if data is set for this key
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    boolean hasData(@NotNull DataKey<?> key);

    /**
     * Returns all data keys that have values set in this profile.
     *
     * @return an unmodifiable set of data keys
     * @since 1.0.0
     */
    @NotNull
    Set<DataKey<?>> getDataKeys();

    /**
     * Atomically computes a new value for the given key.
     *
     * <p>This method is useful for atomic read-modify-write operations.
     * The function receives the current value (or null if not set) and
     * returns the new value to store.
     *
     * @param key      the data key
     * @param function the function to compute the new value
     * @param <T>      the type of the value
     * @return the new computed value
     * @throws NullPointerException if key or function is null
     * @since 1.0.0
     */
    @NotNull
    <T> T compute(@NotNull DataKey<T> key,
                  @NotNull java.util.function.Function<@Nullable T, @NotNull T> function);

    /**
     * Atomically sets data if no value is currently set.
     *
     * @param key   the data key
     * @param value the value to set
     * @param <T>   the type of the value
     * @return true if the value was set, false if a value already existed
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    <T> boolean setDataIfAbsent(@NotNull DataKey<T> key, @NotNull T value);

    // ==================== Persistence ====================

    /**
     * Saves all pending changes to the database.
     *
     * <p>This method is asynchronous and returns immediately. Use the returned
     * future to track completion or handle errors.
     *
     * @return a future that completes when the save is finished
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save();

    /**
     * Reloads the profile from the database.
     *
     * <p>This discards any unsaved changes and loads fresh data from the database.
     * Use with caution as it may overwrite recent changes.
     *
     * @return a future that completes when the reload is finished
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> reload();

    /**
     * Invalidates the cached profile data.
     *
     * <p>The next access will reload from the database. Unlike {@link #reload()},
     * this does not immediately load new data.
     *
     * @since 1.0.0
     */
    void invalidateCache();

    /**
     * Returns whether the profile has unsaved changes.
     *
     * @return true if there are changes that haven't been saved
     * @since 1.0.0
     */
    boolean isDirty();

    /**
     * Returns the timestamp of the last save operation.
     *
     * @return the last save time, or empty if never saved
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getLastSaveTime();

    // ==================== Metadata ====================

    /**
     * Returns internal metadata about this profile.
     *
     * <p>Metadata includes system information like schema version, creation time,
     * and other internal tracking data. This is separate from user data.
     *
     * @return an unmodifiable map of metadata
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> getMetadata();

    /**
     * Returns the data schema version for this profile.
     *
     * <p>The version is incremented when the data schema changes, allowing
     * for data migration when loading older profiles.
     *
     * @return the data version number
     * @since 1.0.0
     */
    long getDataVersion();

    /**
     * Returns the IP address from the player's last login.
     *
     * @return the last known IP address
     * @since 1.0.0
     */
    @NotNull
    String getLastIpAddress();

    // ==================== Locking ====================

    /**
     * Acquires a distributed lock for this profile.
     *
     * <p>While the lock is held, other servers cannot modify this profile's data.
     * Always release the lock when done, preferably using try-with-resources.
     *
     * @return a future that completes with the lock when acquired
     * @since 1.0.0
     * @see PlayerLock
     */
    @NotNull
    CompletableFuture<PlayerLock> acquireLock();

    /**
     * Attempts to acquire a distributed lock without blocking.
     *
     * @return an Optional containing the lock if acquired, or empty if already locked
     * @since 1.0.0
     */
    @NotNull
    Optional<PlayerLock> tryAcquireLock();

    /**
     * Checks if this profile is currently locked by any server.
     *
     * @return true if the profile is locked
     * @since 1.0.0
     */
    boolean isLocked();
}
