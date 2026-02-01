/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import sh.pcx.unified.content.loot.LootTypes.LootTableType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Represents a loot table containing pools of potential drops.
 *
 * <p>LootTable encapsulates the definition of possible item drops,
 * organized into pools with entries, conditions, and functions.
 *
 * <h2>Structure</h2>
 * <pre>
 * LootTable
 *   |-- Pool 1 (rolls: 1-3)
 *   |     |-- Entry: Diamond (weight: 10)
 *   |     |-- Entry: Gold (weight: 20)
 *   |     \-- Condition: killedByPlayer()
 *   |
 *   \-- Pool 2 (rolls: 1)
 *         |-- Entry: Rare Item (weight: 1)
 *         \-- Condition: randomChance(0.01)
 * </pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootTableService
 * @see LootPool
 */
public interface LootTable {

    /**
     * Returns the unique key for this loot table.
     *
     * @return the namespaced key
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Returns all pools in this loot table.
     *
     * @return an unmodifiable list of pools
     * @since 1.0.0
     */
    @NotNull
    List<LootPool> getPools();

    /**
     * Gets a pool by name.
     *
     * @param name the pool name
     * @return an Optional containing the pool if found
     * @since 1.0.0
     */
    @NotNull
    Optional<LootPool> getPool(@NotNull String name);

    /**
     * Returns the type of this loot table.
     *
     * @return the loot table type
     * @since 1.0.0
     */
    @NotNull
    LootTableType getType();

    /**
     * Creates a builder from this table for modifications.
     *
     * @return a new builder pre-populated with this table's data
     * @since 1.0.0
     */
    @NotNull
    LootTableBuilder toBuilder();

    /**
     * Creates a new loot table builder.
     *
     * @return a new LootTableBuilder
     * @since 1.0.0
     */
    @NotNull
    static LootTableBuilder builder() {
        return LootTableBuilder.create();
    }
}
