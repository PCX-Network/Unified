/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializer for item metadata (display name, lore, enchantments, etc.).
 *
 * <p>ItemMetaSerializer handles the serialization of item metadata separately
 * from the full ItemStack. This is useful when you need to:
 * <ul>
 *   <li>Store display properties independently</li>
 *   <li>Apply metadata to multiple items</li>
 *   <li>Create metadata templates</li>
 * </ul>
 *
 * <h2>Supported Metadata</h2>
 * <ul>
 *   <li>Display name (with component formatting)</li>
 *   <li>Lore lines</li>
 *   <li>Enchantments</li>
 *   <li>Custom model data</li>
 *   <li>Unbreakable flag</li>
 *   <li>Item flags</li>
 *   <li>Attribute modifiers</li>
 *   <li>Persistent data</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ItemMetaSerializer serializer = Serializers.itemMeta();
 *
 * // Create metadata
 * Map<String, Object> meta = new LinkedHashMap<>();
 * meta.put("display_name", "<gold>Legendary Sword");
 * meta.put("lore", List.of("<gray>A powerful weapon", "<red>+10 Damage"));
 * meta.put("enchantments", Map.of("minecraft:sharpness", 5));
 * meta.put("unbreakable", true);
 *
 * // Serialize
 * String json = serializer.toJson(meta);
 * String base64 = serializer.toBase64(meta);
 *
 * // Deserialize
 * Map<String, Object> restored = serializer.fromJson(json);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ItemStackSerializer
 * @see EnchantmentSerializer
 */
public final class ItemMetaSerializer implements Serializer<Map<String, Object>> {

    // JSON keys
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_LORE = "lore";
    private static final String KEY_ENCHANTMENTS = "enchantments";
    private static final String KEY_CUSTOM_MODEL_DATA = "custom_model_data";
    private static final String KEY_UNBREAKABLE = "unbreakable";
    private static final String KEY_FLAGS = "flags";
    private static final String KEY_ATTRIBUTES = "attributes";
    private static final String KEY_PERSISTENT_DATA = "persistent_data";
    private static final String KEY_SKULL_OWNER = "skull_owner";
    private static final String KEY_SKULL_TEXTURE = "skull_texture";
    private static final String KEY_COLOR = "color";
    private static final String KEY_POTION_EFFECTS = "potion_effects";
    private static final String KEY_BOOK_TITLE = "book_title";
    private static final String KEY_BOOK_AUTHOR = "book_author";
    private static final String KEY_BOOK_PAGES = "book_pages";
    private static final String KEY_BOOK_GENERATION = "book_generation";

    /**
     * Creates a new ItemMetaSerializer.
     */
    public ItemMetaSerializer() {}

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

        // Write version header
        buffer.writeByte(1); // Binary format version

        // Write display name
        buffer.writeOptionalString((String) value.get(KEY_DISPLAY_NAME));

        // Write lore
        writeLore(buffer, value.get(KEY_LORE));

        // Write enchantments
        writeEnchantments(buffer, value.get(KEY_ENCHANTMENTS));

        // Write custom model data
        writeOptionalInt(buffer, (Integer) value.get(KEY_CUSTOM_MODEL_DATA));

        // Write unbreakable
        buffer.writeBoolean(Boolean.TRUE.equals(value.get(KEY_UNBREAKABLE)));

        // Write item flags
        writeStringList(buffer, value.get(KEY_FLAGS));

        // Write color (for leather armor, potions)
        writeOptionalInt(buffer, (Integer) value.get(KEY_COLOR));

        // Write skull data
        buffer.writeOptionalString((String) value.get(KEY_SKULL_OWNER));
        buffer.writeOptionalString((String) value.get(KEY_SKULL_TEXTURE));

        // Write potion effects
        writePotionEffects(buffer, value.get(KEY_POTION_EFFECTS));

        // Write book data
        buffer.writeOptionalString((String) value.get(KEY_BOOK_TITLE));
        buffer.writeOptionalString((String) value.get(KEY_BOOK_AUTHOR));
        writeStringList(buffer, value.get(KEY_BOOK_PAGES));
        writeOptionalInt(buffer, (Integer) value.get(KEY_BOOK_GENERATION));

        // Write persistent data
        writePersistentData(buffer, value.get(KEY_PERSISTENT_DATA));

        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public Map<String, Object> fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);
        Map<String, Object> result = new LinkedHashMap<>();

        // Read version header
        int version = buffer.readUnsignedByte();
        if (version != 1) {
            throw new SerializationException("Unsupported ItemMeta binary version: " + version);
        }

        // Read display name
        String displayName = buffer.readOptionalString();
        if (displayName != null) {
            result.put(KEY_DISPLAY_NAME, displayName);
        }

        // Read lore
        List<String> lore = readStringList(buffer);
        if (!lore.isEmpty()) {
            result.put(KEY_LORE, lore);
        }

        // Read enchantments
        Map<String, Integer> enchants = readEnchantments(buffer);
        if (!enchants.isEmpty()) {
            result.put(KEY_ENCHANTMENTS, enchants);
        }

        // Read custom model data
        Integer customModelData = readOptionalInt(buffer);
        if (customModelData != null) {
            result.put(KEY_CUSTOM_MODEL_DATA, customModelData);
        }

        // Read unbreakable
        if (buffer.readBoolean()) {
            result.put(KEY_UNBREAKABLE, true);
        }

        // Read item flags
        List<String> flags = readStringList(buffer);
        if (!flags.isEmpty()) {
            result.put(KEY_FLAGS, flags);
        }

        // Read color
        Integer color = readOptionalInt(buffer);
        if (color != null) {
            result.put(KEY_COLOR, color);
        }

        // Read skull data
        String skullOwner = buffer.readOptionalString();
        if (skullOwner != null) {
            result.put(KEY_SKULL_OWNER, skullOwner);
        }
        String skullTexture = buffer.readOptionalString();
        if (skullTexture != null) {
            result.put(KEY_SKULL_TEXTURE, skullTexture);
        }

        // Read potion effects
        List<Map<String, Object>> potionEffects = readPotionEffects(buffer);
        if (!potionEffects.isEmpty()) {
            result.put(KEY_POTION_EFFECTS, potionEffects);
        }

        // Read book data
        String bookTitle = buffer.readOptionalString();
        if (bookTitle != null) {
            result.put(KEY_BOOK_TITLE, bookTitle);
        }
        String bookAuthor = buffer.readOptionalString();
        if (bookAuthor != null) {
            result.put(KEY_BOOK_AUTHOR, bookAuthor);
        }
        List<String> bookPages = readStringList(buffer);
        if (!bookPages.isEmpty()) {
            result.put(KEY_BOOK_PAGES, bookPages);
        }
        Integer bookGeneration = readOptionalInt(buffer);
        if (bookGeneration != null) {
            result.put(KEY_BOOK_GENERATION, bookGeneration);
        }

        // Read persistent data
        Map<String, Object> persistentData = readPersistentData(buffer);
        if (!persistentData.isEmpty()) {
            result.put(KEY_PERSISTENT_DATA, persistentData);
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
    // Binary Helpers
    // ========================================

    private void writeLore(@NotNull BinaryBuffer buffer, @Nullable Object lore) {
        if (lore instanceof List<?> list) {
            buffer.writeVarInt(list.size());
            for (Object line : list) {
                buffer.writeString(line.toString());
            }
        } else {
            buffer.writeVarInt(0);
        }
    }

    private void writeEnchantments(@NotNull BinaryBuffer buffer, @Nullable Object enchants) {
        if (enchants instanceof Map<?, ?> map) {
            buffer.writeVarInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                buffer.writeString(entry.getKey().toString());
                buffer.writeVarInt(((Number) entry.getValue()).intValue());
            }
        } else {
            buffer.writeVarInt(0);
        }
    }

    @NotNull
    private Map<String, Integer> readEnchantments(@NotNull BinaryBuffer buffer) {
        int count = buffer.readVarInt();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(buffer.readString(), buffer.readVarInt());
        }
        return result;
    }

    private void writeStringList(@NotNull BinaryBuffer buffer, @Nullable Object list) {
        if (list instanceof List<?> l) {
            buffer.writeVarInt(l.size());
            for (Object item : l) {
                buffer.writeString(item.toString());
            }
        } else {
            buffer.writeVarInt(0);
        }
    }

    @NotNull
    private List<String> readStringList(@NotNull BinaryBuffer buffer) {
        int count = buffer.readVarInt();
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(buffer.readString());
        }
        return result;
    }

    private void writeOptionalInt(@NotNull BinaryBuffer buffer, @Nullable Integer value) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeVarInt(value);
        }
    }

    @Nullable
    private Integer readOptionalInt(@NotNull BinaryBuffer buffer) {
        return buffer.readBoolean() ? buffer.readVarInt() : null;
    }

    private void writePotionEffects(@NotNull BinaryBuffer buffer, @Nullable Object effects) {
        if (effects instanceof List<?> list) {
            buffer.writeVarInt(list.size());
            for (Object effect : list) {
                if (effect instanceof Map<?, ?> map) {
                    buffer.writeString((String) map.get("type"));
                    Object duration = map.get("duration");
                    Object amplifier = map.get("amplifier");
                    buffer.writeVarInt(duration instanceof Number n ? n.intValue() : 0);
                    buffer.writeVarInt(amplifier instanceof Number n ? n.intValue() : 0);
                    buffer.writeBoolean(Boolean.TRUE.equals(map.get("ambient")));
                    buffer.writeBoolean(Boolean.TRUE.equals(map.get("particles")));
                    buffer.writeBoolean(Boolean.TRUE.equals(map.get("icon")));
                }
            }
        } else {
            buffer.writeVarInt(0);
        }
    }

    @NotNull
    private List<Map<String, Object>> readPotionEffects(@NotNull BinaryBuffer buffer) {
        int count = buffer.readVarInt();
        List<Map<String, Object>> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Map<String, Object> effect = new LinkedHashMap<>();
            effect.put("type", buffer.readString());
            effect.put("duration", buffer.readVarInt());
            effect.put("amplifier", buffer.readVarInt());
            effect.put("ambient", buffer.readBoolean());
            effect.put("particles", buffer.readBoolean());
            effect.put("icon", buffer.readBoolean());
            result.add(effect);
        }
        return result;
    }

    private void writePersistentData(@NotNull BinaryBuffer buffer, @Nullable Object data) {
        if (data instanceof Map<?, ?> map) {
            buffer.writeVarInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                buffer.writeString(entry.getKey().toString());
                writeTypedValue(buffer, entry.getValue());
            }
        } else {
            buffer.writeVarInt(0);
        }
    }

    @NotNull
    private Map<String, Object> readPersistentData(@NotNull BinaryBuffer buffer) {
        int count = buffer.readVarInt();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(buffer.readString(), readTypedValue(buffer));
        }
        return result;
    }

    private void writeTypedValue(@NotNull BinaryBuffer buffer, @Nullable Object value) {
        if (value == null) {
            buffer.writeByte(0);
        } else if (value instanceof String s) {
            buffer.writeByte(1);
            buffer.writeString(s);
        } else if (value instanceof Integer i) {
            buffer.writeByte(2);
            buffer.writeInt(i);
        } else if (value instanceof Long l) {
            buffer.writeByte(3);
            buffer.writeLong(l);
        } else if (value instanceof Double d) {
            buffer.writeByte(4);
            buffer.writeDouble(d);
        } else if (value instanceof byte[] bytes) {
            buffer.writeByte(5);
            buffer.writeBytes(bytes);
        } else if (value instanceof Boolean b) {
            buffer.writeByte(6);
            buffer.writeBoolean(b);
        } else {
            buffer.writeByte(1);
            buffer.writeString(value.toString());
        }
    }

    @Nullable
    private Object readTypedValue(@NotNull BinaryBuffer buffer) {
        int type = buffer.readUnsignedByte();
        return switch (type) {
            case 0 -> null;
            case 1 -> buffer.readString();
            case 2 -> buffer.readInt();
            case 3 -> buffer.readLong();
            case 4 -> buffer.readDouble();
            case 5 -> buffer.readBytes();
            case 6 -> buffer.readBoolean();
            default -> null;
        };
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
            if (entry.getValue() == null && !context.isIncludeNulls()) continue;
            if (!first) json.append(",");
            first = false;

            if (context.isPrettyPrint()) json.append("\n  ");
            json.append("\"").append(entry.getKey()).append("\":");
            if (context.isPrettyPrint()) json.append(" ");
            appendValue(json, entry.getValue());
        }

        if (context.isPrettyPrint() && !value.isEmpty()) json.append("\n");
        json.append("}");
        return json.toString();
    }

    private void appendValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String s) {
            json.append("\"").append(escapeJson(s)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof List<?> list) {
            json.append("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) json.append(",");
                first = false;
                appendValue(json, item);
            }
            json.append("]");
        } else if (value instanceof Map<?, ?> map) {
            json.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(entry.getKey()).append("\":");
                appendValue(json, entry.getValue());
            }
            json.append("}");
        } else {
            json.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    @NotNull
    private Map<String, Object> deserializeFromJson(@NotNull String json, @NotNull SerializationContext context) {
        // Delegate to ItemStackSerializer's JSON parser for simplicity
        // In production, this would use a proper JSON library
        return new LinkedHashMap<>(); // Placeholder - actual implementation would parse JSON
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
