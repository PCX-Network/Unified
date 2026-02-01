/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents the result of validating a configuration.
 *
 * <p>ValidationResult contains all validation errors encountered during
 * validation, including the path to each invalid value and descriptive
 * error messages. It supports combining results from multiple validators
 * and provides convenient methods for error handling.</p>
 *
 * <h2>Checking Validation Results</h2>
 * <pre>{@code
 * ValidationResult result = configValidator.validate(config);
 *
 * if (result.isValid()) {
 *     logger.info("Configuration is valid!");
 * } else {
 *     logger.warning("Configuration has " + result.getErrorCount() + " errors:");
 *     result.getErrors().forEach(error ->
 *         logger.warning("  - " + error.getPath() + ": " + error.getMessage())
 *     );
 * }
 * }</pre>
 *
 * <h2>Handling Errors</h2>
 * <pre>{@code
 * result.ifValid(() -> {
 *     // Apply configuration
 *     applyConfig(config);
 * }).ifInvalid(errors -> {
 *     // Report errors
 *     errors.forEach(e -> sender.sendMessage("Error: " + e.getMessage()));
 * });
 * }</pre>
 *
 * <h2>Combining Results</h2>
 * <pre>{@code
 * ValidationResult result1 = validator.validate(part1);
 * ValidationResult result2 = validator.validate(part2);
 *
 * ValidationResult combined = result1.merge(result2);
 * // Or
 * ValidationResult combined = ValidationResult.merge(result1, result2, result3);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigValidator
 * @see ValidationConstraint
 */
public final class ValidationResult {

    private static final ValidationResult SUCCESS = new ValidationResult(List.of());

    private final List<ValidationError> errors;

    private ValidationResult(@NotNull List<ValidationError> errors) {
        this.errors = Collections.unmodifiableList(errors);
    }

    /**
     * Creates a successful validation result with no errors.
     *
     * @return a successful result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult success() {
        return SUCCESS;
    }

    /**
     * Creates a failed validation result with a single error.
     *
     * @param message the error message
     * @param path the path to the invalid value
     * @return a failed result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult failure(@NotNull String message, @NotNull String path) {
        return new ValidationResult(List.of(new ValidationError(message, path)));
    }

    /**
     * Creates a failed validation result with a single error.
     *
     * @param error the validation error
     * @return a failed result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult failure(@NotNull ValidationError error) {
        return new ValidationResult(List.of(error));
    }

    /**
     * Creates a validation result from a list of errors.
     *
     * @param errors the list of errors
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult of(@NotNull List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return SUCCESS;
        }
        return new ValidationResult(new ArrayList<>(errors));
    }

    /**
     * Merges multiple validation results into one.
     *
     * @param results the results to merge
     * @return the combined result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult merge(@NotNull ValidationResult... results) {
        List<ValidationError> allErrors = new ArrayList<>();
        for (ValidationResult result : results) {
            allErrors.addAll(result.errors);
        }
        return of(allErrors);
    }

    /**
     * Checks if the validation passed (no errors).
     *
     * @return true if valid
     * @since 1.0.0
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Checks if the validation failed (has errors).
     *
     * @return true if invalid
     * @since 1.0.0
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets the number of validation errors.
     *
     * @return the error count
     * @since 1.0.0
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Gets all validation errors.
     *
     * @return unmodifiable list of errors
     * @since 1.0.0
     */
    @NotNull
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * Gets the first validation error, if any.
     *
     * @return the first error, or empty
     * @since 1.0.0
     */
    @NotNull
    public Optional<ValidationError> getFirstError() {
        return errors.isEmpty() ? Optional.empty() : Optional.of(errors.get(0));
    }

    /**
     * Gets error messages for a specific path.
     *
     * @param path the configuration path
     * @return list of error messages for that path
     * @since 1.0.0
     */
    @NotNull
    public List<String> getErrorsForPath(@NotNull String path) {
        return errors.stream()
                .filter(e -> e.getPath().equals(path) || e.getPath().startsWith(path + "."))
                .map(ValidationError::getMessage)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a specific path has errors.
     *
     * @param path the configuration path
     * @return true if the path has errors
     * @since 1.0.0
     */
    public boolean hasErrorsForPath(@NotNull String path) {
        return errors.stream()
                .anyMatch(e -> e.getPath().equals(path) || e.getPath().startsWith(path + "."));
    }

    /**
     * Merges this result with another.
     *
     * @param other the other result
     * @return a new combined result
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult merge(@NotNull ValidationResult other) {
        if (this.isValid()) {
            return other;
        }
        if (other.isValid()) {
            return this;
        }
        List<ValidationError> combined = new ArrayList<>(this.errors.size() + other.errors.size());
        combined.addAll(this.errors);
        combined.addAll(other.errors);
        return new ValidationResult(combined);
    }

    /**
     * Adds an error to this result (returns a new result).
     *
     * @param message the error message
     * @param path the error path
     * @return a new result with the added error
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult withError(@NotNull String message, @NotNull String path) {
        List<ValidationError> newErrors = new ArrayList<>(errors.size() + 1);
        newErrors.addAll(errors);
        newErrors.add(new ValidationError(message, path));
        return new ValidationResult(newErrors);
    }

    /**
     * Executes an action if the validation passed.
     *
     * @param action the action to execute
     * @return this result for chaining
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult ifValid(@NotNull Runnable action) {
        if (isValid()) {
            action.run();
        }
        return this;
    }

    /**
     * Executes an action if the validation failed.
     *
     * @param action the action to execute with the errors
     * @return this result for chaining
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult ifInvalid(@NotNull Consumer<List<ValidationError>> action) {
        if (hasErrors()) {
            action.accept(errors);
        }
        return this;
    }

    /**
     * Throws an exception if validation failed.
     *
     * @throws sh.pcx.unified.config.ConfigException if there are errors
     * @since 1.0.0
     */
    public void throwIfInvalid() {
        if (hasErrors()) {
            throw new sh.pcx.unified.config.ConfigException(
                    "Configuration validation failed with " + errors.size() + " error(s)",
                    null,
                    errors.stream().map(ValidationError::getMessage).collect(Collectors.toList())
            );
        }
    }

    /**
     * Gets a formatted summary of all errors.
     *
     * @return the error summary
     * @since 1.0.0
     */
    @NotNull
    public String getSummary() {
        if (isValid()) {
            return "Validation passed";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Validation failed with ").append(errors.size()).append(" error(s):\n");
        for (ValidationError error : errors) {
            sb.append("  - [").append(error.getPath()).append("]: ");
            sb.append(error.getMessage()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return isValid() ? "ValidationResult{valid=true}" :
                "ValidationResult{valid=false, errorCount=" + errors.size() + "}";
    }

    /**
     * Represents a single validation error.
     */
    public static final class ValidationError {

        private final String message;
        private final String path;
        private final String suggestion;

        /**
         * Creates a validation error.
         *
         * @param message the error message
         * @param path the path to the invalid value
         */
        public ValidationError(@NotNull String message, @NotNull String path) {
            this(message, path, null);
        }

        /**
         * Creates a validation error with a suggestion.
         *
         * @param message the error message
         * @param path the path to the invalid value
         * @param suggestion a suggestion for fixing the error
         */
        public ValidationError(@NotNull String message, @NotNull String path, @Nullable String suggestion) {
            this.message = message;
            this.path = path;
            this.suggestion = suggestion;
        }

        /**
         * Gets the error message.
         *
         * @return the message
         */
        @NotNull
        public String getMessage() {
            return message;
        }

        /**
         * Gets the path to the invalid value.
         *
         * @return the path
         */
        @NotNull
        public String getPath() {
            return path;
        }

        /**
         * Gets a suggestion for fixing the error.
         *
         * @return the suggestion, or null
         */
        @Nullable
        public String getSuggestion() {
            return suggestion;
        }

        /**
         * Gets the full error string including path.
         *
         * @return the formatted error string
         */
        @NotNull
        public String getFullMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(path).append("]: ").append(message);
            if (suggestion != null) {
                sb.append(" (").append(suggestion).append(")");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return getFullMessage();
        }
    }
}
