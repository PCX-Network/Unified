/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provides limited access to a region of blocks around a chunk during population.
 *
 * <p>LimitedRegion allows block populators to safely access blocks beyond chunk
 * boundaries. The region is typically the target chunk plus a buffer of adjacent
 * chunks to allow for cross-chunk features.
 *
 * <h2>Region Bounds:</h2>
 * <p>The region typically extends 8 chunks in each direction from the center chunk,
 * allowing populators to place structures that span multiple chunks without
 * causing concurrent modification issues.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Override
 * public void populate(WorldInfo worldInfo, Random random,
 *                      int chunkX, int chunkZ, LimitedRegion region) {
 *     // Check if coordinates are within the region
 *     int x = (chunkX << 4) + 8;
 *     int z = (chunkZ << 4) + 8;
 *
 *     if (region.isInRegion(x, 64, z)) {
 *         // Get existing block
 *         BlockType existing = region.getBlock(x, 64, z);
 *
 *         if (existing.equals(BlockType.GRASS_BLOCK)) {
 *             // Set a new block
 *             region.setBlock(x, 65, z, BlockType.OAK_LOG);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see BlockPopulator
 */
public interface LimitedRegion {

    /**
     * Gets the minimum X coordinate (block) of this region.
     *
     * @return the minimum X coordinate
     * @since 1.0.0
     */
    int getMinX();

    /**
     * Gets the minimum Y coordinate (block) of this region.
     *
     * @return the minimum Y coordinate
     * @since 1.0.0
     */
    int getMinY();

    /**
     * Gets the minimum Z coordinate (block) of this region.
     *
     * @return the minimum Z coordinate
     * @since 1.0.0
     */
    int getMinZ();

    /**
     * Gets the maximum X coordinate (block) of this region.
     *
     * @return the maximum X coordinate
     * @since 1.0.0
     */
    int getMaxX();

    /**
     * Gets the maximum Y coordinate (block) of this region.
     *
     * @return the maximum Y coordinate
     * @since 1.0.0
     */
    int getMaxY();

    /**
     * Gets the maximum Z coordinate (block) of this region.
     *
     * @return the maximum Z coordinate
     * @since 1.0.0
     */
    int getMaxZ();

    /**
     * Gets the buffer size in chunks around the center chunk.
     *
     * @return the buffer size
     * @since 1.0.0
     */
    int getBuffer();

    /**
     * Checks if the specified coordinates are within this region.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return true if the coordinates are in the region
     * @since 1.0.0
     */
    default boolean isInRegion(int x, int y, int z) {
        return x >= getMinX() && x <= getMaxX() &&
               y >= getMinY() && y <= getMaxY() &&
               z >= getMinZ() && z <= getMaxZ();
    }

    /**
     * Gets the block type at the specified coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the block type
     * @throws IllegalArgumentException if coordinates are outside the region
     * @since 1.0.0
     */
    @NotNull
    BlockType getBlock(int x, int y, int z);

    /**
     * Gets the block data at the specified coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the block data, or null if none
     * @throws IllegalArgumentException if coordinates are outside the region
     * @since 1.0.0
     */
    @Nullable
    BlockData getBlockData(int x, int y, int z);

    /**
     * Sets the block type at the specified coordinates.
     *
     * @param x         the X coordinate
     * @param y         the Y coordinate
     * @param z         the Z coordinate
     * @param blockType the block type to set
     * @throws IllegalArgumentException if coordinates are outside the region
     * @since 1.0.0
     */
    void setBlock(int x, int y, int z, @NotNull BlockType blockType);

    /**
     * Sets the block type and data at the specified coordinates.
     *
     * @param x         the X coordinate
     * @param y         the Y coordinate
     * @param z         the Z coordinate
     * @param blockType the block type to set
     * @param data      the block data to set
     * @throws IllegalArgumentException if coordinates are outside the region
     * @since 1.0.0
     */
    void setBlock(int x, int y, int z, @NotNull BlockType blockType, @Nullable BlockData data);

    /**
     * Gets the biome at the specified coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the biome
     * @throws IllegalArgumentException if coordinates are outside the region
     * @since 1.0.0
     */
    @NotNull
    Biome getBiome(int x, int y, int z);

    /**
     * Sets the biome at the specified coordinates.
     *
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     * @param biome the biome to set
     * @throws IllegalArgumentException if coordinates are outside the region
     * @since 1.0.0
     */
    void setBiome(int x, int y, int z, @NotNull Biome biome);

    /**
     * Spawns an entity at the specified coordinates.
     *
     * @param x          the X coordinate
     * @param y          the Y coordinate
     * @param z          the Z coordinate
     * @param entityType the entity type key (e.g., "minecraft:zombie")
     * @throws IllegalArgumentException if coordinates are outside the region
     * @since 1.0.0
     */
    void spawnEntity(int x, int y, int z, @NotNull String entityType);

    /**
     * Gets the entities to be spawned in this region.
     *
     * <p>Entities spawned via {@link #spawnEntity} are queued and spawned
     * after population completes.
     *
     * @return a list of queued entity spawns
     * @since 1.0.0
     */
    @NotNull
    List<EntitySpawn> getScheduledEntities();

    /**
     * Gets the center chunk X coordinate of this region.
     *
     * @return the center chunk X
     * @since 1.0.0
     */
    int getCenterChunkX();

    /**
     * Gets the center chunk Z coordinate of this region.
     *
     * @return the center chunk Z
     * @since 1.0.0
     */
    int getCenterChunkZ();

    /**
     * Gets the underlying platform-specific limited region object.
     *
     * @param <T> the expected platform type
     * @return the platform-specific limited region
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();

    /**
     * Represents a scheduled entity spawn.
     *
     * @param x          the X coordinate
     * @param y          the Y coordinate
     * @param z          the Z coordinate
     * @param entityType the entity type key
     * @since 1.0.0
     */
    record EntitySpawn(double x, double y, double z, @NotNull String entityType) {}
}
