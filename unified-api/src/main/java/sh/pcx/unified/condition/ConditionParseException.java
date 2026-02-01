/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when a condition expression cannot be parsed.
 *
 * <p>This exception provides details about the parsing failure, including
 * the position in the expression where the error occurred.</p>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition#expression(String)
 * @see ConditionParser
 */
public class ConditionParseException extends RuntimeException {

    private final @Nullable String expression;
    private final int position;

    /**
     * Constructs a new parse exception with a message.
     *
     * @param message the error message
     * @since 1.0.0
     */
    public ConditionParseException(@NotNull String message) {
        super(message);
        this.expression = null;
        this.position = -1;
    }

    /**
     * Constructs a new parse exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     * @since 1.0.0
     */
    public ConditionParseException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.expression = null;
        this.position = -1;
    }

    /**
     * Constructs a new parse exception with details about the expression.
     *
     * @param message    the error message
     * @param expression the expression that failed to parse
     * @param position   the position in the expression where the error occurred
     * @since 1.0.0
     */
    public ConditionParseException(@NotNull String message, @NotNull String expression, int position) {
        super(formatMessage(message, expression, position));
        this.expression = expression;
        this.position = position;
    }

    /**
     * Constructs a new parse exception with details and a cause.
     *
     * @param message    the error message
     * @param expression the expression that failed to parse
     * @param position   the position in the expression where the error occurred
     * @param cause      the underlying cause
     * @since 1.0.0
     */
    public ConditionParseException(
            @NotNull String message,
            @NotNull String expression,
            int position,
            @NotNull Throwable cause
    ) {
        super(formatMessage(message, expression, position), cause);
        this.expression = expression;
        this.position = position;
    }

    /**
     * Returns the expression that failed to parse.
     *
     * @return the expression, or null if not available
     * @since 1.0.0
     */
    @Nullable
    public String getExpression() {
        return expression;
    }

    /**
     * Returns the position in the expression where the error occurred.
     *
     * @return the error position, or -1 if not available
     * @since 1.0.0
     */
    public int getPosition() {
        return position;
    }

    /**
     * Formats the error message with expression context.
     */
    private static String formatMessage(String message, String expression, int position) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        sb.append("\n  Expression: ").append(expression);
        if (position >= 0 && position <= expression.length()) {
            sb.append("\n  Position:   ");
            sb.append(" ".repeat(position));
            sb.append("^");
        }
        return sb.toString();
    }
}
