/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation.generators;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.generation.BiomeProvider;
import sh.pcx.unified.world.generation.ChunkData;
import sh.pcx.unified.world.generation.ChunkGenerator;
import sh.pcx.unified.world.generation.Biome;
import sh.pcx.unified.world.generation.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * A chunk generator that creates a completely empty void world.
 *
 * <p>VoidGenerator produces no blocks at all, creating a world of only air.
 * This is useful for lobbies, minigame arenas, and custom builds where
 * terrain would be in the way.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a void world
 * worlds.create("lobby")
 *     .generator(new VoidGenerator())
 *     .create();
 *
 * // Or register and use by ID
 * worlds.registerGenerator("void", new VoidGenerator());
 * worlds.create("arena")
 *     .generator("void")
 *     .create();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ChunkGenerator
 */
public class VoidGenerator extends ChunkGenerator {

    private final int spawnY;
    private final Biome biome;

    /**
     * Creates a void generator with default settings.
     *
     * <p>Uses spawn Y of 64 and THE_VOID biome.
     *
     * @since 1.0.0
     */
    public VoidGenerator() {
        this(64, Biome.THE_VOID);
    }

    /**
     * Creates a void generator with a custom spawn height.
     *
     * @param spawnY the Y level for the spawn location
     * @since 1.0.0
     */
    public VoidGenerator(int spawnY) {
        this(spawnY, Biome.THE_VOID);
    }

    /**
     * Creates a void generator with custom settings.
     *
     * @param spawnY the Y level for the spawn location
     * @param biome  the biome to use
     * @since 1.0.0
     */
    public VoidGenerator(int spawnY, @NotNull Biome biome) {
        this.spawnY = spawnY;
        this.biome = biome;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Generate nothing - void world
    }

    @Override
    @Nullable
    public UnifiedLocation getFixedSpawnLocation(@NotNull WorldInfo worldInfo, @NotNull Random random) {
        return UnifiedLocation.of(worldInfo.getName(), 0.5, spawnY, 0.5);
    }

    @Override
    @NotNull
    public BiomeProvider getBiomeProvider(@NotNull WorldInfo worldInfo) {
        return BiomeProvider.single(biome);
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
        return spawnY;
    }
}
