/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Unified player events that bridge platform-specific player events.
 *
 * <p>This package contains platform-agnostic player events that are automatically
 * bridged from the underlying platform's event system (Bukkit, Sponge, etc.).
 * These events allow plugins to handle player actions without worrying about
 * platform differences.
 *
 * <h2>Available Events</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.event.player.PlayerJoinEvent} - When a player joins</li>
 *   <li>{@link sh.pcx.unified.event.player.PlayerQuitEvent} - When a player quits</li>
 *   <li>{@link sh.pcx.unified.event.player.PlayerChatEvent} - When a player chats</li>
 *   <li>{@link sh.pcx.unified.event.player.PlayerMoveEvent} - When a player moves</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyListener implements EventListener {
 *
 *     @EventHandler
 *     public void onJoin(PlayerJoinEvent event) {
 *         UnifiedPlayer player = event.getPlayer();
 *         player.sendMessage(Component.text("Welcome to the server!"));
 *     }
 *
 *     @EventHandler
 *     public void onChat(PlayerChatEvent event) {
 *         if (event.getMessage().contains("spam")) {
 *             event.setCancelled(true);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.event.player;
