/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Administration tools and commands for module management.
 *
 * <p>This package provides components for runtime module administration:
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.admin.ModuleCommands} -
 *       Command logic for /modules admin commands</li>
 *   <li>{@link sh.pcx.unified.modules.admin.ModuleInfo} -
 *       Detailed module information for display</li>
 *   <li>{@link sh.pcx.unified.modules.admin.HotReloader} -
 *       Hot reload support with file watching</li>
 * </ul>
 *
 * <h2>Admin Commands</h2>
 * <ul>
 *   <li>{@code /modules list} - List all modules and their status</li>
 *   <li>{@code /modules info <name>} - Show detailed module information</li>
 *   <li>{@code /modules enable <name>} - Enable a disabled module</li>
 *   <li>{@code /modules disable <name>} - Disable an enabled module</li>
 *   <li>{@code /modules reload <name>} - Reload a specific module</li>
 *   <li>{@code /modules reload} - Reload all modules</li>
 * </ul>
 *
 * <h2>Hot Reload</h2>
 * <p>The HotReloader can watch configuration files and automatically
 * reload modules when changes are detected:
 *
 * <pre>{@code
 * HotReloader reloader = new HotReloader(moduleManager, logger);
 * reloader.watchConfigFolder(plugin.getDataFolder().toPath());
 * reloader.start();
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create command handler
 * ModuleCommands commands = new ModuleCommands(moduleManager);
 *
 * // Execute command
 * CommandResult result = commands.list();
 *
 * // Display result
 * for (String line : result.getMessages()) {
 *     sender.sendMessage(line);
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
package sh.pcx.unified.modules.admin;
