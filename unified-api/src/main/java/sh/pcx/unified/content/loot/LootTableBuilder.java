/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import sh.pcx.unified.content.loot.LootTypes.LootTableType;
import org.jetbrains.annotations.NotNull;

/**
 * Fluent builder for creating loot tables.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LootTable table = LootTableBuilder.create()
 *     .key("myplugin:boss_drops")
 *     .type(LootTableType.ENTITY)
 *     .pool(LootPool.builder()
 *         .name("guaranteed")
 *         .rolls(1)
 *         .entry(LootEntry.item("minecraft:diamond").weight(1))
 *         .build())
 *     .pool(LootPool.builder()
 *         .name("bonus")
 *         .rolls(RollRange.between(1, 3))
 *         .condition(LootCondition.killedByPlayer())
 *         .entry(LootEntry.item("minecraft:emerald")
 *             .weight(10)
 *             .count(CountRange.between(1, 5)))
 *         .build())
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootTable
 * @see LootPool
 */
public interface LootTableBuilder {

    /**
     * Creates a new loot table builder.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    static LootTableBuilder create() {
        return LootTableBuilderProvider.INSTANCE.create();
    }

    /**
     * Sets the loot table key.
     *
     * @param key the namespaced key
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    LootTableBuilder key(@NotNull String key);

    /**
     * Sets the loot table type.
     *
     * @param type the table type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    LootTableBuilder type(@NotNull LootTableType type);

    /**
     * Adds a pool to the loot table.
     *
     * @param pool the loot pool
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    LootTableBuilder pool(@NotNull LootPool pool);

    /**
     * Adds multiple pools to the loot table.
     *
     * @param pools the loot pools
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    LootTableBuilder pools(@NotNull LootPool... pools);

    /**
     * Builds the loot table.
     *
     * @return the constructed LootTable
     * @throws IllegalStateException if required fields are missing
     * @since 1.0.0
     */
    @NotNull
    LootTable build();

    /**
     * Builds and registers the loot table.
     *
     * @return the registered LootTable
     * @since 1.0.0
     */
    @NotNull
    LootTable register();
}

/**
 * Internal provider for creating LootTableBuilder instances.
 */
interface LootTableBuilderProvider {
    LootTableBuilderProvider INSTANCE = null; // Set by implementation

    @NotNull
    LootTableBuilder create();
}
