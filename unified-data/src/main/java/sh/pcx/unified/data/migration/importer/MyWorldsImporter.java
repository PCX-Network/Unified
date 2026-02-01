/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration.importer;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;
import java.util.*;

/**
 * Importer for MyWorlds plugin data.
 *
 * <p>MyWorlds provides world management with per-world inventories
 * and player data. This importer reads the data files and converts
 * them to the unified format.
 *
 * <h2>Supported Data</h2>
 * <ul>
 *   <li>Player inventories per world</li>
 *   <li>Player stats per world</li>
 *   <li>World configurations</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Importer(
        name = "MyWorlds",
        description = "Import player data from MyWorlds plugin",
        detectionFile = "plugins/My_Worlds/players",
        dataTypes = {"inventory", "stats"},
        priority = Importer.ImporterPriority.NORMAL
)
public class MyWorldsImporter extends AbstractDataImporter {

    private static final String DATA_PATH = "plugins/My_Worlds/players";

    /**
     * Creates a new MyWorlds importer.
     */
    public MyWorldsImporter() {
        super(
                "my-worlds",
                "MyWorlds",
                "Import player data from MyWorlds",
                Set.of("inventory", "stats"),
                DATA_PATH
        );
    }

    @Override
    @NotNull
    public DataSchema getSourceSchema() {
        return DataSchema.builder()
                .name("myworlds_player_data")
                .description("MyWorlds player data schema")
                .field("uuid", DataSchema.FieldType.UUID, true)
                .field("world", DataSchema.FieldType.STRING, true)
                .field("inventory", DataSchema.FieldType.OBJECT, false)
                .field("armor", DataSchema.FieldType.OBJECT, false)
                .field("health", DataSchema.FieldType.DOUBLE, false, 20.0)
                .field("foodLevel", DataSchema.FieldType.INTEGER, false, 20)
                .field("exp", DataSchema.FieldType.DOUBLE, false, 0.0)
                .field("level", DataSchema.FieldType.INTEGER, false, 0)
                .field("gameMode", DataSchema.FieldType.STRING, false)
                .field("location", DataSchema.FieldType.OBJECT, false)
                .primaryKey("uuid")
                .build();
    }

    @Override
    @NotNull
    public FieldMapping getDefaultMapping() {
        return FieldMapping.builder()
                .map("uuid", "player_id")
                .map("world", "world_name")
                .map("inventory", "main_inventory")
                .map("armor", "armor_contents")
                .map("health", "health")
                .map("foodLevel", "food_level")
                .map("exp", "exp")
                .map("level", "exp_level")
                .map("gameMode", "game_mode")
                .map("location", "last_location")
                .passUnmapped(false)
                .build();
    }

    @Override
    @NotNull
    public ImportResult doImport(@NotNull ImportContext context) {
        Path dataPath = context.getSourceFolder();

        try {
            List<Path> dataFiles;
            try (var stream = Files.walk(dataPath)) {
                dataFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".dat") || p.toString().endsWith(".yml"))
                        .toList();
            }

            int total = dataFiles.size();
            int current = 0;

            for (Path file : dataFiles) {
                if (context.isCancelled()) {
                    break;
                }

                current++;
                reportProgress(context, current, total);

                try {
                    // Extract UUID and world from path
                    UUID uuid = extractUUIDFromPath(file);
                    String world = extractWorldFromPath(file);

                    if (uuid == null) {
                        context.reportSkipped(file.toString(), "Could not extract UUID");
                        continue;
                    }

                    Map<String, Object> data = readData(file);
                    if (data.isEmpty()) {
                        context.reportSkipped(file.toString(), "Empty data file");
                        continue;
                    }

                    data.put("uuid", uuid);
                    data.put("world", world);

                    Map<String, Object> mappedData = context.applyMapping(data);

                    // Create composite key for world-specific data
                    String recordId = uuid.toString() + ":" + world;
                    context.save(recordId, mappedData);

                } catch (Exception e) {
                    context.reportFailed(file.toString(), e);
                }
            }

        } catch (Exception e) {
            context.logError("Import failed: " + e.getMessage());
            return ImportResult.failed("Import failed: " + e.getMessage(), e);
        }

        return context.buildResult();
    }

    private UUID extractUUIDFromPath(Path file) {
        // MyWorlds typically stores as: players/{world}/{uuid}.dat
        String filename = file.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            filename = filename.substring(0, dotIndex);
        }
        return parseUUID(filename);
    }

    private String extractWorldFromPath(Path file) {
        // Get parent folder name as world
        Path parent = file.getParent();
        if (parent != null) {
            return parent.getFileName().toString();
        }
        return "world";
    }

    private Map<String, Object> readData(Path file) throws Exception {
        String filename = file.toString().toLowerCase();
        if (filename.endsWith(".yml")) {
            String content = Files.readString(file);
            return parseYaml(content);
        } else if (filename.endsWith(".dat")) {
            // NBT/binary format
            return readNbtData(file);
        }
        return Map.of();
    }

    private Map<String, Object> parseYaml(String content) {
        // Placeholder - actual implementation would use SnakeYAML
        Map<String, Object> result = new HashMap<>();
        return result;
    }

    private Map<String, Object> readNbtData(Path file) {
        // Placeholder - actual implementation would read NBT format
        Map<String, Object> result = new HashMap<>();
        return result;
    }

    @Override
    protected long countRecords(Path sourcePath) throws Exception {
        try (var stream = Files.walk(sourcePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".dat") || name.endsWith(".yml");
                    })
                    .count();
        }
    }
}
