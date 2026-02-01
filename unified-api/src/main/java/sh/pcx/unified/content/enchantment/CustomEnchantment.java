/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.enchantment;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Represents a custom enchantment registered with the API.
 *
 * <p>CustomEnchantment encapsulates all properties and behavior of a custom
 * enchantment, including display properties, targeting rules, effects, and
 * integration settings for the Minecraft enchanting system.
 *
 * <h2>Enchantment Properties</h2>
 * <ul>
 *   <li><b>Key</b> - Unique namespaced identifier</li>
 *   <li><b>Display Name</b> - Name shown in item lore</li>
 *   <li><b>Description</b> - Optional description text</li>
 *   <li><b>Max Level</b> - Maximum enchantment level</li>
 *   <li><b>Rarity</b> - How rare the enchantment is</li>
 *   <li><b>Target</b> - Which items can have this enchantment</li>
 *   <li><b>Conflicts</b> - Enchantments that cannot coexist</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EnchantmentService
 * @see EnchantmentBuilder
 */
public interface CustomEnchantment {

    /**
     * Returns the unique key for this enchantment.
     *
     * @return the namespaced key (e.g., "myplugin:lifesteal")
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Returns the display name of this enchantment.
     *
     * <p>This is shown in item lore when the enchantment is applied.
     *
     * @return the display name component
     * @since 1.0.0
     */
    @NotNull
    Component getDisplayName();

    /**
     * Returns the display name for a specific level.
     *
     * <p>If a level-based name function was provided, returns the
     * appropriate name for the level. Otherwise, returns the base name.
     *
     * @param level the enchantment level
     * @return the display name component for this level
     * @since 1.0.0
     */
    @NotNull
    Component getDisplayName(int level);

    /**
     * Returns the description of this enchantment.
     *
     * @return an Optional containing the description if set
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getDescription();

    /**
     * Returns the maximum level for this enchantment.
     *
     * @return the max level (minimum 1)
     * @since 1.0.0
     */
    int getMaxLevel();

    /**
     * Returns the starting level for this enchantment.
     *
     * @return the starting level (usually 1)
     * @since 1.0.0
     */
    int getStartLevel();

    /**
     * Returns the rarity of this enchantment.
     *
     * @return the enchantment rarity
     * @since 1.0.0
     */
    @NotNull
    EnchantmentRarity getRarity();

    /**
     * Returns the target for this enchantment.
     *
     * @return the enchantment target defining valid item types
     * @since 1.0.0
     */
    @NotNull
    EnchantmentTarget getTarget();

    /**
     * Checks if this enchantment can be applied to the specified item type.
     *
     * @param itemType the item type ID
     * @return true if this enchantment can be applied
     * @since 1.0.0
     */
    boolean canEnchantItem(@NotNull String itemType);

    /**
     * Returns the set of enchantment keys that conflict with this one.
     *
     * @return an unmodifiable set of conflicting enchantment keys
     * @since 1.0.0
     */
    @NotNull
    Set<String> getConflicts();

    /**
     * Checks if this enchantment conflicts with another.
     *
     * @param otherKey the other enchantment key
     * @return true if they conflict
     * @since 1.0.0
     */
    boolean conflictsWith(@NotNull String otherKey);

    /**
     * Checks if this is a treasure enchantment.
     *
     * <p>Treasure enchantments cannot be obtained from enchanting tables
     * but can appear in loot and trades.
     *
     * @return true if this is a treasure enchantment
     * @since 1.0.0
     */
    boolean isTreasure();

    /**
     * Checks if this enchantment is tradeable by villagers.
     *
     * @return true if villagers can offer this enchantment
     * @since 1.0.0
     */
    boolean isTradeable();

    /**
     * Checks if this enchantment can be discovered in loot.
     *
     * @return true if this enchantment can appear in loot tables
     * @since 1.0.0
     */
    boolean isDiscoverable();

    /**
     * Checks if this enchantment is a curse.
     *
     * <p>Curses have negative effects and cannot be removed by grindstone.
     *
     * @return true if this is a curse enchantment
     * @since 1.0.0
     */
    boolean isCurse();

    /**
     * Returns the cooldown duration between activations.
     *
     * @return an Optional containing the cooldown if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Duration> getCooldown();

    /**
     * Returns the activation chance function.
     *
     * @return a function that takes level and returns activation chance (0.0-1.0)
     * @since 1.0.0
     */
    @NotNull
    IntFunction<Double> getChanceFunction();

    /**
     * Returns the activation chance for a specific level.
     *
     * @param level the enchantment level
     * @return the activation chance (0.0-1.0)
     * @since 1.0.0
     */
    double getChance(int level);

    /**
     * Returns the tick interval for passive effects.
     *
     * @return an Optional containing the tick interval if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Integer> getTickInterval();

    /**
     * Formats the enchantment name for display in item lore.
     *
     * @param level the enchantment level
     * @return the formatted lore component
     * @since 1.0.0
     */
    @NotNull
    Component formatLore(int level);

    /**
     * Converts a level to Roman numerals.
     *
     * @param level the level to convert
     * @return the Roman numeral string (e.g., "III" for 3)
     * @since 1.0.0
     */
    @NotNull
    static String toRoman(int level) {
        if (level <= 0) return "";
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }
}
