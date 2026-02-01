/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration record for Redis connections.
 *
 * <p>This record holds all configuration options needed to establish and manage
 * Redis connections, including connection details, authentication, SSL settings,
 * and connection pool configuration.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple configuration
 * RedisConfig config = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .build();
 *
 * // Full configuration
 * RedisConfig config = RedisConfig.builder()
 *     .host("redis.example.com")
 *     .port(6379)
 *     .password("secretPassword")
 *     .database(0)
 *     .ssl(true)
 *     .timeout(Duration.ofSeconds(5))
 *     .client(RedisClient.LETTUCE)
 *     .pool(PoolConfig.builder()
 *         .maxTotal(16)
 *         .maxIdle(8)
 *         .minIdle(2)
 *         .build())
 *     .build();
 * }</pre>
 *
 * <h2>Environment Variables</h2>
 * <p>The builder supports reading from environment variables:
 * <ul>
 *   <li>{@code REDIS_HOST} - Redis server hostname</li>
 *   <li>{@code REDIS_PORT} - Redis server port</li>
 *   <li>{@code REDIS_PASSWORD} - Redis password</li>
 *   <li>{@code REDIS_DATABASE} - Redis database index</li>
 *   <li>{@code REDIS_SSL} - Enable SSL (true/false)</li>
 * </ul>
 *
 * @param host       the Redis server hostname
 * @param port       the Redis server port
 * @param password   the Redis password (null if none)
 * @param database   the Redis database index (0-15)
 * @param ssl        whether to use SSL/TLS
 * @param timeout    the connection timeout
 * @param client     the Redis client implementation to use
 * @param pool       the connection pool configuration
 * @param clientName the client name for identification
 * @since 1.0.0
 * @author Supatuck
 * @see RedisClient
 * @see RedisService
 */
public record RedisConfig(
        @NotNull String host,
        int port,
        @Nullable String password,
        int database,
        boolean ssl,
        @NotNull Duration timeout,
        @NotNull RedisClient client,
        @NotNull PoolConfig pool,
        @Nullable String clientName
) {

    /**
     * Default Redis host.
     */
    public static final String DEFAULT_HOST = "localhost";

    /**
     * Default Redis port.
     */
    public static final int DEFAULT_PORT = 6379;

    /**
     * Default Redis database index.
     */
    public static final int DEFAULT_DATABASE = 0;

    /**
     * Default connection timeout.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Creates a new RedisConfig with validation.
     *
     * @param host       the Redis server hostname
     * @param port       the Redis server port
     * @param password   the Redis password (null if none)
     * @param database   the Redis database index (0-15)
     * @param ssl        whether to use SSL/TLS
     * @param timeout    the connection timeout
     * @param client     the Redis client implementation to use
     * @param pool       the connection pool configuration
     * @param clientName the client name for identification
     * @throws IllegalArgumentException if parameters are invalid
     */
    public RedisConfig {
        Objects.requireNonNull(host, "host cannot be null");
        Objects.requireNonNull(timeout, "timeout cannot be null");
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(pool, "pool cannot be null");

        if (host.isBlank()) {
            throw new IllegalArgumentException("host cannot be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (database < 0 || database > 15) {
            throw new IllegalArgumentException("database must be between 0 and 15");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    /**
     * Creates a new builder for RedisConfig.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration for localhost.
     *
     * @return a default Redis configuration
     * @since 1.0.0
     */
    public static RedisConfig localhost() {
        return builder().build();
    }

    /**
     * Creates a configuration from environment variables.
     *
     * @return a Redis configuration from environment
     * @since 1.0.0
     */
    public static RedisConfig fromEnvironment() {
        return builder().fromEnvironment().build();
    }

    /**
     * Creates a new builder pre-populated with this configuration's values.
     *
     * @return a builder with current values
     * @since 1.0.0
     */
    public Builder toBuilder() {
        return new Builder()
                .host(host)
                .port(port)
                .password(password)
                .database(database)
                .ssl(ssl)
                .timeout(timeout)
                .client(client)
                .pool(pool)
                .clientName(clientName);
    }

    /**
     * Returns the Redis URI for this configuration.
     *
     * @return the Redis URI string
     * @since 1.0.0
     */
    public String toUri() {
        StringBuilder uri = new StringBuilder();
        uri.append(ssl ? "rediss://" : "redis://");

        if (password != null && !password.isEmpty()) {
            uri.append(":").append(password).append("@");
        }

        uri.append(host).append(":").append(port);

        if (database != 0) {
            uri.append("/").append(database);
        }

        return uri.toString();
    }

    /**
     * Connection pool configuration.
     *
     * @param maxTotal        maximum total connections
     * @param maxIdle         maximum idle connections
     * @param minIdle         minimum idle connections
     * @param maxWait         maximum wait time for a connection
     * @param testOnBorrow    test connections when borrowing
     * @param testOnReturn    test connections when returning
     * @param testWhileIdle   test idle connections
     * @param timeBetweenEvictionRuns time between eviction runs
     * @since 1.0.0
     */
    public record PoolConfig(
            int maxTotal,
            int maxIdle,
            int minIdle,
            @NotNull Duration maxWait,
            boolean testOnBorrow,
            boolean testOnReturn,
            boolean testWhileIdle,
            @NotNull Duration timeBetweenEvictionRuns
    ) {

        /**
         * Default maximum total connections.
         */
        public static final int DEFAULT_MAX_TOTAL = 16;

        /**
         * Default maximum idle connections.
         */
        public static final int DEFAULT_MAX_IDLE = 8;

        /**
         * Default minimum idle connections.
         */
        public static final int DEFAULT_MIN_IDLE = 2;

        /**
         * Default maximum wait time.
         */
        public static final Duration DEFAULT_MAX_WAIT = Duration.ofSeconds(10);

        /**
         * Default time between eviction runs.
         */
        public static final Duration DEFAULT_EVICTION_INTERVAL = Duration.ofSeconds(30);

        /**
         * Creates a new PoolConfig with validation.
         *
         * @param maxTotal                maximum total connections
         * @param maxIdle                 maximum idle connections
         * @param minIdle                 minimum idle connections
         * @param maxWait                 maximum wait time for a connection
         * @param testOnBorrow            test connections when borrowing
         * @param testOnReturn            test connections when returning
         * @param testWhileIdle           test idle connections
         * @param timeBetweenEvictionRuns time between eviction runs
         * @throws IllegalArgumentException if parameters are invalid
         */
        public PoolConfig {
            Objects.requireNonNull(maxWait, "maxWait cannot be null");
            Objects.requireNonNull(timeBetweenEvictionRuns, "timeBetweenEvictionRuns cannot be null");

            if (maxTotal < 1) {
                throw new IllegalArgumentException("maxTotal must be at least 1");
            }
            if (maxIdle < 0) {
                throw new IllegalArgumentException("maxIdle cannot be negative");
            }
            if (minIdle < 0) {
                throw new IllegalArgumentException("minIdle cannot be negative");
            }
            if (minIdle > maxIdle) {
                throw new IllegalArgumentException("minIdle cannot exceed maxIdle");
            }
        }

        /**
         * Creates a new builder for PoolConfig.
         *
         * @return a new builder instance
         * @since 1.0.0
         */
        public static PoolBuilder builder() {
            return new PoolBuilder();
        }

        /**
         * Returns a default pool configuration.
         *
         * @return the default pool configuration
         * @since 1.0.0
         */
        public static PoolConfig defaults() {
            return builder().build();
        }

        /**
         * Builder for PoolConfig.
         *
         * @since 1.0.0
         */
        public static final class PoolBuilder {

            private int maxTotal = DEFAULT_MAX_TOTAL;
            private int maxIdle = DEFAULT_MAX_IDLE;
            private int minIdle = DEFAULT_MIN_IDLE;
            private Duration maxWait = DEFAULT_MAX_WAIT;
            private boolean testOnBorrow = true;
            private boolean testOnReturn = false;
            private boolean testWhileIdle = true;
            private Duration timeBetweenEvictionRuns = DEFAULT_EVICTION_INTERVAL;

            private PoolBuilder() {
            }

            /**
             * Sets the maximum total connections.
             *
             * @param maxTotal the maximum total connections
             * @return this builder
             */
            public PoolBuilder maxTotal(int maxTotal) {
                this.maxTotal = maxTotal;
                return this;
            }

            /**
             * Sets the maximum idle connections.
             *
             * @param maxIdle the maximum idle connections
             * @return this builder
             */
            public PoolBuilder maxIdle(int maxIdle) {
                this.maxIdle = maxIdle;
                return this;
            }

            /**
             * Sets the minimum idle connections.
             *
             * @param minIdle the minimum idle connections
             * @return this builder
             */
            public PoolBuilder minIdle(int minIdle) {
                this.minIdle = minIdle;
                return this;
            }

            /**
             * Sets the maximum wait time for a connection.
             *
             * @param maxWait the maximum wait time
             * @return this builder
             */
            public PoolBuilder maxWait(@NotNull Duration maxWait) {
                this.maxWait = Objects.requireNonNull(maxWait);
                return this;
            }

            /**
             * Sets whether to test connections when borrowing.
             *
             * @param testOnBorrow true to test on borrow
             * @return this builder
             */
            public PoolBuilder testOnBorrow(boolean testOnBorrow) {
                this.testOnBorrow = testOnBorrow;
                return this;
            }

            /**
             * Sets whether to test connections when returning.
             *
             * @param testOnReturn true to test on return
             * @return this builder
             */
            public PoolBuilder testOnReturn(boolean testOnReturn) {
                this.testOnReturn = testOnReturn;
                return this;
            }

            /**
             * Sets whether to test idle connections.
             *
             * @param testWhileIdle true to test while idle
             * @return this builder
             */
            public PoolBuilder testWhileIdle(boolean testWhileIdle) {
                this.testWhileIdle = testWhileIdle;
                return this;
            }

            /**
             * Sets the time between eviction runs.
             *
             * @param duration the time between runs
             * @return this builder
             */
            public PoolBuilder timeBetweenEvictionRuns(@NotNull Duration duration) {
                this.timeBetweenEvictionRuns = Objects.requireNonNull(duration);
                return this;
            }

            /**
             * Builds the PoolConfig.
             *
             * @return the pool configuration
             */
            public PoolConfig build() {
                return new PoolConfig(
                        maxTotal, maxIdle, minIdle, maxWait,
                        testOnBorrow, testOnReturn, testWhileIdle,
                        timeBetweenEvictionRuns
                );
            }
        }
    }

    /**
     * Builder for RedisConfig.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private String host = DEFAULT_HOST;
        private int port = DEFAULT_PORT;
        private String password = null;
        private int database = DEFAULT_DATABASE;
        private boolean ssl = false;
        private Duration timeout = DEFAULT_TIMEOUT;
        private RedisClient client = RedisClient.JEDIS;
        private PoolConfig pool = PoolConfig.defaults();
        private String clientName = null;

        private Builder() {
        }

        /**
         * Sets the Redis server hostname.
         *
         * @param host the hostname
         * @return this builder
         */
        public Builder host(@NotNull String host) {
            this.host = Objects.requireNonNull(host);
            return this;
        }

        /**
         * Sets the Redis server port.
         *
         * @param port the port number
         * @return this builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the Redis password.
         *
         * @param password the password (null for no authentication)
         * @return this builder
         */
        public Builder password(@Nullable String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the Redis database index.
         *
         * @param database the database index (0-15)
         * @return this builder
         */
        public Builder database(int database) {
            this.database = database;
            return this;
        }

        /**
         * Enables or disables SSL/TLS.
         *
         * @param ssl true to enable SSL
         * @return this builder
         */
        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param timeout the timeout duration
         * @return this builder
         */
        public Builder timeout(@NotNull Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout);
            return this;
        }

        /**
         * Sets the Redis client implementation.
         *
         * @param client the client implementation
         * @return this builder
         */
        public Builder client(@NotNull RedisClient client) {
            this.client = Objects.requireNonNull(client);
            return this;
        }

        /**
         * Sets the connection pool configuration.
         *
         * @param pool the pool configuration
         * @return this builder
         */
        public Builder pool(@NotNull PoolConfig pool) {
            this.pool = Objects.requireNonNull(pool);
            return this;
        }

        /**
         * Sets the client name for identification.
         *
         * @param clientName the client name
         * @return this builder
         */
        public Builder clientName(@Nullable String clientName) {
            this.clientName = clientName;
            return this;
        }

        /**
         * Loads configuration from environment variables.
         *
         * @return this builder
         */
        public Builder fromEnvironment() {
            String envHost = System.getenv("REDIS_HOST");
            if (envHost != null && !envHost.isBlank()) {
                this.host = envHost;
            }

            String envPort = System.getenv("REDIS_PORT");
            if (envPort != null && !envPort.isBlank()) {
                try {
                    this.port = Integer.parseInt(envPort);
                } catch (NumberFormatException ignored) {
                    // Keep default
                }
            }

            String envPassword = System.getenv("REDIS_PASSWORD");
            if (envPassword != null && !envPassword.isBlank()) {
                this.password = envPassword;
            }

            String envDatabase = System.getenv("REDIS_DATABASE");
            if (envDatabase != null && !envDatabase.isBlank()) {
                try {
                    this.database = Integer.parseInt(envDatabase);
                } catch (NumberFormatException ignored) {
                    // Keep default
                }
            }

            String envSsl = System.getenv("REDIS_SSL");
            if (envSsl != null && !envSsl.isBlank()) {
                this.ssl = Boolean.parseBoolean(envSsl);
            }

            return this;
        }

        /**
         * Builds the RedisConfig.
         *
         * @return the Redis configuration
         * @throws IllegalArgumentException if configuration is invalid
         */
        public RedisConfig build() {
            return new RedisConfig(
                    host, port, password, database, ssl,
                    timeout, client, pool, clientName
            );
        }
    }
}
