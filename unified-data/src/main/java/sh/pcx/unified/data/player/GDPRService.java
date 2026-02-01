/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for GDPR (General Data Protection Regulation) compliance.
 *
 * <p>The GDPRService provides functionality for data portability and the
 * right to erasure (right to be forgotten) as required by GDPR and similar
 * privacy regulations. It coordinates with all data stores to ensure
 * complete data handling.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private GDPRService gdpr;
 *
 * // Export all player data (data portability)
 * gdpr.exportPlayerData(playerId)
 *     .thenAccept(export -> {
 *         Path file = export.getFilePath();
 *         // Send to player or store for download
 *         logger.info("Exported data to: " + file);
 *     });
 *
 * // Erase all player data (right to be forgotten)
 * gdpr.erasePlayerData(playerId, EraseOptions.builder()
 *         .reason("Player request")
 *         .keepAuditLog(true)
 *         .build())
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             logger.info("Erased " + result.getRecordsDeleted() + " records");
 *         }
 *     });
 *
 * // Anonymize data instead of deleting
 * gdpr.anonymizePlayerData(playerId)
 *     .thenAccept(result -> {
 *         // Player data is now anonymized
 *     });
 * }</pre>
 *
 * <h2>GDPR Rights Supported</h2>
 * <ul>
 *   <li><b>Right of Access:</b> Export player data in readable format</li>
 *   <li><b>Right to Rectification:</b> Via standard data modification APIs</li>
 *   <li><b>Right to Erasure:</b> Delete all player data</li>
 *   <li><b>Right to Portability:</b> Export in machine-readable format (JSON)</li>
 *   <li><b>Right to Restriction:</b> Freeze data processing</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Operations are performed asynchronously.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataExporter
 * @see DataEraser
 */
public interface GDPRService {

    /**
     * Returns the data exporter for creating data exports.
     *
     * @return the data exporter
     * @since 1.0.0
     */
    @NotNull
    DataExporter getExporter();

    /**
     * Returns the data eraser for deleting player data.
     *
     * @return the data eraser
     * @since 1.0.0
     */
    @NotNull
    DataEraser getEraser();

    /**
     * Exports all data for a player.
     *
     * <p>This creates a complete export of all player data in JSON format,
     * suitable for the GDPR right to data portability.
     *
     * @param playerId the player's UUID
     * @return a future containing the export result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ExportResult> exportPlayerData(@NotNull UUID playerId);

    /**
     * Exports all data for a player with custom options.
     *
     * @param playerId the player's UUID
     * @param options  export configuration options
     * @return a future containing the export result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ExportResult> exportPlayerData(@NotNull UUID playerId,
                                                      @NotNull ExportOptions options);

    /**
     * Erases all data for a player.
     *
     * <p>This permanently deletes all player data from all data stores,
     * implementing the GDPR right to erasure (right to be forgotten).
     *
     * <p><b>Warning:</b> This operation cannot be undone.
     *
     * @param playerId the player's UUID
     * @return a future containing the erasure result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<EraseResult> erasePlayerData(@NotNull UUID playerId);

    /**
     * Erases all data for a player with custom options.
     *
     * @param playerId the player's UUID
     * @param options  erasure configuration options
     * @return a future containing the erasure result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<EraseResult> erasePlayerData(@NotNull UUID playerId,
                                                    @NotNull EraseOptions options);

    /**
     * Anonymizes a player's data instead of deleting it.
     *
     * <p>Anonymization replaces identifying information with anonymous values
     * while keeping aggregate data intact. This is useful when complete deletion
     * would affect statistics or other players' data.
     *
     * @param playerId the player's UUID
     * @return a future containing the anonymization result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<AnonymizeResult> anonymizePlayerData(@NotNull UUID playerId);

    /**
     * Restricts processing of a player's data.
     *
     * <p>When restricted, the player's data can still be stored but not
     * processed or modified. This implements the GDPR right to restriction
     * of processing.
     *
     * @param playerId the player's UUID
     * @param reason   the reason for restriction
     * @return a future that completes when restriction is applied
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> restrictProcessing(@NotNull UUID playerId, @NotNull String reason);

    /**
     * Removes processing restrictions from a player's data.
     *
     * @param playerId the player's UUID
     * @return a future that completes when restriction is removed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> removeRestriction(@NotNull UUID playerId);

    /**
     * Checks if a player's data processing is restricted.
     *
     * @param playerId the player's UUID
     * @return true if processing is restricted
     * @since 1.0.0
     */
    boolean isRestricted(@NotNull UUID playerId);

    /**
     * Gets all pending data requests for a player.
     *
     * @param playerId the player's UUID
     * @return set of pending request types
     * @since 1.0.0
     */
    @NotNull
    Set<RequestType> getPendingRequests(@NotNull UUID playerId);

    /**
     * Submits a data request on behalf of a player.
     *
     * @param playerId    the player's UUID
     * @param requestType the type of request
     * @return a future containing the request ID
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<String> submitRequest(@NotNull UUID playerId,
                                             @NotNull RequestType requestType);

    /**
     * Gets the status of a data request.
     *
     * @param requestId the request ID
     * @return the request status if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<RequestStatus>> getRequestStatus(@NotNull String requestId);

    // ==================== Nested Types ====================

    /**
     * Result of a data export operation.
     *
     * @since 1.0.0
     */
    interface ExportResult {

        /**
         * Returns whether the export was successful.
         *
         * @return true if successful
         */
        boolean isSuccess();

        /**
         * Returns the path to the export file.
         *
         * @return the file path
         */
        @NotNull
        Path getFilePath();

        /**
         * Returns the size of the export in bytes.
         *
         * @return file size in bytes
         */
        long getFileSizeBytes();

        /**
         * Returns the export format.
         *
         * @return the format (e.g., "JSON", "CSV")
         */
        @NotNull
        String getFormat();

        /**
         * Returns when the export was created.
         *
         * @return creation timestamp
         */
        @NotNull
        Instant getCreatedAt();

        /**
         * Returns when the export expires and will be deleted.
         *
         * @return expiration timestamp
         */
        @NotNull
        Instant getExpiresAt();

        /**
         * Returns the number of data categories exported.
         *
         * @return category count
         */
        int getCategoriesExported();

        /**
         * Returns any error message if the export failed.
         *
         * @return error message, or empty if successful
         */
        @NotNull
        Optional<String> getErrorMessage();
    }

    /**
     * Options for data export.
     *
     * @since 1.0.0
     */
    interface ExportOptions {

        /**
         * Returns a builder for export options.
         *
         * @return a new builder
         */
        @NotNull
        static Builder builder() {
            return new Builder();
        }

        /**
         * Returns the export format.
         *
         * @return the format
         */
        @NotNull
        ExportFormat getFormat();

        /**
         * Returns whether to include metadata.
         *
         * @return true to include metadata
         */
        boolean includeMetadata();

        /**
         * Returns whether to include session history.
         *
         * @return true to include history
         */
        boolean includeSessionHistory();

        /**
         * Returns whether to include audit logs.
         *
         * @return true to include logs
         */
        boolean includeAuditLogs();

        /**
         * Builder for export options.
         */
        class Builder {
            private ExportFormat format = ExportFormat.JSON;
            private boolean includeMetadata = true;
            private boolean includeSessionHistory = true;
            private boolean includeAuditLogs = false;

            /**
             * Sets the export format.
             *
             * @param format the format
             * @return this builder
             */
            @NotNull
            public Builder format(@NotNull ExportFormat format) {
                this.format = format;
                return this;
            }

            /**
             * Sets whether to include metadata.
             *
             * @param include true to include
             * @return this builder
             */
            @NotNull
            public Builder includeMetadata(boolean include) {
                this.includeMetadata = include;
                return this;
            }

            /**
             * Sets whether to include session history.
             *
             * @param include true to include
             * @return this builder
             */
            @NotNull
            public Builder includeSessionHistory(boolean include) {
                this.includeSessionHistory = include;
                return this;
            }

            /**
             * Sets whether to include audit logs.
             *
             * @param include true to include
             * @return this builder
             */
            @NotNull
            public Builder includeAuditLogs(boolean include) {
                this.includeAuditLogs = include;
                return this;
            }

            /**
             * Builds the options.
             *
             * @return the options
             */
            @NotNull
            public ExportOptions build() {
                return new ExportOptions() {
                    @Override
                    public @NotNull ExportFormat getFormat() {
                        return format;
                    }

                    @Override
                    public boolean includeMetadata() {
                        return includeMetadata;
                    }

                    @Override
                    public boolean includeSessionHistory() {
                        return includeSessionHistory;
                    }

                    @Override
                    public boolean includeAuditLogs() {
                        return includeAuditLogs;
                    }
                };
            }
        }
    }

    /**
     * Result of a data erasure operation.
     *
     * @since 1.0.0
     */
    interface EraseResult {

        /**
         * Returns whether the erasure was successful.
         *
         * @return true if successful
         */
        boolean isSuccess();

        /**
         * Returns the number of records deleted.
         *
         * @return record count
         */
        long getRecordsDeleted();

        /**
         * Returns the number of data stores affected.
         *
         * @return store count
         */
        int getDataStoresAffected();

        /**
         * Returns when the erasure was completed.
         *
         * @return completion timestamp
         */
        @NotNull
        Instant getCompletedAt();

        /**
         * Returns any error message if the erasure failed.
         *
         * @return error message, or empty if successful
         */
        @NotNull
        Optional<String> getErrorMessage();

        /**
         * Returns any records that could not be deleted.
         *
         * @return set of failed record identifiers
         */
        @NotNull
        Set<String> getFailedRecords();
    }

    /**
     * Options for data erasure.
     *
     * @since 1.0.0
     */
    interface EraseOptions {

        /**
         * Returns a builder for erase options.
         *
         * @return a new builder
         */
        @NotNull
        static Builder builder() {
            return new Builder();
        }

        /**
         * Returns the reason for erasure.
         *
         * @return the reason
         */
        @NotNull
        String getReason();

        /**
         * Returns whether to keep audit logs.
         *
         * @return true to preserve audit logs
         */
        boolean keepAuditLog();

        /**
         * Returns whether to anonymize instead of delete when required.
         *
         * @return true to anonymize non-deletable records
         */
        boolean anonymizeWhenRequired();

        /**
         * Builder for erase options.
         */
        class Builder {
            private String reason = "User request";
            private boolean keepAuditLog = true;
            private boolean anonymizeWhenRequired = true;

            /**
             * Sets the reason for erasure.
             *
             * @param reason the reason
             * @return this builder
             */
            @NotNull
            public Builder reason(@NotNull String reason) {
                this.reason = reason;
                return this;
            }

            /**
             * Sets whether to keep audit logs.
             *
             * @param keep true to preserve logs
             * @return this builder
             */
            @NotNull
            public Builder keepAuditLog(boolean keep) {
                this.keepAuditLog = keep;
                return this;
            }

            /**
             * Sets whether to anonymize when deletion is not possible.
             *
             * @param anonymize true to anonymize
             * @return this builder
             */
            @NotNull
            public Builder anonymizeWhenRequired(boolean anonymize) {
                this.anonymizeWhenRequired = anonymize;
                return this;
            }

            /**
             * Builds the options.
             *
             * @return the options
             */
            @NotNull
            public EraseOptions build() {
                return new EraseOptions() {
                    @Override
                    public @NotNull String getReason() {
                        return reason;
                    }

                    @Override
                    public boolean keepAuditLog() {
                        return keepAuditLog;
                    }

                    @Override
                    public boolean anonymizeWhenRequired() {
                        return anonymizeWhenRequired;
                    }
                };
            }
        }
    }

    /**
     * Result of a data anonymization operation.
     *
     * @since 1.0.0
     */
    interface AnonymizeResult {

        /**
         * Returns whether anonymization was successful.
         *
         * @return true if successful
         */
        boolean isSuccess();

        /**
         * Returns the number of fields anonymized.
         *
         * @return field count
         */
        long getFieldsAnonymized();

        /**
         * Returns when anonymization was completed.
         *
         * @return completion timestamp
         */
        @NotNull
        Instant getCompletedAt();

        /**
         * Returns any error message if anonymization failed.
         *
         * @return error message, or empty if successful
         */
        @NotNull
        Optional<String> getErrorMessage();
    }

    /**
     * Types of GDPR data requests.
     *
     * @since 1.0.0
     */
    enum RequestType {
        /**
         * Request for data export.
         */
        EXPORT,

        /**
         * Request for data deletion.
         */
        ERASURE,

        /**
         * Request for data rectification.
         */
        RECTIFICATION,

        /**
         * Request for processing restriction.
         */
        RESTRICTION,

        /**
         * Request for data portability.
         */
        PORTABILITY
    }

    /**
     * Status of a data request.
     *
     * @since 1.0.0
     */
    interface RequestStatus {

        /**
         * Returns the request ID.
         *
         * @return the request ID
         */
        @NotNull
        String getRequestId();

        /**
         * Returns the request type.
         *
         * @return the type
         */
        @NotNull
        RequestType getType();

        /**
         * Returns the current state.
         *
         * @return the state
         */
        @NotNull
        RequestState getState();

        /**
         * Returns when the request was submitted.
         *
         * @return submission timestamp
         */
        @NotNull
        Instant getSubmittedAt();

        /**
         * Returns when the request was completed, if applicable.
         *
         * @return completion timestamp
         */
        @NotNull
        Optional<Instant> getCompletedAt();

        /**
         * Returns progress as a percentage (0-100).
         *
         * @return progress percentage
         */
        int getProgressPercent();
    }

    /**
     * States of a data request.
     *
     * @since 1.0.0
     */
    enum RequestState {
        /**
         * Request received and queued.
         */
        PENDING,

        /**
         * Request is being processed.
         */
        PROCESSING,

        /**
         * Request completed successfully.
         */
        COMPLETED,

        /**
         * Request failed.
         */
        FAILED,

        /**
         * Request was cancelled.
         */
        CANCELLED
    }

    /**
     * Export formats.
     *
     * @since 1.0.0
     */
    enum ExportFormat {
        /**
         * JSON format.
         */
        JSON,

        /**
         * CSV format.
         */
        CSV,

        /**
         * XML format.
         */
        XML
    }
}
