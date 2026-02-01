/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages teams for scoreboard functionality.
 *
 * <p>Teams can be used for player name formatting, collision rules,
 * friendly fire settings, and tab list sorting.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface TeamManager {

    /**
     * Creates a new team with the given name.
     *
     * @param name the team name
     * @return the created team
     * @throws IllegalArgumentException if a team with the name already exists
     * @since 1.0.0
     */
    @NotNull
    ScoreboardTeam createTeam(@NotNull String name);

    /**
     * Creates a new team with the given name and display name.
     *
     * @param name        the team name
     * @param displayName the team display name
     * @return the created team
     * @since 1.0.0
     */
    @NotNull
    ScoreboardTeam createTeam(@NotNull String name, @NotNull Component displayName);

    /**
     * Gets a team by name.
     *
     * @param name the team name
     * @return the team if found
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardTeam> getTeam(@NotNull String name);

    /**
     * Gets a team by name or throws if not found.
     *
     * @param name the team name
     * @return the team
     * @throws IllegalArgumentException if no team with the name exists
     * @since 1.0.0
     */
    @NotNull
    default ScoreboardTeam getTeamOrThrow(@NotNull String name) {
        return getTeam(name).orElseThrow(() ->
                new IllegalArgumentException("Team not found: " + name));
    }

    /**
     * Removes a team.
     *
     * @param name the team name
     * @return true if the team was removed
     * @since 1.0.0
     */
    boolean removeTeam(@NotNull String name);

    /**
     * Removes a team.
     *
     * @param team the team to remove
     * @return true if the team was removed
     * @since 1.0.0
     */
    boolean removeTeam(@NotNull ScoreboardTeam team);

    /**
     * Checks if a team exists.
     *
     * @param name the team name
     * @return true if the team exists
     * @since 1.0.0
     */
    boolean hasTeam(@NotNull String name);

    /**
     * Returns all teams.
     *
     * @return a collection of all teams
     * @since 1.0.0
     */
    @NotNull
    Collection<ScoreboardTeam> getTeams();

    /**
     * Returns the number of teams.
     *
     * @return the team count
     * @since 1.0.0
     */
    int getTeamCount();

    /**
     * Gets the team a player is on.
     *
     * @param player the player
     * @return the player's team if on one
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardTeam> getPlayerTeam(@NotNull UnifiedPlayer player);

    /**
     * Gets the team a player is on by UUID.
     *
     * @param playerId the player's UUID
     * @return the player's team if on one
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardTeam> getPlayerTeam(@NotNull UUID playerId);

    /**
     * Adds a player to a team.
     *
     * @param player   the player
     * @param teamName the team name
     * @throws IllegalArgumentException if the team doesn't exist
     * @since 1.0.0
     */
    void addPlayerToTeam(@NotNull UnifiedPlayer player, @NotNull String teamName);

    /**
     * Removes a player from their current team.
     *
     * @param player the player
     * @return true if the player was removed from a team
     * @since 1.0.0
     */
    boolean removePlayerFromTeam(@NotNull UnifiedPlayer player);

    /**
     * Removes a player from their current team by UUID.
     *
     * @param playerId the player's UUID
     * @return true if the player was removed from a team
     * @since 1.0.0
     */
    boolean removePlayerFromTeam(@NotNull UUID playerId);

    /**
     * Clears all teams.
     *
     * @since 1.0.0
     */
    void clearTeams();

    /**
     * Updates all teams for a specific player.
     *
     * @param player the player to update for
     * @since 1.0.0
     */
    void updateForPlayer(@NotNull UnifiedPlayer player);

    /**
     * Represents a scoreboard team.
     *
     * @since 1.0.0
     */
    interface ScoreboardTeam {

        /**
         * Returns the team name.
         *
         * @return the team name
         * @since 1.0.0
         */
        @NotNull
        String getName();

        /**
         * Returns the display name.
         *
         * @return the display name
         * @since 1.0.0
         */
        @NotNull
        Component getDisplayName();

        /**
         * Sets the display name.
         *
         * @param displayName the new display name
         * @since 1.0.0
         */
        void setDisplayName(@NotNull Component displayName);

        /**
         * Returns the prefix shown before player names.
         *
         * @return the prefix
         * @since 1.0.0
         */
        @NotNull
        Component getPrefix();

        /**
         * Sets the prefix shown before player names.
         *
         * @param prefix the prefix
         * @since 1.0.0
         */
        void setPrefix(@NotNull Component prefix);

        /**
         * Returns the suffix shown after player names.
         *
         * @return the suffix
         * @since 1.0.0
         */
        @NotNull
        Component getSuffix();

        /**
         * Sets the suffix shown after player names.
         *
         * @param suffix the suffix
         * @since 1.0.0
         */
        void setSuffix(@NotNull Component suffix);

        /**
         * Returns the team color.
         *
         * @return the team color, or null if not set
         * @since 1.0.0
         */
        @Nullable
        NamedTextColor getColor();

        /**
         * Sets the team color.
         *
         * @param color the team color
         * @since 1.0.0
         */
        void setColor(@Nullable NamedTextColor color);

        /**
         * Returns whether friendly fire is allowed.
         *
         * @return true if friendly fire is allowed
         * @since 1.0.0
         */
        boolean isAllowFriendlyFire();

        /**
         * Sets whether friendly fire is allowed.
         *
         * @param allowFriendlyFire true to allow friendly fire
         * @since 1.0.0
         */
        void setAllowFriendlyFire(boolean allowFriendlyFire);

        /**
         * Returns whether invisible teammates can be seen.
         *
         * @return true if invisible teammates can be seen
         * @since 1.0.0
         */
        boolean isCanSeeFriendlyInvisibles();

        /**
         * Sets whether invisible teammates can be seen.
         *
         * @param canSeeFriendlyInvisibles true if invisible teammates can be seen
         * @since 1.0.0
         */
        void setCanSeeFriendlyInvisibles(boolean canSeeFriendlyInvisibles);

        /**
         * Returns all member UUIDs.
         *
         * @return a collection of member UUIDs
         * @since 1.0.0
         */
        @NotNull
        Collection<UUID> getMembers();

        /**
         * Returns the number of members.
         *
         * @return the member count
         * @since 1.0.0
         */
        int getMemberCount();

        /**
         * Checks if a player is on this team.
         *
         * @param playerId the player's UUID
         * @return true if the player is on this team
         * @since 1.0.0
         */
        boolean hasMember(@NotNull UUID playerId);

        /**
         * Adds a member to this team.
         *
         * @param playerId the player's UUID
         * @since 1.0.0
         */
        void addMember(@NotNull UUID playerId);

        /**
         * Removes a member from this team.
         *
         * @param playerId the player's UUID
         * @return true if the member was removed
         * @since 1.0.0
         */
        boolean removeMember(@NotNull UUID playerId);

        /**
         * Clears all members from this team.
         *
         * @since 1.0.0
         */
        void clearMembers();

        /**
         * Updates this team for all viewers.
         *
         * @since 1.0.0
         */
        void update();
    }
}
