/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.TemporaryCondition;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Event fired when a temporary condition expires naturally.
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onTemporaryExpired(TemporaryConditionExpiredEvent event) {
 *     TemporaryCondition condition = event.getTemporaryCondition();
 *
 *     logger.info("Temporary condition '" + condition.getName() +
 *                 "' expired after " + event.getOriginalDuration().toMinutes() + " minutes");
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see TemporaryConditionEvent
 */
public class TemporaryConditionExpiredEvent extends TemporaryConditionEvent {

    /**
     * Constructs a new expired event.
     *
     * @param condition the temporary condition that expired
     */
    public TemporaryConditionExpiredEvent(@NotNull TemporaryCondition condition) {
        super(condition);
    }

    /**
     * Returns the original duration of the condition.
     *
     * @return the original duration
     * @since 1.0.0
     */
    @NotNull
    public Duration getOriginalDuration() {
        return getTemporaryCondition().getDuration();
    }

    /**
     * Returns how long the condition was active.
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
        return "TemporaryConditionExpiredEvent[player=" + getPlayerId() +
                ", name=" + getConditionName() +
                ", duration=" + getOriginalDuration() + "]";
    }
}
