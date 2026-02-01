/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.parsing;

import sh.pcx.unified.commands.completion.CompletionContext;
import sh.pcx.unified.commands.core.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser for boolean arguments.
 *
 * <p>Parses string input into boolean values. Supports various representations
 * of true and false for user convenience.</p>
 *
 * <h2>Accepted True Values</h2>
 * <ul>
 *   <li>true, yes, on, 1, enable, enabled, y</li>
 * </ul>
 *
 * <h2>Accepted False Values</h2>
 * <ul>
 *   <li>false, no, off, 0, disable, disabled, n</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Commands</h3>
 * <pre>{@code
 * @Subcommand("fly")
 * public void setFlight(
 *     @Sender Player player,
 *     @Arg("enabled") boolean enabled
 * ) {
 *     player.setAllowFlight(enabled);
 * }
 *
 * // Can be invoked as:
 * // /fly true
 * // /fly yes
 * // /fly on
 * // /fly 1
 * }</pre>
 *
 * <h3>Toggle Pattern</h3>
 * <pre>{@code
 * @Subcommand("fly")
 * public void toggleFlight(
 *     @Sender Player player,
 *     @Arg("enabled") @Default("toggle") Boolean enabled
 * ) {
 *     // "toggle" would flip current state
 *     boolean newState = enabled != null ? enabled : !player.getAllowFlight();
 *     player.setAllowFlight(newState);
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 */
public class BooleanParser implements ArgumentParser<Boolean> {

    private static final Set<String> TRUE_VALUES = Set.of(
            "true", "yes", "on", "1", "enable", "enabled", "y"
    );

    private static final Set<String> FALSE_VALUES = Set.of(
            "false", "no", "off", "0", "disable", "disabled", "n"
    );

    private static final List<String> SUGGESTIONS = Arrays.asList(
            "true", "false", "yes", "no", "on", "off"
    );

    @Override
    @NotNull
    public Boolean parse(@NotNull CommandContext context, @NotNull String input) throws ParseException {
        String lower = input.trim().toLowerCase(Locale.ROOT);

        if (TRUE_VALUES.contains(lower)) {
            return Boolean.TRUE;
        }

        if (FALSE_VALUES.contains(lower)) {
            return Boolean.FALSE;
        }

        throw new ParseException("Invalid boolean value", input)
                .withSuggestions("true", "false", "yes", "no");
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull CompletionContext context) {
        String current = context.getCurrentInput().toLowerCase(Locale.ROOT);
        if (current.isEmpty()) {
            return SUGGESTIONS;
        }

        return SUGGESTIONS.stream()
                .filter(s -> s.startsWith(current))
                .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public String getErrorMessage() {
        return "Expected true/false, got: {input}";
    }

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }
}
