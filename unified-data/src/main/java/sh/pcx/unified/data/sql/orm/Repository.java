/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.orm;

import sh.pcx.unified.data.sql.query.SelectBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Base repository interface for entity CRUD operations.
 *
 * <p>Repositories provide a high-level, type-safe interface for common
 * database operations on entities. They abstract away SQL details and
 * provide async-first operations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get repository from database service
 * Repository<PlayerData, UUID> playerRepo = database.getRepository(PlayerData.class);
 *
 * // Save an entity
 * PlayerData player = new PlayerData(uuid, "Steve", 1000.0);
 * playerRepo.save(player).join();
 *
 * // Find by ID
 * Optional<PlayerData> found = playerRepo.findById(uuid).join();
 *
 * // Find all matching criteria
 * List<PlayerData> vipPlayers = playerRepo.findAll(query -> query
 *     .where("vip", true)
 *     .orderBy("balance", Order.DESC)
 *     .limit(100)
 * ).join();
 *
 * // Update
 * player.setBalance(1500.0);
 * playerRepo.update(player).join();
 *
 * // Delete
 * playerRepo.deleteById(uuid).join();
 *
 * // Count
 * long count = playerRepo.count().join();
 *
 * // Check existence
 * boolean exists = playerRepo.existsById(uuid).join();
 * }</pre>
 *
 * <h2>Custom Repositories</h2>
 * <pre>{@code
 * public interface PlayerRepository extends Repository<PlayerData, UUID> {
 *     // Custom query methods
 *     CompletableFuture<List<PlayerData>> findTopByKills(int limit);
 *     CompletableFuture<Optional<PlayerData>> findByName(String name);
 * }
 *
 * public class PlayerRepositoryImpl extends AbstractRepository<PlayerData, UUID>
 *         implements PlayerRepository {
 *
 *     @Override
 *     public CompletableFuture<List<PlayerData>> findTopByKills(int limit) {
 *         return query(q -> q.orderBy("kills", Order.DESC).limit(limit));
 *     }
 *
 *     @Override
 *     public CompletableFuture<Optional<PlayerData>> findByName(String name) {
 *         return findFirst(q -> q.where("name", name));
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Repository implementations should be thread-safe.
 *
 * @param <T>  the entity type
 * @param <ID> the ID type
 * @since 1.0.0
 * @author Supatuck
 * @see EntityMapper
 */
public interface Repository<T, ID> {

    // ========================================================================
    // Find Operations
    // ========================================================================

    /**
     * Finds an entity by its ID.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Optional<PlayerData> player = repository.findById(uuid).join();
     * player.ifPresent(p -> logger.info("Found player: {}", p.getName()));
     * }</pre>
     *
     * @param id the entity ID
     * @return a future containing an Optional with the entity if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<T>> findById(@NotNull ID id);

    /**
     * Finds all entities matching the given IDs.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * List<UUID> ids = List.of(uuid1, uuid2, uuid3);
     * List<PlayerData> players = repository.findAllById(ids).join();
     * }</pre>
     *
     * @param ids the entity IDs
     * @return a future containing the list of found entities
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> findAllById(@NotNull Collection<ID> ids);

    /**
     * Finds all entities in the table.
     *
     * <p><strong>Warning:</strong> This loads all entities into memory.
     * Use with caution on large tables.
     *
     * @return a future containing all entities
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> findAll();

    /**
     * Finds entities matching the specified query criteria.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * List<PlayerData> activePlayers = repository.findAll(query -> query
     *     .where("active", true)
     *     .and("level", ">", 10)
     *     .orderBy("last_login", Order.DESC)
     *     .limit(100)
     * ).join();
     * }</pre>
     *
     * @param queryConfigurer a function to configure the query
     * @return a future containing the matching entities
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> findAll(@NotNull Consumer<SelectBuilder<T>> queryConfigurer);

    /**
     * Finds the first entity matching the specified query criteria.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Optional<PlayerData> topPlayer = repository.findFirst(query -> query
     *     .orderBy("balance", Order.DESC)
     * ).join();
     * }</pre>
     *
     * @param queryConfigurer a function to configure the query
     * @return a future containing an Optional with the first matching entity
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<T>> findFirst(@NotNull Consumer<SelectBuilder<T>> queryConfigurer);

    // ========================================================================
    // Save Operations
    // ========================================================================

    /**
     * Saves an entity (insert or update).
     *
     * <p>If the entity has an auto-generated ID that is null, an INSERT is
     * performed. If the ID is set, an UPDATE is performed (or INSERT if
     * the entity doesn't exist).
     *
     * <h2>Example</h2>
     * <pre>{@code
     * PlayerData player = new PlayerData(uuid, "Steve", 1000.0);
     * repository.save(player).join();
     * }</pre>
     *
     * @param entity the entity to save
     * @return a future containing the saved entity (with generated ID if applicable)
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<T> save(@NotNull T entity);

    /**
     * Saves multiple entities.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * List<PlayerData> players = List.of(player1, player2, player3);
     * List<PlayerData> saved = repository.saveAll(players).join();
     * }</pre>
     *
     * @param entities the entities to save
     * @return a future containing the saved entities
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<T>> saveAll(@NotNull Iterable<T> entities);

    /**
     * Inserts a new entity.
     *
     * <p>Use this when you know the entity is new and should be inserted.
     * Throws an exception if an entity with the same ID already exists.
     *
     * @param entity the entity to insert
     * @return a future containing the inserted entity
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<T> insert(@NotNull T entity);

    /**
     * Updates an existing entity.
     *
     * <p>Use this when you know the entity exists and should be updated.
     *
     * @param entity the entity to update
     * @return a future that completes when the update is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> update(@NotNull T entity);

    // ========================================================================
    // Delete Operations
    // ========================================================================

    /**
     * Deletes an entity by its ID.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * repository.deleteById(uuid).join();
     * }</pre>
     *
     * @param id the entity ID
     * @return a future that completes with true if the entity was deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> deleteById(@NotNull ID id);

    /**
     * Deletes an entity.
     *
     * @param entity the entity to delete
     * @return a future that completes when the deletion is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> delete(@NotNull T entity);

    /**
     * Deletes multiple entities.
     *
     * @param entities the entities to delete
     * @return a future that completes when all deletions are done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> deleteAll(@NotNull Iterable<T> entities);

    /**
     * Deletes entities matching the specified IDs.
     *
     * @param ids the entity IDs
     * @return a future that completes with the number of deleted entities
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> deleteAllById(@NotNull Collection<ID> ids);

    /**
     * Deletes entities matching the specified query criteria.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * int deleted = repository.deleteWhere(query -> query
     *     .where("last_login", "<", oneYearAgo)
     * ).join();
     * logger.info("Deleted {} inactive players", deleted);
     * }</pre>
     *
     * @param queryConfigurer a function to configure the WHERE clause
     * @return a future that completes with the number of deleted entities
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> deleteWhere(@NotNull Consumer<SelectBuilder<T>> queryConfigurer);

    // ========================================================================
    // Count and Existence
    // ========================================================================

    /**
     * Counts all entities in the table.
     *
     * @return a future containing the total count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> count();

    /**
     * Counts entities matching the specified query criteria.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * long activeCount = repository.count(query -> query
     *     .where("active", true)
     * ).join();
     * }</pre>
     *
     * @param queryConfigurer a function to configure the query
     * @return a future containing the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Long> count(@NotNull Consumer<SelectBuilder<T>> queryConfigurer);

    /**
     * Checks if an entity with the given ID exists.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * if (repository.existsById(uuid).join()) {
     *     logger.info("Player exists");
     * }
     * }</pre>
     *
     * @param id the entity ID
     * @return a future containing true if the entity exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> existsById(@NotNull ID id);

    /**
     * Checks if any entities match the specified query criteria.
     *
     * @param queryConfigurer a function to configure the query
     * @return a future containing true if any entities match
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists(@NotNull Consumer<SelectBuilder<T>> queryConfigurer);

    // ========================================================================
    // Query Builder Access
    // ========================================================================

    /**
     * Creates a new SelectBuilder for custom queries.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * List<PlayerData> results = repository.query()
     *     .select("uuid", "name", "balance")
     *     .where("vip", true)
     *     .orderBy("balance", Order.DESC)
     *     .limit(10)
     *     .executeAsync()
     *     .join();
     * }</pre>
     *
     * @return a new SelectBuilder
     * @since 1.0.0
     */
    @NotNull
    SelectBuilder<T> query();

    // ========================================================================
    // Metadata
    // ========================================================================

    /**
     * Returns the entity class managed by this repository.
     *
     * @return the entity class
     * @since 1.0.0
     */
    @NotNull
    Class<T> getEntityClass();

    /**
     * Returns the entity mapper used by this repository.
     *
     * @return the entity mapper
     * @since 1.0.0
     */
    @NotNull
    EntityMapper<T> getEntityMapper();

    /**
     * Returns the table name for this repository.
     *
     * @return the table name
     * @since 1.0.0
     */
    @NotNull
    default String getTableName() {
        return getEntityMapper().getTableName();
    }
}
