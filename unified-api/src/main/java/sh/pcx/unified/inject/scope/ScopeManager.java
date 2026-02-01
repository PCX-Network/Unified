/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject.scope;

import com.google.inject.Key;
import sh.pcx.unified.inject.lifecycle.LifecycleProcessor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for entering and exiting custom scopes.
 *
 * <p>The ScopeManager provides a unified API for managing the lifecycle of
 * player, world, and plugin scopes. It handles scope entry/exit with proper
 * resource cleanup and lifecycle callback invocation.</p>
 *
 * <h2>Scope Contexts</h2>
 * <p>Each scope entry returns a {@link ScopeContext} that implements {@link AutoCloseable},
 * allowing use with try-with-resources for automatic scope cleanup:</p>
 *
 * <pre>{@code
 * try (ScopeContext ctx = scopeManager.enterPlayer(player.getUniqueId())) {
 *     PlayerSession session = injector.getInstance(PlayerSession.class);
 *     session.handleCommand(command);
 * }
 * // Scope automatically exited, but instances remain cached
 * }</pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Player Scope</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class CommandHandler {
 *     @Inject private ScopeManager scopeManager;
 *     @Inject private Provider<PlayerSession> sessionProvider;
 *
 *     public void handleCommand(Player player, String command) {
 *         try (ScopeContext ctx = scopeManager.enterPlayer(player.getUniqueId())) {
 *             PlayerSession session = sessionProvider.get();
 *             session.processCommand(command);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>World Scope</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class RegionService {
 *     @Inject private ScopeManager scopeManager;
 *     @Inject private Provider<WorldRegionManager> regionManagerProvider;
 *
 *     public Optional<Region> getRegionAt(Location location) {
 *         try (ScopeContext ctx = scopeManager.enterWorld(location.getWorld().getName())) {
 *             return regionManagerProvider.get().getRegionAt(location);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Combined Scopes</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class PlayerWorldService {
 *     @Inject private ScopeManager scopeManager;
 *
 *     public void handlePlayerInWorld(Player player, World world) {
 *         try (ScopeContext worldCtx = scopeManager.enterWorld(world.getName());
 *              ScopeContext playerCtx = scopeManager.enterPlayer(player.getUniqueId())) {
 *             // Both world and player scopes active
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Lifecycle Management</h3>
 * <pre>{@code
 * // Called when player disconnects
 * public void onPlayerQuit(UUID playerId) {
 *     scopeManager.destroyPlayerScope(playerId);
 * }
 *
 * // Called when world unloads
 * public void onWorldUnload(String worldName) {
 *     scopeManager.destroyWorldScope(worldName);
 * }
 *
 * // Called when plugin disables
 * public void onPluginDisable(String pluginName) {
 *     scopeManager.destroyPluginScope(pluginName);
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PlayerScope
 * @see WorldScope
 * @see PluginScope
 * @see ScopeContext
 */
@Singleton
public class ScopeManager {

    private static final Logger LOGGER = Logger.getLogger(ScopeManager.class.getName());

    private final PlayerScope playerScope;
    private final WorldScope worldScope;
    private final PluginScope pluginScope;
    private final LifecycleProcessor lifecycleProcessor;

    /**
     * Creates a new ScopeManager with the provided scope implementations.
     *
     * @param playerScope the player scope implementation
     * @param worldScope the world scope implementation
     * @param pluginScope the plugin scope implementation
     * @param lifecycleProcessor the lifecycle processor for callback invocation
     */
    @Inject
    public ScopeManager(
            PlayerScope playerScope,
            WorldScope worldScope,
            PluginScope pluginScope,
            LifecycleProcessor lifecycleProcessor) {
        this.playerScope = playerScope;
        this.worldScope = worldScope;
        this.pluginScope = pluginScope;
        this.lifecycleProcessor = lifecycleProcessor;
    }

    // ========== Player Scope ==========

    /**
     * Enters the player scope for the specified player.
     *
     * <p>Returns a {@link ScopeContext} that should be closed when the scope
     * operation is complete. Use with try-with-resources.</p>
     *
     * @param playerId the UUID of the player
     * @return a closeable scope context
     * @throws IllegalStateException if already in player scope on this thread
     */
    public ScopeContext enterPlayer(UUID playerId) {
        playerScope.enterScope(playerId);
        return new PlayerScopeContext(playerId);
    }

    /**
     * Leaves the current player scope on this thread.
     *
     * <p>Prefer using {@link ScopeContext#close()} via try-with-resources instead.</p>
     */
    public void leavePlayer() {
        playerScope.leaveScope();
    }

    /**
     * Destroys the player scope, invoking lifecycle callbacks and cleaning up instances.
     *
     * <p>This should be called when a player disconnects to properly clean up
     * all player-scoped instances and invoke their {@code @PreDestroy} methods.</p>
     *
     * @param playerId the UUID of the player
     */
    public void destroyPlayerScope(UUID playerId) {
        LOGGER.fine("Destroying player scope for: " + playerId);

        // Get instances before removal for lifecycle processing
        Map<Key<?>, Object> instances = playerScope.exitScope(playerId);

        // Invoke PreDestroy on all instances
        for (Object instance : instances.values()) {
            try {
                lifecycleProcessor.invokePreDestroy(instance);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "Error invoking @PreDestroy for player " + playerId + " on " + instance.getClass(), e);
            }
        }

        LOGGER.fine("Player scope destroyed: " + playerId + " (" + instances.size() + " instances)");
    }

    /**
     * Checks if the current thread is in player scope.
     *
     * @return {@code true} if in player scope
     */
    public boolean isInPlayerScope() {
        return playerScope.isInScope();
    }

    /**
     * Gets the current player UUID if in scope.
     *
     * @return the current player UUID, or {@code null} if not in player scope
     */
    public UUID getCurrentPlayer() {
        return playerScope.getCurrentPlayer();
    }

    // ========== World Scope ==========

    /**
     * Enters the world scope for the specified world.
     *
     * <p>Returns a {@link ScopeContext} that should be closed when the scope
     * operation is complete. Use with try-with-resources.</p>
     *
     * @param worldName the name of the world
     * @return a closeable scope context
     * @throws IllegalStateException if already in world scope on this thread
     */
    public ScopeContext enterWorld(String worldName) {
        worldScope.enterScope(worldName);
        return new WorldScopeContext(worldName);
    }

    /**
     * Leaves the current world scope on this thread.
     *
     * <p>Prefer using {@link ScopeContext#close()} via try-with-resources instead.</p>
     */
    public void leaveWorld() {
        worldScope.leaveScope();
    }

    /**
     * Destroys the world scope, invoking lifecycle callbacks and cleaning up instances.
     *
     * <p>This should be called when a world is unloaded to properly clean up
     * all world-scoped instances and invoke their {@code @PreDestroy} methods.</p>
     *
     * @param worldName the name of the world
     */
    public void destroyWorldScope(String worldName) {
        LOGGER.fine("Destroying world scope for: " + worldName);

        // Get instances before removal for lifecycle processing
        Map<Key<?>, Object> instances = worldScope.exitScope(worldName);

        // Invoke PreDestroy on all instances
        for (Object instance : instances.values()) {
            try {
                lifecycleProcessor.invokePreDestroy(instance);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "Error invoking @PreDestroy for world " + worldName + " on " + instance.getClass(), e);
            }
        }

        LOGGER.fine("World scope destroyed: " + worldName + " (" + instances.size() + " instances)");
    }

    /**
     * Checks if the current thread is in world scope.
     *
     * @return {@code true} if in world scope
     */
    public boolean isInWorldScope() {
        return worldScope.isInScope();
    }

    /**
     * Gets the current world name if in scope.
     *
     * @return the current world name, or {@code null} if not in world scope
     */
    public String getCurrentWorld() {
        return worldScope.getCurrentWorld();
    }

    // ========== Plugin Scope ==========

    /**
     * Enters the plugin scope for the specified plugin.
     *
     * <p>Returns a {@link ScopeContext} that should be closed when the scope
     * operation is complete. Use with try-with-resources.</p>
     *
     * @param pluginName the name of the plugin
     * @return a closeable scope context
     * @throws IllegalStateException if already in plugin scope on this thread
     */
    public ScopeContext enterPlugin(String pluginName) {
        pluginScope.enterScope(pluginName);
        return new PluginScopeContext(pluginName);
    }

    /**
     * Leaves the current plugin scope on this thread.
     *
     * <p>Prefer using {@link ScopeContext#close()} via try-with-resources instead.</p>
     */
    public void leavePlugin() {
        pluginScope.leaveScope();
    }

    /**
     * Destroys the plugin scope, invoking lifecycle callbacks and cleaning up instances.
     *
     * <p>This should be called when a plugin is disabled to properly clean up
     * all plugin-scoped instances and invoke their {@code @PreDestroy} methods.</p>
     *
     * @param pluginName the name of the plugin
     */
    public void destroyPluginScope(String pluginName) {
        LOGGER.fine("Destroying plugin scope for: " + pluginName);

        // Get instances before removal for lifecycle processing
        Map<Key<?>, Object> instances = pluginScope.exitScope(pluginName);

        // Invoke PreDestroy on all instances
        for (Object instance : instances.values()) {
            try {
                lifecycleProcessor.invokePreDestroy(instance);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "Error invoking @PreDestroy for plugin " + pluginName + " on " + instance.getClass(), e);
            }
        }

        LOGGER.fine("Plugin scope destroyed: " + pluginName + " (" + instances.size() + " instances)");
    }

    /**
     * Checks if the current thread is in plugin scope.
     *
     * @return {@code true} if in plugin scope
     */
    public boolean isInPluginScope() {
        return pluginScope.isInScope();
    }

    /**
     * Gets the current plugin name if in scope.
     *
     * @return the current plugin name, or {@code null} if not in plugin scope
     */
    public String getCurrentPlugin() {
        return pluginScope.getCurrentPlugin();
    }

    // ========== Scope Accessors ==========

    /**
     * Gets the player scope implementation.
     *
     * @return the player scope
     */
    public PlayerScope getPlayerScope() {
        return playerScope;
    }

    /**
     * Gets the world scope implementation.
     *
     * @return the world scope
     */
    public WorldScope getWorldScope() {
        return worldScope;
    }

    /**
     * Gets the plugin scope implementation.
     *
     * @return the plugin scope
     */
    public PluginScope getPluginScope() {
        return pluginScope;
    }

    // ========== Scope Context Implementations ==========

    /**
     * AutoCloseable context for managing scope entry/exit.
     */
    public interface ScopeContext extends AutoCloseable {
        /**
         * Exits the scope. Does not throw exceptions.
         */
        @Override
        void close();
    }

    private class PlayerScopeContext implements ScopeContext {
        private final UUID playerId;

        PlayerScopeContext(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public void close() {
            if (playerScope.isInScope(playerId)) {
                playerScope.leaveScope();
            }
        }
    }

    private class WorldScopeContext implements ScopeContext {
        private final String worldName;

        WorldScopeContext(String worldName) {
            this.worldName = worldName;
        }

        @Override
        public void close() {
            if (worldScope.isInScope(worldName)) {
                worldScope.leaveScope();
            }
        }
    }

    private class PluginScopeContext implements ScopeContext {
        private final String pluginName;

        PluginScopeContext(String pluginName) {
            this.pluginName = pluginName;
        }

        @Override
        public void close() {
            if (pluginScope.isInScope(pluginName)) {
                pluginScope.leaveScope();
            }
        }
    }
}
