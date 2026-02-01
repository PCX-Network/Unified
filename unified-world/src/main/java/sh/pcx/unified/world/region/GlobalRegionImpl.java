/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.GlobalRegion;
import sh.pcx.unified.region.Region;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link GlobalRegion}.
 *
 * <p>Represents a region that encompasses an entire world.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class GlobalRegionImpl extends AbstractRegion implements GlobalRegion {

    /**
     * Creates a new global region.
     *
     * @param id    the region UUID
     * @param world the world
     */
    public GlobalRegionImpl(@NotNull UUID id, @NotNull UnifiedWorld world) {
        super(id, NAME_PREFIX + world.getName(), world);
        this.priority = GLOBAL_PRIORITY;
    }

    /**
     * Creates a new global region with auto-generated UUID.
     *
     * @param world the world
     */
    public GlobalRegionImpl(@NotNull UnifiedWorld world) {
        this(UUID.randomUUID(), world);
    }

    @Override
    public int getPriority() {
        return GLOBAL_PRIORITY;
    }

    @Override
    public void setPriority(int priority) {
        throw new UnsupportedOperationException("Global regions have fixed priority");
    }

    @Override
    public void setParent(Region parent) {
        throw new UnsupportedOperationException("Global regions cannot have parents");
    }

    @Override
    @NotNull
    public Optional<Region> getParent() {
        return Optional.empty();
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return true; // Global region contains all coordinates
    }

    @Override
    @NotNull
    public UnifiedLocation getMinimumPoint() {
        return new UnifiedLocation(world, Double.MIN_VALUE, world.getMinHeight(), Double.MIN_VALUE);
    }

    @Override
    @NotNull
    public UnifiedLocation getMaximumPoint() {
        return new UnifiedLocation(world, Double.MAX_VALUE, world.getMaxHeight(), Double.MAX_VALUE);
    }

    @Override
    @NotNull
    public UnifiedLocation getCenter() {
        return world.getSpawnLocation();
    }

    @Override
    public long getVolume() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean intersects(@NotNull Region other) {
        return other.getWorld().equals(world);
    }

    @Override
    public String toString() {
        return "GlobalRegion[" +
            "id=" + id +
            ", world=" + world.getName() +
            ']';
    }
}
