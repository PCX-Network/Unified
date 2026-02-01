/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of the BlockData.Builder interface.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
final class BlockDataBuilder implements BlockData.Builder {

    private static final Pattern BLOCK_DATA_PATTERN = Pattern.compile(
            "([a-z_:]+)(?:\\[([^]]+)])?");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "([a-z_]+)=([a-zA-Z0-9_]+)");

    private final BlockType blockType;
    private final Map<String, Object> properties = new HashMap<>();

    BlockDataBuilder(@NotNull BlockType blockType) {
        this.blockType = Objects.requireNonNull(blockType, "blockType cannot be null");
    }

    @Override
    @NotNull
    public BlockData.Builder property(@NotNull String property, @NotNull String value) {
        properties.put(property, value);
        return this;
    }

    @Override
    @NotNull
    public BlockData.Builder property(@NotNull String property, int value) {
        properties.put(property, value);
        return this;
    }

    @Override
    @NotNull
    public BlockData.Builder property(@NotNull String property, boolean value) {
        properties.put(property, value);
        return this;
    }

    @Override
    @NotNull
    public BlockData.Builder facing(@NotNull String direction) {
        return property("facing", direction.toLowerCase());
    }

    @Override
    @NotNull
    public BlockData.Builder axis(@NotNull String axis) {
        return property("axis", axis.toLowerCase());
    }

    @Override
    @NotNull
    public BlockData.Builder waterlogged(boolean waterlogged) {
        return property("waterlogged", waterlogged);
    }

    @Override
    @NotNull
    public BlockData.Builder half(@NotNull String half) {
        return property("half", half.toLowerCase());
    }

    @Override
    @NotNull
    public BlockData.Builder age(int age) {
        return property("age", age);
    }

    @Override
    @NotNull
    public BlockData.Builder power(int power) {
        return property("power", Math.max(0, Math.min(15, power)));
    }

    @Override
    @NotNull
    public BlockData build() {
        return new BlockDataImpl(blockType, Map.copyOf(properties));
    }

    /**
     * Parses block data from a string representation.
     */
    @Nullable
    static BlockData parse(@NotNull String data) {
        Matcher matcher = BLOCK_DATA_PATTERN.matcher(data);
        if (!matcher.matches()) {
            return null;
        }

        BlockType blockType = BlockType.of(matcher.group(1));
        BlockDataBuilder builder = new BlockDataBuilder(blockType);

        String propertiesStr = matcher.group(2);
        if (propertiesStr != null && !propertiesStr.isEmpty()) {
            Matcher propMatcher = PROPERTY_PATTERN.matcher(propertiesStr);
            while (propMatcher.find()) {
                String key = propMatcher.group(1);
                String value = propMatcher.group(2);
                builder.properties.put(key, parseValue(value));
            }
        }

        return builder.build();
    }

    private static Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * Internal implementation of BlockData.
     */
    private record BlockDataImpl(
            BlockType blockType,
            Map<String, Object> properties
    ) implements BlockData {

        @Override
        @NotNull
        public BlockType getBlockType() {
            return blockType;
        }

        @Override
        @NotNull
        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        @NotNull
        public Optional<Object> getProperty(@NotNull String property) {
            return Optional.ofNullable(properties.get(property));
        }

        @Override
        @NotNull
        public String getString(@NotNull String property, @NotNull String defaultValue) {
            Object value = properties.get(property);
            return value != null ? value.toString() : defaultValue;
        }

        @Override
        public int getInt(@NotNull String property, int defaultValue) {
            Object value = properties.get(property);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String str) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }

        @Override
        public boolean getBoolean(@NotNull String property, boolean defaultValue) {
            Object value = properties.get(property);
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof String str) {
                return Boolean.parseBoolean(str);
            }
            return defaultValue;
        }

        @Override
        @NotNull
        public BlockData withProperty(@NotNull String property, @NotNull Object value) {
            Map<String, Object> newProperties = new HashMap<>(properties);
            newProperties.put(property, value);
            return new BlockDataImpl(blockType, Map.copyOf(newProperties));
        }

        @Override
        @NotNull
        public String asString() {
            if (properties.isEmpty()) {
                return blockType.key();
            }

            StringJoiner joiner = new StringJoiner(",", "[", "]");
            properties.forEach((key, value) -> {
                String valueStr = value instanceof Boolean b ? (b ? "true" : "false") : value.toString();
                joiner.add(key + "=" + valueStr);
            });

            return blockType.key() + joiner;
        }

        @Override
        @NotNull
        @SuppressWarnings("unchecked")
        public <T> T getHandle() {
            // This would be replaced by platform-specific implementations
            throw new UnsupportedOperationException(
                    "getHandle() requires a platform-specific implementation");
        }

        @Override
        public String toString() {
            return asString();
        }
    }
}
