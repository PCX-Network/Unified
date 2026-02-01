/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Type-safe key for storing and retrieving values from GUI state.
 *
 * <p>StateKey provides compile-time type safety when working with GUI state,
 * eliminating the need for casting and reducing runtime errors.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Define state keys as constants
 * public class MyGUI extends AbstractGUI {
 *
 *     private static final StateKey<Integer> PAGE = StateKey.of("page", Integer.class);
 *     private static final StateKey<String> SEARCH_QUERY = StateKey.of("searchQuery", String.class);
 *     private static final StateKey<Boolean> SHOW_OFFLINE = StateKey.of("showOffline", Boolean.class);
 *     private static final StateKey<List<PlayerData>> PLAYERS = StateKey.ofGeneric("players");
 *
 *     @Override
 *     protected void setup() {
 *         // Get state with defaults
 *         int page = getState(PAGE, 1);
 *         String query = getState(SEARCH_QUERY, "");
 *         boolean showOffline = getState(SHOW_OFFLINE, false);
 *
 *         // Set state
 *         setState(PAGE, page + 1);
 *         setState(SEARCH_QUERY, "admin");
 *
 *         // Check if state exists
 *         if (hasState(PAGE)) {
 *             // Page was explicitly set
 *         }
 *     }
 * }
 *
 * // Using with GUIState directly
 * GUIState state = GUIState.create();
 * state.set(PAGE, 5);
 * int page = state.get(PAGE).orElse(1);
 * }</pre>
 *
 * <h2>Type Safety</h2>
 * <pre>{@code
 * // These compile:
 * StateKey<String> name = StateKey.of("name", String.class);
 * state.set(name, "Hello");
 * String value = state.get(name).orElse("");
 *
 * // This would not compile - type mismatch:
 * // state.set(name, 123); // Error: Integer cannot be converted to String
 * }</pre>
 *
 * @param <T> the type of value this key represents
 * @since 1.0.0
 * @author Supatuck
 * @see GUIState
 * @see StatefulGUI
 */
public final class StateKey<T> {

    private final String name;
    private final Class<T> type;

    /**
     * Creates a new state key with the specified name and type.
     *
     * @param name the key name (must be unique within a state container)
     * @param type the value type class
     */
    private StateKey(@NotNull String name, @NotNull Class<T> type) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    /**
     * Creates a new state key with the specified name and type.
     *
     * @param <T>  the value type
     * @param name the key name (must be unique within a state container)
     * @param type the value type class
     * @return the created state key
     */
    @NotNull
    public static <T> StateKey<T> of(@NotNull String name, @NotNull Class<T> type) {
        return new StateKey<>(name, type);
    }

    /**
     * Creates a state key for generic types where the class cannot be specified.
     *
     * <p>This is useful for complex generic types like {@code List<PlayerData>}
     * where type erasure prevents specifying the full type.
     *
     * <p><strong>Warning:</strong> This method provides less type safety than
     * {@link #of(String, Class)}. Use only when necessary for generic types.
     *
     * @param <T>  the value type
     * @param name the key name (must be unique within a state container)
     * @return the created state key
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> StateKey<T> ofGeneric(@NotNull String name) {
        return new StateKey<>(name, (Class<T>) Object.class);
    }

    /**
     * Creates a state key for Integer values.
     *
     * @param name the key name
     * @return the created state key
     */
    @NotNull
    public static StateKey<Integer> ofInt(@NotNull String name) {
        return of(name, Integer.class);
    }

    /**
     * Creates a state key for Long values.
     *
     * @param name the key name
     * @return the created state key
     */
    @NotNull
    public static StateKey<Long> ofLong(@NotNull String name) {
        return of(name, Long.class);
    }

    /**
     * Creates a state key for Double values.
     *
     * @param name the key name
     * @return the created state key
     */
    @NotNull
    public static StateKey<Double> ofDouble(@NotNull String name) {
        return of(name, Double.class);
    }

    /**
     * Creates a state key for Boolean values.
     *
     * @param name the key name
     * @return the created state key
     */
    @NotNull
    public static StateKey<Boolean> ofBoolean(@NotNull String name) {
        return of(name, Boolean.class);
    }

    /**
     * Creates a state key for String values.
     *
     * @param name the key name
     * @return the created state key
     */
    @NotNull
    public static StateKey<String> ofString(@NotNull String name) {
        return of(name, String.class);
    }

    /**
     * Returns the name of this key.
     *
     * @return the key name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the type of values for this key.
     *
     * @return the value type class
     */
    @NotNull
    public Class<T> getType() {
        return type;
    }

    /**
     * Casts the given value to this key's type.
     *
     * @param value the value to cast
     * @return the cast value
     * @throws ClassCastException if the value cannot be cast
     */
    @SuppressWarnings("unchecked")
    public T cast(@NotNull Object value) {
        return (T) value;
    }

    /**
     * Checks if the given value is compatible with this key's type.
     *
     * @param value the value to check
     * @return true if the value can be stored with this key
     */
    public boolean isCompatible(@NotNull Object value) {
        return type.isInstance(value) || type == Object.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateKey<?> stateKey = (StateKey<?>) o;
        return name.equals(stateKey.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "StateKey{name='" + name + "', type=" + type.getSimpleName() + '}';
    }
}
