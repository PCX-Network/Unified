/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.execution;

import sh.pcx.unified.commands.core.CommandRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Generates help messages for commands.
 *
 * <p>The {@code HelpGenerator} creates formatted help output for commands,
 * including usage information, descriptions, and permission requirements.
 * It supports pagination for commands with many subcommands.</p>
 *
 * <h2>Help Format</h2>
 * <pre>
 * ====== /command Help (Page 1/3) ======
 *
 * /command - Command description
 *   /command subcommand1 &lt;arg&gt; - Subcommand description
 *   /command subcommand2 [optional] - Another description
 *
 * Aliases: cmd, c
 * Permission: plugin.command
 *
 * Use /command help &lt;page&gt; for more
 * </pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Generating Command Help</h3>
 * <pre>{@code
 * HelpGenerator help = commandService.getHelpGenerator();
 *
 * // Get help for a command
 * List<String> lines = help.generateHelp("warp", player);
 * lines.forEach(player::sendMessage);
 * }</pre>
 *
 * <h3>Paginated Help</h3>
 * <pre>{@code
 * // Get page 2 of help
 * List<String> lines = help.generateHelp("game", player, 2);
 * lines.forEach(player::sendMessage);
 * }</pre>
 *
 * <h3>Command List</h3>
 * <pre>{@code
 * // List all available commands
 * List<String> lines = help.generateCommandList(player);
 * lines.forEach(player::sendMessage);
 * }</pre>
 *
 * <h3>Usage String</h3>
 * <pre>{@code
 * // Get usage for a specific command
 * String usage = help.getUsage("teleport", "player");
 * // Returns: "/teleport player <target>"
 * }</pre>
 *
 * <h3>Custom Formatting</h3>
 * <pre>{@code
 * // Configure help appearance
 * help.setHeaderFormat("<gold>====== <yellow>/{command}</yellow> Help ======</gold>");
 * help.setCommandFormat("<gray>/{command} {args}</gray> - <white>{description}</white>");
 * help.setItemsPerPage(8);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CommandRegistry
 */
public interface HelpGenerator {

    /**
     * Generates help for a command.
     *
     * <pre>{@code
     * List<String> help = generator.generateHelp("warp", player);
     * }</pre>
     *
     * @param command the command name
     * @param sender the sender (for permission filtering)
     * @return list of help message lines
     */
    @NotNull
    List<String> generateHelp(@NotNull String command, @NotNull Object sender);

    /**
     * Generates paginated help for a command.
     *
     * <pre>{@code
     * List<String> help = generator.generateHelp("game", player, 2);
     * }</pre>
     *
     * @param command the command name
     * @param sender the sender (for permission filtering)
     * @param page the page number (1-based)
     * @return list of help message lines
     */
    @NotNull
    List<String> generateHelp(@NotNull String command, @NotNull Object sender, int page);

    /**
     * Generates help for a subcommand.
     *
     * <pre>{@code
     * List<String> help = generator.generateSubcommandHelp("game", "team", player);
     * }</pre>
     *
     * @param command the root command name
     * @param subcommand the subcommand path
     * @param sender the sender
     * @return list of help message lines
     */
    @NotNull
    List<String> generateSubcommandHelp(
            @NotNull String command,
            @NotNull String subcommand,
            @NotNull Object sender
    );

    /**
     * Generates a list of all available commands.
     *
     * <pre>{@code
     * List<String> commands = generator.generateCommandList(player);
     * }</pre>
     *
     * @param sender the sender (for permission filtering)
     * @return list of command descriptions
     */
    @NotNull
    List<String> generateCommandList(@NotNull Object sender);

    /**
     * Generates a paginated list of all commands.
     *
     * @param sender the sender
     * @param page the page number (1-based)
     * @return list of command descriptions
     */
    @NotNull
    List<String> generateCommandList(@NotNull Object sender, int page);

    /**
     * Gets the usage string for a command.
     *
     * <pre>{@code
     * String usage = generator.getUsage("teleport");
     * // Returns: "/teleport <target> [x] [y] [z]"
     * }</pre>
     *
     * @param command the command name
     * @return the usage string
     */
    @NotNull
    String getUsage(@NotNull String command);

    /**
     * Gets the usage string for a subcommand.
     *
     * <pre>{@code
     * String usage = generator.getUsage("game", "team", "add");
     * // Returns: "/game team add <player>"
     * }</pre>
     *
     * @param command the root command
     * @param path the subcommand path segments
     * @return the usage string
     */
    @NotNull
    String getUsage(@NotNull String command, @NotNull String... path);

    /**
     * Gets the total number of help pages for a command.
     *
     * @param command the command name
     * @param sender the sender (for permission filtering)
     * @return the total page count
     */
    int getPageCount(@NotNull String command, @NotNull Object sender);

    /**
     * Sets the number of items per help page.
     *
     * @param itemsPerPage items per page
     */
    void setItemsPerPage(int itemsPerPage);

    /**
     * Gets the number of items per help page.
     *
     * @return items per page
     */
    int getItemsPerPage();

    /**
     * Sets the header format.
     *
     * <p>Placeholders:</p>
     * <ul>
     *   <li>{@code {command}} - Command name</li>
     *   <li>{@code {page}} - Current page</li>
     *   <li>{@code {pages}} - Total pages</li>
     * </ul>
     *
     * @param format the header format
     */
    void setHeaderFormat(@NotNull String format);

    /**
     * Sets the command line format.
     *
     * <p>Placeholders:</p>
     * <ul>
     *   <li>{@code {command}} - Full command path</li>
     *   <li>{@code {args}} - Argument syntax</li>
     *   <li>{@code {description}} - Command description</li>
     *   <li>{@code {permission}} - Required permission</li>
     * </ul>
     *
     * @param format the command format
     */
    void setCommandFormat(@NotNull String format);

    /**
     * Sets the footer format.
     *
     * <p>Placeholders:</p>
     * <ul>
     *   <li>{@code {command}} - Command name</li>
     *   <li>{@code {nextPage}} - Next page number</li>
     * </ul>
     *
     * @param format the footer format
     */
    void setFooterFormat(@Nullable String format);

    /**
     * Sets whether to show permissions in help.
     *
     * @param show {@code true} to show permissions
     */
    void setShowPermissions(boolean show);

    /**
     * Sets whether to show aliases in help.
     *
     * @param show {@code true} to show aliases
     */
    void setShowAliases(boolean show);

    /**
     * Sets whether to show hidden commands.
     *
     * <p>Hidden commands are normally excluded from help listings.</p>
     *
     * @param show {@code true} to include hidden commands
     */
    void setShowHidden(boolean show);

    /**
     * Generates a formatted error message for unknown commands.
     *
     * @param input the unknown command input
     * @param suggestions suggested similar commands
     * @return the error message
     */
    @NotNull
    String generateUnknownCommandMessage(@NotNull String input, @NotNull List<String> suggestions);

    /**
     * Generates a formatted usage error message.
     *
     * @param command the command that was used incorrectly
     * @param usage the correct usage
     * @return the error message
     */
    @NotNull
    String generateUsageError(@NotNull String command, @NotNull String usage);
}
