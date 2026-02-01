/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.Injector;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Global holder for {@link Injector} instances associated with plugins.
 *
 * <p>InjectorHolder provides a centralized registry for accessing plugin injectors.
 * This is useful for cross-plugin integration and for accessing the injector from
 * contexts where dependency injection is not directly available.</p>
 *
 * <h2>Registration</h2>
 * <p>Injectors should be registered when plugins enable and unregistered when they disable:</p>
 *
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *     private Injector injector;
 *
 *     @Override
 *     public void onEnable() {
 *         injector = InjectorFactory.create(new MyPluginModule(this));
 *         InjectorHolder.register(this.getName(), injector);
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         InjectorHolder.unregister(this.getName());
 *         InjectorFactory.shutdown(injector);
 *     }
 * }
 * }</pre>
 *
 * <h2>Retrieval</h2>
 * <pre>{@code
 * // Get your own plugin's injector
 * Optional<Injector> myInjector = InjectorHolder.get("MyPlugin");
 *
 * // Get another plugin's injector for integration
 * InjectorHolder.get("SomeOtherPlugin").ifPresent(injector -> {
 *     SomeService service = injector.getInstance(SomeService.class);
 *     service.doSomething();
 * });
 *
 * // Check if a plugin has registered an injector
 * if (InjectorHolder.isRegistered("OptionalDependency")) {
 *     // Safe to integrate
 * }
 * }</pre>
 *
 * <h2>Direct Instance Access</h2>
 * <pre>{@code
 * // Get an instance directly from a plugin's injector
 * Optional<PlayerManager> manager = InjectorHolder.getInstance("MyPlugin", PlayerManager.class);
 *
 * // With default fallback
 * EconomyService economy = InjectorHolder.getInstance("EconomyPlugin", EconomyService.class)
 *     .orElse(new NoOpEconomyService());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe. All operations can be safely called from
 * any thread.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see InjectorFactory
 * @see ServiceLocator
 */
public final class InjectorHolder {

    private static final Logger LOGGER = Logger.getLogger(InjectorHolder.class.getName());

    /**
     * Thread-safe map of plugin names to their injectors.
     */
    private static final Map<String, Injector> INJECTORS = new ConcurrentHashMap<>();

    private InjectorHolder() {
        // Utility class
    }

    /**
     * Registers an injector for a plugin.
     *
     * <p>If an injector is already registered for the plugin, it will be replaced
     * and a warning will be logged.</p>
     *
     * @param pluginName the plugin name (case-sensitive)
     * @param injector the injector to register
     * @throws IllegalArgumentException if pluginName is null or empty, or if injector is null
     */
    public static void register(String pluginName, Injector injector) {
        if (pluginName == null || pluginName.isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be null or empty");
        }
        if (injector == null) {
            throw new IllegalArgumentException("Injector cannot be null");
        }

        Injector previous = INJECTORS.put(pluginName, injector);
        if (previous != null) {
            LOGGER.warning("Replaced existing injector for plugin: " + pluginName);
        } else {
            LOGGER.fine("Registered injector for plugin: " + pluginName);
        }
    }

    /**
     * Unregisters the injector for a plugin.
     *
     * <p>This should be called when the plugin is disabled to prevent memory leaks
     * and stale references.</p>
     *
     * @param pluginName the plugin name
     * @return the removed injector, or {@code null} if none was registered
     */
    public static Injector unregister(String pluginName) {
        if (pluginName == null) {
            return null;
        }

        Injector removed = INJECTORS.remove(pluginName);
        if (removed != null) {
            LOGGER.fine("Unregistered injector for plugin: " + pluginName);
        }
        return removed;
    }

    /**
     * Gets the injector for a plugin.
     *
     * @param pluginName the plugin name
     * @return an Optional containing the injector, or empty if not registered
     */
    public static Optional<Injector> get(String pluginName) {
        if (pluginName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(INJECTORS.get(pluginName));
    }

    /**
     * Gets the injector for a plugin, throwing if not found.
     *
     * @param pluginName the plugin name
     * @return the injector
     * @throws IllegalStateException if no injector is registered for the plugin
     */
    public static Injector getOrThrow(String pluginName) {
        return get(pluginName).orElseThrow(() ->
            new IllegalStateException("No injector registered for plugin: " + pluginName)
        );
    }

    /**
     * Checks if an injector is registered for a plugin.
     *
     * @param pluginName the plugin name
     * @return {@code true} if an injector is registered
     */
    public static boolean isRegistered(String pluginName) {
        return pluginName != null && INJECTORS.containsKey(pluginName);
    }

    /**
     * Gets an instance from a plugin's injector.
     *
     * <p>This is a convenience method that combines {@link #get(String)} with
     * {@link Injector#getInstance(Class)}.</p>
     *
     * @param pluginName the plugin name
     * @param type the type to get
     * @param <T> the type
     * @return an Optional containing the instance, or empty if plugin not registered
     */
    public static <T> Optional<T> getInstance(String pluginName, Class<T> type) {
        return get(pluginName).map(injector -> {
            try {
                return injector.getInstance(type);
            } catch (Exception e) {
                LOGGER.warning("Failed to get instance of " + type.getName() +
                    " from plugin " + pluginName + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Gets the names of all plugins with registered injectors.
     *
     * @return unmodifiable set of plugin names
     */
    public static Set<String> getRegisteredPlugins() {
        return Collections.unmodifiableSet(INJECTORS.keySet());
    }

    /**
     * Gets the number of registered injectors.
     *
     * @return the count of registered injectors
     */
    public static int getRegisteredCount() {
        return INJECTORS.size();
    }

    /**
     * Clears all registered injectors.
     *
     * <p>Use with caution - this is primarily intended for testing or server shutdown.
     * It does NOT invoke lifecycle callbacks or perform cleanup.</p>
     */
    public static void clearAll() {
        int count = INJECTORS.size();
        INJECTORS.clear();
        LOGGER.info("Cleared " + count + " registered injectors");
    }

    /**
     * Unregisters and shuts down all injectors.
     *
     * <p>This method should be called during server shutdown to properly
     * clean up all plugin injectors.</p>
     */
    public static void shutdownAll() {
        LOGGER.info("Shutting down all registered injectors");

        for (Map.Entry<String, Injector> entry : INJECTORS.entrySet()) {
            try {
                LOGGER.fine("Shutting down injector for: " + entry.getKey());
                InjectorFactory.shutdown(entry.getValue());
            } catch (Exception e) {
                LOGGER.warning("Error shutting down injector for " + entry.getKey() + ": " + e.getMessage());
            }
        }

        clearAll();
    }
}
