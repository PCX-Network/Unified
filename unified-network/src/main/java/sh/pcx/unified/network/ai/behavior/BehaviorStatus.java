/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.behavior;

/**
 * Status returned by behavior tree nodes.
 *
 * <p>Each node in a behavior tree returns one of these statuses
 * to indicate its execution result.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BehaviorTree
 * @see BehaviorNode
 */
public enum BehaviorStatus {

    /**
     * The node completed successfully.
     *
     * <p>For composites:
     * <ul>
     *   <li>Selector: moves to next child or returns SUCCESS if this was the successful one</li>
     *   <li>Sequence: continues to next child or returns SUCCESS if this was the last</li>
     * </ul>
     */
    SUCCESS,

    /**
     * The node failed to complete.
     *
     * <p>For composites:
     * <ul>
     *   <li>Selector: tries the next child or returns FAILURE if no more children</li>
     *   <li>Sequence: returns FAILURE immediately</li>
     * </ul>
     */
    FAILURE,

    /**
     * The node is still running and needs more ticks.
     *
     * <p>The tree will resume from this node on the next tick.
     */
    RUNNING;

    /**
     * Checks if this status indicates completion (success or failure).
     *
     * @return true if completed
     * @since 1.0.0
     */
    public boolean isComplete() {
        return this == SUCCESS || this == FAILURE;
    }

    /**
     * Checks if this status indicates success.
     *
     * @return true if success
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * Checks if this status indicates failure.
     *
     * @return true if failure
     * @since 1.0.0
     */
    public boolean isFailure() {
        return this == FAILURE;
    }

    /**
     * Checks if this status indicates running.
     *
     * @return true if running
     * @since 1.0.0
     */
    public boolean isRunning() {
        return this == RUNNING;
    }

    /**
     * Inverts the status.
     *
     * <p>SUCCESS becomes FAILURE and vice versa.
     * RUNNING remains RUNNING.
     *
     * @return the inverted status
     * @since 1.0.0
     */
    public BehaviorStatus invert() {
        return switch (this) {
            case SUCCESS -> FAILURE;
            case FAILURE -> SUCCESS;
            case RUNNING -> RUNNING;
        };
    }
}
