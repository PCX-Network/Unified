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
 * Marks a method to be called immediately after dependency injection is complete.
 *
 * <p>Methods annotated with {@code @PostConstruct} are invoked after all dependencies
 * have been injected into the object. This is the ideal place for initialization logic
 * that requires access to injected dependencies.</p>
 *
 * <h2>Method Requirements</h2>
 * <ul>
 *   <li>Must be a public or package-private instance method</li>
 *   <li>Must not be static</li>
 *   <li>Must have no parameters (or only injectable parameters)</li>
 *   <li>Return type is ignored (typically void)</li>
 *   <li>May throw exceptions (will be propagated)</li>
 * </ul>
 *
 * <h2>Execution Order</h2>
 * <ol>
 *   <li>Constructor is called with injected parameters</li>
 *   <li>Field injection is performed</li>
 *   <li>Method injection is performed</li>
 *   <li>{@code @PostConstruct} method is called</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Initialization</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class CacheManager {
 *     @Inject
 *     private DatabaseService database;
 *
 *     @Inject
 *     @Config("cache.max-size")
 *     private int maxSize;
 *
 *     private Cache<String, Object> cache;
 *
 *     @PostConstruct
 *     public void initialize() {
 *         // Dependencies are available here
 *         this.cache = Caffeine.newBuilder()
 *             .maximumSize(maxSize)
 *             .build();
 *
 *         // Preload cache from database
 *         database.query("SELECT * FROM cache_data")
 *             .forEach(row -> cache.put(row.key(), row.value()));
 *     }
 * }
 * }</pre>
 *
 * <h3>Async Initialization</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class PlayerDataService {
 *     @Inject
 *     private DatabaseService database;
 *
 *     @Inject
 *     private SchedulerService scheduler;
 *
 *     @PostConstruct
 *     public void initialize() {
 *         // Start async background tasks
 *         scheduler.runAsyncRepeating(this::cleanupExpiredData,
 *             Duration.ofMinutes(5), Duration.ofMinutes(5));
 *     }
 *
 *     private void cleanupExpiredData() {
 *         database.execute("DELETE FROM player_data WHERE expires_at < NOW()");
 *     }
 * }
 * }</pre>
 *
 * <h3>With Exception Handling</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class ConfigLoader {
 *     @Inject
 *     private UnifiedPlugin plugin;
 *
 *     private ConfigurationNode config;
 *
 *     @PostConstruct
 *     public void loadConfig() throws IOException {
 *         Path configPath = plugin.getDataFolder().toPath().resolve("config.yml");
 *         if (!Files.exists(configPath)) {
 *             plugin.saveResource("config.yml", false);
 *         }
 *         this.config = YamlConfigurationLoader.builder()
 *             .path(configPath)
 *             .build()
 *             .load();
 *     }
 * }
 * }</pre>
 *
 * <h3>Multiple Post-Construct Methods</h3>
 * <pre>{@code
 * @Service
 * public class ComplexService extends BaseService {
 *     // If multiple @PostConstruct methods exist (including inherited),
 *     // the execution order is:
 *     // 1. Superclass @PostConstruct methods (in declaration order)
 *     // 2. Subclass @PostConstruct methods (in declaration order)
 *
 *     @PostConstruct
 *     void initializePhaseOne() {
 *         // First initialization phase
 *     }
 *
 *     @PostConstruct
 *     void initializePhaseTwo() {
 *         // Second initialization phase
 *     }
 * }
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Keep initialization logic lightweight for fast startup</li>
 *   <li>Move heavy operations to background tasks</li>
 *   <li>Handle exceptions gracefully or let them propagate for fail-fast behavior</li>
 *   <li>Avoid circular initialization dependencies</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PreDestroy
 * @see OnReload
 * @see sh.pcx.unified.inject.lifecycle.LifecycleProcessor
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostConstruct {

    /**
     * Priority of this initialization method relative to others.
     *
     * <p>Lower values execute first. Default is 0. Useful when multiple
     * services have dependencies on initialization order.</p>
     *
     * @return the priority value
     */
    int priority() default 0;
}
