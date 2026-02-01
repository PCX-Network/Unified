/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import sh.pcx.unified.service.Service;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main interface for MongoDB operations in the Unified Plugin API.
 *
 * <p>This service provides a high-level, async-first API for interacting with MongoDB,
 * wrapping the MongoDB Java Driver 5.5.0 async operations with CompletableFuture
 * for seamless integration with Java's async patterns.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private MongoService mongoService;
 *
 * // Insert a document
 * Document player = new Document("uuid", uuid.toString())
 *     .append("name", "Steve")
 *     .append("balance", 1000.0);
 *
 * mongoService.insertOne("players", player)
 *     .thenAccept(result -> {
 *         logger.info("Inserted player with id: " + result.getInsertedId());
 *     });
 *
 * // Find documents
 * mongoService.find("players", Filters.eq("name", "Steve"))
 *     .thenAccept(players -> {
 *         players.forEach(p -> logger.info("Found: " + p.toJson()));
 *     });
 *
 * // Update documents
 * mongoService.updateOne("players",
 *     Filters.eq("uuid", uuid.toString()),
 *     Updates.set("balance", 2000.0))
 *     .thenAccept(result -> {
 *         logger.info("Modified: " + result.getModifiedCount());
 *     });
 *
 * // Type-safe collections
 * MongoCollectionWrapper<PlayerData> players =
 *     mongoService.getCollection("players", PlayerData.class);
 *
 * players.findOne(Filters.eq("uuid", uuid.toString()))
 *     .thenAccept(player -> {
 *         player.ifPresent(p -> logger.info("Balance: " + p.getBalance()));
 *     });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This service is thread-safe. All operations return CompletableFuture and
 * execute asynchronously. Results are delivered on the MongoDB driver's async
 * thread pool.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoConnection
 * @see MongoCollectionWrapper
 * @see MongoRepository
 */
public interface MongoService extends Service {

    // ===========================================
    // Connection Management
    // ===========================================

    /**
     * Returns the underlying MongoDB connection.
     *
     * @return the MongoDB connection
     * @since 1.0.0
     */
    @NotNull
    MongoConnection getConnection();

    /**
     * Returns the current configuration.
     *
     * @return the MongoDB configuration
     * @since 1.0.0
     */
    @NotNull
    MongoConfig getConfig();

    /**
     * Checks if the connection is healthy.
     *
     * @return a future completing with true if connected and healthy
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> isHealthy();

    /**
     * Pings the MongoDB server to verify connectivity.
     *
     * @return a future completing when the ping succeeds
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> ping();

    // ===========================================
    // Collection Access
    // ===========================================

    /**
     * Gets a type-safe collection wrapper.
     *
     * @param collectionName the collection name
     * @param documentClass  the document class
     * @param <T>            the document type
     * @return a type-safe collection wrapper
     * @since 1.0.0
     */
    @NotNull
    <T> MongoCollectionWrapper<T> getCollection(@NotNull String collectionName, @NotNull Class<T> documentClass);

    /**
     * Gets a collection wrapper for raw BSON documents.
     *
     * @param collectionName the collection name
     * @return a document collection wrapper
     * @since 1.0.0
     */
    @NotNull
    MongoCollectionWrapper<Document> getCollection(@NotNull String collectionName);

    /**
     * Lists all collection names in the database.
     *
     * @return a future completing with the list of collection names
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<String>> listCollectionNames();

    /**
     * Creates a new collection.
     *
     * @param collectionName the collection name
     * @return a future completing when the collection is created
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> createCollection(@NotNull String collectionName);

    /**
     * Drops a collection.
     *
     * @param collectionName the collection name
     * @return a future completing when the collection is dropped
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> dropCollection(@NotNull String collectionName);

    /**
     * Checks if a collection exists.
     *
     * @param collectionName the collection name
     * @return a future completing with true if the collection exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> collectionExists(@NotNull String collectionName);

    // ===========================================
    // Insert Operations
    // ===========================================

    /**
     * Inserts a single document into a collection.
     *
     * @param collectionName the collection name
     * @param document       the document to insert
     * @return a future completing with the insert result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<InsertOneResult> insertOne(@NotNull String collectionName, @NotNull Document document);

    /**
     * Inserts multiple documents into a collection.
     *
     * @param collectionName the collection name
     * @param documents      the documents to insert
     * @return a future completing with the insert result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<InsertManyResult> insertMany(@NotNull String collectionName, @NotNull List<Document> documents);

    // ===========================================
    // Find Operations
    // ===========================================

    /**
     * Finds all documents in a collection.
     *
     * @param collectionName the collection name
     * @return a future completing with the list of documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Document>> find(@NotNull String collectionName);

    /**
     * Finds documents matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @return a future completing with the list of matching documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Document>> find(@NotNull String collectionName, @NotNull Bson filter);

    /**
     * Finds a single document matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @return a future completing with the document if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Document>> findOne(@NotNull String collectionName, @NotNull Bson filter);

    /**
     * Finds a document by its _id field.
     *
     * @param collectionName the collection name
     * @param id             the document id
     * @return a future completing with the document if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Document>> findById(@NotNull String collectionName, @NotNull Object id);

    /**
     * Counts documents in a collection.
     *
     * @param collectionName the collection name
     * @return a future completing with the document count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> countDocuments(@NotNull String collectionName);

    /**
     * Counts documents matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @return a future completing with the matching document count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> countDocuments(@NotNull String collectionName, @NotNull Bson filter);

    /**
     * Checks if any documents exist matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @return a future completing with true if any matching documents exist
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists(@NotNull String collectionName, @NotNull Bson filter);

    // ===========================================
    // Update Operations
    // ===========================================

    /**
     * Updates a single document matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @param update         the update to apply
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UpdateResult> updateOne(
            @NotNull String collectionName,
            @NotNull Bson filter,
            @NotNull Bson update
    );

    /**
     * Updates all documents matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @param update         the update to apply
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UpdateResult> updateMany(
            @NotNull String collectionName,
            @NotNull Bson filter,
            @NotNull Bson update
    );

    /**
     * Replaces a single document matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @param replacement    the replacement document
     * @return a future completing with the update result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UpdateResult> replaceOne(
            @NotNull String collectionName,
            @NotNull Bson filter,
            @NotNull Document replacement
    );

    /**
     * Finds a document and updates it atomically.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @param update         the update to apply
     * @return a future completing with the document before the update
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Document>> findOneAndUpdate(
            @NotNull String collectionName,
            @NotNull Bson filter,
            @NotNull Bson update
    );

    /**
     * Finds a document and replaces it atomically.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @param replacement    the replacement document
     * @return a future completing with the document before the replacement
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Document>> findOneAndReplace(
            @NotNull String collectionName,
            @NotNull Bson filter,
            @NotNull Document replacement
    );

    // ===========================================
    // Delete Operations
    // ===========================================

    /**
     * Deletes a single document matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @return a future completing with the delete result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<DeleteResult> deleteOne(@NotNull String collectionName, @NotNull Bson filter);

    /**
     * Deletes all documents matching a filter.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @return a future completing with the delete result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<DeleteResult> deleteMany(@NotNull String collectionName, @NotNull Bson filter);

    /**
     * Finds a document and deletes it atomically.
     *
     * @param collectionName the collection name
     * @param filter         the filter to apply
     * @return a future completing with the deleted document
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Document>> findOneAndDelete(@NotNull String collectionName, @NotNull Bson filter);

    // ===========================================
    // Aggregation Operations
    // ===========================================

    /**
     * Executes an aggregation pipeline.
     *
     * @param collectionName the collection name
     * @param pipeline       the aggregation pipeline stages
     * @return a future completing with the aggregation results
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Document>> aggregate(@NotNull String collectionName, @NotNull List<Bson> pipeline);

    /**
     * Executes an aggregation pipeline with typed results.
     *
     * @param collectionName the collection name
     * @param pipeline       the aggregation pipeline stages
     * @param resultClass    the result document class
     * @param <T>            the result type
     * @return a future completing with the typed aggregation results
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<List<T>> aggregate(
            @NotNull String collectionName,
            @NotNull List<Bson> pipeline,
            @NotNull Class<T> resultClass
    );

    /**
     * Returns distinct values for a field.
     *
     * @param collectionName the collection name
     * @param fieldName      the field name
     * @param resultClass    the result class
     * @param <T>            the result type
     * @return a future completing with the distinct values
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<List<T>> distinct(
            @NotNull String collectionName,
            @NotNull String fieldName,
            @NotNull Class<T> resultClass
    );

    /**
     * Returns distinct values for a field with a filter.
     *
     * @param collectionName the collection name
     * @param fieldName      the field name
     * @param filter         the filter to apply
     * @param resultClass    the result class
     * @param <T>            the result type
     * @return a future completing with the distinct values
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<List<T>> distinct(
            @NotNull String collectionName,
            @NotNull String fieldName,
            @NotNull Bson filter,
            @NotNull Class<T> resultClass
    );

    // ===========================================
    // Index Operations
    // ===========================================

    /**
     * Creates an index on a collection.
     *
     * @param collectionName the collection name
     * @param keys           the index keys
     * @return a future completing with the index name
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<String> createIndex(@NotNull String collectionName, @NotNull Bson keys);

    /**
     * Creates an index on a collection with a name.
     *
     * @param collectionName the collection name
     * @param keys           the index keys
     * @param indexName      the index name
     * @return a future completing with the index name
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<String> createIndex(
            @NotNull String collectionName,
            @NotNull Bson keys,
            @NotNull String indexName
    );

    /**
     * Lists all indexes on a collection.
     *
     * @param collectionName the collection name
     * @return a future completing with the list of index documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Document>> listIndexes(@NotNull String collectionName);

    /**
     * Drops an index from a collection.
     *
     * @param collectionName the collection name
     * @param indexName      the index name
     * @return a future completing when the index is dropped
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> dropIndex(@NotNull String collectionName, @NotNull String indexName);

    /**
     * Drops all indexes from a collection.
     *
     * @param collectionName the collection name
     * @return a future completing when all indexes are dropped
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> dropAllIndexes(@NotNull String collectionName);

    // ===========================================
    // Query Builders
    // ===========================================

    /**
     * Creates a new query builder for find operations.
     *
     * @param collectionName the collection name
     * @return a new query builder
     * @since 1.0.0
     */
    @NotNull
    MongoQuery<Document> query(@NotNull String collectionName);

    /**
     * Creates a new typed query builder for find operations.
     *
     * @param collectionName the collection name
     * @param documentClass  the document class
     * @param <T>            the document type
     * @return a new typed query builder
     * @since 1.0.0
     */
    @NotNull
    <T> MongoQuery<T> query(@NotNull String collectionName, @NotNull Class<T> documentClass);

    /**
     * Creates a new update builder.
     *
     * @return a new update builder
     * @since 1.0.0
     */
    @NotNull
    MongoUpdate update();

    /**
     * Creates a new aggregation pipeline builder.
     *
     * @param collectionName the collection name
     * @return a new aggregation builder
     * @since 1.0.0
     */
    @NotNull
    MongoAggregation aggregation(@NotNull String collectionName);

    // ===========================================
    // Change Streams
    // ===========================================

    /**
     * Gets the change stream manager.
     *
     * @return the change stream manager
     * @since 1.0.0
     */
    @NotNull
    ChangeStreamManager getChangeStreamManager();

    // ===========================================
    // GridFS
    // ===========================================

    /**
     * Gets the GridFS service for large file storage.
     *
     * @return the GridFS service
     * @since 1.0.0
     */
    @NotNull
    GridFSService getGridFS();

    /**
     * Gets a GridFS service with a custom bucket name.
     *
     * @param bucketName the bucket name
     * @return the GridFS service for the specified bucket
     * @since 1.0.0
     */
    @NotNull
    GridFSService getGridFS(@NotNull String bucketName);
}
