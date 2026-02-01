/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */

/**
 * Argument parsing components for the command framework.
 *
 * <p>This package contains the argument parser interface and built-in
 * implementations for common types. Custom parsers can be implemented
 * and registered to handle plugin-specific types.</p>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.parsing.ArgumentParser} -
 *       Interface for type-specific parsing</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.ParserRegistry} -
 *       Registry for parser lookup</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.ParseException} -
 *       Exception for parse failures</li>
 * </ul>
 *
 * <h2>Built-in Parsers</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.parsing.IntegerParser} -
 *       Parses integer values</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.DoubleParser} -
 *       Parses decimal values</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.BooleanParser} -
 *       Parses boolean values</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.PlayerParser} -
 *       Parses online players</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.WorldParser} -
 *       Parses loaded worlds</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.MaterialParser} -
 *       Parses Minecraft materials</li>
 *   <li>{@link sh.pcx.unified.commands.parsing.DurationParser} -
 *       Parses time durations</li>
 * </ul>
 *
 * <h2>Custom Parser Example</h2>
 * <pre>{@code
 * public class ArenaParser implements ArgumentParser<Arena> {
 *
 *     private final ArenaManager arenaManager;
 *
 *     public ArenaParser(ArenaManager arenaManager) {
 *         this.arenaManager = arenaManager;
 *     }
 *
 *     @Override
 *     public Arena parse(CommandContext context, String input) throws ParseException {
 *         Arena arena = arenaManager.getArena(input);
 *         if (arena == null) {
 *             throw new ParseException("Unknown arena", input);
 *         }
 *         return arena;
 *     }
 *
 *     @Override
 *     public List<String> suggest(CompletionContext context) {
 *         return arenaManager.getArenaNames();
 *     }
 * }
 *
 * // Register
 * commandService.registerParser(Arena.class, new ArenaParser(arenaManager));
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.commands.core.CommandService
 */
package sh.pcx.unified.commands.parsing;
