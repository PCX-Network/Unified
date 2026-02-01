/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame;

import com.google.inject.Injector;
import sh.pcx.example.minigame.arena.ArenaModule;
import sh.pcx.example.minigame.config.MinigameConfig;
import sh.pcx.example.minigame.data.PlayerStats;
import sh.pcx.example.minigame.data.PlayerStatsKeys;
import sh.pcx.example.minigame.game.GameModule;
import sh.pcx.example.minigame.listener.PlayerConnectionListener;
import sh.pcx.example.minigame.listener.RegionListener;
import sh.pcx.example.minigame.visual.LeaderboardManager;
import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.inject.InjectorFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main plugin class demonstrating a complete minigame plugin using the UnifiedPlugin API.
 *
 * <p>This example showcases the following features:
 * <ul>
 *   <li>Modular architecture with {@link ArenaModule} and {@link GameModule}</li>
 *   <li>Region-based arena detection using {@code RegionService}</li>
 *   <li>Scheduler tasks with Folia support via {@code SchedulerService}</li>
 *   <li>Per-player scoreboards via {@code ScoreboardService}</li>
 *   <li>Boss bar countdowns via {@code BossBarService}</li>
 *   <li>Hologram leaderboards via {@code HologramService}</li>
 *   <li>Player data and statistics via {@code PlayerDataService}</li>
 *   <li>GUI menus for arena selection via the GUI framework</li>
 *   <li>Annotation-based commands via the command framework</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class MinigamePlugin extends UnifiedPlugin {

    private Injector injector;
    private MinigameConfig config;
    private ArenaModule arenaModule;
    private GameModule gameModule;
    private LeaderboardManager leaderboardManager;

    /**
     * Called when the plugin JAR is loaded (before enabling).
     */
    @Override
    public void onLoad() {
        getLogger().info("MinigamePlugin loading...");

        // Ensure data folder exists
        try {
            Files.createDirectories(getDataFolder());
        } catch (IOException e) {
            getLogger().severe("Failed to create data folder: " + e.getMessage());
        }

        // Save default resources
        saveDefaultResource("config.yml");
        saveDefaultResource("arenas.yml");
        saveDefaultResource("messages.yml");
    }

    /**
     * Called when the plugin is enabled.
     */
    @Override
    public void onEnable() {
        getLogger().info("MinigamePlugin enabling...");
        long startTime = System.currentTimeMillis();

        // Load configuration
        loadConfiguration();

        // Initialize dependency injection
        initializeInjector();

        // Initialize modules
        initializeModules();

        // Initialize leaderboards
        initializeLeaderboards();

        // Log startup completion
        long duration = System.currentTimeMillis() - startTime;
        getLogger().info("MinigamePlugin v" + getVersion() + " enabled in " + duration + "ms!");
        getLogger().info("Loaded " + arenaModule.getArenaManager().getArenaCount() + " arenas.");
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("MinigamePlugin disabling...");

        // End all active games
        if (gameModule != null) {
            gameModule.getGameManager().endAllGames();
        }

        // Save arena data
        if (arenaModule != null) {
            arenaModule.getArenaManager().saveAll();
        }

        // Clean up leaderboards
        if (leaderboardManager != null) {
            leaderboardManager.shutdown();
        }

        // Clean up resources
        this.injector = null;
        this.config = null;
        this.arenaModule = null;
        this.gameModule = null;
        this.leaderboardManager = null;

        getLogger().info("MinigamePlugin disabled.");
    }

    /**
     * Called when an admin requests a reload.
     */
    @Override
    public void onReload() {
        getLogger().info("Reloading MinigamePlugin configuration...");

        // Reload configuration
        loadConfiguration();

        // Reload arenas
        if (arenaModule != null) {
            arenaModule.getArenaManager().reload();
        }

        // Reload leaderboards
        if (leaderboardManager != null) {
            leaderboardManager.reload();
        }

        getLogger().info("MinigamePlugin reloaded successfully!");
    }

    /**
     * Loads the plugin configuration.
     */
    private void loadConfiguration() {
        // In production, use ConfigService from UnifiedPluginAPI:
        // ConfigService configService = getServices().get(ConfigService.class);
        // config = configService.load(MinigameConfig.class, getDataFolder().resolve("config.yml"));
        this.config = new MinigameConfig();
        getLogger().info("Configuration loaded - Debug: " + config.isDebug());
    }

    /**
     * Initializes the dependency injection framework.
     */
    private void initializeInjector() {
        // In production with UnifiedPluginAPI:
        // injector = InjectorFactory.create(this, new MinigameModule(this));
        //
        // For this example, we'll manually create instances
        getLogger().info("Dependency injection initialized.");
    }

    /**
     * Initializes the arena and game modules.
     */
    private void initializeModules() {
        // Initialize arena module
        this.arenaModule = new ArenaModule(this, config);
        arenaModule.enable();

        // Initialize game module
        this.gameModule = new GameModule(this, config, arenaModule.getArenaManager());
        gameModule.enable();

        // Register listeners
        // In production, listeners would be auto-registered via @Listen annotation
        new PlayerConnectionListener(this, gameModule.getGameManager());
        new RegionListener(this, arenaModule.getArenaManager(), gameModule.getGameManager());

        getLogger().info("Modules initialized.");
    }

    /**
     * Initializes hologram leaderboards.
     */
    private void initializeLeaderboards() {
        if (config.isLeaderboardsEnabled()) {
            this.leaderboardManager = new LeaderboardManager(this, config);
            leaderboardManager.initialize();
            getLogger().info("Leaderboard holograms initialized.");
        }
    }

    // ==================== Getters ====================

    /**
     * Returns the plugin configuration.
     *
     * @return the plugin configuration
     */
    @NotNull
    public MinigameConfig getPluginConfig() {
        return config;
    }

    /**
     * Returns the arena module.
     *
     * @return the arena module
     */
    @NotNull
    public ArenaModule getArenaModule() {
        return arenaModule;
    }

    /**
     * Returns the game module.
     *
     * @return the game module
     */
    @NotNull
    public GameModule getGameModule() {
        return gameModule;
    }

    /**
     * Returns the leaderboard manager.
     *
     * @return the leaderboard manager, or null if disabled
     */
    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {
        Path targetPath = getDataFolder().resolve(resourcePath);

        if (Files.exists(targetPath) && !replace) {
            return;
        }

        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                getLogger().warning("Resource not found: " + resourcePath);
                return;
            }

            Files.createDirectories(targetPath.getParent());
            Files.copy(in, targetPath);
            getLogger().info("Saved resource: " + resourcePath);
        } catch (IOException e) {
            getLogger().severe("Could not save resource " + resourcePath + ": " + e.getMessage());
        }
    }
}
