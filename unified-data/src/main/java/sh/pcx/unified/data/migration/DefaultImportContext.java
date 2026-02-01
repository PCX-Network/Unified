/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link ImportContext}.
 *
 * <p>Provides context to importers during import operations, handling
 * progress tracking, data mapping, and persistence.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultImportContext implements ImportContext {

    private final Path sourceFolder;
    private final FieldMapping fieldMapping;
    private final boolean dryRun;
    private final int batchSize;
    private final boolean skipExisting;
    private final boolean overwriteExisting;
    private final MergeStrategy mergeStrategy;
    private final Map<String, String> options;
    private final Consumer<MigrationProgress> progressCallback;
    private final Logger logger;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger importedCount = new AtomicInteger(0);
    private final AtomicInteger skippedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final List<ImportResult.ImportError> errors = Collections.synchronizedList(new ArrayList<>());
    private final List<ImportResult.SkippedRecord> skipped = Collections.synchronizedList(new ArrayList<>());
    private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Map<String, Object>> pendingBatch = new ConcurrentHashMap<>();
    private final Set<UUID> existingRecords = ConcurrentHashMap.newKeySet();
    private final Set<String> existingNamedRecords = ConcurrentHashMap.newKeySet();

    private final Instant startTime = Instant.now();
    private int totalCount = -1;

    /**
     * Creates a new import context.
     */
    DefaultImportContext(@NotNull Path sourceFolder,
                         @NotNull FieldMapping fieldMapping,
                         boolean dryRun,
                         int batchSize,
                         boolean skipExisting,
                         boolean overwriteExisting,
                         @NotNull MergeStrategy mergeStrategy,
                         @NotNull Map<String, String> options,
                         @Nullable Consumer<MigrationProgress> progressCallback,
                         @NotNull Logger logger) {
        this.sourceFolder = sourceFolder;
        this.fieldMapping = fieldMapping;
        this.dryRun = dryRun;
        this.batchSize = batchSize;
        this.skipExisting = skipExisting;
        this.overwriteExisting = overwriteExisting;
        this.mergeStrategy = mergeStrategy;
        this.options = Map.copyOf(options);
        this.progressCallback = progressCallback;
        this.logger = logger;
    }

    // ========================================================================
    // Configuration
    // ========================================================================

    @Override
    @NotNull
    public Path getSourceFolder() {
        return sourceFolder;
    }

    @Override
    @NotNull
    public FieldMapping getFieldMapping() {
        return fieldMapping;
    }

    @Override
    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public boolean isSkipExisting() {
        return skipExisting;
    }

    @Override
    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    @Override
    @NotNull
    public MergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }

    @Override
    @Nullable
    public String getOption(@NotNull String key) {
        return options.get(key);
    }

    @Override
    @NotNull
    public Map<String, String> getOptions() {
        return options;
    }

    // ========================================================================
    // Data Mapping
    // ========================================================================

    @Override
    @NotNull
    public Map<String, Object> applyMapping(@NotNull Map<String, Object> sourceData) {
        return fieldMapping.apply(sourceData);
    }

    @Override
    @Nullable
    public Object transformValue(@NotNull String fieldName, @Nullable Object value) {
        return fieldMapping.transform(fieldName, value);
    }

    // ========================================================================
    // Data Persistence
    // ========================================================================

    @Override
    public void save(@NotNull UUID playerId, @NotNull Map<String, Object> data) {
        if (isCancelled()) {
            return;
        }

        if (skipExisting && recordExists(playerId)) {
            reportSkipped(playerId.toString(), "Record already exists");
            return;
        }

        if (!dryRun) {
            // Apply merge strategy if record exists
            if (recordExists(playerId)) {
                switch (mergeStrategy) {
                    case SKIP -> {
                        reportSkipped(playerId.toString(), "Record exists, merge strategy: SKIP");
                        return;
                    }
                    case REPLACE -> {
                        // Continue to save (replace)
                    }
                    case MERGE_PREFER_NEW, MERGE_PREFER_EXISTING -> {
                        data = mergeData(playerId, data);
                    }
                    case CUSTOM -> {
                        // Custom handling would be configured separately
                    }
                }
            }

            pendingBatch.put(playerId, data);
            if (pendingBatch.size() >= batchSize) {
                flushBatch();
            }
        }

        reportImported(playerId.toString());
    }

    @Override
    public void save(@NotNull String recordId, @NotNull Map<String, Object> data) {
        if (isCancelled()) {
            return;
        }

        if (skipExisting && recordExists(recordId)) {
            reportSkipped(recordId, "Record already exists");
            return;
        }

        if (!dryRun) {
            // Actual persistence would happen here
            logger.fine("Saving record: " + recordId);
        }

        reportImported(recordId);
    }

    @Override
    public void saveBatch(@NotNull Map<UUID, Map<String, Object>> batch) {
        for (var entry : batch.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean recordExists(@NotNull UUID playerId) {
        return existingRecords.contains(playerId);
    }

    @Override
    public boolean recordExists(@NotNull String recordId) {
        return existingNamedRecords.contains(recordId);
    }

    private Map<String, Object> mergeData(UUID playerId, Map<String, Object> newData) {
        // Placeholder for actual merge logic
        // Would fetch existing data and merge based on strategy
        return newData;
    }

    private void flushBatch() {
        if (pendingBatch.isEmpty() || dryRun) {
            pendingBatch.clear();
            return;
        }

        try {
            // Actual batch persistence would happen here
            logger.fine("Flushing batch of " + pendingBatch.size() + " records");
            pendingBatch.clear();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Batch flush failed", e);
            for (UUID id : pendingBatch.keySet()) {
                reportFailed(id.toString(), e);
            }
            pendingBatch.clear();
        }
    }

    // ========================================================================
    // Progress Reporting
    // ========================================================================

    @Override
    public void reportProgress(double progress) {
        reportProgress(progress, null);
    }

    @Override
    public void reportProgress(double progress, @NotNull String message) {
        if (progressCallback != null) {
            MigrationProgress.Builder builder = MigrationProgress.builder()
                    .phase(MigrationProgress.Phase.PROCESSING)
                    .processedCount(importedCount.get() + skippedCount.get() + failedCount.get())
                    .successCount(importedCount.get())
                    .failedCount(failedCount.get())
                    .skippedCount(skippedCount.get())
                    .startTime(startTime);

            if (totalCount > 0) {
                builder.totalCount(totalCount);
            }
            if (message != null) {
                builder.message(message);
            }

            progressCallback.accept(builder.build());
        }
    }

    @Override
    public void reportImported(@NotNull String recordId) {
        importedCount.incrementAndGet();
    }

    @Override
    public void reportSkipped(@NotNull String recordId, @NotNull String reason) {
        skippedCount.incrementAndGet();
        skipped.add(new ImportResult.SkippedRecord(recordId, reason));
    }

    @Override
    public void reportFailed(@NotNull String recordId, @NotNull String error) {
        failedCount.incrementAndGet();
        errors.add(new ImportResult.ImportError(recordId, error, null));
    }

    @Override
    public void reportFailed(@NotNull String recordId, @NotNull Throwable exception) {
        failedCount.incrementAndGet();
        errors.add(new ImportResult.ImportError(recordId, exception.getMessage(), exception));
    }

    @Override
    public void logInfo(@NotNull String message) {
        logger.info(message);
    }

    @Override
    public void logWarning(@NotNull String message) {
        logger.warning(message);
        warnings.add(message);
    }

    @Override
    public void logError(@NotNull String message) {
        logger.severe(message);
    }

    // ========================================================================
    // Cancellation
    // ========================================================================

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    // ========================================================================
    // Result Building
    // ========================================================================

    @Override
    @NotNull
    public ImportResult buildResult() {
        // Flush any pending batch
        flushBatch();

        int totalProcessed = importedCount.get() + skippedCount.get() + failedCount.get();
        ImportResult.Status status;

        if (cancelled.get()) {
            status = ImportResult.Status.CANCELLED;
        } else if (failedCount.get() == 0 && skippedCount.get() == 0) {
            status = ImportResult.Status.SUCCESS;
        } else if (importedCount.get() > 0) {
            status = ImportResult.Status.PARTIAL_SUCCESS;
        } else {
            status = ImportResult.Status.FAILED;
        }

        return ImportResult.builder()
                .status(status)
                .totalProcessed(totalProcessed)
                .importedCount(importedCount.get())
                .skippedCount(skippedCount.get())
                .failedCount(failedCount.get())
                .startTime(startTime)
                .endTime(Instant.now())
                .dryRun(dryRun)
                .errors(new ArrayList<>(errors))
                .skipped(new ArrayList<>(skipped))
                .warnings(new ArrayList<>(warnings))
                .build();
    }

    // ========================================================================
    // Internal Methods
    // ========================================================================

    /**
     * Sets the total count for progress tracking.
     *
     * @param total the total count
     */
    void setTotalCount(int total) {
        this.totalCount = total;
    }

    /**
     * Marks a record as existing.
     *
     * @param playerId the player UUID
     */
    void markAsExisting(@NotNull UUID playerId) {
        existingRecords.add(playerId);
    }

    /**
     * Marks a named record as existing.
     *
     * @param recordId the record identifier
     */
    void markAsExisting(@NotNull String recordId) {
        existingNamedRecords.add(recordId);
    }

    /**
     * Builder for creating DefaultImportContext instances.
     */
    static final class Builder {
        private Path sourceFolder;
        private FieldMapping fieldMapping = FieldMapping.identity();
        private boolean dryRun = false;
        private int batchSize = 100;
        private boolean skipExisting = false;
        private boolean overwriteExisting = false;
        private MergeStrategy mergeStrategy = MergeStrategy.SKIP;
        private Map<String, String> options = new HashMap<>();
        private Consumer<MigrationProgress> progressCallback;
        private Logger logger;

        Builder sourceFolder(Path sourceFolder) {
            this.sourceFolder = sourceFolder;
            return this;
        }

        Builder fieldMapping(FieldMapping fieldMapping) {
            this.fieldMapping = fieldMapping;
            return this;
        }

        Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        Builder skipExisting(boolean skipExisting) {
            this.skipExisting = skipExisting;
            return this;
        }

        Builder overwriteExisting(boolean overwriteExisting) {
            this.overwriteExisting = overwriteExisting;
            return this;
        }

        Builder mergeStrategy(MergeStrategy mergeStrategy) {
            this.mergeStrategy = mergeStrategy;
            return this;
        }

        Builder options(Map<String, String> options) {
            this.options = new HashMap<>(options);
            return this;
        }

        Builder option(String key, String value) {
            this.options.put(key, value);
            return this;
        }

        Builder progressCallback(Consumer<MigrationProgress> progressCallback) {
            this.progressCallback = progressCallback;
            return this;
        }

        Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        DefaultImportContext build() {
            Objects.requireNonNull(sourceFolder, "sourceFolder is required");
            Objects.requireNonNull(logger, "logger is required");
            return new DefaultImportContext(
                    sourceFolder, fieldMapping, dryRun, batchSize,
                    skipExisting, overwriteExisting, mergeStrategy,
                    options, progressCallback, logger
            );
        }
    }
}
