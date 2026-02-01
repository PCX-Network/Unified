/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.util;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for converting between time units and Minecraft server ticks.
 *
 * <p>Minecraft servers run at a target of 20 ticks per second (TPS).
 * This class provides convenient methods for converting between real time
 * and tick counts.
 *
 * <h2>Tick Rate</h2>
 * <ul>
 *   <li>1 tick = 50 milliseconds (at 20 TPS)</li>
 *   <li>20 ticks = 1 second</li>
 *   <li>1200 ticks = 1 minute</li>
 *   <li>72000 ticks = 1 hour</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Convert time to ticks
 * long ticks = Ticks.from(5, TimeUnit.SECONDS);  // 100 ticks
 * long ticks = Ticks.fromMinutes(2);              // 2400 ticks
 * long ticks = Ticks.from(Duration.ofSeconds(10)); // 200 ticks
 *
 * // Convert ticks to time
 * Duration duration = Ticks.toDuration(100);      // 5 seconds
 * long millis = Ticks.toMillis(20);               // 1000 ms
 * double seconds = Ticks.toSeconds(40);           // 2.0 seconds
 *
 * // Common tick values
 * long oneSec = Ticks.SECOND;   // 20
 * long oneMin = Ticks.MINUTE;   // 1200
 * long oneHour = Ticks.HOUR;    // 72000
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class Ticks {

    /**
     * Number of ticks per second (at 20 TPS).
     */
    public static final long TICKS_PER_SECOND = 20L;

    /**
     * Duration of one tick in milliseconds.
     */
    public static final long MILLIS_PER_TICK = 50L;

    /**
     * Ticks in one second.
     */
    public static final long SECOND = TICKS_PER_SECOND;

    /**
     * Ticks in one minute.
     */
    public static final long MINUTE = SECOND * 60;

    /**
     * Ticks in one hour.
     */
    public static final long HOUR = MINUTE * 60;

    /**
     * Ticks in one Minecraft day (20 minutes).
     */
    public static final long MINECRAFT_DAY = MINUTE * 20;

    private Ticks() {
        // Utility class
    }

    // ==================== From Time to Ticks ====================

    /**
     * Converts a duration to ticks.
     *
     * @param duration the duration to convert
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long from(@NotNull Duration duration) {
        return duration.toMillis() / MILLIS_PER_TICK;
    }

    /**
     * Converts a time value to ticks.
     *
     * @param value the time value
     * @param unit  the time unit
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long from(long value, @NotNull TimeUnit unit) {
        return unit.toMillis(value) / MILLIS_PER_TICK;
    }

    /**
     * Converts milliseconds to ticks.
     *
     * @param millis the milliseconds
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long fromMillis(long millis) {
        return millis / MILLIS_PER_TICK;
    }

    /**
     * Converts seconds to ticks.
     *
     * @param seconds the seconds
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long fromSeconds(double seconds) {
        return Math.round(seconds * TICKS_PER_SECOND);
    }

    /**
     * Converts seconds to ticks.
     *
     * @param seconds the seconds
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long fromSeconds(long seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /**
     * Converts minutes to ticks.
     *
     * @param minutes the minutes
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long fromMinutes(long minutes) {
        return minutes * MINUTE;
    }

    /**
     * Converts minutes to ticks.
     *
     * @param minutes the minutes
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long fromMinutes(double minutes) {
        return Math.round(minutes * MINUTE);
    }

    /**
     * Converts hours to ticks.
     *
     * @param hours the hours
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long fromHours(long hours) {
        return hours * HOUR;
    }

    /**
     * Converts hours to ticks.
     *
     * @param hours the hours
     * @return the equivalent number of ticks
     * @since 1.0.0
     */
    public static long fromHours(double hours) {
        return Math.round(hours * HOUR);
    }

    // ==================== From Ticks to Time ====================

    /**
     * Converts ticks to a Duration.
     *
     * @param ticks the ticks
     * @return the equivalent duration
     * @since 1.0.0
     */
    @NotNull
    public static Duration toDuration(long ticks) {
        return Duration.ofMillis(ticks * MILLIS_PER_TICK);
    }

    /**
     * Converts ticks to milliseconds.
     *
     * @param ticks the ticks
     * @return the equivalent milliseconds
     * @since 1.0.0
     */
    public static long toMillis(long ticks) {
        return ticks * MILLIS_PER_TICK;
    }

    /**
     * Converts ticks to seconds.
     *
     * @param ticks the ticks
     * @return the equivalent seconds
     * @since 1.0.0
     */
    public static double toSeconds(long ticks) {
        return (double) ticks / TICKS_PER_SECOND;
    }

    /**
     * Converts ticks to whole seconds (rounded down).
     *
     * @param ticks the ticks
     * @return the equivalent whole seconds
     * @since 1.0.0
     */
    public static long toSecondsWhole(long ticks) {
        return ticks / TICKS_PER_SECOND;
    }

    /**
     * Converts ticks to minutes.
     *
     * @param ticks the ticks
     * @return the equivalent minutes
     * @since 1.0.0
     */
    public static double toMinutes(long ticks) {
        return (double) ticks / MINUTE;
    }

    /**
     * Converts ticks to whole minutes (rounded down).
     *
     * @param ticks the ticks
     * @return the equivalent whole minutes
     * @since 1.0.0
     */
    public static long toMinutesWhole(long ticks) {
        return ticks / MINUTE;
    }

    /**
     * Converts ticks to hours.
     *
     * @param ticks the ticks
     * @return the equivalent hours
     * @since 1.0.0
     */
    public static double toHours(long ticks) {
        return (double) ticks / HOUR;
    }

    /**
     * Converts ticks to the specified time unit.
     *
     * @param ticks the ticks
     * @param unit  the desired time unit
     * @return the equivalent value in the specified unit
     * @since 1.0.0
     */
    public static long to(long ticks, @NotNull TimeUnit unit) {
        return unit.convert(ticks * MILLIS_PER_TICK, TimeUnit.MILLISECONDS);
    }

    // ==================== Formatting ====================

    /**
     * Formats ticks as a human-readable duration string.
     *
     * @param ticks the ticks
     * @return a formatted string (e.g., "2m 30s", "1h 5m")
     * @since 1.0.0
     */
    @NotNull
    public static String format(long ticks) {
        if (ticks < SECOND) {
            return ticks + " tick" + (ticks == 1 ? "" : "s");
        }

        long hours = ticks / HOUR;
        long minutes = (ticks % HOUR) / MINUTE;
        long seconds = (ticks % MINUTE) / SECOND;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Formats ticks as a precise duration string including ticks.
     *
     * @param ticks the ticks
     * @return a formatted string (e.g., "2m 30s 10t")
     * @since 1.0.0
     */
    @NotNull
    public static String formatPrecise(long ticks) {
        long hours = ticks / HOUR;
        long minutes = (ticks % HOUR) / MINUTE;
        long seconds = (ticks % MINUTE) / SECOND;
        long remainingTicks = ticks % SECOND;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0) {
            sb.append(seconds).append("s ");
        }
        if (remainingTicks > 0 || sb.length() == 0) {
            sb.append(remainingTicks).append("t");
        }

        return sb.toString().trim();
    }

    // ==================== Validation ====================

    /**
     * Ensures ticks is non-negative.
     *
     * @param ticks the ticks value
     * @return the validated ticks value
     * @throws IllegalArgumentException if ticks is negative
     * @since 1.0.0
     */
    public static long requireNonNegative(long ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Ticks cannot be negative: " + ticks);
        }
        return ticks;
    }

    /**
     * Ensures ticks is positive.
     *
     * @param ticks the ticks value
     * @return the validated ticks value
     * @throws IllegalArgumentException if ticks is not positive
     * @since 1.0.0
     */
    public static long requirePositive(long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Ticks must be positive: " + ticks);
        }
        return ticks;
    }

    /**
     * Clamps ticks to a range.
     *
     * @param ticks the ticks value
     * @param min   the minimum value
     * @param max   the maximum value
     * @return the clamped value
     * @since 1.0.0
     */
    public static long clamp(long ticks, long min, long max) {
        return Math.max(min, Math.min(max, ticks));
    }
}
