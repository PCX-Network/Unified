/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A condition that checks if the current time is within a specified range.
 *
 * <p>This condition supports ranges that wrap around midnight. For example,
 * a range from 20:00 to 06:00 matches evening and night hours.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Night time (8 PM to 6 AM, wraps around midnight)
 * Condition isNight = Condition.timeRange(
 *     LocalTime.of(20, 0),
 *     LocalTime.of(6, 0)
 * );
 *
 * // Business hours (9 AM to 5 PM)
 * Condition isBusinessHours = Condition.timeRange(
 *     LocalTime.of(9, 0),
 *     LocalTime.of(17, 0)
 * );
 *
 * // Morning (6 AM to 12 PM)
 * Condition isMorning = Condition.timeRange(
 *     LocalTime.of(6, 0),
 *     LocalTime.of(12, 0)
 * );
 *
 * // Combine with other conditions
 * Condition nightVip = Condition.all(
 *     Condition.permission("group.vip"),
 *     Condition.timeRange(LocalTime.of(20, 0), LocalTime.of(6, 0))
 * );
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public interface TimeRangeCondition extends Condition {

    /**
     * Returns the start time of the range (inclusive).
     *
     * @return the start time
     * @since 1.0.0
     */
    @NotNull
    LocalTime getStartTime();

    /**
     * Returns the end time of the range (inclusive).
     *
     * @return the end time
     * @since 1.0.0
     */
    @NotNull
    LocalTime getEndTime();

    /**
     * Checks if this range wraps around midnight.
     *
     * @return true if the range wraps (e.g., 20:00 to 06:00)
     * @since 1.0.0
     */
    default boolean wrapsAroundMidnight() {
        return getStartTime().isAfter(getEndTime());
    }

    @Override
    @NotNull
    default String getName() {
        return "timeRange:" + getStartTime() + "-" + getEndTime();
    }

    @Override
    @NotNull
    default String getType() {
        return "timeRange";
    }

    @Override
    @NotNull
    default String getDescription() {
        return "Time between " + getStartTime() + " and " + getEndTime();
    }

    @Override
    @NotNull
    default Optional<Duration> getCacheTtl() {
        // Cache for 1 minute
        return Optional.of(Duration.ofMinutes(1));
    }

    @Override
    @NotNull
    default ConditionResult evaluate(@NotNull ConditionContext context) {
        LocalTime now = context.getLocalDateTime().toLocalTime();
        LocalTime start = getStartTime();
        LocalTime end = getEndTime();

        boolean inRange;
        if (wrapsAroundMidnight()) {
            // Range wraps around midnight (e.g., 20:00 to 06:00)
            inRange = !now.isBefore(start) || !now.isAfter(end);
        } else {
            // Normal range (e.g., 09:00 to 17:00)
            inRange = !now.isBefore(start) && !now.isAfter(end);
        }

        if (inRange) {
            return ConditionResult.success("Current time " + now + " is in range " + start + " - " + end);
        }
        return ConditionResult.failure("Current time " + now + " is not in range " + start + " - " + end);
    }

    /**
     * Creates a time range condition.
     *
     * @param start the start time (inclusive)
     * @param end   the end time (inclusive)
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static TimeRangeCondition of(@NotNull LocalTime start, @NotNull LocalTime end) {
        Objects.requireNonNull(start, "start cannot be null");
        Objects.requireNonNull(end, "end cannot be null");
        return new TimeRangeCondition() {
            @Override
            public @NotNull LocalTime getStartTime() {
                return start;
            }

            @Override
            public @NotNull LocalTime getEndTime() {
                return end;
            }
        };
    }

    /**
     * Creates a time range condition from hour values.
     *
     * @param startHour the start hour (0-23)
     * @param endHour   the end hour (0-23)
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static TimeRangeCondition of(int startHour, int endHour) {
        return of(LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }
}
