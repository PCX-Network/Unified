/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.core;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable snapshot of a player's complete inventory state.
 *
 * <p>An InventorySnapshot captures the complete state of a player's inventory at a
 * specific point in time, including main inventory contents, armor slots, offhand,
 * and optionally ender chest contents. Snapshots are immutable and can be safely
 * serialized, stored, and transferred across servers.
 *
 * <h2>Captured Contents</h2>
 * <ul>
 *   <li><b>Main Inventory</b>: 36 slots (0-35), including hotbar (0-8)</li>
 *   <li><b>Armor</b>: 4 slots (helmet, chestplate, leggings, boots)</li>
 *   <li><b>Offhand</b>: 1 slot (shield/secondary item)</li>
 *   <li><b>Ender Chest</b>: 27 slots (optional, if captured)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Capture a snapshot
 * InventorySnapshot snapshot = inventoryService.capture(player);
 *
 * // Access contents
 * UnifiedItemStack[] contents = snapshot.getContents();
 * UnifiedItemStack[] armor = snapshot.getArmorContents();
 * UnifiedItemStack offhand = snapshot.getOffhand();
 * UnifiedItemStack[] enderChest = snapshot.getEnderChest();
 *
 * // Access specific slots
 * UnifiedItemStack helmet = snapshot.getHelmet();
 * UnifiedItemStack itemAtSlot5 = snapshot.getItem(5);
 *
 * // Check metadata
 * UUID playerId = snapshot.getPlayerId();
 * Instant capturedAt = snapshot.getCapturedAt();
 * String snapshotId = snapshot.getSnapshotId();
 *
 * // Serialize for storage or transfer
 * byte[] bytes = snapshot.toBytes();
 * String base64 = snapshot.toBase64();
 * String json = snapshot.toJson();
 *
 * // Apply to a player
 * snapshot.applyTo(player);
 * snapshot.applyTo(player, ApplyMode.MERGE);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>InventorySnapshot instances are immutable and therefore thread-safe.
 * The same snapshot can be safely shared between threads.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventoryService
 * @see ApplyMode
 */
public interface InventorySnapshot {

    /**
     * Returns the unique identifier for this snapshot.
     *
     * <p>The snapshot ID is automatically generated when the snapshot is created
     * and can be used to reference this specific snapshot in storage or logs.
     *
     * @return the unique snapshot identifier
     * @since 1.0.0
     */
    @NotNull
    String getSnapshotId();

    /**
     * Returns the UUID of the player this snapshot belongs to.
     *
     * @return the player's UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getPlayerId();

    /**
     * Returns the timestamp when this snapshot was captured.
     *
     * @return the capture timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getCapturedAt();

    /**
     * Returns the version number of this snapshot.
     *
     * <p>Version numbers increment for each snapshot of the same player,
     * enabling rollback functionality and history tracking.
     *
     * @return the snapshot version
     * @since 1.0.0
     */
    int getVersion();

    // ========== Contents Access ==========

    /**
     * Returns the main inventory contents (36 slots).
     *
     * <p>The returned array is a defensive copy; modifications will not
     * affect the snapshot.
     *
     * @return array of items in the main inventory (length 36)
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack[] getContents();

    /**
     * Returns the armor contents.
     *
     * <p>Array indices: 0=boots, 1=leggings, 2=chestplate, 3=helmet
     *
     * @return array of armor items (length 4)
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack[] getArmorContents();

    /**
     * Returns the offhand item.
     *
     * @return the offhand item, or empty item if none
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getOffhand();

    /**
     * Returns the ender chest contents.
     *
     * <p>Returns an empty array if ender chest was not captured.
     *
     * @return array of ender chest items (length 27, or 0 if not captured)
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack[] getEnderChest();

    /**
     * Checks if this snapshot includes ender chest contents.
     *
     * @return true if ender chest was captured
     * @since 1.0.0
     */
    boolean hasEnderChest();

    // ========== Slot Access ==========

    /**
     * Returns the item at the specified main inventory slot.
     *
     * @param slot the slot index (0-35)
     * @return the item at that slot, or empty item if none
     * @throws IndexOutOfBoundsException if slot is out of range
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getItem(int slot);

    /**
     * Returns all slots as InventorySlot records.
     *
     * <p>This includes main inventory, armor, offhand, and ender chest (if captured).
     *
     * @return list of all inventory slots
     * @since 1.0.0
     */
    @NotNull
    List<InventorySlot> getAllSlots();

    /**
     * Returns only non-empty slots.
     *
     * @return list of slots containing items
     * @since 1.0.0
     */
    @NotNull
    List<InventorySlot> getNonEmptySlots();

    /**
     * Returns the helmet.
     *
     * @return the helmet item, or empty item if none
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getHelmet();

    /**
     * Returns the chestplate.
     *
     * @return the chestplate item, or empty item if none
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getChestplate();

    /**
     * Returns the leggings.
     *
     * @return the leggings item, or empty item if none
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getLeggings();

    /**
     * Returns the boots.
     *
     * @return the boots item, or empty item if none
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getBoots();

    // ========== Statistics ==========

    /**
     * Returns the total number of items across all slots.
     *
     * <p>This counts the sum of all item amounts, not just filled slots.
     *
     * @return the total item count
     * @since 1.0.0
     */
    int getTotalItemCount();

    /**
     * Returns the number of empty slots in main inventory.
     *
     * @return the empty slot count
     * @since 1.0.0
     */
    int getEmptySlotCount();

    /**
     * Checks if the inventory is completely empty.
     *
     * @return true if no items in any slot (including armor and offhand)
     * @since 1.0.0
     */
    boolean isEmpty();

    // ========== Serialization ==========

    /**
     * Serializes this snapshot to a byte array.
     *
     * <p>The byte array can be stored in a database, sent over network,
     * or converted to Base64 for text-based storage.
     *
     * @return the serialized snapshot data
     * @since 1.0.0
     */
    byte @NotNull [] toBytes();

    /**
     * Serializes this snapshot to a Base64 string.
     *
     * <p>Useful for storing in text-based configurations or databases
     * that don't support binary data.
     *
     * @return the Base64-encoded snapshot
     * @since 1.0.0
     */
    @NotNull
    String toBase64();

    /**
     * Serializes this snapshot to a JSON string.
     *
     * <p>The JSON format is human-readable and suitable for debugging
     * or external API integration.
     *
     * @return the JSON representation
     * @since 1.0.0
     */
    @NotNull
    String toJson();

    // ========== Application ==========

    /**
     * Applies this snapshot to a player, replacing their inventory.
     *
     * <p>This is equivalent to calling {@code applyTo(player, ApplyMode.REPLACE)}.
     *
     * @param player the player to apply the snapshot to
     * @since 1.0.0
     */
    void applyTo(@NotNull UnifiedPlayer player);

    /**
     * Applies this snapshot to a player with the specified apply mode.
     *
     * @param player the player to apply the snapshot to
     * @param mode   the apply mode determining how to handle existing items
     * @since 1.0.0
     * @see ApplyMode
     */
    void applyTo(@NotNull UnifiedPlayer player, @NotNull ApplyMode mode);

    // ========== Builder ==========

    /**
     * Creates a new builder for constructing an InventorySnapshot.
     *
     * @param playerId the UUID of the player
     * @return a new snapshot builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder(@NotNull UUID playerId) {
        return new InventorySnapshotImpl.BuilderImpl(playerId);
    }

    /**
     * Builder for creating InventorySnapshot instances.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the main inventory contents.
         *
         * @param contents the inventory contents (36 slots)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder contents(@NotNull UnifiedItemStack[] contents);

        /**
         * Sets the armor contents.
         *
         * @param armor the armor contents (4 slots)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder armor(@NotNull UnifiedItemStack[] armor);

        /**
         * Sets the offhand item.
         *
         * @param offhand the offhand item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder offhand(@Nullable UnifiedItemStack offhand);

        /**
         * Sets the ender chest contents.
         *
         * @param enderChest the ender chest contents (27 slots)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder enderChest(@NotNull UnifiedItemStack[] enderChest);

        /**
         * Sets a specific item in the main inventory.
         *
         * @param slot the slot index (0-35)
         * @param item the item to set
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder slot(int slot, @Nullable UnifiedItemStack item);

        /**
         * Sets the helmet.
         *
         * @param helmet the helmet item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder helmet(@Nullable UnifiedItemStack helmet);

        /**
         * Sets the chestplate.
         *
         * @param chestplate the chestplate item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder chestplate(@Nullable UnifiedItemStack chestplate);

        /**
         * Sets the leggings.
         *
         * @param leggings the leggings item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder leggings(@Nullable UnifiedItemStack leggings);

        /**
         * Sets the boots.
         *
         * @param boots the boots item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder boots(@Nullable UnifiedItemStack boots);

        /**
         * Sets the version number.
         *
         * @param version the version number
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder version(int version);

        /**
         * Sets a custom snapshot ID.
         *
         * @param snapshotId the snapshot ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder snapshotId(@NotNull String snapshotId);

        /**
         * Sets the capture timestamp.
         *
         * @param timestamp the capture timestamp
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder capturedAt(@NotNull Instant timestamp);

        /**
         * Builds the InventorySnapshot.
         *
         * @return the constructed snapshot
         * @since 1.0.0
         */
        @NotNull
        InventorySnapshot build();
    }
}
