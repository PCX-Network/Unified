/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * World abstraction interfaces for platform-agnostic world operations.
 *
 * <p>This package provides interfaces for working with Minecraft worlds:
 * <ul>
 *   <li>{@link sh.pcx.unified.world.UnifiedWorld} - World operations interface</li>
 *   <li>{@link sh.pcx.unified.world.UnifiedLocation} - Immutable location record</li>
 *   <li>{@link sh.pcx.unified.world.UnifiedBlock} - Block operations interface</li>
 *   <li>{@link sh.pcx.unified.world.UnifiedChunk} - Chunk operations interface</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a world
 * Optional<UnifiedWorld> world = server.getWorld("world");
 *
 * world.ifPresent(w -> {
 *     // Get spawn location
 *     UnifiedLocation spawn = w.getSpawnLocation();
 *
 *     // Get a block
 *     UnifiedBlock block = w.getBlockAt(100, 64, 200);
 *
 *     // Set world time
 *     w.setTime(6000); // Noon
 * });
 *
 * // Create a location
 * UnifiedLocation loc = new UnifiedLocation(world, 100, 64, 200, 0, 0);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.world;
