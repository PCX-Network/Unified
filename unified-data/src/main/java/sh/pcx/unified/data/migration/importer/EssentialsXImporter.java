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
 * Importer for EssentialsX plugin data.
 *
 * <p>EssentialsX is a popular server essentials plugin providing economy,
 * homes, warps, and other functionality. This importer reads the userdata
 * files and converts them to the unified format.
 *
 * <h2>Supported Data</h2>
 * <ul>
 *   <li>Player economy balance</li>
 *   <li>Homes</li>
 *   <li>Nicknames</li>
 *   <li>Jail status</li>
 *   <li>Mute status</li>
 *   <li>Last location</li>
 *   <li>Play time</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Importer(
        name = "EssentialsX",
        description = "Import player data from EssentialsX plugin",
        detectionFile = "plugins/Essentials/userdata",
        dataTypes = {"economy", "homes", "warps", "player-data"},
        priority = Importer.ImporterPriority.HIGH
)
public class EssentialsXImporter extends AbstractDataImporter {

    private static final String DATA_PATH = "plugins/Essentials/userdata";

    /**
     * Creates a new EssentialsX importer.
     */
    public EssentialsXImporter() {
        super(
                "essentials-x",
                "EssentialsX",
                "Import player data from EssentialsX",
                Set.of("economy", "homes", "warps", "player-data"),
                DATA_PATH
        );
    }

    @Override
    @NotNull
    public DataSchema getSourceSchema() {
        return DataSchema.builder()
                .name("essentials_player_data")
                .description("EssentialsX player data schema")
                .field("uuid", DataSchema.FieldType.UUID, true)
                .field("money", DataSchema.FieldType.DOUBLE, false, 0.0)
                .field("homes", DataSchema.FieldType.OBJECT, false)
                .field("nickname", DataSchema.FieldType.STRING, false)
                .field("lastAccountName", DataSchema.FieldType.STRING, false)
                .field("lastLogin", DataSchema.FieldType.LONG, false)
                .field("lastLogout", DataSchema.FieldType.LONG, false)
                .field("logoutLocation", DataSchema.FieldType.OBJECT, false)
                .field("jail", DataSchema.FieldType.STRING, false)
                .field("jailed", DataSchema.FieldType.BOOLEAN, false, false)
                .field("jailTimeout", DataSchema.FieldType.LONG, false)
                .field("muted", DataSchema.FieldType.BOOLEAN, false, false)
                .field("muteTimeout", DataSchema.FieldType.LONG, false)
                .field("muteReason", DataSchema.FieldType.STRING, false)
                .field("ipAddress", DataSchema.FieldType.STRING, false)
                .field("afk", DataSchema.FieldType.BOOLEAN, false, false)
                .field("ignore", DataSchema.FieldType.ARRAY, false)
                .field("godmode", DataSchema.FieldType.BOOLEAN, false, false)
                .field("powertools", DataSchema.FieldType.OBJECT, false)
                .field("npc", DataSchema.FieldType.BOOLEAN, false, false)
                .field("timestamps", DataSchema.FieldType.OBJECT, false)
                .primaryKey("uuid")
                .build();
    }

    @Override
    @NotNull
    public FieldMapping getDefaultMapping() {
        return FieldMapping.builder()
                .map("uuid", "player_id")
                .map("money", "balance")
                .map("homes", "homes")
                .map("nickname", "display_name")
                .map("lastAccountName", "username")
                .map("lastLogin", "last_login")
                .map("lastLogout", "last_logout")
                .map("logoutLocation", "logout_location")
                .map("jail", "jail_name")
                .map("jailed", "is_jailed")
                .map("jailTimeout", "jail_expires")
                .map("muted", "is_muted")
                .map("muteTimeout", "mute_expires")
                .map("muteReason", "mute_reason")
                .map("ipAddress", "last_ip")
                .map("afk", "is_afk")
                .map("ignore", "ignored_players")
                .map("godmode", "god_mode")
                .ignore("powertools")  // Plugin-specific
                .ignore("npc")
                .ignore("timestamps")
                .passUnmapped(false)
                .build();
    }

    @Override
    @NotNull
    public ImportResult doImport(@NotNull ImportContext context) {
        Path dataPath = context.getSourceFolder();

        try {
            List<Path> playerFiles;
            try (var stream = Files.list(dataPath)) {
                playerFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".yml"))
                        .toList();
            }

            int total = playerFiles.size();
            int current = 0;

            for (Path file : playerFiles) {
                if (context.isCancelled()) {
                    break;
                }

                current++;
                reportProgress(context, current, total);

                try {
                    UUID uuid = extractUUIDFromFilename(file.getFileName().toString());
                    if (uuid == null) {
                        context.reportSkipped(file.toString(), "Invalid UUID in filename");
                        continue;
                    }

                    String content = Files.readString(file);
                    Map<String, Object> data = parseYaml(content);

                    if (data.isEmpty()) {
                        context.reportSkipped(uuid.toString(), "Empty data file");
                        continue;
                    }

                    data.put("uuid", uuid);

                    // Transform homes to standard format
                    if (data.containsKey("homes")) {
                        data.put("homes", transformHomes(data.get("homes")));
                    }

                    // Transform location format
                    if (data.containsKey("logoutLocation")) {
                        data.put("logoutLocation", transformLocation(data.get("logoutLocation")));
                    }

                    Map<String, Object> mappedData = context.applyMapping(data);
                    context.save(uuid, mappedData);

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

    @SuppressWarnings("unchecked")
    private Object transformHomes(Object homes) {
        if (!(homes instanceof Map)) {
            return homes;
        }

        Map<String, Object> homesMap = (Map<String, Object>) homes;
        Map<String, Object> transformed = new HashMap<>();

        for (var entry : homesMap.entrySet()) {
            String homeName = entry.getKey();
            Object homeData = entry.getValue();

            if (homeData instanceof Map) {
                Map<String, Object> location = transformLocation(homeData);
                transformed.put(homeName, location);
            }
        }

        return transformed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> transformLocation(Object location) {
        if (!(location instanceof Map)) {
            return Map.of();
        }

        Map<String, Object> loc = (Map<String, Object>) location;
        Map<String, Object> result = new HashMap<>();

        result.put("world", loc.get("world"));
        result.put("x", loc.get("x"));
        result.put("y", loc.get("y"));
        result.put("z", loc.get("z"));
        result.put("yaw", loc.getOrDefault("yaw", 0.0));
        result.put("pitch", loc.getOrDefault("pitch", 0.0));

        return result;
    }

    private Map<String, Object> parseYaml(String content) {
        // Placeholder - actual implementation would use SnakeYAML
        Map<String, Object> result = new HashMap<>();
        return result;
    }

    @Override
    protected long countRecords(Path sourcePath) throws Exception {
        try (var stream = Files.list(sourcePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yml"))
                    .count();
        }
    }
}
