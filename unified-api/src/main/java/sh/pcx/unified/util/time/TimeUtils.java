package sh.pcx.unified.util.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for time-related operations including duration parsing,
 * formatting, and relative time calculations.
 *
 * <p>This class provides comprehensive time utilities commonly needed in
 * plugin development, such as parsing human-readable duration strings,
 * formatting durations for display, and calculating relative times.</p>
 *
 * <h2>Duration Parsing Examples:</h2>
 * <pre>{@code
 * // Parse various duration formats
 * Duration d1 = TimeUtils.parseDuration("1h30m");      // 1 hour 30 minutes
 * Duration d2 = TimeUtils.parseDuration("1d2h3m4s");   // 1 day, 2 hours, 3 minutes, 4 seconds
 * Duration d3 = TimeUtils.parseDuration("1.5h");       // 1 hour 30 minutes (decimal)
 * Duration d4 = TimeUtils.parseDuration("90m");        // 90 minutes
 * Duration d5 = TimeUtils.parseDuration("2w");         // 2 weeks
 *
 * // Convert to milliseconds for scheduling
 * long millis = TimeUtils.parseToMillis("30s");
 * }</pre>
 *
 * <h2>Duration Formatting Examples:</h2>
 * <pre>{@code
 * // Format durations for display
 * String formatted = TimeUtils.formatDuration(Duration.ofHours(2).plusMinutes(30));
 * // Result: "2h 30m"
 *
 * String compact = TimeUtils.formatDurationCompact(Duration.ofSeconds(3661));
 * // Result: "1:01:01"
 *
 * String verbose = TimeUtils.formatDurationVerbose(Duration.ofDays(1).plusHours(5));
 * // Result: "1 day, 5 hours"
 * }</pre>
 *
 * <h2>Relative Time Examples:</h2>
 * <pre>{@code
 * Instant pastTime = Instant.now().minus(2, ChronoUnit.HOURS);
 * String relative = TimeUtils.getRelativeTime(pastTime);
 * // Result: "2 hours ago"
 *
 * Instant futureTime = Instant.now().plus(30, ChronoUnit.MINUTES);
 * String futureRelative = TimeUtils.getRelativeTime(futureTime);
 * // Result: "in 30 minutes"
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public final class TimeUtils {

    /**
     * Pattern for parsing duration strings.
     * Matches formats like "1d2h3m4s", "1.5h", "30m", etc.
     */
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?:(\\d+(?:\\.\\d+)?)\\s*w(?:eeks?)?)?\\s*" +
            "(?:(\\d+(?:\\.\\d+)?)\\s*d(?:ays?)?)?\\s*" +
            "(?:(\\d+(?:\\.\\d+)?)\\s*h(?:ours?)?)?\\s*" +
            "(?:(\\d+(?:\\.\\d+)?)\\s*m(?:in(?:utes?)?)?)?\\s*" +
            "(?:(\\d+(?:\\.\\d+)?)\\s*s(?:ec(?:onds?)?)?)?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Simple pattern for single unit durations like "30s", "5m", "2h".
     */
    private static final Pattern SIMPLE_DURATION_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*([wdhms])",
            Pattern.CASE_INSENSITIVE
    );

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    private static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
    private static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
    private static final long MILLIS_PER_WEEK = 7 * MILLIS_PER_DAY;

    private TimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== Duration Parsing ====================

    /**
     * Parses a human-readable duration string into a {@link Duration}.
     *
     * <p>Supported formats:</p>
     * <ul>
     *   <li>{@code w} - weeks</li>
     *   <li>{@code d} - days</li>
     *   <li>{@code h} - hours</li>
     *   <li>{@code m} - minutes</li>
     *   <li>{@code s} - seconds</li>
     * </ul>
     *
     * <p>Decimal values are supported (e.g., "1.5h" = 1 hour 30 minutes).</p>
     *
     * @param input the duration string to parse (e.g., "1h30m", "1.5d", "2w1d")
     * @return the parsed Duration
     * @throws IllegalArgumentException if the input is null, empty, or invalid
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * Duration d = TimeUtils.parseDuration("1d2h30m");
     * System.out.println(d.toHours()); // 26
     * }</pre>
     */
    public static Duration parseDuration(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be null or empty");
        }

        String trimmed = input.trim().toLowerCase();
        long totalMillis = 0;
        boolean matched = false;

        Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            // Weeks
            if (matcher.group(1) != null) {
                totalMillis += (long) (Double.parseDouble(matcher.group(1)) * MILLIS_PER_WEEK);
                matched = true;
            }
            // Days
            if (matcher.group(2) != null) {
                totalMillis += (long) (Double.parseDouble(matcher.group(2)) * MILLIS_PER_DAY);
                matched = true;
            }
            // Hours
            if (matcher.group(3) != null) {
                totalMillis += (long) (Double.parseDouble(matcher.group(3)) * MILLIS_PER_HOUR);
                matched = true;
            }
            // Minutes
            if (matcher.group(4) != null) {
                totalMillis += (long) (Double.parseDouble(matcher.group(4)) * MILLIS_PER_MINUTE);
                matched = true;
            }
            // Seconds
            if (matcher.group(5) != null) {
                totalMillis += (long) (Double.parseDouble(matcher.group(5)) * MILLIS_PER_SECOND);
                matched = true;
            }
        }

        if (!matched) {
            // Try parsing as plain number (assume seconds)
            try {
                double seconds = Double.parseDouble(trimmed);
                totalMillis = (long) (seconds * MILLIS_PER_SECOND);
                matched = true;
            } catch (NumberFormatException ignored) {
                // Not a plain number
            }
        }

        if (!matched) {
            throw new IllegalArgumentException("Invalid duration format: " + input);
        }

        return Duration.ofMillis(totalMillis);
    }

    /**
     * Parses a duration string and returns the value in milliseconds.
     *
     * @param input the duration string to parse
     * @return the duration in milliseconds
     * @throws IllegalArgumentException if the input is invalid
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * long millis = TimeUtils.parseToMillis("30s");
     * // millis = 30000
     * }</pre>
     */
    public static long parseToMillis(String input) {
        return parseDuration(input).toMillis();
    }

    /**
     * Parses a duration string and returns the value in the specified time unit.
     *
     * @param input the duration string to parse
     * @param unit the time unit to convert to
     * @return the duration in the specified unit
     * @throws IllegalArgumentException if the input is invalid
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * long seconds = TimeUtils.parseTo("5m", TimeUnit.SECONDS);
     * // seconds = 300
     * }</pre>
     */
    public static long parseTo(String input, TimeUnit unit) {
        return unit.convert(parseToMillis(input), TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to parse a duration string, returning a default value if parsing fails.
     *
     * @param input the duration string to parse
     * @param defaultValue the default duration if parsing fails
     * @return the parsed Duration or the default value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * Duration d = TimeUtils.parseDurationOrDefault("invalid", Duration.ofMinutes(5));
     * // Returns 5 minute duration
     * }</pre>
     */
    public static Duration parseDurationOrDefault(String input, Duration defaultValue) {
        try {
            return parseDuration(input);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    // ==================== Duration Formatting ====================

    /**
     * Formats a duration into a human-readable string.
     *
     * <p>Output format: "Xd Xh Xm Xs" (only non-zero components included)</p>
     *
     * @param duration the duration to format
     * @return the formatted duration string
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * String formatted = TimeUtils.formatDuration(Duration.ofHours(25).plusMinutes(30));
     * // Result: "1d 1h 30m"
     * }</pre>
     */
    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "0s";
        }

        if (duration.isZero()) {
            return "0s";
        }

        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
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
     * Formats a duration in compact HH:MM:SS format.
     *
     * @param duration the duration to format
     * @return the formatted duration string
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * String compact = TimeUtils.formatDurationCompact(Duration.ofSeconds(3661));
     * // Result: "1:01:01"
     * }</pre>
     */
    public static String formatDurationCompact(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "0:00";
        }

        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats a duration in verbose, human-friendly format.
     *
     * @param duration the duration to format
     * @return the formatted duration string
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * String verbose = TimeUtils.formatDurationVerbose(Duration.ofDays(2).plusHours(5));
     * // Result: "2 days, 5 hours"
     * }</pre>
     */
    public static String formatDurationVerbose(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "0 seconds";
        }

        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     *
     * @param millis the duration in milliseconds
     * @return the formatted duration string
     */
    public static String formatMillis(long millis) {
        return formatDuration(Duration.ofMillis(millis));
    }

    // ==================== Relative Time ====================

    /**
     * Returns a relative time string (e.g., "2 hours ago", "in 5 minutes").
     *
     * @param instant the instant to compare against the current time
     * @return a human-readable relative time string
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
     * String relative = TimeUtils.getRelativeTime(past);
     * // Result: "2 hours ago"
     *
     * Instant future = Instant.now().plus(30, ChronoUnit.MINUTES);
     * String futureRelative = TimeUtils.getRelativeTime(future);
     * // Result: "in 30 minutes"
     * }</pre>
     */
    public static String getRelativeTime(Instant instant) {
        return getRelativeTime(instant, Instant.now());
    }

    /**
     * Returns a relative time string comparing two instants.
     *
     * @param instant the instant to describe
     * @param reference the reference instant to compare against
     * @return a human-readable relative time string
     */
    public static String getRelativeTime(Instant instant, Instant reference) {
        if (instant == null || reference == null) {
            return "unknown";
        }

        long diffMillis = reference.toEpochMilli() - instant.toEpochMilli();
        boolean isPast = diffMillis > 0;
        long absDiff = Math.abs(diffMillis);

        String timeString;
        if (absDiff < MILLIS_PER_SECOND) {
            timeString = "just now";
            return timeString;
        } else if (absDiff < MILLIS_PER_MINUTE) {
            long seconds = absDiff / MILLIS_PER_SECOND;
            timeString = seconds + (seconds == 1 ? " second" : " seconds");
        } else if (absDiff < MILLIS_PER_HOUR) {
            long minutes = absDiff / MILLIS_PER_MINUTE;
            timeString = minutes + (minutes == 1 ? " minute" : " minutes");
        } else if (absDiff < MILLIS_PER_DAY) {
            long hours = absDiff / MILLIS_PER_HOUR;
            timeString = hours + (hours == 1 ? " hour" : " hours");
        } else if (absDiff < MILLIS_PER_WEEK) {
            long days = absDiff / MILLIS_PER_DAY;
            timeString = days + (days == 1 ? " day" : " days");
        } else {
            long weeks = absDiff / MILLIS_PER_WEEK;
            timeString = weeks + (weeks == 1 ? " week" : " weeks");
        }

        return isPast ? timeString + " ago" : "in " + timeString;
    }

    /**
     * Returns a relative time string from a timestamp in milliseconds.
     *
     * @param timestampMillis the timestamp in milliseconds since epoch
     * @return a human-readable relative time string
     */
    public static String getRelativeTime(long timestampMillis) {
        return getRelativeTime(Instant.ofEpochMilli(timestampMillis));
    }

    // ==================== Time Conversion ====================

    /**
     * Converts ticks to milliseconds (20 ticks = 1 second).
     *
     * @param ticks the number of ticks
     * @return the equivalent milliseconds
     */
    public static long ticksToMillis(long ticks) {
        return ticks * 50L;
    }

    /**
     * Converts milliseconds to ticks (20 ticks = 1 second).
     *
     * @param millis the milliseconds
     * @return the equivalent ticks
     */
    public static long millisToTicks(long millis) {
        return millis / 50L;
    }

    /**
     * Converts a Duration to ticks.
     *
     * @param duration the duration
     * @return the equivalent ticks
     */
    public static long toTicks(Duration duration) {
        return millisToTicks(duration.toMillis());
    }

    /**
     * Creates a Duration from ticks.
     *
     * @param ticks the number of ticks
     * @return the equivalent Duration
     */
    public static Duration fromTicks(long ticks) {
        return Duration.ofMillis(ticksToMillis(ticks));
    }

    // ==================== Formatting ====================

    /**
     * Formats an instant using the default date-time format.
     *
     * @param instant the instant to format
     * @return the formatted date-time string
     */
    public static String formatDateTime(Instant instant) {
        return formatDateTime(instant, DEFAULT_DATE_FORMAT);
    }

    /**
     * Formats an instant using a custom formatter.
     *
     * @param instant the instant to format
     * @param formatter the formatter to use
     * @return the formatted date-time string
     */
    public static String formatDateTime(Instant instant, DateTimeFormatter formatter) {
        if (instant == null) {
            return "N/A";
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return ldt.format(formatter);
    }

    /**
     * Formats a timestamp in milliseconds using the default format.
     *
     * @param timestampMillis the timestamp in milliseconds
     * @return the formatted date-time string
     */
    public static String formatDateTime(long timestampMillis) {
        return formatDateTime(Instant.ofEpochMilli(timestampMillis));
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if a duration string is valid and can be parsed.
     *
     * @param input the duration string to validate
     * @return true if the string can be parsed as a duration
     */
    public static boolean isValidDuration(String input) {
        try {
            parseDuration(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns the current timestamp in milliseconds.
     *
     * @return the current timestamp
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Returns the current instant.
     *
     * @return the current instant
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * Calculates the time remaining until a future timestamp.
     *
     * @param futureMillis the future timestamp in milliseconds
     * @return the remaining duration, or Duration.ZERO if already passed
     */
    public static Duration timeUntil(long futureMillis) {
        long remaining = futureMillis - System.currentTimeMillis();
        return remaining > 0 ? Duration.ofMillis(remaining) : Duration.ZERO;
    }

    /**
     * Calculates the time elapsed since a past timestamp.
     *
     * @param pastMillis the past timestamp in milliseconds
     * @return the elapsed duration, or Duration.ZERO if in the future
     */
    public static Duration timeSince(long pastMillis) {
        long elapsed = System.currentTimeMillis() - pastMillis;
        return elapsed > 0 ? Duration.ofMillis(elapsed) : Duration.ZERO;
    }
}
