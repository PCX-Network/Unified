# Configuration System Guide

The UnifiedPlugin configuration system provides a powerful, type-safe way to manage configuration files using Sponge Configurate. It supports multiple formats, hot reloading, validation, and environment variable overrides.

## Table of Contents

- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Configuration Classes](#configuration-classes)
- [Annotations](#annotations)
- [Hot Reload](#hot-reload)
- [Validation](#validation)
- [Environment Overrides](#environment-overrides)
- [Profiles](#profiles)
- [Advanced Features](#advanced-features)

---

## Overview

### Key Features

- **Multi-format support:** YAML, HOCON, JSON, and TOML
- **Type-safe mapping:** Map configurations to annotated Java objects
- **Hot reload:** Automatic file watching with reload callbacks
- **Validation:** Constraint annotations with descriptive error messages
- **Environment overrides:** Override values using environment variables
- **Profiles:** Support for dev, prod, and custom profiles

### Getting the ConfigService

```java
import sh.pcx.unified.config.ConfigService;
import javax.inject.Inject;

public class MyPlugin extends UnifiedPlugin {

    @Inject
    private ConfigService configService;

    // Or retrieve from services
    @Override
    public void onEnable() {
        ConfigService config = getServices().get(ConfigService.class);
    }
}
```

---

## Basic Usage

### Loading a Configuration

```java
// Load a configuration file
PluginConfig config = configService.load(
    PluginConfig.class,
    getDataFolder().resolve("config.yml")
);

// Access configuration values
String serverName = config.getServerName();
int maxPlayers = config.getMaxPlayers();
```

### Saving a Configuration

```java
// Modify configuration
config.setMaxPlayers(50);

// Save to file
configService.save(config, getDataFolder().resolve("config.yml"));
```

### Async Operations

```java
// Load asynchronously
configService.loadAsync(PluginConfig.class, configPath)
    .thenAccept(config -> {
        getLogger().info("Loaded: " + config.getServerName());
    });

// Save asynchronously
configService.saveAsync(config, configPath)
    .thenRun(() -> getLogger().info("Config saved!"));
```

### Creating Defaults

```java
// Creates file with defaults if it doesn't exist
PluginConfig config = configService.createDefaults(
    PluginConfig.class,
    getDataFolder().resolve("config.yml")
);
```

---

## Configuration Classes

### Basic Configuration Class

```java
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class PluginConfig {

    @Comment("The server display name")
    private String serverName = "My Server";

    @Comment("Maximum players allowed")
    private int maxPlayers = 20;

    @Comment("Enable debug logging")
    private boolean debug = false;

    // Getters and setters
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
```

### Nested Configuration

```java
@ConfigSerializable
public class PluginConfig {

    @Comment("Database connection settings")
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Redis cache settings")
    private RedisConfig redis = new RedisConfig();

    @Comment("Feature toggles")
    private FeatureConfig features = new FeatureConfig();

    // Getters
    public DatabaseConfig getDatabase() {
        return database;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public FeatureConfig getFeatures() {
        return features;
    }
}

@ConfigSerializable
public class DatabaseConfig {

    private String host = "localhost";
    private int port = 3306;
    private String database = "minecraft";
    private String username = "root";
    private String password = "";
    private int poolSize = 10;

    // Getters and setters...
}

@ConfigSerializable
public class RedisConfig {

    private boolean enabled = false;
    private String host = "localhost";
    private int port = 6379;
    private String password = "";
    private int database = 0;

    // Getters and setters...
}
```

### Lists and Maps

```java
@ConfigSerializable
public class SpawnsConfig {

    @Comment("Spawn locations by name")
    private Map<String, LocationConfig> spawns = new HashMap<>();

    @Comment("Allowed worlds for spawning")
    private List<String> allowedWorlds = List.of("world", "world_nether");

    @Comment("Blacklisted players")
    private Set<String> blacklist = new HashSet<>();

    // Getters...
}

@ConfigSerializable
public class LocationConfig {

    private String world = "world";
    private double x = 0;
    private double y = 64;
    private double z = 0;
    private float yaw = 0;
    private float pitch = 0;

    // Getters and setters...
}
```

---

## Annotations

### @ConfigSerializable

Marks a class as serializable by Configurate:

```java
@ConfigSerializable
public class MyConfig {
    // Fields...
}
```

### @Comment

Adds a comment above the field in the generated config file:

```java
@Comment("This setting controls the server's difficulty level")
@Comment("Values: peaceful, easy, normal, hard")
private String difficulty = "normal";
```

### @Setting

Customizes the field name in the config file:

```java
import org.spongepowered.configurate.objectmapping.meta.Setting;

@Setting("max-players")  // Uses "max-players" instead of "maxPlayers"
private int maxPlayers = 20;

@Setting(value = "server-name", nodeFromParent = true)
private String serverName;
```

### @Required

Marks a field as required (must have a value):

```java
import org.spongepowered.configurate.objectmapping.meta.Required;

@Required
private String serverName;  // Will fail validation if null or missing
```

---

## Hot Reload

### Watching for Changes

```java
Path configPath = getDataFolder().resolve("config.yml");

// Enable hot reload with callback
configService.watch(PluginConfig.class, configPath, result -> {
    if (result.isSuccess()) {
        this.config = result.get();
        getLogger().info("Configuration reloaded automatically!");
        applyConfigChanges();
    } else {
        getLogger().warning("Failed to reload config: " + result.getError());
    }
});
```

### Stop Watching

```java
// Stop watching a specific file
configService.unwatch(configPath);
```

### Manual Reload

```java
// Force reload from disk
PluginConfig config = configService.reload(PluginConfig.class, configPath);
```

### Using ConfigRoot

`ConfigRoot` provides a higher-level API with built-in save/reload functionality:

```java
// Create a config root
ConfigRoot<PluginConfig> configRoot = configService.createRoot(
    PluginConfig.class,
    getDataFolder().resolve("config.yml")
);

// Access the configuration
PluginConfig config = configRoot.get();

// Modify and save
config.setMaxPlayers(50);
configRoot.save();

// Reload from disk
configRoot.reload();
```

---

## Validation

### Built-in Validation

```java
import sh.pcx.unified.config.validation.Range;
import sh.pcx.unified.config.validation.NotEmpty;
import sh.pcx.unified.config.validation.Pattern;

@ConfigSerializable
public class PluginConfig {

    @NotEmpty(message = "Server name cannot be empty")
    private String serverName = "My Server";

    @Range(min = 1, max = 100, message = "Max players must be between 1 and 100")
    private int maxPlayers = 20;

    @Range(min = 0.0, max = 1.0)
    private double spawnChance = 0.5;

    @Pattern(value = "^[a-zA-Z0-9_]+$", message = "Invalid prefix format")
    private String prefix = "Server";
}
```

### Validating Configurations

```java
// Validate a configuration
ValidationResult result = configService.validate(config);

if (!result.isValid()) {
    for (ValidationError error : result.getErrors()) {
        getLogger().warning("Config error: " + error.getField() + " - " + error.getMessage());
    }
}

// Load and validate in one step
Optional<PluginConfig> validated = configService.loadValidated(
    PluginConfig.class,
    configPath
);

if (validated.isEmpty()) {
    getLogger().severe("Configuration validation failed!");
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

### Custom Validators

```java
import sh.pcx.unified.config.validation.Validator;

public class PortValidator implements Validator<Integer> {

    @Override
    public boolean validate(Integer value) {
        return value != null && value >= 1 && value <= 65535;
    }

    @Override
    public String getMessage() {
        return "Port must be between 1 and 65535";
    }
}

// Use in configuration
@ConfigSerializable
public class ServerConfig {

    @Validate(PortValidator.class)
    private int port = 25565;
}
```

---

## Environment Overrides

Environment variables can override configuration values.

### Automatic Mapping

```java
// Apply environment overrides with a prefix
config = configService.applyEnvironmentOverrides(config, "MYPLUGIN");

// Environment variables are mapped by path:
// MYPLUGIN_DATABASE_HOST -> database.host
// MYPLUGIN_DATABASE_PORT -> database.port
// MYPLUGIN_MAX_PLAYERS -> maxPlayers
```

### Variable Substitution

In your configuration file, use `${ENV_VAR}` syntax:

```yaml
database:
  host: ${DATABASE_HOST:localhost}  # Default to localhost
  port: ${DATABASE_PORT:3306}
  password: ${DATABASE_PASSWORD}     # Required, no default
```

### Docker Example

```dockerfile
ENV MYPLUGIN_DATABASE_HOST=mysql-server
ENV MYPLUGIN_DATABASE_PORT=3306
ENV MYPLUGIN_DATABASE_PASSWORD=secret123
```

---

## Profiles

Profiles allow environment-specific configurations.

### Setting Active Profile

```java
// Set profile via code
configService.setActiveProfile("production");

// Or via environment variable
// UNIFIED_PROFILE=production

// Or via system property
// -Dunified.profile=production
```

### Profile-Specific Files

With profile `production`, the system looks for:
1. `config-production.yml` (profile-specific)
2. `config.yml` (default)

```java
// Loads config-production.yml if profile is "production"
PluginConfig config = configService.load(PluginConfig.class, configPath);
```

### Profile-Aware Configuration

```java
@ConfigSerializable
public class PluginConfig {

    private String serverName = "Development Server";
    private boolean debug = true;
    private DatabaseConfig database = new DatabaseConfig();
}

// config-development.yml
serverName: "Development Server"
debug: true
database:
  host: localhost
  poolSize: 2

// config-production.yml
serverName: "Production Server"
debug: false
database:
  host: mysql-cluster
  poolSize: 20
```

---

## Advanced Features

### Raw Node Access

For dynamic configuration access without mapping to objects:

```java
// Get raw ConfigNode
ConfigNode node = configService.getNode(configPath);

// Access values dynamically
String name = node.node("server", "name").getString("default");
int port = node.node("server", "port").getInt(25565);
List<String> worlds = node.node("worlds").getList(String.class);

// Check if path exists
if (node.node("features", "economy").virtual() == false) {
    // Economy config exists
}
```

### Multiple Formats

```java
import sh.pcx.unified.config.format.ConfigFormat;

// Load HOCON format
PluginConfig hoconConfig = configService.load(
    PluginConfig.class,
    getDataFolder().resolve("config.conf"),
    ConfigFormat.HOCON
);

// Load JSON format
PluginConfig jsonConfig = configService.load(
    PluginConfig.class,
    getDataFolder().resolve("config.json"),
    ConfigFormat.JSON
);

// Load TOML format
PluginConfig tomlConfig = configService.load(
    PluginConfig.class,
    getDataFolder().resolve("config.toml"),
    ConfigFormat.TOML
);
```

### Config Migration

```java
@ConfigSerializable
public class PluginConfig {

    @Comment("Config version - do not modify")
    private int configVersion = 2;

    // New field in v2
    private String newFeature = "default";

    // Deprecated field (kept for migration)
    @Deprecated
    private transient String oldField;
}

// Migration logic
public void migrateConfig(ConfigNode node) {
    int version = node.node("configVersion").getInt(1);

    if (version < 2) {
        // Migrate from v1 to v2
        String oldValue = node.node("oldField").getString("");
        node.node("newFeature").set(transformOldValue(oldValue));
        node.node("oldField").set(null);
        node.node("configVersion").set(2);
    }
}
```

### Serializers for Custom Types

```java
import org.spongepowered.configurate.serialize.TypeSerializer;

public class LocationSerializer implements TypeSerializer<UnifiedLocation> {

    @Override
    public UnifiedLocation deserialize(Type type, ConfigurationNode node) {
        String world = node.node("world").getString("world");
        double x = node.node("x").getDouble(0);
        double y = node.node("y").getDouble(64);
        double z = node.node("z").getDouble(0);
        return new UnifiedLocation(world, x, y, z);
    }

    @Override
    public void serialize(Type type, @Nullable UnifiedLocation obj, ConfigurationNode node) {
        if (obj == null) return;
        node.node("world").set(obj.getWorld());
        node.node("x").set(obj.getX());
        node.node("y").set(obj.getY());
        node.node("z").set(obj.getZ());
    }
}
```

---

## Complete Example

```java
@ConfigSerializable
public class GameConfig {

    @Comment("Server display name shown to players")
    @NotEmpty
    private String serverName = "Awesome Server";

    @Comment("Maximum concurrent players")
    @Range(min = 1, max = 500)
    private int maxPlayers = 100;

    @Comment("Enable debug mode for verbose logging")
    private boolean debug = false;

    @Comment("Database configuration")
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Economy settings")
    private EconomyConfig economy = new EconomyConfig();

    // Getters and setters...
}

@ConfigSerializable
public class EconomyConfig {

    @Comment("Starting balance for new players")
    @Range(min = 0)
    private double startingBalance = 1000.0;

    @Comment("Currency symbol")
    private String currencySymbol = "$";

    @Comment("Maximum balance a player can have")
    @Range(min = 0)
    private double maxBalance = 1_000_000_000.0;
}

// Usage in plugin
public class MyPlugin extends UnifiedPlugin {

    private GameConfig config;

    @Override
    public void onEnable() {
        ConfigService configService = getServices().get(ConfigService.class);
        Path configPath = getDataFolder().resolve("config.yml");

        // Create defaults and load
        config = configService.createDefaults(GameConfig.class, configPath);

        // Validate
        ValidationResult validation = configService.validate(config);
        if (!validation.isValid()) {
            getLogger().severe("Configuration errors:");
            validation.getErrors().forEach(e ->
                getLogger().severe("  - " + e.getField() + ": " + e.getMessage())
            );
            return;
        }

        // Enable hot reload
        configService.watch(GameConfig.class, configPath, result -> {
            if (result.isSuccess()) {
                this.config = result.get();
                getLogger().info("Config reloaded!");
            }
        });

        getLogger().info("Loaded config: " + config.getServerName());
    }
}
```

---

## Next Steps

- [Commands Guide](commands.md) - Create annotation-based commands
- [Database Guide](database.md) - Work with SQL, Redis, and MongoDB
- [Modules Guide](modules.md) - Build modular plugin architectures
