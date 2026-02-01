/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a Folia region context for thread-safe operations.
 *
 * <p>In Folia's threading model, the world is divided into regions, and each
 * region is processed by a dedicated thread. This class encapsulates the
 * information needed to identify a specific region and schedule tasks on it.
 *
 * <h2>Region Model</h2>
 * <p>A region is typically a group of chunks that are processed together.
 * The exact size and boundaries are determined by Folia's internal algorithms
 * based on entity density and player locations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a region context from a location
 * RegionContext context = RegionContext.of(location);
 *
 * // Check if current thread owns this region
 * if (context.isOwnedByCurrentThread()) {
 *     // Safe to modify blocks/entities here
 *     block.setType(Material.STONE);
 * } else {
 *     // Need to schedule on the correct thread
 *     scheduler.runAtRegion(context, () -> {
 *         block.setType(Material.STONE);
 *     });
 * }
 *
 * // Create a global region context
 * RegionContext global = RegionContext.global();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe. Region contexts can be safely
 * shared between threads without synchronization.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FoliaRegionScheduler
 * @see FoliaEntityScheduler
 */
public final class RegionContext {

    /**
     * Singleton instance representing the global region.
     */
    private static final RegionContext GLOBAL = new RegionContext(null, 0, 0, true);

    /**
     * The world this region is in.
     */
    @Nullable
    private final UnifiedWorld world;

    /**
     * The chunk X coordinate representing this region.
     */
    private final int chunkX;

    /**
     * The chunk Z coordinate representing this region.
     */
    private final int chunkZ;

    /**
     * Whether this represents the global region.
     */
    private final boolean global;

    /**
     * The platform-specific world handle for scheduler operations.
     */
    @Nullable
    private final Object worldHandle;

    /**
     * Constructs a new RegionContext.
     *
     * @param world the world this region is in
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @param global whether this is the global region
     */
    private RegionContext(@Nullable UnifiedWorld world, int chunkX, int chunkZ, boolean global) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.global = global;
        this.worldHandle = world != null ? world.getHandle() : null;
    }

    /**
     * Creates a region context for a specific location.
     *
     * @param location the location to create a context for
     * @return a new RegionContext for the location's region
     * @throws NullPointerException if location or location's world is null
     * @since 1.0.0
     */
    @NotNull
    public static RegionContext of(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");
        UnifiedWorld world = location.world();
        if (world == null) {
            throw new IllegalArgumentException("Location must have a world");
        }
        return new RegionContext(world, location.getChunkX(), location.getChunkZ(), false);
    }

    /**
     * Creates a region context for a specific chunk.
     *
     * @param world the world containing the chunk
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return a new RegionContext for the specified chunk's region
     * @throws NullPointerException if world is null
     * @since 1.0.0
     */
    @NotNull
    public static RegionContext of(@NotNull UnifiedWorld world, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world cannot be null");
        return new RegionContext(world, chunkX, chunkZ, false);
    }

    /**
     * Creates a region context for a specific block coordinate.
     *
     * @param world the world containing the block
     * @param blockX the block X coordinate
     * @param blockZ the block Z coordinate
     * @return a new RegionContext for the block's region
     * @throws NullPointerException if world is null
     * @since 1.0.0
     */
    @NotNull
    public static RegionContext ofBlock(@NotNull UnifiedWorld world, int blockX, int blockZ) {
        Objects.requireNonNull(world, "world cannot be null");
        return new RegionContext(world, blockX >> 4, blockZ >> 4, false);
    }

    /**
     * Returns the global region context.
     *
     * <p>The global region is used for tasks that don't belong to any
     * specific world location, such as:
     * <ul>
     *   <li>Server-wide broadcasts</li>
     *   <li>Plugin initialization</li>
     *   <li>Cross-world operations</li>
     * </ul>
     *
     * @return the global region context
     * @since 1.0.0
     */
    @NotNull
    public static RegionContext global() {
        return GLOBAL;
    }

    /**
     * Returns the world this region is in.
     *
     * @return an Optional containing the world, or empty for global region
     * @since 1.0.0
     */
    @NotNull
    public Optional<UnifiedWorld> getWorld() {
        return Optional.ofNullable(world);
    }

    /**
     * Returns the platform-specific world handle.
     *
     * @param <T> the expected world type
     * @return the world handle, or null for global region
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getWorldHandle() {
        return (T) worldHandle;
    }

    /**
     * Returns the chunk X coordinate of this region.
     *
     * @return the chunk X coordinate
     * @since 1.0.0
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * Returns the chunk Z coordinate of this region.
     *
     * @return the chunk Z coordinate
     * @since 1.0.0
     */
    public int getChunkZ() {
        return chunkZ;
    }

    /**
     * Returns the block X coordinate at the center of this chunk.
     *
     * @return the center block X coordinate
     * @since 1.0.0
     */
    public int getCenterBlockX() {
        return (chunkX << 4) + 8;
    }

    /**
     * Returns the block Z coordinate at the center of this chunk.
     *
     * @return the center block Z coordinate
     * @since 1.0.0
     */
    public int getCenterBlockZ() {
        return (chunkZ << 4) + 8;
    }

    /**
     * Checks if this represents the global region.
     *
     * @return true if this is the global region
     * @since 1.0.0
     */
    public boolean isGlobal() {
        return global;
    }

    /**
     * Checks if the current thread owns this region.
     *
     * <p>In Folia, only the owning thread can safely modify entities
     * and blocks within a region. This method checks if it's safe to
     * perform modifications on the current thread.
     *
     * @return true if the current thread owns this region
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentThread() {
        if (global) {
            return FoliaDetector.isGlobalTickThread();
        }

        if (!FoliaDetector.isFolia()) {
            // On non-Folia servers, main thread owns everything
            return Thread.currentThread().getName().equals("Server thread");
        }

        if (worldHandle == null) {
            return false;
        }

        return FoliaDetector.isOwnedByCurrentRegion(worldHandle, chunkX, chunkZ);
    }

    /**
     * Checks if this region contains the specified location.
     *
     * <p>Note that a region may span multiple chunks, but this method
     * only checks the specific chunk this context was created for.
     *
     * @param location the location to check
     * @return true if the location is in the same chunk as this context
     * @since 1.0.0
     */
    public boolean contains(@NotNull UnifiedLocation location) {
        if (global) {
            return true;
        }
        if (world == null || location.world() == null) {
            return false;
        }
        return world.equals(location.world())
                && chunkX == location.getChunkX()
                && chunkZ == location.getChunkZ();
    }

    /**
     * Creates a new context for a neighboring chunk.
     *
     * @param offsetX the chunk X offset
     * @param offsetZ the chunk Z offset
     * @return a new RegionContext for the neighboring chunk
     * @throws IllegalStateException if this is the global region
     * @since 1.0.0
     */
    @NotNull
    public RegionContext neighbor(int offsetX, int offsetZ) {
        if (global) {
            throw new IllegalStateException("Cannot get neighbor of global region");
        }
        if (world == null) {
            throw new IllegalStateException("Cannot get neighbor without world");
        }
        return new RegionContext(world, chunkX + offsetX, chunkZ + offsetZ, false);
    }

    /**
     * Returns the distance in chunks to another region context.
     *
     * @param other the other region context
     * @return the Chebyshev distance in chunks, or -1 if incomparable
     * @since 1.0.0
     */
    public int distanceTo(@NotNull RegionContext other) {
        if (global || other.global) {
            return -1;
        }
        if (!Objects.equals(world, other.world)) {
            return -1;
        }
        return Math.max(Math.abs(chunkX - other.chunkX), Math.abs(chunkZ - other.chunkZ));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RegionContext other)) return false;
        if (global && other.global) return true;
        return chunkX == other.chunkX
                && chunkZ == other.chunkZ
                && global == other.global
                && Objects.equals(world, other.world);
    }

    @Override
    public int hashCode() {
        if (global) {
            return 0;
        }
        return Objects.hash(world, chunkX, chunkZ);
    }

    @Override
    public String toString() {
        if (global) {
            return "RegionContext[global]";
        }
        String worldName = world != null ? world.getName() : "null";
        return String.format("RegionContext[world=%s, chunk=(%d, %d)]",
                worldName, chunkX, chunkZ);
    }
}
