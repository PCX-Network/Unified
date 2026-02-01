/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Objects;

/**
 * Base class for binary format serializers.
 *
 * <p>BinarySerializer provides a foundation for implementing efficient binary
 * serialization using {@link BinaryBuffer}. Subclasses implement the actual
 * encoding logic for specific types.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class PointSerializer extends BinarySerializer<Point> {
 *
 *     @Override
 *     protected void writeToBinary(Point value, BinaryBuffer buffer, SerializationContext context) {
 *         buffer.writeInt(value.x());
 *         buffer.writeInt(value.y());
 *         buffer.writeOptionalString(value.label());
 *     }
 *
 *     @Override
 *     protected Point readFromBinary(BinaryBuffer buffer, SerializationContext context) {
 *         int x = buffer.readInt();
 *         int y = buffer.readInt();
 *         String label = buffer.readOptionalString();
 *         return new Point(x, y, label);
 *     }
 *
 *     @Override
 *     public Class<Point> getTargetType() {
 *         return Point.class;
 *     }
 * }
 * }</pre>
 *
 * <h2>Format Support</h2>
 * <ul>
 *   <li><b>BINARY:</b> Raw binary output</li>
 *   <li><b>BASE64:</b> Base64-encoded binary</li>
 *   <li><b>JSON:</b> Not directly supported; subclasses may override</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>BinarySerializer implementations should be thread-safe as BinaryBuffer
 * instances are created per-operation.
 *
 * @param <T> the type of object this serializer handles
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BinaryBuffer
 * @see CompressedSerializer
 */
public abstract class BinarySerializer<T> implements Serializer<T> {

    private static final int INITIAL_BUFFER_SIZE = 512;
    private static final byte MAGIC_BYTE_1 = (byte) 0xBF; // Binary Format
    private static final byte MAGIC_BYTE_2 = (byte) 0x01; // Version 1

    /**
     * Creates a new BinarySerializer.
     */
    protected BinarySerializer() {}

    @Override
    @NotNull
    public String serialize(@NotNull T value, @NotNull SerializationContext context) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        byte[] bytes = toBytes(value, context);

        return switch (context.getFormat()) {
            case BINARY -> new String(bytes, context.getCharset());
            case BASE64 -> Base64.getEncoder().encodeToString(bytes);
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };
    }

    @Override
    @NotNull
    public T deserialize(@NotNull String data, @NotNull SerializationContext context) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        byte[] bytes = switch (context.getFormat()) {
            case BINARY -> data.getBytes(context.getCharset());
            case BASE64 -> Base64.getDecoder().decode(data);
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };

        return fromBytes(bytes, context);
    }

    @Override
    public byte @NotNull [] toBytes(@NotNull T value, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.allocate(INITIAL_BUFFER_SIZE);

        // Write header
        writeHeader(buffer, context);

        // Write payload
        try {
            writeToBinary(value, buffer, context);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw SerializationException.serializationFailed(getTargetType(), e);
        }

        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public T fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);

        // Read and validate header
        readHeader(buffer, context);

        // Read payload
        try {
            return readFromBinary(buffer, context);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw SerializationException.deserializationFailed(getTargetType(),
                    Base64.getEncoder().encodeToString(data), e);
        }
    }

    /**
     * Writes the object to the binary buffer.
     *
     * <p>Subclasses implement this method to define the binary format.
     *
     * @param value   the object to serialize
     * @param buffer  the output buffer
     * @param context the serialization context
     * @since 1.0.0
     */
    protected abstract void writeToBinary(@NotNull T value, @NotNull BinaryBuffer buffer,
                                          @NotNull SerializationContext context);

    /**
     * Reads an object from the binary buffer.
     *
     * <p>Subclasses implement this method to define the binary format.
     *
     * @param buffer  the input buffer
     * @param context the serialization context
     * @return the deserialized object
     * @since 1.0.0
     */
    @NotNull
    protected abstract T readFromBinary(@NotNull BinaryBuffer buffer,
                                         @NotNull SerializationContext context);

    /**
     * Writes the binary header with version information.
     *
     * @param buffer  the output buffer
     * @param context the serialization context
     * @since 1.0.0
     */
    protected void writeHeader(@NotNull BinaryBuffer buffer, @NotNull SerializationContext context) {
        buffer.writeByte(MAGIC_BYTE_1);
        buffer.writeByte(MAGIC_BYTE_2);

        SchemaVersion version = context.getVersion();
        buffer.writeVarInt(version.getMajor());
        buffer.writeVarInt(version.getMinor());
        buffer.writeVarInt(version.getPatch());
    }

    /**
     * Reads and validates the binary header.
     *
     * @param buffer  the input buffer
     * @param context the serialization context
     * @return the schema version from the header
     * @since 1.0.0
     */
    @NotNull
    protected SchemaVersion readHeader(@NotNull BinaryBuffer buffer, @NotNull SerializationContext context) {
        byte magic1 = buffer.readByte();
        byte magic2 = buffer.readByte();

        if (magic1 != MAGIC_BYTE_1 || magic2 != MAGIC_BYTE_2) {
            throw new SerializationException("Invalid binary format magic bytes");
        }

        int major = buffer.readVarInt();
        int minor = buffer.readVarInt();
        int patch = buffer.readVarInt();

        SchemaVersion dataVersion = SchemaVersion.of(major, minor, patch);
        SchemaVersion contextVersion = context.getVersion();

        // Check version compatibility
        if (!contextVersion.isCompatibleWith(dataVersion) && !context.isLenientParsing()) {
            throw SerializationException.versionIncompatible(getTargetType(), dataVersion, contextVersion);
        }

        return dataVersion;
    }

    /**
     * Returns the estimated buffer size for the given value.
     *
     * <p>Subclasses may override to optimize buffer allocation.
     *
     * @param value   the value to estimate
     * @param context the serialization context
     * @return the estimated size in bytes
     * @since 1.0.0
     */
    protected int estimateSize(@NotNull T value, @NotNull SerializationContext context) {
        return INITIAL_BUFFER_SIZE;
    }

    @Override
    public boolean supportsFormat(@NotNull SerializationContext.Format format) {
        return format == SerializationContext.Format.BINARY ||
               format == SerializationContext.Format.BASE64;
    }

    /**
     * Helper method to write a nullable value.
     *
     * @param buffer the buffer
     * @param value  the value or null
     * @param writer the writer function for non-null values
     * @param <V>    the value type
     * @since 1.0.0
     */
    protected static <V> void writeNullable(@NotNull BinaryBuffer buffer, V value,
                                            @NotNull java.util.function.BiConsumer<BinaryBuffer, V> writer) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            writer.accept(buffer, value);
        }
    }

    /**
     * Helper method to read a nullable value.
     *
     * @param buffer the buffer
     * @param reader the reader function for non-null values
     * @param <V>    the value type
     * @return the value or null
     * @since 1.0.0
     */
    protected static <V> V readNullable(@NotNull BinaryBuffer buffer,
                                         @NotNull java.util.function.Function<BinaryBuffer, V> reader) {
        boolean present = buffer.readBoolean();
        return present ? reader.apply(buffer) : null;
    }
}
