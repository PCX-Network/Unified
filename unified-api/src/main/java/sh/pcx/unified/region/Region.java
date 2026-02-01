/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a protected region within a world.
 *
 * <p>A region defines a bounded area with associated flags that control
 * what actions are allowed or denied within that area. Regions can overlap,
 * with priorities determining which region's flags take precedence.
 *
 * <h2>Region Types</h2>
 * <p>The system supports multiple region shapes:
 * <ul>
 *   <li>{@link RegionType#CUBOID} - Axis-aligned box</li>
 *   <li>{@link RegionType#SPHERE} - Spherical area</li>
 *   <li>{@link RegionType#CYLINDER} - Vertical cylinder</li>
 *   <li>{@link RegionType#POLYGON} - 2D polygon extruded vertically</li>
 *   <li>{@link RegionType#GLOBAL} - Entire world</li>
 * </ul>
 *
 * <h2>Priority System</h2>
 * <p>When multiple regions overlap, the region with the highest priority
 * takes precedence for flag queries. If priorities are equal, the most
 * recently created region takes precedence.
 *
 * <h2>Parent Regions</h2>
 * <p>Regions can have parent regions to inherit flags. If a flag is not
 * set on a region, it will inherit from its parent (if any).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Check if a location is in a region
 * if (region.contains(location)) {
 *     // Get a flag value
 *     boolean canBuild = region.getFlag(RegionFlag.BUILD)
 *         .orElse(RegionFlag.BUILD.getDefaultValue());
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RegionService
 * @see RegionFlag
 */
public interface Region extends Comparable<Region> {

    /**
     * Returns the unique identifier for this region.
     *
     * @return the region's UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns the name of this region.
     *
     * <p>Region names are unique within a world but may be duplicated
     * across different worlds.
     *
     * @return the region name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the world this region is in.
     *
     * @return the region's world
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld getWorld();

    /**
     * Returns the type of this region.
     *
     * @return the region type
     * @since 1.0.0
     */
    @NotNull
    RegionType getType();

    /**
     * Returns the priority of this region.
     *
     * <p>Higher priority regions take precedence when regions overlap.
     * Default priority is 0.
     *
     * @return the region priority
     * @since 1.0.0
     */
    int getPriority();

    /**
     * Sets the priority of this region.
     *
     * @param priority the new priority
     * @since 1.0.0
     */
    void setPriority(int priority);

    /**
     * Returns the parent region, if any.
     *
     * <p>Child regions inherit flags from their parent when not explicitly set.
     *
     * @return an Optional containing the parent region
     * @since 1.0.0
     */
    @NotNull
    Optional<Region> getParent();

    /**
     * Sets the parent region.
     *
     * @param parent the parent region, or null to remove parent
     * @since 1.0.0
     */
    void setParent(@Nullable Region parent);

    /**
     * Checks if this region contains the given location.
     *
     * @param location the location to check
     * @return true if the location is inside this region
     * @since 1.0.0
     */
    boolean contains(@NotNull UnifiedLocation location);

    /**
     * Checks if this region contains the given coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return true if the coordinates are inside this region
     * @since 1.0.0
     */
    boolean contains(double x, double y, double z);

    /**
     * Gets the value of a flag on this region.
     *
     * <p>If the flag is not set on this region, it will check the parent
     * region (if any).
     *
     * @param flag the flag to query
     * @param <T>  the flag value type
     * @return an Optional containing the flag value if set
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getFlag(@NotNull RegionFlag<T> flag);

    /**
     * Gets the value of a flag, returning a default if not set.
     *
     * @param flag         the flag to query
     * @param defaultValue the default value if not set
     * @param <T>          the flag value type
     * @return the flag value, or the default
     * @since 1.0.0
     */
    default <T> T getFlag(@NotNull RegionFlag<T> flag, @NotNull T defaultValue) {
        return getFlag(flag).orElse(defaultValue);
    }

    /**
     * Sets a flag value on this region.
     *
     * @param flag  the flag to set
     * @param value the value to set
     * @param <T>   the flag value type
     * @since 1.0.0
     */
    <T> void setFlag(@NotNull RegionFlag<T> flag, @Nullable T value);

    /**
     * Removes a flag from this region.
     *
     * @param flag the flag to remove
     * @since 1.0.0
     */
    void removeFlag(@NotNull RegionFlag<?> flag);

    /**
     * Checks if this region has a flag explicitly set.
     *
     * <p>This does not check parent regions.
     *
     * @param flag the flag to check
     * @return true if the flag is set on this region
     * @since 1.0.0
     */
    boolean hasFlag(@NotNull RegionFlag<?> flag);

    /**
     * Returns all flags explicitly set on this region.
     *
     * @return an unmodifiable map of flags to values
     * @since 1.0.0
     */
    @NotNull
    Map<RegionFlag<?>, Object> getFlags();

    /**
     * Returns the owners of this region.
     *
     * <p>Owners have full control over the region and bypass most restrictions.
     *
     * @return a set of owner UUIDs
     * @since 1.0.0
     */
    @NotNull
    Set<UUID> getOwners();

    /**
     * Adds an owner to this region.
     *
     * @param owner the owner's UUID
     * @since 1.0.0
     */
    void addOwner(@NotNull UUID owner);

    /**
     * Removes an owner from this region.
     *
     * @param owner the owner's UUID
     * @return true if the owner was removed
     * @since 1.0.0
     */
    boolean removeOwner(@NotNull UUID owner);

    /**
     * Checks if a player is an owner of this region.
     *
     * @param player the player's UUID
     * @return true if the player is an owner
     * @since 1.0.0
     */
    boolean isOwner(@NotNull UUID player);

    /**
     * Returns the members of this region.
     *
     * <p>Members have limited permissions as defined by flags.
     *
     * @return a set of member UUIDs
     * @since 1.0.0
     */
    @NotNull
    Set<UUID> getMembers();

    /**
     * Adds a member to this region.
     *
     * @param member the member's UUID
     * @since 1.0.0
     */
    void addMember(@NotNull UUID member);

    /**
     * Removes a member from this region.
     *
     * @param member the member's UUID
     * @return true if the member was removed
     * @since 1.0.0
     */
    boolean removeMember(@NotNull UUID member);

    /**
     * Checks if a player is a member of this region.
     *
     * @param player the player's UUID
     * @return true if the player is a member
     * @since 1.0.0
     */
    boolean isMember(@NotNull UUID player);

    /**
     * Checks if a player is an owner or member of this region.
     *
     * @param player the player's UUID
     * @return true if the player is an owner or member
     * @since 1.0.0
     */
    default boolean isMemberOrOwner(@NotNull UUID player) {
        return isOwner(player) || isMember(player);
    }

    /**
     * Returns the minimum bounding location of this region.
     *
     * <p>For non-cuboid regions, this returns the minimum bounds of the
     * axis-aligned bounding box.
     *
     * @return the minimum bounding location
     * @since 1.0.0
     */
    @NotNull
    UnifiedLocation getMinimumPoint();

    /**
     * Returns the maximum bounding location of this region.
     *
     * <p>For non-cuboid regions, this returns the maximum bounds of the
     * axis-aligned bounding box.
     *
     * @return the maximum bounding location
     * @since 1.0.0
     */
    @NotNull
    UnifiedLocation getMaximumPoint();

    /**
     * Returns the center point of this region.
     *
     * @return the center location
     * @since 1.0.0
     */
    @NotNull
    UnifiedLocation getCenter();

    /**
     * Calculates the volume of this region in blocks.
     *
     * @return the volume in blocks
     * @since 1.0.0
     */
    long getVolume();

    /**
     * Checks if this region intersects with another region.
     *
     * @param other the other region
     * @return true if the regions intersect
     * @since 1.0.0
     */
    boolean intersects(@NotNull Region other);

    /**
     * Checks if this region is transient (not persisted).
     *
     * <p>Transient regions exist only in memory and are not saved.
     *
     * @return true if this region is transient
     * @since 1.0.0
     */
    boolean isTransient();

    /**
     * Marks whether this region should be persisted.
     *
     * @param transient_ true to make the region transient
     * @since 1.0.0
     */
    void setTransient(boolean transient_);

    /**
     * Compares regions by priority (descending) then by name.
     *
     * @param other the region to compare with
     * @return a negative integer, zero, or a positive integer
     * @since 1.0.0
     */
    @Override
    default int compareTo(@NotNull Region other) {
        int priorityCompare = Integer.compare(other.getPriority(), this.getPriority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return this.getName().compareToIgnoreCase(other.getName());
    }

    /**
     * Region shape types.
     *
     * @since 1.0.0
     */
    enum RegionType {
        /** Axis-aligned bounding box. */
        CUBOID,
        /** Spherical region. */
        SPHERE,
        /** Vertical cylinder. */
        CYLINDER,
        /** 2D polygon extruded vertically. */
        POLYGON,
        /** Entire world. */
        GLOBAL
    }
}
