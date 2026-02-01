/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * MongoDB codec for serializing and deserializing Java UUIDs.
 *
 * <p>This codec supports multiple UUID representations in MongoDB:
 * <ul>
 *   <li>String representation (default) - e.g., "550e8400-e29b-41d4-a716-446655440000"</li>
 *   <li>Binary representation (BSON subtype 4) - more efficient storage</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Register the codec
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .addCodec(new UUIDCodec())
 *     .build();
 *
 * // Use in a document
 * Document player = new Document("_id", UUID.randomUUID())
 *     .append("name", "Steve");
 *
 * // The UUID will be stored as a string:
 * // { "_id": "550e8400-e29b-41d4-a716-446655440000", "name": "Steve" }
 * }</pre>
 *
 * <h2>Storage Format</h2>
 * <p>By default, this codec stores UUIDs as strings for readability and
 * compatibility. This makes debugging easier and allows querying with
 * string comparison in MongoDB Compass or the shell.
 *
 * <h2>Thread Safety</h2>
 * <p>This codec is stateless and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CodecRegistryBuilder
 */
public class UUIDCodec implements Codec<UUID> {

    /**
     * Representation mode for UUID storage.
     */
    public enum UUIDRepresentation {
        /**
         * Store as a string (default).
         */
        STRING,

        /**
         * Store as BSON binary (subtype 4).
         */
        BINARY
    }

    private final UUIDRepresentation representation;

    /**
     * Creates a new UUID codec with string representation.
     *
     * @since 1.0.0
     */
    public UUIDCodec() {
        this(UUIDRepresentation.STRING);
    }

    /**
     * Creates a new UUID codec with the specified representation.
     *
     * @param representation the UUID representation mode
     * @since 1.0.0
     */
    public UUIDCodec(@NotNull UUIDRepresentation representation) {
        this.representation = representation;
    }

    /**
     * Returns the encoder class.
     *
     * @return UUID.class
     * @since 1.0.0
     */
    @Override
    @NotNull
    public Class<UUID> getEncoderClass() {
        return UUID.class;
    }

    /**
     * Encodes a UUID to BSON.
     *
     * @param writer         the BSON writer
     * @param value          the UUID value
     * @param encoderContext the encoder context
     * @since 1.0.0
     */
    @Override
    public void encode(
            @NotNull BsonWriter writer,
            @NotNull UUID value,
            @NotNull EncoderContext encoderContext
    ) {
        if (representation == UUIDRepresentation.STRING) {
            writer.writeString(value.toString());
        } else {
            // Binary representation (subtype 4)
            byte[] bytes = uuidToBytes(value);
            writer.writeBinaryData(new org.bson.BsonBinary(org.bson.BsonBinarySubType.UUID_STANDARD, bytes));
        }
    }

    /**
     * Decodes a UUID from BSON.
     *
     * <p>This method handles both string and binary representations,
     * regardless of the codec's configured representation mode.
     *
     * @param reader         the BSON reader
     * @param decoderContext the decoder context
     * @return the decoded UUID
     * @since 1.0.0
     */
    @Override
    @NotNull
    public UUID decode(
            @NotNull BsonReader reader,
            @NotNull DecoderContext decoderContext
    ) {
        BsonType currentType = reader.getCurrentBsonType();

        return switch (currentType) {
            case STRING -> UUID.fromString(reader.readString());
            case BINARY -> {
                org.bson.BsonBinary binary = reader.readBinaryData();
                yield bytesToUuid(binary.getData());
            }
            default -> throw new IllegalArgumentException(
                    "Cannot decode UUID from BSON type: " + currentType
            );
        };
    }

    /**
     * Converts a UUID to a byte array.
     *
     * @param uuid the UUID
     * @return the byte array (16 bytes)
     */
    private static byte[] uuidToBytes(@NotNull UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (15 - i)));
        }

        return bytes;
    }

    /**
     * Converts a byte array to a UUID.
     *
     * @param bytes the byte array (16 bytes)
     * @return the UUID
     */
    @NotNull
    private static UUID bytesToUuid(byte @NotNull [] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("UUID byte array must be 16 bytes, got: " + bytes.length);
        }

        long msb = 0;
        long lsb = 0;

        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xFF);
        }

        return new UUID(msb, lsb);
    }

    /**
     * Returns the configured UUID representation.
     *
     * @return the representation mode
     * @since 1.0.0
     */
    @NotNull
    public UUIDRepresentation getRepresentation() {
        return representation;
    }
}
