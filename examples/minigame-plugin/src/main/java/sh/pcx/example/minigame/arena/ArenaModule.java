/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.arena;

import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.config.MinigameConfig;
import sh.pcx.unified.inject.FeatureModule;
import sh.pcx.unified.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

/**
 * Feature module for arena management functionality.
 *
 * <p>This module encapsulates all arena-related functionality including:
 * <ul>
 *   <li>Arena creation, deletion, and configuration</li>
 *   <li>Arena region setup using the RegionService</li>
 *   <li>Arena data persistence</li>
 *   <li>Periodic auto-save functionality</li>
 * </ul>
 *
 * <h2>Module Structure</h2>
 * <p>In a full UnifiedPlugin application, this would extend {@code FeatureModule}
 * and be automatically discovered and loaded. This example shows the module
 * pattern with manual lifecycle management.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ArenaModule arenaModule = new ArenaModule(plugin, config);
 * arenaModule.enable();
 *
 * // Access the arena manager
 * ArenaManager manager = arenaModule.getArenaManager();
 *
 * // On shutdown
 * arenaModule.disable();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArenaManager
 * @see Arena
 */
public class ArenaModule {

    private final MinigamePlugin plugin;
    private final MinigameConfig config;
    private ArenaManager arenaManager;
    private TaskHandle autoSaveTask;
    private boolean enabled;

    /**
     * Creates a new ArenaModule.
     *
     * @param plugin the plugin instance
     * @param config the plugin configuration
     */
    public ArenaModule(@NotNull MinigamePlugin plugin, @NotNull MinigameConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.enabled = false;
    }

    /**
     * Enables the arena module.
     *
     * <p>This initializes the arena manager, loads arena data from storage,
     * and starts the auto-save task if enabled.
     */
    public void enable() {
        if (enabled) {
            return;
        }

        plugin.getLogger().info("Enabling ArenaModule...");

        // Initialize the arena manager
        this.arenaManager = new ArenaManager(plugin, config);

        // Load arenas from storage
        arenaManager.loadAll();

        // Start auto-save task if enabled
        if (config.isAutoSaveArenas()) {
            startAutoSaveTask();
        }

        enabled = true;
        plugin.getLogger().info("ArenaModule enabled with " + arenaManager.getArenaCount() + " arenas.");
    }

    /**
     * Disables the arena module.
     *
     * <p>This saves all arena data and stops the auto-save task.
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        plugin.getLogger().info("Disabling ArenaModule...");

        // Stop auto-save task
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        // Save all arenas
        if (arenaManager != null) {
            arenaManager.saveAll();
        }

        enabled = false;
        plugin.getLogger().info("ArenaModule disabled.");
    }

    /**
     * Reloads the arena module.
     *
     * <p>This reloads all arena data from storage.
     */
    public void reload() {
        plugin.getLogger().info("Reloading ArenaModule...");

        if (arenaManager != null) {
            arenaManager.reload();
        }

        plugin.getLogger().info("ArenaModule reloaded.");
    }

    /**
     * Starts the auto-save task.
     *
     * <p>This demonstrates using the UnifiedPlugin SchedulerService for
     * periodic tasks with Folia support.
     */
    private void startAutoSaveTask() {
        int intervalMinutes = config.getSaveIntervalMinutes();
        long intervalTicks = intervalMinutes * 60 * 20L; // minutes to ticks

        // In production, this would use the SchedulerService:
        //
        // autoSaveTask = schedulerService.runTaskTimer(() -> {
        //     if (config.isDebug()) {
        //         plugin.getLogger().info("Auto-saving arenas...");
        //     }
        //     arenaManager.saveAll();
        // }, intervalTicks, intervalTicks);

        plugin.getLogger().info("Auto-save task started (every " + intervalMinutes + " minutes).");
    }

    // ==================== Getters ====================

    /**
     * Returns the arena manager.
     *
     * @return the arena manager
     * @throws IllegalStateException if the module is not enabled
     */
    @NotNull
    public ArenaManager getArenaManager() {
        if (!enabled) {
            throw new IllegalStateException("ArenaModule is not enabled");
        }
        return arenaManager;
    }

    /**
     * Checks if the module is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the module name.
     *
     * @return the module name
     */
    @NotNull
    public String getName() {
        return "ArenaModule";
    }
}
