/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializer for chunk position data.
 *
 * <p>ChunkSerializer handles the serialization of chunk coordinates,
 * including world reference and chunk X/Z positions. Chunks are 16x16
 * block areas identified by their chunk coordinates.
 *
 * <h2>Chunk Properties</h2>
 * <ul>
 *   <li><b>world:</b> The world name (optional for relative references)</li>
 *   <li><b>x:</b> The chunk X coordinate</li>
 *   <li><b>z:</b> The chunk Z coordinate</li>
 * </ul>
 *
 * <h2>Coordinate Conversion</h2>
 * <p>Chunk coordinates are derived from block coordinates:
 * <ul>
 *   <li>chunkX = blockX >> 4 (equivalent to Math.floor(blockX / 16))</li>
 *   <li>chunkZ = blockZ >> 4 (equivalent to Math.floor(blockZ / 16))</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ChunkSerializer serializer = Serializers.chunk();
 *
 * // Create chunk data
 * Map<String, Object> chunk = Map.of(
 *     "world", "world",
 *     "x", 6,
 *     "z", -13
 * );
 *
 * // Serialize
 * String json = serializer.toJson(chunk);
 * String base64 = serializer.toBase64(chunk);
 *
 * // Deserialize
 * Map<String, Object> restored = serializer.fromJson(json);
 *
 * // Convert from block coordinates
 * Map<String, Object> fromBlock = ChunkSerializer.fromBlockCoordinates("world", 100, -200);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LocationSerializer
 * @see BlockDataSerializer
 */
public final class ChunkSerializer implements Serializer<Map<String, Object>> {

    // JSON/map keys
    private static final String KEY_WORLD = "world";
    private static final String KEY_X = "x";
    private static final String KEY_Z = "z";

    /**
     * Creates a new ChunkSerializer.
     */
    public ChunkSerializer() {}

    @Override
    @NotNull
    public String serialize(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return switch (context.getFormat()) {
            case JSON -> serializeToJson(value, context);
            case BASE64 -> Base64.getEncoder().encodeToString(toBytes(value, context));
            case BINARY -> new String(toBytes(value, context), context.getCharset());
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };
    }

    @Override
    @NotNull
    public Map<String, Object> deserialize(@NotNull String data, @NotNull SerializationContext context) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return switch (context.getFormat()) {
            case JSON -> deserializeFromJson(data, context);
            case BASE64 -> fromBytes(Base64.getDecoder().decode(data), context);
            case BINARY -> fromBytes(data.getBytes(context.getCharset()), context);
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };
    }

    @Override
    public byte @NotNull [] toBytes(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.allocate(32);

        // Write world name (optional)
        String world = (String) value.get(KEY_WORLD);
        buffer.writeOptionalString(world);

        // Write chunk coordinates as VarInt for efficiency
        buffer.writeVarInt(getInt(value, KEY_X, 0));
        buffer.writeVarInt(getInt(value, KEY_Z, 0));

        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public Map<String, Object> fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);
        Map<String, Object> result = new LinkedHashMap<>();

        // Read world name
        String world = buffer.readOptionalString();
        if (world != null) {
            result.put(KEY_WORLD, world);
        }

        // Read chunk coordinates
        result.put(KEY_X, buffer.readVarInt());
        result.put(KEY_Z, buffer.readVarInt());

        return result;
    }

    @Override
    @NotNull
    public Class<Map<String, Object>> getTargetType() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> type = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return type;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Creates chunk data from chunk coordinates.
     *
     * @param world the world name
     * @param x     the chunk X coordinate
     * @param z     the chunk Z coordinate
     * @return the chunk data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@Nullable String world, int x, int z) {
        Map<String, Object> chunk = new LinkedHashMap<>();
        if (world != null) {
            chunk.put(KEY_WORLD, world);
        }
        chunk.put(KEY_X, x);
        chunk.put(KEY_Z, z);
        return chunk;
    }

    /**
     * Creates chunk data from block coordinates.
     *
     * @param world  the world name
     * @param blockX the block X coordinate
     * @param blockZ the block Z coordinate
     * @return the chunk data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> fromBlockCoordinates(@Nullable String world, int blockX, int blockZ) {
        return create(world, blockX >> 4, blockZ >> 4);
    }

    /**
     * Creates chunk data from a location map.
     *
     * @param location the location data (must contain x and z)
     * @return the chunk data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> fromLocation(@NotNull Map<String, Object> location) {
        String world = (String) location.get(KEY_WORLD);
        int blockX = (int) Math.floor(getDouble(location, "x", 0));
        int blockZ = (int) Math.floor(getDouble(location, "z", 0));
        return fromBlockCoordinates(world, blockX, blockZ);
    }

    /**
     * Gets the minimum block X coordinate for a chunk.
     *
     * @param chunk the chunk data
     * @return the minimum block X
     * @since 1.0.0
     */
    public static int getMinBlockX(@NotNull Map<String, Object> chunk) {
        return getInt(chunk, KEY_X, 0) << 4;
    }

    /**
     * Gets the minimum block Z coordinate for a chunk.
     *
     * @param chunk the chunk data
     * @return the minimum block Z
     * @since 1.0.0
     */
    public static int getMinBlockZ(@NotNull Map<String, Object> chunk) {
        return getInt(chunk, KEY_Z, 0) << 4;
    }

    /**
     * Gets the maximum block X coordinate for a chunk.
     *
     * @param chunk the chunk data
     * @return the maximum block X
     * @since 1.0.0
     */
    public static int getMaxBlockX(@NotNull Map<String, Object> chunk) {
        return (getInt(chunk, KEY_X, 0) << 4) + 15;
    }

    /**
     * Gets the maximum block Z coordinate for a chunk.
     *
     * @param chunk the chunk data
     * @return the maximum block Z
     * @since 1.0.0
     */
    public static int getMaxBlockZ(@NotNull Map<String, Object> chunk) {
        return (getInt(chunk, KEY_Z, 0) << 4) + 15;
    }

    /**
     * Gets the center location of a chunk at the given Y level.
     *
     * @param chunk the chunk data
     * @param y     the Y coordinate
     * @return the center location data
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> getCenterLocation(@NotNull Map<String, Object> chunk, double y) {
        Map<String, Object> location = new LinkedHashMap<>();
        Object world = chunk.get(KEY_WORLD);
        if (world != null) {
            location.put(KEY_WORLD, world);
        }
        location.put("x", getMinBlockX(chunk) + 8.0);
        location.put("y", y);
        location.put("z", getMinBlockZ(chunk) + 8.0);
        return location;
    }

    /**
     * Checks if a block coordinate is within the given chunk.
     *
     * @param chunk  the chunk data
     * @param blockX the block X coordinate
     * @param blockZ the block Z coordinate
     * @return true if the block is in the chunk
     * @since 1.0.0
     */
    public static boolean containsBlock(@NotNull Map<String, Object> chunk, int blockX, int blockZ) {
        int chunkX = getInt(chunk, KEY_X, 0);
        int chunkZ = getInt(chunk, KEY_Z, 0);
        return (blockX >> 4) == chunkX && (blockZ >> 4) == chunkZ;
    }

    /**
     * Checks if two chunk coordinates represent the same chunk.
     *
     * @param chunk1 the first chunk data
     * @param chunk2 the second chunk data
     * @return true if same chunk (ignoring world)
     * @since 1.0.0
     */
    public static boolean sameChunk(@NotNull Map<String, Object> chunk1, @NotNull Map<String, Object> chunk2) {
        return getInt(chunk1, KEY_X, 0) == getInt(chunk2, KEY_X, 0) &&
               getInt(chunk1, KEY_Z, 0) == getInt(chunk2, KEY_Z, 0);
    }

    /**
     * Checks if two chunks are in the same world.
     *
     * @param chunk1 the first chunk data
     * @param chunk2 the second chunk data
     * @return true if same world
     * @since 1.0.0
     */
    public static boolean sameWorld(@NotNull Map<String, Object> chunk1, @NotNull Map<String, Object> chunk2) {
        return Objects.equals(chunk1.get(KEY_WORLD), chunk2.get(KEY_WORLD));
    }

    /**
     * Calculates the chunk distance (Chebyshev distance) between two chunks.
     *
     * @param chunk1 the first chunk data
     * @param chunk2 the second chunk data
     * @return the chunk distance
     * @since 1.0.0
     */
    public static int chunkDistance(@NotNull Map<String, Object> chunk1, @NotNull Map<String, Object> chunk2) {
        int dx = Math.abs(getInt(chunk1, KEY_X, 0) - getInt(chunk2, KEY_X, 0));
        int dz = Math.abs(getInt(chunk1, KEY_Z, 0) - getInt(chunk2, KEY_Z, 0));
        return Math.max(dx, dz);
    }

    /**
     * Creates a unique key string for a chunk.
     *
     * @param chunk the chunk data
     * @return a unique key string (world:x:z)
     * @since 1.0.0
     */
    @NotNull
    public static String toKey(@NotNull Map<String, Object> chunk) {
        String world = (String) chunk.get(KEY_WORLD);
        int x = getInt(chunk, KEY_X, 0);
        int z = getInt(chunk, KEY_Z, 0);
        return (world != null ? world : "null") + ":" + x + ":" + z;
    }

    /**
     * Parses a chunk key string.
     *
     * @param key the key string (world:x:z)
     * @return the chunk data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> fromKey(@NotNull String key) {
        String[] parts = key.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid chunk key format: " + key);
        }
        String world = "null".equals(parts[0]) ? null : parts[0];
        return create(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    /**
     * Converts a packed long to chunk data.
     *
     * <p>Packed format: high 32 bits = X, low 32 bits = Z
     *
     * @param world  the world name
     * @param packed the packed chunk coordinates
     * @return the chunk data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> fromPackedLong(@Nullable String world, long packed) {
        int x = (int) (packed >> 32);
        int z = (int) packed;
        return create(world, x, z);
    }

    /**
     * Packs chunk coordinates into a long.
     *
     * @param chunk the chunk data
     * @return the packed long (high 32 bits = X, low 32 bits = Z)
     * @since 1.0.0
     */
    public static long toPackedLong(@NotNull Map<String, Object> chunk) {
        long x = getInt(chunk, KEY_X, 0);
        long z = getInt(chunk, KEY_Z, 0);
        return (x << 32) | (z & 0xFFFFFFFFL);
    }

    // ========================================
    // JSON Serialization
    // ========================================

    @NotNull
    private String serializeToJson(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        String world = (String) value.get(KEY_WORLD);
        if (world != null) {
            json.append("\"world\":\"").append(world).append("\"");
            first = false;
        }

        if (!first) json.append(",");
        json.append("\"x\":").append(getInt(value, KEY_X, 0));
        json.append(",\"z\":").append(getInt(value, KEY_Z, 0));

        json.append("}");
        return json.toString();
    }

    @NotNull
    private Map<String, Object> deserializeFromJson(@NotNull String json, @NotNull SerializationContext context) {
        Map<String, Object> result = new LinkedHashMap<>();

        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }

        for (String part : json.split(",")) {
            part = part.trim();
            int colonIndex = part.indexOf(":");
            if (colonIndex > 0) {
                String key = part.substring(0, colonIndex).trim().replace("\"", "");
                String valueStr = part.substring(colonIndex + 1).trim().replace("\"", "");

                switch (key) {
                    case KEY_WORLD -> result.put(KEY_WORLD, valueStr);
                    case KEY_X, KEY_Z -> result.put(key, Integer.parseInt(valueStr));
                }
            }
        }

        return result;
    }

    // ========================================
    // Helper Methods
    // ========================================

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.intValue();
        }
        return defaultValue;
    }

    private static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return defaultValue;
    }
}
