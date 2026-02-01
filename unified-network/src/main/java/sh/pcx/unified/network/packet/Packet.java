/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Wrapper interface for Minecraft protocol packets.
 *
 * <p>This interface provides a unified abstraction over raw Minecraft packets,
 * allowing version-independent packet inspection and modification. Implementations
 * wrap either ProtocolLib's {@code PacketContainer} or raw NMS packets.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Reading packet data
 * Packet packet = event.getPacket();
 * PacketType type = packet.getType();
 *
 * if (type == PacketType.PLAY_IN_USE_ENTITY) {
 *     int entityId = packet.getIntegers().read(0);
 *     // Handle entity interaction
 * }
 *
 * // Writing packet data
 * Packet spawn = packetService.createPacket(PacketType.PLAY_OUT_SPAWN_ENTITY);
 * spawn.getIntegers().write(0, entityId);
 * spawn.getDoubles().write(0, x);
 * spawn.getDoubles().write(1, y);
 * spawn.getDoubles().write(2, z);
 * packetService.sendPacket(player, spawn);
 *
 * // Cloning packets
 * Packet copy = packet.deepClone();
 * copy.getIntegers().write(0, newEntityId);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Packet instances are not thread-safe. Modifications should be done
 * on the same thread that handles the packet event.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PacketType
 * @see PacketService
 * @see PacketEvent
 */
public interface Packet {

    /**
     * Returns the type of this packet.
     *
     * @return the packet type
     * @since 1.0.0
     */
    @NotNull
    PacketType getType();

    /**
     * Returns the raw underlying packet object.
     *
     * <p>The actual type depends on the implementation:
     * <ul>
     *   <li>ProtocolLib: Returns the NMS packet object</li>
     *   <li>Standalone: Returns the NMS packet object</li>
     * </ul>
     *
     * @param <T> the expected packet type
     * @return the underlying packet object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();

    /**
     * Returns the ProtocolLib packet container if available.
     *
     * <p>This returns empty when using standalone mode without ProtocolLib.
     *
     * @param <T> the PacketContainer type
     * @return an Optional containing the packet container
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getProtocolLibContainer();

    /**
     * Returns a modifier for integer fields in this packet.
     *
     * @return the integer field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Integer> getIntegers();

    /**
     * Returns a modifier for byte fields in this packet.
     *
     * @return the byte field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Byte> getBytes();

    /**
     * Returns a modifier for short fields in this packet.
     *
     * @return the short field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Short> getShorts();

    /**
     * Returns a modifier for long fields in this packet.
     *
     * @return the long field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Long> getLongs();

    /**
     * Returns a modifier for float fields in this packet.
     *
     * @return the float field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Float> getFloats();

    /**
     * Returns a modifier for double fields in this packet.
     *
     * @return the double field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Double> getDoubles();

    /**
     * Returns a modifier for boolean fields in this packet.
     *
     * @return the boolean field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Boolean> getBooleans();

    /**
     * Returns a modifier for string fields in this packet.
     *
     * @return the string field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<String> getStrings();

    /**
     * Returns a modifier for UUID fields in this packet.
     *
     * @return the UUID field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<java.util.UUID> getUUIDs();

    /**
     * Returns a modifier for byte array fields in this packet.
     *
     * @return the byte array field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<byte[]> getByteArrays();

    /**
     * Returns a modifier for integer array fields in this packet.
     *
     * @return the integer array field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<int[]> getIntegerArrays();

    /**
     * Returns a modifier for specific field types.
     *
     * @param <T>   the field type
     * @param clazz the class of the field type
     * @return the field modifier
     * @since 1.0.0
     */
    @NotNull
    <T> FieldModifier<T> getSpecificModifier(@NotNull Class<T> clazz);

    /**
     * Returns the raw modifier for direct field access.
     *
     * @return the raw field modifier
     * @since 1.0.0
     */
    @NotNull
    FieldModifier<Object> getModifier();

    /**
     * Creates a deep clone of this packet.
     *
     * @return a new packet with copied data
     * @since 1.0.0
     */
    @NotNull
    Packet deepClone();

    /**
     * Creates a shallow clone of this packet.
     *
     * @return a new packet referencing the same data
     * @since 1.0.0
     */
    @NotNull
    Packet shallowClone();

    /**
     * Interface for modifying packet fields.
     *
     * @param <T> the field type
     * @since 1.0.0
     */
    interface FieldModifier<T> {

        /**
         * Reads a field value at the specified index.
         *
         * @param index the field index
         * @return the field value, or null if not present
         * @since 1.0.0
         */
        @Nullable
        T read(int index);

        /**
         * Reads a field value at the specified index with a default.
         *
         * @param index        the field index
         * @param defaultValue the default value if field is null
         * @return the field value or default
         * @since 1.0.0
         */
        @NotNull
        default T readOrDefault(int index, @NotNull T defaultValue) {
            T value = read(index);
            return value != null ? value : defaultValue;
        }

        /**
         * Reads a field value as Optional.
         *
         * @param index the field index
         * @return an Optional containing the value
         * @since 1.0.0
         */
        @NotNull
        default Optional<T> readOptional(int index) {
            return Optional.ofNullable(read(index));
        }

        /**
         * Writes a value to the field at the specified index.
         *
         * @param index the field index
         * @param value the value to write
         * @return this modifier for chaining
         * @since 1.0.0
         */
        @NotNull
        FieldModifier<T> write(int index, @Nullable T value);

        /**
         * Writes a value and returns the modifier for chaining.
         *
         * @param index the field index
         * @param value the value to write
         * @return this modifier for chaining
         * @since 1.0.0
         */
        @NotNull
        default FieldModifier<T> set(int index, @Nullable T value) {
            return write(index, value);
        }

        /**
         * Writes multiple values starting at index 0.
         *
         * @param values the values to write
         * @return this modifier for chaining
         * @since 1.0.0
         */
        @SuppressWarnings("unchecked")
        @NotNull
        default FieldModifier<T> writeAll(@NotNull T... values) {
            for (int i = 0; i < values.length; i++) {
                write(i, values[i]);
            }
            return this;
        }

        /**
         * Returns the number of fields of this type.
         *
         * @return the field count
         * @since 1.0.0
         */
        int size();

        /**
         * Checks if there are any fields of this type.
         *
         * @return true if there are fields
         * @since 1.0.0
         */
        default boolean isEmpty() {
            return size() == 0;
        }

        /**
         * Modifies a field value using a function.
         *
         * @param index    the field index
         * @param modifier the modification function
         * @return this modifier for chaining
         * @since 1.0.0
         */
        @NotNull
        default FieldModifier<T> modify(int index, @NotNull java.util.function.Function<T, T> modifier) {
            T current = read(index);
            if (current != null) {
                write(index, modifier.apply(current));
            }
            return this;
        }
    }
}
