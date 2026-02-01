# Testing Guide

The UnifiedPlugin testing framework provides MockServer, MockPlayer, and test annotations to unit test your plugins without a real Minecraft server.

## Table of Contents

- [Overview](#overview)
- [MockServer](#mockserver)
- [MockPlayer](#mockplayer)
- [Test Annotations](#test-annotations)
- [Testing Commands](#testing-commands)
- [Testing Events](#testing-events)
- [Testing Database Operations](#testing-database-operations)
- [Assertions](#assertions)
- [Best Practices](#best-practices)

---

## Overview

### Key Features

- **Full server simulation** without Minecraft dependencies
- **Player management** with message/action tracking
- **Event system** with collection and verification
- **Command execution** with result verification
- **Tick control** for time-based testing
- **Service mocking** for dependency isolation
- **Database mocking** for data layer testing

### Maven/Gradle Setup

```xml
<dependency>
    <groupId>sh.pcx</groupId>
    <artifactId>unified-testing</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.2</version>
    <scope>test</scope>
</dependency>
```

---

## MockServer

### Starting the Server

```java
import sh.pcx.unified.testing.server.MockServer;

class MyPluginTest {

    private MockServer server;

    @BeforeEach
    void setUp() {
        server = MockServer.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void testServerStarts() {
        assertThat(server.getName()).isEqualTo("MockServer");
        assertThat(server.getOnlinePlayerCount()).isEqualTo(0);
    }
}
```

### Server Configuration

```java
import sh.pcx.unified.testing.server.MockServerConfiguration;
import sh.pcx.unified.server.MinecraftVersion;

MockServer server = MockServer.start(MockServerConfiguration.builder()
    .minecraftVersion(MinecraftVersion.of(1, 21, 0))
    .maxPlayers(100)
    .onlineMode(true)
    .build());
```

### Loading Plugins

```java
@Test
void testPluginLoads() {
    // Load your plugin
    MyPlugin plugin = server.loadPlugin(MyPlugin.class);

    assertThat(plugin).isNotNull();
    assertThat(plugin.isEnabled()).isTrue();
}

@Test
void testPluginWithDependencies() {
    // Load dependency first
    EconomyPlugin economy = server.loadPlugin(EconomyPlugin.class);

    // Then load your plugin
    MyPlugin plugin = server.loadPlugin(MyPlugin.class);

    // Verify interaction
    assertThat(plugin.getEconomyService()).isNotNull();
}
```

### World Management

```java
@Test
void testWorldCreation() {
    // Default world is created automatically
    MockWorld defaultWorld = (MockWorld) server.getDefaultWorld();
    assertThat(defaultWorld.getName()).isEqualTo("world");

    // Create additional worlds
    MockWorld nether = server.createWorld("world_nether", UnifiedWorld.Environment.NETHER);
    MockWorld end = server.createWorld("world_the_end", UnifiedWorld.Environment.THE_END);

    assertThat(server.getWorlds()).hasSize(3);
}
```

### Tick Control

```java
@Test
void testScheduledTask() {
    MyPlugin plugin = server.loadPlugin(MyPlugin.class);

    // Verify initial state
    assertThat(plugin.getTaskCounter()).isEqualTo(0);

    // Advance 20 ticks (1 second)
    server.advanceTicks(20);

    // Verify task ran
    assertThat(plugin.getTaskCounter()).isEqualTo(1);
}

@Test
void testDelayedAction() {
    MockPlayer player = server.addPlayer("Steve");

    plugin.scheduleTeleport(player, 60);  // 3 second delay

    // Advance 2 seconds - shouldn't teleport yet
    server.advanceSeconds(2);
    assertThat(player.getLocation()).isEqualTo(originalLocation);

    // Advance past delay - should teleport
    server.advanceSeconds(2);
    assertThat(player.getLocation()).isEqualTo(targetLocation);
}

@Test
void testAsyncOperations() {
    // Start async operation
    plugin.loadDataAsync(uuid);

    // Wait for async tasks to complete
    server.waitForAsyncTasks();

    // Verify result
    assertThat(plugin.getData(uuid)).isNotNull();
}
```

### Resetting Between Tests

```java
@Test
void testWithCleanState() {
    // Reset clears players, events, tasks
    server.reset();

    assertThat(server.getOnlinePlayerCount()).isEqualTo(0);
    assertThat(server.getCurrentTick()).isEqualTo(0);
}
```

---

## MockPlayer

### Creating Players

```java
@Test
void testPlayerCreation() {
    // Simple creation
    MockPlayer steve = server.addPlayer("Steve");

    assertThat(steve.getName()).isEqualTo("Steve");
    assertThat(steve.isOnline()).isTrue();
    assertThat(steve.getWorld()).isEqualTo(server.getDefaultWorld());
}

@Test
void testPlayerWithUUID() {
    UUID uuid = UUID.randomUUID();
    MockPlayer player = server.addPlayer("Steve", uuid);

    assertThat(player.getUniqueId()).isEqualTo(uuid);
}

@Test
void testPlayerWithConfiguration() {
    MockPlayer admin = server.addPlayer(builder -> builder
        .name("Admin")
        .op(true)
        .gameMode(GameMode.CREATIVE)
        .health(20.0)
        .foodLevel(20)
        .withPermission("admin.all")
        .withPermission("moderator.kick")
    );

    assertThat(admin.isOp()).isTrue();
    assertThat(admin.getGameMode()).isEqualTo(GameMode.CREATIVE);
    assertThat(admin.hasPermission("admin.all")).isTrue();
}
```

### Player State

```java
@Test
void testPlayerState() {
    MockPlayer player = server.addPlayer("Steve");

    // Set state
    player.setHealth(10.0);
    player.setFoodLevel(15);
    player.setLevel(5);
    player.setExp(0.5f);

    // Verify
    assertThat(player.getHealth()).isEqualTo(10.0);
    assertThat(player.getFoodLevel()).isEqualTo(15);
    assertThat(player.getLevel()).isEqualTo(5);
}

@Test
void testPlayerPermissions() {
    MockPlayer player = server.addPlayer("Steve");

    // Add permissions
    player.addPermission("myplugin.use");
    player.addPermission("myplugin.admin");

    assertThat(player.hasPermission("myplugin.use")).isTrue();
    assertThat(player.hasPermission("myplugin.admin")).isTrue();
    assertThat(player.hasPermission("other.permission")).isFalse();

    // Remove permission
    player.removePermission("myplugin.admin");
    assertThat(player.hasPermission("myplugin.admin")).isFalse();
}
```

### Message Verification

```java
@Test
void testPlayerMessages() {
    MockPlayer player = server.addPlayer("Steve");

    // Trigger action that sends messages
    plugin.greetPlayer(player);

    // Verify messages
    assertThat(player.getMessages())
        .hasSize(1)
        .first()
        .extracting(Component::toString)
        .asString()
        .contains("Welcome");

    // Get raw messages as strings
    assertThat(player.getRawMessages()).contains("Welcome, Steve!");

    // Get last message
    Optional<Component> lastMessage = player.getLastMessage();
    assertThat(lastMessage).isPresent();
}

@Test
void testActionBar() {
    MockPlayer player = server.addPlayer("Steve");

    plugin.showActionBar(player, "Health: 100%");

    assertThat(player.getActionBar()).isNotNull();
}

@Test
void testTitle() {
    MockPlayer player = server.addPlayer("Steve");

    plugin.showTitle(player, "Welcome!", "Enjoy your stay");

    Title title = player.getLastTitle();
    assertThat(title).isNotNull();
}
```

### Player Actions

```java
@Test
void testPlayerChat() {
    MockPlayer player = server.addPlayer("Steve");

    player.chat("Hello everyone!");

    // Verify chat event was fired
    // (depends on your event collection setup)
}

@Test
void testPlayerCommand() {
    MockPlayer player = server.addPlayer("Steve");

    boolean success = player.performCommand("spawn");

    assertThat(success).isTrue();
    assertThat(player.getMessages()).contains("Teleported to spawn!");
}

@Test
void testPlayerCommandWithResult() {
    MockPlayer player = server.addPlayer("Steve");

    CommandResult result = player.performCommandWithResult("teleport Steve");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getOutput()).contains("Teleported");
}

@Test
void testPlayerInteraction() {
    MockPlayer player = server.addPlayer("Steve");
    MockBlock block = server.getWorld("world").getBlockAt(0, 64, 0);

    // Simulate interactions
    player.leftClick(block);
    player.rightClick(block);
}
```

### Player Disconnect

```java
@Test
void testPlayerDisconnect() {
    MockPlayer player = server.addPlayer("Steve");

    player.disconnect();

    assertThat(player.isOnline()).isFalse();
    assertThat(server.getOnlinePlayerCount()).isEqualTo(0);
}

@Test
void testPlayerKick() {
    MockPlayer player = server.addPlayer("Steve");

    player.kick(Component.text("You have been kicked!"));

    assertThat(player.isOnline()).isFalse();
    assertThat(player.getMessages()).anyMatch(msg ->
        msg.toString().contains("kicked"));
}
```

---

## Test Annotations

### @UnifiedTest

```java
import sh.pcx.unified.testing.annotation.UnifiedTest;

@UnifiedTest(plugins = {MyPlugin.class})
class MyPluginTest {

    @Inject
    MockServer server;

    @Inject
    MyPlugin plugin;

    @Test
    void testSomething() {
        MockPlayer player = server.addPlayer("Steve");
        assertThat(player).isNotNull();
    }
}
```

### @WithPlayer

```java
@UnifiedTest
class PlayerTest {

    @Test
    @WithPlayer("Steve")
    void testPlayer(MockPlayer steve) {
        assertThat(steve.getName()).isEqualTo("Steve");
    }

    @Test
    @WithPlayer(name = "Admin", op = true)
    @WithPermission("myplugin.admin")
    void testAdminPlayer(MockPlayer admin) {
        assertThat(admin.isOp()).isTrue();
        assertThat(admin.hasPermission("myplugin.admin")).isTrue();
    }

    @Test
    @WithPlayer(name = "Player1")
    @WithPlayer(name = "Player2")
    void testMultiplePlayers(MockPlayer player1, MockPlayer player2) {
        assertThat(player1.getName()).isEqualTo("Player1");
        assertThat(player2.getName()).isEqualTo("Player2");
    }
}
```

### @WithPermission

```java
@Test
@WithPlayer("Steve")
@WithPermission("myplugin.admin")
@WithPermission("myplugin.moderator")
void testWithPermissions(MockPlayer steve) {
    assertThat(steve.hasPermission("myplugin.admin")).isTrue();
    assertThat(steve.hasPermission("myplugin.moderator")).isTrue();
}
```

### @ServerTest

```java
import sh.pcx.unified.testing.annotation.ServerTest;

@ServerTest
class ServerLevelTest {

    @Test
    void testServerProperties(MockServer server) {
        assertThat(server.getMaxPlayers()).isEqualTo(20);
        assertThat(server.getTPS()).isEqualTo(20.0);
    }
}
```

### @WorldTest

```java
import sh.pcx.unified.testing.annotation.WorldTest;

@WorldTest(name = "test_world", environment = Environment.NORMAL)
class WorldTest {

    @Test
    void testWorld(MockWorld world) {
        assertThat(world.getName()).isEqualTo("test_world");
    }
}
```

### @IntegrationTest

```java
import sh.pcx.unified.testing.annotation.IntegrationTest;

@IntegrationTest
class FullIntegrationTest {
    // More complex test setup
    // May take longer to run
}
```

---

## Testing Commands

### Basic Command Testing

```java
@UnifiedTest(plugins = {MyPlugin.class})
class CommandTest {

    @Inject MockServer server;
    @Inject MyPlugin plugin;

    @Test
    @WithPlayer("Steve")
    void testSpawnCommand(MockPlayer player) {
        // Execute command
        boolean success = player.performCommand("spawn");

        // Verify
        assertThat(success).isTrue();
        assertThat(player.getMessages())
            .anyMatch(msg -> msg.toString().contains("Teleported"));
    }

    @Test
    @WithPlayer(name = "Admin", op = true)
    void testAdminCommand(MockPlayer admin) {
        CommandResult result = admin.performCommandWithResult("gamemode creative");

        assertThat(result.isSuccess()).isTrue();
        assertThat(admin.getGameMode()).isEqualTo(GameMode.CREATIVE);
    }

    @Test
    @WithPlayer("Steve")
    void testCommandWithoutPermission(MockPlayer player) {
        CommandResult result = player.performCommandWithResult("admin reset");

        assertThat(result.isSuccess()).isFalse();
        assertThat(player.getMessages())
            .anyMatch(msg -> msg.toString().contains("permission"));
    }
}
```

### Tab Completion Testing

```java
@Test
void testTabCompletion() {
    MockPlayer player = server.addPlayer("Steve");
    server.addPlayer("Alex");
    server.addPlayer("Admin");

    List<String> completions = server.tabComplete(player, "teleport A");

    assertThat(completions).containsExactlyInAnyOrder("Alex", "Admin");
}
```

---

## Testing Events

### Event Collection

```java
import sh.pcx.unified.testing.event.EventCollector;

@Test
void testEventFiring() {
    // Create event collector
    EventCollector<PlayerJoinEvent> joinEvents = server.collectEvents(PlayerJoinEvent.class);

    // Trigger event
    MockPlayer player = server.addPlayer("Steve");

    // Verify
    assertThat(joinEvents.getEvents()).hasSize(1);
    assertThat(joinEvents.getEvents().get(0).getPlayer().getName()).isEqualTo("Steve");
}

@Test
void testEventCancellation() {
    EventCollector<PlayerMoveEvent> moveEvents = server.collectEvents(PlayerMoveEvent.class);

    // Register listener that cancels events
    plugin.registerMoveBlocker();

    MockPlayer player = server.addPlayer("Steve");
    player.move(new Location(world, 10, 64, 10));

    // Verify event was cancelled
    assertThat(moveEvents.getEvents())
        .first()
        .extracting(PlayerMoveEvent::isCancelled)
        .isEqualTo(true);
}
```

### Custom Event Testing

```java
@Test
void testCustomEvent() {
    EventCollector<ShopPurchaseEvent> purchaseEvents =
        server.collectEvents(ShopPurchaseEvent.class);

    MockPlayer player = server.addPlayer("Steve");
    player.addPermission("shop.buy");

    // Trigger purchase
    plugin.getShop().purchase(player, "diamond", 1);

    // Verify custom event
    assertThat(purchaseEvents.getEvents()).hasSize(1);
    ShopPurchaseEvent event = purchaseEvents.getEvents().get(0);
    assertThat(event.getItem()).isEqualTo("diamond");
    assertThat(event.getQuantity()).isEqualTo(1);
}
```

---

## Testing Database Operations

### MockDatabase

```java
import sh.pcx.unified.testing.database.MockDatabase;

@Test
void testDatabaseOperations() {
    MockDatabase db = server.getMockDatabase();

    // Insert test data
    db.insert("players", Map.of(
        "uuid", uuid.toString(),
        "name", "Steve",
        "balance", 1000.0
    ));

    // Query
    Optional<Map<String, Object>> result = db.findOne("players",
        row -> row.get("uuid").equals(uuid.toString()));

    assertThat(result).isPresent();
    assertThat(result.get().get("name")).isEqualTo("Steve");
}
```

### Service Mocking

```java
@Test
void testWithMockedService() {
    // Create mock service
    EconomyService mockEconomy = server.mockService(EconomyService.class);

    // Configure mock behavior
    when(mockEconomy.getBalance(any())).thenReturn(1000.0);
    when(mockEconomy.withdraw(any(), anyDouble())).thenReturn(true);

    // Register mock
    server.registerMockService(EconomyService.class, mockEconomy);

    // Test
    MockPlayer player = server.addPlayer("Steve");
    plugin.purchaseItem(player, "diamond");

    // Verify interaction
    verify(mockEconomy).withdraw(player.getUniqueId(), 100.0);
}
```

---

## Assertions

### PlayerAssertions

```java
import static sh.pcx.unified.testing.assertion.PlayerAssertions.*;

@Test
void testPlayerAssertions() {
    MockPlayer player = server.addPlayer("Steve");

    assertThat(player)
        .isOnline()
        .hasName("Steve")
        .hasHealth(20.0)
        .hasPermission("basic.permission")
        .receivedMessage("Welcome")
        .isInWorld("world")
        .hasGameMode(GameMode.SURVIVAL);
}
```

### Fluent Assertions

```java
@Test
void testFluentAssertions() {
    MockPlayer player = server.addPlayer("Steve");

    // Messages
    assertThat(player.getMessages())
        .hasSize(1)
        .first()
        .satisfies(msg -> {
            assertThat(msg.toString()).contains("Welcome");
        });

    // Inventory
    assertThat(player.getInventory().contains("minecraft:diamond")).isTrue();

    // Location
    assertThat(player.getLocation())
        .extracting("world", "x", "y", "z")
        .containsExactly(world, 0.0, 64.0, 0.0);
}
```

---

## Best Practices

### 1. Use @UnifiedTest for Most Tests

```java
@UnifiedTest(plugins = {MyPlugin.class}, resetBetweenTests = true)
class MyPluginTest {
    // Server is automatically managed
    // Plugin is loaded once
    // State resets between tests
}
```

### 2. Test One Thing Per Test

```java
// Good
@Test
void testPlayerReceivesWelcomeMessage() { }

@Test
void testPlayerGetsStartingItems() { }

// Bad
@Test
void testPlayerJoin() {
    // Tests welcome message AND starting items AND permissions
}
```

### 3. Use Descriptive Test Names

```java
@Test
void shouldTeleportPlayerToSpawnWhenSpawnCommandExecuted() { }

@Test
void shouldDenyAccessWhenPlayerLacksPermission() { }

@Test
void shouldDeductBalanceWhenPurchaseSucceeds() { }
```

### 4. Arrange-Act-Assert Pattern

```java
@Test
void testPurchase() {
    // Arrange
    MockPlayer player = server.addPlayer("Steve");
    player.addPermission("shop.buy");
    economy.setBalance(player.getUniqueId(), 1000);

    // Act
    boolean success = shop.purchase(player, "diamond", 1);

    // Assert
    assertThat(success).isTrue();
    assertThat(economy.getBalance(player.getUniqueId())).isEqualTo(900);
    assertThat(player.getInventory().contains("minecraft:diamond")).isTrue();
}
```

### 5. Clean Up Resources

```java
@AfterEach
void tearDown() {
    server.reset();  // Or server.stop() if starting fresh
}

@AfterAll
static void cleanup() {
    MockServer.getInstance().stop();
}
```

### 6. Test Edge Cases

```java
@Test
void testPurchaseWithExactBalance() { }

@Test
void testPurchaseWithInsufficientBalance() { }

@Test
void testPurchaseWithZeroQuantity() { }

@Test
void testPurchaseWithNegativeQuantity() { }
```

---

## Complete Test Example

```java
@UnifiedTest(plugins = {MyPlugin.class})
class EconomyIntegrationTest {

    @Inject MockServer server;
    @Inject MyPlugin plugin;

    private EconomyService economy;
    private ShopService shop;

    @BeforeEach
    void setUp() {
        economy = plugin.getEconomyService();
        shop = plugin.getShopService();
    }

    @Test
    @WithPlayer("Steve")
    void shouldAllowPurchaseWithSufficientBalance(MockPlayer player) {
        // Arrange
        economy.setBalance(player.getUniqueId(), 1000);

        // Act
        boolean success = shop.purchase(player, "diamond_sword", 1);

        // Assert
        assertThat(success).isTrue();
        assertThat(economy.getBalance(player.getUniqueId())).isEqualTo(900);
        assertThat(player.getInventory().contains("minecraft:diamond_sword")).isTrue();
        assertThat(player.getMessages())
            .anyMatch(msg -> msg.toString().contains("Purchased"));
    }

    @Test
    @WithPlayer("Steve")
    void shouldDenyPurchaseWithInsufficientBalance(MockPlayer player) {
        // Arrange
        economy.setBalance(player.getUniqueId(), 50);

        // Act
        boolean success = shop.purchase(player, "diamond_sword", 1);

        // Assert
        assertThat(success).isFalse();
        assertThat(economy.getBalance(player.getUniqueId())).isEqualTo(50);
        assertThat(player.getInventory().contains("minecraft:diamond_sword")).isFalse();
        assertThat(player.getMessages())
            .anyMatch(msg -> msg.toString().contains("Not enough"));
    }

    @Test
    @WithPlayer(name = "Admin", op = true)
    void shouldAllowAdminToGiveItems(MockPlayer admin) {
        MockPlayer target = server.addPlayer("Steve");

        // Act
        CommandResult result = admin.performCommandWithResult("give Steve diamond 64");

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(target.getInventory().getItemCount("minecraft:diamond")).isEqualTo(64);
    }

    @Test
    void shouldPersistBalanceAcrossReconnect() {
        // Arrange
        MockPlayer player = server.addPlayer("Steve");
        economy.setBalance(player.getUniqueId(), 5000);

        // Act - simulate disconnect and reconnect
        UUID uuid = player.getUniqueId();
        player.disconnect();
        server.waitForAsyncTasks();  // Wait for save

        MockPlayer reconnected = server.addPlayer("Steve", uuid);

        // Assert
        assertThat(economy.getBalance(uuid)).isEqualTo(5000);
    }
}
```

---

## Summary

The UnifiedPlugin testing framework enables comprehensive unit and integration testing:

- **MockServer** simulates a full Minecraft server
- **MockPlayer** tracks messages, actions, and state
- **Test annotations** simplify setup and teardown
- **Event collection** verifies plugin behavior
- **Database mocking** isolates data layer testing

Use these tools to ensure your plugin works correctly before deployment.
