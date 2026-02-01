/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.input;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Validator interface for validating user input before accepting.
 *
 * <p>Validators are used to check if input meets certain criteria before
 * the input dialog is closed. If validation fails, the player can be
 * shown an error message and allowed to correct their input.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple length validation
 * InputValidator<String> lengthValidator = InputValidator.minLength(3);
 *
 * // Pattern validation
 * InputValidator<String> alphanumeric = InputValidator.pattern("[a-zA-Z0-9]+");
 *
 * // Custom validation with error message
 * InputValidator<String> customValidator = InputValidator.of(
 *     text -> text.length() >= 3 && text.length() <= 16,
 *     Component.text("Name must be 3-16 characters")
 * );
 *
 * // Chained validators
 * InputValidator<String> combined = InputValidator.<String>notEmpty()
 *     .and(InputValidator.maxLength(32))
 *     .and(InputValidator.pattern("[a-zA-Z ]+"));
 *
 * // Number range validation
 * InputValidator<Integer> rangeValidator = InputValidator.inRange(1, 100);
 *
 * // Use in builder
 * AnvilInput.builder()
 *     .validator(customValidator)
 *     .onComplete(result -> handleResult(result))
 *     .open(player);
 * }</pre>
 *
 * <h2>Validation Result</h2>
 * <p>Validators return a {@link ValidationResult} that indicates:
 * <ul>
 *   <li>Whether the input is valid</li>
 *   <li>An optional error message to display on failure</li>
 * </ul>
 *
 * @param <T> the type of value to validate
 * @since 1.0.0
 * @author Supatuck
 */
@FunctionalInterface
public interface InputValidator<T> {

    /**
     * Validates the given input.
     *
     * @param input the input to validate
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    ValidationResult validate(@NotNull T input);

    /**
     * Creates a validator that always passes.
     *
     * @param <T> the input type
     * @return a validator that always returns valid
     * @since 1.0.0
     */
    @NotNull
    static <T> InputValidator<T> alwaysValid() {
        return input -> ValidationResult.valid();
    }

    /**
     * Creates a validator from a predicate.
     *
     * @param <T>       the input type
     * @param predicate the validation predicate
     * @return a validator based on the predicate
     * @since 1.0.0
     */
    @NotNull
    static <T> InputValidator<T> of(@NotNull Predicate<T> predicate) {
        return input -> predicate.test(input)
                ? ValidationResult.valid()
                : ValidationResult.invalid();
    }

    /**
     * Creates a validator from a predicate with a custom error message.
     *
     * @param <T>          the input type
     * @param predicate    the validation predicate
     * @param errorMessage the error message when validation fails
     * @return a validator based on the predicate
     * @since 1.0.0
     */
    @NotNull
    static <T> InputValidator<T> of(@NotNull Predicate<T> predicate, @NotNull Component errorMessage) {
        return input -> predicate.test(input)
                ? ValidationResult.valid()
                : ValidationResult.invalid(errorMessage);
    }

    /**
     * Creates a validator that checks for non-empty strings.
     *
     * @return a validator that rejects empty or whitespace-only strings
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> notEmpty() {
        return input -> input != null && !input.trim().isEmpty()
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Input cannot be empty"));
    }

    /**
     * Creates a validator for minimum string length.
     *
     * @param minLength the minimum length (inclusive)
     * @return a length validator
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> minLength(int minLength) {
        return input -> input.length() >= minLength
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Must be at least " + minLength + " characters"));
    }

    /**
     * Creates a validator for maximum string length.
     *
     * @param maxLength the maximum length (inclusive)
     * @return a length validator
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> maxLength(int maxLength) {
        return input -> input.length() <= maxLength
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Must be at most " + maxLength + " characters"));
    }

    /**
     * Creates a validator for string length range.
     *
     * @param minLength the minimum length (inclusive)
     * @param maxLength the maximum length (inclusive)
     * @return a length validator
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> lengthBetween(int minLength, int maxLength) {
        return input -> input.length() >= minLength && input.length() <= maxLength
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Must be " + minLength + "-" + maxLength + " characters"));
    }

    /**
     * Creates a validator that matches a regex pattern.
     *
     * @param regex the regex pattern
     * @return a pattern validator
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> pattern(@NotNull String regex) {
        Pattern compiled = Pattern.compile(regex);
        return input -> compiled.matcher(input).matches()
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Invalid format"));
    }

    /**
     * Creates a validator that matches a regex pattern with custom error message.
     *
     * @param regex        the regex pattern
     * @param errorMessage the error message on failure
     * @return a pattern validator
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> pattern(@NotNull String regex, @NotNull Component errorMessage) {
        Pattern compiled = Pattern.compile(regex);
        return input -> compiled.matcher(input).matches()
                ? ValidationResult.valid()
                : ValidationResult.invalid(errorMessage);
    }

    /**
     * Creates a validator for alphanumeric strings.
     *
     * @return an alphanumeric validator
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> alphanumeric() {
        return pattern("[a-zA-Z0-9]+", Component.text("Only letters and numbers allowed"));
    }

    /**
     * Creates a validator for numeric strings.
     *
     * @return a numeric string validator
     * @since 1.0.0
     */
    @NotNull
    static InputValidator<String> numeric() {
        return pattern("-?\\d+", Component.text("Must be a number"));
    }

    /**
     * Creates a validator for numbers within a range.
     *
     * @param <T> the number type
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a range validator
     * @since 1.0.0
     */
    @NotNull
    static <T extends Number & Comparable<T>> InputValidator<T> inRange(@NotNull T min, @NotNull T max) {
        return input -> input.compareTo(min) >= 0 && input.compareTo(max) <= 0
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Must be between " + min + " and " + max));
    }

    /**
     * Creates a validator for numbers greater than or equal to a minimum.
     *
     * @param <T> the number type
     * @param min the minimum value (inclusive)
     * @return a minimum value validator
     * @since 1.0.0
     */
    @NotNull
    static <T extends Number & Comparable<T>> InputValidator<T> atLeast(@NotNull T min) {
        return input -> input.compareTo(min) >= 0
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Must be at least " + min));
    }

    /**
     * Creates a validator for numbers less than or equal to a maximum.
     *
     * @param <T> the number type
     * @param max the maximum value (inclusive)
     * @return a maximum value validator
     * @since 1.0.0
     */
    @NotNull
    static <T extends Number & Comparable<T>> InputValidator<T> atMost(@NotNull T max) {
        return input -> input.compareTo(max) <= 0
                ? ValidationResult.valid()
                : ValidationResult.invalid(Component.text("Must be at most " + max));
    }

    /**
     * Combines this validator with another using logical AND.
     *
     * <p>Both validators must pass for the combined validator to pass.
     * If this validator fails, the other is not evaluated.
     *
     * @param other the other validator
     * @return a combined validator
     * @since 1.0.0
     */
    @NotNull
    default InputValidator<T> and(@NotNull InputValidator<T> other) {
        return input -> {
            ValidationResult result = this.validate(input);
            if (!result.isValid()) {
                return result;
            }
            return other.validate(input);
        };
    }

    /**
     * Combines this validator with another using logical OR.
     *
     * <p>At least one validator must pass for the combined validator to pass.
     * If this validator passes, the other is not evaluated.
     *
     * @param other the other validator
     * @return a combined validator
     * @since 1.0.0
     */
    @NotNull
    default InputValidator<T> or(@NotNull InputValidator<T> other) {
        return input -> {
            ValidationResult result = this.validate(input);
            if (result.isValid()) {
                return result;
            }
            return other.validate(input);
        };
    }

    /**
     * Negates this validator.
     *
     * @return a validator that passes when this one fails
     * @since 1.0.0
     */
    @NotNull
    default InputValidator<T> negate() {
        return input -> {
            ValidationResult result = this.validate(input);
            return result.isValid()
                    ? ValidationResult.invalid()
                    : ValidationResult.valid();
        };
    }

    /**
     * Result of a validation operation.
     *
     * @since 1.0.0
     */
    record ValidationResult(boolean isValid, @Nullable Component errorMessage) {

        /**
         * Creates a valid result.
         *
         * @return a valid result
         * @since 1.0.0
         */
        @NotNull
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        /**
         * Creates an invalid result without an error message.
         *
         * @return an invalid result
         * @since 1.0.0
         */
        @NotNull
        public static ValidationResult invalid() {
            return new ValidationResult(false, null);
        }

        /**
         * Creates an invalid result with an error message.
         *
         * @param errorMessage the error message to display
         * @return an invalid result
         * @since 1.0.0
         */
        @NotNull
        public static ValidationResult invalid(@NotNull Component errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        /**
         * Checks if this result represents valid input.
         *
         * @return true if the input is valid
         * @since 1.0.0
         */
        @Override
        public boolean isValid() {
            return isValid;
        }

        /**
         * Returns whether this result has an error message.
         *
         * @return true if an error message is present
         * @since 1.0.0
         */
        public boolean hasErrorMessage() {
            return errorMessage != null;
        }
    }
}
