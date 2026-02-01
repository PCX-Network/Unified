/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Thread-safe cooldown manager for tracking cooldowns on entities.
 *
 * <p>This class provides a flexible system for managing cooldowns identified
 * by an entity key (typically a UUID) and a cooldown name. Cooldowns automatically
 * expire and can be queried for remaining time.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * CooldownManager cooldowns = new CooldownManager();
 *
 * // Set a cooldown
 * cooldowns.set(player.getUniqueId(), "ability", Duration.ofSeconds(30));
 *
 * // Check if on cooldown
 * if (cooldowns.isOnCooldown(player.getUniqueId(), "ability")) {
 *     Duration remaining = cooldowns.getRemaining(player.getUniqueId(), "ability");
 *     player.sendMessage("Cooldown: " + remaining.getSeconds() + "s remaining");
 *     return;
 * }
 *
 * // Use the ability
 * doAbility();
 * }</pre>
 *
 * <h2>With Players</h2>
 * <pre>{@code
 * // Using with UnifiedPlayer
 * cooldowns.set(player, "teleport", Duration.ofMinutes(5));
 *
 * // Check with callback
 * cooldowns.check(player, "teleport", () -> {
 *     player.teleport(spawn);
 * }, remaining -> {
 *     player.sendMessage("Teleport on cooldown for " + remaining.getSeconds() + "s");
 * });
 * }</pre>
 *
 * <h2>Cleanup</h2>
 * <pre>{@code
 * // Remove all cooldowns for a player (on disconnect)
 * cooldowns.clearAll(player.getUniqueId());
 *
 * // Cleanup expired entries periodically
 * cooldowns.cleanupExpired();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe. All operations are atomic and can be
 * called from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class CooldownManager {

    /**
     * Nested map: entityId -> (cooldownName -> expirationTime)
     */
    private final Map<UUID, Map<String, Instant>> cooldowns;

    /**
     * Creates a new CooldownManager instance.
     *
     * @since 1.0.0
     */
    public CooldownManager() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    // ==================== Setting Cooldowns ====================

    /**
     * Sets a cooldown for the specified entity and cooldown name.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @param duration     the cooldown duration
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    public void set(@NotNull UUID entityId, @NotNull String cooldownName, @NotNull Duration duration) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        Objects.requireNonNull(cooldownName, "cooldownName cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");

        if (duration.isNegative() || duration.isZero()) {
            remove(entityId, cooldownName);
            return;
        }

        Instant expiration = Instant.now().plus(duration);
        cooldowns.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>())
                .put(cooldownName.toLowerCase(), expiration);
    }

    /**
     * Sets a cooldown for the specified entity and cooldown name.
     *
     * <p>This overload accepts an object that provides a UUID (such as a Player).
     *
     * @param entity       the entity (must have a getUniqueId() method conceptually)
     * @param cooldownName the name of the cooldown
     * @param duration     the cooldown duration
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    public void set(@NotNull HasUniqueId entity, @NotNull String cooldownName, @NotNull Duration duration) {
        Objects.requireNonNull(entity, "entity cannot be null");
        set(entity.getUniqueId(), cooldownName, duration);
    }

    /**
     * Sets a cooldown using seconds.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @param seconds      the cooldown duration in seconds
     * @since 1.0.0
     */
    public void setSeconds(@NotNull UUID entityId, @NotNull String cooldownName, long seconds) {
        set(entityId, cooldownName, Duration.ofSeconds(seconds));
    }

    /**
     * Sets a cooldown using ticks (20 ticks = 1 second).
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @param ticks        the cooldown duration in ticks
     * @since 1.0.0
     */
    public void setTicks(@NotNull UUID entityId, @NotNull String cooldownName, long ticks) {
        set(entityId, cooldownName, Duration.ofMillis(ticks * 50));
    }

    // ==================== Checking Cooldowns ====================

    /**
     * Checks if the specified entity is on cooldown.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return true if on cooldown, false otherwise
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    public boolean isOnCooldown(@NotNull UUID entityId, @NotNull String cooldownName) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        Objects.requireNonNull(cooldownName, "cooldownName cannot be null");

        Map<String, Instant> entityCooldowns = cooldowns.get(entityId);
        if (entityCooldowns == null) {
            return false;
        }

        Instant expiration = entityCooldowns.get(cooldownName.toLowerCase());
        if (expiration == null) {
            return false;
        }

        if (Instant.now().isAfter(expiration)) {
            entityCooldowns.remove(cooldownName.toLowerCase());
            return false;
        }

        return true;
    }

    /**
     * Checks if the specified entity is on cooldown.
     *
     * @param entity       the entity
     * @param cooldownName the name of the cooldown
     * @return true if on cooldown, false otherwise
     * @since 1.0.0
     */
    public boolean isOnCooldown(@NotNull HasUniqueId entity, @NotNull String cooldownName) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return isOnCooldown(entity.getUniqueId(), cooldownName);
    }

    /**
     * Checks if the entity is NOT on cooldown (ready to use ability).
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return true if NOT on cooldown, false otherwise
     * @since 1.0.0
     */
    public boolean isReady(@NotNull UUID entityId, @NotNull String cooldownName) {
        return !isOnCooldown(entityId, cooldownName);
    }

    /**
     * Checks if the entity is NOT on cooldown (ready to use ability).
     *
     * @param entity       the entity
     * @param cooldownName the name of the cooldown
     * @return true if NOT on cooldown, false otherwise
     * @since 1.0.0
     */
    public boolean isReady(@NotNull HasUniqueId entity, @NotNull String cooldownName) {
        return !isOnCooldown(entity, cooldownName);
    }

    // ==================== Getting Remaining Time ====================

    /**
     * Gets the remaining cooldown duration.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return the remaining duration, or {@link Duration#ZERO} if not on cooldown
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    public Duration getRemaining(@NotNull UUID entityId, @NotNull String cooldownName) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        Objects.requireNonNull(cooldownName, "cooldownName cannot be null");

        Map<String, Instant> entityCooldowns = cooldowns.get(entityId);
        if (entityCooldowns == null) {
            return Duration.ZERO;
        }

        Instant expiration = entityCooldowns.get(cooldownName.toLowerCase());
        if (expiration == null) {
            return Duration.ZERO;
        }

        Duration remaining = Duration.between(Instant.now(), expiration);
        if (remaining.isNegative() || remaining.isZero()) {
            entityCooldowns.remove(cooldownName.toLowerCase());
            return Duration.ZERO;
        }

        return remaining;
    }

    /**
     * Gets the remaining cooldown duration.
     *
     * @param entity       the entity
     * @param cooldownName the name of the cooldown
     * @return the remaining duration, or {@link Duration#ZERO} if not on cooldown
     * @since 1.0.0
     */
    @NotNull
    public Duration getRemaining(@NotNull HasUniqueId entity, @NotNull String cooldownName) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return getRemaining(entity.getUniqueId(), cooldownName);
    }

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return the remaining seconds, or 0 if not on cooldown
     * @since 1.0.0
     */
    public long getRemainingSeconds(@NotNull UUID entityId, @NotNull String cooldownName) {
        return getRemaining(entityId, cooldownName).toSeconds();
    }

    /**
     * Gets the remaining cooldown time in ticks.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return the remaining ticks, or 0 if not on cooldown
     * @since 1.0.0
     */
    public long getRemainingTicks(@NotNull UUID entityId, @NotNull String cooldownName) {
        return getRemaining(entityId, cooldownName).toMillis() / 50;
    }

    /**
     * Gets the remaining cooldown as an Optional.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return an Optional containing the remaining duration, or empty if not on cooldown
     * @since 1.0.0
     */
    @NotNull
    public Optional<Duration> getRemainingOptional(@NotNull UUID entityId, @NotNull String cooldownName) {
        Duration remaining = getRemaining(entityId, cooldownName);
        return remaining.isZero() ? Optional.empty() : Optional.of(remaining);
    }

    /**
     * Gets the expiration instant for a cooldown.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return an Optional containing the expiration instant, or empty if not on cooldown
     * @since 1.0.0
     */
    @NotNull
    public Optional<Instant> getExpiration(@NotNull UUID entityId, @NotNull String cooldownName) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        Objects.requireNonNull(cooldownName, "cooldownName cannot be null");

        Map<String, Instant> entityCooldowns = cooldowns.get(entityId);
        if (entityCooldowns == null) {
            return Optional.empty();
        }

        Instant expiration = entityCooldowns.get(cooldownName.toLowerCase());
        if (expiration == null || Instant.now().isAfter(expiration)) {
            return Optional.empty();
        }

        return Optional.of(expiration);
    }

    // ==================== Callback-based Checks ====================

    /**
     * Checks cooldown and executes the appropriate callback.
     *
     * @param entityId       the entity's UUID
     * @param cooldownName   the name of the cooldown
     * @param onReady        callback to execute if not on cooldown
     * @param onCooldown     callback to execute if on cooldown (receives remaining duration)
     * @return true if the action was executed (not on cooldown)
     * @since 1.0.0
     */
    public boolean check(@NotNull UUID entityId, @NotNull String cooldownName,
                         @NotNull Runnable onReady, @NotNull Consumer<Duration> onCooldown) {
        Objects.requireNonNull(onReady, "onReady cannot be null");
        Objects.requireNonNull(onCooldown, "onCooldown cannot be null");

        Duration remaining = getRemaining(entityId, cooldownName);
        if (remaining.isZero()) {
            onReady.run();
            return true;
        } else {
            onCooldown.accept(remaining);
            return false;
        }
    }

    /**
     * Checks cooldown and executes the appropriate callback.
     *
     * @param entity         the entity
     * @param cooldownName   the name of the cooldown
     * @param onReady        callback to execute if not on cooldown
     * @param onCooldown     callback to execute if on cooldown
     * @return true if the action was executed
     * @since 1.0.0
     */
    public boolean check(@NotNull HasUniqueId entity, @NotNull String cooldownName,
                         @NotNull Runnable onReady, @NotNull Consumer<Duration> onCooldown) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return check(entity.getUniqueId(), cooldownName, onReady, onCooldown);
    }

    /**
     * Checks cooldown and executes the action if ready.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @param onReady      callback to execute if not on cooldown
     * @return true if the action was executed
     * @since 1.0.0
     */
    public boolean checkAndRun(@NotNull UUID entityId, @NotNull String cooldownName,
                               @NotNull Runnable onReady) {
        if (isReady(entityId, cooldownName)) {
            onReady.run();
            return true;
        }
        return false;
    }

    /**
     * Checks cooldown, executes the action if ready, and sets a new cooldown.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @param duration     the cooldown duration to set after execution
     * @param action       the action to execute
     * @return true if the action was executed
     * @since 1.0.0
     */
    public boolean useWithCooldown(@NotNull UUID entityId, @NotNull String cooldownName,
                                   @NotNull Duration duration, @NotNull Runnable action) {
        if (isReady(entityId, cooldownName)) {
            action.run();
            set(entityId, cooldownName, duration);
            return true;
        }
        return false;
    }

    // ==================== Removing Cooldowns ====================

    /**
     * Removes a specific cooldown for an entity.
     *
     * @param entityId     the entity's UUID
     * @param cooldownName the name of the cooldown
     * @return true if a cooldown was removed
     * @since 1.0.0
     */
    public boolean remove(@NotNull UUID entityId, @NotNull String cooldownName) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        Objects.requireNonNull(cooldownName, "cooldownName cannot be null");

        Map<String, Instant> entityCooldowns = cooldowns.get(entityId);
        if (entityCooldowns == null) {
            return false;
        }

        boolean removed = entityCooldowns.remove(cooldownName.toLowerCase()) != null;
        if (entityCooldowns.isEmpty()) {
            cooldowns.remove(entityId);
        }
        return removed;
    }

    /**
     * Removes a specific cooldown for an entity.
     *
     * @param entity       the entity
     * @param cooldownName the name of the cooldown
     * @return true if a cooldown was removed
     * @since 1.0.0
     */
    public boolean remove(@NotNull HasUniqueId entity, @NotNull String cooldownName) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return remove(entity.getUniqueId(), cooldownName);
    }

    /**
     * Removes all cooldowns for an entity.
     *
     * @param entityId the entity's UUID
     * @return the number of cooldowns removed
     * @since 1.0.0
     */
    public int clearAll(@NotNull UUID entityId) {
        Objects.requireNonNull(entityId, "entityId cannot be null");

        Map<String, Instant> removed = cooldowns.remove(entityId);
        return removed != null ? removed.size() : 0;
    }

    /**
     * Removes all cooldowns for an entity.
     *
     * @param entity the entity
     * @return the number of cooldowns removed
     * @since 1.0.0
     */
    public int clearAll(@NotNull HasUniqueId entity) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return clearAll(entity.getUniqueId());
    }

    /**
     * Clears all cooldowns for all entities.
     *
     * @since 1.0.0
     */
    public void clearAll() {
        cooldowns.clear();
    }

    // ==================== Query Methods ====================

    /**
     * Gets all active cooldown names for an entity.
     *
     * @param entityId the entity's UUID
     * @return a set of active cooldown names
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getActiveCooldowns(@NotNull UUID entityId) {
        Objects.requireNonNull(entityId, "entityId cannot be null");

        Map<String, Instant> entityCooldowns = cooldowns.get(entityId);
        if (entityCooldowns == null) {
            return Set.of();
        }

        Instant now = Instant.now();
        return entityCooldowns.entrySet().stream()
                .filter(e -> now.isBefore(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets all entity UUIDs that have the specified cooldown active.
     *
     * @param cooldownName the name of the cooldown
     * @return a collection of entity UUIDs with this cooldown active
     * @since 1.0.0
     */
    @NotNull
    public Collection<UUID> getEntitiesOnCooldown(@NotNull String cooldownName) {
        Objects.requireNonNull(cooldownName, "cooldownName cannot be null");

        String normalizedName = cooldownName.toLowerCase();
        Instant now = Instant.now();

        return cooldowns.entrySet().stream()
                .filter(entry -> {
                    Instant expiration = entry.getValue().get(normalizedName);
                    return expiration != null && now.isBefore(expiration);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Gets the total number of active cooldowns across all entities.
     *
     * @return the total count of active cooldowns
     * @since 1.0.0
     */
    public int getActiveCount() {
        Instant now = Instant.now();
        return cooldowns.values().stream()
                .mapToInt(map -> (int) map.values().stream()
                        .filter(now::isBefore)
                        .count())
                .sum();
    }

    /**
     * Gets the number of entities with active cooldowns.
     *
     * @return the number of entities
     * @since 1.0.0
     */
    public int getEntityCount() {
        cleanupExpired();
        return cooldowns.size();
    }

    // ==================== Maintenance ====================

    /**
     * Removes all expired cooldowns from memory.
     *
     * <p>This is called automatically during checks, but can be called
     * manually for maintenance.
     *
     * @return the number of expired entries removed
     * @since 1.0.0
     */
    public int cleanupExpired() {
        Instant now = Instant.now();
        int removed = 0;

        var iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Map<String, Instant> entityCooldowns = entry.getValue();

            var cooldownIterator = entityCooldowns.entrySet().iterator();
            while (cooldownIterator.hasNext()) {
                if (now.isAfter(cooldownIterator.next().getValue())) {
                    cooldownIterator.remove();
                    removed++;
                }
            }

            if (entityCooldowns.isEmpty()) {
                iterator.remove();
            }
        }

        return removed;
    }

    /**
     * Interface for objects that have a unique ID.
     *
     * <p>This allows the CooldownManager to work with any entity type that
     * has a UUID, not just Bukkit Players.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface HasUniqueId {
        /**
         * Gets the unique ID of this entity.
         *
         * @return the UUID
         */
        @NotNull UUID getUniqueId();
    }
}
