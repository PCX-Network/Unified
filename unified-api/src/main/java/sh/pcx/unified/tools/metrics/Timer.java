/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A metric for measuring durations and timing events.
 *
 * <p>Timers automatically track count, total time, min, max, and can
 * calculate percentiles if configured. They are useful for measuring
 * latencies, processing times, and other duration-based metrics.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Timer requestTimer = metrics.timer("request_duration", "Request processing time");
 *
 * // Using try-with-resources
 * try (Timer.Context ctx = requestTimer.time()) {
 *     processRequest();
 * }
 *
 * // Using record method
 * requestTimer.record(Duration.ofMillis(150));
 * requestTimer.record(150, TimeUnit.MILLISECONDS);
 *
 * // Using wrap methods
 * String result = requestTimer.record(() -> fetchData());
 * requestTimer.recordRunnable(() -> doWork());
 *
 * // Access statistics
 * long count = requestTimer.count();
 * Duration total = requestTimer.totalTime();
 * Duration mean = requestTimer.mean();
 * Duration max = requestTimer.max();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations must be thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Metric
 * @see MetricsService
 */
public interface Timer extends Metric {

    /**
     * Starts timing an event.
     *
     * <p>The returned context should be closed when the event completes,
     * preferably using try-with-resources.
     *
     * @return a context that records the elapsed time when closed
     * @since 1.0.0
     */
    @NotNull
    Context time();

    /**
     * Records a duration.
     *
     * @param duration the duration to record
     * @since 1.0.0
     */
    void record(@NotNull Duration duration);

    /**
     * Records a duration.
     *
     * @param amount the duration amount
     * @param unit   the time unit
     * @since 1.0.0
     */
    void record(long amount, @NotNull TimeUnit unit);

    /**
     * Records the time taken to execute a runnable.
     *
     * @param runnable the runnable to time
     * @since 1.0.0
     */
    default void recordRunnable(@NotNull Runnable runnable) {
        try (Context ignored = time()) {
            runnable.run();
        }
    }

    /**
     * Records the time taken to execute a supplier.
     *
     * @param <T>      the return type
     * @param supplier the supplier to time
     * @return the result of the supplier
     * @since 1.0.0
     */
    default <T> T record(@NotNull Supplier<T> supplier) {
        try (Context ignored = time()) {
            return supplier.get();
        }
    }

    /**
     * Records the time taken to execute a callable.
     *
     * @param <T>      the return type
     * @param callable the callable to time
     * @return the result of the callable
     * @throws Exception if the callable throws
     * @since 1.0.0
     */
    default <T> T recordCallable(@NotNull Callable<T> callable) throws Exception {
        try (Context ignored = time()) {
            return callable.call();
        }
    }

    /**
     * Returns the number of recorded events.
     *
     * @return the count
     * @since 1.0.0
     */
    long count();

    /**
     * Returns the total recorded time.
     *
     * @return the total time
     * @since 1.0.0
     */
    @NotNull
    Duration totalTime();

    /**
     * Returns the mean (average) duration.
     *
     * @return the mean duration, or {@link Duration#ZERO} if no events recorded
     * @since 1.0.0
     */
    @NotNull
    Duration mean();

    /**
     * Returns the maximum recorded duration.
     *
     * @return the max duration, or {@link Duration#ZERO} if no events recorded
     * @since 1.0.0
     */
    @NotNull
    Duration max();

    /**
     * Returns the minimum recorded duration.
     *
     * @return the min duration, or {@link Duration#ZERO} if no events recorded
     * @since 1.0.0
     */
    @NotNull
    Duration min();

    /**
     * Creates a labeled child timer.
     *
     * @param labelValues the label values
     * @return a child timer with the specified labels
     * @throws IllegalArgumentException if the number of values doesn't match
     * @since 1.0.0
     */
    @NotNull
    Timer labels(@NotNull String... labelValues);

    /**
     * Resets all timer statistics.
     *
     * @since 1.0.0
     */
    void reset();

    /**
     * A context for timing an event.
     *
     * <p>Contexts should be used with try-with-resources to ensure
     * proper cleanup and recording.
     *
     * @since 1.0.0
     */
    interface Context extends AutoCloseable {

        /**
         * Stops the timer and records the elapsed time.
         *
         * <p>Calling this method multiple times has no additional effect
         * after the first call.
         *
         * @return the elapsed time in nanoseconds
         * @since 1.0.0
         */
        long stop();

        /**
         * {@inheritDoc}
         *
         * <p>Equivalent to calling {@link #stop()}.
         */
        @Override
        default void close() {
            stop();
        }

        /**
         * Returns the elapsed time without stopping the timer.
         *
         * @return the current elapsed time in nanoseconds
         * @since 1.0.0
         */
        long elapsed();
    }
}
