/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Hologram system API with display entity support and legacy armor stand fallback.
 *
 * <p>The hologram system provides:
 * <ul>
 *   <li>Text holograms with multi-line support</li>
 *   <li>Item holograms with animations (spin, bob)</li>
 *   <li>Per-player visibility control</li>
 *   <li>Display entities (1.19.4+) with armor stand fallback</li>
 *   <li>Animations including rotation, bobbing, and color cycling</li>
 *   <li>Click detection and interaction</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a text hologram
 * Hologram hologram = hologramService.create(location)
 *     .addLine(Component.text("Welcome!", NamedTextColor.GOLD))
 *     .addLine(Component.text("Server Status: Online"))
 *     .billboard(Billboard.CENTER)
 *     .build();
 *
 * // Create an item hologram
 * Hologram itemDisplay = hologramService.createItem(location)
 *     .item(itemStack)
 *     .spin(true, 2.0f)
 *     .bob(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
@NullMarked
package sh.pcx.unified.visual.hologram;

import org.jspecify.annotations.NullMarked;
