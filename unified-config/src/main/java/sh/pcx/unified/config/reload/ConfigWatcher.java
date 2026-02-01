/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.reload;

import sh.pcx.unified.config.ConfigException;
import sh.pcx.unified.config.format.ConfigFormat;
import sh.pcx.unified.config.format.ConfigLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches configuration files for changes and triggers reload callbacks.
 *
 * <p>ConfigWatcher uses the Java NIO WatchService to efficiently monitor
 * configuration files for modifications. It includes debouncing to avoid
 * multiple rapid reloads when files are being edited.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Efficient watching:</b> Uses OS-native file watching</li>
 *   <li><b>Debouncing:</b> Prevents rapid consecutive reloads</li>
 *   <li><b>Error handling:</b> Graceful handling of reload failures</li>
 *   <li><b>Thread safety:</b> Safe for use from multiple threads</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * ConfigWatcher watcher = new ConfigWatcher(configLoader);
 *
 * // Start watching a file
 * watcher.watch(PluginConfig.class, configPath, result -> {
 *     if (result.isSuccess()) {
 *         applyConfig(result.get());
 *     }
 * });
 *
 * // Stop watching
 * watcher.unwatch(configPath);
 *
 * // Shutdown when done
 * watcher.shutdown();
 * }</pre>
 *
 * <h2>Debounce Configuration</h2>
 * <pre>{@code
 * // Create watcher with custom debounce time
 * ConfigWatcher watcher = new ConfigWatcher(configLoader, 500, TimeUnit.MILLISECONDS);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ReloadHandler
 * @see ReloadResult
 */
public class ConfigWatcher implements AutoCloseable {

    private static final long DEFAULT_DEBOUNCE_MS = 300;

    private final ConfigLoader configLoader;
    private final long debounceMs;
    private final Map<Path, WatchEntry<?>> watchEntries;
    private final Map<Path, ScheduledFuture<?>> pendingReloads;
    private final ScheduledExecutorService debounceExecutor;
    private final ExecutorService watchExecutor;
    private final AtomicBoolean running;

    private WatchService watchService;
    private Thread watchThread;

    /**
     * Creates a new ConfigWatcher with default settings.
     *
     * @param configLoader the config loader to use for reloading
     */
    public ConfigWatcher(@NotNull ConfigLoader configLoader) {
        this(configLoader, DEFAULT_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new ConfigWatcher with custom debounce time.
     *
     * @param configLoader the config loader to use for reloading
     * @param debounceTime the debounce time
     * @param timeUnit the time unit
     */
    public ConfigWatcher(
            @NotNull ConfigLoader configLoader,
            long debounceTime,
            @NotNull TimeUnit timeUnit
    ) {
        this.configLoader = configLoader;
        this.debounceMs = timeUnit.toMillis(debounceTime);
        this.watchEntries = new ConcurrentHashMap<>();
        this.pendingReloads = new ConcurrentHashMap<>();
        this.debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConfigWatcher-Debounce");
            t.setDaemon(true);
            return t;
        });
        this.watchExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ConfigWatcher-Handler");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
    }

    /**
     * Starts the watch service.
     *
     * @throws ConfigException if the watch service cannot be started
     * @since 1.0.0
     */
    public synchronized void start() {
        if (running.get()) {
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            running.set(true);

            watchThread = new Thread(this::watchLoop, "ConfigWatcher-Main");
            watchThread.setDaemon(true);
            watchThread.start();

        } catch (IOException e) {
            throw new ConfigException("Failed to start config watcher", e);
        }
    }

    /**
     * Registers a file for watching.
     *
     * @param type the configuration class type
     * @param path the path to watch
     * @param handler the reload handler
     * @param <T> the configuration type
     * @since 1.0.0
     */
    public <T> void watch(
            @NotNull Class<T> type,
            @NotNull Path path,
            @NotNull ReloadHandler<T> handler
    ) {
        watch(type, path, null, handler);
    }

    /**
     * Registers a file for watching with explicit format.
     *
     * @param type the configuration class type
     * @param path the path to watch
     * @param format the configuration format (null for auto-detect)
     * @param handler the reload handler
     * @param <T> the configuration type
     * @since 1.0.0
     */
    public <T> void watch(
            @NotNull Class<T> type,
            @NotNull Path path,
            @Nullable ConfigFormat format,
            @NotNull ReloadHandler<T> handler
    ) {
        Path absolutePath = path.toAbsolutePath().normalize();
        Path directory = absolutePath.getParent();

        if (directory == null) {
            throw new ConfigException("Cannot watch root directory");
        }

        // Ensure watcher is started
        if (!running.get()) {
            start();
        }

        // Register the directory for watching
        try {
            directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE
            );
        } catch (IOException e) {
            throw new ConfigException("Failed to register watch for " + path, e);
        }

        // Store the watch entry
        ConfigFormat detectedFormat = format != null ? format : ConfigFormat.fromPath(path);
        watchEntries.put(absolutePath, new WatchEntry<>(type, detectedFormat, handler));
    }

    /**
     * Unregisters a file from watching.
     *
     * @param path the path to stop watching
     * @since 1.0.0
     */
    public void unwatch(@NotNull Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        watchEntries.remove(absolutePath);

        // Cancel any pending reload
        ScheduledFuture<?> pending = pendingReloads.remove(absolutePath);
        if (pending != null) {
            pending.cancel(false);
        }
    }

    /**
     * Forces a reload of a watched file.
     *
     * @param path the path to reload
     * @since 1.0.0
     */
    public void forceReload(@NotNull Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        WatchEntry<?> entry = watchEntries.get(absolutePath);

        if (entry != null) {
            triggerReload(absolutePath, entry, false);
        }
    }

    /**
     * Checks if a path is being watched.
     *
     * @param path the path to check
     * @return true if the path is being watched
     * @since 1.0.0
     */
    public boolean isWatching(@NotNull Path path) {
        return watchEntries.containsKey(path.toAbsolutePath().normalize());
    }

    /**
     * Gets the number of files being watched.
     *
     * @return the watch count
     * @since 1.0.0
     */
    public int getWatchCount() {
        return watchEntries.size();
    }

    /**
     * Shuts down the watcher.
     *
     * @since 1.0.0
     */
    public void shutdown() {
        running.set(false);

        // Cancel pending reloads
        pendingReloads.values().forEach(f -> f.cancel(true));
        pendingReloads.clear();

        // Shutdown executors
        debounceExecutor.shutdownNow();
        watchExecutor.shutdownNow();

        // Close watch service
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }

        // Interrupt watch thread
        if (watchThread != null) {
            watchThread.interrupt();
        }

        watchEntries.clear();
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Main watch loop.
     */
    private void watchLoop() {
        while (running.get()) {
            try {
                WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);

                if (key == null) {
                    continue;
                }

                Path directory = (Path) key.watchable();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();
                    Path fullPath = directory.resolve(filename).normalize();

                    // Check if we're watching this file
                    WatchEntry<?> entry = watchEntries.get(fullPath);
                    if (entry != null) {
                        scheduleReload(fullPath, entry);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }
    }

    /**
     * Schedules a reload with debouncing.
     */
    private void scheduleReload(@NotNull Path path, @NotNull WatchEntry<?> entry) {
        // Cancel any pending reload for this path
        ScheduledFuture<?> existing = pendingReloads.get(path);
        if (existing != null) {
            existing.cancel(false);
        }

        // Schedule new reload
        ScheduledFuture<?> future = debounceExecutor.schedule(
                () -> triggerReload(path, entry, true),
                debounceMs,
                TimeUnit.MILLISECONDS
        );

        pendingReloads.put(path, future);
    }

    /**
     * Triggers the actual reload.
     */
    private <T> void triggerReload(
            @NotNull Path path,
            @NotNull WatchEntry<T> entry,
            boolean wasModified
    ) {
        pendingReloads.remove(path);

        watchExecutor.submit(() -> {
            try {
                // Check if file exists
                if (!Files.exists(path)) {
                    entry.handler.onReload(ReloadResult.failure(
                            "File no longer exists: " + path,
                            path
                    ));
                    return;
                }

                // Load the configuration
                T config = configLoader.load(path, entry.format, entry.type);

                // Call the handler
                entry.handler.onReload(ReloadResult.success(config, path, wasModified));

            } catch (Exception e) {
                entry.handler.onReload(ReloadResult.failure(e, path));
            }
        });
    }

    /**
     * Holds information about a watched file.
     */
    private record WatchEntry<T>(
            Class<T> type,
            ConfigFormat format,
            ReloadHandler<T> handler
    ) {
    }
}
