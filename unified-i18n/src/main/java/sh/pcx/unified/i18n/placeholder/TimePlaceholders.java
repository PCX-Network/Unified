/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Built-in time and date placeholders.
 *
 * <p>Provides placeholders for time and date information:
 * <ul>
 *   <li>{@code %time_now%} - Current time (HH:mm:ss)</li>
 *   <li>{@code %time_date%} - Current date (yyyy-MM-dd)</li>
 *   <li>{@code %time_datetime%} - Date and time</li>
 *   <li>{@code %time_year%} - Current year</li>
 *   <li>{@code %time_month%} - Current month</li>
 *   <li>{@code %time_day%} - Current day of month</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@PlaceholderExpansion(
        identifier = "time",
        author = "Supatuck",
        version = "1.0.0"
)
public final class TimePlaceholders {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Placeholder(value = "now", description = "Current time (HH:mm:ss)", cacheable = false)
    public String getNow() {
        return LocalTime.now().format(TIME_FORMAT);
    }

    @Placeholder(value = "date", description = "Current date (yyyy-MM-dd)", cacheable = false)
    public String getDate() {
        return LocalDate.now().format(DATE_FORMAT);
    }

    @Placeholder(value = "datetime", description = "Current date and time", cacheable = false)
    public String getDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }

    @Placeholder(value = "year", description = "Current year", cacheable = false)
    public String getYear() {
        return String.valueOf(LocalDate.now().getYear());
    }

    @Placeholder(value = "month", description = "Current month (1-12)", cacheable = false)
    public String getMonth() {
        return String.valueOf(LocalDate.now().getMonthValue());
    }

    @Placeholder(value = "month_name", description = "Month name", cacheable = false)
    public String getMonthName() {
        return LocalDate.now().getMonth().name();
    }

    @Placeholder(value = "day", description = "Day of month", cacheable = false)
    public String getDay() {
        return String.valueOf(LocalDate.now().getDayOfMonth());
    }

    @Placeholder(value = "day_of_week", description = "Day of week", cacheable = false)
    public String getDayOfWeek() {
        return LocalDate.now().getDayOfWeek().name();
    }

    @Placeholder(value = "hour", description = "Current hour (0-23)", cacheable = false)
    public String getHour() {
        return String.valueOf(LocalTime.now().getHour());
    }

    @Placeholder(value = "minute", description = "Current minute", cacheable = false)
    public String getMinute() {
        return String.valueOf(LocalTime.now().getMinute());
    }

    @Placeholder(value = "second", description = "Current second", cacheable = false)
    public String getSecond() {
        return String.valueOf(LocalTime.now().getSecond());
    }

    @Placeholder(value = "timezone", description = "System timezone", cacheable = false)
    public String getTimezone() {
        return ZoneId.systemDefault().getId();
    }

    @Placeholder(value = "epoch", description = "Unix timestamp", cacheable = false)
    public String getEpoch() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    @Placeholder(value = "epoch_millis", description = "Unix timestamp in milliseconds", cacheable = false)
    public String getEpochMillis() {
        return String.valueOf(System.currentTimeMillis());
    }
}
