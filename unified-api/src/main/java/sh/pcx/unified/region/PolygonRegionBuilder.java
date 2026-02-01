/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Builder for creating polygonal regions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a triangular region point by point
 * PolygonRegion triangle = regions.polygon("triangle")
 *     .world(world)
 *     .point(0, 0)
 *     .point(100, 0)
 *     .point(50, 100)
 *     .minY(60)
 *     .maxY(128)
 *     .create();
 *
 * // Create from a list of points
 * PolygonRegion complex = regions.polygon("complex")
 *     .world(world)
 *     .points(List.of(
 *         new int[]{0, 0},
 *         new int[]{50, 0},
 *         new int[]{75, 25},
 *         new int[]{75, 75}
 *     ))
 *     .minY(60)
 *     .maxY(128)
 *     .create();
 *
 * // Create from locations (uses X and Z)
 * PolygonRegion fromLocs = regions.polygon("from-locs")
 *     .world(world)
 *     .pointsFromLocations(locationList)
 *     .minY(60)
 *     .maxY(128)
 *     .create();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PolygonRegion
 * @see RegionService#polygon(String)
 */
public interface PolygonRegionBuilder extends RegionBuilder<PolygonRegionBuilder, PolygonRegion> {

    /**
     * Adds a point to the polygon.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder point(int x, int z);

    /**
     * Adds a point to the polygon from a location.
     *
     * <p>Uses the floored X and Z coordinates.
     *
     * @param location the location
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default PolygonRegionBuilder point(@NotNull UnifiedLocation location) {
        return point(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Sets all points of the polygon.
     *
     * <p>Each array should have exactly 2 elements: [x, z].
     *
     * @param points the polygon points
     * @return this builder
     * @throws IllegalArgumentException if any point array doesn't have 2 elements
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder points(@NotNull List<int[]> points);

    /**
     * Sets all points from a list of locations.
     *
     * <p>Uses the floored X and Z coordinates of each location.
     *
     * @param locations the locations defining the polygon
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder pointsFromLocations(@NotNull List<UnifiedLocation> locations);

    /**
     * Sets the minimum Y coordinate.
     *
     * @param minY the minimum Y level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder minY(int minY);

    /**
     * Sets the maximum Y coordinate.
     *
     * @param maxY the maximum Y level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder maxY(int maxY);

    /**
     * Sets both Y bounds at once.
     *
     * @param minY the minimum Y level
     * @param maxY the maximum Y level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default PolygonRegionBuilder yBounds(int minY, int maxY) {
        return minY(minY).maxY(maxY);
    }

    /**
     * Sets the height of the polygon region from the minimum Y.
     *
     * <p>Requires that minY has already been set.
     *
     * @param height the height in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder height(int height);

    /**
     * Clears all previously added points.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder clearPoints();
}
