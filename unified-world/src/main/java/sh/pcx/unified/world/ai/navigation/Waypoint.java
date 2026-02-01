package sh.pcx.unified.world.ai.navigation;

import sh.pcx.unified.world.ai.core.AIController;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a waypoint in a patrol or navigation path.
 *
 * <p>A Waypoint defines a location that an entity should navigate to,
 * optionally with a wait time, custom speed, and action to execute
 * upon arrival.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Simple waypoint
 * Waypoint point1 = new Waypoint(location);
 *
 * // Waypoint with wait time
 * Waypoint point2 = new Waypoint(location, 60); // Wait 3 seconds
 *
 * // Waypoint with action using builder
 * Waypoint point3 = Waypoint.builder(location)
 *     .waitTime(100)
 *     .speed(0.5)
 *     .onArrive(controller -> {
 *         // Do something when arrived
 *     })
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class Waypoint {

    private final Location location;
    private final int waitTime;
    private final double speed;
    private final Consumer<AIController> action;
    private final String name;

    /**
     * Creates a waypoint at the specified location.
     *
     * @param location the waypoint location
     */
    public Waypoint(@NotNull Location location) {
        this(location, 0, -1, null, null);
    }

    /**
     * Creates a waypoint with a wait time.
     *
     * @param location the waypoint location
     * @param waitTime the wait time in ticks
     */
    public Waypoint(@NotNull Location location, int waitTime) {
        this(location, waitTime, -1, null, null);
    }

    /**
     * Creates a waypoint with all parameters.
     *
     * @param location the waypoint location
     * @param waitTime the wait time in ticks
     * @param speed the movement speed, or -1 for default
     * @param action the action to execute on arrival
     * @param name the waypoint name
     */
    protected Waypoint(
            @NotNull Location location,
            int waitTime,
            double speed,
            @Nullable Consumer<AIController> action,
            @Nullable String name
    ) {
        this.location = location.clone();
        this.waitTime = waitTime;
        this.speed = speed;
        this.action = action;
        this.name = name;
    }

    /**
     * Gets the waypoint location.
     *
     * @return the location (cloned)
     */
    @NotNull
    public Location getLocation() {
        return location.clone();
    }

    /**
     * Gets the wait time at this waypoint.
     *
     * @return the wait time in ticks
     */
    public int getWaitTime() {
        return waitTime;
    }

    /**
     * Gets the movement speed to this waypoint.
     *
     * @return the speed, or -1 if using default
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Gets the action to execute on arrival.
     *
     * @return the optional action
     */
    @NotNull
    public Optional<Consumer<AIController>> getAction() {
        return Optional.ofNullable(action);
    }

    /**
     * Gets the waypoint name.
     *
     * @return the optional name
     */
    @NotNull
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Creates a new builder for waypoints.
     *
     * @param location the waypoint location
     * @return the builder
     */
    @NotNull
    public static Builder builder(@NotNull Location location) {
        return new Builder(location);
    }

    /**
     * Builder for creating waypoints.
     */
    public static class Builder {
        private final Location location;
        private int waitTime = 0;
        private double speed = -1;
        private Consumer<AIController> action;
        private String name;

        /**
         * Creates a new builder.
         *
         * @param location the waypoint location
         */
        public Builder(@NotNull Location location) {
            this.location = location.clone();
        }

        /**
         * Sets the wait time.
         *
         * @param ticks the wait time in ticks
         * @return this builder
         */
        @NotNull
        public Builder waitTime(int ticks) {
            this.waitTime = ticks;
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
         * Sets the arrival action.
         *
         * @param action the action to execute
         * @return this builder
         */
        @NotNull
        public Builder onArrive(@NotNull Consumer<AIController> action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the waypoint name.
         *
         * @param name the name
         * @return this builder
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds the waypoint.
         *
         * @return the waypoint
         */
        @NotNull
        public Waypoint build() {
            return new Waypoint(location, waitTime, speed, action, name);
        }
    }
}
