/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.recipe;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for smithing table recipes.
 *
 * @since 1.0.0
 */
public interface SmithingRecipeBuilder {

    /**
     * Sets the template item (for 1.20+).
     *
     * @param itemType the template item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmithingRecipeBuilder template(@NotNull String itemType);

    /**
     * Sets the base item to upgrade.
     *
     * @param itemType the base item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmithingRecipeBuilder base(@NotNull String itemType);

    /**
     * Sets the addition item (material for upgrade).
     *
     * @param itemType the addition item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmithingRecipeBuilder addition(@NotNull String itemType);

    /**
     * Sets the result item.
     *
     * @param result the result item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmithingRecipeBuilder result(@NotNull UnifiedItemStack result);

    /**
     * Sets the recipe group.
     *
     * @param group the group name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    SmithingRecipeBuilder group(@NotNull String group);

    /**
     * Registers the recipe.
     *
     * @return the registered CustomRecipe
     * @since 1.0.0
     */
    @NotNull
    CustomRecipe register();
}
