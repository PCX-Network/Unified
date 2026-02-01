/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of a validation operation.
 *
 * <p>ValidationResult contains errors, warnings, and information messages
 * from validating import configurations, field mappings, or storage schemas.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ValidationResult result = migration.importFrom(importer).validate();
 *
 * if (result.isValid()) {
 *     // Proceed with import
 *     migration.importFrom(importer).execute();
 * } else {
 *     // Show validation errors
 *     for (ValidationMessage error : result.getErrors()) {
 *         log.error("Validation error: {} - {}", error.field(), error.message());
 *     }
 * }
 *
 * // Check warnings
 * for (ValidationMessage warning : result.getWarnings()) {
 *     log.warn("Warning: {}", warning.message());
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ImportBuilder
 * @see StorageMigrationBuilder
 */
public final class ValidationResult {

    private final List<ValidationMessage> errors;
    private final List<ValidationMessage> warnings;
    private final List<ValidationMessage> info;

    private ValidationResult(Builder builder) {
        this.errors = List.copyOf(builder.errors);
        this.warnings = List.copyOf(builder.warnings);
        this.info = List.copyOf(builder.info);
    }

    // ========================================================================
    // Status Checks
    // ========================================================================

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
     * Checks if there are any errors.
     *
     * @return true if there are errors
     * @since 1.0.0
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Checks if there are any warnings.
     *
     * @return true if there are warnings
     * @since 1.0.0
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Checks if there are any messages of any type.
     *
     * @return true if there are any messages
     * @since 1.0.0
     */
    public boolean hasMessages() {
        return !errors.isEmpty() || !warnings.isEmpty() || !info.isEmpty();
    }

    // ========================================================================
    // Messages
    // ========================================================================

    /**
     * Returns all error messages.
     *
     * @return the errors
     * @since 1.0.0
     */
    @NotNull
    public List<ValidationMessage> getErrors() {
        return errors;
    }

    /**
     * Returns all warning messages.
     *
     * @return the warnings
     * @since 1.0.0
     */
    @NotNull
    public List<ValidationMessage> getWarnings() {
        return warnings;
    }

    /**
     * Returns all informational messages.
     *
     * @return the info messages
     * @since 1.0.0
     */
    @NotNull
    public List<ValidationMessage> getInfo() {
        return info;
    }

    /**
     * Returns all messages of all types.
     *
     * @return all messages
     * @since 1.0.0
     */
    @NotNull
    public List<ValidationMessage> getAllMessages() {
        List<ValidationMessage> all = new ArrayList<>();
        all.addAll(errors);
        all.addAll(warnings);
        all.addAll(info);
        return all;
    }

    /**
     * Returns the error count.
     *
     * @return number of errors
     * @since 1.0.0
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Returns the warning count.
     *
     * @return number of warnings
     * @since 1.0.0
     */
    public int getWarningCount() {
        return warnings.size();
    }

    // ========================================================================
    // Combination
    // ========================================================================

    /**
     * Combines this result with another.
     *
     * @param other the other result
     * @return a combined result
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult combine(@NotNull ValidationResult other) {
        Builder builder = builder();
        builder.errors.addAll(this.errors);
        builder.errors.addAll(other.errors);
        builder.warnings.addAll(this.warnings);
        builder.warnings.addAll(other.warnings);
        builder.info.addAll(this.info);
        builder.info.addAll(other.info);
        return builder.build();
    }

    // ========================================================================
    // Formatting
    // ========================================================================

    /**
     * Returns a human-readable summary.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        if (isValid() && !hasWarnings()) {
            return "Validation passed";
        }

        StringBuilder sb = new StringBuilder();
        if (!errors.isEmpty()) {
            sb.append("Errors (").append(errors.size()).append("):\n");
            for (ValidationMessage error : errors) {
                sb.append("  - ").append(error.toDisplayString()).append("\n");
            }
        }
        if (!warnings.isEmpty()) {
            sb.append("Warnings (").append(warnings.size()).append("):\n");
            for (ValidationMessage warning : warnings) {
                sb.append("  - ").append(warning.toDisplayString()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + isValid() +
                ", errors=" + errors.size() +
                ", warnings=" + warnings.size() +
                '}';
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Creates a valid result with no messages.
     *
     * @return a valid result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult valid() {
        return builder().build();
    }

    /**
     * Creates an invalid result with a single error.
     *
     * @param message the error message
     * @return an invalid result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult error(@NotNull String message) {
        return builder().error(message).build();
    }

    /**
     * Creates an invalid result with a single error for a field.
     *
     * @param field   the field name
     * @param message the error message
     * @return an invalid result
     * @since 1.0.0
     */
    @NotNull
    public static ValidationResult error(@NotNull String field, @NotNull String message) {
        return builder().error(field, message).build();
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    // ========================================================================
    // Nested Types
    // ========================================================================

    /**
     * A validation message.
     *
     * @param level   the message level
     * @param field   the related field (may be null)
     * @param message the message text
     * @param code    an optional error code
     * @since 1.0.0
     */
    public record ValidationMessage(
            @NotNull Level level,
            String field,
            @NotNull String message,
            String code
    ) {
        public ValidationMessage {
            Objects.requireNonNull(level, "level cannot be null");
            Objects.requireNonNull(message, "message cannot be null");
        }

        /**
         * Creates an error message.
         *
         * @param message the message text
         * @return an error message
         */
        @NotNull
        public static ValidationMessage error(@NotNull String message) {
            return new ValidationMessage(Level.ERROR, null, message, null);
        }

        /**
         * Creates an error message for a field.
         *
         * @param field   the field name
         * @param message the message text
         * @return an error message
         */
        @NotNull
        public static ValidationMessage error(@NotNull String field, @NotNull String message) {
            return new ValidationMessage(Level.ERROR, field, message, null);
        }

        /**
         * Creates a warning message.
         *
         * @param message the message text
         * @return a warning message
         */
        @NotNull
        public static ValidationMessage warning(@NotNull String message) {
            return new ValidationMessage(Level.WARNING, null, message, null);
        }

        /**
         * Creates an info message.
         *
         * @param message the message text
         * @return an info message
         */
        @NotNull
        public static ValidationMessage info(@NotNull String message) {
            return new ValidationMessage(Level.INFO, null, message, null);
        }

        /**
         * Returns a display string.
         *
         * @return the display string
         */
        @NotNull
        public String toDisplayString() {
            if (field != null) {
                return field + ": " + message;
            }
            return message;
        }
    }

    /**
     * Message severity levels.
     *
     * @since 1.0.0
     */
    public enum Level {
        /** Informational message */
        INFO,
        /** Warning - operation can proceed but with caution */
        WARNING,
        /** Error - operation cannot proceed */
        ERROR
    }

    /**
     * Builder for ValidationResult.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final List<ValidationMessage> errors = new ArrayList<>();
        private final List<ValidationMessage> warnings = new ArrayList<>();
        private final List<ValidationMessage> info = new ArrayList<>();

        private Builder() {}

        /**
         * Adds an error message.
         *
         * @param message the error message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder error(@NotNull String message) {
            errors.add(ValidationMessage.error(message));
            return this;
        }

        /**
         * Adds an error message for a field.
         *
         * @param field   the field name
         * @param message the error message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder error(@NotNull String field, @NotNull String message) {
            errors.add(ValidationMessage.error(field, message));
            return this;
        }

        /**
         * Adds an error message with code.
         *
         * @param field   the field name
         * @param message the error message
         * @param code    the error code
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder error(String field, @NotNull String message, String code) {
            errors.add(new ValidationMessage(Level.ERROR, field, message, code));
            return this;
        }

        /**
         * Adds a warning message.
         *
         * @param message the warning message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder warning(@NotNull String message) {
            warnings.add(ValidationMessage.warning(message));
            return this;
        }

        /**
         * Adds a warning message for a field.
         *
         * @param field   the field name
         * @param message the warning message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder warning(@NotNull String field, @NotNull String message) {
            warnings.add(new ValidationMessage(Level.WARNING, field, message, null));
            return this;
        }

        /**
         * Adds an info message.
         *
         * @param message the info message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder info(@NotNull String message) {
            info.add(ValidationMessage.info(message));
            return this;
        }

        /**
         * Adds a validation message.
         *
         * @param message the message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder message(@NotNull ValidationMessage message) {
            switch (message.level()) {
                case ERROR -> errors.add(message);
                case WARNING -> warnings.add(message);
                case INFO -> info.add(message);
            }
            return this;
        }

        /**
         * Builds the validation result.
         *
         * @return a new ValidationResult
         * @since 1.0.0
         */
        @NotNull
        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}
