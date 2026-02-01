/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.RegionService;
import sh.pcx.unified.region.SphereRegion;
import sh.pcx.unified.region.SphereRegionBuilder;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Implementation of {@link SphereRegionBuilder}.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class SphereRegionBuilderImpl
        extends AbstractRegionBuilder<SphereRegionBuilder, SphereRegion>
        implements SphereRegionBuilder {

    private double centerX, centerY, centerZ;
    private double radius = -1;
    private boolean centerSet = false;

    /**
     * Creates a new sphere region builder.
     *
     * @param name    the region name
     * @param service the region service
     */
    public SphereRegionBuilderImpl(@NotNull String name, @NotNull RegionService service) {
        super(name, service);
    }

    @Override
    @NotNull
    public SphereRegionBuilder center(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");
        return center(location.x(), location.y(), location.z());
    }

    @Override
    @NotNull
    public SphereRegionBuilder center(double x, double y, double z) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.centerSet = true;
        return this;
    }

    @Override
    @NotNull
    public SphereRegionBuilder radius(double radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        this.radius = radius;
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
    }

    @Override
    @NotNull
    public SphereRegion build() {
        validate();

        UnifiedLocation center = new UnifiedLocation(world, centerX, centerY, centerZ);
        SphereRegionImpl region = new SphereRegionImpl(name, world, center, radius);
        applyCommonProperties(region);

        return region;
    }
}
