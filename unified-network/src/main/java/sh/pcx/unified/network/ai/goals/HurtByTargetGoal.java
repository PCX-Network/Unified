/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.goals;

import sh.pcx.unified.network.ai.target.TargetGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Target goal that targets entities that have hurt this entity.
 *
 * <p>Optionally alerts other nearby mobs of the same type.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * targets.addTarget(2, new HurtByTargetGoal(entity)
 *     .alertOthers(Zombie.class)
 *     .alertRange(16.0)
 *     .ignoreEntityType(Skeleton.class)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TargetGoal
 */
public class HurtByTargetGoal extends TargetGoal {

    private final Set<Class<?>> alertClasses = new HashSet<>();
    private final Set<Class<?>> ignoredClasses = new HashSet<>();
    private double alertRange = 16.0;
    private int forgetTicks = 100;
    private boolean alertSameType = false;

    private Object lastHurtBy;
    private int lastHurtTick = 0;

    /**
     * Creates a hurt by target goal.
     *
     * @param entity the entity that will target its attacker
     */
    public HurtByTargetGoal(@NotNull Object entity) {
        super(entity);
    }

    /**
     * Adds entity types that should be alerted when this entity is hurt.
     *
     * @param classes the entity classes to alert
     * @return this goal for chaining
     */
    @NotNull
    @SafeVarargs
    public final HurtByTargetGoal alertOthers(@NotNull Class<?>... classes) {
        for (Class<?> clazz : classes) {
            alertClasses.add(clazz);
        }
        return this;
    }

    /**
     * Sets whether to alert entities of the same type.
     *
     * @param alertSameType true to alert same type
     * @return this goal for chaining
     */
    @NotNull
    public HurtByTargetGoal alertSameType(boolean alertSameType) {
        this.alertSameType = alertSameType;
        return this;
    }

    /**
     * Sets the range for alerting other entities.
     *
     * @param range the alert range
     * @return this goal for chaining
     */
    @NotNull
    public HurtByTargetGoal alertRange(double range) {
        this.alertRange = range;
        return this;
    }

    /**
     * Adds entity types to ignore when targeting.
     *
     * @param classes the entity classes to ignore
     * @return this goal for chaining
     */
    @NotNull
    @SafeVarargs
    public final HurtByTargetGoal ignoreEntityType(@NotNull Class<?>... classes) {
        for (Class<?> clazz : classes) {
            ignoredClasses.add(clazz);
        }
        return this;
    }

    /**
     * Sets how long to remember the attacker.
     *
     * @param ticks the forget time in ticks
     * @return this goal for chaining
     */
    @NotNull
    public HurtByTargetGoal forgetAfter(int ticks) {
        this.forgetTicks = ticks;
        return this;
    }

    /**
     * Gets the last entity that hurt this entity.
     *
     * @param <T> the entity type
     * @return the last attacker, or null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getLastHurtBy() {
        return (T) lastHurtBy;
    }

    /**
     * Gets the alert range.
     *
     * @return the range in blocks
     */
    public double getAlertRange() {
        return alertRange;
    }

    @Override
    public boolean canStart() {
        Object attacker = getLastHurtByEntity();

        if (attacker == null) {
            return false;
        }

        // Check if we should forget
        int ticksSinceHurt = getCurrentTick() - lastHurtTick;
        if (ticksSinceHurt > forgetTicks) {
            lastHurtBy = null;
            return false;
        }

        // Check if attacker is in ignored list
        if (isIgnored(attacker)) {
            return false;
        }

        // Check filter
        if (!acceptsTarget(attacker)) {
            return false;
        }

        lastHurtBy = attacker;
        return true;
    }

    @Override
    public boolean canContinue() {
        Object target = getTarget();
        if (target == null) {
            return false;
        }

        return isTargetValid(target) && acceptsTarget(target);
    }

    @Override
    public void start() {
        setTarget(lastHurtBy);

        // Alert nearby entities
        if (!alertClasses.isEmpty() || alertSameType) {
            alertNearbyMobs();
        }
    }

    @Override
    public void stop() {
        setTarget(null);
        lastHurtBy = null;
    }

    @Override
    @Nullable
    public <T> T findTarget() {
        return getLastHurtBy();
    }

    /**
     * Called when this entity is hurt.
     *
     * <p>This should be called by the damage system.
     *
     * @param attacker the attacking entity
     */
    public void onHurt(@NotNull Object attacker) {
        lastHurtBy = attacker;
        lastHurtTick = getCurrentTick();
    }

    /**
     * Alerts nearby mobs to target the attacker.
     */
    protected void alertNearbyMobs() {
        // Implementation would:
        // 1. Find nearby entities of alert types
        // 2. Set their target to the attacker
    }

    /**
     * Gets the last entity that hurt this entity.
     *
     * @return the attacker, or null
     */
    @Nullable
    protected Object getLastHurtByEntity() {
        // Implementation would get from entity data
        return lastHurtBy;
    }

    /**
     * Checks if an entity type is ignored.
     *
     * @param entity the entity
     * @return true if ignored
     */
    protected boolean isIgnored(@NotNull Object entity) {
        for (Class<?> ignoredClass : ignoredClasses) {
            if (ignoredClass.isInstance(entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the target is valid.
     *
     * @param target the target
     * @return true if valid
     */
    protected boolean isTargetValid(@NotNull Object target) {
        // Implementation would check if entity is alive
        return true;
    }

    /**
     * Gets the current game tick.
     *
     * @return the current tick
     */
    protected int getCurrentTick() {
        // Implementation would return server tick
        return 0;
    }
}
