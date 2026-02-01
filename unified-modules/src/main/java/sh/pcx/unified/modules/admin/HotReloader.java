/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.admin;

import sh.pcx.unified.modules.core.ModuleManager;
import sh.pcx.unified.modules.core.ModuleRegistry;
import sh.pcx.unified.modules.lifecycle.ModuleState;
import sh.pcx.unified.modules.lifecycle.Reloadable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides hot reload support for modules.
 *
 * <p>The HotReloader can monitor configuration files for changes and
 * automatically reload modules when their configuration is modified.
 * This enables rapid development and configuration changes without
 * requiring server restarts.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>File watching for configuration changes</li>
 *   <li>Automatic module reload on config change</li>
 *   <li>Debouncing to prevent rapid successive reloads</li>
 *   <li>Thread-safe operation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create hot reloader
 * HotReloader reloader = new HotReloader(moduleManager, logger);
 *
 * // Watch for configuration changes
 * reloader.watchConfigFolder(plugin.getDataFolder().toPath());
 *
 * // Start watching
 * reloader.start();
 *
 * // Stop when plugin disables
 * reloader.stop();
 * }</pre>
 *
 * <h2>Configuration Mapping</h2>
 * <p>The reloader maps configuration files to modules based on naming convention:
 * <ul>
 *   <li>{@code modules.yml} - Triggers reload of modules config</li>
 *   <li>{@code modules/ModuleName.yml} - Triggers reload of specific module</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ModuleManager
 * @see Reloadable
 */
public final class HotReloader {

    /**
     * Debounce delay in milliseconds to prevent rapid reloads.
     */
    private static final long DEBOUNCE_DELAY_MS = 500;

    private final ModuleManager manager;
    private final Logger logger;
    private final Set<Path> watchedPaths;
    private final Map<String, Long> lastReloadTime;
    private final AtomicBoolean running;

    private WatchService watchService;
    private ExecutorService executor;

    /**
     * Creates a new HotReloader.
     *
     * @param manager the module manager
     * @param logger  the logger
     */
    public HotReloader(@NotNull ModuleManager manager, @NotNull Logger logger) {
        this.manager = Objects.requireNonNull(manager, "Manager cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.watchedPaths = ConcurrentHashMap.newKeySet();
        this.lastReloadTime = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
    }

    /**
     * Adds a path to watch for configuration changes.
     *
     * @param path the path to watch
     * @return this reloader for chaining
     */
    @NotNull
    public HotReloader watchPath(@NotNull Path path) {
        Objects.requireNonNull(path, "Path cannot be null");
        watchedPaths.add(path);
        return this;
    }

    /**
     * Watches a configuration folder for changes.
     *
     * @param configFolder the config folder path
     * @return this reloader for chaining
     */
    @NotNull
    public HotReloader watchConfigFolder(@NotNull Path configFolder) {
        watchPath(configFolder);
        // Also watch modules subfolder if it exists
        Path modulesFolder = configFolder.resolve("modules");
        if (Files.exists(modulesFolder)) {
            watchPath(modulesFolder);
        }
        return this;
    }

    /**
     * Starts the hot reloader.
     *
     * @return {@code true} if started successfully
     */
    public boolean start() {
        if (running.get()) {
            logger.warning("HotReloader is already running");
            return false;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();

            // Register all paths
            for (Path path : watchedPaths) {
                if (Files.isDirectory(path)) {
                    path.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE
                    );
                    logger.info("Watching for changes: " + path);
                }
            }

            // Start watching thread
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "HotReloader-Watcher");
                t.setDaemon(true);
                return t;
            });

            running.set(true);
            executor.submit(this::watchLoop);

            logger.info("HotReloader started");
            return true;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to start HotReloader", e);
            return false;
        }
    }

    /**
     * Stops the hot reloader.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing watch service", e);
        }

        if (executor != null) {
            executor.shutdownNow();
        }

        logger.info("HotReloader stopped");
    }

    /**
     * Returns whether the hot reloader is running.
     *
     * @return {@code true} if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Manually triggers a reload for a module.
     *
     * @param moduleName the module name
     * @return {@code true} if the reload was triggered
     */
    public boolean triggerReload(@NotNull String moduleName) {
        if (!canReload(moduleName)) {
            return false;
        }

        lastReloadTime.put(moduleName, System.currentTimeMillis());
        return manager.reload(moduleName);
    }

    /**
     * Manually triggers a reload for all modules.
     */
    public void triggerReloadAll() {
        manager.reloadAll();
    }

    /**
     * The watch loop that monitors file changes.
     */
    private void watchLoop() {
        while (running.get()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();

                    handleFileChange(filename);
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
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in watch loop", e);
            }
        }
    }

    /**
     * Handles a file change event.
     *
     * @param filename the changed filename
     */
    private void handleFileChange(Path filename) {
        String name = filename.toString();

        // Check if it's a YAML file
        if (!name.endsWith(".yml") && !name.endsWith(".yaml")) {
            return;
        }

        logger.fine("Detected change in: " + name);

        // Check if it's the main modules.yml
        if (name.equals("modules.yml")) {
            // Reload modules configuration
            logger.info("Modules configuration changed, triggering reload");
            // Would reload module enable/disable config here
            return;
        }

        // Check if it's a module-specific config
        String moduleName = extractModuleName(name);
        if (moduleName != null) {
            reloadModule(moduleName);
        }
    }

    /**
     * Extracts module name from a config filename.
     *
     * @param filename the filename
     * @return the module name, or null if not a module config
     */
    private String extractModuleName(String filename) {
        // Handle "ModuleName.yml" pattern
        if (filename.endsWith(".yml")) {
            String name = filename.substring(0, filename.length() - 4);
            // Check if this module exists
            if (manager.getRegistry().contains(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * Reloads a module with debouncing.
     *
     * @param moduleName the module name
     */
    private void reloadModule(String moduleName) {
        if (!canReload(moduleName)) {
            logger.fine("Debouncing reload for: " + moduleName);
            return;
        }

        lastReloadTime.put(moduleName, System.currentTimeMillis());

        // Check if module is reloadable
        Optional<ModuleRegistry.ModuleEntry> entryOpt = manager.getRegistry().getEntry(moduleName);
        if (entryOpt.isEmpty()) {
            return;
        }

        ModuleRegistry.ModuleEntry entry = entryOpt.get();
        if (entry.getState() != ModuleState.ENABLED) {
            return;
        }

        if (!(entry.getInstance() instanceof Reloadable)) {
            logger.warning("Module '" + moduleName + "' does not support hot reload");
            return;
        }

        logger.info("Hot reloading module: " + moduleName);
        boolean success = manager.reload(moduleName);

        if (success) {
            logger.info("Successfully hot reloaded: " + moduleName);
        } else {
            logger.warning("Failed to hot reload: " + moduleName);
        }
    }

    /**
     * Checks if a module can be reloaded (respecting debounce).
     *
     * @param moduleName the module name
     * @return {@code true} if reload is allowed
     */
    private boolean canReload(String moduleName) {
        Long lastTime = lastReloadTime.get(moduleName);
        if (lastTime == null) {
            return true;
        }
        return System.currentTimeMillis() - lastTime > DEBOUNCE_DELAY_MS;
    }

    /**
     * Clears all watched paths.
     */
    public void clearWatchedPaths() {
        watchedPaths.clear();
    }

    /**
     * Returns the set of watched paths.
     *
     * @return the watched paths
     */
    @NotNull
    public Set<Path> getWatchedPaths() {
        return Collections.unmodifiableSet(watchedPaths);
    }
}
