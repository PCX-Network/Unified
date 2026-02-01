/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Custom enchantment system for the UnifiedPlugin framework.
 *
 * <p>This package provides a complete API for creating custom enchantments
 * with triggers, effects, level scaling, and full integration into
 * Minecraft's enchanting system.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.content.enchantment.EnchantmentService} - Main service interface</li>
 *   <li>{@link sh.pcx.unified.content.enchantment.CustomEnchantment} - Enchantment representation</li>
 *   <li>{@link sh.pcx.unified.content.enchantment.EnchantmentBuilder} - Fluent builder</li>
 *   <li>{@link sh.pcx.unified.content.enchantment.EnchantmentTarget} - Target item types</li>
 *   <li>{@link sh.pcx.unified.content.enchantment.EnchantmentContext} - Effect handler contexts</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CustomEnchantment lifesteal = enchantments.register("myplugin:lifesteal")
 *     .displayName(Component.text("Lifesteal"))
 *     .maxLevel(5)
 *     .target(EnchantmentTarget.WEAPON)
 *     .onHit((ctx, level) -> {
 *         double heal = ctx.getDamage() * (0.05 * level);
 *         ctx.getPlayer().heal(heal);
 *     })
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.content.enchantment;
