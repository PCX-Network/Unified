/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.parsing;

import sh.pcx.unified.commands.completion.CompletionContext;
import sh.pcx.unified.commands.core.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Interface for parsing command arguments into typed values.
 *
 * <p>Argument parsers convert raw string input from commands into typed Java objects.
 * The command framework uses parsers to populate method parameters annotated with
 * {@link sh.pcx.unified.commands.annotation.Arg}.</p>
 *
 * <h2>Built-in Parsers</h2>
 * <p>The framework provides parsers for common types:</p>
 * <ul>
 *   <li>{@code String} - Returns input as-is</li>
 *   <li>{@code Integer}, {@code int} - Parses integers</li>
 *   <li>{@code Double}, {@code double} - Parses decimal numbers</li>
 *   <li>{@code Boolean}, {@code boolean} - Parses true/false/yes/no</li>
 *   <li>{@code Player} - Resolves online player by name</li>
 *   <li>{@code World} - Resolves loaded world by name</li>
 *   <li>{@code Material} - Parses Minecraft materials</li>
 *   <li>{@code Duration} - Parses time durations (1h30m)</li>
 *   <li>Any {@code Enum} - Parses enum constants</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Implementing a Custom Parser</h3>
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
 *             throw new ParseException("Unknown arena: " + input);
 *         }
 *         return arena;
 *     }
 *
 *     @Override
 *     public List<String> suggest(CompletionContext context) {
 *         return arenaManager.getArenas().stream()
 *             .map(Arena::getName)
 *             .collect(Collectors.toList());
 *     }
 * }
 * }</pre>
 *
 * <h3>Registering the Parser</h3>
 * <pre>{@code
 * commandService.registerParser(Arena.class, new ArenaParser(arenaManager));
 * }</pre>
 *
 * <h3>Using in Commands</h3>
 * <pre>{@code
 * @Subcommand("join")
 * public void joinArena(
 *     @Sender Player player,
 *     @Arg("arena") Arena arena
 * ) {
 *     arena.addPlayer(player);
 * }
 * }</pre>
 *
 * <h3>Parser with Validation</h3>
 * <pre>{@code
 * public class PositiveIntegerParser implements ArgumentParser<Integer> {
 *
 *     @Override
 *     public Integer parse(CommandContext context, String input) throws ParseException {
 *         try {
 *             int value = Integer.parseInt(input);
 *             if (value <= 0) {
 *                 throw new ParseException("Value must be positive: " + input);
 *             }
 *             return value;
 *         } catch (NumberFormatException e) {
 *             throw new ParseException("Invalid number: " + input);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type this parser produces
 * @author Supatuck
 * @since 1.0.0
 * @see ParserRegistry
 * @see ParseException
 */
public interface ArgumentParser<T> {

    /**
     * Parses a string argument into the target type.
     *
     * <p>This method should throw a {@link ParseException} if the input
     * cannot be converted to the target type. The exception message
     * is displayed to the user.</p>
     *
     * <pre>{@code
     * @Override
     * public Player parse(CommandContext context, String input) throws ParseException {
     *     Player player = Bukkit.getPlayer(input);
     *     if (player == null) {
     *         throw new ParseException("Player not found: " + input);
     *     }
     *     return player;
     * }
     * }</pre>
     *
     * @param context the command context
     * @param input the raw string input
     * @return the parsed value
     * @throws ParseException if parsing fails
     */
    @NotNull
    T parse(@NotNull CommandContext context, @NotNull String input) throws ParseException;

    /**
     * Provides tab completion suggestions for this argument type.
     *
     * <p>This method is called during tab completion to provide relevant
     * suggestions. The default implementation returns an empty list.</p>
     *
     * <pre>{@code
     * @Override
     * public List<String> suggest(CompletionContext context) {
     *     return Bukkit.getOnlinePlayers().stream()
     *         .map(Player::getName)
     *         .filter(name -> name.toLowerCase().startsWith(
     *             context.getCurrentInput().toLowerCase()))
     *         .collect(Collectors.toList());
     * }
     * }</pre>
     *
     * @param context the completion context
     * @return list of suggestions
     */
    @NotNull
    default List<String> suggest(@NotNull CompletionContext context) {
        return Collections.emptyList();
    }

    /**
     * Gets the type this parser produces.
     *
     * <p>Used for parser registration and lookup. The default implementation
     * uses reflection, but can be overridden for efficiency.</p>
     *
     * @return the parsed type class
     */
    @Nullable
    default Class<T> getType() {
        return null; // Determined at registration time
    }

    /**
     * Checks if this parser consumes multiple arguments (greedy).
     *
     * <p>Greedy parsers consume all remaining arguments. Used for
     * message-type arguments that span multiple words.</p>
     *
     * @return {@code true} if this parser is greedy
     */
    default boolean isGreedy() {
        return false;
    }

    /**
     * Gets the default error message for parse failures.
     *
     * <p>Override to provide a custom default error format.</p>
     *
     * @return the error message format with {input} placeholder
     */
    @NotNull
    default String getErrorMessage() {
        return "Invalid value: {input}";
    }

    /**
     * Validates a parsed value against argument constraints.
     *
     * <p>Called after parsing to apply additional validation based
     * on {@link sh.pcx.unified.commands.annotation.Arg} constraints
     * like min/max values or string length.</p>
     *
     * @param value the parsed value
     * @param context the command context
     * @throws ParseException if validation fails
     */
    default void validate(@NotNull T value, @NotNull CommandContext context) throws ParseException {
        // Default: no additional validation
    }
}
