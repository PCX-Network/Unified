/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Hot reload functionality for automatic configuration updates.
 *
 * <p>This package provides file watching and automatic reloading of
 * configuration files when they are modified.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.reload.ConfigWatcher} -
 *       Watches files for changes</li>
 *   <li>{@link sh.pcx.unified.config.reload.ReloadHandler} -
 *       Callback for reload events</li>
 *   <li>{@link sh.pcx.unified.config.reload.ReloadResult} -
 *       Result of a reload operation</li>
 *   <li>{@link sh.pcx.unified.config.reload.ReloadableConfig} -
 *       Self-reloading configuration wrapper</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Create a watcher
 * ConfigWatcher watcher = new ConfigWatcher(configLoader);
 *
 * // Watch a file
 * watcher.watch(PluginConfig.class, configPath, result -> {
 *     if (result.isSuccess()) {
 *         this.config = result.get();
 *         logger.info("Configuration reloaded!");
 *     } else {
 *         logger.warning("Reload failed: " + result.getErrorMessage());
 *     }
 * });
 *
 * // Or use ReloadableConfig
 * ReloadableConfig<PluginConfig> config = ReloadableConfig.create(
 *     PluginConfig.class, configPath, configLoader
 * );
 * config.enableHotReload();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.config.reload;
