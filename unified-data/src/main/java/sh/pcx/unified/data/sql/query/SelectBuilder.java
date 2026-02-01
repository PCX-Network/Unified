/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.query;

import sh.pcx.unified.data.sql.ConnectionProvider;
import sh.pcx.unified.data.sql.DatabaseConnection;
import sh.pcx.unified.data.sql.DatabaseType;
import sh.pcx.unified.data.sql.orm.EntityMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Fluent SELECT query builder with type-safe entity mapping.
 *
 * <p>This builder provides a convenient API for constructing SELECT queries
 * and mapping results to entity objects using the ORM-lite annotations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Basic query with entity mapping
 * List<PlayerData> players = selectBuilder
 *     .where("uuid", uuid)
 *     .execute();
 *
 * // Complex query with fluent API
 * List<PlayerData> topPlayers = selectBuilder
 *     .where("active", true)
 *     .and("level", ">", 10)
 *     .orderBy("kills", Order.DESC)
 *     .limit(100)
 *     .execute();
 *
 * // Async execution
 * selectBuilder
 *     .where("uuid", uuid)
 *     .executeAsync()
 *     .thenAccept(results -> {
 *         results.forEach(player -> logger.info("Found: {}", player));
 *     });
 *
 * // Get single result
 * Optional<PlayerData> player = selectBuilder
 *     .where("uuid", uuid)
 *     .executeFirst();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Create new instances for each query.
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author Supatuck
 * @see QueryBuilder
 * @see EntityMapper
 */
public class SelectBuilder<T> {

    private final Class<T> entityClass;
    private final EntityMapper<T> entityMapper;
    private final ConnectionProvider connectionProvider;
    private final Executor asyncExecutor;
    private final DatabaseType databaseType;

    private final List<String> columns;
    private String tableName;
    private final WhereClause whereClause;
    private final List<String> joins;
    private final List<OrderBy> orderByClauses;
    private String groupBy;
    private String having;
    private final List<Object> havingParams;
    private Integer limit;
    private Integer offset;
    private boolean distinct;

    /**
     * Sort order specification.
     */
    public enum Order {
        /** Ascending order. */
        ASC,
        /** Descending order. */
        DESC
    }

    private record OrderBy(String column, Order order) {
        @Override
        public String toString() {
            return column + " " + order.name();
        }
    }

    /**
     * Creates a new SELECT builder for the specified entity class.
     *
     * @param entityClass        the entity class to map results to
     * @param entityMapper       the entity mapper
     * @param connectionProvider the connection provider
     * @param asyncExecutor      the executor for async operations
     * @param databaseType       the database type
     * @since 1.0.0
     */
    public SelectBuilder(
            @NotNull Class<T> entityClass,
            @NotNull EntityMapper<T> entityMapper,
            @NotNull ConnectionProvider connectionProvider,
            @NotNull Executor asyncExecutor,
            @NotNull DatabaseType databaseType
    ) {
        this.entityClass = entityClass;
        this.entityMapper = entityMapper;
        this.connectionProvider = connectionProvider;
        this.asyncExecutor = asyncExecutor;
        this.databaseType = databaseType;

        this.columns = new ArrayList<>();
        this.tableName = entityMapper.getTableName();
        this.whereClause = new WhereClause();
        this.joins = new ArrayList<>();
        this.orderByClauses = new ArrayList<>();
        this.havingParams = new ArrayList<>();
        this.distinct = false;
    }

    // ========================================================================
    // Column Selection
    // ========================================================================

    /**
     * Specifies which columns to select.
     *
     * <p>If not called, all columns defined in the entity will be selected.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.select("id", "name", "balance");
     * }</pre>
     *
     * @param columns the column names
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> select(@NotNull String... columns) {
        this.columns.clear();
        this.columns.addAll(List.of(columns));
        return this;
    }

    /**
     * Enables DISTINCT for the query.
     *
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * Overrides the table name.
     *
     * @param tableName the table name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> from(@NotNull String tableName) {
        this.tableName = tableName;
        return this;
    }

    // ========================================================================
    // JOINs
    // ========================================================================

    /**
     * Adds a JOIN clause.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.join("statistics s", "p.id = s.player_id");
     * }</pre>
     *
     * @param table     the table to join
     * @param condition the join condition
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> join(@NotNull String table, @NotNull String condition) {
        joins.add("JOIN " + table + " ON " + condition);
        return this;
    }

    /**
     * Adds a LEFT JOIN clause.
     *
     * @param table     the table to join
     * @param condition the join condition
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> leftJoin(@NotNull String table, @NotNull String condition) {
        joins.add("LEFT JOIN " + table + " ON " + condition);
        return this;
    }

    /**
     * Adds a RIGHT JOIN clause.
     *
     * @param table     the table to join
     * @param condition the join condition
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> rightJoin(@NotNull String table, @NotNull String condition) {
        joins.add("RIGHT JOIN " + table + " ON " + condition);
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
     * builder.where("uuid", uuid);  // WHERE uuid = ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> where(@NotNull String column, @Nullable Object value) {
        whereClause.eq(column, value);
        return this;
    }

    /**
     * Adds a WHERE condition with a custom operator.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.where("level", ">", 10);
     * builder.where("balance", ">=", 1000.0);
     * }</pre>
     *
     * @param column   the column name
     * @param operator the SQL operator
     * @param value    the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> where(@NotNull String column, @NotNull String operator, @Nullable Object value) {
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
    public SelectBuilder<T> and(@NotNull String column, @Nullable Object value) {
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
    public SelectBuilder<T> and(@NotNull String column, @NotNull String operator, @Nullable Object value) {
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
    public SelectBuilder<T> or(@NotNull String column, @Nullable Object value) {
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
    public SelectBuilder<T> or(@NotNull String column, @NotNull String operator, @Nullable Object value) {
        whereClause.or().condition(column, operator, value);
        return this;
    }

    /**
     * Adds an IN condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.whereIn("status", List.of("active", "pending"));
     * }</pre>
     *
     * @param column the column name
     * @param values the collection of values
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> whereIn(@NotNull String column, @NotNull Collection<?> values) {
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
    public SelectBuilder<T> whereNotIn(@NotNull String column, @NotNull Collection<?> values) {
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
    public SelectBuilder<T> whereBetween(@NotNull String column, @NotNull Object start, @NotNull Object end) {
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
    public SelectBuilder<T> whereLike(@NotNull String column, @NotNull String pattern) {
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
    public SelectBuilder<T> whereNull(@NotNull String column) {
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
    public SelectBuilder<T> whereNotNull(@NotNull String column) {
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
    public SelectBuilder<T> whereClause(@NotNull Consumer<WhereClause> configurator) {
        configurator.accept(whereClause);
        return this;
    }

    // ========================================================================
    // Ordering
    // ========================================================================

    /**
     * Adds an ORDER BY clause.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.orderBy("balance", Order.DESC);
     * builder.orderBy("name", Order.ASC);
     * }</pre>
     *
     * @param column the column to sort by
     * @param order  the sort order
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> orderBy(@NotNull String column, @NotNull Order order) {
        orderByClauses.add(new OrderBy(column, order));
        return this;
    }

    /**
     * Adds an ORDER BY clause with ascending order.
     *
     * @param column the column to sort by
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> orderByAsc(@NotNull String column) {
        return orderBy(column, Order.ASC);
    }

    /**
     * Adds an ORDER BY clause with descending order.
     *
     * @param column the column to sort by
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> orderByDesc(@NotNull String column) {
        return orderBy(column, Order.DESC);
    }

    // ========================================================================
    // Grouping
    // ========================================================================

    /**
     * Adds a GROUP BY clause.
     *
     * @param columns the columns to group by
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> groupBy(@NotNull String... columns) {
        this.groupBy = String.join(", ", columns);
        return this;
    }

    /**
     * Adds a HAVING clause.
     *
     * @param condition the HAVING condition
     * @param params    the parameters
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> having(@NotNull String condition, @NotNull Object... params) {
        this.having = condition;
        this.havingParams.addAll(List.of(params));
        return this;
    }

    // ========================================================================
    // Pagination
    // ========================================================================

    /**
     * Adds a LIMIT clause.
     *
     * @param limit the maximum number of rows
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Adds an OFFSET clause.
     *
     * @param offset the number of rows to skip
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Sets both LIMIT and OFFSET for pagination.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * // Page 3 with 10 items per page
     * builder.page(3, 10);  // OFFSET 20 LIMIT 10
     * }</pre>
     *
     * @param page     the page number (1-based)
     * @param pageSize the number of items per page
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public SelectBuilder<T> page(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be >= 1");
        }
        this.limit = pageSize;
        this.offset = (page - 1) * pageSize;
        return this;
    }

    // ========================================================================
    // Build SQL
    // ========================================================================

    /**
     * Builds the SQL query string.
     *
     * @return the complete SQL SELECT statement
     * @since 1.0.0
     */
    @NotNull
    public String buildSql() {
        StringBuilder sql = new StringBuilder();

        // SELECT clause
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }
        if (columns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", columns));
        }

        // FROM clause
        sql.append(" FROM ").append(tableName);

        // JOIN clauses
        for (String join : joins) {
            sql.append(" ").append(join);
        }

        // WHERE clause
        if (!whereClause.isEmpty()) {
            sql.append(" ").append(whereClause.toSqlWithWhere());
        }

        // GROUP BY clause
        if (groupBy != null) {
            sql.append(" GROUP BY ").append(groupBy);
        }

        // HAVING clause
        if (having != null) {
            sql.append(" HAVING ").append(having);
        }

        // ORDER BY clause
        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(String.join(", ", orderByClauses.stream().map(OrderBy::toString).toList()));
        }

        // LIMIT and OFFSET
        if (limit != null) {
            if (offset != null && offset > 0) {
                sql.append(" ").append(databaseType.getLimitClause(limit, offset));
            } else {
                sql.append(" LIMIT ").append(limit);
            }
        } else if (offset != null && offset > 0) {
            sql.append(" OFFSET ").append(offset);
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
        List<Object> params = new ArrayList<>(whereClause.getParameters());
        params.addAll(havingParams);
        return params.toArray();
    }

    // ========================================================================
    // Execution
    // ========================================================================

    /**
     * Executes the query and returns all results.
     *
     * @return the list of mapped entities
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public List<T> execute() throws SQLException {
        String sql = buildSql();
        Object[] params = getParameters();

        try (DatabaseConnection conn = connectionProvider.getConnection();
             ResultSet rs = conn.executeQuery(sql, params)) {

            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(entityMapper.mapRow(rs));
            }
            return results;
        }
    }

    /**
     * Executes the query asynchronously and returns all results.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.where("active", true)
     *     .executeAsync()
     *     .thenAccept(players -> {
     *         players.forEach(p -> logger.info("Player: {}", p.getName()));
     *     });
     * }</pre>
     *
     * @return a future that completes with the list of mapped entities
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<T>> executeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute query", e);
            }
        }, asyncExecutor);
    }

    /**
     * Executes the query and returns the first result.
     *
     * @return an Optional containing the first result, or empty if none
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public Optional<T> executeFirst() throws SQLException {
        // Optimize by limiting to 1
        Integer originalLimit = this.limit;
        this.limit = 1;

        try {
            List<T> results = execute();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            this.limit = originalLimit;
        }
    }

    /**
     * Executes the query asynchronously and returns the first result.
     *
     * @return a future that completes with an Optional containing the first result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<T>> executeFirstAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeFirst();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute query", e);
            }
        }, asyncExecutor);
    }

    /**
     * Executes a COUNT query and returns the count.
     *
     * @return the count of matching rows
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public long executeCount() throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableName);

        // JOIN clauses
        for (String join : joins) {
            sql.append(" ").append(join);
        }

        // WHERE clause
        if (!whereClause.isEmpty()) {
            sql.append(" ").append(whereClause.toSqlWithWhere());
        }

        try (DatabaseConnection conn = connectionProvider.getConnection();
             ResultSet rs = conn.executeQuery(sql.toString(), whereClause.getParameterArray())) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    /**
     * Executes a COUNT query asynchronously.
     *
     * @return a future that completes with the count
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> executeCountAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeCount();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute count query", e);
            }
        }, asyncExecutor);
    }

    /**
     * Checks if any rows match the query conditions.
     *
     * @return true if at least one row matches
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public boolean exists() throws SQLException {
        return executeCount() > 0;
    }

    /**
     * Checks if any rows match asynchronously.
     *
     * @return a future that completes with true if at least one row matches
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> existsAsync() {
        return executeCountAsync().thenApply(count -> count > 0);
    }

    @Override
    public String toString() {
        return "SelectBuilder{sql='" + buildSql() + "', parameters=" + List.of(getParameters()) + "}";
    }
}
