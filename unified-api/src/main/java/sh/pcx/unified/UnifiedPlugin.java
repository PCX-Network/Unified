/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified;

import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.service.ServiceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Abstract base class for all plugins using the UnifiedPlugin API framework.
 *
 * <p>This class provides a platform-agnostic plugin lifecycle with standardized
 * hooks for initialization, enabling, disabling, and reloading. Extending this
 * class allows your plugin to run seamlessly on Paper, Folia, and Sponge servers.
 *
 * <h2>Lifecycle Order</h2>
 * <ol>
 *   <li>{@link #onLoad()} - Called when the plugin JAR is loaded (before enabling)</li>
 *   <li>{@link #onEnable()} - Called when the plugin is enabled and ready</li>
 *   <li>{@link #onDisable()} - Called when the plugin is being disabled</li>
 *   <li>{@link #onReload()} - Called when an administrator requests a reload</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *
 *     @Override
 *     public void onLoad() {
 *         getLogger().info("Plugin loading...");
 *         // Register services, load configs
 *     }
 *
 *     @Override
 *     public void onEnable() {
 *         getLogger().info("Plugin enabled!");
 *         // Register commands, listeners, start tasks
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         getLogger().info("Plugin disabled!");
 *         // Save data, cleanup resources
 *     }
 *
 *     @Override
 *     public void onReload() {
 *         getLogger().info("Plugin reloading...");
 *         // Reload configurations, reset caches
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class UnifiedPlugin {

    private PluginMeta meta;
    private Logger logger;
    private Path dataFolder;
    private boolean enabled;
    private UnifiedServer server;
    private ServiceRegistry serviceRegistry;

    /**
     * Called when the plugin is loaded, before it is enabled.
     *
     * <p>Use this method to perform early initialization such as:
     * <ul>
     *   <li>Registering services with the service registry</li>
     *   <li>Loading configuration files</li>
     *   <li>Setting up database connections</li>
     *   <li>Initializing static resources</li>
     * </ul>
     *
     * <p>At this stage, other plugins may not be loaded yet. Do not
     * attempt to access other plugins or their services in this method.
     *
     * @since 1.0.0
     */
    public void onLoad() {
        // Default implementation does nothing
    }

    /**
     * Called when the plugin is enabled and ready to operate.
     *
     * <p>Use this method to:
     * <ul>
     *   <li>Register event listeners</li>
     *   <li>Register commands</li>
     *   <li>Start scheduled tasks</li>
     *   <li>Hook into other plugins</li>
     *   <li>Initialize gameplay features</li>
     * </ul>
     *
     * <p>All dependencies should be available at this point.
     *
     * @since 1.0.0
     */
    public abstract void onEnable();

    /**
     * Called when the plugin is being disabled.
     *
     * <p>Use this method to:
     * <ul>
     *   <li>Save any unsaved data</li>
     *   <li>Cancel scheduled tasks</li>
     *   <li>Close database connections</li>
     *   <li>Release resources</li>
     *   <li>Unregister listeners and commands</li>
     * </ul>
     *
     * <p>This method should complete quickly to avoid blocking server shutdown.
     *
     * @since 1.0.0
     */
    public abstract void onDisable();

    /**
     * Called when an administrator requests a plugin reload.
     *
     * <p>Use this method to:
     * <ul>
     *   <li>Reload configuration files</li>
     *   <li>Reset caches</li>
     *   <li>Refresh external data sources</li>
     *   <li>Re-register modified commands or listeners</li>
     * </ul>
     *
     * <p>The default implementation does nothing. Override this method
     * if your plugin supports hot-reloading.
     *
     * @since 1.0.0
     */
    public void onReload() {
        // Default implementation does nothing
    }

    /**
     * Returns the metadata for this plugin.
     *
     * @return the plugin metadata, never null after initialization
     * @since 1.0.0
     */
    @NotNull
    public final PluginMeta getMeta() {
        return meta;
    }

    /**
     * Returns the name of this plugin.
     *
     * @return the plugin name
     * @since 1.0.0
     */
    @NotNull
    public final String getName() {
        return meta.name();
    }

    /**
     * Returns the version of this plugin.
     *
     * @return the plugin version string
     * @since 1.0.0
     */
    @NotNull
    public final String getVersion() {
        return meta.version();
    }

    /**
     * Returns the logger for this plugin.
     *
     * @return the plugin logger
     * @since 1.0.0
     */
    @NotNull
    public final Logger getLogger() {
        return logger;
    }

    /**
     * Returns the data folder for this plugin.
     *
     * <p>This is the directory where the plugin should store its
     * configuration files, data files, and other persistent data.
     * The directory is automatically created if it does not exist.
     *
     * @return the plugin's data folder path
     * @since 1.0.0
     */
    @NotNull
    public final Path getDataFolder() {
        return dataFolder;
    }

    /**
     * Returns the data folder as a File object.
     *
     * @return the plugin's data folder as a File
     * @since 1.0.0
     */
    @NotNull
    public final File getDataFolderAsFile() {
        return dataFolder.toFile();
    }

    /**
     * Returns whether this plugin is currently enabled.
     *
     * @return true if the plugin is enabled, false otherwise
     * @since 1.0.0
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the unified server instance.
     *
     * @return the server instance
     * @since 1.0.0
     */
    @NotNull
    public final UnifiedServer getServer() {
        return server;
    }

    /**
     * Returns the service registry for registering and looking up services.
     *
     * @return the service registry
     * @since 1.0.0
     */
    @NotNull
    public final ServiceRegistry getServices() {
        return serviceRegistry;
    }

    /**
     * Returns a resource from the plugin's JAR file as an input stream.
     *
     * <p>Use this to read embedded resource files like default configurations
     * or language files.
     *
     * @param name the name of the resource (e.g., "config.yml")
     * @return an input stream to the resource, or null if not found
     * @since 1.0.0
     */
    @Nullable
    public final InputStream getResource(@NotNull String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    /**
     * Saves a default resource to the plugin's data folder if it doesn't exist.
     *
     * <p>This is typically used to extract default configuration files from
     * the plugin JAR to the data folder on first run.
     *
     * @param resourcePath the path to the resource in the JAR
     * @since 1.0.0
     */
    public final void saveDefaultResource(@NotNull String resourcePath) {
        saveResource(resourcePath, false);
    }

    /**
     * Saves a resource to the plugin's data folder.
     *
     * @param resourcePath the path to the resource in the JAR
     * @param replace      whether to replace existing files
     * @since 1.0.0
     */
    public abstract void saveResource(@NotNull String resourcePath, boolean replace);

    /**
     * Initializes the plugin with platform-specific components.
     *
     * <p>This method is called by the platform implementation and should
     * not be called directly by plugin developers.
     *
     * @param meta            the plugin metadata
     * @param logger          the logger for this plugin
     * @param dataFolder      the data folder for this plugin
     * @param server          the unified server instance
     * @param serviceRegistry the service registry
     * @since 1.0.0
     */
    public final void initialize(
            @NotNull PluginMeta meta,
            @NotNull Logger logger,
            @NotNull Path dataFolder,
            @NotNull UnifiedServer server,
            @NotNull ServiceRegistry serviceRegistry
    ) {
        this.meta = meta;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.server = server;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Sets the enabled state of this plugin.
     *
     * <p>This method is called by the platform implementation and should
     * not be called directly by plugin developers.
     *
     * @param enabled the new enabled state
     * @since 1.0.0
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
