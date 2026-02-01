/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic pagination class for managing paginated collections of items.
 *
 * <p>This class provides efficient pagination of any collection of items,
 * with support for filtering, sorting, and navigation between pages.
 * The pagination is immutable with respect to the underlying data after
 * construction - modifications require creating a new Pagination instance.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create pagination with 45 items per page
 * List<Player> allPlayers = server.getOnlinePlayers();
 * Pagination<Player> pagination = new Pagination<>(allPlayers, 45);
 *
 * // Navigate pages
 * List<Player> firstPage = pagination.getPageItems();
 * pagination.nextPage();
 * List<Player> secondPage = pagination.getPageItems();
 *
 * // With filtering
 * Pagination<Player> filtered = pagination.filter(p -> p.hasPermission("vip"));
 *
 * // With sorting
 * Pagination<Player> sorted = pagination.sort(Comparator.comparing(Player::getName));
 *
 * // Chained operations
 * Pagination<Player> result = new Pagination<>(allPlayers, 45)
 *     .filter(p -> p.isOnline())
 *     .sort(Comparator.comparing(Player::getName))
 *     .goToPage(2);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. External synchronization is required
 * when accessing from multiple threads. Consider creating copies for
 * concurrent access.
 *
 * @param <T> the type of items being paginated
 * @since 1.0.0
 * @author Supatuck
 * @see PageContext
 * @see PaginatedGUI
 */
public class Pagination<T> {

    /**
     * The default number of items per page.
     */
    public static final int DEFAULT_PAGE_SIZE = 45;

    private final List<T> allItems;
    private final List<T> filteredItems;
    private final int pageSize;
    private int currentPage;

    /**
     * Creates a new Pagination with the specified items and default page size.
     *
     * @param items the items to paginate
     * @throws NullPointerException if items is null
     * @since 1.0.0
     */
    public Pagination(@NotNull Collection<T> items) {
        this(items, DEFAULT_PAGE_SIZE);
    }

    /**
     * Creates a new Pagination with the specified items and page size.
     *
     * @param items    the items to paginate
     * @param pageSize the number of items per page
     * @throws NullPointerException     if items is null
     * @throws IllegalArgumentException if pageSize is less than 1
     * @since 1.0.0
     */
    public Pagination(@NotNull Collection<T> items, int pageSize) {
        Objects.requireNonNull(items, "items cannot be null");
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1, was: " + pageSize);
        }

        this.allItems = new ArrayList<>(items);
        this.filteredItems = new ArrayList<>(this.allItems);
        this.pageSize = pageSize;
        this.currentPage = 0;
    }

    /**
     * Private constructor for internal operations that preserve filtered state.
     */
    private Pagination(@NotNull List<T> allItems, @NotNull List<T> filteredItems,
                       int pageSize, int currentPage) {
        this.allItems = new ArrayList<>(allItems);
        this.filteredItems = new ArrayList<>(filteredItems);
        this.pageSize = pageSize;
        this.currentPage = Math.max(0, Math.min(currentPage, getMaxPage(filteredItems.size(), pageSize)));
    }

    /**
     * Returns the items for the current page.
     *
     * @return an unmodifiable list of items on the current page
     * @since 1.0.0
     */
    @NotNull
    public List<T> getPageItems() {
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, filteredItems.size());

        if (start >= filteredItems.size()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(filteredItems.subList(start, end));
    }

    /**
     * Returns all items across all pages (after filtering).
     *
     * @return an unmodifiable list of all filtered items
     * @since 1.0.0
     */
    @NotNull
    public List<T> getAllItems() {
        return Collections.unmodifiableList(filteredItems);
    }

    /**
     * Returns all original items (before filtering).
     *
     * @return an unmodifiable list of all original items
     * @since 1.0.0
     */
    @NotNull
    public List<T> getOriginalItems() {
        return Collections.unmodifiableList(allItems);
    }

    /**
     * Returns the current page number (0-indexed).
     *
     * @return the current page number
     * @since 1.0.0
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Returns the current page number (1-indexed) for display purposes.
     *
     * @return the current page number starting from 1
     * @since 1.0.0
     */
    public int getDisplayPage() {
        return currentPage + 1;
    }

    /**
     * Returns the total number of pages.
     *
     * @return the total number of pages (at least 1)
     * @since 1.0.0
     */
    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredItems.size() / pageSize));
    }

    /**
     * Returns the number of items per page.
     *
     * @return the page size
     * @since 1.0.0
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Returns the total number of items (after filtering).
     *
     * @return the total number of filtered items
     * @since 1.0.0
     */
    public int getTotalItems() {
        return filteredItems.size();
    }

    /**
     * Returns the number of items on the current page.
     *
     * @return the number of items on the current page
     * @since 1.0.0
     */
    public int getCurrentPageItemCount() {
        return getPageItems().size();
    }

    /**
     * Checks if there is a next page available.
     *
     * @return true if there is a next page
     * @since 1.0.0
     */
    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }

    /**
     * Checks if there is a previous page available.
     *
     * @return true if there is a previous page
     * @since 1.0.0
     */
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    /**
     * Checks if the pagination is empty (no items).
     *
     * @return true if there are no items
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return filteredItems.isEmpty();
    }

    /**
     * Navigates to the next page.
     *
     * @return this pagination for chaining
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> nextPage() {
        if (hasNextPage()) {
            currentPage++;
        }
        return this;
    }

    /**
     * Navigates to the previous page.
     *
     * @return this pagination for chaining
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
        }
        return this;
    }

    /**
     * Navigates to the first page.
     *
     * @return this pagination for chaining
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> firstPage() {
        currentPage = 0;
        return this;
    }

    /**
     * Navigates to the last page.
     *
     * @return this pagination for chaining
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> lastPage() {
        currentPage = getTotalPages() - 1;
        return this;
    }

    /**
     * Navigates to a specific page (0-indexed).
     *
     * <p>If the page number is out of bounds, it will be clamped
     * to the valid range.
     *
     * @param page the page number to navigate to
     * @return this pagination for chaining
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> goToPage(int page) {
        currentPage = Math.max(0, Math.min(page, getTotalPages() - 1));
        return this;
    }

    /**
     * Creates a new Pagination with items filtered by the predicate.
     *
     * <p>The current page is reset to 0 in the new pagination.
     *
     * @param predicate the filter predicate
     * @return a new filtered Pagination
     * @throws NullPointerException if predicate is null
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> filter(@NotNull Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        List<T> newFiltered = filteredItems.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        return new Pagination<>(allItems, newFiltered, pageSize, 0);
    }

    /**
     * Creates a new Pagination with items sorted by the comparator.
     *
     * <p>The current page is preserved if still valid.
     *
     * @param comparator the comparator to sort by
     * @return a new sorted Pagination
     * @throws NullPointerException if comparator is null
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> sort(@NotNull Comparator<T> comparator) {
        Objects.requireNonNull(comparator, "comparator cannot be null");
        List<T> newFiltered = new ArrayList<>(filteredItems);
        newFiltered.sort(comparator);
        return new Pagination<>(allItems, newFiltered, pageSize, currentPage);
    }

    /**
     * Resets any filters, showing all original items.
     *
     * @return a new Pagination with no filters applied
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> clearFilters() {
        return new Pagination<>(allItems, new ArrayList<>(allItems), pageSize, 0);
    }

    /**
     * Creates a new Pagination with a different page size.
     *
     * <p>The current page is adjusted to keep the first visible item
     * on screen if possible.
     *
     * @param newPageSize the new page size
     * @return a new Pagination with the new page size
     * @throws IllegalArgumentException if newPageSize is less than 1
     * @since 1.0.0
     */
    @NotNull
    public Pagination<T> withPageSize(int newPageSize) {
        if (newPageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1, was: " + newPageSize);
        }
        // Try to keep approximately the same position
        int firstItemIndex = currentPage * pageSize;
        int newPage = firstItemIndex / newPageSize;
        return new Pagination<>(allItems, filteredItems, newPageSize, newPage);
    }

    /**
     * Creates a PageContext for the current state.
     *
     * @return a PageContext representing the current page state
     * @since 1.0.0
     */
    @NotNull
    public PageContext<T> getContext() {
        return new PageContext<>(
                currentPage,
                getTotalPages(),
                getPageItems(),
                getTotalItems(),
                hasNextPage(),
                hasPreviousPage()
        );
    }

    /**
     * Gets the item at the specified index on the current page.
     *
     * @param indexOnPage the index on the current page (0-indexed)
     * @return the item at the index, or null if index is out of bounds
     * @since 1.0.0
     */
    @Nullable
    public T getItemAt(int indexOnPage) {
        List<T> pageItems = getPageItems();
        if (indexOnPage < 0 || indexOnPage >= pageItems.size()) {
            return null;
        }
        return pageItems.get(indexOnPage);
    }

    /**
     * Gets the global index of an item on the current page.
     *
     * @param indexOnPage the index on the current page (0-indexed)
     * @return the global index in the filtered items list
     * @since 1.0.0
     */
    public int getGlobalIndex(int indexOnPage) {
        return currentPage * pageSize + indexOnPage;
    }

    /**
     * Calculates the maximum page index for a given item count and page size.
     */
    private static int getMaxPage(int itemCount, int pageSize) {
        if (itemCount == 0) {
            return 0;
        }
        return (int) Math.ceil((double) itemCount / pageSize) - 1;
    }

    @Override
    public String toString() {
        return String.format("Pagination{page=%d/%d, items=%d, pageSize=%d}",
                getDisplayPage(), getTotalPages(), getTotalItems(), pageSize);
    }
}
