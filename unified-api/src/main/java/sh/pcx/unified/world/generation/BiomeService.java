/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing custom biomes and modifying vanilla biomes.
 *
 * <p>BiomeService provides the ability to create custom biomes with unique
 * colors, sounds, mob spawns, and world generation features. It also supports
 * modifying vanilla biomes to add or remove features.
 *
 * <h2>Note:</h2>
 * <p>Custom biomes require Minecraft 1.20.5+ and may not work on all platforms.
 * Check {@link #isCustomBiomesSupported()} before using custom biome features.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Inject
 * private BiomeService biomes;
 *
 * // Create custom biome
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
 *         spawns.addSpawn(SpawnGroup.MONSTER, "minecraft:zombie", 5, 1, 2);
 *     })
 *     .features(features -> {
 *         features.addFeature(GenerationStep.VEGETAL_DECORATION, "myplugin:crystal_tree");
 *         features.addFeature(GenerationStep.UNDERGROUND_ORES, "myplugin:crystal_ore");
 *     })
 *     .register();
 *
 * // Modify vanilla biome
 * biomes.modify(Biome.PLAINS)
 *     .addSpawn(SpawnGroup.CREATURE, "minecraft:fox", 5, 1, 2)
 *     .removeSpawn("minecraft:sheep")
 *     .addFeature(GenerationStep.VEGETAL_DECORATION, "myplugin:wildflowers")
 *     .apply();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see CustomBiome
 * @see WorldService
 */
public interface BiomeService extends Service {

    /**
     * Checks if custom biomes are supported on this platform.
     *
     * @return true if custom biomes are supported
     * @since 1.0.0
     */
    boolean isCustomBiomesSupported();

    /**
     * Checks if biome modification is supported on this platform.
     *
     * @return true if biome modification is supported
     * @since 1.0.0
     */
    boolean isBiomeModificationSupported();

    // ==================== Custom Biome Creation ====================

    /**
     * Creates a new custom biome builder.
     *
     * @param key the namespaced key for the biome (e.g., "myplugin:custom_biome")
     * @return a new CustomBiome.Builder
     * @throws UnsupportedOperationException if custom biomes are not supported
     * @since 1.0.0
     */
    @NotNull
    CustomBiome.Builder create(@NotNull String key);

    /**
     * Gets a registered custom biome by key.
     *
     * @param key the biome key
     * @return the custom biome, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<CustomBiome> getCustomBiome(@NotNull String key);

    /**
     * Gets all registered custom biomes.
     *
     * @return an unmodifiable collection of custom biomes
     * @since 1.0.0
     */
    @NotNull
    Collection<CustomBiome> getCustomBiomes();

    /**
     * Unregisters a custom biome.
     *
     * <p>Note: This may not take effect in already-generated chunks.
     *
     * @param key the biome key to unregister
     * @return true if a biome was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull String key);

    // ==================== Biome Modification ====================

    /**
     * Creates a modifier for a vanilla biome.
     *
     * @param biome the biome to modify
     * @return a biome modifier
     * @throws UnsupportedOperationException if modification is not supported
     * @since 1.0.0
     */
    @NotNull
    BiomeModifier modify(@NotNull Biome biome);

    /**
     * Creates a modifier that applies to multiple biomes.
     *
     * @param biomes the biomes to modify
     * @return a biome modifier
     * @since 1.0.0
     */
    @NotNull
    BiomeModifier modify(@NotNull Collection<Biome> biomes);

    /**
     * Creates a modifier that applies to biomes matching a predicate.
     *
     * @param selector the biome selector
     * @return a biome modifier
     * @since 1.0.0
     */
    @NotNull
    BiomeModifier modifyMatching(@NotNull BiomeSelector selector);

    // ==================== Queries ====================

    /**
     * Gets all vanilla biomes.
     *
     * @return a collection of vanilla biomes
     * @since 1.0.0
     */
    @NotNull
    Collection<Biome> getVanillaBiomes();

    /**
     * Gets all biomes (vanilla + custom).
     *
     * @return a collection of all biomes
     * @since 1.0.0
     */
    @NotNull
    Collection<Biome> getAllBiomes();

    /**
     * Checks if a biome key is registered.
     *
     * @param key the biome key
     * @return true if the biome exists
     * @since 1.0.0
     */
    boolean exists(@NotNull String key);

    // ==================== Nested Interfaces ====================

    /**
     * Selector for choosing which biomes to modify.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface BiomeSelector {
        /**
         * Tests if the given biome should be selected.
         *
         * @param biome the biome to test
         * @return true if the biome should be selected
         * @since 1.0.0
         */
        boolean test(@NotNull Biome biome);

        /**
         * Selects all ocean biomes.
         *
         * @return a selector for ocean biomes
         * @since 1.0.0
         */
        @NotNull
        static BiomeSelector oceans() {
            return Biome::isOcean;
        }

        /**
         * Selects all forest biomes.
         *
         * @return a selector for forest biomes
         * @since 1.0.0
         */
        @NotNull
        static BiomeSelector forests() {
            return Biome::isForest;
        }

        /**
         * Selects all cold/snowy biomes.
         *
         * @return a selector for cold biomes
         * @since 1.0.0
         */
        @NotNull
        static BiomeSelector cold() {
            return Biome::isCold;
        }

        /**
         * Selects all nether biomes.
         *
         * @return a selector for nether biomes
         * @since 1.0.0
         */
        @NotNull
        static BiomeSelector nether() {
            return Biome::isNether;
        }

        /**
         * Selects all end biomes.
         *
         * @return a selector for end biomes
         * @since 1.0.0
         */
        @NotNull
        static BiomeSelector end() {
            return Biome::isEnd;
        }

        /**
         * Selects all overworld biomes.
         *
         * @return a selector for overworld biomes
         * @since 1.0.0
         */
        @NotNull
        static BiomeSelector overworld() {
            return biome -> !biome.isNether() && !biome.isEnd();
        }
    }

    /**
     * Modifier for changing biome properties.
     *
     * @since 1.0.0
     */
    interface BiomeModifier {

        /**
         * Adds a mob spawn to the biome.
         *
         * @param group      the spawn group
         * @param entityType the entity type key
         * @param weight     the spawn weight
         * @param minCount   minimum spawn count
         * @param maxCount   maximum spawn count
         * @return this modifier
         * @since 1.0.0
         */
        @NotNull
        BiomeModifier addSpawn(@NotNull SpawnGroup group, @NotNull String entityType,
                               int weight, int minCount, int maxCount);

        /**
         * Removes a mob spawn from the biome.
         *
         * @param entityType the entity type key to remove
         * @return this modifier
         * @since 1.0.0
         */
        @NotNull
        BiomeModifier removeSpawn(@NotNull String entityType);

        /**
         * Clears all spawns in a group.
         *
         * @param group the spawn group to clear
         * @return this modifier
         * @since 1.0.0
         */
        @NotNull
        BiomeModifier clearSpawns(@NotNull SpawnGroup group);

        /**
         * Adds a world generation feature to the biome.
         *
         * @param step    the generation step
         * @param feature the feature key
         * @return this modifier
         * @since 1.0.0
         */
        @NotNull
        BiomeModifier addFeature(@NotNull GenerationStep step, @NotNull String feature);

        /**
         * Removes a world generation feature from the biome.
         *
         * @param feature the feature key to remove
         * @return this modifier
         * @since 1.0.0
         */
        @NotNull
        BiomeModifier removeFeature(@NotNull String feature);

        /**
         * Applies the modifications.
         *
         * @since 1.0.0
         */
        void apply();
    }

    /**
     * Spawn groups for mob spawning.
     *
     * @since 1.0.0
     */
    enum SpawnGroup {
        MONSTER,
        CREATURE,
        AMBIENT,
        AXOLOTLS,
        UNDERGROUND_WATER_CREATURE,
        WATER_CREATURE,
        WATER_AMBIENT,
        MISC
    }

    /**
     * Generation steps for feature placement.
     *
     * @since 1.0.0
     */
    enum GenerationStep {
        RAW_GENERATION,
        LAKES,
        LOCAL_MODIFICATIONS,
        UNDERGROUND_STRUCTURES,
        SURFACE_STRUCTURES,
        STRONGHOLDS,
        UNDERGROUND_ORES,
        UNDERGROUND_DECORATION,
        FLUID_SPRINGS,
        VEGETAL_DECORATION,
        TOP_LAYER_MODIFICATION
    }
}
