/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.player;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents a group of players sharing the same scoreboard.
 *
 * <p>Scoreboard groups allow multiple players to view the same scoreboard
 * with shared updates and synchronized content.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface ScoreboardGroup {

    /**
     * Returns the unique identifier for this group.
     *
     * @return the group ID
     * @since 1.0.0
     */
    @NotNull
    String getId();

    /**
     * Returns the scoreboard for this group.
     *
     * @return the scoreboard
     * @since 1.0.0
     */
    @NotNull
    Scoreboard getScoreboard();

    /**
     * Sets the scoreboard for this group.
     *
     * @param scoreboard the new scoreboard
     * @since 1.0.0
     */
    void setScoreboard(@NotNull Scoreboard scoreboard);

    /**
     * Adds a player to this group.
     *
     * @param player the player to add
     * @since 1.0.0
     */
    void addPlayer(@NotNull UnifiedPlayer player);

    /**
     * Removes a player from this group.
     *
     * @param player the player to remove
     * @return true if the player was removed
     * @since 1.0.0
     */
    boolean removePlayer(@NotNull UnifiedPlayer player);

    /**
     * Removes a player from this group by UUID.
     *
     * @param playerId the player's UUID
     * @return true if the player was removed
     * @since 1.0.0
     */
    boolean removePlayer(@NotNull UUID playerId);

    /**
     * Checks if a player is in this group.
     *
     * @param player the player to check
     * @return true if the player is in the group
     * @since 1.0.0
     */
    boolean containsPlayer(@NotNull UnifiedPlayer player);

    /**
     * Checks if a player is in this group by UUID.
     *
     * @param playerId the player's UUID
     * @return true if the player is in the group
     * @since 1.0.0
     */
    boolean containsPlayer(@NotNull UUID playerId);

    /**
     * Returns all player UUIDs in this group.
     *
     * @return a collection of player UUIDs
     * @since 1.0.0
     */
    @NotNull
    Collection<UUID> getPlayerIds();

    /**
     * Returns the number of players in this group.
     *
     * @return the player count
     * @since 1.0.0
     */
    int getPlayerCount();

    /**
     * Clears all players from this group.
     *
     * @since 1.0.0
     */
    void clear();

    /**
     * Updates the scoreboard for all players in this group.
     *
     * @since 1.0.0
     */
    void update();

    /**
     * Destroys this group, removing all players and hiding the scoreboard.
     *
     * @since 1.0.0
     */
    void destroy();

    /**
     * Returns whether this group has been destroyed.
     *
     * @return true if destroyed
     * @since 1.0.0
     */
    boolean isDestroyed();
}
