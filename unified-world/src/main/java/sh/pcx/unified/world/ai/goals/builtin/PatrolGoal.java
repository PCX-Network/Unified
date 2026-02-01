package sh.pcx.unified.world.ai.goals.builtin;

import sh.pcx.unified.world.ai.core.AIController;
import sh.pcx.unified.world.ai.goals.AIGoal;
import sh.pcx.unified.world.ai.goals.GoalResult;
import sh.pcx.unified.world.ai.navigation.PathResult;
import sh.pcx.unified.world.ai.navigation.Waypoint;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A goal that causes the entity to patrol between waypoints.
 *
 * <p>The entity will move between a series of waypoints in order,
 * optionally pausing at each point. Supports multiple patrol modes
 * including loop, ping-pong, and one-way.</p>
 *
 * <h2>Patrol Modes:</h2>
 * <ul>
 *   <li><b>LOOP:</b> Cycle through waypoints continuously</li>
 *   <li><b>PING_PONG:</b> Go back and forth between waypoints</li>
 *   <li><b>ONE_WAY:</b> Patrol once and stop at the end</li>
 *   <li><b>RANDOM:</b> Move to random waypoints</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * List<Waypoint> waypoints = Arrays.asList(
 *     new Waypoint(location1, 60),  // Wait 3 seconds
 *     new Waypoint(location2, 40),  // Wait 2 seconds
 *     new Waypoint(location3, 0)    // No wait
 * );
 *
 * PatrolGoal patrol = PatrolGoal.builder()
 *     .waypoints(waypoints)
 *     .mode(PatrolMode.LOOP)
 *     .speed(1.0)
 *     .arrivalDistance(1.5)
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class PatrolGoal implements AIGoal {

    private static final String IDENTIFIER = "builtin:patrol";
    private static final String NAME = "Patrol";

    private final List<Waypoint> waypoints;
    private final PatrolMode mode;
    private final double speed;
    private final double arrivalDistance;
    private final int pathfindInterval;

    private AIController controller;
    private int currentIndex;
    private boolean movingForward;
    private int waitTicks;
    private int ticksSincePathfind;
    private PatrolState state;

    /**
     * Creates a new patrol goal.
     *
     * @param waypoints the waypoints to patrol
     * @param mode the patrol mode
     * @param speed the movement speed
     * @param arrivalDistance distance considered as "arrived"
     * @param pathfindInterval ticks between pathfinding updates
     */
    protected PatrolGoal(
            @NotNull List<Waypoint> waypoints,
            @NotNull PatrolMode mode,
            double speed,
            double arrivalDistance,
            int pathfindInterval
    ) {
        this.waypoints = new ArrayList<>(waypoints);
        this.mode = mode;
        this.speed = speed;
        this.arrivalDistance = arrivalDistance;
        this.pathfindInterval = pathfindInterval;
        this.currentIndex = 0;
        this.movingForward = true;
        this.state = PatrolState.IDLE;
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
        return !waypoints.isEmpty();
    }

    @Override
    public void start() {
        currentIndex = 0;
        movingForward = true;
        waitTicks = 0;
        ticksSincePathfind = pathfindInterval; // Force immediate pathfind
        state = PatrolState.MOVING;
    }

    @Override
    @NotNull
    public GoalResult tick() {
        if (waypoints.isEmpty()) {
            return GoalResult.failure("No waypoints defined");
        }

        return switch (state) {
            case IDLE -> {
                state = PatrolState.MOVING;
                yield GoalResult.RUNNING;
            }
            case MOVING -> tickMoving();
            case WAITING -> tickWaiting();
            case COMPLETE -> GoalResult.SUCCESS;
        };
    }

    /**
     * Ticks the moving state.
     *
     * @return the tick result
     */
    @NotNull
    private GoalResult tickMoving() {
        Waypoint current = waypoints.get(currentIndex);
        Location targetLocation = current.getLocation();

        if (!targetLocation.getWorld().equals(controller.getEntity().getWorld())) {
            return GoalResult.failure("Waypoint in different world");
        }

        double distance = controller.getEntity().getLocation()
                .distance(targetLocation);

        // Check if arrived
        if (distance <= arrivalDistance) {
            onArrived(current);
            return GoalResult.RUNNING;
        }

        // Update pathfinding
        ticksSincePathfind++;
        if (ticksSincePathfind >= pathfindInterval) {
            ticksSincePathfind = 0;
            pathToWaypoint(current);
        }

        return GoalResult.RUNNING;
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
            if (!moveToNext()) {
                state = PatrolState.COMPLETE;
                return GoalResult.SUCCESS;
            }
            state = PatrolState.MOVING;
            ticksSincePathfind = pathfindInterval; // Force immediate pathfind
        }

        return GoalResult.RUNNING;
    }

    /**
     * Called when the entity arrives at a waypoint.
     *
     * @param waypoint the arrived waypoint
     */
    private void onArrived(@NotNull Waypoint waypoint) {
        controller.getNavigation().stop();

        // Execute waypoint action if any
        waypoint.getAction().ifPresent(action -> action.accept(controller));

        // Start waiting
        int waitTime = waypoint.getWaitTime();
        if (waitTime > 0) {
            waitTicks = waitTime;
            state = PatrolState.WAITING;
        } else {
            if (!moveToNext()) {
                state = PatrolState.COMPLETE;
            }
        }
    }

    /**
     * Moves to the next waypoint.
     *
     * @return true if there is a next waypoint
     */
    private boolean moveToNext() {
        switch (mode) {
            case LOOP -> {
                currentIndex = (currentIndex + 1) % waypoints.size();
                return true;
            }
            case PING_PONG -> {
                if (movingForward) {
                    if (currentIndex >= waypoints.size() - 1) {
                        movingForward = false;
                        currentIndex--;
                    } else {
                        currentIndex++;
                    }
                } else {
                    if (currentIndex <= 0) {
                        movingForward = true;
                        currentIndex++;
                    } else {
                        currentIndex--;
                    }
                }
                return true;
            }
            case ONE_WAY -> {
                if (currentIndex >= waypoints.size() - 1) {
                    return false;
                }
                currentIndex++;
                return true;
            }
            case RANDOM -> {
                if (waypoints.size() <= 1) {
                    return true;
                }
                int newIndex;
                do {
                    newIndex = (int) (Math.random() * waypoints.size());
                } while (newIndex == currentIndex);
                currentIndex = newIndex;
                return true;
            }
        }
        return false;
    }

    /**
     * Pathfinds to a waypoint.
     *
     * @param waypoint the target waypoint
     */
    private void pathToWaypoint(@NotNull Waypoint waypoint) {
        double waypointSpeed = waypoint.getSpeed() > 0 ? waypoint.getSpeed() : speed;

        PathResult result = controller.getNavigation().pathTo(
                waypoint.getLocation(),
                waypointSpeed
        );

        if (!result.isSuccess() && !result.isPartial()) {
            // Skip this waypoint if unreachable
            moveToNext();
        }
    }

    @Override
    public boolean shouldContinue() {
        return state != PatrolState.COMPLETE;
    }

    @Override
    public void stop(@NotNull GoalResult result) {
        controller.getNavigation().stop();
        state = PatrolState.IDLE;
    }

    @Override
    public void reset() {
        currentIndex = 0;
        movingForward = true;
        waitTicks = 0;
        state = PatrolState.IDLE;
    }

    @Override
    public int getFlags() {
        return GoalFlag.MOVEMENT.getValue() | GoalFlag.LOOK.getValue();
    }

    /**
     * Gets the waypoints.
     *
     * @return an unmodifiable list of waypoints
     */
    @NotNull
    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    /**
     * Gets the patrol mode.
     *
     * @return the patrol mode
     */
    @NotNull
    public PatrolMode getMode() {
        return mode;
    }

    /**
     * Gets the current waypoint index.
     *
     * @return the current index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Gets the current patrol state.
     *
     * @return the patrol state
     */
    @NotNull
    public PatrolState getState() {
        return state;
    }

    /**
     * Creates a new builder for patrol goals.
     *
     * @return the builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Patrol mode enumeration.
     */
    public enum PatrolMode {
        /**
         * Cycle through waypoints continuously.
         */
        LOOP,

        /**
         * Go back and forth between waypoints.
         */
        PING_PONG,

        /**
         * Patrol once and stop at the end.
         */
        ONE_WAY,

        /**
         * Move to random waypoints.
         */
        RANDOM
    }

    /**
     * Patrol state enumeration.
     */
    public enum PatrolState {
        /**
         * Not patrolling.
         */
        IDLE,

        /**
         * Moving to waypoint.
         */
        MOVING,

        /**
         * Waiting at waypoint.
         */
        WAITING,

        /**
         * Patrol complete.
         */
        COMPLETE
    }

    /**
     * Builder for creating patrol goals.
     */
    public static class Builder {
        private final List<Waypoint> waypoints = new ArrayList<>();
        private PatrolMode mode = PatrolMode.LOOP;
        private double speed = 1.0;
        private double arrivalDistance = 1.5;
        private int pathfindInterval = 10;

        /**
         * Adds a waypoint.
         *
         * @param waypoint the waypoint to add
         * @return this builder
         */
        @NotNull
        public Builder addWaypoint(@NotNull Waypoint waypoint) {
            this.waypoints.add(waypoint);
            return this;
        }

        /**
         * Adds a waypoint at a location.
         *
         * @param location the waypoint location
         * @return this builder
         */
        @NotNull
        public Builder addWaypoint(@NotNull Location location) {
            this.waypoints.add(new Waypoint(location));
            return this;
        }

        /**
         * Adds a waypoint with wait time.
         *
         * @param location the waypoint location
         * @param waitTicks the wait time in ticks
         * @return this builder
         */
        @NotNull
        public Builder addWaypoint(@NotNull Location location, int waitTicks) {
            this.waypoints.add(new Waypoint(location, waitTicks));
            return this;
        }

        /**
         * Sets all waypoints.
         *
         * @param waypoints the waypoints
         * @return this builder
         */
        @NotNull
        public Builder waypoints(@NotNull List<Waypoint> waypoints) {
            this.waypoints.clear();
            this.waypoints.addAll(waypoints);
            return this;
        }

        /**
         * Sets the patrol mode.
         *
         * @param mode the patrol mode
         * @return this builder
         */
        @NotNull
        public Builder mode(@NotNull PatrolMode mode) {
            this.mode = mode;
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
         * Sets the arrival distance.
         *
         * @param distance the arrival distance in blocks
         * @return this builder
         */
        @NotNull
        public Builder arrivalDistance(double distance) {
            this.arrivalDistance = distance;
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
         * Builds the patrol goal.
         *
         * @return the patrol goal
         */
        @NotNull
        public PatrolGoal build() {
            return new PatrolGoal(waypoints, mode, speed, arrivalDistance, pathfindInterval);
        }
    }
}
