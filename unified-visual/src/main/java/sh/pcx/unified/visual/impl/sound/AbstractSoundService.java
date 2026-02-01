/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.sound;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.sound.SoundBuilder;
import sh.pcx.unified.visual.sound.SoundCategory;
import sh.pcx.unified.visual.sound.SoundService;
import sh.pcx.unified.visual.sound.SoundType;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * Abstract base implementation of {@link SoundService}.
 *
 * <p>Provides common sound playback functionality. Subclasses
 * implement platform-specific sound rendering.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractSoundService implements SoundService {

    @Override
    public void play(@NotNull UnifiedLocation location, @NotNull SoundType sound) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(sound, "sound cannot be null");
        play(location).sound(sound).play();
    }

    @Override
    public void play(@NotNull UnifiedLocation location, @NotNull SoundType sound,
                     float volume, float pitch) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(sound, "sound cannot be null");
        play(location).sound(sound).volume(volume).pitch(pitch).play();
    }

    @Override
    public void playCustom(@NotNull UnifiedLocation location, @NotNull String soundKey) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(soundKey, "soundKey cannot be null");
        play(location).custom(soundKey).play();
    }

    @Override
    public void playCustom(@NotNull UnifiedLocation location, @NotNull String soundKey,
                           float volume, float pitch) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(soundKey, "soundKey cannot be null");
        play(location).custom(soundKey).volume(volume).pitch(pitch).play();
    }

    @Override
    public void playTo(@NotNull UnifiedPlayer player, @NotNull SoundType sound) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(sound, "sound cannot be null");
        playTo(player).sound(sound).play();
    }

    @Override
    public void playTo(@NotNull UnifiedPlayer player, @NotNull SoundType sound,
                       float volume, float pitch) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(sound, "sound cannot be null");
        playTo(player).sound(sound).volume(volume).pitch(pitch).play();
    }

    @Override
    public void playTo(@NotNull UnifiedLocation location, @NotNull SoundType sound,
                       @NotNull Collection<? extends UnifiedPlayer> players) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(sound, "sound cannot be null");
        Objects.requireNonNull(players, "players cannot be null");
        play(location).sound(sound).listeners(players).play();
    }

    @Override
    public void playFrom(@NotNull UnifiedPlayer source, @NotNull UnifiedPlayer listener,
                         @NotNull SoundType sound) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(sound, "sound cannot be null");
        playToFrom(source.getLocation(), listener, sound);
    }

    /**
     * Plays a sound at a location to a specific listener.
     *
     * @param location the sound location
     * @param listener the player who will hear the sound
     * @param sound    the sound to play
     */
    protected abstract void playToFrom(@NotNull UnifiedLocation location,
                                        @NotNull UnifiedPlayer listener,
                                        @NotNull SoundType sound);

    @Override
    public String getServiceName() {
        return "SoundService";
    }
}
