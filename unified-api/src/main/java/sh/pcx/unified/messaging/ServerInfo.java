/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.messaging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Information about a server in the network.
 *
 * <p>ServerInfo provides details about servers discovered through the
 * messaging system. This includes server metadata, player counts, and
 * connectivity status.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MessagingService messaging = ...;
 *
 * // Get info about current server
 * ServerInfo current = messaging.serverInfo();
 * log.info("Running on: " + current.name() + " with " + current.playerCount() + " players");
 *
 * // Find all game servers
 * Collection<ServerInfo> gameServers = messaging.servers().stream()
 *     .filter(s -> s.hasTag("game"))
 *     .toList();
 *
 * // Get specific server
 * messaging.server("lobby-1").ifPresent(lobby -> {
 *     if (lobby.isOnline()) {
 *         messaging.transfers().send(player, "lobby-1");
 *     }
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessagingService#serverInfo()
 * @see MessagingService#servers()
 */
public interface ServerInfo {

    /**
     * Returns the unique server identifier.
     *
     * <p>This is the ID used for message routing and player transfers.
     *
     * @return the server ID
     * @since 1.0.0
     */
    @NotNull
    String id();

    /**
     * Returns the display name of the server.
     *
     * <p>This is typically more human-readable than the ID.
     *
     * @return the server name
     * @since 1.0.0
     */
    @NotNull
    String name();

    /**
     * Returns the server address.
     *
     * @return the address (hostname:port), or empty if not available
     * @since 1.0.0
     */
    @NotNull
    Optional<String> address();

    /**
     * Checks if this is the current server.
     *
     * @return true if this represents the local server
     * @since 1.0.0
     */
    boolean isLocal();

    /**
     * Checks if the server is online and reachable.
     *
     * @return true if online
     * @since 1.0.0
     */
    boolean isOnline();

    /**
     * Returns the current player count.
     *
     * @return number of players
     * @since 1.0.0
     */
    int playerCount();

    /**
     * Returns the maximum player capacity.
     *
     * @return max players, or -1 if unlimited
     * @since 1.0.0
     */
    int maxPlayers();

    /**
     * Returns the UUIDs of players on this server.
     *
     * <p>This may not be available for all transports.
     *
     * @return player UUIDs
     * @since 1.0.0
     */
    @NotNull
    Collection<UUID> players();

    /**
     * Checks if a player is on this server.
     *
     * @param playerId the player's UUID
     * @return true if the player is on this server
     * @since 1.0.0
     */
    default boolean hasPlayer(@NotNull UUID playerId) {
        return players().contains(playerId);
    }

    /**
     * Returns the server type/category.
     *
     * <p>Examples: "lobby", "game", "hub", "survival"
     *
     * @return the server type
     * @since 1.0.0
     */
    @NotNull
    Optional<String> type();

    /**
     * Returns the server group.
     *
     * <p>Used for grouping similar servers (e.g., "lobby", "bedwars").
     *
     * @return the server group
     * @since 1.0.0
     */
    @NotNull
    Optional<String> group();

    /**
     * Returns tags associated with this server.
     *
     * @return the tags
     * @since 1.0.0
     */
    @NotNull
    Collection<String> tags();

    /**
     * Checks if the server has a specific tag.
     *
     * @param tag the tag to check
     * @return true if tagged
     * @since 1.0.0
     */
    default boolean hasTag(@NotNull String tag) {
        return tags().contains(tag);
    }

    /**
     * Returns custom metadata for this server.
     *
     * @return the metadata map
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> metadata();

    /**
     * Gets a metadata value.
     *
     * @param key the key
     * @param <T> expected type
     * @return the value, or null if not present
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <T> T metadata(@NotNull String key) {
        return (T) metadata().get(key);
    }

    /**
     * Gets a metadata value with a default.
     *
     * @param key          the key
     * @param defaultValue default value
     * @param <T>          expected type
     * @return the value or default
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default <T> T metadata(@NotNull String key, @NotNull T defaultValue) {
        Object value = metadata().get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Returns the server's MOTD.
     *
     * @return the MOTD
     * @since 1.0.0
     */
    @NotNull
    Optional<String> motd();

    /**
     * Returns the Minecraft version.
     *
     * @return the version string
     * @since 1.0.0
     */
    @NotNull
    Optional<String> version();

    /**
     * Returns when this info was last updated.
     *
     * @return the last update time
     * @since 1.0.0
     */
    @NotNull
    Instant lastUpdated();

    /**
     * Returns the server's uptime since last restart.
     *
     * @return uptime in milliseconds
     * @since 1.0.0
     */
    long uptime();

    /**
     * Returns the average TPS (ticks per second).
     *
     * @return TPS (typically 20.0 for healthy servers)
     * @since 1.0.0
     */
    double tps();

    /**
     * Returns the memory usage as a percentage.
     *
     * @return memory usage (0.0 to 1.0)
     * @since 1.0.0
     */
    double memoryUsage();

    /**
     * Checks if the server is accepting new players.
     *
     * <p>A server might be online but not accepting players if it's
     * full, in maintenance mode, or shutting down.
     *
     * @return true if accepting players
     * @since 1.0.0
     */
    default boolean isAcceptingPlayers() {
        return isOnline() && (maxPlayers() < 0 || playerCount() < maxPlayers());
    }

    /**
     * Returns the ping/latency to this server in milliseconds.
     *
     * @return ping in ms, or -1 if unknown
     * @since 1.0.0
     */
    long ping();
}
