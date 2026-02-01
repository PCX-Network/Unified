/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.enchantment;

/**
 * Enumeration of enchantment rarity levels.
 *
 * <p>Rarity affects the likelihood of an enchantment appearing in enchanting
 * tables, villager trades, and loot generation. Higher rarity enchantments
 * appear less frequently.
 *
 * <h2>Rarity Levels</h2>
 * <ul>
 *   <li>{@link #COMMON} - Frequently appears, low enchanting cost</li>
 *   <li>{@link #UNCOMMON} - Moderately common, medium cost</li>
 *   <li>{@link #RARE} - Harder to find, higher cost</li>
 *   <li>{@link #VERY_RARE} - Very difficult to obtain</li>
 *   <li>{@link #LEGENDARY} - Extremely rare, custom tier</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomEnchantment
 */
public enum EnchantmentRarity {

    /**
     * Common rarity - frequently appears with low cost.
     */
    COMMON(10, 1),

    /**
     * Uncommon rarity - moderately common with medium cost.
     */
    UNCOMMON(5, 2),

    /**
     * Rare rarity - harder to find with higher cost.
     */
    RARE(2, 4),

    /**
     * Very rare rarity - very difficult to obtain.
     */
    VERY_RARE(1, 8),

    /**
     * Legendary rarity - extremely rare, custom tier.
     */
    LEGENDARY(1, 16);

    private final int weight;
    private final int anvilCostMultiplier;

    EnchantmentRarity(int weight, int anvilCostMultiplier) {
        this.weight = weight;
        this.anvilCostMultiplier = anvilCostMultiplier;
    }

    /**
     * Returns the weight used in enchantment selection.
     *
     * <p>Higher weight means more likely to appear.
     *
     * @return the selection weight
     * @since 1.0.0
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Returns the anvil cost multiplier.
     *
     * <p>Used to calculate the XP cost when applying via anvil.
     *
     * @return the anvil cost multiplier
     * @since 1.0.0
     */
    public int getAnvilCostMultiplier() {
        return anvilCostMultiplier;
    }

    /**
     * Parses a rarity from a string name.
     *
     * @param name the rarity name (case-insensitive)
     * @return the corresponding EnchantmentRarity
     * @throws IllegalArgumentException if not recognized
     * @since 1.0.0
     */
    public static EnchantmentRarity fromName(String name) {
        for (EnchantmentRarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(name)) {
                return rarity;
            }
        }
        throw new IllegalArgumentException("Unknown enchantment rarity: " + name);
    }
}
