package sh.pcx.unified.world.ai.goals;

import sh.pcx.unified.world.ai.core.AIController;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all AI goals.
 *
 * <p>An AIGoal represents a specific behavior or objective that an entity
 * can pursue. Goals are managed by the {@link GoalSelector} which handles
 * priority and execution.</p>
 *
 * <h2>Goal Lifecycle:</h2>
 * <ol>
 *   <li><b>canStart():</b> Checked each tick to see if the goal can begin</li>
 *   <li><b>start():</b> Called when the goal is activated</li>
 *   <li><b>tick():</b> Called each tick while the goal is running</li>
 *   <li><b>shouldContinue():</b> Checked each tick to see if the goal should keep running</li>
 *   <li><b>stop():</b> Called when the goal is deactivated</li>
 * </ol>
 *
 * <h2>Implementation Example:</h2>
 * <pre>{@code
 * public class CustomGoal implements AIGoal {
 *     private AIController controller;
 *
 *     @Override
 *     public void initialize(AIController controller) {
 *         this.controller = controller;
 *     }
 *
 *     @Override
 *     public boolean canStart() {
 *         return controller.getTarget().isPresent();
 *     }
 *
 *     @Override
 *     public void start() {
 *         // Initialize goal state
 *     }
 *
 *     @Override
 *     public GoalResult tick() {
 *         // Execute goal logic
 *         return GoalResult.RUNNING;
 *     }
 *
 *     @Override
 *     public boolean shouldContinue() {
 *         return controller.getTarget().isPresent();
 *     }
 *
 *     @Override
 *     public void stop(GoalResult result) {
 *         // Cleanup goal state
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see GoalSelector
 * @see GoalPriority
 * @see GoalResult
 */
public interface AIGoal {

    /**
     * Gets the unique identifier for this goal type.
     *
     * @return the goal identifier
     */
    @NotNull
    String getIdentifier();

    /**
     * Gets a human-readable name for this goal.
     *
     * @return the goal name
     */
    @NotNull
    String getName();

    /**
     * Initializes the goal with its controller.
     *
     * <p>This method is called once when the goal is added to a goal selector.
     * Use this to store a reference to the controller and perform setup.</p>
     *
     * @param controller the AI controller this goal belongs to
     */
    void initialize(@NotNull AIController controller);

    /**
     * Checks if this goal can start execution.
     *
     * <p>This method is called each tick when the goal is not running
     * to determine if conditions are met for the goal to begin.</p>
     *
     * @return true if the goal can start
     */
    boolean canStart();

    /**
     * Called when the goal starts execution.
     *
     * <p>Use this to initialize any state needed for the goal's execution.</p>
     */
    void start();

    /**
     * Executes a single tick of this goal.
     *
     * <p>This method is called every tick while the goal is active.
     * Return the current execution status.</p>
     *
     * @return the result of this tick's execution
     */
    @NotNull
    GoalResult tick();

    /**
     * Checks if this goal should continue running.
     *
     * <p>This method is called each tick after {@link #tick()} to determine
     * if the goal should keep running. Return false to stop the goal.</p>
     *
     * @return true if the goal should continue, false to stop
     */
    boolean shouldContinue();

    /**
     * Called when the goal stops execution.
     *
     * <p>Use this to clean up any state and reset the goal for potential reuse.</p>
     *
     * @param result the final result of the goal execution
     */
    void stop(@NotNull GoalResult result);

    /**
     * Checks if this goal can be interrupted by another goal.
     *
     * <p>If true, higher priority goals can interrupt this goal.
     * If false, this goal will run to completion.</p>
     *
     * @return true if the goal can be interrupted
     */
    default boolean canBeInterrupted() {
        return true;
    }

    /**
     * Checks if this goal requires exclusive control.
     *
     * <p>If true, no other goals can run simultaneously.
     * If false, compatible goals may run in parallel.</p>
     *
     * @return true if the goal requires exclusive control
     */
    default boolean requiresExclusiveControl() {
        return false;
    }

    /**
     * Gets the goal flags indicating what this goal controls.
     *
     * <p>Goals with overlapping flags cannot run simultaneously.</p>
     *
     * @return the goal flags
     */
    default int getFlags() {
        return GoalFlag.MOVEMENT.getValue() | GoalFlag.LOOK.getValue();
    }

    /**
     * Called when this goal is interrupted by another goal.
     *
     * <p>This provides an opportunity to handle interruption gracefully
     * before {@link #stop(GoalResult)} is called.</p>
     */
    default void onInterrupted() {
        // Default implementation does nothing
    }

    /**
     * Resets the goal to its initial state.
     *
     * <p>Called when the goal needs to be reset without stopping.</p>
     */
    default void reset() {
        // Default implementation does nothing
    }

    /**
     * Gets the cooldown time before this goal can start again.
     *
     * @return the cooldown in ticks
     */
    default int getCooldown() {
        return 0;
    }

    /**
     * Gets the maximum duration this goal can run.
     *
     * @return the maximum duration in ticks, or -1 for unlimited
     */
    default int getMaxDuration() {
        return -1;
    }

    /**
     * Enumeration of goal control flags.
     */
    enum GoalFlag {
        /**
         * Controls entity movement.
         */
        MOVEMENT(1),

        /**
         * Controls entity look direction.
         */
        LOOK(2),

        /**
         * Controls entity jumping.
         */
        JUMP(4),

        /**
         * Controls entity attacks.
         */
        ATTACK(8),

        /**
         * Controls entity target selection.
         */
        TARGET(16);

        private final int value;

        GoalFlag(int value) {
            this.value = value;
        }

        /**
         * Gets the flag value.
         *
         * @return the flag value
         */
        public int getValue() {
            return value;
        }
    }
}
