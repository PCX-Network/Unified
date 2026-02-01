/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for creating cuboid (box-shaped) regions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CuboidRegion spawn = regions.cuboid("spawn")
 *     .world(world)
 *     .min(new UnifiedLocation(world, -50, 60, -50))
 *     .max(new UnifiedLocation(world, 50, 120, 50))
 *     .flag(RegionFlag.PVP, false)
 *     .flag(RegionFlag.BUILD, false)
 *     .priority(10)
 *     .create();
 *
 * // Or using corner points
 * CuboidRegion area = regions.cuboid("area")
 *     .world(world)
 *     .corners(cornerA, cornerB)  // Auto-calculates min/max
 *     .create();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CuboidRegion
 * @see RegionService#cuboid(String)
 */
public interface CuboidRegionBuilder extends RegionBuilder<CuboidRegionBuilder, CuboidRegion> {

    /**
     * Sets the minimum corner of the cuboid.
     *
     * @param location the minimum corner location
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CuboidRegionBuilder min(@NotNull UnifiedLocation location);

    /**
     * Sets the minimum corner of the cuboid using coordinates.
     *
     * @param x the minimum X coordinate
     * @param y the minimum Y coordinate
     * @param z the minimum Z coordinate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CuboidRegionBuilder min(double x, double y, double z);

    /**
     * Sets the maximum corner of the cuboid.
     *
     * @param location the maximum corner location
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CuboidRegionBuilder max(@NotNull UnifiedLocation location);

    /**
     * Sets the maximum corner of the cuboid using coordinates.
     *
     * @param x the maximum X coordinate
     * @param y the maximum Y coordinate
     * @param z the maximum Z coordinate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CuboidRegionBuilder max(double x, double y, double z);

    /**
     * Sets both corners of the cuboid.
     *
     * <p>The builder will automatically determine which corner is
     * minimum and which is maximum.
     *
     * @param corner1 the first corner
     * @param corner2 the opposite corner
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CuboidRegionBuilder corners(@NotNull UnifiedLocation corner1, @NotNull UnifiedLocation corner2);

    /**
     * Creates a cuboid centered at a location with specified dimensions.
     *
     * @param center the center location
     * @param width  the width (X-axis)
     * @param height the height (Y-axis)
     * @param length the length (Z-axis)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CuboidRegionBuilder centered(@NotNull UnifiedLocation center, int width, int height, int length);

    /**
     * Creates a cuboid centered at a location with uniform dimensions.
     *
     * @param center the center location
     * @param size   the size in all directions
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default CuboidRegionBuilder centered(@NotNull UnifiedLocation center, int size) {
        return centered(center, size, size, size);
    }
}
