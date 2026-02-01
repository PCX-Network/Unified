/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.group;

import sh.pcx.unified.i18n.permissions.check.PermissionContext;
import sh.pcx.unified.i18n.permissions.core.Permission;
import sh.pcx.unified.i18n.permissions.core.TriState;
import sh.pcx.unified.i18n.permissions.meta.Prefix;
import sh.pcx.unified.i18n.permissions.meta.Suffix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a permission group.
 *
 * <p>Groups are named collections of permissions that can be assigned to players.
 * They support:
 * <ul>
 *   <li>Permission inheritance from parent groups</li>
 *   <li>Prefix and suffix for chat formatting</li>
 *   <li>Weight/priority for ordering</li>
 *   <li>Context-aware permissions</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * GroupManager groups = permService.getGroups();
 *
 * // Get a group
 * Optional<Group> adminGroup = groups.getGroup("admin");
 *
 * adminGroup.ifPresent(group -> {
 *     // Get group info
 *     String name = group.getName();
 *     int weight = group.getWeight().orElse(0);
 *
 *     // Get group prefix
 *     String prefix = group.getPrefix()
 *         .map(Prefix::getValue)
 *         .orElse("");
 *
 *     // Check group permissions
 *     Collection<Permission> perms = group.getPermissions().join();
 *
 *     // Get parent groups
 *     Collection<Group> parents = group.getParents();
 *
 *     // Check if group has permission
 *     boolean canTeleport = group.hasPermission("essentials.teleport");
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see GroupManager
 * @see GroupMembership
 */
public interface Group {

    /**
     * Returns the unique name of this group.
     *
     * <p>Group names are case-insensitive and typically lowercase.
     *
     * @return the group name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the display name of this group.
     *
     * <p>The display name may include formatting and is used for display purposes.
     * If not set, this returns the same value as {@link #getName()}.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    String getDisplayName();

    /**
     * Returns the weight/priority of this group.
     *
     * <p>Higher weight groups take precedence. When a player is in multiple groups,
     * the group with the highest weight is used for prefix/suffix selection.
     *
     * @return an OptionalInt containing the weight if set
     * @since 1.0.0
     */
    @NotNull
    OptionalInt getWeight();

    /**
     * Returns the prefix for this group.
     *
     * @return an Optional containing the prefix if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Prefix> getPrefix();

    /**
     * Returns the prefix for this group in a specific context.
     *
     * @param context the context
     * @return an Optional containing the prefix if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Prefix> getPrefix(@NotNull PermissionContext context);

    /**
     * Returns the suffix for this group.
     *
     * @return an Optional containing the suffix if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Suffix> getSuffix();

    /**
     * Returns the suffix for this group in a specific context.
     *
     * @param context the context
     * @return an Optional containing the suffix if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Suffix> getSuffix(@NotNull PermissionContext context);

    /**
     * Returns all permissions directly assigned to this group.
     *
     * <p>This does not include inherited permissions from parent groups.
     *
     * @return a future that completes with the group's permissions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Permission>> getPermissions();

    /**
     * Returns all permissions for this group in a specific context.
     *
     * @param context the context
     * @return a future that completes with the permissions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Permission>> getPermissions(@NotNull PermissionContext context);

    /**
     * Returns all permissions including inherited ones.
     *
     * @return a future that completes with all effective permissions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Permission>> getAllPermissions();

    /**
     * Checks if this group has a permission.
     *
     * @param permission the permission node
     * @return true if the group has the permission
     * @since 1.0.0
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Checks if this group has a permission with context.
     *
     * @param permission the permission node
     * @param context    the context
     * @return true if the group has the permission
     * @since 1.0.0
     */
    boolean hasPermission(@NotNull String permission, @NotNull PermissionContext context);

    /**
     * Gets the value of a permission for this group.
     *
     * @param permission the permission node
     * @return the permission value
     * @since 1.0.0
     */
    @NotNull
    TriState getPermissionValue(@NotNull String permission);

    /**
     * Gets the value of a permission with context.
     *
     * @param permission the permission node
     * @param context    the context
     * @return the permission value
     * @since 1.0.0
     */
    @NotNull
    TriState getPermissionValue(@NotNull String permission, @NotNull PermissionContext context);

    /**
     * Adds a permission to this group.
     *
     * @param permission the permission to add
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> addPermission(@NotNull Permission permission);

    /**
     * Adds a permission node to this group.
     *
     * @param permission the permission node
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> addPermission(@NotNull String permission) {
        return addPermission(Permission.of(permission));
    }

    /**
     * Removes a permission from this group.
     *
     * @param permission the permission node
     * @return a future that completes with true if removed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> removePermission(@NotNull String permission);

    /**
     * Removes a permission from this group.
     *
     * @param permission the permission to remove
     * @return a future that completes with true if removed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> removePermission(@NotNull Permission permission);

    /**
     * Sets a permission value for this group.
     *
     * @param permission the permission node
     * @param value      true to grant, false to deny
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setPermission(@NotNull String permission, boolean value);

    /**
     * Returns the parent groups this group inherits from.
     *
     * @return a collection of parent groups
     * @since 1.0.0
     */
    @NotNull
    Collection<Group> getParents();

    /**
     * Adds a parent group.
     *
     * @param parent the parent group name
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> addParent(@NotNull String parent);

    /**
     * Removes a parent group.
     *
     * @param parent the parent group name
     * @return a future that completes with true if removed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> removeParent(@NotNull String parent);

    /**
     * Sets the weight of this group.
     *
     * @param weight the new weight
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setWeight(int weight);

    /**
     * Sets the display name of this group.
     *
     * @param displayName the new display name
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setDisplayName(@NotNull String displayName);

    /**
     * Sets the prefix for this group.
     *
     * @param prefix the prefix value, or null to remove
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setPrefix(@Nullable String prefix);

    /**
     * Sets the prefix for this group with priority.
     *
     * @param prefix   the prefix value
     * @param priority the priority
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setPrefix(@NotNull String prefix, int priority);

    /**
     * Sets the suffix for this group.
     *
     * @param suffix the suffix value, or null to remove
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setSuffix(@Nullable String suffix);

    /**
     * Sets the suffix for this group with priority.
     *
     * @param suffix   the suffix value
     * @param priority the priority
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setSuffix(@NotNull String suffix, int priority);

    /**
     * Checks if this is the default group.
     *
     * <p>The default group is automatically assigned to new players.
     *
     * @return true if this is the default group
     * @since 1.0.0
     */
    boolean isDefault();

    /**
     * Returns the number of members in this group.
     *
     * @return a future that completes with the member count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> getMemberCount();

    /**
     * Clears all permissions from this group.
     *
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> clearPermissions();

    /**
     * Gets a metadata value for this group.
     *
     * @param key the metadata key
     * @return an Optional containing the value if set
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getMeta(@NotNull String key);

    /**
     * Sets a metadata value for this group.
     *
     * @param key   the metadata key
     * @param value the value, or null to remove
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setMeta(@NotNull String key, @Nullable String value);
}
