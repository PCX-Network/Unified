/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.game.Game;
import sh.pcx.unified.scheduler.TaskHandle;
import sh.pcx.unified.visual.bossbar.BossBarColor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A boss bar that displays countdown or game timer information.
 *
 * <p>This class demonstrates using the UnifiedPlugin BossBarService for
 * creating animated countdown displays.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class CountdownBossBar {

    private final MinigamePlugin plugin;
    private final Game game;
    private final boolean isGameTimer;

    private UUID bossBarId;
    private TaskHandle updateTask;
    private int totalSeconds;
    private int remainingSeconds;
    private boolean running;

    /**
     * Creates a countdown boss bar for the starting countdown.
     */
    public CountdownBossBar(@NotNull MinigamePlugin plugin, @NotNull Game game) {
        this(plugin, game, false);
    }

    /**
     * Creates a countdown boss bar.
     *
     * @param plugin      the plugin instance
     * @param game        the game
     * @param isGameTimer true for game timer, false for starting countdown
     */
    public CountdownBossBar(@NotNull MinigamePlugin plugin, @NotNull Game game, boolean isGameTimer) {
        this.plugin = plugin;
        this.game = game;
        this.isGameTimer = isGameTimer;
        this.running = false;

        if (isGameTimer) {
            this.totalSeconds = game.getGameDurationSeconds();
        } else {
            this.totalSeconds = game.getCountdownSeconds();
        }
        this.remainingSeconds = totalSeconds;
    }

    /**
     * Starts the countdown/timer.
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        remainingSeconds = totalSeconds;

        // In production, this would use BossBarService:
        // bossBar = bossBarService.create()
        //     .title(buildTitle())
        //     .color(getColor())
        //     .progress(1.0f)
        //     .addPlayers(game.getPlayerIds())
        //     .build();
        //
        // updateTask = schedulerService.runTaskTimer(this::tick, 20L, 20L);

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info("Started boss bar for game " + game.getId());
        }
    }

    /**
     * Stops the countdown/timer.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info("Stopped boss bar for game " + game.getId());
        }
    }

    private void tick() {
        if (!running) {
            return;
        }

        remainingSeconds--;

        if (remainingSeconds <= 0) {
            onComplete();
            stop();
            return;
        }

        if (remainingSeconds <= 5 || (remainingSeconds <= 10 && !isGameTimer)) {
            playTickSound();
        }
    }

    private Component buildTitle() {
        if (isGameTimer) {
            return Component.text("Time Remaining: ", NamedTextColor.WHITE)
                    .append(Component.text(formatTime(remainingSeconds), getTimeColor()));
        } else {
            return Component.text("Game starts in ", NamedTextColor.YELLOW)
                    .append(Component.text(remainingSeconds + "s", NamedTextColor.WHITE));
        }
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    private float getProgress() {
        if (totalSeconds <= 0) {
            return 0f;
        }
        return (float) remainingSeconds / totalSeconds;
    }

    private BossBarColor getColor() {
        float progress = getProgress();
        if (progress > 0.5f) {
            return BossBarColor.GREEN;
        } else if (progress > 0.25f) {
            return BossBarColor.YELLOW;
        } else {
            return BossBarColor.RED;
        }
    }

    private NamedTextColor getTimeColor() {
        float progress = getProgress();
        if (progress > 0.5f) {
            return NamedTextColor.GREEN;
        } else if (progress > 0.25f) {
            return NamedTextColor.YELLOW;
        } else {
            return NamedTextColor.RED;
        }
    }

    private void playTickSound() {
        // Would use player.playSound() for each player
    }

    private void onComplete() {
        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info((isGameTimer ? "Game timer" : "Countdown") +
                    " complete for game " + game.getId());
        }
    }

    public void addPlayer(@NotNull UUID playerId) {
        // In production: bossBar.addPlayer(player)
    }

    public void removePlayer(@NotNull UUID playerId) {
        // In production: bossBar.removePlayer(player)
    }

    public boolean isRunning() {
        return running;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    @NotNull
    public Game getGame() {
        return game;
    }
}
