/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides biome distribution for custom world generation.
 *
 * <p>BiomeProvider determines which biomes are placed at each location
 * during world generation. Implementations can create custom biome
 * distributions, single-biome worlds, or modify vanilla biome placement.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class IslandBiomeProvider extends BiomeProvider {
 *
 *     @Override
 *     public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
 *         // Use plains for the island, ocean everywhere else
 *         double distance = Math.sqrt(x * x + z * z);
 *         if (distance < 100) {
 *             return Biome.PLAINS;
 *         }
 *         return Biome.OCEAN;
 *     }
 *
 *     @Override
 *     public List<Biome> getBiomes(WorldInfo worldInfo) {
 *         return List.of(Biome.PLAINS, Biome.OCEAN);
 *     }
 * }
 *
 * // Use in a chunk generator
 * @Override
 * public BiomeProvider getBiomeProvider(WorldInfo worldInfo) {
 *     return new IslandBiomeProvider();
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ChunkGenerator
 * @see BiomeService
 */
public abstract class BiomeProvider {

    /**
     * Default constructor for biome providers.
     *
     * @since 1.0.0
     */
    protected BiomeProvider() {
    }

    /**
     * Gets the biome at the specified coordinates.
     *
     * <p>Coordinates are world coordinates, not chunk-relative.
     *
     * @param worldInfo the world information
     * @param x         the X coordinate
     * @param y         the Y coordinate
     * @param z         the Z coordinate
     * @return the biome at this location
     * @since 1.0.0
     */
    @NotNull
    public abstract Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z);

    /**
     * Gets all biomes that this provider can generate.
     *
     * <p>This is used to determine which biomes need to be available
     * during generation and for structure placement.
     *
     * @param worldInfo the world information
     * @return a list of all possible biomes
     * @since 1.0.0
     */
    @NotNull
    public abstract List<Biome> getBiomes(@NotNull WorldInfo worldInfo);

    /**
     * Creates a single-biome provider.
     *
     * @param biome the biome to use everywhere
     * @return a provider that returns only one biome
     * @since 1.0.0
     */
    @NotNull
    public static BiomeProvider single(@NotNull Biome biome) {
        return new SingleBiomeProvider(biome);
    }

    /**
     * Creates a checkerboard pattern provider.
     *
     * @param biome1 the first biome
     * @param biome2 the second biome
     * @param size   the size of each square in blocks
     * @return a checkerboard pattern provider
     * @since 1.0.0
     */
    @NotNull
    public static BiomeProvider checkerboard(@NotNull Biome biome1, @NotNull Biome biome2, int size) {
        return new CheckerboardBiomeProvider(biome1, biome2, size);
    }

    /**
     * Single biome provider implementation.
     */
    private static final class SingleBiomeProvider extends BiomeProvider {
        private final Biome biome;
        private final List<Biome> biomes;

        SingleBiomeProvider(Biome biome) {
            this.biome = biome;
            this.biomes = List.of(biome);
        }

        @Override
        @NotNull
        public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            return biome;
        }

        @Override
        @NotNull
        public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return biomes;
        }
    }

    /**
     * Checkerboard pattern provider implementation.
     */
    private static final class CheckerboardBiomeProvider extends BiomeProvider {
        private final Biome biome1;
        private final Biome biome2;
        private final int size;
        private final List<Biome> biomes;

        CheckerboardBiomeProvider(Biome biome1, Biome biome2, int size) {
            this.biome1 = biome1;
            this.biome2 = biome2;
            this.size = Math.max(1, size);
            this.biomes = List.of(biome1, biome2);
        }

        @Override
        @NotNull
        public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            int xSquare = Math.floorDiv(x, size);
            int zSquare = Math.floorDiv(z, size);
            return ((xSquare + zSquare) % 2 == 0) ? biome1 : biome2;
        }

        @Override
        @NotNull
        public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return biomes;
        }
    }
}
