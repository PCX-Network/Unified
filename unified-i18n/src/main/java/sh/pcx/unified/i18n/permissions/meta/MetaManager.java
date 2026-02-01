/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.meta;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Manager for player metadata (prefix, suffix, custom meta values).
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface MetaManager {

    /**
     * Gets the prefix for a player.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the prefix if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Prefix> getPrefix(@NotNull UUID playerId);

    /**
     * Gets the suffix for a player.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the suffix if set
     * @since 1.0.0
     */
    @NotNull
    Optional<Suffix> getSuffix(@NotNull UUID playerId);

    /**
     * Gets a metadata value for a player.
     *
     * @param playerId the player's UUID
     * @param key      the metadata key
     * @return an Optional containing the value if set
     * @since 1.0.0
     */
    @NotNull
    Optional<MetaValue> getValue(@NotNull UUID playerId, @NotNull String key);
}
