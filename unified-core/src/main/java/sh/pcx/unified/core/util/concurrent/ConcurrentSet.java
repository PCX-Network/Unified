/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A thread-safe Set implementation backed by ConcurrentHashMap.
 *
 * <p>This class provides a simple, thread-safe Set that can be used in
 * concurrent environments without external synchronization.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ConcurrentSet<UUID> onlinePlayers = new ConcurrentSet<>();
 *
 * // Thread-safe add/remove
 * onlinePlayers.add(player.getUniqueId());
 * onlinePlayers.remove(player.getUniqueId());
 *
 * // Thread-safe iteration (uses snapshot)
 * for (UUID uuid : onlinePlayers) {
 *     // Process each player
 * }
 *
 * // Atomic conditional operations
 * boolean added = onlinePlayers.addIfAbsent(uuid);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All operations are thread-safe. Iteration uses a snapshot of the set
 * at the time iteration begins.
 *
 * @param <E> the element type
 * @since 1.0.0
 * @author Supatuck
 */
public final class ConcurrentSet<E> implements Set<E> {

    private static final Object PRESENT = new Object();

    private final ConcurrentHashMap<E, Object> map;

    /**
     * Creates a new empty ConcurrentSet.
     *
     * @since 1.0.0
     */
    public ConcurrentSet() {
        this.map = new ConcurrentHashMap<>();
    }

    /**
     * Creates a ConcurrentSet with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity
     * @since 1.0.0
     */
    public ConcurrentSet(int initialCapacity) {
        this.map = new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * Creates a ConcurrentSet containing the elements of the specified collection.
     *
     * @param collection the collection to copy
     * @since 1.0.0
     */
    public ConcurrentSet(@NotNull Collection<? extends E> collection) {
        this(Math.max(16, collection.size()));
        addAll(collection);
    }

    /**
     * Creates a ConcurrentSet from varargs.
     *
     * @param elements the elements
     * @param <E>      the element type
     * @return a new ConcurrentSet
     * @since 1.0.0
     */
    @SafeVarargs
    @NotNull
    public static <E> ConcurrentSet<E> of(E... elements) {
        ConcurrentSet<E> set = new ConcurrentSet<>(elements.length);
        set.addAll(Arrays.asList(elements));
        return set;
    }

    /**
     * Creates an empty ConcurrentSet.
     *
     * @param <E> the element type
     * @return a new empty ConcurrentSet
     * @since 1.0.0
     */
    @NotNull
    public static <E> ConcurrentSet<E> create() {
        return new ConcurrentSet<>();
    }

    // ==================== Set Implementation ====================

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        // Return iterator over a snapshot for thread safety
        return new ArrayList<>(map.keySet()).iterator();
    }

    @Override
    @NotNull
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    @NotNull
    public <T> T[] toArray(@NotNull T[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        boolean modified = false;
        Iterator<E> it = map.keySet().iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            if (remove(o)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super E> action) {
        map.keySet().forEach(action);
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super E> filter) {
        return map.keySet().removeIf(filter);
    }

    @Override
    @NotNull
    public Spliterator<E> spliterator() {
        return map.keySet().spliterator();
    }

    @Override
    @NotNull
    public Stream<E> stream() {
        return map.keySet().stream();
    }

    @Override
    @NotNull
    public Stream<E> parallelStream() {
        return map.keySet().parallelStream();
    }

    // ==================== Additional Methods ====================

    /**
     * Adds the element if it is not already present.
     *
     * <p>This is an alias for {@link #add(Object)} for clarity.
     *
     * @param e the element to add
     * @return true if the element was added (was not present)
     * @since 1.0.0
     */
    public boolean addIfAbsent(E e) {
        return map.putIfAbsent(e, PRESENT) == null;
    }

    /**
     * Gets an unmodifiable view of this set.
     *
     * @return an unmodifiable set view
     * @since 1.0.0
     */
    @NotNull
    public Set<E> unmodifiableView() {
        return Collections.unmodifiableSet(map.keySet());
    }

    /**
     * Gets a snapshot (copy) of this set.
     *
     * @return a new Set containing the current elements
     * @since 1.0.0
     */
    @NotNull
    public Set<E> snapshot() {
        return new HashSet<>(map.keySet());
    }

    /**
     * Copies all elements to the specified collection.
     *
     * @param collection the collection to copy to
     * @param <C>        the collection type
     * @return the collection
     * @since 1.0.0
     */
    @NotNull
    public <C extends Collection<? super E>> C copyTo(@NotNull C collection) {
        collection.addAll(map.keySet());
        return collection;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Set<?> other)) return false;
        return map.keySet().equals(other);
    }

    @Override
    public int hashCode() {
        return map.keySet().hashCode();
    }

    @Override
    public String toString() {
        return map.keySet().toString();
    }
}
