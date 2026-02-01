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
 * Builder for configuring and executing import operations.
 *
 * <p>ImportBuilder provides a fluent API for configuring all aspects of an
 * import operation, including source location, field mapping, progress
 * tracking, and execution options.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Basic import
 * ImportResult result = migration.importFrom(Importers.PER_WORLD_INVENTORY)
 *     .sourceFolder(Paths.get("plugins/PerWorldInventory"))
 *     .execute();
 *
 * // Dry run with progress tracking
 * ImportResult preview = migration.importFrom("essentials-x")
 *     .sourceFolder(Paths.get("plugins/Essentials/userdata"))
 *     .mapping(FieldMapping.builder()
 *         .map("money", "balance")
 *         .ignore("temp_*")
 *         .build())
 *     .dryRun(true)
 *     .onProgress(progress -> updateProgressBar(progress))
 *     .execute();
 *
 * // Async import with callbacks
 * migration.importFrom(importer)
 *     .sourceFolder(dataPath)
 *     .batchSize(50)
 *     .onProgress(this::updateProgress)
 *     .onComplete(result -> showResult(result))
 *     .onError(error -> handleError(error))
 *     .executeAsync();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationService
 * @see DataImporter
 * @see ImportResult
 */
public interface ImportBuilder {

    /**
     * Sets the source folder for the import.
     *
     * <p>This overrides the importer's default source folder.
     *
     * @param folder the source folder path
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder sourceFolder(@NotNull Path folder);

    /**
     * Sets the field mapping configuration.
     *
     * <p>This overrides the importer's default mapping.
     *
     * @param mapping the field mapping
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder mapping(@NotNull FieldMapping mapping);

    /**
     * Enables or disables dry run mode.
     *
     * <p>In dry run mode, no data is actually saved. This allows previewing
     * the import results.
     *
     * @param dryRun true for dry run mode
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder dryRun(boolean dryRun);

    /**
     * Sets the batch size for processing.
     *
     * <p>Larger batches are more efficient but use more memory.
     * Default is typically 100.
     *
     * @param size the batch size
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder batchSize(int size);

    /**
     * Sets whether to skip existing records.
     *
     * @param skip true to skip existing records
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder skipExisting(boolean skip);

    /**
     * Sets whether to overwrite existing records.
     *
     * @param overwrite true to overwrite existing records
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder overwriteExisting(boolean overwrite);

    /**
     * Sets the merge strategy for existing records.
     *
     * @param strategy the merge strategy
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder mergeStrategy(@NotNull ImportContext.MergeStrategy strategy);

    /**
     * Sets an additional option.
     *
     * @param key   the option key
     * @param value the option value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder option(@NotNull String key, @NotNull String value);

    /**
     * Enables backup before import.
     *
     * <p>If enabled, existing data is backed up before the import,
     * allowing rollback if needed.
     *
     * @param backup true to create backup
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder createBackup(boolean backup);

    /**
     * Sets a filter for which records to import.
     *
     * @param filter the record filter
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder filter(@NotNull ImportFilter filter);

    /**
     * Limits the number of records to import.
     *
     * <p>Useful for testing imports with a subset of data.
     *
     * @param limit the maximum number of records
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder limit(int limit);

    // ========================================================================
    // Progress and Callbacks
    // ========================================================================

    /**
     * Sets the progress callback.
     *
     * <p>The callback is invoked periodically with progress updates
     * during the import.
     *
     * @param callback the progress callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder onProgress(@NotNull Consumer<MigrationProgress> callback);

    /**
     * Sets the completion callback.
     *
     * <p>Called when the import completes (success or failure).
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder onComplete(@NotNull Consumer<ImportResult> callback);

    /**
     * Sets the error callback.
     *
     * <p>Called when the import encounters an unrecoverable error.
     *
     * @param callback the error callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder onError(@NotNull Consumer<Throwable> callback);

    /**
     * Sets a callback for each imported record.
     *
     * @param callback the per-record callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ImportBuilder onRecordImported(@NotNull Consumer<String> callback);

    // ========================================================================
    // Validation and Preview
    // ========================================================================

    /**
     * Validates the import configuration.
     *
     * <p>Checks that the source folder exists, the mapping is valid,
     * and the import can proceed.
     *
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    ValidationResult validate();

    /**
     * Estimates the import scope.
     *
     * <p>Returns an estimate of records to process and time required.
     *
     * @return the import estimate
     * @since 1.0.0
     */
    @NotNull
    ImportEstimate estimate();

    /**
     * Previews the import without making changes.
     *
     * <p>Equivalent to executing with dryRun(true).
     *
     * @return the preview result
     * @since 1.0.0
     */
    @NotNull
    ImportResult preview();

    /**
     * Previews the import asynchronously.
     *
     * @return a future that completes with the preview result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ImportResult> previewAsync();

    // ========================================================================
    // Execution
    // ========================================================================

    /**
     * Executes the import synchronously.
     *
     * <p>This blocks until the import completes.
     *
     * @return the import result
     * @since 1.0.0
     */
    @NotNull
    ImportResult execute();

    /**
     * Executes the import asynchronously.
     *
     * <p>The import runs on a background thread and callbacks are
     * invoked as configured.
     *
     * @return a future that completes with the import result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ImportResult> executeAsync();

    /**
     * Filter interface for selecting which records to import.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface ImportFilter {
        /**
         * Tests if a record should be imported.
         *
         * @param recordId the record identifier
         * @return true if the record should be imported
         */
        boolean shouldImport(@NotNull String recordId);
    }
}
