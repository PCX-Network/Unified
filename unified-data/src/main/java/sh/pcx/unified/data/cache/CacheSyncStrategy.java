/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

/**
 * Enumeration of cache synchronization strategies for distributed caching.
 *
 * <p>CacheSyncStrategy defines how cache writes are synchronized with
 * the remote cache (e.g., Redis) in a distributed environment.
 *
 * <h2>Strategy Comparison</h2>
 * <table border="1">
 *   <tr>
 *     <th>Strategy</th>
 *     <th>Consistency</th>
 *     <th>Performance</th>
 *     <th>Use Case</th>
 *   </tr>
 *   <tr>
 *     <td>WRITE_THROUGH</td>
 *     <td>Strong</td>
 *     <td>Lower (sync)</td>
 *     <td>Critical data requiring consistency</td>
 *   </tr>
 *   <tr>
 *     <td>WRITE_BEHIND</td>
 *     <td>Eventual</td>
 *     <td>Higher (async)</td>
 *     <td>High-throughput, acceptable delay</td>
 *   </tr>
 *   <tr>
 *     <td>REFRESH_AHEAD</td>
 *     <td>Eventual</td>
 *     <td>Highest</td>
 *     <td>Read-heavy workloads</td>
 *   </tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * DistributedCache<UUID, PlayerData> cache = caches.<UUID, PlayerData>distributedBuilder()
 *     .name("player-data")
 *     .syncStrategy(CacheSyncStrategy.WRITE_THROUGH)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DistributedCache
 */
public enum CacheSyncStrategy {

    /**
     * Synchronous write-through strategy.
     *
     * <p>Writes are immediately propagated to the remote cache synchronously.
     * The operation blocks until the remote write completes. This provides
     * strong consistency but with higher latency.
     *
     * <p><b>Characteristics:</b>
     * <ul>
     *   <li>Synchronous - write waits for remote confirmation</li>
     *   <li>Strong consistency - all servers see the same data</li>
     *   <li>Higher write latency</li>
     *   <li>Suitable for critical data</li>
     * </ul>
     */
    WRITE_THROUGH("write-through", true, false),

    /**
     * Asynchronous write-behind strategy.
     *
     * <p>Writes are buffered locally and propagated to the remote cache
     * asynchronously in batches. The operation returns immediately after
     * updating the local cache. This provides higher throughput but with
     * eventual consistency.
     *
     * <p><b>Characteristics:</b>
     * <ul>
     *   <li>Asynchronous - write returns immediately</li>
     *   <li>Eventual consistency - brief delay before all servers see updates</li>
     *   <li>Lower write latency</li>
     *   <li>Batched writes improve throughput</li>
     *   <li>Suitable for high-volume, non-critical data</li>
     * </ul>
     */
    WRITE_BEHIND("write-behind", false, true),

    /**
     * Refresh-ahead caching strategy.
     *
     * <p>Proactively refreshes frequently accessed entries before they
     * expire. Combined with local caching, this minimizes cache misses
     * and provides the best read performance.
     *
     * <p><b>Characteristics:</b>
     * <ul>
     *   <li>Proactive refresh before expiration</li>
     *   <li>Optimized for read-heavy workloads</li>
     *   <li>Background refresh doesn't block reads</li>
     *   <li>May increase network traffic for prefetching</li>
     * </ul>
     */
    REFRESH_AHEAD("refresh-ahead", false, false),

    /**
     * Local-only caching with manual sync.
     *
     * <p>Changes are only made to the local cache. Remote synchronization
     * must be triggered manually via pub/sub or other mechanisms.
     *
     * <p><b>Characteristics:</b>
     * <ul>
     *   <li>No automatic synchronization</li>
     *   <li>Fastest local operations</li>
     *   <li>Requires manual coordination</li>
     *   <li>Suitable when explicit control is needed</li>
     * </ul>
     */
    LOCAL_ONLY("local-only", false, false);

    private final String name;
    private final boolean synchronous;
    private final boolean batched;

    CacheSyncStrategy(String name, boolean synchronous, boolean batched) {
        this.name = name;
        this.synchronous = synchronous;
        this.batched = batched;
    }

    /**
     * Returns the display name of this strategy.
     *
     * @return the strategy name
     * @since 1.0.0
     */
    public String displayName() {
        return name;
    }

    /**
     * Returns whether this strategy uses synchronous writes.
     *
     * @return true if writes are synchronous
     * @since 1.0.0
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * Returns whether this strategy uses asynchronous writes.
     *
     * @return true if writes are asynchronous
     * @since 1.0.0
     */
    public boolean isAsynchronous() {
        return !synchronous;
    }

    /**
     * Returns whether this strategy batches writes.
     *
     * @return true if writes are batched
     * @since 1.0.0
     */
    public boolean isBatched() {
        return batched;
    }

    /**
     * Returns whether this strategy provides strong consistency.
     *
     * @return true if strongly consistent
     * @since 1.0.0
     */
    public boolean isStronglyConsistent() {
        return synchronous;
    }

    /**
     * Returns whether this strategy provides eventual consistency.
     *
     * @return true if eventually consistent
     * @since 1.0.0
     */
    public boolean isEventuallyConsistent() {
        return !synchronous && this != LOCAL_ONLY;
    }

    @Override
    public String toString() {
        return name;
    }
}
