/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base interface for fluent region builders.
 *
 * <p>Region builders provide a fluent API for creating regions with various
 * configurations. Each region type has its own specific builder with
 * shape-specific methods.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Region spawn = regions.cuboid("spawn")
 *     .world(world)
 *     .min(minLocation)
 *     .max(maxLocation)
 *     .flag(RegionFlag.PVP, false)
 *     .flag(RegionFlag.BUILD, false)
 *     .priority(10)
 *     .owner(ownerUuid)
 *     .create();
 * }</pre>
 *
 * @param <B> the builder type (for fluent chaining)
 * @param <R> the region type being built
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CuboidRegionBuilder
 * @see SphereRegionBuilder
 * @see CylinderRegionBuilder
 * @see PolygonRegionBuilder
 */
public interface RegionBuilder<B extends RegionBuilder<B, R>, R extends Region> {

    /**
     * Sets the world for this region.
     *
     * @param world the world
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    B world(@NotNull UnifiedWorld world);

    /**
     * Sets the priority for this region.
     *
     * <p>Higher priority regions take precedence when regions overlap.
     * Default priority is 0.
     *
     * @param priority the priority
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    B priority(int priority);

    /**
     * Sets the parent region for inheritance.
     *
     * @param parent the parent region
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    B parent(@Nullable Region parent);

    /**
     * Sets a flag value on this region.
     *
     * @param flag  the flag to set
     * @param value the value
     * @param <T>   the flag value type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    <T> B flag(@NotNull RegionFlag<T> flag, @Nullable T value);

    /**
     * Adds an owner to this region.
     *
     * @param owner the owner's UUID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    B owner(@NotNull UUID owner);

    /**
     * Adds a member to this region.
     *
     * @param member the member's UUID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    B member(@NotNull UUID member);

    /**
     * Marks this region as transient (not persisted).
     *
     * @param transient_ true to make transient
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    B transient_(boolean transient_);

    /**
     * Marks this region as transient (not persisted).
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default B transient_() {
        return transient_(true);
    }

    /**
     * Creates and registers the region.
     *
     * @return the created region
     * @throws IllegalStateException if required properties are not set
     * @since 1.0.0
     */
    @NotNull
    R create();

    /**
     * Creates the region without registering it.
     *
     * <p>This is useful for temporary regions that don't need to be tracked
     * by the region service.
     *
     * @return the created region
     * @throws IllegalStateException if required properties are not set
     * @since 1.0.0
     */
    @NotNull
    R build();
}
