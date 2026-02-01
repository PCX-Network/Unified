/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for migrating data from other plugins and between storage backends.
 *
 * <p>The MigrationService provides functionality for:
 * <ul>
 *   <li>Discovering and running plugin importers</li>
 *   <li>Migrating data between storage backends</li>
 *   <li>Validating and mapping fields</li>
 *   <li>Preview changes with dry run mode</li>
 *   <li>Tracking migration progress</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private MigrationService migration;
 *
 * // Check available importers
 * List<DataImporter> available = migration.getAvailableImporters();
 *
 * // Start import
 * ImportResult result = migration.importFrom(Importers.PER_WORLD_INVENTORY)
 *     .sourceFolder(Paths.get("plugins/PerWorldInventory"))
 *     .dryRun(true)
 *     .execute();
 *
 * // Migrate storage
 * MigrationResult result = migration.migrateStorage()
 *     .from(StorageType.YAML)
 *     .to(StorageType.MYSQL)
 *     .execute();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The migration service is thread-safe. All operations can be safely
 * called from any thread. Async operations execute on a dedicated thread pool.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataImporter
 * @see ImportBuilder
 * @see StorageMigrationBuilder
 */
public interface MigrationService extends Service {

    // ========================================================================
    // Importer Discovery
    // ========================================================================

    /**
     * Returns all registered importers.
     *
     * <p>This includes both built-in importers and any custom importers
     * registered through the plugin API.
     *
     * @return an unmodifiable list of all registered importers
     * @since 1.0.0
     */
    @NotNull
    List<DataImporter> getRegisteredImporters();

    /**
     * Returns importers that are available for use.
     *
     * <p>An importer is available if its source data can be detected on the
     * server (e.g., the source plugin's data folder exists).
     *
     * @return an unmodifiable list of available importers
     * @since 1.0.0
     */
    @NotNull
    List<DataImporter> getAvailableImporters();

    /**
     * Finds an importer by its identifier.
     *
     * @param identifier the importer identifier (case-insensitive)
     * @return an Optional containing the importer if found
     * @since 1.0.0
     */
    @NotNull
    Optional<DataImporter> findImporter(@NotNull String identifier);

    /**
     * Finds an importer by its type.
     *
     * @param <T>          the importer type
     * @param importerType the importer class
     * @return an Optional containing the importer if registered
     * @since 1.0.0
     */
    @NotNull
    <T extends DataImporter> Optional<T> findImporter(@NotNull Class<T> importerType);

    /**
     * Registers a custom importer.
     *
     * <p>Custom importers can be created to support plugins not included
     * in the built-in importers.
     *
     * @param importer the importer to register
     * @throws IllegalArgumentException if an importer with the same identifier
     *                                  is already registered
     * @since 1.0.0
     */
    void registerImporter(@NotNull DataImporter importer);

    /**
     * Unregisters an importer by its identifier.
     *
     * @param identifier the importer identifier
     * @return true if the importer was removed
     * @since 1.0.0
     */
    boolean unregisterImporter(@NotNull String identifier);

    // ========================================================================
    // Import Operations
    // ========================================================================

    /**
     * Creates an import builder for the specified importer.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * ImportResult result = migration.importFrom(Importers.PER_WORLD_INVENTORY)
     *     .sourceFolder(Paths.get("plugins/PerWorldInventory"))
     *     .mapping(FieldMapping.builder()
     *         .map("armour", "armor")
     *         .build())
     *     .dryRun(true)
     *     .execute();
     * }</pre>
     *
     * @param importer the importer to use
     * @return a new import builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder importFrom(@NotNull DataImporter importer);

    /**
     * Creates an import builder for the importer with the specified identifier.
     *
     * @param importerIdentifier the importer identifier
     * @return a new import builder
     * @throws IllegalArgumentException if no importer with the identifier exists
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder importFrom(@NotNull String importerIdentifier);

    /**
     * Creates an import wizard for interactive imports.
     *
     * <p>The wizard provides a step-by-step interface for configuring
     * and executing imports, typically used for GUI-based migration.
     *
     * @return a new import wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard createWizard();

    // ========================================================================
    // Storage Migration
    // ========================================================================

    /**
     * Creates a storage migration builder.
     *
     * <p>Use this to migrate data between different storage backends
     * (e.g., YAML to MySQL, SQLite to PostgreSQL).
     *
     * <h2>Example</h2>
     * <pre>{@code
     * MigrationResult result = migration.migrateStorage()
     *     .from(StorageType.YAML)
     *     .to(StorageType.MYSQL)
     *     .batchSize(100)
     *     .validateSchema(true)
     *     .onProgress(progress -> updateProgressBar(progress))
     *     .execute();
     * }</pre>
     *
     * @return a new storage migration builder
     * @since 1.0.0
     */
    @NotNull
    StorageMigrationBuilder migrateStorage();

    // ========================================================================
    // Export Operations
    // ========================================================================

    /**
     * Creates an export builder for exporting data.
     *
     * <p>Exports can be used for backup, data transfer, or migration to
     * other systems.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * ExportResult result = migration.export()
     *     .format(ExportFormat.JSON)
     *     .destination(Paths.get("exports/backup.json"))
     *     .includeMetadata(true)
     *     .execute();
     * }</pre>
     *
     * @return a new export builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder export();

    // ========================================================================
    // Migration History
    // ========================================================================

    /**
     * Returns the migration history.
     *
     * <p>The history tracks all import and storage migrations performed
     * through this service.
     *
     * @return an unmodifiable list of migration records
     * @since 1.0.0
     */
    @NotNull
    List<MigrationRecord> getMigrationHistory();

    /**
     * Finds a migration record by its identifier.
     *
     * @param migrationId the migration identifier
     * @return an Optional containing the record if found
     * @since 1.0.0
     */
    @NotNull
    Optional<MigrationRecord> findMigration(@NotNull String migrationId);

    /**
     * Attempts to rollback a migration.
     *
     * <p>Rollback is only possible if the migration was configured to
     * create a backup and the backup is still available.
     *
     * @param migrationId the migration identifier
     * @return a future that completes with the rollback result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<RollbackResult> rollback(@NotNull String migrationId);

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Detects the storage type at the specified path.
     *
     * <p>This analyzes the files at the path to determine the storage format
     * (YAML, JSON, SQLite database, etc.).
     *
     * @param path the path to analyze
     * @return an Optional containing the detected storage type
     * @since 1.0.0
     */
    @NotNull
    Optional<StorageType> detectStorageType(@NotNull Path path);

    /**
     * Validates field mappings against source and target schemas.
     *
     * @param mapping      the field mapping to validate
     * @param sourceSchema the source schema
     * @param targetSchema the target schema
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    ValidationResult validateMapping(@NotNull FieldMapping mapping,
                                     @NotNull DataSchema sourceSchema,
                                     @NotNull DataSchema targetSchema);

    /**
     * Generates a suggested field mapping between two schemas.
     *
     * <p>This uses heuristics to match fields by name, type, and pattern
     * to suggest a starting mapping configuration.
     *
     * @param sourceSchema the source schema
     * @param targetSchema the target schema
     * @return a suggested field mapping
     * @since 1.0.0
     */
    @NotNull
    FieldMapping suggestMapping(@NotNull DataSchema sourceSchema,
                                @NotNull DataSchema targetSchema);
}
