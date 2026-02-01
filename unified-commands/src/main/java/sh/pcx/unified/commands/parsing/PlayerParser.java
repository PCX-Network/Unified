/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.parsing;

import sh.pcx.unified.commands.completion.CompletionContext;
import sh.pcx.unified.commands.core.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Parser for online player arguments.
 *
 * <p>Parses player names into Player objects. Only matches online players.
 * For offline player lookup, use OfflinePlayerParser.</p>
 *
 * <h2>Special Values</h2>
 * <ul>
 *   <li>{@code @s} or {@code @self} - The command sender (if player)</li>
 *   <li>{@code @r} or {@code @random} - A random online player</li>
 *   <li>{@code @p} or {@code @nearest} - Nearest player to sender</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Commands</h3>
 * <pre>{@code
 * @Subcommand("teleport")
 * public void teleport(
 *     @Sender Player sender,
 *     @Arg("target") Player target
 * ) {
 *     sender.teleport(target.getLocation());
 * }
 *
 * // Invoked as:
 * // /teleport Steve
 * // /teleport @s (teleports to self)
 * // /teleport @nearest
 * }</pre>
 *
 * <h3>With Self Default</h3>
 * <pre>{@code
 * @Subcommand("heal")
 * public void heal(
 *     @Sender CommandSender sender,
 *     @Arg("target") @Default("@self") Player target
 * ) {
 *     // Defaults to sender if player
 *     target.setHealth(target.getMaxHealth());
 * }
 * }</pre>
 *
 * <h2>Platform Implementation</h2>
 * <p>This is an abstract parser that requires platform-specific implementation
 * to access the player list. Paper/Spigot and Sponge implementations are
 * provided in their respective platform modules.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 */
public class PlayerParser implements ArgumentParser<Object> {

    /**
     * Special selector for the command sender.
     */
    public static final String SELF_SELECTOR = "@self";

    /**
     * Alias for self selector.
     */
    public static final String SELF_SELECTOR_SHORT = "@s";

    /**
     * Selector for a random online player.
     */
    public static final String RANDOM_SELECTOR = "@random";

    /**
     * Alias for random selector.
     */
    public static final String RANDOM_SELECTOR_SHORT = "@r";

    /**
     * Selector for the nearest player.
     */
    public static final String NEAREST_SELECTOR = "@nearest";

    /**
     * Alias for nearest selector.
     */
    public static final String NEAREST_SELECTOR_SHORT = "@p";

    @Override
    @NotNull
    public Object parse(@NotNull CommandContext context, @NotNull String input) throws ParseException {
        String trimmed = input.trim();

        // Handle special selectors
        if (trimmed.equalsIgnoreCase(SELF_SELECTOR) || trimmed.equalsIgnoreCase(SELF_SELECTOR_SHORT)) {
            if (!context.isPlayer()) {
                throw new ParseException("@self can only be used by players");
            }
            return context.getSenderAsPlayer();
        }

        // Platform-specific implementation would resolve the player
        // This base implementation throws an error indicating platform support needed
        throw new UnsupportedOperationException(
                "PlayerParser requires platform-specific implementation. " +
                        "Use PaperPlayerParser or SpongePlayerParser."
        );
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull CompletionContext context) {
        // Platform-specific implementation would return online player names
        // Base implementation returns empty list
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public String getErrorMessage() {
        return "Player not found: {input}";
    }

    /**
     * Checks if the input is a special selector.
     *
     * @param input the input to check
     * @return {@code true} if the input is a selector
     */
    public static boolean isSelector(@NotNull String input) {
        String lower = input.toLowerCase();
        return lower.equals(SELF_SELECTOR) || lower.equals(SELF_SELECTOR_SHORT)
                || lower.equals(RANDOM_SELECTOR) || lower.equals(RANDOM_SELECTOR_SHORT)
                || lower.equals(NEAREST_SELECTOR) || lower.equals(NEAREST_SELECTOR_SHORT);
    }
}
