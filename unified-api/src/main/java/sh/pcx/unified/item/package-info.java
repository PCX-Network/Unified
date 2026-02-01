/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Item abstraction interfaces for platform-agnostic item handling.
 *
 * <p>This package provides interfaces for working with items:
 * <ul>
 *   <li>{@link sh.pcx.unified.item.UnifiedItemStack} - Item stack interface</li>
 *   <li>{@link sh.pcx.unified.item.ItemBuilder} - Fluent item builder interface</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create an item using the builder
 * UnifiedItemStack sword = ItemBuilder.of("minecraft:diamond_sword")
 *     .name(Component.text("Legendary Sword"))
 *     .lore(
 *         Component.text("A powerful weapon"),
 *         Component.text("Forged in ancient times")
 *     )
 *     .enchant("minecraft:sharpness", 5)
 *     .unbreakable(true)
 *     .build();
 *
 * // Give to player
 * player.giveItem(sword);
 *
 * // Check item properties
 * UnifiedItemStack item = player.getItemInMainHand();
 * if (item.hasEnchantment("minecraft:fire_aspect")) {
 *     // Handle fire aspect
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.item;
