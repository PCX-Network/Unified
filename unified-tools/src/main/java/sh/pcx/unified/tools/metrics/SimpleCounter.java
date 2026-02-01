/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import sh.pcx.unified.tools.metrics.Counter;
import sh.pcx.unified.tools.metrics.Metric;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple thread-safe implementation of {@link Counter}.
 *
 * @since 1.0.0
 */
public final class SimpleCounter implements Counter {

    private final String name;
    private final String description;
    private final String[] labelNames;
    private final Map<String, String> metadata;
    private final LongAdder value;
    private final Map<LabelKey, LabeledCounter> children;

    /**
     * Creates a new counter.
     *
     * @param name        the counter name
     * @param description the description
     * @param labelNames  the label names
     */
    public SimpleCounter(
            @NotNull String name,
            @Nullable String description,
            @NotNull String... labelNames
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.labelNames = labelNames.clone();
        this.metadata = new ConcurrentHashMap<>();
        this.value = new LongAdder();
        this.children = new ConcurrentHashMap<>();
    }

    @Override
    public void increment() {
        value.increment();
    }

    @Override
    public void increment(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Counter increment must be non-negative: " + amount);
        }
        value.add(amount);
    }

    @Override
    public long get() {
        return value.sum();
    }

    @Override
    public void reset() {
        value.reset();
    }

    @Override
    public @NotNull Counter labels(@NotNull String... labelValues) {
        if (labelValues.length != labelNames.length) {
            throw new IllegalArgumentException(
                    "Expected " + labelNames.length + " label values, got " + labelValues.length
            );
        }

        var key = new LabelKey(labelValues);
        return children.computeIfAbsent(key, k -> new LabeledCounter(this, labelValues));
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
        return Type.COUNTER;
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
    public Map<LabelKey, LabeledCounter> children() {
        return children;
    }

    /**
     * Key for labeled counter lookups.
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
     * A labeled child counter.
     */
    public static final class LabeledCounter implements Counter.Labeled {
        private final SimpleCounter parent;
        private final String[] labelValues;
        private final LongAdder value;

        LabeledCounter(SimpleCounter parent, String[] labelValues) {
            this.parent = parent;
            this.labelValues = labelValues.clone();
            this.value = new LongAdder();
        }

        @Override
        public void increment() {
            value.increment();
        }

        @Override
        public void increment(long amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("Counter increment must be non-negative: " + amount);
            }
            value.add(amount);
        }

        @Override
        public long get() {
            return value.sum();
        }

        @Override
        public void reset() {
            value.reset();
        }

        @Override
        public @NotNull Counter labels(@NotNull String... labelValues) {
            throw new UnsupportedOperationException("Cannot add labels to a labeled counter");
        }

        @Override
        public @NotNull String[] labelValues() {
            return labelValues.clone();
        }

        @Override
        public @NotNull String name() {
            return parent.name();
        }

        @Override
        public @Nullable String description() {
            return parent.description();
        }

        @Override
        public @NotNull Type type() {
            return Type.COUNTER;
        }

        @Override
        public @NotNull String[] labelNames() {
            return parent.labelNames();
        }

        @Override
        public @NotNull Map<String, String> metadata() {
            return parent.metadata();
        }
    }
}
