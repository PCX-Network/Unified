/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Platform detection and scheduler adaptation.
 *
 * <p>This package handles automatic detection of the server platform
 * and provides adapters for platform-specific scheduler APIs:
 * <ul>
 *   <li>{@link sh.pcx.unified.scheduler.detection.SchedulerDetector} -
 *       Detects Folia, Paper, Spigot, or Sponge</li>
 *   <li>{@link sh.pcx.unified.scheduler.detection.SchedulerAdapter} -
 *       Adapts platform schedulers to unified interface</li>
 * </ul>
 *
 * <h2>Platform Detection</h2>
 * <pre>{@code
 * SchedulerType type = SchedulerDetector.detect();
 *
 * if (SchedulerDetector.isFolia()) {
 *     log.info("Running on Folia - region-aware scheduling enabled");
 * }
 *
 * if (SchedulerDetector.supportsAsyncChunks()) {
 *     log.info("Async chunk loading available");
 * }
 * }</pre>
 *
 * <h2>Adapter Usage</h2>
 * <p>SchedulerAdapter is typically used internally by SchedulerService
 * implementations. Users should use the high-level SchedulerService API.
 *
 * <h2>Supported Platforms</h2>
 * <table border="1">
 *   <tr>
 *     <th>Platform</th>
 *     <th>Region Threading</th>
 *     <th>Async Chunks</th>
 *   </tr>
 *   <tr><td>Folia</td><td>Yes</td><td>Yes</td></tr>
 *   <tr><td>Paper</td><td>No</td><td>Yes</td></tr>
 *   <tr><td>Spigot</td><td>No</td><td>No</td></tr>
 *   <tr><td>Sponge</td><td>No</td><td>Yes</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.scheduler.detection;
