/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.game.Game;
import sh.pcx.example.minigame.game.GameManager;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles player connection events for the minigame.
 *
 * <p>This listener demonstrates handling player join and quit events
 * to properly manage game state when players connect or disconnect.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Welcome messages for returning players</li>
 *   <li>Player data preloading on join</li>
 *   <li>Removing players from games on quit</li>
 *   <li>Updating scoreboards on reconnect</li>
 * </ul>
 *
 * <h2>Event Handling</h2>
 * <p>In production, this would use the UnifiedPlugin event system:
 * <pre>{@code
 * @Listen
 * public void onPlayerJoin(UnifiedPlayerJoinEvent event) {
 *     // ...
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class PlayerConnectionListener {

    private final MinigamePlugin plugin;
    private final GameManager gameManager;

    /**
     * Creates a new PlayerConnectionListener.
     *
     * @param plugin      the plugin instance
     * @param gameManager the game manager
     */
    public PlayerConnectionListener(@NotNull MinigamePlugin plugin, @NotNull GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        // In production, the listener would be auto-registered via @Listen annotation
        // or manually registered with the event service:
        //
        // eventService.register(this);

        plugin.getLogger().info("PlayerConnectionListener registered.");
    }

    /**
     * Called when a player joins the server.
     *
     * <p>In production, this would be annotated with {@code @Listen}:
     * <pre>{@code
     * @Listen
     * public void onPlayerJoin(UnifiedPlayerJoinEvent event) {
     *     UnifiedPlayer player = event.getPlayer();
     *     handleJoin(player);
     * }
     * }</pre>
     *
     * @param player the player who joined
     */
    public void handleJoin(@NotNull UnifiedPlayer player) {
        UUID playerId = player.getUniqueId();

        // Preload player data asynchronously
        // In production:
        // playerDataService.preload(playerId);

        // Check if player was in a game (reconnection)
        gameManager.getPlayerGame(playerId).ifPresent(game -> {
            // Player reconnected during a game
            handleReconnect(player, game);
        });

        // Send welcome message if enabled
        if (plugin.getPluginConfig().isDebug()) {
            player.sendMessage(Component.text("Welcome to the minigame server!", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Use /arena to join a game.", NamedTextColor.GRAY));
        }

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " joined.");
        }
    }

    /**
     * Called when a player quits the server.
     *
     * <p>In production, this would be annotated with {@code @Listen}:
     * <pre>{@code
     * @Listen
     * public void onPlayerQuit(UnifiedPlayerQuitEvent event) {
     *     UnifiedPlayer player = event.getPlayer();
     *     handleQuit(player);
     * }
     * }</pre>
     *
     * @param player the player who quit
     */
    public void handleQuit(@NotNull UnifiedPlayer player) {
        // Remove from any active game
        if (gameManager.isInGame(player)) {
            gameManager.leaveGame(player, "disconnected");
        }

        // Save player data
        // In production:
        // playerDataService.save(player.getUniqueId());

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " quit.");
        }
    }

    /**
     * Handles a player reconnecting during a game.
     *
     * @param player the reconnected player
     * @param game   the game they were in
     */
    private void handleReconnect(@NotNull UnifiedPlayer player, @NotNull Game game) {
        player.sendMessage(Component.text("Reconnected to your game!", NamedTextColor.GREEN));

        // Restore scoreboard
        // In production:
        // GameScoreboard scoreboard = new GameScoreboard(plugin, player, game);
        // scoreboard.show();

        // Teleport to spawn if still in waiting/starting
        if (game.getPhase().allowsJoin()) {
            var arena = game.getArena();
            if (arena.getLobbySpawn() != null) {
                player.teleport(arena.getLobbySpawn());
            }
        } else {
            // Teleport to spectator spawn if game already started
            var arena = game.getArena();
            if (arena.getSpectatorSpawn() != null) {
                player.teleport(arena.getSpectatorSpawn());
                game.addSpectator(player.getUniqueId());
                player.sendMessage(Component.text("You are now spectating.", NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Called when a player is kicked from the server.
     *
     * @param player the player who was kicked
     * @param reason the kick reason
     */
    public void handleKick(@NotNull UnifiedPlayer player, @NotNull String reason) {
        // Same handling as quit
        handleQuit(player);
    }
}
