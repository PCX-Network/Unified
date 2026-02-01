/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link StorageMigrationBuilder}.
 *
 * <p>Provides storage migration capabilities between different backends
 * (YAML, JSON, SQLite, MySQL, PostgreSQL, etc.).
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultStorageMigrationBuilder implements StorageMigrationBuilder {

    private final DefaultMigrationService service;
    private final ExecutorService executor;
    private final Logger logger;

    private StorageType fromType;
    private StorageType toType;
    private Path fromPath;
    private Path toPath;
    private Object fromConfig;
    private Object toConfig;
    private String fromConnectionString;
    private String toConnectionString;

    private int batchSize = 100;
    private boolean validateSchema = true;
    private boolean createSchema = true;
    private boolean createBackup = true;
    private boolean dryRun = false;
    private boolean truncateTarget = false;
    private boolean useTransaction = true;
    private ConflictStrategy conflictStrategy = ConflictStrategy.SKIP;
    private TableFilter tableFilter;
    private Set<String> includeTables = new HashSet<>();
    private Set<String> excludeTables = new HashSet<>();

    private Consumer<MigrationProgress> progressCallback;
    private Consumer<MigrationResult> completeCallback;
    private Consumer<Throwable> errorCallback;
    private Consumer<String> tableCallback;

    /**
     * Creates a new storage migration builder.
     */
    DefaultStorageMigrationBuilder(@NotNull DefaultMigrationService service,
                                   @NotNull ExecutorService executor,
                                   @NotNull Logger logger) {
        this.service = service;
        this.executor = executor;
        this.logger = logger;
    }

    // ========================================================================
    // Source Configuration
    // ========================================================================

    @Override
    @NotNull
    public StorageMigrationBuilder from(@NotNull StorageType type) {
        this.fromType = Objects.requireNonNull(type);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder fromPath(@NotNull Path path) {
        this.fromPath = Objects.requireNonNull(path);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder fromConfig(@NotNull Object config) {
        this.fromConfig = Objects.requireNonNull(config);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder fromConnectionString(@NotNull String connectionString) {
        this.fromConnectionString = Objects.requireNonNull(connectionString);
        return this;
    }

    // ========================================================================
    // Target Configuration
    // ========================================================================

    @Override
    @NotNull
    public StorageMigrationBuilder to(@NotNull StorageType type) {
        this.toType = Objects.requireNonNull(type);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder toPath(@NotNull Path path) {
        this.toPath = Objects.requireNonNull(path);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder toConfig(@NotNull Object config) {
        this.toConfig = Objects.requireNonNull(config);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder toConnectionString(@NotNull String connectionString) {
        this.toConnectionString = Objects.requireNonNull(connectionString);
        return this;
    }

    // ========================================================================
    // Migration Options
    // ========================================================================

    @Override
    @NotNull
    public StorageMigrationBuilder batchSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        this.batchSize = size;
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder validateSchema(boolean validate) {
        this.validateSchema = validate;
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder createSchema(boolean create) {
        this.createSchema = create;
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder createBackup(boolean backup) {
        this.createBackup = backup;
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder truncateTarget(boolean truncate) {
        this.truncateTarget = truncate;
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder useTransaction(boolean useTransaction) {
        this.useTransaction = useTransaction;
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder onConflict(@NotNull ConflictStrategy strategy) {
        this.conflictStrategy = Objects.requireNonNull(strategy);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder tableFilter(@NotNull TableFilter filter) {
        this.tableFilter = Objects.requireNonNull(filter);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder includeTables(@NotNull String... tables) {
        this.includeTables.addAll(Arrays.asList(tables));
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder excludeTables(@NotNull String... tables) {
        this.excludeTables.addAll(Arrays.asList(tables));
        return this;
    }

    // ========================================================================
    // Callbacks
    // ========================================================================

    @Override
    @NotNull
    public StorageMigrationBuilder onProgress(@NotNull Consumer<MigrationProgress> callback) {
        this.progressCallback = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder onComplete(@NotNull Consumer<MigrationResult> callback) {
        this.completeCallback = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder onError(@NotNull Consumer<Throwable> callback) {
        this.errorCallback = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    @NotNull
    public StorageMigrationBuilder onTableMigrated(@NotNull Consumer<String> callback) {
        this.tableCallback = Objects.requireNonNull(callback);
        return this;
    }

    // ========================================================================
    // Validation and Execution
    // ========================================================================

    @Override
    @NotNull
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();

        // Validate source
        if (fromType == null) {
            result.error("fromType", "Source storage type is required");
        } else if (fromType.isFileBased() && fromPath == null) {
            result.error("fromPath", "Source path is required for file-based storage");
        } else if (!fromType.isFileBased() && fromConfig == null && fromConnectionString == null) {
            result.error("fromConfig", "Source configuration or connection string required");
        }

        // Validate target
        if (toType == null) {
            result.error("toType", "Target storage type is required");
        } else if (toType.isFileBased() && toPath == null) {
            result.error("toPath", "Target path is required for file-based storage");
        } else if (!toType.isFileBased() && toConfig == null && toConnectionString == null) {
            result.error("toConfig", "Target configuration or connection string required");
        }

        // Validate source exists
        if (fromPath != null && !Files.exists(fromPath)) {
            result.error("fromPath", "Source path does not exist: " + fromPath);
        }

        // Warn about potential data loss
        if (truncateTarget) {
            result.warning("truncateTarget", "Target data will be deleted before migration");
        }

        return result.build();
    }

    @Override
    @NotNull
    public MigrationEstimate estimate() {
        try {
            // Gather table information
            List<MigrationEstimate.TableEstimate> tableEstimates = new ArrayList<>();
            long totalRecords = 0;
            long totalSize = 0;

            // This would scan the source to estimate
            // Placeholder implementation
            if (fromPath != null && Files.exists(fromPath)) {
                try (var stream = Files.walk(fromPath)) {
                    totalSize = stream
                            .filter(Files::isRegularFile)
                            .mapToLong(p -> {
                                try {
                                    return Files.size(p);
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .sum();
                }

                try (var stream = Files.list(fromPath)) {
                    var tables = stream.filter(p -> shouldMigrateTable(p.getFileName().toString()))
                            .toList();

                    for (Path tablePath : tables) {
                        String tableName = tablePath.getFileName().toString();
                        long records = countRecordsInTable(tablePath);
                        long size = Files.size(tablePath);

                        tableEstimates.add(new MigrationEstimate.TableEstimate(tableName, records, size));
                        totalRecords += records;
                    }
                }
            }

            // Estimate duration: roughly 5ms per record for file-based, 1ms for SQL
            long msPerRecord = fromType != null && fromType.isFileBased() ? 5 : 1;
            Duration estimatedDuration = Duration.ofMillis(totalRecords * msPerRecord);

            return new MigrationEstimate(
                    tableEstimates.size(),
                    totalRecords,
                    totalSize,
                    estimatedDuration,
                    tableEstimates,
                    List.of()
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to estimate migration", e);
            return MigrationEstimate.unknown();
        }
    }

    @Override
    @NotNull
    public MigrationResult preview() {
        boolean originalDryRun = dryRun;
        dryRun = true;
        try {
            return execute();
        } finally {
            dryRun = originalDryRun;
        }
    }

    @Override
    @NotNull
    public MigrationResult execute() {
        Instant startTime = Instant.now();
        String migrationId = UUID.randomUUID().toString();
        String backupId = null;
        List<MigrationResult.TableResult> tableResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Validate
            ValidationResult validation = validate();
            if (validation.hasErrors()) {
                return buildFailedResult(migrationId, startTime,
                        "Validation failed: " + validation.getErrors().get(0).message());
            }

            // Report progress - preparing
            reportProgress(MigrationProgress.Phase.PREPARING, "Preparing migration...");

            // Create backup if requested
            if (createBackup && !dryRun) {
                backupId = createBackupOfTarget();
            }

            // Truncate target if requested
            if (truncateTarget && !dryRun) {
                truncateTargetStorage();
            }

            // Get tables to migrate
            List<String> tables = getTablesToMigrate();
            int totalTables = tables.size();
            int currentTable = 0;
            int totalRecords = 0;
            int totalMigrated = 0;
            int totalFailed = 0;

            // Report progress - processing
            reportProgress(MigrationProgress.Phase.PROCESSING, "Migrating data...");

            for (String table : tables) {
                currentTable++;

                Instant tableStart = Instant.now();
                int tableRecords = 0;
                int tableMigrated = 0;
                int tableErrors = 0;

                try {
                    reportProgress(MigrationProgress.Phase.PROCESSING,
                            "Migrating table " + currentTable + "/" + totalTables + ": " + table);

                    // Migrate table
                    MigrateTableResult result = migrateTable(table);
                    tableRecords = result.total;
                    tableMigrated = result.migrated;
                    tableErrors = result.errors;

                    totalRecords += tableRecords;
                    totalMigrated += tableMigrated;
                    totalFailed += tableErrors;

                    if (tableCallback != null) {
                        tableCallback.accept(table);
                    }

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to migrate table: " + table, e);
                    errors.add("Table " + table + ": " + e.getMessage());
                    tableErrors++;
                }

                tableResults.add(new MigrationResult.TableResult(
                        table, tableRecords, tableMigrated, tableErrors,
                        Duration.between(tableStart, Instant.now())
                ));
            }

            // Report progress - complete
            reportProgress(MigrationProgress.Phase.COMPLETED, "Migration complete");

            // Determine status
            MigrationResult.Status status;
            if (totalFailed == 0) {
                status = MigrationResult.Status.SUCCESS;
            } else if (totalMigrated > 0) {
                status = MigrationResult.Status.PARTIAL_SUCCESS;
            } else {
                status = MigrationResult.Status.FAILED;
            }

            // Build result
            MigrationResult result = MigrationResult.builder()
                    .migrationId(migrationId)
                    .status(status)
                    .sourceType(fromType)
                    .targetType(toType)
                    .totalRecords(totalRecords)
                    .migratedCount(totalMigrated)
                    .failedCount(totalFailed)
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .dryRun(dryRun)
                    .tableResults(tableResults)
                    .errors(errors)
                    .warnings(warnings)
                    .backupId(backupId)
                    .build();

            // Record migration
            if (!dryRun) {
                recordMigration(result);
            }

            if (completeCallback != null) {
                completeCallback.accept(result);
            }

            return result;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Migration failed", e);

            if (errorCallback != null) {
                errorCallback.accept(e);
            }

            return buildFailedResult(migrationId, startTime, e.getMessage());
        }
    }

    @Override
    @NotNull
    public CompletableFuture<MigrationResult> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute, executor);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void reportProgress(MigrationProgress.Phase phase, String message) {
        if (progressCallback != null) {
            progressCallback.accept(MigrationProgress.builder()
                    .phase(phase)
                    .message(message)
                    .build());
        }
    }

    private MigrationResult buildFailedResult(String migrationId, Instant startTime, String error) {
        return MigrationResult.builder()
                .migrationId(migrationId)
                .status(MigrationResult.Status.FAILED)
                .sourceType(fromType)
                .targetType(toType)
                .startTime(startTime)
                .endTime(Instant.now())
                .dryRun(dryRun)
                .errorMessage(error)
                .build();
    }

    private boolean shouldMigrateTable(String tableName) {
        // Check exclusions first
        if (excludeTables.contains(tableName)) {
            return false;
        }

        // Check inclusions
        if (!includeTables.isEmpty() && !includeTables.contains(tableName)) {
            return false;
        }

        // Check custom filter
        if (tableFilter != null) {
            return tableFilter.shouldMigrate(tableName);
        }

        return true;
    }

    private long countRecordsInTable(Path tablePath) throws Exception {
        if (Files.isDirectory(tablePath)) {
            try (var stream = Files.list(tablePath)) {
                return stream.count();
            }
        }
        return 1;
    }

    private String createBackupOfTarget() {
        String backupId = UUID.randomUUID().toString();
        logger.info("Creating backup: " + backupId);
        // Actual backup implementation would go here
        return backupId;
    }

    private void truncateTargetStorage() {
        logger.info("Truncating target storage");
        // Actual truncation implementation would go here
    }

    private List<String> getTablesToMigrate() throws Exception {
        List<String> tables = new ArrayList<>();

        if (fromPath != null && Files.exists(fromPath)) {
            try (var stream = Files.list(fromPath)) {
                stream.filter(p -> shouldMigrateTable(p.getFileName().toString()))
                      .forEach(p -> tables.add(p.getFileName().toString()));
            }
        }

        return tables;
    }

    private MigrateTableResult migrateTable(String tableName) throws Exception {
        // Placeholder implementation
        // Actual implementation would read from source and write to target
        logger.info("Migrating table: " + tableName);
        return new MigrateTableResult(0, 0, 0);
    }

    private void recordMigration(MigrationResult result) {
        MigrationRecord record = MigrationRecord.Builder.create()
                .migrationId(result.getMigrationId())
                .type(MigrationRecord.MigrationType.STORAGE_MIGRATION)
                .status(toRecordStatus(result.getStatus()))
                .sourceInfo(fromType.getDisplayName())
                .targetInfo(toType.getDisplayName())
                .recordCount(result.getTotalRecords())
                .successCount(result.getMigratedCount())
                .failedCount(result.getFailedCount())
                .duration(result.getDuration())
                .backupId(result.getBackupId())
                .build();
        service.recordMigration(record);
    }

    private MigrationRecord.MigrationStatus toRecordStatus(MigrationResult.Status status) {
        return switch (status) {
            case SUCCESS -> MigrationRecord.MigrationStatus.SUCCESS;
            case PARTIAL_SUCCESS -> MigrationRecord.MigrationStatus.PARTIAL_SUCCESS;
            case FAILED -> MigrationRecord.MigrationStatus.FAILED;
            case CANCELLED -> MigrationRecord.MigrationStatus.CANCELLED;
        };
    }

    private record MigrateTableResult(int total, int migrated, int errors) {}
}
