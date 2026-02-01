/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import sh.pcx.unified.tools.metrics.Gauge;
import sh.pcx.unified.tools.metrics.Metric;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.DoubleSupplier;

/**
 * Simple thread-safe implementation of {@link Gauge}.
 *
 * @since 1.0.0
 */
public final class SimpleGauge implements Gauge {

    private final String name;
    private final String description;
    private final String[] labelNames;
    private final Map<String, String> metadata;
    private final DoubleAdder value;
    private final Map<LabelKey, LabeledGauge> children;

    /**
     * Creates a new gauge.
     *
     * @param name        the gauge name
     * @param description the description
     * @param labelNames  the label names
     */
    public SimpleGauge(
            @NotNull String name,
            @Nullable String description,
            @NotNull String... labelNames
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.labelNames = labelNames.clone();
        this.metadata = new ConcurrentHashMap<>();
        this.value = new DoubleAdder();
        this.children = new ConcurrentHashMap<>();
    }

    @Override
    public void set(double value) {
        // DoubleAdder doesn't support set, so we reset and add
        synchronized (this.value) {
            this.value.reset();
            this.value.add(value);
        }
    }

    @Override
    public double get() {
        return value.sum();
    }

    @Override
    public void increment(double amount) {
        value.add(amount);
    }

    @Override
    public void decrement(double amount) {
        value.add(-amount);
    }

    @Override
    public void reset() {
        value.reset();
    }

    @Override
    public @NotNull Gauge labels(@NotNull String... labelValues) {
        if (labelValues.length != labelNames.length) {
            throw new IllegalArgumentException(
                    "Expected " + labelNames.length + " label values, got " + labelValues.length
            );
        }

        var key = new LabelKey(labelValues);
        return children.computeIfAbsent(key, k -> new LabeledGauge(this, labelValues));
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
        return Type.GAUGE;
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
    public Map<LabelKey, LabeledGauge> children() {
        return children;
    }

    /**
     * Key for labeled gauge lookups.
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
     * A labeled child gauge.
     */
    public static final class LabeledGauge implements Gauge.Labeled {
        private final SimpleGauge parent;
        private final String[] labelValues;
        private final DoubleAdder value;

        LabeledGauge(SimpleGauge parent, String[] labelValues) {
            this.parent = parent;
            this.labelValues = labelValues.clone();
            this.value = new DoubleAdder();
        }

        @Override
        public void set(double value) {
            synchronized (this.value) {
                this.value.reset();
                this.value.add(value);
            }
        }

        @Override
        public double get() {
            return value.sum();
        }

        @Override
        public void increment(double amount) {
            value.add(amount);
        }

        @Override
        public void decrement(double amount) {
            value.add(-amount);
        }

        @Override
        public void reset() {
            value.reset();
        }

        @Override
        public @NotNull Gauge labels(@NotNull String... labelValues) {
            throw new UnsupportedOperationException("Cannot add labels to a labeled gauge");
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
            return Type.GAUGE;
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

    /**
     * Supplier-backed gauge implementation.
     */
    public static final class SupplierGauge implements Gauge.Supplier {
        private final String name;
        private final String description;
        private final DoubleSupplier supplier;
        private final Map<String, String> metadata;

        /**
         * Creates a supplier-backed gauge.
         *
         * @param name        the gauge name
         * @param description the description
         * @param supplier    the value supplier
         */
        public SupplierGauge(
                @NotNull String name,
                @Nullable String description,
                @NotNull DoubleSupplier supplier
        ) {
            this.name = Objects.requireNonNull(name, "name");
            this.description = description;
            this.supplier = Objects.requireNonNull(supplier, "supplier");
            this.metadata = new ConcurrentHashMap<>();
        }

        @Override
        public double get() {
            return supplier.getAsDouble();
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
            return Type.GAUGE;
        }

        @Override
        public @NotNull String[] labelNames() {
            return new String[0];
        }

        @Override
        public @NotNull Map<String, String> metadata() {
            return metadata;
        }
    }
}
