# BasicPlugin - UnifiedPlugin API Example

This is a complete example plugin demonstrating the core features of the UnifiedPlugin API framework.

## Features Demonstrated

### 1. Plugin Lifecycle (`BasicPlugin.java`)
- Extending `UnifiedPlugin` base class
- Proper `onLoad()`, `onEnable()`, `onDisable()`, `onReload()` implementation
- Resource saving and data folder management

### 2. Configuration System (`config/BasicPluginConfig.java`)
- `@ConfigSerializable` annotation for type-safe configs
- Nested configuration sections
- `@ConfigComment` for documentation in YAML files
- `@Range` validation constraint
- Default values and getters/setters

### 3. Module System (`module/WelcomeModule.java`)
- `@Module` annotation with metadata
- `@Listen` for automatic event listener registration
- `Initializable`, `Reloadable`, `Disableable` lifecycle interfaces
- `ModuleContext` for accessing plugin services

### 4. Command Framework (`command/BasicCommand.java`)
- `@Command` annotation for command registration
- `@Subcommand` for nested commands
- `@Permission` for access control
- `@Arg` for argument parsing
- `@Default` for default values and handlers
- `@Completions` for tab completion
- `@Sender` for sender type restrictions

### 5. Event Listeners (`listener/PlayerEventListener.java`)
- Event handling patterns
- Configuration-driven feature toggles
- Player session tracking
- Cooldown management

### 6. Paginated GUIs (`gui/PlayerListGUI.java`)
- Extending `PaginatedGUI<T>`
- Custom item rendering with `ItemBuilder`
- Navigation buttons (previous/next)
- Filtering and sorting
- Click handling
- Empty state display

## Project Structure

```
basic-plugin/
├── build.gradle.kts              # Build configuration
├── README.md                     # This file
└── src/main/
    ├── java/net/pcxnetwork/example/
    │   ├── BasicPlugin.java      # Main plugin class
    │   ├── package-info.java     # Package documentation
    │   ├── config/
    │   │   └── BasicPluginConfig.java
    │   ├── command/
    │   │   └── BasicCommand.java
    │   ├── listener/
    │   │   └── PlayerEventListener.java
    │   ├── module/
    │   │   └── WelcomeModule.java
    │   └── gui/
    │       └── PlayerListGUI.java
    └── resources/
        ├── plugin.yml            # Plugin descriptor
        └── config.yml            # Default configuration
```

## Building

```bash
# From the UnifiedPlugin-API root directory
./gradlew :examples:basic-plugin:shadowJar

# The JAR will be at:
# examples/basic-plugin/build/libs/BasicPlugin-1.0.0.jar
```

## Installation

1. Build the plugin using the command above
2. Ensure `UnifiedPluginAPI.jar` is in your server's `plugins/` folder
3. Copy `BasicPlugin-1.0.0.jar` to your server's `plugins/` folder
4. Start/restart the server

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/basic` | `basicplugin.use` | Shows plugin info |
| `/basic help` | `basicplugin.use` | Shows command help |
| `/basic info` | `basicplugin.use` | Shows plugin information |
| `/basic reload` | `basicplugin.admin.reload` | Reloads configuration |
| `/basic debug [on\|off]` | `basicplugin.admin.debug` | Toggles debug mode |
| `/basic message <player> <message>` | `basicplugin.message` | Sends a private message |
| `/basic gui` | `basicplugin.gui` | Opens player list GUI |
| `/basic give <player> <amount>` | `basicplugin.admin.give` | Example give command |
| `/basic admin stats` | `basicplugin.admin` | Shows server statistics |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `basicplugin.*` | op | All permissions |
| `basicplugin.use` | true | Basic command access |
| `basicplugin.gui` | true | GUI access |
| `basicplugin.message` | true | Private messaging |
| `basicplugin.admin.*` | op | All admin permissions |
| `basicplugin.admin.reload` | op | Reload configuration |
| `basicplugin.admin.debug` | op | Toggle debug mode |
| `basicplugin.admin.give` | op | Give command |

## Configuration

The default configuration file (`config.yml`) includes:

- **Debug mode**: Toggle verbose logging
- **Database settings**: MySQL/PostgreSQL connection
- **Messages**: Customizable MiniMessage formatted text
- **Features**: Toggle individual plugin features
- **GUI settings**: Customize GUI behavior

See `src/main/resources/config.yml` for the full documented configuration.

## Dependencies

- **Required**: UnifiedPluginAPI
- **Optional**: Vault, PlaceholderAPI, LuckPerms

## License

MIT License - Copyright (c) 2025 Supatuck
