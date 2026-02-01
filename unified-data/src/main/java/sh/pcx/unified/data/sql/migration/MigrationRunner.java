/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.migration;

import sh.pcx.unified.data.sql.ConnectionProvider;
import sh.pcx.unified.data.sql.DatabaseConnection;
import sh.pcx.unified.data.sql.DatabaseType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Executes database migrations in version order.
 *
 * <p>The migration runner manages the lifecycle of database migrations,
 * tracking which migrations have been applied and executing pending ones
 * in the correct order.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create migration runner
 * MigrationRunner runner = new MigrationRunner(connectionProvider, databaseType);
 *
 * // Register migrations
 * runner.register(new V1_CreatePlayersTable());
 * runner.register(new V2_AddPlayerLevel());
 * runner.register(new V3_CreateTransactionsTable());
 *
 * // Run all pending migrations
 * runner.migrate()
 *     .thenAccept(results -> {
 *         logger.info("Applied {} migrations", results.size());
 *     })
 *     .exceptionally(e -> {
 *         logger.error("Migration failed", e);
 *         return null;
 *     });
 *
 * // Check current version
 * int currentVersion = runner.getCurrentVersion();
 *
 * // Get migration history
 * List<MigrationHistory> history = runner.getAppliedMigrations();
 *
 * // Rollback last migration
 * runner.rollback(1).join();
 * }</pre>
 *
 * <h2>Migration Table</h2>
 * <p>The runner creates a table to track applied migrations:
 * <pre>
 * CREATE TABLE schema_migrations (
 *     version INT PRIMARY KEY,
 *     description VARCHAR(255) NOT NULL,
 *     checksum INT NOT NULL,
 *     applied_at TIMESTAMP NOT NULL,
 *     execution_time_ms BIGINT NOT NULL,
 *     success BOOLEAN NOT NULL,
 *     author VARCHAR(64),
 *     error_message TEXT
 * )
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The MigrationRunner is thread-safe. Migrations are executed sequentially
 * with proper locking to prevent concurrent execution.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Migration
 * @see MigrationHistory
 */
public class MigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);
    private static final String DEFAULT_TABLE_NAME = "schema_migrations";

    private final ConnectionProvider connectionProvider;
    private final DatabaseType databaseType;
    private final String tableName;
    private final Executor executor;
    private final TreeMap<Integer, Migration> migrations;
    private final Object lock = new Object();

    /**
     * Creates a new migration runner with the default table name.
     *
     * @param connectionProvider the connection provider
     * @param databaseType       the database type
     * @since 1.0.0
     */
    public MigrationRunner(@NotNull ConnectionProvider connectionProvider, @NotNull DatabaseType databaseType) {
        this(connectionProvider, databaseType, DEFAULT_TABLE_NAME);
    }

    /**
     * Creates a new migration runner with a custom table name.
     *
     * @param connectionProvider the connection provider
     * @param databaseType       the database type
     * @param tableName          the migration history table name
     * @since 1.0.0
     */
    public MigrationRunner(
            @NotNull ConnectionProvider connectionProvider,
            @NotNull DatabaseType databaseType,
            @NotNull String tableName
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.databaseType = Objects.requireNonNull(databaseType);
        this.tableName = Objects.requireNonNull(tableName);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MigrationRunner");
            t.setDaemon(true);
            return t;
        });
        this.migrations = new TreeMap<>();
    }

    // ========================================================================
    // Migration Registration
    // ========================================================================

    /**
     * Registers a migration.
     *
     * <p>Migrations are ordered by version number. Each version must be unique.
     *
     * @param migration the migration to register
     * @return this runner for chaining
     * @throws IllegalArgumentException if a migration with the same version exists
     * @since 1.0.0
     */
    @NotNull
    public MigrationRunner register(@NotNull Migration migration) {
        synchronized (lock) {
            int version = migration.getVersion();
            if (migrations.containsKey(version)) {
                throw new IllegalArgumentException(
                        "Migration version " + version + " is already registered"
                );
            }
            migrations.put(version, migration);
            logger.debug("Registered migration V{}: {}", version, migration.getDescription());
        }
        return this;
    }

    /**
     * Registers multiple migrations.
     *
     * @param migrations the migrations to register
     * @return this runner for chaining
     * @since 1.0.0
     */
    @NotNull
    public MigrationRunner registerAll(@NotNull Migration... migrations) {
        for (Migration migration : migrations) {
            register(migration);
        }
        return this;
    }

    /**
     * Registers multiple migrations from a collection.
     *
     * @param migrations the migrations to register
     * @return this runner for chaining
     * @since 1.0.0
     */
    @NotNull
    public MigrationRunner registerAll(@NotNull Collection<Migration> migrations) {
        for (Migration migration : migrations) {
            register(migration);
        }
        return this;
    }

    // ========================================================================
    // Migration Execution
    // ========================================================================

    /**
     * Runs all pending migrations.
     *
     * <p>Migrations are executed in version order. Each migration runs in
     * its own transaction.
     *
     * @return a future containing the list of applied migrations
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<MigrationHistory>> migrate() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    ensureMigrationTableExists();
                    int currentVersion = getCurrentVersionInternal();
                    List<MigrationHistory> results = new ArrayList<>();

                    for (Map.Entry<Integer, Migration> entry : migrations.entrySet()) {
                        if (entry.getKey() <= currentVersion) {
                            continue; // Already applied
                        }

                        Migration migration = entry.getValue();
                        logger.info("Applying migration V{}: {}", migration.getVersion(), migration.getDescription());

                        MigrationHistory result = applyMigration(migration);
                        results.add(result);

                        if (!result.isSuccess()) {
                            throw new MigrationException(
                                    "Migration V" + migration.getVersion() + " failed: " + result.getErrorMessage()
                            );
                        }

                        logger.info("Migration V{} applied successfully ({}ms)",
                                migration.getVersion(), result.getExecutionTimeMs());
                    }

                    if (results.isEmpty()) {
                        logger.info("No pending migrations");
                    } else {
                        logger.info("Applied {} migration(s)", results.size());
                    }

                    return results;
                } catch (SQLException e) {
                    throw new MigrationException("Migration failed", e);
                }
            }
        }, executor);
    }

    /**
     * Runs migrations up to a specific version.
     *
     * @param targetVersion the target version
     * @return a future containing the list of applied migrations
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<MigrationHistory>> migrateTo(int targetVersion) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    ensureMigrationTableExists();
                    int currentVersion = getCurrentVersionInternal();
                    List<MigrationHistory> results = new ArrayList<>();

                    for (Map.Entry<Integer, Migration> entry : migrations.entrySet()) {
                        int version = entry.getKey();
                        if (version <= currentVersion || version > targetVersion) {
                            continue;
                        }

                        Migration migration = entry.getValue();
                        logger.info("Applying migration V{}: {}", version, migration.getDescription());

                        MigrationHistory result = applyMigration(migration);
                        results.add(result);

                        if (!result.isSuccess()) {
                            throw new MigrationException(
                                    "Migration V" + version + " failed: " + result.getErrorMessage()
                            );
                        }
                    }

                    return results;
                } catch (SQLException e) {
                    throw new MigrationException("Migration failed", e);
                }
            }
        }, executor);
    }

    /**
     * Rolls back a number of migrations.
     *
     * @param count the number of migrations to roll back
     * @return a future containing the list of rolled back migrations
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<MigrationHistory>> rollback(int count) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    List<MigrationHistory> appliedHistory = getAppliedMigrationsInternal();
                    List<MigrationHistory> results = new ArrayList<>();

                    // Roll back in reverse order
                    for (int i = 0; i < count && i < appliedHistory.size(); i++) {
                        MigrationHistory history = appliedHistory.get(appliedHistory.size() - 1 - i);
                        Migration migration = migrations.get(history.getVersion());

                        if (migration == null) {
                            throw new MigrationException(
                                    "Cannot rollback V" + history.getVersion() + ": migration not found"
                            );
                        }

                        if (!migration.supportsRollback()) {
                            throw new MigrationException(
                                    "Cannot rollback V" + history.getVersion() + ": rollback not supported"
                            );
                        }

                        logger.info("Rolling back migration V{}: {}", migration.getVersion(), migration.getDescription());

                        MigrationHistory result = rollbackMigration(migration);
                        results.add(result);

                        if (!result.isSuccess()) {
                            throw new MigrationException(
                                    "Rollback of V" + migration.getVersion() + " failed: " + result.getErrorMessage()
                            );
                        }
                    }

                    return results;
                } catch (SQLException e) {
                    throw new MigrationException("Rollback failed", e);
                }
            }
        }, executor);
    }

    // ========================================================================
    // Version Information
    // ========================================================================

    /**
     * Returns the current schema version.
     *
     * @return the current version, or 0 if no migrations have been applied
     * @since 1.0.0
     */
    public int getCurrentVersion() {
        try {
            ensureMigrationTableExists();
            return getCurrentVersionInternal();
        } catch (SQLException e) {
            throw new MigrationException("Failed to get current version", e);
        }
    }

    /**
     * Returns all applied migrations.
     *
     * @return the list of applied migrations, ordered by version
     * @since 1.0.0
     */
    @NotNull
    public List<MigrationHistory> getAppliedMigrations() {
        try {
            ensureMigrationTableExists();
            return getAppliedMigrationsInternal();
        } catch (SQLException e) {
            throw new MigrationException("Failed to get migration history", e);
        }
    }

    /**
     * Returns pending migrations that have not been applied.
     *
     * @return the list of pending migrations
     * @since 1.0.0
     */
    @NotNull
    public List<Migration> getPendingMigrations() {
        int currentVersion = getCurrentVersion();
        List<Migration> pending = new ArrayList<>();

        for (Map.Entry<Integer, Migration> entry : migrations.entrySet()) {
            if (entry.getKey() > currentVersion) {
                pending.add(entry.getValue());
            }
        }

        return pending;
    }

    /**
     * Checks if there are pending migrations.
     *
     * @return true if there are migrations to apply
     * @since 1.0.0
     */
    public boolean hasPendingMigrations() {
        return !getPendingMigrations().isEmpty();
    }

    // ========================================================================
    // Validation
    // ========================================================================

    /**
     * Validates that applied migrations have not been modified.
     *
     * <p>This checks that the checksums of applied migrations match the
     * current migration code.
     *
     * @return a list of migrations with checksum mismatches
     * @since 1.0.0
     */
    @NotNull
    public List<Migration> validateChecksums() {
        List<MigrationHistory> history = getAppliedMigrations();
        List<Migration> mismatches = new ArrayList<>();

        for (MigrationHistory applied : history) {
            Migration migration = migrations.get(applied.getVersion());
            if (migration != null && !applied.checksumMatches(migration)) {
                logger.warn("Migration V{} checksum mismatch: applied={}, current={}",
                        applied.getVersion(), applied.getChecksum(), migration.getChecksum());
                mismatches.add(migration);
            }
        }

        return mismatches;
    }

    // ========================================================================
    // Private Methods
    // ========================================================================

    private void ensureMigrationTableExists() throws SQLException {
        try (DatabaseConnection conn = connectionProvider.getConnection()) {
            String createTableSql = getCreateTableSql();
            conn.executeUpdate(createTableSql);
        }
    }

    private String getCreateTableSql() {
        return String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                version INT PRIMARY KEY,
                description VARCHAR(255) NOT NULL,
                checksum INT NOT NULL,
                applied_at TIMESTAMP NOT NULL,
                execution_time_ms BIGINT NOT NULL,
                success BOOLEAN NOT NULL,
                author VARCHAR(64),
                error_message TEXT
            )
            """, tableName);
    }

    private int getCurrentVersionInternal() throws SQLException {
        try (DatabaseConnection conn = connectionProvider.getConnection();
             ResultSet rs = conn.executeQuery(
                     "SELECT MAX(version) FROM " + tableName + " WHERE success = ?", true)) {

            if (rs.next()) {
                int version = rs.getInt(1);
                return rs.wasNull() ? 0 : version;
            }
            return 0;
        }
    }

    private List<MigrationHistory> getAppliedMigrationsInternal() throws SQLException {
        try (DatabaseConnection conn = connectionProvider.getConnection();
             ResultSet rs = conn.executeQuery(
                     "SELECT version, description, checksum, applied_at, execution_time_ms, " +
                     "success, author, error_message FROM " + tableName + " ORDER BY version")) {

            List<MigrationHistory> history = new ArrayList<>();
            while (rs.next()) {
                history.add(new MigrationHistory(
                        rs.getInt("version"),
                        rs.getString("description"),
                        rs.getInt("checksum"),
                        rs.getTimestamp("applied_at").toInstant(),
                        rs.getLong("execution_time_ms"),
                        rs.getBoolean("success"),
                        rs.getString("author"),
                        rs.getString("error_message")
                ));
            }
            return history;
        }
    }

    private MigrationHistory applyMigration(Migration migration) throws SQLException {
        long startTime = System.currentTimeMillis();

        try (DatabaseConnection conn = connectionProvider.getConnection()) {
            conn.beginTransaction();

            try {
                migration.up(conn);
                conn.commit();

                long executionTime = System.currentTimeMillis() - startTime;
                MigrationHistory history = MigrationHistory.success(migration, executionTime);
                recordMigration(history);
                return history;

            } catch (SQLException e) {
                conn.rollback();
                long executionTime = System.currentTimeMillis() - startTime;
                MigrationHistory history = MigrationHistory.failure(migration, executionTime, e);
                recordMigration(history);
                return history;
            }
        }
    }

    private MigrationHistory rollbackMigration(Migration migration) throws SQLException {
        long startTime = System.currentTimeMillis();

        try (DatabaseConnection conn = connectionProvider.getConnection()) {
            conn.beginTransaction();

            try {
                migration.down(conn);
                conn.commit();

                // Remove from history
                conn.executeUpdate("DELETE FROM " + tableName + " WHERE version = ?", migration.getVersion());

                long executionTime = System.currentTimeMillis() - startTime;
                return MigrationHistory.success(migration, executionTime);

            } catch (SQLException e) {
                conn.rollback();
                long executionTime = System.currentTimeMillis() - startTime;
                return MigrationHistory.failure(migration, executionTime, e);
            }
        }
    }

    private void recordMigration(MigrationHistory history) throws SQLException {
        try (DatabaseConnection conn = connectionProvider.getConnection()) {
            conn.executeUpdate(
                    "INSERT INTO " + tableName +
                    " (version, description, checksum, applied_at, execution_time_ms, success, author, error_message) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    history.getVersion(),
                    history.getDescription(),
                    history.getChecksum(),
                    Timestamp.from(history.getAppliedAt()),
                    history.getExecutionTimeMs(),
                    history.isSuccess(),
                    history.getAuthor(),
                    history.getErrorMessage()
            );
        }
    }

    /**
     * Exception thrown when a migration fails.
     *
     * @since 1.0.0
     */
    public static class MigrationException extends RuntimeException {
        public MigrationException(String message) {
            super(message);
        }

        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
