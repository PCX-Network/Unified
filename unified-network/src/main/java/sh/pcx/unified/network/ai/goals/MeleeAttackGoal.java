/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.goals;

import sh.pcx.unified.network.ai.goal.AIGoal;
import org.jetbrains.annotations.NotNull;

/**
 * AI goal that makes an entity perform melee attacks on its target.
 *
 * <p>This goal moves the entity towards its target and attacks when in range.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * controller.addGoal(0, new MeleeAttackGoal(entity)
 *     .attackInterval(20)
 *     .attackDamage(5.0)
 *     .attackReach(2.0)
 *     .speed(1.0)
 *     .longMemory(true)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AIGoal
 */
public class MeleeAttackGoal extends AIGoal {

    private double speed = 1.0;
    private int attackInterval = 20;
    private double attackDamage = -1; // -1 means use entity's default
    private double attackReach = 2.0;
    private boolean longMemory = false;

    private int attackCooldown = 0;
    private int ticksWithoutLOS = 0;
    private int maxTicksWithoutLOS = 60;

    /**
     * Creates a melee attack goal.
     *
     * @param entity the entity that will attack
     */
    public MeleeAttackGoal(@NotNull Object entity) {
        super(entity);
        setFlags(GoalFlag.MOVE, GoalFlag.LOOK);
    }

    /**
     * Sets the movement speed modifier.
     *
     * @param speed the speed modifier
     * @return this goal for chaining
     */
    @NotNull
    public MeleeAttackGoal speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Sets the attack interval in ticks.
     *
     * @param ticks the interval between attacks
     * @return this goal for chaining
     */
    @NotNull
    public MeleeAttackGoal attackInterval(int ticks) {
        this.attackInterval = ticks;
        return this;
    }

    /**
     * Sets the attack damage.
     *
     * <p>Set to -1 to use the entity's default damage.
     *
     * @param damage the damage amount
     * @return this goal for chaining
     */
    @NotNull
    public MeleeAttackGoal attackDamage(double damage) {
        this.attackDamage = damage;
        return this;
    }

    /**
     * Sets the attack reach distance.
     *
     * @param reach the reach in blocks
     * @return this goal for chaining
     */
    @NotNull
    public MeleeAttackGoal attackReach(double reach) {
        this.attackReach = reach;
        return this;
    }

    /**
     * Sets whether the entity has long memory.
     *
     * <p>When true, the entity remembers its target even when
     * line of sight is lost.
     *
     * @param longMemory true for long memory
     * @return this goal for chaining
     */
    @NotNull
    public MeleeAttackGoal longMemory(boolean longMemory) {
        this.longMemory = longMemory;
        return this;
    }

    /**
     * Sets the maximum ticks without line of sight before giving up.
     *
     * @param ticks the maximum ticks
     * @return this goal for chaining
     */
    @NotNull
    public MeleeAttackGoal maxTicksWithoutLOS(int ticks) {
        this.maxTicksWithoutLOS = ticks;
        return this;
    }

    /**
     * Gets the movement speed.
     *
     * @return the speed modifier
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Gets the attack interval.
     *
     * @return the interval in ticks
     */
    public int getAttackInterval() {
        return attackInterval;
    }

    /**
     * Gets the attack damage.
     *
     * @return the damage, or -1 for default
     */
    public double getAttackDamage() {
        return attackDamage;
    }

    /**
     * Gets the attack reach.
     *
     * @return the reach in blocks
     */
    public double getAttackReach() {
        return attackReach;
    }

    @Override
    public boolean canStart() {
        Object target = getTarget();
        return target != null && isTargetValid(target);
    }

    @Override
    public boolean canContinue() {
        Object target = getTarget();
        if (target == null || !isTargetValid(target)) {
            return false;
        }

        // Check line of sight
        if (!hasLineOfSight(target)) {
            ticksWithoutLOS++;
            if (!longMemory || ticksWithoutLOS > maxTicksWithoutLOS) {
                return false;
            }
        } else {
            ticksWithoutLOS = 0;
        }

        return true;
    }

    @Override
    public void start() {
        ticksWithoutLOS = 0;
        attackCooldown = 0;
    }

    @Override
    public void tick() {
        Object target = getTarget();
        if (target == null) {
            return;
        }

        // Look at target
        lookAt(target);

        // Decrease cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Check if in attack range
        double distance = getDistanceTo(target);
        if (distance <= attackReach) {
            // Stop moving when in range
            getNavigation().stop();

            // Attack if cooldown is ready
            if (attackCooldown <= 0) {
                performAttack(target);
                attackCooldown = attackInterval;
            }
        } else {
            // Move towards target
            getNavigation().moveTo(target, speed);
        }
    }

    @Override
    public void stop() {
        getNavigation().stop();
        ticksWithoutLOS = 0;
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    /**
     * Performs the attack on the target.
     *
     * @param target the target entity
     */
    protected void performAttack(@NotNull Object target) {
        // Implementation would:
        // 1. Play swing animation
        // 2. Deal damage to target
        // 3. Apply knockback
    }

    /**
     * Gets the current attack target.
     *
     * @return the target, or null if none
     */
    protected Object getTarget() {
        // Implementation would get the entity's attack target
        return null;
    }

    /**
     * Checks if the target is valid.
     *
     * @param target the target
     * @return true if valid
     */
    protected boolean isTargetValid(@NotNull Object target) {
        // Implementation would check if target is alive and valid
        return true;
    }

    /**
     * Checks if there is line of sight to the target.
     *
     * @param target the target
     * @return true if visible
     */
    protected boolean hasLineOfSight(@NotNull Object target) {
        // Implementation would ray-trace to target
        return true;
    }

    /**
     * Gets the distance to the target.
     *
     * @param target the target
     * @return the distance in blocks
     */
    protected double getDistanceTo(@NotNull Object target) {
        // Implementation would calculate distance
        return 0.0;
    }

    /**
     * Makes the entity look at the target.
     *
     * @param target the target
     */
    protected void lookAt(@NotNull Object target) {
        // Implementation would set entity head rotation
    }
}
