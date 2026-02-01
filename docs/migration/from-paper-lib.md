# Migrating from paper-lib to UnifiedPlugin

This guide helps you migrate your plugin from paper-lib (PaperLib by PaperMC) to the UnifiedPlugin API. UnifiedPlugin provides a more comprehensive abstraction layer that goes beyond async teleportation to include full cross-platform support, dependency injection, and modern API patterns.

## Table of Contents

1. [Overview](#overview)
2. [Project Setup](#project-setup)
3. [Async Teleportation](#async-teleportation)
4. [Chunk Operations](#chunk-operations)
5. [Version Detection](#version-detection)
6. [Scheduler Compatibility](#scheduler-compatibility)
7. [Region Support (Folia)](#region-support-folia)
8. [Complete Service Migration](#complete-service-migration)
9. [Common Gotchas](#common-gotchas)

---

## Overview

### What is paper-lib?

PaperLib is a library that provides async chunk loading and teleportation APIs with graceful fallback for non-Paper servers. It's commonly used for:

- Async teleportation
- Async chunk loading
- Paper feature detection
- Folia compatibility layer

### Why Migrate to UnifiedPlugin?

| Feature | paper-lib | UnifiedPlugin |
|---------|----------|---------------|
| Async Teleport | Yes | Yes |
| Async Chunk Load | Yes | Yes |
| Folia Support | Partial | Full |
| Sponge Support | No | Yes |
| Dependency Injection | No | Yes |
| Command Framework | No | Yes |
| Configuration | No | Yes |
| GUI Framework | No | Yes |
| Event System | No | Yes |

UnifiedPlugin includes everything paper-lib offers, plus a complete plugin development framework.

---

## Project Setup

### Before: paper-lib Dependencies

```kotlin
// build.gradle.kts
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc:paperlib:1.0.8")
}
```

```java
// Plugin shading (required for paper-lib)
shadowJar {
    relocate("io.papermc.lib", "com.myplugin.libs.paperlib")
}
```

### After: UnifiedPlugin Dependencies

```kotlin
// build.gradle.kts
repositories {
    maven("https://repo.pcxnetwork.net/releases")
}

dependencies {
    compileOnly("sh.pcx:unified-api:1.0.0")
    compileOnly("sh.pcx:unified-scheduler:1.0.0")
}

// No shading required - UnifiedPlugin is a server plugin dependency
```

**Key Difference:** UnifiedPlugin is loaded as a server dependency, not shaded into your plugin.

---

## Async Teleportation

### Before: paper-lib Teleportation

```java
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportManager {

    public void teleportPlayer(Player player, Location destination) {
        PaperLib.teleportAsync(player, destination).thenAccept(result -> {
            if (result) {
                player.sendMessage("Teleported successfully!");
            } else {
                player.sendMessage("Teleportation failed!");
            }
        });
    }

    public void teleportWithEvent(Player player, Location destination) {
        PaperLib.teleportAsync(player, destination,
                PlayerTeleportEvent.TeleportCause.PLUGIN)
            .thenAccept(result -> {
                // Handle result
            });
    }
}
```

### After: UnifiedPlugin Teleportation

```java
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import net.kyori.adventure.text.Component;

public class TeleportManager {

    public void teleportPlayer(UnifiedPlayer player, UnifiedLocation destination) {
        player.teleport(destination).thenAccept(result -> {
            if (result) {
                player.sendMessage(Component.text("Teleported successfully!"));
            } else {
                player.sendMessage(Component.text("Teleportation failed!"));
            }
        });
    }

    // Teleport to another player
    public void teleportToPlayer(UnifiedPlayer player, UnifiedPlayer target) {
        player.teleport(target).thenAccept(result -> {
            if (result) {
                player.sendMessage(Component.text("Teleported to " + target.getName()));
            }
        });
    }
}
```

### Synchronous Fallback Handling

```java
// Before: paper-lib with fallback check
if (PaperLib.isPaper()) {
    PaperLib.teleportAsync(player, location);
} else {
    // Fallback for Spigot
    player.teleport(location);
}

// After: UnifiedPlugin (automatic handling)
// UnifiedPlayer.teleport() always returns CompletableFuture
// It uses async on Paper/Folia, sync on Spigot, and appropriate API on Sponge
player.teleport(location).thenAccept(success -> {
    // Works on all platforms
});
```

---

## Chunk Operations

### Before: paper-lib Chunk Loading

```java
import io.papermc.lib.PaperLib;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class ChunkManager {

    public void loadChunkAsync(World world, int x, int z) {
        PaperLib.getChunkAtAsync(world, x, z).thenAccept(chunk -> {
            // Chunk is loaded
            processChunk(chunk);
        });
    }

    public void loadChunkAtLocation(Location location) {
        PaperLib.getChunkAtAsync(location).thenAccept(chunk -> {
            // Chunk at location is loaded
        });
    }

    public void loadChunkGenerate(World world, int x, int z, boolean generate) {
        PaperLib.getChunkAtAsync(world, x, z, generate).thenAccept(chunk -> {
            // Chunk loaded (generated if needed and generate=true)
        });
    }
}
```

### After: UnifiedPlugin Chunk Loading

```java
import sh.pcx.unified.world.UnifiedWorld;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;

public class ChunkManager {

    public void loadChunkAsync(UnifiedWorld world, int x, int z) {
        world.getChunkAtAsync(x, z).thenAccept(chunk -> {
            // Chunk is loaded
            processChunk(chunk);
        });
    }

    public void loadChunkAtLocation(UnifiedLocation location) {
        location.getWorld().getChunkAtAsync(location).thenAccept(chunk -> {
            // Chunk at location is loaded
        });
    }

    public void loadChunkGenerate(UnifiedWorld world, int x, int z, boolean generate) {
        world.getChunkAtAsync(x, z, generate).thenAccept(chunk -> {
            // Chunk loaded with generation control
        });
    }

    // Additional UnifiedPlugin features
    public void checkChunkLoaded(UnifiedWorld world, int x, int z) {
        if (world.isChunkLoaded(x, z)) {
            UnifiedChunk chunk = world.getChunkAt(x, z);
            // Immediate access when loaded
        }
    }
}
```

### Bulk Chunk Operations

```java
// UnifiedPlugin provides batch operations
public void loadRegion(UnifiedWorld world, int startX, int startZ, int endX, int endZ) {
    List<CompletableFuture<UnifiedChunk>> futures = new ArrayList<>();

    for (int x = startX; x <= endX; x++) {
        for (int z = startZ; z <= endZ; z++) {
            futures.add(world.getChunkAtAsync(x, z));
        }
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> {
            // All chunks loaded
        });
}
```

---

## Version Detection

### Before: paper-lib Version Checks

```java
import io.papermc.lib.PaperLib;

public class CompatibilityChecker {

    public void checkEnvironment() {
        if (PaperLib.isPaper()) {
            getLogger().info("Running on Paper!");
        } else if (PaperLib.isSpigot()) {
            getLogger().info("Running on Spigot");
        }

        if (PaperLib.getMinecraftVersion() >= 17) {
            // Use 1.17+ features
        }

        if (PaperLib.getMinecraftPatchVersion() >= 1) {
            // Use patch-specific features
        }
    }

    public void suggestPaper() {
        PaperLib.suggestPaper(this);  // Logs recommendation to use Paper
    }
}
```

### After: UnifiedPlugin Platform Detection

```java
import sh.pcx.unified.UnifiedAPI;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.server.ServerType;
import sh.pcx.unified.server.MinecraftVersion;

public class CompatibilityChecker {

    public void checkEnvironment() {
        UnifiedServer server = UnifiedAPI.getServer();
        ServerType type = server.getServerType();

        switch (type) {
            case PAPER:
                getLogger().info("Running on Paper!");
                break;
            case FOLIA:
                getLogger().info("Running on Folia!");
                break;
            case SPIGOT:
                getLogger().info("Running on Spigot");
                break;
            case SPONGE:
                getLogger().info("Running on Sponge");
                break;
        }

        MinecraftVersion version = server.getMinecraftVersion();

        if (version.isAtLeast(1, 17)) {
            // Use 1.17+ features
        }

        if (version.isAtLeast(1, 20, 4)) {
            // Use 1.20.4+ features
        }

        // Detailed version info
        getLogger().info("MC Version: " + version.getMajor() + "." +
                        version.getMinor() + "." + version.getPatch());
    }

    // Check specific platform features
    public boolean supportsFolia() {
        return UnifiedAPI.getServer().getServerType() == ServerType.FOLIA;
    }

    public boolean supportsAdventure() {
        // All platforms in UnifiedPlugin support Adventure
        return true;
    }
}
```

### Platform-Specific Code

```java
// Execute platform-specific code safely
public void platformSpecificOperation(UnifiedPlayer player) {
    ServerType type = UnifiedAPI.getServer().getServerType();

    if (type == ServerType.PAPER || type == ServerType.FOLIA) {
        // Paper/Folia specific API
        org.bukkit.entity.Player bukkitPlayer = player.getHandle();
        bukkitPlayer.sendActionBar(Component.text("Action bar!"));
    } else if (type == ServerType.SPONGE) {
        // Sponge specific API
        ServerPlayer spongePlayer = player.getHandle();
        // Sponge operations
    }
}
```

---

## Scheduler Compatibility

### Before: paper-lib with BukkitScheduler

```java
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;

public class TaskManager {

    private final JavaPlugin plugin;

    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runLater(Runnable task, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    // paper-lib doesn't provide scheduler abstraction
    // You still use BukkitScheduler directly
}
```

### After: UnifiedPlugin SchedulerService

```java
import sh.pcx.unified.scheduler.SchedulerService;
import sh.pcx.unified.scheduler.TaskHandle;
import javax.inject.Inject;

@Service
public class TaskManager {

    @Inject
    private SchedulerService scheduler;

    public void runAsync(Runnable task) {
        scheduler.runTaskAsync(task);
    }

    public void runSync(Runnable task) {
        scheduler.runTask(task);
    }

    public void runLater(Runnable task, long delay) {
        scheduler.runTaskLater(task, delay);
    }

    // Fluent API
    public void scheduleComplexTask() {
        scheduler.builder()
            .async()
            .delay(5, TimeUnit.SECONDS)
            .repeat(1, TimeUnit.MINUTES)
            .execute(() -> performMaintenance())
            .build();
    }

    // CompletableFuture support
    public CompletableFuture<String> loadData() {
        return scheduler.supplyAsync(() -> {
            // Load from database
            return fetchFromDatabase();
        });
    }
}
```

---

## Region Support (Folia)

### Before: paper-lib Folia Handling

paper-lib provides basic Folia detection but requires manual handling:

```java
import io.papermc.lib.PaperLib;

public class FoliaCompatibility {

    private boolean isFolia;

    public void init() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public void modifyPlayer(Player player, Runnable action) {
        if (isFolia) {
            // Must use Folia's entity scheduler
            player.getScheduler().run(plugin, task -> action.run(), null);
        } else {
            // Standard Bukkit
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }

    public void modifyBlock(Location location, Runnable action) {
        if (isFolia) {
            // Must use Folia's region scheduler
            Bukkit.getRegionScheduler()
                .execute(plugin, location, action);
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
}
```

### After: UnifiedPlugin Automatic Folia Support

```java
import sh.pcx.unified.scheduler.SchedulerService;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;

@Service
public class RegionSafeOperations {

    @Inject
    private SchedulerService scheduler;

    // Automatically uses correct scheduler for platform
    public void modifyPlayer(UnifiedPlayer player, Runnable action) {
        scheduler.runAtPlayer(player, action);
        // On Folia: Uses entity scheduler
        // On Paper/Spigot: Uses main thread
    }

    public void modifyBlock(UnifiedLocation location, Runnable action) {
        scheduler.runAtLocation(location, action);
        // On Folia: Uses region scheduler
        // On Paper/Spigot: Uses main thread
    }

    // Entity-bound with delay
    public void delayedPlayerAction(UnifiedPlayer player, long ticks) {
        scheduler.runAtPlayerLater(player, () -> {
            player.setHealth(20.0);
            player.sendMessage(Component.text("Healed!"));
        }, ticks);
    }

    // Global region operations (Folia) / main thread (others)
    public void globalOperation() {
        scheduler.runOnGlobal(() -> {
            // Safe for non-world operations
        });
    }

    // Check current platform
    public void debugScheduler() {
        if (scheduler.isFolia()) {
            getLogger().info("Running on Folia - using region schedulers");
        }

        if (scheduler.isMainThread()) {
            getLogger().info("Currently on main/region thread");
        }
    }
}
```

### Retired Entity Handling

```java
// UnifiedPlugin handles entity retirement on Folia gracefully
public void safeEntityOperation(UnifiedPlayer player) {
    scheduler.builder()
        .entity(player)
        .delay(5, TimeUnit.SECONDS)
        .onRetired(() -> {
            // Called if player logs out before task runs
            getLogger().info("Player left before operation completed");
        })
        .execute(() -> {
            player.sendMessage(Component.text("Operation completed!"));
        })
        .build();
}
```

---

## Complete Service Migration

Here's an example of migrating a complete feature from paper-lib to UnifiedPlugin:

### Before: paper-lib Warp System

```java
public class WarpManager {

    private final JavaPlugin plugin;
    private final Map<String, Location> warps = new HashMap<>();

    public WarpManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadWarps() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            warps.put(name, section.getLocation(name));
        }
    }

    public void teleportToWarp(Player player, String warpName) {
        Location location = warps.get(warpName);
        if (location == null) {
            player.sendMessage(ChatColor.RED + "Warp not found!");
            return;
        }

        PaperLib.teleportAsync(player, location).thenAccept(success -> {
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Teleported to " + warpName);
            } else {
                player.sendMessage(ChatColor.RED + "Teleportation failed!");
            }
        });
    }

    public void setWarp(Player player, String warpName) {
        warps.put(warpName, player.getLocation());
        plugin.getConfig().set("warps." + warpName, player.getLocation());
        plugin.saveConfig();
        player.sendMessage(ChatColor.GREEN + "Warp set!");
    }
}
```

### After: UnifiedPlugin Warp System

```java
// Configuration class
@ConfigSerializable
public class WarpConfig {

    @ConfigComment("Configured warps")
    private Map<String, WarpLocation> warps = new HashMap<>();

    public Map<String, WarpLocation> getWarps() { return warps; }
    public void setWarps(Map<String, WarpLocation> warps) { this.warps = warps; }
}

@ConfigSerializable
public class WarpLocation {
    private String world;
    private double x, y, z;
    private float yaw, pitch;

    // Constructors, getters, conversion methods
    public UnifiedLocation toUnifiedLocation(UnifiedServer server) {
        return server.getWorld(world)
            .map(w -> new UnifiedLocation(w, x, y, z, yaw, pitch))
            .orElse(null);
    }

    public static WarpLocation fromUnifiedLocation(UnifiedLocation loc) {
        WarpLocation warp = new WarpLocation();
        warp.world = loc.getWorld().getName();
        warp.x = loc.getX();
        warp.y = loc.getY();
        warp.z = loc.getZ();
        warp.yaw = loc.getYaw();
        warp.pitch = loc.getPitch();
        return warp;
    }
}
```

```java
// Service class
@Service
@Singleton
public class WarpService implements Service {

    @Inject
    private ConfigService configService;

    @Inject
    private UnifiedPlugin plugin;

    @Inject
    private UnifiedServer server;

    private WarpConfig config;
    private Path configPath;

    @PostConstruct
    public void init() {
        configPath = plugin.getDataFolder().resolve("warps.yml");
        config = configService.load(WarpConfig.class, configPath);

        // Enable hot-reload
        configService.watch(WarpConfig.class, configPath, result -> {
            if (result.isSuccess()) {
                config = result.get();
            }
        });
    }

    public CompletableFuture<Boolean> teleportToWarp(UnifiedPlayer player, String warpName) {
        WarpLocation warpLoc = config.getWarps().get(warpName.toLowerCase());

        if (warpLoc == null) {
            player.sendMessage(Component.text("Warp not found!", NamedTextColor.RED));
            return CompletableFuture.completedFuture(false);
        }

        UnifiedLocation location = warpLoc.toUnifiedLocation(server);
        if (location == null) {
            player.sendMessage(Component.text("Warp world not loaded!", NamedTextColor.RED));
            return CompletableFuture.completedFuture(false);
        }

        return player.teleport(location).thenApply(success -> {
            if (success) {
                player.sendMessage(Component.text("Teleported to " + warpName, NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Teleportation failed!", NamedTextColor.RED));
            }
            return success;
        });
    }

    public void setWarp(UnifiedPlayer player, String warpName) {
        config.getWarps().put(
            warpName.toLowerCase(),
            WarpLocation.fromUnifiedLocation(player.getLocation())
        );

        configService.saveAsync(config, configPath).thenRun(() -> {
            player.sendMessage(Component.text("Warp set!", NamedTextColor.GREEN));
        });
    }

    public void deleteWarp(String warpName) {
        config.getWarps().remove(warpName.toLowerCase());
        configService.saveAsync(config, configPath);
    }

    public Set<String> getWarpNames() {
        return Collections.unmodifiableSet(config.getWarps().keySet());
    }
}
```

```java
// Command class
@Command(
    name = "warp",
    aliases = {"warps"},
    description = "Teleport to warps",
    permission = "myplugin.warp"
)
public class WarpCommand {

    @Inject
    private WarpService warpService;

    @Default
    public void listWarps(@Sender UnifiedPlayer player) {
        Set<String> warps = warpService.getWarpNames();
        if (warps.isEmpty()) {
            player.sendMessage(Component.text("No warps available.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Available warps: " +
            String.join(", ", warps), NamedTextColor.GOLD));
    }

    @Subcommand("tp")
    public void teleport(
        @Sender UnifiedPlayer player,
        @Arg("name") String warpName
    ) {
        warpService.teleportToWarp(player, warpName);
    }

    @Subcommand("set")
    @Permission("myplugin.warp.set")
    public void setWarp(
        @Sender UnifiedPlayer player,
        @Arg("name") String warpName
    ) {
        warpService.setWarp(player, warpName);
    }

    @Subcommand("delete")
    @Permission("myplugin.warp.delete")
    public void deleteWarp(
        @Sender UnifiedPlayer player,
        @Arg("name") String warpName
    ) {
        warpService.deleteWarp(warpName);
        player.sendMessage(Component.text("Warp deleted!", NamedTextColor.GREEN));
    }
}
```

---

## Common Gotchas

### 1. PaperLib Static Methods

**Problem:** paper-lib uses static methods like `PaperLib.teleportAsync()`.

**Solution:** Use injected services and instance methods.

```java
// Before
PaperLib.teleportAsync(player, location);

// After
@Inject
private SchedulerService scheduler;  // If needed for scheduling

// Teleport is now a method on UnifiedPlayer
player.teleport(location);
```

### 2. Bukkit Location vs UnifiedLocation

**Problem:** paper-lib uses Bukkit `Location` directly.

**Solution:** Convert or use UnifiedLocation throughout.

```java
// Before
Location bukkitLoc = player.getLocation();
PaperLib.teleportAsync(player, bukkitLoc);

// After
UnifiedLocation loc = player.getLocation();
player.teleport(loc);

// If you have a Bukkit Location from external source
Location bukkitLoc = someBukkitLocation;
UnifiedWorld world = UnifiedAPI.getServer().getWorld(bukkitLoc.getWorld().getName()).orElseThrow();
UnifiedLocation unifiedLoc = new UnifiedLocation(world,
    bukkitLoc.getX(), bukkitLoc.getY(), bukkitLoc.getZ(),
    bukkitLoc.getYaw(), bukkitLoc.getPitch());
```

### 3. Chunk Future Return Types

**Problem:** paper-lib returns `CompletableFuture<Chunk>` (Bukkit type).

**Solution:** UnifiedPlugin returns `CompletableFuture<UnifiedChunk>`.

```java
// Before
PaperLib.getChunkAtAsync(world, x, z).thenAccept(chunk -> {
    Block block = chunk.getBlock(0, 64, 0);  // Bukkit Block
});

// After
world.getChunkAtAsync(x, z).thenAccept(chunk -> {
    UnifiedBlock block = chunk.getBlock(0, 64, 0);  // UnifiedBlock
});
```

### 4. Plugin Instance Access

**Problem:** paper-lib methods require plugin instance for some operations.

**Solution:** Use dependency injection.

```java
// Before
PaperLib.suggestPaper(this);  // In main class

// After - version checking is more comprehensive
ServerType type = UnifiedAPI.getServer().getServerType();
if (type == ServerType.SPIGOT) {
    getLogger().warning("Consider using Paper for better performance!");
}
```

### 5. Minimum Version Check

**Problem:** paper-lib has `getMinecraftVersion()` returning int.

**Solution:** Use `MinecraftVersion` object for detailed comparisons.

```java
// Before
if (PaperLib.getMinecraftVersion() >= 17) {
    // 1.17+ code
}

// After
MinecraftVersion version = UnifiedAPI.getServer().getMinecraftVersion();

if (version.isAtLeast(1, 17)) {
    // 1.17+ code
}

if (version.isAtLeast(1, 20, 4)) {
    // Specific patch version
}

if (version.isBetween(1, 17, 1, 19)) {
    // Version range
}
```

### 6. isSpigot() vs ServerType

**Problem:** paper-lib has boolean checks like `isSpigot()`, `isPaper()`.

**Solution:** Use enum comparison for cleaner code.

```java
// Before
if (PaperLib.isPaper()) {
    // Paper code
} else if (PaperLib.isSpigot()) {
    // Spigot code
}

// After
switch (UnifiedAPI.getServer().getServerType()) {
    case PAPER:
        // Paper code
        break;
    case FOLIA:
        // Folia code (paper-lib doesn't have this!)
        break;
    case SPIGOT:
        // Spigot code
        break;
    case SPONGE:
        // Sponge code (paper-lib doesn't support this!)
        break;
}
```

### 7. Event Synchronization

**Problem:** paper-lib async operations complete on async threads.

**Solution:** UnifiedPlugin provides easy thread switching.

```java
// Before (paper-lib) - manual sync
PaperLib.getChunkAtAsync(world, x, z).thenAccept(chunk -> {
    // This runs async! Must sync for Bukkit API
    Bukkit.getScheduler().runTask(plugin, () -> {
        chunk.getBlock(0, 64, 0).setType(Material.STONE);
    });
});

// After (UnifiedPlugin) - scheduler handles it
world.getChunkAtAsync(x, z).thenAccept(chunk -> {
    scheduler.runAtLocation(location, () -> {
        // Automatically runs on correct thread
        chunk.getBlock(0, 64, 0).setType(Material.STONE);
    });
});

// Or use chain()
scheduler.chain()
    .async(() -> world.getChunkAtAsync(x, z).join())
    .syncAtLocation(location, chunk -> {
        chunk.getBlock(0, 64, 0).setType(Material.STONE);
    })
    .execute();
```

---

## Migration Checklist

- [ ] Remove paper-lib dependency and shading configuration
- [ ] Add UnifiedPlugin dependencies
- [ ] Replace `PaperLib.teleportAsync()` with `UnifiedPlayer.teleport()`
- [ ] Replace `PaperLib.getChunkAtAsync()` with `UnifiedWorld.getChunkAtAsync()`
- [ ] Replace `PaperLib.isPaper()` checks with `ServerType` enum
- [ ] Replace `PaperLib.getMinecraftVersion()` with `MinecraftVersion`
- [ ] Migrate BukkitScheduler usage to SchedulerService
- [ ] Add Folia region-awareness using `runAtPlayer()`, `runAtLocation()`
- [ ] Convert Bukkit types to Unified types throughout codebase
- [ ] Test on Paper, Folia, and optionally Sponge

---

## Feature Comparison Reference

| paper-lib Method | UnifiedPlugin Equivalent |
|-----------------|-------------------------|
| `PaperLib.teleportAsync(player, loc)` | `player.teleport(loc)` |
| `PaperLib.getChunkAtAsync(world, x, z)` | `world.getChunkAtAsync(x, z)` |
| `PaperLib.getChunkAtAsync(loc)` | `world.getChunkAtAsync(loc)` |
| `PaperLib.isPaper()` | `server.getServerType() == PAPER` |
| `PaperLib.isSpigot()` | `server.getServerType() == SPIGOT` |
| `PaperLib.getMinecraftVersion()` | `server.getMinecraftVersion()` |
| `PaperLib.getMinecraftPatchVersion()` | `server.getMinecraftVersion().getPatch()` |
| `PaperLib.suggestPaper(plugin)` | Manual logging if desired |

---

## Need Help?

- [UnifiedPlugin API Documentation](../README.md)
- [Migrating from Spigot](./from-spigot.md)
- [Version Upgrade Guide](./version-upgrade.md)
- [Discord Community](https://discord.pcxnetwork.net)
