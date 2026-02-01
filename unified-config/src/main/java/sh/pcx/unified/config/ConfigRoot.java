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

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * A root configuration holder that manages a typed configuration file.
 *
 * <p>ConfigRoot provides a high-level API for working with a single configuration
 * file, including loading, saving, validation, and hot-reload functionality.
 * It maintains both the typed configuration object and the underlying file path.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Typed access:</b> Direct access to the configuration object</li>
 *   <li><b>Save/reload:</b> Simple save and reload operations</li>
 *   <li><b>Validation:</b> Built-in validation support</li>
 *   <li><b>Hot reload:</b> File watching with callbacks</li>
 *   <li><b>Raw access:</b> Access underlying ConfigNode when needed</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create a config root
 * ConfigRoot<PluginConfig> configRoot = configService.createRoot(
 *     PluginConfig.class,
 *     dataFolder.resolve("config.yml")
 * );
 *
 * // Access configuration
 * PluginConfig config = configRoot.get();
 * int maxPlayers = config.getMaxPlayers();
 *
 * // Modify and save
 * config.setMaxPlayers(32);
 * configRoot.save();
 *
 * // Enable hot reload
 * configRoot.enableHotReload(result -> {
 *     if (result.isSuccess()) {
 *         logger.info("Config reloaded: maxPlayers=" + result.get().getMaxPlayers());
 *     }
 * });
 *
 * // Reload manually
 * configRoot.reload();
 * }</pre>
 *
 * <h2>Validation</h2>
 * <pre>{@code
 * ValidationResult result = configRoot.validate();
 * if (result.hasErrors()) {
 *     result.getErrors().forEach(error ->
 *         logger.warning("Config error: " + error.getMessage())
 *     );
 * }
 * }</pre>
 *
 * @param <T> the configuration type
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigService#createRoot(Class, Path)
 * @see ConfigNode
 */
public interface ConfigRoot<T> {

    /**
     * Gets the current configuration object.
     *
     * @return the configuration object
     * @since 1.0.0
     */
    @NotNull
    T get();

    /**
     * Sets the configuration object.
     *
     * <p>Note: This only updates the in-memory configuration.
     * Call {@link #save()} to persist changes.</p>
     *
     * @param config the new configuration object
     * @since 1.0.0
     */
    void set(@NotNull T config);

    /**
     * Gets the configuration type.
     *
     * @return the configuration class
     * @since 1.0.0
     */
    @NotNull
    Class<T> getType();

    /**
     * Gets the path to the configuration file.
     *
     * @return the file path
     * @since 1.0.0
     */
    @NotNull
    Path getPath();

    /**
     * Gets the configuration format.
     *
     * @return the format
     * @since 1.0.0
     */
    @NotNull
    ConfigFormat getFormat();

    /**
     * Gets the underlying ConfigNode.
     *
     * <p>Useful for raw access to configuration values.</p>
     *
     * @return the configuration node
     * @since 1.0.0
     */
    @NotNull
    ConfigNode getNode();

    /**
     * Saves the current configuration to disk.
     *
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    void save();

    /**
     * Saves the configuration asynchronously.
     *
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveAsync();

    /**
     * Reloads the configuration from disk.
     *
     * @return the reloaded configuration
     * @throws ConfigException if reloading fails
     * @since 1.0.0
     */
    @NotNull
    T reload();

    /**
     * Reloads the configuration asynchronously.
     *
     * @return a future that completes with the reloaded configuration
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<T> reloadAsync();

    /**
     * Validates the current configuration.
     *
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    ValidationResult validate();

    /**
     * Checks if the current configuration is valid.
     *
     * @return true if the configuration passes all validations
     * @since 1.0.0
     */
    boolean isValid();

    /**
     * Enables hot reload for this configuration.
     *
     * <p>When the configuration file is modified externally, the handler
     * is called with the reloaded configuration.</p>
     *
     * @param handler the reload handler
     * @since 1.0.0
     */
    void enableHotReload(@NotNull ReloadHandler<T> handler);

    /**
     * Enables hot reload without a handler.
     *
     * <p>The configuration is automatically reloaded but no
     * callback is invoked.</p>
     *
     * @since 1.0.0
     */
    void enableHotReload();

    /**
     * Disables hot reload for this configuration.
     *
     * @since 1.0.0
     */
    void disableHotReload();

    /**
     * Checks if hot reload is enabled.
     *
     * @return true if hot reload is enabled
     * @since 1.0.0
     */
    boolean isHotReloadEnabled();

    /**
     * Gets the last reload timestamp.
     *
     * @return timestamp of last reload in milliseconds, or 0 if never reloaded
     * @since 1.0.0
     */
    long getLastReloadTime();

    /**
     * Resets the configuration to defaults.
     *
     * <p>Creates a new instance of the configuration with default values.</p>
     *
     * @return the default configuration
     * @throws ConfigException if creating defaults fails
     * @since 1.0.0
     */
    @NotNull
    T resetToDefaults();

    /**
     * Merges default values into the current configuration.
     *
     * <p>Missing values are populated from a new default instance
     * while preserving existing values.</p>
     *
     * @since 1.0.0
     */
    void mergeDefaults();

    /**
     * Applies environment variable overrides.
     *
     * @param prefix optional prefix for environment variables
     * @since 1.0.0
     */
    void applyEnvironmentOverrides(@Nullable String prefix);

    /**
     * Closes this config root and releases resources.
     *
     * <p>Disables hot reload if enabled.</p>
     *
     * @since 1.0.0
     */
    void close();
}
