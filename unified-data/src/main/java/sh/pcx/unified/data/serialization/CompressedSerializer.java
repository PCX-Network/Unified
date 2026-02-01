/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Serializer wrapper that adds compression to binary data.
 *
 * <p>CompressedSerializer wraps another serializer and applies compression
 * to the output. Supports GZIP and LZ4 compression algorithms.
 *
 * <h2>Compression Types</h2>
 * <ul>
 *   <li><b>GZIP:</b> Good compression ratio, widely compatible</li>
 *   <li><b>LZ4:</b> Fast compression/decompression, moderate ratio (simulated with DEFLATE)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Wrap an existing serializer with GZIP compression
 * Serializer<LargeData> compressed = CompressedSerializer.gzip(new LargeDataSerializer());
 *
 * // Use with compressed context
 * SerializationContext context = SerializationContext.compressedBinary(CompressionType.GZIP);
 * String encoded = compressed.serialize(data, context);
 *
 * // Auto-detect compression on read
 * LargeData restored = compressed.deserialize(encoded, context);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>CompressedSerializer is thread-safe if the wrapped serializer is thread-safe.
 *
 * @param <T> the type of object this serializer handles
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BinarySerializer
 * @see SerializationContext.CompressionType
 */
public final class CompressedSerializer<T> implements Serializer<T> {

    private static final byte[] GZIP_MAGIC = { 0x1F, (byte) 0x8B };
    private static final int COMPRESSION_THRESHOLD = 256; // Minimum size before compression

    private final Serializer<T> delegate;
    private final SerializationContext.CompressionType compressionType;
    private final int compressionLevel;
    private final int threshold;

    private CompressedSerializer(Serializer<T> delegate, SerializationContext.CompressionType compressionType,
                                 int compressionLevel, int threshold) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.compressionType = Objects.requireNonNull(compressionType, "compressionType cannot be null");
        this.compressionLevel = compressionLevel;
        this.threshold = threshold;
    }

    /**
     * Creates a GZIP-compressed serializer.
     *
     * @param delegate the serializer to wrap
     * @param <T>      the type
     * @return a compressed serializer
     * @since 1.0.0
     */
    @NotNull
    public static <T> CompressedSerializer<T> gzip(@NotNull Serializer<T> delegate) {
        return new CompressedSerializer<>(delegate, SerializationContext.CompressionType.GZIP,
                Deflater.DEFAULT_COMPRESSION, COMPRESSION_THRESHOLD);
    }

    /**
     * Creates an LZ4-style compressed serializer (using fast DEFLATE).
     *
     * @param delegate the serializer to wrap
     * @param <T>      the type
     * @return a compressed serializer
     * @since 1.0.0
     */
    @NotNull
    public static <T> CompressedSerializer<T> lz4(@NotNull Serializer<T> delegate) {
        return new CompressedSerializer<>(delegate, SerializationContext.CompressionType.LZ4,
                Deflater.BEST_SPEED, COMPRESSION_THRESHOLD);
    }

    /**
     * Creates a compressed serializer with custom settings.
     *
     * @param delegate    the serializer to wrap
     * @param compression the compression type
     * @param level       the compression level (1-9 for GZIP)
     * @param threshold   minimum size for compression (smaller data is stored uncompressed)
     * @param <T>         the type
     * @return a compressed serializer
     * @since 1.0.0
     */
    @NotNull
    public static <T> CompressedSerializer<T> create(@NotNull Serializer<T> delegate,
                                                      @NotNull SerializationContext.CompressionType compression,
                                                      int level, int threshold) {
        return new CompressedSerializer<>(delegate, compression, level, threshold);
    }

    /**
     * Creates a builder for configuring a compressed serializer.
     *
     * @param delegate the serializer to wrap
     * @param <T>      the type
     * @return a builder
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder(@NotNull Serializer<T> delegate) {
        return new Builder<>(delegate);
    }

    @Override
    @NotNull
    public String serialize(@NotNull T value, @NotNull SerializationContext context) {
        byte[] bytes = toBytes(value, context);

        return switch (context.getFormat()) {
            case BINARY -> new String(bytes, context.getCharset());
            case BASE64 -> Base64.getEncoder().encodeToString(bytes);
            default -> delegate.serialize(value, context);
        };
    }

    @Override
    @NotNull
    public T deserialize(@NotNull String data, @NotNull SerializationContext context) {
        return switch (context.getFormat()) {
            case BINARY -> fromBytes(data.getBytes(context.getCharset()), context);
            case BASE64 -> fromBytes(Base64.getDecoder().decode(data), context);
            default -> delegate.deserialize(data, context);
        };
    }

    @Override
    public byte @NotNull [] toBytes(@NotNull T value, @NotNull SerializationContext context) {
        // Get uncompressed data from delegate
        byte[] uncompressed = delegate.toBytes(value, context);

        // Skip compression for small data
        if (uncompressed.length < threshold) {
            return addHeader(uncompressed, false);
        }

        // Compress data
        byte[] compressed = compress(uncompressed);

        // Use compressed only if it's actually smaller
        if (compressed.length < uncompressed.length) {
            return addHeader(compressed, true);
        }

        return addHeader(uncompressed, false);
    }

    @Override
    @NotNull
    public T fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        if (data.length < 2) {
            throw new SerializationException("Data too short for compressed format");
        }

        boolean isCompressed = readHeader(data);
        byte[] payload = extractPayload(data);

        if (isCompressed) {
            payload = decompress(payload);
        }

        return delegate.fromBytes(payload, context);
    }

    @Override
    @NotNull
    public Class<T> getTargetType() {
        return delegate.getTargetType();
    }

    @Override
    public boolean supportsFormat(@NotNull SerializationContext.Format format) {
        return format == SerializationContext.Format.BINARY ||
               format == SerializationContext.Format.BASE64 ||
               delegate.supportsFormat(format);
    }

    /**
     * Returns the wrapped delegate serializer.
     *
     * @return the delegate
     * @since 1.0.0
     */
    @NotNull
    public Serializer<T> getDelegate() {
        return delegate;
    }

    /**
     * Returns the compression type.
     *
     * @return the compression type
     * @since 1.0.0
     */
    @NotNull
    public SerializationContext.CompressionType getCompressionType() {
        return compressionType;
    }

    // ========================================
    // Internal Methods
    // ========================================

    private byte[] compress(byte[] data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);

            switch (compressionType) {
                case GZIP -> {
                    try (GZIPOutputStream gzip = new GZIPOutputStream(baos) {
                        { def.setLevel(compressionLevel); }
                    }) {
                        gzip.write(data);
                    }
                }
                case LZ4 -> {
                    // Use DEFLATE with fast settings as LZ4 approximation
                    Deflater deflater = new Deflater(compressionLevel);
                    try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater)) {
                        dos.write(data);
                    }
                }
                default -> {
                    return data;
                }
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Compression failed", e);
        }
    }

    private byte[] decompress(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length * 2);

            // Auto-detect compression type from data
            boolean isGzip = data.length >= 2 &&
                    (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B;

            try (var input = isGzip ? new GZIPInputStream(bais) : new InflaterInputStream(bais)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Decompression failed", e);
        }
    }

    private byte[] addHeader(byte[] data, boolean compressed) {
        byte[] result = new byte[data.length + 1];
        result[0] = (byte) (compressed ? 1 : 0);
        System.arraycopy(data, 0, result, 1, data.length);
        return result;
    }

    private boolean readHeader(byte[] data) {
        return data[0] == 1;
    }

    private byte[] extractPayload(byte[] data) {
        byte[] payload = new byte[data.length - 1];
        System.arraycopy(data, 1, payload, 0, payload.length);
        return payload;
    }

    /**
     * Builder for creating {@link CompressedSerializer} instances.
     *
     * @param <T> the type
     * @since 1.0.0
     */
    public static final class Builder<T> {
        private final Serializer<T> delegate;
        private SerializationContext.CompressionType compressionType = SerializationContext.CompressionType.GZIP;
        private int compressionLevel = Deflater.DEFAULT_COMPRESSION;
        private int threshold = COMPRESSION_THRESHOLD;

        private Builder(Serializer<T> delegate) {
            this.delegate = delegate;
        }

        /**
         * Sets the compression type.
         *
         * @param type the compression type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> compression(@NotNull SerializationContext.CompressionType type) {
            this.compressionType = type;
            return this;
        }

        /**
         * Sets the compression level (1-9, or -1 for default).
         *
         * @param level the compression level
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> level(int level) {
            this.compressionLevel = level;
            return this;
        }

        /**
         * Sets the minimum size for compression.
         *
         * @param threshold the threshold in bytes
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> threshold(int threshold) {
            this.threshold = threshold;
            return this;
        }

        /**
         * Uses best speed compression settings.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> bestSpeed() {
            this.compressionLevel = Deflater.BEST_SPEED;
            return this;
        }

        /**
         * Uses best compression ratio settings.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> bestCompression() {
            this.compressionLevel = Deflater.BEST_COMPRESSION;
            return this;
        }

        /**
         * Builds the CompressedSerializer.
         *
         * @return the compressed serializer
         * @since 1.0.0
         */
        @NotNull
        public CompressedSerializer<T> build() {
            return new CompressedSerializer<>(delegate, compressionType, compressionLevel, threshold);
        }
    }
}
