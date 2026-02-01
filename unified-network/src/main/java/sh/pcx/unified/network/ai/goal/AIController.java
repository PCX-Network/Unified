/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.goal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Controller for managing entity AI goals.
 *
 * <p>The AIController allows adding, removing, and manipulating AI goals
 * that control entity behavior. Goals are processed by priority, with
 * lower numbers being higher priority.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * AIController controller = ai.getController(entity);
 *
 * // Remove all vanilla goals
 * controller.clearGoals();
 *
 * // Remove specific goals
 * controller.removeGoal(VanillaGoal.RANDOM_STROLL);
 * controller.removeGoal(VanillaGoal.LOOK_AT_PLAYER);
 *
 * // Add custom goals
 * controller.addGoal(1, new FollowPlayerGoal(entity, targetPlayer)
 *     .speed(1.2)
 *     .minDistance(2.0)
 * );
 *
 * controller.addGoal(0, new MeleeAttackGoal(entity)
 *     .attackInterval(20)
 *     .attackDamage(5.0)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AIGoal
 * @see VanillaGoal
 */
public interface AIController {

    /**
     * Returns the entity this controller manages.
     *
     * @param <T> the entity type
     * @return the entity
     * @since 1.0.0
     */
    @NotNull
    <T> T getEntity();

    /**
     * Adds a goal with the specified priority.
     *
     * <p>Lower priority numbers are executed first.
     *
     * @param priority the goal priority
     * @param goal     the goal to add
     * @since 1.0.0
     */
    void addGoal(int priority, @NotNull AIGoal goal);

    /**
     * Removes a goal instance.
     *
     * @param goal the goal to remove
     * @return true if the goal was removed
     * @since 1.0.0
     */
    boolean removeGoal(@NotNull AIGoal goal);

    /**
     * Removes a vanilla goal by type.
     *
     * @param vanillaGoal the vanilla goal type
     * @return true if any goals were removed
     * @since 1.0.0
     */
    boolean removeGoal(@NotNull VanillaGoal vanillaGoal);

    /**
     * Removes all goals of a specific class.
     *
     * @param goalClass the goal class
     * @return the number of goals removed
     * @since 1.0.0
     */
    int removeGoalsOfType(@NotNull Class<? extends AIGoal> goalClass);

    /**
     * Removes all goals.
     *
     * @since 1.0.0
     */
    void clearGoals();

    /**
     * Gets all current goals.
     *
     * @return the goals with their priorities
     * @since 1.0.0
     */
    @NotNull
    Collection<GoalEntry> getGoals();

    /**
     * Gets the currently running goals.
     *
     * @return the active goals
     * @since 1.0.0
     */
    @NotNull
    Collection<AIGoal> getRunningGoals();

    /**
     * Checks if a goal is currently running.
     *
     * @param goal the goal to check
     * @return true if running
     * @since 1.0.0
     */
    boolean isRunning(@NotNull AIGoal goal);

    /**
     * Gets a goal by its class type.
     *
     * @param <T>       the goal type
     * @param goalClass the goal class
     * @return the goal, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    <T extends AIGoal> Optional<T> getGoal(@NotNull Class<T> goalClass);

    /**
     * Checks if a goal of a specific type exists.
     *
     * @param goalClass the goal class
     * @return true if a goal of this type exists
     * @since 1.0.0
     */
    boolean hasGoal(@NotNull Class<? extends AIGoal> goalClass);

    /**
     * Checks if a vanilla goal type exists.
     *
     * @param vanillaGoal the vanilla goal type
     * @return true if this goal exists
     * @since 1.0.0
     */
    boolean hasGoal(@NotNull VanillaGoal vanillaGoal);

    /**
     * Changes the priority of a goal.
     *
     * @param goal        the goal
     * @param newPriority the new priority
     * @return true if the priority was changed
     * @since 1.0.0
     */
    boolean setPriority(@NotNull AIGoal goal, int newPriority);

    /**
     * Gets the priority of a goal.
     *
     * @param goal the goal
     * @return the priority, or -1 if not found
     * @since 1.0.0
     */
    int getPriority(@NotNull AIGoal goal);

    /**
     * Temporarily disables a goal.
     *
     * @param goal the goal
     * @since 1.0.0
     */
    void disableGoal(@NotNull AIGoal goal);

    /**
     * Re-enables a disabled goal.
     *
     * @param goal the goal
     * @since 1.0.0
     */
    void enableGoal(@NotNull AIGoal goal);

    /**
     * Checks if a goal is enabled.
     *
     * @param goal the goal
     * @return true if enabled
     * @since 1.0.0
     */
    boolean isGoalEnabled(@NotNull AIGoal goal);

    /**
     * Forces a goal to start immediately.
     *
     * @param goal the goal
     * @since 1.0.0
     */
    void forceStart(@NotNull AIGoal goal);

    /**
     * Forces a goal to stop immediately.
     *
     * @param goal the goal
     * @since 1.0.0
     */
    void forceStop(@NotNull AIGoal goal);

    /**
     * Represents a goal with its priority.
     *
     * @since 1.0.0
     */
    interface GoalEntry {
        /**
         * Returns the goal.
         *
         * @return the goal
         */
        @NotNull
        AIGoal getGoal();

        /**
         * Returns the priority.
         *
         * @return the priority
         */
        int getPriority();

        /**
         * Checks if this goal is enabled.
         *
         * @return true if enabled
         */
        boolean isEnabled();
    }

    /**
     * Enumeration of vanilla AI goals.
     *
     * @since 1.0.0
     */
    enum VanillaGoal {
        // Movement Goals
        FLOAT,
        RANDOM_STROLL,
        RANDOM_STROLL_IN_VILLAGE,
        RANDOM_SWIMMING,
        RANDOM_FLYING,
        WATER_AVOIDING_RANDOM_STROLL,
        MOVE_THROUGH_VILLAGE,
        MOVE_TOWARDS_TARGET,
        MOVE_TOWARDS_RESTRICTION,
        MOVE_BACK_TO_VILLAGE,
        STROLL_THROUGH_VILLAGE,
        FOLLOW_OWNER,
        FOLLOW_PARENT,
        FOLLOW_MOB,
        FLEE_SUN,
        RESTRICT_SUN,
        PANIC,
        AVOID_ENTITY,

        // Look Goals
        LOOK_AT_PLAYER,
        LOOK_AT_TRADING_PLAYER,
        RANDOM_LOOK_AROUND,

        // Combat Goals
        MELEE_ATTACK,
        RANGED_ATTACK,
        RANGED_BOW_ATTACK,
        RANGED_CROSSBOW_ATTACK,
        LEAP_AT_TARGET,
        OCELOT_ATTACK,
        ZOMBIE_ATTACK,
        SPIDER_ATTACK,
        ENDERMAN_FREEZE_WHEN_LOOKED_AT,
        HURT_BY_TARGET,
        NEAREST_ATTACKABLE_TARGET,
        DEFEND_VILLAGE_TARGET,
        OWNER_HURT_BY_TARGET,
        OWNER_HURT_TARGET,

        // Interaction Goals
        TEMPT,
        BREED,
        OFFER_FLOWER,
        TRADE_WITH_PLAYER,
        INTERACT,
        DOOR_INTERACT,
        USE_ITEM,

        // Misc Goals
        OPEN_DOOR,
        BREAK_DOOR,
        SIT,
        BEG,
        EAT_BLOCK,
        RAID,
        HOLD_GROUND_ATTACK,
        SLEEP,
        WAKE_UP,

        // Unknown/Custom
        UNKNOWN
    }
}
