package sh.pcx.unified.world.ai.core;

import sh.pcx.unified.world.ai.behavior.BehaviorTree;
import sh.pcx.unified.world.ai.boss.BossController;
import sh.pcx.unified.world.ai.goals.AIGoal;
import sh.pcx.unified.world.ai.navigation.NavigationController;
import sh.pcx.unified.world.ai.targeting.TargetSelector;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service interface for the Entity AI system.
 *
 * <p>The EntityAIService provides centralized management for all AI-related
 * functionality including AI controllers, navigation, targeting, and behavior
 * trees. It serves as the primary entry point for interacting with the AI system.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>AI Controller management for entities</li>
 *   <li>Goal-based AI system with priorities</li>
 *   <li>Custom pathfinding and navigation</li>
 *   <li>Target selection and filtering</li>
 *   <li>Behavior tree support</li>
 *   <li>Boss phase controller</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * EntityAIService aiService = // obtain service instance
 *
 * // Get or create AI controller for entity
 * AIController controller = aiService.getOrCreateController(entity);
 *
 * // Add goals
 * controller.getGoalSelector().addGoal(new FollowEntityGoal(target), GoalPriority.NORMAL);
 *
 * // Enable the controller
 * controller.setEnabled(true);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public interface EntityAIService {

    /**
     * Gets the AI controller for the specified entity.
     *
     * @param entity the entity to get the controller for
     * @return an optional containing the controller, or empty if none exists
     */
    @NotNull
    Optional<AIController> getController(@NotNull LivingEntity entity);

    /**
     * Gets the AI controller for the specified entity UUID.
     *
     * @param entityId the UUID of the entity
     * @return an optional containing the controller, or empty if none exists
     */
    @NotNull
    Optional<AIController> getController(@NotNull UUID entityId);

    /**
     * Gets or creates an AI controller for the specified entity.
     *
     * <p>If a controller already exists for the entity, it is returned.
     * Otherwise, a new controller is created and registered.</p>
     *
     * @param entity the entity to get or create a controller for
     * @return the AI controller for the entity
     */
    @NotNull
    AIController getOrCreateController(@NotNull LivingEntity entity);

    /**
     * Creates a new AI controller for the specified entity.
     *
     * <p>This will replace any existing controller for the entity.</p>
     *
     * @param entity the entity to create a controller for
     * @return the newly created AI controller
     */
    @NotNull
    AIController createController(@NotNull LivingEntity entity);

    /**
     * Removes the AI controller for the specified entity.
     *
     * @param entity the entity to remove the controller for
     * @return true if a controller was removed, false if none existed
     */
    boolean removeController(@NotNull LivingEntity entity);

    /**
     * Removes the AI controller for the specified entity UUID.
     *
     * @param entityId the UUID of the entity
     * @return true if a controller was removed, false if none existed
     */
    boolean removeController(@NotNull UUID entityId);

    /**
     * Checks if the specified entity has an AI controller.
     *
     * @param entity the entity to check
     * @return true if the entity has a controller
     */
    boolean hasController(@NotNull LivingEntity entity);

    /**
     * Gets all active AI controllers.
     *
     * @return an unmodifiable collection of all active controllers
     */
    @NotNull
    Collection<AIController> getAllControllers();

    /**
     * Creates a new navigation controller for the specified entity.
     *
     * @param entity the entity to create navigation for
     * @return the navigation controller
     */
    @NotNull
    NavigationController createNavigationController(@NotNull LivingEntity entity);

    /**
     * Creates a new target selector for the specified entity.
     *
     * @param entity the entity to create target selection for
     * @return the target selector
     */
    @NotNull
    TargetSelector createTargetSelector(@NotNull LivingEntity entity);

    /**
     * Creates a new behavior tree with the specified name.
     *
     * @param name the name of the behavior tree
     * @return the behavior tree builder
     */
    @NotNull
    BehaviorTree.Builder createBehaviorTree(@NotNull String name);

    /**
     * Creates a new boss controller for the specified entity.
     *
     * @param entity the boss entity
     * @param name the display name of the boss
     * @return the boss controller
     */
    @NotNull
    BossController createBossController(@NotNull LivingEntity entity, @NotNull String name);

    /**
     * Registers a custom goal type.
     *
     * @param <T> the goal type
     * @param identifier the unique identifier for the goal type
     * @param goalClass the goal class
     */
    <T extends AIGoal> void registerGoalType(@NotNull String identifier, @NotNull Class<T> goalClass);

    /**
     * Gets a registered goal type by identifier.
     *
     * @param identifier the goal type identifier
     * @return the goal class, or null if not registered
     */
    @Nullable
    Class<? extends AIGoal> getGoalType(@NotNull String identifier);

    /**
     * Ticks all active AI controllers.
     *
     * <p>This method should be called every game tick to update
     * all AI controllers and their associated systems.</p>
     */
    void tick();

    /**
     * Shuts down the AI service and cleans up all resources.
     */
    void shutdown();

    /**
     * Gets the number of active AI controllers.
     *
     * @return the controller count
     */
    int getControllerCount();

    /**
     * Checks if the AI service is enabled.
     *
     * @return true if the service is enabled
     */
    boolean isEnabled();

    /**
     * Sets whether the AI service is enabled.
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);
}
