/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Implementation classes for the unified-content module.
 *
 * <p>This package contains the default implementations of the content services:
 * <ul>
 *   <li>{@link sh.pcx.unified.content.impl.EnchantmentServiceImpl} - Custom enchantments with triggers</li>
 *   <li>{@link sh.pcx.unified.content.impl.LootTableServiceImpl} - Custom loot tables with pools/conditions/functions</li>
 *   <li>{@link sh.pcx.unified.content.impl.AdvancementServiceImpl} - Custom advancements with triggers and rewards</li>
 *   <li>{@link sh.pcx.unified.content.impl.ResourcePackServiceImpl} - Dynamic resource pack generation</li>
 *   <li>{@link sh.pcx.unified.content.impl.RecipeServiceImpl} - Custom crafting recipes</li>
 * </ul>
 *
 * <p>These implementations are platform-agnostic and provide the core logic
 * for content management. Platform-specific implementations (e.g., for Paper
 * or Sponge) can extend these classes to integrate with the native APIs.
 *
 * <h2>Usage</h2>
 * <p>Obtain service instances through dependency injection:
 * <pre>{@code
 * @Inject
 * private EnchantmentService enchantments;
 *
 * @Inject
 * private LootTableService lootTables;
 *
 * @Inject
 * private AdvancementService advancements;
 *
 * @Inject
 * private ResourcePackService resourcePacks;
 *
 * @Inject
 * private RecipeService recipes;
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.content.impl;
