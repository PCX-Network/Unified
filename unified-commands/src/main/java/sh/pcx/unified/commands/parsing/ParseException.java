/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.parsing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when argument parsing fails.
 *
 * <p>This exception is used by {@link ArgumentParser} implementations to indicate
 * that an input string could not be converted to the expected type. The message
 * is displayed to the command sender.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Exception</h3>
 * <pre>{@code
 * if (player == null) {
 *     throw new ParseException("Player not found: " + input);
 * }
 * }</pre>
 *
 * <h3>With Input Reference</h3>
 * <pre>{@code
 * throw new ParseException("Invalid number", input);
 * // Message: "Invalid number: <input>"
 * }</pre>
 *
 * <h3>With Suggestions</h3>
 * <pre>{@code
 * throw new ParseException("Unknown gamemode", input)
 *     .withSuggestions("survival", "creative", "adventure", "spectator");
 * }</pre>
 *
 * <h3>With Cause</h3>
 * <pre>{@code
 * try {
 *     return Integer.parseInt(input);
 * } catch (NumberFormatException e) {
 *     throw new ParseException("Invalid number format", input, e);
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 */
public class ParseException extends Exception {

    private final String input;
    private String[] suggestions;

    /**
     * Creates a new parse exception with a message.
     *
     * @param message the error message
     */
    public ParseException(@NotNull String message) {
        super(message);
        this.input = null;
    }

    /**
     * Creates a new parse exception with a message and input.
     *
     * @param message the error message
     * @param input the input that failed to parse
     */
    public ParseException(@NotNull String message, @Nullable String input) {
        super(input != null ? message + ": " + input : message);
        this.input = input;
    }

    /**
     * Creates a new parse exception with a message, input, and cause.
     *
     * @param message the error message
     * @param input the input that failed to parse
     * @param cause the underlying cause
     */
    public ParseException(@NotNull String message, @Nullable String input, @Nullable Throwable cause) {
        super(input != null ? message + ": " + input : message, cause);
        this.input = input;
    }

    /**
     * Creates a new parse exception with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ParseException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.input = null;
    }

    /**
     * Gets the input string that failed to parse.
     *
     * @return the input string, or {@code null} if not provided
     */
    @Nullable
    public String getInput() {
        return input;
    }

    /**
     * Gets suggested valid values.
     *
     * @return array of suggestions, or {@code null} if none
     */
    @Nullable
    public String[] getSuggestions() {
        return suggestions;
    }

    /**
     * Adds suggestions for valid values.
     *
     * <p>These suggestions are displayed to the user to help them
     * correct their input.</p>
     *
     * <pre>{@code
     * throw new ParseException("Unknown color", input)
     *     .withSuggestions("red", "green", "blue");
     * }</pre>
     *
     * @param suggestions the valid values
     * @return this exception for chaining
     */
    @NotNull
    public ParseException withSuggestions(@NotNull String... suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    /**
     * Creates a formatted error message suitable for display.
     *
     * <p>Includes suggestions if available.</p>
     *
     * @return the formatted error message
     */
    @NotNull
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder(getMessage());
        if (suggestions != null && suggestions.length > 0) {
            sb.append(" (valid values: ");
            sb.append(String.join(", ", suggestions));
            sb.append(")");
        }
        return sb.toString();
    }
}
