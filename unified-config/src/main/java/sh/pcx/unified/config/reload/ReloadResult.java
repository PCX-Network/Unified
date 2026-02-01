/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.reload;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents the result of a configuration reload operation.
 *
 * <p>ReloadResult contains either the successfully reloaded configuration
 * or information about why the reload failed. It provides a fluent API
 * for handling both success and failure cases.</p>
 *
 * <h2>Checking Results</h2>
 * <pre>{@code
 * ReloadResult<PluginConfig> result = ...;
 *
 * if (result.isSuccess()) {
 *     PluginConfig config = result.get();
 *     // Use the config
 * } else {
 *     String error = result.getErrorMessage();
 *     logger.warning("Reload failed: " + error);
 * }
 * }</pre>
 *
 * <h2>Fluent Handling</h2>
 * <pre>{@code
 * result.ifSuccess(config -> {
 *     this.config = config;
 *     logger.info("Config reloaded!");
 * }).ifFailure(error -> {
 *     logger.severe("Reload failed: " + error.getMessage());
 * });
 * }</pre>
 *
 * <h2>Transforming Results</h2>
 * <pre>{@code
 * // Map to a different type
 * ReloadResult<DatabaseConfig> dbResult = result.map(PluginConfig::getDatabase);
 *
 * // Get with default
 * PluginConfig config = result.orElse(defaultConfig);
 *
 * // Get or throw
 * PluginConfig config = result.orElseThrow();
 * }</pre>
 *
 * @param <T> the configuration type
 * @author Supatuck
 * @since 1.0.0
 * @see ReloadHandler
 */
public final class ReloadResult<T> {

    private final T value;
    private final Throwable error;
    private final Path path;
    private final Instant timestamp;
    private final boolean wasModified;

    private ReloadResult(
            @Nullable T value,
            @Nullable Throwable error,
            @Nullable Path path,
            @NotNull Instant timestamp,
            boolean wasModified
    ) {
        this.value = value;
        this.error = error;
        this.path = path;
        this.timestamp = timestamp;
        this.wasModified = wasModified;
    }

    /**
     * Creates a successful reload result.
     *
     * @param value the reloaded configuration
     * @param path the path to the configuration file
     * @param <T> the configuration type
     * @return the success result
     * @since 1.0.0
     */
    @NotNull
    public static <T> ReloadResult<T> success(@NotNull T value, @Nullable Path path) {
        return new ReloadResult<>(value, null, path, Instant.now(), true);
    }

    /**
     * Creates a successful reload result with modification flag.
     *
     * @param value the reloaded configuration
     * @param path the path to the configuration file
     * @param wasModified whether the file was actually modified
     * @param <T> the configuration type
     * @return the success result
     * @since 1.0.0
     */
    @NotNull
    public static <T> ReloadResult<T> success(@NotNull T value, @Nullable Path path, boolean wasModified) {
        return new ReloadResult<>(value, null, path, Instant.now(), wasModified);
    }

    /**
     * Creates a failed reload result.
     *
     * @param error the error that occurred
     * @param path the path to the configuration file
     * @param <T> the configuration type
     * @return the failure result
     * @since 1.0.0
     */
    @NotNull
    public static <T> ReloadResult<T> failure(@NotNull Throwable error, @Nullable Path path) {
        return new ReloadResult<>(null, error, path, Instant.now(), false);
    }

    /**
     * Creates a failed reload result with message.
     *
     * @param message the error message
     * @param path the path to the configuration file
     * @param <T> the configuration type
     * @return the failure result
     * @since 1.0.0
     */
    @NotNull
    public static <T> ReloadResult<T> failure(@NotNull String message, @Nullable Path path) {
        return failure(new RuntimeException(message), path);
    }

    /**
     * Checks if the reload was successful.
     *
     * @return true if successful
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return value != null && error == null;
    }

    /**
     * Checks if the reload failed.
     *
     * @return true if failed
     * @since 1.0.0
     */
    public boolean isFailure() {
        return error != null;
    }

    /**
     * Gets the reloaded configuration.
     *
     * @return the configuration
     * @throws IllegalStateException if the reload failed
     * @since 1.0.0
     */
    @NotNull
    public T get() {
        if (value == null) {
            throw new IllegalStateException("Reload failed: " + getErrorMessage());
        }
        return value;
    }

    /**
     * Gets the configuration as an Optional.
     *
     * @return optional containing the configuration, or empty if failed
     * @since 1.0.0
     */
    @NotNull
    public Optional<T> toOptional() {
        return Optional.ofNullable(value);
    }

    /**
     * Gets the configuration or a default value.
     *
     * @param defaultValue the default value
     * @return the configuration or default
     * @since 1.0.0
     */
    @NotNull
    public T orElse(@NotNull T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Gets the configuration or throws.
     *
     * @return the configuration
     * @throws RuntimeException if the reload failed
     * @since 1.0.0
     */
    @NotNull
    public T orElseThrow() {
        if (value == null) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new RuntimeException("Reload failed", error);
        }
        return value;
    }

    /**
     * Gets the error that occurred.
     *
     * @return the error, or null if successful
     * @since 1.0.0
     */
    @Nullable
    public Throwable getError() {
        return error;
    }

    /**
     * Gets the error message.
     *
     * @return the error message, or empty string if successful
     * @since 1.0.0
     */
    @NotNull
    public String getErrorMessage() {
        if (error == null) {
            return "";
        }
        return error.getMessage() != null ? error.getMessage() : error.getClass().getName();
    }

    /**
     * Gets the path to the configuration file.
     *
     * @return the path, or null if not available
     * @since 1.0.0
     */
    @Nullable
    public Path getPath() {
        return path;
    }

    /**
     * Gets the timestamp of the reload.
     *
     * @return the reload timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if the file was actually modified.
     *
     * <p>This may be false for reloads triggered manually rather than
     * by file modification.</p>
     *
     * @return true if the file was modified
     * @since 1.0.0
     */
    public boolean wasModified() {
        return wasModified;
    }

    /**
     * Executes an action if the reload was successful.
     *
     * @param action the action to execute
     * @return this result for chaining
     * @since 1.0.0
     */
    @NotNull
    public ReloadResult<T> ifSuccess(@NotNull Consumer<T> action) {
        if (isSuccess()) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Executes an action if the reload failed.
     *
     * @param action the action to execute
     * @return this result for chaining
     * @since 1.0.0
     */
    @NotNull
    public ReloadResult<T> ifFailure(@NotNull Consumer<Throwable> action) {
        if (isFailure()) {
            action.accept(error);
        }
        return this;
    }

    /**
     * Maps the configuration to a different type.
     *
     * @param mapper the mapping function
     * @param <U> the new type
     * @return the mapped result
     * @since 1.0.0
     */
    @NotNull
    public <U> ReloadResult<U> map(@NotNull Function<T, U> mapper) {
        if (isSuccess()) {
            try {
                return ReloadResult.success(mapper.apply(value), path, wasModified);
            } catch (Exception e) {
                return ReloadResult.failure(e, path);
            }
        }
        return ReloadResult.failure(error, path);
    }

    /**
     * Flat maps the configuration to a different result.
     *
     * @param mapper the mapping function
     * @param <U> the new type
     * @return the mapped result
     * @since 1.0.0
     */
    @NotNull
    public <U> ReloadResult<U> flatMap(@NotNull Function<T, ReloadResult<U>> mapper) {
        if (isSuccess()) {
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                return ReloadResult.failure(e, path);
            }
        }
        return ReloadResult.failure(error, path);
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "ReloadResult{success=true, path=" + path + ", timestamp=" + timestamp + "}";
        } else {
            return "ReloadResult{success=false, error=" + getErrorMessage() +
                    ", path=" + path + ", timestamp=" + timestamp + "}";
        }
    }
}
