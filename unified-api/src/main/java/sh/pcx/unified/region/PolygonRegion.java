/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A polygonal region defined by 2D vertices extruded vertically.
 *
 * <p>Polygon regions are defined by a series of 2D points (X, Z) that form
 * a closed polygon, which is then extruded between minimum and maximum Y
 * coordinates. This allows for complex, non-rectangular region shapes.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a triangular region
 * PolygonRegion triangle = regions.polygon("triangle")
 *     .world(world)
 *     .point(0, 0)
 *     .point(100, 0)
 *     .point(50, 100)
 *     .minY(60)
 *     .maxY(128)
 *     .create();
 *
 * // Create a complex region
 * PolygonRegion complex = regions.polygon("complex")
 *     .world(world)
 *     .points(List.of(
 *         new int[]{0, 0},
 *         new int[]{50, 0},
 *         new int[]{75, 25},
 *         new int[]{75, 75},
 *         new int[]{50, 100},
 *         new int[]{0, 100},
 *         new int[]{-25, 50}
 *     ))
 *     .minY(60)
 *     .maxY(128)
 *     .create();
 * }</pre>
 *
 * <h2>Point Ordering</h2>
 * <p>Points can be specified in either clockwise or counter-clockwise order.
 * The polygon is automatically closed between the last and first points.
 * At least 3 points are required to form a valid polygon.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Region
 * @see RegionService#polygon(String)
 */
public interface PolygonRegion extends Region {

    /**
     * {@inheritDoc}
     *
     * @return always {@link RegionType#POLYGON}
     */
    @Override
    @NotNull
    default RegionType getType() {
        return RegionType.POLYGON;
    }

    /**
     * Returns the vertices of this polygon as 2D points (X, Z).
     *
     * <p>The returned list is unmodifiable. Each point is a 2-element array
     * where index 0 is X and index 1 is Z.
     *
     * @return an unmodifiable list of polygon vertices
     * @since 1.0.0
     */
    @NotNull
    List<int[]> getPoints();

    /**
     * Returns the number of vertices in this polygon.
     *
     * @return the vertex count
     * @since 1.0.0
     */
    default int getPointCount() {
        return getPoints().size();
    }

    /**
     * Adds a point to this polygon.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @since 1.0.0
     */
    void addPoint(int x, int z);

    /**
     * Removes a point from this polygon by index.
     *
     * @param index the index of the point to remove
     * @throws IndexOutOfBoundsException if index is invalid
     * @throws IllegalStateException     if removing would leave less than 3 points
     * @since 1.0.0
     */
    void removePoint(int index);

    /**
     * Clears all points from this polygon.
     *
     * <p>After calling this method, new points must be added before
     * the region can be used for containment checks.
     *
     * @since 1.0.0
     */
    void clearPoints();

    /**
     * Sets all points of this polygon.
     *
     * @param points the new points (each as [x, z] arrays)
     * @throws IllegalArgumentException if less than 3 points provided
     * @since 1.0.0
     */
    void setPoints(@NotNull List<int[]> points);

    /**
     * Returns the minimum Y coordinate of this polygon.
     *
     * @return the minimum Y level
     * @since 1.0.0
     */
    int getMinY();

    /**
     * Sets the minimum Y coordinate of this polygon.
     *
     * @param minY the new minimum Y level
     * @since 1.0.0
     */
    void setMinY(int minY);

    /**
     * Returns the maximum Y coordinate of this polygon.
     *
     * @return the maximum Y level
     * @since 1.0.0
     */
    int getMaxY();

    /**
     * Sets the maximum Y coordinate of this polygon.
     *
     * @param maxY the new maximum Y level
     * @since 1.0.0
     */
    void setMaxY(int maxY);

    /**
     * Returns the height of this polygon region.
     *
     * @return the height in blocks
     * @since 1.0.0
     */
    default int getHeight() {
        return getMaxY() - getMinY() + 1;
    }

    /**
     * Calculates the 2D area of this polygon using the shoelace formula.
     *
     * @return the area in square blocks
     * @since 1.0.0
     */
    default double getArea() {
        List<int[]> points = getPoints();
        int n = points.size();
        if (n < 3) return 0;

        double area = 0;
        for (int i = 0; i < n; i++) {
            int[] current = points.get(i);
            int[] next = points.get((i + 1) % n);
            area += (double) current[0] * next[1];
            area -= (double) next[0] * current[1];
        }
        return Math.abs(area) / 2.0;
    }

    /**
     * {@inheritDoc}
     *
     * @return the volume (area * height)
     */
    @Override
    default long getVolume() {
        return (long) (getArea() * getHeight());
    }

    /**
     * Checks if this polygon is convex.
     *
     * <p>Convex polygons allow for faster containment checks.
     *
     * @return true if the polygon is convex
     * @since 1.0.0
     */
    boolean isConvex();

    /**
     * {@inheritDoc}
     *
     * <p>Uses point-in-polygon algorithm with Y bounds check.
     */
    @Override
    default boolean contains(@NotNull UnifiedLocation location) {
        if (!location.sameWorld(getCenter())) {
            return false;
        }
        return contains(location.x(), location.y(), location.z());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean contains(double x, double y, double z);
}
