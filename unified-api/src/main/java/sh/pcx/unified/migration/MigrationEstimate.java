/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Estimate of a storage migration operation's scope and requirements.
 *
 * <p>MigrationEstimate provides information about the expected scope of a
 * storage migration before execution.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MigrationEstimate estimate = migration.migrateStorage()
 *     .from(StorageType.YAML)
 *     .to(StorageType.MYSQL)
 *     .estimate();
 *
 * System.out.println("Tables: " + estimate.tableCount());
 * System.out.println("Records: " + estimate.totalRecords());
 * System.out.println("Estimated time: " + estimate.estimatedDuration());
 *
 * for (TableEstimate table : estimate.tables()) {
 *     System.out.println("  " + table.name() + ": " + table.recordCount());
 * }
 * }</pre>
 *
 * @param tableCount        number of tables to migrate
 * @param totalRecords      total records across all tables
 * @param dataSizeBytes     estimated total data size
 * @param estimatedDuration estimated time required
 * @param tables            per-table estimates
 * @param warnings          any warnings about the migration
 * @since 1.0.0
 * @author Supatuck
 * @see StorageMigrationBuilder
 */
public record MigrationEstimate(
        int tableCount,
        long totalRecords,
        long dataSizeBytes,
        @Nullable Duration estimatedDuration,
        @NotNull List<TableEstimate> tables,
        @NotNull List<String> warnings
) {

    /**
     * Compact constructor with validation.
     */
    public MigrationEstimate {
        tables = tables != null ? List.copyOf(tables) : List.of();
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    /**
     * Creates a simple estimate.
     *
     * @param tableCount   the table count
     * @param totalRecords the total records
     * @return a new estimate
     * @since 1.0.0
     */
    @NotNull
    public static MigrationEstimate of(int tableCount, long totalRecords) {
        return new MigrationEstimate(tableCount, totalRecords, 0, null, List.of(), List.of());
    }

    /**
     * Creates an unknown estimate.
     *
     * @return an estimate with unknown values
     * @since 1.0.0
     */
    @NotNull
    public static MigrationEstimate unknown() {
        return new MigrationEstimate(0, -1, -1, null, List.of(), List.of());
    }

    /**
     * Checks if the total record count is known.
     *
     * @return true if known
     * @since 1.0.0
     */
    public boolean isCountKnown() {
        return totalRecords >= 0;
    }

    /**
     * Checks if the duration estimate is available.
     *
     * @return true if available
     * @since 1.0.0
     */
    public boolean hasDurationEstimate() {
        return estimatedDuration != null;
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
     * Checks if this is a large migration.
     *
     * @return true if large (> 100,000 records)
     * @since 1.0.0
     */
    public boolean isLargeMigration() {
        return totalRecords > 100_000;
    }

    /**
     * Returns the data size formatted as a human-readable string.
     *
     * @return formatted size string
     * @since 1.0.0
     */
    @NotNull
    public String getFormattedSize() {
        if (dataSizeBytes < 0) {
            return "Unknown";
        }
        return formatBytes(dataSizeBytes);
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
        sb.append("Migration Estimate:\n");
        sb.append("  Tables: ").append(tableCount).append("\n");
        sb.append("  Total records: ");
        if (isCountKnown()) {
            sb.append(String.format("%,d", totalRecords));
        } else {
            sb.append("Unknown");
        }
        sb.append("\n");
        sb.append("  Data size: ").append(getFormattedSize()).append("\n");
        sb.append("  Estimated time: ");
        if (hasDurationEstimate()) {
            sb.append(formatDuration(estimatedDuration));
        } else {
            sb.append("Unknown");
        }
        sb.append("\n");

        if (!tables.isEmpty()) {
            sb.append("\n  Tables:\n");
            for (TableEstimate table : tables) {
                sb.append("    ").append(table.name())
                  .append(": ").append(String.format("%,d", table.recordCount()))
                  .append(" records\n");
            }
        }

        if (!warnings.isEmpty()) {
            sb.append("\n  Warnings:\n");
            for (String warning : warnings) {
                sb.append("    - ").append(warning).append("\n");
            }
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
     * Estimate for a single table.
     *
     * @param name          the table name
     * @param recordCount   the record count
     * @param dataSizeBytes the data size in bytes
     * @since 1.0.0
     */
    public record TableEstimate(
            @NotNull String name,
            long recordCount,
            long dataSizeBytes
    ) {
        public TableEstimate {
            Objects.requireNonNull(name, "name cannot be null");
        }
    }

    /**
     * Builder for MigrationEstimate.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private int tableCount;
        private long totalRecords = -1;
        private long dataSizeBytes = -1;
        private Duration estimatedDuration;
        private List<TableEstimate> tables = List.of();
        private List<String> warnings = List.of();

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
        public Builder tableCount(int tableCount) {
            this.tableCount = tableCount;
            return this;
        }

        @NotNull
        public Builder totalRecords(long totalRecords) {
            this.totalRecords = totalRecords;
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
        public Builder tables(@NotNull List<TableEstimate> tables) {
            this.tables = tables;
            return this;
        }

        @NotNull
        public Builder warnings(@NotNull List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        @NotNull
        public MigrationEstimate build() {
            return new MigrationEstimate(tableCount, totalRecords, dataSizeBytes,
                    estimatedDuration, tables, warnings);
        }
    }
}
