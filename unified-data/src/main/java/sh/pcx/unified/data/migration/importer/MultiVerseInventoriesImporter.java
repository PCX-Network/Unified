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
 * Importer for MultiVerse-Inventories plugin data.
 *
 * <p>MultiVerse-Inventories provides per-world inventory management for
 * MultiVerse servers. This importer reads the data files and converts
 * them to the unified format.
 *
 * <h2>Supported Data</h2>
 * <ul>
 *   <li>Player inventories per world group</li>
 *   <li>Ender chest contents</li>
 *   <li>Player stats</li>
 *   <li>World group configurations</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Importer(
        name = "MultiVerse-Inventories",
        description = "Import player inventories from MultiVerse-Inventories",
        detectionFile = "plugins/Multiverse-Inventories/players",
        dataTypes = {"inventory", "enderchest", "stats"},
        priority = Importer.ImporterPriority.HIGH
)
public class MultiVerseInventoriesImporter extends AbstractDataImporter {

    private static final String DATA_PATH = "plugins/Multiverse-Inventories/players";

    /**
     * Creates a new MultiVerse-Inventories importer.
     */
    public MultiVerseInventoriesImporter() {
        super(
                "multiverse-inventories",
                "MultiVerse-Inventories",
                "Import player inventories from MultiVerse-Inventories",
                Set.of("inventory", "enderchest", "stats"),
                DATA_PATH
        );
    }

    @Override
    @NotNull
    public DataSchema getSourceSchema() {
        return DataSchema.builder()
                .name("mvi_player_data")
                .description("MultiVerse-Inventories player data schema")
                .field("uuid", DataSchema.FieldType.UUID, true)
                .field("world_group", DataSchema.FieldType.STRING, true)
                .field("inventoryContents", DataSchema.FieldType.OBJECT, false)
                .field("armorContents", DataSchema.FieldType.OBJECT, false)
                .field("enderChestContents", DataSchema.FieldType.OBJECT, false)
                .field("health", DataSchema.FieldType.DOUBLE, false, 20.0)
                .field("exp", DataSchema.FieldType.DOUBLE, false, 0.0)
                .field("totalExperience", DataSchema.FieldType.INTEGER, false, 0)
                .field("level", DataSchema.FieldType.INTEGER, false, 0)
                .field("foodLevel", DataSchema.FieldType.INTEGER, false, 20)
                .field("saturation", DataSchema.FieldType.DOUBLE, false, 5.0)
                .field("exhaustion", DataSchema.FieldType.DOUBLE, false, 0.0)
                .field("bedSpawnLocation", DataSchema.FieldType.OBJECT, false)
                .field("potionEffects", DataSchema.FieldType.ARRAY, false)
                .field("fallDistance", DataSchema.FieldType.DOUBLE, false, 0.0)
                .field("fireTicks", DataSchema.FieldType.INTEGER, false, 0)
                .field("remainingAir", DataSchema.FieldType.INTEGER, false, 300)
                .primaryKey("uuid")
                .build();
    }

    @Override
    @NotNull
    public FieldMapping getDefaultMapping() {
        return FieldMapping.builder()
                .map("uuid", "player_id")
                .map("world_group", "world_group")
                .map("inventoryContents", "main_inventory")
                .map("armorContents", "armor_contents")
                .map("enderChestContents", "ender_chest")
                .map("health", "health")
                .map("exp", "exp")
                .map("totalExperience", "total_experience")
                .map("level", "exp_level")
                .map("foodLevel", "food_level")
                .map("saturation", "saturation")
                .map("exhaustion", "exhaustion")
                .map("bedSpawnLocation", "bed_spawn")
                .map("potionEffects", "active_effects")
                .map("fallDistance", "fall_distance")
                .map("fireTicks", "fire_ticks")
                .map("remainingAir", "remaining_air")
                .passUnmapped(false)
                .build();
    }

    @Override
    @NotNull
    public ImportResult doImport(@NotNull ImportContext context) {
        Path dataPath = context.getSourceFolder();

        try {
            // Get list of player folders
            List<Path> playerFolders;
            try (var stream = Files.list(dataPath)) {
                playerFolders = stream.filter(Files::isDirectory).toList();
            }

            int total = playerFolders.size();
            int current = 0;

            for (Path playerFolder : playerFolders) {
                if (context.isCancelled()) {
                    break;
                }

                current++;
                reportProgress(context, current, total);

                try {
                    UUID uuid = parseUUID(playerFolder.getFileName().toString());
                    if (uuid == null) {
                        context.reportSkipped(playerFolder.toString(), "Invalid UUID folder name");
                        continue;
                    }

                    // Process each world group file for this player
                    try (var files = Files.list(playerFolder)) {
                        var groupFiles = files.filter(Files::isRegularFile).toList();
                        for (Path groupFile : groupFiles) {
                            processGroupFile(context, uuid, groupFile);
                        }
                    }

                } catch (Exception e) {
                    context.reportFailed(playerFolder.toString(), e);
                }
            }

        } catch (Exception e) {
            context.logError("Import failed: " + e.getMessage());
            return ImportResult.failed("Import failed: " + e.getMessage(), e);
        }

        return context.buildResult();
    }

    private void processGroupFile(ImportContext context, UUID uuid, Path groupFile) {
        try {
            String groupName = groupFile.getFileName().toString();
            // Remove extension
            int dotIndex = groupName.lastIndexOf('.');
            if (dotIndex > 0) {
                groupName = groupName.substring(0, dotIndex);
            }

            String content = Files.readString(groupFile);
            Map<String, Object> data = parseJson(content);

            if (data.isEmpty()) {
                return;
            }

            data.put("uuid", uuid);
            data.put("world_group", groupName);

            Map<String, Object> mappedData = context.applyMapping(data);

            // Create a composite key for world-specific data
            String recordId = uuid.toString() + ":" + groupName;
            context.save(recordId, mappedData);

        } catch (Exception e) {
            context.reportFailed(groupFile.toString(), e);
        }
    }

    private Map<String, Object> parseJson(String content) {
        // Placeholder - actual implementation would use Jackson/Gson
        Map<String, Object> result = new HashMap<>();
        return result;
    }

    @Override
    protected long countRecords(Path sourcePath) throws Exception {
        long count = 0;
        try (var stream = Files.list(sourcePath)) {
            var folders = stream.filter(Files::isDirectory).toList();
            for (Path folder : folders) {
                try (var files = Files.list(folder)) {
                    count += files.filter(Files::isRegularFile).count();
                }
            }
        }
        return count;
    }
}
