/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.query;

import sh.pcx.unified.data.sql.ConnectionProvider;
import sh.pcx.unified.data.sql.DatabaseConnection;
import sh.pcx.unified.data.sql.DatabaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Fluent DELETE query builder.
 *
 * <p>This builder provides a convenient API for constructing DELETE queries
 * with support for WHERE conditions and safety checks to prevent accidental
 * deletion of all rows.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Delete by ID
 * int affected = deleteBuilder
 *     .where("uuid", uuid)
 *     .execute();
 *
 * // Delete with complex conditions
 * deleteBuilder
 *     .where("status", "inactive")
 *     .and("last_login", "<", oneYearAgo)
 *     .executeAsync()
 *     .thenAccept(affected -> logger.info("Deleted {} inactive users", affected));
 *
 * // Delete with IN clause
 * deleteBuilder
 *     .whereIn("id", List.of(1, 2, 3, 4, 5))
 *     .execute();
 *
 * // Delete all (requires explicit confirmation)
 * deleteBuilder
 *     .deleteAll()  // Must call this to delete without WHERE
 *     .execute();
 * }</pre>
 *
 * <h2>Safety Features</h2>
 * <p>By default, attempting to execute a DELETE without a WHERE clause will
 * throw an exception. You must explicitly call {@link #deleteAll()} to
 * confirm deletion of all rows.
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Create new instances for each query.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see QueryBuilder
 */
public class DeleteBuilder {

    private final String tableName;
    private final ConnectionProvider connectionProvider;
    private final Executor asyncExecutor;
    private final DatabaseType databaseType;

    private final WhereClause whereClause;
    private Integer limit;
    private boolean confirmDeleteAll;

    /**
     * Creates a new DELETE builder for the specified table.
     *
     * @param tableName          the table to delete from
     * @param connectionProvider the connection provider
     * @param asyncExecutor      the executor for async operations
     * @param databaseType       the database type
     * @since 1.0.0
     */
    public DeleteBuilder(
            @NotNull String tableName,
            @NotNull ConnectionProvider connectionProvider,
            @NotNull Executor asyncExecutor,
            @NotNull DatabaseType databaseType
    ) {
        this.tableName = Objects.requireNonNull(tableName);
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor);
        this.databaseType = Objects.requireNonNull(databaseType);

        this.whereClause = new WhereClause();
        this.confirmDeleteAll = false;
    }

    // ========================================================================
    // WHERE Clause
    // ========================================================================

    /**
     * Adds a WHERE condition with equality.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.where("uuid", uuid);  // WHERE uuid = ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder where(@NotNull String column, @Nullable Object value) {
        whereClause.eq(column, value);
        return this;
    }

    /**
     * Adds a WHERE condition with a custom operator.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.where("created_at", "<", cutoffDate);
     * }</pre>
     *
     * @param column   the column name
     * @param operator the SQL operator
     * @param value    the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder where(@NotNull String column, @NotNull String operator, @Nullable Object value) {
        whereClause.condition(column, operator, value);
        return this;
    }

    /**
     * Adds an AND condition.
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder and(@NotNull String column, @Nullable Object value) {
        whereClause.and().eq(column, value);
        return this;
    }

    /**
     * Adds an AND condition with a custom operator.
     *
     * @param column   the column name
     * @param operator the SQL operator
     * @param value    the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder and(@NotNull String column, @NotNull String operator, @Nullable Object value) {
        whereClause.and().condition(column, operator, value);
        return this;
    }

    /**
     * Adds an OR condition.
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder or(@NotNull String column, @Nullable Object value) {
        whereClause.or().eq(column, value);
        return this;
    }

    /**
     * Adds an OR condition with a custom operator.
     *
     * @param column   the column name
     * @param operator the SQL operator
     * @param value    the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder or(@NotNull String column, @NotNull String operator, @Nullable Object value) {
        whereClause.or().condition(column, operator, value);
        return this;
    }

    /**
     * Adds an IN condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.whereIn("status", List.of("expired", "cancelled"));
     * }</pre>
     *
     * @param column the column name
     * @param values the collection of values
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder whereIn(@NotNull String column, @NotNull Collection<?> values) {
        whereClause.in(column, values);
        return this;
    }

    /**
     * Adds a NOT IN condition.
     *
     * @param column the column name
     * @param values the collection of values
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder whereNotIn(@NotNull String column, @NotNull Collection<?> values) {
        whereClause.notIn(column, values);
        return this;
    }

    /**
     * Adds a BETWEEN condition.
     *
     * @param column the column name
     * @param start  the range start
     * @param end    the range end
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder whereBetween(@NotNull String column, @NotNull Object start, @NotNull Object end) {
        whereClause.between(column, start, end);
        return this;
    }

    /**
     * Adds a LIKE condition.
     *
     * @param column  the column name
     * @param pattern the LIKE pattern
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder whereLike(@NotNull String column, @NotNull String pattern) {
        whereClause.like(column, pattern);
        return this;
    }

    /**
     * Adds an IS NULL condition.
     *
     * @param column the column name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder whereNull(@NotNull String column) {
        whereClause.isNull(column);
        return this;
    }

    /**
     * Adds an IS NOT NULL condition.
     *
     * @param column the column name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder whereNotNull(@NotNull String column) {
        whereClause.isNotNull(column);
        return this;
    }

    /**
     * Provides access to the full WHERE clause builder.
     *
     * @param configurator a function to configure the WHERE clause
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder whereClause(@NotNull Consumer<WhereClause> configurator) {
        configurator.accept(whereClause);
        return this;
    }

    // ========================================================================
    // Delete All
    // ========================================================================

    /**
     * Confirms deletion of all rows without a WHERE clause.
     *
     * <p>This method must be called to execute a DELETE without any WHERE
     * conditions. This is a safety feature to prevent accidental deletion
     * of all data.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * // This will throw an exception:
     * deleteBuilder.execute();  // No WHERE clause!
     *
     * // This will work:
     * deleteBuilder.deleteAll().execute();
     * }</pre>
     *
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder deleteAll() {
        this.confirmDeleteAll = true;
        return this;
    }

    // ========================================================================
    // LIMIT
    // ========================================================================

    /**
     * Adds a LIMIT clause to restrict the number of rows deleted.
     *
     * <p><strong>Note:</strong> Not all databases support LIMIT in DELETE.
     * MySQL/MariaDB and SQLite support it; PostgreSQL does not.
     *
     * @param limit the maximum number of rows to delete
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public DeleteBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    // ========================================================================
    // Build SQL
    // ========================================================================

    /**
     * Builds the SQL DELETE statement.
     *
     * @return the complete SQL DELETE statement
     * @throws IllegalStateException if no WHERE clause and deleteAll() not called
     * @since 1.0.0
     */
    @NotNull
    public String buildSql() {
        // Safety check
        if (whereClause.isEmpty() && !confirmDeleteAll) {
            throw new IllegalStateException(
                    "DELETE without WHERE clause requires explicit confirmation via deleteAll(). " +
                    "This is a safety feature to prevent accidental deletion of all rows."
            );
        }

        StringBuilder sql = new StringBuilder();

        // DELETE FROM table
        sql.append("DELETE FROM ").append(tableName);

        // WHERE clause
        if (!whereClause.isEmpty()) {
            sql.append(" ").append(whereClause.toSqlWithWhere());
        }

        // LIMIT (MySQL/MariaDB/SQLite only)
        if (limit != null && databaseType != DatabaseType.POSTGRESQL) {
            sql.append(" LIMIT ").append(limit);
        }

        return sql.toString();
    }

    /**
     * Returns all bound parameters in order.
     *
     * @return the parameter array
     * @since 1.0.0
     */
    @NotNull
    public Object[] getParameters() {
        return whereClause.getParameterArray();
    }

    // ========================================================================
    // Execution
    // ========================================================================

    /**
     * Executes the delete.
     *
     * @return the number of deleted rows
     * @throws SQLException if a database error occurs
     * @throws IllegalStateException if no WHERE clause and deleteAll() not called
     * @since 1.0.0
     */
    public int execute() throws SQLException {
        String sql = buildSql();
        Object[] params = getParameters();

        try (DatabaseConnection conn = connectionProvider.getConnection()) {
            return conn.executeUpdate(sql, params);
        }
    }

    /**
     * Executes the delete asynchronously.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.where("status", "expired")
     *     .executeAsync()
     *     .thenAccept(affected -> logger.info("Deleted {} expired records", affected));
     * }</pre>
     *
     * @return a future that completes with the number of deleted rows
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Integer> executeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute delete", e);
            }
        }, asyncExecutor);
    }

    /**
     * Performs a dry run, returning the SQL and parameters without executing.
     *
     * <p>Useful for debugging and testing.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * DeleteBuilder.DryRunResult result = builder
     *     .where("status", "expired")
     *     .dryRun();
     * logger.info("SQL: {}", result.sql());
     * logger.info("Params: {}", Arrays.toString(result.parameters()));
     * }</pre>
     *
     * @return the dry run result containing SQL and parameters
     * @since 1.0.0
     */
    @NotNull
    public DryRunResult dryRun() {
        return new DryRunResult(buildSql(), getParameters());
    }

    /**
     * Result of a dry run execution.
     *
     * @param sql        the SQL statement
     * @param parameters the bound parameters
     * @since 1.0.0
     */
    public record DryRunResult(@NotNull String sql, @NotNull Object[] parameters) {}

    @Override
    public String toString() {
        try {
            return "DeleteBuilder{sql='" + buildSql() + "', parameters=" + java.util.Arrays.toString(getParameters()) + "}";
        } catch (IllegalStateException e) {
            return "DeleteBuilder{table='" + tableName + "', confirmDeleteAll=" + confirmDeleteAll + "}";
        }
    }
}
