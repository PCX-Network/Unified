package sh.pcx.unified.world.ai.navigation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a pathfinding operation.
 *
 * <p>PathResult provides information about whether pathfinding was successful,
 * partial, or failed, along with any relevant details about the result.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * PathResult result = nav.pathTo(targetLocation, 1.0);
 * if (result.isSuccess()) {
 *     System.out.println("Path found with length: " + result.getPathLength());
 * } else if (result.isPartial()) {
 *     System.out.println("Partial path found");
 * } else {
 *     System.out.println("Pathfinding failed: " + result.getFailureReason());
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class PathResult {

    private final Status status;
    private final String failureReason;
    private final double pathLength;
    private final int nodeCount;
    private final NavigationController.Path path;

    /**
     * Creates a new PathResult.
     *
     * @param status the result status
     * @param failureReason the failure reason if failed
     * @param pathLength the path length if successful
     * @param nodeCount the node count if successful
     * @param path the path if successful
     */
    private PathResult(
            @NotNull Status status,
            @Nullable String failureReason,
            double pathLength,
            int nodeCount,
            @Nullable NavigationController.Path path
    ) {
        this.status = status;
        this.failureReason = failureReason;
        this.pathLength = pathLength;
        this.nodeCount = nodeCount;
        this.path = path;
    }

    /**
     * Creates a successful result.
     *
     * @param pathLength the path length
     * @param nodeCount the node count
     * @param path the computed path
     * @return the success result
     */
    @NotNull
    public static PathResult success(double pathLength, int nodeCount, @Nullable NavigationController.Path path) {
        return new PathResult(Status.SUCCESS, null, pathLength, nodeCount, path);
    }

    /**
     * Creates a successful result without path details.
     *
     * @return the success result
     */
    @NotNull
    public static PathResult success() {
        return new PathResult(Status.SUCCESS, null, 0, 0, null);
    }

    /**
     * Creates a partial result.
     *
     * @param pathLength the partial path length
     * @param nodeCount the node count
     * @param path the partial path
     * @return the partial result
     */
    @NotNull
    public static PathResult partial(double pathLength, int nodeCount, @Nullable NavigationController.Path path) {
        return new PathResult(Status.PARTIAL, null, pathLength, nodeCount, path);
    }

    /**
     * Creates a partial result without path details.
     *
     * @return the partial result
     */
    @NotNull
    public static PathResult partial() {
        return new PathResult(Status.PARTIAL, null, 0, 0, null);
    }

    /**
     * Creates a failed result.
     *
     * @param reason the failure reason
     * @return the failure result
     */
    @NotNull
    public static PathResult failure(@NotNull String reason) {
        return new PathResult(Status.FAILED, reason, 0, 0, null);
    }

    /**
     * Creates a failed result with default reason.
     *
     * @return the failure result
     */
    @NotNull
    public static PathResult failure() {
        return failure("Path not found");
    }

    /**
     * Checks if pathfinding was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if pathfinding resulted in a partial path.
     *
     * @return true if partial
     */
    public boolean isPartial() {
        return status == Status.PARTIAL;
    }

    /**
     * Checks if pathfinding failed.
     *
     * @return true if failed
     */
    public boolean isFailed() {
        return status == Status.FAILED;
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
     * Gets the failure reason.
     *
     * @return the failure reason, or null if not failed
     */
    @Nullable
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Gets the path length.
     *
     * @return the path length
     */
    public double getPathLength() {
        return pathLength;
    }

    /**
     * Gets the node count.
     *
     * @return the node count
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Gets the computed path.
     *
     * @return the path, or null if failed
     */
    @Nullable
    public NavigationController.Path getPath() {
        return path;
    }

    /**
     * Result status enumeration.
     */
    public enum Status {
        /**
         * Pathfinding was successful.
         */
        SUCCESS,

        /**
         * Pathfinding resulted in a partial path.
         */
        PARTIAL,

        /**
         * Pathfinding failed.
         */
        FAILED
    }
}
