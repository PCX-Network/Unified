# Getting Started with UnifiedPlugin API

This guide covers installation, creating your first plugin, and understanding the basic concepts of the UnifiedPlugin API framework.

## Table of Contents

- [Installation](#installation)
- [Your First Plugin](#your-first-plugin)
- [Plugin Lifecycle](#plugin-lifecycle)
- [Dependency Injection](#dependency-injection)
- [Service Registry](#service-registry)
- [Platform Support](#platform-support)

---

## Installation

### Maven

Add the UnifiedPlugin API dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>pcx-network</id>
        <url>https://repo.pcxnetwork.net/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>sh.pcx</groupId>
        <artifactId>unified-api</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- Optional modules -->
    <dependency>
        <groupId>sh.pcx</groupId>
        <artifactId>unified-commands</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>sh.pcx</groupId>
        <artifactId>unified-config</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>sh.pcx</groupId>
        <artifactId>unified-data</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>sh.pcx</groupId>
        <artifactId>unified-gui</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>sh.pcx</groupId>
        <artifactId>unified-modules</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>sh.pcx</groupId>
        <artifactId>unified-testing</artifactId>
        <version>1.0.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Gradle

```kotlin
repositories {
    maven("https://repo.pcxnetwork.net/releases")
}

dependencies {
    compileOnly("sh.pcx:unified-api:1.0.0")
    compileOnly("sh.pcx:unified-commands:1.0.0")
    compileOnly("sh.pcx:unified-config:1.0.0")
    compileOnly("sh.pcx:unified-data:1.0.0")
    compileOnly("sh.pcx:unified-gui:1.0.0")
    compileOnly("sh.pcx:unified-modules:1.0.0")
    testImplementation("sh.pcx:unified-testing:1.0.0")
}
```

---

## Your First Plugin

### Basic Plugin Structure

Create your main plugin class by extending `UnifiedPlugin`:

```java
package com.example.myplugin;

import sh.pcx.unified.UnifiedPlugin;

public class MyPlugin extends UnifiedPlugin {

    @Override
    public void onLoad() {
        getLogger().info("Plugin loading...");
        // Register services, load configs
    }

    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled!");
        // Register commands, listeners, start tasks
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
        // Save data, cleanup resources
    }

    @Override
    public void onReload() {
        getLogger().info("Plugin reloading...");
        // Reload configurations, reset caches
    }
}
```

### Plugin Metadata

Your plugin requires metadata configuration. For Paper/Spigot, create `plugin.yml`:

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: "1.21"
description: My awesome plugin
authors:
  - YourName
depend:
  - UnifiedPlugin
```

### Accessing Plugin Resources

```java
@Override
public void onEnable() {
    // Get the data folder for saving files
    Path dataFolder = getDataFolder();

    // Access the server instance
    UnifiedServer server = getServer();

    // Get the logger
    Logger logger = getLogger();

    // Access plugin metadata
    PluginMeta meta = getMeta();
    String version = getVersion();
    String name = getName();

    // Save default config from JAR
    saveDefaultResource("config.yml");

    // Access the service registry
    ServiceRegistry services = getServices();
}
```

---

## Plugin Lifecycle

The UnifiedPlugin lifecycle consists of four main phases:

### 1. onLoad()

Called when the plugin JAR is loaded, before enabling. Use for:
- Registering services with the service registry
- Loading configuration files
- Setting up database connections
- Initializing static resources

**Note:** Other plugins may not be loaded yet. Do not access other plugins here.

### 2. onEnable()

Called when the plugin is enabled and ready. Use for:
- Registering event listeners
- Registering commands
- Starting scheduled tasks
- Hooking into other plugins
- Initializing gameplay features

All dependencies should be available at this point.

### 3. onDisable()

Called when the plugin is being disabled. Use for:
- Saving unsaved data
- Cancelling scheduled tasks
- Closing database connections
- Releasing resources
- Unregistering listeners and commands

This method should complete quickly to avoid blocking server shutdown.

### 4. onReload()

Called when an administrator requests a plugin reload. Use for:
- Reloading configuration files
- Resetting caches
- Refreshing external data sources
- Re-registering modified commands or listeners

---

## Dependency Injection

UnifiedPlugin supports Guice-based dependency injection with custom annotations.

### Service Annotation

Mark classes as injectable services:

```java
import sh.pcx.unified.inject.Service;

@Service
public class EconomyService {

    public double getBalance(UUID playerId) {
        // Implementation
    }

    public void deposit(UUID playerId, double amount) {
        // Implementation
    }
}
```

### Injecting Services

```java
import javax.inject.Inject;

public class ShopCommand {

    @Inject
    private EconomyService economy;

    @Inject
    private ConfigService config;

    public void purchase(Player player, String item) {
        double price = config.get("prices." + item, Double.class).orElse(100.0);
        economy.withdraw(player.getUniqueId(), price);
    }
}
```

### Scopes

UnifiedPlugin provides several scopes for controlling instance lifecycle:

```java
import sh.pcx.unified.inject.PluginScoped;
import sh.pcx.unified.inject.PlayerScoped;
import sh.pcx.unified.inject.WorldScoped;

@Service
@PluginScoped  // One instance per plugin
public class PluginWideService { }

@Service
@PlayerScoped  // One instance per player
public class PlayerDataService { }

@Service
@WorldScoped  // One instance per world
public class WorldService { }
```

### Lifecycle Annotations

```java
import sh.pcx.unified.inject.PostConstruct;
import sh.pcx.unified.inject.PreDestroy;
import sh.pcx.unified.inject.OnReload;

@Service
public class DatabaseService {

    @PostConstruct
    public void init() {
        // Called after dependency injection
        connectToDatabase();
    }

    @PreDestroy
    public void cleanup() {
        // Called before the service is destroyed
        closeConnections();
    }

    @OnReload
    public void reload() {
        // Called when plugin reloads
        refreshConnectionPool();
    }
}
```

---

## Service Registry

The `ServiceRegistry` allows you to register and retrieve services programmatically.

### Registering Services

```java
@Override
public void onLoad() {
    ServiceRegistry services = getServices();

    // Register a service instance
    services.register(EconomyService.class, new EconomyServiceImpl());

    // Register with a provider for lazy initialization
    services.registerProvider(DatabaseService.class, () -> {
        return new DatabaseServiceImpl(loadDatabaseConfig());
    });
}
```

### Retrieving Services

```java
@Override
public void onEnable() {
    ServiceRegistry services = getServices();

    // Get a required service (throws if not found)
    EconomyService economy = services.get(EconomyService.class);

    // Get an optional service
    Optional<DiscordService> discord = services.getOptional(DiscordService.class);
    discord.ifPresent(d -> d.sendStartupMessage());
}
```

---

## Platform Support

UnifiedPlugin supports multiple server platforms:

### Paper/Spigot

The primary supported platform. Full feature support including:
- All API features
- Adventure components
- Paper-specific optimizations

### Folia

Region-threaded server support with automatic task scheduling:
- Region-aware task execution
- Thread-safe player operations
- Automatic detection

```java
import sh.pcx.unified.platform.Platform;

if (Platform.current() == PlatformType.FOLIA) {
    // Folia-specific code
}
```

### Platform Detection

```java
import sh.pcx.unified.server.ServerType;
import sh.pcx.unified.server.MinecraftVersion;

@Override
public void onEnable() {
    UnifiedServer server = getServer();

    // Check server type
    ServerType type = server.getServerType();
    if (type == ServerType.PAPER) {
        getLogger().info("Running on Paper!");
    }

    // Check Minecraft version
    MinecraftVersion version = server.getMinecraftVersion();
    if (version.isAtLeast(1, 21)) {
        // Use 1.21+ features
    }
}
```

---

## Complete Example

Here's a complete example bringing everything together:

```java
package com.example.myplugin;

import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.config.ConfigService;
import sh.pcx.unified.commands.core.CommandService;
import javax.inject.Inject;

public class MyPlugin extends UnifiedPlugin {

    @Inject
    private ConfigService config;

    @Inject
    private CommandService commands;

    private MyPluginConfig pluginConfig;

    @Override
    public void onLoad() {
        // Save default config if it doesn't exist
        saveDefaultResource("config.yml");
    }

    @Override
    public void onEnable() {
        // Load configuration
        pluginConfig = config.load(
            MyPluginConfig.class,
            getDataFolder().resolve("config.yml")
        );

        // Register commands
        commands.register(new SpawnCommand(this));
        commands.register(new HomeCommand(this));

        // Log startup
        getLogger().info("MyPlugin v" + getVersion() + " enabled!");
        getLogger().info("Debug mode: " + pluginConfig.isDebugEnabled());
    }

    @Override
    public void onDisable() {
        // Cleanup
        commands.unregisterAll();
        getLogger().info("MyPlugin disabled!");
    }

    @Override
    public void onReload() {
        // Reload configuration
        pluginConfig = config.reload(
            MyPluginConfig.class,
            getDataFolder().resolve("config.yml")
        );
        getLogger().info("Configuration reloaded!");
    }

    public MyPluginConfig getPluginConfig() {
        return pluginConfig;
    }
}
```

---

## Next Steps

- [Configuration Guide](configuration.md) - Learn about the configuration system
- [Commands Guide](commands.md) - Create annotation-based commands
- [Database Guide](database.md) - Work with SQL, Redis, and MongoDB
- [Modules Guide](modules.md) - Build modular plugin architectures
- [GUI Guide](gui.md) - Create inventory-based GUIs
- [Testing Guide](testing.md) - Test your plugins with MockServer
