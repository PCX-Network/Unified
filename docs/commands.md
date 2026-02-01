# Command Framework Guide

The UnifiedPlugin command framework provides an annotation-based approach to creating commands with automatic argument parsing, tab completion, permissions, and cooldowns.

## Table of Contents

- [Overview](#overview)
- [Basic Commands](#basic-commands)
- [Subcommands](#subcommands)
- [Arguments](#arguments)
- [Tab Completion](#tab-completion)
- [Permissions](#permissions)
- [Cooldowns](#cooldowns)
- [Async Commands](#async-commands)
- [Custom Parsers](#custom-parsers)
- [Command Service](#command-service)

---

## Overview

### Key Features

- **Annotation-based** command definitions
- **Automatic argument parsing** for common types
- **Tab completion** with built-in and custom providers
- **Permission checks** at command and subcommand level
- **Cooldown management** to prevent spam
- **Async execution** for long-running commands
- **Dependency injection** in command classes

### Getting CommandService

```java
import sh.pcx.unified.commands.core.CommandService;
import javax.inject.Inject;

public class MyPlugin extends UnifiedPlugin {

    @Inject
    private CommandService commands;

    @Override
    public void onEnable() {
        // Register commands
        commands.register(new SpawnCommand());
        commands.register(new HomeCommand());
    }

    @Override
    public void onDisable() {
        commands.unregisterAll();
    }
}
```

---

## Basic Commands

### Simple Command

```java
import sh.pcx.unified.commands.annotation.*;
import org.bukkit.entity.Player;

@Command(name = "spawn", description = "Teleport to spawn")
public class SpawnCommand {

    @Default
    public void execute(@Sender Player player) {
        player.teleport(player.getWorld().getSpawnLocation());
        player.sendMessage("Teleported to spawn!");
    }
}
```

### Command with Aliases

```java
@Command(
    name = "teleport",
    aliases = {"tp", "tele"},
    description = "Teleport commands"
)
public class TeleportCommand {

    @Default
    public void teleport(@Sender Player player) {
        // Default behavior when /teleport is used without subcommand
        player.sendMessage("Usage: /teleport <player> or /teleport <x> <y> <z>");
    }
}
```

### Player-Only Command

```java
@Command(
    name = "fly",
    description = "Toggle flight mode",
    playerOnly = true  // Console cannot use this command
)
public class FlyCommand {

    @Default
    public void toggle(@Sender Player player) {
        player.setAllowFlight(!player.getAllowFlight());
        player.sendMessage("Flight " + (player.getAllowFlight() ? "enabled" : "disabled"));
    }
}
```

### Hidden Command

```java
@Command(
    name = "debug",
    description = "Debug commands",
    hidden = true  // Hidden from help listings
)
public class DebugCommand {
    // ...
}
```

---

## Subcommands

### Defining Subcommands

```java
@Command(name = "gamemode", aliases = {"gm"}, description = "Change gamemode")
public class GamemodeCommand {

    @Subcommand("creative")
    public void creative(@Sender Player player) {
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage("Gamemode set to Creative");
    }

    @Subcommand("survival")
    public void survival(@Sender Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage("Gamemode set to Survival");
    }

    @Subcommand("spectator")
    public void spectator(@Sender Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("Gamemode set to Spectator");
    }

    @Subcommand("adventure")
    public void adventure(@Sender Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage("Gamemode set to Adventure");
    }
}
```

### Nested Subcommands

```java
@Command(name = "warp", description = "Warp management")
public class WarpCommand {

    @Subcommand("set")
    public void setWarp(
        @Sender Player player,
        @Arg("name") String warpName
    ) {
        // /warp set <name>
    }

    @Subcommand("delete")
    public void deleteWarp(
        @Sender Player player,
        @Arg("name") String warpName
    ) {
        // /warp delete <name>
    }

    @Subcommand("list")
    public void listWarps(@Sender Player player) {
        // /warp list
    }

    @Subcommand("tp")  // Or use @Default for the main action
    public void teleport(
        @Sender Player player,
        @Arg("name") String warpName
    ) {
        // /warp tp <name>
    }
}
```

---

## Arguments

### Basic Arguments

```java
@Command(name = "give", description = "Give items to players")
public class GiveCommand {

    @Default
    public void give(
        @Sender Player sender,
        @Arg("player") Player target,
        @Arg("item") Material material,
        @Arg("amount") int amount
    ) {
        ItemStack item = new ItemStack(material, amount);
        target.getInventory().addItem(item);
        sender.sendMessage("Gave " + amount + "x " + material.name() + " to " + target.getName());
    }
}
```

### Optional Arguments

```java
@Command(name = "heal", description = "Heal a player")
public class HealCommand {

    @Default
    public void heal(
        @Sender Player sender,
        @Arg(value = "player", optional = true) Player target,
        @Arg(value = "amount", optional = true, defaultValue = "20") double amount
    ) {
        Player toHeal = target != null ? target : sender;
        toHeal.setHealth(Math.min(amount, toHeal.getMaxHealth()));
        sender.sendMessage("Healed " + toHeal.getName());
    }
}
```

### Variadic Arguments (Greedy Strings)

```java
@Command(name = "broadcast", description = "Broadcast a message")
public class BroadcastCommand {

    @Default
    public void broadcast(
        @Sender CommandSender sender,
        @Arg(value = "message", greedy = true) String message
    ) {
        Bukkit.broadcastMessage("[Broadcast] " + message);
    }
}
```

### Supported Argument Types

The framework automatically parses these types:

| Type | Example Input |
|------|---------------|
| `String` | `hello` |
| `int` / `Integer` | `42` |
| `long` / `Long` | `123456` |
| `double` / `Double` | `3.14` |
| `float` / `Float` | `1.5` |
| `boolean` / `Boolean` | `true`, `false`, `yes`, `no` |
| `Player` | `Steve` (online player name) |
| `OfflinePlayer` | `Steve` (any known player) |
| `World` | `world_nether` |
| `Material` | `diamond_sword` |
| `Duration` | `1h30m`, `5m`, `30s` |
| `UUID` | `550e8400-e29b-41d4-a716-446655440000` |

---

## Tab Completion

### Built-in Completion Providers

```java
@Command(name = "teleport", description = "Teleport to a player")
public class TeleportCommand {

    @Subcommand("player")
    public void teleportToPlayer(
        @Sender Player sender,
        @Arg("target") @Completions("@players") Player target
    ) {
        sender.teleport(target.getLocation());
    }

    @Subcommand("world")
    public void teleportToWorld(
        @Sender Player sender,
        @Arg("world") @Completions("@worlds") World world
    ) {
        sender.teleport(world.getSpawnLocation());
    }
}
```

### Available Built-in Providers

| Provider | Description |
|----------|-------------|
| `@players` | All online player names |
| `@offlinePlayers` | All known player names |
| `@worlds` | All loaded world names |
| `@materials` | All Material enum values |
| `@enchantments` | All enchantment names |
| `@potionTypes` | All potion effect types |
| `@gamemodes` | All GameMode values |
| `@sounds` | All Sound enum values |
| `@particles` | All Particle enum values |
| `@biomes` | All biome names |
| `@entityTypes` | All EntityType values |
| `@range:min:max` | Integer range (e.g., `@range:1:100`) |
| `@empty` | No suggestions |
| `@nothing` | Disable auto-completion |

### Custom Completion Providers

```java
// Register during plugin initialization
commands.registerCompletions("@warps", context -> {
    return warpManager.getWarpNames();
});

commands.registerCompletions("@arenas", context -> {
    return arenaManager.getArenas().stream()
        .map(Arena::getName)
        .collect(Collectors.toList());
});

// Use in command
@Subcommand("warp")
public void warp(
    @Sender Player player,
    @Arg("name") @Completions("@warps") String warpName
) {
    warpManager.teleport(player, warpName);
}
```

### Context-Aware Completions

```java
// Register context-aware provider
commands.registerCompletions("@teamMembers", context -> {
    Player sender = context.getSender();
    Team team = teamManager.getTeam(sender);
    return team != null ? team.getMemberNames() : Collections.emptyList();
});

@Subcommand("team kick")
public void kickMember(
    @Sender Player sender,
    @Arg("member") @Completions("@teamMembers") String memberName
) {
    // Only suggests members of the sender's team
}
```

### Multiple and Static Completions

```java
// Multiple providers
@Arg("target") @Completions({"@players", "@self"}) String target

// Static values
@Arg("size") @Completions({"small", "medium", "large"}) String size

// Mixed static and dynamic
@Arg("mode") @Completions({"pvp", "pve", "@customModes"}) String mode
```

### Completion Options

```java
@Completions(
    value = "@players",
    filter = true,        // Filter based on input (default: true)
    caseSensitive = false, // Case-insensitive matching (default: false)
    limit = 20            // Max suggestions (default: -1 for unlimited)
)
```

---

## Permissions

### Command-Level Permission

```java
@Command(
    name = "fly",
    description = "Toggle flight",
    permission = "myplugin.fly"
)
public class FlyCommand {
    // Players need "myplugin.fly" to use any part of this command
}
```

### Subcommand-Level Permission

```java
@Command(name = "gamemode", aliases = {"gm"}, description = "Change gamemode")
@Permission("essentials.gamemode")  // Base permission
public class GamemodeCommand {

    @Subcommand("creative")
    @Permission("essentials.gamemode.creative")  // Additional permission
    public void creative(@Sender Player player) {
        player.setGameMode(GameMode.CREATIVE);
    }

    @Subcommand("survival")
    @Permission("essentials.gamemode.survival")
    public void survival(@Sender Player player) {
        player.setGameMode(GameMode.SURVIVAL);
    }
}
```

### Permission for Other Players

```java
@Command(name = "heal", description = "Heal players")
public class HealCommand {

    @Subcommand("self")
    @Permission("myplugin.heal")
    public void healSelf(@Sender Player player) {
        player.setHealth(player.getMaxHealth());
    }

    @Subcommand("other")
    @Permission("myplugin.heal.others")  // Higher permission for targeting others
    public void healOther(
        @Sender Player sender,
        @Arg("target") Player target
    ) {
        target.setHealth(target.getMaxHealth());
    }
}
```

---

## Cooldowns

### Basic Cooldown

```java
@Command(name = "spawn", description = "Teleport to spawn")
public class SpawnCommand {

    @Default
    @Cooldown(5000)  // 5 second cooldown in milliseconds
    public void spawn(@Sender Player player) {
        player.teleport(player.getWorld().getSpawnLocation());
    }
}
```

### Cooldown Options

```java
@Cooldown(
    value = 30000,              // 30 seconds
    message = "Please wait %time% before using this again!",
    bypassPermission = "myplugin.cooldown.bypass"
)
public void execute(@Sender Player player) {
    // ...
}
```

### Per-Command Cooldowns

```java
@Command(name = "teleport", description = "Teleportation commands")
public class TeleportCommand {

    @Subcommand("home")
    @Cooldown(5000)  // 5 second cooldown
    public void home(@Sender Player player) {
        // Teleport to home
    }

    @Subcommand("spawn")
    @Cooldown(3000)  // 3 second cooldown
    public void spawn(@Sender Player player) {
        // Teleport to spawn
    }

    @Subcommand("warp")
    @Cooldown(10000)  // 10 second cooldown
    public void warp(@Sender Player player, @Arg("name") String warpName) {
        // Teleport to warp
    }
}
```

---

## Async Commands

### Async Execution

```java
@Command(name = "stats", description = "Player statistics")
public class StatsCommand {

    @Inject
    private DatabaseService database;

    @Default
    @Async  // Execute on async thread
    public void showStats(@Sender Player player) {
        // Safe to do database queries
        PlayerStats stats = database.query(PlayerStats.class)
            .where("uuid", player.getUniqueId())
            .first()
            .orElse(new PlayerStats());

        // Return to main thread for sending messages
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage("Kills: " + stats.getKills());
            player.sendMessage("Deaths: " + stats.getDeaths());
        });
    }
}
```

### Mixed Sync/Async

```java
@Command(name = "lookup", description = "Look up player data")
public class LookupCommand {

    @Subcommand("online")
    public void lookupOnline(@Sender Player sender, @Arg("player") Player target) {
        // Sync - accessing online player data
        sender.sendMessage("Level: " + target.getLevel());
    }

    @Subcommand("offline")
    @Async
    public void lookupOffline(@Sender Player sender, @Arg("name") String name) {
        // Async - database query for offline player
        Optional<PlayerData> data = database.queryFirst(
            "SELECT * FROM players WHERE name = ?",
            PlayerData::fromResultSet,
            name
        );

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (data.isPresent()) {
                sender.sendMessage("Found: " + data.get().getName());
            } else {
                sender.sendMessage("Player not found");
            }
        });
    }
}
```

---

## Custom Parsers

### Creating a Custom Parser

```java
import sh.pcx.unified.commands.parsing.ArgumentParser;
import sh.pcx.unified.commands.parsing.ParseException;
import sh.pcx.unified.commands.core.CommandContext;
import sh.pcx.unified.commands.completion.CompletionContext;

public class ArenaParser implements ArgumentParser<Arena> {

    private final ArenaManager arenaManager;

    public ArenaParser(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public Arena parse(CommandContext context, String input) throws ParseException {
        Arena arena = arenaManager.getArena(input);
        if (arena == null) {
            throw new ParseException("Unknown arena: " + input);
        }
        return arena;
    }

    @Override
    public List<String> suggest(CompletionContext context) {
        return arenaManager.getArenas().stream()
            .map(Arena::getName)
            .filter(name -> name.toLowerCase().startsWith(context.getInput().toLowerCase()))
            .collect(Collectors.toList());
    }
}
```

### Registering Custom Parsers

```java
@Override
public void onEnable() {
    // Register custom parser
    commands.registerParser(Arena.class, new ArenaParser(arenaManager));
    commands.registerParser(Kit.class, new KitParser(kitManager));
    commands.registerParser(Rank.class, new RankParser(rankManager));

    // Now use in commands
    commands.register(new ArenaCommand());
}
```

### Using Custom Types

```java
@Command(name = "arena", description = "Arena commands")
public class ArenaCommand {

    @Subcommand("join")
    public void join(
        @Sender Player player,
        @Arg("arena") Arena arena  // Automatically parsed using ArenaParser
    ) {
        arena.addPlayer(player);
        player.sendMessage("Joined arena: " + arena.getName());
    }

    @Subcommand("info")
    public void info(
        @Sender Player player,
        @Arg("arena") Arena arena
    ) {
        player.sendMessage("Arena: " + arena.getName());
        player.sendMessage("Players: " + arena.getPlayerCount() + "/" + arena.getMaxPlayers());
        player.sendMessage("State: " + arena.getState());
    }
}
```

---

## Command Service

### Registration Methods

```java
// Register single command instance
commands.register(new SpawnCommand());

// Register with dependency injection
commands.register(SpawnCommand.class);

// Register multiple commands
commands.registerAll(
    new SpawnCommand(),
    new HomeCommand(),
    new WarpCommand()
);

// Register multiple classes with DI
commands.registerAllClasses(
    SpawnCommand.class,
    HomeCommand.class,
    WarpCommand.class
);
```

### Unregistration

```java
// Unregister by name
commands.unregister("spawn");

// Unregister by instance
commands.unregister(spawnCommand);

// Unregister all (usually in onDisable)
commands.unregisterAll();
```

### Checking Registration

```java
// Check if command is registered
boolean isRegistered = commands.isRegistered("spawn");

// Get all registered command names
Collection<String> commandNames = commands.getRegisteredCommands();
```

### Reloading Commands

```java
// Reload all command metadata (useful for development)
commands.reload();
```

---

## Complete Example

```java
@Command(
    name = "home",
    aliases = {"h", "homes"},
    description = "Home management commands",
    permission = "myplugin.home"
)
public class HomeCommand {

    @Inject
    private HomeManager homeManager;

    @Inject
    private ConfigService config;

    @Default
    @Cooldown(3000)
    public void teleportDefault(@Sender Player player) {
        // /home - teleport to default home
        teleportToHome(player, "home");
    }

    @Subcommand("tp")
    @Cooldown(3000)
    public void teleport(
        @Sender Player player,
        @Arg("name") @Completions("@homes") String homeName
    ) {
        // /home tp <name>
        teleportToHome(player, homeName);
    }

    @Subcommand("set")
    @Permission("myplugin.home.set")
    public void setHome(
        @Sender Player player,
        @Arg(value = "name", optional = true, defaultValue = "home") String homeName
    ) {
        // /home set [name]
        int maxHomes = config.get("homes.max", Integer.class).orElse(3);
        int currentHomes = homeManager.getHomeCount(player.getUniqueId());

        if (currentHomes >= maxHomes && !homeManager.hasHome(player.getUniqueId(), homeName)) {
            player.sendMessage("You have reached the maximum number of homes!");
            return;
        }

        homeManager.setHome(player.getUniqueId(), homeName, player.getLocation());
        player.sendMessage("Home '" + homeName + "' set!");
    }

    @Subcommand("delete")
    @Permission("myplugin.home.delete")
    public void deleteHome(
        @Sender Player player,
        @Arg("name") @Completions("@homes") String homeName
    ) {
        // /home delete <name>
        if (homeManager.deleteHome(player.getUniqueId(), homeName)) {
            player.sendMessage("Home '" + homeName + "' deleted!");
        } else {
            player.sendMessage("Home not found!");
        }
    }

    @Subcommand("list")
    public void listHomes(@Sender Player player) {
        // /home list
        List<String> homes = homeManager.getHomeNames(player.getUniqueId());
        if (homes.isEmpty()) {
            player.sendMessage("You have no homes set.");
        } else {
            player.sendMessage("Your homes: " + String.join(", ", homes));
        }
    }

    @Subcommand("admin tp")
    @Permission("myplugin.home.admin")
    public void adminTeleport(
        @Sender Player sender,
        @Arg("player") OfflinePlayer target,
        @Arg("home") String homeName
    ) {
        // /home admin tp <player> <home>
        Optional<Location> home = homeManager.getHome(target.getUniqueId(), homeName);
        if (home.isPresent()) {
            sender.teleport(home.get());
            sender.sendMessage("Teleported to " + target.getName() + "'s home: " + homeName);
        } else {
            sender.sendMessage("Home not found!");
        }
    }

    private void teleportToHome(Player player, String homeName) {
        Optional<Location> home = homeManager.getHome(player.getUniqueId(), homeName);
        if (home.isPresent()) {
            player.teleport(home.get());
            player.sendMessage("Teleported to home: " + homeName);
        } else {
            player.sendMessage("Home '" + homeName + "' not found!");
        }
    }
}
```

---

## Next Steps

- [GUI Guide](gui.md) - Create inventory-based GUIs
- [Testing Guide](testing.md) - Test commands with MockServer
- [Modules Guide](modules.md) - Organize commands in modules
