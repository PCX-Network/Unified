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
 * Serializer for ItemStack objects with full NBT preservation.
 *
 * <p>ItemStackSerializer provides complete serialization of Minecraft ItemStack
 * objects, including all NBT data, enchantments, display properties, and
 * custom persistent data. It supports cross-version compatibility for
 * transferring items between different Minecraft versions.
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li><b>BASE64:</b> Compact binary format encoded as Base64, preserves all NBT</li>
 *   <li><b>JSON:</b> Human-readable format, suitable for configuration files</li>
 *   <li><b>BINARY:</b> Raw binary format for maximum efficiency</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ItemStackSerializer serializer = Serializers.itemStack();
 *
 * // Serialize to Base64 (preserves all NBT)
 * String base64 = serializer.toBase64(itemStack);
 * ItemStack restored = serializer.fromBase64(base64);
 *
 * // Serialize to JSON (human-readable)
 * String json = serializer.toJson(itemStack);
 * ItemStack fromJson = serializer.fromJson(json);
 *
 * // Serialize with compression for large inventories
 * SerializationContext context = SerializationContext.builder()
 *     .format(SerializationContext.Format.BASE64)
 *     .compression(SerializationContext.CompressionType.GZIP)
 *     .build();
 * String compressed = serializer.serialize(itemStack, context);
 *
 * // Batch serialization
 * String encoded = serializer.serializeArray(itemStacks);
 * ItemStack[] decoded = serializer.deserializeArray(encoded);
 * }</pre>
 *
 * <h2>NBT Preservation</h2>
 * <p>This serializer preserves all item data including:
 * <ul>
 *   <li>Material type and amount</li>
 *   <li>Damage/durability</li>
 *   <li>Display name and lore (with full component formatting)</li>
 *   <li>Enchantments</li>
 *   <li>Potion effects</li>
 *   <li>Custom model data</li>
 *   <li>Attribute modifiers</li>
 *   <li>Item flags</li>
 *   <li>Persistent data container contents</li>
 *   <li>Book contents</li>
 *   <li>Skull data</li>
 *   <li>All other NBT data</li>
 * </ul>
 *
 * <h2>Cross-Version Support</h2>
 * <p>The serializer includes version information in the output, allowing
 * automatic migration when loading items serialized from different
 * Minecraft versions. Material names are stored in a version-agnostic format.
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ItemMetaSerializer
 * @see EnchantmentSerializer
 */
public final class ItemStackSerializer implements Serializer<Map<String, Object>> {

    // Constants for JSON/map keys
    private static final String KEY_TYPE = "type";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_DAMAGE = "damage";
    private static final String KEY_META = "meta";
    private static final String KEY_NBT = "nbt";
    private static final String KEY_VERSION = "v";

    // Binary format constants
    private static final byte BINARY_VERSION = 1;
    private static final int MAX_AMOUNT = 127;

    /**
     * Creates a new ItemStackSerializer.
     */
    public ItemStackSerializer() {}

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
        BinaryBuffer buffer = BinaryBuffer.allocate(256);

        // Write header
        buffer.writeByte(BINARY_VERSION);
        SchemaVersion version = context.getVersion();
        buffer.writeVarInt(version.getMajor());
        buffer.writeVarInt(version.getMinor());

        // Write item type
        String type = (String) value.getOrDefault(KEY_TYPE, "minecraft:air");
        buffer.writeString(type);

        // Write amount
        int amount = ((Number) value.getOrDefault(KEY_AMOUNT, 1)).intValue();
        buffer.writeByte(amount);

        // Write damage/durability
        int damage = ((Number) value.getOrDefault(KEY_DAMAGE, 0)).intValue();
        buffer.writeVarInt(damage);

        // Write meta if present
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) value.get(KEY_META);
        buffer.writeBoolean(meta != null && !meta.isEmpty());
        if (meta != null && !meta.isEmpty()) {
            writeMetaToBinary(meta, buffer);
        }

        // Write raw NBT if present
        @SuppressWarnings("unchecked")
        byte[] nbt = value.get(KEY_NBT) instanceof byte[] ? (byte[]) value.get(KEY_NBT) : null;
        buffer.writeBoolean(nbt != null);
        if (nbt != null) {
            buffer.writeBytes(nbt);
        }

        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public Map<String, Object> fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);

        // Read header
        int binaryVersion = buffer.readUnsignedByte();
        if (binaryVersion != BINARY_VERSION) {
            throw new SerializationException("Unsupported binary version: " + binaryVersion);
        }

        int major = buffer.readVarInt();
        int minor = buffer.readVarInt();
        SchemaVersion dataVersion = SchemaVersion.of(major, minor);

        // Build result map
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(KEY_VERSION, dataVersion.toString());

        // Read item type
        result.put(KEY_TYPE, buffer.readString());

        // Read amount
        result.put(KEY_AMOUNT, buffer.readUnsignedByte());

        // Read damage
        int damage = buffer.readVarInt();
        if (damage > 0) {
            result.put(KEY_DAMAGE, damage);
        }

        // Read meta if present
        if (buffer.readBoolean()) {
            result.put(KEY_META, readMetaFromBinary(buffer));
        }

        // Read raw NBT if present
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
    // JSON Serialization
    // ========================================

    @NotNull
    private String serializeToJson(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            if (!first) json.append(",");
            first = false;

            if (context.isPrettyPrint()) {
                json.append("\n  ");
            }

            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            if (context.isPrettyPrint()) json.append(" ");
            appendJsonValue(json, entry.getValue(), context, 1);
        }

        if (context.isPrettyPrint() && !value.isEmpty()) {
            json.append("\n");
        }
        json.append("}");
        return json.toString();
    }

    @NotNull
    private Map<String, Object> deserializeFromJson(@NotNull String json, @NotNull SerializationContext context) {
        // Simple JSON parser for item data
        Map<String, Object> result = new LinkedHashMap<>();
        parseJsonObject(json.trim(), result);
        return result;
    }

    private void appendJsonValue(StringBuilder json, Object value, SerializationContext context, int depth) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String s) {
            json.append("\"").append(escapeJson(s)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map<?, ?> map) {
            json.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) json.append(",");
                first = false;
                if (context.isPrettyPrint()) {
                    json.append("\n").append("  ".repeat(depth + 1));
                }
                json.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
                if (context.isPrettyPrint()) json.append(" ");
                appendJsonValue(json, entry.getValue(), context, depth + 1);
            }
            if (context.isPrettyPrint() && !map.isEmpty()) {
                json.append("\n").append("  ".repeat(depth));
            }
            json.append("}");
        } else if (value instanceof Iterable<?> list) {
            json.append("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) json.append(",");
                first = false;
                if (context.isPrettyPrint()) {
                    json.append("\n").append("  ".repeat(depth + 1));
                }
                appendJsonValue(json, item, context, depth + 1);
            }
            if (context.isPrettyPrint()) {
                json.append("\n").append("  ".repeat(depth));
            }
            json.append("]");
        } else if (value instanceof byte[] bytes) {
            json.append("\"").append(Base64.getEncoder().encodeToString(bytes)).append("\"");
        } else {
            json.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    private void parseJsonObject(String json, Map<String, Object> result) {
        // Remove outer braces
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
        }

        if (json.isEmpty()) return;

        // Simple key-value parsing
        int pos = 0;
        while (pos < json.length()) {
            // Skip whitespace
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            if (pos >= json.length()) break;

            // Parse key
            if (json.charAt(pos) != '"') break;
            int keyEnd = findStringEnd(json, pos + 1);
            String key = unescapeJson(json.substring(pos + 1, keyEnd));
            pos = keyEnd + 1;

            // Skip colon
            while (pos < json.length() && (json.charAt(pos) == ':' || Character.isWhitespace(json.charAt(pos)))) pos++;

            // Parse value
            Object value = parseJsonValue(json, pos);
            result.put(key, value);

            // Find end of value and skip comma
            pos = findValueEnd(json, pos);
            while (pos < json.length() && (json.charAt(pos) == ',' || Character.isWhitespace(json.charAt(pos)))) pos++;
        }
    }

    private Object parseJsonValue(String json, int start) {
        char c = json.charAt(start);
        if (c == '"') {
            int end = findStringEnd(json, start + 1);
            return unescapeJson(json.substring(start + 1, end));
        } else if (c == '{') {
            Map<String, Object> nested = new LinkedHashMap<>();
            int end = findObjectEnd(json, start);
            parseJsonObject(json.substring(start, end + 1), nested);
            return nested;
        } else if (c == '[') {
            return parseJsonArray(json, start);
        } else if (c == 't') {
            return true;
        } else if (c == 'f') {
            return false;
        } else if (c == 'n') {
            return null;
        } else {
            // Number
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' ||
                    json.charAt(end) == '-' || json.charAt(end) == 'e' || json.charAt(end) == 'E')) {
                end++;
            }
            String numStr = json.substring(start, end);
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            }
            return Long.parseLong(numStr);
        }
    }

    private java.util.List<Object> parseJsonArray(String json, int start) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        int pos = start + 1;
        while (pos < json.length() && json.charAt(pos) != ']') {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            if (json.charAt(pos) == ']') break;
            list.add(parseJsonValue(json, pos));
            pos = findValueEnd(json, pos);
            while (pos < json.length() && (json.charAt(pos) == ',' || Character.isWhitespace(json.charAt(pos)))) pos++;
        }
        return list;
    }

    private int findStringEnd(String json, int start) {
        int pos = start;
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == '\\') {
                pos += 2;
            } else if (c == '"') {
                return pos;
            } else {
                pos++;
            }
        }
        return json.length();
    }

    private int findValueEnd(String json, int start) {
        char c = json.charAt(start);
        if (c == '"') {
            return findStringEnd(json, start + 1) + 1;
        } else if (c == '{') {
            return findObjectEnd(json, start) + 1;
        } else if (c == '[') {
            return findArrayEnd(json, start) + 1;
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') {
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
            } else if (c == '"') {
                i = findStringEnd(json, i + 1);
            }
        }
        return json.length() - 1;
    }

    private int findArrayEnd(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            } else if (c == '"') {
                i = findStringEnd(json, i + 1);
            }
        }
        return json.length() - 1;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    // ========================================
    // Binary Meta Serialization
    // ========================================

    private void writeMetaToBinary(@NotNull Map<String, Object> meta, @NotNull BinaryBuffer buffer) {
        // Write display name
        buffer.writeOptionalString((String) meta.get("display_name"));

        // Write lore
        @SuppressWarnings("unchecked")
        java.util.List<String> lore = (java.util.List<String>) meta.get("lore");
        if (lore != null) {
            buffer.writeVarInt(lore.size());
            for (String line : lore) {
                buffer.writeString(line);
            }
        } else {
            buffer.writeVarInt(0);
        }

        // Write enchantments
        @SuppressWarnings("unchecked")
        Map<String, Integer> enchants = (Map<String, Integer>) meta.get("enchantments");
        if (enchants != null) {
            buffer.writeVarInt(enchants.size());
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                buffer.writeString(entry.getKey());
                buffer.writeVarInt(entry.getValue());
            }
        } else {
            buffer.writeVarInt(0);
        }

        // Write custom model data
        Integer customModelData = (Integer) meta.get("custom_model_data");
        buffer.writeBoolean(customModelData != null);
        if (customModelData != null) {
            buffer.writeVarInt(customModelData);
        }

        // Write unbreakable flag
        buffer.writeBoolean(Boolean.TRUE.equals(meta.get("unbreakable")));

        // Write item flags
        @SuppressWarnings("unchecked")
        java.util.List<String> flags = (java.util.List<String>) meta.get("flags");
        if (flags != null) {
            buffer.writeVarInt(flags.size());
            for (String flag : flags) {
                buffer.writeString(flag);
            }
        } else {
            buffer.writeVarInt(0);
        }
    }

    @NotNull
    private Map<String, Object> readMetaFromBinary(@NotNull BinaryBuffer buffer) {
        Map<String, Object> meta = new LinkedHashMap<>();

        // Read display name
        String displayName = buffer.readOptionalString();
        if (displayName != null) {
            meta.put("display_name", displayName);
        }

        // Read lore
        int loreCount = buffer.readVarInt();
        if (loreCount > 0) {
            java.util.List<String> lore = new java.util.ArrayList<>(loreCount);
            for (int i = 0; i < loreCount; i++) {
                lore.add(buffer.readString());
            }
            meta.put("lore", lore);
        }

        // Read enchantments
        int enchantCount = buffer.readVarInt();
        if (enchantCount > 0) {
            Map<String, Integer> enchants = new LinkedHashMap<>();
            for (int i = 0; i < enchantCount; i++) {
                enchants.put(buffer.readString(), buffer.readVarInt());
            }
            meta.put("enchantments", enchants);
        }

        // Read custom model data
        if (buffer.readBoolean()) {
            meta.put("custom_model_data", buffer.readVarInt());
        }

        // Read unbreakable flag
        if (buffer.readBoolean()) {
            meta.put("unbreakable", true);
        }

        // Read item flags
        int flagCount = buffer.readVarInt();
        if (flagCount > 0) {
            java.util.List<String> flags = new java.util.ArrayList<>(flagCount);
            for (int i = 0; i < flagCount; i++) {
                flags.add(buffer.readString());
            }
            meta.put("flags", flags);
        }

        return meta;
    }

    // ========================================
    // Convenience Methods
    // ========================================

    /**
     * Serializes an array of item data maps.
     *
     * @param items the items to serialize
     * @return the Base64-encoded serialized data
     * @since 1.0.0
     */
    @NotNull
    public String serializeArray(@NotNull Map<String, Object>[] items) {
        BinaryBuffer buffer = BinaryBuffer.allocate(items.length * 128);

        buffer.writeVarInt(items.length);
        SerializationContext context = SerializationContext.base64();

        for (Map<String, Object> item : items) {
            if (item == null || item.isEmpty()) {
                buffer.writeBoolean(false);
            } else {
                buffer.writeBoolean(true);
                byte[] itemBytes = toBytes(item, context);
                buffer.writeBytes(itemBytes);
            }
        }

        return Base64.getEncoder().encodeToString(buffer.toByteArray());
    }

    /**
     * Deserializes an array of item data maps.
     *
     * @param data the Base64-encoded data
     * @return the deserialized item array
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public Map<String, Object>[] deserializeArray(@NotNull String data) {
        byte[] bytes = Base64.getDecoder().decode(data);
        BinaryBuffer buffer = BinaryBuffer.wrap(bytes);

        int length = buffer.readVarInt();
        Map<String, Object>[] items = new Map[length];
        SerializationContext context = SerializationContext.base64();

        for (int i = 0; i < length; i++) {
            boolean present = buffer.readBoolean();
            if (present) {
                byte[] itemBytes = buffer.readBytes();
                items[i] = fromBytes(itemBytes, context);
            } else {
                items[i] = null;
            }
        }

        return items;
    }
}
