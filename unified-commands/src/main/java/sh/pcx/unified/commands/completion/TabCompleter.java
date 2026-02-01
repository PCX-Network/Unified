/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.completion;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for providing tab completion for commands.
 *
 * <p>The {@code TabCompleter} processes tab completion requests and returns
 * a list of suggestions based on the current input and context. It integrates
 * with registered {@link CompletionProvider}s and argument parsers.</p>
 *
 * <h2>Completion Flow</h2>
 * <pre>
 * Tab Key Pressed
 *     │
 *     ▼
 * Build CompletionContext
 *     │
 *     ▼
 * Determine Current Argument
 *     │
 *     ├── Subcommand Position ──► Suggest Subcommands
 *     │
 *     └── Argument Position ──► Check @Completions
 *                                    │
 *                                    ├── Provider Found ──► Get Provider Suggestions
 *                                    │
 *                                    └── No Provider ──► Use Parser Suggestions
 *     │
 *     ▼
 * Filter by Current Input
 *     │
 *     ▼
 * Return Sorted Suggestions
 * </pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Getting Completions</h3>
 * <pre>{@code
 * TabCompleter completer = commandService.getCompleter();
 *
 * // Get completions for current input
 * List<String> suggestions = completer.complete(player, "warp ", 5);
 * // Returns: ["hub", "spawn", "shop", ...]
 *
 * // With partial input
 * List<String> suggestions = completer.complete(player, "warp h", 6);
 * // Returns: ["hub", "home", ...] (filtered by "h")
 * }</pre>
 *
 * <h3>Using CompletionContext</h3>
 * <pre>{@code
 * CompletionContext context = CompletionContext.builder()
 *     .sender(player)
 *     .commandLine("warp ")
 *     .cursorPosition(5)
 *     .build();
 *
 * List<String> suggestions = completer.complete(context);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CompletionProvider
 * @see CompletionContext
 */
public interface TabCompleter {

    /**
     * Generates tab completion suggestions.
     *
     * <p>The cursor position indicates where in the command line the user
     * is currently typing. Suggestions are filtered based on partial input.</p>
     *
     * <pre>{@code
     * // User typed "/warp h" with cursor at position 7
     * List<String> suggestions = completer.complete(player, "/warp h", 7);
     * // Returns warps starting with "h"
     * }</pre>
     *
     * @param sender the command sender
     * @param commandLine the full command line being typed
     * @param cursorPosition the cursor position in the command line
     * @return list of completion suggestions
     */
    @NotNull
    List<String> complete(@NotNull Object sender, @NotNull String commandLine, int cursorPosition);

    /**
     * Generates tab completion suggestions using a context.
     *
     * @param context the completion context
     * @return list of completion suggestions
     */
    @NotNull
    List<String> complete(@NotNull CompletionContext context);

    /**
     * Generates completions for a specific command and argument position.
     *
     * <pre>{@code
     * // Get completions for argument at index 0 of "warp" command
     * List<String> suggestions = completer.complete(player, "warp", 0, "h");
     * }</pre>
     *
     * @param sender the command sender
     * @param command the command name
     * @param argIndex the argument index (0-based)
     * @param currentArg the current partial argument
     * @return list of completion suggestions
     */
    @NotNull
    List<String> complete(
            @NotNull Object sender,
            @NotNull String command,
            int argIndex,
            @NotNull String currentArg
    );

    /**
     * Registers a completion provider.
     *
     * <p>Providers are referenced by key in the {@code @Completions} annotation.</p>
     *
     * <pre>{@code
     * completer.registerProvider("@warps", context -> {
     *     return warpManager.getWarpNames();
     * });
     * }</pre>
     *
     * @param key the provider key (should start with @)
     * @param provider the completion provider
     */
    void registerProvider(@NotNull String key, @NotNull CompletionProvider provider);

    /**
     * Unregisters a completion provider.
     *
     * @param key the provider key
     * @return {@code true} if the provider was removed
     */
    boolean unregisterProvider(@NotNull String key);

    /**
     * Gets a registered completion provider.
     *
     * @param key the provider key
     * @return the provider, or {@code null} if not found
     */
    CompletionProvider getProvider(@NotNull String key);

    /**
     * Checks if a provider is registered.
     *
     * @param key the provider key
     * @return {@code true} if registered
     */
    boolean hasProvider(@NotNull String key);

    /**
     * Sets the maximum number of suggestions to return.
     *
     * <p>Limits the number of suggestions to prevent overwhelming the client.
     * Default is typically 100.</p>
     *
     * @param limit the maximum suggestions
     */
    void setMaxSuggestions(int limit);

    /**
     * Gets the maximum number of suggestions.
     *
     * @return the suggestion limit
     */
    int getMaxSuggestions();

    /**
     * Enables or disables case-sensitive filtering.
     *
     * <p>When disabled (default), "h" matches "Hub", "home", "HELLO".</p>
     *
     * @param caseSensitive {@code true} for case-sensitive matching
     */
    void setCaseSensitive(boolean caseSensitive);

    /**
     * Checks if filtering is case-sensitive.
     *
     * @return {@code true} if case-sensitive
     */
    boolean isCaseSensitive();
}
