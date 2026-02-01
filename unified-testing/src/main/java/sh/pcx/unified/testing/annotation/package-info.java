/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Test annotations for JUnit 5 integration.
 *
 * <p>This package provides annotations for declarative test configuration
 * and automatic resource setup/teardown.
 *
 * <h2>Key Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.testing.annotation.UnifiedTest} - Main test annotation</li>
 *   <li>{@link sh.pcx.unified.testing.annotation.ServerTest} - Server-only test</li>
 *   <li>{@link sh.pcx.unified.testing.annotation.WithPlayer} - Auto-create player</li>
 *   <li>{@link sh.pcx.unified.testing.annotation.WithWorld} - Custom world config</li>
 *   <li>{@link sh.pcx.unified.testing.annotation.WithPermission} - Grant permission</li>
 *   <li>{@link sh.pcx.unified.testing.annotation.IntegrationTest} - Integration test marker</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @UnifiedTest(plugins = MyPlugin.class)
 * class MyPluginTest {
 *
 *     @Inject MockServer server;
 *     @Inject MyPlugin plugin;
 *
 *     @Test
 *     @WithPlayer("Steve")
 *     @WithPermission("myplugin.use")
 *     void testPlayerCommand(MockPlayer steve) {
 *         CommandResult result = steve.performCommandWithResult("mycommand");
 *         assertThat(result.isSuccess()).isTrue();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
package sh.pcx.unified.testing.annotation;
