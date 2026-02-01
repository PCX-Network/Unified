/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A cylindrical region defined by a center, radius, and vertical bounds.
 *
 * <p>Cylinder regions are vertical cylinders defined by a 2D center point (X, Z),
 * a radius, and minimum/maximum Y coordinates. They are useful for circular
 * areas that span specific height ranges.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a cylindrical arena
 * CylinderRegion arena = regions.cylinder("arena")
 *     .center(new UnifiedLocation(world, 100, 64, 100))
 *     .radius(50.0)
 *     .minY(60)
 *     .maxY(128)
 *     .create();
 *
 * // Check if player is in horizontal range
 * double distance = arena.horizontalDistanceFromCenter(playerLocation);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Region
 * @see RegionService#cylinder(String)
 */
public interface CylinderRegion extends Region {

    /**
     * {@inheritDoc}
     *
     * @return always {@link RegionType#CYLINDER}
     */
    @Override
    @NotNull
    default RegionType getType() {
        return RegionType.CYLINDER;
    }

    /**
     * Returns the radius of this cylinder.
     *
     * @return the radius in blocks
     * @since 1.0.0
     */
    double getRadius();

    /**
     * Sets the radius of this cylinder.
     *
     * @param radius the new radius
     * @throws IllegalArgumentException if radius is negative
     * @since 1.0.0
     */
    void setRadius(double radius);

    /**
     * Returns the minimum Y coordinate of this cylinder.
     *
     * @return the minimum Y level
     * @since 1.0.0
     */
    int getMinY();

    /**
     * Sets the minimum Y coordinate of this cylinder.
     *
     * @param minY the new minimum Y level
     * @since 1.0.0
     */
    void setMinY(int minY);

    /**
     * Returns the maximum Y coordinate of this cylinder.
     *
     * @return the maximum Y level
     * @since 1.0.0
     */
    int getMaxY();

    /**
     * Sets the maximum Y coordinate of this cylinder.
     *
     * @param maxY the new maximum Y level
     * @since 1.0.0
     */
    void setMaxY(int maxY);

    /**
     * Returns the height of this cylinder.
     *
     * @return the height in blocks
     * @since 1.0.0
     */
    default int getHeight() {
        return getMaxY() - getMinY() + 1;
    }

    /**
     * Sets the center point of this cylinder (X and Z coordinates).
     *
     * @param center the new center location
     * @since 1.0.0
     */
    void setCenter(@NotNull UnifiedLocation center);

    /**
     * Calculates the horizontal (2D) distance from the center to a location.
     *
     * @param location the location to measure to
     * @return the horizontal distance from the center
     * @since 1.0.0
     */
    default double horizontalDistanceFromCenter(@NotNull UnifiedLocation location) {
        UnifiedLocation center = getCenter();
        double dx = location.x() - center.x();
        double dz = location.z() - center.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses 2D distance calculation and Y bounds for containment.
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
    default boolean contains(double x, double y, double z) {
        // Check Y bounds
        if (y < getMinY() || y > getMaxY()) {
            return false;
        }
        // Check 2D radius
        UnifiedLocation center = getCenter();
        double dx = x - center.x();
        double dz = z - center.z();
        return (dx * dx + dz * dz) <= (getRadius() * getRadius());
    }

    /**
     * {@inheritDoc}
     *
     * @return the volume of the cylinder
     */
    @Override
    default long getVolume() {
        double r = getRadius();
        return (long) (Math.PI * r * r * getHeight());
    }

    /**
     * {@inheritDoc}
     *
     * @return the minimum point of the bounding box
     */
    @Override
    @NotNull
    default UnifiedLocation getMinimumPoint() {
        UnifiedLocation center = getCenter();
        double r = getRadius();
        return new UnifiedLocation(center.world(),
            center.x() - r, getMinY(), center.z() - r);
    }

    /**
     * {@inheritDoc}
     *
     * @return the maximum point of the bounding box
     */
    @Override
    @NotNull
    default UnifiedLocation getMaximumPoint() {
        UnifiedLocation center = getCenter();
        double r = getRadius();
        return new UnifiedLocation(center.world(),
            center.x() + r, getMaxY(), center.z() + r);
    }
}
