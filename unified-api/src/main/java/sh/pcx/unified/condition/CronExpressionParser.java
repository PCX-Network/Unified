/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal parser for cron expressions.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
final class CronExpressionParser {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^(\\*|\\d+(?:-\\d+)?(?:/\\d+)?|(?:\\d+(?:-\\d+)?,)+\\d+(?:-\\d+)?|" +
            "(?:SUN|MON|TUE|WED|THU|FRI|SAT|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)" +
            "(?:,(?:SUN|MON|TUE|WED|THU|FRI|SAT|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))*)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, Integer> DAY_NAMES = Map.of(
            "SUN", 0, "MON", 1, "TUE", 2, "WED", 3,
            "THU", 4, "FRI", 5, "SAT", 6
    );

    private static final Map<String, Integer> MONTH_NAMES = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("FEB", 2), Map.entry("MAR", 3),
            Map.entry("APR", 4), Map.entry("MAY", 5), Map.entry("JUN", 6),
            Map.entry("JUL", 7), Map.entry("AUG", 8), Map.entry("SEP", 9),
            Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12)
    );

    private CronExpressionParser() {}

    @NotNull
    static CronExpression parse(@NotNull String expression, @NotNull ZoneId timeZone) {
        Objects.requireNonNull(expression, "expression cannot be null");
        Objects.requireNonNull(timeZone, "timeZone cannot be null");

        String[] fields = expression.trim().split("\\s+");
        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "Cron expression must have exactly 5 fields (minute, hour, day, month, day-of-week), got " + fields.length
            );
        }

        Set<Integer> minutes = parseField(fields[0], 0, 59, "minute");
        Set<Integer> hours = parseField(fields[1], 0, 23, "hour");
        Set<Integer> daysOfMonth = parseField(fields[2], 1, 31, "day of month");
        Set<Integer> months = parseMonthField(fields[3]);
        Set<DayOfWeek> daysOfWeek = parseDayOfWeekField(fields[4]);

        return new CronExpressionImpl(expression, timeZone, minutes, hours, daysOfMonth, months, daysOfWeek);
    }

    private static Set<Integer> parseField(String field, int min, int max, String name) {
        Set<Integer> values = new TreeSet<>();

        // Handle wildcard
        if (field.equals("*")) {
            for (int i = min; i <= max; i++) {
                values.add(i);
            }
            return values;
        }

        // Handle step values (*/5 or 1-10/2)
        if (field.contains("/")) {
            String[] parts = field.split("/");
            int step = Integer.parseInt(parts[1]);
            int start = min;
            int end = max;

            if (!parts[0].equals("*")) {
                if (parts[0].contains("-")) {
                    String[] range = parts[0].split("-");
                    start = Integer.parseInt(range[0]);
                    end = Integer.parseInt(range[1]);
                } else {
                    start = Integer.parseInt(parts[0]);
                }
            }

            for (int i = start; i <= end; i += step) {
                if (i >= min && i <= max) {
                    values.add(i);
                }
            }
            return values;
        }

        // Handle comma-separated values
        if (field.contains(",")) {
            for (String part : field.split(",")) {
                values.addAll(parseField(part, min, max, name));
            }
            return values;
        }

        // Handle ranges
        if (field.contains("-")) {
            String[] parts = field.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            for (int i = start; i <= end; i++) {
                if (i >= min && i <= max) {
                    values.add(i);
                }
            }
            return values;
        }

        // Single value
        int value = Integer.parseInt(field);
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "Value " + value + " out of range for " + name + " (" + min + "-" + max + ")"
            );
        }
        values.add(value);
        return values;
    }

    private static Set<Integer> parseMonthField(String field) {
        // Replace month names with numbers
        String normalizedField = field.toUpperCase();
        for (Map.Entry<String, Integer> entry : MONTH_NAMES.entrySet()) {
            normalizedField = normalizedField.replace(entry.getKey(), entry.getValue().toString());
        }
        return parseField(normalizedField, 1, 12, "month");
    }

    private static Set<DayOfWeek> parseDayOfWeekField(String field) {
        // Handle wildcard
        if (field.equals("*")) {
            return EnumSet.allOf(DayOfWeek.class);
        }

        // Replace day names with numbers
        String normalizedField = field.toUpperCase();
        for (Map.Entry<String, Integer> entry : DAY_NAMES.entrySet()) {
            normalizedField = normalizedField.replace(entry.getKey(), entry.getValue().toString());
        }

        Set<Integer> dayNumbers = parseField(normalizedField, 0, 6, "day of week");
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

        for (int dayNum : dayNumbers) {
            // Convert cron day (0=SUN, 6=SAT) to Java DayOfWeek (1=MON, 7=SUN)
            DayOfWeek day = dayNum == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(dayNum);
            days.add(day);
        }

        return days;
    }

    /**
     * Implementation of CronExpression.
     */
    private record CronExpressionImpl(
            String expression,
            ZoneId timeZone,
            Set<Integer> minutes,
            Set<Integer> hours,
            Set<Integer> daysOfMonth,
            Set<Integer> months,
            Set<DayOfWeek> daysOfWeek
    ) implements CronExpression {

        @Override
        public @NotNull String getExpression() {
            return expression;
        }

        @Override
        public @NotNull ZoneId getTimeZone() {
            return timeZone;
        }

        @Override
        public boolean matches(@NotNull Instant instant) {
            ZonedDateTime zdt = instant.atZone(timeZone);
            return matches(zdt);
        }

        @Override
        public boolean matches(@NotNull LocalDateTime dateTime) {
            return matches(dateTime.atZone(timeZone));
        }

        @Override
        public boolean matches(@NotNull ZonedDateTime zonedDateTime) {
            ZonedDateTime zdt = zonedDateTime.withZoneSameInstant(timeZone);

            return minutes.contains(zdt.getMinute()) &&
                   hours.contains(zdt.getHour()) &&
                   daysOfMonth.contains(zdt.getDayOfMonth()) &&
                   months.contains(zdt.getMonthValue()) &&
                   daysOfWeek.contains(zdt.getDayOfWeek());
        }

        @Override
        public @NotNull Optional<Instant> nextExecution() {
            return nextExecution(Instant.now());
        }

        @Override
        public @NotNull Optional<Instant> nextExecution(@NotNull Instant after) {
            ZonedDateTime candidate = after.atZone(timeZone).plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
            ZonedDateTime maxSearch = candidate.plusYears(1);

            while (candidate.isBefore(maxSearch)) {
                if (matches(candidate)) {
                    return Optional.of(candidate.toInstant());
                }

                // Advance to next possible time
                candidate = candidate.plusMinutes(1);
            }

            return Optional.empty();
        }

        @Override
        public @NotNull Optional<Instant> previousExecution() {
            return previousExecution(Instant.now());
        }

        @Override
        public @NotNull Optional<Instant> previousExecution(@NotNull Instant before) {
            ZonedDateTime candidate = before.atZone(timeZone).minusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
            ZonedDateTime minSearch = candidate.minusYears(1);

            while (candidate.isAfter(minSearch)) {
                if (matches(candidate)) {
                    return Optional.of(candidate.toInstant());
                }

                // Go back to previous possible time
                candidate = candidate.minusMinutes(1);
            }

            return Optional.empty();
        }

        @Override
        public @NotNull Set<Integer> getMinutes() {
            return Set.copyOf(minutes);
        }

        @Override
        public @NotNull Set<Integer> getHours() {
            return Set.copyOf(hours);
        }

        @Override
        public @NotNull Set<Integer> getDaysOfMonth() {
            return Set.copyOf(daysOfMonth);
        }

        @Override
        public @NotNull Set<Integer> getMonths() {
            return Set.copyOf(months);
        }

        @Override
        public @NotNull Set<DayOfWeek> getDaysOfWeek() {
            return EnumSet.copyOf(daysOfWeek);
        }

        @Override
        public @NotNull String getDescription() {
            StringBuilder sb = new StringBuilder();

            // Minutes
            if (minutes.size() == 60) {
                sb.append("Every minute");
            } else if (minutes.size() == 1) {
                sb.append("At minute ").append(minutes.iterator().next());
            } else {
                sb.append("At minutes ").append(formatSet(minutes));
            }

            // Hours
            if (hours.size() != 24) {
                if (hours.size() == 1) {
                    sb.append(" of hour ").append(hours.iterator().next());
                } else {
                    sb.append(" during hours ").append(formatSet(hours));
                }
            }

            // Days of week
            if (daysOfWeek.size() != 7) {
                sb.append(" on ").append(formatDays(daysOfWeek));
            }

            return sb.toString();
        }

        @Override
        public @NotNull CronExpression withTimeZone(@NotNull ZoneId newTimeZone) {
            return new CronExpressionImpl(expression, newTimeZone, minutes, hours, daysOfMonth, months, daysOfWeek);
        }

        private static String formatSet(Set<Integer> set) {
            if (set.isEmpty()) return "";
            return set.toString().replace("[", "").replace("]", "");
        }

        private static String formatDays(Set<DayOfWeek> days) {
            return days.stream()
                    .map(d -> d.name().substring(0, 3))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }

        @Override
        public String toString() {
            return "CronExpression{" + expression + "}";
        }
    }
}
