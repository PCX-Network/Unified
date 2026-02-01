/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import org.jetbrains.annotations.NotNull;

/**
 * Fluent interface for modifying existing loot tables.
 *
 * <p>LootTableModifier allows adding, removing, and replacing pools and
 * entries in both custom and vanilla loot tables without replacing
 * the entire table.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Add a pool to zombie drops
 * lootTables.modify(LootTables.ZOMBIE)
 *     .addPool(LootPool.builder()
 *         .name("custom_drops")
 *         .rolls(1)
 *         .condition(LootCondition.killedByPlayer())
 *         .entry(LootEntry.item("minecraft:emerald")
 *             .weight(5)
 *             .condition(LootCondition.randomChance(0.1f)))
 *         .build())
 *     .apply();
 *
 * // Remove vanilla gunpowder from creeper and add TNT
 * lootTables.modify(LootTables.CREEPER)
 *     .removeEntry("minecraft:gunpowder")
 *     .addEntry("main", LootEntry.item("minecraft:tnt")
 *         .weight(1)
 *         .count(CountRange.between(1, 2))
 *         .build())
 *     .apply();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootTableService
 */
public interface LootTableModifier {

    /**
     * Returns the key of the table being modified.
     *
     * @return the loot table key
     * @since 1.0.0
     */
    @NotNull
    String getTableKey();

    /**
     * Adds a new pool to the loot table.
     *
     * @param pool the pool to add
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier addPool(@NotNull LootPool pool);

    /**
     * Removes a pool by name.
     *
     * @param poolName the pool name
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier removePool(@NotNull String poolName);

    /**
     * Replaces a pool by name.
     *
     * @param poolName the pool name to replace
     * @param pool     the new pool
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier replacePool(@NotNull String poolName, @NotNull LootPool pool);

    /**
     * Adds an entry to a specific pool.
     *
     * @param poolName the pool name
     * @param entry    the entry to add
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier addEntry(@NotNull String poolName, @NotNull LootEntry entry);

    /**
     * Removes an entry from all pools by item type.
     *
     * @param itemType the item type to remove (e.g., "minecraft:gunpowder")
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier removeEntry(@NotNull String itemType);

    /**
     * Removes an entry from a specific pool.
     *
     * @param poolName the pool name
     * @param itemType the item type to remove
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier removeEntry(@NotNull String poolName, @NotNull String itemType);

    /**
     * Adds a condition to all pools.
     *
     * @param condition the condition to add
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier addCondition(@NotNull LootCondition condition);

    /**
     * Adds a condition to a specific pool.
     *
     * @param poolName  the pool name
     * @param condition the condition to add
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier addCondition(@NotNull String poolName, @NotNull LootCondition condition);

    /**
     * Adds a function to all entries in all pools.
     *
     * @param function the function to add
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier addFunction(@NotNull LootFunction function);

    /**
     * Adds a function to a specific entry.
     *
     * @param itemType the item type
     * @param function the function to add
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier addFunction(@NotNull String itemType, @NotNull LootFunction function);

    /**
     * Sets a modifier weight multiplier for all entries.
     *
     * @param multiplier the weight multiplier
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier scaleWeights(float multiplier);

    /**
     * Sets a modifier for rolls in all pools.
     *
     * @param bonusRolls bonus rolls to add
     * @return this modifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier addBonusRolls(int bonusRolls);

    /**
     * Applies all modifications to the loot table.
     *
     * <p>This method must be called to make the modifications take effect.
     *
     * @since 1.0.0
     */
    void apply();

    /**
     * Clears all pending modifications without applying.
     *
     * @since 1.0.0
     */
    void clear();

    /**
     * Reverts all modifications made by this modifier.
     *
     * <p>Restores the table to its state before any modifications from
     * this modifier were applied.
     *
     * @since 1.0.0
     */
    void revert();
}
