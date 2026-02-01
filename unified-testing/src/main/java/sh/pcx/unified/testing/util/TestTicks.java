/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.util;

/**
 * Utility class for tick-based time conversions in tests.
 *
 * <p>Provides convenient constants and methods for working with
 * Minecraft server ticks (20 ticks = 1 second).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Advance by 5 seconds
 * server.advanceTicks(TestTicks.seconds(5));
 *
 * // Advance by 2 minutes
 * server.advanceTicks(TestTicks.minutes(2));
 *
 * // Use constants
 * server.advanceTicks(TestTicks.ONE_MINUTE);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class TestTicks {

    /** Ticks per second (20). */
    public static final long TICKS_PER_SECOND = 20;

    /** Ticks per minute (1200). */
    public static final long TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;

    /** Ticks per hour (72000). */
    public static final long TICKS_PER_HOUR = TICKS_PER_MINUTE * 60;

    /** Ticks per Minecraft day (24000). */
    public static final long TICKS_PER_DAY = 24000;

    /** One second in ticks. */
    public static final long ONE_SECOND = TICKS_PER_SECOND;

    /** One minute in ticks. */
    public static final long ONE_MINUTE = TICKS_PER_MINUTE;

    /** One hour in ticks. */
    public static final long ONE_HOUR = TICKS_PER_HOUR;

    /** One Minecraft day in ticks. */
    public static final long ONE_DAY = TICKS_PER_DAY;

    private TestTicks() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts seconds to ticks.
     *
     * @param seconds the number of seconds
     * @return the equivalent number of ticks
     */
    public static long seconds(double seconds) {
        return Math.round(seconds * TICKS_PER_SECOND);
    }

    /**
     * Converts seconds to ticks.
     *
     * @param seconds the number of seconds
     * @return the equivalent number of ticks
     */
    public static long seconds(long seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /**
     * Converts minutes to ticks.
     *
     * @param minutes the number of minutes
     * @return the equivalent number of ticks
     */
    public static long minutes(double minutes) {
        return Math.round(minutes * TICKS_PER_MINUTE);
    }

    /**
     * Converts minutes to ticks.
     *
     * @param minutes the number of minutes
     * @return the equivalent number of ticks
     */
    public static long minutes(long minutes) {
        return minutes * TICKS_PER_MINUTE;
    }

    /**
     * Converts hours to ticks.
     *
     * @param hours the number of hours
     * @return the equivalent number of ticks
     */
    public static long hours(double hours) {
        return Math.round(hours * TICKS_PER_HOUR);
    }

    /**
     * Converts hours to ticks.
     *
     * @param hours the number of hours
     * @return the equivalent number of ticks
     */
    public static long hours(long hours) {
        return hours * TICKS_PER_HOUR;
    }

    /**
     * Converts Minecraft days to ticks.
     *
     * @param days the number of Minecraft days
     * @return the equivalent number of ticks
     */
    public static long days(long days) {
        return days * TICKS_PER_DAY;
    }

    /**
     * Converts ticks to seconds.
     *
     * @param ticks the number of ticks
     * @return the equivalent number of seconds
     */
    public static double toSeconds(long ticks) {
        return (double) ticks / TICKS_PER_SECOND;
    }

    /**
     * Converts ticks to minutes.
     *
     * @param ticks the number of ticks
     * @return the equivalent number of minutes
     */
    public static double toMinutes(long ticks) {
        return (double) ticks / TICKS_PER_MINUTE;
    }

    /**
     * Converts ticks to milliseconds.
     *
     * @param ticks the number of ticks
     * @return the equivalent number of milliseconds
     */
    public static long toMillis(long ticks) {
        return ticks * 50; // 50ms per tick at 20 TPS
    }

    /**
     * Converts milliseconds to ticks.
     *
     * @param millis the number of milliseconds
     * @return the equivalent number of ticks
     */
    public static long fromMillis(long millis) {
        return millis / 50;
    }

    /**
     * Formats a tick count as a human-readable duration.
     *
     * @param ticks the number of ticks
     * @return a formatted string (e.g., "1m 30s")
     */
    public static String format(long ticks) {
        double seconds = toSeconds(ticks);

        if (seconds < 60) {
            return String.format("%.1fs", seconds);
        }

        long minutes = (long) (seconds / 60);
        seconds %= 60;

        if (minutes < 60) {
            return String.format("%dm %.0fs", minutes, seconds);
        }

        long hours = minutes / 60;
        minutes %= 60;

        return String.format("%dh %dm", hours, minutes);
    }
}
