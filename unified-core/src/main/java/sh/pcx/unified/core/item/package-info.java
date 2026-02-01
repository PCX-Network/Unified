/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Core item building implementation with full NBT support.
 *
 * <p>This package provides the core implementation of the ItemBuilder interface
 * with comprehensive support for all item properties including NBT data.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * UnifiedItemStack item = CoreItemBuilder.create("minecraft:diamond_sword")
 *     .name(Component.text("Excalibur").color(NamedTextColor.GOLD))
 *     .lore(
 *         Component.text("A legendary blade"),
 *         Component.text("Damage: +50")
 *     )
 *     .enchant("minecraft:sharpness", 5)
 *     .unbreakable(true)
 *     .build();
 * }</pre>
 *
 * <h2>Persistent Data</h2>
 * <pre>{@code
 * UnifiedItemStack item = CoreItemBuilder.create("minecraft:stick")
 *     .name(Component.text("Magic Wand"))
 *     .persistentData("myplugin:wand_type", "fire")
 *     .persistentData("myplugin:power_level", 10)
 *     .build();
 * }</pre>
 *
 * <h2>Skulls</h2>
 * <pre>{@code
 * UnifiedItemStack skull = CoreItemBuilder.skull()
 *     .skullOwner(playerUuid)
 *     .name(Component.text("Player's Head"))
 *     .build();
 * }</pre>
 *
 * <h2>Platform Implementation</h2>
 * <p>The CoreItemBuilder is designed to be extended by platform-specific
 * implementations (Paper, Sponge) that handle the actual item creation.
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.core.item;
