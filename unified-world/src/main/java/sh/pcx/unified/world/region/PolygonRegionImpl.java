/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.PolygonRegion;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of {@link PolygonRegion}.
 *
 * <p>Represents a 2D polygon extruded vertically between Y bounds.
 * Uses the ray casting algorithm for point-in-polygon tests.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class PolygonRegionImpl extends AbstractRegion implements PolygonRegion {

    private final List<int[]> points = new CopyOnWriteArrayList<>();
    private volatile int minY, maxY;
    private volatile Boolean convex = null; // Lazy computed

    /**
     * Creates a new polygon region.
     *
     * @param id     the region UUID
     * @param name   the region name
     * @param world  the world
     * @param points the polygon vertices
     * @param minY   the minimum Y coordinate
     * @param maxY   the maximum Y coordinate
     */
    public PolygonRegionImpl(
            @NotNull UUID id,
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull List<int[]> points,
            int minY,
            int maxY
    ) {
        super(id, name, world);
        setPoints(points);
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    /**
     * Creates a new polygon region with auto-generated UUID.
     *
     * @param name   the region name
     * @param world  the world
     * @param points the polygon vertices
     * @param minY   the minimum Y coordinate
     * @param maxY   the maximum Y coordinate
     */
    public PolygonRegionImpl(
            @NotNull String name,
            @NotNull UnifiedWorld world,
            @NotNull List<int[]> points,
            int minY,
            int maxY
    ) {
        this(UUID.randomUUID(), name, world, points, minY, maxY);
    }

    @Override
    @NotNull
    public List<int[]> getPoints() {
        List<int[]> copy = new ArrayList<>(points.size());
        for (int[] point : points) {
            copy.add(new int[]{point[0], point[1]});
        }
        return Collections.unmodifiableList(copy);
    }

    @Override
    public void addPoint(int x, int z) {
        points.add(new int[]{x, z});
        invalidateCache();
    }

    @Override
    public void removePoint(int index) {
        if (index < 0 || index >= points.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + points.size());
        }
        if (points.size() <= 3) {
            throw new IllegalStateException("Cannot remove point: polygon must have at least 3 points");
        }
        points.remove(index);
        invalidateCache();
    }

    @Override
    public void clearPoints() {
        points.clear();
        invalidateCache();
    }

    @Override
    public void setPoints(@NotNull List<int[]> newPoints) {
        Objects.requireNonNull(newPoints, "points cannot be null");
        if (newPoints.size() < 3) {
            throw new IllegalArgumentException("Polygon must have at least 3 points");
        }

        points.clear();
        for (int[] point : newPoints) {
            if (point == null || point.length != 2) {
                throw new IllegalArgumentException("Each point must be a 2-element array [x, z]");
            }
            points.add(new int[]{point[0], point[1]});
        }
        invalidateCache();
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
    public boolean isConvex() {
        Boolean cached = convex;
        if (cached != null) {
            return cached;
        }

        if (points.size() < 3) {
            return false;
        }

        boolean sign = false;
        int n = points.size();

        for (int i = 0; i < n; i++) {
            int[] p1 = points.get(i);
            int[] p2 = points.get((i + 1) % n);
            int[] p3 = points.get((i + 2) % n);

            int cross = (p2[0] - p1[0]) * (p3[1] - p2[1]) - (p2[1] - p1[1]) * (p3[0] - p2[0]);

            if (i == 0) {
                sign = cross > 0;
            } else if ((cross > 0) != sign) {
                convex = false;
                return false;
            }
        }

        convex = true;
        return true;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        // Check Y bounds
        if (y < minY || y > maxY) {
            return false;
        }

        // Ray casting algorithm for point-in-polygon
        return isPointInPolygon((int) Math.floor(x), (int) Math.floor(z));
    }

    /**
     * Ray casting algorithm for point-in-polygon test.
     */
    private boolean isPointInPolygon(int testX, int testZ) {
        int n = points.size();
        if (n < 3) {
            return false;
        }

        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            int[] pi = points.get(i);
            int[] pj = points.get(j);

            int xi = pi[0], zi = pi[1];
            int xj = pj[0], zj = pj[1];

            if (((zi > testZ) != (zj > testZ)) &&
                (testX < (xj - xi) * (testZ - zi) / (double) (zj - zi) + xi)) {
                inside = !inside;
            }
        }

        return inside;
    }

    @Override
    @NotNull
    public UnifiedLocation getMinimumPoint() {
        if (points.isEmpty()) {
            return new UnifiedLocation(world, 0, minY, 0);
        }

        int minPX = Integer.MAX_VALUE;
        int minPZ = Integer.MAX_VALUE;
        for (int[] point : points) {
            minPX = Math.min(minPX, point[0]);
            minPZ = Math.min(minPZ, point[1]);
        }

        return new UnifiedLocation(world, minPX, minY, minPZ);
    }

    @Override
    @NotNull
    public UnifiedLocation getMaximumPoint() {
        if (points.isEmpty()) {
            return new UnifiedLocation(world, 0, maxY, 0);
        }

        int maxPX = Integer.MIN_VALUE;
        int maxPZ = Integer.MIN_VALUE;
        for (int[] point : points) {
            maxPX = Math.max(maxPX, point[0]);
            maxPZ = Math.max(maxPZ, point[1]);
        }

        return new UnifiedLocation(world, maxPX, maxY, maxPZ);
    }

    @Override
    @NotNull
    public UnifiedLocation getCenter() {
        if (points.isEmpty()) {
            return new UnifiedLocation(world, 0, (minY + maxY) / 2.0, 0);
        }

        double sumX = 0, sumZ = 0;
        for (int[] point : points) {
            sumX += point[0];
            sumZ += point[1];
        }

        return new UnifiedLocation(
            world,
            sumX / points.size(),
            (minY + maxY) / 2.0,
            sumZ / points.size()
        );
    }

    private void invalidateCache() {
        convex = null;
    }

    @Override
    public String toString() {
        return "PolygonRegion[" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", world=" + world.getName() +
            ", points=" + points.size() +
            ", minY=" + minY +
            ", maxY=" + maxY +
            ", priority=" + priority +
            ']';
    }
}
