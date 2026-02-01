/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import sh.pcx.unified.tools.metrics.Histogram;
import sh.pcx.unified.tools.metrics.Metric;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple thread-safe implementation of {@link Histogram}.
 *
 * @since 1.0.0
 */
public final class SimpleHistogram implements Histogram {

    private final String name;
    private final String description;
    private final String[] labelNames;
    private final Map<String, String> metadata;
    private final double[] bucketBoundaries;
    private final LongAdder[] bucketCounts;
    private final LongAdder count;
    private final DoubleAdder sum;
    private volatile double min = Double.MAX_VALUE;
    private volatile double max = Double.MIN_VALUE;
    private final Object minMaxLock = new Object();
    private final Map<LabelKey, SimpleHistogram> children;

    /**
     * Creates a new histogram with the given buckets.
     *
     * @param name        the histogram name
     * @param description the description
     * @param labelNames  the label names
     * @param buckets     the bucket boundaries
     */
    public SimpleHistogram(
            @NotNull String name,
            @Nullable String description,
            @NotNull String[] labelNames,
            @NotNull double[] buckets
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.labelNames = labelNames.clone();
        this.metadata = new ConcurrentHashMap<>();
        this.bucketBoundaries = buckets.clone();
        Arrays.sort(this.bucketBoundaries);

        // Create bucket counters (+1 for the +Inf bucket)
        this.bucketCounts = new LongAdder[buckets.length + 1];
        for (int i = 0; i < bucketCounts.length; i++) {
            bucketCounts[i] = new LongAdder();
        }

        this.count = new LongAdder();
        this.sum = new DoubleAdder();
        this.children = new ConcurrentHashMap<>();
    }

    @Override
    public void observe(double value) {
        count.increment();
        sum.add(value);

        // Update min/max
        synchronized (minMaxLock) {
            if (value < min) min = value;
            if (value > max) max = value;
        }

        // Find the correct bucket
        int bucketIndex = bucketBoundaries.length; // Default to +Inf bucket
        for (int i = 0; i < bucketBoundaries.length; i++) {
            if (value <= bucketBoundaries[i]) {
                bucketIndex = i;
                break;
            }
        }
        bucketCounts[bucketIndex].increment();
    }

    @Override
    public long count() {
        return count.sum();
    }

    @Override
    public double sum() {
        return sum.sum();
    }

    @Override
    public double min() {
        return min == Double.MAX_VALUE ? 0 : min;
    }

    @Override
    public double max() {
        return max == Double.MIN_VALUE ? 0 : max;
    }

    @Override
    public double percentile(double percentile) {
        if (percentile < 0 || percentile > 1) {
            throw new IllegalArgumentException("Percentile must be between 0 and 1: " + percentile);
        }

        long totalCount = count.sum();
        if (totalCount == 0) {
            return 0;
        }

        long targetCount = (long) (totalCount * percentile);
        long cumulative = 0;

        for (int i = 0; i < bucketCounts.length; i++) {
            cumulative += bucketCounts[i].sum();
            if (cumulative >= targetCount) {
                if (i == 0) {
                    return bucketBoundaries[0];
                } else if (i < bucketBoundaries.length) {
                    // Linear interpolation within the bucket
                    double lowerBound = i == 0 ? 0 : bucketBoundaries[i - 1];
                    double upperBound = bucketBoundaries[i];
                    return lowerBound + (upperBound - lowerBound) *
                            (percentile - (cumulative - bucketCounts[i].sum()) / (double) totalCount) /
                            (bucketCounts[i].sum() / (double) totalCount);
                } else {
                    return bucketBoundaries[bucketBoundaries.length - 1];
                }
            }
        }

        return max();
    }

    @Override
    public @NotNull long[] bucketCounts() {
        long[] counts = new long[bucketCounts.length];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = bucketCounts[i].sum();
        }
        return counts;
    }

    @Override
    public @NotNull double[] bucketBoundaries() {
        return bucketBoundaries.clone();
    }

    @Override
    public @NotNull Histogram labels(@NotNull String... labelValues) {
        if (labelValues.length != labelNames.length) {
            throw new IllegalArgumentException(
                    "Expected " + labelNames.length + " label values, got " + labelValues.length
            );
        }

        var key = new LabelKey(labelValues);
        return children.computeIfAbsent(key, k ->
                new SimpleHistogram(name, description, labelNames, bucketBoundaries)
        );
    }

    @Override
    public void reset() {
        count.reset();
        sum.reset();
        for (LongAdder adder : bucketCounts) {
            adder.reset();
        }
        synchronized (minMaxLock) {
            min = Double.MAX_VALUE;
            max = Double.MIN_VALUE;
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
        return Type.HISTOGRAM;
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
    public Map<LabelKey, SimpleHistogram> children() {
        return children;
    }

    /**
     * Key for labeled histogram lookups.
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
     * Builder implementation for histograms.
     */
    public static class HistogramBuilder implements Histogram.Builder {
        private final String name;
        private final String description;
        private double[] buckets = DEFAULT_BUCKETS;
        private String[] labelNames = new String[0];

        /**
         * Creates a new histogram builder.
         *
         * @param name        the histogram name
         * @param description the description
         */
        public HistogramBuilder(@NotNull String name, @Nullable String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public @NotNull Builder buckets(double... boundaries) {
            this.buckets = boundaries.clone();
            return this;
        }

        @Override
        public @NotNull Builder linearBuckets(double start, double width, int count) {
            double[] newBuckets = new double[count];
            for (int i = 0; i < count; i++) {
                newBuckets[i] = start + (i * width);
            }
            this.buckets = newBuckets;
            return this;
        }

        @Override
        public @NotNull Builder exponentialBuckets(double start, double factor, int count) {
            double[] newBuckets = new double[count];
            for (int i = 0; i < count; i++) {
                newBuckets[i] = start * Math.pow(factor, i);
            }
            this.buckets = newBuckets;
            return this;
        }

        @Override
        public @NotNull Builder labelNames(@NotNull String... labelNames) {
            this.labelNames = labelNames.clone();
            return this;
        }

        @Override
        public @NotNull Histogram build() {
            return new SimpleHistogram(name, description, labelNames, buckets);
        }
    }
}
