/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.core;

import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.lifecycle.ModuleState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry that stores and manages registered modules.
 *
 * <p>The ModuleRegistry maintains the collection of all registered modules,
 * their instances, states, and metadata. It provides thread-safe access
 * to module lookup and state queries.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Thread-safe module storage</li>
 *   <li>Lookup by name or class</li>
 *   <li>State tracking for all modules</li>
 *   <li>Iteration over modules by state</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ModuleRegistry registry = new ModuleRegistry();
 *
 * // Register a module
 * registry.register("Economy", economyModule, EconomyModule.class);
 *
 * // Lookup by name
 * Optional<Object> module = registry.get("Economy");
 *
 * // Lookup by class
 * Optional<EconomyModule> economy = registry.get(EconomyModule.class);
 *
 * // Check state
 * ModuleState state = registry.getState("Economy");
 *
 * // Get all enabled modules
 * List<Object> enabled = registry.getByState(ModuleState.ENABLED);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ModuleManager
 * @see ModuleState
 */
public final class ModuleRegistry {

    /**
     * Map from module name to entry.
     */
    private final Map<String, ModuleEntry> modulesByName;

    /**
     * Map from module class to name for reverse lookup.
     */
    private final Map<Class<?>, String> modulesByClass;

    /**
     * Creates a new module registry.
     */
    public ModuleRegistry() {
        this.modulesByName = new ConcurrentHashMap<>();
        this.modulesByClass = new ConcurrentHashMap<>();
    }

    /**
     * Registers a module with the registry.
     *
     * @param name        the module name
     * @param instance    the module instance
     * @param moduleClass the module class
     */
    public void register(
            @NotNull String name,
            @NotNull Object instance,
            @NotNull Class<?> moduleClass
    ) {
        Objects.requireNonNull(name, "Module name cannot be null");
        Objects.requireNonNull(instance, "Module instance cannot be null");
        Objects.requireNonNull(moduleClass, "Module class cannot be null");

        Module annotation = moduleClass.getAnnotation(Module.class);
        ModuleEntry entry = new ModuleEntry(name, instance, moduleClass, annotation);

        modulesByName.put(name, entry);
        modulesByClass.put(moduleClass, name);
    }

    /**
     * Unregisters a module from the registry.
     *
     * @param name the module name
     */
    public void unregister(@NotNull String name) {
        ModuleEntry entry = modulesByName.remove(name);
        if (entry != null) {
            modulesByClass.remove(entry.moduleClass);
        }
    }

    /**
     * Gets a module by name.
     *
     * @param name the module name
     * @return an Optional containing the module instance if found
     */
    @NotNull
    public Optional<Object> get(@NotNull String name) {
        ModuleEntry entry = modulesByName.get(name);
        return entry != null ? Optional.of(entry.instance) : Optional.empty();
    }

    /**
     * Gets a module by class.
     *
     * @param moduleClass the module class
     * @param <T>         the module type
     * @return an Optional containing the module instance if found
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull Class<T> moduleClass) {
        String name = modulesByClass.get(moduleClass);
        if (name == null) {
            return Optional.empty();
        }
        ModuleEntry entry = modulesByName.get(name);
        return entry != null ? Optional.of((T) entry.instance) : Optional.empty();
    }

    /**
     * Gets the entry for a module.
     *
     * @param name the module name
     * @return an Optional containing the entry if found
     */
    @NotNull
    public Optional<ModuleEntry> getEntry(@NotNull String name) {
        return Optional.ofNullable(modulesByName.get(name));
    }

    /**
     * Returns whether a module is registered.
     *
     * @param name the module name
     * @return {@code true} if registered
     */
    public boolean contains(@NotNull String name) {
        return modulesByName.containsKey(name);
    }

    /**
     * Returns whether a module is registered by class.
     *
     * @param moduleClass the module class
     * @return {@code true} if registered
     */
    public boolean contains(@NotNull Class<?> moduleClass) {
        return modulesByClass.containsKey(moduleClass);
    }

    /**
     * Returns the state of a module.
     *
     * @param name the module name
     * @return the module state, or UNLOADED if not found
     */
    @NotNull
    public ModuleState getState(@NotNull String name) {
        ModuleEntry entry = modulesByName.get(name);
        return entry != null ? entry.state : ModuleState.UNLOADED;
    }

    /**
     * Sets the state of a module.
     *
     * @param name  the module name
     * @param state the new state
     */
    public void setState(@NotNull String name, @NotNull ModuleState state) {
        ModuleEntry entry = modulesByName.get(name);
        if (entry != null) {
            entry.state = state;
        }
    }

    /**
     * Returns whether a module is enabled.
     *
     * @param name the module name
     * @return {@code true} if the module is in ENABLED state
     */
    public boolean isEnabled(@NotNull String name) {
        return getState(name) == ModuleState.ENABLED;
    }

    /**
     * Returns all registered module names.
     *
     * @return an unmodifiable set of module names
     */
    @NotNull
    public Set<String> getNames() {
        return Collections.unmodifiableSet(modulesByName.keySet());
    }

    /**
     * Returns all registered modules.
     *
     * @return an unmodifiable collection of module instances
     */
    @NotNull
    public Collection<Object> getAll() {
        return modulesByName.values().stream()
                .map(e -> e.instance)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all module entries.
     *
     * @return an unmodifiable collection of module entries
     */
    @NotNull
    public Collection<ModuleEntry> getAllEntries() {
        return Collections.unmodifiableCollection(modulesByName.values());
    }

    /**
     * Returns modules in a specific state.
     *
     * @param state the state to filter by
     * @return a list of module instances in that state
     */
    @NotNull
    public List<Object> getByState(@NotNull ModuleState state) {
        return modulesByName.values().stream()
                .filter(e -> e.state == state)
                .map(e -> e.instance)
                .collect(Collectors.toList());
    }

    /**
     * Returns module entries in a specific state.
     *
     * @param state the state to filter by
     * @return a list of module entries in that state
     */
    @NotNull
    public List<ModuleEntry> getEntriesByState(@NotNull ModuleState state) {
        return modulesByName.values().stream()
                .filter(e -> e.state == state)
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of registered modules.
     *
     * @return the module count
     */
    public int size() {
        return modulesByName.size();
    }

    /**
     * Returns whether the registry is empty.
     *
     * @return {@code true} if no modules are registered
     */
    public boolean isEmpty() {
        return modulesByName.isEmpty();
    }

    /**
     * Clears all modules from the registry.
     */
    public void clear() {
        modulesByName.clear();
        modulesByClass.clear();
    }

    /**
     * Gets error message for a failed module.
     *
     * @param name the module name
     * @return the error message, or null if no error
     */
    public String getError(@NotNull String name) {
        ModuleEntry entry = modulesByName.get(name);
        return entry != null ? entry.errorMessage : null;
    }

    /**
     * Sets error message for a failed module.
     *
     * @param name    the module name
     * @param message the error message
     */
    public void setError(@NotNull String name, String message) {
        ModuleEntry entry = modulesByName.get(name);
        if (entry != null) {
            entry.errorMessage = message;
        }
    }

    /**
     * Entry storing module instance and metadata.
     */
    public static final class ModuleEntry {
        private final String name;
        private final Object instance;
        private final Class<?> moduleClass;
        private final Module annotation;
        private volatile ModuleState state;
        private volatile String errorMessage;
        private volatile long loadTime;
        private volatile long enableTime;

        /**
         * Creates a module entry.
         *
         * @param name        the module name
         * @param instance    the module instance
         * @param moduleClass the module class
         * @param annotation  the @Module annotation
         */
        public ModuleEntry(
                String name,
                Object instance,
                Class<?> moduleClass,
                Module annotation
        ) {
            this.name = name;
            this.instance = instance;
            this.moduleClass = moduleClass;
            this.annotation = annotation;
            this.state = ModuleState.UNLOADED;
        }

        /**
         * Returns the module name.
         *
         * @return the name
         */
        @NotNull
        public String getName() {
            return name;
        }

        /**
         * Returns the module instance.
         *
         * @return the instance
         */
        @NotNull
        public Object getInstance() {
            return instance;
        }

        /**
         * Returns the module class.
         *
         * @return the class
         */
        @NotNull
        public Class<?> getModuleClass() {
            return moduleClass;
        }

        /**
         * Returns the @Module annotation.
         *
         * @return the annotation, or null if not present
         */
        public Module getAnnotation() {
            return annotation;
        }

        /**
         * Returns the current state.
         *
         * @return the state
         */
        @NotNull
        public ModuleState getState() {
            return state;
        }

        /**
         * Sets the current state.
         *
         * @param state the new state
         */
        public void setState(@NotNull ModuleState state) {
            this.state = state;
        }

        /**
         * Returns the error message if the module failed.
         *
         * @return the error message, or null
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * Returns the time taken to load the module in milliseconds.
         *
         * @return the load time
         */
        public long getLoadTime() {
            return loadTime;
        }

        /**
         * Sets the load time.
         *
         * @param loadTime the load time in milliseconds
         */
        public void setLoadTime(long loadTime) {
            this.loadTime = loadTime;
        }

        /**
         * Returns the timestamp when the module was enabled.
         *
         * @return the enable timestamp
         */
        public long getEnableTime() {
            return enableTime;
        }

        /**
         * Sets the enable timestamp.
         *
         * @param enableTime the enable timestamp
         */
        public void setEnableTime(long enableTime) {
            this.enableTime = enableTime;
        }

        @Override
        public String toString() {
            return "ModuleEntry{" +
                    "name='" + name + '\'' +
                    ", state=" + state +
                    ", class=" + moduleClass.getSimpleName() +
                    '}';
        }
    }
}
