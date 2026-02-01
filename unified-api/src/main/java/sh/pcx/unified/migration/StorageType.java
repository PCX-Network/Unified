/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of storage types for migration operations.
 *
 * <p>StorageType identifies the format or backend of data storage, used
 * when migrating data between different storage systems.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Migrate from YAML to MySQL
 * MigrationResult result = migration.migrateStorage()
 *     .from(StorageType.YAML)
 *     .to(StorageType.MYSQL)
 *     .execute();
 *
 * // Auto-detect storage type
 * Optional<StorageType> detected = migration.detectStorageType(dataPath);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationService
 * @see StorageMigrationBuilder
 */
public enum StorageType {

    /**
     * YAML file storage.
     *
     * <p>Common for Bukkit/Spigot plugin configurations and simple data storage.
     * Files typically have .yml or .yaml extension.
     */
    YAML("yaml", "YAML", ".yml", true),

    /**
     * JSON file storage.
     *
     * <p>Structured data format, commonly used for modern configurations
     * and data exchange.
     */
    JSON("json", "JSON", ".json", true),

    /**
     * SQLite database storage.
     *
     * <p>Embedded file-based SQL database. Good for single-server setups
     * with moderate data volumes.
     */
    SQLITE("sqlite", "SQLite", ".db", true),

    /**
     * MySQL database storage.
     *
     * <p>Client-server SQL database. Suitable for large data volumes and
     * multi-server setups. Requires external database server.
     */
    MYSQL("mysql", "MySQL", null, false),

    /**
     * MariaDB database storage.
     *
     * <p>MySQL-compatible database with enhanced features. Drop-in
     * replacement for MySQL.
     */
    MARIADB("mariadb", "MariaDB", null, false),

    /**
     * PostgreSQL database storage.
     *
     * <p>Advanced SQL database with rich features including JSON support,
     * full-text search, and custom types.
     */
    POSTGRESQL("postgresql", "PostgreSQL", null, false),

    /**
     * MongoDB document storage.
     *
     * <p>NoSQL document database. Good for flexible schemas and
     * complex nested data structures.
     */
    MONGODB("mongodb", "MongoDB", null, false),

    /**
     * Redis key-value storage.
     *
     * <p>In-memory data store. Excellent for caching and temporary data.
     * Can be persisted to disk.
     */
    REDIS("redis", "Redis", null, false),

    /**
     * NBT (Named Binary Tag) storage.
     *
     * <p>Minecraft's native binary format. Used for player data,
     * world data, and item serialization.
     */
    NBT("nbt", "NBT", ".dat", true),

    /**
     * H2 database storage.
     *
     * <p>Embedded Java SQL database. Can run in file or in-memory mode.
     */
    H2("h2", "H2", ".h2.db", true),

    /**
     * Custom/unknown storage type.
     *
     * <p>Used for custom storage implementations not covered by the
     * standard types.
     */
    CUSTOM("custom", "Custom", null, false);

    private final String identifier;
    private final String displayName;
    private final String fileExtension;
    private final boolean fileBased;

    StorageType(@NotNull String identifier, @NotNull String displayName,
                String fileExtension, boolean fileBased) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.fileExtension = fileExtension;
        this.fileBased = fileBased;
    }

    /**
     * Returns the identifier for this storage type.
     *
     * @return the identifier (lowercase)
     * @since 1.0.0
     */
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the display name for this storage type.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the typical file extension for file-based storage.
     *
     * @return the file extension, or null for non-file-based storage
     * @since 1.0.0
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Checks if this is a file-based storage type.
     *
     * @return true if file-based
     * @since 1.0.0
     */
    public boolean isFileBased() {
        return fileBased;
    }

    /**
     * Checks if this is a SQL database type.
     *
     * @return true if SQL-based
     * @since 1.0.0
     */
    public boolean isSqlBased() {
        return switch (this) {
            case SQLITE, MYSQL, MARIADB, POSTGRESQL, H2 -> true;
            default -> false;
        };
    }

    /**
     * Checks if this is a NoSQL storage type.
     *
     * @return true if NoSQL
     * @since 1.0.0
     */
    public boolean isNoSql() {
        return this == MONGODB || this == REDIS;
    }

    /**
     * Checks if this storage type requires an external server.
     *
     * @return true if external server required
     * @since 1.0.0
     */
    public boolean requiresServer() {
        return !fileBased && this != CUSTOM;
    }

    /**
     * Parses a storage type from string.
     *
     * @param value the string to parse (case-insensitive)
     * @return the storage type
     * @throws IllegalArgumentException if the value is not recognized
     * @since 1.0.0
     */
    @NotNull
    public static StorageType fromString(@NotNull String value) {
        String lower = value.toLowerCase().trim();
        for (StorageType type : values()) {
            if (type.identifier.equals(lower) || type.name().equalsIgnoreCase(lower)) {
                return type;
            }
        }

        // Handle aliases
        return switch (lower) {
            case "yml" -> YAML;
            case "postgres", "pg" -> POSTGRESQL;
            case "maria" -> MARIADB;
            case "mongo" -> MONGODB;
            default -> throw new IllegalArgumentException("Unknown storage type: " + value);
        };
    }

    /**
     * Detects storage type from file extension.
     *
     * @param extension the file extension (with or without leading dot)
     * @return the storage type, or CUSTOM if not recognized
     * @since 1.0.0
     */
    @NotNull
    public static StorageType fromExtension(@NotNull String extension) {
        String ext = extension.startsWith(".") ? extension : "." + extension;
        for (StorageType type : values()) {
            if (type.fileExtension != null && type.fileExtension.equalsIgnoreCase(ext)) {
                return type;
            }
        }
        return CUSTOM;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
