/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import sh.pcx.unified.tools.metrics.Metric;
import sh.pcx.unified.tools.metrics.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple thread-safe implementation of {@link Timer}.
 *
 * @since 1.0.0
 */
public final class SimpleTimer implements Timer {

    private final String name;
    private final String description;
    private final String[] labelNames;
    private final Map<String, String> metadata;
    private final LongAdder count;
    private final LongAdder totalNanos;
    private volatile long maxNanos;
    private volatile long minNanos = Long.MAX_VALUE;
    private final Map<LabelKey, SimpleTimer> children;
    private final Object minMaxLock = new Object();

    /**
     * Creates a new timer.
     *
     * @param name        the timer name
     * @param description the description
     * @param labelNames  the label names
     */
    public SimpleTimer(
            @NotNull String name,
            @Nullable String description,
            @NotNull String... labelNames
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.labelNames = labelNames.clone();
        this.metadata = new ConcurrentHashMap<>();
        this.count = new LongAdder();
        this.totalNanos = new LongAdder();
        this.children = new ConcurrentHashMap<>();
    }

    @Override
    public @NotNull Context time() {
        return new TimerContext(this);
    }

    @Override
    public void record(@NotNull Duration duration) {
        record(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void record(long amount, @NotNull TimeUnit unit) {
        long nanos = unit.toNanos(amount);
        count.increment();
        totalNanos.add(nanos);

        synchronized (minMaxLock) {
            if (nanos > maxNanos) {
                maxNanos = nanos;
            }
            if (nanos < minNanos) {
                minNanos = nanos;
            }
        }
    }

    @Override
    public long count() {
        return count.sum();
    }

    @Override
    public @NotNull Duration totalTime() {
        return Duration.ofNanos(totalNanos.sum());
    }

    @Override
    public @NotNull Duration mean() {
        long c = count.sum();
        if (c == 0) {
            return Duration.ZERO;
        }
        return Duration.ofNanos(totalNanos.sum() / c);
    }

    @Override
    public @NotNull Duration max() {
        return maxNanos == 0 ? Duration.ZERO : Duration.ofNanos(maxNanos);
    }

    @Override
    public @NotNull Duration min() {
        return minNanos == Long.MAX_VALUE ? Duration.ZERO : Duration.ofNanos(minNanos);
    }

    @Override
    public @NotNull Timer labels(@NotNull String... labelValues) {
        if (labelValues.length != labelNames.length) {
            throw new IllegalArgumentException(
                    "Expected " + labelNames.length + " label values, got " + labelValues.length
            );
        }

        var key = new LabelKey(labelValues);
        return children.computeIfAbsent(key, k ->
                new SimpleTimer(name, description, labelNames)
        );
    }

    @Override
    public void reset() {
        count.reset();
        totalNanos.reset();
        synchronized (minMaxLock) {
            maxNanos = 0;
            minNanos = Long.MAX_VALUE;
        }
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @Nullable String description() {
        return description;
    }

    @Override
    public @NotNull Type type() {
        return Type.TIMER;
    }

    @Override
    public @NotNull String[] labelNames() {
        return labelNames.clone();
    }

    @Override
    public @NotNull Map<String, String> metadata() {
        return metadata;
    }

    /**
     * Returns all labeled children.
     *
     * @return the children map
     */
    public Map<LabelKey, SimpleTimer> children() {
        return children;
    }

    /**
     * Key for labeled timer lookups.
     */
    record LabelKey(String[] values) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LabelKey that)) return false;
            return Arrays.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }
    }

    /**
     * Timer context for measuring duration.
     */
    private static final class TimerContext implements Timer.Context {
        private final SimpleTimer timer;
        private final long startNanos;
        private volatile long endNanos;
        private volatile boolean stopped;

        TimerContext(SimpleTimer timer) {
            this.timer = timer;
            this.startNanos = System.nanoTime();
        }

        @Override
        public long stop() {
            if (!stopped) {
                stopped = true;
                endNanos = System.nanoTime();
                long elapsed = endNanos - startNanos;
                timer.record(elapsed, TimeUnit.NANOSECONDS);
                return elapsed;
            }
            return endNanos - startNanos;
        }

        @Override
        public long elapsed() {
            if (stopped) {
                return endNanos - startNanos;
            }
            return System.nanoTime() - startNanos;
        }
    }
}
