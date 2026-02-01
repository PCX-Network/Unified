/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Abstract base class for creating paginated GUI interfaces.
 *
 * <p>This class provides a foundation for inventory-based GUIs that display
 * paginated content. It handles page navigation, item rendering, filtering,
 * sorting, and empty state display. Subclasses must implement methods to
 * provide the actual inventory management.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class PlayerListGUI extends PaginatedGUI<UUID> {
 *
 *     public PlayerListGUI(UnifiedPlayer viewer) {
 *         super(viewer, "Players", 54);
 *
 *         // Configure content slots (rows 1-4, leaving row 5 for navigation)
 *         setContentSlots(ContentSlots.rows(0, 4, 9)); // 36 slots
 *
 *         // Set up pagination config
 *         setPaginationConfig(PaginationConfig.builder()
 *             .previousButtonSlot(45)
 *             .nextButtonSlot(53)
 *             .pageInfoSlot(49)
 *             .build());
 *
 *         // Set the item renderer
 *         setItemRenderer((uuid, slot, context) -> {
 *             Player player = Bukkit.getPlayer(uuid);
 *             return ItemBuilder.skull()
 *                 .skullOwner(uuid)
 *                 .name(Component.text(player.getName()))
 *                 .build();
 *         });
 *
 *         // Load players
 *         setItems(server.getOnlinePlayers().stream()
 *             .map(Player::getUniqueId)
 *             .toList());
 *     }
 *
 *     protected void handleItemClick(UUID uuid, int slot, ClickType click) {
 *         Player target = Bukkit.getPlayer(uuid);
 *         viewer.teleport(target);
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. All operations should be performed
 * on the main server thread unless otherwise noted.
 *
 * @param <T> the type of items displayed in the GUI
 * @since 1.0.0
 * @author Supatuck
 * @see Pagination
 * @see PaginationConfig
 * @see ContentSlots
 * @see ItemRenderer
 */
public abstract class PaginatedGUI<T> {

    /**
     * The player viewing this GUI.
     */
    protected final UnifiedPlayer viewer;

    /**
     * The title of the GUI.
     */
    protected final String title;

    /**
     * The size of the GUI in slots.
     */
    protected final int size;

    private Pagination<T> pagination;
    private PaginationConfig config;
    private ContentSlots contentSlots;
    private ItemRenderer<T> itemRenderer;
    private EmptyState emptyState;
    private EmptyStateRenderer emptyStateRenderer;
    private AsyncItemLoader<T> asyncLoader;

    private Predicate<T> currentFilter;
    private Comparator<T> currentSorter;
    private SortDirection currentSortDirection;
    private boolean isLoading;

    /**
     * Creates a new PaginatedGUI.
     *
     * @param viewer the player who will view this GUI
     * @param title  the title of the GUI
     * @param size   the size of the GUI in slots (must be a multiple of 9, max 54)
     * @throws NullPointerException     if viewer or title is null
     * @throws IllegalArgumentException if size is invalid
     * @since 1.0.0
     */
    protected PaginatedGUI(@NotNull UnifiedPlayer viewer, @NotNull String title, int size) {
        Objects.requireNonNull(viewer, "viewer cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        validateSize(size);

        this.viewer = viewer;
        this.title = title;
        this.size = size;
        this.pagination = new Pagination<>(java.util.Collections.emptyList());
        this.config = PaginationConfig.defaults();
        this.contentSlots = ContentSlots.full(size);
        this.currentSortDirection = SortDirection.ASCENDING;
        this.isLoading = false;
    }

    /**
     * Validates that the inventory size is valid.
     */
    private void validateSize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException(
                    "Size must be a multiple of 9 between 9 and 54, was: " + size);
        }
    }

    // ==================== Configuration ====================

    /**
     * Sets the items to display in the GUI.
     *
     * <p>This replaces any existing items and resets to the first page.
     *
     * @param items the items to display
     * @throws NullPointerException if items is null
     * @since 1.0.0
     */
    public void setItems(@NotNull Collection<T> items) {
        Objects.requireNonNull(items, "items cannot be null");
        this.pagination = new Pagination<>(items, contentSlots.getSlotCount());
        applyFilterAndSort();
        refresh();
    }

    /**
     * Sets the pagination configuration.
     *
     * @param config the pagination configuration
     * @throws NullPointerException if config is null
     * @since 1.0.0
     */
    public void setPaginationConfig(@NotNull PaginationConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        this.config = config;
        refresh();
    }

    /**
     * Sets which slots are used for content.
     *
     * @param slots the content slots configuration
     * @throws NullPointerException if slots is null
     * @since 1.0.0
     */
    public void setContentSlots(@NotNull ContentSlots slots) {
        Objects.requireNonNull(slots, "slots cannot be null");
        this.contentSlots = slots;
        if (pagination != null) {
            this.pagination = pagination.withPageSize(slots.getSlotCount());
        }
        refresh();
    }

    /**
     * Sets the item renderer for converting data items to displayable items.
     *
     * @param renderer the item renderer
     * @throws NullPointerException if renderer is null
     * @since 1.0.0
     */
    public void setItemRenderer(@NotNull ItemRenderer<T> renderer) {
        Objects.requireNonNull(renderer, "renderer cannot be null");
        this.itemRenderer = renderer;
        refresh();
    }

    /**
     * Sets the empty state configuration.
     *
     * @param emptyState the empty state configuration
     * @since 1.0.0
     */
    public void setEmptyState(@Nullable EmptyState emptyState) {
        this.emptyState = emptyState;
        refresh();
    }

    /**
     * Sets the empty state renderer.
     *
     * @param renderer the empty state renderer
     * @since 1.0.0
     */
    public void setEmptyStateRenderer(@Nullable EmptyStateRenderer renderer) {
        this.emptyStateRenderer = renderer;
        refresh();
    }

    /**
     * Sets the async item loader for loading items in the background.
     *
     * @param loader the async item loader
     * @since 1.0.0
     */
    public void setAsyncLoader(@Nullable AsyncItemLoader<T> loader) {
        this.asyncLoader = loader;
    }

    // ==================== Navigation ====================

    /**
     * Navigates to the next page.
     *
     * @since 1.0.0
     */
    public void nextPage() {
        if (pagination.hasNextPage()) {
            pagination.nextPage();
            refresh();
            onPageChange(pagination.getContext());
        }
    }

    /**
     * Navigates to the previous page.
     *
     * @since 1.0.0
     */
    public void previousPage() {
        if (pagination.hasPreviousPage()) {
            pagination.previousPage();
            refresh();
            onPageChange(pagination.getContext());
        }
    }

    /**
     * Navigates to a specific page (0-indexed).
     *
     * @param page the page number to navigate to
     * @since 1.0.0
     */
    public void goToPage(int page) {
        int oldPage = pagination.getCurrentPage();
        pagination.goToPage(page);
        if (oldPage != pagination.getCurrentPage()) {
            refresh();
            onPageChange(pagination.getContext());
        }
    }

    /**
     * Navigates to the first page.
     *
     * @since 1.0.0
     */
    public void firstPage() {
        goToPage(0);
    }

    /**
     * Navigates to the last page.
     *
     * @since 1.0.0
     */
    public void lastPage() {
        goToPage(pagination.getTotalPages() - 1);
    }

    // ==================== Filtering and Sorting ====================

    /**
     * Applies a filter to the displayed items.
     *
     * @param filter the filter predicate, or null to clear the filter
     * @since 1.0.0
     */
    public void setFilter(@Nullable Predicate<T> filter) {
        this.currentFilter = filter;
        applyFilterAndSort();
        refresh();
    }

    /**
     * Applies a filter from a Filter instance.
     *
     * @param filter the filter to apply, or null to clear
     * @since 1.0.0
     */
    public void setFilter(@Nullable Filter<T> filter) {
        this.currentFilter = filter != null ? filter::test : null;
        applyFilterAndSort();
        refresh();
    }

    /**
     * Clears the current filter.
     *
     * @since 1.0.0
     */
    public void clearFilter() {
        setFilter((Predicate<T>) null);
    }

    /**
     * Sets the sorter for displayed items.
     *
     * @param sorter the sorter, or null to clear sorting
     * @since 1.0.0
     */
    public void setSorter(@Nullable Sorter<T> sorter) {
        this.currentSorter = sorter != null ? sorter::compare : null;
        applyFilterAndSort();
        refresh();
    }

    /**
     * Sets the sort comparator for displayed items.
     *
     * @param comparator the comparator, or null to clear sorting
     * @since 1.0.0
     */
    public void setSorter(@Nullable Comparator<T> comparator) {
        this.currentSorter = comparator;
        applyFilterAndSort();
        refresh();
    }

    /**
     * Sets the sort direction.
     *
     * @param direction the sort direction
     * @throws NullPointerException if direction is null
     * @since 1.0.0
     */
    public void setSortDirection(@NotNull SortDirection direction) {
        Objects.requireNonNull(direction, "direction cannot be null");
        this.currentSortDirection = direction;
        applyFilterAndSort();
        refresh();
    }

    /**
     * Toggles the sort direction.
     *
     * @since 1.0.0
     */
    public void toggleSortDirection() {
        setSortDirection(currentSortDirection.opposite());
    }

    /**
     * Clears sorting.
     *
     * @since 1.0.0
     */
    public void clearSorter() {
        setSorter((Comparator<T>) null);
    }

    /**
     * Applies the current filter and sorter to the pagination.
     */
    private void applyFilterAndSort() {
        Pagination<T> result = pagination.clearFilters();

        if (currentFilter != null) {
            result = result.filter(currentFilter);
        }

        if (currentSorter != null) {
            Comparator<T> effectiveSorter = currentSortDirection == SortDirection.DESCENDING
                    ? currentSorter.reversed()
                    : currentSorter;
            result = result.sort(effectiveSorter);
        }

        this.pagination = result;
    }

    // ==================== Async Loading ====================

    /**
     * Loads items asynchronously using the configured loader.
     *
     * @return a CompletableFuture that completes when loading is done
     * @throws IllegalStateException if no async loader is configured
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> loadAsync() {
        if (asyncLoader == null) {
            throw new IllegalStateException("No async loader configured");
        }

        isLoading = true;
        showLoadingState();

        return asyncLoader.load()
                .thenAccept(items -> {
                    isLoading = false;
                    setItems(items);
                })
                .exceptionally(ex -> {
                    isLoading = false;
                    onLoadError(ex);
                    refresh();
                    return null;
                });
    }

    /**
     * Shows the loading indicator.
     *
     * <p>Override this method to customize loading display.
     *
     * @since 1.0.0
     */
    protected void showLoadingState() {
        // Default implementation does nothing
        // Subclasses can override to show loading indicators
    }

    /**
     * Called when async loading fails.
     *
     * <p>Override this method to handle loading errors.
     *
     * @param error the error that occurred
     * @since 1.0.0
     */
    protected void onLoadError(@NotNull Throwable error) {
        // Default implementation does nothing
        // Subclasses can override to display error messages
    }

    // ==================== Rendering ====================

    /**
     * Refreshes the GUI display.
     *
     * <p>This method re-renders all items based on the current pagination state.
     *
     * @since 1.0.0
     */
    public void refresh() {
        if (isLoading) {
            return;
        }

        clearSlots();

        PageContext<T> context = pagination.getContext();

        if (context.isEmpty() && emptyState != null) {
            renderEmptyState(context);
        } else {
            renderItems(context);
        }

        renderNavigationButtons(context);
        renderPageInfo(context);

        onRefresh(context);
    }

    /**
     * Renders the items for the current page.
     *
     * @param context the current page context
     */
    private void renderItems(@NotNull PageContext<T> context) {
        if (itemRenderer == null) {
            return;
        }

        int[] slots = contentSlots.getSlots();
        java.util.List<T> items = context.getItems();

        for (int i = 0; i < items.size() && i < slots.length; i++) {
            T item = items.get(i);
            int slot = slots[i];
            UnifiedItemStack displayItem = itemRenderer.render(item, slot, context);
            if (displayItem != null) {
                setSlot(slot, displayItem);
            }
        }
    }

    /**
     * Renders the empty state.
     *
     * @param context the current page context
     */
    private void renderEmptyState(@NotNull PageContext<T> context) {
        if (emptyStateRenderer != null && emptyState != null) {
            emptyStateRenderer.render(emptyState, contentSlots, this::setSlot);
        } else if (emptyState != null) {
            // Default: center the empty state item in the content area
            int[] slots = contentSlots.getSlots();
            if (slots.length > 0) {
                int centerSlot = slots[slots.length / 2];
                UnifiedItemStack item = emptyState.toItemStack();
                if (item != null) {
                    setSlot(centerSlot, item);
                }
            }
        }
    }

    /**
     * Renders the navigation buttons.
     *
     * @param context the current page context
     */
    private void renderNavigationButtons(@NotNull PageContext<T> context) {
        // Previous button
        if (config.getPreviousButtonSlot() >= 0) {
            UnifiedItemStack prevButton = context.hasPreviousPage()
                    ? config.getPreviousButton()
                    : config.getDisabledPreviousButton();
            if (prevButton != null) {
                setSlot(config.getPreviousButtonSlot(), prevButton);
            }
        }

        // Next button
        if (config.getNextButtonSlot() >= 0) {
            UnifiedItemStack nextButton = context.hasNextPage()
                    ? config.getNextButton()
                    : config.getDisabledNextButton();
            if (nextButton != null) {
                setSlot(config.getNextButtonSlot(), nextButton);
            }
        }
    }

    /**
     * Renders the page info indicator.
     *
     * @param context the current page context
     */
    private void renderPageInfo(@NotNull PageContext<T> context) {
        if (config.getPageInfoSlot() >= 0 && config.getPageInfoBuilder() != null) {
            UnifiedItemStack pageInfo = config.getPageInfoBuilder().buildPageInfo(context);
            if (pageInfo != null) {
                setSlot(config.getPageInfoSlot(), pageInfo);
            }
        }
    }

    // ==================== Slot Handling ====================

    /**
     * Handles a click on a slot.
     *
     * @param slot      the slot that was clicked
     * @param clickType the type of click
     * @since 1.0.0
     */
    public void handleClick(int slot, @NotNull Object clickType) {
        // Check navigation buttons
        if (slot == config.getPreviousButtonSlot()) {
            previousPage();
            return;
        }

        if (slot == config.getNextButtonSlot()) {
            nextPage();
            return;
        }

        // Check if it's a content slot
        int contentIndex = contentSlots.getContentIndex(slot);
        if (contentIndex >= 0) {
            PageContext<T> context = pagination.getContext();
            Optional<T> itemOpt = context.getItemAt(contentIndex);
            itemOpt.ifPresent(item -> handleItemClick(item, slot, clickType));
        }
    }

    /**
     * Returns the item at a slot, if any.
     *
     * @param slot the slot to check
     * @return an Optional containing the item at the slot
     * @since 1.0.0
     */
    @NotNull
    public Optional<T> getItemAtSlot(int slot) {
        int contentIndex = contentSlots.getContentIndex(slot);
        if (contentIndex >= 0) {
            return pagination.getContext().getItemAt(contentIndex);
        }
        return Optional.empty();
    }

    // ==================== Abstract Methods ====================

    /**
     * Sets an item in a specific slot.
     *
     * <p>Implementations should update the underlying inventory.
     *
     * @param slot the slot to set
     * @param item the item to place in the slot
     * @since 1.0.0
     */
    protected abstract void setSlot(int slot, @NotNull UnifiedItemStack item);

    /**
     * Clears all content slots.
     *
     * <p>Implementations should clear the underlying inventory.
     *
     * @since 1.0.0
     */
    protected abstract void clearSlots();

    /**
     * Opens the GUI for the viewer.
     *
     * @since 1.0.0
     */
    public abstract void open();

    /**
     * Closes the GUI.
     *
     * @since 1.0.0
     */
    public abstract void close();

    /**
     * Called when an item in the content area is clicked.
     *
     * @param item      the clicked item
     * @param slot      the slot that was clicked
     * @param clickType the type of click (platform-specific)
     * @since 1.0.0
     */
    protected abstract void handleItemClick(@NotNull T item, int slot, @NotNull Object clickType);

    // ==================== Lifecycle Callbacks ====================

    /**
     * Called when the page changes.
     *
     * <p>Override this method to react to page changes.
     *
     * @param context the new page context
     * @since 1.0.0
     */
    protected void onPageChange(@NotNull PageContext<T> context) {
        // Default implementation does nothing
    }

    /**
     * Called after a refresh completes.
     *
     * <p>Override this method to perform additional rendering.
     *
     * @param context the current page context
     * @since 1.0.0
     */
    protected void onRefresh(@NotNull PageContext<T> context) {
        // Default implementation does nothing
    }

    // ==================== Getters ====================

    /**
     * Returns the player viewing this GUI.
     *
     * @return the viewer
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getViewer() {
        return viewer;
    }

    /**
     * Returns the title of this GUI.
     *
     * @return the title
     * @since 1.0.0
     */
    @NotNull
    public String getTitle() {
        return title;
    }

    /**
     * Returns the size of this GUI in slots.
     *
     * @return the size
     * @since 1.0.0
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the current pagination state.
     *
     * @return the pagination
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> getPagination() {
        return pagination;
    }

    /**
     * Returns the current page context.
     *
     * @return the current page context
     * @since 1.0.0
     */
    @NotNull
    public PageContext<T> getContext() {
        return pagination.getContext();
    }

    /**
     * Returns the pagination configuration.
     *
     * @return the pagination config
     * @since 1.0.0
     */
    @NotNull
    public PaginationConfig getConfig() {
        return config;
    }

    /**
     * Returns the content slots configuration.
     *
     * @return the content slots
     * @since 1.0.0
     */
    @NotNull
    public ContentSlots getContentSlots() {
        return contentSlots;
    }

    /**
     * Returns the current sort direction.
     *
     * @return the current sort direction
     * @since 1.0.0
     */
    @NotNull
    public SortDirection getSortDirection() {
        return currentSortDirection;
    }

    /**
     * Checks if the GUI is currently loading.
     *
     * @return true if loading
     * @since 1.0.0
     */
    public boolean isLoading() {
        return isLoading;
    }
}
