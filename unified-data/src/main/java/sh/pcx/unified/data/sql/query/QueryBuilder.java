/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.query;

import sh.pcx.unified.data.sql.DatabaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Fluent SQL query builder for constructing SQL statements.
 *
 * <p>This class provides a type-safe, fluent API for building SQL queries
 * with proper parameter binding to prevent SQL injection attacks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Build a SELECT query
 * QueryBuilder query = QueryBuilder.create()
 *     .select("id", "name", "balance")
 *     .from("players")
 *     .where("uuid", uuid)
 *     .and("active", true)
 *     .orderBy("balance", Order.DESC)
 *     .limit(10);
 *
 * String sql = query.build();          // SELECT id, name, balance FROM players WHERE uuid = ? AND active = ? ORDER BY balance DESC LIMIT 10
 * Object[] params = query.getParameters();
 *
 * // Build an INSERT query
 * QueryBuilder insert = QueryBuilder.create()
 *     .insertInto("players")
 *     .columns("uuid", "name", "balance")
 *     .values(uuid, "Steve", 1000.0);
 *
 * // Build an UPDATE query
 * QueryBuilder update = QueryBuilder.create()
 *     .update("players")
 *     .set("balance", 1500.0)
 *     .set("last_login", Instant.now())
 *     .where("uuid", uuid);
 *
 * // Build a DELETE query
 * QueryBuilder delete = QueryBuilder.create()
 *     .deleteFrom("players")
 *     .where("inactive_days", ">", 365);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Create new instances for each query.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SelectBuilder
 * @see InsertBuilder
 * @see UpdateBuilder
 * @see DeleteBuilder
 */
public class QueryBuilder {

    /**
     * Sort order for ORDER BY clauses.
     */
    public enum Order {
        /** Ascending order (A-Z, 0-9). */
        ASC("ASC"),
        /** Descending order (Z-A, 9-0). */
        DESC("DESC");

        private final String sql;

        Order(String sql) {
            this.sql = sql;
        }

        @Override
        public String toString() {
            return sql;
        }
    }

    private final StringBuilder sql;
    private final List<Object> parameters;
    private final WhereClause whereClause;
    private DatabaseType databaseType;
    private QueryType queryType;

    private enum QueryType {
        SELECT, INSERT, UPDATE, DELETE, RAW
    }

    /**
     * Creates a new query builder.
     *
     * @since 1.0.0
     */
    public QueryBuilder() {
        this.sql = new StringBuilder();
        this.parameters = new ArrayList<>();
        this.whereClause = new WhereClause();
        this.databaseType = DatabaseType.MYSQL; // Default
    }

    /**
     * Factory method to create a new query builder.
     *
     * @return a new QueryBuilder instance
     * @since 1.0.0
     */
    @NotNull
    public static QueryBuilder create() {
        return new QueryBuilder();
    }

    /**
     * Factory method to create a query builder for a specific database type.
     *
     * @param databaseType the target database type
     * @return a new QueryBuilder instance
     * @since 1.0.0
     */
    @NotNull
    public static QueryBuilder create(@NotNull DatabaseType databaseType) {
        QueryBuilder builder = new QueryBuilder();
        builder.databaseType = databaseType;
        return builder;
    }

    // ========================================================================
    // SELECT Operations
    // ========================================================================

    /**
     * Starts a SELECT query with the specified columns.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.select("id", "name", "balance")  // SELECT id, name, balance
     * builder.select("*")                      // SELECT *
     * }</pre>
     *
     * @param columns the columns to select
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder select(@NotNull String... columns) {
        queryType = QueryType.SELECT;
        sql.append("SELECT ").append(String.join(", ", columns));
        return this;
    }

    /**
     * Starts a SELECT DISTINCT query.
     *
     * @param columns the columns to select
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder selectDistinct(@NotNull String... columns) {
        queryType = QueryType.SELECT;
        sql.append("SELECT DISTINCT ").append(String.join(", ", columns));
        return this;
    }

    /**
     * Starts a SELECT COUNT query.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.selectCount().from("players").where("active", true);
     * // SELECT COUNT(*) FROM players WHERE active = ?
     * }</pre>
     *
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder selectCount() {
        return selectCount("*");
    }

    /**
     * Starts a SELECT COUNT query for a specific column.
     *
     * @param column the column to count
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder selectCount(@NotNull String column) {
        queryType = QueryType.SELECT;
        sql.append("SELECT COUNT(").append(column).append(")");
        return this;
    }

    /**
     * Adds the FROM clause.
     *
     * @param table the table name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder from(@NotNull String table) {
        sql.append(" FROM ").append(table);
        return this;
    }

    /**
     * Adds a JOIN clause.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.select("p.name", "s.value")
     *     .from("players p")
     *     .join("statistics s", "p.id = s.player_id");
     * }</pre>
     *
     * @param table     the table to join
     * @param condition the join condition
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder join(@NotNull String table, @NotNull String condition) {
        sql.append(" JOIN ").append(table).append(" ON ").append(condition);
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
    public QueryBuilder leftJoin(@NotNull String table, @NotNull String condition) {
        sql.append(" LEFT JOIN ").append(table).append(" ON ").append(condition);
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
    public QueryBuilder rightJoin(@NotNull String table, @NotNull String condition) {
        sql.append(" RIGHT JOIN ").append(table).append(" ON ").append(condition);
        return this;
    }

    // ========================================================================
    // INSERT Operations
    // ========================================================================

    /**
     * Starts an INSERT query.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.insertInto("players")
     *     .columns("uuid", "name", "balance")
     *     .values(uuid, "Steve", 1000.0);
     * }</pre>
     *
     * @param table the table name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder insertInto(@NotNull String table) {
        queryType = QueryType.INSERT;
        sql.append("INSERT INTO ").append(table);
        return this;
    }

    /**
     * Specifies the columns for an INSERT query.
     *
     * @param columns the column names
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder columns(@NotNull String... columns) {
        sql.append(" (").append(String.join(", ", columns)).append(")");
        return this;
    }

    /**
     * Specifies the values for an INSERT query.
     *
     * @param values the values to insert
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder values(@NotNull Object... values) {
        String placeholders = String.join(", ", Arrays.stream(values).map(v -> "?").toList());
        sql.append(" VALUES (").append(placeholders).append(")");
        parameters.addAll(Arrays.asList(values));
        return this;
    }

    /**
     * Adds an ON DUPLICATE KEY UPDATE clause (MySQL/MariaDB) or equivalent.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.insertInto("players")
     *     .columns("uuid", "name", "balance")
     *     .values(uuid, "Steve", 1000.0)
     *     .onDuplicateKeyUpdate("name", "Steve")
     *     .onDuplicateKeyUpdate("balance", 1000.0);
     * }</pre>
     *
     * @param column the column to update
     * @param value  the new value
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder onDuplicateKeyUpdate(@NotNull String column, @Nullable Object value) {
        if (!sql.toString().contains("ON DUPLICATE KEY UPDATE")) {
            sql.append(" ON DUPLICATE KEY UPDATE ");
        } else {
            sql.append(", ");
        }
        sql.append(column).append(" = ?");
        parameters.add(value);
        return this;
    }

    // ========================================================================
    // UPDATE Operations
    // ========================================================================

    /**
     * Starts an UPDATE query.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.update("players")
     *     .set("balance", 1500.0)
     *     .set("last_login", Instant.now())
     *     .where("uuid", uuid);
     * }</pre>
     *
     * @param table the table name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder update(@NotNull String table) {
        queryType = QueryType.UPDATE;
        sql.append("UPDATE ").append(table);
        return this;
    }

    /**
     * Adds a SET clause to an UPDATE query.
     *
     * @param column the column to update
     * @param value  the new value
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder set(@NotNull String column, @Nullable Object value) {
        if (!sql.toString().contains(" SET ")) {
            sql.append(" SET ");
        } else {
            sql.append(", ");
        }
        sql.append(column).append(" = ?");
        parameters.add(value);
        return this;
    }

    /**
     * Adds a SET clause that increments a column.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.update("players").increment("kills", 1).where("uuid", uuid);
     * // UPDATE players SET kills = kills + ? WHERE uuid = ?
     * }</pre>
     *
     * @param column the column to increment
     * @param value  the value to add
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder increment(@NotNull String column, @NotNull Number value) {
        if (!sql.toString().contains(" SET ")) {
            sql.append(" SET ");
        } else {
            sql.append(", ");
        }
        sql.append(column).append(" = ").append(column).append(" + ?");
        parameters.add(value);
        return this;
    }

    /**
     * Adds a SET clause that decrements a column.
     *
     * @param column the column to decrement
     * @param value  the value to subtract
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder decrement(@NotNull String column, @NotNull Number value) {
        if (!sql.toString().contains(" SET ")) {
            sql.append(" SET ");
        } else {
            sql.append(", ");
        }
        sql.append(column).append(" = ").append(column).append(" - ?");
        parameters.add(value);
        return this;
    }

    // ========================================================================
    // DELETE Operations
    // ========================================================================

    /**
     * Starts a DELETE query.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.deleteFrom("sessions")
     *     .where("expired_at", "<", Instant.now());
     * }</pre>
     *
     * @param table the table name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder deleteFrom(@NotNull String table) {
        queryType = QueryType.DELETE;
        sql.append("DELETE FROM ").append(table);
        return this;
    }

    // ========================================================================
    // WHERE Clause
    // ========================================================================

    /**
     * Adds a WHERE condition with equality.
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder where(@NotNull String column, @Nullable Object value) {
        whereClause.eq(column, value);
        return this;
    }

    /**
     * Adds a WHERE condition with a custom operator.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.select("*").from("players")
     *     .where("level", ">", 10)
     *     .and("active", "=", true);
     * }</pre>
     *
     * @param column   the column name
     * @param operator the SQL operator
     * @param value    the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder where(@NotNull String column, @NotNull String operator, @Nullable Object value) {
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
    public QueryBuilder and(@NotNull String column, @Nullable Object value) {
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
    public QueryBuilder and(@NotNull String column, @NotNull String operator, @Nullable Object value) {
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
    public QueryBuilder or(@NotNull String column, @Nullable Object value) {
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
    public QueryBuilder or(@NotNull String column, @NotNull String operator, @Nullable Object value) {
        whereClause.or().condition(column, operator, value);
        return this;
    }

    /**
     * Provides access to the underlying WHERE clause builder.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.select("*").from("players")
     *     .whereClause(w -> w
     *         .eq("active", true)
     *         .and().in("rank", List.of("GOLD", "PLATINUM"))
     *         .or().gt("level", 50)
     *     );
     * }</pre>
     *
     * @param configurator a function to configure the WHERE clause
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder whereClause(@NotNull java.util.function.Consumer<WhereClause> configurator) {
        configurator.accept(whereClause);
        return this;
    }

    // ========================================================================
    // Sorting and Pagination
    // ========================================================================

    /**
     * Adds an ORDER BY clause.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * builder.select("*").from("players").orderBy("balance", Order.DESC);
     * }</pre>
     *
     * @param column the column to sort by
     * @param order  the sort order
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder orderBy(@NotNull String column, @NotNull Order order) {
        if (!sql.toString().contains("ORDER BY")) {
            sql.append(" ORDER BY ");
        } else {
            sql.append(", ");
        }
        sql.append(column).append(" ").append(order);
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
    public QueryBuilder orderByAsc(@NotNull String column) {
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
    public QueryBuilder orderByDesc(@NotNull String column) {
        return orderBy(column, Order.DESC);
    }

    /**
     * Adds a LIMIT clause.
     *
     * @param limit the maximum number of rows
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder limit(int limit) {
        sql.append(" LIMIT ").append(limit);
        return this;
    }

    /**
     * Adds a LIMIT clause with offset.
     *
     * @param limit  the maximum number of rows
     * @param offset the number of rows to skip
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder limit(int limit, int offset) {
        sql.append(" ").append(databaseType.getLimitClause(limit, offset));
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
    public QueryBuilder offset(int offset) {
        sql.append(" OFFSET ").append(offset);
        return this;
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
    public QueryBuilder groupBy(@NotNull String... columns) {
        sql.append(" GROUP BY ").append(String.join(", ", columns));
        return this;
    }

    /**
     * Adds a HAVING clause.
     *
     * @param condition the HAVING condition
     * @param params    the parameters for the condition
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder having(@NotNull String condition, @NotNull Object... params) {
        sql.append(" HAVING ").append(condition);
        parameters.addAll(Arrays.asList(params));
        return this;
    }

    // ========================================================================
    // Build Methods
    // ========================================================================

    /**
     * Builds and returns the final SQL string.
     *
     * @return the complete SQL statement
     * @since 1.0.0
     */
    @NotNull
    public String build() {
        StringBuilder result = new StringBuilder(sql);

        // Append WHERE clause if present
        if (!whereClause.isEmpty()) {
            result.append(" ").append(whereClause.toSqlWithWhere());
        }

        return result.toString();
    }

    /**
     * Returns all bound parameters in order.
     *
     * @return the list of parameters
     * @since 1.0.0
     */
    @NotNull
    public List<Object> getParametersList() {
        List<Object> allParams = new ArrayList<>(parameters);
        allParams.addAll(whereClause.getParameters());
        return allParams;
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

    /**
     * Sets the target database type for database-specific SQL generation.
     *
     * @param databaseType the database type
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public QueryBuilder forDatabase(@NotNull DatabaseType databaseType) {
        this.databaseType = Objects.requireNonNull(databaseType);
        return this;
    }

    /**
     * Returns the current database type.
     *
     * @return the database type
     * @since 1.0.0
     */
    @NotNull
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public String toString() {
        return "QueryBuilder{sql='" + build() + "', parameters=" + getParametersList() + "}";
    }
}
