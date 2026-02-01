/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies custom tab completion suggestions for a command argument.
 *
 * <p>This annotation provides dynamic tab completion by referencing registered
 * completion providers. Providers are identified by keys prefixed with {@code @}
 * and are registered with the command service during initialization.</p>
 *
 * <h2>Built-in Completion Providers</h2>
 * <table border="1">
 *   <tr><th>Provider</th><th>Description</th></tr>
 *   <tr><td>{@code @players}</td><td>All online player names</td></tr>
 *   <tr><td>{@code @offlinePlayers}</td><td>All known player names</td></tr>
 *   <tr><td>{@code @worlds}</td><td>All loaded world names</td></tr>
 *   <tr><td>{@code @materials}</td><td>All Material enum values</td></tr>
 *   <tr><td>{@code @enchantments}</td><td>All enchantment names</td></tr>
 *   <tr><td>{@code @potionTypes}</td><td>All potion effect types</td></tr>
 *   <tr><td>{@code @gamemodes}</td><td>All GameMode values</td></tr>
 *   <tr><td>{@code @sounds}</td><td>All Sound enum values</td></tr>
 *   <tr><td>{@code @particles}</td><td>All Particle enum values</td></tr>
 *   <tr><td>{@code @biomes}</td><td>All biome names</td></tr>
 *   <tr><td>{@code @entityTypes}</td><td>All EntityType values</td></tr>
 *   <tr><td>{@code @range:min:max}</td><td>Integer range (e.g., @range:1:100)</td></tr>
 *   <tr><td>{@code @empty}</td><td>No suggestions</td></tr>
 *   <tr><td>{@code @nothing}</td><td>Disable auto-completion</td></tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Built-in Provider</h3>
 * <pre>{@code
 * @Subcommand("teleport")
 * public void teleport(
 *     @Sender Player sender,
 *     @Arg("target") @Completions("@players") String targetName
 * ) {
 *     // Tab completes with online player names
 * }
 * }</pre>
 *
 * <h3>Custom Provider</h3>
 * <pre>{@code
 * // Register during plugin init
 * commandService.registerCompletions("@warps", context -> {
 *     return warpManager.getWarpNames();
 * });
 *
 * // Use in command
 * @Subcommand("warp")
 * public void warp(
 *     @Sender Player player,
 *     @Arg("name") @Completions("@warps") String warpName
 * ) {
 *     warpManager.teleport(player, warpName);
 * }
 * }</pre>
 *
 * <h3>Multiple Providers</h3>
 * <pre>{@code
 * @Subcommand("give")
 * public void give(
 *     @Sender Player sender,
 *     @Arg("target") @Completions({"@players", "@self"}) String target,
 *     @Arg("item") @Completions("@materials") String material
 * ) {
 *     // Combines suggestions from multiple providers
 * }
 * }</pre>
 *
 * <h3>Static Suggestions with Provider</h3>
 * <pre>{@code
 * @Subcommand("mode")
 * public void setMode(
 *     @Sender Player sender,
 *     @Arg("mode") @Completions({"pvp", "pve", "creative", "@customModes"}) String mode
 * ) {
 *     // Static values combined with dynamic provider
 * }
 * }</pre>
 *
 * <h3>Context-Aware Completions</h3>
 * <pre>{@code
 * // Register context-aware provider
 * commandService.registerCompletions("@teamMembers", context -> {
 *     Player sender = context.getSender();
 *     Team team = teamManager.getTeam(sender);
 *     return team != null ? team.getMemberNames() : Collections.emptyList();
 * });
 *
 * @Subcommand("team kick")
 * public void kickMember(
 *     @Sender Player sender,
 *     @Arg("member") @Completions("@teamMembers") String memberName
 * ) {
 *     // Only suggests members of the sender's team
 * }
 * }</pre>
 *
 * <h3>Filtered Completions</h3>
 * <pre>{@code
 * // Filter by permission
 * commandService.registerCompletions("@adminPlayers", context -> {
 *     return Bukkit.getOnlinePlayers().stream()
 *         .filter(p -> p.hasPermission("admin"))
 *         .map(Player::getName)
 *         .collect(Collectors.toList());
 * });
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Arg
 * @see sh.pcx.unified.commands.completion.CompletionProvider
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Completions {

    /**
     * The completion provider keys or static suggestions.
     *
     * <p>Values starting with {@code @} reference registered completion providers.
     * Other values are treated as static suggestions.</p>
     *
     * <pre>{@code
     * @Completions("@players")                    // Single provider
     * @Completions({"@players", "@offlinePlayers"}) // Multiple providers
     * @Completions({"small", "medium", "large"})  // Static values
     * @Completions({"@players", "console"})       // Mixed
     * }</pre>
     *
     * @return array of provider keys or static suggestions
     */
    String[] value();

    /**
     * Whether to filter suggestions based on partial input.
     *
     * <p>When {@code true} (default), only suggestions that start with
     * the current input are shown. Set to {@code false} to show all suggestions.</p>
     *
     * @return {@code true} to filter by partial input
     */
    boolean filter() default true;

    /**
     * Whether suggestions are case-sensitive.
     *
     * <p>When {@code false} (default), filtering ignores case.
     * Set to {@code true} for case-sensitive filtering.</p>
     *
     * @return {@code true} for case-sensitive filtering
     */
    boolean caseSensitive() default false;

    /**
     * Maximum number of suggestions to show.
     *
     * <p>Limits the number of suggestions displayed to prevent overwhelming
     * the user. Set to -1 for no limit.</p>
     *
     * @return maximum suggestions, -1 for unlimited
     */
    int limit() default -1;
}
