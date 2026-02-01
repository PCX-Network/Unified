/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A spherical region defined by a center point and radius.
 *
 * <p>Sphere regions are defined by their center location and a radius.
 * The containment check uses distance calculations, making them slightly
 * more expensive than cuboid regions but providing a natural circular shape.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a sphere region
 * SphereRegion arena = regions.sphere("arena")
 *     .center(arenaCenter)
 *     .radius(30.0)
 *     .create();
 *
 * // Check distance from center
 * double distance = arena.distanceFromCenter(playerLocation);
 *
 * // Expand the sphere
 * arena.setRadius(arena.getRadius() + 10);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Region
 * @see RegionService#sphere(String)
 */
public interface SphereRegion extends Region {

    /**
     * {@inheritDoc}
     *
     * @return always {@link RegionType#SPHERE}
     */
    @Override
    @NotNull
    default RegionType getType() {
        return RegionType.SPHERE;
    }

    /**
     * Returns the radius of this sphere.
     *
     * @return the radius in blocks
     * @since 1.0.0
     */
    double getRadius();

    /**
     * Sets the radius of this sphere.
     *
     * @param radius the new radius
     * @throws IllegalArgumentException if radius is negative
     * @since 1.0.0
     */
    void setRadius(double radius);

    /**
     * Returns the center point of this sphere.
     *
     * @return the center location
     * @since 1.0.0
     */
    @Override
    @NotNull
    UnifiedLocation getCenter();

    /**
     * Sets the center point of this sphere.
     *
     * @param center the new center location
     * @since 1.0.0
     */
    void setCenter(@NotNull UnifiedLocation center);

    /**
     * Calculates the distance from the center to a location.
     *
     * @param location the location to measure to
     * @return the distance from the center
     * @since 1.0.0
     */
    default double distanceFromCenter(@NotNull UnifiedLocation location) {
        return getCenter().distance(location);
    }

    /**
     * Calculates how far a location is from the edge of the sphere.
     *
     * <p>Positive values indicate the location is inside the sphere,
     * negative values indicate it is outside.
     *
     * @param location the location to check
     * @return the distance from the edge (positive = inside)
     * @since 1.0.0
     */
    default double distanceFromEdge(@NotNull UnifiedLocation location) {
        return getRadius() - distanceFromCenter(location);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses distance calculation for accurate spherical containment.
     */
    @Override
    default boolean contains(@NotNull UnifiedLocation location) {
        if (!location.sameWorld(getCenter())) {
            return false;
        }
        return distanceFromCenter(location) <= getRadius();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean contains(double x, double y, double z) {
        UnifiedLocation center = getCenter();
        double dx = x - center.x();
        double dy = y - center.y();
        double dz = z - center.z();
        return (dx * dx + dy * dy + dz * dz) <= (getRadius() * getRadius());
    }

    /**
     * {@inheritDoc}
     *
     * @return the volume of the sphere
     */
    @Override
    default long getVolume() {
        double r = getRadius();
        return (long) ((4.0 / 3.0) * Math.PI * r * r * r);
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
            center.x() - r, center.y() - r, center.z() - r);
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
            center.x() + r, center.y() + r, center.z() + r);
    }
}
