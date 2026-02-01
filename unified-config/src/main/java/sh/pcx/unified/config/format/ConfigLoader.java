/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.format;

import sh.pcx.unified.config.ConfigException;
import sh.pcx.unified.config.ConfigNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Loads configuration files in any supported format using Sponge Configurate.
 *
 * <p>ConfigLoader provides a unified interface for loading configuration files
 * regardless of their format. It automatically creates the appropriate loader
 * based on the file extension and handles common error cases.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Multi-format:</b> YAML, HOCON, JSON, TOML</li>
 *   <li><b>Auto-detection:</b> Format detected from file extension</li>
 *   <li><b>Object mapping:</b> Direct deserialization to typed objects</li>
 *   <li><b>Resource loading:</b> Load from classpath resources</li>
 *   <li><b>Async support:</b> Non-blocking file operations</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * ConfigLoader loader = new ConfigLoader();
 *
 * // Load raw node
 * ConfigurationNode node = loader.load(path);
 *
 * // Load with explicit format
 * ConfigurationNode node = loader.load(path, ConfigFormat.HOCON);
 *
 * // Load and map to object
 * PluginConfig config = loader.load(path, PluginConfig.class);
 *
 * // Load from resource
 * ConfigurationNode defaults = loader.loadResource("defaults.yml", getClass());
 * }</pre>
 *
 * <h2>Async Loading</h2>
 * <pre>{@code
 * loader.loadAsync(path, PluginConfig.class)
 *     .thenAccept(config -> {
 *         logger.info("Loaded config: " + config);
 *     })
 *     .exceptionally(error -> {
 *         logger.severe("Failed to load: " + error.getMessage());
 *         return null;
 *     });
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigFormat
 * @see ConfigSaver
 */
public class ConfigLoader {

    private final ConfigurationOptions options;

    /**
     * Creates a new ConfigLoader with default options.
     */
    public ConfigLoader() {
        this(ConfigurationOptions.defaults());
    }

    /**
     * Creates a new ConfigLoader with custom options.
     *
     * @param options the configuration options
     */
    public ConfigLoader(@NotNull ConfigurationOptions options) {
        this.options = options;
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
     * Creates a new loader with updated options.
     *
     * @param options the new options
     * @return a new ConfigLoader with the options
     * @since 1.0.0
     */
    @NotNull
    public ConfigLoader withOptions(@NotNull ConfigurationOptions options) {
        return new ConfigLoader(options);
    }

    /**
     * Loads a configuration file as a raw node.
     *
     * <p>The format is auto-detected from the file extension.</p>
     *
     * @param path the path to the configuration file
     * @return the loaded configuration node
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode load(@NotNull Path path) {
        return load(path, ConfigFormat.fromPath(path));
    }

    /**
     * Loads a configuration file with explicit format.
     *
     * @param path the path to the configuration file
     * @param format the configuration format
     * @return the loaded configuration node
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode load(@NotNull Path path, @NotNull ConfigFormat format) {
        if (!Files.exists(path)) {
            throw ConfigException.fileNotFound(path);
        }

        try {
            ConfigurationLoader<?> loader = createLoader(path, format);
            return loader.load();
        } catch (IOException e) {
            throw ConfigException.parseError(path, e);
        }
    }

    /**
     * Loads a configuration file and maps it to a typed object.
     *
     * @param path the path to the configuration file
     * @param type the configuration class type
     * @param <T> the configuration type
     * @return the deserialized configuration
     * @throws ConfigException if loading or mapping fails
     * @since 1.0.0
     */
    @NotNull
    public <T> T load(@NotNull Path path, @NotNull Class<T> type) {
        return load(path, ConfigFormat.fromPath(path), type);
    }

    /**
     * Loads a configuration file with explicit format and maps to object.
     *
     * @param path the path to the configuration file
     * @param format the configuration format
     * @param type the configuration class type
     * @param <T> the configuration type
     * @return the deserialized configuration
     * @throws ConfigException if loading or mapping fails
     * @since 1.0.0
     */
    @NotNull
    public <T> T load(@NotNull Path path, @NotNull ConfigFormat format, @NotNull Class<T> type) {
        ConfigurationNode node = load(path, format);
        try {
            T result = node.get(type);
            if (result == null) {
                throw new ConfigException("Failed to deserialize configuration to " + type.getName(), path);
            }
            return result;
        } catch (Exception e) {
            if (e instanceof ConfigException) {
                throw (ConfigException) e;
            }
            throw ConfigException.serializationError(path, "", e);
        }
    }

    /**
     * Loads a configuration file asynchronously.
     *
     * @param path the path to the configuration file
     * @return a future that completes with the loaded node
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ConfigurationNode> loadAsync(@NotNull Path path) {
        return loadAsync(path, ConfigFormat.fromPath(path));
    }

    /**
     * Loads a configuration file asynchronously with explicit format.
     *
     * @param path the path to the configuration file
     * @param format the configuration format
     * @return a future that completes with the loaded node
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ConfigurationNode> loadAsync(@NotNull Path path, @NotNull ConfigFormat format) {
        return CompletableFuture.supplyAsync(() -> load(path, format));
    }

    /**
     * Loads a configuration file asynchronously and maps to object.
     *
     * @param path the path to the configuration file
     * @param type the configuration class type
     * @param <T> the configuration type
     * @return a future that completes with the configuration
     * @since 1.0.0
     */
    @NotNull
    public <T> CompletableFuture<T> loadAsync(@NotNull Path path, @NotNull Class<T> type) {
        return CompletableFuture.supplyAsync(() -> load(path, type));
    }

    /**
     * Loads a configuration from a classpath resource.
     *
     * @param resourcePath the path to the resource
     * @param clazz the class to use for resource loading
     * @param format the configuration format
     * @return the loaded configuration node
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode loadResource(
            @NotNull String resourcePath,
            @NotNull Class<?> clazz,
            @NotNull ConfigFormat format
    ) {
        try (InputStream is = clazz.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new ConfigException("Resource not found: " + resourcePath);
            }
            return loadFromStream(is, format);
        } catch (IOException e) {
            throw new ConfigException("Failed to load resource: " + resourcePath, e);
        }
    }

    /**
     * Loads a configuration from a classpath resource with auto-detected format.
     *
     * @param resourcePath the path to the resource
     * @param clazz the class to use for resource loading
     * @return the loaded configuration node
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode loadResource(@NotNull String resourcePath, @NotNull Class<?> clazz) {
        ConfigFormat format = ConfigFormat.fromFilename(resourcePath)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot detect format from resource path: " + resourcePath
                ));
        return loadResource(resourcePath, clazz, format);
    }

    /**
     * Loads a configuration from an input stream.
     *
     * @param inputStream the input stream
     * @param format the configuration format
     * @return the loaded configuration node
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode loadFromStream(@NotNull InputStream inputStream, @NotNull ConfigFormat format) {
        try {
            ConfigurationLoader<?> loader = createStreamLoader(inputStream, format);
            return loader.load();
        } catch (IOException e) {
            throw new ConfigException("Failed to load from stream", e);
        }
    }

    /**
     * Loads a configuration from a string.
     *
     * @param content the configuration content
     * @param format the configuration format
     * @return the loaded configuration node
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode loadFromString(@NotNull String content, @NotNull ConfigFormat format) {
        try {
            ConfigurationLoader<?> loader = createStringLoader(content, format);
            return loader.load();
        } catch (IOException e) {
            throw new ConfigException("Failed to load from string", e);
        }
    }

    /**
     * Loads configuration and merges with defaults.
     *
     * <p>Values from the file override defaults. Missing values
     * are populated from defaults.</p>
     *
     * @param path the path to the configuration file
     * @param defaults the default configuration node
     * @return the merged configuration node
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode loadWithDefaults(@NotNull Path path, @NotNull ConfigurationNode defaults) {
        if (!Files.exists(path)) {
            return defaults.copy();
        }

        ConfigurationNode loaded = load(path);
        return mergeNodes(defaults.copy(), loaded);
    }

    /**
     * Creates a configuration loader for the specified path and format.
     *
     * @param path the file path
     * @param format the configuration format
     * @return the configuration loader
     * @throws ConfigException if the format is not supported
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationLoader<?> createLoader(@NotNull Path path, @NotNull ConfigFormat format) {
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
                    .build();
            case TOML -> throw new UnsupportedOperationException(
                    "TOML format is not yet supported. Use YAML, HOCON, or JSON instead."
            );
        };
    }

    /**
     * Creates a configuration loader for a stream.
     *
     * @param inputStream the input stream
     * @param format the configuration format
     * @return the configuration loader
     * @since 1.0.0
     */
    @NotNull
    private ConfigurationLoader<?> createStreamLoader(
            @NotNull InputStream inputStream,
            @NotNull ConfigFormat format
    ) {
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream));
        return createReaderLoader(reader, format);
    }

    /**
     * Creates a configuration loader for a string.
     *
     * @param content the string content
     * @param format the configuration format
     * @return the configuration loader
     * @since 1.0.0
     */
    @NotNull
    private ConfigurationLoader<?> createStringLoader(@NotNull String content, @NotNull ConfigFormat format) {
        return createReaderLoader(new java.io.StringReader(content), format);
    }

    /**
     * Creates a configuration loader for a reader.
     *
     * @param reader the reader
     * @param format the configuration format
     * @return the configuration loader
     * @since 1.0.0
     */
    @NotNull
    private ConfigurationLoader<?> createReaderLoader(@NotNull Reader reader, @NotNull ConfigFormat format) {
        BufferedReader bufferedReader = reader instanceof BufferedReader
                ? (BufferedReader) reader
                : new BufferedReader(reader);

        return switch (format) {
            case YAML -> org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .source(() -> bufferedReader)
                    .defaultOptions(options)
                    .build();
            case HOCON -> org.spongepowered.configurate.hocon.HoconConfigurationLoader.builder()
                    .source(() -> bufferedReader)
                    .defaultOptions(options)
                    .build();
            case JSON -> org.spongepowered.configurate.gson.GsonConfigurationLoader.builder()
                    .source(() -> bufferedReader)
                    .defaultOptions(options)
                    .build();
            case TOML -> throw new UnsupportedOperationException(
                    "TOML format is not yet supported. Use YAML, HOCON, or JSON instead."
            );
        };
    }

    /**
     * Merges two configuration nodes.
     *
     * <p>Values from the override node take precedence.</p>
     *
     * @param base the base node
     * @param override the override node
     * @return the merged node
     * @since 1.0.0
     */
    @NotNull
    private ConfigurationNode mergeNodes(
            @NotNull ConfigurationNode base,
            @NotNull ConfigurationNode override
    ) {
        if (override.isMap()) {
            for (var entry : override.childrenMap().entrySet()) {
                ConfigurationNode baseChild = base.node(entry.getKey());
                ConfigurationNode overrideChild = entry.getValue();

                if (baseChild.isMap() && overrideChild.isMap()) {
                    mergeNodes(baseChild, overrideChild);
                } else {
                    try {
                        baseChild.set(overrideChild.raw());
                    } catch (Exception e) {
                        // Ignore merge errors for individual nodes
                    }
                }
            }
        } else if (!override.virtual()) {
            try {
                base.set(override.raw());
            } catch (Exception e) {
                // Ignore merge errors
            }
        }
        return base;
    }
}
