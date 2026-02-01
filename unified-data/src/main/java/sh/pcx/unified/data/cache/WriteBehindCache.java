/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A cache that buffers writes and flushes them asynchronously in batches.
 *
 * <p>WriteBehindCache extends LocalCache with write-behind (asynchronous write)
 * capabilities. Changes are buffered in memory and periodically flushed to
 * the underlying storage in batches, improving write throughput.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a write-behind cache
 * WriteBehindCache<UUID, PlayerData> cache = WriteBehindCache.<UUID, PlayerData>builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .writer(entries -> database.batchSave(entries))
 *     .writeDelay(Duration.ofSeconds(5))
 *     .batchSize(100)
 *     .build();
 *
 * // Writes are buffered
 * cache.put(uuid, playerData);  // Returns immediately
 *
 * // Force flush pending writes
 * cache.flush();
 *
 * // Shutdown with final flush
 * cache.shutdown();
 * }</pre>
 *
 * <h2>Write Modes</h2>
 * <ul>
 *   <li><b>Write-Behind</b>: Writes are buffered and flushed asynchronously (default)</li>
 *   <li><b>Write-Through</b>: Writes are immediately persisted (use CacheWriter directly)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe. The writer is invoked from a background
 * thread, never from the calling thread.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see CacheWriter
 * @see LocalCache
 */
public class WriteBehindCache<K, V> extends LocalCache<K, V> {

    private final CacheWriter<K, V> writer;
    private final Duration writeDelay;
    private final int batchSize;
    private final boolean coalesceWrites;

    private final Map<K, V> pendingWrites;
    private final ReentrantLock writeLock;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean shutdown;

    private ScheduledFuture<?> flushTask;

    /**
     * Creates a new write-behind cache.
     *
     * @param config          the cache configuration
     * @param writer          the cache writer
     * @param writeDelay      the delay before flushing writes
     * @param batchSize       the maximum batch size
     * @param coalesceWrites  whether to coalesce multiple writes to the same key
     * @param conflictResolver optional conflict resolver
     */
    private WriteBehindCache(
            @NotNull CacheConfig config,
            @NotNull CacheWriter<K, V> writer,
            @NotNull Duration writeDelay,
            int batchSize,
            boolean coalesceWrites,
            @Nullable ConflictResolver<V> conflictResolver) {
        super(config, conflictResolver, null);
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
        this.writeDelay = Objects.requireNonNull(writeDelay, "writeDelay cannot be null");
        this.batchSize = batchSize > 0 ? batchSize : 100;
        this.coalesceWrites = coalesceWrites;

        this.pendingWrites = new ConcurrentHashMap<>();
        this.writeLock = new ReentrantLock();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-write-behind-" + config.name());
            t.setDaemon(true);
            return t;
        });
        this.shutdown = new AtomicBoolean(false);

        scheduleFlush();
    }

    /**
     * Schedules the periodic flush task.
     */
    private void scheduleFlush() {
        this.flushTask = scheduler.scheduleWithFixedDelay(
                this::flushPending,
                writeDelay.toMillis(),
                writeDelay.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Returns the cache writer.
     *
     * @return the writer
     * @since 1.0.0
     */
    @NotNull
    public CacheWriter<K, V> writer() {
        return writer;
    }

    /**
     * Returns the write delay.
     *
     * @return the delay before flushing
     * @since 1.0.0
     */
    @NotNull
    public Duration writeDelay() {
        return writeDelay;
    }

    /**
     * Returns the batch size.
     *
     * @return the maximum batch size
     * @since 1.0.0
     */
    public int batchSize() {
        return batchSize;
    }

    /**
     * Returns the number of pending writes.
     *
     * @return the pending write count
     * @since 1.0.0
     */
    public int pendingCount() {
        return pendingWrites.size();
    }

    /**
     * Checks if there are pending writes.
     *
     * @return true if writes are pending
     * @since 1.0.0
     */
    public boolean hasPendingWrites() {
        return !pendingWrites.isEmpty();
    }

    @Override
    public void put(@NotNull K key, @NotNull V value) {
        super.put(key, value);
        queueWrite(key, value);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> entries) {
        super.putAll(entries);
        entries.forEach(this::queueWrite);
    }

    /**
     * Queues a write for later flushing.
     */
    private void queueWrite(K key, V value) {
        if (shutdown.get()) {
            throw new IllegalStateException("Cache is shutting down");
        }

        pendingWrites.put(key, value);

        // Flush immediately if we hit batch size
        if (pendingWrites.size() >= batchSize) {
            scheduler.execute(this::flushPending);
        }
    }

    /**
     * Flushes all pending writes to storage.
     *
     * <p>This method blocks until all pending writes are persisted.
     *
     * @since 1.0.0
     */
    public void flush() {
        flushPending();
    }

    /**
     * Flushes pending writes (internal implementation).
     */
    private void flushPending() {
        if (pendingWrites.isEmpty()) {
            return;
        }

        writeLock.lock();
        try {
            Map<K, V> toWrite = new HashMap<>();

            // Drain pending writes in batches
            var iterator = pendingWrites.entrySet().iterator();
            while (iterator.hasNext() && toWrite.size() < batchSize) {
                var entry = iterator.next();
                toWrite.put(entry.getKey(), entry.getValue());
                iterator.remove();
            }

            if (!toWrite.isEmpty()) {
                try {
                    writer.writeAll(toWrite);
                } catch (Exception e) {
                    // Re-queue failed writes
                    if (coalesceWrites) {
                        toWrite.forEach((k, v) -> pendingWrites.putIfAbsent(k, v));
                    } else {
                        pendingWrites.putAll(toWrite);
                    }
                    throw new RuntimeException("Failed to flush writes", e);
                }
            }

            // Flush remaining if any
            if (!pendingWrites.isEmpty()) {
                scheduler.execute(this::flushPending);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Invalidates a key and removes any pending write for it.
     */
    @Override
    public void invalidate(@NotNull K key) {
        super.invalidate(key);
        pendingWrites.remove(key);
    }

    /**
     * Invalidates multiple keys and removes pending writes.
     */
    @Override
    public void invalidateAll(@NotNull Iterable<? extends K> keys) {
        super.invalidateAll(keys);
        for (K key : keys) {
            pendingWrites.remove(key);
        }
    }

    /**
     * Invalidates all entries and clears pending writes.
     */
    @Override
    public void invalidateAll() {
        super.invalidateAll();
        pendingWrites.clear();
    }

    /**
     * Shuts down the write-behind mechanism.
     *
     * <p>This will flush all pending writes before shutting down.
     * After shutdown, no more writes can be queued.
     *
     * @since 1.0.0
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            // Cancel scheduled flush
            if (flushTask != null) {
                flushTask.cancel(false);
            }

            // Flush remaining writes
            while (!pendingWrites.isEmpty()) {
                flushPending();
            }

            // Shutdown scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Shuts down immediately without flushing.
     *
     * <p>WARNING: This will discard any pending writes.
     *
     * @since 1.0.0
     */
    public void shutdownNow() {
        if (shutdown.compareAndSet(false, true)) {
            if (flushTask != null) {
                flushTask.cancel(true);
            }
            pendingWrites.clear();
            scheduler.shutdownNow();
        }
    }

    /**
     * Checks if the cache has been shut down.
     *
     * @return true if shutdown
     * @since 1.0.0
     */
    public boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * Creates a new builder for WriteBehindCache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <K, V> Builder<K, V> writeBehindBuilder() {
        return new Builder<>();
    }

    /**
     * Builder for WriteBehindCache.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @since 1.0.0
     */
    public static final class Builder<K, V> {

        private final CacheConfig.Builder configBuilder = CacheConfig.builder();
        private CacheWriter<K, V> writer;
        private Duration writeDelay = Duration.ofSeconds(5);
        private int batchSize = 100;
        private boolean coalesceWrites = true;
        private ConflictResolver<V> conflictResolver;

        private Builder() {}

        /**
         * Sets the cache name.
         */
        @NotNull
        public Builder<K, V> name(@NotNull String name) {
            configBuilder.name(name);
            return this;
        }

        /**
         * Sets the maximum number of entries.
         */
        @NotNull
        public Builder<K, V> maximumSize(long maximumSize) {
            configBuilder.maximumSize(maximumSize);
            return this;
        }

        /**
         * Sets the time after last access before expiration.
         */
        @NotNull
        public Builder<K, V> expireAfterAccess(@NotNull Duration duration) {
            configBuilder.expireAfterAccess(duration);
            return this;
        }

        /**
         * Sets the time after write before expiration.
         */
        @NotNull
        public Builder<K, V> expireAfterWrite(@NotNull Duration duration) {
            configBuilder.expireAfterWrite(duration);
            return this;
        }

        /**
         * Enables statistics recording.
         */
        @NotNull
        public Builder<K, V> recordStats(boolean recordStats) {
            configBuilder.recordStats(recordStats);
            return this;
        }

        /**
         * Sets the cache writer.
         */
        @NotNull
        public Builder<K, V> writer(@NotNull CacheWriter<K, V> writer) {
            this.writer = writer;
            return this;
        }

        /**
         * Sets the cache writer using a batch consumer.
         */
        @NotNull
        public Builder<K, V> writer(@NotNull java.util.function.Consumer<Map<? extends K, ? extends V>> batchWriter) {
            this.writer = CacheWriter.batchOnly(batchWriter);
            return this;
        }

        /**
         * Sets the delay before flushing writes.
         */
        @NotNull
        public Builder<K, V> writeDelay(@NotNull Duration delay) {
            this.writeDelay = delay;
            return this;
        }

        /**
         * Sets the maximum batch size for writes.
         */
        @NotNull
        public Builder<K, V> batchSize(int size) {
            this.batchSize = size;
            return this;
        }

        /**
         * Sets whether to coalesce multiple writes to the same key.
         *
         * <p>When enabled (default), only the latest value for each key is written.
         * When disabled, all writes are persisted (may cause duplicates).
         */
        @NotNull
        public Builder<K, V> coalesceWrites(boolean coalesce) {
            this.coalesceWrites = coalesce;
            return this;
        }

        /**
         * Sets the conflict resolver.
         */
        @NotNull
        public Builder<K, V> conflictResolver(@Nullable ConflictResolver<V> resolver) {
            this.conflictResolver = resolver;
            return this;
        }

        /**
         * Builds the WriteBehindCache instance.
         *
         * @return a new WriteBehindCache
         * @throws IllegalStateException if writer is not set
         */
        @NotNull
        public WriteBehindCache<K, V> build() {
            if (writer == null) {
                throw new IllegalStateException("writer is required");
            }
            return new WriteBehindCache<>(
                    configBuilder.build(), writer, writeDelay, batchSize, coalesceWrites, conflictResolver);
        }
    }

    @Override
    public String toString() {
        return "WriteBehindCache{name='" + config.name() + "', size=" + size() +
               ", pending=" + pendingCount() + "}";
    }
}
