/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for condition expression strings.
 *
 * <h2>Expression Syntax:</h2>
 * <ul>
 *   <li>{@code permission:node} - Permission check</li>
 *   <li>{@code world:name} - World check</li>
 *   <li>{@code region:name} - Region check</li>
 *   <li>{@code cron:expression} - Cron time check</li>
 *   <li>{@code placeholder:%name%>value} - Placeholder comparison</li>
 *   <li>{@code AND} - Logical AND</li>
 *   <li>{@code OR} - Logical OR</li>
 *   <li>{@code NOT} - Logical NOT</li>
 *   <li>{@code ()} - Parentheses for grouping</li>
 * </ul>
 *
 * <h2>Examples:</h2>
 * <pre>{@code
 * // Simple permission
 * "permission:admin.use"
 *
 * // Combined conditions
 * "permission:vip AND world:survival"
 *
 * // Complex expression with grouping
 * "(permission:vip OR placeholder:%level%>50) AND world:survival AND NOT region:pvp_arena"
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public final class ConditionParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\(|\\)|AND|OR|NOT|" +
            "permission:[\\w.]+|" +
            "world:[\\w,]+|" +
            "region:[\\w]+|" +
            "cron:[^)]+|" +
            "placeholder:%[^%]+%[<>=!]+[^)\\s]+|" +
            "[\\w]+:[^)\\s]+"
    );

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "placeholder:(%[^%]+%)([<>=!]+)(.+)"
    );

    private ConditionParser() {}

    /**
     * Parses a condition expression string.
     *
     * @param expression the expression to parse
     * @return the parsed condition
     * @throws ConditionParseException if parsing fails
     * @since 1.0.0
     */
    @NotNull
    public static Condition parse(@NotNull String expression) {
        if (expression == null || expression.isBlank()) {
            throw new ConditionParseException("Expression cannot be null or empty");
        }

        List<Token> tokens = tokenize(expression);
        if (tokens.isEmpty()) {
            throw new ConditionParseException("No valid tokens found in expression: " + expression);
        }

        return parseExpression(tokens, 0, tokens.size());
    }

    private static List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(expression);

        while (matcher.find()) {
            String value = matcher.group().trim();
            int position = matcher.start();

            if (value.equals("(")) {
                tokens.add(new Token(TokenType.LPAREN, value, position));
            } else if (value.equals(")")) {
                tokens.add(new Token(TokenType.RPAREN, value, position));
            } else if (value.equals("AND")) {
                tokens.add(new Token(TokenType.AND, value, position));
            } else if (value.equals("OR")) {
                tokens.add(new Token(TokenType.OR, value, position));
            } else if (value.equals("NOT")) {
                tokens.add(new Token(TokenType.NOT, value, position));
            } else {
                tokens.add(new Token(TokenType.CONDITION, value, position));
            }
        }

        return tokens;
    }

    private static Condition parseExpression(List<Token> tokens, int start, int end) {
        if (start >= end) {
            throw new ConditionParseException("Empty expression");
        }

        // Handle parentheses
        if (tokens.get(start).type == TokenType.LPAREN) {
            int depth = 1;
            int closeIndex = start + 1;
            while (closeIndex < end && depth > 0) {
                if (tokens.get(closeIndex).type == TokenType.LPAREN) depth++;
                else if (tokens.get(closeIndex).type == TokenType.RPAREN) depth--;
                closeIndex++;
            }
            if (depth != 0) {
                throw new ConditionParseException("Unmatched parentheses", tokens.get(start).value, tokens.get(start).position);
            }
            if (closeIndex == end) {
                return parseExpression(tokens, start + 1, closeIndex - 1);
            }
        }

        // Find lowest precedence operator (OR, then AND)
        int orIndex = findOperator(tokens, start, end, TokenType.OR);
        if (orIndex >= 0) {
            Condition left = parseExpression(tokens, start, orIndex);
            Condition right = parseExpression(tokens, orIndex + 1, end);
            return Condition.any(left, right);
        }

        int andIndex = findOperator(tokens, start, end, TokenType.AND);
        if (andIndex >= 0) {
            Condition left = parseExpression(tokens, start, andIndex);
            Condition right = parseExpression(tokens, andIndex + 1, end);
            return Condition.all(left, right);
        }

        // Handle NOT
        if (tokens.get(start).type == TokenType.NOT) {
            Condition inner = parseExpression(tokens, start + 1, end);
            return Condition.not(inner);
        }

        // Parse single condition
        if (end - start == 1 && tokens.get(start).type == TokenType.CONDITION) {
            return parseCondition(tokens.get(start));
        }

        throw new ConditionParseException("Cannot parse expression",
                tokens.get(start).value, tokens.get(start).position);
    }

    private static int findOperator(List<Token> tokens, int start, int end, TokenType type) {
        int depth = 0;
        for (int i = start; i < end; i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.LPAREN) depth++;
            else if (token.type == TokenType.RPAREN) depth--;
            else if (depth == 0 && token.type == type) {
                return i;
            }
        }
        return -1;
    }

    private static Condition parseCondition(Token token) {
        String value = token.value;
        int colonIndex = value.indexOf(':');

        if (colonIndex < 0) {
            throw new ConditionParseException("Invalid condition format: " + value, value, token.position);
        }

        String type = value.substring(0, colonIndex);
        String args = value.substring(colonIndex + 1);

        return switch (type) {
            case "permission" -> Condition.permission(args);
            case "world" -> {
                String[] worlds = args.split(",");
                yield Condition.world(worlds);
            }
            case "region" -> Condition.region(args);
            case "cron" -> Condition.cron(args);
            case "placeholder" -> parsePlaceholderCondition(value, token.position);
            default -> throw new ConditionParseException("Unknown condition type: " + type, value, token.position);
        };
    }

    private static Condition parsePlaceholderCondition(String value, int position) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new ConditionParseException("Invalid placeholder condition: " + value, value, position);
        }

        String placeholder = matcher.group(1);
        String operator = matcher.group(2);
        String compareValue = matcher.group(3);

        PlaceholderCondition.Builder builder = Condition.placeholder(placeholder);

        return switch (operator) {
            case "=", "==" -> builder.isEqualTo(compareValue);
            case "!=" -> builder.notEquals(compareValue);
            case ">" -> builder.greaterThan(parseNumber(compareValue));
            case ">=" -> builder.greaterThanOrEquals(parseNumber(compareValue));
            case "<" -> builder.lessThan(parseNumber(compareValue));
            case "<=" -> builder.lessThanOrEquals(parseNumber(compareValue));
            default -> throw new ConditionParseException("Unknown operator: " + operator, value, position);
        };
    }

    private static Number parseNumber(String value) {
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ConditionParseException("Invalid number: " + value);
        }
    }

    private enum TokenType {
        LPAREN, RPAREN, AND, OR, NOT, CONDITION
    }

    private record Token(TokenType type, String value, int position) {}
}
