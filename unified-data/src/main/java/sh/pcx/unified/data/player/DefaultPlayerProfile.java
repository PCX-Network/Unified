/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Default implementation of {@link PlayerProfile}.
 *
 * <p>This class provides a thread-safe implementation of the PlayerProfile interface
 * with support for dirty tracking, atomic operations, and persistence.
 *
 * @since 1.0.0
 * @author Supatuck
 */
final class DefaultPlayerProfile implements PlayerProfile {

    private final UUID uuid;
    private volatile String name;
    private final List<String> nameHistory;
    private final Instant firstJoin;
    private volatile Instant lastJoin;
    private volatile Instant lastSeen;
    private volatile Duration totalPlayTime;
    private volatile String sessionId;
    private volatile boolean online;
    private volatile String currentServer;
    private volatile String lastIpAddress;
    private final long dataVersion;
    private final Map<String, Object> metadata;
    private final Map<DataKey<?>, Object> data;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicReference<Instant> lastSaveTime = new AtomicReference<>();

    /**
     * Creates a new DefaultPlayerProfile with the specified values.
     */
    DefaultPlayerProfile(
            @NotNull UUID uuid,
            @NotNull String name,
            @NotNull List<String> nameHistory,
            @NotNull Instant firstJoin,
            @NotNull Instant lastJoin,
            @NotNull Instant lastSeen,
            @NotNull Duration totalPlayTime,
            @Nullable String sessionId,
            boolean online,
            @Nullable String currentServer,
            @NotNull String lastIpAddress,
            long dataVersion,
            @NotNull Map<String, Object> metadata,
            @NotNull Map<DataKey<?>, Object> data
    ) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.nameHistory = List.copyOf(nameHistory);
        this.firstJoin = Objects.requireNonNull(firstJoin, "firstJoin must not be null");
        this.lastJoin = Objects.requireNonNull(lastJoin, "lastJoin must not be null");
        this.lastSeen = Objects.requireNonNull(lastSeen, "lastSeen must not be null");
        this.totalPlayTime = Objects.requireNonNull(totalPlayTime, "totalPlayTime must not be null");
        this.sessionId = sessionId;
        this.online = online;
        this.currentServer = currentServer;
        this.lastIpAddress = Objects.requireNonNull(lastIpAddress, "lastIpAddress must not be null");
        this.dataVersion = dataVersion;
        this.metadata = new ConcurrentHashMap<>(metadata);
        this.data = new ConcurrentHashMap<>(data);
    }

    // ==================== Identity ====================

    @Override
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    @NotNull
    public String getLastKnownName() {
        return name;
    }

    @Override
    @NotNull
    public List<String> getNameHistory() {
        return nameHistory;
    }

    // ==================== Timestamps ====================

    @Override
    @NotNull
    public Instant getFirstJoin() {
        return firstJoin;
    }

    @Override
    @NotNull
    public Instant getLastJoin() {
        return lastJoin;
    }

    @Override
    @NotNull
    public Instant getLastSeen() {
        if (online) {
            return Instant.now();
        }
        return lastSeen;
    }

    @Override
    @NotNull
    public Duration getTotalPlayTime() {
        if (online && lastJoin != null) {
            Duration sessionTime = Duration.between(lastJoin, Instant.now());
            return totalPlayTime.plus(sessionTime);
        }
        return totalPlayTime;
    }

    @Override
    @NotNull
    public Duration getSessionPlayTime() {
        if (online && lastJoin != null) {
            return Duration.between(lastJoin, Instant.now());
        }
        return Duration.ZERO;
    }

    // ==================== Session ====================

    @Override
    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    @Override
    @NotNull
    public Optional<String> getCurrentServer() {
        return Optional.ofNullable(currentServer);
    }

    // ==================== Data Access ====================

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getData(@NotNull DataKey<T> key) {
        Objects.requireNonNull(key, "key must not be null");
        Object value = data.get(key);
        if (value != null) {
            return key.cast(value);
        }
        return key.getDefaultValue();
    }

    @Override
    @NotNull
    public <T> T getData(@NotNull DataKey<T> key, @NotNull T defaultValue) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        T value = getData(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public <T> void setData(@NotNull DataKey<T> key, @Nullable T value) {
        Objects.requireNonNull(key, "key must not be null");
        if (value != null) {
            if (!key.isValidValue(value)) {
                throw new ClassCastException("Value type " + value.getClass().getName()
                        + " is not compatible with key type " + key.getType().getName());
            }
            data.put(key, value);
        } else {
            data.remove(key);
        }
        dirty.set(true);
    }

    @Override
    public <T> void removeData(@NotNull DataKey<T> key) {
        Objects.requireNonNull(key, "key must not be null");
        if (data.remove(key) != null) {
            dirty.set(true);
        }
    }

    @Override
    public boolean hasData(@NotNull DataKey<?> key) {
        Objects.requireNonNull(key, "key must not be null");
        return data.containsKey(key);
    }

    @Override
    @NotNull
    public Set<DataKey<?>> getDataKeys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T compute(@NotNull DataKey<T> key,
                         @NotNull Function<@Nullable T, @NotNull T> function) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(function, "function must not be null");

        Object result = data.compute(key, (k, oldValue) -> {
            T typedOldValue = oldValue != null ? key.cast(oldValue) : key.getDefaultValue();
            T newValue = function.apply(typedOldValue);
            if (newValue == null) {
                throw new NullPointerException("Compute function must not return null");
            }
            return newValue;
        });

        dirty.set(true);
        return key.cast(result);
    }

    @Override
    public <T> boolean setDataIfAbsent(@NotNull DataKey<T> key, @NotNull T value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        Object previous = data.putIfAbsent(key, value);
        if (previous == null) {
            dirty.set(true);
            return true;
        }
        return false;
    }

    // ==================== Persistence ====================

    @Override
    @NotNull
    public CompletableFuture<Void> save() {
        // In a real implementation, this would persist to database
        // For now, we just mark as saved
        return CompletableFuture.runAsync(() -> {
            // Simulate save operation
            dirty.set(false);
            lastSaveTime.set(Instant.now());
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Void> reload() {
        // In a real implementation, this would reload from database
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void invalidateCache() {
        // In a real implementation, this would invalidate cached data
    }

    @Override
    public boolean isDirty() {
        return dirty.get();
    }

    @Override
    @NotNull
    public Optional<Instant> getLastSaveTime() {
        return Optional.ofNullable(lastSaveTime.get());
    }

    // ==================== Metadata ====================

    @Override
    @NotNull
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public long getDataVersion() {
        return dataVersion;
    }

    @Override
    @NotNull
    public String getLastIpAddress() {
        return lastIpAddress;
    }

    // ==================== Locking ====================

    @Override
    @NotNull
    public CompletableFuture<PlayerLock> acquireLock() {
        // In a real implementation, this would acquire a distributed lock
        return CompletableFuture.completedFuture(new SimplePlayerLock(uuid));
    }

    @Override
    @NotNull
    public Optional<PlayerLock> tryAcquireLock() {
        // In a real implementation, this would attempt to acquire without blocking
        return Optional.of(new SimplePlayerLock(uuid));
    }

    @Override
    public boolean isLocked() {
        // In a real implementation, this would check distributed lock state
        return false;
    }

    // ==================== Internal Methods ====================

    /**
     * Updates the session state when a player logs in.
     *
     * @param sessionId     the new session ID
     * @param serverName    the server name
     * @param ipAddress     the player's IP address
     */
    void startSession(@NotNull String sessionId, @NotNull String serverName, @NotNull String ipAddress) {
        this.sessionId = sessionId;
        this.online = true;
        this.currentServer = serverName;
        this.lastJoin = Instant.now();
        this.lastIpAddress = ipAddress;
        dirty.set(true);
    }

    /**
     * Updates the session state when a player logs out.
     */
    void endSession() {
        if (online) {
            this.totalPlayTime = getTotalPlayTime(); // Capture current total
            this.lastSeen = Instant.now();
        }
        this.sessionId = null;
        this.online = false;
        this.currentServer = null;
        dirty.set(true);
    }

    /**
     * Updates the player's name if it has changed.
     *
     * @param newName the new username
     */
    void updateName(@NotNull String newName) {
        if (!newName.equals(this.name)) {
            this.name = newName;
            dirty.set(true);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultPlayerProfile that = (DefaultPlayerProfile) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "PlayerProfile{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", online=" + online +
                ", dataKeys=" + data.size() +
                '}';
    }

    /**
     * Simple implementation of PlayerLock for local testing.
     */
    private static class SimplePlayerLock implements PlayerLock {
        private final UUID playerId;
        private final Instant acquiredAt = Instant.now();
        private volatile boolean released = false;

        SimplePlayerLock(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public @NotNull UUID getPlayerId() {
            return playerId;
        }

        @Override
        public @NotNull String getLockId() {
            return "lock:" + playerId;
        }

        @Override
        public @NotNull Instant getAcquiredAt() {
            return acquiredAt;
        }

        @Override
        public boolean isHeld() {
            return !released;
        }

        @Override
        public void release() {
            released = true;
        }

        @Override
        public void close() {
            release();
        }
    }
}
