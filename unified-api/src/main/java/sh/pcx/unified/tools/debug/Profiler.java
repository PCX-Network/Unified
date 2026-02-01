/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * A profiler for measuring code performance.
 *
 * <p>The profiler provides detailed timing information for code blocks,
 * including aggregated statistics over multiple runs.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Profiler profiler = debug.profiler();
 *
 * // Start profiling
 * profiler.start();
 *
 * // Profile individual operations
 * try (Profiler.Section section = profiler.section("database")) {
 *     queryDatabase();
 * }
 *
 * // Profile with return value
 * String result = profiler.profile("fetch", () -> fetchData());
 *
 * // Stop and get results
 * ProfileReport report = profiler.stop();
 *
 * // Print summary
 * System.out.println(report.summary());
 *
 * // Access individual sections
 * for (ProfileReport.SectionStats stats : report.sections()) {
 *     System.out.println(stats.name() + ": " + stats.totalTime());
 * }
 * }</pre>
 *
 * <h2>Flamegraph Output</h2>
 * <pre>{@code
 * // Generate flamegraph data
 * String flamegraph = profiler.flamegraph();
 *
 * // Or save to file
 * profiler.saveFlamegraph(Path.of("profile.svg"));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DebugService
 * @see ProfileResult
 */
public interface Profiler {

    /**
     * Starts the profiler.
     *
     * @return this profiler for chaining
     * @since 1.0.0
     */
    @NotNull
    Profiler start();

    /**
     * Stops the profiler and returns the report.
     *
     * @return the profile report
     * @since 1.0.0
     */
    @NotNull
    ProfileReport stop();

    /**
     * Checks if the profiler is running.
     *
     * @return true if running
     * @since 1.0.0
     */
    boolean isRunning();

    /**
     * Resets the profiler, clearing all data.
     *
     * @return this profiler for chaining
     * @since 1.0.0
     */
    @NotNull
    Profiler reset();

    /**
     * Creates a profiling section.
     *
     * <p>Sections should be used with try-with-resources.
     *
     * @param name the section name
     * @return the section
     * @since 1.0.0
     */
    @NotNull
    Section section(@NotNull String name);

    /**
     * Profiles a runnable.
     *
     * @param name     the section name
     * @param runnable the code to profile
     * @since 1.0.0
     */
    void profile(@NotNull String name, @NotNull Runnable runnable);

    /**
     * Profiles a supplier and returns the result.
     *
     * @param <T>      the return type
     * @param name     the section name
     * @param supplier the code to profile
     * @return the result
     * @since 1.0.0
     */
    <T> T profile(@NotNull String name, @NotNull Supplier<T> supplier);

    /**
     * Profiles a callable and returns the result.
     *
     * @param <T>      the return type
     * @param name     the section name
     * @param callable the code to profile
     * @return the result
     * @throws Exception if the callable throws
     * @since 1.0.0
     */
    <T> T profileCallable(@NotNull String name, @NotNull Callable<T> callable) throws Exception;

    /**
     * Returns the current profile report without stopping.
     *
     * @return the current report
     * @since 1.0.0
     */
    @NotNull
    ProfileReport currentReport();

    /**
     * Sets the sampling rate for stack traces.
     *
     * @param intervalMs the sampling interval in milliseconds
     * @return this profiler for chaining
     * @since 1.0.0
     */
    @NotNull
    Profiler setSamplingInterval(int intervalMs);

    /**
     * Enables or disables stack trace sampling.
     *
     * @param enabled true to enable
     * @return this profiler for chaining
     * @since 1.0.0
     */
    @NotNull
    Profiler setStackSampling(boolean enabled);

    /**
     * Generates flamegraph data.
     *
     * @return the flamegraph in folded stack format
     * @since 1.0.0
     */
    @NotNull
    String flamegraph();

    /**
     * A profiling section.
     *
     * @since 1.0.0
     */
    interface Section extends AutoCloseable {

        /**
         * Returns the section name.
         *
         * @return the name
         * @since 1.0.0
         */
        @NotNull
        String name();

        /**
         * Returns the elapsed time.
         *
         * @return the elapsed duration
         * @since 1.0.0
         */
        @NotNull
        Duration elapsed();

        /**
         * Ends the section.
         *
         * @since 1.0.0
         */
        void end();

        @Override
        default void close() {
            end();
        }
    }

    /**
     * A profile report containing aggregated statistics.
     *
     * @since 1.0.0
     */
    interface ProfileReport {

        /**
         * Returns the total profiling duration.
         *
         * @return the total duration
         * @since 1.0.0
         */
        @NotNull
        Duration totalDuration();

        /**
         * Returns statistics for all sections.
         *
         * @return list of section statistics
         * @since 1.0.0
         */
        @NotNull
        List<SectionStats> sections();

        /**
         * Returns statistics for a specific section.
         *
         * @param name the section name
         * @return the section statistics, or null if not found
         * @since 1.0.0
         */
        @Nullable
        SectionStats section(@NotNull String name);

        /**
         * Returns the top N sections by total time.
         *
         * @param n the number of sections
         * @return list of top sections
         * @since 1.0.0
         */
        @NotNull
        List<SectionStats> topByTime(int n);

        /**
         * Returns the top N sections by call count.
         *
         * @param n the number of sections
         * @return list of top sections
         * @since 1.0.0
         */
        @NotNull
        List<SectionStats> topByCount(int n);

        /**
         * Returns a summary string.
         *
         * @return the summary
         * @since 1.0.0
         */
        @NotNull
        String summary();

        /**
         * Exports the report to JSON.
         *
         * @return the JSON string
         * @since 1.0.0
         */
        @NotNull
        String toJson();
    }

    /**
     * Statistics for a profiling section.
     *
     * @since 1.0.0
     */
    interface SectionStats {

        /**
         * Returns the section name.
         *
         * @return the name
         * @since 1.0.0
         */
        @NotNull
        String name();

        /**
         * Returns the number of times this section was profiled.
         *
         * @return the call count
         * @since 1.0.0
         */
        long callCount();

        /**
         * Returns the total time spent in this section.
         *
         * @return the total time
         * @since 1.0.0
         */
        @NotNull
        Duration totalTime();

        /**
         * Returns the average time per call.
         *
         * @return the average time
         * @since 1.0.0
         */
        @NotNull
        Duration averageTime();

        /**
         * Returns the minimum time.
         *
         * @return the min time
         * @since 1.0.0
         */
        @NotNull
        Duration minTime();

        /**
         * Returns the maximum time.
         *
         * @return the max time
         * @since 1.0.0
         */
        @NotNull
        Duration maxTime();

        /**
         * Returns the percentage of total profiling time.
         *
         * @return the percentage (0-100)
         * @since 1.0.0
         */
        double percentageOfTotal();

        /**
         * Returns stack sample data if available.
         *
         * @return map of stack trace to count
         * @since 1.0.0
         */
        @NotNull
        Map<String, Long> stackSamples();
    }
}
