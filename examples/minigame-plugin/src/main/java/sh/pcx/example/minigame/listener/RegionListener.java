/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.arena.Arena;
import sh.pcx.example.minigame.arena.ArenaManager;
import sh.pcx.example.minigame.game.Game;
import sh.pcx.example.minigame.game.GameManager;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.region.Region;
import sh.pcx.unified.region.event.RegionEnterEvent;
import sh.pcx.unified.region.event.RegionExitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handles region enter/exit events for arena detection.
 *
 * <p>This listener demonstrates using the UnifiedPlugin RegionService for
 * detecting when players enter or exit arena boundaries. This enables:
 * <ul>
 *   <li>Automatic arena boundary enforcement</li>
 *   <li>Entry/exit notifications</li>
 *   <li>Prevention of leaving during games</li>
 * </ul>
 *
 * <h2>Region Events</h2>
 * <p>The RegionService fires events when players cross region boundaries:
 * <ul>
 *   <li>{@link RegionEnterEvent} - Player entered a region</li>
 *   <li>{@link RegionExitEvent} - Player left a region</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Listen
 * public void onRegionEnter(RegionEnterEvent event) {
 *     Region region = event.getRegion();
 *     UnifiedPlayer player = event.getPlayer();
 *     // Handle entry
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class RegionListener {

    private final MinigamePlugin plugin;
    private final ArenaManager arenaManager;
    private final GameManager gameManager;

    /**
     * Creates a new RegionListener.
     *
     * @param plugin       the plugin instance
     * @param arenaManager the arena manager
     * @param gameManager  the game manager
     */
    public RegionListener(@NotNull MinigamePlugin plugin,
                          @NotNull ArenaManager arenaManager,
                          @NotNull GameManager gameManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;

        // In production, the listener would be auto-registered via @Listen annotation
        plugin.getLogger().info("RegionListener registered.");
    }

    /**
     * Called when a player enters a region.
     *
     * <p>In production, this would be annotated with {@code @Listen}:
     * <pre>{@code
     * @Listen
     * public void onRegionEnter(RegionEnterEvent event) {
     *     Region region = event.getRegion();
     *     UnifiedPlayer player = event.getPlayer();
     *     handleRegionEnter(player, region);
     * }
     * }</pre>
     *
     * @param player the player who entered
     * @param region the region that was entered
     */
    public void handleRegionEnter(@NotNull UnifiedPlayer player, @NotNull Region region) {
        // Check if this is an arena region
        String regionName = region.getName();
        if (!regionName.startsWith("arena-")) {
            return;
        }

        // Extract arena name from region name
        String arenaName = regionName.substring("arena-".length());
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);

        if (arenaOpt.isEmpty()) {
            return;
        }

        Arena arena = arenaOpt.get();

        // Send entry message if not already in game
        if (!gameManager.isInGame(player)) {
            player.sendMessage(Component.text("Entering arena: ", NamedTextColor.GRAY)
                    .append(Component.text(arena.getDisplayName(), NamedTextColor.GOLD)));

            if (arena.isAvailable()) {
                player.sendMessage(Component.text("Use /arena join " + arena.getName() + " to play!", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("This arena is currently " + arena.getState().getDisplayName().toLowerCase() + ".", NamedTextColor.YELLOW));
            }
        }

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info(player.getName() + " entered arena region: " + arena.getName());
        }
    }

    /**
     * Called when a player exits a region.
     *
     * <p>In production, this would be annotated with {@code @Listen}:
     * <pre>{@code
     * @Listen
     * public void onRegionExit(RegionExitEvent event) {
     *     Region region = event.getRegion();
     *     UnifiedPlayer player = event.getPlayer();
     *     handleRegionExit(player, region);
     * }
     * }</pre>
     *
     * @param player the player who exited
     * @param region the region that was exited
     */
    public void handleRegionExit(@NotNull UnifiedPlayer player, @NotNull Region region) {
        // Check if this is an arena region
        String regionName = region.getName();
        if (!regionName.startsWith("arena-")) {
            return;
        }

        // Extract arena name from region name
        String arenaName = regionName.substring("arena-".length());
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);

        if (arenaOpt.isEmpty()) {
            return;
        }

        Arena arena = arenaOpt.get();

        // Check if player is in a game in this arena
        Optional<Game> gameOpt = gameManager.getPlayerGame(player);
        if (gameOpt.isPresent() && gameOpt.get().getArena().equals(arena)) {
            Game game = gameOpt.get();

            // Prevent leaving during active game
            if (game.isActive()) {
                // Teleport back to arena
                var spawnPoint = arena.getCenter();
                if (spawnPoint != null) {
                    player.teleport(spawnPoint);
                }
                player.sendMessage(Component.text("You cannot leave the arena during a game!", NamedTextColor.RED));
                return;
            }
        }

        // Send exit message if not in game
        if (!gameManager.isInGame(player)) {
            player.sendMessage(Component.text("Leaving arena: ", NamedTextColor.GRAY)
                    .append(Component.text(arena.getDisplayName(), NamedTextColor.GOLD)));
        }

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info(player.getName() + " exited arena region: " + arena.getName());
        }
    }

    /**
     * Called when a player moves within a region.
     *
     * <p>This can be used for boundary enforcement or position tracking.
     *
     * @param player the player who moved
     * @param region the region the player is in
     */
    public void handleRegionMove(@NotNull UnifiedPlayer player, @NotNull Region region) {
        // This could be used for more granular position tracking
        // For performance, this should only be called periodically, not every move event
    }

    /**
     * Checks if a player is currently within an arena's region.
     *
     * @param player the player to check
     * @return the arena the player is in, or empty if not in an arena
     */
    @NotNull
    public Optional<Arena> getPlayerArena(@NotNull UnifiedPlayer player) {
        return arenaManager.getArenaContaining(player);
    }
}
