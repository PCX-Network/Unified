/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A collection that supports weighted random selection.
 *
 * <p>Elements can be added with associated weights, and random selection will
 * favor elements with higher weights. This is useful for loot tables, spawn
 * chances, and other probabilistic game mechanics.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a weighted loot table
 * WeightedCollection<String> loot = new WeightedCollection<>();
 * loot.add("Common Sword", 70);    // 70% chance
 * loot.add("Rare Sword", 25);      // 25% chance
 * loot.add("Legendary Sword", 5);  // 5% chance
 *
 * // Get random item
 * String item = loot.next();
 *
 * // Get multiple random items
 * List<String> items = loot.next(3);
 *
 * // Get unique random items (no duplicates)
 * List<String> uniqueItems = loot.nextUnique(3);
 * }</pre>
 *
 * <h2>Advanced Usage</h2>
 * <pre>{@code
 * // Conditional selection
 * String item = loot.nextWhere(s -> !s.startsWith("Common"));
 *
 * // Get weight information
 * double chance = loot.getChance("Legendary Sword"); // Returns 0.05 (5%)
 *
 * // Modify weights
 * loot.setWeight("Rare Sword", 35);
 * loot.adjustWeight("Common Sword", -10); // Reduce by 10
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations use proper synchronization.
 *
 * @param <E> the type of elements in this collection
 * @since 1.0.0
 * @author Supatuck
 */
public final class WeightedCollection<E> implements Iterable<E> {

    /**
     * Entry in the weighted collection.
     */
    private record Entry<E>(@NotNull E element, double weight) {
        Entry {
            Objects.requireNonNull(element, "element cannot be null");
            if (weight < 0) {
                throw new IllegalArgumentException("weight cannot be negative");
            }
        }
    }

    private final List<Entry<E>> entries;
    private final Map<E, Entry<E>> elementMap;
    private final ReadWriteLock lock;
    private double totalWeight;
    private boolean sorted;

    /**
     * Creates a new empty WeightedCollection.
     *
     * @since 1.0.0
     */
    public WeightedCollection() {
        this.entries = new ArrayList<>();
        this.elementMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.totalWeight = 0;
        this.sorted = true;
    }

    /**
     * Creates a new WeightedCollection with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity
     * @since 1.0.0
     */
    public WeightedCollection(int initialCapacity) {
        this.entries = new ArrayList<>(initialCapacity);
        this.elementMap = new HashMap<>(initialCapacity);
        this.lock = new ReentrantReadWriteLock();
        this.totalWeight = 0;
        this.sorted = true;
    }

    // ==================== Adding Elements ====================

    /**
     * Adds an element with the specified weight.
     *
     * <p>If the element already exists, its weight is updated.
     *
     * @param element the element to add
     * @param weight  the weight (must be non-negative)
     * @return this collection for chaining
     * @throws NullPointerException if element is null
     * @throws IllegalArgumentException if weight is negative
     * @since 1.0.0
     */
    @NotNull
    public WeightedCollection<E> add(@NotNull E element, double weight) {
        Objects.requireNonNull(element, "element cannot be null");
        if (weight < 0) {
            throw new IllegalArgumentException("weight cannot be negative");
        }

        lock.writeLock().lock();
        try {
            Entry<E> existing = elementMap.get(element);
            if (existing != null) {
                totalWeight -= existing.weight();
                entries.remove(existing);
            }

            Entry<E> entry = new Entry<>(element, weight);
            entries.add(entry);
            elementMap.put(element, entry);
            totalWeight += weight;
            sorted = false;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds an element with weight 1.0.
     *
     * @param element the element to add
     * @return this collection for chaining
     * @since 1.0.0
     */
    @NotNull
    public WeightedCollection<E> add(@NotNull E element) {
        return add(element, 1.0);
    }

    /**
     * Adds multiple elements with the same weight.
     *
     * @param weight   the weight for all elements
     * @param elements the elements to add
     * @return this collection for chaining
     * @since 1.0.0
     */
    @SafeVarargs
    @NotNull
    public final WeightedCollection<E> addAll(double weight, @NotNull E... elements) {
        for (E element : elements) {
            add(element, weight);
        }
        return this;
    }

    /**
     * Adds all elements from another WeightedCollection.
     *
     * @param other the collection to add from
     * @return this collection for chaining
     * @since 1.0.0
     */
    @NotNull
    public WeightedCollection<E> addAll(@NotNull WeightedCollection<E> other) {
        Objects.requireNonNull(other, "other cannot be null");
        other.lock.readLock().lock();
        try {
            for (Entry<E> entry : other.entries) {
                add(entry.element(), entry.weight());
            }
        } finally {
            other.lock.readLock().unlock();
        }
        return this;
    }

    // ==================== Removing Elements ====================

    /**
     * Removes an element from the collection.
     *
     * @param element the element to remove
     * @return true if the element was removed
     * @since 1.0.0
     */
    public boolean remove(@NotNull E element) {
        Objects.requireNonNull(element, "element cannot be null");

        lock.writeLock().lock();
        try {
            Entry<E> entry = elementMap.remove(element);
            if (entry != null) {
                entries.remove(entry);
                totalWeight -= entry.weight();
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all elements matching the predicate.
     *
     * @param predicate the predicate to test elements
     * @return the number of elements removed
     * @since 1.0.0
     */
    public int removeIf(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");

        lock.writeLock().lock();
        try {
            int removed = 0;
            var iterator = entries.iterator();
            while (iterator.hasNext()) {
                Entry<E> entry = iterator.next();
                if (predicate.test(entry.element())) {
                    iterator.remove();
                    elementMap.remove(entry.element());
                    totalWeight -= entry.weight();
                    removed++;
                }
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all elements from the collection.
     *
     * @since 1.0.0
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            entries.clear();
            elementMap.clear();
            totalWeight = 0;
            sorted = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== Random Selection ====================

    /**
     * Gets a random element based on weights.
     *
     * @return a randomly selected element, or null if empty
     * @since 1.0.0
     */
    @Nullable
    public E next() {
        lock.readLock().lock();
        try {
            if (entries.isEmpty() || totalWeight <= 0) {
                return null;
            }

            double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
            double cumulative = 0;

            for (Entry<E> entry : entries) {
                cumulative += entry.weight();
                if (random < cumulative) {
                    return entry.element();
                }
            }

            // Fallback to last element (shouldn't normally happen)
            return entries.getLast().element();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a random element, throwing if empty.
     *
     * @return a randomly selected element
     * @throws NoSuchElementException if the collection is empty
     * @since 1.0.0
     */
    @NotNull
    public E nextOrThrow() {
        E result = next();
        if (result == null) {
            throw new NoSuchElementException("WeightedCollection is empty");
        }
        return result;
    }

    /**
     * Gets a random element, returning a default if empty.
     *
     * @param defaultValue the default value to return if empty
     * @return a randomly selected element or the default
     * @since 1.0.0
     */
    @NotNull
    public E nextOrDefault(@NotNull E defaultValue) {
        E result = next();
        return result != null ? result : defaultValue;
    }

    /**
     * Gets multiple random elements (may contain duplicates).
     *
     * @param count the number of elements to select
     * @return a list of randomly selected elements
     * @since 1.0.0
     */
    @NotNull
    public List<E> next(int count) {
        if (count <= 0) {
            return List.of();
        }

        List<E> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            E element = next();
            if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Gets multiple unique random elements (no duplicates).
     *
     * @param count the maximum number of elements to select
     * @return a list of unique randomly selected elements
     * @since 1.0.0
     */
    @NotNull
    public List<E> nextUnique(int count) {
        if (count <= 0) {
            return List.of();
        }

        lock.readLock().lock();
        try {
            if (entries.isEmpty()) {
                return List.of();
            }

            int actualCount = Math.min(count, entries.size());
            Set<E> selected = new LinkedHashSet<>(actualCount);
            int maxAttempts = actualCount * 10; // Prevent infinite loops
            int attempts = 0;

            while (selected.size() < actualCount && attempts < maxAttempts) {
                E element = next();
                if (element != null) {
                    selected.add(element);
                }
                attempts++;
            }

            return new ArrayList<>(selected);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a random element matching the predicate.
     *
     * @param predicate the predicate to filter elements
     * @return a randomly selected matching element, or null if none match
     * @since 1.0.0
     */
    @Nullable
    public E nextWhere(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");

        lock.readLock().lock();
        try {
            // Build filtered list
            List<Entry<E>> filtered = entries.stream()
                    .filter(e -> predicate.test(e.element()))
                    .toList();

            if (filtered.isEmpty()) {
                return null;
            }

            double filteredWeight = filtered.stream()
                    .mapToDouble(Entry::weight)
                    .sum();

            if (filteredWeight <= 0) {
                return null;
            }

            double random = ThreadLocalRandom.current().nextDouble() * filteredWeight;
            double cumulative = 0;

            for (Entry<E> entry : filtered) {
                cumulative += entry.weight();
                if (random < cumulative) {
                    return entry.element();
                }
            }

            return filtered.getLast().element();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Weight Operations ====================

    /**
     * Gets the weight of an element.
     *
     * @param element the element
     * @return the weight, or 0 if the element is not present
     * @since 1.0.0
     */
    public double getWeight(@NotNull E element) {
        Objects.requireNonNull(element, "element cannot be null");

        lock.readLock().lock();
        try {
            Entry<E> entry = elementMap.get(element);
            return entry != null ? entry.weight() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets the weight of an existing element.
     *
     * @param element the element
     * @param weight  the new weight
     * @return true if the element exists and weight was updated
     * @since 1.0.0
     */
    public boolean setWeight(@NotNull E element, double weight) {
        Objects.requireNonNull(element, "element cannot be null");
        if (weight < 0) {
            throw new IllegalArgumentException("weight cannot be negative");
        }

        lock.writeLock().lock();
        try {
            if (!elementMap.containsKey(element)) {
                return false;
            }
            add(element, weight); // Re-add with new weight
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adjusts the weight of an existing element by a delta.
     *
     * @param element the element
     * @param delta   the amount to add to the weight (can be negative)
     * @return true if the element exists and weight was adjusted
     * @since 1.0.0
     */
    public boolean adjustWeight(@NotNull E element, double delta) {
        lock.writeLock().lock();
        try {
            Entry<E> entry = elementMap.get(element);
            if (entry == null) {
                return false;
            }
            double newWeight = Math.max(0, entry.weight() + delta);
            return setWeight(element, newWeight);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the chance (probability 0-1) of selecting an element.
     *
     * @param element the element
     * @return the probability of selection (0 to 1)
     * @since 1.0.0
     */
    public double getChance(@NotNull E element) {
        lock.readLock().lock();
        try {
            if (totalWeight <= 0) {
                return 0;
            }
            return getWeight(element) / totalWeight;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the percentage chance (0-100) of selecting an element.
     *
     * @param element the element
     * @return the percentage chance (0 to 100)
     * @since 1.0.0
     */
    public double getPercentage(@NotNull E element) {
        return getChance(element) * 100;
    }

    /**
     * Gets the total weight of all elements.
     *
     * @return the total weight
     * @since 1.0.0
     */
    public double getTotalWeight() {
        lock.readLock().lock();
        try {
            return totalWeight;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Query Methods ====================

    /**
     * Checks if the collection contains an element.
     *
     * @param element the element to check
     * @return true if the element is present
     * @since 1.0.0
     */
    public boolean contains(@NotNull E element) {
        Objects.requireNonNull(element, "element cannot be null");
        lock.readLock().lock();
        try {
            return elementMap.containsKey(element);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if the collection is empty.
     *
     * @return true if empty
     * @since 1.0.0
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return entries.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the number of elements in the collection.
     *
     * @return the size
     * @since 1.0.0
     */
    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all elements as a list.
     *
     * @return an unmodifiable list of all elements
     * @since 1.0.0
     */
    @NotNull
    public List<E> getElements() {
        lock.readLock().lock();
        try {
            return entries.stream()
                    .map(Entry::element)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all elements sorted by weight (highest first).
     *
     * @return a list sorted by weight descending
     * @since 1.0.0
     */
    @NotNull
    public List<E> getElementsByWeight() {
        lock.readLock().lock();
        try {
            return entries.stream()
                    .sorted((a, b) -> Double.compare(b.weight(), a.weight()))
                    .map(Entry::element)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a stream of the elements.
     *
     * @return a stream of elements
     * @since 1.0.0
     */
    @NotNull
    public Stream<E> stream() {
        lock.readLock().lock();
        try {
            // Create a copy to avoid concurrent modification
            return new ArrayList<>(entries).stream().map(Entry::element);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        lock.readLock().lock();
        try {
            // Return iterator over a copy
            return new ArrayList<>(getElements()).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Normalizes all weights so they sum to 1.0.
     *
     * @return this collection for chaining
     * @since 1.0.0
     */
    @NotNull
    public WeightedCollection<E> normalize() {
        lock.writeLock().lock();
        try {
            if (totalWeight <= 0 || entries.isEmpty()) {
                return this;
            }

            List<Entry<E>> oldEntries = new ArrayList<>(entries);
            entries.clear();
            elementMap.clear();
            totalWeight = 0;

            for (Entry<E> entry : oldEntries) {
                double normalizedWeight = entry.weight() / totalWeight;
                add(entry.element(), normalizedWeight);
            }

            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Scales all weights by a factor.
     *
     * @param factor the factor to multiply weights by
     * @return this collection for chaining
     * @since 1.0.0
     */
    @NotNull
    public WeightedCollection<E> scale(double factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("factor cannot be negative");
        }

        lock.writeLock().lock();
        try {
            List<Entry<E>> oldEntries = new ArrayList<>(entries);
            entries.clear();
            elementMap.clear();
            totalWeight = 0;

            for (Entry<E> entry : oldEntries) {
                add(entry.element(), entry.weight() * factor);
            }

            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates a deep copy of this collection.
     *
     * @return a new WeightedCollection with the same elements and weights
     * @since 1.0.0
     */
    @NotNull
    public WeightedCollection<E> copy() {
        WeightedCollection<E> copy = new WeightedCollection<>(size());
        copy.addAll(this);
        return copy;
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder("WeightedCollection{");
            boolean first = true;
            for (Entry<E> entry : entries) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(entry.element())
                        .append("=")
                        .append(String.format("%.2f", entry.weight()))
                        .append(" (")
                        .append(String.format("%.1f%%", getPercentage(entry.element())))
                        .append(")");
            }
            sb.append("}");
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
}
