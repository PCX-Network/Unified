/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for cache time-to-live (TTL) in the placeholder system.
 *
 * <p>CacheTTL defines how long cached placeholder results remain valid before
 * being evicted or refreshed. Different placeholders may require different
 * TTL values based on how frequently their values change.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Common TTL values
 * CacheTTL instant = CacheTTL.NONE;           // No caching
 * CacheTTL quick = CacheTTL.SECONDS_1;        // 1 second (rapidly changing)
 * CacheTTL normal = CacheTTL.SECONDS_30;      // 30 seconds (default)
 * CacheTTL slow = CacheTTL.MINUTES_5;         // 5 minutes (rarely changes)
 * CacheTTL persistent = CacheTTL.HOURS_1;     // 1 hour (almost static)
 *
 * // Custom TTL
 * CacheTTL custom = CacheTTL.of(15, TimeUnit.SECONDS);
 * CacheTTL fromDuration = CacheTTL.of(Duration.ofMinutes(2));
 *
 * // Usage in placeholder definition
 * @Placeholder(value = "balance", cacheTTL = 5000) // 5 seconds
 * public String getBalance(UnifiedPlayer player) {
 *     return economy.getBalance(player).toString();
 * }
 * }</pre>
 *
 * <h2>TTL Guidelines</h2>
 * <ul>
 *   <li><strong>NONE (0ms)</strong> - Time, random values, rapidly changing data</li>
 *   <li><strong>1-5 seconds</strong> - Health, position, TPS</li>
 *   <li><strong>10-30 seconds</strong> - Balance, stats, online count</li>
 *   <li><strong>1-5 minutes</strong> - Faction info, permissions</li>
 *   <li><strong>1+ hours</strong> - Server name, version, static config</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderCache
 * @see Placeholder#cacheTTL()
 */
public final class CacheTTL {

    /**
     * No caching - value is computed every time.
     */
    public static final CacheTTL NONE = new CacheTTL(0);

    /**
     * 1 second TTL - for rapidly changing values.
     */
    public static final CacheTTL SECONDS_1 = new CacheTTL(1000);

    /**
     * 5 seconds TTL.
     */
    public static final CacheTTL SECONDS_5 = new CacheTTL(5000);

    /**
     * 10 seconds TTL.
     */
    public static final CacheTTL SECONDS_10 = new CacheTTL(10000);

    /**
     * 30 seconds TTL - default for most placeholders.
     */
    public static final CacheTTL SECONDS_30 = new CacheTTL(30000);

    /**
     * 1 minute TTL.
     */
    public static final CacheTTL MINUTES_1 = new CacheTTL(60000);

    /**
     * 5 minutes TTL - for slowly changing data.
     */
    public static final CacheTTL MINUTES_5 = new CacheTTL(300000);

    /**
     * 15 minutes TTL.
     */
    public static final CacheTTL MINUTES_15 = new CacheTTL(900000);

    /**
     * 30 minutes TTL.
     */
    public static final CacheTTL MINUTES_30 = new CacheTTL(1800000);

    /**
     * 1 hour TTL - for rarely changing data.
     */
    public static final CacheTTL HOURS_1 = new CacheTTL(3600000);

    /**
     * 24 hours TTL - for essentially static data.
     */
    public static final CacheTTL HOURS_24 = new CacheTTL(86400000);

    /**
     * Default TTL used when not specified (30 seconds).
     */
    public static final CacheTTL DEFAULT = SECONDS_30;

    private final long milliseconds;

    private CacheTTL(long milliseconds) {
        this.milliseconds = Math.max(0, milliseconds);
    }

    /**
     * Creates a TTL from a duration in the specified time unit.
     *
     * @param duration the duration value
     * @param unit     the time unit
     * @return a new CacheTTL
     */
    @NotNull
    public static CacheTTL of(long duration, @NotNull TimeUnit unit) {
        Objects.requireNonNull(unit, "unit cannot be null");
        return new CacheTTL(unit.toMillis(duration));
    }

    /**
     * Creates a TTL from a Duration.
     *
     * @param duration the duration
     * @return a new CacheTTL
     */
    @NotNull
    public static CacheTTL of(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        return new CacheTTL(duration.toMillis());
    }

    /**
     * Creates a TTL from milliseconds.
     *
     * @param milliseconds the TTL in milliseconds
     * @return a new CacheTTL
     */
    @NotNull
    public static CacheTTL ofMillis(long milliseconds) {
        if (milliseconds <= 0) return NONE;
        return new CacheTTL(milliseconds);
    }

    /**
     * Creates a TTL from seconds.
     *
     * @param seconds the TTL in seconds
     * @return a new CacheTTL
     */
    @NotNull
    public static CacheTTL ofSeconds(long seconds) {
        return of(seconds, TimeUnit.SECONDS);
    }

    /**
     * Creates a TTL from minutes.
     *
     * @param minutes the TTL in minutes
     * @return a new CacheTTL
     */
    @NotNull
    public static CacheTTL ofMinutes(long minutes) {
        return of(minutes, TimeUnit.MINUTES);
    }

    /**
     * Creates a TTL from hours.
     *
     * @param hours the TTL in hours
     * @return a new CacheTTL
     */
    @NotNull
    public static CacheTTL ofHours(long hours) {
        return of(hours, TimeUnit.HOURS);
    }

    /**
     * Returns the TTL in milliseconds.
     *
     * @return the TTL in milliseconds
     */
    public long toMillis() {
        return milliseconds;
    }

    /**
     * Returns the TTL in the specified time unit.
     *
     * @param unit the target time unit
     * @return the TTL in the specified unit
     */
    public long to(@NotNull TimeUnit unit) {
        Objects.requireNonNull(unit, "unit cannot be null");
        return unit.convert(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the TTL as a Duration.
     *
     * @return the duration
     */
    @NotNull
    public Duration toDuration() {
        return Duration.ofMillis(milliseconds);
    }

    /**
     * Checks if this TTL disables caching.
     *
     * @return {@code true} if TTL is zero (no caching)
     */
    public boolean isNone() {
        return milliseconds == 0;
    }

    /**
     * Checks if the given timestamp has expired based on this TTL.
     *
     * @param timestamp the timestamp to check
     * @return {@code true} if expired
     */
    public boolean isExpired(long timestamp) {
        if (milliseconds == 0) return true;
        return System.currentTimeMillis() - timestamp >= milliseconds;
    }

    /**
     * Returns a new TTL multiplied by the given factor.
     *
     * @param factor the multiplication factor
     * @return a new scaled CacheTTL
     */
    @NotNull
    public CacheTTL multiply(double factor) {
        return new CacheTTL((long) (milliseconds * factor));
    }

    /**
     * Returns the longer of this TTL and the other.
     *
     * @param other the other TTL
     * @return the longer TTL
     */
    @NotNull
    public CacheTTL max(@NotNull CacheTTL other) {
        Objects.requireNonNull(other, "other cannot be null");
        return milliseconds >= other.milliseconds ? this : other;
    }

    /**
     * Returns the shorter of this TTL and the other.
     *
     * @param other the other TTL
     * @return the shorter TTL
     */
    @NotNull
    public CacheTTL min(@NotNull CacheTTL other) {
        Objects.requireNonNull(other, "other cannot be null");
        return milliseconds <= other.milliseconds ? this : other;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CacheTTL)) return false;
        return milliseconds == ((CacheTTL) obj).milliseconds;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(milliseconds);
    }

    @Override
    public String toString() {
        if (milliseconds == 0) return "CacheTTL.NONE";
        if (milliseconds < 1000) return "CacheTTL(" + milliseconds + "ms)";
        if (milliseconds < 60000) return "CacheTTL(" + (milliseconds / 1000) + "s)";
        if (milliseconds < 3600000) return "CacheTTL(" + (milliseconds / 60000) + "m)";
        return "CacheTTL(" + (milliseconds / 3600000) + "h)";
    }
}
