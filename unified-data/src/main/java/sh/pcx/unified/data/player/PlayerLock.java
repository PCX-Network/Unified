/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * A distributed lock for protecting a specific player's data.
 *
 * <p>PlayerLock is a specialized form of {@link DistributedLock} that locks
 * all data operations for a specific player. While the lock is held, other
 * servers cannot modify the player's data, ensuring consistency for compound
 * operations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Transfer money between players atomically
 * public CompletableFuture<Boolean> transfer(UUID from, UUID to, double amount) {
 *     return lockManager.acquirePlayerLock(from).thenCompose(fromLock -> {
 *         return lockManager.acquirePlayerLock(to).thenCompose(toLock -> {
 *             try {
 *                 PlayerProfile fromProfile = playerData.getProfile(from);
 *                 PlayerProfile toProfile = playerData.getProfile(to);
 *
 *                 double fromBalance = fromProfile.getData(BALANCE);
 *                 if (fromBalance < amount) {
 *                     return CompletableFuture.completedFuture(false);
 *                 }
 *
 *                 fromProfile.setData(BALANCE, fromBalance - amount);
 *                 toProfile.setData(BALANCE, toProfile.getData(BALANCE) + amount);
 *
 *                 return CompletableFuture.allOf(
 *                     fromProfile.save(),
 *                     toProfile.save()
 *                 ).thenApply(v -> true);
 *             } finally {
 *                 toLock.release();
 *                 fromLock.release();
 *             }
 *         });
 *     });
 * }
 *
 * // Simple lock usage with try-with-resources
 * try (PlayerLock lock = profile.acquireLock().join()) {
 *     // Modify player data safely
 *     int kills = profile.getData(KILLS);
 *     profile.setData(KILLS, kills + 1);
 *     profile.save().join();
 * }
 * }</pre>
 *
 * <h2>When to Use Player Locks</h2>
 * <ul>
 *   <li>Transferring resources between players</li>
 *   <li>Complex read-modify-write operations</li>
 *   <li>Operations that must not be interrupted</li>
 *   <li>Cross-server player data modifications</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. The lock can be acquired and released
 * from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DistributedLock
 * @see LockManager
 * @see PlayerProfile#acquireLock()
 */
public interface PlayerLock extends AutoCloseable {

    /**
     * Returns the UUID of the player this lock protects.
     *
     * @return the player's UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getPlayerId();

    /**
     * Returns the unique lock identifier.
     *
     * <p>This is typically "player:" followed by the player's UUID.
     *
     * @return the lock ID
     * @since 1.0.0
     */
    @NotNull
    String getLockId();

    /**
     * Returns when this lock was acquired.
     *
     * @return the acquisition timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getAcquiredAt();

    /**
     * Checks if this lock is still held.
     *
     * @return true if the lock is still active
     * @since 1.0.0
     */
    boolean isHeld();

    /**
     * Releases the lock.
     *
     * <p>After release, other servers can modify the player's data.
     * If already released, this method has no effect.
     *
     * @since 1.0.0
     */
    void release();

    /**
     * Releases the lock when used with try-with-resources.
     *
     * @since 1.0.0
     */
    @Override
    default void close() {
        release();
    }
}
