/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.impl;

import sh.pcx.unified.messaging.ServerInfo;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Default implementation of {@link ServerInfo}.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultServerInfo implements ServerInfo {

    private final String id;
    private final boolean local;
    private volatile String name;
    private volatile String address;
    private volatile boolean online;
    private volatile int playerCount;
    private volatile int maxPlayers;
    private volatile String type;
    private volatile String group;
    private volatile String motd;
    private volatile String version;
    private volatile Instant lastUpdated;
    private volatile long uptime;
    private volatile double tps;
    private volatile double memoryUsage;
    private volatile long ping;

    private final Set<UUID> players;
    private final Set<String> tags;
    private final Map<String, Object> metadata;

    /**
     * Creates a new server info.
     *
     * @param id    the server ID
     * @param local whether this is the local server
     */
    public DefaultServerInfo(@NotNull String id, boolean local) {
        this.id = Objects.requireNonNull(id);
        this.local = local;
        this.name = id;
        this.online = true;
        this.maxPlayers = -1;
        this.lastUpdated = Instant.now();
        this.tps = 20.0;
        this.ping = -1;
        this.players = new CopyOnWriteArraySet<>();
        this.tags = new CopyOnWriteArraySet<>();
        this.metadata = new ConcurrentHashMap<>();
    }

    @Override
    @NotNull
    public String id() {
        return id;
    }

    @Override
    @NotNull
    public String name() {
        return name;
    }

    /**
     * Sets the display name.
     *
     * @param name the name
     */
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /**
     * Sets the server address.
     *
     * @param address the address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean isLocal() {
        return local;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    /**
     * Sets the online status.
     *
     * @param online true if online
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public int playerCount() {
        return playerCount;
    }

    /**
     * Sets the player count.
     *
     * @param count the count
     */
    public void setPlayerCount(int count) {
        this.playerCount = count;
    }

    @Override
    public int maxPlayers() {
        return maxPlayers;
    }

    /**
     * Sets the max players.
     *
     * @param max the max
     */
    public void setMaxPlayers(int max) {
        this.maxPlayers = max;
    }

    @Override
    @NotNull
    public Collection<UUID> players() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * Adds a player.
     *
     * @param playerId the player UUID
     */
    public void addPlayer(@NotNull UUID playerId) {
        players.add(playerId);
        playerCount = players.size();
    }

    /**
     * Removes a player.
     *
     * @param playerId the player UUID
     */
    public void removePlayer(@NotNull UUID playerId) {
        players.remove(playerId);
        playerCount = players.size();
    }

    @Override
    @NotNull
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the server type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    @NotNull
    public Optional<String> group() {
        return Optional.ofNullable(group);
    }

    /**
     * Sets the server group.
     *
     * @param group the group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    @NotNull
    public Collection<String> tags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Adds a tag.
     *
     * @param tag the tag
     */
    public void addTag(@NotNull String tag) {
        tags.add(tag);
    }

    /**
     * Removes a tag.
     *
     * @param tag the tag
     */
    public void removeTag(@NotNull String tag) {
        tags.remove(tag);
    }

    @Override
    @NotNull
    public Map<String, Object> metadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Sets a metadata value.
     *
     * @param key   the key
     * @param value the value
     */
    public void setMetadata(@NotNull String key, Object value) {
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
    }

    @Override
    @NotNull
    public Optional<String> motd() {
        return Optional.ofNullable(motd);
    }

    /**
     * Sets the MOTD.
     *
     * @param motd the MOTD
     */
    public void setMotd(String motd) {
        this.motd = motd;
    }

    @Override
    @NotNull
    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Sets the Minecraft version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    @NotNull
    public Instant lastUpdated() {
        return lastUpdated;
    }

    /**
     * Updates the last updated timestamp.
     */
    public void touch() {
        this.lastUpdated = Instant.now();
    }

    @Override
    public long uptime() {
        return uptime;
    }

    /**
     * Sets the uptime.
     *
     * @param uptime uptime in milliseconds
     */
    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    @Override
    public double tps() {
        return tps;
    }

    /**
     * Sets the TPS.
     *
     * @param tps the TPS value
     */
    public void setTps(double tps) {
        this.tps = tps;
    }

    @Override
    public double memoryUsage() {
        return memoryUsage;
    }

    /**
     * Sets the memory usage.
     *
     * @param usage the memory usage (0.0 to 1.0)
     */
    public void setMemoryUsage(double usage) {
        this.memoryUsage = usage;
    }

    @Override
    public long ping() {
        return ping;
    }

    /**
     * Sets the ping.
     *
     * @param ping ping in milliseconds
     */
    public void setPing(long ping) {
        this.ping = ping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultServerInfo that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", online=" + online +
                ", players=" + playerCount +
                "/" + (maxPlayers < 0 ? "unlimited" : maxPlayers) +
                '}';
    }
}
