/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.particle;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.awt.Color;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for spawning particles and particle effects.
 *
 * <p>The ParticleService provides methods to spawn individual particles,
 * create particle shapes, and run particle animations.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>All Minecraft particle types</li>
 *   <li>Particle shapes (line, circle, sphere, helix)</li>
 *   <li>Animated particle effects</li>
 *   <li>Per-player particles</li>
 *   <li>Colored dust particles</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private ParticleService particles;
 *
 * // Simple particle
 * particles.spawn(location, ParticleType.FLAME, 50);
 *
 * // Detailed particle
 * particles.spawn(location)
 *     .particle(ParticleType.DUST)
 *     .color(Color.RED)
 *     .size(1.5f)
 *     .count(100)
 *     .offset(0.5, 0.5, 0.5)
 *     .viewers(nearbyPlayers)
 *     .spawn();
 *
 * // Shapes
 * particles.circle(center, radius, ParticleType.END_ROD, 50);
 * particles.line(start, end, ParticleType.FLAME, 20);
 * particles.sphere(center, radius, ParticleType.SOUL_FIRE_FLAME, 200);
 * particles.helix(center, height, radius, ParticleType.WITCH, 100);
 *
 * // Animation
 * ParticleAnimation anim = particles.animate()
 *     .duration(Duration.ofSeconds(3))
 *     .effect(t -> center.add(Math.cos(t * 4) * 2, t * 3, Math.sin(t * 4) * 2))
 *     .particle(ParticleType.FLAME)
 *     .start();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ParticleBuilder
 * @see ParticleAnimation
 */
public interface ParticleService extends Service {

    // ==================== Simple Spawn Methods ====================

    /**
     * Spawns particles at a location.
     *
     * @param location the location to spawn at
     * @param type     the particle type
     * @param count    the number of particles
     * @since 1.0.0
     */
    void spawn(@NotNull UnifiedLocation location, @NotNull ParticleType type, int count);

    /**
     * Spawns colored dust particles at a location.
     *
     * @param location the location to spawn at
     * @param color    the particle color
     * @param size     the particle size
     * @param count    the number of particles
     * @since 1.0.0
     */
    void spawnDust(@NotNull UnifiedLocation location, @NotNull Color color, float size, int count);

    /**
     * Spawns particles visible only to specific players.
     *
     * @param location the location to spawn at
     * @param type     the particle type
     * @param count    the number of particles
     * @param viewers  the players who can see the particles
     * @since 1.0.0
     */
    void spawnTo(@NotNull UnifiedLocation location, @NotNull ParticleType type,
                 int count, @NotNull Collection<? extends UnifiedPlayer> viewers);

    // ==================== Builder Method ====================

    /**
     * Creates a particle builder for detailed configuration.
     *
     * @param location the location to spawn at
     * @return a particle builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder spawn(@NotNull UnifiedLocation location);

    // ==================== Shape Methods ====================

    /**
     * Spawns particles in a line between two points.
     *
     * @param start the start location
     * @param end   the end location
     * @param type  the particle type
     * @param count the total number of particles
     * @since 1.0.0
     */
    void line(@NotNull UnifiedLocation start, @NotNull UnifiedLocation end,
              @NotNull ParticleType type, int count);

    /**
     * Spawns particles in a line visible to specific players.
     *
     * @param start   the start location
     * @param end     the end location
     * @param type    the particle type
     * @param count   the total number of particles
     * @param viewers the players who can see the particles
     * @since 1.0.0
     */
    void line(@NotNull UnifiedLocation start, @NotNull UnifiedLocation end,
              @NotNull ParticleType type, int count, @NotNull Collection<? extends UnifiedPlayer> viewers);

    /**
     * Spawns particles in a circle.
     *
     * @param center the center location
     * @param radius the circle radius
     * @param type   the particle type
     * @param count  the number of particles
     * @since 1.0.0
     */
    void circle(@NotNull UnifiedLocation center, double radius,
                @NotNull ParticleType type, int count);

    /**
     * Spawns particles in a circle visible to specific players.
     *
     * @param center  the center location
     * @param radius  the circle radius
     * @param type    the particle type
     * @param count   the number of particles
     * @param viewers the players who can see the particles
     * @since 1.0.0
     */
    void circle(@NotNull UnifiedLocation center, double radius, @NotNull ParticleType type,
                int count, @NotNull Collection<? extends UnifiedPlayer> viewers);

    /**
     * Spawns particles in a sphere.
     *
     * @param center the center location
     * @param radius the sphere radius
     * @param type   the particle type
     * @param count  the number of particles
     * @since 1.0.0
     */
    void sphere(@NotNull UnifiedLocation center, double radius,
                @NotNull ParticleType type, int count);

    /**
     * Spawns particles in a sphere visible to specific players.
     *
     * @param center  the center location
     * @param radius  the sphere radius
     * @param type    the particle type
     * @param count   the number of particles
     * @param viewers the players who can see the particles
     * @since 1.0.0
     */
    void sphere(@NotNull UnifiedLocation center, double radius, @NotNull ParticleType type,
                int count, @NotNull Collection<? extends UnifiedPlayer> viewers);

    /**
     * Spawns particles in a helix pattern.
     *
     * @param base   the base location (bottom of helix)
     * @param height the helix height
     * @param radius the helix radius
     * @param type   the particle type
     * @param count  the number of particles
     * @since 1.0.0
     */
    void helix(@NotNull UnifiedLocation base, double height, double radius,
               @NotNull ParticleType type, int count);

    /**
     * Spawns particles in a helix visible to specific players.
     *
     * @param base    the base location
     * @param height  the helix height
     * @param radius  the helix radius
     * @param type    the particle type
     * @param count   the number of particles
     * @param viewers the players who can see the particles
     * @since 1.0.0
     */
    void helix(@NotNull UnifiedLocation base, double height, double radius,
               @NotNull ParticleType type, int count, @NotNull Collection<? extends UnifiedPlayer> viewers);

    /**
     * Spawns particles in a cube outline.
     *
     * @param corner1 one corner of the cube
     * @param corner2 the opposite corner
     * @param type    the particle type
     * @param density particles per block
     * @since 1.0.0
     */
    void cube(@NotNull UnifiedLocation corner1, @NotNull UnifiedLocation corner2,
              @NotNull ParticleType type, double density);

    /**
     * Spawns particles in a ring (horizontal circle).
     *
     * @param center the center location
     * @param radius the ring radius
     * @param type   the particle type
     * @param count  the number of particles
     * @since 1.0.0
     */
    default void ring(@NotNull UnifiedLocation center, double radius,
                      @NotNull ParticleType type, int count) {
        circle(center, radius, type, count);
    }

    // ==================== Animation Methods ====================

    /**
     * Creates a particle animation builder.
     *
     * @return an animation builder
     * @since 1.0.0
     */
    @NotNull
    ParticleAnimation.Builder animate();

    /**
     * Returns a running animation by its ID.
     *
     * @param id the animation ID
     * @return an Optional containing the animation if found
     * @since 1.0.0
     */
    @NotNull
    Optional<ParticleAnimation> getAnimation(@NotNull UUID id);

    /**
     * Returns all running animations.
     *
     * @return an unmodifiable collection of running animations
     * @since 1.0.0
     */
    @NotNull
    @Unmodifiable
    Collection<ParticleAnimation> getRunningAnimations();

    /**
     * Stops all running animations.
     *
     * @since 1.0.0
     */
    void stopAllAnimations();
}
