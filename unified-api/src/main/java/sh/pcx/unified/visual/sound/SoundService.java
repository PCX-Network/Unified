/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.sound;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Service for playing sounds to players.
 *
 * <p>The SoundService provides methods to play sounds at locations or
 * directly to players, with support for 3D positional audio and custom
 * resource pack sounds.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>All Minecraft sounds</li>
 *   <li>3D positional audio</li>
 *   <li>Sound categories for volume settings</li>
 *   <li>Custom resource pack sounds</li>
 *   <li>Per-player sound playback</li>
 *   <li>Volume and pitch control</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private SoundService sounds;
 *
 * // Play sound at location
 * sounds.play(location, SoundType.ENTITY_ENDER_DRAGON_GROWL);
 *
 * // Detailed sound
 * sounds.play(location)
 *     .sound(SoundType.ENTITY_PLAYER_LEVELUP)
 *     .category(SoundCategory.PLAYERS)
 *     .volume(1.0f)
 *     .pitch(1.2f)
 *     .play();
 *
 * // Play to specific player (client-side)
 * sounds.playTo(player, SoundType.UI_BUTTON_CLICK);
 *
 * // Custom resource pack sound
 * sounds.play(location)
 *     .custom("myplugin:custom_sound")
 *     .play();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SoundBuilder
 * @see SoundType
 * @see SoundCategory
 */
public interface SoundService extends Service {

    // ==================== Simple Play Methods ====================

    /**
     * Plays a sound at a location.
     *
     * @param location the location to play at
     * @param sound    the sound to play
     * @since 1.0.0
     */
    void play(@NotNull UnifiedLocation location, @NotNull SoundType sound);

    /**
     * Plays a sound at a location with volume and pitch.
     *
     * @param location the location to play at
     * @param sound    the sound to play
     * @param volume   the volume (1.0 = normal)
     * @param pitch    the pitch (0.5-2.0, 1.0 = normal)
     * @since 1.0.0
     */
    void play(@NotNull UnifiedLocation location, @NotNull SoundType sound, float volume, float pitch);

    /**
     * Plays a custom sound at a location.
     *
     * @param location the location to play at
     * @param soundKey the custom sound key (e.g., "myplugin:custom")
     * @since 1.0.0
     */
    void playCustom(@NotNull UnifiedLocation location, @NotNull String soundKey);

    /**
     * Plays a custom sound at a location with volume and pitch.
     *
     * @param location the location to play at
     * @param soundKey the custom sound key
     * @param volume   the volume
     * @param pitch    the pitch
     * @since 1.0.0
     */
    void playCustom(@NotNull UnifiedLocation location, @NotNull String soundKey, float volume, float pitch);

    // ==================== Player-Specific Methods ====================

    /**
     * Plays a sound to a specific player at their location.
     *
     * <p>This is client-side only; other players won't hear it.
     *
     * @param player the player to play the sound to
     * @param sound  the sound to play
     * @since 1.0.0
     */
    void playTo(@NotNull UnifiedPlayer player, @NotNull SoundType sound);

    /**
     * Plays a sound to a specific player with volume and pitch.
     *
     * @param player the player to play the sound to
     * @param sound  the sound to play
     * @param volume the volume
     * @param pitch  the pitch
     * @since 1.0.0
     */
    void playTo(@NotNull UnifiedPlayer player, @NotNull SoundType sound, float volume, float pitch);

    /**
     * Plays a sound to multiple players at a location.
     *
     * @param location the location to play at
     * @param sound    the sound to play
     * @param players  the players who will hear the sound
     * @since 1.0.0
     */
    void playTo(@NotNull UnifiedLocation location, @NotNull SoundType sound,
                @NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Plays a sound from one player to another (3D positional).
     *
     * @param source   the source player (position of the sound)
     * @param listener the player who will hear the sound
     * @param sound    the sound to play
     * @since 1.0.0
     */
    void playFrom(@NotNull UnifiedPlayer source, @NotNull UnifiedPlayer listener, @NotNull SoundType sound);

    // ==================== Builder Method ====================

    /**
     * Creates a sound builder for detailed configuration.
     *
     * @param location the location to play at
     * @return a sound builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder play(@NotNull UnifiedLocation location);

    /**
     * Creates a sound builder for playing to a specific player.
     *
     * @param player the player to play to
     * @return a sound builder
     * @since 1.0.0
     */
    @NotNull
    SoundBuilder playTo(@NotNull UnifiedPlayer player);

    // ==================== Stop Methods ====================

    /**
     * Stops all sounds for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void stopAll(@NotNull UnifiedPlayer player);

    /**
     * Stops a specific sound for a player.
     *
     * @param player the player
     * @param sound  the sound to stop
     * @since 1.0.0
     */
    void stop(@NotNull UnifiedPlayer player, @NotNull SoundType sound);

    /**
     * Stops sounds in a specific category for a player.
     *
     * @param player   the player
     * @param category the category to stop
     * @since 1.0.0
     */
    void stopCategory(@NotNull UnifiedPlayer player, @NotNull SoundCategory category);

    /**
     * Stops a custom sound for a player.
     *
     * @param player   the player
     * @param soundKey the custom sound key
     * @since 1.0.0
     */
    void stopCustom(@NotNull UnifiedPlayer player, @NotNull String soundKey);
}
