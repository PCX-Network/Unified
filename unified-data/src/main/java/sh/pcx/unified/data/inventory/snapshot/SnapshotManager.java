/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.snapshot;

import sh.pcx.unified.data.inventory.core.CaptureOptions;
import sh.pcx.unified.data.inventory.core.InventorySnapshot;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages inventory snapshots including creation, caching, and lifecycle.
 *
 * <p>SnapshotManager provides a high-level API for managing inventory snapshots,
 * including automatic history tracking, caching, and cleanup. It coordinates
 * between the capture system and storage backends.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Automatic History</b>: Optionally tracks all inventory changes</li>
 *   <li><b>Caching</b>: In-memory cache for frequently accessed snapshots</li>
 *   <li><b>Versioning</b>: Automatic version numbering for each player</li>
 *   <li><b>Cleanup</b>: Automatic purging of old snapshots</li>
 *   <li><b>Events</b>: Fires events on snapshot operations</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private SnapshotManager snapshotManager;
 *
 * // Create a snapshot with automatic storage
 * SnapshotMetadata metadata = snapshotManager.createSnapshot(player, "death_backup");
 *
 * // Create with reason
 * SnapshotMetadata metadata = snapshotManager.createSnapshot(
 *     player,
 *     "auto_backup",
 *     SnapshotMetadata.REASON_AUTO
 * );
 *
 * // Get history
 * List<SnapshotMetadata> history = snapshotManager.getHistory(player.getUniqueId(), 10);
 *
 * // Restore from history
 * snapshotManager.restore(player, history.get(2).snapshotId());
 *
 * // Configure automatic cleanup
 * snapshotManager.setRetentionPolicy(
 *     Duration.ofDays(30),
 *     100  // max per player
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SnapshotStorage
 * @see SnapshotMetadata
 */
public class SnapshotManager {

    private final SnapshotStorage storage;
    private final Map<UUID, Integer> versionCounters;
    private final Map<String, InventorySnapshot> cache;
    private final int cacheMaxSize;

    private Duration retentionDuration;
    private int maxSnapshotsPerPlayer;
    private boolean historyEnabled;
    private String serverName;

    /**
     * Creates a new SnapshotManager with the specified storage backend.
     *
     * @param storage the storage backend to use
     * @since 1.0.0
     */
    public SnapshotManager(@NotNull SnapshotStorage storage) {
        this(storage, 1000);
    }

    /**
     * Creates a new SnapshotManager with custom cache size.
     *
     * @param storage      the storage backend to use
     * @param cacheMaxSize maximum number of cached snapshots
     * @since 1.0.0
     */
    public SnapshotManager(@NotNull SnapshotStorage storage, int cacheMaxSize) {
        this.storage = Objects.requireNonNull(storage, "Storage cannot be null");
        this.cacheMaxSize = cacheMaxSize;
        this.versionCounters = new ConcurrentHashMap<>();
        this.cache = new LinkedHashMap<>(cacheMaxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, InventorySnapshot> eldest) {
                return size() > SnapshotManager.this.cacheMaxSize;
            }
        };
        this.retentionDuration = Duration.ofDays(30);
        this.maxSnapshotsPerPlayer = 100;
        this.historyEnabled = true;
    }

    // ========== Configuration ==========

    /**
     * Sets the retention policy for snapshots.
     *
     * @param duration           delete snapshots older than this
     * @param maxPerPlayer       maximum snapshots per player
     * @since 1.0.0
     */
    public void setRetentionPolicy(@NotNull Duration duration, int maxPerPlayer) {
        this.retentionDuration = Objects.requireNonNull(duration);
        this.maxSnapshotsPerPlayer = maxPerPlayer;
    }

    /**
     * Enables or disables automatic history tracking.
     *
     * @param enabled true to enable history
     * @since 1.0.0
     */
    public void setHistoryEnabled(boolean enabled) {
        this.historyEnabled = enabled;
    }

    /**
     * Returns whether history tracking is enabled.
     *
     * @return true if history is enabled
     * @since 1.0.0
     */
    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    /**
     * Sets the server name for metadata.
     *
     * @param serverName the server name
     * @since 1.0.0
     */
    public void setServerName(@Nullable String serverName) {
        this.serverName = serverName;
    }

    // ========== Snapshot Creation ==========

    /**
     * Creates a snapshot of a player's inventory and stores it.
     *
     * @param player the player
     * @param name   the snapshot name
     * @return future containing the snapshot metadata
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<SnapshotMetadata> createSnapshot(
        @NotNull UnifiedPlayer player,
        @NotNull String name
    ) {
        return createSnapshot(player, name, null, CaptureOptions.defaults());
    }

    /**
     * Creates a snapshot with a specific reason.
     *
     * @param player the player
     * @param name   the snapshot name
     * @param reason the creation reason
     * @return future containing the snapshot metadata
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<SnapshotMetadata> createSnapshot(
        @NotNull UnifiedPlayer player,
        @NotNull String name,
        @Nullable String reason
    ) {
        return createSnapshot(player, name, reason, CaptureOptions.defaults());
    }

    /**
     * Creates a snapshot with full options.
     *
     * @param player        the player
     * @param name          the snapshot name
     * @param reason        the creation reason
     * @param captureOptions capture options
     * @return future containing the snapshot metadata
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<SnapshotMetadata> createSnapshot(
        @NotNull UnifiedPlayer player,
        @Nullable String name,
        @Nullable String reason,
        @NotNull CaptureOptions captureOptions
    ) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(captureOptions, "CaptureOptions cannot be null");

        UUID playerId = player.getUniqueId();
        String snapshotId = generateSnapshotId();
        int version = getNextVersion(playerId);

        // Capture the snapshot
        InventorySnapshot snapshot = sh.pcx.unified.data.inventory.core.InventoryService
            .getInstance().capture(player, captureOptions);

        // Build metadata
        SnapshotMetadata metadata = SnapshotMetadata.builder(snapshotId, playerId)
            .name(name)
            .reason(reason)
            .version(version)
            .itemCount(snapshot.getTotalItemCount())
            .serverName(serverName)
            .worldName(player.getWorld().getName())
            .createdAt(Instant.now())
            .build();

        // Store the snapshot
        return storage.save(snapshotId, snapshot, metadata)
            .thenApply(v -> {
                // Cache it
                synchronized (cache) {
                    cache.put(snapshotId, snapshot);
                }
                return metadata;
            });
    }

    /**
     * Creates a snapshot for automatic history tracking.
     *
     * <p>This method is called automatically when history tracking is enabled.
     *
     * @param player the player
     * @return future containing the snapshot metadata, or empty if history is disabled
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<SnapshotMetadata>> createHistorySnapshot(
        @NotNull UnifiedPlayer player
    ) {
        if (!historyEnabled) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return createSnapshot(player, null, SnapshotMetadata.REASON_AUTO, CaptureOptions.defaults())
            .thenApply(Optional::of);
    }

    // ========== Snapshot Retrieval ==========

    /**
     * Gets a snapshot by ID.
     *
     * @param snapshotId the snapshot ID
     * @return future containing the snapshot if found
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<InventorySnapshot>> getSnapshot(@NotNull String snapshotId) {
        // Check cache first
        synchronized (cache) {
            InventorySnapshot cached = cache.get(snapshotId);
            if (cached != null) {
                return CompletableFuture.completedFuture(Optional.of(cached));
            }
        }

        // Load from storage
        return storage.load(snapshotId).thenApply(opt -> {
            opt.ifPresent(snapshot -> {
                synchronized (cache) {
                    cache.put(snapshotId, snapshot);
                }
            });
            return opt;
        });
    }

    /**
     * Gets snapshot metadata by ID.
     *
     * @param snapshotId the snapshot ID
     * @return future containing the metadata if found
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<SnapshotMetadata>> getMetadata(@NotNull String snapshotId) {
        return storage.loadMetadata(snapshotId);
    }

    /**
     * Gets a named snapshot for a player.
     *
     * @param playerId the player's UUID
     * @param name     the snapshot name
     * @return future containing the snapshot if found
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<InventorySnapshot>> getByName(
        @NotNull UUID playerId,
        @NotNull String name
    ) {
        return storage.loadByName(playerId, name);
    }

    /**
     * Gets the most recent snapshot for a player.
     *
     * @param playerId the player's UUID
     * @return future containing the snapshot if found
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<InventorySnapshot>> getLatest(@NotNull UUID playerId) {
        return storage.loadLatest(playerId);
    }

    // ========== History ==========

    /**
     * Gets snapshot history for a player.
     *
     * @param playerId the player's UUID
     * @param limit    maximum number of snapshots
     * @return future containing list of metadata, newest first
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<SnapshotMetadata>> getHistory(
        @NotNull UUID playerId,
        int limit
    ) {
        return storage.listByPlayer(playerId, 0, limit);
    }

    /**
     * Gets all snapshot metadata for a player.
     *
     * @param playerId the player's UUID
     * @return future containing list of metadata
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<SnapshotMetadata>> listSnapshots(@NotNull UUID playerId) {
        return storage.listByPlayer(playerId);
    }

    // ========== Restoration ==========

    /**
     * Restores a player's inventory from a snapshot.
     *
     * @param player     the player
     * @param snapshotId the snapshot ID to restore
     * @return future that completes when restored, containing true if successful
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> restore(
        @NotNull UnifiedPlayer player,
        @NotNull String snapshotId
    ) {
        return getSnapshot(snapshotId).thenApply(opt -> {
            if (opt.isEmpty()) {
                return false;
            }

            opt.get().applyTo(player);
            return true;
        });
    }

    /**
     * Restores from the latest snapshot for a player.
     *
     * @param player the player
     * @return future that completes when restored, containing true if successful
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> restoreLatest(@NotNull UnifiedPlayer player) {
        return getLatest(player.getUniqueId()).thenApply(opt -> {
            if (opt.isEmpty()) {
                return false;
            }

            opt.get().applyTo(player);
            return true;
        });
    }

    // ========== Deletion ==========

    /**
     * Deletes a snapshot.
     *
     * @param snapshotId the snapshot ID
     * @return future that completes with true if deleted
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> deleteSnapshot(@NotNull String snapshotId) {
        synchronized (cache) {
            cache.remove(snapshotId);
        }
        return storage.delete(snapshotId);
    }

    /**
     * Deletes all snapshots for a player.
     *
     * @param playerId the player's UUID
     * @return future that completes with count of deleted snapshots
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Integer> deleteAllForPlayer(@NotNull UUID playerId) {
        return storage.deleteByPlayer(playerId);
    }

    // ========== Cleanup ==========

    /**
     * Runs cleanup based on retention policy.
     *
     * @return future that completes with count of deleted snapshots
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Integer> runCleanup() {
        Instant cutoff = Instant.now().minus(retentionDuration);
        return storage.deleteOlderThan(cutoff);
    }

    /**
     * Enforces per-player snapshot limits.
     *
     * @param playerId the player's UUID
     * @return future that completes with count of deleted snapshots
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Integer> enforcePlayerLimit(@NotNull UUID playerId) {
        return storage.listByPlayer(playerId).thenCompose(snapshots -> {
            if (snapshots.size() <= maxSnapshotsPerPlayer) {
                return CompletableFuture.completedFuture(0);
            }

            // Delete oldest snapshots beyond the limit
            List<SnapshotMetadata> toDelete = snapshots.subList(
                maxSnapshotsPerPlayer,
                snapshots.size()
            );

            List<CompletableFuture<Boolean>> deleteFutures = toDelete.stream()
                .map(meta -> storage.delete(meta.snapshotId()))
                .toList();

            return CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> toDelete.size());
        });
    }

    /**
     * Clears the snapshot cache.
     *
     * @since 1.0.0
     */
    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * Gets storage statistics.
     *
     * @return future containing storage stats
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<SnapshotStorage.StorageStats> getStats() {
        return storage.getStats();
    }

    // ========== Internal ==========

    /**
     * Generates a unique snapshot ID.
     *
     * @return a new snapshot ID
     */
    private String generateSnapshotId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets the next version number for a player.
     *
     * @param playerId the player's UUID
     * @return the next version number
     */
    private int getNextVersion(@NotNull UUID playerId) {
        return versionCounters.compute(playerId, (id, version) ->
            version == null ? 1 : version + 1
        );
    }

    /**
     * Closes the snapshot manager and releases resources.
     *
     * @return a future that completes when closed
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> close() {
        clearCache();
        versionCounters.clear();
        return storage.close();
    }
}
