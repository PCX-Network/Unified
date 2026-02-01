/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Default implementation of {@link ExportBuilder}.
 *
 * <p>Provides data export capabilities to various formats.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultExportBuilder implements ExportBuilder {

    private final DefaultMigrationService service;
    private final Path dataFolder;
    private final ExecutorService executor;
    private final Logger logger;

    private ExportFormat format = ExportFormat.JSON;
    private Path destination;
    private Set<String> dataTypes = new HashSet<>();
    private boolean includeMetadata = true;
    private boolean compress = false;
    private ExportFilter filter;
    private boolean prettyPrint = false;
    private Consumer<MigrationProgress> progressCallback;

    /**
     * Creates a new export builder.
     */
    DefaultExportBuilder(@NotNull DefaultMigrationService service,
                         @NotNull Path dataFolder,
                         @NotNull ExecutorService executor,
                         @NotNull Logger logger) {
        this.service = service;
        this.dataFolder = dataFolder;
        this.executor = executor;
        this.logger = logger;
    }

    @Override
    @NotNull
    public ExportBuilder format(@NotNull ExportFormat format) {
        this.format = Objects.requireNonNull(format);
        return this;
    }

    @Override
    @NotNull
    public ExportBuilder destination(@NotNull Path path) {
        this.destination = Objects.requireNonNull(path);
        return this;
    }

    @Override
    @NotNull
    public ExportBuilder dataTypes(@NotNull String... types) {
        this.dataTypes.addAll(Arrays.asList(types));
        return this;
    }

    @Override
    @NotNull
    public ExportBuilder includeMetadata(boolean include) {
        this.includeMetadata = include;
        return this;
    }

    @Override
    @NotNull
    public ExportBuilder compress(boolean compress) {
        this.compress = compress;
        return this;
    }

    @Override
    @NotNull
    public ExportBuilder filter(@NotNull ExportFilter filter) {
        this.filter = Objects.requireNonNull(filter);
        return this;
    }

    @Override
    @NotNull
    public ExportBuilder prettyPrint(boolean pretty) {
        this.prettyPrint = pretty;
        return this;
    }

    @Override
    @NotNull
    public ExportBuilder onProgress(@NotNull Consumer<MigrationProgress> callback) {
        this.progressCallback = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    @NotNull
    public ExportResult execute() {
        Instant startTime = Instant.now();

        try {
            // Validate
            if (destination == null) {
                // Generate default destination
                String filename = "export_" + System.currentTimeMillis() + format.getExtension();
                if (compress) {
                    filename += ".gz";
                }
                destination = dataFolder.resolve("exports").resolve(filename);
            }

            // Ensure parent directory exists
            Files.createDirectories(destination.getParent());

            // Report progress
            reportProgress(MigrationProgress.Phase.PREPARING, "Preparing export...");

            // Gather data to export
            List<Map<String, Object>> records = gatherDataForExport();
            int totalRecords = records.size();

            // Report progress
            reportProgress(MigrationProgress.Phase.PROCESSING, "Exporting " + totalRecords + " records...");

            // Write to file
            long fileSize;
            if (compress) {
                fileSize = writeCompressed(records);
            } else {
                fileSize = writeUncompressed(records);
            }

            // Report completion
            reportProgress(MigrationProgress.Phase.COMPLETED, "Export complete");

            Duration duration = Duration.between(startTime, Instant.now());
            return ExportResult.success(format, destination, totalRecords, fileSize, duration);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Export failed", e);
            return ExportResult.failed(format, e.getMessage());
        }
    }

    @Override
    @NotNull
    public CompletableFuture<ExportResult> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute, executor);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void reportProgress(MigrationProgress.Phase phase, String message) {
        if (progressCallback != null) {
            progressCallback.accept(MigrationProgress.builder()
                    .phase(phase)
                    .message(message)
                    .build());
        }
    }

    private List<Map<String, Object>> gatherDataForExport() {
        List<Map<String, Object>> records = new ArrayList<>();

        // Placeholder - actual implementation would read from data storage
        // This would iterate over stored data and collect records matching
        // the configured data types and filter

        return records;
    }

    private long writeUncompressed(List<Map<String, Object>> records) throws IOException {
        try (OutputStream out = Files.newOutputStream(destination);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

            String content = formatRecords(records);
            writer.write(content);
        }

        return Files.size(destination);
    }

    private long writeCompressed(List<Map<String, Object>> records) throws IOException {
        try (OutputStream out = Files.newOutputStream(destination);
             GZIPOutputStream gzip = new GZIPOutputStream(out);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip))) {

            String content = formatRecords(records);
            writer.write(content);
        }

        return Files.size(destination);
    }

    private String formatRecords(List<Map<String, Object>> records) {
        return switch (format) {
            case JSON -> formatAsJson(records);
            case YAML -> formatAsYaml(records);
            case CSV -> formatAsCsv(records);
            case SQL -> formatAsSql(records);
            case BINARY -> ""; // Binary format handled differently
        };
    }

    private String formatAsJson(List<Map<String, Object>> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            sb.append(prettyPrint ? "  " : "");
            sb.append(mapToJson(record, prettyPrint ? 2 : 0));
            if (i < records.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("]");
        return sb.toString();
    }

    private String mapToJson(Map<String, Object> map, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Object> entry = iter.next();
            if (indent > 0) {
                sb.append("\n").append(" ".repeat(indent));
            }
            sb.append("\"").append(entry.getKey()).append("\": ");
            sb.append(valueToJson(entry.getValue(), indent));
            if (iter.hasNext()) {
                sb.append(",");
            }
        }

        if (indent > 0 && !map.isEmpty()) {
            sb.append("\n").append(" ".repeat(indent - 2));
        }
        sb.append("}");
        return sb.toString();
    }

    private String valueToJson(Object value, int indent) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return mapToJson(map, indent > 0 ? indent + 2 : 0);
        } else if (value instanceof List) {
            return listToJson((List<?>) value, indent);
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    private String listToJson(List<?> list, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < list.size(); i++) {
            if (indent > 0) {
                sb.append("\n").append(" ".repeat(indent + 2));
            }
            sb.append(valueToJson(list.get(i), indent));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }

        if (indent > 0 && !list.isEmpty()) {
            sb.append("\n").append(" ".repeat(indent));
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String formatAsYaml(List<Map<String, Object>> records) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                sb.append("\n---\n");
            }
            sb.append(mapToYaml(records.get(i), 0));
        }

        return sb.toString();
    }

    private String mapToYaml(Map<String, Object> map, int indent) {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(indentStr).append(entry.getKey()).append(": ");

            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append("\n");
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                sb.append(mapToYaml(nested, indent + 1));
            } else if (value instanceof List) {
                sb.append("\n");
                for (Object item : (List<?>) value) {
                    sb.append(indentStr).append("  - ").append(item).append("\n");
                }
            } else {
                sb.append(value).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatAsCsv(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Header
        Set<String> columns = records.get(0).keySet();
        sb.append(String.join(",", columns)).append("\n");

        // Rows
        for (Map<String, Object> record : records) {
            List<String> values = new ArrayList<>();
            for (String column : columns) {
                Object value = record.get(column);
                values.add(escapeCsv(value != null ? value.toString() : ""));
            }
            sb.append(String.join(",", values)).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatAsSql(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String tableName = "exported_data";

        Set<String> columns = records.get(0).keySet();

        for (Map<String, Object> record : records) {
            sb.append("INSERT INTO ").append(tableName).append(" (");
            sb.append(String.join(", ", columns));
            sb.append(") VALUES (");

            List<String> values = new ArrayList<>();
            for (String column : columns) {
                Object value = record.get(column);
                values.add(sqlValue(value));
            }
            sb.append(String.join(", ", values));
            sb.append(");\n");
        }

        return sb.toString();
    }

    private String sqlValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            return "'" + value.toString().replace("'", "''") + "'";
        }
    }
}
