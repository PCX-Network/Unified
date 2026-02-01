package sh.pcx.unified.world.ai.goals.builtin;

import sh.pcx.unified.world.ai.core.AIController;
import sh.pcx.unified.world.ai.goals.AIGoal;
import sh.pcx.unified.world.ai.goals.GoalResult;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A goal that causes the entity to attack a target.
 *
 * <p>The entity will move towards and attack a target entity,
 * respecting attack cooldowns and range requirements. Supports
 * both melee and ranged attack configurations.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Configurable attack range and cooldown</li>
 *   <li>Dynamic target via supplier or controller target</li>
 *   <li>Custom attack callback for special attacks</li>
 *   <li>Chase target if out of range</li>
 *   <li>Line of sight requirements</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * AttackGoal goal = AttackGoal.builder()
 *     .attackRange(3.0)
 *     .attackCooldown(20)
 *     .damage(5.0)
 *     .chaseRange(16.0)
 *     .onAttack(target -> {
 *         // Custom attack logic
 *         target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK, 1, 1);
 *     })
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class AttackGoal implements AIGoal {

    private static final String IDENTIFIER = "builtin:attack";
    private static final String NAME = "Attack";

    private final Supplier<LivingEntity> targetSupplier;
    private final double attackRange;
    private final int attackCooldown;
    private final double damage;
    private final double chaseRange;
    private final double chaseSpeed;
    private final boolean requireLineOfSight;
    private final Consumer<LivingEntity> attackCallback;
    private final int pathfindInterval;

    private AIController controller;
    private LivingEntity currentTarget;
    private int cooldownTicks;
    private int ticksSincePathfind;
    private AttackState state;

    /**
     * Creates a new attack goal.
     *
     * @param targetSupplier supplier for the target entity
     * @param attackRange the attack range
     * @param attackCooldown cooldown between attacks in ticks
     * @param damage damage per attack
     * @param chaseRange maximum chase range
     * @param chaseSpeed chase movement speed
     * @param requireLineOfSight whether line of sight is required
     * @param attackCallback optional callback when attacking
     * @param pathfindInterval ticks between pathfinding updates
     */
    protected AttackGoal(
            @Nullable Supplier<LivingEntity> targetSupplier,
            double attackRange,
            int attackCooldown,
            double damage,
            double chaseRange,
            double chaseSpeed,
            boolean requireLineOfSight,
            @Nullable Consumer<LivingEntity> attackCallback,
            int pathfindInterval
    ) {
        this.targetSupplier = targetSupplier;
        this.attackRange = attackRange;
        this.attackCooldown = attackCooldown;
        this.damage = damage;
        this.chaseRange = chaseRange;
        this.chaseSpeed = chaseSpeed;
        this.requireLineOfSight = requireLineOfSight;
        this.attackCallback = attackCallback;
        this.pathfindInterval = pathfindInterval;
        this.state = AttackState.IDLE;
    }

    /**
     * Creates a simple attack goal using controller's target.
     */
    public AttackGoal() {
        this(null, 2.0, 20, 2.0, 16.0, 1.0, true, null, 10);
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    @NotNull
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize(@NotNull AIController controller) {
        this.controller = controller;
    }

    @Override
    public boolean canStart() {
        LivingEntity target = getTarget();

        if (target == null || !target.isValid() || target.isDead()) {
            return false;
        }

        if (!target.getWorld().equals(controller.getEntity().getWorld())) {
            return false;
        }

        double distance = controller.getEntity().getLocation()
                .distance(target.getLocation());

        if (distance > chaseRange) {
            return false;
        }

        this.currentTarget = target;
        return true;
    }

    /**
     * Gets the current target entity.
     *
     * @return the target, or null if none
     */
    @Nullable
    private LivingEntity getTarget() {
        if (targetSupplier != null) {
            return targetSupplier.get();
        }
        return controller.getTarget().orElse(null);
    }

    @Override
    public void start() {
        cooldownTicks = 0;
        ticksSincePathfind = pathfindInterval;
        state = AttackState.CHASING;
    }

    @Override
    @NotNull
    public GoalResult tick() {
        // Update target reference
        currentTarget = getTarget();

        if (currentTarget == null || !currentTarget.isValid() || currentTarget.isDead()) {
            return GoalResult.failure("Target lost");
        }

        if (!currentTarget.getWorld().equals(controller.getEntity().getWorld())) {
            return GoalResult.failure("Target in different world");
        }

        double distance = controller.getEntity().getLocation()
                .distance(currentTarget.getLocation());

        // Check if target escaped
        if (distance > chaseRange) {
            return GoalResult.failure("Target escaped");
        }

        // Decrease cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        // Check line of sight
        if (requireLineOfSight && !controller.getEntity().hasLineOfSight(currentTarget)) {
            state = AttackState.CHASING;
            updatePath();
            return GoalResult.RUNNING;
        }

        // In attack range
        if (distance <= attackRange) {
            state = AttackState.ATTACKING;
            controller.getNavigation().stop();

            // Look at target
            lookAtTarget();

            // Attack if cooldown is ready
            if (cooldownTicks <= 0) {
                performAttack();
                cooldownTicks = attackCooldown;
            }
        } else {
            // Chase target
            state = AttackState.CHASING;
            ticksSincePathfind++;
            if (ticksSincePathfind >= pathfindInterval) {
                ticksSincePathfind = 0;
                updatePath();
            }
        }

        return GoalResult.RUNNING;
    }

    /**
     * Updates the pathfinding to chase the target.
     */
    private void updatePath() {
        if (currentTarget == null) {
            return;
        }

        controller.getNavigation().pathTo(currentTarget.getLocation(), chaseSpeed);
    }

    /**
     * Makes the entity look at the target.
     */
    private void lookAtTarget() {
        if (currentTarget == null) {
            return;
        }

        // Calculate look direction
        controller.getEntity().getLocation().setDirection(
                currentTarget.getLocation().toVector()
                        .subtract(controller.getEntity().getLocation().toVector())
        );
    }

    /**
     * Performs the attack on the target.
     */
    private void performAttack() {
        if (currentTarget == null) {
            return;
        }

        // Apply damage
        currentTarget.damage(damage, controller.getEntity());

        // Execute callback
        if (attackCallback != null) {
            attackCallback.accept(currentTarget);
        }
    }

    @Override
    public boolean shouldContinue() {
        if (currentTarget == null || !currentTarget.isValid() || currentTarget.isDead()) {
            return false;
        }

        double distance = controller.getEntity().getLocation()
                .distance(currentTarget.getLocation());

        return distance <= chaseRange;
    }

    @Override
    public void stop(@NotNull GoalResult result) {
        controller.getNavigation().stop();
        currentTarget = null;
        state = AttackState.IDLE;
    }

    @Override
    public int getFlags() {
        return GoalFlag.MOVEMENT.getValue() | GoalFlag.LOOK.getValue() | GoalFlag.ATTACK.getValue();
    }

    /**
     * Gets the attack range.
     *
     * @return the attack range
     */
    public double getAttackRange() {
        return attackRange;
    }

    /**
     * Gets the attack cooldown.
     *
     * @return the cooldown in ticks
     */
    public int getAttackCooldown() {
        return attackCooldown;
    }

    /**
     * Gets the current attack state.
     *
     * @return the attack state
     */
    @NotNull
    public AttackState getState() {
        return state;
    }

    /**
     * Checks if the attack is on cooldown.
     *
     * @return true if on cooldown
     */
    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }

    /**
     * Gets the current target.
     *
     * @return the current target, or null
     */
    @Nullable
    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Creates a new builder for attack goals.
     *
     * @return the builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Attack state enumeration.
     */
    public enum AttackState {
        /**
         * Not attacking.
         */
        IDLE,

        /**
         * Chasing the target.
         */
        CHASING,

        /**
         * Attacking the target.
         */
        ATTACKING
    }

    /**
     * Builder for creating attack goals.
     */
    public static class Builder {
        private Supplier<LivingEntity> targetSupplier;
        private double attackRange = 2.0;
        private int attackCooldown = 20;
        private double damage = 2.0;
        private double chaseRange = 16.0;
        private double chaseSpeed = 1.0;
        private boolean requireLineOfSight = true;
        private Consumer<LivingEntity> attackCallback;
        private int pathfindInterval = 10;

        /**
         * Sets the target entity.
         *
         * @param target the target entity
         * @return this builder
         */
        @NotNull
        public Builder target(@NotNull LivingEntity target) {
            this.targetSupplier = () -> target;
            return this;
        }

        /**
         * Sets the target supplier.
         *
         * @param supplier the target supplier
         * @return this builder
         */
        @NotNull
        public Builder targetSupplier(@NotNull Supplier<LivingEntity> supplier) {
            this.targetSupplier = supplier;
            return this;
        }

        /**
         * Sets the attack range.
         *
         * @param range the attack range in blocks
         * @return this builder
         */
        @NotNull
        public Builder attackRange(double range) {
            this.attackRange = range;
            return this;
        }

        /**
         * Sets the attack cooldown.
         *
         * @param ticks the cooldown in ticks
         * @return this builder
         */
        @NotNull
        public Builder attackCooldown(int ticks) {
            this.attackCooldown = ticks;
            return this;
        }

        /**
         * Sets the damage per attack.
         *
         * @param damage the damage amount
         * @return this builder
         */
        @NotNull
        public Builder damage(double damage) {
            this.damage = damage;
            return this;
        }

        /**
         * Sets the chase range.
         *
         * @param range the chase range in blocks
         * @return this builder
         */
        @NotNull
        public Builder chaseRange(double range) {
            this.chaseRange = range;
            return this;
        }

        /**
         * Sets the chase speed.
         *
         * @param speed the speed multiplier
         * @return this builder
         */
        @NotNull
        public Builder chaseSpeed(double speed) {
            this.chaseSpeed = speed;
            return this;
        }

        /**
         * Sets whether line of sight is required.
         *
         * @param require true to require line of sight
         * @return this builder
         */
        @NotNull
        public Builder requireLineOfSight(boolean require) {
            this.requireLineOfSight = require;
            return this;
        }

        /**
         * Sets the attack callback.
         *
         * @param callback the callback to run on attack
         * @return this builder
         */
        @NotNull
        public Builder onAttack(@NotNull Consumer<LivingEntity> callback) {
            this.attackCallback = callback;
            return this;
        }

        /**
         * Sets the pathfinding interval.
         *
         * @param ticks the interval in ticks
         * @return this builder
         */
        @NotNull
        public Builder pathfindInterval(int ticks) {
            this.pathfindInterval = ticks;
            return this;
        }

        /**
         * Builds the attack goal.
         *
         * @return the attack goal
         */
        @NotNull
        public AttackGoal build() {
            return new AttackGoal(
                    targetSupplier,
                    attackRange,
                    attackCooldown,
                    damage,
                    chaseRange,
                    chaseSpeed,
                    requireLineOfSight,
                    attackCallback,
                    pathfindInterval
            );
        }
    }
}
