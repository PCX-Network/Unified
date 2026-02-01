/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for synchronizing player data across servers in a network.
 *
 * <p>CrossServerSync uses Redis pub/sub to propagate data changes between
 * servers in real-time. It supports different synchronization strategies
 * per data key and handles conflict resolution when concurrent modifications
 * occur.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private CrossServerSync sync;
 *
 * public void onEnable() {
 *     // Listen for incoming sync events
 *     sync.onSync(event -> {
 *         UUID playerId = event.getPlayerId();
 *         Set<DataKey<?>> changed = event.getChangedKeys();
 *
 *         // Handle specific key changes
 *         if (event.hasChanged(MyPlugin.BALANCE)) {
 *             double newBalance = event.getValue(MyPlugin.BALANCE);
 *             updateBalanceDisplay(playerId, newBalance);
 *         }
 *     });
 *
 *     // Manually trigger sync for a player
 *     sync.syncPlayer(playerId, SyncStrategy.EAGER);
 *
 *     // Sync specific keys
 *     sync.syncKeys(playerId, Set.of(MyPlugin.BALANCE, MyPlugin.RANK));
 * }
 * }</pre>
 *
 * <h2>Sync Strategies</h2>
 * <ul>
 *   <li>{@link SyncStrategy#EAGER} - Sync immediately on change</li>
 *   <li>{@link SyncStrategy#LAZY} - Batch changes and sync periodically</li>
 *   <li>{@link SyncStrategy#ON_DEMAND} - Only sync when explicitly requested</li>
 *   <li>{@link SyncStrategy#NONE} - Never sync (local only)</li>
 * </ul>
 *
 * <h2>Conflict Resolution</h2>
 * <p>When concurrent modifications occur on different servers, conflicts are
 * resolved using the configured {@link SyncConflictResolver}. The default
 * resolver uses last-write-wins based on timestamps.
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Sync events may be delivered on any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SyncStrategy
 * @see SyncConflictResolver
 */
public interface CrossServerSync {

    /**
     * Checks if cross-server sync is enabled and connected.
     *
     * @return true if sync is available
     * @since 1.0.0
     */
    boolean isEnabled();

    /**
     * Checks if the sync service is currently connected to Redis.
     *
     * @return true if connected
     * @since 1.0.0
     */
    boolean isConnected();

    /**
     * Returns the name of this server in the network.
     *
     * @return the server identifier
     * @since 1.0.0
     */
    @NotNull
    String getServerName();

    /**
     * Returns the names of all known servers in the network.
     *
     * @return set of server names
     * @since 1.0.0
     */
    @NotNull
    Set<String> getKnownServers();

    /**
     * Synchronizes all dirty data for a player.
     *
     * @param playerId the player's UUID
     * @return a future that completes when sync is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> syncPlayer(@NotNull UUID playerId);

    /**
     * Synchronizes a player's data using a specific strategy.
     *
     * <p>This overrides the per-key sync strategy for this sync operation.
     *
     * @param playerId the player's UUID
     * @param strategy the sync strategy to use
     * @return a future that completes when sync is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> syncPlayer(@NotNull UUID playerId, @NotNull SyncStrategy strategy);

    /**
     * Synchronizes specific keys for a player.
     *
     * @param playerId the player's UUID
     * @param keys     the keys to sync
     * @return a future that completes when sync is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> syncKeys(@NotNull UUID playerId, @NotNull Set<DataKey<?>> keys);

    /**
     * Requests fresh data for a player from the network.
     *
     * <p>This fetches the latest data from Redis/database, useful for
     * ensuring you have the most up-to-date values.
     *
     * @param playerId the player's UUID
     * @return a future containing the refreshed profile
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<PlayerProfile> refreshFromNetwork(@NotNull UUID playerId);

    /**
     * Broadcasts a data change to all other servers.
     *
     * @param playerId the player's UUID
     * @param key      the changed key
     * @param value    the new value
     * @param <T>      the value type
     * @since 1.0.0
     */
    <T> void broadcast(@NotNull UUID playerId, @NotNull DataKey<T> key, @Nullable T value);

    /**
     * Broadcasts multiple data changes to all other servers.
     *
     * @param playerId the player's UUID
     * @param changes  map of keys to new values
     * @since 1.0.0
     */
    void broadcastAll(@NotNull UUID playerId, @NotNull Map<DataKey<?>, Object> changes);

    /**
     * Registers a handler for incoming sync events.
     *
     * @param handler the event handler
     * @since 1.0.0
     */
    void onSync(@NotNull Consumer<SyncEvent> handler);

    /**
     * Unregisters a sync event handler.
     *
     * @param handler the handler to remove
     * @since 1.0.0
     */
    void removeHandler(@NotNull Consumer<SyncEvent> handler);

    /**
     * Sets the conflict resolver for this sync service.
     *
     * @param resolver the conflict resolver
     * @since 1.0.0
     */
    void setConflictResolver(@NotNull SyncConflictResolver resolver);

    /**
     * Returns the current conflict resolver.
     *
     * @return the conflict resolver
     * @since 1.0.0
     */
    @NotNull
    SyncConflictResolver getConflictResolver();

    /**
     * Flushes all pending sync operations immediately.
     *
     * <p>This is useful for ensuring all changes are propagated before
     * a server shutdown or player transfer.
     *
     * @return a future that completes when all pending syncs are done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> flush();

    /**
     * Returns sync statistics.
     *
     * @return sync statistics
     * @since 1.0.0
     */
    @NotNull
    SyncStatistics getStatistics();

    // ==================== Nested Types ====================

    /**
     * Event fired when data is synchronized from another server.
     *
     * @since 1.0.0
     */
    interface SyncEvent {

        /**
         * Returns the player's UUID.
         *
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();

        /**
         * Returns the server that sent this sync.
         *
         * @return the source server name
         */
        @NotNull
        String getSourceServer();

        /**
         * Returns when this sync was sent.
         *
         * @return the sync timestamp
         */
        @NotNull
        Instant getTimestamp();

        /**
         * Returns the keys that were changed.
         *
         * @return set of changed keys
         */
        @NotNull
        Set<DataKey<?>> getChangedKeys();

        /**
         * Checks if a specific key was changed.
         *
         * @param key the key to check
         * @return true if the key was changed
         */
        boolean hasChanged(@NotNull DataKey<?> key);

        /**
         * Returns the new value for a key.
         *
         * @param key the data key
         * @param <T> the value type
         * @return the new value, or null if not in this sync
         */
        @Nullable
        <T> T getValue(@NotNull DataKey<T> key);

        /**
         * Returns the previous value for a key (if known).
         *
         * @param key the data key
         * @param <T> the value type
         * @return the previous value, or null if unknown
         */
        @Nullable
        <T> T getPreviousValue(@NotNull DataKey<T> key);

        /**
         * Returns the player's profile.
         *
         * @return the updated profile
         */
        @NotNull
        PlayerProfile getProfile();
    }

    /**
     * Statistics about sync operations.
     *
     * @since 1.0.0
     */
    interface SyncStatistics {

        /**
         * Returns the total number of syncs sent.
         *
         * @return sent count
         */
        long getSyncsSent();

        /**
         * Returns the total number of syncs received.
         *
         * @return received count
         */
        long getSyncsReceived();

        /**
         * Returns the number of conflicts resolved.
         *
         * @return conflict count
         */
        long getConflictsResolved();

        /**
         * Returns the number of pending syncs in the queue.
         *
         * @return pending count
         */
        int getPendingCount();

        /**
         * Returns the average sync latency.
         *
         * @return average latency
         */
        @NotNull
        Duration getAverageLatency();

        /**
         * Returns when statistics collection started.
         *
         * @return start time
         */
        @NotNull
        Instant getCollectionStartTime();

        /**
         * Returns the last sync timestamp.
         *
         * @return last sync time
         */
        @NotNull
        Instant getLastSyncTime();

        /**
         * Returns the number of bytes sent.
         *
         * @return bytes sent
         */
        long getBytesSent();

        /**
         * Returns the number of bytes received.
         *
         * @return bytes received
         */
        long getBytesReceived();
    }
}
