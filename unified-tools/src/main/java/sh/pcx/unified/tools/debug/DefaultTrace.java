/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import sh.pcx.unified.tools.debug.Trace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link Trace}.
 *
 * @since 1.0.0
 */
public final class DefaultTrace implements Trace {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTrace.class);

    private final String name;
    private final String traceId;
    private final Trace parent;
    private final Instant startTime;
    private final boolean debugMode;
    private volatile Instant endTime;
    private volatile boolean active = true;

    private final List<Checkpoint> checkpoints = new ArrayList<>();
    private final Map<String, String> tags = new ConcurrentHashMap<>();
    private final List<Trace> children = new ArrayList<>();
    private volatile Throwable error;

    /**
     * Creates a new root trace.
     *
     * @param name      the trace name
     * @param context   optional context
     * @param debugMode whether debug mode is enabled
     */
    public DefaultTrace(@NotNull String name, @Nullable String context, boolean debugMode) {
        this(name, (Trace) null, debugMode);
        if (context != null) {
            tags.put("context", context);
        }
    }

    /**
     * Creates a new trace.
     *
     * @param name      the trace name
     * @param parent    the parent trace
     * @param debugMode whether debug mode is enabled
     */
    public DefaultTrace(@NotNull String name, @Nullable Trace parent, boolean debugMode) {
        this.name = name;
        this.traceId = UUID.randomUUID().toString().substring(0, 8);
        this.parent = parent;
        this.startTime = Instant.now();
        this.debugMode = debugMode;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull String traceId() {
        return traceId;
    }

    @Override
    public @Nullable Trace parent() {
        return parent;
    }

    @Override
    public @NotNull Instant startTime() {
        return startTime;
    }

    @Override
    public @Nullable Instant endTime() {
        return endTime;
    }

    @Override
    public @NotNull Duration duration() {
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public @NotNull Trace checkpoint(@NotNull String name) {
        return checkpoint(name, null);
    }

    @Override
    public @NotNull Trace checkpoint(@NotNull String name, @Nullable String data) {
        Instant now = Instant.now();
        Duration elapsed = Duration.between(startTime, now);
        checkpoints.add(new Checkpoint(name, now, elapsed, data));

        if (debugMode) {
            logger.debug("Trace [{}] checkpoint {}: {}ms {}",
                    this.name, name, elapsed.toMillis(),
                    data != null ? "(" + data + ")" : "");
        }

        return this;
    }

    @Override
    public @NotNull List<Checkpoint> checkpoints() {
        return List.copyOf(checkpoints);
    }

    @Override
    public @NotNull Trace tag(@NotNull String key, @NotNull String value) {
        tags.put(key, value);
        return this;
    }

    @Override
    public @NotNull Map<String, String> tags() {
        return Map.copyOf(tags);
    }

    @Override
    public @NotNull Trace child(@NotNull String name) {
        DefaultTrace child = new DefaultTrace(name, this, debugMode);
        synchronized (children) {
            children.add(child);
        }
        return child;
    }

    @Override
    public @NotNull List<Trace> children() {
        synchronized (children) {
            return List.copyOf(children);
        }
    }

    @Override
    public @NotNull Trace error(@NotNull Throwable error) {
        this.error = error;
        if (debugMode) {
            logger.debug("Trace [{}] error: {}", name, error.getMessage());
        }
        return this;
    }

    @Override
    public @NotNull Trace error(@NotNull String message) {
        this.error = new RuntimeException(message);
        if (debugMode) {
            logger.debug("Trace [{}] error: {}", name, message);
        }
        return this;
    }

    @Override
    public @Nullable Throwable error() {
        return error;
    }

    @Override
    public void end() {
        if (active) {
            active = false;
            endTime = Instant.now();

            // End all children
            synchronized (children) {
                for (Trace child : children) {
                    if (child.isActive()) {
                        child.end();
                    }
                }
            }

            if (debugMode) {
                logger.debug("Trace [{}] ended: {}ms {}",
                        name, duration().toMillis(),
                        hasError() ? "(with error)" : "");
            }
        }
    }

    @Override
    public @NotNull String export() {
        StringBuilder sb = new StringBuilder();
        export(sb, 0);
        return sb.toString();
    }

    private void export(StringBuilder sb, int indent) {
        String prefix = "  ".repeat(indent);

        sb.append(prefix).append("Trace: ").append(name)
                .append(" [").append(traceId).append("]")
                .append(" (").append(duration().toMillis()).append("ms)");

        if (hasError()) {
            sb.append(" ERROR: ").append(error.getMessage());
        }
        sb.append("\n");

        // Tags
        if (!tags.isEmpty()) {
            sb.append(prefix).append("  Tags: ").append(tags).append("\n");
        }

        // Checkpoints
        for (Checkpoint cp : checkpoints) {
            sb.append(prefix).append("  - ").append(cp.name())
                    .append(" @ ").append(cp.elapsed().toMillis()).append("ms");
            if (cp.data() != null) {
                sb.append(" (").append(cp.data()).append(")");
            }
            sb.append("\n");
        }

        // Children
        synchronized (children) {
            for (Trace child : children) {
                if (child instanceof DefaultTrace dt) {
                    dt.export(sb, indent + 1);
                }
            }
        }
    }
}
