/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.check;

import sh.pcx.unified.i18n.permissions.core.Permission;
import sh.pcx.unified.i18n.permissions.core.TriState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Interface for performing detailed permission checks.
 *
 * <p>PermissionChecker provides more control over permission checking than the
 * basic methods in {@link sh.pcx.unified.i18n.permissions.core.PermissionService}.
 * It supports:
 * <ul>
 *   <li>Bulk permission checks</li>
 *   <li>Wildcard expansion</li>
 *   <li>Context-aware checking</li>
 *   <li>Permission inheritance traversal</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PermissionChecker checker = permService.getChecker();
 *
 * // Check a single permission
 * PermissionCheck result = checker.check(playerId, "myplugin.admin");
 *
 * // Check with context
 * PermissionContext ctx = PermissionContext.world("world_nether");
 * PermissionCheck contextResult = checker.check(playerId, "myplugin.nether", ctx);
 *
 * // Check multiple permissions
 * Collection<PermissionCheck> results = checker.checkAll(playerId,
 *     List.of("myplugin.admin", "myplugin.mod", "myplugin.user"));
 *
 * // Check if any permission matches
 * boolean hasAny = checker.hasAny(playerId,
 *     List.of("myplugin.admin", "myplugin.mod"));
 *
 * // Check if all permissions match
 * boolean hasAll = checker.hasAll(playerId,
 *     List.of("myplugin.feature.use", "myplugin.feature.configure"));
 *
 * // Get all matching permissions for a pattern
 * Collection<Permission> matching = checker.getMatching(playerId, "myplugin.*").join();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PermissionCheck
 * @see PermissionContext
 */
public interface PermissionChecker {

    /**
     * Checks a permission for a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return the check result
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    PermissionCheck check(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Checks a permission for a player with context.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @param context    the check context
     * @return the check result
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    PermissionCheck check(@NotNull UUID playerId, @NotNull String permission, @NotNull PermissionContext context);

    /**
     * Checks multiple permissions for a player.
     *
     * @param playerId    the player's UUID
     * @param permissions the permission nodes to check
     * @return a collection of check results
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    Collection<PermissionCheck> checkAll(@NotNull UUID playerId, @NotNull Collection<String> permissions);

    /**
     * Checks multiple permissions for a player with context.
     *
     * @param playerId    the player's UUID
     * @param permissions the permission nodes to check
     * @param context     the check context
     * @return a collection of check results
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    Collection<PermissionCheck> checkAll(
            @NotNull UUID playerId,
            @NotNull Collection<String> permissions,
            @NotNull PermissionContext context
    );

    /**
     * Checks if a player has a permission (simple boolean check).
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return true if the permission is granted
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean has(@NotNull UUID playerId, @NotNull String permission) {
        return check(playerId, permission).isAllowed();
    }

    /**
     * Checks if a player has a permission with context.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @param context    the check context
     * @return true if the permission is granted
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean has(@NotNull UUID playerId, @NotNull String permission, @NotNull PermissionContext context) {
        return check(playerId, permission, context).isAllowed();
    }

    /**
     * Checks if a player has any of the specified permissions.
     *
     * @param playerId    the player's UUID
     * @param permissions the permission nodes to check
     * @return true if any permission is granted
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean hasAny(@NotNull UUID playerId, @NotNull Collection<String> permissions) {
        return checkAll(playerId, permissions).stream()
                .anyMatch(PermissionCheck::isAllowed);
    }

    /**
     * Checks if a player has any of the specified permissions with context.
     *
     * @param playerId    the player's UUID
     * @param permissions the permission nodes to check
     * @param context     the check context
     * @return true if any permission is granted
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean hasAny(
            @NotNull UUID playerId,
            @NotNull Collection<String> permissions,
            @NotNull PermissionContext context
    ) {
        return checkAll(playerId, permissions, context).stream()
                .anyMatch(PermissionCheck::isAllowed);
    }

    /**
     * Checks if a player has all of the specified permissions.
     *
     * @param playerId    the player's UUID
     * @param permissions the permission nodes to check
     * @return true if all permissions are granted
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean hasAll(@NotNull UUID playerId, @NotNull Collection<String> permissions) {
        return checkAll(playerId, permissions).stream()
                .allMatch(PermissionCheck::isAllowed);
    }

    /**
     * Checks if a player has all of the specified permissions with context.
     *
     * @param playerId    the player's UUID
     * @param permissions the permission nodes to check
     * @param context     the check context
     * @return true if all permissions are granted
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean hasAll(
            @NotNull UUID playerId,
            @NotNull Collection<String> permissions,
            @NotNull PermissionContext context
    ) {
        return checkAll(playerId, permissions, context).stream()
                .allMatch(PermissionCheck::isAllowed);
    }

    /**
     * Gets the raw value of a permission (without inheritance).
     *
     * <p>This checks only direct permissions on the player, not permissions
     * inherited from groups.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return the direct permission value
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    TriState getDirectValue(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Gets the raw value of a permission with context.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @param context    the check context
     * @return the direct permission value
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    TriState getDirectValue(@NotNull UUID playerId, @NotNull String permission, @NotNull PermissionContext context);

    /**
     * Gets all permissions that match a pattern.
     *
     * <p>The pattern can include wildcards (*) to match multiple permissions.
     *
     * @param playerId the player's UUID
     * @param pattern  the permission pattern (e.g., "myplugin.*")
     * @return a future that completes with matching permissions
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Permission>> getMatching(@NotNull UUID playerId, @NotNull String pattern);

    /**
     * Gets all permissions that match a predicate.
     *
     * @param playerId  the player's UUID
     * @param predicate the filter predicate
     * @return a future that completes with matching permissions
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Permission>> getMatching(
            @NotNull UUID playerId,
            @NotNull Predicate<Permission> predicate
    );

    /**
     * Expands a wildcard permission node to all matching nodes.
     *
     * <p>For example, "myplugin.*" might expand to:
     * ["myplugin.admin", "myplugin.user", "myplugin.mod"]
     *
     * @param playerId       the player's UUID
     * @param wildcardNode   the wildcard permission node
     * @return a future that completes with expanded nodes
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<String>> expandWildcard(@NotNull UUID playerId, @NotNull String wildcardNode);

    /**
     * Gets the inheritance chain for a permission.
     *
     * <p>Returns the sequence of permission sources that were checked to resolve
     * the final value, from most specific to least specific.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return a collection of check results showing the inheritance chain
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    Collection<PermissionCheck> getInheritanceChain(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Calculates the effective permission after all inheritance and context.
     *
     * <p>This is similar to {@link #check} but returns a Permission object
     * representing the final effective state.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return the effective permission
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    Permission getEffective(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Calculates the effective permission with context.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @param context    the check context
     * @return the effective permission
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    Permission getEffective(@NotNull UUID playerId, @NotNull String permission, @NotNull PermissionContext context);

    /**
     * Checks if a permission is set (not undefined) for a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return true if the permission has any explicit value
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean isSet(@NotNull UUID playerId, @NotNull String permission) {
        return check(playerId, permission).getValue().isDefined();
    }

    /**
     * Checks if a permission is explicitly denied for a player.
     *
     * @param playerId   the player's UUID
     * @param permission the permission node
     * @return true if the permission is explicitly denied
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    default boolean isDenied(@NotNull UUID playerId, @NotNull String permission) {
        return check(playerId, permission).isDenied();
    }
}
