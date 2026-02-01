/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A cuboid (box-shaped) region defined by two corner points.
 *
 * <p>Cuboid regions are axis-aligned bounding boxes (AABBs) defined by
 * their minimum and maximum corner coordinates. They are the most efficient
 * region type for containment checks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a cuboid region
 * CuboidRegion region = regions.cuboid("my-region")
 *     .world(world)
 *     .min(new UnifiedLocation(world, 0, 60, 0))
 *     .max(new UnifiedLocation(world, 100, 120, 100))
 *     .create();
 *
 * // Expand the region
 * region.expand(5, 10, 5); // Expand by 5 in X, 10 in Y, 5 in Z
 *
 * // Contract the region
 * region.contract(2, 0, 2); // Contract by 2 in X and Z
 *
 * // Shift the region
 * region.shift(10, 0, 0); // Move 10 blocks in the X direction
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Region
 * @see RegionService#cuboid(String)
 */
public interface CuboidRegion extends Region {

    /**
     * {@inheritDoc}
     *
     * @return always {@link RegionType#CUBOID}
     */
    @Override
    @NotNull
    default RegionType getType() {
        return RegionType.CUBOID;
    }

    /**
     * Returns the width (X-axis size) of this region.
     *
     * @return the width in blocks
     * @since 1.0.0
     */
    default int getWidth() {
        return (int) Math.abs(getMaximumPoint().x() - getMinimumPoint().x()) + 1;
    }

    /**
     * Returns the height (Y-axis size) of this region.
     *
     * @return the height in blocks
     * @since 1.0.0
     */
    default int getHeight() {
        return (int) Math.abs(getMaximumPoint().y() - getMinimumPoint().y()) + 1;
    }

    /**
     * Returns the length (Z-axis size) of this region.
     *
     * @return the length in blocks
     * @since 1.0.0
     */
    default int getLength() {
        return (int) Math.abs(getMaximumPoint().z() - getMinimumPoint().z()) + 1;
    }

    /**
     * Expands this region in all directions by the specified amounts.
     *
     * @param x the amount to expand in the X direction (on both sides)
     * @param y the amount to expand in the Y direction (on both sides)
     * @param z the amount to expand in the Z direction (on both sides)
     * @since 1.0.0
     */
    void expand(int x, int y, int z);

    /**
     * Expands this region in all directions by the same amount.
     *
     * @param amount the amount to expand in all directions
     * @since 1.0.0
     */
    default void expand(int amount) {
        expand(amount, amount, amount);
    }

    /**
     * Contracts this region in all directions by the specified amounts.
     *
     * @param x the amount to contract in the X direction
     * @param y the amount to contract in the Y direction
     * @param z the amount to contract in the Z direction
     * @since 1.0.0
     */
    void contract(int x, int y, int z);

    /**
     * Contracts this region in all directions by the same amount.
     *
     * @param amount the amount to contract in all directions
     * @since 1.0.0
     */
    default void contract(int amount) {
        contract(amount, amount, amount);
    }

    /**
     * Shifts this region by the specified offset.
     *
     * @param x the X offset
     * @param y the Y offset
     * @param z the Z offset
     * @since 1.0.0
     */
    void shift(int x, int y, int z);

    /**
     * Sets new bounds for this region.
     *
     * @param min the new minimum point
     * @param max the new maximum point
     * @since 1.0.0
     */
    void setBounds(@NotNull UnifiedLocation min, @NotNull UnifiedLocation max);

    /**
     * {@inheritDoc}
     */
    @Override
    default long getVolume() {
        return (long) getWidth() * getHeight() * getLength();
    }
}
