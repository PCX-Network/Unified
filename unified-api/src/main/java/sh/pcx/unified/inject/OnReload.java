/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called when the plugin configuration is reloaded.
 *
 * <p>Methods annotated with {@code @OnReload} are invoked when an administrator
 * triggers a plugin reload, typically via a command like {@code /plugin reload}.
 * This allows services to refresh their configuration without requiring a full
 * server restart.</p>
 *
 * <h2>Method Requirements</h2>
 * <ul>
 *   <li>Must be a public or package-private instance method</li>
 *   <li>Must not be static</li>
 *   <li>May have no parameters or accept injectable parameters</li>
 *   <li>Return type is ignored (typically void)</li>
 *   <li>May throw exceptions (will be logged)</li>
 * </ul>
 *
 * <h2>Reload Phases</h2>
 * <p>The reload process follows this order:</p>
 * <ol>
 *   <li>Configuration files are re-read from disk</li>
 *   <li>{@code @OnReload} methods are invoked (sorted by priority)</li>
 *   <li>New @Config values are resolved for future injections</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Configuration Reload</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class MessageService {
 *     @Inject
 *     private ConfigService config;
 *
 *     private Map<String, String> messages;
 *
 *     @PostConstruct
 *     public void initialize() {
 *         loadMessages();
 *     }
 *
 *     @OnReload
 *     public void onReload() {
 *         loadMessages();
 *     }
 *
 *     private void loadMessages() {
 *         this.messages = config.getSection("messages")
 *             .entrySet()
 *             .stream()
 *             .collect(Collectors.toMap(
 *                 Map.Entry::getKey,
 *                 e -> String.valueOf(e.getValue())
 *             ));
 *     }
 *
 *     public String getMessage(String key) {
 *         return messages.getOrDefault(key, key);
 *     }
 * }
 * }</pre>
 *
 * <h3>Cache Invalidation</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class PermissionCache {
 *     @Inject
 *     private ConfigService config;
 *
 *     private final Cache<UUID, Set<String>> playerPermissions;
 *
 *     @Inject
 *     public PermissionCache() {
 *         this.playerPermissions = Caffeine.newBuilder()
 *             .expireAfterWrite(Duration.ofMinutes(5))
 *             .build();
 *     }
 *
 *     @OnReload
 *     public void clearCacheOnReload() {
 *         // Permissions may have changed, clear cache to force refresh
 *         playerPermissions.invalidateAll();
 *     }
 * }
 * }</pre>
 *
 * <h3>Feature Toggle Updates</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class FeatureManager {
 *     @Inject
 *     private ConfigService config;
 *
 *     private final Set<String> enabledFeatures = ConcurrentHashMap.newKeySet();
 *
 *     @PostConstruct
 *     public void initialize() {
 *         loadFeatures();
 *     }
 *
 *     @OnReload
 *     public void onReload() {
 *         Set<String> previousFeatures = new HashSet<>(enabledFeatures);
 *         loadFeatures();
 *
 *         // Log changes
 *         Set<String> newlyEnabled = new HashSet<>(enabledFeatures);
 *         newlyEnabled.removeAll(previousFeatures);
 *
 *         Set<String> newlyDisabled = new HashSet<>(previousFeatures);
 *         newlyDisabled.removeAll(enabledFeatures);
 *
 *         if (!newlyEnabled.isEmpty()) {
 *             logger.info("Enabled features: " + newlyEnabled);
 *         }
 *         if (!newlyDisabled.isEmpty()) {
 *             logger.info("Disabled features: " + newlyDisabled);
 *         }
 *     }
 *
 *     private void loadFeatures() {
 *         enabledFeatures.clear();
 *         enabledFeatures.addAll(config.getStringList("features.enabled"));
 *     }
 * }
 * }</pre>
 *
 * <h3>With Injected Dependencies</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class LocaleService {
 *     private Map<String, ResourceBundle> bundles;
 *
 *     @OnReload
 *     public void reloadLocales(ConfigService config, UnifiedPlugin plugin) {
 *         // Parameters are injected at invocation time
 *         String defaultLocale = config.getString("locale.default", "en_US");
 *         List<String> supportedLocales = config.getStringList("locale.supported");
 *
 *         bundles = new HashMap<>();
 *         for (String locale : supportedLocales) {
 *             bundles.put(locale, loadBundle(plugin, locale));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Async Reload Operations</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class DataSyncService {
 *     @Inject
 *     private SchedulerService scheduler;
 *
 *     @Inject
 *     private DatabaseService database;
 *
 *     @OnReload(async = true)
 *     public CompletableFuture<Void> reloadDataAsync() {
 *         return CompletableFuture.runAsync(() -> {
 *             // Perform expensive reload operation off main thread
 *             database.refreshCachedData();
 *         }, scheduler.getAsyncExecutor());
 *     }
 * }
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Keep reload logic fast to maintain server responsiveness</li>
 *   <li>Use async for heavy operations</li>
 *   <li>Validate new configuration before applying</li>
 *   <li>Log significant configuration changes</li>
 *   <li>Handle partial reload failures gracefully</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PostConstruct
 * @see PreDestroy
 * @see Config
 * @see sh.pcx.unified.inject.lifecycle.LifecycleProcessor
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnReload {

    /**
     * Priority of this reload method relative to others.
     *
     * <p>Lower values execute first. Default is 0. Use this to ensure
     * configuration services reload before services that depend on them.</p>
     *
     * @return the priority value
     */
    int priority() default 0;

    /**
     * Whether this reload method should be executed asynchronously.
     *
     * <p>When {@code true}, the method is invoked on a separate thread,
     * allowing the reload command to complete without waiting. The method
     * should return {@link java.util.concurrent.CompletableFuture} if it
     * needs to signal completion.</p>
     *
     * @return {@code true} for async execution, {@code false} for sync (default)
     */
    boolean async() default false;

    /**
     * Configuration keys that trigger this reload method.
     *
     * <p>If specified, the method is only invoked when one of these
     * configuration keys has changed. If empty (default), the method
     * is always invoked on reload.</p>
     *
     * <pre>{@code
     * @OnReload(watchKeys = {"database.host", "database.port"})
     * public void onDatabaseConfigChange() {
     *     reconnectDatabase();
     * }
     * }</pre>
     *
     * @return array of configuration keys to watch
     */
    String[] watchKeys() default {};
}
