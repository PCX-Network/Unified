/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * A condition that evaluates placeholder values and compares them.
 *
 * <p>Placeholder conditions integrate with placeholder providers (like PlaceholderAPI)
 * to evaluate dynamic values and compare them against expected values.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Check if player level is greater than 50
 * Condition highLevel = Condition.placeholder("%player_level%")
 *     .greaterThan(50);
 *
 * // Check if player is in a faction
 * Condition inFaction = Condition.placeholder("%factions_faction_name%")
 *     .notEquals("Wilderness");
 *
 * // Check if player balance is at least 1000
 * Condition hasBalance = Condition.placeholder("%vault_eco_balance%")
 *     .greaterThanOrEquals(1000.0);
 *
 * // Check if placeholder contains a substring
 * Condition isVip = Condition.placeholder("%luckperms_prefix%")
 *     .contains("[VIP]");
 *
 * // Check if placeholder matches a pattern
 * Condition hasTag = Condition.placeholder("%player_displayname%")
 *     .matches(".*\\[Admin\\].*");
 *
 * // Combine with other conditions
 * Condition richVip = Condition.all(
 *     Condition.permission("group.vip"),
 *     Condition.placeholder("%vault_eco_balance%").greaterThan(10000)
 * );
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public interface PlaceholderCondition extends Condition {

    /**
     * Returns the placeholder string.
     *
     * @return the placeholder
     * @since 1.0.0
     */
    @NotNull
    String getPlaceholder();

    /**
     * Returns the comparison operator.
     *
     * @return the operator
     * @since 1.0.0
     */
    @NotNull
    ComparisonOperator getOperator();

    /**
     * Returns the value to compare against.
     *
     * @return the comparison value
     * @since 1.0.0
     */
    @NotNull
    Object getValue();

    @Override
    @NotNull
    default String getName() {
        return "placeholder:" + getPlaceholder() + getOperator().getSymbol() + getValue();
    }

    @Override
    @NotNull
    default String getType() {
        return "placeholder";
    }

    @Override
    @NotNull
    default String getDescription() {
        return getPlaceholder() + " " + getOperator().getDescription() + " " + getValue();
    }

    /**
     * Creates a builder for a placeholder condition.
     *
     * @param placeholder the placeholder string
     * @return a builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder(@NotNull String placeholder) {
        Objects.requireNonNull(placeholder, "placeholder cannot be null");
        return new Builder(placeholder);
    }

    /**
     * Comparison operators for placeholder conditions.
     *
     * @since 1.0.0
     */
    enum ComparisonOperator {
        EQUALS("==", "equals") {
            @Override
            public boolean compare(Object actual, Object expected) {
                return Objects.equals(actual, expected);
            }
        },
        NOT_EQUALS("!=", "does not equal") {
            @Override
            public boolean compare(Object actual, Object expected) {
                return !Objects.equals(actual, expected);
            }
        },
        GREATER_THAN(">", "is greater than") {
            @Override
            public boolean compare(Object actual, Object expected) {
                return compareNumbers(actual, expected) > 0;
            }
        },
        GREATER_THAN_OR_EQUALS(">=", "is greater than or equal to") {
            @Override
            public boolean compare(Object actual, Object expected) {
                return compareNumbers(actual, expected) >= 0;
            }
        },
        LESS_THAN("<", "is less than") {
            @Override
            public boolean compare(Object actual, Object expected) {
                return compareNumbers(actual, expected) < 0;
            }
        },
        LESS_THAN_OR_EQUALS("<=", "is less than or equal to") {
            @Override
            public boolean compare(Object actual, Object expected) {
                return compareNumbers(actual, expected) <= 0;
            }
        },
        CONTAINS("~=", "contains") {
            @Override
            public boolean compare(Object actual, Object expected) {
                if (actual == null) return false;
                return actual.toString().contains(expected.toString());
            }
        },
        STARTS_WITH("^=", "starts with") {
            @Override
            public boolean compare(Object actual, Object expected) {
                if (actual == null) return false;
                return actual.toString().startsWith(expected.toString());
            }
        },
        ENDS_WITH("$=", "ends with") {
            @Override
            public boolean compare(Object actual, Object expected) {
                if (actual == null) return false;
                return actual.toString().endsWith(expected.toString());
            }
        },
        MATCHES("*=", "matches pattern") {
            @Override
            public boolean compare(Object actual, Object expected) {
                if (actual == null) return false;
                return actual.toString().matches(expected.toString());
            }
        };

        private final String symbol;
        private final String description;

        ComparisonOperator(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        /**
         * Returns the symbol for this operator.
         *
         * @return the symbol
         * @since 1.0.0
         */
        public String getSymbol() {
            return symbol;
        }

        /**
         * Returns a description of this operator.
         *
         * @return the description
         * @since 1.0.0
         */
        public String getDescription() {
            return description;
        }

        /**
         * Compares two values using this operator.
         *
         * @param actual   the actual value
         * @param expected the expected value
         * @return true if the comparison passes
         * @since 1.0.0
         */
        public abstract boolean compare(Object actual, Object expected);

        /**
         * Compares two values as numbers.
         */
        protected static int compareNumbers(Object actual, Object expected) {
            if (actual == null || expected == null) {
                return actual == null && expected == null ? 0 : (actual == null ? -1 : 1);
            }
            try {
                double actualNum = Double.parseDouble(actual.toString());
                double expectedNum = Double.parseDouble(expected.toString());
                return Double.compare(actualNum, expectedNum);
            } catch (NumberFormatException e) {
                return actual.toString().compareTo(expected.toString());
            }
        }

        /**
         * Parses an operator from a symbol.
         *
         * @param symbol the symbol to parse
         * @return the operator
         * @throws IllegalArgumentException if the symbol is unknown
         * @since 1.0.0
         */
        @NotNull
        public static ComparisonOperator fromSymbol(@NotNull String symbol) {
            for (ComparisonOperator op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operator: " + symbol);
        }
    }

    /**
     * Builder for placeholder conditions.
     *
     * @since 1.0.0
     */
    final class Builder {
        private final String placeholder;

        Builder(String placeholder) {
            this.placeholder = placeholder;
        }

        /**
         * Creates a condition that checks equality.
         *
         * @param value the expected value
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition isEqualTo(@NotNull Object value) {
            return create(ComparisonOperator.EQUALS, value);
        }

        /**
         * Creates a condition that checks inequality.
         *
         * @param value the value that should not match
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition notEquals(@NotNull Object value) {
            return create(ComparisonOperator.NOT_EQUALS, value);
        }

        /**
         * Creates a condition that checks if greater than.
         *
         * @param value the threshold value
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition greaterThan(@NotNull Number value) {
            return create(ComparisonOperator.GREATER_THAN, value);
        }

        /**
         * Creates a condition that checks if greater than or equal.
         *
         * @param value the threshold value
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition greaterThanOrEquals(@NotNull Number value) {
            return create(ComparisonOperator.GREATER_THAN_OR_EQUALS, value);
        }

        /**
         * Creates a condition that checks if less than.
         *
         * @param value the threshold value
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition lessThan(@NotNull Number value) {
            return create(ComparisonOperator.LESS_THAN, value);
        }

        /**
         * Creates a condition that checks if less than or equal.
         *
         * @param value the threshold value
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition lessThanOrEquals(@NotNull Number value) {
            return create(ComparisonOperator.LESS_THAN_OR_EQUALS, value);
        }

        /**
         * Creates a condition that checks if the placeholder contains a substring.
         *
         * @param substring the substring to find
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition contains(@NotNull String substring) {
            return create(ComparisonOperator.CONTAINS, substring);
        }

        /**
         * Creates a condition that checks if the placeholder starts with a prefix.
         *
         * @param prefix the prefix to check
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition startsWith(@NotNull String prefix) {
            return create(ComparisonOperator.STARTS_WITH, prefix);
        }

        /**
         * Creates a condition that checks if the placeholder ends with a suffix.
         *
         * @param suffix the suffix to check
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition endsWith(@NotNull String suffix) {
            return create(ComparisonOperator.ENDS_WITH, suffix);
        }

        /**
         * Creates a condition that checks if the placeholder matches a regex pattern.
         *
         * @param pattern the regex pattern
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition matches(@NotNull String pattern) {
            return create(ComparisonOperator.MATCHES, pattern);
        }

        /**
         * Creates a condition with a custom comparator.
         *
         * @param comparator the comparator
         * @param value      the value to compare against
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        public PlaceholderCondition custom(@NotNull BiPredicate<String, Object> comparator, @NotNull Object value) {
            Objects.requireNonNull(comparator, "comparator cannot be null");
            Objects.requireNonNull(value, "value cannot be null");
            return new PlaceholderCondition() {
                @Override
                public @NotNull String getPlaceholder() {
                    return placeholder;
                }

                @Override
                public @NotNull ComparisonOperator getOperator() {
                    return ComparisonOperator.EQUALS; // Default for display
                }

                @Override
                public @NotNull Object getValue() {
                    return value;
                }

                @Override
                public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
                    // Actual placeholder resolution is done by the implementation
                    // in unified-world which has access to placeholder providers
                    return ConditionResult.failure("Placeholder resolution not available");
                }
            };
        }

        private PlaceholderCondition create(ComparisonOperator operator, Object value) {
            Objects.requireNonNull(value, "value cannot be null");
            return new PlaceholderCondition() {
                @Override
                public @NotNull String getPlaceholder() {
                    return placeholder;
                }

                @Override
                public @NotNull ComparisonOperator getOperator() {
                    return operator;
                }

                @Override
                public @NotNull Object getValue() {
                    return value;
                }

                @Override
                public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
                    // Actual placeholder resolution is done by the implementation
                    // in unified-world which has access to placeholder providers
                    return ConditionResult.failure("Placeholder resolution not available");
                }
            };
        }
    }
}
