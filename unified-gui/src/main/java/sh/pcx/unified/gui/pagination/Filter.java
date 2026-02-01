/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface for filtering items in a paginated GUI.
 *
 * <p>Filter provides a type-safe, composable way to filter items based on
 * various criteria. Unlike raw Predicates, Filters can carry additional
 * metadata like display names and descriptions for UI integration.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple predicate-based filter
 * Filter<Player> onlineFilter = Filter.of(Player::isOnline, "Online Only");
 *
 * // Property-based filter
 * Filter<Player> opFilter = Filter.property(Player::isOp);
 *
 * // Range filter
 * Filter<Player> levelFilter = Filter.range(
 *     Player::getLevel,
 *     10, 50,
 *     "Level 10-50"
 * );
 *
 * // Composed filters
 * Filter<Player> combined = onlineFilter
 *     .and(opFilter)
 *     .and(levelFilter);
 *
 * // Negated filter
 * Filter<Player> notOp = opFilter.negate();
 *
 * // Apply filter
 * boolean matches = combined.test(player);
 * List<Player> filtered = players.stream()
 *     .filter(combined::test)
 *     .toList();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Filter implementations should be thread-safe. Avoid mutable state
 * within filter predicates.
 *
 * @param <T> the type of items to filter
 * @since 1.0.0
 * @author Supatuck
 * @see FilterBuilder
 * @see SearchFilter
 * @see PaginatedGUI
 */
@FunctionalInterface
public interface Filter<T> {

    /**
     * Tests whether the specified item matches this filter.
     *
     * @param item the item to test
     * @return true if the item matches the filter criteria
     * @since 1.0.0
     */
    boolean test(@NotNull T item);

    /**
     * Returns a display name for this filter.
     *
     * <p>Used for UI display purposes. Default implementation returns null.
     *
     * @return the display name, or null if not set
     * @since 1.0.0
     */
    @Nullable
    default String getDisplayName() {
        return null;
    }

    /**
     * Returns a description of this filter.
     *
     * <p>Used for tooltips and help text. Default implementation returns null.
     *
     * @return the description, or null if not set
     * @since 1.0.0
     */
    @Nullable
    default String getDescription() {
        return null;
    }

    /**
     * Creates a filter that accepts all items (no filtering).
     *
     * @param <T> the item type
     * @return a filter that accepts all items
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> all() {
        return new NamedFilter<>(item -> true, "All", "Shows all items");
    }

    /**
     * Creates a filter that rejects all items.
     *
     * @param <T> the item type
     * @return a filter that rejects all items
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> none() {
        return new NamedFilter<>(item -> false, "None", "Hides all items");
    }

    /**
     * Creates a filter from a predicate.
     *
     * @param <T>       the item type
     * @param predicate the predicate to use
     * @return a Filter wrapping the predicate
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> of(@NotNull Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return predicate::test;
    }

    /**
     * Creates a named filter from a predicate.
     *
     * @param <T>       the item type
     * @param predicate the predicate to use
     * @param name      the display name
     * @return a named Filter wrapping the predicate
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> of(@NotNull Predicate<T> predicate, @NotNull String name) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        return new NamedFilter<>(predicate, name, null);
    }

    /**
     * Creates a named filter from a predicate with description.
     *
     * @param <T>         the item type
     * @param predicate   the predicate to use
     * @param name        the display name
     * @param description the description
     * @return a named Filter wrapping the predicate
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> of(@NotNull Predicate<T> predicate,
                            @NotNull String name,
                            @Nullable String description) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        return new NamedFilter<>(predicate, name, description);
    }

    /**
     * Creates a filter based on a boolean property.
     *
     * @param <T>      the item type
     * @param property the property getter
     * @return a filter that accepts items where the property is true
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> property(@NotNull Function<T, Boolean> property) {
        Objects.requireNonNull(property, "property cannot be null");
        return item -> {
            Boolean value = property.apply(item);
            return value != null && value;
        };
    }

    /**
     * Creates a filter that matches a specific value.
     *
     * @param <T>      the item type
     * @param <V>      the value type
     * @param getter   the property getter
     * @param expected the expected value
     * @return a filter that matches items with the expected value
     * @since 1.0.0
     */
    @NotNull
    static <T, V> Filter<T> equals(@NotNull Function<T, V> getter, @Nullable V expected) {
        Objects.requireNonNull(getter, "getter cannot be null");
        return item -> Objects.equals(getter.apply(item), expected);
    }

    /**
     * Creates a filter for numeric values within a range.
     *
     * @param <T>    the item type
     * @param <N>    the numeric type
     * @param getter the property getter
     * @param min    the minimum value (inclusive)
     * @param max    the maximum value (inclusive)
     * @return a filter for values in the range
     * @since 1.0.0
     */
    @NotNull
    static <T, N extends Number & Comparable<N>> Filter<T> range(
            @NotNull Function<T, N> getter,
            @NotNull N min,
            @NotNull N max) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(min, "min cannot be null");
        Objects.requireNonNull(max, "max cannot be null");
        return item -> {
            N value = getter.apply(item);
            if (value == null) return false;
            return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
        };
    }

    /**
     * Creates a named range filter.
     *
     * @param <T>    the item type
     * @param <N>    the numeric type
     * @param getter the property getter
     * @param min    the minimum value (inclusive)
     * @param max    the maximum value (inclusive)
     * @param name   the display name
     * @return a named filter for values in the range
     * @since 1.0.0
     */
    @NotNull
    static <T, N extends Number & Comparable<N>> Filter<T> range(
            @NotNull Function<T, N> getter,
            @NotNull N min,
            @NotNull N max,
            @NotNull String name) {
        Filter<T> rangeFilter = range(getter, min, max);
        return new NamedFilter<>(rangeFilter::test, name,
                String.format("Values between %s and %s", min, max));
    }

    /**
     * Creates a filter for values greater than a minimum.
     *
     * @param <T>    the item type
     * @param <N>    the numeric type
     * @param getter the property getter
     * @param min    the minimum value (exclusive)
     * @return a filter for values greater than min
     * @since 1.0.0
     */
    @NotNull
    static <T, N extends Number & Comparable<N>> Filter<T> greaterThan(
            @NotNull Function<T, N> getter,
            @NotNull N min) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(min, "min cannot be null");
        return item -> {
            N value = getter.apply(item);
            return value != null && value.compareTo(min) > 0;
        };
    }

    /**
     * Creates a filter for values less than a maximum.
     *
     * @param <T>    the item type
     * @param <N>    the numeric type
     * @param getter the property getter
     * @param max    the maximum value (exclusive)
     * @return a filter for values less than max
     * @since 1.0.0
     */
    @NotNull
    static <T, N extends Number & Comparable<N>> Filter<T> lessThan(
            @NotNull Function<T, N> getter,
            @NotNull N max) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(max, "max cannot be null");
        return item -> {
            N value = getter.apply(item);
            return value != null && value.compareTo(max) < 0;
        };
    }

    /**
     * Creates a filter that checks if a string property contains a substring.
     *
     * @param <T>       the item type
     * @param getter    the string property getter
     * @param substring the substring to search for
     * @return a filter for items containing the substring
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> contains(@NotNull Function<T, String> getter,
                                   @NotNull String substring) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(substring, "substring cannot be null");
        String lowerSubstring = substring.toLowerCase();
        return item -> {
            String value = getter.apply(item);
            return value != null && value.toLowerCase().contains(lowerSubstring);
        };
    }

    /**
     * Creates a filter that checks if a value is not null.
     *
     * @param <T>    the item type
     * @param getter the property getter
     * @return a filter for items with non-null values
     * @since 1.0.0
     */
    @NotNull
    static <T> Filter<T> notNull(@NotNull Function<T, ?> getter) {
        Objects.requireNonNull(getter, "getter cannot be null");
        return item -> getter.apply(item) != null;
    }

    // ==================== Composition Methods ====================

    /**
     * Creates a filter that is the logical AND of this filter and another.
     *
     * @param other the other filter
     * @return a combined filter requiring both conditions
     * @since 1.0.0
     */
    @NotNull
    default Filter<T> and(@NotNull Filter<T> other) {
        Objects.requireNonNull(other, "other cannot be null");
        Filter<T> self = this;
        return new NamedFilter<>(
                item -> self.test(item) && other.test(item),
                combineNames(self.getDisplayName(), other.getDisplayName(), " AND "),
                null
        );
    }

    /**
     * Creates a filter that is the logical OR of this filter and another.
     *
     * @param other the other filter
     * @return a combined filter accepting either condition
     * @since 1.0.0
     */
    @NotNull
    default Filter<T> or(@NotNull Filter<T> other) {
        Objects.requireNonNull(other, "other cannot be null");
        Filter<T> self = this;
        return new NamedFilter<>(
                item -> self.test(item) || other.test(item),
                combineNames(self.getDisplayName(), other.getDisplayName(), " OR "),
                null
        );
    }

    /**
     * Creates a filter that is the logical negation of this filter.
     *
     * @return a negated filter
     * @since 1.0.0
     */
    @NotNull
    default Filter<T> negate() {
        Filter<T> self = this;
        String name = self.getDisplayName();
        return new NamedFilter<>(
                item -> !self.test(item),
                name != null ? "NOT " + name : null,
                null
        );
    }

    /**
     * Creates a named version of this filter.
     *
     * @param name the display name
     * @return a named filter
     * @since 1.0.0
     */
    @NotNull
    default Filter<T> named(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new NamedFilter<>(this::test, name, this.getDescription());
    }

    /**
     * Creates a described version of this filter.
     *
     * @param name        the display name
     * @param description the description
     * @return a described filter
     * @since 1.0.0
     */
    @NotNull
    default Filter<T> described(@NotNull String name, @Nullable String description) {
        Objects.requireNonNull(name, "name cannot be null");
        return new NamedFilter<>(this::test, name, description);
    }

    /**
     * Converts this filter to a standard Predicate.
     *
     * @return a Predicate view of this filter
     * @since 1.0.0
     */
    @NotNull
    default Predicate<T> toPredicate() {
        return this::test;
    }

    /**
     * Helper method to combine filter names.
     */
    private static String combineNames(String name1, String name2, String separator) {
        if (name1 == null && name2 == null) return null;
        if (name1 == null) return name2;
        if (name2 == null) return name1;
        return name1 + separator + name2;
    }
}

/**
 * Internal implementation of a named filter.
 */
final class NamedFilter<T> implements Filter<T> {

    private final Predicate<T> predicate;
    private final String name;
    private final String description;

    NamedFilter(@NotNull Predicate<T> predicate, @Nullable String name, @Nullable String description) {
        this.predicate = predicate;
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean test(@NotNull T item) {
        return predicate.test(item);
    }

    @Override
    @Nullable
    public String getDisplayName() {
        return name;
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name != null ? "Filter[" + name + "]" : "Filter[anonymous]";
    }
}
