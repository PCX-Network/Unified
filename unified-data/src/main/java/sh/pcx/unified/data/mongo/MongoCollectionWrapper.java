/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static sh.pcx.unified.data.mongo.MongoConnection.toCompletableFuture;
import static sh.pcx.unified.data.mongo.MongoConnection.toCompletableFutureList;
import static sh.pcx.unified.data.mongo.MongoConnection.toCompletableFutureVoid;

/**
 * Type-safe wrapper for MongoDB collections providing async operations.
 *
 * <p>This class wraps a MongoDB collection and provides CompletableFuture-based
 * async operations with full type safety. It supports all common CRUD operations,
 * aggregations, and index management.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a type-safe collection
 * MongoCollectionWrapper<PlayerData> players =
 *     mongoService.getCollection("players", PlayerData.class);
 *
 * // Insert a document
 * PlayerData player = new PlayerData(uuid, "Steve", 1000.0);
 * players.insertOne(player)
 *     .thenAccept(result -> {
 *         logger.info("Inserted with id: " + result.getInsertedId());
 *     });
 *
 * // Find documents
 * players.find(Filters.eq("name", "Steve"))
 *     .thenAccept(list -> {
 *         list.forEach(p -> logger.info("Found: " + p.getName()));
 *     });
 *
 * // Find one document
 * players.findOne(Filters.eq("uuid", uuid.toString()))
 *     .thenAccept(opt -> {
 *         opt.ifPresent(p -> logger.info("Balance: " + p.getBalance()));
 *     });
 *
 * // Update documents
 * players.updateOne(
 *     Filters.eq("uuid", uuid.toString()),
 *     Updates.set("balance", 2000.0))
 *     .thenAccept(result -> {
 *         logger.info("Modified: " + result.getModifiedCount());
 *     });
 *
 * // Upsert (insert or update)
 * players.upsert(
 *     Filters.eq("uuid", uuid.toString()),
 *     Updates.set("balance", 2000.0))
 *     .thenAccept(result -> {
 *         if (result.getUpsertedId() != null) {
 *             logger.info("Inserted new document");
 *         } else {
 *             logger.info("Updated existing document");
 *         }
 *     });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations return CompletableFuture and
 * execute asynchronously.
 *
 * @param <T> the document type
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoService
 * @see MongoConnection
 */
public class MongoCollectionWrapper<T> {

    private final MongoCollection<T> collection;
    private final String collectionName;
    private final Class<T> documentClass;

    /**
     * Creates a new collection wrapper.
     *
     * @param collection    the underlying MongoDB collection
     * @param collectionName the collection name
     * @param documentClass the document class
     * @since 1.0.0
     */
    public MongoCollectionWrapper(
            @NotNull MongoCollection<T> collection,
            @NotNull String collectionName,
            @NotNull Class<T> documentClass
    ) {
        this.collection = Objects.requireNonNull(collection, "Collection cannot be null");
        this.collectionName = Objects.requireNonNull(collectionName, "Collection name cannot be null");
        this.documentClass = Objects.requireNonNull(documentClass, "Document class cannot be null");
    }

    /**
     * Returns the collection name.
     *
     * @return the collection name
     * @since 1.0.0
     */
    @NotNull
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Returns the document class.
     *
     * @return the document class
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getDocumentClass() {
        return documentClass;
    }

    /**
     * Returns the underlying MongoDB collection.
     *
     * @return the MongoDB collection
     * @since 1.0.0
     */
    @NotNull
    public MongoCollection<T> getCollection() {
        return collection;
    }

    // ===========================================
    // Insert Operations
    // ===========================================

    /**
     * Inserts a single document.
     *
     * @param document the document to insert
     * @return a future completing with the insert result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<InsertOneResult> insertOne(@NotNull T document) {
        Objects.requireNonNull(document, "Document cannot be null");
        return toCompletableFuture(collection.insertOne(document));
    }

    /**
     * Inserts a single document with options.
     *
     * @param document the document to insert
     * @param options  the insert options
     * @return a future completing with the insert result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<InsertOneResult> insertOne(@NotNull T document, @NotNull InsertOneOptions options) {
        Objects.requireNonNull(document, "Document cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.insertOne(document, options));
    }

    /**
     * Inserts multiple documents.
     *
     * @param documents the documents to insert
     * @return a future completing with the insert result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<InsertManyResult> insertMany(@NotNull List<T> documents) {
        Objects.requireNonNull(documents, "Documents cannot be null");
        return toCompletableFuture(collection.insertMany(documents));
    }

    /**
     * Inserts multiple documents with options.
     *
     * @param documents the documents to insert
     * @param options   the insert options
     * @return a future completing with the insert result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<InsertManyResult> insertMany(
            @NotNull List<T> documents,
            @NotNull InsertManyOptions options
    ) {
        Objects.requireNonNull(documents, "Documents cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.insertMany(documents, options));
    }

    // ===========================================
    // Find Operations
    // ===========================================

    /**
     * Finds all documents in the collection.
     *
     * @return a future completing with all documents
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<T>> find() {
        return toCompletableFutureList(collection.find());
    }

    /**
     * Finds documents matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with matching documents
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<T>> find(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return toCompletableFutureList(collection.find(filter));
    }

    /**
     * Finds documents with filter, sort, skip, and limit.
     *
     * @param filter the filter to apply
     * @param sort   the sort order (null for no sort)
     * @param skip   the number of documents to skip
     * @param limit  the maximum number of documents to return
     * @return a future completing with matching documents
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<T>> find(
            @NotNull Bson filter,
            @Nullable Bson sort,
            int skip,
            int limit
    ) {
        Objects.requireNonNull(filter, "Filter cannot be null");

        var findPublisher = collection.find(filter);
        if (sort != null) {
            findPublisher = findPublisher.sort(sort);
        }
        if (skip > 0) {
            findPublisher = findPublisher.skip(skip);
        }
        if (limit > 0) {
            findPublisher = findPublisher.limit(limit);
        }

        return toCompletableFutureList(findPublisher);
    }

    /**
     * Finds documents with projection.
     *
     * @param filter     the filter to apply
     * @param projection the fields to include/exclude
     * @return a future completing with matching documents
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<T>> find(@NotNull Bson filter, @NotNull Bson projection) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(projection, "Projection cannot be null");
        return toCompletableFutureList(collection.find(filter).projection(projection));
    }

    /**
     * Finds the first document matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the document if found
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findOne(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return toCompletableFuture(collection.find(filter).first())
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds a document by its _id field.
     *
     * @param id the document id
     * @return a future completing with the document if found
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findById(@NotNull Object id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return findOne(new Document("_id", id));
    }

    /**
     * Counts all documents in the collection.
     *
     * @return a future completing with the document count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> countDocuments() {
        return toCompletableFuture(collection.countDocuments());
    }

    /**
     * Counts documents matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the matching document count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> countDocuments(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return toCompletableFuture(collection.countDocuments(filter));
    }

    /**
     * Checks if any documents exist matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with true if any documents match
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> exists(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return countDocuments(filter).thenApply(count -> count > 0);
    }

    // ===========================================
    // Update Operations
    // ===========================================

    /**
     * Updates the first document matching a filter.
     *
     * @param filter the filter to apply
     * @param update the update to apply
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<UpdateResult> updateOne(@NotNull Bson filter, @NotNull Bson update) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(update, "Update cannot be null");
        return toCompletableFuture(collection.updateOne(filter, update));
    }

    /**
     * Updates the first document matching a filter with options.
     *
     * @param filter  the filter to apply
     * @param update  the update to apply
     * @param options the update options
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<UpdateResult> updateOne(
            @NotNull Bson filter,
            @NotNull Bson update,
            @NotNull UpdateOptions options
    ) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(update, "Update cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.updateOne(filter, update, options));
    }

    /**
     * Updates all documents matching a filter.
     *
     * @param filter the filter to apply
     * @param update the update to apply
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<UpdateResult> updateMany(@NotNull Bson filter, @NotNull Bson update) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(update, "Update cannot be null");
        return toCompletableFuture(collection.updateMany(filter, update));
    }

    /**
     * Updates all documents matching a filter with options.
     *
     * @param filter  the filter to apply
     * @param update  the update to apply
     * @param options the update options
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<UpdateResult> updateMany(
            @NotNull Bson filter,
            @NotNull Bson update,
            @NotNull UpdateOptions options
    ) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(update, "Update cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.updateMany(filter, update, options));
    }

    /**
     * Updates or inserts a document (upsert).
     *
     * @param filter the filter to apply
     * @param update the update to apply
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<UpdateResult> upsert(@NotNull Bson filter, @NotNull Bson update) {
        return updateOne(filter, update, new UpdateOptions().upsert(true));
    }

    /**
     * Replaces the first document matching a filter.
     *
     * @param filter      the filter to apply
     * @param replacement the replacement document
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<UpdateResult> replaceOne(@NotNull Bson filter, @NotNull T replacement) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(replacement, "Replacement cannot be null");
        return toCompletableFuture(collection.replaceOne(filter, replacement));
    }

    /**
     * Replaces the first document matching a filter with options.
     *
     * @param filter      the filter to apply
     * @param replacement the replacement document
     * @param options     the replace options
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<UpdateResult> replaceOne(
            @NotNull Bson filter,
            @NotNull T replacement,
            @NotNull ReplaceOptions options
    ) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(replacement, "Replacement cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.replaceOne(filter, replacement, options));
    }

    /**
     * Finds and updates a document atomically.
     *
     * @param filter the filter to apply
     * @param update the update to apply
     * @return a future completing with the document before the update
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findOneAndUpdate(@NotNull Bson filter, @NotNull Bson update) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(update, "Update cannot be null");
        return toCompletableFuture(collection.findOneAndUpdate(filter, update))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds and updates a document atomically with options.
     *
     * @param filter  the filter to apply
     * @param update  the update to apply
     * @param options the update options
     * @return a future completing with the document
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findOneAndUpdate(
            @NotNull Bson filter,
            @NotNull Bson update,
            @NotNull FindOneAndUpdateOptions options
    ) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(update, "Update cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.findOneAndUpdate(filter, update, options))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds and replaces a document atomically.
     *
     * @param filter      the filter to apply
     * @param replacement the replacement document
     * @return a future completing with the document before the replacement
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findOneAndReplace(@NotNull Bson filter, @NotNull T replacement) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(replacement, "Replacement cannot be null");
        return toCompletableFuture(collection.findOneAndReplace(filter, replacement))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds and replaces a document atomically with options.
     *
     * @param filter      the filter to apply
     * @param replacement the replacement document
     * @param options     the replace options
     * @return a future completing with the document
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findOneAndReplace(
            @NotNull Bson filter,
            @NotNull T replacement,
            @NotNull FindOneAndReplaceOptions options
    ) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(replacement, "Replacement cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.findOneAndReplace(filter, replacement, options))
                .thenApply(Optional::ofNullable);
    }

    // ===========================================
    // Delete Operations
    // ===========================================

    /**
     * Deletes the first document matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the delete result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<DeleteResult> deleteOne(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return toCompletableFuture(collection.deleteOne(filter));
    }

    /**
     * Deletes all documents matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the delete result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<DeleteResult> deleteMany(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return toCompletableFuture(collection.deleteMany(filter));
    }

    /**
     * Finds and deletes a document atomically.
     *
     * @param filter the filter to apply
     * @return a future completing with the deleted document
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findOneAndDelete(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return toCompletableFuture(collection.findOneAndDelete(filter))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds and deletes a document atomically with options.
     *
     * @param filter  the filter to apply
     * @param options the delete options
     * @return a future completing with the deleted document
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> findOneAndDelete(
            @NotNull Bson filter,
            @NotNull FindOneAndDeleteOptions options
    ) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.findOneAndDelete(filter, options))
                .thenApply(Optional::ofNullable);
    }

    // ===========================================
    // Aggregation Operations
    // ===========================================

    /**
     * Executes an aggregation pipeline.
     *
     * @param pipeline the aggregation pipeline stages
     * @return a future completing with the aggregation results
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<T>> aggregate(@NotNull List<Bson> pipeline) {
        Objects.requireNonNull(pipeline, "Pipeline cannot be null");
        return toCompletableFutureList(collection.aggregate(pipeline));
    }

    /**
     * Executes an aggregation pipeline with typed results.
     *
     * @param pipeline    the aggregation pipeline stages
     * @param resultClass the result class
     * @param <R>         the result type
     * @return a future completing with the typed aggregation results
     * @since 1.0.0
     */
    @NotNull
    public <R> CompletableFuture<List<R>> aggregate(
            @NotNull List<Bson> pipeline,
            @NotNull Class<R> resultClass
    ) {
        Objects.requireNonNull(pipeline, "Pipeline cannot be null");
        Objects.requireNonNull(resultClass, "Result class cannot be null");
        return toCompletableFutureList(collection.aggregate(pipeline, resultClass));
    }

    /**
     * Returns distinct values for a field.
     *
     * @param fieldName   the field name
     * @param resultClass the result class
     * @param <R>         the result type
     * @return a future completing with the distinct values
     * @since 1.0.0
     */
    @NotNull
    public <R> CompletableFuture<List<R>> distinct(
            @NotNull String fieldName,
            @NotNull Class<R> resultClass
    ) {
        Objects.requireNonNull(fieldName, "Field name cannot be null");
        Objects.requireNonNull(resultClass, "Result class cannot be null");
        return toCompletableFutureList(collection.distinct(fieldName, resultClass));
    }

    /**
     * Returns distinct values for a field with a filter.
     *
     * @param fieldName   the field name
     * @param filter      the filter to apply
     * @param resultClass the result class
     * @param <R>         the result type
     * @return a future completing with the distinct values
     * @since 1.0.0
     */
    @NotNull
    public <R> CompletableFuture<List<R>> distinct(
            @NotNull String fieldName,
            @NotNull Bson filter,
            @NotNull Class<R> resultClass
    ) {
        Objects.requireNonNull(fieldName, "Field name cannot be null");
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(resultClass, "Result class cannot be null");
        return toCompletableFutureList(collection.distinct(fieldName, filter, resultClass));
    }

    // ===========================================
    // Index Operations
    // ===========================================

    /**
     * Creates an index on the collection.
     *
     * @param keys the index keys
     * @return a future completing with the index name
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<String> createIndex(@NotNull Bson keys) {
        Objects.requireNonNull(keys, "Keys cannot be null");
        return toCompletableFuture(collection.createIndex(keys));
    }

    /**
     * Creates an index on the collection with options.
     *
     * @param keys    the index keys
     * @param options the index options
     * @return a future completing with the index name
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<String> createIndex(@NotNull Bson keys, @NotNull IndexOptions options) {
        Objects.requireNonNull(keys, "Keys cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        return toCompletableFuture(collection.createIndex(keys, options));
    }

    /**
     * Lists all indexes on the collection.
     *
     * @return a future completing with the list of index documents
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<Document>> listIndexes() {
        return toCompletableFutureList(collection.listIndexes());
    }

    /**
     * Drops an index from the collection.
     *
     * @param indexName the index name
     * @return a future completing when the index is dropped
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> dropIndex(@NotNull String indexName) {
        Objects.requireNonNull(indexName, "Index name cannot be null");
        return toCompletableFutureVoid(collection.dropIndex(indexName));
    }

    /**
     * Drops all indexes from the collection (except _id).
     *
     * @return a future completing when all indexes are dropped
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> dropAllIndexes() {
        return toCompletableFutureVoid(collection.dropIndexes());
    }

    // ===========================================
    // Collection Operations
    // ===========================================

    /**
     * Drops the collection.
     *
     * @return a future completing when the collection is dropped
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> drop() {
        return toCompletableFutureVoid(collection.drop());
    }

    /**
     * Renames the collection.
     *
     * @param newName the new collection name
     * @return a future completing when the collection is renamed
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> rename(@NotNull String newName) {
        Objects.requireNonNull(newName, "New name cannot be null");
        return toCompletableFutureVoid(
                collection.renameCollection(
                        new com.mongodb.MongoNamespace(
                                collection.getNamespace().getDatabaseName(),
                                newName
                        )
                )
        );
    }
}
