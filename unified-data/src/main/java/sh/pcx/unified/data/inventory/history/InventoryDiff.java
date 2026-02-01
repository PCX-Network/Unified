/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.history;

import sh.pcx.unified.data.inventory.core.InventorySlot;
import sh.pcx.unified.data.inventory.core.InventorySnapshot;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the difference between two inventory snapshots.
 *
 * <p>InventoryDiff tracks all changes between a "before" and "after" snapshot,
 * including added, removed, and modified items. It can be used for auditing,
 * rollback calculations, and change visualization.
 *
 * <h2>Change Types</h2>
 * <ul>
 *   <li><b>ADDED</b>: Item added to an empty slot</li>
 *   <li><b>REMOVED</b>: Item removed from a slot (now empty)</li>
 *   <li><b>MODIFIED</b>: Item in slot changed (type, amount, or metadata)</li>
 *   <li><b>MOVED</b>: Item moved to a different slot</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Compare two snapshots
 * InventoryDiff diff = InventoryDiff.compare(beforeSnapshot, afterSnapshot);
 *
 * // Get all changes
 * List<ItemChange> changes = diff.getChanges();
 *
 * // Iterate through changes
 * for (ItemChange change : changes) {
 *     int slot = change.getSlot();
 *     ChangeType type = change.getType();
 *     ItemStack before = change.getBefore();
 *     ItemStack after = change.getAfter();
 *
 *     switch (type) {
 *         case ADDED -> log("Added " + after + " to slot " + slot);
 *         case REMOVED -> log("Removed " + before + " from slot " + slot);
 *         case MODIFIED -> log("Slot " + slot + " changed from " + before + " to " + after);
 *     }
 * }
 *
 * // Get specific change types
 * List<ItemChange> added = diff.getAddedItems();
 * List<ItemChange> removed = diff.getRemovedItems();
 *
 * // Summary statistics
 * int totalChanges = diff.getTotalChanges();
 * int itemsGained = diff.getItemsGained();
 * int itemsLost = diff.getItemsLost();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventorySnapshot
 * @see ItemChange
 */
public final class InventoryDiff {

    private final String beforeSnapshotId;
    private final String afterSnapshotId;
    private final UUID playerId;
    private final Instant beforeTime;
    private final Instant afterTime;
    private final List<ItemChange> changes;

    // Cached statistics
    private transient Integer itemsGained;
    private transient Integer itemsLost;

    private InventoryDiff(
        @NotNull String beforeSnapshotId,
        @NotNull String afterSnapshotId,
        @NotNull UUID playerId,
        @NotNull Instant beforeTime,
        @NotNull Instant afterTime,
        @NotNull List<ItemChange> changes
    ) {
        this.beforeSnapshotId = beforeSnapshotId;
        this.afterSnapshotId = afterSnapshotId;
        this.playerId = playerId;
        this.beforeTime = beforeTime;
        this.afterTime = afterTime;
        this.changes = List.copyOf(changes);
    }

    /**
     * Compares two snapshots and returns the differences.
     *
     * @param before the before snapshot
     * @param after  the after snapshot
     * @return the diff between the snapshots
     * @throws IllegalArgumentException if snapshots are for different players
     * @since 1.0.0
     */
    @NotNull
    public static InventoryDiff compare(@NotNull InventorySnapshot before, @NotNull InventorySnapshot after) {
        Objects.requireNonNull(before, "Before snapshot cannot be null");
        Objects.requireNonNull(after, "After snapshot cannot be null");

        if (!before.getPlayerId().equals(after.getPlayerId())) {
            throw new IllegalArgumentException("Cannot compare snapshots for different players");
        }

        List<ItemChange> changes = new ArrayList<>();

        // Compare main inventory
        UnifiedItemStack[] beforeContents = before.getContents();
        UnifiedItemStack[] afterContents = after.getContents();
        for (int i = 0; i < Math.max(beforeContents.length, afterContents.length); i++) {
            UnifiedItemStack beforeItem = i < beforeContents.length ? beforeContents[i] : null;
            UnifiedItemStack afterItem = i < afterContents.length ? afterContents[i] : null;
            ItemChange change = compareItems(i, InventorySlot.SlotType.MAIN, beforeItem, afterItem);
            if (change != null) {
                changes.add(change);
            }
        }

        // Compare armor
        UnifiedItemStack[] beforeArmor = before.getArmorContents();
        UnifiedItemStack[] afterArmor = after.getArmorContents();
        int[] armorSlots = {
            InventorySlot.BOOTS_SLOT,
            InventorySlot.LEGGINGS_SLOT,
            InventorySlot.CHESTPLATE_SLOT,
            InventorySlot.HELMET_SLOT
        };
        for (int i = 0; i < 4; i++) {
            UnifiedItemStack beforeItem = beforeArmor[i];
            UnifiedItemStack afterItem = afterArmor[i];
            ItemChange change = compareItems(armorSlots[i], InventorySlot.SlotType.ARMOR, beforeItem, afterItem);
            if (change != null) {
                changes.add(change);
            }
        }

        // Compare offhand
        ItemChange offhandChange = compareItems(
            InventorySlot.OFFHAND_SLOT,
            InventorySlot.SlotType.OFFHAND,
            before.getOffhand(),
            after.getOffhand()
        );
        if (offhandChange != null) {
            changes.add(offhandChange);
        }

        // Compare ender chest if both have it
        if (before.hasEnderChest() && after.hasEnderChest()) {
            UnifiedItemStack[] beforeEnder = before.getEnderChest();
            UnifiedItemStack[] afterEnder = after.getEnderChest();
            for (int i = 0; i < Math.max(beforeEnder.length, afterEnder.length); i++) {
                UnifiedItemStack beforeItem = i < beforeEnder.length ? beforeEnder[i] : null;
                UnifiedItemStack afterItem = i < afterEnder.length ? afterEnder[i] : null;
                ItemChange change = compareItems(
                    InventorySlot.ENDER_CHEST_START + i,
                    InventorySlot.SlotType.ENDER_CHEST,
                    beforeItem,
                    afterItem
                );
                if (change != null) {
                    changes.add(change);
                }
            }
        }

        return new InventoryDiff(
            before.getSnapshotId(),
            after.getSnapshotId(),
            before.getPlayerId(),
            before.getCapturedAt(),
            after.getCapturedAt(),
            changes
        );
    }

    private static ItemChange compareItems(
        int slot,
        InventorySlot.SlotType slotType,
        @Nullable UnifiedItemStack before,
        @Nullable UnifiedItemStack after
    ) {
        boolean beforeEmpty = before == null || before.isEmpty();
        boolean afterEmpty = after == null || after.isEmpty();

        if (beforeEmpty && afterEmpty) {
            return null; // No change
        }

        if (beforeEmpty) {
            return new ItemChange(slot, slotType, null, after, ChangeType.ADDED);
        }

        if (afterEmpty) {
            return new ItemChange(slot, slotType, before, null, ChangeType.REMOVED);
        }

        // Both have items - check if they're different
        if (!before.isSimilar(after) || before.getAmount() != after.getAmount()) {
            return new ItemChange(slot, slotType, before, after, ChangeType.MODIFIED);
        }

        return null; // Items are identical
    }

    // ========== Getters ==========

    /**
     * Returns the before snapshot ID.
     *
     * @return the before snapshot ID
     * @since 1.0.0
     */
    @NotNull
    public String getBeforeSnapshotId() {
        return beforeSnapshotId;
    }

    /**
     * Returns the after snapshot ID.
     *
     * @return the after snapshot ID
     * @since 1.0.0
     */
    @NotNull
    public String getAfterSnapshotId() {
        return afterSnapshotId;
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player ID
     * @since 1.0.0
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the before snapshot timestamp.
     *
     * @return the before time
     * @since 1.0.0
     */
    @NotNull
    public Instant getBeforeTime() {
        return beforeTime;
    }

    /**
     * Returns the after snapshot timestamp.
     *
     * @return the after time
     * @since 1.0.0
     */
    @NotNull
    public Instant getAfterTime() {
        return afterTime;
    }

    /**
     * Returns all changes.
     *
     * @return unmodifiable list of changes
     * @since 1.0.0
     */
    @NotNull
    public List<ItemChange> getChanges() {
        return changes;
    }

    /**
     * Returns changes of a specific type.
     *
     * @param type the change type
     * @return list of changes of that type
     * @since 1.0.0
     */
    @NotNull
    public List<ItemChange> getChanges(@NotNull ChangeType type) {
        return changes.stream()
            .filter(c -> c.type() == type)
            .toList();
    }

    /**
     * Returns added items.
     *
     * @return list of ADDED changes
     * @since 1.0.0
     */
    @NotNull
    public List<ItemChange> getAddedItems() {
        return getChanges(ChangeType.ADDED);
    }

    /**
     * Returns removed items.
     *
     * @return list of REMOVED changes
     * @since 1.0.0
     */
    @NotNull
    public List<ItemChange> getRemovedItems() {
        return getChanges(ChangeType.REMOVED);
    }

    /**
     * Returns modified items.
     *
     * @return list of MODIFIED changes
     * @since 1.0.0
     */
    @NotNull
    public List<ItemChange> getModifiedItems() {
        return getChanges(ChangeType.MODIFIED);
    }

    /**
     * Returns changes for a specific slot type.
     *
     * @param slotType the slot type
     * @return list of changes for that slot type
     * @since 1.0.0
     */
    @NotNull
    public List<ItemChange> getChangesBySlotType(@NotNull InventorySlot.SlotType slotType) {
        return changes.stream()
            .filter(c -> c.slotType() == slotType)
            .toList();
    }

    /**
     * Gets the change for a specific slot.
     *
     * @param slot the slot index
     * @return the change, or empty if no change in that slot
     * @since 1.0.0
     */
    @NotNull
    public Optional<ItemChange> getChangeForSlot(int slot) {
        return changes.stream()
            .filter(c -> c.slot() == slot)
            .findFirst();
    }

    // ========== Statistics ==========

    /**
     * Returns the total number of changes.
     *
     * @return the change count
     * @since 1.0.0
     */
    public int getTotalChanges() {
        return changes.size();
    }

    /**
     * Returns the count of changes by type.
     *
     * @return map of change type to count
     * @since 1.0.0
     */
    @NotNull
    public Map<ChangeType, Integer> getChangeCounts() {
        return changes.stream()
            .collect(Collectors.groupingBy(
                ItemChange::type,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    /**
     * Returns the net item count change (positive = gained, negative = lost).
     *
     * @return the net item count change
     * @since 1.0.0
     */
    public int getNetItemChange() {
        return getItemsGained() - getItemsLost();
    }

    /**
     * Returns the total number of items gained.
     *
     * @return items gained
     * @since 1.0.0
     */
    public int getItemsGained() {
        if (itemsGained == null) {
            itemsGained = changes.stream()
                .filter(c -> c.type() == ChangeType.ADDED)
                .mapToInt(c -> c.after() != null ? c.after().getAmount() : 0)
                .sum();
            itemsGained += changes.stream()
                .filter(c -> c.type() == ChangeType.MODIFIED)
                .mapToInt(c -> {
                    int beforeAmt = c.before() != null ? c.before().getAmount() : 0;
                    int afterAmt = c.after() != null ? c.after().getAmount() : 0;
                    return Math.max(0, afterAmt - beforeAmt);
                })
                .sum();
        }
        return itemsGained;
    }

    /**
     * Returns the total number of items lost.
     *
     * @return items lost
     * @since 1.0.0
     */
    public int getItemsLost() {
        if (itemsLost == null) {
            itemsLost = changes.stream()
                .filter(c -> c.type() == ChangeType.REMOVED)
                .mapToInt(c -> c.before() != null ? c.before().getAmount() : 0)
                .sum();
            itemsLost += changes.stream()
                .filter(c -> c.type() == ChangeType.MODIFIED)
                .mapToInt(c -> {
                    int beforeAmt = c.before() != null ? c.before().getAmount() : 0;
                    int afterAmt = c.after() != null ? c.after().getAmount() : 0;
                    return Math.max(0, beforeAmt - afterAmt);
                })
                .sum();
        }
        return itemsLost;
    }

    /**
     * Checks if there are any changes.
     *
     * @return true if there are changes
     * @since 1.0.0
     */
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Checks if there are any changes of a specific type.
     *
     * @param type the change type
     * @return true if there are changes of that type
     * @since 1.0.0
     */
    public boolean hasChanges(@NotNull ChangeType type) {
        return changes.stream().anyMatch(c -> c.type() == type);
    }

    @Override
    public String toString() {
        return "InventoryDiff{" +
            "player=" + playerId +
            ", changes=" + changes.size() +
            ", gained=" + getItemsGained() +
            ", lost=" + getItemsLost() +
            ", from=" + beforeTime +
            ", to=" + afterTime +
            '}';
    }

    // ========== Nested Types ==========

    /**
     * Represents a single item change.
     *
     * @param slot     the slot index
     * @param slotType the slot type
     * @param before   the item before (null if added)
     * @param after    the item after (null if removed)
     * @param type     the change type
     * @since 1.0.0
     */
    public record ItemChange(
        int slot,
        @NotNull InventorySlot.SlotType slotType,
        @Nullable UnifiedItemStack before,
        @Nullable UnifiedItemStack after,
        @NotNull ChangeType type
    ) {
        /**
         * Returns the slot index.
         */
        public int getSlot() {
            return slot;
        }

        /**
         * Returns the item before the change.
         */
        @Nullable
        public UnifiedItemStack getBefore() {
            return before;
        }

        /**
         * Returns the item after the change.
         */
        @Nullable
        public UnifiedItemStack getAfter() {
            return after;
        }

        /**
         * Returns the change type.
         */
        @NotNull
        public ChangeType getType() {
            return type;
        }

        /**
         * Returns the amount changed (positive for gains, negative for losses).
         */
        public int getAmountChange() {
            int beforeAmt = before != null ? before.getAmount() : 0;
            int afterAmt = after != null ? after.getAmount() : 0;
            return afterAmt - beforeAmt;
        }
    }

    /**
     * Types of inventory changes.
     *
     * @since 1.0.0
     */
    public enum ChangeType {
        /**
         * Item added to an empty slot.
         */
        ADDED,

        /**
         * Item removed from a slot.
         */
        REMOVED,

        /**
         * Item in slot was modified.
         */
        MODIFIED,

        /**
         * Item moved to a different slot.
         */
        MOVED
    }
}
