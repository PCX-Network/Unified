/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import org.jetbrains.annotations.NotNull;

/**
 * Simple implementation of TriggerData.
 */
record SimpleTriggerData(java.util.Map<String, Object> data) implements TriggerData {
    static final SimpleTriggerData EMPTY = new SimpleTriggerData(java.util.Map.of());

    @Override
    public String getString(@NotNull String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public int getInt(@NotNull String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    @Override
    public double getDouble(@NotNull String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return defaultValue;
    }

    @Override
    public boolean getBoolean(@NotNull String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }

    @Override
    public Object get(@NotNull String key) {
        return data.get(key);
    }

    @Override
    public boolean has(@NotNull String key) {
        return data.containsKey(key);
    }
}
