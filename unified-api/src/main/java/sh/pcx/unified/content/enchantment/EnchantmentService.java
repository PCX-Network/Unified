/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.enchantment;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for creating and managing custom enchantments.
 *
 * <p>EnchantmentService provides a fluent API for defining custom enchantments
 * with triggers, effects, level scaling, and full integration into Minecraft's
 * enchanting system including enchanting tables, anvils, and loot generation.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Custom Registration</b> - Define new enchantments with unique behavior</li>
 *   <li><b>Trigger System</b> - On hit, on damage, on mine, on use, etc.</li>
 *   <li><b>Level Scaling</b> - Configurable effects per level</li>
 *   <li><b>Target Restrictions</b> - Limit to specific item types</li>
 *   <li><b>Conflict System</b> - Define incompatible enchantments</li>
 *   <li><b>Enchanting Table Integration</b> - Custom enchantments in vanilla tables</li>
 *   <li><b>Persistence</b> - Survives restarts and item serialization</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private EnchantmentService enchantments;
 *
 * // Register a custom enchantment
 * CustomEnchantment lifesteal = enchantments.register("myplugin:lifesteal")
 *     .displayName(Component.text("Lifesteal", NamedTextColor.RED))
 *     .description("Heal for a percentage of damage dealt")
 *     .maxLevel(5)
 *     .rarity(EnchantmentRarity.RARE)
 *     .target(EnchantmentTarget.WEAPON)
 *     .treasure(false)
 *     .conflictsWith("myplugin:vampirism", "minecraft:mending")
 *     .onHit((event, level) -> {
 *         double heal = event.getDamage() * (0.05 * level);
 *         Player attacker = (Player) event.getDamager();
 *         attacker.heal(heal);
 *     })
 *     .register();
 *
 * // Apply to item
 * enchantments.apply(sword, lifesteal, 3);
 *
 * // Check if item has enchantment
 * int level = enchantments.getLevel(sword, lifesteal);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomEnchantment
 * @see EnchantmentBuilder
 */
public interface EnchantmentService extends Service {

    /**
     * Creates a new enchantment builder with the specified key.
     *
     * <p>The key should be a namespaced identifier (e.g., "myplugin:lifesteal").
     *
     * @param key the unique enchantment key
     * @return a new EnchantmentBuilder
     * @throws NullPointerException     if key is null
     * @throws IllegalArgumentException if key format is invalid
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder register(@NotNull String key);

    /**
     * Unregisters a custom enchantment.
     *
     * <p>After unregistering, the enchantment will no longer be processed
     * or appear in enchanting tables. Existing items retain the enchantment
     * data but effects will not trigger.
     *
     * @param key the enchantment key to unregister
     * @return true if the enchantment was unregistered, false if not found
     * @since 1.0.0
     */
    boolean unregister(@NotNull String key);

    /**
     * Gets a registered custom enchantment by its key.
     *
     * @param key the enchantment key
     * @return an Optional containing the enchantment if found
     * @since 1.0.0
     */
    @NotNull
    Optional<CustomEnchantment> get(@NotNull String key);

    /**
     * Returns all registered custom enchantments.
     *
     * @return an unmodifiable collection of all custom enchantments
     * @since 1.0.0
     */
    @NotNull
    Collection<CustomEnchantment> getAll();

    /**
     * Checks if an enchantment with the given key is registered.
     *
     * @param key the enchantment key
     * @return true if the enchantment is registered
     * @since 1.0.0
     */
    boolean isRegistered(@NotNull String key);

    /**
     * Applies a custom enchantment to an item.
     *
     * @param item        the item to enchant
     * @param enchantment the enchantment to apply
     * @param level       the enchantment level
     * @throws IllegalArgumentException if level exceeds max level
     * @since 1.0.0
     */
    void apply(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment, int level);

    /**
     * Applies a custom enchantment to an item by key.
     *
     * @param item the item to enchant
     * @param key  the enchantment key
     * @param level the enchantment level
     * @return true if applied successfully, false if enchantment not found
     * @since 1.0.0
     */
    boolean apply(@NotNull UnifiedItemStack item, @NotNull String key, int level);

    /**
     * Removes a custom enchantment from an item.
     *
     * @param item        the item to modify
     * @param enchantment the enchantment to remove
     * @return true if the enchantment was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment);

    /**
     * Removes a custom enchantment from an item by key.
     *
     * @param item the item to modify
     * @param key  the enchantment key
     * @return true if the enchantment was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UnifiedItemStack item, @NotNull String key);

    /**
     * Checks if an item has a specific custom enchantment.
     *
     * @param item        the item to check
     * @param enchantment the enchantment to check for
     * @return true if the item has the enchantment
     * @since 1.0.0
     */
    boolean has(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment);

    /**
     * Checks if an item has a specific custom enchantment by key.
     *
     * @param item the item to check
     * @param key  the enchantment key
     * @return true if the item has the enchantment
     * @since 1.0.0
     */
    boolean has(@NotNull UnifiedItemStack item, @NotNull String key);

    /**
     * Gets the level of a custom enchantment on an item.
     *
     * @param item        the item to check
     * @param enchantment the enchantment to check
     * @return the enchantment level, or 0 if not present
     * @since 1.0.0
     */
    int getLevel(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment);

    /**
     * Gets the level of a custom enchantment on an item by key.
     *
     * @param item the item to check
     * @param key  the enchantment key
     * @return the enchantment level, or 0 if not present
     * @since 1.0.0
     */
    int getLevel(@NotNull UnifiedItemStack item, @NotNull String key);

    /**
     * Gets all custom enchantments on an item.
     *
     * @param item the item to check
     * @return a map of enchantment to level
     * @since 1.0.0
     */
    @NotNull
    Map<CustomEnchantment, Integer> getCustomEnchantments(@NotNull UnifiedItemStack item);

    /**
     * Clears all custom enchantments from an item.
     *
     * @param item the item to clear
     * @return the number of enchantments removed
     * @since 1.0.0
     */
    int clearCustomEnchantments(@NotNull UnifiedItemStack item);

    /**
     * Checks if a player has an enchantment on cooldown.
     *
     * @param playerId    the player's UUID
     * @param enchantment the enchantment
     * @return true if on cooldown
     * @since 1.0.0
     */
    boolean isOnCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment);

    /**
     * Gets the remaining cooldown time in milliseconds.
     *
     * @param playerId    the player's UUID
     * @param enchantment the enchantment
     * @return remaining cooldown in milliseconds, or 0 if not on cooldown
     * @since 1.0.0
     */
    long getRemainingCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment);

    /**
     * Sets an enchantment on cooldown for a player.
     *
     * @param playerId    the player's UUID
     * @param enchantment the enchantment
     * @since 1.0.0
     */
    void setCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment);

    /**
     * Clears an enchantment cooldown for a player.
     *
     * @param playerId    the player's UUID
     * @param enchantment the enchantment
     * @since 1.0.0
     */
    void clearCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment);
}
