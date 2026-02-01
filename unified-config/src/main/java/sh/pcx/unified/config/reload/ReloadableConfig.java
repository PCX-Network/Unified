/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.reload;

import sh.pcx.unified.config.ConfigException;
import sh.pcx.unified.config.ConfigRoot;
import sh.pcx.unified.config.format.ConfigFormat;
import sh.pcx.unified.config.format.ConfigLoader;
import sh.pcx.unified.config.format.ConfigSaver;
import sh.pcx.unified.config.validation.ConfigValidator;
import sh.pcx.unified.config.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A self-reloading configuration wrapper that watches for file changes.
 *
 * <p>ReloadableConfig provides a convenient way to work with configurations
 * that automatically reload when the underlying file changes. It maintains
 * the current configuration in memory and updates it transparently.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Auto-reload:</b> Watches file for changes</li>
 *   <li><b>Thread-safe:</b> Safe concurrent access</li>
 *   <li><b>Validation:</b> Optional validation on reload</li>
 *   <li><b>Callbacks:</b> Notification of reload events</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create reloadable config
 * ReloadableConfig<PluginConfig> config = ReloadableConfig.create(
 *     PluginConfig.class,
 *     dataFolder.resolve("config.yml"),
 *     configLoader
 * );
 *
 * // Enable watching
 * config.enableHotReload();
 *
 * // Access current config
 * int maxPlayers = config.get().getMaxPlayers();
 *
 * // Add reload listener
 * config.addReloadListener(result -> {
 *     if (result.isSuccess()) {
 *         logger.info("Config reloaded!");
 *     }
 * });
 *
 * // Save changes
 * config.get().setMaxPlayers(50);
 * config.save();
 *
 * // Cleanup
 * config.close();
 * }</pre>
 *
 * <h2>With Validation</h2>
 * <pre>{@code
 * ReloadableConfig<PluginConfig> config = ReloadableConfig.builder(PluginConfig.class)
 *     .path(configPath)
 *     .loader(configLoader)
 *     .saver(configSaver)
 *     .validator(configValidator)
 *     .validateOnReload(true)
 *     .build();
 * }</pre>
 *
 * @param <T> the configuration type
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigWatcher
 * @see ReloadHandler
 */
public class ReloadableConfig<T> implements AutoCloseable {

    private final Class<T> type;
    private final Path path;
    private final ConfigFormat format;
    private final ConfigLoader loader;
    private final ConfigSaver saver;
    private final ConfigValidator validator;
    private final boolean validateOnReload;

    private final AtomicReference<T> current;
    private final AtomicLong lastReloadTime;

    private ConfigWatcher watcher;
    private ReloadHandler<T> reloadListener;
    private boolean hotReloadEnabled;

    private ReloadableConfig(
            @NotNull Class<T> type,
            @NotNull Path path,
            @NotNull ConfigFormat format,
            @NotNull ConfigLoader loader,
            @NotNull ConfigSaver saver,
            @Nullable ConfigValidator validator,
            boolean validateOnReload,
            @NotNull T initial
    ) {
        this.type = type;
        this.path = path.toAbsolutePath().normalize();
        this.format = format;
        this.loader = loader;
        this.saver = saver;
        this.validator = validator;
        this.validateOnReload = validateOnReload;
        this.current = new AtomicReference<>(initial);
        this.lastReloadTime = new AtomicLong(System.currentTimeMillis());
        this.hotReloadEnabled = false;
    }

    /**
     * Creates a new ReloadableConfig.
     *
     * @param type the configuration class type
     * @param path the path to the configuration file
     * @param loader the config loader
     * @param <T> the configuration type
     * @return the reloadable config
     * @since 1.0.0
     */
    @NotNull
    public static <T> ReloadableConfig<T> create(
            @NotNull Class<T> type,
            @NotNull Path path,
            @NotNull ConfigLoader loader
    ) {
        return builder(type)
                .path(path)
                .loader(loader)
                .build();
    }

    /**
     * Creates a builder for ReloadableConfig.
     *
     * @param type the configuration class type
     * @param <T> the configuration type
     * @return the builder
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder(@NotNull Class<T> type) {
        return new Builder<>(type);
    }

    /**
     * Gets the current configuration.
     *
     * @return the current configuration
     * @since 1.0.0
     */
    @NotNull
    public T get() {
        return current.get();
    }

    /**
     * Sets the current configuration.
     *
     * <p>Note: This only updates the in-memory config.
     * Call {@link #save()} to persist.</p>
     *
     * @param config the new configuration
     * @since 1.0.0
     */
    public void set(@NotNull T config) {
        current.set(config);
    }

    /**
     * Gets the configuration type.
     *
     * @return the configuration class
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getType() {
        return type;
    }

    /**
     * Gets the path to the configuration file.
     *
     * @return the file path
     * @since 1.0.0
     */
    @NotNull
    public Path getPath() {
        return path;
    }

    /**
     * Gets the configuration format.
     *
     * @return the format
     * @since 1.0.0
     */
    @NotNull
    public ConfigFormat getFormat() {
        return format;
    }

    /**
     * Gets the last reload timestamp.
     *
     * @return milliseconds since epoch of last reload
     * @since 1.0.0
     */
    public long getLastReloadTime() {
        return lastReloadTime.get();
    }

    /**
     * Saves the current configuration to disk.
     *
     * @throws ConfigException if saving fails
     * @since 1.0.0
     */
    public void save() {
        saver.save(current.get(), path, format);
    }

    /**
     * Saves the configuration asynchronously.
     *
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> saveAsync() {
        return saver.saveAsync(current.get(), path);
    }

    /**
     * Reloads the configuration from disk.
     *
     * @return the reloaded configuration
     * @throws ConfigException if reloading fails
     * @since 1.0.0
     */
    @NotNull
    public T reload() {
        T loaded = loader.load(path, format, type);

        if (validateOnReload && validator != null) {
            ValidationResult result = validator.validate(loaded);
            if (result.hasErrors()) {
                throw new ConfigException(
                        "Configuration validation failed after reload",
                        path,
                        result.getErrors().stream()
                                .map(e -> e.getFullMessage())
                                .toList()
                );
            }
        }

        current.set(loaded);
        lastReloadTime.set(System.currentTimeMillis());

        if (reloadListener != null) {
            reloadListener.onReload(ReloadResult.success(loaded, path, false));
        }

        return loaded;
    }

    /**
     * Reloads the configuration asynchronously.
     *
     * @return a future that completes with the reloaded configuration
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<T> reloadAsync() {
        return CompletableFuture.supplyAsync(this::reload);
    }

    /**
     * Validates the current configuration.
     *
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult validate() {
        if (validator == null) {
            return ValidationResult.success();
        }
        return validator.validate(current.get());
    }

    /**
     * Enables hot reload (file watching).
     *
     * @since 1.0.0
     */
    public void enableHotReload() {
        enableHotReload(null);
    }

    /**
     * Enables hot reload with a listener.
     *
     * @param listener the reload listener
     * @since 1.0.0
     */
    public void enableHotReload(@Nullable ReloadHandler<T> listener) {
        if (hotReloadEnabled) {
            return;
        }

        watcher = new ConfigWatcher(loader);
        watcher.watch(type, path, format, result -> {
            if (result.isSuccess()) {
                T loaded = result.get();

                // Validate if configured
                if (validateOnReload && validator != null) {
                    ValidationResult validation = validator.validate(loaded);
                    if (validation.hasErrors()) {
                        if (listener != null) {
                            listener.onReload(ReloadResult.failure(
                                    new ConfigException(
                                            "Validation failed",
                                            path,
                                            validation.getErrors().stream()
                                                    .map(e -> e.getFullMessage())
                                                    .toList()
                                    ),
                                    path
                            ));
                        }
                        return;
                    }
                }

                current.set(loaded);
                lastReloadTime.set(System.currentTimeMillis());
            }

            // Notify listener
            if (listener != null) {
                listener.onReload(result);
            }
            if (reloadListener != null && reloadListener != listener) {
                reloadListener.onReload(result);
            }
        });

        hotReloadEnabled = true;
    }

    /**
     * Disables hot reload.
     *
     * @since 1.0.0
     */
    public void disableHotReload() {
        if (!hotReloadEnabled) {
            return;
        }

        if (watcher != null) {
            watcher.unwatch(path);
            watcher.close();
            watcher = null;
        }

        hotReloadEnabled = false;
    }

    /**
     * Checks if hot reload is enabled.
     *
     * @return true if enabled
     * @since 1.0.0
     */
    public boolean isHotReloadEnabled() {
        return hotReloadEnabled;
    }

    /**
     * Sets the reload listener.
     *
     * @param listener the listener
     * @since 1.0.0
     */
    public void setReloadListener(@Nullable ReloadHandler<T> listener) {
        this.reloadListener = listener;
    }

    /**
     * Adds a reload listener.
     *
     * @param listener the listener to add
     * @since 1.0.0
     */
    public void addReloadListener(@NotNull ReloadHandler<T> listener) {
        if (this.reloadListener == null) {
            this.reloadListener = listener;
        } else {
            this.reloadListener = this.reloadListener.andThen(listener);
        }
    }

    @Override
    public void close() {
        disableHotReload();
    }

    /**
     * Builder for ReloadableConfig.
     *
     * @param <T> the configuration type
     */
    public static class Builder<T> {

        private final Class<T> type;
        private Path path;
        private ConfigFormat format;
        private ConfigLoader loader;
        private ConfigSaver saver;
        private ConfigValidator validator;
        private boolean validateOnReload;

        private Builder(@NotNull Class<T> type) {
            this.type = type;
        }

        /**
         * Sets the path to the configuration file.
         *
         * @param path the file path
         * @return this builder
         */
        @NotNull
        public Builder<T> path(@NotNull Path path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the configuration format.
         *
         * @param format the format
         * @return this builder
         */
        @NotNull
        public Builder<T> format(@NotNull ConfigFormat format) {
            this.format = format;
            return this;
        }

        /**
         * Sets the config loader.
         *
         * @param loader the loader
         * @return this builder
         */
        @NotNull
        public Builder<T> loader(@NotNull ConfigLoader loader) {
            this.loader = loader;
            return this;
        }

        /**
         * Sets the config saver.
         *
         * @param saver the saver
         * @return this builder
         */
        @NotNull
        public Builder<T> saver(@NotNull ConfigSaver saver) {
            this.saver = saver;
            return this;
        }

        /**
         * Sets the config validator.
         *
         * @param validator the validator
         * @return this builder
         */
        @NotNull
        public Builder<T> validator(@NotNull ConfigValidator validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Enables validation on reload.
         *
         * @param validate true to validate
         * @return this builder
         */
        @NotNull
        public Builder<T> validateOnReload(boolean validate) {
            this.validateOnReload = validate;
            return this;
        }

        /**
         * Builds the ReloadableConfig.
         *
         * @return the reloadable config
         * @throws ConfigException if configuration is invalid or loading fails
         */
        @NotNull
        public ReloadableConfig<T> build() {
            if (path == null) {
                throw new IllegalStateException("Path is required");
            }
            if (loader == null) {
                loader = new ConfigLoader();
            }
            if (saver == null) {
                saver = new ConfigSaver();
            }
            if (format == null) {
                format = ConfigFormat.fromPath(path);
            }

            // Load initial configuration
            T initial = loader.load(path, format, type);

            return new ReloadableConfig<>(
                    type, path, format, loader, saver,
                    validator, validateOnReload, initial
            );
        }
    }
}
