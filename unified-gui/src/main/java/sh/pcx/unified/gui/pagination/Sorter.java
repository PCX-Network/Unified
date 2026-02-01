/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Functional interface for sorting items in paginated GUIs.
 *
 * <p>Sorter extends {@link Comparator} and provides additional factory
 * methods for creating common sorting implementations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Sort by name
 * Sorter<Player> byName = Sorter.comparing(Player::getName);
 *
 * // Sort by level descending
 * Sorter<Player> byLevel = Sorter.comparingInt(Player::getLevel).reversed();
 *
 * // Compound sort: by level, then by name
 * Sorter<Player> compound = Sorter.comparingInt(Player::getLevel)
 *     .thenComparing(Player::getName);
 *
 * // Apply to GUI
 * gui.setSorter(byName);
 * gui.setSortDirection(SortDirection.ASCENDING);
 * }</pre>
 *
 * @param <T> the type of items being sorted
 * @since 1.0.0
 * @author Supatuck
 * @see SortDirection
 * @see PaginatedGUI
 */
@FunctionalInterface
public interface Sorter<T> extends Comparator<T> {

    /**
     * Compares two items for ordering.
     *
     * @param o1 the first item
     * @param o2 the second item
     * @return negative if o1 < o2, zero if equal, positive if o1 > o2
     */
    @Override
    int compare(T o1, T o2);

    /**
     * Returns a sorter that reverses this sorter's order.
     *
     * @return the reversed sorter
     */
    @Override
    @NotNull
    default Sorter<T> reversed() {
        return (o1, o2) -> compare(o2, o1);
    }

    /**
     * Returns a sorter that uses this sorter, then the specified sorter for ties.
     *
     * @param other the secondary sorter
     * @return the compound sorter
     */
    @NotNull
    default Sorter<T> thenComparing(@NotNull Sorter<? super T> other) {
        Objects.requireNonNull(other);
        return (o1, o2) -> {
            int result = compare(o1, o2);
            return result != 0 ? result : other.compare(o1, o2);
        };
    }

    /**
     * Returns a sorter that uses this sorter, then compares by the key.
     *
     * @param keyExtractor the function to extract the comparable key
     * @param <U>          the type of the key
     * @return the compound sorter
     */
    @NotNull
    default <U extends Comparable<? super U>> Sorter<T> thenComparing(
            @NotNull Function<? super T, ? extends U> keyExtractor) {
        return thenComparing(comparing(keyExtractor));
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a sorter that compares by a comparable key.
     *
     * @param keyExtractor the function to extract the key
     * @param <T>          the item type
     * @param <U>          the key type
     * @return the new sorter
     */
    @NotNull
    static <T, U extends Comparable<? super U>> Sorter<T> comparing(
            @NotNull Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (o1, o2) -> {
            U k1 = keyExtractor.apply(o1);
            U k2 = keyExtractor.apply(o2);
            if (k1 == null && k2 == null) return 0;
            if (k1 == null) return -1;
            if (k2 == null) return 1;
            return k1.compareTo(k2);
        };
    }

    /**
     * Creates a sorter that compares by an int key.
     *
     * @param keyExtractor the function to extract the int key
     * @param <T>          the item type
     * @return the new sorter
     */
    @NotNull
    static <T> Sorter<T> comparingInt(@NotNull java.util.function.ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (o1, o2) -> Integer.compare(keyExtractor.applyAsInt(o1), keyExtractor.applyAsInt(o2));
    }

    /**
     * Creates a sorter that compares by a long key.
     *
     * @param keyExtractor the function to extract the long key
     * @param <T>          the item type
     * @return the new sorter
     */
    @NotNull
    static <T> Sorter<T> comparingLong(@NotNull java.util.function.ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (o1, o2) -> Long.compare(keyExtractor.applyAsLong(o1), keyExtractor.applyAsLong(o2));
    }

    /**
     * Creates a sorter that compares by a double key.
     *
     * @param keyExtractor the function to extract the double key
     * @param <T>          the item type
     * @return the new sorter
     */
    @NotNull
    static <T> Sorter<T> comparingDouble(@NotNull java.util.function.ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (o1, o2) -> Double.compare(keyExtractor.applyAsDouble(o1), keyExtractor.applyAsDouble(o2));
    }

    /**
     * Creates a sorter from a standard Comparator.
     *
     * @param comparator the comparator
     * @param <T>        the item type
     * @return the new sorter
     */
    @NotNull
    static <T> Sorter<T> from(@NotNull Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return comparator::compare;
    }

    /**
     * Returns a sorter that considers all items equal (natural order).
     *
     * @param <T> the item type
     * @return a no-op sorter
     */
    @NotNull
    static <T> Sorter<T> natural() {
        return (o1, o2) -> 0;
    }
}
