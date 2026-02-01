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
import java.util.Collections;
import java.util.List;

/**
 * Parser for integer arguments.
 *
 * <p>Parses string input into integer values. Supports standard decimal notation
 * and provides validation for min/max constraints.</p>
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li>Positive integers: "42", "100", "999"</li>
 *   <li>Negative integers: "-5", "-100"</li>
 *   <li>Zero: "0"</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Commands</h3>
 * <pre>{@code
 * @Subcommand("give")
 * public void give(
 *     @Sender Player player,
 *     @Arg(value = "amount", min = 1, max = 64) int amount
 * ) {
 *     // amount is validated to be between 1 and 64
 * }
 * }</pre>
 *
 * <h3>Direct Usage</h3>
 * <pre>{@code
 * IntegerParser parser = new IntegerParser();
 * int value = parser.parse(context, "42"); // Returns 42
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 * @see DoubleParser
 */
public class IntegerParser implements ArgumentParser<Integer> {

    private static final List<String> COMMON_SUGGESTIONS = Arrays.asList(
            "1", "5", "10", "16", "32", "64"
    );

    @Override
    @NotNull
    public Integer parse(@NotNull CommandContext context, @NotNull String input) throws ParseException {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid integer", input, e);
        }
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull CompletionContext context) {
        String current = context.getCurrentInput();
        if (current.isEmpty()) {
            return COMMON_SUGGESTIONS;
        }

        // If input is a valid partial number, suggest completions
        try {
            Integer.parseInt(current);
            // Valid number, suggest with trailing zeros
            return Arrays.asList(current + "0", current + "00");
        } catch (NumberFormatException e) {
            // Invalid, filter suggestions
            return Collections.emptyList();
        }
    }

    @Override
    @NotNull
    public String getErrorMessage() {
        return "Expected an integer, got: {input}";
    }

    @Override
    public void validate(@NotNull Integer value, @NotNull CommandContext context) throws ParseException {
        // Additional validation can be applied based on @Arg constraints
        // This is handled by the framework using the annotation values
    }

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }
}
