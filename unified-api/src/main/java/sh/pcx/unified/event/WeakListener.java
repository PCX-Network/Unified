/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

/**
 * A wrapper for event listeners that uses weak references.
 *
 * <p>Weak listeners are automatically unregistered when the underlying listener
 * object is garbage collected. This is useful for:
 * <ul>
 *   <li>Optional plugin integrations that may be unloaded</li>
 *   <li>GUI-related listeners tied to temporary objects</li>
 *   <li>Listeners in systems where explicit cleanup is difficult</li>
 *   <li>Preventing memory leaks from forgotten listeners</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <p>The weak listener holds a {@link WeakReference} to the actual listener.
 * When the event system attempts to invoke a handler, it first checks if the
 * underlying listener is still available. If the listener has been garbage
 * collected, the handler is automatically unregistered.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create a weak listener wrapper
 * MyListener listener = new MyListener();
 * WeakListener<MyListener> weakListener = WeakListener.of(listener);
 *
 * // Register with the event bus
 * eventBus.register(plugin, weakListener);
 *
 * // Later, if 'listener' is no longer strongly referenced and GC runs,
 * // the listener will be automatically cleaned up
 * listener = null;
 * System.gc(); // After GC, the weak listener becomes invalid
 * }</pre>
 *
 * <h2>Implementing WeakListener Directly</h2>
 * <p>You can also implement this interface directly in your listener class
 * to indicate it should be registered with weak reference semantics:
 *
 * <pre>{@code
 * public class OptionalIntegrationListener implements EventListener, WeakListener<OptionalIntegrationListener> {
 *
 *     @Override
 *     public Optional<OptionalIntegrationListener> getListener() {
 *         return Optional.of(this);
 *     }
 *
 *     @EventHandler
 *     public void onEvent(SomeEvent event) {
 *         // Handle event
 *     }
 * }
 * }</pre>
 *
 * <h2>Important Notes</h2>
 * <ul>
 *   <li>The listener must be strongly referenced somewhere else to prevent
 *       immediate garbage collection</li>
 *   <li>There's no guarantee when garbage collection will occur</li>
 *   <li>Cleanup happens lazily when an event is dispatched</li>
 *   <li>You can manually check if the listener is still valid using
 *       {@link #isAlive()}</li>
 * </ul>
 *
 * @param <T> the type of the underlying listener
 * @since 1.0.0
 * @author Supatuck
 * @see EventListener
 * @see EventBus
 */
public interface WeakListener<T extends EventListener> extends EventListener {

    /**
     * Returns the underlying listener if it's still available.
     *
     * <p>If the listener has been garbage collected, this returns an empty
     * Optional, and the event system will automatically unregister this
     * weak listener.
     *
     * @return the underlying listener, or empty if garbage collected
     * @since 1.0.0
     */
    @NotNull
    Optional<T> getListener();

    /**
     * Checks if the underlying listener is still alive.
     *
     * @return true if the listener is still available
     * @since 1.0.0
     */
    default boolean isAlive() {
        return getListener().isPresent();
    }

    /**
     * Returns the underlying listener or null if garbage collected.
     *
     * @return the listener or null
     * @since 1.0.0
     */
    @Nullable
    default T getListenerOrNull() {
        return getListener().orElse(null);
    }

    /**
     * Creates a weak listener wrapper for the given listener.
     *
     * @param listener the listener to wrap
     * @param <T>      the listener type
     * @return a weak listener wrapper
     * @throws NullPointerException if listener is null
     * @since 1.0.0
     */
    @NotNull
    static <T extends EventListener> WeakListener<T> of(@NotNull T listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        return new WeakListenerWrapper<>(listener);
    }

    /**
     * Creates a weak listener wrapper with an expiry callback.
     *
     * <p>The callback is invoked once when the listener is garbage collected
     * and the weak listener is cleaned up.
     *
     * @param listener        the listener to wrap
     * @param expiryCallback  callback invoked when the listener expires
     * @param <T>             the listener type
     * @return a weak listener wrapper with callback
     * @since 1.0.0
     */
    @NotNull
    static <T extends EventListener> WeakListener<T> of(
            @NotNull T listener,
            @NotNull Runnable expiryCallback
    ) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(expiryCallback, "expiryCallback cannot be null");
        return new WeakListenerWrapper<>(listener, expiryCallback);
    }
}

/**
 * Default implementation of WeakListener using WeakReference.
 *
 * @param <T> the listener type
 */
class WeakListenerWrapper<T extends EventListener> implements WeakListener<T> {

    private final WeakReference<T> reference;
    private final Runnable expiryCallback;
    private volatile boolean expired;

    WeakListenerWrapper(T listener) {
        this(listener, null);
    }

    WeakListenerWrapper(T listener, Runnable expiryCallback) {
        this.reference = new WeakReference<>(listener);
        this.expiryCallback = expiryCallback;
        this.expired = false;
    }

    @Override
    @NotNull
    public Optional<T> getListener() {
        T listener = reference.get();
        if (listener == null && !expired) {
            expired = true;
            if (expiryCallback != null) {
                expiryCallback.run();
            }
        }
        return Optional.ofNullable(listener);
    }

    @Override
    public boolean isAlive() {
        return reference.get() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeakListenerWrapper<?> that)) return false;
        T listener = reference.get();
        Object thatListener = that.reference.get();
        return listener != null && listener.equals(thatListener);
    }

    @Override
    public int hashCode() {
        T listener = reference.get();
        return listener != null ? listener.hashCode() : 0;
    }

    @Override
    public String toString() {
        T listener = reference.get();
        return "WeakListener[" + (listener != null ? listener.getClass().getSimpleName() : "expired") + "]";
    }
}
