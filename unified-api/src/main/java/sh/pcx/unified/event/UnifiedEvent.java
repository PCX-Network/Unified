/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events in the unified event system.
 *
 * <p>This class provides the foundation for all events, both platform-bridged events
 * (e.g., PlayerJoinEvent) and custom plugin events. All events have a unique identifier,
 * a timestamp, and an optional name for debugging purposes.
 *
 * <h2>Creating Custom Events</h2>
 * <p>To create a custom event, extend this class and add your event-specific fields
 * and methods. If the event should be cancellable, also implement {@link Cancellable}.
 *
 * <pre>{@code
 * public class CustomRewardEvent extends UnifiedEvent implements Cancellable {
 *
 *     private final UnifiedPlayer player;
 *     private int rewardAmount;
 *     private boolean cancelled;
 *
 *     public CustomRewardEvent(UnifiedPlayer player, int rewardAmount) {
 *         this.player = player;
 *         this.rewardAmount = rewardAmount;
 *     }
 *
 *     public UnifiedPlayer getPlayer() {
 *         return player;
 *     }
 *
 *     public int getRewardAmount() {
 *         return rewardAmount;
 *     }
 *
 *     public void setRewardAmount(int amount) {
 *         this.rewardAmount = amount;
 *     }
 *
 *     @Override
 *     public boolean isCancelled() {
 *         return cancelled;
 *     }
 *
 *     @Override
 *     public void setCancelled(boolean cancelled) {
 *         this.cancelled = cancelled;
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Events are not thread-safe by default. If an event may be accessed from
 * multiple threads, the implementer should ensure proper synchronization or
 * use thread-safe data structures.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Cancellable
 * @see EventBus
 */
public abstract class UnifiedEvent {

    private final UUID eventId;
    private final Instant timestamp;
    private final String eventName;
    private boolean async;

    /**
     * Constructs a new unified event with auto-generated ID and current timestamp.
     *
     * <p>The event name is derived from the class name.
     *
     * @since 1.0.0
     */
    protected UnifiedEvent() {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.eventName = getClass().getSimpleName();
        this.async = false;
    }

    /**
     * Constructs a new unified event with a custom name.
     *
     * @param eventName the custom event name for debugging
     * @since 1.0.0
     */
    protected UnifiedEvent(@NotNull String eventName) {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.eventName = eventName;
        this.async = false;
    }

    /**
     * Constructs a new unified event with async flag.
     *
     * @param async whether this event is fired asynchronously
     * @since 1.0.0
     */
    protected UnifiedEvent(boolean async) {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.eventName = getClass().getSimpleName();
        this.async = async;
    }

    /**
     * Returns the unique identifier for this event instance.
     *
     * <p>Each event instance has a unique ID that can be used for tracking,
     * logging, or correlating related events across the system.
     *
     * @return the unique event ID
     * @since 1.0.0
     */
    @NotNull
    public final UUID getEventId() {
        return eventId;
    }

    /**
     * Returns the timestamp when this event was created.
     *
     * <p>This is the instant when the event object was constructed,
     * which is typically when the event occurred.
     *
     * @return the event creation timestamp
     * @since 1.0.0
     */
    @NotNull
    public final Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the name of this event.
     *
     * <p>By default, this returns the simple class name of the event.
     * This name is primarily used for debugging and logging purposes.
     *
     * @return the event name
     * @since 1.0.0
     */
    @NotNull
    public final String getEventName() {
        return eventName;
    }

    /**
     * Returns whether this event is being called asynchronously.
     *
     * <p>Async events are fired from threads other than the main server thread.
     * Handlers of async events must be thread-safe and should not directly
     * modify game state without proper synchronization.
     *
     * @return true if this event is async
     * @since 1.0.0
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Sets whether this event is being called asynchronously.
     *
     * <p>This method is typically called by the event system and should
     * not be called by plugin developers directly.
     *
     * @param async whether this event is async
     * @since 1.0.0
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * Returns whether this event can be cancelled.
     *
     * <p>This is a convenience method that checks if the event implements
     * the {@link Cancellable} interface.
     *
     * @return true if this event can be cancelled
     * @since 1.0.0
     */
    public final boolean isCancellable() {
        return this instanceof Cancellable;
    }

    /**
     * Returns the class of this event.
     *
     * <p>This method is useful for event dispatching and filtering.
     *
     * @return the event class
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public final Class<? extends UnifiedEvent> getEventClass() {
        return (Class<? extends UnifiedEvent>) getClass();
    }

    /**
     * Returns a string representation of this event.
     *
     * <p>The default implementation includes the event name and ID.
     * Subclasses may override this to include additional information.
     *
     * @return a string representation of this event
     * @since 1.0.0
     */
    @Override
    public String toString() {
        return eventName + "[id=" + eventId + ", timestamp=" + timestamp + ", async=" + async + "]";
    }
}
