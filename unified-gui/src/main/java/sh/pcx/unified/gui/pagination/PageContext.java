/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable context representing the current state of a paginated view.
 *
 * <p>PageContext provides a snapshot of pagination state including the current
 * page number, total pages, and items on the current page. This is useful for
 * rendering page indicators, navigation buttons, and page content.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PageContext<Item> context = pagination.getContext();
 *
 * // Display page indicator
 * String indicator = String.format("Page %d of %d",
 *     context.getDisplayPage(), context.getTotalPages());
 *
 * // Render items
 * for (int i = 0; i < context.getItemCount(); i++) {
 *     Item item = context.getItems().get(i);
 *     renderItem(slots[i], item);
 * }
 *
 * // Show/hide navigation buttons
 * if (context.hasNextPage()) {
 *     showNextButton();
 * }
 * if (context.hasPreviousPage()) {
 *     showPreviousButton();
 * }
 *
 * // Check if empty
 * if (context.isEmpty()) {
 *     showEmptyState();
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe. The items list is wrapped
 * in an unmodifiable view, but the items themselves may be mutable.
 *
 * @param <T> the type of items on the page
 * @since 1.0.0
 * @author Supatuck
 * @see Pagination
 */
public final class PageContext<T> {

    private final int pageNumber;
    private final int totalPages;
    private final List<T> items;
    private final int totalItems;
    private final boolean hasNextPage;
    private final boolean hasPreviousPage;

    /**
     * Creates a new PageContext with the specified values.
     *
     * @param pageNumber      the current page number (0-indexed)
     * @param totalPages      the total number of pages
     * @param items           the items on the current page
     * @param totalItems      the total number of items across all pages
     * @param hasNextPage     whether there is a next page
     * @param hasPreviousPage whether there is a previous page
     * @throws NullPointerException     if items is null
     * @throws IllegalArgumentException if pageNumber or totalPages is negative
     * @since 1.0.0
     */
    public PageContext(int pageNumber, int totalPages, @NotNull List<T> items,
                       int totalItems, boolean hasNextPage, boolean hasPreviousPage) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber cannot be negative: " + pageNumber);
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages cannot be negative: " + totalPages);
        }
        Objects.requireNonNull(items, "items cannot be null");

        this.pageNumber = pageNumber;
        this.totalPages = totalPages;
        this.items = Collections.unmodifiableList(items);
        this.totalItems = totalItems;
        this.hasNextPage = hasNextPage;
        this.hasPreviousPage = hasPreviousPage;
    }

    /**
     * Creates an empty PageContext.
     *
     * @param <T> the type of items
     * @return an empty PageContext
     * @since 1.0.0
     */
    @NotNull
    public static <T> PageContext<T> empty() {
        return new PageContext<>(0, 1, Collections.emptyList(), 0, false, false);
    }

    /**
     * Returns the current page number (0-indexed).
     *
     * @return the current page number
     * @since 1.0.0
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Returns the current page number (1-indexed) for display purposes.
     *
     * @return the current page number starting from 1
     * @since 1.0.0
     */
    public int getDisplayPage() {
        return pageNumber + 1;
    }

    /**
     * Returns the total number of pages.
     *
     * @return the total number of pages
     * @since 1.0.0
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Returns the items on the current page.
     *
     * @return an unmodifiable list of items on the current page
     * @since 1.0.0
     */
    @NotNull
    public List<T> getItems() {
        return items;
    }

    /**
     * Returns the number of items on the current page.
     *
     * @return the number of items on the current page
     * @since 1.0.0
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Returns the total number of items across all pages.
     *
     * @return the total number of items
     * @since 1.0.0
     */
    public int getTotalItems() {
        return totalItems;
    }

    /**
     * Checks if there is a next page available.
     *
     * @return true if there is a next page
     * @since 1.0.0
     */
    public boolean hasNextPage() {
        return hasNextPage;
    }

    /**
     * Checks if there is a previous page available.
     *
     * @return true if there is a previous page
     * @since 1.0.0
     */
    public boolean hasPreviousPage() {
        return hasPreviousPage;
    }

    /**
     * Checks if this context represents an empty page (no items).
     *
     * @return true if there are no items on this page
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Checks if this is the first page.
     *
     * @return true if this is the first page
     * @since 1.0.0
     */
    public boolean isFirstPage() {
        return pageNumber == 0;
    }

    /**
     * Checks if this is the last page.
     *
     * @return true if this is the last page
     * @since 1.0.0
     */
    public boolean isLastPage() {
        return pageNumber == totalPages - 1 || totalPages == 0;
    }

    /**
     * Gets the item at the specified index on this page.
     *
     * @param index the index on this page (0-indexed)
     * @return an Optional containing the item, or empty if index is out of bounds
     * @since 1.0.0
     */
    @NotNull
    public Optional<T> getItemAt(int index) {
        if (index < 0 || index >= items.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(index));
    }

    /**
     * Formats a page indicator string.
     *
     * <p>Returns a string in the format "Page X of Y".
     *
     * @return a formatted page indicator string
     * @since 1.0.0
     */
    @NotNull
    public String formatPageIndicator() {
        return String.format("Page %d of %d", getDisplayPage(), totalPages);
    }

    /**
     * Formats a page indicator string with a custom format.
     *
     * <p>The format string should contain two {@code %d} placeholders
     * for current page and total pages respectively.
     *
     * @param format the format string
     * @return a formatted page indicator string
     * @since 1.0.0
     */
    @NotNull
    public String formatPageIndicator(@NotNull String format) {
        return String.format(format, getDisplayPage(), totalPages);
    }

    /**
     * Formats an item count string.
     *
     * <p>Returns a string describing the items shown, e.g., "Showing 1-45 of 100".
     *
     * @param pageSize the number of items per page
     * @return a formatted item count string
     * @since 1.0.0
     */
    @NotNull
    public String formatItemCount(int pageSize) {
        if (totalItems == 0) {
            return "No items";
        }
        int start = pageNumber * pageSize + 1;
        int end = start + items.size() - 1;
        return String.format("Showing %d-%d of %d", start, end, totalItems);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageContext<?> that = (PageContext<?>) o;
        return pageNumber == that.pageNumber &&
                totalPages == that.totalPages &&
                totalItems == that.totalItems &&
                hasNextPage == that.hasNextPage &&
                hasPreviousPage == that.hasPreviousPage &&
                Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, totalPages, items, totalItems, hasNextPage, hasPreviousPage);
    }

    @Override
    public String toString() {
        return String.format("PageContext{page=%d/%d, items=%d, total=%d, hasNext=%s, hasPrev=%s}",
                getDisplayPage(), totalPages, items.size(), totalItems, hasNextPage, hasPreviousPage);
    }
}
