/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents a custom biome created through the BiomeService.
 *
 * <p>CustomBiome contains all properties that define a biome's appearance,
 * sounds, mob spawns, and world generation features. Custom biomes can be
 * used in custom chunk generators or registered globally.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * CustomBiome crystalForest = biomes.create("myplugin:crystal_forest")
 *     .temperature(0.5f)
 *     .downfall(0.8f)
 *     .precipitation(Precipitation.RAIN)
 *     .skyColor(0x77CCFF)
 *     .fogColor(0xC0D8FF)
 *     .waterColor(0x3F76E4)
 *     .grassColor(0x7AFFCF)
 *     .foliageColor(0x4AFFB2)
 *     .ambientSound("minecraft:ambient.crimson_forest.loop")
 *     .spawnSettings(spawns -> {
 *         spawns.addSpawn(SpawnGroup.CREATURE, "minecraft:rabbit", 10, 2, 4);
 *     })
 *     .features(features -> {
 *         features.addFeature(GenerationStep.VEGETAL_DECORATION, "myplugin:crystal_tree");
 *     })
 *     .register();
 *
 * // Use in a biome provider
 * Biome customBiome = crystalForest.asBiome();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see BiomeService
 */
public interface CustomBiome {

    /**
     * Gets the namespaced key for this biome.
     *
     * @return the biome key
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Gets this custom biome as a Biome reference.
     *
     * @return the biome reference
     * @since 1.0.0
     */
    @NotNull
    Biome asBiome();

    /**
     * Gets the temperature of this biome.
     *
     * @return the temperature (0.0 = cold, 1.0+ = hot)
     * @since 1.0.0
     */
    float getTemperature();

    /**
     * Gets the downfall (humidity) of this biome.
     *
     * @return the downfall (0.0 = dry, 1.0 = wet)
     * @since 1.0.0
     */
    float getDownfall();

    /**
     * Gets the precipitation type for this biome.
     *
     * @return the precipitation type
     * @since 1.0.0
     */
    @NotNull
    Precipitation getPrecipitation();

    /**
     * Gets the sky color for this biome.
     *
     * @return the sky color as RGB integer
     * @since 1.0.0
     */
    int getSkyColor();

    /**
     * Gets the fog color for this biome.
     *
     * @return the fog color as RGB integer
     * @since 1.0.0
     */
    int getFogColor();

    /**
     * Gets the water color for this biome.
     *
     * @return the water color as RGB integer
     * @since 1.0.0
     */
    int getWaterColor();

    /**
     * Gets the water fog color for this biome.
     *
     * @return the water fog color as RGB integer
     * @since 1.0.0
     */
    int getWaterFogColor();

    /**
     * Gets the grass color for this biome.
     *
     * @return the grass color as RGB integer, or null for default
     * @since 1.0.0
     */
    @Nullable
    Integer getGrassColor();

    /**
     * Gets the foliage color for this biome.
     *
     * @return the foliage color as RGB integer, or null for default
     * @since 1.0.0
     */
    @Nullable
    Integer getFoliageColor();

    /**
     * Checks if this biome is registered.
     *
     * @return true if registered
     * @since 1.0.0
     */
    boolean isRegistered();

    // ==================== Builder ====================

    /**
     * Builder for creating custom biomes.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the temperature.
         *
         * @param temperature the temperature (0.0 = cold, 1.0+ = hot)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder temperature(float temperature);

        /**
         * Sets the downfall (humidity).
         *
         * @param downfall the downfall (0.0 = dry, 1.0 = wet)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder downfall(float downfall);

        /**
         * Sets the precipitation type.
         *
         * @param precipitation the precipitation type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder precipitation(@NotNull Precipitation precipitation);

        /**
         * Sets the sky color.
         *
         * @param color the sky color as RGB integer (0xRRGGBB)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder skyColor(int color);

        /**
         * Sets the fog color.
         *
         * @param color the fog color as RGB integer (0xRRGGBB)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder fogColor(int color);

        /**
         * Sets the water color.
         *
         * @param color the water color as RGB integer (0xRRGGBB)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder waterColor(int color);

        /**
         * Sets the water fog color.
         *
         * @param color the water fog color as RGB integer (0xRRGGBB)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder waterFogColor(int color);

        /**
         * Sets the grass color.
         *
         * @param color the grass color as RGB integer (0xRRGGBB)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder grassColor(int color);

        /**
         * Sets the foliage (leaves) color.
         *
         * @param color the foliage color as RGB integer (0xRRGGBB)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder foliageColor(int color);

        /**
         * Sets the grass color modifier.
         *
         * @param modifier the grass color modifier
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder grassColorModifier(@NotNull GrassColorModifier modifier);

        /**
         * Sets the ambient sound.
         *
         * @param sound the sound key (e.g., "minecraft:ambient.crimson_forest.loop")
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder ambientSound(@NotNull String sound);

        /**
         * Sets the ambient mood sound.
         *
         * @param sound       the sound key
         * @param tickDelay   delay between plays in ticks
         * @param blockRadius search radius for darkness
         * @param offset      sound offset
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder moodSound(@NotNull String sound, int tickDelay, int blockRadius, double offset);

        /**
         * Sets additional ambient sounds.
         *
         * @param sound       the sound key
         * @param probability chance per tick to play
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder additionsSound(@NotNull String sound, double probability);

        /**
         * Sets the background music.
         *
         * @param sound       the music sound key
         * @param minDelay    minimum delay between plays
         * @param maxDelay    maximum delay between plays
         * @param replaceCurrentMusic whether to interrupt current music
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder music(@NotNull String sound, int minDelay, int maxDelay, boolean replaceCurrentMusic);

        /**
         * Sets the ambient particle effect.
         *
         * @param particle    the particle type key
         * @param probability spawn probability
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder ambientParticle(@NotNull String particle, float probability);

        /**
         * Configures mob spawn settings.
         *
         * @param configurator the spawn settings configurator
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder spawnSettings(@NotNull Consumer<SpawnConfigurator> configurator);

        /**
         * Configures world generation features.
         *
         * @param configurator the features configurator
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder features(@NotNull Consumer<FeatureConfigurator> configurator);

        /**
         * Copies settings from an existing biome.
         *
         * @param biome the biome to copy from
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder copyFrom(@NotNull Biome biome);

        /**
         * Builds and registers the custom biome.
         *
         * @return the registered custom biome
         * @since 1.0.0
         */
        @NotNull
        CustomBiome register();

        /**
         * Builds the custom biome without registering.
         *
         * @return the built custom biome
         * @since 1.0.0
         */
        @NotNull
        CustomBiome build();
    }

    /**
     * Configurator for mob spawn settings.
     *
     * @since 1.0.0
     */
    interface SpawnConfigurator {

        /**
         * Adds a mob spawn.
         *
         * @param group      the spawn group
         * @param entityType the entity type key
         * @param weight     spawn weight
         * @param minCount   minimum count per spawn
         * @param maxCount   maximum count per spawn
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        SpawnConfigurator addSpawn(@NotNull BiomeService.SpawnGroup group,
                                   @NotNull String entityType,
                                   int weight, int minCount, int maxCount);

        /**
         * Sets the spawn probability for a group.
         *
         * @param group       the spawn group
         * @param probability spawn probability (0.0-1.0)
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        SpawnConfigurator spawnProbability(@NotNull BiomeService.SpawnGroup group, float probability);

        /**
         * Sets the mob spawn cost (for monster density cap).
         *
         * @param entityType the entity type key
         * @param charge     energy per mob
         * @param energyBudget total energy budget
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        SpawnConfigurator spawnCost(@NotNull String entityType, double charge, double energyBudget);
    }

    /**
     * Configurator for world generation features.
     *
     * @since 1.0.0
     */
    interface FeatureConfigurator {

        /**
         * Adds a feature to a generation step.
         *
         * @param step    the generation step
         * @param feature the feature key
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        FeatureConfigurator addFeature(@NotNull BiomeService.GenerationStep step,
                                       @NotNull String feature);

        /**
         * Adds a carver (cave system) to this biome.
         *
         * @param step   the carving step (AIR or LIQUID)
         * @param carver the carver key
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        FeatureConfigurator addCarver(@NotNull CarvingStep step, @NotNull String carver);
    }

    /**
     * Precipitation types for biomes.
     *
     * @since 1.0.0
     */
    enum Precipitation {
        NONE,
        RAIN,
        SNOW
    }

    /**
     * Grass color modifiers.
     *
     * @since 1.0.0
     */
    enum GrassColorModifier {
        NONE,
        DARK_FOREST,
        SWAMP
    }

    /**
     * Carving steps for cave generation.
     *
     * @since 1.0.0
     */
    enum CarvingStep {
        AIR,
        LIQUID
    }
}
