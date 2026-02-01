/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Service for discovering and tracking servers in the network.
 *
 * <p>The ServerList provides methods for querying server information,
 * finding servers by various criteria, and locating players.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ServerList servers = messaging.getServerList();
 *
 * // Get all online servers
 * Collection<ServerInfo> online = servers.getOnlineServers();
 *
 * // Find servers by type
 * Collection<ServerInfo> lobbies = servers.findByType("lobby");
 *
 * // Find server with least players
 * servers.findLeastCrowded("lobby").ifPresent(server -> {
 *     log.info("Best lobby: " + server.id() + " with " + server.playerCount() + " players");
 * });
 *
 * // Locate a player
 * servers.findPlayer(playerId).ifPresent(server -> {
 *     log.info("Player is on server: " + server.id());
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ServerInfo
 */
public interface ServerList {

    /**
     * Returns all known servers.
     *
     * @return collection of all servers
     * @since 1.0.0
     */
    @NotNull
    Collection<ServerInfo> getAllServers();

    /**
     * Returns all online servers.
     *
     * @return collection of online servers
     * @since 1.0.0
     */
    @NotNull
    Collection<ServerInfo> getOnlineServers();

    /**
     * Gets a server by ID.
     *
     * @param serverId the server ID
     * @return the server info, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<ServerInfo> getServer(@NotNull String serverId);

    /**
     * Finds servers by type.
     *
     * @param type the server type
     * @return matching servers
     * @since 1.0.0
     */
    @NotNull
    Collection<ServerInfo> findByType(@NotNull String type);

    /**
     * Finds servers by group.
     *
     * @param group the server group
     * @return matching servers
     * @since 1.0.0
     */
    @NotNull
    Collection<ServerInfo> findByGroup(@NotNull String group);

    /**
     * Finds servers with a specific tag.
     *
     * @param tag the tag to search for
     * @return matching servers
     * @since 1.0.0
     */
    @NotNull
    Collection<ServerInfo> findByTag(@NotNull String tag);

    /**
     * Finds servers matching a predicate.
     *
     * @param predicate the filter predicate
     * @return matching servers
     * @since 1.0.0
     */
    @NotNull
    Collection<ServerInfo> find(@NotNull Predicate<ServerInfo> predicate);

    /**
     * Finds the server with the least players in a group.
     *
     * @param group the server group
     * @return the least crowded server, or empty if no servers in group
     * @since 1.0.0
     */
    @NotNull
    Optional<ServerInfo> findLeastCrowded(@NotNull String group);

    /**
     * Finds the server where a player is located.
     *
     * @param playerId the player's UUID
     * @return the server, or empty if player not found
     * @since 1.0.0
     */
    @NotNull
    Optional<ServerInfo> findPlayer(@NotNull UUID playerId);

    /**
     * Refreshes the server list from the network.
     *
     * @return a future that completes when refresh is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> refresh();

    /**
     * Returns the total player count across all servers.
     *
     * @return total player count
     * @since 1.0.0
     */
    int getTotalPlayerCount();

    /**
     * Returns the number of known servers.
     *
     * @return server count
     * @since 1.0.0
     */
    int getServerCount();

    /**
     * Returns the number of online servers.
     *
     * @return online server count
     * @since 1.0.0
     */
    int getOnlineServerCount();
}
