/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.*;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Serializes and deserializes regions to/from maps.
 *
 * <p>This serializer produces and consumes Map structures that can be
 * easily converted to YAML, JSON, or other formats.
 *
 * <h2>Serialization Format</h2>
 * <pre>{@code
 * id: "uuid-string"
 * name: "region-name"
 * type: "CUBOID"
 * world: "world"
 * priority: 10
 * transient: false
 * parent: "parent-region-name"  # optional
 *
 * # Type-specific data
 * bounds:
 *   min: {x: 0, y: 60, z: 0}
 *   max: {x: 100, y: 120, z: 100}
 *
 * flags:
 *   pvp: false
 *   build: true
 *
 * owners:
 *   - "uuid-string"
 *
 * members:
 *   - "uuid-string"
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class RegionSerializer {

    private RegionSerializer() {
        // Utility class
    }

    /**
     * Serializes a region to a map.
     *
     * @param region the region to serialize
     * @return a map representation of the region
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> serialize(@NotNull Region region) {
        Objects.requireNonNull(region, "region cannot be null");

        Map<String, Object> data = new LinkedHashMap<>();

        // Common properties
        data.put("id", region.getId().toString());
        data.put("name", region.getName());
        data.put("type", region.getType().name());
        data.put("world", region.getWorld().getName());
        data.put("priority", region.getPriority());
        data.put("transient", region.isTransient());

        // Parent reference
        region.getParent().ifPresent(parent ->
            data.put("parent", parent.getName())
        );

        // Type-specific serialization
        data.putAll(serializeTypeSpecific(region));

        // Flags
        Map<String, Object> flagsMap = new LinkedHashMap<>();
        for (Map.Entry<RegionFlag<?>, Object> entry : region.getFlags().entrySet()) {
            flagsMap.put(entry.getKey().getName(), entry.getValue());
        }
        if (!flagsMap.isEmpty()) {
            data.put("flags", flagsMap);
        }

        // Owners
        if (!region.getOwners().isEmpty()) {
            data.put("owners", region.getOwners().stream()
                .map(UUID::toString)
                .toList());
        }

        // Members
        if (!region.getMembers().isEmpty()) {
            data.put("members", region.getMembers().stream()
                .map(UUID::toString)
                .toList());
        }

        return data;
    }

    /**
     * Deserializes a region from a map.
     *
     * @param data         the map data
     * @param worldLookup  function to look up worlds by name
     * @param regionLookup function to look up parent regions by name
     * @return the deserialized region
     * @throws IllegalArgumentException if data is invalid
     * @since 1.0.0
     */
    @NotNull
    public static Region deserialize(
            @NotNull Map<String, Object> data,
            @NotNull Function<String, UnifiedWorld> worldLookup,
            @Nullable Function<String, Region> regionLookup
    ) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(worldLookup, "worldLookup cannot be null");

        UUID id = UUID.fromString(getString(data, "id"));
        String name = getString(data, "name");
        Region.RegionType type = Region.RegionType.valueOf(getString(data, "type"));
        String worldName = getString(data, "world");
        UnifiedWorld world = worldLookup.apply(worldName);

        if (world == null) {
            throw new IllegalArgumentException("World not found: " + worldName);
        }

        int priority = getInt(data, "priority", 0);
        boolean transient_ = getBoolean(data, "transient", false);

        // Create the appropriate region type
        AbstractRegion region = switch (type) {
            case CUBOID -> deserializeCuboid(id, name, world, data);
            case SPHERE -> deserializeSphere(id, name, world, data);
            case CYLINDER -> deserializeCylinder(id, name, world, data);
            case POLYGON -> deserializePolygon(id, name, world, data);
            case GLOBAL -> new GlobalRegionImpl(id, world);
        };

        // Apply common properties
        if (type != Region.RegionType.GLOBAL) {
            region.setPriority(priority);
        }
        region.setTransient(transient_);

        // Parent reference
        String parentName = getStringOrNull(data, "parent");
        if (parentName != null && regionLookup != null) {
            Region parent = regionLookup.apply(parentName);
            if (parent != null) {
                region.setParent(parent);
            }
        }

        // Flags
        @SuppressWarnings("unchecked")
        Map<String, Object> flagsMap = (Map<String, Object>) data.get("flags");
        if (flagsMap != null) {
            for (Map.Entry<String, Object> entry : flagsMap.entrySet()) {
                RegionFlag<?> flag = RegionFlag.getByName(entry.getKey());
                if (flag != null) {
                    setFlagFromObject(region, flag, entry.getValue());
                }
            }
        }

        // Owners
        @SuppressWarnings("unchecked")
        List<String> ownersList = (List<String>) data.get("owners");
        if (ownersList != null) {
            for (String ownerStr : ownersList) {
                region.addOwner(UUID.fromString(ownerStr));
            }
        }

        // Members
        @SuppressWarnings("unchecked")
        List<String> membersList = (List<String>) data.get("members");
        if (membersList != null) {
            for (String memberStr : membersList) {
                region.addMember(UUID.fromString(memberStr));
            }
        }

        return region;
    }

    private static Map<String, Object> serializeTypeSpecific(@NotNull Region region) {
        Map<String, Object> data = new LinkedHashMap<>();

        switch (region) {
            case CuboidRegion cuboid -> {
                data.put("bounds", Map.of(
                    "min", serializeLocation(cuboid.getMinimumPoint()),
                    "max", serializeLocation(cuboid.getMaximumPoint())
                ));
            }
            case SphereRegion sphere -> {
                data.put("center", serializeLocation(sphere.getCenter()));
                data.put("radius", sphere.getRadius());
            }
            case CylinderRegion cylinder -> {
                data.put("center", serializeLocation(cylinder.getCenter()));
                data.put("radius", cylinder.getRadius());
                data.put("minY", cylinder.getMinY());
                data.put("maxY", cylinder.getMaxY());
            }
            case PolygonRegion polygon -> {
                List<Map<String, Integer>> pointsList = new ArrayList<>();
                for (int[] point : polygon.getPoints()) {
                    pointsList.add(Map.of("x", point[0], "z", point[1]));
                }
                data.put("points", pointsList);
                data.put("minY", polygon.getMinY());
                data.put("maxY", polygon.getMaxY());
            }
            case GlobalRegion ignored -> {
                // No additional data needed
            }
            default -> {
                // Unknown type, no additional serialization
            }
        }

        return data;
    }

    private static Map<String, Object> serializeLocation(@NotNull UnifiedLocation loc) {
        return Map.of(
            "x", loc.x(),
            "y", loc.y(),
            "z", loc.z()
        );
    }

    private static CuboidRegionImpl deserializeCuboid(
            UUID id, String name, UnifiedWorld world, Map<String, Object> data
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> bounds = (Map<String, Object>) data.get("bounds");
        if (bounds == null) {
            throw new IllegalArgumentException("Cuboid region missing bounds");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> minData = (Map<String, Object>) bounds.get("min");
        @SuppressWarnings("unchecked")
        Map<String, Object> maxData = (Map<String, Object>) bounds.get("max");

        UnifiedLocation min = deserializeLocation(world, minData);
        UnifiedLocation max = deserializeLocation(world, maxData);

        return new CuboidRegionImpl(id, name, world, min, max);
    }

    private static SphereRegionImpl deserializeSphere(
            UUID id, String name, UnifiedWorld world, Map<String, Object> data
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> centerData = (Map<String, Object>) data.get("center");
        double radius = getDouble(data, "radius", 0);

        UnifiedLocation center = deserializeLocation(world, centerData);
        return new SphereRegionImpl(id, name, world, center, radius);
    }

    private static CylinderRegionImpl deserializeCylinder(
            UUID id, String name, UnifiedWorld world, Map<String, Object> data
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> centerData = (Map<String, Object>) data.get("center");
        double radius = getDouble(data, "radius", 0);
        int minY = getInt(data, "minY", 0);
        int maxY = getInt(data, "maxY", 256);

        UnifiedLocation center = deserializeLocation(world, centerData);
        return new CylinderRegionImpl(id, name, world, center, radius, minY, maxY);
    }

    private static PolygonRegionImpl deserializePolygon(
            UUID id, String name, UnifiedWorld world, Map<String, Object> data
    ) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pointsData = (List<Map<String, Object>>) data.get("points");
        int minY = getInt(data, "minY", 0);
        int maxY = getInt(data, "maxY", 256);

        List<int[]> points = new ArrayList<>();
        if (pointsData != null) {
            for (Map<String, Object> pointData : pointsData) {
                int x = getInt(pointData, "x", 0);
                int z = getInt(pointData, "z", 0);
                points.add(new int[]{x, z});
            }
        }

        return new PolygonRegionImpl(id, name, world, points, minY, maxY);
    }

    private static UnifiedLocation deserializeLocation(
            UnifiedWorld world, Map<String, Object> data
    ) {
        double x = getDouble(data, "x", 0);
        double y = getDouble(data, "y", 0);
        double z = getDouble(data, "z", 0);
        return new UnifiedLocation(world, x, y, z);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setFlagFromObject(
            AbstractRegion region, RegionFlag<T> flag, Object value
    ) {
        T typedValue = flag.cast(value);
        if (typedValue != null) {
            region.setFlag(flag, typedValue);
        }
    }

    private static String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value.toString();
    }

    @Nullable
    private static String getStringOrNull(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : value.toString();
    }

    private static int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.intValue();
        return Integer.parseInt(value.toString());
    }

    private static double getDouble(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(value.toString());
    }
}
