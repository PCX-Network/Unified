package sh.pcx.unified.world.ai.goals.builtin;

import sh.pcx.unified.world.ai.core.AIController;
import sh.pcx.unified.world.ai.goals.AIGoal;
import sh.pcx.unified.world.ai.goals.GoalResult;
import sh.pcx.unified.world.ai.navigation.PathResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A goal that causes the entity to wander randomly.
 *
 * <p>The entity will move to random nearby locations, creating natural
 * roaming behavior. The goal can be configured with distance limits,
 * home location tethering, and terrain preferences.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Configurable wander radius</li>
 *   <li>Optional home location tethering</li>
 *   <li>Wait time between movements</li>
 *   <li>Terrain awareness</li>
 *   <li>Movement speed control</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * WanderGoal goal = WanderGoal.builder()
 *     .radius(16.0)
 *     .homeLocation(spawnPoint)
 *     .maxHomeDistance(32.0)
 *     .speed(0.8)
 *     .waitTime(60, 200)
 *     .avoidWater(true)
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class WanderGoal implements AIGoal {

    private static final String IDENTIFIER = "builtin:wander";
    private static final String NAME = "Wander";

    private final double radius;
    private final Location homeLocation;
    private final double maxHomeDistance;
    private final double speed;
    private final int minWaitTime;
    private final int maxWaitTime;
    private final boolean avoidWater;
    private final boolean avoidLava;
    private final int maxAttempts;

    private AIController controller;
    private Location targetLocation;
    private WanderState state;
    private int waitTicks;
    private int pathfindAttempts;

    /**
     * Creates a new wander goal.
     *
     * @param radius the wander radius
     * @param homeLocation optional home location
     * @param maxHomeDistance maximum distance from home
     * @param speed the movement speed
     * @param minWaitTime minimum wait between movements
     * @param maxWaitTime maximum wait between movements
     * @param avoidWater whether to avoid water
     * @param avoidLava whether to avoid lava
     * @param maxAttempts maximum pathfinding attempts
     */
    protected WanderGoal(
            double radius,
            @Nullable Location homeLocation,
            double maxHomeDistance,
            double speed,
            int minWaitTime,
            int maxWaitTime,
            boolean avoidWater,
            boolean avoidLava,
            int maxAttempts
    ) {
        this.radius = radius;
        this.homeLocation = homeLocation != null ? homeLocation.clone() : null;
        this.maxHomeDistance = maxHomeDistance;
        this.speed = speed;
        this.minWaitTime = minWaitTime;
        this.maxWaitTime = maxWaitTime;
        this.avoidWater = avoidWater;
        this.avoidLava = avoidLava;
        this.maxAttempts = maxAttempts;
        this.state = WanderState.IDLE;
    }

    /**
     * Creates a simple wander goal with default settings.
     */
    public WanderGoal() {
        this(10.0, null, 0, 0.8, 60, 200, true, true, 10);
    }

    /**
     * Creates a wander goal with the specified radius.
     *
     * @param radius the wander radius
     */
    public WanderGoal(double radius) {
        this(radius, null, 0, 0.8, 60, 200, true, true, 10);
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
        return true; // Wander can always start as background behavior
    }

    @Override
    public void start() {
        state = WanderState.WAITING;
        waitTicks = randomWaitTime();
        targetLocation = null;
        pathfindAttempts = 0;
    }

    @Override
    @NotNull
    public GoalResult tick() {
        return switch (state) {
            case IDLE -> {
                state = WanderState.WAITING;
                waitTicks = randomWaitTime();
                yield GoalResult.RUNNING;
            }
            case WAITING -> tickWaiting();
            case MOVING -> tickMoving();
        };
    }

    /**
     * Ticks the waiting state.
     *
     * @return the tick result
     */
    @NotNull
    private GoalResult tickWaiting() {
        waitTicks--;

        if (waitTicks <= 0) {
            if (findWanderTarget()) {
                state = WanderState.MOVING;
            } else {
                // Couldn't find target, wait again
                waitTicks = randomWaitTime() / 2;
            }
        }

        return GoalResult.RUNNING;
    }

    /**
     * Ticks the moving state.
     *
     * @return the tick result
     */
    @NotNull
    private GoalResult tickMoving() {
        if (targetLocation == null) {
            state = WanderState.WAITING;
            waitTicks = randomWaitTime();
            return GoalResult.RUNNING;
        }

        double distance = controller.getEntity().getLocation()
                .distance(targetLocation);

        // Arrived at target
        if (distance <= 1.5) {
            controller.getNavigation().stop();
            state = WanderState.WAITING;
            waitTicks = randomWaitTime();
            targetLocation = null;
            return GoalResult.RUNNING;
        }

        // Check if navigation is stuck
        if (!controller.isMoving()) {
            pathfindAttempts++;
            if (pathfindAttempts >= maxAttempts) {
                // Give up on this target
                state = WanderState.WAITING;
                waitTicks = randomWaitTime() / 2;
                targetLocation = null;
                pathfindAttempts = 0;
            } else {
                // Try to path again
                controller.getNavigation().pathTo(targetLocation, speed);
            }
        }

        return GoalResult.RUNNING;
    }

    /**
     * Finds a valid wander target location.
     *
     * @return true if a valid target was found
     */
    private boolean findWanderTarget() {
        Location entityLoc = controller.getEntity().getLocation();
        Location baseLocation = homeLocation != null ? homeLocation : entityLoc;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random offset
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;

            double dx = Math.cos(angle) * distance;
            double dz = Math.sin(angle) * distance;

            Location target = entityLoc.clone().add(dx, 0, dz);

            // Check home distance constraint
            if (homeLocation != null) {
                double homeDistance = target.distance(homeLocation);
                if (homeDistance > maxHomeDistance) {
                    continue;
                }
            }

            // Find solid ground
            target = findSolidGround(target);
            if (target == null) {
                continue;
            }

            // Check terrain restrictions
            if (!isValidLocation(target)) {
                continue;
            }

            // Try to path to target
            PathResult result = controller.getNavigation().pathTo(target, speed);
            if (result.isSuccess() || result.isPartial()) {
                targetLocation = target;
                pathfindAttempts = 0;
                return true;
            }
        }

        return false;
    }

    /**
     * Finds solid ground at or near the given location.
     *
     * @param location the starting location
     * @return a location on solid ground, or null if not found
     */
    @Nullable
    private Location findSolidGround(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Search up and down for solid ground
        for (int dy = 0; dy < 5; dy++) {
            // Check below
            Block below = world.getBlockAt(x, y - dy, z);
            Block at = world.getBlockAt(x, y - dy + 1, z);
            Block above = world.getBlockAt(x, y - dy + 2, z);

            if (below.getType().isSolid() && !at.getType().isSolid() && !above.getType().isSolid()) {
                return new Location(world, x + 0.5, y - dy + 1, z + 0.5);
            }

            // Check above
            below = world.getBlockAt(x, y + dy, z);
            at = world.getBlockAt(x, y + dy + 1, z);
            above = world.getBlockAt(x, y + dy + 2, z);

            if (below.getType().isSolid() && !at.getType().isSolid() && !above.getType().isSolid()) {
                return new Location(world, x + 0.5, y + dy + 1, z + 0.5);
            }
        }

        return null;
    }

    /**
     * Checks if a location is valid for wandering.
     *
     * @param location the location to check
     * @return true if valid
     */
    private boolean isValidLocation(@NotNull Location location) {
        Block block = location.getBlock();

        if (avoidWater && block.isLiquid()) {
            return false;
        }

        if (avoidLava && block.getType().name().contains("LAVA")) {
            return false;
        }

        return true;
    }

    /**
     * Gets a random wait time.
     *
     * @return the random wait time in ticks
     */
    private int randomWaitTime() {
        return minWaitTime + (int) (Math.random() * (maxWaitTime - minWaitTime));
    }

    @Override
    public boolean shouldContinue() {
        return true; // Wander continues as background behavior
    }

    @Override
    public void stop(@NotNull GoalResult result) {
        controller.getNavigation().stop();
        state = WanderState.IDLE;
        targetLocation = null;
    }

    @Override
    public boolean canBeInterrupted() {
        return true;
    }

    @Override
    public int getFlags() {
        return GoalFlag.MOVEMENT.getValue();
    }

    /**
     * Gets the wander radius.
     *
     * @return the radius in blocks
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Gets the home location.
     *
     * @return the home location, or null if not set
     */
    @Nullable
    public Location getHomeLocation() {
        return homeLocation != null ? homeLocation.clone() : null;
    }

    /**
     * Gets the current wander state.
     *
     * @return the wander state
     */
    @NotNull
    public WanderState getState() {
        return state;
    }

    /**
     * Gets the current target location.
     *
     * @return the target location, or null if not moving
     */
    @Nullable
    public Location getTargetLocation() {
        return targetLocation != null ? targetLocation.clone() : null;
    }

    /**
     * Creates a new builder for wander goals.
     *
     * @return the builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Wander state enumeration.
     */
    public enum WanderState {
        /**
         * Not wandering.
         */
        IDLE,

        /**
         * Waiting before next movement.
         */
        WAITING,

        /**
         * Moving to target.
         */
        MOVING
    }

    /**
     * Builder for creating wander goals.
     */
    public static class Builder {
        private double radius = 10.0;
        private Location homeLocation;
        private double maxHomeDistance = 32.0;
        private double speed = 0.8;
        private int minWaitTime = 60;
        private int maxWaitTime = 200;
        private boolean avoidWater = true;
        private boolean avoidLava = true;
        private int maxAttempts = 10;

        /**
         * Sets the wander radius.
         *
         * @param radius the radius in blocks
         * @return this builder
         */
        @NotNull
        public Builder radius(double radius) {
            this.radius = radius;
            return this;
        }

        /**
         * Sets the home location.
         *
         * @param location the home location
         * @return this builder
         */
        @NotNull
        public Builder homeLocation(@NotNull Location location) {
            this.homeLocation = location;
            return this;
        }

        /**
         * Sets the maximum distance from home.
         *
         * @param distance the maximum distance in blocks
         * @return this builder
         */
        @NotNull
        public Builder maxHomeDistance(double distance) {
            this.maxHomeDistance = distance;
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
         * Sets the wait time range.
         *
         * @param min minimum wait time in ticks
         * @param max maximum wait time in ticks
         * @return this builder
         */
        @NotNull
        public Builder waitTime(int min, int max) {
            this.minWaitTime = min;
            this.maxWaitTime = max;
            return this;
        }

        /**
         * Sets whether to avoid water.
         *
         * @param avoid true to avoid water
         * @return this builder
         */
        @NotNull
        public Builder avoidWater(boolean avoid) {
            this.avoidWater = avoid;
            return this;
        }

        /**
         * Sets whether to avoid lava.
         *
         * @param avoid true to avoid lava
         * @return this builder
         */
        @NotNull
        public Builder avoidLava(boolean avoid) {
            this.avoidLava = avoid;
            return this;
        }

        /**
         * Sets the maximum pathfinding attempts.
         *
         * @param attempts the maximum attempts
         * @return this builder
         */
        @NotNull
        public Builder maxAttempts(int attempts) {
            this.maxAttempts = attempts;
            return this;
        }

        /**
         * Builds the wander goal.
         *
         * @return the wander goal
         */
        @NotNull
        public WanderGoal build() {
            return new WanderGoal(
                    radius,
                    homeLocation,
                    maxHomeDistance,
                    speed,
                    minWaitTime,
                    maxWaitTime,
                    avoidWater,
                    avoidLava,
                    maxAttempts
            );
        }
    }
}
