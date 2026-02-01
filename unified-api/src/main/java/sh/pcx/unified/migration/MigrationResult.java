/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of a storage migration operation.
 *
 * <p>MigrationResult contains detailed information about the outcome of a
 * storage migration, including counts, errors, and timing information.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MigrationResult result = migration.migrateStorage()
 *     .from(StorageType.YAML)
 *     .to(StorageType.MYSQL)
 *     .execute();
 *
 * if (result.isSuccess()) {
 *     log.info("Migration completed: {} records in {}",
 *         result.getMigratedCount(), result.getDuration());
 * } else {
 *     log.error("Migration failed: {}", result.getError());
 *     for (TableResult table : result.getTableResults()) {
 *         if (table.hasErrors()) {
 *             log.error("  Table {}: {} errors", table.tableName(), table.errorCount());
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see StorageMigrationBuilder
 * @see MigrationService
 */
public final class MigrationResult {

    private final String migrationId;
    private final Status status;
    private final StorageType sourceType;
    private final StorageType targetType;
    private final int totalRecords;
    private final int migratedCount;
    private final int skippedCount;
    private final int failedCount;
    private final Instant startTime;
    private final Instant endTime;
    private final boolean dryRun;
    private final List<TableResult> tableResults;
    private final List<String> errors;
    private final List<String> warnings;
    private final String errorMessage;
    private final Throwable exception;
    private final String backupId;

    private MigrationResult(Builder builder) {
        this.migrationId = builder.migrationId;
        this.status = builder.status;
        this.sourceType = builder.sourceType;
        this.targetType = builder.targetType;
        this.totalRecords = builder.totalRecords;
        this.migratedCount = builder.migratedCount;
        this.skippedCount = builder.skippedCount;
        this.failedCount = builder.failedCount;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.dryRun = builder.dryRun;
        this.tableResults = builder.tableResults != null ? List.copyOf(builder.tableResults) : List.of();
        this.errors = builder.errors != null ? List.copyOf(builder.errors) : List.of();
        this.warnings = builder.warnings != null ? List.copyOf(builder.warnings) : List.of();
        this.errorMessage = builder.errorMessage;
        this.exception = builder.exception;
        this.backupId = builder.backupId;
    }

    // ========================================================================
    // Identification
    // ========================================================================

    /**
     * Returns the unique migration identifier.
     *
     * <p>Can be used to reference this migration for rollback.
     *
     * @return the migration ID
     * @since 1.0.0
     */
    @NotNull
    public String getMigrationId() {
        return migrationId;
    }

    /**
     * Returns the source storage type.
     *
     * @return the source type
     * @since 1.0.0
     */
    @NotNull
    public StorageType getSourceType() {
        return sourceType;
    }

    /**
     * Returns the target storage type.
     *
     * @return the target type
     * @since 1.0.0
     */
    @NotNull
    public StorageType getTargetType() {
        return targetType;
    }

    // ========================================================================
    // Status
    // ========================================================================

    /**
     * Returns the result status.
     *
     * @return the status
     * @since 1.0.0
     */
    @NotNull
    public Status getStatus() {
        return status;
    }

    /**
     * Checks if the migration was successful.
     *
     * @return true if successful
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS || status == Status.PARTIAL_SUCCESS;
    }

    /**
     * Checks if the migration completely succeeded.
     *
     * @return true if fully successful
     * @since 1.0.0
     */
    public boolean isFullSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the migration failed.
     *
     * @return true if failed
     * @since 1.0.0
     */
    public boolean isFailed() {
        return status == Status.FAILED;
    }

    /**
     * Checks if this was a dry run.
     *
     * @return true if dry run
     * @since 1.0.0
     */
    public boolean isDryRun() {
        return dryRun;
    }

    // ========================================================================
    // Counts
    // ========================================================================

    /**
     * Returns the total number of records.
     *
     * @return the total count
     * @since 1.0.0
     */
    public int getTotalRecords() {
        return totalRecords;
    }

    /**
     * Returns the number of successfully migrated records.
     *
     * @return the migrated count
     * @since 1.0.0
     */
    public int getMigratedCount() {
        return migratedCount;
    }

    /**
     * Returns the number of skipped records.
     *
     * @return the skipped count
     * @since 1.0.0
     */
    public int getSkippedCount() {
        return skippedCount;
    }

    /**
     * Returns the number of failed records.
     *
     * @return the failed count
     * @since 1.0.0
     */
    public int getFailedCount() {
        return failedCount;
    }

    // ========================================================================
    // Timing
    // ========================================================================

    /**
     * Returns when the migration started.
     *
     * @return the start time
     * @since 1.0.0
     */
    @NotNull
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns when the migration ended.
     *
     * @return the end time
     * @since 1.0.0
     */
    @NotNull
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Returns the duration of the migration.
     *
     * @return the duration
     * @since 1.0.0
     */
    @NotNull
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    // ========================================================================
    // Table Results
    // ========================================================================

    /**
     * Returns results for each table migrated.
     *
     * @return the table results
     * @since 1.0.0
     */
    @NotNull
    public List<TableResult> getTableResults() {
        return tableResults;
    }

    /**
     * Returns the result for a specific table.
     *
     * @param tableName the table name
     * @return the table result, or null if not found
     * @since 1.0.0
     */
    @Nullable
    public TableResult getTableResult(@NotNull String tableName) {
        return tableResults.stream()
                .filter(r -> r.tableName().equals(tableName))
                .findFirst()
                .orElse(null);
    }

    // ========================================================================
    // Errors and Warnings
    // ========================================================================

    /**
     * Checks if there are any errors.
     *
     * @return true if there are errors
     * @since 1.0.0
     */
    public boolean hasErrors() {
        return !errors.isEmpty() || errorMessage != null;
    }

    /**
     * Returns the list of error messages.
     *
     * @return the errors
     * @since 1.0.0
     */
    @NotNull
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns the general error message.
     *
     * @return the error message, or null
     * @since 1.0.0
     */
    @Nullable
    public String getError() {
        return errorMessage;
    }

    /**
     * Returns the exception that caused failure.
     *
     * @return the exception, or null
     * @since 1.0.0
     */
    @Nullable
    public Throwable getException() {
        return exception;
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
     * Returns the list of warnings.
     *
     * @return the warnings
     * @since 1.0.0
     */
    @NotNull
    public List<String> getWarnings() {
        return warnings;
    }

    // ========================================================================
    // Backup
    // ========================================================================

    /**
     * Returns the backup identifier if a backup was created.
     *
     * @return the backup ID, or null if no backup
     * @since 1.0.0
     */
    @Nullable
    public String getBackupId() {
        return backupId;
    }

    /**
     * Checks if a backup was created.
     *
     * @return true if backup exists
     * @since 1.0.0
     */
    public boolean hasBackup() {
        return backupId != null;
    }

    // ========================================================================
    // Formatting
    // ========================================================================

    /**
     * Returns a human-readable summary.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Migration ").append(status.getDisplayName());
        if (dryRun) {
            sb.append(" (Dry Run)");
        }
        sb.append("\n");
        sb.append("  From: ").append(sourceType.getDisplayName()).append("\n");
        sb.append("  To: ").append(targetType.getDisplayName()).append("\n");
        sb.append("  Total: ").append(totalRecords).append("\n");
        sb.append("  Migrated: ").append(migratedCount).append("\n");
        if (skippedCount > 0) {
            sb.append("  Skipped: ").append(skippedCount).append("\n");
        }
        if (failedCount > 0) {
            sb.append("  Failed: ").append(failedCount).append("\n");
        }
        sb.append("  Duration: ").append(formatDuration(getDuration()));
        if (backupId != null) {
            sb.append("\n  Backup: ").append(backupId);
        }
        return sb.toString();
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return duration.toMillis() + "ms";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

    @Override
    public String toString() {
        return "MigrationResult{" +
                "id=" + migrationId +
                ", status=" + status +
                ", migrated=" + migratedCount +
                "/" + totalRecords +
                ", duration=" + getDuration() +
                '}';
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Creates a new builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    // ========================================================================
    // Nested Types
    // ========================================================================

    /**
     * Status of a migration operation.
     *
     * @since 1.0.0
     */
    public enum Status {
        /** Migration completed successfully */
        SUCCESS("Successful"),
        /** Migration completed with some failures */
        PARTIAL_SUCCESS("Partially Successful"),
        /** Migration failed */
        FAILED("Failed"),
        /** Migration was cancelled */
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

    /**
     * Result for a single table.
     *
     * @param tableName     the table name
     * @param recordCount   the total records in the table
     * @param migratedCount the migrated count
     * @param errorCount    the error count
     * @param duration      the migration duration for this table
     * @since 1.0.0
     */
    public record TableResult(
            @NotNull String tableName,
            int recordCount,
            int migratedCount,
            int errorCount,
            @NotNull Duration duration
    ) {
        /**
         * Checks if there were any errors for this table.
         *
         * @return true if there are errors
         */
        public boolean hasErrors() {
            return errorCount > 0;
        }

        /**
         * Checks if all records were migrated.
         *
         * @return true if fully migrated
         */
        public boolean isComplete() {
            return migratedCount == recordCount;
        }
    }

    /**
     * Builder for MigrationResult.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String migrationId;
        private Status status = Status.SUCCESS;
        private StorageType sourceType;
        private StorageType targetType;
        private int totalRecords;
        private int migratedCount;
        private int skippedCount;
        private int failedCount;
        private Instant startTime = Instant.now();
        private Instant endTime;
        private boolean dryRun;
        private List<TableResult> tableResults;
        private List<String> errors;
        private List<String> warnings;
        private String errorMessage;
        private Throwable exception;
        private String backupId;

        private Builder() {}

        @NotNull
        public Builder migrationId(@NotNull String migrationId) {
            this.migrationId = migrationId;
            return this;
        }

        @NotNull
        public Builder status(@NotNull Status status) {
            this.status = status;
            return this;
        }

        @NotNull
        public Builder sourceType(@NotNull StorageType sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        @NotNull
        public Builder targetType(@NotNull StorageType targetType) {
            this.targetType = targetType;
            return this;
        }

        @NotNull
        public Builder totalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
            return this;
        }

        @NotNull
        public Builder migratedCount(int migratedCount) {
            this.migratedCount = migratedCount;
            return this;
        }

        @NotNull
        public Builder skippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
            return this;
        }

        @NotNull
        public Builder failedCount(int failedCount) {
            this.failedCount = failedCount;
            return this;
        }

        @NotNull
        public Builder startTime(@NotNull Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        @NotNull
        public Builder endTime(@NotNull Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        @NotNull
        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        @NotNull
        public Builder tableResults(@NotNull List<TableResult> tableResults) {
            this.tableResults = tableResults;
            return this;
        }

        @NotNull
        public Builder errors(@NotNull List<String> errors) {
            this.errors = errors;
            return this;
        }

        @NotNull
        public Builder warnings(@NotNull List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        @NotNull
        public Builder errorMessage(@Nullable String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        @NotNull
        public Builder exception(@Nullable Throwable exception) {
            this.exception = exception;
            return this;
        }

        @NotNull
        public Builder backupId(@Nullable String backupId) {
            this.backupId = backupId;
            return this;
        }

        @NotNull
        public MigrationResult build() {
            if (migrationId == null) {
                migrationId = java.util.UUID.randomUUID().toString();
            }
            if (endTime == null) {
                endTime = Instant.now();
            }
            return new MigrationResult(this);
        }
    }
}
