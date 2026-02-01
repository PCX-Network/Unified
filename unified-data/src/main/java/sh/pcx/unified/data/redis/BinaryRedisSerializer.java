/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Objects;

/**
 * Binary serializer for Redis using Java serialization.
 *
 * <p>This serializer converts Java objects to Base64-encoded binary strings
 * for storage in Redis. Objects must implement {@link Serializable}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a serializer
 * BinaryRedisSerializer<PlayerData> serializer = BinaryRedisSerializer.create();
 *
 * // Use with Redis
 * redis.set("player:uuid", playerData, serializer);
 * Optional<PlayerData> data = redis.get("player:uuid", PlayerData.class, serializer);
 * }</pre>
 *
 * <h2>Considerations</h2>
 * <ul>
 *   <li>Objects must implement {@link Serializable}</li>
 *   <li>Binary data is larger than JSON for small objects</li>
 *   <li>Not human-readable (debugging is harder)</li>
 *   <li>Version compatibility requires {@code serialVersionUID}</li>
 *   <li>Faster than JSON for complex object graphs</li>
 * </ul>
 *
 * <h2>When to Use</h2>
 * <p>Binary serialization is best for:
 * <ul>
 *   <li>Complex object graphs with circular references</li>
 *   <li>Objects that are not easily represented in JSON</li>
 *   <li>Performance-critical scenarios with large objects</li>
 *   <li>Temporary storage where human-readability isn't needed</li>
 * </ul>
 *
 * <p>Prefer {@link JsonRedisSerializer} for:
 * <ul>
 *   <li>Debugging and troubleshooting</li>
 *   <li>Cross-language compatibility</li>
 *   <li>Long-term storage</li>
 *   <li>Simple data structures</li>
 * </ul>
 *
 * @param <T> the type to serialize (must be Serializable)
 * @since 1.0.0
 * @author Supatuck
 * @see RedisSerializer
 * @see Serializable
 */
public final class BinaryRedisSerializer<T extends Serializable> implements RedisSerializer<T> {

    private static final BinaryRedisSerializer<?> INSTANCE = new BinaryRedisSerializer<>();

    private BinaryRedisSerializer() {
    }

    /**
     * Creates a binary serializer.
     *
     * <p>This method returns a shared instance since the serializer is stateless.
     *
     * @param <T> the type (must be Serializable)
     * @return a binary serializer
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> BinaryRedisSerializer<T> create() {
        return (BinaryRedisSerializer<T>) INSTANCE;
    }

    /**
     * Creates a binary serializer for a specific type.
     *
     * <p>The type parameter is for API compatibility; the returned serializer
     * can serialize any Serializable object.
     *
     * @param <T>  the type
     * @param type the class (must be Serializable)
     * @return a binary serializer
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> BinaryRedisSerializer<T> create(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");
        return (BinaryRedisSerializer<T>) INSTANCE;
    }

    @Override
    @NotNull
    public String serialize(@NotNull T value) {
        Objects.requireNonNull(value, "value cannot be null");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(value);
            oos.flush();

            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (IOException e) {
            throw new SerializationException("Failed to serialize object", e);
        }
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public T deserialize(@NotNull String data, @NotNull Class<T> type) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        try {
            byte[] bytes = Base64.getDecoder().decode(data);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {

                Object obj = ois.readObject();

                if (!type.isInstance(obj)) {
                    throw new SerializationException(
                            "Deserialized object is not of expected type. Expected: " +
                            type.getName() + ", got: " + obj.getClass().getName()
                    );
                }

                return (T) obj;
            }

        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid Base64 data", e);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Failed to deserialize object", e);
        }
    }

    /**
     * Deserializes without type checking.
     *
     * <p>Use this when the exact type is unknown but you know it's compatible.
     *
     * @param <R>  the expected return type
     * @param data the serialized data
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <R extends Serializable> R deserializeUnchecked(@NotNull String data) {
        Objects.requireNonNull(data, "data cannot be null");

        try {
            byte[] bytes = Base64.getDecoder().decode(data);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {

                return (R) ois.readObject();
            }

        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid Base64 data", e);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Failed to deserialize object", e);
        }
    }

    @Override
    @NotNull
    public String getContentType() {
        return "application/octet-stream";
    }

    /**
     * Gets the approximate size of the serialized data.
     *
     * <p>This can be used to estimate storage requirements.
     *
     * @param value the object to measure
     * @return the approximate size in bytes
     * @since 1.0.0
     */
    public int getSerializedSize(@NotNull T value) {
        return serialize(value).length();
    }

    /**
     * Checks if an object can be serialized.
     *
     * @param value the object to check
     * @return true if the object can be serialized
     * @since 1.0.0
     */
    public boolean canSerialize(@NotNull Object value) {
        if (!(value instanceof Serializable)) {
            return false;
        }

        try {
            serialize((T) value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a copy of an object through serialization.
     *
     * <p>This creates a deep copy of the object by serializing and
     * deserializing it.
     *
     * @param value the object to copy
     * @return a deep copy of the object
     * @throws SerializationException if the copy fails
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public T deepCopy(@NotNull T value) {
        Objects.requireNonNull(value, "value cannot be null");

        String serialized = serialize(value);
        return (T) deserializeUnchecked(serialized);
    }

    @Override
    public String toString() {
        return "BinaryRedisSerializer";
    }
}
