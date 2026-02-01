/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Lifecycle interfaces for module state management.
 *
 * <p>This package provides interfaces that modules can implement to hook
 * into various lifecycle events:
 *
 * <h2>Interfaces</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.lifecycle.Initializable} -
 *       Called once during module loading for setup</li>
 *   <li>{@link sh.pcx.unified.modules.lifecycle.Reloadable} -
 *       Called when the module should reload its configuration</li>
 *   <li>{@link sh.pcx.unified.modules.lifecycle.Healthy} -
 *       Called when server TPS changes (for performance-aware modules)</li>
 *   <li>{@link sh.pcx.unified.modules.lifecycle.Disableable} -
 *       Called when the module is disabled for cleanup</li>
 *   <li>{@link sh.pcx.unified.modules.lifecycle.Schedulable} -
 *       Returns scheduled tasks to register with the module</li>
 * </ul>
 *
 * <h2>State Enum</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.lifecycle.ModuleState} -
 *       Represents module states: UNLOADED, LOADING, ENABLED, DISABLED, FAILED</li>
 * </ul>
 *
 * <h2>Lifecycle Order</h2>
 * <ol>
 *   <li>Module class is instantiated</li>
 *   <li>Dependency injection is performed</li>
 *   <li>{@link sh.pcx.unified.modules.lifecycle.Initializable#init} is called</li>
 *   <li>Listeners and commands are registered</li>
 *   <li>Scheduled tasks are started</li>
 *   <li>Module state changes to ENABLED</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * public class BattlePassModule implements Initializable, Reloadable, Healthy, Disableable {
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         // One-time setup
 *     }
 *
 *     @Override
 *     public void reload(ModuleContext context) {
 *         // Reload configuration
 *     }
 *
 *     @Override
 *     public void ifUnhealthy(HealthContext context) {
 *         // Reduce load when TPS is low
 *     }
 *
 *     @Override
 *     public void ifBackToHealth(HealthContext context) {
 *         // Resume normal operation
 *     }
 *
 *     @Override
 *     public void onDisable(ModuleContext context) {
 *         // Cleanup resources
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
package sh.pcx.unified.modules.lifecycle;
