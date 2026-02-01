/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Record of a completed migration operation stored in the migration history.
 *
 * <p>MigrationRecord captures all details about a past migration, enabling
 * tracking, auditing, and potential rollback of migrations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get migration history
 * List<MigrationRecord> history = migration.getMigrationHistory();
 *
 * for (MigrationRecord record : history) {
 *     System.out.println(record.timestamp() + " - " + record.type());
 *     System.out.println("  Status: " + record.status());
 *     System.out.println("  Records: " + record.recordCount());
 *     if (record.canRollback()) {
 *         System.out.println("  Rollback available: " + record.backupId());
 *     }
 * }
 *
 * // Find and rollback a migration
 * Optional<MigrationRecord> record = migration.findMigration(migrationId);
 * if (record.isPresent() && record.get().canRollback()) {
 *     migration.rollback(migrationId);
 * }
 * }</pre>
 *
 * @param migrationId   unique identifier for this migration
 * @param type          the type of migration
 * @param status        the final status
 * @param timestamp     when the migration was executed
 * @param duration      how long the migration took
 * @param sourceInfo    information about the source
 * @param targetInfo    information about the target
 * @param recordCount   total records processed
 * @param successCount  records successfully migrated
 * @param failedCount   records that failed
 * @param backupId      identifier of the backup, if created
 * @param executedBy    who executed the migration
 * @param notes         optional notes about the migration
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationService
 * @see RollbackResult
 */
public record MigrationRecord(
        @NotNull String migrationId,
        @NotNull MigrationType type,
        @NotNull MigrationStatus status,
        @NotNull Instant timestamp,
        @NotNull Duration duration,
        @NotNull String sourceInfo,
        @NotNull String targetInfo,
        int recordCount,
        int successCount,
        int failedCount,
        @Nullable String backupId,
        @Nullable String executedBy,
        @Nullable String notes
) {

    /**
     * Compact constructor with validation.
     */
    public MigrationRecord {
        Objects.requireNonNull(migrationId, "migrationId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");
        Objects.requireNonNull(sourceInfo, "sourceInfo cannot be null");
        Objects.requireNonNull(targetInfo, "targetInfo cannot be null");
    }

    /**
     * Checks if this migration can be rolled back.
     *
     * @return true if rollback is possible
     * @since 1.0.0
     */
    public boolean canRollback() {
        return backupId != null && !backupId.isBlank() &&
               (status == MigrationStatus.SUCCESS || status == MigrationStatus.PARTIAL_SUCCESS);
    }

    /**
     * Checks if the migration was successful.
     *
     * @return true if successful
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return status == MigrationStatus.SUCCESS || status == MigrationStatus.PARTIAL_SUCCESS;
    }

    /**
     * Returns the skip count (records not migrated).
     *
     * @return the skip count
     * @since 1.0.0
     */
    public int getSkippedCount() {
        return recordCount - successCount - failedCount;
    }

    /**
     * Returns a summary of this migration.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        return String.format("[%s] %s: %s -> %s (%d/%d records, %s)",
                timestamp.toString().substring(0, 19),
                type.getDisplayName(),
                sourceInfo,
                targetInfo,
                successCount,
                recordCount,
                status.getDisplayName());
    }

    /**
     * Types of migration operations.
     *
     * @since 1.0.0
     */
    public enum MigrationType {
        /** Import from another plugin */
        PLUGIN_IMPORT("Plugin Import"),
        /** Storage backend migration */
        STORAGE_MIGRATION("Storage Migration"),
        /** Data export */
        DATA_EXPORT("Data Export"),
        /** Rollback operation */
        ROLLBACK("Rollback");

        private final String displayName;

        MigrationType(String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Status of a migration.
     *
     * @since 1.0.0
     */
    public enum MigrationStatus {
        /** Migration completed successfully */
        SUCCESS("Success"),
        /** Migration completed with some failures */
        PARTIAL_SUCCESS("Partial Success"),
        /** Migration failed */
        FAILED("Failed"),
        /** Migration was cancelled */
        CANCELLED("Cancelled"),
        /** Migration was rolled back */
        ROLLED_BACK("Rolled Back");

        private final String displayName;

        MigrationStatus(String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Builder for MigrationRecord.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String migrationId;
        private MigrationType type;
        private MigrationStatus status;
        private Instant timestamp = Instant.now();
        private Duration duration = Duration.ZERO;
        private String sourceInfo = "";
        private String targetInfo = "";
        private int recordCount;
        private int successCount;
        private int failedCount;
        private String backupId;
        private String executedBy;
        private String notes;

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
        public Builder migrationId(@NotNull String migrationId) {
            this.migrationId = migrationId;
            return this;
        }

        @NotNull
        public Builder type(@NotNull MigrationType type) {
            this.type = type;
            return this;
        }

        @NotNull
        public Builder status(@NotNull MigrationStatus status) {
            this.status = status;
            return this;
        }

        @NotNull
        public Builder timestamp(@NotNull Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @NotNull
        public Builder duration(@NotNull Duration duration) {
            this.duration = duration;
            return this;
        }

        @NotNull
        public Builder sourceInfo(@NotNull String sourceInfo) {
            this.sourceInfo = sourceInfo;
            return this;
        }

        @NotNull
        public Builder targetInfo(@NotNull String targetInfo) {
            this.targetInfo = targetInfo;
            return this;
        }

        @NotNull
        public Builder recordCount(int recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        @NotNull
        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        @NotNull
        public Builder failedCount(int failedCount) {
            this.failedCount = failedCount;
            return this;
        }

        @NotNull
        public Builder backupId(@Nullable String backupId) {
            this.backupId = backupId;
            return this;
        }

        @NotNull
        public Builder executedBy(@Nullable String executedBy) {
            this.executedBy = executedBy;
            return this;
        }

        @NotNull
        public Builder notes(@Nullable String notes) {
            this.notes = notes;
            return this;
        }

        @NotNull
        public MigrationRecord build() {
            if (migrationId == null) {
                migrationId = java.util.UUID.randomUUID().toString();
            }
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(status, "status is required");
            return new MigrationRecord(migrationId, type, status, timestamp, duration,
                    sourceInfo, targetInfo, recordCount, successCount, failedCount,
                    backupId, executedBy, notes);
        }
    }
}
