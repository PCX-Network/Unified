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
 * Guice {@link Scope} implementation for world-scoped instances.
 *
 * <p>This scope maintains a separate instance of each bound type for each world.
 * Instances are created lazily when first requested within a world's scope context
 * and are cached for the duration of the world's existence.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is fully thread-safe and designed to work with Folia's
 * multi-threaded region system. Multiple threads can safely request and create
 * instances for different worlds concurrently.</p>
 *
 * <h2>World Identification</h2>
 * <p>Worlds are identified by their name string, which is stable across server
 * restarts and unique per server instance.</p>
 *
 * <h2>Scope Entry/Exit</h2>
 * <p>The world scope must be entered before requesting world-scoped instances.
 * This is typically managed by the {@link ScopeManager} and happens automatically
 * when processing location-based events or region operations.</p>
 *
 * <pre>{@code
 * // Manual scope management (usually handled by framework)
 * try (ScopeContext context = scopeManager.enterWorld(world)) {
 *     WorldSettings settings = injector.getInstance(WorldSettings.class);
 *     // Use settings...
 * }
 * }</pre>
 *
 * <h2>Instance Lifecycle</h2>
 * <ul>
 *   <li>Instances are created on first access within the scope</li>
 *   <li>Same instance is returned for all requests within the same world context</li>
 *   <li>Instances are destroyed when {@link #exitScope(String)} is called (on world unload)</li>
 *   <li>{@link sh.pcx.unified.inject.PreDestroy} methods are invoked before destruction</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In module configuration
 * public class MyModule extends AbstractModule {
 *     @Override
 *     protected void configure() {
 *         WorldScope worldScope = new WorldScope();
 *         bindScope(WorldScoped.class, worldScope);
 *         bind(WorldScope.class).toInstance(worldScope);
 *
 *         // Bind world-scoped services
 *         bind(WorldSettings.class).in(WorldScoped.class);
 *         bind(WorldRegionManager.class).in(WorldScoped.class);
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.inject.WorldScoped
 * @see ScopeManager
 */
public class WorldScope implements Scope {

    /**
     * ThreadLocal holding the current world name for scope resolution.
     */
    private static final ThreadLocal<String> CURRENT_WORLD = new ThreadLocal<>();

    /**
     * Storage for world-scoped instances.
     * Outer map: World name -> (Key -> Instance)
     */
    private final Map<String, Map<Key<?>, Object>> worldInstances = new ConcurrentHashMap<>();

    /**
     * Creates a new WorldScope instance.
     */
    public WorldScope() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a provider that resolves instances within the current world scope.
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
            String worldName = CURRENT_WORLD.get();
            if (worldName == null) {
                throw new OutOfScopeException(
                    "Cannot access world-scoped binding outside of world scope. " +
                    "Ensure you are within a world context using ScopeManager.enterWorld(). " +
                    "Requested key: " + key
                );
            }

            Map<Key<?>, Object> instances = worldInstances.computeIfAbsent(
                worldName,
                k -> new ConcurrentHashMap<>()
            );

            @SuppressWarnings("unchecked")
            T instance = (T) instances.computeIfAbsent(key, k -> unscoped.get());
            return instance;
        };
    }

    /**
     * Enters the scope for the specified world.
     *
     * <p>This method sets the current thread's world context, allowing
     * world-scoped instances to be resolved. Must be paired with
     * {@link #leaveScope()}.</p>
     *
     * @param worldName the name of the world entering scope
     * @throws IllegalStateException if already in a world scope on this thread
     * @throws IllegalArgumentException if worldName is null or empty
     */
    public void enterScope(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            throw new IllegalArgumentException("World name cannot be null or empty");
        }
        if (CURRENT_WORLD.get() != null) {
            throw new IllegalStateException(
                "Already in world scope for '" + CURRENT_WORLD.get() + "'. " +
                "Cannot enter scope for '" + worldName + "'. " +
                "Nested world scopes are not supported."
            );
        }
        CURRENT_WORLD.set(worldName);
    }

    /**
     * Leaves the current world scope on this thread.
     *
     * <p>This method clears the thread's world context. Does not destroy
     * the cached instances - they remain available for future scope entries.</p>
     */
    public void leaveScope() {
        CURRENT_WORLD.remove();
    }

    /**
     * Gets the current world name for this thread, if in scope.
     *
     * @return the current world name, or {@code null} if not in world scope
     */
    public String getCurrentWorld() {
        return CURRENT_WORLD.get();
    }

    /**
     * Checks if the current thread is within a world scope.
     *
     * @return {@code true} if in world scope, {@code false} otherwise
     */
    public boolean isInScope() {
        return CURRENT_WORLD.get() != null;
    }

    /**
     * Checks if the current thread is in scope for the specified world.
     *
     * @param worldName the world name to check
     * @return {@code true} if in scope for the specified world
     */
    public boolean isInScope(String worldName) {
        return worldName != null && worldName.equals(CURRENT_WORLD.get());
    }

    /**
     * Exits and cleans up all instances for the specified world.
     *
     * <p>This method should be called when a world is unloaded to free
     * resources. All cached instances for the world are removed.</p>
     *
     * <p>Note: This does NOT invoke {@link sh.pcx.unified.inject.PreDestroy}
     * methods directly. Use {@link sh.pcx.unified.inject.lifecycle.LifecycleProcessor}
     * to handle lifecycle callbacks before calling this method.</p>
     *
     * @param worldName the name of the world exiting scope
     * @return the map of instances that were removed, for lifecycle processing
     */
    public Map<Key<?>, Object> exitScope(String worldName) {
        Map<Key<?>, Object> instances = worldInstances.remove(worldName);

        // Also clear thread local if this thread was scoped to the exiting world
        if (worldName.equals(CURRENT_WORLD.get())) {
            CURRENT_WORLD.remove();
        }

        return instances != null ? instances : Collections.emptyMap();
    }

    /**
     * Gets all instance keys for a specific world.
     *
     * <p>Useful for lifecycle processing to iterate over all instances
     * for a world.</p>
     *
     * @param worldName the world name
     * @return iterable of keys for the world's instances
     */
    public Iterable<Key<?>> getInstanceKeys(String worldName) {
        Map<Key<?>, Object> instances = worldInstances.get(worldName);
        return instances != null ? instances.keySet() : Collections.emptySet();
    }

    /**
     * Gets a specific instance for a world without requiring scope entry.
     *
     * <p>This method allows direct access to a world's cached instance,
     * useful for lifecycle processing or cross-world operations.</p>
     *
     * @param worldName the world name
     * @param key the binding key
     * @param <T> the instance type
     * @return the cached instance, or {@code null} if not yet created
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(String worldName, Key<T> key) {
        Map<Key<?>, Object> instances = worldInstances.get(worldName);
        return instances != null ? (T) instances.get(key) : null;
    }

    /**
     * Gets all world names that have cached instances.
     *
     * @return unmodifiable set of world names with active scopes
     */
    public Set<String> getActiveWorlds() {
        return Collections.unmodifiableSet(worldInstances.keySet());
    }

    /**
     * Gets the number of worlds currently with cached instances.
     *
     * @return the number of active world scopes
     */
    public int getActiveWorldCount() {
        return worldInstances.size();
    }

    /**
     * Clears all cached instances for all worlds.
     *
     * <p>Use with caution - this does not invoke lifecycle methods.
     * Primarily intended for testing or emergency cleanup.</p>
     */
    public void clearAll() {
        worldInstances.clear();
    }

    @Override
    public String toString() {
        return "WorldScope[active=" + worldInstances.size() + ", worlds=" + worldInstances.keySet() + "]";
    }
}
