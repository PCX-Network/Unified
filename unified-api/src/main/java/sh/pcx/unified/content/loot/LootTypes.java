/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Supporting types for the loot table system.
 *
 * <p>This class contains various range types, formulas, and predicates
 * used throughout the loot table API.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class LootTypes {
    private LootTypes() {}

    /**
     * Represents a range for roll counts in a loot pool.
     *
     * @since 1.0.0
     */
    public record RollRange(int min, int max) {

        /**
         * Creates a fixed roll count.
         *
         * @param count the exact count
         * @return a RollRange
         */
        @NotNull
        public static RollRange exactly(int count) {
            return new RollRange(count, count);
        }

        /**
         * Creates a range between min and max.
         *
         * @param min the minimum (inclusive)
         * @param max the maximum (inclusive)
         * @return a RollRange
         */
        @NotNull
        public static RollRange between(int min, int max) {
            return new RollRange(min, max);
        }

        /**
         * Rolls a value within this range.
         *
         * @param random the random instance
         * @return a value between min and max
         */
        public int roll(@NotNull Random random) {
            if (min == max) return min;
            return random.nextInt(max - min + 1) + min;
        }
    }

    /**
     * Represents a range for item counts.
     *
     * @since 1.0.0
     */
    public record CountRange(int min, int max) {

        /**
         * Creates a fixed count.
         *
         * @param count the exact count
         * @return a CountRange
         */
        @NotNull
        public static CountRange exactly(int count) {
            return new CountRange(count, count);
        }

        /**
         * Creates a range between min and max.
         *
         * @param min the minimum (inclusive)
         * @param max the maximum (inclusive)
         * @return a CountRange
         */
        @NotNull
        public static CountRange between(int min, int max) {
            return new CountRange(min, max);
        }

        /**
         * Rolls a value within this range.
         *
         * @param random the random instance
         * @return a value between min and max
         */
        public int roll(@NotNull Random random) {
            if (min == max) return min;
            return random.nextInt(max - min + 1) + min;
        }
    }

    /**
     * Represents a range for item damage (durability loss).
     *
     * @since 1.0.0
     */
    public record DamageRange(float min, float max) {

        /**
         * Creates a fixed damage percentage.
         *
         * @param damage the damage (0.0 = full, 1.0 = broken)
         * @return a DamageRange
         */
        @NotNull
        public static DamageRange exactly(float damage) {
            return new DamageRange(damage, damage);
        }

        /**
         * Creates a range between min and max.
         *
         * @param min the minimum damage (inclusive)
         * @param max the maximum damage (inclusive)
         * @return a DamageRange
         */
        @NotNull
        public static DamageRange between(float min, float max) {
            return new DamageRange(min, max);
        }

        /**
         * Rolls a value within this range.
         *
         * @param random the random instance
         * @return a value between min and max
         */
        public float roll(@NotNull Random random) {
            if (min == max) return min;
            return random.nextFloat() * (max - min) + min;
        }
    }

    /**
     * Represents a range for enchanting levels.
     *
     * @since 1.0.0
     */
    public record LevelRange(int min, int max) {

        /**
         * Creates a fixed level.
         *
         * @param level the exact level
         * @return a LevelRange
         */
        @NotNull
        public static LevelRange exactly(int level) {
            return new LevelRange(level, level);
        }

        /**
         * Creates a range between min and max.
         *
         * @param min the minimum level (inclusive)
         * @param max the maximum level (inclusive)
         * @return a LevelRange
         */
        @NotNull
        public static LevelRange between(int min, int max) {
            return new LevelRange(min, max);
        }

        /**
         * Rolls a value within this range.
         *
         * @param random the random instance
         * @return a value between min and max
         */
        public int roll(@NotNull Random random) {
            if (min == max) return min;
            return random.nextInt(max - min + 1) + min;
        }
    }

    /**
     * Represents bonus rolls per luck level.
     *
     * @since 1.0.0
     */
    public record BonusRolls(float multiplier) {

        /**
         * Creates bonus rolls per luck level.
         *
         * @param rollsPerLuck additional rolls per luck point
         * @return a BonusRolls
         */
        @NotNull
        public static BonusRolls perLuck(float rollsPerLuck) {
            return new BonusRolls(rollsPerLuck);
        }

        /**
         * Calculates the bonus rolls for a given luck value.
         *
         * @param luck the luck value
         * @return the number of bonus rolls
         */
        public int calculate(float luck) {
            return (int) (multiplier * luck);
        }
    }

    /**
     * Formula for calculating enchantment bonus.
     *
     * @since 1.0.0
     */
    public enum BonusFormula {
        /**
         * Uniform bonus (used by Fortune on most blocks).
         * Adds 0 to (enchantment level) items.
         */
        UNIFORM {
            @Override
            public int calculate(int level, @NotNull Random random) {
                if (level <= 0) return 0;
                return random.nextInt(level + 1);
            }
        },

        /**
         * Binomial bonus (used by Fortune on wheat, etc.).
         * Rolls (enchantment level) times, each with 1/(level+2) chance.
         */
        BINOMIAL {
            @Override
            public int calculate(int level, @NotNull Random random) {
                if (level <= 0) return 0;
                int bonus = 0;
                float chance = 1.0f / (level + 2);
                for (int i = 0; i < level; i++) {
                    if (random.nextFloat() < chance) {
                        bonus++;
                    }
                }
                return bonus;
            }
        },

        /**
         * Ore drops formula (used by Fortune on ores).
         * Multiplies drops by 1 to (enchantment level + 1).
         */
        ORE_DROPS {
            @Override
            public int calculate(int level, @NotNull Random random) {
                if (level <= 0) return 0;
                int multiplier = random.nextInt(level + 2);
                return Math.max(multiplier - 1, 0);
            }
        };

        /**
         * Calculates the bonus for this formula.
         *
         * @param level  the enchantment level
         * @param random the random instance
         * @return the bonus count
         */
        public abstract int calculate(int level, @NotNull Random random);
    }

    /**
     * Types of loot tables.
     *
     * @since 1.0.0
     */
    public enum LootTableType {
        /** Entity drops (mobs, animals) */
        ENTITY,
        /** Block drops */
        BLOCK,
        /** Chest loot */
        CHEST,
        /** Fishing loot */
        FISHING,
        /** Gift loot (villager hero gifts) */
        GIFT,
        /** Advancement rewards */
        ADVANCEMENT_REWARD,
        /** Archaeology (suspicious blocks) */
        ARCHAEOLOGY,
        /** Generic/custom */
        GENERIC
    }

    /**
     * Predicate for matching items (used in conditions).
     *
     * @since 1.0.0
     */
    public record ItemPredicate(
            String type,
            String tag,
            String enchantment,
            int minCount,
            int maxCount
    ) {

        /**
         * Creates a predicate matching an item type.
         *
         * @param type the item type ID
         * @return an ItemPredicate
         */
        @NotNull
        public static ItemPredicate of(@NotNull String type) {
            return new ItemPredicate(type, null, null, -1, -1);
        }

        /**
         * Creates a predicate matching an item tag.
         *
         * @param tag the item tag
         * @return an ItemPredicate
         */
        @NotNull
        public static ItemPredicate tag(@NotNull String tag) {
            return new ItemPredicate(null, tag, null, -1, -1);
        }

        /**
         * Creates a predicate matching an enchantment.
         *
         * @param enchantment the enchantment key
         * @return an ItemPredicate
         */
        @NotNull
        public static ItemPredicate enchantment(@NotNull String enchantment) {
            return new ItemPredicate(null, null, enchantment, -1, -1);
        }

        /**
         * Tests if an item matches this predicate.
         *
         * @param item the item to test
         * @return true if the item matches
         */
        public boolean test(@NotNull sh.pcx.unified.item.UnifiedItemStack item) {
            if (type != null && !item.getType().equalsIgnoreCase(type)) {
                return false;
            }
            if (enchantment != null && item.getEnchantmentLevel(enchantment) <= 0) {
                return false;
            }
            if (minCount >= 0 && item.getAmount() < minCount) {
                return false;
            }
            if (maxCount >= 0 && item.getAmount() > maxCount) {
                return false;
            }
            // Tag checking would require platform implementation
            return true;
        }
    }
}
