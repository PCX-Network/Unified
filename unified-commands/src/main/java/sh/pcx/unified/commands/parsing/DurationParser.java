/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.parsing;

import sh.pcx.unified.commands.completion.CompletionContext;
import sh.pcx.unified.commands.core.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for time duration arguments.
 *
 * <p>Parses human-readable duration strings into {@link Duration} objects.
 * Supports various time units and compound durations.</p>
 *
 * <h2>Supported Formats</h2>
 * <table border="1">
 *   <tr><th>Format</th><th>Duration</th></tr>
 *   <tr><td>30s, 30sec, 30secs, 30second, 30seconds</td><td>30 seconds</td></tr>
 *   <tr><td>5m, 5min, 5mins, 5minute, 5minutes</td><td>5 minutes</td></tr>
 *   <tr><td>2h, 2hr, 2hrs, 2hour, 2hours</td><td>2 hours</td></tr>
 *   <tr><td>1d, 1day, 1days</td><td>1 day</td></tr>
 *   <tr><td>1w, 1week, 1weeks</td><td>1 week</td></tr>
 *   <tr><td>1mo, 1month, 1months</td><td>1 month (30 days)</td></tr>
 *   <tr><td>1y, 1yr, 1year, 1years</td><td>1 year (365 days)</td></tr>
 * </table>
 *
 * <h2>Compound Durations</h2>
 * <p>Multiple units can be combined:</p>
 * <ul>
 *   <li>{@code 1h30m} - 1 hour and 30 minutes</li>
 *   <li>{@code 2d12h} - 2 days and 12 hours</li>
 *   <li>{@code 1w2d3h4m5s} - 1 week, 2 days, 3 hours, 4 minutes, 5 seconds</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Commands</h3>
 * <pre>{@code
 * @Subcommand("mute")
 * public void mute(
 *     @Sender CommandSender sender,
 *     @Arg("player") Player target,
 *     @Arg("duration") Duration duration,
 *     @Arg(value = "reason", greedy = true) @Default("No reason") String reason
 * ) {
 *     Instant expiry = Instant.now().plus(duration);
 *     muteManager.mute(target, expiry, reason);
 * }
 *
 * // Invoked as:
 * // /mute Steve 1h
 * // /mute Steve 30m Spamming
 * // /mute Steve 1d12h Breaking rules
 * }</pre>
 *
 * <h3>Cooldown Usage</h3>
 * <pre>{@code
 * @Subcommand("kit")
 * @Cooldown(value = 24, unit = TimeUnit.HOURS)
 * public void claimKit(@Sender Player player, @Arg("kit") String kitName) {
 *     // 24-hour cooldown between uses
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 */
public class DurationParser implements ArgumentParser<Duration> {

    /**
     * Pattern to match duration components (number + unit).
     */
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(\\d+)\\s*(y(?:ears?)?|mo(?:nths?)?|w(?:eeks?)?|d(?:ays?)?|h(?:(?:ou)?rs?)?|m(?:in(?:ute)?s?)?|s(?:ec(?:ond)?s?)?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> SUGGESTIONS = Arrays.asList(
            "30s", "1m", "5m", "10m", "30m",
            "1h", "2h", "6h", "12h", "24h",
            "1d", "7d", "30d"
    );

    /**
     * Seconds in a minute.
     */
    private static final long SECONDS_PER_MINUTE = 60;

    /**
     * Seconds in an hour.
     */
    private static final long SECONDS_PER_HOUR = 3600;

    /**
     * Seconds in a day.
     */
    private static final long SECONDS_PER_DAY = 86400;

    /**
     * Seconds in a week.
     */
    private static final long SECONDS_PER_WEEK = 604800;

    /**
     * Seconds in a month (30 days).
     */
    private static final long SECONDS_PER_MONTH = 2592000;

    /**
     * Seconds in a year (365 days).
     */
    private static final long SECONDS_PER_YEAR = 31536000;

    @Override
    @NotNull
    public Duration parse(@NotNull CommandContext context, @NotNull String input) throws ParseException {
        String trimmed = input.trim().toLowerCase(Locale.ROOT);

        if (trimmed.isEmpty()) {
            throw new ParseException("Duration cannot be empty");
        }

        // Try parsing as pure seconds
        try {
            long seconds = Long.parseLong(trimmed);
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            // Not a pure number, continue with pattern matching
        }

        Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        long totalSeconds = 0;
        boolean foundMatch = false;

        while (matcher.find()) {
            foundMatch = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            totalSeconds += parseUnit(value, unit);
        }

        if (!foundMatch) {
            throw new ParseException("Invalid duration format", input)
                    .withSuggestions("1h", "30m", "1d", "1h30m");
        }

        return Duration.ofSeconds(totalSeconds);
    }

    /**
     * Converts a value and unit to seconds.
     *
     * @param value the numeric value
     * @param unit the unit string
     * @return the value in seconds
     */
    private long parseUnit(long value, String unit) {
        if (unit.startsWith("y")) {
            return value * SECONDS_PER_YEAR;
        } else if (unit.startsWith("mo")) {
            return value * SECONDS_PER_MONTH;
        } else if (unit.startsWith("w")) {
            return value * SECONDS_PER_WEEK;
        } else if (unit.startsWith("d")) {
            return value * SECONDS_PER_DAY;
        } else if (unit.startsWith("h")) {
            return value * SECONDS_PER_HOUR;
        } else if (unit.startsWith("m")) {
            return value * SECONDS_PER_MINUTE;
        } else { // seconds
            return value;
        }
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull CompletionContext context) {
        String current = context.getCurrentInput();
        if (current.isEmpty()) {
            return SUGGESTIONS;
        }

        // If ends with a number, suggest units
        if (Character.isDigit(current.charAt(current.length() - 1))) {
            return Arrays.asList(
                    current + "s",
                    current + "m",
                    current + "h",
                    current + "d"
            );
        }

        return SUGGESTIONS;
    }

    @Override
    @NotNull
    public String getErrorMessage() {
        return "Invalid duration format: {input}. Use formats like 1h, 30m, 1d12h";
    }

    @Override
    public Class<Duration> getType() {
        return Duration.class;
    }

    /**
     * Formats a duration into a human-readable string.
     *
     * @param duration the duration to format
     * @return formatted string (e.g., "1h 30m 45s")
     */
    @NotNull
    public static String format(@NotNull Duration duration) {
        long seconds = duration.getSeconds();

        if (seconds == 0) {
            return "0s";
        }

        StringBuilder sb = new StringBuilder();

        long days = seconds / SECONDS_PER_DAY;
        if (days > 0) {
            sb.append(days).append("d ");
            seconds %= SECONDS_PER_DAY;
        }

        long hours = seconds / SECONDS_PER_HOUR;
        if (hours > 0) {
            sb.append(hours).append("h ");
            seconds %= SECONDS_PER_HOUR;
        }

        long minutes = seconds / SECONDS_PER_MINUTE;
        if (minutes > 0) {
            sb.append(minutes).append("m ");
            seconds %= SECONDS_PER_MINUTE;
        }

        if (seconds > 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }
}
