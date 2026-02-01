/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a module class as an event listener that should be automatically registered.
 *
 * <p>When a class annotated with {@code @Module} also has {@code @Listen}, the module
 * system will automatically register it as an event listener when the module is enabled.
 * The listener is automatically unregistered when the module is disabled.
 *
 * <p>Classes with this annotation should implement the platform's listener interface
 * (e.g., {@code org.bukkit.event.Listener} for Paper/Spigot).
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "Teleportation")
 * @Listen
 * public class TeleportModule implements Listener {
 *
 *     @EventHandler
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         Player player = event.getPlayer();
 *         // Handle player join
 *     }
 *
 *     @EventHandler(priority = EventPriority.HIGH)
 *     public void onPlayerTeleport(PlayerTeleportEvent event) {
 *         // Handle teleportation
 *     }
 * }
 * }</pre>
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>Listener is registered when module state changes to ENABLED</li>
 *   <li>Listener is unregistered when module state changes to DISABLED or UNLOADED</li>
 *   <li>Listeners are automatically re-registered on module reload</li>
 *   <li>Events are not received while the module is disabled</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Module
 * @see Command
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Listen {

    /**
     * The event priority for all handlers in this listener.
     *
     * <p>This sets a default priority for all event handlers in the class.
     * Individual handlers can override this using their own priority annotation.
     * Valid values depend on the platform (e.g., Bukkit's EventPriority).
     *
     * <p>If not specified, the platform's default priority is used.
     *
     * @return the default event priority name, empty for platform default
     */
    String defaultPriority() default "";

    /**
     * Whether to ignore cancelled events by default.
     *
     * <p>When {@code true}, event handlers in this class will not receive
     * events that have been cancelled by other plugins/modules, unless
     * explicitly overridden on individual handlers.
     *
     * @return {@code true} to ignore cancelled events by default
     */
    boolean ignoreCancelled() default false;
}
