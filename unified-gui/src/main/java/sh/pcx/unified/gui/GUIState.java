/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Container for GUI state data with type-safe access through {@link StateKey}.
 *
 * <p>GUIState provides a thread-safe container for storing arbitrary data
 * associated with a GUI instance. It supports type-safe access through
 * StateKey objects and provides various convenience methods.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a state container
 * GUIState state = GUIState.create();
 *
 * // Define keys
 * StateKey<Integer> PAGE = StateKey.of("page", Integer.class);
 * StateKey<String> FILTER = StateKey.of("filter", String.class);
 * StateKey<Boolean> DESCENDING = StateKey.of("descending", Boolean.class);
 *
 * // Set values
 * state.set(PAGE, 1);
 * state.set(FILTER, "admin");
 * state.set(DESCENDING, true);
 *
 * // Get values
 * int page = state.get(PAGE).orElse(1);
 * String filter = state.getOrDefault(FILTER, "");
 * boolean descending = state.getOrDefault(DESCENDING, false);
 *
 * // Check existence
 * if (state.has(PAGE)) {
 *     // Page was explicitly set
 * }
 *
 * // Remove values
 * state.remove(FILTER);
 * state.clear();
 * }</pre>
 *
 * <h2>Computed Values</h2>
 * <pre>{@code
 * // Compute if absent
 * List<Player> players = state.computeIfAbsent(PLAYERS, () -> loadPlayers());
 *
 * // Compute and update
 * int newPage = state.compute(PAGE, current -> current == null ? 1 : current + 1);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>GUIState is thread-safe and can be accessed from any thread. This is
 * important for GUIs that load data asynchronously.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see StateKey
 * @see StatefulGUI
 */
public final class GUIState {

    private final Map<String, Object> data;

    /**
     * Creates a new empty state container.
     */
    private GUIState() {
        this.data = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new empty state container.
     *
     * @return a new GUIState instance
     */
    @NotNull
    public static GUIState create() {
        return new GUIState();
    }

    /**
     * Creates a new state container with initial values.
     *
     * @param initialData the initial data to populate
     * @return a new GUIState instance with the initial data
     */
    @NotNull
    public static GUIState of(@NotNull Map<String, Object> initialData) {
        GUIState state = new GUIState();
        state.data.putAll(initialData);
        return state;
    }

    /**
     * Sets a value for the given key.
     *
     * @param <T>   the value type
     * @param key   the state key
     * @param value the value to store
     * @return this state for chaining
     */
    @NotNull
    public <T> GUIState set(@NotNull StateKey<T> key, @NotNull T value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        data.put(key.getName(), value);
        return this;
    }

    /**
     * Gets the value for the given key.
     *
     * @param <T> the value type
     * @param key the state key
     * @return an Optional containing the value if present
     */
    @NotNull
    public <T> Optional<T> get(@NotNull StateKey<T> key) {
        Objects.requireNonNull(key, "key cannot be null");
        Object value = data.get(key.getName());
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(key.cast(value));
    }

    /**
     * Gets the value for the given key, or returns a default.
     *
     * @param <T>          the value type
     * @param key          the state key
     * @param defaultValue the default value if not present
     * @return the stored value or the default
     */
    @NotNull
    public <T> T getOrDefault(@NotNull StateKey<T> key, @NotNull T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * Gets the value for the given key, or computes a default.
     *
     * @param <T>             the value type
     * @param key             the state key
     * @param defaultSupplier supplier for the default value if not present
     * @return the stored value or the computed default
     */
    @NotNull
    public <T> T getOrDefault(@NotNull StateKey<T> key, @NotNull Supplier<T> defaultSupplier) {
        return get(key).orElseGet(defaultSupplier);
    }

    /**
     * Checks if a value exists for the given key.
     *
     * @param key the state key
     * @return true if a value is stored for the key
     */
    public boolean has(@NotNull StateKey<?> key) {
        Objects.requireNonNull(key, "key cannot be null");
        return data.containsKey(key.getName());
    }

    /**
     * Removes the value for the given key.
     *
     * @param <T> the value type
     * @param key the state key
     * @return an Optional containing the removed value if it existed
     */
    @NotNull
    public <T> Optional<T> remove(@NotNull StateKey<T> key) {
        Objects.requireNonNull(key, "key cannot be null");
        Object value = data.remove(key.getName());
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(key.cast(value));
    }

    /**
     * Computes a value if absent, storing and returning the computed value.
     *
     * @param <T>      the value type
     * @param key      the state key
     * @param supplier the supplier to compute the value if absent
     * @return the existing or computed value
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(@NotNull StateKey<T> key, @NotNull Supplier<T> supplier) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return (T) data.computeIfAbsent(key.getName(), k -> supplier.get());
    }

    /**
     * Computes a new value based on the current value.
     *
     * @param <T>     the value type
     * @param key     the state key
     * @param compute function to compute the new value (receives null if not present)
     * @return the computed value
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T compute(@NotNull StateKey<T> key, @NotNull Function<T, T> compute) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(compute, "compute cannot be null");
        return (T) data.compute(key.getName(), (k, v) -> compute.apply((T) v));
    }

    /**
     * Increments an integer value and returns the new value.
     *
     * @param key    the state key for an integer value
     * @param amount the amount to increment by
     * @return the new value after incrementing
     */
    public int increment(@NotNull StateKey<Integer> key, int amount) {
        Integer result = compute(key, current -> (current == null ? 0 : current) + amount);
        return result != null ? result : amount;
    }

    /**
     * Increments an integer value by 1 and returns the new value.
     *
     * @param key the state key for an integer value
     * @return the new value after incrementing
     */
    public int increment(@NotNull StateKey<Integer> key) {
        return increment(key, 1);
    }

    /**
     * Decrements an integer value and returns the new value.
     *
     * @param key    the state key for an integer value
     * @param amount the amount to decrement by
     * @return the new value after decrementing
     */
    public int decrement(@NotNull StateKey<Integer> key, int amount) {
        return increment(key, -amount);
    }

    /**
     * Decrements an integer value by 1 and returns the new value.
     *
     * @param key the state key for an integer value
     * @return the new value after decrementing
     */
    public int decrement(@NotNull StateKey<Integer> key) {
        return decrement(key, 1);
    }

    /**
     * Toggles a boolean value and returns the new value.
     *
     * @param key the state key for a boolean value
     * @return the new value after toggling
     */
    public boolean toggle(@NotNull StateKey<Boolean> key) {
        Boolean result = compute(key, current -> current == null || !current);
        return result != null && result;
    }

    /**
     * Clears all stored values.
     *
     * @return this state for chaining
     */
    @NotNull
    public GUIState clear() {
        data.clear();
        return this;
    }

    /**
     * Returns the number of stored values.
     *
     * @return the number of stored values
     */
    public int size() {
        return data.size();
    }

    /**
     * Checks if no values are stored.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Returns all stored key names.
     *
     * @return an unmodifiable set of key names
     */
    @NotNull
    public Set<String> keys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    /**
     * Copies all values from another state.
     *
     * @param other the state to copy from
     * @return this state for chaining
     */
    @NotNull
    public GUIState copyFrom(@NotNull GUIState other) {
        Objects.requireNonNull(other, "other cannot be null");
        data.putAll(other.data);
        return this;
    }

    /**
     * Creates a shallow copy of this state.
     *
     * @return a new GUIState with the same values
     */
    @NotNull
    public GUIState copy() {
        return GUIState.of(data);
    }

    @Override
    public String toString() {
        return "GUIState{data=" + data + '}';
    }
}
