/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */

/**
 * Core classes for the UnifiedPlugin command framework.
 *
 * <p>This package contains the main service interfaces and classes for
 * registering, storing, and executing commands. These form the backbone
 * of the annotation-based command system.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.core.CommandService} -
 *       Main entry point for command registration</li>
 *   <li>{@link sh.pcx.unified.commands.core.CommandRegistry} -
 *       Storage for registered commands</li>
 *   <li>{@link sh.pcx.unified.commands.core.CommandExecutor} -
 *       Executes commands with parsing and validation</li>
 *   <li>{@link sh.pcx.unified.commands.core.CommandContext} -
 *       Context object passed to command handlers</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *
 *     @Inject
 *     private CommandService commands;
 *
 *     @Override
 *     public void onEnable() {
 *         // Register commands
 *         commands.register(new SpawnCommand());
 *         commands.register(new HomeCommand());
 *
 *         // Register custom parser
 *         commands.registerParser(Arena.class, new ArenaParser());
 *
 *         // Register completion provider
 *         commands.registerCompletions("@arenas", ctx ->
 *             arenaManager.getArenaNames()
 *         );
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         commands.unregisterAll();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.commands.annotation
 */
package sh.pcx.unified.commands.core;
