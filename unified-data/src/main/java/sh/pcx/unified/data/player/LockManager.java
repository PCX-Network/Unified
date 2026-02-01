/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages distributed locks for coordinating access across servers.
 *
 * <p>The LockManager provides a centralized interface for acquiring and
 * managing distributed locks. It uses Redis as the coordination backend
 * to ensure locks are properly shared across all servers in the network.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private LockManager lockManager;
 *
 * // Acquire a player lock
 * lockManager.acquirePlayerLock(playerId)
 *     .thenAccept(lock -> {
 *         try {
 *             // Perform operations on player data
 *             PlayerProfile profile = playerData.getProfile(playerId);
 *             // ... modify data ...
 *             profile.save().join();
 *         } finally {
 *             lock.release();
 *         }
 *     });
 *
 * // Try to acquire without blocking
 * Optional<PlayerLock> lock = lockManager.tryAcquirePlayerLock(playerId).join();
 * if (lock.isPresent()) {
 *     try {
 *         // Got the lock
 *     } finally {
 *         lock.get().release();
 *     }
 * } else {
 *     // Lock is held by another server
 * }
 *
 * // Acquire a general-purpose lock
 * lockManager.acquire("economy:transfer:" + transactionId)
 *     .thenAccept(lock -> {
 *         // ...
 *     });
 * }</pre>
 *
 * <h2>Lock Types</h2>
 * <ul>
 *   <li><b>Player Locks:</b> Protect all data for a specific player</li>
 *   <li><b>General Locks:</b> Named locks for any resource</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>Default TTL:</b> How long locks are held before auto-release (default: 30s)</li>
 *   <li><b>Acquire Timeout:</b> How long to wait when acquiring (default: 10s)</li>
 *   <li><b>Retry Interval:</b> How often to retry failed acquisitions (default: 100ms)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DistributedLock
 * @see PlayerLock
 */
public interface LockManager {

    /**
     * Returns the default lock TTL (time-to-live).
     *
     * @return the default TTL
     * @since 1.0.0
     */
    @NotNull
    Duration getDefaultTtl();

    /**
     * Returns the default acquire timeout.
     *
     * @return the default timeout
     * @since 1.0.0
     */
    @NotNull
    Duration getDefaultTimeout();

    // ==================== Player Locks ====================

    /**
     * Acquires a lock for a player's data.
     *
     * <p>Blocks until the lock is acquired or the timeout expires. Uses the
     * default TTL and timeout.
     *
     * @param playerId the player's UUID
     * @return a future containing the acquired lock
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<PlayerLock> acquirePlayerLock(@NotNull UUID playerId);

    /**
     * Acquires a lock for a player's data with custom TTL.
     *
     * @param playerId the player's UUID
     * @param ttl      how long the lock should be held
     * @return a future containing the acquired lock
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<PlayerLock> acquirePlayerLock(@NotNull UUID playerId, @NotNull Duration ttl);

    /**
     * Acquires a lock for a player's data with custom TTL and timeout.
     *
     * @param playerId the player's UUID
     * @param ttl      how long the lock should be held
     * @param timeout  how long to wait to acquire
     * @return a future containing the acquired lock
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<PlayerLock> acquirePlayerLock(@NotNull UUID playerId,
                                                     @NotNull Duration ttl,
                                                     @NotNull Duration timeout);

    /**
     * Tries to acquire a player lock without blocking.
     *
     * @param playerId the player's UUID
     * @return a future containing the lock if acquired, empty otherwise
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<PlayerLock>> tryAcquirePlayerLock(@NotNull UUID playerId);

    /**
     * Checks if a player's data is currently locked.
     *
     * @param playerId the player's UUID
     * @return true if locked by any server
     * @since 1.0.0
     */
    boolean isPlayerLocked(@NotNull UUID playerId);

    /**
     * Gets information about a player lock if it exists.
     *
     * @param playerId the player's UUID
     * @return lock info if the player is locked
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<LockInfo>> getPlayerLockInfo(@NotNull UUID playerId);

    // ==================== General Locks ====================

    /**
     * Acquires a named lock.
     *
     * @param lockId the lock identifier
     * @return a future containing the acquired lock
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<DistributedLock> acquire(@NotNull String lockId);

    /**
     * Acquires a named lock with custom TTL.
     *
     * @param lockId the lock identifier
     * @param ttl    how long the lock should be held
     * @return a future containing the acquired lock
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<DistributedLock> acquire(@NotNull String lockId, @NotNull Duration ttl);

    /**
     * Acquires a named lock with custom TTL and timeout.
     *
     * @param lockId  the lock identifier
     * @param ttl     how long the lock should be held
     * @param timeout how long to wait to acquire
     * @return a future containing the acquired lock
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<DistributedLock> acquire(@NotNull String lockId,
                                                @NotNull Duration ttl,
                                                @NotNull Duration timeout);

    /**
     * Tries to acquire a named lock without blocking.
     *
     * @param lockId the lock identifier
     * @return a future containing the lock if acquired, empty otherwise
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<DistributedLock>> tryAcquire(@NotNull String lockId);

    /**
     * Tries to acquire a named lock with custom TTL without blocking.
     *
     * @param lockId the lock identifier
     * @param ttl    how long the lock should be held
     * @return a future containing the lock if acquired, empty otherwise
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<DistributedLock>> tryAcquire(@NotNull String lockId,
                                                            @NotNull Duration ttl);

    /**
     * Checks if a lock is currently held.
     *
     * @param lockId the lock identifier
     * @return true if locked by any server
     * @since 1.0.0
     */
    boolean isLocked(@NotNull String lockId);

    /**
     * Gets information about a lock if it exists.
     *
     * @param lockId the lock identifier
     * @return lock info if locked
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<LockInfo>> getLockInfo(@NotNull String lockId);

    // ==================== Management ====================

    /**
     * Returns all locks currently held by this server.
     *
     * @return set of held lock IDs
     * @since 1.0.0
     */
    @NotNull
    Set<String> getHeldLocks();

    /**
     * Releases all locks held by this server.
     *
     * <p>This is typically called during server shutdown.
     *
     * @return a future that completes when all locks are released
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> releaseAll();

    /**
     * Returns statistics about lock operations.
     *
     * @return lock statistics
     * @since 1.0.0
     */
    @NotNull
    LockStatistics getStatistics();

    // ==================== Nested Types ====================

    /**
     * Information about a held lock.
     *
     * @since 1.0.0
     */
    interface LockInfo {

        /**
         * Returns the lock identifier.
         *
         * @return the lock ID
         */
        @NotNull
        String getLockId();

        /**
         * Returns the server holding the lock.
         *
         * @return the holder server name
         */
        @NotNull
        String getHolderServer();

        /**
         * Returns when the lock was acquired.
         *
         * @return acquisition timestamp
         */
        @NotNull
        Instant getAcquiredAt();

        /**
         * Returns when the lock expires.
         *
         * @return expiration timestamp
         */
        @NotNull
        Instant getExpiresAt();

        /**
         * Returns the remaining time until expiration.
         *
         * @return time remaining
         */
        @NotNull
        Duration getTimeRemaining();

        /**
         * Returns how long the lock has been held.
         *
         * @return hold duration
         */
        @NotNull
        Duration getHoldDuration();
    }

    /**
     * Statistics about lock operations.
     *
     * @since 1.0.0
     */
    interface LockStatistics {

        /**
         * Returns the total lock acquisitions.
         *
         * @return acquisition count
         */
        long getTotalAcquisitions();

        /**
         * Returns failed acquisition attempts (timeouts).
         *
         * @return failed count
         */
        long getFailedAcquisitions();

        /**
         * Returns the total lock releases.
         *
         * @return release count
         */
        long getTotalReleases();

        /**
         * Returns locks that expired without explicit release.
         *
         * @return expired count
         */
        long getExpiredLocks();

        /**
         * Returns currently held locks by this server.
         *
         * @return current count
         */
        int getCurrentHeldCount();

        /**
         * Returns the average lock hold time.
         *
         * @return average duration
         */
        @NotNull
        Duration getAverageHoldTime();

        /**
         * Returns the average time to acquire a lock.
         *
         * @return average acquisition time
         */
        @NotNull
        Duration getAverageAcquireTime();

        /**
         * Returns when statistics collection started.
         *
         * @return start time
         */
        @NotNull
        Instant getCollectionStartTime();
    }
}
