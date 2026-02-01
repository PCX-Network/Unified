/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

/**
 * A global region that encompasses an entire world.
 *
 * <p>Global regions are special regions that cover the entire world. They are
 * typically used as default/fallback regions to define base behaviors for a
 * world when no other region applies. Each world can have at most one global
 * region.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get or create the global region for a world
 * GlobalRegion worldDefaults = regions.global(world);
 *
 * // Set default flags for the entire world
 * worldDefaults.setFlag(RegionFlag.PVP, true);
 * worldDefaults.setFlag(RegionFlag.MOB_SPAWNING, true);
 * worldDefaults.setFlag(RegionFlag.BUILD, true);
 *
 * // Global region always has lowest priority
 * assert worldDefaults.getPriority() == Integer.MIN_VALUE;
 * }</pre>
 *
 * <h2>Priority</h2>
 * <p>Global regions always have the lowest possible priority
 * ({@code Integer.MIN_VALUE}), ensuring that any other region will take
 * precedence. This cannot be changed.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Region
 * @see RegionService#global(UnifiedWorld)
 */
public interface GlobalRegion extends Region {

    /**
     * The fixed priority for global regions.
     */
    int GLOBAL_PRIORITY = Integer.MIN_VALUE;

    /**
     * The standard name prefix for global regions.
     */
    String NAME_PREFIX = "__global__";

    /**
     * {@inheritDoc}
     *
     * @return always {@link RegionType#GLOBAL}
     */
    @Override
    @NotNull
    default RegionType getType() {
        return RegionType.GLOBAL;
    }

    /**
     * {@inheritDoc}
     *
     * @return always {@link #GLOBAL_PRIORITY}
     */
    @Override
    default int getPriority() {
        return GLOBAL_PRIORITY;
    }

    /**
     * Global regions cannot have their priority changed.
     *
     * @param priority ignored
     * @throws UnsupportedOperationException always
     * @since 1.0.0
     */
    @Override
    default void setPriority(int priority) {
        throw new UnsupportedOperationException("Global regions have fixed priority");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Global regions contain all locations in their world.
     */
    @Override
    default boolean contains(@NotNull UnifiedLocation location) {
        return location.sameWorld(getCenter());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Global regions contain all coordinates.
     */
    @Override
    default boolean contains(double x, double y, double z) {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the maximum possible volume.
     */
    @Override
    default long getVolume() {
        return Long.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the world's spawn location as the center.
     */
    @Override
    @NotNull
    default UnifiedLocation getCenter() {
        return getWorld().getSpawnLocation();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the minimum world bounds.
     */
    @Override
    @NotNull
    default UnifiedLocation getMinimumPoint() {
        UnifiedWorld world = getWorld();
        return new UnifiedLocation(world,
            Double.MIN_VALUE, world.getMinHeight(), Double.MIN_VALUE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the maximum world bounds.
     */
    @Override
    @NotNull
    default UnifiedLocation getMaximumPoint() {
        UnifiedWorld world = getWorld();
        return new UnifiedLocation(world,
            Double.MAX_VALUE, world.getMaxHeight(), Double.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Global regions always intersect with any region in the same world.
     */
    @Override
    default boolean intersects(@NotNull Region other) {
        return other.getWorld().equals(getWorld());
    }
}
