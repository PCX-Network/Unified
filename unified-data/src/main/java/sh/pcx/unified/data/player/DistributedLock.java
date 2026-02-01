/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A distributed lock for coordinating access across multiple servers.
 *
 * <p>Distributed locks use Redis to ensure that only one server can hold
 * a lock at a time. This is essential for preventing data corruption when
 * multiple servers might modify the same player's data simultaneously.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Using try-with-resources (recommended)
 * try (DistributedLock lock = lockManager.acquire("player:" + uuid).join()) {
 *     // Critical section - only one server can execute this at a time
 *     PlayerProfile profile = playerData.getProfile(uuid);
 *     double balance = profile.getData(BALANCE);
 *     profile.setData(BALANCE, balance - amount);
 *     profile.save().join();
 * }
 * // Lock automatically released
 *
 * // With timeout
 * Optional<DistributedLock> maybeLock = lockManager.tryAcquire("resource:123",
 *     Duration.ofSeconds(5)).join();
 *
 * maybeLock.ifPresentOrElse(
 *     lock -> {
 *         try {
 *             // Use the lock
 *         } finally {
 *             lock.release();
 *         }
 *     },
 *     () -> {
 *         // Could not acquire lock
 *         logger.warn("Resource is locked by another server");
 *     }
 * );
 * }</pre>
 *
 * <h2>Lock Properties</h2>
 * <ul>
 *   <li><b>Mutual Exclusion:</b> Only one holder at a time</li>
 *   <li><b>Deadlock Prevention:</b> Locks have automatic expiration (TTL)</li>
 *   <li><b>Fault Tolerance:</b> Lock is released if holder crashes</li>
 *   <li><b>Reentrancy:</b> Not reentrant - same server cannot acquire twice</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. The lock can be released from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LockManager
 * @see PlayerLock
 */
public interface DistributedLock extends AutoCloseable {

    /**
     * Returns the unique identifier of this lock.
     *
     * <p>The lock ID is typically a namespaced key like "player:uuid" or
     * "resource:name".
     *
     * @return the lock identifier
     * @since 1.0.0
     */
    @NotNull
    String getLockId();

    /**
     * Returns the unique token for this lock acquisition.
     *
     * <p>The token is used internally to ensure only the rightful owner
     * can release the lock.
     *
     * @return the lock token
     * @since 1.0.0
     */
    @NotNull
    String getLockToken();

    /**
     * Returns when this lock was acquired.
     *
     * @return the acquisition timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getAcquiredAt();

    /**
     * Returns when this lock will expire.
     *
     * <p>After expiration, the lock is automatically released even if not
     * explicitly released. This prevents deadlocks if the holder crashes.
     *
     * @return the expiration timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getExpiresAt();

    /**
     * Returns the time remaining until the lock expires.
     *
     * @return the remaining time, or Duration.ZERO if expired
     * @since 1.0.0
     */
    @NotNull
    Duration getTimeRemaining();

    /**
     * Returns the name of the server holding this lock.
     *
     * @return the holder server name
     * @since 1.0.0
     */
    @NotNull
    String getHolderServer();

    /**
     * Returns how long this lock has been held.
     *
     * @return the hold duration
     * @since 1.0.0
     */
    @NotNull
    Duration getHoldDuration();

    /**
     * Checks if this lock is still held (not released or expired).
     *
     * @return true if the lock is still held
     * @since 1.0.0
     */
    boolean isHeld();

    /**
     * Checks if this lock has expired.
     *
     * <p>An expired lock may still report isHeld() as true if it hasn't been
     * checked against Redis yet. Use {@link #refresh()} to get the latest state.
     *
     * @return true if the TTL has passed
     * @since 1.0.0
     */
    boolean isExpired();

    /**
     * Releases the lock.
     *
     * <p>After release, other servers can acquire the lock. If the lock has
     * already been released or expired, this method has no effect.
     *
     * @since 1.0.0
     */
    void release();

    /**
     * Releases the lock asynchronously.
     *
     * @return a future that completes when released
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> releaseAsync();

    /**
     * Extends the lock's TTL.
     *
     * <p>Use this for long-running operations to prevent the lock from expiring.
     * The extension is added to the current expiration time.
     *
     * @param extension the additional time to hold the lock
     * @return true if the extension was successful
     * @throws IllegalStateException if the lock is no longer held
     * @since 1.0.0
     */
    boolean extend(@NotNull Duration extension);

    /**
     * Extends the lock's TTL asynchronously.
     *
     * @param extension the additional time
     * @return a future containing true if successful
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> extendAsync(@NotNull Duration extension);

    /**
     * Refreshes the lock state from Redis.
     *
     * <p>Updates the local state to reflect whether the lock is still held
     * in Redis. This is useful for checking if another server has stolen
     * the lock or if it has expired.
     *
     * @return true if the lock is still held
     * @since 1.0.0
     */
    boolean refresh();

    /**
     * Refreshes the lock state asynchronously.
     *
     * @return a future containing true if still held
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> refreshAsync();

    /**
     * Releases the lock when used with try-with-resources.
     *
     * <p>This calls {@link #release()} and does not throw exceptions.
     *
     * @since 1.0.0
     */
    @Override
    default void close() {
        release();
    }

    /**
     * Awaits until the lock is released by another holder.
     *
     * <p>This does NOT acquire the lock, only waits for it to become available.
     *
     * @param timeout maximum time to wait
     * @param unit    time unit
     * @return true if the lock became available within the timeout
     * @since 1.0.0
     */
    boolean awaitRelease(long timeout, @NotNull TimeUnit unit);

    /**
     * Awaits until the lock is released by another holder asynchronously.
     *
     * @param timeout maximum time to wait
     * @return a future that completes with true if available
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> awaitReleaseAsync(@NotNull Duration timeout);
}
