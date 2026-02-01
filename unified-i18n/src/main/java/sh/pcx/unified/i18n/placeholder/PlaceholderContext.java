/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Context for placeholder resolution containing player and environment information.
 *
 * <p>The placeholder context provides all necessary information for resolving
 * a placeholder, including the viewing player, relational target player (for
 * relational placeholders), and additional custom data.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a simple player context
 * PlaceholderContext context = PlaceholderContext.of(player);
 *
 * // Create a relational context (two players)
 * PlaceholderContext relContext = PlaceholderContext.relational(viewer, target);
 *
 * // Create context with custom data
 * PlaceholderContext customContext = PlaceholderContext.builder()
 *     .player(player)
 *     .data("message", "Hello")
 *     .data("level", 42)
 *     .build();
 *
 * // Use in placeholder resolution
 * String result = placeholderService.resolve("%player_name%", context);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>PlaceholderContext is immutable and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderResolver
 * @see Relational
 */
public final class PlaceholderContext {

    /**
     * An empty context with no player or data.
     */
    public static final PlaceholderContext EMPTY = new PlaceholderContext(null, null, null, Collections.emptyMap());

    private final UnifiedPlayer player;
    private final OfflineUnifiedPlayer offlinePlayer;
    private final UnifiedPlayer relationalPlayer;
    private final Map<String, Object> data;

    private PlaceholderContext(
            @Nullable UnifiedPlayer player,
            @Nullable OfflineUnifiedPlayer offlinePlayer,
            @Nullable UnifiedPlayer relationalPlayer,
            @NotNull Map<String, Object> data) {
        this.player = player;
        this.offlinePlayer = offlinePlayer != null ? offlinePlayer : player;
        this.relationalPlayer = relationalPlayer;
        this.data = Collections.unmodifiableMap(new HashMap<>(data));
    }

    /**
     * Creates a context for a single online player.
     *
     * @param player the player
     * @return a new context
     */
    @NotNull
    public static PlaceholderContext of(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        return new PlaceholderContext(player, null, null, Collections.emptyMap());
    }

    /**
     * Creates a context for an offline player.
     *
     * @param player the offline player
     * @return a new context
     */
    @NotNull
    public static PlaceholderContext of(@NotNull OfflineUnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        if (player instanceof UnifiedPlayer) {
            return of((UnifiedPlayer) player);
        }
        return new PlaceholderContext(null, player, null, Collections.emptyMap());
    }

    /**
     * Creates a relational context for two players.
     *
     * @param viewer the viewing player
     * @param target the target player
     * @return a new relational context
     */
    @NotNull
    public static PlaceholderContext relational(@NotNull UnifiedPlayer viewer, @NotNull UnifiedPlayer target) {
        Objects.requireNonNull(viewer, "viewer cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        return new PlaceholderContext(viewer, null, target, Collections.emptyMap());
    }

    /**
     * Creates a new builder for constructing a context.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the online player in this context.
     *
     * @return an Optional containing the online player, or empty if not available
     */
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        return Optional.ofNullable(player);
    }

    /**
     * Returns the offline player in this context.
     *
     * <p>This returns the offline player if set, or the online player if available.
     *
     * @return an Optional containing the player, or empty if not available
     */
    @NotNull
    public Optional<OfflineUnifiedPlayer> getOfflinePlayer() {
        return Optional.ofNullable(offlinePlayer);
    }

    /**
     * Returns the relational target player.
     *
     * <p>This is the second player in a relational placeholder context.
     *
     * @return an Optional containing the relational player, or empty if not relational
     */
    @NotNull
    public Optional<UnifiedPlayer> getRelationalPlayer() {
        return Optional.ofNullable(relationalPlayer);
    }

    /**
     * Checks if this is a relational context (has two players).
     *
     * @return {@code true} if this context has a relational player
     */
    public boolean isRelational() {
        return relationalPlayer != null;
    }

    /**
     * Checks if this context has a player.
     *
     * @return {@code true} if a player is available
     */
    public boolean hasPlayer() {
        return offlinePlayer != null;
    }

    /**
     * Checks if the player in this context is online.
     *
     * @return {@code true} if the player is online
     */
    public boolean isPlayerOnline() {
        return player != null;
    }

    /**
     * Returns the player's UUID if available.
     *
     * @return an Optional containing the UUID
     */
    @NotNull
    public Optional<UUID> getPlayerUUID() {
        return getOfflinePlayer().map(OfflineUnifiedPlayer::getUniqueId);
    }

    /**
     * Returns the player's name if available.
     *
     * @return an Optional containing the name
     */
    @NotNull
    public Optional<String> getPlayerName() {
        return getOfflinePlayer().flatMap(OfflineUnifiedPlayer::getName);
    }

    /**
     * Returns custom data stored in this context.
     *
     * @param key  the data key
     * @param type the expected type
     * @param <T>  the type parameter
     * @return an Optional containing the data
     */
    @NotNull
    public <T> Optional<T> getData(@NotNull String key, @NotNull Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Returns custom data as a string.
     *
     * @param key the data key
     * @return an Optional containing the string value
     */
    @NotNull
    public Optional<String> getString(@NotNull String key) {
        Object value = data.get(key);
        return value != null ? Optional.of(String.valueOf(value)) : Optional.empty();
    }

    /**
     * Returns all custom data in this context.
     *
     * @return an unmodifiable map of custom data
     */
    @NotNull
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Creates a new context with additional data.
     *
     * @param key   the data key
     * @param value the data value
     * @return a new context with the added data
     */
    @NotNull
    public PlaceholderContext withData(@NotNull String key, @Nullable Object value) {
        Map<String, Object> newData = new HashMap<>(this.data);
        if (value != null) {
            newData.put(key, value);
        } else {
            newData.remove(key);
        }
        return new PlaceholderContext(player, offlinePlayer, relationalPlayer, newData);
    }

    /**
     * Creates a new context with a relational player.
     *
     * @param target the target player
     * @return a new relational context
     */
    @NotNull
    public PlaceholderContext withRelational(@NotNull UnifiedPlayer target) {
        Objects.requireNonNull(target, "target cannot be null");
        return new PlaceholderContext(player, offlinePlayer, target, data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlaceholderContext)) return false;
        PlaceholderContext other = (PlaceholderContext) obj;
        return Objects.equals(player, other.player)
                && Objects.equals(offlinePlayer, other.offlinePlayer)
                && Objects.equals(relationalPlayer, other.relationalPlayer)
                && Objects.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, offlinePlayer, relationalPlayer, data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlaceholderContext{");
        if (player != null) {
            sb.append("player=").append(player.getName());
        } else if (offlinePlayer != null) {
            sb.append("offlinePlayer=").append(offlinePlayer.getName());
        }
        if (relationalPlayer != null) {
            sb.append(", relationalPlayer=").append(relationalPlayer.getName());
        }
        if (!data.isEmpty()) {
            sb.append(", data=").append(data);
        }
        return sb.append("}").toString();
    }

    /**
     * Builder for creating {@link PlaceholderContext} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private UnifiedPlayer player;
        private OfflineUnifiedPlayer offlinePlayer;
        private UnifiedPlayer relationalPlayer;
        private final Map<String, Object> data = new HashMap<>();

        private Builder() {}

        /**
         * Sets the online player for this context.
         *
         * @param player the player
         * @return this builder
         */
        @NotNull
        public Builder player(@Nullable UnifiedPlayer player) {
            this.player = player;
            return this;
        }

        /**
         * Sets the offline player for this context.
         *
         * @param player the offline player
         * @return this builder
         */
        @NotNull
        public Builder offlinePlayer(@Nullable OfflineUnifiedPlayer player) {
            this.offlinePlayer = player;
            return this;
        }

        /**
         * Sets the relational target player.
         *
         * @param player the target player
         * @return this builder
         */
        @NotNull
        public Builder relationalPlayer(@Nullable UnifiedPlayer player) {
            this.relationalPlayer = player;
            return this;
        }

        /**
         * Adds custom data to the context.
         *
         * @param key   the data key
         * @param value the data value
         * @return this builder
         */
        @NotNull
        public Builder data(@NotNull String key, @Nullable Object value) {
            if (value != null) {
                this.data.put(key, value);
            }
            return this;
        }

        /**
         * Adds all data from a map.
         *
         * @param data the data to add
         * @return this builder
         */
        @NotNull
        public Builder data(@NotNull Map<String, Object> data) {
            this.data.putAll(data);
            return this;
        }

        /**
         * Builds the placeholder context.
         *
         * @return the constructed context
         */
        @NotNull
        public PlaceholderContext build() {
            return new PlaceholderContext(player, offlinePlayer, relationalPlayer, data);
        }
    }
}
