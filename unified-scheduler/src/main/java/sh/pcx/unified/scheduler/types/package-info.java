/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Standard task type implementations.
 *
 * <p>This package contains the core task types that work on all platforms:
 * <ul>
 *   <li>{@link sh.pcx.unified.scheduler.types.SyncTask} - Main thread tasks</li>
 *   <li>{@link sh.pcx.unified.scheduler.types.AsyncTask} - Background thread tasks</li>
 *   <li>{@link sh.pcx.unified.scheduler.types.DelayedTask} - One-time delayed tasks</li>
 *   <li>{@link sh.pcx.unified.scheduler.types.RepeatingTask} - Periodic tasks</li>
 * </ul>
 *
 * <h2>Choosing a Task Type</h2>
 * <table border="1">
 *   <tr>
 *     <th>Task Type</th>
 *     <th>Use Case</th>
 *   </tr>
 *   <tr>
 *     <td>SyncTask</td>
 *     <td>Accessing world state, entity modification, player interaction</td>
 *   </tr>
 *   <tr>
 *     <td>AsyncTask</td>
 *     <td>Database queries, file I/O, HTTP requests, heavy computation</td>
 *   </tr>
 *   <tr>
 *     <td>DelayedTask</td>
 *     <td>Countdown timers, deferred actions, debouncing</td>
 *   </tr>
 *   <tr>
 *     <td>RepeatingTask</td>
 *     <td>Scoreboard updates, auto-save, particle effects</td>
 *   </tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.scheduler.folia
 */
package sh.pcx.unified.scheduler.types;
