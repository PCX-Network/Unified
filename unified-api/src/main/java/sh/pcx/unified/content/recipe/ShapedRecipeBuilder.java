/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.recipe;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for shaped crafting recipes.
 *
 * @since 1.0.0
 */
public interface ShapedRecipeBuilder {

    /**
     * Sets the recipe result.
     *
     * @param result the result item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder result(@NotNull UnifiedItemStack result);

    /**
     * Sets the crafting pattern.
     *
     * <p>Each string represents a row, with characters mapping to ingredients.
     * Space represents an empty slot.
     *
     * @param rows the pattern rows (1-3 rows, 1-3 characters each)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder shape(@NotNull String... rows);

    /**
     * Maps a character to an ingredient item type.
     *
     * @param character the pattern character
     * @param itemType  the item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder ingredient(char character, @NotNull String itemType);

    /**
     * Maps a character to an ingredient item.
     *
     * @param character the pattern character
     * @param item      the ingredient item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder ingredient(char character, @NotNull UnifiedItemStack item);

    /**
     * Maps a character to an item tag.
     *
     * @param character the pattern character
     * @param tag       the item tag
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder ingredientTag(char character, @NotNull String tag);

    /**
     * Sets the recipe group for the recipe book.
     *
     * @param group the group name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder group(@NotNull String group);

    /**
     * Sets the permission required to craft.
     *
     * @param permission the permission node
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder permission(@NotNull String permission);

    /**
     * Registers the recipe.
     *
     * @return the registered CustomRecipe
     * @since 1.0.0
     */
    @NotNull
    CustomRecipe register();
}
