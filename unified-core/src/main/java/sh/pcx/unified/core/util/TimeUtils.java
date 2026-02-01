/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting time durations.
 *
 * <p>Provides human-readable duration parsing and formatting with support for
 * multiple time units including weeks, days, hours, minutes, seconds, and
 * milliseconds.
 *
 * <h2>Parsing Examples</h2>
 * <pre>{@code
 * // Parse various duration formats
 * Duration duration1 = TimeUtils.parse("1h30m");      // 1 hour 30 minutes
 * Duration duration2 = TimeUtils.parse("2d12h");      // 2 days 12 hours
 * Duration duration3 = TimeUtils.parse("1w2d3h4m5s"); // Complex duration
 * Duration duration4 = TimeUtils.parse("500ms");      // 500 milliseconds
 *
 * // Parse with unit names
 * Duration duration5 = TimeUtils.parse("2 hours");
 * Duration duration6 = TimeUtils.parse("1 day 6 hours");
 * }</pre>
 *
 * <h2>Formatting Examples</h2>
 * <pre>{@code
 * // Format durations
 * String formatted = TimeUtils.format(Duration.ofHours(2).plusMinutes(30));
 * // Result: "2 hours, 30 minutes"
 *
 * // Compact format
 * String compact = TimeUtils.formatCompact(Duration.ofHours(2).plusMinutes(30));
 * // Result: "2h30m"
 *
 * // Relative time
 * String relative = TimeUtils.relative(Instant.now().minus(5, ChronoUnit.MINUTES));
 * // Result: "5 minutes ago"
 * }</pre>
 *
 * <h2>Supported Units</h2>
 * <ul>
 *   <li>w, week, weeks - weeks</li>
 *   <li>d, day, days - days</li>
 *   <li>h, hour, hours, hr, hrs - hours</li>
 *   <li>m, min, mins, minute, minutes - minutes</li>
 *   <li>s, sec, secs, second, seconds - seconds</li>
 *   <li>ms, milli, millis, millisecond, milliseconds - milliseconds</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe and can be called from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class TimeUtils {

    /**
     * Pattern for parsing duration strings.
     * Matches formats like "1h30m", "2 hours 30 minutes", "1w2d3h4m5s", etc.
     */
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(\\d+)\\s*(w(?:eeks?)?|d(?:ays?)?|h(?:(?:ou)?rs?)?|m(?:in(?:ute)?s?)?|s(?:ec(?:ond)?s?)?|ms|milli(?:second)?s?)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern for parsing simple numeric strings (assumed seconds).
     */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

    /**
     * Ordered map of time units for formatting (largest to smallest).
     */
    private static final Map<ChronoUnit, String[]> UNITS;

    static {
        UNITS = new LinkedHashMap<>();
        UNITS.put(ChronoUnit.WEEKS, new String[]{"week", "weeks", "w"});
        UNITS.put(ChronoUnit.DAYS, new String[]{"day", "days", "d"});
        UNITS.put(ChronoUnit.HOURS, new String[]{"hour", "hours", "h"});
        UNITS.put(ChronoUnit.MINUTES, new String[]{"minute", "minutes", "m"});
        UNITS.put(ChronoUnit.SECONDS, new String[]{"second", "seconds", "s"});
    }

    private TimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses a duration string into a {@link Duration}.
     *
     * <p>Supports formats like:
     * <ul>
     *   <li>"1h30m" - compact format</li>
     *   <li>"2 hours 30 minutes" - verbose format</li>
     *   <li>"1w2d3h4m5s" - combined format</li>
     *   <li>"30" - plain number (interpreted as seconds)</li>
     *   <li>"500ms" - milliseconds</li>
     * </ul>
     *
     * @param input the duration string to parse
     * @return the parsed duration
     * @throws IllegalArgumentException if the input cannot be parsed
     * @throws NullPointerException if input is null
     * @since 1.0.0
     */
    @NotNull
    public static Duration parse(@NotNull String input) {
        Objects.requireNonNull(input, "input cannot be null");

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be empty");
        }

        // Handle plain numeric input (assumed seconds)
        if (NUMERIC_PATTERN.matcher(trimmed).matches()) {
            return Duration.ofSeconds(Long.parseLong(trimmed));
        }

        Duration result = Duration.ZERO;
        Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            result = result.plus(toDuration(value, unit));
        }

        if (!found) {
            throw new IllegalArgumentException(
                    "Cannot parse duration: '" + input + "'. " +
                    "Expected format like '1h30m', '2 hours 30 minutes', or '1w2d3h4m5s'"
            );
        }

        return result;
    }

    /**
     * Attempts to parse a duration string, returning null if parsing fails.
     *
     * @param input the duration string to parse
     * @return the parsed duration, or null if parsing fails
     * @since 1.0.0
     */
    @Nullable
    public static Duration tryParse(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return parse(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Formats a duration into a human-readable string.
     *
     * <p>Example: Duration of 2 hours and 30 minutes formats as "2 hours, 30 minutes".
     *
     * @param duration the duration to format
     * @return the formatted duration string
     * @throws NullPointerException if duration is null
     * @since 1.0.0
     */
    @NotNull
    public static String format(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");

        if (duration.isZero() || duration.isNegative()) {
            return "0 seconds";
        }

        StringBuilder result = new StringBuilder();
        long totalSeconds = duration.getSeconds();
        long millis = duration.toMillisPart();

        long weeks = totalSeconds / (7 * 24 * 60 * 60);
        totalSeconds %= 7 * 24 * 60 * 60;

        long days = totalSeconds / (24 * 60 * 60);
        totalSeconds %= 24 * 60 * 60;

        long hours = totalSeconds / (60 * 60);
        totalSeconds %= 60 * 60;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        appendUnit(result, weeks, "week", "weeks");
        appendUnit(result, days, "day", "days");
        appendUnit(result, hours, "hour", "hours");
        appendUnit(result, minutes, "minute", "minutes");
        appendUnit(result, seconds, "second", "seconds");

        if (millis > 0 && result.isEmpty()) {
            appendUnit(result, millis, "millisecond", "milliseconds");
        }

        return result.isEmpty() ? "0 seconds" : result.toString();
    }

    /**
     * Formats a duration into a compact string.
     *
     * <p>Example: Duration of 2 hours and 30 minutes formats as "2h30m".
     *
     * @param duration the duration to format
     * @return the compact formatted duration string
     * @throws NullPointerException if duration is null
     * @since 1.0.0
     */
    @NotNull
    public static String formatCompact(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");

        if (duration.isZero() || duration.isNegative()) {
            return "0s";
        }

        StringBuilder result = new StringBuilder();
        long totalSeconds = duration.getSeconds();
        long millis = duration.toMillisPart();

        long weeks = totalSeconds / (7 * 24 * 60 * 60);
        totalSeconds %= 7 * 24 * 60 * 60;

        long days = totalSeconds / (24 * 60 * 60);
        totalSeconds %= 24 * 60 * 60;

        long hours = totalSeconds / (60 * 60);
        totalSeconds %= 60 * 60;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (weeks > 0) result.append(weeks).append("w");
        if (days > 0) result.append(days).append("d");
        if (hours > 0) result.append(hours).append("h");
        if (minutes > 0) result.append(minutes).append("m");
        if (seconds > 0) result.append(seconds).append("s");
        if (millis > 0 && result.isEmpty()) result.append(millis).append("ms");

        return result.isEmpty() ? "0s" : result.toString();
    }

    /**
     * Formats a duration into a short countdown format (HH:MM:SS).
     *
     * <p>Useful for displaying remaining time in GUIs or chat.
     *
     * @param duration the duration to format
     * @return the countdown formatted string
     * @throws NullPointerException if duration is null
     * @since 1.0.0
     */
    @NotNull
    public static String formatCountdown(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");

        if (duration.isNegative()) {
            return "00:00:00";
        }

        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("00:%02d", seconds);
        }
    }

    /**
     * Formats an instant as a relative time string.
     *
     * <p>Examples:
     * <ul>
     *   <li>"just now" - less than 5 seconds ago</li>
     *   <li>"30 seconds ago"</li>
     *   <li>"5 minutes ago"</li>
     *   <li>"2 hours ago"</li>
     *   <li>"in 5 minutes" - for future times</li>
     * </ul>
     *
     * @param instant the instant to format relative to now
     * @return the relative time string
     * @throws NullPointerException if instant is null
     * @since 1.0.0
     */
    @NotNull
    public static String relative(@NotNull Instant instant) {
        Objects.requireNonNull(instant, "instant cannot be null");
        return relative(instant, Instant.now());
    }

    /**
     * Formats an instant as a relative time string compared to a reference time.
     *
     * @param instant   the instant to format
     * @param reference the reference instant to compare against
     * @return the relative time string
     * @throws NullPointerException if either parameter is null
     * @since 1.0.0
     */
    @NotNull
    public static String relative(@NotNull Instant instant, @NotNull Instant reference) {
        Objects.requireNonNull(instant, "instant cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");

        Duration duration = Duration.between(instant, reference);
        boolean past = !duration.isNegative();

        if (duration.isNegative()) {
            duration = duration.negated();
        }

        long seconds = duration.getSeconds();

        if (seconds < 5) {
            return "just now";
        }

        String timeStr;
        if (seconds < 60) {
            timeStr = seconds + " second" + (seconds == 1 ? "" : "s");
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            timeStr = minutes + " minute" + (minutes == 1 ? "" : "s");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            timeStr = hours + " hour" + (hours == 1 ? "" : "s");
        } else if (seconds < 604800) {
            long days = seconds / 86400;
            timeStr = days + " day" + (days == 1 ? "" : "s");
        } else if (seconds < 2592000) { // ~30 days
            long weeks = seconds / 604800;
            timeStr = weeks + " week" + (weeks == 1 ? "" : "s");
        } else if (seconds < 31536000) { // ~365 days
            long months = seconds / 2592000;
            timeStr = months + " month" + (months == 1 ? "" : "s");
        } else {
            long years = seconds / 31536000;
            timeStr = years + " year" + (years == 1 ? "" : "s");
        }

        return past ? timeStr + " ago" : "in " + timeStr;
    }

    /**
     * Converts a duration to ticks (20 ticks per second).
     *
     * @param duration the duration to convert
     * @return the number of ticks
     * @throws NullPointerException if duration is null
     * @since 1.0.0
     */
    public static long toTicks(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        return duration.toMillis() / 50;
    }

    /**
     * Creates a duration from ticks (20 ticks per second).
     *
     * @param ticks the number of ticks
     * @return the duration
     * @since 1.0.0
     */
    @NotNull
    public static Duration fromTicks(long ticks) {
        return Duration.ofMillis(ticks * 50);
    }

    /**
     * Checks if a duration has elapsed since the given instant.
     *
     * @param since    the starting instant
     * @param duration the duration to check
     * @return true if the duration has elapsed
     * @throws NullPointerException if either parameter is null
     * @since 1.0.0
     */
    public static boolean hasElapsed(@NotNull Instant since, @NotNull Duration duration) {
        Objects.requireNonNull(since, "since cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");
        return Instant.now().isAfter(since.plus(duration));
    }

    /**
     * Gets the remaining duration until the specified end time.
     *
     * @param end the end instant
     * @return the remaining duration, or {@link Duration#ZERO} if already passed
     * @throws NullPointerException if end is null
     * @since 1.0.0
     */
    @NotNull
    public static Duration remaining(@NotNull Instant end) {
        Objects.requireNonNull(end, "end cannot be null");
        Duration duration = Duration.between(Instant.now(), end);
        return duration.isNegative() ? Duration.ZERO : duration;
    }

    /**
     * Converts a value and unit string to a duration.
     */
    private static Duration toDuration(long value, String unit) {
        return switch (unit.charAt(0)) {
            case 'w' -> Duration.ofDays(value * 7);
            case 'd' -> Duration.ofDays(value);
            case 'h' -> Duration.ofHours(value);
            case 'm' -> {
                if (unit.startsWith("ms") || unit.startsWith("milli")) {
                    yield Duration.ofMillis(value);
                }
                yield Duration.ofMinutes(value);
            }
            case 's' -> Duration.ofSeconds(value);
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };
    }

    /**
     * Appends a time unit to the result builder if the value is non-zero.
     */
    private static void appendUnit(StringBuilder builder, long value, String singular, String plural) {
        if (value <= 0) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(value).append(" ").append(value == 1 ? singular : plural);
    }
}
