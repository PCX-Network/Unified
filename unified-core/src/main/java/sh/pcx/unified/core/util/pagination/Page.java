/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.pagination;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Represents a single page of paginated results.
 *
 * <p>A Page contains a subset of items from a larger collection along with
 * metadata about the page's position within the full result set.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a paginated result
 * Page<Player> page = Page.of(players, 1, 10, totalPlayers);
 *
 * // Access page data
 * List<Player> items = page.items();
 * int pageNumber = page.pageNumber();
 * int totalPages = page.totalPages();
 *
 * // Navigation
 * if (page.hasNext()) {
 *     // Show "Next" button
 * }
 * if (page.hasPrevious()) {
 *     // Show "Previous" button
 * }
 *
 * // Iterate items
 * for (Player player : page) {
 *     player.sendMessage("You are on page " + page.pageNumber());
 * }
 *
 * // Transform items
 * Page<String> names = page.map(Player::getName);
 * }</pre>
 *
 * @param <T> the type of items in the page
 * @since 1.0.0
 * @author Supatuck
 */
public record Page<T>(
        @NotNull List<T> items,
        int pageNumber,
        int pageSize,
        long totalItems,
        int totalPages
) implements Iterable<T> {

    /**
     * Creates a new Page with validation.
     *
     * @param items      the items on this page
     * @param pageNumber the 1-based page number
     * @param pageSize   the maximum items per page
     * @param totalItems the total number of items across all pages
     * @param totalPages the total number of pages
     */
    public Page {
        Objects.requireNonNull(items, "items cannot be null");
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        if (totalItems < 0) {
            throw new IllegalArgumentException("totalItems must be >= 0");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages must be >= 0");
        }
        items = List.copyOf(items); // Make immutable
    }

    /**
     * Creates a page from a list of items.
     *
     * @param items      the items on this page
     * @param pageNumber the 1-based page number
     * @param pageSize   the maximum items per page
     * @param totalItems the total number of items
     * @param <T>        the item type
     * @return a new Page
     * @since 1.0.0
     */
    @NotNull
    public static <T> Page<T> of(@NotNull List<T> items, int pageNumber, int pageSize, long totalItems) {
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / pageSize);
        return new Page<>(items, pageNumber, pageSize, totalItems, totalPages);
    }

    /**
     * Creates an empty page.
     *
     * @param pageSize the page size
     * @param <T>      the item type
     * @return an empty Page
     * @since 1.0.0
     */
    @NotNull
    public static <T> Page<T> empty(int pageSize) {
        return new Page<>(Collections.emptyList(), 1, pageSize, 0, 1);
    }

    /**
     * Creates a single page containing all items.
     *
     * @param items the items
     * @param <T>   the item type
     * @return a Page containing all items
     * @since 1.0.0
     */
    @NotNull
    public static <T> Page<T> single(@NotNull List<T> items) {
        if (items.isEmpty()) {
            return empty(10);
        }
        return new Page<>(items, 1, items.size(), items.size(), 1);
    }

    // ==================== Navigation ====================

    /**
     * Checks if there is a next page.
     *
     * @return true if there is a next page
     * @since 1.0.0
     */
    public boolean hasNext() {
        return pageNumber < totalPages;
    }

    /**
     * Checks if there is a previous page.
     *
     * @return true if there is a previous page
     * @since 1.0.0
     */
    public boolean hasPrevious() {
        return pageNumber > 1;
    }

    /**
     * Checks if this is the first page.
     *
     * @return true if this is page 1
     * @since 1.0.0
     */
    public boolean isFirst() {
        return pageNumber == 1;
    }

    /**
     * Checks if this is the last page.
     *
     * @return true if this is the last page
     * @since 1.0.0
     */
    public boolean isLast() {
        return pageNumber >= totalPages;
    }

    /**
     * Gets the next page number, or the current if on last page.
     *
     * @return the next page number
     * @since 1.0.0
     */
    public int nextPageNumber() {
        return hasNext() ? pageNumber + 1 : pageNumber;
    }

    /**
     * Gets the previous page number, or 1 if on first page.
     *
     * @return the previous page number
     * @since 1.0.0
     */
    public int previousPageNumber() {
        return hasPrevious() ? pageNumber - 1 : 1;
    }

    // ==================== Item Access ====================

    /**
     * Gets the number of items on this page.
     *
     * @return the item count
     * @since 1.0.0
     */
    public int size() {
        return items.size();
    }

    /**
     * Checks if this page is empty.
     *
     * @return true if no items on this page
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Checks if this page has any items.
     *
     * @return true if there are items on this page
     * @since 1.0.0
     */
    public boolean hasContent() {
        return !items.isEmpty();
    }

    /**
     * Gets the item at the specified index on this page.
     *
     * @param index the index (0-based)
     * @return the item
     * @throws IndexOutOfBoundsException if index is out of range
     * @since 1.0.0
     */
    @NotNull
    public T get(int index) {
        return items.get(index);
    }

    /**
     * Gets the first item on this page.
     *
     * @return the first item
     * @throws java.util.NoSuchElementException if page is empty
     * @since 1.0.0
     */
    @NotNull
    public T first() {
        if (items.isEmpty()) {
            throw new java.util.NoSuchElementException("Page is empty");
        }
        return items.getFirst();
    }

    /**
     * Gets the last item on this page.
     *
     * @return the last item
     * @throws java.util.NoSuchElementException if page is empty
     * @since 1.0.0
     */
    @NotNull
    public T last() {
        if (items.isEmpty()) {
            throw new java.util.NoSuchElementException("Page is empty");
        }
        return items.getLast();
    }

    // ==================== Index Calculations ====================

    /**
     * Gets the 0-based offset of the first item on this page.
     *
     * @return the offset
     * @since 1.0.0
     */
    public long offset() {
        return (long) (pageNumber - 1) * pageSize;
    }

    /**
     * Gets the 1-based index of the first item on this page.
     *
     * @return the start index
     * @since 1.0.0
     */
    public long startIndex() {
        return offset() + 1;
    }

    /**
     * Gets the 1-based index of the last item on this page.
     *
     * @return the end index
     * @since 1.0.0
     */
    public long endIndex() {
        return offset() + items.size();
    }

    /**
     * Gets a display string like "1-10 of 100".
     *
     * @return the display string
     * @since 1.0.0
     */
    @NotNull
    public String displayRange() {
        if (isEmpty()) {
            return "0 of 0";
        }
        return startIndex() + "-" + endIndex() + " of " + totalItems;
    }

    // ==================== Transformation ====================

    /**
     * Transforms the items using the given function.
     *
     * @param mapper the transformation function
     * @param <R>    the result type
     * @return a new Page with transformed items
     * @since 1.0.0
     */
    @NotNull
    public <R> Page<R> map(@NotNull Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        List<R> mappedItems = items.stream().map(mapper).toList();
        return new Page<>(mappedItems, pageNumber, pageSize, totalItems, totalPages);
    }

    /**
     * Returns a stream of the items.
     *
     * @return a stream
     * @since 1.0.0
     */
    @NotNull
    public Stream<T> stream() {
        return items.stream();
    }

    @Override
    @NotNull
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    public void forEach(@NotNull Consumer<? super T> action) {
        items.forEach(action);
    }

    @Override
    public String toString() {
        return "Page{" +
                "page=" + pageNumber + "/" + totalPages +
                ", items=" + items.size() + "/" + totalItems +
                '}';
    }
}
