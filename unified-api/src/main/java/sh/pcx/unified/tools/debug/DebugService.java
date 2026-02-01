/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Service for debugging, profiling, and monitoring plugin performance.
 *
 * <p>The DebugService provides comprehensive debugging tools including
 * trace logging, profiling, memory monitoring, and performance analysis.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private DebugService debug;
 *
 * // Enable debug mode
 * debug.setDebugMode(true);
 *
 * // Trace execution
 * try (var trace = debug.trace("processRequest")) {
 *     processRequest();
 *     trace.checkpoint("validated");
 *     doMoreWork();
 * } // Automatically logs duration and checkpoints
 *
 * // Profile a block of code
 * ProfileResult result = debug.profile("heavyOperation", () -> {
 *     performHeavyOperation();
 * });
 * System.out.println("Took: " + result.duration());
 *
 * // Memory monitoring
 * MemorySnapshot before = debug.memorySnapshot();
 * loadData();
 * MemorySnapshot after = debug.memorySnapshot();
 * System.out.println("Memory delta: " + after.usedMemory() - before.usedMemory());
 *
 * // Dump thread info
 * debug.dumpThreads();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Trace
 * @see Profiler
 * @see MemoryMonitor
 */
public interface DebugService extends Service {

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is enabled
     * @since 1.0.0
     */
    boolean isDebugMode();

    /**
     * Sets whether debug mode is enabled.
     *
     * <p>When debug mode is enabled, additional logging and tracing
     * information is collected.
     *
     * @param enabled true to enable debug mode
     * @since 1.0.0
     */
    void setDebugMode(boolean enabled);

    /**
     * Starts a new trace.
     *
     * <p>Traces are used to track execution flow and timing through
     * a block of code. Use try-with-resources to ensure proper cleanup.
     *
     * @param name the trace name
     * @return the trace context
     * @since 1.0.0
     */
    @NotNull
    Trace trace(@NotNull String name);

    /**
     * Starts a new trace with additional context.
     *
     * @param name    the trace name
     * @param context additional context information
     * @return the trace context
     * @since 1.0.0
     */
    @NotNull
    Trace trace(@NotNull String name, @Nullable String context);

    /**
     * Profiles a runnable and returns the result.
     *
     * @param name     the profile name
     * @param runnable the code to profile
     * @return the profile result
     * @since 1.0.0
     */
    @NotNull
    ProfileResult profile(@NotNull String name, @NotNull Runnable runnable);

    /**
     * Profiles a supplier and returns the result with the value.
     *
     * @param <T>      the return type
     * @param name     the profile name
     * @param supplier the code to profile
     * @return the profile result with the value
     * @since 1.0.0
     */
    @NotNull
    <T> ProfileResult.WithValue<T> profile(@NotNull String name, @NotNull Supplier<T> supplier);

    /**
     * Returns the profiler instance.
     *
     * @return the profiler
     * @since 1.0.0
     */
    @NotNull
    Profiler profiler();

    /**
     * Returns the memory monitor.
     *
     * @return the memory monitor
     * @since 1.0.0
     */
    @NotNull
    MemoryMonitor memoryMonitor();

    /**
     * Takes a memory snapshot.
     *
     * @return the memory snapshot
     * @since 1.0.0
     */
    @NotNull
    default MemorySnapshot memorySnapshot() {
        return memoryMonitor().snapshot();
    }

    /**
     * Dumps current thread information to the log.
     *
     * @since 1.0.0
     */
    void dumpThreads();

    /**
     * Returns information about all threads.
     *
     * @return list of thread information
     * @since 1.0.0
     */
    @NotNull
    List<ThreadInfo> getThreadInfo();

    /**
     * Logs a debug message if debug mode is enabled.
     *
     * @param message the message
     * @since 1.0.0
     */
    void debug(@NotNull String message);

    /**
     * Logs a debug message with formatting if debug mode is enabled.
     *
     * @param format the format string
     * @param args   the arguments
     * @since 1.0.0
     */
    void debug(@NotNull String format, @NotNull Object... args);

    /**
     * Logs a trace-level message (more verbose than debug).
     *
     * @param message the message
     * @since 1.0.0
     */
    void logTrace(@NotNull String message);

    /**
     * Logs a trace-level message with formatting.
     *
     * @param format the format string
     * @param args   the arguments
     * @since 1.0.0
     */
    void logTrace(@NotNull String format, @NotNull Object... args);

    /**
     * Creates a debug timer that logs elapsed time.
     *
     * @param name the timer name
     * @return the timer
     * @since 1.0.0
     */
    @NotNull
    DebugTimer timer(@NotNull String name);

    /**
     * Registers a health check.
     *
     * @param name  the health check name
     * @param check the health check
     * @since 1.0.0
     */
    void registerHealthCheck(@NotNull String name, @NotNull HealthCheck check);

    /**
     * Runs all registered health checks.
     *
     * @return list of health check results
     * @since 1.0.0
     */
    @NotNull
    List<HealthCheckResult> runHealthChecks();

    /**
     * A simple timer for debug logging.
     *
     * @since 1.0.0
     */
    interface DebugTimer extends AutoCloseable {

        /**
         * Returns the elapsed time.
         *
         * @return the elapsed duration
         * @since 1.0.0
         */
        @NotNull
        Duration elapsed();

        /**
         * Logs a checkpoint with the current elapsed time.
         *
         * @param name the checkpoint name
         * @since 1.0.0
         */
        void checkpoint(@NotNull String name);

        /**
         * Stops the timer and logs the result.
         *
         * @since 1.0.0
         */
        void stop();

        @Override
        default void close() {
            stop();
        }
    }

    /**
     * A health check function.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface HealthCheck {

        /**
         * Performs the health check.
         *
         * @return the result
         * @since 1.0.0
         */
        @NotNull
        HealthCheckResult check();
    }

    /**
     * Result of a health check.
     *
     * @param name    the health check name
     * @param healthy true if healthy
     * @param message optional message
     * @param error   optional error
     * @since 1.0.0
     */
    record HealthCheckResult(
            @NotNull String name,
            boolean healthy,
            @Nullable String message,
            @Nullable Throwable error
    ) {
        /**
         * Creates a healthy result.
         *
         * @param name the check name
         * @return healthy result
         */
        public static HealthCheckResult healthy(@NotNull String name) {
            return new HealthCheckResult(name, true, null, null);
        }

        /**
         * Creates a healthy result with message.
         *
         * @param name    the check name
         * @param message the message
         * @return healthy result
         */
        public static HealthCheckResult healthy(@NotNull String name, @NotNull String message) {
            return new HealthCheckResult(name, true, message, null);
        }

        /**
         * Creates an unhealthy result.
         *
         * @param name    the check name
         * @param message the message
         * @return unhealthy result
         */
        public static HealthCheckResult unhealthy(@NotNull String name, @NotNull String message) {
            return new HealthCheckResult(name, false, message, null);
        }

        /**
         * Creates an unhealthy result with error.
         *
         * @param name  the check name
         * @param error the error
         * @return unhealthy result
         */
        public static HealthCheckResult unhealthy(@NotNull String name, @NotNull Throwable error) {
            return new HealthCheckResult(name, false, error.getMessage(), error);
        }
    }

    /**
     * Information about a thread.
     *
     * @param id        the thread ID
     * @param name      the thread name
     * @param state     the thread state
     * @param priority  the thread priority
     * @param daemon    true if daemon thread
     * @param stackTrace the stack trace
     * @since 1.0.0
     */
    record ThreadInfo(
            long id,
            @NotNull String name,
            @NotNull Thread.State state,
            int priority,
            boolean daemon,
            @NotNull StackTraceElement[] stackTrace
    ) {
        /**
         * Creates ThreadInfo from a Thread.
         *
         * @param thread the thread
         * @return the thread info
         */
        public static ThreadInfo from(@NotNull Thread thread) {
            return new ThreadInfo(
                    thread.threadId(),
                    thread.getName(),
                    thread.getState(),
                    thread.getPriority(),
                    thread.isDaemon(),
                    thread.getStackTrace()
            );
        }
    }
}
