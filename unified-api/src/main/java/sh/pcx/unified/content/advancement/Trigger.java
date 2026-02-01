/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Represents a trigger condition for advancement criteria.
 *
 * <p>Triggers define when a criterion is considered complete. Built-in
 * triggers cover common gameplay events, and custom triggers can be
 * created for plugin-specific conditions.
 *
 * <h2>Built-in Triggers</h2>
 * <ul>
 *   <li>{@link #impossible()} - Only granted programmatically</li>
 *   <li>{@link #tick()} - Every tick (use with conditions)</li>
 *   <li>{@link #playerKilledEntity(EntityPredicate)} - Kill entity</li>
 *   <li>{@link #entityKilledPlayer(EntityPredicate)} - Killed by entity</li>
 *   <li>{@link #inventory(ItemPredicate)} - Has item in inventory</li>
 *   <li>{@link #changedDimension(String)} - Dimension change</li>
 *   <li>{@link #location(LocationPredicate)} - At location</li>
 *   <li>{@link #sleptInBed()} - Used bed</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Kill any zombie
 * Trigger killZombie = Trigger.playerKilledEntity(
 *     EntityPredicate.type("minecraft:zombie"));
 *
 * // Kill 100 zombies
 * Trigger kill100Zombies = Trigger.playerKilledEntity(
 *     EntityPredicate.type("minecraft:zombie")).count(100);
 *
 * // Enter the nether
 * Trigger enterNether = Trigger.changedDimension("minecraft:the_nether");
 *
 * // Custom trigger
 * CustomTrigger bossKilled = advancements.createTrigger("myplugin:boss_killed");
 * Trigger killDragonBoss = bossKilled.matching(data ->
 *     "dragon".equals(data.getString("boss_type")));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomAdvancement
 * @see AdvancementBuilder
 */
public interface Trigger {

    /**
     * Returns the trigger type identifier.
     *
     * @return the trigger type
     * @since 1.0.0
     */
    @NotNull
    String getType();

    /**
     * Returns a trigger that requires a count of completions.
     *
     * @param count the required count
     * @return a counting trigger
     * @since 1.0.0
     */
    @NotNull
    default Trigger count(int count) {
        return new CountingTrigger(this, count);
    }

    // === Factory Methods ===

    /**
     * Creates a trigger that can only be granted programmatically.
     *
     * @return an impossible trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger impossible() {
        return ImpossibleTrigger.INSTANCE;
    }

    /**
     * Creates a trigger that fires every tick.
     *
     * <p>Should be used with conditions to avoid constant firing.
     *
     * @return a tick trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger tick() {
        return TickTrigger.INSTANCE;
    }

    /**
     * Creates a trigger for killing an entity.
     *
     * @param predicate the entity predicate
     * @return a player killed entity trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger playerKilledEntity(@NotNull EntityPredicate predicate) {
        return new PlayerKilledEntityTrigger(predicate);
    }

    /**
     * Creates a trigger for being killed by an entity.
     *
     * @param predicate the killer entity predicate
     * @return an entity killed player trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger entityKilledPlayer(@NotNull EntityPredicate predicate) {
        return new EntityKilledPlayerTrigger(predicate);
    }

    /**
     * Creates a trigger for having an item in inventory.
     *
     * @param predicate the item predicate
     * @return an inventory trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger inventory(@NotNull ItemPredicate predicate) {
        return new InventoryTrigger(predicate);
    }

    /**
     * Creates a trigger for changing dimensions.
     *
     * @param toDimension the destination dimension key
     * @return a changed dimension trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger changedDimension(@NotNull String toDimension) {
        return new ChangedDimensionTrigger(null, toDimension);
    }

    /**
     * Creates a trigger for changing dimensions with source.
     *
     * @param fromDimension the source dimension key (null for any)
     * @param toDimension   the destination dimension key
     * @return a changed dimension trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger changedDimension(String fromDimension, @NotNull String toDimension) {
        return new ChangedDimensionTrigger(fromDimension, toDimension);
    }

    /**
     * Creates a trigger for being at a location.
     *
     * @param predicate the location predicate
     * @return a location trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger location(@NotNull LocationPredicate predicate) {
        return new LocationTrigger(predicate);
    }

    /**
     * Creates a trigger for sleeping in a bed.
     *
     * @return a slept in bed trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger sleptInBed() {
        return SleptInBedTrigger.INSTANCE;
    }

    /**
     * Creates a trigger for using an ender eye.
     *
     * @return an ender eye trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger usedEnderEye() {
        return UsedEnderEyeTrigger.INSTANCE;
    }

    /**
     * Creates a trigger for summoning an entity.
     *
     * @param predicate the summoned entity predicate
     * @return a summoned entity trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger summonedEntity(@NotNull EntityPredicate predicate) {
        return new SummonedEntityTrigger(predicate);
    }

    /**
     * Creates a trigger for levitating a distance.
     *
     * @param minDistance the minimum distance in blocks
     * @return a levitation trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger levitation(double minDistance) {
        return new LevitationTrigger(minDistance);
    }

    /**
     * Creates a trigger for crafting an item.
     *
     * @param predicate the crafted item predicate
     * @return a recipe crafted trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger recipeCrafted(@NotNull ItemPredicate predicate) {
        return new RecipeCraftedTrigger(predicate);
    }

    /**
     * Creates a trigger for consuming an item.
     *
     * @param predicate the consumed item predicate
     * @return a consume item trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger consumeItem(@NotNull ItemPredicate predicate) {
        return new ConsumeItemTrigger(predicate);
    }

    /**
     * Creates a trigger for breaking a block.
     *
     * @param blockType the block type ID
     * @return a placed block trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger brokeBlock(@NotNull String blockType) {
        return new BrokeBlockTrigger(blockType);
    }

    /**
     * Creates a trigger for placing a block.
     *
     * @param blockType the block type ID
     * @return a placed block trigger
     * @since 1.0.0
     */
    @NotNull
    static Trigger placedBlock(@NotNull String blockType) {
        return new PlacedBlockTrigger(blockType);
    }
}

// === Trigger Implementations ===

final class ImpossibleTrigger implements Trigger {
    static final ImpossibleTrigger INSTANCE = new ImpossibleTrigger();
    private ImpossibleTrigger() {}

    @Override
    @NotNull
    public String getType() {
        return "minecraft:impossible";
    }
}

final class TickTrigger implements Trigger {
    static final TickTrigger INSTANCE = new TickTrigger();
    private TickTrigger() {}

    @Override
    @NotNull
    public String getType() {
        return "minecraft:tick";
    }
}

record PlayerKilledEntityTrigger(EntityPredicate predicate) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:player_killed_entity";
    }
}

record EntityKilledPlayerTrigger(EntityPredicate predicate) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:entity_killed_player";
    }
}

record InventoryTrigger(ItemPredicate predicate) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:inventory_changed";
    }
}

record ChangedDimensionTrigger(String from, String to) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:changed_dimension";
    }
}

record LocationTrigger(LocationPredicate predicate) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:location";
    }
}

final class SleptInBedTrigger implements Trigger {
    static final SleptInBedTrigger INSTANCE = new SleptInBedTrigger();
    private SleptInBedTrigger() {}

    @Override
    @NotNull
    public String getType() {
        return "minecraft:slept_in_bed";
    }
}

final class UsedEnderEyeTrigger implements Trigger {
    static final UsedEnderEyeTrigger INSTANCE = new UsedEnderEyeTrigger();
    private UsedEnderEyeTrigger() {}

    @Override
    @NotNull
    public String getType() {
        return "minecraft:used_ender_eye";
    }
}

record SummonedEntityTrigger(EntityPredicate predicate) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:summoned_entity";
    }
}

record LevitationTrigger(double minDistance) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:levitation";
    }
}

record RecipeCraftedTrigger(ItemPredicate predicate) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:recipe_crafted";
    }
}

record ConsumeItemTrigger(ItemPredicate predicate) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:consume_item";
    }
}

record BrokeBlockTrigger(String blockType) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:item_used_on_block";
    }
}

record PlacedBlockTrigger(String blockType) implements Trigger {
    @Override
    @NotNull
    public String getType() {
        return "minecraft:placed_block";
    }
}

/**
 * Predicate for matching entities.
 *
 * @since 1.0.0
 */
record EntityPredicate(
        String type,
        String category,
        String nbt
) {
    /**
     * Creates a predicate matching an entity type.
     *
     * @param type the entity type ID
     * @return an EntityPredicate
     */
    @NotNull
    public static EntityPredicate type(@NotNull String type) {
        return new EntityPredicate(type, null, null);
    }

    /**
     * Creates a predicate matching multiple entity types.
     *
     * @param types the entity type IDs
     * @return an EntityPredicate
     */
    @NotNull
    public static EntityPredicate type(@NotNull String... types) {
        return new EntityPredicate(String.join(",", types), null, null);
    }

    /**
     * Creates a predicate matching an entity category.
     *
     * @param category the category (e.g., "monster", "animal")
     * @return an EntityPredicate
     */
    @NotNull
    public static EntityPredicate category(@NotNull String category) {
        return new EntityPredicate(null, category, null);
    }
}

/**
 * Predicate for matching items.
 *
 * @since 1.0.0
 */
record ItemPredicate(
        String type,
        String tag,
        Integer minCount,
        Integer maxCount
) {
    /**
     * Creates a predicate matching an item type.
     *
     * @param type the item type ID
     * @return an ItemPredicate
     */
    @NotNull
    public static ItemPredicate type(@NotNull String type) {
        return new ItemPredicate(type, null, null, null);
    }

    /**
     * Creates a predicate matching an item tag.
     *
     * @param tag the item tag
     * @return an ItemPredicate
     */
    @NotNull
    public static ItemPredicate tag(@NotNull String tag) {
        return new ItemPredicate(null, tag, null, null);
    }
}

/**
 * Predicate for matching locations.
 *
 * @since 1.0.0
 */
record LocationPredicate(
        String dimension,
        String biome,
        String structure,
        Double x, Double y, Double z,
        Double radius
) {
    /**
     * Creates a predicate matching a dimension.
     *
     * @param dimension the dimension key
     * @return a LocationPredicate
     */
    @NotNull
    public static LocationPredicate dimension(@NotNull String dimension) {
        return new LocationPredicate(dimension, null, null, null, null, null, null);
    }

    /**
     * Creates a predicate matching a biome.
     *
     * @param biome the biome key
     * @return a LocationPredicate
     */
    @NotNull
    public static LocationPredicate biome(@NotNull String biome) {
        return new LocationPredicate(null, biome, null, null, null, null, null);
    }

    /**
     * Creates a predicate matching coordinates.
     *
     * @param x      the X coordinate
     * @param y      the Y coordinate
     * @param z      the Z coordinate
     * @param radius the radius
     * @return a LocationPredicate
     */
    @NotNull
    public static LocationPredicate at(double x, double y, double z, double radius) {
        return new LocationPredicate(null, null, null, x, y, z, radius);
    }
}
