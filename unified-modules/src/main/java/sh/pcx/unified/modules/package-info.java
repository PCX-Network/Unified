/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Modularize-style module system for UnifiedPlugin API.
 *
 * <p>This package provides a comprehensive module system that allows plugins
 * to be built as collections of independent, hot-swappable modules. Inspired
 * by <a href="https://github.com/jbwm/Modularize">Modularize</a> with extended
 * functionality.
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>Module</b>: A self-contained feature unit with its own listeners,
 *       commands, and lifecycle</li>
 *   <li><b>ModuleManager</b>: Central registry that handles loading, enabling,
 *       and dependency resolution</li>
 *   <li><b>Lifecycle Interfaces</b>: Optional interfaces for initialization,
 *       reload, and health monitoring</li>
 *   <li><b>Auto-Registration</b>: Automatic listener and command registration
 *       via annotations</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.annotation} - Annotations for
 *       defining modules (@Module, @Listen, @Command, @Configurable)</li>
 *   <li>{@link sh.pcx.unified.modules.core} - Core module system
 *       (ModuleManager, ModuleRegistry, ModuleLoader, ModuleContext)</li>
 *   <li>{@link sh.pcx.unified.modules.lifecycle} - Lifecycle interfaces
 *       (Initializable, Reloadable, Healthy, Disableable, Schedulable)</li>
 *   <li>{@link sh.pcx.unified.modules.health} - Health monitoring
 *       (HealthCheck, HealthStatus, TPSTracker)</li>
 *   <li>{@link sh.pcx.unified.modules.dependency} - Dependency resolution
 *       (DependencyResolver, DependencyGraph, CircularDependencyException)</li>
 *   <li>{@link sh.pcx.unified.modules.admin} - Admin commands and tools
 *       (ModuleCommands, ModuleInfo, HotReloader)</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // 1. Create a module
 * @Module(name = "Economy")
 * @Listen
 * @Command
 * public class EconomyModule implements Listener, TabExecutor, Initializable {
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         context.getLogger().info("Economy module initialized!");
 *     }
 *
 *     @EventHandler
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         // Handle event
 *     }
 *
 *     @Override
 *     public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
 *         return true;
 *     }
 * }
 *
 * // 2. Set up module manager in your plugin
 * public class MyPlugin extends UnifiedPlugin {
 *
 *     private ModuleManager modules;
 *
 *     @Override
 *     public void onEnable() {
 *         modules = ModuleManager.builder(this)
 *             .scanPackage("com.example.myplugin.modules")
 *             .enableHealthMonitoring(true)
 *             .healthThreshold(18.0)
 *             .build();
 *
 *         modules.registerAll();
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         modules.disableAll();
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
package sh.pcx.unified.modules;
