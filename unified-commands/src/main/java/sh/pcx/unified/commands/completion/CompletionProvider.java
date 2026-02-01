/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.completion;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Interface for providing dynamic tab completion suggestions.
 *
 * <p>Completion providers are registered with a key and referenced in command
 * methods using the {@link sh.pcx.unified.commands.annotation.Completions}
 * annotation. They provide context-aware suggestions based on the current
 * command state and sender.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Static Suggestions</h3>
 * <pre>{@code
 * // Using lambda
 * commandService.registerCompletions("@gamemodes", context ->
 *     List.of("survival", "creative", "adventure", "spectator")
 * );
 *
 * // In command
 * @Subcommand("gamemode")
 * public void setGamemode(
 *     @Sender Player player,
 *     @Arg("mode") @Completions("@gamemodes") String mode
 * ) {
 *     // Tab complete shows: survival, creative, adventure, spectator
 * }
 * }</pre>
 *
 * <h3>Dynamic Suggestions</h3>
 * <pre>{@code
 * commandService.registerCompletions("@warps", context -> {
 *     // Check sender permissions for VIP warps
 *     if (context.hasPermission("warps.vip")) {
 *         return warpManager.getAllWarps();
 *     }
 *     return warpManager.getPublicWarps();
 * });
 * }</pre>
 *
 * <h3>Context-Dependent Suggestions</h3>
 * <pre>{@code
 * commandService.registerCompletions("@teamMembers", context -> {
 *     // Get sender's team and return member names
 *     Player sender = (Player) context.getSender();
 *     Team team = teamManager.getTeam(sender);
 *     if (team == null) {
 *         return Collections.emptyList();
 *     }
 *     return team.getMembers().stream()
 *         .map(Player::getName)
 *         .collect(Collectors.toList());
 * });
 * }</pre>
 *
 * <h3>Argument-Based Suggestions</h3>
 * <pre>{@code
 * // Previous arguments affect suggestions
 * commandService.registerCompletions("@kitItems", context -> {
 *     // Get kit name from previous argument
 *     String kitName = context.getArg(0, "default");
 *     Kit kit = kitManager.getKit(kitName);
 *     if (kit == null) {
 *         return Collections.emptyList();
 *     }
 *     return kit.getItems().stream()
 *         .map(ItemStack::getType)
 *         .map(Material::name)
 *         .collect(Collectors.toList());
 * });
 * }</pre>
 *
 * <h3>Filtered Suggestions</h3>
 * <pre>{@code
 * commandService.registerCompletions("@onlinePlayers", context -> {
 *     String filter = context.getCurrentInput().toLowerCase();
 *     return Bukkit.getOnlinePlayers().stream()
 *         .map(Player::getName)
 *         .filter(name -> name.toLowerCase().startsWith(filter))
 *         .limit(20)  // Limit to prevent lag
 *         .collect(Collectors.toList());
 * });
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.commands.annotation.Completions
 * @see CompletionContext
 * @see TabCompleter
 */
@FunctionalInterface
public interface CompletionProvider {

    /**
     * Provides completion suggestions for the current context.
     *
     * <p>This method is called when the user presses Tab and the cursor
     * is at an argument position that references this provider.</p>
     *
     * <pre>{@code
     * @Override
     * public List<String> suggest(CompletionContext context) {
     *     return arenaManager.getArenaNames();
     * }
     * }</pre>
     *
     * @param context the completion context with sender, args, and input info
     * @return list of suggestion strings (never null)
     */
    @NotNull
    List<String> suggest(@NotNull CompletionContext context);

    /**
     * Creates a provider from a static collection.
     *
     * <pre>{@code
     * CompletionProvider provider = CompletionProvider.of("red", "green", "blue");
     * }</pre>
     *
     * @param values the static values
     * @return a provider returning the values
     */
    @NotNull
    static CompletionProvider of(@NotNull String... values) {
        List<String> list = List.of(values);
        return context -> list;
    }

    /**
     * Creates a provider from a collection.
     *
     * <pre>{@code
     * Set<String> modes = Set.of("pvp", "pve", "creative");
     * CompletionProvider provider = CompletionProvider.of(modes);
     * }</pre>
     *
     * @param values the values collection
     * @return a provider returning the values
     */
    @NotNull
    static CompletionProvider of(@NotNull Collection<String> values) {
        List<String> list = List.copyOf(values);
        return context -> list;
    }

    /**
     * Creates a provider that filters another provider's results.
     *
     * <pre>{@code
     * CompletionProvider filtered = CompletionProvider.filtered(
     *     baseProvider,
     *     suggestion -> suggestion.length() > 3
     * );
     * }</pre>
     *
     * @param delegate the underlying provider
     * @param filter the filter predicate
     * @return a filtered provider
     */
    @NotNull
    static CompletionProvider filtered(
            @NotNull CompletionProvider delegate,
            @NotNull java.util.function.Predicate<String> filter
    ) {
        return context -> delegate.suggest(context).stream()
                .filter(filter)
                .toList();
    }

    /**
     * Creates a provider that combines multiple providers.
     *
     * <pre>{@code
     * CompletionProvider combined = CompletionProvider.combine(
     *     playersProvider,
     *     specialSelectorsProvider
     * );
     * }</pre>
     *
     * @param providers the providers to combine
     * @return a combined provider
     */
    @NotNull
    static CompletionProvider combine(@NotNull CompletionProvider... providers) {
        return context -> {
            List<String> results = new java.util.ArrayList<>();
            for (CompletionProvider provider : providers) {
                results.addAll(provider.suggest(context));
            }
            return results;
        };
    }

    /**
     * Creates an empty provider that returns no suggestions.
     *
     * @return an empty provider
     */
    @NotNull
    static CompletionProvider empty() {
        return context -> List.of();
    }
}
