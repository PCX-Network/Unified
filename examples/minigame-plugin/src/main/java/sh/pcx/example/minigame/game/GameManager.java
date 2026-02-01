/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.arena.Arena;
import sh.pcx.example.minigame.arena.ArenaManager;
import sh.pcx.example.minigame.arena.ArenaState;
import sh.pcx.example.minigame.config.MinigameConfig;
import sh.pcx.example.minigame.visual.GameScoreboard;
import sh.pcx.example.minigame.visual.CountdownBossBar;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active games in the minigame plugin.
 *
 * <p>The GameManager handles:
 * <ul>
 *   <li>Creating and destroying game instances</li>
 *   <li>Player join/leave operations</li>
 *   <li>Game lifecycle (countdown, start, end)</li>
 *   <li>Scoreboard and boss bar updates</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Start a game in an arena
 * Game game = gameManager.createGame(arena);
 *
 * // Add a player to a game
 * gameManager.joinGame(player, game);
 *
 * // Get player's current game
 * Optional<Game> game = gameManager.getPlayerGame(player);
 *
 * // End all games
 * gameManager.endAllGames();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Game
 * @see GameModule
 */
public class GameManager {

    private final MinigamePlugin plugin;
    private final MinigameConfig config;
    private final ArenaManager arenaManager;

    private final Map<UUID, Game> games;  // Game ID -> Game
    private final Map<UUID, Game> playerGames;  // Player ID -> Game
    private final Map<UUID, GameScoreboard> playerScoreboards;
    private final Map<UUID, CountdownBossBar> gameBossBars;

    // In production, these would be injected:
    // @Inject private SchedulerService scheduler;
    // @Inject private ScoreboardService scoreboards;
    // @Inject private BossBarService bossBars;
    // @Inject private PlayerDataService playerData;

    /**
     * Creates a new GameManager.
     *
     * @param plugin       the plugin instance
     * @param config       the plugin configuration
     * @param arenaManager the arena manager
     */
    public GameManager(@NotNull MinigamePlugin plugin, @NotNull MinigameConfig config,
                       @NotNull ArenaManager arenaManager) {
        this.plugin = plugin;
        this.config = config;
        this.arenaManager = arenaManager;
        this.games = new ConcurrentHashMap<>();
        this.playerGames = new ConcurrentHashMap<>();
        this.playerScoreboards = new ConcurrentHashMap<>();
        this.gameBossBars = new ConcurrentHashMap<>();
    }

    // ==================== Game Lifecycle ====================

    /**
     * Creates a new game in the specified arena.
     *
     * @param arena the arena for the game
     * @return the created game
     * @throws IllegalStateException if the arena is not available
     */
    @NotNull
    public Game createGame(@NotNull Arena arena) {
        if (!arena.isAvailable()) {
            throw new IllegalStateException("Arena '" + arena.getName() + "' is not available");
        }

        Game game = new Game(arena);
        game.setCountdownSeconds(config.getCountdownSeconds());
        game.setGameDurationSeconds(config.getGameDurationMinutes() * 60);

        games.put(game.getId(), game);

        plugin.getLogger().info("Created game " + game.getId() + " in arena " + arena.getName());
        return game;
    }

    /**
     * Gets or creates a game in the specified arena.
     *
     * @param arena the arena
     * @return the existing or new game
     */
    @NotNull
    public Game getOrCreateGame(@NotNull Arena arena) {
        // Check for existing game in arena
        for (Game game : games.values()) {
            if (game.getArena().equals(arena) && game.getPhase().allowsJoin()) {
                return game;
            }
        }
        return createGame(arena);
    }

    /**
     * Starts the countdown for a game.
     *
     * @param game the game to start countdown for
     */
    public void startCountdown(@NotNull Game game) {
        if (game.getPhase() != GamePhase.WAITING) {
            return;
        }

        if (!game.canStart()) {
            return;
        }

        game.startCountdown();

        // Create boss bar countdown
        if (config.isBossBarEnabled()) {
            CountdownBossBar bossBar = new CountdownBossBar(plugin, game);
            gameBossBars.put(game.getId(), bossBar);
            bossBar.start();
        }

        // Broadcast countdown start
        broadcastToGame(game, Component.text("Game starting in " + config.getCountdownSeconds() + " seconds!", NamedTextColor.YELLOW));

        // In production, schedule the game start:
        //
        // scheduler.runTaskLater(() -> {
        //     if (game.getPhase() == GamePhase.STARTING && game.canStart()) {
        //         startGame(game);
        //     } else {
        //         cancelCountdown(game);
        //     }
        // }, config.getCountdownSeconds() * 20L);

        plugin.getLogger().info("Started countdown for game " + game.getId());
    }

    /**
     * Starts a game after countdown completes.
     *
     * @param game the game to start
     */
    public void startGame(@NotNull Game game) {
        if (game.getPhase() != GamePhase.STARTING) {
            return;
        }

        game.start();

        // Remove countdown boss bar
        CountdownBossBar bossBar = gameBossBars.remove(game.getId());
        if (bossBar != null) {
            bossBar.stop();
        }

        // Teleport players to spawn points
        for (UUID playerId : game.getPlayerIds()) {
            // In production:
            // UnifiedPlayer player = UnifiedAPI.getPlayer(playerId).orElse(null);
            // if (player != null) {
            //     player.teleport(game.getSpawnLocation(playerId));
            // }
        }

        // Create game timer boss bar
        if (config.isBossBarEnabled()) {
            CountdownBossBar timerBar = new CountdownBossBar(plugin, game, true);
            gameBossBars.put(game.getId(), timerBar);
            timerBar.start();
        }

        // Update scoreboards
        updateScoreboards(game);

        broadcastToGame(game, Component.text("The game has started! Good luck!", NamedTextColor.GREEN));

        plugin.getLogger().info("Started game " + game.getId() + " with " + game.getPlayerCount() + " players");
    }

    /**
     * Ends a game.
     *
     * @param game   the game to end
     * @param winner the winner's UUID, or null for draw
     */
    public void endGame(@NotNull Game game, @Nullable UUID winner) {
        if (game.getPhase() == GamePhase.ENDING) {
            return;
        }

        game.end(winner);

        // Stop boss bar
        CountdownBossBar bossBar = gameBossBars.remove(game.getId());
        if (bossBar != null) {
            bossBar.stop();
        }

        // Award rewards and update stats
        for (UUID playerId : game.getPlayerIds()) {
            processPlayerGameEnd(game, playerId, winner);
        }

        // Announce winner
        if (winner != null) {
            GamePlayer winnerPlayer = game.getPlayer(winner).orElse(null);
            String winnerName = winnerPlayer != null ? winnerPlayer.getPlayerName() : "Unknown";
            broadcastToGame(game, Component.text(winnerName + " wins the game!", NamedTextColor.GOLD));
        } else {
            broadcastToGame(game, Component.text("The game ended in a draw!", NamedTextColor.YELLOW));
        }

        // Schedule cleanup
        // In production:
        // scheduler.runTaskLater(() -> cleanupGame(game), 10 * 20L);

        plugin.getLogger().info("Ended game " + game.getId() + ", winner: " + winner);
    }

    /**
     * Cleans up a finished game.
     *
     * @param game the game to clean up
     */
    public void cleanupGame(@NotNull Game game) {
        // Remove players from game tracking
        for (UUID playerId : game.getPlayerIds()) {
            playerGames.remove(playerId);

            // Remove scoreboard
            GameScoreboard scoreboard = playerScoreboards.remove(playerId);
            if (scoreboard != null) {
                scoreboard.destroy();
            }

            // Teleport to lobby
            // In production:
            // UnifiedPlayer player = UnifiedAPI.getPlayer(playerId).orElse(null);
            // if (player != null && game.getArena().getLobbySpawn() != null) {
            //     player.teleport(game.getArena().getLobbySpawn());
            // }
        }

        // Reset arena state
        game.getArena().setState(ArenaState.WAITING);

        // Remove game from tracking
        games.remove(game.getId());

        plugin.getLogger().info("Cleaned up game " + game.getId());
    }

    /**
     * Ends all active games.
     */
    public void endAllGames() {
        for (Game game : new ArrayList<>(games.values())) {
            if (game.isActive()) {
                endGame(game, null);
            }
            cleanupGame(game);
        }
        plugin.getLogger().info("Ended all games");
    }

    // ==================== Player Management ====================

    /**
     * Joins a player to a game.
     *
     * @param player the player to join
     * @param game   the game to join
     * @return true if the player joined successfully
     */
    public boolean joinGame(@NotNull UnifiedPlayer player, @NotNull Game game) {
        UUID playerId = player.getUniqueId();

        // Check if already in a game
        if (playerGames.containsKey(playerId)) {
            player.sendMessage(Component.text("You are already in a game!", NamedTextColor.RED));
            return false;
        }

        // Try to add player to game
        if (!game.addPlayer(player)) {
            player.sendMessage(Component.text("Unable to join the game.", NamedTextColor.RED));
            return false;
        }

        playerGames.put(playerId, game);

        // Teleport to arena lobby or spawn
        if (game.getArena().getLobbySpawn() != null) {
            player.teleport(game.getArena().getLobbySpawn());
        }

        // Create scoreboard
        if (config.isScoreboardEnabled()) {
            GameScoreboard scoreboard = new GameScoreboard(plugin, player, game);
            playerScoreboards.put(playerId, scoreboard);
            scoreboard.show();
        }

        // Notify other players
        broadcastToGame(game, Component.text(player.getName() + " joined the game! (" +
                game.getPlayerCount() + "/" + game.getArena().getMaxPlayers() + ")", NamedTextColor.GREEN));

        // Check if we can start countdown
        if (game.canStart() && game.getPhase() == GamePhase.WAITING) {
            startCountdown(game);
        }

        plugin.getLogger().info(player.getName() + " joined game " + game.getId());
        return true;
    }

    /**
     * Removes a player from their current game.
     *
     * @param player the player to remove
     * @param reason the reason for leaving
     */
    public void leaveGame(@NotNull UnifiedPlayer player, @NotNull String reason) {
        UUID playerId = player.getUniqueId();
        Game game = playerGames.remove(playerId);

        if (game == null) {
            return;
        }

        game.removePlayer(playerId);

        // Remove scoreboard
        GameScoreboard scoreboard = playerScoreboards.remove(playerId);
        if (scoreboard != null) {
            scoreboard.destroy();
        }

        // Teleport to lobby
        if (game.getArena().getLobbySpawn() != null) {
            player.teleport(game.getArena().getLobbySpawn());
        }

        // Notify other players
        broadcastToGame(game, Component.text(player.getName() + " left the game. (" + reason + ")", NamedTextColor.YELLOW));

        // Check if game should be cancelled
        if (game.getPhase() == GamePhase.STARTING && !game.canStart()) {
            cancelCountdown(game);
        }

        // Check if game should end (only one player left during game)
        if (game.getPhase() == GamePhase.RUNNING && game.getPlayerCount() <= 1) {
            Optional<UUID> lastPlayer = game.getPlayerIds().stream().findFirst();
            endGame(game, lastPlayer.orElse(null));
        }

        plugin.getLogger().info(player.getName() + " left game " + game.getId() + " (" + reason + ")");
    }

    /**
     * Cancels the countdown for a game.
     *
     * @param game the game
     */
    public void cancelCountdown(@NotNull Game game) {
        if (game.getPhase() != GamePhase.STARTING) {
            return;
        }

        // Reset to waiting
        game.getArena().setState(ArenaState.WAITING);

        // Stop boss bar
        CountdownBossBar bossBar = gameBossBars.remove(game.getId());
        if (bossBar != null) {
            bossBar.stop();
        }

        broadcastToGame(game, Component.text("Not enough players! Countdown cancelled.", NamedTextColor.RED));

        plugin.getLogger().info("Cancelled countdown for game " + game.getId());
    }

    // ==================== Scoreboard Updates ====================

    /**
     * Updates scoreboards for all players in a game.
     *
     * @param game the game
     */
    public void updateScoreboards(@NotNull Game game) {
        for (UUID playerId : game.getPlayerIds()) {
            GameScoreboard scoreboard = playerScoreboards.get(playerId);
            if (scoreboard != null) {
                scoreboard.update();
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Broadcasts a message to all players in a game.
     *
     * @param game    the game
     * @param message the message to broadcast
     */
    public void broadcastToGame(@NotNull Game game, @NotNull Component message) {
        for (UUID playerId : game.getPlayerIds()) {
            // In production:
            // UnifiedAPI.getPlayer(playerId).ifPresent(p -> p.sendMessage(message));
        }
    }

    /**
     * Processes end-of-game rewards and stats for a player.
     *
     * @param game     the game
     * @param playerId the player's UUID
     * @param winner   the winner's UUID
     */
    private void processPlayerGameEnd(@NotNull Game game, @NotNull UUID playerId, @Nullable UUID winner) {
        GamePlayer gamePlayer = game.getPlayer(playerId).orElse(null);
        if (gamePlayer == null) {
            return;
        }

        // In production, this would use PlayerDataService:
        //
        // PlayerProfile profile = playerData.getProfile(playerId);
        //
        // // Update stats
        // profile.incrementData(PlayerStatsKeys.GAMES_PLAYED, 1);
        // profile.incrementData(PlayerStatsKeys.KILLS, gamePlayer.getKills());
        // profile.incrementData(PlayerStatsKeys.DEATHS, gamePlayer.getDeaths());
        //
        // // Award coins
        // int reward = config.getParticipationReward();
        // reward += gamePlayer.getKills() * config.getKillReward();
        //
        // if (playerId.equals(winner)) {
        //     reward += config.getWinReward();
        //     profile.incrementData(PlayerStatsKeys.WINS, 1);
        // }
        //
        // profile.incrementData(PlayerStatsKeys.COINS, reward);
    }

    // ==================== Lookup Methods ====================

    /**
     * Gets a game by ID.
     *
     * @param id the game ID
     * @return an Optional containing the game
     */
    @NotNull
    public Optional<Game> getGame(@NotNull UUID id) {
        return Optional.ofNullable(games.get(id));
    }

    /**
     * Gets the game a player is in.
     *
     * @param player the player
     * @return an Optional containing the player's game
     */
    @NotNull
    public Optional<Game> getPlayerGame(@NotNull UnifiedPlayer player) {
        return Optional.ofNullable(playerGames.get(player.getUniqueId()));
    }

    /**
     * Gets the game a player is in by UUID.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the player's game
     */
    @NotNull
    public Optional<Game> getPlayerGame(@NotNull UUID playerId) {
        return Optional.ofNullable(playerGames.get(playerId));
    }

    /**
     * Checks if a player is in any game.
     *
     * @param player the player
     * @return true if the player is in a game
     */
    public boolean isInGame(@NotNull UnifiedPlayer player) {
        return playerGames.containsKey(player.getUniqueId());
    }

    /**
     * Returns all active games.
     *
     * @return an unmodifiable collection of games
     */
    @NotNull
    public Collection<Game> getActiveGames() {
        return Collections.unmodifiableCollection(games.values());
    }

    /**
     * Returns the number of active games.
     *
     * @return the game count
     */
    public int getActiveGameCount() {
        return games.size();
    }
}
