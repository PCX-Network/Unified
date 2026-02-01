/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Folia-specific task types for region-aware scheduling.
 *
 * <p>Folia partitions the Minecraft world into regions, each ticked by
 * its own thread. These task types ensure operations run on the correct
 * thread for thread-safe access to world state.
 *
 * <h2>Task Types</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.scheduler.folia.RegionTask} -
 *       Base class for region-bound tasks</li>
 *   <li>{@link sh.pcx.unified.scheduler.folia.EntityTask} -
 *       Tasks bound to an entity's owning thread</li>
 *   <li>{@link sh.pcx.unified.scheduler.folia.LocationTask} -
 *       Tasks bound to a location's owning thread</li>
 *   <li>{@link sh.pcx.unified.scheduler.folia.GlobalTask} -
 *       Tasks on the global region (non-world operations)</li>
 * </ul>
 *
 * <h2>Folia Threading Model</h2>
 * <p>In Folia, the world is divided into chunk-based regions. Each region
 * has its own tick thread. To safely access entities or blocks, you must
 * run code on the owning region's thread.
 *
 * <h2>Fallback Behavior</h2>
 * <p>On non-Folia servers (Paper, Spigot), these task types gracefully
 * fall back to running on the main server thread, maintaining compatibility.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Entity-bound task
 * scheduler.runAtEntity(player, () -> {
 *     player.setHealth(20.0);  // Thread-safe on Folia
 * });
 *
 * // Location-bound task
 * scheduler.runAtLocation(blockLocation, () -> {
 *     world.setBlockData(location, data);  // Thread-safe on Folia
 * });
 *
 * // Global task (no world access)
 * scheduler.runOnGlobal(() -> {
 *     Bukkit.broadcast(Component.text("Hello!"));
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.scheduler.types
 */
package sh.pcx.unified.scheduler.folia;
