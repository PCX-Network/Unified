/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.player;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for storing and retrieving player locale preferences.
 *
 * <p>Implementations of this interface handle the persistence of player
 * locale preferences across server restarts. Built-in implementations
 * include file-based and database-backed storage.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class FilePlayerLocaleStorage implements PlayerLocaleStorage {
 *
 *     private final Path dataFolder;
 *
 *     @Override
 *     public CompletableFuture<Optional<PlayerLocale>> load(UUID playerId) {
 *         return CompletableFuture.supplyAsync(() -> {
 *             Path file = dataFolder.resolve(playerId + ".json");
 *             // Load from file...
 *         });
 *     }
 *
 *     @Override
 *     public CompletableFuture<Void> save(PlayerLocale playerLocale) {
 *         return CompletableFuture.runAsync(() -> {
 *             Path file = dataFolder.resolve(playerLocale.getPlayerId() + ".json");
 *             // Save to file...
 *         });
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerLocale
 */
public interface PlayerLocaleStorage {

    /**
     * Loads a player's locale preferences.
     *
     * @param playerId the player's UUID
     * @return a future completing with the player locale, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<PlayerLocale>> load(@NotNull UUID playerId);

    /**
     * Saves a player's locale preferences.
     *
     * @param playerLocale the player locale to save
     * @return a future completing when the save is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save(@NotNull PlayerLocale playerLocale);

    /**
     * Deletes a player's locale preferences.
     *
     * @param playerId the player's UUID
     * @return a future completing with true if deleted, false if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> delete(@NotNull UUID playerId);

    /**
     * Checks if a player has stored locale preferences.
     *
     * @param playerId the player's UUID
     * @return a future completing with true if preferences exist
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Boolean> exists(@NotNull UUID playerId) {
        return load(playerId).thenApply(Optional::isPresent);
    }

    /**
     * Performs any cleanup operations.
     *
     * <p>Called when the i18n service is shutting down.
     *
     * @return a future completing when cleanup is done
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }
}
