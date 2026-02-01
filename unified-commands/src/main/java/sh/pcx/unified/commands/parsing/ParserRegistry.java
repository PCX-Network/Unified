/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.parsing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Registry for argument parsers.
 *
 * <p>The {@code ParserRegistry} maintains mappings between types and their
 * corresponding parsers. It supports registration of custom parsers and
 * provides lookup methods for the command framework.</p>
 *
 * <h2>Built-in Parsers</h2>
 * <p>The registry is pre-populated with parsers for common types:</p>
 * <table border="1">
 *   <tr><th>Type</th><th>Parser</th><th>Example Input</th></tr>
 *   <tr><td>String</td><td>StringParser</td><td>"hello"</td></tr>
 *   <tr><td>Integer</td><td>IntegerParser</td><td>"42"</td></tr>
 *   <tr><td>Double</td><td>DoubleParser</td><td>"3.14"</td></tr>
 *   <tr><td>Boolean</td><td>BooleanParser</td><td>"true", "yes"</td></tr>
 *   <tr><td>Player</td><td>PlayerParser</td><td>"Steve"</td></tr>
 *   <tr><td>World</td><td>WorldParser</td><td>"world_nether"</td></tr>
 *   <tr><td>Material</td><td>MaterialParser</td><td>"diamond_sword"</td></tr>
 *   <tr><td>Duration</td><td>DurationParser</td><td>"1h30m"</td></tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Registering a Parser</h3>
 * <pre>{@code
 * ParserRegistry registry = commandService.getRegistry().getParserRegistry();
 *
 * // Register custom parser
 * registry.register(Arena.class, new ArenaParser(arenaManager));
 *
 * // Register with priority (higher priority overrides)
 * registry.register(Player.class, new CustomPlayerParser(), 10);
 * }</pre>
 *
 * <h3>Getting a Parser</h3>
 * <pre>{@code
 * ArgumentParser<Player> parser = registry.getParser(Player.class);
 * if (parser != null) {
 *     Player player = parser.parse(context, "Steve");
 * }
 * }</pre>
 *
 * <h3>Enum Parser</h3>
 * <pre>{@code
 * // Enums are automatically handled
 * public enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }
 *
 * @Subcommand("gamemode")
 * public void setGamemode(@Sender Player p, @Arg("mode") GameMode mode) {
 *     // GameMode is automatically parsed
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 * @see sh.pcx.unified.commands.core.CommandService
 */
public interface ParserRegistry {

    /**
     * Registers a parser for a type.
     *
     * <p>If a parser is already registered for the type, it is replaced.</p>
     *
     * <pre>{@code
     * registry.register(Arena.class, new ArenaParser());
     * }</pre>
     *
     * @param type the type to register for
     * @param parser the parser implementation
     * @param <T> the type
     */
    <T> void register(@NotNull Class<T> type, @NotNull ArgumentParser<T> parser);

    /**
     * Registers a parser with a priority.
     *
     * <p>Higher priority parsers take precedence. Useful for overriding
     * built-in parsers or having platform-specific implementations.</p>
     *
     * @param type the type to register for
     * @param parser the parser implementation
     * @param priority the priority (higher = takes precedence)
     * @param <T> the type
     */
    <T> void register(@NotNull Class<T> type, @NotNull ArgumentParser<T> parser, int priority);

    /**
     * Unregisters a parser for a type.
     *
     * @param type the type to unregister
     * @return {@code true} if a parser was unregistered
     */
    boolean unregister(@NotNull Class<?> type);

    /**
     * Gets the parser for a type.
     *
     * <p>For enum types, returns a generic enum parser if no specific
     * parser is registered.</p>
     *
     * <pre>{@code
     * ArgumentParser<Player> parser = registry.getParser(Player.class);
     * }</pre>
     *
     * @param type the type to get the parser for
     * @param <T> the type
     * @return the parser, or {@code null} if none registered
     */
    @Nullable
    <T> ArgumentParser<T> getParser(@NotNull Class<T> type);

    /**
     * Checks if a parser is registered for a type.
     *
     * @param type the type to check
     * @return {@code true} if a parser is registered
     */
    boolean hasParser(@NotNull Class<?> type);

    /**
     * Gets all registered types.
     *
     * @return set of types with registered parsers
     */
    @NotNull
    Set<Class<?>> getRegisteredTypes();

    /**
     * Gets all registered parsers.
     *
     * @return map of types to parsers
     */
    @NotNull
    Map<Class<?>, ArgumentParser<?>> getAllParsers();

    /**
     * Registers all built-in parsers.
     *
     * <p>Called during initialization to set up parsers for common types.
     * Can be called again to reset to default parsers.</p>
     */
    void registerDefaults();

    /**
     * Clears all registered parsers.
     */
    void clear();

    /**
     * Gets the default priority for parser registration.
     *
     * @return the default priority (0)
     */
    default int getDefaultPriority() {
        return 0;
    }
}
