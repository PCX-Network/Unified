/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.core;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Legacy per-player scoreboard interface.
 *
 * <p>This interface represents the traditional per-player scoreboard model
 * where each player has their own scoreboard instance. This is different from
 * the newer {@link sh.pcx.unified.visual.scoreboard.Scoreboard} interface which
 * supports shared scoreboards with named registrations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.visual.scoreboard.Scoreboard
 */
public interface LegacyScoreboard {

    /**
     * Returns the unique identifier for this scoreboard.
     *
     * @return the scoreboard's unique ID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns the player this scoreboard belongs to.
     *
     * @return the scoreboard owner
     * @since 1.0.0
     */
    @NotNull
    UnifiedPlayer getPlayer();

    /**
     * Returns the current title of this scoreboard.
     *
     * @return the scoreboard title
     * @since 1.0.0
     */
    @NotNull
    Component getTitle();

    /**
     * Sets the title of this scoreboard.
     *
     * @param title the new title
     * @since 1.0.0
     */
    void setTitle(@NotNull Component title);

    /**
     * Starts animating the title with the given frames.
     *
     * @param frames        the animation frames
     * @param ticksPerFrame ticks between frame changes
     * @since 1.0.0
     */
    void animateTitle(@NotNull List<Component> frames, int ticksPerFrame);

    /**
     * Stops any current title animation.
     *
     * @since 1.0.0
     */
    void stopTitleAnimation();

    /**
     * Returns whether the title is currently animating.
     *
     * @return true if animating
     * @since 1.0.0
     */
    boolean isTitleAnimating();

    /**
     * Returns all lines as a score-to-content map.
     *
     * @return the lines map
     * @since 1.0.0
     */
    @NotNull
    Map<Integer, Component> getLines();

    /**
     * Returns the line at the given score.
     *
     * @param score the line score
     * @return the line content if present
     * @since 1.0.0
     */
    @NotNull
    Optional<Component> getLine(int score);

    /**
     * Sets a line at the given score.
     *
     * @param score   the line score
     * @param content the line content
     * @since 1.0.0
     */
    void setLine(int score, @NotNull Component content);

    /**
     * Sets a conditional line at the given score.
     *
     * @param score     the line score
     * @param content   the line content
     * @param condition the visibility condition
     * @since 1.0.0
     */
    void setLine(int score, @NotNull Component content, @NotNull Predicate<UnifiedPlayer> condition);

    /**
     * Removes the line at the given score.
     *
     * @param score the line score
     * @since 1.0.0
     */
    void removeLine(int score);

    /**
     * Clears all lines from this scoreboard.
     *
     * @since 1.0.0
     */
    void clearLines();

    /**
     * Sets multiple lines at once from a list.
     *
     * @param lines the lines (top to bottom)
     * @since 1.0.0
     */
    void setLines(@NotNull List<Component> lines);

    /**
     * Sets multiple lines at once from a map.
     *
     * @param lines the score-to-content map
     * @since 1.0.0
     */
    void setLines(@NotNull Map<Integer, Component> lines);

    /**
     * Returns the number of lines.
     *
     * @return the line count
     * @since 1.0.0
     */
    int getLineCount();

    /**
     * Adds a dynamic line updater.
     *
     * @param score   the line score
     * @param updater the updater function
     * @since 1.0.0
     */
    void addLineUpdater(int score, @NotNull ScoreboardLineUpdater updater);

    /**
     * Removes a line updater.
     *
     * @param score the line score
     * @since 1.0.0
     */
    void removeLineUpdater(int score);

    /**
     * Returns the update interval in ticks.
     *
     * @return the update interval
     * @since 1.0.0
     */
    int getUpdateInterval();

    /**
     * Sets the update interval in ticks.
     *
     * @param ticks the update interval
     * @since 1.0.0
     */
    void setUpdateInterval(int ticks);

    /**
     * Returns whether this scoreboard is currently visible.
     *
     * @return true if visible
     * @since 1.0.0
     */
    boolean isVisible();

    /**
     * Shows this scoreboard to the player.
     *
     * @since 1.0.0
     */
    void show();

    /**
     * Hides this scoreboard from the player.
     *
     * @since 1.0.0
     */
    void hide();

    /**
     * Toggles the visibility of this scoreboard.
     *
     * @since 1.0.0
     */
    void toggle();

    /**
     * Updates dynamic content on this scoreboard.
     *
     * @since 1.0.0
     */
    void update();

    /**
     * Removes this scoreboard permanently.
     *
     * @since 1.0.0
     */
    void remove();

    /**
     * Returns whether this scoreboard has been removed.
     *
     * @return true if removed
     * @since 1.0.0
     */
    boolean isRemoved();
}
