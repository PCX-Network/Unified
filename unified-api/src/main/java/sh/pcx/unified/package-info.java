/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Core interfaces and classes for the UnifiedPlugin API.
 *
 * <p>This package contains the foundational components of the UnifiedPlugin API:
 * <ul>
 *   <li>{@link sh.pcx.unified.UnifiedPlugin} - Abstract base class for plugins</li>
 *   <li>{@link sh.pcx.unified.UnifiedAPI} - Static entry point for the API</li>
 *   <li>{@link sh.pcx.unified.PluginMeta} - Plugin metadata record</li>
 * </ul>
 *
 * <h2>Getting Started</h2>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *
 *     @Override
 *     public void onEnable() {
 *         // Your plugin logic here
 *         getLogger().info("Plugin enabled!");
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         // Cleanup logic here
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified;
