/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject.scope;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guice {@link Scope} implementation for plugin-scoped instances.
 *
 * <p>This scope maintains a separate instance of each bound type for each plugin.
 * Unlike {@link com.google.inject.Scopes#SINGLETON}, which creates one instance per
 * injector, PluginScope creates one instance per plugin, allowing multiple plugins
 * to have isolated instances of shared service types.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is fully thread-safe. Multiple threads can safely request
 * and create instances for different plugins concurrently.</p>
 *
 * <h2>Plugin Identification</h2>
 * <p>Plugins are identified by their name string, which must be unique across
 * all loaded plugins.</p>
 *
 * <h2>Scope Entry/Exit</h2>
 * <p>The plugin scope is typically entered automatically when processing
 * plugin-related operations. For cross-plugin access, use {@link ScopeManager}.</p>
 *
 * <pre>{@code
 * // Manual scope management (usually handled by framework)
 * try (ScopeContext context = scopeManager.enterPlugin(plugin)) {
 *     PluginCache cache = injector.getInstance(PluginCache.class);
 *     // Use cache...
 * }
 * }</pre>
 *
 * <h2>Instance Lifecycle</h2>
 * <ul>
 *   <li>Instances are created on first access within the scope</li>
 *   <li>Same instance is returned for all requests within the same plugin context</li>
 *   <li>Instances are destroyed when {@link #exitScope(String)} is called (on plugin disable)</li>
 *   <li>{@link sh.pcx.unified.inject.PreDestroy} methods are invoked before destruction</li>
 * </ul>
 *
 * <h2>Comparison with @Singleton</h2>
 * <table border="1">
 *   <tr><th>Aspect</th><th>@Singleton</th><th>@PluginScoped</th></tr>
 *   <tr><td>Scope</td><td>Per Injector</td><td>Per Plugin</td></tr>
 *   <tr><td>Isolation</td><td>Shared globally</td><td>Isolated per plugin</td></tr>
 *   <tr><td>Use Case</td><td>True singletons</td><td>Plugin-specific singletons</td></tr>
 * </table>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In module configuration
 * public class MyModule extends AbstractModule {
 *     @Override
 *     protected void configure() {
 *         PluginScope pluginScope = new PluginScope();
 *         bindScope(PluginScoped.class, pluginScope);
 *         bind(PluginScope.class).toInstance(pluginScope);
 *
 *         // Bind plugin-scoped services
 *         bind(PluginCache.class).in(PluginScoped.class);
 *         bind(PluginLogger.class).in(PluginScoped.class);
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.inject.PluginScoped
 * @see ScopeManager
 */
public class PluginScope implements Scope {

    /**
     * ThreadLocal holding the current plugin name for scope resolution.
     */
    private static final ThreadLocal<String> CURRENT_PLUGIN = new ThreadLocal<>();

    /**
     * Storage for plugin-scoped instances.
     * Outer map: Plugin name -> (Key -> Instance)
     */
    private final Map<String, Map<Key<?>, Object>> pluginInstances = new ConcurrentHashMap<>();

    /**
     * Creates a new PluginScope instance.
     */
    public PluginScope() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a provider that resolves instances within the current plugin scope.
     * The provider checks for an existing instance and creates one if necessary.</p>
     *
     * @param key the binding key for the type
     * @param unscoped the unscoped provider for creating new instances
     * @param <T> the type being scoped
     * @return a scoped provider
     */
    @Override
    public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
        return () -> {
            String pluginName = CURRENT_PLUGIN.get();
            if (pluginName == null) {
                throw new OutOfScopeException(
                    "Cannot access plugin-scoped binding outside of plugin scope. " +
                    "Ensure you are within a plugin context using ScopeManager.enterPlugin(). " +
                    "Requested key: " + key
                );
            }

            Map<Key<?>, Object> instances = pluginInstances.computeIfAbsent(
                pluginName,
                k -> new ConcurrentHashMap<>()
            );

            @SuppressWarnings("unchecked")
            T instance = (T) instances.computeIfAbsent(key, k -> unscoped.get());
            return instance;
        };
    }

    /**
     * Enters the scope for the specified plugin.
     *
     * <p>This method sets the current thread's plugin context, allowing
     * plugin-scoped instances to be resolved. Must be paired with
     * {@link #leaveScope()}.</p>
     *
     * @param pluginName the name of the plugin entering scope
     * @throws IllegalStateException if already in a plugin scope on this thread
     * @throws IllegalArgumentException if pluginName is null or empty
     */
    public void enterScope(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be null or empty");
        }
        if (CURRENT_PLUGIN.get() != null) {
            throw new IllegalStateException(
                "Already in plugin scope for '" + CURRENT_PLUGIN.get() + "'. " +
                "Cannot enter scope for '" + pluginName + "'. " +
                "Nested plugin scopes are not supported."
            );
        }
        CURRENT_PLUGIN.set(pluginName);
    }

    /**
     * Leaves the current plugin scope on this thread.
     *
     * <p>This method clears the thread's plugin context. Does not destroy
     * the cached instances - they remain available for future scope entries.</p>
     */
    public void leaveScope() {
        CURRENT_PLUGIN.remove();
    }

    /**
     * Gets the current plugin name for this thread, if in scope.
     *
     * @return the current plugin name, or {@code null} if not in plugin scope
     */
    public String getCurrentPlugin() {
        return CURRENT_PLUGIN.get();
    }

    /**
     * Checks if the current thread is within a plugin scope.
     *
     * @return {@code true} if in plugin scope, {@code false} otherwise
     */
    public boolean isInScope() {
        return CURRENT_PLUGIN.get() != null;
    }

    /**
     * Checks if the current thread is in scope for the specified plugin.
     *
     * @param pluginName the plugin name to check
     * @return {@code true} if in scope for the specified plugin
     */
    public boolean isInScope(String pluginName) {
        return pluginName != null && pluginName.equals(CURRENT_PLUGIN.get());
    }

    /**
     * Exits and cleans up all instances for the specified plugin.
     *
     * <p>This method should be called when a plugin is disabled to free
     * resources. All cached instances for the plugin are removed.</p>
     *
     * <p>Note: This does NOT invoke {@link sh.pcx.unified.inject.PreDestroy}
     * methods directly. Use {@link sh.pcx.unified.inject.lifecycle.LifecycleProcessor}
     * to handle lifecycle callbacks before calling this method.</p>
     *
     * @param pluginName the name of the plugin exiting scope
     * @return the map of instances that were removed, for lifecycle processing
     */
    public Map<Key<?>, Object> exitScope(String pluginName) {
        Map<Key<?>, Object> instances = pluginInstances.remove(pluginName);

        // Also clear thread local if this thread was scoped to the exiting plugin
        if (pluginName.equals(CURRENT_PLUGIN.get())) {
            CURRENT_PLUGIN.remove();
        }

        return instances != null ? instances : Collections.emptyMap();
    }

    /**
     * Gets all instance keys for a specific plugin.
     *
     * <p>Useful for lifecycle processing to iterate over all instances
     * for a plugin.</p>
     *
     * @param pluginName the plugin name
     * @return iterable of keys for the plugin's instances
     */
    public Iterable<Key<?>> getInstanceKeys(String pluginName) {
        Map<Key<?>, Object> instances = pluginInstances.get(pluginName);
        return instances != null ? instances.keySet() : Collections.emptySet();
    }

    /**
     * Gets a specific instance for a plugin without requiring scope entry.
     *
     * <p>This method allows direct access to a plugin's cached instance,
     * useful for lifecycle processing or cross-plugin operations.</p>
     *
     * @param pluginName the plugin name
     * @param key the binding key
     * @param <T> the instance type
     * @return the cached instance, or {@code null} if not yet created
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(String pluginName, Key<T> key) {
        Map<Key<?>, Object> instances = pluginInstances.get(pluginName);
        return instances != null ? (T) instances.get(key) : null;
    }

    /**
     * Gets all plugin names that have cached instances.
     *
     * @return unmodifiable set of plugin names with active scopes
     */
    public Set<String> getActivePlugins() {
        return Collections.unmodifiableSet(pluginInstances.keySet());
    }

    /**
     * Gets the number of plugins currently with cached instances.
     *
     * @return the number of active plugin scopes
     */
    public int getActivePluginCount() {
        return pluginInstances.size();
    }

    /**
     * Clears all cached instances for all plugins.
     *
     * <p>Use with caution - this does not invoke lifecycle methods.
     * Primarily intended for testing or emergency cleanup.</p>
     */
    public void clearAll() {
        pluginInstances.clear();
    }

    @Override
    public String toString() {
        return "PluginScope[active=" + pluginInstances.size() + ", plugins=" + pluginInstances.keySet() + "]";
    }
}
