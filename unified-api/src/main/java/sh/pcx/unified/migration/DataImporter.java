/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Interface for data importers that can import data from other plugins.
 *
 * <p>DataImporter provides a standardized way to import data from various
 * source plugins into the unified data format. Implementations handle the
 * specifics of reading and converting data from their source format.
 *
 * <h2>Implementing a Custom Importer</h2>
 * <pre>{@code
 * @Importer(
 *     name = "MyOldPlugin",
 *     description = "Import from MyOldPlugin v1.x",
 *     detectionFile = "plugins/MyOldPlugin/data.yml"
 * )
 * public class MyOldPluginImporter implements DataImporter {
 *
 *     @Override
 *     public String getIdentifier() {
 *         return "my-old-plugin";
 *     }
 *
 *     @Override
 *     public boolean canImport() {
 *         return Files.exists(Paths.get("plugins/MyOldPlugin/data.yml"));
 *     }
 *
 *     @Override
 *     public ImportResult doImport(ImportContext context) {
 *         // Read and convert data
 *         int imported = 0;
 *         for (String uuidStr : config.getKeys(false)) {
 *             try {
 *                 context.save(uuid, data);
 *                 imported++;
 *                 context.reportProgress((double) imported / total);
 *             } catch (Exception e) {
 *                 context.reportSkipped(uuidStr, e.getMessage());
 *             }
 *         }
 *         return ImportResult.success(imported, skipped);
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Importer implementations should be thread-safe for the detection methods
 * ({@link #canImport()}, {@link #getSourceInfo()}). The {@link #doImport}
 * method is called in a controlled manner and does not require thread safety.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Importer
 * @see ImportContext
 * @see ImportResult
 */
public interface DataImporter {

    /**
     * Returns the unique identifier for this importer.
     *
     * <p>The identifier should be lowercase, hyphen-separated, and stable
     * across versions (e.g., "per-world-inventory", "essentials-x").
     *
     * @return the importer identifier
     * @since 1.0.0
     */
    @NotNull
    String getIdentifier();

    /**
     * Returns the display name for this importer.
     *
     * <p>This is shown to users in UI and logs.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    String getDisplayName();

    /**
     * Returns a description of what this importer does.
     *
     * @return the description
     * @since 1.0.0
     */
    @NotNull
    String getDescription();

    /**
     * Returns the data types this importer can import.
     *
     * <p>Examples: "inventory", "economy", "homes", "warps"
     *
     * @return the set of supported data types
     * @since 1.0.0
     */
    @NotNull
    Set<String> getSupportedDataTypes();

    /**
     * Returns the versions of the source plugin supported by this importer.
     *
     * <p>Returns null if all versions are supported.
     *
     * @return the supported versions, or null for all versions
     * @since 1.0.0
     */
    @Nullable
    String getSupportedVersions();

    /**
     * Checks if this importer can perform an import.
     *
     * <p>This typically checks if the source plugin's data files exist and
     * are in a readable format.
     *
     * @return true if import is possible
     * @since 1.0.0
     */
    boolean canImport();

    /**
     * Returns information about the source data if available.
     *
     * <p>This is called when the importer is available to display information
     * to the user before starting the import.
     *
     * @return source information, or null if not available
     * @since 1.0.0
     */
    @Nullable
    SourceInfo getSourceInfo();

    /**
     * Returns the default source folder for this importer.
     *
     * @return the default source folder path
     * @since 1.0.0
     */
    @NotNull
    Path getDefaultSourceFolder();

    /**
     * Returns the schema of the source data.
     *
     * <p>The schema describes the structure of the source data, which is used
     * for field mapping and validation.
     *
     * @return the source data schema
     * @since 1.0.0
     */
    @NotNull
    DataSchema getSourceSchema();

    /**
     * Returns the fields that can be mapped from this source.
     *
     * @return the list of mappable fields
     * @since 1.0.0
     */
    @NotNull
    List<MappableField> getMappableFields();

    /**
     * Returns the default field mapping for this importer.
     *
     * <p>This provides sensible defaults for mapping source fields to target
     * fields in the unified format.
     *
     * @return the default field mapping
     * @since 1.0.0
     */
    @NotNull
    FieldMapping getDefaultMapping();

    /**
     * Validates the import configuration before execution.
     *
     * @param context the import context with configuration
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    ValidationResult validateConfiguration(@NotNull ImportContext context);

    /**
     * Estimates the scope of the import operation.
     *
     * <p>This provides an estimate of how many records will be processed,
     * useful for progress tracking and resource planning.
     *
     * @param context the import context
     * @return the import estimate
     * @since 1.0.0
     */
    @NotNull
    ImportEstimate estimateImport(@NotNull ImportContext context);

    /**
     * Performs the actual import operation.
     *
     * <p>This method is called after validation passes. It should read data
     * from the source, convert it to the target format, and save it using
     * the context.
     *
     * @param context the import context
     * @return the import result
     * @since 1.0.0
     */
    @NotNull
    ImportResult doImport(@NotNull ImportContext context);

    /**
     * Information about the source data.
     *
     * @param pluginName    the source plugin name
     * @param pluginVersion the detected plugin version
     * @param recordCount   the estimated number of records
     * @param dataSize      the estimated data size in bytes
     * @param lastModified  when the data was last modified
     * @since 1.0.0
     */
    record SourceInfo(
            @NotNull String pluginName,
            @Nullable String pluginVersion,
            long recordCount,
            long dataSize,
            @Nullable java.time.Instant lastModified
    ) {}
}
