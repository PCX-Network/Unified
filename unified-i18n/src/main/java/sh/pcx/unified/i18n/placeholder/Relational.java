/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a placeholder method as relational, requiring two player contexts.
 *
 * <p>Relational placeholders are used when the placeholder value depends on the
 * relationship between two players. Common use cases include:
 * <ul>
 *   <li>Faction/team relationships</li>
 *   <li>Friend status</li>
 *   <li>Combat tags</li>
 *   <li>Relative position/distance</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @PlaceholderExpansion(identifier = "factions")
 * public class FactionsPlaceholders {
 *
 *     @Placeholder("rel_faction")
 *     @Relational
 *     public String getRelationalFaction(UnifiedPlayer viewer, UnifiedPlayer target) {
 *         Faction viewerFaction = factions.getFaction(viewer);
 *         Faction targetFaction = factions.getFaction(target);
 *
 *         if (viewerFaction.equals(targetFaction)) {
 *             return "&aAlly";
 *         } else if (viewerFaction.isEnemyOf(targetFaction)) {
 *             return "&cEnemy";
 *         }
 *         return "&7Neutral";
 *     }
 *
 *     @Placeholder("rel_distance")
 *     @Relational
 *     public String getDistance(UnifiedPlayer viewer, UnifiedPlayer target) {
 *         double distance = viewer.getLocation().distance(target.getLocation());
 *         return String.format("%.1f", distance);
 *     }
 * }
 * }</pre>
 *
 * <h2>Method Signature Requirements</h2>
 * <p>Relational placeholder methods must accept two player parameters:
 * <ol>
 *   <li><strong>Viewer</strong> - The player viewing the placeholder</li>
 *   <li><strong>Target</strong> - The player being viewed/referenced</li>
 * </ol>
 *
 * <p>Alternative signatures:
 * <pre>{@code
 * // Full context access
 * @Placeholder("rel_custom")
 * @Relational
 * public String getRelational(PlaceholderContext context) {
 *     UnifiedPlayer viewer = context.getPlayer().orElse(null);
 *     UnifiedPlayer target = context.getRelationalPlayer().orElse(null);
 *     // ...
 * }
 * }</pre>
 *
 * <h2>Usage in Messages</h2>
 * <p>Relational placeholders use the {@code %rel_} prefix:
 * <pre>
 * %rel_factions_rel_faction%  - Shows faction relationship
 * %rel_factions_rel_distance% - Shows distance between players
 * </pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Placeholder
 * @see PlaceholderContext#getRelationalPlayer()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Relational {

    /**
     * A description of the relational context.
     *
     * @return the description
     */
    String description() default "";

    /**
     * Whether both players must be in the same world.
     *
     * <p>If {@code true}, the placeholder will return the fallback value
     * when players are in different worlds.
     *
     * @return {@code true} if same-world is required
     */
    boolean sameWorld() default false;

    /**
     * The maximum distance between players for this placeholder to work.
     *
     * <p>A value of -1 means no distance limit. Only checked if both players
     * are in the same world.
     *
     * @return the maximum distance, or -1 for unlimited
     */
    double maxDistance() default -1;
}
