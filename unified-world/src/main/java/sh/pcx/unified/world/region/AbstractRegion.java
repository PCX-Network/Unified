/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.Region;
import sh.pcx.unified.region.RegionFlag;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of {@link Region}.
 *
 * <p>Provides common functionality for all region types including flag management,
 * owner/member handling, and parent region inheritance.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractRegion implements Region {

    protected final UUID id;
    protected final String name;
    protected final UnifiedWorld world;
    protected volatile int priority;
    protected volatile Region parent;
    protected volatile boolean transient_;

    protected final Map<RegionFlag<?>, Object> flags = new ConcurrentHashMap<>();
    protected final Set<UUID> owners = ConcurrentHashMap.newKeySet();
    protected final Set<UUID> members = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new abstract region.
     *
     * @param id    the region UUID
     * @param name  the region name
     * @param world the world
     */
    protected AbstractRegion(@NotNull UUID id, @NotNull String name, @NotNull UnifiedWorld world) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.priority = 0;
        this.transient_ = false;
    }

    /**
     * Creates a new abstract region with auto-generated UUID.
     *
     * @param name  the region name
     * @param world the world
     */
    protected AbstractRegion(@NotNull String name, @NotNull UnifiedWorld world) {
        this(UUID.randomUUID(), name, world);
    }

    @Override
    @NotNull
    public UUID getId() {
        return id;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return world;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    @NotNull
    public Optional<Region> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public void setParent(@Nullable Region parent) {
        // Prevent circular references
        if (parent != null) {
            Region current = parent;
            while (current != null) {
                if (current.equals(this)) {
                    throw new IllegalArgumentException("Circular parent reference detected");
                }
                current = current.getParent().orElse(null);
            }
        }
        this.parent = parent;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getFlag(@NotNull RegionFlag<T> flag) {
        Objects.requireNonNull(flag, "flag cannot be null");

        // Check local flags first
        Object value = flags.get(flag);
        if (value != null) {
            return Optional.of((T) value);
        }

        // Check parent region
        if (parent != null) {
            return parent.getFlag(flag);
        }

        return Optional.empty();
    }

    @Override
    public <T> void setFlag(@NotNull RegionFlag<T> flag, @Nullable T value) {
        Objects.requireNonNull(flag, "flag cannot be null");

        if (value == null) {
            flags.remove(flag);
        } else {
            flags.put(flag, value);
        }
    }

    @Override
    public void removeFlag(@NotNull RegionFlag<?> flag) {
        Objects.requireNonNull(flag, "flag cannot be null");
        flags.remove(flag);
    }

    @Override
    public boolean hasFlag(@NotNull RegionFlag<?> flag) {
        Objects.requireNonNull(flag, "flag cannot be null");
        return flags.containsKey(flag);
    }

    @Override
    @NotNull
    public Map<RegionFlag<?>, Object> getFlags() {
        return Collections.unmodifiableMap(new HashMap<>(flags));
    }

    @Override
    @NotNull
    public Set<UUID> getOwners() {
        return Collections.unmodifiableSet(new HashSet<>(owners));
    }

    @Override
    public void addOwner(@NotNull UUID owner) {
        Objects.requireNonNull(owner, "owner cannot be null");
        owners.add(owner);
    }

    @Override
    public boolean removeOwner(@NotNull UUID owner) {
        Objects.requireNonNull(owner, "owner cannot be null");
        return owners.remove(owner);
    }

    @Override
    public boolean isOwner(@NotNull UUID player) {
        Objects.requireNonNull(player, "player cannot be null");
        return owners.contains(player);
    }

    @Override
    @NotNull
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(new HashSet<>(members));
    }

    @Override
    public void addMember(@NotNull UUID member) {
        Objects.requireNonNull(member, "member cannot be null");
        members.add(member);
    }

    @Override
    public boolean removeMember(@NotNull UUID member) {
        Objects.requireNonNull(member, "member cannot be null");
        return members.remove(member);
    }

    @Override
    public boolean isMember(@NotNull UUID player) {
        Objects.requireNonNull(player, "player cannot be null");
        return members.contains(player);
    }

    @Override
    public boolean isTransient() {
        return transient_;
    }

    @Override
    public void setTransient(boolean transient_) {
        this.transient_ = transient_;
    }

    @Override
    public boolean contains(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");

        // Must be in same world
        if (!location.sameWorld(new UnifiedLocation(world, 0, 0, 0))) {
            return false;
        }

        return contains(location.x(), location.y(), location.z());
    }

    @Override
    public boolean intersects(@NotNull Region other) {
        Objects.requireNonNull(other, "other region cannot be null");

        // Must be in same world
        if (!other.getWorld().equals(world)) {
            return false;
        }

        // AABB intersection test using bounding boxes
        UnifiedLocation thisMin = getMinimumPoint();
        UnifiedLocation thisMax = getMaximumPoint();
        UnifiedLocation otherMin = other.getMinimumPoint();
        UnifiedLocation otherMax = other.getMaximumPoint();

        return thisMin.x() <= otherMax.x() && thisMax.x() >= otherMin.x()
            && thisMin.y() <= otherMax.y() && thisMax.y() >= otherMin.y()
            && thisMin.z() <= otherMax.z() && thisMax.z() >= otherMin.z();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Region that)) return false;
        return id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", world=" + world.getName() +
            ", priority=" + priority +
            ']';
    }
}
