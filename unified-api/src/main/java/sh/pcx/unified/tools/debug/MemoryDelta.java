/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Represents the difference between two memory snapshots.
 *
 * <p>Memory deltas are useful for tracking memory changes between
 * two points in time, such as before and after an operation.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MemorySnapshot before = debug.memorySnapshot();
 * loadLargeDataset();
 * MemorySnapshot after = debug.memorySnapshot();
 *
 * MemoryDelta delta = debug.memoryMonitor().compare(before, after);
 *
 * System.out.println("Used memory delta: " + delta.usedDelta());
 * System.out.println("Time elapsed: " + delta.timeElapsed());
 *
 * if (delta.usedDeltaMB() > 100) {
 *     logger.warn("Operation used more than 100 MB");
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MemorySnapshot
 * @see MemoryMonitor
 */
public interface MemoryDelta {

    /**
     * Returns the before snapshot.
     *
     * @return the before snapshot
     * @since 1.0.0
     */
    @NotNull
    MemorySnapshot before();

    /**
     * Returns the after snapshot.
     *
     * @return the after snapshot
     * @since 1.0.0
     */
    @NotNull
    MemorySnapshot after();

    /**
     * Returns the change in used heap memory (bytes).
     *
     * <p>Positive values indicate increased memory usage.
     *
     * @return the used memory delta
     * @since 1.0.0
     */
    long usedDelta();

    /**
     * Returns the change in free heap memory (bytes).
     *
     * @return the free memory delta
     * @since 1.0.0
     */
    long freeDelta();

    /**
     * Returns the change in total heap memory (bytes).
     *
     * @return the total memory delta
     * @since 1.0.0
     */
    long totalDelta();

    /**
     * Returns the change in non-heap memory (bytes).
     *
     * @return the non-heap memory delta
     * @since 1.0.0
     */
    long nonHeapDelta();

    /**
     * Returns the change in usage percentage.
     *
     * @return the usage percentage delta
     * @since 1.0.0
     */
    default double usagePercentageDelta() {
        return after().usagePercentage() - before().usagePercentage();
    }

    /**
     * Returns the time elapsed between snapshots.
     *
     * @return the elapsed time
     * @since 1.0.0
     */
    @NotNull
    Duration timeElapsed();

    /**
     * Returns the used memory delta in megabytes.
     *
     * @return used delta in MB
     * @since 1.0.0
     */
    default double usedDeltaMB() {
        return usedDelta() / (1024.0 * 1024.0);
    }

    /**
     * Returns the free memory delta in megabytes.
     *
     * @return free delta in MB
     * @since 1.0.0
     */
    default double freeDeltaMB() {
        return freeDelta() / (1024.0 * 1024.0);
    }

    /**
     * Checks if memory usage increased.
     *
     * @return true if used memory increased
     * @since 1.0.0
     */
    default boolean memoryIncreased() {
        return usedDelta() > 0;
    }

    /**
     * Checks if memory usage decreased.
     *
     * @return true if used memory decreased
     * @since 1.0.0
     */
    default boolean memoryDecreased() {
        return usedDelta() < 0;
    }

    /**
     * Returns a human-readable summary.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    default String summary() {
        String direction = memoryIncreased() ? "+" : "";
        return String.format(
                "Memory delta: %s%.2f MB (%s%.1f%%) over %d ms",
                direction,
                usedDeltaMB(),
                direction,
                usagePercentageDelta(),
                timeElapsed().toMillis()
        );
    }

    /**
     * Creates a memory delta from two snapshots.
     *
     * @param before the before snapshot
     * @param after  the after snapshot
     * @return the memory delta
     * @since 1.0.0
     */
    static MemoryDelta of(@NotNull MemorySnapshot before, @NotNull MemorySnapshot after) {
        return new MemoryDelta() {
            @Override
            public @NotNull MemorySnapshot before() { return before; }

            @Override
            public @NotNull MemorySnapshot after() { return after; }

            @Override
            public long usedDelta() {
                return after.usedMemory() - before.usedMemory();
            }

            @Override
            public long freeDelta() {
                return after.freeMemory() - before.freeMemory();
            }

            @Override
            public long totalDelta() {
                return after.totalMemory() - before.totalMemory();
            }

            @Override
            public long nonHeapDelta() {
                return after.nonHeapUsed() - before.nonHeapUsed();
            }

            @Override
            public @NotNull Duration timeElapsed() {
                return Duration.between(before.timestamp(), after.timestamp());
            }
        };
    }
}
