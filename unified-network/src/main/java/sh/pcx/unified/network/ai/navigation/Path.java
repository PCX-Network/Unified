/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.navigation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a navigation path for entity movement.
 *
 * <p>A path consists of a series of nodes (waypoints) that an entity
 * follows to reach a destination.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Path path = nav.getCurrentPath();
 *
 * if (path != null && path.isValid()) {
 *     Location destination = path.getDestination();
 *     int length = path.getLength();
 *     double distance = path.getDistanceToTarget();
 *
 *     // Get specific nodes
 *     PathNode current = path.getCurrentNode();
 *     PathNode next = path.getNextNode();
 *
 *     // Iterate all nodes
 *     for (PathNode node : path.getNodes()) {
 *         // Process node
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see NavigationController
 * @see PathNode
 */
public interface Path {

    /**
     * Checks if this path is valid and can be followed.
     *
     * @return true if valid
     * @since 1.0.0
     */
    boolean isValid();

    /**
     * Checks if this path has reached its destination.
     *
     * @return true if done
     * @since 1.0.0
     */
    boolean isDone();

    /**
     * Gets the destination of this path.
     *
     * @param <T> the location type
     * @return the destination
     * @since 1.0.0
     */
    @NotNull
    <T> T getDestination();

    /**
     * Gets the final target of this path.
     *
     * @param <T> the location type
     * @return the target
     * @since 1.0.0
     */
    @NotNull
    <T> T getTarget();

    /**
     * Gets the number of nodes in this path.
     *
     * @return the path length
     * @since 1.0.0
     */
    int getLength();

    /**
     * Gets the current node index.
     *
     * @return the current index
     * @since 1.0.0
     */
    int getCurrentIndex();

    /**
     * Sets the current node index.
     *
     * @param index the new index
     * @since 1.0.0
     */
    void setCurrentIndex(int index);

    /**
     * Gets the current node.
     *
     * @return the current node, or null if done
     * @since 1.0.0
     */
    @Nullable
    PathNode getCurrentNode();

    /**
     * Gets the next node after current.
     *
     * @return the next node, or null if none
     * @since 1.0.0
     */
    @Nullable
    PathNode getNextNode();

    /**
     * Gets a specific node by index.
     *
     * @param index the node index
     * @return the node
     * @throws IndexOutOfBoundsException if index is invalid
     * @since 1.0.0
     */
    @NotNull
    PathNode getNode(int index);

    /**
     * Gets all nodes in this path.
     *
     * @return the nodes
     * @since 1.0.0
     */
    @NotNull
    List<PathNode> getNodes();

    /**
     * Gets the remaining nodes to visit.
     *
     * @return the remaining nodes
     * @since 1.0.0
     */
    @NotNull
    List<PathNode> getRemainingNodes();

    /**
     * Gets the distance to the target.
     *
     * @return the distance
     * @since 1.0.0
     */
    double getDistanceToTarget();

    /**
     * Gets the remaining distance to travel.
     *
     * @return the remaining distance
     * @since 1.0.0
     */
    double getRemainingDistance();

    /**
     * Advances to the next node.
     *
     * @since 1.0.0
     */
    void advance();

    /**
     * Checks if this path can reach its target.
     *
     * @return true if reachable
     * @since 1.0.0
     */
    boolean canReachTarget();

    /**
     * Checks if this path reaches within a distance of target.
     *
     * @param distance the distance threshold
     * @return true if within distance
     * @since 1.0.0
     */
    boolean reachesWithin(double distance);

    /**
     * Represents a single node (waypoint) in a path.
     *
     * @since 1.0.0
     */
    interface PathNode {
        /**
         * Gets the X coordinate.
         *
         * @return the X coordinate
         */
        int getX();

        /**
         * Gets the Y coordinate.
         *
         * @return the Y coordinate
         */
        int getY();

        /**
         * Gets the Z coordinate.
         *
         * @return the Z coordinate
         */
        int getZ();

        /**
         * Gets this node's location.
         *
         * @param <T> the location type
         * @return the location
         */
        @NotNull
        <T> T getLocation();

        /**
         * Gets the node type.
         *
         * @return the type
         */
        @NotNull
        NavigationController.PathType getType();

        /**
         * Gets the movement cost to reach this node.
         *
         * @return the cost
         */
        float getCost();

        /**
         * Gets the total cost (g + h for A* pathfinding).
         *
         * @return the total cost
         */
        float getTotalCost();

        /**
         * Checks if this node is walkable.
         *
         * @return true if walkable
         */
        boolean isWalkable();

        /**
         * Gets the previous node in the path.
         *
         * @return the previous node, or null
         */
        @Nullable
        PathNode getPrevious();

        /**
         * Checks if this node is inside a closed space.
         *
         * @return true if closed
         */
        boolean isClosed();
    }
}
