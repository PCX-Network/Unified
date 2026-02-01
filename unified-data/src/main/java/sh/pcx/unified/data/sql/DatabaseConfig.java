/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Immutable configuration record for database connections.
 *
 * <p>This record encapsulates all the settings needed to establish a database
 * connection, including connection details, credentials, and driver-specific
 * properties.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create MySQL configuration
 * DatabaseConfig mysqlConfig = DatabaseConfig.builder()
 *     .type(DatabaseType.MYSQL)
 *     .host("localhost")
 *     .port(3306)
 *     .database("minecraft")
 *     .username("mc_user")
 *     .password("secret")
 *     .property("useSSL", "false")
 *     .property("serverTimezone", "UTC")
 *     .build();
 *
 * // Create SQLite configuration
 * DatabaseConfig sqliteConfig = DatabaseConfig.builder()
 *     .type(DatabaseType.SQLITE)
 *     .database("./plugins/MyPlugin/data.db")
 *     .build();
 *
 * // Create PostgreSQL configuration with SSL
 * DatabaseConfig pgConfig = DatabaseConfig.builder()
 *     .type(DatabaseType.POSTGRESQL)
 *     .host("db.example.com")
 *     .port(5432)
 *     .database("gamedata")
 *     .username("admin")
 *     .password("secure123")
 *     .property("ssl", "true")
 *     .property("sslmode", "require")
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This record is immutable and therefore thread-safe.
 *
 * @param type       the database type
 * @param host       the database host (null for file-based databases)
 * @param port       the database port (0 for file-based databases)
 * @param database   the database name or file path
 * @param username   the database username (null for file-based databases)
 * @param password   the database password (null for file-based databases)
 * @param properties additional driver-specific properties
 * @since 1.0.0
 * @author Supatuck
 */
public record DatabaseConfig(
        @NotNull DatabaseType type,
        @Nullable String host,
        int port,
        @NotNull String database,
        @Nullable String username,
        @Nullable String password,
        @NotNull Map<String, String> properties
) {

    /**
     * Compact constructor with validation.
     */
    public DatabaseConfig {
        Objects.requireNonNull(type, "Database type cannot be null");
        Objects.requireNonNull(database, "Database name/path cannot be null");

        if (!type.isFileBased()) {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException(
                        "Host is required for " + type.getDisplayName() + " databases"
                );
            }
            if (port <= 0) {
                port = type.getDefaultPort();
            }
        } else {
            // For file-based databases, host and port are not used
            host = null;
            port = 0;
        }

        // Create defensive copy of properties
        properties = Map.copyOf(properties != null ? properties : Map.of());
    }

    /**
     * Builds the complete JDBC connection URL.
     *
     * <h2>Example URLs</h2>
     * <pre>{@code
     * // SQLite: jdbc:sqlite:./plugins/MyPlugin/data.db
     * // MySQL: jdbc:mysql://localhost:3306/minecraft?useSSL=false
     * // PostgreSQL: jdbc:postgresql://db.example.com:5432/gamedata?ssl=true
     * }</pre>
     *
     * @return the complete JDBC URL with properties
     * @since 1.0.0
     */
    @NotNull
    public String buildJdbcUrl() {
        String baseUrl;
        if (type.isFileBased()) {
            baseUrl = type.buildConnectionUrl(database);
        } else {
            baseUrl = type.buildConnectionUrl(host, port, database);
        }

        if (properties.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append(type == DatabaseType.POSTGRESQL ? "?" : "?");

        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return urlBuilder.toString();
    }

    /**
     * Converts this configuration to a Properties object for JDBC drivers.
     *
     * @return the properties including username and password if set
     * @since 1.0.0
     */
    @NotNull
    public Properties toJdbcProperties() {
        Properties props = new Properties();
        props.putAll(properties);

        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }

        return props;
    }

    /**
     * Checks if this configuration has authentication credentials.
     *
     * @return true if username is set
     * @since 1.0.0
     */
    public boolean hasCredentials() {
        return username != null && !username.isBlank();
    }

    /**
     * Returns a copy of this configuration with the password redacted.
     *
     * <p>Useful for logging without exposing sensitive information.
     *
     * @return a new configuration with password replaced by asterisks
     * @since 1.0.0
     */
    @NotNull
    public DatabaseConfig withRedactedPassword() {
        return new DatabaseConfig(type, host, port, database, username,
                password != null ? "********" : null, properties);
    }

    /**
     * Creates a new builder for constructing database configurations.
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
                .type(type)
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .properties(new HashMap<>(properties));
    }

    @Override
    public String toString() {
        // Don't expose password in toString
        return "DatabaseConfig[" +
                "type=" + type +
                ", host=" + host +
                ", port=" + port +
                ", database=" + database +
                ", username=" + username +
                ", password=" + (password != null ? "********" : "null") +
                ", properties=" + properties +
                "]";
    }

    /**
     * Builder for creating {@link DatabaseConfig} instances.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * DatabaseConfig config = DatabaseConfig.builder()
     *     .type(DatabaseType.MYSQL)
     *     .host("localhost")
     *     .port(3306)
     *     .database("minecraft")
     *     .username("user")
     *     .password("pass")
     *     .property("useSSL", "false")
     *     .build();
     * }</pre>
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private DatabaseType type;
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private Map<String, String> properties = new HashMap<>();

        private Builder() {}

        /**
         * Sets the database type.
         *
         * @param type the database type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder type(@NotNull DatabaseType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the database host.
         *
         * @param host the host address
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder host(@Nullable String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the database port.
         *
         * <p>If not set, the default port for the database type will be used.
         *
         * @param port the port number
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the database name or file path.
         *
         * @param database the database name or file path for SQLite
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder database(@NotNull String database) {
            this.database = database;
            return this;
        }

        /**
         * Sets the database username.
         *
         * @param username the username
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder username(@Nullable String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the database password.
         *
         * @param password the password
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder password(@Nullable String password) {
            this.password = password;
            return this;
        }

        /**
         * Adds a driver-specific property.
         *
         * <h2>Common MySQL Properties</h2>
         * <pre>{@code
         * .property("useSSL", "false")
         * .property("serverTimezone", "UTC")
         * .property("characterEncoding", "utf8mb4")
         * .property("autoReconnect", "true")
         * }</pre>
         *
         * <h2>Common PostgreSQL Properties</h2>
         * <pre>{@code
         * .property("ssl", "true")
         * .property("sslmode", "require")
         * .property("currentSchema", "public")
         * }</pre>
         *
         * @param key   the property key
         * @param value the property value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder property(@NotNull String key, @NotNull String value) {
            this.properties.put(key, value);
            return this;
        }

        /**
         * Sets all driver-specific properties.
         *
         * @param properties the properties map
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder properties(@NotNull Map<String, String> properties) {
            this.properties = new HashMap<>(properties);
            return this;
        }

        /**
         * Builds the database configuration.
         *
         * @return the new configuration instance
         * @throws NullPointerException     if type or database is null
         * @throws IllegalArgumentException if required fields are missing
         * @since 1.0.0
         */
        @NotNull
        public DatabaseConfig build() {
            return new DatabaseConfig(type, host, port, database, username, password, properties);
        }
    }
}
