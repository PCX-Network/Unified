/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Health monitoring system for modules.
 *
 * <p>This package provides components for monitoring server health (TPS)
 * and notifying modules when they should adapt their behavior:
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.health.TPSTracker} -
 *       Tracks server TPS over time with averaging and statistics</li>
 *   <li>{@link sh.pcx.unified.modules.health.HealthContext} -
 *       Context passed to modules during health state transitions</li>
 *   <li>{@link sh.pcx.unified.modules.health.HealthCheck} -
 *       Interface for modules to provide custom health checks</li>
 *   <li>{@link sh.pcx.unified.modules.health.HealthStatus} -
 *       Record representing health status with level, message, and metrics</li>
 *   <li>{@link sh.pcx.unified.modules.health.HealthLevel} -
 *       Enum for health levels: HEALTHY, WARNING, DEGRADED, UNHEALTHY</li>
 * </ul>
 *
 * <h2>Health Monitoring Flow</h2>
 * <pre>
 * TPS Normal (20.0)
 *       |
 *       v
 * Module Operating Normally
 *       |
 * TPS drops below threshold (e.g., < 18.0)
 *       |
 *       v
 * ifUnhealthy() called
 *       |
 *       v
 * Module in Low Performance Mode
 *       |
 * TPS recovers above recovery threshold (e.g., > 19.5)
 *       |
 *       v
 * ifBackToHealth() called
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "Particles")
 * public class ParticlesModule implements Healthy {
 *
 *     private double particleMultiplier = 1.0;
 *
 *     @Override
 *     public void ifUnhealthy(HealthContext context) {
 *         // Reduce particles when TPS is low
 *         particleMultiplier = 0.1;
 *         context.getLogger().warn("Reducing particles (TPS: " + context.getTps() + ")");
 *     }
 *
 *     @Override
 *     public void ifBackToHealth(HealthContext context) {
 *         // Restore normal particles
 *         particleMultiplier = 1.0;
 *         context.getLogger().info("Resuming normal particle effects");
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
package sh.pcx.unified.modules.health;
