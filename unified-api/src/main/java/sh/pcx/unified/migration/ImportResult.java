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
import java.util.Objects;

/**
 * Result of an import operation.
 *
 * <p>ImportResult contains detailed information about the outcome of an import,
 * including counts of records processed, errors encountered, and timing.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ImportResult result = migration.importFrom(importer).execute();
 *
 * if (result.isSuccess()) {
 *     log.info("Imported {} records in {}", result.getImportedCount(), result.getDuration());
 * } else {
 *     log.error("Import failed: {}", result.getError());
 *     for (ImportError error : result.getErrors()) {
 *         log.error("  {} - {}", error.recordId(), error.message());
 *     }
 * }
 *
 * // Check warnings
 * if (result.hasWarnings()) {
 *     for (String warning : result.getWarnings()) {
 *         log.warn(warning);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataImporter
 * @see ImportContext
 */
public final class ImportResult {

    private final Status status;
    private final int totalProcessed;
    private final int importedCount;
    private final int skippedCount;
    private final int failedCount;
    private final int mergedCount;
    private final Instant startTime;
    private final Instant endTime;
    private final boolean dryRun;
    private final List<ImportError> errors;
    private final List<SkippedRecord> skipped;
    private final List<String> warnings;
    private final Map<String, Long> statistics;
    private final String errorMessage;
    private final Throwable exception;

    private ImportResult(Builder builder) {
        this.status = builder.status;
        this.totalProcessed = builder.totalProcessed;
        this.importedCount = builder.importedCount;
        this.skippedCount = builder.skippedCount;
        this.failedCount = builder.failedCount;
        this.mergedCount = builder.mergedCount;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.dryRun = builder.dryRun;
        this.errors = builder.errors != null ? List.copyOf(builder.errors) : List.of();
        this.skipped = builder.skipped != null ? List.copyOf(builder.skipped) : List.of();
        this.warnings = builder.warnings != null ? List.copyOf(builder.warnings) : List.of();
        this.statistics = builder.statistics != null ? Map.copyOf(builder.statistics) : Map.of();
        this.errorMessage = builder.errorMessage;
        this.exception = builder.exception;
    }

    // ========================================================================
    // Status Checks
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
     * Checks if the import was successful.
     *
     * @return true if successful (no failures)
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS || status == Status.PARTIAL_SUCCESS;
    }

    /**
     * Checks if the import completely succeeded without any issues.
     *
     * @return true if fully successful
     * @since 1.0.0
     */
    public boolean isFullSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the import failed.
     *
     * @return true if failed
     * @since 1.0.0
     */
    public boolean isFailed() {
        return status == Status.FAILED;
    }

    /**
     * Checks if the import was cancelled.
     *
     * @return true if cancelled
     * @since 1.0.0
     */
    public boolean isCancelled() {
        return status == Status.CANCELLED;
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
     * Returns the total number of records processed.
     *
     * @return the total count
     * @since 1.0.0
     */
    public int getTotalProcessed() {
        return totalProcessed;
    }

    /**
     * Returns the number of records successfully imported.
     *
     * @return the imported count
     * @since 1.0.0
     */
    public int getImportedCount() {
        return importedCount;
    }

    /**
     * Returns the number of records skipped.
     *
     * @return the skipped count
     * @since 1.0.0
     */
    public int getSkippedCount() {
        return skippedCount;
    }

    /**
     * Returns the number of records that failed to import.
     *
     * @return the failed count
     * @since 1.0.0
     */
    public int getFailedCount() {
        return failedCount;
    }

    /**
     * Returns the number of records that were merged with existing data.
     *
     * @return the merged count
     * @since 1.0.0
     */
    public int getMergedCount() {
        return mergedCount;
    }

    // ========================================================================
    // Timing
    // ========================================================================

    /**
     * Returns when the import started.
     *
     * @return the start time
     * @since 1.0.0
     */
    @NotNull
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns when the import ended.
     *
     * @return the end time
     * @since 1.0.0
     */
    @NotNull
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Returns the duration of the import.
     *
     * @return the duration
     * @since 1.0.0
     */
    @NotNull
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    /**
     * Returns the average time per record in milliseconds.
     *
     * @return the average time per record
     * @since 1.0.0
     */
    public double getAverageTimePerRecord() {
        if (totalProcessed == 0) {
            return 0;
        }
        return getDuration().toMillis() / (double) totalProcessed;
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
     * Returns the list of import errors.
     *
     * @return the errors
     * @since 1.0.0
     */
    @NotNull
    public List<ImportError> getErrors() {
        return errors;
    }

    /**
     * Returns the general error message if the import failed.
     *
     * @return the error message, or null
     * @since 1.0.0
     */
    @Nullable
    public String getError() {
        return errorMessage;
    }

    /**
     * Returns the exception that caused the failure, if any.
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

    /**
     * Returns the list of skipped records.
     *
     * @return the skipped records
     * @since 1.0.0
     */
    @NotNull
    public List<SkippedRecord> getSkipped() {
        return skipped;
    }

    // ========================================================================
    // Statistics
    // ========================================================================

    /**
     * Returns additional statistics from the import.
     *
     * @return the statistics map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Long> getStatistics() {
        return statistics;
    }

    /**
     * Returns a specific statistic value.
     *
     * @param key the statistic key
     * @return the value, or 0 if not present
     * @since 1.0.0
     */
    public long getStatistic(@NotNull String key) {
        return statistics.getOrDefault(key, 0L);
    }

    // ========================================================================
    // Formatting
    // ========================================================================

    /**
     * Returns a human-readable summary of the result.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Import ").append(status.getDisplayName());
        if (dryRun) {
            sb.append(" (Dry Run)");
        }
        sb.append("\n");
        sb.append("  Total: ").append(totalProcessed).append("\n");
        sb.append("  Imported: ").append(importedCount).append("\n");
        if (mergedCount > 0) {
            sb.append("  Merged: ").append(mergedCount).append("\n");
        }
        if (skippedCount > 0) {
            sb.append("  Skipped: ").append(skippedCount).append("\n");
        }
        if (failedCount > 0) {
            sb.append("  Failed: ").append(failedCount).append("\n");
        }
        sb.append("  Duration: ").append(formatDuration(getDuration()));
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
        return "ImportResult{" +
                "status=" + status +
                ", imported=" + importedCount +
                ", skipped=" + skippedCount +
                ", failed=" + failedCount +
                ", duration=" + getDuration() +
                '}';
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Creates a successful result.
     *
     * @param imported the number of imported records
     * @param skipped  the number of skipped records
     * @return a success result
     * @since 1.0.0
     */
    @NotNull
    public static ImportResult success(int imported, int skipped) {
        return builder()
                .status(skipped > 0 ? Status.PARTIAL_SUCCESS : Status.SUCCESS)
                .importedCount(imported)
                .skippedCount(skipped)
                .totalProcessed(imported + skipped)
                .endTime(Instant.now())
                .build();
    }

    /**
     * Creates a failed result.
     *
     * @param error the error message
     * @return a failed result
     * @since 1.0.0
     */
    @NotNull
    public static ImportResult failed(@NotNull String error) {
        return builder()
                .status(Status.FAILED)
                .errorMessage(error)
                .endTime(Instant.now())
                .build();
    }

    /**
     * Creates a failed result with an exception.
     *
     * @param error     the error message
     * @param exception the exception
     * @return a failed result
     * @since 1.0.0
     */
    @NotNull
    public static ImportResult failed(@NotNull String error, @NotNull Throwable exception) {
        return builder()
                .status(Status.FAILED)
                .errorMessage(error)
                .exception(exception)
                .endTime(Instant.now())
                .build();
    }

    /**
     * Creates a cancelled result.
     *
     * @param processed the number of records processed before cancellation
     * @param imported  the number of records imported before cancellation
     * @return a cancelled result
     * @since 1.0.0
     */
    @NotNull
    public static ImportResult cancelled(int processed, int imported) {
        return builder()
                .status(Status.CANCELLED)
                .totalProcessed(processed)
                .importedCount(imported)
                .endTime(Instant.now())
                .build();
    }

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
     * Status of an import operation.
     *
     * @since 1.0.0
     */
    public enum Status {
        /** Import completed successfully with no issues */
        SUCCESS("Successful"),
        /** Import completed but some records were skipped or had issues */
        PARTIAL_SUCCESS("Partially Successful"),
        /** Import failed completely */
        FAILED("Failed"),
        /** Import was cancelled before completion */
        CANCELLED("Cancelled");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the display name.
         *
         * @return the display name
         * @since 1.0.0
         */
        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Record of an import error.
     *
     * @param recordId  the record identifier
     * @param message   the error message
     * @param exception the exception, if any
     * @since 1.0.0
     */
    public record ImportError(
            @NotNull String recordId,
            @NotNull String message,
            @Nullable Throwable exception
    ) {
        public ImportError {
            Objects.requireNonNull(recordId, "recordId cannot be null");
            Objects.requireNonNull(message, "message cannot be null");
        }
    }

    /**
     * Record of a skipped record.
     *
     * @param recordId the record identifier
     * @param reason   the reason for skipping
     * @since 1.0.0
     */
    public record SkippedRecord(
            @NotNull String recordId,
            @NotNull String reason
    ) {
        public SkippedRecord {
            Objects.requireNonNull(recordId, "recordId cannot be null");
            Objects.requireNonNull(reason, "reason cannot be null");
        }
    }

    /**
     * Builder for ImportResult.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private Status status = Status.SUCCESS;
        private int totalProcessed;
        private int importedCount;
        private int skippedCount;
        private int failedCount;
        private int mergedCount;
        private Instant startTime = Instant.now();
        private Instant endTime;
        private boolean dryRun;
        private List<ImportError> errors;
        private List<SkippedRecord> skipped;
        private List<String> warnings;
        private Map<String, Long> statistics;
        private String errorMessage;
        private Throwable exception;

        private Builder() {}

        @NotNull
        public Builder status(@NotNull Status status) {
            this.status = status;
            return this;
        }

        @NotNull
        public Builder totalProcessed(int totalProcessed) {
            this.totalProcessed = totalProcessed;
            return this;
        }

        @NotNull
        public Builder importedCount(int importedCount) {
            this.importedCount = importedCount;
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
        public Builder mergedCount(int mergedCount) {
            this.mergedCount = mergedCount;
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
        public Builder errors(@NotNull List<ImportError> errors) {
            this.errors = errors;
            return this;
        }

        @NotNull
        public Builder skipped(@NotNull List<SkippedRecord> skipped) {
            this.skipped = skipped;
            return this;
        }

        @NotNull
        public Builder warnings(@NotNull List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        @NotNull
        public Builder statistics(@NotNull Map<String, Long> statistics) {
            this.statistics = statistics;
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
        public ImportResult build() {
            if (endTime == null) {
                endTime = Instant.now();
            }
            return new ImportResult(this);
        }
    }
}
