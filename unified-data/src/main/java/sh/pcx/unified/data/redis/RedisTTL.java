/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for managing Redis key TTL (Time-To-Live) values.
 *
 * <p>This class provides common TTL durations and helper methods for
 * calculating expiration times in Redis operations.
 *
 * <h2>Common TTL Values</h2>
 * <pre>{@code
 * // Use predefined TTL constants
 * redis.setex("session", data, RedisTTL.SESSION);
 * redis.setex("cache", data, RedisTTL.CACHE_SHORT);
 *
 * // Or create custom TTL
 * redis.setex("key", value, RedisTTL.minutes(30));
 * redis.setex("key", value, RedisTTL.hours(2));
 * redis.setex("key", value, RedisTTL.days(7));
 * }</pre>
 *
 * <h2>Expiration Calculation</h2>
 * <pre>{@code
 * // Get seconds for Redis commands
 * long seconds = RedisTTL.toSeconds(Duration.ofMinutes(30));
 *
 * // Get milliseconds for Redis commands
 * long millis = RedisTTL.toMillis(Duration.ofMinutes(30));
 *
 * // Calculate absolute expiration time
 * Instant expireAt = RedisTTL.expireAt(Duration.ofHours(1));
 * }</pre>
 *
 * <h2>TTL Strategies</h2>
 * <pre>{@code
 * // Sliding expiration: reset TTL on each access
 * String value = redis.get("session").orElse(null);
 * if (value != null) {
 *     redis.expire("session", RedisTTL.SESSION);
 * }
 *
 * // Check remaining TTL
 * Optional<Duration> remaining = redis.ttl("key");
 * if (remaining.isPresent() && remaining.get().toMinutes() < 5) {
 *     // Refresh key before it expires
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisService#expire(String, Duration)
 * @see RedisService#ttl(String)
 */
public final class RedisTTL {

    // ========== Common TTL Constants ==========

    /**
     * Short cache TTL: 5 minutes.
     *
     * <p>Suitable for frequently changing data like real-time stats.
     */
    public static final Duration CACHE_SHORT = Duration.ofMinutes(5);

    /**
     * Medium cache TTL: 30 minutes.
     *
     * <p>Suitable for moderately changing data.
     */
    public static final Duration CACHE_MEDIUM = Duration.ofMinutes(30);

    /**
     * Long cache TTL: 1 hour.
     *
     * <p>Suitable for relatively stable data.
     */
    public static final Duration CACHE_LONG = Duration.ofHours(1);

    /**
     * Extended cache TTL: 6 hours.
     *
     * <p>Suitable for rarely changing data.
     */
    public static final Duration CACHE_EXTENDED = Duration.ofHours(6);

    /**
     * Daily cache TTL: 24 hours.
     *
     * <p>Suitable for daily aggregations or slow-changing data.
     */
    public static final Duration CACHE_DAILY = Duration.ofDays(1);

    /**
     * Session TTL: 30 minutes.
     *
     * <p>Standard session expiration for inactive users.
     */
    public static final Duration SESSION = Duration.ofMinutes(30);

    /**
     * Long session TTL: 24 hours.
     *
     * <p>Extended session for "remember me" functionality.
     */
    public static final Duration SESSION_LONG = Duration.ofDays(1);

    /**
     * Lock TTL: 30 seconds.
     *
     * <p>Default TTL for distributed locks to prevent deadlocks.
     */
    public static final Duration LOCK = Duration.ofSeconds(30);

    /**
     * Extended lock TTL: 5 minutes.
     *
     * <p>For longer-running operations that need locks.
     */
    public static final Duration LOCK_EXTENDED = Duration.ofMinutes(5);

    /**
     * Rate limit window: 1 minute.
     *
     * <p>Common window for rate limiting checks.
     */
    public static final Duration RATE_LIMIT_MINUTE = Duration.ofMinutes(1);

    /**
     * Hourly rate limit window: 1 hour.
     */
    public static final Duration RATE_LIMIT_HOUR = Duration.ofHours(1);

    /**
     * Temporary data TTL: 1 minute.
     *
     * <p>For very short-lived temporary data.
     */
    public static final Duration TEMP_SHORT = Duration.ofMinutes(1);

    /**
     * Temporary data TTL: 10 minutes.
     */
    public static final Duration TEMP_MEDIUM = Duration.ofMinutes(10);

    /**
     * No expiration (persistent key).
     *
     * <p>Represented as Duration.ZERO for API purposes.
     * When used, the key will not have a TTL set.
     */
    public static final Duration PERSISTENT = Duration.ZERO;

    private RedisTTL() {
        // Utility class - no instantiation
    }

    // ========== Factory Methods ==========

    /**
     * Creates a TTL of the specified number of seconds.
     *
     * @param seconds the number of seconds
     * @return the duration
     * @throws IllegalArgumentException if seconds is negative
     * @since 1.0.0
     */
    @NotNull
    public static Duration seconds(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds cannot be negative");
        }
        return Duration.ofSeconds(seconds);
    }

    /**
     * Creates a TTL of the specified number of minutes.
     *
     * @param minutes the number of minutes
     * @return the duration
     * @throws IllegalArgumentException if minutes is negative
     * @since 1.0.0
     */
    @NotNull
    public static Duration minutes(long minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("minutes cannot be negative");
        }
        return Duration.ofMinutes(minutes);
    }

    /**
     * Creates a TTL of the specified number of hours.
     *
     * @param hours the number of hours
     * @return the duration
     * @throws IllegalArgumentException if hours is negative
     * @since 1.0.0
     */
    @NotNull
    public static Duration hours(long hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("hours cannot be negative");
        }
        return Duration.ofHours(hours);
    }

    /**
     * Creates a TTL of the specified number of days.
     *
     * @param days the number of days
     * @return the duration
     * @throws IllegalArgumentException if days is negative
     * @since 1.0.0
     */
    @NotNull
    public static Duration days(long days) {
        if (days < 0) {
            throw new IllegalArgumentException("days cannot be negative");
        }
        return Duration.ofDays(days);
    }

    /**
     * Creates a TTL of the specified number of weeks.
     *
     * @param weeks the number of weeks
     * @return the duration
     * @throws IllegalArgumentException if weeks is negative
     * @since 1.0.0
     */
    @NotNull
    public static Duration weeks(long weeks) {
        if (weeks < 0) {
            throw new IllegalArgumentException("weeks cannot be negative");
        }
        return Duration.ofDays(weeks * 7);
    }

    /**
     * Creates a TTL from a TimeUnit value.
     *
     * @param duration the duration value
     * @param unit     the time unit
     * @return the duration
     * @throws IllegalArgumentException if duration is negative
     * @since 1.0.0
     */
    @NotNull
    public static Duration of(long duration, @NotNull TimeUnit unit) {
        Objects.requireNonNull(unit, "unit cannot be null");
        if (duration < 0) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
        return Duration.of(duration, toChronoUnit(unit));
    }

    // ========== Conversion Methods ==========

    /**
     * Converts a Duration to seconds.
     *
     * @param duration the duration
     * @return the number of seconds
     * @since 1.0.0
     */
    public static long toSeconds(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        return duration.toSeconds();
    }

    /**
     * Converts a Duration to milliseconds.
     *
     * @param duration the duration
     * @return the number of milliseconds
     * @since 1.0.0
     */
    public static long toMillis(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        return duration.toMillis();
    }

    /**
     * Converts a Duration to a human-readable string.
     *
     * @param duration the duration
     * @return a formatted string (e.g., "5m", "2h 30m", "1d 5h")
     * @since 1.0.0
     */
    @NotNull
    public static String format(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");

        if (duration.isZero()) {
            return "0s";
        }

        StringBuilder sb = new StringBuilder();
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.toSeconds() % 60;

        if (days > 0) {
            sb.append(days).append("d");
        }
        if (hours > 0) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(minutes).append("m");
        }
        if (seconds > 0 && days == 0) { // Skip seconds for long durations
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(seconds).append("s");
        }

        return sb.toString();
    }

    /**
     * Parses a duration string (e.g., "5m", "2h", "1d").
     *
     * <p>Supported units:
     * <ul>
     *   <li>{@code s} - seconds</li>
     *   <li>{@code m} - minutes</li>
     *   <li>{@code h} - hours</li>
     *   <li>{@code d} - days</li>
     *   <li>{@code w} - weeks</li>
     * </ul>
     *
     * @param text the duration string
     * @return the parsed duration
     * @throws IllegalArgumentException if the format is invalid
     * @since 1.0.0
     */
    @NotNull
    public static Duration parse(@NotNull String text) {
        Objects.requireNonNull(text, "text cannot be null");

        String trimmed = text.strip().toLowerCase();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be empty");
        }

        // Extract the numeric part and unit
        int unitIndex = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                unitIndex = i;
                break;
            }
        }

        if (unitIndex == -1 || unitIndex == 0) {
            throw new IllegalArgumentException("Invalid duration format: " + text);
        }

        long value = Long.parseLong(trimmed.substring(0, unitIndex));
        String unit = trimmed.substring(unitIndex);

        return switch (unit) {
            case "s", "sec", "second", "seconds" -> seconds(value);
            case "m", "min", "minute", "minutes" -> minutes(value);
            case "h", "hr", "hour", "hours" -> hours(value);
            case "d", "day", "days" -> days(value);
            case "w", "week", "weeks" -> weeks(value);
            default -> throw new IllegalArgumentException("Unknown time unit: " + unit);
        };
    }

    // ========== Expiration Calculation ==========

    /**
     * Calculates the absolute expiration time.
     *
     * @param ttl the TTL duration
     * @return the instant when the key will expire
     * @since 1.0.0
     */
    @NotNull
    public static Instant expireAt(@NotNull Duration ttl) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        return Instant.now().plus(ttl);
    }

    /**
     * Calculates the remaining TTL from an expiration instant.
     *
     * @param expireAt the expiration instant
     * @return the remaining duration, or ZERO if already expired
     * @since 1.0.0
     */
    @NotNull
    public static Duration remainingTtl(@NotNull Instant expireAt) {
        Objects.requireNonNull(expireAt, "expireAt cannot be null");
        Duration remaining = Duration.between(Instant.now(), expireAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Checks if a TTL duration represents a persistent (non-expiring) key.
     *
     * @param ttl the TTL to check
     * @return true if the TTL indicates no expiration
     * @since 1.0.0
     */
    public static boolean isPersistent(@NotNull Duration ttl) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        return ttl.isZero() || ttl.isNegative();
    }

    /**
     * Returns a TTL with a minimum value.
     *
     * <p>If the provided TTL is less than the minimum, returns the minimum.
     *
     * @param ttl     the requested TTL
     * @param minimum the minimum allowed TTL
     * @return the effective TTL
     * @since 1.0.0
     */
    @NotNull
    public static Duration atLeast(@NotNull Duration ttl, @NotNull Duration minimum) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Objects.requireNonNull(minimum, "minimum cannot be null");
        return ttl.compareTo(minimum) < 0 ? minimum : ttl;
    }

    /**
     * Returns a TTL with a maximum value.
     *
     * <p>If the provided TTL is greater than the maximum, returns the maximum.
     *
     * @param ttl     the requested TTL
     * @param maximum the maximum allowed TTL
     * @return the effective TTL
     * @since 1.0.0
     */
    @NotNull
    public static Duration atMost(@NotNull Duration ttl, @NotNull Duration maximum) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Objects.requireNonNull(maximum, "maximum cannot be null");
        return ttl.compareTo(maximum) > 0 ? maximum : ttl;
    }

    /**
     * Clamps a TTL between minimum and maximum values.
     *
     * @param ttl     the requested TTL
     * @param minimum the minimum allowed TTL
     * @param maximum the maximum allowed TTL
     * @return the clamped TTL
     * @since 1.0.0
     */
    @NotNull
    public static Duration clamp(@NotNull Duration ttl, @NotNull Duration minimum, @NotNull Duration maximum) {
        return atLeast(atMost(ttl, maximum), minimum);
    }

    private static ChronoUnit toChronoUnit(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> ChronoUnit.NANOS;
            case MICROSECONDS -> ChronoUnit.MICROS;
            case MILLISECONDS -> ChronoUnit.MILLIS;
            case SECONDS -> ChronoUnit.SECONDS;
            case MINUTES -> ChronoUnit.MINUTES;
            case HOURS -> ChronoUnit.HOURS;
            case DAYS -> ChronoUnit.DAYS;
        };
    }
}
