/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Base repository interface for MongoDB document operations.
 *
 * <p>This interface defines the standard CRUD operations for a MongoDB
 * collection with a specific document type and ID type. Implementations
 * provide type-safe access to MongoDB documents.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class PlayerRepository implements MongoRepository<PlayerData, UUID> {
 *     private final MongoCollectionWrapper<PlayerData> collection;
 *
 *     public PlayerRepository(MongoService mongoService) {
 *         this.collection = mongoService.getCollection("players", PlayerData.class);
 *     }
 *
 *     @Override
 *     public CompletableFuture<Optional<PlayerData>> findById(UUID id) {
 *         return collection.findOne(Filters.eq("_id", id.toString()));
 *     }
 *
 *     @Override
 *     public CompletableFuture<PlayerData> save(PlayerData entity) {
 *         return collection.replaceOne(
 *             Filters.eq("_id", entity.getUuid().toString()),
 *             entity,
 *             new ReplaceOptions().upsert(true)
 *         ).thenApply(result -> entity);
 *     }
 *
 *     // ... other methods
 * }
 * }</pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private PlayerRepository playerRepo;
 *
 * // Find a player
 * playerRepo.findById(uuid).thenAccept(playerOpt -> {
 *     playerOpt.ifPresent(player -> {
 *         logger.info("Found player: " + player.getName());
 *     });
 * });
 *
 * // Save a player
 * PlayerData player = new PlayerData(uuid, "Steve", 1000.0);
 * playerRepo.save(player).thenAccept(saved -> {
 *     logger.info("Saved player: " + saved.getName());
 * });
 *
 * // Find all VIP players
 * playerRepo.findAll(Filters.eq("vip", true)).thenAccept(vips -> {
 *     logger.info("Found " + vips.size() + " VIP players");
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe. All operations return
 * CompletableFuture for async execution.
 *
 * @param <T>  the document type
 * @param <ID> the ID type
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoCollectionWrapper
 * @see MongoService
 */
public interface MongoRepository<T, ID> {

    // ===========================================
    // Find Operations
    // ===========================================

    /**
     * Finds a document by its ID.
     *
     * @param id the document ID
     * @return a future completing with the document if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<T>> findById(@NotNull ID id);

    /**
     * Finds all documents in the collection.
     *
     * @return a future completing with all documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> findAll();

    /**
     * Finds all documents matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with matching documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> findAll(@NotNull Bson filter);

    /**
     * Finds documents with pagination.
     *
     * @param skip  the number of documents to skip
     * @param limit the maximum number of documents to return
     * @return a future completing with the documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> findAll(int skip, int limit);

    /**
     * Finds documents matching a filter with pagination.
     *
     * @param filter the filter to apply
     * @param skip   the number of documents to skip
     * @param limit  the maximum number of documents to return
     * @return a future completing with matching documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> findAll(@NotNull Bson filter, int skip, int limit);

    /**
     * Finds the first document matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the document if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<T>> findOne(@NotNull Bson filter);

    /**
     * Checks if a document with the given ID exists.
     *
     * @param id the document ID
     * @return a future completing with true if the document exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> existsById(@NotNull ID id);

    /**
     * Checks if any documents match the filter.
     *
     * @param filter the filter to apply
     * @return a future completing with true if any documents match
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists(@NotNull Bson filter);

    /**
     * Counts all documents in the collection.
     *
     * @return a future completing with the document count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> count();

    /**
     * Counts documents matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the matching document count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> count(@NotNull Bson filter);

    // ===========================================
    // Save Operations
    // ===========================================

    /**
     * Saves a document (insert or update).
     *
     * <p>If the document exists, it will be replaced. If it doesn't
     * exist, it will be inserted.
     *
     * @param entity the document to save
     * @return a future completing with the saved document
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<T> save(@NotNull T entity);

    /**
     * Saves multiple documents.
     *
     * @param entities the documents to save
     * @return a future completing with the saved documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> saveAll(@NotNull Iterable<T> entities);

    /**
     * Inserts a new document.
     *
     * <p>Unlike {@link #save(Object)}, this will fail if a document
     * with the same ID already exists.
     *
     * @param entity the document to insert
     * @return a future completing with the inserted document
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<T> insert(@NotNull T entity);

    /**
     * Inserts multiple new documents.
     *
     * @param entities the documents to insert
     * @return a future completing with the inserted documents
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> insertAll(@NotNull Iterable<T> entities);

    // ===========================================
    // Update Operations
    // ===========================================

    /**
     * Updates a document by ID with the specified update.
     *
     * @param id     the document ID
     * @param update the update to apply
     * @return a future completing with true if a document was updated
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> updateById(@NotNull ID id, @NotNull Bson update);

    /**
     * Updates the first document matching a filter.
     *
     * @param filter the filter to apply
     * @param update the update to apply
     * @return a future completing with the number of documents updated
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> updateOne(@NotNull Bson filter, @NotNull Bson update);

    /**
     * Updates all documents matching a filter.
     *
     * @param filter the filter to apply
     * @param update the update to apply
     * @return a future completing with the number of documents updated
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> updateMany(@NotNull Bson filter, @NotNull Bson update);

    /**
     * Finds a document and updates it atomically, returning the updated document.
     *
     * @param filter the filter to apply
     * @param update the update to apply
     * @return a future completing with the updated document
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<T>> findAndUpdate(@NotNull Bson filter, @NotNull Bson update);

    // ===========================================
    // Delete Operations
    // ===========================================

    /**
     * Deletes a document by its ID.
     *
     * @param id the document ID
     * @return a future completing with true if a document was deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> deleteById(@NotNull ID id);

    /**
     * Deletes a document.
     *
     * @param entity the document to delete
     * @return a future completing with true if the document was deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> delete(@NotNull T entity);

    /**
     * Deletes the first document matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with true if a document was deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> deleteOne(@NotNull Bson filter);

    /**
     * Deletes all documents matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the number of documents deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> deleteMany(@NotNull Bson filter);

    /**
     * Deletes all documents in the collection.
     *
     * @return a future completing with the number of documents deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> deleteAll();

    /**
     * Finds a document and deletes it atomically.
     *
     * @param filter the filter to apply
     * @return a future completing with the deleted document
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<T>> findAndDelete(@NotNull Bson filter);

    // ===========================================
    // Utility Methods
    // ===========================================

    /**
     * Returns the collection name this repository operates on.
     *
     * @return the collection name
     * @since 1.0.0
     */
    @NotNull
    String getCollectionName();

    /**
     * Returns the document class this repository operates on.
     *
     * @return the document class
     * @since 1.0.0
     */
    @NotNull
    Class<T> getDocumentClass();

    /**
     * Returns the underlying collection wrapper.
     *
     * @return the collection wrapper
     * @since 1.0.0
     */
    @NotNull
    MongoCollectionWrapper<T> getCollection();

    /**
     * Extracts the ID from a document.
     *
     * @param entity the document
     * @return the document ID
     * @since 1.0.0
     */
    @NotNull
    ID getId(@NotNull T entity);

    /**
     * Creates a filter for finding a document by ID.
     *
     * @param id the document ID
     * @return the ID filter
     * @since 1.0.0
     */
    @NotNull
    Bson createIdFilter(@NotNull ID id);
}
