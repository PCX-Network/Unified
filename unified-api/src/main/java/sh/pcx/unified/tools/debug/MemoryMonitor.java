/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Monitor for tracking memory usage and detecting leaks.
 *
 * <p>The MemoryMonitor provides tools for tracking memory usage,
 * detecting potential memory leaks, and analyzing heap composition.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MemoryMonitor memory = debug.memoryMonitor();
 *
 * // Take a snapshot
 * MemorySnapshot snapshot = memory.snapshot();
 * System.out.println("Used: " + snapshot.usedMemory());
 * System.out.println("Free: " + snapshot.freeMemory());
 * System.out.println("Max: " + snapshot.maxMemory());
 *
 * // Compare snapshots
 * MemorySnapshot before = memory.snapshot();
 * loadHugeDataset();
 * MemorySnapshot after = memory.snapshot();
 * MemoryDelta delta = memory.compare(before, after);
 * System.out.println("Memory delta: " + delta.usedDelta());
 *
 * // Start monitoring
 * memory.startMonitoring(Duration.ofSeconds(30), snapshot -> {
 *     if (snapshot.usagePercentage() > 90) {
 *         logger.warn("High memory usage: " + snapshot.usagePercentage() + "%");
 *     }
 * });
 *
 * // Request GC and wait
 * memory.requestGc();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DebugService
 * @see MemorySnapshot
 */
public interface MemoryMonitor {

    /**
     * Takes a memory snapshot.
     *
     * @return the snapshot
     * @since 1.0.0
     */
    @NotNull
    MemorySnapshot snapshot();

    /**
     * Compares two memory snapshots.
     *
     * @param before the earlier snapshot
     * @param after  the later snapshot
     * @return the delta between snapshots
     * @since 1.0.0
     */
    @NotNull
    MemoryDelta compare(@NotNull MemorySnapshot before, @NotNull MemorySnapshot after);

    /**
     * Starts periodic memory monitoring.
     *
     * @param interval the monitoring interval
     * @param callback the callback to invoke with each snapshot
     * @since 1.0.0
     */
    void startMonitoring(@NotNull Duration interval, @NotNull Consumer<MemorySnapshot> callback);

    /**
     * Stops periodic memory monitoring.
     *
     * @since 1.0.0
     */
    void stopMonitoring();

    /**
     * Checks if monitoring is active.
     *
     * @return true if monitoring
     * @since 1.0.0
     */
    boolean isMonitoring();

    /**
     * Returns the monitoring history.
     *
     * @return list of snapshots taken during monitoring
     * @since 1.0.0
     */
    @NotNull
    List<MemorySnapshot> history();

    /**
     * Returns the maximum history size.
     *
     * @return the max history size
     * @since 1.0.0
     */
    int maxHistorySize();

    /**
     * Sets the maximum history size.
     *
     * @param size the max size
     * @since 1.0.0
     */
    void setMaxHistorySize(int size);

    /**
     * Clears the monitoring history.
     *
     * @since 1.0.0
     */
    void clearHistory();

    /**
     * Requests garbage collection.
     *
     * <p>Note: This is only a suggestion to the JVM; garbage collection
     * may not happen immediately.
     *
     * @since 1.0.0
     */
    void requestGc();

    /**
     * Gets the current memory pool statistics.
     *
     * @return list of memory pool stats
     * @since 1.0.0
     */
    @NotNull
    List<MemoryPoolStats> memoryPools();

    /**
     * Sets a threshold for memory warnings.
     *
     * @param percentage the usage percentage threshold (0-100)
     * @param callback   the callback when threshold is exceeded
     * @since 1.0.0
     */
    void setWarningThreshold(double percentage, @NotNull Consumer<MemorySnapshot> callback);

    /**
     * Sets a threshold for critical memory alerts.
     *
     * @param percentage the usage percentage threshold (0-100)
     * @param callback   the callback when threshold is exceeded
     * @since 1.0.0
     */
    void setCriticalThreshold(double percentage, @NotNull Consumer<MemorySnapshot> callback);

    /**
     * Analyzes memory usage and returns recommendations.
     *
     * @return the analysis result
     * @since 1.0.0
     */
    @NotNull
    MemoryAnalysis analyze();

    /**
     * Statistics for a memory pool.
     *
     * @param name      the pool name
     * @param type      the pool type (heap or non-heap)
     * @param used      used memory in bytes
     * @param committed committed memory in bytes
     * @param max       max memory in bytes (-1 if undefined)
     * @since 1.0.0
     */
    record MemoryPoolStats(
            @NotNull String name,
            @NotNull String type,
            long used,
            long committed,
            long max
    ) {
        /**
         * Returns the usage percentage.
         *
         * @return the usage percentage, or -1 if max is undefined
         */
        public double usagePercentage() {
            return max > 0 ? (double) used / max * 100 : -1;
        }
    }

    /**
     * Memory analysis result with recommendations.
     *
     * @since 1.0.0
     */
    interface MemoryAnalysis {

        /**
         * Returns the overall health status.
         *
         * @return the health status
         * @since 1.0.0
         */
        @NotNull
        HealthStatus status();

        /**
         * Returns analysis messages.
         *
         * @return list of messages
         * @since 1.0.0
         */
        @NotNull
        List<String> messages();

        /**
         * Returns recommendations.
         *
         * @return list of recommendations
         * @since 1.0.0
         */
        @NotNull
        List<String> recommendations();

        /**
         * Returns detected issues.
         *
         * @return list of issues
         * @since 1.0.0
         */
        @NotNull
        List<Issue> issues();

        /**
         * Memory health status.
         *
         * @since 1.0.0
         */
        enum HealthStatus {
            /**
             * Memory usage is healthy.
             */
            HEALTHY,

            /**
             * Memory usage is elevated but acceptable.
             */
            WARNING,

            /**
             * Memory usage is critically high.
             */
            CRITICAL
        }

        /**
         * A detected memory issue.
         *
         * @param severity    the issue severity
         * @param description the issue description
         * @param suggestion  suggested fix
         * @since 1.0.0
         */
        record Issue(
                @NotNull Severity severity,
                @NotNull String description,
                @Nullable String suggestion
        ) {
            /**
             * Issue severity levels.
             */
            enum Severity {
                INFO, WARNING, ERROR
            }
        }
    }
}
