/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.core;

import sh.pcx.unified.UnifiedPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Context passed to modules during lifecycle events.
 *
 * <p>The ModuleContext provides modules with access to plugin services,
 * configuration loading, logging, and other utilities needed during
 * initialization, reload, and disable operations.
 *
 * <h2>Provided Services</h2>
 * <ul>
 *   <li>Plugin reference and data folder access</li>
 *   <li>Module-specific logger with prefixed name</li>
 *   <li>Configuration loading and saving</li>
 *   <li>Access to other modules via the registry</li>
 *   <li>Scheduler access for task registration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * public class BattlePassModule implements Initializable, Reloadable {
 *
 *     private BattlePassConfig config;
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         // Load configuration
 *         this.config = context.loadConfig(BattlePassConfig.class);
 *
 *         // Log initialization
 *         context.getLogger().info("Loaded configuration for season " + config.getSeason());
 *
 *         // Access data folder
 *         Path dataPath = context.getModuleDataFolder();
 *
 *         // Get another module (if needed)
 *         Optional<EconomyModule> economy = context.getModule(EconomyModule.class);
 *     }
 *
 *     @Override
 *     public void reload(ModuleContext context) {
 *         // Reload configuration
 *         this.config = context.loadConfig(BattlePassConfig.class);
 *         context.getLogger().info("Configuration reloaded");
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.modules.lifecycle.Initializable
 * @see sh.pcx.unified.modules.lifecycle.Reloadable
 */
public final class ModuleContext {

    private final String moduleName;
    private final UnifiedPlugin plugin;
    private final ModuleRegistry registry;
    private final Logger logger;
    private final Path moduleDataFolder;

    /**
     * Creates a new module context.
     *
     * @param moduleName       the name of the module
     * @param plugin           the owning plugin
     * @param registry         the module registry
     * @param moduleDataFolder the module's data folder
     */
    public ModuleContext(
            @NotNull String moduleName,
            @NotNull UnifiedPlugin plugin,
            @NotNull ModuleRegistry registry,
            @NotNull Path moduleDataFolder
    ) {
        this.moduleName = Objects.requireNonNull(moduleName, "Module name cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
        this.moduleDataFolder = Objects.requireNonNull(moduleDataFolder, "Data folder cannot be null");
        this.logger = new ModuleLogger(plugin.getLogger(), moduleName);
    }

    /**
     * Returns the name of the module.
     *
     * @return the module name
     */
    @NotNull
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns the owning plugin.
     *
     * @return the plugin instance
     */
    @NotNull
    public UnifiedPlugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the module's data folder.
     *
     * <p>This is a subfolder within the plugin's data folder specifically
     * for this module's data and configuration files.
     *
     * @return the module data folder path
     */
    @NotNull
    public Path getModuleDataFolder() {
        return moduleDataFolder;
    }

    /**
     * Returns the plugin's main data folder.
     *
     * @return the plugin data folder path
     */
    @NotNull
    public Path getPluginDataFolder() {
        return plugin.getDataFolder();
    }

    /**
     * Returns the logger for this module.
     *
     * <p>The logger prefixes all messages with the module name for
     * easy identification in console output.
     *
     * @return the module logger
     */
    @NotNull
    public Logger getLogger() {
        return logger;
    }

    /**
     * Loads a configuration class from the module's config section.
     *
     * <p>The configuration is loaded from the modules.yml file under
     * this module's config section, or from a separate file if the
     * module uses non-embedded configuration.
     *
     * @param configClass the configuration class to load
     * @param <T>         the configuration type
     * @return the loaded configuration instance
     */
    @NotNull
    public <T> T loadConfig(@NotNull Class<T> configClass) {
        // This would be implemented with the unified-config module
        // For now, return a new instance using default constructor
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config class: " + configClass.getName(), e);
        }
    }

    /**
     * Saves the module's configuration to disk.
     *
     * @param config the configuration object to save
     * @param <T>    the configuration type
     */
    public <T> void saveConfig(@NotNull T config) {
        // Implementation would use unified-config module
        getLogger().fine("Saving configuration for module " + moduleName);
    }

    /**
     * Gets another module instance from the registry.
     *
     * @param moduleClass the class of the module to get
     * @param <T>         the module type
     * @return an Optional containing the module if found and enabled
     */
    @NotNull
    public <T> Optional<T> getModule(@NotNull Class<T> moduleClass) {
        return registry.get(moduleClass);
    }

    /**
     * Gets another module instance by name from the registry.
     *
     * @param name the name of the module
     * @return an Optional containing the module if found and enabled
     */
    @NotNull
    public Optional<Object> getModule(@NotNull String name) {
        return registry.get(name);
    }

    /**
     * Checks if another module is enabled.
     *
     * @param moduleName the name of the module to check
     * @return {@code true} if the module is enabled
     */
    public boolean isModuleEnabled(@NotNull String moduleName) {
        return registry.isEnabled(moduleName);
    }

    /**
     * Returns the module registry.
     *
     * @return the registry
     */
    @NotNull
    public ModuleRegistry getRegistry() {
        return registry;
    }

    @Override
    public String toString() {
        return "ModuleContext{" +
                "moduleName='" + moduleName + '\'' +
                ", plugin=" + plugin.getName() +
                '}';
    }

    /**
     * Logger wrapper that prefixes messages with module name.
     */
    private static class ModuleLogger extends Logger {
        private final Logger parent;
        private final String prefix;

        ModuleLogger(Logger parent, String moduleName) {
            super(parent.getName() + "." + moduleName, null);
            this.parent = parent;
            this.prefix = "[" + moduleName + "] ";
            setParent(parent);
            setLevel(parent.getLevel());
        }

        @Override
        public void info(@Nullable String msg) {
            parent.info(prefix + msg);
        }

        @Override
        public void warning(@Nullable String msg) {
            parent.warning(prefix + msg);
        }

        @Override
        public void severe(@Nullable String msg) {
            parent.severe(prefix + msg);
        }

        @Override
        public void fine(@Nullable String msg) {
            parent.fine(prefix + msg);
        }

        @Override
        public void finer(@Nullable String msg) {
            parent.finer(prefix + msg);
        }

        @Override
        public void finest(@Nullable String msg) {
            parent.finest(prefix + msg);
        }
    }
}
