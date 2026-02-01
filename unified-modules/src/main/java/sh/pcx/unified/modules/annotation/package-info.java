/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Annotations for defining modules in the UnifiedPlugin module system.
 *
 * <p>This package provides the core annotations used to mark and configure
 * module classes:
 *
 * <h2>Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.annotation.Module @Module} -
 *       Marks a class as a plugin module with name, description, dependencies,
 *       and other metadata</li>
 *   <li>{@link sh.pcx.unified.modules.annotation.Listen @Listen} -
 *       Marks a module class as an event listener for automatic registration</li>
 *   <li>{@link sh.pcx.unified.modules.annotation.Command @Command} -
 *       Marks a module class as a command executor for automatic registration</li>
 *   <li>{@link sh.pcx.unified.modules.annotation.Configurable @Configurable} -
 *       Marks a module as having configuration that should be automatically loaded</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(
 *     name = "BattlePass",
 *     description = "Seasonal battle pass progression system",
 *     version = "2.1.0",
 *     dependencies = {"Economy", "PlayerData"},
 *     priority = ModulePriority.HIGH
 * )
 * @Listen
 * @Command(name = "battlepass", aliases = {"bp"})
 * @Configurable
 * public class BattlePassModule implements
 *         Listener,
 *         TabExecutor,
 *         Initializable,
 *         Reloadable {
 *     // Module implementation
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.modules.core.ModuleManager
 */
package sh.pcx.unified.modules.annotation;
