/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Configuration record for MongoDB connection settings.
 *
 * <p>This immutable record holds all configuration options needed to establish
 * and maintain a MongoDB connection, including connection URI, database name,
 * timeouts, and pool settings.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple configuration
 * MongoConfig config = new MongoConfig("mongodb://localhost:27017", "myDatabase");
 *
 * // Full configuration using builder
 * MongoConfig config = MongoConfig.builder()
 *     .uri("mongodb://user:pass@host:27017")
 *     .database("myDatabase")
 *     .applicationName("MyPlugin")
 *     .connectTimeout(Duration.ofSeconds(10))
 *     .socketTimeout(Duration.ofSeconds(30))
 *     .maxPoolSize(50)
 *     .minPoolSize(5)
 *     .maxIdleTime(Duration.ofMinutes(10))
 *     .maxWaitTime(Duration.ofSeconds(120))
 *     .retryWrites(true)
 *     .retryReads(true)
 *     .build();
 * }</pre>
 *
 * <h2>Connection URI Format</h2>
 * <p>The connection URI follows the MongoDB connection string format:
 * <ul>
 *   <li>{@code mongodb://host:port} - Simple connection</li>
 *   <li>{@code mongodb://user:password@host:port/database} - Authenticated</li>
 *   <li>{@code mongodb+srv://cluster.mongodb.net} - SRV records (Atlas)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This record is immutable and therefore thread-safe.
 *
 * @param uri             the MongoDB connection URI
 * @param database        the database name to use
 * @param applicationName the application name for connection identification
 * @param connectTimeout  the connection timeout duration
 * @param socketTimeout   the socket timeout duration
 * @param serverSelectionTimeout the server selection timeout duration
 * @param maxPoolSize     the maximum number of connections in the pool
 * @param minPoolSize     the minimum number of connections in the pool
 * @param maxIdleTime     the maximum idle time before a connection is closed
 * @param maxWaitTime     the maximum time to wait for a connection from the pool
 * @param retryWrites     whether to retry writes on transient failures
 * @param retryReads      whether to retry reads on transient failures
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoConnection
 * @see MongoClientProvider
 */
public record MongoConfig(
        @NotNull String uri,
        @NotNull String database,
        @Nullable String applicationName,
        @NotNull Duration connectTimeout,
        @NotNull Duration socketTimeout,
        @NotNull Duration serverSelectionTimeout,
        int maxPoolSize,
        int minPoolSize,
        @NotNull Duration maxIdleTime,
        @NotNull Duration maxWaitTime,
        boolean retryWrites,
        boolean retryReads
) {

    /**
     * Default connection timeout (10 seconds).
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Default socket timeout (0 = no timeout).
     */
    public static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ZERO;

    /**
     * Default server selection timeout (30 seconds).
     */
    public static final Duration DEFAULT_SERVER_SELECTION_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default maximum pool size.
     */
    public static final int DEFAULT_MAX_POOL_SIZE = 100;

    /**
     * Default minimum pool size.
     */
    public static final int DEFAULT_MIN_POOL_SIZE = 0;

    /**
     * Default maximum idle time (0 = no limit).
     */
    public static final Duration DEFAULT_MAX_IDLE_TIME = Duration.ZERO;

    /**
     * Default maximum wait time for pool (2 minutes).
     */
    public static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofMinutes(2);

    /**
     * Creates a new MongoConfig with validation.
     */
    public MongoConfig {
        Objects.requireNonNull(uri, "URI cannot be null");
        Objects.requireNonNull(database, "Database name cannot be null");
        Objects.requireNonNull(connectTimeout, "Connect timeout cannot be null");
        Objects.requireNonNull(socketTimeout, "Socket timeout cannot be null");
        Objects.requireNonNull(serverSelectionTimeout, "Server selection timeout cannot be null");
        Objects.requireNonNull(maxIdleTime, "Max idle time cannot be null");
        Objects.requireNonNull(maxWaitTime, "Max wait time cannot be null");

        if (uri.isBlank()) {
            throw new IllegalArgumentException("URI cannot be blank");
        }
        if (database.isBlank()) {
            throw new IllegalArgumentException("Database name cannot be blank");
        }
        if (maxPoolSize < 1) {
            throw new IllegalArgumentException("Max pool size must be at least 1");
        }
        if (minPoolSize < 0) {
            throw new IllegalArgumentException("Min pool size cannot be negative");
        }
        if (minPoolSize > maxPoolSize) {
            throw new IllegalArgumentException("Min pool size cannot exceed max pool size");
        }
    }

    /**
     * Creates a simple configuration with default settings.
     *
     * @param uri      the MongoDB connection URI
     * @param database the database name
     */
    public MongoConfig(@NotNull String uri, @NotNull String database) {
        this(
                uri,
                database,
                null,
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_SOCKET_TIMEOUT,
                DEFAULT_SERVER_SELECTION_TIMEOUT,
                DEFAULT_MAX_POOL_SIZE,
                DEFAULT_MIN_POOL_SIZE,
                DEFAULT_MAX_IDLE_TIME,
                DEFAULT_MAX_WAIT_TIME,
                true,
                true
        );
    }

    /**
     * Returns the application name as an Optional.
     *
     * @return an Optional containing the application name if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getApplicationName() {
        return Optional.ofNullable(applicationName);
    }

    /**
     * Returns the connect timeout in milliseconds.
     *
     * @return the connect timeout in milliseconds
     * @since 1.0.0
     */
    public long connectTimeoutMillis() {
        return connectTimeout.toMillis();
    }

    /**
     * Returns the socket timeout in milliseconds.
     *
     * @return the socket timeout in milliseconds
     * @since 1.0.0
     */
    public long socketTimeoutMillis() {
        return socketTimeout.toMillis();
    }

    /**
     * Returns the server selection timeout in milliseconds.
     *
     * @return the server selection timeout in milliseconds
     * @since 1.0.0
     */
    public long serverSelectionTimeoutMillis() {
        return serverSelectionTimeout.toMillis();
    }

    /**
     * Returns the max idle time in milliseconds.
     *
     * @return the max idle time in milliseconds
     * @since 1.0.0
     */
    public long maxIdleTimeMillis() {
        return maxIdleTime.toMillis();
    }

    /**
     * Returns the max wait time in milliseconds.
     *
     * @return the max wait time in milliseconds
     * @since 1.0.0
     */
    public long maxWaitTimeMillis() {
        return maxWaitTime.toMillis();
    }

    /**
     * Creates a new config with a different database name.
     *
     * @param database the new database name
     * @return a new config with the specified database
     * @since 1.0.0
     */
    @NotNull
    public MongoConfig withDatabase(@NotNull String database) {
        return new MongoConfig(
                uri, database, applicationName, connectTimeout, socketTimeout,
                serverSelectionTimeout, maxPoolSize, minPoolSize, maxIdleTime,
                maxWaitTime, retryWrites, retryReads
        );
    }

    /**
     * Creates a new config with a different application name.
     *
     * @param applicationName the new application name
     * @return a new config with the specified application name
     * @since 1.0.0
     */
    @NotNull
    public MongoConfig withApplicationName(@Nullable String applicationName) {
        return new MongoConfig(
                uri, database, applicationName, connectTimeout, socketTimeout,
                serverSelectionTimeout, maxPoolSize, minPoolSize, maxIdleTime,
                maxWaitTime, retryWrites, retryReads
        );
    }

    /**
     * Creates a new builder for constructing MongoConfig instances.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized from this config.
     *
     * @return a new builder with values from this config
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .uri(uri)
                .database(database)
                .applicationName(applicationName)
                .connectTimeout(connectTimeout)
                .socketTimeout(socketTimeout)
                .serverSelectionTimeout(serverSelectionTimeout)
                .maxPoolSize(maxPoolSize)
                .minPoolSize(minPoolSize)
                .maxIdleTime(maxIdleTime)
                .maxWaitTime(maxWaitTime)
                .retryWrites(retryWrites)
                .retryReads(retryReads);
    }

    /**
     * Builder class for creating {@link MongoConfig} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String uri;
        private String database;
        private String applicationName;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        private Duration serverSelectionTimeout = DEFAULT_SERVER_SELECTION_TIMEOUT;
        private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        private int minPoolSize = DEFAULT_MIN_POOL_SIZE;
        private Duration maxIdleTime = DEFAULT_MAX_IDLE_TIME;
        private Duration maxWaitTime = DEFAULT_MAX_WAIT_TIME;
        private boolean retryWrites = true;
        private boolean retryReads = true;

        private Builder() {}

        /**
         * Sets the MongoDB connection URI.
         *
         * @param uri the connection URI
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder uri(@NotNull String uri) {
            this.uri = Objects.requireNonNull(uri, "URI cannot be null");
            return this;
        }

        /**
         * Sets the database name.
         *
         * @param database the database name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder database(@NotNull String database) {
            this.database = Objects.requireNonNull(database, "Database cannot be null");
            return this;
        }

        /**
         * Sets the application name.
         *
         * @param applicationName the application name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder applicationName(@Nullable String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param timeout the connection timeout
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder connectTimeout(@NotNull Duration timeout) {
            this.connectTimeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param timeout  the timeout value
         * @param timeUnit the time unit
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder connectTimeout(long timeout, @NotNull TimeUnit timeUnit) {
            return connectTimeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
        }

        /**
         * Sets the socket timeout.
         *
         * @param timeout the socket timeout
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder socketTimeout(@NotNull Duration timeout) {
            this.socketTimeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
            return this;
        }

        /**
         * Sets the socket timeout.
         *
         * @param timeout  the timeout value
         * @param timeUnit the time unit
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder socketTimeout(long timeout, @NotNull TimeUnit timeUnit) {
            return socketTimeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
        }

        /**
         * Sets the server selection timeout.
         *
         * @param timeout the server selection timeout
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder serverSelectionTimeout(@NotNull Duration timeout) {
            this.serverSelectionTimeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
            return this;
        }

        /**
         * Sets the server selection timeout.
         *
         * @param timeout  the timeout value
         * @param timeUnit the time unit
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder serverSelectionTimeout(long timeout, @NotNull TimeUnit timeUnit) {
            return serverSelectionTimeout(Duration.ofMillis(timeUnit.toMillis(timeout)));
        }

        /**
         * Sets the maximum connection pool size.
         *
         * @param maxPoolSize the maximum pool size
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        /**
         * Sets the minimum connection pool size.
         *
         * @param minPoolSize the minimum pool size
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder minPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        /**
         * Sets the maximum idle time for connections.
         *
         * @param maxIdleTime the maximum idle time
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxIdleTime(@NotNull Duration maxIdleTime) {
            this.maxIdleTime = Objects.requireNonNull(maxIdleTime, "Max idle time cannot be null");
            return this;
        }

        /**
         * Sets the maximum idle time for connections.
         *
         * @param maxIdleTime the maximum idle time value
         * @param timeUnit    the time unit
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxIdleTime(long maxIdleTime, @NotNull TimeUnit timeUnit) {
            return maxIdleTime(Duration.ofMillis(timeUnit.toMillis(maxIdleTime)));
        }

        /**
         * Sets the maximum wait time for a connection from the pool.
         *
         * @param maxWaitTime the maximum wait time
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxWaitTime(@NotNull Duration maxWaitTime) {
            this.maxWaitTime = Objects.requireNonNull(maxWaitTime, "Max wait time cannot be null");
            return this;
        }

        /**
         * Sets the maximum wait time for a connection from the pool.
         *
         * @param maxWaitTime the maximum wait time value
         * @param timeUnit    the time unit
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxWaitTime(long maxWaitTime, @NotNull TimeUnit timeUnit) {
            return maxWaitTime(Duration.ofMillis(timeUnit.toMillis(maxWaitTime)));
        }

        /**
         * Sets whether to retry writes on transient failures.
         *
         * @param retryWrites true to retry writes
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder retryWrites(boolean retryWrites) {
            this.retryWrites = retryWrites;
            return this;
        }

        /**
         * Sets whether to retry reads on transient failures.
         *
         * @param retryReads true to retry reads
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder retryReads(boolean retryReads) {
            this.retryReads = retryReads;
            return this;
        }

        /**
         * Builds the MongoConfig instance.
         *
         * @return a new MongoConfig
         * @throws IllegalStateException if required fields are not set
         * @since 1.0.0
         */
        @NotNull
        public MongoConfig build() {
            if (uri == null) {
                throw new IllegalStateException("URI is required");
            }
            if (database == null) {
                throw new IllegalStateException("Database name is required");
            }
            return new MongoConfig(
                    uri, database, applicationName, connectTimeout, socketTimeout,
                    serverSelectionTimeout, maxPoolSize, minPoolSize, maxIdleTime,
                    maxWaitTime, retryWrites, retryReads
            );
        }
    }
}
