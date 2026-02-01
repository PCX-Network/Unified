/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * A block populator that can modify chunks after generation.
 *
 * <p>Block populators run after the main chunk generation phases and can
 * safely modify blocks across chunk boundaries. They are useful for adding
 * features that span multiple chunks, like large structures or ore veins.
 *
 * <h2>Execution Order:</h2>
 * <ol>
 *   <li>Chunk noise generation</li>
 *   <li>Surface generation</li>
 *   <li>Bedrock generation</li>
 *   <li>Cave generation</li>
 *   <li>Feature population (vanilla)</li>
 *   <li>Block populators (custom)</li>
 * </ol>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class OrePopulator extends BlockPopulator {
 *
 *     @Override
 *     public void populate(WorldInfo worldInfo, Random random,
 *                          int chunkX, int chunkZ, LimitedRegion region) {
 *         // Generate 8 ore veins per chunk
 *         for (int i = 0; i < 8; i++) {
 *             // Random position in chunk
 *             int x = (chunkX << 4) + random.nextInt(16);
 *             int y = random.nextInt(32) + 8;
 *             int z = (chunkZ << 4) + random.nextInt(16);
 *
 *             // Generate vein
 *             generateOreVein(region, x, y, z, random);
 *         }
 *     }
 *
 *     private void generateOreVein(LimitedRegion region, int x, int y, int z, Random random) {
 *         for (int i = 0; i < 9; i++) {
 *             int dx = x + random.nextInt(3) - 1;
 *             int dy = y + random.nextInt(3) - 1;
 *             int dz = z + random.nextInt(3) - 1;
 *
 *             if (region.isInRegion(dx, dy, dz) &&
 *                 region.getBlock(dx, dy, dz).equals(BlockType.STONE)) {
 *                 region.setBlock(dx, dy, dz, BlockType.DIAMOND_ORE);
 *             }
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
public abstract class BlockPopulator {

    /**
     * Default constructor for block populators.
     *
     * @since 1.0.0
     */
    protected BlockPopulator() {
    }

    /**
     * Populates a chunk with additional blocks.
     *
     * <p>This method is called after the main generation phases. Use the
     * LimitedRegion to safely access and modify blocks across chunk boundaries.
     *
     * @param worldInfo the world information
     * @param random    random number generator seeded for this chunk
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param region    the limited region for safe block access
     * @since 1.0.0
     */
    public abstract void populate(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                  int chunkX, int chunkZ, @NotNull LimitedRegion region);

    /**
     * Legacy populate method for compatibility.
     *
     * <p>This method is called on platforms that don't support LimitedRegion.
     * Override this for maximum compatibility.
     *
     * @param world   the world being populated
     * @param random  random number generator
     * @param chunkX  the chunk X coordinate
     * @param chunkZ  the chunk Z coordinate
     * @since 1.0.0
     * @deprecated Use {@link #populate(WorldInfo, Random, int, int, LimitedRegion)} instead
     */
    @Deprecated
    public void populate(@NotNull UnifiedWorld world, @NotNull Random random,
                         int chunkX, int chunkZ) {
        // Override for legacy support
    }

    /**
     * Creates a random instance seeded for the given chunk.
     *
     * @param worldInfo the world info containing the seed
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @return a seeded random instance
     * @since 1.0.0
     */
    @NotNull
    protected Random createChunkRandom(@NotNull WorldInfo worldInfo, int chunkX, int chunkZ) {
        long seed = worldInfo.getSeed();
        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
        return new Random(chunkSeed);
    }
}
