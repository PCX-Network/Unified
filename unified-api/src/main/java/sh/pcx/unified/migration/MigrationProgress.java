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
 * Progress information for ongoing migration operations.
 *
 * <p>MigrationProgress provides real-time information about the state of
 * an import or storage migration, including counts, timing, and status.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * migration.importFrom(importer)
 *     .onProgress(progress -> {
 *         double percent = progress.getPercentComplete();
 *         String status = progress.getCurrentPhase();
 *         int processed = progress.getProcessedCount();
 *         Duration eta = progress.getEstimatedTimeRemaining();
 *
 *         updateProgressBar(percent);
 *         setStatusText(String.format("%s - %d processed, ETA: %s",
 *             status, processed, formatDuration(eta)));
 *     })
 *     .executeAsync();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ImportBuilder
 * @see StorageMigrationBuilder
 */
public final class MigrationProgress {

    private final Phase phase;
    private final String currentPhase;
    private final int totalCount;
    private final int processedCount;
    private final int successCount;
    private final int failedCount;
    private final int skippedCount;
    private final Instant startTime;
    private final String currentItem;
    private final String message;

    private MigrationProgress(Builder builder) {
        this.phase = builder.phase;
        this.currentPhase = builder.currentPhase;
        this.totalCount = builder.totalCount;
        this.processedCount = builder.processedCount;
        this.successCount = builder.successCount;
        this.failedCount = builder.failedCount;
        this.skippedCount = builder.skippedCount;
        this.startTime = builder.startTime;
        this.currentItem = builder.currentItem;
        this.message = builder.message;
    }

    // ========================================================================
    // Phase Information
    // ========================================================================

    /**
     * Returns the current migration phase.
     *
     * @return the phase
     * @since 1.0.0
     */
    @NotNull
    public Phase getPhase() {
        return phase;
    }

    /**
     * Returns a human-readable description of the current phase.
     *
     * @return the phase description
     * @since 1.0.0
     */
    @NotNull
    public String getCurrentPhase() {
        return currentPhase != null ? currentPhase : phase.getDisplayName();
    }

    /**
     * Returns the current status message.
     *
     * @return the message, or null
     * @since 1.0.0
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * Returns the current item being processed.
     *
     * @return the current item identifier, or null
     * @since 1.0.0
     */
    @Nullable
    public String getCurrentItem() {
        return currentItem;
    }

    // ========================================================================
    // Counts
    // ========================================================================

    /**
     * Returns the total number of items to process.
     *
     * @return the total count, or -1 if unknown
     * @since 1.0.0
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Returns the number of items processed so far.
     *
     * @return the processed count
     * @since 1.0.0
     */
    public int getProcessedCount() {
        return processedCount;
    }

    /**
     * Returns the number of successful items.
     *
     * @return the success count
     * @since 1.0.0
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * Returns the number of failed items.
     *
     * @return the failed count
     * @since 1.0.0
     */
    public int getFailedCount() {
        return failedCount;
    }

    /**
     * Returns the number of skipped items.
     *
     * @return the skipped count
     * @since 1.0.0
     */
    public int getSkippedCount() {
        return skippedCount;
    }

    /**
     * Returns the number of remaining items.
     *
     * @return the remaining count, or -1 if unknown
     * @since 1.0.0
     */
    public int getRemainingCount() {
        if (totalCount < 0) {
            return -1;
        }
        return totalCount - processedCount;
    }

    // ========================================================================
    // Progress Calculation
    // ========================================================================

    /**
     * Returns the progress as a fraction (0.0 to 1.0).
     *
     * @return the progress fraction, or 0 if total is unknown
     * @since 1.0.0
     */
    public double getProgress() {
        if (totalCount <= 0) {
            return 0;
        }
        return (double) processedCount / totalCount;
    }

    /**
     * Returns the progress as a percentage (0 to 100).
     *
     * @return the progress percentage
     * @since 1.0.0
     */
    public double getPercentComplete() {
        return getProgress() * 100;
    }

    /**
     * Returns the progress as a formatted percentage string.
     *
     * @return the percentage string (e.g., "75.5%")
     * @since 1.0.0
     */
    @NotNull
    public String getPercentString() {
        return String.format("%.1f%%", getPercentComplete());
    }

    /**
     * Checks if the migration has started.
     *
     * @return true if started
     * @since 1.0.0
     */
    public boolean isStarted() {
        return phase != Phase.PENDING;
    }

    /**
     * Checks if the migration is currently running.
     *
     * @return true if running
     * @since 1.0.0
     */
    public boolean isRunning() {
        return phase == Phase.PROCESSING || phase == Phase.VALIDATING ||
               phase == Phase.PREPARING || phase == Phase.FINALIZING;
    }

    /**
     * Checks if the migration is complete.
     *
     * @return true if complete
     * @since 1.0.0
     */
    public boolean isComplete() {
        return phase == Phase.COMPLETED || phase == Phase.FAILED || phase == Phase.CANCELLED;
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
     * Returns the elapsed time since the migration started.
     *
     * @return the elapsed duration
     * @since 1.0.0
     */
    @NotNull
    public Duration getElapsedTime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Estimates the time remaining based on current progress.
     *
     * @return the estimated time remaining, or null if unknown
     * @since 1.0.0
     */
    @Nullable
    public Duration getEstimatedTimeRemaining() {
        if (totalCount <= 0 || processedCount <= 0) {
            return null;
        }
        double progress = getProgress();
        if (progress <= 0) {
            return null;
        }
        long elapsedMillis = getElapsedTime().toMillis();
        long totalEstimatedMillis = (long) (elapsedMillis / progress);
        long remainingMillis = totalEstimatedMillis - elapsedMillis;
        return Duration.ofMillis(Math.max(0, remainingMillis));
    }

    /**
     * Returns the average time per item in milliseconds.
     *
     * @return the average time per item, or 0 if no items processed
     * @since 1.0.0
     */
    public double getAverageTimePerItem() {
        if (processedCount <= 0) {
            return 0;
        }
        return (double) getElapsedTime().toMillis() / processedCount;
    }

    /**
     * Returns the estimated items per second.
     *
     * @return items per second, or 0 if no items processed
     * @since 1.0.0
     */
    public double getItemsPerSecond() {
        long elapsedSeconds = getElapsedTime().getSeconds();
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (double) processedCount / elapsedSeconds;
    }

    // ========================================================================
    // Formatting
    // ========================================================================

    /**
     * Returns a human-readable summary of the progress.
     *
     * @return the summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCurrentPhase()).append(": ");
        sb.append(processedCount);
        if (totalCount > 0) {
            sb.append("/").append(totalCount);
            sb.append(" (").append(getPercentString()).append(")");
        }
        Duration eta = getEstimatedTimeRemaining();
        if (eta != null) {
            sb.append(" - ETA: ").append(formatDuration(eta));
        }
        return sb.toString();
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

    @Override
    public String toString() {
        return "MigrationProgress{" +
                "phase=" + phase +
                ", processed=" + processedCount +
                "/" + totalCount +
                ", success=" + successCount +
                ", failed=" + failedCount +
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

    /**
     * Creates progress for the pending state.
     *
     * @return pending progress
     * @since 1.0.0
     */
    @NotNull
    public static MigrationProgress pending() {
        return builder().phase(Phase.PENDING).build();
    }

    /**
     * Creates progress for the completed state.
     *
     * @param total     the total count
     * @param success   the success count
     * @param failed    the failed count
     * @param startTime when the migration started
     * @return completed progress
     * @since 1.0.0
     */
    @NotNull
    public static MigrationProgress completed(int total, int success, int failed, Instant startTime) {
        return builder()
                .phase(Phase.COMPLETED)
                .totalCount(total)
                .processedCount(total)
                .successCount(success)
                .failedCount(failed)
                .startTime(startTime)
                .build();
    }

    // ========================================================================
    // Nested Types
    // ========================================================================

    /**
     * Migration phases.
     *
     * @since 1.0.0
     */
    public enum Phase {
        /** Migration has not started */
        PENDING("Pending"),
        /** Validating configuration and source data */
        VALIDATING("Validating"),
        /** Preparing for migration (creating backup, schema, etc.) */
        PREPARING("Preparing"),
        /** Processing records */
        PROCESSING("Processing"),
        /** Finalizing migration (cleanup, verification) */
        FINALIZING("Finalizing"),
        /** Migration completed successfully */
        COMPLETED("Completed"),
        /** Migration failed */
        FAILED("Failed"),
        /** Migration was cancelled */
        CANCELLED("Cancelled");

        private final String displayName;

        Phase(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the display name.
         *
         * @return the display name
         */
        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Builder for MigrationProgress.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private Phase phase = Phase.PENDING;
        private String currentPhase;
        private int totalCount = -1;
        private int processedCount = 0;
        private int successCount = 0;
        private int failedCount = 0;
        private int skippedCount = 0;
        private Instant startTime = Instant.now();
        private String currentItem;
        private String message;

        private Builder() {}

        @NotNull
        public Builder phase(@NotNull Phase phase) {
            this.phase = phase;
            return this;
        }

        @NotNull
        public Builder currentPhase(@Nullable String currentPhase) {
            this.currentPhase = currentPhase;
            return this;
        }

        @NotNull
        public Builder totalCount(int totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        @NotNull
        public Builder processedCount(int processedCount) {
            this.processedCount = processedCount;
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
        public Builder skippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
            return this;
        }

        @NotNull
        public Builder startTime(@NotNull Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        @NotNull
        public Builder currentItem(@Nullable String currentItem) {
            this.currentItem = currentItem;
            return this;
        }

        @NotNull
        public Builder message(@Nullable String message) {
            this.message = message;
            return this;
        }

        @NotNull
        public MigrationProgress build() {
            return new MigrationProgress(this);
        }
    }
}
