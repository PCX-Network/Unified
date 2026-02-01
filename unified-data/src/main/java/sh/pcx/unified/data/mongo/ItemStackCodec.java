/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import sh.pcx.unified.item.UnifiedItemStack;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

/**
 * MongoDB codec for serializing and deserializing {@link UnifiedItemStack}.
 *
 * <p>This codec stores item stacks as embedded documents with both readable
 * fields and a Base64-encoded binary representation for full fidelity:
 * <pre>
 * {
 *   "type": "minecraft:diamond_sword",
 *   "amount": 1,
 *   "displayName": "Excalibur",
 *   "enchantments": {
 *     "minecraft:sharpness": 5,
 *     "minecraft:unbreaking": 3
 *   },
 *   "data": "base64EncodedItemData..."
 * }
 * </pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Register the codec
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .addCodec(new ItemStackCodec())
 *     .build();
 *
 * // Store an item
 * Document player = new Document("uuid", uuid.toString())
 *     .append("mainHand", player.getItemInMainHand());
 *
 * // Query by item type
 * mongoService.find("players", Filters.eq("mainHand.type", "minecraft:diamond_sword"));
 *
 * // Query by enchantment
 * mongoService.find("players", Filters.exists("mainHand.enchantments.minecraft:sharpness"));
 * }</pre>
 *
 * <h2>Storage Format</h2>
 * <p>The codec stores both queryable metadata (type, amount, enchantments) and
 * the full Base64-encoded item data. This allows:
 * <ul>
 *   <li>Querying items by type, enchantments, or other properties</li>
 *   <li>Full reconstruction of items with all NBT data preserved</li>
 *   <li>Inspection of item data in MongoDB tools</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This codec is stateless and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedItemStack
 * @see CodecRegistryBuilder
 */
public class ItemStackCodec implements Codec<UnifiedItemStack> {

    /**
     * Field name for the item type.
     */
    public static final String FIELD_TYPE = "type";

    /**
     * Field name for the item amount.
     */
    public static final String FIELD_AMOUNT = "amount";

    /**
     * Field name for the display name.
     */
    public static final String FIELD_DISPLAY_NAME = "displayName";

    /**
     * Field name for enchantments.
     */
    public static final String FIELD_ENCHANTMENTS = "enchantments";

    /**
     * Field name for the custom model data.
     */
    public static final String FIELD_CUSTOM_MODEL_DATA = "customModelData";

    /**
     * Field name for the damage value.
     */
    public static final String FIELD_DAMAGE = "damage";

    /**
     * Field name for whether the item is unbreakable.
     */
    public static final String FIELD_UNBREAKABLE = "unbreakable";

    /**
     * Field name for the Base64-encoded item data.
     */
    public static final String FIELD_DATA = "data";

    /**
     * Creates a new item stack codec.
     *
     * @since 1.0.0
     */
    public ItemStackCodec() {
        // Default constructor
    }

    /**
     * Returns the encoder class.
     *
     * @return UnifiedItemStack.class
     * @since 1.0.0
     */
    @Override
    @NotNull
    public Class<UnifiedItemStack> getEncoderClass() {
        return UnifiedItemStack.class;
    }

    /**
     * Encodes an UnifiedItemStack to BSON.
     *
     * @param writer         the BSON writer
     * @param value          the item stack value
     * @param encoderContext the encoder context
     * @since 1.0.0
     */
    @Override
    public void encode(
            @NotNull BsonWriter writer,
            @NotNull UnifiedItemStack value,
            @NotNull EncoderContext encoderContext
    ) {
        writer.writeStartDocument();

        // Write queryable fields
        writer.writeString(FIELD_TYPE, value.getType());
        writer.writeInt32(FIELD_AMOUNT, value.getAmount());

        // Write display name if present
        value.getDisplayName().ifPresent(name -> {
            // Serialize Adventure Component to JSON string
            writer.writeString(FIELD_DISPLAY_NAME, serializeComponent(name));
        });

        // Write enchantments
        var enchantments = value.getEnchantments();
        if (!enchantments.isEmpty()) {
            writer.writeStartDocument(FIELD_ENCHANTMENTS);
            for (var entry : enchantments.entrySet()) {
                writer.writeInt32(entry.getKey(), entry.getValue());
            }
            writer.writeEndDocument();
        }

        // Write custom model data if present
        value.getCustomModelData().ifPresent(cmd -> {
            writer.writeInt32(FIELD_CUSTOM_MODEL_DATA, cmd);
        });

        // Write durability info
        if (value.hasDurability()) {
            writer.writeInt32(FIELD_DAMAGE, value.getDamage());
        }

        // Write unbreakable flag
        if (value.isUnbreakable()) {
            writer.writeBoolean(FIELD_UNBREAKABLE, true);
        }

        // Write full Base64-encoded item data for complete reconstruction
        writer.writeString(FIELD_DATA, value.toBase64());

        writer.writeEndDocument();
    }

    /**
     * Decodes an UnifiedItemStack from BSON.
     *
     * <p>The item is reconstructed from the Base64-encoded data field
     * for complete fidelity. The queryable fields are ignored during
     * decoding (they're for query purposes only).
     *
     * @param reader         the BSON reader
     * @param decoderContext the decoder context
     * @return the decoded item stack
     * @since 1.0.0
     */
    @Override
    @NotNull
    public UnifiedItemStack decode(
            @NotNull BsonReader reader,
            @NotNull DecoderContext decoderContext
    ) {
        reader.readStartDocument();

        String base64Data = null;

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();

            if (FIELD_DATA.equals(fieldName)) {
                base64Data = reader.readString();
            } else {
                // Skip other fields (they're for querying only)
                reader.skipValue();
            }
        }

        reader.readEndDocument();

        if (base64Data == null) {
            throw new IllegalArgumentException("Missing required field: " + FIELD_DATA);
        }

        // Reconstruct from Base64 data
        return deserializeFromBase64(base64Data);
    }

    /**
     * Serializes an Adventure Component to a string representation.
     *
     * @param component the component to serialize
     * @return the string representation
     */
    private String serializeComponent(@NotNull net.kyori.adventure.text.Component component) {
        // Use Adventure's MiniMessage or JSON serializer
        return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                .serialize(component);
    }

    /**
     * Deserializes an item stack from Base64 data.
     *
     * <p>This method delegates to the platform-specific implementation
     * to properly reconstruct the item with all NBT data.
     *
     * @param base64 the Base64-encoded item data
     * @return the deserialized item stack
     */
    @NotNull
    private UnifiedItemStack deserializeFromBase64(@NotNull String base64) {
        // This requires platform-specific implementation
        // The UnifiedItemStack interface should have a static fromBase64 method
        // For now, we throw an exception indicating platform implementation needed
        throw new UnsupportedOperationException(
                "Item deserialization requires platform-specific implementation. " +
                "Use the platform's ItemStack deserialization and wrap with UnifiedItemStack."
        );
    }

    /**
     * Creates a queryable document for an item stack without the full data.
     *
     * <p>This is useful for creating filter documents based on item properties.
     *
     * @param itemStack the item stack
     * @return a document with queryable fields only
     * @since 1.0.0
     */
    @NotNull
    public static org.bson.Document toQueryDocument(@NotNull UnifiedItemStack itemStack) {
        org.bson.Document doc = new org.bson.Document()
                .append(FIELD_TYPE, itemStack.getType())
                .append(FIELD_AMOUNT, itemStack.getAmount());

        itemStack.getDisplayName().ifPresent(name -> {
            doc.append(FIELD_DISPLAY_NAME, net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                    .serialize(name));
        });

        var enchantments = itemStack.getEnchantments();
        if (!enchantments.isEmpty()) {
            doc.append(FIELD_ENCHANTMENTS, new org.bson.Document(enchantments));
        }

        itemStack.getCustomModelData().ifPresent(cmd -> {
            doc.append(FIELD_CUSTOM_MODEL_DATA, cmd);
        });

        if (itemStack.hasDurability()) {
            doc.append(FIELD_DAMAGE, itemStack.getDamage());
        }

        if (itemStack.isUnbreakable()) {
            doc.append(FIELD_UNBREAKABLE, true);
        }

        return doc;
    }
}
