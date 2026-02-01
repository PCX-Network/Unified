/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Loot table system for the UnifiedPlugin framework.
 *
 * <p>This package provides a complete API for creating custom loot tables
 * and modifying vanilla loot tables with pools, conditions, functions,
 * and context-aware generation.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.content.loot.LootTableService} - Main service interface</li>
 *   <li>{@link sh.pcx.unified.content.loot.LootTable} - Loot table representation</li>
 *   <li>{@link sh.pcx.unified.content.loot.LootPool} - Pool of loot entries</li>
 *   <li>{@link sh.pcx.unified.content.loot.LootEntry} - Individual loot entries</li>
 *   <li>{@link sh.pcx.unified.content.loot.LootCondition} - Drop conditions</li>
 *   <li>{@link sh.pcx.unified.content.loot.LootFunction} - Item modifiers</li>
 *   <li>{@link sh.pcx.unified.content.loot.LootTables} - Vanilla table constants</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LootTable bossDrop = lootTables.create("myplugin:dragon_boss")
 *     .pool(LootPool.builder()
 *         .rolls(RollRange.between(2, 5))
 *         .condition(LootCondition.killedByPlayer())
 *         .entry(LootEntry.item("minecraft:diamond")
 *             .weight(10)
 *             .count(CountRange.between(5, 10)))
 *         .build())
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.content.loot;
