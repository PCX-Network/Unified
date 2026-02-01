/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for storing and managing registered commands.
 *
 * <p>The {@code CommandRegistry} maintains the mapping between command names
 * and their metadata, including handlers, subcommands, and permissions.
 * It provides lookup methods for command execution and tab completion.</p>
 *
 * <h2>Internal Structure</h2>
 * <p>Commands are stored hierarchically:</p>
 * <pre>
 * /game                    (root command)
 *   ├── start              (subcommand)
 *   ├── stop               (subcommand)
 *   └── team               (subcommand group)
 *       ├── create         (nested subcommand)
 *       └── delete         (nested subcommand)
 * </pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Looking Up Commands</h3>
 * <pre>{@code
 * // Find a command by name
 * Optional<RegisteredCommand> cmd = registry.findCommand("game");
 *
 * // Find by name or alias
 * Optional<RegisteredCommand> cmd = registry.findCommandOrAlias("tp");
 *
 * // Find a subcommand
 * Optional<RegisteredSubcommand> sub = registry.findSubcommand("game", "team", "create");
 * }</pre>
 *
 * <h3>Iterating Commands</h3>
 * <pre>{@code
 * // All registered commands
 * for (RegisteredCommand cmd : registry.getAllCommands()) {
 *     System.out.println(cmd.getName());
 * }
 *
 * // Commands visible to a player
 * for (RegisteredCommand cmd : registry.getVisibleCommands(player)) {
 *     System.out.println(cmd.getName());
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CommandService
 * @see RegisteredCommand
 * @see RegisteredSubcommand
 */
public interface CommandRegistry {

    /**
     * Registers a command with the registry.
     *
     * @param command the command metadata to register
     * @throws IllegalArgumentException if a command with the same name exists
     */
    void register(@NotNull RegisteredCommand command);

    /**
     * Unregisters a command by name.
     *
     * @param name the command name
     * @return {@code true} if the command was unregistered
     */
    boolean unregister(@NotNull String name);

    /**
     * Finds a command by its primary name.
     *
     * @param name the command name
     * @return the command, or empty if not found
     */
    @NotNull
    Optional<RegisteredCommand> findCommand(@NotNull String name);

    /**
     * Finds a command by name or alias.
     *
     * @param nameOrAlias the command name or one of its aliases
     * @return the command, or empty if not found
     */
    @NotNull
    Optional<RegisteredCommand> findCommandOrAlias(@NotNull String nameOrAlias);

    /**
     * Finds a subcommand by its path.
     *
     * <pre>{@code
     * // Find /game team create
     * registry.findSubcommand("game", "team", "create");
     *
     * // Find /admin reload config
     * registry.findSubcommand("admin", "reload", "config");
     * }</pre>
     *
     * @param commandName the root command name
     * @param path the subcommand path segments
     * @return the subcommand, or empty if not found
     */
    @NotNull
    Optional<RegisteredSubcommand> findSubcommand(@NotNull String commandName, @NotNull String... path);

    /**
     * Gets all registered commands.
     *
     * @return unmodifiable collection of all commands
     */
    @NotNull
    Collection<RegisteredCommand> getAllCommands();

    /**
     * Gets command names that the sender has permission to see.
     *
     * @param sender the command sender
     * @return collection of visible command names
     */
    @NotNull
    Collection<RegisteredCommand> getVisibleCommands(@NotNull Object sender);

    /**
     * Gets all registered command names (including aliases).
     *
     * @return collection of all command names and aliases
     */
    @NotNull
    Collection<String> getAllCommandNames();

    /**
     * Checks if a command is registered.
     *
     * @param name the command name
     * @return {@code true} if registered
     */
    boolean isRegistered(@NotNull String name);

    /**
     * Clears all registered commands.
     */
    void clear();

    /**
     * Gets the number of registered commands.
     *
     * @return the command count
     */
    int size();

    /**
     * Represents a registered top-level command.
     */
    interface RegisteredCommand {

        /**
         * Gets the primary command name.
         *
         * @return the command name
         */
        @NotNull
        String getName();

        /**
         * Gets command aliases.
         *
         * @return list of aliases
         */
        @NotNull
        List<String> getAliases();

        /**
         * Gets the command description.
         *
         * @return the description
         */
        @NotNull
        String getDescription();

        /**
         * Gets the required permission.
         *
         * @return the permission node, or empty string if none
         */
        @NotNull
        String getPermission();

        /**
         * Gets the usage string.
         *
         * @return the usage string
         */
        @NotNull
        String getUsage();

        /**
         * Checks if the command is player-only.
         *
         * @return {@code true} if only players can execute
         */
        boolean isPlayerOnly();

        /**
         * Checks if the command is hidden from help.
         *
         * @return {@code true} if hidden
         */
        boolean isHidden();

        /**
         * Gets the command handler instance.
         *
         * @return the handler object
         */
        @NotNull
        Object getInstance();

        /**
         * Gets the command handler class.
         *
         * @return the handler class
         */
        @NotNull
        Class<?> getHandlerClass();

        /**
         * Gets the default handler method.
         *
         * @return the default method, or {@code null} if none
         */
        @Nullable
        Method getDefaultHandler();

        /**
         * Gets all subcommands.
         *
         * @return map of subcommand names to subcommands
         */
        @NotNull
        Map<String, RegisteredSubcommand> getSubcommands();

        /**
         * Finds a subcommand by name or alias.
         *
         * @param nameOrAlias the subcommand name or alias
         * @return the subcommand, or empty if not found
         */
        @NotNull
        Optional<RegisteredSubcommand> findSubcommand(@NotNull String nameOrAlias);

        /**
         * Checks if a sender has permission to use this command.
         *
         * @param sender the sender to check
         * @return {@code true} if permitted
         */
        boolean hasPermission(@NotNull Object sender);
    }

    /**
     * Represents a registered subcommand.
     */
    interface RegisteredSubcommand {

        /**
         * Gets the subcommand path (e.g., "team add").
         *
         * @return the subcommand path
         */
        @NotNull
        String getPath();

        /**
         * Gets the final subcommand name.
         *
         * @return the subcommand name
         */
        @NotNull
        String getName();

        /**
         * Gets subcommand aliases.
         *
         * @return list of aliases
         */
        @NotNull
        List<String> getAliases();

        /**
         * Gets the description.
         *
         * @return the description
         */
        @NotNull
        String getDescription();

        /**
         * Gets the usage string.
         *
         * @return the usage string
         */
        @NotNull
        String getUsage();

        /**
         * Gets the required permission.
         *
         * @return the permission, or empty if none
         */
        @NotNull
        String getPermission();

        /**
         * Gets the handler method.
         *
         * @return the handler method
         */
        @NotNull
        Method getHandler();

        /**
         * Gets the parent command.
         *
         * @return the parent command
         */
        @NotNull
        RegisteredCommand getParent();

        /**
         * Checks if this subcommand runs async.
         *
         * @return {@code true} if async
         */
        boolean isAsync();

        /**
         * Gets the cooldown in milliseconds.
         *
         * @return cooldown in ms, 0 if none
         */
        long getCooldownMillis();

        /**
         * Gets nested subcommands.
         *
         * @return map of nested subcommand names to subcommands
         */
        @NotNull
        Map<String, RegisteredSubcommand> getChildren();

        /**
         * Checks if a sender has permission to use this subcommand.
         *
         * @param sender the sender to check
         * @return {@code true} if permitted
         */
        boolean hasPermission(@NotNull Object sender);
    }
}
