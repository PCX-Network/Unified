/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.concurrent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A thread-safe observable map that notifies listeners on changes.
 *
 * <p>Useful for maintaining maps that need to trigger side effects when
 * modified, such as player data caches or configuration values.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ObservableMap<UUID, PlayerData> playerData = new ObservableMap<>();
 *
 * // Add listeners
 * playerData.onPut((uuid, data) -> {
 *     System.out.println("Data set for " + uuid);
 * });
 *
 * playerData.onRemove((uuid, data) -> {
 *     System.out.println("Data removed for " + uuid);
 * });
 *
 * // Modify map (triggers listeners)
 * playerData.put(player.getUniqueId(), data);
 * playerData.remove(player.getUniqueId());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class uses {@link ConcurrentHashMap} internally and is thread-safe.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 */
public final class ObservableMap<K, V> implements Map<K, V> {

    /**
     * Change type enumeration.
     */
    public enum ChangeType {
        PUT, REMOVE, CLEAR
    }

    /**
     * Detailed change event.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public record Change<K, V>(
            @NotNull ChangeType type,
            @Nullable K key,
            @Nullable V oldValue,
            @Nullable V newValue
    ) {
        /**
         * Creates a put change event.
         */
        @NotNull
        public static <K, V> Change<K, V> put(K key, V oldValue, V newValue) {
            return new Change<>(ChangeType.PUT, key, oldValue, newValue);
        }

        /**
         * Creates a remove change event.
         */
        @NotNull
        public static <K, V> Change<K, V> remove(K key, V value) {
            return new Change<>(ChangeType.REMOVE, key, value, null);
        }

        /**
         * Creates a clear change event.
         */
        @NotNull
        public static <K, V> Change<K, V> clear() {
            return new Change<>(ChangeType.CLEAR, null, null, null);
        }
    }

    private final Map<K, V> delegate;
    private final List<Consumer<Change<K, V>>> changeListeners;
    private final List<BiConsumer<K, V>> putListeners;
    private final List<BiConsumer<K, V>> removeListeners;
    private final List<Runnable> anyChangeListeners;

    /**
     * Creates a new empty ObservableMap.
     *
     * @since 1.0.0
     */
    public ObservableMap() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Creates an ObservableMap backed by the specified map.
     *
     * @param delegate the backing map
     * @since 1.0.0
     */
    public ObservableMap(@NotNull Map<K, V> delegate) {
        this.delegate = delegate;
        this.changeListeners = new CopyOnWriteArrayList<>();
        this.putListeners = new CopyOnWriteArrayList<>();
        this.removeListeners = new CopyOnWriteArrayList<>();
        this.anyChangeListeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Creates an ObservableMap with initial entries.
     *
     * @param initial the initial entries
     * @since 1.0.0
     */
    public ObservableMap(@NotNull Map<K, V> delegate, @NotNull Map<K, V> initial) {
        this(delegate);
        this.delegate.putAll(initial);
    }

    // ==================== Listeners ====================

    /**
     * Adds a listener for any change event.
     *
     * @param listener the listener
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableMap<K, V> onChange(@NotNull Runnable listener) {
        anyChangeListeners.add(Objects.requireNonNull(listener));
        return this;
    }

    /**
     * Adds a listener for detailed change events.
     *
     * @param listener the listener
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableMap<K, V> onDetailedChange(@NotNull Consumer<Change<K, V>> listener) {
        changeListeners.add(Objects.requireNonNull(listener));
        return this;
    }

    /**
     * Adds a listener for put events.
     *
     * @param listener the listener receiving (key, newValue)
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableMap<K, V> onPut(@NotNull BiConsumer<K, V> listener) {
        putListeners.add(Objects.requireNonNull(listener));
        return this;
    }

    /**
     * Adds a listener for remove events.
     *
     * @param listener the listener receiving (key, oldValue)
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableMap<K, V> onRemove(@NotNull BiConsumer<K, V> listener) {
        removeListeners.add(Objects.requireNonNull(listener));
        return this;
    }

    /**
     * Removes all listeners.
     *
     * @since 1.0.0
     */
    public void clearListeners() {
        changeListeners.clear();
        putListeners.clear();
        removeListeners.clear();
        anyChangeListeners.clear();
    }

    // ==================== Map Implementation ====================

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    @Nullable
    public V get(Object key) {
        return delegate.get(key);
    }

    @Override
    @Nullable
    public V put(@NotNull K key, @NotNull V value) {
        V oldValue = delegate.put(key, value);
        notifyChange(Change.put(key, oldValue, value));
        notifyPut(key, value);
        return oldValue;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        V oldValue = delegate.remove(key);
        if (oldValue != null) {
            notifyChange(Change.remove((K) key, oldValue));
            notifyRemove((K) key, oldValue);
        }
        return oldValue;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        if (delegate.isEmpty()) return;

        Map<K, V> removed = new HashMap<>(delegate);
        delegate.clear();

        notifyChange(Change.clear());
        removed.forEach(this::notifyRemove);
    }

    @Override
    @NotNull
    public Set<K> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    @Override
    @NotNull
    public Collection<V> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    @Override
    @NotNull
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }

    // ==================== Additional Map Methods ====================

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(@NotNull BiConsumer<? super K, ? super V> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(@NotNull BiFunction<? super K, ? super V, ? extends V> function) {
        delegate.forEach((key, oldValue) -> {
            V newValue = function.apply(key, oldValue);
            if (!Objects.equals(oldValue, newValue)) {
                delegate.put(key, newValue);
                notifyChange(Change.put(key, oldValue, newValue));
                notifyPut(key, newValue);
            }
        });
    }

    @Override
    @Nullable
    public V putIfAbsent(@NotNull K key, @NotNull V value) {
        V existing = delegate.get(key);
        if (existing == null) {
            return put(key, value);
        }
        return existing;
    }

    @Override
    public boolean remove(Object key, Object value) {
        V existing = delegate.get(key);
        if (Objects.equals(existing, value)) {
            remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        V existing = delegate.get(key);
        if (Objects.equals(existing, oldValue)) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public V replace(@NotNull K key, @NotNull V value) {
        if (delegate.containsKey(key)) {
            return put(key, value);
        }
        return null;
    }

    @Override
    public V computeIfAbsent(@NotNull K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        V existing = delegate.get(key);
        if (existing != null) {
            return existing;
        }
        V computed = mappingFunction.apply(key);
        if (computed != null) {
            put(key, computed);
        }
        return computed;
    }

    @Override
    public V computeIfPresent(@NotNull K key,
                              @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V existing = delegate.get(key);
        if (existing == null) {
            return null;
        }
        V computed = remappingFunction.apply(key, existing);
        if (computed != null) {
            put(key, computed);
        } else {
            remove(key);
        }
        return computed;
    }

    @Override
    public V compute(@NotNull K key,
                     @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V existing = delegate.get(key);
        V computed = remappingFunction.apply(key, existing);
        if (computed != null) {
            put(key, computed);
        } else if (existing != null) {
            remove(key);
        }
        return computed;
    }

    @Override
    public V merge(@NotNull K key, @NotNull V value,
                   @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        V existing = delegate.get(key);
        V newValue = (existing == null) ? value : remappingFunction.apply(existing, value);
        if (newValue != null) {
            put(key, newValue);
        } else {
            remove(key);
        }
        return newValue;
    }

    // ==================== Additional Methods ====================

    /**
     * Gets a snapshot of the map.
     *
     * @return an unmodifiable copy
     * @since 1.0.0
     */
    @NotNull
    public Map<K, V> snapshot() {
        return Map.copyOf(delegate);
    }

    /**
     * Gets a value as an Optional.
     *
     * @param key the key
     * @return an optional containing the value
     * @since 1.0.0
     */
    @NotNull
    public Optional<V> getOptional(@NotNull K key) {
        return Optional.ofNullable(delegate.get(key));
    }

    // ==================== Notification Helpers ====================

    private void notifyChange(Change<K, V> change) {
        for (Consumer<Change<K, V>> listener : changeListeners) {
            try {
                listener.accept(change);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
        for (Runnable listener : anyChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }

    private void notifyPut(K key, V value) {
        for (BiConsumer<K, V> listener : putListeners) {
            try {
                listener.accept(key, value);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }

    private void notifyRemove(K key, V value) {
        for (BiConsumer<K, V> listener : removeListeners) {
            try {
                listener.accept(key, value);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }

    @Override
    public String toString() {
        return "ObservableMap" + delegate.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Map<?, ?> other)) return false;
        return delegate.equals(other);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
