/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.player;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface representing a player who may be offline.
 *
 * <p>This interface provides access to persistent player data that is available
 * even when the player is not currently connected to the server. For online
 * players, use {@link UnifiedPlayer} which extends this interface with
 * additional online-only functionality.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get an offline player
 * OfflineUnifiedPlayer offlinePlayer = server.getOfflinePlayer(uuid);
 *
 * // Check if they've played before
 * if (offlinePlayer.hasPlayedBefore()) {
 *     // Get their last known name
 *     String name = offlinePlayer.getName().orElse("Unknown");
 *
 *     // Get their last seen time
 *     Instant lastSeen = offlinePlayer.getLastSeen().orElse(Instant.EPOCH);
 *
 *     // Check if they're banned
 *     if (offlinePlayer.isBanned()) {
 *         // Handle banned player
 *     }
 * }
 *
 * // Get their last location (async operation)
 * offlinePlayer.getLastLocation().thenAccept(location -> {
 *     location.ifPresent(loc -> {
 *         // Use the location
 *     });
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods on this interface are thread-safe. Methods that perform
 * I/O operations (like retrieving data from storage) return CompletableFuture.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlayer
 */
public interface OfflineUnifiedPlayer {

    /**
     * Returns the player's unique identifier.
     *
     * <p>This UUID is consistent across name changes and server restarts.
     *
     * @return the player's UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getUniqueId();

    /**
     * Returns the player's current or last known name.
     *
     * <p>Player names can change. For persistent identification,
     * use {@link #getUniqueId()} instead.
     *
     * @return an Optional containing the player's name if known
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getName();

    /**
     * Checks if the player is currently online.
     *
     * @return true if the player is currently connected
     * @since 1.0.0
     */
    boolean isOnline();

    /**
     * Returns the player as an online player if they are connected.
     *
     * @return an Optional containing the UnifiedPlayer if online
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedPlayer> getPlayer();

    /**
     * Checks if this player has played on this server before.
     *
     * @return true if the player has joined at least once
     * @since 1.0.0
     */
    boolean hasPlayedBefore();

    /**
     * Returns the first time this player joined the server.
     *
     * @return an Optional containing the first join time if they've played before
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getFirstPlayed();

    /**
     * Returns the last time this player was seen on the server.
     *
     * <p>For online players, this returns the current time.
     *
     * @return an Optional containing the last seen time if they've played before
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> getLastSeen();

    /**
     * Returns the last known location of the player.
     *
     * <p>This operation may require loading data from storage,
     * hence it returns a CompletableFuture.
     *
     * @return a future that resolves to the last known location if available
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<UnifiedLocation>> getLastLocation();

    /**
     * Returns the bed spawn location (respawn point) of the player.
     *
     * @return a future that resolves to the bed spawn location if set
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<UnifiedLocation>> getBedSpawnLocation();

    /**
     * Checks if the player is banned.
     *
     * @return true if the player is banned
     * @since 1.0.0
     */
    boolean isBanned();

    /**
     * Sets the player's banned status.
     *
     * @param banned true to ban the player
     * @since 1.0.0
     */
    void setBanned(boolean banned);

    /**
     * Bans the player with a reason and optional expiration.
     *
     * @param reason     the ban reason shown to the player
     * @param expiration the expiration time, or null for permanent ban
     * @param source     the source of the ban (e.g., admin name), or null
     * @since 1.0.0
     */
    void ban(@NotNull String reason, @Nullable Instant expiration, @Nullable String source);

    /**
     * Pardons (unbans) the player.
     *
     * @since 1.0.0
     */
    void pardon();

    /**
     * Checks if the player is whitelisted.
     *
     * @return true if the player is on the whitelist
     * @since 1.0.0
     */
    boolean isWhitelisted();

    /**
     * Sets the player's whitelist status.
     *
     * @param whitelisted true to add to whitelist
     * @since 1.0.0
     */
    void setWhitelisted(boolean whitelisted);

    /**
     * Checks if the player is an operator.
     *
     * @return true if the player is an operator
     * @since 1.0.0
     */
    boolean isOp();

    /**
     * Sets the player's operator status.
     *
     * @param op true to make the player an operator
     * @since 1.0.0
     */
    void setOp(boolean op);

    /**
     * Returns a map of statistics for this player.
     *
     * <p>Statistics include things like time played, distance walked,
     * mobs killed, etc.
     *
     * @param statistic the statistic type to retrieve
     * @return a future that resolves to the statistic value
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> getStatistic(@NotNull String statistic);

    /**
     * Increments a statistic for this player.
     *
     * @param statistic the statistic type to increment
     * @param amount    the amount to add
     * @return a future that completes when the operation is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> incrementStatistic(@NotNull String statistic, int amount);

    /**
     * Returns the underlying platform-specific offline player object.
     *
     * <p>Use this method when you need to access platform-specific functionality
     * not available through the unified API.
     *
     * @param <T> the expected platform player type
     * @return the platform-specific offline player object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();
}
