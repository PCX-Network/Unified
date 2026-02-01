/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Builder for configuring and executing storage migration operations.
 *
 * <p>StorageMigrationBuilder provides a fluent API for migrating data between
 * different storage backends (e.g., YAML to MySQL, SQLite to PostgreSQL).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple migration from YAML to MySQL
 * MigrationResult result = migration.migrateStorage()
 *     .from(StorageType.YAML)
 *     .to(StorageType.MYSQL)
 *     .execute();
 *
 * // Migration with configuration
 * MigrationResult result = migration.migrateStorage()
 *     .from(StorageType.SQLITE)
 *     .fromPath(Paths.get("plugins/MyPlugin/data.db"))
 *     .to(StorageType.POSTGRESQL)
 *     .toConfig(postgresConfig)
 *     .batchSize(100)
 *     .validateSchema(true)
 *     .createBackup(true)
 *     .onProgress(progress -> updateProgressBar(progress))
 *     .execute();
 *
 * // Async migration with callbacks
 * migration.migrateStorage()
 *     .from(StorageType.YAML)
 *     .to(StorageType.MYSQL)
 *     .onProgress(this::updateProgress)
 *     .onComplete(result -> showResult(result))
 *     .executeAsync();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationService
 * @see MigrationResult
 * @see StorageType
 */
public interface StorageMigrationBuilder {

    // ========================================================================
    // Source Configuration
    // ========================================================================

    /**
     * Sets the source storage type.
     *
     * @param type the source storage type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder from(@NotNull StorageType type);

    /**
     * Sets the source path for file-based storage.
     *
     * @param path the source path
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder fromPath(@NotNull Path path);

    /**
     * Sets the source database configuration.
     *
     * @param config the source database configuration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder fromConfig(@NotNull Object config);

    /**
     * Sets the source connection string.
     *
     * @param connectionString the JDBC or connection string
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder fromConnectionString(@NotNull String connectionString);

    // ========================================================================
    // Target Configuration
    // ========================================================================

    /**
     * Sets the target storage type.
     *
     * @param type the target storage type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder to(@NotNull StorageType type);

    /**
     * Sets the target path for file-based storage.
     *
     * @param path the target path
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder toPath(@NotNull Path path);

    /**
     * Sets the target database configuration.
     *
     * @param config the target database configuration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder toConfig(@NotNull Object config);

    /**
     * Sets the target connection string.
     *
     * @param connectionString the JDBC or connection string
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder toConnectionString(@NotNull String connectionString);

    // ========================================================================
    // Migration Options
    // ========================================================================

    /**
     * Sets the batch size for processing.
     *
     * @param size the batch size
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder batchSize(int size);

    /**
     * Enables or disables schema validation.
     *
     * <p>When enabled, the target schema is validated before migration.
     *
     * @param validate true to validate schema
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder validateSchema(boolean validate);

    /**
     * Enables or disables automatic schema creation.
     *
     * <p>When enabled, missing tables are created in the target.
     *
     * @param create true to create missing schema
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder createSchema(boolean create);

    /**
     * Enables or disables backup creation.
     *
     * <p>When enabled, existing target data is backed up.
     *
     * @param backup true to create backup
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder createBackup(boolean backup);

    /**
     * Enables or disables dry run mode.
     *
     * @param dryRun true for dry run
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder dryRun(boolean dryRun);

    /**
     * Sets whether to truncate target before migration.
     *
     * <p>Warning: This will delete existing data in the target.
     *
     * @param truncate true to truncate target
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder truncateTarget(boolean truncate);

    /**
     * Enables or disables transaction support.
     *
     * <p>When enabled, the entire migration runs in a transaction
     * and is rolled back on failure.
     *
     * @param useTransaction true to use transactions
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder useTransaction(boolean useTransaction);

    /**
     * Sets the conflict resolution strategy.
     *
     * @param strategy the conflict strategy
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder onConflict(@NotNull ConflictStrategy strategy);

    /**
     * Sets a filter for which tables/collections to migrate.
     *
     * @param filter the table filter
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder tableFilter(@NotNull TableFilter filter);

    /**
     * Specifies tables to include in the migration.
     *
     * @param tables the table names to include
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder includeTables(@NotNull String... tables);

    /**
     * Specifies tables to exclude from the migration.
     *
     * @param tables the table names to exclude
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder excludeTables(@NotNull String... tables);

    // ========================================================================
    // Progress and Callbacks
    // ========================================================================

    /**
     * Sets the progress callback.
     *
     * @param callback the progress callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder onProgress(@NotNull Consumer<MigrationProgress> callback);

    /**
     * Sets the completion callback.
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder onComplete(@NotNull Consumer<MigrationResult> callback);

    /**
     * Sets the error callback.
     *
     * @param callback the error callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder onError(@NotNull Consumer<Throwable> callback);

    /**
     * Sets a callback for each table migrated.
     *
     * @param callback the per-table callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder onTableMigrated(@NotNull Consumer<String> callback);

    // ========================================================================
    // Validation and Execution
    // ========================================================================

    /**
     * Validates the migration configuration.
     *
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    ValidationResult validate();

    /**
     * Estimates the migration scope.
     *
     * @return the migration estimate
     * @since 1.0.0
     */
    @NotNull
    MigrationEstimate estimate();

    /**
     * Previews the migration without making changes.
     *
     * @return the preview result
     * @since 1.0.0
     */
    @NotNull
    MigrationResult preview();

    /**
     * Executes the migration synchronously.
     *
     * @return the migration result
     * @since 1.0.0
     */
    @NotNull
    MigrationResult execute();

    /**
     * Executes the migration asynchronously.
     *
     * @return a future that completes with the migration result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<MigrationResult> executeAsync();

    // ========================================================================
    // Nested Types
    // ========================================================================

    /**
     * Strategy for handling conflicts during migration.
     *
     * @since 1.0.0
     */
    enum ConflictStrategy {
        /** Skip conflicting records */
        SKIP,
        /** Replace existing records */
        REPLACE,
        /** Fail the migration on conflict */
        FAIL,
        /** Merge records, preferring new data */
        MERGE_NEW,
        /** Merge records, preferring existing data */
        MERGE_EXISTING
    }

    /**
     * Filter interface for selecting which tables to migrate.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface TableFilter {
        /**
         * Tests if a table should be migrated.
         *
         * @param tableName the table name
         * @return true if the table should be migrated
         */
        boolean shouldMigrate(@NotNull String tableName);
    }
}
