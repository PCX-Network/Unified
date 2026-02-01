/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */

/**
 * Custom Guice scope implementations for Minecraft-specific lifecycles.
 *
 * <p>This package provides thread-safe scope implementations that manage object
 * lifecycles based on Minecraft concepts like players, worlds, and plugins.</p>
 *
 * <h2>Available Scopes</h2>
 *
 * <h3>PlayerScope</h3>
 * <p>Maintains one instance per player session. Instances are created when first
 * requested within a player's context and destroyed when the player disconnects.</p>
 *
 * <pre>{@code
 * @Service
 * @PlayerScoped
 * public class PlayerSession {
 *     private final UUID playerId;
 *     private long loginTime;
 *
 *     @Inject
 *     public PlayerSession(UnifiedPlayer player) {
 *         this.playerId = player.getUniqueId();
 *         this.loginTime = System.currentTimeMillis();
 *     }
 * }
 * }</pre>
 *
 * <h3>WorldScope</h3>
 * <p>Maintains one instance per world. Useful for world-specific configurations,
 * region managers, or other world-bound services.</p>
 *
 * <pre>{@code
 * @Service
 * @WorldScoped
 * public class WorldSettings {
 *     private boolean pvpEnabled;
 *     private double mobSpawnRate;
 * }
 * }</pre>
 *
 * <h3>PluginScope</h3>
 * <p>Maintains one instance per plugin. Unlike {@code @Singleton} which is per-injector,
 * plugin scope allows multiple plugins to have isolated instances of shared types.</p>
 *
 * <pre>{@code
 * @Service
 * @PluginScoped
 * public class PluginCache {
 *     private final Cache<String, Object> cache;
 * }
 * }</pre>
 *
 * <h2>ScopeManager</h2>
 * <p>The {@link sh.pcx.unified.inject.scope.ScopeManager} provides a unified
 * API for entering, exiting, and destroying scopes:</p>
 *
 * <pre>{@code
 * @Inject
 * private ScopeManager scopeManager;
 *
 * // Enter player scope
 * try (var ctx = scopeManager.enterPlayer(player.getUniqueId())) {
 *     PlayerSession session = injector.getInstance(PlayerSession.class);
 *     // Use session...
 * } // Scope automatically exited
 *
 * // Destroy scope on player quit
 * scopeManager.destroyPlayerScope(player.getUniqueId());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All scope implementations are fully thread-safe using {@link java.util.concurrent.ConcurrentHashMap}
 * for storage and {@link ThreadLocal} for context tracking.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.inject.PlayerScoped
 * @see sh.pcx.unified.inject.WorldScoped
 * @see sh.pcx.unified.inject.PluginScoped
 */
package sh.pcx.unified.inject.scope;
