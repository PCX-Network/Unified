/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for creating cylindrical regions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CylinderRegion arena = regions.cylinder("arena")
 *     .center(arenaCenter)
 *     .radius(50)
 *     .minY(60)
 *     .maxY(128)
 *     .flag(RegionFlag.PVP, true)
 *     .create();
 *
 * // Create with height from center
 * CylinderRegion tower = regions.cylinder("tower")
 *     .center(baseCenter)
 *     .radius(10)
 *     .height(50)  // From minY to minY + 50
 *     .create();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CylinderRegion
 * @see RegionService#cylinder(String)
 */
public interface CylinderRegionBuilder extends RegionBuilder<CylinderRegionBuilder, CylinderRegion> {

    /**
     * Sets the center of the cylinder (X and Z coordinates).
     *
     * <p>The Y coordinate of the location is used as the base (minY).
     *
     * @param location the center location
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CylinderRegionBuilder center(@NotNull UnifiedLocation location);

    /**
     * Sets the center of the cylinder using coordinates.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CylinderRegionBuilder center(double x, double z);

    /**
     * Sets the radius of the cylinder.
     *
     * @param radius the radius in blocks
     * @return this builder
     * @throws IllegalArgumentException if radius is negative
     * @since 1.0.0
     */
    @NotNull
    CylinderRegionBuilder radius(double radius);

    /**
     * Sets the radius of the cylinder using integer value.
     *
     * @param radius the radius in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default CylinderRegionBuilder radius(int radius) {
        return radius((double) radius);
    }

    /**
     * Sets the diameter of the cylinder (radius = diameter / 2).
     *
     * @param diameter the diameter in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default CylinderRegionBuilder diameter(double diameter) {
        return radius(diameter / 2.0);
    }

    /**
     * Sets the minimum Y coordinate.
     *
     * @param minY the minimum Y level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CylinderRegionBuilder minY(int minY);

    /**
     * Sets the maximum Y coordinate.
     *
     * @param maxY the maximum Y level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CylinderRegionBuilder maxY(int maxY);

    /**
     * Sets both Y bounds at once.
     *
     * @param minY the minimum Y level
     * @param maxY the maximum Y level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default CylinderRegionBuilder yBounds(int minY, int maxY) {
        return minY(minY).maxY(maxY);
    }

    /**
     * Sets the height of the cylinder from the minimum Y.
     *
     * <p>Requires that minY has already been set.
     *
     * @param height the height in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CylinderRegionBuilder height(int height);
}
