/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.CuboidRegion;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Default implementation of {@link CuboidRegion}.
 *
 * <p>Represents an axis-aligned bounding box (AABB) region.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class CuboidRegionImpl extends AbstractRegion implements CuboidRegion {

    private volatile double minX, minY, minZ;
    private volatile double maxX, maxY, maxZ;

    /**
     * Creates a new cuboid region.
     *
     * @param id    the region UUID
     * @param name  the region name
     * @param world the world
     * @param min   the minimum corner
     * @param max   the maximum corner
     */
    public CuboidRegionImpl(
            @NotNull UUID id,
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull UnifiedLocation min,
            @NotNull UnifiedLocation max
    ) {
        super(id, name, world);
        setBoundsInternal(min, max);
    }

    /**
     * Creates a new cuboid region with auto-generated UUID.
     *
     * @param name  the region name
     * @param world the world
     * @param min   the minimum corner
     * @param max   the maximum corner
     */
    public CuboidRegionImpl(
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull UnifiedLocation min,
            @NotNull UnifiedLocation max
    ) {
        this(UUID.randomUUID(), name, world, min, max);
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    @Override
    @NotNull
    public UnifiedLocation getMinimumPoint() {
        return new UnifiedLocation(world, minX, minY, minZ);
    }

    @Override
    @NotNull
    public UnifiedLocation getMaximumPoint() {
        return new UnifiedLocation(world, maxX, maxY, maxZ);
    }

    @Override
    @NotNull
    public UnifiedLocation getCenter() {
        return new UnifiedLocation(
            world,
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0
        );
    }

    @Override
    public void expand(int x, int y, int z) {
        minX -= x;
        minY -= y;
        minZ -= z;
        maxX += x;
        maxY += y;
        maxZ += z;
    }

    @Override
    public void contract(int x, int y, int z) {
        double newMinX = minX + x;
        double newMinY = minY + y;
        double newMinZ = minZ + z;
        double newMaxX = maxX - x;
        double newMaxY = maxY - y;
        double newMaxZ = maxZ - z;

        // Ensure valid bounds
        if (newMinX > newMaxX || newMinY > newMaxY || newMinZ > newMaxZ) {
            throw new IllegalArgumentException("Contraction would result in invalid region");
        }

        minX = newMinX;
        minY = newMinY;
        minZ = newMinZ;
        maxX = newMaxX;
        maxY = newMaxY;
        maxZ = newMaxZ;
    }

    @Override
    public void shift(int x, int y, int z) {
        minX += x;
        minY += y;
        minZ += z;
        maxX += x;
        maxY += y;
        maxZ += z;
    }

    @Override
    public void setBounds(@NotNull UnifiedLocation min, @NotNull UnifiedLocation max) {
        Objects.requireNonNull(min, "min cannot be null");
        Objects.requireNonNull(max, "max cannot be null");
        setBoundsInternal(min, max);
    }

    private void setBoundsInternal(@NotNull UnifiedLocation min, @NotNull UnifiedLocation max) {
        // Normalize bounds (ensure min <= max)
        this.minX = Math.min(min.x(), max.x());
        this.minY = Math.min(min.y(), max.y());
        this.minZ = Math.min(min.z(), max.z());
        this.maxX = Math.max(min.x(), max.x());
        this.maxY = Math.max(min.y(), max.y());
        this.maxZ = Math.max(min.z(), max.z());
    }

    @Override
    public String toString() {
        return "CuboidRegion[" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", world=" + world.getName() +
            ", min=(" + minX + ", " + minY + ", " + minZ + ")" +
            ", max=(" + maxX + ", " + maxY + ", " + maxZ + ")" +
            ", priority=" + priority +
            ']';
    }
}
