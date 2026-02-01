/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.particle;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Collection;

/**
 * Builder for spawning particles with detailed configuration.
 *
 * <p>Use this builder to configure particle properties before spawning.
 * Obtain a builder from {@link ParticleService#spawn(sh.pcx.unified.world.UnifiedLocation)}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * particleService.spawn(location)
 *     .particle(ParticleType.DUST)
 *     .color(Color.RED)
 *     .size(1.5f)
 *     .count(100)
 *     .offset(0.5, 0.5, 0.5)
 *     .viewers(nearbyPlayers)
 *     .spawn();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ParticleService
 */
public interface ParticleBuilder {

    /**
     * Sets the particle type.
     *
     * @param type the particle type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder particle(@NotNull ParticleType type);

    /**
     * Sets the number of particles to spawn.
     *
     * @param count the particle count
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder count(int count);

    /**
     * Sets the offset (spread) for particle spawning.
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder offset(double x, double y, double z);

    /**
     * Sets the offset uniformly in all directions.
     *
     * @param offset the offset value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ParticleBuilder offset(double offset) {
        return offset(offset, offset, offset);
    }

    /**
     * Sets the extra data (typically speed for most particles).
     *
     * @param extra the extra value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder extra(double extra);

    /**
     * Sets the color for DUST particles.
     *
     * @param color the particle color
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder color(@NotNull Color color);

    /**
     * Sets the color for DUST particles using RGB values.
     *
     * @param red   the red component (0-255)
     * @param green the green component (0-255)
     * @param blue  the blue component (0-255)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ParticleBuilder color(int red, int green, int blue) {
        return color(new Color(red, green, blue));
    }

    /**
     * Sets the size for DUST particles.
     *
     * @param size the particle size
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder size(float size);

    /**
     * Sets the transition color for DUST_COLOR_TRANSITION particles.
     *
     * @param toColor the color to transition to
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder toColor(@NotNull Color toColor);

    /**
     * Sets the velocity/direction for directional particles.
     *
     * @param x the x velocity
     * @param y the y velocity
     * @param z the z velocity
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder velocity(double x, double y, double z);

    /**
     * Sets the particle to be directional (uses offset as direction).
     *
     * @param directional true for directional mode
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder directional(boolean directional);

    /**
     * Forces the particle to be shown regardless of particle settings.
     *
     * @param force true to force display
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder force(boolean force);

    /**
     * Sets specific players who will see this particle.
     *
     * <p>If not called, the particle is visible to all nearby players.
     *
     * @param viewers the players who can see the particle
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder viewers(@NotNull UnifiedPlayer... viewers);

    /**
     * Sets specific players who will see this particle.
     *
     * @param viewers the players who can see the particle
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder viewers(@NotNull Collection<? extends UnifiedPlayer> viewers);

    /**
     * Sets the maximum view distance for this particle.
     *
     * @param distance the view distance in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ParticleBuilder viewDistance(double distance);

    /**
     * Spawns the configured particles.
     *
     * @since 1.0.0
     */
    void spawn();
}
