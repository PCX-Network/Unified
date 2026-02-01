/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.group;

import sh.pcx.unified.i18n.permissions.check.PermissionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing permission groups and player memberships.
 *
 * <p>GroupManager provides operations for:
 * <ul>
 *   <li>Creating, deleting, and querying groups</li>
 *   <li>Managing player group memberships</li>
 *   <li>Temporary group assignments</li>
 *   <li>Primary group management</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * GroupManager groups = permService.getGroups();
 *
 * // Get all groups
 * Collection<Group> allGroups = groups.getGroups();
 *
 * // Get a specific group
 * Optional<Group> vipGroup = groups.getGroup("vip");
 *
 * // Create a new group
 * groups.createGroup("elite").thenAccept(group -> {
 *     group.setPrefix("[Elite] ");
 *     group.addPermission("elite.perks");
 * });
 *
 * // Add player to group
 * groups.addMember(playerId, "vip");
 *
 * // Add player to temporary group
 * groups.addTemporaryMember(playerId, "vip", Duration.ofDays(30));
 *
 * // Set player's primary group
 * groups.setPrimaryGroup(playerId, "admin");
 *
 * // Get player's groups
 * Collection<GroupMembership> memberships = groups.getMemberships(playerId);
 *
 * // Check if player is in group
 * boolean isVip = groups.isMember(playerId, "vip");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Group
 * @see GroupMembership
 */
public interface GroupManager {

    /**
     * Gets all registered groups.
     *
     * @return a collection of all groups
     * @since 1.0.0
     */
    @NotNull
    Collection<Group> getGroups();

    /**
     * Gets a group by name.
     *
     * @param name the group name (case-insensitive)
     * @return an Optional containing the group if found
     * @throws NullPointerException if name is null
     * @since 1.0.0
     */
    @NotNull
    Optional<Group> getGroup(@NotNull String name);

    /**
     * Gets a group by name, throwing if not found.
     *
     * @param name the group name
     * @return the group
     * @throws IllegalArgumentException if the group doesn't exist
     * @since 1.0.0
     */
    @NotNull
    default Group getGroupOrThrow(@NotNull String name) {
        return getGroup(name).orElseThrow(() ->
                new IllegalArgumentException("Group not found: " + name));
    }

    /**
     * Checks if a group exists.
     *
     * @param name the group name
     * @return true if the group exists
     * @throws NullPointerException if name is null
     * @since 1.0.0
     */
    default boolean groupExists(@NotNull String name) {
        return getGroup(name).isPresent();
    }

    /**
     * Creates a new group.
     *
     * @param name the group name
     * @return a future that completes with the created group
     * @throws NullPointerException if name is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Group> createGroup(@NotNull String name);

    /**
     * Deletes a group.
     *
     * @param name the group name
     * @return a future that completes with true if deleted
     * @throws NullPointerException if name is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> deleteGroup(@NotNull String name);

    /**
     * Gets the default group.
     *
     * @return an Optional containing the default group
     * @since 1.0.0
     */
    @NotNull
    Optional<Group> getDefaultGroup();

    /**
     * Gets all group memberships for a player.
     *
     * @param playerId the player's UUID
     * @return a collection of group memberships
     * @throws NullPointerException if playerId is null
     * @since 1.0.0
     */
    @NotNull
    Collection<GroupMembership> getMemberships(@NotNull UUID playerId);

    /**
     * Gets all group memberships for a player in a context.
     *
     * @param playerId the player's UUID
     * @param context  the context to filter by
     * @return a collection of applicable memberships
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    Collection<GroupMembership> getMemberships(@NotNull UUID playerId, @NotNull PermissionContext context);

    /**
     * Gets all groups a player is a member of.
     *
     * @param playerId the player's UUID
     * @return a collection of groups
     * @throws NullPointerException if playerId is null
     * @since 1.0.0
     */
    @NotNull
    default Collection<Group> getPlayerGroups(@NotNull UUID playerId) {
        return getMemberships(playerId).stream()
                .map(GroupMembership::getGroup)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Gets a player's primary group.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the primary group
     * @throws NullPointerException if playerId is null
     * @since 1.0.0
     */
    @NotNull
    Optional<Group> getPrimaryGroup(@NotNull UUID playerId);

    /**
     * Gets the name of a player's primary group.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the primary group name
     * @since 1.0.0
     */
    @NotNull
    default Optional<String> getPrimaryGroupName(@NotNull UUID playerId) {
        return getPrimaryGroup(playerId).map(Group::getName);
    }

    /**
     * Sets a player's primary group.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @return a future that completes when done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setPrimaryGroup(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Checks if a player is a member of a group.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @return true if the player is a member
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    boolean isMember(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Checks if a player is a member of a group in a context.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @param context   the context
     * @return true if the player is a member in the context
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    boolean isMember(@NotNull UUID playerId, @NotNull String groupName, @NotNull PermissionContext context);

    /**
     * Adds a player to a group.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @return a future that completes when done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> addMember(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Adds a player to a group with context.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @param context   the context
     * @return a future that completes when done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> addMember(@NotNull UUID playerId, @NotNull String groupName, @NotNull PermissionContext context);

    /**
     * Adds a player to a group temporarily.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @param duration  how long the membership should last
     * @return a future that completes when done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> addTemporaryMember(@NotNull UUID playerId, @NotNull String groupName, @NotNull Duration duration);

    /**
     * Adds a player to a group temporarily with context.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @param duration  how long the membership should last
     * @param context   the context
     * @return a future that completes when done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> addTemporaryMember(
            @NotNull UUID playerId,
            @NotNull String groupName,
            @NotNull Duration duration,
            @NotNull PermissionContext context
    );

    /**
     * Removes a player from a group.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @return a future that completes with true if removed
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> removeMember(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Removes a player from a group in a specific context.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @param context   the context
     * @return a future that completes with true if removed
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> removeMember(@NotNull UUID playerId, @NotNull String groupName, @NotNull PermissionContext context);

    /**
     * Removes a player from all groups except the default group.
     *
     * @param playerId the player's UUID
     * @return a future that completes with the number of groups removed from
     * @throws NullPointerException if playerId is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> removeFromAllGroups(@NotNull UUID playerId);

    /**
     * Gets all members of a group.
     *
     * @param groupName the group name
     * @return a future that completes with the member UUIDs
     * @throws NullPointerException if groupName is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<UUID>> getMembers(@NotNull String groupName);

    /**
     * Gets the track a group belongs to.
     *
     * <p>Tracks define promotion/demotion paths between groups.
     *
     * @param groupName the group name
     * @return an Optional containing the track name
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getTrack(@NotNull String groupName);

    /**
     * Gets all groups in a track.
     *
     * @param trackName the track name
     * @return a list of groups in order
     * @since 1.0.0
     */
    @NotNull
    Collection<Group> getTrackGroups(@NotNull String trackName);

    /**
     * Promotes a player along a track.
     *
     * @param playerId  the player's UUID
     * @param trackName the track name
     * @return a future that completes with the new group, or empty if at top
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Group>> promote(@NotNull UUID playerId, @NotNull String trackName);

    /**
     * Demotes a player along a track.
     *
     * @param playerId  the player's UUID
     * @param trackName the track name
     * @return a future that completes with the new group, or empty if at bottom
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Group>> demote(@NotNull UUID playerId, @NotNull String trackName);

    /**
     * Gets all available track names.
     *
     * @return a collection of track names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getTracks();

    /**
     * Calculates the effective group for a player.
     *
     * <p>This returns the group with the highest weight among all groups
     * the player is a member of.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the effective group
     * @since 1.0.0
     */
    @NotNull
    Optional<Group> getEffectiveGroup(@NotNull UUID playerId);

    /**
     * Calculates the effective group for a player in a context.
     *
     * @param playerId the player's UUID
     * @param context  the context
     * @return an Optional containing the effective group
     * @since 1.0.0
     */
    @NotNull
    Optional<Group> getEffectiveGroup(@NotNull UUID playerId, @NotNull PermissionContext context);
}
