/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base class for composite conditions that combine multiple conditions.
 *
 * <p>This sealed class provides implementations for AND, OR, NOT, and XOR
 * logical combinations of conditions.</p>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public sealed abstract class CompositeCondition implements Condition
        permits CompositeCondition.And, CompositeCondition.Or, CompositeCondition.Not, CompositeCondition.Xor {

    /**
     * Returns a list of all child conditions.
     *
     * @return the child conditions
     * @since 1.0.0
     */
    @NotNull
    public abstract List<Condition> getChildren();

    /**
     * AND condition - all conditions must pass.
     *
     * @since 1.0.0
     */
    public static final class And extends CompositeCondition {
        private final List<Condition> conditions;

        /**
         * Creates an AND condition.
         *
         * @param conditions the conditions to combine
         */
        public And(@NotNull List<Condition> conditions) {
            this.conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions cannot be null"));
            if (this.conditions.isEmpty()) {
                throw new IllegalArgumentException("At least one condition required");
            }
        }

        @Override
        public @NotNull List<Condition> getChildren() {
            return conditions;
        }

        @Override
        public @NotNull String getName() {
            return "and(" + conditions.stream()
                    .map(Condition::getName)
                    .collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public @NotNull String getType() {
            return "and";
        }

        @Override
        public @NotNull String getDescription() {
            return "All of: " + conditions.stream()
                    .map(Condition::getDescription)
                    .collect(Collectors.joining("; "));
        }

        @Override
        public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
            for (Condition condition : conditions) {
                ConditionResult result = condition.evaluate(context);
                if (result.failed()) {
                    return result.withReasonPrefix("AND failed");
                }
            }
            return ConditionResult.success("All " + conditions.size() + " conditions passed");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            And and = (And) o;
            return Objects.equals(conditions, and.conditions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conditions);
        }

        @Override
        public String toString() {
            return "And{" + conditions + '}';
        }
    }

    /**
     * OR condition - any condition must pass.
     *
     * @since 1.0.0
     */
    public static final class Or extends CompositeCondition {
        private final List<Condition> conditions;

        /**
         * Creates an OR condition.
         *
         * @param conditions the conditions to combine
         */
        public Or(@NotNull List<Condition> conditions) {
            this.conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions cannot be null"));
            if (this.conditions.isEmpty()) {
                throw new IllegalArgumentException("At least one condition required");
            }
        }

        @Override
        public @NotNull List<Condition> getChildren() {
            return conditions;
        }

        @Override
        public @NotNull String getName() {
            return "or(" + conditions.stream()
                    .map(Condition::getName)
                    .collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public @NotNull String getType() {
            return "or";
        }

        @Override
        public @NotNull String getDescription() {
            return "Any of: " + conditions.stream()
                    .map(Condition::getDescription)
                    .collect(Collectors.joining("; "));
        }

        @Override
        public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
            StringBuilder failures = new StringBuilder();
            for (Condition condition : conditions) {
                ConditionResult result = condition.evaluate(context);
                if (result.passed()) {
                    return result.withReasonPrefix("OR passed");
                }
                if (!failures.isEmpty()) {
                    failures.append("; ");
                }
                failures.append(result.getReasonOr("unknown"));
            }
            return ConditionResult.failure("None of " + conditions.size() + " conditions passed: " + failures);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Or or = (Or) o;
            return Objects.equals(conditions, or.conditions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conditions);
        }

        @Override
        public String toString() {
            return "Or{" + conditions + '}';
        }
    }

    /**
     * NOT condition - negates a condition.
     *
     * @since 1.0.0
     */
    public static final class Not extends CompositeCondition {
        private final Condition condition;

        /**
         * Creates a NOT condition.
         *
         * @param condition the condition to negate
         */
        public Not(@NotNull Condition condition) {
            this.condition = Objects.requireNonNull(condition, "condition cannot be null");
        }

        /**
         * Returns the negated condition.
         *
         * @return the negated condition
         * @since 1.0.0
         */
        @NotNull
        public Condition getCondition() {
            return condition;
        }

        @Override
        public @NotNull List<Condition> getChildren() {
            return List.of(condition);
        }

        @Override
        public @NotNull String getName() {
            return "not(" + condition.getName() + ")";
        }

        @Override
        public @NotNull String getType() {
            return "not";
        }

        @Override
        public @NotNull String getDescription() {
            return "Not: " + condition.getDescription();
        }

        @Override
        public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
            ConditionResult result = condition.evaluate(context);
            if (result.passed()) {
                return ConditionResult.failure("NOT failed: " + condition.getName() + " was true");
            }
            return ConditionResult.success("NOT passed: " + condition.getName() + " was false");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Not not = (Not) o;
            return Objects.equals(condition, not.condition);
        }

        @Override
        public int hashCode() {
            return Objects.hash(condition);
        }

        @Override
        public String toString() {
            return "Not{" + condition + '}';
        }
    }

    /**
     * XOR condition - exactly one condition must pass.
     *
     * @since 1.0.0
     */
    public static final class Xor extends CompositeCondition {
        private final Condition first;
        private final Condition second;

        /**
         * Creates an XOR condition.
         *
         * @param first  the first condition
         * @param second the second condition
         */
        public Xor(@NotNull Condition first, @NotNull Condition second) {
            this.first = Objects.requireNonNull(first, "first cannot be null");
            this.second = Objects.requireNonNull(second, "second cannot be null");
        }

        /**
         * Returns the first condition.
         *
         * @return the first condition
         * @since 1.0.0
         */
        @NotNull
        public Condition getFirst() {
            return first;
        }

        /**
         * Returns the second condition.
         *
         * @return the second condition
         * @since 1.0.0
         */
        @NotNull
        public Condition getSecond() {
            return second;
        }

        @Override
        public @NotNull List<Condition> getChildren() {
            return List.of(first, second);
        }

        @Override
        public @NotNull String getName() {
            return "xor(" + first.getName() + ", " + second.getName() + ")";
        }

        @Override
        public @NotNull String getType() {
            return "xor";
        }

        @Override
        public @NotNull String getDescription() {
            return "Exactly one of: " + first.getDescription() + " or " + second.getDescription();
        }

        @Override
        public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
            boolean firstPassed = first.evaluate(context).passed();
            boolean secondPassed = second.evaluate(context).passed();

            if (firstPassed != secondPassed) {
                String which = firstPassed ? first.getName() : second.getName();
                return ConditionResult.success("XOR passed: exactly " + which + " was true");
            }

            if (firstPassed) {
                return ConditionResult.failure("XOR failed: both conditions were true");
            } else {
                return ConditionResult.failure("XOR failed: neither condition was true");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Xor xor = (Xor) o;
            return Objects.equals(first, xor.first) && Objects.equals(second, xor.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

        @Override
        public String toString() {
            return "Xor{first=" + first + ", second=" + second + '}';
        }
    }
}
