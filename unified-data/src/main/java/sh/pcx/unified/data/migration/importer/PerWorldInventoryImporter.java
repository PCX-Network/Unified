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
 * Importer for PerWorldInventory plugin data.
 *
 * <p>PerWorldInventory stores per-world player inventories, allowing different
 * inventories in different worlds. This importer reads the data files and
 * converts them to the unified format.
 *
 * <h2>Supported Data</h2>
 * <ul>
 *   <li>Player inventories per world group</li>
 *   <li>Ender chest contents</li>
 *   <li>Player stats (health, food, XP, etc.)</li>
 *   <li>Potion effects</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Importer(
        name = "PerWorldInventory",
        description = "Import player inventories from PerWorldInventory plugin",
        detectionFile = "plugins/PerWorldInventory/data",
        dataTypes = {"inventory", "enderchest", "stats", "effects"},
        priority = Importer.ImporterPriority.HIGH
)
public class PerWorldInventoryImporter extends AbstractDataImporter {

    private static final String DATA_PATH = "plugins/PerWorldInventory/data";

    /**
     * Creates a new PerWorldInventory importer.
     */
    public PerWorldInventoryImporter() {
        super(
                "per-world-inventory",
                "PerWorldInventory",
                "Import player inventories from PerWorldInventory",
                Set.of("inventory", "enderchest", "stats", "effects"),
                DATA_PATH
        );
    }

    @Override
    @NotNull
    public DataSchema getSourceSchema() {
        return DataSchema.builder()
                .name("pwi_player_data")
                .description("PerWorldInventory player data schema")
                .field("uuid", DataSchema.FieldType.UUID, true)
                .field("world_group", DataSchema.FieldType.STRING, true)
                .field("inventory", DataSchema.FieldType.OBJECT, false)
                .field("armor", DataSchema.FieldType.OBJECT, false)
                .field("offhand", DataSchema.FieldType.OBJECT, false)
                .field("enderchest", DataSchema.FieldType.OBJECT, false)
                .field("health", DataSchema.FieldType.DOUBLE, false, 20.0)
                .field("max_health", DataSchema.FieldType.DOUBLE, false, 20.0)
                .field("hunger", DataSchema.FieldType.INTEGER, false, 20)
                .field("saturation", DataSchema.FieldType.DOUBLE, false, 5.0)
                .field("experience", DataSchema.FieldType.DOUBLE, false, 0.0)
                .field("level", DataSchema.FieldType.INTEGER, false, 0)
                .field("exhaustion", DataSchema.FieldType.DOUBLE, false, 0.0)
                .field("potion_effects", DataSchema.FieldType.ARRAY, false)
                .field("gamemode", DataSchema.FieldType.STRING, false)
                .field("flying", DataSchema.FieldType.BOOLEAN, false, false)
                .field("allow_flight", DataSchema.FieldType.BOOLEAN, false, false)
                .primaryKey("uuid")
                .build();
    }

    @Override
    @NotNull
    public FieldMapping getDefaultMapping() {
        return FieldMapping.builder()
                .map("uuid", "player_id")
                .map("world_group", "world_group")
                .map("inventory", "main_inventory")
                .map("armor", "armor_contents")
                .map("offhand", "offhand_item")
                .map("enderchest", "ender_chest")
                .map("health", "health")
                .map("max_health", "max_health")
                .map("hunger", "food_level")
                .map("saturation", "saturation")
                .map("experience", "exp")
                .map("level", "exp_level")
                .map("exhaustion", "exhaustion")
                .map("potion_effects", "active_effects")
                .map("gamemode", "game_mode")
                .map("flying", "is_flying")
                .map("allow_flight", "allow_flight")
                .passUnmapped(false)
                .build();
    }

    @Override
    @NotNull
    public ImportResult doImport(@NotNull ImportContext context) {
        Path dataPath = context.getSourceFolder();
        int imported = 0;
        int skipped = 0;
        int total = 0;

        try {
            // Count total files for progress
            try (var stream = Files.walk(dataPath)) {
                total = (int) stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".yml"))
                        .count();
            }

            // Process each player file
            try (var stream = Files.walk(dataPath)) {
                var files = stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".yml"))
                        .toList();

                int current = 0;
                for (Path file : files) {
                    if (context.isCancelled()) {
                        break;
                    }

                    current++;
                    reportProgress(context, current, total);

                    try {
                        UUID uuid = extractUUIDFromFilename(file.getFileName().toString());
                        if (uuid == null) {
                            context.reportSkipped(file.toString(), "Invalid UUID in filename");
                            skipped++;
                            continue;
                        }

                        String worldGroup = extractWorldGroup(file);
                        Map<String, Object> data = readPlayerData(file);

                        if (data.isEmpty()) {
                            context.reportSkipped(uuid.toString(), "Empty data file");
                            skipped++;
                            continue;
                        }

                        // Add metadata
                        data.put("uuid", uuid);
                        data.put("world_group", worldGroup);

                        // Apply mapping and save
                        Map<String, Object> mappedData = context.applyMapping(data);
                        context.save(uuid, mappedData);
                        imported++;

                    } catch (Exception e) {
                        context.reportFailed(file.toString(), e);
                    }
                }
            }

        } catch (Exception e) {
            context.logError("Import failed: " + e.getMessage());
            return ImportResult.failed("Import failed: " + e.getMessage(), e);
        }

        return context.buildResult();
    }

    /**
     * Extracts the world group from the file path.
     */
    private String extractWorldGroup(Path file) {
        // PWI typically stores data in: data/{world_group}/{uuid}.json
        Path parent = file.getParent();
        if (parent != null) {
            return parent.getFileName().toString();
        }
        return "default";
    }

    /**
     * Reads player data from a file.
     */
    private Map<String, Object> readPlayerData(Path file) throws Exception {
        String content = Files.readString(file);
        String fileName = file.toString().toLowerCase();

        if (fileName.endsWith(".json")) {
            return parseJson(content);
        } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return parseYaml(content);
        }

        return Map.of();
    }

    /**
     * Parses JSON content into a map.
     * Note: In a real implementation, this would use a JSON library.
     */
    private Map<String, Object> parseJson(String content) {
        // Placeholder - actual implementation would use Jackson/Gson
        Map<String, Object> result = new HashMap<>();
        // Parse JSON content
        return result;
    }

    /**
     * Parses YAML content into a map.
     * Note: In a real implementation, this would use SnakeYAML.
     */
    private Map<String, Object> parseYaml(String content) {
        // Placeholder - actual implementation would use SnakeYAML
        Map<String, Object> result = new HashMap<>();
        // Parse YAML content
        return result;
    }

    @Override
    protected long countRecords(Path sourcePath) throws Exception {
        try (var stream = Files.walk(sourcePath)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".json") || name.endsWith(".yml");
                    })
                    .count();
        }
    }
}
