/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Context object containing all information needed for condition evaluation.
 *
 * <p>A ConditionContext holds references to the player, location, world, and
 * any additional metadata needed to evaluate conditions. Contexts are immutable
 * and can be safely shared across threads.</p>
 *
 * <h2>Creating Contexts:</h2>
 * <pre>{@code
 * // From a player (location and world derived from player)
 * ConditionContext ctx1 = ConditionContext.of(player);
 *
 * // From a location
 * ConditionContext ctx2 = ConditionContext.of(location);
 *
 * // Using builder for full control
 * ConditionContext ctx3 = ConditionContext.builder()
 *     .player(player)
 *     .location(customLocation)
 *     .world(customWorld)
 *     .metadata("key", value)
 *     .build();
 * }</pre>
 *
 * <h2>Accessing Context Data:</h2>
 * <pre>{@code
 * context.getPlayer().ifPresent(player -> {
 *     // Use player
 * });
 *
 * String world = context.getWorldName().orElse("unknown");
 * Object value = context.getMetadata("customKey").orElse(null);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 * @see ConditionResult
 */
public sealed interface ConditionContext permits ConditionContext.Impl {

    /**
     * Returns the player associated with this context.
     *
     * @return an optional containing the player, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedPlayer> getPlayer();

    /**
     * Returns the location associated with this context.
     *
     * <p>If no explicit location was set but a player is present,
     * returns the player's location.</p>
     *
     * @return an optional containing the location, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedLocation> getLocation();

    /**
     * Returns the world associated with this context.
     *
     * <p>If no explicit world was set but a player is present,
     * returns the player's world. If a location is present,
     * returns the location's world.</p>
     *
     * @return an optional containing the world, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld();

    /**
     * Returns the name of the world in this context.
     *
     * @return an optional containing the world name, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getWorldName();

    /**
     * Returns the unique identifier of the player in this context.
     *
     * @return an optional containing the player's UUID, or empty if no player
     * @since 1.0.0
     */
    @NotNull
    Optional<UUID> getPlayerId();

    /**
     * Returns the timestamp when this context was created.
     *
     * @return the context creation timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getTimestamp();

    /**
     * Returns the local date time for this context.
     *
     * @return the local date time
     * @since 1.0.0
     */
    @NotNull
    default LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(getTimestamp(), getTimeZone());
    }

    /**
     * Returns the time zone for this context.
     *
     * @return the time zone
     * @since 1.0.0
     */
    @NotNull
    ZoneId getTimeZone();

    /**
     * Returns the unique identifier for this context.
     *
     * @return the context ID
     * @since 1.0.0
     */
    @NotNull
    UUID getContextId();

    /**
     * Returns metadata associated with this context.
     *
     * @param key the metadata key
     * @return an optional containing the value, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<Object> getMetadata(@NotNull String key);

    /**
     * Returns typed metadata associated with this context.
     *
     * @param <T>  the expected value type
     * @param key  the metadata key
     * @param type the expected class type
     * @return an optional containing the typed value, or empty if not found or wrong type
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getMetadata(@NotNull String key, @NotNull Class<T> type);

    /**
     * Returns all metadata in this context.
     *
     * @return an unmodifiable map of all metadata
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> getAllMetadata();

    /**
     * Checks if this context has a player.
     *
     * @return true if a player is present
     * @since 1.0.0
     */
    boolean hasPlayer();

    /**
     * Checks if this context has a location.
     *
     * @return true if a location is present
     * @since 1.0.0
     */
    boolean hasLocation();

    /**
     * Checks if this context has a world.
     *
     * @return true if a world is present
     * @since 1.0.0
     */
    boolean hasWorld();

    /**
     * Creates a new context with additional metadata.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return a new context with the added metadata
     * @since 1.0.0
     */
    @NotNull
    ConditionContext withMetadata(@NotNull String key, @NotNull Object value);

    /**
     * Creates a new context with a different player.
     *
     * @param player the new player
     * @return a new context with the updated player
     * @since 1.0.0
     */
    @NotNull
    ConditionContext withPlayer(@Nullable UnifiedPlayer player);

    /**
     * Creates a new context with a different location.
     *
     * @param location the new location
     * @return a new context with the updated location
     * @since 1.0.0
     */
    @NotNull
    ConditionContext withLocation(@Nullable UnifiedLocation location);

    /**
     * Creates a new context with a different world.
     *
     * @param world the new world
     * @return a new context with the updated world
     * @since 1.0.0
     */
    @NotNull
    ConditionContext withWorld(@Nullable UnifiedWorld world);

    /**
     * Creates a new context with a different time zone.
     *
     * @param timeZone the new time zone
     * @return a new context with the updated time zone
     * @since 1.0.0
     */
    @NotNull
    ConditionContext withTimeZone(@NotNull ZoneId timeZone);

    // ==================== Static Factory Methods ====================

    /**
     * Creates a context from a player.
     *
     * <p>Location and world are derived from the player.</p>
     *
     * @param player the player
     * @return a new context for the player
     * @since 1.0.0
     */
    @NotNull
    static ConditionContext of(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        return new Impl(player, null, null, Collections.emptyMap(), null, ZoneId.systemDefault());
    }

    /**
     * Creates a context from a location.
     *
     * @param location the location
     * @return a new context for the location
     * @since 1.0.0
     */
    @NotNull
    static ConditionContext of(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");
        return new Impl(null, location, null, Collections.emptyMap(), null, ZoneId.systemDefault());
    }

    /**
     * Creates a context from a world.
     *
     * @param world the world
     * @return a new context for the world
     * @since 1.0.0
     */
    @NotNull
    static ConditionContext of(@NotNull UnifiedWorld world) {
        Objects.requireNonNull(world, "world cannot be null");
        return new Impl(null, null, world, Collections.emptyMap(), null, ZoneId.systemDefault());
    }

    /**
     * Creates a context from a player and location.
     *
     * @param player   the player
     * @param location the location
     * @return a new context
     * @since 1.0.0
     */
    @NotNull
    static ConditionContext of(@NotNull UnifiedPlayer player, @NotNull UnifiedLocation location) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        return new Impl(player, location, null, Collections.emptyMap(), null, ZoneId.systemDefault());
    }

    /**
     * Creates an empty context with no player, location, or world.
     *
     * @return an empty context
     * @since 1.0.0
     */
    @NotNull
    static ConditionContext empty() {
        return new Impl(null, null, null, Collections.emptyMap(), null, ZoneId.systemDefault());
    }

    /**
     * Creates a new builder for constructing contexts.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new Builder();
    }

    // ==================== Implementation ====================

    /**
     * Internal sealed implementation of ConditionContext.
     */
    final class Impl implements ConditionContext {
        private final @Nullable UnifiedPlayer player;
        private final @Nullable UnifiedLocation location;
        private final @Nullable UnifiedWorld world;
        private final @NotNull Map<String, Object> metadata;
        private final @NotNull Instant timestamp;
        private final @NotNull UUID contextId;
        private final @NotNull ZoneId timeZone;

        Impl(
                @Nullable UnifiedPlayer player,
                @Nullable UnifiedLocation location,
                @Nullable UnifiedWorld world,
                @NotNull Map<String, Object> metadata,
                @Nullable UUID contextId,
                @NotNull ZoneId timeZone
        ) {
            this.player = player;
            this.location = location;
            this.world = world;
            this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
            this.timestamp = Instant.now();
            this.contextId = contextId != null ? contextId : UUID.randomUUID();
            this.timeZone = timeZone;
        }

        @Override
        public @NotNull Optional<UnifiedPlayer> getPlayer() {
            return Optional.ofNullable(player);
        }

        @Override
        public @NotNull Optional<UnifiedLocation> getLocation() {
            if (location != null) {
                return Optional.of(location);
            }
            if (player != null) {
                return Optional.of(player.getLocation());
            }
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<UnifiedWorld> getWorld() {
            if (world != null) {
                return Optional.of(world);
            }
            if (player != null) {
                return Optional.of(player.getWorld());
            }
            if (location != null) {
                return location.getWorld();
            }
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<String> getWorldName() {
            return getWorld().map(UnifiedWorld::getName);
        }

        @Override
        public @NotNull Optional<UUID> getPlayerId() {
            return getPlayer().map(UnifiedPlayer::getUniqueId);
        }

        @Override
        public @NotNull Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public @NotNull ZoneId getTimeZone() {
            return timeZone;
        }

        @Override
        public @NotNull UUID getContextId() {
            return contextId;
        }

        @Override
        public @NotNull Optional<Object> getMetadata(@NotNull String key) {
            return Optional.ofNullable(metadata.get(key));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> @NotNull Optional<T> getMetadata(@NotNull String key, @NotNull Class<T> type) {
            Object value = metadata.get(key);
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        }

        @Override
        public @NotNull Map<String, Object> getAllMetadata() {
            return metadata;
        }

        @Override
        public boolean hasPlayer() {
            return player != null;
        }

        @Override
        public boolean hasLocation() {
            return getLocation().isPresent();
        }

        @Override
        public boolean hasWorld() {
            return getWorld().isPresent();
        }

        @Override
        public @NotNull ConditionContext withMetadata(@NotNull String key, @NotNull Object value) {
            Map<String, Object> newMetadata = new HashMap<>(this.metadata);
            newMetadata.put(key, value);
            return new Impl(player, location, world, newMetadata, contextId, timeZone);
        }

        @Override
        public @NotNull ConditionContext withPlayer(@Nullable UnifiedPlayer player) {
            return new Impl(player, location, world, metadata, contextId, timeZone);
        }

        @Override
        public @NotNull ConditionContext withLocation(@Nullable UnifiedLocation location) {
            return new Impl(player, location, world, metadata, contextId, timeZone);
        }

        @Override
        public @NotNull ConditionContext withWorld(@Nullable UnifiedWorld world) {
            return new Impl(player, location, world, metadata, contextId, timeZone);
        }

        @Override
        public @NotNull ConditionContext withTimeZone(@NotNull ZoneId timeZone) {
            return new Impl(player, location, world, metadata, contextId, timeZone);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Impl impl = (Impl) o;
            return Objects.equals(contextId, impl.contextId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextId);
        }

        @Override
        public String toString() {
            return "ConditionContext{" +
                    "player=" + (player != null ? player.getName() : "null") +
                    ", world=" + getWorldName().orElse("null") +
                    ", hasLocation=" + hasLocation() +
                    ", metadata=" + metadata.size() + " entries" +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    /**
     * Builder for creating {@link ConditionContext} instances.
     *
     * @since 1.0.0
     */
    final class Builder {
        private UnifiedPlayer player;
        private UnifiedLocation location;
        private UnifiedWorld world;
        private final Map<String, Object> metadata = new HashMap<>();
        private UUID contextId;
        private ZoneId timeZone = ZoneId.systemDefault();

        Builder() {}

        /**
         * Sets the player.
         *
         * @param player the player
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder player(@Nullable UnifiedPlayer player) {
            this.player = player;
            return this;
        }

        /**
         * Sets the location.
         *
         * @param location the location
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder location(@Nullable UnifiedLocation location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the world.
         *
         * @param world the world
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder world(@Nullable UnifiedWorld world) {
            this.world = world;
            return this;
        }

        /**
         * Adds metadata.
         *
         * @param key   the key
         * @param value the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder metadata(@NotNull String key, @NotNull Object value) {
            Objects.requireNonNull(key, "key cannot be null");
            Objects.requireNonNull(value, "value cannot be null");
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Adds all metadata from a map.
         *
         * @param metadata the metadata map
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder metadata(@NotNull Map<String, Object> metadata) {
            Objects.requireNonNull(metadata, "metadata cannot be null");
            this.metadata.putAll(metadata);
            return this;
        }

        /**
         * Sets a custom context key.
         *
         * @param key   the key
         * @param value the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder custom(@NotNull String key, @NotNull Object value) {
            return metadata(key, value);
        }

        /**
         * Sets the context ID.
         *
         * @param contextId the context ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder contextId(@Nullable UUID contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets the time zone.
         *
         * @param timeZone the time zone
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder timeZone(@NotNull ZoneId timeZone) {
            this.timeZone = Objects.requireNonNull(timeZone, "timeZone cannot be null");
            return this;
        }

        /**
         * Copies values from an existing context.
         *
         * @param context the context to copy from
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder from(@NotNull ConditionContext context) {
            Objects.requireNonNull(context, "context cannot be null");
            if (context instanceof Impl impl) {
                this.player = impl.player;
                this.location = impl.location;
                this.world = impl.world;
                this.metadata.putAll(impl.metadata);
                this.contextId = impl.contextId;
                this.timeZone = impl.timeZone;
            }
            return this;
        }

        /**
         * Builds the context.
         *
         * @return the built context
         * @since 1.0.0
         */
        @NotNull
        public ConditionContext build() {
            return new Impl(player, location, world, metadata, contextId, timeZone);
        }
    }
}
