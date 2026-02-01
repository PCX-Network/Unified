/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for writing cache values to persistent storage.
 *
 * <p>A CacheWriter is used with write-through and write-behind caches to
 * persist cache changes to an underlying data store. The writer can operate
 * synchronously or asynchronously depending on the cache configuration.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple synchronous writer
 * CacheWriter<UUID, PlayerData> writer = new CacheWriter<>() {
 *     @Override
 *     public void write(UUID key, PlayerData value) {
 *         database.savePlayerData(key, value);
 *     }
 *
 *     @Override
 *     public void delete(UUID key) {
 *         database.deletePlayerData(key);
 *     }
 * };
 *
 * // Batch writer for write-behind caching
 * CacheWriter<UUID, PlayerData> batchWriter = new CacheWriter<>() {
 *     @Override
 *     public void write(UUID key, PlayerData value) {
 *         database.savePlayerData(key, value);
 *     }
 *
 *     @Override
 *     public void writeAll(Map<? extends UUID, ? extends PlayerData> entries) {
 *         database.batchSave(entries);
 *     }
 *
 *     @Override
 *     public void delete(UUID key) {
 *         database.deletePlayerData(key);
 *     }
 *
 *     @Override
 *     public void deleteAll(Collection<? extends UUID> keys) {
 *         database.batchDelete(keys);
 *     }
 * };
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as writers may be invoked from
 * multiple threads concurrently, especially in write-behind scenarios.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see WriteBehindCache
 */
public interface CacheWriter<K, V> {

    /**
     * Writes a single entry to the underlying store.
     *
     * <p>This method is called synchronously when write-through caching is
     * enabled, or batched asynchronously for write-behind caching.
     *
     * @param key   the key to write
     * @param value the value to write
     * @throws Exception if the write fails
     * @since 1.0.0
     */
    void write(@NotNull K key, @NotNull V value) throws Exception;

    /**
     * Asynchronously writes a single entry.
     *
     * <p>The default implementation wraps {@link #write(Object, Object)} in
     * a CompletableFuture. Override for true async behavior.
     *
     * @param key   the key to write
     * @param value the value to write
     * @return a future that completes when the write is finished
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> writeAsync(@NotNull K key, @NotNull V value) {
        return CompletableFuture.runAsync(() -> {
            try {
                write(key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write entry: " + key, e);
            }
        });
    }

    /**
     * Writes multiple entries in a batch operation.
     *
     * <p>The default implementation writes each entry individually. Override
     * this method to provide efficient bulk writing.
     *
     * @param entries the entries to write
     * @throws Exception if the batch write fails
     * @since 1.0.0
     */
    default void writeAll(@NotNull Map<? extends K, ? extends V> entries) throws Exception {
        for (Map.Entry<? extends K, ? extends V> entry : entries.entrySet()) {
            write(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Asynchronously writes multiple entries.
     *
     * <p>The default implementation wraps {@link #writeAll(Map)} in a
     * CompletableFuture. Override for true async bulk writing.
     *
     * @param entries the entries to write
     * @return a future that completes when all writes are finished
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> writeAllAsync(@NotNull Map<? extends K, ? extends V> entries) {
        return CompletableFuture.runAsync(() -> {
            try {
                writeAll(entries);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write batch entries", e);
            }
        });
    }

    /**
     * Deletes a single entry from the underlying store.
     *
     * <p>This method is called when an entry is explicitly removed from
     * the cache and should be deleted from the backing store.
     *
     * @param key the key to delete
     * @throws Exception if the delete fails
     * @since 1.0.0
     */
    void delete(@NotNull K key) throws Exception;

    /**
     * Asynchronously deletes a single entry.
     *
     * <p>The default implementation wraps {@link #delete(Object)} in a
     * CompletableFuture. Override for true async behavior.
     *
     * @param key the key to delete
     * @return a future that completes when the delete is finished
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> deleteAsync(@NotNull K key) {
        return CompletableFuture.runAsync(() -> {
            try {
                delete(key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete entry: " + key, e);
            }
        });
    }

    /**
     * Deletes multiple entries in a batch operation.
     *
     * <p>The default implementation deletes each entry individually. Override
     * this method to provide efficient bulk deletion.
     *
     * @param keys the keys to delete
     * @throws Exception if the batch delete fails
     * @since 1.0.0
     */
    default void deleteAll(@NotNull Collection<? extends K> keys) throws Exception {
        for (K key : keys) {
            delete(key);
        }
    }

    /**
     * Asynchronously deletes multiple entries.
     *
     * <p>The default implementation wraps {@link #deleteAll(Collection)} in a
     * CompletableFuture. Override for true async bulk deletion.
     *
     * @param keys the keys to delete
     * @return a future that completes when all deletes are finished
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Void> deleteAllAsync(@NotNull Collection<? extends K> keys) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteAll(keys);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete batch entries", e);
            }
        });
    }

    /**
     * Creates a simple CacheWriter from write and delete functions.
     *
     * @param <K>          the key type
     * @param <V>          the value type
     * @param writeHandler the write function
     * @param deleteHandler the delete function
     * @return a CacheWriter using the provided functions
     * @since 1.0.0
     */
    @NotNull
    static <K, V> CacheWriter<K, V> of(
            @NotNull java.util.function.BiConsumer<K, V> writeHandler,
            @NotNull java.util.function.Consumer<K> deleteHandler) {
        return new CacheWriter<>() {
            @Override
            public void write(@NotNull K key, @NotNull V value) {
                writeHandler.accept(key, value);
            }

            @Override
            public void delete(@NotNull K key) {
                deleteHandler.accept(key);
            }
        };
    }

    /**
     * Creates a write-only CacheWriter (deletes are no-ops).
     *
     * @param <K>          the key type
     * @param <V>          the value type
     * @param writeHandler the write function
     * @return a CacheWriter that only handles writes
     * @since 1.0.0
     */
    @NotNull
    static <K, V> CacheWriter<K, V> writeOnly(@NotNull java.util.function.BiConsumer<K, V> writeHandler) {
        return new CacheWriter<>() {
            @Override
            public void write(@NotNull K key, @NotNull V value) {
                writeHandler.accept(key, value);
            }

            @Override
            public void delete(@NotNull K key) {
                // No-op for write-only writer
            }
        };
    }

    /**
     * Creates a batch-only CacheWriter for write-behind scenarios.
     *
     * <p>Individual writes are accumulated and written via the batch method.
     *
     * @param <K>         the key type
     * @param <V>         the value type
     * @param batchWriter the batch write function
     * @return a CacheWriter optimized for batch operations
     * @since 1.0.0
     */
    @NotNull
    static <K, V> CacheWriter<K, V> batchOnly(
            @NotNull java.util.function.Consumer<Map<? extends K, ? extends V>> batchWriter) {
        return new CacheWriter<>() {
            @Override
            public void write(@NotNull K key, @NotNull V value) throws Exception {
                writeAll(Map.of(key, value));
            }

            @Override
            public void writeAll(@NotNull Map<? extends K, ? extends V> entries) {
                batchWriter.accept(entries);
            }

            @Override
            public void delete(@NotNull K key) {
                // Batch writers typically don't handle individual deletes
            }
        };
    }
}
