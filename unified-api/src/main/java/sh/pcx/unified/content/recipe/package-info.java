/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Custom recipe system for the UnifiedPlugin framework.
 *
 * <p>This package provides a complete API for creating custom crafting,
 * smelting, smithing, and stonecutter recipes with permission support
 * and recipe book integration.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.content.recipe.RecipeService} - Main service interface</li>
 *   <li>{@link sh.pcx.unified.content.recipe.CustomRecipe} - Recipe representation</li>
 *   <li>{@link sh.pcx.unified.content.recipe.ShapedRecipeBuilder} - Shaped recipe builder</li>
 *   <li>{@link sh.pcx.unified.content.recipe.ShapelessRecipeBuilder} - Shapeless recipe builder</li>
 *   <li>{@link sh.pcx.unified.content.recipe.SmeltingRecipeBuilder} - Furnace recipe builder</li>
 *   <li>{@link sh.pcx.unified.content.recipe.SmithingRecipeBuilder} - Smithing recipe builder</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Shaped recipe
 * recipes.shaped("myplugin:magic_sword")
 *     .result(magicSwordItem)
 *     .shape("DED", " S ", " S ")
 *     .ingredient('D', "minecraft:diamond")
 *     .ingredient('E', "minecraft:emerald")
 *     .ingredient('S', "minecraft:stick")
 *     .permission("myplugin.craft.magic")
 *     .register();
 *
 * // Smelting recipe
 * recipes.smelting("myplugin:custom_ingot")
 *     .input("minecraft:raw_iron")
 *     .result(customIngot)
 *     .experience(2.0f)
 *     .cookTime(100)
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.content.recipe;
