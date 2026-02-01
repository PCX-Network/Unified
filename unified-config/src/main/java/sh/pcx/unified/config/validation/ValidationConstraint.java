/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for configuration value validation constraints.
 *
 * <p>ValidationConstraint defines the contract for validators that check
 * configuration values against specific rules. Implementations can be
 * registered with the {@link ConfigValidator} or used via the
 * {@link sh.pcx.unified.config.annotation.ConfigValidate} annotation.</p>
 *
 * <h2>Implementing a Constraint</h2>
 * <pre>{@code
 * public class PositiveNumberConstraint implements ValidationConstraint<Number> {
 *
 *     @Override
 *     public boolean isValid(Number value, String path) {
 *         return value != null && value.doubleValue() > 0;
 *     }
 *
 *     @Override
 *     public String getMessage(Number value, String path) {
 *         return String.format("Value at '%s' must be positive, got: %s", path, value);
 *     }
 *
 *     @Override
 *     public Class<Number> getType() {
 *         return Number.class;
 *     }
 * }
 * }</pre>
 *
 * <h2>Parameterized Constraints</h2>
 * <pre>{@code
 * public class LengthConstraint implements ValidationConstraint<String> {
 *
 *     private final int minLength;
 *     private final int maxLength;
 *
 *     public LengthConstraint(int minLength, int maxLength) {
 *         this.minLength = minLength;
 *         this.maxLength = maxLength;
 *     }
 *
 *     @Override
 *     public boolean isValid(String value, String path) {
 *         if (value == null) return minLength == 0;
 *         int len = value.length();
 *         return len >= minLength && len <= maxLength;
 *     }
 *
 *     @Override
 *     public String getMessage(String value, String path) {
 *         int len = value == null ? 0 : value.length();
 *         return String.format(
 *             "Value at '%s' must have length between %d and %d, got: %d",
 *             path, minLength, maxLength, len
 *         );
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of value this constraint validates
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigValidator
 * @see ValidationResult
 */
public interface ValidationConstraint<T> {

    /**
     * Validates a configuration value.
     *
     * @param value the value to validate (may be null)
     * @param path the path to the value in the configuration
     * @return true if the value is valid
     * @since 1.0.0
     */
    boolean isValid(@Nullable T value, @NotNull String path);

    /**
     * Gets an error message for an invalid value.
     *
     * <p>This method is only called when {@link #isValid} returns false.
     * The message should be descriptive and include the path and actual value.</p>
     *
     * @param value the invalid value
     * @param path the path to the value
     * @return a descriptive error message
     * @since 1.0.0
     */
    @NotNull
    String getMessage(@Nullable T value, @NotNull String path);

    /**
     * Gets the type of value this constraint validates.
     *
     * <p>Used for type checking before validation. Return Object.class
     * if the constraint can handle any type.</p>
     *
     * @return the value type class
     * @since 1.0.0
     */
    @NotNull
    default Class<?> getType() {
        return Object.class;
    }

    /**
     * Gets the name of this constraint.
     *
     * <p>Used for logging and error reporting.</p>
     *
     * @return the constraint name
     * @since 1.0.0
     */
    @NotNull
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Gets a brief description of what this constraint validates.
     *
     * @return the constraint description
     * @since 1.0.0
     */
    @NotNull
    default String getDescription() {
        return "Validates configuration value";
    }

    /**
     * Checks if this constraint should stop validation on failure.
     *
     * <p>When true, subsequent constraints are not checked if this one fails.
     * Useful for null-checks that should prevent further validation.</p>
     *
     * @return true to stop on failure
     * @since 1.0.0
     */
    default boolean stopOnFailure() {
        return false;
    }

    /**
     * Creates a ValidationResult for this constraint.
     *
     * @param value the value to validate
     * @param path the path to the value
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    default ValidationResult validate(@Nullable T value, @NotNull String path) {
        if (isValid(value, path)) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(getMessage(value, path), path);
        }
    }
}
