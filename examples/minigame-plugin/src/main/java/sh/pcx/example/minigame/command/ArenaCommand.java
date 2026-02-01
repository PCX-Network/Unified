/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.arena.Arena;
import sh.pcx.example.minigame.arena.ArenaManager;
import sh.pcx.example.minigame.arena.ArenaState;
import sh.pcx.example.minigame.game.Game;
import sh.pcx.example.minigame.game.GameManager;
import sh.pcx.example.minigame.gui.ArenaSelectionGUI;
import sh.pcx.unified.commands.annotation.Arg;
import sh.pcx.unified.commands.annotation.Command;
import sh.pcx.unified.commands.annotation.Default;
import sh.pcx.unified.commands.annotation.Permission;
import sh.pcx.unified.commands.annotation.Sender;
import sh.pcx.unified.commands.annotation.Subcommand;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Command handler for arena management.
 *
 * <p>This class demonstrates the UnifiedPlugin command framework with:
 * <ul>
 *   <li>{@code @Command} annotation for the root command</li>
 *   <li>{@code @Subcommand} annotations for subcommands</li>
 *   <li>{@code @Permission} annotations for permission requirements</li>
 *   <li>{@code @Arg} annotations for command arguments</li>
 *   <li>{@code @Sender} annotation for the command sender</li>
 *   <li>Dependency injection with {@code @Inject}</li>
 * </ul>
 *
 * <h2>Available Commands</h2>
 * <ul>
 *   <li>{@code /arena} - Opens arena selection GUI</li>
 *   <li>{@code /arena create <name>} - Creates a new arena</li>
 *   <li>{@code /arena delete <name>} - Deletes an arena</li>
 *   <li>{@code /arena join [arena]} - Joins an arena</li>
 *   <li>{@code /arena leave} - Leaves the current game</li>
 *   <li>{@code /arena list} - Lists all arenas</li>
 *   <li>{@code /arena setspawn <arena>} - Sets a spawn point</li>
 *   <li>{@code /arena setlobby <arena>} - Sets the lobby spawn</li>
 *   <li>{@code /arena enable <arena>} - Enables an arena</li>
 *   <li>{@code /arena disable <arena>} - Disables an arena</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Command(
    name = "arena",
    aliases = {"mg", "minigame"},
    description = "Minigame arena commands",
    permission = "minigame.arena"
)
public class ArenaCommand {

    private final MinigamePlugin plugin;
    private final ArenaManager arenaManager;
    private final GameManager gameManager;

    /**
     * Creates a new ArenaCommand.
     *
     * <p>In production, dependencies would be automatically injected.
     *
     * @param plugin the plugin instance
     */
    @Inject
    public ArenaCommand(MinigamePlugin plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaModule().getArenaManager();
        this.gameManager = plugin.getGameModule().getGameManager();
    }

    /**
     * Default command handler - opens arena selection GUI.
     *
     * @param player the player executing the command
     */
    @Default
    public void openArenaGUI(@Sender UnifiedPlayer player) {
        ArenaSelectionGUI gui = new ArenaSelectionGUI(plugin, player, arenaManager, gameManager);
        gui.open();
    }

    /**
     * Creates a new arena at the player's location.
     *
     * @param player the player executing the command
     * @param name   the name for the new arena
     */
    @Subcommand("create")
    @Permission("minigame.admin.create")
    public void createArena(@Sender UnifiedPlayer player, @Arg("name") String name) {
        // Check if arena already exists
        if (arenaManager.getArena(name).isPresent()) {
            player.sendMessage(Component.text("Arena '" + name + "' already exists!", NamedTextColor.RED));
            return;
        }

        // Create the arena
        Arena arena = arenaManager.createArena(name, player.getWorld());

        // Set initial spawn point at player's location
        arena.addSpawnPoint(player.getLocation());
        arena.setLobbySpawn(player.getLocation());

        player.sendMessage(Component.text("Created arena '" + name + "'!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Use /arena setspawn " + name + " to add more spawn points.", NamedTextColor.GRAY));
    }

    /**
     * Deletes an arena.
     *
     * @param player the player executing the command
     * @param name   the name of the arena to delete
     */
    @Subcommand("delete")
    @Permission("minigame.admin.delete")
    public void deleteArena(@Sender UnifiedPlayer player, @Arg("name") String name) {
        Optional<Arena> arenaOpt = arenaManager.getArena(name);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        if (arena.isInGame()) {
            player.sendMessage(Component.text("Cannot delete arena while a game is in progress!", NamedTextColor.RED));
            return;
        }

        arenaManager.deleteArena(name);
        player.sendMessage(Component.text("Deleted arena '" + name + "'.", NamedTextColor.GREEN));
    }

    /**
     * Joins an arena (or a random available one).
     *
     * @param player    the player executing the command
     * @param arenaName optional arena name (null for random)
     */
    @Subcommand("join")
    public void joinArena(@Sender UnifiedPlayer player, @Arg(value = "arena", required = false) String arenaName) {
        // Check if already in game
        if (gameManager.isInGame(player)) {
            player.sendMessage(Component.text("You are already in a game! Use /arena leave first.", NamedTextColor.RED));
            return;
        }

        Arena arena;
        if (arenaName != null) {
            // Join specific arena
            Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
            if (arenaOpt.isEmpty()) {
                player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
                return;
            }
            arena = arenaOpt.get();
        } else {
            // Join random available arena
            Optional<Arena> arenaOpt = arenaManager.getRandomAvailableArena();
            if (arenaOpt.isEmpty()) {
                player.sendMessage(Component.text("No available arenas! Try again later.", NamedTextColor.RED));
                return;
            }
            arena = arenaOpt.get();
        }

        // Check if arena is available
        if (!arena.isAvailable()) {
            player.sendMessage(Component.text("Arena '" + arena.getName() + "' is not available!", NamedTextColor.RED));
            player.sendMessage(Component.text("Status: " + arena.getState().getColoredName(), NamedTextColor.GRAY));
            return;
        }

        // Get or create game and join
        Game game = gameManager.getOrCreateGame(arena);
        if (gameManager.joinGame(player, game)) {
            player.sendMessage(Component.text("Joined arena '" + arena.getDisplayName() + "'!", NamedTextColor.GREEN));
        }
    }

    /**
     * Leaves the current game.
     *
     * @param player the player executing the command
     */
    @Subcommand("leave")
    public void leaveArena(@Sender UnifiedPlayer player) {
        if (!gameManager.isInGame(player)) {
            player.sendMessage(Component.text("You are not in a game!", NamedTextColor.RED));
            return;
        }

        gameManager.leaveGame(player, "left voluntarily");
        player.sendMessage(Component.text("You left the game.", NamedTextColor.YELLOW));
    }

    /**
     * Lists all arenas.
     *
     * @param player the player executing the command
     */
    @Subcommand("list")
    public void listArenas(@Sender UnifiedPlayer player) {
        var arenas = arenaManager.getAllArenas();
        if (arenas.isEmpty()) {
            player.sendMessage(Component.text("No arenas exist yet.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("=== Arenas (" + arenas.size() + ") ===", NamedTextColor.GOLD));
        for (Arena arena : arenas) {
            String status = arena.getState().getColoredName();
            int spawnCount = arena.getSpawnPointCount();
            player.sendMessage(Component.text(" - " + arena.getDisplayName() + " [" + status + "] (" +
                    spawnCount + " spawns)", NamedTextColor.WHITE));
        }
    }

    /**
     * Adds a spawn point to an arena at the player's location.
     *
     * @param player    the player executing the command
     * @param arenaName the arena name
     */
    @Subcommand("setspawn")
    @Permission("minigame.admin.setup")
    public void setSpawnPoint(@Sender UnifiedPlayer player, @Arg("arena") String arenaName) {
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        arena.addSpawnPoint(player.getLocation());

        player.sendMessage(Component.text("Added spawn point #" + arena.getSpawnPointCount() +
                " to arena '" + arenaName + "'!", NamedTextColor.GREEN));
    }

    /**
     * Sets the lobby spawn for an arena.
     *
     * @param player    the player executing the command
     * @param arenaName the arena name
     */
    @Subcommand("setlobby")
    @Permission("minigame.admin.setup")
    public void setLobbySpawn(@Sender UnifiedPlayer player, @Arg("arena") String arenaName) {
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        arena.setLobbySpawn(player.getLocation());

        player.sendMessage(Component.text("Set lobby spawn for arena '" + arenaName + "'!", NamedTextColor.GREEN));
    }

    /**
     * Sets the spectator spawn for an arena.
     *
     * @param player    the player executing the command
     * @param arenaName the arena name
     */
    @Subcommand("setspectator")
    @Permission("minigame.admin.setup")
    public void setSpectatorSpawn(@Sender UnifiedPlayer player, @Arg("arena") String arenaName) {
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        arena.setSpectatorSpawn(player.getLocation());

        player.sendMessage(Component.text("Set spectator spawn for arena '" + arenaName + "'!", NamedTextColor.GREEN));
    }

    /**
     * Enables an arena.
     *
     * @param player    the player executing the command
     * @param arenaName the arena name
     */
    @Subcommand("enable")
    @Permission("minigame.admin.manage")
    public void enableArena(@Sender UnifiedPlayer player, @Arg("arena") String arenaName) {
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        if (!arena.isConfigured()) {
            player.sendMessage(Component.text("Arena is not fully configured!", NamedTextColor.RED));
            player.sendMessage(Component.text("Required: Region, spawn points, lobby spawn", NamedTextColor.GRAY));
            return;
        }

        arena.setEnabled(true);
        player.sendMessage(Component.text("Enabled arena '" + arenaName + "'!", NamedTextColor.GREEN));
    }

    /**
     * Disables an arena.
     *
     * @param player    the player executing the command
     * @param arenaName the arena name
     */
    @Subcommand("disable")
    @Permission("minigame.admin.manage")
    public void disableArena(@Sender UnifiedPlayer player, @Arg("arena") String arenaName) {
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        if (arena.isInGame()) {
            player.sendMessage(Component.text("Cannot disable arena while a game is in progress!", NamedTextColor.RED));
            return;
        }

        arena.setEnabled(false);
        player.sendMessage(Component.text("Disabled arena '" + arenaName + "'.", NamedTextColor.YELLOW));
    }

    /**
     * Shows info about an arena.
     *
     * @param player    the player executing the command
     * @param arenaName the arena name
     */
    @Subcommand("info")
    public void arenaInfo(@Sender UnifiedPlayer player, @Arg("arena") String arenaName) {
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        player.sendMessage(Component.text("=== Arena: " + arena.getDisplayName() + " ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Status: " + arena.getState().getColoredName(), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Enabled: " + (arena.isEnabled() ? "Yes" : "No"), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Players: " + arena.getMinPlayers() + "-" + arena.getMaxPlayers(), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Spawn Points: " + arena.getSpawnPointCount(), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Lobby Set: " + (arena.getLobbySpawn() != null ? "Yes" : "No"), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Region Set: " + (arena.getRegion() != null ? "Yes" : "No"), NamedTextColor.WHITE));
    }

    /**
     * Teleports to an arena.
     *
     * @param player    the player executing the command
     * @param arenaName the arena name
     */
    @Subcommand("tp")
    @Permission("minigame.admin.teleport")
    public void teleportToArena(@Sender UnifiedPlayer player, @Arg("arena") String arenaName) {
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            return;
        }

        Arena arena = arenaOpt.get();
        UnifiedLocation destination = arena.getLobbySpawn() != null ? arena.getLobbySpawn() : arena.getCenter();

        if (destination == null) {
            player.sendMessage(Component.text("Arena has no teleport location set!", NamedTextColor.RED));
            return;
        }

        player.teleport(destination);
        player.sendMessage(Component.text("Teleported to arena '" + arenaName + "'!", NamedTextColor.GREEN));
    }
}
