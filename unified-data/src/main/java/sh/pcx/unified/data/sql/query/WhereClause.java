/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for SQL WHERE clauses.
 *
 * <p>This class provides a type-safe way to construct WHERE clauses with
 * proper parameter binding to prevent SQL injection attacks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple equality
 * WhereClause where = WhereClause.create()
 *     .eq("uuid", uuid)
 *     .and().eq("active", true);
 *
 * // Complex conditions
 * WhereClause where = WhereClause.create()
 *     .eq("status", "active")
 *     .and().gt("level", 10)
 *     .and().in("rank", List.of("GOLD", "PLATINUM", "DIAMOND"))
 *     .or().isNull("expiry_date");
 *
 * // Using operators
 * WhereClause where = WhereClause.create()
 *     .condition("balance", ">=", 1000)
 *     .and().condition("created_at", ">", startDate)
 *     .and().like("name", "%Steve%");
 *
 * // Nested conditions (grouping)
 * WhereClause where = WhereClause.create()
 *     .eq("type", "player")
 *     .and().group(w -> w
 *         .gt("kills", 100)
 *         .or().gt("deaths", 50)
 *     );
 * // Results in: type = ? AND (kills > ? OR deaths > ?)
 *
 * // Get the clause and parameters
 * String sql = where.toSql();           // "uuid = ? AND active = ?"
 * List<Object> params = where.getParameters();
 * }</pre>
 *
 * <h2>Supported Operators</h2>
 * <ul>
 *   <li>{@code =} (eq) - Equality</li>
 *   <li>{@code <>} (ne) - Not equal</li>
 *   <li>{@code >} (gt) - Greater than</li>
 *   <li>{@code >=} (gte) - Greater than or equal</li>
 *   <li>{@code <} (lt) - Less than</li>
 *   <li>{@code <=} (lte) - Less than or equal</li>
 *   <li>{@code LIKE} - Pattern matching</li>
 *   <li>{@code IN} - Set membership</li>
 *   <li>{@code BETWEEN} - Range check</li>
 *   <li>{@code IS NULL} / {@code IS NOT NULL} - Null checks</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Create new instances for each query.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class WhereClause {

    private final StringBuilder clause;
    private final List<Object> parameters;
    private boolean needsConnector;

    /**
     * Creates a new empty WHERE clause builder.
     *
     * @since 1.0.0
     */
    public WhereClause() {
        this.clause = new StringBuilder();
        this.parameters = new ArrayList<>();
        this.needsConnector = false;
    }

    /**
     * Factory method to create a new WHERE clause builder.
     *
     * @return a new WhereClause instance
     * @since 1.0.0
     */
    @NotNull
    public static WhereClause create() {
        return new WhereClause();
    }

    // ========================================================================
    // Equality Operations
    // ========================================================================

    /**
     * Adds an equality condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.eq("uuid", uuid);  // uuid = ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause eq(@NotNull String column, @Nullable Object value) {
        return condition(column, "=", value);
    }

    /**
     * Adds a not-equal condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.ne("status", "banned");  // status <> ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause ne(@NotNull String column, @Nullable Object value) {
        return condition(column, "<>", value);
    }

    // ========================================================================
    // Comparison Operations
    // ========================================================================

    /**
     * Adds a greater-than condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.gt("level", 10);  // level > ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause gt(@NotNull String column, @Nullable Object value) {
        return condition(column, ">", value);
    }

    /**
     * Adds a greater-than-or-equal condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.gte("balance", 1000);  // balance >= ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause gte(@NotNull String column, @Nullable Object value) {
        return condition(column, ">=", value);
    }

    /**
     * Adds a less-than condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.lt("deaths", 5);  // deaths < ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause lt(@NotNull String column, @Nullable Object value) {
        return condition(column, "<", value);
    }

    /**
     * Adds a less-than-or-equal condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.lte("age", 18);  // age <= ?
     * }</pre>
     *
     * @param column the column name
     * @param value  the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause lte(@NotNull String column, @Nullable Object value) {
        return condition(column, "<=", value);
    }

    /**
     * Adds a generic condition with any operator.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.condition("level", ">", 10);
     * where.condition("created_at", ">=", startDate);
     * }</pre>
     *
     * @param column   the column name
     * @param operator the SQL operator (=, <>, >, >=, <, <=)
     * @param value    the value to compare
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause condition(@NotNull String column, @NotNull String operator, @Nullable Object value) {
        Objects.requireNonNull(column, "Column cannot be null");
        Objects.requireNonNull(operator, "Operator cannot be null");

        appendCondition(column + " " + operator + " ?");
        parameters.add(value);
        return this;
    }

    // ========================================================================
    // Pattern Matching
    // ========================================================================

    /**
     * Adds a LIKE condition for pattern matching.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.like("name", "Steve%");     // name LIKE ?
     * where.like("email", "%@gmail.com");
     * }</pre>
     *
     * <p>Use {@code %} for any sequence and {@code _} for single character.
     *
     * @param column  the column name
     * @param pattern the LIKE pattern
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause like(@NotNull String column, @NotNull String pattern) {
        appendCondition(column + " LIKE ?");
        parameters.add(pattern);
        return this;
    }

    /**
     * Adds a NOT LIKE condition.
     *
     * @param column  the column name
     * @param pattern the LIKE pattern
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause notLike(@NotNull String column, @NotNull String pattern) {
        appendCondition(column + " NOT LIKE ?");
        parameters.add(pattern);
        return this;
    }

    /**
     * Adds a case-insensitive LIKE condition (using LOWER).
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.likeIgnoreCase("name", "%steve%");  // LOWER(name) LIKE LOWER(?)
     * }</pre>
     *
     * @param column  the column name
     * @param pattern the LIKE pattern (will be lowercased)
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause likeIgnoreCase(@NotNull String column, @NotNull String pattern) {
        appendCondition("LOWER(" + column + ") LIKE LOWER(?)");
        parameters.add(pattern);
        return this;
    }

    // ========================================================================
    // Set Operations
    // ========================================================================

    /**
     * Adds an IN condition for set membership.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.in("rank", List.of("GOLD", "PLATINUM", "DIAMOND"));
     * // Results in: rank IN (?, ?, ?)
     * }</pre>
     *
     * @param column the column name
     * @param values the collection of values
     * @return this builder for chaining
     * @throws IllegalArgumentException if values is empty
     * @since 1.0.0
     */
    @NotNull
    public WhereClause in(@NotNull String column, @NotNull Collection<?> values) {
        Objects.requireNonNull(values, "Values cannot be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN clause requires at least one value");
        }

        String placeholders = String.join(", ", values.stream().map(v -> "?").toList());
        appendCondition(column + " IN (" + placeholders + ")");
        parameters.addAll(values);
        return this;
    }

    /**
     * Adds an IN condition with varargs.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.in("status", "active", "pending", "review");
     * }</pre>
     *
     * @param column the column name
     * @param values the values
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause in(@NotNull String column, @NotNull Object... values) {
        return in(column, Arrays.asList(values));
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
    public WhereClause notIn(@NotNull String column, @NotNull Collection<?> values) {
        Objects.requireNonNull(values, "Values cannot be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("NOT IN clause requires at least one value");
        }

        String placeholders = String.join(", ", values.stream().map(v -> "?").toList());
        appendCondition(column + " NOT IN (" + placeholders + ")");
        parameters.addAll(values);
        return this;
    }

    // ========================================================================
    // Range Operations
    // ========================================================================

    /**
     * Adds a BETWEEN condition for range checks.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.between("level", 10, 50);  // level BETWEEN ? AND ?
     * where.between("created_at", startDate, endDate);
     * }</pre>
     *
     * @param column the column name
     * @param start  the range start (inclusive)
     * @param end    the range end (inclusive)
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause between(@NotNull String column, @NotNull Object start, @NotNull Object end) {
        appendCondition(column + " BETWEEN ? AND ?");
        parameters.add(start);
        parameters.add(end);
        return this;
    }

    /**
     * Adds a NOT BETWEEN condition.
     *
     * @param column the column name
     * @param start  the range start
     * @param end    the range end
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause notBetween(@NotNull String column, @NotNull Object start, @NotNull Object end) {
        appendCondition(column + " NOT BETWEEN ? AND ?");
        parameters.add(start);
        parameters.add(end);
        return this;
    }

    // ========================================================================
    // Null Checks
    // ========================================================================

    /**
     * Adds an IS NULL condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.isNull("deleted_at");  // deleted_at IS NULL
     * }</pre>
     *
     * @param column the column name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause isNull(@NotNull String column) {
        appendCondition(column + " IS NULL");
        return this;
    }

    /**
     * Adds an IS NOT NULL condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.isNotNull("email");  // email IS NOT NULL
     * }</pre>
     *
     * @param column the column name
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause isNotNull(@NotNull String column) {
        appendCondition(column + " IS NOT NULL");
        return this;
    }

    // ========================================================================
    // Logical Connectors
    // ========================================================================

    /**
     * Adds an AND connector for the next condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.eq("active", true).and().gt("level", 10);
     * // Results in: active = ? AND level > ?
     * }</pre>
     *
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause and() {
        if (clause.length() > 0 && !needsConnector) {
            clause.append(" AND ");
            needsConnector = true;
        }
        return this;
    }

    /**
     * Adds an OR connector for the next condition.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.eq("rank", "ADMIN").or().eq("rank", "MODERATOR");
     * // Results in: rank = ? OR rank = ?
     * }</pre>
     *
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause or() {
        if (clause.length() > 0 && !needsConnector) {
            clause.append(" OR ");
            needsConnector = true;
        }
        return this;
    }

    // ========================================================================
    // Grouping (Nested Conditions)
    // ========================================================================

    /**
     * Adds a grouped (parenthesized) condition using a builder function.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.eq("type", "player")
     *     .and().group(w -> w
     *         .gt("kills", 100)
     *         .or().gt("deaths", 50)
     *     );
     * // Results in: type = ? AND (kills > ? OR deaths > ?)
     * }</pre>
     *
     * @param groupBuilder a function that builds the grouped conditions
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause group(@NotNull java.util.function.Consumer<WhereClause> groupBuilder) {
        WhereClause nested = new WhereClause();
        groupBuilder.accept(nested);

        if (!nested.isEmpty()) {
            appendCondition("(" + nested.toSql() + ")");
            parameters.addAll(nested.getParameters());
        }
        return this;
    }

    /**
     * Adds raw SQL to the WHERE clause.
     *
     * <p><strong>Warning:</strong> This method does not escape input. Only use
     * with trusted, constant strings. Never use with user input.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.raw("YEAR(created_at) = 2024");
     * }</pre>
     *
     * @param sql the raw SQL condition
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause raw(@NotNull String sql) {
        appendCondition(sql);
        return this;
    }

    /**
     * Adds raw SQL with parameters.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * where.raw("YEAR(created_at) = ?", 2024);
     * }</pre>
     *
     * @param sql    the raw SQL with placeholders
     * @param params the parameters
     * @return this builder for chaining
     * @since 1.0.0
     */
    @NotNull
    public WhereClause raw(@NotNull String sql, @NotNull Object... params) {
        appendCondition(sql);
        parameters.addAll(Arrays.asList(params));
        return this;
    }

    // ========================================================================
    // Output Methods
    // ========================================================================

    /**
     * Returns the SQL WHERE clause (without the WHERE keyword).
     *
     * @return the SQL clause string
     * @since 1.0.0
     */
    @NotNull
    public String toSql() {
        return clause.toString();
    }

    /**
     * Returns the SQL WHERE clause with the WHERE keyword.
     *
     * @return the complete WHERE clause, or empty string if no conditions
     * @since 1.0.0
     */
    @NotNull
    public String toSqlWithWhere() {
        if (isEmpty()) {
            return "";
        }
        return "WHERE " + clause.toString();
    }

    /**
     * Returns the bound parameters in order.
     *
     * @return the list of parameters
     * @since 1.0.0
     */
    @NotNull
    public List<Object> getParameters() {
        return new ArrayList<>(parameters);
    }

    /**
     * Returns the bound parameters as an array.
     *
     * @return the parameter array
     * @since 1.0.0
     */
    @NotNull
    public Object[] getParameterArray() {
        return parameters.toArray();
    }

    /**
     * Checks if this WHERE clause is empty (no conditions).
     *
     * @return true if no conditions have been added
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return clause.length() == 0;
    }

    /**
     * Returns the number of conditions in this clause.
     *
     * @return the condition count
     * @since 1.0.0
     */
    public int getConditionCount() {
        return parameters.size();
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Appends a condition to the clause, adding AND if needed.
     */
    private void appendCondition(@NotNull String condition) {
        if (clause.length() > 0 && !needsConnector) {
            clause.append(" AND ");
        }
        clause.append(condition);
        needsConnector = false;
    }

    @Override
    public String toString() {
        return "WhereClause{clause='" + clause + "', parameters=" + parameters + "}";
    }
}
