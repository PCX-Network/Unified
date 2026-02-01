/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;

/**
 * Strategy interface for merging two values into one during conflict resolution.
 *
 * <p>MergeStrategy is used with {@link ConflictResolver} when conflicts need
 * to be resolved by combining data from both the existing and incoming values,
 * rather than simply choosing one.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Custom merge strategy for PlayerStats
 * MergeStrategy<PlayerStats> statsMerge = (existing, incoming) -> {
 *     return new PlayerStats(
 *         Math.max(existing.getKills(), incoming.getKills()),
 *         Math.max(existing.getDeaths(), incoming.getDeaths()),
 *         existing.getPlayTime() + incoming.getPlayTime()
 *     );
 * };
 *
 * // Use with conflict resolver
 * ConflictResolver<PlayerStats> resolver = ConflictResolver.withMerge(statsMerge);
 *
 * // Built-in merge strategies
 * MergeStrategy<Set<String>> setUnion = MergeStrategy.setUnion();
 * MergeStrategy<Map<String, Integer>> mapMerge = MergeStrategy.mapMerge(Integer::max);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as merging may occur from multiple
 * threads concurrently.
 *
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see ConflictResolver
 */
@FunctionalInterface
public interface MergeStrategy<V> {

    /**
     * Merges two values into a single result.
     *
     * <p>Both values are guaranteed to be non-null. The implementation should
     * return a new value that appropriately combines data from both inputs.
     *
     * @param existing the current cached value
     * @param incoming the new value being written
     * @return the merged result
     * @since 1.0.0
     */
    @NotNull
    V merge(@NotNull V existing, @NotNull V incoming);

    /**
     * Creates a merge strategy that always returns the incoming value.
     *
     * <p>Equivalent to "last write wins" - no actual merging occurs.
     *
     * @param <V> the value type
     * @return a strategy that returns incoming values
     * @since 1.0.0
     */
    @NotNull
    static <V> MergeStrategy<V> replaceWith() {
        return (existing, incoming) -> incoming;
    }

    /**
     * Creates a merge strategy that always returns the existing value.
     *
     * <p>Equivalent to "first write wins" - ignores incoming values.
     *
     * @param <V> the value type
     * @return a strategy that returns existing values
     * @since 1.0.0
     */
    @NotNull
    static <V> MergeStrategy<V> keepExisting() {
        return (existing, incoming) -> existing;
    }

    /**
     * Creates a merge strategy for sets that performs a union.
     *
     * @param <E> the element type
     * @return a strategy that unions two sets
     * @since 1.0.0
     */
    @NotNull
    static <E> MergeStrategy<Set<E>> setUnion() {
        return (existing, incoming) -> {
            Set<E> result = new HashSet<>(existing);
            result.addAll(incoming);
            return result;
        };
    }

    /**
     * Creates a merge strategy for sets that performs an intersection.
     *
     * @param <E> the element type
     * @return a strategy that intersects two sets
     * @since 1.0.0
     */
    @NotNull
    static <E> MergeStrategy<Set<E>> setIntersection() {
        return (existing, incoming) -> {
            Set<E> result = new HashSet<>(existing);
            result.retainAll(incoming);
            return result;
        };
    }

    /**
     * Creates a merge strategy for collections that concatenates them.
     *
     * @param <E> the element type
     * @param <C> the collection type
     * @param factory a function to create the result collection
     * @return a strategy that concatenates collections
     * @since 1.0.0
     */
    @NotNull
    static <E, C extends Collection<E>> MergeStrategy<C> collectionConcat(
            @NotNull java.util.function.Supplier<C> factory) {
        return (existing, incoming) -> {
            C result = factory.get();
            result.addAll(existing);
            result.addAll(incoming);
            return result;
        };
    }

    /**
     * Creates a merge strategy for maps that merges entries.
     *
     * <p>When both maps contain the same key, the valueMerger is used
     * to combine the values.
     *
     * @param <K>         the key type
     * @param <V>         the value type
     * @param valueMerger function to merge values for the same key
     * @return a strategy that merges maps
     * @since 1.0.0
     */
    @NotNull
    static <K, V> MergeStrategy<Map<K, V>> mapMerge(@NotNull BinaryOperator<V> valueMerger) {
        return (existing, incoming) -> {
            Map<K, V> result = new HashMap<>(existing);
            incoming.forEach((key, value) ->
                    result.merge(key, value, valueMerger)
            );
            return result;
        };
    }

    /**
     * Creates a merge strategy for maps that prefers incoming values.
     *
     * <p>When both maps contain the same key, the incoming value is used.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a strategy that merges maps preferring incoming values
     * @since 1.0.0
     */
    @NotNull
    static <K, V> MergeStrategy<Map<K, V>> mapMergeIncoming() {
        return (existing, incoming) -> {
            Map<K, V> result = new HashMap<>(existing);
            result.putAll(incoming);
            return result;
        };
    }

    /**
     * Creates a merge strategy for maps that prefers existing values.
     *
     * <p>When both maps contain the same key, the existing value is kept.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a strategy that merges maps preferring existing values
     * @since 1.0.0
     */
    @NotNull
    static <K, V> MergeStrategy<Map<K, V>> mapMergeExisting() {
        return (existing, incoming) -> {
            Map<K, V> result = new HashMap<>(incoming);
            result.putAll(existing);
            return result;
        };
    }

    /**
     * Creates a merge strategy for numeric values using maximum.
     *
     * @return a strategy that returns the maximum value
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Long> maxLong() {
        return Math::max;
    }

    /**
     * Creates a merge strategy for numeric values using minimum.
     *
     * @return a strategy that returns the minimum value
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Long> minLong() {
        return Math::min;
    }

    /**
     * Creates a merge strategy for numeric values using sum.
     *
     * @return a strategy that returns the sum
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Long> sumLong() {
        return Long::sum;
    }

    /**
     * Creates a merge strategy for numeric values using maximum.
     *
     * @return a strategy that returns the maximum value
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Integer> maxInt() {
        return Math::max;
    }

    /**
     * Creates a merge strategy for numeric values using minimum.
     *
     * @return a strategy that returns the minimum value
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Integer> minInt() {
        return Math::min;
    }

    /**
     * Creates a merge strategy for numeric values using sum.
     *
     * @return a strategy that returns the sum
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Integer> sumInt() {
        return Integer::sum;
    }

    /**
     * Creates a merge strategy for numeric values using maximum.
     *
     * @return a strategy that returns the maximum value
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Double> maxDouble() {
        return Math::max;
    }

    /**
     * Creates a merge strategy for numeric values using minimum.
     *
     * @return a strategy that returns the minimum value
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Double> minDouble() {
        return Math::min;
    }

    /**
     * Creates a merge strategy for numeric values using sum.
     *
     * @return a strategy that returns the sum
     * @since 1.0.0
     */
    @NotNull
    static MergeStrategy<Double> sumDouble() {
        return Double::sum;
    }

    /**
     * Chains this strategy with another, applying both in sequence.
     *
     * <p>First this strategy is applied, then the result is passed
     * as "existing" to the next strategy along with the original incoming.
     *
     * @param next the next strategy to apply
     * @return a chained merge strategy
     * @since 1.0.0
     */
    @NotNull
    default MergeStrategy<V> andThen(@NotNull MergeStrategy<V> next) {
        return (existing, incoming) -> {
            V intermediate = this.merge(existing, incoming);
            return next.merge(intermediate, incoming);
        };
    }
}
