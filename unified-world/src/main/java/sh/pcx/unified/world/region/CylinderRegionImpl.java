/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.CylinderRegion;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Default implementation of {@link CylinderRegion}.
 *
 * <p>Represents a vertical cylindrical region.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class CylinderRegionImpl extends AbstractRegion implements CylinderRegion {

    private volatile double centerX, centerZ;
    private volatile double radius;
    private volatile double radiusSquared;
    private volatile int minY, maxY;

    /**
     * Creates a new cylinder region.
     *
     * @param id     the region UUID
     * @param name   the region name
     * @param world  the world
     * @param center the center location (uses X, Z and Y for minY)
     * @param radius the radius
     * @param minY   the minimum Y coordinate
     * @param maxY   the maximum Y coordinate
     */
    public CylinderRegionImpl(
            @NotNull UUID id,
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull UnifiedLocation center,
            double radius,
            int minY,
            int maxY
    ) {
        super(id, name, world);
        Objects.requireNonNull(center, "center cannot be null");
        this.centerX = center.x();
        this.centerZ = center.z();
        setRadius(radius);
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    /**
     * Creates a new cylinder region with auto-generated UUID.
     *
     * @param name   the region name
     * @param world  the world
     * @param center the center location
     * @param radius the radius
     * @param minY   the minimum Y coordinate
     * @param maxY   the maximum Y coordinate
     */
    public CylinderRegionImpl(
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull UnifiedLocation center,
            double radius,
            int minY,
            int maxY
    ) {
        this(UUID.randomUUID(), name, world, center, radius, minY, maxY);
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
    public int getMinY() {
        return minY;
    }

    @Override
    public void setMinY(int minY) {
        if (minY > maxY) {
            throw new IllegalArgumentException("minY cannot be greater than maxY");
        }
        this.minY = minY;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public void setMaxY(int maxY) {
        if (maxY < minY) {
            throw new IllegalArgumentException("maxY cannot be less than minY");
        }
        this.maxY = maxY;
    }

    @Override
    @NotNull
    public UnifiedLocation getCenter() {
        return new UnifiedLocation(world, centerX, (minY + maxY) / 2.0, centerZ);
    }

    @Override
    public void setCenter(@NotNull UnifiedLocation center) {
        Objects.requireNonNull(center, "center cannot be null");
        this.centerX = center.x();
        this.centerZ = center.z();
    }

    @Override
    public boolean contains(double x, double y, double z) {
        // Check Y bounds
        if (y < minY || y > maxY) {
            return false;
        }
        // Check 2D radius
        double dx = x - centerX;
        double dz = z - centerZ;
        return (dx * dx + dz * dz) <= radiusSquared;
    }

    @Override
    @NotNull
    public UnifiedLocation getMinimumPoint() {
        return new UnifiedLocation(world, centerX - radius, minY, centerZ - radius);
    }

    @Override
    @NotNull
    public UnifiedLocation getMaximumPoint() {
        return new UnifiedLocation(world, centerX + radius, maxY, centerZ + radius);
    }

    @Override
    public long getVolume() {
        return (long) (Math.PI * radius * radius * getHeight());
    }

    @Override
    public String toString() {
        return "CylinderRegion[" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", world=" + world.getName() +
            ", center=(" + centerX + ", " + centerZ + ")" +
            ", radius=" + radius +
            ", minY=" + minY +
            ", maxY=" + maxY +
            ", priority=" + priority +
            ']';
    }
}
