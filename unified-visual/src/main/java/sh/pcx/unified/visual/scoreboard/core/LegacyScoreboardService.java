/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.core;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Legacy service for managing per-player scoreboards.
 *
 * <p>This service manages the traditional per-player scoreboard model where
 * each player has their own dedicated scoreboard instance.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LegacyScoreboard
 * @see sh.pcx.unified.visual.scoreboard.ScoreboardService
 */
public interface LegacyScoreboardService extends Service {

    /**
     * Creates a new scoreboard for a player.
     *
     * @param player the player
     * @return the created scoreboard
     * @since 1.0.0
     */
    @NotNull
    LegacyScoreboard create(@NotNull UnifiedPlayer player);

    /**
     * Gets the scoreboard for a player.
     *
     * @param player the player
     * @return the player's scoreboard if present
     * @since 1.0.0
     */
    @NotNull
    Optional<LegacyScoreboard> getScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Gets the scoreboard for a player by UUID.
     *
     * @param playerId the player's UUID
     * @return the player's scoreboard if present
     * @since 1.0.0
     */
    @NotNull
    Optional<LegacyScoreboard> getScoreboard(@NotNull UUID playerId);

    /**
     * Checks if a player has a scoreboard.
     *
     * @param player the player
     * @return true if the player has a scoreboard
     * @since 1.0.0
     */
    boolean hasScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Gets all scoreboards.
     *
     * @return all scoreboards
     * @since 1.0.0
     */
    @NotNull
    Collection<LegacyScoreboard> getAll();

    /**
     * Gets the count of scoreboards.
     *
     * @return the scoreboard count
     * @since 1.0.0
     */
    int getCount();

    /**
     * Removes a player's scoreboard.
     *
     * @param player the player
     * @return true if a scoreboard was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UnifiedPlayer player);

    /**
     * Removes a player's scoreboard by UUID.
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
     * Shows the scoreboard to a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void show(@NotNull UnifiedPlayer player);

    /**
     * Hides the scoreboard from a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void hide(@NotNull UnifiedPlayer player);

    /**
     * Toggles the scoreboard visibility for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void toggle(@NotNull UnifiedPlayer player);

    /**
     * Updates all scoreboards.
     *
     * @since 1.0.0
     */
    void updateAll();
}
