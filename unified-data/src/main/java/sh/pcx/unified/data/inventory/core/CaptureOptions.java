/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.core;

import org.jetbrains.annotations.NotNull;

/**
 * Configuration options for capturing inventory snapshots.
 *
 * <p>CaptureOptions allows fine-grained control over what parts of a player's
 * inventory are included in a snapshot. By default, all main inventory, armor,
 * and offhand slots are captured, but ender chest is excluded.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Capture everything including ender chest
 * CaptureOptions fullCapture = CaptureOptions.builder()
 *     .includeEnderChest(true)
 *     .build();
 *
 * // Capture only main inventory
 * CaptureOptions mainOnly = CaptureOptions.builder()
 *     .includeArmor(false)
 *     .includeOffhand(false)
 *     .build();
 *
 * // Use default options
 * CaptureOptions defaults = CaptureOptions.defaults();
 *
 * // Use in capture
 * InventorySnapshot snapshot = inventoryService.capture(player, fullCapture);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventoryService#capture(sh.pcx.unified.player.UnifiedPlayer, CaptureOptions)
 */
public final class CaptureOptions {

    private static final CaptureOptions DEFAULTS = builder().build();
    private static final CaptureOptions FULL = builder()
        .includeEnderChest(true)
        .build();

    private final boolean includeArmor;
    private final boolean includeOffhand;
    private final boolean includeEnderChest;
    private final boolean cloneItems;
    private final int[] specificSlots;

    private CaptureOptions(Builder builder) {
        this.includeArmor = builder.includeArmor;
        this.includeOffhand = builder.includeOffhand;
        this.includeEnderChest = builder.includeEnderChest;
        this.cloneItems = builder.cloneItems;
        this.specificSlots = builder.specificSlots;
    }

    /**
     * Returns the default capture options.
     *
     * <p>Default options include armor and offhand, but exclude ender chest.
     *
     * @return default capture options
     * @since 1.0.0
     */
    @NotNull
    public static CaptureOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns capture options for a full inventory capture.
     *
     * <p>Includes all inventory sections including ender chest.
     *
     * @return full capture options
     * @since 1.0.0
     */
    @NotNull
    public static CaptureOptions full() {
        return FULL;
    }

    /**
     * Creates a new builder for CaptureOptions.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether armor slots should be captured.
     *
     * @return true if armor is included
     * @since 1.0.0
     */
    public boolean includeArmor() {
        return includeArmor;
    }

    /**
     * Returns whether the offhand slot should be captured.
     *
     * @return true if offhand is included
     * @since 1.0.0
     */
    public boolean includeOffhand() {
        return includeOffhand;
    }

    /**
     * Returns whether ender chest contents should be captured.
     *
     * @return true if ender chest is included
     * @since 1.0.0
     */
    public boolean includeEnderChest() {
        return includeEnderChest;
    }

    /**
     * Returns whether items should be cloned during capture.
     *
     * <p>Cloning ensures the snapshot is completely independent of the
     * original inventory, but is slightly slower.
     *
     * @return true if items are cloned
     * @since 1.0.0
     */
    public boolean cloneItems() {
        return cloneItems;
    }

    /**
     * Returns specific slots to capture, if configured.
     *
     * <p>If null, all slots in the selected categories are captured.
     *
     * @return array of slot indices to capture, or null for all slots
     * @since 1.0.0
     */
    public int[] getSpecificSlots() {
        return specificSlots != null ? specificSlots.clone() : null;
    }

    /**
     * Checks if only specific slots should be captured.
     *
     * @return true if specific slots are configured
     * @since 1.0.0
     */
    public boolean hasSpecificSlots() {
        return specificSlots != null && specificSlots.length > 0;
    }

    @Override
    public String toString() {
        return "CaptureOptions{" +
            "armor=" + includeArmor +
            ", offhand=" + includeOffhand +
            ", enderChest=" + includeEnderChest +
            ", cloneItems=" + cloneItems +
            ", specificSlots=" + (specificSlots != null ? specificSlots.length : "all") +
            '}';
    }

    /**
     * Builder for CaptureOptions.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private boolean includeArmor = true;
        private boolean includeOffhand = true;
        private boolean includeEnderChest = false;
        private boolean cloneItems = true;
        private int[] specificSlots = null;

        private Builder() {}

        /**
         * Sets whether to include armor slots.
         *
         * @param include true to include armor
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder includeArmor(boolean include) {
            this.includeArmor = include;
            return this;
        }

        /**
         * Sets whether to include the offhand slot.
         *
         * @param include true to include offhand
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder includeOffhand(boolean include) {
            this.includeOffhand = include;
            return this;
        }

        /**
         * Sets whether to include ender chest contents.
         *
         * @param include true to include ender chest
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder includeEnderChest(boolean include) {
            this.includeEnderChest = include;
            return this;
        }

        /**
         * Sets whether to clone items during capture.
         *
         * @param clone true to clone items
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder cloneItems(boolean clone) {
            this.cloneItems = clone;
            return this;
        }

        /**
         * Sets specific slots to capture.
         *
         * <p>When set, only these main inventory slots are captured.
         * Armor, offhand, and ender chest settings still apply separately.
         *
         * @param slots the slot indices to capture
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder specificSlots(int... slots) {
            this.specificSlots = slots != null ? slots.clone() : null;
            return this;
        }

        /**
         * Sets whether to capture only the hotbar (slots 0-8).
         *
         * @param hotbarOnly true to capture only hotbar
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder hotbarOnly(boolean hotbarOnly) {
            if (hotbarOnly) {
                this.specificSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
            } else {
                this.specificSlots = null;
            }
            return this;
        }

        /**
         * Builds the CaptureOptions.
         *
         * @return the configured CaptureOptions
         * @since 1.0.0
         */
        @NotNull
        public CaptureOptions build() {
            return new CaptureOptions(this);
        }
    }
}
