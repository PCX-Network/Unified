/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Objects;

/**
 * A wrapper around {@link Connection} that provides auto-close functionality
 * and additional convenience methods.
 *
 * <p>This class implements {@link AutoCloseable} and is designed to be used
 * with try-with-resources blocks to ensure proper connection cleanup.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Using try-with-resources
 * try (DatabaseConnection conn = connectionProvider.getConnection()) {
 *     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?");
 *     stmt.setString(1, uuid.toString());
 *     ResultSet rs = stmt.executeQuery();
 *
 *     while (rs.next()) {
 *         // Process results
 *     }
 * } // Connection automatically returned to pool
 *
 * // Transaction support
 * try (DatabaseConnection conn = connectionProvider.getConnection()) {
 *     conn.beginTransaction();
 *     try {
 *         conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE id = ?")
 *             .execute(amount, fromId);
 *         conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = ?")
 *             .execute(amount, toId);
 *         conn.commit();
 *     } catch (SQLException e) {
 *         conn.rollback();
 *         throw e;
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Each thread should obtain its own connection.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DatabaseConnection implements AutoCloseable {

    private final Connection connection;
    private final DatabaseType databaseType;
    private final long acquiredAt;
    private boolean inTransaction;

    /**
     * Creates a new database connection wrapper.
     *
     * @param connection   the underlying JDBC connection
     * @param databaseType the type of database
     * @throws NullPointerException if connection or databaseType is null
     * @since 1.0.0
     */
    public DatabaseConnection(@NotNull Connection connection, @NotNull DatabaseType databaseType) {
        this.connection = Objects.requireNonNull(connection, "Connection cannot be null");
        this.databaseType = Objects.requireNonNull(databaseType, "Database type cannot be null");
        this.acquiredAt = System.currentTimeMillis();
        this.inTransaction = false;
    }

    /**
     * Returns the underlying JDBC connection.
     *
     * <p><strong>Warning:</strong> Direct manipulation of the underlying connection
     * may interfere with connection pool management. Use with caution.
     *
     * @return the raw JDBC connection
     * @since 1.0.0
     */
    @NotNull
    public Connection getConnection() {
        return connection;
    }

    /**
     * Returns the database type for this connection.
     *
     * @return the database type
     * @since 1.0.0
     */
    @NotNull
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * Returns when this connection was acquired from the pool.
     *
     * @return the acquisition timestamp in milliseconds
     * @since 1.0.0
     */
    public long getAcquiredAt() {
        return acquiredAt;
    }

    /**
     * Returns how long this connection has been held.
     *
     * @return the duration in milliseconds
     * @since 1.0.0
     */
    public long getHoldDuration() {
        return System.currentTimeMillis() - acquiredAt;
    }

    /**
     * Checks if the connection is currently in a transaction.
     *
     * @return true if a transaction is active
     * @since 1.0.0
     */
    public boolean isInTransaction() {
        return inTransaction;
    }

    /**
     * Checks if the connection is still valid.
     *
     * @param timeout the timeout in seconds for validation
     * @return true if the connection is valid
     * @since 1.0.0
     */
    public boolean isValid(int timeout) {
        try {
            return connection.isValid(timeout);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Checks if the connection is closed.
     *
     * @return true if the connection is closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    // ========================================================================
    // Transaction Management
    // ========================================================================

    /**
     * Begins a new transaction.
     *
     * <p>This disables auto-commit mode. You must call either {@link #commit()}
     * or {@link #rollback()} to complete the transaction.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * conn.beginTransaction();
     * try {
     *     // Execute multiple statements
     *     conn.commit();
     * } catch (SQLException e) {
     *     conn.rollback();
     *     throw e;
     * }
     * }</pre>
     *
     * @throws SQLException if a database error occurs
     * @throws IllegalStateException if a transaction is already active
     * @since 1.0.0
     */
    public void beginTransaction() throws SQLException {
        if (inTransaction) {
            throw new IllegalStateException("Transaction already in progress");
        }
        connection.setAutoCommit(false);
        inTransaction = true;
    }

    /**
     * Commits the current transaction.
     *
     * @throws SQLException if a database error occurs
     * @throws IllegalStateException if no transaction is active
     * @since 1.0.0
     */
    public void commit() throws SQLException {
        if (!inTransaction) {
            throw new IllegalStateException("No transaction in progress");
        }
        try {
            connection.commit();
        } finally {
            connection.setAutoCommit(true);
            inTransaction = false;
        }
    }

    /**
     * Rolls back the current transaction.
     *
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public void rollback() throws SQLException {
        if (!inTransaction) {
            return; // Nothing to rollback
        }
        try {
            connection.rollback();
        } finally {
            connection.setAutoCommit(true);
            inTransaction = false;
        }
    }

    /**
     * Creates a savepoint within the current transaction.
     *
     * @param name the savepoint name
     * @return the savepoint
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public Savepoint createSavepoint(@NotNull String name) throws SQLException {
        return connection.setSavepoint(name);
    }

    /**
     * Rolls back to a savepoint.
     *
     * @param savepoint the savepoint to rollback to
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public void rollbackTo(@NotNull Savepoint savepoint) throws SQLException {
        connection.rollback(savepoint);
    }

    /**
     * Releases a savepoint.
     *
     * @param savepoint the savepoint to release
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public void releaseSavepoint(@NotNull Savepoint savepoint) throws SQLException {
        connection.releaseSavepoint(savepoint);
    }

    // ========================================================================
    // Statement Creation
    // ========================================================================

    /**
     * Creates a prepared statement with the given SQL.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * PreparedStatement stmt = conn.prepareStatement(
     *     "SELECT * FROM players WHERE uuid = ?"
     * );
     * stmt.setString(1, uuid.toString());
     * ResultSet rs = stmt.executeQuery();
     * }</pre>
     *
     * @param sql the SQL statement with parameter placeholders
     * @return the prepared statement
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public PreparedStatement prepareStatement(@NotNull String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    /**
     * Creates a prepared statement that returns generated keys.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * PreparedStatement stmt = conn.prepareStatementWithGeneratedKeys(
     *     "INSERT INTO players (name) VALUES (?)"
     * );
     * stmt.setString(1, "Steve");
     * stmt.executeUpdate();
     * ResultSet keys = stmt.getGeneratedKeys();
     * if (keys.next()) {
     *     long id = keys.getLong(1);
     * }
     * }</pre>
     *
     * @param sql the SQL statement
     * @return the prepared statement configured to return generated keys
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public PreparedStatement prepareStatementWithGeneratedKeys(@NotNull String sql) throws SQLException {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * Creates a callable statement for stored procedures.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * CallableStatement stmt = conn.prepareCall("{call update_balance(?, ?)}");
     * stmt.setString(1, uuid.toString());
     * stmt.setDouble(2, 100.0);
     * stmt.execute();
     * }</pre>
     *
     * @param sql the SQL call statement
     * @return the callable statement
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public CallableStatement prepareCall(@NotNull String sql) throws SQLException {
        return connection.prepareCall(sql);
    }

    /**
     * Creates a statement for simple SQL execution.
     *
     * <p><strong>Warning:</strong> Prefer {@link #prepareStatement(String)} with
     * parameter binding to prevent SQL injection.
     *
     * @return a new statement
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    // ========================================================================
    // Convenience Execute Methods
    // ========================================================================

    /**
     * Executes an update with the given SQL and parameters.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * int affected = conn.executeUpdate(
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
    public int executeUpdate(@NotNull String sql, @Nullable Object... params) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(sql)) {
            bindParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    /**
     * Executes a query with the given SQL and parameters.
     *
     * <p><strong>Note:</strong> The caller is responsible for closing the returned
     * ResultSet. Consider using try-with-resources.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * try (ResultSet rs = conn.executeQuery(
     *         "SELECT * FROM players WHERE uuid = ?",
     *         uuid.toString())) {
     *     while (rs.next()) {
     *         // Process row
     *     }
     * }
     * }</pre>
     *
     * @param sql    the SQL query
     * @param params the parameters to bind
     * @return the result set (caller must close)
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public ResultSet executeQuery(@NotNull String sql, @Nullable Object... params) throws SQLException {
        PreparedStatement stmt = prepareStatement(sql);
        bindParameters(stmt, params);
        return stmt.executeQuery();
    }

    /**
     * Executes an insert and returns the generated key.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Long id = conn.executeInsert(
     *     "INSERT INTO players (name, uuid) VALUES (?, ?)",
     *     name, uuid.toString()
     * );
     * }</pre>
     *
     * @param sql    the INSERT SQL statement
     * @param params the parameters to bind
     * @return the generated key, or null if none was generated
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @Nullable
    public Long executeInsert(@NotNull String sql, @Nullable Object... params) throws SQLException {
        try (PreparedStatement stmt = prepareStatementWithGeneratedKeys(sql)) {
            bindParameters(stmt, params);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return null;
    }

    /**
     * Executes a batch of updates.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * List<Object[]> batchParams = new ArrayList<>();
     * batchParams.add(new Object[]{"Steve", uuid1.toString()});
     * batchParams.add(new Object[]{"Alex", uuid2.toString()});
     *
     * int[] results = conn.executeBatch(
     *     "INSERT INTO players (name, uuid) VALUES (?, ?)",
     *     batchParams
     * );
     * }</pre>
     *
     * @param sql         the SQL statement
     * @param batchParams list of parameter arrays for each batch item
     * @return an array of update counts
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public int[] executeBatch(@NotNull String sql, @NotNull Iterable<Object[]> batchParams) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(sql)) {
            for (Object[] params : batchParams) {
                bindParameters(stmt, params);
                stmt.addBatch();
            }
            return stmt.executeBatch();
        }
    }

    // ========================================================================
    // Metadata
    // ========================================================================

    /**
     * Returns the database metadata.
     *
     * @return the database metadata
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName the table name to check
     * @return true if the table exists
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    public boolean tableExists(@NotNull String tableName) throws SQLException {
        try (ResultSet rs = getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Binds parameters to a prepared statement.
     *
     * @param stmt   the statement
     * @param params the parameters to bind
     * @throws SQLException if a database error occurs
     */
    private void bindParameters(@NotNull PreparedStatement stmt, @Nullable Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (param instanceof String s) {
                stmt.setString(i + 1, s);
            } else if (param instanceof Integer n) {
                stmt.setInt(i + 1, n);
            } else if (param instanceof Long n) {
                stmt.setLong(i + 1, n);
            } else if (param instanceof Double n) {
                stmt.setDouble(i + 1, n);
            } else if (param instanceof Float n) {
                stmt.setFloat(i + 1, n);
            } else if (param instanceof Boolean b) {
                stmt.setBoolean(i + 1, b);
            } else if (param instanceof byte[] bytes) {
                stmt.setBytes(i + 1, bytes);
            } else if (param instanceof java.util.Date date) {
                stmt.setTimestamp(i + 1, new Timestamp(date.getTime()));
            } else if (param instanceof java.time.Instant instant) {
                stmt.setTimestamp(i + 1, Timestamp.from(instant));
            } else if (param instanceof java.time.LocalDateTime ldt) {
                stmt.setTimestamp(i + 1, Timestamp.valueOf(ldt));
            } else if (param instanceof java.time.LocalDate ld) {
                stmt.setDate(i + 1, java.sql.Date.valueOf(ld));
            } else if (param instanceof java.util.UUID uuid) {
                stmt.setString(i + 1, uuid.toString());
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }

    // ========================================================================
    // AutoCloseable
    // ========================================================================

    /**
     * Closes this connection, returning it to the pool.
     *
     * <p>If a transaction is in progress, it will be rolled back before closing.
     *
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @Override
    public void close() throws SQLException {
        if (inTransaction) {
            try {
                rollback();
            } catch (SQLException e) {
                // Log but don't throw - we still want to close
            }
        }
        connection.close();
    }
}
