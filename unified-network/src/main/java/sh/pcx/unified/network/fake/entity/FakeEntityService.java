/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.fake.entity;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for creating and managing client-side fake entities.
 *
 * <p>Fake entities are rendered on the client but don't exist on the server.
 * They are perfect for holograms, NPCs, visual effects, and other client-side
 * elements.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private FakeEntityService fakeEntities;
 *
 * // Spawn a hologram
 * FakeEntity hologram = fakeEntities.spawn(player, EntityType.ARMOR_STAND, location)
 *     .invisible(true)
 *     .customName(Component.text("Hologram Text"))
 *     .customNameVisible(true)
 *     .marker(true)
 *     .build();
 *
 * // Spawn an NPC
 * FakePlayer npc = fakeEntities.spawnPlayer(player, location)
 *     .name("Steve")
 *     .skin(skinTexture, skinSignature)
 *     .listed(false)
 *     .build();
 *
 * // Find entities
 * Optional<FakeEntity> entity = fakeEntities.getById(entityId);
 * Collection<FakeEntity> nearby = fakeEntities.getNearby(location, 10.0);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This service is thread-safe for lookups but entity modifications
 * should be done on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FakeEntity
 * @see FakePlayer
 */
public interface FakeEntityService extends Service {

    /**
     * Creates a builder for spawning a fake entity.
     *
     * @param viewer   the player to spawn for
     * @param type     the entity type
     * @param location the spawn location
     * @return a builder for configuring the entity
     * @since 1.0.0
     */
    @NotNull
    FakeEntityBuilder spawn(@NotNull Object viewer, @NotNull Object type, @NotNull Object location);

    /**
     * Creates a builder for spawning a fake entity for multiple viewers.
     *
     * @param viewers  the players to spawn for
     * @param type     the entity type
     * @param location the spawn location
     * @return a builder for configuring the entity
     * @since 1.0.0
     */
    @NotNull
    FakeEntityBuilder spawnForAll(@NotNull Collection<?> viewers, @NotNull Object type, @NotNull Object location);

    /**
     * Creates a builder for spawning a fake player (NPC).
     *
     * @param viewer   the player to spawn for
     * @param location the spawn location
     * @return a builder for configuring the player
     * @since 1.0.0
     */
    @NotNull
    FakePlayerBuilder spawnPlayer(@NotNull Object viewer, @NotNull Object location);

    /**
     * Creates a builder for spawning a fake player for multiple viewers.
     *
     * @param viewers  the players to spawn for
     * @param location the spawn location
     * @return a builder for configuring the player
     * @since 1.0.0
     */
    @NotNull
    FakePlayerBuilder spawnPlayerForAll(@NotNull Collection<?> viewers, @NotNull Object location);

    /**
     * Gets a fake entity by its entity ID.
     *
     * @param entityId the entity ID
     * @return the entity, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<FakeEntity> getById(int entityId);

    /**
     * Gets a fake entity by its UUID.
     *
     * @param uuid the entity UUID
     * @return the entity, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<FakeEntity> getByUuid(@NotNull UUID uuid);

    /**
     * Gets all fake entities visible to a player.
     *
     * @param playerId the player's UUID
     * @return the visible entities
     * @since 1.0.0
     */
    @NotNull
    Collection<FakeEntity> getVisibleTo(@NotNull UUID playerId);

    /**
     * Gets all fake entities near a location.
     *
     * @param location the center location
     * @param radius   the search radius
     * @return the nearby entities
     * @since 1.0.0
     */
    @NotNull
    Collection<FakeEntity> getNearby(@NotNull Object location, double radius);

    /**
     * Gets all registered fake entities.
     *
     * @return all fake entities
     * @since 1.0.0
     */
    @NotNull
    Collection<FakeEntity> getAll();

    /**
     * Destroys all fake entities.
     *
     * @since 1.0.0
     */
    void destroyAll();

    /**
     * Destroys all fake entities visible to a player.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void destroyAllFor(@NotNull UUID playerId);

    /**
     * Generates a unique entity ID for a fake entity.
     *
     * @return a unique entity ID
     * @since 1.0.0
     */
    int generateEntityId();

    /**
     * Builder for creating fake entities.
     *
     * @since 1.0.0
     */
    interface FakeEntityBuilder {
        /**
         * Sets the entity as invisible.
         *
         * @param invisible true for invisible
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder invisible(boolean invisible);

        /**
         * Sets the custom name.
         *
         * @param name the custom name component
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder customName(@Nullable Object name);

        /**
         * Sets custom name visibility.
         *
         * @param visible true to show name
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder customNameVisible(boolean visible);

        /**
         * Sets the entity as a marker (no hitbox, for armor stands).
         *
         * @param marker true for marker
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder marker(boolean marker);

        /**
         * Sets whether the entity is small (for armor stands).
         *
         * @param small true for small
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder small(boolean small);

        /**
         * Sets whether the entity has arms (for armor stands).
         *
         * @param arms true for arms
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder arms(boolean arms);

        /**
         * Sets whether the entity has no base plate (for armor stands).
         *
         * @param noBasePlate true for no base plate
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder noBasePlate(boolean noBasePlate);

        /**
         * Sets the entity as glowing.
         *
         * @param glowing true for glowing
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder glowing(boolean glowing);

        /**
         * Sets the entity on fire visually.
         *
         * @param onFire true for on fire
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder onFire(boolean onFire);

        /**
         * Disables gravity for this entity.
         *
         * @param noGravity true to disable gravity
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder noGravity(boolean noGravity);

        /**
         * Sets the entity as silent.
         *
         * @param silent true for silent
         * @return this builder
         */
        @NotNull
        FakeEntityBuilder silent(boolean silent);

        /**
         * Sets metadata on the entity.
         *
         * @param <T>   the value type
         * @param key   the metadata key
         * @param value the value
         * @return this builder
         */
        @NotNull
        <T> FakeEntityBuilder metadata(@NotNull EntityData<T> key, @Nullable T value);

        /**
         * Builds and spawns the entity.
         *
         * @return the created entity
         */
        @NotNull
        FakeEntity build();
    }

    /**
     * Builder for creating fake players (NPCs).
     *
     * @since 1.0.0
     */
    interface FakePlayerBuilder {
        /**
         * Sets the player name.
         *
         * @param name the display name
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder name(@NotNull String name);

        /**
         * Sets the player skin.
         *
         * @param texture   the texture value (Base64)
         * @param signature the texture signature
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder skin(@NotNull String texture, @NotNull String signature);

        /**
         * Sets the skin from another player.
         *
         * @param playerName the player name to copy skin from
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder skinFromPlayer(@NotNull String playerName);

        /**
         * Sets whether the player is listed in tab.
         *
         * @param listed true to list in tab
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder listed(boolean listed);

        /**
         * Sets the game mode appearance.
         *
         * @param gameMode the game mode (0-3)
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder gameMode(int gameMode);

        /**
         * Sets the latency bar appearance.
         *
         * @param latency latency in milliseconds
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder latency(int latency);

        /**
         * Sets equipment in a slot.
         *
         * @param slot the equipment slot
         * @param item the item
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder equipment(@NotNull FakePlayer.EquipmentSlot slot, @Nullable Object item);

        /**
         * Sets the skin parts displayed.
         *
         * @param parts the skin parts
         * @return this builder
         */
        @NotNull
        FakePlayerBuilder skinParts(@NotNull FakePlayer.SkinParts parts);

        /**
         * Builds and spawns the fake player.
         *
         * @return the created player
         */
        @NotNull
        FakePlayer build();
    }
}
