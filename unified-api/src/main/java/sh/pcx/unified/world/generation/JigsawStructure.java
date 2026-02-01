/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Represents a jigsaw-based structure like villages or bastions.
 *
 * <p>Jigsaw structures are modular structures assembled from multiple pieces
 * using jigsaw connections. This allows for procedural generation of varied
 * structures from a set of building blocks.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Register jigsaw structure
 * structures.registerJigsaw("myplugin:ruins")
 *     .startPool("myplugin:ruins/center")
 *     .maxDepth(5)
 *     .biomes(Biome.PLAINS, Biome.FOREST)
 *     .spacing(20)
 *     .separation(8)
 *     .register();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see StructureService
 * @see StructurePiece
 */
public interface JigsawStructure extends Structure {

    /**
     * Gets the starting template pool.
     *
     * @return the start pool key
     * @since 1.0.0
     */
    @NotNull
    String getStartPool();

    /**
     * Gets the maximum depth for jigsaw connections.
     *
     * @return the max depth
     * @since 1.0.0
     */
    int getMaxDepth();

    /**
     * Gets the heightmap type used for placement.
     *
     * @return the heightmap type
     * @since 1.0.0
     */
    @NotNull
    HeightmapType getHeightmapType();

    /**
     * Builder for creating jigsaw structures.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the starting template pool.
         *
         * @param pool the pool key
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder startPool(@NotNull String pool);

        /**
         * Sets the maximum jigsaw depth.
         *
         * @param depth the max depth
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder maxDepth(int depth);

        /**
         * Sets the heightmap type for placement.
         *
         * @param heightmap the heightmap type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder heightmap(@NotNull HeightmapType heightmap);

        /**
         * Sets the biomes where this structure can generate.
         *
         * @param biomes the allowed biomes
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder biomes(@NotNull Biome... biomes);

        /**
         * Sets the biomes from a collection.
         *
         * @param biomes the allowed biomes
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder biomes(@NotNull Collection<Biome> biomes);

        /**
         * Sets the minimum spacing between structures in chunks.
         *
         * @param spacing the spacing
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder spacing(int spacing);

        /**
         * Sets the additional separation within the spacing.
         *
         * @param separation the separation
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder separation(int separation);

        /**
         * Sets the salt for placement randomization.
         *
         * @param salt the salt
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder salt(int salt);

        /**
         * Sets the terrain adaptation mode.
         *
         * @param adaptation the terrain adaptation
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder terrainAdaptation(@NotNull StructureService.TerrainAdaptation adaptation);

        /**
         * Sets the maximum Y level for the start position.
         *
         * @param maxY the max Y
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder maxY(int maxY);

        /**
         * Sets whether to use legacy bounding box expansion.
         *
         * @param legacy true for legacy mode
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder legacyBoundingBoxExpansion(boolean legacy);

        /**
         * Registers the jigsaw structure.
         *
         * @return the registered structure
         * @since 1.0.0
         */
        @NotNull
        JigsawStructure register();
    }

    /**
     * Heightmap types for structure placement.
     *
     * @since 1.0.0
     */
    enum HeightmapType {
        /** Use the motion blocking heightmap. */
        MOTION_BLOCKING,
        /** Use the motion blocking heightmap excluding leaves. */
        MOTION_BLOCKING_NO_LEAVES,
        /** Use the ocean floor heightmap. */
        OCEAN_FLOOR,
        /** Use the world surface heightmap. */
        WORLD_SURFACE
    }
}
