package sh.pcx.unified.world.ai.goals.builtin;

import sh.pcx.unified.world.ai.core.AIController;
import sh.pcx.unified.world.ai.goals.AIGoal;
import sh.pcx.unified.world.ai.goals.GoalResult;
import sh.pcx.unified.world.ai.navigation.PathResult;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A goal that causes the entity to follow a target entity.
 *
 * <p>The entity will continuously pathfind to the target entity,
 * maintaining a specified distance. The goal can be configured
 * with minimum and maximum follow distances, and speed modifiers.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Configurable follow distance range</li>
 *   <li>Dynamic target via supplier or direct reference</li>
 *   <li>Speed adjustment based on distance</li>
 *   <li>Line of sight requirements</li>
 *   <li>Teleport on excessive distance</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Follow a specific player
 * FollowEntityGoal goal = FollowEntityGoal.builder()
 *     .target(player)
 *     .minDistance(2.0)
 *     .maxDistance(10.0)
 *     .speed(1.0)
 *     .teleportDistance(32.0)
 *     .build();
 *
 * // Follow dynamically selected target
 * FollowEntityGoal dynamicGoal = FollowEntityGoal.builder()
 *     .targetSupplier(() -> controller.getTarget().orElse(null))
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class FollowEntityGoal implements AIGoal {

    private static final String IDENTIFIER = "builtin:follow_entity";
    private static final String NAME = "Follow Entity";

    private final Supplier<LivingEntity> targetSupplier;
    private final double minDistance;
    private final double maxDistance;
    private final double speed;
    private final double teleportDistance;
    private final boolean requireLineOfSight;
    private final int pathfindInterval;

    private AIController controller;
    private LivingEntity currentTarget;
    private int ticksSincePathfind;
    private boolean isFollowing;

    /**
     * Creates a new follow entity goal.
     *
     * @param targetSupplier supplier for the target entity
     * @param minDistance minimum follow distance
     * @param maxDistance maximum follow distance
     * @param speed movement speed multiplier
     * @param teleportDistance distance at which to teleport to target
     * @param requireLineOfSight whether line of sight is required
     * @param pathfindInterval ticks between pathfinding updates
     */
    protected FollowEntityGoal(
            @NotNull Supplier<LivingEntity> targetSupplier,
            double minDistance,
            double maxDistance,
            double speed,
            double teleportDistance,
            boolean requireLineOfSight,
            int pathfindInterval
    ) {
        this.targetSupplier = targetSupplier;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.speed = speed;
        this.teleportDistance = teleportDistance;
        this.requireLineOfSight = requireLineOfSight;
        this.pathfindInterval = pathfindInterval;
    }

    /**
     * Creates a simple follow entity goal for a static target.
     *
     * @param target the target entity to follow
     */
    public FollowEntityGoal(@NotNull LivingEntity target) {
        this(() -> target, 2.0, 10.0, 1.0, 32.0, false, 10);
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
        LivingEntity target = targetSupplier.get();

        if (target == null || !target.isValid() || target.isDead()) {
            return false;
        }

        if (!target.getWorld().equals(controller.getEntity().getWorld())) {
            return false;
        }

        double distance = controller.getEntity().getLocation()
                .distance(target.getLocation());

        if (distance <= minDistance) {
            return false;
        }

        if (requireLineOfSight && !controller.getEntity().hasLineOfSight(target)) {
            return false;
        }

        this.currentTarget = target;
        return true;
    }

    @Override
    public void start() {
        ticksSincePathfind = pathfindInterval; // Force immediate pathfind
        isFollowing = true;
    }

    @Override
    @NotNull
    public GoalResult tick() {
        if (currentTarget == null || !currentTarget.isValid() || currentTarget.isDead()) {
            return GoalResult.failure("Target is no longer valid");
        }

        if (!currentTarget.getWorld().equals(controller.getEntity().getWorld())) {
            return GoalResult.failure("Target is in different world");
        }

        double distance = controller.getEntity().getLocation()
                .distance(currentTarget.getLocation());

        // Check for teleport
        if (teleportDistance > 0 && distance > teleportDistance) {
            teleportToTarget();
            return GoalResult.RUNNING;
        }

        // Already at destination
        if (distance <= minDistance) {
            controller.getNavigation().stop();
            return GoalResult.success("Reached target");
        }

        // Update pathfinding
        ticksSincePathfind++;
        if (ticksSincePathfind >= pathfindInterval) {
            ticksSincePathfind = 0;
            updatePath();
        }

        return GoalResult.RUNNING;
    }

    /**
     * Updates the pathfinding to the target.
     */
    private void updatePath() {
        if (currentTarget == null) {
            return;
        }

        double adjustedSpeed = calculateSpeed();

        PathResult result = controller.getNavigation().pathTo(
                currentTarget.getLocation(),
                adjustedSpeed
        );

        if (!result.isSuccess() && !result.isPartial()) {
            // Path failed, try to get closer by other means
            isFollowing = false;
        } else {
            isFollowing = true;
        }
    }

    /**
     * Calculates the movement speed based on distance.
     *
     * @return the adjusted speed
     */
    private double calculateSpeed() {
        if (currentTarget == null) {
            return speed;
        }

        double distance = controller.getEntity().getLocation()
                .distance(currentTarget.getLocation());

        // Speed up when far away
        if (distance > maxDistance) {
            return speed * 1.5;
        }

        // Slow down when close
        if (distance < minDistance * 2) {
            return speed * 0.7;
        }

        return speed;
    }

    /**
     * Teleports the entity near the target.
     */
    private void teleportToTarget() {
        if (currentTarget == null) {
            return;
        }

        // Find a safe location near the target
        controller.getEntity().teleport(
                currentTarget.getLocation().add(
                        (Math.random() - 0.5) * 2,
                        0,
                        (Math.random() - 0.5) * 2
                )
        );

        ticksSincePathfind = pathfindInterval; // Force pathfind next tick
    }

    @Override
    public boolean shouldContinue() {
        if (currentTarget == null || !currentTarget.isValid() || currentTarget.isDead()) {
            return false;
        }

        double distance = controller.getEntity().getLocation()
                .distance(currentTarget.getLocation());

        // Stop if we're close enough
        if (distance <= minDistance) {
            return false;
        }

        // Continue if target is within reasonable range
        return distance <= teleportDistance * 2 || teleportDistance <= 0;
    }

    @Override
    public void stop(@NotNull GoalResult result) {
        controller.getNavigation().stop();
        currentTarget = null;
        isFollowing = false;
    }

    @Override
    public int getFlags() {
        return GoalFlag.MOVEMENT.getValue() | GoalFlag.LOOK.getValue();
    }

    /**
     * Gets the current target entity.
     *
     * @return the current target, or null if not following
     */
    @Nullable
    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Gets the minimum follow distance.
     *
     * @return the minimum distance
     */
    public double getMinDistance() {
        return minDistance;
    }

    /**
     * Gets the maximum follow distance.
     *
     * @return the maximum distance
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Checks if the entity is currently following.
     *
     * @return true if following
     */
    public boolean isFollowing() {
        return isFollowing;
    }

    /**
     * Creates a new builder for follow entity goals.
     *
     * @return the builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating follow entity goals.
     */
    public static class Builder {
        private Supplier<LivingEntity> targetSupplier;
        private double minDistance = 2.0;
        private double maxDistance = 10.0;
        private double speed = 1.0;
        private double teleportDistance = 32.0;
        private boolean requireLineOfSight = false;
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
         * Sets the target supplier for dynamic targeting.
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
         * Sets the minimum follow distance.
         *
         * @param distance the minimum distance in blocks
         * @return this builder
         */
        @NotNull
        public Builder minDistance(double distance) {
            this.minDistance = distance;
            return this;
        }

        /**
         * Sets the maximum follow distance.
         *
         * @param distance the maximum distance in blocks
         * @return this builder
         */
        @NotNull
        public Builder maxDistance(double distance) {
            this.maxDistance = distance;
            return this;
        }

        /**
         * Sets the movement speed multiplier.
         *
         * @param speed the speed multiplier
         * @return this builder
         */
        @NotNull
        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        /**
         * Sets the teleport distance.
         *
         * @param distance the distance at which to teleport, or 0 to disable
         * @return this builder
         */
        @NotNull
        public Builder teleportDistance(double distance) {
            this.teleportDistance = distance;
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
         * Sets the pathfinding update interval.
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
         * Builds the follow entity goal.
         *
         * @return the follow entity goal
         * @throws IllegalStateException if no target is set
         */
        @NotNull
        public FollowEntityGoal build() {
            if (targetSupplier == null) {
                throw new IllegalStateException("Target or target supplier must be set");
            }

            return new FollowEntityGoal(
                    targetSupplier,
                    minDistance,
                    maxDistance,
                    speed,
                    teleportDistance,
                    requireLineOfSight,
                    pathfindInterval
            );
        }
    }
}
