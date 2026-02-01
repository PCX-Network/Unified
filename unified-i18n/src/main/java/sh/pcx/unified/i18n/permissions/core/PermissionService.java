/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.core;

import sh.pcx.unified.i18n.permissions.check.PermissionCheck;
import sh.pcx.unified.i18n.permissions.check.PermissionChecker;
import sh.pcx.unified.i18n.permissions.check.PermissionContext;
import sh.pcx.unified.i18n.permissions.group.Group;
import sh.pcx.unified.i18n.permissions.group.GroupManager;
import sh.pcx.unified.i18n.permissions.group.GroupMembership;
import sh.pcx.unified.i18n.permissions.integration.PermissionProvider;
import sh.pcx.unified.i18n.permissions.meta.MetaManager;
import sh.pcx.unified.i18n.permissions.meta.MetaValue;
import sh.pcx.unified.i18n.permissions.meta.Prefix;
import sh.pcx.unified.i18n.permissions.meta.Suffix;
import sh.pcx.unified.i18n.permissions.temporary.TemporaryManager;
import sh.pcx.unified.i18n.permissions.temporary.TemporaryPermission;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main interface for the permissions abstraction layer.
 *
 * <p>PermissionService provides a unified API for permission management across
 * different permission plugins. It supports LuckPerms as the primary backend
 * with Vault as a fallback.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Permission checking with context support (world, server)</li>
 *   <li>Permission assignment and removal</li>
 *   <li>Temporary permissions with automatic expiration</li>
 *   <li>Group management</li>
 *   <li>Prefix/suffix and metadata access</li>
 *   <li>Offline player support</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the permission service
 * PermissionService perms = UnifiedAPI.getInstance().services()
 *     .get(PermissionService.class)
 *     .orElseThrow();
 *
 * // Check a permission
 * boolean hasAdmin = perms.hasPermission(player.getUniqueId(), "myplugin.admin");
 *
 * // Check with context
 * PermissionContext ctx = PermissionContext.builder()
 *     .world("world_nether")
 *     .build();
 * PermissionCheck check = perms.checkPermission(player.getUniqueId(), "myplugin.nether", ctx);
 *
 * // Add a temporary permission
 * perms.addPermission(player.getUniqueId(),
 *     Permission.builder("myplugin.vip")
 *         .expiresIn(Duration.ofDays(30))
 *         .build()
 * );
 *
 * // Get player's prefix
 * String prefix = perms.getMeta().getPrefix(player.getUniqueId())
 *     .map(Prefix::getValue)
 *     .orElse("");
 *
 * // Add player to group
 * perms.getGroups().addMember(player.getUniqueId(), "vip");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods returning {@link CompletableFuture} perform operations asynchronously.
 * Methods that return immediate results are thread-safe for read operations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PermissionChecker
 * @see GroupManager
 * @see MetaManager
 * @see TemporaryManager
 */
public interface PermissionService extends Service {

    /**
     * Checks if a player has a permission.
     *
     * <p>This is a convenience method equivalent to calling
     * {@link #checkPermission(UUID, String)} and checking if the result is TRUE.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node to check
     * @return true if the player has the permission
     * @throws NullPointerException if playerId or permission is null
     * @since 1.0.0
     */
    boolean hasPermission(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Checks if a player has a permission in a specific context.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node to check
     * @param context    the context for the check
     * @return true if the player has the permission in the given context
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    boolean hasPermission(@NotNull UUID playerId, @NotNull String permission, @NotNull PermissionContext context);

    /**
     * Performs a detailed permission check.
     *
     * <p>Unlike {@link #hasPermission}, this returns detailed information about
     * the check result including the actual TriState value and source.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node to check
     * @return the permission check result
     * @throws NullPointerException if playerId or permission is null
     * @since 1.0.0
     */
    @NotNull
    PermissionCheck checkPermission(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Performs a detailed permission check with context.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node to check
     * @param context    the context for the check
     * @return the permission check result
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    PermissionCheck checkPermission(@NotNull UUID playerId, @NotNull String permission, @NotNull PermissionContext context);

    /**
     * Gets all permissions for a player.
     *
     * @param playerId the player's UUID
     * @return a future that completes with all permissions
     * @throws NullPointerException if playerId is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Permission>> getPermissions(@NotNull UUID playerId);

    /**
     * Gets all permissions for a player in a specific context.
     *
     * @param playerId the player's UUID
     * @param context  the context to filter by
     * @return a future that completes with the permissions
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Permission>> getPermissions(@NotNull UUID playerId, @NotNull PermissionContext context);

    /**
     * Adds a permission to a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission to add
     * @return a future that completes when the operation is done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> addPermission(@NotNull UUID playerId, @NotNull Permission permission);

    /**
     * Adds a simple permission node to a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return a future that completes when the operation is done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> addPermission(@NotNull UUID playerId, @NotNull String permission) {
        return addPermission(playerId, Permission.of(permission));
    }

    /**
     * Adds a temporary permission to a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @param duration   how long the permission should last
     * @return a future that completes when the operation is done
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> addTemporaryPermission(
            @NotNull UUID playerId,
            @NotNull String permission,
            @NotNull Duration duration
    ) {
        return addPermission(playerId, Permission.builder(permission).expiresIn(duration).build());
    }

    /**
     * Removes a permission from a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node to remove
     * @return a future that completes with true if the permission was removed
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> removePermission(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Removes a permission with context from a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission to remove
     * @return a future that completes with true if the permission was removed
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> removePermission(@NotNull UUID playerId, @NotNull Permission permission);

    /**
     * Sets a permission value for a player (can grant or deny).
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @param value      true to grant, false to deny
     * @return a future that completes when the operation is done
     * @throws NullPointerException if playerId or permission is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> setPermission(@NotNull UUID playerId, @NotNull String permission, boolean value);

    /**
     * Clears all permissions for a player.
     *
     * @param playerId the player's UUID
     * @return a future that completes when all permissions are cleared
     * @throws NullPointerException if playerId is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> clearPermissions(@NotNull UUID playerId);

    /**
     * Returns the permission checker for detailed checks.
     *
     * @return the permission checker
     * @since 1.0.0
     */
    @NotNull
    PermissionChecker getChecker();

    /**
     * Returns the group manager for group operations.
     *
     * @return the group manager
     * @since 1.0.0
     */
    @NotNull
    GroupManager getGroups();

    /**
     * Returns the meta manager for prefix/suffix/metadata operations.
     *
     * @return the meta manager
     * @since 1.0.0
     */
    @NotNull
    MetaManager getMeta();

    /**
     * Returns the temporary permission manager.
     *
     * @return the temporary permission manager
     * @since 1.0.0
     */
    @NotNull
    TemporaryManager getTemporary();

    /**
     * Returns the underlying permission provider.
     *
     * @return the permission provider
     * @since 1.0.0
     */
    @NotNull
    PermissionProvider getProvider();

    /**
     * Returns the name of the active permission backend.
     *
     * @return the backend name (e.g., "LuckPerms", "Vault")
     * @since 1.0.0
     */
    @NotNull
    String getBackendName();

    /**
     * Checks if the specified backend is available.
     *
     * @param backend the backend name to check
     * @return true if the backend is available
     * @since 1.0.0
     */
    boolean isBackendAvailable(@NotNull String backend);

    /**
     * Checks if LuckPerms is the active backend.
     *
     * @return true if LuckPerms is being used
     * @since 1.0.0
     */
    default boolean isLuckPerms() {
        return "LuckPerms".equals(getBackendName());
    }

    /**
     * Checks if Vault is the active backend.
     *
     * @return true if Vault is being used
     * @since 1.0.0
     */
    default boolean isVault() {
        return "Vault".equals(getBackendName());
    }

    /**
     * Saves any pending changes to the backend.
     *
     * <p>Most operations are saved automatically, but this can be used to force
     * an immediate save.
     *
     * @return a future that completes when the save is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save();

    /**
     * Reloads permission data from the backend.
     *
     * @return a future that completes when the reload is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> reload();

    /**
     * Gets a player's primary group.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the primary group if found
     * @since 1.0.0
     */
    @NotNull
    default Optional<Group> getPrimaryGroup(@NotNull UUID playerId) {
        return getGroups().getPrimaryGroup(playerId);
    }

    /**
     * Gets a player's prefix.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the prefix if set
     * @since 1.0.0
     */
    @NotNull
    default Optional<Prefix> getPrefix(@NotNull UUID playerId) {
        return getMeta().getPrefix(playerId);
    }

    /**
     * Gets a player's suffix.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the suffix if set
     * @since 1.0.0
     */
    @NotNull
    default Optional<Suffix> getSuffix(@NotNull UUID playerId) {
        return getMeta().getSuffix(playerId);
    }

    /**
     * Gets a metadata value for a player.
     *
     * @param playerId the player's UUID
     * @param key      the metadata key
     * @return an Optional containing the meta value if set
     * @since 1.0.0
     */
    @NotNull
    default Optional<MetaValue> getMetaValue(@NotNull UUID playerId, @NotNull String key) {
        return getMeta().getValue(playerId, key);
    }

    /**
     * Gets all temporary permissions for a player.
     *
     * @param playerId the player's UUID
     * @return a future that completes with all temporary permissions
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Collection<TemporaryPermission>> getTemporaryPermissions(@NotNull UUID playerId) {
        return getTemporary().getPermissions(playerId);
    }

    /**
     * Gets all group memberships for a player.
     *
     * @param playerId the player's UUID
     * @return a collection of group memberships
     * @since 1.0.0
     */
    @NotNull
    default Collection<GroupMembership> getGroupMemberships(@NotNull UUID playerId) {
        return getGroups().getMemberships(playerId);
    }

    @Override
    default String getServiceName() {
        return "PermissionService";
    }
}
