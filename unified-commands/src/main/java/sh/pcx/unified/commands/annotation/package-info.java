/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */

/**
 * Annotations for defining commands in the UnifiedPlugin command framework.
 *
 * <p>This package contains all annotations used to define commands, subcommands,
 * arguments, and command behavior. The annotation-based approach allows for
 * clean, declarative command definitions.</p>
 *
 * <h2>Core Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.annotation.Command @Command} -
 *       Marks a class as a command handler</li>
 *   <li>{@link sh.pcx.unified.commands.annotation.Subcommand @Subcommand} -
 *       Marks a method as a subcommand handler</li>
 *   <li>{@link sh.pcx.unified.commands.annotation.Default @Default} -
 *       Marks the default handler or provides default parameter values</li>
 * </ul>
 *
 * <h2>Parameter Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.annotation.Arg @Arg} -
 *       Marks a parameter as a command argument</li>
 *   <li>{@link sh.pcx.unified.commands.annotation.Sender @Sender} -
 *       Marks a parameter to receive the command sender</li>
 *   <li>{@link sh.pcx.unified.commands.annotation.Completions @Completions} -
 *       Specifies tab completion providers</li>
 * </ul>
 *
 * <h2>Behavior Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.annotation.Permission @Permission} -
 *       Specifies required permissions</li>
 *   <li>{@link sh.pcx.unified.commands.annotation.Cooldown @Cooldown} -
 *       Applies rate limiting</li>
 *   <li>{@link sh.pcx.unified.commands.annotation.Async @Async} -
 *       Executes command asynchronously</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Command(name = "warp", description = "Warp commands")
 * @Permission("warps.use")
 * public class WarpCommand {
 *
 *     @Subcommand("go")
 *     @Cooldown(value = 10, unit = TimeUnit.SECONDS)
 *     public void warpTo(
 *         @Sender Player player,
 *         @Arg("name") @Completions("@warps") String warpName
 *     ) {
 *         // Teleport to warp
 *     }
 *
 *     @Subcommand("set")
 *     @Permission("warps.set")
 *     @Async
 *     public void setWarp(
 *         @Sender Player player,
 *         @Arg("name") String warpName
 *     ) {
 *         // Save warp location (async for database)
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.commands.core.CommandService
 */
package sh.pcx.unified.commands.annotation;
