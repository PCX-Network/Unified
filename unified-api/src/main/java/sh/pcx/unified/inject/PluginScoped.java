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
 * Scope annotation indicating that a single instance is created per plugin.
 *
 * <p>Objects annotated with {@code @PluginScoped} have their lifecycle tied to a specific
 * plugin instance. This differs from {@link com.google.inject.Singleton} in that the scope
 * is per-plugin rather than per-injector, allowing multiple plugins to have their own
 * isolated instances of the same service.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li><b>Creation:</b> First injection request for a plugin creates the instance</li>
 *   <li><b>Reuse:</b> Subsequent injections in the same plugin return the same instance</li>
 *   <li><b>Destruction:</b> Instance is destroyed when the plugin is disabled</li>
 * </ul>
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>Services that need plugin-specific configuration</li>
 *   <li>Resources that should be isolated between plugins</li>
 *   <li>Caches that are specific to a plugin's domain</li>
 *   <li>Services that extend shared functionality with plugin-specific behavior</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Plugin-Specific Cache</h3>
 * <pre>{@code
 * @Service
 * @PluginScoped
 * public class PluginCache {
 *     private final Cache<String, Object> cache;
 *     private final String pluginName;
 *
 *     @Inject
 *     public PluginCache(UnifiedPlugin plugin, CacheConfig config) {
 *         this.pluginName = plugin.getName();
 *         this.cache = Caffeine.newBuilder()
 *             .maximumSize(config.getMaxSize())
 *             .expireAfterWrite(config.getTtl())
 *             .build();
 *     }
 *
 *     public void put(String key, Object value) {
 *         cache.put(pluginName + ":" + key, value);
 *     }
 *
 *     @PreDestroy
 *     public void onPluginDisable() {
 *         cache.invalidateAll();
 *     }
 * }
 * }</pre>
 *
 * <h3>Plugin Logger Wrapper</h3>
 * <pre>{@code
 * @Service
 * @PluginScoped
 * public class PluginLogger {
 *     private final Logger logger;
 *     private final boolean debugEnabled;
 *
 *     @Inject
 *     public PluginLogger(UnifiedPlugin plugin, @Config("debug") boolean debug) {
 *         this.logger = plugin.getLogger();
 *         this.debugEnabled = debug;
 *     }
 *
 *     public void debug(String message, Object... args) {
 *         if (debugEnabled) {
 *             logger.info("[DEBUG] " + String.format(message, args));
 *         }
 *     }
 *
 *     public void info(String message, Object... args) {
 *         logger.info(String.format(message, args));
 *     }
 * }
 * }</pre>
 *
 * <h3>Plugin Configuration Manager</h3>
 * <pre>{@code
 * @Service
 * @PluginScoped
 * public class PluginConfigManager {
 *     private final ConfigurationNode root;
 *     private final Path configPath;
 *
 *     @Inject
 *     public PluginConfigManager(UnifiedPlugin plugin) throws IOException {
 *         this.configPath = plugin.getDataFolder().toPath().resolve("config.yml");
 *         this.root = YamlConfigurationLoader.builder()
 *             .path(configPath)
 *             .build()
 *             .load();
 *     }
 *
 *     @OnReload
 *     public void reload() throws IOException {
 *         // Reload configuration
 *     }
 *
 *     @PreDestroy
 *     public void save() throws IOException {
 *         // Save any pending changes
 *     }
 * }
 * }</pre>
 *
 * <h2>Comparison with @Singleton</h2>
 * <table border="1">
 *   <tr><th>Aspect</th><th>@Singleton</th><th>@PluginScoped</th></tr>
 *   <tr><td>Instance Count</td><td>One per Injector</td><td>One per Plugin</td></tr>
 *   <tr><td>Isolation</td><td>Shared across plugins</td><td>Isolated per plugin</td></tr>
 *   <tr><td>Lifecycle</td><td>Injector lifetime</td><td>Plugin lifetime</td></tr>
 *   <tr><td>Use Case</td><td>Truly global services</td><td>Plugin-specific singletons</td></tr>
 * </table>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PlayerScoped
 * @see WorldScoped
 * @see sh.pcx.unified.inject.scope.PluginScope
 * @see com.google.inject.Singleton
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ScopeAnnotation
public @interface PluginScoped {
}
