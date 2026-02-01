# Unified Plugin API

A cross-platform Minecraft plugin framework providing unified abstractions for building plugins that work seamlessly across **Paper**, **Spigot**, **Folia**, and **Sponge** servers.

## Features

- **Cross-Platform Support** - Write once, run on Paper, Spigot, Folia, and Sponge
- **Unified Abstractions** - Common interfaces for players, worlds, items, and more
- **Modern Java** - Built for Java 21 with preview features
- **Modular Architecture** - Use only what you need with 18 specialized modules
- **Service Registry** - Dependency injection and service discovery
- **Adventure Support** - Native Adventure component support across platforms
- **Folia Compatible** - Region-aware scheduling for Folia servers

## Modules

| Module | Description |
|--------|-------------|
| `unified-api` | Core interfaces and abstractions |
| `unified-platform` | Platform-specific implementations (Paper, Sponge, Folia) |
| `unified-core` | Main runtime plugin JAR |
| `unified-commands` | Command framework with annotations |
| `unified-config` | Configuration management (YAML, JSON, HOCON) |
| `unified-data` | Data persistence (SQL, MongoDB, Redis) |
| `unified-gui` | Inventory GUI framework |
| `unified-scheduler` | Cross-platform task scheduling |
| `unified-i18n` | Internationalization and localization |
| `unified-economy` | Economy integration (Vault support) |
| `unified-modules` | Hot-loadable module system |
| `unified-network` | Plugin messaging and networking |
| `unified-world` | World and location utilities |
| `unified-content` | Custom items, blocks, and recipes |
| `unified-visual` | Particles, sounds, and visual effects |
| `unified-version` | NMS version abstraction |
| `unified-testing` | Testing utilities and mocks |
| `unified-tools` | Gradle plugin for development |

## Installation

### For Server Administrators

1. Download `UnifiedPluginAPI-{version}.jar` from [Releases](https://github.com/PCX-Network/Unified/releases)
2. Place in your server's `plugins/` folder
3. Restart your server

The plugin works on:
- **Paper** 1.21+
- **Spigot** 1.21+
- **Folia** 1.21+
- **Sponge** API 11+

### For Plugin Developers

Add the dependency to your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://repo.pcx.sh/releases")
    maven("https://repo.pcx.sh/snapshots")
}

dependencies {
    compileOnly("sh.pcx.unified:unified-api:1.0.0-SNAPSHOT")
}
```

Or for Maven:

```xml
<repository>
    <id>pcx-repo</id>
    <url>https://repo.pcx.sh/releases</url>
</repository>

<dependency>
    <groupId>sh.pcx.unified</groupId>
    <artifactId>unified-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

## Quick Start

### Creating a Plugin (Bukkit/Paper/Folia)

```java
package com.example.myplugin;

import sh.pcx.unified.platform.paper.BukkitUnifiedPlugin;

public class MyPlugin extends BukkitUnifiedPlugin {

    @Override
    protected void onPluginLoad() {
        getLogger().info("Loading MyPlugin...");
    }

    @Override
    protected void onPluginEnable() {
        getLogger().info("MyPlugin enabled!");

        // Access unified server
        var server = getUnifiedBridge().getServer();
        getLogger().info("Running on: " + server.getPlatform().getName());
    }

    @Override
    protected void onPluginDisable() {
        getLogger().info("MyPlugin disabled!");
    }

    @Override
    protected void onPluginReload() {
        getLogger().info("MyPlugin reloaded!");
    }
}
```

### Creating a Plugin (Sponge)

```java
package com.example.myplugin;

import com.google.inject.Inject;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("myplugin")
public class MyPlugin {

    @Inject
    public MyPlugin() {
    }

    @Listener
    public void onServerStarted(StartedEngineEvent<org.spongepowered.api.Server> event) {
        // Plugin logic here
    }
}
```

### Working with Players

```java
// Get a unified player
UnifiedPlayer player = getUnifiedBridge().getServer().getPlayer(uuid).orElse(null);

if (player != null) {
    // Send a message (Adventure components)
    player.sendMessage(Component.text("Hello!").color(NamedTextColor.GREEN));

    // Teleport
    player.teleport(location);

    // Check permissions
    if (player.hasPermission("myplugin.admin")) {
        // Admin logic
    }
}
```

### Using the Service Registry

```java
// Register a service
ServiceRegistry services = getUnifiedBridge().getServices();
services.register(MyService.class, this, new MyServiceImpl());

// Retrieve a service
MyService service = services.get(MyService.class).orElseThrow();
```

## Building from Source

Requirements:
- Java 21+
- Gradle 8.14+

```bash
# Clone the repository
git clone https://github.com/PCX-Network/Unified.git
cd Unified

# Build all modules
./gradlew build

# Build the plugin JAR
./gradlew :unified-core:shadowJar

# Run tests
./gradlew test
```

The built JAR will be at `unified-core/build/libs/UnifiedPluginAPI-{version}.jar`

## Documentation

- [Javadocs](https://pcx-network.github.io/Unified/)
- [Wiki](https://github.com/PCX-Network/Unified/wiki) (Coming Soon)
- [Examples](examples/)

## Platform Detection

The API automatically detects and uses the appropriate platform:

| Platform | Provider | Priority |
|----------|----------|----------|
| Folia | `FoliaPlatformProvider` | 150 |
| Paper/Spigot | `PaperPlatformProvider` | 100 |
| Sponge | `SpongePlatformProvider` | 0 |

Higher priority providers are selected when multiple are compatible.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

**Supatuck** - [GitHub](https://github.com/supatuck)

## Acknowledgments

- [Paper](https://papermc.io/) - High performance Minecraft server
- [Sponge](https://spongepowered.org/) - Flexible plugin platform
- [Adventure](https://docs.advntr.dev/) - Text component library
- [Configurate](https://github.com/SpongePowered/Configurate) - Configuration library
