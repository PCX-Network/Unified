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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guice {@link Scope} implementation for player-scoped instances.
 *
 * <p>This scope maintains a separate instance of each bound type for each player.
 * Instances are created lazily when first requested within a player's scope context
 * and are cached for the duration of the player's session.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is fully thread-safe. Multiple threads can safely request
 * and create instances for different players concurrently. The internal storage uses
 * {@link ConcurrentHashMap} for lock-free concurrent access.</p>
 *
 * <h2>Scope Entry/Exit</h2>
 * <p>The player scope must be entered before requesting player-scoped instances.
 * This is typically managed by the {@link ScopeManager} and happens automatically
 * when processing player events or commands.</p>
 *
 * <pre>{@code
 * // Manual scope management (usually handled by framework)
 * try (ScopeContext context = scopeManager.enterPlayer(player)) {
 *     PlayerSession session = injector.getInstance(PlayerSession.class);
 *     // Use session...
 * }
 * }</pre>
 *
 * <h2>Instance Lifecycle</h2>
 * <ul>
 *   <li>Instances are created on first access within the scope</li>
 *   <li>Same instance is returned for all requests within the same player context</li>
 *   <li>Instances are destroyed when {@link #exitScope(UUID)} is called (on player disconnect)</li>
 *   <li>{@link sh.pcx.unified.inject.PreDestroy} methods are invoked before destruction</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In module configuration
 * public class MyModule extends AbstractModule {
 *     @Override
 *     protected void configure() {
 *         PlayerScope playerScope = new PlayerScope();
 *         bindScope(PlayerScoped.class, playerScope);
 *         bind(PlayerScope.class).toInstance(playerScope);
 *
 *         // Bind player-scoped services
 *         bind(PlayerSession.class).in(PlayerScoped.class);
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.inject.PlayerScoped
 * @see ScopeManager
 */
public class PlayerScope implements Scope {

    /**
     * ThreadLocal holding the current player UUID for scope resolution.
     */
    private static final ThreadLocal<UUID> CURRENT_PLAYER = new ThreadLocal<>();

    /**
     * Storage for player-scoped instances.
     * Outer map: Player UUID -> (Key -> Instance)
     */
    private final Map<UUID, Map<Key<?>, Object>> playerInstances = new ConcurrentHashMap<>();

    /**
     * Creates a new PlayerScope instance.
     */
    public PlayerScope() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a provider that resolves instances within the current player scope.
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
            UUID playerId = CURRENT_PLAYER.get();
            if (playerId == null) {
                throw new OutOfScopeException(
                    "Cannot access player-scoped binding outside of player scope. " +
                    "Ensure you are within a player context using ScopeManager.enterPlayer(). " +
                    "Requested key: " + key
                );
            }

            Map<Key<?>, Object> instances = playerInstances.computeIfAbsent(
                playerId,
                k -> new ConcurrentHashMap<>()
            );

            @SuppressWarnings("unchecked")
            T instance = (T) instances.computeIfAbsent(key, k -> unscoped.get());
            return instance;
        };
    }

    /**
     * Enters the scope for the specified player.
     *
     * <p>This method sets the current thread's player context, allowing
     * player-scoped instances to be resolved. Must be paired with
     * {@link #leaveScope()}.</p>
     *
     * @param playerId the UUID of the player entering scope
     * @throws IllegalStateException if already in a player scope on this thread
     */
    public void enterScope(UUID playerId) {
        if (CURRENT_PLAYER.get() != null) {
            throw new IllegalStateException(
                "Already in player scope for " + CURRENT_PLAYER.get() +
                ". Cannot enter scope for " + playerId + ". " +
                "Nested player scopes are not supported."
            );
        }
        CURRENT_PLAYER.set(playerId);
    }

    /**
     * Leaves the current player scope on this thread.
     *
     * <p>This method clears the thread's player context. Does not destroy
     * the cached instances - they remain available for future scope entries.</p>
     */
    public void leaveScope() {
        CURRENT_PLAYER.remove();
    }

    /**
     * Gets the current player UUID for this thread, if in scope.
     *
     * @return the current player UUID, or {@code null} if not in player scope
     */
    public UUID getCurrentPlayer() {
        return CURRENT_PLAYER.get();
    }

    /**
     * Checks if the current thread is within a player scope.
     *
     * @return {@code true} if in player scope, {@code false} otherwise
     */
    public boolean isInScope() {
        return CURRENT_PLAYER.get() != null;
    }

    /**
     * Checks if the current thread is in scope for the specified player.
     *
     * @param playerId the player UUID to check
     * @return {@code true} if in scope for the specified player
     */
    public boolean isInScope(UUID playerId) {
        return playerId.equals(CURRENT_PLAYER.get());
    }

    /**
     * Exits and cleans up all instances for the specified player.
     *
     * <p>This method should be called when a player disconnects to free
     * resources. All cached instances for the player are removed.</p>
     *
     * <p>Note: This does NOT invoke {@link sh.pcx.unified.inject.PreDestroy}
     * methods directly. Use {@link sh.pcx.unified.inject.lifecycle.LifecycleProcessor}
     * to handle lifecycle callbacks before calling this method.</p>
     *
     * @param playerId the UUID of the player exiting scope
     * @return the map of instances that were removed, for lifecycle processing
     */
    public Map<Key<?>, Object> exitScope(UUID playerId) {
        Map<Key<?>, Object> instances = playerInstances.remove(playerId);

        // Also clear thread local if this thread was scoped to the exiting player
        if (playerId.equals(CURRENT_PLAYER.get())) {
            CURRENT_PLAYER.remove();
        }

        return instances != null ? instances : Map.of();
    }

    /**
     * Gets all instance keys for a specific player.
     *
     * <p>Useful for lifecycle processing to iterate over all instances
     * for a player.</p>
     *
     * @param playerId the player UUID
     * @return iterable of keys for the player's instances
     */
    public Iterable<Key<?>> getInstanceKeys(UUID playerId) {
        Map<Key<?>, Object> instances = playerInstances.get(playerId);
        return instances != null ? instances.keySet() : Set.of();
    }

    /**
     * Gets a specific instance for a player without requiring scope entry.
     *
     * <p>This method allows direct access to a player's cached instance,
     * useful for lifecycle processing or cross-player operations.</p>
     *
     * @param playerId the player UUID
     * @param key the binding key
     * @param <T> the instance type
     * @return the cached instance, or {@code null} if not yet created
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(UUID playerId, Key<T> key) {
        Map<Key<?>, Object> instances = playerInstances.get(playerId);
        return instances != null ? (T) instances.get(key) : null;
    }

    /**
     * Gets the number of players currently with cached instances.
     *
     * @return the number of active player scopes
     */
    public int getActivePlayerCount() {
        return playerInstances.size();
    }

    /**
     * Clears all cached instances for all players.
     *
     * <p>Use with caution - this does not invoke lifecycle methods.
     * Primarily intended for testing or emergency cleanup.</p>
     */
    public void clearAll() {
        playerInstances.clear();
    }

    @Override
    public String toString() {
        return "PlayerScope[active=" + playerInstances.size() + "]";
    }

    // Import for Set.of()
    private static final class Set {
        static <T> java.util.Set<T> of() {
            return java.util.Collections.emptySet();
        }
    }
}
