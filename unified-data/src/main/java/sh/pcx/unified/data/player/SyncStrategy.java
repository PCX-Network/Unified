/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

/**
 * Defines strategies for synchronizing player data across servers.
 *
 * <p>The sync strategy determines when and how data changes are propagated
 * to other servers in a network. Different strategies offer trade-offs between
 * consistency, latency, and resource usage.
 *
 * <h2>Strategy Comparison</h2>
 * <table>
 *   <tr>
 *     <th>Strategy</th>
 *     <th>Latency</th>
 *     <th>Consistency</th>
 *     <th>Resource Usage</th>
 *     <th>Use Case</th>
 *   </tr>
 *   <tr>
 *     <td>EAGER</td>
 *     <td>Immediate</td>
 *     <td>High</td>
 *     <td>High</td>
 *     <td>Critical data (balance, permissions)</td>
 *   </tr>
 *   <tr>
 *     <td>LAZY</td>
 *     <td>Batched</td>
 *     <td>Eventual</td>
 *     <td>Low</td>
 *     <td>Statistics, non-critical data</td>
 *   </tr>
 *   <tr>
 *     <td>ON_DEMAND</td>
 *     <td>On access</td>
 *     <td>Fresh</td>
 *     <td>Variable</td>
 *     <td>Infrequently accessed data</td>
 *   </tr>
 *   <tr>
 *     <td>NONE</td>
 *     <td>N/A</td>
 *     <td>Server-local</td>
 *     <td>None</td>
 *     <td>Server-specific data</td>
 *   </tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Critical data - sync immediately
 * PersistentDataKey<Double> BALANCE = PersistentDataKey.builder("balance", Double.class)
 *     .syncStrategy(SyncStrategy.EAGER)
 *     .build();
 *
 * // Statistics - eventual consistency is fine
 * PersistentDataKey<Integer> KILLS = PersistentDataKey.builder("kills", Integer.class)
 *     .syncStrategy(SyncStrategy.LAZY)
 *     .build();
 *
 * // Profile data - load fresh when needed
 * PersistentDataKey<String> BIO = PersistentDataKey.builder("bio", String.class)
 *     .syncStrategy(SyncStrategy.ON_DEMAND)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CrossServerSync
 * @see PersistentDataKey
 */
public enum SyncStrategy {

    /**
     * Synchronize changes immediately when they occur.
     *
     * <p>When data is modified, the change is immediately published to Redis
     * and propagated to all other servers. This ensures strong consistency
     * but has higher network overhead.
     *
     * <h3>Characteristics:</h3>
     * <ul>
     *   <li>Changes are visible on other servers within milliseconds</li>
     *   <li>Each write triggers a Redis publish</li>
     *   <li>Best for critical, frequently-read data</li>
     * </ul>
     *
     * <h3>Use Cases:</h3>
     * <ul>
     *   <li>Economy balance</li>
     *   <li>Permissions and ranks</li>
     *   <li>Ban/mute status</li>
     *   <li>Party/guild membership</li>
     * </ul>
     *
     * @since 1.0.0
     */
    EAGER,

    /**
     * Batch changes and synchronize periodically.
     *
     * <p>Changes are collected and synchronized in batches at regular intervals
     * (e.g., every 5 seconds). This reduces network overhead but means changes
     * may not be immediately visible on other servers.
     *
     * <h3>Characteristics:</h3>
     * <ul>
     *   <li>Changes may take several seconds to propagate</li>
     *   <li>Multiple changes are batched into single operations</li>
     *   <li>Lower network and CPU overhead</li>
     * </ul>
     *
     * <h3>Use Cases:</h3>
     * <ul>
     *   <li>Kill/death statistics</li>
     *   <li>Play time tracking</li>
     *   <li>Achievement progress</li>
     *   <li>Non-critical game state</li>
     * </ul>
     *
     * @since 1.0.0
     */
    LAZY,

    /**
     * Synchronize only when data is explicitly requested.
     *
     * <p>Data is not actively pushed to other servers. Instead, when another
     * server needs the data, it requests the latest version from the database
     * or cache. This is useful for data that is written often but read rarely.
     *
     * <h3>Characteristics:</h3>
     * <ul>
     *   <li>No automatic synchronization</li>
     *   <li>Data is always fresh when requested</li>
     *   <li>Higher latency on first access</li>
     *   <li>Lowest background resource usage</li>
     * </ul>
     *
     * <h3>Use Cases:</h3>
     * <ul>
     *   <li>Player profile/biography</li>
     *   <li>Settings and preferences</li>
     *   <li>Historical data</li>
     *   <li>Large data structures</li>
     * </ul>
     *
     * @since 1.0.0
     */
    ON_DEMAND,

    /**
     * Do not synchronize across servers.
     *
     * <p>Data with this strategy is only stored locally on the current server
     * and in the database. Other servers will not receive real-time updates
     * and will load from the database when needed.
     *
     * <h3>Characteristics:</h3>
     * <ul>
     *   <li>No cross-server communication</li>
     *   <li>Data persists to database but not synced</li>
     *   <li>Each server has its own cached copy</li>
     * </ul>
     *
     * <h3>Use Cases:</h3>
     * <ul>
     *   <li>Server-specific settings</li>
     *   <li>Local caches</li>
     *   <li>Data that doesn't need real-time sync</li>
     * </ul>
     *
     * @since 1.0.0
     */
    NONE;

    /**
     * Returns whether this strategy involves any cross-server synchronization.
     *
     * @return true if data is synchronized to other servers
     * @since 1.0.0
     */
    public boolean isSynced() {
        return this != NONE;
    }

    /**
     * Returns whether this strategy synchronizes immediately on write.
     *
     * @return true if changes are propagated immediately
     * @since 1.0.0
     */
    public boolean isImmediate() {
        return this == EAGER;
    }

    /**
     * Returns whether this strategy batches synchronization.
     *
     * @return true if changes are batched
     * @since 1.0.0
     */
    public boolean isBatched() {
        return this == LAZY;
    }

    /**
     * Returns the recommended sync interval in milliseconds for batched strategies.
     *
     * <p>Returns 0 for non-batched strategies.
     *
     * @return the sync interval in milliseconds
     * @since 1.0.0
     */
    public long getSyncIntervalMs() {
        return switch (this) {
            case LAZY -> 5000L; // 5 seconds
            default -> 0L;
        };
    }
}
