/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.TemporaryCondition;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Event fired when a temporary condition is cancelled before expiration.
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onTemporaryCancelled(TemporaryConditionCancelledEvent event) {
 *     TemporaryCondition condition = event.getTemporaryCondition();
 *     Duration remaining = event.getRemainingTime();
 *
 *     logger.info("Temporary condition '" + condition.getName() +
 *                 "' cancelled with " + remaining.toMinutes() + " minutes remaining");
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see TemporaryConditionEvent
 */
public class TemporaryConditionCancelledEvent extends TemporaryConditionEvent {

    private final Duration remainingTime;

    /**
     * Constructs a new cancelled event.
     *
     * @param condition     the temporary condition that was cancelled
     * @param remainingTime the time that was remaining
     */
    public TemporaryConditionCancelledEvent(@NotNull TemporaryCondition condition, @NotNull Duration remainingTime) {
        super(condition);
        this.remainingTime = remainingTime;
    }

    /**
     * Returns the time that was remaining when the condition was cancelled.
     *
     * @return the remaining time
     * @since 1.0.0
     */
    @NotNull
    public Duration getRemainingTime() {
        return remainingTime;
    }

    /**
     * Returns how long the condition was active before cancellation.
     *
     * @return the elapsed time
     * @since 1.0.0
     */
    @NotNull
    public Duration getElapsedTime() {
        return getTemporaryCondition().getElapsed();
    }

    @Override
    public String toString() {
        return "TemporaryConditionCancelledEvent[player=" + getPlayerId() +
                ", name=" + getConditionName() +
                ", remaining=" + remainingTime + "]";
    }
}
