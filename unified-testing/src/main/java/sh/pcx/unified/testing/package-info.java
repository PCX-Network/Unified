/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * UnifiedPlugin API Testing Framework.
 *
 * <p>This package provides comprehensive testing utilities for plugin development,
 * inspired by MockBukkit with full UnifiedPlugin API integration.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.testing.server.MockServer} - Full mock server simulation</li>
 *   <li>{@link sh.pcx.unified.testing.player.MockPlayer} - Simulated players with all operations</li>
 *   <li>{@link sh.pcx.unified.testing.world.MockWorld} - Test worlds with block manipulation</li>
 *   <li>{@link sh.pcx.unified.testing.inventory.MockInventory} - Inventory testing utilities</li>
 *   <li>{@link sh.pcx.unified.testing.event.EventCollector} - Event capture and assertion</li>
 *   <li>{@link sh.pcx.unified.testing.command.CommandResult} - Command execution testing</li>
 *   <li>{@link sh.pcx.unified.testing.scheduler.MockScheduler} - Tick control utilities</li>
 *   <li>{@link sh.pcx.unified.testing.database.MockDatabase} - In-memory database testing</li>
 *   <li>{@link sh.pcx.unified.testing.service.MockServiceRegistry} - Service mocking</li>
 *   <li>{@link sh.pcx.unified.testing.annotation} - Test annotations for JUnit 5</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * class MyPluginTest {
 *     private static MockServer server;
 *     private static MyPlugin plugin;
 *
 *     @BeforeAll
 *     static void setup() {
 *         server = MockServer.start();
 *         plugin = server.loadPlugin(MyPlugin.class);
 *     }
 *
 *     @AfterAll
 *     static void teardown() {
 *         server.stop();
 *     }
 *
 *     @Test
 *     void testPlayerJoin() {
 *         MockPlayer player = server.addPlayer("TestPlayer");
 *         assertThat(player.getMessages()).anyMatch(msg -> msg.contains("Welcome"));
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.testing;
