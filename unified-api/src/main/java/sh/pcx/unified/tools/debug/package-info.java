/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Debug and profiling tools for development and troubleshooting.
 *
 * <p>This package provides comprehensive debugging capabilities including:
 * <ul>
 *   <li>{@link sh.pcx.unified.tools.debug.DebugService} - Central debug service</li>
 *   <li>{@link sh.pcx.unified.tools.debug.Trace} - Execution tracing</li>
 *   <li>{@link sh.pcx.unified.tools.debug.Profiler} - Performance profiling</li>
 *   <li>{@link sh.pcx.unified.tools.debug.MemoryMonitor} - Memory monitoring</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private DebugService debug;
 *
 * // Enable debug mode
 * debug.setDebugMode(true);
 *
 * // Trace execution
 * try (Trace trace = debug.trace("processRequest")) {
 *     processRequest();
 *     trace.checkpoint("validated");
 * }
 *
 * // Profile code
 * ProfileResult result = debug.profile("operation", () -> doWork());
 * System.out.println("Duration: " + result.duration());
 *
 * // Memory monitoring
 * MemorySnapshot snapshot = debug.memorySnapshot();
 * System.out.println(snapshot.summary());
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.tools.debug.DebugService
 */
package sh.pcx.unified.tools.debug;
