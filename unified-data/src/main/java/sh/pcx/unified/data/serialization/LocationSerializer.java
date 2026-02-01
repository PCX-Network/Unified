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
 * Serializer for location data with world reference.
 *
 * <p>LocationSerializer handles the serialization of location data, including
 * world name, coordinates (x, y, z), and rotation (yaw, pitch). Locations
 * can be serialized with or without rotation data.
 *
 * <h2>Location Properties</h2>
 * <ul>
 *   <li><b>world:</b> The world name (e.g., "world", "world_nether")</li>
 *   <li><b>x:</b> The X coordinate (double)</li>
 *   <li><b>y:</b> The Y coordinate (double)</li>
 *   <li><b>z:</b> The Z coordinate (double)</li>
 *   <li><b>yaw:</b> The yaw rotation (float, -180 to 180)</li>
 *   <li><b>pitch:</b> The pitch rotation (float, -90 to 90)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LocationSerializer serializer = Serializers.location();
 *
 * // Create location data
 * Map<String, Object> location = Map.of(
 *     "world", "world",
 *     "x", 100.5,
 *     "y", 64.0,
 *     "z", -200.5,
 *     "yaw", 90.0f,
 *     "pitch", 0.0f
 * );
 *
 * // Serialize
 * String json = serializer.toJson(location);
 * String base64 = serializer.toBase64(location);
 *
 * // Deserialize
 * Map<String, Object> restored = serializer.fromJson(json);
 *
 * // Block location (integer coordinates)
 * Map<String, Object> block = serializer.toBlockLocation(location);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BlockDataSerializer
 * @see ChunkSerializer
 */
public final class LocationSerializer implements Serializer<Map<String, Object>> {

    // JSON/map keys
    private static final String KEY_WORLD = "world";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";
    private static final String KEY_YAW = "yaw";
    private static final String KEY_PITCH = "pitch";

    /**
     * Creates a new LocationSerializer.
     */
    public LocationSerializer() {}

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
        BinaryBuffer buffer = BinaryBuffer.allocate(64);

        // Write world name (optional)
        String world = (String) value.get(KEY_WORLD);
        buffer.writeOptionalString(world);

        // Write coordinates
        buffer.writeDouble(getDouble(value, KEY_X, 0));
        buffer.writeDouble(getDouble(value, KEY_Y, 0));
        buffer.writeDouble(getDouble(value, KEY_Z, 0));

        // Write rotation (check if present)
        boolean hasRotation = value.containsKey(KEY_YAW) || value.containsKey(KEY_PITCH);
        buffer.writeBoolean(hasRotation);
        if (hasRotation) {
            buffer.writeFloat(getFloat(value, KEY_YAW, 0));
            buffer.writeFloat(getFloat(value, KEY_PITCH, 0));
        }

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

        // Read coordinates
        result.put(KEY_X, buffer.readDouble());
        result.put(KEY_Y, buffer.readDouble());
        result.put(KEY_Z, buffer.readDouble());

        // Read rotation if present
        if (buffer.readBoolean()) {
            result.put(KEY_YAW, buffer.readFloat());
            result.put(KEY_PITCH, buffer.readFloat());
        }

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
     * Creates a location data map with coordinates only.
     *
     * @param world the world name
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     * @return the location data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@Nullable String world, double x, double y, double z) {
        Map<String, Object> location = new LinkedHashMap<>();
        if (world != null) {
            location.put(KEY_WORLD, world);
        }
        location.put(KEY_X, x);
        location.put(KEY_Y, y);
        location.put(KEY_Z, z);
        return location;
    }

    /**
     * Creates a location data map with coordinates and rotation.
     *
     * @param world the world name
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     * @param yaw   the yaw rotation
     * @param pitch the pitch rotation
     * @return the location data map
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> create(@Nullable String world, double x, double y, double z,
                                              float yaw, float pitch) {
        Map<String, Object> location = create(world, x, y, z);
        location.put(KEY_YAW, yaw);
        location.put(KEY_PITCH, pitch);
        return location;
    }

    /**
     * Converts a location to block coordinates (integer).
     *
     * @param location the location data
     * @return a new location with floored coordinates
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> toBlockLocation(@NotNull Map<String, Object> location) {
        Map<String, Object> block = new LinkedHashMap<>();
        Object world = location.get(KEY_WORLD);
        if (world != null) {
            block.put(KEY_WORLD, world);
        }
        block.put(KEY_X, (int) Math.floor(getDouble(location, KEY_X, 0)));
        block.put(KEY_Y, (int) Math.floor(getDouble(location, KEY_Y, 0)));
        block.put(KEY_Z, (int) Math.floor(getDouble(location, KEY_Z, 0)));
        return block;
    }

    /**
     * Converts a location to the center of its block.
     *
     * @param location the location data
     * @return a new location at the block center
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> toBlockCenter(@NotNull Map<String, Object> location) {
        Map<String, Object> center = new LinkedHashMap<>();
        Object world = location.get(KEY_WORLD);
        if (world != null) {
            center.put(KEY_WORLD, world);
        }
        center.put(KEY_X, Math.floor(getDouble(location, KEY_X, 0)) + 0.5);
        center.put(KEY_Y, Math.floor(getDouble(location, KEY_Y, 0)) + 0.5);
        center.put(KEY_Z, Math.floor(getDouble(location, KEY_Z, 0)) + 0.5);
        if (location.containsKey(KEY_YAW)) {
            center.put(KEY_YAW, location.get(KEY_YAW));
        }
        if (location.containsKey(KEY_PITCH)) {
            center.put(KEY_PITCH, location.get(KEY_PITCH));
        }
        return center;
    }

    /**
     * Calculates the distance between two locations.
     *
     * @param loc1 the first location
     * @param loc2 the second location
     * @return the distance
     * @since 1.0.0
     */
    public static double distance(@NotNull Map<String, Object> loc1, @NotNull Map<String, Object> loc2) {
        double dx = getDouble(loc1, KEY_X, 0) - getDouble(loc2, KEY_X, 0);
        double dy = getDouble(loc1, KEY_Y, 0) - getDouble(loc2, KEY_Y, 0);
        double dz = getDouble(loc1, KEY_Z, 0) - getDouble(loc2, KEY_Z, 0);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates the squared distance between two locations.
     *
     * @param loc1 the first location
     * @param loc2 the second location
     * @return the squared distance
     * @since 1.0.0
     */
    public static double distanceSquared(@NotNull Map<String, Object> loc1, @NotNull Map<String, Object> loc2) {
        double dx = getDouble(loc1, KEY_X, 0) - getDouble(loc2, KEY_X, 0);
        double dy = getDouble(loc1, KEY_Y, 0) - getDouble(loc2, KEY_Y, 0);
        double dz = getDouble(loc1, KEY_Z, 0) - getDouble(loc2, KEY_Z, 0);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Checks if two locations are in the same world.
     *
     * @param loc1 the first location
     * @param loc2 the second location
     * @return true if same world
     * @since 1.0.0
     */
    public static boolean sameWorld(@NotNull Map<String, Object> loc1, @NotNull Map<String, Object> loc2) {
        Object world1 = loc1.get(KEY_WORLD);
        Object world2 = loc2.get(KEY_WORLD);
        return Objects.equals(world1, world2);
    }

    /**
     * Adds offset to a location.
     *
     * @param location the location
     * @param dx       X offset
     * @param dy       Y offset
     * @param dz       Z offset
     * @return a new location with offset applied
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> add(@NotNull Map<String, Object> location,
                                           double dx, double dy, double dz) {
        Map<String, Object> result = new LinkedHashMap<>(location);
        result.put(KEY_X, getDouble(location, KEY_X, 0) + dx);
        result.put(KEY_Y, getDouble(location, KEY_Y, 0) + dy);
        result.put(KEY_Z, getDouble(location, KEY_Z, 0) + dz);
        return result;
    }

    /**
     * Gets the chunk coordinates for a location.
     *
     * @param location the location
     * @return a map with "x" and "z" chunk coordinates
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> getChunkCoordinates(@NotNull Map<String, Object> location) {
        Map<String, Object> chunk = new LinkedHashMap<>();
        Object world = location.get(KEY_WORLD);
        if (world != null) {
            chunk.put(KEY_WORLD, world);
        }
        chunk.put(KEY_X, (int) Math.floor(getDouble(location, KEY_X, 0)) >> 4);
        chunk.put(KEY_Z, (int) Math.floor(getDouble(location, KEY_Z, 0)) >> 4);
        return chunk;
    }

    // ========================================
    // JSON Serialization
    // ========================================

    @NotNull
    private String serializeToJson(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        // World
        String world = (String) value.get(KEY_WORLD);
        if (world != null) {
            json.append("\"world\":\"").append(world).append("\"");
            first = false;
        }

        // Coordinates
        if (!first) json.append(",");
        if (context.isPrettyPrint()) {
            json.append("\n  \"x\": ").append(getDouble(value, KEY_X, 0));
            json.append(",\n  \"y\": ").append(getDouble(value, KEY_Y, 0));
            json.append(",\n  \"z\": ").append(getDouble(value, KEY_Z, 0));
        } else {
            json.append("\"x\":").append(getDouble(value, KEY_X, 0));
            json.append(",\"y\":").append(getDouble(value, KEY_Y, 0));
            json.append(",\"z\":").append(getDouble(value, KEY_Z, 0));
        }

        // Rotation
        if (value.containsKey(KEY_YAW) || value.containsKey(KEY_PITCH)) {
            if (context.isPrettyPrint()) {
                json.append(",\n  \"yaw\": ").append(getFloat(value, KEY_YAW, 0));
                json.append(",\n  \"pitch\": ").append(getFloat(value, KEY_PITCH, 0));
                json.append("\n");
            } else {
                json.append(",\"yaw\":").append(getFloat(value, KEY_YAW, 0));
                json.append(",\"pitch\":").append(getFloat(value, KEY_PITCH, 0));
            }
        }

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
                    case KEY_X, KEY_Y, KEY_Z -> result.put(key, Double.parseDouble(valueStr));
                    case KEY_YAW, KEY_PITCH -> result.put(key, Float.parseFloat(valueStr));
                }
            }
        }

        return result;
    }

    // ========================================
    // Helper Methods
    // ========================================

    private static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return defaultValue;
    }

    private static float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.floatValue();
        }
        return defaultValue;
    }
}
