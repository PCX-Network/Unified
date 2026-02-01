/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.concurrent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A thread-safe observable value wrapper that notifies listeners on changes.
 *
 * <p>Useful for reactive programming patterns where you want to trigger
 * actions when a value changes.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ObservableValue<Integer> health = new ObservableValue<>(100);
 *
 * // Add change listener
 * health.onChange((oldValue, newValue) -> {
 *     player.sendMessage("Health changed from " + oldValue + " to " + newValue);
 * });
 *
 * // Update value
 * health.set(80); // Triggers listener
 *
 * // Conditional update
 * health.updateIf(h -> h > 0, h -> h - 10);
 *
 * // Transform value
 * health.update(h -> Math.min(100, h + 20));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations are atomic.
 *
 * @param <T> the type of the value
 * @since 1.0.0
 * @author Supatuck
 */
public final class ObservableValue<T> {

    /**
     * Listener interface for value changes.
     *
     * @param <T> the value type
     */
    @FunctionalInterface
    public interface ChangeListener<T> {
        /**
         * Called when the value changes.
         *
         * @param oldValue the previous value
         * @param newValue the new value
         */
        void onChange(@Nullable T oldValue, @Nullable T newValue);
    }

    private final AtomicReference<T> value;
    private final List<ChangeListener<T>> listeners;
    private volatile boolean notifying;

    /**
     * Creates a new ObservableValue with a null initial value.
     *
     * @since 1.0.0
     */
    public ObservableValue() {
        this(null);
    }

    /**
     * Creates a new ObservableValue with the specified initial value.
     *
     * @param initialValue the initial value
     * @since 1.0.0
     */
    public ObservableValue(@Nullable T initialValue) {
        this.value = new AtomicReference<>(initialValue);
        this.listeners = new CopyOnWriteArrayList<>();
        this.notifying = false;
    }

    /**
     * Creates an ObservableValue with the specified initial value.
     *
     * @param value the initial value
     * @param <T>   the value type
     * @return a new ObservableValue
     * @since 1.0.0
     */
    @NotNull
    public static <T> ObservableValue<T> of(@Nullable T value) {
        return new ObservableValue<>(value);
    }

    /**
     * Creates an ObservableValue with a null initial value.
     *
     * @param <T> the value type
     * @return a new ObservableValue
     * @since 1.0.0
     */
    @NotNull
    public static <T> ObservableValue<T> empty() {
        return new ObservableValue<>();
    }

    // ==================== Value Access ====================

    /**
     * Gets the current value.
     *
     * @return the current value
     * @since 1.0.0
     */
    @Nullable
    public T get() {
        return value.get();
    }

    /**
     * Gets the current value, or a default if null.
     *
     * @param defaultValue the default value
     * @return the current value or default
     * @since 1.0.0
     */
    @NotNull
    public T getOrDefault(@NotNull T defaultValue) {
        T current = value.get();
        return current != null ? current : defaultValue;
    }

    /**
     * Checks if the value is null.
     *
     * @return true if the value is null
     * @since 1.0.0
     */
    public boolean isNull() {
        return value.get() == null;
    }

    /**
     * Checks if the value is not null.
     *
     * @return true if the value is not null
     * @since 1.0.0
     */
    public boolean isPresent() {
        return value.get() != null;
    }

    // ==================== Value Modification ====================

    /**
     * Sets a new value and notifies listeners if changed.
     *
     * @param newValue the new value
     * @return the old value
     * @since 1.0.0
     */
    @Nullable
    public T set(@Nullable T newValue) {
        T oldValue = value.getAndSet(newValue);
        if (!Objects.equals(oldValue, newValue)) {
            notifyListeners(oldValue, newValue);
        }
        return oldValue;
    }

    /**
     * Sets a new value only if the current value is null.
     *
     * @param newValue the new value
     * @return true if the value was set
     * @since 1.0.0
     */
    public boolean setIfAbsent(@Nullable T newValue) {
        boolean set = value.compareAndSet(null, newValue);
        if (set && newValue != null) {
            notifyListeners(null, newValue);
        }
        return set;
    }

    /**
     * Sets a new value only if the current value equals the expected value.
     *
     * @param expectedValue the expected current value
     * @param newValue      the new value
     * @return true if the value was updated
     * @since 1.0.0
     */
    public boolean compareAndSet(@Nullable T expectedValue, @Nullable T newValue) {
        boolean updated = value.compareAndSet(expectedValue, newValue);
        if (updated && !Objects.equals(expectedValue, newValue)) {
            notifyListeners(expectedValue, newValue);
        }
        return updated;
    }

    /**
     * Updates the value using a function and notifies listeners if changed.
     *
     * @param updateFunction the update function
     * @return the new value
     * @since 1.0.0
     */
    @Nullable
    public T update(@NotNull UnaryOperator<T> updateFunction) {
        Objects.requireNonNull(updateFunction, "updateFunction cannot be null");

        T[] oldValueHolder = (T[]) new Object[1];
        T newValue = value.updateAndGet(current -> {
            oldValueHolder[0] = current;
            return updateFunction.apply(current);
        });

        if (!Objects.equals(oldValueHolder[0], newValue)) {
            notifyListeners(oldValueHolder[0], newValue);
        }

        return newValue;
    }

    /**
     * Updates the value only if it matches the predicate.
     *
     * @param condition      the condition to check
     * @param updateFunction the update function
     * @return true if the value was updated
     * @since 1.0.0
     */
    public boolean updateIf(@NotNull java.util.function.Predicate<T> condition,
                            @NotNull UnaryOperator<T> updateFunction) {
        Objects.requireNonNull(condition, "condition cannot be null");
        Objects.requireNonNull(updateFunction, "updateFunction cannot be null");

        T current = value.get();
        if (!condition.test(current)) {
            return false;
        }

        T newValue = updateFunction.apply(current);
        boolean updated = value.compareAndSet(current, newValue);

        if (updated && !Objects.equals(current, newValue)) {
            notifyListeners(current, newValue);
        }

        return updated;
    }

    /**
     * Clears the value (sets to null) and notifies listeners.
     *
     * @return the old value
     * @since 1.0.0
     */
    @Nullable
    public T clear() {
        return set(null);
    }

    // ==================== Listeners ====================

    /**
     * Adds a change listener.
     *
     * @param listener the listener to add
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableValue<T> onChange(@NotNull ChangeListener<T> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
        return this;
    }

    /**
     * Adds a listener that only receives the new value.
     *
     * @param listener the listener to add
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ObservableValue<T> onNewValue(@NotNull Consumer<T> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add((oldValue, newValue) -> listener.accept(newValue));
        return this;
    }

    /**
     * Removes a change listener.
     *
     * @param listener the listener to remove
     * @return true if the listener was removed
     * @since 1.0.0
     */
    public boolean removeListener(@NotNull ChangeListener<T> listener) {
        return listeners.remove(listener);
    }

    /**
     * Removes all listeners.
     *
     * @since 1.0.0
     */
    public void clearListeners() {
        listeners.clear();
    }

    /**
     * Gets the number of registered listeners.
     *
     * @return the listener count
     * @since 1.0.0
     */
    public int getListenerCount() {
        return listeners.size();
    }

    // ==================== Derived Observables ====================

    /**
     * Creates a derived observable that transforms this value.
     *
     * @param mapper the transformation function
     * @param <R>    the result type
     * @return a new ObservableValue that updates when this one changes
     * @since 1.0.0
     */
    @NotNull
    public <R> ObservableValue<R> map(@NotNull Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");

        ObservableValue<R> derived = new ObservableValue<>(mapper.apply(get()));
        onChange((oldValue, newValue) -> derived.set(mapper.apply(newValue)));
        return derived;
    }

    /**
     * Binds this observable to update when the source changes.
     *
     * @param source the source observable
     * @param <S>    the source type
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public <S> ObservableValue<T> bindTo(@NotNull ObservableValue<S> source,
                                          @NotNull Function<S, T> mapper) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");

        // Set initial value
        set(mapper.apply(source.get()));

        // Bind to changes
        source.onChange((oldValue, newValue) -> set(mapper.apply(newValue)));

        return this;
    }

    // ==================== Helper Methods ====================

    private void notifyListeners(T oldValue, T newValue) {
        if (notifying) {
            return; // Prevent recursive notifications
        }

        notifying = true;
        try {
            for (ChangeListener<T> listener : listeners) {
                try {
                    listener.onChange(oldValue, newValue);
                } catch (Exception e) {
                    // Log but don't propagate
                }
            }
        } finally {
            notifying = false;
        }
    }

    @Override
    public String toString() {
        return "ObservableValue{" + value.get() + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ObservableValue<?> other)) return false;
        return Objects.equals(value.get(), other.value.get());
    }

    @Override
    public int hashCode() {
        T current = value.get();
        return current != null ? current.hashCode() : 0;
    }
}
