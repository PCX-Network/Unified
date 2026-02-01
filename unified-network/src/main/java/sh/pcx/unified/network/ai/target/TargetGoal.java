/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.target;

import sh.pcx.unified.network.ai.goal.AIGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Base class for target selection goals.
 *
 * <p>Target goals determine which entities a mob will target for attack
 * or other interactions. They are separate from regular AI goals.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class ReputationTargetGoal extends TargetGoal {
 *
 *     private final double range;
 *     private LivingEntity target;
 *
 *     public ReputationTargetGoal(Mob entity, double range) {
 *         super(entity);
 *         this.range = range;
 *     }
 *
 *     @Override
 *     public boolean canStart() {
 *         target = findTarget();
 *         return target != null;
 *     }
 *
 *     @Override
 *     public LivingEntity findTarget() {
 *         return getNearbyPlayers(range).stream()
 *             .filter(p -> getReputation(p) < 0)
 *             .min(Comparator.comparingDouble(this::distanceTo))
 *             .orElse(null);
 *     }
 *
 *     @Override
 *     public void start() {
 *         setTarget(target);
 *     }
 *
 *     private int getReputation(Player player) {
 *         // Custom reputation logic
 *         return 0;
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TargetSelector
 */
public abstract class TargetGoal extends AIGoal {

    /** The target filter. */
    private Predicate<Object> filter = t -> true;

    /** The targeting range. */
    private double range = 16.0;

    /** Whether to require line of sight. */
    private boolean requireLineOfSight = true;

    /** Whether to target invisible entities. */
    private boolean targetInvisible = false;

    /** Cooldown between target searches in ticks. */
    private int searchCooldown = 10;

    /** The current cooldown counter. */
    private int currentCooldown = 0;

    /**
     * Creates a new target goal.
     *
     * @param entity the entity this goal controls
     */
    protected TargetGoal(@NotNull Object entity) {
        super(entity);
        setFlags(GoalFlag.TARGET);
    }

    /**
     * Finds a target entity.
     *
     * <p>Override this method to implement custom target selection.
     *
     * @param <T> the entity type
     * @return the target, or null if none found
     * @since 1.0.0
     */
    @Nullable
    public abstract <T> T findTarget();

    /**
     * Sets the target filter.
     *
     * @param filter the filter predicate
     * @return this goal for chaining
     * @since 1.0.0
     */
    @NotNull
    public TargetGoal filter(@NotNull Predicate<Object> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Gets the target filter.
     *
     * @return the filter
     * @since 1.0.0
     */
    @NotNull
    public Predicate<Object> getFilter() {
        return filter;
    }

    /**
     * Sets the targeting range.
     *
     * @param range the range in blocks
     * @return this goal for chaining
     * @since 1.0.0
     */
    @NotNull
    public TargetGoal range(double range) {
        this.range = range;
        return this;
    }

    /**
     * Gets the targeting range.
     *
     * @return the range
     * @since 1.0.0
     */
    public double getRange() {
        return range;
    }

    /**
     * Sets whether line of sight is required.
     *
     * @param required true to require line of sight
     * @return this goal for chaining
     * @since 1.0.0
     */
    @NotNull
    public TargetGoal requireLineOfSight(boolean required) {
        this.requireLineOfSight = required;
        return this;
    }

    /**
     * Checks if line of sight is required.
     *
     * @return true if required
     * @since 1.0.0
     */
    public boolean requiresLineOfSight() {
        return requireLineOfSight;
    }

    /**
     * Sets whether to target invisible entities.
     *
     * @param target true to target invisible
     * @return this goal for chaining
     * @since 1.0.0
     */
    @NotNull
    public TargetGoal targetInvisible(boolean target) {
        this.targetInvisible = target;
        return this;
    }

    /**
     * Checks if invisible entities are targeted.
     *
     * @return true if targeting invisible
     * @since 1.0.0
     */
    public boolean targetsInvisible() {
        return targetInvisible;
    }

    /**
     * Sets the search cooldown.
     *
     * @param ticks the cooldown in ticks
     * @return this goal for chaining
     * @since 1.0.0
     */
    @NotNull
    public TargetGoal searchCooldown(int ticks) {
        this.searchCooldown = ticks;
        return this;
    }

    /**
     * Gets the search cooldown.
     *
     * @return the cooldown in ticks
     * @since 1.0.0
     */
    public int getSearchCooldown() {
        return searchCooldown;
    }

    /**
     * Checks if a search can be performed.
     *
     * @return true if cooldown has elapsed
     * @since 1.0.0
     */
    protected boolean canSearch() {
        return currentCooldown <= 0;
    }

    /**
     * Resets the search cooldown.
     *
     * @since 1.0.0
     */
    protected void resetCooldown() {
        currentCooldown = searchCooldown;
    }

    /**
     * Sets the current target.
     *
     * @param target the target entity
     * @since 1.0.0
     */
    protected void setTarget(@Nullable Object target) {
        // Implementation will set the entity's target
    }

    /**
     * Gets the current target.
     *
     * @param <T> the entity type
     * @return the current target, or null
     * @since 1.0.0
     */
    @Nullable
    protected <T> T getTarget() {
        // Implementation will get the entity's target
        return null;
    }

    /**
     * Checks if the filter accepts a target.
     *
     * @param target the potential target
     * @return true if accepted
     * @since 1.0.0
     */
    protected boolean acceptsTarget(@NotNull Object target) {
        return filter.test(target);
    }

    @Override
    public void tick() {
        if (currentCooldown > 0) {
            currentCooldown--;
        }
    }

    @Override
    public boolean requiresTarget() {
        return false;
    }
}
