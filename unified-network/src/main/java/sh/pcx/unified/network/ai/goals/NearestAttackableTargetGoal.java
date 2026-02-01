/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.goals;

import sh.pcx.unified.network.ai.target.TargetGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Target goal that selects the nearest attackable entity of a type.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * targets.addTarget(1, new NearestAttackableTargetGoal(entity, Player.class)
 *     .filter(player -> !player.hasPermission("myplugin.npc.ignore"))
 *     .range(16.0)
 *     .requireLineOfSight(true)
 *     .checkInterval(10)
 * );
 * }</pre>
 *
 * @param <T> the target entity type
 * @since 1.0.0
 * @author Supatuck
 * @see TargetGoal
 */
public class NearestAttackableTargetGoal<T> extends TargetGoal {

    private final Class<T> targetClass;
    private T cachedTarget;
    private int randomInterval = 0;

    /**
     * Creates a nearest attackable target goal.
     *
     * @param entity      the entity that will target
     * @param targetClass the class of entities to target
     */
    public NearestAttackableTargetGoal(@NotNull Object entity, @NotNull Class<T> targetClass) {
        super(entity);
        this.targetClass = targetClass;
    }

    /**
     * Gets the target class.
     *
     * @return the target class
     */
    @NotNull
    public Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * Sets a random interval for target checks.
     *
     * <p>Adds randomness to reduce CPU usage and prevent synchronized targeting.
     *
     * @param ticks the random interval variation
     * @return this goal for chaining
     */
    @NotNull
    public NearestAttackableTargetGoal<T> randomInterval(int ticks) {
        this.randomInterval = ticks;
        return this;
    }

    @Override
    @NotNull
    public NearestAttackableTargetGoal<T> filter(@NotNull Predicate<Object> filter) {
        super.filter(filter);
        return this;
    }

    @Override
    @NotNull
    public NearestAttackableTargetGoal<T> range(double range) {
        super.range(range);
        return this;
    }

    @Override
    @NotNull
    public NearestAttackableTargetGoal<T> requireLineOfSight(boolean required) {
        super.requireLineOfSight(required);
        return this;
    }

    @Override
    @NotNull
    public NearestAttackableTargetGoal<T> targetInvisible(boolean target) {
        super.targetInvisible(target);
        return this;
    }

    @Override
    @NotNull
    public NearestAttackableTargetGoal<T> searchCooldown(int ticks) {
        super.searchCooldown(ticks);
        return this;
    }

    @Override
    public boolean canStart() {
        if (!canSearch()) {
            return false;
        }

        resetCooldown();
        cachedTarget = findTarget();
        return cachedTarget != null;
    }

    @Override
    public boolean canContinue() {
        T target = getTarget();
        if (target == null) {
            return false;
        }

        if (!acceptsTarget(target)) {
            return false;
        }

        // Check if target is still in range
        double distance = getDistanceTo(target);
        if (distance > getRange() * 1.5) { // Use larger range for continue
            return false;
        }

        // Check line of sight if required
        if (requiresLineOfSight() && !hasLineOfSight(target)) {
            return false;
        }

        return isTargetValid(target);
    }

    @Override
    public void start() {
        setTarget(cachedTarget);
        cachedTarget = null;
    }

    @Override
    public void stop() {
        setTarget(null);
        cachedTarget = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public T findTarget() {
        // Get all nearby entities of the target type
        var nearby = getNearbyEntities(targetClass, getRange());

        // Filter and find nearest
        return nearby.stream()
                .filter(this::acceptsTarget)
                .filter(this::isTargetValid)
                .filter(e -> targetsInvisible() || !isInvisible(e))
                .filter(e -> !requiresLineOfSight() || hasLineOfSight(e))
                .min(Comparator.comparingDouble(this::getDistanceTo))
                .orElse(null);
    }

    /**
     * Gets nearby entities of the target type.
     *
     * @param type  the entity type
     * @param range the search range
     * @return nearby entities
     */
    @NotNull
    protected java.util.Collection<T> getNearbyEntities(@NotNull Class<T> type, double range) {
        // Implementation would search for nearby entities
        return java.util.Collections.emptyList();
    }

    /**
     * Checks if the target is valid.
     *
     * @param target the target
     * @return true if valid
     */
    protected boolean isTargetValid(@NotNull T target) {
        // Implementation would check if entity is alive
        return true;
    }

    /**
     * Checks if the target is invisible.
     *
     * @param target the target
     * @return true if invisible
     */
    protected boolean isInvisible(@NotNull T target) {
        // Implementation would check invisibility status
        return false;
    }

    /**
     * Checks for line of sight to the target.
     *
     * @param target the target
     * @return true if visible
     */
    protected boolean hasLineOfSight(@NotNull T target) {
        // Implementation would ray-trace
        return true;
    }

    /**
     * Gets the distance to the target.
     *
     * @param target the target
     * @return the distance
     */
    protected double getDistanceTo(@NotNull T target) {
        // Implementation would calculate distance
        return 0.0;
    }
}
