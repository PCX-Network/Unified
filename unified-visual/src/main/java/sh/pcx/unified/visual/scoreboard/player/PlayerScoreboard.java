/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.player;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.scoreboard.line.ScoreboardLine;
import sh.pcx.unified.visual.scoreboard.title.ScoreboardTitle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Represents a per-player scoreboard.
 *
 * <p>Player scoreboards are individual scoreboards managed for each player,
 * allowing for completely personalized content and independent control.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface PlayerScoreboard {

    /**
     * Returns the player this scoreboard belongs to.
     *
     * @return the owner player
     * @since 1.0.0
     */
    @NotNull
    UnifiedPlayer getPlayer();

    /**
     * Returns the title of this scoreboard.
     *
     * @return the title
     * @since 1.0.0
     */
    @NotNull
    ScoreboardTitle getTitle();

    /**
     * Sets the title of this scoreboard.
     *
     * @param title the new title
     * @since 1.0.0
     */
    void setTitle(@NotNull ScoreboardTitle title);

    /**
     * Sets a static title for this scoreboard.
     *
     * @param title the title text
     * @since 1.0.0
     */
    void setTitle(@NotNull Component title);

    /**
     * Returns all lines in this scoreboard.
     *
     * @return the list of lines
     * @since 1.0.0
     */
    @NotNull
    List<ScoreboardLine> getLines();

    /**
     * Returns the line at the specified index.
     *
     * @param index the line index
     * @return the line if present
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardLine> getLine(int index);

    /**
     * Sets a line at the specified index.
     *
     * @param index the line index
     * @param line  the line to set
     * @since 1.0.0
     */
    void setLine(int index, @NotNull ScoreboardLine line);

    /**
     * Adds a line to the end of the scoreboard.
     *
     * @param line the line to add
     * @since 1.0.0
     */
    void addLine(@NotNull ScoreboardLine line);

    /**
     * Removes the line at the specified index.
     *
     * @param index the line index
     * @return the removed line if present
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardLine> removeLine(int index);

    /**
     * Clears all lines from this scoreboard.
     *
     * @since 1.0.0
     */
    void clearLines();

    /**
     * Returns the number of lines.
     *
     * @return the line count
     * @since 1.0.0
     */
    int getLineCount();

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
     * Returns whether this scoreboard is currently visible.
     *
     * @return true if visible
     * @since 1.0.0
     */
    boolean isVisible();

    /**
     * Updates the scoreboard content.
     *
     * @since 1.0.0
     */
    void update();

    /**
     * Updates a specific line.
     *
     * @param index the line index
     * @since 1.0.0
     */
    void updateLine(int index);

    /**
     * Updates the title.
     *
     * @since 1.0.0
     */
    void updateTitle();

    /**
     * Destroys this scoreboard.
     *
     * @since 1.0.0
     */
    void destroy();

    /**
     * Returns whether this scoreboard has been destroyed.
     *
     * @return true if destroyed
     * @since 1.0.0
     */
    boolean isDestroyed();

    /**
     * Returns metadata for this scoreboard.
     *
     * @param key the metadata key
     * @param <T> the value type
     * @return the metadata value or null
     * @since 1.0.0
     */
    @Nullable
    <T> T getMetadata(@NotNull String key);

    /**
     * Sets metadata on this scoreboard.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @param <T>   the value type
     * @since 1.0.0
     */
    <T> void setMetadata(@NotNull String key, @Nullable T value);
}
