/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.serialization;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes and deserializes individual items.
 *
 * <p>ItemSerializer provides methods for converting UnifiedItemStack instances
 * to various serialized formats (bytes, Base64, JSON) and back. It handles
 * all item metadata including enchantments, lore, custom model data, and
 * persistent data.
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li><b>Binary</b>: Raw byte array, most compact</li>
 *   <li><b>Base64</b>: Text-safe encoding of binary data</li>
 *   <li><b>JSON</b>: Human-readable structured format</li>
 *   <li><b>Compressed</b>: GZIP-compressed binary</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private ItemSerializer itemSerializer;
 *
 * // Serialize to different formats
 * byte[] bytes = itemSerializer.toBytes(itemStack);
 * String base64 = itemSerializer.toBase64(itemStack);
 * String json = itemSerializer.toJson(itemStack);
 *
 * // Deserialize from different formats
 * ItemStack fromBytes = itemSerializer.fromBytes(bytes);
 * ItemStack fromBase64 = itemSerializer.fromBase64(base64);
 * ItemStack fromJson = itemSerializer.fromJson(json);
 *
 * // Batch operations
 * String encoded = itemSerializer.serializeArray(itemStacks);
 * ItemStack[] decoded = itemSerializer.deserializeArray(encoded);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>ItemSerializer instances are thread-safe and can be shared.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SerializationFormat
 * @see InventorySerializer
 */
public class ItemSerializer {

    private static final int MAGIC_NUMBER = 0x49544D53; // "ITMS"
    private static final int VERSION = 1;

    private static ItemSerializer instance;

    private final SerializationFormat defaultFormat;

    /**
     * Creates an ItemSerializer with the default format.
     *
     * @since 1.0.0
     */
    public ItemSerializer() {
        this(SerializationFormat.BASE64);
    }

    /**
     * Creates an ItemSerializer with a specific default format.
     *
     * @param defaultFormat the default serialization format
     * @since 1.0.0
     */
    public ItemSerializer(@NotNull SerializationFormat defaultFormat) {
        this.defaultFormat = Objects.requireNonNull(defaultFormat);
    }

    /**
     * Returns the singleton instance.
     *
     * @return the item serializer instance
     * @since 1.0.0
     */
    @NotNull
    public static ItemSerializer getInstance() {
        if (instance == null) {
            instance = new ItemSerializer();
        }
        return instance;
    }

    /**
     * Sets the singleton instance (for testing/customization).
     *
     * @param serializer the serializer instance
     * @since 1.0.0
     */
    public static void setInstance(@NotNull ItemSerializer serializer) {
        instance = serializer;
    }

    // ========== Single Item Serialization ==========

    /**
     * Serializes an item to bytes.
     *
     * @param item the item to serialize
     * @return the serialized bytes
     * @since 1.0.0
     */
    public byte @NotNull [] toBytes(@Nullable UnifiedItemStack item) {
        if (item == null || item.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(MAGIC_NUMBER);
            dos.writeInt(VERSION);

            // Use the item's native serialization
            byte[] itemData = item.serialize();
            dos.writeInt(itemData.length);
            dos.write(itemData);

            return baos.toByteArray();

        } catch (IOException e) {
            throw new SerializationException("Failed to serialize item", e);
        }
    }

    /**
     * Deserializes an item from bytes.
     *
     * @param bytes the serialized bytes
     * @return the deserialized item, or null if bytes are empty
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack fromBytes(byte @NotNull [] bytes) {
        if (bytes.length == 0) {
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {

            int magic = dis.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new SerializationException("Invalid item data magic number");
            }

            int version = dis.readInt();
            if (version != VERSION) {
                throw new SerializationException("Unsupported item data version: " + version);
            }

            int dataLength = dis.readInt();
            byte[] itemData = new byte[dataLength];
            dis.readFully(itemData);

            // Delegate to platform-specific deserialization
            return deserializeItemData(itemData);

        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize item", e);
        }
    }

    /**
     * Serializes an item to Base64.
     *
     * @param item the item to serialize
     * @return the Base64-encoded string
     * @since 1.0.0
     */
    @NotNull
    public String toBase64(@Nullable UnifiedItemStack item) {
        if (item == null || item.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(toBytes(item));
    }

    /**
     * Deserializes an item from Base64.
     *
     * @param base64 the Base64-encoded string
     * @return the deserialized item, or null if string is empty
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack fromBase64(@NotNull String base64) {
        if (base64.isEmpty()) {
            return null;
        }
        return fromBytes(Base64.getDecoder().decode(base64));
    }

    /**
     * Serializes an item to JSON.
     *
     * @param item the item to serialize
     * @return the JSON string
     * @since 1.0.0
     */
    @NotNull
    public String toJson(@Nullable UnifiedItemStack item) {
        if (item == null || item.isEmpty()) {
            return "null";
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"").append(escapeJson(item.getType())).append("\"");
        json.append(",\"amount\":").append(item.getAmount());

        if (item.hasDisplayName()) {
            // Simplified - would use Adventure serialization
            json.append(",\"displayName\":\"").append(escapeJson(item.getDisplayName().toString())).append("\"");
        }

        if (item.hasLore()) {
            json.append(",\"lore\":[");
            var lore = item.getLore();
            for (int i = 0; i < lore.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJson(lore.get(i).toString())).append("\"");
            }
            json.append("]");
        }

        if (item.hasEnchantments()) {
            json.append(",\"enchantments\":{");
            var enchants = item.getEnchantments();
            int count = 0;
            for (var entry : enchants.entrySet()) {
                if (count > 0) json.append(",");
                json.append("\"").append(escapeJson(entry.getKey())).append("\":").append(entry.getValue());
                count++;
            }
            json.append("}");
        }

        if (item.hasDurability() && item.getDamage() > 0) {
            json.append(",\"damage\":").append(item.getDamage());
        }

        if (item.isUnbreakable()) {
            json.append(",\"unbreakable\":true");
        }

        if (item.hasCustomModelData()) {
            json.append(",\"customModelData\":").append(item.getCustomModelData().get());
        }

        // Include raw data for full fidelity restoration
        json.append(",\"_data\":\"").append(toBase64(item)).append("\"");

        json.append("}");
        return json.toString();
    }

    /**
     * Deserializes an item from JSON.
     *
     * @param json the JSON string
     * @return the deserialized item, or null if "null"
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack fromJson(@NotNull String json) {
        if (json.equals("null") || json.isEmpty()) {
            return null;
        }

        // Look for _data field for full fidelity restoration
        int dataIndex = json.indexOf("\"_data\":\"");
        if (dataIndex != -1) {
            int start = dataIndex + 9;
            int end = json.indexOf("\"", start);
            if (end != -1) {
                String base64 = json.substring(start, end);
                return fromBase64(base64);
            }
        }

        // Fallback: parse JSON structure (simplified implementation)
        throw new SerializationException("JSON parsing without _data field not implemented");
    }

    // ========== Compressed Serialization ==========

    /**
     * Serializes an item to compressed bytes.
     *
     * @param item the item to serialize
     * @return the compressed bytes
     * @since 1.0.0
     */
    public byte @NotNull [] toCompressed(@Nullable UnifiedItemStack item) {
        byte[] uncompressed = toBytes(item);
        if (uncompressed.length == 0) {
            return uncompressed;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {

            gzos.write(uncompressed);
            gzos.finish();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new SerializationException("Failed to compress item data", e);
        }
    }

    /**
     * Deserializes an item from compressed bytes.
     *
     * @param compressed the compressed bytes
     * @return the deserialized item
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack fromCompressed(byte @NotNull [] compressed) {
        if (compressed.length == 0) {
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            return fromBytes(baos.toByteArray());

        } catch (IOException e) {
            throw new SerializationException("Failed to decompress item data", e);
        }
    }

    // ========== Array Serialization ==========

    /**
     * Serializes an array of items.
     *
     * @param items the items to serialize
     * @return the Base64-encoded string
     * @since 1.0.0
     */
    @NotNull
    public String serializeArray(@Nullable UnifiedItemStack @NotNull [] items) {
        return serializeArray(items, defaultFormat);
    }

    /**
     * Serializes an array of items with a specific format.
     *
     * @param items  the items to serialize
     * @param format the serialization format
     * @return the serialized string
     * @since 1.0.0
     */
    @NotNull
    public String serializeArray(@Nullable UnifiedItemStack @NotNull [] items, @NotNull SerializationFormat format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(MAGIC_NUMBER);
            dos.writeInt(VERSION);
            dos.writeInt(items.length);

            for (UnifiedItemStack item : items) {
                byte[] itemBytes = item != null ? toBytes(item) : new byte[0];
                dos.writeInt(itemBytes.length);
                if (itemBytes.length > 0) {
                    dos.write(itemBytes);
                }
            }

            byte[] data = baos.toByteArray();

            return switch (format) {
                case BASE64, JSON -> Base64.getEncoder().encodeToString(data);
                case BINARY -> new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
                case COMPRESSED -> Base64.getEncoder().encodeToString(compress(data));
                case NBT -> throw new UnsupportedOperationException("NBT format not implemented");
            };

        } catch (IOException e) {
            throw new SerializationException("Failed to serialize item array", e);
        }
    }

    /**
     * Deserializes an array of items.
     *
     * @param encoded the encoded string
     * @return the deserialized items
     * @since 1.0.0
     */
    @NotNull
    public UnifiedItemStack @NotNull [] deserializeArray(@NotNull String encoded) {
        return deserializeArray(encoded, defaultFormat);
    }

    /**
     * Deserializes an array of items with a specific format.
     *
     * @param encoded the encoded string
     * @param format  the serialization format
     * @return the deserialized items
     * @since 1.0.0
     */
    @NotNull
    public UnifiedItemStack @NotNull [] deserializeArray(@NotNull String encoded, @NotNull SerializationFormat format) {
        if (encoded.isEmpty()) {
            return new UnifiedItemStack[0];
        }

        byte[] data = switch (format) {
            case BASE64, JSON -> Base64.getDecoder().decode(encoded);
            case BINARY -> encoded.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            case COMPRESSED -> decompress(Base64.getDecoder().decode(encoded));
            case NBT -> throw new UnsupportedOperationException("NBT format not implemented");
        };

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            int magic = dis.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new SerializationException("Invalid item array data");
            }

            int version = dis.readInt();
            int length = dis.readInt();

            UnifiedItemStack[] items = new UnifiedItemStack[length];
            for (int i = 0; i < length; i++) {
                int itemLength = dis.readInt();
                if (itemLength > 0) {
                    byte[] itemBytes = new byte[itemLength];
                    dis.readFully(itemBytes);
                    items[i] = fromBytes(itemBytes);
                }
            }

            return items;

        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize item array", e);
        }
    }

    // ========== Utility Methods ==========

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();

        } catch (IOException e) {
            throw new SerializationException("Failed to decompress", e);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Platform-specific item deserialization.
     * This method should be overridden by platform implementations.
     */
    @Nullable
    protected UnifiedItemStack deserializeItemData(byte @NotNull [] data) {
        // Platform-specific implementation would go here
        // For now, throw to indicate this needs platform implementation
        throw new UnsupportedOperationException(
            "Platform-specific item deserialization not implemented");
    }

    /**
     * Returns the default serialization format.
     *
     * @return the default format
     * @since 1.0.0
     */
    @NotNull
    public SerializationFormat getDefaultFormat() {
        return defaultFormat;
    }

    /**
     * Exception thrown when serialization/deserialization fails.
     *
     * @since 1.0.0
     */
    public static class SerializationException extends RuntimeException {
        public SerializationException(String message) {
            super(message);
        }

        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
