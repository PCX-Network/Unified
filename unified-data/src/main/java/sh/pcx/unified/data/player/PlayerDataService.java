/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Central service for managing player data across the server network.
 *
 * <p>The PlayerDataService provides a unified API for loading, caching, and
 * persisting player profiles. It handles cross-server synchronization on
 * networks and ensures thread-safe access to player data.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @Inject
 * private PlayerDataService playerData;
 *
 * public void example(Player player) {
 *     // Get profile for online player (cached, fast)
 *     PlayerProfile profile = playerData.getProfile(player);
 *     int kills = profile.getData(MyPlugin.KILLS);
 *     profile.setData(MyPlugin.KILLS, kills + 1);
 * }
 * }</pre>
 *
 * <h2>Async Operations</h2>
 * <pre>{@code
 * // Load profile asynchronously (for offline players or preloading)
 * playerData.getProfileAsync(uuid)
 *     .thenAccept(profile -> {
 *         // Process profile on async thread
 *         int totalKills = profile.getData(MyPlugin.KILLS);
 *         // ...
 *     })
 *     .exceptionally(error -> {
 *         logger.error("Failed to load profile", error);
 *         return null;
 *     });
 *
 * // Preload profiles before they're needed
 * playerData.preload(List.of(uuid1, uuid2, uuid3));
 * }</pre>
 *
 * <h2>Querying Data</h2>
 * <pre>{@code
 * // Find top players by kills
 * playerData.query()
 *     .where(MyPlugin.VIP, true)
 *     .orderBy(MyPlugin.KILLS, Order.DESC)
 *     .limit(10)
 *     .execute()
 *     .thenAccept(topPlayers -> {
 *         // Display leaderboard
 *     });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Async operations return {@link CompletableFuture}
 * and should be used for database operations to avoid blocking the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerProfile
 * @see DataKey
 */
public interface PlayerDataService extends Service {

    /**
     * Retrieves the profile for an online player.
     *
     * <p>If the profile is cached, returns immediately. Otherwise, loads from
     * the database synchronously. For async loading, use {@link #getProfileAsync(UUID)}.
     *
     * <p>This method is guaranteed to return a non-null profile for online players.
     * The profile is automatically loaded when a player joins and cached until
     * they leave.
     *
     * @param playerId the player's unique identifier
     * @return the player's profile, never null for online players
     * @throws IllegalArgumentException if playerId is null
     * @throws IllegalStateException    if player is not online and not cached
     * @see #getProfileAsync(UUID)
     * @see #getCachedProfile(UUID)
     * @since 1.0.0
     */
    @NotNull
    PlayerProfile getProfile(@NotNull UUID playerId);

    /**
     * Asynchronously retrieves a player profile by UUID.
     *
     * <p>This method is safe to call for both online and offline players.
     * It will check the cache first and load from the database if not found.
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * playerData.getProfileAsync(uuid)
     *     .thenAccept(profile -> {
     *         // Process profile (may be on async thread)
     *         int kills = profile.getData(MyPlugin.KILLS);
     *     })
     *     .exceptionally(error -> {
     *         logger.error("Failed to load profile for " + uuid, error);
     *         return null;
     *     });
     * }</pre>
     *
     * @param playerId the player's unique ID
     * @return a future containing the profile, or completing exceptionally if not found
     * @throws IllegalArgumentException if playerId is null
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<PlayerProfile> getProfileAsync(@NotNull UUID playerId);

    /**
     * Gets a cached profile without loading from database.
     *
     * <p>This method only checks the in-memory cache and returns immediately.
     * It will not load from the database if the profile is not cached.
     *
     * <p>Use this when you want to check if a profile is already loaded without
     * triggering a database query.
     *
     * @param playerId the player's unique ID
     * @return an Optional containing the cached profile, or empty if not cached
     * @since 1.0.0
     */
    @NotNull
    Optional<PlayerProfile> getCachedProfile(@NotNull UUID playerId);

    /**
     * Preloads profiles into the cache.
     *
     * <p>This is useful for warming the cache before players join or when you
     * know you'll need multiple profiles. The profiles are loaded in the background.
     *
     * @param playerIds the UUIDs to preload
     * @return a future that completes when all profiles are loaded
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> preload(@NotNull Collection<UUID> playerIds);

    /**
     * Preloads a single profile into the cache.
     *
     * @param playerId the UUID to preload
     * @return a future that completes when the profile is loaded
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> preload(@NotNull UUID playerId) {
        return preload(List.of(playerId));
    }

    /**
     * Returns all currently cached profiles.
     *
     * <p>This typically includes all online players and recently accessed
     * offline player profiles.
     *
     * @return a future containing all cached profiles
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<PlayerProfile>> getAllCachedProfiles();

    /**
     * Gets the profile for an offline player.
     *
     * <p>This always loads from the database, even if a cached version exists.
     * Use this when you need guaranteed fresh data.
     *
     * @param playerId the player's unique ID
     * @return a future containing the profile, or completing exceptionally if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<PlayerProfile> getOfflineProfile(@NotNull UUID playerId);

    /**
     * Creates a query builder for searching player profiles.
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * playerData.query()
     *     .where(MyPlugin.VIP, true)
     *     .where(MyPlugin.KILLS, Comparator.GREATER_THAN, 100)
     *     .orderBy(MyPlugin.KILLS, Order.DESC)
     *     .limit(10)
     *     .execute()
     *     .thenAccept(results -> {
     *         // Process top VIP killers
     *     });
     * }</pre>
     *
     * @return a new query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery query();

    /**
     * Saves all dirty profiles to the database.
     *
     * <p>This is called automatically at configured intervals and on server
     * shutdown, but can be triggered manually if needed.
     *
     * @return a future that completes when all profiles are saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveAll();

    /**
     * Saves a specific player's profile.
     *
     * @param playerId the player's UUID
     * @return a future that completes when the profile is saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save(@NotNull UUID playerId);

    /**
     * Invalidates the cache for a specific player.
     *
     * <p>The next access will reload from the database.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void invalidateCache(@NotNull UUID playerId);

    /**
     * Clears all cached profiles.
     *
     * <p>Use with caution - this will force all profiles to be reloaded
     * from the database on next access.
     *
     * @since 1.0.0
     */
    void clearCache();

    /**
     * Returns the data key registry.
     *
     * <p>The registry tracks all registered data keys and can be used for
     * discovery and validation.
     *
     * @return the data key registry
     * @since 1.0.0
     */
    @NotNull
    DataKeyRegistry getKeyRegistry();

    /**
     * Returns the session manager for tracking player sessions.
     *
     * @return the session manager
     * @since 1.0.0
     */
    @NotNull
    SessionManager getSessionManager();

    /**
     * Returns the cross-server sync service.
     *
     * @return the sync service, or empty if running in standalone mode
     * @since 1.0.0
     */
    @NotNull
    Optional<CrossServerSync> getCrossServerSync();

    /**
     * Returns the GDPR compliance service.
     *
     * @return the GDPR service
     * @since 1.0.0
     */
    @NotNull
    GDPRService getGDPRService();

    /**
     * Returns the lock manager for distributed locking.
     *
     * @return the lock manager
     * @since 1.0.0
     */
    @NotNull
    LockManager getLockManager();

    /**
     * Checks if a player has ever joined the server.
     *
     * @param playerId the player's UUID
     * @return a future containing true if the player has joined before
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> hasPlayed(@NotNull UUID playerId);

    /**
     * Gets the UUID for a player name.
     *
     * <p>This searches the name history for an exact match (case-insensitive).
     *
     * @param name the player name
     * @return a future containing the UUID if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<UUID>> getUUID(@NotNull String name);

    /**
     * Gets the current name for a player UUID.
     *
     * @param playerId the player's UUID
     * @return a future containing the name if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> getName(@NotNull UUID playerId);
}
