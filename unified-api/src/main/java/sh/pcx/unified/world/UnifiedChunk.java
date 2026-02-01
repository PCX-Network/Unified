/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Platform-agnostic interface representing a chunk in a Minecraft world.
 *
 * <p>A chunk is a 16x16 column of blocks extending from the world's minimum
 * to maximum Y level. This interface provides access to chunk loading,
 * unloading, and block access within the chunk.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a chunk from the world
 * UnifiedChunk chunk = world.getChunkAt(6, 12);
 *
 * // Check if chunk is loaded
 * if (chunk.isLoaded()) {
 *     // Access blocks in the chunk
 *     UnifiedBlock block = chunk.getBlock(8, 64, 8); // Relative to chunk
 *
 *     // Get chunk coordinates
 *     int chunkX = chunk.getX();
 *     int chunkZ = chunk.getZ();
 *
 *     // Force load the chunk
 *     chunk.setForceLoaded(true);
 * }
 *
 * // Load chunk asynchronously
 * chunk.load().thenAccept(success -> {
 *     if (success) {
 *         // Chunk is now loaded
 *     }
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Chunk loading and unloading should be performed on the appropriate thread.
 * For Folia, use region-aware scheduling when modifying chunks.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedWorld
 * @see UnifiedBlock
 */
public interface UnifiedChunk {

    /**
     * Returns the X coordinate of this chunk.
     *
     * <p>Chunk coordinates are world block coordinates divided by 16.
     *
     * @return the chunk x coordinate
     * @since 1.0.0
     */
    int getX();

    /**
     * Returns the Z coordinate of this chunk.
     *
     * <p>Chunk coordinates are world block coordinates divided by 16.
     *
     * @return the chunk z coordinate
     * @since 1.0.0
     */
    int getZ();

    /**
     * Returns the world this chunk is in.
     *
     * @return the chunk's world
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld getWorld();

    /**
     * Returns a block at the specified chunk-relative coordinates.
     *
     * @param x the x coordinate (0-15)
     * @param y the y coordinate (world min to max height)
     * @param z the z coordinate (0-15)
     * @return the block at the coordinates
     * @throws IllegalArgumentException if x or z is outside 0-15 range
     * @since 1.0.0
     */
    @NotNull
    UnifiedBlock getBlock(int x, int y, int z);

    /**
     * Checks if this chunk is currently loaded.
     *
     * @return true if the chunk is loaded
     * @since 1.0.0
     */
    boolean isLoaded();

    /**
     * Loads this chunk.
     *
     * @return a future that completes with true if the chunk was loaded
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> load();

    /**
     * Loads this chunk synchronously.
     *
     * @param generate whether to generate the chunk if it doesn't exist
     * @return true if the chunk is now loaded
     * @since 1.0.0
     */
    boolean load(boolean generate);

    /**
     * Unloads this chunk.
     *
     * @param save whether to save the chunk before unloading
     * @return true if the chunk was unloaded
     * @since 1.0.0
     */
    boolean unload(boolean save);

    /**
     * Unloads this chunk, saving if modified.
     *
     * @return true if the chunk was unloaded
     * @since 1.0.0
     */
    boolean unload();

    /**
     * Checks if this chunk is force-loaded.
     *
     * <p>Force-loaded chunks remain loaded even without players nearby.
     *
     * @return true if the chunk is force-loaded
     * @since 1.0.0
     */
    boolean isForceLoaded();

    /**
     * Sets whether this chunk should be force-loaded.
     *
     * @param forceLoaded true to force-load the chunk
     * @since 1.0.0
     */
    void setForceLoaded(boolean forceLoaded);

    /**
     * Checks if this chunk has been generated.
     *
     * @return true if the chunk has been generated
     * @since 1.0.0
     */
    boolean isGenerated();

    /**
     * Returns the inhabited time of this chunk in ticks.
     *
     * <p>This is the cumulative time that players have spent in this chunk,
     * used for local difficulty calculations.
     *
     * @return the inhabited time in ticks
     * @since 1.0.0
     */
    long getInhabitedTime();

    /**
     * Sets the inhabited time of this chunk.
     *
     * @param time the inhabited time in ticks
     * @since 1.0.0
     */
    void setInhabitedTime(long time);

    /**
     * Checks if entities are loaded in this chunk.
     *
     * <p>In some versions, entity loading is separate from chunk loading.
     *
     * @return true if entities are loaded
     * @since 1.0.0
     */
    boolean areEntitiesLoaded();

    /**
     * Returns the center location of this chunk at the specified Y level.
     *
     * @param y the y coordinate
     * @return the center location
     * @since 1.0.0
     */
    @NotNull
    default UnifiedLocation getCenterLocation(double y) {
        return new UnifiedLocation(
                getWorld(),
                (getX() << 4) + 8,
                y,
                (getZ() << 4) + 8
        );
    }

    /**
     * Returns the minimum corner location of this chunk.
     *
     * @param y the y coordinate
     * @return the minimum corner location
     * @since 1.0.0
     */
    @NotNull
    default UnifiedLocation getMinLocation(double y) {
        return new UnifiedLocation(
                getWorld(),
                getX() << 4,
                y,
                getZ() << 4
        );
    }

    /**
     * Returns the maximum corner location of this chunk.
     *
     * @param y the y coordinate
     * @return the maximum corner location
     * @since 1.0.0
     */
    @NotNull
    default UnifiedLocation getMaxLocation(double y) {
        return new UnifiedLocation(
                getWorld(),
                (getX() << 4) + 15,
                y,
                (getZ() << 4) + 15
        );
    }

    /**
     * Checks if the given world coordinates are within this chunk.
     *
     * @param worldX the world x coordinate
     * @param worldZ the world z coordinate
     * @return true if the coordinates are within this chunk
     * @since 1.0.0
     */
    default boolean contains(int worldX, int worldZ) {
        return (worldX >> 4) == getX() && (worldZ >> 4) == getZ();
    }

    /**
     * Returns the underlying platform-specific chunk object.
     *
     * @param <T> the expected platform chunk type
     * @return the platform-specific chunk object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();
}
