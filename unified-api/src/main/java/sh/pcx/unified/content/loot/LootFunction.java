/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.content.loot.LootTypes.BonusFormula;
import sh.pcx.unified.content.loot.LootTypes.CountRange;
import sh.pcx.unified.content.loot.LootTypes.DamageRange;
import sh.pcx.unified.content.loot.LootTypes.LevelRange;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Represents a function that modifies items after loot generation.
 *
 * <p>LootFunction allows transforming selected items before they are dropped,
 * such as adding enchantments, setting counts, applying damage, or adding
 * custom data.
 *
 * <h2>Built-in Functions</h2>
 * <ul>
 *   <li>{@link #setCount(CountRange)} - Set item count</li>
 *   <li>{@link #setDamage(DamageRange)} - Set item damage/durability</li>
 *   <li>{@link #enchantRandomly()} - Add random enchantments</li>
 *   <li>{@link #enchantWithLevels(LevelRange)} - Enchant with level range</li>
 *   <li>{@link #lootingEnchant(CountRange)} - Bonus per looting level</li>
 *   <li>{@link #setName(Component)} - Set item display name</li>
 *   <li>{@link #setLore(List)} - Set item lore</li>
 *   <li>{@link #applyBonus(String)} - Apply enchantment bonus (Fortune)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LootEntry.item("minecraft:diamond_sword")
 *     .function(LootFunction.enchantRandomly(3))
 *     .function(LootFunction.setDamage(DamageRange.between(0.5f, 1.0f)))
 *     .function(LootFunction.setName(Component.text("Hero's Blade")))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootEntry
 */
public interface LootFunction {

    /**
     * Applies this function to an item.
     *
     * @param item    the item to modify
     * @param context the loot context
     * @return the modified item
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context);

    /**
     * Returns the function type identifier.
     *
     * @return the function type
     * @since 1.0.0
     */
    @NotNull
    String getType();

    // === Factory Methods ===

    /**
     * Creates a function to set the item count.
     *
     * @param count the count range
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction setCount(@NotNull CountRange count) {
        return new SetCountFunction(count);
    }

    /**
     * Creates a function to set item damage.
     *
     * @param damage the damage range (0.0 = full durability, 1.0 = broken)
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction setDamage(@NotNull DamageRange damage) {
        return new SetDamageFunction(damage);
    }

    /**
     * Creates a function to add random enchantments.
     *
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction enchantRandomly() {
        return new EnchantRandomlyFunction(1, false);
    }

    /**
     * Creates a function to add multiple random enchantments.
     *
     * @param count the number of enchantments to add
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction enchantRandomly(int count) {
        return new EnchantRandomlyFunction(count, false);
    }

    /**
     * Creates a function to add random enchantments including treasure.
     *
     * @param count          the number of enchantments
     * @param includeTreasure whether to include treasure enchantments
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction enchantRandomly(int count, boolean includeTreasure) {
        return new EnchantRandomlyFunction(count, includeTreasure);
    }

    /**
     * Creates a function to enchant with a level range (like enchanting table).
     *
     * @param levels the enchanting level range
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction enchantWithLevels(@NotNull LevelRange levels) {
        return new EnchantWithLevelsFunction(levels, false);
    }

    /**
     * Creates a function to enchant with levels and optionally treasure.
     *
     * @param levels          the enchanting level range
     * @param includeTreasure whether to include treasure enchantments
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction enchantWithLevels(@NotNull LevelRange levels, boolean includeTreasure) {
        return new EnchantWithLevelsFunction(levels, includeTreasure);
    }

    /**
     * Creates a function to add bonus items per looting level.
     *
     * @param countPerLevel the count range added per looting level
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction lootingEnchant(@NotNull CountRange countPerLevel) {
        return new LootingEnchantFunction(countPerLevel, 0);
    }

    /**
     * Creates a function to add bonus items per looting level with a cap.
     *
     * @param countPerLevel the count range per looting level
     * @param limit         the maximum bonus count (0 = no limit)
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction lootingEnchant(@NotNull CountRange countPerLevel, int limit) {
        return new LootingEnchantFunction(countPerLevel, limit);
    }

    /**
     * Creates a function to set the item display name.
     *
     * @param name the display name
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction setName(@NotNull Component name) {
        return new SetNameFunction(name);
    }

    /**
     * Creates a function to set the item lore.
     *
     * @param lore the lore lines
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction setLore(@NotNull List<Component> lore) {
        return new SetLoreFunction(lore);
    }

    /**
     * Creates a function to apply enchantment bonus (like Fortune).
     *
     * @param enchantment the enchantment key
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction applyBonus(@NotNull String enchantment) {
        return new ApplyBonusFunction(enchantment, BonusFormula.UNIFORM);
    }

    /**
     * Creates a function to apply enchantment bonus with a formula.
     *
     * @param enchantment the enchantment key
     * @param formula     the bonus formula
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction applyBonus(@NotNull String enchantment, @NotNull BonusFormula formula) {
        return new ApplyBonusFunction(enchantment, formula);
    }

    /**
     * Creates a custom function with a transformer.
     *
     * @param transformer the item transformer
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static LootFunction custom(@NotNull BiFunction<UnifiedItemStack, LootContext, UnifiedItemStack> transformer) {
        return new CustomFunction(transformer);
    }

    /**
     * Creates a function to add persistent data to the item.
     *
     * @param key   the data key
     * @param value the data value
     * @param <T>   the data type
     * @return the function
     * @since 1.0.0
     */
    @NotNull
    static <T> LootFunction setPersistentData(@NotNull String key, @NotNull T value) {
        return new SetPersistentDataFunction<>(key, value);
    }
}

// === Function Implementations ===

record SetCountFunction(CountRange count) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        int newCount = count.roll(context.getRandom());
        return item.toBuilder().amount(newCount).build();
    }

    @Override
    @NotNull
    public String getType() {
        return "set_count";
    }
}

record SetDamageFunction(DamageRange damage) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        float damagePercent = damage.roll(context.getRandom());
        int maxDamage = item.getMaxDamage();
        int actualDamage = (int) (maxDamage * damagePercent);
        return item.toBuilder().damage(actualDamage).build();
    }

    @Override
    @NotNull
    public String getType() {
        return "set_damage";
    }
}

record EnchantRandomlyFunction(int count, boolean includeTreasure) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        // Delegate to platform implementation
        return context.enchantRandomly(item, count, includeTreasure);
    }

    @Override
    @NotNull
    public String getType() {
        return "enchant_randomly";
    }
}

record EnchantWithLevelsFunction(LevelRange levels, boolean includeTreasure) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        int level = levels.roll(context.getRandom());
        return context.enchantWithLevels(item, level, includeTreasure);
    }

    @Override
    @NotNull
    public String getType() {
        return "enchant_with_levels";
    }
}

record LootingEnchantFunction(CountRange countPerLevel, int limit) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        int lootingLevel = context.getLootingLevel();
        int bonus = countPerLevel.roll(context.getRandom()) * lootingLevel;
        if (limit > 0 && bonus > limit) {
            bonus = limit;
        }
        int newCount = item.getAmount() + bonus;
        return item.toBuilder().amount(newCount).build();
    }

    @Override
    @NotNull
    public String getType() {
        return "looting_enchant";
    }
}

record SetNameFunction(Component name) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        return item.toBuilder().name(name).build();
    }

    @Override
    @NotNull
    public String getType() {
        return "set_name";
    }
}

record SetLoreFunction(List<Component> lore) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        return item.toBuilder().lore(lore).build();
    }

    @Override
    @NotNull
    public String getType() {
        return "set_lore";
    }
}

record ApplyBonusFunction(String enchantment, BonusFormula formula) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        int level = context.getEnchantmentLevel(enchantment);
        int bonus = formula.calculate(level, context.getRandom());
        int newCount = item.getAmount() + bonus;
        return item.toBuilder().amount(Math.max(1, newCount)).build();
    }

    @Override
    @NotNull
    public String getType() {
        return "apply_bonus";
    }
}

record CustomFunction(
        BiFunction<UnifiedItemStack, LootContext, UnifiedItemStack> transformer
) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        return transformer.apply(item, context);
    }

    @Override
    @NotNull
    public String getType() {
        return "custom";
    }
}

record SetPersistentDataFunction<T>(String key, T value) implements LootFunction {
    @Override
    @NotNull
    public UnifiedItemStack apply(@NotNull UnifiedItemStack item, @NotNull LootContext context) {
        return item.toBuilder().persistentData(key, value).build();
    }

    @Override
    @NotNull
    public String getType() {
        return "set_persistent_data";
    }
}
