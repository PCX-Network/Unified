/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.enchantment;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Defines which items can receive a specific enchantment.
 *
 * <p>EnchantmentTarget specifies the valid item types for an enchantment,
 * supporting both predefined categories and custom item sets.
 *
 * <h2>Predefined Targets</h2>
 * <ul>
 *   <li>{@link #ALL} - Any item</li>
 *   <li>{@link #WEAPON} - Swords and axes</li>
 *   <li>{@link #SWORD} - Swords only</li>
 *   <li>{@link #TOOL} - Pickaxes, axes, shovels, hoes</li>
 *   <li>{@link #ARMOR} - All armor pieces</li>
 *   <li>{@link #ARMOR_HEAD} - Helmets</li>
 *   <li>{@link #ARMOR_CHEST} - Chestplates</li>
 *   <li>{@link #ARMOR_LEGS} - Leggings</li>
 *   <li>{@link #ARMOR_FEET} - Boots</li>
 *   <li>{@link #BOW} - Bows</li>
 *   <li>{@link #CROSSBOW} - Crossbows</li>
 *   <li>{@link #TRIDENT} - Tridents</li>
 *   <li>{@link #FISHING_ROD} - Fishing rods</li>
 *   <li>{@link #BREAKABLE} - Any breakable item</li>
 *   <li>{@link #WEARABLE} - Any wearable item</li>
 * </ul>
 *
 * <h2>Custom Targets</h2>
 * <pre>{@code
 * EnchantmentTarget custom = EnchantmentTarget.of(
 *     "minecraft:shield",
 *     "minecraft:elytra",
 *     "minecraft:carved_pumpkin"
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomEnchantment
 */
public sealed interface EnchantmentTarget permits
        EnchantmentTarget.PredefinedTarget,
        EnchantmentTarget.CustomTarget {

    // Predefined targets
    EnchantmentTarget ALL = PredefinedTarget.ALL;
    EnchantmentTarget WEAPON = PredefinedTarget.WEAPON;
    EnchantmentTarget SWORD = PredefinedTarget.SWORD;
    EnchantmentTarget TOOL = PredefinedTarget.TOOL;
    EnchantmentTarget ARMOR = PredefinedTarget.ARMOR;
    EnchantmentTarget ARMOR_HEAD = PredefinedTarget.ARMOR_HEAD;
    EnchantmentTarget ARMOR_CHEST = PredefinedTarget.ARMOR_CHEST;
    EnchantmentTarget ARMOR_LEGS = PredefinedTarget.ARMOR_LEGS;
    EnchantmentTarget ARMOR_FEET = PredefinedTarget.ARMOR_FEET;
    EnchantmentTarget BOW = PredefinedTarget.BOW;
    EnchantmentTarget CROSSBOW = PredefinedTarget.CROSSBOW;
    EnchantmentTarget TRIDENT = PredefinedTarget.TRIDENT;
    EnchantmentTarget FISHING_ROD = PredefinedTarget.FISHING_ROD;
    EnchantmentTarget BREAKABLE = PredefinedTarget.BREAKABLE;
    EnchantmentTarget WEARABLE = PredefinedTarget.WEARABLE;

    /**
     * Tests if this target includes the specified item type.
     *
     * @param itemType the item type ID
     * @return true if the item type is valid for this target
     * @since 1.0.0
     */
    boolean includes(@NotNull String itemType);

    /**
     * Returns the name identifier for this target.
     *
     * @return the target name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Creates a custom target from specific item types.
     *
     * @param itemTypes the item type IDs
     * @return a new EnchantmentTarget
     * @since 1.0.0
     */
    @NotNull
    static EnchantmentTarget of(@NotNull String... itemTypes) {
        return new CustomTarget(Set.of(itemTypes));
    }

    /**
     * Creates a custom target from a set of item types.
     *
     * @param itemTypes the item type IDs
     * @return a new EnchantmentTarget
     * @since 1.0.0
     */
    @NotNull
    static EnchantmentTarget of(@NotNull Set<String> itemTypes) {
        return new CustomTarget(itemTypes);
    }

    /**
     * Creates a target using a predicate function.
     *
     * @param name      the target name
     * @param predicate the predicate to test item types
     * @return a new EnchantmentTarget
     * @since 1.0.0
     */
    @NotNull
    static EnchantmentTarget matching(@NotNull String name, @NotNull Predicate<String> predicate) {
        return new CustomTarget(name, predicate);
    }

    /**
     * Predefined enchantment target categories.
     */
    enum PredefinedTarget implements EnchantmentTarget {
        ALL("all", _ -> true),

        WEAPON("weapon", type ->
                type.endsWith("_sword") || type.endsWith("_axe")),

        SWORD("sword", type ->
                type.endsWith("_sword")),

        TOOL("tool", type ->
                type.endsWith("_pickaxe") ||
                type.endsWith("_axe") ||
                type.endsWith("_shovel") ||
                type.endsWith("_hoe")),

        ARMOR("armor", type ->
                type.endsWith("_helmet") ||
                type.endsWith("_chestplate") ||
                type.endsWith("_leggings") ||
                type.endsWith("_boots")),

        ARMOR_HEAD("armor_head", type ->
                type.endsWith("_helmet") || type.equals("minecraft:turtle_helmet")),

        ARMOR_CHEST("armor_chest", type ->
                type.endsWith("_chestplate") || type.equals("minecraft:elytra")),

        ARMOR_LEGS("armor_legs", type ->
                type.endsWith("_leggings")),

        ARMOR_FEET("armor_feet", type ->
                type.endsWith("_boots")),

        BOW("bow", type ->
                type.equals("minecraft:bow")),

        CROSSBOW("crossbow", type ->
                type.equals("minecraft:crossbow")),

        TRIDENT("trident", type ->
                type.equals("minecraft:trident")),

        FISHING_ROD("fishing_rod", type ->
                type.equals("minecraft:fishing_rod")),

        BREAKABLE("breakable", type ->
                // Most tools, weapons, and armor are breakable
                type.endsWith("_sword") ||
                type.endsWith("_axe") ||
                type.endsWith("_pickaxe") ||
                type.endsWith("_shovel") ||
                type.endsWith("_hoe") ||
                type.endsWith("_helmet") ||
                type.endsWith("_chestplate") ||
                type.endsWith("_leggings") ||
                type.endsWith("_boots") ||
                type.equals("minecraft:bow") ||
                type.equals("minecraft:crossbow") ||
                type.equals("minecraft:trident") ||
                type.equals("minecraft:fishing_rod") ||
                type.equals("minecraft:shield") ||
                type.equals("minecraft:elytra") ||
                type.equals("minecraft:flint_and_steel") ||
                type.equals("minecraft:shears") ||
                type.equals("minecraft:brush")),

        WEARABLE("wearable", type ->
                type.endsWith("_helmet") ||
                type.endsWith("_chestplate") ||
                type.endsWith("_leggings") ||
                type.endsWith("_boots") ||
                type.equals("minecraft:elytra") ||
                type.equals("minecraft:carved_pumpkin") ||
                type.endsWith("_head") ||
                type.endsWith("_skull"));

        private final String name;
        private final Predicate<String> predicate;

        PredefinedTarget(String name, Predicate<String> predicate) {
            this.name = name;
            this.predicate = predicate;
        }

        @Override
        public boolean includes(@NotNull String itemType) {
            return predicate.test(itemType.toLowerCase());
        }

        @Override
        @NotNull
        public String getName() {
            return name;
        }
    }

    /**
     * Custom enchantment target with specific item types or predicate.
     */
    record CustomTarget(
            @NotNull String name,
            @NotNull Predicate<String> predicate,
            @NotNull Set<String> explicitTypes
    ) implements EnchantmentTarget {

        public CustomTarget(@NotNull Set<String> itemTypes) {
            this("custom", itemTypes::contains, Collections.unmodifiableSet(new HashSet<>(itemTypes)));
        }

        public CustomTarget(@NotNull String name, @NotNull Predicate<String> predicate) {
            this(name, predicate, Set.of());
        }

        @Override
        public boolean includes(@NotNull String itemType) {
            return predicate.test(itemType.toLowerCase());
        }

        @Override
        @NotNull
        public String getName() {
            return name;
        }
    }
}
