/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for loading cache values on cache miss.
 *
 * <p>A CacheLoader is used with {@link LoadingCache} to automatically load
 * values when they are not present in the cache. The loader is invoked
 * synchronously or asynchronously depending on the cache operation used.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple synchronous loader
 * CacheLoader<UUID, PlayerData> loader = uuid -> database.loadPlayerData(uuid);
 *
 * // Async loader using CompletableFuture
 * CacheLoader<UUID, PlayerData> asyncLoader = new CacheLoader<UUID, PlayerData>() {
 *     @Override
 *     public PlayerData load(UUID key) {
 *         return database.loadPlayerData(key);
 *     }
 *
 *     @Override
 *     public CompletableFuture<PlayerData> loadAsync(UUID key) {
 *         return CompletableFuture.supplyAsync(() -> database.loadPlayerData(key));
 *     }
 * };
 *
 * // Bulk loader for efficient batch loading
 * CacheLoader<UUID, PlayerData> bulkLoader = new CacheLoader<UUID, PlayerData>() {
 *     @Override
 *     public PlayerData load(UUID key) {
 *         return database.loadPlayerData(key);
 *     }
 *
 *     @Override
 *     public Map<UUID, PlayerData> loadAll(Set<? extends UUID> keys) {
 *         return database.loadAllPlayerData(keys);
 *     }
 * };
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as loaders may be invoked from
 * multiple threads concurrently.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see LoadingCache
 */
@FunctionalInterface
public interface CacheLoader<K, V> {

    /**
     * Loads a value for the specified key.
     *
     * <p>This method is called when a cache lookup results in a miss.
     * Implementations should return the value to be cached, or null if
     * no value exists for the key.
     *
     * @param key the key to load a value for
     * @return the loaded value, or null if not found
     * @throws Exception if the value cannot be loaded
     * @since 1.0.0
     */
    @Nullable
    V load(@NotNull K key) throws Exception;

    /**
     * Asynchronously loads a value for the specified key.
     *
     * <p>The default implementation wraps {@link #load(Object)} in a
     * CompletableFuture. Override this method to provide true async
     * loading behavior.
     *
     * @param key the key to load a value for
     * @return a future that completes with the loaded value
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<V> loadAsync(@NotNull K key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return load(key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load value for key: " + key, e);
            }
        });
    }

    /**
     * Loads values for multiple keys in a batch operation.
     *
     * <p>The default implementation loads each key individually. Override
     * this method to provide efficient bulk loading.
     *
     * <p>The returned map need not contain entries for all requested keys.
     * Keys not present in the returned map will be treated as cache misses.
     *
     * @param keys the keys to load values for
     * @return a map of loaded key-value pairs
     * @throws Exception if values cannot be loaded
     * @since 1.0.0
     */
    @NotNull
    default Map<K, V> loadAll(@NotNull Set<? extends K> keys) throws Exception {
        java.util.HashMap<K, V> result = new java.util.HashMap<>();
        for (K key : keys) {
            V value = load(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Asynchronously loads values for multiple keys.
     *
     * <p>The default implementation wraps {@link #loadAll(Set)} in a
     * CompletableFuture. Override to provide true async bulk loading.
     *
     * @param keys the keys to load values for
     * @return a future that completes with the loaded key-value map
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<Map<K, V>> loadAllAsync(@NotNull Set<? extends K> keys) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadAll(keys);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load values for keys", e);
            }
        });
    }

    /**
     * Called when a value is being refreshed in the background.
     *
     * <p>This method is invoked when a cached value is eligible for refresh
     * (based on refreshAfterWrite settings). The old value is still returned
     * to callers while the refresh is in progress.
     *
     * <p>The default implementation delegates to {@link #load(Object)}.
     *
     * @param key      the key being refreshed
     * @param oldValue the current cached value
     * @return a future that completes with the refreshed value
     * @since 1.0.0
     */
    @NotNull
    default CompletableFuture<V> reload(@NotNull K key, @NotNull V oldValue) {
        return loadAsync(key);
    }

    /**
     * Creates a simple CacheLoader from a loading function.
     *
     * @param <K>    the key type
     * @param <V>    the value type
     * @param loader the loading function
     * @return a CacheLoader that uses the provided function
     * @since 1.0.0
     */
    @NotNull
    static <K, V> CacheLoader<K, V> from(@NotNull java.util.function.Function<K, V> loader) {
        return loader::apply;
    }

    /**
     * Creates an async CacheLoader from an async loading function.
     *
     * @param <K>         the key type
     * @param <V>         the value type
     * @param asyncLoader the async loading function
     * @return a CacheLoader that uses the provided async function
     * @since 1.0.0
     */
    @NotNull
    static <K, V> CacheLoader<K, V> fromAsync(
            @NotNull java.util.function.Function<K, CompletableFuture<V>> asyncLoader) {
        return new CacheLoader<>() {
            @Override
            public V load(@NotNull K key) throws Exception {
                return asyncLoader.apply(key).get();
            }

            @Override
            @NotNull
            public CompletableFuture<V> loadAsync(@NotNull K key) {
                return asyncLoader.apply(key);
            }
        };
    }
}
