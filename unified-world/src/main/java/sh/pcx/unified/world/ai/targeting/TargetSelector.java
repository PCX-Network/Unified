package sh.pcx.unified.world.ai.targeting;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Manages target selection and filtering for an entity.
 *
 * <p>The TargetSelector is responsible for finding, evaluating, and tracking
 * potential targets for an AI-controlled entity. It supports filtering,
 * prioritization, and automatic target updates.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * TargetSelector targeting = aiController.getTargeting();
 *
 * // Configure targeting
 * targeting.addFilter(TargetFilter.hostile())
 *          .setMaxDistance(32.0)
 *          .setPrioritization(TargetPriority.NEAREST);
 *
 * // Find targets
 * Optional<LivingEntity> target = targeting.findTarget();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public interface TargetSelector {

    /**
     * Gets the entity that this selector belongs to.
     *
     * @return the owner entity
     */
    @NotNull
    LivingEntity getEntity();

    /**
     * Gets the current target.
     *
     * @return the current target, or empty if none
     */
    @NotNull
    Optional<LivingEntity> getTarget();

    /**
     * Sets the current target.
     *
     * @param target the target, or null to clear
     */
    void setTarget(@Nullable LivingEntity target);

    /**
     * Clears the current target.
     */
    void clearTarget();

    /**
     * Checks if there is a current target.
     *
     * @return true if there is a target
     */
    boolean hasTarget();

    /**
     * Finds and returns the best target based on current filters.
     *
     * @return the best target, or empty if none found
     */
    @NotNull
    Optional<LivingEntity> findTarget();

    /**
     * Finds all potential targets based on current filters.
     *
     * @return collection of potential targets
     */
    @NotNull
    Collection<LivingEntity> findAllTargets();

    /**
     * Adds a target filter.
     *
     * @param filter the filter predicate
     * @return this selector for chaining
     */
    @NotNull
    TargetSelector addFilter(@NotNull Predicate<Entity> filter);

    /**
     * Removes all filters.
     *
     * @return this selector for chaining
     */
    @NotNull
    TargetSelector clearFilters();

    /**
     * Sets the maximum target distance.
     *
     * @param distance the maximum distance in blocks
     * @return this selector for chaining
     */
    @NotNull
    TargetSelector setMaxDistance(double distance);

    /**
     * Gets the maximum target distance.
     *
     * @return the maximum distance
     */
    double getMaxDistance();

    /**
     * Sets whether line of sight is required.
     *
     * @param required true to require line of sight
     * @return this selector for chaining
     */
    @NotNull
    TargetSelector setRequireLineOfSight(boolean required);

    /**
     * Checks if line of sight is required.
     *
     * @return true if line of sight is required
     */
    boolean requiresLineOfSight();

    /**
     * Sets the target prioritization strategy.
     *
     * @param priority the prioritization strategy
     * @return this selector for chaining
     */
    @NotNull
    TargetSelector setPrioritization(@NotNull TargetPriority priority);

    /**
     * Gets the current prioritization strategy.
     *
     * @return the prioritization strategy
     */
    @NotNull
    TargetPriority getPrioritization();

    /**
     * Validates the current target.
     *
     * @return true if the target is still valid
     */
    boolean validateTarget();

    /**
     * Ticks the target selector.
     */
    void tick();

    /**
     * Target prioritization strategies.
     */
    enum TargetPriority {
        /**
         * Prioritize the nearest target.
         */
        NEAREST,

        /**
         * Prioritize the target with lowest health.
         */
        LOWEST_HEALTH,

        /**
         * Prioritize the target with highest health.
         */
        HIGHEST_HEALTH,

        /**
         * Prioritize the target dealing the most damage.
         */
        MOST_DAMAGE,

        /**
         * Random target selection.
         */
        RANDOM
    }
}
