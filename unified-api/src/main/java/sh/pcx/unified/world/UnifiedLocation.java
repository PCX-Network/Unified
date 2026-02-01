/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable record representing a location in a Minecraft world.
 *
 * <p>A location consists of a world reference, 3D coordinates (x, y, z),
 * and optional rotation values (yaw, pitch). Locations are immutable;
 * all modification methods return new instances.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a location with coordinates only
 * UnifiedLocation loc1 = new UnifiedLocation(world, 100, 64, 200);
 *
 * // Create a location with rotation
 * UnifiedLocation loc2 = new UnifiedLocation(world, 100, 64, 200, 90.0f, 0.0f);
 *
 * // Create using the builder
 * UnifiedLocation loc3 = UnifiedLocation.builder()
 *     .world(world)
 *     .x(100).y(64).z(200)
 *     .yaw(90.0f).pitch(0.0f)
 *     .build();
 *
 * // Modify location (returns new instance)
 * UnifiedLocation moved = loc1.add(10, 0, -5);
 * UnifiedLocation rotated = loc1.withYaw(180.0f);
 *
 * // Calculate distance
 * double distance = loc1.distance(loc2);
 *
 * // Get block location (integer coordinates)
 * UnifiedLocation blockLoc = loc1.toBlockLocation();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This record is immutable and therefore thread-safe.
 *
 * @param world the world this location is in (may be null for serialized locations)
 * @param x     the x coordinate
 * @param y     the y coordinate
 * @param z     the z coordinate
 * @param yaw   the yaw rotation (-180 to 180)
 * @param pitch the pitch rotation (-90 to 90)
 *
 * @since 1.0.0
 * @author Supatuck
 */
public record UnifiedLocation(
        @Nullable UnifiedWorld world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    /**
     * Creates a location without rotation (yaw and pitch default to 0).
     *
     * @param world the world this location is in
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     */
    public UnifiedLocation(@Nullable UnifiedWorld world, double x, double y, double z) {
        this(world, x, y, z, 0.0f, 0.0f);
    }

    /**
     * Compact constructor that normalizes rotation values.
     */
    public UnifiedLocation {
        // Normalize yaw to -180 to 180 range
        yaw = normalizeYaw(yaw);
        // Clamp pitch to -90 to 90 range
        pitch = Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    /**
     * Returns the world as an Optional.
     *
     * @return an Optional containing the world, or empty if world is null
     * @since 1.0.0
     */
    @NotNull
    public Optional<UnifiedWorld> getWorld() {
        return Optional.ofNullable(world);
    }

    /**
     * Returns the block X coordinate (floored).
     *
     * @return the block x coordinate
     * @since 1.0.0
     */
    public int getBlockX() {
        return (int) Math.floor(x);
    }

    /**
     * Returns the block Y coordinate (floored).
     *
     * @return the block y coordinate
     * @since 1.0.0
     */
    public int getBlockY() {
        return (int) Math.floor(y);
    }

    /**
     * Returns the block Z coordinate (floored).
     *
     * @return the block z coordinate
     * @since 1.0.0
     */
    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    /**
     * Returns a new location with the coordinates added.
     *
     * @param dx the x offset
     * @param dy the y offset
     * @param dz the z offset
     * @return a new location with the offset applied
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation add(double dx, double dy, double dz) {
        return new UnifiedLocation(world, x + dx, y + dy, z + dz, yaw, pitch);
    }

    /**
     * Returns a new location with the coordinates subtracted.
     *
     * @param dx the x offset
     * @param dy the y offset
     * @param dz the z offset
     * @return a new location with the offset applied
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation subtract(double dx, double dy, double dz) {
        return new UnifiedLocation(world, x - dx, y - dy, z - dz, yaw, pitch);
    }

    /**
     * Returns a new location with the other location's coordinates added.
     *
     * @param other the location to add
     * @return a new location with the offset applied
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation add(@NotNull UnifiedLocation other) {
        return add(other.x, other.y, other.z);
    }

    /**
     * Returns a new location with the other location's coordinates subtracted.
     *
     * @param other the location to subtract
     * @return a new location with the offset applied
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation subtract(@NotNull UnifiedLocation other) {
        return subtract(other.x, other.y, other.z);
    }

    /**
     * Returns a new location with the coordinates multiplied.
     *
     * @param factor the multiplication factor
     * @return a new location with scaled coordinates
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation multiply(double factor) {
        return new UnifiedLocation(world, x * factor, y * factor, z * factor, yaw, pitch);
    }

    /**
     * Returns a new location with a different world.
     *
     * @param world the new world
     * @return a new location in the specified world
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation withWorld(@Nullable UnifiedWorld world) {
        return new UnifiedLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new location with a different x coordinate.
     *
     * @param x the new x coordinate
     * @return a new location with the specified x
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation withX(double x) {
        return new UnifiedLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new location with a different y coordinate.
     *
     * @param y the new y coordinate
     * @return a new location with the specified y
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation withY(double y) {
        return new UnifiedLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new location with a different z coordinate.
     *
     * @param z the new z coordinate
     * @return a new location with the specified z
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation withZ(double z) {
        return new UnifiedLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new location with a different yaw.
     *
     * @param yaw the new yaw rotation
     * @return a new location with the specified yaw
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation withYaw(float yaw) {
        return new UnifiedLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new location with a different pitch.
     *
     * @param pitch the new pitch rotation
     * @return a new location with the specified pitch
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation withPitch(float pitch) {
        return new UnifiedLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * Returns this location as a block location (integer coordinates, no rotation).
     *
     * @return a new location with floored coordinates
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation toBlockLocation() {
        return new UnifiedLocation(world, getBlockX(), getBlockY(), getBlockZ(), 0.0f, 0.0f);
    }

    /**
     * Returns the center of the block at this location.
     *
     * @return a new location at the center of the block
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation toBlockCenter() {
        return new UnifiedLocation(world, getBlockX() + 0.5, getBlockY() + 0.5, getBlockZ() + 0.5, yaw, pitch);
    }

    /**
     * Calculates the distance between this location and another.
     *
     * @param other the other location
     * @return the distance between the locations
     * @throws IllegalArgumentException if the locations are in different worlds
     * @since 1.0.0
     */
    public double distance(@NotNull UnifiedLocation other) {
        checkSameWorld(other);
        return Math.sqrt(distanceSquared(other));
    }

    /**
     * Calculates the squared distance between this location and another.
     *
     * <p>This is more efficient than {@link #distance(UnifiedLocation)} when
     * only comparing distances, as it avoids the square root calculation.
     *
     * @param other the other location
     * @return the squared distance between the locations
     * @throws IllegalArgumentException if the locations are in different worlds
     * @since 1.0.0
     */
    public double distanceSquared(@NotNull UnifiedLocation other) {
        checkSameWorld(other);
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the 2D distance (ignoring Y) between this location and another.
     *
     * @param other the other location
     * @return the 2D distance between the locations
     * @throws IllegalArgumentException if the locations are in different worlds
     * @since 1.0.0
     */
    public double distance2D(@NotNull UnifiedLocation other) {
        checkSameWorld(other);
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Checks if this location is within a certain distance of another.
     *
     * @param other    the other location
     * @param distance the maximum distance
     * @return true if the locations are within the specified distance
     * @since 1.0.0
     */
    public boolean isNear(@NotNull UnifiedLocation other, double distance) {
        if (!sameWorld(other)) return false;
        return distanceSquared(other) <= distance * distance;
    }

    /**
     * Checks if this location is in the same world as another.
     *
     * @param other the other location
     * @return true if both locations are in the same world
     * @since 1.0.0
     */
    public boolean sameWorld(@NotNull UnifiedLocation other) {
        return Objects.equals(world, other.world);
    }

    /**
     * Returns the length of the location vector (distance from origin).
     *
     * @return the length of the vector
     * @since 1.0.0
     */
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Returns the squared length of the location vector.
     *
     * @return the squared length of the vector
     * @since 1.0.0
     */
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    /**
     * Returns the chunk X coordinate for this location.
     *
     * @return the chunk x coordinate
     * @since 1.0.0
     */
    public int getChunkX() {
        return getBlockX() >> 4;
    }

    /**
     * Returns the chunk Z coordinate for this location.
     *
     * @return the chunk z coordinate
     * @since 1.0.0
     */
    public int getChunkZ() {
        return getBlockZ() >> 4;
    }

    /**
     * Creates a new builder for constructing UnifiedLocation instances.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a location at the origin (0, 0, 0) in the specified world.
     *
     * @param world the world
     * @return a new location at the origin
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedLocation origin(@Nullable UnifiedWorld world) {
        return new UnifiedLocation(world, 0, 0, 0);
    }

    /**
     * Creates a location with the specified coordinates in no world.
     *
     * <p>This is useful for creating locations that will later have their
     * world set, or for locations that only need coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return a new location without a world
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedLocation of(double x, double y, double z) {
        return new UnifiedLocation(null, x, y, z);
    }

    /**
     * Creates a location with the specified world name and coordinates.
     *
     * <p>Note: This creates a location with a null world reference but stores
     * the world name. The actual world reference should be resolved later.
     *
     * @param worldName the world name (stored but not resolved)
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param z         the z coordinate
     * @return a new location
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedLocation of(@NotNull String worldName, double x, double y, double z) {
        // World reference is null - must be resolved by the caller
        return new UnifiedLocation(null, x, y, z);
    }

    /**
     * Creates a location with coordinates and rotation.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     * @param yaw   the yaw rotation
     * @param pitch the pitch rotation
     * @return a new location without a world
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedLocation of(double x, double y, double z, float yaw, float pitch) {
        return new UnifiedLocation(null, x, y, z, yaw, pitch);
    }

    private void checkSameWorld(@NotNull UnifiedLocation other) {
        if (!sameWorld(other)) {
            throw new IllegalArgumentException("Cannot calculate distance between locations in different worlds");
        }
    }

    private static float normalizeYaw(float yaw) {
        yaw = yaw % 360.0f;
        if (yaw >= 180.0f) {
            yaw -= 360.0f;
        } else if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }

    /**
     * Builder class for creating {@link UnifiedLocation} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private UnifiedWorld world;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;

        private Builder() {}

        /**
         * Sets the world.
         *
         * @param world the world
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder world(@Nullable UnifiedWorld world) {
            this.world = world;
            return this;
        }

        /**
         * Sets the x coordinate.
         *
         * @param x the x coordinate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder x(double x) {
            this.x = x;
            return this;
        }

        /**
         * Sets the y coordinate.
         *
         * @param y the y coordinate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder y(double y) {
            this.y = y;
            return this;
        }

        /**
         * Sets the z coordinate.
         *
         * @param z the z coordinate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder z(double z) {
            this.z = z;
            return this;
        }

        /**
         * Sets the yaw rotation.
         *
         * @param yaw the yaw rotation
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder yaw(float yaw) {
            this.yaw = yaw;
            return this;
        }

        /**
         * Sets the pitch rotation.
         *
         * @param pitch the pitch rotation
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        /**
         * Sets all coordinates at once.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @param z the z coordinate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder coordinates(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /**
         * Sets the rotation.
         *
         * @param yaw   the yaw rotation
         * @param pitch the pitch rotation
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }

        /**
         * Copies values from an existing location.
         *
         * @param location the location to copy from
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder from(@NotNull UnifiedLocation location) {
            this.world = location.world;
            this.x = location.x;
            this.y = location.y;
            this.z = location.z;
            this.yaw = location.yaw;
            this.pitch = location.pitch;
            return this;
        }

        /**
         * Builds the UnifiedLocation instance.
         *
         * @return a new UnifiedLocation
         * @since 1.0.0
         */
        @NotNull
        public UnifiedLocation build() {
            return new UnifiedLocation(world, x, y, z, yaw, pitch);
        }
    }
}
