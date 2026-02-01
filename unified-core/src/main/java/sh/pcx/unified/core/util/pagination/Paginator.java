/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.pagination;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility class for paginating collections of items.
 *
 * <p>Provides both static utility methods and a builder pattern for creating
 * paginated views of data.
 *
 * <h2>Static Utility Methods</h2>
 * <pre>{@code
 * // Paginate a list
 * List<String> items = List.of("a", "b", "c", "d", "e");
 * Page<String> page = Paginator.paginate(items, 1, 2);
 * // page.items() = ["a", "b"], page 1 of 3
 *
 * // Get total pages
 * int pages = Paginator.totalPages(items.size(), 2); // 3
 * }</pre>
 *
 * <h2>Builder Pattern</h2>
 * <pre>{@code
 * Paginator<Player> paginator = Paginator.<Player>builder()
 *     .source(players)
 *     .pageSize(10)
 *     .filter(p -> p.isOnline())
 *     .sort(Comparator.comparing(Player::getName))
 *     .build();
 *
 * Page<Player> page1 = paginator.getPage(1);
 * Page<Player> page2 = paginator.getPage(2);
 *
 * // Navigate
 * Page<Player> next = paginator.next(page1);
 * Page<Player> prev = paginator.previous(page2);
 * }</pre>
 *
 * <h2>Dynamic Sources</h2>
 * <pre>{@code
 * // For data that changes frequently, use a supplier
 * Paginator<Player> paginator = Paginator.<Player>builder()
 *     .sourceSupplier(() -> Bukkit.getOnlinePlayers())
 *     .pageSize(10)
 *     .build();
 * }</pre>
 *
 * @param <T> the type of items being paginated
 * @since 1.0.0
 * @author Supatuck
 */
public final class Paginator<T> {

    /**
     * Default page size if not specified.
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    private final java.util.function.Supplier<? extends Collection<T>> sourceSupplier;
    private final int pageSize;
    private final Predicate<T> filter;
    private final Comparator<T> comparator;

    private Paginator(Builder<T> builder) {
        this.sourceSupplier = builder.sourceSupplier;
        this.pageSize = builder.pageSize;
        this.filter = builder.filter;
        this.comparator = builder.comparator;
    }

    // ==================== Static Utility Methods ====================

    /**
     * Paginates a list, returning the specified page.
     *
     * @param items      the full list of items
     * @param pageNumber the 1-based page number
     * @param pageSize   the number of items per page
     * @param <T>        the item type
     * @return the requested page
     * @since 1.0.0
     */
    @NotNull
    public static <T> Page<T> paginate(@NotNull List<T> items, int pageNumber, int pageSize) {
        Objects.requireNonNull(items, "items cannot be null");
        if (pageNumber < 1) pageNumber = 1;
        if (pageSize < 1) pageSize = DEFAULT_PAGE_SIZE;

        int totalItems = items.size();
        int totalPages = totalPages(totalItems, pageSize);

        if (pageNumber > totalPages) {
            pageNumber = Math.max(1, totalPages);
        }

        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        List<T> pageItems;
        if (startIndex >= totalItems) {
            pageItems = Collections.emptyList();
        } else {
            pageItems = items.subList(startIndex, endIndex);
        }

        return Page.of(pageItems, pageNumber, pageSize, totalItems);
    }

    /**
     * Paginates a collection (converts to list first).
     *
     * @param items      the collection of items
     * @param pageNumber the 1-based page number
     * @param pageSize   the number of items per page
     * @param <T>        the item type
     * @return the requested page
     * @since 1.0.0
     */
    @NotNull
    public static <T> Page<T> paginate(@NotNull Collection<T> items, int pageNumber, int pageSize) {
        return paginate(new ArrayList<>(items), pageNumber, pageSize);
    }

    /**
     * Paginates a stream.
     *
     * @param stream     the stream of items
     * @param pageNumber the 1-based page number
     * @param pageSize   the number of items per page
     * @param <T>        the item type
     * @return the requested page
     * @since 1.0.0
     */
    @NotNull
    public static <T> Page<T> paginate(@NotNull Stream<T> stream, int pageNumber, int pageSize) {
        return paginate(stream.toList(), pageNumber, pageSize);
    }

    /**
     * Calculates the total number of pages.
     *
     * @param totalItems the total number of items
     * @param pageSize   the items per page
     * @return the total number of pages
     * @since 1.0.0
     */
    public static int totalPages(long totalItems, int pageSize) {
        if (totalItems <= 0) return 1;
        if (pageSize <= 0) pageSize = DEFAULT_PAGE_SIZE;
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    /**
     * Calculates the 0-based offset for a page.
     *
     * @param pageNumber the 1-based page number
     * @param pageSize   the items per page
     * @return the offset
     * @since 1.0.0
     */
    public static int offset(int pageNumber, int pageSize) {
        return Math.max(0, pageNumber - 1) * Math.max(1, pageSize);
    }

    /**
     * Validates and normalizes a page number.
     *
     * @param pageNumber the requested page number
     * @param totalPages the total number of pages
     * @return a valid page number (1 to totalPages)
     * @since 1.0.0
     */
    public static int normalizePageNumber(int pageNumber, int totalPages) {
        if (totalPages <= 0) return 1;
        return Math.max(1, Math.min(pageNumber, totalPages));
    }

    /**
     * Creates a range of page numbers for navigation.
     *
     * <p>Useful for creating page number buttons in GUIs.
     *
     * @param currentPage   the current page number
     * @param totalPages    the total number of pages
     * @param maxVisible    the maximum number of page numbers to show
     * @return a list of page numbers to display
     * @since 1.0.0
     */
    @NotNull
    public static List<Integer> pageRange(int currentPage, int totalPages, int maxVisible) {
        if (totalPages <= 0) return List.of(1);
        if (maxVisible <= 0) maxVisible = 5;

        List<Integer> pages = new ArrayList<>(maxVisible);

        int half = maxVisible / 2;
        int start = Math.max(1, currentPage - half);
        int end = Math.min(totalPages, start + maxVisible - 1);

        // Adjust start if end hit the limit
        if (end - start + 1 < maxVisible) {
            start = Math.max(1, end - maxVisible + 1);
        }

        for (int i = start; i <= end; i++) {
            pages.add(i);
        }

        return pages;
    }

    // ==================== Builder ====================

    /**
     * Creates a new Paginator builder.
     *
     * @param <T> the item type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for creating Paginator instances.
     *
     * @param <T> the item type
     * @since 1.0.0
     */
    public static final class Builder<T> {
        private java.util.function.Supplier<? extends Collection<T>> sourceSupplier;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private Predicate<T> filter;
        private Comparator<T> comparator;

        private Builder() {}

        /**
         * Sets the source collection.
         *
         * @param source the source collection
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> source(@NotNull Collection<T> source) {
            Objects.requireNonNull(source, "source cannot be null");
            this.sourceSupplier = () -> source;
            return this;
        }

        /**
         * Sets a dynamic source supplier.
         *
         * <p>The supplier is called each time a page is requested.
         *
         * @param supplier the source supplier
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> sourceSupplier(@NotNull java.util.function.Supplier<? extends Collection<T>> supplier) {
            Objects.requireNonNull(supplier, "supplier cannot be null");
            this.sourceSupplier = supplier;
            return this;
        }

        /**
         * Sets the page size.
         *
         * @param pageSize the items per page
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> pageSize(int pageSize) {
            if (pageSize < 1) {
                throw new IllegalArgumentException("pageSize must be >= 1");
            }
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets a filter predicate.
         *
         * <p>Items that don't match the filter are excluded from pagination.
         *
         * @param filter the filter predicate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> filter(@NotNull Predicate<T> filter) {
            this.filter = Objects.requireNonNull(filter, "filter cannot be null");
            return this;
        }

        /**
         * Sets a comparator for sorting.
         *
         * @param comparator the comparator
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> sort(@NotNull Comparator<T> comparator) {
            this.comparator = Objects.requireNonNull(comparator, "comparator cannot be null");
            return this;
        }

        /**
         * Sets sorting by a comparable property.
         *
         * @param keyExtractor the key extractor function
         * @param <U>          the comparable type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public <U extends Comparable<? super U>> Builder<T> sortBy(@NotNull Function<T, U> keyExtractor) {
            this.comparator = Comparator.comparing(keyExtractor);
            return this;
        }

        /**
         * Sets descending sort by a comparable property.
         *
         * @param keyExtractor the key extractor function
         * @param <U>          the comparable type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public <U extends Comparable<? super U>> Builder<T> sortByDescending(@NotNull Function<T, U> keyExtractor) {
            this.comparator = Comparator.comparing(keyExtractor).reversed();
            return this;
        }

        /**
         * Builds the Paginator.
         *
         * @return a new Paginator
         * @throws IllegalStateException if source is not set
         * @since 1.0.0
         */
        @NotNull
        public Paginator<T> build() {
            if (sourceSupplier == null) {
                throw new IllegalStateException("source or sourceSupplier must be set");
            }
            return new Paginator<>(this);
        }
    }

    // ==================== Instance Methods ====================

    /**
     * Gets a specific page.
     *
     * @param pageNumber the 1-based page number
     * @return the requested page
     * @since 1.0.0
     */
    @NotNull
    public Page<T> getPage(int pageNumber) {
        List<T> items = getProcessedItems();
        return paginate(items, pageNumber, pageSize);
    }

    /**
     * Gets the first page.
     *
     * @return the first page
     * @since 1.0.0
     */
    @NotNull
    public Page<T> firstPage() {
        return getPage(1);
    }

    /**
     * Gets the last page.
     *
     * @return the last page
     * @since 1.0.0
     */
    @NotNull
    public Page<T> lastPage() {
        List<T> items = getProcessedItems();
        int totalPages = totalPages(items.size(), pageSize);
        return getPage(totalPages);
    }

    /**
     * Gets the next page from the given page.
     *
     * @param currentPage the current page
     * @return the next page, or the current page if already on last
     * @since 1.0.0
     */
    @NotNull
    public Page<T> next(@NotNull Page<T> currentPage) {
        return getPage(currentPage.nextPageNumber());
    }

    /**
     * Gets the previous page from the given page.
     *
     * @param currentPage the current page
     * @return the previous page, or the current page if already on first
     * @since 1.0.0
     */
    @NotNull
    public Page<T> previous(@NotNull Page<T> currentPage) {
        return getPage(currentPage.previousPageNumber());
    }

    /**
     * Gets all pages as a list.
     *
     * @return a list of all pages
     * @since 1.0.0
     */
    @NotNull
    public List<Page<T>> getAllPages() {
        List<T> items = getProcessedItems();
        int total = totalPages(items.size(), pageSize);
        List<Page<T>> pages = new ArrayList<>(total);
        for (int i = 1; i <= total; i++) {
            pages.add(paginate(items, i, pageSize));
        }
        return pages;
    }

    /**
     * Gets the total number of items after filtering.
     *
     * @return the total item count
     * @since 1.0.0
     */
    public int getTotalItems() {
        return getProcessedItems().size();
    }

    /**
     * Gets the total number of pages.
     *
     * @return the total page count
     * @since 1.0.0
     */
    public int getTotalPages() {
        return totalPages(getTotalItems(), pageSize);
    }

    /**
     * Gets the configured page size.
     *
     * @return the page size
     * @since 1.0.0
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Checks if the source has any items after filtering.
     *
     * @return true if there are items
     * @since 1.0.0
     */
    public boolean hasItems() {
        return !getProcessedItems().isEmpty();
    }

    // ==================== Helper Methods ====================

    private List<T> getProcessedItems() {
        Collection<T> source = sourceSupplier.get();
        if (source == null) {
            return Collections.emptyList();
        }

        Stream<T> stream = source.stream();

        if (filter != null) {
            stream = stream.filter(filter);
        }

        if (comparator != null) {
            stream = stream.sorted(comparator);
        }

        return stream.toList();
    }

    @Override
    public String toString() {
        return "Paginator{" +
                "pageSize=" + pageSize +
                ", totalItems=" + getTotalItems() +
                ", totalPages=" + getTotalPages() +
                '}';
    }
}
