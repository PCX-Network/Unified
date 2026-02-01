/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Record of a migration that has been applied to the database.
 *
 * <p>This class represents an entry in the migration history table,
 * tracking which migrations have been applied, when, and by whom.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get migration history from the runner
 * MigrationRunner runner = new MigrationRunner(connectionProvider, databaseType);
 * List<MigrationHistory> history = runner.getAppliedMigrations();
 *
 * for (MigrationHistory entry : history) {
 *     logger.info("Migration V{}: {} (applied at {})",
 *         entry.getVersion(),
 *         entry.getDescription(),
 *         entry.getAppliedAt()
 *     );
 * }
 *
 * // Check if a specific version is applied
 * boolean isV3Applied = history.stream()
 *     .anyMatch(h -> h.getVersion() == 3 && h.isSuccess());
 * }</pre>
 *
 * <h2>Database Schema</h2>
 * <p>The migration history is stored in a table with this structure:
 * <pre>
 * CREATE TABLE schema_migrations (
 *     version INT PRIMARY KEY,
 *     description VARCHAR(255) NOT NULL,
 *     checksum INT NOT NULL,
 *     applied_at TIMESTAMP NOT NULL,
 *     execution_time_ms BIGINT NOT NULL,
 *     success BOOLEAN NOT NULL,
 *     author VARCHAR(64),
 *     error_message TEXT
 * )
 * </pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Migration
 * @see MigrationRunner
 */
public class MigrationHistory {

    private final int version;
    private final String description;
    private final int checksum;
    private final Instant appliedAt;
    private final long executionTimeMs;
    private final boolean success;
    private final String author;
    private final String errorMessage;

    /**
     * Creates a new migration history entry.
     *
     * @param version         the migration version
     * @param description     the migration description
     * @param checksum        the migration checksum
     * @param appliedAt       when the migration was applied
     * @param executionTimeMs execution time in milliseconds
     * @param success         whether the migration succeeded
     * @param author          the migration author
     * @param errorMessage    error message if failed
     * @since 1.0.0
     */
    public MigrationHistory(
            int version,
            @NotNull String description,
            int checksum,
            @NotNull Instant appliedAt,
            long executionTimeMs,
            boolean success,
            @Nullable String author,
            @Nullable String errorMessage
    ) {
        this.version = version;
        this.description = Objects.requireNonNull(description);
        this.checksum = checksum;
        this.appliedAt = Objects.requireNonNull(appliedAt);
        this.executionTimeMs = executionTimeMs;
        this.success = success;
        this.author = author;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful migration history entry.
     *
     * @param migration       the migration that was applied
     * @param executionTimeMs execution time in milliseconds
     * @return the history entry
     * @since 1.0.0
     */
    @NotNull
    public static MigrationHistory success(@NotNull Migration migration, long executionTimeMs) {
        return new MigrationHistory(
                migration.getVersion(),
                migration.getDescription(),
                migration.getChecksum(),
                Instant.now(),
                executionTimeMs,
                true,
                migration.getAuthor(),
                null
        );
    }

    /**
     * Creates a failed migration history entry.
     *
     * @param migration       the migration that failed
     * @param executionTimeMs execution time in milliseconds
     * @param error           the error that occurred
     * @return the history entry
     * @since 1.0.0
     */
    @NotNull
    public static MigrationHistory failure(@NotNull Migration migration, long executionTimeMs, @NotNull Throwable error) {
        return new MigrationHistory(
                migration.getVersion(),
                migration.getDescription(),
                migration.getChecksum(),
                Instant.now(),
                executionTimeMs,
                false,
                migration.getAuthor(),
                error.getMessage()
        );
    }

    /**
     * Returns the migration version.
     *
     * @return the version number
     * @since 1.0.0
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns the migration description.
     *
     * @return the description
     * @since 1.0.0
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Returns the migration checksum.
     *
     * @return the checksum
     * @since 1.0.0
     */
    public int getChecksum() {
        return checksum;
    }

    /**
     * Returns when the migration was applied.
     *
     * @return the application timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getAppliedAt() {
        return appliedAt;
    }

    /**
     * Returns the execution time in milliseconds.
     *
     * @return the execution time
     * @since 1.0.0
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Returns whether the migration succeeded.
     *
     * @return true if the migration was applied successfully
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the migration author.
     *
     * @return the author, or null if not specified
     * @since 1.0.0
     */
    @Nullable
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the error message if the migration failed.
     *
     * @return the error message, or null if successful
     * @since 1.0.0
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Checks if the checksum matches another migration.
     *
     * <p>This is used to detect if a migration has been modified after
     * it was applied.
     *
     * @param migration the migration to compare
     * @return true if the checksums match
     * @since 1.0.0
     */
    public boolean checksumMatches(@NotNull Migration migration) {
        return this.checksum == migration.getChecksum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationHistory that = (MigrationHistory) o;
        return version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MigrationHistory{");
        sb.append("V").append(version);
        sb.append(": ").append(description);
        sb.append(", applied=").append(appliedAt);
        sb.append(", time=").append(executionTimeMs).append("ms");
        sb.append(", success=").append(success);
        if (!success && errorMessage != null) {
            sb.append(", error='").append(errorMessage).append("'");
        }
        sb.append("}");
        return sb.toString();
    }
}
