/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Context object passed to command handlers containing execution information.
 *
 * <p>The {@code CommandContext} provides access to the command sender, arguments,
 * and parsed values. It also allows storing custom data that can be accessed
 * throughout the command execution lifecycle.</p>
 *
 * <h2>Context Properties</h2>
 * <ul>
 *   <li><b>Sender</b> - The entity that executed the command</li>
 *   <li><b>Label</b> - The alias used to invoke the command</li>
 *   <li><b>Arguments</b> - Raw string arguments</li>
 *   <li><b>Parsed Args</b> - Type-converted argument values</li>
 *   <li><b>Data</b> - Custom key-value storage</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Accessing Context in Commands</h3>
 * <pre>{@code
 * @Subcommand("give")
 * public void give(
 *     CommandContext context,
 *     @Sender Player sender,
 *     @Arg("player") Player target,
 *     @Arg("item") Material material
 * ) {
 *     // Context is auto-injected if parameter is declared
 *     String label = context.getLabel(); // e.g., "give"
 *     String[] rawArgs = context.getArgs();
 *
 *     // Sender access
 *     if (context.isPlayer()) {
 *         Player player = context.getSenderAsPlayer();
 *     }
 * }
 * }</pre>
 *
 * <h3>Storing Custom Data</h3>
 * <pre>{@code
 * // In argument parser
 * context.put("parsed_arena", arena);
 *
 * // In command handler
 * Arena arena = context.get("parsed_arena", Arena.class);
 * }</pre>
 *
 * <h3>Building Context Programmatically</h3>
 * <pre>{@code
 * CommandContext context = CommandContext.builder()
 *     .sender(player)
 *     .label("spawn")
 *     .args(new String[]{"home"})
 *     .put("source", "gui")
 *     .build();
 *
 * executor.execute(context);
 * }</pre>
 *
 * <h3>Checking Sender Type</h3>
 * <pre>{@code
 * if (context.isPlayer()) {
 *     Player player = context.getSenderAsPlayer();
 *     player.teleport(location);
 * } else if (context.isConsole()) {
 *     context.sendMessage("This command is player-only");
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CommandExecutor
 * @see CommandRegistry.RegisteredCommand
 */
public interface CommandContext {

    /**
     * Gets the command sender.
     *
     * <p>The sender type depends on the platform:</p>
     * <ul>
     *   <li>Paper/Spigot: CommandSender, Player, ConsoleCommandSender</li>
     *   <li>Sponge: CommandSource, ServerPlayer</li>
     * </ul>
     *
     * @return the command sender
     */
    @NotNull
    Object getSender();

    /**
     * Gets the sender cast to the specified type.
     *
     * <pre>{@code
     * Player player = context.getSender(Player.class);
     * }</pre>
     *
     * @param type the type to cast to
     * @param <T> the sender type
     * @return the sender as the specified type
     * @throws ClassCastException if the sender is not the expected type
     */
    @NotNull
    <T> T getSender(@NotNull Class<T> type);

    /**
     * Gets the sender as a Player, if applicable.
     *
     * @return the player, or {@code null} if sender is not a player
     */
    @Nullable
    Object getSenderAsPlayer();

    /**
     * Checks if the sender is a player.
     *
     * @return {@code true} if the sender is a player
     */
    boolean isPlayer();

    /**
     * Checks if the sender is the console.
     *
     * @return {@code true} if the sender is the console
     */
    boolean isConsole();

    /**
     * Gets the command label (alias) used to invoke the command.
     *
     * <pre>{@code
     * // If command was invoked as "/tp Steve"
     * context.getLabel(); // Returns "tp"
     * }</pre>
     *
     * @return the command label
     */
    @NotNull
    String getLabel();

    /**
     * Gets the raw string arguments.
     *
     * @return array of argument strings
     */
    @NotNull
    String[] getArgs();

    /**
     * Gets the number of arguments.
     *
     * @return argument count
     */
    int getArgCount();

    /**
     * Gets an argument at the specified index.
     *
     * @param index the argument index
     * @return the argument value
     * @throws IndexOutOfBoundsException if index is invalid
     */
    @NotNull
    String getArg(int index);

    /**
     * Gets an argument at the specified index, or a default value.
     *
     * <pre>{@code
     * String mode = context.getArg(0, "default");
     * }</pre>
     *
     * @param index the argument index
     * @param defaultValue the default if index is invalid
     * @return the argument value or default
     */
    @NotNull
    String getArg(int index, @NotNull String defaultValue);

    /**
     * Checks if an argument exists at the index.
     *
     * @param index the argument index
     * @return {@code true} if the argument exists
     */
    boolean hasArg(int index);

    /**
     * Gets a parsed argument value by name.
     *
     * <pre>{@code
     * Player target = context.getParsedArg("target", Player.class);
     * }</pre>
     *
     * @param name the argument name (from @Arg annotation)
     * @param type the expected type
     * @param <T> the argument type
     * @return the parsed value, or empty if not found
     */
    @NotNull
    <T> Optional<T> getParsedArg(@NotNull String name, @NotNull Class<T> type);

    /**
     * Gets all parsed arguments.
     *
     * @return map of argument names to values
     */
    @NotNull
    Map<String, Object> getParsedArgs();

    /**
     * Stores a custom value in the context.
     *
     * <pre>{@code
     * context.put("start_time", System.currentTimeMillis());
     * }</pre>
     *
     * @param key the storage key
     * @param value the value to store
     */
    void put(@NotNull String key, @Nullable Object value);

    /**
     * Retrieves a custom value from the context.
     *
     * <pre>{@code
     * long startTime = context.get("start_time", Long.class);
     * }</pre>
     *
     * @param key the storage key
     * @param type the expected type
     * @param <T> the value type
     * @return the value, or empty if not found
     */
    @NotNull
    <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type);

    /**
     * Checks if a key exists in the context.
     *
     * @param key the storage key
     * @return {@code true} if the key exists
     */
    boolean has(@NotNull String key);

    /**
     * Sends a message to the sender.
     *
     * <p>Supports MiniMessage formatting.</p>
     *
     * <pre>{@code
     * context.sendMessage("<green>Command executed successfully!");
     * }</pre>
     *
     * @param message the message to send
     */
    void sendMessage(@NotNull String message);

    /**
     * Sends an error message to the sender.
     *
     * <pre>{@code
     * context.sendError("Invalid argument: " + input);
     * }</pre>
     *
     * @param message the error message
     */
    void sendError(@NotNull String message);

    /**
     * Checks if the sender has a permission.
     *
     * @param permission the permission node
     * @return {@code true} if the sender has the permission
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Gets the full command string as entered.
     *
     * <pre>{@code
     * // If entered: /warp hub
     * context.getFullCommand(); // Returns "/warp hub"
     * }</pre>
     *
     * @return the full command string
     */
    @NotNull
    String getFullCommand();

    /**
     * Gets the registered command being executed.
     *
     * @return the registered command
     */
    @NotNull
    CommandRegistry.RegisteredCommand getCommand();

    /**
     * Gets the subcommand being executed, if any.
     *
     * @return the subcommand, or empty if executing default handler
     */
    @NotNull
    Optional<CommandRegistry.RegisteredSubcommand> getSubcommand();

    /**
     * Creates a new builder for constructing contexts.
     *
     * @return a new context builder
     */
    static Builder builder() {
        return new CommandContextBuilder();
    }

    /**
     * Builder for creating CommandContext instances.
     */
    interface Builder {

        /**
         * Sets the command sender.
         *
         * @param sender the sender
         * @return this builder
         */
        Builder sender(@NotNull Object sender);

        /**
         * Sets the command label.
         *
         * @param label the label
         * @return this builder
         */
        Builder label(@NotNull String label);

        /**
         * Sets the raw arguments.
         *
         * @param args the arguments
         * @return this builder
         */
        Builder args(@NotNull String[] args);

        /**
         * Sets the registered command.
         *
         * @param command the command
         * @return this builder
         */
        Builder command(@NotNull CommandRegistry.RegisteredCommand command);

        /**
         * Sets the subcommand.
         *
         * @param subcommand the subcommand
         * @return this builder
         */
        Builder subcommand(@Nullable CommandRegistry.RegisteredSubcommand subcommand);

        /**
         * Adds a parsed argument.
         *
         * @param name the argument name
         * @param value the parsed value
         * @return this builder
         */
        Builder parsedArg(@NotNull String name, @Nullable Object value);

        /**
         * Adds custom data.
         *
         * @param key the data key
         * @param value the data value
         * @return this builder
         */
        Builder put(@NotNull String key, @Nullable Object value);

        /**
         * Builds the context.
         *
         * @return the built context
         */
        @NotNull
        CommandContext build();
    }
}
