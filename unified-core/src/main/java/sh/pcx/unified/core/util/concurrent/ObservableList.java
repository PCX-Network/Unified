/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.concurrent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A thread-safe observable list that notifies listeners on changes.
 *
 * <p>Useful for maintaining lists that need to trigger side effects when
 * modified, such as updating GUIs or synchronizing state.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ObservableList<Player> players = new ObservableList<>();
 *
 * // Add listeners
 * players.onAdd(player -> {
 *     broadcast(player.getName() + " joined!");
 * });
 *
 * players.onRemove(player -> {
 *     broadcast(player.getName() + " left!");
 * });
 *
 * players.onChange(() -> {
 *     updateScoreboard();
 * });
 *
 * // Modify list (triggers listeners)
 * players.add(newPlayer);
 * players.remove(leavingPlayer);
 * players.clear();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations use proper synchronization.
 *
 * @param <E> the element type
 * @since 1.0.0
 * @author Supatuck
 */
public final class ObservableList<E> implements List<E> {

    /**
     * Change type enumeration.
     */
    public enum ChangeType {
        ADD, REMOVE, SET, CLEAR
    }

    /**
     * Detailed change event.
     *
     * @param <E> the element type
     */
    public record Change<E>(
            @NotNull ChangeType type,
            int index,
            @Nullable E oldElement,
            @Nullable E newElement,
            @Nullable Collection<? extends E> elements
    ) {
        /**
         * Creates an add change event.
         */
        @NotNull
        public static <E> Change<E> add(int index, E element) {
            return new Change<>(ChangeType.ADD, index, null, element, null);
        }

        /**
         * Creates a remove change event.
         */
        @NotNull
        public static <E> Change<E> remove(int index, E element) {
            return new Change<>(ChangeType.REMOVE, index, element, null, null);
        }

        /**
         * Creates a set change event.
         */
        @NotNull
        public static <E> Change<E> set(int index, E oldElement, E newElement) {
            return new Change<>(ChangeType.SET, index, oldElement, newElement, null);
        }

        /**
         * Creates a clear change event.
         */
        @NotNull
        public static <E> Change<E> clear(Collection<? extends E> removed) {
            return new Change<>(ChangeType.CLEAR, -1, null, null, removed);
        }
    }

    private final List<E> delegate;
    private final ReadWriteLock lock;
    private final List<Consumer<Change<E>>> changeListeners;
    private final List<Consumer<E>> addListeners;
    private final List<Consumer<E>> removeListeners;
    private final List<Runnable> anyChangeListeners;
    private volatile boolean notifying;

    /**
     * Creates a new empty ObservableList.
     *
     * @since 1.0.0
     */
    public ObservableList() {
        this(new ArrayList<>());
    }

    /**
     * Creates an ObservableList with initial elements.
     *
     * @param initialElements the initial elements
     * @since 1.0.0
     */
    public ObservableList(@NotNull Collection<? extends E> initialElements) {
        this.delegate = new ArrayList<>(initialElements);
        this.lock = new ReentrantReadWriteLock();
        this.changeListeners = new CopyOnWriteArrayList<>();
        this.addListeners = new CopyOnWriteArrayList<>();
        this.removeListeners = new CopyOnWriteArrayList<>();
        this.anyChangeListeners = new CopyOnWriteArrayList<>();
        this.notifying = false;
    }

    /**
     * Creates an ObservableList from varargs.
     *
     * @param elements the elements
     * @param <E>      the element type
     * @return a new ObservableList
     * @since 1.0.0
     */
    @SafeVarargs
    @NotNull
    public static <E> ObservableList<E> of(E... elements) {
        return new ObservableList<>(Arrays.asList(elements));
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
    public ObservableList<E> onChange(@NotNull Runnable listener) {
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
    public ObservableList<E> onDetailedChange(@NotNull Consumer<Change<E>> listener) {
        changeListeners.add(Objects.requireNonNull(listener));
        return this;
    }

    /**
     * Adds a listener for add events.
     *
     * @param listener the listener
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableList<E> onAdd(@NotNull Consumer<E> listener) {
        addListeners.add(Objects.requireNonNull(listener));
        return this;
    }

    /**
     * Adds a listener for remove events.
     *
     * @param listener the listener
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableList<E> onRemove(@NotNull Consumer<E> listener) {
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
        addListeners.clear();
        removeListeners.clear();
        anyChangeListeners.clear();
    }

    // ==================== List Implementation ====================

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return delegate.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return delegate.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        lock.readLock().lock();
        try {
            return delegate.contains(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public Object[] toArray() {
        lock.readLock().lock();
        try {
            return delegate.toArray();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public <T> T[] toArray(@NotNull T[] a) {
        lock.readLock().lock();
        try {
            return delegate.toArray(a);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean add(E e) {
        lock.writeLock().lock();
        try {
            int index = delegate.size();
            boolean result = delegate.add(e);
            if (result) {
                notifyChange(Change.add(index, e));
                notifyAdd(e);
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        lock.writeLock().lock();
        try {
            int index = delegate.indexOf(o);
            boolean result = delegate.remove(o);
            if (result) {
                @SuppressWarnings("unchecked")
                E element = (E) o;
                notifyChange(Change.remove(index, element));
                notifyRemove(element);
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        lock.readLock().lock();
        try {
            return delegate.containsAll(c);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        if (c.isEmpty()) return false;

        lock.writeLock().lock();
        try {
            int startIndex = delegate.size();
            boolean result = delegate.addAll(c);
            if (result) {
                int index = startIndex;
                for (E element : c) {
                    notifyChange(Change.add(index++, element));
                    notifyAdd(element);
                }
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        if (c.isEmpty()) return false;

        lock.writeLock().lock();
        try {
            boolean result = delegate.addAll(index, c);
            if (result) {
                int i = index;
                for (E element : c) {
                    notifyChange(Change.add(i++, element));
                    notifyAdd(element);
                }
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        lock.writeLock().lock();
        try {
            boolean modified = false;
            for (Object o : c) {
                if (remove(o)) {
                    modified = true;
                }
            }
            return modified;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        lock.writeLock().lock();
        try {
            List<E> toRemove = new ArrayList<>();
            for (E element : delegate) {
                if (!c.contains(element)) {
                    toRemove.add(element);
                }
            }
            return removeAll(toRemove);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void replaceAll(@NotNull UnaryOperator<E> operator) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < delegate.size(); i++) {
                E oldElement = delegate.get(i);
                E newElement = operator.apply(oldElement);
                if (!Objects.equals(oldElement, newElement)) {
                    delegate.set(i, newElement);
                    notifyChange(Change.set(i, oldElement, newElement));
                    notifyRemove(oldElement);
                    notifyAdd(newElement);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        lock.writeLock().lock();
        try {
            delegate.sort(c);
            notifyAnyChange();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            if (delegate.isEmpty()) return;

            List<E> removed = new ArrayList<>(delegate);
            delegate.clear();

            notifyChange(Change.clear(removed));
            for (E element : removed) {
                notifyRemove(element);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E get(int index) {
        lock.readLock().lock();
        try {
            return delegate.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E set(int index, E element) {
        lock.writeLock().lock();
        try {
            E oldElement = delegate.set(index, element);
            if (!Objects.equals(oldElement, element)) {
                notifyChange(Change.set(index, oldElement, element));
                notifyRemove(oldElement);
                notifyAdd(element);
            }
            return oldElement;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void add(int index, E element) {
        lock.writeLock().lock();
        try {
            delegate.add(index, element);
            notifyChange(Change.add(index, element));
            notifyAdd(element);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E remove(int index) {
        lock.writeLock().lock();
        try {
            E removed = delegate.remove(index);
            notifyChange(Change.remove(index, removed));
            notifyRemove(removed);
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        lock.readLock().lock();
        try {
            return delegate.indexOf(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        lock.readLock().lock();
        try {
            return delegate.lastIndexOf(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public ListIterator<E> listIterator() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate).listIterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public ListIterator<E> listIterator(int index) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate).listIterator(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public List<E> subList(int fromIndex, int toIndex) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate.subList(fromIndex, toIndex));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super E> filter) {
        lock.writeLock().lock();
        try {
            boolean modified = false;
            Iterator<E> iterator = delegate.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (filter.test(element)) {
                    iterator.remove();
                    notifyChange(Change.remove(index, element));
                    notifyRemove(element);
                    modified = true;
                } else {
                    index++;
                }
            }
            return modified;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void forEach(@NotNull Consumer<? super E> action) {
        lock.readLock().lock();
        try {
            delegate.forEach(action);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public Stream<E> stream() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate).stream();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public Stream<E> parallelStream() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate).parallelStream();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Additional Methods ====================

    /**
     * Gets a snapshot of the list.
     *
     * @return an unmodifiable copy
     * @since 1.0.0
     */
    @NotNull
    public List<E> snapshot() {
        lock.readLock().lock();
        try {
            return List.copyOf(delegate);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the first element.
     *
     * @return an optional containing the first element
     * @since 1.0.0
     */
    @NotNull
    public Optional<E> first() {
        lock.readLock().lock();
        try {
            return delegate.isEmpty() ? Optional.empty() : Optional.of(delegate.getFirst());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the last element.
     *
     * @return an optional containing the last element
     * @since 1.0.0
     */
    @NotNull
    public Optional<E> last() {
        lock.readLock().lock();
        try {
            return delegate.isEmpty() ? Optional.empty() : Optional.of(delegate.getLast());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Notification Helpers ====================

    private void notifyChange(Change<E> change) {
        if (notifying) return;
        notifying = true;
        try {
            for (Consumer<Change<E>> listener : changeListeners) {
                try {
                    listener.accept(change);
                } catch (Exception e) {
                    // Log but don't propagate
                }
            }
            notifyAnyChange();
        } finally {
            notifying = false;
        }
    }

    private void notifyAdd(E element) {
        for (Consumer<E> listener : addListeners) {
            try {
                listener.accept(element);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }

    private void notifyRemove(E element) {
        for (Consumer<E> listener : removeListeners) {
            try {
                listener.accept(element);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }

    private void notifyAnyChange() {
        for (Runnable listener : anyChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return "ObservableList" + delegate.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
}
