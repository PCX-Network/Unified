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
 * Parser for double (decimal) arguments.
 *
 * <p>Parses string input into double values. Supports standard decimal notation
 * with both positive and negative values.</p>
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li>Decimal: "3.14", "0.5", "100.0"</li>
 *   <li>Integer (coerced to double): "42", "100"</li>
 *   <li>Negative: "-5.5", "-100.25"</li>
 *   <li>Scientific notation: "1.5e10", "3e-5"</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Commands</h3>
 * <pre>{@code
 * @Subcommand("pay")
 * public void pay(
 *     @Sender Player player,
 *     @Arg("target") Player target,
 *     @Arg(value = "amount", min = 0.01) double amount
 * ) {
 *     economy.transfer(player, target, amount);
 * }
 * }</pre>
 *
 * <h3>Direct Usage</h3>
 * <pre>{@code
 * DoubleParser parser = new DoubleParser();
 * double value = parser.parse(context, "3.14"); // Returns 3.14
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 * @see IntegerParser
 */
public class DoubleParser implements ArgumentParser<Double> {

    private static final List<String> COMMON_SUGGESTIONS = Arrays.asList(
            "0.5", "1.0", "1.5", "2.0", "5.0", "10.0", "100.0"
    );

    @Override
    @NotNull
    public Double parse(@NotNull CommandContext context, @NotNull String input) throws ParseException {
        try {
            double value = Double.parseDouble(input.trim());

            // Check for special values that might not be desired
            if (Double.isNaN(value)) {
                throw new ParseException("NaN is not a valid value", input);
            }
            if (Double.isInfinite(value)) {
                throw new ParseException("Infinite values are not allowed", input);
            }

            return value;
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid decimal number", input, e);
        }
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull CompletionContext context) {
        String current = context.getCurrentInput();
        if (current.isEmpty()) {
            return COMMON_SUGGESTIONS;
        }

        // If input ends with a digit, suggest decimal point
        if (Character.isDigit(current.charAt(current.length() - 1)) && !current.contains(".")) {
            return Arrays.asList(current + ".0", current + ".5");
        }

        try {
            Double.parseDouble(current);
            return Collections.emptyList(); // Valid number, no need for suggestions
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }

    @Override
    @NotNull
    public String getErrorMessage() {
        return "Expected a decimal number, got: {input}";
    }

    @Override
    public Class<Double> getType() {
        return Double.class;
    }
}
