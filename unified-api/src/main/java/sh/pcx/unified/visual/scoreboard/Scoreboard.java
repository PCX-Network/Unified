/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Represents a sidebar scoreboard for a player.
 *
 * <p>Scoreboards display information in the sidebar on the right side of
 * the player's screen. Each scoreboard can have up to 15 lines of text,
 * with support for animations and dynamic updates.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a player's scoreboard
 * Scoreboard board = scoreboardService.getScoreboard(player).orElseThrow();
 *
 * // Update the title
 * board.setTitle(Component.text("Updated Title"));
 *
 * // Update a line
 * board.setLine(10, Component.text("Balance: $" + balance));
 *
 * // Animate the title
 * board.animateTitle(List.of(
 *     Component.text("Title 1"),
 *     Component.text("Title 2"),
 *     Component.text("Title 3")
 * ), 20);
 *
 * // Hide the scoreboard
 * board.hide();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ScoreboardService
 * @see ScoreboardBuilder
 */
public interface Scoreboard {

    /**
     * Returns the unique identifier of this scoreboard.
     *
     * @return the scoreboard's unique ID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns the player this scoreboard belongs to.
     *
     * @return the owning player
     * @since 1.0.0
     */
    @NotNull
    UnifiedPlayer getPlayer();

    /**
     * Returns the current title of this scoreboard.
     *
     * @return the title
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
     * Animates the title by cycling through the given frames.
     *
     * @param frames     the frames to cycle through
     * @param ticksPerFrame ticks between frame changes
     * @since 1.0.0
     */
    void animateTitle(@NotNull List<Component> frames, int ticksPerFrame);

    /**
     * Stops any running title animation.
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
     * Returns all lines as a map of score to component.
     *
     * @return an unmodifiable map of score to line content
     * @since 1.0.0
     */
    @NotNull
    @Unmodifiable
    Map<Integer, Component> getLines();

    /**
     * Returns the content of a specific line.
     *
     * @param score the line score (higher = higher on screen)
     * @return an Optional containing the line content
     * @since 1.0.0
     */
    @NotNull
    Optional<Component> getLine(int score);

    /**
     * Sets the content of a specific line.
     *
     * @param score   the line score (higher = higher on screen)
     * @param content the line content
     * @since 1.0.0
     */
    void setLine(int score, @NotNull Component content);

    /**
     * Sets the content of a specific line with a condition.
     *
     * <p>The line is only shown when the condition returns true.
     *
     * @param score     the line score
     * @param content   the line content
     * @param condition the visibility condition
     * @since 1.0.0
     */
    void setLine(int score, @NotNull Component content, @NotNull Predicate<UnifiedPlayer> condition);

    /**
     * Removes a line from the scoreboard.
     *
     * @param score the line score to remove
     * @since 1.0.0
     */
    void removeLine(int score);

    /**
     * Clears all lines from the scoreboard.
     *
     * @since 1.0.0
     */
    void clearLines();

    /**
     * Sets multiple lines at once.
     *
     * <p>Lines are set from top to bottom starting at score 15.
     *
     * @param lines the lines to set
     * @since 1.0.0
     */
    void setLines(@NotNull List<Component> lines);

    /**
     * Sets multiple lines at once from a map.
     *
     * @param lines the map of score to line content
     * @since 1.0.0
     */
    void setLines(@NotNull Map<Integer, Component> lines);

    /**
     * Returns the number of lines in this scoreboard.
     *
     * @return the line count
     * @since 1.0.0
     */
    int getLineCount();

    /**
     * Adds a line updater that is called on each update cycle.
     *
     * @param score   the line score to update
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
     * Sets the update interval for dynamic content.
     *
     * @param ticks the update interval in ticks
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
     * <p>The scoreboard data is preserved and can be shown again.
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
     * Forces an update of all dynamic content.
     *
     * @since 1.0.0
     */
    void update();

    /**
     * Removes this scoreboard completely.
     *
     * <p>After removal, this scoreboard instance should not be used.
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

    /**
     * Functional interface for dynamic line updates.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface ScoreboardLineUpdater {
        /**
         * Called to update a line's content.
         *
         * @param player the scoreboard owner
         * @return the updated line content
         */
        @NotNull
        Component update(@NotNull UnifiedPlayer player);
    }
}
