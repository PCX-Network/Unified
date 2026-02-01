/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.format;

import sh.pcx.unified.config.ConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

/**
 * Saves configuration files in any supported format using Sponge Configurate.
 *
 * <p>ConfigSaver provides a unified interface for saving configuration data
 * regardless of the target format. It supports atomic saves with backup,
 * comment preservation, and async operations.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Multi-format:</b> YAML, HOCON, JSON, TOML</li>
 *   <li><b>Atomic saves:</b> Write to temp file then rename</li>
 *   <li><b>Backup support:</b> Optional backup before overwriting</li>
 *   <li><b>Comment preservation:</b> Maintains comments where supported</li>
 *   <li><b>Async support:</b> Non-blocking file operations</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * ConfigSaver saver = new ConfigSaver();
 *
 * // Save raw node
 * saver.save(node, path);
 *
 * // Save with explicit format
 * saver.save(node, path, ConfigFormat.YAML);
 *
 * // Save configuration object
 * saver.save(config, path);
 *
 * // Save with backup
 * saver.saveWithBackup(node, path);
 * }</pre>
 *
 * <h2>Async Saving</h2>
 * <pre>{@code
 * saver.saveAsync(config, path)
 *     .thenRun(() -> logger.info("Config saved!"))
 *     .exceptionally(error -> {
 *         logger.severe("Failed to save: " + error.getMessage());
 *         return null;
 *     });
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigFormat
 * @see ConfigLoader
 */
public class ConfigSaver {

    private final ConfigurationOptions options;
    private final boolean atomicSave;
    private final boolean createParentDirs;

    /**
     * Creates a new ConfigSaver with default settings.
     */
    public ConfigSaver() {
        this(ConfigurationOptions.defaults(), true, true);
    }

    /**
     * Creates a new ConfigSaver with custom options.
     *
     * @param options the configuration options
     */
    public ConfigSaver(@NotNull ConfigurationOptions options) {
        this(options, true, true);
    }

    /**
     * Creates a new ConfigSaver with full customization.
     *
     * @param options the configuration options
     * @param atomicSave whether to use atomic saves
     * @param createParentDirs whether to create parent directories
     */
    public ConfigSaver(
            @NotNull ConfigurationOptions options,
            boolean atomicSave,
            boolean createParentDirs
    ) {
        this.options = options;
        this.atomicSave = atomicSave;
        this.createParentDirs = createParentDirs;
    }

    /**
     * Gets the configuration options.
     *
     * @return the configuration options
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationOptions getOptions() {
        return options;
    }

    /**
     * Checks if atomic saves are enabled.
     *
     * @return true if atomic saves are enabled
     * @since 1.0.0
     */
    public boolean isAtomicSave() {
        return atomicSave;
    }

    /**
     * Creates a new saver with updated settings.
     *
     * @param options the new options
     * @param atomicSave whether to use atomic saves
     * @param createParentDirs whether to create parent directories
     * @return a new ConfigSaver
     * @since 1.0.0
     */
    @NotNull
    public ConfigSaver with(
            @NotNull ConfigurationOptions options,
            boolean atomicSave,
            boolean createParentDirs
    ) {
        return new ConfigSaver(options, atomicSave, createParentDirs);
    }

    /**
     * Saves a configuration node to a file.
     *
     * <p>The format is auto-detected from the file extension.</p>
     *
     * @param node the configuration node to save
     * @param path the path to save to
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    public void save(@NotNull ConfigurationNode node, @NotNull Path path) {
        save(node, path, ConfigFormat.fromPath(path));
    }

    /**
     * Saves a configuration node with explicit format.
     *
     * @param node the configuration node to save
     * @param path the path to save to
     * @param format the configuration format
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    public void save(@NotNull ConfigurationNode node, @NotNull Path path, @NotNull ConfigFormat format) {
        try {
            ensureParentDirectories(path);

            if (atomicSave) {
                saveAtomic(node, path, format);
            } else {
                saveDirectly(node, path, format);
            }
        } catch (IOException e) {
            throw new ConfigException("Failed to save configuration to " + path, path, null, e);
        }
    }

    /**
     * Saves a configuration object to a file.
     *
     * @param config the configuration object to save
     * @param path the path to save to
     * @param <T> the configuration type
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    public <T> void save(@NotNull T config, @NotNull Path path) {
        save(config, path, ConfigFormat.fromPath(path));
    }

    /**
     * Saves a configuration object with explicit format.
     *
     * @param config the configuration object to save
     * @param path the path to save to
     * @param format the configuration format
     * @param <T> the configuration type
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    public <T> void save(@NotNull T config, @NotNull Path path, @NotNull ConfigFormat format) {
        try {
            ensureParentDirectories(path);

            ConfigurationLoader<?> loader = createLoader(path, format);
            ConfigurationNode node = loader.createNode();
            node.set(config.getClass(), config);

            if (atomicSave) {
                saveAtomic(node, path, format);
            } else {
                loader.save(node);
            }
        } catch (IOException e) {
            throw new ConfigException("Failed to save configuration to " + path, path, null, e);
        } catch (Exception e) {
            throw new ConfigException("Failed to serialize configuration", path, null, e);
        }
    }

    /**
     * Saves a configuration node asynchronously.
     *
     * @param node the configuration node to save
     * @param path the path to save to
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> saveAsync(@NotNull ConfigurationNode node, @NotNull Path path) {
        return CompletableFuture.runAsync(() -> save(node, path));
    }

    /**
     * Saves a configuration object asynchronously.
     *
     * @param config the configuration object to save
     * @param path the path to save to
     * @param <T> the configuration type
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    public <T> CompletableFuture<Void> saveAsync(@NotNull T config, @NotNull Path path) {
        return CompletableFuture.runAsync(() -> save(config, path));
    }

    /**
     * Saves with a backup of the existing file.
     *
     * @param node the configuration node to save
     * @param path the path to save to
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    public void saveWithBackup(@NotNull ConfigurationNode node, @NotNull Path path) {
        saveWithBackup(node, path, ConfigFormat.fromPath(path));
    }

    /**
     * Saves with a backup and explicit format.
     *
     * @param node the configuration node to save
     * @param path the path to save to
     * @param format the configuration format
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    public void saveWithBackup(
            @NotNull ConfigurationNode node,
            @NotNull Path path,
            @NotNull ConfigFormat format
    ) {
        try {
            createBackup(path);
            save(node, path, format);
        } catch (IOException e) {
            throw new ConfigException("Failed to create backup for " + path, path, null, e);
        }
    }

    /**
     * Converts a configuration node to a string.
     *
     * @param node the configuration node
     * @param format the configuration format
     * @return the configuration as a string
     * @throws ConfigException if conversion fails
     * @since 1.0.0
     */
    @NotNull
    public String toString(@NotNull ConfigurationNode node, @NotNull ConfigFormat format) {
        try {
            StringWriter writer = new StringWriter();
            ConfigurationLoader<?> loader = createWriterLoader(writer, format);
            loader.save(node);
            return writer.toString();
        } catch (IOException e) {
            throw new ConfigException("Failed to convert configuration to string", e);
        }
    }

    /**
     * Converts a configuration object to a string.
     *
     * @param config the configuration object
     * @param format the configuration format
     * @param <T> the configuration type
     * @return the configuration as a string
     * @throws ConfigException if conversion fails
     * @since 1.0.0
     */
    @NotNull
    public <T> String toString(@NotNull T config, @NotNull ConfigFormat format) {
        try {
            StringWriter writer = new StringWriter();
            ConfigurationLoader<?> loader = createWriterLoader(writer, format);
            ConfigurationNode node = loader.createNode();
            node.set(config.getClass(), config);
            loader.save(node);
            return writer.toString();
        } catch (Exception e) {
            throw new ConfigException("Failed to convert configuration to string", e);
        }
    }

    /**
     * Saves a configuration node directly without atomic write.
     */
    private void saveDirectly(
            @NotNull ConfigurationNode node,
            @NotNull Path path,
            @NotNull ConfigFormat format
    ) throws IOException {
        ConfigurationLoader<?> loader = createLoader(path, format);
        loader.save(node);
    }

    /**
     * Saves a configuration node atomically (write to temp, then rename).
     */
    private void saveAtomic(
            @NotNull ConfigurationNode node,
            @NotNull Path path,
            @NotNull ConfigFormat format
    ) throws IOException {
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            ConfigurationLoader<?> loader = createLoader(tempPath, format);
            loader.save(node);
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Try non-atomic if atomic move not supported
            try {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                // Clean up temp file if move fails
                Files.deleteIfExists(tempPath);
                throw e;
            }
        }
    }

    /**
     * Creates a backup of the existing file.
     */
    private void createBackup(@NotNull Path path) throws IOException {
        if (Files.exists(path)) {
            Path backupPath = path.resolveSibling(path.getFileName() + ".bak");
            Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Ensures parent directories exist.
     */
    private void ensureParentDirectories(@NotNull Path path) throws IOException {
        if (createParentDirs) {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        }
    }

    /**
     * Creates a configuration loader for the specified path.
     */
    @NotNull
    private ConfigurationLoader<?> createLoader(@NotNull Path path, @NotNull ConfigFormat format) {
        return switch (format) {
            case YAML -> org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .path(path)
                    .defaultOptions(options)
                    .build();
            case HOCON -> org.spongepowered.configurate.hocon.HoconConfigurationLoader.builder()
                    .path(path)
                    .defaultOptions(options)
                    .build();
            case JSON -> org.spongepowered.configurate.gson.GsonConfigurationLoader.builder()
                    .path(path)
                    .defaultOptions(options)
                    .indent(2)
                    .build();
            case TOML -> throw new UnsupportedOperationException(
                    "TOML format is not yet supported. Use YAML, HOCON, or JSON instead."
            );
        };
    }

    /**
     * Creates a configuration loader for a writer.
     */
    @NotNull
    private ConfigurationLoader<?> createWriterLoader(@NotNull Writer writer, @NotNull ConfigFormat format) {
        BufferedWriter bufferedWriter = writer instanceof BufferedWriter
                ? (BufferedWriter) writer
                : new BufferedWriter(writer);

        return switch (format) {
            case YAML -> org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .sink(() -> bufferedWriter)
                    .defaultOptions(options)
                    .build();
            case HOCON -> org.spongepowered.configurate.hocon.HoconConfigurationLoader.builder()
                    .sink(() -> bufferedWriter)
                    .defaultOptions(options)
                    .build();
            case JSON -> org.spongepowered.configurate.gson.GsonConfigurationLoader.builder()
                    .sink(() -> bufferedWriter)
                    .defaultOptions(options)
                    .indent(2)
                    .build();
            case TOML -> throw new UnsupportedOperationException(
                    "TOML format is not yet supported. Use YAML, HOCON, or JSON instead."
            );
        };
    }
}
