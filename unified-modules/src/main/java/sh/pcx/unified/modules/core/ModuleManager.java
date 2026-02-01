/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.core;

import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.dependency.CircularDependencyException;
import sh.pcx.unified.modules.dependency.DependencyResolver;
import sh.pcx.unified.modules.health.HealthContext;
import sh.pcx.unified.modules.health.HealthStatus;
import sh.pcx.unified.modules.health.TPSTracker;
import sh.pcx.unified.modules.lifecycle.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for the module system lifecycle.
 *
 * <p>ModuleManager is the primary entry point for managing plugin modules.
 * It handles module discovery, dependency resolution, loading, enabling,
 * disabling, reloading, and health monitoring.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic module discovery via package scanning</li>
 *   <li>Dependency resolution with cycle detection</li>
 *   <li>Hot reload support without server restart</li>
 *   <li>Health monitoring with TPS-aware modules</li>
 *   <li>Config-driven enable/disable</li>
 *   <li>Guice dependency injection support</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *
 *     private ModuleManager modules;
 *
 *     @Override
 *     public void onEnable() {
 *         // Create module manager
 *         modules = ModuleManager.builder(this)
 *             .scanPackage("com.example.myplugin.modules")
 *             .enableHealthMonitoring(true)
 *             .healthThreshold(18.0)
 *             .recoveryThreshold(19.5)
 *             .configPath(getDataFolder().toPath().resolve("modules.yml"))
 *             .build();
 *
 *         // Register all discovered modules
 *         modules.registerAll();
 *
 *         // Or register specific modules
 *         modules.register(EconomyModule.class);
 *         modules.register(BattlePassModule.class);
 *
 *         // Access module instances
 *         EconomyModule economy = modules.get(EconomyModule.class);
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         modules.disableAll();
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ModuleRegistry
 * @see ModuleLoader
 * @see Module
 */
public final class ModuleManager {

    private final UnifiedPlugin plugin;
    private final Logger logger;
    private final ModuleRegistry registry;
    private final ModuleLoader loader;
    private final DependencyResolver dependencyResolver;
    private final TPSTracker tpsTracker;

    private final Set<String> packagesToScan;
    private final List<Class<?>> discoveredModules;
    private final Map<String, Boolean> moduleEnabledConfig;

    private final boolean healthMonitoringEnabled;
    private final double healthThreshold;
    private final double recoveryThreshold;
    private final Duration healthCheckInterval;
    private final Path configPath;

    private volatile boolean isHealthy;
    private volatile boolean initialized;

    /**
     * Creates a new ModuleManager via the builder.
     */
    private ModuleManager(Builder builder) {
        this.plugin = builder.plugin;
        this.logger = plugin.getLogger();
        this.registry = new ModuleRegistry();
        this.loader = new ModuleLoader(plugin, registry);
        this.dependencyResolver = new DependencyResolver();
        this.tpsTracker = new TPSTracker();

        this.packagesToScan = new HashSet<>(builder.packagesToScan);
        this.discoveredModules = new ArrayList<>();
        this.moduleEnabledConfig = new ConcurrentHashMap<>();

        this.healthMonitoringEnabled = builder.healthMonitoringEnabled;
        this.healthThreshold = builder.healthThreshold;
        this.recoveryThreshold = builder.recoveryThreshold;
        this.healthCheckInterval = builder.healthCheckInterval;
        this.configPath = builder.configPath;

        this.isHealthy = true;
        this.initialized = false;
    }

    /**
     * Creates a builder for ModuleManager.
     *
     * @param plugin the owning plugin
     * @return a new builder
     */
    @NotNull
    public static Builder builder(@NotNull UnifiedPlugin plugin) {
        return new Builder(plugin);
    }

    /**
     * Discovers and loads all modules from configured packages.
     *
     * @return this manager for chaining
     * @throws CircularDependencyException if circular dependencies are detected
     */
    @NotNull
    public ModuleManager registerAll() throws CircularDependencyException {
        if (initialized) {
            logger.warning("ModuleManager already initialized");
            return this;
        }

        logger.info("Discovering modules...");

        // Scan packages for modules
        for (String packageName : packagesToScan) {
            List<Class<?>> found = loader.scanPackage(packageName);
            discoveredModules.addAll(found);
        }

        // Load config to check enabled/disabled status
        loadModuleConfig();

        // Add to dependency resolver
        for (Class<?> moduleClass : discoveredModules) {
            Module annotation = moduleClass.getAnnotation(Module.class);
            if (annotation != null) {
                dependencyResolver.addModule(annotation.name(), moduleClass);
            }
        }

        // Resolve dependencies
        DependencyResolver.ResolutionResult result = dependencyResolver.resolve();
        if (!result.isSuccess()) {
            for (String error : result.getErrors()) {
                logger.severe(error);
            }
            if (result.hasCycle()) {
                throw new CircularDependencyException(result.getCycle());
            }
            return this;
        }

        // Load modules in resolved order
        List<String> loadOrder = result.getLoadOrder();
        logger.info("Loading " + loadOrder.size() + " modules...");

        for (String moduleName : loadOrder) {
            Class<?> moduleClass = findModuleClass(moduleName);
            if (moduleClass != null && isModuleEnabled(moduleName)) {
                loadAndEnable(moduleClass);
            } else if (moduleClass != null) {
                logger.info("Module '" + moduleName + "' is disabled in config");
            }
        }

        initialized = true;
        return this;
    }

    /**
     * Registers and enables a specific module.
     *
     * @param moduleClass the module class
     * @return this manager for chaining
     */
    @NotNull
    public ModuleManager register(@NotNull Class<?> moduleClass) {
        loadAndEnable(moduleClass);
        return this;
    }

    /**
     * Loads and enables a module.
     */
    private void loadAndEnable(Class<?> moduleClass) {
        Module annotation = moduleClass.getAnnotation(Module.class);
        if (annotation == null) {
            return;
        }

        String name = annotation.name();

        // Load the module
        Optional<Object> loaded = loader.load(moduleClass);
        if (loaded.isEmpty()) {
            return;
        }

        Object module = loaded.get();

        try {
            // Create context
            Path moduleDataFolder = plugin.getDataFolder().resolve("modules").resolve(name);
            ModuleContext context = new ModuleContext(name, plugin, registry, moduleDataFolder);

            // Initialize if applicable
            if (module instanceof Initializable) {
                ((Initializable) module).init(context);
            }

            // Register listeners if applicable
            // (Platform-specific implementation would handle actual registration)

            // Register commands if applicable
            // (Platform-specific implementation would handle actual registration)

            // Register scheduled tasks if applicable
            if (module instanceof Schedulable) {
                List<ScheduledTask> tasks = ((Schedulable) module).getTasks();
                // (Platform-specific implementation would schedule tasks)
            }

            // Mark as enabled
            registry.setState(name, ModuleState.ENABLED);
            registry.getEntry(name).ifPresent(entry -> entry.setEnableTime(System.currentTimeMillis()));

            logger.info("Enabled module: " + name);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to enable module: " + name, e);
            registry.setState(name, ModuleState.FAILED);
            registry.setError(name, e.getMessage());
        }
    }

    /**
     * Gets a module instance by class.
     *
     * @param moduleClass the module class
     * @param <T>         the module type
     * @return the module instance, or null if not found/enabled
     */
    public <T> T get(@NotNull Class<T> moduleClass) {
        return registry.get(moduleClass).orElse(null);
    }

    /**
     * Gets a module instance by name.
     *
     * @param name the module name
     * @return an Optional containing the module if found
     */
    @NotNull
    public Optional<Object> get(@NotNull String name) {
        return registry.get(name);
    }

    /**
     * Checks if a module is enabled.
     *
     * @param name the module name
     * @return {@code true} if the module is enabled
     */
    public boolean isEnabled(@NotNull String name) {
        return registry.isEnabled(name);
    }

    /**
     * Enables a disabled module.
     *
     * @param name the module name
     * @return {@code true} if successfully enabled
     */
    public boolean enable(@NotNull String name) {
        ModuleState state = registry.getState(name);
        if (!state.canEnable()) {
            logger.warning("Cannot enable module '" + name + "' from state: " + state);
            return false;
        }

        Optional<ModuleRegistry.ModuleEntry> entryOpt = registry.getEntry(name);
        if (entryOpt.isEmpty()) {
            logger.warning("Module not found: " + name);
            return false;
        }

        ModuleRegistry.ModuleEntry entry = entryOpt.get();
        Object module = entry.getInstance();

        try {
            Path moduleDataFolder = plugin.getDataFolder().resolve("modules").resolve(name);
            ModuleContext context = new ModuleContext(name, plugin, registry, moduleDataFolder);

            if (module instanceof Initializable) {
                ((Initializable) module).init(context);
            }

            registry.setState(name, ModuleState.ENABLED);
            entry.setEnableTime(System.currentTimeMillis());

            logger.info("Enabled module: " + name);
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to enable module: " + name, e);
            registry.setState(name, ModuleState.FAILED);
            registry.setError(name, e.getMessage());
            return false;
        }
    }

    /**
     * Disables an enabled module.
     *
     * @param name the module name
     * @return {@code true} if successfully disabled
     */
    public boolean disable(@NotNull String name) {
        ModuleState state = registry.getState(name);
        if (!state.canDisable()) {
            logger.warning("Cannot disable module '" + name + "' from state: " + state);
            return false;
        }

        Optional<ModuleRegistry.ModuleEntry> entryOpt = registry.getEntry(name);
        if (entryOpt.isEmpty()) {
            return false;
        }

        ModuleRegistry.ModuleEntry entry = entryOpt.get();
        Object module = entry.getInstance();

        try {
            Path moduleDataFolder = plugin.getDataFolder().resolve("modules").resolve(name);
            ModuleContext context = new ModuleContext(name, plugin, registry, moduleDataFolder);

            if (module instanceof Disableable) {
                ((Disableable) module).onDisable(context);
            }

            // Cancel tasks, unregister listeners/commands
            // (Platform-specific implementation)

            registry.setState(name, ModuleState.DISABLED);
            logger.info("Disabled module: " + name);
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error disabling module: " + name, e);
            return false;
        }
    }

    /**
     * Reloads a specific module.
     *
     * @param name the module name
     * @return {@code true} if successfully reloaded
     */
    public boolean reload(@NotNull String name) {
        ModuleState state = registry.getState(name);
        if (!state.canReload()) {
            logger.warning("Cannot reload module '" + name + "' from state: " + state);
            return false;
        }

        Optional<ModuleRegistry.ModuleEntry> entryOpt = registry.getEntry(name);
        if (entryOpt.isEmpty()) {
            return false;
        }

        ModuleRegistry.ModuleEntry entry = entryOpt.get();
        Object module = entry.getInstance();

        if (!(module instanceof Reloadable)) {
            logger.warning("Module '" + name + "' does not support reloading");
            return false;
        }

        try {
            Path moduleDataFolder = plugin.getDataFolder().resolve("modules").resolve(name);
            ModuleContext context = new ModuleContext(name, plugin, registry, moduleDataFolder);

            ((Reloadable) module).reload(context);

            logger.info("Reloaded module: " + name);
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to reload module: " + name, e);
            return false;
        }
    }

    /**
     * Reloads all reloadable modules.
     */
    public void reloadAll() {
        for (String name : registry.getNames()) {
            if (registry.getState(name).canReload()) {
                reload(name);
            }
        }
    }

    /**
     * Disables all enabled modules.
     */
    public void disableAll() {
        // Disable in reverse order
        List<String> names = new ArrayList<>(registry.getNames());
        Collections.reverse(names);

        for (String name : names) {
            if (registry.getState(name) == ModuleState.ENABLED) {
                disable(name);
            }
        }
    }

    /**
     * Gets the status of a module.
     *
     * @param name the module name
     * @return the module status
     */
    @NotNull
    public ModuleStatus getStatus(@NotNull String name) {
        Optional<ModuleRegistry.ModuleEntry> entryOpt = registry.getEntry(name);
        if (entryOpt.isEmpty()) {
            return new ModuleStatus(name, ModuleState.UNLOADED, 0, 0, null);
        }

        ModuleRegistry.ModuleEntry entry = entryOpt.get();
        return new ModuleStatus(
                name,
                entry.getState(),
                entry.getLoadTime(),
                entry.getEnableTime(),
                entry.getErrorMessage()
        );
    }

    /**
     * Gets info for all modules.
     *
     * @return list of module info
     */
    @NotNull
    public List<ModuleInfo> getAllModules() {
        List<ModuleInfo> infos = new ArrayList<>();
        for (ModuleRegistry.ModuleEntry entry : registry.getAllEntries()) {
            Module annotation = entry.getAnnotation();
            infos.add(new ModuleInfo(
                    entry.getName(),
                    annotation != null ? annotation.description() : "",
                    annotation != null ? annotation.version() : "1.0.0",
                    entry.getState().isActive(),
                    entry.getState()
            ));
        }
        return infos;
    }

    /**
     * Notifies health-aware modules of TPS change.
     */
    public void notifyHealthChange(boolean healthy) {
        if (healthy == this.isHealthy) {
            return;
        }

        this.isHealthy = healthy;
        double currentTps = tpsTracker.getCurrentTps();

        for (ModuleRegistry.ModuleEntry entry : registry.getEntriesByState(ModuleState.ENABLED)) {
            Object module = entry.getInstance();
            if (module instanceof Healthy) {
                try {
                    Path moduleDataFolder = plugin.getDataFolder().resolve("modules").resolve(entry.getName());
                    HealthContext context = new HealthContext(
                            currentTps,
                            healthThreshold,
                            recoveryThreshold,
                            new ModuleContext(entry.getName(), plugin, registry, moduleDataFolder).getLogger(),
                            tpsTracker
                    );

                    if (healthy) {
                        ((Healthy) module).ifBackToHealth(context);
                    } else {
                        ((Healthy) module).ifUnhealthy(context);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error notifying module of health change: " + entry.getName(), e);
                }
            }
        }
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

    /**
     * Returns the TPS tracker.
     *
     * @return the TPS tracker
     */
    @NotNull
    public TPSTracker getTpsTracker() {
        return tpsTracker;
    }

    /**
     * Returns whether health monitoring is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isHealthMonitoringEnabled() {
        return healthMonitoringEnabled;
    }

    /**
     * Returns the health threshold.
     *
     * @return the TPS threshold
     */
    public double getHealthThreshold() {
        return healthThreshold;
    }

    /**
     * Finds a module class by name.
     */
    private Class<?> findModuleClass(String name) {
        for (Class<?> moduleClass : discoveredModules) {
            Module annotation = moduleClass.getAnnotation(Module.class);
            if (annotation != null && annotation.name().equals(name)) {
                return moduleClass;
            }
        }
        return null;
    }

    /**
     * Loads module enable/disable config.
     */
    private void loadModuleConfig() {
        // Would load from configPath
        // For now, enable all by default
    }

    /**
     * Checks if a module is enabled in config.
     */
    private boolean isModuleEnabled(String name) {
        return moduleEnabledConfig.getOrDefault(name, true);
    }

    /**
     * Record for module status.
     */
    public record ModuleStatus(
            String name,
            ModuleState state,
            long loadTime,
            long enableTime,
            String error
    ) {
        public boolean isEnabled() {
            return state == ModuleState.ENABLED;
        }

        public boolean isHealthy() {
            return state != ModuleState.FAILED;
        }
    }

    /**
     * Record for module info display.
     */
    public record ModuleInfo(
            String name,
            String description,
            String version,
            boolean enabled,
            ModuleState state
    ) {
    }

    /**
     * Builder for ModuleManager.
     */
    public static final class Builder {
        private final UnifiedPlugin plugin;
        private final Set<String> packagesToScan = new HashSet<>();
        private boolean healthMonitoringEnabled = false;
        private double healthThreshold = 18.0;
        private double recoveryThreshold = 19.5;
        private Duration healthCheckInterval = Duration.ofSeconds(5);
        private Path configPath;

        private Builder(UnifiedPlugin plugin) {
            this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
            this.configPath = plugin.getDataFolder().resolve("modules.yml");
        }

        /**
         * Adds a package to scan for modules.
         *
         * @param packageName the package name
         * @return this builder
         */
        @NotNull
        public Builder scanPackage(@NotNull String packageName) {
            packagesToScan.add(packageName);
            return this;
        }

        /**
         * Enables or disables health monitoring.
         *
         * @param enabled whether to enable health monitoring
         * @return this builder
         */
        @NotNull
        public Builder enableHealthMonitoring(boolean enabled) {
            this.healthMonitoringEnabled = enabled;
            return this;
        }

        /**
         * Sets the TPS threshold for unhealthy state.
         *
         * @param threshold the TPS threshold
         * @return this builder
         */
        @NotNull
        public Builder healthThreshold(double threshold) {
            this.healthThreshold = threshold;
            return this;
        }

        /**
         * Sets the TPS threshold for healthy state recovery.
         *
         * @param threshold the recovery threshold
         * @return this builder
         */
        @NotNull
        public Builder recoveryThreshold(double threshold) {
            this.recoveryThreshold = threshold;
            return this;
        }

        /**
         * Sets the health check interval.
         *
         * @param interval the check interval
         * @return this builder
         */
        @NotNull
        public Builder checkInterval(@NotNull Duration interval) {
            this.healthCheckInterval = interval;
            return this;
        }

        /**
         * Sets the path to the modules configuration file.
         *
         * @param path the config file path
         * @return this builder
         */
        @NotNull
        public Builder configPath(@NotNull Path path) {
            this.configPath = path;
            return this;
        }

        /**
         * Builds the ModuleManager.
         *
         * @return the configured ModuleManager
         */
        @NotNull
        public ModuleManager build() {
            return new ModuleManager(this);
        }
    }
}
