/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Fluent builder for creating new worlds with customized settings.
 *
 * <p>WorldCreator provides a type-safe, fluent API for configuring all aspects
 * of world creation including environment, generation, game rules, and borders.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a standard survival world
 * World survival = worlds.create("survival")
 *     .environment(Environment.NORMAL)
 *     .type(WorldType.NORMAL)
 *     .seed(12345L)
 *     .generateStructures(true)
 *     .hardcore(false)
 *     .create();
 *
 * // Create a void world for lobbies
 * World lobby = worlds.create("lobby")
 *     .environment(Environment.NORMAL)
 *     .generator(VoidGenerator.class)
 *     .spawnLocation(0, 64, 0)
 *     .create();
 *
 * // Create a custom arena world
 * World arena = worlds.create("arena-1")
 *     .environment(Environment.NORMAL)
 *     .generator(ArenaGenerator.class)
 *     .generatorSettings(settings -> {
 *         settings.put("size", 100);
 *         settings.put("theme", "medieval");
 *     })
 *     .gameRules(rules -> {
 *         rules.set(GameRule.DO_MOB_SPAWNING, false);
 *         rules.set(GameRule.DO_DAYLIGHT_CYCLE, false);
 *         rules.set(GameRule.PVP, true);
 *     })
 *     .worldBorder(border -> {
 *         border.setSize(200);
 *         border.setCenter(0, 0);
 *     })
 *     .createAsync();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 * @see WorldTemplate
 */
public interface WorldCreator {

    // ==================== Basic Settings ====================

    /**
     * Sets the world environment (dimension type).
     *
     * @param environment the environment type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator environment(@NotNull UnifiedWorld.Environment environment);

    /**
     * Sets the world type (generation style).
     *
     * @param type the world type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator type(@NotNull WorldType type);

    /**
     * Sets the world seed.
     *
     * @param seed the seed value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator seed(long seed);

    /**
     * Sets the world seed from a string (hashed).
     *
     * @param seed the seed string
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator seed(@NotNull String seed);

    /**
     * Uses a random seed.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator randomSeed();

    // ==================== Generation Settings ====================

    /**
     * Sets whether structures should be generated.
     *
     * @param generate true to generate structures
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator generateStructures(boolean generate);

    /**
     * Sets a custom chunk generator by ID.
     *
     * @param generatorId the registered generator ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator generator(@NotNull String generatorId);

    /**
     * Sets a custom chunk generator instance.
     *
     * @param generator the chunk generator
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator generator(@NotNull ChunkGenerator generator);

    /**
     * Sets a custom chunk generator by class (will be instantiated).
     *
     * @param generatorClass the generator class
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator generator(@NotNull Class<? extends ChunkGenerator> generatorClass);

    /**
     * Configures generator-specific settings.
     *
     * @param configurator the settings configurator
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator generatorSettings(@NotNull Consumer<GeneratorSettings> configurator);

    /**
     * Sets raw generator settings string (for vanilla generators).
     *
     * @param settings the settings string
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator generatorSettings(@NotNull String settings);

    // ==================== World Settings ====================

    /**
     * Sets whether this is a hardcore world.
     *
     * @param hardcore true for hardcore mode
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator hardcore(boolean hardcore);

    /**
     * Sets the world difficulty.
     *
     * @param difficulty the difficulty level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator difficulty(@NotNull UnifiedWorld.Difficulty difficulty);

    /**
     * Sets the spawn location.
     *
     * @param location the spawn location
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator spawnLocation(@NotNull UnifiedLocation location);

    /**
     * Sets the spawn location by coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator spawnLocation(double x, double y, double z);

    /**
     * Sets whether to keep the spawn area loaded.
     *
     * @param keepLoaded true to keep spawn loaded
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator keepSpawnLoaded(boolean keepLoaded);

    /**
     * Sets whether PvP is enabled.
     *
     * @param pvp true to enable PvP
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator pvp(boolean pvp);

    // ==================== Game Rules ====================

    /**
     * Configures game rules for this world.
     *
     * @param configurator the game rules configurator
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator gameRules(@NotNull Consumer<GameRuleConfigurator> configurator);

    /**
     * Sets a boolean game rule.
     *
     * @param rule  the game rule name
     * @param value the value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator gameRule(@NotNull String rule, boolean value);

    /**
     * Sets an integer game rule.
     *
     * @param rule  the game rule name
     * @param value the value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator gameRule(@NotNull String rule, int value);

    // ==================== World Border ====================

    /**
     * Configures the world border.
     *
     * @param configurator the border configurator
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator worldBorder(@NotNull Consumer<WorldBorderConfigurator> configurator);

    // ==================== Template ====================

    /**
     * Applies settings from a world template.
     *
     * @param template the template to apply
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator template(@NotNull WorldTemplate template);

    // ==================== World Groups ====================

    /**
     * Adds this world to a world group.
     *
     * @param groupName the group name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldCreator group(@NotNull String groupName);

    // ==================== Build ====================

    /**
     * Creates the world synchronously.
     *
     * <p>Warning: This may cause the server to hang during generation.
     * Prefer {@link #createAsync()} when possible.
     *
     * @return the created world
     * @throws WorldCreationException if creation fails
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld create();

    /**
     * Creates the world asynchronously.
     *
     * @return a future that completes with the created world
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UnifiedWorld> createAsync();

    /**
     * Gets the configured world name.
     *
     * @return the world name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Gets the configured environment.
     *
     * @return the environment, or null if not set
     * @since 1.0.0
     */
    @Nullable
    UnifiedWorld.Environment getEnvironment();

    /**
     * Gets the configured seed.
     *
     * @return the seed, or null if random
     * @since 1.0.0
     */
    @Nullable
    Long getSeed();

    /**
     * Gets the configured generator.
     *
     * @return the generator, or null if using default
     * @since 1.0.0
     */
    @Nullable
    ChunkGenerator getGenerator();

    // ==================== Nested Interfaces ====================

    /**
     * Game rule configurator for fluent game rule setup.
     *
     * @since 1.0.0
     */
    interface GameRuleConfigurator {

        /**
         * Sets a boolean game rule.
         *
         * @param rule  the game rule name
         * @param value the value
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        GameRuleConfigurator set(@NotNull String rule, boolean value);

        /**
         * Sets an integer game rule.
         *
         * @param rule  the game rule name
         * @param value the value
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        GameRuleConfigurator set(@NotNull String rule, int value);

        /**
         * Copies game rules from another world.
         *
         * @param source the source world
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        GameRuleConfigurator copyFrom(@NotNull UnifiedWorld source);
    }

    /**
     * World border configurator for fluent border setup.
     *
     * @since 1.0.0
     */
    interface WorldBorderConfigurator {

        /**
         * Sets the border center.
         *
         * @param x the x coordinate
         * @param z the z coordinate
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        WorldBorderConfigurator setCenter(double x, double z);

        /**
         * Sets the border size (diameter).
         *
         * @param size the size in blocks
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        WorldBorderConfigurator setSize(double size);

        /**
         * Sets the damage amount per block outside the border.
         *
         * @param damage the damage per block
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        WorldBorderConfigurator setDamageAmount(double damage);

        /**
         * Sets the damage buffer (safe zone outside border).
         *
         * @param buffer the buffer in blocks
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        WorldBorderConfigurator setDamageBuffer(double buffer);

        /**
         * Sets the warning distance.
         *
         * @param distance the warning distance in blocks
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        WorldBorderConfigurator setWarningDistance(int distance);

        /**
         * Sets the warning time.
         *
         * @param seconds the warning time in seconds
         * @return this configurator
         * @since 1.0.0
         */
        @NotNull
        WorldBorderConfigurator setWarningTime(int seconds);
    }

    /**
     * Generator settings for custom chunk generators.
     *
     * @since 1.0.0
     */
    interface GeneratorSettings {

        /**
         * Sets a string setting.
         *
         * @param key   the setting key
         * @param value the value
         * @return this settings object
         * @since 1.0.0
         */
        @NotNull
        GeneratorSettings put(@NotNull String key, @NotNull String value);

        /**
         * Sets an integer setting.
         *
         * @param key   the setting key
         * @param value the value
         * @return this settings object
         * @since 1.0.0
         */
        @NotNull
        GeneratorSettings put(@NotNull String key, int value);

        /**
         * Sets a double setting.
         *
         * @param key   the setting key
         * @param value the value
         * @return this settings object
         * @since 1.0.0
         */
        @NotNull
        GeneratorSettings put(@NotNull String key, double value);

        /**
         * Sets a boolean setting.
         *
         * @param key   the setting key
         * @param value the value
         * @return this settings object
         * @since 1.0.0
         */
        @NotNull
        GeneratorSettings put(@NotNull String key, boolean value);

        /**
         * Sets an object setting.
         *
         * @param key   the setting key
         * @param value the value
         * @return this settings object
         * @since 1.0.0
         */
        @NotNull
        GeneratorSettings put(@NotNull String key, @NotNull Object value);

        /**
         * Gets a setting value.
         *
         * @param key the setting key
         * @param <T> the value type
         * @return the value, or null if not set
         * @since 1.0.0
         */
        @Nullable
        <T> T get(@NotNull String key);

        /**
         * Gets a setting value with a default.
         *
         * @param key          the setting key
         * @param defaultValue the default value
         * @param <T>          the value type
         * @return the value, or the default if not set
         * @since 1.0.0
         */
        @NotNull
        <T> T getOrDefault(@NotNull String key, @NotNull T defaultValue);
    }
}
