/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.sound;

/**
 * Sound categories that correspond to the player's sound settings.
 *
 * <p>Each category has its own volume slider in the game's sound settings,
 * allowing players to control which types of sounds they hear.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public enum SoundCategory {

    /**
     * Master volume - affects all sounds.
     */
    MASTER,

    /**
     * Music and background audio.
     */
    MUSIC,

    /**
     * Music from records/jukeboxes.
     */
    RECORDS,

    /**
     * Weather sounds (rain, thunder).
     */
    WEATHER,

    /**
     * Block sounds (placing, breaking, stepping).
     */
    BLOCKS,

    /**
     * Hostile mob sounds.
     */
    HOSTILE,

    /**
     * Passive/neutral mob sounds.
     */
    NEUTRAL,

    /**
     * Player sounds (footsteps, damage, etc.).
     */
    PLAYERS,

    /**
     * Ambient/environmental sounds.
     */
    AMBIENT,

    /**
     * Voice/speech sounds.
     */
    VOICE
}
