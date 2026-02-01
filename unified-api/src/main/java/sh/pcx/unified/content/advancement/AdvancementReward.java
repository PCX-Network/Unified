/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents the rewards granted when an advancement is completed.
 *
 * <p>AdvancementReward can include experience points, items, commands to
 * execute, recipes to unlock, and custom reward handlers.
 *
 * <h2>Reward Types</h2>
 * <ul>
 *   <li><b>Experience</b> - XP points granted to the player</li>
 *   <li><b>Items</b> - Items added to inventory</li>
 *   <li><b>Commands</b> - Console commands executed (%player% placeholder)</li>
 *   <li><b>Recipes</b> - Recipes unlocked in recipe book</li>
 *   <li><b>Custom</b> - Custom code executed via consumer</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * AdvancementReward reward = AdvancementReward.builder()
 *     .experience(100)
 *     .item(ItemBuilder.of("minecraft:diamond").amount(5).build())
 *     .item(ItemBuilder.of("minecraft:emerald").amount(10).build())
 *     .command("title %player% title {\"text\":\"Achievement!\",\"color\":\"gold\"}")
 *     .recipe("minecraft:diamond_pickaxe")
 *     .custom(player -> {
 *         // Grant special permission or title
 *         grantTitle(player, "Adventurer");
 *     })
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomAdvancement
 * @see AdvancementBuilder
 */
public final class AdvancementReward {

    private final int experience;
    private final List<UnifiedItemStack> items;
    private final List<String> commands;
    private final List<String> recipes;
    private final List<Consumer<UnifiedPlayer>> customRewards;

    private AdvancementReward(Builder builder) {
        this.experience = builder.experience;
        this.items = List.copyOf(builder.items);
        this.commands = List.copyOf(builder.commands);
        this.recipes = List.copyOf(builder.recipes);
        this.customRewards = List.copyOf(builder.customRewards);
    }

    /**
     * Creates a new reward builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple experience reward.
     *
     * @param experience the experience amount
     * @return an AdvancementReward
     * @since 1.0.0
     */
    @NotNull
    public static AdvancementReward experience(int experience) {
        return builder().experience(experience).build();
    }

    /**
     * Creates a simple item reward.
     *
     * @param item the item to give
     * @return an AdvancementReward
     * @since 1.0.0
     */
    @NotNull
    public static AdvancementReward item(@NotNull UnifiedItemStack item) {
        return builder().item(item).build();
    }

    /**
     * Returns the experience reward amount.
     *
     * @return the experience points
     * @since 1.0.0
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Returns the item rewards.
     *
     * @return an unmodifiable list of items
     * @since 1.0.0
     */
    @NotNull
    public List<UnifiedItemStack> getItems() {
        return items;
    }

    /**
     * Returns the commands to execute.
     *
     * @return an unmodifiable list of commands
     * @since 1.0.0
     */
    @NotNull
    public List<String> getCommands() {
        return commands;
    }

    /**
     * Returns the recipes to unlock.
     *
     * @return an unmodifiable list of recipe keys
     * @since 1.0.0
     */
    @NotNull
    public List<String> getRecipes() {
        return recipes;
    }

    /**
     * Returns the custom reward handlers.
     *
     * @return an unmodifiable list of custom rewards
     * @since 1.0.0
     */
    @NotNull
    public List<Consumer<UnifiedPlayer>> getCustomRewards() {
        return customRewards;
    }

    /**
     * Checks if this reward has any content.
     *
     * @return true if any reward is configured
     * @since 1.0.0
     */
    public boolean hasRewards() {
        return experience > 0 || !items.isEmpty() || !commands.isEmpty() ||
               !recipes.isEmpty() || !customRewards.isEmpty();
    }

    /**
     * Grants this reward to a player.
     *
     * @param player the player to reward
     * @since 1.0.0
     */
    public void grant(@NotNull UnifiedPlayer player) {
        // Experience
        if (experience > 0) {
            player.giveExp(experience);
        }

        // Items
        for (UnifiedItemStack item : items) {
            player.giveItem(item);
        }

        // Commands would be executed by the platform implementation

        // Custom rewards
        for (Consumer<UnifiedPlayer> reward : customRewards) {
            reward.accept(player);
        }
    }

    /**
     * Fluent builder for AdvancementReward.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private int experience = 0;
        private final List<UnifiedItemStack> items = new ArrayList<>();
        private final List<String> commands = new ArrayList<>();
        private final List<String> recipes = new ArrayList<>();
        private final List<Consumer<UnifiedPlayer>> customRewards = new ArrayList<>();

        /**
         * Sets the experience reward.
         *
         * @param experience the experience points
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder experience(int experience) {
            this.experience = experience;
            return this;
        }

        /**
         * Adds an item reward.
         *
         * @param item the item to give
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder item(@NotNull UnifiedItemStack item) {
            items.add(item);
            return this;
        }

        /**
         * Adds multiple item rewards.
         *
         * @param items the items to give
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder items(@NotNull UnifiedItemStack... items) {
            Collections.addAll(this.items, items);
            return this;
        }

        /**
         * Adds a command to execute.
         *
         * <p>Use %player% as a placeholder for the player name.
         *
         * @param command the command to execute
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder command(@NotNull String command) {
            commands.add(command);
            return this;
        }

        /**
         * Adds multiple commands to execute.
         *
         * @param commands the commands to execute
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder commands(@NotNull String... commands) {
            Collections.addAll(this.commands, commands);
            return this;
        }

        /**
         * Adds a recipe to unlock.
         *
         * @param recipeKey the recipe key
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder recipe(@NotNull String recipeKey) {
            recipes.add(recipeKey);
            return this;
        }

        /**
         * Adds multiple recipes to unlock.
         *
         * @param recipes the recipe keys
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder recipes(@NotNull String... recipes) {
            Collections.addAll(this.recipes, recipes);
            return this;
        }

        /**
         * Adds a custom reward handler.
         *
         * @param reward the custom reward consumer
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder custom(@NotNull Consumer<UnifiedPlayer> reward) {
            customRewards.add(reward);
            return this;
        }

        /**
         * Builds the advancement reward.
         *
         * @return the constructed AdvancementReward
         * @since 1.0.0
         */
        @NotNull
        public AdvancementReward build() {
            return new AdvancementReward(this);
        }
    }
}
