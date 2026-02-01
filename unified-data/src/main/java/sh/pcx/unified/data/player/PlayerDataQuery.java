/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Builder for querying player profiles from the database.
 *
 * <p>PlayerDataQuery provides a fluent API for constructing complex queries
 * against the player data store. Queries are executed asynchronously and
 * return matching player profiles.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Find top 10 VIP players by kills
 * playerData.query()
 *     .where(MyPlugin.VIP, true)
 *     .orderBy(MyPlugin.KILLS, Order.DESC)
 *     .limit(10)
 *     .execute()
 *     .thenAccept(results -> {
 *         for (PlayerProfile profile : results) {
 *             System.out.println(profile.getLastKnownName() + ": "
 *                 + profile.getData(MyPlugin.KILLS) + " kills");
 *         }
 *     });
 *
 * // Find players with balance > 1000
 * playerData.query()
 *     .where(MyPlugin.BALANCE, Comparator.GREATER_THAN, 1000.0)
 *     .execute();
 *
 * // Find players online in the last 24 hours
 * playerData.query()
 *     .onlineSince(Instant.now().minus(Duration.ofDays(1)))
 *     .execute();
 *
 * // Complex query with multiple conditions
 * playerData.query()
 *     .where(MyPlugin.LEVEL, Comparator.GREATER_THAN_OR_EQUAL, 50)
 *     .where(MyPlugin.RANK, "elite")
 *     .where(profile -> profile.getTotalPlayTime().toHours() > 100)
 *     .orderBy(MyPlugin.LEVEL, Order.DESC)
 *     .offset(20)
 *     .limit(10)
 *     .execute();
 * }</pre>
 *
 * <h2>Performance Notes</h2>
 * <ul>
 *   <li>Indexed keys ({@link PersistentDataKey#isIndexed()}) are queried efficiently</li>
 *   <li>Non-indexed keys require full table scans</li>
 *   <li>Use {@link #limit(int)} to prevent loading too many profiles</li>
 *   <li>Predicate filters are applied post-retrieval (less efficient)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerDataService#query()
 */
public interface PlayerDataQuery {

    /**
     * Adds an equality condition.
     *
     * @param key   the data key
     * @param value the value to match
     * @param <T>   the value type
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    <T> PlayerDataQuery where(@NotNull DataKey<T> key, @Nullable T value);

    /**
     * Adds a comparison condition.
     *
     * @param key        the data key
     * @param comparator the comparison operator
     * @param value      the value to compare against
     * @param <T>        the value type
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    <T extends Comparable<T>> PlayerDataQuery where(@NotNull DataKey<T> key,
                                                     @NotNull Comparator comparator,
                                                     @NotNull T value);

    /**
     * Adds a range condition (between min and max, inclusive).
     *
     * @param key the data key
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @param <T> the value type
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    <T extends Comparable<T>> PlayerDataQuery whereBetween(@NotNull DataKey<T> key,
                                                            @NotNull T min, @NotNull T max);

    /**
     * Adds a condition for values in a collection.
     *
     * @param key    the data key
     * @param values the collection of acceptable values
     * @param <T>    the value type
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    <T> PlayerDataQuery whereIn(@NotNull DataKey<T> key, @NotNull Collection<T> values);

    /**
     * Adds a null check condition.
     *
     * @param key the data key
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery whereNull(@NotNull DataKey<?> key);

    /**
     * Adds a not-null check condition.
     *
     * @param key the data key
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery whereNotNull(@NotNull DataKey<?> key);

    /**
     * Adds a custom predicate filter.
     *
     * <p>Note: Predicate filters are applied post-retrieval and may be less
     * efficient than key-based conditions.
     *
     * @param predicate the filter predicate
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery where(@NotNull Predicate<PlayerProfile> predicate);

    /**
     * Adds a condition for players online after the given time.
     *
     * @param timestamp the cutoff time
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery onlineSince(@NotNull java.time.Instant timestamp);

    /**
     * Adds a condition for players who first joined after the given time.
     *
     * @param timestamp the cutoff time
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery joinedAfter(@NotNull java.time.Instant timestamp);

    /**
     * Adds a condition for players who first joined before the given time.
     *
     * @param timestamp the cutoff time
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery joinedBefore(@NotNull java.time.Instant timestamp);

    /**
     * Adds a condition for currently online players only.
     *
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery onlineOnly();

    /**
     * Adds a condition for offline players only.
     *
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery offlineOnly();

    /**
     * Adds a search condition for player names.
     *
     * @param namePattern the name pattern (supports * and ? wildcards)
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery nameMatches(@NotNull String namePattern);

    /**
     * Sets the sort order.
     *
     * @param key   the data key to sort by
     * @param order the sort order
     * @param <T>   the value type
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    <T extends Comparable<T>> PlayerDataQuery orderBy(@NotNull DataKey<T> key,
                                                       @NotNull Order order);

    /**
     * Sets the sort order by play time.
     *
     * @param order the sort order
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery orderByPlayTime(@NotNull Order order);

    /**
     * Sets the sort order by first join time.
     *
     * @param order the sort order
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery orderByFirstJoin(@NotNull Order order);

    /**
     * Sets the sort order by last seen time.
     *
     * @param order the sort order
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery orderByLastSeen(@NotNull Order order);

    /**
     * Sets the number of results to skip.
     *
     * @param offset the number to skip
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery offset(int offset);

    /**
     * Sets the maximum number of results.
     *
     * @param limit the maximum results
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    PlayerDataQuery limit(int limit);

    /**
     * Executes the query and returns matching profiles.
     *
     * @return a future containing the matching profiles
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<PlayerProfile>> execute();

    /**
     * Executes the query and returns only the first result.
     *
     * @return a future containing the first matching profile, or empty
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<java.util.Optional<PlayerProfile>> findFirst();

    /**
     * Counts matching profiles without loading them.
     *
     * @return a future containing the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> count();

    /**
     * Checks if any profiles match the query.
     *
     * @return a future containing true if matches exist
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists();

    /**
     * Deletes all matching profiles.
     *
     * <p><b>Warning:</b> This is a destructive operation.
     *
     * @return a future containing the number of deleted profiles
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> delete();

    // ==================== Enums ====================

    /**
     * Comparison operators for query conditions.
     *
     * @since 1.0.0
     */
    enum Comparator {
        /**
         * Equal to.
         */
        EQUALS,

        /**
         * Not equal to.
         */
        NOT_EQUALS,

        /**
         * Greater than.
         */
        GREATER_THAN,

        /**
         * Greater than or equal to.
         */
        GREATER_THAN_OR_EQUAL,

        /**
         * Less than.
         */
        LESS_THAN,

        /**
         * Less than or equal to.
         */
        LESS_THAN_OR_EQUAL,

        /**
         * String contains.
         */
        CONTAINS,

        /**
         * String starts with.
         */
        STARTS_WITH,

        /**
         * String ends with.
         */
        ENDS_WITH,

        /**
         * Regex pattern match.
         */
        MATCHES
    }

    /**
     * Sort order for query results.
     *
     * @since 1.0.0
     */
    enum Order {
        /**
         * Ascending order (smallest first).
         */
        ASC,

        /**
         * Descending order (largest first).
         */
        DESC
    }
}
