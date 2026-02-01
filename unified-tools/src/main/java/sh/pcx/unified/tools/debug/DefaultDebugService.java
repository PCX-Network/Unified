/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.debug;

import sh.pcx.unified.tools.debug.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Default implementation of {@link DebugService}.
 *
 * @since 1.0.0
 */
public final class DefaultDebugService implements DebugService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDebugService.class);

    private volatile boolean debugMode = false;
    private final DefaultProfiler profiler = new DefaultProfiler();
    private final DefaultMemoryMonitor memoryMonitor = new DefaultMemoryMonitor();
    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();

    @Override
    public boolean isDebugMode() {
        return debugMode;
    }

    @Override
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        if (enabled) {
            logger.info("Debug mode enabled");
        } else {
            logger.info("Debug mode disabled");
        }
    }

    @Override
    public @NotNull Trace trace(@NotNull String name) {
        return trace(name, null);
    }

    @Override
    public @NotNull Trace trace(@NotNull String name, @Nullable String context) {
        DefaultTrace trace = new DefaultTrace(name, context, debugMode);
        if (debugMode) {
            logger.debug("Starting trace: {} {}", name, context != null ? "(" + context + ")" : "");
        }
        return trace;
    }

    @Override
    public @NotNull ProfileResult profile(@NotNull String name, @NotNull Runnable runnable) {
        Instant start = Instant.now();
        Throwable error = null;

        try {
            runnable.run();
        } catch (Throwable t) {
            error = t;
        }

        Instant end = Instant.now();
        ProfileResult result = ProfileResult.of(name, start, end, error);

        if (debugMode) {
            logger.debug("Profiled {}: {}ms", name, result.durationMillis());
        }

        return result;
    }

    @Override
    public <T> ProfileResult.@NotNull WithValue<T> profile(@NotNull String name, @NotNull Supplier<T> supplier) {
        Instant start = Instant.now();
        T value = null;
        Throwable error = null;

        try {
            value = supplier.get();
        } catch (Throwable t) {
            error = t;
        }

        Instant end = Instant.now();
        ProfileResult.WithValue<T> result = ProfileResult.WithValue.of(name, start, end, value, error);

        if (debugMode) {
            logger.debug("Profiled {}: {}ms", name, result.durationMillis());
        }

        return result;
    }

    @Override
    public @NotNull Profiler profiler() {
        return profiler;
    }

    @Override
    public @NotNull MemoryMonitor memoryMonitor() {
        return memoryMonitor;
    }

    @Override
    public void dumpThreads() {
        List<ThreadInfo> threads = getThreadInfo();
        logger.info("=== Thread Dump ({} threads) ===", threads.size());

        for (ThreadInfo info : threads) {
            logger.info("Thread: {} (ID: {}, State: {}, Priority: {}, Daemon: {})",
                    info.name(), info.id(), info.state(), info.priority(), info.daemon());

            if (debugMode && info.stackTrace().length > 0) {
                for (StackTraceElement element : info.stackTrace()) {
                    logger.info("    at {}", element);
                }
            }
        }
    }

    @Override
    public @NotNull List<ThreadInfo> getThreadInfo() {
        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
        List<ThreadInfo> result = new ArrayList<>(allThreads.size());

        for (Map.Entry<Thread, StackTraceElement[]> entry : allThreads.entrySet()) {
            Thread thread = entry.getKey();
            result.add(new ThreadInfo(
                    thread.threadId(),
                    thread.getName(),
                    thread.getState(),
                    thread.getPriority(),
                    thread.isDaemon(),
                    entry.getValue()
            ));
        }

        return result;
    }

    @Override
    public void debug(@NotNull String message) {
        if (debugMode) {
            logger.debug(message);
        }
    }

    @Override
    public void debug(@NotNull String format, @NotNull Object... args) {
        if (debugMode) {
            logger.debug(format, args);
        }
    }

    @Override
    public void logTrace(@NotNull String message) {
        if (debugMode) {
            logger.trace(message);
        }
    }

    @Override
    public void logTrace(@NotNull String format, @NotNull Object... args) {
        if (debugMode) {
            logger.trace(format, args);
        }
    }

    @Override
    public @NotNull DebugTimer timer(@NotNull String name) {
        return new SimpleDebugTimer(name, debugMode);
    }

    @Override
    public void registerHealthCheck(@NotNull String name, @NotNull HealthCheck check) {
        healthChecks.put(name, check);
    }

    @Override
    public @NotNull List<HealthCheckResult> runHealthChecks() {
        List<HealthCheckResult> results = new ArrayList<>();

        for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
            try {
                results.add(entry.getValue().check());
            } catch (Throwable t) {
                results.add(HealthCheckResult.unhealthy(entry.getKey(), t));
            }
        }

        return results;
    }

    @Override
    public String getServiceName() {
        return "DebugService";
    }

    /**
     * Simple debug timer implementation.
     */
    private static final class SimpleDebugTimer implements DebugTimer {
        private static final Logger timerLogger = LoggerFactory.getLogger(SimpleDebugTimer.class);

        private final String name;
        private final boolean debugMode;
        private final long startNanos;
        private final List<String> checkpoints = new ArrayList<>();
        private volatile boolean stopped = false;

        SimpleDebugTimer(String name, boolean debugMode) {
            this.name = name;
            this.debugMode = debugMode;
            this.startNanos = System.nanoTime();
            if (debugMode) {
                timerLogger.debug("Timer started: {}", name);
            }
        }

        @Override
        public @NotNull Duration elapsed() {
            return Duration.ofNanos(System.nanoTime() - startNanos);
        }

        @Override
        public void checkpoint(@NotNull String checkpointName) {
            Duration elapsed = elapsed();
            checkpoints.add(checkpointName + ": " + elapsed.toMillis() + "ms");
            if (debugMode) {
                timerLogger.debug("Timer {} checkpoint {}: {}ms", name, checkpointName, elapsed.toMillis());
            }
        }

        @Override
        public void stop() {
            if (!stopped) {
                stopped = true;
                Duration elapsed = elapsed();
                if (debugMode) {
                    timerLogger.debug("Timer {} stopped: {}ms", name, elapsed.toMillis());
                    for (String checkpoint : checkpoints) {
                        timerLogger.debug("  {}", checkpoint);
                    }
                }
            }
        }
    }
}
