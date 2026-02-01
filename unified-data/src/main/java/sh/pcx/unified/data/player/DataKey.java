/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Type-safe key for storing and retrieving player data.
 *
 * <p>DataKeys provide compile-time type safety for player data storage operations.
 * Each key has a unique identifier, a type, and an optional default value. Keys
 * can be namespaced to prevent conflicts between plugins.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Define typed data keys
 * public final class MyPluginData {
 *     // Primitives with defaults
 *     public static final DataKey<Integer> KILLS = DataKey.of("kills", Integer.class, 0);
 *     public static final DataKey<Double> BALANCE = DataKey.of("balance", Double.class, 0.0);
 *     public static final DataKey<Boolean> VIP = DataKey.of("vip", Boolean.class, false);
 *
 *     // Complex types
 *     public static final DataKey<List<String>> UNLOCKS =
 *         DataKey.listOf("unlocks", String.class);
 *     public static final DataKey<Map<String, Integer>> STATISTICS =
 *         DataKey.mapOf("statistics", String.class, Integer.class);
 *
 *     // Namespaced keys (prevent conflicts between plugins)
 *     public static final DataKey<Integer> COINS =
 *         DataKey.of("myplugin:coins", Integer.class, 0);
 * }
 *
 * // Usage with PlayerProfile
 * PlayerProfile profile = playerData.getProfile(player);
 * int kills = profile.getData(MyPluginData.KILLS);
 * profile.setData(MyPluginData.KILLS, kills + 1);
 * }</pre>
 *
 * <h2>Key Naming Conventions</h2>
 * <ul>
 *   <li>Use lowercase with underscores: {@code "player_kills"}</li>
 *   <li>Use namespace prefix for plugin-specific keys: {@code "myplugin:coins"}</li>
 *   <li>Avoid generic names that might conflict: use {@code "economy:balance"} not {@code "balance"}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>DataKey instances are immutable and thread-safe. They can be safely shared
 * between threads and stored as static final constants.
 *
 * @param <T> the type of value this key represents
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerProfile
 * @see PersistentDataKey
 * @see TransientDataKey
 */
public class DataKey<T> {

    private final String key;
    private final Class<T> type;
    private final T defaultValue;
    private final boolean persistent;

    /**
     * Creates a new DataKey with the specified properties.
     *
     * @param key          the unique identifier for this key
     * @param type         the class type of the value
     * @param defaultValue the default value when no value is set
     * @param persistent   whether this key should persist to database
     */
    protected DataKey(@NotNull String key, @NotNull Class<T> type,
                      @Nullable T defaultValue, boolean persistent) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.defaultValue = defaultValue;
        this.persistent = persistent;
    }

    /**
     * Creates a new persistent DataKey with a default value.
     *
     * <p>Persistent keys are automatically saved to and loaded from the database.
     *
     * @param key          the unique identifier for this key
     * @param type         the class type of the value
     * @param defaultValue the default value when no value is set
     * @param <T>          the type of value
     * @return a new DataKey instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> DataKey<T> of(@NotNull String key, @NotNull Class<T> type,
                                    @Nullable T defaultValue) {
        return new DataKey<>(key, type, defaultValue, true);
    }

    /**
     * Creates a new persistent DataKey without a default value.
     *
     * <p>When no value is set, {@link PlayerProfile#getData(DataKey)} will return null.
     *
     * @param key  the unique identifier for this key
     * @param type the class type of the value
     * @param <T>  the type of value
     * @return a new DataKey instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> DataKey<T> of(@NotNull String key, @NotNull Class<T> type) {
        return new DataKey<>(key, type, null, true);
    }

    /**
     * Creates a new DataKey for a List type.
     *
     * <p>The default value is an empty list.
     *
     * @param key         the unique identifier for this key
     * @param elementType the class type of list elements
     * @param <E>         the element type
     * @return a new DataKey for List&lt;E&gt;
     * @throws NullPointerException if key or elementType is null
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <E> DataKey<List<E>> listOf(@NotNull String key,
                                               @NotNull Class<E> elementType) {
        Objects.requireNonNull(elementType, "elementType must not be null");
        return new ListDataKey<>(key, elementType);
    }

    /**
     * Creates a new DataKey for a Map type.
     *
     * <p>The default value is an empty map.
     *
     * @param key       the unique identifier for this key
     * @param keyType   the class type of map keys
     * @param valueType the class type of map values
     * @param <K>       the key type
     * @param <V>       the value type
     * @return a new DataKey for Map&lt;K, V&gt;
     * @throws NullPointerException if any parameter is null
     * @since 1.0.0
     */
    @NotNull
    public static <K, V> DataKey<Map<K, V>> mapOf(@NotNull String key,
                                                   @NotNull Class<K> keyType,
                                                   @NotNull Class<V> valueType) {
        Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(valueType, "valueType must not be null");
        return new MapDataKey<>(key, keyType, valueType);
    }

    /**
     * Creates a transient DataKey that is not persisted to the database.
     *
     * <p>Transient keys are stored in memory only and are lost when the player
     * logs out or the server restarts. Use for temporary data.
     *
     * @param key          the unique identifier for this key
     * @param type         the class type of the value
     * @param defaultValue the default value when no value is set
     * @param <T>          the type of value
     * @return a new transient DataKey instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> DataKey<T> transientKey(@NotNull String key, @NotNull Class<T> type,
                                               @Nullable T defaultValue) {
        return new DataKey<>(key, type, defaultValue, false);
    }

    /**
     * Returns the unique identifier for this key.
     *
     * <p>Keys may be namespaced using a colon separator (e.g., "myplugin:coins").
     *
     * @return the key identifier
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Returns the namespace portion of the key, if present.
     *
     * <p>For key "myplugin:coins", returns "myplugin".
     * For key "coins" (no namespace), returns an empty string.
     *
     * @return the namespace or empty string
     * @since 1.0.0
     */
    @NotNull
    public String getNamespace() {
        int colonIndex = key.indexOf(':');
        return colonIndex > 0 ? key.substring(0, colonIndex) : "";
    }

    /**
     * Returns the name portion of the key (without namespace).
     *
     * <p>For key "myplugin:coins", returns "coins".
     * For key "coins" (no namespace), returns "coins".
     *
     * @return the key name without namespace
     * @since 1.0.0
     */
    @NotNull
    public String getName() {
        int colonIndex = key.indexOf(':');
        return colonIndex > 0 ? key.substring(colonIndex + 1) : key;
    }

    /**
     * Returns the class type of the value.
     *
     * @return the value type
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getType() {
        return type;
    }

    /**
     * Returns the default value for this key.
     *
     * @return the default value, may be null
     * @since 1.0.0
     */
    @Nullable
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns whether this key has a default value.
     *
     * @return true if a default value is set
     * @since 1.0.0
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Returns whether this key persists to the database.
     *
     * <p>Persistent keys are saved when player data is saved and loaded
     * when player data is loaded. Non-persistent (transient) keys are
     * stored in memory only.
     *
     * @return true if this key persists to database
     * @since 1.0.0
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Validates that a value is compatible with this key's type.
     *
     * @param value the value to validate
     * @return true if the value is compatible
     * @since 1.0.0
     */
    public boolean isValidValue(@Nullable Object value) {
        return value == null || type.isInstance(value);
    }

    /**
     * Casts a value to this key's type.
     *
     * @param value the value to cast
     * @return the cast value
     * @throws ClassCastException if the value is not compatible
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T cast(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("Cannot cast " + value.getClass().getName()
                    + " to " + type.getName() + " for key " + key);
        }
        return (T) value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataKey<?> dataKey = (DataKey<?>) o;
        return key.equals(dataKey.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "DataKey{" +
                "key='" + key + '\'' +
                ", type=" + type.getSimpleName() +
                ", persistent=" + persistent +
                '}';
    }

    /**
     * Internal class for List-type data keys.
     *
     * @param <E> the element type
     */
    private static class ListDataKey<E> extends DataKey<List<E>> {
        private final Class<E> elementType;

        @SuppressWarnings("unchecked")
        ListDataKey(String key, Class<E> elementType) {
            super(key, (Class<List<E>>) (Class<?>) List.class, List.of(), true);
            this.elementType = elementType;
        }

        /**
         * Returns the element type of this list key.
         *
         * @return the element class type
         */
        public Class<E> getElementType() {
            return elementType;
        }
    }

    /**
     * Internal class for Map-type data keys.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     */
    private static class MapDataKey<K, V> extends DataKey<Map<K, V>> {
        private final Class<K> keyType;
        private final Class<V> valueType;

        @SuppressWarnings("unchecked")
        MapDataKey(String key, Class<K> keyType, Class<V> valueType) {
            super(key, (Class<Map<K, V>>) (Class<?>) Map.class, Map.of(), true);
            this.keyType = keyType;
            this.valueType = valueType;
        }

        /**
         * Returns the key type of this map key.
         *
         * @return the map key class type
         */
        public Class<K> getKeyType() {
            return keyType;
        }

        /**
         * Returns the value type of this map key.
         *
         * @return the map value class type
         */
        public Class<V> getValueType() {
            return valueType;
        }
    }
}
