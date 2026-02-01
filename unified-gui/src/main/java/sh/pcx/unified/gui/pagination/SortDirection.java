/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;

/**
 * Enum representing the direction of sorting.
 *
 * <p>SortDirection is used with {@link Sorter} to determine whether
 * items should be sorted in ascending or descending order.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Set initial sort direction
 * gui.setSortDirection(SortDirection.ASCENDING);
 *
 * // Toggle direction
 * SortDirection current = gui.getSortDirection();
 * gui.setSortDirection(current.opposite());
 *
 * // Or use the toggle method
 * gui.toggleSortDirection();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Sorter
 * @see PaginatedGUI
 */
public enum SortDirection {

    /**
     * Ascending order (A-Z, 0-9, oldest first).
     */
    ASCENDING,

    /**
     * Descending order (Z-A, 9-0, newest first).
     */
    DESCENDING;

    /**
     * Returns the opposite direction.
     *
     * @return DESCENDING if currently ASCENDING, ASCENDING if currently DESCENDING
     */
    @NotNull
    public SortDirection opposite() {
        return this == ASCENDING ? DESCENDING : ASCENDING;
    }

    /**
     * Checks if this is ascending order.
     *
     * @return true if ascending
     */
    public boolean isAscending() {
        return this == ASCENDING;
    }

    /**
     * Checks if this is descending order.
     *
     * @return true if descending
     */
    public boolean isDescending() {
        return this == DESCENDING;
    }
}
