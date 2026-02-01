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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fluent INSERT query builder.
 *
 * <p>This builder provides a convenient API for constructing INSERT queries
 * with support for single inserts, batch inserts, and upserts (insert or update).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple insert
 * Long id = insertBuilder
 *     .columns("uuid", "name", "balance")
 *     .values(uuid, "Steve", 1000.0)
 *     .executeReturningId();
 *
 * // Insert with explicit column-value pairs
 * insertBuilder
 *     .column("uuid", uuid)
 *     .column("name", "Steve")
 *     .column("balance", 1000.0)
 *     .execute();
 *
 * // Upsert (insert or update on conflict)
 * insertBuilder
 *     .columns("uuid", "name", "balance")
 *     .values(uuid, "Steve", 1500.0)
 *     .onDuplicateKeyUpdate("balance")
 *     .execute();
 *
 * // Batch insert
 * insertBuilder
 *     .columns("uuid", "name")
 *     .batch()
 *         .values(uuid1, "Steve")
 *         .values(uuid2, "Alex")
 *         .values(uuid3, "Notch")
 *     .executeBatch();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Create new instances for each query.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see QueryBuilder
 */
public class InsertBuilder {

    private final String tableName;
    private final ConnectionProvider connectionProvider;
    private final Executor asyncExecutor;
    private final DatabaseType databaseType;

    private final List<String> columns;
    private final List<Object> values;
    private final List<String> updateOnDuplicateColumns;
    private final List<Object[]> batchValues;
    private boolean ignoreDuplicates;

    /**
     * Creates a new INSERT builder for the specified table.
     *
     * @param tableName          the table to insert into
     * @param connectionProvider the connection provider
     * @param asyncExecutor      the executor for async operations
     * @param databaseType       the database type
     * @since 1.0.0
     */
    public InsertBuilder(
            @NotNull String tableName,
            @NotNull ConnectionProvider connectionProvider,
            @NotNull Executor asyncExecutor,
            @NotNull DatabaseType databaseType
    ) {
        this.tableName = Objects.requireNonNull(tableName);
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor);
        this.databaseType = Objects.requireNonNull(databaseType);

        this.columns = new ArrayList<>();
        this.values = new ArrayList<>();
        this.updateOnDuplicateColumns = new ArrayList<>();
        this.batchValues = new ArrayList<>();
        this.ignoreDuplicates = false;
    }

    // ========================================================================
    // Column Specification
    // ========================================================================

    /**
     * Specifies the columns for the insert.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.columns("uuid", "name", "balance")
     *     .values(uuid, "Steve", 1000.0);
     * }</pre>
     *
     * @param columns the column names
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public InsertBuilder columns(@NotNull String... columns) {
        this.columns.clear();
        this.columns.addAll(List.of(columns));
        return this;
    }

    /**
     * Adds a column-value pair.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.column("uuid", uuid)
     *     .column("name", "Steve")
     *     .column("balance", 1000.0);
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to insert
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public InsertBuilder column(@NotNull String column, @Nullable Object value) {
        this.columns.add(column);
        this.values.add(value);
        return this;
    }

    // ========================================================================
    // Values
    // ========================================================================

    /**
     * Specifies the values to insert.
     *
     * <p>The number of values must match the number of columns specified
     * via {@link #columns(String...)}.
     *
     * @param values the values to insert
     * @return this builder for chaining
     * @throws IllegalArgumentException if value count doesn't match column count
     * @since 1.0.0
     */
    @NotNull
    public InsertBuilder values(@NotNull Object... values) {
        if (!this.columns.isEmpty() && values.length != this.columns.size()) {
            throw new IllegalArgumentException(
                    "Value count (" + values.length + ") doesn't match column count (" + columns.size() + ")"
            );
        }
        this.values.clear();
        this.values.addAll(List.of(values));
        return this;
    }

    /**
     * Specifies a map of column-value pairs.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Map<String, Object> data = Map.of(
     *     "uuid", uuid,
     *     "name", "Steve",
     *     "balance", 1000.0
     * );
     * builder.values(data);
     * }</pre>
     *
     * @param columnValues the column-value map
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public InsertBuilder values(@NotNull Map<String, Object> columnValues) {
        this.columns.clear();
        this.values.clear();
        this.columns.addAll(columnValues.keySet());
        this.values.addAll(columnValues.values());
        return this;
    }

    // ========================================================================
    // Upsert (Insert or Update)
    // ========================================================================

    /**
     * Enables ON DUPLICATE KEY UPDATE for specified columns.
     *
     * <p>If a row with the same primary key or unique constraint exists,
     * the specified columns will be updated instead of inserting a new row.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.columns("uuid", "name", "balance", "last_login")
     *     .values(uuid, "Steve", 1500.0, Instant.now())
     *     .onDuplicateKeyUpdate("balance", "last_login")
     *     .execute();
     * }</pre>
     *
     * @param columns the columns to update on duplicate
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public InsertBuilder onDuplicateKeyUpdate(@NotNull String... columns) {
        this.updateOnDuplicateColumns.addAll(List.of(columns));
        return this;
    }

    /**
     * Enables INSERT IGNORE mode (silently skip duplicate key errors).
     *
     * <p>Note: Behavior varies by database:
     * <ul>
     *   <li>MySQL/MariaDB: INSERT IGNORE</li>
     *   <li>SQLite: INSERT OR IGNORE</li>
     *   <li>PostgreSQL: INSERT ... ON CONFLICT DO NOTHING</li>
     * </ul>
     *
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public InsertBuilder ignoreDuplicates() {
        this.ignoreDuplicates = true;
        return this;
    }

    // ========================================================================
    // Batch Operations
    // ========================================================================

    /**
     * Starts batch mode for inserting multiple rows.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * int[] affected = builder
     *     .columns("uuid", "name")
     *     .batch()
     *         .values(uuid1, "Steve")
     *         .values(uuid2, "Alex")
     *     .executeBatch();
     * }</pre>
     *
     * @return a batch builder
     * @since 1.0.0
     */
    @NotNull
    public BatchBuilder batch() {
        return new BatchBuilder();
    }

    /**
     * Builder for batch insert operations.
     *
     * @since 1.0.0
     */
    public class BatchBuilder {

        /**
         * Adds a row to the batch.
         *
         * @param values the values for this row
         * @return this builder for chaining
         * @since 1.0.0
         */
        @NotNull
        public BatchBuilder values(@NotNull Object... values) {
            if (!columns.isEmpty() && values.length != columns.size()) {
                throw new IllegalArgumentException(
                        "Value count (" + values.length + ") doesn't match column count (" + columns.size() + ")"
                );
            }
            batchValues.add(values);
            return this;
        }

        /**
         * Executes the batch insert.
         *
         * @return an array of update counts
         * @throws SQLException if a database error occurs
         * @since 1.0.0
         */
        public int[] executeBatch() throws SQLException {
            return InsertBuilder.this.executeBatch();
        }

        /**
         * Executes the batch insert asynchronously.
         *
         * @return a future that completes with the update counts
         * @since 1.0.0
         */
        @NotNull
        public CompletableFuture<int[]> executeBatchAsync() {
            return InsertBuilder.this.executeBatchAsync();
        }
    }

    // ========================================================================
    // Build SQL
    // ========================================================================

    /**
     * Builds the SQL INSERT statement.
     *
     * @return the complete SQL INSERT statement
     * @since 1.0.0
     */
    @NotNull
    public String buildSql() {
        StringBuilder sql = new StringBuilder();

        // INSERT INTO
        if (ignoreDuplicates) {
            sql.append(getIgnoreSyntax());
        } else {
            sql.append("INSERT INTO ");
        }
        sql.append(tableName);

        // Columns
        if (!columns.isEmpty()) {
            sql.append(" (").append(String.join(", ", columns)).append(")");
        }

        // Values
        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());
        if (columns.isEmpty() && !values.isEmpty()) {
            placeholders = String.join(", ", values.stream().map(v -> "?").toList());
        }
        sql.append(" VALUES (").append(placeholders).append(")");

        // ON DUPLICATE KEY UPDATE
        if (!updateOnDuplicateColumns.isEmpty()) {
            sql.append(buildUpsertClause());
        }

        return sql.toString();
    }

    /**
     * Gets the database-specific ignore duplicates syntax.
     */
    @NotNull
    private String getIgnoreSyntax() {
        return switch (databaseType) {
            case SQLITE -> "INSERT OR IGNORE INTO ";
            case MYSQL, MARIADB -> "INSERT IGNORE INTO ";
            case POSTGRESQL -> "INSERT INTO "; // Handled differently
        };
    }

    /**
     * Builds the database-specific upsert clause.
     */
    @NotNull
    private String buildUpsertClause() {
        StringBuilder clause = new StringBuilder();

        switch (databaseType) {
            case MYSQL, MARIADB -> {
                clause.append(" ON DUPLICATE KEY UPDATE ");
                List<String> updates = new ArrayList<>();
                for (String col : updateOnDuplicateColumns) {
                    updates.add(col + " = VALUES(" + col + ")");
                }
                clause.append(String.join(", ", updates));
            }
            case POSTGRESQL -> {
                clause.append(" ON CONFLICT DO UPDATE SET ");
                List<String> updates = new ArrayList<>();
                for (String col : updateOnDuplicateColumns) {
                    updates.add(col + " = EXCLUDED." + col);
                }
                clause.append(String.join(", ", updates));
            }
            case SQLITE -> {
                // SQLite uses INSERT OR REPLACE, which requires different handling
                clause.append(" ON CONFLICT DO UPDATE SET ");
                List<String> updates = new ArrayList<>();
                for (String col : updateOnDuplicateColumns) {
                    updates.add(col + " = excluded." + col);
                }
                clause.append(String.join(", ", updates));
            }
        }

        return clause.toString();
    }

    /**
     * Returns all bound parameters in order.
     *
     * @return the parameter array
     * @since 1.0.0
     */
    @NotNull
    public Object[] getParameters() {
        return values.toArray();
    }

    // ========================================================================
    // Execution
    // ========================================================================

    /**
     * Executes the insert.
     *
     * @return the number of affected rows
     * @throws SQLException if a database error occurs
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
     * Executes the insert asynchronously.
     *
     * @return a future that completes with the number of affected rows
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Integer> executeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute insert", e);
            }
        }, asyncExecutor);
    }

    /**
     * Executes the insert and returns the generated ID.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Long id = builder
     *     .columns("name", "balance")
     *     .values("Steve", 1000.0)
     *     .executeReturningId();
     * }</pre>
     *
     * @return the generated ID, or null if none was generated
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @Nullable
    public Long executeReturningId() throws SQLException {
        String sql = buildSql();
        Object[] params = getParameters();

        try (DatabaseConnection conn = connectionProvider.getConnection()) {
            return conn.executeInsert(sql, params);
        }
    }

    /**
     * Executes the insert asynchronously and returns the generated ID.
     *
     * @return a future that completes with the generated ID
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> executeReturningIdAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeReturningId();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute insert", e);
            }
        }, asyncExecutor);
    }

    /**
     * Executes a batch insert.
     *
     * @return an array of update counts for each batch item
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public int[] executeBatch() throws SQLException {
        if (batchValues.isEmpty()) {
            return new int[0];
        }

        String sql = buildSql();

        try (DatabaseConnection conn = connectionProvider.getConnection()) {
            return conn.executeBatch(sql, batchValues);
        }
    }

    /**
     * Executes a batch insert asynchronously.
     *
     * @return a future that completes with the update counts
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<int[]> executeBatchAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeBatch();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute batch insert", e);
            }
        }, asyncExecutor);
    }

    @Override
    public String toString() {
        return "InsertBuilder{sql='" + buildSql() + "', parameters=" + values + "}";
    }
}
