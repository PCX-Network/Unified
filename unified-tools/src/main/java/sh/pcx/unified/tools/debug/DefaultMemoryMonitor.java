/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import sh.pcx.unified.tools.debug.MemoryDelta;
import sh.pcx.unified.tools.debug.MemoryMonitor;
import sh.pcx.unified.tools.debug.MemorySnapshot;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Default implementation of {@link MemoryMonitor}.
 *
 * @since 1.0.0
 */
public final class DefaultMemoryMonitor implements MemoryMonitor {

    private final List<MemorySnapshot> history = new CopyOnWriteArrayList<>();
    private volatile int maxHistorySize = 1000;
    private volatile ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> monitoringTask;
    private volatile Consumer<MemorySnapshot> warningCallback;
    private volatile Consumer<MemorySnapshot> criticalCallback;
    private volatile double warningThreshold = 80.0;
    private volatile double criticalThreshold = 95.0;

    @Override
    public @NotNull MemorySnapshot snapshot() {
        return MemorySnapshot.now();
    }

    @Override
    public @NotNull MemoryDelta compare(@NotNull MemorySnapshot before, @NotNull MemorySnapshot after) {
        return MemoryDelta.of(before, after);
    }

    @Override
    public void startMonitoring(@NotNull Duration interval, @NotNull Consumer<MemorySnapshot> callback) {
        stopMonitoring();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UnifiedPlugin-MemoryMonitor");
            t.setDaemon(true);
            return t;
        });

        monitoringTask = scheduler.scheduleAtFixedRate(() -> {
            MemorySnapshot snap = snapshot();

            // Add to history
            history.add(snap);
            while (history.size() > maxHistorySize) {
                history.removeFirst();
            }

            // Invoke callback
            callback.accept(snap);

            // Check thresholds
            double usage = snap.usagePercentage();
            if (criticalCallback != null && usage >= criticalThreshold) {
                criticalCallback.accept(snap);
            } else if (warningCallback != null && usage >= warningThreshold) {
                warningCallback.accept(snap);
            }

        }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopMonitoring() {
        if (monitoringTask != null) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    public boolean isMonitoring() {
        return monitoringTask != null && !monitoringTask.isCancelled();
    }

    @Override
    public @NotNull List<MemorySnapshot> history() {
        return List.copyOf(history);
    }

    @Override
    public int maxHistorySize() {
        return maxHistorySize;
    }

    @Override
    public void setMaxHistorySize(int size) {
        this.maxHistorySize = size;
        while (history.size() > size) {
            history.removeFirst();
        }
    }

    @Override
    public void clearHistory() {
        history.clear();
    }

    @Override
    public void requestGc() {
        System.gc();
    }

    @Override
    public @NotNull List<MemoryPoolStats> memoryPools() {
        List<MemoryPoolStats> stats = new ArrayList<>();

        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            stats.add(new MemoryPoolStats(
                    pool.getName(),
                    pool.getType().name(),
                    pool.getUsage().getUsed(),
                    pool.getUsage().getCommitted(),
                    pool.getUsage().getMax()
            ));
        }

        return stats;
    }

    @Override
    public void setWarningThreshold(double percentage, @NotNull Consumer<MemorySnapshot> callback) {
        this.warningThreshold = percentage;
        this.warningCallback = callback;
    }

    @Override
    public void setCriticalThreshold(double percentage, @NotNull Consumer<MemorySnapshot> callback) {
        this.criticalThreshold = percentage;
        this.criticalCallback = callback;
    }

    @Override
    public @NotNull MemoryAnalysis analyze() {
        MemorySnapshot current = snapshot();
        List<MemoryPoolStats> pools = memoryPools();
        List<MemorySnapshot> recentHistory = history.stream()
                .skip(Math.max(0, history.size() - 10))
                .toList();

        return new DefaultMemoryAnalysis(current, pools, recentHistory);
    }

    /**
     * Default memory analysis implementation.
     */
    private record DefaultMemoryAnalysis(
            MemorySnapshot current,
            List<MemoryPoolStats> pools,
            List<MemorySnapshot> recentHistory
    ) implements MemoryAnalysis {

        @Override
        public @NotNull HealthStatus status() {
            double usage = current.usagePercentage();
            if (usage >= 95) return HealthStatus.CRITICAL;
            if (usage >= 80) return HealthStatus.WARNING;
            return HealthStatus.HEALTHY;
        }

        @Override
        public @NotNull List<String> messages() {
            List<String> messages = new ArrayList<>();
            messages.add(String.format("Heap usage: %.1f%% (%.1f MB / %.1f MB)",
                    current.usagePercentage(),
                    current.usedMemoryMB(),
                    current.maxMemoryMB()));

            for (MemoryPoolStats pool : pools) {
                if (pool.usagePercentage() > 80) {
                    messages.add(String.format("Pool %s: %.1f%% usage", pool.name(), pool.usagePercentage()));
                }
            }

            return messages;
        }

        @Override
        public @NotNull List<String> recommendations() {
            List<String> recs = new ArrayList<>();

            double usage = current.usagePercentage();
            if (usage >= 95) {
                recs.add("CRITICAL: Memory is almost exhausted. Immediate action required.");
                recs.add("Consider increasing heap size with -Xmx flag.");
                recs.add("Review for memory leaks.");
            } else if (usage >= 80) {
                recs.add("WARNING: Memory usage is high.");
                recs.add("Monitor for potential memory leaks.");
                recs.add("Consider increasing heap size if this persists.");
            } else if (usage >= 60) {
                recs.add("Memory usage is moderate. Continue monitoring.");
            }

            // Check for memory growth trend
            if (recentHistory.size() >= 5) {
                long first = recentHistory.getFirst().usedMemory();
                long last = recentHistory.getLast().usedMemory();
                if (last > first * 1.2) {
                    recs.add("Memory appears to be growing. Check for leaks.");
                }
            }

            return recs;
        }

        @Override
        public @NotNull List<Issue> issues() {
            List<Issue> issues = new ArrayList<>();

            double usage = current.usagePercentage();
            if (usage >= 95) {
                issues.add(new Issue(
                        Issue.Severity.ERROR,
                        "Critical memory usage: " + String.format("%.1f%%", usage),
                        "Increase heap size or reduce memory consumption"
                ));
            } else if (usage >= 80) {
                issues.add(new Issue(
                        Issue.Severity.WARNING,
                        "High memory usage: " + String.format("%.1f%%", usage),
                        "Monitor closely and consider optimization"
                ));
            }

            for (MemoryPoolStats pool : pools) {
                if (pool.usagePercentage() > 90) {
                    issues.add(new Issue(
                            Issue.Severity.WARNING,
                            "Memory pool " + pool.name() + " is near capacity",
                            null
                    ));
                }
            }

            return issues;
        }
    }
}
