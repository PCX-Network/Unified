/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Context object that provides configuration for serialization operations.
 *
 * <p>A SerializationContext carries information about the serialization format,
 * version, compression settings, and other options that affect how objects
 * are serialized and deserialized.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a context for JSON serialization
 * SerializationContext context = SerializationContext.builder()
 *     .format(SerializationFormat.JSON)
 *     .version(SchemaVersion.of(1, 0))
 *     .prettyPrint(true)
 *     .build();
 *
 * // Serialize with context
 * String json = serializer.serialize(item, context);
 *
 * // Create a context for binary serialization with compression
 * SerializationContext binaryContext = SerializationContext.builder()
 *     .format(SerializationFormat.BINARY)
 *     .compression(CompressionType.GZIP)
 *     .version(SchemaVersion.of(2, 0))
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>SerializationContext instances are immutable and therefore thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Serializer
 * @see Serializers
 */
public final class SerializationContext {

    /**
     * Supported serialization formats.
     *
     * @since 1.0.0
     */
    public enum Format {
        /**
         * JSON format - human-readable, moderate size.
         */
        JSON,

        /**
         * Base64 encoded binary - for text storage of binary data.
         */
        BASE64,

        /**
         * Raw binary format - most compact, fastest.
         */
        BINARY,

        /**
         * MiniMessage format - for Adventure text components.
         */
        MINIMESSAGE,

        /**
         * Legacy text format - for legacy color codes.
         */
        LEGACY_TEXT,

        /**
         * NBT format - for Minecraft NBT data.
         */
        NBT
    }

    /**
     * Supported compression types.
     *
     * @since 1.0.0
     */
    public enum CompressionType {
        /**
         * No compression.
         */
        NONE,

        /**
         * GZIP compression - good compression ratio.
         */
        GZIP,

        /**
         * LZ4 compression - fast compression/decompression.
         */
        LZ4
    }

    private static final SerializationContext DEFAULT = builder().build();

    private final Format format;
    private final CompressionType compression;
    private final SchemaVersion version;
    private final Charset charset;
    private final boolean prettyPrint;
    private final boolean includeNulls;
    private final boolean preserveOrder;
    private final boolean lenientParsing;
    private final Map<String, Object> properties;

    private SerializationContext(Builder builder) {
        this.format = builder.format;
        this.compression = builder.compression;
        this.version = builder.version;
        this.charset = builder.charset;
        this.prettyPrint = builder.prettyPrint;
        this.includeNulls = builder.includeNulls;
        this.preserveOrder = builder.preserveOrder;
        this.lenientParsing = builder.lenientParsing;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
    }

    /**
     * Returns the default serialization context.
     *
     * <p>The default context uses JSON format, no compression, and UTF-8 charset.
     *
     * @return the default context
     * @since 1.0.0
     */
    @NotNull
    public static SerializationContext getDefault() {
        return DEFAULT;
    }

    /**
     * Creates a new builder for constructing SerializationContext instances.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a context for JSON serialization.
     *
     * @return a JSON serialization context
     * @since 1.0.0
     */
    @NotNull
    public static SerializationContext json() {
        return builder().format(Format.JSON).build();
    }

    /**
     * Creates a context for pretty-printed JSON serialization.
     *
     * @return a pretty JSON serialization context
     * @since 1.0.0
     */
    @NotNull
    public static SerializationContext prettyJson() {
        return builder().format(Format.JSON).prettyPrint(true).build();
    }

    /**
     * Creates a context for Base64 serialization.
     *
     * @return a Base64 serialization context
     * @since 1.0.0
     */
    @NotNull
    public static SerializationContext base64() {
        return builder().format(Format.BASE64).build();
    }

    /**
     * Creates a context for binary serialization.
     *
     * @return a binary serialization context
     * @since 1.0.0
     */
    @NotNull
    public static SerializationContext binary() {
        return builder().format(Format.BINARY).build();
    }

    /**
     * Creates a context for compressed binary serialization.
     *
     * @param compressionType the compression type to use
     * @return a compressed binary serialization context
     * @since 1.0.0
     */
    @NotNull
    public static SerializationContext compressedBinary(@NotNull CompressionType compressionType) {
        return builder()
                .format(Format.BINARY)
                .compression(compressionType)
                .build();
    }

    /**
     * Returns the serialization format.
     *
     * @return the format
     * @since 1.0.0
     */
    @NotNull
    public Format getFormat() {
        return format;
    }

    /**
     * Returns the compression type.
     *
     * @return the compression type
     * @since 1.0.0
     */
    @NotNull
    public CompressionType getCompression() {
        return compression;
    }

    /**
     * Returns the schema version.
     *
     * @return the schema version
     * @since 1.0.0
     */
    @NotNull
    public SchemaVersion getVersion() {
        return version;
    }

    /**
     * Returns the character set used for string encoding.
     *
     * @return the charset
     * @since 1.0.0
     */
    @NotNull
    public Charset getCharset() {
        return charset;
    }

    /**
     * Returns whether to format output for readability.
     *
     * @return true if pretty printing is enabled
     * @since 1.0.0
     */
    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    /**
     * Returns whether to include null values in output.
     *
     * @return true if nulls should be included
     * @since 1.0.0
     */
    public boolean isIncludeNulls() {
        return includeNulls;
    }

    /**
     * Returns whether to preserve field order in maps/objects.
     *
     * @return true if order should be preserved
     * @since 1.0.0
     */
    public boolean isPreserveOrder() {
        return preserveOrder;
    }

    /**
     * Returns whether to use lenient parsing for deserialization.
     *
     * @return true if lenient parsing is enabled
     * @since 1.0.0
     */
    public boolean isLenientParsing() {
        return lenientParsing;
    }

    /**
     * Returns whether compression is enabled.
     *
     * @return true if compression is enabled
     * @since 1.0.0
     */
    public boolean isCompressed() {
        return compression != CompressionType.NONE;
    }

    /**
     * Gets a custom property value.
     *
     * @param key the property key
     * @param <T> the expected value type
     * @return an Optional containing the property value if present
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(@NotNull String key) {
        return Optional.ofNullable((T) properties.get(key));
    }

    /**
     * Gets a custom property value with a default.
     *
     * @param key          the property key
     * @param defaultValue the default value if not present
     * @param <T>          the expected value type
     * @return the property value or the default
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getProperty(@NotNull String key, @NotNull T defaultValue) {
        Object value = properties.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Returns all custom properties.
     *
     * @return an unmodifiable map of properties
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Creates a new context with a different format.
     *
     * @param format the new format
     * @return a new context with the specified format
     * @since 1.0.0
     */
    @NotNull
    public SerializationContext withFormat(@NotNull Format format) {
        return toBuilder().format(format).build();
    }

    /**
     * Creates a new context with a different compression type.
     *
     * @param compression the new compression type
     * @return a new context with the specified compression
     * @since 1.0.0
     */
    @NotNull
    public SerializationContext withCompression(@NotNull CompressionType compression) {
        return toBuilder().compression(compression).build();
    }

    /**
     * Creates a new context with a different version.
     *
     * @param version the new version
     * @return a new context with the specified version
     * @since 1.0.0
     */
    @NotNull
    public SerializationContext withVersion(@NotNull SchemaVersion version) {
        return toBuilder().version(version).build();
    }

    /**
     * Creates a builder pre-populated with this context's values.
     *
     * @return a new builder with this context's values
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .format(format)
                .compression(compression)
                .version(version)
                .charset(charset)
                .prettyPrint(prettyPrint)
                .includeNulls(includeNulls)
                .preserveOrder(preserveOrder)
                .lenientParsing(lenientParsing)
                .properties(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializationContext that = (SerializationContext) o;
        return prettyPrint == that.prettyPrint &&
                includeNulls == that.includeNulls &&
                preserveOrder == that.preserveOrder &&
                lenientParsing == that.lenientParsing &&
                format == that.format &&
                compression == that.compression &&
                Objects.equals(version, that.version) &&
                Objects.equals(charset, that.charset) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, compression, version, charset, prettyPrint,
                includeNulls, preserveOrder, lenientParsing, properties);
    }

    @Override
    public String toString() {
        return "SerializationContext{" +
                "format=" + format +
                ", compression=" + compression +
                ", version=" + version +
                ", charset=" + charset +
                ", prettyPrint=" + prettyPrint +
                ", lenientParsing=" + lenientParsing +
                '}';
    }

    /**
     * Builder for creating {@link SerializationContext} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private Format format = Format.JSON;
        private CompressionType compression = CompressionType.NONE;
        private SchemaVersion version = SchemaVersion.current();
        private Charset charset = StandardCharsets.UTF_8;
        private boolean prettyPrint = false;
        private boolean includeNulls = false;
        private boolean preserveOrder = true;
        private boolean lenientParsing = false;
        private Map<String, Object> properties = new HashMap<>();

        private Builder() {}

        /**
         * Sets the serialization format.
         *
         * @param format the format
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder format(@NotNull Format format) {
            this.format = Objects.requireNonNull(format, "format cannot be null");
            return this;
        }

        /**
         * Sets the compression type.
         *
         * @param compression the compression type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder compression(@NotNull CompressionType compression) {
            this.compression = Objects.requireNonNull(compression, "compression cannot be null");
            return this;
        }

        /**
         * Sets the schema version.
         *
         * @param version the version
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder version(@NotNull SchemaVersion version) {
            this.version = Objects.requireNonNull(version, "version cannot be null");
            return this;
        }

        /**
         * Sets the character set.
         *
         * @param charset the charset
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder charset(@NotNull Charset charset) {
            this.charset = Objects.requireNonNull(charset, "charset cannot be null");
            return this;
        }

        /**
         * Sets whether to format output for readability.
         *
         * @param prettyPrint true to enable pretty printing
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        /**
         * Sets whether to include null values in output.
         *
         * @param includeNulls true to include nulls
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder includeNulls(boolean includeNulls) {
            this.includeNulls = includeNulls;
            return this;
        }

        /**
         * Sets whether to preserve field order.
         *
         * @param preserveOrder true to preserve order
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder preserveOrder(boolean preserveOrder) {
            this.preserveOrder = preserveOrder;
            return this;
        }

        /**
         * Sets whether to use lenient parsing.
         *
         * @param lenientParsing true to enable lenient parsing
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder lenientParsing(boolean lenientParsing) {
            this.lenientParsing = lenientParsing;
            return this;
        }

        /**
         * Sets a custom property.
         *
         * @param key   the property key
         * @param value the property value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder property(@NotNull String key, @Nullable Object value) {
            if (value == null) {
                this.properties.remove(key);
            } else {
                this.properties.put(key, value);
            }
            return this;
        }

        /**
         * Sets all custom properties from a map.
         *
         * @param properties the properties map
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder properties(@NotNull Map<String, Object> properties) {
            this.properties = new HashMap<>(properties);
            return this;
        }

        /**
         * Builds the SerializationContext instance.
         *
         * @return a new SerializationContext
         * @since 1.0.0
         */
        @NotNull
        public SerializationContext build() {
            return new SerializationContext(this);
        }
    }
}
