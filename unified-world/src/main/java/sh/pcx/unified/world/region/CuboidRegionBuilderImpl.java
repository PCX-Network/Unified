/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.CuboidRegion;
import sh.pcx.unified.region.CuboidRegionBuilder;
import sh.pcx.unified.region.RegionService;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Implementation of {@link CuboidRegionBuilder}.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class CuboidRegionBuilderImpl
        extends AbstractRegionBuilder<CuboidRegionBuilder, CuboidRegion>
        implements CuboidRegionBuilder {

    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private boolean boundsSet = false;

    /**
     * Creates a new cuboid region builder.
     *
     * @param name    the region name
     * @param service the region service
     */
    public CuboidRegionBuilderImpl(@NotNull String name, @NotNull RegionService service) {
        super(name, service);
    }

    @Override
    @NotNull
    public CuboidRegionBuilder min(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");
        return min(location.x(), location.y(), location.z());
    }

    @Override
    @NotNull
    public CuboidRegionBuilder min(double x, double y, double z) {
        this.minX = x;
        this.minY = y;
        this.minZ = z;
        this.boundsSet = true;
        return this;
    }

    @Override
    @NotNull
    public CuboidRegionBuilder max(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");
        return max(location.x(), location.y(), location.z());
    }

    @Override
    @NotNull
    public CuboidRegionBuilder max(double x, double y, double z) {
        this.maxX = x;
        this.maxY = y;
        this.maxZ = z;
        this.boundsSet = true;
        return this;
    }

    @Override
    @NotNull
    public CuboidRegionBuilder corners(@NotNull UnifiedLocation corner1, @NotNull UnifiedLocation corner2) {
        Objects.requireNonNull(corner1, "corner1 cannot be null");
        Objects.requireNonNull(corner2, "corner2 cannot be null");

        this.minX = Math.min(corner1.x(), corner2.x());
        this.minY = Math.min(corner1.y(), corner2.y());
        this.minZ = Math.min(corner1.z(), corner2.z());
        this.maxX = Math.max(corner1.x(), corner2.x());
        this.maxY = Math.max(corner1.y(), corner2.y());
        this.maxZ = Math.max(corner1.z(), corner2.z());
        this.boundsSet = true;

        return this;
    }

    @Override
    @NotNull
    public CuboidRegionBuilder centered(@NotNull UnifiedLocation center, int width, int height, int length) {
        Objects.requireNonNull(center, "center cannot be null");

        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double halfLength = length / 2.0;

        this.minX = center.x() - halfWidth;
        this.minY = center.y() - halfHeight;
        this.minZ = center.z() - halfLength;
        this.maxX = center.x() + halfWidth;
        this.maxY = center.y() + halfHeight;
        this.maxZ = center.z() + halfLength;
        this.boundsSet = true;

        return this;
    }

    @Override
    protected void validate() {
        super.validate();
        if (!boundsSet) {
            throw new IllegalStateException("Region bounds must be set (use min/max, corners, or centered)");
        }
    }

    @Override
    @NotNull
    public CuboidRegion build() {
        validate();

        UnifiedLocation min = new UnifiedLocation(world, minX, minY, minZ);
        UnifiedLocation max = new UnifiedLocation(world, maxX, maxY, maxZ);

        CuboidRegionImpl region = new CuboidRegionImpl(name, world, min, max);
        applyCommonProperties(region);

        return region;
    }
}
