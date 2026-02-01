/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Estimate of an import operation's scope and resource requirements.
 *
 * <p>ImportEstimate provides information about the expected size and duration
 * of an import before execution, allowing users to prepare and allocate resources.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ImportEstimate estimate = migration.importFrom(importer).estimate();
 *
 * System.out.println("Records: " + estimate.recordCount());
 * System.out.println("Data size: " + formatBytes(estimate.dataSizeBytes()));
 * System.out.println("Estimated time: " + estimate.estimatedDuration());
 *
 * if (estimate.requiresWarning()) {
 *     System.out.println("Warning: " + estimate.warningMessage());
 * }
 * }</pre>
 *
 * @param recordCount       the estimated number of records
 * @param dataSizeBytes     the estimated data size in bytes
 * @param estimatedDuration the estimated duration
 * @param byTypeCount       record counts by type
 * @param memoryRequired    estimated memory required in bytes
 * @param warningMessage    optional warning message
 * @since 1.0.0
 * @author Supatuck
 * @see ImportBuilder
 * @see DataImporter
 */
public record ImportEstimate(
        long recordCount,
        long dataSizeBytes,
        @Nullable Duration estimatedDuration,
        @NotNull Map<String, Long> byTypeCount,
        long memoryRequired,
        @Nullable String warningMessage
) {

    /**
     * Compact constructor with validation.
     */
    public ImportEstimate {
        byTypeCount = byTypeCount != null ? Map.copyOf(byTypeCount) : Map.of();
    }

    /**
     * Creates a simple estimate.
     *
     * @param recordCount the record count
     * @param dataSize    the data size in bytes
     * @return a new estimate
     * @since 1.0.0
     */
    @NotNull
    public static ImportEstimate of(long recordCount, long dataSize) {
        return new ImportEstimate(recordCount, dataSize, null, Map.of(), 0, null);
    }

    /**
     * Creates an estimate with duration.
     *
     * @param recordCount the record count
     * @param dataSize    the data size in bytes
     * @param duration    the estimated duration
     * @return a new estimate
     * @since 1.0.0
     */
    @NotNull
    public static ImportEstimate of(long recordCount, long dataSize, @NotNull Duration duration) {
        return new ImportEstimate(recordCount, dataSize, duration, Map.of(), 0, null);
    }

    /**
     * Creates an unknown estimate.
     *
     * @return an estimate with unknown values
     * @since 1.0.0
     */
    @NotNull
    public static ImportEstimate unknown() {
        return new ImportEstimate(-1, -1, null, Map.of(), 0, null);
    }

    /**
     * Checks if the record count is known.
     *
     * @return true if count is known
     * @since 1.0.0
     */
    public boolean isCountKnown() {
        return recordCount >= 0;
    }

    /**
     * Checks if the data size is known.
     *
     * @return true if size is known
     * @since 1.0.0
     */
    public boolean isSizeKnown() {
        return dataSizeBytes >= 0;
    }

    /**
     * Checks if the duration estimate is available.
     *
     * @return true if duration is estimated
     * @since 1.0.0
     */
    public boolean hasDurationEstimate() {
        return estimatedDuration != null;
    }

    /**
     * Checks if the estimate requires a warning.
     *
     * @return true if there is a warning
     * @since 1.0.0
     */
    public boolean requiresWarning() {
        return warningMessage != null && !warningMessage.isBlank();
    }

    /**
     * Checks if this is a large import that may require extra resources.
     *
     * @return true if large (> 10,000 records or > 100MB)
     * @since 1.0.0
     */
    public boolean isLargeImport() {
        return recordCount > 10_000 || dataSizeBytes > 100_000_000;
    }

    /**
     * Checks if this is a very large import.
     *
     * @return true if very large (> 100,000 records or > 1GB)
     * @since 1.0.0
     */
    public boolean isVeryLargeImport() {
        return recordCount > 100_000 || dataSizeBytes > 1_000_000_000;
    }

    /**
     * Returns the data size formatted as a human-readable string.
     *
     * @return formatted size string
     * @since 1.0.0
     */
    @NotNull
    public String getFormattedSize() {
        if (!isSizeKnown()) {
            return "Unknown";
        }
        return formatBytes(dataSizeBytes);
    }

    /**
     * Returns the estimated duration formatted as a string.
     *
     * @return formatted duration string
     * @since 1.0.0
     */
    @NotNull
    public String getFormattedDuration() {
        if (!hasDurationEstimate()) {
            return "Unknown";
        }
        return formatDuration(estimatedDuration);
    }

    /**
     * Returns a summary of the estimate.
     *
     * @return summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Import Estimate:\n");

        if (isCountKnown()) {
            sb.append("  Records: ").append(String.format("%,d", recordCount)).append("\n");
        } else {
            sb.append("  Records: Unknown\n");
        }

        sb.append("  Data size: ").append(getFormattedSize()).append("\n");
        sb.append("  Estimated time: ").append(getFormattedDuration()).append("\n");

        if (!byTypeCount.isEmpty()) {
            sb.append("  By type:\n");
            for (var entry : byTypeCount.entrySet()) {
                sb.append("    ").append(entry.getKey()).append(": ")
                  .append(String.format("%,d", entry.getValue())).append("\n");
            }
        }

        if (memoryRequired > 0) {
            sb.append("  Memory required: ").append(formatBytes(memoryRequired)).append("\n");
        }

        if (requiresWarning()) {
            sb.append("  Warning: ").append(warningMessage).append("\n");
        }

        return sb.toString().trim();
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
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        } else {
            return (seconds / 3600) + " hours " + ((seconds % 3600) / 60) + " minutes";
        }
    }

    /**
     * Builder for ImportEstimate.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private long recordCount = -1;
        private long dataSizeBytes = -1;
        private Duration estimatedDuration;
        private Map<String, Long> byTypeCount = Map.of();
        private long memoryRequired;
        private String warningMessage;

        private Builder() {}

        /**
         * Creates a new builder.
         *
         * @return a new builder
         * @since 1.0.0
         */
        @NotNull
        public static Builder create() {
            return new Builder();
        }

        @NotNull
        public Builder recordCount(long recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        @NotNull
        public Builder dataSizeBytes(long dataSizeBytes) {
            this.dataSizeBytes = dataSizeBytes;
            return this;
        }

        @NotNull
        public Builder estimatedDuration(@Nullable Duration estimatedDuration) {
            this.estimatedDuration = estimatedDuration;
            return this;
        }

        @NotNull
        public Builder byTypeCount(@NotNull Map<String, Long> byTypeCount) {
            this.byTypeCount = byTypeCount;
            return this;
        }

        @NotNull
        public Builder memoryRequired(long memoryRequired) {
            this.memoryRequired = memoryRequired;
            return this;
        }

        @NotNull
        public Builder warningMessage(@Nullable String warningMessage) {
            this.warningMessage = warningMessage;
            return this;
        }

        @NotNull
        public ImportEstimate build() {
            return new ImportEstimate(recordCount, dataSizeBytes, estimatedDuration,
                    byTypeCount, memoryRequired, warningMessage);
        }
    }
}
