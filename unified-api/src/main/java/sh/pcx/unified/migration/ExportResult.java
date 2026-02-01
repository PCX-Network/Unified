/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Result of a data export operation.
 *
 * <p>ExportResult contains information about the outcome of an export,
 * including the output location, record count, and any errors.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ExportResult result = migration.export()
 *     .format(ExportFormat.JSON)
 *     .destination(Paths.get("backup.json"))
 *     .execute();
 *
 * if (result.isSuccess()) {
 *     System.out.println("Exported " + result.recordCount() + " records");
 *     System.out.println("Output: " + result.outputPath());
 *     System.out.println("Size: " + result.fileSizeBytes() + " bytes");
 * } else {
 *     System.err.println("Export failed: " + result.errorMessage());
 * }
 * }</pre>
 *
 * @param exportId       unique export identifier
 * @param status         the export status
 * @param format         the export format
 * @param outputPath     the output file path
 * @param recordCount    number of records exported
 * @param fileSizeBytes  size of the output file
 * @param timestamp      when the export was performed
 * @param duration       how long the export took
 * @param errorMessage   error message if failed
 * @param warnings       any warnings during export
 * @since 1.0.0
 * @author Supatuck
 * @see ExportBuilder
 * @see MigrationService
 */
public record ExportResult(
        @NotNull String exportId,
        @NotNull Status status,
        @NotNull ExportBuilder.ExportFormat format,
        @Nullable Path outputPath,
        int recordCount,
        long fileSizeBytes,
        @NotNull Instant timestamp,
        @NotNull Duration duration,
        @Nullable String errorMessage,
        @NotNull List<String> warnings
) {

    /**
     * Compact constructor with validation.
     */
    public ExportResult {
        Objects.requireNonNull(exportId, "exportId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(format, "format cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    /**
     * Checks if the export was successful.
     *
     * @return true if successful
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the export failed.
     *
     * @return true if failed
     * @since 1.0.0
     */
    public boolean isFailed() {
        return status == Status.FAILED;
    }

    /**
     * Checks if there are any warnings.
     *
     * @return true if there are warnings
     * @since 1.0.0
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Returns the file size formatted as a human-readable string.
     *
     * @return formatted size string
     * @since 1.0.0
     */
    @NotNull
    public String getFormattedSize() {
        return formatBytes(fileSizeBytes);
    }

    /**
     * Returns a summary of the export.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Export ").append(status.getDisplayName());
        sb.append("\n  Format: ").append(format.name());
        if (outputPath != null) {
            sb.append("\n  Output: ").append(outputPath);
        }
        sb.append("\n  Records: ").append(recordCount);
        sb.append("\n  Size: ").append(getFormattedSize());
        sb.append("\n  Duration: ").append(formatDuration(duration));
        if (errorMessage != null) {
            sb.append("\n  Error: ").append(errorMessage);
        }
        if (!warnings.isEmpty()) {
            sb.append("\n  Warnings:");
            for (String warning : warnings) {
                sb.append("\n    - ").append(warning);
            }
        }
        return sb.toString();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private static String formatDuration(Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return (millis / 1000) + "s";
        } else {
            return (millis / 60000) + "m " + ((millis % 60000) / 1000) + "s";
        }
    }

    @Override
    public String toString() {
        return "ExportResult{" +
                "id='" + exportId + '\'' +
                ", status=" + status +
                ", records=" + recordCount +
                ", size=" + getFormattedSize() +
                '}';
    }

    /**
     * Creates a successful export result.
     *
     * @param format      the export format
     * @param outputPath  the output path
     * @param recordCount the record count
     * @param fileSize    the file size
     * @param duration    the duration
     * @return a success result
     * @since 1.0.0
     */
    @NotNull
    public static ExportResult success(@NotNull ExportBuilder.ExportFormat format,
                                       @NotNull Path outputPath, int recordCount,
                                       long fileSize, @NotNull Duration duration) {
        return new ExportResult(
                java.util.UUID.randomUUID().toString(),
                Status.SUCCESS, format, outputPath, recordCount, fileSize,
                Instant.now(), duration, null, List.of()
        );
    }

    /**
     * Creates a failed export result.
     *
     * @param format the export format
     * @param error  the error message
     * @return a failed result
     * @since 1.0.0
     */
    @NotNull
    public static ExportResult failed(@NotNull ExportBuilder.ExportFormat format,
                                      @NotNull String error) {
        return new ExportResult(
                java.util.UUID.randomUUID().toString(),
                Status.FAILED, format, null, 0, 0,
                Instant.now(), Duration.ZERO, error, List.of()
        );
    }

    /**
     * Status of an export operation.
     *
     * @since 1.0.0
     */
    public enum Status {
        /** Export completed successfully */
        SUCCESS("Successful"),
        /** Export failed */
        FAILED("Failed"),
        /** Export was cancelled */
        CANCELLED("Cancelled");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }
}
