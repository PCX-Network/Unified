/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A thread-safe map implementation where entries automatically expire after a TTL.
 *
 * <p>Entries are automatically removed when their time-to-live (TTL) expires.
 * This is useful for caching, rate limiting, and temporary data storage.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a map with 5-minute TTL
 * ExpiringMap<UUID, PlayerData> cache = new ExpiringMap<>(5, TimeUnit.MINUTES);
 *
 * // Add entries (they expire after 5 minutes)
 * cache.put(player.getUniqueId(), data);
 *
 * // Get entries (returns null if expired)
 * PlayerData data = cache.get(player.getUniqueId());
 *
 * // Add with custom TTL
 * cache.put(player.getUniqueId(), data, Duration.ofMinutes(10));
 *
 * // Register expiration listener
 * cache.onExpire((key, value) -> {
 *     System.out.println("Entry expired: " + key);
 * });
 * }</pre>
 *
 * <h2>Expiration Modes</h2>
 * <ul>
 *   <li><b>CREATED</b> - Entries expire after a fixed time from creation</li>
 *   <li><b>ACCESSED</b> - Entries expire after a fixed time from last access</li>
 *   <li><b>MODIFIED</b> - Entries expire after a fixed time from last modification</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is fully thread-safe and can be accessed from
 * multiple threads concurrently.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @since 1.0.0
 * @author Supatuck
 */
public final class ExpiringMap<K, V> implements Map<K, V> {

    /**
     * Expiration policy modes.
     */
    public enum ExpirationPolicy {
        /** Entries expire after a fixed time from creation */
        CREATED,
        /** Entries expire after a fixed time from last access */
        ACCESSED,
        /** Entries expire after a fixed time from last modification */
        MODIFIED
    }

    /**
     * Internal entry wrapper with expiration tracking.
     */
    private record ExpiringEntry<V>(
            V value,
            Instant expiration,
            Instant created,
            Instant lastAccessed,
            Instant lastModified
    ) {
        ExpiringEntry<V> withAccessed(Instant time) {
            return new ExpiringEntry<>(value, expiration, created, time, lastModified);
        }

        ExpiringEntry<V> withValue(V newValue, Instant newExpiration) {
            Instant now = Instant.now();
            return new ExpiringEntry<>(newValue, newExpiration, created, now, now);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiration);
        }

        Duration remainingTime() {
            Duration remaining = Duration.between(Instant.now(), expiration);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        }
    }

    private final Map<K, ExpiringEntry<V>> entries;
    private final ReadWriteLock lock;
    private final Duration defaultTtl;
    private final ExpirationPolicy policy;
    private final ScheduledExecutorService scheduler;
    private final List<BiConsumer<K, V>> expirationListeners;
    private final boolean ownScheduler;
    private volatile boolean closed;

    /**
     * Creates a new ExpiringMap with the specified default TTL.
     *
     * @param ttl      the default time-to-live
     * @param timeUnit the time unit
     * @since 1.0.0
     */
    public ExpiringMap(long ttl, @NotNull TimeUnit timeUnit) {
        this(Duration.of(ttl, timeUnit.toChronoUnit()), ExpirationPolicy.CREATED);
    }

    /**
     * Creates a new ExpiringMap with the specified default TTL and policy.
     *
     * @param defaultTtl the default time-to-live
     * @param policy     the expiration policy
     * @since 1.0.0
     */
    public ExpiringMap(@NotNull Duration defaultTtl, @NotNull ExpirationPolicy policy) {
        this(defaultTtl, policy, null);
    }

    /**
     * Creates a new ExpiringMap with the specified configuration.
     *
     * @param defaultTtl the default time-to-live
     * @param policy     the expiration policy
     * @param scheduler  the scheduler for cleanup tasks (null to create internal)
     * @since 1.0.0
     */
    public ExpiringMap(@NotNull Duration defaultTtl, @NotNull ExpirationPolicy policy,
                       @Nullable ScheduledExecutorService scheduler) {
        Objects.requireNonNull(defaultTtl, "defaultTtl cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");

        if (defaultTtl.isNegative() || defaultTtl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }

        this.entries = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.defaultTtl = defaultTtl;
        this.policy = policy;
        this.expirationListeners = new CopyOnWriteArrayList<>();
        this.closed = false;

        if (scheduler != null) {
            this.scheduler = scheduler;
            this.ownScheduler = false;
        } else {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ExpiringMap-Cleaner");
                t.setDaemon(true);
                return t;
            });
            this.ownScheduler = true;
        }

        // Schedule periodic cleanup
        long cleanupIntervalMs = Math.max(1000, defaultTtl.toMillis() / 4);
        this.scheduler.scheduleWithFixedDelay(
                this::cleanupExpired,
                cleanupIntervalMs,
                cleanupIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    // ==================== Map Interface Implementation ====================

    @Override
    public int size() {
        cleanupExpired();
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        cleanupExpired();
        return entries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) return false;
        ExpiringEntry<V> entry = entries.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }
        touchEntry(key, entry);
        return true;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) return false;
        cleanupExpired();
        return entries.values().stream()
                .filter(e -> !e.isExpired())
                .anyMatch(e -> value.equals(e.value()));
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        if (key == null) return null;

        ExpiringEntry<V> entry = entries.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            handleExpiration((K) key, entry);
            return null;
        }

        touchEntry(key, entry);
        return entry.value();
    }

    @Override
    @Nullable
    public V put(@NotNull K key, @NotNull V value) {
        return put(key, value, defaultTtl);
    }

    /**
     * Puts a value with a custom TTL.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the time-to-live for this entry
     * @return the previous value, or null
     * @since 1.0.0
     */
    @Nullable
    public V put(@NotNull K key, @NotNull V value, @NotNull Duration ttl) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(ttl, "ttl cannot be null");

        checkClosed();

        Instant now = Instant.now();
        Instant expiration = now.plus(ttl);
        ExpiringEntry<V> newEntry = new ExpiringEntry<>(value, expiration, now, now, now);

        ExpiringEntry<V> oldEntry = entries.put(key, newEntry);
        return oldEntry != null && !oldEntry.isExpired() ? oldEntry.value() : null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (key == null) return null;

        ExpiringEntry<V> entry = entries.remove(key);
        return entry != null && !entry.isExpired() ? entry.value() : null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        Objects.requireNonNull(m, "map cannot be null");
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    @NotNull
    public Set<K> keySet() {
        cleanupExpired();
        return Collections.unmodifiableSet(new HashSet<>(entries.keySet()));
    }

    @Override
    @NotNull
    public Collection<V> values() {
        cleanupExpired();
        return entries.values().stream()
                .filter(e -> !e.isExpired())
                .map(ExpiringEntry::value)
                .toList();
    }

    @Override
    @NotNull
    public Set<Entry<K, V>> entrySet() {
        cleanupExpired();
        Set<Entry<K, V>> result = new HashSet<>();
        entries.forEach((k, v) -> {
            if (!v.isExpired()) {
                result.add(new AbstractMap.SimpleImmutableEntry<>(k, v.value()));
            }
        });
        return Collections.unmodifiableSet(result);
    }

    // ==================== Additional Map Methods ====================

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V value = get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public V putIfAbsent(@NotNull K key, @NotNull V value) {
        return putIfAbsent(key, value, defaultTtl);
    }

    /**
     * Puts a value if the key is absent, with a custom TTL.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the time-to-live
     * @return the existing value, or null if inserted
     * @since 1.0.0
     */
    @Nullable
    public V putIfAbsent(@NotNull K key, @NotNull V value, @NotNull Duration ttl) {
        V existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value, ttl);
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null || value == null) return false;

        ExpiringEntry<V> entry = entries.get(key);
        if (entry != null && !entry.isExpired() && value.equals(entry.value())) {
            return entries.remove(key, entry);
        }
        return false;
    }

    @Override
    public V replace(@NotNull K key, @NotNull V value) {
        return replace(key, value, defaultTtl);
    }

    /**
     * Replaces a value with a custom TTL.
     *
     * @param key   the key
     * @param value the new value
     * @param ttl   the time-to-live
     * @return the previous value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    public V replace(@NotNull K key, @NotNull V value, @NotNull Duration ttl) {
        V existing = get(key);
        if (existing != null) {
            put(key, value, ttl);
            return existing;
        }
        return null;
    }

    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        V existing = get(key);
        if (existing != null && existing.equals(oldValue)) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public V computeIfAbsent(@NotNull K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        return computeIfAbsent(key, mappingFunction, defaultTtl);
    }

    /**
     * Computes a value if absent, with a custom TTL.
     *
     * @param key             the key
     * @param mappingFunction the function to compute the value
     * @param ttl             the time-to-live
     * @return the existing or computed value
     * @since 1.0.0
     */
    public V computeIfAbsent(@NotNull K key, @NotNull Function<? super K, ? extends V> mappingFunction,
                             @NotNull Duration ttl) {
        V existing = get(key);
        if (existing != null) {
            return existing;
        }

        V computed = mappingFunction.apply(key);
        if (computed != null) {
            put(key, computed, ttl);
        }
        return computed;
    }

    @Override
    public V computeIfPresent(@NotNull K key,
                              @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V existing = get(key);
        if (existing == null) {
            return null;
        }

        V computed = remappingFunction.apply(key, existing);
        if (computed != null) {
            put(key, computed);
            return computed;
        } else {
            remove(key);
            return null;
        }
    }

    @Override
    public V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V existing = get(key);
        V computed = remappingFunction.apply(key, existing);

        if (computed != null) {
            put(key, computed);
            return computed;
        } else if (existing != null) {
            remove(key);
        }
        return null;
    }

    // ==================== Expiration-specific Methods ====================

    /**
     * Gets the remaining time-to-live for a key.
     *
     * @param key the key
     * @return the remaining TTL, or {@link Duration#ZERO} if not present or expired
     * @since 1.0.0
     */
    @NotNull
    public Duration getRemainingTtl(@NotNull K key) {
        Objects.requireNonNull(key, "key cannot be null");
        ExpiringEntry<V> entry = entries.get(key);
        return entry != null ? entry.remainingTime() : Duration.ZERO;
    }

    /**
     * Gets the expiration instant for a key.
     *
     * @param key the key
     * @return an Optional containing the expiration instant
     * @since 1.0.0
     */
    @NotNull
    public Optional<Instant> getExpiration(@NotNull K key) {
        Objects.requireNonNull(key, "key cannot be null");
        ExpiringEntry<V> entry = entries.get(key);
        return entry != null && !entry.isExpired()
                ? Optional.of(entry.expiration())
                : Optional.empty();
    }

    /**
     * Resets the expiration time for a key.
     *
     * @param key the key
     * @param ttl the new TTL
     * @return true if the key was found and TTL was reset
     * @since 1.0.0
     */
    public boolean resetExpiration(@NotNull K key, @NotNull Duration ttl) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(ttl, "ttl cannot be null");

        ExpiringEntry<V> entry = entries.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }

        Instant newExpiration = Instant.now().plus(ttl);
        entries.put(key, new ExpiringEntry<>(
                entry.value(), newExpiration, entry.created(),
                entry.lastAccessed(), Instant.now()
        ));
        return true;
    }

    /**
     * Registers an expiration listener.
     *
     * <p>The listener is called when entries expire.
     *
     * @param listener the listener
     * @return this map for chaining
     * @since 1.0.0
     */
    @NotNull
    public ExpiringMap<K, V> onExpire(@NotNull BiConsumer<K, V> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        expirationListeners.add(listener);
        return this;
    }

    /**
     * Removes an expiration listener.
     *
     * @param listener the listener to remove
     * @return true if the listener was removed
     * @since 1.0.0
     */
    public boolean removeExpirationListener(@NotNull BiConsumer<K, V> listener) {
        return expirationListeners.remove(listener);
    }

    /**
     * Gets the default TTL for this map.
     *
     * @return the default TTL
     * @since 1.0.0
     */
    @NotNull
    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    /**
     * Gets the expiration policy for this map.
     *
     * @return the expiration policy
     * @since 1.0.0
     */
    @NotNull
    public ExpirationPolicy getExpirationPolicy() {
        return policy;
    }

    // ==================== Cleanup Methods ====================

    /**
     * Manually triggers cleanup of expired entries.
     *
     * @return the number of expired entries removed
     * @since 1.0.0
     */
    public int cleanupExpired() {
        if (closed) return 0;

        int removed = 0;
        var iterator = entries.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                notifyExpiration(entry.getKey(), entry.getValue().value());
                removed++;
            }
        }

        return removed;
    }

    /**
     * Closes this map and shuts down the cleanup scheduler.
     *
     * <p>After closing, the map can still be used but entries will not be
     * automatically cleaned up.
     *
     * @since 1.0.0
     */
    public void close() {
        if (closed) return;
        closed = true;

        if (ownScheduler) {
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
     * Checks if this map has been closed.
     *
     * @return true if closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        return closed;
    }

    // ==================== Helper Methods ====================

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("ExpiringMap has been closed");
        }
    }

    @SuppressWarnings("unchecked")
    private void touchEntry(Object key, ExpiringEntry<V> entry) {
        if (policy == ExpirationPolicy.ACCESSED) {
            Instant now = Instant.now();
            Instant newExpiration = now.plus(defaultTtl);
            entries.put((K) key, new ExpiringEntry<>(
                    entry.value(), newExpiration, entry.created(), now, entry.lastModified()
            ));
        }
    }

    private void handleExpiration(K key, ExpiringEntry<V> entry) {
        if (entries.remove(key, entry)) {
            notifyExpiration(key, entry.value());
        }
    }

    private void notifyExpiration(K key, V value) {
        for (BiConsumer<K, V> listener : expirationListeners) {
            try {
                listener.accept(key, value);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }

    @Override
    public String toString() {
        cleanupExpired();
        return "ExpiringMap{" +
                "size=" + entries.size() +
                ", ttl=" + defaultTtl +
                ", policy=" + policy +
                '}';
    }
}
