/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Erases player data to comply with GDPR right to erasure (right to be forgotten).
 *
 * <p>The DataEraser coordinates with all data stores to completely remove a player's
 * personal data. It supports complete deletion, selective deletion, and anonymization
 * as fallback when complete deletion is not possible.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private DataEraser eraser;
 *
 * // Complete data erasure
 * eraser.eraseAll(playerId)
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             logger.info("Erased " + result.getRecordsDeleted() + " records");
 *         } else {
 *             logger.warn("Erasure failed: " + result.getErrorMessage());
 *         }
 *     });
 *
 * // Erase with options
 * eraser.erase(playerId, EraseConfig.builder()
 *         .categories(Set.of("profile", "statistics", "economy"))
 *         .preserveAuditLog(true)
 *         .anonymizeOnFailure(true)
 *         .reason("GDPR request from user")
 *         .build())
 *     .thenAccept(this::handleResult);
 *
 * // Anonymize instead of delete
 * eraser.anonymize(playerId)
 *     .thenAccept(result -> {
 *         // Personal data replaced with anonymous values
 *     });
 *
 * // Register a custom data source for erasure
 * eraser.registerEraseHandler("my_plugin", (playerId, config) -> {
 *     // Delete custom plugin data
 *     myDatabase.deletePlayerData(playerId);
 *     return EraseHandlerResult.deleted(100); // 100 records deleted
 * });
 * }</pre>
 *
 * <h2>Erasure Process</h2>
 * <ol>
 *   <li>Player is kicked if online (cannot erase active session)</li>
 *   <li>Distributed lock is acquired to prevent concurrent access</li>
 *   <li>All registered erasure handlers are called</li>
 *   <li>Core player data is erased from database</li>
 *   <li>Cache entries are invalidated</li>
 *   <li>Audit log entry is created (if configured)</li>
 *   <li>Lock is released</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Erasure operations are performed asynchronously.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see GDPRService
 */
public interface DataEraser {

    /**
     * Erases all data for a player.
     *
     * <p><b>Warning:</b> This operation is irreversible.
     *
     * @param playerId the player's UUID
     * @return a future containing the erasure result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<EraseResult> eraseAll(@NotNull UUID playerId);

    /**
     * Erases player data with custom configuration.
     *
     * @param playerId the player's UUID
     * @param config   erasure configuration
     * @return a future containing the erasure result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<EraseResult> erase(@NotNull UUID playerId, @NotNull EraseConfig config);

    /**
     * Erases specific data categories for a player.
     *
     * @param playerId   the player's UUID
     * @param categories the categories to erase
     * @return a future containing the erasure result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<EraseResult> eraseCategories(@NotNull UUID playerId,
                                                    @NotNull Set<String> categories);

    /**
     * Erases specific data keys for a player.
     *
     * @param playerId the player's UUID
     * @param keys     the data keys to erase
     * @return a future containing the erasure result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<EraseResult> eraseKeys(@NotNull UUID playerId,
                                              @NotNull Set<DataKey<?>> keys);

    /**
     * Anonymizes player data instead of deleting it.
     *
     * <p>Replaces identifying information with anonymous values while
     * preserving aggregate data structure for statistics.
     *
     * @param playerId the player's UUID
     * @return a future containing the anonymization result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<AnonymizeResult> anonymize(@NotNull UUID playerId);

    /**
     * Anonymizes player data with custom configuration.
     *
     * @param playerId the player's UUID
     * @param config   anonymization configuration
     * @return a future containing the anonymization result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<AnonymizeResult> anonymize(@NotNull UUID playerId,
                                                  @NotNull AnonymizeConfig config);

    /**
     * Registers a handler for erasing data from a custom source.
     *
     * @param category the category name
     * @param handler  the erasure handler
     * @since 1.0.0
     */
    void registerEraseHandler(@NotNull String category, @NotNull EraseHandler handler);

    /**
     * Unregisters an erasure handler.
     *
     * @param category the category to unregister
     * @return true if the handler was registered
     * @since 1.0.0
     */
    boolean unregisterEraseHandler(@NotNull String category);

    /**
     * Returns all registered erasure handler categories.
     *
     * @return set of category names
     * @since 1.0.0
     */
    @NotNull
    Set<String> getRegisteredCategories();

    /**
     * Schedules data erasure for a future time.
     *
     * <p>Useful for implementing retention periods or delayed deletion.
     *
     * @param playerId    the player's UUID
     * @param scheduledAt when to perform the erasure
     * @param config      erasure configuration
     * @return a future containing the schedule ID
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<String> scheduleErasure(@NotNull UUID playerId,
                                               @NotNull Instant scheduledAt,
                                               @NotNull EraseConfig config);

    /**
     * Cancels a scheduled erasure.
     *
     * @param scheduleId the schedule ID returned by scheduleErasure
     * @return true if the erasure was cancelled
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> cancelScheduledErasure(@NotNull String scheduleId);

    /**
     * Gets all scheduled erasures for a player.
     *
     * @param playerId the player's UUID
     * @return map of schedule IDs to scheduled times
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Map<String, Instant>> getScheduledErasures(@NotNull UUID playerId);

    /**
     * Checks if data can be erased for a player.
     *
     * <p>Some data may not be erasable due to legal requirements or
     * technical constraints.
     *
     * @param playerId the player's UUID
     * @return information about what can be erased
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ErasabilityInfo> checkErasability(@NotNull UUID playerId);

    // ==================== Nested Types ====================

    /**
     * Result of an erasure operation.
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
         * Returns whether the operation was partial.
         *
         * @return true if some data could not be erased
         */
        boolean isPartial();

        /**
         * Returns the number of records deleted.
         *
         * @return record count
         */
        long getRecordsDeleted();

        /**
         * Returns the number of records anonymized.
         *
         * @return anonymized count
         */
        long getRecordsAnonymized();

        /**
         * Returns the categories that were erased.
         *
         * @return set of category names
         */
        @NotNull
        Set<String> getCategoriesErased();

        /**
         * Returns categories that failed to erase.
         *
         * @return map of category to error message
         */
        @NotNull
        Map<String, String> getFailedCategories();

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
         * @return error message, or null if successful
         */
        @Nullable
        String getErrorMessage();

        /**
         * Returns the audit log entry ID for this erasure.
         *
         * @return audit log ID
         */
        @NotNull
        Optional<String> getAuditLogId();
    }

    /**
     * Configuration for data erasure.
     *
     * @since 1.0.0
     */
    interface EraseConfig {

        /**
         * Returns a builder for erasure configuration.
         *
         * @return a new builder
         */
        @NotNull
        static Builder builder() {
            return new Builder();
        }

        /**
         * Returns the default configuration.
         *
         * @return default config
         */
        @NotNull
        static EraseConfig defaults() {
            return builder().build();
        }

        /**
         * Returns the categories to erase (empty = all).
         *
         * @return set of categories
         */
        @NotNull
        Set<String> getCategories();

        /**
         * Returns whether to preserve audit logs.
         *
         * @return true to preserve audit logs
         */
        boolean preserveAuditLog();

        /**
         * Returns whether to anonymize on failure instead of failing.
         *
         * @return true to anonymize on failure
         */
        boolean anonymizeOnFailure();

        /**
         * Returns the reason for the erasure.
         *
         * @return the reason
         */
        @NotNull
        String getReason();

        /**
         * Returns the requestor identity (for audit log).
         *
         * @return the requestor ID
         */
        @NotNull
        Optional<String> getRequestor();

        /**
         * Returns whether to kick the player if online.
         *
         * @return true to kick online player
         */
        boolean kickIfOnline();

        /**
         * Builder for erasure configuration.
         */
        class Builder {
            private Set<String> categories = Set.of();
            private boolean preserveAuditLog = true;
            private boolean anonymizeOnFailure = true;
            private String reason = "User request";
            private String requestor = null;
            private boolean kickIfOnline = true;

            /**
             * Sets the categories to erase.
             *
             * @param categories the categories (empty = all)
             * @return this builder
             */
            @NotNull
            public Builder categories(@NotNull Set<String> categories) {
                this.categories = Set.copyOf(categories);
                return this;
            }

            /**
             * Sets whether to preserve audit logs.
             *
             * @param preserve true to preserve
             * @return this builder
             */
            @NotNull
            public Builder preserveAuditLog(boolean preserve) {
                this.preserveAuditLog = preserve;
                return this;
            }

            /**
             * Sets whether to anonymize on failure.
             *
             * @param anonymize true to anonymize on failure
             * @return this builder
             */
            @NotNull
            public Builder anonymizeOnFailure(boolean anonymize) {
                this.anonymizeOnFailure = anonymize;
                return this;
            }

            /**
             * Sets the erasure reason.
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
             * Sets the requestor identity.
             *
             * @param requestor the requestor ID
             * @return this builder
             */
            @NotNull
            public Builder requestor(@Nullable String requestor) {
                this.requestor = requestor;
                return this;
            }

            /**
             * Sets whether to kick online player.
             *
             * @param kick true to kick
             * @return this builder
             */
            @NotNull
            public Builder kickIfOnline(boolean kick) {
                this.kickIfOnline = kick;
                return this;
            }

            /**
             * Builds the configuration.
             *
             * @return the config
             */
            @NotNull
            public EraseConfig build() {
                return new EraseConfig() {
                    @Override
                    public @NotNull Set<String> getCategories() {
                        return categories;
                    }

                    @Override
                    public boolean preserveAuditLog() {
                        return preserveAuditLog;
                    }

                    @Override
                    public boolean anonymizeOnFailure() {
                        return anonymizeOnFailure;
                    }

                    @Override
                    public @NotNull String getReason() {
                        return reason;
                    }

                    @Override
                    public @NotNull Optional<String> getRequestor() {
                        return Optional.ofNullable(requestor);
                    }

                    @Override
                    public boolean kickIfOnline() {
                        return kickIfOnline;
                    }
                };
            }
        }
    }

    /**
     * Result of an anonymization operation.
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
         * Returns the anonymous UUID assigned.
         *
         * @return the anonymous UUID
         */
        @NotNull
        UUID getAnonymousId();

        /**
         * Returns any error message if anonymization failed.
         *
         * @return error message, or null if successful
         */
        @Nullable
        String getErrorMessage();
    }

    /**
     * Configuration for anonymization.
     *
     * @since 1.0.0
     */
    interface AnonymizeConfig {

        /**
         * Returns a builder for anonymize configuration.
         *
         * @return a new builder
         */
        @NotNull
        static Builder builder() {
            return new Builder();
        }

        /**
         * Returns fields to preserve (not anonymize).
         *
         * @return set of field names
         */
        @NotNull
        Set<String> getPreservedFields();

        /**
         * Returns the anonymous UUID to use.
         *
         * @return the UUID, or empty to generate
         */
        @NotNull
        Optional<UUID> getAnonymousId();

        /**
         * Builder for anonymize configuration.
         */
        class Builder {
            private Set<String> preservedFields = Set.of();
            private UUID anonymousId = null;

            /**
             * Sets fields to preserve.
             *
             * @param fields the fields
             * @return this builder
             */
            @NotNull
            public Builder preserveFields(@NotNull Set<String> fields) {
                this.preservedFields = Set.copyOf(fields);
                return this;
            }

            /**
             * Sets the anonymous UUID to use.
             *
             * @param uuid the UUID
             * @return this builder
             */
            @NotNull
            public Builder anonymousId(@Nullable UUID uuid) {
                this.anonymousId = uuid;
                return this;
            }

            /**
             * Builds the configuration.
             *
             * @return the config
             */
            @NotNull
            public AnonymizeConfig build() {
                return new AnonymizeConfig() {
                    @Override
                    public @NotNull Set<String> getPreservedFields() {
                        return preservedFields;
                    }

                    @Override
                    public @NotNull Optional<UUID> getAnonymousId() {
                        return Optional.ofNullable(anonymousId);
                    }
                };
            }
        }
    }

    /**
     * Handler for erasing data from a custom source.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface EraseHandler {

        /**
         * Erases data for a player.
         *
         * @param playerId the player's UUID
         * @param config   erasure configuration
         * @return the handler result
         */
        @NotNull
        EraseHandlerResult erase(@NotNull UUID playerId, @NotNull EraseConfig config);
    }

    /**
     * Result from an erasure handler.
     *
     * @since 1.0.0
     */
    interface EraseHandlerResult {

        /**
         * Creates a successful deletion result.
         *
         * @param recordsDeleted number of records deleted
         * @return the result
         */
        @NotNull
        static EraseHandlerResult deleted(long recordsDeleted) {
            return new EraseHandlerResult() {
                @Override
                public boolean isSuccess() {
                    return true;
                }

                @Override
                public long getRecordsDeleted() {
                    return recordsDeleted;
                }

                @Override
                public long getRecordsAnonymized() {
                    return 0;
                }

                @Override
                public @Nullable String getErrorMessage() {
                    return null;
                }
            };
        }

        /**
         * Creates an anonymization result.
         *
         * @param recordsAnonymized number of records anonymized
         * @return the result
         */
        @NotNull
        static EraseHandlerResult anonymized(long recordsAnonymized) {
            return new EraseHandlerResult() {
                @Override
                public boolean isSuccess() {
                    return true;
                }

                @Override
                public long getRecordsDeleted() {
                    return 0;
                }

                @Override
                public long getRecordsAnonymized() {
                    return recordsAnonymized;
                }

                @Override
                public @Nullable String getErrorMessage() {
                    return null;
                }
            };
        }

        /**
         * Creates a failure result.
         *
         * @param errorMessage the error message
         * @return the result
         */
        @NotNull
        static EraseHandlerResult failed(@NotNull String errorMessage) {
            return new EraseHandlerResult() {
                @Override
                public boolean isSuccess() {
                    return false;
                }

                @Override
                public long getRecordsDeleted() {
                    return 0;
                }

                @Override
                public long getRecordsAnonymized() {
                    return 0;
                }

                @Override
                public @Nullable String getErrorMessage() {
                    return errorMessage;
                }
            };
        }

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
         * Returns the number of records anonymized.
         *
         * @return record count
         */
        long getRecordsAnonymized();

        /**
         * Returns any error message.
         *
         * @return error message, or null if successful
         */
        @Nullable
        String getErrorMessage();
    }

    /**
     * Information about what can be erased for a player.
     *
     * @since 1.0.0
     */
    interface ErasabilityInfo {

        /**
         * Returns whether full erasure is possible.
         *
         * @return true if fully erasable
         */
        boolean isFullyErasable();

        /**
         * Returns categories that can be erased.
         *
         * @return erasable categories
         */
        @NotNull
        Set<String> getErasableCategories();

        /**
         * Returns categories that cannot be erased.
         *
         * @return non-erasable categories with reasons
         */
        @NotNull
        Map<String, String> getNonErasableCategories();

        /**
         * Returns categories that can be anonymized.
         *
         * @return anonymizable categories
         */
        @NotNull
        Set<String> getAnonymizableCategories();

        /**
         * Returns the estimated record count for erasure.
         *
         * @return estimated count
         */
        long getEstimatedRecordCount();
    }
}
