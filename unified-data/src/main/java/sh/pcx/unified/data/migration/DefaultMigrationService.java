/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of the {@link MigrationService}.
 *
 * <p>This implementation provides:
 * <ul>
 *   <li>Registration and discovery of data importers</li>
 *   <li>Import and storage migration execution</li>
 *   <li>Migration history tracking</li>
 *   <li>Rollback support</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultMigrationService implements MigrationService {

    private final Logger logger;
    private final Path dataFolder;
    private final ExecutorService executor;
    private final Map<String, DataImporter> importers = new ConcurrentHashMap<>();
    private final List<MigrationRecord> history = new CopyOnWriteArrayList<>();
    private final Map<String, Path> backups = new ConcurrentHashMap<>();

    /**
     * Creates a new migration service.
     *
     * @param logger     the logger to use
     * @param dataFolder the plugin data folder
     */
    public DefaultMigrationService(@NotNull Logger logger, @NotNull Path dataFolder) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder cannot be null");
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        registerBuiltInImporters();
    }

    /**
     * Creates a new migration service with a custom executor.
     *
     * @param logger     the logger to use
     * @param dataFolder the plugin data folder
     * @param executor   the executor service
     */
    public DefaultMigrationService(@NotNull Logger logger, @NotNull Path dataFolder,
                                   @NotNull ExecutorService executor) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");

        registerBuiltInImporters();
    }

    private void registerBuiltInImporters() {
        // Register built-in importers
        registerImporter(new sh.pcx.unified.data.migration.importer.PerWorldInventoryImporter());
        registerImporter(new sh.pcx.unified.data.migration.importer.MultiVerseInventoriesImporter());
        registerImporter(new sh.pcx.unified.data.migration.importer.EssentialsXImporter());
        registerImporter(new sh.pcx.unified.data.migration.importer.MyWorldsImporter());

        logger.info("Registered " + importers.size() + " built-in importers");
    }

    // ========================================================================
    // Importer Discovery
    // ========================================================================

    @Override
    @NotNull
    public List<DataImporter> getRegisteredImporters() {
        return List.copyOf(importers.values());
    }

    @Override
    @NotNull
    public List<DataImporter> getAvailableImporters() {
        return importers.values().stream()
                .filter(DataImporter::canImport)
                .toList();
    }

    @Override
    @NotNull
    public Optional<DataImporter> findImporter(@NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return Optional.ofNullable(importers.get(identifier.toLowerCase()));
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends DataImporter> Optional<T> findImporter(@NotNull Class<T> importerType) {
        Objects.requireNonNull(importerType, "importerType cannot be null");
        return importers.values().stream()
                .filter(importerType::isInstance)
                .map(i -> (T) i)
                .findFirst();
    }

    @Override
    public void registerImporter(@NotNull DataImporter importer) {
        Objects.requireNonNull(importer, "importer cannot be null");
        String id = importer.getIdentifier().toLowerCase();
        if (importers.containsKey(id)) {
            throw new IllegalArgumentException("Importer already registered: " + id);
        }
        importers.put(id, importer);
        logger.fine("Registered importer: " + importer.getDisplayName());
    }

    @Override
    public boolean unregisterImporter(@NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return importers.remove(identifier.toLowerCase()) != null;
    }

    // ========================================================================
    // Import Operations
    // ========================================================================

    @Override
    @NotNull
    public ImportBuilder importFrom(@NotNull DataImporter importer) {
        Objects.requireNonNull(importer, "importer cannot be null");
        return new DefaultImportBuilder(this, importer, executor, logger);
    }

    @Override
    @NotNull
    public ImportBuilder importFrom(@NotNull String importerIdentifier) {
        Objects.requireNonNull(importerIdentifier, "importerIdentifier cannot be null");
        DataImporter importer = findImporter(importerIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Unknown importer: " + importerIdentifier));
        return importFrom(importer);
    }

    @Override
    @NotNull
    public ImportWizard createWizard() {
        return new DefaultImportWizard(this, executor, logger);
    }

    // ========================================================================
    // Storage Migration
    // ========================================================================

    @Override
    @NotNull
    public StorageMigrationBuilder migrateStorage() {
        return new DefaultStorageMigrationBuilder(this, executor, logger);
    }

    // ========================================================================
    // Export Operations
    // ========================================================================

    @Override
    @NotNull
    public ExportBuilder export() {
        return new DefaultExportBuilder(this, dataFolder, executor, logger);
    }

    // ========================================================================
    // Migration History
    // ========================================================================

    @Override
    @NotNull
    public List<MigrationRecord> getMigrationHistory() {
        return List.copyOf(history);
    }

    @Override
    @NotNull
    public Optional<MigrationRecord> findMigration(@NotNull String migrationId) {
        Objects.requireNonNull(migrationId, "migrationId cannot be null");
        return history.stream()
                .filter(r -> r.migrationId().equals(migrationId))
                .findFirst();
    }

    @Override
    @NotNull
    public CompletableFuture<RollbackResult> rollback(@NotNull String migrationId) {
        Objects.requireNonNull(migrationId, "migrationId cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            Optional<MigrationRecord> record = findMigration(migrationId);
            if (record.isEmpty()) {
                return RollbackResult.notFound(migrationId);
            }

            MigrationRecord migration = record.get();
            if (!migration.canRollback()) {
                return RollbackResult.failed(migrationId, "Rollback not available for this migration");
            }

            Path backupPath = backups.get(migration.backupId());
            if (backupPath == null || !Files.exists(backupPath)) {
                return RollbackResult.failed(migrationId, "Backup file not found");
            }

            try {
                // Perform rollback
                long startTime = System.currentTimeMillis();
                int restored = performRollback(backupPath);
                long duration = System.currentTimeMillis() - startTime;

                // Update history
                MigrationRecord rollbackRecord = MigrationRecord.Builder.create()
                        .type(MigrationRecord.MigrationType.ROLLBACK)
                        .status(MigrationRecord.MigrationStatus.SUCCESS)
                        .sourceInfo("Backup: " + migration.backupId())
                        .targetInfo(migration.targetInfo())
                        .recordCount(restored)
                        .successCount(restored)
                        .duration(java.time.Duration.ofMillis(duration))
                        .notes("Rollback of migration " + migrationId)
                        .build();
                history.add(rollbackRecord);

                return RollbackResult.success(migrationId, restored,
                        java.time.Duration.ofMillis(duration));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Rollback failed", e);
                return RollbackResult.failed(migrationId, e.getMessage());
            }
        }, executor);
    }

    private int performRollback(Path backupPath) throws Exception {
        // Implementation would restore from backup
        // This is a placeholder for the actual restoration logic
        logger.info("Restoring from backup: " + backupPath);
        return 0;
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    @Override
    @NotNull
    public Optional<StorageType> detectStorageType(@NotNull Path path) {
        Objects.requireNonNull(path, "path cannot be null");

        if (!Files.exists(path)) {
            return Optional.empty();
        }

        String fileName = path.getFileName().toString().toLowerCase();

        // Check file extension
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return Optional.of(StorageType.YAML);
        }
        if (fileName.endsWith(".json")) {
            return Optional.of(StorageType.JSON);
        }
        if (fileName.endsWith(".db") || fileName.endsWith(".sqlite")) {
            return Optional.of(StorageType.SQLITE);
        }
        if (fileName.endsWith(".h2.db")) {
            return Optional.of(StorageType.H2);
        }
        if (fileName.endsWith(".dat")) {
            return Optional.of(StorageType.NBT);
        }

        // Check if it's a directory (could be YAML/JSON storage)
        if (Files.isDirectory(path)) {
            try (var files = Files.list(path)) {
                Optional<Path> first = files.findFirst();
                if (first.isPresent()) {
                    return detectStorageType(first.get());
                }
            } catch (Exception e) {
                logger.fine("Error detecting storage type: " + e.getMessage());
            }
        }

        return Optional.empty();
    }

    @Override
    @NotNull
    public ValidationResult validateMapping(@NotNull FieldMapping mapping,
                                            @NotNull DataSchema sourceSchema,
                                            @NotNull DataSchema targetSchema) {
        Objects.requireNonNull(mapping, "mapping cannot be null");
        Objects.requireNonNull(sourceSchema, "sourceSchema cannot be null");
        Objects.requireNonNull(targetSchema, "targetSchema cannot be null");

        ValidationResult.Builder result = ValidationResult.builder();

        // Check that all mapped source fields exist
        for (String sourceField : mapping.getDirectMappings().keySet()) {
            if (!sourceSchema.hasField(sourceField)) {
                result.warning("sourceField", "Source field does not exist: " + sourceField);
            }
        }

        // Check that all required target fields are mapped
        for (var field : targetSchema.getRequiredFields()) {
            boolean isMapped = mapping.getDirectMappings().containsValue(field.name());
            if (!isMapped && mapping.getDefaultValue(field.name()) == null) {
                result.error("targetField", "Required target field not mapped: " + field.name());
            }
        }

        return result.build();
    }

    @Override
    @NotNull
    public FieldMapping suggestMapping(@NotNull DataSchema sourceSchema,
                                       @NotNull DataSchema targetSchema) {
        Objects.requireNonNull(sourceSchema, "sourceSchema cannot be null");
        Objects.requireNonNull(targetSchema, "targetSchema cannot be null");

        FieldMapping.Builder builder = FieldMapping.builder();

        for (String sourceField : sourceSchema.getFieldNames()) {
            // Exact match
            if (targetSchema.hasField(sourceField)) {
                builder.map(sourceField, sourceField);
                continue;
            }

            // Case-insensitive match
            for (String targetField : targetSchema.getFieldNames()) {
                if (sourceField.equalsIgnoreCase(targetField)) {
                    builder.map(sourceField, targetField);
                    break;
                }
            }

            // Common variations (e.g., armour -> armor)
            String normalized = normalizeFieldName(sourceField);
            for (String targetField : targetSchema.getFieldNames()) {
                if (normalizeFieldName(targetField).equals(normalized)) {
                    builder.map(sourceField, targetField);
                    break;
                }
            }
        }

        return builder.passUnmapped(false).build();
    }

    private String normalizeFieldName(String name) {
        return name.toLowerCase()
                .replace("armour", "armor")
                .replace("colour", "color")
                .replace("_", "")
                .replace("-", "");
    }

    // ========================================================================
    // Internal Methods
    // ========================================================================

    /**
     * Records a migration in the history.
     *
     * @param record the migration record
     */
    void recordMigration(@NotNull MigrationRecord record) {
        history.add(record);
    }

    /**
     * Registers a backup for potential rollback.
     *
     * @param backupId   the backup identifier
     * @param backupPath the backup file path
     */
    void registerBackup(@NotNull String backupId, @NotNull Path backupPath) {
        backups.put(backupId, backupPath);
    }

    /**
     * Returns the data folder.
     *
     * @return the data folder path
     */
    @NotNull
    Path getDataFolder() {
        return dataFolder;
    }

    // ========================================================================
    // Service Implementation
    // ========================================================================

    @Override
    public String getServiceName() {
        return "MigrationService";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Shuts down the migration service.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
