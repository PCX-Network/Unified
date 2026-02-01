/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for creating and managing per-player scoreboards.
 *
 * <p>The ScoreboardService provides easy sidebar scoreboard creation with
 * support for animated titles, dynamic line updates, and per-player visibility.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Per-player scoreboards</li>
 *   <li>Animated titles with frame cycling</li>
 *   <li>Dynamic line updates without flickering</li>
 *   <li>Placeholder integration</li>
 *   <li>Conditional line visibility</li>
 *   <li>Team-based shared scoreboards</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private ScoreboardService scoreboards;
 *
 * // Create scoreboard for player
 * Scoreboard board = scoreboards.create(player)
 *     .title(Component.text("My Server", NamedTextColor.GOLD))
 *     .line(10, Component.text("Balance: ${balance}"))
 *     .line(9, Component.empty())
 *     .line(8, Component.text("Kills: {kills}"))
 *     .updateInterval(20)
 *     .build();
 *
 * // Update specific line
 * board.setLine(10, Component.text("Balance: $" + balance));
 *
 * // Animated title
 * board.animateTitle(List.of(
 *     Component.text("My Server", NamedTextColor.GOLD),
 *     Component.text("My Server", NamedTextColor.YELLOW)
 * ), 10);
 *
 * // Remove scoreboard
 * scoreboards.remove(player);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Scoreboard
 * @see ScoreboardBuilder
 */
public interface ScoreboardService extends Service {

    /**
     * Creates a builder for a new scoreboard for the specified player.
     *
     * <p>If the player already has a scoreboard, it will be replaced.
     *
     * @param player the player to create the scoreboard for
     * @return a scoreboard builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder create(@NotNull UnifiedPlayer player);

    /**
     * Returns the scoreboard for a player.
     *
     * @param player the player
     * @return an Optional containing the scoreboard if one exists
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> getScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Returns the scoreboard for a player by UUID.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the scoreboard if one exists
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> getScoreboard(@NotNull UUID playerId);

    /**
     * Checks if a player has a scoreboard.
     *
     * @param player the player
     * @return true if the player has a scoreboard
     * @since 1.0.0
     */
    boolean hasScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Returns all active scoreboards.
     *
     * @return an unmodifiable collection of all scoreboards
     * @since 1.0.0
     */
    @NotNull
    @Unmodifiable
    Collection<Scoreboard> getAll();

    /**
     * Returns the number of active scoreboards.
     *
     * @return the scoreboard count
     * @since 1.0.0
     */
    int getCount();

    /**
     * Removes the scoreboard for a player.
     *
     * @param player the player
     * @return true if a scoreboard was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UnifiedPlayer player);

    /**
     * Removes the scoreboard for a player by UUID.
     *
     * @param playerId the player's UUID
     * @return true if a scoreboard was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UUID playerId);

    /**
     * Removes all scoreboards.
     *
     * @since 1.0.0
     */
    void removeAll();

    /**
     * Shows a player's scoreboard if they have one.
     *
     * @param player the player
     * @since 1.0.0
     */
    void show(@NotNull UnifiedPlayer player);

    /**
     * Hides a player's scoreboard without removing it.
     *
     * @param player the player
     * @since 1.0.0
     */
    void hide(@NotNull UnifiedPlayer player);

    /**
     * Toggles a player's scoreboard visibility.
     *
     * @param player the player
     * @since 1.0.0
     */
    void toggle(@NotNull UnifiedPlayer player);

    /**
     * Forces an update of all scoreboards.
     *
     * @since 1.0.0
     */
    void updateAll();
}
