/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.recipe;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Service for creating and managing custom recipes.
 *
 * <p>RecipeService provides a fluent API for defining shaped, shapeless,
 * furnace, smithing, and stonecutter recipes with support for custom
 * results, permissions, and recipe book integration.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Shaped Recipes</b> - Grid-based crafting patterns</li>
 *   <li><b>Shapeless Recipes</b> - Any arrangement of ingredients</li>
 *   <li><b>Furnace Recipes</b> - Smelting, blasting, smoking</li>
 *   <li><b>Smithing Recipes</b> - Smithing table transforms</li>
 *   <li><b>Stonecutter Recipes</b> - Stonecutter options</li>
 *   <li><b>Permissions</b> - Restrict crafting by permission</li>
 *   <li><b>Recipe Discovery</b> - Control recipe book unlocks</li>
 *   <li><b>Custom Results</b> - Full ItemStack support</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private RecipeService recipes;
 *
 * // Shaped recipe
 * recipes.shaped("myplugin:magic_sword")
 *     .result(ItemBuilder.of("minecraft:diamond_sword")
 *         .name(Component.text("Magic Sword"))
 *         .enchant("minecraft:sharpness", 10)
 *         .build())
 *     .shape("DED", " S ", " S ")
 *     .ingredient('D', "minecraft:diamond")
 *     .ingredient('E', "minecraft:emerald")
 *     .ingredient('S', "minecraft:stick")
 *     .group("magic_weapons")
 *     .permission("myplugin.craft.magic")
 *     .register();
 *
 * // Shapeless recipe
 * recipes.shapeless("myplugin:mega_apple")
 *     .result(ItemBuilder.of("minecraft:enchanted_golden_apple").build())
 *     .ingredients("minecraft:golden_apple", "minecraft:nether_star")
 *     .register();
 *
 * // Furnace recipe
 * recipes.smelting("myplugin:custom_ingot")
 *     .input("minecraft:raw_iron")
 *     .result(customIngotItem)
 *     .experience(2.0f)
 *     .cookTime(100)
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ShapedRecipeBuilder
 * @see ShapelessRecipeBuilder
 */
public interface RecipeService extends Service {

    /**
     * Creates a new shaped recipe builder.
     *
     * @param key the recipe key (e.g., "myplugin:magic_sword")
     * @return a new ShapedRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    ShapedRecipeBuilder shaped(@NotNull String key);

    /**
     * Creates a new shapeless recipe builder.
     *
     * @param key the recipe key
     * @return a new ShapelessRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    ShapelessRecipeBuilder shapeless(@NotNull String key);

    /**
     * Creates a new smelting (furnace) recipe builder.
     *
     * @param key the recipe key
     * @return a new SmeltingRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder smelting(@NotNull String key);

    /**
     * Creates a new blasting (blast furnace) recipe builder.
     *
     * @param key the recipe key
     * @return a new SmeltingRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder blasting(@NotNull String key);

    /**
     * Creates a new smoking (smoker) recipe builder.
     *
     * @param key the recipe key
     * @return a new SmeltingRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder smoking(@NotNull String key);

    /**
     * Creates a new campfire cooking recipe builder.
     *
     * @param key the recipe key
     * @return a new SmeltingRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    SmeltingRecipeBuilder campfire(@NotNull String key);

    /**
     * Creates a new smithing transform recipe builder.
     *
     * @param key the recipe key
     * @return a new SmithingRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    SmithingRecipeBuilder smithing(@NotNull String key);

    /**
     * Creates a new stonecutter recipe builder.
     *
     * @param key the recipe key
     * @return a new StonecutterRecipeBuilder
     * @since 1.0.0
     */
    @NotNull
    StonecutterRecipeBuilder stonecutter(@NotNull String key);

    /**
     * Gets a registered custom recipe by key.
     *
     * @param key the recipe key
     * @return an Optional containing the recipe if found
     * @since 1.0.0
     */
    @NotNull
    Optional<CustomRecipe> get(@NotNull String key);

    /**
     * Returns all registered custom recipes.
     *
     * @return an unmodifiable collection of recipes
     * @since 1.0.0
     */
    @NotNull
    Collection<CustomRecipe> getAll();

    /**
     * Unregisters a custom recipe.
     *
     * @param key the recipe key
     * @return true if the recipe was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull String key);

    /**
     * Discovers a recipe for a player (adds to recipe book).
     *
     * @param player the player
     * @param key    the recipe key
     * @return true if the recipe was newly discovered
     * @since 1.0.0
     */
    boolean discoverRecipe(@NotNull UnifiedPlayer player, @NotNull String key);

    /**
     * Undiscovers a recipe for a player (removes from recipe book).
     *
     * @param player the player
     * @param key    the recipe key
     * @return true if the recipe was undiscovered
     * @since 1.0.0
     */
    boolean undiscoverRecipe(@NotNull UnifiedPlayer player, @NotNull String key);

    /**
     * Checks if a player has discovered a recipe.
     *
     * @param player the player
     * @param key    the recipe key
     * @return true if discovered
     * @since 1.0.0
     */
    boolean hasDiscovered(@NotNull UnifiedPlayer player, @NotNull String key);

    /**
     * Discovers all custom recipes for a player.
     *
     * @param player the player
     * @return the number of recipes discovered
     * @since 1.0.0
     */
    int discoverAll(@NotNull UnifiedPlayer player);
}
