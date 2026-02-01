/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.core;

import sh.pcx.unified.commands.completion.CompletionProvider;
import sh.pcx.unified.commands.parsing.ArgumentParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Main service interface for the annotation-based command framework.
 *
 * <p>The {@code CommandService} provides methods to register command classes,
 * argument parsers, and tab completion providers. It serves as the entry point
 * for integrating commands into a plugin.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Registering Commands</h3>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *
 *     @Inject
 *     private CommandService commands;
 *
 *     @Override
 *     public void onEnable() {
 *         // Register a single command
 *         commands.register(new SpawnCommand());
 *
 *         // Register multiple commands
 *         commands.registerAll(
 *             new HomeCommand(),
 *             new WarpCommand(),
 *             new TeleportCommand()
 *         );
 *
 *         // Register with dependency injection
 *         commands.register(GameCommand.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Registering Custom Parsers</h3>
 * <pre>{@code
 * // Custom type parser
 * commands.registerParser(Arena.class, new ArgumentParser<Arena>() {
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
 *         return arenaManager.getArenaNames();
 *     }
 * });
 * }</pre>
 *
 * <h3>Registering Completion Providers</h3>
 * <pre>{@code
 * // Register custom completion provider
 * commands.registerCompletions("@arenas", context -> {
 *     return arenaManager.getArenas().stream()
 *         .map(Arena::getName)
 *         .collect(Collectors.toList());
 * });
 *
 * // Use in command
 * @Subcommand("join")
 * public void join(
 *     @Sender Player player,
 *     @Arg("arena") @Completions("@arenas") String arenaName
 * ) {
 *     // ...
 * }
 * }</pre>
 *
 * <h3>Unregistering Commands</h3>
 * <pre>{@code
 * @Override
 * public void onDisable() {
 *     // Unregister specific command
 *     commands.unregister("spawn");
 *
 *     // Unregister all plugin commands
 *     commands.unregisterAll();
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CommandRegistry
 * @see CommandExecutor
 * @see ArgumentParser
 * @see CompletionProvider
 */
public interface CommandService {

    /**
     * Registers a command instance with the framework.
     *
     * <p>The command class must be annotated with {@link sh.pcx.unified.commands.annotation.Command}.
     * Methods annotated with {@link sh.pcx.unified.commands.annotation.Subcommand} or
     * {@link sh.pcx.unified.commands.annotation.Default} are registered as handlers.</p>
     *
     * <pre>{@code
     * commands.register(new GameCommand());
     * }</pre>
     *
     * @param command the command instance to register
     * @throws IllegalArgumentException if the class is not annotated with @Command
     * @throws CommandRegistrationException if registration fails
     */
    void register(@NotNull Object command);

    /**
     * Registers a command class using dependency injection.
     *
     * <p>The command instance is created using the configured injector,
     * allowing for automatic dependency injection.</p>
     *
     * <pre>{@code
     * commands.register(GameCommand.class);
     * }</pre>
     *
     * @param commandClass the command class to register
     * @param <T> the command class type
     * @throws IllegalArgumentException if the class is not annotated with @Command
     * @throws CommandRegistrationException if registration fails
     */
    <T> void register(@NotNull Class<T> commandClass);

    /**
     * Registers multiple command instances.
     *
     * <pre>{@code
     * commands.registerAll(
     *     new SpawnCommand(),
     *     new HomeCommand(),
     *     new WarpCommand()
     * );
     * }</pre>
     *
     * @param commands the command instances to register
     */
    void registerAll(@NotNull Object... commands);

    /**
     * Registers multiple command classes using dependency injection.
     *
     * <pre>{@code
     * commands.registerAll(
     *     SpawnCommand.class,
     *     HomeCommand.class,
     *     WarpCommand.class
     * );
     * }</pre>
     *
     * @param commandClasses the command classes to register
     */
    void registerAllClasses(@NotNull Class<?>... commandClasses);

    /**
     * Unregisters a command by its name.
     *
     * <pre>{@code
     * commands.unregister("spawn");
     * }</pre>
     *
     * @param commandName the name of the command to unregister
     * @return {@code true} if the command was unregistered, {@code false} if not found
     */
    boolean unregister(@NotNull String commandName);

    /**
     * Unregisters a command by its instance.
     *
     * @param command the command instance to unregister
     * @return {@code true} if the command was unregistered
     */
    boolean unregister(@NotNull Object command);

    /**
     * Unregisters all commands registered by this service.
     *
     * <p>Typically called during plugin disable.</p>
     */
    void unregisterAll();

    /**
     * Registers a custom argument parser for a type.
     *
     * <p>Custom parsers allow commands to accept plugin-specific types as arguments.</p>
     *
     * <pre>{@code
     * commands.registerParser(Arena.class, new ArenaParser());
     * }</pre>
     *
     * @param type the class to register the parser for
     * @param parser the parser implementation
     * @param <T> the type being parsed
     */
    <T> void registerParser(@NotNull Class<T> type, @NotNull ArgumentParser<T> parser);

    /**
     * Gets the registered parser for a type.
     *
     * @param type the class to get the parser for
     * @param <T> the type
     * @return the parser, or {@code null} if none registered
     */
    @Nullable
    <T> ArgumentParser<T> getParser(@NotNull Class<T> type);

    /**
     * Registers a tab completion provider.
     *
     * <p>Completion providers are referenced by their key (prefixed with {@code @})
     * in the {@link sh.pcx.unified.commands.annotation.Completions} annotation.</p>
     *
     * <pre>{@code
     * commands.registerCompletions("@arenas", context -> {
     *     return arenaManager.getArenaNames();
     * });
     * }</pre>
     *
     * @param key the provider key (should start with @)
     * @param provider the completion provider
     */
    void registerCompletions(@NotNull String key, @NotNull CompletionProvider provider);

    /**
     * Gets a registered completion provider.
     *
     * @param key the provider key
     * @return the provider, or {@code null} if not found
     */
    @Nullable
    CompletionProvider getCompletionProvider(@NotNull String key);

    /**
     * Gets all registered command names.
     *
     * @return collection of registered command names
     */
    @NotNull
    Collection<String> getRegisteredCommands();

    /**
     * Checks if a command is registered.
     *
     * @param commandName the command name to check
     * @return {@code true} if the command is registered
     */
    boolean isRegistered(@NotNull String commandName);

    /**
     * Gets the command registry.
     *
     * @return the command registry
     */
    @NotNull
    CommandRegistry getRegistry();

    /**
     * Gets the command executor.
     *
     * @return the command executor
     */
    @NotNull
    CommandExecutor getExecutor();

    /**
     * Reloads all registered commands.
     *
     * <p>This re-reads command metadata and refreshes registrations.
     * Useful for hot-reloading during development.</p>
     */
    void reload();
}
