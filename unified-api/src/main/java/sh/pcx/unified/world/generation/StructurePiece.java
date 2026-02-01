/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Base class for custom structure pieces in jigsaw structures.
 *
 * <p>Structure pieces are the building blocks of complex structures like
 * villages. Each piece defines how it places blocks and connects to other
 * pieces via jigsaw connections.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class TowerBasePiece extends StructurePiece {
 *
 *     @Override
 *     public void generate(GenerationContext context) {
 *         // Generate the tower base
 *         int baseY = context.getY();
 *         for (int x = -2; x <= 2; x++) {
 *             for (int z = -2; z <= 2; z++) {
 *                 for (int y = 0; y < 5; y++) {
 *                     if (x == -2 || x == 2 || z == -2 || z == 2) {
 *                         context.setBlock(x, baseY + y, z, BlockType.STONE_BRICKS);
 *                     } else if (y == 0) {
 *                         context.setBlock(x, baseY + y, z, BlockType.STONE);
 *                     }
 *                 }
 *             }
 *         }
 *     }
 *
 *     @Override
 *     public int getWidth() { return 5; }
 *
 *     @Override
 *     public int getHeight() { return 5; }
 *
 *     @Override
 *     public int getDepth() { return 5; }
 * }
 *
 * // Register the piece
 * structures.registerPiece("myplugin:tower_base", TowerBasePiece.class);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see JigsawStructure
 * @see StructureService
 */
public abstract class StructurePiece {

    /**
     * Default constructor for structure pieces.
     *
     * @since 1.0.0
     */
    protected StructurePiece() {
    }

    /**
     * Generates this structure piece.
     *
     * @param context the generation context
     * @since 1.0.0
     */
    public abstract void generate(@NotNull GenerationContext context);

    /**
     * Gets the width (X size) of this piece.
     *
     * @return the width
     * @since 1.0.0
     */
    public abstract int getWidth();

    /**
     * Gets the height (Y size) of this piece.
     *
     * @return the height
     * @since 1.0.0
     */
    public abstract int getHeight();

    /**
     * Gets the depth (Z size) of this piece.
     *
     * @return the depth
     * @since 1.0.0
     */
    public abstract int getDepth();

    /**
     * Called after generation to allow post-processing.
     *
     * @param context the generation context
     * @since 1.0.0
     */
    public void postGenerate(@NotNull GenerationContext context) {
        // Override for post-processing
    }

    /**
     * Context provided during structure piece generation.
     *
     * @since 1.0.0
     */
    public interface GenerationContext {

        /**
         * Gets the X coordinate of the piece origin.
         *
         * @return the X coordinate
         * @since 1.0.0
         */
        int getX();

        /**
         * Gets the Y coordinate of the piece origin.
         *
         * @return the Y coordinate
         * @since 1.0.0
         */
        int getY();

        /**
         * Gets the Z coordinate of the piece origin.
         *
         * @return the Z coordinate
         * @since 1.0.0
         */
        int getZ();

        /**
         * Gets the rotation applied to this piece.
         *
         * @return the rotation
         * @since 1.0.0
         */
        @NotNull
        StructureService.Rotation getRotation();

        /**
         * Gets the random number generator for this piece.
         *
         * @return the random
         * @since 1.0.0
         */
        @NotNull
        Random getRandom();

        /**
         * Gets the world info.
         *
         * @return the world info
         * @since 1.0.0
         */
        @NotNull
        WorldInfo getWorldInfo();

        /**
         * Sets a block relative to the piece origin.
         *
         * <p>Coordinates are automatically transformed based on rotation.
         *
         * @param relX      X offset from origin
         * @param relY      Y offset from origin
         * @param relZ      Z offset from origin
         * @param blockType the block type
         * @since 1.0.0
         */
        void setBlock(int relX, int relY, int relZ, @NotNull BlockType blockType);

        /**
         * Sets a block with data relative to the piece origin.
         *
         * @param relX      X offset from origin
         * @param relY      Y offset from origin
         * @param relZ      Z offset from origin
         * @param blockType the block type
         * @param data      the block data
         * @since 1.0.0
         */
        void setBlock(int relX, int relY, int relZ, @NotNull BlockType blockType, @NotNull BlockData data);

        /**
         * Gets a block relative to the piece origin.
         *
         * @param relX X offset from origin
         * @param relY Y offset from origin
         * @param relZ Z offset from origin
         * @return the block type
         * @since 1.0.0
         */
        @NotNull
        BlockType getBlock(int relX, int relY, int relZ);

        /**
         * Fills a region with a block type.
         *
         * @param minX      minimum X offset
         * @param minY      minimum Y offset
         * @param minZ      minimum Z offset
         * @param maxX      maximum X offset
         * @param maxY      maximum Y offset
         * @param maxZ      maximum Z offset
         * @param blockType the block type
         * @since 1.0.0
         */
        void fill(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                  @NotNull BlockType blockType);

        /**
         * Fills the outline of a region.
         *
         * @param minX      minimum X offset
         * @param minY      minimum Y offset
         * @param minZ      minimum Z offset
         * @param maxX      maximum X offset
         * @param maxY      maximum Y offset
         * @param maxZ      maximum Z offset
         * @param blockType the block type
         * @since 1.0.0
         */
        void fillOutline(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                         @NotNull BlockType blockType);

        /**
         * Spawns an entity relative to the piece origin.
         *
         * @param relX       X offset from origin
         * @param relY       Y offset from origin
         * @param relZ       Z offset from origin
         * @param entityType the entity type key
         * @since 1.0.0
         */
        void spawnEntity(double relX, double relY, double relZ, @NotNull String entityType);

        /**
         * Places a loot chest with the specified loot table.
         *
         * @param relX      X offset from origin
         * @param relY      Y offset from origin
         * @param relZ      Z offset from origin
         * @param lootTable the loot table key
         * @since 1.0.0
         */
        void placeLootChest(int relX, int relY, int relZ, @NotNull String lootTable);
    }
}
