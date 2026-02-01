/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Unified world events that bridge platform-specific world and block events.
 *
 * <p>This package contains platform-agnostic world events that are automatically
 * bridged from the underlying platform's event system. These events cover block
 * changes, world modifications, and other world-related actions.
 *
 * <h2>Available Events</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.event.world.BlockBreakEvent} - When a block is broken</li>
 *   <li>{@link sh.pcx.unified.event.world.BlockPlaceEvent} - When a block is placed</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyListener implements EventListener {
 *
 *     @EventHandler
 *     public void onBlockBreak(BlockBreakEvent event) {
 *         UnifiedBlock block = event.getBlock();
 *         UnifiedPlayer player = event.getPlayer();
 *
 *         // Protect diamond ore
 *         if (block.getType().equals("minecraft:diamond_ore")) {
 *             if (!player.hasPermission("mining.diamond")) {
 *                 event.setCancelled(true);
 *                 player.sendMessage(Component.text("You cannot mine diamonds!"));
 *             }
 *         }
 *     }
 *
 *     @EventHandler
 *     public void onBlockPlace(BlockPlaceEvent event) {
 *         // Log block placements
 *         logger.info(event.getPlayer().getName() + " placed "
 *             + event.getBlockType() + " at " + event.getBlock().getLocation());
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.event.world;
