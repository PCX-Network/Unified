package sh.pcx.unified.world.ai.goals;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of standard goal priority levels.
 *
 * <p>Goal priority determines which goals take precedence when multiple
 * goals can run. Higher priority goals can interrupt lower priority goals.</p>
 *
 * <h2>Priority Levels:</h2>
 * <ul>
 *   <li><b>CRITICAL (1000):</b> Emergency behaviors like fleeing from death</li>
 *   <li><b>HIGHEST (800):</b> Urgent behaviors like fleeing from threats</li>
 *   <li><b>HIGH (600):</b> Important behaviors like attacking</li>
 *   <li><b>NORMAL (400):</b> Standard behaviors like following</li>
 *   <li><b>LOW (200):</b> Background behaviors like patrolling</li>
 *   <li><b>LOWEST (100):</b> Idle behaviors like wandering</li>
 *   <li><b>NONE (0):</b> Disabled goals</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * GoalSelector selector = controller.getGoalSelector();
 *
 * // Standard priority levels
 * selector.addGoal(new IdleGoal(), GoalPriority.LOWEST);
 * selector.addGoal(new AttackGoal(), GoalPriority.HIGH);
 *
 * // Custom priority values
 * selector.addGoal(new CustomGoal(), 550); // Between NORMAL and HIGH
 *
 * // Create custom priority
 * GoalPriority custom = GoalPriority.custom(750);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see GoalSelector
 * @see AIGoal
 */
public enum GoalPriority {

    /**
     * No priority - goal is disabled.
     */
    NONE(0),

    /**
     * Lowest priority for idle behaviors.
     *
     * <p>Use for: Idle animations, standing still</p>
     */
    LOWEST(100),

    /**
     * Low priority for background behaviors.
     *
     * <p>Use for: Wandering, random movement</p>
     */
    LOW(200),

    /**
     * Normal priority for standard behaviors.
     *
     * <p>Use for: Following, patrolling</p>
     */
    NORMAL(400),

    /**
     * High priority for important behaviors.
     *
     * <p>Use for: Attacking, chasing targets</p>
     */
    HIGH(600),

    /**
     * Highest priority for urgent behaviors.
     *
     * <p>Use for: Fleeing from threats, avoiding danger</p>
     */
    HIGHEST(800),

    /**
     * Critical priority for emergency behaviors.
     *
     * <p>Use for: Fleeing from death, critical self-preservation</p>
     */
    CRITICAL(1000);

    private final int value;

    /**
     * Creates a new priority level.
     *
     * @param value the priority value
     */
    GoalPriority(int value) {
        this.value = value;
    }

    /**
     * Gets the numeric priority value.
     *
     * @return the priority value
     */
    public int getValue() {
        return value;
    }

    /**
     * Checks if this priority is higher than another.
     *
     * @param other the other priority to compare
     * @return true if this priority is higher
     */
    public boolean isHigherThan(@NotNull GoalPriority other) {
        return this.value > other.value;
    }

    /**
     * Checks if this priority is lower than another.
     *
     * @param other the other priority to compare
     * @return true if this priority is lower
     */
    public boolean isLowerThan(@NotNull GoalPriority other) {
        return this.value < other.value;
    }

    /**
     * Checks if this priority is at least as high as another.
     *
     * @param other the other priority to compare
     * @return true if this priority is at least as high
     */
    public boolean isAtLeast(@NotNull GoalPriority other) {
        return this.value >= other.value;
    }

    /**
     * Gets the priority level from a numeric value.
     *
     * <p>Returns the closest standard priority level.</p>
     *
     * @param value the priority value
     * @return the closest priority level
     */
    @NotNull
    public static GoalPriority fromValue(int value) {
        if (value <= 0) return NONE;
        if (value <= 150) return LOWEST;
        if (value <= 300) return LOW;
        if (value <= 500) return NORMAL;
        if (value <= 700) return HIGH;
        if (value <= 900) return HIGHEST;
        return CRITICAL;
    }

    /**
     * Creates a custom priority wrapper with the specified value.
     *
     * <p>This allows for fine-grained priority control between standard levels.</p>
     *
     * @param value the priority value
     * @return a priority holder with the custom value
     */
    @NotNull
    public static CustomPriority custom(int value) {
        return new CustomPriority(value);
    }

    /**
     * Holder for custom priority values.
     */
    public static class CustomPriority {
        private final int value;

        /**
         * Creates a custom priority with the specified value.
         *
         * @param value the priority value
         */
        public CustomPriority(int value) {
            this.value = Math.max(0, value);
        }

        /**
         * Gets the priority value.
         *
         * @return the priority value
         */
        public int getValue() {
            return value;
        }

        /**
         * Gets the closest standard priority level.
         *
         * @return the closest standard priority
         */
        @NotNull
        public GoalPriority toStandard() {
            return GoalPriority.fromValue(value);
        }

        /**
         * Checks if this custom priority is higher than a standard priority.
         *
         * @param priority the standard priority to compare
         * @return true if this is higher
         */
        public boolean isHigherThan(@NotNull GoalPriority priority) {
            return this.value > priority.getValue();
        }

        /**
         * Checks if this custom priority is lower than a standard priority.
         *
         * @param priority the standard priority to compare
         * @return true if this is lower
         */
        public boolean isLowerThan(@NotNull GoalPriority priority) {
            return this.value < priority.getValue();
        }
    }
}
