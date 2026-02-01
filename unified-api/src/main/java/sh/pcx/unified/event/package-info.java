/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Unified event system for platform-agnostic event handling.
 *
 * <p>This package provides a comprehensive event system that abstracts the differences
 * between platform-specific event systems (Bukkit, Sponge) while providing a unified
 * API for event handling, filtering, and async processing.
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.event.UnifiedEvent} - Base class for all events</li>
 *   <li>{@link sh.pcx.unified.event.Cancellable} - Interface for cancellable events</li>
 *   <li>{@link sh.pcx.unified.event.EventPriority} - Handler execution priority</li>
 *   <li>{@link sh.pcx.unified.event.EventBus} - Main event bus for registration and firing</li>
 * </ul>
 *
 * <h2>Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.event.EventHandler} - Marks methods as event handlers</li>
 *   <li>{@link sh.pcx.unified.event.Filter} - Adds conditional filtering to handlers</li>
 *   <li>{@link sh.pcx.unified.event.AsyncHandler} - Marks handlers for async execution</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyListener implements EventListener {
 *
 *     @EventHandler(priority = EventPriority.NORMAL)
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         event.getPlayer().sendMessage(Component.text("Welcome!"));
 *     }
 *
 *     @EventHandler
 *     @Filter(permission = "vip.join")
 *     public void onVipJoin(PlayerJoinEvent event) {
 *         // Only fires for players with vip.join permission
 *     }
 *
 *     @AsyncHandler
 *     public void onAsyncEvent(AsyncEvent event) {
 *         // Runs on async thread pool
 *     }
 * }
 *
 * // Register listener
 * eventBus.register(plugin, new MyListener());
 *
 * // Fire custom event
 * MyCustomEvent event = new MyCustomEvent();
 * eventBus.fire(event);
 * if (!event.isCancelled()) {
 *     // Handle event result
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.event;
