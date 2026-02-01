/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A trace for tracking execution flow and timing.
 *
 * <p>Traces provide a way to track the execution path and timing of
 * operations, including nested sub-operations and checkpoints.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * try (Trace trace = debug.trace("handleRequest")) {
 *     trace.tag("user", userId);
 *     trace.tag("method", "POST");
 *
 *     validateRequest();
 *     trace.checkpoint("validated");
 *
 *     try (Trace child = trace.child("processData")) {
 *         processData();
 *     }
 *
 *     saveResult();
 *     trace.checkpoint("saved");
 * }
 *
 * // Access trace data
 * Duration total = trace.duration();
 * List<Trace.Checkpoint> checkpoints = trace.checkpoints();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Traces are not thread-safe and should be used within a single thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DebugService
 */
public interface Trace extends AutoCloseable {

    /**
     * Returns the trace name.
     *
     * @return the name
     * @since 1.0.0
     */
    @NotNull
    String name();

    /**
     * Returns the trace ID.
     *
     * @return the unique trace ID
     * @since 1.0.0
     */
    @NotNull
    String traceId();

    /**
     * Returns the parent trace, if any.
     *
     * @return the parent trace, or null if root trace
     * @since 1.0.0
     */
    @Nullable
    Trace parent();

    /**
     * Returns the start time of this trace.
     *
     * @return the start instant
     * @since 1.0.0
     */
    @NotNull
    Instant startTime();

    /**
     * Returns the end time of this trace.
     *
     * @return the end instant, or null if not ended
     * @since 1.0.0
     */
    @Nullable
    Instant endTime();

    /**
     * Returns the duration of this trace.
     *
     * <p>If the trace is still active, returns the elapsed time so far.
     *
     * @return the duration
     * @since 1.0.0
     */
    @NotNull
    Duration duration();

    /**
     * Checks if this trace is still active.
     *
     * @return true if active
     * @since 1.0.0
     */
    boolean isActive();

    /**
     * Adds a checkpoint at the current time.
     *
     * @param name the checkpoint name
     * @return this trace for chaining
     * @since 1.0.0
     */
    @NotNull
    Trace checkpoint(@NotNull String name);

    /**
     * Adds a checkpoint with additional data.
     *
     * @param name the checkpoint name
     * @param data additional data
     * @return this trace for chaining
     * @since 1.0.0
     */
    @NotNull
    Trace checkpoint(@NotNull String name, @Nullable String data);

    /**
     * Returns all checkpoints.
     *
     * @return list of checkpoints
     * @since 1.0.0
     */
    @NotNull
    List<Checkpoint> checkpoints();

    /**
     * Adds a tag to this trace.
     *
     * <p>Tags are key-value pairs that provide additional context.
     *
     * @param key   the tag key
     * @param value the tag value
     * @return this trace for chaining
     * @since 1.0.0
     */
    @NotNull
    Trace tag(@NotNull String key, @NotNull String value);

    /**
     * Adds a tag to this trace.
     *
     * @param key   the tag key
     * @param value the tag value
     * @return this trace for chaining
     * @since 1.0.0
     */
    @NotNull
    default Trace tag(@NotNull String key, @NotNull Object value) {
        return tag(key, String.valueOf(value));
    }

    /**
     * Returns all tags.
     *
     * @return the tags map
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> tags();

    /**
     * Creates a child trace.
     *
     * @param name the child trace name
     * @return the child trace
     * @since 1.0.0
     */
    @NotNull
    Trace child(@NotNull String name);

    /**
     * Returns all child traces.
     *
     * @return list of child traces
     * @since 1.0.0
     */
    @NotNull
    List<Trace> children();

    /**
     * Logs an error in this trace.
     *
     * @param error the error
     * @return this trace for chaining
     * @since 1.0.0
     */
    @NotNull
    Trace error(@NotNull Throwable error);

    /**
     * Logs an error message in this trace.
     *
     * @param message the error message
     * @return this trace for chaining
     * @since 1.0.0
     */
    @NotNull
    Trace error(@NotNull String message);

    /**
     * Returns the error if one was logged.
     *
     * @return the error, or null
     * @since 1.0.0
     */
    @Nullable
    Throwable error();

    /**
     * Checks if this trace has an error.
     *
     * @return true if an error was logged
     * @since 1.0.0
     */
    default boolean hasError() {
        return error() != null;
    }

    /**
     * Ends this trace.
     *
     * @since 1.0.0
     */
    void end();

    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to calling {@link #end()}.
     */
    @Override
    default void close() {
        end();
    }

    /**
     * Exports this trace to a string representation.
     *
     * @return the string representation
     * @since 1.0.0
     */
    @NotNull
    String export();

    /**
     * A checkpoint in a trace.
     *
     * @param name      the checkpoint name
     * @param timestamp the checkpoint timestamp
     * @param elapsed   elapsed time since trace start
     * @param data      optional additional data
     * @since 1.0.0
     */
    record Checkpoint(
            @NotNull String name,
            @NotNull Instant timestamp,
            @NotNull Duration elapsed,
            @Nullable String data
    ) {}
}
