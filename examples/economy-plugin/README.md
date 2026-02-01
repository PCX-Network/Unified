# Economy Plugin Example

A comprehensive example plugin demonstrating the UnifiedPlugin-API economy, database, commands, and GUI features.

## Features Demonstrated

### 1. Multi-Currency Economy System
- Support for multiple currencies (Dollars, Coins, Gems)
- Configurable currency properties (symbol, decimals, formatting)
- Starting balances and balance limits
- Currency-specific operations

### 2. Database Storage
- SQL storage with HikariCP connection pooling
- Support for SQLite, MySQL, MariaDB, and PostgreSQL
- Async database operations
- Transaction history logging
- Automatic table creation and schema management

### 3. Annotation-Based Commands
- `/balance` - View your balance or another player's balance
- `/pay` - Send money to another player
- `/baltop` - View the richest players leaderboard
- `/eco` - Admin commands (give, take, set, reset, stats)

### 4. Interactive GUI
- Stateful GUI with multiple views
- Balance display with all currencies
- Leaderboard view with pagination
- Transfer interface
- Transaction history view

### 5. Configuration Management
- YAML configuration with Sponge Configurate
- Type-safe config classes with annotations
- Hot-reload support
- Environment-specific settings

## Project Structure

```
economy-plugin/
├── build.gradle.kts              # Gradle build configuration
├── src/main/java/sh/pcx/examples/economy/
│   ├── EconomyExamplePlugin.java # Main plugin class
│   ├── commands/
│   │   ├── BalanceCommand.java   # /balance command
│   │   ├── PayCommand.java       # /pay command
│   │   ├── BalanceTopCommand.java # /baltop command
│   │   └── EconomyAdminCommand.java # /eco admin commands
│   ├── config/
│   │   └── EconomyConfig.java    # Configuration classes
│   ├── gui/
│   │   └── EconomyGUI.java       # Interactive economy GUI
│   ├── module/
│   │   └── EconomyModule.java    # Economy module with lifecycle
│   └── storage/
│       └── SQLEconomyStorage.java # Database storage implementation
└── src/main/resources/
    ├── plugin.yml                # Plugin descriptor
    └── config.yml                # Default configuration
```

## API Usage Examples

### Economy Service

```java
// Get economy service
EconomyService economy = plugin.getServices().get(EconomyService.class);

// Check balance
BigDecimal balance = economy.getBalance(player.getUniqueId());

// Transfer money
TransactionResult result = economy.transfer(
    sender.getUniqueId(),
    recipient.getUniqueId(),
    BigDecimal.valueOf(100),
    "Payment for items"
);

if (result.isSuccess()) {
    sender.sendMessage("Transfer successful!");
}
```

### Multi-Currency

```java
// Get a specific currency
Optional<Currency> gems = economy.getCurrency("gems");

// Deposit to specific currency
economy.deposit(player.getUniqueId(), BigDecimal.valueOf(50), "gems", "Reward");

// Check balance in specific currency
BigDecimal gemBalance = economy.getBalance(player.getUniqueId(), "gems");
```

### Database Storage

```java
// Save account
storage.saveAccount(account).thenAccept(v -> {
    logger.info("Account saved");
});

// Get top balances
storage.getTopBalances("dollars", 10).thenAccept(entries -> {
    for (BalanceEntry entry : entries) {
        logger.info(entry.playerName() + ": " + entry.balance());
    }
});
```

### Command Annotations

```java
@Command(
    name = "balance",
    aliases = {"bal", "money"},
    permission = "economy.balance"
)
public class BalanceCommand {

    @Default
    public void showBalance(@Sender UnifiedPlayer player) {
        // Show player's balance
    }

    @Subcommand("player")
    @Permission("economy.balance.others")
    public void showOtherBalance(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target
    ) {
        // Show target's balance
    }
}
```

### GUI Building

```java
public class EconomyGUI implements StatefulGUI {

    private static final StateKey<Integer> PAGE = StateKey.ofInt("page");

    public Map<Integer, Button> build() {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(13, Button.builder()
                .item(() -> ItemBuilder.of("minecraft:gold_ingot")
                        .name(Component.text("Balance"))
                        .build())
                .onClick(ctx -> {
                    ctx.sendMessage("You clicked!");
                })
                .build());

        return buttons;
    }
}
```

## Configuration

The plugin uses a comprehensive configuration file supporting:

- Database connection settings (SQLite, MySQL, etc.)
- Connection pool configuration
- Multiple currency definitions
- Economy settings (fees, limits, etc.)
- Customizable messages with MiniMessage format

See `config.yml` for the full configuration options.

## Building

```bash
./gradlew :examples:economy-plugin:build
```

The plugin JAR will be created in `build/libs/`.

## Dependencies

This example uses the following UnifiedPlugin-API modules:
- `unified-api` - Core plugin API
- `unified-economy` - Economy system
- `unified-commands` - Command framework
- `unified-gui` - GUI framework
- `unified-data` - Database abstraction
- `unified-config` - Configuration management
- `unified-modules` - Module system

## License

MIT License - See the project root LICENSE file.
