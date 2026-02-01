/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.game.Game;
import sh.pcx.example.minigame.game.GamePhase;
import sh.pcx.example.minigame.game.GamePlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-player scoreboard during a minigame.
 *
 * <p>This class demonstrates using the UnifiedPlugin ScoreboardService for
 * creating dynamic, per-player scoreboards. The scoreboard updates in real-time
 * to show:
 * <ul>
 *   <li>Game phase and timer</li>
 *   <li>Player's kills, deaths, and score</li>
 *   <li>Top players in the game</li>
 * </ul>
 *
 * <h2>Scoreboard Layout</h2>
 * <pre>
 * ===== MINIGAME =====
 *
 * Time: 5:42
 * Phase: In Progress
 *
 * Your Stats:
 * Kills: 5
 * Deaths: 2
 * Score: 470
 *
 * Top Players:
 * 1. Player1 - 8
 * 2. Player2 - 6
 * 3. Player3 - 5
 *
 * play.server.net
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create and show scoreboard
 * GameScoreboard scoreboard = new GameScoreboard(plugin, player, game);
 * scoreboard.show();
 *
 * // Update periodically
 * scoreboard.update();
 *
 * // Clean up when done
 * scoreboard.destroy();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class GameScoreboard {

    private final MinigamePlugin plugin;
    private final UnifiedPlayer player;
    private final Game game;
    private final UUID playerId;
    private boolean visible;

    // In production, this would hold the actual scoreboard instance:
    // private Scoreboard scoreboard;

    /**
     * Creates a new GameScoreboard.
     *
     * @param plugin the plugin instance
     * @param player the player to show the scoreboard to
     * @param game   the game to display stats for
     */
    public GameScoreboard(@NotNull MinigamePlugin plugin, @NotNull UnifiedPlayer player,
                          @NotNull Game game) {
        this.plugin = plugin;
        this.player = player;
        this.game = game;
        this.playerId = player.getUniqueId();
        this.visible = false;
    }

    /**
     * Creates and shows the scoreboard.
     *
     * <p>In production, this would use the ScoreboardService:
     * <pre>{@code
     * scoreboard = scoreboardService.create(player)
     *     .title(Component.text("MINIGAME", NamedTextColor.GOLD, TextDecoration.BOLD))
     *     .updateInterval(20)  // 1 second
     *     .build();
     * }</pre>
     */
    public void show() {
        if (visible) {
            return;
        }

        // In production:
        //
        // scoreboard = scoreboardService.create(player)
        //     .title(Component.text("MINIGAME", NamedTextColor.GOLD, TextDecoration.BOLD))
        //     .line(15, Component.empty())
        //     .line(14, () -> buildTimeLine())
        //     .line(13, () -> buildPhaseLine())
        //     .line(12, Component.empty())
        //     .line(11, Component.text("Your Stats:", NamedTextColor.YELLOW))
        //     .line(10, () -> buildKillsLine())
        //     .line(9, () -> buildDeathsLine())
        //     .line(8, () -> buildScoreLine())
        //     .line(7, Component.empty())
        //     .line(6, Component.text("Top Players:", NamedTextColor.YELLOW))
        //     .line(5, () -> buildLeaderboardLine(0))
        //     .line(4, () -> buildLeaderboardLine(1))
        //     .line(3, () -> buildLeaderboardLine(2))
        //     .line(2, Component.empty())
        //     .line(1, Component.text("play.server.net", NamedTextColor.GRAY))
        //     .updateInterval(20)
        //     .build();

        visible = true;
        update();

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info("Showing scoreboard for " + player.getName());
        }
    }

    /**
     * Updates the scoreboard content.
     */
    public void update() {
        if (!visible) {
            return;
        }

        // In production, the scoreboard would auto-update with the lambdas,
        // but we can also force an update:
        //
        // scoreboard.update();

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().fine("Updated scoreboard for " + player.getName());
        }
    }

    /**
     * Hides and destroys the scoreboard.
     */
    public void destroy() {
        if (!visible) {
            return;
        }

        // In production:
        // scoreboardService.remove(player);

        visible = false;

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info("Destroyed scoreboard for " + player.getName());
        }
    }

    /**
     * Hides the scoreboard without destroying it.
     */
    public void hide() {
        if (!visible) {
            return;
        }

        // In production:
        // scoreboardService.hide(player);

        visible = false;
    }

    // ==================== Line Builders ====================

    /**
     * Builds the time remaining line.
     *
     * @return the time component
     */
    private Component buildTimeLine() {
        int remaining = game.getRemainingSeconds();
        String timeStr;

        if (remaining < 0) {
            timeStr = "--:--";
        } else {
            int minutes = remaining / 60;
            int seconds = remaining % 60;
            timeStr = String.format("%d:%02d", minutes, seconds);
        }

        return Component.text("Time: ", NamedTextColor.WHITE)
                .append(Component.text(timeStr, NamedTextColor.GREEN));
    }

    /**
     * Builds the game phase line.
     *
     * @return the phase component
     */
    private Component buildPhaseLine() {
        GamePhase phase = game.getPhase();
        NamedTextColor color = switch (phase) {
            case WAITING -> NamedTextColor.YELLOW;
            case STARTING -> NamedTextColor.GOLD;
            case RUNNING -> NamedTextColor.GREEN;
            case ENDING -> NamedTextColor.RED;
        };

        return Component.text("Phase: ", NamedTextColor.WHITE)
                .append(Component.text(phase.getDisplayName(), color));
    }

    /**
     * Builds the kills line.
     *
     * @return the kills component
     */
    private Component buildKillsLine() {
        int kills = game.getKills(playerId);
        return Component.text("Kills: ", NamedTextColor.WHITE)
                .append(Component.text(kills, NamedTextColor.GREEN));
    }

    /**
     * Builds the deaths line.
     *
     * @return the deaths component
     */
    private Component buildDeathsLine() {
        int deaths = game.getDeaths(playerId);
        return Component.text("Deaths: ", NamedTextColor.WHITE)
                .append(Component.text(deaths, NamedTextColor.RED));
    }

    /**
     * Builds the score line.
     *
     * @return the score component
     */
    private Component buildScoreLine() {
        Optional<GamePlayer> gamePlayer = game.getPlayer(playerId);
        int score = gamePlayer.map(GamePlayer::getScore).orElse(0);

        return Component.text("Score: ", NamedTextColor.WHITE)
                .append(Component.text(score, NamedTextColor.GOLD));
    }

    /**
     * Builds a leaderboard line.
     *
     * @param rank the rank (0-indexed)
     * @return the leaderboard entry component
     */
    private Component buildLeaderboardLine(int rank) {
        List<UUID> leaderboard = game.getLeaderboard();

        if (rank >= leaderboard.size()) {
            return Component.empty();
        }

        UUID leaderId = leaderboard.get(rank);
        Optional<GamePlayer> leaderPlayer = game.getPlayer(leaderId);

        if (leaderPlayer.isEmpty()) {
            return Component.empty();
        }

        GamePlayer leader = leaderPlayer.get();
        NamedTextColor color = rank == 0 ? NamedTextColor.GOLD :
                rank == 1 ? NamedTextColor.WHITE :
                        NamedTextColor.GRAY;

        // Highlight if this is the current player
        if (leaderId.equals(playerId)) {
            color = NamedTextColor.AQUA;
        }

        return Component.text((rank + 1) + ". ", NamedTextColor.GRAY)
                .append(Component.text(leader.getPlayerName(), color))
                .append(Component.text(" - " + leader.getKills(), NamedTextColor.WHITE));
    }

    // ==================== Getters ====================

    /**
     * Checks if the scoreboard is visible.
     *
     * @return true if visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Returns the player this scoreboard is for.
     *
     * @return the player
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the game this scoreboard is displaying.
     *
     * @return the game
     */
    @NotNull
    public Game getGame() {
        return game;
    }
}
