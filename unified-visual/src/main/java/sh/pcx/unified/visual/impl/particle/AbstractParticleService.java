/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.particle;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.particle.ParticleAnimation;
import sh.pcx.unified.visual.particle.ParticleBuilder;
import sh.pcx.unified.visual.particle.ParticleService;
import sh.pcx.unified.visual.particle.ParticleType;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of {@link ParticleService}.
 *
 * <p>Provides common particle spawning functionality including shape
 * calculations. Subclasses implement platform-specific particle rendering.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractParticleService implements ParticleService {

    protected final Map<UUID, ParticleAnimation> runningAnimations = new ConcurrentHashMap<>();

    @Override
    public void spawn(@NotNull UnifiedLocation location, @NotNull ParticleType type, int count) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        spawn(location).particle(type).count(count).spawn();
    }

    @Override
    public void spawnDust(@NotNull UnifiedLocation location, @NotNull Color color,
                          float size, int count) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        spawn(location)
                .particle(ParticleType.DUST)
                .color(color)
                .size(size)
                .count(count)
                .spawn();
    }

    @Override
    public void spawnTo(@NotNull UnifiedLocation location, @NotNull ParticleType type,
                        int count, @NotNull Collection<? extends UnifiedPlayer> viewers) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(viewers, "viewers cannot be null");
        spawn(location).particle(type).count(count).viewers(viewers).spawn();
    }

    @Override
    public void line(@NotNull UnifiedLocation start, @NotNull UnifiedLocation end,
                     @NotNull ParticleType type, int count) {
        line(start, end, type, count, null);
    }

    @Override
    public void line(@NotNull UnifiedLocation start, @NotNull UnifiedLocation end,
                     @NotNull ParticleType type, int count,
                     @NotNull Collection<? extends UnifiedPlayer> viewers) {
        Objects.requireNonNull(start, "start cannot be null");
        Objects.requireNonNull(end, "end cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        double distance = start.distance(end);
        double dx = (end.x() - start.x()) / count;
        double dy = (end.y() - start.y()) / count;
        double dz = (end.z() - start.z()) / count;

        for (int i = 0; i <= count; i++) {
            UnifiedLocation point = start.add(dx * i, dy * i, dz * i);
            ParticleBuilder builder = spawn(point).particle(type).count(1);
            if (viewers != null) {
                builder.viewers(viewers);
            }
            builder.spawn();
        }
    }

    @Override
    public void circle(@NotNull UnifiedLocation center, double radius,
                       @NotNull ParticleType type, int count) {
        circle(center, radius, type, count, null);
    }

    @Override
    public void circle(@NotNull UnifiedLocation center, double radius,
                       @NotNull ParticleType type, int count,
                       @NotNull Collection<? extends UnifiedPlayer> viewers) {
        Objects.requireNonNull(center, "center cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        double angleStep = 2 * Math.PI / count;

        for (int i = 0; i < count; i++) {
            double angle = angleStep * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            UnifiedLocation point = center.add(x, 0, z);

            ParticleBuilder builder = spawn(point).particle(type).count(1);
            if (viewers != null) {
                builder.viewers(viewers);
            }
            builder.spawn();
        }
    }

    @Override
    public void sphere(@NotNull UnifiedLocation center, double radius,
                       @NotNull ParticleType type, int count) {
        sphere(center, radius, type, count, null);
    }

    @Override
    public void sphere(@NotNull UnifiedLocation center, double radius,
                       @NotNull ParticleType type, int count,
                       @NotNull Collection<? extends UnifiedPlayer> viewers) {
        Objects.requireNonNull(center, "center cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        Random random = new Random();

        for (int i = 0; i < count; i++) {
            // Generate random point on sphere using spherical coordinates
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * random.nextDouble() - 1);

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);

            UnifiedLocation point = center.add(x, y, z);

            ParticleBuilder builder = spawn(point).particle(type).count(1);
            if (viewers != null) {
                builder.viewers(viewers);
            }
            builder.spawn();
        }
    }

    @Override
    public void helix(@NotNull UnifiedLocation base, double height, double radius,
                      @NotNull ParticleType type, int count) {
        helix(base, height, radius, type, count, null);
    }

    @Override
    public void helix(@NotNull UnifiedLocation base, double height, double radius,
                      @NotNull ParticleType type, int count,
                      @NotNull Collection<? extends UnifiedPlayer> viewers) {
        Objects.requireNonNull(base, "base cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        double revolutions = 2; // Number of complete rotations
        double angleStep = (2 * Math.PI * revolutions) / count;
        double heightStep = height / count;

        for (int i = 0; i < count; i++) {
            double angle = angleStep * i;
            double x = Math.cos(angle) * radius;
            double y = heightStep * i;
            double z = Math.sin(angle) * radius;

            UnifiedLocation point = base.add(x, y, z);

            ParticleBuilder builder = spawn(point).particle(type).count(1);
            if (viewers != null) {
                builder.viewers(viewers);
            }
            builder.spawn();
        }
    }

    @Override
    public void cube(@NotNull UnifiedLocation corner1, @NotNull UnifiedLocation corner2,
                     @NotNull ParticleType type, double density) {
        Objects.requireNonNull(corner1, "corner1 cannot be null");
        Objects.requireNonNull(corner2, "corner2 cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        double minX = Math.min(corner1.x(), corner2.x());
        double maxX = Math.max(corner1.x(), corner2.x());
        double minY = Math.min(corner1.y(), corner2.y());
        double maxY = Math.max(corner1.y(), corner2.y());
        double minZ = Math.min(corner1.z(), corner2.z());
        double maxZ = Math.max(corner1.z(), corner2.z());

        double step = 1.0 / density;

        // Draw edges along X axis
        for (double y : new double[]{minY, maxY}) {
            for (double z : new double[]{minZ, maxZ}) {
                for (double x = minX; x <= maxX; x += step) {
                    spawn(new UnifiedLocation(corner1.world(), x, y, z))
                            .particle(type).count(1).spawn();
                }
            }
        }

        // Draw edges along Y axis
        for (double x : new double[]{minX, maxX}) {
            for (double z : new double[]{minZ, maxZ}) {
                for (double y = minY; y <= maxY; y += step) {
                    spawn(new UnifiedLocation(corner1.world(), x, y, z))
                            .particle(type).count(1).spawn();
                }
            }
        }

        // Draw edges along Z axis
        for (double x : new double[]{minX, maxX}) {
            for (double y : new double[]{minY, maxY}) {
                for (double z = minZ; z <= maxZ; z += step) {
                    spawn(new UnifiedLocation(corner1.world(), x, y, z))
                            .particle(type).count(1).spawn();
                }
            }
        }
    }

    @Override
    public @NotNull Optional<ParticleAnimation> getAnimation(@NotNull UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        return Optional.ofNullable(runningAnimations.get(id));
    }

    @Override
    public @NotNull Collection<ParticleAnimation> getRunningAnimations() {
        return List.copyOf(runningAnimations.values());
    }

    @Override
    public void stopAllAnimations() {
        List<ParticleAnimation> animations = new ArrayList<>(runningAnimations.values());
        runningAnimations.clear();
        animations.forEach(ParticleAnimation::stop);
    }

    /**
     * Registers a running animation.
     *
     * @param animation the animation
     */
    protected void registerAnimation(@NotNull ParticleAnimation animation) {
        runningAnimations.put(animation.getId(), animation);
    }

    /**
     * Unregisters a running animation.
     *
     * @param animation the animation
     */
    protected void unregisterAnimation(@NotNull ParticleAnimation animation) {
        runningAnimations.remove(animation.getId());
    }

    @Override
    public String getServiceName() {
        return "ParticleService";
    }
}
