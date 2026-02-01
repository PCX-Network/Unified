/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.update;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Represents an update interval for scoreboard refreshing.
 *
 * <p>Update intervals define how frequently the scoreboard content
 * should be refreshed. Predefined intervals are available for common
 * use cases, and custom intervals can be created.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface UpdateInterval {

    /**
     * Very fast update interval (50ms / every tick).
     */
    UpdateInterval VERY_FAST = of(Duration.ofMillis(50));

    /**
     * Fast update interval (100ms / every 2 ticks).
     */
    UpdateInterval FAST = of(Duration.ofMillis(100));

    /**
     * Normal update interval (250ms / every 5 ticks).
     */
    UpdateInterval NORMAL = of(Duration.ofMillis(250));

    /**
     * Slow update interval (500ms / every 10 ticks).
     */
    UpdateInterval SLOW = of(Duration.ofMillis(500));

    /**
     * Very slow update interval (1000ms / every 20 ticks).
     */
    UpdateInterval VERY_SLOW = of(Duration.ofMillis(1000));

    /**
     * Creates an update interval with the given duration.
     *
     * @param duration the interval duration
     * @return the update interval
     * @since 1.0.0
     */
    @NotNull
    static UpdateInterval of(@NotNull Duration duration) {
        return new DefaultUpdateInterval(duration);
    }

    /**
     * Creates a custom update interval with the given duration.
     *
     * @param duration the interval duration
     * @return the update interval
     * @since 1.0.0
     */
    @NotNull
    static UpdateInterval custom(@NotNull Duration duration) {
        return of(duration);
    }

    /**
     * Returns the duration of this update interval.
     *
     * @return the interval duration
     * @since 1.0.0
     */
    @NotNull
    Duration getDuration();

    /**
     * Returns the interval in milliseconds.
     *
     * @return the interval in milliseconds
     * @since 1.0.0
     */
    long toMillis();

    /**
     * Returns the interval in ticks (assuming 50ms per tick).
     *
     * @return the interval in ticks
     * @since 1.0.0
     */
    long toTicks();
}

/**
 * Default implementation of UpdateInterval.
 */
class DefaultUpdateInterval implements UpdateInterval {

    private final Duration duration;

    DefaultUpdateInterval(@NotNull Duration duration) {
        this.duration = duration;
    }

    @Override
    public @NotNull Duration getDuration() {
        return duration;
    }

    @Override
    public long toMillis() {
        return duration.toMillis();
    }

    @Override
    public long toTicks() {
        return duration.toMillis() / 50;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultUpdateInterval that = (DefaultUpdateInterval) o;
        return duration.equals(that.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

    @Override
    public String toString() {
        return "UpdateInterval{" + duration.toMillis() + "ms}";
    }
}
