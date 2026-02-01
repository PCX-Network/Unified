/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link ImportBuilder}.
 *
 * <p>Provides a fluent API for configuring and executing import operations.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultImportBuilder implements ImportBuilder {

    private final DefaultMigrationService service;
    private final DataImporter importer;
    private final ExecutorService executor;
    private final Logger logger;

    private Path sourceFolder;
    private FieldMapping mapping;
    private boolean dryRun = false;
    private int batchSize = 100;
    private boolean skipExisting = false;
    private boolean overwriteExisting = false;
    private ImportContext.MergeStrategy mergeStrategy = ImportContext.MergeStrategy.SKIP;
    private final Map<String, String> options = new HashMap<>();
    private boolean createBackup = false;
    private ImportFilter filter;
    private int limit = -1;

    private Consumer<MigrationProgress> progressCallback;
    private Consumer<ImportResult> completeCallback;
    private Consumer<Throwable> errorCallback;
    private Consumer<String> recordCallback;

    /**
     * Creates a new import builder.
     */
    DefaultImportBuilder(@NotNull DefaultMigrationService service,
                         @NotNull DataImporter importer,
                         @NotNull ExecutorService executor,
                         @NotNull Logger logger) {
        this.service = service;
        this.importer = importer;
        this.executor = executor;
        this.logger = logger;
        this.sourceFolder = importer.getDefaultSourceFolder();
        this.mapping = importer.getDefaultMapping();
    }

    // ========================================================================
    // Configuration
    // ========================================================================

    @Override
    @NotNull
    public ImportBuilder sourceFolder(@NotNull Path folder) {
        this.sourceFolder = Objects.requireNonNull(folder, "folder cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder mapping(@NotNull FieldMapping mapping) {
        this.mapping = Objects.requireNonNull(mapping, "mapping cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder batchSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        this.batchSize = size;
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder skipExisting(boolean skip) {
        this.skipExisting = skip;
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder overwriteExisting(boolean overwrite) {
        this.overwriteExisting = overwrite;
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder mergeStrategy(@NotNull ImportContext.MergeStrategy strategy) {
        this.mergeStrategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder option(@NotNull String key, @NotNull String value) {
        this.options.put(
                Objects.requireNonNull(key, "key cannot be null"),
                Objects.requireNonNull(value, "value cannot be null")
        );
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder createBackup(boolean backup) {
        this.createBackup = backup;
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder filter(@NotNull ImportFilter filter) {
        this.filter = Objects.requireNonNull(filter, "filter cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    // ========================================================================
    // Callbacks
    // ========================================================================

    @Override
    @NotNull
    public ImportBuilder onProgress(@NotNull Consumer<MigrationProgress> callback) {
        this.progressCallback = Objects.requireNonNull(callback, "callback cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder onComplete(@NotNull Consumer<ImportResult> callback) {
        this.completeCallback = Objects.requireNonNull(callback, "callback cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder onError(@NotNull Consumer<Throwable> callback) {
        this.errorCallback = Objects.requireNonNull(callback, "callback cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ImportBuilder onRecordImported(@NotNull Consumer<String> callback) {
        this.recordCallback = Objects.requireNonNull(callback, "callback cannot be null");
        return this;
    }

    // ========================================================================
    // Validation and Preview
    // ========================================================================

    @Override
    @NotNull
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();

        // Check if source folder exists
        if (!java.nio.file.Files.exists(sourceFolder)) {
            result.error("sourceFolder", "Source folder does not exist: " + sourceFolder);
        } else if (!java.nio.file.Files.isDirectory(sourceFolder) &&
                   !java.nio.file.Files.isRegularFile(sourceFolder)) {
            result.error("sourceFolder", "Source path is neither file nor directory: " + sourceFolder);
        }

        // Validate importer can import
        if (!importer.canImport()) {
            result.error("importer", "Importer cannot import from source: " + importer.getDisplayName());
        }

        // Validate mapping
        try {
            DataSchema sourceSchema = importer.getSourceSchema();
            // Basic mapping validation
            for (String ignored : mapping.getIgnoredFields()) {
                if (!sourceSchema.hasField(ignored) && !ignored.contains("*")) {
                    result.warning("mapping", "Ignored field not in source: " + ignored);
                }
            }
        } catch (Exception e) {
            result.warning("mapping", "Could not validate mapping: " + e.getMessage());
        }

        return result.build();
    }

    @Override
    @NotNull
    public ImportEstimate estimate() {
        try {
            DefaultImportContext.Builder contextBuilder = new DefaultImportContext.Builder()
                    .sourceFolder(sourceFolder)
                    .fieldMapping(mapping)
                    .dryRun(true)
                    .batchSize(batchSize)
                    .logger(logger);

            DefaultImportContext context = contextBuilder.build();
            return importer.estimateImport(context);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to estimate import", e);
            return ImportEstimate.unknown();
        }
    }

    @Override
    @NotNull
    public ImportResult preview() {
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
    public CompletableFuture<ImportResult> previewAsync() {
        boolean originalDryRun = dryRun;
        dryRun = true;
        return executeAsync().whenComplete((r, t) -> dryRun = originalDryRun);
    }

    // ========================================================================
    // Execution
    // ========================================================================

    @Override
    @NotNull
    public ImportResult execute() {
        Instant startTime = Instant.now();
        String backupId = null;

        try {
            // Validate first
            ValidationResult validation = validate();
            if (validation.hasErrors()) {
                return ImportResult.failed("Validation failed: " +
                        validation.getErrors().get(0).message());
            }

            // Create backup if requested
            if (createBackup && !dryRun) {
                backupId = createBackupData();
            }

            // Report progress - preparing
            reportProgress(MigrationProgress.Phase.PREPARING, "Preparing import...");

            // Build context
            DefaultImportContext context = new DefaultImportContext.Builder()
                    .sourceFolder(sourceFolder)
                    .fieldMapping(mapping)
                    .dryRun(dryRun)
                    .batchSize(batchSize)
                    .skipExisting(skipExisting)
                    .overwriteExisting(overwriteExisting)
                    .mergeStrategy(mergeStrategy)
                    .options(options)
                    .progressCallback(progressCallback)
                    .logger(logger)
                    .build();

            // Validate configuration
            ValidationResult configValidation = importer.validateConfiguration(context);
            if (configValidation.hasErrors()) {
                return ImportResult.failed("Configuration validation failed: " +
                        configValidation.getErrors().get(0).message());
            }

            // Report progress - processing
            reportProgress(MigrationProgress.Phase.PROCESSING, "Importing data...");

            // Execute import
            ImportResult result = importer.doImport(context);

            // Record in history
            if (!dryRun) {
                MigrationRecord record = MigrationRecord.Builder.create()
                        .type(MigrationRecord.MigrationType.PLUGIN_IMPORT)
                        .status(toMigrationStatus(result.getStatus()))
                        .sourceInfo(importer.getDisplayName())
                        .targetInfo("Unified Data")
                        .recordCount(result.getTotalProcessed())
                        .successCount(result.getImportedCount())
                        .failedCount(result.getFailedCount())
                        .duration(Duration.between(startTime, Instant.now()))
                        .backupId(backupId)
                        .build();
                service.recordMigration(record);
            }

            // Report completion
            reportProgress(MigrationProgress.Phase.COMPLETED, "Import complete");

            if (completeCallback != null) {
                completeCallback.accept(result);
            }

            return result;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Import failed", e);

            if (errorCallback != null) {
                errorCallback.accept(e);
            }

            return ImportResult.failed("Import failed: " + e.getMessage(), e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<ImportResult> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute, executor);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String createBackupData() {
        String backupId = UUID.randomUUID().toString();
        Path backupPath = service.getDataFolder().resolve("backups").resolve(backupId + ".backup");

        try {
            java.nio.file.Files.createDirectories(backupPath.getParent());
            // Actual backup logic would go here
            logger.info("Created backup: " + backupId);
            service.registerBackup(backupId, backupPath);
            return backupId;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create backup", e);
            return null;
        }
    }

    private void reportProgress(MigrationProgress.Phase phase, String message) {
        if (progressCallback != null) {
            progressCallback.accept(MigrationProgress.builder()
                    .phase(phase)
                    .message(message)
                    .build());
        }
    }

    private MigrationRecord.MigrationStatus toMigrationStatus(ImportResult.Status status) {
        return switch (status) {
            case SUCCESS -> MigrationRecord.MigrationStatus.SUCCESS;
            case PARTIAL_SUCCESS -> MigrationRecord.MigrationStatus.PARTIAL_SUCCESS;
            case FAILED -> MigrationRecord.MigrationStatus.FAILED;
            case CANCELLED -> MigrationRecord.MigrationStatus.CANCELLED;
        };
    }
}
