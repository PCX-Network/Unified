/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.core;

import sh.pcx.unified.commands.execution.CommandResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Executes registered commands with argument parsing and validation.
 *
 * <p>The {@code CommandExecutor} handles the complete execution flow:</p>
 * <ol>
 *   <li>Permission checking</li>
 *   <li>Cooldown verification</li>
 *   <li>Argument parsing</li>
 *   <li>Validation</li>
 *   <li>Handler invocation</li>
 *   <li>Result handling</li>
 * </ol>
 *
 * <h2>Execution Flow</h2>
 * <pre>
 * Command Input
 *     │
 *     ▼
 * Permission Check ─── Denied ──► Send Error
 *     │
 *     ▼
 * Cooldown Check ───── Active ──► Send Remaining Time
 *     │
 *     ▼
 * Parse Arguments ──── Failed ──► Send Usage
 *     │
 *     ▼
 * Validate Args ────── Invalid ─► Send Error
 *     │
 *     ▼
 * Execute Handler ──── Async? ──► Run on Thread Pool
 *     │
 *     ▼
 * Return Result
 * </pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Direct Execution</h3>
 * <pre>{@code
 * // Execute a command programmatically
 * CommandResult result = executor.execute(player, "spawn");
 *
 * // With arguments
 * CommandResult result = executor.execute(player, "warp", "hub");
 *
 * // Check result
 * if (result.isSuccess()) {
 *     log.info("Command executed successfully");
 * } else {
 *     log.warn("Command failed: " + result.getMessage());
 * }
 * }</pre>
 *
 * <h3>Async Execution</h3>
 * <pre>{@code
 * // Execute asynchronously
 * executor.executeAsync(player, "stats")
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             log.info("Stats command completed");
 *         }
 *     });
 * }</pre>
 *
 * <h3>With Context</h3>
 * <pre>{@code
 * // Create custom context
 * CommandContext context = CommandContext.builder()
 *     .sender(player)
 *     .label("spawn")
 *     .args(new String[0])
 *     .build();
 *
 * CommandResult result = executor.execute(context);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CommandContext
 * @see CommandResult
 * @see CommandRegistry
 */
public interface CommandExecutor {

    /**
     * Executes a command with the given arguments.
     *
     * <p>This is the main entry point for command execution. The executor
     * handles all aspects of command processing including permission checks,
     * cooldowns, argument parsing, and handler invocation.</p>
     *
     * <pre>{@code
     * CommandResult result = executor.execute(player, "teleport", "Steve");
     * }</pre>
     *
     * @param sender the command sender
     * @param command the command name
     * @param args the command arguments
     * @return the execution result
     */
    @NotNull
    CommandResult execute(@NotNull Object sender, @NotNull String command, @NotNull String... args);

    /**
     * Executes a command using a pre-built context.
     *
     * <pre>{@code
     * CommandContext context = CommandContext.builder()
     *     .sender(player)
     *     .label("home")
     *     .args(new String[]{"base"})
     *     .build();
     *
     * CommandResult result = executor.execute(context);
     * }</pre>
     *
     * @param context the command context
     * @return the execution result
     */
    @NotNull
    CommandResult execute(@NotNull CommandContext context);

    /**
     * Executes a command asynchronously.
     *
     * <p>The command is executed on an async thread pool, regardless of
     * whether it's marked with {@code @Async}. The result is returned
     * via a CompletableFuture.</p>
     *
     * <pre>{@code
     * executor.executeAsync(player, "database", "backup")
     *     .thenAccept(result -> {
     *         player.sendMessage("Backup " +
     *             (result.isSuccess() ? "complete" : "failed"));
     *     });
     * }</pre>
     *
     * @param sender the command sender
     * @param command the command name
     * @param args the command arguments
     * @return a future that completes with the result
     */
    @NotNull
    CompletableFuture<CommandResult> executeAsync(
            @NotNull Object sender,
            @NotNull String command,
            @NotNull String... args
    );

    /**
     * Executes a command asynchronously using a context.
     *
     * @param context the command context
     * @return a future that completes with the result
     */
    @NotNull
    CompletableFuture<CommandResult> executeAsync(@NotNull CommandContext context);

    /**
     * Checks if a command can be executed by the sender.
     *
     * <p>This checks permissions and cooldowns without actually executing
     * the command. Useful for UI elements that need to show available commands.</p>
     *
     * <pre>{@code
     * if (executor.canExecute(player, "fly")) {
     *     showFlyButton(player);
     * }
     * }</pre>
     *
     * @param sender the command sender
     * @param command the command name
     * @param args the command arguments (for permission placeholders)
     * @return {@code true} if the command can be executed
     */
    boolean canExecute(@NotNull Object sender, @NotNull String command, @NotNull String... args);

    /**
     * Gets the remaining cooldown for a command in milliseconds.
     *
     * <pre>{@code
     * long remaining = executor.getRemainingCooldown(player, "spawn");
     * if (remaining > 0) {
     *     player.sendMessage("Wait " + (remaining / 1000) + " seconds");
     * }
     * }</pre>
     *
     * @param sender the command sender
     * @param command the command name
     * @return remaining cooldown in milliseconds, 0 if no cooldown
     */
    long getRemainingCooldown(@NotNull Object sender, @NotNull String command);

    /**
     * Clears the cooldown for a sender and command.
     *
     * <pre>{@code
     * executor.clearCooldown(player, "spawn");
     * }</pre>
     *
     * @param sender the command sender
     * @param command the command name
     */
    void clearCooldown(@NotNull Object sender, @NotNull String command);

    /**
     * Dispatches a command as if typed by the sender.
     *
     * <p>This parses the full command string including the command name.
     * Useful for executing commands from config files or chat triggers.</p>
     *
     * <pre>{@code
     * // Execute a full command string
     * executor.dispatch(player, "/spawn");
     * executor.dispatch(console, "say Hello World");
     * }</pre>
     *
     * @param sender the command sender
     * @param commandLine the full command line
     * @return the execution result
     */
    @NotNull
    CommandResult dispatch(@NotNull Object sender, @NotNull String commandLine);
}
