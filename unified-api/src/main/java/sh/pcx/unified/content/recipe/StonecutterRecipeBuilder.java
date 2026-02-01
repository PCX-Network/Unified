/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.recipe;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for stonecutter recipes.
 *
 * @since 1.0.0
 */
public interface StonecutterRecipeBuilder {

    /**
     * Sets the input item type.
     *
     * @param itemType the input item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StonecutterRecipeBuilder input(@NotNull String itemType);

    /**
     * Sets the input by item tag.
     *
     * @param tag the item tag
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StonecutterRecipeBuilder inputTag(@NotNull String tag);

    /**
     * Sets the result item.
     *
     * @param result the result item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StonecutterRecipeBuilder result(@NotNull UnifiedItemStack result);

    /**
     * Sets the result by item type and count.
     *
     * @param itemType the result item type ID
     * @param count    the result count
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StonecutterRecipeBuilder result(@NotNull String itemType, int count);

    /**
     * Sets the recipe group.
     *
     * @param group the group name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StonecutterRecipeBuilder group(@NotNull String group);

    /**
     * Registers the recipe.
     *
     * @return the registered CustomRecipe
     * @since 1.0.0
     */
    @NotNull
    CustomRecipe register();
}
