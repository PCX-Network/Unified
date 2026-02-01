/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.preset;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for loading inventory presets from various sources.
 *
 * <p>PresetLoader implementations can load presets from configuration files,
 * databases, or other storage backends. Multiple loaders can be registered
 * with the PresetManager to support different sources.
 *
 * <h2>Built-in Loaders</h2>
 * <ul>
 *   <li><b>ConfigPresetLoader</b>: Loads from YAML/HOCON configuration files</li>
 *   <li><b>DatabasePresetLoader</b>: Loads from database tables</li>
 *   <li><b>FilePresetLoader</b>: Loads individual preset files from a directory</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class YamlPresetLoader implements PresetLoader {
 *
 *     private final Path presetsDirectory;
 *
 *     public YamlPresetLoader(Path presetsDirectory) {
 *         this.presetsDirectory = presetsDirectory;
 *     }
 *
 *     @Override
 *     public CompletableFuture<List<InventoryPreset>> loadPresets() {
 *         return CompletableFuture.supplyAsync(() -> {
 *             List<InventoryPreset> presets = new ArrayList<>();
 *
 *             try (Stream<Path> files = Files.list(presetsDirectory)) {
 *                 files.filter(p -> p.toString().endsWith(".yml"))
 *                     .forEach(file -> {
 *                         InventoryPreset preset = parseYaml(file);
 *                         if (preset != null) {
 *                             presets.add(preset);
 *                         }
 *                     });
 *             }
 *
 *             return presets;
 *         });
 *     }
 *
 *     @Override
 *     public CompletableFuture<Void> savePreset(InventoryPreset preset) {
 *         return CompletableFuture.runAsync(() -> {
 *             Path file = presetsDirectory.resolve(preset.getName() + ".yml");
 *             writeYaml(file, preset);
 *         });
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration File Format (YAML)</h2>
 * <pre>{@code
 * # kits.yml
 * presets:
 *   kit_pvp:
 *     display-name: "PvP Kit"
 *     description: "Basic PvP equipment"
 *     permission: "kits.pvp"
 *     cooldown: 5m
 *     category: combat
 *     clear-inventory: true
 *     armor:
 *       helmet: "iron_helmet"
 *       chestplate: "iron_chestplate"
 *       leggings: "iron_leggings"
 *       boots: "iron_boots"
 *     items:
 *       0: "iron_sword"
 *       1: "bow"
 *       8: "golden_apple:3"
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PresetManager
 * @see InventoryPreset
 */
public interface PresetLoader {

    /**
     * Returns the name of this loader.
     *
     * <p>Used for identification and logging.
     *
     * @return the loader name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Loads all presets from the source.
     *
     * @return a future containing the list of loaded presets
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<InventoryPreset>> loadPresets();

    /**
     * Loads a specific preset by name.
     *
     * @param name the preset name
     * @return a future containing the preset, or null if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<@Nullable InventoryPreset> loadPreset(@NotNull String name);

    /**
     * Saves a preset to the source.
     *
     * @param preset the preset to save
     * @return a future that completes when saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> savePreset(@NotNull InventoryPreset preset);

    /**
     * Deletes a preset from the source.
     *
     * @param name the preset name
     * @return a future that completes with true if deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> deletePreset(@NotNull String name);

    /**
     * Checks if a preset exists in the source.
     *
     * @param name the preset name
     * @return a future containing true if exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists(@NotNull String name);

    /**
     * Lists all preset names in the source.
     *
     * @return a future containing the list of preset names
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<String>> listNames();

    /**
     * Reloads presets from the source.
     *
     * <p>This method may clear caches and reload all data.
     *
     * @return a future containing the list of reloaded presets
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<List<InventoryPreset>> reload() {
        return loadPresets();
    }

    /**
     * Checks if this loader supports saving.
     *
     * <p>Some loaders may be read-only.
     *
     * @return true if saving is supported
     * @since 1.0.0
     */
    default boolean supportsSaving() {
        return true;
    }

    /**
     * Checks if this loader supports deleting.
     *
     * @return true if deleting is supported
     * @since 1.0.0
     */
    default boolean supportsDeleting() {
        return true;
    }

    /**
     * Returns the source location for logging/debugging.
     *
     * @return the source description (e.g., file path, database name)
     * @since 1.0.0
     */
    @Nullable
    default String getSourceDescription() {
        return null;
    }

    /**
     * Closes the loader and releases resources.
     *
     * @return a future that completes when closed
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

    // ========== Factory Methods ==========

    /**
     * Creates a loader for YAML files in a directory.
     *
     * @param directory the directory containing preset YAML files
     * @return a new preset loader
     * @since 1.0.0
     */
    @NotNull
    static PresetLoader fromDirectory(@NotNull Path directory) {
        return new FilePresetLoader(directory);
    }

    /**
     * Creates a loader for a single configuration file.
     *
     * @param configFile the configuration file path
     * @return a new preset loader
     * @since 1.0.0
     */
    @NotNull
    static PresetLoader fromConfig(@NotNull Path configFile) {
        return new ConfigPresetLoader(configFile);
    }

    /**
     * Creates a read-only loader with fixed presets.
     *
     * @param presets the presets to provide
     * @return a new preset loader
     * @since 1.0.0
     */
    @NotNull
    static PresetLoader fixed(@NotNull List<InventoryPreset> presets) {
        return new FixedPresetLoader(presets);
    }

    // ========== Nested Implementations ==========

    /**
     * Loader implementation for a directory of preset files.
     */
    class FilePresetLoader implements PresetLoader {

        private final Path directory;

        public FilePresetLoader(@NotNull Path directory) {
            this.directory = directory;
        }

        @Override
        @NotNull
        public String getName() {
            return "FilePresetLoader";
        }

        @Override
        @NotNull
        public CompletableFuture<List<InventoryPreset>> loadPresets() {
            return CompletableFuture.supplyAsync(() -> {
                // Implementation would scan directory and parse files
                return List.of();
            });
        }

        @Override
        @NotNull
        public CompletableFuture<@Nullable InventoryPreset> loadPreset(@NotNull String name) {
            return CompletableFuture.supplyAsync(() -> {
                // Implementation would load specific file
                return null;
            });
        }

        @Override
        @NotNull
        public CompletableFuture<Void> savePreset(@NotNull InventoryPreset preset) {
            return CompletableFuture.runAsync(() -> {
                // Implementation would write file
            });
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> deletePreset(@NotNull String name) {
            return CompletableFuture.supplyAsync(() -> {
                // Implementation would delete file
                return false;
            });
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> exists(@NotNull String name) {
            return CompletableFuture.supplyAsync(() ->
                java.nio.file.Files.exists(directory.resolve(name + ".yml"))
            );
        }

        @Override
        @NotNull
        public CompletableFuture<List<String>> listNames() {
            return CompletableFuture.supplyAsync(() -> {
                // Implementation would list files
                return List.of();
            });
        }

        @Override
        @Nullable
        public String getSourceDescription() {
            return directory.toString();
        }
    }

    /**
     * Loader implementation for a single config file.
     */
    class ConfigPresetLoader implements PresetLoader {

        private final Path configFile;

        public ConfigPresetLoader(@NotNull Path configFile) {
            this.configFile = configFile;
        }

        @Override
        @NotNull
        public String getName() {
            return "ConfigPresetLoader";
        }

        @Override
        @NotNull
        public CompletableFuture<List<InventoryPreset>> loadPresets() {
            return CompletableFuture.supplyAsync(() -> {
                // Implementation would parse config file
                return List.of();
            });
        }

        @Override
        @NotNull
        public CompletableFuture<@Nullable InventoryPreset> loadPreset(@NotNull String name) {
            return loadPresets().thenApply(presets ->
                presets.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null)
            );
        }

        @Override
        @NotNull
        public CompletableFuture<Void> savePreset(@NotNull InventoryPreset preset) {
            return CompletableFuture.runAsync(() -> {
                // Implementation would update config file
            });
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> deletePreset(@NotNull String name) {
            return CompletableFuture.supplyAsync(() -> {
                // Implementation would remove from config
                return false;
            });
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> exists(@NotNull String name) {
            return loadPresets().thenApply(presets ->
                presets.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name))
            );
        }

        @Override
        @NotNull
        public CompletableFuture<List<String>> listNames() {
            return loadPresets().thenApply(presets ->
                presets.stream().map(InventoryPreset::getName).toList()
            );
        }

        @Override
        @Nullable
        public String getSourceDescription() {
            return configFile.toString();
        }
    }

    /**
     * Read-only loader with fixed presets.
     */
    class FixedPresetLoader implements PresetLoader {

        private final List<InventoryPreset> presets;

        public FixedPresetLoader(@NotNull List<InventoryPreset> presets) {
            this.presets = List.copyOf(presets);
        }

        @Override
        @NotNull
        public String getName() {
            return "FixedPresetLoader";
        }

        @Override
        @NotNull
        public CompletableFuture<List<InventoryPreset>> loadPresets() {
            return CompletableFuture.completedFuture(presets);
        }

        @Override
        @NotNull
        public CompletableFuture<@Nullable InventoryPreset> loadPreset(@NotNull String name) {
            return CompletableFuture.completedFuture(
                presets.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null)
            );
        }

        @Override
        @NotNull
        public CompletableFuture<Void> savePreset(@NotNull InventoryPreset preset) {
            return CompletableFuture.failedFuture(
                new UnsupportedOperationException("FixedPresetLoader is read-only")
            );
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> deletePreset(@NotNull String name) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> exists(@NotNull String name) {
            return CompletableFuture.completedFuture(
                presets.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name))
            );
        }

        @Override
        @NotNull
        public CompletableFuture<List<String>> listNames() {
            return CompletableFuture.completedFuture(
                presets.stream().map(InventoryPreset::getName).toList()
            );
        }

        @Override
        public boolean supportsSaving() {
            return false;
        }

        @Override
        public boolean supportsDeleting() {
            return false;
        }
    }
}
