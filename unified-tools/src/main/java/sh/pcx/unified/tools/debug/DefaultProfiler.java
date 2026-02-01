/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sh.pcx.unified.tools.debug.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link Profiler}.
 *
 * @since 1.0.0
 */
public final class DefaultProfiler implements Profiler {

    private final Map<String, SectionData> sections = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private volatile long startTimeNanos;
    private volatile long endTimeNanos;
    private int samplingIntervalMs = 10;
    private boolean stackSamplingEnabled = false;

    @Override
    public @NotNull Profiler start() {
        running = true;
        startTimeNanos = System.nanoTime();
        sections.clear();
        return this;
    }

    @Override
    public @NotNull ProfileReport stop() {
        running = false;
        endTimeNanos = System.nanoTime();
        return createReport();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public @NotNull Profiler reset() {
        running = false;
        sections.clear();
        startTimeNanos = 0;
        endTimeNanos = 0;
        return this;
    }

    @Override
    public @NotNull Section section(@NotNull String name) {
        return new DefaultSection(name, this);
    }

    @Override
    public void profile(@NotNull String name, @NotNull Runnable runnable) {
        try (Section section = section(name)) {
            runnable.run();
        }
    }

    @Override
    public <T> T profile(@NotNull String name, @NotNull Supplier<T> supplier) {
        try (Section section = section(name)) {
            return supplier.get();
        }
    }

    @Override
    public <T> T profileCallable(@NotNull String name, @NotNull Callable<T> callable) throws Exception {
        try (Section section = section(name)) {
            return callable.call();
        }
    }

    @Override
    public @NotNull ProfileReport currentReport() {
        return createReport();
    }

    @Override
    public @NotNull Profiler setSamplingInterval(int intervalMs) {
        this.samplingIntervalMs = intervalMs;
        return this;
    }

    @Override
    public @NotNull Profiler setStackSampling(boolean enabled) {
        this.stackSamplingEnabled = enabled;
        return this;
    }

    @Override
    public @NotNull String flamegraph() {
        StringBuilder sb = new StringBuilder();

        for (SectionData data : sections.values()) {
            for (Map.Entry<String, Long> entry : data.stackSamples.entrySet()) {
                sb.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    void recordSection(String name, long durationNanos) {
        SectionData data = sections.computeIfAbsent(name, SectionData::new);
        data.record(durationNanos);

        if (stackSamplingEnabled) {
            String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                    .skip(3)
                    .limit(10)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining(";"));
            data.recordStack(name + ";" + stack);
        }
    }

    private ProfileReport createReport() {
        Duration totalDuration = Duration.ofNanos(
                (endTimeNanos > 0 ? endTimeNanos : System.nanoTime()) - startTimeNanos
        );

        List<SectionStats> stats = sections.values().stream()
                .map(data -> data.toStats(totalDuration))
                .sorted((a, b) -> Long.compare(b.totalTime().toNanos(), a.totalTime().toNanos()))
                .collect(Collectors.toList());

        return new DefaultProfileReport(totalDuration, stats);
    }

    /**
     * Internal data holder for section statistics.
     */
    private static final class SectionData {
        final String name;
        final LongAdder count = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
        volatile long minNanos = Long.MAX_VALUE;
        volatile long maxNanos = 0;
        final Map<String, Long> stackSamples = new ConcurrentHashMap<>();
        final Object minMaxLock = new Object();

        SectionData(String name) {
            this.name = name;
        }

        void record(long nanos) {
            count.increment();
            totalNanos.add(nanos);

            synchronized (minMaxLock) {
                if (nanos < minNanos) minNanos = nanos;
                if (nanos > maxNanos) maxNanos = nanos;
            }
        }

        void recordStack(String stack) {
            stackSamples.merge(stack, 1L, Long::sum);
        }

        SectionStats toStats(Duration totalProfileDuration) {
            long c = count.sum();
            long total = totalNanos.sum();
            double percentage = totalProfileDuration.toNanos() > 0
                    ? (double) total / totalProfileDuration.toNanos() * 100
                    : 0;

            return new DefaultSectionStats(
                    name,
                    c,
                    Duration.ofNanos(total),
                    c > 0 ? Duration.ofNanos(total / c) : Duration.ZERO,
                    minNanos == Long.MAX_VALUE ? Duration.ZERO : Duration.ofNanos(minNanos),
                    maxNanos == 0 ? Duration.ZERO : Duration.ofNanos(maxNanos),
                    percentage,
                    Map.copyOf(stackSamples)
            );
        }
    }

    /**
     * Default section implementation.
     */
    private static final class DefaultSection implements Section {
        private final String name;
        private final DefaultProfiler profiler;
        private final long startNanos;
        private volatile boolean ended = false;

        DefaultSection(String name, DefaultProfiler profiler) {
            this.name = name;
            this.profiler = profiler;
            this.startNanos = System.nanoTime();
        }

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull Duration elapsed() {
            return Duration.ofNanos(System.nanoTime() - startNanos);
        }

        @Override
        public void end() {
            if (!ended) {
                ended = true;
                profiler.recordSection(name, System.nanoTime() - startNanos);
            }
        }
    }

    /**
     * Default profile report implementation.
     */
    private record DefaultProfileReport(
            Duration totalDuration,
            List<SectionStats> sectionStats
    ) implements ProfileReport {

        @Override
        public @NotNull Duration totalDuration() {
            return totalDuration;
        }

        @Override
        public @NotNull List<SectionStats> sections() {
            return sectionStats;
        }

        @Override
        public @Nullable SectionStats section(@NotNull String name) {
            return sectionStats.stream()
                    .filter(s -> s.name().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public @NotNull List<SectionStats> topByTime(int n) {
            return sectionStats.stream()
                    .sorted((a, b) -> Long.compare(b.totalTime().toNanos(), a.totalTime().toNanos()))
                    .limit(n)
                    .collect(Collectors.toList());
        }

        @Override
        public @NotNull List<SectionStats> topByCount(int n) {
            return sectionStats.stream()
                    .sorted((a, b) -> Long.compare(b.callCount(), a.callCount()))
                    .limit(n)
                    .collect(Collectors.toList());
        }

        @Override
        public @NotNull String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Profile Report\n");
            sb.append("==============\n");
            sb.append("Total Duration: ").append(totalDuration.toMillis()).append("ms\n");
            sb.append("Sections:\n");

            for (SectionStats stats : sectionStats) {
                sb.append(String.format("  %-30s %6d calls, %8dms total, %6dms avg (%.1f%%)\n",
                        stats.name(),
                        stats.callCount(),
                        stats.totalTime().toMillis(),
                        stats.averageTime().toMillis(),
                        stats.percentageOfTotal()));
            }

            return sb.toString();
        }

        @Override
        public @NotNull String toJson() {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("totalDurationMs", totalDuration.toMillis());
            data.put("sections", sectionStats.stream().map(s -> {
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("name", s.name());
                section.put("callCount", s.callCount());
                section.put("totalTimeMs", s.totalTime().toMillis());
                section.put("averageTimeMs", s.averageTime().toMillis());
                section.put("minTimeMs", s.minTime().toMillis());
                section.put("maxTimeMs", s.maxTime().toMillis());
                section.put("percentageOfTotal", s.percentageOfTotal());
                return section;
            }).collect(Collectors.toList()));
            return gson.toJson(data);
        }
    }

    /**
     * Default section stats implementation.
     */
    private record DefaultSectionStats(
            String name,
            long callCount,
            Duration totalTime,
            Duration averageTime,
            Duration minTime,
            Duration maxTime,
            double percentageOfTotal,
            Map<String, Long> stackSamples
    ) implements SectionStats {

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public long callCount() {
            return callCount;
        }

        @Override
        public @NotNull Duration totalTime() {
            return totalTime;
        }

        @Override
        public @NotNull Duration averageTime() {
            return averageTime;
        }

        @Override
        public @NotNull Duration minTime() {
            return minTime;
        }

        @Override
        public @NotNull Duration maxTime() {
            return maxTime;
        }

        @Override
        public double percentageOfTotal() {
            return percentageOfTotal;
        }

        @Override
        public @NotNull Map<String, Long> stackSamples() {
            return stackSamples;
        }
    }
}
