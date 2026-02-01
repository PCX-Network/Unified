/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Fluent builder for MongoDB aggregation pipelines.
 *
 * <p>This builder provides a type-safe, fluent API for constructing
 * MongoDB aggregation pipelines with support for all common stages.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple aggregation - count players by rank
 * mongoService.aggregation("players")
 *     .match(Filters.eq("active", true))
 *     .group("$rank",
 *         Accumulators.sum("count", 1),
 *         Accumulators.avg("avgLevel", "$level"))
 *     .sort(Sorts.descending("count"))
 *     .execute()
 *     .thenAccept(results -> {
 *         results.forEach(doc -> {
 *             logger.info(doc.getString("_id") + ": " + doc.getInteger("count"));
 *         });
 *     });
 *
 * // Leaderboard query
 * mongoService.aggregation("players")
 *     .match(Filters.gte("kills", 100))
 *     .project(
 *         Projections.include("name", "kills", "deaths"),
 *         Projections.computed("kd", new Document("$divide", Arrays.asList("$kills", "$deaths")))
 *     )
 *     .sort(Sorts.descending("kd"))
 *     .limit(10)
 *     .execute()
 *     .thenAccept(leaderboard -> {
 *         // Top 10 players by K/D ratio
 *     });
 *
 * // Lookup (join) with another collection
 * mongoService.aggregation("orders")
 *     .match(Filters.eq("status", "completed"))
 *     .lookup("products", "productId", "_id", "product")
 *     .unwind("$product")
 *     .group("$product.category",
 *         Accumulators.sum("totalSales", "$amount"))
 *     .execute();
 *
 * // Complex pipeline with facets
 * mongoService.aggregation("players")
 *     .facet(
 *         new Facet("byRank",
 *             Aggregates.group("$rank", Accumulators.sum("count", 1)),
 *             Aggregates.sort(Sorts.descending("count"))),
 *         new Facet("topKillers",
 *             Aggregates.sort(Sorts.descending("kills")),
 *             Aggregates.limit(5),
 *             Aggregates.project(Projections.include("name", "kills")))
 *     )
 *     .execute();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This builder is NOT thread-safe. Create a new builder for each aggregation.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoService#aggregation(String)
 */
public class MongoAggregation {

    private final MongoCollectionWrapper<Document> collection;
    private final List<Bson> pipeline = new ArrayList<>();

    /**
     * Creates a new aggregation builder for the given collection.
     *
     * @param collection the collection wrapper
     * @since 1.0.0
     */
    public MongoAggregation(@NotNull MongoCollectionWrapper<Document> collection) {
        this.collection = Objects.requireNonNull(collection, "Collection cannot be null");
    }

    // ===========================================
    // Pipeline Stages
    // ===========================================

    /**
     * Adds a match stage to filter documents.
     *
     * @param filter the filter to apply
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation match(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        pipeline.add(Aggregates.match(filter));
        return this;
    }

    /**
     * Adds a match stage with equality conditions.
     *
     * @param field the field name
     * @param value the value to match
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation match(@NotNull String field, @Nullable Object value) {
        pipeline.add(Aggregates.match(Filters.eq(field, value)));
        return this;
    }

    /**
     * Adds a project stage to reshape documents.
     *
     * @param projection the projection specification
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation project(@NotNull Bson projection) {
        Objects.requireNonNull(projection, "Projection cannot be null");
        pipeline.add(Aggregates.project(projection));
        return this;
    }

    /**
     * Adds a project stage with multiple projections.
     *
     * @param projections the projection specifications
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation project(@NotNull Bson... projections) {
        Objects.requireNonNull(projections, "Projections cannot be null");
        Document combined = new Document();
        for (Bson proj : projections) {
            combined.putAll(proj.toBsonDocument());
        }
        pipeline.add(Aggregates.project(combined));
        return this;
    }

    /**
     * Adds a project stage to include specified fields.
     *
     * @param fields the fields to include
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation projectInclude(@NotNull String... fields) {
        Document projection = new Document();
        for (String field : fields) {
            projection.append(field, 1);
        }
        pipeline.add(Aggregates.project(projection));
        return this;
    }

    /**
     * Adds a project stage to exclude specified fields.
     *
     * @param fields the fields to exclude
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation projectExclude(@NotNull String... fields) {
        Document projection = new Document();
        for (String field : fields) {
            projection.append(field, 0);
        }
        pipeline.add(Aggregates.project(projection));
        return this;
    }

    /**
     * Adds a group stage.
     *
     * @param id           the group id expression (field reference like "$fieldName" or null for all)
     * @param accumulators the field accumulators
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation group(@Nullable Object id, @NotNull BsonField... accumulators) {
        pipeline.add(Aggregates.group(id, accumulators));
        return this;
    }

    /**
     * Adds a group stage with a list of accumulators.
     *
     * @param id           the group id expression
     * @param accumulators the field accumulators
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation group(@Nullable Object id, @NotNull List<BsonField> accumulators) {
        pipeline.add(Aggregates.group(id, accumulators));
        return this;
    }

    /**
     * Adds a sort stage.
     *
     * @param sort the sort specification
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation sort(@NotNull Bson sort) {
        Objects.requireNonNull(sort, "Sort cannot be null");
        pipeline.add(Aggregates.sort(sort));
        return this;
    }

    /**
     * Adds an ascending sort stage.
     *
     * @param fields the fields to sort by
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation sortAsc(@NotNull String... fields) {
        pipeline.add(Aggregates.sort(Sorts.ascending(fields)));
        return this;
    }

    /**
     * Adds a descending sort stage.
     *
     * @param fields the fields to sort by
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation sortDesc(@NotNull String... fields) {
        pipeline.add(Aggregates.sort(Sorts.descending(fields)));
        return this;
    }

    /**
     * Adds a limit stage.
     *
     * @param limit the maximum number of documents
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation limit(int limit) {
        pipeline.add(Aggregates.limit(limit));
        return this;
    }

    /**
     * Adds a skip stage.
     *
     * @param skip the number of documents to skip
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation skip(int skip) {
        pipeline.add(Aggregates.skip(skip));
        return this;
    }

    /**
     * Adds a count stage.
     *
     * @param field the name of the output field containing the count
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation count(@NotNull String field) {
        Objects.requireNonNull(field, "Field cannot be null");
        pipeline.add(Aggregates.count(field));
        return this;
    }

    /**
     * Adds a count stage with default field name "count".
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation count() {
        return count("count");
    }

    /**
     * Adds an unwind stage to deconstruct an array field.
     *
     * @param field the array field path (e.g., "$items")
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation unwind(@NotNull String field) {
        Objects.requireNonNull(field, "Field cannot be null");
        pipeline.add(Aggregates.unwind(field));
        return this;
    }

    /**
     * Adds an unwind stage with options.
     *
     * @param field   the array field path
     * @param options the unwind options
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation unwind(@NotNull String field, @NotNull UnwindOptions options) {
        Objects.requireNonNull(field, "Field cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        pipeline.add(Aggregates.unwind(field, options));
        return this;
    }

    /**
     * Adds an unwind stage that preserves null and empty arrays.
     *
     * @param field the array field path
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation unwindPreserveEmpty(@NotNull String field) {
        return unwind(field, new UnwindOptions().preserveNullAndEmptyArrays(true));
    }

    /**
     * Adds a lookup (left outer join) stage.
     *
     * @param from         the collection to join with
     * @param localField   the field from the input documents
     * @param foreignField the field from the "from" collection
     * @param as           the output array field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation lookup(
            @NotNull String from,
            @NotNull String localField,
            @NotNull String foreignField,
            @NotNull String as
    ) {
        pipeline.add(Aggregates.lookup(from, localField, foreignField, as));
        return this;
    }

    /**
     * Adds a lookup stage with pipeline.
     *
     * @param from     the collection to join with
     * @param pipeline the pipeline to run on the joined collection
     * @param as       the output array field name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation lookup(
            @NotNull String from,
            @NotNull List<Bson> pipeline,
            @NotNull String as
    ) {
        this.pipeline.add(Aggregates.lookup(from, pipeline, as));
        return this;
    }

    /**
     * Adds an addFields stage to add new fields.
     *
     * @param fields the fields to add (as Field objects)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    @SafeVarargs
    public final MongoAggregation addFields(@NotNull com.mongodb.client.model.Field<?>... fields) {
        Objects.requireNonNull(fields, "Fields cannot be null");
        pipeline.add(Aggregates.addFields(fields));
        return this;
    }

    /**
     * Adds an addFields stage to add a single field.
     *
     * @param fieldName the field name
     * @param value     the field value expression
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation addField(@NotNull String fieldName, @Nullable Object value) {
        pipeline.add(Aggregates.addFields(new com.mongodb.client.model.Field<>(fieldName, value)));
        return this;
    }

    /**
     * Adds a replaceRoot stage to replace the root document.
     *
     * @param newRoot the new root document expression
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation replaceRoot(@NotNull Bson newRoot) {
        Objects.requireNonNull(newRoot, "New root cannot be null");
        pipeline.add(Aggregates.replaceRoot(newRoot));
        return this;
    }

    /**
     * Adds a replaceWith stage (alias for replaceRoot).
     *
     * @param replacement the replacement document expression
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation replaceWith(@NotNull Bson replacement) {
        return replaceRoot(replacement);
    }

    /**
     * Adds a facet stage for parallel pipelines.
     *
     * @param facets the facet definitions
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation facet(@NotNull Facet... facets) {
        Objects.requireNonNull(facets, "Facets cannot be null");
        pipeline.add(Aggregates.facet(facets));
        return this;
    }

    /**
     * Adds a facet stage with a list of facets.
     *
     * @param facets the facet definitions
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation facet(@NotNull List<Facet> facets) {
        Objects.requireNonNull(facets, "Facets cannot be null");
        pipeline.add(Aggregates.facet(facets));
        return this;
    }

    /**
     * Adds a bucket stage for grouping into discrete buckets.
     *
     * @param groupBy    the field to bucket by
     * @param boundaries the bucket boundaries
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation bucket(@NotNull Object groupBy, @NotNull List<?> boundaries) {
        Objects.requireNonNull(groupBy, "GroupBy cannot be null");
        Objects.requireNonNull(boundaries, "Boundaries cannot be null");
        pipeline.add(Aggregates.bucket(groupBy, boundaries));
        return this;
    }

    /**
     * Adds a bucketAuto stage for automatic bucket boundaries.
     *
     * @param groupBy the field to bucket by
     * @param buckets the number of buckets
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation bucketAuto(@NotNull Object groupBy, int buckets) {
        Objects.requireNonNull(groupBy, "GroupBy cannot be null");
        pipeline.add(Aggregates.bucketAuto(groupBy, buckets));
        return this;
    }

    /**
     * Adds a sample stage to randomly select documents.
     *
     * @param size the number of documents to sample
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation sample(int size) {
        pipeline.add(Aggregates.sample(size));
        return this;
    }

    /**
     * Adds a sortByCount stage.
     *
     * @param expression the expression to group and count by
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation sortByCount(@NotNull Object expression) {
        Objects.requireNonNull(expression, "Expression cannot be null");
        pipeline.add(Aggregates.sortByCount(expression));
        return this;
    }

    /**
     * Adds an out stage to write results to a collection.
     *
     * @param collectionName the output collection name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation out(@NotNull String collectionName) {
        Objects.requireNonNull(collectionName, "Collection name cannot be null");
        pipeline.add(Aggregates.out(collectionName));
        return this;
    }

    /**
     * Adds a merge stage to merge results into a collection.
     *
     * @param collectionName the target collection name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation merge(@NotNull String collectionName) {
        Objects.requireNonNull(collectionName, "Collection name cannot be null");
        pipeline.add(Aggregates.merge(collectionName));
        return this;
    }

    /**
     * Adds a raw pipeline stage.
     *
     * @param stage the pipeline stage
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation stage(@NotNull Bson stage) {
        Objects.requireNonNull(stage, "Stage cannot be null");
        pipeline.add(stage);
        return this;
    }

    /**
     * Adds multiple raw pipeline stages.
     *
     * @param stages the pipeline stages
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public MongoAggregation stages(@NotNull Bson... stages) {
        Objects.requireNonNull(stages, "Stages cannot be null");
        pipeline.addAll(Arrays.asList(stages));
        return this;
    }

    // ===========================================
    // Common Accumulators (convenience methods)
    // ===========================================

    /**
     * Creates a sum accumulator.
     *
     * @param fieldName  the output field name
     * @param expression the expression to sum
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField sum(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.sum(fieldName, expression);
    }

    /**
     * Creates an average accumulator.
     *
     * @param fieldName  the output field name
     * @param expression the expression to average
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField avg(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.avg(fieldName, expression);
    }

    /**
     * Creates a first accumulator.
     *
     * @param fieldName  the output field name
     * @param expression the expression to get first value from
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField first(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.first(fieldName, expression);
    }

    /**
     * Creates a last accumulator.
     *
     * @param fieldName  the output field name
     * @param expression the expression to get last value from
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField last(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.last(fieldName, expression);
    }

    /**
     * Creates a max accumulator.
     *
     * @param fieldName  the output field name
     * @param expression the expression to get max from
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField max(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.max(fieldName, expression);
    }

    /**
     * Creates a min accumulator.
     *
     * @param fieldName  the output field name
     * @param expression the expression to get min from
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField min(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.min(fieldName, expression);
    }

    /**
     * Creates a push accumulator (collect into array).
     *
     * @param fieldName  the output field name
     * @param expression the expression to push
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField push(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.push(fieldName, expression);
    }

    /**
     * Creates an addToSet accumulator (collect unique values).
     *
     * @param fieldName  the output field name
     * @param expression the expression to add
     * @return the accumulator
     * @since 1.0.0
     */
    @NotNull
    public static BsonField addToSet(@NotNull String fieldName, @NotNull Object expression) {
        return Accumulators.addToSet(fieldName, expression);
    }

    // ===========================================
    // Execution Methods
    // ===========================================

    /**
     * Executes the aggregation pipeline.
     *
     * @return a future completing with the aggregation results
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<Document>> execute() {
        return collection.aggregate(pipeline);
    }

    /**
     * Executes the aggregation pipeline with typed results.
     *
     * @param resultClass the result class
     * @param <R>         the result type
     * @return a future completing with the typed aggregation results
     * @since 1.0.0
     */
    @NotNull
    public <R> CompletableFuture<List<R>> execute(@NotNull Class<R> resultClass) {
        Objects.requireNonNull(resultClass, "Result class cannot be null");
        return collection.aggregate(pipeline, resultClass);
    }

    /**
     * Returns the built pipeline stages.
     *
     * @return the pipeline stages
     * @since 1.0.0
     */
    @NotNull
    public List<Bson> getPipeline() {
        return new ArrayList<>(pipeline);
    }

    /**
     * Checks if the pipeline is empty.
     *
     * @return true if no stages have been added
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return pipeline.isEmpty();
    }

    /**
     * Returns the number of stages in the pipeline.
     *
     * @return the stage count
     * @since 1.0.0
     */
    public int size() {
        return pipeline.size();
    }
}
