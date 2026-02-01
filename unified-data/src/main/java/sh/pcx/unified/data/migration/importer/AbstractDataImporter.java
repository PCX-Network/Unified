/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration.importer;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

/**
 * Abstract base class for data importers.
 *
 * <p>Provides common functionality for implementing {@link DataImporter}
 * including default implementations for several methods.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractDataImporter implements DataImporter {

    private final String identifier;
    private final String displayName;
    private final String description;
    private final Set<String> supportedDataTypes;
    private final String detectionPath;

    /**
     * Creates a new importer.
     *
     * @param identifier         the unique identifier
     * @param displayName        the display name
     * @param description        the description
     * @param supportedDataTypes the supported data types
     * @param detectionPath      the path used to detect availability
     */
    protected AbstractDataImporter(@NotNull String identifier,
                                   @NotNull String displayName,
                                   @NotNull String description,
                                   @NotNull Set<String> supportedDataTypes,
                                   @NotNull String detectionPath) {
        this.identifier = Objects.requireNonNull(identifier);
        this.displayName = Objects.requireNonNull(displayName);
        this.description = Objects.requireNonNull(description);
        this.supportedDataTypes = Set.copyOf(supportedDataTypes);
        this.detectionPath = Objects.requireNonNull(detectionPath);
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @Override
    @NotNull
    public String getDescription() {
        return description;
    }

    @Override
    @NotNull
    public Set<String> getSupportedDataTypes() {
        return supportedDataTypes;
    }

    @Override
    @Nullable
    public String getSupportedVersions() {
        return null; // All versions by default
    }

    @Override
    public boolean canImport() {
        return Files.exists(getDefaultSourceFolder());
    }

    @Override
    @Nullable
    public SourceInfo getSourceInfo() {
        if (!canImport()) {
            return null;
        }

        Path sourcePath = getDefaultSourceFolder();
        try {
            long recordCount = countRecords(sourcePath);
            long dataSize = calculateDataSize(sourcePath);
            java.time.Instant lastModified = Files.getLastModifiedTime(sourcePath).toInstant();

            return new SourceInfo(
                    displayName,
                    detectVersion(),
                    recordCount,
                    dataSize,
                    lastModified
            );
        } catch (Exception e) {
            return new SourceInfo(displayName, null, -1, -1, null);
        }
    }

    @Override
    @NotNull
    public Path getDefaultSourceFolder() {
        return Paths.get(detectionPath);
    }

    @Override
    @NotNull
    public List<MappableField> getMappableFields() {
        List<MappableField> fields = new ArrayList<>();
        for (var entry : getSourceSchema().getFields().entrySet()) {
            var field = entry.getValue();
            fields.add(new MappableField(
                    field.name(),
                    field.type(),
                    field.description(),
                    field.required(),
                    field.name(), // Use same name as suggestion by default
                    null,
                    null
            ));
        }
        return fields;
    }

    @Override
    @NotNull
    public FieldMapping getDefaultMapping() {
        return FieldMapping.identity();
    }

    @Override
    @NotNull
    public ValidationResult validateConfiguration(@NotNull ImportContext context) {
        ValidationResult.Builder result = ValidationResult.builder();

        if (!Files.exists(context.getSourceFolder())) {
            result.error("sourceFolder", "Source folder does not exist");
        }

        return result.build();
    }

    @Override
    @NotNull
    public ImportEstimate estimateImport(@NotNull ImportContext context) {
        try {
            long recordCount = countRecords(context.getSourceFolder());
            long dataSize = calculateDataSize(context.getSourceFolder());

            // Estimate duration: roughly 10ms per record
            Duration estimatedDuration = Duration.ofMillis(recordCount * 10);

            return new ImportEstimate(
                    recordCount,
                    dataSize,
                    estimatedDuration,
                    Map.of(),
                    dataSize * 2, // Rough memory estimate
                    null
            );
        } catch (Exception e) {
            return ImportEstimate.unknown();
        }
    }

    // ========================================================================
    // Helper Methods for Subclasses
    // ========================================================================

    /**
     * Counts the number of records in the source.
     *
     * @param sourcePath the source path
     * @return the record count
     * @throws Exception if counting fails
     */
    protected long countRecords(Path sourcePath) throws Exception {
        if (Files.isDirectory(sourcePath)) {
            try (var stream = Files.list(sourcePath)) {
                return stream.count();
            }
        }
        return 1;
    }

    /**
     * Calculates the total data size.
     *
     * @param sourcePath the source path
     * @return the data size in bytes
     * @throws Exception if calculation fails
     */
    protected long calculateDataSize(Path sourcePath) throws Exception {
        if (Files.isDirectory(sourcePath)) {
            try (var stream = Files.walk(sourcePath)) {
                return stream
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
        }
        return Files.size(sourcePath);
    }

    /**
     * Detects the version of the source plugin.
     *
     * @return the detected version, or null
     */
    @Nullable
    protected String detectVersion() {
        // Subclasses can override to detect version from config files
        return null;
    }

    /**
     * Reads a UUID from a string, handling various formats.
     *
     * @param str the string
     * @return the UUID, or null if invalid
     */
    @Nullable
    protected UUID parseUUID(@NotNull String str) {
        try {
            // Handle format with or without dashes
            if (str.length() == 32) {
                str = str.substring(0, 8) + "-" +
                      str.substring(8, 12) + "-" +
                      str.substring(12, 16) + "-" +
                      str.substring(16, 20) + "-" +
                      str.substring(20);
            }
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts a UUID from a filename.
     *
     * @param filename the filename
     * @return the UUID, or null if not found
     */
    @Nullable
    protected UUID extractUUIDFromFilename(@NotNull String filename) {
        // Remove extension
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            filename = filename.substring(0, dotIndex);
        }
        return parseUUID(filename);
    }

    /**
     * Reports progress during import.
     *
     * @param context   the import context
     * @param current   current record number
     * @param total     total record count
     */
    protected void reportProgress(@NotNull ImportContext context, int current, int total) {
        double progress = total > 0 ? (double) current / total : 0;
        context.reportProgress(progress, "Processing record " + current + " of " + total);
    }
}
