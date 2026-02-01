package sh.pcx.unified.world.ai.goals;

import sh.pcx.unified.world.ai.core.AIController;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A goal that combines multiple sub-goals into a single composite goal.
 *
 * <p>CompositeGoal allows complex behaviors to be built from simpler goals.
 * It supports different execution modes for running sub-goals in sequence,
 * parallel, or with custom logic.</p>
 *
 * <h2>Execution Modes:</h2>
 * <ul>
 *   <li><b>SEQUENCE:</b> Run goals one after another until all succeed or one fails</li>
 *   <li><b>PARALLEL:</b> Run all goals simultaneously</li>
 *   <li><b>SELECTOR:</b> Run goals until one succeeds</li>
 *   <li><b>RANDOM:</b> Run a random goal from the list</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * CompositeGoal patrolAndGuard = CompositeGoal.builder("patrol_guard")
 *     .name("Patrol and Guard")
 *     .mode(ExecutionMode.SEQUENCE)
 *     .addGoal(new PatrolGoal(waypoints))
 *     .addGoal(new GuardGoal(position))
 *     .loop(true)
 *     .build();
 *
 * controller.getGoalSelector().addGoal(patrolAndGuard, GoalPriority.NORMAL);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see AIGoal
 * @see GoalSelector
 */
public class CompositeGoal implements AIGoal {

    private final String identifier;
    private final String name;
    private final List<AIGoal> subGoals;
    private final ExecutionMode mode;
    private final boolean loop;

    private AIController controller;
    private int currentIndex;
    private boolean[] runningGoals;

    /**
     * Creates a new composite goal.
     *
     * @param identifier the unique identifier
     * @param name the display name
     * @param subGoals the sub-goals to execute
     * @param mode the execution mode
     * @param loop whether to loop after completion
     */
    protected CompositeGoal(
            @NotNull String identifier,
            @NotNull String name,
            @NotNull List<AIGoal> subGoals,
            @NotNull ExecutionMode mode,
            boolean loop
    ) {
        this.identifier = identifier;
        this.name = name;
        this.subGoals = new ArrayList<>(subGoals);
        this.mode = mode;
        this.loop = loop;
        this.currentIndex = 0;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public void initialize(@NotNull AIController controller) {
        this.controller = controller;
        this.runningGoals = new boolean[subGoals.size()];
        for (AIGoal goal : subGoals) {
            goal.initialize(controller);
        }
    }

    @Override
    public boolean canStart() {
        if (subGoals.isEmpty()) {
            return false;
        }

        return switch (mode) {
            case SEQUENCE, RANDOM -> subGoals.get(0).canStart();
            case PARALLEL -> subGoals.stream().anyMatch(AIGoal::canStart);
            case SELECTOR -> subGoals.stream().anyMatch(AIGoal::canStart);
        };
    }

    @Override
    public void start() {
        currentIndex = 0;
        runningGoals = new boolean[subGoals.size()];

        switch (mode) {
            case SEQUENCE -> {
                if (!subGoals.isEmpty() && subGoals.get(0).canStart()) {
                    subGoals.get(0).start();
                    runningGoals[0] = true;
                }
            }
            case PARALLEL -> {
                for (int i = 0; i < subGoals.size(); i++) {
                    if (subGoals.get(i).canStart()) {
                        subGoals.get(i).start();
                        runningGoals[i] = true;
                    }
                }
            }
            case SELECTOR -> {
                for (int i = 0; i < subGoals.size(); i++) {
                    if (subGoals.get(i).canStart()) {
                        subGoals.get(i).start();
                        runningGoals[i] = true;
                        currentIndex = i;
                        break;
                    }
                }
            }
            case RANDOM -> {
                currentIndex = (int) (Math.random() * subGoals.size());
                if (subGoals.get(currentIndex).canStart()) {
                    subGoals.get(currentIndex).start();
                    runningGoals[currentIndex] = true;
                }
            }
        }
    }

    @Override
    @NotNull
    public GoalResult tick() {
        return switch (mode) {
            case SEQUENCE -> tickSequence();
            case PARALLEL -> tickParallel();
            case SELECTOR -> tickSelector();
            case RANDOM -> tickRandom();
        };
    }

    /**
     * Ticks the composite goal in sequence mode.
     *
     * @return the tick result
     */
    @NotNull
    private GoalResult tickSequence() {
        if (currentIndex >= subGoals.size()) {
            if (loop) {
                currentIndex = 0;
                start();
                return GoalResult.RUNNING;
            }
            return GoalResult.SUCCESS;
        }

        AIGoal current = subGoals.get(currentIndex);

        if (!runningGoals[currentIndex]) {
            if (current.canStart()) {
                current.start();
                runningGoals[currentIndex] = true;
            } else {
                return GoalResult.failure("Sub-goal cannot start: " + current.getName());
            }
        }

        GoalResult result = current.tick();

        if (result.isComplete()) {
            current.stop(result);
            runningGoals[currentIndex] = false;

            if (result.isFailure()) {
                return result;
            }

            currentIndex++;

            if (currentIndex >= subGoals.size()) {
                if (loop) {
                    currentIndex = 0;
                    return GoalResult.RUNNING;
                }
                return GoalResult.SUCCESS;
            }
        }

        return GoalResult.RUNNING;
    }

    /**
     * Ticks the composite goal in parallel mode.
     *
     * @return the tick result
     */
    @NotNull
    private GoalResult tickParallel() {
        boolean anyRunning = false;
        boolean anyFailed = false;

        for (int i = 0; i < subGoals.size(); i++) {
            if (!runningGoals[i]) {
                continue;
            }

            AIGoal goal = subGoals.get(i);
            GoalResult result = goal.tick();

            if (result.isComplete()) {
                goal.stop(result);
                runningGoals[i] = false;

                if (result.isFailure()) {
                    anyFailed = true;
                }
            } else {
                anyRunning = true;
            }
        }

        if (anyFailed) {
            stopAllRunning(GoalResult.failure("Parallel sub-goal failed"));
            return GoalResult.FAILURE;
        }

        if (!anyRunning) {
            if (loop) {
                start();
                return GoalResult.RUNNING;
            }
            return GoalResult.SUCCESS;
        }

        return GoalResult.RUNNING;
    }

    /**
     * Ticks the composite goal in selector mode.
     *
     * @return the tick result
     */
    @NotNull
    private GoalResult tickSelector() {
        if (!runningGoals[currentIndex]) {
            // Find next goal that can start
            for (int i = 0; i < subGoals.size(); i++) {
                if (subGoals.get(i).canStart()) {
                    subGoals.get(i).start();
                    runningGoals[i] = true;
                    currentIndex = i;
                    break;
                }
            }

            if (!runningGoals[currentIndex]) {
                return GoalResult.failure("No sub-goal can start");
            }
        }

        AIGoal current = subGoals.get(currentIndex);
        GoalResult result = current.tick();

        if (result.isComplete()) {
            current.stop(result);
            runningGoals[currentIndex] = false;

            if (result.isSuccess()) {
                if (loop) {
                    return GoalResult.RUNNING;
                }
                return GoalResult.SUCCESS;
            }

            // Try next goal
            for (int i = currentIndex + 1; i < subGoals.size(); i++) {
                if (subGoals.get(i).canStart()) {
                    subGoals.get(i).start();
                    runningGoals[i] = true;
                    currentIndex = i;
                    return GoalResult.RUNNING;
                }
            }

            return GoalResult.failure("All sub-goals failed");
        }

        return GoalResult.RUNNING;
    }

    /**
     * Ticks the composite goal in random mode.
     *
     * @return the tick result
     */
    @NotNull
    private GoalResult tickRandom() {
        if (!runningGoals[currentIndex]) {
            // Pick a new random goal
            List<Integer> available = new ArrayList<>();
            for (int i = 0; i < subGoals.size(); i++) {
                if (subGoals.get(i).canStart()) {
                    available.add(i);
                }
            }

            if (available.isEmpty()) {
                return GoalResult.failure("No sub-goal can start");
            }

            currentIndex = available.get((int) (Math.random() * available.size()));
            subGoals.get(currentIndex).start();
            runningGoals[currentIndex] = true;
        }

        AIGoal current = subGoals.get(currentIndex);
        GoalResult result = current.tick();

        if (result.isComplete()) {
            current.stop(result);
            runningGoals[currentIndex] = false;

            if (loop) {
                return GoalResult.RUNNING;
            }
            return result;
        }

        return GoalResult.RUNNING;
    }

    /**
     * Stops all running sub-goals.
     *
     * @param result the result to pass to stopped goals
     */
    private void stopAllRunning(@NotNull GoalResult result) {
        for (int i = 0; i < subGoals.size(); i++) {
            if (runningGoals[i]) {
                subGoals.get(i).stop(result);
                runningGoals[i] = false;
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        for (int i = 0; i < subGoals.size(); i++) {
            if (runningGoals[i] && !subGoals.get(i).shouldContinue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void stop(@NotNull GoalResult result) {
        stopAllRunning(result);
        currentIndex = 0;
    }

    @Override
    public void onInterrupted() {
        for (int i = 0; i < subGoals.size(); i++) {
            if (runningGoals[i]) {
                subGoals.get(i).onInterrupted();
            }
        }
    }

    @Override
    public void reset() {
        stopAllRunning(GoalResult.CANCELLED);
        currentIndex = 0;
        for (AIGoal goal : subGoals) {
            goal.reset();
        }
    }

    /**
     * Gets the sub-goals of this composite goal.
     *
     * @return an unmodifiable list of sub-goals
     */
    @NotNull
    public List<AIGoal> getSubGoals() {
        return Collections.unmodifiableList(subGoals);
    }

    /**
     * Gets the execution mode.
     *
     * @return the execution mode
     */
    @NotNull
    public ExecutionMode getMode() {
        return mode;
    }

    /**
     * Checks if this goal loops after completion.
     *
     * @return true if looping
     */
    public boolean isLooping() {
        return loop;
    }

    /**
     * Gets the current sub-goal index.
     *
     * @return the current index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Creates a new builder for composite goals.
     *
     * @param identifier the unique identifier
     * @return the builder
     */
    @NotNull
    public static Builder builder(@NotNull String identifier) {
        return new Builder(identifier);
    }

    /**
     * Execution mode for composite goals.
     */
    public enum ExecutionMode {
        /**
         * Run goals one after another until all succeed or one fails.
         */
        SEQUENCE,

        /**
         * Run all goals simultaneously.
         */
        PARALLEL,

        /**
         * Run goals until one succeeds.
         */
        SELECTOR,

        /**
         * Run a random goal from the list.
         */
        RANDOM
    }

    /**
     * Builder for creating composite goals.
     */
    public static class Builder {
        private final String identifier;
        private String name;
        private final List<AIGoal> subGoals = new ArrayList<>();
        private ExecutionMode mode = ExecutionMode.SEQUENCE;
        private boolean loop = false;

        /**
         * Creates a new builder.
         *
         * @param identifier the unique identifier
         */
        public Builder(@NotNull String identifier) {
            this.identifier = identifier;
            this.name = identifier;
        }

        /**
         * Sets the display name.
         *
         * @param name the display name
         * @return this builder
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds a sub-goal.
         *
         * @param goal the goal to add
         * @return this builder
         */
        @NotNull
        public Builder addGoal(@NotNull AIGoal goal) {
            this.subGoals.add(goal);
            return this;
        }

        /**
         * Adds multiple sub-goals.
         *
         * @param goals the goals to add
         * @return this builder
         */
        @NotNull
        public Builder addGoals(@NotNull AIGoal... goals) {
            Collections.addAll(this.subGoals, goals);
            return this;
        }

        /**
         * Sets the execution mode.
         *
         * @param mode the execution mode
         * @return this builder
         */
        @NotNull
        public Builder mode(@NotNull ExecutionMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Sets whether the goal should loop.
         *
         * @param loop true to loop
         * @return this builder
         */
        @NotNull
        public Builder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        /**
         * Builds the composite goal.
         *
         * @return the composite goal
         */
        @NotNull
        public CompositeGoal build() {
            return new CompositeGoal(identifier, name, subGoals, mode, loop);
        }
    }
}
