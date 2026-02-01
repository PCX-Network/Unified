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
import java.util.Objects;

/**
 * Result of a rollback operation.
 *
 * <p>RollbackResult contains information about the outcome of attempting
 * to restore data from a migration backup.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CompletableFuture<RollbackResult> future = migration.rollback(migrationId);
 *
 * future.thenAccept(result -> {
 *     if (result.isSuccess()) {
 *         log.info("Rollback successful: {} records restored", result.recordsRestored());
 *     } else {
 *         log.error("Rollback failed: {}", result.errorMessage());
 *     }
 * });
 * }</pre>
 *
 * @param migrationId      the original migration ID
 * @param status           the rollback status
 * @param recordsRestored  number of records restored
 * @param timestamp        when the rollback was performed
 * @param duration         how long the rollback took
 * @param errorMessage     error message if failed
 * @param warnings         any warnings during rollback
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationService
 * @see MigrationRecord
 */
public record RollbackResult(
        @NotNull String migrationId,
        @NotNull Status status,
        int recordsRestored,
        @NotNull Instant timestamp,
        @NotNull Duration duration,
        @Nullable String errorMessage,
        @NotNull List<String> warnings
) {

    /**
     * Compact constructor with validation.
     */
    public RollbackResult {
        Objects.requireNonNull(migrationId, "migrationId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    /**
     * Checks if the rollback was successful.
     *
     * @return true if successful
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS || status == Status.PARTIAL_SUCCESS;
    }

    /**
     * Checks if the rollback completely succeeded.
     *
     * @return true if fully successful
     * @since 1.0.0
     */
    public boolean isFullSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the rollback failed.
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
     * Returns a summary of the rollback.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rollback ").append(status.getDisplayName());
        sb.append(" for migration ").append(migrationId);
        sb.append("\n  Records restored: ").append(recordsRestored);
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

    private String formatDuration(Duration duration) {
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
        return "RollbackResult{" +
                "migrationId='" + migrationId + '\'' +
                ", status=" + status +
                ", restored=" + recordsRestored +
                '}';
    }

    /**
     * Creates a successful rollback result.
     *
     * @param migrationId     the migration ID
     * @param recordsRestored the records restored
     * @param duration        the duration
     * @return a success result
     * @since 1.0.0
     */
    @NotNull
    public static RollbackResult success(@NotNull String migrationId, int recordsRestored,
                                         @NotNull Duration duration) {
        return new RollbackResult(migrationId, Status.SUCCESS, recordsRestored,
                Instant.now(), duration, null, List.of());
    }

    /**
     * Creates a failed rollback result.
     *
     * @param migrationId the migration ID
     * @param error       the error message
     * @return a failed result
     * @since 1.0.0
     */
    @NotNull
    public static RollbackResult failed(@NotNull String migrationId, @NotNull String error) {
        return new RollbackResult(migrationId, Status.FAILED, 0,
                Instant.now(), Duration.ZERO, error, List.of());
    }

    /**
     * Creates a "not found" rollback result.
     *
     * @param migrationId the migration ID
     * @return a not found result
     * @since 1.0.0
     */
    @NotNull
    public static RollbackResult notFound(@NotNull String migrationId) {
        return new RollbackResult(migrationId, Status.NOT_FOUND, 0,
                Instant.now(), Duration.ZERO, "Migration not found or backup not available", List.of());
    }

    /**
     * Status of a rollback operation.
     *
     * @since 1.0.0
     */
    public enum Status {
        /** Rollback completed successfully */
        SUCCESS("Successful"),
        /** Rollback completed with some issues */
        PARTIAL_SUCCESS("Partially Successful"),
        /** Rollback failed */
        FAILED("Failed"),
        /** Migration or backup not found */
        NOT_FOUND("Not Found"),
        /** Rollback not supported for this migration */
        NOT_SUPPORTED("Not Supported");

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
