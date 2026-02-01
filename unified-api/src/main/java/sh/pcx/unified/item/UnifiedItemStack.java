/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.item;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Platform-agnostic interface representing an item stack.
 *
 * <p>This interface provides access to item properties, metadata, and modifications.
 * It abstracts the differences between Bukkit's ItemStack and Sponge's ItemStack.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get item from player
 * UnifiedItemStack item = player.getItemInMainHand();
 *
 * // Check item properties
 * String type = item.getType();
 * int amount = item.getAmount();
 * boolean hasMeta = item.hasDisplayName();
 *
 * // Get display properties
 * Optional<Component> name = item.getDisplayName();
 * List<Component> lore = item.getLore();
 *
 * // Get enchantments
 * Map<String, Integer> enchants = item.getEnchantments();
 * int sharpnessLevel = item.getEnchantmentLevel("minecraft:sharpness");
 *
 * // Check durability
 * if (item.hasDurability()) {
 *     int damage = item.getDamage();
 *     int maxDamage = item.getMaxDamage();
 * }
 *
 * // Create a modified copy
 * UnifiedItemStack modified = item.withAmount(64);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>ItemStack instances are NOT thread-safe. Create copies when passing
 * between threads or use {@link #clone()}.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ItemBuilder
 */
public interface UnifiedItemStack {

    /**
     * Returns the item type as a namespaced ID.
     *
     * <p>Examples: "minecraft:diamond_sword", "minecraft:oak_log"
     *
     * @return the item type ID
     * @since 1.0.0
     */
    @NotNull
    String getType();

    /**
     * Returns the stack amount.
     *
     * @return the number of items in the stack
     * @since 1.0.0
     */
    int getAmount();

    /**
     * Sets the stack amount.
     *
     * @param amount the new amount
     * @throws IllegalArgumentException if amount is negative
     * @since 1.0.0
     */
    void setAmount(int amount);

    /**
     * Returns a copy with a different amount.
     *
     * @param amount the new amount
     * @return a new ItemStack with the specified amount
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack withAmount(int amount);

    /**
     * Returns the maximum stack size for this item type.
     *
     * @return the maximum stack size
     * @since 1.0.0
     */
    int getMaxStackSize();

    /**
     * Checks if this item stack is empty (air or amount 0).
     *
     * @return true if the stack is empty
     * @since 1.0.0
     */
    boolean isEmpty();

    /**
     * Checks if this item is similar to another (same type and metadata).
     *
     * <p>Amount is not considered in similarity checks.
     *
     * @param other the item to compare with
     * @return true if the items are similar
     * @since 1.0.0
     */
    boolean isSimilar(@NotNull UnifiedItemStack other);

    /**
     * Returns the display name of this item.
     *
     * @return an Optional containing the display name if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Component> getDisplayName();

    /**
     * Checks if this item has a custom display name.
     *
     * @return true if a display name is set
     * @since 1.0.0
     */
    boolean hasDisplayName();

    /**
     * Sets the display name of this item.
     *
     * @param name the new display name, or null to remove
     * @since 1.0.0
     */
    void setDisplayName(@Nullable Component name);

    /**
     * Returns the lore (description) of this item.
     *
     * @return the lore lines, empty list if none
     * @since 1.0.0
     */
    @NotNull
    List<Component> getLore();

    /**
     * Checks if this item has lore.
     *
     * @return true if lore is set
     * @since 1.0.0
     */
    boolean hasLore();

    /**
     * Sets the lore of this item.
     *
     * @param lore the new lore lines, or null to remove
     * @since 1.0.0
     */
    void setLore(@Nullable List<Component> lore);

    /**
     * Adds a line to the lore.
     *
     * @param line the lore line to add
     * @since 1.0.0
     */
    void addLoreLine(@NotNull Component line);

    /**
     * Returns all enchantments on this item.
     *
     * @return a map of enchantment ID to level
     * @since 1.0.0
     */
    @NotNull
    Map<String, Integer> getEnchantments();

    /**
     * Checks if this item has any enchantments.
     *
     * @return true if the item is enchanted
     * @since 1.0.0
     */
    boolean hasEnchantments();

    /**
     * Checks if this item has a specific enchantment.
     *
     * @param enchantment the enchantment ID
     * @return true if the enchantment is present
     * @since 1.0.0
     */
    boolean hasEnchantment(@NotNull String enchantment);

    /**
     * Returns the level of a specific enchantment.
     *
     * @param enchantment the enchantment ID
     * @return the enchantment level, or 0 if not present
     * @since 1.0.0
     */
    int getEnchantmentLevel(@NotNull String enchantment);

    /**
     * Adds an enchantment to this item.
     *
     * @param enchantment the enchantment ID
     * @param level       the enchantment level
     * @since 1.0.0
     */
    void addEnchantment(@NotNull String enchantment, int level);

    /**
     * Removes an enchantment from this item.
     *
     * @param enchantment the enchantment ID
     * @return true if the enchantment was removed
     * @since 1.0.0
     */
    boolean removeEnchantment(@NotNull String enchantment);

    /**
     * Checks if this item type has durability.
     *
     * @return true if the item has durability
     * @since 1.0.0
     */
    boolean hasDurability();

    /**
     * Returns the current damage value.
     *
     * @return the damage (0 = full durability)
     * @since 1.0.0
     */
    int getDamage();

    /**
     * Sets the damage value.
     *
     * @param damage the new damage value
     * @since 1.0.0
     */
    void setDamage(int damage);

    /**
     * Returns the maximum damage before the item breaks.
     *
     * @return the maximum damage
     * @since 1.0.0
     */
    int getMaxDamage();

    /**
     * Returns the remaining durability.
     *
     * @return the remaining durability (maxDamage - damage)
     * @since 1.0.0
     */
    default int getRemainingDurability() {
        return getMaxDamage() - getDamage();
    }

    /**
     * Checks if this item is unbreakable.
     *
     * @return true if the item is unbreakable
     * @since 1.0.0
     */
    boolean isUnbreakable();

    /**
     * Sets whether this item is unbreakable.
     *
     * @param unbreakable true to make unbreakable
     * @since 1.0.0
     */
    void setUnbreakable(boolean unbreakable);

    /**
     * Returns the custom model data value.
     *
     * @return an Optional containing the custom model data if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Integer> getCustomModelData();

    /**
     * Sets the custom model data value.
     *
     * @param customModelData the custom model data, or null to remove
     * @since 1.0.0
     */
    void setCustomModelData(@Nullable Integer customModelData);

    /**
     * Checks if this item has custom model data.
     *
     * @return true if custom model data is set
     * @since 1.0.0
     */
    boolean hasCustomModelData();

    /**
     * Returns item flags that are hidden.
     *
     * @return a list of hidden flag names
     * @since 1.0.0
     */
    @NotNull
    List<String> getHiddenFlags();

    /**
     * Adds a hidden item flag.
     *
     * @param flag the flag name
     * @since 1.0.0
     */
    void addHiddenFlag(@NotNull String flag);

    /**
     * Removes a hidden item flag.
     *
     * @param flag the flag name
     * @since 1.0.0
     */
    void removeHiddenFlag(@NotNull String flag);

    /**
     * Checks if a flag is hidden.
     *
     * @param flag the flag name
     * @return true if the flag is hidden
     * @since 1.0.0
     */
    boolean hasHiddenFlag(@NotNull String flag);

    /**
     * Gets persistent data stored on this item.
     *
     * @param key  the data key
     * @param type the expected data type
     * @param <T>  the data type
     * @return an Optional containing the data if present
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getPersistentData(@NotNull String key, @NotNull Class<T> type);

    /**
     * Sets persistent data on this item.
     *
     * @param key   the data key
     * @param value the value to store
     * @param <T>   the data type
     * @since 1.0.0
     */
    <T> void setPersistentData(@NotNull String key, @NotNull T value);

    /**
     * Removes persistent data from this item.
     *
     * @param key the data key
     * @since 1.0.0
     */
    void removePersistentData(@NotNull String key);

    /**
     * Checks if this item has persistent data for a key.
     *
     * @param key the data key
     * @return true if data is present
     * @since 1.0.0
     */
    boolean hasPersistentData(@NotNull String key);

    /**
     * Serializes this item to a byte array.
     *
     * @return the serialized item data
     * @since 1.0.0
     */
    byte @NotNull [] serialize();

    /**
     * Serializes this item to a Base64 string.
     *
     * @return the Base64-encoded item data
     * @since 1.0.0
     */
    @NotNull
    String toBase64();

    /**
     * Creates a deep copy of this item stack.
     *
     * @return a cloned item stack
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack clone();

    /**
     * Creates an ItemBuilder from this item for modifications.
     *
     * @return a new ItemBuilder based on this item
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder toBuilder();

    /**
     * Returns the underlying platform-specific item object.
     *
     * @param <T> the expected platform item type
     * @return the platform-specific item object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();

    /**
     * Creates an empty item stack (air).
     *
     * @return an empty item stack
     * @since 1.0.0
     */
    @NotNull
    static UnifiedItemStack empty() {
        return ItemBuilder.of("minecraft:air").amount(0).build();
    }
}
