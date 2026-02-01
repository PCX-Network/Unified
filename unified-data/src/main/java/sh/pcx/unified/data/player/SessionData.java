/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Container for session-scoped player data.
 *
 * <p>SessionData holds transient data that exists only for the duration of a
 * player's session (from login to logout). Unlike persistent data, session data
 * is not saved to the database and is automatically cleared when the session ends.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get session data for a player
 * SessionData session = sessionManager.getSession(player.getUniqueId());
 *
 * // Store temporary data
 * session.set("lastCommand", Instant.now());
 * session.set("combatTagged", true);
 * session.set("tempCoins", 100);
 *
 * // Retrieve data
 * Optional<Instant> lastCmd = session.get("lastCommand", Instant.class);
 * boolean inCombat = session.getOrDefault("combatTagged", false);
 *
 * // Atomic operations
 * int newCoins = session.compute("tempCoins", Integer.class, old ->
 *     old != null ? old + 10 : 10
 * );
 * }</pre>
 *
 * <h2>Expiring Data</h2>
 * <p>Session data can be configured to expire after a certain duration using
 * {@link TransientDataKey}:
 * <pre>{@code
 * TransientDataKey<Boolean> COMBAT_TAG = TransientDataKey.builder("combat", Boolean.class)
 *     .expiration(Duration.ofSeconds(15))
 *     .build();
 *
 * session.set(COMBAT_TAG, true);
 * // Value automatically expires after 15 seconds
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe. Data is stored in a concurrent map.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SessionManager
 * @see TransientDataKey
 */
public final class SessionData {

    private final String sessionId;
    private final UUID playerId;
    private final Instant startTime;
    private final String serverName;
    private final String ipAddress;
    private final Map<String, ExpiringValue> data = new ConcurrentHashMap<>();
    private volatile boolean valid = true;

    /**
     * Creates a new SessionData instance.
     *
     * @param sessionId  the unique session identifier
     * @param playerId   the player's UUID
     * @param serverName the server name
     * @param ipAddress  the player's IP address
     */
    public SessionData(@NotNull String sessionId, @NotNull UUID playerId,
                       @NotNull String serverName, @NotNull String ipAddress) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.serverName = serverName;
        this.ipAddress = ipAddress;
        this.startTime = Instant.now();
    }

    /**
     * Returns the unique session identifier.
     *
     * @return the session ID
     * @since 1.0.0
     */
    @NotNull
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player's unique identifier
     * @since 1.0.0
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns when this session started.
     *
     * @return the session start time
     * @since 1.0.0
     */
    @NotNull
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns how long this session has been active.
     *
     * @return the session duration
     * @since 1.0.0
     */
    @NotNull
    public Duration getDuration() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Returns the server name for this session.
     *
     * @return the server name
     * @since 1.0.0
     */
    @NotNull
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the player's IP address for this session.
     *
     * @return the IP address
     * @since 1.0.0
     */
    @NotNull
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Checks if this session is still valid (player still online).
     *
     * @return true if the session is valid
     * @since 1.0.0
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Stores a value in the session.
     *
     * @param key   the key
     * @param value the value, or null to remove
     * @since 1.0.0
     */
    public void set(@NotNull String key, @Nullable Object value) {
        set(key, value, null);
    }

    /**
     * Stores a value in the session with an expiration time.
     *
     * @param key        the key
     * @param value      the value, or null to remove
     * @param expiration when the value should expire, or null for no expiration
     * @since 1.0.0
     */
    public void set(@NotNull String key, @Nullable Object value, @Nullable Duration expiration) {
        if (value == null) {
            data.remove(key);
        } else {
            Instant expiresAt = expiration != null ? Instant.now().plus(expiration) : null;
            data.put(key, new ExpiringValue(value, expiresAt));
        }
    }

    /**
     * Stores a value using a TransientDataKey.
     *
     * @param key   the data key
     * @param value the value, or null to remove
     * @param <T>   the value type
     * @since 1.0.0
     */
    public <T> void set(@NotNull TransientDataKey<T> key, @Nullable T value) {
        set(key.getKey(), value, key.getExpiration());
    }

    /**
     * Retrieves a value from the session.
     *
     * @param key  the key
     * @param type the expected type
     * @param <T>  the value type
     * @return an Optional containing the value if present and not expired
     * @since 1.0.0
     */
    @NotNull
    public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
        ExpiringValue entry = data.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            data.remove(key, entry);
            return Optional.empty();
        }
        if (type.isInstance(entry.value)) {
            return Optional.of(type.cast(entry.value));
        }
        return Optional.empty();
    }

    /**
     * Retrieves a value using a TransientDataKey.
     *
     * @param key the data key
     * @param <T> the value type
     * @return an Optional containing the value if present
     * @since 1.0.0
     */
    @NotNull
    public <T> Optional<T> get(@NotNull TransientDataKey<T> key) {
        return get(key.getKey(), key.getType());
    }

    /**
     * Retrieves a value with a default fallback.
     *
     * @param key          the key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return the value or the default
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(@NotNull String key, @NotNull T defaultValue) {
        ExpiringValue entry = data.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                data.remove(key, entry);
            }
            return defaultValue;
        }
        if (defaultValue.getClass().isInstance(entry.value)) {
            return (T) entry.value;
        }
        return defaultValue;
    }

    /**
     * Retrieves a value using a TransientDataKey with its default.
     *
     * @param key the data key
     * @param <T> the value type
     * @return the value or the key's default
     * @since 1.0.0
     */
    @Nullable
    public <T> T getOrDefault(@NotNull TransientDataKey<T> key) {
        return get(key).orElse(key.getDefaultValue());
    }

    /**
     * Checks if a key exists and is not expired.
     *
     * @param key the key
     * @return true if the key exists and is not expired
     * @since 1.0.0
     */
    public boolean has(@NotNull String key) {
        ExpiringValue entry = data.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            data.remove(key, entry);
            return false;
        }
        return true;
    }

    /**
     * Removes a key from the session.
     *
     * @param key the key
     * @return true if the key existed
     * @since 1.0.0
     */
    public boolean remove(@NotNull String key) {
        return data.remove(key) != null;
    }

    /**
     * Atomically computes a new value.
     *
     * @param key      the key
     * @param type     the value type
     * @param function the compute function
     * @param <T>      the value type
     * @return the computed value
     * @since 1.0.0
     */
    @NotNull
    public <T> T compute(@NotNull String key, @NotNull Class<T> type,
                         @NotNull Function<@Nullable T, @NotNull T> function) {
        ExpiringValue result = data.compute(key, (k, oldEntry) -> {
            T oldValue = null;
            if (oldEntry != null && !oldEntry.isExpired() && type.isInstance(oldEntry.value)) {
                oldValue = type.cast(oldEntry.value);
            }
            T newValue = function.apply(oldValue);
            return new ExpiringValue(newValue, null);
        });
        return type.cast(result.value);
    }

    /**
     * Increments an integer value atomically.
     *
     * @param key   the key
     * @param delta the amount to add
     * @return the new value
     * @since 1.0.0
     */
    public int increment(@NotNull String key, int delta) {
        return compute(key, Integer.class, old -> (old != null ? old : 0) + delta);
    }

    /**
     * Increments an integer value by 1.
     *
     * @param key the key
     * @return the new value
     * @since 1.0.0
     */
    public int increment(@NotNull String key) {
        return increment(key, 1);
    }

    /**
     * Returns all current keys (excluding expired).
     *
     * @return the set of keys
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getKeys() {
        cleanExpired();
        return Set.copyOf(data.keySet());
    }

    /**
     * Returns the number of stored values (excluding expired).
     *
     * @return the count
     * @since 1.0.0
     */
    public int size() {
        cleanExpired();
        return data.size();
    }

    /**
     * Clears all session data.
     *
     * @since 1.0.0
     */
    public void clear() {
        data.clear();
    }

    /**
     * Returns all data as an immutable map.
     *
     * @return the data map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> toMap() {
        cleanExpired();
        Map<String, Object> result = new ConcurrentHashMap<>();
        data.forEach((key, entry) -> {
            if (!entry.isExpired()) {
                result.put(key, entry.value);
            }
        });
        return Map.copyOf(result);
    }

    /**
     * Marks this session as ended.
     */
    void invalidate() {
        this.valid = false;
        clear();
    }

    /**
     * Removes all expired entries.
     */
    private void cleanExpired() {
        data.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @Override
    public String toString() {
        return "SessionData{" +
                "sessionId='" + sessionId + '\'' +
                ", playerId=" + playerId +
                ", duration=" + getDuration() +
                ", dataCount=" + size() +
                '}';
    }

    /**
     * Wrapper for values with optional expiration.
     */
    private static class ExpiringValue {
        final Object value;
        final Instant expiresAt;

        ExpiringValue(Object value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }
}
