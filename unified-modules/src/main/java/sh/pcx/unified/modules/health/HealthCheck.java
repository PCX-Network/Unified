/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.health;

/**
 * Interface for performing health checks on modules.
 *
 * <p>Modules can implement this interface to provide custom health check logic
 * beyond the standard TPS-based health monitoring. This allows modules to report
 * their own health status based on internal state, external dependencies, or
 * custom metrics.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "DatabaseService")
 * public class DatabaseModule implements Initializable, HealthCheck {
 *
 *     private DatabaseConnectionPool pool;
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         this.pool = new DatabaseConnectionPool(context.getConfig());
 *     }
 *
 *     @Override
 *     public HealthStatus checkHealth() {
 *         int activeConnections = pool.getActiveConnections();
 *         int maxConnections = pool.getMaxConnections();
 *         double usagePercent = (double) activeConnections / maxConnections * 100;
 *
 *         Map<String, Object> metrics = Map.of(
 *             "activeConnections", activeConnections,
 *             "maxConnections", maxConnections,
 *             "usagePercent", usagePercent,
 *             "pendingQueries", pool.getPendingQueries()
 *         );
 *
 *         if (!pool.isConnected()) {
 *             return HealthStatus.unhealthy("Database connection lost", metrics);
 *         }
 *
 *         if (usagePercent > 90) {
 *             return HealthStatus.degraded("Connection pool near capacity", metrics);
 *         }
 *
 *         if (usagePercent > 75) {
 *             return HealthStatus.warning("High connection pool usage", metrics);
 *         }
 *
 *         return HealthStatus.healthy("Database connection pool healthy", metrics);
 *     }
 * }
 * }</pre>
 *
 * <h2>Health Check Aggregation</h2>
 * <p>The module system aggregates health checks from all modules to provide
 * an overall system health view:
 *
 * <pre>{@code
 * // Get aggregated health status
 * HealthStatus systemHealth = moduleManager.getSystemHealth();
 *
 * // Get individual module health
 * HealthStatus dbHealth = moduleManager.getModuleHealth("DatabaseService");
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Health checks should be fast and non-blocking</li>
 *   <li>Include relevant metrics for debugging</li>
 *   <li>Use appropriate status levels (HEALTHY, WARNING, DEGRADED, UNHEALTHY)</li>
 *   <li>Provide meaningful messages describing the status</li>
 *   <li>Avoid expensive operations - cache metrics if needed</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see HealthStatus
 * @see sh.pcx.unified.modules.lifecycle.Healthy
 */
@FunctionalInterface
public interface HealthCheck {

    /**
     * Performs a health check and returns the current status.
     *
     * <p>This method should:
     * <ul>
     *   <li>Execute quickly (avoid blocking I/O)</li>
     *   <li>Return current, accurate status</li>
     *   <li>Include relevant metrics for monitoring</li>
     *   <li>Not throw exceptions (return UNHEALTHY status instead)</li>
     * </ul>
     *
     * @return the current health status of this module
     */
    HealthStatus checkHealth();
}
