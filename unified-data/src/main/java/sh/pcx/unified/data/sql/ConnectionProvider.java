/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * Interface for obtaining database connections.
 *
 * <p>This interface abstracts the connection acquisition mechanism, allowing
 * for different implementations such as direct connections, connection pools,
 * or test mocks.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Inject the connection provider
 * @Inject
 * private ConnectionProvider connectionProvider;
 *
 * // Get a connection (try-with-resources ensures proper cleanup)
 * try (DatabaseConnection conn = connectionProvider.getConnection()) {
 *     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players");
 *     ResultSet rs = stmt.executeQuery();
 *     // Process results
 * } // Connection automatically returned to pool
 *
 * // Check if connections are available
 * if (connectionProvider.isAvailable()) {
 *     // Safe to perform database operations
 * }
 * }</pre>
 *
 * <h2>Implementation Notes</h2>
 * <p>Implementations should:
 * <ul>
 *   <li>Return connections that implement auto-close behavior</li>
 *   <li>Handle connection validation before returning</li>
 *   <li>Support connection pooling for production use</li>
 *   <li>Be thread-safe for concurrent access</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DatabaseConnection
 * @see HikariPoolManager
 */
public interface ConnectionProvider {

    /**
     * Obtains a database connection.
     *
     * <p>The returned connection should be used within a try-with-resources
     * block to ensure proper resource cleanup. When closed, the connection
     * is returned to the pool (for pooled implementations) or closed directly.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * try (DatabaseConnection conn = provider.getConnection()) {
     *     // Use the connection
     *     conn.executeUpdate("UPDATE players SET online = true WHERE uuid = ?", uuid);
     * } catch (SQLException e) {
     *     logger.error("Database operation failed", e);
     * }
     * }</pre>
     *
     * @return a database connection wrapper
     * @throws SQLException if a connection cannot be obtained
     * @since 1.0.0
     */
    @NotNull
    DatabaseConnection getConnection() throws SQLException;

    /**
     * Checks if connections are available.
     *
     * <p>This method performs a quick check to determine if the connection
     * provider can supply connections. For pooled implementations, this
     * checks if the pool is running and has available connections.
     *
     * @return true if connections can be obtained
     * @since 1.0.0
     */
    boolean isAvailable();

    /**
     * Returns the database type for this provider.
     *
     * @return the database type
     * @since 1.0.0
     */
    @NotNull
    DatabaseType getDatabaseType();

    /**
     * Returns the number of active connections.
     *
     * <p>For pooled implementations, this returns the number of connections
     * currently in use. For non-pooled implementations, returns 0 or 1.
     *
     * @return the number of active connections
     * @since 1.0.0
     */
    int getActiveConnections();

    /**
     * Returns the number of idle connections in the pool.
     *
     * <p>For non-pooled implementations, this always returns 0.
     *
     * @return the number of idle connections
     * @since 1.0.0
     */
    int getIdleConnections();

    /**
     * Returns the total number of connections in the pool.
     *
     * <p>This is the sum of active and idle connections.
     *
     * @return the total connection count
     * @since 1.0.0
     */
    default int getTotalConnections() {
        return getActiveConnections() + getIdleConnections();
    }

    /**
     * Returns the maximum pool size.
     *
     * <p>For non-pooled implementations, this returns 1.
     *
     * @return the maximum number of connections
     * @since 1.0.0
     */
    int getMaxPoolSize();

    /**
     * Closes all connections and releases resources.
     *
     * <p>After calling this method, no more connections can be obtained.
     * Any attempt to call {@link #getConnection()} will throw an exception.
     *
     * @since 1.0.0
     */
    void close();

    /**
     * Checks if the provider has been closed.
     *
     * @return true if the provider is closed
     * @since 1.0.0
     */
    boolean isClosed();
}
