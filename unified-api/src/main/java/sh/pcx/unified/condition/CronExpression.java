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
import java.util.Optional;
import java.util.Set;

/**
 * Represents a cron expression for time-based scheduling and matching.
 *
 * <p>This implementation supports the standard 5-field cron format:</p>
 * <pre>
 * ┌───────────── minute (0 - 59)
 * │ ┌───────────── hour (0 - 23)
 * │ │ ┌───────────── day of month (1 - 31)
 * │ │ │ ┌───────────── month (1 - 12 or JAN-DEC)
 * │ │ │ │ ┌───────────── day of week (0 - 6 or SUN-SAT, where 0 = Sunday)
 * │ │ │ │ │
 * * * * * *
 * </pre>
 *
 * <h2>Supported Syntax:</h2>
 * <ul>
 *   <li>{@code *} - Any value</li>
 *   <li>{@code 5} - Specific value</li>
 *   <li>{@code 1,3,5} - List of values</li>
 *   <li>{@code 1-5} - Range of values</li>
 *   <li>{@code *&#47;5} - Step values (every 5)</li>
 *   <li>{@code 1-10/2} - Step within range</li>
 *   <li>{@code SUN,SAT} - Day names</li>
 *   <li>{@code JAN,FEB} - Month names</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Parse expressions
 * CronExpression weekends = CronExpression.parse("0 0 * * SAT,SUN");
 * CronExpression nightTime = CronExpression.parse("* 20-23,0-6 * * *");
 * CronExpression hourly = CronExpression.parse("0 * * * *");
 * CronExpression everyFiveMinutes = CronExpression.parse("*&#47;5 * * * *");
 *
 * // Check if currently matching
 * if (weekends.matches(Instant.now())) {
 *     // It's the weekend!
 * }
 *
 * // Get next occurrence
 * Optional<Instant> next = hourly.nextExecution();
 * next.ifPresent(time -> System.out.println("Next run: " + time));
 *
 * // Check specific time
 * ZonedDateTime christmas = ZonedDateTime.of(2024, 12, 25, 0, 0, 0, 0, ZoneId.systemDefault());
 * boolean isChristmas = CronExpression.parse("* * 25 12 *").matches(christmas.toInstant());
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see CronCondition
 */
public interface CronExpression {

    /**
     * Returns the original cron expression string.
     *
     * @return the cron expression
     * @since 1.0.0
     */
    @NotNull
    String getExpression();

    /**
     * Returns the time zone used for matching.
     *
     * @return the time zone
     * @since 1.0.0
     */
    @NotNull
    ZoneId getTimeZone();

    /**
     * Checks if the given instant matches this cron expression.
     *
     * @param instant the instant to check
     * @return true if matching
     * @since 1.0.0
     */
    boolean matches(@NotNull Instant instant);

    /**
     * Checks if the given date-time matches this cron expression.
     *
     * @param dateTime the date-time to check
     * @return true if matching
     * @since 1.0.0
     */
    boolean matches(@NotNull LocalDateTime dateTime);

    /**
     * Checks if the given zoned date-time matches this cron expression.
     *
     * @param zonedDateTime the zoned date-time to check
     * @return true if matching
     * @since 1.0.0
     */
    boolean matches(@NotNull ZonedDateTime zonedDateTime);

    /**
     * Checks if the current time matches this cron expression.
     *
     * @return true if currently matching
     * @since 1.0.0
     */
    default boolean matchesNow() {
        return matches(Instant.now());
    }

    /**
     * Returns the next instant that matches this cron expression.
     *
     * @return the next matching instant, or empty if none within reasonable range
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> nextExecution();

    /**
     * Returns the next instant that matches this cron expression after the given time.
     *
     * @param after the time to search after
     * @return the next matching instant, or empty if none within reasonable range
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> nextExecution(@NotNull Instant after);

    /**
     * Returns the previous instant that matched this cron expression.
     *
     * @return the previous matching instant, or empty if none within reasonable range
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> previousExecution();

    /**
     * Returns the previous instant that matched this cron expression before the given time.
     *
     * @param before the time to search before
     * @return the previous matching instant, or empty if none within reasonable range
     * @since 1.0.0
     */
    @NotNull
    Optional<Instant> previousExecution(@NotNull Instant before);

    /**
     * Returns the set of matching minutes (0-59).
     *
     * @return the matching minutes
     * @since 1.0.0
     */
    @NotNull
    Set<Integer> getMinutes();

    /**
     * Returns the set of matching hours (0-23).
     *
     * @return the matching hours
     * @since 1.0.0
     */
    @NotNull
    Set<Integer> getHours();

    /**
     * Returns the set of matching days of month (1-31).
     *
     * @return the matching days of month
     * @since 1.0.0
     */
    @NotNull
    Set<Integer> getDaysOfMonth();

    /**
     * Returns the set of matching months (1-12).
     *
     * @return the matching months
     * @since 1.0.0
     */
    @NotNull
    Set<Integer> getMonths();

    /**
     * Returns the set of matching days of week.
     *
     * @return the matching days of week
     * @since 1.0.0
     */
    @NotNull
    Set<DayOfWeek> getDaysOfWeek();

    /**
     * Returns a human-readable description of this cron expression.
     *
     * @return a description
     * @since 1.0.0
     */
    @NotNull
    String getDescription();

    /**
     * Creates a new cron expression with a different time zone.
     *
     * @param timeZone the new time zone
     * @return a new cron expression
     * @since 1.0.0
     */
    @NotNull
    CronExpression withTimeZone(@NotNull ZoneId timeZone);

    // ==================== Static Factory Methods ====================

    /**
     * Parses a cron expression string.
     *
     * @param expression the cron expression (5 fields)
     * @return the parsed cron expression
     * @throws IllegalArgumentException if the expression is invalid
     * @since 1.0.0
     */
    @NotNull
    static CronExpression parse(@NotNull String expression) {
        return parse(expression, ZoneId.systemDefault());
    }

    /**
     * Parses a cron expression string with a specific time zone.
     *
     * @param expression the cron expression (5 fields)
     * @param timeZone   the time zone for matching
     * @return the parsed cron expression
     * @throws IllegalArgumentException if the expression is invalid
     * @since 1.0.0
     */
    @NotNull
    static CronExpression parse(@NotNull String expression, @NotNull ZoneId timeZone) {
        return CronExpressionParser.parse(expression, timeZone);
    }

    /**
     * Creates a cron expression that matches every minute.
     *
     * @return a cron expression for every minute
     * @since 1.0.0
     */
    @NotNull
    static CronExpression everyMinute() {
        return parse("* * * * *");
    }

    /**
     * Creates a cron expression that matches every hour.
     *
     * @return a cron expression for every hour
     * @since 1.0.0
     */
    @NotNull
    static CronExpression hourly() {
        return parse("0 * * * *");
    }

    /**
     * Creates a cron expression that matches every day at midnight.
     *
     * @return a cron expression for daily at midnight
     * @since 1.0.0
     */
    @NotNull
    static CronExpression daily() {
        return parse("0 0 * * *");
    }

    /**
     * Creates a cron expression that matches every day at a specific hour.
     *
     * @param hour the hour (0-23)
     * @return a cron expression for daily at the specified hour
     * @since 1.0.0
     */
    @NotNull
    static CronExpression dailyAt(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be 0-23");
        }
        return parse("0 " + hour + " * * *");
    }

    /**
     * Creates a cron expression that matches every week on Sunday at midnight.
     *
     * @return a cron expression for weekly
     * @since 1.0.0
     */
    @NotNull
    static CronExpression weekly() {
        return parse("0 0 * * 0");
    }

    /**
     * Creates a cron expression that matches weekends.
     *
     * @return a cron expression for weekends
     * @since 1.0.0
     */
    @NotNull
    static CronExpression weekends() {
        return parse("* * * * SAT,SUN");
    }

    /**
     * Creates a cron expression that matches weekdays.
     *
     * @return a cron expression for weekdays
     * @since 1.0.0
     */
    @NotNull
    static CronExpression weekdays() {
        return parse("* * * * MON-FRI");
    }

    /**
     * Checks if a string is a valid cron expression.
     *
     * @param expression the expression to validate
     * @return true if valid
     * @since 1.0.0
     */
    static boolean isValid(@NotNull String expression) {
        try {
            parse(expression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
