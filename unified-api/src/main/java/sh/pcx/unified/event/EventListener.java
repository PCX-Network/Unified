/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

/**
 * Marker interface for event listener classes.
 *
 * <p>Classes that contain event handler methods must implement this interface
 * to be registered with the {@link EventBus}. The interface itself has no
 * methods; it serves as a marker to identify listener classes.
 *
 * <h2>Usage</h2>
 * <p>Create a class that implements this interface and add handler methods
 * annotated with {@link EventHandler} or {@link AsyncHandler}:
 *
 * <pre>{@code
 * public class MyListener implements EventListener {
 *
 *     @EventHandler
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         event.getPlayer().sendMessage(Component.text("Welcome!"));
 *     }
 *
 *     @EventHandler(priority = EventPriority.HIGH)
 *     public void onBlockBreak(BlockBreakEvent event) {
 *         if (isProtected(event.getBlock())) {
 *             event.setCancelled(true);
 *         }
 *     }
 *
 *     @AsyncHandler
 *     public void onPlayerJoinAsync(PlayerJoinEvent event) {
 *         // Async database operations
 *         loadPlayerData(event.getPlayer().getUniqueId());
 *     }
 * }
 * }</pre>
 *
 * <h2>Registration</h2>
 * <p>Register the listener with the event bus:
 *
 * <pre>{@code
 * EventBus eventBus = UnifiedAPI.getEventBus();
 * eventBus.register(plugin, new MyListener());
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 * <p>Listeners are typically registered in the plugin's {@code onEnable()}
 * method and automatically unregistered when the plugin is disabled. You
 * can also manually unregister a listener:
 *
 * <pre>{@code
 * eventBus.unregister(myListener);
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Keep listeners focused on a single responsibility</li>
 *   <li>Use appropriate priorities for your use case</li>
 *   <li>Handle exceptions gracefully within handlers</li>
 *   <li>Use async handlers for I/O operations</li>
 *   <li>Consider using weak listeners for optional integrations</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventHandler
 * @see AsyncHandler
 * @see EventBus
 * @see WeakListener
 */
public interface EventListener {

    // Marker interface - no methods required
}
