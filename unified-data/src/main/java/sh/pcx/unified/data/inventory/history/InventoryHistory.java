/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.history;

import sh.pcx.unified.data.inventory.core.InventorySnapshot;
import sh.pcx.unified.data.inventory.snapshot.SnapshotMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Tracks and manages inventory change history for players.
 *
 * <p>InventoryHistory provides comprehensive tracking of inventory changes over time.
 * It supports multiple tracking modes, configurable retention policies, and efficient
 * querying of historical data.
 *
 * <h2>Tracking Modes</h2>
 * <ul>
 *   <li><b>EVENT</b>: Track on specific events (death, world change, etc.)</li>
 *   <li><b>PERIODIC</b>: Track at regular intervals</li>
 *   <li><b>CHANGE</b>: Track on any inventory change</li>
 *   <li><b>MANUAL</b>: Only track when explicitly requested</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private InventoryHistory history;
 *
 * // Configure tracking
 * history.setTrackingMode(TrackingMode.EVENT);
 * history.setRetentionDuration(Duration.ofDays(7));
 * history.setMaxEntriesPerPlayer(50);
 *
 * // Record a snapshot
 * history.record(player, "death");
 *
 * // Get history for a player
 * List<HistoryEntry> entries = history.getHistory(playerId, 10);
 *
 * // Get history in time range
 * List<HistoryEntry> rangeEntries = history.getHistory(
 *     playerId,
 *     Instant.now().minus(Duration.ofHours(1)),
 *     Instant.now()
 * );
 *
 * // Find specific entries
 * Optional<HistoryEntry> deathEntry = history.findByReason(playerId, "death");
 *
 * // Compare history entries
 * InventoryDiff diff = history.diff(entries.get(0), entries.get(1));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventoryDiff
 * @see InventoryRollback
 */
public class InventoryHistory {

    private final Map<UUID, Deque<HistoryEntry>> playerHistory;
    private final HistoryStorage storage;

    private TrackingMode trackingMode;
    private Duration retentionDuration;
    private int maxEntriesPerPlayer;
    private Duration periodicInterval;
    private Set<String> trackedEvents;

    /**
     * Creates a new InventoryHistory with default settings.
     *
     * @since 1.0.0
     */
    public InventoryHistory() {
        this(null);
    }

    /**
     * Creates a new InventoryHistory with persistent storage.
     *
     * @param storage the storage backend, or null for in-memory only
     * @since 1.0.0
     */
    public InventoryHistory(@Nullable HistoryStorage storage) {
        this.playerHistory = new ConcurrentHashMap<>();
        this.storage = storage;

        this.trackingMode = TrackingMode.EVENT;
        this.retentionDuration = Duration.ofDays(7);
        this.maxEntriesPerPlayer = 100;
        this.periodicInterval = Duration.ofMinutes(30);
        this.trackedEvents = new HashSet<>(Set.of("death", "world_change", "manual"));
    }

    // ========== Configuration ==========

    /**
     * Sets the tracking mode.
     *
     * @param mode the tracking mode
     * @since 1.0.0
     */
    public void setTrackingMode(@NotNull TrackingMode mode) {
        this.trackingMode = mode;
    }

    /**
     * Gets the tracking mode.
     *
     * @return the current tracking mode
     * @since 1.0.0
     */
    @NotNull
    public TrackingMode getTrackingMode() {
        return trackingMode;
    }

    /**
     * Sets the retention duration.
     *
     * @param duration how long to keep history
     * @since 1.0.0
     */
    public void setRetentionDuration(@NotNull Duration duration) {
        this.retentionDuration = duration;
    }

    /**
     * Sets the maximum entries per player.
     *
     * @param max maximum entries
     * @since 1.0.0
     */
    public void setMaxEntriesPerPlayer(int max) {
        this.maxEntriesPerPlayer = max;
    }

    /**
     * Sets the periodic tracking interval.
     *
     * @param interval the interval between automatic captures
     * @since 1.0.0
     */
    public void setPeriodicInterval(@NotNull Duration interval) {
        this.periodicInterval = interval;
    }

    /**
     * Sets which events to track.
     *
     * @param events the set of event names to track
     * @since 1.0.0
     */
    public void setTrackedEvents(@NotNull Set<String> events) {
        this.trackedEvents = new HashSet<>(events);
    }

    /**
     * Adds an event to track.
     *
     * @param event the event name
     * @since 1.0.0
     */
    public void addTrackedEvent(@NotNull String event) {
        this.trackedEvents.add(event);
    }

    /**
     * Removes an event from tracking.
     *
     * @param event the event name
     * @since 1.0.0
     */
    public void removeTrackedEvent(@NotNull String event) {
        this.trackedEvents.remove(event);
    }

    /**
     * Checks if an event should be tracked.
     *
     * @param event the event name
     * @return true if the event is tracked
     * @since 1.0.0
     */
    public boolean isEventTracked(@NotNull String event) {
        return trackedEvents.contains(event);
    }

    // ========== Recording ==========

    /**
     * Records a history entry for a player.
     *
     * @param playerId the player's UUID
     * @param snapshot the inventory snapshot
     * @param reason   the reason for recording
     * @return the created history entry
     * @since 1.0.0
     */
    @NotNull
    public HistoryEntry record(
        @NotNull UUID playerId,
        @NotNull InventorySnapshot snapshot,
        @Nullable String reason
    ) {
        HistoryEntry entry = new HistoryEntry(
            UUID.randomUUID().toString(),
            playerId,
            snapshot.getSnapshotId(),
            Instant.now(),
            reason,
            snapshot.getTotalItemCount(),
            snapshot
        );

        // Add to in-memory history
        Deque<HistoryEntry> history = playerHistory.computeIfAbsent(
            playerId, k -> new LinkedList<>()
        );

        synchronized (history) {
            history.addFirst(entry);

            // Enforce max entries
            while (history.size() > maxEntriesPerPlayer) {
                history.removeLast();
            }
        }

        // Persist if storage is available
        if (storage != null) {
            storage.save(entry);
        }

        return entry;
    }

    /**
     * Records a history entry if the event should be tracked.
     *
     * @param playerId the player's UUID
     * @param snapshot the inventory snapshot
     * @param reason   the reason/event name
     * @return the entry if recorded, or empty if not tracked
     * @since 1.0.0
     */
    @NotNull
    public Optional<HistoryEntry> recordIfTracked(
        @NotNull UUID playerId,
        @NotNull InventorySnapshot snapshot,
        @NotNull String reason
    ) {
        if (trackingMode == TrackingMode.MANUAL) {
            return Optional.empty();
        }

        if (trackingMode == TrackingMode.EVENT && !isEventTracked(reason)) {
            return Optional.empty();
        }

        return Optional.of(record(playerId, snapshot, reason));
    }

    // ========== Retrieval ==========

    /**
     * Gets history entries for a player.
     *
     * @param playerId the player's UUID
     * @param limit    maximum entries to return
     * @return list of history entries, newest first
     * @since 1.0.0
     */
    @NotNull
    public List<HistoryEntry> getHistory(@NotNull UUID playerId, int limit) {
        Deque<HistoryEntry> history = playerHistory.get(playerId);
        if (history == null) {
            if (storage != null) {
                return storage.loadHistory(playerId, limit).join();
            }
            return List.of();
        }

        synchronized (history) {
            return history.stream()
                .limit(limit)
                .toList();
        }
    }

    /**
     * Gets history entries within a time range.
     *
     * @param playerId the player's UUID
     * @param from     start time (inclusive)
     * @param to       end time (exclusive)
     * @return list of entries in the time range
     * @since 1.0.0
     */
    @NotNull
    public List<HistoryEntry> getHistory(
        @NotNull UUID playerId,
        @NotNull Instant from,
        @NotNull Instant to
    ) {
        Deque<HistoryEntry> history = playerHistory.get(playerId);
        if (history == null) {
            if (storage != null) {
                return storage.loadHistoryByRange(playerId, from, to).join();
            }
            return List.of();
        }

        synchronized (history) {
            return history.stream()
                .filter(e -> !e.timestamp().isBefore(from) && e.timestamp().isBefore(to))
                .toList();
        }
    }

    /**
     * Gets all history entries for a player.
     *
     * @param playerId the player's UUID
     * @return list of all history entries
     * @since 1.0.0
     */
    @NotNull
    public List<HistoryEntry> getAllHistory(@NotNull UUID playerId) {
        return getHistory(playerId, maxEntriesPerPlayer);
    }

    /**
     * Finds an entry by ID.
     *
     * @param playerId the player's UUID
     * @param entryId  the entry ID
     * @return the entry if found
     * @since 1.0.0
     */
    @NotNull
    public Optional<HistoryEntry> findById(@NotNull UUID playerId, @NotNull String entryId) {
        return getHistory(playerId, maxEntriesPerPlayer).stream()
            .filter(e -> e.entryId().equals(entryId))
            .findFirst();
    }

    /**
     * Finds entries by reason.
     *
     * @param playerId the player's UUID
     * @param reason   the reason to search for
     * @return list of matching entries
     * @since 1.0.0
     */
    @NotNull
    public List<HistoryEntry> findByReason(@NotNull UUID playerId, @NotNull String reason) {
        return getHistory(playerId, maxEntriesPerPlayer).stream()
            .filter(e -> reason.equals(e.reason()))
            .toList();
    }

    /**
     * Finds entries matching a predicate.
     *
     * @param playerId  the player's UUID
     * @param predicate the filter predicate
     * @return list of matching entries
     * @since 1.0.0
     */
    @NotNull
    public List<HistoryEntry> find(
        @NotNull UUID playerId,
        @NotNull Predicate<HistoryEntry> predicate
    ) {
        return getHistory(playerId, maxEntriesPerPlayer).stream()
            .filter(predicate)
            .toList();
    }

    /**
     * Gets the most recent entry for a player.
     *
     * @param playerId the player's UUID
     * @return the most recent entry, or empty if none
     * @since 1.0.0
     */
    @NotNull
    public Optional<HistoryEntry> getLatest(@NotNull UUID playerId) {
        Deque<HistoryEntry> history = playerHistory.get(playerId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(history.getFirst());
    }

    /**
     * Gets the oldest entry for a player.
     *
     * @param playerId the player's UUID
     * @return the oldest entry, or empty if none
     * @since 1.0.0
     */
    @NotNull
    public Optional<HistoryEntry> getOldest(@NotNull UUID playerId) {
        Deque<HistoryEntry> history = playerHistory.get(playerId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(history.getLast());
    }

    // ========== Comparison ==========

    /**
     * Compares two history entries.
     *
     * @param before the before entry
     * @param after  the after entry
     * @return the diff between entries
     * @since 1.0.0
     */
    @NotNull
    public InventoryDiff diff(@NotNull HistoryEntry before, @NotNull HistoryEntry after) {
        if (before.snapshot() == null || after.snapshot() == null) {
            throw new IllegalArgumentException("Entries must have snapshots for comparison");
        }
        return InventoryDiff.compare(before.snapshot(), after.snapshot());
    }

    // ========== Cleanup ==========

    /**
     * Clears history for a player.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    public void clearHistory(@NotNull UUID playerId) {
        playerHistory.remove(playerId);
        if (storage != null) {
            storage.deleteByPlayer(playerId);
        }
    }

    /**
     * Clears all history.
     *
     * @since 1.0.0
     */
    public void clearAllHistory() {
        playerHistory.clear();
        if (storage != null) {
            storage.deleteAll();
        }
    }

    /**
     * Removes old entries based on retention duration.
     *
     * @return the number of entries removed
     * @since 1.0.0
     */
    public int cleanup() {
        Instant cutoff = Instant.now().minus(retentionDuration);
        int removed = 0;

        for (Deque<HistoryEntry> history : playerHistory.values()) {
            synchronized (history) {
                while (!history.isEmpty() && history.getLast().timestamp().isBefore(cutoff)) {
                    history.removeLast();
                    removed++;
                }
            }
        }

        // Remove empty player entries
        playerHistory.entrySet().removeIf(e -> e.getValue().isEmpty());

        if (storage != null) {
            storage.deleteOlderThan(cutoff);
        }

        return removed;
    }

    // ========== Statistics ==========

    /**
     * Gets the entry count for a player.
     *
     * @param playerId the player's UUID
     * @return the entry count
     * @since 1.0.0
     */
    public int getEntryCount(@NotNull UUID playerId) {
        Deque<HistoryEntry> history = playerHistory.get(playerId);
        return history != null ? history.size() : 0;
    }

    /**
     * Gets the total number of tracked players.
     *
     * @return the player count
     * @since 1.0.0
     */
    public int getTrackedPlayerCount() {
        return playerHistory.size();
    }

    /**
     * Gets the total entry count across all players.
     *
     * @return the total entry count
     * @since 1.0.0
     */
    public int getTotalEntryCount() {
        return playerHistory.values().stream()
            .mapToInt(Deque::size)
            .sum();
    }

    // ========== Nested Types ==========

    /**
     * Tracking modes for inventory history.
     *
     * @since 1.0.0
     */
    public enum TrackingMode {
        /**
         * Track on specific events.
         */
        EVENT,

        /**
         * Track at regular intervals.
         */
        PERIODIC,

        /**
         * Track on any inventory change.
         */
        CHANGE,

        /**
         * Only track when explicitly requested.
         */
        MANUAL
    }

    /**
     * A single history entry.
     *
     * @param entryId    the unique entry ID
     * @param playerId   the player's UUID
     * @param snapshotId the associated snapshot ID
     * @param timestamp  when the entry was created
     * @param reason     the reason for the entry
     * @param itemCount  the total item count at the time
     * @param snapshot   the snapshot (may be null if not loaded)
     * @since 1.0.0
     */
    public record HistoryEntry(
        @NotNull String entryId,
        @NotNull UUID playerId,
        @NotNull String snapshotId,
        @NotNull Instant timestamp,
        @Nullable String reason,
        int itemCount,
        @Nullable InventorySnapshot snapshot
    ) {
        /**
         * Returns the age of this entry.
         */
        @NotNull
        public Duration getAge() {
            return Duration.between(timestamp, Instant.now());
        }
    }

    /**
     * Interface for persistent history storage.
     *
     * @since 1.0.0
     */
    public interface HistoryStorage {
        /**
         * Saves an entry.
         */
        CompletableFuture<Void> save(@NotNull HistoryEntry entry);

        /**
         * Loads history for a player.
         */
        CompletableFuture<List<HistoryEntry>> loadHistory(@NotNull UUID playerId, int limit);

        /**
         * Loads history in a time range.
         */
        CompletableFuture<List<HistoryEntry>> loadHistoryByRange(
            @NotNull UUID playerId,
            @NotNull Instant from,
            @NotNull Instant to
        );

        /**
         * Deletes all entries for a player.
         */
        CompletableFuture<Void> deleteByPlayer(@NotNull UUID playerId);

        /**
         * Deletes entries older than a timestamp.
         */
        CompletableFuture<Integer> deleteOlderThan(@NotNull Instant timestamp);

        /**
         * Deletes all entries.
         */
        CompletableFuture<Void> deleteAll();
    }
}
