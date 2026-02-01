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
 * Marks a method to be called before the object is destroyed or the scope ends.
 *
 * <p>Methods annotated with {@code @PreDestroy} are invoked during cleanup phases,
 * such as plugin shutdown, player logout, or world unload (depending on the object's
 * scope). This is the ideal place for cleanup logic such as closing connections,
 * saving data, or releasing resources.</p>
 *
 * <h2>Method Requirements</h2>
 * <ul>
 *   <li>Must be a public or package-private instance method</li>
 *   <li>Must not be static</li>
 *   <li>Must have no parameters</li>
 *   <li>Return type is ignored (typically void)</li>
 *   <li>Should not throw exceptions (logged and swallowed if thrown)</li>
 * </ul>
 *
 * <h2>Invocation Timing by Scope</h2>
 * <table border="1">
 *   <tr><th>Scope</th><th>@PreDestroy Called When</th></tr>
 *   <tr><td>@Singleton</td><td>Plugin is disabled</td></tr>
 *   <tr><td>@PluginScoped</td><td>Plugin is disabled</td></tr>
 *   <tr><td>@PlayerScoped</td><td>Player disconnects</td></tr>
 *   <tr><td>@WorldScoped</td><td>World is unloaded</td></tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Resource Cleanup</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class DatabaseConnectionPool {
 *     private HikariDataSource dataSource;
 *
 *     @PostConstruct
 *     public void initialize() {
 *         HikariConfig config = new HikariConfig();
 *         config.setJdbcUrl("jdbc:mysql://localhost/minecraft");
 *         this.dataSource = new HikariDataSource(config);
 *     }
 *
 *     @PreDestroy
 *     public void shutdown() {
 *         if (dataSource != null && !dataSource.isClosed()) {
 *             dataSource.close();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Data Persistence</h3>
 * <pre>{@code
 * @Service
 * @PlayerScoped
 * public class PlayerSession {
 *     @Inject
 *     private DatabaseService database;
 *
 *     private UUID playerId;
 *     private long loginTime;
 *     private Map<String, Object> sessionData = new HashMap<>();
 *
 *     @PreDestroy
 *     public void saveSession() {
 *         long sessionDuration = System.currentTimeMillis() - loginTime;
 *
 *         // Save session data before player leaves
 *         database.executeAsync(
 *             "INSERT INTO sessions (player_id, duration, data) VALUES (?, ?, ?)",
 *             playerId, sessionDuration, serializeData(sessionData)
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h3>Task Cancellation</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class ScheduledTaskService {
 *     private final List<ScheduledTask> tasks = new ArrayList<>();
 *
 *     public ScheduledTask schedule(Runnable task, Duration interval) {
 *         ScheduledTask scheduled = scheduler.runRepeating(task, interval);
 *         tasks.add(scheduled);
 *         return scheduled;
 *     }
 *
 *     @PreDestroy
 *     public void cancelAllTasks() {
 *         for (ScheduledTask task : tasks) {
 *             task.cancel();
 *         }
 *         tasks.clear();
 *     }
 * }
 * }</pre>
 *
 * <h3>Cache Flush</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class DataCache {
 *     @Inject
 *     private DatabaseService database;
 *
 *     private final Map<String, CachedData> dirtyCache = new ConcurrentHashMap<>();
 *
 *     public void update(String key, CachedData data) {
 *         dirtyCache.put(key, data);
 *     }
 *
 *     @PreDestroy
 *     public void flushCache() {
 *         // Ensure all dirty data is persisted before shutdown
 *         for (Map.Entry<String, CachedData> entry : dirtyCache.entrySet()) {
 *             try {
 *                 database.save(entry.getKey(), entry.getValue());
 *             } catch (Exception e) {
 *                 // Log but continue - don't let one failure prevent others
 *                 logger.error("Failed to save cache entry: " + entry.getKey(), e);
 *             }
 *         }
 *         dirtyCache.clear();
 *     }
 * }
 * }</pre>
 *
 * <h2>Execution Order</h2>
 * <p>When multiple {@code @PreDestroy} methods exist (including inherited ones),
 * they are executed in reverse order of declaration:</p>
 * <ol>
 *   <li>Subclass @PreDestroy methods (in reverse declaration order)</li>
 *   <li>Superclass @PreDestroy methods (in reverse declaration order)</li>
 * </ol>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Keep cleanup logic fast to avoid blocking shutdown</li>
 *   <li>Catch and log exceptions rather than throwing them</li>
 *   <li>Use timeouts for operations that might hang</li>
 *   <li>Avoid accessing other services that may already be destroyed</li>
 *   <li>Mark resources as destroyed to prevent use-after-free issues</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PostConstruct
 * @see OnReload
 * @see sh.pcx.unified.inject.lifecycle.LifecycleProcessor
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {

    /**
     * Priority of this cleanup method relative to others.
     *
     * <p>Higher values execute first during cleanup (reverse of initialization).
     * Default is 0. Useful when cleanup order matters.</p>
     *
     * @return the priority value
     */
    int priority() default 0;

    /**
     * Timeout in milliseconds for the cleanup method.
     *
     * <p>If the method takes longer than this timeout, it will be interrupted.
     * Default is 5000ms (5 seconds). Use -1 for no timeout.</p>
     *
     * @return the timeout in milliseconds
     */
    long timeout() default 5000;
}
