package sh.pcx.unified.world.ai.goals;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a goal execution tick or completion.
 *
 * <p>GoalResult is used to communicate the status of a goal's execution
 * back to the goal selector. It indicates whether the goal is still
 * running, has succeeded, failed, or was cancelled.</p>
 *
 * <h2>Result Types:</h2>
 * <ul>
 *   <li><b>RUNNING:</b> Goal is still executing</li>
 *   <li><b>SUCCESS:</b> Goal completed successfully</li>
 *   <li><b>FAILURE:</b> Goal failed to complete</li>
 *   <li><b>CANCELLED:</b> Goal was cancelled externally</li>
 *   <li><b>INTERRUPTED:</b> Goal was interrupted by a higher priority goal</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Override
 * public GoalResult tick() {
 *     if (targetReached) {
 *         return GoalResult.SUCCESS;
 *     }
 *     if (targetLost) {
 *         return GoalResult.failure("Target lost");
 *     }
 *     if (pathBlocked) {
 *         return GoalResult.failure("Path blocked");
 *     }
 *     return GoalResult.RUNNING;
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see AIGoal
 */
public final class GoalResult {

    /**
     * The goal is still running.
     */
    public static final GoalResult RUNNING = new GoalResult(Status.RUNNING, null);

    /**
     * The goal completed successfully.
     */
    public static final GoalResult SUCCESS = new GoalResult(Status.SUCCESS, null);

    /**
     * The goal failed with no specific reason.
     */
    public static final GoalResult FAILURE = new GoalResult(Status.FAILURE, null);

    /**
     * The goal was cancelled.
     */
    public static final GoalResult CANCELLED = new GoalResult(Status.CANCELLED, null);

    /**
     * The goal was interrupted by another goal.
     */
    public static final GoalResult INTERRUPTED = new GoalResult(Status.INTERRUPTED, null);

    private final Status status;
    private final String message;

    /**
     * Creates a new goal result.
     *
     * @param status the result status
     * @param message an optional message
     */
    private GoalResult(@NotNull Status status, @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Gets the result status.
     *
     * @return the status
     */
    @NotNull
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the optional result message.
     *
     * @return the message, or null if not set
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * Checks if the goal is still running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    /**
     * Checks if the goal completed successfully.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the goal failed.
     *
     * @return true if failed
     */
    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    /**
     * Checks if the goal was cancelled.
     *
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    /**
     * Checks if the goal was interrupted.
     *
     * @return true if interrupted
     */
    public boolean isInterrupted() {
        return status == Status.INTERRUPTED;
    }

    /**
     * Checks if the goal has completed (success, failure, cancelled, or interrupted).
     *
     * @return true if completed
     */
    public boolean isComplete() {
        return status != Status.RUNNING;
    }

    /**
     * Checks if the goal ended normally (success or failure).
     *
     * @return true if ended normally
     */
    public boolean isNormalEnd() {
        return status == Status.SUCCESS || status == Status.FAILURE;
    }

    /**
     * Creates a success result with a message.
     *
     * @param message the success message
     * @return the result
     */
    @NotNull
    public static GoalResult success(@NotNull String message) {
        return new GoalResult(Status.SUCCESS, message);
    }

    /**
     * Creates a failure result with a message.
     *
     * @param message the failure message
     * @return the result
     */
    @NotNull
    public static GoalResult failure(@NotNull String message) {
        return new GoalResult(Status.FAILURE, message);
    }

    /**
     * Creates a cancelled result with a message.
     *
     * @param message the cancellation message
     * @return the result
     */
    @NotNull
    public static GoalResult cancelled(@NotNull String message) {
        return new GoalResult(Status.CANCELLED, message);
    }

    /**
     * Creates an interrupted result with a message.
     *
     * @param message the interruption message
     * @return the result
     */
    @NotNull
    public static GoalResult interrupted(@NotNull String message) {
        return new GoalResult(Status.INTERRUPTED, message);
    }

    /**
     * Creates a result from a boolean condition.
     *
     * @param success true for success, false for failure
     * @return SUCCESS if true, FAILURE if false
     */
    @NotNull
    public static GoalResult of(boolean success) {
        return success ? SUCCESS : FAILURE;
    }

    /**
     * Creates a result from a boolean condition with messages.
     *
     * @param success true for success, false for failure
     * @param successMessage the message if successful
     * @param failureMessage the message if failed
     * @return the appropriate result
     */
    @NotNull
    public static GoalResult of(boolean success, @NotNull String successMessage, @NotNull String failureMessage) {
        return success ? success(successMessage) : failure(failureMessage);
    }

    @Override
    public String toString() {
        if (message != null) {
            return status.name() + ": " + message;
        }
        return status.name();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GoalResult other)) return false;
        return status == other.status &&
               (message == null ? other.message == null : message.equals(other.message));
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        if (message != null) {
            result = 31 * result + message.hashCode();
        }
        return result;
    }

    /**
     * Enumeration of result statuses.
     */
    public enum Status {
        /**
         * Goal is still running.
         */
        RUNNING,

        /**
         * Goal completed successfully.
         */
        SUCCESS,

        /**
         * Goal failed to complete.
         */
        FAILURE,

        /**
         * Goal was cancelled externally.
         */
        CANCELLED,

        /**
         * Goal was interrupted by another goal.
         */
        INTERRUPTED
    }
}
