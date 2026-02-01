/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.PluginMeta;
import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.platform.PlatformProvider;
import sh.pcx.unified.service.ServiceRegistry;
import sh.pcx.unified.service.SimpleServiceRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

/**
 * JavaPlugin subclass that bridges Bukkit's plugin lifecycle to {@link UnifiedPlugin}.
 *
 * <p>This class serves as the base class for plugins that want to use the UnifiedPlugin API
 * on Bukkit-based servers (Spigot, Paper, Folia). It handles the translation between Bukkit's
 * plugin lifecycle events and the unified lifecycle methods.
 *
 * <h2>Usage</h2>
 * <p>Plugins should extend this class instead of {@link JavaPlugin}:
 * <pre>{@code
 * public class MyPlugin extends BukkitUnifiedPlugin {
 *
 *     @Override
 *     public void onPluginEnable() {
 *         getLogger().info("Plugin enabled!");
 *         // Register commands, listeners, etc.
 *     }
 *
 *     @Override
 *     public void onPluginDisable() {
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
public abstract class BukkitUnifiedPlugin extends JavaPlugin {

    private final UnifiedPluginBridge unifiedBridge;

    /**
     * Creates a new BukkitUnifiedPlugin.
     *
     * <p>This constructor initializes the unified plugin bridge that handles
     * lifecycle management.
     *
     * @since 1.0.0
     */
    public BukkitUnifiedPlugin() {
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
     * Internal bridge class that wraps the BukkitUnifiedPlugin as a UnifiedPlugin.
     *
     * <p>This class handles the initialization and lifecycle of the unified plugin
     * abstraction layer.
     */
    public static final class UnifiedPluginBridge extends UnifiedPlugin {

        private final BukkitUnifiedPlugin bukkitPlugin;
        private PluginMeta meta;

        /**
         * Creates a new bridge for the given Bukkit plugin.
         *
         * @param bukkitPlugin the Bukkit plugin to wrap
         */
        UnifiedPluginBridge(@NotNull BukkitUnifiedPlugin bukkitPlugin) {
            this.bukkitPlugin = bukkitPlugin;
        }

        /**
         * Initializes the bridge with plugin metadata.
         */
        void initialize() {
            // Bootstrap platform if not already initialized
            ensurePlatformInitialized();

            // Create plugin metadata from Bukkit's plugin description
            var description = bukkitPlugin.getDescription();

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
            super.initialize(
                    meta,
                    bukkitPlugin.getLogger(),
                    bukkitPlugin.getDataFolder().toPath(),
                    PaperConversions.getUnifiedServer(),
                    PaperConversions.getServiceRegistry()
            );
        }

        /**
         * Ensures the platform is initialized, bootstrapping if necessary.
         * This allows the first UnifiedPlugin to load the platform.
         */
        private void ensurePlatformInitialized() {
            if (PaperConversions.getProvider() != null) {
                // Already initialized
                return;
            }

            bukkitPlugin.getLogger().info("Bootstrapping Unified platform...");

            try {
                // Find compatible provider using ServiceLoader
                PlatformProvider provider = findCompatibleProvider();
                if (provider == null) {
                    throw new IllegalStateException("No compatible PlatformProvider found");
                }

                bukkitPlugin.getLogger().info("Using platform provider: " + provider.getClass().getSimpleName());

                // Initialize the provider
                provider.initialize();

                // Set up service registry if not already set
                if (!isServiceRegistryInitialized()) {
                    ServiceRegistry registry = new SimpleServiceRegistry();
                    PaperConversions.setServiceRegistry(registry);
                }

                bukkitPlugin.getLogger().info("Platform bootstrapped successfully");
            } catch (Exception e) {
                throw new RuntimeException("Failed to bootstrap platform", e);
            }
        }

        /**
         * Finds a compatible platform provider using ServiceLoader.
         */
        private PlatformProvider findCompatibleProvider() {
            ServiceLoader<PlatformProvider> loader = ServiceLoader.load(
                    PlatformProvider.class,
                    getClass().getClassLoader()
            );

            PlatformProvider best = null;
            int bestPriority = Integer.MIN_VALUE;

            for (PlatformProvider provider : loader) {
                try {
                    if (provider.isCompatible() && provider.getPriority() > bestPriority) {
                        best = provider;
                        bestPriority = provider.getPriority();
                    }
                } catch (Exception e) {
                    bukkitPlugin.getLogger().log(Level.WARNING,
                            "Error checking compatibility for provider: " + provider.getClass().getName(), e);
                }
            }

            return best;
        }

        /**
         * Checks if service registry is initialized.
         */
        private boolean isServiceRegistryInitialized() {
            try {
                PaperConversions.getServiceRegistry();
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
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
            bukkitPlugin.onPluginEnable();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDisable() {
            bukkitPlugin.onPluginDisable();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReload() {
            bukkitPlugin.onPluginReload();
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
            Path outPath = bukkitPlugin.getDataFolder().toPath().resolve(resourcePath);

            if (!replace && Files.exists(outPath)) {
                return;
            }

            try (InputStream in = bukkitPlugin.getResource(resourcePath)) {
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
                bukkitPlugin.getLogger().log(
                        Level.SEVERE,
                        "Could not save resource " + resourcePath,
                        e
                );
            }
        }
    }
}
