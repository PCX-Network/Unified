/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for rendering data items as displayable inventory items.
 *
 * <p>ItemRenderer transforms domain objects (like players, items, or custom data)
 * into {@link UnifiedItemStack} instances that can be displayed in a GUI inventory.
 * This decouples the data model from its visual representation.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple renderer for player UUIDs
 * ItemRenderer<UUID> playerRenderer = (uuid, slot, context) -> {
 *     Player player = Bukkit.getPlayer(uuid);
 *     if (player == null) return null;
 *
 *     return ItemBuilder.skull()
 *         .skullOwner(uuid)
 *         .name(Component.text(player.getName()))
 *         .lore(
 *             Component.text("Level: " + player.getLevel()),
 *             Component.text("Click to teleport")
 *         )
 *         .build();
 * };
 *
 * // Renderer with context awareness
 * ItemRenderer<ShopItem> shopRenderer = (item, slot, context) -> {
 *     boolean isLastPage = context.isLastPage();
 *     int globalIndex = context.getPageNumber() * context.getItemCount() + slot;
 *
 *     return ItemBuilder.of(item.getMaterial())
 *         .name(Component.text(item.getName()))
 *         .lore(
 *             Component.text("Price: $" + item.getPrice()),
 *             Component.text("Item #" + (globalIndex + 1))
 *         )
 *         .build();
 * };
 *
 * // Conditional rendering
 * ItemRenderer<Quest> questRenderer = (quest, slot, context) -> {
 *     ItemBuilder builder = ItemBuilder.of(quest.getIcon())
 *         .name(Component.text(quest.getName()));
 *
 *     if (quest.isCompleted()) {
 *         builder.glowing(true)
 *                .addLore(Component.text("COMPLETED", NamedTextColor.GREEN));
 *     } else {
 *         builder.addLore(Component.text("Progress: " + quest.getProgress() + "%"));
 *     }
 *
 *     return builder.build();
 * };
 *
 * // Set renderer on GUI
 * paginatedGui.setItemRenderer(playerRenderer);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>ItemRenderer implementations should be thread-safe if they will be
 * used with async item loading. Avoid modifying shared state within
 * the render method.
 *
 * @param <T> the type of item to render
 * @since 1.0.0
 * @author Supatuck
 * @see PaginatedGUI
 * @see AsyncItemLoader
 */
@FunctionalInterface
public interface ItemRenderer<T> {

    /**
     * Renders a data item as a displayable inventory item.
     *
     * <p>This method is called for each item that needs to be displayed
     * on the current page. The implementation should create an appropriate
     * {@link UnifiedItemStack} representation of the data item.
     *
     * @param item    the data item to render
     * @param slot    the inventory slot where this item will be placed
     * @param context the current page context with navigation information
     * @return the rendered item stack, or null to leave the slot empty
     * @since 1.0.0
     */
    @Nullable
    UnifiedItemStack render(@NotNull T item, int slot, @NotNull PageContext<T> context);

    /**
     * Creates a renderer that applies a transformation after this renderer.
     *
     * @param transformer the transformation to apply to rendered items
     * @return a composed renderer
     * @since 1.0.0
     */
    @NotNull
    default ItemRenderer<T> andThen(@NotNull ItemTransformer transformer) {
        return (item, slot, context) -> {
            UnifiedItemStack rendered = render(item, slot, context);
            if (rendered == null) {
                return null;
            }
            return transformer.transform(rendered, slot, context);
        };
    }

    /**
     * Creates a renderer that provides a fallback if this renderer returns null.
     *
     * @param fallback the fallback renderer
     * @return a composed renderer that uses the fallback when necessary
     * @since 1.0.0
     */
    @NotNull
    default ItemRenderer<T> orElse(@NotNull ItemRenderer<T> fallback) {
        return (item, slot, context) -> {
            UnifiedItemStack rendered = render(item, slot, context);
            if (rendered == null) {
                return fallback.render(item, slot, context);
            }
            return rendered;
        };
    }

    /**
     * Creates a renderer that provides a fallback item if this renderer returns null.
     *
     * @param fallbackItem the fallback item stack
     * @return a composed renderer that uses the fallback item when necessary
     * @since 1.0.0
     */
    @NotNull
    default ItemRenderer<T> orElse(@NotNull UnifiedItemStack fallbackItem) {
        return (item, slot, context) -> {
            UnifiedItemStack rendered = render(item, slot, context);
            return rendered != null ? rendered : fallbackItem;
        };
    }

    /**
     * Creates a simple renderer that ignores the slot and context.
     *
     * @param <T>      the item type
     * @param renderer the simple rendering function
     * @return an ItemRenderer that uses the simple function
     * @since 1.0.0
     */
    @NotNull
    static <T> ItemRenderer<T> simple(@NotNull SimpleRenderer<T> renderer) {
        return (item, slot, context) -> renderer.render(item);
    }

    /**
     * Creates a cached renderer that caches rendered items by item identity.
     *
     * <p>Note: The cache is NOT thread-safe. Use with caution in async contexts.
     *
     * @param <T>      the item type
     * @param renderer the underlying renderer
     * @return a caching ItemRenderer
     * @since 1.0.0
     */
    @NotNull
    static <T> ItemRenderer<T> cached(@NotNull ItemRenderer<T> renderer) {
        java.util.Map<T, UnifiedItemStack> cache = new java.util.WeakHashMap<>();
        return (item, slot, context) -> cache.computeIfAbsent(item,
                k -> renderer.render(k, slot, context));
    }

    /**
     * Simplified rendering function that only takes the item.
     *
     * @param <T> the item type
     * @since 1.0.0
     */
    @FunctionalInterface
    interface SimpleRenderer<T> {

        /**
         * Renders an item without slot or context information.
         *
         * @param item the item to render
         * @return the rendered item stack
         * @since 1.0.0
         */
        @Nullable
        UnifiedItemStack render(@NotNull T item);
    }

    /**
     * Transformation function applied to rendered items.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface ItemTransformer {

        /**
         * Transforms a rendered item.
         *
         * @param item    the rendered item to transform
         * @param slot    the slot where the item will be placed
         * @param context the current page context
         * @return the transformed item
         * @since 1.0.0
         */
        @NotNull
        UnifiedItemStack transform(@NotNull UnifiedItemStack item, int slot,
                                   @NotNull PageContext<?> context);
    }
}
