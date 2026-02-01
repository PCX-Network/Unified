/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.bossbar;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/**
 * Represents a boss bar displayed to players.
 *
 * <p>Boss bars appear at the top of the player's screen and can be used
 * to display progress, health, or any other information.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Update the bar
 * bar.setProgress(0.5f);
 * bar.setTitle(Component.text("50% Complete"));
 *
 * // Animate progress
 * bar.animateProgress(0.0f, 1.0f, Duration.ofSeconds(10));
 *
 * // Add/remove players
 * bar.addPlayer(player);
 * bar.removePlayer(player);
 *
 * // Remove when done
 * bar.remove();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BossBarService
 * @see BossBarBuilder
 */
public interface BossBarDisplay {

    /**
     * Returns the unique identifier of this boss bar.
     *
     * @return the boss bar's unique ID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns the title of this boss bar.
     *
     * @return the title
     * @since 1.0.0
     */
    @NotNull
    Component getTitle();

    /**
     * Sets the title of this boss bar.
     *
     * @param title the new title
     * @since 1.0.0
     */
    void setTitle(@NotNull Component title);

    /**
     * Returns the current progress of this boss bar.
     *
     * @return the progress (0.0 to 1.0)
     * @since 1.0.0
     */
    float getProgress();

    /**
     * Sets the progress of this boss bar.
     *
     * @param progress the progress (0.0 to 1.0)
     * @throws IllegalArgumentException if progress is not in range [0.0, 1.0]
     * @since 1.0.0
     */
    void setProgress(float progress);

    /**
     * Animates the progress from current to target over the specified duration.
     *
     * @param target   the target progress (0.0 to 1.0)
     * @param duration the animation duration
     * @since 1.0.0
     */
    void animateProgress(float target, @NotNull Duration duration);

    /**
     * Animates the progress from start to end over the specified duration.
     *
     * @param start    the starting progress
     * @param end      the ending progress
     * @param duration the animation duration
     * @since 1.0.0
     */
    void animateProgress(float start, float end, @NotNull Duration duration);

    /**
     * Stops any running progress animation.
     *
     * @since 1.0.0
     */
    void stopProgressAnimation();

    /**
     * Returns whether a progress animation is running.
     *
     * @return true if animating
     * @since 1.0.0
     */
    boolean isAnimatingProgress();

    /**
     * Returns the color of this boss bar.
     *
     * @return the color
     * @since 1.0.0
     */
    @NotNull
    BossBarColor getColor();

    /**
     * Sets the color of this boss bar.
     *
     * @param color the new color
     * @since 1.0.0
     */
    void setColor(@NotNull BossBarColor color);

    /**
     * Returns the overlay style of this boss bar.
     *
     * @return the overlay
     * @since 1.0.0
     */
    @NotNull
    BossBarOverlay getOverlay();

    /**
     * Sets the overlay style of this boss bar.
     *
     * @param overlay the new overlay
     * @since 1.0.0
     */
    void setOverlay(@NotNull BossBarOverlay overlay);

    /**
     * Returns whether the sky darkens when this bar is shown.
     *
     * @return true if sky darkens
     * @since 1.0.0
     */
    boolean isDarkenSky();

    /**
     * Sets whether the sky darkens when this bar is shown.
     *
     * @param darkenSky true to darken the sky
     * @since 1.0.0
     */
    void setDarkenSky(boolean darkenSky);

    /**
     * Returns whether boss music plays when this bar is shown.
     *
     * @return true if music plays
     * @since 1.0.0
     */
    boolean isPlayBossMusic();

    /**
     * Sets whether boss music plays when this bar is shown.
     *
     * @param playMusic true to play music
     * @since 1.0.0
     */
    void setPlayBossMusic(boolean playMusic);

    /**
     * Returns whether fog is created when this bar is shown.
     *
     * @return true if fog is created
     * @since 1.0.0
     */
    boolean isCreateFog();

    /**
     * Sets whether fog is created when this bar is shown.
     *
     * @param createFog true to create fog
     * @since 1.0.0
     */
    void setCreateFog(boolean createFog);

    /**
     * Returns all players viewing this boss bar.
     *
     * @return an unmodifiable collection of viewers
     * @since 1.0.0
     */
    @NotNull
    @Unmodifiable
    Collection<UnifiedPlayer> getPlayers();

    /**
     * Adds a player to view this boss bar.
     *
     * @param player the player to add
     * @since 1.0.0
     */
    void addPlayer(@NotNull UnifiedPlayer player);

    /**
     * Adds multiple players to view this boss bar.
     *
     * @param players the players to add
     * @since 1.0.0
     */
    void addPlayers(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Removes a player from viewing this boss bar.
     *
     * @param player the player to remove
     * @since 1.0.0
     */
    void removePlayer(@NotNull UnifiedPlayer player);

    /**
     * Removes all players from this boss bar.
     *
     * @since 1.0.0
     */
    void removeAllPlayers();

    /**
     * Checks if a player is viewing this boss bar.
     *
     * @param player the player to check
     * @return true if the player can see this bar
     * @since 1.0.0
     */
    boolean hasPlayer(@NotNull UnifiedPlayer player);

    /**
     * Returns whether this boss bar is visible.
     *
     * @return true if visible
     * @since 1.0.0
     */
    boolean isVisible();

    /**
     * Sets whether this boss bar is visible.
     *
     * <p>When hidden, the bar is not shown to any players but data is preserved.
     *
     * @param visible true to show
     * @since 1.0.0
     */
    void setVisible(boolean visible);

    /**
     * Removes this boss bar completely.
     *
     * <p>After removal, this boss bar instance should not be used.
     *
     * @since 1.0.0
     */
    void remove();

    /**
     * Returns whether this boss bar has been removed.
     *
     * @return true if removed
     * @since 1.0.0
     */
    boolean isRemoved();
}
