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
import java.util.function.Consumer;

/**
 * Fluent UPDATE query builder.
 *
 * <p>This builder provides a convenient API for constructing UPDATE queries
 * with support for SET clauses, WHERE conditions, and arithmetic operations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple update
 * int affected = updateBuilder
 *     .set("balance", 1500.0)
 *     .set("last_login", Instant.now())
 *     .where("uuid", uuid)
 *     .execute();
 *
 * // Increment/decrement values
 * updateBuilder
 *     .increment("kills", 1)
 *     .decrement("lives", 1)
 *     .where("uuid", uuid)
 *     .execute();
 *
 * // Conditional update with complex WHERE
 * updateBuilder
 *     .set("status", "inactive")
 *     .where("last_login", "<", thirtyDaysAgo)
 *     .and("status", "active")
 *     .executeAsync()
 *     .thenAccept(affected -> logger.info("Deactivated {} users", affected));
 *
 * // Set to NULL
 * updateBuilder
 *     .setNull("email_verified_at")
 *     .where("email", oldEmail)
 *     .execute();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Create new instances for each query.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see QueryBuilder
 */
public class UpdateBuilder {

    private final String tableName;
    private final ConnectionProvider connectionProvider;
    private final Executor asyncExecutor;
    private final DatabaseType databaseType;

    private final List<SetClause> setClauses;
    private final WhereClause whereClause;
    private Integer limit;

    /**
     * Represents a SET clause entry.
     */
    private sealed interface SetClause permits SimpleSet, IncrementSet, DecrementSet, RawSet {
        String toSql();
        List<Object> getParameters();
    }

    private record SimpleSet(String column, Object value) implements SetClause {
        @Override
        public String toSql() {
            return column + " = ?";
        }

        @Override
        public List<Object> getParameters() {
            return List.of(value);
        }
    }

    private record IncrementSet(String column, Number value) implements SetClause {
        @Override
        public String toSql() {
            return column + " = " + column + " + ?";
        }

        @Override
        public List<Object> getParameters() {
            return List.of(value);
        }
    }

    private record DecrementSet(String column, Number value) implements SetClause {
        @Override
        public String toSql() {
            return column + " = " + column + " - ?";
        }

        @Override
        public List<Object> getParameters() {
            return List.of(value);
        }
    }

    private record RawSet(String expression, List<Object> params) implements SetClause {
        @Override
        public String toSql() {
            return expression;
        }

        @Override
        public List<Object> getParameters() {
            return params;
        }
    }

    /**
     * Creates a new UPDATE builder for the specified table.
     *
     * @param tableName          the table to update
     * @param connectionProvider the connection provider
     * @param asyncExecutor      the executor for async operations
     * @param databaseType       the database type
     * @since 1.0.0
     */
    public UpdateBuilder(
            @NotNull String tableName,
            @NotNull ConnectionProvider connectionProvider,
            @NotNull Executor asyncExecutor,
            @NotNull DatabaseType databaseType
    ) {
        this.tableName = Objects.requireNonNull(tableName);
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor);
        this.databaseType = Objects.requireNonNull(databaseType);

        this.setClauses = new ArrayList<>();
        this.whereClause = new WhereClause();
    }

    // ========================================================================
    // SET Clauses
    // ========================================================================

    /**
     * Adds a SET clause to update a column.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.set("balance", 1500.0)
     *     .set("last_login", Instant.now());
     * }</pre>
     *
     * @param column the column to update
     * @param value  the new value
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder set(@NotNull String column, @Nullable Object value) {
        setClauses.add(new SimpleSet(column, value));
        return this;
    }

    /**
     * Adds SET clauses from a map.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Map<String, Object> updates = Map.of(
     *     "name", "NewName",
     *     "balance", 1500.0
     * );
     * builder.setAll(updates);
     * }</pre>
     *
     * @param columnValues the column-value map
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder setAll(@NotNull Map<String, Object> columnValues) {
        for (var entry : columnValues.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Sets a column to NULL.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.setNull("email_verified_at");
     * }</pre>
     *
     * @param column the column to set to NULL
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder setNull(@NotNull String column) {
        return set(column, null);
    }

    /**
     * Increments a numeric column.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.increment("kills", 1);      // kills = kills + 1
     * builder.increment("balance", 100.0); // balance = balance + 100.0
     * }</pre>
     *
     * @param column the column to increment
     * @param value  the value to add
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder increment(@NotNull String column, @NotNull Number value) {
        setClauses.add(new IncrementSet(column, value));
        return this;
    }

    /**
     * Decrements a numeric column.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.decrement("lives", 1);     // lives = lives - 1
     * builder.decrement("balance", 50.0); // balance = balance - 50.0
     * }</pre>
     *
     * @param column the column to decrement
     * @param value  the value to subtract
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder decrement(@NotNull String column, @NotNull Number value) {
        setClauses.add(new DecrementSet(column, value));
        return this;
    }

    /**
     * Adds a raw SET expression.
     *
     * <p><strong>Warning:</strong> Be careful with raw SQL to avoid injection.
     * Use parameter placeholders where possible.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.setRaw("updated_at = NOW()");
     * builder.setRaw("balance = balance * ?", 1.1); // 10% increase
     * }</pre>
     *
     * @param expression the raw SQL expression
     * @param params     the parameters for the expression
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder setRaw(@NotNull String expression, @NotNull Object... params) {
        setClauses.add(new RawSet(expression, List.of(params)));
        return this;
    }

    // ========================================================================
    // WHERE Clause
    // ========================================================================

    /**
     * Adds a WHERE condition with equality.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.set("status", "banned")
     *     .where("uuid", uuid);
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder where(@NotNull String column, @Nullable Object value) {
        whereClause.eq(column, value);
        return this;
    }

    /**
     * Adds a WHERE condition with a custom operator.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.set("status", "inactive")
     *     .where("last_login", "<", thirtyDaysAgo);
     * }</pre>
     *
     * @param column   the column name
     * @param operator the SQL operator
     * @param value    the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder where(@NotNull String column, @NotNull String operator, @Nullable Object value) {
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
    public UpdateBuilder and(@NotNull String column, @Nullable Object value) {
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
    public UpdateBuilder and(@NotNull String column, @NotNull String operator, @Nullable Object value) {
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
    public UpdateBuilder or(@NotNull String column, @Nullable Object value) {
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
    public UpdateBuilder or(@NotNull String column, @NotNull String operator, @Nullable Object value) {
        whereClause.or().condition(column, operator, value);
        return this;
    }

    /**
     * Adds an IN condition.
     *
     * @param column the column name
     * @param values the collection of values
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder whereIn(@NotNull String column, @NotNull Collection<?> values) {
        whereClause.in(column, values);
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
    public UpdateBuilder whereNull(@NotNull String column) {
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
    public UpdateBuilder whereNotNull(@NotNull String column) {
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
    public UpdateBuilder whereClause(@NotNull Consumer<WhereClause> configurator) {
        configurator.accept(whereClause);
        return this;
    }

    // ========================================================================
    // LIMIT
    // ========================================================================

    /**
     * Adds a LIMIT clause to restrict the number of rows updated.
     *
     * <p><strong>Note:</strong> Not all databases support LIMIT in UPDATE.
     * MySQL/MariaDB support it; PostgreSQL does not.
     *
     * @param limit the maximum number of rows to update
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public UpdateBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    // ========================================================================
    // Build SQL
    // ========================================================================

    /**
     * Builds the SQL UPDATE statement.
     *
     * @return the complete SQL UPDATE statement
     * @throws IllegalStateException if no SET clauses are defined
     * @since 1.0.0
     */
    @NotNull
    public String buildSql() {
        if (setClauses.isEmpty()) {
            throw new IllegalStateException("At least one SET clause is required");
        }

        StringBuilder sql = new StringBuilder();

        // UPDATE table
        sql.append("UPDATE ").append(tableName);

        // SET clauses
        sql.append(" SET ");
        List<String> setParts = new ArrayList<>();
        for (SetClause clause : setClauses) {
            setParts.add(clause.toSql());
        }
        sql.append(String.join(", ", setParts));

        // WHERE clause
        if (!whereClause.isEmpty()) {
            sql.append(" ").append(whereClause.toSqlWithWhere());
        }

        // LIMIT (MySQL/MariaDB only)
        if (limit != null && (databaseType == DatabaseType.MYSQL || databaseType == DatabaseType.MARIADB)) {
            sql.append(" LIMIT ").append(limit);
        }

        return sql.toString();
    }

    /**
     * Returns all bound parameters in order.
     *
     * @return the parameter list
     * @since 1.0.0
     */
    @NotNull
    public List<Object> getParametersList() {
        List<Object> params = new ArrayList<>();

        // SET clause parameters
        for (SetClause clause : setClauses) {
            params.addAll(clause.getParameters());
        }

        // WHERE clause parameters
        params.addAll(whereClause.getParameters());

        return params;
    }

    /**
     * Returns all bound parameters as an array.
     *
     * @return the parameter array
     * @since 1.0.0
     */
    @NotNull
    public Object[] getParameters() {
        return getParametersList().toArray();
    }

    // ========================================================================
    // Execution
    // ========================================================================

    /**
     * Executes the update.
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
     * Executes the update asynchronously.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.set("status", "inactive")
     *     .where("last_login", "<", thirtyDaysAgo)
     *     .executeAsync()
     *     .thenAccept(affected -> logger.info("Deactivated {} users", affected));
     * }</pre>
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
                throw new RuntimeException("Failed to execute update", e);
            }
        }, asyncExecutor);
    }

    @Override
    public String toString() {
        return "UpdateBuilder{sql='" + buildSql() + "', parameters=" + getParametersList() + "}";
    }
}
