/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */

/**
 * Tab completion components for the command framework.
 *
 * <p>This package provides interfaces and classes for implementing
 * context-aware tab completion. Completion providers can be registered
 * and referenced in commands using the {@code @Completions} annotation.</p>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.commands.completion.TabCompleter} -
 *       Main interface for generating completions</li>
 *   <li>{@link sh.pcx.unified.commands.completion.CompletionProvider} -
 *       Interface for custom completion providers</li>
 *   <li>{@link sh.pcx.unified.commands.completion.CompletionContext} -
 *       Context information for completions</li>
 * </ul>
 *
 * <h2>Built-in Providers</h2>
 * <table border="1">
 *   <tr><th>Provider</th><th>Description</th></tr>
 *   <tr><td>{@code @players}</td><td>Online player names</td></tr>
 *   <tr><td>{@code @worlds}</td><td>Loaded world names</td></tr>
 *   <tr><td>{@code @materials}</td><td>Material enum values</td></tr>
 *   <tr><td>{@code @gamemodes}</td><td>GameMode values</td></tr>
 *   <tr><td>{@code @empty}</td><td>No suggestions</td></tr>
 * </table>
 *
 * <h2>Custom Provider Example</h2>
 * <pre>{@code
 * // Register provider
 * commandService.registerCompletions("@warps", context -> {
 *     Player player = (Player) context.getSender();
 *
 *     // Filter by permission
 *     return warpManager.getWarps().stream()
 *         .filter(warp -> player.hasPermission("warps." + warp.getName()))
 *         .map(Warp::getName)
 *         .filter(name -> name.startsWith(context.getCurrentInput()))
 *         .collect(Collectors.toList());
 * });
 *
 * // Use in command
 * @Subcommand("go")
 * public void warpTo(
 *     @Sender Player player,
 *     @Arg("name") @Completions("@warps") String warpName
 * ) {
 *     // Tab complete shows filtered warp names
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.commands.annotation.Completions
 */
package sh.pcx.unified.commands.completion;
