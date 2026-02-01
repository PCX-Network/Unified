/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Exception thrown when configuration operations fail.
 *
 * <p>ConfigException provides detailed information about configuration errors
 * including the path to the configuration file, the specific node path where
 * the error occurred, and suggestions for resolution.</p>
 *
 * <h2>Common Causes</h2>
 * <ul>
 *   <li>File not found or not readable</li>
 *   <li>Invalid syntax in configuration file</li>
 *   <li>Type conversion errors during deserialization</li>
 *   <li>Validation constraint violations</li>
 *   <li>Missing required values</li>
 * </ul>
 *
 * <h2>Example Handling</h2>
 * <pre>{@code
 * try {
 *     PluginConfig config = configService.load(PluginConfig.class, path);
 * } catch (ConfigException e) {
 *     logger.severe("Failed to load config: " + e.getMessage());
 *     if (e.getNodePath() != null) {
 *         logger.severe("Error at: " + e.getNodePath());
 *     }
 *     if (e.getSuggestion() != null) {
 *         logger.info("Suggestion: " + e.getSuggestion());
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Path filePath;
    private final String nodePath;
    private final String suggestion;
    private final List<String> validationErrors;

    /**
     * Creates a new configuration exception.
     *
     * @param message the error message
     */
    public ConfigException(@NotNull String message) {
        super(message);
        this.filePath = null;
        this.nodePath = null;
        this.suggestion = null;
        this.validationErrors = List.of();
    }

    /**
     * Creates a new configuration exception with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ConfigException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.filePath = null;
        this.nodePath = null;
        this.suggestion = null;
        this.validationErrors = List.of();
    }

    /**
     * Creates a new configuration exception with file path.
     *
     * @param message the error message
     * @param filePath the path to the configuration file
     */
    public ConfigException(@NotNull String message, @Nullable Path filePath) {
        super(message);
        this.filePath = filePath;
        this.nodePath = null;
        this.suggestion = null;
        this.validationErrors = List.of();
    }

    /**
     * Creates a new configuration exception with full details.
     *
     * @param message the error message
     * @param filePath the path to the configuration file
     * @param nodePath the path to the problematic node
     * @param cause the underlying cause
     */
    public ConfigException(
            @NotNull String message,
            @Nullable Path filePath,
            @Nullable String nodePath,
            @Nullable Throwable cause
    ) {
        super(message, cause);
        this.filePath = filePath;
        this.nodePath = nodePath;
        this.suggestion = null;
        this.validationErrors = List.of();
    }

    /**
     * Creates a new configuration exception with all details.
     *
     * @param message the error message
     * @param filePath the path to the configuration file
     * @param nodePath the path to the problematic node
     * @param suggestion a suggestion for fixing the error
     * @param cause the underlying cause
     */
    public ConfigException(
            @NotNull String message,
            @Nullable Path filePath,
            @Nullable String nodePath,
            @Nullable String suggestion,
            @Nullable Throwable cause
    ) {
        super(message, cause);
        this.filePath = filePath;
        this.nodePath = nodePath;
        this.suggestion = suggestion;
        this.validationErrors = List.of();
    }

    /**
     * Creates a configuration exception for validation errors.
     *
     * @param message the error message
     * @param filePath the path to the configuration file
     * @param validationErrors list of validation error messages
     */
    public ConfigException(
            @NotNull String message,
            @Nullable Path filePath,
            @NotNull List<String> validationErrors
    ) {
        super(message);
        this.filePath = filePath;
        this.nodePath = null;
        this.suggestion = null;
        this.validationErrors = List.copyOf(validationErrors);
    }

    /**
     * Gets the path to the configuration file.
     *
     * @return the file path, or null if not available
     * @since 1.0.0
     */
    @Nullable
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Gets the path to the problematic configuration node.
     *
     * <p>The path is in dot-notation (e.g., "database.connection.host").</p>
     *
     * @return the node path, or null if not applicable
     * @since 1.0.0
     */
    @Nullable
    public String getNodePath() {
        return nodePath;
    }

    /**
     * Gets a suggestion for fixing the error.
     *
     * @return the suggestion, or null if none available
     * @since 1.0.0
     */
    @Nullable
    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Gets the list of validation errors.
     *
     * @return list of validation error messages (empty if not a validation error)
     * @since 1.0.0
     */
    @NotNull
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Checks if this exception was caused by validation errors.
     *
     * @return true if there are validation errors
     * @since 1.0.0
     */
    public boolean isValidationError() {
        return !validationErrors.isEmpty();
    }

    /**
     * Creates a detailed error message including all available information.
     *
     * @return a detailed error message
     * @since 1.0.0
     */
    @NotNull
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(getMessage());

        if (filePath != null) {
            sb.append("\n  File: ").append(filePath);
        }

        if (nodePath != null) {
            sb.append("\n  Path: ").append(nodePath);
        }

        if (!validationErrors.isEmpty()) {
            sb.append("\n  Validation errors:");
            for (String error : validationErrors) {
                sb.append("\n    - ").append(error);
            }
        }

        if (suggestion != null) {
            sb.append("\n  Suggestion: ").append(suggestion);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "ConfigException: " + getDetailedMessage();
    }

    // Static factory methods for common error types

    /**
     * Creates an exception for a file not found error.
     *
     * @param path the path that was not found
     * @return the exception
     */
    @NotNull
    public static ConfigException fileNotFound(@NotNull Path path) {
        return new ConfigException(
                "Configuration file not found: " + path,
                path,
                null,
                "Create the file or check the path",
                null
        );
    }

    /**
     * Creates an exception for a parse error.
     *
     * @param path the path to the file
     * @param cause the parse exception
     * @return the exception
     */
    @NotNull
    public static ConfigException parseError(@NotNull Path path, @NotNull Throwable cause) {
        return new ConfigException(
                "Failed to parse configuration file",
                path,
                null,
                "Check the file syntax and ensure it's valid " + getFormatFromPath(path),
                cause
        );
    }

    /**
     * Creates an exception for a serialization error.
     *
     * @param path the path to the file
     * @param nodePath the path to the problematic node
     * @param cause the serialization exception
     * @return the exception
     */
    @NotNull
    public static ConfigException serializationError(
            @Nullable Path path,
            @NotNull String nodePath,
            @NotNull Throwable cause
    ) {
        return new ConfigException(
                "Failed to deserialize configuration at '" + nodePath + "'",
                path,
                nodePath,
                "Check the value type matches the expected type",
                cause
        );
    }

    /**
     * Creates an exception for a type mismatch error.
     *
     * @param nodePath the path to the node
     * @param expectedType the expected type
     * @param actualValue the actual value
     * @return the exception
     */
    @NotNull
    public static ConfigException typeMismatch(
            @NotNull String nodePath,
            @NotNull Class<?> expectedType,
            @Nullable Object actualValue
    ) {
        String actualType = actualValue == null ? "null" : actualValue.getClass().getSimpleName();
        return new ConfigException(
                "Type mismatch at '" + nodePath + "': expected " +
                        expectedType.getSimpleName() + " but got " + actualType,
                null,
                nodePath,
                "Update the value to match the expected type",
                null
        );
    }

    private static String getFormatFromPath(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return "YAML";
        } else if (fileName.endsWith(".conf") || fileName.endsWith(".hocon")) {
            return "HOCON";
        } else if (fileName.endsWith(".json")) {
            return "JSON";
        } else if (fileName.endsWith(".toml")) {
            return "TOML";
        }
        return "configuration";
    }
}
