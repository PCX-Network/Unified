/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * JSON serializer for Redis using Gson.
 *
 * <p>This serializer converts Java objects to JSON strings for storage in Redis
 * and deserializes JSON strings back to Java objects on retrieval.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a serializer for a specific type
 * JsonRedisSerializer<PlayerData> serializer = JsonRedisSerializer.create(PlayerData.class);
 *
 * // Use with Redis
 * redis.set("player:uuid", playerData, serializer);
 * Optional<PlayerData> data = redis.get("player:uuid", PlayerData.class, serializer);
 *
 * // With custom Gson configuration
 * Gson gson = new GsonBuilder()
 *     .setDateFormat("yyyy-MM-dd HH:mm:ss")
 *     .registerTypeAdapter(Location.class, new LocationAdapter())
 *     .create();
 *
 * JsonRedisSerializer<PlayerData> customSerializer = JsonRedisSerializer.create(PlayerData.class, gson);
 *
 * // For generic types
 * Type listType = new TypeToken<List<PlayerData>>(){}.getType();
 * JsonRedisSerializer<List<PlayerData>> listSerializer = JsonRedisSerializer.create(listType);
 * }</pre>
 *
 * <h2>Pretty Printing</h2>
 * <pre>{@code
 * // Enable pretty printing for debugging
 * JsonRedisSerializer<PlayerData> prettySerializer = JsonRedisSerializer.builder(PlayerData.class)
 *     .prettyPrint()
 *     .build();
 * }</pre>
 *
 * <h2>Type Handling</h2>
 * <p>For complex generic types, use Gson's {@code TypeToken}:
 * <pre>{@code
 * Type mapType = new TypeToken<Map<UUID, PlayerData>>(){}.getType();
 * JsonRedisSerializer<Map<UUID, PlayerData>> mapSerializer = JsonRedisSerializer.create(mapType);
 * }</pre>
 *
 * @param <T> the type to serialize
 * @since 1.0.0
 * @author Supatuck
 * @see RedisSerializer
 * @see Gson
 */
public final class JsonRedisSerializer<T> implements RedisSerializer<T> {

    /**
     * Default shared Gson instance.
     */
    private static final Gson DEFAULT_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private final Gson gson;
    private final Type type;

    /**
     * Creates a new JSON serializer.
     *
     * @param type the target type
     * @param gson the Gson instance to use
     */
    private JsonRedisSerializer(@NotNull Type type, @NotNull Gson gson) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.gson = Objects.requireNonNull(gson, "gson cannot be null");
    }

    /**
     * Creates a JSON serializer for the specified class.
     *
     * @param <T>  the type
     * @param type the class to serialize
     * @return a new JSON serializer
     * @since 1.0.0
     */
    @NotNull
    public static <T> JsonRedisSerializer<T> create(@NotNull Class<T> type) {
        return new JsonRedisSerializer<>(type, DEFAULT_GSON);
    }

    /**
     * Creates a JSON serializer for the specified class with a custom Gson.
     *
     * @param <T>  the type
     * @param type the class to serialize
     * @param gson the Gson instance
     * @return a new JSON serializer
     * @since 1.0.0
     */
    @NotNull
    public static <T> JsonRedisSerializer<T> create(@NotNull Class<T> type, @NotNull Gson gson) {
        return new JsonRedisSerializer<>(type, gson);
    }

    /**
     * Creates a JSON serializer for the specified generic type.
     *
     * <p>Use Gson's {@code TypeToken} to create the Type:
     * <pre>{@code
     * Type type = new TypeToken<List<String>>(){}.getType();
     * }</pre>
     *
     * @param <T>  the type
     * @param type the generic type
     * @return a new JSON serializer
     * @since 1.0.0
     */
    @NotNull
    public static <T> JsonRedisSerializer<T> create(@NotNull Type type) {
        return new JsonRedisSerializer<>(type, DEFAULT_GSON);
    }

    /**
     * Creates a JSON serializer for the specified generic type with a custom Gson.
     *
     * @param <T>  the type
     * @param type the generic type
     * @param gson the Gson instance
     * @return a new JSON serializer
     * @since 1.0.0
     */
    @NotNull
    public static <T> JsonRedisSerializer<T> create(@NotNull Type type, @NotNull Gson gson) {
        return new JsonRedisSerializer<>(type, gson);
    }

    /**
     * Creates a builder for configuring the JSON serializer.
     *
     * @param <T>  the type
     * @param type the class to serialize
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder(@NotNull Class<T> type) {
        return new Builder<>(type);
    }

    /**
     * Creates a builder for configuring the JSON serializer with a generic type.
     *
     * @param <T>  the type
     * @param type the generic type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder(@NotNull Type type) {
        return new Builder<>(type);
    }

    @Override
    @NotNull
    public String serialize(@NotNull T value) {
        Objects.requireNonNull(value, "value cannot be null");
        try {
            return gson.toJson(value, type);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize to JSON", e);
        }
    }

    @Override
    @NotNull
    public T deserialize(@NotNull String data, @NotNull Class<T> clazz) {
        Objects.requireNonNull(data, "data cannot be null");
        try {
            T result = gson.fromJson(data, type);
            if (result == null) {
                throw new SerializationException("Deserialized value is null");
            }
            return result;
        } catch (JsonSyntaxException e) {
            throw new SerializationException("Failed to deserialize JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes using the configured type (ignoring the class parameter).
     *
     * @param data the JSON string
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public T deserialize(@NotNull String data) {
        Objects.requireNonNull(data, "data cannot be null");
        try {
            T result = gson.fromJson(data, type);
            if (result == null) {
                throw new SerializationException("Deserialized value is null");
            }
            return result;
        } catch (JsonSyntaxException e) {
            throw new SerializationException("Failed to deserialize JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @NotNull
    public String getContentType() {
        return "application/json";
    }

    /**
     * Gets the Gson instance used by this serializer.
     *
     * @return the Gson instance
     * @since 1.0.0
     */
    @NotNull
    public Gson getGson() {
        return gson;
    }

    /**
     * Gets the target type for this serializer.
     *
     * @return the target type
     * @since 1.0.0
     */
    @NotNull
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "JsonRedisSerializer[type=" + type.getTypeName() + "]";
    }

    /**
     * Builder for configuring JsonRedisSerializer.
     *
     * @param <T> the type to serialize
     * @since 1.0.0
     */
    public static final class Builder<T> {

        private final Type type;
        private GsonBuilder gsonBuilder;
        private Gson gson;

        private Builder(@NotNull Type type) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            this.gsonBuilder = new GsonBuilder().disableHtmlEscaping();
        }

        /**
         * Uses a pre-configured Gson instance.
         *
         * <p>This overrides any configuration set via builder methods.
         *
         * @param gson the Gson instance
         * @return this builder
         */
        @NotNull
        public Builder<T> gson(@NotNull Gson gson) {
            this.gson = Objects.requireNonNull(gson);
            return this;
        }

        /**
         * Enables pretty printing for human-readable output.
         *
         * <p>Note: This increases storage size and should only be used for debugging.
         *
         * @return this builder
         */
        @NotNull
        public Builder<T> prettyPrint() {
            ensureBuilder();
            gsonBuilder.setPrettyPrinting();
            return this;
        }

        /**
         * Enables serialization of null values.
         *
         * @return this builder
         */
        @NotNull
        public Builder<T> serializeNulls() {
            ensureBuilder();
            gsonBuilder.serializeNulls();
            return this;
        }

        /**
         * Sets the date format for serialization.
         *
         * @param pattern the date format pattern
         * @return this builder
         */
        @NotNull
        public Builder<T> dateFormat(@NotNull String pattern) {
            ensureBuilder();
            gsonBuilder.setDateFormat(pattern);
            return this;
        }

        /**
         * Registers a type adapter.
         *
         * @param type    the type to adapt
         * @param adapter the adapter instance
         * @return this builder
         */
        @NotNull
        public Builder<T> registerTypeAdapter(@NotNull Type type, @NotNull Object adapter) {
            ensureBuilder();
            gsonBuilder.registerTypeAdapter(type, adapter);
            return this;
        }

        /**
         * Registers a type adapter factory.
         *
         * @param factory the factory
         * @return this builder
         */
        @NotNull
        public Builder<T> registerTypeAdapterFactory(@NotNull com.google.gson.TypeAdapterFactory factory) {
            ensureBuilder();
            gsonBuilder.registerTypeAdapterFactory(factory);
            return this;
        }

        /**
         * Enables lenient parsing (allows malformed JSON).
         *
         * @return this builder
         */
        @NotNull
        public Builder<T> lenient() {
            ensureBuilder();
            gsonBuilder.setLenient();
            return this;
        }

        /**
         * Excludes fields without @Expose annotation.
         *
         * @return this builder
         */
        @NotNull
        public Builder<T> excludeFieldsWithoutExposeAnnotation() {
            ensureBuilder();
            gsonBuilder.excludeFieldsWithoutExposeAnnotation();
            return this;
        }

        /**
         * Sets a field naming policy.
         *
         * @param policy the naming policy
         * @return this builder
         */
        @NotNull
        public Builder<T> fieldNamingPolicy(@NotNull com.google.gson.FieldNamingPolicy policy) {
            ensureBuilder();
            gsonBuilder.setFieldNamingPolicy(policy);
            return this;
        }

        private void ensureBuilder() {
            if (gson != null) {
                throw new IllegalStateException("Cannot modify builder after gson() was called");
            }
        }

        /**
         * Builds the JSON serializer.
         *
         * @return the configured serializer
         */
        @NotNull
        public JsonRedisSerializer<T> build() {
            Gson effectiveGson = gson != null ? gson : gsonBuilder.create();
            return new JsonRedisSerializer<>(type, effectiveGson);
        }
    }
}
