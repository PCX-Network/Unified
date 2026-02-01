/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.CylinderRegion;
import sh.pcx.unified.region.CylinderRegionBuilder;
import sh.pcx.unified.region.RegionService;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Implementation of {@link CylinderRegionBuilder}.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class CylinderRegionBuilderImpl
        extends AbstractRegionBuilder<CylinderRegionBuilder, CylinderRegion>
        implements CylinderRegionBuilder {

    private double centerX, centerZ;
    private double radius = -1;
    private int minY = Integer.MIN_VALUE;
    private int maxY = Integer.MAX_VALUE;
    private boolean centerSet = false;
    private boolean minYSet = false;
    private boolean maxYSet = false;

    /**
     * Creates a new cylinder region builder.
     *
     * @param name    the region name
     * @param service the region service
     */
    public CylinderRegionBuilderImpl(@NotNull String name, @NotNull RegionService service) {
        super(name, service);
    }

    @Override
    @NotNull
    public CylinderRegionBuilder center(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");
        this.centerX = location.x();
        this.centerZ = location.z();
        this.centerSet = true;

        // Use location Y as minY if not already set
        if (!minYSet) {
            this.minY = location.getBlockY();
            this.minYSet = true;
        }

        return this;
    }

    @Override
    @NotNull
    public CylinderRegionBuilder center(double x, double z) {
        this.centerX = x;
        this.centerZ = z;
        this.centerSet = true;
        return this;
    }

    @Override
    @NotNull
    public CylinderRegionBuilder radius(double radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        this.radius = radius;
        return this;
    }

    @Override
    @NotNull
    public CylinderRegionBuilder minY(int minY) {
        this.minY = minY;
        this.minYSet = true;
        return this;
    }

    @Override
    @NotNull
    public CylinderRegionBuilder maxY(int maxY) {
        this.maxY = maxY;
        this.maxYSet = true;
        return this;
    }

    @Override
    @NotNull
    public CylinderRegionBuilder height(int height) {
        if (!minYSet) {
            throw new IllegalStateException("minY must be set before height");
        }
        this.maxY = this.minY + height - 1;
        this.maxYSet = true;
        return this;
    }

    @Override
    protected void validate() {
        super.validate();
        if (!centerSet) {
            throw new IllegalStateException("Center must be set");
        }
        if (radius < 0) {
            throw new IllegalStateException("Radius must be set");
        }
        if (!minYSet || !maxYSet) {
            throw new IllegalStateException("Both minY and maxY must be set");
        }
        if (minY > maxY) {
            throw new IllegalStateException("minY cannot be greater than maxY");
        }
    }

    @Override
    @NotNull
    public CylinderRegion build() {
        validate();

        UnifiedLocation center = new UnifiedLocation(world, centerX, (minY + maxY) / 2.0, centerZ);
        CylinderRegionImpl region = new CylinderRegionImpl(name, world, center, radius, minY, maxY);
        applyCommonProperties(region);

        return region;
    }
}
