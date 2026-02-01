/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql;

import sh.pcx.unified.data.sql.query.QueryBuilder;
import sh.pcx.unified.data.sql.query.SelectBuilder;
import sh.pcx.unified.data.sql.orm.Repository;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Main service interface for database operations.
 *
 * <p>This service provides a high-level API for interacting with SQL databases,
 * including connection management, query building, and ORM functionality.
 * All operations support both synchronous and asynchronous execution.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the database service via DI
 * @Inject
 * private DatabaseService database;
 *
 * // Simple query execution
 * CompletableFuture<List<PlayerData>> future = database.query(PlayerData.class)
 *     .where("uuid", uuid)
 *     .orderBy("last_login", Order.DESC)
 *     .limit(10)
 *     .executeAsync();
 *
 * // Execute raw SQL with parameters
 * database.executeAsync("UPDATE players SET balance = ? WHERE uuid = ?", 100.0, uuid)
 *     .thenAccept(affected -> logger.info("Updated {} rows", affected));
 *
 * // Transaction support
 * database.transactionAsync(conn -> {
 *     conn.executeUpdate("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, fromId);
 *     conn.executeUpdate("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, toId);
 *     return true; // Commit
 * }).exceptionally(e -> {
 *     logger.error("Transfer failed", e);
 *     return false;
 * });
 *
 * // Use repository pattern
 * Repository<PlayerData, UUID> playerRepo = database.getRepository(PlayerData.class);
 * Optional<PlayerData> player = playerRepo.findById(uuid).join();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The database service is thread-safe and can be injected and used from
 * any thread. Async methods execute on a dedicated database thread pool.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ConnectionProvider
 * @see QueryBuilder
 * @see Repository
 */
public interface DatabaseService extends Service {

    // ========================================================================
    // Connection Management
    // ========================================================================

    /**
     * Returns the connection provider for this database service.
     *
     * <p>Use this when you need direct connection access for complex operations.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * try (DatabaseConnection conn = database.getConnectionProvider().getConnection()) {
     *     // Complex operations with direct connection access
     * }
     * }</pre>
     *
     * @return the connection provider
     * @since 1.0.0
     */
    @NotNull
    ConnectionProvider getConnectionProvider();

    /**
     * Returns the database type for this service.
     *
     * @return the database type
     * @since 1.0.0
     */
    @NotNull
    DatabaseType getDatabaseType();

    /**
     * Returns the database configuration.
     *
     * <p>The returned configuration has the password redacted for security.
     *
     * @return the database configuration (password redacted)
     * @since 1.0.0
     */
    @NotNull
    DatabaseConfig getConfig();

    /**
     * Checks if the database connection is healthy.
     *
     * @return true if the connection is valid
     * @since 1.0.0
     */
    boolean isHealthy();

    /**
     * Checks if the database connection is healthy asynchronously.
     *
     * @return a future that completes with the health status
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> isHealthyAsync();

    // ========================================================================
    // Query Building
    // ========================================================================

    /**
     * Creates a new query builder for the specified entity type.
     *
     * <p>The entity class must be annotated with {@code @Table} and have
     * fields annotated with {@code @Column}.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * List<PlayerData> players = database.query(PlayerData.class)
     *     .where("level", ">", 10)
     *     .and("vip", true)
     *     .orderBy("level", Order.DESC)
     *     .limit(100)
     *     .execute();
     * }</pre>
     *
     * @param <T>         the entity type
     * @param entityClass the entity class
     * @return a new query builder
     * @since 1.0.0
     */
    @NotNull
    <T> SelectBuilder<T> query(@NotNull Class<T> entityClass);

    /**
     * Creates a new generic query builder.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * QueryBuilder builder = database.queryBuilder();
     * String sql = builder.select("name", "balance")
     *     .from("players")
     *     .where("uuid", uuid)
     *     .build();
     * }</pre>
     *
     * @return a new query builder
     * @since 1.0.0
     */
    @NotNull
    QueryBuilder queryBuilder();

    // ========================================================================
    // Direct Execution
    // ========================================================================

    /**
     * Executes an update statement and returns the number of affected rows.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * int affected = database.execute(
     *     "UPDATE players SET balance = ? WHERE uuid = ?",
     *     newBalance, uuid.toString()
     * );
     * }</pre>
     *
     * @param sql    the SQL statement
     * @param params the parameters to bind
     * @return the number of affected rows
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    int execute(@NotNull String sql, @NotNull Object... params) throws SQLException;

    /**
     * Executes an update statement asynchronously.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * database.executeAsync("UPDATE players SET online = false WHERE server = ?", serverName)
     *     .thenAccept(affected -> logger.info("Marked {} players offline", affected));
     * }</pre>
     *
     * @param sql    the SQL statement
     * @param params the parameters to bind
     * @return a future that completes with the number of affected rows
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> executeAsync(@NotNull String sql, @NotNull Object... params);

    /**
     * Executes a query and maps results using the provided mapper.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * List<String> names = database.query(
     *     "SELECT name FROM players WHERE level > ?",
     *     rs -> rs.getString("name"),
     *     10
     * );
     * }</pre>
     *
     * @param <T>    the result type
     * @param sql    the SQL query
     * @param mapper the function to map each row
     * @param params the parameters to bind
     * @return the list of mapped results
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    <T> List<T> query(@NotNull String sql, @NotNull ResultSetMapper<T> mapper, @NotNull Object... params)
            throws SQLException;

    /**
     * Executes a query asynchronously and maps results.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * database.queryAsync("SELECT * FROM players WHERE vip = ?", rs -> {
     *     return new PlayerData(rs.getString("uuid"), rs.getString("name"));
     * }, true).thenAccept(vipPlayers -> {
     *     vipPlayers.forEach(p -> logger.info("VIP: {}", p.getName()));
     * });
     * }</pre>
     *
     * @param <T>    the result type
     * @param sql    the SQL query
     * @param mapper the function to map each row
     * @param params the parameters to bind
     * @return a future that completes with the list of mapped results
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<List<T>> queryAsync(@NotNull String sql, @NotNull ResultSetMapper<T> mapper,
                                               @NotNull Object... params);

    /**
     * Executes a query and returns the first result.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Optional<PlayerData> player = database.queryFirst(
     *     "SELECT * FROM players WHERE uuid = ?",
     *     rs -> new PlayerData(rs),
     *     uuid.toString()
     * );
     * }</pre>
     *
     * @param <T>    the result type
     * @param sql    the SQL query
     * @param mapper the function to map the row
     * @param params the parameters to bind
     * @return an Optional containing the first result, or empty if none
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> queryFirst(@NotNull String sql, @NotNull ResultSetMapper<T> mapper, @NotNull Object... params)
            throws SQLException;

    /**
     * Executes a query asynchronously and returns the first result.
     *
     * @param <T>    the result type
     * @param sql    the SQL query
     * @param mapper the function to map the row
     * @param params the parameters to bind
     * @return a future that completes with an Optional containing the first result
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<Optional<T>> queryFirstAsync(@NotNull String sql, @NotNull ResultSetMapper<T> mapper,
                                                        @NotNull Object... params);

    // ========================================================================
    // Transactions
    // ========================================================================

    /**
     * Executes operations within a transaction.
     *
     * <p>If the function completes normally, the transaction is committed.
     * If an exception is thrown, the transaction is rolled back.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * boolean success = database.transaction(conn -> {
     *     conn.executeUpdate("UPDATE accounts SET balance = balance - ? WHERE id = ?", 100, fromId);
     *     conn.executeUpdate("UPDATE accounts SET balance = balance + ? WHERE id = ?", 100, toId);
     *     return true;
     * });
     * }</pre>
     *
     * @param <T>      the result type
     * @param function the function to execute within the transaction
     * @return the result of the function
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    <T> T transaction(@NotNull TransactionFunction<T> function) throws SQLException;

    /**
     * Executes operations within a transaction asynchronously.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * database.transactionAsync(conn -> {
     *     conn.executeUpdate("INSERT INTO audit_log (action) VALUES (?)", "transfer");
     *     conn.executeUpdate("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, from);
     *     conn.executeUpdate("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, to);
     *     return true;
     * }).thenAccept(success -> {
     *     if (success) {
     *         player.sendMessage("Transfer complete!");
     *     }
     * });
     * }</pre>
     *
     * @param <T>      the result type
     * @param function the function to execute within the transaction
     * @return a future that completes with the result of the function
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<T> transactionAsync(@NotNull TransactionFunction<T> function);

    /**
     * Executes a void operation within a transaction.
     *
     * @param consumer the consumer to execute within the transaction
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    void transactionVoid(@NotNull TransactionConsumer consumer) throws SQLException;

    /**
     * Executes a void operation within a transaction asynchronously.
     *
     * @param consumer the consumer to execute within the transaction
     * @return a future that completes when the transaction is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> transactionVoidAsync(@NotNull TransactionConsumer consumer);

    // ========================================================================
    // Repository
    // ========================================================================

    /**
     * Gets or creates a repository for the specified entity type.
     *
     * <p>Repositories provide CRUD operations with automatic mapping between
     * entities and database rows.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Repository<PlayerData, UUID> repo = database.getRepository(PlayerData.class);
     *
     * // Save entity
     * repo.save(playerData);
     *
     * // Find by ID
     * Optional<PlayerData> player = repo.findById(uuid).join();
     *
     * // Find all matching criteria
     * List<PlayerData> vips = repo.findAll(query -> query.where("vip", true)).join();
     * }</pre>
     *
     * @param <T>         the entity type
     * @param <ID>        the ID type
     * @param entityClass the entity class
     * @return the repository for the entity
     * @since 1.0.0
     */
    @NotNull
    <T, ID> Repository<T, ID> getRepository(@NotNull Class<T> entityClass);

    // ========================================================================
    // Schema Management
    // ========================================================================

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName the table name
     * @return true if the table exists
     * @since 1.0.0
     */
    boolean tableExists(@NotNull String tableName);

    /**
     * Checks if a table exists asynchronously.
     *
     * @param tableName the table name
     * @return a future that completes with the existence status
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> tableExistsAsync(@NotNull String tableName);

    /**
     * Creates a table from an entity class schema.
     *
     * <p>The table structure is derived from the entity's annotations.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * database.createTableAsync(PlayerData.class)
     *     .thenRun(() -> logger.info("Player data table created"));
     * }</pre>
     *
     * @param entityClass the entity class defining the schema
     * @return a future that completes when the table is created
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> createTableAsync(@NotNull Class<?> entityClass);

    // ========================================================================
    // Lifecycle
    // ========================================================================

    /**
     * Shuts down the database service and releases all connections.
     *
     * <p>This method blocks until all connections are closed or the timeout
     * is reached.
     *
     * @since 1.0.0
     */
    void shutdown();

    /**
     * Shuts down the database service asynchronously.
     *
     * @return a future that completes when shutdown is complete
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> shutdownAsync();

    // ========================================================================
    // Functional Interfaces
    // ========================================================================

    /**
     * Functional interface for mapping ResultSet rows to objects.
     *
     * @param <T> the result type
     * @since 1.0.0
     */
    @FunctionalInterface
    interface ResultSetMapper<T> {
        /**
         * Maps a single row from the ResultSet to an object.
         *
         * @param rs the result set positioned at the current row
         * @return the mapped object
         * @throws SQLException if a database error occurs
         */
        T map(@NotNull java.sql.ResultSet rs) throws SQLException;
    }

    /**
     * Functional interface for transaction operations that return a value.
     *
     * @param <T> the result type
     * @since 1.0.0
     */
    @FunctionalInterface
    interface TransactionFunction<T> {
        /**
         * Executes operations within a transaction.
         *
         * @param connection the database connection
         * @return the result
         * @throws SQLException if a database error occurs
         */
        T apply(@NotNull DatabaseConnection connection) throws SQLException;
    }

    /**
     * Functional interface for transaction operations that don't return a value.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface TransactionConsumer {
        /**
         * Executes operations within a transaction.
         *
         * @param connection the database connection
         * @throws SQLException if a database error occurs
         */
        void accept(@NotNull DatabaseConnection connection) throws SQLException;
    }
}
