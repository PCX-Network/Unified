/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.navigation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Controller for entity navigation and pathfinding.
 *
 * <p>The NavigationController provides methods for moving entities,
 * computing paths, and configuring pathfinding behavior.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * NavigationController nav = ai.getNavigation(entity);
 *
 * // Move to location
 * nav.moveTo(targetLocation, speed);
 *
 * // Move to entity
 * nav.moveTo(targetEntity, speed);
 *
 * // Check navigation state
 * if (nav.isNavigating()) {
 *     Path path = nav.getCurrentPath();
 *     Location destination = nav.getDestination();
 * }
 *
 * // Async path computation
 * nav.computePath(destination).thenAccept(path -> {
 *     if (path != null && path.isValid()) {
 *         nav.followPath(path, speed);
 *     }
 * });
 *
 * // Configure pathfinding
 * nav.setCanOpenDoors(true);
 * nav.setCanFloat(true);
 * nav.setAvoidWater(false);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Path
 */
public interface NavigationController {

    /**
     * Returns the entity this controller manages.
     *
     * @param <T> the entity type
     * @return the entity
     * @since 1.0.0
     */
    @NotNull
    <T> T getEntity();

    // =========================================================================
    // Movement
    // =========================================================================

    /**
     * Moves the entity to a target (location or entity).
     *
     * <p>The target can be a location object or another entity.
     * The implementation will determine the appropriate behavior
     * based on the target type.
     *
     * @param target the target location or entity
     * @param speed  the movement speed modifier
     * @return true if navigation started
     * @since 1.0.0
     */
    boolean moveTo(@NotNull Object target, double speed);

    /**
     * Moves the entity to exact coordinates.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     * @param speed the movement speed modifier
     * @return true if navigation started
     * @since 1.0.0
     */
    boolean moveTo(double x, double y, double z, double speed);

    /**
     * Stops the current navigation.
     *
     * @since 1.0.0
     */
    void stop();

    /**
     * Checks if the entity is currently navigating.
     *
     * @return true if navigating
     * @since 1.0.0
     */
    boolean isNavigating();

    /**
     * Checks if the entity is stuck.
     *
     * @return true if stuck
     * @since 1.0.0
     */
    boolean isStuck();

    /**
     * Checks if navigation has completed.
     *
     * @return true if done
     * @since 1.0.0
     */
    boolean isDone();

    // =========================================================================
    // Path Information
    // =========================================================================

    /**
     * Gets the current path.
     *
     * @return the current path, or null if not navigating
     * @since 1.0.0
     */
    @Nullable
    Path getCurrentPath();

    /**
     * Gets the current destination.
     *
     * @param <T> the location type
     * @return the destination, or null if not navigating
     * @since 1.0.0
     */
    @Nullable
    <T> T getDestination();

    /**
     * Gets the distance to the destination.
     *
     * @return the distance, or -1 if not navigating
     * @since 1.0.0
     */
    double getDistanceToDestination();

    /**
     * Gets the current speed modifier.
     *
     * @return the speed modifier
     * @since 1.0.0
     */
    double getSpeed();

    /**
     * Sets the movement speed.
     *
     * @param speed the speed modifier
     * @since 1.0.0
     */
    void setSpeed(double speed);

    // =========================================================================
    // Path Computation
    // =========================================================================

    /**
     * Computes a path to a location asynchronously.
     *
     * @param location the target location
     * @return a future containing the path
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Path> computePath(@NotNull Object location);

    /**
     * Computes a path to coordinates asynchronously.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return a future containing the path
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Path> computePath(double x, double y, double z);

    /**
     * Follows a pre-computed path.
     *
     * @param path  the path to follow
     * @param speed the movement speed modifier
     * @return true if navigation started
     * @since 1.0.0
     */
    boolean followPath(@NotNull Path path, double speed);

    /**
     * Recalculates the current path.
     *
     * @since 1.0.0
     */
    void recalculatePath();

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Sets whether the entity can open doors.
     *
     * @param canOpen true to allow
     * @since 1.0.0
     */
    void setCanOpenDoors(boolean canOpen);

    /**
     * Checks if the entity can open doors.
     *
     * @return true if allowed
     * @since 1.0.0
     */
    boolean canOpenDoors();

    /**
     * Sets whether the entity can break doors.
     *
     * @param canBreak true to allow
     * @since 1.0.0
     */
    void setCanBreakDoors(boolean canBreak);

    /**
     * Checks if the entity can break doors.
     *
     * @return true if allowed
     * @since 1.0.0
     */
    boolean canBreakDoors();

    /**
     * Sets whether the entity can pass through doors.
     *
     * @param canPass true to allow
     * @since 1.0.0
     */
    void setCanPassDoors(boolean canPass);

    /**
     * Checks if the entity can pass through doors.
     *
     * @return true if allowed
     * @since 1.0.0
     */
    boolean canPassDoors();

    /**
     * Sets whether the entity can float in water.
     *
     * @param canFloat true to allow
     * @since 1.0.0
     */
    void setCanFloat(boolean canFloat);

    /**
     * Checks if the entity can float.
     *
     * @return true if allowed
     * @since 1.0.0
     */
    boolean canFloat();

    /**
     * Sets whether the entity avoids water.
     *
     * @param avoidWater true to avoid
     * @since 1.0.0
     */
    void setAvoidWater(boolean avoidWater);

    /**
     * Checks if the entity avoids water.
     *
     * @return true if avoiding
     * @since 1.0.0
     */
    boolean avoidsWater();

    /**
     * Sets whether the entity avoids sunlight.
     *
     * @param avoidSun true to avoid
     * @since 1.0.0
     */
    void setAvoidSun(boolean avoidSun);

    /**
     * Checks if the entity avoids sunlight.
     *
     * @return true if avoiding
     * @since 1.0.0
     */
    boolean avoidsSun();

    /**
     * Sets the pathfinding range.
     *
     * @param range the maximum pathfinding range
     * @since 1.0.0
     */
    void setRange(float range);

    /**
     * Gets the pathfinding range.
     *
     * @return the range
     * @since 1.0.0
     */
    float getRange();

    // =========================================================================
    // Path Penalties
    // =========================================================================

    /**
     * Sets the path penalty for a block type.
     *
     * <p>Higher penalties make paths avoid these blocks.
     * A penalty of -1 makes the block impassable.
     *
     * @param pathType the path node type
     * @param penalty  the penalty value
     * @since 1.0.0
     */
    void setPathPenalty(@NotNull PathType pathType, float penalty);

    /**
     * Gets the path penalty for a block type.
     *
     * @param pathType the path node type
     * @return the penalty value
     * @since 1.0.0
     */
    float getPathPenalty(@NotNull PathType pathType);

    /**
     * Resets all path penalties to default.
     *
     * @since 1.0.0
     */
    void resetPathPenalties();

    /**
     * Path node types for penalty configuration.
     *
     * @since 1.0.0
     */
    enum PathType {
        BLOCKED,
        OPEN,
        WALKABLE,
        TRAPDOOR,
        FENCE,
        LAVA,
        WATER,
        WATER_BORDER,
        RAIL,
        UNPASSABLE_RAIL,
        DANGER_FIRE,
        DAMAGE_FIRE,
        DANGER_OTHER,
        DAMAGE_OTHER,
        DOOR_OPEN,
        DOOR_WOOD_CLOSED,
        DOOR_IRON_CLOSED,
        BREACH,
        LEAVES,
        STICKY_HONEY,
        COCOA,
        POWDER_SNOW,
        DANGER_POWDER_SNOW,
        DAMAGE_CACTUS
    }
}
