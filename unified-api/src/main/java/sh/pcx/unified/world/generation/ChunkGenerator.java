/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Abstract base class for custom chunk generators.
 *
 * <p>ChunkGenerator provides the foundation for creating custom world generation.
 * Implementations can override various generation phases to customize terrain,
 * biomes, structures, and spawn behavior.
 *
 * <h2>Generation Phases:</h2>
 * <ol>
 *   <li>{@link #generateNoise} - Generate base terrain shape</li>
 *   <li>{@link #generateSurface} - Apply surface blocks (grass, sand, etc.)</li>
 *   <li>{@link #generateBedrock} - Place bedrock layer</li>
 *   <li>{@link #generateCaves} - Carve caves and caverns</li>
 *   <li>{@link #populate} - Add features (trees, ores, etc.)</li>
 * </ol>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class IslandGenerator extends ChunkGenerator {
 *
 *     @Override
 *     public void generateNoise(WorldInfo worldInfo, Random random,
 *                               int chunkX, int chunkZ, ChunkData chunkData) {
 *         // Generate island at spawn
 *         if (chunkX == 0 && chunkZ == 0) {
 *             generateSpawnIsland(chunkData);
 *         }
 *     }
 *
 *     private void generateSpawnIsland(ChunkData data) {
 *         int centerX = 8, centerZ = 8;
 *         int radius = 5;
 *
 *         for (int x = 0; x < 16; x++) {
 *             for (int z = 0; z < 16; z++) {
 *                 double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
 *                 if (distance <= radius) {
 *                     int height = (int) (64 - distance);
 *                     for (int y = 60; y <= height; y++) {
 *                         if (y == height) {
 *                             data.setBlock(x, y, z, BlockType.GRASS_BLOCK);
 *                         } else if (y >= height - 3) {
 *                             data.setBlock(x, y, z, BlockType.DIRT);
 *                         } else {
 *                             data.setBlock(x, y, z, BlockType.STONE);
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 *
 *     @Override
 *     public Location getFixedSpawnLocation(WorldInfo worldInfo, Random random) {
 *         return new Location(null, 8, 65, 8);
 *     }
 * }
 *
 * // Register and use
 * worlds.registerGenerator("island", new IslandGenerator());
 * World skyblock = worlds.create("skyblock")
 *     .generator("island")
 *     .create();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 * @see ChunkData
 * @see WorldInfo
 */
public abstract class ChunkGenerator {

    /**
     * Settings passed to this generator during world creation.
     */
    protected WorldCreator.GeneratorSettings settings;

    /**
     * Default constructor for chunk generators.
     *
     * @since 1.0.0
     */
    protected ChunkGenerator() {
    }

    /**
     * Initializes the generator with settings from world creation.
     *
     * <p>This method is called automatically before any generation occurs.
     * Override to validate or process settings.
     *
     * @param settings the generator settings
     * @since 1.0.0
     */
    public void initialize(@NotNull WorldCreator.GeneratorSettings settings) {
        this.settings = settings;
    }

    /**
     * Gets the settings for this generator.
     *
     * @return the settings, or null if not initialized
     * @since 1.0.0
     */
    @Nullable
    public WorldCreator.GeneratorSettings getSettings() {
        return settings;
    }

    // ==================== Generation Phases ====================

    /**
     * Generates the base terrain noise/shape for a chunk.
     *
     * <p>This is the first generation phase. Use this to create the basic
     * terrain shape by setting blocks in the chunk data.
     *
     * @param worldInfo information about the world being generated
     * @param random    random number generator seeded for this chunk
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param chunkData the chunk data to modify
     * @since 1.0.0
     */
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Default: no generation
    }

    /**
     * Generates the surface layer for a chunk.
     *
     * <p>Called after noise generation. Use this to apply surface blocks
     * like grass, sand, or snow based on biome and height.
     *
     * @param worldInfo information about the world being generated
     * @param random    random number generator seeded for this chunk
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param chunkData the chunk data to modify
     * @since 1.0.0
     */
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Default: no surface modification
    }

    /**
     * Generates the bedrock layer.
     *
     * <p>Called after surface generation. Override to customize bedrock placement.
     *
     * @param worldInfo information about the world being generated
     * @param random    random number generator seeded for this chunk
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param chunkData the chunk data to modify
     * @since 1.0.0
     */
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Default: no bedrock
    }

    /**
     * Carves caves and caverns.
     *
     * <p>Called after bedrock generation. Override to add custom cave systems.
     *
     * @param worldInfo information about the world being generated
     * @param random    random number generator seeded for this chunk
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param chunkData the chunk data to modify
     * @since 1.0.0
     */
    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Default: no caves
    }

    /**
     * Populates the chunk with features like trees, ores, and structures.
     *
     * <p>Called as the final generation phase. Use this to add decorations
     * and features to the generated terrain.
     *
     * @param worldInfo information about the world being generated
     * @param random    random number generator seeded for this chunk
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @since 1.0.0
     */
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random,
                         int chunkX, int chunkZ) {
        // Default: no population
    }

    // ==================== Spawn Location ====================

    /**
     * Gets the default spawn point for this generator.
     *
     * <p>If this returns non-null, the world will use this as its spawn point
     * instead of searching for a suitable location.
     *
     * @param worldInfo information about the world
     * @param random    random number generator
     * @return the spawn location, or null to use default search behavior
     * @since 1.0.0
     */
    @Nullable
    public UnifiedLocation getFixedSpawnLocation(@NotNull WorldInfo worldInfo, @NotNull Random random) {
        return null;
    }

    /**
     * Checks if a location is a valid spawn point.
     *
     * <p>Used when searching for spawn points if {@link #getFixedSpawnLocation}
     * returns null.
     *
     * @param world    the world
     * @param location the location to check
     * @return true if this is a valid spawn location
     * @since 1.0.0
     */
    public boolean isValidSpawn(@NotNull UnifiedWorld world, @NotNull UnifiedLocation location) {
        return true;
    }

    // ==================== Biome Support ====================

    /**
     * Gets the biome provider for this generator.
     *
     * <p>Override to provide custom biome distribution. If null, vanilla
     * biome generation will be used.
     *
     * @param worldInfo information about the world
     * @return the biome provider, or null for vanilla
     * @since 1.0.0
     */
    @Nullable
    public BiomeProvider getBiomeProvider(@NotNull WorldInfo worldInfo) {
        return null;
    }

    // ==================== Structure Support ====================

    /**
     * Checks if default structures should be generated.
     *
     * @return true to generate vanilla structures
     * @since 1.0.0
     */
    public boolean shouldGenerateStructures() {
        return false;
    }

    /**
     * Checks if default caves should be generated.
     *
     * @return true to generate vanilla caves
     * @since 1.0.0
     */
    public boolean shouldGenerateCaves() {
        return false;
    }

    /**
     * Checks if default decorations should be generated.
     *
     * @return true to generate vanilla decorations
     * @since 1.0.0
     */
    public boolean shouldGenerateDecorations() {
        return false;
    }

    /**
     * Checks if default mob spawning should occur.
     *
     * @return true to enable vanilla mob spawning
     * @since 1.0.0
     */
    public boolean shouldGenerateMobs() {
        return true;
    }

    // ==================== Block Populators ====================

    /**
     * Gets additional block populators for this generator.
     *
     * <p>Block populators run after chunk generation and can modify
     * blocks across chunk boundaries.
     *
     * @param worldInfo information about the world
     * @return list of block populators
     * @since 1.0.0
     */
    @NotNull
    public List<BlockPopulator> getBlockPopulators(@NotNull WorldInfo worldInfo) {
        return Collections.emptyList();
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the base height at the given coordinates.
     *
     * <p>Override to provide consistent height data for structure placement.
     *
     * @param worldInfo information about the world
     * @param x         the world X coordinate
     * @param z         the world Z coordinate
     * @return the height at this location
     * @since 1.0.0
     */
    public int getBaseHeight(@NotNull WorldInfo worldInfo, int x, int z) {
        return worldInfo.getMinHeight();
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
