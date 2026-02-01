/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.region.Region;
import sh.pcx.unified.region.RegionBuilder;
import sh.pcx.unified.region.RegionFlag;
import sh.pcx.unified.region.RegionService;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Abstract base class for region builders.
 *
 * @param <B> the builder type
 * @param <R> the region type
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractRegionBuilder<B extends RegionBuilder<B, R>, R extends Region>
        implements RegionBuilder<B, R> {

    protected final String name;
    protected final RegionService service;

    protected UnifiedWorld world;
    protected int priority = 0;
    protected Region parent;
    protected boolean transient_ = false;

    protected final Map<RegionFlag<?>, Object> flags = new HashMap<>();
    protected final Set<UUID> owners = new HashSet<>();
    protected final Set<UUID> members = new HashSet<>();

    /**
     * Creates a new region builder.
     *
     * @param name    the region name
     * @param service the region service
     */
    protected AbstractRegionBuilder(@NotNull String name, @NotNull RegionService service) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.service = Objects.requireNonNull(service, "service cannot be null");
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public B world(@NotNull UnifiedWorld world) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        return (B) this;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public B priority(int priority) {
        this.priority = priority;
        return (B) this;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public B parent(@Nullable Region parent) {
        this.parent = parent;
        return (B) this;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> B flag(@NotNull RegionFlag<T> flag, @Nullable T value) {
        Objects.requireNonNull(flag, "flag cannot be null");
        if (value == null) {
            flags.remove(flag);
        } else {
            flags.put(flag, value);
        }
        return (B) this;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public B owner(@NotNull UUID owner) {
        Objects.requireNonNull(owner, "owner cannot be null");
        owners.add(owner);
        return (B) this;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public B member(@NotNull UUID member) {
        Objects.requireNonNull(member, "member cannot be null");
        members.add(member);
        return (B) this;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public B transient_(boolean transient_) {
        this.transient_ = transient_;
        return (B) this;
    }

    @Override
    @NotNull
    public R create() {
        R region = build();
        service.registerRegion(region);
        return region;
    }

    /**
     * Validates that required properties are set.
     *
     * @throws IllegalStateException if validation fails
     */
    protected void validate() {
        if (world == null) {
            throw new IllegalStateException("World must be set");
        }
    }

    /**
     * Applies common properties to an abstract region.
     *
     * @param region the region to configure
     */
    @SuppressWarnings("unchecked")
    protected void applyCommonProperties(AbstractRegion region) {
        region.setPriority(priority);
        region.setParent(parent);
        region.setTransient(transient_);

        for (Map.Entry<RegionFlag<?>, Object> entry : flags.entrySet()) {
            RegionFlag<Object> flag = (RegionFlag<Object>) entry.getKey();
            region.setFlag(flag, entry.getValue());
        }

        for (UUID owner : owners) {
            region.addOwner(owner);
        }

        for (UUID member : members) {
            region.addMember(member);
        }
    }
}
