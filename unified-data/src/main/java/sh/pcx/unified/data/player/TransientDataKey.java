/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A {@link DataKey} that exists only in memory and is not persisted.
 *
 * <p>Transient data keys are stored in the player's session or in-memory cache
 * and are automatically cleared when the player disconnects or when the
 * configured expiration time passes. Use this for temporary data that should
 * not survive server restarts.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Define transient keys for temporary data
 * public final class SessionKeys {
 *     // Combat tag that expires after 15 seconds
 *     public static final TransientDataKey<Boolean> COMBAT_TAGGED =
 *         TransientDataKey.create("combat_tagged", Boolean.class, false)
 *             .withExpiration(Duration.ofSeconds(15));
 *
 *     // Cooldown timestamp (no expiration, cleared on logout)
 *     public static final TransientDataKey<Long> ABILITY_COOLDOWN =
 *         TransientDataKey.create("ability_cooldown", Long.class, 0L);
 *
 *     // Last teleport location (cleared on logout)
 *     public static final TransientDataKey<Location> LAST_TELEPORT =
 *         TransientDataKey.create("last_teleport", Location.class);
 * }
 *
 * // Usage
 * PlayerProfile profile = playerData.getProfile(player);
 *
 * // Set combat tag (auto-expires after configured duration)
 * profile.setData(SessionKeys.COMBAT_TAGGED, true);
 *
 * // Check if in combat
 * if (profile.getData(SessionKeys.COMBAT_TAGGED)) {
 *     player.sendMessage("You cannot teleport while in combat!");
 * }
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Combat tags and cooldowns</li>
 *   <li>Temporary permissions or buffs</li>
 *   <li>Session-specific state (last location, last command, etc.)</li>
 *   <li>Caching computed values</li>
 *   <li>Inter-plugin communication within a session</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>TransientDataKey instances are immutable and thread-safe. The values stored
 * using these keys are accessed through thread-safe data structures.
 *
 * @param <T> the type of value this key represents
 * @since 1.0.0
 * @author Supatuck
 * @see DataKey
 * @see PersistentDataKey
 * @see SessionData
 */
public final class TransientDataKey<T> extends DataKey<T> {

    private final Duration expiration;
    private final boolean sessionScoped;
    private final boolean clearOnServerSwitch;

    /**
     * Creates a new TransientDataKey with full configuration.
     *
     * @param key                 the unique identifier for this key
     * @param type                the class type of the value
     * @param defaultValue        the default value when no value is set
     * @param expiration          the duration after which the value expires, or null for no expiration
     * @param sessionScoped       whether the value is tied to the player's session
     * @param clearOnServerSwitch whether to clear the value when player switches servers
     */
    private TransientDataKey(@NotNull String key, @NotNull Class<T> type,
                              @Nullable T defaultValue, @Nullable Duration expiration,
                              boolean sessionScoped, boolean clearOnServerSwitch) {
        super(key, type, defaultValue, false);
        this.expiration = expiration;
        this.sessionScoped = sessionScoped;
        this.clearOnServerSwitch = clearOnServerSwitch;
    }

    /**
     * Creates a new TransientDataKey with default settings.
     *
     * <p>The key is session-scoped with no expiration and clears on server switch.
     *
     * @param key          the unique identifier for this key
     * @param type         the class type of the value
     * @param defaultValue the default value when no value is set
     * @param <T>          the type of value
     * @return a new TransientDataKey instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> TransientDataKey<T> create(@NotNull String key, @NotNull Class<T> type,
                                                  @Nullable T defaultValue) {
        return new TransientDataKey<>(key, type, defaultValue, null, true, true);
    }

    /**
     * Creates a new TransientDataKey without a default value.
     *
     * @param key  the unique identifier for this key
     * @param type the class type of the value
     * @param <T>  the type of value
     * @return a new TransientDataKey instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> TransientDataKey<T> create(@NotNull String key, @NotNull Class<T> type) {
        return create(key, type, null);
    }

    /**
     * Returns a builder for creating a TransientDataKey with custom settings.
     *
     * @param key  the unique identifier for this key
     * @param type the class type of the value
     * @param <T>  the type of value
     * @return a new builder instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder(@NotNull String key, @NotNull Class<T> type) {
        return new Builder<>(key, type);
    }

    /**
     * Creates a new TransientDataKey with the specified expiration.
     *
     * @param expiration the duration after which the value expires
     * @return a new TransientDataKey with the expiration set
     * @throws NullPointerException if expiration is null
     * @since 1.0.0
     */
    @NotNull
    public TransientDataKey<T> withExpiration(@NotNull Duration expiration) {
        Objects.requireNonNull(expiration, "expiration must not be null");
        return new TransientDataKey<>(getKey(), getType(), getDefaultValue(),
                expiration, sessionScoped, clearOnServerSwitch);
    }

    /**
     * Returns the expiration duration for values stored with this key.
     *
     * <p>After this duration passes since the value was set, the value is
     * automatically removed. Returns null if the value does not expire.
     *
     * @return the expiration duration, or null if no expiration
     * @since 1.0.0
     */
    @Nullable
    public Duration getExpiration() {
        return expiration;
    }

    /**
     * Returns whether this key has an expiration time.
     *
     * @return true if values expire after a set duration
     * @since 1.0.0
     */
    public boolean hasExpiration() {
        return expiration != null;
    }

    /**
     * Returns whether this key is session-scoped.
     *
     * <p>Session-scoped keys are automatically cleared when the player's
     * session ends (disconnects from the server).
     *
     * @return true if session-scoped
     * @since 1.0.0
     */
    public boolean isSessionScoped() {
        return sessionScoped;
    }

    /**
     * Returns whether values are cleared when the player switches servers.
     *
     * <p>On a network (BungeeCord/Velocity), this controls whether the value
     * persists when the player moves between backend servers.
     *
     * @return true if cleared on server switch
     * @since 1.0.0
     */
    public boolean isClearOnServerSwitch() {
        return clearOnServerSwitch;
    }

    @Override
    public String toString() {
        return "TransientDataKey{" +
                "key='" + getKey() + '\'' +
                ", type=" + getType().getSimpleName() +
                ", expiration=" + expiration +
                ", sessionScoped=" + sessionScoped +
                ", clearOnServerSwitch=" + clearOnServerSwitch +
                '}';
    }

    /**
     * Builder for creating TransientDataKey instances with custom configuration.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * TransientDataKey<Boolean> INVISIBILITY = TransientDataKey.builder("invisibility", Boolean.class)
     *     .defaultValue(false)
     *     .expiration(Duration.ofMinutes(5))
     *     .clearOnServerSwitch(false)
     *     .build();
     * }</pre>
     *
     * @param <T> the type of value
     * @since 1.0.0
     */
    public static final class Builder<T> {
        private final String key;
        private final Class<T> type;
        private T defaultValue;
        private Duration expiration;
        private boolean sessionScoped = true;
        private boolean clearOnServerSwitch = true;

        private Builder(@NotNull String key, @NotNull Class<T> type) {
            this.key = Objects.requireNonNull(key, "key must not be null");
            this.type = Objects.requireNonNull(type, "type must not be null");
        }

        /**
         * Sets the default value for this key.
         *
         * @param defaultValue the default value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> defaultValue(@Nullable T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Sets the expiration duration for values.
         *
         * @param expiration the expiration duration, or null for no expiration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> expiration(@Nullable Duration expiration) {
            this.expiration = expiration;
            return this;
        }

        /**
         * Sets whether the key is session-scoped.
         *
         * @param sessionScoped true to clear on session end
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> sessionScoped(boolean sessionScoped) {
            this.sessionScoped = sessionScoped;
            return this;
        }

        /**
         * Sets whether to clear values on server switch.
         *
         * @param clearOnServerSwitch true to clear on server switch
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> clearOnServerSwitch(boolean clearOnServerSwitch) {
            this.clearOnServerSwitch = clearOnServerSwitch;
            return this;
        }

        /**
         * Builds the TransientDataKey with the configured settings.
         *
         * @return the new TransientDataKey instance
         * @since 1.0.0
         */
        @NotNull
        public TransientDataKey<T> build() {
            return new TransientDataKey<>(key, type, defaultValue, expiration,
                    sessionScoped, clearOnServerSwitch);
        }
    }
}
