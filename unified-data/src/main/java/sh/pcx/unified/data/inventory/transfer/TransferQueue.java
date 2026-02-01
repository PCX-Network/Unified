/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Queue for managing pending inventory transfers.
 *
 * <p>TransferQueue provides a priority-based queue for inventory transfer packets.
 * It handles packet ordering, expiration, retry logic, and delivery confirmation.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Priority Queue</b>: Higher priority packets processed first</li>
 *   <li><b>Expiration</b>: Automatic removal of expired packets</li>
 *   <li><b>Retry</b>: Automatic retry with exponential backoff</li>
 *   <li><b>Persistence</b>: Optional persistence for crash recovery</li>
 *   <li><b>Callbacks</b>: Notification on success/failure</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a queue
 * TransferQueue queue = new TransferQueue();
 *
 * // Configure
 * queue.setMaxRetries(3);
 * queue.setRetryDelay(Duration.ofSeconds(5));
 *
 * // Add a packet
 * queue.enqueue(packet, result -> {
 *     if (result.isSuccess()) {
 *         log.info("Transfer completed for " + result.getPacket().getPlayerId());
 *     } else {
 *         log.warn("Transfer failed: " + result.getError());
 *     }
 * });
 *
 * // Process the queue
 * queue.startProcessing(transferService::send);
 *
 * // Check status
 * int pending = queue.getPendingCount();
 * Optional<QueuedPacket> packet = queue.getByPlayerId(playerId);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TransferPacket
 * @see InventoryTransfer
 */
public class TransferQueue {

    private final PriorityBlockingQueue<QueuedPacket> queue;
    private final Map<String, QueuedPacket> packetIndex;
    private final Map<UUID, QueuedPacket> playerIndex;
    private final AtomicLong processedCount;
    private final AtomicLong failedCount;

    private int maxRetries;
    private Duration retryDelay;
    private Duration cleanupInterval;
    private int maxQueueSize;

    private ScheduledExecutorService scheduler;
    private Consumer<TransferPacket> processor;
    private volatile boolean processing;

    /**
     * Creates a new TransferQueue with default settings.
     *
     * @since 1.0.0
     */
    public TransferQueue() {
        this.queue = new PriorityBlockingQueue<>(100, Comparator
            .comparing((QueuedPacket qp) -> qp.packet.getPriority()).reversed()
            .thenComparing(qp -> qp.enqueuedAt));
        this.packetIndex = new ConcurrentHashMap<>();
        this.playerIndex = new ConcurrentHashMap<>();
        this.processedCount = new AtomicLong(0);
        this.failedCount = new AtomicLong(0);

        this.maxRetries = 3;
        this.retryDelay = Duration.ofSeconds(5);
        this.cleanupInterval = Duration.ofMinutes(1);
        this.maxQueueSize = 10000;
        this.processing = false;
    }

    // ========== Configuration ==========

    /**
     * Sets the maximum number of retries for failed transfers.
     *
     * @param maxRetries the maximum retries
     * @since 1.0.0
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Sets the delay between retry attempts.
     *
     * @param retryDelay the retry delay
     * @since 1.0.0
     */
    public void setRetryDelay(@NotNull Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * Sets the interval for cleanup of expired packets.
     *
     * @param cleanupInterval the cleanup interval
     * @since 1.0.0
     */
    public void setCleanupInterval(@NotNull Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    /**
     * Sets the maximum queue size.
     *
     * @param maxQueueSize the maximum size
     * @since 1.0.0
     */
    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    // ========== Queue Operations ==========

    /**
     * Adds a packet to the queue.
     *
     * @param packet the packet to enqueue
     * @return the queued packet wrapper
     * @throws IllegalStateException if queue is full
     * @since 1.0.0
     */
    @NotNull
    public QueuedPacket enqueue(@NotNull TransferPacket packet) {
        return enqueue(packet, null);
    }

    /**
     * Adds a packet to the queue with a completion callback.
     *
     * @param packet   the packet to enqueue
     * @param callback callback to invoke on completion
     * @return the queued packet wrapper
     * @throws IllegalStateException if queue is full
     * @since 1.0.0
     */
    @NotNull
    public QueuedPacket enqueue(@NotNull TransferPacket packet, @Nullable Consumer<TransferResult> callback) {
        Objects.requireNonNull(packet, "Packet cannot be null");

        if (queue.size() >= maxQueueSize) {
            throw new IllegalStateException("Transfer queue is full");
        }

        // Remove any existing packet for this player
        QueuedPacket existing = playerIndex.get(packet.getPlayerId());
        if (existing != null) {
            remove(existing.packet.getPacketId());
        }

        QueuedPacket queued = new QueuedPacket(packet, callback);
        queue.offer(queued);
        packetIndex.put(packet.getPacketId(), queued);
        playerIndex.put(packet.getPlayerId(), queued);

        return queued;
    }

    /**
     * Removes a packet from the queue by ID.
     *
     * @param packetId the packet ID
     * @return true if removed
     * @since 1.0.0
     */
    public boolean remove(@NotNull String packetId) {
        QueuedPacket removed = packetIndex.remove(packetId);
        if (removed != null) {
            queue.remove(removed);
            playerIndex.remove(removed.packet.getPlayerId());
            return true;
        }
        return false;
    }

    /**
     * Gets a queued packet by ID.
     *
     * @param packetId the packet ID
     * @return the queued packet, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public Optional<QueuedPacket> getById(@NotNull String packetId) {
        return Optional.ofNullable(packetIndex.get(packetId));
    }

    /**
     * Gets a queued packet by player ID.
     *
     * @param playerId the player's UUID
     * @return the queued packet, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public Optional<QueuedPacket> getByPlayerId(@NotNull UUID playerId) {
        return Optional.ofNullable(playerIndex.get(playerId));
    }

    /**
     * Polls the next packet from the queue.
     *
     * @return the next packet, or null if empty
     * @since 1.0.0
     */
    @Nullable
    public QueuedPacket poll() {
        QueuedPacket queued = queue.poll();
        if (queued != null) {
            packetIndex.remove(queued.packet.getPacketId());
            playerIndex.remove(queued.packet.getPlayerId());
        }
        return queued;
    }

    /**
     * Peeks at the next packet without removing it.
     *
     * @return the next packet, or null if empty
     * @since 1.0.0
     */
    @Nullable
    public QueuedPacket peek() {
        return queue.peek();
    }

    /**
     * Checks if the queue contains a packet for a player.
     *
     * @param playerId the player's UUID
     * @return true if a packet exists
     * @since 1.0.0
     */
    public boolean hasPacketFor(@NotNull UUID playerId) {
        return playerIndex.containsKey(playerId);
    }

    /**
     * Clears all packets from the queue.
     *
     * @since 1.0.0
     */
    public void clear() {
        queue.clear();
        packetIndex.clear();
        playerIndex.clear();
    }

    // ========== Processing ==========

    /**
     * Starts processing the queue.
     *
     * @param processor function to process each packet
     * @since 1.0.0
     */
    public void startProcessing(@NotNull Consumer<TransferPacket> processor) {
        if (processing) {
            throw new IllegalStateException("Already processing");
        }

        this.processor = Objects.requireNonNull(processor);
        this.processing = true;
        this.scheduler = Executors.newScheduledThreadPool(2);

        // Main processing loop
        scheduler.scheduleWithFixedDelay(this::processNextPacket, 0, 100, TimeUnit.MILLISECONDS);

        // Cleanup task
        scheduler.scheduleWithFixedDelay(this::cleanup, cleanupInterval.toMillis(),
            cleanupInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Stops processing the queue.
     *
     * @since 1.0.0
     */
    public void stopProcessing() {
        processing = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Checks if the queue is currently processing.
     *
     * @return true if processing
     * @since 1.0.0
     */
    public boolean isProcessing() {
        return processing;
    }

    private void processNextPacket() {
        if (!processing) return;

        QueuedPacket queued = poll();
        if (queued == null) return;

        // Check if expired
        if (queued.packet.isExpired()) {
            handleFailure(queued, "Packet expired");
            return;
        }

        // Check if should delay (for retries)
        if (queued.nextAttemptAt != null && Instant.now().isBefore(queued.nextAttemptAt)) {
            // Re-queue for later
            queue.offer(queued);
            packetIndex.put(queued.packet.getPacketId(), queued);
            playerIndex.put(queued.packet.getPlayerId(), queued);
            return;
        }

        try {
            queued.attempts++;
            processor.accept(queued.packet);
            handleSuccess(queued);
        } catch (Exception e) {
            handleProcessingError(queued, e);
        }
    }

    private void handleSuccess(@NotNull QueuedPacket queued) {
        processedCount.incrementAndGet();
        if (queued.callback != null) {
            queued.callback.accept(TransferResult.success(queued.packet));
        }
    }

    private void handleProcessingError(@NotNull QueuedPacket queued, @NotNull Exception error) {
        if (queued.attempts < maxRetries) {
            // Schedule retry
            queued.nextAttemptAt = Instant.now().plus(
                retryDelay.multipliedBy((long) Math.pow(2, queued.attempts - 1)));
            queue.offer(queued);
            packetIndex.put(queued.packet.getPacketId(), queued);
            playerIndex.put(queued.packet.getPlayerId(), queued);
        } else {
            handleFailure(queued, "Max retries exceeded: " + error.getMessage());
        }
    }

    private void handleFailure(@NotNull QueuedPacket queued, @NotNull String error) {
        failedCount.incrementAndGet();
        if (queued.callback != null) {
            queued.callback.accept(TransferResult.failure(queued.packet, error));
        }
    }

    private void cleanup() {
        Instant now = Instant.now();
        queue.removeIf(queued -> {
            if (queued.packet.isExpired()) {
                packetIndex.remove(queued.packet.getPacketId());
                playerIndex.remove(queued.packet.getPlayerId());
                handleFailure(queued, "Packet expired");
                return true;
            }
            return false;
        });
    }

    // ========== Statistics ==========

    /**
     * Returns the number of pending packets.
     *
     * @return the pending count
     * @since 1.0.0
     */
    public int getPendingCount() {
        return queue.size();
    }

    /**
     * Returns the total number of successfully processed packets.
     *
     * @return the processed count
     * @since 1.0.0
     */
    public long getProcessedCount() {
        return processedCount.get();
    }

    /**
     * Returns the total number of failed packets.
     *
     * @return the failed count
     * @since 1.0.0
     */
    public long getFailedCount() {
        return failedCount.get();
    }

    /**
     * Returns queue statistics.
     *
     * @return the queue statistics
     * @since 1.0.0
     */
    @NotNull
    public QueueStats getStats() {
        return new QueueStats(
            queue.size(),
            processedCount.get(),
            failedCount.get(),
            playerIndex.size()
        );
    }

    /**
     * Returns all queued packets (for debugging/monitoring).
     *
     * @return unmodifiable collection of queued packets
     * @since 1.0.0
     */
    @NotNull
    public Collection<QueuedPacket> getAllQueued() {
        return Collections.unmodifiableCollection(packetIndex.values());
    }

    // ========== Nested Classes ==========

    /**
     * Wrapper for a queued transfer packet.
     *
     * @since 1.0.0
     */
    public static final class QueuedPacket {

        private final TransferPacket packet;
        private final Consumer<TransferResult> callback;
        private final Instant enqueuedAt;
        private int attempts;
        private Instant nextAttemptAt;

        private QueuedPacket(@NotNull TransferPacket packet, @Nullable Consumer<TransferResult> callback) {
            this.packet = packet;
            this.callback = callback;
            this.enqueuedAt = Instant.now();
            this.attempts = 0;
            this.nextAttemptAt = null;
        }

        /**
         * Returns the transfer packet.
         */
        @NotNull
        public TransferPacket getPacket() {
            return packet;
        }

        /**
         * Returns when this was enqueued.
         */
        @NotNull
        public Instant getEnqueuedAt() {
            return enqueuedAt;
        }

        /**
         * Returns the number of attempts.
         */
        public int getAttempts() {
            return attempts;
        }

        /**
         * Returns when the next attempt is scheduled.
         */
        @Nullable
        public Instant getNextAttemptAt() {
            return nextAttemptAt;
        }

        /**
         * Returns how long this has been in the queue.
         */
        @NotNull
        public Duration getQueueTime() {
            return Duration.between(enqueuedAt, Instant.now());
        }
    }

    /**
     * Result of a transfer attempt.
     *
     * @since 1.0.0
     */
    public record TransferResult(
        boolean success,
        @NotNull TransferPacket packet,
        @Nullable String error
    ) {
        @NotNull
        public static TransferResult success(@NotNull TransferPacket packet) {
            return new TransferResult(true, packet, null);
        }

        @NotNull
        public static TransferResult failure(@NotNull TransferPacket packet, @NotNull String error) {
            return new TransferResult(false, packet, error);
        }

        public boolean isSuccess() {
            return success;
        }

        @Nullable
        public String getError() {
            return error;
        }
    }

    /**
     * Queue statistics.
     *
     * @since 1.0.0
     */
    public record QueueStats(
        int pending,
        long processed,
        long failed,
        int uniquePlayers
    ) {}
}
