package sh.pcx.unified.world.ai.navigation;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Controls navigation and pathfinding for an entity.
 *
 * <p>The NavigationController is responsible for pathfinding, movement control,
 * and obstacle avoidance. It provides methods for navigating to locations,
 * following paths, and controlling movement speed.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * NavigationController nav = aiController.getNavigation();
 *
 * // Path to a location
 * PathResult result = nav.pathTo(targetLocation, 1.0);
 * if (result.isSuccess()) {
 *     // Navigation started successfully
 * }
 *
 * // Stop navigation
 * nav.stop();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public interface NavigationController {

    /**
     * Gets the entity being navigated.
     *
     * @return the entity
     */
    @NotNull
    LivingEntity getEntity();

    /**
     * Attempts to pathfind to the specified location.
     *
     * @param location the target location
     * @param speed the movement speed multiplier
     * @return the result of the pathfinding attempt
     */
    @NotNull
    PathResult pathTo(@NotNull Location location, double speed);

    /**
     * Attempts to pathfind to the specified location with default speed.
     *
     * @param location the target location
     * @return the result of the pathfinding attempt
     */
    @NotNull
    default PathResult pathTo(@NotNull Location location) {
        return pathTo(location, 1.0);
    }

    /**
     * Attempts to pathfind to the specified entity.
     *
     * @param entity the target entity
     * @param speed the movement speed multiplier
     * @return the result of the pathfinding attempt
     */
    @NotNull
    PathResult pathTo(@NotNull LivingEntity entity, double speed);

    /**
     * Checks if the entity is currently navigating.
     *
     * @return true if navigating
     */
    boolean isNavigating();

    /**
     * Checks if the entity has reached its destination.
     *
     * @return true if at destination
     */
    boolean hasReachedDestination();

    /**
     * Stops all current navigation.
     */
    void stop();

    /**
     * Gets the current target location.
     *
     * @return the target location, or empty if not navigating
     */
    @NotNull
    Optional<Location> getTargetLocation();

    /**
     * Gets the current path.
     *
     * @return the current path, or null if not navigating
     */
    @Nullable
    Path getCurrentPath();

    /**
     * Gets the movement speed multiplier.
     *
     * @return the speed multiplier
     */
    double getSpeed();

    /**
     * Sets the movement speed multiplier.
     *
     * @param speed the speed multiplier
     */
    void setSpeed(double speed);

    /**
     * Gets the distance considered as "arrived".
     *
     * @return the arrival distance
     */
    double getArrivalDistance();

    /**
     * Sets the distance considered as "arrived".
     *
     * @param distance the arrival distance
     */
    void setArrivalDistance(double distance);

    /**
     * Checks if the entity can reach the specified location.
     *
     * @param location the target location
     * @return true if reachable
     */
    boolean canReach(@NotNull Location location);

    /**
     * Gets the distance remaining to the target.
     *
     * @return the remaining distance, or -1 if not navigating
     */
    double getRemainingDistance();

    /**
     * Ticks the navigation controller.
     */
    void tick();

    /**
     * Represents a navigation path.
     */
    interface Path {

        /**
         * Gets the target location of the path.
         *
         * @return the target location
         */
        @NotNull
        Location getTarget();

        /**
         * Gets the total length of the path.
         *
         * @return the path length
         */
        double getLength();

        /**
         * Gets the number of nodes in the path.
         *
         * @return the node count
         */
        int getNodeCount();

        /**
         * Gets the current node index.
         *
         * @return the current node index
         */
        int getCurrentNodeIndex();

        /**
         * Checks if the path is complete.
         *
         * @return true if complete
         */
        boolean isComplete();

        /**
         * Checks if the path is valid.
         *
         * @return true if valid
         */
        boolean isValid();
    }
}
