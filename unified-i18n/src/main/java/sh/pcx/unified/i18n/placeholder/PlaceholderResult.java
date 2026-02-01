/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the result of a placeholder resolution.
 *
 * <p>A placeholder result can be successful with a value, empty (placeholder not found),
 * or failed with an error. This class provides a safe way to handle all these cases.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Creating results
 * PlaceholderResult success = PlaceholderResult.success("42");
 * PlaceholderResult empty = PlaceholderResult.empty();
 * PlaceholderResult error = PlaceholderResult.error("Database connection failed");
 *
 * // Using results
 * String value = result.orElse("default");
 * String mapped = result.map(s -> s.toUpperCase()).orElse("N/A");
 *
 * // Pattern matching style
 * result.ifPresent(value -> sendMessage(value));
 * result.ifPresentOrElse(
 *     value -> sendMessage(value),
 *     () -> sendError("Not found")
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderResolver
 */
public final class PlaceholderResult {

    private static final PlaceholderResult EMPTY = new PlaceholderResult(null, null, false);

    private final String value;
    private final String error;
    private final boolean cached;

    private PlaceholderResult(@Nullable String value, @Nullable String error, boolean cached) {
        this.value = value;
        this.error = error;
        this.cached = cached;
    }

    /**
     * Creates a successful result with the given value.
     *
     * @param value the placeholder value
     * @return a successful result
     */
    @NotNull
    public static PlaceholderResult success(@NotNull String value) {
        Objects.requireNonNull(value, "value cannot be null");
        return new PlaceholderResult(value, null, false);
    }

    /**
     * Creates a successful cached result with the given value.
     *
     * @param value the placeholder value
     * @return a successful cached result
     */
    @NotNull
    public static PlaceholderResult cached(@NotNull String value) {
        Objects.requireNonNull(value, "value cannot be null");
        return new PlaceholderResult(value, null, true);
    }

    /**
     * Creates an empty result (placeholder not found or not applicable).
     *
     * @return an empty result
     */
    @NotNull
    public static PlaceholderResult empty() {
        return EMPTY;
    }

    /**
     * Creates a result from a nullable value.
     *
     * @param value the value, or null for empty
     * @return a result containing the value, or empty if null
     */
    @NotNull
    public static PlaceholderResult ofNullable(@Nullable String value) {
        return value != null ? success(value) : empty();
    }

    /**
     * Creates a failed result with an error message.
     *
     * @param errorMessage the error message
     * @return a failed result
     */
    @NotNull
    public static PlaceholderResult error(@NotNull String errorMessage) {
        Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
        return new PlaceholderResult(null, errorMessage, false);
    }

    /**
     * Creates a failed result from an exception.
     *
     * @param exception the exception
     * @return a failed result
     */
    @NotNull
    public static PlaceholderResult error(@NotNull Throwable exception) {
        Objects.requireNonNull(exception, "exception cannot be null");
        String message = exception.getMessage();
        return new PlaceholderResult(null, message != null ? message : exception.getClass().getSimpleName(), false);
    }

    /**
     * Checks if this result has a value.
     *
     * @return {@code true} if a value is present
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Checks if this result is empty (no value and no error).
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return value == null && error == null;
    }

    /**
     * Checks if this result is an error.
     *
     * @return {@code true} if this result represents an error
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Checks if this result was retrieved from cache.
     *
     * @return {@code true} if the value was cached
     */
    public boolean isCached() {
        return cached;
    }

    /**
     * Returns the value if present, otherwise throws.
     *
     * @return the value
     * @throws IllegalStateException if no value is present
     */
    @NotNull
    public String get() {
        if (value == null) {
            if (error != null) {
                throw new IllegalStateException("Placeholder resolution failed: " + error);
            }
            throw new IllegalStateException("No value present");
        }
        return value;
    }

    /**
     * Returns the value as an Optional.
     *
     * @return an Optional containing the value, or empty
     */
    @NotNull
    public Optional<String> toOptional() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns the value if present, otherwise returns the default.
     *
     * @param defaultValue the default value
     * @return the value or default
     */
    @NotNull
    public String orElse(@NotNull String defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the value if present, otherwise computes a default.
     *
     * @param supplier the supplier for the default value
     * @return the value or computed default
     */
    @NotNull
    public String orElseGet(@NotNull Supplier<String> supplier) {
        return value != null ? value : supplier.get();
    }

    /**
     * Returns the value if present, otherwise throws the supplied exception.
     *
     * @param exceptionSupplier the exception supplier
     * @param <X>               the exception type
     * @return the value
     * @throws X if no value is present
     */
    @NotNull
    public <X extends Throwable> String orElseThrow(@NotNull Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) {
            return value;
        }
        throw exceptionSupplier.get();
    }

    /**
     * Returns the error message if this is an error result.
     *
     * @return an Optional containing the error message
     */
    @NotNull
    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }

    /**
     * Maps the value using the given function.
     *
     * @param mapper the mapping function
     * @return a new result with the mapped value, or this result if no value
     */
    @NotNull
    public PlaceholderResult map(@NotNull Function<String, String> mapper) {
        if (value != null) {
            return success(mapper.apply(value));
        }
        return this;
    }

    /**
     * Flat-maps the value using the given function.
     *
     * @param mapper the mapping function returning a result
     * @return the mapped result, or this result if no value
     */
    @NotNull
    public PlaceholderResult flatMap(@NotNull Function<String, PlaceholderResult> mapper) {
        if (value != null) {
            return mapper.apply(value);
        }
        return this;
    }

    /**
     * Filters the value using the given predicate.
     *
     * @param predicate the predicate to test
     * @return this result if value matches, otherwise empty
     */
    @NotNull
    public PlaceholderResult filter(@NotNull java.util.function.Predicate<String> predicate) {
        if (value != null && predicate.test(value)) {
            return this;
        }
        return empty();
    }

    /**
     * Executes the action if a value is present.
     *
     * @param action the action to execute
     */
    public void ifPresent(@NotNull java.util.function.Consumer<String> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * Executes an action based on whether a value is present.
     *
     * @param presentAction the action if value is present
     * @param emptyAction   the action if no value
     */
    public void ifPresentOrElse(
            @NotNull java.util.function.Consumer<String> presentAction,
            @NotNull Runnable emptyAction) {
        if (value != null) {
            presentAction.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * Returns this result if it has a value, otherwise returns the other.
     *
     * @param other the alternative result
     * @return this result or the alternative
     */
    @NotNull
    public PlaceholderResult or(@NotNull Supplier<PlaceholderResult> other) {
        return value != null ? this : other.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlaceholderResult)) return false;
        PlaceholderResult other = (PlaceholderResult) obj;
        return Objects.equals(value, other.value)
                && Objects.equals(error, other.error)
                && cached == other.cached;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, error, cached);
    }

    @Override
    public String toString() {
        if (value != null) {
            return "PlaceholderResult.success(" + value + (cached ? ", cached" : "") + ")";
        } else if (error != null) {
            return "PlaceholderResult.error(" + error + ")";
        }
        return "PlaceholderResult.empty()";
    }
}
