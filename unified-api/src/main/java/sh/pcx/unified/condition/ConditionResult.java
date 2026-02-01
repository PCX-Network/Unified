/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the result of evaluating a {@link Condition}.
 *
 * <p>A ConditionResult contains whether the condition passed or failed,
 * along with an optional reason explaining the result. Results are immutable
 * and can be safely shared.</p>
 *
 * <h2>Creating Results:</h2>
 * <pre>{@code
 * // Success
 * ConditionResult success = ConditionResult.success();
 * ConditionResult successWithReason = ConditionResult.success("Permission granted");
 *
 * // Failure
 * ConditionResult failure = ConditionResult.failure("Missing permission: admin");
 * ConditionResult failureNoReason = ConditionResult.failure();
 *
 * // From boolean
 * ConditionResult result = ConditionResult.of(hasPermission, "Permission check");
 * }</pre>
 *
 * <h2>Using Results:</h2>
 * <pre>{@code
 * ConditionResult result = condition.evaluate(context);
 *
 * if (result.passed()) {
 *     // Execute action
 * } else {
 *     String reason = result.getReason().orElse("Unknown reason");
 *     player.sendMessage(Component.text("Denied: " + reason));
 * }
 *
 * // Or use ifPassed/ifFailed
 * result.ifPassed(() -> executeAction());
 * result.ifFailed(reason -> notifyPlayer(reason));
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 * @see ConditionContext
 */
public sealed interface ConditionResult permits ConditionResult.Impl {

    /**
     * Cached success result with no reason.
     */
    ConditionResult SUCCESS = new Impl(true, null, null);

    /**
     * Cached failure result with no reason.
     */
    ConditionResult FAILURE = new Impl(false, null, null);

    /**
     * Returns whether the condition passed.
     *
     * @return true if the condition passed
     * @since 1.0.0
     */
    boolean passed();

    /**
     * Returns whether the condition failed.
     *
     * @return true if the condition failed
     * @since 1.0.0
     */
    default boolean failed() {
        return !passed();
    }

    /**
     * Returns the reason for the result.
     *
     * <p>For failures, this explains why the condition failed.
     * For successes, this may explain why it passed.</p>
     *
     * @return an optional containing the reason, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getReason();

    /**
     * Returns the reason, or a default value if none.
     *
     * @param defaultReason the default reason
     * @return the reason or default
     * @since 1.0.0
     */
    @NotNull
    String getReasonOr(@NotNull String defaultReason);

    /**
     * Returns the type of condition that produced this result.
     *
     * @return an optional containing the condition type, or empty if unknown
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getConditionType();

    /**
     * Executes an action if the condition passed.
     *
     * @param action the action to execute
     * @return this result for chaining
     * @since 1.0.0
     */
    @NotNull
    ConditionResult ifPassed(@NotNull Runnable action);

    /**
     * Executes an action if the condition failed.
     *
     * @param action the action to execute with the optional reason
     * @return this result for chaining
     * @since 1.0.0
     */
    @NotNull
    ConditionResult ifFailed(@NotNull Consumer<Optional<String>> action);

    /**
     * Maps the result using a function if passed.
     *
     * @param <T>    the result type
     * @param mapper the mapping function
     * @return an optional containing the mapped value if passed
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> map(@NotNull Supplier<T> mapper);

    /**
     * Returns a value based on whether the condition passed.
     *
     * @param <T>          the result type
     * @param ifPassed     the value if passed
     * @param ifFailed     the value if failed
     * @return the appropriate value
     * @since 1.0.0
     */
    @NotNull
    <T> T fold(@NotNull Supplier<T> ifPassed, @NotNull Function<Optional<String>, T> ifFailed);

    /**
     * Returns a new result with the condition type set.
     *
     * @param type the condition type
     * @return a new result with the type
     * @since 1.0.0
     */
    @NotNull
    ConditionResult withType(@NotNull String type);

    /**
     * Returns a new result with an additional reason prefix.
     *
     * @param prefix the prefix to add
     * @return a new result with the prefixed reason
     * @since 1.0.0
     */
    @NotNull
    ConditionResult withReasonPrefix(@NotNull String prefix);

    /**
     * Combines this result with another using AND logic.
     *
     * <p>The combined result passes only if both results passed.
     * If either failed, the first failure reason is used.</p>
     *
     * @param other the other result
     * @return the combined result
     * @since 1.0.0
     */
    @NotNull
    ConditionResult and(@NotNull ConditionResult other);

    /**
     * Combines this result with another using OR logic.
     *
     * <p>The combined result passes if either result passed.
     * If both failed, the first failure reason is used.</p>
     *
     * @param other the other result
     * @return the combined result
     * @since 1.0.0
     */
    @NotNull
    ConditionResult or(@NotNull ConditionResult other);

    /**
     * Negates this result.
     *
     * <p>A passing result becomes failing (with "Condition negated" reason)
     * and a failing result becomes passing.</p>
     *
     * @return the negated result
     * @since 1.0.0
     */
    @NotNull
    ConditionResult negate();

    // ==================== Static Factory Methods ====================

    /**
     * Creates a successful result with no reason.
     *
     * @return a success result
     * @since 1.0.0
     */
    @NotNull
    static ConditionResult success() {
        return SUCCESS;
    }

    /**
     * Creates a successful result with a reason.
     *
     * @param reason the success reason
     * @return a success result with the reason
     * @since 1.0.0
     */
    @NotNull
    static ConditionResult success(@NotNull String reason) {
        Objects.requireNonNull(reason, "reason cannot be null");
        return new Impl(true, reason, null);
    }

    /**
     * Creates a failed result with no reason.
     *
     * @return a failure result
     * @since 1.0.0
     */
    @NotNull
    static ConditionResult failure() {
        return FAILURE;
    }

    /**
     * Creates a failed result with a reason.
     *
     * @param reason the failure reason
     * @return a failure result with the reason
     * @since 1.0.0
     */
    @NotNull
    static ConditionResult failure(@NotNull String reason) {
        Objects.requireNonNull(reason, "reason cannot be null");
        return new Impl(false, reason, null);
    }

    /**
     * Creates a result from a boolean value.
     *
     * @param passed true for success, false for failure
     * @return the corresponding result
     * @since 1.0.0
     */
    @NotNull
    static ConditionResult of(boolean passed) {
        return passed ? SUCCESS : FAILURE;
    }

    /**
     * Creates a result from a boolean value with a reason.
     *
     * @param passed true for success, false for failure
     * @param reason the reason (used for both success and failure)
     * @return the corresponding result with reason
     * @since 1.0.0
     */
    @NotNull
    static ConditionResult of(boolean passed, @NotNull String reason) {
        Objects.requireNonNull(reason, "reason cannot be null");
        return new Impl(passed, reason, null);
    }

    /**
     * Creates a result from a boolean value with separate reasons.
     *
     * @param passed        true for success, false for failure
     * @param successReason the reason if passed
     * @param failureReason the reason if failed
     * @return the corresponding result
     * @since 1.0.0
     */
    @NotNull
    static ConditionResult of(boolean passed, @NotNull String successReason, @NotNull String failureReason) {
        return new Impl(passed, passed ? successReason : failureReason, null);
    }

    // ==================== Implementation ====================

    /**
     * Internal sealed implementation of ConditionResult.
     */
    record Impl(boolean passed, @Nullable String reason, @Nullable String conditionType) implements ConditionResult {

        @Override
        public @NotNull Optional<String> getReason() {
            return Optional.ofNullable(reason);
        }

        @Override
        public @NotNull String getReasonOr(@NotNull String defaultReason) {
            return reason != null ? reason : defaultReason;
        }

        @Override
        public @NotNull Optional<String> getConditionType() {
            return Optional.ofNullable(conditionType);
        }

        @Override
        public @NotNull ConditionResult ifPassed(@NotNull Runnable action) {
            Objects.requireNonNull(action, "action cannot be null");
            if (passed) {
                action.run();
            }
            return this;
        }

        @Override
        public @NotNull ConditionResult ifFailed(@NotNull Consumer<Optional<String>> action) {
            Objects.requireNonNull(action, "action cannot be null");
            if (!passed) {
                action.accept(getReason());
            }
            return this;
        }

        @Override
        public <T> @NotNull Optional<T> map(@NotNull Supplier<T> mapper) {
            Objects.requireNonNull(mapper, "mapper cannot be null");
            if (passed) {
                return Optional.ofNullable(mapper.get());
            }
            return Optional.empty();
        }

        @Override
        public <T> @NotNull T fold(@NotNull Supplier<T> ifPassed, @NotNull Function<Optional<String>, T> ifFailed) {
            Objects.requireNonNull(ifPassed, "ifPassed cannot be null");
            Objects.requireNonNull(ifFailed, "ifFailed cannot be null");
            return passed ? ifPassed.get() : ifFailed.apply(getReason());
        }

        @Override
        public @NotNull ConditionResult withType(@NotNull String type) {
            return new Impl(passed, reason, type);
        }

        @Override
        public @NotNull ConditionResult withReasonPrefix(@NotNull String prefix) {
            if (reason == null) {
                return new Impl(passed, prefix, conditionType);
            }
            return new Impl(passed, prefix + ": " + reason, conditionType);
        }

        @Override
        public @NotNull ConditionResult and(@NotNull ConditionResult other) {
            Objects.requireNonNull(other, "other cannot be null");
            if (!passed) {
                return this;
            }
            if (!other.passed()) {
                return other;
            }
            return SUCCESS;
        }

        @Override
        public @NotNull ConditionResult or(@NotNull ConditionResult other) {
            Objects.requireNonNull(other, "other cannot be null");
            if (passed) {
                return this;
            }
            if (other.passed()) {
                return other;
            }
            return this;
        }

        @Override
        public @NotNull ConditionResult negate() {
            if (passed) {
                return failure("Condition negated");
            }
            return success();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(passed ? "PASSED" : "FAILED");
            if (conditionType != null) {
                sb.append("[").append(conditionType).append("]");
            }
            if (reason != null) {
                sb.append(": ").append(reason);
            }
            return sb.toString();
        }
    }
}
