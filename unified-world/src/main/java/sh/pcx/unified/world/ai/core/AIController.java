package sh.pcx.unified.world.ai.core;

import sh.pcx.unified.world.ai.behavior.BehaviorTree;
import sh.pcx.unified.world.ai.goals.GoalSelector;
import sh.pcx.unified.world.ai.navigation.NavigationController;
import sh.pcx.unified.world.ai.targeting.TargetSelector;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Controls the AI behavior of an entity.
 *
 * <p>The AIController is the central component that manages all AI-related
 * functionality for a single entity. It coordinates goal selection, navigation,
 * targeting, and behavior tree execution.</p>
 *
 * <h2>Components:</h2>
 * <ul>
 *   <li><b>GoalSelector:</b> Manages and prioritizes AI goals</li>
 *   <li><b>NavigationController:</b> Handles pathfinding and movement</li>
 *   <li><b>TargetSelector:</b> Manages target acquisition and tracking</li>
 *   <li><b>BehaviorTree:</b> Optional behavior tree for complex AI</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * AIController controller = aiService.getOrCreateController(entity);
 *
 * // Configure goals
 * controller.getGoalSelector()
 *     .addGoal(new WanderGoal(), GoalPriority.LOW)
 *     .addGoal(new AttackGoal(), GoalPriority.HIGH);
 *
 * // Set target
 * controller.getTargetSelector()
 *     .addFilter(TargetFilter.hostile())
 *     .setMaxDistance(32.0);
 *
 * // Enable AI
 * controller.setEnabled(true);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AIController {

    /**
     * Gets the entity controlled by this AI controller.
     *
     * @return the controlled entity
     */
    @NotNull
    LivingEntity getEntity();

    /**
     * Gets the unique ID of the controlled entity.
     *
     * @return the entity's UUID
     */
    @NotNull
    UUID getEntityId();

    /**
     * Gets the goal selector for this controller.
     *
     * <p>The goal selector manages AI goals and their priorities.</p>
     *
     * @return the goal selector
     */
    @NotNull
    GoalSelector getGoalSelector();

    /**
     * Gets the navigation controller for this entity.
     *
     * <p>The navigation controller handles pathfinding and movement.</p>
     *
     * @return the navigation controller
     */
    @NotNull
    NavigationController getNavigation();

    /**
     * Gets the target selector for this entity.
     *
     * <p>The target selector manages target acquisition and tracking.</p>
     *
     * @return the target selector
     */
    @NotNull
    TargetSelector getTargeting();

    /**
     * Gets the behavior tree for this controller.
     *
     * @return an optional containing the behavior tree, or empty if not set
     */
    @NotNull
    Optional<BehaviorTree> getBehaviorTree();

    /**
     * Sets the behavior tree for this controller.
     *
     * <p>When a behavior tree is set, it takes priority over the goal selector.
     * Set to null to disable behavior tree execution.</p>
     *
     * @param behaviorTree the behavior tree to use, or null to disable
     */
    void setBehaviorTree(@Nullable BehaviorTree behaviorTree);

    /**
     * Gets the current target entity.
     *
     * @return an optional containing the target, or empty if no target
     */
    @NotNull
    Optional<LivingEntity> getTarget();

    /**
     * Sets the current target entity.
     *
     * @param target the target entity, or null to clear
     */
    void setTarget(@Nullable LivingEntity target);

    /**
     * Clears the current target.
     */
    void clearTarget();

    /**
     * Checks if this controller is enabled.
     *
     * @return true if the controller is enabled
     */
    boolean isEnabled();

    /**
     * Sets whether this controller is enabled.
     *
     * <p>When disabled, the controller will not tick and all AI
     * processing will be paused.</p>
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Checks if the controlled entity is alive and valid.
     *
     * @return true if the entity is valid
     */
    boolean isValid();

    /**
     * Ticks the AI controller.
     *
     * <p>This updates all AI components including goals, navigation,
     * targeting, and behavior tree execution.</p>
     */
    void tick();

    /**
     * Resets the AI controller to its default state.
     *
     * <p>This clears all goals, stops navigation, clears targets,
     * and resets the behavior tree.</p>
     */
    void reset();

    /**
     * Gets the number of ticks since this controller was created.
     *
     * @return the tick count
     */
    long getTickCount();

    /**
     * Gets the time in milliseconds since the last tick.
     *
     * @return the delta time in milliseconds
     */
    long getDeltaTime();

    /**
     * Checks if the entity is currently moving.
     *
     * @return true if the entity is moving
     */
    boolean isMoving();

    /**
     * Checks if the entity is currently attacking.
     *
     * @return true if the entity is attacking
     */
    boolean isAttacking();

    /**
     * Checks if the entity is currently fleeing.
     *
     * @return true if the entity is fleeing
     */
    boolean isFleeing();

    /**
     * Gets the aggression level of the entity.
     *
     * <p>The aggression level affects target selection and attack behavior.
     * Values range from 0.0 (passive) to 1.0 (highly aggressive).</p>
     *
     * @return the aggression level
     */
    double getAggressionLevel();

    /**
     * Sets the aggression level of the entity.
     *
     * @param level the aggression level (0.0 to 1.0)
     * @throws IllegalArgumentException if level is outside valid range
     */
    void setAggressionLevel(double level);

    /**
     * Gets the awareness radius for detecting targets.
     *
     * @return the awareness radius in blocks
     */
    double getAwarenessRadius();

    /**
     * Sets the awareness radius for detecting targets.
     *
     * @param radius the awareness radius in blocks
     */
    void setAwarenessRadius(double radius);

    /**
     * Gets metadata associated with this controller.
     *
     * @param key the metadata key
     * @param <T> the value type
     * @return an optional containing the value, or empty if not set
     */
    @NotNull
    <T> Optional<T> getMetadata(@NotNull String key);

    /**
     * Sets metadata on this controller.
     *
     * @param key the metadata key
     * @param value the value to set
     * @param <T> the value type
     */
    <T> void setMetadata(@NotNull String key, @NotNull T value);

    /**
     * Removes metadata from this controller.
     *
     * @param key the metadata key
     * @return true if metadata was removed
     */
    boolean removeMetadata(@NotNull String key);

    /**
     * Disposes of this controller and releases all resources.
     */
    void dispose();
}
