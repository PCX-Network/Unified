/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * A condition that checks if the current time matches a cron expression.
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Check if it's the weekend
 * Condition isWeekend = Condition.cron("0 0 * * SAT,SUN");
 *
 * // Check if it's night time (8 PM to 6 AM)
 * Condition isNight = Condition.cron("* 20-23,0-6 * * *");
 *
 * // Check if it's during business hours
 * Condition businessHours = Condition.cron("* 9-17 * * MON-FRI");
 *
 * // Every hour on the hour
 * Condition hourlyEvent = Condition.cron("0 * * * *");
 *
 * // Combine with other conditions
 * Condition weekendVip = Condition.all(
 *     Condition.permission("group.vip"),
 *     Condition.cron("* * * * SAT,SUN")
 * );
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 * @see CronExpression
 */
public interface CronCondition extends Condition {

    /**
     * Returns the cron expression for this condition.
     *
     * @return the cron expression
     * @since 1.0.0
     */
    @NotNull
    CronExpression getCronExpression();

    /**
     * Returns the cron expression string.
     *
     * @return the cron expression string
     * @since 1.0.0
     */
    @NotNull
    default String getExpressionString() {
        return getCronExpression().getExpression();
    }

    @Override
    @NotNull
    default String getName() {
        return "cron:" + getExpressionString();
    }

    @Override
    @NotNull
    default String getType() {
        return "cron";
    }

    @Override
    @NotNull
    default String getDescription() {
        return getCronExpression().getDescription();
    }

    @Override
    @NotNull
    default Optional<Duration> getCacheTtl() {
        // Cache cron results for 1 minute since they change at most every minute
        return Optional.of(Duration.ofMinutes(1));
    }

    @Override
    @NotNull
    default ConditionResult evaluate(@NotNull ConditionContext context) {
        CronExpression cron = getCronExpression();
        if (cron.matches(context.getTimestamp())) {
            return ConditionResult.success("Time matches: " + cron.getDescription());
        }
        return ConditionResult.failure("Time does not match: " + cron.getDescription());
    }

    /**
     * Creates a cron condition.
     *
     * @param cronExpression the cron expression string
     * @return the condition
     * @throws IllegalArgumentException if the expression is invalid
     * @since 1.0.0
     */
    @NotNull
    static CronCondition of(@NotNull String cronExpression) {
        Objects.requireNonNull(cronExpression, "cronExpression cannot be null");
        CronExpression parsed = CronExpression.parse(cronExpression);
        return () -> parsed;
    }

    /**
     * Creates a cron condition with a specific time zone.
     *
     * @param cronExpression the cron expression string
     * @param timeZone       the time zone for matching
     * @return the condition
     * @throws IllegalArgumentException if the expression is invalid
     * @since 1.0.0
     */
    @NotNull
    static CronCondition of(@NotNull String cronExpression, @NotNull ZoneId timeZone) {
        Objects.requireNonNull(cronExpression, "cronExpression cannot be null");
        Objects.requireNonNull(timeZone, "timeZone cannot be null");
        CronExpression parsed = CronExpression.parse(cronExpression, timeZone);
        return () -> parsed;
    }

    /**
     * Creates a cron condition from a parsed expression.
     *
     * @param cronExpression the parsed cron expression
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static CronCondition of(@NotNull CronExpression cronExpression) {
        Objects.requireNonNull(cronExpression, "cronExpression cannot be null");
        return () -> cronExpression;
    }
}
