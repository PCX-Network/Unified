/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.world;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Mock implementation of a Minecraft entity for testing purposes.
 *
 * <p>MockEntity represents an entity in a MockWorld and tracks its type,
 * location, and state for testing entity-related functionality.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockWorld world = server.getWorld("world");
 * UnifiedLocation location = new UnifiedLocation(world, 100, 64, 100);
 *
 * // Spawn entity
 * MockEntity zombie = world.spawnEntity(location, "minecraft:zombie");
 *
 * // Check entity state
 * assertThat(zombie.getType()).isEqualTo("minecraft:zombie");
 * assertThat(zombie.isAlive()).isTrue();
 *
 * // Remove entity
 * zombie.remove();
 * assertThat(zombie.isDead()).isTrue();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockWorld
 */
public final class MockEntity {

    private final MockWorld world;
    private final String type;
    private final UUID uuid;
    private UnifiedLocation location;
    private String customName;
    private boolean customNameVisible = false;
    private boolean alive = true;
    private boolean removed = false;
    private boolean persistent = true;
    private boolean glowing = false;
    private boolean silent = false;
    private boolean gravity = true;
    private boolean invulnerable = false;
    private int fireTicks = 0;
    private int freezeTicks = 0;
    private double health = 20.0;
    private double maxHealth = 20.0;

    /**
     * Creates a new mock entity.
     *
     * @param world    the containing world
     * @param type     the entity type
     * @param location the spawn location
     */
    public MockEntity(
        @NotNull MockWorld world,
        @NotNull String type,
        @NotNull UnifiedLocation location
    ) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.uuid = UUID.randomUUID();
    }

    /**
     * Returns the entity type.
     *
     * @return the entity type (e.g., "minecraft:zombie")
     */
    @NotNull
    public String getType() {
        return type;
    }

    /**
     * Returns the entity's unique ID.
     *
     * @return the UUID
     */
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Returns the entity's current location.
     *
     * @return the location
     */
    @NotNull
    public UnifiedLocation getLocation() {
        return location;
    }

    /**
     * Sets the entity's location (teleport).
     *
     * @param location the new location
     */
    public void setLocation(@NotNull UnifiedLocation location) {
        this.location = Objects.requireNonNull(location);
    }

    /**
     * Teleports the entity to a new location.
     *
     * @param location the destination
     * @return true if teleport was successful
     */
    public boolean teleport(@NotNull UnifiedLocation location) {
        if (!alive || removed) {
            return false;
        }
        this.location = Objects.requireNonNull(location);
        return true;
    }

    /**
     * Returns the world this entity is in.
     *
     * @return the world
     */
    @NotNull
    public MockWorld getWorld() {
        return world;
    }

    /**
     * Returns the custom name of this entity.
     *
     * @return the custom name, or null if not set
     */
    @Nullable
    public String getCustomName() {
        return customName;
    }

    /**
     * Sets the custom name of this entity.
     *
     * @param name the custom name
     */
    public void setCustomName(@Nullable String name) {
        this.customName = name;
    }

    /**
     * Returns whether the custom name is visible.
     *
     * @return true if visible
     */
    public boolean isCustomNameVisible() {
        return customNameVisible;
    }

    /**
     * Sets whether the custom name is visible.
     *
     * @param visible the visibility
     */
    public void setCustomNameVisible(boolean visible) {
        this.customNameVisible = visible;
    }

    /**
     * Returns whether this entity is alive.
     *
     * @return true if alive
     */
    public boolean isAlive() {
        return alive && !removed;
    }

    /**
     * Returns whether this entity is dead.
     *
     * @return true if dead
     */
    public boolean isDead() {
        return !alive;
    }

    /**
     * Returns whether this entity has been removed.
     *
     * @return true if removed
     */
    public boolean isRemoved() {
        return removed;
    }

    /**
     * Removes this entity from the world.
     */
    public void remove() {
        if (!removed) {
            removed = true;
            alive = false;
            world.removeEntity(this);
        }
    }

    /**
     * Returns whether this entity is persistent.
     *
     * @return true if persistent
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Sets whether this entity is persistent.
     *
     * @param persistent the persistence
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * Returns whether this entity is glowing.
     *
     * @return true if glowing
     */
    public boolean isGlowing() {
        return glowing;
    }

    /**
     * Sets whether this entity is glowing.
     *
     * @param glowing the glowing state
     */
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    /**
     * Returns whether this entity is silent.
     *
     * @return true if silent
     */
    public boolean isSilent() {
        return silent;
    }

    /**
     * Sets whether this entity is silent.
     *
     * @param silent the silent state
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    /**
     * Returns whether this entity has gravity.
     *
     * @return true if has gravity
     */
    public boolean hasGravity() {
        return gravity;
    }

    /**
     * Sets whether this entity has gravity.
     *
     * @param gravity the gravity state
     */
    public void setGravity(boolean gravity) {
        this.gravity = gravity;
    }

    /**
     * Returns whether this entity is invulnerable.
     *
     * @return true if invulnerable
     */
    public boolean isInvulnerable() {
        return invulnerable;
    }

    /**
     * Sets whether this entity is invulnerable.
     *
     * @param invulnerable the invulnerability state
     */
    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    /**
     * Returns the fire ticks remaining.
     *
     * @return the fire ticks
     */
    public int getFireTicks() {
        return fireTicks;
    }

    /**
     * Sets the fire ticks.
     *
     * @param ticks the fire ticks
     */
    public void setFireTicks(int ticks) {
        this.fireTicks = Math.max(0, ticks);
    }

    /**
     * Returns the freeze ticks.
     *
     * @return the freeze ticks
     */
    public int getFreezeTicks() {
        return freezeTicks;
    }

    /**
     * Sets the freeze ticks.
     *
     * @param ticks the freeze ticks
     */
    public void setFreezeTicks(int ticks) {
        this.freezeTicks = Math.max(0, ticks);
    }

    /**
     * Returns whether this entity is on fire.
     *
     * @return true if on fire
     */
    public boolean isOnFire() {
        return fireTicks > 0;
    }

    /**
     * Returns the entity's health.
     *
     * @return the health
     */
    public double getHealth() {
        return health;
    }

    /**
     * Sets the entity's health.
     *
     * @param health the health
     */
    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
        if (this.health <= 0) {
            this.alive = false;
        }
    }

    /**
     * Returns the entity's maximum health.
     *
     * @return the max health
     */
    public double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Sets the entity's maximum health.
     *
     * @param maxHealth the max health
     */
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = Math.max(0, maxHealth);
    }

    /**
     * Damages this entity.
     *
     * @param amount the damage amount
     */
    public void damage(double amount) {
        if (!invulnerable && alive) {
            setHealth(health - amount);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockEntity that = (MockEntity) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return "MockEntity{" +
            "type='" + type + '\'' +
            ", uuid=" + uuid +
            ", alive=" + alive +
            '}';
    }
}
