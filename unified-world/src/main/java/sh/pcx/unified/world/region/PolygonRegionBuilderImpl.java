/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.PolygonRegion;
import sh.pcx.unified.region.PolygonRegionBuilder;
import sh.pcx.unified.region.RegionService;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link PolygonRegionBuilder}.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class PolygonRegionBuilderImpl
        extends AbstractRegionBuilder<PolygonRegionBuilder, PolygonRegion>
        implements PolygonRegionBuilder {

    private final List<int[]> points = new ArrayList<>();
    private int minY = Integer.MIN_VALUE;
    private int maxY = Integer.MAX_VALUE;
    private boolean minYSet = false;
    private boolean maxYSet = false;

    /**
     * Creates a new polygon region builder.
     *
     * @param name    the region name
     * @param service the region service
     */
    public PolygonRegionBuilderImpl(@NotNull String name, @NotNull RegionService service) {
        super(name, service);
    }

    @Override
    @NotNull
    public PolygonRegionBuilder point(int x, int z) {
        points.add(new int[]{x, z});
        return this;
    }

    @Override
    @NotNull
    public PolygonRegionBuilder points(@NotNull List<int[]> newPoints) {
        Objects.requireNonNull(newPoints, "points cannot be null");

        for (int[] point : newPoints) {
            if (point == null || point.length != 2) {
                throw new IllegalArgumentException("Each point must be a 2-element array [x, z]");
            }
            points.add(new int[]{point[0], point[1]});
        }

        return this;
    }

    @Override
    @NotNull
    public PolygonRegionBuilder pointsFromLocations(@NotNull List<UnifiedLocation> locations) {
        Objects.requireNonNull(locations, "locations cannot be null");

        for (UnifiedLocation loc : locations) {
            if (loc == null) {
                throw new IllegalArgumentException("Location cannot be null");
            }
            points.add(new int[]{loc.getBlockX(), loc.getBlockZ()});
        }

        return this;
    }

    @Override
    @NotNull
    public PolygonRegionBuilder minY(int minY) {
        this.minY = minY;
        this.minYSet = true;
        return this;
    }

    @Override
    @NotNull
    public PolygonRegionBuilder maxY(int maxY) {
        this.maxY = maxY;
        this.maxYSet = true;
        return this;
    }

    @Override
    @NotNull
    public PolygonRegionBuilder height(int height) {
        if (!minYSet) {
            throw new IllegalStateException("minY must be set before height");
        }
        this.maxY = this.minY + height - 1;
        this.maxYSet = true;
        return this;
    }

    @Override
    @NotNull
    public PolygonRegionBuilder clearPoints() {
        points.clear();
        return this;
    }

    @Override
    protected void validate() {
        super.validate();
        if (points.size() < 3) {
            throw new IllegalStateException("Polygon must have at least 3 points");
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
    public PolygonRegion build() {
        validate();

        PolygonRegionImpl region = new PolygonRegionImpl(name, world, points, minY, maxY);
        applyCommonProperties(region);

        return region;
    }
}
