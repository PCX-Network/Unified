/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for creating and managing loot tables.
 *
 * <p>LootTableService provides a fluent API for defining custom loot tables,
 * modifying vanilla loot tables, and generating loot with full support for
 * pools, conditions, functions, and context-aware drops.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Custom Loot Tables</b> - Define new loot tables for any purpose</li>
 *   <li><b>Modify Vanilla</b> - Add/remove entries from existing tables</li>
 *   <li><b>Pools &amp; Weights</b> - Probability-based item selection</li>
 *   <li><b>Conditions</b> - Drop requirements (player kill, looting, etc.)</li>
 *   <li><b>Functions</b> - Modify items after selection (enchant, set NBT)</li>
 *   <li><b>Context</b> - Access killer, tool, location for decisions</li>
 *   <li><b>Hot Reload</b> - Update tables without restart</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private LootTableService lootTables;
 *
 * // Create custom loot table
 * LootTable bossDrop = lootTables.create("myplugin:dragon_boss")
 *     .pool(LootPool.builder()
 *         .name("guaranteed")
 *         .rolls(1)
 *         .entry(LootEntry.item("minecraft:dragon_egg").weight(1))
 *         .build())
 *     .pool(LootPool.builder()
 *         .name("bonus")
 *         .rolls(RollRange.between(2, 5))
 *         .condition(LootCondition.killedByPlayer())
 *         .entry(LootEntry.item("minecraft:diamond")
 *             .weight(10)
 *             .count(CountRange.between(5, 10)))
 *         .build())
 *     .register();
 *
 * // Generate loot
 * List<UnifiedItemStack> drops = lootTables.generate(bossDrop, context);
 *
 * // Modify vanilla table
 * lootTables.modify(LootTables.ZOMBIE)
 *     .addPool(customPool)
 *     .apply();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootTable
 * @see LootPool
 * @see LootEntry
 */
public interface LootTableService extends Service {

    /**
     * Creates a new loot table builder with the specified key.
     *
     * @param key the unique loot table key (e.g., "myplugin:dragon_boss")
     * @return a new LootTableBuilder
     * @throws NullPointerException     if key is null
     * @throws IllegalArgumentException if key format is invalid
     * @since 1.0.0
     */
    @NotNull
    LootTableBuilder create(@NotNull String key);

    /**
     * Gets a registered loot table by key.
     *
     * @param key the loot table key
     * @return an Optional containing the loot table if found
     * @since 1.0.0
     */
    @NotNull
    Optional<LootTable> get(@NotNull String key);

    /**
     * Returns all registered custom loot tables.
     *
     * @return an unmodifiable collection of loot tables
     * @since 1.0.0
     */
    @NotNull
    Collection<LootTable> getAll();

    /**
     * Unregisters a custom loot table.
     *
     * @param key the loot table key
     * @return true if the table was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull String key);

    /**
     * Creates a modifier for an existing loot table.
     *
     * <p>Can modify both custom and vanilla loot tables.
     *
     * @param key the loot table key (e.g., "minecraft:entities/zombie")
     * @return a new LootTableModifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier modify(@NotNull String key);

    /**
     * Creates a modifier for a predefined vanilla loot table.
     *
     * @param table the vanilla loot table constant
     * @return a new LootTableModifier
     * @since 1.0.0
     */
    @NotNull
    LootTableModifier modify(@NotNull LootTables table);

    /**
     * Generates loot from a loot table.
     *
     * @param table   the loot table
     * @param context the loot context
     * @return a list of generated items
     * @since 1.0.0
     */
    @NotNull
    List<UnifiedItemStack> generate(@NotNull LootTable table, @NotNull LootContext context);

    /**
     * Generates loot from a loot table by key.
     *
     * @param key     the loot table key
     * @param context the loot context
     * @return a list of generated items, empty if table not found
     * @since 1.0.0
     */
    @NotNull
    List<UnifiedItemStack> generate(@NotNull String key, @NotNull LootContext context);

    /**
     * Registers a custom block drop table.
     *
     * @param blockType the block type ID
     * @param table     the loot table for drops
     * @since 1.0.0
     */
    void registerBlockDrop(@NotNull String blockType, @NotNull LootTable table);

    /**
     * Removes a custom block drop registration.
     *
     * @param blockType the block type ID
     * @return true if the registration was removed
     * @since 1.0.0
     */
    boolean unregisterBlockDrop(@NotNull String blockType);

    /**
     * Registers a custom entity drop table.
     *
     * @param entityType the entity type ID
     * @param table      the loot table for drops
     * @since 1.0.0
     */
    void registerEntityDrop(@NotNull String entityType, @NotNull LootTable table);

    /**
     * Removes a custom entity drop registration.
     *
     * @param entityType the entity type ID
     * @return true if the registration was removed
     * @since 1.0.0
     */
    boolean unregisterEntityDrop(@NotNull String entityType);

    /**
     * Reloads all loot table modifications.
     *
     * <p>Reapplies all custom modifications to vanilla tables.
     *
     * @since 1.0.0
     */
    void reloadModifications();
}
