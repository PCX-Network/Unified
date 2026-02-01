/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import sh.pcx.unified.content.loot.LootTypes.CountRange;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry in a loot pool that can produce items.
 *
 * <p>LootEntry defines what item(s) can be selected from a pool, along with
 * weight for probability, count ranges, conditions, and functions to modify
 * the resulting item.
 *
 * <h2>Entry Types</h2>
 * <ul>
 *   <li><b>Item</b> - Drops a specific item type</li>
 *   <li><b>Tag</b> - Drops from an item tag group</li>
 *   <li><b>Table</b> - References another loot table</li>
 *   <li><b>Empty</b> - Produces nothing (for weighted no-drop)</li>
 *   <li><b>Group</b> - Contains child entries</li>
 *   <li><b>Alternatives</b> - First matching child is used</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple item entry
 * LootEntry diamond = LootEntry.item("minecraft:diamond")
 *     .weight(10)
 *     .count(CountRange.between(1, 3))
 *     .build();
 *
 * // Entry with functions
 * LootEntry enchantedSword = LootEntry.item("minecraft:diamond_sword")
 *     .weight(1)
 *     .function(LootFunction.enchantRandomly(3))
 *     .function(LootFunction.setDamage(DamageRange.between(0.5f, 1.0f)))
 *     .condition(LootCondition.killedByPlayer())
 *     .build();
 *
 * // Empty entry for weighted nothing
 * LootEntry nothing = LootEntry.empty().weight(80).build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootPool
 * @see LootFunction
 */
public interface LootEntry {

    /**
     * Returns the entry type.
     *
     * @return the entry type
     * @since 1.0.0
     */
    @NotNull
    LootEntryType getType();

    /**
     * Returns the weight of this entry.
     *
     * @return the selection weight
     * @since 1.0.0
     */
    int getWeight();

    /**
     * Returns the quality bonus per luck level.
     *
     * @return the quality value
     * @since 1.0.0
     */
    int getQuality();

    /**
     * Returns the conditions for this entry.
     *
     * @return an unmodifiable list of conditions
     * @since 1.0.0
     */
    @NotNull
    List<LootCondition> getConditions();

    /**
     * Returns the functions to apply to generated items.
     *
     * @return an unmodifiable list of functions
     * @since 1.0.0
     */
    @NotNull
    List<LootFunction> getFunctions();

    /**
     * Creates an item entry builder.
     *
     * @param itemType the item type ID
     * @return a new entry builder
     * @since 1.0.0
     */
    @NotNull
    static Builder item(@NotNull String itemType) {
        return new Builder(LootEntryType.ITEM).itemType(itemType);
    }

    /**
     * Creates an item entry builder from an existing item.
     *
     * @param item the item to drop
     * @return a new entry builder
     * @since 1.0.0
     */
    @NotNull
    static Builder item(@NotNull UnifiedItemStack item) {
        return new Builder(LootEntryType.ITEM).item(item);
    }

    /**
     * Creates a tag entry builder.
     *
     * @param tag the item tag
     * @return a new entry builder
     * @since 1.0.0
     */
    @NotNull
    static Builder tag(@NotNull String tag) {
        return new Builder(LootEntryType.TAG).tag(tag);
    }

    /**
     * Creates a loot table reference entry.
     *
     * @param tableKey the referenced table key
     * @return a new entry builder
     * @since 1.0.0
     */
    @NotNull
    static Builder table(@NotNull String tableKey) {
        return new Builder(LootEntryType.LOOT_TABLE).tableKey(tableKey);
    }

    /**
     * Creates an empty entry (drops nothing).
     *
     * @return a new entry builder
     * @since 1.0.0
     */
    @NotNull
    static Builder empty() {
        return new Builder(LootEntryType.EMPTY);
    }

    /**
     * Fluent builder for LootEntry.
     *
     * @since 1.0.0
     */
    final class Builder {
        private final LootEntryType type;
        private String itemType;
        private UnifiedItemStack item;
        private String tag;
        private String tableKey;
        private int weight = 1;
        private int quality = 0;
        private CountRange count = CountRange.exactly(1);
        private final List<LootCondition> conditions = new ArrayList<>();
        private final List<LootFunction> functions = new ArrayList<>();

        Builder(LootEntryType type) {
            this.type = type;
        }

        Builder itemType(String itemType) {
            this.itemType = itemType;
            return this;
        }

        Builder item(UnifiedItemStack item) {
            this.item = item;
            return this;
        }

        Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        Builder tableKey(String tableKey) {
            this.tableKey = tableKey;
            return this;
        }

        /**
         * Sets the selection weight.
         *
         * @param weight the weight (higher = more likely)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        /**
         * Sets the quality bonus per luck level.
         *
         * @param quality the quality bonus
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder quality(int quality) {
            this.quality = quality;
            return this;
        }

        /**
         * Sets the count range for dropped items.
         *
         * @param count the count range
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder count(@NotNull CountRange count) {
            this.count = count;
            return this;
        }

        /**
         * Sets a fixed count for dropped items.
         *
         * @param count the exact count
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder count(int count) {
            this.count = CountRange.exactly(count);
            return this;
        }

        /**
         * Adds a condition to this entry.
         *
         * @param condition the condition
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder condition(@NotNull LootCondition condition) {
            conditions.add(condition);
            return this;
        }

        /**
         * Adds a function to apply to generated items.
         *
         * @param function the function
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder function(@NotNull LootFunction function) {
            functions.add(function);
            return this;
        }

        /**
         * Builds the loot entry.
         *
         * @return the constructed LootEntry
         * @since 1.0.0
         */
        @NotNull
        public LootEntry build() {
            return new LootEntryImpl(type, itemType, item, tag, tableKey,
                    weight, quality, count, List.copyOf(conditions), List.copyOf(functions));
        }
    }
}

/**
 * Default implementation of LootEntry.
 */
record LootEntryImpl(
        LootEntryType type,
        String itemType,
        UnifiedItemStack item,
        String tag,
        String tableKey,
        int weight,
        int quality,
        CountRange count,
        List<LootCondition> conditions,
        List<LootFunction> functions
) implements LootEntry {

    @Override
    @NotNull
    public LootEntryType getType() {
        return type;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public int getQuality() {
        return quality;
    }

    @Override
    @NotNull
    public List<LootCondition> getConditions() {
        return conditions;
    }

    @Override
    @NotNull
    public List<LootFunction> getFunctions() {
        return functions;
    }
}
