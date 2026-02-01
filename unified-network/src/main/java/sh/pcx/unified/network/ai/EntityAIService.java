/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai;

import sh.pcx.unified.network.ai.behavior.BehaviorTree;
import sh.pcx.unified.network.ai.goal.AIController;
import sh.pcx.unified.network.ai.navigation.NavigationController;
import sh.pcx.unified.network.ai.target.TargetSelector;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for customizing entity AI, goals, and pathfinding.
 *
 * <p>The EntityAIService provides access to mob AI systems including
 * goal management, navigation, target selection, and behavior trees.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private EntityAIService ai;
 *
 * // Get AI controller for entity
 * AIController controller = ai.getController(entity);
 *
 * // Remove vanilla goals
 * controller.clearGoals();
 * controller.removeGoal(VanillaGoal.RANDOM_STROLL);
 *
 * // Add custom goals
 * controller.addGoal(1, new FollowPlayerGoal(entity, targetPlayer)
 *     .speed(1.2)
 *     .minDistance(2.0)
 *     .maxDistance(10.0)
 * );
 *
 * controller.addGoal(2, new PatrolPathGoal(entity, patrolPoints)
 *     .speed(1.0)
 *     .waitTime(Duration.ofSeconds(5))
 *     .loop(true)
 * );
 *
 * // Set up navigation
 * NavigationController nav = ai.getNavigation(entity);
 * nav.moveTo(targetLocation, speed);
 *
 * // Set up targeting
 * TargetSelector targets = ai.getTargetSelector(entity);
 * targets.addTarget(1, new NearestAttackableTargetGoal(entity, Player.class)
 *     .filter(p -> !p.hasPermission("myPlugin.ignore"))
 *     .range(16.0)
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>AI modifications should be done on the main server thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AIController
 * @see NavigationController
 * @see TargetSelector
 */
public interface EntityAIService extends Service {

    /**
     * Gets the AI controller for an entity.
     *
     * @param entity the mob entity
     * @return the AI controller
     * @throws IllegalArgumentException if the entity doesn't support AI
     * @since 1.0.0
     */
    @NotNull
    AIController getController(@NotNull Object entity);

    /**
     * Gets the AI controller for an entity by UUID.
     *
     * @param entityId the entity's UUID
     * @return the AI controller, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<AIController> getController(@NotNull UUID entityId);

    /**
     * Gets the navigation controller for an entity.
     *
     * @param entity the mob entity
     * @return the navigation controller
     * @throws IllegalArgumentException if the entity doesn't support navigation
     * @since 1.0.0
     */
    @NotNull
    NavigationController getNavigation(@NotNull Object entity);

    /**
     * Gets the navigation controller for an entity by UUID.
     *
     * @param entityId the entity's UUID
     * @return the navigation controller, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<NavigationController> getNavigation(@NotNull UUID entityId);

    /**
     * Gets the target selector for an entity.
     *
     * @param entity the mob entity
     * @return the target selector
     * @throws IllegalArgumentException if the entity doesn't support targeting
     * @since 1.0.0
     */
    @NotNull
    TargetSelector getTargetSelector(@NotNull Object entity);

    /**
     * Gets the target selector for an entity by UUID.
     *
     * @param entityId the entity's UUID
     * @return the target selector, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<TargetSelector> getTargetSelector(@NotNull UUID entityId);

    /**
     * Sets a behavior tree for an entity.
     *
     * <p>Behavior trees provide advanced AI control and can replace
     * the standard goal-based AI system.
     *
     * @param entity the mob entity
     * @param tree   the behavior tree
     * @since 1.0.0
     */
    void setBehaviorTree(@NotNull Object entity, @NotNull BehaviorTree tree);

    /**
     * Gets the behavior tree for an entity.
     *
     * @param entity the mob entity
     * @return the behavior tree, or empty if none set
     * @since 1.0.0
     */
    @NotNull
    Optional<BehaviorTree> getBehaviorTree(@NotNull Object entity);

    /**
     * Clears the behavior tree from an entity.
     *
     * @param entity the mob entity
     * @since 1.0.0
     */
    void clearBehaviorTree(@NotNull Object entity);

    /**
     * Checks if an entity supports AI customization.
     *
     * @param entity the entity to check
     * @return true if the entity supports AI
     * @since 1.0.0
     */
    boolean supportsAI(@NotNull Object entity);

    /**
     * Freezes an entity's AI, preventing all goal execution.
     *
     * @param entity the mob entity
     * @since 1.0.0
     */
    void freezeAI(@NotNull Object entity);

    /**
     * Unfreezes an entity's AI.
     *
     * @param entity the mob entity
     * @since 1.0.0
     */
    void unfreezeAI(@NotNull Object entity);

    /**
     * Checks if an entity's AI is frozen.
     *
     * @param entity the mob entity
     * @return true if frozen
     * @since 1.0.0
     */
    boolean isAIFrozen(@NotNull Object entity);

    /**
     * Ticks the AI for an entity manually.
     *
     * <p>This is useful for custom tick rates or paused entities.
     *
     * @param entity the mob entity
     * @since 1.0.0
     */
    void tickAI(@NotNull Object entity);

    /**
     * Registers a custom goal type for use in configurations.
     *
     * @param name      the goal type name
     * @param goalClass the goal class
     * @since 1.0.0
     */
    void registerGoalType(@NotNull String name, @NotNull Class<?> goalClass);

    /**
     * Creates a goal instance from configuration.
     *
     * @param <T>    the goal type
     * @param entity the target entity
     * @param config the goal configuration
     * @return the created goal
     * @since 1.0.0
     */
    @NotNull
    <T> T createGoal(@NotNull Object entity, @NotNull Object config);
}
