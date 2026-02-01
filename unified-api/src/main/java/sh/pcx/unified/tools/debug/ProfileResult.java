/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of a profiling operation.
 *
 * <p>Contains timing information and metadata about a profiled code block.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ProfileResult result = debug.profile("operation", () -> doWork());
 *
 * System.out.println("Duration: " + result.duration());
 * System.out.println("Started: " + result.startTime());
 * System.out.println("Ended: " + result.endTime());
 *
 * if (result.hasError()) {
 *     System.err.println("Error: " + result.error());
 * }
 *
 * // With return value
 * ProfileResult.WithValue<String> resultWithValue =
 *     debug.profile("fetch", () -> fetchData());
 * String data = resultWithValue.value();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DebugService
 * @see Profiler
 */
public interface ProfileResult {

    /**
     * Returns the name of the profiled operation.
     *
     * @return the name
     * @since 1.0.0
     */
    @NotNull
    String name();

    /**
     * Returns the duration of the operation.
     *
     * @return the duration
     * @since 1.0.0
     */
    @NotNull
    Duration duration();

    /**
     * Returns the duration in milliseconds.
     *
     * @return the duration in ms
     * @since 1.0.0
     */
    default long durationMillis() {
        return duration().toMillis();
    }

    /**
     * Returns the duration in nanoseconds.
     *
     * @return the duration in ns
     * @since 1.0.0
     */
    default long durationNanos() {
        return duration().toNanos();
    }

    /**
     * Returns the start time.
     *
     * @return the start instant
     * @since 1.0.0
     */
    @NotNull
    Instant startTime();

    /**
     * Returns the end time.
     *
     * @return the end instant
     * @since 1.0.0
     */
    @NotNull
    Instant endTime();

    /**
     * Returns any error that occurred during profiling.
     *
     * @return the error, or null if none
     * @since 1.0.0
     */
    @Nullable
    Throwable error();

    /**
     * Checks if an error occurred.
     *
     * @return true if an error occurred
     * @since 1.0.0
     */
    default boolean hasError() {
        return error() != null;
    }

    /**
     * Checks if the operation completed successfully.
     *
     * @return true if successful
     * @since 1.0.0
     */
    default boolean isSuccess() {
        return !hasError();
    }

    /**
     * Returns the thread that executed the operation.
     *
     * @return the thread name
     * @since 1.0.0
     */
    @NotNull
    String threadName();

    /**
     * Creates a simple profile result.
     *
     * @param name      the operation name
     * @param startTime the start time
     * @param endTime   the end time
     * @param error     any error that occurred
     * @return the profile result
     * @since 1.0.0
     */
    static ProfileResult of(
            @NotNull String name,
            @NotNull Instant startTime,
            @NotNull Instant endTime,
            @Nullable Throwable error
    ) {
        return new ProfileResult() {
            private final Duration duration = Duration.between(startTime, endTime);
            private final String thread = Thread.currentThread().getName();

            @Override
            public @NotNull String name() { return name; }

            @Override
            public @NotNull Duration duration() { return duration; }

            @Override
            public @NotNull Instant startTime() { return startTime; }

            @Override
            public @NotNull Instant endTime() { return endTime; }

            @Override
            public @Nullable Throwable error() { return error; }

            @Override
            public @NotNull String threadName() { return thread; }
        };
    }

    /**
     * A profile result that includes a return value.
     *
     * @param <T> the value type
     * @since 1.0.0
     */
    interface WithValue<T> extends ProfileResult {

        /**
         * Returns the value produced by the profiled operation.
         *
         * @return the value, or null if an error occurred
         * @since 1.0.0
         */
        @Nullable
        T value();

        /**
         * Returns the value or throws if an error occurred.
         *
         * @return the value
         * @throws RuntimeException if an error occurred
         * @since 1.0.0
         */
        default T valueOrThrow() {
            if (hasError()) {
                Throwable e = error();
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(e);
            }
            return value();
        }

        /**
         * Returns the value or a default if null or error.
         *
         * @param defaultValue the default value
         * @return the value or default
         * @since 1.0.0
         */
        default T valueOrDefault(@NotNull T defaultValue) {
            T v = value();
            return v != null ? v : defaultValue;
        }

        /**
         * Creates a profile result with value.
         *
         * @param <T>       the value type
         * @param name      the operation name
         * @param startTime the start time
         * @param endTime   the end time
         * @param value     the value
         * @param error     any error that occurred
         * @return the profile result
         * @since 1.0.0
         */
        static <T> WithValue<T> of(
                @NotNull String name,
                @NotNull Instant startTime,
                @NotNull Instant endTime,
                @Nullable T value,
                @Nullable Throwable error
        ) {
            return new WithValue<>() {
                private final Duration duration = Duration.between(startTime, endTime);
                private final String thread = Thread.currentThread().getName();

                @Override
                public @NotNull String name() { return name; }

                @Override
                public @NotNull Duration duration() { return duration; }

                @Override
                public @NotNull Instant startTime() { return startTime; }

                @Override
                public @NotNull Instant endTime() { return endTime; }

                @Override
                public @Nullable Throwable error() { return error; }

                @Override
                public @NotNull String threadName() { return thread; }

                @Override
                public @Nullable T value() { return value; }
            };
        }
    }
}
