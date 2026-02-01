/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

/**
 * Builder for creating scoreboards.
 *
 * <p>Use this builder to configure scoreboard properties before creation.
 * Obtain a builder from {@link ScoreboardService#create(UnifiedPlayer)}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Scoreboard board = scoreboardService.create(player)
 *     .title(Component.text("My Server", NamedTextColor.GOLD, TextDecoration.BOLD))
 *     .line(15, Component.empty())
 *     .line(14, Component.text("Balance: ${balance}"))
 *     .line(13, Component.empty())
 *     .line(12, Component.text("Kills: {kills}"))
 *     .line(11, Component.text("Deaths: {deaths}"))
 *     .line(10, Component.empty())
 *     .line(9, Component.text("play.myserver.com", NamedTextColor.YELLOW))
 *     .updateInterval(20)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ScoreboardService
 * @see Scoreboard
 */
public interface ScoreboardBuilder {

    /**
     * Sets the title of the scoreboard.
     *
     * @param title the scoreboard title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder title(@NotNull Component title);

    /**
     * Sets an animated title that cycles through frames.
     *
     * @param frames        the frames to cycle through
     * @param ticksPerFrame ticks between frame changes
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder animatedTitle(@NotNull List<Component> frames, int ticksPerFrame);

    /**
     * Adds a line to the scoreboard.
     *
     * @param score   the line score (higher = higher on screen, max 15)
     * @param content the line content
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder line(int score, @NotNull Component content);

    /**
     * Adds a conditional line to the scoreboard.
     *
     * <p>The line is only shown when the condition returns true.
     *
     * @param score     the line score
     * @param content   the line content
     * @param condition the visibility condition
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder line(int score, @NotNull Component content, @NotNull Predicate<UnifiedPlayer> condition);

    /**
     * Adds a dynamic line with an updater.
     *
     * @param score   the line score
     * @param updater the line updater function
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder dynamicLine(int score, @NotNull Scoreboard.ScoreboardLineUpdater updater);

    /**
     * Sets multiple lines from top to bottom.
     *
     * <p>Lines are assigned scores starting from 15 and decreasing.
     *
     * @param lines the lines to set
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder lines(@NotNull List<Component> lines);

    /**
     * Adds a blank line at the specified score.
     *
     * @param score the line score
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ScoreboardBuilder blankLine(int score) {
        return line(score, Component.empty());
    }

    /**
     * Sets the update interval for dynamic content.
     *
     * @param ticks the update interval in ticks (20 ticks = 1 second)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder updateInterval(int ticks);

    /**
     * Sets whether the scoreboard is initially visible.
     *
     * @param visible true to show immediately
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder visible(boolean visible);

    /**
     * Builds and shows the scoreboard.
     *
     * @return the created scoreboard
     * @since 1.0.0
     */
    @NotNull
    Scoreboard build();

    /**
     * Builds the scoreboard without showing it.
     *
     * @return the created scoreboard (hidden)
     * @since 1.0.0
     */
    @NotNull
    Scoreboard buildWithoutShowing();
}
