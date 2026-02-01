/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent builder for creating complex {@link Filter} instances.
 *
 * <p>FilterBuilder provides a chainable API for constructing filters with
 * multiple conditions, logical operators, and metadata. It supports building
 * both AND and OR combinations of conditions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Build a complex filter
 * Filter<Player> filter = FilterBuilder.<Player>create()
 *     .name("Active VIP Players")
 *     .description("Online VIP players with high level")
 *     .where(Player::isOnline)
 *     .and(p -> p.hasPermission("vip"))
 *     .and(p -> p.getLevel() >= 50)
 *     .build();
 *
 * // Using property methods
 * Filter<ShopItem> itemFilter = FilterBuilder.<ShopItem>create()
 *     .name("Affordable Weapons")
 *     .whereEquals(ShopItem::getCategory, "weapon")
 *     .and(item -> item.getPrice() <= 1000)
 *     .build();
 *
 * // Combining multiple filters
 * Filter<Quest> questFilter = FilterBuilder.<Quest>create()
 *     .anyOf(
 *         q -> q.isCompleted(),
 *         q -> q.getProgress() >= 90,
 *         q -> q.isEpic()
 *     )
 *     .named("Near Completion")
 *     .build();
 *
 * // Range-based filtering
 * Filter<Item> priceFilter = FilterBuilder.<Item>create()
 *     .whereInRange(Item::getPrice, 100, 500)
 *     .and(item -> item.isAvailable())
 *     .build();
 *
 * // Negation
 * Filter<Player> nonOpFilter = FilterBuilder.<Player>create()
 *     .whereNot(Player::isOp)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>FilterBuilder is NOT thread-safe. Create a new builder for each
 * filter construction. The resulting Filter is thread-safe.
 *
 * @param <T> the type of items to filter
 * @since 1.0.0
 * @author Supatuck
 * @see Filter
 * @see SearchFilter
 */
public final class FilterBuilder<T> {

    private final List<Predicate<T>> conditions;
    private boolean useOrLogic;
    private String name;
    private String description;

    /**
     * Private constructor.
     */
    private FilterBuilder() {
        this.conditions = new ArrayList<>();
        this.useOrLogic = false;
    }

    /**
     * Creates a new FilterBuilder.
     *
     * @param <T> the item type
     * @return a new FilterBuilder instance
     * @since 1.0.0
     */
    @NotNull
    public static <T> FilterBuilder<T> create() {
        return new FilterBuilder<>();
    }

    /**
     * Creates a FilterBuilder starting with an existing filter.
     *
     * @param <T>    the item type
     * @param filter the initial filter
     * @return a new FilterBuilder with the filter as the first condition
     * @since 1.0.0
     */
    @NotNull
    public static <T> FilterBuilder<T> from(@NotNull Filter<T> filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        FilterBuilder<T> builder = new FilterBuilder<>();
        builder.conditions.add(filter::test);
        builder.name = filter.getDisplayName();
        builder.description = filter.getDescription();
        return builder;
    }

    // ==================== Metadata ====================

    /**
     * Sets the display name for the filter.
     *
     * @param name the display name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> name(@NotNull String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        return this;
    }

    /**
     * Sets the description for the filter.
     *
     * @param description the description
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> description(@Nullable String description) {
        this.description = description;
        return this;
    }

    // ==================== Basic Conditions ====================

    /**
     * Adds a condition to the filter.
     *
     * @param predicate the condition to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> where(@NotNull Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        conditions.add(predicate);
        return this;
    }

    /**
     * Adds a condition based on a boolean property.
     *
     * @param property the property getter
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereTrue(@NotNull Function<T, Boolean> property) {
        Objects.requireNonNull(property, "property cannot be null");
        return where(item -> {
            Boolean value = property.apply(item);
            return value != null && value;
        });
    }

    /**
     * Adds a negated condition based on a boolean property.
     *
     * @param property the property getter
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereFalse(@NotNull Function<T, Boolean> property) {
        Objects.requireNonNull(property, "property cannot be null");
        return where(item -> {
            Boolean value = property.apply(item);
            return value == null || !value;
        });
    }

    /**
     * Adds a negated condition.
     *
     * @param predicate the condition to negate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereNot(@NotNull Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return where(predicate.negate());
    }

    // ==================== Equality Conditions ====================

    /**
     * Adds a condition checking for equality.
     *
     * @param <V>      the value type
     * @param getter   the property getter
     * @param expected the expected value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <V> FilterBuilder<T> whereEquals(@NotNull Function<T, V> getter,
                                             @Nullable V expected) {
        Objects.requireNonNull(getter, "getter cannot be null");
        return where(item -> Objects.equals(getter.apply(item), expected));
    }

    /**
     * Adds a condition checking for inequality.
     *
     * @param <V>      the value type
     * @param getter   the property getter
     * @param excluded the value to exclude
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <V> FilterBuilder<T> whereNotEquals(@NotNull Function<T, V> getter,
                                                @Nullable V excluded) {
        Objects.requireNonNull(getter, "getter cannot be null");
        return where(item -> !Objects.equals(getter.apply(item), excluded));
    }

    /**
     * Adds a condition checking if a value is in a collection.
     *
     * @param <V>       the value type
     * @param getter    the property getter
     * @param allowedValues the allowed values
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <V> FilterBuilder<T> whereIn(@NotNull Function<T, V> getter,
                                         @NotNull Collection<V> allowedValues) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(allowedValues, "allowedValues cannot be null");
        return where(item -> allowedValues.contains(getter.apply(item)));
    }

    /**
     * Adds a condition checking if a value is NOT in a collection.
     *
     * @param <V>            the value type
     * @param getter         the property getter
     * @param excludedValues the excluded values
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <V> FilterBuilder<T> whereNotIn(@NotNull Function<T, V> getter,
                                            @NotNull Collection<V> excludedValues) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(excludedValues, "excludedValues cannot be null");
        return where(item -> !excludedValues.contains(getter.apply(item)));
    }

    // ==================== Null Conditions ====================

    /**
     * Adds a condition checking for null values.
     *
     * @param getter the property getter
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereNull(@NotNull Function<T, ?> getter) {
        Objects.requireNonNull(getter, "getter cannot be null");
        return where(item -> getter.apply(item) == null);
    }

    /**
     * Adds a condition checking for non-null values.
     *
     * @param getter the property getter
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereNotNull(@NotNull Function<T, ?> getter) {
        Objects.requireNonNull(getter, "getter cannot be null");
        return where(item -> getter.apply(item) != null);
    }

    // ==================== Range Conditions ====================

    /**
     * Adds a condition for values within a range.
     *
     * @param <N>    the numeric type
     * @param getter the property getter
     * @param min    the minimum value (inclusive)
     * @param max    the maximum value (inclusive)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <N extends Number & Comparable<N>> FilterBuilder<T> whereInRange(
            @NotNull Function<T, N> getter,
            @NotNull N min,
            @NotNull N max) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(min, "min cannot be null");
        Objects.requireNonNull(max, "max cannot be null");
        return where(item -> {
            N value = getter.apply(item);
            if (value == null) return false;
            return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
        });
    }

    /**
     * Adds a condition for values greater than a minimum.
     *
     * @param <N>    the numeric type
     * @param getter the property getter
     * @param min    the minimum value (exclusive)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <N extends Number & Comparable<N>> FilterBuilder<T> whereGreaterThan(
            @NotNull Function<T, N> getter,
            @NotNull N min) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(min, "min cannot be null");
        return where(item -> {
            N value = getter.apply(item);
            return value != null && value.compareTo(min) > 0;
        });
    }

    /**
     * Adds a condition for values less than a maximum.
     *
     * @param <N>    the numeric type
     * @param getter the property getter
     * @param max    the maximum value (exclusive)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <N extends Number & Comparable<N>> FilterBuilder<T> whereLessThan(
            @NotNull Function<T, N> getter,
            @NotNull N max) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(max, "max cannot be null");
        return where(item -> {
            N value = getter.apply(item);
            return value != null && value.compareTo(max) < 0;
        });
    }

    // ==================== String Conditions ====================

    /**
     * Adds a condition for string containment (case-insensitive).
     *
     * @param getter    the string property getter
     * @param substring the substring to search for
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereContains(@NotNull Function<T, String> getter,
                                           @NotNull String substring) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(substring, "substring cannot be null");
        String lower = substring.toLowerCase();
        return where(item -> {
            String value = getter.apply(item);
            return value != null && value.toLowerCase().contains(lower);
        });
    }

    /**
     * Adds a condition for string prefix (case-insensitive).
     *
     * @param getter the string property getter
     * @param prefix the prefix to check
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereStartsWith(@NotNull Function<T, String> getter,
                                             @NotNull String prefix) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(prefix, "prefix cannot be null");
        String lower = prefix.toLowerCase();
        return where(item -> {
            String value = getter.apply(item);
            return value != null && value.toLowerCase().startsWith(lower);
        });
    }

    /**
     * Adds a condition for string suffix (case-insensitive).
     *
     * @param getter the string property getter
     * @param suffix the suffix to check
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereEndsWith(@NotNull Function<T, String> getter,
                                           @NotNull String suffix) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(suffix, "suffix cannot be null");
        String lower = suffix.toLowerCase();
        return where(item -> {
            String value = getter.apply(item);
            return value != null && value.toLowerCase().endsWith(lower);
        });
    }

    /**
     * Adds a condition for regex matching.
     *
     * @param getter  the string property getter
     * @param pattern the regex pattern
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> whereMatches(@NotNull Function<T, String> getter,
                                          @NotNull String pattern) {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(pattern, "pattern cannot be null");
        java.util.regex.Pattern compiled = java.util.regex.Pattern.compile(pattern);
        return where(item -> {
            String value = getter.apply(item);
            return value != null && compiled.matcher(value).matches();
        });
    }

    // ==================== Logical Operators ====================

    /**
     * Adds an AND condition (same as {@link #where}).
     *
     * @param predicate the condition to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> and(@NotNull Predicate<T> predicate) {
        return where(predicate);
    }

    /**
     * Configures the builder to use OR logic between conditions.
     *
     * <p>When OR logic is used, items match if ANY condition is true.
     * Default is AND logic where ALL conditions must be true.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public FilterBuilder<T> useOrLogic() {
        this.useOrLogic = true;
        return this;
    }

    /**
     * Adds multiple conditions where ANY must match (OR logic).
     *
     * @param predicates the conditions to add
     * @return this builder
     * @since 1.0.0
     */
    @SafeVarargs
    @NotNull
    public final FilterBuilder<T> anyOf(@NotNull Predicate<T>... predicates) {
        if (predicates.length == 0) {
            return this;
        }

        return where(item -> {
            for (Predicate<T> predicate : predicates) {
                if (predicate.test(item)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Adds multiple conditions where ALL must match (AND logic).
     *
     * @param predicates the conditions to add
     * @return this builder
     * @since 1.0.0
     */
    @SafeVarargs
    @NotNull
    public final FilterBuilder<T> allOf(@NotNull Predicate<T>... predicates) {
        if (predicates.length == 0) {
            return this;
        }

        return where(item -> {
            for (Predicate<T> predicate : predicates) {
                if (!predicate.test(item)) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Adds multiple conditions where NONE must match.
     *
     * @param predicates the conditions that should not match
     * @return this builder
     * @since 1.0.0
     */
    @SafeVarargs
    @NotNull
    public final FilterBuilder<T> noneOf(@NotNull Predicate<T>... predicates) {
        if (predicates.length == 0) {
            return this;
        }

        return where(item -> {
            for (Predicate<T> predicate : predicates) {
                if (predicate.test(item)) {
                    return false;
                }
            }
            return true;
        });
    }

    // ==================== Building ====================

    /**
     * Builds the filter.
     *
     * @return the constructed Filter
     * @since 1.0.0
     */
    @NotNull
    public Filter<T> build() {
        if (conditions.isEmpty()) {
            return Filter.all();
        }

        Predicate<T> combined;
        if (useOrLogic) {
            combined = item -> {
                for (Predicate<T> condition : conditions) {
                    if (condition.test(item)) {
                        return true;
                    }
                }
                return false;
            };
        } else {
            combined = item -> {
                for (Predicate<T> condition : conditions) {
                    if (!condition.test(item)) {
                        return false;
                    }
                }
                return true;
            };
        }

        return Filter.of(combined, name, description);
    }

    /**
     * Builds the filter with a specific name.
     *
     * @param name the display name
     * @return the constructed Filter
     * @since 1.0.0
     */
    @NotNull
    public Filter<T> named(@NotNull String name) {
        return name(name).build();
    }
}
