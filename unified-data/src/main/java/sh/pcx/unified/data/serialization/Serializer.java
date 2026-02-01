/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Base interface for object serializers.
 *
 * <p>A Serializer provides bidirectional conversion between objects of type {@code T}
 * and various serialized formats (JSON, Base64, binary). Implementations should be
 * thread-safe and reusable.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class PlayerDataSerializer implements Serializer<PlayerData> {
 *
 *     @Override
 *     public String serialize(PlayerData value, SerializationContext context) {
 *         return switch (context.getFormat()) {
 *             case JSON -> serializeToJson(value, context);
 *             case BASE64 -> Base64.getEncoder().encodeToString(toBytes(value, context));
 *             default -> throw new SerializationException("Unsupported format: " + context.getFormat());
 *         };
 *     }
 *
 *     @Override
 *     public PlayerData deserialize(String data, SerializationContext context) {
 *         return switch (context.getFormat()) {
 *             case JSON -> deserializeFromJson(data, context);
 *             case BASE64 -> fromBytes(Base64.getDecoder().decode(data), context);
 *             default -> throw new SerializationException("Unsupported format: " + context.getFormat());
 *         };
 *     }
 *
 *     @Override
 *     public Class<PlayerData> getTargetType() {
 *         return PlayerData.class;
 *     }
 * }
 * }</pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Serializer<ItemStack> serializer = Serializers.itemStack();
 *
 * // Serialize to JSON
 * String json = serializer.serialize(item, SerializationContext.json());
 *
 * // Serialize to Base64
 * String base64 = serializer.serialize(item, SerializationContext.base64());
 *
 * // Deserialize
 * ItemStack restored = serializer.deserialize(json, SerializationContext.json());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe. State should be stored in the
 * {@link SerializationContext} rather than in the serializer itself.
 *
 * @param <T> the type of object this serializer handles
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SerializationContext
 * @see Serializers
 */
public interface Serializer<T> {

    /**
     * Serializes an object to a string representation.
     *
     * <p>The output format depends on the {@link SerializationContext#getFormat()}.
     *
     * @param value   the object to serialize
     * @param context the serialization context
     * @return the serialized string
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    @NotNull
    String serialize(@NotNull T value, @NotNull SerializationContext context);

    /**
     * Deserializes a string to an object.
     *
     * <p>The input format should match the {@link SerializationContext#getFormat()}.
     *
     * @param data    the serialized data
     * @param context the serialization context
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    T deserialize(@NotNull String data, @NotNull SerializationContext context);

    /**
     * Serializes an object to a byte array.
     *
     * <p>For binary formats, this is the native representation. For text formats,
     * the string is encoded using the context's charset.
     *
     * @param value   the object to serialize
     * @param context the serialization context
     * @return the serialized bytes
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    default byte @NotNull [] toBytes(@NotNull T value, @NotNull SerializationContext context) {
        String serialized = serialize(value, context);
        return serialized.getBytes(context.getCharset());
    }

    /**
     * Deserializes a byte array to an object.
     *
     * <p>For binary formats, this is the native representation. For text formats,
     * the bytes are decoded using the context's charset.
     *
     * @param data    the serialized bytes
     * @param context the serialization context
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    default T fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        String decoded = new String(data, context.getCharset());
        return deserialize(decoded, context);
    }

    /**
     * Writes a serialized object to an output stream.
     *
     * @param value   the object to serialize
     * @param output  the output stream
     * @param context the serialization context
     * @throws IOException            if an I/O error occurs
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    default void writeTo(@NotNull T value, @NotNull OutputStream output,
                         @NotNull SerializationContext context) throws IOException {
        byte[] bytes = toBytes(value, context);
        output.write(bytes);
    }

    /**
     * Reads and deserializes an object from an input stream.
     *
     * @param input   the input stream
     * @param context the serialization context
     * @return the deserialized object
     * @throws IOException            if an I/O error occurs
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    default T readFrom(@NotNull InputStream input, @NotNull SerializationContext context)
            throws IOException {
        byte[] bytes = input.readAllBytes();
        return fromBytes(bytes, context);
    }

    /**
     * Serializes an object using default context.
     *
     * @param value the object to serialize
     * @return the serialized string
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    @NotNull
    default String serialize(@NotNull T value) {
        return serialize(value, SerializationContext.getDefault());
    }

    /**
     * Deserializes a string using default context.
     *
     * @param data the serialized data
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    default T deserialize(@NotNull String data) {
        return deserialize(data, SerializationContext.getDefault());
    }

    /**
     * Serializes an object to JSON format.
     *
     * @param value the object to serialize
     * @return the JSON string
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    @NotNull
    default String toJson(@NotNull T value) {
        return serialize(value, SerializationContext.json());
    }

    /**
     * Deserializes an object from JSON format.
     *
     * @param json the JSON string
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    default T fromJson(@NotNull String json) {
        return deserialize(json, SerializationContext.json());
    }

    /**
     * Serializes an object to Base64 format.
     *
     * @param value the object to serialize
     * @return the Base64 string
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    @NotNull
    default String toBase64(@NotNull T value) {
        return serialize(value, SerializationContext.base64());
    }

    /**
     * Deserializes an object from Base64 format.
     *
     * @param base64 the Base64 string
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    default T fromBase64(@NotNull String base64) {
        return deserialize(base64, SerializationContext.base64());
    }

    /**
     * Attempts to deserialize a string, returning empty on failure.
     *
     * @param data    the serialized data
     * @param context the serialization context
     * @return an Optional containing the deserialized object, or empty if failed
     * @since 1.0.0
     */
    @NotNull
    default Optional<T> tryDeserialize(@Nullable String data, @NotNull SerializationContext context) {
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(deserialize(data, context));
        } catch (SerializationException e) {
            return Optional.empty();
        }
    }

    /**
     * Attempts to deserialize a string using default context.
     *
     * @param data the serialized data
     * @return an Optional containing the deserialized object, or empty if failed
     * @since 1.0.0
     */
    @NotNull
    default Optional<T> tryDeserialize(@Nullable String data) {
        return tryDeserialize(data, SerializationContext.getDefault());
    }

    /**
     * Returns the target type class that this serializer handles.
     *
     * @return the target type class
     * @since 1.0.0
     */
    @NotNull
    Class<T> getTargetType();

    /**
     * Returns whether this serializer supports the specified format.
     *
     * @param format the format to check
     * @return true if the format is supported
     * @since 1.0.0
     */
    default boolean supportsFormat(@NotNull SerializationContext.Format format) {
        return true;
    }

    /**
     * Creates a serializer that applies a transformation before serialization
     * and after deserialization.
     *
     * @param <R>            the transformed type
     * @param toTransformed  function to transform from T to R
     * @param fromTransformed function to transform from R to T
     * @param targetType     the target type class for the new serializer
     * @return a new serializer that handles the transformed type
     * @since 1.0.0
     */
    @NotNull
    default <R> Serializer<R> map(
            @NotNull java.util.function.Function<T, R> toTransformed,
            @NotNull java.util.function.Function<R, T> fromTransformed,
            @NotNull Class<R> targetType) {
        Serializer<T> delegate = this;
        return new Serializer<>() {
            @Override
            @NotNull
            public String serialize(@NotNull R value, @NotNull SerializationContext context) {
                return delegate.serialize(fromTransformed.apply(value), context);
            }

            @Override
            @NotNull
            public R deserialize(@NotNull String data, @NotNull SerializationContext context) {
                return toTransformed.apply(delegate.deserialize(data, context));
            }

            @Override
            @NotNull
            public Class<R> getTargetType() {
                return targetType;
            }
        };
    }
}
