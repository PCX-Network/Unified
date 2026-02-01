/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.SphereRegion;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Default implementation of {@link SphereRegion}.
 *
 * <p>Represents a spherical region defined by center and radius.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class SphereRegionImpl extends AbstractRegion implements SphereRegion {

    private volatile double centerX, centerY, centerZ;
    private volatile double radius;
    private volatile double radiusSquared;

    /**
     * Creates a new sphere region.
     *
     * @param id     the region UUID
     * @param name   the region name
     * @param world  the world
     * @param center the center location
     * @param radius the radius
     */
    public SphereRegionImpl(
            @NotNull UUID id,
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull UnifiedLocation center,
            double radius
    ) {
        super(id, name, world);
        Objects.requireNonNull(center, "center cannot be null");
        this.centerX = center.x();
        this.centerY = center.y();
        this.centerZ = center.z();
        setRadius(radius);
    }

    /**
     * Creates a new sphere region with auto-generated UUID.
     *
     * @param name   the region name
     * @param world  the world
     * @param center the center location
     * @param radius the radius
     */
    public SphereRegionImpl(
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull UnifiedLocation center,
            double radius
    ) {
        this(UUID.randomUUID(), name, world, center, radius);
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public void setRadius(double radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    @Override
    @NotNull
    public UnifiedLocation getCenter() {
        return new UnifiedLocation(world, centerX, centerY, centerZ);
    }

    @Override
    public void setCenter(@NotNull UnifiedLocation center) {
        Objects.requireNonNull(center, "center cannot be null");
        this.centerX = center.x();
        this.centerY = center.y();
        this.centerZ = center.z();
    }

    @Override
    public boolean contains(double x, double y, double z) {
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
    }

    @Override
    @NotNull
    public UnifiedLocation getMinimumPoint() {
        return new UnifiedLocation(world,
            centerX - radius,
            centerY - radius,
            centerZ - radius
        );
    }

    @Override
    @NotNull
    public UnifiedLocation getMaximumPoint() {
        return new UnifiedLocation(world,
            centerX + radius,
            centerY + radius,
            centerZ + radius
        );
    }

    @Override
    public long getVolume() {
        return (long) ((4.0 / 3.0) * Math.PI * radius * radius * radius);
    }

    @Override
    public String toString() {
        return "SphereRegion[" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", world=" + world.getName() +
            ", center=(" + centerX + ", " + centerY + ", " + centerZ + ")" +
            ", radius=" + radius +
            ", priority=" + priority +
            ']';
    }
}
