/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.target;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Controller for managing entity target selection.
 *
 * <p>The TargetSelector manages which entities a mob will target for
 * attack or other interactions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * TargetSelector targets = ai.getTargetSelector(entity);
 *
 * // Clear vanilla targeting
 * targets.clearTargets();
 *
 * // Add custom target rules
 * targets.addTarget(1, new NearestAttackableTargetGoal(entity, Player.class)
 *     .filter(player -> !player.hasPermission("myPlugin.npc.ignore"))
 *     .range(16.0)
 * );
 *
 * targets.addTarget(2, new HurtByTargetGoal(entity)
 *     .alertOthers(Zombie.class)
 * );
 *
 * // Custom target selector
 * targets.addTarget(0, new CustomTargetGoal(entity) {
 *     @Override
 *     public LivingEntity findTarget() {
 *         return getNearbyPlayers(16.0).stream()
 *             .filter(p -> getReputation(p) < 0)
 *             .min(Comparator.comparingDouble(this::distanceTo))
 *             .orElse(null);
 *     }
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TargetGoal
 */
public interface TargetSelector {

    /**
     * Returns the entity this selector manages.
     *
     * @param <T> the entity type
     * @return the entity
     * @since 1.0.0
     */
    @NotNull
    <T> T getEntity();

    /**
     * Gets the current target.
     *
     * @param <T> the entity type
     * @return the current target, or null if none
     * @since 1.0.0
     */
    @Nullable
    <T> T getTarget();

    /**
     * Sets the current target manually.
     *
     * @param target the target entity
     * @since 1.0.0
     */
    void setTarget(@Nullable Object target);

    /**
     * Clears the current target.
     *
     * @since 1.0.0
     */
    void clearTarget();

    /**
     * Checks if the entity has a target.
     *
     * @return true if a target exists
     * @since 1.0.0
     */
    boolean hasTarget();

    // =========================================================================
    // Target Goals
    // =========================================================================

    /**
     * Adds a target goal with priority.
     *
     * @param priority the goal priority
     * @param goal     the target goal
     * @since 1.0.0
     */
    void addTarget(int priority, @NotNull TargetGoal goal);

    /**
     * Removes a target goal.
     *
     * @param goal the goal to remove
     * @return true if removed
     * @since 1.0.0
     */
    boolean removeTarget(@NotNull TargetGoal goal);

    /**
     * Removes all target goals of a specific type.
     *
     * @param goalClass the goal class
     * @return the number removed
     * @since 1.0.0
     */
    int removeTargetsOfType(@NotNull Class<? extends TargetGoal> goalClass);

    /**
     * Clears all target goals.
     *
     * @since 1.0.0
     */
    void clearTargets();

    /**
     * Gets all target goals.
     *
     * @return the target goals
     * @since 1.0.0
     */
    @NotNull
    Collection<TargetEntry> getTargetGoals();

    // =========================================================================
    // Target Filtering
    // =========================================================================

    /**
     * Adds a global target filter.
     *
     * <p>Entities that don't pass the filter will never be targeted.
     *
     * @param filter the filter predicate
     * @since 1.0.0
     */
    void addGlobalFilter(@NotNull Predicate<Object> filter);

    /**
     * Removes a global target filter.
     *
     * @param filter the filter to remove
     * @since 1.0.0
     */
    void removeGlobalFilter(@NotNull Predicate<Object> filter);

    /**
     * Clears all global filters.
     *
     * @since 1.0.0
     */
    void clearGlobalFilters();

    /**
     * Sets a blacklist of entity types.
     *
     * @param entityTypes the types to never target
     * @since 1.0.0
     */
    void setBlacklist(@NotNull Class<?>... entityTypes);

    /**
     * Adds to the blacklist.
     *
     * @param entityType the type to add
     * @since 1.0.0
     */
    void addToBlacklist(@NotNull Class<?> entityType);

    /**
     * Removes from the blacklist.
     *
     * @param entityType the type to remove
     * @since 1.0.0
     */
    void removeFromBlacklist(@NotNull Class<?> entityType);

    /**
     * Clears the blacklist.
     *
     * @since 1.0.0
     */
    void clearBlacklist();

    // =========================================================================
    // Target Information
    // =========================================================================

    /**
     * Gets the distance to the current target.
     *
     * @return the distance, or -1 if no target
     * @since 1.0.0
     */
    double getTargetDistance();

    /**
     * Checks if the current target is valid.
     *
     * @return true if valid
     * @since 1.0.0
     */
    boolean isTargetValid();

    /**
     * Checks if the target is in range for attack.
     *
     * @param range the attack range
     * @return true if in range
     * @since 1.0.0
     */
    boolean isTargetInRange(double range);

    /**
     * Checks if the target is visible.
     *
     * @return true if visible
     * @since 1.0.0
     */
    boolean canSeeTarget();

    /**
     * Gets nearby potential targets.
     *
     * @param <T>    the entity type
     * @param type   the target type class
     * @param range  the search range
     * @return nearby targets
     * @since 1.0.0
     */
    @NotNull
    <T> Collection<T> getNearbyTargets(@NotNull Class<T> type, double range);

    /**
     * Finds the nearest target of a type.
     *
     * @param <T>   the entity type
     * @param type  the target type class
     * @param range the search range
     * @return the nearest target, or empty if none
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> findNearest(@NotNull Class<T> type, double range);

    /**
     * Finds the nearest target matching a filter.
     *
     * @param <T>    the entity type
     * @param type   the target type class
     * @param range  the search range
     * @param filter the filter predicate
     * @return the nearest matching target
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> findNearest(@NotNull Class<T> type, double range, @NotNull Predicate<T> filter);

    /**
     * Represents a target goal entry.
     *
     * @since 1.0.0
     */
    interface TargetEntry {
        /**
         * Gets the target goal.
         *
         * @return the goal
         */
        @NotNull
        TargetGoal getGoal();

        /**
         * Gets the priority.
         *
         * @return the priority
         */
        int getPriority();
    }
}
