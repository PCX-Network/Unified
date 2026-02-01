/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages world groups and provides group-related queries.
 *
 * <p>The WorldGroupManager is responsible for creating, retrieving, and
 * managing world groups. It also provides methods to query which group
 * a world belongs to.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Inject
 * private WorldService worlds;
 *
 * WorldGroupManager groups = worlds.getGroupManager();
 *
 * // Create a new group
 * WorldGroup survival = groups.createGroup("survival")
 *     .add("survival_overworld", "survival_nether", "survival_end")
 *     .sharedInventory(true)
 *     .sharedEnderChest(true)
 *     .build();
 *
 * // Get a group by name
 * Optional<WorldGroup> group = groups.getGroup("survival");
 *
 * // Get the group for a world
 * Optional<WorldGroup> worldGroup = groups.getGroupFor("survival_nether");
 *
 * // Check if two worlds are in the same group
 * boolean sameGroup = groups.areInSameGroup("survival_overworld", "survival_nether");
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldGroup
 * @see WorldService
 */
public interface WorldGroupManager {

    /**
     * Creates a new world group builder.
     *
     * @param name the group name
     * @return a new group builder
     * @since 1.0.0
     */
    @NotNull
    WorldGroup.Builder createGroup(@NotNull String name);

    /**
     * Gets a group by name.
     *
     * @param name the group name
     * @return the group, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<WorldGroup> getGroup(@NotNull String name);

    /**
     * Gets the group that contains the specified world.
     *
     * @param worldName the world name
     * @return the group containing the world, or empty if not in a group
     * @since 1.0.0
     */
    @NotNull
    Optional<WorldGroup> getGroupFor(@NotNull String worldName);

    /**
     * Gets the group that contains the specified world.
     *
     * @param world the world
     * @return the group containing the world, or empty if not in a group
     * @since 1.0.0
     */
    @NotNull
    Optional<WorldGroup> getGroupFor(@NotNull UnifiedWorld world);

    /**
     * Gets all registered groups.
     *
     * @return an unmodifiable collection of groups
     * @since 1.0.0
     */
    @NotNull
    Collection<WorldGroup> getGroups();

    /**
     * Gets all group names.
     *
     * @return an unmodifiable collection of group names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getGroupNames();

    /**
     * Checks if a group exists.
     *
     * @param name the group name
     * @return true if the group exists
     * @since 1.0.0
     */
    boolean hasGroup(@NotNull String name);

    /**
     * Checks if a world is in any group.
     *
     * @param worldName the world name
     * @return true if the world is in a group
     * @since 1.0.0
     */
    boolean isInGroup(@NotNull String worldName);

    /**
     * Checks if two worlds are in the same group.
     *
     * @param world1 the first world name
     * @param world2 the second world name
     * @return true if both worlds are in the same group
     * @since 1.0.0
     */
    boolean areInSameGroup(@NotNull String world1, @NotNull String world2);

    /**
     * Checks if two worlds are in the same group.
     *
     * @param world1 the first world
     * @param world2 the second world
     * @return true if both worlds are in the same group
     * @since 1.0.0
     */
    default boolean areInSameGroup(@NotNull UnifiedWorld world1, @NotNull UnifiedWorld world2) {
        return areInSameGroup(world1.getName(), world2.getName());
    }

    /**
     * Removes a group.
     *
     * @param name the group name
     * @return true if the group was removed
     * @since 1.0.0
     */
    boolean removeGroup(@NotNull String name);

    /**
     * Removes a world from all groups.
     *
     * @param worldName the world name
     * @return true if the world was removed from any group
     * @since 1.0.0
     */
    boolean removeWorldFromGroups(@NotNull String worldName);

    /**
     * Saves group configurations to disk.
     *
     * @since 1.0.0
     */
    void save();

    /**
     * Reloads group configurations from disk.
     *
     * @since 1.0.0
     */
    void reload();

    // ==================== Player Data Management ====================

    /**
     * Saves a player's data for a specific group.
     *
     * <p>This is called automatically when needed, but can be called manually
     * to force a save.
     *
     * @param playerId  the player UUID
     * @param groupName the group name
     * @since 1.0.0
     */
    void savePlayerData(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Loads a player's data for a specific group.
     *
     * <p>This is called automatically when a player switches between groups.
     *
     * @param playerId  the player UUID
     * @param groupName the group name
     * @since 1.0.0
     */
    void loadPlayerData(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Gets the data file name for a player in a group.
     *
     * @param playerId  the player UUID
     * @param groupName the group name
     * @return the data file identifier
     * @since 1.0.0
     */
    @NotNull
    String getPlayerDataKey(@NotNull UUID playerId, @NotNull String groupName);
}
