/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Builder for creating {@link PlayerProfile} instances.
 *
 * <p>This builder is primarily used internally by the player data system for
 * constructing profiles from database records or for testing purposes.
 * Most plugin developers will obtain profiles through {@link PlayerDataService}
 * rather than building them directly.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Build a profile from loaded data
 * PlayerProfile profile = PlayerProfileBuilder.create(uuid)
 *     .name("Steve")
 *     .firstJoin(Instant.parse("2024-01-15T10:30:00Z"))
 *     .lastJoin(Instant.now())
 *     .totalPlayTime(Duration.ofHours(100))
 *     .data(MyPluginData.KILLS, 42)
 *     .data(MyPluginData.BALANCE, 1000.0)
 *     .build();
 *
 * // Build a new profile for a first-time player
 * PlayerProfile newPlayer = PlayerProfileBuilder.newProfile(uuid, "NewPlayer")
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The builder itself is NOT thread-safe and should be used from a single thread.
 * The resulting {@link PlayerProfile} is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerProfile
 * @see PlayerDataService
 */
public final class PlayerProfileBuilder {

    private final UUID uuid;
    private String name;
    private final List<String> nameHistory = new ArrayList<>();
    private Instant firstJoin;
    private Instant lastJoin;
    private Instant lastSeen;
    private Duration totalPlayTime = Duration.ZERO;
    private String sessionId;
    private boolean online = false;
    private String currentServer;
    private String lastIpAddress = "";
    private long dataVersion = 1L;
    private final Map<String, Object> metadata = new HashMap<>();
    private final Map<DataKey<?>, Object> data = new HashMap<>();

    /**
     * Creates a new builder for the specified player UUID.
     *
     * @param uuid the player's unique identifier
     */
    private PlayerProfileBuilder(@NotNull UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
    }

    /**
     * Creates a new builder for the specified player UUID.
     *
     * @param uuid the player's unique identifier
     * @return a new builder instance
     * @throws NullPointerException if uuid is null
     * @since 1.0.0
     */
    @NotNull
    public static PlayerProfileBuilder create(@NotNull UUID uuid) {
        return new PlayerProfileBuilder(uuid);
    }

    /**
     * Creates a builder configured for a new player's first join.
     *
     * <p>Sets appropriate defaults for first join timestamp, last join, etc.
     *
     * @param uuid the player's unique identifier
     * @param name the player's username
     * @return a new builder with first-join defaults
     * @throws NullPointerException if uuid or name is null
     * @since 1.0.0
     */
    @NotNull
    public static PlayerProfileBuilder newProfile(@NotNull UUID uuid, @NotNull String name) {
        Instant now = Instant.now();
        return new PlayerProfileBuilder(uuid)
                .name(name)
                .firstJoin(now)
                .lastJoin(now)
                .lastSeen(now)
                .online(true);
    }

    /**
     * Sets the player's username.
     *
     * @param name the player's username
     * @return this builder
     * @throws NullPointerException if name is null
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder name(@NotNull String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        return this;
    }

    /**
     * Adds a previous username to the name history.
     *
     * @param previousName a previous username
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder addNameHistory(@NotNull String previousName) {
        if (previousName != null && !previousName.isEmpty()) {
            this.nameHistory.add(previousName);
        }
        return this;
    }

    /**
     * Sets the complete name history.
     *
     * @param nameHistory the list of previous usernames
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder nameHistory(@NotNull List<String> nameHistory) {
        this.nameHistory.clear();
        if (nameHistory != null) {
            this.nameHistory.addAll(nameHistory);
        }
        return this;
    }

    /**
     * Sets the first join timestamp.
     *
     * @param firstJoin when the player first joined
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder firstJoin(@NotNull Instant firstJoin) {
        this.firstJoin = firstJoin;
        return this;
    }

    /**
     * Sets the last join timestamp.
     *
     * @param lastJoin when the player last joined
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder lastJoin(@NotNull Instant lastJoin) {
        this.lastJoin = lastJoin;
        return this;
    }

    /**
     * Sets the last seen timestamp.
     *
     * @param lastSeen when the player was last seen
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder lastSeen(@NotNull Instant lastSeen) {
        this.lastSeen = lastSeen;
        return this;
    }

    /**
     * Sets the total play time.
     *
     * @param totalPlayTime the total time played
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder totalPlayTime(@NotNull Duration totalPlayTime) {
        this.totalPlayTime = totalPlayTime != null ? totalPlayTime : Duration.ZERO;
        return this;
    }

    /**
     * Sets the current session ID.
     *
     * @param sessionId the session ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder sessionId(@Nullable String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Sets whether the player is currently online.
     *
     * @param online true if online
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder online(boolean online) {
        this.online = online;
        return this;
    }

    /**
     * Sets the current server name.
     *
     * @param currentServer the server name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder currentServer(@Nullable String currentServer) {
        this.currentServer = currentServer;
        return this;
    }

    /**
     * Sets the last known IP address.
     *
     * @param ipAddress the IP address
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder lastIpAddress(@NotNull String ipAddress) {
        this.lastIpAddress = ipAddress != null ? ipAddress : "";
        return this;
    }

    /**
     * Sets the data schema version.
     *
     * @param dataVersion the version number
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder dataVersion(long dataVersion) {
        this.dataVersion = dataVersion;
        return this;
    }

    /**
     * Adds metadata.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder metadata(@NotNull String key, @NotNull Object value) {
        if (key != null && value != null) {
            this.metadata.put(key, value);
        }
        return this;
    }

    /**
     * Sets all metadata from a map.
     *
     * @param metadata the metadata map
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder metadata(@NotNull Map<String, Object> metadata) {
        this.metadata.clear();
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        return this;
    }

    /**
     * Sets a data value for the specified key.
     *
     * @param key   the data key
     * @param value the value to set
     * @param <T>   the type of the value
     * @return this builder
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    @NotNull
    public <T> PlayerProfileBuilder data(@NotNull DataKey<T> key, @Nullable T value) {
        Objects.requireNonNull(key, "key must not be null");
        if (value != null) {
            this.data.put(key, value);
        } else {
            this.data.remove(key);
        }
        return this;
    }

    /**
     * Sets all data from a map.
     *
     * @param data the data map
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfileBuilder data(@NotNull Map<DataKey<?>, Object> data) {
        this.data.clear();
        if (data != null) {
            this.data.putAll(data);
        }
        return this;
    }

    /**
     * Builds the {@link PlayerProfile} with the configured values.
     *
     * <p>The returned profile is an immutable snapshot. Changes to this builder
     * after calling build() will not affect the built profile.
     *
     * @return a new PlayerProfile instance
     * @throws IllegalStateException if required fields are not set
     * @since 1.0.0
     */
    @NotNull
    public PlayerProfile build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Player name must be set");
        }
        if (firstJoin == null) {
            firstJoin = Instant.now();
        }
        if (lastJoin == null) {
            lastJoin = firstJoin;
        }
        if (lastSeen == null) {
            lastSeen = online ? Instant.now() : lastJoin;
        }

        return new DefaultPlayerProfile(
                uuid,
                name,
                List.copyOf(nameHistory),
                firstJoin,
                lastJoin,
                lastSeen,
                totalPlayTime,
                sessionId,
                online,
                currentServer,
                lastIpAddress,
                dataVersion,
                Map.copyOf(metadata),
                new HashMap<>(data)
        );
    }

    /**
     * Returns the UUID being used for this builder.
     *
     * @return the player UUID
     * @since 1.0.0
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Returns the current name set in this builder.
     *
     * @return the player name, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public String getName() {
        return name;
    }
}
