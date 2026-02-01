/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for serializing and deserializing values for Redis storage.
 *
 * <p>Redis stores all values as strings (or bytes). This interface provides
 * a way to convert Java objects to and from their Redis representation.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Using the JSON serializer
 * RedisSerializer<PlayerData> serializer = JsonRedisSerializer.create(PlayerData.class);
 *
 * // Store typed data
 * redis.set("player:uuid", playerData, serializer);
 *
 * // Retrieve typed data
 * Optional<PlayerData> data = redis.get("player:uuid", PlayerData.class, serializer);
 *
 * // Custom serializer
 * RedisSerializer<Location> locationSerializer = new RedisSerializer<>() {
 *     @Override
 *     public String serialize(Location loc) {
 *         return loc.getWorld() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
 *     }
 *
 *     @Override
 *     public Location deserialize(String data, Class<Location> type) {
 *         String[] parts = data.split(":");
 *         return new Location(
 *             getWorld(parts[0]),
 *             Double.parseDouble(parts[1]),
 *             Double.parseDouble(parts[2]),
 *             Double.parseDouble(parts[3])
 *         );
 *     }
 * };
 * }</pre>
 *
 * <h2>Built-in Serializers</h2>
 * <ul>
 *   <li>{@link JsonRedisSerializer} - JSON serialization using Gson</li>
 *   <li>{@link BinaryRedisSerializer} - Binary serialization using Java serialization</li>
 *   <li>{@link #string()} - Pass-through for String values</li>
 *   <li>{@link #integer()} - Integer serialization</li>
 *   <li>{@link #longValue()} - Long serialization</li>
 *   <li>{@link #doubleValue()} - Double serialization</li>
 *   <li>{@link #bool()} - Boolean serialization</li>
 * </ul>
 *
 * @param <T> the type to serialize
 * @since 1.0.0
 * @author Supatuck
 * @see JsonRedisSerializer
 * @see BinaryRedisSerializer
 */
public interface RedisSerializer<T> {

    /**
     * Serializes an object to a string representation.
     *
     * @param value the object to serialize
     * @return the serialized string
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    @NotNull
    String serialize(@NotNull T value);

    /**
     * Deserializes a string to an object.
     *
     * @param data the serialized string
     * @param type the target type class
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    T deserialize(@NotNull String data, @NotNull Class<T> type);

    /**
     * Deserializes a string to an object, returning null on failure.
     *
     * @param data the serialized string (may be null)
     * @param type the target type class
     * @return the deserialized object, or null if data is null or deserialization fails
     * @since 1.0.0
     */
    @Nullable
    default T deserializeSafe(@Nullable String data, @NotNull Class<T> type) {
        if (data == null) {
            return null;
        }
        try {
            return deserialize(data, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the content type identifier for this serializer.
     *
     * <p>This can be used for debugging or for choosing serialization strategies.
     *
     * @return the content type (e.g., "application/json", "text/plain", "application/octet-stream")
     * @since 1.0.0
     */
    @NotNull
    default String getContentType() {
        return "text/plain";
    }

    // ========== Built-in Serializers ==========

    /**
     * Returns a pass-through serializer for String values.
     *
     * @return the string serializer
     * @since 1.0.0
     */
    @NotNull
    static RedisSerializer<String> string() {
        return StringSerializer.INSTANCE;
    }

    /**
     * Returns a serializer for Integer values.
     *
     * @return the integer serializer
     * @since 1.0.0
     */
    @NotNull
    static RedisSerializer<Integer> integer() {
        return IntegerSerializer.INSTANCE;
    }

    /**
     * Returns a serializer for Long values.
     *
     * @return the long serializer
     * @since 1.0.0
     */
    @NotNull
    static RedisSerializer<Long> longValue() {
        return LongSerializer.INSTANCE;
    }

    /**
     * Returns a serializer for Double values.
     *
     * @return the double serializer
     * @since 1.0.0
     */
    @NotNull
    static RedisSerializer<Double> doubleValue() {
        return DoubleSerializer.INSTANCE;
    }

    /**
     * Returns a serializer for Boolean values.
     *
     * @return the boolean serializer
     * @since 1.0.0
     */
    @NotNull
    static RedisSerializer<Boolean> bool() {
        return BooleanSerializer.INSTANCE;
    }

    /**
     * Creates a serializer that compresses data using GZIP.
     *
     * @param <T>      the value type
     * @param delegate the underlying serializer
     * @return a compressing serializer
     * @since 1.0.0
     */
    @NotNull
    static <T> RedisSerializer<T> compressed(@NotNull RedisSerializer<T> delegate) {
        return new CompressedSerializer<>(delegate);
    }

    /**
     * Creates a serializer that adds a type prefix for type safety.
     *
     * @param <T>      the value type
     * @param delegate the underlying serializer
     * @param prefix   the type prefix
     * @return a prefixed serializer
     * @since 1.0.0
     */
    @NotNull
    static <T> RedisSerializer<T> prefixed(@NotNull RedisSerializer<T> delegate, @NotNull String prefix) {
        return new PrefixedSerializer<>(delegate, prefix);
    }

    // ========== Exception ==========

    /**
     * Exception thrown when serialization or deserialization fails.
     *
     * @since 1.0.0
     */
    class SerializationException extends RuntimeException {

        /**
         * Creates a new serialization exception.
         *
         * @param message the error message
         */
        public SerializationException(String message) {
            super(message);
        }

        /**
         * Creates a new serialization exception with a cause.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ========== Built-in Implementations ==========

    /**
     * String pass-through serializer.
     */
    final class StringSerializer implements RedisSerializer<String> {
        static final StringSerializer INSTANCE = new StringSerializer();

        private StringSerializer() {}

        @Override
        @NotNull
        public String serialize(@NotNull String value) {
            return value;
        }

        @Override
        @NotNull
        public String deserialize(@NotNull String data, @NotNull Class<String> type) {
            return data;
        }

        @Override
        @NotNull
        public String getContentType() {
            return "text/plain";
        }
    }

    /**
     * Integer serializer.
     */
    final class IntegerSerializer implements RedisSerializer<Integer> {
        static final IntegerSerializer INSTANCE = new IntegerSerializer();

        private IntegerSerializer() {}

        @Override
        @NotNull
        public String serialize(@NotNull Integer value) {
            return value.toString();
        }

        @Override
        @NotNull
        public Integer deserialize(@NotNull String data, @NotNull Class<Integer> type) {
            try {
                return Integer.parseInt(data);
            } catch (NumberFormatException e) {
                throw new SerializationException("Invalid integer: " + data, e);
            }
        }
    }

    /**
     * Long serializer.
     */
    final class LongSerializer implements RedisSerializer<Long> {
        static final LongSerializer INSTANCE = new LongSerializer();

        private LongSerializer() {}

        @Override
        @NotNull
        public String serialize(@NotNull Long value) {
            return value.toString();
        }

        @Override
        @NotNull
        public Long deserialize(@NotNull String data, @NotNull Class<Long> type) {
            try {
                return Long.parseLong(data);
            } catch (NumberFormatException e) {
                throw new SerializationException("Invalid long: " + data, e);
            }
        }
    }

    /**
     * Double serializer.
     */
    final class DoubleSerializer implements RedisSerializer<Double> {
        static final DoubleSerializer INSTANCE = new DoubleSerializer();

        private DoubleSerializer() {}

        @Override
        @NotNull
        public String serialize(@NotNull Double value) {
            return value.toString();
        }

        @Override
        @NotNull
        public Double deserialize(@NotNull String data, @NotNull Class<Double> type) {
            try {
                return Double.parseDouble(data);
            } catch (NumberFormatException e) {
                throw new SerializationException("Invalid double: " + data, e);
            }
        }
    }

    /**
     * Boolean serializer.
     */
    final class BooleanSerializer implements RedisSerializer<Boolean> {
        static final BooleanSerializer INSTANCE = new BooleanSerializer();

        private BooleanSerializer() {}

        @Override
        @NotNull
        public String serialize(@NotNull Boolean value) {
            return value ? "1" : "0";
        }

        @Override
        @NotNull
        public Boolean deserialize(@NotNull String data, @NotNull Class<Boolean> type) {
            return "1".equals(data) || "true".equalsIgnoreCase(data);
        }
    }

    /**
     * Compressed serializer wrapper.
     */
    final class CompressedSerializer<T> implements RedisSerializer<T> {
        private final RedisSerializer<T> delegate;

        CompressedSerializer(RedisSerializer<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        @NotNull
        public String serialize(@NotNull T value) {
            String serialized = delegate.serialize(value);
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(baos)) {
                    gzip.write(serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (java.io.IOException e) {
                throw new SerializationException("Compression failed", e);
            }
        }

        @Override
        @NotNull
        public T deserialize(@NotNull String data, @NotNull Class<T> type) {
            try {
                byte[] compressed = java.util.Base64.getDecoder().decode(data);
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(compressed);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = gzip.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                }
                String decompressed = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
                return delegate.deserialize(decompressed, type);
            } catch (java.io.IOException e) {
                throw new SerializationException("Decompression failed", e);
            }
        }

        @Override
        @NotNull
        public String getContentType() {
            return "application/gzip";
        }
    }

    /**
     * Prefixed serializer wrapper.
     */
    final class PrefixedSerializer<T> implements RedisSerializer<T> {
        private final RedisSerializer<T> delegate;
        private final String prefix;

        PrefixedSerializer(RedisSerializer<T> delegate, String prefix) {
            this.delegate = delegate;
            this.prefix = prefix + ":";
        }

        @Override
        @NotNull
        public String serialize(@NotNull T value) {
            return prefix + delegate.serialize(value);
        }

        @Override
        @NotNull
        public T deserialize(@NotNull String data, @NotNull Class<T> type) {
            if (!data.startsWith(prefix)) {
                throw new SerializationException("Expected prefix '" + prefix + "' not found");
            }
            return delegate.deserialize(data.substring(prefix.length()), type);
        }

        @Override
        @NotNull
        public String getContentType() {
            return delegate.getContentType();
        }
    }
}
