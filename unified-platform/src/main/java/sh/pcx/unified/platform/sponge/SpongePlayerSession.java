/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.player.PlayerSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Sponge implementation of the {@link PlayerSession} interface.
 *
 * <p>This class manages transient session data for a player that is cleared
 * when the session ends (player disconnects). Session data is stored in a
 * thread-safe concurrent map.
 *
 * <h2>Session Lifecycle</h2>
 * <ul>
 *   <li>Created when a player connects to the server</li>
 *   <li>Data is stored in memory only (not persisted)</li>
 *   <li>Cleared when the player disconnects</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe. The internal data store uses
 * {@link ConcurrentHashMap} for safe concurrent access.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerSession
 * @see SpongeUnifiedPlayer
 */
public final class SpongePlayerSession implements PlayerSession {

    private final String sessionId;
    private final UUID playerId;
    private final Instant joinTime;
    private final String ipAddress;
    private final String serverName;
    private final ConcurrentHashMap<String, Object> data;

    /**
     * Creates a new SpongePlayerSession for the given player.
     *
     * @param player the Sponge ServerPlayer
     * @since 1.0.0
     */
    public SpongePlayerSession(@NotNull ServerPlayer player) {
        this.sessionId = UUID.randomUUID().toString();
        this.playerId = player.uniqueId();
        this.joinTime = Instant.now();
        this.ipAddress = player.connection().address().getAddress().getHostAddress();
        // Use server MOTD as a fallback server name
        this.serverName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(Sponge.server().motd());
        this.data = new ConcurrentHashMap<>();
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
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
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
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(@NotNull String key) {
        return data.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(@NotNull String key) {
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
        return Collections.unmodifiableSet(data.keySet());
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
    public <T> T compute(@NotNull String key, @NotNull Class<T> type,
                         @NotNull Function<Optional<T>, T> function) {
        return data.compute(key, (k, v) -> {
            Optional<T> current = Optional.empty();
            if (v != null && type.isInstance(v)) {
                current = Optional.of(type.cast(v));
            }
            return function.apply(current);
        }) == null ? function.apply(Optional.empty()) : type.cast(data.get(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean setIfAbsent(@NotNull String key, @NotNull T value) {
        return data.putIfAbsent(key, value) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int increment(@NotNull String key, int delta) {
        return (int) data.compute(key, (k, v) -> {
            if (v == null) {
                return delta;
            }
            if (v instanceof Integer) {
                return (Integer) v + delta;
            }
            return delta;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(data));
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
        return Sponge.server().player(playerId).isPresent();
    }

    /**
     * Returns a string representation of this session.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongePlayerSession{" +
                "sessionId='" + sessionId + '\'' +
                ", playerId=" + playerId +
                ", joinTime=" + joinTime +
                ", dataSize=" + data.size() +
                '}';
    }
}
