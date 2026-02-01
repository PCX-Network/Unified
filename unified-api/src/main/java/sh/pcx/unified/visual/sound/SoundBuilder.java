/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.sound;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Builder for playing sounds with detailed configuration.
 *
 * <p>Use this builder to configure sound properties before playing.
 * Obtain a builder from {@link SoundService#play(sh.pcx.unified.world.UnifiedLocation)}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * soundService.play(location)
 *     .sound(SoundType.ENTITY_PLAYER_LEVELUP)
 *     .category(SoundCategory.PLAYERS)
 *     .volume(1.0f)
 *     .pitch(1.2f)
 *     .play();
 *
 * // Custom resource pack sound
 * soundService.play(location)
 *     .custom("myplugin:boss_roar")
 *     .volume(2.0f)
 *     .play();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SoundService
 */
public interface SoundBuilder {

    /**
     * Sets the sound to play.
     *
     * @param sound the sound type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder sound(@NotNull SoundType sound);

    /**
     * Sets a custom sound by its resource location.
     *
     * <p>Custom sounds must be defined in a resource pack.
     *
     * @param soundKey the sound key (e.g., "myplugin:custom_sound")
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder custom(@NotNull String soundKey);

    /**
     * Sets the sound category.
     *
     * <p>This determines which volume slider affects this sound.
     *
     * @param category the sound category
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder category(@NotNull SoundCategory category);

    /**
     * Sets the volume of the sound.
     *
     * <p>Values above 1.0 increase the audible distance rather than
     * the actual volume.
     *
     * @param volume the volume (default: 1.0)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder volume(float volume);

    /**
     * Sets the pitch of the sound.
     *
     * @param pitch the pitch (0.5 to 2.0, default: 1.0)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder pitch(float pitch);

    /**
     * Sets a random pitch within a range.
     *
     * @param min the minimum pitch
     * @param max the maximum pitch
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder randomPitch(float min, float max);

    /**
     * Sets specific players who will hear this sound.
     *
     * <p>If not called, the sound is played to all nearby players.
     *
     * @param listeners the players who can hear the sound
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder listeners(@NotNull UnifiedPlayer... listeners);

    /**
     * Sets specific players who will hear this sound.
     *
     * @param listeners the players who can hear the sound
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder listeners(@NotNull Collection<? extends UnifiedPlayer> listeners);

    /**
     * Sets the seed for sound variants.
     *
     * <p>Some sounds have multiple variants; the seed determines which one plays.
     *
     * @param seed the variant seed
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder seed(long seed);

    /**
     * Plays the configured sound.
     *
     * @since 1.0.0
     */
    void play();
}
