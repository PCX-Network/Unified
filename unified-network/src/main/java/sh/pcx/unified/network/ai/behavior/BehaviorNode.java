/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.behavior;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A node in a behavior tree.
 *
 * <p>Behavior trees are composed of nodes that can be:
 * <ul>
 *   <li>Composites: Nodes with children (Selector, Sequence, Parallel)</li>
 *   <li>Decorators: Nodes that modify a single child's behavior</li>
 *   <li>Leaves: Action or condition nodes with no children</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BehaviorTree
 * @see BehaviorStatus
 */
public interface BehaviorNode {

    /**
     * Gets the name of this node.
     *
     * @return the node name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Gets the type of this node.
     *
     * @return the node type
     * @since 1.0.0
     */
    @NotNull
    NodeType getType();

    /**
     * Ticks this node for an entity.
     *
     * @param entity the entity
     * @return the execution status
     * @since 1.0.0
     */
    @NotNull
    BehaviorStatus tick(@NotNull Object entity);

    /**
     * Called when the node starts execution.
     *
     * @param entity the entity
     * @since 1.0.0
     */
    void onStart(@NotNull Object entity);

    /**
     * Called when the node finishes execution.
     *
     * @param entity the entity
     * @param status the final status
     * @since 1.0.0
     */
    void onFinish(@NotNull Object entity, @NotNull BehaviorStatus status);

    /**
     * Resets this node to its initial state.
     *
     * @since 1.0.0
     */
    void reset();

    /**
     * Gets the parent node.
     *
     * @return the parent, or null if root
     * @since 1.0.0
     */
    @Nullable
    BehaviorNode getParent();

    /**
     * Gets the child nodes.
     *
     * @return the children, or empty list for leaf nodes
     * @since 1.0.0
     */
    @NotNull
    List<BehaviorNode> getChildren();

    /**
     * Checks if this node has children.
     *
     * @return true if has children
     * @since 1.0.0
     */
    default boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    /**
     * Gets the current status of this node.
     *
     * @return the current status
     * @since 1.0.0
     */
    @NotNull
    BehaviorStatus getStatus();

    /**
     * Checks if this node is currently running.
     *
     * @return true if running
     * @since 1.0.0
     */
    default boolean isRunning() {
        return getStatus() == BehaviorStatus.RUNNING;
    }

    /**
     * Node types.
     *
     * @since 1.0.0
     */
    enum NodeType {
        // Composites
        /** Tries children until one succeeds. */
        SELECTOR,
        /** Runs children until one fails. */
        SEQUENCE,
        /** Runs all children simultaneously. */
        PARALLEL,
        /** Selects a random child. */
        RANDOM_SELECTOR,
        /** Runs children in random order. */
        RANDOM_SEQUENCE,

        // Decorators
        /** Always succeeds. */
        SUCCEEDER,
        /** Inverts child result. */
        INVERTER,
        /** Repeats child N times. */
        REPEATER,
        /** Repeats until failure. */
        REPEAT_UNTIL_FAIL,
        /** Limits executions. */
        LIMITER,
        /** Adds cooldown between runs. */
        COOLDOWN,
        /** Fails after timeout. */
        TIMEOUT,

        // Leaves
        /** Checks a condition. */
        CONDITION,
        /** Executes an action. */
        ACTION,
        /** Waits for ticks. */
        WAIT,
        /** References a sub-tree. */
        SUB_TREE
    }
}
