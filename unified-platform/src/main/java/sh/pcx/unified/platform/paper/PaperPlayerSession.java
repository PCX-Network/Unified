/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.player.PlayerSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Paper/Spigot implementation of {@link PlayerSession}.
 *
 * <p>This class provides transient session storage for online players.
 * Session data is automatically cleared when the player disconnects.
 *
 * <h2>Data Storage</h2>
 * <p>Session data is stored in a ConcurrentHashMap, making all operations
 * thread-safe. The session ID is generated using a combination of the
 * player's UUID and join timestamp.
 *
 * <h2>Atomic Operations</h2>
 * <p>The {@link #compute}, {@link #setIfAbsent}, and {@link #increment} methods
 * provide atomic operations for thread-safe read-modify-write patterns.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PlayerSession session = player.getSession();
 *
 * // Store data
 * session.set("last_action", Instant.now());
 * session.set("kills", 0);
 *
 * // Atomic increment
 * int kills = session.increment("kills");
 *
 * // Retrieve data
 * Instant lastAction = session.getOrDefault("last_action", Instant.EPOCH);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerSession
 */
public final class PaperPlayerSession implements PlayerSession {

    private final Player player;
    private final String sessionId;
    private final UUID playerId;
    private final Instant joinTime;
    private final String ipAddress;
    private final String serverName;
    private final ConcurrentHashMap<String, Object> data;

    /**
     * Creates a new PaperPlayerSession for the given player.
     *
     * @param player the Bukkit player
     * @since 1.0.0
     */
    public PaperPlayerSession(@NotNull Player player) {
        this.player = Objects.requireNonNull(player, "player");
        this.playerId = player.getUniqueId();
        this.joinTime = Instant.now();
        this.sessionId = generateSessionId(playerId, joinTime);
        this.data = new ConcurrentHashMap<>();

        // Capture IP address
        var address = player.getAddress();
        this.ipAddress = address != null ? address.getAddress().getHostAddress() : "0.0.0.0";

        // Get server name
        this.serverName = Bukkit.getServer().getName();
    }

    /**
     * Generates a unique session ID.
     *
     * @param playerId the player's UUID
     * @param joinTime the join timestamp
     * @return a unique session identifier
     */
    @NotNull
    private static String generateSessionId(@NotNull UUID playerId, @NotNull Instant joinTime) {
        return playerId.toString().substring(0, 8) + "-" + Long.toHexString(joinTime.toEpochMilli());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getSessionId() {
        return sessionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Instant getJoinTime() {
        return joinTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Duration getDuration() {
        return Duration.between(joinTime, Instant.now());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void set(@NotNull String key, @Nullable T value) {
        Objects.requireNonNull(key, "key");
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");

        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(@NotNull String key, @NotNull T defaultValue) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(defaultValue, "defaultValue");

        Object value = data.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(@NotNull String key) {
        Objects.requireNonNull(key, "key");
        return data.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(@NotNull String key) {
        Objects.requireNonNull(key, "key");
        return data.remove(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        data.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(new HashSet<>(data.keySet()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return data.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T compute(@NotNull String key, @NotNull Class<T> type,
                         @NotNull Function<Optional<T>, T> function) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(function, "function");

        Object newValue = data.compute(key, (k, v) -> {
            Optional<T> existing = (v != null && type.isInstance(v))
                    ? Optional.of(type.cast(v))
                    : Optional.empty();
            return function.apply(existing);
        });

        return type.cast(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean setIfAbsent(@NotNull String key, @NotNull T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        return data.putIfAbsent(key, value) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int increment(@NotNull String key, int delta) {
        Objects.requireNonNull(key, "key");

        // Use compute for atomic operation
        AtomicInteger result = new AtomicInteger();
        data.compute(key, (k, v) -> {
            int current = 0;
            if (v instanceof Number) {
                current = ((Number) v).intValue();
            }
            int newValue = current + delta;
            result.set(newValue);
            return newValue;
        });
        return result.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new HashMap<>(data));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getServerName() {
        return serverName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return player.isOnline();
    }

    /**
     * Returns a string representation of this session.
     *
     * @return a string containing session ID and player UUID
     */
    @Override
    public String toString() {
        return "PaperPlayerSession{" +
                "sessionId='" + sessionId + '\'' +
                ", playerId=" + playerId +
                ", joinTime=" + joinTime +
                ", dataSize=" + data.size() +
                '}';
    }
}
