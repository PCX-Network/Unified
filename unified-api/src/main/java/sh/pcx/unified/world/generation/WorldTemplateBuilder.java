/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Default implementation of the WorldTemplate.Builder interface.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
final class WorldTemplateBuilder implements WorldTemplate.Builder {

    private String id;
    private String displayName;
    private String description;
    private UnifiedWorld.Environment environment = UnifiedWorld.Environment.NORMAL;
    private WorldType type = WorldType.NORMAL;
    private UnifiedWorld.Difficulty difficulty;
    private boolean hardcore = false;
    private boolean generateStructures = true;
    private String generatorId;
    private String generatorSettings;
    private final Map<String, Object> gameRules = new HashMap<>();
    private double[] spawnLocation;
    private boolean keepSpawnLoaded = true;
    private WorldTemplate.WorldBorderConfig worldBorder;
    private String group;

    WorldTemplateBuilder() {
    }

    WorldTemplateBuilder(@NotNull WorldTemplate template) {
        this.id = template.getId();
        this.displayName = template.getDisplayName();
        this.description = template.getDescription().orElse(null);
        this.environment = template.getEnvironment();
        this.type = template.getType();
        this.difficulty = template.getDifficulty().orElse(null);
        this.hardcore = template.isHardcore();
        this.generateStructures = template.shouldGenerateStructures();
        this.generatorId = template.getGeneratorId().orElse(null);
        this.generatorSettings = template.getGeneratorSettings().orElse(null);
        this.gameRules.putAll(template.getGameRules());
        this.spawnLocation = template.getSpawnLocation().orElse(null);
        this.keepSpawnLoaded = template.shouldKeepSpawnLoaded();
        this.worldBorder = template.getWorldBorder().orElse(null);
        this.group = template.getGroup().orElse(null);
    }

    @Override
    @NotNull
    public WorldTemplate.Builder id(@NotNull String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder displayName(@NotNull String name) {
        this.displayName = Objects.requireNonNull(name, "displayName cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder description(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder environment(@NotNull UnifiedWorld.Environment environment) {
        this.environment = Objects.requireNonNull(environment, "environment cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder type(@NotNull WorldType type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder difficulty(@NotNull UnifiedWorld.Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder hardcore(boolean hardcore) {
        this.hardcore = hardcore;
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder generateStructures(boolean generate) {
        this.generateStructures = generate;
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder generatorId(@NotNull String generatorId) {
        this.generatorId = Objects.requireNonNull(generatorId, "generatorId cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder generatorSettings(@NotNull String settings) {
        this.generatorSettings = Objects.requireNonNull(settings, "settings cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder gameRule(@NotNull String rule, boolean value) {
        this.gameRules.put(Objects.requireNonNull(rule, "rule cannot be null"), value);
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder gameRule(@NotNull String rule, int value) {
        this.gameRules.put(Objects.requireNonNull(rule, "rule cannot be null"), value);
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder gameRules(@NotNull Consumer<WorldCreator.GameRuleConfigurator> configurator) {
        var config = new GameRuleConfiguratorImpl();
        configurator.accept(config);
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder spawnLocation(double x, double y, double z) {
        this.spawnLocation = new double[]{x, y, z};
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder keepSpawnLoaded(boolean keep) {
        this.keepSpawnLoaded = keep;
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder worldBorder(@NotNull Consumer<WorldCreator.WorldBorderConfigurator> configurator) {
        var config = new WorldBorderConfiguratorImpl();
        configurator.accept(config);
        this.worldBorder = config.build();
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate.Builder group(@NotNull String groupName) {
        this.group = Objects.requireNonNull(groupName, "groupName cannot be null");
        return this;
    }

    @Override
    @NotNull
    public WorldTemplate build() {
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Template ID is required");
        }
        if (displayName == null) {
            displayName = id;
        }
        return new WorldTemplateImpl(this);
    }

    // Internal record implementing WorldTemplate
    private record WorldTemplateImpl(
            String id,
            String displayName,
            String description,
            UnifiedWorld.Environment environment,
            WorldType type,
            UnifiedWorld.Difficulty difficulty,
            boolean hardcore,
            boolean generateStructures,
            String generatorId,
            String generatorSettings,
            Map<String, Object> gameRules,
            double[] spawnLocation,
            boolean keepSpawnLoaded,
            WorldBorderConfig worldBorder,
            String group
    ) implements WorldTemplate {

        WorldTemplateImpl(WorldTemplateBuilder builder) {
            this(
                    builder.id,
                    builder.displayName,
                    builder.description,
                    builder.environment,
                    builder.type,
                    builder.difficulty,
                    builder.hardcore,
                    builder.generateStructures,
                    builder.generatorId,
                    builder.generatorSettings,
                    Map.copyOf(builder.gameRules),
                    builder.spawnLocation != null ? builder.spawnLocation.clone() : null,
                    builder.keepSpawnLoaded,
                    builder.worldBorder,
                    builder.group
            );
        }

        @Override
        @NotNull
        public String getId() {
            return id;
        }

        @Override
        @NotNull
        public String getDisplayName() {
            return displayName;
        }

        @Override
        @NotNull
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        @NotNull
        public UnifiedWorld.Environment getEnvironment() {
            return environment;
        }

        @Override
        @NotNull
        public WorldType getType() {
            return type;
        }

        @Override
        @NotNull
        public Optional<UnifiedWorld.Difficulty> getDifficulty() {
            return Optional.ofNullable(difficulty);
        }

        @Override
        public boolean isHardcore() {
            return hardcore;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return generateStructures;
        }

        @Override
        @NotNull
        public Optional<String> getGeneratorId() {
            return Optional.ofNullable(generatorId);
        }

        @Override
        @NotNull
        public Optional<String> getGeneratorSettings() {
            return Optional.ofNullable(generatorSettings);
        }

        @Override
        @NotNull
        public Map<String, Object> getGameRules() {
            return gameRules;
        }

        @Override
        @NotNull
        public Optional<double[]> getSpawnLocation() {
            return Optional.ofNullable(spawnLocation != null ? spawnLocation.clone() : null);
        }

        @Override
        public boolean shouldKeepSpawnLoaded() {
            return keepSpawnLoaded;
        }

        @Override
        @NotNull
        public Optional<WorldBorderConfig> getWorldBorder() {
            return Optional.ofNullable(worldBorder);
        }

        @Override
        @NotNull
        public Optional<String> getGroup() {
            return Optional.ofNullable(group);
        }

        @Override
        public void applyTo(@NotNull WorldCreator creator) {
            creator.environment(environment);
            creator.type(type);
            creator.generateStructures(generateStructures);
            creator.hardcore(hardcore);
            creator.keepSpawnLoaded(keepSpawnLoaded);

            if (difficulty != null) {
                creator.difficulty(difficulty);
            }

            if (generatorId != null) {
                creator.generator(generatorId);
            }

            if (generatorSettings != null) {
                creator.generatorSettings(generatorSettings);
            }

            for (var entry : gameRules.entrySet()) {
                if (entry.getValue() instanceof Boolean b) {
                    creator.gameRule(entry.getKey(), b);
                } else if (entry.getValue() instanceof Integer i) {
                    creator.gameRule(entry.getKey(), i);
                }
            }

            if (spawnLocation != null) {
                creator.spawnLocation(spawnLocation[0], spawnLocation[1], spawnLocation[2]);
            }

            if (worldBorder != null) {
                creator.worldBorder(border -> {
                    border.setCenter(worldBorder.centerX(), worldBorder.centerZ());
                    border.setSize(worldBorder.size());
                    border.setDamageAmount(worldBorder.damageAmount());
                    border.setDamageBuffer(worldBorder.damageBuffer());
                    border.setWarningDistance(worldBorder.warningDistance());
                    border.setWarningTime(worldBorder.warningTime());
                });
            }

            if (group != null) {
                creator.group(group);
            }
        }
    }

    // Internal game rule configurator
    private class GameRuleConfiguratorImpl implements WorldCreator.GameRuleConfigurator {
        @Override
        @NotNull
        public WorldCreator.GameRuleConfigurator set(@NotNull String rule, boolean value) {
            gameRules.put(rule, value);
            return this;
        }

        @Override
        @NotNull
        public WorldCreator.GameRuleConfigurator set(@NotNull String rule, int value) {
            gameRules.put(rule, value);
            return this;
        }

        @Override
        @NotNull
        public WorldCreator.GameRuleConfigurator copyFrom(@NotNull UnifiedWorld source) {
            // This would need platform-specific implementation
            return this;
        }
    }

    // Internal world border configurator
    private static class WorldBorderConfiguratorImpl implements WorldCreator.WorldBorderConfigurator {
        private double centerX = 0;
        private double centerZ = 0;
        private double size = 60000000;
        private double damageAmount = 0.2;
        private double damageBuffer = 5.0;
        private int warningDistance = 5;
        private int warningTime = 15;

        @Override
        @NotNull
        public WorldCreator.WorldBorderConfigurator setCenter(double x, double z) {
            this.centerX = x;
            this.centerZ = z;
            return this;
        }

        @Override
        @NotNull
        public WorldCreator.WorldBorderConfigurator setSize(double size) {
            this.size = size;
            return this;
        }

        @Override
        @NotNull
        public WorldCreator.WorldBorderConfigurator setDamageAmount(double damage) {
            this.damageAmount = damage;
            return this;
        }

        @Override
        @NotNull
        public WorldCreator.WorldBorderConfigurator setDamageBuffer(double buffer) {
            this.damageBuffer = buffer;
            return this;
        }

        @Override
        @NotNull
        public WorldCreator.WorldBorderConfigurator setWarningDistance(int distance) {
            this.warningDistance = distance;
            return this;
        }

        @Override
        @NotNull
        public WorldCreator.WorldBorderConfigurator setWarningTime(int seconds) {
            this.warningTime = seconds;
            return this;
        }

        WorldTemplate.WorldBorderConfig build() {
            return new WorldTemplate.WorldBorderConfig(
                    centerX, centerZ, size, damageAmount, damageBuffer, warningDistance, warningTime
            );
        }
    }
}
