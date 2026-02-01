/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of supported database types.
 *
 * <p>Each database type provides specific configuration for JDBC drivers,
 * connection URLs, and database-specific SQL dialect variations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get database type from configuration
 * DatabaseType type = DatabaseType.fromString("mysql");
 *
 * // Build connection URL
 * String url = type.buildConnectionUrl("localhost", 3306, "mydb");
 *
 * // Check if type supports specific features
 * if (type.supportsAsyncCommit()) {
 *     // Enable async commits
 * }
 * }</pre>
 *
 * <h2>Supported Databases</h2>
 * <ul>
 *   <li>{@link #SQLITE} - Embedded file-based database, ideal for development</li>
 *   <li>{@link #MYSQL} - Popular production database with wide hosting support</li>
 *   <li>{@link #MARIADB} - MySQL-compatible fork with enhanced features</li>
 *   <li>{@link #POSTGRESQL} - Advanced open-source database with rich features</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public enum DatabaseType {

    /**
     * SQLite embedded database.
     *
     * <p>SQLite is a file-based database that requires no separate server process.
     * It is ideal for development, testing, and small server deployments.
     *
     * <p>Features:
     * <ul>
     *   <li>Zero configuration required</li>
     *   <li>Single file storage</li>
     *   <li>Cross-platform compatibility</li>
     *   <li>Limited concurrent write support</li>
     * </ul>
     */
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:", "SQLite", 0),

    /**
     * MySQL database.
     *
     * <p>MySQL is a widely-used production database, commonly available
     * on shared hosting and cloud platforms.
     *
     * <p>Features:
     * <ul>
     *   <li>Excellent performance for read-heavy workloads</li>
     *   <li>Wide hosting availability</li>
     *   <li>Strong community support</li>
     *   <li>Replication and clustering support</li>
     * </ul>
     */
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://", "MySQL", 3306),

    /**
     * MariaDB database.
     *
     * <p>MariaDB is a community-developed fork of MySQL with enhanced
     * features and optimizations while maintaining MySQL compatibility.
     *
     * <p>Features:
     * <ul>
     *   <li>Drop-in MySQL replacement</li>
     *   <li>Enhanced storage engines</li>
     *   <li>Better performance optimizations</li>
     *   <li>Active development and security updates</li>
     * </ul>
     */
    MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://", "MariaDB", 3306),

    /**
     * PostgreSQL database.
     *
     * <p>PostgreSQL is an advanced open-source database with extensive
     * features including JSON support, full-text search, and custom types.
     *
     * <p>Features:
     * <ul>
     *   <li>ACID compliance with advanced transaction support</li>
     *   <li>Native JSON/JSONB support</li>
     *   <li>Extensible type system</li>
     *   <li>Powerful indexing options</li>
     * </ul>
     */
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://", "PostgreSQL", 5432);

    private final String driverClassName;
    private final String jdbcPrefix;
    private final String displayName;
    private final int defaultPort;

    /**
     * Creates a new database type.
     *
     * @param driverClassName the fully qualified JDBC driver class name
     * @param jdbcPrefix      the JDBC URL prefix
     * @param displayName     the human-readable display name
     * @param defaultPort     the default connection port (0 for file-based)
     */
    DatabaseType(@NotNull String driverClassName, @NotNull String jdbcPrefix,
                 @NotNull String displayName, int defaultPort) {
        this.driverClassName = driverClassName;
        this.jdbcPrefix = jdbcPrefix;
        this.displayName = displayName;
        this.defaultPort = defaultPort;
    }

    /**
     * Returns the fully qualified JDBC driver class name.
     *
     * @return the driver class name
     * @since 1.0.0
     */
    @NotNull
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * Returns the JDBC URL prefix for this database type.
     *
     * @return the JDBC prefix (e.g., "jdbc:mysql://")
     * @since 1.0.0
     */
    @NotNull
    public String getJdbcPrefix() {
        return jdbcPrefix;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the default port for this database type.
     *
     * @return the default port, or 0 for file-based databases
     * @since 1.0.0
     */
    public int getDefaultPort() {
        return defaultPort;
    }

    /**
     * Checks if this database type is file-based (embedded).
     *
     * @return true if this is a file-based database (e.g., SQLite)
     * @since 1.0.0
     */
    public boolean isFileBased() {
        return this == SQLITE;
    }

    /**
     * Checks if this database type supports connection pooling effectively.
     *
     * <p>All database types support pooling, but file-based databases like
     * SQLite have limited benefits due to single-writer constraints.
     *
     * @return true if connection pooling is recommended
     * @since 1.0.0
     */
    public boolean supportsPooling() {
        return !isFileBased();
    }

    /**
     * Builds a JDBC connection URL for this database type.
     *
     * <h2>Example URLs</h2>
     * <pre>{@code
     * // SQLite
     * DatabaseType.SQLITE.buildConnectionUrl("./plugins/MyPlugin/data.db");
     * // Result: jdbc:sqlite:./plugins/MyPlugin/data.db
     *
     * // MySQL
     * DatabaseType.MYSQL.buildConnectionUrl("localhost", 3306, "mydb");
     * // Result: jdbc:mysql://localhost:3306/mydb
     * }</pre>
     *
     * @param host     the database host (ignored for SQLite)
     * @param port     the database port (ignored for SQLite)
     * @param database the database name or file path for SQLite
     * @return the complete JDBC connection URL
     * @since 1.0.0
     */
    @NotNull
    public String buildConnectionUrl(@NotNull String host, int port, @NotNull String database) {
        if (isFileBased()) {
            return jdbcPrefix + database;
        }
        return String.format("%s%s:%d/%s", jdbcPrefix, host, port, database);
    }

    /**
     * Builds a JDBC connection URL for file-based databases.
     *
     * <p>This is a convenience method for SQLite databases.
     *
     * @param filePath the path to the database file
     * @return the complete JDBC connection URL
     * @throws IllegalStateException if called on a non-file-based database
     * @since 1.0.0
     */
    @NotNull
    public String buildConnectionUrl(@NotNull String filePath) {
        if (!isFileBased()) {
            throw new IllegalStateException(
                    "buildConnectionUrl(filePath) can only be used with file-based databases. " +
                    "Use buildConnectionUrl(host, port, database) for " + displayName
            );
        }
        return jdbcPrefix + filePath;
    }

    /**
     * Returns the appropriate SQL syntax for auto-increment primary keys.
     *
     * @return the auto-increment SQL syntax
     * @since 1.0.0
     */
    @NotNull
    public String getAutoIncrementSyntax() {
        return switch (this) {
            case SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT";
            case MYSQL, MARIADB -> "INT AUTO_INCREMENT PRIMARY KEY";
            case POSTGRESQL -> "SERIAL PRIMARY KEY";
        };
    }

    /**
     * Returns the SQL syntax for upsert (insert or update on conflict).
     *
     * @return the upsert keyword/clause
     * @since 1.0.0
     */
    @NotNull
    public String getUpsertSyntax() {
        return switch (this) {
            case SQLITE -> "INSERT OR REPLACE INTO";
            case MYSQL, MARIADB -> "INSERT INTO ... ON DUPLICATE KEY UPDATE";
            case POSTGRESQL -> "INSERT INTO ... ON CONFLICT DO UPDATE";
        };
    }

    /**
     * Returns the placeholder for LIMIT clause.
     *
     * <p>PostgreSQL uses different syntax for limiting with offset.
     *
     * @param limit  the maximum number of rows
     * @param offset the number of rows to skip
     * @return the LIMIT clause
     * @since 1.0.0
     */
    @NotNull
    public String getLimitClause(int limit, int offset) {
        if (offset > 0) {
            return switch (this) {
                case POSTGRESQL -> String.format("LIMIT %d OFFSET %d", limit, offset);
                default -> String.format("LIMIT %d, %d", offset, limit);
            };
        }
        return "LIMIT " + limit;
    }

    /**
     * Parses a database type from a string.
     *
     * <p>The parsing is case-insensitive and supports common aliases:
     * <ul>
     *   <li>"sqlite", "lite" -> {@link #SQLITE}</li>
     *   <li>"mysql" -> {@link #MYSQL}</li>
     *   <li>"mariadb", "maria" -> {@link #MARIADB}</li>
     *   <li>"postgresql", "postgres", "pg" -> {@link #POSTGRESQL}</li>
     * </ul>
     *
     * @param value the string to parse
     * @return the corresponding database type
     * @throws IllegalArgumentException if the value doesn't match any type
     * @since 1.0.0
     */
    @NotNull
    public static DatabaseType fromString(@NotNull String value) {
        return switch (value.toLowerCase().trim()) {
            case "sqlite", "lite" -> SQLITE;
            case "mysql" -> MYSQL;
            case "mariadb", "maria" -> MARIADB;
            case "postgresql", "postgres", "pg", "pgsql" -> POSTGRESQL;
            default -> throw new IllegalArgumentException(
                    "Unknown database type: " + value + ". " +
                    "Supported types: sqlite, mysql, mariadb, postgresql"
            );
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
