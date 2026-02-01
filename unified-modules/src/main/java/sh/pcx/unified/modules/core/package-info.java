/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Core module system components for managing module lifecycle.
 *
 * <p>This package provides the central classes for module management:
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.core.ModuleManager} -
 *       Central manager for the module system lifecycle. Handles discovery,
 *       dependency resolution, loading, enabling, disabling, and reloading</li>
 *   <li>{@link sh.pcx.unified.modules.core.ModuleRegistry} -
 *       Registry that stores and manages registered modules with thread-safe access</li>
 *   <li>{@link sh.pcx.unified.modules.core.ModuleLoader} -
 *       Loads modules from classes and packages, supports package scanning</li>
 *   <li>{@link sh.pcx.unified.modules.core.ModuleContext} -
 *       Context passed to modules during lifecycle events providing access to services</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create module manager
 * ModuleManager modules = ModuleManager.builder(plugin)
 *     .scanPackage("com.example.myplugin.modules")
 *     .enableHealthMonitoring(true)
 *     .healthThreshold(18.0)
 *     .configPath(plugin.getDataFolder().toPath().resolve("modules.yml"))
 *     .build();
 *
 * // Register all discovered modules
 * modules.registerAll();
 *
 * // Access module instances
 * EconomyModule economy = modules.get(EconomyModule.class);
 *
 * // Check module status
 * if (modules.isEnabled("BattlePass")) {
 *     // Do something
 * }
 *
 * // Runtime control
 * modules.disable("BattlePass");
 * modules.enable("BattlePass");
 * modules.reload("BattlePass");
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
package sh.pcx.unified.modules.core;
