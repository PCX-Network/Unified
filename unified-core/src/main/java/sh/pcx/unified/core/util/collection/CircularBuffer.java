/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A thread-safe circular buffer (ring buffer) implementation.
 *
 * <p>A circular buffer has a fixed capacity and overwrites the oldest elements
 * when new elements are added beyond capacity. This is useful for maintaining
 * a fixed-size history of events, logs, or other temporal data.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a buffer to store the last 100 log entries
 * CircularBuffer<LogEntry> logs = new CircularBuffer<>(100);
 *
 * // Add entries (oldest are overwritten when full)
 * logs.add(new LogEntry("Server started"));
 * logs.add(new LogEntry("Player joined"));
 *
 * // Get recent entries
 * List<LogEntry> recent = logs.toList();
 *
 * // Get the most recent entry
 * LogEntry latest = logs.getLast();
 *
 * // Iterate from oldest to newest
 * for (LogEntry entry : logs) {
 *     System.out.println(entry);
 * }
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Chat history - keep last N messages</li>
 *   <li>Command history - for command recall</li>
 *   <li>Event logging - rolling log buffer</li>
 *   <li>Performance metrics - moving average calculations</li>
 *   <li>Undo/redo buffers</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is thread-safe. All operations use a read-write lock
 * to allow concurrent reads while ensuring exclusive writes.
 *
 * @param <E> the type of elements in this buffer
 * @since 1.0.0
 * @author Supatuck
 */
public final class CircularBuffer<E> implements Iterable<E> {

    private final Object[] buffer;
    private final int capacity;
    private final ReadWriteLock lock;
    private int head;      // Index of next write position
    private int size;      // Current number of elements
    private long totalAdded; // Total elements ever added (for statistics)

    /**
     * Creates a new CircularBuffer with the specified capacity.
     *
     * @param capacity the maximum number of elements (must be positive)
     * @throws IllegalArgumentException if capacity is not positive
     * @since 1.0.0
     */
    public CircularBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.buffer = new Object[capacity];
        this.capacity = capacity;
        this.lock = new ReentrantReadWriteLock();
        this.head = 0;
        this.size = 0;
        this.totalAdded = 0;
    }

    // ==================== Adding Elements ====================

    /**
     * Adds an element to the buffer.
     *
     * <p>If the buffer is full, the oldest element is overwritten.
     *
     * @param element the element to add
     * @return the element that was overwritten, or null if none
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public E add(@NotNull E element) {
        Objects.requireNonNull(element, "element cannot be null");

        lock.writeLock().lock();
        try {
            E overwritten = (E) buffer[head];
            buffer[head] = element;
            head = (head + 1) % capacity;

            if (size < capacity) {
                size++;
                overwritten = null; // No element was actually overwritten
            }

            totalAdded++;
            return overwritten;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds all elements from a collection to the buffer.
     *
     * @param elements the elements to add
     * @return the number of elements that were overwritten
     * @since 1.0.0
     */
    public int addAll(@NotNull Collection<? extends E> elements) {
        Objects.requireNonNull(elements, "elements cannot be null");

        lock.writeLock().lock();
        try {
            int overwritten = 0;
            for (E element : elements) {
                if (element != null && add(element) != null) {
                    overwritten++;
                }
            }
            return overwritten;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds all elements from varargs to the buffer.
     *
     * @param elements the elements to add
     * @return the number of elements that were overwritten
     * @since 1.0.0
     */
    @SafeVarargs
    public final int addAll(@NotNull E... elements) {
        return addAll(Arrays.asList(elements));
    }

    // ==================== Accessing Elements ====================

    /**
     * Gets the element at the specified index (0 = oldest).
     *
     * @param index the index (0 to size-1)
     * @return the element at that index
     * @throws IndexOutOfBoundsException if index is out of range
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public E get(int index) {
        lock.readLock().lock();
        try {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }

            int actualIndex = getActualIndex(index);
            return (E) buffer[actualIndex];
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the oldest element in the buffer.
     *
     * @return the oldest element, or null if empty
     * @since 1.0.0
     */
    @Nullable
    public E getFirst() {
        lock.readLock().lock();
        try {
            if (size == 0) {
                return null;
            }
            return get(0);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the newest element in the buffer.
     *
     * @return the newest element, or null if empty
     * @since 1.0.0
     */
    @Nullable
    public E getLast() {
        lock.readLock().lock();
        try {
            if (size == 0) {
                return null;
            }
            return get(size - 1);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the last N elements (newest first).
     *
     * @param count the number of elements to get
     * @return a list of the last N elements (newest first)
     * @since 1.0.0
     */
    @NotNull
    public List<E> getLast(int count) {
        lock.readLock().lock();
        try {
            int actualCount = Math.min(count, size);
            List<E> result = new ArrayList<>(actualCount);

            for (int i = size - 1; i >= size - actualCount; i--) {
                result.add(get(i));
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the first N elements (oldest first).
     *
     * @param count the number of elements to get
     * @return a list of the first N elements (oldest first)
     * @since 1.0.0
     */
    @NotNull
    public List<E> getFirst(int count) {
        lock.readLock().lock();
        try {
            int actualCount = Math.min(count, size);
            List<E> result = new ArrayList<>(actualCount);

            for (int i = 0; i < actualCount; i++) {
                result.add(get(i));
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Peeks at the newest element without removing it.
     *
     * @return the newest element, or null if empty
     * @since 1.0.0
     */
    @Nullable
    public E peek() {
        return getLast();
    }

    /**
     * Peeks at the oldest element without removing it.
     *
     * @return the oldest element, or null if empty
     * @since 1.0.0
     */
    @Nullable
    public E peekFirst() {
        return getFirst();
    }

    // ==================== Removing Elements ====================

    /**
     * Removes and returns the oldest element.
     *
     * @return the oldest element, or null if empty
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public E removeFirst() {
        lock.writeLock().lock();
        try {
            if (size == 0) {
                return null;
            }

            int oldestIndex = getActualIndex(0);
            E element = (E) buffer[oldestIndex];
            buffer[oldestIndex] = null;
            size--;

            return element;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes and returns the newest element.
     *
     * @return the newest element, or null if empty
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public E removeLast() {
        lock.writeLock().lock();
        try {
            if (size == 0) {
                return null;
            }

            head = (head - 1 + capacity) % capacity;
            E element = (E) buffer[head];
            buffer[head] = null;
            size--;

            return element;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all elements from the buffer.
     *
     * @since 1.0.0
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            Arrays.fill(buffer, null);
            head = 0;
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== Query Methods ====================

    /**
     * Checks if the buffer contains the specified element.
     *
     * @param element the element to search for
     * @return true if the element is present
     * @since 1.0.0
     */
    public boolean contains(@NotNull Object element) {
        Objects.requireNonNull(element, "element cannot be null");

        lock.readLock().lock();
        try {
            for (int i = 0; i < size; i++) {
                if (element.equals(get(i))) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Finds the index of the specified element (0 = oldest).
     *
     * @param element the element to search for
     * @return the index, or -1 if not found
     * @since 1.0.0
     */
    public int indexOf(@NotNull Object element) {
        Objects.requireNonNull(element, "element cannot be null");

        lock.readLock().lock();
        try {
            for (int i = 0; i < size; i++) {
                if (element.equals(get(i))) {
                    return i;
                }
            }
            return -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Finds the last index of the specified element.
     *
     * @param element the element to search for
     * @return the index, or -1 if not found
     * @since 1.0.0
     */
    public int lastIndexOf(@NotNull Object element) {
        Objects.requireNonNull(element, "element cannot be null");

        lock.readLock().lock();
        try {
            for (int i = size - 1; i >= 0; i--) {
                if (element.equals(get(i))) {
                    return i;
                }
            }
            return -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the current number of elements in the buffer.
     *
     * @return the size
     * @since 1.0.0
     */
    public int size() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the maximum capacity of the buffer.
     *
     * @return the capacity
     * @since 1.0.0
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Checks if the buffer is empty.
     *
     * @return true if empty
     * @since 1.0.0
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return size == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if the buffer is full.
     *
     * @return true if full (at capacity)
     * @since 1.0.0
     */
    public boolean isFull() {
        lock.readLock().lock();
        try {
            return size == capacity;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the number of empty slots remaining.
     *
     * @return the remaining capacity
     * @since 1.0.0
     */
    public int remainingCapacity() {
        lock.readLock().lock();
        try {
            return capacity - size;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the total number of elements ever added.
     *
     * @return the total count
     * @since 1.0.0
     */
    public long getTotalAdded() {
        lock.readLock().lock();
        try {
            return totalAdded;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the number of elements that have been overwritten.
     *
     * @return the overwritten count
     * @since 1.0.0
     */
    public long getOverwrittenCount() {
        lock.readLock().lock();
        try {
            return Math.max(0, totalAdded - capacity);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Iteration and Conversion ====================

    /**
     * Performs an action for each element (oldest to newest).
     *
     * @param action the action to perform
     * @since 1.0.0
     */
    public void forEach(@NotNull Consumer<? super E> action) {
        Objects.requireNonNull(action, "action cannot be null");

        lock.readLock().lock();
        try {
            for (int i = 0; i < size; i++) {
                action.accept(get(i));
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Performs an action for each element in reverse order (newest to oldest).
     *
     * @param action the action to perform
     * @since 1.0.0
     */
    public void forEachReverse(@NotNull Consumer<? super E> action) {
        Objects.requireNonNull(action, "action cannot be null");

        lock.readLock().lock();
        try {
            for (int i = size - 1; i >= 0; i--) {
                action.accept(get(i));
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Finds elements matching the predicate.
     *
     * @param predicate the predicate to match
     * @return a list of matching elements
     * @since 1.0.0
     */
    @NotNull
    public List<E> filter(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");

        lock.readLock().lock();
        try {
            List<E> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                E element = get(i);
                if (predicate.test(element)) {
                    result.add(element);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        lock.readLock().lock();
        try {
            // Return iterator over a snapshot
            return toList().iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a stream of elements (oldest to newest).
     *
     * @return a stream
     * @since 1.0.0
     */
    @NotNull
    public Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Converts the buffer to a list (oldest to newest).
     *
     * @return an unmodifiable list
     * @since 1.0.0
     */
    @NotNull
    public List<E> toList() {
        lock.readLock().lock();
        try {
            List<E> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(get(i));
            }
            return Collections.unmodifiableList(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Converts the buffer to a list in reverse order (newest to oldest).
     *
     * @return an unmodifiable list
     * @since 1.0.0
     */
    @NotNull
    public List<E> toListReversed() {
        lock.readLock().lock();
        try {
            List<E> result = new ArrayList<>(size);
            for (int i = size - 1; i >= 0; i--) {
                result.add(get(i));
            }
            return Collections.unmodifiableList(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Converts the buffer to an array.
     *
     * @return an array of elements
     * @since 1.0.0
     */
    @NotNull
    public Object[] toArray() {
        lock.readLock().lock();
        try {
            Object[] result = new Object[size];
            for (int i = 0; i < size; i++) {
                result[i] = get(i);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Converts the buffer to a typed array.
     *
     * @param array the array to populate
     * @return the populated array
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@NotNull T[] array) {
        lock.readLock().lock();
        try {
            if (array.length < size) {
                array = (T[]) java.lang.reflect.Array.newInstance(
                        array.getClass().getComponentType(), size);
            }
            for (int i = 0; i < size; i++) {
                array[i] = (T) get(i);
            }
            if (array.length > size) {
                array[size] = null;
            }
            return array;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Calculates the actual buffer index for a logical index.
     */
    private int getActualIndex(int logicalIndex) {
        if (size == capacity) {
            // Buffer is full, oldest is at head
            return (head + logicalIndex) % capacity;
        } else {
            // Buffer not full, oldest is at 0
            return logicalIndex;
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder("CircularBuffer[");
            sb.append(size).append("/").append(capacity).append("]{");
            for (int i = 0; i < size; i++) {
                if (i > 0) sb.append(", ");
                sb.append(get(i));
            }
            sb.append("}");
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CircularBuffer<?> other)) return false;

        lock.readLock().lock();
        other.lock.readLock().lock();
        try {
            if (size != other.size) return false;
            for (int i = 0; i < size; i++) {
                if (!Objects.equals(get(i), other.get(i))) {
                    return false;
                }
            }
            return true;
        } finally {
            other.lock.readLock().unlock();
            lock.readLock().unlock();
        }
    }

    @Override
    public int hashCode() {
        lock.readLock().lock();
        try {
            int result = 1;
            for (int i = 0; i < size; i++) {
                E element = get(i);
                result = 31 * result + (element == null ? 0 : element.hashCode());
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
}
