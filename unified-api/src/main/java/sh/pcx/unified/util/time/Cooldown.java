package sh.pcx.unified.util.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Represents a cooldown tracker for a single key or entity.
 *
 * <p>This class provides a simple way to track cooldowns, checking if actions
 * are on cooldown and how much time remains before they can be performed again.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a cooldown for a player's ability
 * Cooldown cooldown = new Cooldown(Duration.ofSeconds(30));
 *
 * // Check if on cooldown before allowing action
 * if (cooldown.isOnCooldown()) {
 *     player.sendMessage("Ability on cooldown! " +
 *         TimeUtils.formatDuration(cooldown.getRemaining()) + " remaining");
 * } else {
 *     // Perform action and start cooldown
 *     performAbility();
 *     cooldown.start();
 * }
 *
 * // Or use the test-and-set pattern
 * if (cooldown.tryStart()) {
 *     // Cooldown was not active, action can proceed
 *     performAbility();
 * } else {
 *     // Cooldown was active
 *     player.sendMessage("Please wait!");
 * }
 * }</pre>
 *
 * <h2>Progress Tracking:</h2>
 * <pre>{@code
 * // Get progress for progress bars
 * double progress = cooldown.getProgress(); // 0.0 to 1.0
 * int bars = (int) (progress * 20);
 * String progressBar = "|".repeat(bars) + " ".repeat(20 - bars);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CooldownManager
 */
public class Cooldown {

    private final long durationMillis;
    private volatile long startTime;
    private volatile boolean active;

    /**
     * Creates a new cooldown with the specified duration.
     *
     * @param duration the cooldown duration
     * @throws IllegalArgumentException if duration is null or negative
     */
    public Cooldown(Duration duration) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("Duration must be non-null and non-negative");
        }
        this.durationMillis = duration.toMillis();
        this.startTime = 0;
        this.active = false;
    }

    /**
     * Creates a new cooldown with the specified duration in milliseconds.
     *
     * @param durationMillis the cooldown duration in milliseconds
     * @throws IllegalArgumentException if duration is negative
     */
    public Cooldown(long durationMillis) {
        if (durationMillis < 0) {
            throw new IllegalArgumentException("Duration must be non-negative");
        }
        this.durationMillis = durationMillis;
        this.startTime = 0;
        this.active = false;
    }

    /**
     * Creates a new cooldown with the specified duration and time unit.
     *
     * @param duration the duration value
     * @param unit the time unit
     * @throws IllegalArgumentException if unit is null or duration is negative
     */
    public Cooldown(long duration, TimeUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("Duration must be non-negative");
        }
        this.durationMillis = unit.toMillis(duration);
        this.startTime = 0;
        this.active = false;
    }

    /**
     * Starts the cooldown from the current time.
     *
     * <p>If the cooldown is already active, this will reset it.</p>
     *
     * @return this cooldown for chaining
     */
    public Cooldown start() {
        this.startTime = System.currentTimeMillis();
        this.active = true;
        return this;
    }

    /**
     * Starts the cooldown only if it's not currently active.
     *
     * @return true if the cooldown was started, false if it was already active
     */
    public boolean tryStart() {
        if (isOnCooldown()) {
            return false;
        }
        start();
        return true;
    }

    /**
     * Resets the cooldown, making it inactive.
     *
     * @return this cooldown for chaining
     */
    public Cooldown reset() {
        this.active = false;
        this.startTime = 0;
        return this;
    }

    /**
     * Checks if the cooldown is currently active.
     *
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown() {
        if (!active) {
            return false;
        }
        if (getRemainingMillis() <= 0) {
            active = false;
            return false;
        }
        return true;
    }

    /**
     * Checks if the cooldown has expired (not on cooldown).
     *
     * @return true if the cooldown has expired or was never started
     */
    public boolean isExpired() {
        return !isOnCooldown();
    }

    /**
     * Checks if the cooldown is ready (not on cooldown).
     *
     * @return true if ready, false if on cooldown
     */
    public boolean isReady() {
        return !isOnCooldown();
    }

    /**
     * Gets the remaining time on the cooldown.
     *
     * @return the remaining duration, or Duration.ZERO if not on cooldown
     */
    public Duration getRemaining() {
        long remaining = getRemainingMillis();
        return remaining > 0 ? Duration.ofMillis(remaining) : Duration.ZERO;
    }

    /**
     * Gets the remaining time in milliseconds.
     *
     * @return the remaining milliseconds, or 0 if not on cooldown
     */
    public long getRemainingMillis() {
        if (!active) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = durationMillis - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Gets the remaining time in the specified time unit.
     *
     * @param unit the time unit
     * @return the remaining time in the specified unit
     */
    public long getRemaining(TimeUnit unit) {
        return unit.convert(getRemainingMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the elapsed time since the cooldown started.
     *
     * @return the elapsed duration
     */
    public Duration getElapsed() {
        if (!active) {
            return Duration.ZERO;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return Duration.ofMillis(Math.min(elapsed, durationMillis));
    }

    /**
     * Gets the elapsed time in milliseconds.
     *
     * @return the elapsed milliseconds
     */
    public long getElapsedMillis() {
        if (!active) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(elapsed, durationMillis);
    }

    /**
     * Gets the progress of the cooldown as a value from 0.0 to 1.0.
     *
     * <p>0.0 means just started, 1.0 means complete/expired.</p>
     *
     * @return the progress value between 0.0 and 1.0
     */
    public double getProgress() {
        if (!active || durationMillis == 0) {
            return 1.0;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0, (double) elapsed / durationMillis);
    }

    /**
     * Gets the remaining progress as a value from 0.0 to 1.0.
     *
     * <p>1.0 means just started, 0.0 means complete/expired.</p>
     *
     * @return the remaining progress value between 0.0 and 1.0
     */
    public double getRemainingProgress() {
        return 1.0 - getProgress();
    }

    /**
     * Gets the total duration of this cooldown.
     *
     * @return the total duration
     */
    public Duration getDuration() {
        return Duration.ofMillis(durationMillis);
    }

    /**
     * Gets the total duration in milliseconds.
     *
     * @return the total duration in milliseconds
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * Gets the instant when the cooldown was started.
     *
     * @return an Optional containing the start instant, or empty if never started
     */
    public Optional<Instant> getStartTime() {
        return active ? Optional.of(Instant.ofEpochMilli(startTime)) : Optional.empty();
    }

    /**
     * Gets the instant when the cooldown will expire.
     *
     * @return an Optional containing the expiry instant, or empty if not active
     */
    public Optional<Instant> getExpiryTime() {
        return active ? Optional.of(Instant.ofEpochMilli(startTime + durationMillis)) : Optional.empty();
    }

    /**
     * Extends the cooldown by the specified duration.
     *
     * <p>If the cooldown is not active, this starts it with the extended duration.</p>
     *
     * @param extension the duration to extend by
     * @return this cooldown for chaining
     */
    public Cooldown extend(Duration extension) {
        if (extension == null || extension.isNegative()) {
            return this;
        }
        if (!active) {
            start();
        }
        // Adjust start time to effectively extend the duration
        long remaining = getRemainingMillis();
        long newRemaining = remaining + extension.toMillis();
        startTime = System.currentTimeMillis() - (durationMillis - newRemaining);
        return this;
    }

    /**
     * Reduces the cooldown by the specified duration.
     *
     * @param reduction the duration to reduce by
     * @return this cooldown for chaining
     */
    public Cooldown reduce(Duration reduction) {
        if (reduction == null || reduction.isNegative() || !active) {
            return this;
        }
        long remaining = getRemainingMillis();
        long newRemaining = Math.max(0, remaining - reduction.toMillis());
        if (newRemaining == 0) {
            reset();
        } else {
            startTime = System.currentTimeMillis() - (durationMillis - newRemaining);
        }
        return this;
    }

    /**
     * Creates a copy of this cooldown with the same duration but reset state.
     *
     * @return a new Cooldown instance
     */
    public Cooldown copy() {
        return new Cooldown(durationMillis);
    }

    /**
     * Creates a copy of this cooldown with a different duration.
     *
     * @param newDuration the new duration
     * @return a new Cooldown instance
     */
    public Cooldown withDuration(Duration newDuration) {
        return new Cooldown(newDuration);
    }

    /**
     * Returns a formatted string of the remaining time.
     *
     * @return the formatted remaining time
     */
    public String formatRemaining() {
        return TimeUtils.formatDuration(getRemaining());
    }

    /**
     * Returns a compact formatted string of the remaining time.
     *
     * @return the compact formatted remaining time
     */
    public String formatRemainingCompact() {
        return TimeUtils.formatDurationCompact(getRemaining());
    }

    @Override
    public String toString() {
        if (isOnCooldown()) {
            return String.format("Cooldown[remaining=%s, progress=%.1f%%]",
                    formatRemaining(), getProgress() * 100);
        } else {
            return String.format("Cooldown[ready, duration=%s]",
                    TimeUtils.formatDuration(getDuration()));
        }
    }
}
