# Module System Guide

The UnifiedPlugin module system allows you to build modular, maintainable plugins with independent feature units that can be loaded, enabled, disabled, and reloaded at runtime.

## Table of Contents

- [Overview](#overview)
- [Creating Modules](#creating-modules)
- [Module Lifecycle](#module-lifecycle)
- [Dependencies](#dependencies)
- [Health Monitoring](#health-monitoring)
- [Module Manager](#module-manager)
- [Admin Commands](#admin-commands)
- [Best Practices](#best-practices)

---

## Overview

### Key Features

- **Automatic discovery** via package scanning
- **Dependency resolution** with cycle detection
- **Hot reload** support without server restart
- **Config-driven** enable/disable via `modules.yml`
- **Health monitoring** with TPS awareness
- **Guice dependency injection** support

### Basic Concept

Modules are self-contained feature units that:
- Have their own lifecycle (init, enable, disable, reload)
- Can declare dependencies on other modules
- Can be independently toggled on/off
- Support health monitoring and graceful degradation

---

## Creating Modules

### Basic Module

```java
import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.annotation.Listen;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

@Module(name = "WelcomeMessage")
@Listen  // Register as event listener
public class WelcomeModule implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Welcome to the server!");
    }
}
```

### Full-Featured Module

```java
import sh.pcx.unified.modules.annotation.*;
import sh.pcx.unified.modules.lifecycle.*;
import sh.pcx.unified.modules.health.*;
import sh.pcx.unified.modules.core.ModuleContext;

@Module(
    name = "BattlePass",
    description = "Seasonal battle pass progression system",
    version = "2.1.0",
    authors = {"PCXNetwork"},
    dependencies = {"Economy", "PlayerData"},
    softDependencies = {"Cosmetics"},
    priority = ModulePriority.HIGH
)
@Listen
@Command
public class BattlePassModule implements
        Listener,
        Initializable,
        Reloadable,
        Disableable,
        Healthy {

    @Inject
    private EconomyService economy;

    @Inject @Nullable
    private CosmeticsService cosmetics;

    private BattlePassConfig config;
    private SeasonManager seasonManager;

    @Override
    public void init(ModuleContext context) {
        // Load configuration
        this.config = context.loadConfig(BattlePassConfig.class);

        // Initialize season manager
        this.seasonManager = new SeasonManager(config, economy);
        seasonManager.loadCurrentSeason();

        context.getLogger().info("BattlePass module initialized!");
    }

    @Override
    public void reload(ModuleContext context) {
        // Reload configuration
        this.config = context.reloadConfig(BattlePassConfig.class);

        // Refresh season data
        seasonManager.refresh();

        context.getLogger().info("BattlePass module reloaded!");
    }

    @Override
    public void onDisable(ModuleContext context) {
        // Save any pending data
        seasonManager.saveAll();

        context.getLogger().info("BattlePass module disabled!");
    }

    @Override
    public HealthStatus checkHealth() {
        Map<String, Object> metrics = Map.of(
            "activeSeason", seasonManager.getCurrentSeason().getName(),
            "activePlayers", seasonManager.getActivePlayerCount(),
            "pendingSaves", seasonManager.getPendingSaveCount()
        );

        if (!seasonManager.isHealthy()) {
            return HealthStatus.unhealthy("Season manager unhealthy", metrics);
        }

        if (seasonManager.getPendingSaveCount() > 100) {
            return HealthStatus.warning("High pending save count", metrics);
        }

        return HealthStatus.healthy("BattlePass operating normally", metrics);
    }

    // Event handlers...
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        seasonManager.loadPlayer(event.getPlayer().getUniqueId());
    }
}
```

---

## Module Lifecycle

### Lifecycle States

Modules transition through these states:

```
                    UNLOADED
                        |
                   register()
                        v
                    LOADING
                   /        \
              success       fail
                 v            v
             ENABLED       FAILED
                |             ^
           disable()          |
                v             |
             DISABLED --------+
                |        enable() fails
            unload()
                v
             UNLOADED
```

### State Definitions

| State | Description |
|-------|-------------|
| `UNLOADED` | Not loaded into the system |
| `LOADING` | Dependencies resolving, config loading, DI being performed |
| `ENABLED` | Active and functioning normally |
| `DISABLED` | Loaded but not active (can be re-enabled quickly) |
| `FAILED` | Failed to load due to error |

### Lifecycle Interfaces

#### Initializable

```java
import sh.pcx.unified.modules.lifecycle.Initializable;

public class MyModule implements Initializable {

    @Override
    public void init(ModuleContext context) {
        // Called when module is being enabled
        // Set up resources, load configs, register tasks
    }
}
```

#### Reloadable

```java
import sh.pcx.unified.modules.lifecycle.Reloadable;

public class MyModule implements Reloadable {

    @Override
    public void reload(ModuleContext context) {
        // Called when admin requests module reload
        // Refresh configs, reset caches
    }
}
```

#### Disableable

```java
import sh.pcx.unified.modules.lifecycle.Disableable;

public class MyModule implements Disableable {

    @Override
    public void onDisable(ModuleContext context) {
        // Called when module is being disabled
        // Save data, cleanup resources
    }
}
```

#### Schedulable

```java
import sh.pcx.unified.modules.lifecycle.Schedulable;
import sh.pcx.unified.modules.lifecycle.ScheduledTask;

public class MyModule implements Schedulable {

    @Override
    public List<ScheduledTask> getTasks() {
        return List.of(
            ScheduledTask.builder()
                .name("cleanup-task")
                .runnable(this::cleanup)
                .delay(0)
                .period(20 * 60)  // Every minute
                .async(true)
                .build(),

            ScheduledTask.builder()
                .name("save-task")
                .runnable(this::saveAll)
                .period(20 * 300)  // Every 5 minutes
                .build()
        );
    }
}
```

---

## Dependencies

### Required Dependencies

Modules that must be loaded before this one:

```java
@Module(
    name = "Rewards",
    dependencies = {"Economy", "PlayerData"}
)
public class RewardsModule implements Initializable {

    @Inject
    private EconomyService economy;  // Guaranteed to exist

    @Inject
    private PlayerDataService playerData;  // Guaranteed to exist

    @Override
    public void init(ModuleContext context) {
        // Both Economy and PlayerData modules are loaded
    }
}
```

If a dependency is missing or fails, this module will not load.

### Soft Dependencies

Optional modules that enhance functionality:

```java
@Module(
    name = "Rewards",
    softDependencies = {"Discord", "WebPanel"}
)
public class RewardsModule implements Initializable {

    @Inject @Nullable
    private DiscordService discord;

    @Override
    public void init(ModuleContext context) {
        if (discord != null) {
            discord.registerNotifications(this);
            context.getLogger().info("Discord integration enabled!");
        } else {
            context.getLogger().info("Running without Discord integration");
        }
    }
}
```

### Dependency Resolution

The module system automatically:
1. Builds a dependency graph
2. Detects circular dependencies
3. Calculates load order (topological sort)
4. Loads modules in correct order

```java
// Circular dependency detection
@Module(name = "A", dependencies = {"B"})
public class ModuleA { }

@Module(name = "B", dependencies = {"C"})
public class ModuleB { }

@Module(name = "C", dependencies = {"A"})  // CIRCULAR!
public class ModuleC { }

// Throws CircularDependencyException: A -> B -> C -> A
```

---

## Health Monitoring

### Implementing HealthCheck

```java
import sh.pcx.unified.modules.health.HealthCheck;
import sh.pcx.unified.modules.health.HealthStatus;

@Module(name = "DatabaseService")
public class DatabaseModule implements Initializable, HealthCheck {

    private DatabaseConnectionPool pool;

    @Override
    public HealthStatus checkHealth() {
        int active = pool.getActiveConnections();
        int max = pool.getMaxConnections();
        double usagePercent = (double) active / max * 100;

        Map<String, Object> metrics = Map.of(
            "activeConnections", active,
            "maxConnections", max,
            "usagePercent", usagePercent,
            "pendingQueries", pool.getPendingQueries()
        );

        if (!pool.isConnected()) {
            return HealthStatus.unhealthy("Database connection lost", metrics);
        }

        if (usagePercent > 90) {
            return HealthStatus.degraded("Connection pool near capacity", metrics);
        }

        if (usagePercent > 75) {
            return HealthStatus.warning("High connection pool usage", metrics);
        }

        return HealthStatus.healthy("Database connection pool healthy", metrics);
    }
}
```

### TPS-Aware Modules

Implement `Healthy` to react to server performance:

```java
import sh.pcx.unified.modules.lifecycle.Healthy;
import sh.pcx.unified.modules.health.HealthContext;

@Module(name = "ParticleEffects")
public class ParticleModule implements Healthy {

    private int particleMultiplier = 100;

    @Override
    public void ifUnhealthy(HealthContext context) {
        // Server TPS dropped below threshold
        context.getLogger().warning("TPS dropped to " + context.getCurrentTps()
            + ", reducing particle effects");

        // Reduce visual effects to help server recover
        particleMultiplier = 25;
    }

    @Override
    public void ifBackToHealth(HealthContext context) {
        // Server TPS recovered
        context.getLogger().info("TPS recovered to " + context.getCurrentTps()
            + ", restoring particle effects");

        particleMultiplier = 100;
    }
}
```

### Health Status Levels

```java
// Healthy - everything normal
HealthStatus.healthy("All systems operational", metrics);

// Warning - minor issues
HealthStatus.warning("High memory usage", metrics);

// Degraded - reduced functionality
HealthStatus.degraded("Running in degraded mode", metrics);

// Unhealthy - critical issues
HealthStatus.unhealthy("Service unavailable", metrics);
```

---

## Module Manager

### Setup

```java
public class MyPlugin extends UnifiedPlugin {

    private ModuleManager modules;

    @Override
    public void onEnable() {
        // Create module manager with builder
        modules = ModuleManager.builder(this)
            .scanPackage("com.example.myplugin.modules")
            .enableHealthMonitoring(true)
            .healthThreshold(18.0)       // TPS threshold for "unhealthy"
            .recoveryThreshold(19.5)     // TPS threshold for "recovered"
            .checkInterval(Duration.ofSeconds(5))
            .configPath(getDataFolder().toPath().resolve("modules.yml"))
            .build();

        // Discover and register all modules
        modules.registerAll();
    }

    @Override
    public void onDisable() {
        modules.disableAll();
    }
}
```

### Manual Module Registration

```java
// Register specific modules
modules.register(EconomyModule.class);
modules.register(BattlePassModule.class);

// Access module instances
EconomyModule economy = modules.get(EconomyModule.class);

// Get by name
Optional<Object> module = modules.get("Economy");
```

### Module Control

```java
// Check if enabled
boolean isEnabled = modules.isEnabled("BattlePass");

// Enable a disabled module
modules.enable("BattlePass");

// Disable a module
modules.disable("BattlePass");

// Reload a module
modules.reload("BattlePass");

// Reload all reloadable modules
modules.reloadAll();
```

### Module Information

```java
// Get module status
ModuleManager.ModuleStatus status = modules.getStatus("BattlePass");
logger.info("State: " + status.state());
logger.info("Enabled: " + status.isEnabled());
logger.info("Healthy: " + status.isHealthy());
if (status.error() != null) {
    logger.warning("Error: " + status.error());
}

// Get all modules info
List<ModuleManager.ModuleInfo> allModules = modules.getAllModules();
for (var info : allModules) {
    logger.info(info.name() + " v" + info.version()
        + " - " + info.state());
}
```

---

## Admin Commands

The module system provides built-in admin commands:

```
/modules list              - List all modules
/modules info <module>     - Show module details
/modules enable <module>   - Enable a module
/modules disable <module>  - Disable a module
/modules reload <module>   - Reload a module
/modules reload all        - Reload all modules
/modules health            - Show health status
```

### Example Output

```
/modules list
Modules (5 total, 4 enabled):
  [ENABLED]  Economy v1.0.0 - Currency and transactions
  [ENABLED]  PlayerData v1.2.0 - Player data storage
  [ENABLED]  BattlePass v2.1.0 - Seasonal progression
  [DISABLED] Cosmetics v1.0.0 - Visual customizations
  [FAILED]   OldModule - Missing dependency: LegacyAPI

/modules info BattlePass
Module: BattlePass
Version: 2.1.0
Description: Seasonal battle pass progression system
Authors: PCXNetwork
State: ENABLED
Dependencies: Economy, PlayerData
Soft Dependencies: Cosmetics
Priority: HIGH
Load Time: 45ms
Uptime: 2h 34m
Health: HEALTHY
  - activeSeason: Season 3
  - activePlayers: 127
  - pendingSaves: 3
```

---

## Best Practices

### 1. Keep Modules Focused

Each module should have a single responsibility:

```java
// Good - focused responsibility
@Module(name = "Economy")
public class EconomyModule { }

@Module(name = "Banking")
public class BankingModule { }

// Bad - too many responsibilities
@Module(name = "EconomyBankingShopAuction")
public class EverythingModule { }
```

### 2. Use Soft Dependencies for Optional Features

```java
@Module(
    name = "Rewards",
    dependencies = {"Economy"},        // Required
    softDependencies = {"Discord"}     // Optional enhancement
)
```

### 3. Handle Health Gracefully

```java
@Override
public void ifUnhealthy(HealthContext context) {
    // Don't completely disable - reduce impact
    reducedMode = true;
    cacheExpirationTime = Duration.ofMinutes(10);  // Cache longer
    batchSize = batchSize / 2;  // Smaller batches
}
```

### 4. Clean Up Resources

```java
@Override
public void onDisable(ModuleContext context) {
    // Save pending data
    saveQueue.forEach(this::saveImmediate);

    // Cancel tasks (usually automatic)
    // Close connections
    database.close();

    // Clear caches
    cache.invalidateAll();
}
```

### 5. Use ModuleContext for Logging

```java
@Override
public void init(ModuleContext context) {
    // Use module-prefixed logger
    context.getLogger().info("Initializing...");
    // Output: [MyPlugin] [EconomyModule] Initializing...
}
```

### 6. Configuration Per Module

```java
@Override
public void init(ModuleContext context) {
    // Loads from plugins/MyPlugin/modules/Economy/config.yml
    this.config = context.loadConfig(EconomyConfig.class);
}
```

---

## Complete Example

```java
@Module(
    name = "Teleportation",
    description = "Teleportation commands and home management",
    version = "1.0.0",
    dependencies = {"PlayerData"},
    priority = ModulePriority.NORMAL
)
@Listen
@Command
public class TeleportModule implements
        Listener,
        Initializable,
        Reloadable,
        Disableable,
        HealthCheck {

    @Inject
    private PlayerDataService playerData;

    private TeleportConfig config;
    private HomeManager homeManager;
    private Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    @Override
    public void init(ModuleContext context) {
        this.config = context.loadConfig(TeleportConfig.class);
        this.homeManager = new HomeManager(playerData, config);

        context.getLogger().info("Teleportation module loaded with "
            + config.getMaxHomes() + " max homes per player");
    }

    @Override
    public void reload(ModuleContext context) {
        this.config = context.reloadConfig(TeleportConfig.class);
        homeManager.setConfig(config);

        context.getLogger().info("Configuration reloaded");
    }

    @Override
    public void onDisable(ModuleContext context) {
        homeManager.saveAll();
        cooldowns.clear();
    }

    @Override
    public HealthStatus checkHealth() {
        int pendingSaves = homeManager.getPendingSaveCount();

        Map<String, Object> metrics = Map.of(
            "pendingSaves", pendingSaves,
            "cachedHomes", homeManager.getCacheSize(),
            "activeCooldowns", cooldowns.size()
        );

        if (pendingSaves > 50) {
            return HealthStatus.warning("High pending save count", metrics);
        }

        return HealthStatus.healthy("Operating normally", metrics);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        homeManager.loadHomes(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        homeManager.saveAndUnload(uuid);
        cooldowns.remove(uuid);
    }

    // Command methods would go here...
}
```

---

## Next Steps

- [Commands Guide](commands.md) - Create annotation-based commands
- [GUI Guide](gui.md) - Create inventory-based GUIs
- [Testing Guide](testing.md) - Test modules with MockServer
