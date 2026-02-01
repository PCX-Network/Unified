/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A fluent builder for constructing complex conditions.
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * Condition condition = Condition.builder()
 *     .permission("myplugin.use")
 *     .and()
 *     .world("survival", "survival_nether")
 *     .or()
 *     .permission("myplugin.bypass")
 *     .build();
 *
 * // With parenthetical grouping
 * Condition complex = Condition.builder()
 *     .group(b -> b
 *         .permission("vip")
 *         .and()
 *         .timeRange(LocalTime.of(20, 0), LocalTime.of(6, 0)))
 *     .or()
 *     .permission("admin")
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public final class ConditionBuilder {

    private final List<Object> tokens = new ArrayList<>();
    private boolean expectingCondition = true;

    /**
     * Creates a new condition builder.
     */
    public ConditionBuilder() {}

    // ==================== Condition Methods ====================

    /**
     * Adds a permission condition.
     *
     * @param permission the permission node
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder permission(@NotNull String permission) {
        addCondition(Condition.permission(permission));
        return this;
    }

    /**
     * Adds a world condition.
     *
     * @param worlds the world names
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder world(@NotNull String... worlds) {
        addCondition(Condition.world(worlds));
        return this;
    }

    /**
     * Adds a region condition.
     *
     * @param region the region name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder region(@NotNull String region) {
        addCondition(Condition.region(region));
        return this;
    }

    /**
     * Adds a cron condition.
     *
     * @param cronExpression the cron expression
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder cron(@NotNull String cronExpression) {
        addCondition(Condition.cron(cronExpression));
        return this;
    }

    /**
     * Adds a time range condition.
     *
     * @param start the start time
     * @param end   the end time
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder timeRange(@NotNull LocalTime start, @NotNull LocalTime end) {
        addCondition(Condition.timeRange(start, end));
        return this;
    }

    /**
     * Adds a placeholder condition.
     *
     * @param placeholder the placeholder condition
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder placeholder(@NotNull PlaceholderCondition placeholder) {
        addCondition(placeholder);
        return this;
    }

    /**
     * Adds a custom condition.
     *
     * @param condition the condition to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder condition(@NotNull Condition condition) {
        addCondition(condition);
        return this;
    }

    /**
     * Adds an always-true condition.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder alwaysTrue() {
        addCondition(Condition.alwaysTrue());
        return this;
    }

    /**
     * Adds an always-false condition.
     *
     * @param reason the failure reason
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder alwaysFalse(@NotNull String reason) {
        addCondition(Condition.alwaysFalse(reason));
        return this;
    }

    // ==================== Operator Methods ====================

    /**
     * Adds an AND operator.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder and() {
        addOperator(Operator.AND);
        return this;
    }

    /**
     * Adds an OR operator.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder or() {
        addOperator(Operator.OR);
        return this;
    }

    /**
     * Negates the next condition.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder not() {
        if (!expectingCondition) {
            throw new IllegalStateException("NOT must be followed by a condition");
        }
        tokens.add(Operator.NOT);
        return this;
    }

    // ==================== Grouping Methods ====================

    /**
     * Adds a grouped sub-condition built by the provided configurator.
     *
     * @param configurator the configurator that builds the group
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ConditionBuilder group(@NotNull java.util.function.Consumer<ConditionBuilder> configurator) {
        Objects.requireNonNull(configurator, "configurator cannot be null");
        ConditionBuilder subBuilder = new ConditionBuilder();
        configurator.accept(subBuilder);
        addCondition(subBuilder.build());
        return this;
    }

    // ==================== Build Method ====================

    /**
     * Builds the final condition.
     *
     * @return the built condition
     * @throws IllegalStateException if the builder is in an invalid state
     * @since 1.0.0
     */
    @NotNull
    public Condition build() {
        if (tokens.isEmpty()) {
            throw new IllegalStateException("No conditions added");
        }
        if (expectingCondition) {
            throw new IllegalStateException("Expression ends with an operator");
        }

        // Process NOT operators first
        List<Object> processedTokens = processNotOperators();

        // Process AND operators (higher precedence)
        processedTokens = processOperator(processedTokens, Operator.AND);

        // Process OR operators (lower precedence)
        processedTokens = processOperator(processedTokens, Operator.OR);

        if (processedTokens.size() != 1 || !(processedTokens.getFirst() instanceof Condition)) {
            throw new IllegalStateException("Failed to build condition");
        }

        return (Condition) processedTokens.getFirst();
    }

    // ==================== Private Helper Methods ====================

    private void addCondition(Condition condition) {
        Objects.requireNonNull(condition, "condition cannot be null");
        if (!expectingCondition) {
            throw new IllegalStateException("Expected an operator, not a condition");
        }
        tokens.add(condition);
        expectingCondition = false;
    }

    private void addOperator(Operator operator) {
        if (expectingCondition) {
            throw new IllegalStateException("Expected a condition, not an operator");
        }
        tokens.add(operator);
        expectingCondition = true;
    }

    private List<Object> processNotOperators() {
        List<Object> result = new ArrayList<>();
        boolean nextIsNegated = false;

        for (Object token : tokens) {
            if (token == Operator.NOT) {
                nextIsNegated = !nextIsNegated;
            } else if (token instanceof Condition condition) {
                if (nextIsNegated) {
                    result.add(Condition.not(condition));
                    nextIsNegated = false;
                } else {
                    result.add(condition);
                }
            } else {
                result.add(token);
            }
        }

        return result;
    }

    private List<Object> processOperator(List<Object> tokens, Operator operator) {
        List<Object> result = new ArrayList<>();

        int i = 0;
        while (i < tokens.size()) {
            Object current = tokens.get(i);

            if (i + 2 < tokens.size() && tokens.get(i + 1) == operator) {
                // Found matching operator, combine conditions
                List<Condition> toMerge = new ArrayList<>();
                toMerge.add((Condition) current);

                while (i + 2 < tokens.size() && tokens.get(i + 1) == operator) {
                    toMerge.add((Condition) tokens.get(i + 2));
                    i += 2;
                }

                Condition merged = operator == Operator.AND
                        ? Condition.all(toMerge)
                        : Condition.any(toMerge);
                result.add(merged);
            } else {
                result.add(current);
            }
            i++;
        }

        return result;
    }

    private enum Operator {
        AND, OR, NOT
    }
}
