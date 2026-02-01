/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Represents a registered structure for world generation.
 *
 * <p>Structures can be generated naturally during world creation or
 * placed manually at specific locations. They support rotation, mirroring,
 * and various terrain adaptation modes.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see StructureService
 */
public interface Structure {

    /**
     * Gets the namespaced key for this structure.
     *
     * @return the structure key
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Gets the biomes where this structure can generate.
     *
     * @return the allowed biomes
     * @since 1.0.0
     */
    @NotNull
    Collection<Biome> getBiomes();

    /**
     * Gets the minimum spacing between structures in chunks.
     *
     * @return the spacing
     * @since 1.0.0
     */
    int getSpacing();

    /**
     * Gets the additional separation within the spacing.
     *
     * @return the separation
     * @since 1.0.0
     */
    int getSeparation();

    /**
     * Gets the salt used for placement randomization.
     *
     * @return the salt
     * @since 1.0.0
     */
    int getSalt();

    /**
     * Gets the terrain adaptation mode.
     *
     * @return the terrain adaptation
     * @since 1.0.0
     */
    @NotNull
    StructureService.TerrainAdaptation getTerrainAdaptation();

    /**
     * Checks if this structure is registered.
     *
     * @return true if registered
     * @since 1.0.0
     */
    boolean isRegistered();

    /**
     * Places this structure at the specified location.
     *
     * @param location the center location
     * @param rotation the rotation
     * @return true if placed successfully
     * @since 1.0.0
     */
    boolean place(@NotNull UnifiedLocation location, @NotNull StructureService.Rotation rotation);

    /**
     * Builder for creating structures.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the schematic from a file path.
         *
         * @param path the path to the schematic file
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder schematic(@NotNull Path path);

        /**
         * Sets the schematic from an input stream.
         *
         * @param stream the input stream
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder schematic(@NotNull InputStream stream);

        /**
         * Sets the schematic directly.
         *
         * @param schematic the schematic
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder schematic(@NotNull Schematic schematic);

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
         * Configures structure processors.
         *
         * @param configurator the processor configurator
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder processors(@NotNull Consumer<ProcessorConfigurator> configurator);

        /**
         * Sets a callback when the structure is placed.
         *
         * @param callback the placement callback
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder onPlace(@NotNull PlacementCallback callback);

        /**
         * Sets the minimum Y level for placement.
         *
         * @param minY the minimum Y
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder minY(int minY);

        /**
         * Sets the maximum Y level for placement.
         *
         * @param maxY the maximum Y
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder maxY(int maxY);

        /**
         * Sets whether the structure must be on a flat surface.
         *
         * @param requireFlat true to require flat surface
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder requireFlatTerrain(boolean requireFlat);

        /**
         * Registers the structure.
         *
         * @return the registered structure
         * @since 1.0.0
         */
        @NotNull
        Structure register();
    }

    /**
     * Configurator for structure processors.
     *
     * @since 1.0.0
     */
    interface ProcessorConfigurator {

        /**
         * Adds a processor instance.
         *
         * @param processor the processor
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        ProcessorConfigurator add(@NotNull StructureProcessor processor);

        /**
         * Adds a block replacement processor.
         *
         * @param from   the block to replace
         * @param to     the replacement block
         * @param chance the replacement chance (0.0-1.0)
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        ProcessorConfigurator replace(@NotNull BlockType from, @NotNull BlockType to, float chance);

        /**
         * Adds a block aging processor for weathered appearance.
         *
         * @param mossiness the mossiness chance (0.0-1.0)
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        ProcessorConfigurator age(float mossiness);

        /**
         * Adds a gravity processor to make floating blocks fall.
         *
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        ProcessorConfigurator gravity();

        /**
         * Adds an air protection processor to prevent air overwriting.
         *
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        ProcessorConfigurator protectAir();
    }

    /**
     * Callback when a structure is placed.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface PlacementCallback {
        /**
         * Called when the structure is placed.
         *
         * @param world    the world
         * @param location the structure location
         * @param rotation the rotation applied
         * @since 1.0.0
         */
        void onPlace(@NotNull UnifiedWorld world, @NotNull UnifiedLocation location,
                     @NotNull StructureService.Rotation rotation);
    }
}
