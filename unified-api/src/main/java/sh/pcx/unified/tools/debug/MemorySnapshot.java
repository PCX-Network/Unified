/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * A snapshot of memory usage at a point in time.
 *
 * <p>Memory snapshots capture the current state of heap and non-heap
 * memory, allowing you to track memory usage over time.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MemorySnapshot snapshot = debug.memorySnapshot();
 *
 * System.out.println("Heap used: " + snapshot.usedMemory());
 * System.out.println("Heap free: " + snapshot.freeMemory());
 * System.out.println("Heap max: " + snapshot.maxMemory());
 * System.out.println("Usage: " + snapshot.usagePercentage() + "%");
 *
 * // Non-heap memory
 * System.out.println("Non-heap used: " + snapshot.nonHeapUsed());
 *
 * // Human-readable
 * System.out.println(snapshot.usedMemoryMB() + " MB used");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MemoryMonitor
 */
public interface MemorySnapshot {

    /**
     * Returns the timestamp of this snapshot.
     *
     * @return the instant when the snapshot was taken
     * @since 1.0.0
     */
    @NotNull
    Instant timestamp();

    /**
     * Returns the used heap memory in bytes.
     *
     * @return used heap memory
     * @since 1.0.0
     */
    long usedMemory();

    /**
     * Returns the free heap memory in bytes.
     *
     * @return free heap memory
     * @since 1.0.0
     */
    long freeMemory();

    /**
     * Returns the total heap memory in bytes.
     *
     * @return total heap memory
     * @since 1.0.0
     */
    long totalMemory();

    /**
     * Returns the maximum heap memory in bytes.
     *
     * @return max heap memory
     * @since 1.0.0
     */
    long maxMemory();

    /**
     * Returns the used non-heap memory in bytes.
     *
     * @return used non-heap memory
     * @since 1.0.0
     */
    long nonHeapUsed();

    /**
     * Returns the committed non-heap memory in bytes.
     *
     * @return committed non-heap memory
     * @since 1.0.0
     */
    long nonHeapCommitted();

    /**
     * Returns the heap usage percentage.
     *
     * @return usage percentage (0-100)
     * @since 1.0.0
     */
    default double usagePercentage() {
        long max = maxMemory();
        return max > 0 ? (double) usedMemory() / max * 100 : 0;
    }

    /**
     * Returns the used memory in megabytes.
     *
     * @return used memory in MB
     * @since 1.0.0
     */
    default double usedMemoryMB() {
        return usedMemory() / (1024.0 * 1024.0);
    }

    /**
     * Returns the free memory in megabytes.
     *
     * @return free memory in MB
     * @since 1.0.0
     */
    default double freeMemoryMB() {
        return freeMemory() / (1024.0 * 1024.0);
    }

    /**
     * Returns the max memory in megabytes.
     *
     * @return max memory in MB
     * @since 1.0.0
     */
    default double maxMemoryMB() {
        return maxMemory() / (1024.0 * 1024.0);
    }

    /**
     * Returns a human-readable summary.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    default String summary() {
        return String.format(
                "Memory: %.1f MB / %.1f MB (%.1f%% used), Free: %.1f MB",
                usedMemoryMB(),
                maxMemoryMB(),
                usagePercentage(),
                freeMemoryMB()
        );
    }

    /**
     * Creates a memory snapshot from the current runtime.
     *
     * @return a new memory snapshot
     * @since 1.0.0
     */
    static MemorySnapshot now() {
        Runtime runtime = Runtime.getRuntime();
        java.lang.management.MemoryMXBean memoryBean =
                java.lang.management.ManagementFactory.getMemoryMXBean();

        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        long used = total - free;

        java.lang.management.MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        return new MemorySnapshot() {
            private final Instant ts = Instant.now();

            @Override
            public @NotNull Instant timestamp() { return ts; }

            @Override
            public long usedMemory() { return used; }

            @Override
            public long freeMemory() { return free; }

            @Override
            public long totalMemory() { return total; }

            @Override
            public long maxMemory() { return max; }

            @Override
            public long nonHeapUsed() { return nonHeap.getUsed(); }

            @Override
            public long nonHeapCommitted() { return nonHeap.getCommitted(); }
        };
    }
}
