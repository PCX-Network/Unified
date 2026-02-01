package sh.pcx.unified.world.ai.goals;

import sh.pcx.unified.world.ai.core.AIController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Manages goal selection and priority for AI entities.
 *
 * <p>The GoalSelector is responsible for managing a collection of AI goals,
 * determining which goals should run based on priority and conditions,
 * and coordinating goal execution.</p>
 *
 * <h2>Goal Execution:</h2>
 * <ul>
 *   <li>Goals are evaluated based on priority each tick</li>
 *   <li>Higher priority goals can interrupt lower priority goals</li>
 *   <li>Goals with non-overlapping flags can run simultaneously</li>
 *   <li>The selector manages goal lifecycle (start, tick, stop)</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * GoalSelector selector = controller.getGoalSelector();
 *
 * // Add goals with priorities
 * selector.addGoal(new IdleGoal(), GoalPriority.LOWEST);
 * selector.addGoal(new WanderGoal(), GoalPriority.LOW);
 * selector.addGoal(new FollowEntityGoal(player), GoalPriority.NORMAL);
 * selector.addGoal(new AttackGoal(), GoalPriority.HIGH);
 * selector.addGoal(new FleeGoal(predator), GoalPriority.HIGHEST);
 *
 * // Remove goals
 * selector.removeGoal(idleGoal);
 *
 * // Clear all goals
 * selector.clearGoals();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see AIGoal
 * @see GoalPriority
 */
public interface GoalSelector {

    /**
     * Gets the AI controller this selector belongs to.
     *
     * @return the AI controller
     */
    @NotNull
    AIController getController();

    /**
     * Adds a goal with the specified priority.
     *
     * @param goal the goal to add
     * @param priority the priority level
     * @return this selector for chaining
     */
    @NotNull
    GoalSelector addGoal(@NotNull AIGoal goal, @NotNull GoalPriority priority);

    /**
     * Adds a goal with a custom priority value.
     *
     * @param goal the goal to add
     * @param priority the priority value (higher = more important)
     * @return this selector for chaining
     */
    @NotNull
    GoalSelector addGoal(@NotNull AIGoal goal, int priority);

    /**
     * Removes a goal from this selector.
     *
     * @param goal the goal to remove
     * @return true if the goal was removed
     */
    boolean removeGoal(@NotNull AIGoal goal);

    /**
     * Removes all goals matching the predicate.
     *
     * @param predicate the predicate to match goals
     * @return the number of goals removed
     */
    int removeGoals(@NotNull Predicate<AIGoal> predicate);

    /**
     * Removes all goals of the specified type.
     *
     * @param goalClass the goal class to remove
     * @return the number of goals removed
     */
    int removeGoals(@NotNull Class<? extends AIGoal> goalClass);

    /**
     * Clears all goals from this selector.
     */
    void clearGoals();

    /**
     * Gets all registered goals.
     *
     * @return an unmodifiable collection of all goals
     */
    @NotNull
    Collection<AIGoal> getGoals();

    /**
     * Gets all currently running goals.
     *
     * @return an unmodifiable collection of running goals
     */
    @NotNull
    Collection<AIGoal> getRunningGoals();

    /**
     * Gets the currently highest priority running goal.
     *
     * @return an optional containing the active goal, or empty if none
     */
    @NotNull
    Optional<AIGoal> getActiveGoal();

    /**
     * Checks if a specific goal is currently running.
     *
     * @param goal the goal to check
     * @return true if the goal is running
     */
    boolean isRunning(@NotNull AIGoal goal);

    /**
     * Checks if any goal of the specified type is running.
     *
     * @param goalClass the goal class to check
     * @return true if any matching goal is running
     */
    boolean isRunning(@NotNull Class<? extends AIGoal> goalClass);

    /**
     * Gets the priority of a specific goal.
     *
     * @param goal the goal to get priority for
     * @return an optional containing the priority, or empty if not registered
     */
    @NotNull
    Optional<Integer> getPriority(@NotNull AIGoal goal);

    /**
     * Sets the priority of a specific goal.
     *
     * @param goal the goal to set priority for
     * @param priority the new priority
     * @return true if the priority was updated
     */
    boolean setPriority(@NotNull AIGoal goal, @NotNull GoalPriority priority);

    /**
     * Sets the priority of a specific goal.
     *
     * @param goal the goal to set priority for
     * @param priority the new priority value
     * @return true if the priority was updated
     */
    boolean setPriority(@NotNull AIGoal goal, int priority);

    /**
     * Gets a goal by its identifier.
     *
     * @param identifier the goal identifier
     * @return the goal, or null if not found
     */
    @Nullable
    AIGoal getGoal(@NotNull String identifier);

    /**
     * Gets the first goal of the specified type.
     *
     * @param <T> the goal type
     * @param goalClass the goal class
     * @return an optional containing the goal, or empty if not found
     */
    @NotNull
    <T extends AIGoal> Optional<T> getGoal(@NotNull Class<T> goalClass);

    /**
     * Checks if this selector has a goal with the specified identifier.
     *
     * @param identifier the goal identifier
     * @return true if the goal exists
     */
    boolean hasGoal(@NotNull String identifier);

    /**
     * Checks if this selector has a goal of the specified type.
     *
     * @param goalClass the goal class
     * @return true if such a goal exists
     */
    boolean hasGoal(@NotNull Class<? extends AIGoal> goalClass);

    /**
     * Ticks the goal selector.
     *
     * <p>This evaluates all goals, starts eligible goals, ticks running
     * goals, and stops goals that should no longer run.</p>
     */
    void tick();

    /**
     * Forces a specific goal to start.
     *
     * <p>This bypasses priority checks and forces the goal to run.
     * Use sparingly as it can disrupt normal AI behavior.</p>
     *
     * @param goal the goal to force start
     * @return true if the goal was started
     */
    boolean forceStart(@NotNull AIGoal goal);

    /**
     * Forces a specific goal to stop.
     *
     * @param goal the goal to force stop
     * @param result the result to pass to the goal's stop method
     * @return true if the goal was stopped
     */
    boolean forceStop(@NotNull AIGoal goal, @NotNull GoalResult result);

    /**
     * Stops all currently running goals.
     */
    void stopAllGoals();

    /**
     * Pauses the goal selector.
     *
     * <p>While paused, no goals will be ticked or started.</p>
     */
    void pause();

    /**
     * Resumes the goal selector.
     */
    void resume();

    /**
     * Checks if the goal selector is paused.
     *
     * @return true if paused
     */
    boolean isPaused();

    /**
     * Resets the goal selector to its default state.
     *
     * <p>This stops all goals and clears cooldowns.</p>
     */
    void reset();

    /**
     * Gets the total number of goals.
     *
     * @return the goal count
     */
    int getGoalCount();

    /**
     * Gets the number of currently running goals.
     *
     * @return the running goal count
     */
    int getRunningGoalCount();

    /**
     * Sets the goal evaluation interval.
     *
     * <p>By default, goals are evaluated every tick. Setting a higher
     * interval can improve performance for entities with many goals.</p>
     *
     * @param ticks the evaluation interval in ticks
     */
    void setEvaluationInterval(int ticks);

    /**
     * Gets the goal evaluation interval.
     *
     * @return the evaluation interval in ticks
     */
    int getEvaluationInterval();
}
