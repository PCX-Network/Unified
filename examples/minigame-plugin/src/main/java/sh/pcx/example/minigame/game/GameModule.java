/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.game;

import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.arena.ArenaManager;
import sh.pcx.example.minigame.config.MinigameConfig;
import sh.pcx.unified.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

/**
 * Feature module for game management functionality.
 *
 * <p>This module encapsulates all game-related functionality including:
 * <ul>
 *   <li>Game creation and lifecycle management</li>
 *   <li>Player join/leave handling</li>
 *   <li>Game timer and win condition checking</li>
 *   <li>Score tracking and rewards</li>
 * </ul>
 *
 * <h2>Module Pattern</h2>
 * <p>In a full UnifiedPlugin application, this would extend {@code FeatureModule}
 * and be automatically discovered and loaded. The module pattern provides:
 * <ul>
 *   <li>Clear separation of concerns</li>
 *   <li>Enable/disable lifecycle hooks</li>
 *   <li>Configuration-driven feature toggling</li>
 *   <li>Dependency injection support</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see GameManager
 * @see Game
 */
public class GameModule {

    private final MinigamePlugin plugin;
    private final MinigameConfig config;
    private final ArenaManager arenaManager;
    private GameManager gameManager;
    private TaskHandle gameTickTask;
    private boolean enabled;

    /**
     * Creates a new GameModule.
     *
     * @param plugin       the plugin instance
     * @param config       the plugin configuration
     * @param arenaManager the arena manager
     */
    public GameModule(@NotNull MinigamePlugin plugin, @NotNull MinigameConfig config,
                      @NotNull ArenaManager arenaManager) {
        this.plugin = plugin;
        this.config = config;
        this.arenaManager = arenaManager;
        this.enabled = false;
    }

    /**
     * Enables the game module.
     *
     * <p>This initializes the game manager and starts the game tick task.
     */
    public void enable() {
        if (enabled) {
            return;
        }

        plugin.getLogger().info("Enabling GameModule...");

        // Initialize the game manager
        this.gameManager = new GameManager(plugin, config, arenaManager);

        // Start game tick task (updates game state)
        startGameTickTask();

        enabled = true;
        plugin.getLogger().info("GameModule enabled.");
    }

    /**
     * Disables the game module.
     *
     * <p>This ends all active games and cleans up resources.
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        plugin.getLogger().info("Disabling GameModule...");

        // Stop game tick task
        if (gameTickTask != null) {
            gameTickTask.cancel();
            gameTickTask = null;
        }

        // End all active games
        if (gameManager != null) {
            gameManager.endAllGames();
        }

        enabled = false;
        plugin.getLogger().info("GameModule disabled.");
    }

    /**
     * Starts the game tick task.
     *
     * <p>This task runs every second and:
     * <ul>
     *   <li>Checks for games that need to start (enough players)</li>
     *   <li>Updates game timers</li>
     *   <li>Checks win conditions</li>
     *   <li>Updates scoreboards</li>
     * </ul>
     *
     * <p>In production, this would use the SchedulerService with Folia support.
     */
    private void startGameTickTask() {
        // In production:
        //
        // gameTickTask = schedulerService.runTaskTimer(() -> {
        //     for (Game game : gameManager.getActiveGames()) {
        //         tickGame(game);
        //     }
        // }, 20L, 20L); // Every second

        plugin.getLogger().info("Game tick task started.");
    }

    /**
     * Processes a single game tick.
     *
     * @param game the game to tick
     */
    private void tickGame(@NotNull Game game) {
        switch (game.getPhase()) {
            case WAITING -> {
                // Check if we have enough players to start countdown
                if (game.canStart()) {
                    gameManager.startCountdown(game);
                }
            }
            case STARTING -> {
                // Countdown is handled by boss bar
                // Check if we still have enough players
                if (!game.canStart()) {
                    gameManager.cancelCountdown(game);
                }
            }
            case RUNNING -> {
                // Check if time is up
                if (game.getRemainingSeconds() <= 0) {
                    // End with leader as winner (or null for draw)
                    gameManager.endGame(game, game.getLeader().orElse(null));
                }

                // Check for solo winner
                if (game.getPlayerCount() == 1) {
                    gameManager.endGame(game, game.getPlayerIds().iterator().next());
                }

                // Update scoreboards periodically
                if (System.currentTimeMillis() % 1000 < 50) {
                    gameManager.updateScoreboards(game);
                }
            }
            case ENDING -> {
                // Cleanup is handled after delay
            }
        }
    }

    // ==================== Getters ====================

    /**
     * Returns the game manager.
     *
     * @return the game manager
     * @throws IllegalStateException if the module is not enabled
     */
    @NotNull
    public GameManager getGameManager() {
        if (!enabled) {
            throw new IllegalStateException("GameModule is not enabled");
        }
        return gameManager;
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
        return "GameModule";
    }
}
