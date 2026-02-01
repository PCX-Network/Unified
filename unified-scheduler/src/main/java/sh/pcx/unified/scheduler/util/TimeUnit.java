/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.util;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Extended time unit support for scheduler operations.
 *
 * <p>This enum provides time units specifically useful for Minecraft
 * scheduling, including tick-based units and Minecraft-specific
 * time periods like day/night cycles.
 *
 * <h2>Standard Units</h2>
 * <ul>
 *   <li>TICKS - Raw server ticks</li>
 *   <li>MILLISECONDS - 1/1000 second</li>
 *   <li>SECONDS - Standard second</li>
 *   <li>MINUTES - 60 seconds</li>
 *   <li>HOURS - 60 minutes</li>
 * </ul>
 *
 * <h2>Minecraft Units</h2>
 * <ul>
 *   <li>MINECRAFT_DAYS - 20 real minutes (24000 ticks)</li>
 *   <li>MINECRAFT_HOURS - ~50 real seconds (1000 ticks)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Convert to ticks
 * long ticks = TimeUnit.SECONDS.toTicks(5);    // 100 ticks
 * long ticks = TimeUnit.MINUTES.toTicks(2);    // 2400 ticks
 *
 * // Convert between units
 * long seconds = TimeUnit.TICKS.toSeconds(100); // 5 seconds
 * long minutes = TimeUnit.SECONDS.toMinutes(120); // 2 minutes
 *
 * // Minecraft time
 * long dayTicks = TimeUnit.MINECRAFT_DAYS.toTicks(1); // 24000 ticks
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Ticks
 */
public enum TimeUnit {

    /**
     * Raw server ticks (1 tick = 50ms at 20 TPS).
     */
    TICKS(1L),

    /**
     * Milliseconds (1 tick = 50ms).
     */
    MILLISECONDS(1L) {
        @Override
        public long toTicks(long value) {
            return value / Ticks.MILLIS_PER_TICK;
        }

        @Override
        public long fromTicks(long ticks) {
            return ticks * Ticks.MILLIS_PER_TICK;
        }
    },

    /**
     * Seconds (20 ticks per second).
     */
    SECONDS(Ticks.SECOND),

    /**
     * Minutes (1200 ticks per minute).
     */
    MINUTES(Ticks.MINUTE),

    /**
     * Hours (72000 ticks per hour).
     */
    HOURS(Ticks.HOUR),

    /**
     * Minecraft hours (1000 ticks, ~50 real seconds).
     *
     * <p>A Minecraft day consists of 24 Minecraft hours.
     */
    MINECRAFT_HOURS(1000L),

    /**
     * Minecraft days (24000 ticks, 20 real minutes).
     *
     * <p>One full day/night cycle in Minecraft.
     */
    MINECRAFT_DAYS(24000L);

    private final long ticksPerUnit;

    TimeUnit(long ticksPerUnit) {
        this.ticksPerUnit = ticksPerUnit;
    }

    /**
     * Converts a value in this unit to ticks.
     *
     * @param value the value in this unit
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public long toTicks(long value) {
        return value * ticksPerUnit;
    }

    /**
     * Converts a value in this unit to ticks.
     *
     * @param value the value in this unit
     * @return the equivalent number of ticks (rounded)
     * @since 1.0.0
     */
    public long toTicks(double value) {
        return Math.round(value * ticksPerUnit);
    }

    /**
     * Converts ticks to this unit.
     *
     * @param ticks the number of ticks
     * @return the equivalent value in this unit
     * @since 1.0.0
     */
    public long fromTicks(long ticks) {
        return ticks / ticksPerUnit;
    }

    /**
     * Converts ticks to this unit with decimal precision.
     *
     * @param ticks the number of ticks
     * @return the equivalent value in this unit
     * @since 1.0.0
     */
    public double fromTicksExact(long ticks) {
        return (double) ticks / ticksPerUnit;
    }

    /**
     * Converts a value from this unit to another unit.
     *
     * @param value      the value in this unit
     * @param targetUnit the target unit
     * @return the equivalent value in the target unit
     * @since 1.0.0
     */
    public long convert(long value, @NotNull TimeUnit targetUnit) {
        return targetUnit.fromTicks(this.toTicks(value));
    }

    /**
     * Converts a value from this unit to milliseconds.
     *
     * @param value the value in this unit
     * @return the equivalent milliseconds
     * @since 1.0.0
     */
    public long toMillis(long value) {
        return Ticks.toMillis(toTicks(value));
    }

    /**
     * Converts a value from this unit to seconds.
     *
     * @param value the value in this unit
     * @return the equivalent seconds
     * @since 1.0.0
     */
    public long toSeconds(long value) {
        return SECONDS.fromTicks(toTicks(value));
    }

    /**
     * Converts a value from this unit to minutes.
     *
     * @param value the value in this unit
     * @return the equivalent minutes
     * @since 1.0.0
     */
    public long toMinutes(long value) {
        return MINUTES.fromTicks(toTicks(value));
    }

    /**
     * Converts a value from this unit to hours.
     *
     * @param value the value in this unit
     * @return the equivalent hours
     * @since 1.0.0
     */
    public long toHours(long value) {
        return HOURS.fromTicks(toTicks(value));
    }

    /**
     * Converts a value from this unit to a Duration.
     *
     * @param value the value in this unit
     * @return the equivalent Duration
     * @since 1.0.0
     */
    @NotNull
    public Duration toDuration(long value) {
        return Ticks.toDuration(toTicks(value));
    }

    /**
     * Returns the number of ticks per unit of this time unit.
     *
     * @return ticks per unit
     * @since 1.0.0
     */
    public long getTicksPerUnit() {
        return ticksPerUnit;
    }

    /**
     * Converts from java.util.concurrent.TimeUnit.
     *
     * @param unit the java TimeUnit
     * @return the equivalent scheduler TimeUnit
     * @since 1.0.0
     */
    @NotNull
    public static TimeUnit from(@NotNull java.util.concurrent.TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS, MICROSECONDS, MILLISECONDS -> MILLISECONDS;
            case SECONDS -> SECONDS;
            case MINUTES -> MINUTES;
            case HOURS, DAYS -> HOURS;
        };
    }

    /**
     * Converts a java.util.concurrent.TimeUnit value to ticks.
     *
     * @param value the time value
     * @param unit  the java TimeUnit
     * @return the equivalent ticks
     * @since 1.0.0
     */
    public static long toTicks(long value, @NotNull java.util.concurrent.TimeUnit unit) {
        return Ticks.from(value, unit);
    }
}
