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
 * Parser for world arguments.
 *
 * <p>Parses world names into World objects. Only matches currently loaded worlds.</p>
 *
 * <h2>Special Values</h2>
 * <ul>
 *   <li>{@code @world} or {@code @current} - The sender's current world</li>
 *   <li>{@code @overworld} - The main overworld</li>
 *   <li>{@code @nether} - The nether dimension</li>
 *   <li>{@code @end} - The end dimension</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Commands</h3>
 * <pre>{@code
 * @Subcommand("time set")
 * public void setTime(
 *     @Sender CommandSender sender,
 *     @Arg("world") World world,
 *     @Arg("time") long time
 * ) {
 *     world.setTime(time);
 * }
 *
 * // Invoked as:
 * // /time set world 6000
 * // /time set world_nether 18000
 * // /time set @current 0
 * }</pre>
 *
 * <h3>With Current World Default</h3>
 * <pre>{@code
 * @Subcommand("weather")
 * public void setWeather(
 *     @Sender Player player,
 *     @Arg("type") String weatherType,
 *     @Arg("world") @Default("@current") World world
 * ) {
 *     // Defaults to player's current world
 * }
 * }</pre>
 *
 * <h2>Platform Implementation</h2>
 * <p>This is an abstract parser that requires platform-specific implementation
 * to access the world list. Paper/Spigot and Sponge implementations are
 * provided in their respective platform modules.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 */
public class WorldParser implements ArgumentParser<Object> {

    /**
     * Selector for the sender's current world.
     */
    public static final String CURRENT_SELECTOR = "@current";

    /**
     * Alias for current world selector.
     */
    public static final String WORLD_SELECTOR = "@world";

    /**
     * Selector for the overworld.
     */
    public static final String OVERWORLD_SELECTOR = "@overworld";

    /**
     * Selector for the nether.
     */
    public static final String NETHER_SELECTOR = "@nether";

    /**
     * Selector for the end.
     */
    public static final String END_SELECTOR = "@end";

    @Override
    @NotNull
    public Object parse(@NotNull CommandContext context, @NotNull String input) throws ParseException {
        String trimmed = input.trim();

        // Handle special selectors
        if (trimmed.equalsIgnoreCase(CURRENT_SELECTOR) || trimmed.equalsIgnoreCase(WORLD_SELECTOR)) {
            if (!context.isPlayer()) {
                throw new ParseException("@current can only be used by players");
            }
            // Platform-specific: get player's world
            throw new UnsupportedOperationException(
                    "WorldParser requires platform-specific implementation"
            );
        }

        // Platform-specific implementation would resolve the world
        throw new UnsupportedOperationException(
                "WorldParser requires platform-specific implementation. " +
                        "Use PaperWorldParser or SpongeWorldParser."
        );
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull CompletionContext context) {
        // Platform-specific implementation would return loaded world names
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public String getErrorMessage() {
        return "World not found: {input}";
    }

    /**
     * Checks if the input is a special selector.
     *
     * @param input the input to check
     * @return {@code true} if the input is a selector
     */
    public static boolean isSelector(@NotNull String input) {
        String lower = input.toLowerCase();
        return lower.equals(CURRENT_SELECTOR) || lower.equals(WORLD_SELECTOR)
                || lower.equals(OVERWORLD_SELECTOR) || lower.equals(NETHER_SELECTOR)
                || lower.equals(END_SELECTOR);
    }
}
