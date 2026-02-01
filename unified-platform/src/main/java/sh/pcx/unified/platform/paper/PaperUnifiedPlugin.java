/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.PluginMeta;
import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.service.ServiceRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * JavaPlugin subclass that bridges Bukkit's plugin lifecycle to {@link UnifiedPlugin}.
 *
 * <p>This class serves as the base class for plugins that want to use the UnifiedPlugin API
 * on Paper/Spigot servers. It handles the translation between Bukkit's plugin lifecycle
 * events and the unified lifecycle methods.
 *
 * <h2>Usage</h2>
 * <p>Plugins should extend this class instead of {@link JavaPlugin}:
 * <pre>{@code
 * public class MyPlugin extends PaperUnifiedPlugin {
 *
 *     @Override
 *     public void onEnable() {
 *         getLogger().info("Plugin enabled!");
 *         // Register commands, listeners, etc.
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         getLogger().info("Plugin disabled!");
 *         // Save data, cleanup resources
 *     }
 * }
 * }</pre>
 *
 * <h2>Lifecycle Mapping</h2>
 * <table border="1">
 *   <tr><th>Bukkit Method</th><th>Unified Method</th></tr>
 *   <tr><td>{@link #onLoad()}</td><td>{@link UnifiedPlugin#onLoad()}</td></tr>
 *   <tr><td>{@link #onEnable()}</td><td>{@link UnifiedPlugin#onEnable()}</td></tr>
 *   <tr><td>{@link #onDisable()}</td><td>{@link UnifiedPlugin#onDisable()}</td></tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <p>Lifecycle methods are called on the main server thread by Bukkit.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlugin
 * @see JavaPlugin
 */
public abstract class PaperUnifiedPlugin extends JavaPlugin {

    private final UnifiedPluginBridge unifiedBridge;

    /**
     * Creates a new PaperUnifiedPlugin.
     *
     * <p>This constructor initializes the unified plugin bridge that handles
     * lifecycle management.
     *
     * @since 1.0.0
     */
    public PaperUnifiedPlugin() {
        this.unifiedBridge = new UnifiedPluginBridge(this);
    }

    /**
     * Called when the plugin JAR is loaded.
     *
     * <p>Initializes the unified plugin bridge and calls the unified onLoad method.
     *
     * @since 1.0.0
     */
    @Override
    public final void onLoad() {
        try {
            unifiedBridge.initialize();
            onPluginLoad();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load plugin", e);
        }
    }

    /**
     * Called when the plugin is enabled.
     *
     * <p>Delegates to the unified onEnable method after setting up the bridge.
     *
     * @since 1.0.0
     */
    @Override
    public final void onEnable() {
        try {
            unifiedBridge.setEnabled(true);
            onPluginEnable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Called when the plugin is disabled.
     *
     * <p>Delegates to the unified onDisable method.
     *
     * @since 1.0.0
     */
    @Override
    public final void onDisable() {
        try {
            onPluginDisable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin disable", e);
        } finally {
            unifiedBridge.setEnabled(false);
        }
    }

    /**
     * Called when the plugin JAR is loaded, before it is enabled.
     *
     * <p>Override this method to perform early initialization.
     *
     * @since 1.0.0
     */
    protected void onPluginLoad() {
        // Default implementation does nothing
    }

    /**
     * Called when the plugin is enabled and ready to operate.
     *
     * <p>Override this method to set up your plugin's functionality.
     *
     * @since 1.0.0
     */
    protected abstract void onPluginEnable();

    /**
     * Called when the plugin is being disabled.
     *
     * <p>Override this method to save data and cleanup resources.
     *
     * @since 1.0.0
     */
    protected abstract void onPluginDisable();

    /**
     * Called when an administrator requests a plugin reload.
     *
     * <p>Override this method to support hot-reloading configuration.
     *
     * @since 1.0.0
     */
    protected void onPluginReload() {
        // Default implementation does nothing
    }

    /**
     * Triggers a plugin reload.
     *
     * <p>Call this method from your reload command to trigger
     * the {@link #onPluginReload()} callback.
     *
     * @since 1.0.0
     */
    public final void reload() {
        onPluginReload();
    }

    /**
     * Returns the unified plugin bridge.
     *
     * @return the bridge instance
     * @since 1.0.0
     */
    @NotNull
    public final UnifiedPluginBridge getUnifiedBridge() {
        return unifiedBridge;
    }

    /**
     * Returns the unified plugin metadata.
     *
     * <p>Note: This method is named differently from Paper's {@code getPluginMeta()}
     * to avoid conflicts with Paper's plugin API which returns a different PluginMeta type.
     *
     * @return the unified plugin metadata
     * @since 1.0.0
     */
    @NotNull
    public final PluginMeta getUnifiedMeta() {
        return unifiedBridge.getUnifiedPluginMeta();
    }

    /**
     * Internal bridge class that wraps the PaperUnifiedPlugin as a UnifiedPlugin.
     *
     * <p>This class handles the initialization and lifecycle of the unified plugin
     * abstraction layer.
     */
    public static final class UnifiedPluginBridge extends UnifiedPlugin {

        private final PaperUnifiedPlugin paperPlugin;
        private PluginMeta meta;

        /**
         * Creates a new bridge for the given Paper plugin.
         *
         * @param paperPlugin the Paper plugin to wrap
         */
        UnifiedPluginBridge(@NotNull PaperUnifiedPlugin paperPlugin) {
            this.paperPlugin = paperPlugin;
        }

        /**
         * Initializes the bridge with plugin metadata.
         */
        void initialize() {
            // Create plugin metadata from Bukkit's plugin description
            var description = paperPlugin.getDescription();

            this.meta = new PluginMeta(
                    description.getName(),
                    description.getVersion(),
                    description.getDescription(),
                    description.getAuthors() != null ? description.getAuthors() : List.of(),
                    description.getWebsite(),
                    description.getDepend() != null ? description.getDepend() : List.of(),
                    description.getSoftDepend() != null ? description.getSoftDepend() : List.of(),
                    description.getAPIVersion() != null ? description.getAPIVersion() : "1.21"
            );

            // Initialize the unified plugin
            // Note: ServiceRegistry should be obtained from the global API instance
            // For now, we pass null and it should be set up by the main API plugin
            super.initialize(
                    meta,
                    paperPlugin.getLogger(),
                    paperPlugin.getDataFolder().toPath(),
                    PaperConversions.getUnifiedServer(),
                    PaperConversions.getServiceRegistry()
            );
        }

        /**
         * Returns the unified plugin metadata.
         *
         * <p>This method is named to avoid conflict with UnifiedPlugin.getMeta() which is final.
         *
         * @return the metadata
         */
        @NotNull
        PluginMeta getUnifiedPluginMeta() {
            return meta;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onEnable() {
            paperPlugin.onPluginEnable();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDisable() {
            paperPlugin.onPluginDisable();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReload() {
            paperPlugin.onPluginReload();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void saveResource(@NotNull String resourcePath, boolean replace) {
            if (resourcePath.isEmpty()) {
                throw new IllegalArgumentException("ResourcePath cannot be empty");
            }

            resourcePath = resourcePath.replace('\\', '/');
            Path outPath = paperPlugin.getDataFolder().toPath().resolve(resourcePath);

            if (!replace && Files.exists(outPath)) {
                return;
            }

            try (InputStream in = paperPlugin.getResource(resourcePath)) {
                if (in == null) {
                    throw new IllegalArgumentException(
                            "The resource '" + resourcePath + "' cannot be found in the plugin JAR"
                    );
                }

                // Create parent directories
                Files.createDirectories(outPath.getParent());

                // Copy resource
                try (OutputStream out = Files.newOutputStream(outPath)) {
                    in.transferTo(out);
                }
            } catch (IOException e) {
                paperPlugin.getLogger().log(
                        Level.SEVERE,
                        "Could not save resource " + resourcePath,
                        e
                );
            }
        }
    }
}
