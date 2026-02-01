/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializer for potion effect data.
 *
 * <p>PotionEffectSerializer handles the serialization of potion effects,
 * including effect type, duration, amplifier, and display properties.
 * Supports both single effects and lists of effects.
 *
 * <h2>Effect Properties</h2>
 * <ul>
 *   <li><b>type:</b> The effect type (namespaced ID, e.g., "minecraft:speed")</li>
 *   <li><b>duration:</b> Duration in ticks (20 ticks = 1 second)</li>
 *   <li><b>amplifier:</b> Effect level (0 = level I, 1 = level II, etc.)</li>
 *   <li><b>ambient:</b> Whether this is an ambient effect (from beacon)</li>
 *   <li><b>particles:</b> Whether to show particles</li>
 *   <li><b>icon:</b> Whether to show the icon in the HUD</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PotionEffectSerializer serializer = Serializers.potionEffect();
 *
 * // Create effect data
 * Map<String, Object> effect = Map.of(
 *     "type", "minecraft:speed",
 *     "duration", 6000, // 5 minutes
 *     "amplifier", 1,   // Speed II
 *     "ambient", false,
 *     "particles", true,
 *     "icon", true
 * );
 *
 * // Serialize
 * String json = serializer.toJson(effect);
 * String base64 = serializer.toBase64(effect);
 *
 * // Serialize multiple effects
 * List<Map<String, Object>> effects = List.of(
 *     Map.of("type", "minecraft:speed", "duration", 6000, "amplifier", 1),
 *     Map.of("type", "minecraft:strength", "duration", 6000, "amplifier", 0)
 * );
 * String encoded = serializer.serializeList(effects);
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
public final class PotionEffectSerializer implements Serializer<Map<String, Object>> {

    // JSON/map keys
    private static final String KEY_TYPE = "type";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_AMPLIFIER = "amplifier";
    private static final String KEY_AMBIENT = "ambient";
    private static final String KEY_PARTICLES = "particles";
    private static final String KEY_ICON = "icon";

    // Defaults
    private static final int DEFAULT_DURATION = 600; // 30 seconds
    private static final int DEFAULT_AMPLIFIER = 0;  // Level I

    /**
     * Creates a new PotionEffectSerializer.
     */
    public PotionEffectSerializer() {}

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

        // Write type
        String type = (String) value.get(KEY_TYPE);
        buffer.writeString(type != null ? type : "minecraft:unknown");

        // Write duration and amplifier
        int duration = getInt(value, KEY_DURATION, DEFAULT_DURATION);
        int amplifier = getInt(value, KEY_AMPLIFIER, DEFAULT_AMPLIFIER);
        buffer.writeVarInt(duration);
        buffer.writeVarInt(amplifier);

        // Write boolean flags as a single byte
        boolean ambient = getBoolean(value, KEY_AMBIENT, false);
        boolean particles = getBoolean(value, KEY_PARTICLES, true);
        boolean icon = getBoolean(value, KEY_ICON, true);
        int flags = (ambient ? 1 : 0) | (particles ? 2 : 0) | (icon ? 4 : 0);
        buffer.writeByte(flags);

        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public Map<String, Object> fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put(KEY_TYPE, buffer.readString());
        result.put(KEY_DURATION, buffer.readVarInt());
        result.put(KEY_AMPLIFIER, buffer.readVarInt());

        int flags = buffer.readUnsignedByte();
        result.put(KEY_AMBIENT, (flags & 1) != 0);
        result.put(KEY_PARTICLES, (flags & 2) != 0);
        result.put(KEY_ICON, (flags & 4) != 0);

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
    // List Serialization
    // ========================================

    /**
     * Serializes a list of potion effects.
     *
     * @param effects the list of effect data maps
     * @return the serialized string
     * @since 1.0.0
     */
    @NotNull
    public String serializeList(@NotNull List<Map<String, Object>> effects) {
        return serializeList(effects, SerializationContext.base64());
    }

    /**
     * Serializes a list of potion effects.
     *
     * @param effects the list of effect data maps
     * @param context the serialization context
     * @return the serialized string
     * @since 1.0.0
     */
    @NotNull
    public String serializeList(@NotNull List<Map<String, Object>> effects,
                                 @NotNull SerializationContext context) {
        if (context.getFormat() == SerializationContext.Format.JSON) {
            return serializeListToJson(effects, context);
        }

        BinaryBuffer buffer = BinaryBuffer.allocate(effects.size() * 32);
        buffer.writeVarInt(effects.size());

        for (Map<String, Object> effect : effects) {
            byte[] effectBytes = toBytes(effect, context);
            buffer.writeBytes(effectBytes);
        }

        byte[] bytes = buffer.toByteArray();
        return context.getFormat() == SerializationContext.Format.BASE64 ?
                Base64.getEncoder().encodeToString(bytes) :
                new String(bytes, context.getCharset());
    }

    /**
     * Deserializes a list of potion effects.
     *
     * @param data the serialized data
     * @return the list of effect data maps
     * @since 1.0.0
     */
    @NotNull
    public List<Map<String, Object>> deserializeList(@NotNull String data) {
        return deserializeList(data, SerializationContext.base64());
    }

    /**
     * Deserializes a list of potion effects.
     *
     * @param data    the serialized data
     * @param context the serialization context
     * @return the list of effect data maps
     * @since 1.0.0
     */
    @NotNull
    public List<Map<String, Object>> deserializeList(@NotNull String data,
                                                       @NotNull SerializationContext context) {
        if (context.getFormat() == SerializationContext.Format.JSON) {
            return deserializeListFromJson(data, context);
        }

        byte[] bytes = context.getFormat() == SerializationContext.Format.BASE64 ?
                Base64.getDecoder().decode(data) :
                data.getBytes(context.getCharset());

        BinaryBuffer buffer = BinaryBuffer.wrap(bytes);
        int count = buffer.readVarInt();

        List<Map<String, Object>> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] effectBytes = buffer.readBytes();
            result.add(fromBytes(effectBytes, context));
        }

        return result;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Creates a potion effect data map with all properties.
     *
     * @param type      the effect type (namespaced ID)
     * @param duration  duration in ticks
     * @param amplifier effect amplifier (0 = level I)
     * @param ambient   whether ambient
     * @param particles whether to show particles
     * @param icon      whether to show icon
     * @return the effect data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@NotNull String type, int duration, int amplifier,
                                              boolean ambient, boolean particles, boolean icon) {
        Map<String, Object> effect = new LinkedHashMap<>();
        effect.put(KEY_TYPE, type);
        effect.put(KEY_DURATION, duration);
        effect.put(KEY_AMPLIFIER, amplifier);
        effect.put(KEY_AMBIENT, ambient);
        effect.put(KEY_PARTICLES, particles);
        effect.put(KEY_ICON, icon);
        return effect;
    }

    /**
     * Creates a potion effect data map with default display properties.
     *
     * @param type      the effect type (namespaced ID)
     * @param duration  duration in ticks
     * @param amplifier effect amplifier (0 = level I)
     * @return the effect data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@NotNull String type, int duration, int amplifier) {
        return create(type, duration, amplifier, false, true, true);
    }

    /**
     * Normalizes a potion effect type to namespaced format.
     *
     * @param type the effect type
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
     * Converts duration from ticks to seconds.
     *
     * @param ticks the duration in ticks
     * @return the duration in seconds
     * @since 1.0.0
     */
    public static double ticksToSeconds(int ticks) {
        return ticks / 20.0;
    }

    /**
     * Converts duration from seconds to ticks.
     *
     * @param seconds the duration in seconds
     * @return the duration in ticks
     * @since 1.0.0
     */
    public static int secondsToTicks(double seconds) {
        return (int) (seconds * 20);
    }

    /**
     * Gets the display level for an amplifier.
     *
     * @param amplifier the amplifier (0-based)
     * @return the display level (1-based) as a Roman numeral
     * @since 1.0.0
     */
    @NotNull
    public static String getDisplayLevel(int amplifier) {
        int level = amplifier + 1;
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }

    // ========================================
    // JSON Serialization
    // ========================================

    @NotNull
    private String serializeToJson(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        StringBuilder json = new StringBuilder("{");

        String type = (String) value.get(KEY_TYPE);
        int duration = getInt(value, KEY_DURATION, DEFAULT_DURATION);
        int amplifier = getInt(value, KEY_AMPLIFIER, DEFAULT_AMPLIFIER);
        boolean ambient = getBoolean(value, KEY_AMBIENT, false);
        boolean particles = getBoolean(value, KEY_PARTICLES, true);
        boolean icon = getBoolean(value, KEY_ICON, true);

        if (context.isPrettyPrint()) {
            json.append("\n  \"type\": \"").append(type).append("\",");
            json.append("\n  \"duration\": ").append(duration).append(",");
            json.append("\n  \"amplifier\": ").append(amplifier).append(",");
            json.append("\n  \"ambient\": ").append(ambient).append(",");
            json.append("\n  \"particles\": ").append(particles).append(",");
            json.append("\n  \"icon\": ").append(icon);
            json.append("\n}");
        } else {
            json.append("\"type\":\"").append(type).append("\",");
            json.append("\"duration\":").append(duration).append(",");
            json.append("\"amplifier\":").append(amplifier).append(",");
            json.append("\"ambient\":").append(ambient).append(",");
            json.append("\"particles\":").append(particles).append(",");
            json.append("\"icon\":").append(icon).append("}");
        }

        return json.toString();
    }

    @NotNull
    private Map<String, Object> deserializeFromJson(@NotNull String json, @NotNull SerializationContext context) {
        Map<String, Object> result = new LinkedHashMap<>();

        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }

        for (String part : json.split(",")) {
            part = part.trim();
            int colonIndex = part.indexOf(":");
            if (colonIndex > 0) {
                String key = part.substring(0, colonIndex).trim().replace("\"", "");
                String valueStr = part.substring(colonIndex + 1).trim().replace("\"", "");

                switch (key) {
                    case KEY_TYPE -> result.put(KEY_TYPE, valueStr);
                    case KEY_DURATION -> result.put(KEY_DURATION, Integer.parseInt(valueStr));
                    case KEY_AMPLIFIER -> result.put(KEY_AMPLIFIER, Integer.parseInt(valueStr));
                    case KEY_AMBIENT -> result.put(KEY_AMBIENT, Boolean.parseBoolean(valueStr));
                    case KEY_PARTICLES -> result.put(KEY_PARTICLES, Boolean.parseBoolean(valueStr));
                    case KEY_ICON -> result.put(KEY_ICON, Boolean.parseBoolean(valueStr));
                }
            }
        }

        return result;
    }

    @NotNull
    private String serializeListToJson(@NotNull List<Map<String, Object>> effects,
                                        @NotNull SerializationContext context) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Map<String, Object> effect : effects) {
            if (!first) json.append(",");
            first = false;
            if (context.isPrettyPrint()) json.append("\n  ");
            json.append(serializeToJson(effect, context.withFormat(SerializationContext.Format.JSON)));
        }

        if (context.isPrettyPrint() && !effects.isEmpty()) json.append("\n");
        json.append("]");
        return json.toString();
    }

    @NotNull
    private List<Map<String, Object>> deserializeListFromJson(@NotNull String json,
                                                                @NotNull SerializationContext context) {
        List<Map<String, Object>> result = new ArrayList<>();

        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            return result;
        }

        // Simple array parsing - find each object
        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(deserializeFromJson(json.substring(start, i + 1), context));
                    start = -1;
                }
            }
        }

        return result;
    }

    // ========================================
    // Helper Methods
    // ========================================

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.intValue();
        }
        return defaultValue;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }
}
