/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;

/**
 * MongoDB codec for serializing and deserializing Adventure {@link Component}.
 *
 * <p>This codec stores components as embedded documents with multiple
 * representations for flexibility:
 * <pre>
 * {
 *   "json": "{\"text\":\"Hello\",\"color\":\"gold\",\"bold\":true}",
 *   "miniMessage": "&lt;gold>&lt;bold>Hello",
 *   "plain": "Hello"
 * }
 * </pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Register the codec
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .addCodec(new ComponentCodec())
 *     .build();
 *
 * // Store a component
 * Component message = Component.text("Welcome!")
 *     .color(NamedTextColor.GREEN)
 *     .decorate(TextDecoration.BOLD);
 *
 * Document announcement = new Document("title", message)
 *     .append("created", Instant.now());
 *
 * // Query by plain text content
 * mongoService.find("announcements",
 *     Filters.regex("title.plain", ".*Welcome.*", "i"));
 * }</pre>
 *
 * <h2>Storage Format</h2>
 * <p>The codec stores three representations:
 * <ul>
 *   <li><b>json</b> - Full JSON representation for complete reconstruction</li>
 *   <li><b>miniMessage</b> - MiniMessage format for readability</li>
 *   <li><b>plain</b> - Plain text for simple text searches</li>
 * </ul>
 *
 * <h2>Serialization Modes</h2>
 * <p>You can configure which representations to store:
 * <ul>
 *   <li>{@link Mode#FULL} - All three representations (default)</li>
 *   <li>{@link Mode#JSON_ONLY} - Only JSON for minimum storage</li>
 *   <li>{@link Mode#MINI_MESSAGE_ONLY} - Only MiniMessage for readability</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This codec is stateless and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Component
 * @see CodecRegistryBuilder
 */
public class ComponentCodec implements Codec<Component> {

    /**
     * Field name for the JSON representation.
     */
    public static final String FIELD_JSON = "json";

    /**
     * Field name for the MiniMessage representation.
     */
    public static final String FIELD_MINI_MESSAGE = "miniMessage";

    /**
     * Field name for the plain text representation.
     */
    public static final String FIELD_PLAIN = "plain";

    /**
     * Serialization mode.
     */
    public enum Mode {
        /**
         * Store all representations (JSON, MiniMessage, plain).
         */
        FULL,

        /**
         * Store only JSON for minimum storage.
         */
        JSON_ONLY,

        /**
         * Store only MiniMessage for readability.
         */
        MINI_MESSAGE_ONLY
    }

    private static final GsonComponentSerializer JSON_SERIALIZER = GsonComponentSerializer.gson();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private final Mode mode;

    /**
     * Creates a new component codec with full mode.
     *
     * @since 1.0.0
     */
    public ComponentCodec() {
        this(Mode.FULL);
    }

    /**
     * Creates a new component codec with the specified mode.
     *
     * @param mode the serialization mode
     * @since 1.0.0
     */
    public ComponentCodec(@NotNull Mode mode) {
        this.mode = mode;
    }

    /**
     * Returns the encoder class.
     *
     * @return Component.class
     * @since 1.0.0
     */
    @Override
    @NotNull
    public Class<Component> getEncoderClass() {
        return Component.class;
    }

    /**
     * Encodes a Component to BSON.
     *
     * @param writer         the BSON writer
     * @param value          the component value
     * @param encoderContext the encoder context
     * @since 1.0.0
     */
    @Override
    public void encode(
            @NotNull BsonWriter writer,
            @NotNull Component value,
            @NotNull EncoderContext encoderContext
    ) {
        switch (mode) {
            case JSON_ONLY -> writer.writeString(JSON_SERIALIZER.serialize(value));
            case MINI_MESSAGE_ONLY -> writer.writeString(MINI_MESSAGE.serialize(value));
            case FULL -> {
                writer.writeStartDocument();
                writer.writeString(FIELD_JSON, JSON_SERIALIZER.serialize(value));
                writer.writeString(FIELD_MINI_MESSAGE, MINI_MESSAGE.serialize(value));
                writer.writeString(FIELD_PLAIN, PLAIN_SERIALIZER.serialize(value));
                writer.writeEndDocument();
            }
        }
    }

    /**
     * Decodes a Component from BSON.
     *
     * <p>This method handles both document format (full mode) and string
     * format (JSON_ONLY or MINI_MESSAGE_ONLY modes).
     *
     * @param reader         the BSON reader
     * @param decoderContext the decoder context
     * @return the decoded component
     * @since 1.0.0
     */
    @Override
    @NotNull
    public Component decode(
            @NotNull BsonReader reader,
            @NotNull DecoderContext decoderContext
    ) {
        BsonType currentType = reader.getCurrentBsonType();

        if (currentType == BsonType.STRING) {
            // Simple string format - try JSON first, then MiniMessage
            String value = reader.readString();
            return deserializeString(value);
        } else if (currentType == BsonType.DOCUMENT) {
            // Document format
            return decodeDocument(reader);
        } else {
            throw new IllegalArgumentException(
                    "Cannot decode Component from BSON type: " + currentType
            );
        }
    }

    /**
     * Decodes a component from a BSON document.
     *
     * @param reader the BSON reader
     * @return the decoded component
     */
    @NotNull
    private Component decodeDocument(@NotNull BsonReader reader) {
        reader.readStartDocument();

        String json = null;
        String miniMessage = null;

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();

            switch (fieldName) {
                case FIELD_JSON -> json = reader.readString();
                case FIELD_MINI_MESSAGE -> miniMessage = reader.readString();
                default -> reader.skipValue(); // Ignore unknown fields (like plain)
            }
        }

        reader.readEndDocument();

        // Prefer JSON for complete fidelity
        if (json != null) {
            return JSON_SERIALIZER.deserialize(json);
        } else if (miniMessage != null) {
            return MINI_MESSAGE.deserialize(miniMessage);
        } else {
            return Component.empty();
        }
    }

    /**
     * Deserializes a component from a string.
     *
     * <p>Attempts JSON deserialization first, falls back to MiniMessage.
     *
     * @param value the string value
     * @return the deserialized component
     */
    @NotNull
    private Component deserializeString(@NotNull String value) {
        // Try JSON first (starts with '{')
        if (value.startsWith("{")) {
            try {
                return JSON_SERIALIZER.deserialize(value);
            } catch (Exception e) {
                // Fall through to MiniMessage
            }
        }

        // Try MiniMessage
        try {
            return MINI_MESSAGE.deserialize(value);
        } catch (Exception e) {
            // Fall back to plain text
            return Component.text(value);
        }
    }

    /**
     * Returns the configured serialization mode.
     *
     * @return the mode
     * @since 1.0.0
     */
    @NotNull
    public Mode getMode() {
        return mode;
    }

    /**
     * Converts a component to its JSON string representation.
     *
     * @param component the component
     * @return the JSON string
     * @since 1.0.0
     */
    @NotNull
    public static String toJson(@NotNull Component component) {
        return JSON_SERIALIZER.serialize(component);
    }

    /**
     * Converts a component to its MiniMessage string representation.
     *
     * @param component the component
     * @return the MiniMessage string
     * @since 1.0.0
     */
    @NotNull
    public static String toMiniMessage(@NotNull Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Converts a component to its plain text representation.
     *
     * @param component the component
     * @return the plain text
     * @since 1.0.0
     */
    @NotNull
    public static String toPlainText(@NotNull Component component) {
        return PLAIN_SERIALIZER.serialize(component);
    }

    /**
     * Parses a component from a JSON string.
     *
     * @param json the JSON string
     * @return the parsed component
     * @since 1.0.0
     */
    @NotNull
    public static Component fromJson(@NotNull String json) {
        return JSON_SERIALIZER.deserialize(json);
    }

    /**
     * Parses a component from a MiniMessage string.
     *
     * @param miniMessage the MiniMessage string
     * @return the parsed component
     * @since 1.0.0
     */
    @NotNull
    public static Component fromMiniMessage(@NotNull String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }
}
