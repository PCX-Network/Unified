/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.serialization;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of supported inventory serialization formats.
 *
 * <p>Each format has different characteristics suited for different use cases:
 *
 * <h2>Format Comparison</h2>
 * <table border="1">
 *   <tr><th>Format</th><th>Size</th><th>Speed</th><th>Readable</th><th>Use Case</th></tr>
 *   <tr><td>BINARY</td><td>Smallest</td><td>Fastest</td><td>No</td><td>Network transfer, performance</td></tr>
 *   <tr><td>BASE64</td><td>~33% larger</td><td>Fast</td><td>No</td><td>Database storage, configs</td></tr>
 *   <tr><td>JSON</td><td>Large</td><td>Slower</td><td>Yes</td><td>Debugging, APIs, exports</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Serialize with specific format
 * byte[] data = serializer.serialize(snapshot, SerializationFormat.BINARY);
 * String base64 = serializer.serialize(snapshot, SerializationFormat.BASE64);
 * String json = serializer.serialize(snapshot, SerializationFormat.JSON);
 *
 * // Deserialize
 * InventorySnapshot fromBytes = serializer.deserialize(data, SerializationFormat.BINARY);
 * InventorySnapshot fromBase64 = serializer.deserialize(base64, SerializationFormat.BASE64);
 * InventorySnapshot fromJson = serializer.deserialize(json, SerializationFormat.JSON);
 *
 * // Check format properties
 * if (format.isHumanReadable()) {
 *     // Can be displayed in logs
 * }
 * if (format.supportsBinary()) {
 *     // Can be stored in BLOB columns
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ItemSerializer
 * @see InventorySerializer
 */
public enum SerializationFormat {

    /**
     * Base64-encoded binary format.
     *
     * <p>The default format for most use cases. Produces a text string that
     * can be stored in any database column or configuration file. About 33%
     * larger than raw binary but widely compatible.
     */
    BASE64("base64", "application/base64", true, false),

    /**
     * Human-readable JSON format.
     *
     * <p>Produces a structured JSON object that can be inspected and edited
     * manually. Best for debugging, API responses, and data exports. Larger
     * and slower than binary formats.
     */
    JSON("json", "application/json", true, true),

    /**
     * Compact binary format.
     *
     * <p>The most efficient format in terms of size and speed. Produces raw
     * bytes that must be stored in binary-capable storage (BLOB columns, files).
     * Best for performance-critical operations and network transfer.
     */
    BINARY("binary", "application/octet-stream", false, false),

    /**
     * Compressed binary format.
     *
     * <p>Binary format with GZIP compression. Smaller than regular binary
     * for large inventories, but requires more CPU for compression/decompression.
     * Best for long-term storage of large snapshots.
     */
    COMPRESSED("compressed", "application/gzip", false, false),

    /**
     * NBT (Named Binary Tag) format.
     *
     * <p>Uses Minecraft's native NBT format. Maximum compatibility with
     * Minecraft internals and other plugins that use NBT. Size is similar
     * to regular binary.
     */
    NBT("nbt", "application/x-nbt", false, false);

    private final String name;
    private final String mimeType;
    private final boolean textBased;
    private final boolean humanReadable;

    SerializationFormat(String name, String mimeType, boolean textBased, boolean humanReadable) {
        this.name = name;
        this.mimeType = mimeType;
        this.textBased = textBased;
        this.humanReadable = humanReadable;
    }

    /**
     * Returns the format name.
     *
     * @return the format name
     * @since 1.0.0
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the MIME type for this format.
     *
     * @return the MIME type
     * @since 1.0.0
     */
    @NotNull
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns whether this format produces text output.
     *
     * <p>Text-based formats can be stored in VARCHAR/TEXT database columns
     * and configuration files.
     *
     * @return true if text-based
     * @since 1.0.0
     */
    public boolean isTextBased() {
        return textBased;
    }

    /**
     * Returns whether this format is human-readable.
     *
     * <p>Human-readable formats can be inspected and edited manually.
     *
     * @return true if human-readable
     * @since 1.0.0
     */
    public boolean isHumanReadable() {
        return humanReadable;
    }

    /**
     * Returns whether this format produces binary output.
     *
     * @return true if binary
     * @since 1.0.0
     */
    public boolean isBinary() {
        return !textBased;
    }

    /**
     * Returns whether this format uses compression.
     *
     * @return true if compressed
     * @since 1.0.0
     */
    public boolean isCompressed() {
        return this == COMPRESSED;
    }

    /**
     * Returns the default file extension for this format.
     *
     * @return the file extension (without dot)
     * @since 1.0.0
     */
    @NotNull
    public String getFileExtension() {
        return switch (this) {
            case BASE64 -> "b64";
            case JSON -> "json";
            case BINARY -> "bin";
            case COMPRESSED -> "gz";
            case NBT -> "nbt";
        };
    }

    /**
     * Returns the default format.
     *
     * @return {@link #BASE64}
     * @since 1.0.0
     */
    @NotNull
    public static SerializationFormat defaultFormat() {
        return BASE64;
    }

    /**
     * Returns the most compact format.
     *
     * @return {@link #COMPRESSED}
     * @since 1.0.0
     */
    @NotNull
    public static SerializationFormat compactFormat() {
        return COMPRESSED;
    }

    /**
     * Returns the most readable format.
     *
     * @return {@link #JSON}
     * @since 1.0.0
     */
    @NotNull
    public static SerializationFormat readableFormat() {
        return JSON;
    }

    /**
     * Parses a format from its name.
     *
     * @param name the format name
     * @return the format, or null if not found
     * @since 1.0.0
     */
    @NotNull
    public static SerializationFormat fromName(@NotNull String name) {
        for (SerializationFormat format : values()) {
            if (format.name.equalsIgnoreCase(name) || format.name().equalsIgnoreCase(name)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown format: " + name);
    }

    /**
     * Detects the format from data content.
     *
     * @param data the data to analyze
     * @return the detected format, or BASE64 as default
     * @since 1.0.0
     */
    @NotNull
    public static SerializationFormat detect(byte @NotNull [] data) {
        if (data.length < 2) {
            return BASE64;
        }

        // Check for GZIP magic number
        if (data[0] == (byte) 0x1f && data[1] == (byte) 0x8b) {
            return COMPRESSED;
        }

        // Check for NBT compound tag start
        if (data[0] == 0x0a) {
            return NBT;
        }

        // Check for JSON (starts with { or [)
        if (data[0] == '{' || data[0] == '[') {
            return JSON;
        }

        return BINARY;
    }

    /**
     * Detects the format from a string.
     *
     * @param data the data string
     * @return the detected format
     * @since 1.0.0
     */
    @NotNull
    public static SerializationFormat detect(@NotNull String data) {
        if (data.isEmpty()) {
            return BASE64;
        }

        char first = data.charAt(0);
        if (first == '{' || first == '[') {
            return JSON;
        }

        return BASE64;
    }

    @Override
    public String toString() {
        return name;
    }
}
