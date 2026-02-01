/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.scoreboard.line.ScoreboardLine;
import sh.pcx.unified.visual.scoreboard.title.ScoreboardTitle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a scoreboard that can be displayed to players.
 *
 * <p>Scoreboards are visual displays shown on the right side of the player's screen,
 * containing a title and up to 15 lines of text. This interface provides methods
 * for managing scoreboard content and visibility.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Dynamic titles with animation support</li>
 *   <li>Static, dynamic, and conditional lines</li>
 *   <li>Placeholder integration for variable content</li>
 *   <li>Flicker-free updates</li>
 *   <li>Per-player content support</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a scoreboard using the builder
 * Scoreboard scoreboard = ScoreboardBuilder.create("lobby")
 *     .title(Component.text("My Server"))
 *     .line(StaticLine.of(Component.text("Welcome!")))
 *     .line(DynamicLine.of(player -> Component.text("Players: " + getPlayerCount())))
 *     .build();
 *
 * // Show to a player
 * scoreboard.show(player);
 *
 * // Update the scoreboard
 * scoreboard.update();
 *
 * // Hide from player
 * scoreboard.hide(player);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as scoreboards may be updated from
 * asynchronous tasks.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ScoreboardBuilder
 * @see ScoreboardService
 * @see ScoreboardLine
 */
public interface Scoreboard {

    /**
     * Returns the unique identifier for this scoreboard.
     *
     * @return the scoreboard's unique ID
     * @since 1.0.0
     */
    @NotNull
    String getId();

    /**
     * Returns the title of this scoreboard.
     *
     * @return the scoreboard title
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
     * Returns the current title text for a specific player.
     *
     * <p>For animated titles, this returns the current frame.
     *
     * @param player the player to get the title for
     * @return the current title text
     * @since 1.0.0
     */
    @NotNull
    Component getCurrentTitle(@NotNull UnifiedPlayer player);

    /**
     * Returns all lines in this scoreboard.
     *
     * @return an unmodifiable list of lines
     * @since 1.0.0
     */
    @NotNull
    List<ScoreboardLine> getLines();

    /**
     * Returns the line at the specified index.
     *
     * @param index the line index (0-based)
     * @return an Optional containing the line if it exists
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardLine> getLine(int index);

    /**
     * Sets a line at the specified index.
     *
     * @param index the line index (0-based, max 14)
     * @param line  the line to set
     * @throws IndexOutOfBoundsException if index is out of range
     * @since 1.0.0
     */
    void setLine(int index, @NotNull ScoreboardLine line);

    /**
     * Adds a line to the end of the scoreboard.
     *
     * @param line the line to add
     * @throws IllegalStateException if the scoreboard already has 15 lines
     * @since 1.0.0
     */
    void addLine(@NotNull ScoreboardLine line);

    /**
     * Inserts a line at the specified index.
     *
     * @param index the index to insert at
     * @param line  the line to insert
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws IllegalStateException     if the scoreboard already has 15 lines
     * @since 1.0.0
     */
    void insertLine(int index, @NotNull ScoreboardLine line);

    /**
     * Removes the line at the specified index.
     *
     * @param index the line index to remove
     * @return the removed line, or empty if index was invalid
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardLine> removeLine(int index);

    /**
     * Removes all lines from this scoreboard.
     *
     * @since 1.0.0
     */
    void clearLines();

    /**
     * Returns the number of lines in this scoreboard.
     *
     * @return the line count
     * @since 1.0.0
     */
    int getLineCount();

    /**
     * Shows this scoreboard to a player.
     *
     * @param player the player to show the scoreboard to
     * @since 1.0.0
     */
    void show(@NotNull UnifiedPlayer player);

    /**
     * Hides this scoreboard from a player.
     *
     * @param player the player to hide the scoreboard from
     * @since 1.0.0
     */
    void hide(@NotNull UnifiedPlayer player);

    /**
     * Checks if this scoreboard is currently visible to a player.
     *
     * @param player the player to check
     * @return true if the scoreboard is visible to the player
     * @since 1.0.0
     */
    boolean isVisibleTo(@NotNull UnifiedPlayer player);

    /**
     * Returns all players currently viewing this scoreboard.
     *
     * @return a list of viewer UUIDs
     * @since 1.0.0
     */
    @NotNull
    List<UUID> getViewers();

    /**
     * Returns the number of players viewing this scoreboard.
     *
     * @return the viewer count
     * @since 1.0.0
     */
    int getViewerCount();

    /**
     * Updates the scoreboard for all viewers.
     *
     * <p>This method refreshes all dynamic content including animated
     * titles, dynamic lines, and conditional lines.
     *
     * @since 1.0.0
     */
    void update();

    /**
     * Updates the scoreboard for a specific player.
     *
     * @param player the player to update the scoreboard for
     * @since 1.0.0
     */
    void update(@NotNull UnifiedPlayer player);

    /**
     * Updates a specific line for all viewers.
     *
     * @param index the line index to update
     * @since 1.0.0
     */
    void updateLine(int index);

    /**
     * Updates a specific line for a specific player.
     *
     * @param player the player to update for
     * @param index  the line index to update
     * @since 1.0.0
     */
    void updateLine(@NotNull UnifiedPlayer player, int index);

    /**
     * Updates the title for all viewers.
     *
     * @since 1.0.0
     */
    void updateTitle();

    /**
     * Destroys this scoreboard, removing it from all viewers.
     *
     * <p>After calling this method, the scoreboard should not be used again.
     *
     * @since 1.0.0
     */
    void destroy();

    /**
     * Checks if this scoreboard has been destroyed.
     *
     * @return true if the scoreboard is destroyed
     * @since 1.0.0
     */
    boolean isDestroyed();

    /**
     * Returns user-defined metadata associated with this scoreboard.
     *
     * @param key the metadata key
     * @param <T> the expected value type
     * @return the metadata value, or null if not set
     * @since 1.0.0
     */
    @Nullable
    <T> T getMetadata(@NotNull String key);

    /**
     * Sets user-defined metadata on this scoreboard.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @param <T>   the value type
     * @since 1.0.0
     */
    <T> void setMetadata(@NotNull String key, @Nullable T value);

    /**
     * Checks if this scoreboard has metadata with the given key.
     *
     * @param key the metadata key
     * @return true if metadata exists for the key
     * @since 1.0.0
     */
    boolean hasMetadata(@NotNull String key);

    /**
     * The maximum number of lines a scoreboard can have.
     */
    int MAX_LINES = 15;
}
