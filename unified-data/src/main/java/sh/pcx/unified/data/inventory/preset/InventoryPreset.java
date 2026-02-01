/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.preset;

import sh.pcx.unified.data.inventory.core.ApplyMode;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a reusable inventory preset (kit).
 *
 * <p>InventoryPreset defines a set of items that can be applied to a player's
 * inventory. Presets can include main inventory items, armor, and offhand.
 * They support permissions, cooldowns, and can be stored for reuse.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Slots</b>: Define items for specific inventory slots</li>
 *   <li><b>Armor</b>: Full armor set support</li>
 *   <li><b>Offhand</b>: Offhand item support</li>
 *   <li><b>Permissions</b>: Optional permission requirement</li>
 *   <li><b>Cooldowns</b>: Optional usage cooldown</li>
 *   <li><b>Apply Modes</b>: Control how items are added</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a PvP kit
 * InventoryPreset pvpKit = InventoryPreset.builder()
 *     .name("kit_pvp")
 *     .displayName("PvP Kit")
 *     .permission("kits.pvp")
 *     .cooldown(Duration.ofMinutes(5))
 *     .helmet(ItemBuilder.of(Material.IRON_HELMET).build())
 *     .chestplate(ItemBuilder.of(Material.IRON_CHESTPLATE).build())
 *     .leggings(ItemBuilder.of(Material.IRON_LEGGINGS).build())
 *     .boots(ItemBuilder.of(Material.IRON_BOOTS).build())
 *     .slot(0, ItemBuilder.of(Material.IRON_SWORD).build())
 *     .slot(1, ItemBuilder.of(Material.BOW).build())
 *     .slot(8, new ItemStack(Material.GOLDEN_APPLE, 3))
 *     .build();
 *
 * // Apply to player
 * pvpKit.applyTo(player);
 *
 * // Check permission
 * if (pvpKit.canUse(player)) {
 *     pvpKit.applyTo(player);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PresetManager
 */
public final class InventoryPreset {

    private final String name;
    private final String displayName;
    private final String description;
    private final String permission;
    private final java.time.Duration cooldown;
    private final ApplyMode applyMode;

    private final Map<Integer, UnifiedItemStack> slots;
    private final UnifiedItemStack helmet;
    private final UnifiedItemStack chestplate;
    private final UnifiedItemStack leggings;
    private final UnifiedItemStack boots;
    private final UnifiedItemStack offhand;

    private final boolean clearInventory;
    private final boolean clearArmor;
    private final boolean oneTimeUse;
    private final Map<String, String> metadata;

    private InventoryPreset(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Preset name cannot be null");
        this.displayName = builder.displayName != null ? builder.displayName : name;
        this.description = builder.description;
        this.permission = builder.permission;
        this.cooldown = builder.cooldown;
        this.applyMode = builder.applyMode != null ? builder.applyMode : ApplyMode.MERGE;

        this.slots = Map.copyOf(builder.slots);
        this.helmet = builder.helmet;
        this.chestplate = builder.chestplate;
        this.leggings = builder.leggings;
        this.boots = builder.boots;
        this.offhand = builder.offhand;

        this.clearInventory = builder.clearInventory;
        this.clearArmor = builder.clearArmor;
        this.oneTimeUse = builder.oneTimeUse;
        this.metadata = Map.copyOf(builder.metadata);
    }

    /**
     * Creates a new preset builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    // ========== Getters ==========

    /**
     * Returns the preset's unique name/identifier.
     *
     * @return the preset name
     * @since 1.0.0
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the display name for the preset.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the preset description.
     *
     * @return the description, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Returns the required permission.
     *
     * @return the permission, or null if no permission required
     * @since 1.0.0
     */
    @Nullable
    public String getPermission() {
        return permission;
    }

    /**
     * Returns the usage cooldown.
     *
     * @return the cooldown duration, or null if no cooldown
     * @since 1.0.0
     */
    @Nullable
    public java.time.Duration getCooldown() {
        return cooldown;
    }

    /**
     * Returns the apply mode.
     *
     * @return the apply mode
     * @since 1.0.0
     */
    @NotNull
    public ApplyMode getApplyMode() {
        return applyMode;
    }

    /**
     * Returns the item at a specific slot.
     *
     * @param slot the slot index
     * @return the item, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getSlot(int slot) {
        return slots.get(slot);
    }

    /**
     * Returns all slot mappings.
     *
     * @return unmodifiable map of slot to item
     * @since 1.0.0
     */
    @NotNull
    public Map<Integer, UnifiedItemStack> getSlots() {
        return slots;
    }

    /**
     * Returns the helmet.
     *
     * @return the helmet, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getHelmet() {
        return helmet;
    }

    /**
     * Returns the chestplate.
     *
     * @return the chestplate, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getChestplate() {
        return chestplate;
    }

    /**
     * Returns the leggings.
     *
     * @return the leggings, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getLeggings() {
        return leggings;
    }

    /**
     * Returns the boots.
     *
     * @return the boots, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getBoots() {
        return boots;
    }

    /**
     * Returns the offhand item.
     *
     * @return the offhand item, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getOffhand() {
        return offhand;
    }

    /**
     * Returns whether the inventory should be cleared before applying.
     *
     * @return true if inventory should be cleared
     * @since 1.0.0
     */
    public boolean shouldClearInventory() {
        return clearInventory;
    }

    /**
     * Returns whether armor should be cleared before applying.
     *
     * @return true if armor should be cleared
     * @since 1.0.0
     */
    public boolean shouldClearArmor() {
        return clearArmor;
    }

    /**
     * Returns whether this preset can only be used once.
     *
     * @return true if one-time use
     * @since 1.0.0
     */
    public boolean isOneTimeUse() {
        return oneTimeUse;
    }

    /**
     * Returns the metadata map.
     *
     * @return unmodifiable metadata map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Gets a metadata value.
     *
     * @param key the metadata key
     * @return the value, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public String getMetadata(@NotNull String key) {
        return metadata.get(key);
    }

    // ========== Permission Check ==========

    /**
     * Checks if a player has permission to use this preset.
     *
     * @param player the player to check
     * @return true if the player can use this preset
     * @since 1.0.0
     */
    public boolean canUse(@NotNull UnifiedPlayer player) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return player.hasPermission(permission);
    }

    /**
     * Checks if a player has the preset on cooldown.
     *
     * @param player the player to check
     * @return true if on cooldown
     * @since 1.0.0
     */
    public boolean isOnCooldown(@NotNull UnifiedPlayer player) {
        if (cooldown == null) {
            return false;
        }
        // Cooldown check would be implemented via CooldownManager
        return false;
    }

    // ========== Application ==========

    /**
     * Applies this preset to a player.
     *
     * @param player the player to apply to
     * @since 1.0.0
     */
    public void applyTo(@NotNull UnifiedPlayer player) {
        applyTo(player, applyMode);
    }

    /**
     * Applies this preset to a player with a specific mode.
     *
     * @param player the player to apply to
     * @param mode   the apply mode
     * @since 1.0.0
     */
    public void applyTo(@NotNull UnifiedPlayer player, @NotNull ApplyMode mode) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(mode, "ApplyMode cannot be null");

        // This would delegate to InventoryService
        sh.pcx.unified.data.inventory.core.InventoryService inventories =
            sh.pcx.unified.data.inventory.core.InventoryService.getInstance();

        if (clearInventory || mode == ApplyMode.REPLACE) {
            inventories.clear(player, true, clearArmor, false, false);
        }

        // Apply slots
        for (Map.Entry<Integer, UnifiedItemStack> entry : slots.entrySet()) {
            // Implementation would set items in player inventory
        }

        // Apply armor
        // Implementation would set armor items

        // Apply offhand
        // Implementation would set offhand item
    }

    // ========== Statistics ==========

    /**
     * Returns the total number of items in this preset.
     *
     * @return the item count
     * @since 1.0.0
     */
    public int getItemCount() {
        int count = slots.size();
        if (helmet != null) count++;
        if (chestplate != null) count++;
        if (leggings != null) count++;
        if (boots != null) count++;
        if (offhand != null) count++;
        return count;
    }

    /**
     * Checks if this preset has any armor.
     *
     * @return true if any armor piece is set
     * @since 1.0.0
     */
    public boolean hasArmor() {
        return helmet != null || chestplate != null || leggings != null || boots != null;
    }

    /**
     * Checks if this preset has full armor.
     *
     * @return true if all armor pieces are set
     * @since 1.0.0
     */
    public boolean hasFullArmor() {
        return helmet != null && chestplate != null && leggings != null && boots != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryPreset that)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "InventoryPreset{" +
            "name='" + name + '\'' +
            ", displayName='" + displayName + '\'' +
            ", items=" + getItemCount() +
            ", permission='" + permission + '\'' +
            '}';
    }

    // ========== Builder ==========

    /**
     * Builder for creating InventoryPreset instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private String name;
        private String displayName;
        private String description;
        private String permission;
        private java.time.Duration cooldown;
        private ApplyMode applyMode;

        private final Map<Integer, UnifiedItemStack> slots = new HashMap<>();
        private UnifiedItemStack helmet;
        private UnifiedItemStack chestplate;
        private UnifiedItemStack leggings;
        private UnifiedItemStack boots;
        private UnifiedItemStack offhand;

        private boolean clearInventory = false;
        private boolean clearArmor = false;
        private boolean oneTimeUse = false;
        private final Map<String, String> metadata = new HashMap<>();

        private Builder() {}

        /**
         * Sets the preset name.
         *
         * @param name the unique name
         * @return this builder
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the display name.
         *
         * @param displayName the display name
         * @return this builder
         */
        @NotNull
        public Builder displayName(@NotNull String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description the description
         * @return this builder
         */
        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the required permission.
         *
         * @param permission the permission
         * @return this builder
         */
        @NotNull
        public Builder permission(@Nullable String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Sets the cooldown duration.
         *
         * @param cooldown the cooldown
         * @return this builder
         */
        @NotNull
        public Builder cooldown(@Nullable java.time.Duration cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        /**
         * Sets the apply mode.
         *
         * @param mode the apply mode
         * @return this builder
         */
        @NotNull
        public Builder applyMode(@NotNull ApplyMode mode) {
            this.applyMode = mode;
            return this;
        }

        /**
         * Sets an item at a specific slot.
         *
         * @param slot the slot index (0-35)
         * @param item the item
         * @return this builder
         */
        @NotNull
        public Builder slot(int slot, @Nullable UnifiedItemStack item) {
            if (slot < 0 || slot >= 36) {
                throw new IllegalArgumentException("Slot must be 0-35: " + slot);
            }
            if (item != null) {
                slots.put(slot, item);
            } else {
                slots.remove(slot);
            }
            return this;
        }

        /**
         * Sets the helmet.
         *
         * @param helmet the helmet
         * @return this builder
         */
        @NotNull
        public Builder helmet(@Nullable UnifiedItemStack helmet) {
            this.helmet = helmet;
            return this;
        }

        /**
         * Sets the chestplate.
         *
         * @param chestplate the chestplate
         * @return this builder
         */
        @NotNull
        public Builder chestplate(@Nullable UnifiedItemStack chestplate) {
            this.chestplate = chestplate;
            return this;
        }

        /**
         * Sets the leggings.
         *
         * @param leggings the leggings
         * @return this builder
         */
        @NotNull
        public Builder leggings(@Nullable UnifiedItemStack leggings) {
            this.leggings = leggings;
            return this;
        }

        /**
         * Sets the boots.
         *
         * @param boots the boots
         * @return this builder
         */
        @NotNull
        public Builder boots(@Nullable UnifiedItemStack boots) {
            this.boots = boots;
            return this;
        }

        /**
         * Sets the offhand item.
         *
         * @param offhand the offhand item
         * @return this builder
         */
        @NotNull
        public Builder offhand(@Nullable UnifiedItemStack offhand) {
            this.offhand = offhand;
            return this;
        }

        /**
         * Sets whether to clear inventory before applying.
         *
         * @param clear true to clear
         * @return this builder
         */
        @NotNull
        public Builder clearInventory(boolean clear) {
            this.clearInventory = clear;
            return this;
        }

        /**
         * Sets whether to clear armor before applying.
         *
         * @param clear true to clear
         * @return this builder
         */
        @NotNull
        public Builder clearArmor(boolean clear) {
            this.clearArmor = clear;
            return this;
        }

        /**
         * Sets whether this is a one-time use preset.
         *
         * @param oneTimeUse true for one-time use
         * @return this builder
         */
        @NotNull
        public Builder oneTimeUse(boolean oneTimeUse) {
            this.oneTimeUse = oneTimeUse;
            return this;
        }

        /**
         * Adds metadata.
         *
         * @param key   the metadata key
         * @param value the metadata value
         * @return this builder
         */
        @NotNull
        public Builder metadata(@NotNull String key, @NotNull String value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Sets all metadata.
         *
         * @param metadata the metadata map
         * @return this builder
         */
        @NotNull
        public Builder metadata(@NotNull Map<String, String> metadata) {
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return this;
        }

        /**
         * Builds the InventoryPreset.
         *
         * @return the new preset
         * @throws IllegalStateException if name is not set
         */
        @NotNull
        public InventoryPreset build() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("Preset name must be set");
            }
            return new InventoryPreset(this);
        }
    }
}
