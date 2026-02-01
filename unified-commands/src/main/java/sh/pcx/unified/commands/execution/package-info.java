/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */

/**
 * Command execution components for the command framework.
 *
 * <p>This package contains classes for command execution support including
 * cooldown management, result handling, and help generation.</p>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.execution.CooldownManager} -
 *       Manages command cooldowns</li>
 *   <li>{@link sh.pcx.unified.commands.execution.CommandResult} -
 *       Represents execution results</li>
 *   <li>{@link sh.pcx.unified.commands.execution.HelpGenerator} -
 *       Generates help messages</li>
 * </ul>
 *
 * <h2>Cooldown Example</h2>
 * <pre>{@code
 * CooldownManager cooldowns = commandService.getCooldownManager();
 *
 * // Check cooldown
 * if (cooldowns.isOnCooldown(player.getUniqueId(), "spawn")) {
 *     long remaining = cooldowns.getRemainingCooldown(player.getUniqueId(), "spawn");
 *     player.sendMessage("Wait " + cooldowns.getFormattedRemaining(player.getUniqueId(), "spawn"));
 *     return;
 * }
 *
 * // Set cooldown after execution
 * cooldowns.setCooldown(player.getUniqueId(), "spawn", Duration.ofSeconds(30));
 * }</pre>
 *
 * <h2>Result Handling Example</h2>
 * <pre>{@code
 * @Subcommand("transfer")
 * public CommandResult transfer(
 *     @Sender Player sender,
 *     @Arg("target") Player target,
 *     @Arg("amount") double amount
 * ) {
 *     if (amount <= 0) {
 *         return CommandResult.failure("Amount must be positive");
 *     }
 *
 *     if (!economy.has(sender, amount)) {
 *         return CommandResult.failure("Insufficient funds");
 *     }
 *
 *     economy.transfer(sender, target, amount);
 *     return CommandResult.success("Transferred $" + amount);
 * }
 * }</pre>
 *
 * <h2>Help Generation Example</h2>
 * <pre>{@code
 * HelpGenerator help = commandService.getHelpGenerator();
 *
 * // Configure formatting
 * help.setHeaderFormat("<gold>===== <yellow>/{command}</yellow> Help =====</gold>");
 * help.setItemsPerPage(8);
 *
 * // Generate and send help
 * List<String> lines = help.generateHelp("warp", player, 1);
 * lines.forEach(player::sendMessage);
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.commands.core.CommandExecutor
 */
package sh.pcx.unified.commands.execution;
