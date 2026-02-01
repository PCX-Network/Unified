/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A condition that checks if a player has a specific permission.
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Check for a specific permission
 * Condition isAdmin = Condition.permission("myplugin.admin");
 *
 * // Check for VIP status
 * Condition isVip = Condition.permission("group.vip");
 *
 * // Negate a permission check
 * Condition notBanned = Condition.not(Condition.permission("banned"));
 *
 * // Combine with other conditions
 * Condition canUseFeature = Condition.all(
 *     Condition.permission("feature.use"),
 *     Condition.world("survival")
 * );
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public interface PermissionCondition extends Condition {

    /**
     * Returns the permission node being checked.
     *
     * @return the permission node
     * @since 1.0.0
     */
    @NotNull
    String getPermission();

    @Override
    @NotNull
    default String getName() {
        return "permission:" + getPermission();
    }

    @Override
    @NotNull
    default String getType() {
        return "permission";
    }

    @Override
    @NotNull
    default String getDescription() {
        return "Requires permission: " + getPermission();
    }

    @Override
    @NotNull
    default ConditionResult evaluate(@NotNull ConditionContext context) {
        return context.getPlayer()
                .map(player -> {
                    if (player.hasPermission(getPermission())) {
                        return ConditionResult.success("Has permission: " + getPermission());
                    }
                    return ConditionResult.failure("Missing permission: " + getPermission());
                })
                .orElse(ConditionResult.failure("No player in context"));
    }

    /**
     * Creates a permission condition.
     *
     * @param permission the permission node to check
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static PermissionCondition of(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission cannot be null");
        return () -> permission;
    }
}
