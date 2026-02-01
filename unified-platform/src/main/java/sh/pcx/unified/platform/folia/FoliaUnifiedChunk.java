/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Region-aware chunk wrapper for Folia.
 *
 * <p>This class wraps a Bukkit Chunk object and provides thread-safe access
 * in Folia's multi-threaded environment. Chunk operations must be performed
 * on the correct region thread.
 *
 * <h2>Region Ownership</h2>
 * <p>A chunk belongs to a specific region in Folia. Only the thread that
 * owns that region can safely perform chunk operations. This wrapper
 * provides methods to check ownership and schedule operations.
 *
 * <h2>Async Operations</h2>
 * <p>Chunk loading should be done asynchronously to avoid blocking.
 * Use the async methods provided by this class.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedChunk
 * @see FoliaUnifiedWorld
 */
public final class FoliaUnifiedChunk implements UnifiedChunk {

    private static final Logger LOGGER = Logger.getLogger(FoliaUnifiedChunk.class.getName());

    /**
     * The underlying Bukkit Chunk object.
     */
    private final Object bukkitChunk;

    /**
     * The world this chunk belongs to.
     */
    private final FoliaUnifiedWorld world;

    /**
     * The platform provider.
     */
    private final FoliaPlatformProvider provider;

    /**
     * Cached chunk X coordinate.
     */
    private final int chunkX;

    /**
     * Cached chunk Z coordinate.
     */
    private final int chunkZ;

    /**
     * Constructs a new FoliaUnifiedChunk.
     *
     * @param bukkitChunk the Bukkit Chunk object
     * @param world the world this chunk belongs to
     * @param provider the platform provider
     * @since 1.0.0
     */
    public FoliaUnifiedChunk(@NotNull Object bukkitChunk,
                              @NotNull FoliaUnifiedWorld world,
                              @NotNull FoliaPlatformProvider provider) {
        this.bukkitChunk = bukkitChunk;
        this.world = world;
        this.provider = provider;

        try {
            this.chunkX = (int) bukkitChunk.getClass().getMethod("getX").invoke(bukkitChunk);
            this.chunkZ = (int) bukkitChunk.getClass().getMethod("getZ").invoke(bukkitChunk);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Bukkit Chunk object", e);
        }
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return world;
    }

    @Override
    @NotNull
    public UnifiedBlock getBlock(int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            throw new IllegalArgumentException("Chunk coordinates must be 0-15, got x=" + x + ", z=" + z);
        }

        try {
            Object block = bukkitChunk.getClass().getMethod("getBlock", int.class, int.class, int.class)
                    .invoke(bukkitChunk, x, y, z);
            return new FoliaUnifiedBlock(block, world, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get block at " + x + ", " + y + ", " + z, e);
        }
    }

    @Override
    public boolean isLoaded() {
        try {
            return (boolean) bukkitChunk.getClass().getMethod("isLoaded").invoke(bukkitChunk);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> load() {
        if (isLoaded()) {
            return CompletableFuture.completedFuture(true);
        }

        // Use async chunk loading
        return world.getChunkAtAsync(chunkX, chunkZ)
                .thenApply(chunk -> true)
                .exceptionally(e -> false);
    }

    @Override
    public boolean load(boolean generate) {
        try {
            return (boolean) bukkitChunk.getClass().getMethod("load", boolean.class)
                    .invoke(bukkitChunk, generate);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load chunk", e);
            return false;
        }
    }

    @Override
    public boolean unload(boolean save) {
        try {
            return (boolean) bukkitChunk.getClass().getMethod("unload", boolean.class)
                    .invoke(bukkitChunk, save);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to unload chunk", e);
            return false;
        }
    }

    @Override
    public boolean unload() {
        return unload(true);
    }

    @Override
    public boolean isForceLoaded() {
        try {
            return (boolean) bukkitChunk.getClass().getMethod("isForceLoaded").invoke(bukkitChunk);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setForceLoaded(boolean forceLoaded) {
        try {
            bukkitChunk.getClass().getMethod("setForceLoaded", boolean.class)
                    .invoke(bukkitChunk, forceLoaded);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set force loaded", e);
        }
    }

    @Override
    public boolean isGenerated() {
        try {
            // Paper API method
            return (boolean) bukkitChunk.getClass().getMethod("isGenerated").invoke(bukkitChunk);
        } catch (Exception e) {
            // Fallback: assume loaded chunks are generated
            return isLoaded();
        }
    }

    @Override
    public long getInhabitedTime() {
        try {
            return (long) bukkitChunk.getClass().getMethod("getInhabitedTime").invoke(bukkitChunk);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setInhabitedTime(long time) {
        try {
            bukkitChunk.getClass().getMethod("setInhabitedTime", long.class)
                    .invoke(bukkitChunk, time);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set inhabited time", e);
        }
    }

    @Override
    public boolean areEntitiesLoaded() {
        try {
            // Paper 1.17+ API
            return (boolean) bukkitChunk.getClass().getMethod("isEntitiesLoaded").invoke(bukkitChunk);
        } catch (Exception e) {
            // Fallback: assume entities are loaded with chunk
            return isLoaded();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getHandle() {
        return (T) bukkitChunk;
    }

    /**
     * Returns the region context for this chunk.
     *
     * @return the region context
     * @since 1.0.0
     */
    @NotNull
    public RegionContext getRegionContext() {
        return RegionContext.of(world, chunkX, chunkZ);
    }

    /**
     * Checks if the current thread owns this chunk's region.
     *
     * @return true if safe to modify this chunk on current thread
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentThread() {
        return getRegionContext().isOwnedByCurrentThread();
    }

    /**
     * Loads chunk entities asynchronously.
     *
     * <p>In Paper/Folia 1.17+, entities may be loaded separately from chunks.
     *
     * @return a future that completes when entities are loaded
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> loadEntitiesAsync() {
        if (areEntitiesLoaded()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            // Try Paper's async entity loading
            Object worldHandle = world.getHandle();
            Object chunkKey = bukkitChunk.getClass().getMethod("getChunkKey").invoke(bukkitChunk);

            // Paper method: getChunkAtAsync with entity loading
            return world.getChunkAtAsync(chunkX, chunkZ)
                    .thenAccept(chunk -> {});
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Gets the chunk key (a unique identifier for this chunk in the world).
     *
     * @return the chunk key
     * @since 1.0.0
     */
    public long getChunkKey() {
        return (long) chunkX << 32 | (chunkZ & 0xffffffffL);
    }

    /**
     * Checks if this chunk contains the given world coordinates.
     *
     * @param worldX the world X coordinate
     * @param worldZ the world Z coordinate
     * @return true if the coordinates are within this chunk
     * @since 1.0.0
     */
    public boolean containsWorld(int worldX, int worldZ) {
        return (worldX >> 4) == chunkX && (worldZ >> 4) == chunkZ;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FoliaUnifiedChunk other)) return false;
        return chunkX == other.chunkX
                && chunkZ == other.chunkZ
                && world.equals(other.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + chunkX;
        result = 31 * result + chunkZ;
        return result;
    }

    @Override
    public String toString() {
        return String.format("FoliaUnifiedChunk[world=%s, x=%d, z=%d]",
                world.getName(), chunkX, chunkZ);
    }
}
