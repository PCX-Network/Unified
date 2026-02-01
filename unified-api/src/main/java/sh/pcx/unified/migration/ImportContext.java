/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Context provided to importers during the import operation.
 *
 * <p>The ImportContext provides access to configuration, field mappings,
 * progress reporting, and data persistence during an import operation.
 *
 * <h2>Example Usage in Importer</h2>
 * <pre>{@code
 * @Override
 * public ImportResult doImport(ImportContext context) {
 *     Path sourceFolder = context.getSourceFolder();
 *     FieldMapping mapping = context.getFieldMapping();
 *     boolean isDryRun = context.isDryRun();
 *
 *     int total = countRecords();
 *     int processed = 0;
 *
 *     for (String uuidStr : getRecords()) {
 *         try {
 *             UUID uuid = UUID.fromString(uuidStr);
 *             Map<String, Object> sourceData = readRecord(uuid);
 *             Map<String, Object> mappedData = context.applyMapping(sourceData);
 *
 *             if (!isDryRun) {
 *                 context.save(uuid, mappedData);
 *             }
 *
 *             processed++;
 *             context.reportProgress((double) processed / total);
 *         } catch (Exception e) {
 *             context.reportSkipped(uuidStr, e.getMessage());
 *         }
 *     }
 *
 *     return context.buildResult();
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataImporter
 * @see ImportResult
 */
public interface ImportContext {

    // ========================================================================
    // Configuration
    // ========================================================================

    /**
     * Returns the source folder for the import.
     *
     * @return the source folder path
     * @since 1.0.0
     */
    @NotNull
    Path getSourceFolder();

    /**
     * Returns the field mapping configuration.
     *
     * @return the field mapping
     * @since 1.0.0
     */
    @NotNull
    FieldMapping getFieldMapping();

    /**
     * Returns whether this is a dry run.
     *
     * <p>In dry run mode, no data should be persisted. The import should
     * proceed normally but skip actual saves.
     *
     * @return true if dry run mode
     * @since 1.0.0
     */
    boolean isDryRun();

    /**
     * Returns the batch size for processing.
     *
     * @return the batch size
     * @since 1.0.0
     */
    int getBatchSize();

    /**
     * Returns whether to skip existing records.
     *
     * @return true if existing records should be skipped
     * @since 1.0.0
     */
    boolean isSkipExisting();

    /**
     * Returns whether to overwrite existing records.
     *
     * @return true if existing records should be overwritten
     * @since 1.0.0
     */
    boolean isOverwriteExisting();

    /**
     * Returns the merge strategy for existing records.
     *
     * @return the merge strategy
     * @since 1.0.0
     */
    @NotNull
    MergeStrategy getMergeStrategy();

    /**
     * Returns additional options configured for the import.
     *
     * @param key the option key
     * @return the option value, or null if not set
     * @since 1.0.0
     */
    @Nullable
    String getOption(@NotNull String key);

    /**
     * Returns all configured options.
     *
     * @return an unmodifiable map of options
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> getOptions();

    // ========================================================================
    // Data Mapping
    // ========================================================================

    /**
     * Applies the configured field mapping to source data.
     *
     * @param sourceData the source data map
     * @return the mapped data
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> applyMapping(@NotNull Map<String, Object> sourceData);

    /**
     * Transforms a value using configured transformers.
     *
     * @param fieldName the field name
     * @param value     the value to transform
     * @return the transformed value
     * @since 1.0.0
     */
    @Nullable
    Object transformValue(@NotNull String fieldName, @Nullable Object value);

    // ========================================================================
    // Data Persistence
    // ========================================================================

    /**
     * Saves imported data for a player.
     *
     * <p>This method respects the dry run setting and merge strategy.
     *
     * @param playerId the player UUID
     * @param data     the data to save
     * @since 1.0.0
     */
    void save(@NotNull UUID playerId, @NotNull Map<String, Object> data);

    /**
     * Saves imported data for a named record.
     *
     * @param recordId the record identifier
     * @param data     the data to save
     * @since 1.0.0
     */
    void save(@NotNull String recordId, @NotNull Map<String, Object> data);

    /**
     * Saves imported data in batch.
     *
     * @param batch the batch of records to save (UUID to data mapping)
     * @since 1.0.0
     */
    void saveBatch(@NotNull Map<UUID, Map<String, Object>> batch);

    /**
     * Checks if a record already exists.
     *
     * @param playerId the player UUID
     * @return true if the record exists
     * @since 1.0.0
     */
    boolean recordExists(@NotNull UUID playerId);

    /**
     * Checks if a named record already exists.
     *
     * @param recordId the record identifier
     * @return true if the record exists
     * @since 1.0.0
     */
    boolean recordExists(@NotNull String recordId);

    // ========================================================================
    // Progress Reporting
    // ========================================================================

    /**
     * Reports the current progress.
     *
     * @param progress the progress (0.0 to 1.0)
     * @since 1.0.0
     */
    void reportProgress(double progress);

    /**
     * Reports progress with a status message.
     *
     * @param progress the progress (0.0 to 1.0)
     * @param message  the status message
     * @since 1.0.0
     */
    void reportProgress(double progress, @NotNull String message);

    /**
     * Reports that a record was successfully imported.
     *
     * @param recordId the record identifier
     * @since 1.0.0
     */
    void reportImported(@NotNull String recordId);

    /**
     * Reports that a record was skipped.
     *
     * @param recordId the record identifier
     * @param reason   the reason for skipping
     * @since 1.0.0
     */
    void reportSkipped(@NotNull String recordId, @NotNull String reason);

    /**
     * Reports that a record import failed.
     *
     * @param recordId the record identifier
     * @param error    the error message
     * @since 1.0.0
     */
    void reportFailed(@NotNull String recordId, @NotNull String error);

    /**
     * Reports that a record import failed with an exception.
     *
     * @param recordId  the record identifier
     * @param exception the exception
     * @since 1.0.0
     */
    void reportFailed(@NotNull String recordId, @NotNull Throwable exception);

    /**
     * Logs an informational message.
     *
     * @param message the message
     * @since 1.0.0
     */
    void logInfo(@NotNull String message);

    /**
     * Logs a warning message.
     *
     * @param message the message
     * @since 1.0.0
     */
    void logWarning(@NotNull String message);

    /**
     * Logs an error message.
     *
     * @param message the message
     * @since 1.0.0
     */
    void logError(@NotNull String message);

    // ========================================================================
    // Cancellation
    // ========================================================================

    /**
     * Checks if the import has been cancelled.
     *
     * <p>Importers should check this periodically and stop if true.
     *
     * @return true if cancelled
     * @since 1.0.0
     */
    boolean isCancelled();

    /**
     * Requests cancellation of the import.
     *
     * @since 1.0.0
     */
    void cancel();

    // ========================================================================
    // Result Building
    // ========================================================================

    /**
     * Builds the import result based on the context's tracked statistics.
     *
     * @return the import result
     * @since 1.0.0
     */
    @NotNull
    ImportResult buildResult();

    /**
     * Strategy for merging data when records already exist.
     *
     * @since 1.0.0
     */
    enum MergeStrategy {
        /** Skip the import if record exists */
        SKIP,
        /** Replace existing record completely */
        REPLACE,
        /** Merge fields, preferring imported values */
        MERGE_PREFER_NEW,
        /** Merge fields, preferring existing values */
        MERGE_PREFER_EXISTING,
        /** Merge fields using custom resolver */
        CUSTOM
    }
}
