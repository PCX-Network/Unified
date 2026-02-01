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
 * Builder for configuring and executing data export operations.
 *
 * <p>ExportBuilder provides a fluent API for exporting data to various formats.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Export to JSON
 * ExportResult result = migration.export()
 *     .format(ExportFormat.JSON)
 *     .destination(Paths.get("exports/backup.json"))
 *     .includeMetadata(true)
 *     .compress(true)
 *     .execute();
 *
 * // Export specific data types
 * ExportResult result = migration.export()
 *     .format(ExportFormat.YAML)
 *     .destination(Paths.get("exports/players"))
 *     .dataTypes("inventory", "economy")
 *     .filter(record -> record.get("balance") > 1000)
 *     .executeAsync();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationService
 * @see ExportResult
 */
public interface ExportBuilder {

    /**
     * Sets the export format.
     *
     * @param format the export format
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder format(@NotNull ExportFormat format);

    /**
     * Sets the destination path.
     *
     * @param path the destination path
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder destination(@NotNull Path path);

    /**
     * Sets the data types to export.
     *
     * @param types the data types
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder dataTypes(@NotNull String... types);

    /**
     * Sets whether to include metadata.
     *
     * @param include true to include metadata
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder includeMetadata(boolean include);

    /**
     * Sets whether to compress the export.
     *
     * @param compress true to compress
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder compress(boolean compress);

    /**
     * Sets a filter for records to export.
     *
     * @param filter the record filter
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder filter(@NotNull ExportFilter filter);

    /**
     * Sets whether to pretty-print the output.
     *
     * @param pretty true for pretty printing
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder prettyPrint(boolean pretty);

    /**
     * Sets the progress callback.
     *
     * @param callback the progress callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ExportBuilder onProgress(@NotNull Consumer<MigrationProgress> callback);

    /**
     * Executes the export synchronously.
     *
     * @return the export result
     * @since 1.0.0
     */
    @NotNull
    ExportResult execute();

    /**
     * Executes the export asynchronously.
     *
     * @return a future that completes with the export result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ExportResult> executeAsync();

    /**
     * Export format options.
     *
     * @since 1.0.0
     */
    enum ExportFormat {
        /** JSON format */
        JSON(".json"),
        /** YAML format */
        YAML(".yml"),
        /** CSV format */
        CSV(".csv"),
        /** SQL dump format */
        SQL(".sql"),
        /** Binary format (for backup/restore) */
        BINARY(".dat");

        private final String extension;

        ExportFormat(String extension) {
            this.extension = extension;
        }

        /**
         * Returns the file extension.
         *
         * @return the extension
         */
        @NotNull
        public String getExtension() {
            return extension;
        }
    }

    /**
     * Filter interface for export records.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface ExportFilter {
        /**
         * Tests if a record should be exported.
         *
         * @param record the record data
         * @return true if the record should be exported
         */
        boolean shouldExport(@NotNull java.util.Map<String, Object> record);
    }
}
