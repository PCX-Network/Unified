/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.recipe;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for shapeless crafting recipes.
 *
 * @since 1.0.0
 */
public interface ShapelessRecipeBuilder {

    /**
     * Sets the recipe result.
     *
     * @param result the result item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder result(@NotNull UnifiedItemStack result);

    /**
     * Adds an ingredient by item type.
     *
     * @param itemType the item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder ingredient(@NotNull String itemType);

    /**
     * Adds an ingredient item.
     *
     * @param item the ingredient item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder ingredient(@NotNull UnifiedItemStack item);

    /**
     * Adds multiple ingredients by item type.
     *
     * @param itemTypes the item type IDs
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder ingredients(@NotNull String... itemTypes);

    /**
     * Adds an ingredient by item tag.
     *
     * @param tag the item tag
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder ingredientTag(@NotNull String tag);

    /**
     * Sets the recipe group.
     *
     * @param group the group name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder group(@NotNull String group);

    /**
     * Sets the required permission.
     *
     * @param permission the permission node
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder permission(@NotNull String permission);

    /**
     * Registers the recipe.
     *
     * @return the registered CustomRecipe
     * @since 1.0.0
     */
    @NotNull
    CustomRecipe register();
}
