/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Fluent query builder for MongoDB find operations.
 *
 * <p>This builder provides a type-safe, fluent API for constructing
 * MongoDB queries with filters, sorting, pagination, and projections.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple query
 * mongoService.query("players")
 *     .filter(Filters.eq("name", "Steve"))
 *     .execute()
 *     .thenAccept(players -> {
 *         players.forEach(p -> logger.info("Found: " + p.toJson()));
 *     });
 *
 * // Complex query with multiple conditions
 * mongoService.query("players", PlayerData.class)
 *     .eq("active", true)
 *     .gte("level", 10)
 *     .lt("level", 50)
 *     .in("rank", Arrays.asList("gold", "platinum", "diamond"))
 *     .sortDesc("level")
 *     .skip(20)
 *     .limit(10)
 *     .execute()
 *     .thenAccept(players -> {
 *         logger.info("Found " + players.size() + " players");
 *     });
 *
 * // Query with projections
 * mongoService.query("players")
 *     .eq("vip", true)
 *     .include("name", "balance", "rank")
 *     .sortAsc("name")
 *     .execute()
 *     .thenAccept(players -> {
 *         // Documents only contain name, balance, rank, and _id
 *     });
 *
 * // Query for a single result
 * mongoService.query("players")
 *     .eq("uuid", uuid.toString())
 *     .first()
 *     .thenAccept(playerOpt -> {
 *         playerOpt.ifPresent(p -> logger.info("Balance: " + p.get("balance")));
 *     });
 *
 * // Count matching documents
 * mongoService.query("players")
 *     .eq("vip", true)
 *     .count()
 *     .thenAccept(count -> logger.info("VIP count: " + count));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This builder is NOT thread-safe. Create a new query for each operation.
 *
 * @param <T> the document type
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoService#query(String)
 * @see MongoService#query(String, Class)
 */
public class MongoQuery<T> {

    private final MongoCollectionWrapper<T> collection;
    private final List<Bson> filters = new ArrayList<>();
    private Bson sort;
    private Bson projection;
    private int skipCount = 0;
    private int limitCount = 0;

    /**
     * Creates a new query builder for the given collection.
     *
     * @param collection the collection wrapper
     * @since 1.0.0
     */
    public MongoQuery(@NotNull MongoCollectionWrapper<T> collection) {
        this.collection = Objects.requireNonNull(collection, "Collection cannot be null");
    }

    // ===========================================
    // Filter Methods
    // ===========================================

    /**
     * Adds a raw BSON filter.
     *
     * @param filter the filter to add
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> filter(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        filters.add(filter);
        return this;
    }

    /**
     * Adds multiple raw BSON filters (AND condition).
     *
     * @param filters the filters to add
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> filters(@NotNull Bson... filters) {
        Objects.requireNonNull(filters, "Filters cannot be null");
        this.filters.addAll(Arrays.asList(filters));
        return this;
    }

    /**
     * Adds an equality filter.
     *
     * @param field the field name
     * @param value the expected value
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> eq(@NotNull String field, @Nullable Object value) {
        filters.add(Filters.eq(field, value));
        return this;
    }

    /**
     * Adds a not-equal filter.
     *
     * @param field the field name
     * @param value the value to exclude
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> ne(@NotNull String field, @Nullable Object value) {
        filters.add(Filters.ne(field, value));
        return this;
    }

    /**
     * Adds a greater-than filter.
     *
     * @param field the field name
     * @param value the minimum value (exclusive)
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> gt(@NotNull String field, @NotNull Object value) {
        filters.add(Filters.gt(field, value));
        return this;
    }

    /**
     * Adds a greater-than-or-equal filter.
     *
     * @param field the field name
     * @param value the minimum value (inclusive)
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> gte(@NotNull String field, @NotNull Object value) {
        filters.add(Filters.gte(field, value));
        return this;
    }

    /**
     * Adds a less-than filter.
     *
     * @param field the field name
     * @param value the maximum value (exclusive)
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> lt(@NotNull String field, @NotNull Object value) {
        filters.add(Filters.lt(field, value));
        return this;
    }

    /**
     * Adds a less-than-or-equal filter.
     *
     * @param field the field name
     * @param value the maximum value (inclusive)
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> lte(@NotNull String field, @NotNull Object value) {
        filters.add(Filters.lte(field, value));
        return this;
    }

    /**
     * Adds a range filter (inclusive).
     *
     * @param field the field name
     * @param min   the minimum value (inclusive)
     * @param max   the maximum value (inclusive)
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> between(@NotNull String field, @NotNull Object min, @NotNull Object max) {
        filters.add(Filters.and(Filters.gte(field, min), Filters.lte(field, max)));
        return this;
    }

    /**
     * Adds an "in" filter for matching any of the given values.
     *
     * @param field  the field name
     * @param values the values to match
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> in(@NotNull String field, @NotNull Iterable<?> values) {
        filters.add(Filters.in(field, values));
        return this;
    }

    /**
     * Adds an "in" filter for matching any of the given values.
     *
     * @param field  the field name
     * @param values the values to match
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> in(@NotNull String field, @NotNull Object... values) {
        filters.add(Filters.in(field, values));
        return this;
    }

    /**
     * Adds a "not in" filter for excluding the given values.
     *
     * @param field  the field name
     * @param values the values to exclude
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> nin(@NotNull String field, @NotNull Iterable<?> values) {
        filters.add(Filters.nin(field, values));
        return this;
    }

    /**
     * Adds a "not in" filter for excluding the given values.
     *
     * @param field  the field name
     * @param values the values to exclude
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> nin(@NotNull String field, @NotNull Object... values) {
        filters.add(Filters.nin(field, values));
        return this;
    }

    /**
     * Adds an "exists" filter to check if a field exists.
     *
     * @param field  the field name
     * @param exists true to match documents where the field exists
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> exists(@NotNull String field, boolean exists) {
        filters.add(Filters.exists(field, exists));
        return this;
    }

    /**
     * Adds a filter for documents where the field exists.
     *
     * @param field the field name
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> exists(@NotNull String field) {
        return exists(field, true);
    }

    /**
     * Adds a regex filter for pattern matching.
     *
     * @param field   the field name
     * @param pattern the regex pattern
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> regex(@NotNull String field, @NotNull String pattern) {
        filters.add(Filters.regex(field, pattern));
        return this;
    }

    /**
     * Adds a regex filter for pattern matching.
     *
     * @param field   the field name
     * @param pattern the regex pattern
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> regex(@NotNull String field, @NotNull Pattern pattern) {
        filters.add(Filters.regex(field, pattern));
        return this;
    }

    /**
     * Adds a case-insensitive regex filter.
     *
     * @param field   the field name
     * @param pattern the regex pattern
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> regexIgnoreCase(@NotNull String field, @NotNull String pattern) {
        filters.add(Filters.regex(field, pattern, "i"));
        return this;
    }

    /**
     * Adds a "contains" filter (case-insensitive).
     *
     * @param field the field name
     * @param text  the text to search for
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> contains(@NotNull String field, @NotNull String text) {
        filters.add(Filters.regex(field, Pattern.quote(text), "i"));
        return this;
    }

    /**
     * Adds a "starts with" filter (case-insensitive).
     *
     * @param field  the field name
     * @param prefix the prefix to match
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> startsWith(@NotNull String field, @NotNull String prefix) {
        filters.add(Filters.regex(field, "^" + Pattern.quote(prefix), "i"));
        return this;
    }

    /**
     * Adds an "ends with" filter (case-insensitive).
     *
     * @param field  the field name
     * @param suffix the suffix to match
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> endsWith(@NotNull String field, @NotNull String suffix) {
        filters.add(Filters.regex(field, Pattern.quote(suffix) + "$", "i"));
        return this;
    }

    /**
     * Adds a text search filter.
     *
     * <p>Requires a text index on the collection.
     *
     * @param search the text to search for
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> text(@NotNull String search) {
        filters.add(Filters.text(search));
        return this;
    }

    /**
     * Adds a filter for null values.
     *
     * @param field the field name
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> isNull(@NotNull String field) {
        filters.add(Filters.eq(field, null));
        return this;
    }

    /**
     * Adds a filter for non-null values.
     *
     * @param field the field name
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> isNotNull(@NotNull String field) {
        filters.add(Filters.ne(field, null));
        return this;
    }

    /**
     * Adds an array "all" filter (array contains all values).
     *
     * @param field  the array field name
     * @param values the values that must all be present
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> all(@NotNull String field, @NotNull Iterable<?> values) {
        filters.add(Filters.all(field, values));
        return this;
    }

    /**
     * Adds an array size filter.
     *
     * @param field the array field name
     * @param size  the expected array size
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> size(@NotNull String field, int size) {
        filters.add(Filters.size(field, size));
        return this;
    }

    /**
     * Adds an element match filter for arrays of documents.
     *
     * @param field  the array field name
     * @param filter the filter to match against array elements
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> elemMatch(@NotNull String field, @NotNull Bson filter) {
        filters.add(Filters.elemMatch(field, filter));
        return this;
    }

    /**
     * Combines the current filters with OR logic.
     *
     * <p>This resets the current filters and replaces them with a single
     * OR filter containing the previous filters.
     *
     * @param additionalFilter additional filter to OR with current filters
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> or(@NotNull Bson additionalFilter) {
        if (filters.isEmpty()) {
            filters.add(additionalFilter);
        } else {
            Bson combined = Filters.and(new ArrayList<>(filters));
            filters.clear();
            filters.add(Filters.or(combined, additionalFilter));
        }
        return this;
    }

    // ===========================================
    // Sort Methods
    // ===========================================

    /**
     * Sets ascending sort order.
     *
     * @param fields the fields to sort by
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> sortAsc(@NotNull String... fields) {
        this.sort = Sorts.ascending(fields);
        return this;
    }

    /**
     * Sets descending sort order.
     *
     * @param fields the fields to sort by
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> sortDesc(@NotNull String... fields) {
        this.sort = Sorts.descending(fields);
        return this;
    }

    /**
     * Sets a custom sort order.
     *
     * @param sort the sort specification
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> sort(@NotNull Bson sort) {
        this.sort = Objects.requireNonNull(sort, "Sort cannot be null");
        return this;
    }

    // ===========================================
    // Pagination Methods
    // ===========================================

    /**
     * Sets the number of documents to skip.
     *
     * @param skip the number of documents to skip
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> skip(int skip) {
        this.skipCount = Math.max(0, skip);
        return this;
    }

    /**
     * Sets the maximum number of documents to return.
     *
     * @param limit the maximum number of documents
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> limit(int limit) {
        this.limitCount = Math.max(0, limit);
        return this;
    }

    /**
     * Sets pagination using page number and page size.
     *
     * @param page     the page number (1-based)
     * @param pageSize the number of documents per page
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> page(int page, int pageSize) {
        this.skipCount = (Math.max(1, page) - 1) * pageSize;
        this.limitCount = Math.max(1, pageSize);
        return this;
    }

    // ===========================================
    // Projection Methods
    // ===========================================

    /**
     * Sets the fields to include in results.
     *
     * @param fields the fields to include
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> include(@NotNull String... fields) {
        Document projDoc = new Document();
        for (String field : fields) {
            projDoc.append(field, 1);
        }
        this.projection = projDoc;
        return this;
    }

    /**
     * Sets the fields to exclude from results.
     *
     * @param fields the fields to exclude
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> exclude(@NotNull String... fields) {
        Document projDoc = new Document();
        for (String field : fields) {
            projDoc.append(field, 0);
        }
        this.projection = projDoc;
        return this;
    }

    /**
     * Sets a custom projection.
     *
     * @param projection the projection specification
     * @return this query builder
     * @since 1.0.0
     */
    @NotNull
    public MongoQuery<T> projection(@NotNull Bson projection) {
        this.projection = Objects.requireNonNull(projection, "Projection cannot be null");
        return this;
    }

    // ===========================================
    // Execution Methods
    // ===========================================

    /**
     * Executes the query and returns all matching documents.
     *
     * @return a future completing with the matching documents
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<T>> execute() {
        Bson combinedFilter = buildFilter();
        return collection.find(combinedFilter, sort, skipCount, limitCount);
    }

    /**
     * Executes the query and returns the first matching document.
     *
     * @return a future completing with the first matching document
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> first() {
        return limit(1).execute()
                .thenApply(list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
    }

    /**
     * Counts the matching documents.
     *
     * @return a future completing with the count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> count() {
        Bson combinedFilter = buildFilter();
        return collection.countDocuments(combinedFilter);
    }

    /**
     * Checks if any matching documents exist.
     *
     * @return a future completing with true if any documents match
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> exists() {
        return count().thenApply(c -> c > 0);
    }

    /**
     * Builds the combined filter from all added filters.
     *
     * @return the combined filter, or an empty document if no filters
     */
    @NotNull
    private Bson buildFilter() {
        if (filters.isEmpty()) {
            return new Document();
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return Filters.and(filters);
        }
    }
}
