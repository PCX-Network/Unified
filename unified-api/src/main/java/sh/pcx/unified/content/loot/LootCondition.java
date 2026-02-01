/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import sh.pcx.unified.content.loot.LootTypes.ItemPredicate;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Represents a condition that must be met for loot to be generated.
 *
 * <p>LootCondition defines requirements that are checked before a pool
 * or entry is processed. Multiple conditions are combined with AND logic.
 *
 * <h2>Built-in Conditions</h2>
 * <ul>
 *   <li>{@link #randomChance(float)} - Random probability check</li>
 *   <li>{@link #randomChanceWithLooting(float, float)} - Chance + looting bonus</li>
 *   <li>{@link #killedByPlayer()} - Requires player kill</li>
 *   <li>{@link #entityProperties(String)} - Entity type check</li>
 *   <li>{@link #weatherCheck(String)} - Weather condition</li>
 *   <li>{@link #timeCheck(int, int)} - Time of day check</li>
 *   <li>{@link #locationCheck(Predicate)} - Location predicate</li>
 *   <li>{@link #matchTool(ItemPredicate)} - Tool requirement</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple random chance
 * LootCondition rare = LootCondition.randomChance(0.05f);
 *
 * // Looting-affected chance
 * LootCondition looting = LootCondition.randomChanceWithLooting(0.1f, 0.02f);
 *
 * // Combined conditions
 * LootCondition combined = LootCondition.all(
 *     LootCondition.killedByPlayer(),
 *     LootCondition.randomChance(0.25f),
 *     LootCondition.weatherCheck("thunder")
 * );
 *
 * // Custom condition
 * LootCondition custom = LootCondition.custom(ctx ->
 *     ctx.getKillerPlayer() != null &&
 *     ctx.getKillerPlayer().hasPermission("vip.bonus_loot"));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootPool
 * @see LootEntry
 */
public interface LootCondition {

    /**
     * Tests if this condition is met for the given context.
     *
     * @param context the loot context
     * @return true if the condition is satisfied
     * @since 1.0.0
     */
    boolean test(@NotNull LootContext context);

    /**
     * Returns the condition type identifier.
     *
     * @return the condition type
     * @since 1.0.0
     */
    @NotNull
    String getType();

    // === Factory Methods ===

    /**
     * Creates a random chance condition.
     *
     * @param chance the probability (0.0-1.0)
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition randomChance(float chance) {
        return new RandomChanceCondition(chance);
    }

    /**
     * Creates a random chance condition with looting bonus.
     *
     * @param baseChance      the base probability
     * @param lootingModifier additional chance per looting level
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition randomChanceWithLooting(float baseChance, float lootingModifier) {
        return new RandomChanceWithLootingCondition(baseChance, lootingModifier);
    }

    /**
     * Creates a condition requiring the entity to be killed by a player.
     *
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition killedByPlayer() {
        return KilledByPlayerCondition.INSTANCE;
    }

    /**
     * Creates an entity type check condition.
     *
     * @param entityType the entity type ID
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition entityProperties(@NotNull String entityType) {
        return new EntityPropertiesCondition(entityType);
    }

    /**
     * Creates a weather check condition.
     *
     * @param weather the weather type ("clear", "rain", "thunder")
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition weatherCheck(@NotNull String weather) {
        return new WeatherCheckCondition(weather);
    }

    /**
     * Creates a time of day check condition.
     *
     * @param minTime the minimum time (0-24000)
     * @param maxTime the maximum time (0-24000)
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition timeCheck(int minTime, int maxTime) {
        return new TimeCheckCondition(minTime, maxTime);
    }

    /**
     * Creates a location predicate condition.
     *
     * @param predicate the location predicate
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition locationCheck(@NotNull Predicate<UnifiedLocation> predicate) {
        return new LocationCheckCondition(predicate);
    }

    /**
     * Creates a tool requirement condition.
     *
     * @param predicate the tool predicate
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition matchTool(@NotNull ItemPredicate predicate) {
        return new MatchToolCondition(predicate);
    }

    /**
     * Creates a table bonus condition (e.g., Fortune).
     *
     * @param enchantment the enchantment key
     * @param chances     the chances per level (index 0 = level 0)
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition tableBonus(@NotNull String enchantment, float... chances) {
        return new TableBonusCondition(enchantment, chances);
    }

    /**
     * Creates a custom condition with a predicate.
     *
     * @param predicate the condition predicate
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition custom(@NotNull Predicate<LootContext> predicate) {
        return new CustomCondition(predicate);
    }

    /**
     * Creates a condition that inverts another condition.
     *
     * @param condition the condition to invert
     * @return the inverted condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition not(@NotNull LootCondition condition) {
        return new InvertedCondition(condition);
    }

    /**
     * Creates a condition requiring all sub-conditions to pass.
     *
     * @param conditions the conditions
     * @return the combined condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition all(@NotNull LootCondition... conditions) {
        return new AllOfCondition(conditions);
    }

    /**
     * Creates a condition requiring any sub-condition to pass.
     *
     * @param conditions the conditions
     * @return the combined condition
     * @since 1.0.0
     */
    @NotNull
    static LootCondition any(@NotNull LootCondition... conditions) {
        return new AnyOfCondition(conditions);
    }
}

// === Condition Implementations ===

record RandomChanceCondition(float chance) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        return context.getRandom().nextFloat() < chance;
    }

    @Override
    @NotNull
    public String getType() {
        return "random_chance";
    }
}

record RandomChanceWithLootingCondition(float baseChance, float lootingModifier) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        float chance = baseChance + (context.getLootingLevel() * lootingModifier);
        return context.getRandom().nextFloat() < chance;
    }

    @Override
    @NotNull
    public String getType() {
        return "random_chance_with_looting";
    }
}

final class KilledByPlayerCondition implements LootCondition {
    static final KilledByPlayerCondition INSTANCE = new KilledByPlayerCondition();

    private KilledByPlayerCondition() {}

    @Override
    public boolean test(@NotNull LootContext context) {
        return context.getKillerPlayer().isPresent();
    }

    @Override
    @NotNull
    public String getType() {
        return "killed_by_player";
    }
}

record EntityPropertiesCondition(String entityType) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        return context.getEntityType().map(t -> t.equalsIgnoreCase(entityType)).orElse(false);
    }

    @Override
    @NotNull
    public String getType() {
        return "entity_properties";
    }
}

record WeatherCheckCondition(String weather) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        return context.getWeather().equalsIgnoreCase(weather);
    }

    @Override
    @NotNull
    public String getType() {
        return "weather_check";
    }
}

record TimeCheckCondition(int minTime, int maxTime) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        long time = context.getTimeOfDay();
        return time >= minTime && time <= maxTime;
    }

    @Override
    @NotNull
    public String getType() {
        return "time_check";
    }
}

record LocationCheckCondition(Predicate<UnifiedLocation> predicate) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        return context.getLocation().map(predicate::test).orElse(false);
    }

    @Override
    @NotNull
    public String getType() {
        return "location_check";
    }
}

record MatchToolCondition(ItemPredicate predicate) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        return context.getTool().map(predicate::test).orElse(false);
    }

    @Override
    @NotNull
    public String getType() {
        return "match_tool";
    }
}

record TableBonusCondition(String enchantment, float[] chances) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        int level = context.getEnchantmentLevel(enchantment);
        if (level >= chances.length) {
            level = chances.length - 1;
        }
        return context.getRandom().nextFloat() < chances[level];
    }

    @Override
    @NotNull
    public String getType() {
        return "table_bonus";
    }
}

record CustomCondition(Predicate<LootContext> predicate) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        return predicate.test(context);
    }

    @Override
    @NotNull
    public String getType() {
        return "custom";
    }
}

record InvertedCondition(LootCondition condition) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        return !condition.test(context);
    }

    @Override
    @NotNull
    public String getType() {
        return "inverted";
    }
}

record AllOfCondition(LootCondition[] conditions) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        for (LootCondition condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @NotNull
    public String getType() {
        return "all_of";
    }
}

record AnyOfCondition(LootCondition[] conditions) implements LootCondition {
    @Override
    public boolean test(@NotNull LootContext context) {
        for (LootCondition condition : conditions) {
            if (condition.test(context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NotNull
    public String getType() {
        return "any_of";
    }
}
