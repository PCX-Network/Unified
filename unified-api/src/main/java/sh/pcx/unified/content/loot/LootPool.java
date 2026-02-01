/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import sh.pcx.unified.content.loot.LootTypes.BonusRolls;
import sh.pcx.unified.content.loot.LootTypes.RollRange;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Represents a pool of entries within a loot table.
 *
 * <p>Each pool is rolled independently to generate items. A pool specifies
 * how many times to roll, the entries to choose from, and conditions that
 * must be met for the pool to be used.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LootPool pool = LootPool.builder()
 *     .name("rare_drops")
 *     .rolls(RollRange.between(1, 3))
 *     .bonusRolls(BonusRolls.perLuck(0.5f))
 *     .condition(LootCondition.killedByPlayer())
 *     .condition(LootCondition.randomChanceWithLooting(0.1f, 0.02f))
 *     .entry(LootEntry.item("minecraft:diamond").weight(10))
 *     .entry(LootEntry.item("minecraft:emerald").weight(20))
 *     .entry(LootEntry.empty().weight(70))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootTable
 * @see LootEntry
 */
public interface LootPool {

    /**
     * Returns the name of this pool.
     *
     * @return an Optional containing the pool name if set
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getName();

    /**
     * Returns the roll range for this pool.
     *
     * @return the roll range
     * @since 1.0.0
     */
    @NotNull
    RollRange getRolls();

    /**
     * Returns the bonus rolls configuration.
     *
     * @return an Optional containing bonus rolls if set
     * @since 1.0.0
     */
    @NotNull
    Optional<BonusRolls> getBonusRolls();

    /**
     * Returns all entries in this pool.
     *
     * @return an unmodifiable list of entries
     * @since 1.0.0
     */
    @NotNull
    List<LootEntry> getEntries();

    /**
     * Returns all conditions for this pool.
     *
     * @return an unmodifiable list of conditions
     * @since 1.0.0
     */
    @NotNull
    List<LootCondition> getConditions();

    /**
     * Creates a new pool builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for LootPool.
     *
     * @since 1.0.0
     */
    final class Builder {
        private String name;
        private RollRange rolls = RollRange.exactly(1);
        private BonusRolls bonusRolls;
        private final java.util.ArrayList<LootEntry> entries = new java.util.ArrayList<>();
        private final java.util.ArrayList<LootCondition> conditions = new java.util.ArrayList<>();

        /**
         * Sets the pool name.
         *
         * @param name the pool name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the number of rolls.
         *
         * @param rolls the exact number of rolls
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder rolls(int rolls) {
            this.rolls = RollRange.exactly(rolls);
            return this;
        }

        /**
         * Sets the roll range.
         *
         * @param rolls the roll range
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder rolls(@NotNull RollRange rolls) {
            this.rolls = rolls;
            return this;
        }

        /**
         * Sets the bonus rolls.
         *
         * @param bonusRolls the bonus rolls configuration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder bonusRolls(@NotNull BonusRolls bonusRolls) {
            this.bonusRolls = bonusRolls;
            return this;
        }

        /**
         * Adds an entry to the pool.
         *
         * @param entry the loot entry
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder entry(@NotNull LootEntry entry) {
            entries.add(entry);
            return this;
        }

        /**
         * Adds multiple entries to the pool.
         *
         * @param entries the loot entries
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder entries(@NotNull LootEntry... entries) {
            this.entries.addAll(List.of(entries));
            return this;
        }

        /**
         * Adds a condition to the pool.
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
         * Adds multiple conditions to the pool.
         *
         * @param conditions the conditions
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder conditions(@NotNull LootCondition... conditions) {
            this.conditions.addAll(List.of(conditions));
            return this;
        }

        /**
         * Builds the loot pool.
         *
         * @return the constructed LootPool
         * @since 1.0.0
         */
        @NotNull
        public LootPool build() {
            return new LootPoolImpl(name, rolls, bonusRolls,
                    List.copyOf(entries), List.copyOf(conditions));
        }
    }
}

/**
 * Default implementation of LootPool.
 */
record LootPoolImpl(
        String name,
        RollRange rolls,
        BonusRolls bonusRolls,
        List<LootEntry> entries,
        List<LootCondition> conditions
) implements LootPool {

    @Override
    @NotNull
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    @NotNull
    public RollRange getRolls() {
        return rolls;
    }

    @Override
    @NotNull
    public Optional<BonusRolls> getBonusRolls() {
        return Optional.ofNullable(bonusRolls);
    }

    @Override
    @NotNull
    public List<LootEntry> getEntries() {
        return entries;
    }

    @Override
    @NotNull
    public List<LootCondition> getConditions() {
        return conditions;
    }
}
