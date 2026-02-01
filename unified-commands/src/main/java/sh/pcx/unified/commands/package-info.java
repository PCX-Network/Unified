/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */

/**
 * Annotation-based command framework for the UnifiedPlugin API.
 *
 * <p>This module provides a complete command framework that allows plugins to
 * define commands using annotations. It supports automatic argument parsing,
 * tab completion, cooldowns, async execution, and permission checking.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Annotation-Based</b> - Define commands declaratively with annotations</li>
 *   <li><b>Subcommands</b> - Support for nested command trees</li>
 *   <li><b>Tab Completion</b> - Context-aware suggestions</li>
 *   <li><b>Argument Parsing</b> - Built-in parsers for common types</li>
 *   <li><b>Permission Checks</b> - Per-command and per-subcommand permissions</li>
 *   <li><b>Cooldowns</b> - Rate limiting with multiple scope options</li>
 *   <li><b>Async Execution</b> - Run commands off the main thread</li>
 *   <li><b>Help Generation</b> - Auto-generated help messages</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Define a command
 * @Command(name = "game", description = "Game commands")
 * @Permission("mygame.use")
 * public class GameCommand {
 *
 *     @Subcommand("start")
 *     @Permission("mygame.start")
 *     @Cooldown(value = 30, unit = TimeUnit.SECONDS)
 *     public void start(
 *         @Sender Player player,
 *         @Arg("arena") @Completions("@arenas") String arenaName,
 *         @Arg("mode") @Default("classic") GameMode mode
 *     ) {
 *         // Start the game
 *     }
 *
 *     @Subcommand("join")
 *     @Async
 *     public void join(
 *         @Sender Player player,
 *         @Arg("arena") String arenaName
 *     ) {
 *         // Join a game (async for database lookup)
 *     }
 * }
 *
 * // Register in plugin
 * public class MyPlugin extends UnifiedPlugin {
 *
 *     @Inject
 *     private CommandService commands;
 *
 *     @Override
 *     public void onEnable() {
 *         commands.register(new GameCommand());
 *
 *         commands.registerCompletions("@arenas", ctx ->
 *             arenaManager.getArenaNames()
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Packages</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.annotation} - Command annotations</li>
 *   <li>{@link sh.pcx.unified.commands.core} - Core service interfaces</li>
 *   <li>{@link sh.pcx.unified.commands.parsing} - Argument parsers</li>
 *   <li>{@link sh.pcx.unified.commands.completion} - Tab completion</li>
 *   <li>{@link sh.pcx.unified.commands.execution} - Execution support</li>
 * </ul>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.commands.core.CommandService
 */
package sh.pcx.unified.commands;
