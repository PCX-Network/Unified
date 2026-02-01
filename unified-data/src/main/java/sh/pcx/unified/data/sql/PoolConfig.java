/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration record for connection pool settings.
 *
 * <p>This record encapsulates all HikariCP pool configuration options with
 * sensible defaults optimized for Minecraft server environments.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create pool configuration with defaults
 * PoolConfig config = PoolConfig.builder()
 *     .poolName("MyPlugin-Pool")
 *     .maximumPoolSize(10)
 *     .minimumIdle(2)
 *     .build();
 *
 * // Create optimized configuration for high-load servers
 * PoolConfig highLoadConfig = PoolConfig.builder()
 *     .poolName("HighLoad-Pool")
 *     .maximumPoolSize(20)
 *     .minimumIdle(5)
 *     .connectionTimeout(Duration.ofSeconds(10))
 *     .idleTimeout(Duration.ofMinutes(5))
 *     .maxLifetime(Duration.ofMinutes(30))
 *     .leakDetectionThreshold(Duration.ofSeconds(30))
 *     .build();
 *
 * // SQLite optimized configuration (single connection)
 * PoolConfig sqliteConfig = PoolConfig.forSQLite("MyPlugin-SQLite");
 * }</pre>
 *
 * <h2>Pool Sizing Guidelines</h2>
 * <ul>
 *   <li><strong>Small servers (< 50 players):</strong> max=5, idle=2</li>
 *   <li><strong>Medium servers (50-200 players):</strong> max=10, idle=3</li>
 *   <li><strong>Large servers (> 200 players):</strong> max=20, idle=5</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This record is immutable and thread-safe.
 *
 * @param poolName               unique name for this pool (for JMX/logging)
 * @param maximumPoolSize        maximum number of connections in the pool
 * @param minimumIdle            minimum number of idle connections to maintain
 * @param connectionTimeout      maximum time to wait for a connection
 * @param idleTimeout            time before an idle connection is removed
 * @param maxLifetime            maximum lifetime of a connection
 * @param keepaliveTime          interval for keepalive queries
 * @param validationTimeout      timeout for connection validation
 * @param leakDetectionThreshold threshold for detecting connection leaks (0 to disable)
 * @param autoCommit             default auto-commit state for connections
 * @param readOnly               whether connections should be read-only
 * @param transactionIsolation   default transaction isolation level (null for driver default)
 * @param catalog                default catalog for connections (null for driver default)
 * @param schema                 default schema for connections (null for driver default)
 * @param dataSourceProperties   additional data source properties
 * @since 1.0.0
 * @author Supatuck
 */
public record PoolConfig(
        @NotNull String poolName,
        int maximumPoolSize,
        int minimumIdle,
        @NotNull Duration connectionTimeout,
        @NotNull Duration idleTimeout,
        @NotNull Duration maxLifetime,
        @NotNull Duration keepaliveTime,
        @NotNull Duration validationTimeout,
        @NotNull Duration leakDetectionThreshold,
        boolean autoCommit,
        boolean readOnly,
        @Nullable String transactionIsolation,
        @Nullable String catalog,
        @Nullable String schema,
        @NotNull Map<String, Object> dataSourceProperties
) {

    // Default values optimized for Minecraft servers
    /** Default maximum pool size. */
    public static final int DEFAULT_MAX_POOL_SIZE = 10;

    /** Default minimum idle connections. */
    public static final int DEFAULT_MIN_IDLE = 2;

    /** Default connection timeout (30 seconds). */
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    /** Default idle timeout (10 minutes). */
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(10);

    /** Default maximum lifetime (30 minutes). */
    public static final Duration DEFAULT_MAX_LIFETIME = Duration.ofMinutes(30);

    /** Default keepalive time (0 = disabled). */
    public static final Duration DEFAULT_KEEPALIVE_TIME = Duration.ZERO;

    /** Default validation timeout (5 seconds). */
    public static final Duration DEFAULT_VALIDATION_TIMEOUT = Duration.ofSeconds(5);

    /** Default leak detection threshold (0 = disabled). */
    public static final Duration DEFAULT_LEAK_DETECTION_THRESHOLD = Duration.ZERO;

    /**
     * Compact constructor with validation.
     */
    public PoolConfig {
        Objects.requireNonNull(poolName, "Pool name cannot be null");
        Objects.requireNonNull(connectionTimeout, "Connection timeout cannot be null");
        Objects.requireNonNull(idleTimeout, "Idle timeout cannot be null");
        Objects.requireNonNull(maxLifetime, "Max lifetime cannot be null");
        Objects.requireNonNull(keepaliveTime, "Keepalive time cannot be null");
        Objects.requireNonNull(validationTimeout, "Validation timeout cannot be null");
        Objects.requireNonNull(leakDetectionThreshold, "Leak detection threshold cannot be null");

        if (poolName.isBlank()) {
            throw new IllegalArgumentException("Pool name cannot be blank");
        }
        if (maximumPoolSize < 1) {
            throw new IllegalArgumentException("Maximum pool size must be at least 1");
        }
        if (minimumIdle < 0) {
            throw new IllegalArgumentException("Minimum idle cannot be negative");
        }
        if (minimumIdle > maximumPoolSize) {
            minimumIdle = maximumPoolSize;
        }
        if (connectionTimeout.isNegative()) {
            throw new IllegalArgumentException("Connection timeout cannot be negative");
        }

        // Create defensive copy of data source properties
        dataSourceProperties = Map.copyOf(dataSourceProperties != null ? dataSourceProperties : Map.of());
    }

    /**
     * Creates a configuration optimized for SQLite databases.
     *
     * <p>SQLite uses file-based locking and doesn't benefit from connection
     * pooling in the same way as client-server databases. This configuration
     * uses a single connection to avoid locking issues.
     *
     * @param poolName the pool name
     * @return a SQLite-optimized configuration
     * @since 1.0.0
     */
    @NotNull
    public static PoolConfig forSQLite(@NotNull String poolName) {
        return builder()
                .poolName(poolName)
                .maximumPoolSize(1)
                .minimumIdle(1)
                .connectionTimeout(Duration.ofSeconds(30))
                .idleTimeout(Duration.ZERO) // Never timeout
                .maxLifetime(Duration.ZERO) // Never expire
                .build();
    }

    /**
     * Creates a configuration with sensible defaults.
     *
     * @param poolName the pool name
     * @return a default configuration
     * @since 1.0.0
     */
    @NotNull
    public static PoolConfig withDefaults(@NotNull String poolName) {
        return builder().poolName(poolName).build();
    }

    /**
     * Creates a new builder for constructing pool configurations.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with this configuration's values.
     *
     * @return a new builder with current values
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .poolName(poolName)
                .maximumPoolSize(maximumPoolSize)
                .minimumIdle(minimumIdle)
                .connectionTimeout(connectionTimeout)
                .idleTimeout(idleTimeout)
                .maxLifetime(maxLifetime)
                .keepaliveTime(keepaliveTime)
                .validationTimeout(validationTimeout)
                .leakDetectionThreshold(leakDetectionThreshold)
                .autoCommit(autoCommit)
                .readOnly(readOnly)
                .transactionIsolation(transactionIsolation)
                .catalog(catalog)
                .schema(schema)
                .dataSourceProperties(new HashMap<>(dataSourceProperties));
    }

    /**
     * Builder for creating {@link PoolConfig} instances.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * PoolConfig config = PoolConfig.builder()
     *     .poolName("MyPlugin-DB")
     *     .maximumPoolSize(15)
     *     .minimumIdle(3)
     *     .connectionTimeout(Duration.ofSeconds(20))
     *     .leakDetectionThreshold(Duration.ofSeconds(60))
     *     .build();
     * }</pre>
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private String poolName = "UnifiedPlugin-Pool";
        private int maximumPoolSize = DEFAULT_MAX_POOL_SIZE;
        private int minimumIdle = DEFAULT_MIN_IDLE;
        private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private Duration idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private Duration maxLifetime = DEFAULT_MAX_LIFETIME;
        private Duration keepaliveTime = DEFAULT_KEEPALIVE_TIME;
        private Duration validationTimeout = DEFAULT_VALIDATION_TIMEOUT;
        private Duration leakDetectionThreshold = DEFAULT_LEAK_DETECTION_THRESHOLD;
        private boolean autoCommit = true;
        private boolean readOnly = false;
        private String transactionIsolation = null;
        private String catalog = null;
        private String schema = null;
        private Map<String, Object> dataSourceProperties = new HashMap<>();

        private Builder() {}

        /**
         * Sets the pool name.
         *
         * <p>The pool name is used for JMX registration and logging.
         * It should be unique per connection pool.
         *
         * @param poolName the pool name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder poolName(@NotNull String poolName) {
            this.poolName = poolName;
            return this;
        }

        /**
         * Sets the maximum number of connections in the pool.
         *
         * <p>This is the maximum number of actual connections to the database.
         * When the pool is exhausted, calls to getConnection() will block for
         * up to {@link #connectionTimeout(Duration)} before failing.
         *
         * <h2>Sizing Guidelines</h2>
         * <p>For Minecraft servers, a good formula is:
         * <pre>connections = (core_count * 2) + effective_spindle_count</pre>
         * <p>For most servers with SSDs: 10-20 connections is sufficient.
         *
         * @param maximumPoolSize the maximum pool size (minimum 1)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        /**
         * Sets the minimum number of idle connections to maintain.
         *
         * <p>HikariCP will attempt to maintain this many idle connections
         * in the pool. If traffic is low, connections may be removed down
         * to this number.
         *
         * @param minimumIdle the minimum idle connections
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder minimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        /**
         * Sets the maximum time to wait for a connection from the pool.
         *
         * <p>If a connection is not available within this time, a SQLException
         * is thrown. The minimum value is 250ms.
         *
         * @param connectionTimeout the connection timeout
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder connectionTimeout(@NotNull Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * Sets the maximum time a connection can remain idle in the pool.
         *
         * <p>Connections that exceed this time are eligible for removal.
         * Set to Duration.ZERO to never remove idle connections.
         *
         * @param idleTimeout the idle timeout
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder idleTimeout(@NotNull Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        /**
         * Sets the maximum lifetime of a connection in the pool.
         *
         * <p>Connections are removed from the pool after this time, even if
         * in use. This prevents issues with stale connections.
         * Set to Duration.ZERO to never expire connections.
         *
         * @param maxLifetime the maximum lifetime
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxLifetime(@NotNull Duration maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        /**
         * Sets the interval for keepalive queries.
         *
         * <p>This controls how frequently HikariCP will attempt to keep a
         * connection alive. Set to Duration.ZERO to disable.
         *
         * @param keepaliveTime the keepalive interval
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder keepaliveTime(@NotNull Duration keepaliveTime) {
            this.keepaliveTime = keepaliveTime;
            return this;
        }

        /**
         * Sets the timeout for connection validation queries.
         *
         * @param validationTimeout the validation timeout
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder validationTimeout(@NotNull Duration validationTimeout) {
            this.validationTimeout = validationTimeout;
            return this;
        }

        /**
         * Sets the leak detection threshold.
         *
         * <p>If a connection is held for longer than this threshold, a warning
         * is logged with the stack trace of where the connection was acquired.
         * Set to Duration.ZERO to disable leak detection.
         *
         * <p>Recommended value for development: 30-60 seconds.
         *
         * @param leakDetectionThreshold the leak detection threshold
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder leakDetectionThreshold(@NotNull Duration leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
            return this;
        }

        /**
         * Sets the default auto-commit state for connections.
         *
         * @param autoCommit true to enable auto-commit (default)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder autoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        /**
         * Sets whether connections should be read-only by default.
         *
         * @param readOnly true for read-only connections
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        /**
         * Sets the default transaction isolation level.
         *
         * <p>Valid values:
         * <ul>
         *   <li>TRANSACTION_READ_UNCOMMITTED</li>
         *   <li>TRANSACTION_READ_COMMITTED</li>
         *   <li>TRANSACTION_REPEATABLE_READ</li>
         *   <li>TRANSACTION_SERIALIZABLE</li>
         * </ul>
         *
         * @param transactionIsolation the isolation level name, or null for default
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder transactionIsolation(@Nullable String transactionIsolation) {
            this.transactionIsolation = transactionIsolation;
            return this;
        }

        /**
         * Sets the default catalog for connections.
         *
         * @param catalog the catalog name, or null for default
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder catalog(@Nullable String catalog) {
            this.catalog = catalog;
            return this;
        }

        /**
         * Sets the default schema for connections.
         *
         * @param schema the schema name, or null for default
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder schema(@Nullable String schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Adds a data source property.
         *
         * <p>These properties are passed directly to the underlying JDBC driver.
         *
         * @param key   the property key
         * @param value the property value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder dataSourceProperty(@NotNull String key, @NotNull Object value) {
            this.dataSourceProperties.put(key, value);
            return this;
        }

        /**
         * Sets all data source properties.
         *
         * @param properties the properties map
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder dataSourceProperties(@NotNull Map<String, Object> properties) {
            this.dataSourceProperties = new HashMap<>(properties);
            return this;
        }

        /**
         * Builds the pool configuration.
         *
         * @return the new configuration instance
         * @since 1.0.0
         */
        @NotNull
        public PoolConfig build() {
            return new PoolConfig(
                    poolName,
                    maximumPoolSize,
                    minimumIdle,
                    connectionTimeout,
                    idleTimeout,
                    maxLifetime,
                    keepaliveTime,
                    validationTimeout,
                    leakDetectionThreshold,
                    autoCommit,
                    readOnly,
                    transactionIsolation,
                    catalog,
                    schema,
                    dataSourceProperties
            );
        }
    }
}
