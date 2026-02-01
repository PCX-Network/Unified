/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.behavior;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Behavior tree for advanced AI control.
 *
 * <p>Behavior trees provide a flexible and powerful way to define complex
 * AI behaviors. They support composites (selectors, sequences), decorators,
 * conditions, and actions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * BehaviorTree tree = BehaviorTree.builder()
 *     .selector()  // Try children until one succeeds
 *         .sequence()  // All must succeed
 *             .condition("hasTarget", e -> e.getTarget() != null)
 *             .condition("inRange", e -> e.getTarget().getLocation().distance(e.getLocation()) < 3)
 *             .action("attack", e -> {
 *                 e.swingHand(Hand.MAIN_HAND);
 *                 e.getTarget().damage(5.0, e);
 *                 return BehaviorStatus.SUCCESS;
 *             })
 *         .end()
 *         .sequence()
 *             .condition("hasTarget", e -> e.getTarget() != null)
 *             .action("chase", e -> {
 *                 ai.getNavigation(e).moveTo(e.getTarget(), 1.2);
 *                 return BehaviorStatus.RUNNING;
 *             })
 *         .end()
 *         .action("idle", e -> {
 *             // Random idle behavior
 *             return BehaviorStatus.SUCCESS;
 *         })
 *     .end()
 *     .build();
 *
 * ai.setBehaviorTree(entity, tree);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BehaviorStatus
 * @see BehaviorNode
 */
public interface BehaviorTree {

    /**
     * Creates a new behavior tree builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new BehaviorTreeBuilder();
    }

    /**
     * Gets the root node of this tree.
     *
     * @return the root node
     * @since 1.0.0
     */
    @NotNull
    BehaviorNode getRoot();

    /**
     * Ticks this behavior tree for an entity.
     *
     * @param entity the entity to tick
     * @return the result status
     * @since 1.0.0
     */
    @NotNull
    BehaviorStatus tick(@NotNull Object entity);

    /**
     * Resets this behavior tree to its initial state.
     *
     * @since 1.0.0
     */
    void reset();

    /**
     * Gets the name of this behavior tree.
     *
     * @return the name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Checks if this tree is currently running.
     *
     * @return true if running
     * @since 1.0.0
     */
    boolean isRunning();

    /**
     * Gets the currently running node.
     *
     * @return the running node, or null if none
     * @since 1.0.0
     */
    BehaviorNode getRunningNode();

    /**
     * Builder for creating behavior trees.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the tree name.
         *
         * @param name the name
         * @return this builder
         */
        @NotNull
        Builder name(@NotNull String name);

        /**
         * Starts a selector composite node.
         *
         * <p>Selectors try children in order until one succeeds.
         *
         * @return this builder
         */
        @NotNull
        Builder selector();

        /**
         * Starts a named selector composite node.
         *
         * @param name the node name
         * @return this builder
         */
        @NotNull
        Builder selector(@NotNull String name);

        /**
         * Starts a sequence composite node.
         *
         * <p>Sequences run children in order until one fails.
         *
         * @return this builder
         */
        @NotNull
        Builder sequence();

        /**
         * Starts a named sequence composite node.
         *
         * @param name the node name
         * @return this builder
         */
        @NotNull
        Builder sequence(@NotNull String name);

        /**
         * Starts a parallel composite node.
         *
         * <p>Parallel nodes run all children simultaneously.
         *
         * @param successPolicy the success policy
         * @param failurePolicy the failure policy
         * @return this builder
         */
        @NotNull
        Builder parallel(@NotNull ParallelPolicy successPolicy, @NotNull ParallelPolicy failurePolicy);

        /**
         * Ends the current composite node.
         *
         * @return this builder
         */
        @NotNull
        Builder end();

        /**
         * Adds a condition node.
         *
         * @param name      the condition name
         * @param condition the condition predicate
         * @return this builder
         */
        @NotNull
        Builder condition(@NotNull String name, @NotNull Predicate<Object> condition);

        /**
         * Adds an action node.
         *
         * @param name   the action name
         * @param action the action function
         * @return this builder
         */
        @NotNull
        Builder action(@NotNull String name, @NotNull Function<Object, BehaviorStatus> action);

        /**
         * Adds a wait node.
         *
         * @param name  the node name
         * @param ticks the number of ticks to wait
         * @return this builder
         */
        @NotNull
        Builder wait(@NotNull String name, int ticks);

        /**
         * Adds a random wait node.
         *
         * @param name     the node name
         * @param minTicks minimum ticks
         * @param maxTicks maximum ticks
         * @return this builder
         */
        @NotNull
        Builder waitRandom(@NotNull String name, int minTicks, int maxTicks);

        /**
         * Adds a succeeder decorator.
         *
         * <p>Always returns SUCCESS regardless of child result.
         *
         * @return this builder
         */
        @NotNull
        Builder succeeder();

        /**
         * Adds an inverter decorator.
         *
         * <p>Inverts the child's result (SUCCESS -> FAILURE).
         *
         * @return this builder
         */
        @NotNull
        Builder inverter();

        /**
         * Adds a repeat decorator.
         *
         * @param times the number of times to repeat
         * @return this builder
         */
        @NotNull
        Builder repeat(int times);

        /**
         * Adds a repeat until fail decorator.
         *
         * @return this builder
         */
        @NotNull
        Builder repeatUntilFail();

        /**
         * Adds a limiter decorator.
         *
         * @param maxExecutions maximum executions before failure
         * @return this builder
         */
        @NotNull
        Builder limit(int maxExecutions);

        /**
         * Adds a cooldown decorator.
         *
         * @param ticks cooldown in ticks
         * @return this builder
         */
        @NotNull
        Builder cooldown(int ticks);

        /**
         * Adds a timeout decorator.
         *
         * @param ticks timeout in ticks
         * @return this builder
         */
        @NotNull
        Builder timeout(int ticks);

        /**
         * Adds a sub-tree reference.
         *
         * @param tree the sub-tree
         * @return this builder
         */
        @NotNull
        Builder subTree(@NotNull BehaviorTree tree);

        /**
         * Builds the behavior tree.
         *
         * @return the built tree
         */
        @NotNull
        BehaviorTree build();
    }

    /**
     * Policy for parallel node completion.
     *
     * @since 1.0.0
     */
    enum ParallelPolicy {
        /** Require all children to match for this result. */
        REQUIRE_ALL,
        /** Require only one child to match for this result. */
        REQUIRE_ONE
    }
}
