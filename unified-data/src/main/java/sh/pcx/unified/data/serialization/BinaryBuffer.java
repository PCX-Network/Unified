/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * High-performance binary read/write buffer for serialization.
 *
 * <p>BinaryBuffer provides efficient methods for reading and writing primitive
 * types, strings, arrays, and complex objects to/from binary format. It supports
 * both reading and writing modes with automatic buffer expansion.
 *
 * <h2>Writing Example</h2>
 * <pre>{@code
 * BinaryBuffer buffer = BinaryBuffer.allocate(256);
 *
 * buffer.writeInt(42);
 * buffer.writeString("Hello, World!");
 * buffer.writeUUID(uuid);
 * buffer.writeVarInt(1000000);
 *
 * byte[] bytes = buffer.toByteArray();
 * }</pre>
 *
 * <h2>Reading Example</h2>
 * <pre>{@code
 * BinaryBuffer buffer = BinaryBuffer.wrap(bytes);
 *
 * int number = buffer.readInt();
 * String text = buffer.readString();
 * UUID id = buffer.readUUID();
 * int bigNum = buffer.readVarInt();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>BinaryBuffer is NOT thread-safe. Create separate instances for each thread
 * or synchronize access externally.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BinarySerializer
 * @see CompressedSerializer
 */
public final class BinaryBuffer {

    private static final int DEFAULT_CAPACITY = 256;
    private static final int MAX_STRING_LENGTH = 65535;
    private static final int MAX_ARRAY_LENGTH = 1_000_000;

    private byte[] data;
    private int position;
    private int limit;
    private ByteOrder order = ByteOrder.BIG_ENDIAN;

    private BinaryBuffer(byte[] data, int position, int limit) {
        this.data = data;
        this.position = position;
        this.limit = limit;
    }

    /**
     * Creates a new buffer with the default capacity.
     *
     * @return a new BinaryBuffer
     * @since 1.0.0
     */
    @NotNull
    public static BinaryBuffer allocate() {
        return allocate(DEFAULT_CAPACITY);
    }

    /**
     * Creates a new buffer with the specified initial capacity.
     *
     * @param capacity the initial capacity in bytes
     * @return a new BinaryBuffer
     * @since 1.0.0
     */
    @NotNull
    public static BinaryBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative: " + capacity);
        }
        return new BinaryBuffer(new byte[capacity], 0, 0);
    }

    /**
     * Wraps an existing byte array for reading.
     *
     * @param data the byte array to wrap
     * @return a new BinaryBuffer wrapping the array
     * @since 1.0.0
     */
    @NotNull
    public static BinaryBuffer wrap(byte @NotNull [] data) {
        return new BinaryBuffer(data.clone(), 0, data.length);
    }

    /**
     * Wraps a portion of a byte array for reading.
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @param length the length to wrap
     * @return a new BinaryBuffer wrapping the portion
     * @since 1.0.0
     */
    @NotNull
    public static BinaryBuffer wrap(byte @NotNull [] data, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IllegalArgumentException("Invalid offset/length for array of size " + data.length);
        }
        byte[] copy = new byte[length];
        System.arraycopy(data, offset, copy, 0, length);
        return new BinaryBuffer(copy, 0, length);
    }

    // ========================================
    // Configuration
    // ========================================

    /**
     * Sets the byte order for multi-byte values.
     *
     * @param order the byte order
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer order(@NotNull ByteOrder order) {
        this.order = order;
        return this;
    }

    /**
     * Returns the current byte order.
     *
     * @return the byte order
     * @since 1.0.0
     */
    @NotNull
    public ByteOrder order() {
        return order;
    }

    // ========================================
    // Position and Limit
    // ========================================

    /**
     * Returns the current position.
     *
     * @return the position
     * @since 1.0.0
     */
    public int position() {
        return position;
    }

    /**
     * Sets the current position.
     *
     * @param position the new position
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer position(int position) {
        if (position < 0 || position > limit) {
            throw new IllegalArgumentException("Position must be between 0 and limit: " + position);
        }
        this.position = position;
        return this;
    }

    /**
     * Returns the current limit (amount of data written or available).
     *
     * @return the limit
     * @since 1.0.0
     */
    public int limit() {
        return limit;
    }

    /**
     * Returns the remaining bytes available for reading.
     *
     * @return the remaining bytes
     * @since 1.0.0
     */
    public int remaining() {
        return limit - position;
    }

    /**
     * Checks if there are remaining bytes available.
     *
     * @return true if bytes remain
     * @since 1.0.0
     */
    public boolean hasRemaining() {
        return position < limit;
    }

    /**
     * Returns the capacity of the buffer.
     *
     * @return the capacity
     * @since 1.0.0
     */
    public int capacity() {
        return data.length;
    }

    /**
     * Resets the position to 0 for re-reading.
     *
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer rewind() {
        position = 0;
        return this;
    }

    /**
     * Flips the buffer from writing to reading mode.
     *
     * <p>Sets limit to current position and resets position to 0.
     *
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer flip() {
        limit = position;
        position = 0;
        return this;
    }

    /**
     * Clears the buffer for reuse.
     *
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer clear() {
        position = 0;
        limit = 0;
        return this;
    }

    // ========================================
    // Writing Primitives
    // ========================================

    /**
     * Writes a single byte.
     *
     * @param value the byte value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeByte(int value) {
        ensureCapacity(1);
        data[position++] = (byte) value;
        limit = Math.max(limit, position);
        return this;
    }

    /**
     * Writes a boolean as a single byte.
     *
     * @param value the boolean value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    /**
     * Writes a short (2 bytes).
     *
     * @param value the short value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeShort(int value) {
        ensureCapacity(2);
        if (order == ByteOrder.BIG_ENDIAN) {
            data[position++] = (byte) (value >> 8);
            data[position++] = (byte) value;
        } else {
            data[position++] = (byte) value;
            data[position++] = (byte) (value >> 8);
        }
        limit = Math.max(limit, position);
        return this;
    }

    /**
     * Writes an int (4 bytes).
     *
     * @param value the int value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeInt(int value) {
        ensureCapacity(4);
        if (order == ByteOrder.BIG_ENDIAN) {
            data[position++] = (byte) (value >> 24);
            data[position++] = (byte) (value >> 16);
            data[position++] = (byte) (value >> 8);
            data[position++] = (byte) value;
        } else {
            data[position++] = (byte) value;
            data[position++] = (byte) (value >> 8);
            data[position++] = (byte) (value >> 16);
            data[position++] = (byte) (value >> 24);
        }
        limit = Math.max(limit, position);
        return this;
    }

    /**
     * Writes a long (8 bytes).
     *
     * @param value the long value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeLong(long value) {
        ensureCapacity(8);
        if (order == ByteOrder.BIG_ENDIAN) {
            data[position++] = (byte) (value >> 56);
            data[position++] = (byte) (value >> 48);
            data[position++] = (byte) (value >> 40);
            data[position++] = (byte) (value >> 32);
            data[position++] = (byte) (value >> 24);
            data[position++] = (byte) (value >> 16);
            data[position++] = (byte) (value >> 8);
            data[position++] = (byte) value;
        } else {
            data[position++] = (byte) value;
            data[position++] = (byte) (value >> 8);
            data[position++] = (byte) (value >> 16);
            data[position++] = (byte) (value >> 24);
            data[position++] = (byte) (value >> 32);
            data[position++] = (byte) (value >> 40);
            data[position++] = (byte) (value >> 48);
            data[position++] = (byte) (value >> 56);
        }
        limit = Math.max(limit, position);
        return this;
    }

    /**
     * Writes a float (4 bytes).
     *
     * @param value the float value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeFloat(float value) {
        return writeInt(Float.floatToIntBits(value));
    }

    /**
     * Writes a double (8 bytes).
     *
     * @param value the double value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeDouble(double value) {
        return writeLong(Double.doubleToLongBits(value));
    }

    /**
     * Writes a variable-length integer (1-5 bytes).
     *
     * <p>Uses VarInt encoding where smaller values use fewer bytes.
     *
     * @param value the int value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeVarInt(int value) {
        ensureCapacity(5);
        while ((value & ~0x7F) != 0) {
            data[position++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        data[position++] = (byte) value;
        limit = Math.max(limit, position);
        return this;
    }

    /**
     * Writes a variable-length long (1-10 bytes).
     *
     * @param value the long value
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeVarLong(long value) {
        ensureCapacity(10);
        while ((value & ~0x7FL) != 0) {
            data[position++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        data[position++] = (byte) value;
        limit = Math.max(limit, position);
        return this;
    }

    // ========================================
    // Writing Complex Types
    // ========================================

    /**
     * Writes a byte array with length prefix.
     *
     * @param bytes the byte array
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeBytes(byte @NotNull [] bytes) {
        writeVarInt(bytes.length);
        ensureCapacity(bytes.length);
        System.arraycopy(bytes, 0, data, position, bytes.length);
        position += bytes.length;
        limit = Math.max(limit, position);
        return this;
    }

    /**
     * Writes raw bytes without length prefix.
     *
     * @param bytes the byte array
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeRawBytes(byte @NotNull [] bytes) {
        ensureCapacity(bytes.length);
        System.arraycopy(bytes, 0, data, position, bytes.length);
        position += bytes.length;
        limit = Math.max(limit, position);
        return this;
    }

    /**
     * Writes a string as UTF-8 with length prefix.
     *
     * @param value the string
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeString(@NotNull String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_LENGTH) {
            throw new SerializationException("String too long: " + bytes.length + " > " + MAX_STRING_LENGTH);
        }
        return writeBytes(bytes);
    }

    /**
     * Writes an optional string (null-safe).
     *
     * @param value the string, or null
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeOptionalString(@Nullable String value) {
        writeBoolean(value != null);
        if (value != null) {
            writeString(value);
        }
        return this;
    }

    /**
     * Writes a UUID as 16 bytes.
     *
     * @param uuid the UUID
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public BinaryBuffer writeUUID(@NotNull UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    /**
     * Writes an enum by its ordinal.
     *
     * @param value the enum value
     * @param <E>   the enum type
     * @return this buffer for chaining
     * @since 1.0.0
     */
    @NotNull
    public <E extends Enum<E>> BinaryBuffer writeEnum(@NotNull E value) {
        return writeVarInt(value.ordinal());
    }

    // ========================================
    // Reading Primitives
    // ========================================

    /**
     * Reads a single byte.
     *
     * @return the byte value
     * @since 1.0.0
     */
    public byte readByte() {
        checkReadable(1);
        return data[position++];
    }

    /**
     * Reads an unsigned byte.
     *
     * @return the unsigned byte value (0-255)
     * @since 1.0.0
     */
    public int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    /**
     * Reads a boolean.
     *
     * @return the boolean value
     * @since 1.0.0
     */
    public boolean readBoolean() {
        return readByte() != 0;
    }

    /**
     * Reads a short (2 bytes).
     *
     * @return the short value
     * @since 1.0.0
     */
    public short readShort() {
        checkReadable(2);
        if (order == ByteOrder.BIG_ENDIAN) {
            return (short) ((data[position++] << 8) | (data[position++] & 0xFF));
        } else {
            return (short) ((data[position++] & 0xFF) | (data[position++] << 8));
        }
    }

    /**
     * Reads an unsigned short.
     *
     * @return the unsigned short value (0-65535)
     * @since 1.0.0
     */
    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads an int (4 bytes).
     *
     * @return the int value
     * @since 1.0.0
     */
    public int readInt() {
        checkReadable(4);
        if (order == ByteOrder.BIG_ENDIAN) {
            return ((data[position++] & 0xFF) << 24) |
                   ((data[position++] & 0xFF) << 16) |
                   ((data[position++] & 0xFF) << 8) |
                   (data[position++] & 0xFF);
        } else {
            return (data[position++] & 0xFF) |
                   ((data[position++] & 0xFF) << 8) |
                   ((data[position++] & 0xFF) << 16) |
                   ((data[position++] & 0xFF) << 24);
        }
    }

    /**
     * Reads a long (8 bytes).
     *
     * @return the long value
     * @since 1.0.0
     */
    public long readLong() {
        checkReadable(8);
        if (order == ByteOrder.BIG_ENDIAN) {
            return ((long) (data[position++] & 0xFF) << 56) |
                   ((long) (data[position++] & 0xFF) << 48) |
                   ((long) (data[position++] & 0xFF) << 40) |
                   ((long) (data[position++] & 0xFF) << 32) |
                   ((long) (data[position++] & 0xFF) << 24) |
                   ((long) (data[position++] & 0xFF) << 16) |
                   ((long) (data[position++] & 0xFF) << 8) |
                   ((long) (data[position++] & 0xFF));
        } else {
            return ((long) (data[position++] & 0xFF)) |
                   ((long) (data[position++] & 0xFF) << 8) |
                   ((long) (data[position++] & 0xFF) << 16) |
                   ((long) (data[position++] & 0xFF) << 24) |
                   ((long) (data[position++] & 0xFF) << 32) |
                   ((long) (data[position++] & 0xFF) << 40) |
                   ((long) (data[position++] & 0xFF) << 48) |
                   ((long) (data[position++] & 0xFF) << 56);
        }
    }

    /**
     * Reads a float (4 bytes).
     *
     * @return the float value
     * @since 1.0.0
     */
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads a double (8 bytes).
     *
     * @return the double value
     * @since 1.0.0
     */
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads a variable-length integer.
     *
     * @return the int value
     * @since 1.0.0
     */
    public int readVarInt() {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            if (shift >= 35) {
                throw new SerializationException("VarInt too large");
            }
            b = readByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    /**
     * Reads a variable-length long.
     *
     * @return the long value
     * @since 1.0.0
     */
    public long readVarLong() {
        long value = 0;
        int shift = 0;
        byte b;
        do {
            if (shift >= 70) {
                throw new SerializationException("VarLong too large");
            }
            b = readByte();
            value |= (long) (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    // ========================================
    // Reading Complex Types
    // ========================================

    /**
     * Reads a length-prefixed byte array.
     *
     * @return the byte array
     * @since 1.0.0
     */
    public byte @NotNull [] readBytes() {
        int length = readVarInt();
        if (length < 0 || length > MAX_ARRAY_LENGTH) {
            throw new SerializationException("Invalid byte array length: " + length);
        }
        checkReadable(length);
        byte[] result = new byte[length];
        System.arraycopy(data, position, result, 0, length);
        position += length;
        return result;
    }

    /**
     * Reads a fixed number of raw bytes.
     *
     * @param length the number of bytes to read
     * @return the byte array
     * @since 1.0.0
     */
    public byte @NotNull [] readRawBytes(int length) {
        checkReadable(length);
        byte[] result = new byte[length];
        System.arraycopy(data, position, result, 0, length);
        position += length;
        return result;
    }

    /**
     * Reads a UTF-8 string.
     *
     * @return the string
     * @since 1.0.0
     */
    @NotNull
    public String readString() {
        byte[] bytes = readBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Reads an optional string.
     *
     * @return the string, or null if not present
     * @since 1.0.0
     */
    @Nullable
    public String readOptionalString() {
        boolean present = readBoolean();
        return present ? readString() : null;
    }

    /**
     * Reads a UUID.
     *
     * @return the UUID
     * @since 1.0.0
     */
    @NotNull
    public UUID readUUID() {
        long most = readLong();
        long least = readLong();
        return new UUID(most, least);
    }

    /**
     * Reads an enum by ordinal.
     *
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return the enum value
     * @since 1.0.0
     */
    @NotNull
    public <E extends Enum<E>> E readEnum(@NotNull Class<E> enumClass) {
        int ordinal = readVarInt();
        E[] values = enumClass.getEnumConstants();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new SerializationException("Invalid enum ordinal " + ordinal + " for " + enumClass.getName());
        }
        return values[ordinal];
    }

    // ========================================
    // Output Methods
    // ========================================

    /**
     * Returns the buffer contents as a byte array.
     *
     * @return the byte array
     * @since 1.0.0
     */
    public byte @NotNull [] toByteArray() {
        return Arrays.copyOf(data, limit);
    }

    /**
     * Returns a ByteBuffer view of the data.
     *
     * @return a ByteBuffer
     * @since 1.0.0
     */
    @NotNull
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(data, 0, limit).order(order);
    }

    /**
     * Returns a DataInputStream for reading.
     *
     * @return a DataInputStream
     * @since 1.0.0
     */
    @NotNull
    public DataInputStream toInputStream() {
        return new DataInputStream(new ByteArrayInputStream(data, 0, limit));
    }

    // ========================================
    // Internal Methods
    // ========================================

    private void ensureCapacity(int bytes) {
        int required = position + bytes;
        if (required > data.length) {
            int newCapacity = Math.max(data.length * 2, required);
            data = Arrays.copyOf(data, newCapacity);
        }
    }

    private void checkReadable(int bytes) {
        if (position + bytes > limit) {
            throw new SerializationException(
                    "Buffer underflow: need " + bytes + " bytes but only " + remaining() + " available");
        }
    }
}
