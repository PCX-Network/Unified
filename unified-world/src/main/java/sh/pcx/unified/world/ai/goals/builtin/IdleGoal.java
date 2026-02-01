package sh.pcx.unified.world.ai.goals.builtin;

import sh.pcx.unified.world.ai.core.AIController;
import sh.pcx.unified.world.ai.goals.AIGoal;
import sh.pcx.unified.world.ai.goals.GoalResult;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A goal that represents idle behavior when no other goals are active.
 *
 * <p>The entity will perform idle behaviors such as looking around,
 * occasional small movements, and playing idle animations. This goal
 * has the lowest priority and serves as a fallback behavior.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Configurable idle duration</li>
 *   <li>Random look direction changes</li>
 *   <li>Optional idle animations</li>
 *   <li>Custom idle callbacks</li>
 *   <li>Periodic idle sounds</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * IdleGoal goal = IdleGoal.builder()
 *     .lookAround(true)
 *     .lookInterval(40, 100)
 *     .onIdle(controller -> {
 *         // Play idle animation or sound
 *     })
 *     .build();
 *
 * controller.getGoalSelector().addGoal(goal, GoalPriority.LOWEST);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class IdleGoal implements AIGoal {

    private static final String IDENTIFIER = "builtin:idle";
    private static final String NAME = "Idle";

    private final boolean lookAround;
    private final int minLookInterval;
    private final int maxLookInterval;
    private final float maxPitchChange;
    private final float maxYawChange;
    private final Consumer<AIController> idleCallback;
    private final int callbackInterval;

    private AIController controller;
    private int ticksUntilLook;
    private int ticksUntilCallback;
    private Location startLocation;

    /**
     * Creates a new idle goal.
     *
     * @param lookAround whether to look around
     * @param minLookInterval minimum ticks between look changes
     * @param maxLookInterval maximum ticks between look changes
     * @param maxPitchChange maximum pitch change per look
     * @param maxYawChange maximum yaw change per look
     * @param idleCallback optional callback when idling
     * @param callbackInterval ticks between callback invocations
     */
    protected IdleGoal(
            boolean lookAround,
            int minLookInterval,
            int maxLookInterval,
            float maxPitchChange,
            float maxYawChange,
            Consumer<AIController> idleCallback,
            int callbackInterval
    ) {
        this.lookAround = lookAround;
        this.minLookInterval = minLookInterval;
        this.maxLookInterval = maxLookInterval;
        this.maxPitchChange = maxPitchChange;
        this.maxYawChange = maxYawChange;
        this.idleCallback = idleCallback;
        this.callbackInterval = callbackInterval;
    }

    /**
     * Creates a simple idle goal with default settings.
     */
    public IdleGoal() {
        this(true, 40, 100, 30f, 60f, null, 0);
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
        // Idle can always start as a fallback
        return true;
    }

    @Override
    public void start() {
        startLocation = controller.getEntity().getLocation().clone();
        ticksUntilLook = randomLookInterval();
        ticksUntilCallback = callbackInterval;

        // Stop any movement
        controller.getNavigation().stop();
    }

    @Override
    @NotNull
    public GoalResult tick() {
        // Look around behavior
        if (lookAround) {
            ticksUntilLook--;
            if (ticksUntilLook <= 0) {
                performLookAround();
                ticksUntilLook = randomLookInterval();
            }
        }

        // Idle callback
        if (idleCallback != null && callbackInterval > 0) {
            ticksUntilCallback--;
            if (ticksUntilCallback <= 0) {
                idleCallback.accept(controller);
                ticksUntilCallback = callbackInterval;
            }
        }

        return GoalResult.RUNNING;
    }

    /**
     * Performs a random look around action.
     */
    private void performLookAround() {
        Location loc = controller.getEntity().getLocation();

        float currentYaw = loc.getYaw();
        float currentPitch = loc.getPitch();

        // Random yaw change
        float yawChange = (float) ((Math.random() - 0.5) * 2 * maxYawChange);
        float newYaw = currentYaw + yawChange;

        // Random pitch change (limited to reasonable range)
        float pitchChange = (float) ((Math.random() - 0.5) * 2 * maxPitchChange);
        float newPitch = Math.max(-60, Math.min(60, currentPitch + pitchChange));

        loc.setYaw(newYaw);
        loc.setPitch(newPitch);

        // Apply the look direction
        controller.getEntity().teleport(loc);
    }

    /**
     * Gets a random look interval.
     *
     * @return the random interval in ticks
     */
    private int randomLookInterval() {
        return minLookInterval + (int) (Math.random() * (maxLookInterval - minLookInterval));
    }

    @Override
    public boolean shouldContinue() {
        // Idle continues until interrupted by another goal
        return true;
    }

    @Override
    public void stop(@NotNull GoalResult result) {
        // Nothing to clean up
    }

    @Override
    public boolean canBeInterrupted() {
        return true; // Idle should always be interruptible
    }

    @Override
    public int getFlags() {
        return GoalFlag.LOOK.getValue();
    }

    /**
     * Gets the start location where idling began.
     *
     * @return the start location
     */
    public Location getStartLocation() {
        return startLocation != null ? startLocation.clone() : null;
    }

    /**
     * Checks if the entity is looking around.
     *
     * @return true if looking around
     */
    public boolean isLookingAround() {
        return lookAround;
    }

    /**
     * Creates a new builder for idle goals.
     *
     * @return the builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating idle goals.
     */
    public static class Builder {
        private boolean lookAround = true;
        private int minLookInterval = 40;
        private int maxLookInterval = 100;
        private float maxPitchChange = 30f;
        private float maxYawChange = 60f;
        private Consumer<AIController> idleCallback;
        private int callbackInterval = 0;

        /**
         * Sets whether to look around.
         *
         * @param lookAround true to look around
         * @return this builder
         */
        @NotNull
        public Builder lookAround(boolean lookAround) {
            this.lookAround = lookAround;
            return this;
        }

        /**
         * Sets the look interval range.
         *
         * @param min minimum ticks between looks
         * @param max maximum ticks between looks
         * @return this builder
         */
        @NotNull
        public Builder lookInterval(int min, int max) {
            this.minLookInterval = min;
            this.maxLookInterval = max;
            return this;
        }

        /**
         * Sets the maximum pitch change per look.
         *
         * @param maxPitch the maximum pitch change in degrees
         * @return this builder
         */
        @NotNull
        public Builder maxPitchChange(float maxPitch) {
            this.maxPitchChange = maxPitch;
            return this;
        }

        /**
         * Sets the maximum yaw change per look.
         *
         * @param maxYaw the maximum yaw change in degrees
         * @return this builder
         */
        @NotNull
        public Builder maxYawChange(float maxYaw) {
            this.maxYawChange = maxYaw;
            return this;
        }

        /**
         * Sets the idle callback.
         *
         * @param callback the callback to run while idling
         * @return this builder
         */
        @NotNull
        public Builder onIdle(@NotNull Consumer<AIController> callback) {
            this.idleCallback = callback;
            return this;
        }

        /**
         * Sets the callback interval.
         *
         * @param ticks the interval between callbacks
         * @return this builder
         */
        @NotNull
        public Builder callbackInterval(int ticks) {
            this.callbackInterval = ticks;
            return this;
        }

        /**
         * Builds the idle goal.
         *
         * @return the idle goal
         */
        @NotNull
        public IdleGoal build() {
            return new IdleGoal(
                    lookAround,
                    minLookInterval,
                    maxLookInterval,
                    maxPitchChange,
                    maxYawChange,
                    idleCallback,
                    callbackInterval
            );
        }
    }
}
