/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the block data for a chunk during generation.
 *
 * <p>ChunkData provides methods for setting and getting blocks within a chunk
 * during the world generation process. All coordinates are relative to the
 * chunk origin (0-15 for X and Z).
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Override
 * public void generateNoise(WorldInfo worldInfo, Random random,
 *                           int chunkX, int chunkZ, ChunkData chunkData) {
 *     int minY = chunkData.getMinHeight();
 *     int maxY = chunkData.getMaxHeight();
 *
 *     for (int x = 0; x < 16; x++) {
 *         for (int z = 0; z < 16; z++) {
 *             // Set bedrock at bottom
 *             chunkData.setBlock(x, minY, z, BlockType.BEDROCK);
 *
 *             // Fill with stone up to y=60
 *             for (int y = minY + 1; y < 60; y++) {
 *                 chunkData.setBlock(x, y, z, BlockType.STONE);
 *             }
 *
 *             // Add dirt and grass on top
 *             chunkData.setBlock(x, 60, z, BlockType.DIRT);
 *             chunkData.setBlock(x, 61, z, BlockType.DIRT);
 *             chunkData.setBlock(x, 62, z, BlockType.DIRT);
 *             chunkData.setBlock(x, 63, z, BlockType.GRASS_BLOCK);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ChunkGenerator
 */
public interface ChunkData {

    /**
     * Gets the minimum height (Y coordinate) of this chunk.
     *
     * @return the minimum Y coordinate
     * @since 1.0.0
     */
    int getMinHeight();

    /**
     * Gets the maximum height (Y coordinate) of this chunk.
     *
     * @return the maximum Y coordinate
     * @since 1.0.0
     */
    int getMaxHeight();

    /**
     * Sets a block at the specified coordinates.
     *
     * @param x         the X coordinate (0-15)
     * @param y         the Y coordinate (minHeight to maxHeight)
     * @param z         the Z coordinate (0-15)
     * @param blockType the block type to set
     * @throws IllegalArgumentException if coordinates are out of range
     * @since 1.0.0
     */
    void setBlock(int x, int y, int z, @NotNull BlockType blockType);

    /**
     * Sets a block at the specified coordinates with block data.
     *
     * @param x         the X coordinate (0-15)
     * @param y         the Y coordinate (minHeight to maxHeight)
     * @param z         the Z coordinate (0-15)
     * @param blockType the block type to set
     * @param data      additional block data
     * @throws IllegalArgumentException if coordinates are out of range
     * @since 1.0.0
     */
    void setBlock(int x, int y, int z, @NotNull BlockType blockType, @Nullable BlockData data);

    /**
     * Gets the block type at the specified coordinates.
     *
     * @param x the X coordinate (0-15)
     * @param y the Y coordinate (minHeight to maxHeight)
     * @param z the Z coordinate (0-15)
     * @return the block type at the location
     * @throws IllegalArgumentException if coordinates are out of range
     * @since 1.0.0
     */
    @NotNull
    BlockType getBlock(int x, int y, int z);

    /**
     * Gets the block data at the specified coordinates.
     *
     * @param x the X coordinate (0-15)
     * @param y the Y coordinate (minHeight to maxHeight)
     * @param z the Z coordinate (0-15)
     * @return the block data at the location, or null if none
     * @throws IllegalArgumentException if coordinates are out of range
     * @since 1.0.0
     */
    @Nullable
    BlockData getBlockData(int x, int y, int z);

    /**
     * Sets a region of blocks to the same type.
     *
     * @param minX      the minimum X coordinate
     * @param minY      the minimum Y coordinate
     * @param minZ      the minimum Z coordinate
     * @param maxX      the maximum X coordinate
     * @param maxY      the maximum Y coordinate
     * @param maxZ      the maximum Z coordinate
     * @param blockType the block type to set
     * @since 1.0.0
     */
    void setRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, @NotNull BlockType blockType);

    /**
     * Fills the entire chunk with a single block type.
     *
     * @param blockType the block type to fill with
     * @since 1.0.0
     */
    default void fill(@NotNull BlockType blockType) {
        setRegion(0, getMinHeight(), 0, 15, getMaxHeight() - 1, 15, blockType);
    }

    /**
     * Sets a vertical column of blocks.
     *
     * @param x         the X coordinate (0-15)
     * @param z         the Z coordinate (0-15)
     * @param minY      the minimum Y coordinate
     * @param maxY      the maximum Y coordinate
     * @param blockType the block type to set
     * @since 1.0.0
     */
    default void setColumn(int x, int z, int minY, int maxY, @NotNull BlockType blockType) {
        setRegion(x, minY, z, x, maxY, z, blockType);
    }

    /**
     * Sets a horizontal layer of blocks at the specified Y level.
     *
     * @param y         the Y coordinate
     * @param blockType the block type to set
     * @since 1.0.0
     */
    default void setLayer(int y, @NotNull BlockType blockType) {
        setRegion(0, y, 0, 15, y, 15, blockType);
    }

    /**
     * Checks if the specified coordinates are within valid bounds.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return true if coordinates are valid
     * @since 1.0.0
     */
    default boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < 16 &&
               z >= 0 && z < 16 &&
               y >= getMinHeight() && y < getMaxHeight();
    }

    /**
     * Gets the underlying platform-specific chunk data object.
     *
     * @param <T> the expected platform type
     * @return the platform-specific chunk data
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();
}
