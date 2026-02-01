package sh.pcx.unified.world.ai.goals.builtin;

import sh.pcx.unified.world.ai.core.AIController;
import sh.pcx.unified.world.ai.goals.AIGoal;
import sh.pcx.unified.world.ai.goals.GoalResult;
import sh.pcx.unified.world.ai.navigation.PathResult;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A goal that causes the entity to flee from a threat.
 *
 * <p>The entity will move away from the threat, attempting to put
 * distance between itself and the danger. Supports various flee
 * strategies and panic behaviors.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Configurable flee distance and speed</li>
 *   <li>Dynamic threat detection via supplier or filter</li>
 *   <li>Multiple flee strategies</li>
 *   <li>Panic mode with erratic movement</li>
 *   <li>Safe distance threshold</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * FleeGoal goal = FleeGoal.builder()
 *     .threatFilter(entity -> entity instanceof Player)
 *     .fleeDistance(16.0)
 *     .safeDistance(24.0)
 *     .speed(1.5)
 *     .panicMode(true)
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class FleeGoal implements AIGoal {

    private static final String IDENTIFIER = "builtin:flee";
    private static final String NAME = "Flee";

    private final Supplier<LivingEntity> threatSupplier;
    private final Predicate<Entity> threatFilter;
    private final double fleeDistance;
    private final double safeDistance;
    private final double speed;
    private final boolean panicMode;
    private final FleeStrategy strategy;
    private final int pathfindInterval;

    private AIController controller;
    private LivingEntity currentThreat;
    private int ticksSincePathfind;
    private int panicChangeTicks;
    private Location fleeTarget;

    /**
     * Creates a new flee goal.
     *
     * @param threatSupplier supplier for the threat entity
     * @param threatFilter predicate to filter threats
     * @param fleeDistance minimum distance to flee
     * @param safeDistance distance considered safe
     * @param speed movement speed multiplier
     * @param panicMode whether to use panic behavior
     * @param strategy the flee strategy
     * @param pathfindInterval ticks between pathfinding updates
     */
    protected FleeGoal(
            @Nullable Supplier<LivingEntity> threatSupplier,
            @Nullable Predicate<Entity> threatFilter,
            double fleeDistance,
            double safeDistance,
            double speed,
            boolean panicMode,
            @NotNull FleeStrategy strategy,
            int pathfindInterval
    ) {
        this.threatSupplier = threatSupplier;
        this.threatFilter = threatFilter;
        this.fleeDistance = fleeDistance;
        this.safeDistance = safeDistance;
        this.speed = speed;
        this.panicMode = panicMode;
        this.strategy = strategy;
        this.pathfindInterval = pathfindInterval;
    }

    /**
     * Creates a simple flee goal from a specific threat.
     *
     * @param threat the threat to flee from
     */
    public FleeGoal(@NotNull LivingEntity threat) {
        this(() -> threat, null, 10.0, 16.0, 1.5, false, FleeStrategy.DIRECT, 5);
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
        LivingEntity threat = findThreat();

        if (threat == null) {
            return false;
        }

        if (!threat.getWorld().equals(controller.getEntity().getWorld())) {
            return false;
        }

        double distance = controller.getEntity().getLocation()
                .distance(threat.getLocation());

        if (distance > fleeDistance) {
            return false;
        }

        this.currentThreat = threat;
        return true;
    }

    /**
     * Finds the current threat.
     *
     * @return the threat entity, or null if none
     */
    @Nullable
    private LivingEntity findThreat() {
        // Check supplier first
        if (threatSupplier != null) {
            LivingEntity threat = threatSupplier.get();
            if (threat != null && threat.isValid() && !threat.isDead()) {
                return threat;
            }
        }

        // Check filter for nearby entities
        if (threatFilter != null) {
            return controller.getEntity().getNearbyEntities(fleeDistance, fleeDistance, fleeDistance)
                    .stream()
                    .filter(e -> e instanceof LivingEntity)
                    .filter(threatFilter)
                    .map(e -> (LivingEntity) e)
                    .filter(e -> e.isValid() && !e.isDead())
                    .min((a, b) -> {
                        double distA = a.getLocation().distanceSquared(controller.getEntity().getLocation());
                        double distB = b.getLocation().distanceSquared(controller.getEntity().getLocation());
                        return Double.compare(distA, distB);
                    })
                    .orElse(null);
        }

        return null;
    }

    @Override
    public void start() {
        ticksSincePathfind = pathfindInterval;
        panicChangeTicks = 0;
        fleeTarget = null;
    }

    @Override
    @NotNull
    public GoalResult tick() {
        // Update threat reference
        if (threatFilter != null) {
            currentThreat = findThreat();
        }

        if (currentThreat == null || !currentThreat.isValid() || currentThreat.isDead()) {
            return GoalResult.success("Threat eliminated");
        }

        if (!currentThreat.getWorld().equals(controller.getEntity().getWorld())) {
            return GoalResult.success("Threat in different world");
        }

        double distance = controller.getEntity().getLocation()
                .distance(currentThreat.getLocation());

        // Check if safe
        if (distance >= safeDistance) {
            return GoalResult.success("Reached safe distance");
        }

        // Update flee path
        ticksSincePathfind++;
        if (panicMode) {
            panicChangeTicks--;
        }

        if (ticksSincePathfind >= pathfindInterval || (panicMode && panicChangeTicks <= 0)) {
            ticksSincePathfind = 0;
            updateFleePath();
        }

        return GoalResult.RUNNING;
    }

    /**
     * Updates the flee path based on strategy.
     */
    private void updateFleePath() {
        if (currentThreat == null) {
            return;
        }

        Location entityLoc = controller.getEntity().getLocation();
        Location threatLoc = currentThreat.getLocation();

        fleeTarget = switch (strategy) {
            case DIRECT -> calculateDirectFlee(entityLoc, threatLoc);
            case DIAGONAL -> calculateDiagonalFlee(entityLoc, threatLoc);
            case RANDOM -> calculateRandomFlee(entityLoc, threatLoc);
            case COVER -> calculateCoverFlee(entityLoc, threatLoc);
        };

        if (panicMode) {
            // Add randomness for panic
            fleeTarget.add(
                    (Math.random() - 0.5) * 4,
                    0,
                    (Math.random() - 0.5) * 4
            );
            panicChangeTicks = 10 + (int) (Math.random() * 10);
        }

        double actualSpeed = panicMode ? speed * 1.3 : speed;
        PathResult result = controller.getNavigation().pathTo(fleeTarget, actualSpeed);

        if (!result.isSuccess() && !result.isPartial()) {
            // Try alternate direction
            fleeTarget = calculateRandomFlee(entityLoc, threatLoc);
            controller.getNavigation().pathTo(fleeTarget, actualSpeed);
        }
    }

    /**
     * Calculates a direct flee location.
     *
     * @param entityLoc the entity location
     * @param threatLoc the threat location
     * @return the flee target location
     */
    @NotNull
    private Location calculateDirectFlee(@NotNull Location entityLoc, @NotNull Location threatLoc) {
        // Move directly away from threat
        double dx = entityLoc.getX() - threatLoc.getX();
        double dz = entityLoc.getZ() - threatLoc.getZ();

        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 0) {
            dx /= length;
            dz /= length;
        }

        return entityLoc.clone().add(dx * safeDistance, 0, dz * safeDistance);
    }

    /**
     * Calculates a diagonal flee location.
     *
     * @param entityLoc the entity location
     * @param threatLoc the threat location
     * @return the flee target location
     */
    @NotNull
    private Location calculateDiagonalFlee(@NotNull Location entityLoc, @NotNull Location threatLoc) {
        double dx = entityLoc.getX() - threatLoc.getX();
        double dz = entityLoc.getZ() - threatLoc.getZ();

        // Rotate 45 degrees
        double angle = Math.atan2(dz, dx) + (Math.random() > 0.5 ? Math.PI / 4 : -Math.PI / 4);
        dx = Math.cos(angle);
        dz = Math.sin(angle);

        return entityLoc.clone().add(dx * safeDistance, 0, dz * safeDistance);
    }

    /**
     * Calculates a random flee location.
     *
     * @param entityLoc the entity location
     * @param threatLoc the threat location
     * @return the flee target location
     */
    @NotNull
    private Location calculateRandomFlee(@NotNull Location entityLoc, @NotNull Location threatLoc) {
        double angle = Math.random() * 2 * Math.PI;

        // Prefer directions away from threat
        double threatAngle = Math.atan2(
                entityLoc.getZ() - threatLoc.getZ(),
                entityLoc.getX() - threatLoc.getX()
        );

        // Bias towards away direction
        angle = threatAngle + (Math.random() - 0.5) * Math.PI;

        double dx = Math.cos(angle);
        double dz = Math.sin(angle);

        return entityLoc.clone().add(dx * safeDistance, 0, dz * safeDistance);
    }

    /**
     * Calculates a flee location seeking cover.
     *
     * @param entityLoc the entity location
     * @param threatLoc the threat location
     * @return the flee target location
     */
    @NotNull
    private Location calculateCoverFlee(@NotNull Location entityLoc, @NotNull Location threatLoc) {
        // For now, use direct flee - cover detection would require block checking
        // This could be enhanced to find nearby solid blocks to hide behind
        return calculateDirectFlee(entityLoc, threatLoc);
    }

    @Override
    public boolean shouldContinue() {
        if (currentThreat == null || !currentThreat.isValid() || currentThreat.isDead()) {
            return false;
        }

        double distance = controller.getEntity().getLocation()
                .distance(currentThreat.getLocation());

        return distance < safeDistance;
    }

    @Override
    public void stop(@NotNull GoalResult result) {
        controller.getNavigation().stop();
        currentThreat = null;
        fleeTarget = null;
    }

    @Override
    public boolean canBeInterrupted() {
        return false; // Flee should not be interrupted
    }

    @Override
    public int getFlags() {
        return GoalFlag.MOVEMENT.getValue() | GoalFlag.LOOK.getValue();
    }

    /**
     * Gets the flee distance.
     *
     * @return the flee distance
     */
    public double getFleeDistance() {
        return fleeDistance;
    }

    /**
     * Gets the safe distance.
     *
     * @return the safe distance
     */
    public double getSafeDistance() {
        return safeDistance;
    }

    /**
     * Checks if panic mode is enabled.
     *
     * @return true if in panic mode
     */
    public boolean isPanicMode() {
        return panicMode;
    }

    /**
     * Gets the current threat.
     *
     * @return the current threat, or null
     */
    @Nullable
    public LivingEntity getCurrentThreat() {
        return currentThreat;
    }

    /**
     * Creates a new builder for flee goals.
     *
     * @return the builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Flee strategy enumeration.
     */
    public enum FleeStrategy {
        /**
         * Move directly away from threat.
         */
        DIRECT,

        /**
         * Move diagonally away from threat.
         */
        DIAGONAL,

        /**
         * Move in random directions biased away.
         */
        RANDOM,

        /**
         * Seek cover while fleeing.
         */
        COVER
    }

    /**
     * Builder for creating flee goals.
     */
    public static class Builder {
        private Supplier<LivingEntity> threatSupplier;
        private Predicate<Entity> threatFilter;
        private double fleeDistance = 10.0;
        private double safeDistance = 16.0;
        private double speed = 1.5;
        private boolean panicMode = false;
        private FleeStrategy strategy = FleeStrategy.DIRECT;
        private int pathfindInterval = 5;

        /**
         * Sets the threat entity.
         *
         * @param threat the threat entity
         * @return this builder
         */
        @NotNull
        public Builder threat(@NotNull LivingEntity threat) {
            this.threatSupplier = () -> threat;
            return this;
        }

        /**
         * Sets the threat supplier.
         *
         * @param supplier the threat supplier
         * @return this builder
         */
        @NotNull
        public Builder threatSupplier(@NotNull Supplier<LivingEntity> supplier) {
            this.threatSupplier = supplier;
            return this;
        }

        /**
         * Sets the threat filter.
         *
         * @param filter the threat filter predicate
         * @return this builder
         */
        @NotNull
        public Builder threatFilter(@NotNull Predicate<Entity> filter) {
            this.threatFilter = filter;
            return this;
        }

        /**
         * Sets the flee distance.
         *
         * @param distance the distance to trigger fleeing
         * @return this builder
         */
        @NotNull
        public Builder fleeDistance(double distance) {
            this.fleeDistance = distance;
            return this;
        }

        /**
         * Sets the safe distance.
         *
         * @param distance the distance considered safe
         * @return this builder
         */
        @NotNull
        public Builder safeDistance(double distance) {
            this.safeDistance = distance;
            return this;
        }

        /**
         * Sets the movement speed.
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
         * Sets whether to use panic mode.
         *
         * @param panic true for panic mode
         * @return this builder
         */
        @NotNull
        public Builder panicMode(boolean panic) {
            this.panicMode = panic;
            return this;
        }

        /**
         * Sets the flee strategy.
         *
         * @param strategy the flee strategy
         * @return this builder
         */
        @NotNull
        public Builder strategy(@NotNull FleeStrategy strategy) {
            this.strategy = strategy;
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
         * Builds the flee goal.
         *
         * @return the flee goal
         */
        @NotNull
        public FleeGoal build() {
            return new FleeGoal(
                    threatSupplier,
                    threatFilter,
                    fleeDistance,
                    safeDistance,
                    speed,
                    panicMode,
                    strategy,
                    pathfindInterval
            );
        }
    }
}
