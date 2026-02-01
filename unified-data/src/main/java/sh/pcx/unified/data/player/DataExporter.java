/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Exports player data in various formats for GDPR compliance and data portability.
 *
 * <p>The DataExporter collects all player data from all registered data sources
 * and exports it in human-readable and/or machine-readable formats. This supports
 * the GDPR right to data portability (Article 20).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private DataExporter exporter;
 *
 * // Export to a file
 * exporter.exportToFile(playerId, Path.of("exports/player_data.json"))
 *     .thenAccept(result -> {
 *         logger.info("Export completed: " + result.getFileSizeBytes() + " bytes");
 *     });
 *
 * // Export to an output stream
 * try (OutputStream out = new FileOutputStream("export.json")) {
 *     exporter.exportToStream(playerId, out, ExportFormat.JSON).join();
 * }
 *
 * // Export specific categories only
 * exporter.export(playerId, ExportConfig.builder()
 *         .format(ExportFormat.JSON)
 *         .categories(Set.of("profile", "economy", "statistics"))
 *         .includeMetadata(true)
 *         .build())
 *     .thenAccept(result -> {
 *         // Handle result
 *     });
 *
 * // Register a custom data source for export
 * exporter.registerDataSource("my_plugin", (playerId, collector) -> {
 *     // Add custom data to the export
 *     collector.addField("custom_field", getCustomData(playerId));
 *     collector.addSection("custom_section", Map.of(
 *         "key1", "value1",
 *         "key2", "value2"
 *     ));
 * });
 * }</pre>
 *
 * <h2>Export Categories</h2>
 * <p>Standard export categories include:
 * <ul>
 *   <li><b>profile</b> - Basic player information (name, UUID, join dates)</li>
 *   <li><b>session_history</b> - Login/logout history</li>
 *   <li><b>player_data</b> - All DataKey values</li>
 *   <li><b>economy</b> - Financial data (if applicable)</li>
 *   <li><b>statistics</b> - Game statistics</li>
 *   <li><b>permissions</b> - Permission data</li>
 *   <li><b>audit_log</b> - Data modification history</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Exports are performed asynchronously.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see GDPRService
 */
public interface DataExporter {

    /**
     * Exports all player data to a file.
     *
     * @param playerId the player's UUID
     * @param filePath the output file path
     * @return a future containing the export result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ExportResult> exportToFile(@NotNull UUID playerId, @NotNull Path filePath);

    /**
     * Exports player data to a file with custom configuration.
     *
     * @param playerId the player's UUID
     * @param filePath the output file path
     * @param config   export configuration
     * @return a future containing the export result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ExportResult> exportToFile(@NotNull UUID playerId, @NotNull Path filePath,
                                                  @NotNull ExportConfig config);

    /**
     * Exports player data to an output stream.
     *
     * @param playerId the player's UUID
     * @param output   the output stream
     * @param format   the export format
     * @return a future that completes when export is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> exportToStream(@NotNull UUID playerId, @NotNull OutputStream output,
                                            @NotNull ExportFormat format);

    /**
     * Exports player data with custom configuration.
     *
     * @param playerId the player's UUID
     * @param config   export configuration
     * @return a future containing the export result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ExportResult> export(@NotNull UUID playerId, @NotNull ExportConfig config);

    /**
     * Returns all data for a player as a map.
     *
     * <p>This does not write to a file but returns the data in memory.
     *
     * @param playerId the player's UUID
     * @return a future containing the data map
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Map<String, Object>> getExportData(@NotNull UUID playerId);

    /**
     * Registers a custom data source for exports.
     *
     * <p>Data sources are called during export to collect data from
     * plugins or systems not using the standard DataKey system.
     *
     * @param category the category name for this data
     * @param source   the data source
     * @since 1.0.0
     */
    void registerDataSource(@NotNull String category, @NotNull DataSource source);

    /**
     * Unregisters a data source.
     *
     * @param category the category to unregister
     * @return true if the source was registered
     * @since 1.0.0
     */
    boolean unregisterDataSource(@NotNull String category);

    /**
     * Returns all registered data source categories.
     *
     * @return set of category names
     * @since 1.0.0
     */
    @NotNull
    Set<String> getRegisteredCategories();

    /**
     * Returns the default export directory.
     *
     * @return the default export path
     * @since 1.0.0
     */
    @NotNull
    Path getDefaultExportDirectory();

    /**
     * Sets the default export directory.
     *
     * @param directory the directory path
     * @since 1.0.0
     */
    void setDefaultExportDirectory(@NotNull Path directory);

    // ==================== Nested Types ====================

    /**
     * Result of an export operation.
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
         * Returns the output file path.
         *
         * @return the file path
         */
        @NotNull
        Path getFilePath();

        /**
         * Returns the file size in bytes.
         *
         * @return size in bytes
         */
        long getFileSizeBytes();

        /**
         * Returns the export format used.
         *
         * @return the format
         */
        @NotNull
        ExportFormat getFormat();

        /**
         * Returns when the export was created.
         *
         * @return creation timestamp
         */
        @NotNull
        Instant getCreatedAt();

        /**
         * Returns when the export file expires (for cleanup).
         *
         * @return expiration timestamp
         */
        @NotNull
        Instant getExpiresAt();

        /**
         * Returns the categories included in the export.
         *
         * @return set of category names
         */
        @NotNull
        Set<String> getCategoriesIncluded();

        /**
         * Returns any error message if the export failed.
         *
         * @return error message, or null if successful
         */
        @Nullable
        String getErrorMessage();
    }

    /**
     * Configuration for data exports.
     *
     * @since 1.0.0
     */
    interface ExportConfig {

        /**
         * Returns a builder for export configuration.
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
        static ExportConfig defaults() {
            return builder().build();
        }

        /**
         * Returns the export format.
         *
         * @return the format
         */
        @NotNull
        ExportFormat getFormat();

        /**
         * Returns the categories to include (empty = all).
         *
         * @return set of categories
         */
        @NotNull
        Set<String> getCategories();

        /**
         * Returns whether to include internal metadata.
         *
         * @return true to include metadata
         */
        boolean includeMetadata();

        /**
         * Returns whether to pretty-print the output.
         *
         * @return true to pretty-print
         */
        boolean prettyPrint();

        /**
         * Returns whether to include null values.
         *
         * @return true to include nulls
         */
        boolean includeNulls();

        /**
         * Returns the maximum file size in bytes (0 = unlimited).
         *
         * @return max size
         */
        long getMaxFileSizeBytes();

        /**
         * Builder for export configuration.
         */
        class Builder {
            private ExportFormat format = ExportFormat.JSON;
            private Set<String> categories = Set.of();
            private boolean includeMetadata = true;
            private boolean prettyPrint = true;
            private boolean includeNulls = false;
            private long maxFileSizeBytes = 0;

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
             * Sets the categories to include.
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
             * Sets whether to pretty-print.
             *
             * @param pretty true to pretty-print
             * @return this builder
             */
            @NotNull
            public Builder prettyPrint(boolean pretty) {
                this.prettyPrint = pretty;
                return this;
            }

            /**
             * Sets whether to include null values.
             *
             * @param include true to include nulls
             * @return this builder
             */
            @NotNull
            public Builder includeNulls(boolean include) {
                this.includeNulls = include;
                return this;
            }

            /**
             * Sets the maximum file size.
             *
             * @param maxBytes max size in bytes
             * @return this builder
             */
            @NotNull
            public Builder maxFileSizeBytes(long maxBytes) {
                this.maxFileSizeBytes = maxBytes;
                return this;
            }

            /**
             * Builds the configuration.
             *
             * @return the config
             */
            @NotNull
            public ExportConfig build() {
                return new ExportConfig() {
                    @Override
                    public @NotNull ExportFormat getFormat() {
                        return format;
                    }

                    @Override
                    public @NotNull Set<String> getCategories() {
                        return categories;
                    }

                    @Override
                    public boolean includeMetadata() {
                        return includeMetadata;
                    }

                    @Override
                    public boolean prettyPrint() {
                        return prettyPrint;
                    }

                    @Override
                    public boolean includeNulls() {
                        return includeNulls;
                    }

                    @Override
                    public long getMaxFileSizeBytes() {
                        return maxFileSizeBytes;
                    }
                };
            }
        }
    }

    /**
     * Interface for custom data sources.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface DataSource {

        /**
         * Collects data for export.
         *
         * @param playerId  the player's UUID
         * @param collector the data collector to add data to
         */
        void collect(@NotNull UUID playerId, @NotNull DataCollector collector);
    }

    /**
     * Collector for gathering export data.
     *
     * @since 1.0.0
     */
    interface DataCollector {

        /**
         * Adds a single field.
         *
         * @param key   the field name
         * @param value the field value
         */
        void addField(@NotNull String key, @Nullable Object value);

        /**
         * Adds a section with multiple fields.
         *
         * @param sectionName the section name
         * @param data        the section data
         */
        void addSection(@NotNull String sectionName, @NotNull Map<String, Object> data);

        /**
         * Adds a list of items.
         *
         * @param key   the list name
         * @param items the list items
         */
        void addList(@NotNull String key, @NotNull Iterable<?> items);

        /**
         * Sets a callback for streaming large data.
         *
         * @param key      the data key
         * @param streamer callback that writes data
         */
        void addStreaming(@NotNull String key, @NotNull Consumer<OutputStream> streamer);
    }

    /**
     * Export formats.
     *
     * @since 1.0.0
     */
    enum ExportFormat {
        /**
         * JSON format (default).
         */
        JSON("application/json", ".json"),

        /**
         * CSV format.
         */
        CSV("text/csv", ".csv"),

        /**
         * XML format.
         */
        XML("application/xml", ".xml"),

        /**
         * YAML format.
         */
        YAML("application/yaml", ".yaml");

        private final String mimeType;
        private final String extension;

        ExportFormat(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        /**
         * Returns the MIME type for this format.
         *
         * @return the MIME type
         */
        @NotNull
        public String getMimeType() {
            return mimeType;
        }

        /**
         * Returns the file extension for this format.
         *
         * @return the extension (including dot)
         */
        @NotNull
        public String getExtension() {
            return extension;
        }
    }
}
