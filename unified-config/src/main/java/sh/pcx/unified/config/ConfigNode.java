/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A type-safe wrapper around Sponge Configurate's {@link ConfigurationNode}.
 *
 * <p>ConfigNode provides a fluent API for accessing and modifying configuration
 * values with automatic type conversion, default value support, and null safety.
 * It wraps the underlying Configurate node while providing a more user-friendly
 * interface.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Type-safe access:</b> Generic methods with automatic conversion</li>
 *   <li><b>Path navigation:</b> Dot-notation and array access</li>
 *   <li><b>Default values:</b> Fallback when values are missing</li>
 *   <li><b>Collection support:</b> Lists, maps, and sets</li>
 *   <li><b>Object mapping:</b> Serialize/deserialize complex objects</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * ConfigNode node = configService.getNode(configPath);
 *
 * // Simple value access
 * String host = node.get("database.host").getString("localhost");
 * int port = node.get("database.port").getInt(3306);
 * boolean debug = node.get("debug").getBoolean(false);
 *
 * // List access
 * List<String> worlds = node.get("worlds").getStringList();
 *
 * // Object mapping
 * DatabaseConfig dbConfig = node.get("database").as(DatabaseConfig.class);
 *
 * // Setting values
 * node.get("settings.maxPlayers").set(50);
 * node.save();
 * }</pre>
 *
 * <h2>Path Navigation</h2>
 * <pre>{@code
 * // Dot notation
 * ConfigNode dbNode = node.get("database.connection.pool");
 *
 * // Array access
 * ConfigNode firstWorld = node.get("worlds[0]");
 *
 * // Child navigation
 * ConfigNode child = node.child("database").child("host");
 *
 * // Parent navigation
 * ConfigNode parent = child.parent();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigService
 * @see ConfigRoot
 */
public interface ConfigNode {

    /**
     * Gets the underlying Configurate node.
     *
     * <p>Use this for advanced operations not covered by this wrapper.</p>
     *
     * @return the underlying ConfigurationNode
     * @since 1.0.0
     */
    @NotNull
    ConfigurationNode raw();

    /**
     * Gets a child node at the specified path.
     *
     * <p>Supports dot-notation (e.g., "database.host") and array access
     * (e.g., "worlds[0]").</p>
     *
     * @param path the path to the child node
     * @return the child node (never null, may be virtual)
     * @since 1.0.0
     */
    @NotNull
    ConfigNode get(@NotNull String path);

    /**
     * Gets a direct child node.
     *
     * @param key the child key
     * @return the child node
     * @since 1.0.0
     */
    @NotNull
    ConfigNode child(@NotNull String key);

    /**
     * Gets a child node at the specified list index.
     *
     * @param index the list index
     * @return the child node
     * @since 1.0.0
     */
    @NotNull
    ConfigNode child(int index);

    /**
     * Gets the parent node.
     *
     * @return the parent node, or empty if this is the root
     * @since 1.0.0
     */
    @NotNull
    Optional<ConfigNode> parent();

    /**
     * Gets the key of this node within its parent.
     *
     * @return the node key, or null if this is the root
     * @since 1.0.0
     */
    @Nullable
    Object key();

    /**
     * Gets the full path to this node from the root.
     *
     * @return the path as a list of keys
     * @since 1.0.0
     */
    @NotNull
    List<Object> path();

    /**
     * Checks if this node exists and has a value.
     *
     * @return true if the node has a value
     * @since 1.0.0
     */
    boolean exists();

    /**
     * Checks if this node is virtual (doesn't exist in the config).
     *
     * @return true if the node is virtual
     * @since 1.0.0
     */
    boolean isVirtual();

    /**
     * Checks if this node is a map (object).
     *
     * @return true if this node is a map
     * @since 1.0.0
     */
    boolean isMap();

    /**
     * Checks if this node is a list.
     *
     * @return true if this node is a list
     * @since 1.0.0
     */
    boolean isList();

    /**
     * Gets the value as a String.
     *
     * @return the String value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    String getString();

    /**
     * Gets the value as a String with a default.
     *
     * @param defaultValue the default value if not present
     * @return the String value or default
     * @since 1.0.0
     */
    @NotNull
    String getString(@NotNull String defaultValue);

    /**
     * Gets the value as an int.
     *
     * @return the int value, or 0 if not present
     * @since 1.0.0
     */
    int getInt();

    /**
     * Gets the value as an int with a default.
     *
     * @param defaultValue the default value if not present
     * @return the int value or default
     * @since 1.0.0
     */
    int getInt(int defaultValue);

    /**
     * Gets the value as a long.
     *
     * @return the long value, or 0 if not present
     * @since 1.0.0
     */
    long getLong();

    /**
     * Gets the value as a long with a default.
     *
     * @param defaultValue the default value if not present
     * @return the long value or default
     * @since 1.0.0
     */
    long getLong(long defaultValue);

    /**
     * Gets the value as a double.
     *
     * @return the double value, or 0.0 if not present
     * @since 1.0.0
     */
    double getDouble();

    /**
     * Gets the value as a double with a default.
     *
     * @param defaultValue the default value if not present
     * @return the double value or default
     * @since 1.0.0
     */
    double getDouble(double defaultValue);

    /**
     * Gets the value as a float.
     *
     * @return the float value, or 0.0f if not present
     * @since 1.0.0
     */
    float getFloat();

    /**
     * Gets the value as a float with a default.
     *
     * @param defaultValue the default value if not present
     * @return the float value or default
     * @since 1.0.0
     */
    float getFloat(float defaultValue);

    /**
     * Gets the value as a boolean.
     *
     * @return the boolean value, or false if not present
     * @since 1.0.0
     */
    boolean getBoolean();

    /**
     * Gets the value as a boolean with a default.
     *
     * @param defaultValue the default value if not present
     * @return the boolean value or default
     * @since 1.0.0
     */
    boolean getBoolean(boolean defaultValue);

    /**
     * Gets the value as a typed object.
     *
     * @param type the target type class
     * @param <T> the target type
     * @return the value, or null if not present or incompatible
     * @since 1.0.0
     */
    @Nullable
    <T> T get(@NotNull Class<T> type);

    /**
     * Gets the value as a typed object with a default.
     *
     * @param type the target type class
     * @param defaultValue the default value
     * @param <T> the target type
     * @return the value or default
     * @since 1.0.0
     */
    @NotNull
    <T> T get(@NotNull Class<T> type, @NotNull T defaultValue);

    /**
     * Gets the value as a typed object with a lazy default.
     *
     * @param type the target type class
     * @param defaultSupplier supplier for the default value
     * @param <T> the target type
     * @return the value or supplied default
     * @since 1.0.0
     */
    @NotNull
    <T> T get(@NotNull Class<T> type, @NotNull Supplier<T> defaultSupplier);

    /**
     * Gets the value as an Optional.
     *
     * @param type the target type class
     * @param <T> the target type
     * @return optional containing the value, or empty
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getOptional(@NotNull Class<T> type);

    /**
     * Deserializes this node to a configuration object.
     *
     * @param type the configuration class type
     * @param <T> the configuration type
     * @return the deserialized object
     * @throws ConfigException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    <T> T as(@NotNull Class<T> type);

    /**
     * Gets the value as a list of Strings.
     *
     * @return the list of strings, empty list if not present
     * @since 1.0.0
     */
    @NotNull
    List<String> getStringList();

    /**
     * Gets the value as a list of the specified type.
     *
     * @param elementType the element type class
     * @param <T> the element type
     * @return the list, empty if not present
     * @since 1.0.0
     */
    @NotNull
    <T> List<T> getList(@NotNull Class<T> elementType);

    /**
     * Gets the value as a set of the specified type.
     *
     * @param elementType the element type class
     * @param <T> the element type
     * @return the set, empty if not present
     * @since 1.0.0
     */
    @NotNull
    <T> Set<T> getSet(@NotNull Class<T> elementType);

    /**
     * Gets the value as a map with string keys.
     *
     * @param valueType the value type class
     * @param <V> the value type
     * @return the map, empty if not present
     * @since 1.0.0
     */
    @NotNull
    <V> Map<String, V> getMap(@NotNull Class<V> valueType);

    /**
     * Gets all child keys of this node.
     *
     * @return set of child keys
     * @since 1.0.0
     */
    @NotNull
    Set<Object> getKeys();

    /**
     * Gets all child nodes as a list.
     *
     * @return list of child nodes
     * @since 1.0.0
     */
    @NotNull
    List<ConfigNode> getChildren();

    /**
     * Sets the value of this node.
     *
     * @param value the value to set
     * @return this node for chaining
     * @throws ConfigException if setting fails
     * @since 1.0.0
     */
    @NotNull
    ConfigNode set(@Nullable Object value);

    /**
     * Sets a child value at the specified path.
     *
     * @param path the path to the child
     * @param value the value to set
     * @return this node for chaining
     * @throws ConfigException if setting fails
     * @since 1.0.0
     */
    @NotNull
    ConfigNode set(@NotNull String path, @Nullable Object value);

    /**
     * Sets this node from a configuration object.
     *
     * @param config the configuration object
     * @param <T> the configuration type
     * @return this node for chaining
     * @throws ConfigException if serialization fails
     * @since 1.0.0
     */
    @NotNull
    <T> ConfigNode from(@NotNull T config);

    /**
     * Removes this node from its parent.
     *
     * @return the parent node
     * @since 1.0.0
     */
    @NotNull
    Optional<ConfigNode> remove();

    /**
     * Removes a child at the specified path.
     *
     * @param path the path to remove
     * @return this node for chaining
     * @since 1.0.0
     */
    @NotNull
    ConfigNode remove(@NotNull String path);

    /**
     * Merges another node's values into this node.
     *
     * <p>Values from the other node override values in this node.</p>
     *
     * @param other the node to merge from
     * @return this node for chaining
     * @since 1.0.0
     */
    @NotNull
    ConfigNode merge(@NotNull ConfigNode other);

    /**
     * Gets the comment associated with this node.
     *
     * @return the comment, or null if none
     * @since 1.0.0
     */
    @Nullable
    String getComment();

    /**
     * Sets the comment for this node.
     *
     * @param comment the comment text
     * @return this node for chaining
     * @since 1.0.0
     */
    @NotNull
    ConfigNode setComment(@Nullable String comment);

    /**
     * Creates a deep copy of this node.
     *
     * @return a copy of this node
     * @since 1.0.0
     */
    @NotNull
    ConfigNode copy();
}
