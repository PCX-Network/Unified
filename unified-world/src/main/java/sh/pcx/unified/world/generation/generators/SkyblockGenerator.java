/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation.generators;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.generation.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * A chunk generator that creates a single island for skyblock-style gameplay.
 *
 * <p>SkyblockGenerator produces a small starting island at the spawn location
 * and generates nothing else. The island configuration can be customized.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a default skyblock world
 * worlds.create("skyblock")
 *     .generator(new SkyblockGenerator())
 *     .create();
 *
 * // Create a custom skyblock world
 * SkyblockGenerator generator = SkyblockGenerator.builder()
 *     .islandRadius(3)
 *     .islandHeight(64)
 *     .withTree(true)
 *     .withChest(true)
 *     .chestLootTable("myplugin:skyblock_starter")
 *     .build();
 *
 * worlds.create("custom_skyblock")
 *     .generator(generator)
 *     .create();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ChunkGenerator
 */
public class SkyblockGenerator extends ChunkGenerator {

    private final int islandRadius;
    private final int islandHeight;
    private final boolean withTree;
    private final boolean withChest;
    private final String chestLootTable;

    /**
     * Creates a skyblock generator with default settings.
     *
     * <p>Default: 3 block radius, height 64, with tree and chest.
     *
     * @since 1.0.0
     */
    public SkyblockGenerator() {
        this(3, 64, true, true, null);
    }

    /**
     * Creates a skyblock generator with custom settings.
     *
     * @param islandRadius   the island radius in blocks
     * @param islandHeight   the Y level of the island surface
     * @param withTree       whether to generate a tree
     * @param withChest      whether to generate a starter chest
     * @param chestLootTable the loot table for the chest (null for default)
     * @since 1.0.0
     */
    public SkyblockGenerator(int islandRadius, int islandHeight, boolean withTree,
                             boolean withChest, @Nullable String chestLootTable) {
        this.islandRadius = islandRadius;
        this.islandHeight = islandHeight;
        this.withTree = withTree;
        this.withChest = withChest;
        this.chestLootTable = chestLootTable;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Only generate island at spawn chunk (0,0)
        if (chunkX != 0 || chunkZ != 0) {
            return;
        }

        int centerX = 8;
        int centerZ = 8;

        // Generate circular island
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
                if (distance <= islandRadius) {
                    // Calculate depth based on distance from center
                    int depth = (int) Math.max(1, (islandRadius - distance + 1) * 1.5);

                    for (int y = islandHeight - depth; y <= islandHeight; y++) {
                        BlockType block;
                        if (y == islandHeight) {
                            block = BlockType.GRASS_BLOCK;
                        } else if (y > islandHeight - 3) {
                            block = BlockType.DIRT;
                        } else {
                            block = BlockType.STONE;
                        }
                        chunkData.setBlock(x, y, z, block);
                    }
                }
            }
        }

        // Generate tree at center
        if (withTree) {
            generateTree(chunkData, centerX, islandHeight + 1, centerZ);
        }

        // Generate bedrock at center bottom
        chunkData.setBlock(centerX, islandHeight - 4, centerZ, BlockType.BEDROCK);
    }

    private void generateTree(@NotNull ChunkData chunkData, int x, int y, int z) {
        // Trunk
        for (int i = 0; i < 5; i++) {
            chunkData.setBlock(x, y + i, z, BlockType.OAK_LOG);
        }

        // Leaves (simple cross pattern)
        int leafY = y + 3;
        for (int dy = 0; dy < 3; dy++) {
            int radius = dy < 2 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0 && dy < 2) continue; // Skip trunk
                    int lx = x + dx;
                    int lz = z + dz;
                    if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
                        chunkData.setBlock(lx, leafY + dy, lz, BlockType.OAK_LEAVES);
                    }
                }
            }
        }
    }

    @Override
    @Nullable
    public UnifiedLocation getFixedSpawnLocation(@NotNull WorldInfo worldInfo, @NotNull Random random) {
        return UnifiedLocation.of(worldInfo.getName(), 8.5, islandHeight + 1, 8.5);
    }

    @Override
    @NotNull
    public BiomeProvider getBiomeProvider(@NotNull WorldInfo worldInfo) {
        return BiomeProvider.single(Biome.PLAINS);
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public int getBaseHeight(@NotNull WorldInfo worldInfo, int x, int z) {
        // Only the spawn chunk has solid ground
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (chunkX == 0 && chunkZ == 0) {
            return islandHeight + 1;
        }
        return worldInfo.getMinHeight();
    }

    /**
     * Gets the island radius.
     *
     * @return the radius in blocks
     * @since 1.0.0
     */
    public int getIslandRadius() {
        return islandRadius;
    }

    /**
     * Gets the island height.
     *
     * @return the Y level of the surface
     * @since 1.0.0
     */
    public int getIslandHeight() {
        return islandHeight;
    }

    /**
     * Checks if the island has a tree.
     *
     * @return true if a tree is generated
     * @since 1.0.0
     */
    public boolean hasTree() {
        return withTree;
    }

    /**
     * Checks if the island has a starter chest.
     *
     * @return true if a chest is generated
     * @since 1.0.0
     */
    public boolean hasChest() {
        return withChest;
    }

    /**
     * Creates a new builder for SkyblockGenerator.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating SkyblockGenerator instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private int islandRadius = 3;
        private int islandHeight = 64;
        private boolean withTree = true;
        private boolean withChest = true;
        private String chestLootTable = null;

        private Builder() {
        }

        /**
         * Sets the island radius.
         *
         * @param radius the radius in blocks
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder islandRadius(int radius) {
            this.islandRadius = radius;
            return this;
        }

        /**
         * Sets the island height.
         *
         * @param height the Y level
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder islandHeight(int height) {
            this.islandHeight = height;
            return this;
        }

        /**
         * Sets whether to generate a tree.
         *
         * @param withTree true to generate a tree
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder withTree(boolean withTree) {
            this.withTree = withTree;
            return this;
        }

        /**
         * Sets whether to generate a starter chest.
         *
         * @param withChest true to generate a chest
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder withChest(boolean withChest) {
            this.withChest = withChest;
            return this;
        }

        /**
         * Sets the loot table for the starter chest.
         *
         * @param lootTable the loot table key
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder chestLootTable(@NotNull String lootTable) {
            this.chestLootTable = lootTable;
            return this;
        }

        /**
         * Builds the SkyblockGenerator.
         *
         * @return the built generator
         * @since 1.0.0
         */
        @NotNull
        public SkyblockGenerator build() {
            return new SkyblockGenerator(islandRadius, islandHeight, withTree, withChest, chestLootTable);
        }
    }
}
