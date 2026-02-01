/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.snapshot;

import sh.pcx.unified.data.inventory.core.InventorySnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for persisting and retrieving inventory snapshots.
 *
 * <p>SnapshotStorage provides an abstraction layer for snapshot persistence,
 * allowing different storage backends (database, file system, cache, etc.)
 * to be used interchangeably.
 *
 * <h2>Storage Backends</h2>
 * <ul>
 *   <li><b>Database</b>: MySQL, PostgreSQL, MongoDB for production use</li>
 *   <li><b>File</b>: YAML, JSON files for simple setups</li>
 *   <li><b>Memory</b>: In-memory storage for testing</li>
 *   <li><b>Redis</b>: For high-performance caching</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class MySQLSnapshotStorage implements SnapshotStorage {
 *
 *     private final DataSource dataSource;
 *
 *     @Override
 *     public CompletableFuture<Void> save(String id, InventorySnapshot snapshot,
 *                                         SnapshotMetadata metadata) {
 *         return CompletableFuture.runAsync(() -> {
 *             try (Connection conn = dataSource.getConnection()) {
 *                 PreparedStatement stmt = conn.prepareStatement(
 *                     "INSERT INTO snapshots (id, player_id, data, metadata) " +
 *                     "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE data = ?, metadata = ?"
 *                 );
 *                 // ... bind parameters and execute
 *             }
 *         });
 *     }
 *
 *     @Override
 *     public CompletableFuture<Optional<InventorySnapshot>> load(String id) {
 *         return CompletableFuture.supplyAsync(() -> {
 *             // ... query and deserialize
 *         });
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations must be thread-safe. Operations return CompletableFutures
 * and should not block the calling thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SnapshotManager
 * @see SnapshotMetadata
 */
public interface SnapshotStorage {

    // ========== Core Operations ==========

    /**
     * Saves a snapshot with its metadata.
     *
     * @param id       the unique snapshot ID
     * @param snapshot the snapshot to save
     * @param metadata the snapshot metadata
     * @return a future that completes when saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save(
        @NotNull String id,
        @NotNull InventorySnapshot snapshot,
        @NotNull SnapshotMetadata metadata
    );

    /**
     * Loads a snapshot by ID.
     *
     * @param id the snapshot ID
     * @return a future containing the snapshot if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<InventorySnapshot>> load(@NotNull String id);

    /**
     * Loads snapshot metadata by ID.
     *
     * @param id the snapshot ID
     * @return a future containing the metadata if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<SnapshotMetadata>> loadMetadata(@NotNull String id);

    /**
     * Deletes a snapshot by ID.
     *
     * @param id the snapshot ID
     * @return a future that completes with true if deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> delete(@NotNull String id);

    /**
     * Checks if a snapshot exists.
     *
     * @param id the snapshot ID
     * @return a future containing true if exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists(@NotNull String id);

    // ========== Player-based Operations ==========

    /**
     * Loads the most recent snapshot for a player.
     *
     * @param playerId the player's UUID
     * @return a future containing the snapshot if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<InventorySnapshot>> loadLatest(@NotNull UUID playerId);

    /**
     * Loads a named snapshot for a player.
     *
     * @param playerId the player's UUID
     * @param name     the snapshot name
     * @return a future containing the snapshot if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<InventorySnapshot>> loadByName(
        @NotNull UUID playerId,
        @NotNull String name
    );

    /**
     * Lists all snapshots for a player.
     *
     * @param playerId the player's UUID
     * @return a future containing the list of metadata
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<SnapshotMetadata>> listByPlayer(@NotNull UUID playerId);

    /**
     * Lists snapshots for a player with pagination.
     *
     * @param playerId the player's UUID
     * @param offset   the starting index
     * @param limit    maximum number to return
     * @return a future containing the list of metadata
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<SnapshotMetadata>> listByPlayer(
        @NotNull UUID playerId,
        int offset,
        int limit
    );

    /**
     * Deletes all snapshots for a player.
     *
     * @param playerId the player's UUID
     * @return a future that completes with count of deleted snapshots
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> deleteByPlayer(@NotNull UUID playerId);

    /**
     * Counts snapshots for a player.
     *
     * @param playerId the player's UUID
     * @return a future containing the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> countByPlayer(@NotNull UUID playerId);

    // ========== Query Operations ==========

    /**
     * Searches snapshots by criteria.
     *
     * @param criteria the search criteria
     * @return a future containing matching metadata
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<SnapshotMetadata>> search(@NotNull SearchCriteria criteria);

    /**
     * Lists snapshots by reason.
     *
     * @param reason the snapshot reason
     * @param limit  maximum number to return
     * @return a future containing the list of metadata
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<SnapshotMetadata>> listByReason(
        @NotNull String reason,
        int limit
    );

    /**
     * Lists snapshots created within a time range.
     *
     * @param from  start time (inclusive)
     * @param to    end time (exclusive)
     * @param limit maximum number to return
     * @return a future containing the list of metadata
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<SnapshotMetadata>> listByTimeRange(
        @NotNull Instant from,
        @NotNull Instant to,
        int limit
    );

    // ========== Maintenance Operations ==========

    /**
     * Deletes snapshots older than a certain age.
     *
     * @param olderThan delete snapshots older than this
     * @return a future that completes with count of deleted snapshots
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> deleteOlderThan(@NotNull Instant olderThan);

    /**
     * Deletes snapshots older than a certain age for a specific reason.
     *
     * @param olderThan delete snapshots older than this
     * @param reason    only delete snapshots with this reason
     * @return a future that completes with count of deleted snapshots
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> deleteOlderThan(
        @NotNull Instant olderThan,
        @NotNull String reason
    );

    /**
     * Gets storage statistics.
     *
     * @return a future containing storage statistics
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<StorageStats> getStats();

    /**
     * Performs storage cleanup and optimization.
     *
     * @return a future that completes when cleanup is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> cleanup();

    /**
     * Closes the storage and releases resources.
     *
     * @return a future that completes when closed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> close();

    // ========== Nested Types ==========

    /**
     * Search criteria for querying snapshots.
     *
     * @since 1.0.0
     */
    record SearchCriteria(
        @Nullable UUID playerId,
        @Nullable String name,
        @Nullable String reason,
        @Nullable String serverName,
        @Nullable String worldName,
        @Nullable Instant createdAfter,
        @Nullable Instant createdBefore,
        int offset,
        int limit
    ) {
        /**
         * Creates a builder for SearchCriteria.
         *
         * @return a new builder
         */
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for SearchCriteria.
         */
        public static final class Builder {
            private UUID playerId;
            private String name;
            private String reason;
            private String serverName;
            private String worldName;
            private Instant createdAfter;
            private Instant createdBefore;
            private int offset = 0;
            private int limit = 100;

            @NotNull
            public Builder playerId(@Nullable UUID playerId) {
                this.playerId = playerId;
                return this;
            }

            @NotNull
            public Builder name(@Nullable String name) {
                this.name = name;
                return this;
            }

            @NotNull
            public Builder reason(@Nullable String reason) {
                this.reason = reason;
                return this;
            }

            @NotNull
            public Builder serverName(@Nullable String serverName) {
                this.serverName = serverName;
                return this;
            }

            @NotNull
            public Builder worldName(@Nullable String worldName) {
                this.worldName = worldName;
                return this;
            }

            @NotNull
            public Builder createdAfter(@Nullable Instant createdAfter) {
                this.createdAfter = createdAfter;
                return this;
            }

            @NotNull
            public Builder createdBefore(@Nullable Instant createdBefore) {
                this.createdBefore = createdBefore;
                return this;
            }

            @NotNull
            public Builder offset(int offset) {
                this.offset = offset;
                return this;
            }

            @NotNull
            public Builder limit(int limit) {
                this.limit = limit;
                return this;
            }

            @NotNull
            public SearchCriteria build() {
                return new SearchCriteria(
                    playerId, name, reason, serverName, worldName,
                    createdAfter, createdBefore, offset, limit
                );
            }
        }
    }

    /**
     * Storage statistics.
     *
     * @param totalSnapshots  total number of stored snapshots
     * @param totalPlayers    number of unique players with snapshots
     * @param totalSizeBytes  total storage size in bytes
     * @param oldestSnapshot  timestamp of oldest snapshot
     * @param newestSnapshot  timestamp of newest snapshot
     * @since 1.0.0
     */
    record StorageStats(
        long totalSnapshots,
        long totalPlayers,
        long totalSizeBytes,
        @Nullable Instant oldestSnapshot,
        @Nullable Instant newestSnapshot
    ) {
        /**
         * Creates empty statistics.
         *
         * @return empty StorageStats
         */
        @NotNull
        public static StorageStats empty() {
            return new StorageStats(0, 0, 0, null, null);
        }
    }
}
