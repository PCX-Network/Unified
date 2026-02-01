/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add conditional filtering to event handlers.
 *
 * <p>When applied to a method annotated with {@link EventHandler}, this filter
 * determines whether the handler should be invoked based on the specified conditions.
 * Multiple filters can be applied to a single handler using repeated annotations.
 *
 * <h2>Filter Types</h2>
 * <p>Filters can check various conditions:
 * <ul>
 *   <li><b>Permission</b>: Player must have a specific permission</li>
 *   <li><b>World</b>: Event must occur in a specific world</li>
 *   <li><b>Condition</b>: Custom condition expression (SpEL-like)</li>
 *   <li><b>Custom</b>: Reference to a predicate method or class</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class FilteredListener implements EventListener {
 *
 *     // Only handle events from players with VIP permission
 *     @EventHandler
 *     @Filter(permission = "vip.features")
 *     public void onVipPlayerJoin(PlayerJoinEvent event) {
 *         event.getPlayer().sendMessage(Component.text("VIP Welcome!"));
 *     }
 *
 *     // Only handle events in the "world" world
 *     @EventHandler
 *     @Filter(world = "world")
 *     public void onOverworldBlockBreak(BlockBreakEvent event) {
 *         // Only fires in the overworld
 *     }
 *
 *     // Multiple filters (AND logic)
 *     @EventHandler
 *     @Filter(permission = "admin.bypass")
 *     @Filter(world = "spawn")
 *     public void onAdminSpawnAction(PlayerMoveEvent event) {
 *         // Only fires for admins in spawn world
 *     }
 *
 *     // Custom condition expression
 *     @EventHandler
 *     @Filter(condition = "player.health < 5")
 *     public void onLowHealthMove(PlayerMoveEvent event) {
 *         // Only fires when player health is low
 *     }
 *
 *     // Negated filter
 *     @EventHandler
 *     @Filter(permission = "staff", negate = true)
 *     public void onNonStaffJoin(PlayerJoinEvent event) {
 *         // Only fires for non-staff players
 *     }
 *
 *     // Custom predicate method
 *     @EventHandler
 *     @Filter(predicate = "isValidTarget")
 *     public void onValidTarget(PlayerDamageEvent event) {
 *         // Only fires if isValidTarget(event) returns true
 *     }
 *
 *     private boolean isValidTarget(PlayerDamageEvent event) {
 *         return event.getDamage() > 0 && !event.getPlayer().isFlying();
 *     }
 * }
 * }</pre>
 *
 * <h2>Multiple Filters</h2>
 * <p>When multiple filters are applied to a handler, they are evaluated using
 * AND logic by default. The handler is only called if ALL filters pass. Use
 * the {@link Filters} container annotation for explicit grouping.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventHandler
 * @see Filters
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Filters.class)
public @interface Filter {

    /**
     * Permission required for this handler to be invoked.
     *
     * <p>If specified, the event must involve a player (directly or indirectly)
     * who has this permission. For non-player events, the filter is skipped.
     *
     * <p>Leave empty to skip permission checking.
     *
     * @return the required permission node
     * @since 1.0.0
     */
    String permission() default "";

    /**
     * World name required for this handler to be invoked.
     *
     * <p>If specified, the event must occur in a world with this name.
     * For events without a world context, the filter is skipped.
     *
     * <p>Leave empty to skip world checking.
     *
     * @return the required world name
     * @since 1.0.0
     */
    String world() default "";

    /**
     * Custom condition expression.
     *
     * <p>A SpEL-like expression that is evaluated against the event.
     * The expression has access to:
     * <ul>
     *   <li>{@code event} - The event object</li>
     *   <li>{@code player} - The player involved (if applicable)</li>
     *   <li>{@code world} - The world involved (if applicable)</li>
     *   <li>{@code location} - The location involved (if applicable)</li>
     * </ul>
     *
     * <p>Example expressions:
     * <ul>
     *   <li>{@code "player.health > 10"}</li>
     *   <li>{@code "event.damage < 5"}</li>
     *   <li>{@code "location.y > 64"}</li>
     * </ul>
     *
     * <p>Leave empty to skip condition checking.
     *
     * @return the condition expression
     * @since 1.0.0
     */
    String condition() default "";

    /**
     * Name of a predicate method in the listener class.
     *
     * <p>The method must:
     * <ul>
     *   <li>Be accessible from the listener class</li>
     *   <li>Accept the event type as its only parameter</li>
     *   <li>Return a boolean</li>
     * </ul>
     *
     * <p>Example: {@code @Filter(predicate = "shouldHandle")}
     * with method {@code boolean shouldHandle(MyEvent event)}
     *
     * <p>Leave empty to skip predicate checking.
     *
     * @return the predicate method name
     * @since 1.0.0
     */
    String predicate() default "";

    /**
     * Custom predicate class.
     *
     * <p>A class implementing {@code java.util.function.Predicate<UnifiedEvent>}
     * that will be instantiated and used to filter events.
     *
     * <p>Use {@code Void.class} (the default) to skip this filter type.
     *
     * @return the predicate class
     * @since 1.0.0
     */
    Class<?> predicateClass() default Void.class;

    /**
     * Whether to negate the filter result.
     *
     * <p>When set to {@code true}, the filter passes when the condition
     * would normally fail, and fails when it would normally pass.
     *
     * <p>Example: {@code @Filter(permission = "staff", negate = true)}
     * passes for players WITHOUT the staff permission.
     *
     * @return true to negate the filter
     * @since 1.0.0
     */
    boolean negate() default false;

    /**
     * Description of this filter for debugging purposes.
     *
     * <p>This description is used in logging and error messages to help
     * identify why an event handler was or was not called.
     *
     * @return the filter description
     * @since 1.0.0
     */
    String description() default "";
}
