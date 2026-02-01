/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.core;

/**
 * Enumeration of modes for applying an inventory snapshot to a player.
 *
 * <p>When applying a snapshot to a player's inventory, different strategies can be used
 * to handle existing items. This enum defines the available strategies.
 *
 * <h2>Mode Descriptions</h2>
 * <ul>
 *   <li><b>REPLACE</b>: Clear the player's inventory before applying the snapshot.
 *       Any existing items are lost.</li>
 *   <li><b>MERGE</b>: Add snapshot items to empty slots only. Existing items are preserved.
 *       Items that cannot be added (no empty slots) are dropped.</li>
 *   <li><b>MERGE_STACK</b>: Like MERGE, but also stacks with existing similar items
 *       before filling empty slots.</li>
 *   <li><b>SAFE</b>: Only apply if the player's inventory is completely empty.
 *       Fails silently if inventory contains any items.</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Replace inventory completely
 * snapshot.applyTo(player, ApplyMode.REPLACE);
 *
 * // Merge with existing items
 * snapshot.applyTo(player, ApplyMode.MERGE);
 *
 * // Merge and stack with similar items
 * snapshot.applyTo(player, ApplyMode.MERGE_STACK);
 *
 * // Only apply if inventory is empty
 * snapshot.applyTo(player, ApplyMode.SAFE);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventorySnapshot#applyTo(sh.pcx.unified.player.UnifiedPlayer, ApplyMode)
 */
public enum ApplyMode {

    /**
     * Clear the player's inventory before applying the snapshot.
     *
     * <p>All existing items are removed and replaced with the snapshot contents.
     * This is the most common mode for restoring saved inventories.
     */
    REPLACE,

    /**
     * Add snapshot items to empty slots only.
     *
     * <p>Existing items are preserved in their slots. Snapshot items are only added
     * to slots that are currently empty. If there are not enough empty slots,
     * remaining items may be dropped on the ground or lost.
     */
    MERGE,

    /**
     * Merge with existing items, stacking when possible.
     *
     * <p>Similar to MERGE, but first attempts to stack snapshot items with
     * existing items of the same type before filling empty slots. This is
     * useful for adding items without creating duplicates.
     */
    MERGE_STACK,

    /**
     * Only apply if the player's inventory is completely empty.
     *
     * <p>This is a safety mode that prevents accidentally overwriting a player's
     * items. The operation will silently do nothing if any slot contains an item.
     * Use this when applying default kits or initial inventories.
     */
    SAFE;

    /**
     * Returns the default apply mode.
     *
     * @return {@link #REPLACE}
     * @since 1.0.0
     */
    public static ApplyMode defaultMode() {
        return REPLACE;
    }

    /**
     * Checks if this mode clears existing items.
     *
     * @return true if existing items are cleared before applying
     * @since 1.0.0
     */
    public boolean clearsExisting() {
        return this == REPLACE;
    }

    /**
     * Checks if this mode preserves existing items.
     *
     * @return true if existing items are preserved
     * @since 1.0.0
     */
    public boolean preservesExisting() {
        return this == MERGE || this == MERGE_STACK || this == SAFE;
    }

    /**
     * Checks if this mode stacks with existing items.
     *
     * @return true if items are stacked with existing similar items
     * @since 1.0.0
     */
    public boolean stacksWithExisting() {
        return this == MERGE_STACK;
    }

    /**
     * Checks if this mode requires an empty inventory.
     *
     * @return true if application requires empty inventory
     * @since 1.0.0
     */
    public boolean requiresEmpty() {
        return this == SAFE;
    }
}
