/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Fluent builder for safely unloading worlds.
 *
 * <p>WorldUnloader provides options for handling players in the world,
 * saving data, and cleanup actions before the world is unloaded.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Unload world safely
 * worlds.unload("my_world")
 *     .save(true)
 *     .teleportPlayersTo(lobbySpawn)
 *     .onComplete(() -> log.info("World unloaded"))
 *     .unload();
 *
 * // Unload with async completion
 * worlds.unload("arena-1")
 *     .save(false)
 *     .kickPlayers("Arena closing!")
 *     .unloadAsync()
 *     .thenRun(() -> {
 *         // World is now unloaded
 *     });
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 */
public interface WorldUnloader {

    /**
     * Sets whether to save the world before unloading.
     *
     * @param save true to save the world
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader save(boolean save);

    /**
     * Sets a location to teleport all players to before unloading.
     *
     * @param location the destination location
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader teleportPlayersTo(@NotNull UnifiedLocation location);

    /**
     * Sets a fallback world to teleport players to.
     *
     * <p>Players will be teleported to the spawn location of the fallback world.
     *
     * @param worldName the fallback world name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader teleportPlayersTo(@NotNull String worldName);

    /**
     * Kicks all players from the world with a message.
     *
     * @param message the kick message
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader kickPlayers(@NotNull String message);

    /**
     * Sets a callback to run for each player before they are moved.
     *
     * @param handler the player handler (receives player UUID)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader forEachPlayer(@NotNull Consumer<UUID> handler);

    /**
     * Sets a callback to run when unloading is complete.
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader onComplete(@NotNull Runnable callback);

    /**
     * Sets a callback to run if unloading fails.
     *
     * @param callback the failure callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader onFailure(@NotNull Consumer<Throwable> callback);

    /**
     * Forces unload even if chunks are busy.
     *
     * <p>Warning: This may cause data loss or corruption. Use with caution.
     *
     * @param force true to force unload
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader force(boolean force);

    /**
     * Unloads the world synchronously.
     *
     * @return true if the world was unloaded successfully
     * @since 1.0.0
     */
    boolean unload();

    /**
     * Unloads the world asynchronously.
     *
     * @return a future that completes with true if successful
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> unloadAsync();

    /**
     * Gets the world name being unloaded.
     *
     * @return the world name
     * @since 1.0.0
     */
    @NotNull
    String getWorldName();
}
