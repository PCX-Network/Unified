/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.goal;

import sh.pcx.unified.network.ai.navigation.NavigationController;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Base class for custom AI goals.
 *
 * <p>AI goals define specific behaviors that entities can perform.
 * Goals are managed by priority and can declare which systems they
 * use (movement, look, etc.) to prevent conflicts.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class GuardLocationGoal extends AIGoal {
 *
 *     private final Location guardPoint;
 *     private final double radius;
 *
 *     public GuardLocationGoal(Mob entity, Location guardPoint, double radius) {
 *         super(entity);
 *         this.guardPoint = guardPoint;
 *         this.radius = radius;
 *
 *         // Define goal flags
 *         setFlags(GoalFlag.MOVE, GoalFlag.LOOK);
 *     }
 *
 *     @Override
 *     public boolean canStart() {
 *         // Start if entity is outside guard radius
 *         return entity.getLocation().distance(guardPoint) > radius;
 *     }
 *
 *     @Override
 *     public boolean canContinue() {
 *         // Continue until back at guard point
 *         return entity.getLocation().distance(guardPoint) > 1.0;
 *     }
 *
 *     @Override
 *     public void start() {
 *         getNavigation().moveTo(guardPoint, 1.0);
 *     }
 *
 *     @Override
 *     public void tick() {
 *         if (getNavigation().isStuck()) {
 *             getNavigation().moveTo(guardPoint, 1.0);
 *         }
 *     }
 *
 *     @Override
 *     public void stop() {
 *         getNavigation().stop();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AIController
 * @see GoalFlag
 */
public abstract class AIGoal {

    /** The entity this goal controls. */
    protected final Object entity;

    /** The goal flags indicating which systems this goal uses. */
    private Set<GoalFlag> flags = EnumSet.noneOf(GoalFlag.class);

    /** Whether this goal is interruptible. */
    private boolean interruptible = true;

    /** Navigation controller for this entity. */
    private NavigationController navigation;

    /**
     * Creates a new AI goal for an entity.
     *
     * @param entity the entity this goal controls
     */
    protected AIGoal(@NotNull Object entity) {
        this.entity = entity;
    }

    /**
     * Returns the entity this goal controls.
     *
     * @param <T> the entity type
     * @return the entity
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getEntity() {
        return (T) entity;
    }

    /**
     * Called to check if this goal can start.
     *
     * <p>This is called every tick when the goal is not running.
     * Return true to start the goal.
     *
     * @return true if the goal should start
     * @since 1.0.0
     */
    public abstract boolean canStart();

    /**
     * Called to check if this goal should continue running.
     *
     * <p>This is called every tick while the goal is running.
     * Return false to stop the goal.
     *
     * @return true if the goal should continue
     * @since 1.0.0
     */
    public boolean canContinue() {
        return canStart();
    }

    /**
     * Called when the goal starts.
     *
     * <p>Use this for initialization and starting actions.
     *
     * @since 1.0.0
     */
    public void start() {
        // Override to implement
    }

    /**
     * Called every tick while the goal is running.
     *
     * @since 1.0.0
     */
    public void tick() {
        // Override to implement
    }

    /**
     * Called when the goal stops.
     *
     * <p>Use this for cleanup and stopping actions.
     *
     * @since 1.0.0
     */
    public void stop() {
        // Override to implement
    }

    /**
     * Called to check if this goal can be interrupted.
     *
     * @return true if interruptible
     * @since 1.0.0
     */
    public boolean isInterruptible() {
        return interruptible;
    }

    /**
     * Sets whether this goal can be interrupted.
     *
     * @param interruptible true to allow interruption
     * @since 1.0.0
     */
    protected void setInterruptible(boolean interruptible) {
        this.interruptible = interruptible;
    }

    /**
     * Returns the flags indicating which systems this goal uses.
     *
     * @return the goal flags
     * @since 1.0.0
     */
    @NotNull
    public Set<GoalFlag> getFlags() {
        return EnumSet.copyOf(flags);
    }

    /**
     * Sets the flags for this goal.
     *
     * @param flags the goal flags
     * @since 1.0.0
     */
    protected void setFlags(@NotNull GoalFlag... flags) {
        this.flags = flags.length > 0 ? EnumSet.of(flags[0], flags) : EnumSet.noneOf(GoalFlag.class);
    }

    /**
     * Sets the flags for this goal.
     *
     * @param flags the goal flags
     * @since 1.0.0
     */
    protected void setFlags(@NotNull Set<GoalFlag> flags) {
        this.flags = EnumSet.copyOf(flags);
    }

    /**
     * Adds a flag to this goal.
     *
     * @param flag the flag to add
     * @since 1.0.0
     */
    protected void addFlag(@NotNull GoalFlag flag) {
        this.flags.add(flag);
    }

    /**
     * Removes a flag from this goal.
     *
     * @param flag the flag to remove
     * @since 1.0.0
     */
    protected void removeFlag(@NotNull GoalFlag flag) {
        this.flags.remove(flag);
    }

    /**
     * Returns the navigation controller for the entity.
     *
     * @return the navigation controller
     * @since 1.0.0
     */
    @NotNull
    protected NavigationController getNavigation() {
        if (navigation == null) {
            throw new IllegalStateException("Navigation not available - goal not properly initialized");
        }
        return navigation;
    }

    /**
     * Sets the navigation controller.
     *
     * <p>This is called internally when the goal is added to a controller.
     *
     * @param navigation the navigation controller
     * @since 1.0.0
     */
    public void setNavigation(@NotNull NavigationController navigation) {
        this.navigation = navigation;
    }

    /**
     * Checks if this goal requires a target.
     *
     * @return true if a target is required
     * @since 1.0.0
     */
    public boolean requiresTarget() {
        return false;
    }

    /**
     * Adjusts the navigation speed for this goal.
     *
     * @param speedModifier the speed modifier
     * @since 1.0.0
     */
    protected void adjustSpeed(double speedModifier) {
        // Override to implement speed adjustments
    }

    /**
     * Flags that indicate which systems a goal uses.
     *
     * <p>Goals with conflicting flags cannot run simultaneously.
     *
     * @since 1.0.0
     */
    public enum GoalFlag {
        /** Controls entity movement. */
        MOVE,

        /** Controls where the entity looks. */
        LOOK,

        /** Controls entity jumping. */
        JUMP,

        /** Controls targeting behavior. */
        TARGET
    }
}
