/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.particle;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

/**
 * Represents an animated particle effect.
 *
 * <p>Particle animations spawn particles over time, following a path
 * or pattern defined by a position function.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Spiral animation
 * ParticleAnimation anim = particleService.animate()
 *     .duration(Duration.ofSeconds(3))
 *     .effect(t -> {
 *         double angle = t * Math.PI * 4;
 *         double x = Math.cos(angle) * 2;
 *         double z = Math.sin(angle) * 2;
 *         return center.add(x, t * 3, z);
 *     })
 *     .particle(ParticleType.FLAME)
 *     .tickRate(1)
 *     .start();
 *
 * // Stop later
 * anim.stop();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ParticleService#animate()
 * @see ParticleAnimationBuilder
 */
public interface ParticleAnimation {

    /**
     * Returns the unique identifier of this animation.
     *
     * @return the animation's unique ID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns whether this animation is currently running.
     *
     * @return true if running
     * @since 1.0.0
     */
    boolean isRunning();

    /**
     * Returns whether this animation is paused.
     *
     * @return true if paused
     * @since 1.0.0
     */
    boolean isPaused();

    /**
     * Returns whether this animation has completed.
     *
     * @return true if completed
     * @since 1.0.0
     */
    boolean isCompleted();

    /**
     * Returns the duration of this animation.
     *
     * @return the animation duration
     * @since 1.0.0
     */
    @NotNull
    Duration getDuration();

    /**
     * Returns the elapsed time since the animation started.
     *
     * @return the elapsed duration
     * @since 1.0.0
     */
    @NotNull
    Duration getElapsed();

    /**
     * Returns the progress of this animation (0.0 to 1.0).
     *
     * @return the animation progress
     * @since 1.0.0
     */
    float getProgress();

    /**
     * Pauses this animation.
     *
     * @since 1.0.0
     */
    void pause();

    /**
     * Resumes this animation if paused.
     *
     * @since 1.0.0
     */
    void resume();

    /**
     * Stops this animation.
     *
     * @since 1.0.0
     */
    void stop();

    /**
     * Restarts this animation from the beginning.
     *
     * @since 1.0.0
     */
    void restart();

    /**
     * Builder for particle animations.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the animation duration.
         *
         * @param duration the animation duration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder duration(@NotNull Duration duration);

        /**
         * Sets the position function for the animation.
         *
         * <p>The function receives a progress value from 0.0 to 1.0 and
         * returns the location to spawn particles at.
         *
         * @param effect the position function
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder effect(@NotNull Function<Float, UnifiedLocation> effect);

        /**
         * Sets the particle type to spawn.
         *
         * @param type the particle type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder particle(@NotNull ParticleType type);

        /**
         * Sets how often particles are spawned (in ticks).
         *
         * @param ticks the tick rate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder tickRate(int ticks);

        /**
         * Sets the number of particles per spawn.
         *
         * @param count the particle count
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder count(int count);

        /**
         * Sets whether the animation should loop.
         *
         * @param loop true to loop
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder loop(boolean loop);

        /**
         * Sets specific viewers for this animation.
         *
         * @param viewers the players who can see the animation
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder viewers(@NotNull Collection<? extends UnifiedPlayer> viewers);

        /**
         * Sets a callback to run when the animation completes.
         *
         * @param callback the completion callback
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder onComplete(@NotNull Runnable callback);

        /**
         * Starts the animation.
         *
         * @return the running animation
         * @since 1.0.0
         */
        @NotNull
        ParticleAnimation start();
    }
}
