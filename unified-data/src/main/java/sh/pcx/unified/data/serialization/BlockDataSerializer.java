/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializer for block data including material and block states.
 *
 * <p>BlockDataSerializer handles the serialization of block data, including
 * the material type and all block state properties. This is useful for
 * storing block configurations, schematics, and world edits.
 *
 * <h2>Block Data Properties</h2>
 * <ul>
 *   <li><b>type:</b> The block type (namespaced ID, e.g., "minecraft:oak_stairs")</li>
 *   <li><b>states:</b> Map of state properties (e.g., {"facing": "north", "half": "bottom"})</li>
 *   <li><b>nbt:</b> Optional NBT data for tile entities</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * BlockDataSerializer serializer = Serializers.blockData();
 *
 * // Create block data
 * Map<String, Object> blockData = Map.of(
 *     "type", "minecraft:oak_stairs",
 *     "states", Map.of(
 *         "facing", "north",
 *         "half", "bottom",
 *         "shape", "straight",
 *         "waterlogged", "false"
 *     )
 * );
 *
 * // Serialize
 * String json = serializer.toJson(blockData);
 * String base64 = serializer.toBase64(blockData);
 *
 * // Deserialize
 * Map<String, Object> restored = serializer.fromJson(json);
 * }</pre>
 *
 * <h2>Block State Format</h2>
 * <p>Block states are stored as string key-value pairs to match Minecraft's
 * internal format. Boolean states are stored as "true" or "false" strings.
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LocationSerializer
 * @see ChunkSerializer
 */
public final class BlockDataSerializer implements Serializer<Map<String, Object>> {

    // JSON/map keys
    private static final String KEY_TYPE = "type";
    private static final String KEY_STATES = "states";
    private static final String KEY_NBT = "nbt";

    /**
     * Creates a new BlockDataSerializer.
     */
    public BlockDataSerializer() {}

    @Override
    @NotNull
    public String serialize(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return switch (context.getFormat()) {
            case JSON -> serializeToJson(value, context);
            case BASE64 -> Base64.getEncoder().encodeToString(toBytes(value, context));
            case BINARY -> new String(toBytes(value, context), context.getCharset());
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };
    }

    @Override
    @NotNull
    public Map<String, Object> deserialize(@NotNull String data, @NotNull SerializationContext context) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return switch (context.getFormat()) {
            case JSON -> deserializeFromJson(data, context);
            case BASE64 -> fromBytes(Base64.getDecoder().decode(data), context);
            case BINARY -> fromBytes(data.getBytes(context.getCharset()), context);
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };
    }

    @Override
    public byte @NotNull [] toBytes(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.allocate(128);

        // Write block type
        String type = (String) value.get(KEY_TYPE);
        buffer.writeString(type != null ? type : "minecraft:air");

        // Write block states
        @SuppressWarnings("unchecked")
        Map<String, String> states = (Map<String, String>) value.get(KEY_STATES);
        if (states != null && !states.isEmpty()) {
            buffer.writeVarInt(states.size());
            for (Map.Entry<String, String> entry : states.entrySet()) {
                buffer.writeString(entry.getKey());
                buffer.writeString(entry.getValue());
            }
        } else {
            buffer.writeVarInt(0);
        }

        // Write NBT data if present
        Object nbt = value.get(KEY_NBT);
        if (nbt instanceof byte[] nbtBytes) {
            buffer.writeBoolean(true);
            buffer.writeBytes(nbtBytes);
        } else {
            buffer.writeBoolean(false);
        }

        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public Map<String, Object> fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);
        Map<String, Object> result = new LinkedHashMap<>();

        // Read block type
        result.put(KEY_TYPE, buffer.readString());

        // Read block states
        int stateCount = buffer.readVarInt();
        if (stateCount > 0) {
            Map<String, String> states = new LinkedHashMap<>();
            for (int i = 0; i < stateCount; i++) {
                states.put(buffer.readString(), buffer.readString());
            }
            result.put(KEY_STATES, states);
        }

        // Read NBT data if present
        if (buffer.readBoolean()) {
            result.put(KEY_NBT, buffer.readBytes());
        }

        return result;
    }

    @Override
    @NotNull
    public Class<Map<String, Object>> getTargetType() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> type = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return type;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Creates block data with type only (default states).
     *
     * @param type the block type (namespaced ID)
     * @return the block data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@NotNull String type) {
        Map<String, Object> blockData = new LinkedHashMap<>();
        blockData.put(KEY_TYPE, type);
        return blockData;
    }

    /**
     * Creates block data with type and states.
     *
     * @param type   the block type (namespaced ID)
     * @param states the block state properties
     * @return the block data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@NotNull String type, @NotNull Map<String, String> states) {
        Map<String, Object> blockData = create(type);
        if (!states.isEmpty()) {
            blockData.put(KEY_STATES, new LinkedHashMap<>(states));
        }
        return blockData;
    }

    /**
     * Parses a Minecraft block state string.
     *
     * <p>Format: "minecraft:oak_stairs[facing=north,half=bottom]"
     *
     * @param blockStateString the block state string
     * @return the parsed block data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> parse(@NotNull String blockStateString) {
        Map<String, Object> result = new LinkedHashMap<>();

        int bracketStart = blockStateString.indexOf('[');
        if (bracketStart < 0) {
            result.put(KEY_TYPE, blockStateString);
            return result;
        }

        result.put(KEY_TYPE, blockStateString.substring(0, bracketStart));

        int bracketEnd = blockStateString.lastIndexOf(']');
        if (bracketEnd > bracketStart + 1) {
            String statesStr = blockStateString.substring(bracketStart + 1, bracketEnd);
            Map<String, String> states = new LinkedHashMap<>();

            for (String state : statesStr.split(",")) {
                String[] parts = state.split("=", 2);
                if (parts.length == 2) {
                    states.put(parts[0].trim(), parts[1].trim());
                }
            }

            if (!states.isEmpty()) {
                result.put(KEY_STATES, states);
            }
        }

        return result;
    }

    /**
     * Converts block data to a Minecraft block state string.
     *
     * <p>Output format: "minecraft:oak_stairs[facing=north,half=bottom]"
     *
     * @param blockData the block data map
     * @return the block state string
     * @since 1.0.0
     */
    @NotNull
    public static String toStateString(@NotNull Map<String, Object> blockData) {
        StringBuilder sb = new StringBuilder();
        sb.append(blockData.getOrDefault(KEY_TYPE, "minecraft:air"));

        @SuppressWarnings("unchecked")
        Map<String, String> states = (Map<String, String>) blockData.get(KEY_STATES);
        if (states != null && !states.isEmpty()) {
            sb.append("[");
            boolean first = true;
            for (Map.Entry<String, String> entry : states.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            sb.append("]");
        }

        return sb.toString();
    }

    /**
     * Gets a block state property value.
     *
     * @param blockData    the block data map
     * @param property     the property name
     * @param defaultValue the default value if not present
     * @return the property value or default
     * @since 1.0.0
     */
    @NotNull
    public static String getState(@NotNull Map<String, Object> blockData,
                                   @NotNull String property,
                                   @NotNull String defaultValue) {
        @SuppressWarnings("unchecked")
        Map<String, String> states = (Map<String, String>) blockData.get(KEY_STATES);
        if (states == null) {
            return defaultValue;
        }
        return states.getOrDefault(property, defaultValue);
    }

    /**
     * Sets a block state property value.
     *
     * @param blockData the block data map (modified in place)
     * @param property  the property name
     * @param value     the property value
     * @since 1.0.0
     */
    public static void setState(@NotNull Map<String, Object> blockData,
                                 @NotNull String property,
                                 @NotNull String value) {
        @SuppressWarnings("unchecked")
        Map<String, String> states = (Map<String, String>) blockData.computeIfAbsent(
                KEY_STATES, k -> new LinkedHashMap<>());
        states.put(property, value);
    }

    /**
     * Creates a copy of block data with a modified state.
     *
     * @param blockData the original block data
     * @param property  the property to modify
     * @param value     the new value
     * @return a new block data map with the modified state
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> withState(@NotNull Map<String, Object> blockData,
                                                  @NotNull String property,
                                                  @NotNull String value) {
        Map<String, Object> copy = new LinkedHashMap<>(blockData);

        @SuppressWarnings("unchecked")
        Map<String, String> originalStates = (Map<String, String>) blockData.get(KEY_STATES);
        Map<String, String> newStates = originalStates != null ?
                new LinkedHashMap<>(originalStates) : new LinkedHashMap<>();
        newStates.put(property, value);
        copy.put(KEY_STATES, newStates);

        return copy;
    }

    /**
     * Checks if the block is air or void.
     *
     * @param blockData the block data map
     * @return true if air
     * @since 1.0.0
     */
    public static boolean isAir(@NotNull Map<String, Object> blockData) {
        String type = (String) blockData.get(KEY_TYPE);
        return type == null ||
               type.equals("minecraft:air") ||
               type.equals("minecraft:void_air") ||
               type.equals("minecraft:cave_air");
    }

    // ========================================
    // JSON Serialization
    // ========================================

    @NotNull
    private String serializeToJson(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        StringBuilder json = new StringBuilder("{");

        String type = (String) value.get(KEY_TYPE);
        json.append("\"type\":\"").append(type != null ? type : "minecraft:air").append("\"");

        @SuppressWarnings("unchecked")
        Map<String, String> states = (Map<String, String>) value.get(KEY_STATES);
        if (states != null && !states.isEmpty()) {
            json.append(",\"states\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : states.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            }
            json.append("}");
        }

        Object nbt = value.get(KEY_NBT);
        if (nbt instanceof byte[] nbtBytes) {
            json.append(",\"nbt\":\"").append(Base64.getEncoder().encodeToString(nbtBytes)).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    @NotNull
    private Map<String, Object> deserializeFromJson(@NotNull String json, @NotNull SerializationContext context) {
        Map<String, Object> result = new LinkedHashMap<>();

        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }

        // Simple parsing - extract type, states, and nbt
        int typeStart = json.indexOf("\"type\"");
        if (typeStart >= 0) {
            int valueStart = json.indexOf(":", typeStart) + 1;
            int valueEnd = findJsonValueEnd(json, valueStart);
            String type = json.substring(valueStart, valueEnd).trim().replace("\"", "");
            result.put(KEY_TYPE, type);
        }

        int statesStart = json.indexOf("\"states\"");
        if (statesStart >= 0) {
            int objStart = json.indexOf("{", statesStart);
            int objEnd = findObjectEnd(json, objStart);
            String statesJson = json.substring(objStart + 1, objEnd);

            Map<String, String> states = new LinkedHashMap<>();
            for (String pair : statesJson.split(",")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    states.put(kv[0].trim().replace("\"", ""), kv[1].trim().replace("\"", ""));
                }
            }
            if (!states.isEmpty()) {
                result.put(KEY_STATES, states);
            }
        }

        int nbtStart = json.indexOf("\"nbt\"");
        if (nbtStart >= 0) {
            int valueStart = json.indexOf(":", nbtStart) + 1;
            int valueEnd = findJsonValueEnd(json, valueStart);
            String nbtBase64 = json.substring(valueStart, valueEnd).trim().replace("\"", "");
            result.put(KEY_NBT, Base64.getDecoder().decode(nbtBase64));
        }

        return result;
    }

    private int findJsonValueEnd(String json, int start) {
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return json.length();

        char c = json.charAt(start);
        if (c == '"') {
            int end = start + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                    return end + 1;
                }
                end++;
            }
            return json.length();
        } else if (c == '{') {
            return findObjectEnd(json, start) + 1;
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return end;
        }
    }

    private int findObjectEnd(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return json.length() - 1;
    }
}
