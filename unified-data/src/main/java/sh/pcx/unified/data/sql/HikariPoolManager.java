/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HikariCP connection pool manager implementing {@link ConnectionProvider}.
 *
 * <p>This class manages a HikariCP connection pool, providing high-performance
 * connection pooling with automatic connection validation, leak detection,
 * and health monitoring.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create database configuration
 * DatabaseConfig dbConfig = DatabaseConfig.builder()
 *     .type(DatabaseType.MYSQL)
 *     .host("localhost")
 *     .port(3306)
 *     .database("minecraft")
 *     .username("mc_user")
 *     .password("secret")
 *     .build();
 *
 * // Create pool configuration
 * PoolConfig poolConfig = PoolConfig.builder()
 *     .poolName("MyPlugin-Pool")
 *     .maximumPoolSize(10)
 *     .minimumIdle(2)
 *     .build();
 *
 * // Initialize the pool manager
 * HikariPoolManager poolManager = new HikariPoolManager(dbConfig, poolConfig);
 *
 * // Get connections
 * try (DatabaseConnection conn = poolManager.getConnection()) {
 *     conn.executeUpdate("UPDATE players SET online = true WHERE uuid = ?", uuid);
 * }
 *
 * // Shutdown when done
 * poolManager.close();
 * }</pre>
 *
 * <h2>Pool Monitoring</h2>
 * <pre>{@code
 * // Monitor pool statistics
 * logger.info("Active: {}, Idle: {}, Total: {}, Max: {}",
 *     poolManager.getActiveConnections(),
 *     poolManager.getIdleConnections(),
 *     poolManager.getTotalConnections(),
 *     poolManager.getMaxPoolSize()
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Multiple threads can safely call
 * {@link #getConnection()} concurrently.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ConnectionProvider
 * @see PoolConfig
 * @see DatabaseConfig
 */
public class HikariPoolManager implements ConnectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(HikariPoolManager.class);

    private final DatabaseConfig databaseConfig;
    private final PoolConfig poolConfig;
    private final HikariDataSource dataSource;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new HikariCP pool manager.
     *
     * <p>The pool is initialized immediately and connections become available
     * based on the minimum idle configuration.
     *
     * @param databaseConfig the database connection configuration
     * @param poolConfig     the pool configuration
     * @throws NullPointerException if any parameter is null
     * @throws HikariPool.PoolInitializationException if the pool cannot be initialized
     * @since 1.0.0
     */
    public HikariPoolManager(@NotNull DatabaseConfig databaseConfig, @NotNull PoolConfig poolConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig, "Database config cannot be null");
        this.poolConfig = Objects.requireNonNull(poolConfig, "Pool config cannot be null");

        logger.info("Initializing HikariCP pool '{}' for {} database",
                poolConfig.poolName(), databaseConfig.type().getDisplayName());

        HikariConfig hikariConfig = createHikariConfig();
        this.dataSource = new HikariDataSource(hikariConfig);

        logger.info("HikariCP pool '{}' initialized successfully (max={}, min={})",
                poolConfig.poolName(), poolConfig.maximumPoolSize(), poolConfig.minimumIdle());
    }

    /**
     * Creates the HikariConfig from our configuration records.
     *
     * @return the configured HikariConfig
     */
    @NotNull
    private HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();

        // Basic connection settings
        config.setJdbcUrl(databaseConfig.buildJdbcUrl());
        config.setDriverClassName(databaseConfig.type().getDriverClassName());

        if (databaseConfig.hasCredentials()) {
            config.setUsername(databaseConfig.username());
            config.setPassword(databaseConfig.password());
        }

        // Pool settings
        config.setPoolName(poolConfig.poolName());
        config.setMaximumPoolSize(poolConfig.maximumPoolSize());
        config.setMinimumIdle(poolConfig.minimumIdle());

        // Timeout settings
        config.setConnectionTimeout(poolConfig.connectionTimeout().toMillis());
        config.setIdleTimeout(poolConfig.idleTimeout().toMillis());
        config.setMaxLifetime(poolConfig.maxLifetime().toMillis());
        config.setValidationTimeout(poolConfig.validationTimeout().toMillis());

        // Keepalive
        if (!poolConfig.keepaliveTime().isZero()) {
            config.setKeepaliveTime(poolConfig.keepaliveTime().toMillis());
        }

        // Leak detection
        if (!poolConfig.leakDetectionThreshold().isZero()) {
            config.setLeakDetectionThreshold(poolConfig.leakDetectionThreshold().toMillis());
        }

        // Connection defaults
        config.setAutoCommit(poolConfig.autoCommit());
        config.setReadOnly(poolConfig.readOnly());

        if (poolConfig.transactionIsolation() != null) {
            config.setTransactionIsolation(poolConfig.transactionIsolation());
        }
        if (poolConfig.catalog() != null) {
            config.setCatalog(poolConfig.catalog());
        }
        if (poolConfig.schema() != null) {
            config.setSchema(poolConfig.schema());
        }

        // Database-specific optimizations
        applyDatabaseOptimizations(config);

        // Add data source properties
        for (var entry : poolConfig.dataSourceProperties().entrySet()) {
            config.addDataSourceProperty(entry.getKey(), entry.getValue());
        }

        return config;
    }

    /**
     * Applies database-specific optimizations to the HikariConfig.
     *
     * @param config the config to modify
     */
    private void applyDatabaseOptimizations(@NotNull HikariConfig config) {
        switch (databaseConfig.type()) {
            case MYSQL, MARIADB -> {
                // MySQL/MariaDB optimizations
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
            }
            case POSTGRESQL -> {
                // PostgreSQL optimizations
                config.addDataSourceProperty("prepareThreshold", "5");
                config.addDataSourceProperty("preparedStatementCacheQueries", "256");
                config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
            }
            case SQLITE -> {
                // SQLite optimizations - enforce WAL mode for better concurrency
                config.addDataSourceProperty("journal_mode", "WAL");
                config.addDataSourceProperty("synchronous", "NORMAL");
                config.addDataSourceProperty("busy_timeout", "30000");
                config.addDataSourceProperty("cache_size", "-2000"); // 2MB
            }
        }
    }

    @Override
    @NotNull
    public DatabaseConnection getConnection() throws SQLException {
        if (closed.get()) {
            throw new SQLException("Pool has been closed");
        }

        Connection connection = dataSource.getConnection();
        return new DatabaseConnection(connection, databaseConfig.type());
    }

    @Override
    public boolean isAvailable() {
        if (closed.get()) {
            return false;
        }
        return dataSource.isRunning();
    }

    @Override
    @NotNull
    public DatabaseType getDatabaseType() {
        return databaseConfig.type();
    }

    @Override
    public int getActiveConnections() {
        if (closed.get()) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    @Override
    public int getIdleConnections() {
        if (closed.get()) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }

    @Override
    public int getMaxPoolSize() {
        return poolConfig.maximumPoolSize();
    }

    /**
     * Returns the underlying HikariDataSource for advanced usage.
     *
     * <p><strong>Warning:</strong> Direct manipulation of the data source
     * may interfere with pool management. Use with caution.
     *
     * @return the HikariDataSource
     * @since 1.0.0
     */
    @NotNull
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns the pool configuration.
     *
     * @return the pool configuration
     * @since 1.0.0
     */
    @NotNull
    public PoolConfig getPoolConfig() {
        return poolConfig;
    }

    /**
     * Returns the database configuration (with password redacted).
     *
     * @return the database configuration
     * @since 1.0.0
     */
    @NotNull
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig.withRedactedPassword();
    }

    /**
     * Returns the number of threads awaiting connections.
     *
     * @return the number of waiting threads
     * @since 1.0.0
     */
    public int getThreadsAwaitingConnection() {
        if (closed.get()) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
    }

    /**
     * Evicts all idle connections from the pool.
     *
     * <p>This can be useful for forcing reconnection after network issues.
     *
     * @since 1.0.0
     */
    public void evictIdleConnections() {
        if (!closed.get()) {
            dataSource.getHikariPoolMXBean().softEvictConnections();
            logger.debug("Evicted idle connections from pool '{}'", poolConfig.poolName());
        }
    }

    /**
     * Suspends the pool, preventing new connections from being created.
     *
     * @since 1.0.0
     */
    public void suspend() {
        if (!closed.get()) {
            dataSource.getHikariPoolMXBean().suspendPool();
            logger.info("Pool '{}' suspended", poolConfig.poolName());
        }
    }

    /**
     * Resumes a suspended pool.
     *
     * @since 1.0.0
     */
    public void resume() {
        if (!closed.get()) {
            dataSource.getHikariPoolMXBean().resumePool();
            logger.info("Pool '{}' resumed", poolConfig.poolName());
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            logger.info("Shutting down HikariCP pool '{}'...", poolConfig.poolName());
            dataSource.close();
            logger.info("HikariCP pool '{}' shut down successfully", poolConfig.poolName());
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Returns pool statistics as a formatted string.
     *
     * @return the pool statistics
     * @since 1.0.0
     */
    @NotNull
    public String getPoolStats() {
        if (closed.get()) {
            return "Pool '" + poolConfig.poolName() + "': CLOSED";
        }

        return String.format(
                "Pool '%s': active=%d, idle=%d, total=%d, max=%d, waiting=%d",
                poolConfig.poolName(),
                getActiveConnections(),
                getIdleConnections(),
                getTotalConnections(),
                getMaxPoolSize(),
                getThreadsAwaitingConnection()
        );
    }

    @Override
    public String toString() {
        return getPoolStats();
    }
}
