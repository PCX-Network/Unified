/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.recipe;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for smelting-type recipes (furnace, blast furnace, smoker, campfire).
 *
 * @since 1.0.0
 */
public interface SmeltingRecipeBuilder {

    /**
     * Sets the input item type.
     *
     * @param itemType the input item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder input(@NotNull String itemType);

    /**
     * Sets the input item.
     *
     * @param item the input item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder input(@NotNull UnifiedItemStack item);

    /**
     * Sets the input by item tag.
     *
     * @param tag the item tag
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder inputTag(@NotNull String tag);

    /**
     * Sets the result item.
     *
     * @param result the result item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder result(@NotNull UnifiedItemStack result);

    /**
     * Sets the result by item type.
     *
     * @param itemType the result item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder result(@NotNull String itemType);

    /**
     * Sets the experience reward.
     *
     * @param experience the experience amount
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder experience(float experience);

    /**
     * Sets the cooking time in ticks.
     *
     * @param ticks the cooking time
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder cookTime(int ticks);

    /**
     * Sets the recipe group.
     *
     * @param group the group name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder group(@NotNull String group);

    /**
     * Registers the recipe.
     *
     * @return the registered CustomRecipe
     * @since 1.0.0
     */
    @NotNull
    CustomRecipe register();
}
