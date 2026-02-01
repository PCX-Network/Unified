/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.impl;

import sh.pcx.unified.content.recipe.*;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link RecipeService}.
 *
 * <p>This implementation manages custom recipes with support for:
 * <ul>
 *   <li>Shaped and shapeless crafting recipes</li>
 *   <li>Furnace, blast furnace, smoker, and campfire smelting</li>
 *   <li>Smithing table recipes</li>
 *   <li>Stonecutter recipes</li>
 *   <li>Recipe permissions and discovery</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class RecipeServiceImpl implements RecipeService {

    private final Map<String, CustomRecipeImpl> recipes = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> discoveredRecipes = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public ShapedRecipeBuilder shaped(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new ShapedRecipeBuilderImpl(key, this);
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder shapeless(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new ShapelessRecipeBuilderImpl(key, this);
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder smelting(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new SmeltingRecipeBuilderImpl(key, RecipeType.SMELTING, this);
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder blasting(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new SmeltingRecipeBuilderImpl(key, RecipeType.BLASTING, this);
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder smoking(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new SmeltingRecipeBuilderImpl(key, RecipeType.SMOKING, this);
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder campfire(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new SmeltingRecipeBuilderImpl(key, RecipeType.CAMPFIRE, this);
    }

    @Override
    @NotNull
    public SmithingRecipeBuilder smithing(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new SmithingRecipeBuilderImpl(key, this);
    }

    @Override
    @NotNull
    public StonecutterRecipeBuilder stonecutter(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new StonecutterRecipeBuilderImpl(key, this);
    }

    @Override
    @NotNull
    public Optional<CustomRecipe> get(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(recipes.get(key));
    }

    @Override
    @NotNull
    public Collection<CustomRecipe> getAll() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    @Override
    public boolean unregister(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return recipes.remove(key) != null;
    }

    @Override
    public boolean discoverRecipe(@NotNull UnifiedPlayer player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        if (!recipes.containsKey(key)) {
            return false;
        }

        Set<String> discovered = getDiscoveredRecipes(player.getUniqueId());
        return discovered.add(key);
    }

    @Override
    public boolean undiscoverRecipe(@NotNull UnifiedPlayer player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        Set<String> discovered = getDiscoveredRecipes(player.getUniqueId());
        return discovered.remove(key);
    }

    @Override
    public boolean hasDiscovered(@NotNull UnifiedPlayer player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        Set<String> discovered = getDiscoveredRecipes(player.getUniqueId());
        return discovered.contains(key);
    }

    @Override
    public int discoverAll(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");

        Set<String> discovered = getDiscoveredRecipes(player.getUniqueId());
        int count = 0;

        for (String key : recipes.keySet()) {
            if (discovered.add(key)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Registers a recipe.
     */
    void registerRecipe(CustomRecipeImpl recipe) {
        recipes.put(recipe.getKey(), recipe);
    }

    /**
     * Checks if a player can craft a recipe.
     */
    boolean canCraft(UnifiedPlayer player, String key) {
        CustomRecipeImpl recipe = recipes.get(key);
        if (recipe == null) {
            return false;
        }

        return recipe.getPermission()
                .map(player::hasPermission)
                .orElse(true);
    }

    private Set<String> getDiscoveredRecipes(UUID playerId) {
        return discoveredRecipes.computeIfAbsent(playerId, _ -> ConcurrentHashMap.newKeySet());
    }

    private void validateKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        if (!key.contains(":")) {
            throw new IllegalArgumentException("Key must be namespaced (e.g., 'myplugin:magic_sword')");
        }
    }
}

/**
 * Base implementation for custom recipes.
 */
abstract class CustomRecipeImpl implements CustomRecipe {

    protected final String key;
    protected final RecipeType type;
    protected final UnifiedItemStack result;
    protected final String group;
    protected final String permission;

    CustomRecipeImpl(String key, RecipeType type, UnifiedItemStack result, String group, String permission) {
        this.key = key;
        this.type = type;
        this.result = result;
        this.group = group;
        this.permission = permission;
    }

    @Override
    @NotNull
    public String getKey() {
        return key;
    }

    @Override
    @NotNull
    public RecipeType getType() {
        return type;
    }

    @Override
    @NotNull
    public UnifiedItemStack getResult() {
        return result;
    }

    @Override
    @NotNull
    public Optional<String> getGroup() {
        return Optional.ofNullable(group);
    }

    @Override
    @NotNull
    public Optional<String> getPermission() {
        return Optional.ofNullable(permission);
    }
}

/**
 * Shaped recipe implementation.
 */
class ShapedRecipeImpl extends CustomRecipeImpl {

    private final String[] shape;
    private final Map<Character, RecipeIngredient> ingredients;

    ShapedRecipeImpl(String key, UnifiedItemStack result, String group, String permission,
                     String[] shape, Map<Character, RecipeIngredient> ingredients) {
        super(key, RecipeType.SHAPED, result, group, permission);
        this.shape = shape;
        this.ingredients = ingredients;
    }

    public String[] getShape() {
        return shape;
    }

    public Map<Character, RecipeIngredient> getIngredients() {
        return ingredients;
    }
}

/**
 * Shapeless recipe implementation.
 */
class ShapelessRecipeImpl extends CustomRecipeImpl {

    private final List<RecipeIngredient> ingredients;

    ShapelessRecipeImpl(String key, UnifiedItemStack result, String group, String permission,
                        List<RecipeIngredient> ingredients) {
        super(key, RecipeType.SHAPELESS, result, group, permission);
        this.ingredients = ingredients;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }
}

/**
 * Smelting recipe implementation.
 */
class SmeltingRecipeImpl extends CustomRecipeImpl {

    private final RecipeIngredient input;
    private final float experience;
    private final int cookTime;

    SmeltingRecipeImpl(String key, RecipeType type, UnifiedItemStack result, String group,
                       RecipeIngredient input, float experience, int cookTime) {
        super(key, type, result, group, null);
        this.input = input;
        this.experience = experience;
        this.cookTime = cookTime;
    }

    public RecipeIngredient getInput() {
        return input;
    }

    public float getExperience() {
        return experience;
    }

    public int getCookTime() {
        return cookTime;
    }
}

/**
 * Smithing recipe implementation.
 */
class SmithingRecipeImpl extends CustomRecipeImpl {

    private final String template;
    private final String base;
    private final String addition;

    SmithingRecipeImpl(String key, UnifiedItemStack result, String group,
                       String template, String base, String addition) {
        super(key, RecipeType.SMITHING, result, group, null);
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    public String getTemplate() {
        return template;
    }

    public String getBase() {
        return base;
    }

    public String getAddition() {
        return addition;
    }
}

/**
 * Stonecutter recipe implementation.
 */
class StonecutterRecipeImpl extends CustomRecipeImpl {

    private final RecipeIngredient input;

    StonecutterRecipeImpl(String key, UnifiedItemStack result, String group, RecipeIngredient input) {
        super(key, RecipeType.STONECUTTER, result, group, null);
        this.input = input;
    }

    public RecipeIngredient getInput() {
        return input;
    }
}

/**
 * Represents a recipe ingredient.
 */
record RecipeIngredient(
        String itemType,
        UnifiedItemStack item,
        String tag
) {
    static RecipeIngredient ofType(String itemType) {
        return new RecipeIngredient(itemType, null, null);
    }

    static RecipeIngredient ofItem(UnifiedItemStack item) {
        return new RecipeIngredient(null, item, null);
    }

    static RecipeIngredient ofTag(String tag) {
        return new RecipeIngredient(null, null, tag);
    }
}

/**
 * Implementation of {@link ShapedRecipeBuilder}.
 */
class ShapedRecipeBuilderImpl implements ShapedRecipeBuilder {

    private final String key;
    private final RecipeServiceImpl service;

    private UnifiedItemStack result;
    private String[] shape;
    private final Map<Character, RecipeIngredient> ingredients = new HashMap<>();
    private String group;
    private String permission;

    ShapedRecipeBuilderImpl(String key, RecipeServiceImpl service) {
        this.key = key;
        this.service = service;
    }

    @Override
    @NotNull
    public ShapedRecipeBuilder result(@NotNull UnifiedItemStack result) {
        this.result = Objects.requireNonNull(result);
        return this;
    }

    @Override
    @NotNull
    public ShapedRecipeBuilder shape(@NotNull String... rows) {
        this.shape = rows;
        return this;
    }

    @Override
    @NotNull
    public ShapedRecipeBuilder ingredient(char character, @NotNull String itemType) {
        ingredients.put(character, RecipeIngredient.ofType(itemType));
        return this;
    }

    @Override
    @NotNull
    public ShapedRecipeBuilder ingredient(char character, @NotNull UnifiedItemStack item) {
        ingredients.put(character, RecipeIngredient.ofItem(item));
        return this;
    }

    @Override
    @NotNull
    public ShapedRecipeBuilder ingredientTag(char character, @NotNull String tag) {
        ingredients.put(character, RecipeIngredient.ofTag(tag));
        return this;
    }

    @Override
    @NotNull
    public ShapedRecipeBuilder group(@NotNull String group) {
        this.group = group;
        return this;
    }

    @Override
    @NotNull
    public ShapedRecipeBuilder permission(@NotNull String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    @NotNull
    public CustomRecipe register() {
        if (result == null) {
            throw new IllegalStateException("Result is required");
        }
        if (shape == null || shape.length == 0) {
            throw new IllegalStateException("Shape is required");
        }

        ShapedRecipeImpl recipe = new ShapedRecipeImpl(
                key, result, group, permission, shape, Map.copyOf(ingredients)
        );

        service.registerRecipe(recipe);
        return recipe;
    }
}

/**
 * Implementation of {@link ShapelessRecipeBuilder}.
 */
class ShapelessRecipeBuilderImpl implements ShapelessRecipeBuilder {

    private final String key;
    private final RecipeServiceImpl service;

    private UnifiedItemStack result;
    private final List<RecipeIngredient> ingredients = new ArrayList<>();
    private String group;
    private String permission;

    ShapelessRecipeBuilderImpl(String key, RecipeServiceImpl service) {
        this.key = key;
        this.service = service;
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder result(@NotNull UnifiedItemStack result) {
        this.result = Objects.requireNonNull(result);
        return this;
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder ingredient(@NotNull String itemType) {
        ingredients.add(RecipeIngredient.ofType(itemType));
        return this;
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder ingredient(@NotNull UnifiedItemStack item) {
        ingredients.add(RecipeIngredient.ofItem(item));
        return this;
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder ingredients(@NotNull String... itemTypes) {
        for (String itemType : itemTypes) {
            ingredients.add(RecipeIngredient.ofType(itemType));
        }
        return this;
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder ingredientTag(@NotNull String tag) {
        ingredients.add(RecipeIngredient.ofTag(tag));
        return this;
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder group(@NotNull String group) {
        this.group = group;
        return this;
    }

    @Override
    @NotNull
    public ShapelessRecipeBuilder permission(@NotNull String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    @NotNull
    public CustomRecipe register() {
        if (result == null) {
            throw new IllegalStateException("Result is required");
        }
        if (ingredients.isEmpty()) {
            throw new IllegalStateException("At least one ingredient is required");
        }

        ShapelessRecipeImpl recipe = new ShapelessRecipeImpl(
                key, result, group, permission, List.copyOf(ingredients)
        );

        service.registerRecipe(recipe);
        return recipe;
    }
}

/**
 * Implementation of {@link SmeltingRecipeBuilder}.
 */
class SmeltingRecipeBuilderImpl implements SmeltingRecipeBuilder {

    private final String key;
    private final RecipeType type;
    private final RecipeServiceImpl service;

    private RecipeIngredient input;
    private UnifiedItemStack result;
    private float experience = 0.1f;
    private int cookTime = 200;
    private String group;

    SmeltingRecipeBuilderImpl(String key, RecipeType type, RecipeServiceImpl service) {
        this.key = key;
        this.type = type;
        this.service = service;

        // Default cook times
        switch (type) {
            case BLASTING, SMOKING -> this.cookTime = 100;
            case CAMPFIRE -> this.cookTime = 600;
        }
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder input(@NotNull String itemType) {
        this.input = RecipeIngredient.ofType(itemType);
        return this;
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder input(@NotNull UnifiedItemStack item) {
        this.input = RecipeIngredient.ofItem(item);
        return this;
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder inputTag(@NotNull String tag) {
        this.input = RecipeIngredient.ofTag(tag);
        return this;
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder result(@NotNull UnifiedItemStack result) {
        this.result = Objects.requireNonNull(result);
        return this;
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder result(@NotNull String itemType) {
        // Would create item from type - platform specific
        return this;
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder experience(float experience) {
        this.experience = experience;
        return this;
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder cookTime(int ticks) {
        this.cookTime = ticks;
        return this;
    }

    @Override
    @NotNull
    public SmeltingRecipeBuilder group(@NotNull String group) {
        this.group = group;
        return this;
    }

    @Override
    @NotNull
    public CustomRecipe register() {
        if (input == null) {
            throw new IllegalStateException("Input is required");
        }
        if (result == null) {
            throw new IllegalStateException("Result is required");
        }

        SmeltingRecipeImpl recipe = new SmeltingRecipeImpl(
                key, type, result, group, input, experience, cookTime
        );

        service.registerRecipe(recipe);
        return recipe;
    }
}

/**
 * Implementation of {@link SmithingRecipeBuilder}.
 */
class SmithingRecipeBuilderImpl implements SmithingRecipeBuilder {

    private final String key;
    private final RecipeServiceImpl service;

    private String template;
    private String base;
    private String addition;
    private UnifiedItemStack result;
    private String group;

    SmithingRecipeBuilderImpl(String key, RecipeServiceImpl service) {
        this.key = key;
        this.service = service;
    }

    @Override
    @NotNull
    public SmithingRecipeBuilder template(@NotNull String itemType) {
        this.template = itemType;
        return this;
    }

    @Override
    @NotNull
    public SmithingRecipeBuilder base(@NotNull String itemType) {
        this.base = itemType;
        return this;
    }

    @Override
    @NotNull
    public SmithingRecipeBuilder addition(@NotNull String itemType) {
        this.addition = itemType;
        return this;
    }

    @Override
    @NotNull
    public SmithingRecipeBuilder result(@NotNull UnifiedItemStack result) {
        this.result = Objects.requireNonNull(result);
        return this;
    }

    @Override
    @NotNull
    public SmithingRecipeBuilder group(@NotNull String group) {
        this.group = group;
        return this;
    }

    @Override
    @NotNull
    public CustomRecipe register() {
        if (base == null) {
            throw new IllegalStateException("Base is required");
        }
        if (addition == null) {
            throw new IllegalStateException("Addition is required");
        }
        if (result == null) {
            throw new IllegalStateException("Result is required");
        }

        SmithingRecipeImpl recipe = new SmithingRecipeImpl(
                key, result, group, template, base, addition
        );

        service.registerRecipe(recipe);
        return recipe;
    }
}

/**
 * Implementation of {@link StonecutterRecipeBuilder}.
 */
class StonecutterRecipeBuilderImpl implements StonecutterRecipeBuilder {

    private final String key;
    private final RecipeServiceImpl service;

    private RecipeIngredient input;
    private UnifiedItemStack result;
    private String group;

    StonecutterRecipeBuilderImpl(String key, RecipeServiceImpl service) {
        this.key = key;
        this.service = service;
    }

    @Override
    @NotNull
    public StonecutterRecipeBuilder input(@NotNull String itemType) {
        this.input = RecipeIngredient.ofType(itemType);
        return this;
    }

    @Override
    @NotNull
    public StonecutterRecipeBuilder inputTag(@NotNull String tag) {
        this.input = RecipeIngredient.ofTag(tag);
        return this;
    }

    @Override
    @NotNull
    public StonecutterRecipeBuilder result(@NotNull UnifiedItemStack result) {
        this.result = Objects.requireNonNull(result);
        return this;
    }

    @Override
    @NotNull
    public StonecutterRecipeBuilder result(@NotNull String itemType, int count) {
        // Would create item from type - platform specific
        return this;
    }

    @Override
    @NotNull
    public StonecutterRecipeBuilder group(@NotNull String group) {
        this.group = group;
        return this;
    }

    @Override
    @NotNull
    public CustomRecipe register() {
        if (input == null) {
            throw new IllegalStateException("Input is required");
        }
        if (result == null) {
            throw new IllegalStateException("Result is required");
        }

        StonecutterRecipeImpl recipe = new StonecutterRecipeImpl(
                key, result, group, input
        );

        service.registerRecipe(recipe);
        return recipe;
    }
}
