# Upgrading Between UnifiedPlugin Versions

This guide covers upgrading your plugin between versions of the UnifiedPlugin API. Each section documents breaking changes, deprecations, and migration steps.

## Table of Contents

1. [Versioning Policy](#versioning-policy)
2. [Upgrade Process](#upgrade-process)
3. [Version 1.x to 2.x](#version-1x-to-2x)
4. [Version 0.x to 1.x](#version-0x-to-1x)
5. [Deprecation Handling](#deprecation-handling)
6. [Compatibility Matrix](#compatibility-matrix)
7. [Common Upgrade Issues](#common-upgrade-issues)

---

## Versioning Policy

UnifiedPlugin follows [Semantic Versioning](https://semver.org/):

- **MAJOR (X.0.0)**: Breaking API changes, removal of deprecated features
- **MINOR (0.X.0)**: New features, deprecations (backward compatible)
- **PATCH (0.0.X)**: Bug fixes, performance improvements (backward compatible)

### Deprecation Timeline

1. **Minor version N**: Feature deprecated, `@Deprecated` annotation added
2. **Minor version N+1 to N+2**: Deprecated feature still works, warnings logged
3. **Major version N+1**: Deprecated feature removed

Always check deprecation warnings and migrate before major upgrades.

---

## Upgrade Process

### Step 1: Check Current Version

```kotlin
// build.gradle.kts
dependencies {
    compileOnly("sh.pcx:unified-api:1.0.0")  // Your current version
}
```

### Step 2: Review Changelog

Check the [CHANGELOG.md](../../CHANGELOG.md) for all versions between your current and target version.

### Step 3: Update Dependencies

```kotlin
dependencies {
    compileOnly("sh.pcx:unified-api:2.0.0")  // New version
}
```

### Step 4: Fix Compilation Errors

Address any compilation errors from removed or changed APIs.

### Step 5: Address Deprecation Warnings

Replace deprecated API usage with recommended alternatives.

### Step 6: Run Tests

Execute your test suite to verify behavior.

### Step 7: Test on Server

Deploy to a test server and verify all functionality.

---

## Version 1.x to 2.x

*Note: This section will be populated when version 2.0.0 is released. The following is a preview of expected changes.*

### Breaking Changes

#### 1. UnifiedPlugin Lifecycle Changes

**Before (1.x):**
```java
public class MyPlugin extends UnifiedPlugin {

    @Override
    public void onLoad() {
        // Called before onEnable
    }

    @Override
    public void onEnable() {
        // Main initialization
    }
}
```

**After (2.x):**
```java
public class MyPlugin extends UnifiedPlugin {

    @Override
    public void onLoad() {
        // Still called before onEnable
    }

    @Override
    public void onEnable() {
        // Main initialization
    }

    @Override
    public void onPostEnable() {
        // NEW: Called after all plugins are enabled
        // Safe to access other plugin APIs here
    }
}
```

#### 2. Service Registration Changes

**Before (1.x):**
```java
@Service
@Singleton
public class MyService implements Service {
    // Service marker interface required
}
```

**After (2.x):**
```java
@Service
@Singleton
public class MyService {
    // Service marker interface no longer required
    // @Service annotation is sufficient
}
```

**Migration:** Remove `implements Service` from your service classes. The interface is now optional.

#### 3. Event Priority Enum

**Before (1.x):**
```java
import sh.pcx.unified.event.EventPriority;

@EventHandler(priority = EventPriority.NORMAL)
public void onEvent(SomeEvent event) { }
```

**After (2.x):**
```java
import sh.pcx.unified.event.Priority;

@EventHandler(priority = Priority.NORMAL)
public void onEvent(SomeEvent event) { }
```

**Migration:** Replace `EventPriority` imports with `Priority`.

#### 4. Configuration Validation Annotations

**Before (1.x):**
```java
@ConfigSerializable
public class MyConfig {

    @ConfigValidate(Range.class)
    @Range(min = 1, max = 100)
    private int value;
}
```

**After (2.x):**
```java
@ConfigSerializable
public class MyConfig {

    @Range(min = 1, max = 100)  // @ConfigValidate no longer needed
    private int value;
}
```

**Migration:** Remove `@ConfigValidate` wrapper annotations.

#### 5. UnifiedPlayer.teleport() Signature

**Before (1.x):**
```java
CompletableFuture<Boolean> teleport(UnifiedLocation location);
```

**After (2.x):**
```java
CompletableFuture<TeleportResult> teleport(UnifiedLocation location);
```

```java
// New TeleportResult enum
public enum TeleportResult {
    SUCCESS,
    FAILED_CANCELLED,     // Event was cancelled
    FAILED_INVALID_WORLD, // World not loaded
    FAILED_UNSAFE,        // Destination unsafe
    FAILED_UNKNOWN        // Other failure
}
```

**Migration:**
```java
// Before
player.teleport(location).thenAccept(success -> {
    if (success) { /* ok */ }
});

// After
player.teleport(location).thenAccept(result -> {
    if (result == TeleportResult.SUCCESS) { /* ok */ }
    else if (result == TeleportResult.FAILED_CANCELLED) { /* cancelled */ }
});

// Or use convenience method
player.teleport(location).thenAccept(result -> {
    if (result.isSuccess()) { /* ok */ }
});
```

### Deprecated API Removals

The following APIs deprecated in 1.x are removed in 2.x:

| Removed API | Replacement |
|------------|-------------|
| `UnifiedServer.getOfflinePlayer(String name)` | `getOfflinePlayer(UUID uuid)` |
| `ConfigService.load(Path path)` | `load(Class<T> type, Path path)` |
| `SchedulerService.runTask(Plugin, Runnable)` | `runTask(Runnable)` |
| `ItemBuilder.setName(String)` | `name(Component)` |
| `UnifiedPlayer.sendMessage(String)` | `sendMessage(Component)` |

### New Features in 2.x

#### 1. Enhanced Module System

```java
@FeatureModule(
    id = "economy",
    description = "Economy features",
    dependencies = {"database"},
    optional = true
)
public class EconomyModule extends UnifiedModule {

    @Override
    protected void configure() {
        bind(EconomyService.class).to(EconomyServiceImpl.class);
    }

    @Override
    public boolean canEnable() {
        // Check if Vault is available
        return VaultHook.isAvailable();
    }
}
```

#### 2. Reactive Configuration

```java
@Service
public class MyService {

    @Inject
    @Reactive
    private Provider<MyConfig> config;

    public void doSomething() {
        // Always gets latest config after hot-reload
        MyConfig current = config.get();
    }
}
```

#### 3. Entity Component System

```java
@Component
public class CustomDataComponent implements EntityComponent {

    private int points;

    @Override
    public String getId() {
        return "myplugin:points";
    }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}

// Usage
player.getComponent(CustomDataComponent.class).ifPresent(comp -> {
    comp.setPoints(100);
});
```

---

## Version 0.x to 1.x

### Breaking Changes

#### 1. Package Restructuring

**Before (0.x):**
```java
import sh.pcx.unified.api.UnifiedPlugin;
import sh.pcx.unified.api.player.Player;
import sh.pcx.unified.api.server.Server;
```

**After (1.x):**
```java
import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.UnifiedServer;
```

**Migration:** Update all imports. Use IDE's "Organize Imports" feature after updating dependencies.

#### 2. API Entry Point

**Before (0.x):**
```java
Unified.getAPI().getServer();
Unified.getAPI().getPlayer(uuid);
```

**After (1.x):**
```java
UnifiedAPI.getServer();
UnifiedAPI.getPlayer(uuid);
```

#### 3. Command Annotations

**Before (0.x):**
```java
@CommandHandler("spawn")
public class SpawnCommand {

    @Execute
    public void run(@Player Player player) { }

    @SubCommand("set")
    public void setSpawn(@Player Player player) { }
}
```

**After (1.x):**
```java
@Command(name = "spawn")
public class SpawnCommand {

    @Default
    public void run(@Sender UnifiedPlayer player) { }

    @Subcommand("set")
    public void setSpawn(@Sender UnifiedPlayer player) { }
}
```

| Old Annotation | New Annotation |
|---------------|----------------|
| `@CommandHandler` | `@Command` |
| `@Execute` | `@Default` |
| `@SubCommand` | `@Subcommand` |
| `@Player` | `@Sender` |
| `@Argument` | `@Arg` |

#### 4. Event System

**Before (0.x):**
```java
@Listener
public class MyListener {

    @Handle(priority = Priority.NORMAL)
    public void onJoin(PlayerJoinEvent event) { }
}
```

**After (1.x):**
```java
@Service
public class MyListener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) { }
}
```

#### 5. Configuration System

**Before (0.x):**
```java
@Config("config.yml")
public class MyConfig {

    @Value("database.host")
    private String host;
}
```

**After (1.x):**
```java
@ConfigSerializable
public class MyConfig {

    private DatabaseConfig database = new DatabaseConfig();

    public DatabaseConfig getDatabase() { return database; }
}

@ConfigSerializable
public class DatabaseConfig {

    private String host = "localhost";

    public String getHost() { return host; }
}
```

#### 6. Scheduler API

**Before (0.x):**
```java
Unified.getScheduler().sync(() -> { });
Unified.getScheduler().async(() -> { });
Unified.getScheduler().later(20, () -> { });
```

**After (1.x):**
```java
@Inject
private SchedulerService scheduler;

scheduler.runTask(() -> { });
scheduler.runTaskAsync(() -> { });
scheduler.runTaskLater(() -> { }, 20);
```

#### 7. Dependency Injection

**Before (0.x):**
```java
// Manual service locator
MyService service = Unified.getService(MyService.class);
```

**After (1.x):**
```java
// Constructor injection (preferred)
@Service
public class MyComponent {

    private final MyService service;

    @Inject
    public MyComponent(MyService service) {
        this.service = service;
    }
}

// Or field injection
@Service
public class MyComponent {

    @Inject
    private MyService service;
}
```

### Migration Script

Here's a Bash script to help automate some migration steps:

```bash
#!/bin/bash
# migrate-0x-to-1x.sh

# Update package imports
find src -name "*.java" -exec sed -i \
    -e 's/net\.pcxnetwork\.unified\.api\./sh.pcx.unified./g' \
    -e 's/import net\.pcxnetwork\.unified\.api\.player\.Player;/import sh.pcx.unified.player.UnifiedPlayer;/g' \
    -e 's/import net\.pcxnetwork\.unified\.api\.server\.Server;/import sh.pcx.unified.server.UnifiedServer;/g' \
    {} \;

# Update annotations
find src -name "*.java" -exec sed -i \
    -e 's/@CommandHandler/@Command/g' \
    -e 's/@Execute/@Default/g' \
    -e 's/@SubCommand/@Subcommand/g' \
    -e 's/@Listener/@Service/g' \
    -e 's/@Handle/@EventHandler/g' \
    {} \;

# Update API calls
find src -name "*.java" -exec sed -i \
    -e 's/Unified\.getAPI()/UnifiedAPI.getInstance()/g' \
    -e 's/Unified\.getScheduler()/scheduler/g' \
    -e 's/Unified\.getService(/UnifiedAPI.getInstance().getService(/g' \
    {} \;

echo "Migration complete. Please review changes and fix any remaining issues."
```

---

## Deprecation Handling

### Finding Deprecated Usage

#### IDE Warnings

Most IDEs will show deprecation warnings. In IntelliJ IDEA:

1. Go to **Analyze > Inspect Code**
2. Select your project scope
3. Look for "Deprecated API usage" under "Code maturity"

#### Gradle Task

```kotlin
// build.gradle.kts
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}
```

Then run: `./gradlew compileJava`

### Suppressing Deprecation Warnings

If you must use deprecated APIs temporarily:

```java
@SuppressWarnings("deprecation")
public void legacyMethod() {
    // Using deprecated API intentionally
    player.sendMessage("Legacy string message");
}
```

### Documenting Migration Plans

Add TODO comments for deprecated usage:

```java
// TODO: Migrate to Component API before 2.0.0
@SuppressWarnings("deprecation")
player.sendMessage("Hello");  // Deprecated in 1.2.0
```

---

## Compatibility Matrix

### Minecraft Version Support

| UnifiedPlugin | Minecraft Versions | Java Version |
|--------------|-------------------|--------------|
| 2.0.x | 1.20.4 - 1.21.x | Java 21+ |
| 1.2.x | 1.19.4 - 1.21.x | Java 17+ |
| 1.1.x | 1.19.4 - 1.20.x | Java 17+ |
| 1.0.x | 1.18.2 - 1.20.x | Java 17+ |
| 0.9.x | 1.16.5 - 1.19.x | Java 16+ |

### Platform Support

| UnifiedPlugin | Paper | Folia | Spigot | Sponge |
|--------------|-------|-------|--------|--------|
| 2.0.x | Yes | Yes | Yes | API 10+ |
| 1.x | Yes | Yes | Yes | API 8+ |
| 0.9.x | Yes | Partial | Yes | API 7+ |

### Dependency Versions

| UnifiedPlugin | Adventure | Guice | Configurate |
|--------------|-----------|-------|-------------|
| 2.0.x | 4.16.0+ | 7.0.0+ | 4.2.0+ |
| 1.x | 4.14.0+ | 5.1.0+ | 4.1.2+ |
| 0.9.x | 4.11.0+ | 5.0.0+ | 4.0.0+ |

---

## Common Upgrade Issues

### 1. NoSuchMethodError

**Cause:** Runtime version mismatch between compile and runtime.

**Solution:**
```kotlin
// Ensure compile and runtime versions match
dependencies {
    compileOnly("sh.pcx:unified-api:1.2.0")
}

// Server should have matching UnifiedPlugin version installed
```

### 2. NoClassDefFoundError

**Cause:** Missing module dependency after upgrade.

**Solution:**
```kotlin
dependencies {
    // Add any new required modules
    compileOnly("sh.pcx:unified-api:1.2.0")
    compileOnly("sh.pcx:unified-scheduler:1.2.0")  // If using scheduler
}
```

### 3. ClassCastException in Events

**Cause:** Event class hierarchy changed.

**Solution:**
```java
// Check event type at runtime during transition
@EventHandler
public void onEvent(UnifiedEvent event) {
    if (event instanceof PlayerJoinEvent joinEvent) {
        // Safe cast
    }
}
```

### 4. Injection Failures

**Cause:** Service binding changes or new requirements.

**Solution:**
```java
// Ensure all dependencies are properly annotated
@Service
@Singleton
public class MyService {

    @Inject
    public MyService(SomeDependency dep) {
        // Constructor injection is most reliable
    }
}
```

### 5. Configuration Migration

**Cause:** Config structure changed between versions.

**Solution:** Implement config migration:

```java
@Service
public class ConfigMigrator {

    @Inject
    private ConfigService configService;

    @PostConstruct
    public void migrate() {
        Path configPath = plugin.getDataFolder().resolve("config.yml");

        // Check config version
        ConfigNode node = configService.getNode(configPath);
        int version = node.node("config-version").getInt(0);

        if (version < 2) {
            migrateV1ToV2(node);
        }
        if (version < 3) {
            migrateV2ToV3(node);
        }

        // Update version
        node.node("config-version").set(3);
        node.save();
    }

    private void migrateV1ToV2(ConfigNode node) {
        // Move database.host to connections.database.host
        String oldHost = node.node("database", "host").getString();
        if (oldHost != null) {
            node.node("connections", "database", "host").set(oldHost);
            node.node("database").set(null);  // Remove old
        }
    }
}
```

### 6. Scheduler Task Cancellation

**Cause:** TaskHandle API changes.

**Solution:**
```java
// Before: Stored BukkitTask
private BukkitTask task;

// After: Store TaskHandle
private TaskHandle task;

public void stopTask() {
    if (task != null && !task.isCancelled()) {
        task.cancel();
        task = null;
    }
}
```

### 7. Service Scope Changes

**Cause:** Default scope changed between versions.

**Solution:** Always explicitly declare scope:

```java
@Service
@Singleton  // Explicit scope
public class MySingletonService { }

@Service
@PlayerScoped  // Explicit scope
public class MyPlayerService { }
```

---

## Pre-Upgrade Checklist

Before upgrading to a new major version:

- [ ] Read the full changelog for all intermediate versions
- [ ] Backup your plugin source code
- [ ] Run deprecation check and document all deprecated usage
- [ ] Create a test environment with the new version
- [ ] Update build dependencies to new version
- [ ] Fix all compilation errors
- [ ] Address all deprecation warnings
- [ ] Run full test suite
- [ ] Test manually on each supported platform
- [ ] Test configuration migration from old configs
- [ ] Verify all commands work correctly
- [ ] Verify all events fire correctly
- [ ] Check scheduler tasks run properly
- [ ] Monitor for runtime errors in logs
- [ ] Deploy to staging server before production

---

## Getting Help

If you encounter issues during upgrade:

1. **Check Documentation:** Review the [API docs](../README.md) for the new version
2. **Search Issues:** Look for similar issues on [GitHub](https://github.com/PCX-Network/UnifiedPlugin-API/issues)
3. **Ask Community:** Join our [Discord](https://discord.pcxnetwork.net) for help
4. **Report Bugs:** File an issue if you find a bug in the migration path

When reporting upgrade issues, include:
- Source and target versions
- Full stack trace
- Minimal reproduction code
- Server type and Minecraft version
