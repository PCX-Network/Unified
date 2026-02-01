/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.async;

import sh.pcx.unified.event.UnifiedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for events that are designed to be processed asynchronously.
 *
 * <p>Unlike regular events that are fired synchronously, async events are
 * intended to be handled on async threads. They provide additional features
 * for tracking completion, handling errors, and coordinating between
 * async handlers and sync code.
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Database operations that shouldn't block the main thread</li>
 *   <li>Network requests (REST APIs, external services)</li>
 *   <li>File I/O operations</li>
 *   <li>Complex calculations</li>
 *   <li>Batch processing</li>
 * </ul>
 *
 * <h2>Creating an Async Event</h2>
 * <pre>{@code
 * public class DataLoadEvent extends AsyncEvent {
 *
 *     private final UUID playerId;
 *     private PlayerData result;
 *
 *     public DataLoadEvent(UUID playerId) {
 *         this.playerId = playerId;
 *     }
 *
 *     public UUID getPlayerId() {
 *         return playerId;
 *     }
 *
 *     public void setResult(PlayerData result) {
 *         this.result = result;
 *         complete(); // Mark as complete
 *     }
 *
 *     public PlayerData getResult() {
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * <h2>Handling Async Events</h2>
 * <pre>{@code
 * @AsyncHandler
 * public void onDataLoad(DataLoadEvent event) {
 *     try {
 *         PlayerData data = database.loadPlayer(event.getPlayerId());
 *         event.setResult(data);
 *     } catch (Exception e) {
 *         event.completeExceptionally(e);
 *     }
 * }
 * }</pre>
 *
 * <h2>Firing and Awaiting</h2>
 * <pre>{@code
 * DataLoadEvent event = new DataLoadEvent(playerId);
 *
 * // Fire and forget
 * eventBus.fireAsync(event);
 *
 * // Fire and await completion
 * eventBus.fireAsync(event).thenAccept(e -> {
 *     if (e.isCompleted() && !e.hasError()) {
 *         PlayerData data = e.getResult();
 *         // Use data
 *     }
 * });
 *
 * // Blocking wait (use sparingly!)
 * event.await(5, TimeUnit.SECONDS);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AsyncEventHandler
 * @see EventFuture
 */
public abstract class AsyncEvent extends UnifiedEvent {

    private final AtomicBoolean completed;
    private final AtomicReference<Throwable> error;
    private final CountDownLatch completionLatch;

    /**
     * Constructs a new async event.
     *
     * @since 1.0.0
     */
    protected AsyncEvent() {
        super(true); // Always mark as async
        this.completed = new AtomicBoolean(false);
        this.error = new AtomicReference<>(null);
        this.completionLatch = new CountDownLatch(1);
    }

    /**
     * Constructs a new async event with a custom name.
     *
     * @param eventName the event name
     * @since 1.0.0
     */
    protected AsyncEvent(@NotNull String eventName) {
        super(eventName);
        setAsync(true);
        this.completed = new AtomicBoolean(false);
        this.error = new AtomicReference<>(null);
        this.completionLatch = new CountDownLatch(1);
    }

    /**
     * Marks this event as completed successfully.
     *
     * <p>Call this method when async processing is finished. This releases
     * any threads waiting on {@link #await()} or {@link #await(long, TimeUnit)}.
     *
     * @since 1.0.0
     */
    public void complete() {
        if (completed.compareAndSet(false, true)) {
            completionLatch.countDown();
        }
    }

    /**
     * Marks this event as completed with an error.
     *
     * <p>Call this method when async processing fails. The error will be
     * available via {@link #getError()}.
     *
     * @param error the error that occurred
     * @since 1.0.0
     */
    public void completeExceptionally(@NotNull Throwable error) {
        this.error.set(error);
        complete();
    }

    /**
     * Returns whether this event has been completed.
     *
     * @return true if completed (successfully or with error)
     * @since 1.0.0
     */
    public boolean isCompleted() {
        return completed.get();
    }

    /**
     * Returns whether this event completed with an error.
     *
     * @return true if an error occurred
     * @since 1.0.0
     */
    public boolean hasError() {
        return error.get() != null;
    }

    /**
     * Returns the error that occurred during processing.
     *
     * @return the error, or null if no error occurred
     * @since 1.0.0
     */
    public Throwable getError() {
        return error.get();
    }

    /**
     * Awaits completion of this event indefinitely.
     *
     * <p><b>Warning:</b> This blocks the current thread. Use with caution
     * and never call on the main server thread.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     * @since 1.0.0
     */
    public void await() throws InterruptedException {
        completionLatch.await();
    }

    /**
     * Awaits completion of this event with a timeout.
     *
     * <p><b>Warning:</b> This blocks the current thread. Use with caution
     * and never call on the main server thread.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit
     * @return true if completed before timeout, false if timed out
     * @throws InterruptedException if the thread is interrupted while waiting
     * @since 1.0.0
     */
    public boolean await(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return completionLatch.await(timeout, unit);
    }

    /**
     * Returns whether this event is pending (not yet completed).
     *
     * @return true if still pending
     * @since 1.0.0
     */
    public boolean isPending() {
        return !completed.get();
    }

    /**
     * Returns whether this event completed successfully (no error).
     *
     * @return true if completed without error
     * @since 1.0.0
     */
    public boolean isSuccessful() {
        return completed.get() && error.get() == null;
    }

    @Override
    public String toString() {
        return getEventName() + "[id=" + getEventId()
                + ", completed=" + completed.get()
                + ", error=" + (error.get() != null ? error.get().getClass().getSimpleName() : "none")
                + "]";
    }
}
