/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.core;

import sh.pcx.unified.data.inventory.history.InventoryDiff;
import sh.pcx.unified.data.inventory.preset.InventoryPreset;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service interface for inventory management operations.
 *
 * <p>InventoryService provides a comprehensive API for capturing, saving, loading,
 * and manipulating player inventories. It supports full inventory snapshots with
 * armor, offhand, and ender chest contents, as well as presets, history tracking,
 * and cross-server transfer.
 *
 * <h2>Core Features</h2>
 * <ul>
 *   <li><b>Capture</b>: Create snapshots of player inventories</li>
 *   <li><b>Save/Load</b>: Persist and restore inventories by name</li>
 *   <li><b>Presets</b>: Define and apply reusable inventory configurations</li>
 *   <li><b>History</b>: Track inventory changes for rollback</li>
 *   <li><b>Serialization</b>: Convert inventories for storage and transfer</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private InventoryService inventories;
 *
 * // Capture current inventory
 * InventorySnapshot snapshot = inventories.capture(player);
 *
 * // Capture with options
 * InventorySnapshot full = inventories.capture(player, CaptureOptions.builder()
 *     .includeEnderChest(true)
 *     .build());
 *
 * // Save to database
 * inventories.save(player, "death_backup").thenAccept(v -> {
 *     player.sendMessage("Inventory saved!");
 * });
 *
 * // Load and apply
 * inventories.load(player, "death_backup").thenAccept(snapshot -> {
 *     snapshot.applyTo(player, ApplyMode.REPLACE);
 * });
 *
 * // Apply a preset
 * inventories.applyPreset(player, "kit_pvp");
 *
 * // Get history
 * List<InventorySnapshot> history = inventories.getHistory(player, 10);
 *
 * // Compare snapshots
 * InventoryDiff diff = inventories.diff(before, after);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this service are thread-safe. Async operations return
 * CompletableFutures and complete on the async thread pool.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventorySnapshot
 * @see InventoryPreset
 */
public interface InventoryService extends Service {

    /**
     * Returns the singleton instance of the InventoryService.
     *
     * <p>This method is primarily for internal use. Prefer dependency injection
     * when possible.
     *
     * @return the inventory service instance
     * @throws IllegalStateException if the service is not initialized
     * @since 1.0.0
     */
    @NotNull
    static InventoryService getInstance() {
        return InventoryServiceHolder.INSTANCE;
    }

    /**
     * Sets the service instance (for internal framework use).
     *
     * @param service the service instance
     * @since 1.0.0
     */
    static void setInstance(@NotNull InventoryService service) {
        InventoryServiceHolder.INSTANCE = service;
    }

    // ========== Capture Operations ==========

    /**
     * Captures the current state of a player's inventory.
     *
     * <p>Uses default capture options (armor + offhand, no ender chest).
     *
     * @param player the player whose inventory to capture
     * @return the inventory snapshot
     * @since 1.0.0
     */
    @NotNull
    InventorySnapshot capture(@NotNull UnifiedPlayer player);

    /**
     * Captures the current state of a player's inventory with options.
     *
     * @param player  the player whose inventory to capture
     * @param options the capture options
     * @return the inventory snapshot
     * @since 1.0.0
     */
    @NotNull
    InventorySnapshot capture(@NotNull UnifiedPlayer player, @NotNull CaptureOptions options);

    /**
     * Creates a snapshot from serialized bytes.
     *
     * @param bytes the serialized snapshot data
     * @return the deserialized snapshot
     * @throws IllegalArgumentException if bytes are invalid
     * @since 1.0.0
     */
    @NotNull
    InventorySnapshot fromBytes(byte @NotNull [] bytes);

    /**
     * Creates a snapshot from a Base64 string.
     *
     * @param base64 the Base64-encoded snapshot
     * @return the deserialized snapshot
     * @throws IllegalArgumentException if the string is invalid
     * @since 1.0.0
     */
    @NotNull
    InventorySnapshot fromBase64(@NotNull String base64);

    /**
     * Creates a snapshot from a JSON string.
     *
     * @param json the JSON representation
     * @return the deserialized snapshot
     * @throws IllegalArgumentException if the JSON is invalid
     * @since 1.0.0
     */
    @NotNull
    InventorySnapshot fromJson(@NotNull String json);

    // ========== Save/Load Operations ==========

    /**
     * Saves a player's current inventory with the given name.
     *
     * @param player the player whose inventory to save
     * @param name   the name for the saved inventory
     * @return a future that completes when saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save(@NotNull UnifiedPlayer player, @NotNull String name);

    /**
     * Saves a snapshot with the given name for a player.
     *
     * @param playerId the player's UUID
     * @param name     the name for the saved inventory
     * @param snapshot the snapshot to save
     * @return a future that completes when saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save(@NotNull UUID playerId, @NotNull String name, @NotNull InventorySnapshot snapshot);

    /**
     * Loads a saved inventory by name.
     *
     * @param player the player
     * @param name   the name of the saved inventory
     * @return a future containing the snapshot, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<InventorySnapshot>> load(@NotNull UnifiedPlayer player, @NotNull String name);

    /**
     * Loads a saved inventory by player UUID and name.
     *
     * @param playerId the player's UUID
     * @param name     the name of the saved inventory
     * @return a future containing the snapshot, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<InventorySnapshot>> load(@NotNull UUID playerId, @NotNull String name);

    /**
     * Lists all saved inventories for a player.
     *
     * @param player the player
     * @return a future containing the list of saved inventory info
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<SavedInventory>> list(@NotNull UnifiedPlayer player);

    /**
     * Lists all saved inventories for a player UUID.
     *
     * @param playerId the player's UUID
     * @return a future containing the list of saved inventory info
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<SavedInventory>> list(@NotNull UUID playerId);

    /**
     * Deletes a saved inventory.
     *
     * @param player the player
     * @param name   the name of the saved inventory to delete
     * @return a future that completes when deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> delete(@NotNull UnifiedPlayer player, @NotNull String name);

    /**
     * Deletes a saved inventory by player UUID.
     *
     * @param playerId the player's UUID
     * @param name     the name of the saved inventory to delete
     * @return a future that completes with true if deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> delete(@NotNull UUID playerId, @NotNull String name);

    // ========== Apply Operations ==========

    /**
     * Applies a snapshot to a player's inventory.
     *
     * @param player   the player
     * @param snapshot the snapshot to apply
     * @param mode     the apply mode
     * @since 1.0.0
     */
    void apply(@NotNull UnifiedPlayer player, @NotNull InventorySnapshot snapshot, @NotNull ApplyMode mode);

    /**
     * Applies a snapshot to a player's inventory with REPLACE mode.
     *
     * @param player   the player
     * @param snapshot the snapshot to apply
     * @since 1.0.0
     */
    default void apply(@NotNull UnifiedPlayer player, @NotNull InventorySnapshot snapshot) {
        apply(player, snapshot, ApplyMode.REPLACE);
    }

    /**
     * Clears a player's inventory.
     *
     * @param player the player
     * @since 1.0.0
     */
    void clear(@NotNull UnifiedPlayer player);

    /**
     * Clears specific parts of a player's inventory.
     *
     * @param player         the player
     * @param clearMain      whether to clear main inventory
     * @param clearArmor     whether to clear armor
     * @param clearOffhand   whether to clear offhand
     * @param clearEnderChest whether to clear ender chest
     * @since 1.0.0
     */
    void clear(@NotNull UnifiedPlayer player, boolean clearMain, boolean clearArmor,
               boolean clearOffhand, boolean clearEnderChest);

    // ========== Preset Operations ==========

    /**
     * Applies a preset to a player.
     *
     * @param player the player
     * @param preset the preset to apply
     * @since 1.0.0
     */
    void applyPreset(@NotNull UnifiedPlayer player, @NotNull InventoryPreset preset);

    /**
     * Applies a preset by name to a player.
     *
     * @param player     the player
     * @param presetName the preset name
     * @return true if the preset was found and applied
     * @since 1.0.0
     */
    boolean applyPreset(@NotNull UnifiedPlayer player, @NotNull String presetName);

    /**
     * Saves a preset for later use.
     *
     * @param preset the preset to save
     * @return a future that completes when saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> savePreset(@NotNull InventoryPreset preset);

    /**
     * Loads a preset by name.
     *
     * @param name the preset name
     * @return the preset, or null if not found
     * @since 1.0.0
     */
    @Nullable
    InventoryPreset loadPreset(@NotNull String name);

    /**
     * Lists all available presets.
     *
     * @return list of preset names
     * @since 1.0.0
     */
    @NotNull
    List<String> listPresets();

    /**
     * Deletes a preset.
     *
     * @param name the preset name
     * @return true if the preset was deleted
     * @since 1.0.0
     */
    boolean deletePreset(@NotNull String name);

    // ========== History Operations ==========

    /**
     * Gets the inventory history for a player.
     *
     * @param player the player
     * @param limit  maximum number of snapshots to return
     * @return list of historical snapshots, newest first
     * @since 1.0.0
     */
    @NotNull
    List<InventorySnapshot> getHistory(@NotNull UnifiedPlayer player, int limit);

    /**
     * Gets the inventory history for a player UUID.
     *
     * @param playerId the player's UUID
     * @param limit    maximum number of snapshots to return
     * @return list of historical snapshots, newest first
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<InventorySnapshot>> getHistory(@NotNull UUID playerId, int limit);

    /**
     * Rolls back a player's inventory to a previous snapshot.
     *
     * @param player   the player
     * @param snapshot the snapshot to roll back to
     * @since 1.0.0
     */
    void rollback(@NotNull UnifiedPlayer player, @NotNull InventorySnapshot snapshot);

    /**
     * Compares two snapshots and returns the differences.
     *
     * @param before the before snapshot
     * @param after  the after snapshot
     * @return the diff between the snapshots
     * @since 1.0.0
     */
    @NotNull
    InventoryDiff diff(@NotNull InventorySnapshot before, @NotNull InventorySnapshot after);

    // ========== Utility Operations ==========

    /**
     * Checks if a player's inventory is empty.
     *
     * @param player the player
     * @return true if the inventory is empty
     * @since 1.0.0
     */
    boolean isEmpty(@NotNull UnifiedPlayer player);

    /**
     * Counts the number of empty slots in main inventory.
     *
     * @param player the player
     * @return the number of empty slots
     * @since 1.0.0
     */
    int getEmptySlotCount(@NotNull UnifiedPlayer player);

    /**
     * Checks if the player has room for additional items.
     *
     * @param player    the player
     * @param itemCount number of item stacks to check for
     * @return true if there's room for the items
     * @since 1.0.0
     */
    boolean hasRoom(@NotNull UnifiedPlayer player, int itemCount);

    /**
     * Information about a saved inventory.
     *
     * @param name      the save name
     * @param playerId  the player's UUID
     * @param savedAt   when the inventory was saved
     * @param itemCount total items in the saved inventory
     * @since 1.0.0
     */
    record SavedInventory(
        @NotNull String name,
        @NotNull UUID playerId,
        @NotNull java.time.Instant savedAt,
        int itemCount
    ) {}
}

/**
 * Holder for the InventoryService singleton instance.
 */
final class InventoryServiceHolder {
    static InventoryService INSTANCE;

    private InventoryServiceHolder() {}
}
