/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.temporary;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for temporary permissions.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface TemporaryManager {

    /**
     * Gets all temporary permissions for a player.
     *
     * @param playerId the player's UUID
     * @return a future that completes with the temporary permissions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<TemporaryPermission>> getPermissions(@NotNull UUID playerId);
}
