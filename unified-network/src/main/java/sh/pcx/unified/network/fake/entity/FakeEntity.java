/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.fake.entity;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a client-side fake entity.
 *
 * <p>Fake entities exist only on the client side and are not stored on the server.
 * They are useful for creating holograms, NPCs, and other visual elements.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a hologram
 * FakeEntity hologram = fakeEntities.spawn(player, EntityType.ARMOR_STAND, location)
 *     .invisible(true)
 *     .customName(Component.text("Hello World"))
 *     .customNameVisible(true)
 *     .marker(true)
 *     .build();
 *
 * // Update the hologram
 * hologram.updateMetadata(meta -> {
 *     meta.set(EntityData.CUSTOM_NAME, Component.text("Updated!"));
 * });
 *
 * // Move it
 * hologram.teleport(newLocation);
 *
 * // Destroy when done
 * hologram.destroy();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FakeEntityService
 * @see FakePlayer
 */
public interface FakeEntity {

    /**
     * Returns the entity ID.
     *
     * <p>This is the network entity ID used in packets.
     *
     * @return the entity ID
     * @since 1.0.0
     */
    int getEntityId();

    /**
     * Returns the unique ID of this fake entity.
     *
     * @return the entity UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getUniqueId();

    /**
     * Returns the entity type.
     *
     * @param <T> the EntityType class
     * @return the entity type
     * @since 1.0.0
     */
    @NotNull
    <T> T getType();

    /**
     * Returns the current location.
     *
     * @param <T> the Location type
     * @return the current location
     * @since 1.0.0
     */
    @NotNull
    <T> T getLocation();

    /**
     * Returns the players who can see this entity.
     *
     * @return the viewer UUIDs
     * @since 1.0.0
     */
    @NotNull
    Collection<UUID> getViewers();

    /**
     * Checks if a player can see this entity.
     *
     * @param playerId the player's UUID
     * @return true if the player can see this entity
     * @since 1.0.0
     */
    boolean isVisibleTo(@NotNull UUID playerId);

    /**
     * Shows this entity to a player.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void showTo(@NotNull UUID playerId);

    /**
     * Shows this entity to a player.
     *
     * @param player the player object
     * @since 1.0.0
     */
    void showTo(@NotNull Object player);

    /**
     * Hides this entity from a player.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void hideFrom(@NotNull UUID playerId);

    /**
     * Hides this entity from a player.
     *
     * @param player the player object
     * @since 1.0.0
     */
    void hideFrom(@NotNull Object player);

    /**
     * Teleports this entity to a new location.
     *
     * @param location the new location
     * @since 1.0.0
     */
    void teleport(@NotNull Object location);

    /**
     * Moves this entity relative to its current position.
     *
     * @param deltaX x offset
     * @param deltaY y offset
     * @param deltaZ z offset
     * @since 1.0.0
     */
    void move(double deltaX, double deltaY, double deltaZ);

    /**
     * Sets the entity's rotation.
     *
     * @param yaw   the yaw angle
     * @param pitch the pitch angle
     * @since 1.0.0
     */
    void setRotation(float yaw, float pitch);

    /**
     * Sets the entity's head rotation (for applicable entities).
     *
     * @param yaw the head yaw angle
     * @since 1.0.0
     */
    void setHeadRotation(float yaw);

    /**
     * Sets the entity's velocity.
     *
     * @param x x velocity
     * @param y y velocity
     * @param z z velocity
     * @since 1.0.0
     */
    void setVelocity(double x, double y, double z);

    /**
     * Updates entity metadata.
     *
     * @param updater the metadata updater
     * @since 1.0.0
     */
    void updateMetadata(@NotNull Consumer<EntityMetadata> updater);

    /**
     * Sets the custom name.
     *
     * @param name the custom name
     * @since 1.0.0
     */
    void setCustomName(@Nullable Component name);

    /**
     * Sets whether the custom name is visible.
     *
     * @param visible true to show the name
     * @since 1.0.0
     */
    void setCustomNameVisible(boolean visible);

    /**
     * Sets whether the entity is invisible.
     *
     * @param invisible true to make invisible
     * @since 1.0.0
     */
    void setInvisible(boolean invisible);

    /**
     * Sets whether the entity is glowing.
     *
     * @param glowing true to make glowing
     * @since 1.0.0
     */
    void setGlowing(boolean glowing);

    /**
     * Sets whether the entity is on fire.
     *
     * @param onFire true to set on fire
     * @since 1.0.0
     */
    void setOnFire(boolean onFire);

    /**
     * Sets whether the entity is sneaking.
     *
     * @param sneaking true to sneak
     * @since 1.0.0
     */
    void setSneaking(boolean sneaking);

    /**
     * Sets whether the entity is silent.
     *
     * @param silent true to make silent
     * @since 1.0.0
     */
    void setSilent(boolean silent);

    /**
     * Plays an animation on this entity.
     *
     * @param animation the animation type
     * @since 1.0.0
     */
    void playAnimation(@NotNull EntityAnimation animation);

    /**
     * Sets the entity as a passenger of another entity.
     *
     * @param vehicle the vehicle entity ID
     * @since 1.0.0
     */
    void setVehicle(int vehicle);

    /**
     * Removes this entity as a passenger.
     *
     * @since 1.0.0
     */
    void eject();

    /**
     * Adds a passenger to this entity.
     *
     * @param passenger the passenger entity ID
     * @since 1.0.0
     */
    void addPassenger(int passenger);

    /**
     * Removes a passenger from this entity.
     *
     * @param passenger the passenger entity ID
     * @since 1.0.0
     */
    void removePassenger(int passenger);

    /**
     * Checks if this entity is still spawned.
     *
     * @return true if spawned
     * @since 1.0.0
     */
    boolean isSpawned();

    /**
     * Respawns this entity for all viewers.
     *
     * @since 1.0.0
     */
    void respawn();

    /**
     * Destroys this entity, removing it from all clients.
     *
     * @since 1.0.0
     */
    void destroy();

    /**
     * Sets a click handler for this entity.
     *
     * @param handler the click handler
     * @since 1.0.0
     */
    void onClick(@Nullable Consumer<FakeEntityClickEvent> handler);

    /**
     * Returns the click handler if set.
     *
     * @return the click handler, or null if none
     * @since 1.0.0
     */
    @Nullable
    Consumer<FakeEntityClickEvent> getClickHandler();

    /**
     * Entity animation types.
     *
     * @since 1.0.0
     */
    enum EntityAnimation {
        SWING_MAIN_ARM,
        HURT,
        LEAVE_BED,
        SWING_OFF_HAND,
        CRITICAL_EFFECT,
        MAGIC_CRITICAL_EFFECT
    }

    /**
     * Event fired when a fake entity is clicked.
     *
     * @since 1.0.0
     */
    interface FakeEntityClickEvent {
        /**
         * Returns the player who clicked.
         *
         * @param <T> the player type
         * @return the player
         */
        @NotNull
        <T> T getPlayer();

        /**
         * Returns the player's UUID.
         *
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();

        /**
         * Returns the clicked entity.
         *
         * @return the fake entity
         */
        @NotNull
        FakeEntity getEntity();

        /**
         * Returns the click action.
         *
         * @return the action type
         */
        @NotNull
        ClickAction getAction();

        /**
         * Click action types.
         */
        enum ClickAction {
            LEFT_CLICK,
            RIGHT_CLICK,
            LEFT_CLICK_SHIFT,
            RIGHT_CLICK_SHIFT
        }
    }
}
