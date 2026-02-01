/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Hologram API for creating floating text, items, and block displays.
 *
 * <p>This package provides a comprehensive API for creating and managing holograms
 * in Minecraft. Holograms are floating displays that can show text, items, or blocks
 * at specific locations in the world.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Text lines with Adventure component support</li>
 *   <li>Item display lines</li>
 *   <li>Block display lines (1.19.4+ with display entities)</li>
 *   <li>Per-player visibility control</li>
 *   <li>Animation support (rotation, bobbing, color cycling)</li>
 *   <li>Placeholder integration</li>
 *   <li>Persistence across server restarts</li>
 *   <li>Modern display entity support with armor stand fallback</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Create a simple text hologram
 * Hologram hologram = HologramBuilder.create("my-hologram")
 *     .location(location)
 *     .addLine(TextLine.of(Component.text("Welcome!")))
 *     .addLine(TextLine.of(Component.text("to our server")))
 *     .build();
 *
 * // Show to all players
 * hologram.showToAll();
 *
 * // Create an animated hologram
 * Hologram animated = HologramBuilder.create("animated")
 *     .location(location)
 *     .addLine(TextLine.of(Component.text("Rotating Item")))
 *     .addLine(ItemLine.of("minecraft:diamond"))
 *     .animation(new RotationAnimation(2.0))
 *     .build();
 * }</pre>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.visual.hologram.core} - Core hologram interfaces</li>
 *   <li>{@link sh.pcx.unified.visual.hologram.line} - Hologram line types</li>
 *   <li>{@link sh.pcx.unified.visual.hologram.display} - Display implementations</li>
 *   <li>{@link sh.pcx.unified.visual.hologram.animation} - Animation types</li>
 *   <li>{@link sh.pcx.unified.visual.hologram.visibility} - Visibility control</li>
 *   <li>{@link sh.pcx.unified.visual.hologram.persistence} - Persistence support</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.visual.hologram;
