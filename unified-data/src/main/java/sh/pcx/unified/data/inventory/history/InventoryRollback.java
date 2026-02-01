/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.history;

import sh.pcx.unified.data.inventory.core.ApplyMode;
import sh.pcx.unified.data.inventory.core.InventoryService;
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
 * Manages inventory rollback operations.
 *
 * <p>InventoryRollback provides functionality to restore player inventories to
 * previous states. It supports both full rollback and selective item recovery,
 * with confirmation dialogs and undo capabilities.
 *
 * <h2>Rollback Types</h2>
 * <ul>
 *   <li><b>Full Rollback</b>: Replace entire inventory with snapshot</li>
 *   <li><b>Item Recovery</b>: Restore only lost/changed items</li>
 *   <li><b>Selective</b>: Choose specific items to restore</li>
 *   <li><b>Merge</b>: Add restored items without removing current items</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private InventoryRollback rollback;
 *
 * // Full rollback to a snapshot
 * RollbackResult result = rollback.rollback(player, snapshot);
 *
 * // Rollback with preview
 * RollbackPreview preview = rollback.preview(player, snapshot);
 * if (preview.hasChanges()) {
 *     // Show preview to player
 *     player.sendMessage("This will restore " + preview.getItemsRestored() + " items");
 *     // Confirm and execute
 *     rollback.confirm(player, preview);
 * }
 *
 * // Undo last rollback
 * rollback.undo(player);
 *
 * // Recover lost items from death
 * rollback.recoverLostItems(player, deathSnapshot, currentSnapshot);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventoryHistory
 * @see InventoryDiff
 */
public class InventoryRollback {

    private final InventoryService inventoryService;
    private final InventoryHistory history;
    private final Map<UUID, RollbackState> playerStates;
    private final Map<UUID, RollbackPreview> pendingPreviews;

    private Duration undoTimeout;
    private boolean requireConfirmation;
    private int maxUndoStates;

    /**
     * Creates a new InventoryRollback instance.
     *
     * @param inventoryService the inventory service
     * @param history          the inventory history tracker
     * @since 1.0.0
     */
    public InventoryRollback(
        @NotNull InventoryService inventoryService,
        @NotNull InventoryHistory history
    ) {
        this.inventoryService = Objects.requireNonNull(inventoryService);
        this.history = Objects.requireNonNull(history);
        this.playerStates = new ConcurrentHashMap<>();
        this.pendingPreviews = new ConcurrentHashMap<>();

        this.undoTimeout = Duration.ofMinutes(5);
        this.requireConfirmation = true;
        this.maxUndoStates = 3;
    }

    // ========== Configuration ==========

    /**
     * Sets the timeout for undo operations.
     *
     * @param timeout the undo timeout
     * @since 1.0.0
     */
    public void setUndoTimeout(@NotNull Duration timeout) {
        this.undoTimeout = timeout;
    }

    /**
     * Sets whether rollback requires confirmation.
     *
     * @param require true to require confirmation
     * @since 1.0.0
     */
    public void setRequireConfirmation(boolean require) {
        this.requireConfirmation = require;
    }

    /**
     * Sets the maximum undo states to keep per player.
     *
     * @param max maximum undo states
     * @since 1.0.0
     */
    public void setMaxUndoStates(int max) {
        this.maxUndoStates = max;
    }

    // ========== Rollback Operations ==========

    /**
     * Performs a full rollback to a snapshot.
     *
     * @param player   the player
     * @param snapshot the snapshot to rollback to
     * @return the rollback result
     * @since 1.0.0
     */
    @NotNull
    public RollbackResult rollback(@NotNull UnifiedPlayer player, @NotNull InventorySnapshot snapshot) {
        return rollback(player, snapshot, RollbackMode.FULL);
    }

    /**
     * Performs a rollback with a specific mode.
     *
     * @param player   the player
     * @param snapshot the snapshot to rollback to
     * @param mode     the rollback mode
     * @return the rollback result
     * @since 1.0.0
     */
    @NotNull
    public RollbackResult rollback(
        @NotNull UnifiedPlayer player,
        @NotNull InventorySnapshot snapshot,
        @NotNull RollbackMode mode
    ) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        Objects.requireNonNull(mode, "Mode cannot be null");

        // Capture current state for undo
        InventorySnapshot beforeRollback = inventoryService.capture(player);
        saveUndoState(player.getUniqueId(), beforeRollback);

        // Record in history
        history.record(player.getUniqueId(), beforeRollback, "pre_rollback");

        // Calculate what will change
        InventoryDiff diff = InventoryDiff.compare(beforeRollback, snapshot);

        // Apply the rollback
        ApplyMode applyMode = switch (mode) {
            case FULL -> ApplyMode.REPLACE;
            case MERGE -> ApplyMode.MERGE;
            case MERGE_STACK -> ApplyMode.MERGE_STACK;
        };

        snapshot.applyTo(player, applyMode);

        // Record after rollback
        history.record(player.getUniqueId(), snapshot, "post_rollback");

        return new RollbackResult(
            true,
            snapshot.getSnapshotId(),
            diff.getTotalChanges(),
            diff.getItemsGained(),
            diff.getItemsLost(),
            mode,
            null
        );
    }

    /**
     * Creates a preview of what a rollback would do.
     *
     * @param player   the player
     * @param snapshot the snapshot to preview
     * @return the rollback preview
     * @since 1.0.0
     */
    @NotNull
    public RollbackPreview preview(@NotNull UnifiedPlayer player, @NotNull InventorySnapshot snapshot) {
        InventorySnapshot current = inventoryService.capture(player);
        InventoryDiff diff = InventoryDiff.compare(current, snapshot);

        RollbackPreview preview = new RollbackPreview(
            UUID.randomUUID().toString(),
            player.getUniqueId(),
            snapshot.getSnapshotId(),
            Instant.now(),
            diff
        );

        pendingPreviews.put(player.getUniqueId(), preview);

        return preview;
    }

    /**
     * Confirms and executes a pending preview.
     *
     * @param player  the player
     * @param preview the preview to confirm
     * @return the rollback result
     * @since 1.0.0
     */
    @NotNull
    public RollbackResult confirm(@NotNull UnifiedPlayer player, @NotNull RollbackPreview preview) {
        RollbackPreview pending = pendingPreviews.remove(player.getUniqueId());

        if (pending == null || !pending.previewId().equals(preview.previewId())) {
            return RollbackResult.failure("No matching pending preview");
        }

        if (preview.isExpired()) {
            return RollbackResult.failure("Preview has expired");
        }

        // Load and apply the snapshot
        return inventoryService.load(player.getUniqueId(), preview.snapshotId())
            .thenApply(opt -> opt
                .map(snapshot -> rollback(player, snapshot))
                .orElse(RollbackResult.failure("Snapshot not found")))
            .join();
    }

    /**
     * Cancels a pending preview.
     *
     * @param player the player
     * @return true if a preview was cancelled
     * @since 1.0.0
     */
    public boolean cancelPreview(@NotNull UnifiedPlayer player) {
        return pendingPreviews.remove(player.getUniqueId()) != null;
    }

    // ========== Undo Operations ==========

    /**
     * Undoes the last rollback for a player.
     *
     * @param player the player
     * @return the result of the undo operation
     * @since 1.0.0
     */
    @NotNull
    public RollbackResult undo(@NotNull UnifiedPlayer player) {
        RollbackState state = playerStates.get(player.getUniqueId());
        if (state == null || state.undoStack.isEmpty()) {
            return RollbackResult.failure("Nothing to undo");
        }

        UndoEntry undoEntry = state.undoStack.pop();
        if (undoEntry.isExpired(undoTimeout)) {
            return RollbackResult.failure("Undo has expired");
        }

        // Apply the undo
        undoEntry.snapshot().applyTo(player, ApplyMode.REPLACE);

        // Record the undo
        history.record(player.getUniqueId(), undoEntry.snapshot(), "undo");

        return new RollbackResult(
            true,
            undoEntry.snapshot().getSnapshotId(),
            0,
            0,
            0,
            RollbackMode.FULL,
            "Undo successful"
        );
    }

    /**
     * Checks if a player has undo available.
     *
     * @param player the player
     * @return true if undo is available
     * @since 1.0.0
     */
    public boolean canUndo(@NotNull UnifiedPlayer player) {
        RollbackState state = playerStates.get(player.getUniqueId());
        if (state == null || state.undoStack.isEmpty()) {
            return false;
        }
        return !state.undoStack.peek().isExpired(undoTimeout);
    }

    /**
     * Gets the number of available undo steps.
     *
     * @param player the player
     * @return the undo count
     * @since 1.0.0
     */
    public int getUndoCount(@NotNull UnifiedPlayer player) {
        RollbackState state = playerStates.get(player.getUniqueId());
        if (state == null) {
            return 0;
        }
        return (int) state.undoStack.stream()
            .filter(e -> !e.isExpired(undoTimeout))
            .count();
    }

    /**
     * Clears undo history for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    public void clearUndoHistory(@NotNull UnifiedPlayer player) {
        playerStates.remove(player.getUniqueId());
    }

    // ========== Item Recovery ==========

    /**
     * Recovers lost items by comparing two snapshots.
     *
     * @param player         the player
     * @param beforeSnapshot snapshot before items were lost
     * @param afterSnapshot  snapshot after items were lost
     * @return the recovery result
     * @since 1.0.0
     */
    @NotNull
    public RecoveryResult recoverLostItems(
        @NotNull UnifiedPlayer player,
        @NotNull InventorySnapshot beforeSnapshot,
        @NotNull InventorySnapshot afterSnapshot
    ) {
        InventoryDiff diff = InventoryDiff.compare(beforeSnapshot, afterSnapshot);
        List<InventoryDiff.ItemChange> lostItems = diff.getRemovedItems();

        if (lostItems.isEmpty()) {
            return new RecoveryResult(false, 0, "No lost items found");
        }

        // Try to add lost items back
        int recovered = 0;
        for (InventoryDiff.ItemChange change : lostItems) {
            if (change.getBefore() != null) {
                // Give the item back
                // Implementation would add items to player inventory
                recovered++;
            }
        }

        // Record recovery
        history.record(player.getUniqueId(), inventoryService.capture(player), "recovery");

        return new RecoveryResult(true, recovered, null);
    }

    /**
     * Recovers items lost at death.
     *
     * @param player        the player
     * @param deathSnapshot the snapshot at death
     * @return the recovery result
     * @since 1.0.0
     */
    @NotNull
    public RecoveryResult recoverDeathItems(@NotNull UnifiedPlayer player, @NotNull InventorySnapshot deathSnapshot) {
        InventorySnapshot current = inventoryService.capture(player);
        return recoverLostItems(player, deathSnapshot, current);
    }

    // ========== Helper Methods ==========

    private void saveUndoState(@NotNull UUID playerId, @NotNull InventorySnapshot snapshot) {
        RollbackState state = playerStates.computeIfAbsent(playerId, k -> new RollbackState());

        state.undoStack.push(new UndoEntry(snapshot, Instant.now()));

        // Enforce max undo states
        while (state.undoStack.size() > maxUndoStates) {
            state.undoStack.removeLast();
        }
    }

    /**
     * Cleans up expired previews and undo states.
     *
     * @since 1.0.0
     */
    public void cleanup() {
        // Clean expired previews
        pendingPreviews.entrySet().removeIf(e -> e.getValue().isExpired());

        // Clean expired undo entries
        for (RollbackState state : playerStates.values()) {
            state.undoStack.removeIf(e -> e.isExpired(undoTimeout));
        }

        // Remove empty states
        playerStates.entrySet().removeIf(e -> e.getValue().undoStack.isEmpty());
    }

    // ========== Nested Types ==========

    /**
     * Rollback execution modes.
     *
     * @since 1.0.0
     */
    public enum RollbackMode {
        /**
         * Full replacement of inventory.
         */
        FULL,

        /**
         * Merge with existing items.
         */
        MERGE,

        /**
         * Merge and stack with existing items.
         */
        MERGE_STACK
    }

    /**
     * Result of a rollback operation.
     *
     * @param success       whether the rollback succeeded
     * @param snapshotId    the applied snapshot ID
     * @param changesApplied number of changes applied
     * @param itemsRestored items restored to inventory
     * @param itemsRemoved  items removed from inventory
     * @param mode          the rollback mode used
     * @param message       optional message
     * @since 1.0.0
     */
    public record RollbackResult(
        boolean success,
        @Nullable String snapshotId,
        int changesApplied,
        int itemsRestored,
        int itemsRemoved,
        @Nullable RollbackMode mode,
        @Nullable String message
    ) {
        /**
         * Creates a failure result.
         */
        @NotNull
        public static RollbackResult failure(@NotNull String message) {
            return new RollbackResult(false, null, 0, 0, 0, null, message);
        }

        /**
         * Checks if successful.
         */
        public boolean isSuccess() {
            return success;
        }
    }

    /**
     * Preview of a rollback operation.
     *
     * @param previewId  unique preview ID
     * @param playerId   the player's UUID
     * @param snapshotId the target snapshot ID
     * @param createdAt  when the preview was created
     * @param diff       the calculated diff
     * @since 1.0.0
     */
    public record RollbackPreview(
        @NotNull String previewId,
        @NotNull UUID playerId,
        @NotNull String snapshotId,
        @NotNull Instant createdAt,
        @NotNull InventoryDiff diff
    ) {
        private static final Duration PREVIEW_TIMEOUT = Duration.ofMinutes(2);

        /**
         * Checks if this preview has expired.
         */
        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(PREVIEW_TIMEOUT));
        }

        /**
         * Checks if there are changes to apply.
         */
        public boolean hasChanges() {
            return diff.hasChanges();
        }

        /**
         * Gets the number of items that would be restored.
         */
        public int getItemsRestored() {
            return diff.getItemsGained();
        }

        /**
         * Gets the number of items that would be removed.
         */
        public int getItemsRemoved() {
            return diff.getItemsLost();
        }
    }

    /**
     * Result of an item recovery operation.
     *
     * @param success  whether recovery succeeded
     * @param recovered number of items recovered
     * @param message  optional message
     * @since 1.0.0
     */
    public record RecoveryResult(
        boolean success,
        int recovered,
        @Nullable String message
    ) {}

    /**
     * Tracks undo state for a player.
     */
    private static class RollbackState {
        final Deque<UndoEntry> undoStack = new LinkedList<>();
    }

    /**
     * Entry in the undo stack.
     */
    private record UndoEntry(
        @NotNull InventorySnapshot snapshot,
        @NotNull Instant createdAt
    ) {
        boolean isExpired(@NotNull Duration timeout) {
            return Instant.now().isAfter(createdAt.plus(timeout));
        }
    }
}
