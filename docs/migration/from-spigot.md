# Migrating from Spigot API to UnifiedPlugin

This guide helps you migrate your existing Spigot/Bukkit plugin to the UnifiedPlugin API framework. By migrating, your plugin will gain cross-platform compatibility (Paper, Folia, Sponge), modern dependency injection, and a cleaner API design.

## Table of Contents

1. [Overview](#overview)
2. [Project Setup](#project-setup)
3. [Plugin Main Class](#plugin-main-class)
4. [Player Handling](#player-handling)
5. [Event Handling](#event-handling)
6. [Command Registration](#command-registration)
7. [Configuration](#configuration)
8. [Scheduling Tasks](#scheduling-tasks)
9. [Services and Dependency Injection](#services-and-dependency-injection)
10. [GUI/Inventory Menus](#guiinventory-menus)
11. [Common Gotchas](#common-gotchas)

---

## Overview

### Key Conceptual Differences

| Aspect | Spigot API | UnifiedPlugin API |
|--------|-----------|-------------------|
| Plugin Base | `JavaPlugin` | `UnifiedPlugin` |
| Players | `org.bukkit.entity.Player` | `UnifiedPlayer` interface |
| Events | Bukkit event system | `UnifiedEvent` with `EventBus` |
| Commands | `CommandExecutor` + plugin.yml | `@Command` annotations |
| Config | `FileConfiguration` | `ConfigService` with Configurate |
| Scheduling | `BukkitScheduler` | `SchedulerService` (Folia-aware) |
| Services | Manual singletons | `@Service` with Guice DI |

### What You Gain

- **Cross-platform support**: Write once, run on Paper, Folia, and Sponge
- **Folia compatibility**: Region-aware scheduling out of the box
- **Dependency injection**: Clean service architecture with Guice
- **Type-safe configurations**: Validation and hot-reload support
- **Modern command system**: Annotation-based with auto-completion

---

## Project Setup

### Before: Spigot Dependencies

```kotlin
// build.gradle.kts (Spigot)
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
}
```

### After: UnifiedPlugin Dependencies

```kotlin
// build.gradle.kts (UnifiedPlugin)
repositories {
    maven("https://repo.pcxnetwork.net/releases")
}

dependencies {
    compileOnly("sh.pcx:unified-api:1.0.0")

    // Optional modules as needed
    compileOnly("sh.pcx:unified-commands:1.0.0")
    compileOnly("sh.pcx:unified-config:1.0.0")
    compileOnly("sh.pcx:unified-gui:1.0.0")
    compileOnly("sh.pcx:unified-scheduler:1.0.0")
}
```

---

## Plugin Main Class

### Before: Spigot JavaPlugin

```java
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    private static MyPlugin instance;
    private DatabaseManager database;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        database = new DatabaseManager(getConfig());

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getCommand("mycommand").setExecutor(new MyCommand());

        getLogger().info("Plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("Plugin disabled!");
    }

    public static MyPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabase() {
        return database;
    }
}
```

### After: UnifiedPlugin

```java
import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.inject.Service;
import javax.inject.Inject;

public class MyPlugin extends UnifiedPlugin {

    @Inject
    private DatabaseService database;

    @Override
    public void onLoad() {
        // Register services during load phase
        saveDefaultResource("config.yml");
    }

    @Override
    public void onEnable() {
        // Services are automatically injected
        // Commands and listeners with @Service are auto-registered
        getLogger().info("Plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Cleanup is handled automatically for @Service classes
        getLogger().info("Plugin disabled!");
    }

    @Override
    public void onReload() {
        // Handle hot-reload (optional)
        getLogger().info("Configuration reloaded!");
    }
}
```

**Key Changes:**
- Extend `UnifiedPlugin` instead of `JavaPlugin`
- Use `@Inject` for dependencies instead of manual instantiation
- No need for static instance pattern
- `onReload()` lifecycle hook available

---

## Player Handling

### Before: Spigot Player

```java
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import net.md_5.bungee.api.chat.TextComponent;

public class PlayerManager {

    public void handlePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        // Get information
        String name = player.getName();
        Location loc = player.getLocation();
        double health = player.getHealth();

        // Modify player
        player.setHealth(20.0);
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);

        // Send messages (legacy)
        player.sendMessage("Hello " + name);

        // Send messages (Adventure on Paper)
        player.sendMessage(Component.text("Hello!"));

        // Teleport
        player.teleport(someLocation);

        // Permissions
        if (player.hasPermission("myplugin.admin")) {
            // Admin action
        }
    }
}
```

### After: UnifiedPlayer

```java
import sh.pcx.unified.UnifiedAPI;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer.GameMode;
import net.kyori.adventure.text.Component;

public class PlayerManager {

    public void handlePlayer(UUID uuid) {
        Optional<UnifiedPlayer> optPlayer = UnifiedAPI.getPlayer(uuid);
        if (optPlayer.isEmpty()) return;

        UnifiedPlayer player = optPlayer.get();

        // Get information
        String name = player.getName();
        UnifiedLocation loc = player.getLocation();
        double health = player.getHealth();

        // Modify player
        player.setHealth(20.0);
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);

        // Send messages (always Adventure API)
        player.sendMessage(Component.text("Hello " + name));

        // Teleport (returns CompletableFuture for async-safety)
        player.teleport(someLocation).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("Teleported!"));
            }
        });

        // Permissions
        if (player.hasPermission("myplugin.admin")) {
            // Admin action
        }

        // Access underlying platform player if needed
        org.bukkit.entity.Player bukkitPlayer = player.getHandle();
    }
}
```

**Key Changes:**
- Use `UnifiedAPI.getPlayer()` returning `Optional<UnifiedPlayer>`
- Teleport is async-safe, returns `CompletableFuture<Boolean>`
- Adventure text API is standard (no legacy `sendMessage(String)`)
- Use `player.getHandle()` to access platform-specific player when needed

---

## Event Handling

### Before: Spigot Events

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final MyPlugin plugin;

    public PlayerListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(player.getName() + " joined!");

        // Load player data
        plugin.getDatabase().loadPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDatabase().savePlayer(event.getPlayer().getUniqueId());
    }
}

// Registration in main class:
getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
```

### After: UnifiedPlugin Events

```java
import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.event.EventHandler;
import sh.pcx.unified.event.EventPriority;
import sh.pcx.unified.event.player.PlayerJoinEvent;
import sh.pcx.unified.event.player.PlayerQuitEvent;
import sh.pcx.unified.inject.Service;
import javax.inject.Inject;

@Service  // Auto-discovered and registered
public class PlayerListener {

    @Inject
    private DatabaseService database;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        UnifiedPlayer player = event.getPlayer();
        event.setJoinMessage(Component.text(player.getName() + " joined!"));

        // Load player data
        database.loadPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        database.savePlayer(event.getPlayer().getUniqueId());
    }
}

// No manual registration needed! @Service classes are auto-discovered.
```

### Custom Events

```java
// Before: Spigot custom event
public class CustomRewardEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private final Player player;
    private int amount;

    // ... constructor, getters, setters

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

// After: UnifiedPlugin custom event
public class CustomRewardEvent extends UnifiedEvent implements Cancellable {
    private boolean cancelled;
    private final UnifiedPlayer player;
    private int amount;

    public CustomRewardEvent(UnifiedPlayer player, int amount) {
        this.player = player;
        this.amount = amount;
    }

    // Getters and setters

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
```

**Key Changes:**
- No need for `HandlerList` boilerplate in custom events
- Event listeners with `@Service` are auto-registered
- Dependencies are injected, no constructor parameters needed
- Use unified event classes that work across platforms

---

## Command Registration

### Before: Spigot Commands

```java
// plugin.yml
// commands:
//   spawn:
//     description: Teleport to spawn
//     permission: myplugin.spawn
//     aliases: [s]

// Command class
public class SpawnCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                            String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage("Teleported to spawn!");
        } else if (args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("myplugin.spawn.set")) {
                player.sendMessage("No permission!");
                return true;
            }
            // Set spawn logic
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                      String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "reset");
        }
        return Collections.emptyList();
    }
}

// Registration
getCommand("spawn").setExecutor(new SpawnCommand());
getCommand("spawn").setTabCompleter(new SpawnCommand());
```

### After: UnifiedPlugin Commands

```java
import sh.pcx.unified.commands.annotation.*;
import sh.pcx.unified.player.UnifiedPlayer;
import net.kyori.adventure.text.Component;

@Command(
    name = "spawn",
    aliases = {"s"},
    description = "Teleport to spawn",
    permission = "myplugin.spawn",
    playerOnly = true
)
public class SpawnCommand {

    @Default
    public void teleportToSpawn(@Sender UnifiedPlayer player) {
        player.teleport(player.getWorld().getSpawnLocation())
            .thenAccept(success -> {
                player.sendMessage(Component.text("Teleported to spawn!"));
            });
    }

    @Subcommand("set")
    @Permission("myplugin.spawn.set")
    public void setSpawn(@Sender UnifiedPlayer player) {
        // Set spawn logic
        player.sendMessage(Component.text("Spawn location set!"));
    }

    @Subcommand("reset")
    @Permission("myplugin.spawn.reset")
    public void resetSpawn(@Sender UnifiedPlayer player) {
        // Reset spawn logic
    }
}

// No manual registration or plugin.yml entries needed!
```

### Commands with Arguments

```java
@Command(name = "teleport", aliases = {"tp"})
public class TeleportCommand {

    @Subcommand("player")
    public void teleportToPlayer(
        @Sender UnifiedPlayer sender,
        @Arg("target") UnifiedPlayer target
    ) {
        sender.teleport(target).thenAccept(success -> {
            if (success) {
                sender.sendMessage(Component.text("Teleported to " + target.getName()));
            }
        });
    }

    @Subcommand("coords")
    public void teleportToCoords(
        @Sender UnifiedPlayer player,
        @Arg("x") double x,
        @Arg("y") double y,
        @Arg("z") double z,
        @Arg("world") @Optional UnifiedWorld world  // Optional with default
    ) {
        UnifiedWorld targetWorld = world != null ? world : player.getWorld();
        UnifiedLocation loc = new UnifiedLocation(targetWorld, x, y, z);
        player.teleport(loc);
    }
}
```

**Key Changes:**
- No plugin.yml command entries needed
- `@Command`, `@Subcommand`, `@Permission` annotations
- `@Sender` for command sender, `@Arg` for arguments
- Auto tab-completion from parameter types
- Permission checks via annotations

---

## Configuration

### Before: Spigot FileConfiguration

```java
// config.yml loading
public class MyPlugin extends JavaPlugin {

    private boolean debugMode;
    private String databaseHost;
    private int databasePort;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();

        debugMode = config.getBoolean("debug", false);
        databaseHost = config.getString("database.host", "localhost");
        databasePort = config.getInt("database.port", 3306);

        // Validation is manual
        if (databasePort < 1 || databasePort > 65535) {
            getLogger().warning("Invalid port, using default");
            databasePort = 3306;
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        loadConfig();
    }
}
```

### After: UnifiedPlugin ConfigService

```java
import sh.pcx.unified.config.annotation.*;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PluginConfig {

    @ConfigComment("Enable debug logging")
    private boolean debug = false;

    @ConfigComment("Database connection settings")
    private DatabaseConfig database = new DatabaseConfig();

    // Getters
    public boolean isDebug() { return debug; }
    public DatabaseConfig getDatabase() { return database; }
}

@ConfigSerializable
public class DatabaseConfig {

    @ConfigComment("Database hostname")
    private String host = "localhost";

    @ConfigComment("Database port (1-65535)")
    @Range(min = 1, max = 65535)
    private int port = 3306;

    @ConfigComment("Database name")
    @NotEmpty
    private String database = "minecraft";

    @ConfigComment("Database username")
    private String username = "root";

    @ConfigComment("Database password")
    private String password = "";

    // Getters
}
```

```java
// In your plugin or service
@Service
public class ConfigManager {

    @Inject
    private ConfigService configService;

    @Inject
    private UnifiedPlugin plugin;

    private PluginConfig config;

    @PostConstruct
    public void init() {
        Path configPath = plugin.getDataFolder().resolve("config.yml");

        // Load with automatic validation
        config = configService.load(PluginConfig.class, configPath);

        // Enable hot-reload
        configService.watch(PluginConfig.class, configPath, result -> {
            if (result.isSuccess()) {
                this.config = result.get();
                plugin.getLogger().info("Config reloaded!");
            } else {
                plugin.getLogger().warning("Config reload failed: " + result.getError());
            }
        });
    }

    public PluginConfig getConfig() {
        return config;
    }
}
```

**Key Changes:**
- Type-safe configuration with `@ConfigSerializable` classes
- Built-in validation with `@Range`, `@NotEmpty`, etc.
- Auto-reload with `configService.watch()`
- Comments preserved in generated files
- Multi-format support (YAML, HOCON, JSON, TOML)

---

## Scheduling Tasks

### Before: Spigot BukkitScheduler

```java
public class MyPlugin extends JavaPlugin {

    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        // Run async delayed task
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            loadDataFromDatabase();
        }, 20L);

        // Run sync repeating task
        saveTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            saveAllPlayers();
        }, 0L, 6000L); // Every 5 minutes

        // Run task on main thread from async context
        Bukkit.getScheduler().runTask(this, () -> {
            player.sendMessage("Done!");
        });
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }
    }
}
```

### After: UnifiedPlugin SchedulerService (Folia-Compatible)

```java
import sh.pcx.unified.scheduler.SchedulerService;
import sh.pcx.unified.scheduler.TaskHandle;

@Service
public class DataManager {

    @Inject
    private SchedulerService scheduler;

    private TaskHandle saveTask;

    @PostConstruct
    public void init() {
        // Run async delayed task
        scheduler.runTaskLaterAsync(() -> {
            loadDataFromDatabase();
        }, 20L);

        // Run sync repeating task
        saveTask = scheduler.runTaskTimer(() -> {
            saveAllPlayers();
        }, 0L, 6000L);

        // Fluent builder API
        scheduler.builder()
            .async()
            .delay(5, TimeUnit.SECONDS)
            .repeat(5, TimeUnit.MINUTES)
            .execute(() -> cleanupCache())
            .build();
    }

    @PreDestroy
    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
    }

    // Folia-safe entity operations
    public void updatePlayer(UnifiedPlayer player) {
        scheduler.runAtPlayer(player, () -> {
            // This runs on the correct region thread for Folia
            player.setHealth(20.0);
        });
    }

    // Folia-safe location operations
    public void modifyBlock(UnifiedLocation location) {
        scheduler.runAtLocation(location, () -> {
            // This runs on the correct region thread for Folia
            location.getWorld().setBlockData(location, blockData);
        });
    }
}
```

### Task Chaining

```java
// Chain async and sync operations
scheduler.chain()
    .async(() -> {
        // Load from database (async)
        return loadPlayerData(uuid);
    })
    .sync(data -> {
        // Update player on main thread
        player.getInventory().setContents(data.getInventory());
    })
    .async(() -> {
        // Log to file (async)
        logPlayerLogin(uuid);
    })
    .execute();
```

**Key Changes:**
- Use `SchedulerService` instead of `BukkitScheduler`
- Folia-compatible with `runAtPlayer()`, `runAtLocation()`, `runOnGlobal()`
- Fluent `builder()` API for complex task configuration
- `chain()` for sequential async/sync operations
- `@PreDestroy` for automatic cleanup

---

## Services and Dependency Injection

### Before: Spigot Manual Singletons

```java
public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // Constructor, methods...
}

public class PlayerManager {
    private final DatabaseManager database;
    private final CacheManager cache;

    public PlayerManager() {
        this.database = DatabaseManager.getInstance();
        this.cache = CacheManager.getInstance();
    }
}
```

### After: UnifiedPlugin Dependency Injection

```java
import sh.pcx.unified.inject.Service;
import sh.pcx.unified.inject.PostConstruct;
import sh.pcx.unified.inject.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

@Service
@Singleton
public class DatabaseService implements Service {

    private HikariDataSource dataSource;

    @Inject
    private ConfigManager configManager;

    @PostConstruct
    public void init() {
        DatabaseConfig config = configManager.getConfig().getDatabase();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + config.getHost() + ":" + config.getPort());
        // ... configure
        dataSource = new HikariDataSource(hikari);
    }

    @PreDestroy
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

@Service
@Singleton
public class PlayerManager {

    @Inject
    private DatabaseService database;

    @Inject
    private CacheService cache;

    @Inject
    private SchedulerService scheduler;

    public CompletableFuture<PlayerData> loadPlayer(UUID uuid) {
        return scheduler.supplyAsync(() -> {
            // Load from cache or database
            return cache.get(uuid).orElseGet(() -> {
                PlayerData data = database.loadPlayer(uuid);
                cache.put(uuid, data);
                return data;
            });
        });
    }
}
```

### Scoped Services

```java
// Plugin-scoped: One instance per plugin
@Service
@PluginScoped
public class PluginSettingsService { }

// Player-scoped: One instance per player
@Service
@PlayerScoped
public class PlayerSession {

    @Inject
    public PlayerSession(UnifiedPlayer player) {
        this.playerId = player.getUniqueId();
        this.loginTime = System.currentTimeMillis();
    }
}

// World-scoped: One instance per world
@Service
@WorldScoped
public class WorldManager { }
```

**Key Changes:**
- No static singletons or `getInstance()` patterns
- Use `@Inject` for all dependencies
- `@PostConstruct` replaces manual init methods
- `@PreDestroy` for cleanup
- Scoped services for player/world-specific data

---

## GUI/Inventory Menus

### Before: Spigot Inventory GUI

```java
public class ShopGUI implements Listener {

    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Shop");

        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemMeta meta = diamond.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Diamond");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Price: $100",
            ChatColor.YELLOW + "Click to buy"
        ));
        diamond.setItemMeta(meta);

        inv.setItem(0, diamond);

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!openInventories.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getSlot() == 0) {
            // Buy diamond logic
            player.sendMessage("Bought diamond!");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }
}
```

### After: UnifiedPlugin GUI System

```java
import sh.pcx.unified.gui.*;
import sh.pcx.unified.gui.component.*;
import sh.pcx.unified.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ShopGUI extends AbstractGUI implements StatefulGUI {

    private static final StateKey<Integer> PAGE = StateKey.ofInt("page");

    @Inject
    private EconomyService economy;

    public ShopGUI(GUIContext context) {
        super(context, Layout.CHEST_54);
        setTitle(Component.text("Shop"));
        setState(PAGE, 1);
    }

    @Override
    protected void setup() {
        // Create items with builder
        UnifiedItemStack diamond = ItemBuilder.of(Material.DIAMOND)
            .name(Component.text("Diamond", NamedTextColor.AQUA))
            .lore(
                Component.text("Price: $100", NamedTextColor.GRAY),
                Component.text("Click to buy", NamedTextColor.YELLOW)
            )
            .build();

        // Set slot with click handler
        setSlot(0, Slot.of(diamond)
            .onClick(click -> {
                UnifiedPlayer player = click.getPlayer();

                if (economy.withdraw(player, 100)) {
                    player.giveItem(ItemBuilder.of(Material.DIAMOND).build());
                    player.sendMessage(Component.text("Bought diamond!"));
                } else {
                    player.sendMessage(Component.text("Not enough money!", NamedTextColor.RED));
                }

                return ClickResult.HANDLED;
            }));

        // Navigation buttons
        setSlot(45, BackButton.create());
        setSlot(53, CloseButton.create());
    }
}

// Opening the GUI
@Inject
private GUIManager guiManager;

public void openShop(UnifiedPlayer player) {
    guiManager.open(player, ShopGUI.class);
}
```

### Paginated GUI

```java
public class PlayerListGUI extends PaginatedGUI<UnifiedPlayer> {

    public PlayerListGUI(GUIContext context) {
        super(context, Layout.CHEST_54);
        setTitle(Component.text("Online Players"));
    }

    @Override
    protected List<UnifiedPlayer> getItems() {
        return new ArrayList<>(UnifiedAPI.getServer().getOnlinePlayers());
    }

    @Override
    protected Slot renderItem(UnifiedPlayer player, int index) {
        return Slot.of(ItemBuilder.ofPlayerHead(player)
            .name(Component.text(player.getName()))
            .lore(Component.text("Click to teleport"))
            .build())
            .onClick(click -> {
                click.getPlayer().teleport(player);
                return ClickResult.CLOSE;
            });
    }
}
```

**Key Changes:**
- Extend `AbstractGUI` or `PaginatedGUI` instead of manual inventory handling
- No event listener boilerplate needed
- `Slot.onClick()` for declarative click handling
- `StatefulGUI` for managing GUI state (pagination, filters)
- Built-in components: `BackButton`, `CloseButton`, `ToggleButton`

---

## Common Gotchas

### 1. Static Plugin Instance Access

**Problem:** You used `MyPlugin.getInstance()` everywhere.

**Solution:** Use dependency injection.

```java
// Before
MyPlugin.getInstance().getDatabase().save(data);

// After
@Inject
private DatabaseService database;

database.save(data);
```

### 2. Accessing Bukkit API Directly

**Problem:** Direct Bukkit calls won't work on Sponge.

**Solution:** Use UnifiedPlugin abstractions, or use `getHandle()` when platform-specific code is necessary.

```java
// Before
Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("Hello"));

// After
UnifiedAPI.getServer().getOnlinePlayers()
    .forEach(p -> p.sendMessage(Component.text("Hello")));

// When you MUST use platform-specific code
if (UnifiedAPI.getServer().getServerType() == ServerType.PAPER) {
    org.bukkit.entity.Player bukkitPlayer = player.getHandle();
    // Bukkit-specific operations
}
```

### 3. Synchronous Teleportation

**Problem:** `teleport()` returns `CompletableFuture` now.

**Solution:** Handle the async result.

```java
// Before
player.teleport(location);
player.sendMessage("Teleported!");

// After
player.teleport(location).thenAccept(success -> {
    if (success) {
        player.sendMessage(Component.text("Teleported!"));
    } else {
        player.sendMessage(Component.text("Teleport failed!"));
    }
});
```

### 4. BukkitScheduler on Folia

**Problem:** `BukkitScheduler.runTask()` is not region-safe on Folia.

**Solution:** Use `SchedulerService` with entity/location-bound tasks.

```java
// Before (breaks on Folia)
Bukkit.getScheduler().runTask(plugin, () -> {
    player.setHealth(20.0);
});

// After (works everywhere)
scheduler.runAtPlayer(player, () -> {
    player.setHealth(20.0);
});
```

### 5. plugin.yml Commands

**Problem:** You have commands defined in plugin.yml.

**Solution:** Remove them and use `@Command` annotations.

```yaml
# Before: plugin.yml
commands:
  mycommand:
    description: My command
    permission: myplugin.command
```

```java
// After: No plugin.yml needed
@Command(
    name = "mycommand",
    description = "My command",
    permission = "myplugin.command"
)
public class MyCommand { }
```

### 6. FileConfiguration Path Access

**Problem:** `config.getString("path.to.value")` doesn't exist.

**Solution:** Use typed configuration classes.

```java
// Before
String value = getConfig().getString("database.host", "localhost");

// After
@ConfigSerializable
public class MyConfig {
    private DatabaseConfig database = new DatabaseConfig();
}

String value = config.getDatabase().getHost();
```

### 7. Event Priority Naming

**Problem:** `EventPriority.HIGHEST` behaves differently.

**Solution:** UnifiedPlugin uses the same priority system, just verify import.

```java
// Before (Bukkit)
import org.bukkit.event.EventPriority;

// After (UnifiedPlugin)
import sh.pcx.unified.event.EventPriority;
```

### 8. Legacy Text Formatting

**Problem:** Using ChatColor and legacy strings.

**Solution:** Always use Adventure Component API.

```java
// Before
player.sendMessage(ChatColor.RED + "Error: " + ChatColor.WHITE + message);

// After
player.sendMessage(Component.text()
    .append(Component.text("Error: ", NamedTextColor.RED))
    .append(Component.text(message, NamedTextColor.WHITE))
    .build());

// Or use MiniMessage
player.sendMessage(MiniMessage.miniMessage()
    .deserialize("<red>Error: </red><white>" + message));
```

---

## Migration Checklist

- [ ] Update build.gradle.kts dependencies
- [ ] Change plugin main class from `JavaPlugin` to `UnifiedPlugin`
- [ ] Replace static instance with dependency injection
- [ ] Convert `Player` usage to `UnifiedPlayer`
- [ ] Migrate events to `@Service` annotated listeners
- [ ] Convert commands to annotation-based system
- [ ] Remove plugin.yml command definitions
- [ ] Migrate configuration to `@ConfigSerializable` classes
- [ ] Replace BukkitScheduler with SchedulerService
- [ ] Update GUI code to use the GUI framework
- [ ] Replace legacy text with Adventure Components
- [ ] Test on Paper, then optionally Folia and Sponge

---

## Need Help?

- [UnifiedPlugin API Documentation](../README.md)
- [Example Projects](../examples/)
- [Discord Community](https://discord.pcxnetwork.net)
- [GitHub Issues](https://github.com/PCX-Network/UnifiedPlugin-API/issues)
