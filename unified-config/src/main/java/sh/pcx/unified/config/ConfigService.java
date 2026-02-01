/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config;

import sh.pcx.unified.config.format.ConfigFormat;
import sh.pcx.unified.config.reload.ReloadHandler;
import sh.pcx.unified.config.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main service interface for configuration management using Sponge Configurate.
 *
 * <p>The ConfigService provides a unified API for loading, saving, validating,
 * and watching configuration files in multiple formats (YAML, HOCON, JSON, TOML).
 * It supports hot-reloading, environment variable overrides, and annotation-based
 * object mapping.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Multi-format support:</b> YAML, HOCON, JSON, and TOML</li>
 *   <li><b>Type-safe mapping:</b> Map configurations to annotated Java objects</li>
 *   <li><b>Hot reload:</b> Automatic file watching with reload callbacks</li>
 *   <li><b>Validation:</b> Constraint annotations with descriptive error messages</li>
 *   <li><b>Environment overrides:</b> Override values using environment variables</li>
 *   <li><b>Profiles:</b> Support for dev, prod, and custom profiles</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Get the config service
 * ConfigService configService = UnifiedAPI.services().get(ConfigService.class);
 *
 * // Load a configuration
 * PluginConfig config = configService.load(
 *     PluginConfig.class,
 *     dataFolder.resolve("config.yml")
 * );
 *
 * // Enable hot reload
 * configService.watch(configPath, result -> {
 *     if (result.isSuccess()) {
 *         this.config = result.get();
 *         getLogger().info("Configuration reloaded!");
 *     } else {
 *         getLogger().warning("Failed to reload config: " + result.getError());
 *     }
 * });
 * }</pre>
 *
 * <h2>Configuration Class</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class PluginConfig {
 *
 *     @ConfigComment("Database connection settings")
 *     private DatabaseConfig database = new DatabaseConfig();
 *
 *     @ConfigComment("Enable debug logging")
 *     @ConfigDefault("false")
 *     private boolean debug = false;
 *
 *     @ConfigComment("Maximum players per game")
 *     @ConfigValidate(Range.class)
 *     @Range(min = 2, max = 100)
 *     private int maxPlayers = 16;
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigNode
 * @see ConfigRoot
 * @see ConfigFormat
 */
public interface ConfigService {

    /**
     * Loads a configuration from the specified path.
     *
     * <p>The format is automatically detected from the file extension.
     * If the file doesn't exist, a new configuration with defaults will be created.</p>
     *
     * @param type the configuration class type
     * @param path the path to the configuration file
     * @param <T> the configuration type
     * @return the loaded configuration instance
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    <T> T load(@NotNull Class<T> type, @NotNull Path path);

    /**
     * Loads a configuration from the specified path with explicit format.
     *
     * @param type the configuration class type
     * @param path the path to the configuration file
     * @param format the configuration format
     * @param <T> the configuration type
     * @return the loaded configuration instance
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    <T> T load(@NotNull Class<T> type, @NotNull Path path, @NotNull ConfigFormat format);

    /**
     * Loads a configuration asynchronously.
     *
     * @param type the configuration class type
     * @param path the path to the configuration file
     * @param <T> the configuration type
     * @return a future that completes with the configuration instance
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<T> loadAsync(@NotNull Class<T> type, @NotNull Path path);

    /**
     * Saves a configuration to the specified path.
     *
     * <p>The format is automatically detected from the file extension.
     * Comments from {@code @ConfigComment} annotations are preserved.</p>
     *
     * @param config the configuration instance to save
     * @param path the path to save to
     * @param <T> the configuration type
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    <T> void save(@NotNull T config, @NotNull Path path);

    /**
     * Saves a configuration with explicit format.
     *
     * @param config the configuration instance to save
     * @param path the path to save to
     * @param format the configuration format
     * @param <T> the configuration type
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    <T> void save(@NotNull T config, @NotNull Path path, @NotNull ConfigFormat format);

    /**
     * Saves a configuration asynchronously.
     *
     * @param config the configuration instance to save
     * @param path the path to save to
     * @param <T> the configuration type
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    <T> CompletableFuture<Void> saveAsync(@NotNull T config, @NotNull Path path);

    /**
     * Validates a configuration instance.
     *
     * <p>Checks all fields with validation annotations and returns
     * a result containing any validation errors.</p>
     *
     * @param config the configuration to validate
     * @param <T> the configuration type
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    <T> ValidationResult validate(@NotNull T config);

    /**
     * Loads and validates a configuration.
     *
     * <p>Combines loading and validation into a single operation.
     * Returns empty if validation fails.</p>
     *
     * @param type the configuration class type
     * @param path the path to the configuration file
     * @param <T> the configuration type
     * @return the configuration if valid, empty otherwise
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> loadValidated(@NotNull Class<T> type, @NotNull Path path);

    /**
     * Gets a raw ConfigNode for the specified path.
     *
     * <p>Useful for accessing configuration values without mapping
     * to a typed object.</p>
     *
     * @param path the path to the configuration file
     * @return the configuration node wrapper
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    ConfigNode getNode(@NotNull Path path);

    /**
     * Gets a raw ConfigNode with explicit format.
     *
     * @param path the path to the configuration file
     * @param format the configuration format
     * @return the configuration node wrapper
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    ConfigNode getNode(@NotNull Path path, @NotNull ConfigFormat format);

    /**
     * Creates a ConfigRoot for managing a configuration file.
     *
     * <p>ConfigRoot provides a higher-level API with built-in
     * save/reload functionality.</p>
     *
     * @param type the configuration class type
     * @param path the path to the configuration file
     * @param <T> the configuration type
     * @return the configuration root
     * @since 1.0.0
     */
    @NotNull
    <T> ConfigRoot<T> createRoot(@NotNull Class<T> type, @NotNull Path path);

    /**
     * Watches a configuration file for changes.
     *
     * <p>When the file is modified, the handler is called with the
     * reloaded configuration. Uses a debounce mechanism to avoid
     * multiple rapid reloads.</p>
     *
     * @param type the configuration class type
     * @param path the path to watch
     * @param handler the reload handler
     * @param <T> the configuration type
     * @since 1.0.0
     */
    <T> void watch(@NotNull Class<T> type, @NotNull Path path, @NotNull ReloadHandler<T> handler);

    /**
     * Stops watching a configuration file.
     *
     * @param path the path to stop watching
     * @since 1.0.0
     */
    void unwatch(@NotNull Path path);

    /**
     * Reloads a configuration from disk.
     *
     * <p>Forces a reload regardless of file modification time.</p>
     *
     * @param type the configuration class type
     * @param path the path to reload
     * @param <T> the configuration type
     * @return the reloaded configuration
     * @throws ConfigException if reloading fails
     * @since 1.0.0
     */
    @NotNull
    <T> T reload(@NotNull Class<T> type, @NotNull Path path);

    /**
     * Creates default configuration file if it doesn't exist.
     *
     * <p>Generates a configuration file with default values from
     * the configuration class, including comments.</p>
     *
     * @param type the configuration class type
     * @param path the path for the configuration file
     * @param <T> the configuration type
     * @return the created/existing configuration
     * @since 1.0.0
     */
    @NotNull
    <T> T createDefaults(@NotNull Class<T> type, @NotNull Path path);

    /**
     * Applies environment variable overrides to a configuration.
     *
     * <p>Environment variables are mapped using the pattern:
     * {@code ${ENV_VAR}} in string values, or by path convention
     * (e.g., {@code DATABASE_HOST} for {@code database.host}).</p>
     *
     * @param config the configuration to apply overrides to
     * @param prefix optional prefix for environment variables
     * @param <T> the configuration type
     * @return the configuration with overrides applied
     * @since 1.0.0
     */
    @NotNull
    <T> T applyEnvironmentOverrides(@NotNull T config, @Nullable String prefix);

    /**
     * Gets the active configuration profile.
     *
     * <p>Profiles allow environment-specific configurations
     * (e.g., dev, staging, prod).</p>
     *
     * @return the current profile name, or null if none
     * @since 1.0.0
     */
    @Nullable
    String getActiveProfile();

    /**
     * Sets the active configuration profile.
     *
     * @param profile the profile name to activate
     * @since 1.0.0
     */
    void setActiveProfile(@Nullable String profile);

    /**
     * Shuts down the configuration service.
     *
     * <p>Stops all file watchers and releases resources.</p>
     *
     * @since 1.0.0
     */
    void shutdown();
}
