/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a pre-configured world template for quick world creation.
 *
 * <p>World templates encapsulate common world configurations that can be reused
 * across multiple world creations. They define environment, generation settings,
 * game rules, world borders, and other properties.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Define a custom template
 * WorldTemplate survivalTemplate = WorldTemplate.builder()
 *     .id("survival_template")
 *     .displayName("Survival World")
 *     .environment(Environment.NORMAL)
 *     .type(WorldType.NORMAL)
 *     .generateStructures(true)
 *     .gameRules(rules -> {
 *         rules.set(GameRule.KEEP_INVENTORY, false);
 *         rules.set(GameRule.DO_MOB_SPAWNING, true);
 *         rules.set(GameRule.PVP, true);
 *     })
 *     .difficulty(Difficulty.HARD)
 *     .worldBorder(border -> {
 *         border.setSize(10000);
 *         border.setCenter(0, 0);
 *     })
 *     .build();
 *
 * // Register the template
 * worlds.registerTemplate(survivalTemplate);
 *
 * // Create world from template
 * World survival = worlds.createFromTemplate("survival_1", survivalTemplate);
 *
 * // Use pre-built templates
 * World arena = worlds.createFromTemplate("arena", WorldTemplate.VOID_FLAT);
 * World skyblock = worlds.createFromTemplate("island", WorldTemplate.SKYBLOCK);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 * @see WorldCreator
 */
public interface WorldTemplate {

    // ==================== Pre-built Templates ====================

    /**
     * A void world template with a single flat layer at y=64.
     *
     * @since 1.0.0
     */
    WorldTemplate VOID_FLAT = builder()
            .id("unified:void_flat")
            .displayName("Void Flat")
            .description("Empty void world with flat platform at y=64")
            .environment(UnifiedWorld.Environment.NORMAL)
            .type(WorldType.FLAT)
            .generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\"}")
            .generateStructures(false)
            .keepSpawnLoaded(false)
            .build();

    /**
     * A complete void world template with no blocks.
     *
     * @since 1.0.0
     */
    WorldTemplate VOID = builder()
            .id("unified:void")
            .displayName("Void")
            .description("Completely empty void world")
            .environment(UnifiedWorld.Environment.NORMAL)
            .type(WorldType.FLAT)
            .generatorSettings("{\"layers\":[],\"biome\":\"minecraft:the_void\"}")
            .generateStructures(false)
            .keepSpawnLoaded(false)
            .build();

    /**
     * A skyblock template with a small starting island.
     *
     * @since 1.0.0
     */
    WorldTemplate SKYBLOCK = builder()
            .id("unified:skyblock")
            .displayName("Skyblock")
            .description("Skyblock island world")
            .environment(UnifiedWorld.Environment.NORMAL)
            .type(WorldType.CUSTOM)
            .generatorId("unified:skyblock")
            .generateStructures(false)
            .gameRule("doMobSpawning", false)
            .gameRule("doDaylightCycle", true)
            .keepSpawnLoaded(true)
            .build();

    /**
     * A creative world template with relaxed settings.
     *
     * @since 1.0.0
     */
    WorldTemplate CREATIVE = builder()
            .id("unified:creative")
            .displayName("Creative")
            .description("Creative mode world with peaceful settings")
            .environment(UnifiedWorld.Environment.NORMAL)
            .type(WorldType.FLAT)
            .difficulty(UnifiedWorld.Difficulty.PEACEFUL)
            .gameRule("doMobSpawning", false)
            .gameRule("doDaylightCycle", false)
            .gameRule("doWeatherCycle", false)
            .gameRule("keepInventory", true)
            .generateStructures(false)
            .build();

    /**
     * A hardcore survival template.
     *
     * @since 1.0.0
     */
    WorldTemplate HARDCORE = builder()
            .id("unified:hardcore")
            .displayName("Hardcore")
            .description("Hardcore survival world")
            .environment(UnifiedWorld.Environment.NORMAL)
            .type(WorldType.NORMAL)
            .difficulty(UnifiedWorld.Difficulty.HARD)
            .hardcore(true)
            .generateStructures(true)
            .gameRule("naturalRegeneration", false)
            .build();

    // ==================== Template Properties ====================

    /**
     * Gets the unique identifier for this template.
     *
     * @return the template ID
     * @since 1.0.0
     */
    @NotNull
    String getId();

    /**
     * Gets the display name for this template.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    String getDisplayName();

    /**
     * Gets the description of this template.
     *
     * @return the description, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getDescription();

    /**
     * Gets the world environment.
     *
     * @return the environment
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld.Environment getEnvironment();

    /**
     * Gets the world type.
     *
     * @return the world type
     * @since 1.0.0
     */
    @NotNull
    WorldType getType();

    /**
     * Gets the difficulty.
     *
     * @return the difficulty, or empty for default
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld.Difficulty> getDifficulty();

    /**
     * Checks if this template uses hardcore mode.
     *
     * @return true if hardcore
     * @since 1.0.0
     */
    boolean isHardcore();

    /**
     * Checks if structures should be generated.
     *
     * @return true if structures should generate
     * @since 1.0.0
     */
    boolean shouldGenerateStructures();

    /**
     * Gets the custom generator ID, if any.
     *
     * @return the generator ID, or empty for vanilla
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getGeneratorId();

    /**
     * Gets the generator settings string.
     *
     * @return the generator settings, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getGeneratorSettings();

    /**
     * Gets the configured game rules.
     *
     * @return an unmodifiable map of game rule names to values
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> getGameRules();

    /**
     * Gets the spawn location, if configured.
     *
     * @return the spawn coordinates [x, y, z], or empty if default
     * @since 1.0.0
     */
    @NotNull
    Optional<double[]> getSpawnLocation();

    /**
     * Checks if the spawn area should be kept loaded.
     *
     * @return true to keep spawn loaded
     * @since 1.0.0
     */
    boolean shouldKeepSpawnLoaded();

    /**
     * Gets the world border configuration.
     *
     * @return the border config, or empty for default
     * @since 1.0.0
     */
    @NotNull
    Optional<WorldBorderConfig> getWorldBorder();

    /**
     * Gets the world group this template assigns worlds to.
     *
     * @return the group name, or empty for none
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getGroup();

    /**
     * Applies this template to a world creator.
     *
     * @param creator the world creator to configure
     * @since 1.0.0
     */
    void applyTo(@NotNull WorldCreator creator);

    // ==================== Builder ====================

    /**
     * Creates a new template builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new WorldTemplateBuilder();
    }

    /**
     * Creates a builder from an existing template (for modification).
     *
     * @param template the template to copy
     * @return a new builder with copied values
     * @since 1.0.0
     */
    @NotNull
    static Builder builder(@NotNull WorldTemplate template) {
        return new WorldTemplateBuilder(template);
    }

    /**
     * Builder for creating world templates.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the template ID.
         *
         * @param id the unique identifier
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder id(@NotNull String id);

        /**
         * Sets the display name.
         *
         * @param name the display name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder displayName(@NotNull String name);

        /**
         * Sets the description.
         *
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder description(@Nullable String description);

        /**
         * Sets the environment.
         *
         * @param environment the world environment
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder environment(@NotNull UnifiedWorld.Environment environment);

        /**
         * Sets the world type.
         *
         * @param type the world type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder type(@NotNull WorldType type);

        /**
         * Sets the difficulty.
         *
         * @param difficulty the difficulty
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder difficulty(@NotNull UnifiedWorld.Difficulty difficulty);

        /**
         * Sets hardcore mode.
         *
         * @param hardcore true for hardcore
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder hardcore(boolean hardcore);

        /**
         * Sets structure generation.
         *
         * @param generate true to generate structures
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder generateStructures(boolean generate);

        /**
         * Sets the custom generator ID.
         *
         * @param generatorId the generator ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder generatorId(@NotNull String generatorId);

        /**
         * Sets the generator settings string.
         *
         * @param settings the settings string
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder generatorSettings(@NotNull String settings);

        /**
         * Adds a boolean game rule.
         *
         * @param rule  the game rule name
         * @param value the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder gameRule(@NotNull String rule, boolean value);

        /**
         * Adds an integer game rule.
         *
         * @param rule  the game rule name
         * @param value the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder gameRule(@NotNull String rule, int value);

        /**
         * Configures game rules via callback.
         *
         * @param configurator the game rules configurator
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder gameRules(@NotNull Consumer<WorldCreator.GameRuleConfigurator> configurator);

        /**
         * Sets the spawn location.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @param z the z coordinate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder spawnLocation(double x, double y, double z);

        /**
         * Sets whether to keep spawn loaded.
         *
         * @param keep true to keep loaded
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder keepSpawnLoaded(boolean keep);

        /**
         * Configures the world border.
         *
         * @param configurator the border configurator
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder worldBorder(@NotNull Consumer<WorldCreator.WorldBorderConfigurator> configurator);

        /**
         * Sets the world group.
         *
         * @param groupName the group name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder group(@NotNull String groupName);

        /**
         * Builds the world template.
         *
         * @return the built template
         * @throws IllegalStateException if required fields are missing
         * @since 1.0.0
         */
        @NotNull
        WorldTemplate build();
    }

    /**
     * Configuration for world borders in templates.
     *
     * @param centerX        the border center X
     * @param centerZ        the border center Z
     * @param size           the border size (diameter)
     * @param damageAmount   damage per block outside border
     * @param damageBuffer   safe buffer zone
     * @param warningDistance warning distance in blocks
     * @param warningTime    warning time in seconds
     * @since 1.0.0
     */
    record WorldBorderConfig(
            double centerX,
            double centerZ,
            double size,
            double damageAmount,
            double damageBuffer,
            int warningDistance,
            int warningTime
    ) {
        /**
         * Creates a default border config with standard settings.
         *
         * @param size the border size
         * @return a new config with defaults
         * @since 1.0.0
         */
        @NotNull
        public static WorldBorderConfig ofSize(double size) {
            return new WorldBorderConfig(0, 0, size, 0.2, 5.0, 5, 15);
        }
    }
}
