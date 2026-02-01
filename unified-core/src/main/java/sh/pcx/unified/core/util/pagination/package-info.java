/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Pagination utilities for displaying large collections in pages.
 *
 * <p>This package provides classes for paginating collections of data,
 * which is essential for creating paginated GUIs, chat menus, and other
 * interfaces that display large amounts of data.
 *
 * <h2>Page</h2>
 * <p>Represents a single page of results:
 * <pre>{@code
 * Page<Player> page = Page.of(players, 1, 10, totalPlayers);
 * page.items();       // Items on this page
 * page.hasNext();     // Is there a next page?
 * page.totalPages();  // Total number of pages
 * }</pre>
 *
 * <h2>Paginator</h2>
 * <p>Utility for creating paginated views of data:
 * <pre>{@code
 * // Static utility
 * Page<String> page = Paginator.paginate(items, pageNumber, pageSize);
 *
 * // Builder pattern with filtering/sorting
 * Paginator<Player> paginator = Paginator.<Player>builder()
 *     .source(players)
 *     .pageSize(10)
 *     .filter(Player::isOnline)
 *     .sortBy(Player::getName)
 *     .build();
 * Page<Player> page = paginator.getPage(1);
 * }</pre>
 *
 * <h2>Navigation</h2>
 * <p>Pages provide convenient navigation methods:
 * <pre>{@code
 * if (page.hasNext()) {
 *     Page<Player> next = paginator.next(page);
 * }
 * if (page.hasPrevious()) {
 *     Page<Player> prev = paginator.previous(page);
 * }
 *
 * // Get range of page numbers for navigation buttons
 * List<Integer> pageNumbers = Paginator.pageRange(currentPage, totalPages, 5);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.core.util.pagination;
