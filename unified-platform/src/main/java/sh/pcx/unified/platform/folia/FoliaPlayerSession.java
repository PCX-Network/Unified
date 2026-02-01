/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.player.PlayerSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Region-aware session implementation for Folia.
 *
 * <p>This implementation provides thread-safe session data storage that works
 * correctly across Folia's multiple region threads. All operations use
 * concurrent data structures to ensure safe access.
 *
 * <h2>Thread Safety</h2>
 * <p>All session operations are fully thread-safe. The internal storage uses
 * {@link ConcurrentHashMap} which provides atomic operations without explicit
 * synchronization.
 *
 * <h2>Session Lifecycle</h2>
 * <p>A session begins when the player joins and ends when they disconnect.
 * Session data is automatically cleared when the session ends.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerSession
 * @see FoliaUnifiedPlayer
 */
public final class FoliaPlayerSession implements PlayerSession {

    /**
     * Counter for generating unique session IDs.
     */
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger(0);

    /**
     * The unique session identifier.
     */
    private final String sessionId;

    /**
     * The player this session belongs to.
     */
    private final FoliaUnifiedPlayer player;

    /**
     * The player's UUID.
     */
    private final UUID playerId;

    /**
     * The time when this session started.
     */
    private final Instant joinTime;

    /**
     * Thread-safe storage for session data.
     */
    private final ConcurrentHashMap<String, Object> data;

    /**
     * The IP address for this session.
     */
    private final String ipAddress;

    /**
     * Atomic counters for common operations.
     */
    private final ConcurrentHashMap<String, AtomicInteger> counters;

    /**
     * Flag indicating if session is still valid.
     */
    private volatile boolean valid;

    /**
     * Constructs a new FoliaPlayerSession.
     *
     * @param player the player this session belongs to
     * @since 1.0.0
     */
    public FoliaPlayerSession(@NotNull FoliaUnifiedPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.playerId = player.getUniqueId();
        this.joinTime = Instant.now();
        this.sessionId = generateSessionId();
        this.data = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();
        this.ipAddress = player.getAddress();
        this.valid = true;
    }

    /**
     * Generates a unique session ID.
     */
    private String generateSessionId() {
        int count = SESSION_COUNTER.incrementAndGet();
        return String.format("%s-%d-%d",
                playerId.toString().substring(0, 8),
                joinTime.toEpochMilli(),
                count);
    }

    @Override
    @NotNull
    public String getSessionId() {
        return sessionId;
    }

    @Override
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    @NotNull
    public Instant getJoinTime() {
        return joinTime;
    }

    @Override
    @NotNull
    public Duration getDuration() {
        return Duration.between(joinTime, Instant.now());
    }

    @Override
    public <T> void set(@NotNull String key, @Nullable T value) {
        Objects.requireNonNull(key, "key cannot be null");

        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }

        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }

        return Optional.empty();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(@NotNull String key, @NotNull T defaultValue) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");

        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean has(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return data.containsKey(key);
    }

    @Override
    public boolean remove(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return data.remove(key) != null;
    }

    @Override
    public void clear() {
        data.clear();
        counters.clear();
    }

    @Override
    @NotNull
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T compute(@NotNull String key, @NotNull Class<T> type,
                          @NotNull Function<Optional<T>, T> function) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(function, "function cannot be null");

        return (T) data.compute(key, (k, oldValue) -> {
            Optional<T> current = Optional.empty();
            if (oldValue != null && type.isInstance(oldValue)) {
                current = Optional.of((T) oldValue);
            }
            return function.apply(current);
        });
    }

    @Override
    public <T> boolean setIfAbsent(@NotNull String key, @NotNull T value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        return data.putIfAbsent(key, value) == null;
    }

    @Override
    public int increment(@NotNull String key, int delta) {
        Objects.requireNonNull(key, "key cannot be null");

        AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int newValue = counter.addAndGet(delta);

        // Also store in data map for compatibility
        data.put(key, newValue);

        return newValue;
    }

    @Override
    @NotNull
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(data));
    }

    @Override
    @NotNull
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    @NotNull
    public String getServerName() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object server = bukkitClass.getMethod("getServer").invoke(null);
            return (String) server.getClass().getMethod("getName").invoke(server);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public boolean isValid() {
        return valid && player.isValid();
    }

    /**
     * Invalidates this session.
     *
     * <p>Called when the player disconnects to mark the session as ended.
     *
     * @since 1.0.0
     */
    public void invalidate() {
        this.valid = false;
    }

    /**
     * Returns the player associated with this session.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public FoliaUnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the current region context for this player.
     *
     * <p>The region context represents the Folia region that currently
     * owns this player's entity.
     *
     * @return the region context for the player's current location
     * @since 1.0.0
     */
    @NotNull
    public RegionContext getRegionContext() {
        return RegionContext.of(player.getLocation());
    }

    /**
     * Checks if the current thread owns this player's region.
     *
     * @return true if safe to modify the player on this thread
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentThread() {
        return getRegionContext().isOwnedByCurrentThread();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FoliaPlayerSession other)) return false;
        return sessionId.equals(other.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("FoliaPlayerSession[id=%s, player=%s, duration=%s, valid=%s]",
                sessionId, playerId, getDuration(), valid);
    }
}
