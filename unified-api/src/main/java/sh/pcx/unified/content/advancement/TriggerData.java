/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import org.jetbrains.annotations.NotNull;

/**
 * Data passed to custom trigger predicates.
 *
 * @since 1.0.0
 */
public interface TriggerData {

    /**
     * Creates trigger data with a single key-value pair.
     *
     * @param key   the data key
     * @param value the data value
     * @return a TriggerData instance
     * @since 1.0.0
     */
    @NotNull
    static TriggerData of(@NotNull String key, @NotNull Object value) {
        return new SimpleTriggerData(java.util.Map.of(key, value));
    }

    /**
     * Creates trigger data with multiple key-value pairs.
     *
     * @param key1   first key
     * @param value1 first value
     * @param key2   second key
     * @param value2 second value
     * @return a TriggerData instance
     * @since 1.0.0
     */
    @NotNull
    static TriggerData of(@NotNull String key1, @NotNull Object value1,
                          @NotNull String key2, @NotNull Object value2) {
        return new SimpleTriggerData(java.util.Map.of(key1, value1, key2, value2));
    }

    /**
     * Creates empty trigger data.
     *
     * @return an empty TriggerData instance
     * @since 1.0.0
     */
    @NotNull
    static TriggerData empty() {
        return SimpleTriggerData.EMPTY;
    }

    /**
     * Gets a string value by key.
     *
     * @param key the data key
     * @return the string value, or null if not present
     * @since 1.0.0
     */
    String getString(@NotNull String key);

    /**
     * Gets an integer value by key.
     *
     * @param key          the data key
     * @param defaultValue the default if not present
     * @return the integer value
     * @since 1.0.0
     */
    int getInt(@NotNull String key, int defaultValue);

    /**
     * Gets a double value by key.
     *
     * @param key          the data key
     * @param defaultValue the default if not present
     * @return the double value
     * @since 1.0.0
     */
    double getDouble(@NotNull String key, double defaultValue);

    /**
     * Gets a boolean value by key.
     *
     * @param key          the data key
     * @param defaultValue the default if not present
     * @return the boolean value
     * @since 1.0.0
     */
    boolean getBoolean(@NotNull String key, boolean defaultValue);

    /**
     * Gets any value by key.
     *
     * @param key the data key
     * @return the value, or null if not present
     * @since 1.0.0
     */
    Object get(@NotNull String key);

    /**
     * Checks if a key exists.
     *
     * @param key the data key
     * @return true if the key exists
     * @since 1.0.0
     */
    boolean has(@NotNull String key);
}
