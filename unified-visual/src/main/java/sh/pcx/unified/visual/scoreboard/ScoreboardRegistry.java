/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Registry for managing active scoreboards.
 *
 * <p>The ScoreboardRegistry tracks all registered scoreboards and maintains
 * the mapping between players and their currently displayed scoreboards.
 * This is primarily used internally by the {@link ScoreboardService}.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Scoreboard registration and lookup</li>
 *   <li>Player-to-scoreboard mapping</li>
 *   <li>Active viewer tracking</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ScoreboardRegistry registry = ...;
 *
 * // Register a scoreboard
 * registry.register(scoreboard);
 *
 * // Get a scoreboard by ID
 * Optional<Scoreboard> board = registry.get("lobby");
 *
 * // Track player viewing
 * registry.setPlayerScoreboard(player.getUniqueId(), scoreboard);
 *
 * // Get player's current scoreboard
 * Optional<Scoreboard> current = registry.getPlayerScoreboard(player.getUniqueId());
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Scoreboard
 * @see ScoreboardService
 */
public interface ScoreboardRegistry {

    /**
     * Registers a scoreboard.
     *
     * @param scoreboard the scoreboard to register
     * @throws IllegalArgumentException if a scoreboard with the same ID already exists
     * @since 1.0.0
     */
    void register(@NotNull Scoreboard scoreboard);

    /**
     * Unregisters a scoreboard by its ID.
     *
     * <p>This also removes the scoreboard from all players currently viewing it.
     *
     * @param id the scoreboard ID
     * @return the unregistered scoreboard, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> unregister(@NotNull String id);

    /**
     * Unregisters a scoreboard.
     *
     * @param scoreboard the scoreboard to unregister
     * @return true if the scoreboard was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull Scoreboard scoreboard);

    /**
     * Retrieves a scoreboard by its ID.
     *
     * @param id the scoreboard ID
     * @return an Optional containing the scoreboard if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> get(@NotNull String id);

    /**
     * Checks if a scoreboard with the given ID is registered.
     *
     * @param id the scoreboard ID
     * @return true if the scoreboard is registered
     * @since 1.0.0
     */
    boolean contains(@NotNull String id);

    /**
     * Returns all registered scoreboards.
     *
     * @return an unmodifiable collection of all scoreboards
     * @since 1.0.0
     */
    @NotNull
    Collection<Scoreboard> getAll();

    /**
     * Returns all registered scoreboard IDs.
     *
     * @return an unmodifiable collection of all scoreboard IDs
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getAllIds();

    /**
     * Returns the number of registered scoreboards.
     *
     * @return the count of registered scoreboards
     * @since 1.0.0
     */
    int size();

    /**
     * Checks if the registry is empty.
     *
     * @return true if no scoreboards are registered
     * @since 1.0.0
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Sets the scoreboard currently displayed to a player.
     *
     * @param playerId   the player's UUID
     * @param scoreboard the scoreboard to set, or null to clear
     * @since 1.0.0
     */
    void setPlayerScoreboard(@NotNull UUID playerId, @Nullable Scoreboard scoreboard);

    /**
     * Sets the scoreboard currently displayed to a player.
     *
     * @param player     the player
     * @param scoreboard the scoreboard to set, or null to clear
     * @since 1.0.0
     */
    default void setPlayerScoreboard(@NotNull UnifiedPlayer player, @Nullable Scoreboard scoreboard) {
        setPlayerScoreboard(player.getUniqueId(), scoreboard);
    }

    /**
     * Gets the scoreboard currently displayed to a player.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the current scoreboard
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> getPlayerScoreboard(@NotNull UUID playerId);

    /**
     * Gets the scoreboard currently displayed to a player.
     *
     * @param player the player
     * @return an Optional containing the current scoreboard
     * @since 1.0.0
     */
    @NotNull
    default Optional<Scoreboard> getPlayerScoreboard(@NotNull UnifiedPlayer player) {
        return getPlayerScoreboard(player.getUniqueId());
    }

    /**
     * Clears the scoreboard for a player.
     *
     * @param playerId the player's UUID
     * @return the previously displayed scoreboard, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> clearPlayerScoreboard(@NotNull UUID playerId);

    /**
     * Clears the scoreboard for a player.
     *
     * @param player the player
     * @return the previously displayed scoreboard, or empty if none
     * @since 1.0.0
     */
    @NotNull
    default Optional<Scoreboard> clearPlayerScoreboard(@NotNull UnifiedPlayer player) {
        return clearPlayerScoreboard(player.getUniqueId());
    }

    /**
     * Checks if a player has a scoreboard displayed.
     *
     * @param playerId the player's UUID
     * @return true if the player has a scoreboard
     * @since 1.0.0
     */
    boolean hasPlayerScoreboard(@NotNull UUID playerId);

    /**
     * Checks if a player has a scoreboard displayed.
     *
     * @param player the player
     * @return true if the player has a scoreboard
     * @since 1.0.0
     */
    default boolean hasPlayerScoreboard(@NotNull UnifiedPlayer player) {
        return hasPlayerScoreboard(player.getUniqueId());
    }

    /**
     * Returns all players viewing a specific scoreboard.
     *
     * @param id the scoreboard ID
     * @return an unmodifiable collection of player UUIDs
     * @since 1.0.0
     */
    @NotNull
    Collection<UUID> getViewers(@NotNull String id);

    /**
     * Returns all players viewing a specific scoreboard.
     *
     * @param scoreboard the scoreboard
     * @return an unmodifiable collection of player UUIDs
     * @since 1.0.0
     */
    @NotNull
    default Collection<UUID> getViewers(@NotNull Scoreboard scoreboard) {
        return getViewers(scoreboard.getId());
    }

    /**
     * Returns the total number of players with active scoreboards.
     *
     * @return the count of players with scoreboards
     * @since 1.0.0
     */
    int getActivePlayerCount();

    /**
     * Removes a player from all tracking.
     *
     * <p>This should be called when a player disconnects.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void removePlayer(@NotNull UUID playerId);

    /**
     * Removes a player from all tracking.
     *
     * @param player the player
     * @since 1.0.0
     */
    default void removePlayer(@NotNull UnifiedPlayer player) {
        removePlayer(player.getUniqueId());
    }

    /**
     * Clears all registered scoreboards and player mappings.
     *
     * @since 1.0.0
     */
    void clear();
}
