/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scope annotation indicating that a single instance is created per world.
 *
 * <p>Objects annotated with {@code @WorldScoped} have their lifecycle tied to a specific
 * Minecraft world. A new instance is created for each world, and that same instance is
 * used for all injections within that world's context. When a world is unloaded, the
 * instance is eligible for cleanup.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li><b>Creation:</b> First injection request for a world creates the instance</li>
 *   <li><b>Reuse:</b> Subsequent injections in the same world context return the same instance</li>
 *   <li><b>Destruction:</b> Instance is destroyed when the world is unloaded</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>World-Specific Configuration</h3>
 * <pre>{@code
 * @Service
 * @WorldScoped
 * public class WorldSettings {
 *     private final String worldName;
 *     private boolean pvpEnabled;
 *     private double mobSpawnMultiplier;
 *
 *     @Inject
 *     public WorldSettings(UnifiedWorld world, ConfigService config) {
 *         this.worldName = world.getName();
 *         this.pvpEnabled = config.get("worlds." + worldName + ".pvp", true);
 *         this.mobSpawnMultiplier = config.get("worlds." + worldName + ".mob-spawn", 1.0);
 *     }
 *
 *     @OnReload
 *     public void onReload(ConfigService config) {
 *         this.pvpEnabled = config.get("worlds." + worldName + ".pvp", true);
 *         this.mobSpawnMultiplier = config.get("worlds." + worldName + ".mob-spawn", 1.0);
 *     }
 * }
 * }</pre>
 *
 * <h3>World Region Manager</h3>
 * <pre>{@code
 * @Service
 * @WorldScoped
 * public class WorldRegionManager {
 *     private final QuadTree<Region> regions;
 *     private final String worldName;
 *
 *     @Inject
 *     public WorldRegionManager(UnifiedWorld world, DatabaseService database) {
 *         this.worldName = world.getName();
 *         this.regions = new QuadTree<>(world.getWorldBorder());
 *         loadRegions(database);
 *     }
 *
 *     public Optional<Region> getRegionAt(Location location) {
 *         return regions.findAt(location.getX(), location.getZ());
 *     }
 *
 *     @PreDestroy
 *     public void onWorldUnload() {
 *         // Save any pending changes before unload
 *     }
 * }
 * }</pre>
 *
 * <h3>World Entity Tracker</h3>
 * <pre>{@code
 * @Service
 * @WorldScoped
 * public class WorldEntityTracker {
 *     private final Map<UUID, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();
 *
 *     public void track(Entity entity) {
 *         trackedEntities.put(entity.getUniqueId(), new TrackedEntity(entity));
 *     }
 *
 *     public Collection<TrackedEntity> getTrackedEntities() {
 *         return Collections.unmodifiableCollection(trackedEntities.values());
 *     }
 * }
 * }</pre>
 *
 * <h2>Cross-World Access</h2>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class GlobalWorldService {
 *     @Inject
 *     private ScopeManager scopeManager;
 *
 *     @Inject
 *     private Provider<WorldRegionManager> regionManagerProvider;
 *
 *     public Optional<Region> findRegion(Location location) {
 *         try (var scope = scopeManager.enterWorld(location.getWorld())) {
 *             return regionManagerProvider.get().getRegionAt(location);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>World-scoped instances should be thread-safe, especially on Folia where
 * different regions of the same world may be processed on different threads.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PlayerScoped
 * @see PluginScoped
 * @see sh.pcx.unified.inject.scope.WorldScope
 * @see sh.pcx.unified.inject.scope.ScopeManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ScopeAnnotation
public @interface WorldScoped {
}
