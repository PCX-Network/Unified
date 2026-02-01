/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.core;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable record representing a single inventory slot with its contents and type.
 *
 * <p>This record encapsulates the slot index, the item stored in that slot, and the
 * type of slot (main inventory, armor, offhand, or ender chest). It is used throughout
 * the inventory management system for consistent slot representation.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a slot for main inventory
 * InventorySlot mainSlot = new InventorySlot(0, itemStack, SlotType.MAIN);
 *
 * // Create an armor slot
 * InventorySlot helmetSlot = new InventorySlot(
 *     InventorySlot.HELMET_SLOT,
 *     helmetItem,
 *     SlotType.ARMOR
 * );
 *
 * // Check slot properties
 * int index = slot.slot();
 * UnifiedItemStack item = slot.item();
 * SlotType type = slot.type();
 *
 * // Check if slot is empty
 * boolean empty = slot.isEmpty();
 * }</pre>
 *
 * <h2>Slot Index Constants</h2>
 * <p>Standard slot indices are provided as constants for armor and offhand slots.
 * Main inventory slots use indices 0-35, with 0-8 being the hotbar.
 *
 * @param slot the slot index (0-35 for main inventory, special indices for armor/offhand)
 * @param item the item stored in this slot, may be null for empty slots
 * @param type the type of slot
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SlotType
 * @see InventorySnapshot
 */
public record InventorySlot(
        int slot,
        @Nullable UnifiedItemStack item,
        @NotNull SlotType type
) {

    /**
     * Slot index for the helmet armor slot.
     */
    public static final int HELMET_SLOT = 103;

    /**
     * Slot index for the chestplate armor slot.
     */
    public static final int CHESTPLATE_SLOT = 102;

    /**
     * Slot index for the leggings armor slot.
     */
    public static final int LEGGINGS_SLOT = 101;

    /**
     * Slot index for the boots armor slot.
     */
    public static final int BOOTS_SLOT = 100;

    /**
     * Slot index for the offhand slot.
     */
    public static final int OFFHAND_SLOT = 40;

    /**
     * Starting slot index for ender chest contents.
     */
    public static final int ENDER_CHEST_START = 200;

    /**
     * Number of slots in the main player inventory (excluding armor and offhand).
     */
    public static final int MAIN_INVENTORY_SIZE = 36;

    /**
     * Number of armor slots.
     */
    public static final int ARMOR_SIZE = 4;

    /**
     * Number of ender chest slots.
     */
    public static final int ENDER_CHEST_SIZE = 27;

    /**
     * Creates a new InventorySlot with validation.
     *
     * @param slot the slot index
     * @param item the item in this slot
     * @param type the slot type
     * @throws IllegalArgumentException if slot index is negative
     * @throws NullPointerException if type is null
     */
    public InventorySlot {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot index cannot be negative: " + slot);
        }
        java.util.Objects.requireNonNull(type, "Slot type cannot be null");
    }

    /**
     * Creates an empty slot of the specified type.
     *
     * @param slot the slot index
     * @param type the slot type
     * @return a new InventorySlot with no item
     * @since 1.0.0
     */
    @NotNull
    public static InventorySlot empty(int slot, @NotNull SlotType type) {
        return new InventorySlot(slot, null, type);
    }

    /**
     * Creates a main inventory slot.
     *
     * @param slot the slot index (0-35)
     * @param item the item in the slot
     * @return a new InventorySlot for main inventory
     * @throws IllegalArgumentException if slot is not in valid range
     * @since 1.0.0
     */
    @NotNull
    public static InventorySlot main(int slot, @Nullable UnifiedItemStack item) {
        if (slot < 0 || slot >= MAIN_INVENTORY_SIZE) {
            throw new IllegalArgumentException("Main inventory slot must be 0-35: " + slot);
        }
        return new InventorySlot(slot, item, SlotType.MAIN);
    }

    /**
     * Creates an armor slot.
     *
     * @param slot the armor slot index (100-103)
     * @param item the armor item
     * @return a new InventorySlot for armor
     * @since 1.0.0
     */
    @NotNull
    public static InventorySlot armor(int slot, @Nullable UnifiedItemStack item) {
        return new InventorySlot(slot, item, SlotType.ARMOR);
    }

    /**
     * Creates the offhand slot.
     *
     * @param item the offhand item
     * @return a new InventorySlot for offhand
     * @since 1.0.0
     */
    @NotNull
    public static InventorySlot offhand(@Nullable UnifiedItemStack item) {
        return new InventorySlot(OFFHAND_SLOT, item, SlotType.OFFHAND);
    }

    /**
     * Creates an ender chest slot.
     *
     * @param slot the ender chest slot index (0-26)
     * @param item the item in the slot
     * @return a new InventorySlot for ender chest
     * @throws IllegalArgumentException if slot is not in valid range
     * @since 1.0.0
     */
    @NotNull
    public static InventorySlot enderChest(int slot, @Nullable UnifiedItemStack item) {
        if (slot < 0 || slot >= ENDER_CHEST_SIZE) {
            throw new IllegalArgumentException("Ender chest slot must be 0-26: " + slot);
        }
        return new InventorySlot(ENDER_CHEST_START + slot, item, SlotType.ENDER_CHEST);
    }

    /**
     * Checks if this slot is empty (no item or air item).
     *
     * @return true if the slot contains no item
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return item == null || item.isEmpty();
    }

    /**
     * Checks if this slot contains an item.
     *
     * @return true if the slot contains an item
     * @since 1.0.0
     */
    public boolean hasItem() {
        return !isEmpty();
    }

    /**
     * Returns the item or empty item if null.
     *
     * @return the item, never null (returns empty item if slot is empty)
     * @since 1.0.0
     */
    @NotNull
    public UnifiedItemStack getItemOrEmpty() {
        return item != null ? item : UnifiedItemStack.empty();
    }

    /**
     * Checks if this is a hotbar slot (0-8).
     *
     * @return true if this is a hotbar slot
     * @since 1.0.0
     */
    public boolean isHotbar() {
        return type == SlotType.MAIN && slot >= 0 && slot <= 8;
    }

    /**
     * Returns the ender chest index for ender chest slots.
     *
     * @return the ender chest index (0-26), or -1 if not an ender chest slot
     * @since 1.0.0
     */
    public int getEnderChestIndex() {
        if (type != SlotType.ENDER_CHEST) {
            return -1;
        }
        return slot - ENDER_CHEST_START;
    }

    /**
     * Creates a copy of this slot with a different item.
     *
     * @param newItem the new item for the slot
     * @return a new InventorySlot with the specified item
     * @since 1.0.0
     */
    @NotNull
    public InventorySlot withItem(@Nullable UnifiedItemStack newItem) {
        return new InventorySlot(slot, newItem, type);
    }

    /**
     * Enumeration of inventory slot types.
     *
     * @since 1.0.0
     */
    public enum SlotType {
        /**
         * Main player inventory slots (0-35).
         * Includes hotbar (0-8) and main storage (9-35).
         */
        MAIN,

        /**
         * Armor slots (helmet, chestplate, leggings, boots).
         */
        ARMOR,

        /**
         * Offhand slot (shield slot).
         */
        OFFHAND,

        /**
         * Ender chest storage slots (0-26).
         */
        ENDER_CHEST
    }
}
