/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.event;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Collects events of a specific type for testing assertions.
 *
 * <p>EventCollector allows tests to capture all events of a specific type
 * and then make assertions about the events that were fired.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create collector
 * EventCollector<PlayerJoinEvent> collector = server.collectEvents(PlayerJoinEvent.class);
 *
 * // Trigger events
 * MockPlayer player = server.addPlayer("Steve");
 *
 * // Assert on collected events
 * assertThat(collector.getEvents()).hasSize(1);
 * assertThat(collector.getFirst().getPlayer().getName()).isEqualTo("Steve");
 *
 * // With filtering
 * List<PlayerJoinEvent> adminJoins = collector.filter(e -> e.getPlayer().isOp());
 *
 * // Wait for async events
 * collector.waitForEvents(5, Duration.ofSeconds(2));
 * }</pre>
 *
 * @param <E> the event type to collect
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class EventCollector<E> {

    private final Class<E> eventType;
    private final List<CollectedEvent<E>> events = new CopyOnWriteArrayList<>();
    private volatile boolean collecting = true;

    /**
     * Creates a new event collector.
     *
     * @param eventType the event class to collect
     */
    public EventCollector(@NotNull Class<E> eventType) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
    }

    /**
     * Collects an event (called by the plugin manager).
     *
     * @param event the event to collect
     */
    void collect(@NotNull E event) {
        if (collecting) {
            events.add(new CollectedEvent<>(event, Instant.now()));
        }
    }

    /**
     * Returns all collected events.
     *
     * @return unmodifiable list of events
     */
    @NotNull
    public List<E> getEvents() {
        return events.stream()
            .map(CollectedEvent::event)
            .toList();
    }

    /**
     * Returns all collected events with timestamps.
     *
     * @return unmodifiable list of collected event records
     */
    @NotNull
    public List<CollectedEvent<E>> getCollectedEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Returns a stream of collected events.
     *
     * @return stream of events
     */
    @NotNull
    public Stream<E> stream() {
        return events.stream().map(CollectedEvent::event);
    }

    /**
     * Returns the number of collected events.
     *
     * @return the event count
     */
    public int size() {
        return events.size();
    }

    /**
     * Checks if any events were collected.
     *
     * @return true if at least one event was collected
     */
    public boolean hasEvents() {
        return !events.isEmpty();
    }

    /**
     * Checks if no events were collected.
     *
     * @return true if no events were collected
     */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Returns the first collected event.
     *
     * @return the first event
     * @throws NoSuchElementException if no events were collected
     */
    @NotNull
    public E getFirst() {
        if (events.isEmpty()) {
            throw new NoSuchElementException("No events of type " + eventType.getSimpleName() + " were collected");
        }
        return events.getFirst().event();
    }

    /**
     * Returns the first collected event, or null if none.
     *
     * @return the first event or null
     */
    public E getFirstOrNull() {
        return events.isEmpty() ? null : events.getFirst().event();
    }

    /**
     * Returns the last collected event.
     *
     * @return the last event
     * @throws NoSuchElementException if no events were collected
     */
    @NotNull
    public E getLast() {
        if (events.isEmpty()) {
            throw new NoSuchElementException("No events of type " + eventType.getSimpleName() + " were collected");
        }
        return events.getLast().event();
    }

    /**
     * Returns the event at a specific index.
     *
     * @param index the index
     * @return the event at the index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    @NotNull
    public E get(int index) {
        return events.get(index).event();
    }

    /**
     * Filters events by a predicate.
     *
     * @param predicate the filter predicate
     * @return list of matching events
     */
    @NotNull
    public List<E> filter(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return events.stream()
            .map(CollectedEvent::event)
            .filter(predicate)
            .toList();
    }

    /**
     * Checks if any event matches the predicate.
     *
     * @param predicate the predicate to test
     * @return true if any event matches
     */
    public boolean anyMatch(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return events.stream()
            .map(CollectedEvent::event)
            .anyMatch(predicate);
    }

    /**
     * Checks if all events match the predicate.
     *
     * @param predicate the predicate to test
     * @return true if all events match
     */
    public boolean allMatch(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return events.stream()
            .map(CollectedEvent::event)
            .allMatch(predicate);
    }

    /**
     * Checks if no events match the predicate.
     *
     * @param predicate the predicate to test
     * @return true if no events match
     */
    public boolean noneMatch(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return events.stream()
            .map(CollectedEvent::event)
            .noneMatch(predicate);
    }

    /**
     * Counts events matching a predicate.
     *
     * @param predicate the predicate to test
     * @return the count of matching events
     */
    public long count(@NotNull Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return events.stream()
            .map(CollectedEvent::event)
            .filter(predicate)
            .count();
    }

    /**
     * Waits for a specific number of events.
     *
     * @param count   the number of events to wait for
     * @param timeout the maximum time to wait
     * @return true if the expected count was reached
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean waitForEvents(int count, @NotNull Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout cannot be null");

        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (events.size() < count) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(10);
        }
        return true;
    }

    /**
     * Waits for at least one event.
     *
     * @param timeout the maximum time to wait
     * @return true if at least one event was collected
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean waitForEvent(@NotNull Duration timeout) throws InterruptedException {
        return waitForEvents(1, timeout);
    }

    /**
     * Clears all collected events.
     */
    public void clear() {
        events.clear();
    }

    /**
     * Pauses event collection.
     */
    public void pause() {
        collecting = false;
    }

    /**
     * Resumes event collection.
     */
    public void resume() {
        collecting = true;
    }

    /**
     * Checks if the collector is actively collecting.
     *
     * @return true if collecting
     */
    public boolean isCollecting() {
        return collecting;
    }

    /**
     * Returns the event type being collected.
     *
     * @return the event class
     */
    @NotNull
    public Class<E> getEventType() {
        return eventType;
    }

    /**
     * Asserts that exactly the expected number of events were collected.
     *
     * @param expectedCount the expected count
     * @throws AssertionError if the count doesn't match
     */
    public void assertCount(int expectedCount) {
        if (events.size() != expectedCount) {
            throw new AssertionError(
                "Expected " + expectedCount + " events of type " + eventType.getSimpleName() +
                " but found " + events.size()
            );
        }
    }

    /**
     * Asserts that at least one event was collected.
     *
     * @throws AssertionError if no events were collected
     */
    public void assertHasEvents() {
        if (events.isEmpty()) {
            throw new AssertionError(
                "Expected at least one event of type " + eventType.getSimpleName() + " but none were collected"
            );
        }
    }

    /**
     * Asserts that no events were collected.
     *
     * @throws AssertionError if any events were collected
     */
    public void assertEmpty() {
        if (!events.isEmpty()) {
            throw new AssertionError(
                "Expected no events of type " + eventType.getSimpleName() +
                " but found " + events.size()
            );
        }
    }

    @Override
    public String toString() {
        return "EventCollector{" +
            "eventType=" + eventType.getSimpleName() +
            ", count=" + events.size() +
            ", collecting=" + collecting +
            '}';
    }

    /**
     * Record representing a collected event with its timestamp.
     *
     * @param <E> the event type
     */
    public record CollectedEvent<E>(E event, Instant timestamp) {}
}
