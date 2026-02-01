/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.recipe;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a registered custom recipe.
 *
 * @since 1.0.0
 */
public interface CustomRecipe {

    /**
     * Returns the recipe key.
     *
     * @return the namespaced key
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Returns the recipe type.
     *
     * @return the recipe type
     * @since 1.0.0
     */
    @NotNull
    RecipeType getType();

    /**
     * Returns the result item.
     *
     * @return the result item
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getResult();

    /**
     * Returns the recipe group.
     *
     * @return an Optional containing the group name
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getGroup();

    /**
     * Returns the required permission.
     *
     * @return an Optional containing the permission
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getPermission();
}
