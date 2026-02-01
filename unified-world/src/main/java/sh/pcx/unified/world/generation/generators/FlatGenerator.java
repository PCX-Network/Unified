/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation.generators;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.generation.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * A chunk generator that creates flat worlds with configurable layers.
 *
 * <p>FlatGenerator produces a flat terrain with customizable block layers.
 * Each layer can have a specific block type and height.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a simple flat world
 * worlds.create("flatworld")
 *     .generator(new FlatGenerator())
 *     .create();
 *
 * // Create a custom flat world
 * FlatGenerator generator = FlatGenerator.builder()
 *     .addLayer(BlockType.BEDROCK, 1)
 *     .addLayer(BlockType.STONE, 30)
 *     .addLayer(BlockType.DIRT, 3)
 *     .addLayer(BlockType.GRASS_BLOCK, 1)
 *     .biome(Biome.PLAINS)
 *     .build();
 *
 * worlds.create("custom_flat")
 *     .generator(generator)
 *     .create();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ChunkGenerator
 */
public class FlatGenerator extends ChunkGenerator {

    private final List<Layer> layers;
    private final Biome biome;
    private final int totalHeight;

    /**
     * Creates a flat generator with default settings.
     *
     * <p>Default layers: 1 bedrock, 2 dirt, 1 grass block.
     *
     * @since 1.0.0
     */
    public FlatGenerator() {
        this(defaultLayers(), Biome.PLAINS);
    }

    /**
     * Creates a flat generator with custom layers.
     *
     * @param layers the block layers from bottom to top
     * @param biome  the biome to use
     * @since 1.0.0
     */
    public FlatGenerator(@NotNull List<Layer> layers, @NotNull Biome biome) {
        this.layers = List.copyOf(layers);
        this.biome = Objects.requireNonNull(biome);
        this.totalHeight = layers.stream().mapToInt(Layer::height).sum();
    }

    private static List<Layer> defaultLayers() {
        return List.of(
                new Layer(BlockType.BEDROCK, 1),
                new Layer(BlockType.DIRT, 2),
                new Layer(BlockType.GRASS_BLOCK, 1)
        );
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int minY = worldInfo.getMinHeight();
        int currentY = minY;

        for (Layer layer : layers) {
            for (int y = currentY; y < currentY + layer.height() && y < worldInfo.getMaxHeight(); y++) {
                chunkData.setLayer(y, layer.blockType());
            }
            currentY += layer.height();
        }
    }

    @Override
    @Nullable
    public UnifiedLocation getFixedSpawnLocation(@NotNull WorldInfo worldInfo, @NotNull Random random) {
        int spawnY = worldInfo.getMinHeight() + totalHeight;
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
    public int getBaseHeight(@NotNull WorldInfo worldInfo, int x, int z) {
        return worldInfo.getMinHeight() + totalHeight;
    }

    /**
     * Gets the layers of this generator.
     *
     * @return an unmodifiable list of layers
     * @since 1.0.0
     */
    @NotNull
    public List<Layer> getLayers() {
        return layers;
    }

    /**
     * Gets the biome used by this generator.
     *
     * @return the biome
     * @since 1.0.0
     */
    @NotNull
    public Biome getBiome() {
        return biome;
    }

    /**
     * Creates a new builder for FlatGenerator.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Represents a layer of blocks in the flat world.
     *
     * @param blockType the block type for this layer
     * @param height    the height of this layer in blocks
     * @since 1.0.0
     */
    public record Layer(@NotNull BlockType blockType, int height) {
        /**
         * Creates a layer with validation.
         *
         * @param blockType the block type
         * @param height    the height (must be positive)
         * @since 1.0.0
         */
        public Layer {
            Objects.requireNonNull(blockType, "blockType cannot be null");
            if (height < 1) {
                throw new IllegalArgumentException("Layer height must be at least 1");
            }
        }
    }

    /**
     * Builder for creating FlatGenerator instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final List<Layer> layers = new ArrayList<>();
        private Biome biome = Biome.PLAINS;

        private Builder() {
        }

        /**
         * Adds a layer to the generator.
         *
         * @param blockType the block type
         * @param height    the layer height
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder addLayer(@NotNull BlockType blockType, int height) {
            layers.add(new Layer(blockType, height));
            return this;
        }

        /**
         * Adds a single-block layer.
         *
         * @param blockType the block type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder addLayer(@NotNull BlockType blockType) {
            return addLayer(blockType, 1);
        }

        /**
         * Sets the biome for the flat world.
         *
         * @param biome the biome
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder biome(@NotNull Biome biome) {
            this.biome = Objects.requireNonNull(biome);
            return this;
        }

        /**
         * Clears all layers.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder clearLayers() {
            layers.clear();
            return this;
        }

        /**
         * Builds the FlatGenerator.
         *
         * @return the built generator
         * @throws IllegalStateException if no layers were added
         * @since 1.0.0
         */
        @NotNull
        public FlatGenerator build() {
            if (layers.isEmpty()) {
                throw new IllegalStateException("At least one layer is required");
            }
            return new FlatGenerator(layers, biome);
        }
    }
}
