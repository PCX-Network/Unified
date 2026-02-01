/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.inventory;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.testing.player.MockPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Mock implementation of an inventory for testing purposes.
 *
 * <p>MockInventory provides a complete simulation of a Minecraft inventory,
 * supporting all inventory operations including item manipulation, slot
 * access, and capacity management.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Item addition and removal</li>
 *   <li>Slot-based access</li>
 *   <li>Content searching</li>
 *   <li>Inventory clearing</li>
 *   <li>Item stacking</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockPlayer player = server.addPlayer("Steve");
 * MockInventory inventory = player.getInventory();
 *
 * // Add items
 * UnifiedItemStack item = ItemBuilder.of("minecraft:diamond").amount(10).build();
 * inventory.addItem(item);
 *
 * // Check contents
 * assertThat(inventory.contains("minecraft:diamond")).isTrue();
 * assertThat(inventory.countItem("minecraft:diamond")).isEqualTo(10);
 *
 * // Access slots
 * UnifiedItemStack slot5 = inventory.getItem(5);
 *
 * // Clear inventory
 * inventory.clear();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockPlayer
 */
public final class MockInventory {

    private final MockPlayer holder;
    private final int size;
    private final UnifiedItemStack[] contents;
    private int maxStackSize = 64;

    // Armor slots for player inventory
    private UnifiedItemStack helmet;
    private UnifiedItemStack chestplate;
    private UnifiedItemStack leggings;
    private UnifiedItemStack boots;

    /**
     * Creates a new mock inventory.
     *
     * @param holder the inventory holder (may be null for standalone inventories)
     * @param size   the inventory size
     */
    public MockInventory(@Nullable MockPlayer holder, int size) {
        this.holder = holder;
        this.size = size;
        this.contents = new UnifiedItemStack[size];
    }

    /**
     * Creates a player inventory with default size (36 slots).
     *
     * @param holder the player holder
     * @return the created inventory
     */
    @NotNull
    public static MockInventory playerInventory(@NotNull MockPlayer holder) {
        return new MockInventory(holder, 36);
    }

    /**
     * Creates a chest inventory.
     *
     * @param rows the number of rows (1-6)
     * @return the created inventory
     */
    @NotNull
    public static MockInventory chest(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6");
        }
        return new MockInventory(null, rows * 9);
    }

    /**
     * Returns the inventory size.
     *
     * @return the number of slots
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the inventory holder.
     *
     * @return the holder, or null if none
     */
    @Nullable
    public MockPlayer getHolder() {
        return holder;
    }

    /**
     * Returns the maximum stack size for this inventory.
     *
     * @return the max stack size
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * Sets the maximum stack size for this inventory.
     *
     * @param maxStackSize the max stack size
     */
    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    // ==================== Item Access ====================

    /**
     * Gets the item at a specific slot.
     *
     * @param slot the slot index
     * @return the item at the slot, or null if empty
     */
    @Nullable
    public UnifiedItemStack getItem(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds (0-" + (size - 1) + ")");
        }
        return contents[slot];
    }

    /**
     * Sets the item at a specific slot.
     *
     * @param slot the slot index
     * @param item the item to set (null to clear)
     */
    public void setItem(int slot, @Nullable UnifiedItemStack item) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds (0-" + (size - 1) + ")");
        }
        contents[slot] = item;
    }

    /**
     * Returns all contents of this inventory.
     *
     * @return array of all items
     */
    @NotNull
    public UnifiedItemStack[] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /**
     * Sets all contents of this inventory.
     *
     * @param items the items to set
     */
    public void setContents(@NotNull UnifiedItemStack[] items) {
        Objects.requireNonNull(items, "items cannot be null");
        System.arraycopy(items, 0, contents, 0, Math.min(items.length, size));
    }

    // ==================== Item Addition ====================

    /**
     * Adds an item to the inventory.
     *
     * <p>Attempts to stack with existing items first, then uses empty slots.
     *
     * @param item the item to add
     * @return true if all items were added, false if some couldn't fit
     */
    public boolean addItem(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");

        if (item.isEmpty()) {
            return true;
        }

        int remaining = item.getAmount();
        String type = item.getType();
        int maxStack = Math.min(item.getMaxStackSize(), maxStackSize);

        // First pass: try to stack with existing items
        for (int i = 0; i < size && remaining > 0; i++) {
            UnifiedItemStack existing = contents[i];
            if (existing != null && existing.isSimilar(item)) {
                int canAdd = maxStack - existing.getAmount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining);
                    existing.setAmount(existing.getAmount() + toAdd);
                    remaining -= toAdd;
                }
            }
        }

        // Second pass: use empty slots
        for (int i = 0; i < size && remaining > 0; i++) {
            if (contents[i] == null || contents[i].isEmpty()) {
                int toAdd = Math.min(maxStack, remaining);
                contents[i] = item.withAmount(toAdd);
                remaining -= toAdd;
            }
        }

        return remaining == 0;
    }

    /**
     * Adds multiple items to the inventory.
     *
     * @param items the items to add
     * @return map of slot to items that couldn't be added
     */
    @NotNull
    public Map<Integer, UnifiedItemStack> addItems(@NotNull UnifiedItemStack... items) {
        Map<Integer, UnifiedItemStack> leftover = new HashMap<>();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && !addItem(items[i])) {
                leftover.put(i, items[i]);
            }
        }
        return leftover;
    }

    // ==================== Item Removal ====================

    /**
     * Removes a specific amount of an item type from the inventory.
     *
     * @param type   the item type to remove
     * @param amount the amount to remove
     * @return the actual amount removed
     */
    public int removeItem(@NotNull String type, int amount) {
        Objects.requireNonNull(type, "type cannot be null");

        int remaining = amount;
        for (int i = 0; i < size && remaining > 0; i++) {
            UnifiedItemStack item = contents[i];
            if (item != null && item.getType().equals(type)) {
                int toRemove = Math.min(item.getAmount(), remaining);
                if (toRemove == item.getAmount()) {
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - toRemove);
                }
                remaining -= toRemove;
            }
        }
        return amount - remaining;
    }

    /**
     * Removes an item stack from the inventory.
     *
     * @param item the item to remove
     * @return true if the item was fully removed
     */
    public boolean removeItem(@NotNull UnifiedItemStack item) {
        int removed = removeItem(item.getType(), item.getAmount());
        return removed == item.getAmount();
    }

    /**
     * Clears a specific slot.
     *
     * @param slot the slot to clear
     */
    public void clear(int slot) {
        if (slot >= 0 && slot < size) {
            contents[slot] = null;
        }
    }

    /**
     * Clears all items from the inventory.
     */
    public void clear() {
        Arrays.fill(contents, null);
        helmet = null;
        chestplate = null;
        leggings = null;
        boots = null;
    }

    // ==================== Item Searching ====================

    /**
     * Checks if the inventory contains an item of the specified type.
     *
     * @param type the item type to search for
     * @return true if found
     */
    public boolean contains(@NotNull String type) {
        Objects.requireNonNull(type, "type cannot be null");
        for (UnifiedItemStack item : contents) {
            if (item != null && item.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the inventory contains at least the specified amount of an item type.
     *
     * @param type   the item type
     * @param amount the minimum amount
     * @return true if the inventory contains at least that amount
     */
    public boolean contains(@NotNull String type, int amount) {
        return countItem(type) >= amount;
    }

    /**
     * Checks if the inventory contains an item similar to the given one.
     *
     * @param item the item to check for
     * @return true if found
     */
    public boolean containsSimilar(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");
        for (UnifiedItemStack existing : contents) {
            if (existing != null && existing.isSimilar(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the total amount of an item type in the inventory.
     *
     * @param type the item type
     * @return the total count
     */
    public int countItem(@NotNull String type) {
        Objects.requireNonNull(type, "type cannot be null");
        int count = 0;
        for (UnifiedItemStack item : contents) {
            if (item != null && item.getType().equals(type)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Finds the first slot containing an item of the specified type.
     *
     * @param type the item type
     * @return the slot index, or -1 if not found
     */
    public int first(@NotNull String type) {
        Objects.requireNonNull(type, "type cannot be null");
        for (int i = 0; i < size; i++) {
            if (contents[i] != null && contents[i].getType().equals(type)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the first empty slot.
     *
     * @return the slot index, or -1 if full
     */
    public int firstEmpty() {
        for (int i = 0; i < size; i++) {
            if (contents[i] == null || contents[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if the inventory is empty.
     *
     * @return true if all slots are empty
     */
    public boolean isEmpty() {
        for (UnifiedItemStack item : contents) {
            if (item != null && !item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the inventory is full.
     *
     * @return true if no slots are empty
     */
    public boolean isFull() {
        return firstEmpty() == -1;
    }

    // ==================== Armor Slots (Player Inventory) ====================

    /**
     * Gets the helmet slot.
     *
     * @return the helmet item
     */
    @Nullable
    public UnifiedItemStack getHelmet() {
        return helmet;
    }

    /**
     * Sets the helmet slot.
     *
     * @param helmet the helmet item
     */
    public void setHelmet(@Nullable UnifiedItemStack helmet) {
        this.helmet = helmet;
    }

    /**
     * Gets the chestplate slot.
     *
     * @return the chestplate item
     */
    @Nullable
    public UnifiedItemStack getChestplate() {
        return chestplate;
    }

    /**
     * Sets the chestplate slot.
     *
     * @param chestplate the chestplate item
     */
    public void setChestplate(@Nullable UnifiedItemStack chestplate) {
        this.chestplate = chestplate;
    }

    /**
     * Gets the leggings slot.
     *
     * @return the leggings item
     */
    @Nullable
    public UnifiedItemStack getLeggings() {
        return leggings;
    }

    /**
     * Sets the leggings slot.
     *
     * @param leggings the leggings item
     */
    public void setLeggings(@Nullable UnifiedItemStack leggings) {
        this.leggings = leggings;
    }

    /**
     * Gets the boots slot.
     *
     * @return the boots item
     */
    @Nullable
    public UnifiedItemStack getBoots() {
        return boots;
    }

    /**
     * Sets the boots slot.
     *
     * @param boots the boots item
     */
    public void setBoots(@Nullable UnifiedItemStack boots) {
        this.boots = boots;
    }

    /**
     * Gets all armor items.
     *
     * @return array of armor items [helmet, chestplate, leggings, boots]
     */
    @NotNull
    public UnifiedItemStack[] getArmorContents() {
        return new UnifiedItemStack[]{helmet, chestplate, leggings, boots};
    }

    /**
     * Sets all armor items.
     *
     * @param armor the armor items [helmet, chestplate, leggings, boots]
     */
    public void setArmorContents(@NotNull UnifiedItemStack[] armor) {
        Objects.requireNonNull(armor, "armor cannot be null");
        if (armor.length >= 1) helmet = armor[0];
        if (armor.length >= 2) chestplate = armor[1];
        if (armor.length >= 3) leggings = armor[2];
        if (armor.length >= 4) boots = armor[3];
    }

    // ==================== Iteration ====================

    /**
     * Returns an iterator over all items in this inventory.
     *
     * @return the iterator
     */
    @NotNull
    public Iterator<UnifiedItemStack> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public UnifiedItemStack next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return contents[index++];
            }
        };
    }

    /**
     * Returns all non-null items in this inventory.
     *
     * @return list of items
     */
    @NotNull
    public List<UnifiedItemStack> getItems() {
        List<UnifiedItemStack> items = new ArrayList<>();
        for (UnifiedItemStack item : contents) {
            if (item != null && !item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public String toString() {
        return "MockInventory{" +
            "size=" + size +
            ", holder=" + (holder != null ? holder.getPlayerName() : "null") +
            ", items=" + getItems().size() +
            '}';
    }
}
