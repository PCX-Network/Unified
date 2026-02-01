/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializer for enchantment data (type and level).
 *
 * <p>EnchantmentSerializer handles the serialization of enchantment data,
 * supporting both single enchantments and maps of multiple enchantments.
 * Enchantment types use namespaced IDs (e.g., "minecraft:sharpness").
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li><b>JSON:</b> Human-readable format like {"type": "minecraft:sharpness", "level": 5}</li>
 *   <li><b>BASE64:</b> Compact binary format</li>
 *   <li><b>BINARY:</b> Raw binary format</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * EnchantmentSerializer serializer = Serializers.enchantment();
 *
 * // Single enchantment
 * Map<String, Object> enchant = Map.of(
 *     "type", "minecraft:sharpness",
 *     "level", 5
 * );
 * String json = serializer.toJson(enchant);
 *
 * // Multiple enchantments map
 * Map<String, Integer> enchants = new LinkedHashMap<>();
 * enchants.put("minecraft:sharpness", 5);
 * enchants.put("minecraft:unbreaking", 3);
 * enchants.put("minecraft:fire_aspect", 2);
 *
 * String encoded = serializer.serializeMap(enchants);
 * Map<String, Integer> restored = serializer.deserializeMap(encoded);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ItemStackSerializer
 * @see ItemMetaSerializer
 */
public final class EnchantmentSerializer implements Serializer<Map<String, Object>> {

    private static final String KEY_TYPE = "type";
    private static final String KEY_LEVEL = "level";

    /**
     * Creates a new EnchantmentSerializer.
     */
    public EnchantmentSerializer() {}

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
        BinaryBuffer buffer = BinaryBuffer.allocate(64);

        String type = (String) value.get(KEY_TYPE);
        int level = ((Number) value.getOrDefault(KEY_LEVEL, 1)).intValue();

        buffer.writeString(type != null ? type : "minecraft:unknown");
        buffer.writeVarInt(level);

        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public Map<String, Object> fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(KEY_TYPE, buffer.readString());
        result.put(KEY_LEVEL, buffer.readVarInt());

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
    // Convenience Methods for Enchantment Maps
    // ========================================

    /**
     * Serializes a map of enchantment types to levels.
     *
     * @param enchantments the enchantments map (type -> level)
     * @return the serialized string
     * @since 1.0.0
     */
    @NotNull
    public String serializeMap(@NotNull Map<String, Integer> enchantments) {
        return serializeMap(enchantments, SerializationContext.base64());
    }

    /**
     * Serializes a map of enchantment types to levels.
     *
     * @param enchantments the enchantments map (type -> level)
     * @param context      the serialization context
     * @return the serialized string
     * @since 1.0.0
     */
    @NotNull
    public String serializeMap(@NotNull Map<String, Integer> enchantments,
                                @NotNull SerializationContext context) {
        if (context.getFormat() == SerializationContext.Format.JSON) {
            return serializeMapToJson(enchantments, context);
        }

        BinaryBuffer buffer = BinaryBuffer.allocate(enchantments.size() * 32);
        buffer.writeVarInt(enchantments.size());

        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            buffer.writeString(entry.getKey());
            buffer.writeVarInt(entry.getValue());
        }

        byte[] bytes = buffer.toByteArray();
        return context.getFormat() == SerializationContext.Format.BASE64 ?
                Base64.getEncoder().encodeToString(bytes) :
                new String(bytes, context.getCharset());
    }

    /**
     * Deserializes a map of enchantment types to levels.
     *
     * @param data the serialized data
     * @return the enchantments map (type -> level)
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Integer> deserializeMap(@NotNull String data) {
        return deserializeMap(data, SerializationContext.base64());
    }

    /**
     * Deserializes a map of enchantment types to levels.
     *
     * @param data    the serialized data
     * @param context the serialization context
     * @return the enchantments map (type -> level)
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Integer> deserializeMap(@NotNull String data,
                                                 @NotNull SerializationContext context) {
        if (context.getFormat() == SerializationContext.Format.JSON) {
            return deserializeMapFromJson(data, context);
        }

        byte[] bytes = context.getFormat() == SerializationContext.Format.BASE64 ?
                Base64.getDecoder().decode(data) :
                data.getBytes(context.getCharset());

        BinaryBuffer buffer = BinaryBuffer.wrap(bytes);
        int count = buffer.readVarInt();

        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(buffer.readString(), buffer.readVarInt());
        }

        return result;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Creates an enchantment data map.
     *
     * @param type  the enchantment type (namespaced ID)
     * @param level the enchantment level
     * @return the enchantment data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@NotNull String type, int level) {
        Map<String, Object> enchant = new LinkedHashMap<>();
        enchant.put(KEY_TYPE, type);
        enchant.put(KEY_LEVEL, level);
        return enchant;
    }

    /**
     * Normalizes an enchantment type to namespaced format.
     *
     * <p>If the type does not contain a namespace, "minecraft:" is prepended.
     *
     * @param type the enchantment type
     * @return the normalized type with namespace
     * @since 1.0.0
     */
    @NotNull
    public static String normalizeType(@NotNull String type) {
        if (!type.contains(":")) {
            return "minecraft:" + type.toLowerCase();
        }
        return type.toLowerCase();
    }

    /**
     * Extracts the enchantment name without namespace.
     *
     * @param type the namespaced enchantment type
     * @return the enchantment name
     * @since 1.0.0
     */
    @NotNull
    public static String getEnchantmentName(@NotNull String type) {
        int colonIndex = type.indexOf(':');
        return colonIndex >= 0 ? type.substring(colonIndex + 1) : type;
    }

    // ========================================
    // JSON Serialization
    // ========================================

    @NotNull
    private String serializeToJson(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        String type = (String) value.get(KEY_TYPE);
        int level = ((Number) value.getOrDefault(KEY_LEVEL, 1)).intValue();

        if (context.isPrettyPrint()) {
            return String.format("{\n  \"type\": \"%s\",\n  \"level\": %d\n}", type, level);
        }
        return String.format("{\"type\":\"%s\",\"level\":%d}", type, level);
    }

    @NotNull
    private Map<String, Object> deserializeFromJson(@NotNull String json, @NotNull SerializationContext context) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Simple JSON parsing for enchantment format
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }

        String type = null;
        int level = 1;

        for (String part : json.split(",")) {
            part = part.trim();
            if (part.contains("\"type\"")) {
                int start = part.indexOf(":") + 1;
                type = part.substring(start).trim().replace("\"", "");
            } else if (part.contains("\"level\"")) {
                int start = part.indexOf(":") + 1;
                level = Integer.parseInt(part.substring(start).trim());
            }
        }

        result.put(KEY_TYPE, type != null ? type : "minecraft:unknown");
        result.put(KEY_LEVEL, level);

        return result;
    }

    @NotNull
    private String serializeMapToJson(@NotNull Map<String, Integer> enchantments,
                                       @NotNull SerializationContext context) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            if (!first) json.append(",");
            first = false;

            if (context.isPrettyPrint()) {
                json.append("\n  ");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            if (context.isPrettyPrint()) json.append(" ");
            json.append(entry.getValue());
        }

        if (context.isPrettyPrint() && !enchantments.isEmpty()) {
            json.append("\n");
        }
        json.append("}");
        return json.toString();
    }

    @NotNull
    private Map<String, Integer> deserializeMapFromJson(@NotNull String json,
                                                          @NotNull SerializationContext context) {
        Map<String, Integer> result = new LinkedHashMap<>();

        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }

        for (String part : json.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;

            int colonIndex = part.indexOf(":");
            if (colonIndex > 0) {
                String key = part.substring(0, colonIndex).trim().replace("\"", "");
                int value = Integer.parseInt(part.substring(colonIndex + 1).trim());
                result.put(key, value);
            }
        }

        return result;
    }
}
