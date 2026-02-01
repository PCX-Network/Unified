/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.util;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Information about a server in the network.
 *
 * <p>This interface extends the base API ServerInfo with additional
 * network-specific functionality.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.messaging.ServerInfo
 */
public interface ServerInfo extends sh.pcx.unified.messaging.ServerInfo {

    /**
     * Returns the unique server identifier.
     *
     * @return the server ID
     * @since 1.0.0
     */
    @Override
    @NotNull
    String id();

    /**
     * Returns the display name of the server.
     *
     * @return the server name
     * @since 1.0.0
     */
    @Override
    @NotNull
    String name();

    /**
     * Returns the server address.
     *
     * @return the address, or empty if not available
     * @since 1.0.0
     */
    @Override
    @NotNull
    Optional<String> address();

    /**
     * Checks if this is the current server.
     *
     * @return true if this represents the local server
     * @since 1.0.0
     */
    @Override
    boolean isLocal();

    /**
     * Checks if the server is online and reachable.
     *
     * @return true if online
     * @since 1.0.0
     */
    @Override
    boolean isOnline();

    /**
     * Returns the current player count.
     *
     * @return number of players
     * @since 1.0.0
     */
    @Override
    int playerCount();

    /**
     * Returns the maximum player capacity.
     *
     * @return max players, or -1 if unlimited
     * @since 1.0.0
     */
    @Override
    int maxPlayers();

    /**
     * Returns the UUIDs of players on this server.
     *
     * @return player UUIDs
     * @since 1.0.0
     */
    @Override
    @NotNull
    Collection<UUID> players();

    /**
     * Returns the server type/category.
     *
     * @return the server type
     * @since 1.0.0
     */
    @Override
    @NotNull
    Optional<String> type();

    /**
     * Returns the server group.
     *
     * @return the server group
     * @since 1.0.0
     */
    @Override
    @NotNull
    Optional<String> group();

    /**
     * Returns tags associated with this server.
     *
     * @return the tags
     * @since 1.0.0
     */
    @Override
    @NotNull
    Collection<String> tags();

    /**
     * Returns custom metadata for this server.
     *
     * @return the metadata map
     * @since 1.0.0
     */
    @Override
    @NotNull
    Map<String, Object> metadata();

    /**
     * Returns the server's MOTD.
     *
     * @return the MOTD
     * @since 1.0.0
     */
    @Override
    @NotNull
    Optional<String> motd();

    /**
     * Returns the Minecraft version.
     *
     * @return the version string
     * @since 1.0.0
     */
    @Override
    @NotNull
    Optional<String> version();

    /**
     * Returns when this info was last updated.
     *
     * @return the last update time
     * @since 1.0.0
     */
    @Override
    @NotNull
    Instant lastUpdated();

    /**
     * Returns the server's uptime since last restart.
     *
     * @return uptime in milliseconds
     * @since 1.0.0
     */
    @Override
    long uptime();

    /**
     * Returns the average TPS.
     *
     * @return TPS
     * @since 1.0.0
     */
    @Override
    double tps();

    /**
     * Returns the memory usage as a percentage.
     *
     * @return memory usage (0.0 to 1.0)
     * @since 1.0.0
     */
    @Override
    double memoryUsage();

    /**
     * Returns the ping/latency to this server.
     *
     * @return ping in ms, or -1 if unknown
     * @since 1.0.0
     */
    @Override
    long ping();
}
