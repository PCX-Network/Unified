/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for creating spherical regions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * SphereRegion arena = regions.sphere("arena")
 *     .center(arenaCenter)
 *     .radius(30)
 *     .flag(RegionFlag.PVP, true)
 *     .flag(RegionFlag.HUNGER, false)
 *     .create();
 *
 * // Create using diameter
 * SphereRegion bubble = regions.sphere("bubble")
 *     .center(center)
 *     .diameter(100)
 *     .create();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SphereRegion
 * @see RegionService#sphere(String)
 */
public interface SphereRegionBuilder extends RegionBuilder<SphereRegionBuilder, SphereRegion> {

    /**
     * Sets the center of the sphere.
     *
     * @param location the center location
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SphereRegionBuilder center(@NotNull UnifiedLocation location);

    /**
     * Sets the center of the sphere using coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SphereRegionBuilder center(double x, double y, double z);

    /**
     * Sets the radius of the sphere.
     *
     * @param radius the radius in blocks
     * @return this builder
     * @throws IllegalArgumentException if radius is negative
     * @since 1.0.0
     */
    @NotNull
    SphereRegionBuilder radius(double radius);

    /**
     * Sets the radius of the sphere using integer value.
     *
     * @param radius the radius in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default SphereRegionBuilder radius(int radius) {
        return radius((double) radius);
    }

    /**
     * Sets the diameter of the sphere (radius = diameter / 2).
     *
     * @param diameter the diameter in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default SphereRegionBuilder diameter(double diameter) {
        return radius(diameter / 2.0);
    }
}
