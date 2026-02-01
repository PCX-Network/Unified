/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Context information for loot generation.
 *
 * <p>LootContext provides all relevant information about the circumstances
 * of loot generation, including the killer, tool used, location, and
 * various enchantment levels.
 *
 * <h2>Context Data</h2>
 * <ul>
 *   <li><b>Killer</b> - The player who killed the entity</li>
 *   <li><b>Tool</b> - The item used to kill or break</li>
 *   <li><b>Location</b> - Where the loot is being generated</li>
 *   <li><b>Looting Level</b> - Looting enchantment level</li>
 *   <li><b>Luck</b> - Luck attribute value</li>
 *   <li><b>Weather</b> - Current weather condition</li>
 *   <li><b>Time</b> - Current time of day</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LootContext context = LootContext.builder()
 *     .killer(player)
 *     .tool(player.getInventory().getMainHand())
 *     .location(entity.getLocation())
 *     .entityType("minecraft:zombie")
 *     .lootingLevel(3)
 *     .build();
 *
 * List<UnifiedItemStack> drops = lootTables.generate(table, context);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootTableService
 */
public interface LootContext {

    /**
     * Returns the killer player, if any.
     *
     * @return an Optional containing the killer player
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedPlayer> getKillerPlayer();

    /**
     * Returns the killer entity UUID, if any.
     *
     * @return an Optional containing the killer entity UUID
     * @since 1.0.0
     */
    @NotNull
    Optional<UUID> getKillerEntity();

    /**
     * Returns the tool used, if any.
     *
     * @return an Optional containing the tool
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedItemStack> getTool();

    /**
     * Returns the location of loot generation.
     *
     * @return an Optional containing the location
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedLocation> getLocation();

    /**
     * Returns the entity type being looted.
     *
     * @return an Optional containing the entity type ID
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getEntityType();

    /**
     * Returns the looting enchantment level.
     *
     * @return the looting level (0 if none)
     * @since 1.0.0
     */
    int getLootingLevel();

    /**
     * Returns the luck attribute value.
     *
     * @return the luck value
     * @since 1.0.0
     */
    float getLuck();

    /**
     * Returns the current weather.
     *
     * @return the weather type ("clear", "rain", "thunder")
     * @since 1.0.0
     */
    @NotNull
    String getWeather();

    /**
     * Returns the time of day.
     *
     * @return the time (0-24000)
     * @since 1.0.0
     */
    long getTimeOfDay();

    /**
     * Returns the random instance for this context.
     *
     * @return the random instance
     * @since 1.0.0
     */
    @NotNull
    Random getRandom();

    /**
     * Gets the level of an enchantment on the tool.
     *
     * @param enchantment the enchantment key
     * @return the enchantment level (0 if not present)
     * @since 1.0.0
     */
    int getEnchantmentLevel(@NotNull String enchantment);

    /**
     * Enchants an item randomly.
     *
     * @param item            the item to enchant
     * @param count           number of enchantments
     * @param includeTreasure whether to include treasure enchantments
     * @return the enchanted item
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack enchantRandomly(@NotNull UnifiedItemStack item, int count, boolean includeTreasure);

    /**
     * Enchants an item with a specific level.
     *
     * @param item            the item to enchant
     * @param level           the enchanting level
     * @param includeTreasure whether to include treasure enchantments
     * @return the enchanted item
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack enchantWithLevels(@NotNull UnifiedItemStack item, int level, boolean includeTreasure);

    /**
     * Creates a new context builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for LootContext.
     *
     * @since 1.0.0
     */
    final class Builder {
        private UnifiedPlayer killerPlayer;
        private UUID killerEntity;
        private UnifiedItemStack tool;
        private UnifiedLocation location;
        private String entityType;
        private int lootingLevel = 0;
        private float luck = 0.0f;
        private String weather = "clear";
        private long timeOfDay = 6000;
        private Random random = new Random();

        /**
         * Sets the killer player.
         *
         * @param player the killer player
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder killer(@Nullable UnifiedPlayer player) {
            this.killerPlayer = player;
            return this;
        }

        /**
         * Sets the killer entity UUID.
         *
         * @param entityId the killer entity UUID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder killerEntity(@Nullable UUID entityId) {
            this.killerEntity = entityId;
            return this;
        }

        /**
         * Sets the tool used.
         *
         * @param tool the tool
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder tool(@Nullable UnifiedItemStack tool) {
            this.tool = tool;
            return this;
        }

        /**
         * Sets the location.
         *
         * @param location the location
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder location(@Nullable UnifiedLocation location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the entity type.
         *
         * @param entityType the entity type ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder entityType(@Nullable String entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * Sets the looting level.
         *
         * @param level the looting level
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder lootingLevel(int level) {
            this.lootingLevel = level;
            return this;
        }

        /**
         * Sets the luck value.
         *
         * @param luck the luck value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder luck(float luck) {
            this.luck = luck;
            return this;
        }

        /**
         * Sets the weather.
         *
         * @param weather the weather type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder weather(@NotNull String weather) {
            this.weather = weather;
            return this;
        }

        /**
         * Sets the time of day.
         *
         * @param time the time (0-24000)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder timeOfDay(long time) {
            this.timeOfDay = time;
            return this;
        }

        /**
         * Sets the random instance.
         *
         * @param random the random instance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder random(@NotNull Random random) {
            this.random = random;
            return this;
        }

        /**
         * Builds the loot context.
         *
         * @return the constructed LootContext
         * @since 1.0.0
         */
        @NotNull
        public LootContext build() {
            return new LootContextImpl(
                    killerPlayer, killerEntity, tool, location, entityType,
                    lootingLevel, luck, weather, timeOfDay, random
            );
        }
    }
}

/**
 * Default implementation of LootContext.
 */
record LootContextImpl(
        UnifiedPlayer killerPlayer,
        UUID killerEntity,
        UnifiedItemStack tool,
        UnifiedLocation location,
        String entityType,
        int lootingLevel,
        float luck,
        String weather,
        long timeOfDay,
        Random random
) implements LootContext {

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getKillerPlayer() {
        return Optional.ofNullable(killerPlayer);
    }

    @Override
    @NotNull
    public Optional<UUID> getKillerEntity() {
        return Optional.ofNullable(killerEntity);
    }

    @Override
    @NotNull
    public Optional<UnifiedItemStack> getTool() {
        return Optional.ofNullable(tool);
    }

    @Override
    @NotNull
    public Optional<UnifiedLocation> getLocation() {
        return Optional.ofNullable(location);
    }

    @Override
    @NotNull
    public Optional<String> getEntityType() {
        return Optional.ofNullable(entityType);
    }

    @Override
    public int getLootingLevel() {
        return lootingLevel;
    }

    @Override
    public float getLuck() {
        return luck;
    }

    @Override
    @NotNull
    public String getWeather() {
        return weather;
    }

    @Override
    public long getTimeOfDay() {
        return timeOfDay;
    }

    @Override
    @NotNull
    public Random getRandom() {
        return random;
    }

    @Override
    public int getEnchantmentLevel(@NotNull String enchantment) {
        if (tool == null) return 0;
        return tool.getEnchantmentLevel(enchantment);
    }

    @Override
    @NotNull
    public UnifiedItemStack enchantRandomly(@NotNull UnifiedItemStack item, int count, boolean includeTreasure) {
        // Platform implementation will override this
        return item;
    }

    @Override
    @NotNull
    public UnifiedItemStack enchantWithLevels(@NotNull UnifiedItemStack item, int level, boolean includeTreasure) {
        // Platform implementation will override this
        return item;
    }
}
